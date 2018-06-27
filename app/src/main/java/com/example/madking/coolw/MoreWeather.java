package com.example.madking.coolw;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.madking.coolw.EVENTBUS.MessageEvent;
import com.example.madking.coolw.moreGson.Lifestyle;
import com.example.madking.coolw.moreGson.Mweather;
import com.example.madking.coolw.util.HttpUtil;
import com.example.madking.coolw.util.Utility;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class   MoreWeather extends AppCompatActivity {
    private TextView more_weather;
    private  static  String CURRENTWEATHERID;
    public LinearLayout Moremag_layout;
    String Lifestylename;
    private ImageView weatherImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.demo);
          /*
        view修改操作，隐藏标题栏,加个判断防止低版本崩溃
         */
        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        EventBus.getDefault().register(this);
        /**声名和找到布局文件*/
        Moremag_layout = (LinearLayout)findViewById(R.id.moreWW);
        weatherImage=findViewById(R.id.Moreweather_image);
        /*weatherId = getIntent().getStringExtra("MoreWeatherId");*/
        /*requestMoreWeather(weatherId);*/
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("mweather", null);//weatherString时http传回来的json字符串
        if (weatherString != null) {
            loadphoto();
            //有缓存时直接解析天气数据
            Mweather mweather = Utility.handleMMweatherResponse(weatherString);
            if (mweather.basic.cid!=CURRENTWEATHERID){
                requestMoreWeather(CURRENTWEATHERID);
            }else
            showMoreweather(mweather);
        } else {
            //没有缓存去服务器查询天气
            //String weatherId=getIntent().getStringExtra("weather_id");//weatherActivty传过来的weather_id,很麻烦这个
            requestMoreWeather(CURRENTWEATHERID);
        }
        String bingPic=prefs.getString("bing_pic",null);
        if (bingPic!=null){
            Glide.with(this).load(bingPic).into(weatherImage);
        }else {
            loadphoto();
        }
    }
    @Subscribe(threadMode = ThreadMode.MAIN,sticky = true)
    public void onEvent(MessageEvent messageEvent){
        CURRENTWEATHERID=messageEvent.getMessage();
        Log.d("weatherId",CURRENTWEATHERID);
    }

    private void requestMoreWeather(final String weatherId) {
        String weatherUrl = "https://free-api.heweather.com/s6/weather/lifestyle?" +
                "location=" + weatherId + "&key=eeb8aa5ef878424eadcab76191041f76";//拿到地址就开始解析
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {


            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();//拿到json字符串
                final Mweather mweather = Utility.handleMMweatherResponse(responseText);//获得Mweather实例*/
                runOnUiThread(new Runnable() {//开个子线程拿数据
                    @Override
                    public void run() {
                        Toast.makeText(MoreWeather.this, weatherId, Toast.LENGTH_SHORT).show();
                        if (mweather != null && "ok".equals(mweather.status)) {
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MoreWeather.this)
                                    .edit();
                            editor.putString("mweather", responseText);
                            editor.apply();
                            showMoreweather(mweather);
                        } else {
                            Toast.makeText(MoreWeather.this, "小cool还在路上，再试试看", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MoreWeather.this, "天气获取失败，请检查网络连接", Toast.LENGTH_SHORT).show();
                    }
                });

            }
        });
    }


    /**
     * 给ui设置数据
     */private void  loadphoto(){
    String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
        @Override
        public void onFailure(Call call, IOException e) {
            e.printStackTrace();
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            final String bingPic = response.body().string();
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(MoreWeather.this).edit();
            editor.putString("bing_pic", bingPic);
            editor.apply();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Glide.with(MoreWeather.this).load(bingPic).into(weatherImage);
                }
            });
        }
    });
    }
    private void showMoreweather(Mweather mweather) {

        Moremag_layout.removeAllViews();
       /* runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Glide.with(MoreWeather.this).load("https://cn.bing.com/az/hprichbg/rb/Nyubai_ROW11154199893_1920x1080.jpg").into(weatherImage);
                Log.d("55555","5555");
            }
        });*/

        for (Lifestyle lifestyle : mweather.lifestyleList) {
            View view = LayoutInflater.from(this).inflate(R.layout.more_weather_item,Moremag_layout, false);
            TextView titleText = (TextView) view.findViewById(R.id.moretitle);
            TextView infoText = (TextView) view.findViewById(R.id.moreinfo);
            titleText.setText(getLifestylename(lifestyle.type) + ":" + lifestyle.brf);

            infoText.setText(lifestyle.txt);
            Moremag_layout.addView(view);
            Log.d("nnn",getLifestylename(lifestyle.type) + ":" + lifestyle.brf);
        }


    }
    public String getLifestylename(String T) {
        if (T.equals("comf")){
            Lifestylename="舒适度指数";
        }else
        if (T.equals("drsg") ){
            Lifestylename="穿衣指数";
        }else
        if (T.equals("flu")){
            Lifestylename="流感指数";
        }else
        if (T.equals("sport")){
            Lifestylename="运动指数";
        }else
        if (T.equals("trav")){
            Lifestylename="游玩指数";
        }else
        if (T.equals("cw") ){
            Lifestylename="洗车指数";
        }else
        if (T.equals("uv") ){
            Lifestylename="紫外线指数";
        }else
        if (T.equals("air")){
            Lifestylename="空气指数";
        }
        return Lifestylename;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}