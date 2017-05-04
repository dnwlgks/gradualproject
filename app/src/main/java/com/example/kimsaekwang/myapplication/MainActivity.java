package com.example.kimsaekwang.myapplication;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MainActivity extends AppCompatActivity implements MqttCallback {


    private static final String MQTT_TAG = "MqttService"; // Debug TAG

    private static final String MQTT_THREAD_NAME = "MqttService[" + MQTT_TAG + "]"; // Handler Thread ID

//    private static final String SYNCHRONIZE_MSG = "2";
//    private static final String SYNCHRONIZE_TOPIC = "Test/Synchronize";// 동기화 Topic
    private static final String TEMP_TOPIC = "Test/Temp";// 온도 Topic
    private static final String SOILHUMI_TOPIC = "Test/Soilhumi";// 습도 Topic
    private static final String CDS_TOPIC = "Test/Cds";// 조도 Topic
    private static final String WATERLEVEL_TOPIC = "Test/WaterLevel";// 수위 Topic

    private String broker = "tcp://192.168.0.3:1883";
    private String clientId = "JangGyooSeo";

    private Handler mConnHandler;

    private MqttClient client;
    private MemoryPersistence persistence;
    private MqttConnectOptions connOpts;

    //Status Value
    private String temp;
    private String soilHumi;
    private String cds;
    private String waterLevel;

    private TextView tempTxt;
    private TextView soilhumiTxt;
    private TextView cdsTxt;
    private TextView waterLevelTxt;
    private TextView statTxt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

    }

    public void init() {
        Button waterBtn = (Button) findViewById(R.id.waterBtn);
        ImageButton synBtn = (ImageButton) findViewById(R.id.synchronizedBtn);

        tempTxt = (TextView) findViewById(R.id.temp);
        soilhumiTxt = (TextView) findViewById(R.id.soilhumi);
        cdsTxt = (TextView) findViewById(R.id.cds);
        waterLevelTxt = (TextView) findViewById(R.id.waterLevel);
        statTxt = (TextView) findViewById(R.id.status);

        HandlerThread thread = new HandlerThread(MQTT_THREAD_NAME);
        thread.start();

        mConnHandler = new Handler(thread.getLooper());

        Log.d(MQTT_TAG, "connect전까지 실행");
        connect();

        //Enroll Btn Event
        ButtonHandler btnHandler = new ButtonHandler(client);
        waterBtn.setOnClickListener(btnHandler);
        synBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tempTxt.setText(temp);
                soilhumiTxt.setText(soilHumi);
                cdsTxt.setText(cds);
                waterLevelTxt.setText(waterLevel);
            }
        });

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
}