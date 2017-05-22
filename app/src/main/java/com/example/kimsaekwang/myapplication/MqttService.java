package com.example.kimsaekwang.myapplication;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
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

    private static final int SOIL_HUMI_CHECK_INTEVAL = 2 * 60 * 60 * 1000; // 2시간간격

    private static final String TEMP_TOPIC = "Test/Temp";// 온도 Topic
    private static final String SOILHUMI_TOPIC = "Test/Soilhumi";// 습도 Topic
    private static final String CDS_TOPIC = "Test/Cds";// 조도 Topic
    private static final String WATERLEVEL_TOPIC = "Test/WaterLevel";// 수위 Topic
    private static final String WATERPUMP_TOPIC = "Test/WaterPump";// 물공급 Topic

    private static final String WATER_SUPPLY_MSG = "1";

    private int qos = 0;

    private String broker = "tcp://223.194.134.58:1883";
    private String clientId = "JangGyooSeo";

    private MqttClient client;
    private MemoryPersistence persistence;
    private MqttConnectOptions connOpts;

    private final IBinder mBinder = new LocalBinder();    // 컴포넌트에 반환되는 IBinder

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    // 컴포넌트에 반환해줄 IBinder를 위한 클래스
    public class LocalBinder extends Binder {
        MqttService getService() {
            return MqttService.this;//현재 서비스를 반환
        }
    }
    //콜백 인터페이스 선언
    public interface ICallback {
        public void recvData(String topic, String message);//액티비티에서 선언한 콜백 함수
    }

    private ICallback mCallback;

    //액티비티에서 콜백 함수를 등록하기 위함.
    public void registerCallback(ICallback cb) {
        this.mCallback = cb;
    }

    NotificationManager nm;

    @Override
    public void onCreate() {
        super.onCreate();

        init();
    }


    private void init() {
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        connect();

        //subscribe
        Thread thread = new Thread(this);
        thread.start();

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




    @Override
    public void connectionLost(Throwable cause) {

    }

    //subscribe를 통해 받은 메시지를 처리하는 Callback Method
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        mCallback.recvData(topic,new String(message.getPayload()));

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }
}
