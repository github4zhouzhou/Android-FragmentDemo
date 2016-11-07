package com.example.zhouzhou.fragmentdemo.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.zhouzhou.fragmentdemo.R;

/**
 * Created by zhouzhou on 2015/4/5.
 */
public class Fragment4 extends FragmentBase {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment4, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    // MARK: begin--------------------------------------------------------------------------

    // 提供给外部的函数，用来改变fragment内文字
    public void setText(String str) {
        TextView tv = (TextView) getView().findViewById(R.id.id_tv4);
        tv.setText(str);
    }

    // 通过广播在 fragment 之间传递参数，能够完全解耦，但是开销太大，其他方法实现不了时再用吧
    private void receiveBroadcast() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("action");

        LocalBroadcastManager lbm = LocalBroadcastManager
                .getInstance(getActivity());

        BroadcastReceiver br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String value = intent.getStringExtra("list_item");
                TextView tv = (TextView) getView().findViewById(R.id.id_tv4);
                tv.setText(value);
            }
        };
        lbm.registerReceiver(br, intentFilter);
    }

    // MARK: end--------------------------------------------------------------------------
}
