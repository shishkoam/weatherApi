package shishkoam.weather.weather;

import android.content.Context;

import shishkoam.weather.R;

/**
 * Created by User on 02.02.2017
 */

public class WeatherClass {
    private int weatherId;
    private String description;
    private String conditionMain;
    private String icon;
    private int humidity;
    private int pressure;
    private float temp_max;
    private float temp_min;
    private float temp;
    private float windSpeed;
    private float windDeg;
    private int cloudPerc;
    private City city;

    /**
     * Setters
     */

    public void setCity(City city) {
        this.city = city;
    }

    public void setWeatherId(int id) {
        this.weatherId = id;
    }

    public void setDescr(String description) {
        this.description = description;
    }

    public void setCondition(String conditionMain) {
        this.conditionMain = conditionMain;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }

    public void setPressure(int pressure) {
        this.pressure = pressure;
    }

    public void setMaxTemp(float temp_max) {
        this.temp_max = temp_max;
    }

    public void setMinTemp(float temp_min) {
        this.temp_min = temp_min;
    }

    public void setTemp(float temp) {
        this.temp = temp;
    }

    public void setWindSpeed(float windSpeed) {
        this.windSpeed = windSpeed;
    }

    public void setWindDeg(float windDeg) {
        this.windDeg = windDeg;
    }

    public void setCloudPerc(int all) {
        this.cloudPerc = all;
    }


    /**
     * Getters
     */

    public City getCity() {
        return city;
    }

    public int getWeatherId() {
        return weatherId;
    }

    public String getDescr() {
        return description;
    }

    public String getCondition() {
        return conditionMain;
    }

    public String getIcon() {
        return icon;
    }

    public int getHumidity() {
        return humidity;
    }

    public int getPressure() {
        return pressure;
    }

    public float getMaxTemp() {
        return temp_max;
    }

    public float getMinTemp() {
        return temp_min;
    }

    public float getTemp() {
        return temp;
    }

    public float getWindSpeed() {
        return windSpeed;
    }

    public float getWindDeg() {
        return windDeg;
    }

    public int getCloudPerc() {
        return cloudPerc;
    }
}
