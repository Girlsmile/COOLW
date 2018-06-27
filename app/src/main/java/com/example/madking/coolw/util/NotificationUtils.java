package com.example.madking.coolw.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;

import com.example.madking.coolw.R;

public class NotificationUtils extends ContextWrapper {
    private NotificationManager manager;
    public   String id= "channel_1";
    public    String name="channel_name_1";
    public NotificationUtils(Context base) {
        super(base);
    }
    //第一步声名Notificatiommanager实例
    private  NotificationManager getManager(){
        if (manager==null){
            manager= (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        }
        return manager;
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void createNotifcationChannel(){
        NotificationChannel channel=new NotificationChannel(id,name,NotificationManager.IMPORTANCE_HIGH);
        getManager().createNotificationChannel(channel);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public Notification.Builder getChannelNotification(String title, String content,PendingIntent pendingIntent){
        return new Notification.Builder(getApplicationContext(),id)
                .setContentTitle(title)
                .setContentText(content)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.app)
                .setAutoCancel(true);
    }
    public NotificationCompat.Builder getNotficaition_25(String title,String content,PendingIntent pendingIntent){
        return new NotificationCompat.Builder(getApplicationContext())
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.mipmap.app)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
    }
    public void sendNotification(String title,String content,PendingIntent pendingIntent){
        if (Build.VERSION.SDK_INT>=26){
            createNotifcationChannel();
            Notification notification=getChannelNotification(title,content,pendingIntent).build();
            getManager().notify(1,notification);
        }else {
            Notification notification=getNotficaition_25(title,content,pendingIntent).build();
            getManager().notify(1,notification);
        }
    }

}
