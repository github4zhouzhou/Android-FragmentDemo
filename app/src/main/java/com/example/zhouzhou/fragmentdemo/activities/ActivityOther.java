package com.example.zhouzhou.fragmentdemo.activities;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;

import com.example.zhouzhou.fragmentdemo.R;
import com.example.zhouzhou.fragmentdemo.fragments.Fragment4;

public class ActivityOther extends ActivityBase {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_other);

        String str = getIntent().getStringExtra("title");
        FragmentManager manager = getSupportFragmentManager();
        Fragment4 fragment4 = (Fragment4)manager.findFragmentById(R.id.id_fragment4);
        fragment4.setText(str);
    }
}
