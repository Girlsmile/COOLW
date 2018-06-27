package com.example.madking.coolw.gson;

import com.google.gson.annotations.SerializedName;

public class Now {

    @SerializedName("tmp")
    public String temperature;

    @SerializedName("cond")
    public More more;

    public class More {

        @SerializedName("txt")
        public String info;

    }
    public String pres;
    public  String wind_sc;
    public  String wind_dir;
    /*
    /*cloud": "25",
                "cond_code": "104",
                "cond_txt": "阴",
                "fl": "28",
                "hum": "88",
                "pcpn": "0.0",
                "pres": "1006",
                "tmp": "26",
                "vis": "37",
                "wind_deg": "2",
                "wind_dir": "北风",
                "wind_sc": "2",
                "wind_spd": "10"
                */

}
