package com.example.zhouzhou.fragmentdemo.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.example.zhouzhou.fragmentdemo.R;
import com.example.zhouzhou.fragmentdemo.activities.ActivityBase;

/**
 * Created by zhouzhou on 2015/4/5.
 */
public class Fragment5 extends FragmentBase {

    private boolean bHandler = false;
    private String[] mStrings = {
            "Abbaye de Belloc", "Abbaye du Mont des Cats",
            "Abertam", "Abondance", "Ackawi",
            "Acorn", "Adelost", "Affidelice au Chablis",
            "Afuega'l Pitu", "Airag", "Airedale", "Aisy Cendre",
            "Allgauer Emmentaler", "Abbaye de Belloc",
            "Abbaye du Mont des Cats", "Abertam",
            "Abondance", "Ackawi", "Acorn", "Adelost",
            "Affidelice au Chablis", "Afuega'l Pitu",
            "Airag", "Airedale", "Aisy Cendre",
            "Allgauer Emmentaler"};

    private Handler mHandler;
    private FragmentCallback mCallback;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment5, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Activity activity = getActivity();
        if (bHandler) {
            // 使用 Handler 类实现 fragment 之间参数传递
            if (activity instanceof ActivityBase) {
                mHandler = ((ActivityBase) activity).getActivityHandler();
            }
        } else {
            // 使用接口回调的方式实现 fragment 之间参数传递
            if (activity instanceof FragmentCallback) {
                mCallback = (FragmentCallback) activity;
            }
        }


        ListView listView = (ListView) getView().findViewById(R.id.id_list);
        ArrayAdapter arrayAdapter = new ArrayAdapter<String>(getActivity()
                , android.R.layout.simple_list_item_1, mStrings);

        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String str = mStrings[position];
                //broadcastString(str);
                if (bHandler) {
                    if (mHandler != null) {
                        Message msg = new Message();
                        msg.what = 1;
                        msg.obj = str;
                        mHandler.sendMessage(msg);
                    }
                } else {
                    mCallback.dealWithString(str);
                }
            }
        });
    }

    // 通过广播在 fragment 之间传递参数，能够完全解耦，但是开销太大，其他方法实现不了时再用吧
    private void broadcastString(String str) {
        Intent intent = new Intent("action");
        intent.putExtra("list_item", str);
        LocalBroadcastManager
                .getInstance(getActivity())
                .sendBroadcast(intent);
    }
}
