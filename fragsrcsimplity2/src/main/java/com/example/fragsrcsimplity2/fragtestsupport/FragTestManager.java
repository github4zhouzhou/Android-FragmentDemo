package com.example.fragsrcsimplity2.fragtestsupport;

/**
 * Created by zhouzhou on 2016/11/6.
 */


import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.CallSuper;
import android.support.annotation.IdRes;
import android.support.annotation.StringRes;
import android.support.v4.app.FragmentContainer;
import android.support.v4.util.DebugUtils;
import android.support.v4.util.LogWriter;
import android.support.v4.view.LayoutInflaterFactory;
import android.support.v4.view.ViewCompat;
import android.util.AndroidRuntimeException;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.ScaleAnimation;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;


public abstract class FragTestManager {

    public interface BackStackEntry {
        /**
         * Return the unique identifier for the entry.  This is the only
         * representation of the entry that will persist across activity
         * instances.
         */
        public int getId();

        /**
         * Get the name that was supplied to
         * FragTestTransaction.addToBackStack(String)} when creating this entry.
         */
        public String getName();

        /**
         * Return the full bread crumb title resource identifier for the entry,
         * or 0 if it does not have one.
         */
        @StringRes
        public int getBreadCrumbTitleRes();

        /**
         * Return the short bread crumb title resource identifier for the entry,
         * or 0 if it does not have one.
         */
        @StringRes
        public int getBreadCrumbShortTitleRes();

        /**
         * Return the full bread crumb title for the entry, or null if it
         * does not have one.
         */
        public CharSequence getBreadCrumbTitle();

        /**
         * Return the short bread crumb title for the entry, or null if it
         * does not have one.
         */
        public CharSequence getBreadCrumbShortTitle();
    }

    /**
     * Interface to watch for changes to the back stack.
     */
    public interface OnBackStackChangedListener {
        /**
         * Called whenever the contents of the back stack change.
         */
        public void onBackStackChanged();
    }

    public abstract FragTestTransaction beginTransaction();

    public abstract boolean executePendingTransactions();

    public abstract FragTest findFragTestById(@IdRes int id);

    public abstract FragTest findFragTestByTag(String tag);

    public static final int POP_BACK_STACK_INCLUSIVE = 1<<0;

    public abstract void popBackStack();

    public abstract boolean popBackStackImmediate();

    public abstract void popBackStack(String name, int flags);

    public abstract boolean popBackStackImmediate(String name, int flags);

    public abstract void popBackStack(int id, int flags);

    public abstract boolean popBackStackImmediate(int id, int flags);

    public abstract int getBackStackEntryCount();

    public abstract FragTestManager.BackStackEntry getBackStackEntryAt(int index);

    public abstract void addOnBackStackChangedListener(FragTestManager.OnBackStackChangedListener listener);

    public abstract void removeOnBackStackChangedListener(FragTestManager.OnBackStackChangedListener listener);

    public abstract void putFragTest(Bundle bundle, String key, FragTest FragTest);

    public abstract FragTest getFragTest(Bundle bundle, String key);

    public abstract List<FragTest> getFragTests();

    public abstract FragTest.SavedState saveFragTestInstanceState(FragTest f);

    public abstract boolean isDestroyed();

    public abstract void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args);

    public static void enableDebugLogging(boolean enabled) {
        FragTestManagerImpl.DEBUG = enabled;
    }
}

final class FragTestManagerState implements Parcelable {
    FragTestState[] mActive;
    int[] mAdded;
    BackStackState[] mBackStack;

    public FragTestManagerState() {
    }

    public FragTestManagerState(Parcel in) {
        mActive = in.createTypedArray(FragTestState.CREATOR);
        mAdded = in.createIntArray();
        mBackStack = in.createTypedArray(BackStackState.CREATOR);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedArray(mActive, flags);
        dest.writeIntArray(mAdded);
        dest.writeTypedArray(mBackStack, flags);
    }

    public static final Creator<FragTestManagerState> CREATOR
            = new Creator<FragTestManagerState>() {
        public FragTestManagerState createFromParcel(Parcel in) {
            return new FragTestManagerState(in);
        }

        public FragTestManagerState[] newArray(int size) {
            return new FragTestManagerState[size];
        }
    };
}

/**
 * Container for fragTests associated with an activity.
 */
final class FragTestManagerImpl extends FragTestManager implements LayoutInflaterFactory {
    static boolean DEBUG = false;
    static final String TAG = "FragTestManager";

    static final boolean HONEYCOMB = Build.VERSION.SDK_INT >= 11;

    static final String TARGET_REQUEST_CODE_STATE_TAG = "android:target_req_state";
    static final String TARGET_STATE_TAG = "android:target_state";
    static final String VIEW_STATE_TAG = "android:view_state";
    static final String USER_VISIBLE_HINT_TAG = "android:user_visible_hint";


    static class AnimateOnHWLayerIfNeededListener implements Animation.AnimationListener {
        private Animation.AnimationListener mOrignalListener = null;
        private boolean mShouldRunOnHWLayer = false;
        private View mView = null;
        public AnimateOnHWLayerIfNeededListener(final View v, Animation anim) {
            if (v == null || anim == null) {
                return;
            }
            mView = v;
        }

        public AnimateOnHWLayerIfNeededListener(final View v, Animation anim,
                                                Animation.AnimationListener listener) {
            if (v == null || anim == null) {
                return;
            }
            mOrignalListener = listener;
            mView = v;
        }

        @Override
        @CallSuper
        public void onAnimationStart(Animation animation) {
            if (mView != null) {
                mShouldRunOnHWLayer = shouldRunOnHWLayer(mView, animation);
                if (mShouldRunOnHWLayer) {
                    mView.post(new Runnable() {
                        @Override
                        public void run() {
                            ViewCompat.setLayerType(mView, ViewCompat.LAYER_TYPE_HARDWARE, null);
                        }
                    });
                }
            }
            if (mOrignalListener != null) {
                mOrignalListener.onAnimationStart(animation);
            }
        }

        @Override
        @CallSuper
        public void onAnimationEnd(Animation animation) {
            if (mView != null && mShouldRunOnHWLayer) {
                mView.post(new Runnable() {
                    @Override
                    public void run() {
                        ViewCompat.setLayerType(mView, ViewCompat.LAYER_TYPE_NONE, null);
                    }
                });
            }
            if (mOrignalListener != null) {
                mOrignalListener.onAnimationEnd(animation);
            }
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
            if (mOrignalListener != null) {
                mOrignalListener.onAnimationRepeat(animation);
            }
        }
    }

    ArrayList<Runnable> mPendingActions;
    Runnable[] mTmpActions;
    boolean mExecutingActions;

    ArrayList<FragTest> mActive;
    ArrayList<FragTest> mAdded;
    ArrayList<Integer> mAvailIndices;
    ArrayList<BackStackRecord> mBackStack;
    ArrayList<FragTest> mCreatedMenus;

    // Must be accessed while locked.
    ArrayList<BackStackRecord> mBackStackIndices;
    ArrayList<Integer> mAvailBackStackIndices;

    ArrayList<OnBackStackChangedListener> mBackStackChangeListeners;

    int mCurState = FragTest.INITIALIZING;
    FragTestHostCallback mHost;
    FragTestController mController;
    FragmentContainer mContainer;
    FragTest mParent;

    static Field sAnimationListenerField = null;

    boolean mNeedMenuInvalidate;
    boolean mStateSaved;
    boolean mDestroyed;
    String mNoTransactionsBecause;
    boolean mHavePendingDeferredStart;

    // Temporary vars for state save and restore.
    Bundle mStateBundle = null;
    SparseArray<Parcelable> mStateArray = null;

    Runnable mExecCommit = new Runnable() {
        @Override
        public void run() {
            execPendingActions();
        }
    };

    static boolean modifiesAlpha(Animation anim) {
        if (anim instanceof AlphaAnimation) {
            return true;
        } else if (anim instanceof AnimationSet) {
            List<Animation> anims = ((AnimationSet) anim).getAnimations();
            for (int i = 0; i < anims.size(); i++) {
                if (anims.get(i) instanceof AlphaAnimation) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean shouldRunOnHWLayer(View v, Animation anim) {
        return Build.VERSION.SDK_INT >= 19
                && ViewCompat.getLayerType(v) == ViewCompat.LAYER_TYPE_NONE
                && ViewCompat.hasOverlappingRendering(v)
                && modifiesAlpha(anim);
    }

    private void throwException(RuntimeException ex) {
        Log.e(TAG, ex.getMessage());
        Log.e(TAG, "Activity state:");
        LogWriter logw = new LogWriter(TAG);
        PrintWriter pw = new PrintWriter(logw);
        if (mHost != null) {
            try {
                mHost.onDump("  ", null, pw, new String[] { });
            } catch (Exception e) {
                Log.e(TAG, "Failed dumping state", e);
            }
        } else {
            try {
                dump("  ", null, pw, new String[] { });
            } catch (Exception e) {
                Log.e(TAG, "Failed dumping state", e);
            }
        }
        throw ex;
    }

    @Override
    public FragTestTransaction beginTransaction() {
        return new BackStackRecord(this);
    }

    @Override
    public boolean executePendingTransactions() {
        return execPendingActions();
    }

    @Override
    public void popBackStack() {
        enqueueAction(new Runnable() {
            @Override public void run() {
                popBackStackState(mHost.getHandler(), null, -1, 0);
            }
        }, false);
    }

    @Override
    public boolean popBackStackImmediate() {
        checkStateLoss();
        executePendingTransactions();
        return popBackStackState(mHost.getHandler(), null, -1, 0);
    }

    @Override
    public void popBackStack(final String name, final int flags) {
        enqueueAction(new Runnable() {
            @Override public void run() {
                popBackStackState(mHost.getHandler(), name, -1, flags);
            }
        }, false);
    }

    @Override
    public boolean popBackStackImmediate(String name, int flags) {
        checkStateLoss();
        executePendingTransactions();
        return popBackStackState(mHost.getHandler(), name, -1, flags);
    }

    @Override
    public void popBackStack(final int id, final int flags) {
        if (id < 0) {
            throw new IllegalArgumentException("Bad id: " + id);
        }
        enqueueAction(new Runnable() {
            @Override public void run() {
                popBackStackState(mHost.getHandler(), null, id, flags);
            }
        }, false);
    }

    @Override
    public boolean popBackStackImmediate(int id, int flags) {
        checkStateLoss();
        executePendingTransactions();
        if (id < 0) {
            throw new IllegalArgumentException("Bad id: " + id);
        }
        return popBackStackState(mHost.getHandler(), null, id, flags);
    }

    @Override
    public int getBackStackEntryCount() {
        return mBackStack != null ? mBackStack.size() : 0;
    }

    @Override
    public BackStackEntry getBackStackEntryAt(int index) {
        return mBackStack.get(index);
    }

    @Override
    public void addOnBackStackChangedListener(OnBackStackChangedListener listener) {
        if (mBackStackChangeListeners == null) {
            mBackStackChangeListeners = new ArrayList<OnBackStackChangedListener>();
        }
        mBackStackChangeListeners.add(listener);
    }

    @Override
    public void removeOnBackStackChangedListener(OnBackStackChangedListener listener) {
        if (mBackStackChangeListeners != null) {
            mBackStackChangeListeners.remove(listener);
        }
    }

    @Override
    public void putFragTest(Bundle bundle, String key, FragTest fragTest) {
        if (fragTest.mIndex < 0) {
            throwException(new IllegalStateException("FragTest " + fragTest
                    + " is not currently in the FragTestManager"));
        }
        bundle.putInt(key, fragTest.mIndex);
    }

    @Override
    public FragTest getFragTest(Bundle bundle, String key) {
        int index = bundle.getInt(key, -1);
        if (index == -1) {
            return null;
        }
        if (index >= mActive.size()) {
            throwException(new IllegalStateException("FragTest no longer exists for key "
                    + key + ": index " + index));
        }
        FragTest f = mActive.get(index);
        if (f == null) {
            throwException(new IllegalStateException("FragTest no longer exists for key "
                    + key + ": index " + index));
        }
        return f;
    }

    @Override
    public List<FragTest> getFragTests() {
        return mActive;
    }

    @Override
    public FragTest.SavedState saveFragTestInstanceState(FragTest fragTest) {
        if (fragTest.mIndex < 0) {
            throwException( new IllegalStateException("FragTest " + fragTest
                    + " is not currently in the FragTestManager"));
        }
        if (fragTest.mState > FragTest.INITIALIZING) {
            Bundle result = saveFragTestBasicState(fragTest);
            return result != null ? new FragTest.SavedState(result) : null;
        }
        return null;
    }

    @Override
    public boolean isDestroyed() {
        return mDestroyed;
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("FragTestManager{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" in ");
        if (mParent != null) {
            DebugUtils.buildShortClassTag(mParent, sb);
        } else {
            DebugUtils.buildShortClassTag(mHost, sb);
        }
        sb.append("}}");
        return sb.toString();
    }



    static final Interpolator DECELERATE_QUINT = new DecelerateInterpolator(2.5f);
    static final Interpolator DECELERATE_CUBIC = new DecelerateInterpolator(1.5f);
    static final Interpolator ACCELERATE_QUINT = new AccelerateInterpolator(2.5f);
    static final Interpolator ACCELERATE_CUBIC = new AccelerateInterpolator(1.5f);

    static final int ANIM_DUR = 220;

    static Animation makeOpenCloseAnimation(Context context, float startScale,
                                            float endScale, float startAlpha, float endAlpha) {
        AnimationSet set = new AnimationSet(false);
        ScaleAnimation scale = new ScaleAnimation(startScale, endScale, startScale, endScale,
                Animation.RELATIVE_TO_SELF, .5f, Animation.RELATIVE_TO_SELF, .5f);
        scale.setInterpolator(DECELERATE_QUINT);
        scale.setDuration(ANIM_DUR);
        set.addAnimation(scale);
        AlphaAnimation alpha = new AlphaAnimation(startAlpha, endAlpha);
        alpha.setInterpolator(DECELERATE_CUBIC);
        alpha.setDuration(ANIM_DUR);
        set.addAnimation(alpha);
        return set;
    }

    static Animation makeFadeAnimation(Context context, float start, float end) {
        AlphaAnimation anim = new AlphaAnimation(start, end);
        anim.setInterpolator(DECELERATE_CUBIC);
        anim.setDuration(ANIM_DUR);
        return anim;
    }

    Animation loadAnimation(FragTest fragTest, int transit, boolean enter,
                            int transitionStyle) {
        Animation animObj = fragTest.onCreateAnimation(transit, enter,
                fragTest.mNextAnim);
        if (animObj != null) {
            return animObj;
        }

        if (fragTest.mNextAnim != 0) {
            Animation anim = AnimationUtils.loadAnimation(mHost.getContext(), fragTest.mNextAnim);
            if (anim != null) {
                return anim;
            }
        }

        if (transit == 0) {
            return null;
        }

        int styleIndex = transitToStyleIndex(transit, enter);
        if (styleIndex < 0) {
            return null;
        }

        switch (styleIndex) {
            case ANIM_STYLE_OPEN_ENTER:
                return makeOpenCloseAnimation(mHost.getContext(), 1.125f, 1.0f, 0, 1);
            case ANIM_STYLE_OPEN_EXIT:
                return makeOpenCloseAnimation(mHost.getContext(), 1.0f, .975f, 1, 0);
            case ANIM_STYLE_CLOSE_ENTER:
                return makeOpenCloseAnimation(mHost.getContext(), .975f, 1.0f, 0, 1);
            case ANIM_STYLE_CLOSE_EXIT:
                return makeOpenCloseAnimation(mHost.getContext(), 1.0f, 1.075f, 1, 0);
            case ANIM_STYLE_FADE_ENTER:
                return makeFadeAnimation(mHost.getContext(), 0, 1);
            case ANIM_STYLE_FADE_EXIT:
                return makeFadeAnimation(mHost.getContext(), 1, 0);
        }

        if (transitionStyle == 0 && mHost.onHasWindowAnimations()) {
            transitionStyle = mHost.onGetWindowAnimations();
        }
        if (transitionStyle == 0) {
            return null;
        }
        return null;
    }

    public void performPendingDeferredStart(FragTest f) {
        if (f.mDeferStart) {
            if (mExecutingActions) {
                // Wait until we're done executing our pending transactions
                mHavePendingDeferredStart = true;
                return;
            }
            f.mDeferStart = false;
            moveToState(f, mCurState, 0, 0, false);
        }
    }

    /**
     * Sets the to be animated view on hardware layer during the animation. Note
     * that calling this will replace any existing animation listener on the animation
     * with a new one, as animations do not support more than one listeners. Therefore,
     * animations that already have listeners should do the layer change operations
     * in their existing listeners, rather than calling this function.
     */
    private void setHWLayerAnimListenerIfAlpha(final View v, Animation anim) {
        if (v == null || anim == null) {
            return;
        }
        if (shouldRunOnHWLayer(v, anim)) {
            Animation.AnimationListener originalListener = null;
            try {
                if (sAnimationListenerField == null) {
                    sAnimationListenerField = Animation.class.getDeclaredField("mListener");
                    sAnimationListenerField.setAccessible(true);
                }
                originalListener = (Animation.AnimationListener) sAnimationListenerField.get(anim);
            } catch (NoSuchFieldException e) {
                Log.e(TAG, "No field with the name mListener is found in Animation class", e);
            } catch (IllegalAccessException e) {
                Log.e(TAG, "Cannot access Animation's mListener field", e);
            }
            // If there's already a listener set on the animation, we need wrap the new listener
            // around the existing listener, so that they will both get animation listener
            // callbacks.
            anim.setAnimationListener(new FragTestManagerImpl.AnimateOnHWLayerIfNeededListener(v, anim,
                    originalListener));
        }
    }

    void moveToState(FragTest f, int newState, int transit, int transitionStyle, boolean keepActive) {

        // FragTests that are not currently added will sit in the onCreate() state.
        if ((!f.mAdded || f.mDetached) && newState > FragTest.CREATED) {
            newState = FragTest.CREATED;
        }
        if (f.mRemoving && newState > f.mState) {
            // While removing a fragTest, we can't change it to a higher state.
            newState = f.mState;
        }
        // Defer start if requested; don't allow it to move to STARTED or higher
        // if it's not already started.
        if (f.mDeferStart && f.mState < FragTest.STARTED && newState > FragTest.STOPPED) {
            newState = FragTest.STOPPED;
        }
        if (f.mState < newState) {
            // For fragTests that are created from a layout, when restoring from
            // state we don't want to allow them to be created until they are
            // being reloaded from the layout.
            if (f.mFromLayout && !f.mInLayout) {
                return;
            }
            if (f.mAnimatingAway != null) {
                // The fragTest is currently being animated...  but!  Now we
                // want to move our state back up.  Give up on waiting for the
                // animation, move to whatever the final state should be once
                // the animation is done, and then we can proceed from there.
                f.mAnimatingAway = null;
                moveToState(f, f.mStateAfterAnimating, 0, 0, true);
            }
            switch (f.mState) {
                case FragTest.INITIALIZING:
                    if (DEBUG) Log.v(TAG, "moveto CREATED: " + f);
                    if (f.mSavedFragTestState != null) {
                        f.mSavedFragTestState.setClassLoader(mHost.getContext().getClassLoader());
                        f.mSavedViewState = f.mSavedFragTestState.getSparseParcelableArray(
                                FragTestManagerImpl.VIEW_STATE_TAG);
                        f.mTarget = getFragTest(f.mSavedFragTestState,
                                FragTestManagerImpl.TARGET_STATE_TAG);
                        if (f.mTarget != null) {
                            f.mTargetRequestCode = f.mSavedFragTestState.getInt(
                                    FragTestManagerImpl.TARGET_REQUEST_CODE_STATE_TAG, 0);
                        }
                        f.mUserVisibleHint = f.mSavedFragTestState.getBoolean(
                                FragTestManagerImpl.USER_VISIBLE_HINT_TAG, true);
                        if (!f.mUserVisibleHint) {
                            f.mDeferStart = true;
                            if (newState > FragTest.STOPPED) {
                                newState = FragTest.STOPPED;
                            }
                        }
                    }
                    f.mHost = mHost;
                    f.mParentFragTest = mParent;
                    f.mFragTestManager = mParent != null
                            ? mParent.mChildFragTestManager : mHost.getFragTestManagerImpl();
                    f.mCalled = false;
                    f.onAttach(mHost.getContext());
                    if (!f.mCalled) {
                        throw new AndroidRuntimeException("FragTest " + f
                                + " did not call through to super.onAttach()");
                    }
                    if (f.mParentFragTest == null) {
                        mHost.onAttachFragTest(f);
                    }

                    if (!f.mRetaining) {
                        f.performCreate(f.mSavedFragTestState);
                    }
                    f.mRetaining = false;
                    if (f.mFromLayout) {
                        // For fragTests that are part of the content view
                        // layout, we need to instantiate the view immediately
                        // and the inflater will take care of adding it.
                        f.mView = f.performCreateView(f.getLayoutInflater(
                                f.mSavedFragTestState), null, f.mSavedFragTestState);
                        if (f.mView != null) {
                            f.mInnerView = f.mView;
                            if (Build.VERSION.SDK_INT >= 11) {
                                ViewCompat.setSaveFromParentEnabled(f.mView, false);
                            } else {
                                f.mView = NoSaveStateFrameLayout.wrap(f.mView);
                            }
                            if (f.mHidden) f.mView.setVisibility(View.GONE);
                            f.onViewCreated(f.mView, f.mSavedFragTestState);
                        } else {
                            f.mInnerView = null;
                        }
                    }
                case FragTest.CREATED:
                    if (newState > FragTest.CREATED) {
                        if (DEBUG) Log.v(TAG, "moveto ACTIVITY_CREATED: " + f);
                        if (!f.mFromLayout) {
                            ViewGroup container = null;
                            if (f.mContainerId != 0) {
                                container = (ViewGroup)mContainer.onFindViewById(f.mContainerId);
                                if (container == null && !f.mRestored) {
                                    throwException(new IllegalArgumentException(
                                            "No view found for id 0x"
                                                    + Integer.toHexString(f.mContainerId) + " ("
                                                    + f.getResources().getResourceName(f.mContainerId)
                                                    + ") for fragTest " + f));
                                }
                            }
                            f.mContainer = container;
                            f.mView = f.performCreateView(f.getLayoutInflater(
                                    f.mSavedFragTestState), container, f.mSavedFragTestState);
                            if (f.mView != null) {
                                f.mInnerView = f.mView;
                                if (Build.VERSION.SDK_INT >= 11) {
                                    ViewCompat.setSaveFromParentEnabled(f.mView, false);
                                } else {
                                    f.mView = NoSaveStateFrameLayout.wrap(f.mView);
                                }
                                if (container != null) {
                                    Animation anim = loadAnimation(f, transit, true,
                                            transitionStyle);
                                    if (anim != null) {
                                        setHWLayerAnimListenerIfAlpha(f.mView, anim);
                                        f.mView.startAnimation(anim);
                                    }
                                    container.addView(f.mView);
                                }
                                if (f.mHidden) f.mView.setVisibility(View.GONE);
                                f.onViewCreated(f.mView, f.mSavedFragTestState);
                            } else {
                                f.mInnerView = null;
                            }
                        }

                        f.performActivityCreated(f.mSavedFragTestState);
                        if (f.mView != null) {
                            f.restoreViewState(f.mSavedFragTestState);
                        }
                        f.mSavedFragTestState = null;
                    }
                case FragTest.ACTIVITY_CREATED:
                case FragTest.STOPPED:
                    if (newState > FragTest.STOPPED) {
                        if (DEBUG) Log.v(TAG, "moveto STARTED: " + f);
                        f.performStart();
                    }
                case FragTest.STARTED:
                    if (newState > FragTest.STARTED) {
                        if (DEBUG) Log.v(TAG, "moveto RESUMED: " + f);
                        f.performResume();
                        f.mSavedFragTestState = null;
                        f.mSavedViewState = null;
                    }
            }
        } else if (f.mState > newState) {
            switch (f.mState) {
                case FragTest.RESUMED:
                    if (newState < FragTest.RESUMED) {
                        if (DEBUG) Log.v(TAG, "movefrom RESUMED: " + f);
                        f.performPause();
                    }
                case FragTest.STARTED:
                    if (newState < FragTest.STARTED) {
                        if (DEBUG) Log.v(TAG, "movefrom STARTED: " + f);
                        f.performStop();
                    }
                case FragTest.STOPPED:
                    if (newState < FragTest.STOPPED) {
                        if (DEBUG) Log.v(TAG, "movefrom STOPPED: " + f);
                        f.performReallyStop();
                    }
                case FragTest.ACTIVITY_CREATED:
                    if (newState < FragTest.ACTIVITY_CREATED) {
                        if (DEBUG) Log.v(TAG, "movefrom ACTIVITY_CREATED: " + f);
                        if (f.mView != null) {
                            // Need to save the current view state if not
                            // done already.
                            if (mHost.onShouldSaveFragTestState(f) && f.mSavedViewState == null) {
                                saveFragTestViewState(f);
                            }
                        }
                        f.performDestroyView();
                        if (f.mView != null && f.mContainer != null) {
                            Animation anim = null;
                            if (mCurState > FragTest.INITIALIZING && !mDestroyed) {
                                anim = loadAnimation(f, transit, false,
                                        transitionStyle);
                            }
                            if (anim != null) {
                                final FragTest fragTest = f;
                                f.mAnimatingAway = f.mView;
                                f.mStateAfterAnimating = newState;
                                final View viewToAnimate = f.mView;
                                anim.setAnimationListener(new FragTestManagerImpl.AnimateOnHWLayerIfNeededListener(
                                        viewToAnimate, anim) {
                                    @Override
                                    public void onAnimationEnd(Animation animation) {
                                        super.onAnimationEnd(animation);
                                        if (fragTest.mAnimatingAway != null) {
                                            fragTest.mAnimatingAway = null;
                                            moveToState(fragTest, fragTest.mStateAfterAnimating,
                                                    0, 0, false);
                                        }
                                    }
                                });
                                f.mView.startAnimation(anim);
                            }
                            f.mContainer.removeView(f.mView);
                        }
                        f.mContainer = null;
                        f.mView = null;
                        f.mInnerView = null;
                    }
                case FragTest.CREATED:
                    if (newState < FragTest.CREATED) {
                        if (mDestroyed) {
                            if (f.mAnimatingAway != null) {
                                // The fragTest's containing activity is
                                // being destroyed, but this fragTest is
                                // currently animating away.  Stop the
                                // animation right now -- it is not needed,
                                // and we can't wait any more on destroying
                                // the fragTest.
                                View v = f.mAnimatingAway;
                                f.mAnimatingAway = null;
                                v.clearAnimation();
                            }
                        }
                        if (f.mAnimatingAway != null) {
                            // We are waiting for the fragTest's view to finish
                            // animating away.  Just make a note of the state
                            // the fragTest now should move to once the animation
                            // is done.
                            f.mStateAfterAnimating = newState;
                            newState = FragTest.CREATED;
                        } else {
                            if (DEBUG) Log.v(TAG, "movefrom CREATED: " + f);
                            if (!f.mRetaining) {
                                f.performDestroy();
                            } else {
                                f.mState = FragTest.INITIALIZING;
                            }

                            f.mCalled = false;
                            f.onDetach();
                            if (!f.mCalled) {
                                throw new AndroidRuntimeException("FragTest " + f
                                        + " did not call through to super.onDetach()");
                            }
                            if (!keepActive) {
                                if (!f.mRetaining) {
                                    makeInactive(f);
                                } else {
                                    f.mHost = null;
                                    f.mParentFragTest = null;
                                    f.mFragTestManager = null;
                                    f.mChildFragTestManager = null;
                                }
                            }
                        }
                    }
            }
        }

        if (f.mState != newState) {
            Log.w(TAG, "moveToState: FragTest state for " + f + " not updated inline; "
                    + "expected state " + newState + " found " + f.mState);
            f.mState = newState;
        }
    }

    void moveToState(FragTest f) {
        moveToState(f, mCurState, 0, 0, false);
    }

    void moveToState(int newState, boolean always) {
        moveToState(newState, 0, 0, always);
    }

    void moveToState(int newState, int transit, int transitStyle, boolean always) {
        if (mHost == null && newState != FragTest.INITIALIZING) {
            throw new IllegalStateException("No host");
        }

        if (!always && mCurState == newState) {
            return;
        }

        mCurState = newState;
        if (mActive != null) {
            boolean loadersRunning = false;
            for (int i=0; i<mActive.size(); i++) {
                FragTest f = mActive.get(i);
                if (f != null) {
                    moveToState(f, newState, transit, transitStyle, false);
                    if (f.mLoaderManager != null) {
                        loadersRunning |= f.mLoaderManager.hasRunningLoaders();
                    }
                }
            }

            if (!loadersRunning) {
                startPendingDeferredFragTests();
            }

            if (mNeedMenuInvalidate && mHost != null && mCurState == FragTest.RESUMED) {
                mHost.onSupportInvalidateOptionsMenu();
                mNeedMenuInvalidate = false;
            }
        }
    }

    void startPendingDeferredFragTests() {
        if (mActive == null) return;

        for (int i=0; i<mActive.size(); i++) {
            FragTest f = mActive.get(i);
            if (f != null) {
                performPendingDeferredStart(f);
            }
        }
    }

    void makeActive(FragTest f) {
        if (f.mIndex >= 0) {
            return;
        }

        if (mAvailIndices == null || mAvailIndices.size() <= 0) {
            if (mActive == null) {
                mActive = new ArrayList<FragTest>();
            }
            f.setIndex(mActive.size(), mParent);
            mActive.add(f);

        } else {
            f.setIndex(mAvailIndices.remove(mAvailIndices.size()-1), mParent);
            mActive.set(f.mIndex, f);
        }
        if (DEBUG) Log.v(TAG, "Allocated fragTest index " + f);
    }

    void makeInactive(FragTest f) {
        if (f.mIndex < 0) {
            return;
        }

        if (DEBUG) Log.v(TAG, "Freeing fragTest index " + f);
        mActive.set(f.mIndex, null);
        if (mAvailIndices == null) {
            mAvailIndices = new ArrayList<Integer>();
        }
        mAvailIndices.add(f.mIndex);
        mHost.inactivateFragTest(f.mWho);
        f.initState();
    }

    public void addFragTest(FragTest fragTest, boolean moveToStateNow) {
        if (mAdded == null) {
            mAdded = new ArrayList<FragTest>();
        }
        if (DEBUG) Log.v(TAG, "add: " + fragTest);
        makeActive(fragTest);
        if (!fragTest.mDetached) {
            if (mAdded.contains(fragTest)) {
                throw new IllegalStateException("FragTest already added: " + fragTest);
            }
            mAdded.add(fragTest);
            fragTest.mAdded = true;
            fragTest.mRemoving = false;
            if (fragTest.mHasMenu && fragTest.mMenuVisible) {
                mNeedMenuInvalidate = true;
            }
            if (moveToStateNow) {
                moveToState(fragTest);
            }
        }
    }

    public void removeFragTest(FragTest fragTest, int transition, int transitionStyle) {
        if (DEBUG) Log.v(TAG, "remove: " + fragTest + " nesting=" + fragTest.mBackStackNesting);
        final boolean inactive = !fragTest.isInBackStack();
        if (!fragTest.mDetached || inactive) {
            if (mAdded != null) {
                mAdded.remove(fragTest);
            }
            if (fragTest.mHasMenu && fragTest.mMenuVisible) {
                mNeedMenuInvalidate = true;
            }
            fragTest.mAdded = false;
            fragTest.mRemoving = true;
            moveToState(fragTest, inactive ? FragTest.INITIALIZING : FragTest.CREATED,
                    transition, transitionStyle, false);
        }
    }

    public void hideFragTest(FragTest fragTest, int transition, int transitionStyle) {
        if (DEBUG) Log.v(TAG, "hide: " + fragTest);
        if (!fragTest.mHidden) {
            fragTest.mHidden = true;
            if (fragTest.mView != null) {
                Animation anim = loadAnimation(fragTest, transition, false,
                        transitionStyle);
                if (anim != null) {
                    setHWLayerAnimListenerIfAlpha(fragTest.mView, anim);
                    fragTest.mView.startAnimation(anim);
                }
                fragTest.mView.setVisibility(View.GONE);
            }
            if (fragTest.mAdded && fragTest.mHasMenu && fragTest.mMenuVisible) {
                mNeedMenuInvalidate = true;
            }
            fragTest.onHiddenChanged(true);
        }
    }

    public void showFragTest(FragTest fragTest, int transition, int transitionStyle) {
        if (DEBUG) Log.v(TAG, "show: " + fragTest);
        if (fragTest.mHidden) {
            fragTest.mHidden = false;
            if (fragTest.mView != null) {
                Animation anim = loadAnimation(fragTest, transition, true,
                        transitionStyle);
                if (anim != null) {
                    setHWLayerAnimListenerIfAlpha(fragTest.mView, anim);
                    fragTest.mView.startAnimation(anim);
                }
                fragTest.mView.setVisibility(View.VISIBLE);
            }
            if (fragTest.mAdded && fragTest.mHasMenu && fragTest.mMenuVisible) {
                mNeedMenuInvalidate = true;
            }
            fragTest.onHiddenChanged(false);
        }
    }

    public void detachFragTest(FragTest fragTest, int transition, int transitionStyle) {
        if (DEBUG) Log.v(TAG, "detach: " + fragTest);
        if (!fragTest.mDetached) {
            fragTest.mDetached = true;
            if (fragTest.mAdded) {
                // We are not already in back stack, so need to remove the fragTest.
                if (mAdded != null) {
                    if (DEBUG) Log.v(TAG, "remove from detach: " + fragTest);
                    mAdded.remove(fragTest);
                }
                if (fragTest.mHasMenu && fragTest.mMenuVisible) {
                    mNeedMenuInvalidate = true;
                }
                fragTest.mAdded = false;
                moveToState(fragTest, FragTest.CREATED, transition, transitionStyle, false);
            }
        }
    }

    public void attachFragTest(FragTest fragTest, int transition, int transitionStyle) {
        if (DEBUG) Log.v(TAG, "attach: " + fragTest);
        if (fragTest.mDetached) {
            fragTest.mDetached = false;
            if (!fragTest.mAdded) {
                if (mAdded == null) {
                    mAdded = new ArrayList<FragTest>();
                }
                if (mAdded.contains(fragTest)) {
                    throw new IllegalStateException("FragTest already added: " + fragTest);
                }
                if (DEBUG) Log.v(TAG, "add from attach: " + fragTest);
                mAdded.add(fragTest);
                fragTest.mAdded = true;
                if (fragTest.mHasMenu && fragTest.mMenuVisible) {
                    mNeedMenuInvalidate = true;
                }
                moveToState(fragTest, mCurState, transition, transitionStyle, false);
            }
        }
    }

    public FragTest findFragTestById(int id) {
        if (mAdded != null) {
            // First look through added fragTests.
            for (int i=mAdded.size()-1; i>=0; i--) {
                FragTest f = mAdded.get(i);
                if (f != null && f.mFragTestId == id) {
                    return f;
                }
            }
        }
        if (mActive != null) {
            // Now for any known fragTest.
            for (int i=mActive.size()-1; i>=0; i--) {
                FragTest f = mActive.get(i);
                if (f != null && f.mFragTestId == id) {
                    return f;
                }
            }
        }
        return null;
    }

    public FragTest findFragTestByTag(String tag) {
        if (mAdded != null && tag != null) {
            // First look through added fragTests.
            for (int i=mAdded.size()-1; i>=0; i--) {
                FragTest f = mAdded.get(i);
                if (f != null && tag.equals(f.mTag)) {
                    return f;
                }
            }
        }
        if (mActive != null && tag != null) {
            // Now for any known fragTest.
            for (int i=mActive.size()-1; i>=0; i--) {
                FragTest f = mActive.get(i);
                if (f != null && tag.equals(f.mTag)) {
                    return f;
                }
            }
        }
        return null;
    }

    public FragTest findFragTestByWho(String who) {
        if (mActive != null && who != null) {
            for (int i=mActive.size()-1; i>=0; i--) {
                FragTest f = mActive.get(i);
                if (f != null && (f=f.findFragTestByWho(who)) != null) {
                    return f;
                }
            }
        }
        return null;
    }

    private void checkStateLoss() {
        if (mStateSaved) {
            throw new IllegalStateException(
                    "Can not perform this action after onSaveInstanceState");
        }
        if (mNoTransactionsBecause != null) {
            throw new IllegalStateException(
                    "Can not perform this action inside of " + mNoTransactionsBecause);
        }
    }

    /**
     * Adds an action to the queue of pending actions.
     *
     * @param action the action to add
     * @param allowStateLoss whether to allow loss of state information
     * @throws IllegalStateException if the activity has been destroyed
     */
    public void enqueueAction(Runnable action, boolean allowStateLoss) {
        if (!allowStateLoss) {
            checkStateLoss();
        }
        synchronized (this) {
            if (mDestroyed || mHost == null) {
                throw new IllegalStateException("Activity has been destroyed");
            }
            if (mPendingActions == null) {
                mPendingActions = new ArrayList<Runnable>();
            }
            mPendingActions.add(action);
            if (mPendingActions.size() == 1) {
                mHost.getHandler().removeCallbacks(mExecCommit);
                mHost.getHandler().post(mExecCommit);
            }
        }
    }

    public int allocBackStackIndex(BackStackRecord bse) {
        synchronized (this) {
            if (mAvailBackStackIndices == null || mAvailBackStackIndices.size() <= 0) {
                if (mBackStackIndices == null) {
                    mBackStackIndices = new ArrayList<BackStackRecord>();
                }
                int index = mBackStackIndices.size();
                if (DEBUG) Log.v(TAG, "Setting back stack index " + index + " to " + bse);
                mBackStackIndices.add(bse);
                return index;

            } else {
                int index = mAvailBackStackIndices.remove(mAvailBackStackIndices.size()-1);
                if (DEBUG) Log.v(TAG, "Adding back stack index " + index + " with " + bse);
                mBackStackIndices.set(index, bse);
                return index;
            }
        }
    }

    public void setBackStackIndex(int index, BackStackRecord bse) {
        synchronized (this) {
            if (mBackStackIndices == null) {
                mBackStackIndices = new ArrayList<BackStackRecord>();
            }
            int N = mBackStackIndices.size();
            if (index < N) {
                if (DEBUG) Log.v(TAG, "Setting back stack index " + index + " to " + bse);
                mBackStackIndices.set(index, bse);
            } else {
                while (N < index) {
                    mBackStackIndices.add(null);
                    if (mAvailBackStackIndices == null) {
                        mAvailBackStackIndices = new ArrayList<Integer>();
                    }
                    if (DEBUG) Log.v(TAG, "Adding available back stack index " + N);
                    mAvailBackStackIndices.add(N);
                    N++;
                }
                if (DEBUG) Log.v(TAG, "Adding back stack index " + index + " with " + bse);
                mBackStackIndices.add(bse);
            }
        }
    }

    public void freeBackStackIndex(int index) {
        synchronized (this) {
            mBackStackIndices.set(index, null);
            if (mAvailBackStackIndices == null) {
                mAvailBackStackIndices = new ArrayList<Integer>();
            }
            if (DEBUG) Log.v(TAG, "Freeing back stack index " + index);
            mAvailBackStackIndices.add(index);
        }
    }

    /**
     * Only call from main thread!
     */
    public boolean execPendingActions() {
        if (mExecutingActions) {
            throw new IllegalStateException("Recursive entry to executePendingTransactions");
        }

        if (Looper.myLooper() != mHost.getHandler().getLooper()) {
            throw new IllegalStateException("Must be called from main thread of process");
        }

        boolean didSomething = false;

        while (true) {
            int numActions;

            synchronized (this) {
                if (mPendingActions == null || mPendingActions.size() == 0) {
                    break;
                }

                numActions = mPendingActions.size();
                if (mTmpActions == null || mTmpActions.length < numActions) {
                    mTmpActions = new Runnable[numActions];
                }
                mPendingActions.toArray(mTmpActions);
                mPendingActions.clear();
                mHost.getHandler().removeCallbacks(mExecCommit);
            }

            mExecutingActions = true;
            for (int i=0; i<numActions; i++) {
                mTmpActions[i].run();
                mTmpActions[i] = null;
            }
            mExecutingActions = false;
            didSomething = true;
        }

        if (mHavePendingDeferredStart) {
            boolean loadersRunning = false;
            for (int i=0; i<mActive.size(); i++) {
                FragTest f = mActive.get(i);
                if (f != null && f.mLoaderManager != null) {
                    loadersRunning |= f.mLoaderManager.hasRunningLoaders();
                }
            }
            if (!loadersRunning) {
                mHavePendingDeferredStart = false;
                startPendingDeferredFragTests();
            }
        }
        return didSomething;
    }

    void reportBackStackChanged() {
        if (mBackStackChangeListeners != null) {
            for (int i=0; i<mBackStackChangeListeners.size(); i++) {
                mBackStackChangeListeners.get(i).onBackStackChanged();
            }
        }
    }

    void addBackStackState(BackStackRecord state) {
        if (mBackStack == null) {
            mBackStack = new ArrayList<BackStackRecord>();
        }
        mBackStack.add(state);
        reportBackStackChanged();
    }

    @SuppressWarnings("unused")
    boolean popBackStackState(Handler handler, String name, int id, int flags) {
        if (mBackStack == null) {
            return false;
        }
        if (name == null && id < 0 && (flags&POP_BACK_STACK_INCLUSIVE) == 0) {
            int last = mBackStack.size()-1;
            if (last < 0) {
                return false;
            }
            final BackStackRecord bss = mBackStack.remove(last);
            SparseArray<FragTest> firstOutFragTests = new SparseArray<FragTest>();
            SparseArray<FragTest> lastInFragTests = new SparseArray<FragTest>();
            bss.calculateBackFragTests(firstOutFragTests, lastInFragTests);
            bss.popFromBackStack(true, null, firstOutFragTests, lastInFragTests);
            reportBackStackChanged();
        } else {
            int index = -1;
            if (name != null || id >= 0) {
                // If a name or ID is specified, look for that place in
                // the stack.
                index = mBackStack.size()-1;
                while (index >= 0) {
                    BackStackRecord bss = mBackStack.get(index);
                    if (name != null && name.equals(bss.getName())) {
                        break;
                    }
                    if (id >= 0 && id == bss.mIndex) {
                        break;
                    }
                    index--;
                }
                if (index < 0) {
                    return false;
                }
                if ((flags&POP_BACK_STACK_INCLUSIVE) != 0) {
                    index--;
                    // Consume all following entries that match.
                    while (index >= 0) {
                        BackStackRecord bss = mBackStack.get(index);
                        if ((name != null && name.equals(bss.getName()))
                                || (id >= 0 && id == bss.mIndex)) {
                            index--;
                            continue;
                        }
                        break;
                    }
                }
            }
            if (index == mBackStack.size()-1) {
                return false;
            }
            final ArrayList<BackStackRecord> states
                    = new ArrayList<BackStackRecord>();
            for (int i=mBackStack.size()-1; i>index; i--) {
                states.add(mBackStack.remove(i));
            }
            final int LAST = states.size()-1;
            SparseArray<FragTest> firstOutFragTests = new SparseArray<FragTest>();
            SparseArray<FragTest> lastInFragTests = new SparseArray<FragTest>();
            for (int i=0; i<=LAST; i++) {
                states.get(i).calculateBackFragTests(firstOutFragTests, lastInFragTests);
            }
            BackStackRecord.TransitionState state = null;
            for (int i=0; i<=LAST; i++) {
                if (DEBUG) Log.v(TAG, "Popping back stack state: " + states.get(i));
                state = states.get(i).popFromBackStack(i == LAST, state,
                        firstOutFragTests, lastInFragTests);
            }
            reportBackStackChanged();
        }
        return true;
    }

    ArrayList<FragTest> retainNonConfig() {
        ArrayList<FragTest> fragTests = null;
        if (mActive != null) {
            for (int i=0; i<mActive.size(); i++) {
                FragTest f = mActive.get(i);
                if (f != null && f.mRetainInstance) {
                    if (fragTests == null) {
                        fragTests = new ArrayList<FragTest>();
                    }
                    fragTests.add(f);
                    f.mRetaining = true;
                    f.mTargetIndex = f.mTarget != null ? f.mTarget.mIndex : -1;
                    if (DEBUG) Log.v(TAG, "retainNonConfig: keeping retained " + f);
                }
            }
        }
        return fragTests;
    }

    void saveFragTestViewState(FragTest f) {
        if (f.mInnerView == null) {
            return;
        }
        if (mStateArray == null) {
            mStateArray = new SparseArray<Parcelable>();
        } else {
            mStateArray.clear();
        }
        f.mInnerView.saveHierarchyState(mStateArray);
        if (mStateArray.size() > 0) {
            f.mSavedViewState = mStateArray;
            mStateArray = null;
        }
    }

    Bundle saveFragTestBasicState(FragTest f) {
        Bundle result = null;

        if (mStateBundle == null) {
            mStateBundle = new Bundle();
        }
        f.performSaveInstanceState(mStateBundle);
        if (!mStateBundle.isEmpty()) {
            result = mStateBundle;
            mStateBundle = null;
        }

        if (f.mView != null) {
            saveFragTestViewState(f);
        }
        if (f.mSavedViewState != null) {
            if (result == null) {
                result = new Bundle();
            }
            result.putSparseParcelableArray(
                    FragTestManagerImpl.VIEW_STATE_TAG, f.mSavedViewState);
        }
        if (!f.mUserVisibleHint) {
            if (result == null) {
                result = new Bundle();
            }
            // Only add this if it's not the default value
            result.putBoolean(FragTestManagerImpl.USER_VISIBLE_HINT_TAG, f.mUserVisibleHint);
        }

        return result;
    }

    Parcelable saveAllState() {
        // Make sure all pending operations have now been executed to get
        // our state update-to-date.
        execPendingActions();

        if (HONEYCOMB) {
            // As of Honeycomb, we save state after pausing.  Prior to that
            // it is before pausing.  With fragTests this is an issue, since
            // there are many things you may do after pausing but before
            // stopping that change the fragTest state.  For those older
            // devices, we will not at this point say that we have saved
            // the state, so we will allow them to continue doing fragTest
            // transactions.  This retains the same semantics as Honeycomb,
            // though you do have the risk of losing the very most recent state
            // if the process is killed...  we'll live with that.
            mStateSaved = true;
        }

        if (mActive == null || mActive.size() <= 0) {
            return null;
        }

        // First collect all active fragTests.
        int N = mActive.size();
        FragTestState[] active = new FragTestState[N];
        boolean haveFragTests = false;
        for (int i=0; i<N; i++) {
            FragTest f = mActive.get(i);
            if (f != null) {
                if (f.mIndex < 0) {
                    throwException(new IllegalStateException(
                            "Failure saving state: active " + f
                                    + " has cleared index: " + f.mIndex));
                }

                haveFragTests = true;

                FragTestState fs = new FragTestState(f);
                active[i] = fs;

                if (f.mState > FragTest.INITIALIZING && fs.mSavedFragTestState == null) {
                    fs.mSavedFragTestState = saveFragTestBasicState(f);

                    if (f.mTarget != null) {
                        if (f.mTarget.mIndex < 0) {
                            throwException(new IllegalStateException(
                                    "Failure saving state: " + f
                                            + " has target not in fragTest manager: " + f.mTarget));
                        }
                        if (fs.mSavedFragTestState == null) {
                            fs.mSavedFragTestState = new Bundle();
                        }
                        putFragTest(fs.mSavedFragTestState,
                                FragTestManagerImpl.TARGET_STATE_TAG, f.mTarget);
                        if (f.mTargetRequestCode != 0) {
                            fs.mSavedFragTestState.putInt(
                                    FragTestManagerImpl.TARGET_REQUEST_CODE_STATE_TAG,
                                    f.mTargetRequestCode);
                        }
                    }

                } else {
                    fs.mSavedFragTestState = f.mSavedFragTestState;
                }

                if (DEBUG) Log.v(TAG, "Saved state of " + f + ": "
                        + fs.mSavedFragTestState);
            }
        }

        if (!haveFragTests) {
            if (DEBUG) Log.v(TAG, "saveAllState: no fragTests!");
            return null;
        }

        int[] added = null;
        BackStackState[] backStack = null;

        // Build list of currently added fragTests.
        if (mAdded != null) {
            N = mAdded.size();
            if (N > 0) {
                added = new int[N];
                for (int i=0; i<N; i++) {
                    added[i] = mAdded.get(i).mIndex;
                    if (added[i] < 0) {
                        throwException(new IllegalStateException(
                                "Failure saving state: active " + mAdded.get(i)
                                        + " has cleared index: " + added[i]));
                    }
                    if (DEBUG) Log.v(TAG, "saveAllState: adding fragTest #" + i
                            + ": " + mAdded.get(i));
                }
            }
        }

        // Now save back stack.
        if (mBackStack != null) {
            N = mBackStack.size();
            if (N > 0) {
                backStack = new BackStackState[N];
                for (int i=0; i<N; i++) {
                    backStack[i] = new BackStackState(mBackStack.get(i));
                    if (DEBUG) Log.v(TAG, "saveAllState: adding back stack #" + i
                            + ": " + mBackStack.get(i));
                }
            }
        }

        FragTestManagerState fms = new FragTestManagerState();
        fms.mActive = active;
        fms.mAdded = added;
        fms.mBackStack = backStack;
        return fms;
    }

    void restoreAllState(Parcelable state, List<FragTest> nonConfig) {
        // If there is no saved state at all, then there can not be
        // any nonConfig fragTests either, so that is that.
        if (state == null) return;
        FragTestManagerState fms = (FragTestManagerState)state;
        if (fms.mActive == null) return;

        // First re-attach any non-config instances we are retaining back
        // to their saved state, so we don't try to instantiate them again.
        if (nonConfig != null) {
            for (int i=0; i<nonConfig.size(); i++) {
                FragTest f = nonConfig.get(i);
                if (DEBUG) Log.v(TAG, "restoreAllState: re-attaching retained " + f);
                FragTestState fs = fms.mActive[f.mIndex];
                fs.mInstance = f;
                f.mSavedViewState = null;
                f.mBackStackNesting = 0;
                f.mInLayout = false;
                f.mAdded = false;
                f.mTarget = null;
                if (fs.mSavedFragTestState != null) {
                    fs.mSavedFragTestState.setClassLoader(mHost.getContext().getClassLoader());
                    f.mSavedViewState = fs.mSavedFragTestState.getSparseParcelableArray(
                            FragTestManagerImpl.VIEW_STATE_TAG);
                    f.mSavedFragTestState = fs.mSavedFragTestState;
                }
            }
        }

        // Build the full list of active fragTests, instantiating them from
        // their saved state.
        mActive = new ArrayList<FragTest>(fms.mActive.length);
        if (mAvailIndices != null) {
            mAvailIndices.clear();
        }
        for (int i=0; i<fms.mActive.length; i++) {
            FragTestState fs = fms.mActive[i];
            if (fs != null) {
                FragTest f = fs.instantiate(mHost, mParent);
                if (DEBUG) Log.v(TAG, "restoreAllState: active #" + i + ": " + f);
                mActive.add(f);
                // Now that the fragTest is instantiated (or came from being
                // retained above), clear mInstance in case we end up re-restoring
                // from this FragTestState again.
                fs.mInstance = null;
            } else {
                mActive.add(null);
                if (mAvailIndices == null) {
                    mAvailIndices = new ArrayList<Integer>();
                }
                if (DEBUG) Log.v(TAG, "restoreAllState: avail #" + i);
                mAvailIndices.add(i);
            }
        }

        // Update the target of all retained fragTests.
        if (nonConfig != null) {
            for (int i=0; i<nonConfig.size(); i++) {
                FragTest f = nonConfig.get(i);
                if (f.mTargetIndex >= 0) {
                    if (f.mTargetIndex < mActive.size()) {
                        f.mTarget = mActive.get(f.mTargetIndex);
                    } else {
                        Log.w(TAG, "Re-attaching retained fragTest " + f
                                + " target no longer exists: " + f.mTargetIndex);
                        f.mTarget = null;
                    }
                }
            }
        }

        // Build the list of currently added fragTests.
        if (fms.mAdded != null) {
            mAdded = new ArrayList<FragTest>(fms.mAdded.length);
            for (int i=0; i<fms.mAdded.length; i++) {
                FragTest f = mActive.get(fms.mAdded[i]);
                if (f == null) {
                    throwException(new IllegalStateException(
                            "No instantiated fragTest for index #" + fms.mAdded[i]));
                }
                f.mAdded = true;
                if (DEBUG) Log.v(TAG, "restoreAllState: added #" + i + ": " + f);
                if (mAdded.contains(f)) {
                    throw new IllegalStateException("Already added!");
                }
                mAdded.add(f);
            }
        } else {
            mAdded = null;
        }

        // Build the back stack.
        if (fms.mBackStack != null) {
            mBackStack = new ArrayList<BackStackRecord>(fms.mBackStack.length);
            for (int i=0; i<fms.mBackStack.length; i++) {
                BackStackRecord bse = fms.mBackStack[i].instantiate(this);
                if (DEBUG) {
                    Log.v(TAG, "restoreAllState: back stack #" + i
                            + " (index " + bse.mIndex + "): " + bse);
                    LogWriter logw = new LogWriter(TAG);
                    PrintWriter pw = new PrintWriter(logw);
                    bse.dump("  ", pw, false);
                }
                mBackStack.add(bse);
                if (bse.mIndex >= 0) {
                    setBackStackIndex(bse.mIndex, bse);
                }
            }
        } else {
            mBackStack = null;
        }
    }

    public void attachController(FragTestHostCallback host,
                                 FragmentContainer container, FragTest parent) {
        if (mHost != null) throw new IllegalStateException("Already attached");
        mHost = host;
        mContainer = container;
        mParent = parent;
    }

    public void noteStateNotSaved() {
        mStateSaved = false;
    }

    public void dispatchCreate() {
        mStateSaved = false;
        moveToState(FragTest.CREATED, false);
    }

    public void dispatchActivityCreated() {
        mStateSaved = false;
        moveToState(FragTest.ACTIVITY_CREATED, false);
    }

    public void dispatchStart() {
        mStateSaved = false;
        moveToState(FragTest.STARTED, false);
    }

    public void dispatchResume() {
        mStateSaved = false;
        moveToState(FragTest.RESUMED, false);
    }

    public void dispatchPause() {
        moveToState(FragTest.STARTED, false);
    }

    public void dispatchStop() {
        // See saveAllState() for the explanation of this.  We do this for
        // all platform versions, to keep our behavior more consistent between
        // them.
        mStateSaved = true;

        moveToState(FragTest.STOPPED, false);
    }

    public void dispatchReallyStop() {
        moveToState(FragTest.ACTIVITY_CREATED, false);
    }

    public void dispatchDestroyView() {
        moveToState(FragTest.CREATED, false);
    }

    public void dispatchDestroy() {
        mDestroyed = true;
        execPendingActions();
        moveToState(FragTest.INITIALIZING, false);
        mHost = null;
        mContainer = null;
        mParent = null;
    }

    public void dispatchConfigurationChanged(Configuration newConfig) {
        if (mAdded != null) {
            for (int i=0; i<mAdded.size(); i++) {
                FragTest f = mAdded.get(i);
                if (f != null) {
                    f.performConfigurationChanged(newConfig);
                }
            }
        }
    }

    public void dispatchLowMemory() {
        if (mAdded != null) {
            for (int i=0; i<mAdded.size(); i++) {
                FragTest f = mAdded.get(i);
                if (f != null) {
                    f.performLowMemory();
                }
            }
        }
    }



    public static int reverseTransit(int transit) {
        int rev = 0;
        switch (transit) {
            case FragTestTransaction.TRANSIT_FRAGMENT_OPEN:
                rev = FragTestTransaction.TRANSIT_FRAGMENT_CLOSE;
                break;
            case FragTestTransaction.TRANSIT_FRAGMENT_CLOSE:
                rev = FragTestTransaction.TRANSIT_FRAGMENT_OPEN;
                break;
            case FragTestTransaction.TRANSIT_FRAGMENT_FADE:
                rev = FragTestTransaction.TRANSIT_FRAGMENT_FADE;
                break;
        }
        return rev;

    }

    public static final int ANIM_STYLE_OPEN_ENTER = 1;
    public static final int ANIM_STYLE_OPEN_EXIT = 2;
    public static final int ANIM_STYLE_CLOSE_ENTER = 3;
    public static final int ANIM_STYLE_CLOSE_EXIT = 4;
    public static final int ANIM_STYLE_FADE_ENTER = 5;
    public static final int ANIM_STYLE_FADE_EXIT = 6;

    public static int transitToStyleIndex(int transit, boolean enter) {
        int animAttr = -1;
        switch (transit) {
            case FragTestTransaction.TRANSIT_FRAGMENT_OPEN:
                animAttr = enter ? ANIM_STYLE_OPEN_ENTER : ANIM_STYLE_OPEN_EXIT;
                break;
            case FragTestTransaction.TRANSIT_FRAGMENT_CLOSE:
                animAttr = enter ? ANIM_STYLE_CLOSE_ENTER : ANIM_STYLE_CLOSE_EXIT;
                break;
            case FragTestTransaction.TRANSIT_FRAGMENT_FADE:
                animAttr = enter ? ANIM_STYLE_FADE_ENTER : ANIM_STYLE_FADE_EXIT;
                break;
        }
        return animAttr;
    }

    @Override
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        if (!"fragTest".equals(name)) {
            return null;
        }

        String fname = attrs.getAttributeValue(null, "class");
        TypedArray a =  context.obtainStyledAttributes(attrs, FragTestManagerImpl.FragTestTag.FragTest);
        if (fname == null) {
            fname = a.getString(FragTestManagerImpl.FragTestTag.FragTest_name);
        }
        // v_test
        int id = 0;
        String tag = "FragTestManagerImpl";
//        int id = a.getResourceId(FragTestManagerImpl.FragTestTag.FragTest_id, View.NO_ID);
//        String tag = a.getString(FragTestManagerImpl.FragTestTag.FragTest_tag);
        a.recycle();

        if (!FragTest.isSupportFragTestClass(mHost.getContext(), fname)) {
            // Invalid support lib fragTest; let the device's framework handle it.
            // This will allow android.app.FragTests to do the right thing.
            return null;
        }

        int containerId = parent != null ? parent.getId() : 0;
        if (containerId == View.NO_ID && id == View.NO_ID && tag == null) {
            throw new IllegalArgumentException(attrs.getPositionDescription()
                    + ": Must specify unique android:id, android:tag, or have a parent with an id for " + fname);
        }

        // If we restored from a previous state, we may already have
        // instantiated this fragTest from the state and should use
        // that instance instead of making a new one.
        FragTest fragTest = id != View.NO_ID ? findFragTestById(id) : null;
        if (fragTest == null && tag != null) {
            fragTest = findFragTestByTag(tag);
        }
        if (fragTest == null && containerId != View.NO_ID) {
            fragTest = findFragTestById(containerId);
        }

        if (FragTestManagerImpl.DEBUG) Log.v(TAG, "onCreateView: id=0x"
                + Integer.toHexString(id) + " fname=" + fname
                + " existing=" + fragTest);
        if (fragTest == null) {
            fragTest = FragTest.instantiate(context, fname);
            fragTest.mFromLayout = true;
            fragTest.mFragTestId = id != 0 ? id : containerId;
            fragTest.mContainerId = containerId;
            fragTest.mTag = tag;
            fragTest.mInLayout = true;
            fragTest.mFragTestManager = this;
            fragTest.mHost = mHost;
            fragTest.onInflate(mHost.getContext(), attrs, fragTest.mSavedFragTestState);
            addFragTest(fragTest, true);

        } else if (fragTest.mInLayout) {
            // A fragTest already exists and it is not one we restored from
            // previous state.
            throw new IllegalArgumentException(attrs.getPositionDescription()
                    + ": Duplicate id 0x" + Integer.toHexString(id)
                    + ", tag " + tag + ", or parent id 0x" + Integer.toHexString(containerId)
                    + " with another fragTest for " + fname);
        } else {
            // This fragTest was retained from a previous instance; get it
            // going now.
            fragTest.mInLayout = true;
            fragTest.mHost = mHost;
            // If this fragTest is newly instantiated (either right now, or
            // from last saved state), then give it the attributes to
            // initialize itself.
            if (!fragTest.mRetaining) {
                fragTest.onInflate(mHost.getContext(), attrs, fragTest.mSavedFragTestState);
            }
        }

        // If we haven't finished entering the CREATED state ourselves yet,
        // push the inflated child fragTest along.
        if (mCurState < FragTest.CREATED && fragTest.mFromLayout) {
            moveToState(fragTest, FragTest.CREATED, 0, 0, false);
        } else {
            moveToState(fragTest);
        }

        if (fragTest.mView == null) {
            throw new IllegalStateException("FragTest " + fname
                    + " did not create a view.");
        }
        if (id != 0) {
            fragTest.mView.setId(id);
        }
        if (fragTest.mView.getTag() == null) {
            fragTest.mView.setTag(tag);
        }
        return fragTest.mView;
    }

    LayoutInflaterFactory getLayoutInflaterFactory() {
        return this;
    }

    static class FragTestTag {
        public static final int[] FragTest = {
                0x01010003, 0x010100d0, 0x010100d1
        };
        public static final int FragTest_id = 1;
        public static final int FragTest_name = 0;
        public static final int FragTest_tag = 2;
    }
}
