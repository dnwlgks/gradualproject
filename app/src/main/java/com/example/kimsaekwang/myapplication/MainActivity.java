package com.example.kimsaekwang.myapplication;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, MqttCallback {


    private static final String MQTT_TAG = "MqttService"; // Debug TAG

    private static final String MQTT_THREAD_NAME = "MqttService[" + MQTT_TAG + "]"; // Handler Thread ID

    private static final String WATER_SUPPLY_MSG = "1";
    private static final String SYNCHRONIZE_MSG = "2";
    private static final String SYNCHRONIZE_TOPIC = "Test/Synchronize";// 동기화 Topic
    private static final String TEMP_TOPIC = "Test/Temp";// 온도 Topic
    private static final String SOILHUMI_TOPIC = "Test/Soilhumi";// 습도 Topic
    private static final String CDS_TOPIC = "Test/Cds";// 조도 Topic
    private static final String WATERLEVEL_TOPIC = "Test/WaterLevel";// 수위 Topic
    private static final String WATERPUMP_TOPIC = "Test/WaterPump";// 물공급 Topic


    int qos = 0;
    String broker = "tcp://223.194.134.96:1883";
    String clientId = "JangGyoo Seo";

    Handler mConnHandler;

    MqttClient client;
    MemoryPersistence persistence;
    MqttConnectOptions connOpts;

    //Status Value
    String temp = null;
    String soilHumi = null;
    String cds = null;
    String waterLevel = null;

    TextView tempTxt = (TextView) findViewById(R.id.temp);
    TextView soilhumiTxt = (TextView) findViewById(R.id.humi);
    TextView cdsTxt = (TextView) findViewById(R.id.cds);
    TextView waterLevelTxt = (TextView) findViewById(R.id.waterLevel);
    TextView statTxt = (TextView) findViewById(R.id.status);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

    }

    public void init() {

        HandlerThread thread = new HandlerThread(MQTT_THREAD_NAME);
        thread.start();

        mConnHandler = new Handler(thread.getLooper());

        connect();

    }

    private void connect() {
        try {
            persistence = new MemoryPersistence();
            client = new MqttClient(broker, clientId, persistence);
            connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
        } catch (MqttException e) {
            Log.d(MQTT_TAG, "Error : Create The Client Object");

            mConnHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        client.connect(connOpts);
                        client.subscribe(TEMP_TOPIC);
                        client.subscribe(SOILHUMI_TOPIC);
                        client.subscribe(CDS_TOPIC);
                        client.subscribe(WATERLEVEL_TOPIC);
                        client.setCallback(MainActivity.this);

                    } catch (MqttException e) {
                        Log.d(MQTT_TAG, "Error : Client Connect");
                    }
                }
            });
        }


    }

    @Override
    public void onClick(View v) {
        int btnId = v.getId();
        switch (btnId) {
            case R.id.waterBtn:
                waterSupply();
                break;
            case R.id.synchronizedBtn:
                PubSynchronize();
                break;
        }

    }

    //물 공급 버튼을 눌렀을 때
    public void waterSupply() {
        try {
            MqttMessage message = new MqttMessage(WATER_SUPPLY_MSG.getBytes());
            message.setQos(qos);
            client.publish(WATERPUMP_TOPIC, message);
        } catch (MqttException e) {
            Log.d(MQTT_TAG, "Error : WaterSupplyMsg Publish");
        }

    }

    //동기화 버튼을 눌렀을 때
    public void PubSynchronize() {
        try {
            MqttMessage message = new MqttMessage(SYNCHRONIZE_MSG.getBytes());
            message.setQos(qos);
            client.publish(SYNCHRONIZE_TOPIC, message);
        } catch (MqttException e) {
            Log.d(MQTT_TAG, "Error : SynchronizeMsg Publish");
        }
    }

    //동기화 작업
    public void synchronizedStatus() {
        tempTxt.setText(temp);
        soilhumiTxt.setText(soilHumi);
        cdsTxt.setText(cds);
        waterLevelTxt.setText(waterLevel);
        //식물의 상태 초기화
        this.temp = null;
        this.soilHumi = null;
        this.cds = null;
        this.waterLevel = null;


        //식물의 상태 동기화 기능 추가 필요
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
        if (this.temp != null && this.soilHumi != null && this.cds != null && this.waterLevel != null) {
            synchronizedStatus();
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }
}