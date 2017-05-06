package com.example.kimsaekwang.myapplication;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttService extends Service implements MqttCallback, Runnable{

    private static final String MQTT_TAG = "MqttService"; // Debug TAG

    private static final String MQTT_THREAD_NAME = "MqttService[" + MQTT_TAG + "]"; // Handler Thread ID

    private static final int MILLISINFUTURE = 1000*1000;
    private static final int COUNT_DOWN_INTERVAL = 1000;

    private static final String TEMP_TOPIC = "Test/Temp";// 온도 Topic
    private static final String SOILHUMI_TOPIC = "Test/Soilhumi";// 습도 Topic
    private static final String CDS_TOPIC = "Test/Cds";// 조도 Topic
    private static final String WATERLEVEL_TOPIC = "Test/WaterLevel";// 수위 Topic

    private static final String WATER_SUPPLY_MSG = "1";
    private static final String WATERPUMP_TOPIC = "Test/WaterPump";// 물공급 Topic

    private int qos = 0;

    private String broker = "tcp://192.168.0.3:1883";
    private String clientId = "JangGyooSeo";

    private MqttClient client;
    private MemoryPersistence persistence;
    private MqttConnectOptions connOpts;

    //Status Value
    private String temp;
    private String soilHumi;
    private String cds;
    private String waterLevel;


    private final IBinder mBinder = new LocalBinder();    // 컴포넌트에 반환되는 IBinder

    // 컴포넌트에 반환해줄 IBinder를 위한 클래스
    public class LocalBinder extends Binder {
        MqttService getService(){
            return MqttService.this;
        }
    }

    CountDownTimer countDownTimer;

    @Override
    public void onCreate() {
        unregisterRestartAlarm();
        super.onCreate();

        init();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //Tast kill을 통해 서비스가 죽는걸 방지
        startForeground(1, new Notification());

        return super.onStartCommand(intent, flags, startId);
    }

    private void init(){
        connect();

        Thread thread = new Thread(this);
        thread.start();

        countDownTimerSetting();
        countDownTimer.start();
    }

    private void countDownTimerSetting(){
        countDownTimer = new CountDownTimer(MILLISINFUTURE,COUNT_DOWN_INTERVAL){
            @Override
            public void onTick(long millisUntilFinished) {
                Log.i(MQTT_TAG, "onTick");
            }

            @Override
            public void onFinish() {
                Log.i(MQTT_TAG,"onFinish");
            }
        };
    }

    private void connect() {
        try {
            persistence = new MemoryPersistence();
            client = new MqttClient(broker, clientId, persistence);
            connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
        } catch (MqttException e) {
            Log.d(MQTT_TAG, "Error : Create The Client Object");
        }

    }

    @Override
    public void run() {
        try {
            client.connect(connOpts);
            client.subscribe(TEMP_TOPIC);
            client.subscribe(SOILHUMI_TOPIC);
            client.subscribe(CDS_TOPIC);
            client.subscribe(WATERLEVEL_TOPIC);
            client.setCallback(this);

        } catch (MqttException e) {
            Log.d(MQTT_TAG, "Error : Client Connect");
        }
    }

    //물 공급 버튼을 눌렀을 때
    public void waterSupply() {
        try {
            Log.d(MQTT_TAG, "물공급버튼을 눌렀습니다.");
            MqttMessage message = new MqttMessage(WATER_SUPPLY_MSG.getBytes());
            message.setQos(qos);
            client.publish(WATERPUMP_TOPIC, message);
        } catch (MqttException e) {
            Log.d(MQTT_TAG, "Error : WaterSupplyMsg Publish");
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.i(MQTT_TAG, "onDestroy");
        countDownTimer.cancel();

        /**
         * 서비스 종료 시 알람 등록을 통해 서비스 재 실행
         */
        registerRestartAlarm();
    }

    /**
     * 알람 매니져에 서비스 등록
     */
    private void registerRestartAlarm(){
        Log.i(MQTT_TAG, "registerRestartAlarm");
        Intent intent = new Intent(MqttService.this,RestartService.class);
        intent.setAction("ACTION.RESTART.MqttService");

        long firstTime = SystemClock.elapsedRealtime();
        firstTime += 1*1000;

        PendingIntent sender = PendingIntent.getBroadcast(MqttService.this,0,intent,0);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        /**
         * 알람 등록
         */
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,firstTime,1*1000,sender);
    }

    /**
     * 알람 매니져에 서비스 해제
     */
    private void unregisterRestartAlarm(){

        Log.i(MQTT_TAG, "unregisterRestartAlarm");
        Intent intent = new Intent(MqttService.this, RestartService.class);
        intent.setAction("ACTION.RESTART.MqttService");
        PendingIntent sender = PendingIntent.getBroadcast(MqttService.this,0,intent,0);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        /**
         * 알람 취소
         */
        alarmManager.cancel(sender);
    }

    @Override
    public IBinder onBind(Intent intent) {
        //Tast kill을 통해 서비스가 죽는걸 방지
        startForeground(1, new Notification());
        return mBinder;
    }

    @Override
    public void connectionLost(Throwable cause) {

    }

    //subscribe를 통해 받은 메시지를 처리하는 Callback Method
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        if (topic.equals(TEMP_TOPIC)) this.temp = new String(message.getPayload());
        if (topic.equals(SOILHUMI_TOPIC)) this.soilHumi = new String(message.getPayload());
        if (topic.equals(CDS_TOPIC)) this.cds = new String(message.getPayload());
        if (topic.equals(WATERLEVEL_TOPIC)) this.waterLevel = new String(message.getPayload());
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }

    public String getTemp() {
        return temp;
    }

    public String getSoilHumi() {
        return soilHumi;
    }

    public String getCds() {
        return cds;
    }

    public String getWaterLevel() {
        return waterLevel;
    }
}
