package com.example.fragsrcsimplity2.fragtestsupport;

/**
 * Created by zhouzhou on 2016/11/6.
 */

import android.content.Context;
import android.content.res.Configuration;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.util.SimpleArrayMap;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;


public class FragTestController {
    private final FragTestHostCallback<?> mHost;

    /**
     * Returns a {@link FragTestController}.
     * @param callbacks
     */
    public static final FragTestController createController(FragTestActivity.HostCallbacks callbacks) {
        return new FragTestController(callbacks);
    }

    private FragTestController(FragTestHostCallback<?> callbacks) {
        mHost = callbacks;
    }

   
    public FragTestManager getSupportFragTestManager() {
        return mHost.getFragTestManagerImpl();
    }

    /**
     * Returns a {@link LoaderManager}.
     */
    public LoaderManager getSupportLoaderManager() {
        return mHost.getLoaderManagerImpl();
    }

    /**
     * Returns a fragment with the given identifier.
     */
    @Nullable
    FragTest findFragTestByWho(String who) {
        return mHost.mFragTestManager.findFragTestByWho(who);
    }

    /**
     * Returns the number of active fragments.
     */
    public int getActiveFragTestsCount() {
        final List<FragTest> actives = mHost.mFragTestManager.mActive;
        return actives == null ? 0 : actives.size();
    }

    /**
     * Returns the list of active fragments.
     */
    public List<FragTest> getActiveFragTests(List<FragTest> actives) {
        if (mHost.mFragTestManager.mActive == null) {
            return null;
        }
        if (actives == null) {
            actives = new ArrayList<FragTest>(getActiveFragTestsCount());
        }
        actives.addAll(mHost.mFragTestManager.mActive);
        return actives;
    }

    /**
     * Attaches the host to the FragTestManager for this controller. The host must be
     * attached before the FragTestManager can be used to manage FragTests.
     */
    public void attachHost(FragTest parent) {
        mHost.mFragTestManager.attachController(
                mHost, mHost /*container*/, parent);
    }

    /**
     * Instantiates a FragTest's view.
     *
     * @param parent The parent that the created view will be placed
     * in; <em>note that this may be null</em>.
     * @param name Tag name to be inflated.
     * @param context The context the view is being created in.
     * @param attrs Inflation attributes as specified in XML file.
     *
     * @return view the newly created view
     */
    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        return mHost.mFragTestManager.onCreateView(parent, name, context, attrs);
    }

    /**
     * Marks the fragment state as unsaved. This allows for "state loss" detection.
     */
    public void noteStateNotSaved() {
        mHost.mFragTestManager.noteStateNotSaved();
    }

    /**
     * Saves the state for all FragTests.
     */
    public Parcelable saveAllState() {
        return mHost.mFragTestManager.saveAllState();
    }

    /**
     * Restores the saved state for all FragTests. The given FragTest list are FragTest
     * instances retained across configuration changes.
     *
     * @see #retainNonConfig()
     */
    public void restoreAllState(Parcelable state, List<FragTest> nonConfigList) {
        mHost.mFragTestManager.restoreAllState(state, nonConfigList);
    }

    /**
     * Returns a list of FragTests that have opted to retain their instance across
     * configuration changes.
     */
    public List<FragTest> retainNonConfig() {
        return mHost.mFragTestManager.retainNonConfig();
    }


    public void dispatchCreate() {
        mHost.mFragTestManager.dispatchCreate();
    }

    public void dispatchActivityCreated() {
        mHost.mFragTestManager.dispatchActivityCreated();
    }

    /**
     * Moves all FragTests managed by the controller's FragTestManager
     * into the start state.
     * <p>Call when FragTests should be started.
     *
     * @see FragTest#onStart()
     */
    public void dispatchStart() {
        mHost.mFragTestManager.dispatchStart();
    }

    /**
     * Moves all FragTests managed by the controller's FragTestManager
     * into the resume state.
     * <p>Call when FragTests should be resumed.
     *
     * @see FragTest#onResume()
     */
    public void dispatchResume() {
        mHost.mFragTestManager.dispatchResume();
    }

    /**
     * Moves all FragTests managed by the controller's FragTestManager
     * into the pause state.
     * <p>Call when FragTests should be paused.
     *
     * @see FragTest#onPause()
     */
    public void dispatchPause() {
        mHost.mFragTestManager.dispatchPause();
    }

    /**
     * Moves all FragTests managed by the controller's FragTestManager
     * into the stop state.
     * <p>Call when FragTests should be stopped.
     *
     * @see FragTest#onStop()
     */
    public void dispatchStop() {
        mHost.mFragTestManager.dispatchStop();
    }

    public void dispatchReallyStop() {
        mHost.mFragTestManager.dispatchReallyStop();
    }

    /**
     * Moves all FragTests managed by the controller's FragTestManager
     * into the destroy view state.
     * <p>Call when the FragTest's views should be destroyed.
     *
     * @see FragTest#onDestroyView()
     */
    public void dispatchDestroyView() {
        mHost.mFragTestManager.dispatchDestroyView();
    }

    /**
     * Moves all FragTests managed by the controller's FragTestManager
     * into the destroy state.
     * <p>Call when FragTests should be destroyed.
     *
     * @see FragTest#onDestroy()
     */
    public void dispatchDestroy() {
        mHost.mFragTestManager.dispatchDestroy();
    }

    /**
     * Lets all FragTests managed by the controller's FragTestManager
     * know a configuration change occurred.
     * <p>Call when there is a configuration change.
     *
     * @see FragTest#onConfigurationChanged(Configuration)
     */
    public void dispatchConfigurationChanged(Configuration newConfig) {
        mHost.mFragTestManager.dispatchConfigurationChanged(newConfig);
    }

    /**
     * Lets all FragTests managed by the controller's FragTestManager
     * know the device is in a low memory condition.
     * <p>Call when the device is low on memory and FragTest's should trim
     * their memory usage.
     *
     * @see FragTest#onLowMemory()
     */
    public void dispatchLowMemory() {
        mHost.mFragTestManager.dispatchLowMemory();
    }

    /**
     * Lets all FragTests managed by the controller's FragTestManager
     * know they should create an options menu.
     * <p>Call when the FragTest should create an options menu.
     *
     * @return {@code true} if the options menu contains items to display
     * @see FragTest#onCreateOptionsMenu(Menu, MenuInflater)
     */
    public boolean dispatchCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        return mHost.mFragTestManager.dispatchCreateOptionsMenu(menu, inflater);
    }

    /**
     * Lets all FragTests managed by the controller's FragTestManager
     * know they should prepare their options menu for display.
     * <p>Call immediately before displaying the FragTest's options menu.
     *
     * @return {@code true} if the options menu contains items to display
     * @see FragTest#onPrepareOptionsMenu(Menu)
     */
    public boolean dispatchPrepareOptionsMenu(Menu menu) {
        return mHost.mFragTestManager.dispatchPrepareOptionsMenu(menu);
    }

    /**
     * Sends an option item selection event to the FragTests managed by the
     * controller's FragTestManager. Once the event has been consumed,
     * no additional handling will be performed.
     * <p>Call immediately after an options menu item has been selected
     *
     * @return {@code true} if the options menu selection event was consumed
     * @see FragTest#onOptionsItemSelected(MenuItem)
     */
    public boolean dispatchOptionsItemSelected(MenuItem item) {
        return mHost.mFragTestManager.dispatchOptionsItemSelected(item);
    }

    /**
     * Sends a context item selection event to the FragTests managed by the
     * controller's FragTestManager. Once the event has been consumed,
     * no additional handling will be performed.
     * <p>Call immediately after an options menu item has been selected
     *
     * @return {@code true} if the context menu selection event was consumed
     * @see FragTest#onContextItemSelected(MenuItem)
     */
    public boolean dispatchContextItemSelected(MenuItem item) {
        return mHost.mFragTestManager.dispatchContextItemSelected(item);
    }

    /**
     * Lets all FragTests managed by the controller's FragTestManager
     * know their options menu has closed.
     * <p>Call immediately after closing the FragTest's options menu.
     *
     * @see FragTest#onOptionsMenuClosed(Menu)
     */
    public void dispatchOptionsMenuClosed(Menu menu) {
        mHost.mFragTestManager.dispatchOptionsMenuClosed(menu);
    }

    /**
     * Execute any pending actions for the FragTests managed by the
     * controller's FragTestManager.
     * <p>Call when queued actions can be performed [eg when the
     * FragTest moves into a start or resume state].
     * @return {@code true} if queued actions were performed
     */
    public boolean execPendingActions() {
        return mHost.mFragTestManager.execPendingActions();
    }

    /**
     * Starts the loaders.
     */
    public void doLoaderStart() {
        mHost.doLoaderStart();
    }

    /**
     * Stops the loaders, optionally retaining their state. This is useful for keeping the
     * loader state across configuration changes.
     *
     * @param retain When {@code true}, the loaders aren't stopped, but, their instances
     * are retained in a started state
     */
    public void doLoaderStop(boolean retain) {
        mHost.doLoaderStop(retain);
    }

    /**
     * Retains the state of each of the loaders.
     */
    public void doLoaderRetain() {
        mHost.doLoaderRetain();
    }

    /**
     * Destroys the loaders and, if their state is not being retained, removes them.
     */
    public void doLoaderDestroy() {
        mHost.doLoaderDestroy();
    }

    /**
     * Lets the loaders know the host is ready to receive notifications.
     */
    public void reportLoaderStart() {
        mHost.reportLoaderStart();
    }

    /**
     * Returns a list of LoaderManagers that have opted to retain their instance across
     * configuration changes.
     */
    public SimpleArrayMap<String, LoaderManager> retainLoaderNonConfig() {
        return mHost.retainLoaderNonConfig();
    }

    /**
     * Restores the saved state for all LoaderManagers. The given LoaderManager list are
     * LoaderManager instances retained across configuration changes.
     *
     * @see #retainLoaderNonConfig()
     */
    public void restoreLoaderNonConfig(SimpleArrayMap<String, LoaderManager> loaderManagers) {
        mHost.restoreLoaderNonConfig(loaderManagers);
    }

    /**
     * Dumps the current state of the loaders.
     */
    public void dumpLoaders(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        mHost.dumpLoaders(prefix, fd, writer, args);
    }
}
