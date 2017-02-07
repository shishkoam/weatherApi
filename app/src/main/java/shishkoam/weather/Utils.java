package shishkoam.weather;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import shishkoam.weather.weather.City;

/**
 * Created by User on 02.02.2017.
 */

public class Utils {

    public static String getTheNearestCity(ArrayList<City> cities, double longitude, double latitude) {
        String id = cities.get(0).getId();
        double minDistance = cities.get(0).getDistanceTo(latitude, longitude);
        for (int i = 1; i < cities.size(); i++) {
            double newDistance = cities.get(i).getDistanceTo(latitude, longitude);
            if (newDistance < minDistance) {
                minDistance = newDistance;
                id = cities.get(i).getId();
            }
        }
        return id;
    }

    public static String getCommonCityName(Context context, double lat, double lon) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses.size() > 0) {
                return addresses.get(0).getLocality();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
