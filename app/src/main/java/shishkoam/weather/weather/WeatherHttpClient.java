package shishkoam.weather.weather;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by User on 02.02.2017
 */

public class WeatherHttpClient {
    private static final String BASE_URL = "http://api.openweathermap.org/data/2.5/find?lat=%1$.2f&lon=%2$.2f";
    private static final String API ="&APPID=3de74275a393c4e0b8f3c30bdf93c7ae";
    private Set<AsyncTask> tasks = new HashSet<>();

    public void addTask(AsyncTask task) {
        tasks.add(task);
    }

    public void cancelTasks() {
        for (AsyncTask task : tasks) {
            task.cancel(true);
        }
        tasks.clear();
    }

    public String getWeatherData(double lat, double lon) {
        HttpURLConnection con = null;
        InputStream is = null;
        try {
            String url = String.format(BASE_URL, lat, lon) + API;
            con = (HttpURLConnection) (new URL(url)).openConnection();
            con.setRequestMethod("GET");
            con.setDoInput(true);
            con.setDoOutput(false);
            con.connect();

            // Let's read the response
            StringBuffer buffer = new StringBuffer();
            is = con.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line = null;
            while ((line = br.readLine()) != null)
                buffer.append(line).append('\n');
            is.close();
            con.disconnect();
            return buffer.toString();
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (Throwable t) {
            }
            try {
                con.disconnect();
            } catch (Throwable t) {
            }
        }
        return null;

    }

    @Nullable
    public Bitmap getBitmapData(String s) {
        Bitmap bitmap = null;
        HttpURLConnection conn = null;
        BufferedInputStream buf_stream = null;
        try {
            String iUrl = "http://api.openweathermap.org/img/w/" + s + ".png";
            conn = (HttpURLConnection) new URL(iUrl).openConnection();
            conn.connect();
            buf_stream = new BufferedInputStream(conn.getInputStream(), 8192);
            bitmap = BitmapFactory.decodeStream(buf_stream);
            buf_stream.close();
            conn.disconnect();
            buf_stream = null;
            conn = null;
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        } finally {
            if (buf_stream != null)
                try {
                    buf_stream.close();
                } catch (IOException ex) {
                }
            if (conn != null)
                conn.disconnect();
        }
        return bitmap;
    }

}
