package com.example.kimsaekwang.myapplication;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import static com.example.kimsaekwang.myapplication.R.id.cds;
import static com.example.kimsaekwang.myapplication.R.id.temp;
import static com.example.kimsaekwang.myapplication.R.id.waterLevel;

public class MainActivity extends AppCompatActivity {


    private TextView tempTxt;
    private TextView soilhumiTxt;
    private TextView cdsTxt;
    private TextView waterLevelTxt;
    private TextView statTxt;

    private MqttService mqttService; // 연결 타입 서비스
    private boolean mBound = false;    // 서비스 연결 여부

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent = new Intent(this, MqttService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        init();

    }

    public void init() {

        tempTxt = (TextView) findViewById(temp);
        soilhumiTxt = (TextView) findViewById(R.id.soilhumi);
        cdsTxt = (TextView) findViewById(cds);
        waterLevelTxt = (TextView) findViewById(waterLevel);
        statTxt = (TextView) findViewById(R.id.status);

        Button waterBtn = (Button) findViewById(R.id.waterBtn);
        ImageButton synBtn = (ImageButton) findViewById(R.id.synchronizedBtn);

        //Enroll Btn Event
        waterBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                mqttService.waterSupply();
            }
        });
        synBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tempTxt.setText(mqttService.getTemp());
                soilhumiTxt.setText(mqttService.getSoilHumi());
                cdsTxt.setText(mqttService.getCds());
                waterLevelTxt.setText(mqttService.getWaterLevel());
            }
        });

    }

    // ServiceConnection 인터페이스를 구현하는 객체를 생성한다.
    private ServiceConnection mConnection = new ServiceConnection(){

        @Override
        public void onServiceDisconnected(ComponentName arg0){
            mBound = false;

        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MqttService.LocalBinder binder = (MqttService.LocalBinder) service;
            mqttService = binder.getService();
            mBound = true;
        }
    };
}