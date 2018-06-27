package com.example.madking.coolw;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.example.madking.coolw.EVENTBUS.MessageEvent;
import com.example.madking.coolw.db.City;
import com.example.madking.coolw.db.County;
import com.example.madking.coolw.db.Province;
import com.example.madking.coolw.gson.Weather;
import com.example.madking.coolw.util.Utility;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SharedPreferences pref= PreferenceManager.getDefaultSharedPreferences(this);
        if (pref.getString("weather",null)!=null){
            String weatherString=pref.getString("weather",null);
                Weather weather= Utility.handleWeatherResponse(weatherString);
                String weatherId=weather.basic.weatherId;
            Log.d("main activty",weatherId);
            EventBus.getDefault().postSticky(new MessageEvent(weatherId));
            Intent intent=new Intent(this,WeatherActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
