package com.example.fragsrcsimplity;

/**
 * Created by zhouzhou on 2016/11/6.
 */

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentContainer;
import android.support.v4.util.SimpleArrayMap;
import android.view.LayoutInflater;
import android.view.View;

import java.io.FileDescriptor;
import java.io.PrintWriter;

/**
 * Integration points with the FragTest host.
 * <p>
 * FragTests may be hosted by any object; such as an {@link Activity}. In order to
 * host FragTests, implement {@link android.support.v4.app.FragTestHostCallback}, overriding the methods
 * applicable to the host.
 */
public abstract class FragTestHostCallback<E> extends FragmentContainer {
    private final Activity mActivity;
    final Context mContext;
    private final Handler mHandler;
    final int mWindowAnimations;
    final FragTestManagerImpl mFragTestManager = new FragTestManagerImpl();
    /** The loader managers for individual FragTests [i.e. FragTest#getLoaderManager()] */
    private SimpleArrayMap<String, LoaderManager> mAllLoaderManagers;
    /** Whether or not FragTest loaders should retain their state */
    private boolean mRetainLoaders;
    /** The loader manger for the FragTest host [i.e. Activity#getLoaderManager()] */
    private LoaderManagerImpl mLoaderManager;
    private boolean mCheckedForLoaderManager;
    /** Whether or not the FragTest host loader manager was started */
    private boolean mLoadersStarted;

    public FragTestHostCallback(Context context, Handler handler, int windowAnimations) {
        this(null /*activity*/, context, handler, windowAnimations);
    }

    FragTestHostCallback(FragTestActivity activity) {
        this(activity, activity /*context*/, activity.mMainHandler, 0 /*windowAnimations*/);
    }

    FragTestHostCallback(Activity activity, Context context, Handler handler,
                         int windowAnimations) {
        mActivity = activity;
        mContext = context;
        mHandler = handler;
        mWindowAnimations = windowAnimations;
    }

    /**
     * Print internal state into the given stream.
     *
     * @param prefix Desired prefix to prepend at each line of output.
     * @param fd The raw file descriptor that the dump is being sent to.
     * @param writer The PrintWriter to which you should dump your state. This will be closed
     *                  for you after you return.
     * @param args additional arguments to the dump request.
     */
    public void onDump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
    }

    /**
     * Return {@code true} if the FragTest's state needs to be saved.
     */
    public boolean onShouldSaveFragTestState(FragTest FragTest) {
        return true;
    }

    /**
     * Return a {@link LayoutInflater}.
     * See {@link Activity#getLayoutInflater()}.
     */
    public LayoutInflater onGetLayoutInflater() {
        return (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    /**
     * Return the object that's currently hosting the FragTest. If a {@link FragTest}
     * is hosted by a {@link FragTestActivity}, the object returned here should be
     * the same object returned from {@link FragTest#getActivity()}.
     */
    @Nullable
    public abstract E onGetHost();

    /**
     * Invalidates the activity's options menu.
     * See {@link FragTestActivity#supportInvalidateOptionsMenu()}
     */
    public void onSupportInvalidateOptionsMenu() {
    }

    /**
     * Starts a new {@link Activity} from the given FragTest.
     * See {@link FragTestActivity#startActivityForResult(Intent, int)}.
     */
    public void onStartActivityFromFragTest(FragTest FragTest, Intent intent, int requestCode) {
        onStartActivityFromFragTest(FragTest, intent, requestCode, null);
    }

    /**
     * Starts a new {@link Activity} from the given FragTest.
     * See {@link FragTestActivity#startActivityForResult(Intent, int, Bundle)}.
     */
    public void onStartActivityFromFragTest(
            FragTest FragTest, Intent intent, int requestCode, @Nullable Bundle options) {
        if (requestCode != -1) {
            throw new IllegalStateException(
                    "Starting activity with a requestCode requires a FragTestActivity host");
        }
        mContext.startActivity(intent);
    }

    /**
     * Requests permissions from the given FragTest.
     * See {@link FragTestActivity#requestPermissions(String[], int)}
     */
    public void onRequestPermissionsFromFragTest(@NonNull FragTest FragTest,
                                                 @NonNull String[] permissions, int requestCode) {
    }

    /**
     * Checks wehter to show permission rationale UI from a FragTest.
     * See {@link FragTestActivity#shouldShowRequestPermissionRationale(String)}
     */
    public boolean onShouldShowRequestPermissionRationale(@NonNull String permission) {
        return false;
    }

    /**
     * Return {@code true} if there are window animations.
     */
    public boolean onHasWindowAnimations() {
        return true;
    }

    /**
     * Return the window animations.
     */
    public int onGetWindowAnimations() {
        return mWindowAnimations;
    }

    @Nullable
    @Override
    public View onFindViewById(int id) {
        return null;
    }

    @Override
    public boolean onHasView() {
        return true;
    }

    Activity getActivity() {
        return mActivity;
    }

    Context getContext() {
        return mContext;
    }

    Handler getHandler() {
        return mHandler;
    }

    FragTestManagerImpl getFragTestManagerImpl() {
        return mFragTestManager;
    }

    LoaderManagerImpl getLoaderManagerImpl() {
        if (mLoaderManager != null) {
            return mLoaderManager;
        }
        mCheckedForLoaderManager = true;
        mLoaderManager = getLoaderManager("(root)", mLoadersStarted, true /*create*/);
        return mLoaderManager;
    }

    void inactivateFragTest(String who) {
        //Log.v(TAG, "invalidateSupportFragTest: who=" + who);
        if (mAllLoaderManagers != null) {
            LoaderManagerImpl lm = (LoaderManagerImpl) mAllLoaderManagers.get(who);
            if (lm != null && !lm.mRetaining) {
                lm.doDestroy();
                mAllLoaderManagers.remove(who);
            }
        }
    }

    void onAttachFragTest(FragTest FragTest) {
    }

    boolean getRetainLoaders() {
        return mRetainLoaders;
    }

    void doLoaderStart() {
        if (mLoadersStarted) {
            return;
        }
        mLoadersStarted = true;

        if (mLoaderManager != null) {
            mLoaderManager.doStart();
        } else if (!mCheckedForLoaderManager) {
            mLoaderManager = getLoaderManager("(root)", mLoadersStarted, false);
            // the returned loader manager may be a new one, so we have to start it
            if ((mLoaderManager != null) && (!mLoaderManager.mStarted)) {
                mLoaderManager.doStart();
            }
        }
        mCheckedForLoaderManager = true;
    }

    // retain -- whether to stop the loader or retain it
    void doLoaderStop(boolean retain) {
        mRetainLoaders = retain;

        if (mLoaderManager == null) {
            return;
        }

        if (!mLoadersStarted) {
            return;
        }
        mLoadersStarted = false;

        if (retain) {
            mLoaderManager.doRetain();
        } else {
            mLoaderManager.doStop();
        }
    }

    void doLoaderRetain() {
        if (mLoaderManager == null) {
            return;
        }
        mLoaderManager.doRetain();
    }

    void doLoaderDestroy() {
        if (mLoaderManager == null) {
            return;
        }
        mLoaderManager.doDestroy();
    }

    void reportLoaderStart() {
        if (mAllLoaderManagers != null) {
            final int N = mAllLoaderManagers.size();
            LoaderManagerImpl loaders[] = new LoaderManagerImpl[N];
            for (int i=N-1; i>=0; i--) {
                loaders[i] = (LoaderManagerImpl) mAllLoaderManagers.valueAt(i);
            }
            for (int i=0; i<N; i++) {
                LoaderManagerImpl lm = loaders[i];
                lm.finishRetain();
                lm.doReportStart();
            }
        }
    }

    LoaderManagerImpl getLoaderManager(String who, boolean started, boolean create) {
        if (mAllLoaderManagers == null) {
            mAllLoaderManagers = new SimpleArrayMap<String, LoaderManager>();
        }
        LoaderManagerImpl lm = (LoaderManagerImpl) mAllLoaderManagers.get(who);
        if (lm == null) {
            if (create) {
                lm = new LoaderManagerImpl(who, this, started);
                mAllLoaderManagers.put(who, lm);
            }
        } else {
            lm.updateHostController(this);
        }
        return lm;
    }

    SimpleArrayMap<String, LoaderManager> retainLoaderNonConfig() {
        boolean retainLoaders = false;
        if (mAllLoaderManagers != null) {
            // prune out any loader managers that were already stopped and so
            // have nothing useful to retain.
            final int N = mAllLoaderManagers.size();
            LoaderManagerImpl loaders[] = new LoaderManagerImpl[N];
            for (int i=N-1; i>=0; i--) {
                loaders[i] = (LoaderManagerImpl) mAllLoaderManagers.valueAt(i);
            }
            for (int i=0; i<N; i++) {
                LoaderManagerImpl lm = loaders[i];
                if (lm.mRetaining) {
                    retainLoaders = true;
                } else {
                    lm.doDestroy();
                    mAllLoaderManagers.remove(lm.mWho);
                }
            }
        }

        if (retainLoaders) {
            return mAllLoaderManagers;
        }
        return null;
    }

    void restoreLoaderNonConfig(SimpleArrayMap<String, LoaderManager> loaderManagers) {
        mAllLoaderManagers = loaderManagers;
    }

    void dumpLoaders(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        writer.print(prefix); writer.print("mLoadersStarted=");
        writer.println(mLoadersStarted);
        if (mLoaderManager != null) {
            writer.print(prefix); writer.print("Loader Manager ");
            writer.print(Integer.toHexString(System.identityHashCode(mLoaderManager)));
            writer.println(":");
            mLoaderManager.dump(prefix + "  ", fd, writer, args);
        }
    }
}
