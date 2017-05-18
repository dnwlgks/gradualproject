package com.example.kimsaekwang.myapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import static com.example.kimsaekwang.myapplication.R.id.pager;

public class MainActivity extends AppCompatActivity {

    private static final String MAINACTIVITY_TAG = "MAINACTIVITY";

    private static final String WATERPUMP_TOPIC = "Test/WaterPump";// 물공급 Topic
    private static final String WATER_SUPPLY_MSG = "1";
    private static final String SYNCHRONIZE_TOPIC = "Test/Synchronize";//동기화 Topic
    private static final String SYNCHRONIZE_MSG = "2";


    private TextView tempTxt;
    private TextView soilhumiTxt;
    private TextView cdsTxt;
    private TextView waterLevelTxt;

    private ViewPager mViewPager;
    private ImageView pagerNum;

    private MqttService mqttService; // 연결 타입 서비스
    private boolean mBound = false;    // 서비스 연결 여부

    private View header, header2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startActivity(new Intent(this, splashActivity.class));

        init();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_sample, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch(id) {
            case R.id.action_list :
                Toast.makeText(this, "action_list",Toast.LENGTH_LONG).show();
                break;
            case R.id.action_settings :
                Intent intent = new Intent(this, PreSettingsActivity.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        stat();
    }

    public void stat() {
        SharedPreferences pref = getSharedPreferences("Stat", Activity.MODE_PRIVATE);
        tempTxt.setText(pref.getString("colortemp", "initData"));
        soilhumiTxt.setText(pref.getString("suilhumi", "initData"));
        cdsTxt.setText(pref.getString("cds", "initData"));
        waterLevelTxt.setText(pref.getString("waterLevel", "initData"));

        //동기화된 시간도 알아볼수 있는 곳이 있었으면 좋겠다.
    }

    public void init() {

        header = getLayoutInflater().inflate(R.layout.activity_main_stat,null,false);
        header2 = getLayoutInflater().inflate(R.layout.activity_info,null,false);

        // ViewPager 생성 및 적용,이벤트
        mViewPager = (ViewPager) findViewById(pager);
        pagerNum = (ImageView) findViewById(R.id.pagerNum);
        ViewPagerAdapter adapter = new ViewPagerAdapter(getLayoutInflater(), pagerNum);
        adapter.setView(header, 0);
        adapter.setView(header2, 1);

        mViewPager.setAdapter(adapter);

        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position == 0)
                    pagerNum.setImageResource(R.drawable.circle);
                else if (position == 1)
                pagerNum.setImageResource(R.drawable.circle2);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        //end..

        mqttServiceStart();

        tempTxt = (TextView) header2.findViewById(R.id.temp);
        soilhumiTxt = (TextView) header2.findViewById(R.id.soilhumi);
        cdsTxt = (TextView) header2.findViewById(R.id.cds);
        waterLevelTxt = (TextView) header.findViewById(R.id.waterLevel);



        ImageView synBtn = (ImageView) header.findViewById(R.id.synchronizedBtn);

        Button waterBtn = (Button) header.findViewById(R.id.waterBtn);


        //Enroll Btn Event
        waterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mqttService.pubMessage(WATERPUMP_TOPIC, WATER_SUPPLY_MSG);
            }
        });

        synBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mqttService.pubMessage(SYNCHRONIZE_TOPIC, SYNCHRONIZE_MSG);

                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(4000);
                            stat();
                        } catch (InterruptedException e) {
                            Log.d(MAINACTIVITY_TAG, "Error : Synchronize");
                        }
                    }
                });
            }
        });
    }

    private void mqttServiceStart() {

        //Create RestartService
        RestartService restartService = new RestartService();
        Intent intent = new Intent(this, MqttService.class);

        IntentFilter intentFilter = new IntentFilter("com.example.kimsaekwang.myapplication.MqttService");

        //Enroll Broadcast
        registerReceiver(restartService, intentFilter);

        //Start Service
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        //startService(intent);
    }

    // ServiceConnection 인터페이스를 구현하는 객체를 생성한다.
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;

        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MqttService.LocalBinder binder = (MqttService.LocalBinder) service;
            mqttService = binder.getService();
            mBound = true;
        }
    };

    //Back Key를 눌렀을때
    @Override
    public void onBackPressed() {
        //super.onBackPressed();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("정말 종료하시겠습니까?");
        builder.setPositiveButton("Yes",new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //process전체 종료
                finish();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }
}