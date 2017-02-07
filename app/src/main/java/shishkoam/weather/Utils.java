package shishkoam.weather;

import java.util.ArrayList;

import shishkoam.weather.weather.City;

/**
 * Created by User on 02.02.2017.
 */

public class Utils {
    public static String coordToString(double coord) {
        return String.valueOf(Math.round(coord * 100) / 100f);
    }

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

}
