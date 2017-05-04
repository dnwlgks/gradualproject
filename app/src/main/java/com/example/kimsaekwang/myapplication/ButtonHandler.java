package com.example.kimsaekwang.myapplication;

import android.util.Log;
import android.view.View;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Created by 장규 on 2017-05-04.
 */

public class ButtonHandler implements View.OnClickListener {

    private static final String BTN_TAG = "ButtonHandler"; // Debug TAG

    private static final String WATER_SUPPLY_MSG = "1";
    private static final String WATERPUMP_TOPIC = "Test/WaterPump";// 물공급 Topic

    private int qos = 0;

    private MqttClient client;

    public ButtonHandler(MqttClient client) {
        this.client = client;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.waterBtn:
                waterSupply();
                break;
        }
    }

    //물 공급 버튼을 눌렀을 때
    public void waterSupply() {
        try {
            Log.d(BTN_TAG, "물공급버튼을 눌렀습니다.");
            MqttMessage message = new MqttMessage(WATER_SUPPLY_MSG.getBytes());
            message.setQos(qos);
            client.publish(WATERPUMP_TOPIC, message);
        } catch (MqttException e) {
            Log.d(BTN_TAG, "Error : WaterSupplyMsg Publish");
        }

    }

}
