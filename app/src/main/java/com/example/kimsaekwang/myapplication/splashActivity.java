package com.example.kimsaekwang.myapplication;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;

/**
 * Created by KimSaeKwang on 2017-05-01.
 */

public class splashActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                finish();
            }
        }, 3000);// 3 초
    }
}


