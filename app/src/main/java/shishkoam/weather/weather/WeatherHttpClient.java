package shishkoam.weather.weather;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

/**
 * Created by User on 02.02.2017
 */

public class WeatherHttpClient {
    private static final String BASE_URL = "http://api.openweathermap.org/data/2.5/find?lat=%1$.2f&lon=%2$.2f";
    private static final String API = "&APPID=3de74275a393c4e0b8f3c30bdf93c7ae";

    public String getWeatherData(double lat, double lon) {
        HttpURLConnection con = null;
        InputStream is = null;
        try {
            String url = String.format(Locale.US, BASE_URL, lat, lon) + API;
            con = (HttpURLConnection) (new URL(url)).openConnection();
            con.setRequestMethod("GET");
            con.setDoInput(true);
            con.setDoOutput(false);
            con.connect();

            // Let's read the response
            StringBuilder buffer = new StringBuilder();
            is = con.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = br.readLine()) != null) {
                buffer.append(line).append('\n');
            }
            is.close();
            con.disconnect();
            return buffer.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            safeClose(is);
            if (con != null) {
                con.disconnect();
            }
        }
        return null;

    }

    private void safeClose(InputStream is) {
        if (is == null) {
            return;
        }
        try {
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        } finally {
            safeClose(buf_stream);
            if (conn != null) {
                conn.disconnect();
            }
        }
        return bitmap;
    }

    private void safeClose(BufferedInputStream buf_stream) {
        if (buf_stream == null) {
            return;
        }
        try {
            buf_stream.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }


}
