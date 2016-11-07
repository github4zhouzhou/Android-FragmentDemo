package com.example.fragsrcsimplity2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.fragsrcsimplity2.fragtestsupport.FragTest;

public class FragTest1 extends FragTest {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_test1, container, false);
    }
}