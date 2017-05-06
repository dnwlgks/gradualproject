package com.example.kimsaekwang.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class RestartService extends BroadcastReceiver {

    private static final String RESTART_SERVICE_TAG = "RestartService";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(RESTART_SERVICE_TAG, "RestartSerivce Called : " + intent.getAction());

        /**
         * 서비스 죽일때 알람으로 다시 서비스 등록
         */
        if(intent.getAction().equals("ACTION.RESTART.MqttService")){

            Log.i(RESTART_SERVICE_TAG, "ACTION.RESTART.MqttService");

            Intent i = new Intent(context, MqttService.class);
            context.startService(i);
        }

        /**
         * 폰 재시작 할때 서비스 등록
         * 바인딩서비스는 메인 액티비티를 켰을때
         */
        if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)){

            Log.i(RESTART_SERVICE_TAG, "ACTION_BOOT_COMPLETED");
            Intent i = new Intent(context, MqttService.class);
            context.startService(i);

        }
    }
}
