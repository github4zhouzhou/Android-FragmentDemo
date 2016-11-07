package com.example.fragsrcsimplity2;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.example.fragsrcsimplity2.fragtestsupport.FragTestActivity;
import com.example.fragsrcsimplity2.fragtestsupport.FragTestManager;
import com.example.fragsrcsimplity2.fragtestsupport.FragTestTransaction;

public class ActivityMain extends FragTestActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnLoadFrag1 = (Button) findViewById(R.id.id_btn_show_fragment1);
        btnLoadFrag1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragTestManager manager = getSupportFragTestManager();
                FragTestTransaction transaction = manager.beginTransaction();
                FragTest1 fragment1 = new FragTest1();
                transaction.add(R.id.id_fragment_container, fragment1);
                transaction.commit();
            }
        });
    }
}
