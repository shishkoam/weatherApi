package shishkoam.weather.database;

/**
 * Created by User on 07.02.2017
 */

public class DBObject {
    private int id;
    private double lat;
    private double lon;
    private long date;
    private String request = "";

    public DBObject(int id, double lat, double lon, long date, String request) {
        this.id = id;
        this.lat = lat;
        this.lon = lon;
        this.date = date;
        this.request = request;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public long getDate() {
        return date;
    }

    public String getRequest() {
        return request;
    }
}
