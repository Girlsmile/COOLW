package com.example.madking.coolw.moreGson;

public class Basic {
    public String cid;
    public String location;
    public String parent_city;
    public String admin_area;
    public String cnty;
    public String lat;
    public String lon;
    public String tz;
    /*
      "cid": "CN101010100",
                "location": "北京",
                "parent_city": "北京",
                "admin_area": "北京",
                "cnty": "中国",
                "lat": "39.90498734",
                "lon": "116.4052887",
                "tz": "+8.00"
     */

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getParent_city() {
        return parent_city;
    }

    public void setParent_city(String parent_city) {
        this.parent_city = parent_city;
    }

    public String getAdmin_area() {
        return admin_area;
    }

    public void setAdmin_area(String admin_area) {
        this.admin_area = admin_area;
    }
}
