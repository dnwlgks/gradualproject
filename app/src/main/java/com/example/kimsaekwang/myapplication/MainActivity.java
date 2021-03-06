package com.example.kimsaekwang.myapplication;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import static com.example.kimsaekwang.myapplication.R.id.pager;
import static com.example.kimsaekwang.myapplication.R.id.soilhumi;

public class MainActivity extends AppCompatActivity {

    private static final String MAINACTIVITY_TAG = "MAINACTIVITY";

    private static final int VIEWPAGER_REFRESH_INTERVAL = 1000;// 뷰페이저 새로고침

    private static final String TEMP_TOPIC = "Test/Temp";// 온도 Topic
    private static final String SOILHUMI_TOPIC = "Test/Soilhumi";// 습도 Topic
    private static final String CDS_TOPIC = "Test/Cds";// 조도 Topic
    private static final String WATERLEVEL_TOPIC = "Test/WaterLevel";// 수위 Topic
    private static final String WATERPUMP_TOPIC = "Test/WaterPump";// 물공급 Topic
    private static final String WATER_SUPPLY_MSG = "1";
    private static final String SYNCHRONIZE_TOPIC = "Test/Synchronize";//동기화 Topic
    private static final String SYNCHRONIZE_MSG = "2";


    private TextView tempTxt;
    private TextView soilhumiTxt;
    private TextView cdsTxt;
    private TextView waterLevelTxt;

    private ViewPager mViewPager;
    ViewPagerAdapter viewPagerAdapter;
    private ImageView pagerNum;

    Thread viewPagerRefreshThread;
    Handler viewPagerRefreshHandler = new Handler();



    private MqttService mqttService; // 연결 타입 서비스
    private boolean mBound = false;    // 서비스 연결 여부

    private View header, header2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //startActivity(new Intent(this, splashActivity.class));

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

        switch (id) {
            case R.id.action_list:
                Toast.makeText(this, "action_list", Toast.LENGTH_LONG).show();
                break;
            case R.id.action_settings:
                Intent intent = new Intent(this, PreSettingsActivity.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void init() {

        onCreateViewPager();



        mqttServiceStart();

        tempTxt = (TextView) header2.findViewById(R.id.temp);
        soilhumiTxt = (TextView) header2.findViewById(soilhumi);
        cdsTxt = (TextView) header2.findViewById(R.id.cds);
        waterLevelTxt = (TextView) header.findViewById(R.id.waterLevel);


        ImageView synBtn = (ImageView) header.findViewById(R.id.synchronizedBtn);

        Button waterBtn = (Button) header.findViewById(R.id.waterBtn);


        //Enroll waterBtn Event
        waterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //mqttService.pubMessage(WATERPUMP_TOPIC, WATER_SUPPLY_MSG);
                viewPagerAdapter.notifyDataSetChanged();
            }
        });

    }

    private void onCreateViewPager() {

        //ViewPager에 들어갈 Child xml을 인플레이트
        header = getLayoutInflater().inflate(R.layout.activity_main_stat, null, false);
        header2 = getLayoutInflater().inflate(R.layout.activity_info, null, false);

        // ViewPager 생성 및 적용,이벤트
        mViewPager = (ViewPager) findViewById(pager);
        pagerNum = (ImageView) findViewById(R.id.pagerNum);
        viewPagerAdapter = new ViewPagerAdapter(getLayoutInflater(), pagerNum);
        viewPagerAdapter.setView(header, 0);
        viewPagerAdapter.setView(header2, 1);
        mViewPager.setAdapter(viewPagerAdapter);


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


        //ViewPagerRefresh
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {

                        viewPagerRefreshHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                viewPagerAdapter.notifyDataSetChanged();
                            }
                        });
                        Thread.sleep(VIEWPAGER_REFRESH_INTERVAL);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        thread.start();
    }


    private void mqttServiceStart() {

        Intent intent = new Intent(this, MqttService.class);

        IntentFilter intentFilter = new IntentFilter("com.example.kimsaekwang.myapplication.MqttService");

        //Start BindService
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        //start StartService
        //startService(intent);
    }

    // ServiceConnection 인터페이스를 구현하는 객체를 생성한다.
    private ServiceConnection mConnection = new ServiceConnection() {
        // Called when the connection with the service is established
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MqttService.LocalBinder binder = (MqttService.LocalBinder) service;
            mqttService = binder.getService();
            mqttService.registerCallback(mCallback); //콜백 등록
            mBound = true;
        }

        //서비스에서 아래의 콜백 함수를 호출하며, 콜백 함수에서는 액티비티에서 처리할 내용 입력
        private MqttService.ICallback mCallback = new MqttService.ICallback() {
            public void recvData(String topic, String message) {
                //받아온 데이터를 입력
                if (topic.equals(TEMP_TOPIC)) tempTxt.setText(message);
                if (topic.equals(SOILHUMI_TOPIC)) soilhumiTxt.setText(message);
                if (topic.equals(CDS_TOPIC)) cdsTxt.setText(message);
                if (topic.equals(WATERLEVEL_TOPIC)) waterLevelTxt.setText(message);

            }
        };

        // Called when the connection with the service disconnects unexpectedly
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;

        }
    };

    //Back Key를 눌렀을때
    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("정말 종료하시겠습니까?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //process전체 종료
                finish();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

}