package com.example.fragsrcsimplity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 * Created by zhouzhou on 2016/11/6.
 */

public class ActivityMain extends FragTestActivity {

    //FragmentActivity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_frag_test);

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
