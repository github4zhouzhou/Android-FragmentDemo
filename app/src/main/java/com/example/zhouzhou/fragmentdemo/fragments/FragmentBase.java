package com.example.zhouzhou.fragmentdemo.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.zhouzhou.fragmentdemo.R;

/**
 * Created by zhouzhou on 2015/4/5.
 */
public class FragmentBase extends Fragment {
    public String TAG = getClass().getSimpleName();

    // 用于 fragment 之间参数传递的接口
    public interface FragmentCallback {
        public void dealWithString(String str);
    }

    @Override
    public void onAttach(Context context) {
        Log.v(TAG, "****** " +  TAG + " ****** " + "onAttach");
        super.onAttach(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.v(TAG, "****** " +  TAG + " ****** " + "onCreate");
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.v(TAG, "****** " +  TAG + " ****** " + "onCreateView");
        return inflater.inflate(R.layout.fragment1, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        Log.v(TAG, "****** " +  TAG + " ****** " + "onActivityCreated");
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onStart() {
        Log.v(TAG, "****** " +  TAG + " ****** " + "onStart");
        super.onStart();
    }

    @Override
    public void onResume() {
        Log.v(TAG, "****** " +  TAG + " ****** " + "onResume");
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.v(TAG, "****** " +  TAG + " ****** " + "onPause");
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.v(TAG, "****** " +  TAG + " ****** " + "onStop");
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        Log.v(TAG, "****** " +  TAG + " ****** " + "onDestroyView");
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "****** " +  TAG + " ****** " + "onDestroy");
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        Log.v(TAG, "****** " +  TAG + " ****** " + "onDetach");
        super.onDetach();
    }
}
