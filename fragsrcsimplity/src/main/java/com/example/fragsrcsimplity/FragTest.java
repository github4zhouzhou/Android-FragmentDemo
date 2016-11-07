package com.example.fragsrcsimplity;

import android.app.Activity;
import android.content.ComponentCallbacks;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.FragmentContainer;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.util.DebugUtils;
import android.support.v4.util.SimpleArrayMap;
import android.support.v4.view.LayoutInflaterCompat;
import android.util.AndroidRuntimeException;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;

import java.io.FileDescriptor;
import java.io.PrintWriter;

/**
 * Created by zhouzhou on 2016/11/6.
 */

final class FragTestState implements Parcelable {
    final String mClassName;
    final int mIndex;
    final boolean mFromLayout;
    final int mFragTestId;
    final int mContainerId;
    final String mTag;
    final boolean mRetainInstance;
    final boolean mDetached;
    final Bundle mArguments;

    Bundle mSavedFragTestState;

    FragTest mInstance;

    public FragTestState(FragTest frag) {
        mClassName = frag.getClass().getName();
        mIndex = frag.mIndex;
        mFromLayout = frag.mFromLayout;
        mFragTestId = frag.mFragTestId;
        mContainerId = frag.mContainerId;
        mTag = frag.mTag;
        mRetainInstance = frag.mRetainInstance;
        mDetached = frag.mDetached;
        mArguments = frag.mArguments;
    }

    public FragTestState(Parcel in) {
        mClassName = in.readString();
        mIndex = in.readInt();
        mFromLayout = in.readInt() != 0;
        mFragTestId = in.readInt();
        mContainerId = in.readInt();
        mTag = in.readString();
        mRetainInstance = in.readInt() != 0;
        mDetached = in.readInt() != 0;
        mArguments = in.readBundle();
        mSavedFragTestState = in.readBundle();
    }

    public FragTest instantiate(FragTestHostCallback host, FragTest parent) {
        if (mInstance != null) {
            return mInstance;
        }

        final Context context = host.getContext();
        if (mArguments != null) {
            mArguments.setClassLoader(context.getClassLoader());
        }

        mInstance = FragTest.instantiate(context, mClassName, mArguments);

        if (mSavedFragTestState != null) {
            mSavedFragTestState.setClassLoader(context.getClassLoader());
            mInstance.mSavedFragTestState = mSavedFragTestState;
        }
        mInstance.setIndex(mIndex, parent);
        mInstance.mFromLayout = mFromLayout;
        mInstance.mRestored = true;
        mInstance.mFragTestId = mFragTestId;
        mInstance.mContainerId = mContainerId;
        mInstance.mTag = mTag;
        mInstance.mRetainInstance = mRetainInstance;
        mInstance.mDetached = mDetached;
        mInstance.mFragTestManager = host.mFragTestManager;

        if (FragTestManagerImpl.DEBUG) Log.v(FragTestManagerImpl.TAG,
                "Instantiated FragTest " + mInstance);

        return mInstance;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mClassName);
        dest.writeInt(mIndex);
        dest.writeInt(mFromLayout ? 1 : 0);
        dest.writeInt(mFragTestId);
        dest.writeInt(mContainerId);
        dest.writeString(mTag);
        dest.writeInt(mRetainInstance ? 1 : 0);
        dest.writeInt(mDetached ? 1 : 0);
        dest.writeBundle(mArguments);
        dest.writeBundle(mSavedFragTestState);
    }

    public static final Parcelable.Creator<FragTestState> CREATOR
            = new Parcelable.Creator<FragTestState>() {
        public FragTestState createFromParcel(Parcel in) {
            return new FragTestState(in);
        }

        public FragTestState[] newArray(int size) {
            return new FragTestState[size];
        }
    };
}

public class FragTest implements ComponentCallbacks, View.OnCreateContextMenuListener {
    private static final SimpleArrayMap<String, Class<?>> sClassMap =
            new SimpleArrayMap<String, Class<?>>();

    static final Object USE_DEFAULT_TRANSITION = new Object();

    static final int INITIALIZING = 0;     // Not yet created.
    static final int CREATED = 1;          // Created.
    static final int ACTIVITY_CREATED = 2; // The activity has finished its creation.
    static final int STOPPED = 3;          // Fully created, not started.
    static final int STARTED = 4;          // Created and started, not resumed.
    static final int RESUMED = 5;          // Created started and resumed.

    int mState = INITIALIZING;

    // Non-null if the FragTest's view hierarchy is currently animating away,
    // meaning we need to wait a bit on completely destroying it.  This is the
    // view that is animating.
    View mAnimatingAway;

    // If mAnimatingAway != null, this is the state we should move to once the
    // animation is done.
    int mStateAfterAnimating;

    // When instantiated from saved state, this is the saved state.
    Bundle mSavedFragTestState;
    SparseArray<Parcelable> mSavedViewState;

    // Index into active FragTest array.
    int mIndex = -1;

    // Internal unique name for this FragTest;
    String mWho;

    // Construction arguments;
    Bundle mArguments;

    // Target FragTest.
    FragTest mTarget;

    // For use when retaining a FragTest: this is the index of the last mTarget.
    int mTargetIndex = -1;

    // Target request code.
    int mTargetRequestCode;

    // True if the FragTest is in the list of added FragTests.
    boolean mAdded;

    // If set this FragTest is being removed from its activity.
    boolean mRemoving;

    // Set to true if this FragTest was instantiated from a layout file.
    boolean mFromLayout;

    // Set to true when the view has actually been inflated in its layout.
    boolean mInLayout;

    // True if this FragTest has been restored from previously saved state.
    boolean mRestored;

    // Number of active back stack entries this FragTest is in.
    int mBackStackNesting;

    // The FragTest manager we are associated with.  Set as soon as the
    // FragTest is used in a transaction; cleared after it has been removed
    // from all transactions.
    FragTestManagerImpl mFragTestManager;

    // Host this FragTest is attached to.
    FragTestHostCallback mHost;

    // Private FragTest manager for child FragTests inside of this one.
    FragTestManagerImpl mChildFragTestManager;

    // If this FragTest is contained in another FragTest, this is that container.
    FragTest mParentFragTest;

    // The optional identifier for this FragTest -- either the container ID if it
    // was dynamically added to the view hierarchy, or the ID supplied in
    // layout.
    int mFragTestId;

    // When a FragTest is being dynamically added to the view hierarchy, this
    // is the identifier of the parent container it is being added to.
    int mContainerId;

    // The optional named tag for this FragTest -- usually used to find
    // FragTests that are not part of the layout.
    String mTag;

    // Set to true when the app has requested that this FragTest be hidden
    // from the user.
    boolean mHidden;

    // Set to true when the app has requested that this FragTest be deactivated.
    boolean mDetached;

    // If set this FragTest would like its instance retained across
    // configuration changes.
    boolean mRetainInstance;

    // If set this FragTest is being retained across the current config change.
    boolean mRetaining;

    // If set this FragTest has menu items to contribute.
    boolean mHasMenu;

    // Set to true to allow the FragTest's menu to be shown.
    boolean mMenuVisible = true;

    // Used to verify that subclasses call through to super class.
    boolean mCalled;

    // If app has requested a specific animation, this is the one to use.
    int mNextAnim;

    // The parent container of the FragTest after dynamically added to UI.
    ViewGroup mContainer;

    // The View generated for this FragTest.
    View mView;

    // The real inner view that will save/restore state.
    View mInnerView;

    // Whether this FragTest should defer starting until after other FragTests
    // have been started and their loaders are finished.
    boolean mDeferStart;

    // Hint provided by the app that this FragTest is currently visible to the user.
    boolean mUserVisibleHint = true;

    LoaderManagerImpl mLoaderManager;
    boolean mLoadersStarted;
    boolean mCheckedForLoaderManager;

    Object mEnterTransition = null;
    Object mReturnTransition = USE_DEFAULT_TRANSITION;
    Object mExitTransition = null;
    Object mReenterTransition = USE_DEFAULT_TRANSITION;
    Object mSharedElementEnterTransition = null;
    Object mSharedElementReturnTransition = USE_DEFAULT_TRANSITION;
    Boolean mAllowReturnTransitionOverlap;
    Boolean mAllowEnterTransitionOverlap;

    SharedElementCallback mEnterTransitionCallback = null;
    SharedElementCallback mExitTransitionCallback = null;

    public static class SavedState implements Parcelable {
        final Bundle mState;

        SavedState(Bundle state) {
            mState = state;
        }

        SavedState(Parcel in, ClassLoader loader) {
            mState = in.readBundle();
            if (loader != null && mState != null) {
                mState.setClassLoader(loader);
            }
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeBundle(mState);
        }

        public static final Parcelable.Creator<FragTest.SavedState> CREATOR
                = new Parcelable.Creator<FragTest.SavedState>() {
            @Override
            public FragTest.SavedState createFromParcel(Parcel in) {
                return new FragTest.SavedState(in, null);
            }

            @Override
            public FragTest.SavedState[] newArray(int size) {
                return new FragTest.SavedState[size];
            }
        };
    }

    static public class InstantiationException extends RuntimeException {
        public InstantiationException(String msg, Exception cause) {
            super(msg, cause);
        }
    }

    public FragTest() {
    }

    public static FragTest instantiate(Context context, String fname) {
        return instantiate(context, fname, null);
    }

    public static FragTest instantiate(Context context, String fname, @Nullable Bundle args) {
        try {
            Class<?> clazz = sClassMap.get(fname);
            if (clazz == null) {
                // Class not found in the cache, see if it's real, and try to add it
                clazz = context.getClassLoader().loadClass(fname);
                sClassMap.put(fname, clazz);
            }
            FragTest f = (FragTest)clazz.newInstance();
            if (args != null) {
                args.setClassLoader(f.getClass().getClassLoader());
                f.mArguments = args;
            }
            return f;
        } catch (ClassNotFoundException e) {
            throw new FragTest.InstantiationException("Unable to instantiate FragTest " + fname
                    + ": make sure class name exists, is public, and has an"
                    + " empty constructor that is public", e);
        } catch (java.lang.InstantiationException e) {
            throw new FragTest.InstantiationException("Unable to instantiate FragTest " + fname
                    + ": make sure class name exists, is public, and has an"
                    + " empty constructor that is public", e);
        } catch (IllegalAccessException e) {
            throw new FragTest.InstantiationException("Unable to instantiate FragTest " + fname
                    + ": make sure class name exists, is public, and has an"
                    + " empty constructor that is public", e);
        }
    }

    static boolean isSupportFragTestClass(Context context, String fname) {
        try {
            Class<?> clazz = sClassMap.get(fname);
            if (clazz == null) {
                // Class not found in the cache, see if it's real, and try to add it
                clazz = context.getClassLoader().loadClass(fname);
                sClassMap.put(fname, clazz);
            }
            return FragTest.class.isAssignableFrom(clazz);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    final void restoreViewState(Bundle savedInstanceState) {
        if (mSavedViewState != null) {
            mInnerView.restoreHierarchyState(mSavedViewState);
            mSavedViewState = null;
        }
        mCalled = false;
        onViewStateRestored(savedInstanceState);
        if (!mCalled) {
            throw new AndroidRuntimeException("FragTest " + this
                    + " did not call through to super.onViewStateRestored()");
        }
    }

    final void setIndex(int index, FragTest parent) {
        mIndex = index;
        if (parent != null) {
            mWho = parent.mWho + ":" + mIndex;
        } else {
            mWho = "android:FragTest:" + mIndex;
        }
    }

    final boolean isInBackStack() {
        return mBackStackNesting > 0;
    }

    /**
     * Subclasses can not override equals().
     */
    @Override final public boolean equals(Object o) {
        return super.equals(o);
    }

    /**
     * Subclasses can not override hashCode().
     */
    @Override final public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        DebugUtils.buildShortClassTag(this, sb);
        if (mIndex >= 0) {
            sb.append(" #");
            sb.append(mIndex);
        }
        if (mFragTestId != 0) {
            sb.append(" id=0x");
            sb.append(Integer.toHexString(mFragTestId));
        }
        if (mTag != null) {
            sb.append(" ");
            sb.append(mTag);
        }
        sb.append('}');
        return sb.toString();
    }

    /**
     * Return the identifier this FragTest is known by.  This is either
     * the android:id value supplied in a layout or the container view ID
     * supplied when adding the FragTest.
     */
    final public int getId() {
        return mFragTestId;
    }

    /**
     * Get the tag name of the FragTest, if specified.
     */
    final public String getTag() {
        return mTag;
    }

    /**
     * Supply the construction arguments for this FragTest.  This can only
     * be called before the FragTest has been attached to its activity; that
     * is, you should call it immediately after constructing the FragTest.  The
     * arguments supplied here will be retained across FragTest destroy and
     * creation.
     */
    public void setArguments(Bundle args) {
        if (mIndex >= 0) {
            throw new IllegalStateException("FragTest already active");
        }
        mArguments = args;
    }

    /**
     * Return the arguments supplied when the FragTest was instantiated,
     * if any.
     */
    final public Bundle getArguments() {
        return mArguments;
    }

    public void setInitialSavedState(FragTest.SavedState state) {
        if (mIndex >= 0) {
            throw new IllegalStateException("FragTest already active");
        }
        mSavedFragTestState = state != null && state.mState != null
                ? state.mState : null;
    }

    /**
     * Optional target for this FragTest.  This may be used, for example,
     * if this FragTest is being started by another, and when done wants to
     * give a result back to the first.  The target set here is retained
     * across instances via {@link FragTestManager#putFragTest
     * FragTestManager.putFragTest()}.
     *
     * @param FragTest The FragTest that is the target of this one.
     * @param requestCode Optional request code, for convenience if you
     * are going to call back with {@link #onActivityResult(int, int, Intent)}.
     */
    public void setTargetFragTest(FragTest FragTest, int requestCode) {
        mTarget = FragTest;
        mTargetRequestCode = requestCode;
    }

    /**
     * Return the target FragTest set by {@link #setTargetFragTest}.
     */
    final public FragTest getTargetFragTest() {
        return mTarget;
    }

    /**
     * Return the target request code set by {@link #setTargetFragTest}.
     */
    final public int getTargetRequestCode() {
        return mTargetRequestCode;
    }

    /**
     * Return the {@link Context} this FragTest is currently associated with.
     */
    public Context getContext() {
        return mHost == null ? null : mHost.getContext();
    }

    /**
     * Return the {@link FragTestActivity} this FragTest is currently associated with.
     * May return {@code null} if the FragTest is associated with a {@link Context}
     * instead.
     */
    final public FragTestActivity getActivity() {
        return mHost == null ? null : (FragTestActivity) mHost.getActivity();
    }

    /**
     * Return the host object of this FragTest. May return {@code null} if the FragTest
     * isn't currently being hosted.
     */
    final public Object getHost() {
        return mHost == null ? null : mHost.onGetHost();
    }

    /**
     * Return <code>getActivity().getResources()</code>.
     */
    final public Resources getResources() {
        if (mHost == null) {
            throw new IllegalStateException("FragTest " + this + " not attached to Activity");
        }
        return mHost.getContext().getResources();
    }

    /**
     * Return a localized, styled CharSequence from the application's package's
     * default string table.
     *
     * @param resId Resource id for the CharSequence text
     */
    public final CharSequence getText(@StringRes int resId) {
        return getResources().getText(resId);
    }

    /**
     * Return a localized string from the application's package's
     * default string table.
     *
     * @param resId Resource id for the string
     */
    public final String getString(@StringRes int resId) {
        return getResources().getString(resId);
    }

    /**
     * Return a localized formatted string from the application's package's
     * default string table, substituting the format arguments as defined in
     * {@link java.util.Formatter} and {@link java.lang.String#format}.
     *
     * @param resId Resource id for the format string
     * @param formatArgs The format arguments that will be used for substitution.
     */

    public final String getString(@StringRes int resId, Object... formatArgs) {
        return getResources().getString(resId, formatArgs);
    }

    final public FragTestManager getFragTestManager() {
        return mFragTestManager;
    }

    /**
     * Return a private FragTestManager for placing and managing FragTests
     * inside of this FragTest.
     */
    final public FragTestManager getChildFragTestManager() {
        if (mChildFragTestManager == null) {
            instantiateChildFragTestManager();
            if (mState >= RESUMED) {
                mChildFragTestManager.dispatchResume();
            } else if (mState >= STARTED) {
                mChildFragTestManager.dispatchStart();
            } else if (mState >= ACTIVITY_CREATED) {
                mChildFragTestManager.dispatchActivityCreated();
            } else if (mState >= CREATED) {
                mChildFragTestManager.dispatchCreate();
            }
        }
        return mChildFragTestManager;
    }

    /**
     * Returns the parent FragTest containing this FragTest.  If this FragTest
     * is attached directly to an Activity, returns null.
     */
    final public FragTest getParentFragTest() {
        return mParentFragTest;
    }

    /**
     * Return true if the FragTest is currently added to its activity.
     */
    final public boolean isAdded() {
        return mHost != null && mAdded;
    }

    final public boolean isDetached() {
        return mDetached;
    }

    /**
     * Return true if this FragTest is currently being removed from its
     * activity.  This is  <em>not</em> whether its activity is finishing, but
     * rather whether it is in the process of being removed from its activity.
     */
    final public boolean isRemoving() {
        return mRemoving;
    }

    /**
     * Return true if the layout is included as part of an activity view
     * hierarchy via the &lt;FragTest&gt; tag.  This will always be true when
     * FragTests are created through the &lt;FragTest&gt; tag, <em>except</em>
     * in the case where an old FragTest is restored from a previous state and
     * it does not appear in the layout of the current state.
     */
    final public boolean isInLayout() {
        return mInLayout;
    }

    /**
     * Return true if the FragTest is in the resumed state.  This is true
     * for the duration of {@link #onResume()} and {@link #onPause()} as well.
     */
    final public boolean isResumed() {
        return mState >= RESUMED;
    }

    /**
     * Return true if the FragTest is currently visible to the user.  This means
     * it: (1) has been added, (2) has its view attached to the window, and
     * (3) is not hidden.
     */
    final public boolean isVisible() {
        return isAdded() && !isHidden() && mView != null
                && mView.getWindowToken() != null && mView.getVisibility() == View.VISIBLE;
    }

    /**
     * Return true if the FragTest has been hidden.  By default FragTests
     * are shown.  You can find out about changes to this state with
     * {@link #onHiddenChanged}.  Note that the hidden state is orthogonal
     * to other states -- that is, to be visible to the user, a FragTest
     * must be both started and not hidden.
     */
    final public boolean isHidden() {
        return mHidden;
    }

    /** @hide */
    final public boolean hasOptionsMenu() {
        return mHasMenu;
    }

    /** @hide */
    final public boolean isMenuVisible() {
        return mMenuVisible;
    }

    /**
     * Called when the hidden state (as returned by {@link #isHidden()} of
     * the FragTest has changed.  FragTests start out not hidden; this will
     * be called whenever the FragTest changes state from that.
     * @param hidden True if the FragTest is now hidden, false if it is not
     * visible.
     */
    public void onHiddenChanged(boolean hidden) {
    }

    public void setRetainInstance(boolean retain) {
        if (retain && mParentFragTest != null) {
            throw new IllegalStateException(
                    "Can't retain fragements that are nested in other FragTests");
        }
        mRetainInstance = retain;
    }

    final public boolean getRetainInstance() {
        return mRetainInstance;
    }

    /**
     * Report that this FragTest would like to participate in populating
     * the options menu by receiving a call to {@link #onCreateOptionsMenu}
     * and related methods.
     *
     * @param hasMenu If true, the FragTest has menu items to contribute.
     */
    public void setHasOptionsMenu(boolean hasMenu) {
        if (mHasMenu != hasMenu) {
            mHasMenu = hasMenu;
            if (isAdded() && !isHidden()) {
                mHost.onSupportInvalidateOptionsMenu();
            }
        }
    }

    public void setMenuVisibility(boolean menuVisible) {
        if (mMenuVisible != menuVisible) {
            mMenuVisible = menuVisible;
            if (mHasMenu && isAdded() && !isHidden()) {
                mHost.onSupportInvalidateOptionsMenu();
            }
        }
    }

    public void setUserVisibleHint(boolean isVisibleToUser) {
        if (!mUserVisibleHint && isVisibleToUser && mState < STARTED) {
            mFragTestManager.performPendingDeferredStart(this);
        }
        mUserVisibleHint = isVisibleToUser;
        mDeferStart = !isVisibleToUser;
    }

    /**
     * @return The current value of the user-visible hint on this FragTest.
     * @see #setUserVisibleHint(boolean)
     */
    public boolean getUserVisibleHint() {
        return mUserVisibleHint;
    }

    /**
     * Return the LoaderManager for this FragTest, creating it if needed.
     */
    public LoaderManager getLoaderManager() {
        if (mLoaderManager != null) {
            return mLoaderManager;
        }
        if (mHost == null) {
            throw new IllegalStateException("FragTest " + this + " not attached to Activity");
        }
        mCheckedForLoaderManager = true;
        mLoaderManager = mHost.getLoaderManager(mWho, mLoadersStarted, true);
        return mLoaderManager;
    }

    /**
     * Call {@link Activity#startActivity(Intent)} from the FragTest's
     * containing Activity.
     */
    public void startActivity(Intent intent) {
        startActivity(intent, null);
    }

    /**
     * Call {@link Activity#startActivity(Intent, Bundle)} from the FragTest's
     * containing Activity.
     */
    public void startActivity(Intent intent, @Nullable Bundle options) {
        if (mHost == null) {
            throw new IllegalStateException("FragTest " + this + " not attached to Activity");
        }
        mHost.onStartActivityFromFragTest(this /*FragTest*/, intent, -1, options);
    }

    /**
     * Call {@link Activity#startActivityForResult(Intent, int)} from the FragTest's
     * containing Activity.
     */
    public void startActivityForResult(Intent intent, int requestCode) {
        startActivityForResult(intent, requestCode, null);
    }

    /**
     * Call {@link Activity#startActivityForResult(Intent, int, Bundle)} from the FragTest's
     * containing Activity.
     */
    public void startActivityForResult(Intent intent, int requestCode, @Nullable Bundle options) {
        if (mHost == null) {
            throw new IllegalStateException("FragTest " + this + " not attached to Activity");
        }
        mHost.onStartActivityFromFragTest(this /*FragTest*/, intent, requestCode, options);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    }

    public final void requestPermissions(@NonNull String[] permissions, int requestCode) {
        if (mHost == null) {
            throw new IllegalStateException("FragTest " + this + " not attached to Activity");
        }
        mHost.onRequestPermissionsFromFragTest(this, permissions,requestCode);
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        /* callback - do nothing */
    }

    public boolean shouldShowRequestPermissionRationale(@NonNull String permission) {
        if (mHost != null) {
            return mHost.onShouldShowRequestPermissionRationale(permission);
        }
        return false;
    }

    /**
     * @hide Hack so that DialogFragTest can make its Dialog before creating
     * its views, and the view construction can use the dialog's context for
     * inflation.  Maybe this should become a public API. Note sure.
     */
    public LayoutInflater getLayoutInflater(Bundle savedInstanceState) {
        LayoutInflater result = mHost.onGetLayoutInflater();
        getChildFragTestManager(); // Init if needed; use raw implementation below.
        LayoutInflaterCompat.setFactory(result, mChildFragTestManager.getLayoutInflaterFactory());
        return result;
    }


    public void onInflate(Context context, AttributeSet attrs, Bundle savedInstanceState) {
        mCalled = true;
        final Activity hostActivity = mHost == null ? null : mHost.getActivity();
        if (hostActivity != null) {
            mCalled = false;
            onInflate(hostActivity, attrs, savedInstanceState);
        }
    }

    /**
     * Called when a FragTest is being created as part of a view layout
     * inflation, typically from setting the content view of an activity.
     * <p>Deprecated. See {@link #onInflate(Context, AttributeSet, Bundle)}.
     */
    @Deprecated
    public void onInflate(Activity activity, AttributeSet attrs, Bundle savedInstanceState) {
        mCalled = true;
    }

    /**
     * Called when a FragTest is first attached to its context.
     * {@link #onCreate(Bundle)} will be called after this.
     */
    public void onAttach(Context context) {
        mCalled = true;
        final Activity hostActivity = mHost == null ? null : mHost.getActivity();
        if (hostActivity != null) {
            mCalled = false;
            onAttach(hostActivity);
        }
    }

    /**
     * Called when a FragTest is first attached to its activity.
     * {@link #onCreate(Bundle)} will be called after this.
     * <p>Deprecated. See {@link #onAttach(Context)}.
     */
    @Deprecated
    public void onAttach(Activity activity) {
        mCalled = true;
    }

    /**
     * Called when a FragTest loads an animation.
     */
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        return null;
    }

    public void onCreate(@Nullable Bundle savedInstanceState) {
        mCalled = true;
    }


    @Nullable
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return null;
    }

    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    }

    /**
     * Get the root view for the FragTest's layout (the one returned by {@link #onCreateView}),
     * if provided.
     *
     * @return The FragTest's root view, or null if it has no layout.
     */
    @Nullable
    public View getView() {
        return mView;
    }


    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        mCalled = true;
    }

    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        mCalled = true;
    }

    public void onStart() {
        mCalled = true;

        if (!mLoadersStarted) {
            mLoadersStarted = true;
            if (!mCheckedForLoaderManager) {
                mCheckedForLoaderManager = true;
                mLoaderManager = mHost.getLoaderManager(mWho, mLoadersStarted, false);
            }
            if (mLoaderManager != null) {
                mLoaderManager.doStart();
            }
        }
    }

    public void onResume() {
        mCalled = true;
    }

    public void onSaveInstanceState(Bundle outState) {
    }

    public void onConfigurationChanged(Configuration newConfig) {
        mCalled = true;
    }

    /**
     * Called when the FragTest is no longer resumed.  This is generally
     * tied to {@link Activity#onPause() Activity.onPause} of the containing
     * Activity's lifecycle.
     */
    public void onPause() {
        mCalled = true;
    }

    /**
     * Called when the FragTest is no longer started.  This is generally
     * tied to {@link Activity#onStop() Activity.onStop} of the containing
     * Activity's lifecycle.
     */
    public void onStop() {
        mCalled = true;
    }

    public void onLowMemory() {
        mCalled = true;
    }

    public void onDestroyView() {
        mCalled = true;
    }

    /**
     * Called when the FragTest is no longer in use.  This is called
     * after {@link #onStop()} and before {@link #onDetach()}.
     */
    public void onDestroy() {
        mCalled = true;
        //Log.v("foo", "onDestroy: mCheckedForLoaderManager=" + mCheckedForLoaderManager
        //        + " mLoaderManager=" + mLoaderManager);
        if (!mCheckedForLoaderManager) {
            mCheckedForLoaderManager = true;
            mLoaderManager = mHost.getLoaderManager(mWho, mLoadersStarted, false);
        }
        if (mLoaderManager != null) {
            mLoaderManager.doDestroy();
        }
    }

    /**
     * Called by the FragTest manager once this FragTest has been removed,
     * so that we don't have any left-over state if the application decides
     * to re-use the instance.  This only clears state that the framework
     * internally manages, not things the application sets.
     */
    void initState() {
        mIndex = -1;
        mWho = null;
        mAdded = false;
        mRemoving = false;
        mFromLayout = false;
        mInLayout = false;
        mRestored = false;
        mBackStackNesting = 0;
        mFragTestManager = null;
        mChildFragTestManager = null;
        mHost = null;
        mFragTestId = 0;
        mContainerId = 0;
        mTag = null;
        mHidden = false;
        mDetached = false;
        mRetaining = false;
        mLoaderManager = null;
        mLoadersStarted = false;
        mCheckedForLoaderManager = false;
    }

    /**
     * Called when the FragTest is no longer attached to its activity.  This
     * is called after {@link #onDestroy()}.
     */
    public void onDetach() {
        mCalled = true;
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    }

    public void onPrepareOptionsMenu(Menu menu) {
    }

    public void onDestroyOptionsMenu() {
    }


    public boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

    /**
     * This hook is called whenever the options menu is being closed (either by the user canceling
     * the menu with the back/menu button, or when an item is selected).
     *
     * @param menu The options menu as last shown or first initialized by
     *             onCreateOptionsMenu().
     */
    public void onOptionsMenuClosed(Menu menu) {
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getActivity().onCreateContextMenu(menu, v, menuInfo);
    }

    public void registerForContextMenu(View view) {
        view.setOnCreateContextMenuListener(this);
    }

    /**
     * Prevents a context menu to be shown for the given view. This method will
     * remove the {@link View.OnCreateContextMenuListener} on the view.
     *
     * @see #registerForContextMenu(View)
     * @param view The view that should stop showing a context menu.
     */
    public void unregisterForContextMenu(View view) {
        view.setOnCreateContextMenuListener(null);
    }

    public boolean onContextItemSelected(MenuItem item) {
        return false;
    }

    public void setEnterSharedElementCallback(SharedElementCallback callback) {
        mEnterTransitionCallback = callback;
    }

    /**
     * When custom transitions are used with FragTests, the exit transition callback
     * is called when this FragTest is attached or detached when popping the back stack.
     *
     * @param callback Used to manipulate the shared element transitions on this FragTest
     *                 when added as a pop from the back stack.
     */
    public void setExitSharedElementCallback(SharedElementCallback callback) {
        mExitTransitionCallback = callback;
    }

    public void setEnterTransition(Object transition) {
        mEnterTransition = transition;
    }

    public Object getEnterTransition() {
        return mEnterTransition;
    }

    public void setReturnTransition(Object transition) {
        mReturnTransition = transition;
    }

    public Object getReturnTransition() {
        return mReturnTransition == USE_DEFAULT_TRANSITION ? getEnterTransition()
                : mReturnTransition;
    }

    public void setExitTransition(Object transition) {
        mExitTransition = transition;
    }

    public Object getExitTransition() {
        return mExitTransition;
    }

    public void setReenterTransition(Object transition) {
        mReenterTransition = transition;
    }

    public Object getReenterTransition() {
        return mReenterTransition == USE_DEFAULT_TRANSITION ? getExitTransition()
                : mReenterTransition;
    }

    public void setSharedElementEnterTransition(Object transition) {
        mSharedElementEnterTransition = transition;
    }

    public Object getSharedElementEnterTransition() {
        return mSharedElementEnterTransition;
    }


    public void setSharedElementReturnTransition(Object transition) {
        mSharedElementReturnTransition = transition;
    }

    public Object getSharedElementReturnTransition() {
        return mSharedElementReturnTransition == USE_DEFAULT_TRANSITION ?
                getSharedElementEnterTransition() : mSharedElementReturnTransition;
    }

    public void setAllowEnterTransitionOverlap(boolean allow) {
        mAllowEnterTransitionOverlap = allow;
    }

    public boolean getAllowEnterTransitionOverlap() {
        return (mAllowEnterTransitionOverlap == null) ? true : mAllowEnterTransitionOverlap;
    }

    public void setAllowReturnTransitionOverlap(boolean allow) {
        mAllowReturnTransitionOverlap = allow;
    }

    /**
     * Returns whether the the return transition and reenter transition overlap or not.
     * When true, the reenter transition will start as soon as possible. When false, the
     * reenter transition will wait until the return transition completes before starting.
     *
     * @return true to start the reenter transition when possible or false to wait until the
     *         return transition completes.
     */
    public boolean getAllowReturnTransitionOverlap() {
        return (mAllowReturnTransitionOverlap == null) ? true : mAllowReturnTransitionOverlap;
    }

    /**
     * Print the FragTests's state into the given stream.
     *
     * @param prefix Text to print at the front of each line.
     * @param fd The raw file descriptor that the dump is being sent to.
     * @param writer The PrintWriter to which you should dump your state.  This will be
     * closed for you after you return.
     * @param args additional arguments to the dump request.
     */
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        writer.print(prefix); writer.print("mFragTestId=#");
        writer.print(Integer.toHexString(mFragTestId));
        writer.print(" mContainerId=#");
        writer.print(Integer.toHexString(mContainerId));
        writer.print(" mTag="); writer.println(mTag);
        writer.print(prefix); writer.print("mState="); writer.print(mState);
        writer.print(" mIndex="); writer.print(mIndex);
        writer.print(" mWho="); writer.print(mWho);
        writer.print(" mBackStackNesting="); writer.println(mBackStackNesting);
        writer.print(prefix); writer.print("mAdded="); writer.print(mAdded);
        writer.print(" mRemoving="); writer.print(mRemoving);
        writer.print(" mFromLayout="); writer.print(mFromLayout);
        writer.print(" mInLayout="); writer.println(mInLayout);
        writer.print(prefix); writer.print("mHidden="); writer.print(mHidden);
        writer.print(" mDetached="); writer.print(mDetached);
        writer.print(" mMenuVisible="); writer.print(mMenuVisible);
        writer.print(" mHasMenu="); writer.println(mHasMenu);
        writer.print(prefix); writer.print("mRetainInstance="); writer.print(mRetainInstance);
        writer.print(" mRetaining="); writer.print(mRetaining);
        writer.print(" mUserVisibleHint="); writer.println(mUserVisibleHint);
        if (mFragTestManager != null) {
            writer.print(prefix); writer.print("mFragTestManager=");
            writer.println(mFragTestManager);
        }
        if (mHost != null) {
            writer.print(prefix); writer.print("mHost=");
            writer.println(mHost);
        }
        if (mParentFragTest != null) {
            writer.print(prefix); writer.print("mParentFragTest=");
            writer.println(mParentFragTest);
        }
        if (mArguments != null) {
            writer.print(prefix); writer.print("mArguments="); writer.println(mArguments);
        }
        if (mSavedFragTestState != null) {
            writer.print(prefix); writer.print("mSavedFragTestState=");
            writer.println(mSavedFragTestState);
        }
        if (mSavedViewState != null) {
            writer.print(prefix); writer.print("mSavedViewState=");
            writer.println(mSavedViewState);
        }
        if (mTarget != null) {
            writer.print(prefix); writer.print("mTarget="); writer.print(mTarget);
            writer.print(" mTargetRequestCode=");
            writer.println(mTargetRequestCode);
        }
        if (mNextAnim != 0) {
            writer.print(prefix); writer.print("mNextAnim="); writer.println(mNextAnim);
        }
        if (mContainer != null) {
            writer.print(prefix); writer.print("mContainer="); writer.println(mContainer);
        }
        if (mView != null) {
            writer.print(prefix); writer.print("mView="); writer.println(mView);
        }
        if (mInnerView != null) {
            writer.print(prefix); writer.print("mInnerView="); writer.println(mView);
        }
        if (mAnimatingAway != null) {
            writer.print(prefix); writer.print("mAnimatingAway="); writer.println(mAnimatingAway);
            writer.print(prefix); writer.print("mStateAfterAnimating=");
            writer.println(mStateAfterAnimating);
        }
        if (mLoaderManager != null) {
            writer.print(prefix); writer.println("Loader Manager:");
            mLoaderManager.dump(prefix + "  ", fd, writer, args);
        }
        if (mChildFragTestManager != null) {
            writer.print(prefix); writer.println("Child " + mChildFragTestManager + ":");
            mChildFragTestManager.dump(prefix + "  ", fd, writer, args);
        }
    }

    FragTest findFragTestByWho(String who) {
        if (who.equals(mWho)) {
            return this;
        }
        if (mChildFragTestManager != null) {
            return mChildFragTestManager.findFragTestByWho(who);
        }
        return null;
    }

    void instantiateChildFragTestManager() {
        mChildFragTestManager = new FragTestManagerImpl();
        mChildFragTestManager.attachController(mHost, new FragmentContainer() {
            @Override
            @Nullable
            public View onFindViewById(int id) {
                if (mView == null) {
                    throw new IllegalStateException("FragTest does not have a view");
                }
                return mView.findViewById(id);
            }

            @Override
            public boolean onHasView() {
                return (mView != null);
            }
        }, this);
    }

    void performCreate(Bundle savedInstanceState) {
        if (mChildFragTestManager != null) {
            mChildFragTestManager.noteStateNotSaved();
        }
        mState = CREATED;
        mCalled = false;
        onCreate(savedInstanceState);
        if (!mCalled) {
            throw new AndroidRuntimeException("FragTest " + this
                    + " did not call through to super.onCreate()");
        }
        if (savedInstanceState != null) {
            Parcelable p = savedInstanceState.getParcelable(
                    FragTestActivity.FRAGTESTS_TAG);
            if (p != null) {
                if (mChildFragTestManager == null) {
                    instantiateChildFragTestManager();
                }
                mChildFragTestManager.restoreAllState(p, null);
                mChildFragTestManager.dispatchCreate();
            }
        }
    }

    View performCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        if (mChildFragTestManager != null) {
            mChildFragTestManager.noteStateNotSaved();
        }
        return onCreateView(inflater, container, savedInstanceState);
    }

    void performActivityCreated(Bundle savedInstanceState) {
        if (mChildFragTestManager != null) {
            mChildFragTestManager.noteStateNotSaved();
        }
        mState = ACTIVITY_CREATED;
        mCalled = false;
        onActivityCreated(savedInstanceState);
        if (!mCalled) {
            throw new AndroidRuntimeException("FragTest " + this
                    + " did not call through to super.onActivityCreated()");
        }
        if (mChildFragTestManager != null) {
            mChildFragTestManager.dispatchActivityCreated();
        }
    }

    void performStart() {
        if (mChildFragTestManager != null) {
            mChildFragTestManager.noteStateNotSaved();
            mChildFragTestManager.execPendingActions();
        }
        mState = STARTED;
        mCalled = false;
        onStart();
        if (!mCalled) {
            throw new AndroidRuntimeException("FragTest " + this
                    + " did not call through to super.onStart()");
        }
        if (mChildFragTestManager != null) {
            mChildFragTestManager.dispatchStart();
        }
        if (mLoaderManager != null) {
            mLoaderManager.doReportStart();
        }
    }

    void performResume() {
        if (mChildFragTestManager != null) {
            mChildFragTestManager.noteStateNotSaved();
            mChildFragTestManager.execPendingActions();
        }
        mState = RESUMED;
        mCalled = false;
        onResume();
        if (!mCalled) {
            throw new AndroidRuntimeException("FragTest " + this
                    + " did not call through to super.onResume()");
        }
        if (mChildFragTestManager != null) {
            mChildFragTestManager.dispatchResume();
            mChildFragTestManager.execPendingActions();
        }
    }

    void performConfigurationChanged(Configuration newConfig) {
        onConfigurationChanged(newConfig);
        if (mChildFragTestManager != null) {
            mChildFragTestManager.dispatchConfigurationChanged(newConfig);
        }
    }

    void performLowMemory() {
        onLowMemory();
        if (mChildFragTestManager != null) {
            mChildFragTestManager.dispatchLowMemory();
        }
    }

    boolean performCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        boolean show = false;
        if (!mHidden) {
            if (mHasMenu && mMenuVisible) {
                show = true;
                onCreateOptionsMenu(menu, inflater);
            }
            if (mChildFragTestManager != null) {
                show |= mChildFragTestManager.dispatchCreateOptionsMenu(menu, inflater);
            }
        }
        return show;
    }

    boolean performPrepareOptionsMenu(Menu menu) {
        boolean show = false;
        if (!mHidden) {
            if (mHasMenu && mMenuVisible) {
                show = true;
                onPrepareOptionsMenu(menu);
            }
            if (mChildFragTestManager != null) {
                show |= mChildFragTestManager.dispatchPrepareOptionsMenu(menu);
            }
        }
        return show;
    }

    boolean performOptionsItemSelected(MenuItem item) {
        if (!mHidden) {
            if (mHasMenu && mMenuVisible) {
                if (onOptionsItemSelected(item)) {
                    return true;
                }
            }
            if (mChildFragTestManager != null) {
                if (mChildFragTestManager.dispatchOptionsItemSelected(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean performContextItemSelected(MenuItem item) {
        if (!mHidden) {
            if (onContextItemSelected(item)) {
                return true;
            }
            if (mChildFragTestManager != null) {
                if (mChildFragTestManager.dispatchContextItemSelected(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    void performOptionsMenuClosed(Menu menu) {
        if (!mHidden) {
            if (mHasMenu && mMenuVisible) {
                onOptionsMenuClosed(menu);
            }
            if (mChildFragTestManager != null) {
                mChildFragTestManager.dispatchOptionsMenuClosed(menu);
            }
        }
    }

    void performSaveInstanceState(Bundle outState) {
        onSaveInstanceState(outState);
        if (mChildFragTestManager != null) {
            Parcelable p = mChildFragTestManager.saveAllState();
            if (p != null) {
                outState.putParcelable(FragTestActivity.FRAGTESTS_TAG, p);
            }
        }
    }

    void performPause() {
        if (mChildFragTestManager != null) {
            mChildFragTestManager.dispatchPause();
        }
        mState = STARTED;
        mCalled = false;
        onPause();
        if (!mCalled) {
            throw new AndroidRuntimeException("FragTest " + this
                    + " did not call through to super.onPause()");
        }
    }

    void performStop() {
        if (mChildFragTestManager != null) {
            mChildFragTestManager.dispatchStop();
        }
        mState = STOPPED;
        mCalled = false;
        onStop();
        if (!mCalled) {
            throw new AndroidRuntimeException("FragTest " + this
                    + " did not call through to super.onStop()");
        }
    }

    void performReallyStop() {
        if (mChildFragTestManager != null) {
            mChildFragTestManager.dispatchReallyStop();
        }
        mState = ACTIVITY_CREATED;
        if (mLoadersStarted) {
            mLoadersStarted = false;
            if (!mCheckedForLoaderManager) {
                mCheckedForLoaderManager = true;
                mLoaderManager = mHost.getLoaderManager(mWho, mLoadersStarted, false);
            }
            if (mLoaderManager != null) {
                if (mHost.getRetainLoaders()) {
                    mLoaderManager.doRetain();
                } else {
                    mLoaderManager.doStop();
                }
            }
        }
    }

    void performDestroyView() {
        if (mChildFragTestManager != null) {
            mChildFragTestManager.dispatchDestroyView();
        }
        mState = CREATED;
        mCalled = false;
        onDestroyView();
        if (!mCalled) {
            throw new AndroidRuntimeException("FragTest " + this
                    + " did not call through to super.onDestroyView()");
        }
        if (mLoaderManager != null) {
            mLoaderManager.doReportNextStart();
        }
    }

    void performDestroy() {
        if (mChildFragTestManager != null) {
            mChildFragTestManager.dispatchDestroy();
        }
        mState = INITIALIZING;
        mCalled = false;
        onDestroy();
        if (!mCalled) {
            throw new AndroidRuntimeException("FragTest " + this
                    + " did not call through to super.onDestroy()");
        }
    }
}
