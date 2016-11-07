package com.example.fragsrcsimplity;

import android.support.annotation.AnimRes;
import android.support.annotation.IdRes;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.view.View;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by zhouzhou on 2016/11/6.
 */

public abstract class FragTestTransaction {
    /**
     * Calls {@link #add(int, FragTest, String)} with a 0 containerViewId.
     */
    public abstract FragTestTransaction add(FragTest fragTest, String tag);

    /**
     * Calls {@link #add(int, FragTest, String)} with a null tag.
     */
    public abstract FragTestTransaction add(@IdRes int containerViewId, FragTest fragTest);

    /**
     * Add a fragTest to the activity state.  This fragTest may optionally
     * also have its view (if {@link FragTest#onCreateView FragTest.onCreateView}
     * returns non-null) into a container view of the activity.
     *
     * @param containerViewId Optional identifier of the container this fragTest is
     * to be placed in.  If 0, it will not be placed in a container.
     * @param fragTest The fragTest to be added.  This fragTest must not already
     * be added to the activity.
     * @param tag Optional tag name for the fragTest, to later retrieve the
     * fragTest with {@link FragTestManager#findFragTestByTag(String)
     * FragTestManager.findFragTestByTag(String)}.
     *
     * @return Returns the same FragTestTransaction instance.
     */
    public abstract FragTestTransaction add(@IdRes int containerViewId, FragTest fragTest,
                                                                   @Nullable String tag);

    /**
     * Calls {@link #replace(int, FragTest, String)} with a null tag.
     */
    public abstract FragTestTransaction replace(@IdRes int containerViewId, FragTest fragTest);

    /**
     * Replace an existing fragTest that was added to a container.  This is
     * essentially the same as calling {@link #remove(FragTest)} for all
     * currently added fragTests that were added with the same containerViewId
     * and then {@link #add(int, FragTest, String)} with the same arguments
     * given here.
     *
     * @param containerViewId Identifier of the container whose fragTest(s) are
     * to be replaced.
     * @param fragTest The new fragTest to place in the container.
     * @param tag Optional tag name for the fragTest, to later retrieve the
     * fragTest with {@link FragTestManager#findFragTestByTag(String)
     * FragTestManager.findFragTestByTag(String)}.
     *
     * @return Returns the same FragTestTransaction instance.
     */
    public abstract FragTestTransaction replace(@IdRes int containerViewId, FragTest fragTest,
                                                                       @Nullable String tag);

    /**
     * Remove an existing fragTest.  If it was added to a container, its view
     * is also removed from that container.
     *
     * @param fragTest The fragTest to be removed.
     *
     * @return Returns the same FragTestTransaction instance.
     */
    public abstract FragTestTransaction remove(FragTest fragTest);

    /**
     * Hides an existing fragTest.  This is only relevant for fragTests whose
     * views have been added to a container, as this will cause the view to
     * be hidden.
     *
     * @param fragTest The fragTest to be hidden.
     *
     * @return Returns the same FragTestTransaction instance.
     */
    public abstract FragTestTransaction hide(FragTest fragTest);

    /**
     * Shows a previously hidden fragTest.  This is only relevant for fragTests whose
     * views have been added to a container, as this will cause the view to
     * be shown.
     *
     * @param fragTest The fragTest to be shown.
     *
     * @return Returns the same FragTestTransaction instance.
     */
    public abstract FragTestTransaction show(FragTest fragTest);

    /**
     * Detach the given fragTest from the UI.  This is the same state as
     * when it is put on the back stack: the fragTest is removed from
     * the UI, however its state is still being actively managed by the
     * fragTest manager.  When going into this state its view hierarchy
     * is destroyed.
     *
     * @param fragTest The fragTest to be detached.
     *
     * @return Returns the same FragTestTransaction instance.
     */
    public abstract FragTestTransaction detach(FragTest fragTest);

    /**
     * Re-attach a fragTest after it had previously been deatched from
     * the UI with {@link #detach(FragTest)}.  This
     * causes its view hierarchy to be re-created, attached to the UI,
     * and displayed.
     *
     * @param fragTest The fragTest to be attached.
     *
     * @return Returns the same FragTestTransaction instance.
     */
    public abstract FragTestTransaction attach(FragTest fragTest);

    /**
     * @return <code>true</code> if this transaction contains no operations,
     * <code>false</code> otherwise.
     */
    public abstract boolean isEmpty();

    /**
     * Bit mask that is set for all enter transitions.
     */
    public static final int TRANSIT_ENTER_MASK = 0x1000;

    /**
     * Bit mask that is set for all exit transitions.
     */
    public static final int TRANSIT_EXIT_MASK = 0x2000;

    /** @hide */
    @IntDef({TRANSIT_NONE, TRANSIT_FRAGMENT_OPEN, TRANSIT_FRAGMENT_CLOSE})
    @Retention(RetentionPolicy.SOURCE)
    private @interface Transit {}

    /** Not set up for a transition. */
    public static final int TRANSIT_UNSET = -1;
    /** No animation for transition. */
    public static final int TRANSIT_NONE = 0;
    /** FragTest is being added onto the stack */
    public static final int TRANSIT_FRAGMENT_OPEN = 1 | TRANSIT_ENTER_MASK;
    /** FragTest is being removed from the stack */
    public static final int TRANSIT_FRAGMENT_CLOSE = 2 | TRANSIT_EXIT_MASK;
    /** FragTest should simply fade in or out; that is, no strong navigation associated
     * with it except that it is appearing or disappearing for some reason. */
    public static final int TRANSIT_FRAGMENT_FADE = 3 | TRANSIT_ENTER_MASK;

    /**
     * Set specific animation resources to run for the fragTests that are
     * entering and exiting in this transaction. These animations will not be
     * played when popping the back stack.
     */
    public abstract FragTestTransaction setCustomAnimations(@AnimRes int enter,
                                                                                   @AnimRes int exit);

    /**
     * Set specific animation resources to run for the fragTests that are
     * entering and exiting in this transaction. The <code>popEnter</code>
     * and <code>popExit</code> animations will be played for enter/exit
     * operations specifically when popping the back stack.
     */
    public abstract FragTestTransaction setCustomAnimations(@AnimRes int enter,
                                                                                   @AnimRes int exit, @AnimRes int popEnter, @AnimRes int popExit);

    /**
     * Used with custom Transitions to map a View from a removed or hidden
     * FragTest to a View from a shown or added FragTest.
     * <var>sharedElement</var> must have a unique transitionName in the View hierarchy.
     *
     * @param sharedElement A View in a disappearing FragTest to match with a View in an
     *                      appearing FragTest.
     * @param name The transitionName for a View in an appearing FragTest to match to the shared
     *             element.
     * @see FragTest#setSharedElementReturnTransition(Object)
     * @see FragTest#setSharedElementEnterTransition(Object)
     */
    public abstract FragTestTransaction addSharedElement(View sharedElement, String name);

    /**
     * Select a standard transition animation for this transaction.  May be
     * one of {@link #TRANSIT_NONE}, {@link #TRANSIT_FRAGMENT_OPEN},
     * or {@link #TRANSIT_FRAGMENT_CLOSE}
     */
    public abstract FragTestTransaction setTransition(@Transit int transit);

    /**
     * Set a custom style resource that will be used for resolving transit
     * animations.
     */
    public abstract FragTestTransaction setTransitionStyle(@StyleRes int styleRes);

    /**
     * Add this transaction to the back stack.  This means that the transaction
     * will be remembered after it is committed, and will reverse its operation
     * when later popped off the stack.
     *
     * @param name An optional name for this back stack state, or null.
     */
    public abstract FragTestTransaction addToBackStack(@Nullable String name);

    /**
     * Returns true if this FragTestTransaction is allowed to be added to the back
     * stack. If this method would return false, {@link #addToBackStack(String)}
     * will throw {@link IllegalStateException}.
     *
     * @return True if {@link #addToBackStack(String)} is permitted on this transaction.
     */
    public abstract boolean isAddToBackStackAllowed();

    /**
     * Disallow calls to {@link #addToBackStack(String)}. Any future calls to
     * addToBackStack will throw {@link IllegalStateException}. If addToBackStack
     * has already been called, this method will throw IllegalStateException.
     */
    public abstract FragTestTransaction disallowAddToBackStack();

    /**
     * Set the full title to show as a bread crumb when this transaction
     * is on the back stack.
     *
     * @param res A string resource containing the title.
     */
    public abstract FragTestTransaction setBreadCrumbTitle(@StringRes int res);

    /**
     * Like {@link #setBreadCrumbTitle(int)} but taking a raw string; this
     * method is <em>not</em> recommended, as the string can not be changed
     * later if the locale changes.
     */
    public abstract FragTestTransaction setBreadCrumbTitle(CharSequence text);

    /**
     * Set the short title to show as a bread crumb when this transaction
     * is on the back stack.
     *
     * @param res A string resource containing the title.
     */
    public abstract FragTestTransaction setBreadCrumbShortTitle(@StringRes int res);

    /**
     * Like {@link #setBreadCrumbShortTitle(int)} but taking a raw string; this
     * method is <em>not</em> recommended, as the string can not be changed
     * later if the locale changes.
     */
    public abstract FragTestTransaction setBreadCrumbShortTitle(CharSequence text);

    /**
     * Schedules a commit of this transaction.  The commit does
     * not happen immediately; it will be scheduled as work on the main thread
     * to be done the next time that thread is ready.
     *
     * <p class="note">A transaction can only be committed with this method
     * prior to its containing activity saving its state.  If the commit is
     * attempted after that point, an exception will be thrown.  This is
     * because the state after the commit can be lost if the activity needs to
     * be restored from its state.  See {@link #commitAllowingStateLoss()} for
     * situations where it may be okay to lose the commit.</p>
     *
     * @return Returns the identifier of this transaction's back stack entry,
     * if {@link #addToBackStack(String)} had been called.  Otherwise, returns
     * a negative number.
     */
    public abstract int commit();

    /**
     * Like {@link #commit} but allows the commit to be executed after an
     * activity's state is saved.  This is dangerous because the commit can
     * be lost if the activity needs to later be restored from its state, so
     * this should only be used for cases where it is okay for the UI state
     * to change unexpectedly on the user.
     */
    public abstract int commitAllowingStateLoss();
}

