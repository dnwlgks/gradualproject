package com.example.kimsaekwang.myapplication;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttService extends Service implements MqttCallback, Runnable {

    private static final String MQTT_TAG = "MqttService"; // Debug TAG

    private static final int MILLISINFUTURE = 1000 * 1000;
    private static final int COUNT_DOWN_INTERVAL = 1000;

    private static final int SOIL_HUMI_CHECK_INTEVAL = 2 * 60 * 60 * 1000; // 2시간간격

    private static final String TEMP_TOPIC = "Test/Temp";// 온도 Topic
    private static final String SOILHUMI_TOPIC = "Test/Soilhumi";// 습도 Topic
    private static final String CDS_TOPIC = "Test/Cds";// 조도 Topic
    private static final String WATERLEVEL_TOPIC = "Test/WaterLevel";// 수위 Topic
    private static final String WATERPUMP_TOPIC = "Test/WaterPump";// 물공급 Topic

    private static final String WATER_SUPPLY_MSG = "1";

    private int qos = 0;

    private String broker = "tcp://172.30.1.28:1883";
    private String clientId = "JangGyooSeo";

    private MqttClient client;
    private MemoryPersistence persistence;
    private MqttConnectOptions connOpts;

    private final IBinder mBinder = new LocalBinder();    // 컴포넌트에 반환되는 IBinder

    // 컴포넌트에 반환해줄 IBinder를 위한 클래스
    public class LocalBinder extends Binder {
        MqttService getService() {
            return MqttService.this;
        }
    }

    CountDownTimer countDownTimer;

    NotificationManager nm;

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
        customizedNotification(1,"GradualApplication","GradualApplication이 실행중입니다.");

        /**
         * startForeground 에서 보여주는 notification을 커스텀.
         */
        Notification notification;
        Notification.Builder builder = new Notification.Builder(MqttService.this)
                .setSmallIcon(android.R.drawable.btn_star)
                .setContentTitle("GradualApplication")
                .setContentText("GradualApplication이 실행중입니다.")
                .setPriority(Notification.PRIORITY_MIN);
        notification = builder.build();
        nm.notify(1, notification);

        return super.onStartCommand(intent, flags, startId);
    }

    private void init() {
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        connect();

        //subscribe
        Thread thread = new Thread(this);
        thread.start();

        countDownTimerSetting();
        countDownTimer.start();

        soilHumiCheck();
    }

    private void countDownTimerSetting() {
        countDownTimer = new CountDownTimer(MILLISINFUTURE, COUNT_DOWN_INTERVAL) {
            @Override
            public void onTick(long millisUntilFinished) {
                Log.i(MQTT_TAG, "onTick");
            }

            @Override
            public void onFinish() {
                Log.i(MQTT_TAG, "onFinish");
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

    //msg publish
    public void pubMessage(String topic, String msg) {
        try {
            Log.d(MQTT_TAG, "pubMessage");
            MqttMessage message = new MqttMessage(msg.getBytes());
            message.setQos(qos);
            client.publish(topic, message);
        } catch (MqttException e) {
            Log.d(MQTT_TAG, "Error : Msg Publish");
        }

        if (topic.equals(WATERPUMP_TOPIC)) waterLevelCheck();
    }

    private void waterLevelCheck() {
        Thread thread = new Thread(new Runnable(){
            SharedPreferences pref = getSharedPreferences("Stat", Activity.MODE_PRIVATE);
            @Override
            public void run() {
                try {
                    Thread.sleep(7000);
                    if (Integer.parseInt(pref.getString("waterLevel", null)) <= 30) {
                        addNotification(1234, "물탱크에 물이 30% 미만 입니다.", "물탱크에 물을 더 채워넣어주세요");
                    }
                } catch (InterruptedException e) {
                    Log.d(MQTT_TAG, "Error : 잔량체크 오류");
                }
            }
        });


    }

    private void soilHumiCheck() {


        Thread thread = new Thread(new Runnable() {
            SharedPreferences pref = getSharedPreferences("Stat", Activity.MODE_PRIVATE);

            @Override
            public void run() {
                while (true) {
                    try {
                        if (Integer.parseInt(pref.getString("soulhumi", null)) <= 30) {
                            pubMessage(WATERPUMP_TOPIC, WATER_SUPPLY_MSG);
                        }
                        Thread.sleep(SOIL_HUMI_CHECK_INTEVAL);
                    } catch (InterruptedException e) {
                        Log.d(MQTT_TAG, "Error : soilHumiCheck");
                    }
                }
            }
        });

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
    private void registerRestartAlarm() {
        Log.i(MQTT_TAG, "registerRestartAlarm");
        Intent intent = new Intent(MqttService.this, RestartService.class);
        intent.setAction("ACTION.RESTART.MqttService");

        long firstTime = SystemClock.elapsedRealtime();
        firstTime += 1 * 1000;

        PendingIntent sender = PendingIntent.getBroadcast(MqttService.this, 0, intent, 0);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        /**
         * 알람 등록
         */
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, firstTime, 1 * 1000, sender);
    }

    /**
     * 알람 매니져에 서비스 해제
     */
    private void unregisterRestartAlarm() {

        Log.i(MQTT_TAG, "unregisterRestartAlarm");
        Intent intent = new Intent(MqttService.this, RestartService.class);
        intent.setAction("ACTION.RESTART.MqttService");
        PendingIntent sender = PendingIntent.getBroadcast(MqttService.this, 0, intent, 0);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        /**
         * 알람 취소
         */
        alarmManager.cancel(sender);
    }

    /**
     * Notification을 띄워주는 메소드
     */
    private void addNotification(int id, String title, String text) {


        // PendingIntent를 등록 하고, noti를 클릭시에 어떤 클래스를 호출 할 것인지 등록.
        PendingIntent intent = PendingIntent.getActivity(
                MqttService.this, 0,
                new Intent(MqttService.this, MainActivity.class), 0);

        Notification notification = new Notification();
        notification.flags |= Notification.FLAG_SHOW_LIGHTS;
        notification.flags |= Notification.FLAG_AUTO_CANCEL;// noti를 클릭 했을 경우 자동으로 noti Icon 제거
        notification.defaults = Notification.DEFAULT_ALL;
        notification.when = System.currentTimeMillis();

        Notification.Builder builder = new Notification.Builder(getApplicationContext())
                .setContentIntent(intent)
                .setSmallIcon(android.R.drawable.btn_star)
                .setContentTitle(title)
                .setContentText(text)
                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);
        notification = builder.build();
        nm.notify(id, notification);
        Toast.makeText(MqttService.this, "Notification Registered.",
                Toast.LENGTH_SHORT).show();

    }

    /**
     * startForeground 에서 보여주는 notification을 커스텀.
     */
    private void customizedNotification(int id, String title, String text){
        Notification notification;
        Notification.Builder builder = new Notification.Builder(MqttService.this)
                .setSmallIcon(android.R.drawable.btn_star)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(Notification.PRIORITY_MIN);
        notification = builder.build();
        nm.notify(id, notification);
    }

    @Override
    public IBinder onBind(Intent intent) {
        //Tast kill을 통해 서비스가 죽는걸 방지
        startForeground(1, new Notification());
        customizedNotification(1,"GradualApplication","GradualApplication이 실행중입니다.");

        return mBinder;
    }

    @Override
    public void connectionLost(Throwable cause) {

    }

    //subscribe를 통해 받은 메시지를 처리하는 Callback Method
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        SharedPreferences pref = getSharedPreferences("Stat", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        if (topic.equals(TEMP_TOPIC)) editor.putString("colortemp", new String(message.getPayload()));
        if (topic.equals(SOILHUMI_TOPIC))
            editor.putString("soilhumi", new String(message.getPayload()));
        if (topic.equals(CDS_TOPIC)) editor.putString("cds", new String(message.getPayload()));
        if (topic.equals(WATERLEVEL_TOPIC))
            editor.putString("waterLevel", new String(message.getPayload()));
        unregisterRestartAlarm();

        //동기화된 시간또한 적어주기.

        editor.commit();
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }
}
