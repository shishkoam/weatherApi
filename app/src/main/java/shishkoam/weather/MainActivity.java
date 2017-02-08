package shishkoam.weather;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import shishkoam.weather.database.DBHelper;
import shishkoam.weather.database.DBObject;
import shishkoam.weather.places.PlaceArrayAdapter;
import shishkoam.weather.weather.City;
import shishkoam.weather.weather.OpenWeatherParser;
import shishkoam.weather.weather.TaskManager;
import shishkoam.weather.weather.WeatherClass;
import shishkoam.weather.weather.WeatherHttpClient;


/**
 * Created by User on 02.02.2017
 */

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, Const, GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks {

    private TextView resultText;
    private TextView statusText;
    private TextView locationText;
    private ProgressBar weatherProgressBar;
    private ImageView weatherImage;
    private LocationManager locationManager;
    private Context context = this;
    private GoogleMap googleMap;
    private SupportMapFragment mapFragment;
    private Marker marker;
    private WeatherHttpClient client = new WeatherHttpClient();
    private AutoCompleteTextView autoCompleteViewPlaces;
    private PlaceArrayAdapter placeArrayAdapter;
    private GoogleApiClient googleApiClient;
    private CheckBox button;
    private DBHelper dbHelper;
    private double currentLat;
    private double currentLon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        googleApiClient = new GoogleApiClient.Builder(MainActivity.this)
                .addApi(Places.GEO_DATA_API)
                .enableAutoManage(this, GOOGLE_API_CLIENT_ID, this)
                .addConnectionCallbacks(this)
                .build();
        resultText = (TextView) findViewById(R.id.result);
        locationText = (TextView) findViewById(R.id.location);
        statusText = (TextView) findViewById(R.id.status);
        weatherProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        weatherImage = (ImageView) findViewById(R.id.weather_image);
        button = (CheckBox) findViewById(R.id.btnLocationSettings);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                button.setChecked(true);
                turnOnLocation();
            }
        });
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        removeLocationRequest();
        initializeMap();
        autoCompleteViewPlaces = (AutoCompleteTextView) findViewById(R.id.atv_places);
        initPlacesAutoCompleteView();
        dbHelper = new DBHelper(this);
        if (TaskManager.getInstance().hasActiveTasks()) {
            //here we process async tasks that were running before device rotation
            TaskManager.getInstance().linkTasksToNewActivity(this);
            setStatusInProgress();
        } else {
            restoreLastWeatherData();
        }
    }

    //restore last request after application start
    private void restoreLastWeatherData() {
        DBObject object = dbHelper.readFirstData();
        setLocationInUI(object.getLat(), object.getLon());
        processWeatherJson(object.getRequest(), object.getLat(), object.getLon());
        setStatusSuccess(object.getDate());
        Bitmap bitmap = Utils.loadWeatherPicture();
        if (bitmap != null) {
            weatherImage.setImageBitmap(bitmap);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //saving position
        outState.putString(CITY, autoCompleteViewPlaces.getText().toString());
        if (marker != null) {
            outState.putDouble(LAT, currentLat);
            outState.putDouble(LON, currentLon);
            outState.putBoolean(MARKER_VISIBILITY, marker.isVisible());
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        //restore position
        autoCompleteViewPlaces.setText(savedInstanceState.getString(CITY));
        final double lat = savedInstanceState.getDouble(LAT, 0);
        final double lon = savedInstanceState.getDouble(LON, 0);
        if (lat == 0 && lon == 0) {
            return;
        }
        setLocationInUI(lat, lon);
        boolean markerVisibility = savedInstanceState.getBoolean(MARKER_VISIBILITY);
        if (markerVisibility) {
            currentLon = lon;
            currentLat = lat;
        }
    }

    private void initPlacesAutoCompleteView() {
        autoCompleteViewPlaces.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final PlaceArrayAdapter.PlaceAutocomplete item = placeArrayAdapter.getItem(position);
                final String placeId = String.valueOf(item.placeId);
                PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                        .getPlaceById(googleApiClient, placeId);
                placeResult.setResultCallback(updatePlaceDetailsCallback);
            }
        });
        placeArrayAdapter = new PlaceArrayAdapter(this, android.R.layout.simple_list_item_1, null, null);
        autoCompleteViewPlaces.setAdapter(placeArrayAdapter);
    }

    private ResultCallback<PlaceBuffer> updatePlaceDetailsCallback = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                return;
            }
            removeLocationRequest();
            final Place place = places.get(0);
            LatLng latLng = place.getLatLng();
            processLocation(latLng.latitude, latLng.longitude, false);
        }
    };

    @Override
    public void onConnected(Bundle bundle) {
        placeArrayAdapter.setGoogleApiClient(googleApiClient);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this, getString(R.string.places_api_error, connectionResult.getErrorCode()),
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionSuspended(int i) {
        placeArrayAdapter.setGoogleApiClient(null);
    }

    private void initializeMap() {
        if (mapFragment == null) {
            mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        map.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                removeLocationRequest();
                setCityValueToAutoCompleteView(latLng.latitude, latLng.longitude);
                processLocation(latLng.latitude, latLng.longitude, false);
            }
        });

        if (!(currentLat == 0 && currentLon == 0)) {
            LatLng currentPos = new LatLng(currentLat, currentLon);
            marker = googleMap.addMarker(new MarkerOptions().position(currentPos)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        initializeMap();
    }

    private void requestLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: request permission
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES,
                MIN_DISTANCE_CHANGE_FOR_UPDATES, locationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_BW_UPDATES,
                MIN_DISTANCE_CHANGE_FOR_UPDATES, locationListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        removeLocationRequest();
    }

    private void removeLocationRequest() {
        button.setChecked(false);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //if we have not permission we have any location listener - so we can ignore it
            return;
        }
        locationManager.removeUpdates(locationListener);
    }

    private LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            if (location != null) {
                setCityValueToAutoCompleteView(location.getLatitude(), location.getLongitude());
                processLocation(location.getLatitude(), location.getLongitude(), true);
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // no need here to ask permission because it was asked when listener was added to request location
                return;
            }
            Location location = locationManager.getLastKnownLocation(provider);
            if (location != null) {
                setCityValueToAutoCompleteView(location.getLatitude(), location.getLongitude());
                processLocation(location.getLatitude(), location.getLongitude(), true);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }
    };

    private void setCityValueToAutoCompleteView(double lat, double lon) {
        autoCompleteViewPlaces.setText(Utils.getCommonCityName(context, lat, lon));
        autoCompleteViewPlaces.dismissDropDown();
    }

    private void processLocation(final double lat, final double lon, boolean myPosition) {
        setStatusInProgress();
        currentLat = lat;
        currentLon = lon;
        loadWeatherFromApi(lat, lon);
        if (marker != null) {
            marker.remove();
        }
        marker = googleMap.addMarker(new MarkerOptions().position(new LatLng(lat, lon))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        if (myPosition) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(lat, lon)).zoom(12).build();
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            marker.setVisible(false);
        }
        setLocationInUI(lat, lon);
    }

    private void setStatusInProgress() {
        statusText.setText(R.string.getting_weather);
        weatherProgressBar.setVisibility(View.VISIBLE);
    }

    private void processWeatherFromApiCallback(String weatherInfo, double lat, double lon) {
        WeatherClass weather = processWeatherJson(weatherInfo, lat, lon);
        if (weather != null) {
            loadImageFromApi(weather.getIcon());
            long currentTime = System.currentTimeMillis();
            setStatusSuccess(currentTime);
            dbHelper.clearDataBase();
            dbHelper.addData(lat, lon, currentTime, weatherInfo);
        } else {
            processGettingWeatherFailed();
        }
    }

    private void processWeatherImageToUI(Bitmap result) {
        if (result != null) {
            weatherImage.setImageBitmap(result);
            Utils.saveWeatherPicture(result);
        }
        TaskManager.getInstance().clearTasks();
    }

    private void setLocationInUI(double lat, double lon) {
        String locationFormat = getString(R.string.location_format, lat, lon);
        locationText.setText(locationFormat);
    }

    public void turnOnLocation() {
        boolean network_enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        boolean gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!network_enabled && !gps_enabled) {
            startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        }
        requestLocation();
    }

    private WeatherClass processWeatherJson(String weatherInfo, double lat, double lon) {
        if (weatherInfo == null) {
            return null;
        }
        OpenWeatherParser openWeatherParser = new OpenWeatherParser(weatherInfo);
        ArrayList<City> cities = openWeatherParser.parseTheCitiesFromData();
        if (cities.size() == 0) {
            return null;
        }
        String idOfNearestCity = Utils.getTheNearestCity(cities, lat, lon);
        WeatherClass weather = openWeatherParser.parseTheDataWithId(idOfNearestCity);
        if (weather == null) {
            return null;
        }

        //get common city name with default locale
        String cityName = Utils.getCommonCityName(context, lat, lon);
        if (cityName != null) {
            weather.setCity(new City(cityName));
        }
        resultText.setText(getWeatherInfoString(weather));
        return weather;
    }

    private void processGettingWeatherFailed() {
        statusText.setText(R.string.cant_get_weather);
        weatherProgressBar.setVisibility(View.GONE);
    }

    private void setStatusSuccess(long time) {
        Date date = new Date(time);
        SimpleDateFormat format = new SimpleDateFormat("dd.mm.yyyy hh:mm");
        statusText.setText(getString(R.string.last_weather, format.format(date)));
        weatherProgressBar.setVisibility(View.GONE);
    }

    private String getWeatherInfoString(WeatherClass weather) {
        return context.getString(R.string.weather_info_string, weather.getCity().getName(),
                weather.getCondition(), weather.getDescr(), weather.getHumidity(),
                weather.getPressure(), weather.getTemp(), weather.getMinTemp(),
                weather.getMaxTemp(), weather.getWindDeg(), weather.getWindSpeed(),
                weather.getCloudPerc());
    }

    public void loadWeatherFromApi(double lat, double lon) {
        TaskManager.getInstance().cancelTasks();
        LoadWeatherTask weatherTask = new LoadWeatherTask(this);
        TaskManager.getInstance().addTask(weatherTask);
        weatherTask.execute(lat, lon);
    }

    public void loadImageFromApi(String icon) {
        LoadIconTask imageLoadTask = new LoadIconTask(this);
        TaskManager.getInstance().addTask(imageLoadTask);
        imageLoadTask.execute(icon);
    }

    private class LoadWeatherTask extends LinkActivityAsyncTask<Double, Void, String> {

        double lat, lon;

        public LoadWeatherTask(MainActivity activity) {
            super(activity);
        }

        protected String doInBackground(Double... locationArray) {
            if (locationArray.length < 2) {
                return null;
            }
            lat = locationArray[0];
            lon = locationArray[1];
            return client.getWeatherData(lat, lon);
        }

        protected void onPostExecute(String weatherInfo) {
            getActivity().processWeatherFromApiCallback(weatherInfo, lat, lon);
        }
    }

    private class LoadIconTask extends LinkActivityAsyncTask<String, Void, Bitmap> {

        public LoadIconTask(MainActivity activity) {
            super(activity);
        }

        @Override
        protected Bitmap doInBackground(String... resId) {
            return client.getBitmapData(resId[0]);
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            getActivity().processWeatherImageToUI(result);
        }

    }

    public abstract class LinkActivityAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {
        private MainActivity activity;

        public LinkActivityAsyncTask(MainActivity activity) {
            this.activity = activity;
        }

        public void linkActivity(MainActivity activity) {
            this.activity = activity;
        }

        public MainActivity getActivity() {
            return activity;
        }
    }

}

