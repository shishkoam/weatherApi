package shishkoam.weather;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import shishkoam.weather.places.PlaceArrayAdapter;
import shishkoam.weather.weather.City;
import shishkoam.weather.weather.OpenWeatherParser;
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
    private Geocoder geocoder;
    private CheckBox button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        geocoder = new Geocoder(context, Locale.getDefault());
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
        initializeMap();

        autoCompleteViewPlaces = (AutoCompleteTextView) findViewById(R.id.atv_places);
        initPlacesAutoCompleteView();
        requestLocation();
    }

    private void initPlacesAutoCompleteView() {
        autoCompleteViewPlaces.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final PlaceArrayAdapter.PlaceAutocomplete item = placeArrayAdapter.getItem(position);
                final String placeId = String.valueOf(item.placeId);
                PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                        .getPlaceById(googleApiClient, placeId);
                placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
            }
        });
        placeArrayAdapter = new PlaceArrayAdapter(this, android.R.layout.simple_list_item_1, null, null);
        autoCompleteViewPlaces.setAdapter(placeArrayAdapter);
    }

    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback = new ResultCallback<PlaceBuffer>() {
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
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses.size() > 0) {
                autoCompleteViewPlaces.setText(addresses.get(0).getLocality());
                autoCompleteViewPlaces.dismissDropDown();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processLocation(double lat, double lon, boolean myPosition) {
        statusText.setText(R.string.getting_weather);
        weatherProgressBar.setVisibility(View.VISIBLE);
        JSONSearchTask task = new JSONSearchTask();
        client.cancelTasks();
        client.addTask(task);
        task.execute(lat, lon);
        String locationFormat = getString(R.string.location_format, lat, lon);
        if (marker != null) {
            marker.remove();
        }
        if (myPosition) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(lat, lon)).zoom(12).build();
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        } else {
            marker = googleMap.addMarker(new MarkerOptions().position(new LatLng(lat, lon))
                    .title(locationFormat)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        }
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

    public class JSONSearchTask extends AsyncTask<Double, Void, WeatherClass> {

        double lat, lon;

        protected WeatherClass doInBackground(Double... locationArray) {
            if (locationArray.length < 2) {
                return null;
            }
            lat = locationArray[0];
            lon = locationArray[1];

            String weatherInfo = client.getWeatherData(lat, lon);
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
            return weather;
        }

        protected void onPostExecute(WeatherClass weather) {
            if (weather == null) {
                statusText.setText(R.string.cant_get_weather);
                weatherProgressBar.setVisibility(View.GONE);
                return;
            }
            //get common city name with default locale
            String cityName = validCityName();
            if (cityName != null) {
                weather.setCity(new City(cityName));
            }
//            if (autoCompleteViewPlaces.getText().toString().equals("")) {
//                autoCompleteViewPlaces.setText(weather.getCity().getName());
//                autoCompleteViewPlaces.dismissDropDown();
//            }
            resultText.setText(weather.toString(context));

            //run load icon task
            LoadIconTask iconTask = new LoadIconTask();
            client.addTask(iconTask);
            iconTask.execute(weather.getIcon());

            long time = System.currentTimeMillis();
            Date date = new Date(time);
            SimpleDateFormat format = new SimpleDateFormat("dd.mm.yyyy hh:mm");
            statusText.setText(getString(R.string.last_weather, format.format(date)));
            weatherProgressBar.setVisibility(View.GONE);
        }

        private String validCityName() {
            String cityName = "";
            try {
                List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                cityName = addresses.get(0).getLocality();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return cityName;
        }
    }


    class LoadIconTask extends AsyncTask<String, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(String... resId) {
            return client.getBitmapData(resId[0]);
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            weatherImage.setImageBitmap(result);
        }

    }
}

