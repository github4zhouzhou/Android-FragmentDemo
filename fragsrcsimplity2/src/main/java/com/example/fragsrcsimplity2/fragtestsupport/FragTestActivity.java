package com.example.fragsrcsimplity2.fragtestsupport;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.util.SimpleArrayMap;
import android.support.v4.util.SparseArrayCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.Window;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;

public class FragTestActivity extends Activity {

    private static final String TAG = "FragTestActivity";

    static final String FRAGTESTS_TAG = "android:support:fragTests";
    static final String NEXT_CANDIDATE_REQUEST_INDEX_TAG = "android:support:next_request_index";
    static final String ALLOCATED_REQUEST_INDICIES_TAG = "android:support:request_indicies";
    static final String REQUEST_FRAGMENT_WHO_TAG = "android:support:request_fragTest_who";
    static final int MAX_NUM_PENDING_FRAGMENT_ACTIVITY_RESULTS = 0xffff - 1;


    static final int MSG_REALLY_STOPPED = 1;
    static final int MSG_RESUME_PENDING = 2;

    boolean mCreated;
    boolean mResumed;
    boolean mStopped;
    boolean mReallyStopped;
    boolean mRetaining;

    boolean mOptionsMenuInvalidated;
    boolean mRequestedPermissionsFromFragTest;

    // A hint for the next candidate request index. Request indicies are ints between 0 and 2^16-1
    // which are encoded into the upper 16 bits of the requestCode for
    // FragTest.startActivityForResult(...) calls. This allows us to dispatch onActivityResult(...)
    // to the appropriate FragTest. Request indicies are allocated by allocateRequestIndex(...).
    int mNextCandidateRequestIndex;
    // We need to keep track of whether startActivityForResult originated from a FragTest, so we
    // can conditionally check whether the requestCode collides with our reserved ID space for the
    // request index (see above). Unfortunately we can't just call
    // super.startActivityForResult(...) to bypass the check when the call didn't come from a
    // fragTest, since we need to use the ActivityCompat version for backward compatibility.
    boolean mStartedActivityFromFragTest;
    // A map from request index to FragTest "who" (i.e. a FragTest's unique identifier). Used to
    // keep track of the originating FragTest for FragTest.startActivityForResult(...) calls, so we
    // can dispatch the onActivityResult(...) to the appropriate FragTest. Will only contain entries
    // for startActivityForResult calls where a result has not yet been delivered.
    SparseArrayCompat<String> mPendingFragTestActivityResults;

    static final class NonConfigurationInstances {
        Object custom;
        List<FragTest> fragTests;
        SimpleArrayMap<String, LoaderManager> loaders;
    }

    final Handler mMainHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REALLY_STOPPED:
                    if (mStopped) {
                        doReallyStop(false);
                    }
                    break;
                case MSG_RESUME_PENDING:
                    onResumeFragTests();
                    mFragTests.execPendingActions();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    };

    final FragTestController mFragTests = FragTestController.createController(new HostCallbacks());


    // BaseFragTestActivityDonut
    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        final View v = dispatchFragTestsOnCreateView(null, name, context, attrs);
        if (v == null) {
            return super.onCreateView(name, context, attrs);
        }
        return v;
    }

    // BaseFragTestActivityHoneycomb
    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        final View v = dispatchFragTestsOnCreateView(parent, name, context, attrs);
        if (v == null && Build.VERSION.SDK_INT >= 11) {
            // If we're running on HC or above, let the super have a go
            return super.onCreateView(parent, name, context, attrs);
        }
        return v;
    }

    // BaseFragTestActivityDonut
    /**
     * Called when the back button has been pressed.and not handled by the support fragTest manager.
     */
    void onBackPressedNotHandled() {
        // on v4, just call finish manually
        finish();
    }

    // ------------------------------------------------------------------------
    // HOOKS INTO ACTIVITY
    // ------------------------------------------------------------------------

    /**
     * Dispatch incoming result to the correct fragTest.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mFragTests.noteStateNotSaved();
        int requestIndex = requestCode>>16;
        if (requestIndex != 0) {
            requestIndex--;

            String who = mPendingFragTestActivityResults.get(requestIndex);
            mPendingFragTestActivityResults.remove(requestIndex);
            if (who == null) {
                Log.w(TAG, "Activity result delivered for unknown FragTest.");
                return;
            }
            FragTest targetFragTest = mFragTests.findFragTestByWho(who);
            if (targetFragTest == null) {
                Log.w(TAG, "Activity result no fragTest exists for who: " + who);
            } else {
                targetFragTest.onActivityResult(requestCode&0xffff, resultCode, data);
            }
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Take care of popping the fragTest back stack or finishing the activity
     * as appropriate.
     */
    public void onBackPressed() {
        if (!mFragTests.getSupportFragTestManager().popBackStackImmediate()) {
            onBackPressedNotHandled();
        }
    }

    public void supportFinishAfterTransition() {
        ActivityCompat.finishAfterTransition(this);
    }

    public void setEnterSharedElementCallback(SharedElementCallback callback) {
        ActivityCompat.setEnterSharedElementCallback(this, callback);
    }

    public void setExitSharedElementCallback(SharedElementCallback listener) {
        ActivityCompat.setExitSharedElementCallback(this, listener);
    }

    /**
     * Support library version of {@link Activity#postponeEnterTransition()} that works
     * only on API 21 and later.
     */
    public void supportPostponeEnterTransition() {
        ActivityCompat.postponeEnterTransition(this);
    }

    /**
     * Support library version of {@link Activity#startPostponedEnterTransition()}
     * that only works with API 21 and later.
     */
    public void supportStartPostponedEnterTransition() {
        ActivityCompat.startPostponedEnterTransition(this);
    }

    /**
     * Dispatch configuration change to all fragTests.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mFragTests.dispatchConfigurationChanged(newConfig);
    }

    /**
     * Perform initialization of all fragTests and loaders.
     */
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // 首先初始化 fragtest 的 host
        mFragTests.attachHost(null /*parent*/);

        super.onCreate(savedInstanceState);

        FragTestActivity.NonConfigurationInstances nc =
                (FragTestActivity.NonConfigurationInstances) getLastNonConfigurationInstance();
        if (nc != null) {
            mFragTests.restoreLoaderNonConfig(nc.loaders);
        }
        if (savedInstanceState != null) {
            Parcelable p = savedInstanceState.getParcelable(FRAGTESTS_TAG);
            mFragTests.restoreAllState(p, nc != null ? nc.fragTests : null);

            // Check if there are any pending onActivityResult calls to descendent FragTests.
            if (savedInstanceState.containsKey(NEXT_CANDIDATE_REQUEST_INDEX_TAG)) {
                mNextCandidateRequestIndex =
                        savedInstanceState.getInt(NEXT_CANDIDATE_REQUEST_INDEX_TAG);
                int[] requestCodes = savedInstanceState.getIntArray(ALLOCATED_REQUEST_INDICIES_TAG);
                String[] fragTestWhos = savedInstanceState.getStringArray(REQUEST_FRAGMENT_WHO_TAG);
                if (requestCodes == null || fragTestWhos == null ||
                        requestCodes.length != fragTestWhos.length) {
                    Log.w(TAG, "Invalid requestCode mapping in savedInstanceState.");
                } else {
                    mPendingFragTestActivityResults = new SparseArrayCompat<>(requestCodes.length);
                    for (int i = 0; i < requestCodes.length; i++) {
                        mPendingFragTestActivityResults.put(requestCodes[i], fragTestWhos[i]);
                    }
                }
            }
        }

        if (mPendingFragTestActivityResults == null) {
            mPendingFragTestActivityResults = new SparseArrayCompat<>();
            mNextCandidateRequestIndex = 0;
        }

        mFragTests.dispatchCreate();
    }

//    @Override
    final View dispatchFragTestsOnCreateView(View parent, String name, Context context,
                                             AttributeSet attrs) {
        return mFragTests.onCreateView(parent, name, context, attrs);
    }

    /**
     * Destroy all fragTests and loaders.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        doReallyStop(false);

        mFragTests.dispatchDestroy();
        mFragTests.doLoaderDestroy();
    }

    /**
     * Take care of calling onBackPressed() for pre-Eclair platforms.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (Build.VERSION.SDK_INT < 5 /* ECLAIR */
                && keyCode == KeyEvent.KEYCODE_BACK
                && event.getRepeatCount() == 0) {
            // Take care of calling this method on earlier versions of
            // the platform where it doesn't exist.
            onBackPressed();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    /**
     * Dispatch onLowMemory() to all fragTests.
     */
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mFragTests.dispatchLowMemory();
    }

    /**
     * Dispatch onPause() to fragTests.
     */
    @Override
    protected void onPause() {
        super.onPause();
        mResumed = false;
        if (mMainHandler.hasMessages(MSG_RESUME_PENDING)) {
            mMainHandler.removeMessages(MSG_RESUME_PENDING);
            onResumeFragTests();
        }
        mFragTests.dispatchPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mFragTests.noteStateNotSaved();
    }

    /**
     * Hook in to note that fragTest state is no longer saved.
     */
    public void onStateNotSaved() {
        mFragTests.noteStateNotSaved();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMainHandler.sendEmptyMessage(MSG_RESUME_PENDING);
        mResumed = true;
        mFragTests.execPendingActions();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        mMainHandler.removeMessages(MSG_RESUME_PENDING);
        onResumeFragTests();
        mFragTests.execPendingActions();
    }

    protected void onResumeFragTests() {
        mFragTests.dispatchResume();
    }

    /**
     * Dispatch onPrepareOptionsMenu() to fragTests.
     */
    @Override
    public boolean onPreparePanel(int featureId, View view, Menu menu) {
        if (featureId == Window.FEATURE_OPTIONS_PANEL && menu != null) {
            if (mOptionsMenuInvalidated) {
                mOptionsMenuInvalidated = false;
                menu.clear();
                onCreatePanelMenu(featureId, menu);
            }
            boolean goforit = onPrepareOptionsPanel(view, menu);
            goforit |= mFragTests.dispatchPrepareOptionsMenu(menu);
            return goforit;
        }
        return super.onPreparePanel(featureId, view, menu);
    }

    /**
     * @hide
     */
    protected boolean onPrepareOptionsPanel(View view, Menu menu) {
        return super.onPreparePanel(Window.FEATURE_OPTIONS_PANEL, view, menu);
    }

    @Override
    public final Object onRetainNonConfigurationInstance() {
        if (mStopped) {
            doReallyStop(true);
        }

        Object custom = onRetainCustomNonConfigurationInstance();

        List<FragTest> fragTests = mFragTests.retainNonConfig();
        SimpleArrayMap<String, LoaderManager> loaders = mFragTests.retainLoaderNonConfig();

        if (fragTests == null && loaders == null && custom == null) {
            return null;
        }

        FragTestActivity.NonConfigurationInstances nci = new FragTestActivity.NonConfigurationInstances();
        nci.custom = custom;
        nci.fragTests = fragTests;
        nci.loaders = loaders;
        return nci;
    }

    /**
     * Save all appropriate fragTest state.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Parcelable p = mFragTests.saveAllState();
        if (p != null) {
            outState.putParcelable(FRAGTESTS_TAG, p);
        }
        if (mPendingFragTestActivityResults.size() > 0) {
            outState.putInt(NEXT_CANDIDATE_REQUEST_INDEX_TAG, mNextCandidateRequestIndex);

            int[] requestCodes = new int[mPendingFragTestActivityResults.size()];
            String[] fragTestWhos = new String[mPendingFragTestActivityResults.size()];
            for (int i = 0; i < mPendingFragTestActivityResults.size(); i++) {
                requestCodes[i] = mPendingFragTestActivityResults.keyAt(i);
                fragTestWhos[i] = mPendingFragTestActivityResults.valueAt(i);
            }
            outState.putIntArray(ALLOCATED_REQUEST_INDICIES_TAG, requestCodes);
            outState.putStringArray(REQUEST_FRAGMENT_WHO_TAG, fragTestWhos);
        }
    }

    /**
     * Dispatch onStart() to all fragTests.  Ensure any created loaders are
     * now started.
     */
    @Override
    protected void onStart() {
        super.onStart();

        mStopped = false;
        mReallyStopped = false;
        mMainHandler.removeMessages(MSG_REALLY_STOPPED);

        if (!mCreated) {
            mCreated = true;
            mFragTests.dispatchActivityCreated();
        }

        mFragTests.noteStateNotSaved();
        mFragTests.execPendingActions();

        mFragTests.doLoaderStart();

        // NOTE: HC onStart goes here.

        mFragTests.dispatchStart();
        mFragTests.reportLoaderStart();
    }

    /**
     * Dispatch onStop() to all fragTests.  Ensure all loaders are stopped.
     */
    @Override
    protected void onStop() {
        super.onStop();

        mStopped = true;
        mMainHandler.sendEmptyMessage(MSG_REALLY_STOPPED);

        mFragTests.dispatchStop();
    }


    // ------------------------------------------------------------------------
    // NEW METHODS
    // ------------------------------------------------------------------------

    /**
     * Use this instead of {@link #onRetainNonConfigurationInstance()}.
     * Retrieve later with {@link #getLastCustomNonConfigurationInstance()}.
     */
    public Object onRetainCustomNonConfigurationInstance() {
        return null;
    }

    /**
     * Return the value previously returned from
     * {@link #onRetainCustomNonConfigurationInstance()}.
     */
    @SuppressWarnings("deprecation")
    public Object getLastCustomNonConfigurationInstance() {
        FragTestActivity.NonConfigurationInstances nc = (FragTestActivity.NonConfigurationInstances)
                getLastNonConfigurationInstance();
        return nc != null ? nc.custom : null;
    }

    void doReallyStop(boolean retaining) {
        if (!mReallyStopped) {
            mReallyStopped = true;
            mRetaining = retaining;
            mMainHandler.removeMessages(MSG_REALLY_STOPPED);
            onReallyStop();
        }
    }

    /**
     * Pre-HC, we didn't have a way to determine whether an activity was
     * being stopped for a config change or not until we saw
     * onRetainNonConfigurationInstance() called after onStop().  However
     * we need to know this, to know whether to retain fragTests.  This will
     * tell us what we need to know.
     */
    void onReallyStop() {
        mFragTests.doLoaderStop(mRetaining);

        mFragTests.dispatchReallyStop();
    }

    // ------------------------------------------------------------------------
    // FRAGMENT SUPPORT
    // ------------------------------------------------------------------------

    /**
     * Called when a fragTest is attached to the activity.
     */
    @SuppressWarnings("unused")
    public void onAttachFragTest(FragTest fragTest) {
    }

    /**
     * Return the FragTestManager for interacting with fragTests associated
     * with this activity.
     */
    public FragTestManager getSupportFragTestManager() {
        return mFragTests.getSupportFragTestManager();
    }

    public LoaderManager getSupportLoaderManager() {
        return mFragTests.getSupportLoaderManager();
    }

////////////////////////////////////////////////////////////////////////////////////////
    // MARK：支持 fragment 启动 activity 并接收 activity 返回结果
////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Modifies the standard behavior to allow results to be delivered to fragTests.
     * This imposes a restriction that requestCode be <= 0xffff.
     */
    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        // If this was started from a FragTest we've already checked the upper 16 bits were not in
        // use, and then repurposed them for the FragTest's index.
        if (!mStartedActivityFromFragTest) {
            if (requestCode != -1 && (requestCode&0xffff0000) != 0) {
                throw new IllegalArgumentException("Can only use lower 16 bits for requestCode");
            }
        }
        super.startActivityForResult(intent, requestCode);
    }

    /**
     * Called by FragTest.startActivityForResult() to implement its behavior.
     */
    public void startActivityFromFragTest(FragTest fragTest, Intent intent,
                                          int requestCode) {
        startActivityFromFragTest(fragTest, intent, requestCode, null);
    }

    /**
     * Called by FragTest.startActivityForResult() to implement its behavior.
     */
    public void startActivityFromFragTest(FragTest fragTest, Intent intent,
                                          int requestCode, @Nullable Bundle options) {
        mStartedActivityFromFragTest = true;
        try {
            if (requestCode == -1) {
                ActivityCompat.startActivityForResult(this, intent, -1, options);
                return;
            }
            if ((requestCode&0xffff0000) != 0) {
                throw new IllegalArgumentException("Can only use lower 16 bits for requestCode");
            }
            int requestIndex = allocateRequestIndex(fragTest);
            startActivityForResult(intent, ((requestIndex+1)<<16) + (requestCode&0xffff), options);
        } finally {
            mStartedActivityFromFragTest = false;
        }
    }

    /**
     * Callback for the result from requesting permissions. This method
     * is invoked for every call on {@link #requestPermissions(String[], int)}.
     * <p>
     * <strong>Note:</strong> It is possible that the permissions request interaction
     * with the user is interrupted. In this case you will receive empty permissions
     * and results arrays which should be treated as a cancellation.
     * </p>
     *
     * @param requestCode The request code passed in {@link #requestPermissions(String[], int)}.
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either {@link android.content.pm.PackageManager#PERMISSION_GRANTED}
     *     or {@link android.content.pm.PackageManager#PERMISSION_DENIED}. Never null.
     *
     * @see #requestPermissions(String[], int)
     */
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        int index = (requestCode>>16)&0xffff;
        if (index != 0) {
            index--;

            String who = mPendingFragTestActivityResults.get(index);
            mPendingFragTestActivityResults.remove(index);
            if (who == null) {
                Log.w(TAG, "Activity result delivered for unknown FragTest.");
                return;
            }
            FragTest frag = mFragTests.findFragTestByWho(who);
            if (frag == null) {
                Log.w(TAG, "Activity result no fragTest exists for who: " + who);
            } else {
                frag.onRequestPermissionsResult(requestCode&0xffff, permissions, grantResults);
            }
        }
    }

    // Allocates the next available startActivityForResult request index.
    private int allocateRequestIndex(FragTest fragTest) {
        // Sanity check that we havn't exhaused the request index space.
        if (mPendingFragTestActivityResults.size() >= MAX_NUM_PENDING_FRAGMENT_ACTIVITY_RESULTS) {
            throw new IllegalStateException("Too many pending FragTest activity results.");
        }

        // Find an unallocated request index in the mPendingFragTestActivityResults map.
        while (mPendingFragTestActivityResults.indexOfKey(mNextCandidateRequestIndex) >= 0) {
            mNextCandidateRequestIndex =
                    (mNextCandidateRequestIndex + 1) % MAX_NUM_PENDING_FRAGMENT_ACTIVITY_RESULTS;
        }

        int requestIndex = mNextCandidateRequestIndex;
        mPendingFragTestActivityResults.put(requestIndex, fragTest.mWho);
        mNextCandidateRequestIndex =
                (mNextCandidateRequestIndex + 1) % MAX_NUM_PENDING_FRAGMENT_ACTIVITY_RESULTS;

        return requestIndex;
    }

    /**
     * Called by FragTest.requestPermissions() to implement its behavior.
     */
    private void requestPermissionsFromFragTest(FragTest fragTest, String[] permissions,
                                                int requestCode) {
        if (requestCode == -1) {
            ActivityCompat.requestPermissions(this, permissions, requestCode);
            return;
        }
        if ((requestCode&0xffff0000) != 0) {
            throw new IllegalArgumentException("Can only use lower 16 bits for requestCode");
        }
        try {
            mRequestedPermissionsFromFragTest = true;
            int requestIndex = allocateRequestIndex(fragTest);
            ActivityCompat.requestPermissions(this, permissions,
                    ((requestIndex + 1) << 16) + (requestCode & 0xffff));
        } finally {
            mRequestedPermissionsFromFragTest = false;
        }
    }


    //////////////////////////////////////////////////////////////////////////////////////
    // MARK: inner class HostCallbacks
//////////////////////////////////////////////////////////////////////////////////////
    class HostCallbacks extends FragTestHostCallback<FragTestActivity> {
        public HostCallbacks() {
            super(FragTestActivity.this);
        }

        @Override
        public void onDump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
            FragTestActivity.this.dump(prefix, fd, writer, args);
        }

        @Override
        public boolean onShouldSaveFragTestState(FragTest fragTest) {
            return !isFinishing();
        }

        @Override
        public LayoutInflater onGetLayoutInflater() {
            return FragTestActivity.this.getLayoutInflater().cloneInContext(FragTestActivity.this);
        }

        @Override
        public FragTestActivity onGetHost() {
            return FragTestActivity.this;
        }

        @Override
        public void onSupportInvalidateOptionsMenu() {
        }

        @Override
        public void onStartActivityFromFragTest(FragTest fragTest, Intent intent, int requestCode) {
            FragTestActivity.this.startActivityFromFragTest(fragTest, intent, requestCode);
        }

        @Override
        public void onStartActivityFromFragTest(
                FragTest fragTest, Intent intent, int requestCode, Bundle options) {
            FragTestActivity.this.startActivityFromFragTest(fragTest, intent, requestCode, options);
        }

        @Override
        public void onRequestPermissionsFromFragTest(FragTest fragTest,
                                                     String[] permissions, int requestCode) {
            FragTestActivity.this.requestPermissionsFromFragTest(fragTest, permissions,
                    requestCode);
        }

        @Override
        public boolean onShouldShowRequestPermissionRationale(String permission) {
            return ActivityCompat.shouldShowRequestPermissionRationale(
                    FragTestActivity.this, permission);
        }

        @Override
        public boolean onHasWindowAnimations() {
            return getWindow() != null;
        }

        @Override
        public int onGetWindowAnimations() {
            final Window w = getWindow();
            return (w == null) ? 0 : w.getAttributes().windowAnimations;
        }

        @Override
        public void onAttachFragTest(FragTest fragTest) {
            FragTestActivity.this.onAttachFragTest(fragTest);
        }

        @Override
        public View onFindViewById(int id) {
            return FragTestActivity.this.findViewById(id);
        }

        @Override
        public boolean onHasView() {
            final Window w = getWindow();
            return (w != null && w.peekDecorView() != null);
        }
    }
}
