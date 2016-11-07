package com.example.zhouzhou.fragmentdemo.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import java.lang.ref.WeakReference;


abstract public class ActivityBase extends FragmentActivity {
    public String TAG = getClass().getSimpleName();

    static class MyHandler extends Handler {
        WeakReference<ActivityBase> baseWeakReference;

        public MyHandler(ActivityBase activityBase) {
            super(Looper.getMainLooper());
            baseWeakReference = new WeakReference<>(activityBase);
        }

        public void handleMessage(Message msg) {
            ActivityBase activityBase = baseWeakReference.get();
            if (activityBase != null) {
                activityBase.handleMessage(msg);
            }
        }
    }
    private Handler mHandler;

    public synchronized Handler getActivityHandler() {
        if (mHandler == null) {
            mHandler = new MyHandler(this);
        }
        return mHandler;
    }

    public void handleMessage(Message msg) {

    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        Log.v(TAG, "------ " +  TAG + " ------ " + "onAttachFragment");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "------ " +  TAG + " ------ " + "onCreate");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.v(TAG, "------ " +  TAG + " ------ " + "onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "------ " +  TAG + " ------ " + "onResume");
    }

    @Override
    protected void onPause() {
        Log.v(TAG, "------ " +  TAG + " ------ " + "onPause");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.v(TAG, "------ " +  TAG + " ------ " + "onStop");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, "------ " +  TAG + " ------ " + "onDestroy");
        super.onDestroy();
    }
}
