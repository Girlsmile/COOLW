package com.example.madking.coolw.moreGson;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Mweather {
    public String status;
    public Basic basic;
    @SerializedName("lifestyle")public List<Lifestyle> lifestyleList;
    public Update update;
}
