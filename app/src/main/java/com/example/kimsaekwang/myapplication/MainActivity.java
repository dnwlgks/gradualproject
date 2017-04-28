package com.example.kimsaekwang.myapplication;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    TextView temp = (TextView) findViewById(R.id.temp);
    TextView humi = (TextView) findViewById(R.id.humi);
    TextView chor = (TextView) findViewById(R.id.chr);
    TextView stat = (TextView) findViewById(R.id.status);
    ImageView waterbottle=(ImageView)findViewById(R.id.waterbottle);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }
    public void onClickDemandWater(View v){

    }
    public void onClickReset(View v){

        //온도랑 채도랑 습도 초기화 textview.setText()로 초기화 해주기
        //상태 어떠한 상태인지 이미지 바꾸는 걸로 채크
    }
}
