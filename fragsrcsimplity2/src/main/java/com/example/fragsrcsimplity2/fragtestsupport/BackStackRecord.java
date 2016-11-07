package com.example.fragsrcsimplity2.fragtestsupport;

/**
 * Created by zhouzhou on 2016/11/6.
 */


import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.SharedElementCallback;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.LogWriter;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.example.fragsrcsimplity2.FragTestManagerImpl;
import com.example.fragsrcsimplity2.FragTestTransitionCompat21;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;

final class BackStackState implements Parcelable {
    final int[] mOps;
    final int mTransition;
    final int mTransitionStyle;
    final String mName;
    final int mIndex;
    final int mBreadCrumbTitleRes;
    final CharSequence mBreadCrumbTitleText;
    final int mBreadCrumbShortTitleRes;
    final CharSequence mBreadCrumbShortTitleText;
    final ArrayList<String> mSharedElementSourceNames;
    final ArrayList<String> mSharedElementTargetNames;

    public BackStackState(BackStackRecord bse) {
        int numRemoved = 0;
        BackStackRecord.Op op = bse.mHead;
        while (op != null) {
            if (op.removed != null) numRemoved += op.removed.size();
            op = op.next;
        }
        mOps = new int[bse.mNumOp*7 + numRemoved];

        if (!bse.mAddToBackStack) {
            throw new IllegalStateException("Not on back stack");
        }

        op = bse.mHead;
        int pos = 0;
        while (op != null) {
            mOps[pos++] = op.cmd;
            mOps[pos++] = op.fragment != null ? op.fragment.mIndex : -1;
            mOps[pos++] = op.enterAnim;
            mOps[pos++] = op.exitAnim;
            mOps[pos++] = op.popEnterAnim;
            mOps[pos++] = op.popExitAnim;
            if (op.removed != null) {
                final int N = op.removed.size();
                mOps[pos++] = N;
                for (int i=0; i<N; i++) {
                    mOps[pos++] = op.removed.get(i).mIndex;
                }
            } else {
                mOps[pos++] = 0;
            }
            op = op.next;
        }
        mTransition = bse.mTransition;
        mTransitionStyle = bse.mTransitionStyle;
        mName = bse.mName;
        mIndex = bse.mIndex;
        mBreadCrumbTitleRes = bse.mBreadCrumbTitleRes;
        mBreadCrumbTitleText = bse.mBreadCrumbTitleText;
        mBreadCrumbShortTitleRes = bse.mBreadCrumbShortTitleRes;
        mBreadCrumbShortTitleText = bse.mBreadCrumbShortTitleText;
        mSharedElementSourceNames = bse.mSharedElementSourceNames;
        mSharedElementTargetNames = bse.mSharedElementTargetNames;
    }

    public BackStackState(Parcel in) {
        mOps = in.createIntArray();
        mTransition = in.readInt();
        mTransitionStyle = in.readInt();
        mName = in.readString();
        mIndex = in.readInt();
        mBreadCrumbTitleRes = in.readInt();
        mBreadCrumbTitleText = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        mBreadCrumbShortTitleRes = in.readInt();
        mBreadCrumbShortTitleText = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        mSharedElementSourceNames = in.createStringArrayList();
        mSharedElementTargetNames = in.createStringArrayList();
    }

    public BackStackRecord instantiate(FragTestManagerImpl fm) {
        BackStackRecord bse = new BackStackRecord(fm);
        int pos = 0;
        int num = 0;
        while (pos < mOps.length) {
            BackStackRecord.Op op = new BackStackRecord.Op();
            op.cmd = mOps[pos++];
            if (FragTestManagerImpl.DEBUG) Log.v(FragTestManagerImpl.TAG,
                    "Instantiate " + bse + " op #" + num + " base fragment #" + mOps[pos]);
            int findex = mOps[pos++];
            if (findex >= 0) {
                FragTest f = fm.mActive.get(findex);
                op.fragment = f;
            } else {
                op.fragment = null;
            }
            op.enterAnim = mOps[pos++];
            op.exitAnim = mOps[pos++];
            op.popEnterAnim = mOps[pos++];
            op.popExitAnim = mOps[pos++];
            final int N = mOps[pos++];
            if (N > 0) {
                op.removed = new ArrayList<FragTest>(N);
                for (int i=0; i<N; i++) {
                    if (FragTestManagerImpl.DEBUG) Log.v(FragTestManagerImpl.TAG,
                            "Instantiate " + bse + " set remove fragment #" + mOps[pos]);
                    FragTest r = fm.mActive.get(mOps[pos++]);
                    op.removed.add(r);
                }
            }
            bse.mEnterAnim = op.enterAnim;
            bse.mExitAnim = op.exitAnim;
            bse.mPopEnterAnim = op.popEnterAnim;
            bse.mPopExitAnim = op.popExitAnim;
            bse.addOp(op);
            num++;
        }
        bse.mTransition = mTransition;
        bse.mTransitionStyle = mTransitionStyle;
        bse.mName = mName;
        bse.mIndex = mIndex;
        bse.mAddToBackStack = true;
        bse.mBreadCrumbTitleRes = mBreadCrumbTitleRes;
        bse.mBreadCrumbTitleText = mBreadCrumbTitleText;
        bse.mBreadCrumbShortTitleRes = mBreadCrumbShortTitleRes;
        bse.mBreadCrumbShortTitleText = mBreadCrumbShortTitleText;
        bse.mSharedElementSourceNames = mSharedElementSourceNames;
        bse.mSharedElementTargetNames = mSharedElementTargetNames;
        bse.bumpBackStackNesting(1);
        return bse;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeIntArray(mOps);
        dest.writeInt(mTransition);
        dest.writeInt(mTransitionStyle);
        dest.writeString(mName);
        dest.writeInt(mIndex);
        dest.writeInt(mBreadCrumbTitleRes);
        TextUtils.writeToParcel(mBreadCrumbTitleText, dest, 0);
        dest.writeInt(mBreadCrumbShortTitleRes);
        TextUtils.writeToParcel(mBreadCrumbShortTitleText, dest, 0);
        dest.writeStringList(mSharedElementSourceNames);
        dest.writeStringList(mSharedElementTargetNames);
    }

    public static final Creator<BackStackState> CREATOR
            = new Creator<BackStackState>() {
        public BackStackState createFromParcel(Parcel in) {
            return new BackStackState(in);
        }

        public BackStackState[] newArray(int size) {
            return new BackStackState[size];
        }
    };
}

/**
 * @hide Entry of an operation on the fragment back stack.
 */
final class BackStackRecord extends FragTestTransaction implements
        FragTestManager.BackStackEntry, Runnable {
    static final String TAG = FragTestManagerImpl.TAG;
    static final boolean SUPPORTS_TRANSITIONS = Build.VERSION.SDK_INT >= 21;

    final FragTestManagerImpl mManager;

    static final int OP_NULL = 0;
    static final int OP_ADD = 1;
    static final int OP_REPLACE = 2;
    static final int OP_REMOVE = 3;
    static final int OP_HIDE = 4;
    static final int OP_SHOW = 5;
    static final int OP_DETACH = 6;
    static final int OP_ATTACH = 7;

    static final class Op {
        BackStackRecord.Op next;
        BackStackRecord.Op prev;
        int cmd;
        FragTest fragment;
        int enterAnim;
        int exitAnim;
        int popEnterAnim;
        int popExitAnim;
        ArrayList<FragTest> removed;
    }

    BackStackRecord.Op mHead;
    BackStackRecord.Op mTail;
    int mNumOp;
    int mEnterAnim;
    int mExitAnim;
    int mPopEnterAnim;
    int mPopExitAnim;
    int mTransition;
    int mTransitionStyle;
    boolean mAddToBackStack;
    boolean mAllowAddToBackStack = true;
    String mName;
    boolean mCommitted;
    int mIndex = -1;

    int mBreadCrumbTitleRes;
    CharSequence mBreadCrumbTitleText;
    int mBreadCrumbShortTitleRes;
    CharSequence mBreadCrumbShortTitleText;

    ArrayList<String> mSharedElementSourceNames;
    ArrayList<String> mSharedElementTargetNames;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("BackStackEntry{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        if (mIndex >= 0) {
            sb.append(" #");
            sb.append(mIndex);
        }
        if (mName != null) {
            sb.append(" ");
            sb.append(mName);
        }
        sb.append("}");
        return sb.toString();
    }

    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        dump(prefix, writer, true);
    }

    public void dump(String prefix, PrintWriter writer, boolean full) {
        if (full) {
            writer.print(prefix); writer.print("mName="); writer.print(mName);
            writer.print(" mIndex="); writer.print(mIndex);
            writer.print(" mCommitted="); writer.println(mCommitted);
            if (mTransition != FragTestTransaction.TRANSIT_NONE) {
                writer.print(prefix); writer.print("mTransition=#");
                writer.print(Integer.toHexString(mTransition));
                writer.print(" mTransitionStyle=#");
                writer.println(Integer.toHexString(mTransitionStyle));
            }
            if (mEnterAnim != 0 || mExitAnim !=0) {
                writer.print(prefix); writer.print("mEnterAnim=#");
                writer.print(Integer.toHexString(mEnterAnim));
                writer.print(" mExitAnim=#");
                writer.println(Integer.toHexString(mExitAnim));
            }
            if (mPopEnterAnim != 0 || mPopExitAnim !=0) {
                writer.print(prefix); writer.print("mPopEnterAnim=#");
                writer.print(Integer.toHexString(mPopEnterAnim));
                writer.print(" mPopExitAnim=#");
                writer.println(Integer.toHexString(mPopExitAnim));
            }
            if (mBreadCrumbTitleRes != 0 || mBreadCrumbTitleText != null) {
                writer.print(prefix); writer.print("mBreadCrumbTitleRes=#");
                writer.print(Integer.toHexString(mBreadCrumbTitleRes));
                writer.print(" mBreadCrumbTitleText=");
                writer.println(mBreadCrumbTitleText);
            }
            if (mBreadCrumbShortTitleRes != 0 || mBreadCrumbShortTitleText != null) {
                writer.print(prefix); writer.print("mBreadCrumbShortTitleRes=#");
                writer.print(Integer.toHexString(mBreadCrumbShortTitleRes));
                writer.print(" mBreadCrumbShortTitleText=");
                writer.println(mBreadCrumbShortTitleText);
            }
        }

        if (mHead != null) {
            writer.print(prefix); writer.println("Operations:");
            String innerPrefix = prefix + "    ";
            BackStackRecord.Op op = mHead;
            int num = 0;
            while (op != null) {
                String cmdStr;
                switch (op.cmd) {
                    case OP_NULL: cmdStr="NULL"; break;
                    case OP_ADD: cmdStr="ADD"; break;
                    case OP_REPLACE: cmdStr="REPLACE"; break;
                    case OP_REMOVE: cmdStr="REMOVE"; break;
                    case OP_HIDE: cmdStr="HIDE"; break;
                    case OP_SHOW: cmdStr="SHOW"; break;
                    case OP_DETACH: cmdStr="DETACH"; break;
                    case OP_ATTACH: cmdStr="ATTACH"; break;
                    default: cmdStr="cmd=" + op.cmd; break;
                }
                writer.print(prefix); writer.print("  Op #"); writer.print(num);
                writer.print(": "); writer.print(cmdStr);
                writer.print(" "); writer.println(op.fragment);
                if (full) {
                    if (op.enterAnim != 0 || op.exitAnim != 0) {
                        writer.print(prefix); writer.print("enterAnim=#");
                        writer.print(Integer.toHexString(op.enterAnim));
                        writer.print(" exitAnim=#");
                        writer.println(Integer.toHexString(op.exitAnim));
                    }
                    if (op.popEnterAnim != 0 || op.popExitAnim != 0) {
                        writer.print(prefix); writer.print("popEnterAnim=#");
                        writer.print(Integer.toHexString(op.popEnterAnim));
                        writer.print(" popExitAnim=#");
                        writer.println(Integer.toHexString(op.popExitAnim));
                    }
                }
                if (op.removed != null && op.removed.size() > 0) {
                    for (int i=0; i<op.removed.size(); i++) {
                        writer.print(innerPrefix);
                        if (op.removed.size() == 1) {
                            writer.print("Removed: ");
                        } else {
                            if (i == 0) {
                                writer.println("Removed:");
                            }
                            writer.print(innerPrefix); writer.print("  #"); writer.print(i);
                            writer.print(": ");
                        }
                        writer.println(op.removed.get(i));
                    }
                }
                op = op.next;
                num++;
            }
        }
    }

    public BackStackRecord(FragTestManagerImpl manager) {
        mManager = manager;
    }

    public int getId() {
        return mIndex;
    }

    public int getBreadCrumbTitleRes() {
        return mBreadCrumbTitleRes;
    }

    public int getBreadCrumbShortTitleRes() {
        return mBreadCrumbShortTitleRes;
    }

    public CharSequence getBreadCrumbTitle() {
        if (mBreadCrumbTitleRes != 0) {
            return mManager.mHost.getContext().getText(mBreadCrumbTitleRes);
        }
        return mBreadCrumbTitleText;
    }

    public CharSequence getBreadCrumbShortTitle() {
        if (mBreadCrumbShortTitleRes != 0) {
            return mManager.mHost.getContext().getText(mBreadCrumbShortTitleRes);
        }
        return mBreadCrumbShortTitleText;
    }

    void addOp(BackStackRecord.Op op) {
        if (mHead == null) {
            mHead = mTail = op;
        } else {
            op.prev = mTail;
            mTail.next = op;
            mTail = op;
        }
        op.enterAnim = mEnterAnim;
        op.exitAnim = mExitAnim;
        op.popEnterAnim = mPopEnterAnim;
        op.popExitAnim = mPopExitAnim;
        mNumOp++;
    }

    public FragTestTransaction add(FragTest fragment, String tag) {
        doAddOp(0, fragment, tag, OP_ADD);
        return this;
    }

    public FragTestTransaction add(int containerViewId, FragTest fragment) {
        doAddOp(containerViewId, fragment, null, OP_ADD);
        return this;
    }

    public FragTestTransaction add(int containerViewId, FragTest fragment, String tag) {
        doAddOp(containerViewId, fragment, tag, OP_ADD);
        return this;
    }

    private void doAddOp(int containerViewId, FragTest fragment, String tag, int opcmd) {
        fragment.mFragTestManager = mManager;

        if (tag != null) {
            if (fragment.mTag != null && !tag.equals(fragment.mTag)) {
                throw new IllegalStateException("Can't change tag of fragment "
                        + fragment + ": was " + fragment.mTag
                        + " now " + tag);
            }
            fragment.mTag = tag;
        }

        if (containerViewId != 0) {
            if (fragment.mFragTestId != 0 && fragment.mFragTestId != containerViewId) {
                throw new IllegalStateException("Can't change container ID of fragment "
                        + fragment + ": was " + fragment.mFragTestId
                        + " now " + containerViewId);
            }
            fragment.mContainerId = fragment.mFragTestId = containerViewId;
        }

        BackStackRecord.Op op = new BackStackRecord.Op();
        op.cmd = opcmd;
        op.fragment = fragment;
        addOp(op);
    }

    public FragTestTransaction replace(int containerViewId, FragTest fragment) {
        return replace(containerViewId, fragment, null);
    }

    public FragTestTransaction replace(int containerViewId, FragTest fragment, String tag) {
        if (containerViewId == 0) {
            throw new IllegalArgumentException("Must use non-zero containerViewId");
        }

        doAddOp(containerViewId, fragment, tag, OP_REPLACE);
        return this;
    }

    public FragTestTransaction remove(FragTest fragment) {
        BackStackRecord.Op op = new BackStackRecord.Op();
        op.cmd = OP_REMOVE;
        op.fragment = fragment;
        addOp(op);

        return this;
    }

    public FragTestTransaction hide(FragTest fragment) {
        BackStackRecord.Op op = new BackStackRecord.Op();
        op.cmd = OP_HIDE;
        op.fragment = fragment;
        addOp(op);

        return this;
    }

    public FragTestTransaction show(FragTest fragment) {
        BackStackRecord.Op op = new BackStackRecord.Op();
        op.cmd = OP_SHOW;
        op.fragment = fragment;
        addOp(op);

        return this;
    }

    public FragTestTransaction detach(FragTest fragment) {
        BackStackRecord.Op op = new BackStackRecord.Op();
        op.cmd = OP_DETACH;
        op.fragment = fragment;
        addOp(op);

        return this;
    }

    public FragTestTransaction attach(FragTest fragment) {
        BackStackRecord.Op op = new BackStackRecord.Op();
        op.cmd = OP_ATTACH;
        op.fragment = fragment;
        addOp(op);

        return this;
    }

    public FragTestTransaction setCustomAnimations(int enter, int exit) {
        return setCustomAnimations(enter, exit, 0, 0);
    }

    public FragTestTransaction setCustomAnimations(int enter, int exit,
                                                   int popEnter, int popExit) {
        mEnterAnim = enter;
        mExitAnim = exit;
        mPopEnterAnim = popEnter;
        mPopExitAnim = popExit;
        return this;
    }

    public FragTestTransaction setTransition(int transition) {
        mTransition = transition;
        return this;
    }

    @Override
    public FragTestTransaction addSharedElement(View sharedElement, String name) {
//        if (SUPPORTS_TRANSITIONS) {
//            String transitionName = FragTestTransitionCompat21.getTransitionName(sharedElement);
//            if (transitionName == null) {
//                throw new IllegalArgumentException("Unique transitionNames are required for all" +
//                        " sharedElements");
//            }
//            if (mSharedElementSourceNames == null) {
//                mSharedElementSourceNames = new ArrayList<String>();
//                mSharedElementTargetNames = new ArrayList<String>();
//            }
//
//            mSharedElementSourceNames.add(transitionName);
//            mSharedElementTargetNames.add(name);
//        }
        return this;
    }

    public FragTestTransaction setTransitionStyle(int styleRes) {
        mTransitionStyle = styleRes;
        return this;
    }

    public FragTestTransaction addToBackStack(String name) {
        if (!mAllowAddToBackStack) {
            throw new IllegalStateException(
                    "This FragTestTransaction is not allowed to be added to the back stack.");
        }
        mAddToBackStack = true;
        mName = name;
        return this;
    }

    public boolean isAddToBackStackAllowed() {
        return mAllowAddToBackStack;
    }

    public FragTestTransaction disallowAddToBackStack() {
        if (mAddToBackStack) {
            throw new IllegalStateException(
                    "This transaction is already being added to the back stack");
        }
        mAllowAddToBackStack = false;
        return this;
    }

    public FragTestTransaction setBreadCrumbTitle(int res) {
        mBreadCrumbTitleRes = res;
        mBreadCrumbTitleText = null;
        return this;
    }

    public FragTestTransaction setBreadCrumbTitle(CharSequence text) {
        mBreadCrumbTitleRes = 0;
        mBreadCrumbTitleText = text;
        return this;
    }

    public FragTestTransaction setBreadCrumbShortTitle(int res) {
        mBreadCrumbShortTitleRes = res;
        mBreadCrumbShortTitleText = null;
        return this;
    }

    public FragTestTransaction setBreadCrumbShortTitle(CharSequence text) {
        mBreadCrumbShortTitleRes = 0;
        mBreadCrumbShortTitleText = text;
        return this;
    }

    void bumpBackStackNesting(int amt) {
        if (!mAddToBackStack) {
            return;
        }
        if (FragTestManagerImpl.DEBUG) Log.v(TAG, "Bump nesting in " + this
                + " by " + amt);
        BackStackRecord.Op op = mHead;
        while (op != null) {
            if (op.fragment != null) {
                op.fragment.mBackStackNesting += amt;
                if (FragTestManagerImpl.DEBUG) Log.v(TAG, "Bump nesting of "
                        + op.fragment + " to " + op.fragment.mBackStackNesting);
            }
            if (op.removed != null) {
                for (int i=op.removed.size()-1; i>=0; i--) {
                    FragTest r = op.removed.get(i);
                    r.mBackStackNesting += amt;
                    if (FragTestManagerImpl.DEBUG) Log.v(TAG, "Bump nesting of "
                            + r + " to " + r.mBackStackNesting);
                }
            }
            op = op.next;
        }
    }

    public int commit() {
        return commitInternal(false);
    }

    public int commitAllowingStateLoss() {
        return commitInternal(true);
    }

    int commitInternal(boolean allowStateLoss) {
        if (mCommitted) throw new IllegalStateException("commit already called");
        if (FragTestManagerImpl.DEBUG) {
            Log.v(TAG, "Commit: " + this);
            LogWriter logw = new LogWriter(TAG);
            PrintWriter pw = new PrintWriter(logw);
            dump("  ", null, pw, null);
        }
        mCommitted = true;
        if (mAddToBackStack) {
            mIndex = mManager.allocBackStackIndex(this);
        } else {
            mIndex = -1;
        }
        mManager.enqueueAction(this, allowStateLoss);
        return mIndex;
    }

    public void run() {
        if (FragTestManagerImpl.DEBUG) Log.v(TAG, "Run: " + this);

        if (mAddToBackStack) {
            if (mIndex < 0) {
                throw new IllegalStateException("addToBackStack() called after commit()");
            }
        }

        bumpBackStackNesting(1);

        BackStackRecord.TransitionState state = null;
        SparseArray<FragTest> firstOutFragTests = null;
        SparseArray<FragTest> lastInFragTests = null;
        if (SUPPORTS_TRANSITIONS && mManager.mCurState >= FragTest.CREATED) {
            firstOutFragTests = new SparseArray<FragTest>();
            lastInFragTests = new SparseArray<FragTest>();

            calculateFragTests(firstOutFragTests, lastInFragTests);

            state = beginTransition(firstOutFragTests, lastInFragTests, false);
        }

        int transitionStyle = state != null ? 0 : mTransitionStyle;
        int transition = state != null ? 0 : mTransition;
        BackStackRecord.Op op = mHead;
        while (op != null) {
            int enterAnim = state != null ? 0 : op.enterAnim;
            int exitAnim = state != null ? 0 : op.exitAnim;
            switch (op.cmd) {
                case OP_ADD: {
                    FragTest f = op.fragment;
                    f.mNextAnim = enterAnim;
                    mManager.addFragTest(f, false);
                } break;
                case OP_REPLACE: {
                    FragTest f = op.fragment;
                    int containerId = f.mContainerId;
                    if (mManager.mAdded != null) {
                        for (int i = mManager.mAdded.size() - 1; i >= 0; i--) {
                            FragTest old = mManager.mAdded.get(i);
                            if (FragTestManagerImpl.DEBUG) Log.v(TAG,
                                    "OP_REPLACE: adding=" + f + " old=" + old);
                            if (old.mContainerId == containerId) {
                                if (old == f) {
                                    op.fragment = f = null;
                                } else {
                                    if (op.removed == null) {
                                        op.removed = new ArrayList<FragTest>();
                                    }
                                    op.removed.add(old);
                                    old.mNextAnim = exitAnim;
                                    if (mAddToBackStack) {
                                        old.mBackStackNesting += 1;
                                        if (FragTestManagerImpl.DEBUG) Log.v(TAG, "Bump nesting of "
                                                + old + " to " + old.mBackStackNesting);
                                    }
                                    mManager.removeFragTest(old, transition, transitionStyle);
                                }
                            }
                        }
                    }
                    if (f != null) {
                        f.mNextAnim = enterAnim;
                        mManager.addFragTest(f, false);
                    }
                } break;
                case OP_REMOVE: {
                    FragTest f = op.fragment;
                    f.mNextAnim = exitAnim;
                    mManager.removeFragTest(f, transition, transitionStyle);
                } break;
                case OP_HIDE: {
                    FragTest f = op.fragment;
                    f.mNextAnim = exitAnim;
                    mManager.hideFragTest(f, transition, transitionStyle);
                } break;
                case OP_SHOW: {
                    FragTest f = op.fragment;
                    f.mNextAnim = enterAnim;
                    mManager.showFragTest(f, transition, transitionStyle);
                } break;
                case OP_DETACH: {
                    FragTest f = op.fragment;
                    f.mNextAnim = exitAnim;
                    mManager.detachFragTest(f, transition, transitionStyle);
                } break;
                case OP_ATTACH: {
                    FragTest f = op.fragment;
                    f.mNextAnim = enterAnim;
                    mManager.attachFragTest(f, transition, transitionStyle);
                } break;
                default: {
                    throw new IllegalArgumentException("Unknown cmd: " + op.cmd);
                }
            }

            op = op.next;
        }

        mManager.moveToState(mManager.mCurState, transition, transitionStyle, true);

        if (mAddToBackStack) {
            mManager.addBackStackState(this);
        }
    }

    private static void setFirstOut(SparseArray<FragTest> firstOutFragTests,
                                    SparseArray<FragTest> lastInFragTests, FragTest fragment) {
        if (fragment != null) {
            int containerId = fragment.mContainerId;
            if (containerId != 0 && !fragment.isHidden()) {
                if (fragment.isAdded() && fragment.getView() != null
                        && firstOutFragTests.get(containerId) == null) {
                    firstOutFragTests.put(containerId, fragment);
                }
                if (lastInFragTests.get(containerId) == fragment) {
                    lastInFragTests.remove(containerId);
                }
            }
        }
    }

    private void setLastIn(SparseArray<FragTest> firstOutFragTests,
                           SparseArray<FragTest> lastInFragTests, FragTest fragment) {
        if (fragment != null) {
            int containerId = fragment.mContainerId;
            if (containerId != 0) {
                if (!fragment.isAdded()) {
                    lastInFragTests.put(containerId, fragment);
                }
                if (firstOutFragTests.get(containerId) == fragment) {
                    firstOutFragTests.remove(containerId);
                }
            }
            if (fragment.mState < FragTest.CREATED && mManager.mCurState >= FragTest.CREATED) {
                mManager.makeActive(fragment);
                mManager.moveToState(fragment, FragTest.CREATED, 0, 0, false);
            }
        }
    }

    /**
     * Finds the first removed fragment and last added fragments when going forward.
     * If none of the fragments have transitions, then both lists will be empty.
     *
     * @param firstOutFragTests The list of first fragments to be removed, keyed on the
     *                          container ID. This list will be modified by the method.
     * @param lastInFragTests The list of last fragments to be added, keyed on the
     *                        container ID. This list will be modified by the method.
     */
    private void calculateFragTests(SparseArray<FragTest> firstOutFragTests,
                                    SparseArray<FragTest> lastInFragTests) {
        if (!mManager.mContainer.onHasView()) {
            return; // nothing to see, so no transitions
        }
        BackStackRecord.Op op = mHead;
        while (op != null) {
            switch (op.cmd) {
                case OP_ADD:
                    setLastIn(firstOutFragTests, lastInFragTests, op.fragment);
                    break;
                case OP_REPLACE: {
                    FragTest f = op.fragment;
                    if (mManager.mAdded != null) {
                        for (int i = 0; i < mManager.mAdded.size(); i++) {
                            FragTest old = mManager.mAdded.get(i);
                            if (f == null || old.mContainerId == f.mContainerId) {
                                if (old == f) {
                                    f = null;
                                    lastInFragTests.remove(old.mContainerId);
                                } else {
                                    setFirstOut(firstOutFragTests, lastInFragTests, old);
                                }
                            }
                        }
                    }
                    setLastIn(firstOutFragTests, lastInFragTests, op.fragment);
                    break;
                }
                case OP_REMOVE:
                    setFirstOut(firstOutFragTests, lastInFragTests, op.fragment);
                    break;
                case OP_HIDE:
                    setFirstOut(firstOutFragTests, lastInFragTests, op.fragment);
                    break;
                case OP_SHOW:
                    setLastIn(firstOutFragTests, lastInFragTests, op.fragment);
                    break;
                case OP_DETACH:
                    setFirstOut(firstOutFragTests, lastInFragTests, op.fragment);
                    break;
                case OP_ATTACH:
                    setLastIn(firstOutFragTests, lastInFragTests, op.fragment);
                    break;
            }

            op = op.next;
        }
    }

    /**
     * Finds the first removed fragment and last added fragments when popping the back stack.
     * If none of the fragments have transitions, then both lists will be empty.
     *
     * @param firstOutFragTests The list of first fragments to be removed, keyed on the
     *                          container ID. This list will be modified by the method.
     * @param lastInFragTests The list of last fragments to be added, keyed on the
     *                        container ID. This list will be modified by the method.
     */
    public void calculateBackFragTests(SparseArray<FragTest> firstOutFragTests,
                                       SparseArray<FragTest> lastInFragTests) {
        if (!mManager.mContainer.onHasView()) {
            return; // nothing to see, so no transitions
        }
        BackStackRecord.Op op = mTail;
        while (op != null) {
            switch (op.cmd) {
                case OP_ADD:
                    setFirstOut(firstOutFragTests, lastInFragTests, op.fragment);
                    break;
                case OP_REPLACE:
                    if (op.removed != null) {
                        for (int i = op.removed.size() - 1; i >= 0; i--) {
                            setLastIn(firstOutFragTests, lastInFragTests, op.removed.get(i));
                        }
                    }
                    setFirstOut(firstOutFragTests, lastInFragTests, op.fragment);
                    break;
                case OP_REMOVE:
                    setLastIn(firstOutFragTests, lastInFragTests, op.fragment);
                    break;
                case OP_HIDE:
                    setLastIn(firstOutFragTests, lastInFragTests, op.fragment);
                    break;
                case OP_SHOW:
                    setFirstOut(firstOutFragTests, lastInFragTests, op.fragment);
                    break;
                case OP_DETACH:
                    setLastIn(firstOutFragTests, lastInFragTests, op.fragment);
                    break;
                case OP_ATTACH:
                    setFirstOut(firstOutFragTests, lastInFragTests, op.fragment);
                    break;
            }

            op = op.prev;
        }
    }

    public BackStackRecord.TransitionState popFromBackStack(boolean doStateMove, BackStackRecord.TransitionState state,
                                                                                   SparseArray<FragTest> firstOutFragTests, SparseArray<FragTest> lastInFragTests) {
        if (FragTestManagerImpl.DEBUG) {
            Log.v(TAG, "popFromBackStack: " + this);
            LogWriter logw = new LogWriter(TAG);
            PrintWriter pw = new PrintWriter(logw);
            dump("  ", null, pw, null);
        }

        if (SUPPORTS_TRANSITIONS && mManager.mCurState >= FragTest.CREATED) {
            if (state == null) {
                if (firstOutFragTests.size() != 0 || lastInFragTests.size() != 0) {
                    state = beginTransition(firstOutFragTests, lastInFragTests, true);
                }
            } else if (!doStateMove) {
                setNameOverrides(state, mSharedElementTargetNames, mSharedElementSourceNames);
            }
        }

        bumpBackStackNesting(-1);

        int transitionStyle = state != null ? 0 : mTransitionStyle;
        int transition = state != null ? 0 : mTransition;
        BackStackRecord.Op op = mTail;
        while (op != null) {
            int popEnterAnim = state != null ? 0 : op.popEnterAnim;
            int popExitAnim= state != null ? 0 : op.popExitAnim;
            switch (op.cmd) {
                case OP_ADD: {
                    FragTest f = op.fragment;
                    f.mNextAnim = popExitAnim;
                    mManager.removeFragTest(f,
                            FragTestManagerImpl.reverseTransit(transition), transitionStyle);
                } break;
                case OP_REPLACE: {
                    FragTest f = op.fragment;
                    if (f != null) {
                        f.mNextAnim = popExitAnim;
                        mManager.removeFragTest(f,
                                FragTestManagerImpl.reverseTransit(transition), transitionStyle);
                    }
                    if (op.removed != null) {
                        for (int i=0; i<op.removed.size(); i++) {
                            FragTest old = op.removed.get(i);
                            old.mNextAnim = popEnterAnim;
                            mManager.addFragTest(old, false);
                        }
                    }
                } break;
                case OP_REMOVE: {
                    FragTest f = op.fragment;
                    f.mNextAnim = popEnterAnim;
                    mManager.addFragTest(f, false);
                } break;
                case OP_HIDE: {
                    FragTest f = op.fragment;
                    f.mNextAnim = popEnterAnim;
                    mManager.showFragTest(f,
                            FragTestManagerImpl.reverseTransit(transition), transitionStyle);
                } break;
                case OP_SHOW: {
                    FragTest f = op.fragment;
                    f.mNextAnim = popExitAnim;
                    mManager.hideFragTest(f,
                            FragTestManagerImpl.reverseTransit(transition), transitionStyle);
                } break;
                case OP_DETACH: {
                    FragTest f = op.fragment;
                    f.mNextAnim = popEnterAnim;
                    mManager.attachFragTest(f,
                            FragTestManagerImpl.reverseTransit(transition), transitionStyle);
                } break;
                case OP_ATTACH: {
                    FragTest f = op.fragment;
                    f.mNextAnim = popEnterAnim;
                    mManager.detachFragTest(f,
                            FragTestManagerImpl.reverseTransit(transition), transitionStyle);
                } break;
                default: {
                    throw new IllegalArgumentException("Unknown cmd: " + op.cmd);
                }
            }

            op = op.prev;
        }

        if (doStateMove) {
            mManager.moveToState(mManager.mCurState,
                    FragTestManagerImpl.reverseTransit(transition), transitionStyle, true);
            state = null;
        }

        if (mIndex >= 0) {
            mManager.freeBackStackIndex(mIndex);
            mIndex = -1;
        }
        return state;
    }

    public String getName() {
        return mName;
    }

    public int getTransition() {
        return mTransition;
    }

    public int getTransitionStyle() {
        return mTransitionStyle;
    }

    public boolean isEmpty() {
        return mNumOp == 0;
    }

    /**
     * When custom fragment transitions are used, this sets up the state for each transition
     * and begins the transition. A different transition is started for each fragment container
     * and consists of up to 3 different transitions: the exit transition, a shared element
     * transition and an enter transition.
     *
     * <p>The exit transition operates against the leaf nodes of the first fragment
     * with a view that was removed. If no such fragment was removed, then no exit
     * transition is executed. The exit transition comes from the outgoing fragment.</p>
     *
     * <p>The enter transition operates against the last fragment that was added. If
     * that fragment does not have a view or no fragment was added, then no enter
     * transition is executed. The enter transition comes from the incoming fragment.</p>
     *
     * <p>The shared element transition operates against all views and comes either
     * from the outgoing fragment or the incoming fragment, depending on whether this
     * is going forward or popping the back stack. When going forward, the incoming
     * fragment's enter shared element transition is used, but when going back, the
     * outgoing fragment's return shared element transition is used. Shared element
     * transitions only operate if there is both an incoming and outgoing fragment.</p>
     *
     * @param firstOutFragTests The list of first fragments to be removed, keyed on the
     *                          container ID.
     * @param lastInFragTests The list of last fragments to be added, keyed on the
     *                        container ID.
     * @param isBack true if this is popping the back stack or false if this is a
     *               forward operation.
     * @return The TransitionState used to complete the operation of the transition
     * in {@link #setNameOverrides(BackStackRecord.TransitionState, ArrayList,
     * ArrayList)}.
     */
    private BackStackRecord.TransitionState beginTransition(SparseArray<FragTest> firstOutFragTests,
                                                                                   SparseArray<FragTest> lastInFragTests, boolean isBack) {
        BackStackRecord.TransitionState state = new BackStackRecord.TransitionState();

        // Adding a non-existent target view makes sure that the transitions don't target
        // any views by default. They'll only target the views we tell add. If we don't
        // add any, then no views will be targeted.
        state.nonExistentView = new View(mManager.mHost.getContext());

        boolean anyTransitionStarted = false;
        // Go over all leaving fragments.
        for (int i = 0; i < firstOutFragTests.size(); i++) {
            int containerId = firstOutFragTests.keyAt(i);
            if (configureTransitions(containerId, state, isBack, firstOutFragTests,
                    lastInFragTests)) {
                anyTransitionStarted = true;
            }
        }

        // Now go over all entering fragments that didn't have a leaving fragment.
        for (int i = 0; i < lastInFragTests.size(); i++) {
            int containerId = lastInFragTests.keyAt(i);
            if (firstOutFragTests.get(containerId) == null &&
                    configureTransitions(containerId, state, isBack, firstOutFragTests,
                            lastInFragTests)) {
                anyTransitionStarted = true;
            }
        }

        if (!anyTransitionStarted) {
            state = null;
        }

        return state;
    }

    private static Object getEnterTransition(FragTest inFragTest, boolean isBack) {
        if (inFragTest == null) {
            return null;
        }
        return FragTestTransitionCompat21.cloneTransition(isBack ?
                inFragTest.getReenterTransition() : inFragTest.getEnterTransition());
    }

    private static Object getExitTransition(FragTest outFragTest, boolean isBack) {
        if (outFragTest == null) {
            return null;
        }
        return FragTestTransitionCompat21.cloneTransition(isBack ?
                outFragTest.getReturnTransition() : outFragTest.getExitTransition());
    }

    private static Object getSharedElementTransition(FragTest inFragTest, FragTest outFragTest,
                                                     boolean isBack) {
        if (inFragTest == null || outFragTest == null) {
            return null;
        }
        return FragTestTransitionCompat21.wrapSharedElementTransition(isBack ?
                outFragTest.getSharedElementReturnTransition() :
                inFragTest.getSharedElementEnterTransition());
    }

    private static Object captureExitingViews(Object exitTransition, FragTest outFragTest,
                                              ArrayList<View> exitingViews, ArrayMap<String, View> namedViews, View nonExistentView) {
        if (exitTransition != null) {
            exitTransition = FragTestTransitionCompat21.captureExitingViews(exitTransition,
                    outFragTest.getView(), exitingViews, namedViews, nonExistentView);
        }
        return exitTransition;
    }

    private ArrayMap<String, View> remapSharedElements(BackStackRecord.TransitionState state, FragTest outFragTest,
                                                       boolean isBack) {
        ArrayMap<String, View> namedViews = new ArrayMap<String, View>();
        if (mSharedElementSourceNames != null) {
            FragTestTransitionCompat21.findNamedViews(namedViews, outFragTest.getView());
            if (isBack) {
                namedViews.retainAll(mSharedElementTargetNames);
            } else {
                namedViews = remapNames(mSharedElementSourceNames, mSharedElementTargetNames,
                        namedViews);
            }
        }

        if (isBack) {
            if (outFragTest.mEnterTransitionCallback != null) {
                outFragTest.mEnterTransitionCallback.onMapSharedElements(
                        mSharedElementTargetNames, namedViews);
            }
            setBackNameOverrides(state, namedViews, false);
        } else {
            if (outFragTest.mExitTransitionCallback != null) {
                outFragTest.mExitTransitionCallback.onMapSharedElements(
                        mSharedElementTargetNames, namedViews);
            }
            setNameOverrides(state, namedViews, false);
        }

        return namedViews;
    }

    /**
     * Configures custom transitions for a specific fragment container.
     *
     * @param containerId The container ID of the fragments to configure the transition for.
     * @param state The Transition State keeping track of the executing transitions.
     * @param firstOutFragTests The list of first fragments to be removed, keyed on the
     *                          container ID.
     * @param lastInFragTests The list of last fragments to be added, keyed on the
     *                        container ID.
     * @param isBack true if this is popping the back stack or false if this is a
     *               forward operation.
     */
    private boolean configureTransitions(int containerId, BackStackRecord.TransitionState state, boolean isBack,
                                         SparseArray<FragTest> firstOutFragTests, SparseArray<FragTest> lastInFragTests) {
        ViewGroup sceneRoot = (ViewGroup) mManager.mContainer.onFindViewById(containerId);
        if (sceneRoot == null) {
            return false;
        }
        final FragTest inFragTest = lastInFragTests.get(containerId);
        FragTest outFragTest = firstOutFragTests.get(containerId);

        Object enterTransition = getEnterTransition(inFragTest, isBack);
        Object sharedElementTransition = getSharedElementTransition(inFragTest, outFragTest,
                isBack);
        Object exitTransition = getExitTransition(outFragTest, isBack);
        ArrayMap<String, View> namedViews = null;
        ArrayList<View> sharedElementTargets = new ArrayList<View>();
        if (sharedElementTransition != null) {
            namedViews = remapSharedElements(state, outFragTest, isBack);
            if (namedViews.isEmpty()) {
                sharedElementTransition = null;
                namedViews = null;
            } else {
                // Notify the start of the transition.
                SharedElementCallback callback = isBack ?
                        outFragTest.mEnterTransitionCallback :
                        inFragTest.mEnterTransitionCallback;
                if (callback != null) {
                    ArrayList<String> names = new ArrayList<String>(namedViews.keySet());
                    ArrayList<View> views = new ArrayList<View>(namedViews.values());
                    callback.onSharedElementStart(names, views, null);
                }
                prepareSharedElementTransition(state, sceneRoot, sharedElementTransition,
                        inFragTest, outFragTest, isBack, sharedElementTargets);
            }
        }
        if (enterTransition == null && sharedElementTransition == null &&
                exitTransition == null) {
            return false; // no transitions!
        }

        ArrayList<View> exitingViews = new ArrayList<View>();
        exitTransition = captureExitingViews(exitTransition, outFragTest, exitingViews,
                namedViews, state.nonExistentView);

        // Set the epicenter of the exit transition
        if (mSharedElementTargetNames != null && namedViews != null) {
            View epicenterView = namedViews.get(mSharedElementTargetNames.get(0));
            if (epicenterView != null) {
                if (exitTransition != null) {
                    FragTestTransitionCompat21.setEpicenter(exitTransition, epicenterView);
                }
                if (sharedElementTransition != null) {
                    FragTestTransitionCompat21.setEpicenter(sharedElementTransition,
                            epicenterView);
                }
            }
        }

        FragTestTransitionCompat21.ViewRetriever viewRetriever =
                new FragTestTransitionCompat21.ViewRetriever() {
                    @Override
                    public View getView() {
                        return inFragTest.getView();
                    }
                };

        ArrayList<View> enteringViews = new ArrayList<View>();
        ArrayMap<String, View> renamedViews = new ArrayMap<String, View>();

        boolean allowOverlap = true;
        if (inFragTest != null) {
            allowOverlap = isBack ? inFragTest.getAllowReturnTransitionOverlap() :
                    inFragTest.getAllowEnterTransitionOverlap();
        }
        Object transition = FragTestTransitionCompat21.mergeTransitions(enterTransition,
                exitTransition, sharedElementTransition, allowOverlap);

        if (transition != null) {
            FragTestTransitionCompat21.addTransitionTargets(enterTransition,
                    sharedElementTransition, sceneRoot, viewRetriever, state.nonExistentView,
                    state.enteringEpicenterView, state.nameOverrides, enteringViews,
                    namedViews, renamedViews, sharedElementTargets);
            excludeHiddenFragTestsAfterEnter(sceneRoot, state, containerId, transition);

            // We want to exclude hidden views later, so we need a non-null list in the
            // transition now.
            FragTestTransitionCompat21.excludeTarget(transition, state.nonExistentView, true);
            // Now exclude all currently hidden fragments.
            excludeHiddenFragTests(state, containerId, transition);

            FragTestTransitionCompat21.beginDelayedTransition(sceneRoot, transition);

            FragTestTransitionCompat21.cleanupTransitions(sceneRoot, state.nonExistentView,
                    enterTransition, enteringViews, exitTransition, exitingViews,
                    sharedElementTransition, sharedElementTargets,
                    transition, state.hiddenFragTestViews, renamedViews);
        }
        return transition != null;
    }

    private void prepareSharedElementTransition(final BackStackRecord.TransitionState state, final View sceneRoot,
                                                final Object sharedElementTransition, final FragTest inFragTest,
                                                final FragTest outFragTest, final boolean isBack,
                                                final ArrayList<View> sharedElementTargets) {
        sceneRoot.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        sceneRoot.getViewTreeObserver().removeOnPreDrawListener(this);

                        if (sharedElementTransition != null) {
                            FragTestTransitionCompat21.removeTargets(sharedElementTransition,
                                    sharedElementTargets);
                            sharedElementTargets.clear();

                            ArrayMap<String, View> namedViews = mapSharedElementsIn(
                                    state, isBack, inFragTest);
                            FragTestTransitionCompat21.setSharedElementTargets(sharedElementTransition,
                                    state.nonExistentView, namedViews, sharedElementTargets);

                            setEpicenterIn(namedViews, state);

                            callSharedElementEnd(state, inFragTest, outFragTest, isBack,
                                    namedViews);
                        }

                        return true;
                    }
                });
    }

    private void callSharedElementEnd(BackStackRecord.TransitionState state, FragTest inFragTest,
                                      FragTest outFragTest, boolean isBack, ArrayMap<String, View> namedViews) {
        SharedElementCallback sharedElementCallback = isBack ?
                outFragTest.mEnterTransitionCallback :
                inFragTest.mEnterTransitionCallback;
        if (sharedElementCallback != null) {
            ArrayList<String> names = new ArrayList<String>(namedViews.keySet());
            ArrayList<View> views = new ArrayList<View>(namedViews.values());
            sharedElementCallback.onSharedElementEnd(names, views, null);
        }
    }

    private void setEpicenterIn(ArrayMap<String, View> namedViews, BackStackRecord.TransitionState state) {
        if (mSharedElementTargetNames != null && !namedViews.isEmpty()) {
            // now we know the epicenter of the entering transition.
            View epicenter = namedViews
                    .get(mSharedElementTargetNames.get(0));
            if (epicenter != null) {
                state.enteringEpicenterView.epicenter = epicenter;
            }
        }
    }

    private ArrayMap<String, View> mapSharedElementsIn(BackStackRecord.TransitionState state,
                                                       boolean isBack, FragTest inFragTest) {
        // Now map the shared elements in the incoming fragment
        ArrayMap<String, View> namedViews = mapEnteringSharedElements(state, inFragTest, isBack);

        // remap shared elements and set the name mapping used
        // in the shared element transition.
        if (isBack) {
            if (inFragTest.mExitTransitionCallback != null) {
                inFragTest.mExitTransitionCallback.onMapSharedElements(
                        mSharedElementTargetNames, namedViews);
            }
            setBackNameOverrides(state, namedViews, true);
        } else {
            if (inFragTest.mEnterTransitionCallback != null) {
                inFragTest.mEnterTransitionCallback.onMapSharedElements(
                        mSharedElementTargetNames, namedViews);
            }
            setNameOverrides(state, namedViews, true);
        }
        return namedViews;
    }

    /**
     * Remaps a name-to-View map, substituting different names for keys.
     *
     * @param inMap A list of keys found in the map, in the order in toGoInMap
     * @param toGoInMap A list of keys to use for the new map, in the order of inMap
     * @param namedViews The current mapping
     * @return A copy of namedViews with the keys coming from toGoInMap.
     */
    private static ArrayMap<String, View> remapNames(ArrayList<String> inMap,
                                                     ArrayList<String> toGoInMap, ArrayMap<String, View> namedViews) {
        if (namedViews.isEmpty()) {
            return namedViews;
        }
        ArrayMap<String, View> remappedViews = new ArrayMap<String, View>();
        int numKeys = inMap.size();
        for (int i = 0; i < numKeys; i++) {
            View view = namedViews.get(inMap.get(i));
            if (view != null) {
                remappedViews.put(toGoInMap.get(i), view);
            }
        }
        return remappedViews;
    }

    /**
     * Maps shared elements to views in the entering fragment.
     *
     * @param state The transition State as returned from {@link #beginTransition(
     * SparseArray, SparseArray, boolean)}.
     * @param inFragTest The last fragment to be added.
     * @param isBack true if this is popping the back stack or false if this is a
     *               forward operation.
     */
    private ArrayMap<String, View> mapEnteringSharedElements(BackStackRecord.TransitionState state,
                                                             FragTest inFragTest, boolean isBack) {
        ArrayMap<String, View> namedViews = new ArrayMap<String, View>();
        View root = inFragTest.getView();
        if (root != null) {
            if (mSharedElementSourceNames != null) {
                FragTestTransitionCompat21.findNamedViews(namedViews, root);
                if (isBack) {
                    namedViews = remapNames(mSharedElementSourceNames,
                            mSharedElementTargetNames, namedViews);
                } else {
                    namedViews.retainAll(mSharedElementTargetNames);
                }
            }
        }
        return namedViews;
    }

    private void excludeHiddenFragTestsAfterEnter(final View sceneRoot, final BackStackRecord.TransitionState state,
                                                  final int containerId, final Object transition) {
        sceneRoot.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    public boolean onPreDraw() {
                        sceneRoot.getViewTreeObserver().removeOnPreDrawListener(this);
                        excludeHiddenFragTests(state, containerId, transition);
                        return true;
                    }
                });
    }

    private void excludeHiddenFragTests(BackStackRecord.TransitionState state, int containerId, Object transition) {
        if (mManager.mAdded != null) {
            for (int i = 0; i < mManager.mAdded.size(); i++) {
                FragTest fragment = mManager.mAdded.get(i);
                if (fragment.mView != null && fragment.mContainer != null &&
                        fragment.mContainerId == containerId) {
                    if (fragment.mHidden) {
                        if (!state.hiddenFragTestViews.contains(fragment.mView)) {
                            FragTestTransitionCompat21.excludeTarget(transition, fragment.mView,
                                    true);
                            state.hiddenFragTestViews.add(fragment.mView);
                        }
                    } else {
                        FragTestTransitionCompat21.excludeTarget(transition, fragment.mView,
                                false);
                        state.hiddenFragTestViews.remove(fragment.mView);
                    }
                }
            }
        }
    }

    private static void setNameOverride(ArrayMap<String, String> overrides,
                                        String source, String target) {
        if (source != null && target != null) {
            for (int index = 0; index < overrides.size(); index++) {
                if (source.equals(overrides.valueAt(index))) {
                    overrides.setValueAt(index, target);
                    return;
                }
            }
            overrides.put(source, target);
        }
    }

    private static void setNameOverrides(BackStackRecord.TransitionState state, ArrayList<String> sourceNames,
                                         ArrayList<String> targetNames) {
        if (sourceNames != null) {
            for (int i = 0; i < sourceNames.size(); i++) {
                String source = sourceNames.get(i);
                String target = targetNames.get(i);
                setNameOverride(state.nameOverrides, source, target);
            }
        }
    }

    private void setBackNameOverrides(BackStackRecord.TransitionState state, ArrayMap<String, View> namedViews,
                                      boolean isEnd) {
        int count = mSharedElementTargetNames == null ? 0 : mSharedElementTargetNames.size();
        for (int i = 0; i < count; i++) {
            String source = mSharedElementSourceNames.get(i);
            String originalTarget = mSharedElementTargetNames.get(i);
            View view = namedViews.get(originalTarget);
            if (view != null) {
                String target = FragTestTransitionCompat21.getTransitionName(view);
                if (isEnd) {
                    setNameOverride(state.nameOverrides, source, target);
                } else {
                    setNameOverride(state.nameOverrides, target, source);
                }
            }
        }
    }

    private void setNameOverrides(BackStackRecord.TransitionState state, ArrayMap<String, View> namedViews,
                                  boolean isEnd) {
        int count = namedViews.size();
        for (int i = 0; i < count; i++) {
            String source = namedViews.keyAt(i);
            String target = FragTestTransitionCompat21.getTransitionName(namedViews.valueAt(i));
            if (isEnd) {
                setNameOverride(state.nameOverrides, source, target);
            } else {
                setNameOverride(state.nameOverrides, target, source);
            }
        }
    }

    public class TransitionState {
        public ArrayMap<String, String> nameOverrides = new ArrayMap<String, String>();
        public ArrayList<View> hiddenFragTestViews = new ArrayList<View>();

        public FragTestTransitionCompat21.EpicenterView enteringEpicenterView =
                new FragTestTransitionCompat21.EpicenterView();
        public View nonExistentView;
    }
}

