package com.example.madking.coolw;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.LoginFilter;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.madking.coolw.BroadcastReceiver.AlarmRecevier;
import com.example.madking.coolw.EVENTBUS.MessageEvent;
import com.example.madking.coolw.gson.Forecast;
import com.example.madking.coolw.gson.Weather;
import com.example.madking.coolw.moreGson.Mweather;
import com.example.madking.coolw.service.AutoUpdateService;
import com.example.madking.coolw.util.HttpUtil;
import com.example.madking.coolw.util.NotificationUtils;
import com.example.madking.coolw.util.Utility;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lecho.lib.hellocharts.gesture.ContainerScrollType;
import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.ValueShape;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;

public class WeatherActivity extends AppCompatActivity {
    /*
    temline  layout属性的定义
     */
    private LineChartView lineChart;
    private LineChartView lineChartHourly;
     String[] date=new String[7];//downX轴的标注,这里传的日期
    String[] datetop=new String[7];//topX轴的标注,这里传的天气
     int[] maxscore=new int[7];//图表的数据点，这里传的是最高温度
      int[] minscore=new int[7];//图表的数据点，这里传的是最低温度

    String[] hourlytime=new String[8];//downX轴的标注,这里传的时间和cond_text
    String[] hourlytop=new String[8];//topX轴的标注,这里传的风力和等级
    int[] hourlytem=new int[8];//图表的数据点，这里传的是温度

    private List<PointValue> mPointValues = new ArrayList<PointValue>();
    private List<PointValue> mPointValues1 = new ArrayList<PointValue>();
    private List<AxisValue> mAxisValues = new ArrayList<AxisValue>();
    private List<AxisValue> mAxisValues1 = new ArrayList<AxisValue>();

    private List<PointValue> HPointValues = new ArrayList<PointValue>();
    private List<AxisValue> HAxisValuesdown = new ArrayList<AxisValue>();
    private List<AxisValue> HAxisValuetop = new ArrayList<AxisValue>();

    /*
    普通layout文件的属性定义
     */
    private PopupWindow popupWindow;
    public DrawerLayout drawerLayout;

    public SwipeRefreshLayout swipeRefresh;//下拉刷新

    private ScrollView weatherLayout;

    private Button navButton;


    private TextView titleCity;

    private TextView titleUpdateTime;

    private TextView degreeText;

    private TextView weatherInfoText;

    private LinearLayout forecastLayout;
    private TextView air_text;

    private TextView aqiText;

    private TextView pm25Text;

    private TextView comfortText;

    private TextView carWashText;

    private TextView sportText;

    private ImageView bingPicImg;//背景图

    private String SPEECHVOICE;
    private String mWeatherId;
    protected String MoreWeatherId;
    private FloatingActionButton more_show;
    private static String CURRENTWEATHERID;

    private TextToSpeech textToSpeech;
    private NotificationUtils notificationUtils;
    private Button speech;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        navButton = findViewById(R.id.nav_button);

        EventBus.getDefault().register(this);


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
        setContentView(R.layout.activity_weather);

        /*
        调用一个textToSpeech,实现它接口并初始化
         */
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == textToSpeech.SUCCESS) {
                    int result = textToSpeech.setLanguage(Locale.CHINA);//中文
                    if (result != TextToSpeech.LANG_COUNTRY_AVAILABLE
                            && result != TextToSpeech.LANG_AVAILABLE){
                        Toast.makeText(WeatherActivity.this, "TTS暂时不支持这种语音的朗读！",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        /*
        初始化控件
         */

        more_show = findViewById(R.id.more_open);
        //more_show.getBackground().setAlpha(100);
        bingPicImg = (ImageView) findViewById(R.id.bing_pic_img);
        weatherLayout = (ScrollView) findViewById(R.id.weather_layout);
        titleCity = (TextView) findViewById(R.id.title_city);
        titleUpdateTime = (TextView) findViewById(R.id.title_update_time);
        degreeText = (TextView) findViewById(R.id.degree_text);
        weatherInfoText = (TextView) findViewById(R.id.weather_info_text);
        forecastLayout = (LinearLayout) findViewById(R.id.forecast_layout);
        aqiText = (TextView) findViewById(R.id.aqi_text);
        air_text=findViewById(R.id.air_text);
        pm25Text = (TextView) findViewById(R.id.pm25_text);
        comfortText = (TextView) findViewById(R.id.comfort_text);
        carWashText = (TextView) findViewById(R.id.car_wash_text);
        sportText = (TextView) findViewById(R.id.sport_text);
        swipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);//下拉刷新
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navButton = (Button) findViewById(R.id.nav_button);
        navButton.getBackground().setAlpha(150);
        lineChart = findViewById(R.id.line_chart);
        lineChartHourly=findViewById(R.id.line_chart_hourly);
         speech=findViewById(R.id.speech);

        /*
        各种View监听
         */

        //drawLayout操作
        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        //没什么技术含量copy就好，接下来用到SharedPreference
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);//获得一个SharedPreference实例，记住三个途径
        String weatherString = prefs.getString("weather", null);
        //目标名。默认值，xml文件而已不要想的那么复杂
        final String weatherId;
        if (weatherString != null) {


            Weather weather = Utility.handleWeatherResponse(weatherString);
            weatherId = weather.basic.weatherId;

            MoreWeatherId = weatherId;//chooseAreaFragment传过来的weather_id
            showWeatherInfo(weather);

            //有缓存是直接解析天气数据
        } else {
            //去服务器查
            weatherId = getIntent().getStringExtra("weather_id");
            MoreWeatherId = weatherId;//chooseAreaFragment传过来的weather_id
            weatherLayout.setVisibility(View.INVISIBLE);//应为没有数据所以把layout隐蔽起来，不过构造很奇怪啊
            requestWeather(CURRENTWEATHERID);//顾名思义
        }
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(CURRENTWEATHERID);
            }
        });
        //加载背景图
        String bingPic = prefs.getString("bing_pic", null);
        if (bingPic != null) {
            Glide.with(this).load(bingPic).into(bingPicImg);
        } else {
            loadBingPic();
        }
        /*
        showmore

         */

        more_show.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                View view = getLayoutInflater().inflate(R.layout.popuplayout, null);
                Button startS = view.findViewById(R.id.startS);
                Button stopS = view.findViewById(R.id.stopS);
                Button watchmore = view.findViewById(R.id.watch_more);
                popupWindow = new PopupWindow(view, 500, 500, true);
                popupWindow.setAnimationStyle(R.style.MyPopupWindow_anim_style);
                popupWindow.setContentView(view);
                popupWindow.setOutsideTouchable(true);
                popupWindow.setFocusable(true);
                popupWindow.setBackgroundDrawable(new BitmapDrawable());
                /*ColorDrawable dw = new ColorDrawable(Color.TRANSPARENT);
               popupWindow.setBackgroundDrawable(dw);*/
                popupWindow.showAtLocation(findViewById(R.id.more_open), Gravity.BOTTOM, 300, 160);
                /*showAsDropDown(popupWindow,more_show,0,0);*/
                startS.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent starServiceintent = new Intent(WeatherActivity.this, AutoUpdateService.class);
                        startService(starServiceintent);//服务启动
                        Toast.makeText(WeatherActivity.this, "自动更新开启", Toast.LENGTH_SHORT).show();
                        popupWindow.dismiss();
                    }
                });

                stopS.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent stopServiceintent = new Intent(WeatherActivity.this, AutoUpdateService.class);
                        stopService(stopServiceintent);//服务关闭
                        Toast.makeText(WeatherActivity.this, "自动更新关闭", Toast.LENGTH_SHORT).show();
                        popupWindow.dismiss();
                    }
                });

                watchmore.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(WeatherActivity.this, MoreWeather.class);
                        intent.putExtra("MoreWeatherId", MoreWeatherId);
                        startActivity(intent);
                        popupWindow.dismiss();
                    }
                });


            }
        });
        /*
        speech
         */
      speech.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
              textToSpeech.speak(SPEECHVOICE,
                      TextToSpeech.QUEUE_ADD, null);
          }
      });
       /*
        getAxisXLables();//获取下面的x轴坐标

        getAxisXtopLables();//获取上面的x轴坐标
      */

    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onEvent(MessageEvent messageEvent) {
        CURRENTWEATHERID = messageEvent.getMessage();
    }


    /**
     * 接下来根据weatherid查找城市信息
     */

//&key=eeb8aa5ef878424eadcab76191041f76          &key=bc0418b57b2d4918819d3974ac1285d9
    public void requestWeather(final String weatherId) {
        String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=eeb8aa5ef878424eadcab76191041f76";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);
                //开个ui线程刷新，原理回去看服务哪一part
                runOnUiThread(new Runnable() {
                    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
                    @Override
                    public void run() {
                        if (weather != null && "ok".equals(weather.status)) {
                            SharedPreferences.Editor editor = PreferenceManager.
                                    getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather", responseText);
                            editor.apply();
                            mWeatherId = weather.basic.weatherId;
                            showWeatherInfo(weather);//注意这里存的是weather对象
                            Toast.makeText(getApplicationContext(), "天气已经是最新的哦", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(WeatherActivity.this, "获取天气信息失败，免费访问次数已用完", Toast.LENGTH_SHORT).show();
                        }
                        swipeRefresh.setRefreshing(false);//刷新玩关闭
                    }
                });

            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();//这里告诉你出问题的位置
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        swipeRefresh.setRefreshing(false);//刷新玩关闭
                    }
                });

            }
        });
        loadBingPic();
    }

    /**
     * 加载每日一图
     */
    private void loadBingPic() {
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic", bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
                    }
                });
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 处理Weather实体类的数据，COPY就好，没什么技术难度
     */

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void showWeatherInfo(Weather weather) {
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature + "℃";
        String weatherInfo = weather.now.more.info;
        String weatherInfo2=weather.now.wind_dir+weather.now.wind_sc+"级"+" 大气压强："+weather.now.pres+"hPa";
        titleCity.setText(cityName);
        titleUpdateTime.setText("最近发布：" + updateTime);
        air_text.setText("空气："+weather.aqi.city.qlty);
        degreeText.setText(degree+" "+weatherInfo);
        weatherInfoText.setText(weatherInfo2);
        Log.d("6666666",weather.now.wind_dir);
        forecastLayout.removeAllViews();
        for (Forecast forecast : weather.forecastList) {
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout, false);
            TextView dateText = (TextView) view.findViewById(R.id.date_text);
            TextView infoText = (TextView) view.findViewById(R.id.info_text);
            TextView maxText = (TextView) view.findViewById(R.id.max_text);
            TextView minText = (TextView) view.findViewById(R.id.min_text);
            dateText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max + "℃");
            minText.setText(forecast.temperature.min + "℃");
            forecastLayout.addView(view);
        }//foreach语句实现接下来几天数据的遍历


        setLineValue(weather);
  /*
         temline的数据初始化
         */

        getAxisXLables();//获取下面的x轴坐标
        getAxisXtopLables();//获取上面的x轴坐标
        getAxisXPoints();//获取坐标点

        initLineChart();//初始化

        if (weather.aqi != null) {
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }
        String comfort = "舒适度：" + weather.suggestion.comfort.info;
        String carWash = "洗车指数：" + weather.suggestion.carWash.info;
        String sport = "运行建议：" + weather.suggestion.sport.info;
        comfortText.setText(comfort);
        carWashText.setText(carWash);
        sportText.setText(sport);
        weatherLayout.setVisibility(View.VISIBLE);
        /*Intent serintent=new Intent(this,AutoUpdateService.class);
                startService(serintent);*/
        SPEECHVOICE=initSpeechVoice(weather);
        Log.d("11111",SPEECHVOICE);
        String title=weather.hourlyList.get(0).tmp+"℃"+"  "+weather.aqi.city.qlty;
        String content=weather.hourlyList.get(0).cond_txt+"\u3000"+weather.basic.cityName+"\u3000"+"发布于:"+weather.basic.update.updateTime.
                substring(weather.basic.update.updateTime.length()-5,weather.basic.update.updateTime.length());;
        NotificationUtils notificationUtils=new NotificationUtils(this);
        notificationUtils.sendNotification(title,content,createPendingIntent());
    }

   /* public static void showAsDropDown(PopupWindow pw, View anchor, int xoff, int yoff) {
        if (Build.VERSION.SDK_INT >= 24) {
            Rect visibleFrame = new Rect();
            anchor.getGlobalVisibleRect(visibleFrame);
            int height = anchor.getResources().getDisplayMetrics().heightPixels - visibleFrame.bottom;
            pw.setHeight(height);
            pw.showAsDropDown(anchor, xoff, yoff);
        } else {
            pw.showAsDropDown(anchor, xoff, yoff);
        }
    }
    */

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
            if (textToSpeech != null)
                textToSpeech.shutdown();
    }

    /*
    TEMline的数据的init


     */
    //遍历weather存数据
    private void setLineValue(Weather weather) {
        /*
        maxscore=null;
        minscore=null;
        date=null;
        datetop=null;
        */
        for (int i = 0; i < weather.forecastList.size(); i++) {
            maxscore[i] = Integer.parseInt(weather.forecastList.get(i).temperature.max);
            minscore[i] = Integer.parseInt(weather.forecastList.get(i).temperature.min);
            date[i] = weather.forecastList.get(i).date.replace("2018-0","");//去掉2018-
            datetop[i] = weather.forecastList.get(i).more.info;
        }
        Log.d("000000",weather.hourlyList.get(0).time+"666");
        for (int i=0;i<weather.hourlyList.size();i++){
            hourlytem[i]=Integer.parseInt(weather.hourlyList.get(i).tmp);
            hourlytime[i]=weather.hourlyList.get(i).cond_txt+"\n"+ weather.hourlyList.get(i).time.
                    substring(weather.hourlyList.get(i).time.length()-5,weather.hourlyList.get(i).time.length());
            hourlytop[i]=weather.hourlyList.get(i).wind_dir+"\n"+weather.hourlyList.get(i).wind_sc;
        }

    }

    //底部x轴显示
    private void getAxisXLables() {
        for (int i = 0; i < date.length; i++) {
            if (!(date[i]==null))
            mAxisValues.add(new AxisValue(i).setLabel(date[i]));
        }
        HAxisValuesdown.clear();
        for (int i = 0; i < hourlytime.length; i++) {
            if (!(hourlytime[i]==null))
                HAxisValuesdown.add(new AxisValue(i).setLabel(hourlytime[i]));
        }


    }

    //顶部x轴显示
    private void getAxisXtopLables() {
        mAxisValues1.clear();
        for (int i = 0; i < datetop.length; i++) {
            if (!(datetop[i]==null))
            mAxisValues1.add(new AxisValue(i).setLabel(datetop[i]));
        }
        HAxisValuetop.clear();
        for (int i = 0; i < hourlytop.length; i++) {
            if (!(hourlytop[i]==null))
               HAxisValuetop.add(new AxisValue(i).setLabel(hourlytop[i]));
        }

    }

    //图表每一个点的显示
    private void getAxisXPoints() {
        mPointValues.clear();
        mPointValues1.clear();
        HPointValues.clear();
        for (int i = 0; i < maxscore.length; i++) {
            mPointValues.add(new PointValue(i, maxscore[i]));
        }
        for (int i = 0; i < minscore.length; i++) {
            mPointValues1.add(new PointValue(i, minscore[i]));
        }
        for (int i = 0; i < hourlytem.length; i++) {
          HPointValues.add(new PointValue(i, hourlytem[i]));
        }

    }


/*
线条的样式数据加入
 */
        private void initLineChart () {
            Line line = new Line(mPointValues).setColor(Color.parseColor("#FF0000"));//折线的颜色
            Line line1 = new Line(mPointValues1).setColor(Color.parseColor("#1AE61A"));
            Line lineHourly=new Line(HPointValues).setColor(Color.parseColor("#F7F709"));
            List<Line> lines = new ArrayList<Line>();
            List<Line> hlines=new ArrayList<Line>();
            //maxline的样式
            line.setShape(ValueShape.CIRCLE);//折线图上每个数据点的形状  这里是圆形 （有三种 ：ValueShape.SQUARE  ValueShape.CIRCLE  ValueShape.DIAMOND）
            line.setCubic(true);//曲线是否平滑，即是曲线还是折线
            line.setFilled(false);//是否填充曲线的面积
            line.setHasLabels(true);//曲线的数据坐标是否加上备注
           // line.setHasLabelsOnlyForSelected(true);//点击数据坐标提示数据（设置了这个line.setHasLabels(true);就无效）
            line.setHasLines(true);//是否用线显示。如果为false 则没有曲线只有点显示
            line.setHasPoints(true);//是否显示圆点 如果为false 则没有原点只有点显示（每个数据点都是个大的圆点）
            //minline的样式
            line1.setHasLabels(true);//曲线的数据坐标是否加上备注
            line1.setCubic(true);//曲线是否平滑，即是曲线还是折线
            //lineHourly样式
            lineHourly.setHasLabels(true);
            lineHourly.setCubic(true);
            //加线到LinChartData
            lines.add(line);
            lines.add(line1);
            hlines.add(lineHourly);
            LineChartData data = new LineChartData();
            LineChartData Hdata=new LineChartData();
            Hdata.setLines(hlines);
            data.setLines(lines);

            //坐标轴
            Axis axisX = new Axis(); //下面的X轴
            axisX.setHasTiltedLabels(true);  //X坐标轴字体是斜的显示还是直的，true是斜的显示
            axisX.setTextColor(Color.WHITE);  //设置字体颜色
            //axisX.setName("date");  //表格名称
            axisX.setTextSize(16);//设置字体大小
            axisX.setMaxLabelChars(6); //最多几个X轴坐标，意思就是你的缩放让X轴上数据的个数7<=x<=mAxisXValues.length
            axisX.setValues(mAxisValues);  //填充X轴的坐标名称
            data.setAxisXBottom(axisX); //x 轴在底部
            //data.setAxisXTop(axisX);  //x 轴在顶部
            axisX.setHasLines(true); //x 轴分割线

            //hourly的下X轴
            Axis haxisX = new Axis(); //下面的X轴
            haxisX.setHasTiltedLabels(false);  //X坐标轴字体是斜的显示还是直的，true是斜的显示
            haxisX.setTextColor(Color.WHITE);  //设置字体颜色
            //axisX.setName("date");  //表格名称
            haxisX.setTextSize(13);//设置字体大小
            haxisX.setMaxLabelChars(6); //最多几个X轴坐标，意思就是你的缩放让X轴上数据的个数7<=x<=mAxisXValues.length
            haxisX.setValues(HAxisValuesdown);  //填充X轴的坐标名称
            Hdata.setAxisXBottom(haxisX); //x 轴在底部
            haxisX.setHasLines(true); //x 轴分割线

            Axis axisXtop = new Axis(); //上面的X轴
            axisXtop.setHasTiltedLabels(false);  //X坐标轴字体是斜的显示还是直的，true是斜的显示
            axisXtop.setTextColor(Color.WHITE);  //设置字体颜色
            //axisX.setName("date");  //表格名称
            axisXtop.setTextSize(16);//设置字体大小
            axisXtop.setMaxLabelChars(5); //最多几个X轴坐标，意思就是你的缩放让X轴上数据的个数7<=x<=mAxisXValues.length
            Log.d("44444444",mAxisValues1.get(4).toString());
            axisXtop.setValues(mAxisValues1);  //填充X轴的坐标名称
            data.setAxisXTop(axisXtop);  //x 轴在顶部
            axisXtop.setHasLines(true); //x 轴分割线
            //hourly的上X轴
            Axis haxisXtop = new Axis(); //上面的X轴
            haxisXtop.setHasTiltedLabels(false);  //X坐标轴字体是斜的显示还是直的，true是斜的显示
            haxisXtop.setTextColor(Color.WHITE);  //设置字体颜色
            //axisX.setName("date");  //表格名称
            haxisXtop.setTextSize(13);//设置字体大小
            haxisXtop.setMaxLabelChars(5); //最多几个X轴坐标，意思就是你的缩放让X轴上数据的个数7<=x<=mAxisXValues.length
            haxisXtop.setValues(HAxisValuetop);  //填充X轴的坐标名称
           Hdata.setAxisXTop(haxisXtop);  //x 轴在顶部
            haxisXtop.setHasLines(true); //x 轴分割线


            // Y轴是根据数据的大小自动设置Y轴上限(在下面我会给出固定Y轴数据个数的解决方案)
            Axis axisY = new Axis();  //Y轴
            axisY.setName("温度");//y轴标注
            axisY.setTextSize(10);//设置字体大小
            data.setAxisYLeft(axisY);  //Y轴设置在左边
            //data.setAxisYRight(axisY);  //y轴设置在右边

            // hourly Y轴是根据数据的大小自动设置Y轴上限(在下面我会给出固定Y轴数据个数的解决方案)
            Axis haxisY = new Axis();  //Y轴
            haxisY.setName("温度");//y轴标注
            haxisY.setTextSize(13);//设置字体大小
            Hdata.setAxisYLeft(haxisY);  //Y轴设置在左边
            //data.setAxisYRight(axisY);  //y轴设置在右边

            //设置行为属性，支持缩放、滑动以及平移
            lineChart.setInteractive(true);
            lineChart.setZoomType(ZoomType.HORIZONTAL);
            lineChart.setMaxZoom((float) 2);//最大方法比例
            lineChart.setContainerScrollEnabled(true, ContainerScrollType.HORIZONTAL);
            lineChart.setLineChartData(data);
            lineChart.setVisibility(View.VISIBLE);


           lineChartHourly.setInteractive(true);
         lineChartHourly.setZoomType(ZoomType.HORIZONTAL);
          lineChartHourly.setMaxZoom((float) 2);//最大方法比例
            lineChartHourly.setContainerScrollEnabled(true, ContainerScrollType.HORIZONTAL);
            lineChartHourly.setVisibility(View.VISIBLE);
            lineChartHourly.setLineChartData(Hdata);


            //------------
           /* lineSec.setInteractive(true);
            lineSec.setZoomType(ZoomType.HORIZONTAL);
            lineSec.setMaxZoom((float) 2);//最大方法比例
            lineSec.setContainerScrollEnabled(true, ContainerScrollType.HORIZONTAL);
            lineSec.setLineChartData(data);
            lineSec.setVisibility(View.VISIBLE);*/
            /**注：下面的7，10只是代表一个数字去类比而已
             * 当时是为了解决X轴固定数据个数。见（http://forum.xda-developers.com/tools/programming/library-hellocharts-charting-library-t2904456/page2）;
             */
            Viewport v = new Viewport(lineChart.getMaximumViewport());
            Viewport v2=new Viewport(lineChartHourly.getMaximumViewport());
            v.left = 0;
            v.right = 5;
            v2.left=0;
            v2.right=5;
            lineChart.setCurrentViewport(v);
            lineChartHourly.setCurrentViewport(v2);
            /*lineSec.setCurrentViewport(v);*/
        }
        private String  initSpeechVoice(Weather weather){
           String speechvoice;
            String info;
            String sum=weather.forecastList.get(0).more.info;
           if("阵雨".equals(sum)||"小雨".equals(sum)||"雷阵雨".equals(sum)||"中雨".equals(sum)){
               info="，最近天气多雨，出门请随身带伞";
           }else
               if ("阴".equals(sum)){
                   info="，最近天气阴天，出行和户外运动都很舒适";
               }
               else
                   info="，天气晴朗，出门请做好防晒";


           speechvoice="你好，酷天气为你播报天气预报,"+weather.basic.cityName+","+weather.forecastList.get(0).date.replace("2018-0-6-","")
           +"号"+"天气,"+"最高气温"+weather.forecastList.get(0).temperature.max+"度，"+"最低气温"+
                   weather.forecastList.get(0).temperature.min+"度,"+"天气，"+sum+info;
           return speechvoice;

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
