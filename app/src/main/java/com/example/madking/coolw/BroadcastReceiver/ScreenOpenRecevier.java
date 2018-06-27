package com.example.madking.coolw.BroadcastReceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.madking.coolw.service.AutoUpdateService;

public class ScreenOpenRecevier extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
       String action=intent.getAction();
       if (Intent.ACTION_SCREEN_OFF.equals(action)){

       }
       if (Intent.ACTION_SCREEN_ON.equals(action)){
           Intent i=new Intent(context, AutoUpdateService.class);
           context.startService(i);
       }
    }
}
