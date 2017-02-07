package shishkoam.weather.weather;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by User on 02.02.2017
 */

public class OpenWeatherParser implements JsonConst {
    private WeatherClass weather;
    private String data;

    /**
     * Constructor
     *
     * @param data  - json string from api.openweathermap by coordinates request
     */
    public OpenWeatherParser(String data) {
        this.data = data;
    }

    public ArrayList<City> parseTheCitiesFromData() {
        ArrayList<City> cities = new ArrayList<City>();
        try {
            JSONObject jObj = new JSONObject(data);
            JSONArray jArr = jObj.getJSONArray(LIST);
            for (int i = 0; i < jArr.length(); i++) {
                JSONObject jListObj = jArr.getJSONObject(i);
                JSONObject coordObj = getObject(COORDINATES, jListObj);
                City city = new City(getString(ID, jListObj), getFloat(LATITUDE, coordObj), getFloat(LONGITUDE, coordObj));
                cities.add(city);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return cities;
    }

    public WeatherClass parseTheDataWithId(String id) {
        weather = new WeatherClass();
        String thisId;
        try {
            JSONObject jObj = new JSONObject(data);
            JSONArray jArr = jObj.getJSONArray(LIST);
            for (int i = 0; i < jArr.length(); i++) {
                JSONObject jListObj = jArr.getJSONObject(i);
                thisId = getString(ID, jListObj);
                if (id.equals(thisId)) {
                    City weatherCity = new City(getString(NAME, jListObj));
                    weather.setCity(weatherCity);
                    // We get weather info (This is an array)
                    JSONArray jWeatherArr = jListObj.getJSONArray(WEATHER);
                    // We use only the first value
                    JSONObject JSONWeather = jWeatherArr.getJSONObject(0);
                    weather.setWeatherId(getInt(ID, JSONWeather));
                    weather.setDescr(getString(DESCRIPTION, JSONWeather));
                    weather.setCondition(getString(MAIN, JSONWeather));
                    weather.setIcon(getString(ICON, JSONWeather));

                    JSONObject mainObj = getObject(MAIN, jListObj);
                    weather.setHumidity(getInt(HUMIDITY, mainObj));
                    weather.setPressure(getInt(PRESSURE, mainObj));
                    weather.setMaxTemp(getFloat(TEMP_MAX, mainObj));
                    weather.setMinTemp(getFloat(TEMP_MIN, mainObj));
                    weather.setTemp(getFloat(TEMP, mainObj));

                    // Wind
                    JSONObject windObj = getObject(WIND, jListObj);
                    weather.setWindSpeed(getFloat(SPEED, windObj));
                    weather.setWindDeg(getFloat(DEGREES, windObj));

                    // Clouds
                    JSONObject cloudObj = getObject(CLOUDS, jListObj);
                    weather.setCloudPerc(getInt(ALL, cloudObj));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return weather;
    }

    private static JSONObject getObject(String tagName, JSONObject jObj) throws JSONException {
        JSONObject subObj = jObj.getJSONObject(tagName);
        return subObj;
    }

    private static String getString(String tagName, JSONObject jObj) throws JSONException {
        return jObj.getString(tagName);
    }

    private static float getFloat(String tagName, JSONObject jObj) throws JSONException {
        return (float) jObj.getDouble(tagName);
    }

    private static int getInt(String tagName, JSONObject jObj) throws JSONException {
        return jObj.getInt(tagName);
    }
}
