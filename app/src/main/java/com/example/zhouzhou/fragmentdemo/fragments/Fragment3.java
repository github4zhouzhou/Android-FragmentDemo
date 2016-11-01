package com.example.zhouzhou.fragmentdemo.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.zhouzhou.fragmentdemo.R;

/**
 * Created by zhouzhou on 2015/4/5.
 */
public class Fragment3 extends FragmentBase {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment3, container, false);
    }
}
