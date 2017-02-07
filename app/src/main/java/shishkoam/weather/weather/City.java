package shishkoam.weather.weather;

/**
 * Created by User on 02.02.2017
 */

// helper class, that define the city-object, that has coordinates and id
public class City {
    private String id;
    private String name;
    private String Country;
    private double Latitude;
    private double Longitude;

    public City(String id, double Latitude, double Longitude) {
        this.id = id;
        this.Latitude = Latitude;
        this.Longitude = Longitude;
    }

    public City(String name) {
        this.name = name;
    }

    public double getLatitude() {
        return Latitude;
    }

    public double getLongitude() {
        return Longitude;
    }

    public String getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public double getDistanceTo(double dLatitude, double dLongitude) {
        double distance = (dLatitude - this.Latitude) * (dLatitude - this.Latitude) + (dLongitude - this.Longitude) * (dLongitude - this.Longitude);
        return Math.sqrt(distance);
    }

}
