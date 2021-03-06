package com.example.madking.coolw;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.madking.coolw.EVENTBUS.MessageEvent;
import com.example.madking.coolw.db.City;
import com.example.madking.coolw.db.County;
import com.example.madking.coolw.db.Province;
import com.example.madking.coolw.util.HttpUtil;
import com.example.madking.coolw.util.Utility;

import org.greenrobot.eventbus.EventBus;
import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {
    private static final String TAG = "ChooseAreaFragment";

    public static final int LEVEL_PROVINCE = 0;

    public static final int LEVEL_CITY = 1;

    public static final int LEVEL_COUNTY = 2;

    private ProgressDialog progressDialog;

    private TextView titleText;

    private Button backButton;

    private ListView listView;

    private ArrayAdapter<String> adapter;

    private List<String> dataList = new ArrayList<>();//listview的显示信息

    /**
     * 省列表
     */
    private List<Province> provinceList;

    /**
     * 市列表
     */
    private List<City> cityList;

    /**
     * 县列表
     */
    private List<County> countyList;

    /**
     * 选中的省份
     */
    private Province selectedProvince;

    /**
     * 选中的城市
     */
    private City selectedCity;

    /**
     * 当前选中的级别
     */
    private int currentLevel;
    public static  String CURRENTWEATHERID;


    @Nullable
    @Override//声名view，并将listview的子项目的inflate完返回listview
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.choose_area,container,false);
        titleText=view.findViewById(R.id.title_text);
        backButton=view.findViewById(R.id.back_button);
        listView=view.findViewById(R.id.list_view);
        adapter=new ArrayAdapter<>(getContext(),android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        queryProvinces();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(currentLevel==LEVEL_PROVINCE){
                    selectedProvince=provinceList.get(position);
                    queryCities();
                }else if(currentLevel==LEVEL_CITY){
                    selectedCity=cityList.get(position);
                    queryCounties();
                }
                //这里我们就只能点击县了，所以到了天气详情的页面,后面加个if来判断是不是这个县，不是刷新信息
                else if(currentLevel==LEVEL_COUNTY){
                    Log.d("fragment","22222222222");
                    String weatherId=countyList.get(position).getWeatherId();
                    CURRENTWEATHERID=weatherId;

                    EventBus.getDefault().postSticky(new MessageEvent(CURRENTWEATHERID));//Eventbus发数据
                    Log.d("fragment",CURRENTWEATHERID);
                    if (getActivity()instanceof MainActivity){

                        Intent intent=new Intent(getActivity(),WeatherActivity.class);
                    intent.putExtra("weather_id",weatherId);
                    startActivity(intent);
                    getActivity().finish();//finish就会关掉当前的Activity，包含关掉了fragment。
                    }else if (getActivity() instanceof WeatherActivity){
                        WeatherActivity weatherActivity= (WeatherActivity) getActivity();
                        weatherActivity.drawerLayout.closeDrawers();
                        weatherActivity.swipeRefresh.setRefreshing(true);
                       weatherActivity.requestWeather(weatherId);
                    }
                }

            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentLevel==LEVEL_COUNTY){
                    queryCities();
                }else if (currentLevel==LEVEL_CITY){
                    queryProvinces();
                }
            }
        });

    }
//查询全国的省，先数据库，再去服务器
    private void queryProvinces() {
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);
        provinceList= DataSupport.findAll(Province.class);
        if(provinceList.size()>0){
            dataList.clear();
            for(Province province:provinceList){
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_PROVINCE;
        }else {
            String address="http://guolin.tech/api/china";
            queryFromServer(address,"province");
        }
    }

    //查询城市 先数据库，再去服务器
    private void queryCities() {
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList=DataSupport.where("provinceid= ?",
                String.valueOf(selectedProvince.getId())).find(City.class);
        if(cityList.size()>0){
            dataList.clear();
            for(City city: cityList){
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_CITY;
        }else {
            int provinceCode=selectedProvince.getProvinceCode();
            String address="http://guolin.tech/api/china/"+provinceCode;//根据当前的provincecode去查找json
            queryFromServer(address,"city");
        }
    }

    //查找县 先数据库，再去服务器
    private void queryCounties() {

        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList=DataSupport.where("cityid=?",
                String.valueOf(selectedCity.getId())).find(County.class);
        if(countyList.size()>0){
            dataList.clear();
            for(County county:countyList){
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel=LEVEL_COUNTY;
        }else {
            int provinceCode=selectedProvince.getProvinceCode();
            int cityCode=selectedCity.getCityCode();
            String address="http://guolin.tech/api/china/"+provinceCode+"/"+cityCode;
            queryFromServer(address,"county");
        }
    }


//根据传入的地址和类型查找数据

    private void queryFromServer(String address,final String type ) {
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                     String responseText=response.body().string();
                     boolean result=false;
                     if("province".equals(type)){

                         result= Utility.handleProvinceResponse(responseText);
                     }else if("city".equals(type)){
                         result=Utility.handleCityResponse(responseText,selectedProvince.getId());
                     }else if("county".equals(type)){
                         result=Utility.handleCountyResponse(responseText,selectedCity.getId());
                     }
                     if(result){
                         getActivity().runOnUiThread(new Runnable() {
                             @Override
                             public void run() {
                                 closeProgressDialog();
                                 Log.d("1111","close");
                                 if("province".equals(type)){
                                     queryProvinces();
                                 }else if ("city".equals(type)){
                                     queryCities();
                                 }else if ("county".equals(type)){
                                     queryCounties();
                                 }
                             }
                         });
                     }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                //通过runonuithread()方法回到主线程处理逻辑
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(),"加载失败，请检查网络连接",Toast.LENGTH_SHORT).show();
                    }
                });

            }
        });
    }

//显示进度条
    private void showProgressDialog() {
        if(progressDialog==null){
            //看看
            progressDialog=new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    //关闭进度条
    private void closeProgressDialog() {
        if(progressDialog!=null){
            progressDialog.dismiss();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
