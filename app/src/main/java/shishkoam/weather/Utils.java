package shishkoam.weather;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import shishkoam.weather.weather.City;

/**
 * Created by User on 02.02.2017
 */

public class Utils {
    private final static String WEATHER_FILE = new File(Environment.getExternalStorageDirectory(), "weather_icon.png").getAbsolutePath();

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

    public static boolean saveWeatherPicture(Bitmap bitmap) {

        File file = new File(WEATHER_FILE);
        OutputStream fOut = null;
        if (file.exists()) {
            file.delete();
        }
        try {
            file.createNewFile();
            fOut = new FileOutputStream(file);
            // save image in png-format with 85% compression.
            bitmap.compress(Bitmap.CompressFormat.PNG, 85, fOut);
        } catch (IOException e) {
            return false;
        } finally {
            if (fOut != null) safeClose(fOut);
        }
        return true;
    }

    public static Bitmap loadWeatherPicture() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeFile(WEATHER_FILE, options);
    }

    private static void safeClose(OutputStream fOut) {
        try {
            fOut.flush();
            fOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
