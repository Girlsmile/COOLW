package com.example.madking.coolw.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.example.madking.coolw.BroadcastReceiver.AlarmRecevier;
import com.example.madking.coolw.BroadcastReceiver.ScreenOpenRecevier;
import com.example.madking.coolw.WeatherActivity;
import com.example.madking.coolw.gson.Weather;
import com.example.madking.coolw.util.HttpUtil;
import com.example.madking.coolw.util.NotificationUtils;
import com.example.madking.coolw.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AutoUpdateService extends Service {

 private ScreenOpenRecevier receiver;

    public AutoUpdateService() {
    }

    @Override//服务创建
    public void onCreate() {
        super.onCreate();
          receiver=new ScreenOpenRecevier();
        IntentFilter filter=new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(receiver,filter);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override//服务开始运行的函数，注意要服务创建才可以，
    public int onStartCommand(Intent intent, int flags, int startId) {

        updateWeather();
        updateBingPic();
        updateNotification();
        Log.d("111","2222");

        AlarmManager manager= (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent i=new Intent(this,AlarmRecevier.class);
        //　pendingIntent是一种特殊的Intent。主要的区别在于Intent的执行立刻的，而pendingIntent的执行不是立刻的。
        // pendingIntent执行的操作实质上是参数传进来的Intent的操作，但是使用pendingIntent的
        // 目的在于它所包含的Intent的操作的执行是需要满足某些条件的。
        PendingIntent pi=PendingIntent.getBroadcast(this,0, i,0);//Pending打开一个广播
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,SystemClock.elapsedRealtime()+ (1000 * 60*60),pi);//该方法用于设置一次性闹钟，第一个参数表示闹钟类型，
                                                                           // 第二个参数表示闹钟执行时间，第三个参数表示闹钟响应动作。这里拿来定时启动服务
                                                                             // RTC 在指定的时刻，打开服务，但不唤醒设备

        return super.onStartCommand(intent, flags, startId);

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    /**
     * 更新天气信息。
     */

    private void updateWeather(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather", null);
        if (weatherString != null) {
            // 有缓存时直接解析天气数据
            Weather weather = Utility.handleWeatherResponse(weatherString);
            String weatherId = weather.basic.weatherId;
            String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=eeb8aa5ef878424eadcab76191041f76 ";
            HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseText = response.body().string();
                    Weather weather = Utility.handleWeatherResponse(responseText);
                    if (weather != null && "ok".equals(weather.status)) {
                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                        editor.putString("weather", responseText);
                        editor.apply();
                    }
                }

                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    /**
     * 更新必应每日一图
     */
    private void updateBingPic() {
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                editor.putString("bing_pic", bingPic);
                editor.apply();
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }
        });
    }
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void updateNotification(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather", null);
        Weather weather = Utility.handleWeatherResponse(weatherString);

        String title=weather.hourlyList.get(0).tmp+"℃"+"  "+weather.aqi.city.qlty;
        String content=weather.hourlyList.get(0).cond_txt+"\u3000"+weather.basic.cityName+"\u3000"+"发布于:"+weather.basic.update.updateTime.
                substring(weather.basic.update.updateTime.length()-5,weather.basic.update.updateTime.length());;
        NotificationUtils notificationUtils=new NotificationUtils(this);
        notificationUtils.sendNotification(title,content,createPendingIntent());
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private PendingIntent createPendingIntent(){
        Intent intent=new Intent();
        intent.setClass(this,WeatherActivity.class);
        //创建返回栈
        TaskStackBuilder stackBuilder=TaskStackBuilder.create(this);
        //添加activity的返回栈
        stackBuilder.addParentStack(WeatherActivity.class);
        //添加intent到栈顶
        stackBuilder.addNextIntent(intent);
        PendingIntent pendingIntent=stackBuilder.getPendingIntent(0,PendingIntent.FLAG_UPDATE_CURRENT);
        return pendingIntent;
    }

}