package com.example.danie.techedgebarcode;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;

import android.support.annotation.NonNull;

import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;


import com.example.danie.techedgebarcode.models.Destination;
import com.example.danie.techedgebarcode.models.Origin;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.location.LocationEnginePriority;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.geometry.LatLng;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;


import org.w3c.dom.Text;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by danie on 1/25/2018.
 */

public class MapLookup extends AppCompatActivity implements LocationEngineListener, PermissionsListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private MapView map;
    private MapboxMap mMap;
    private PermissionsManager permissionsManager;
    private LocationLayerPlugin locationPlugin;
    double lat;
    double lng;
    private LocationEngine locationEngine;
    private Location originLocation;
    private Marker destinationMarker;
    private LatLng originCoord;
    private LatLng destinationCoord;
    private Point originPosition;
    private Point destinationPosition;
    private DirectionsRoute currentRoute;
    private static final String TAG = "DirectionsActivity";
    private NavigationMapRoute navigationMapRoute;
    private PendingIntent mGeofencePendingIntent;
    private Button button;
    private Geofence geofence;
    private GeofencingClient mGeofencingClient;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest locationRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGoogleApiClient = new GoogleApiClient.Builder(this, this, this).addApi(LocationServices.API).build();
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.scanned);
        map = (MapView) findViewById(R.id.mapView);
        Destination destination = (Destination) getIntent().getSerializableExtra("Destination");
        Destination temp = new Destination("bob bay", " steep and brew west", "6656 Odana Rd", "Madison", "WI", " 608-833-6656", "test@temp.com", "53719", "US");
        Origin origin;
        origin = (Origin) getIntent().getSerializableExtra("Origin");
        TextView originName;
        originName = (TextView) findViewById(R.id.companyPickup);
        TextView originNumber = (TextView) findViewById(R.id.pickupAddress);
        TextView originStreet = (TextView) findViewById(R.id.pickupNumber);
        TextView destinationName = (TextView) this.<View>findViewById(R.id.deliveryCompany);
        TextView destinationStreet = (TextView) findViewById(R.id.deliveryStreet);
        TextView destinationNumber = (TextView) findViewById(R.id.deliveryNumber);
        originName.setText(origin.getCompany());
        originStreet.setText(origin.getAddress());
        originNumber.setText(String.format("%s\t%s", origin.getPhone(), origin.getName()));
        destinationName.setText(temp.getCompany());
        destinationStreet.setText(temp.getAddress());
        destinationNumber.setText(String.format("%s %s", temp.getPhone(), temp.getName()));
        //Button sendDriver = (Button) findViewById(R.id.sendDriver);
        mGeofencingClient = LocationServices.getGeofencingClient(this);
       /* sendDriver.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View view) {
               getGeofencePendingIntent();


            }
        });*/
        if (destination == null) {
            Toast.makeText(getApplicationContext(), "Invaild Barcode", Toast.LENGTH_LONG).show();
            finish();

        } else {

            map.onCreate(savedInstanceState);
            map.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(final MapboxMap mapboxMap) {
                    mMap = mapboxMap;
                    enableLocationPlugin();
                    getLatLongFromPlace(origin.getAddress());
                    originCoord = new LatLng(lat, lng);
                    Location Origin = new Location("");
                    getLatLongFromPlace(temp.getAddress());
                    destinationCoord = new LatLng(lat, lng);
                    destinationMarker = mapboxMap.addMarker(new MarkerOptions()
                            .position(destinationCoord)
                    );
                    destinationPosition = Point.fromLngLat(destinationCoord.getLongitude(), destinationCoord.getLatitude());
                    originPosition = Point.fromLngLat(originCoord.getLongitude(), originCoord.getLatitude());
                    getRoute(originPosition, destinationPosition);
                    button = (Button) findViewById(R.id.sendDriver);
                    button.setOnClickListener(new View.OnClickListener() {

                        @SuppressLint("MissingPermission")
                        public void onClick(View v) {
                            Point origin = originPosition;
                            Point destination = destinationPosition;
                            geofence = new Geofence.Builder()
                                    .setRequestId("Destination")
                                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                                    .setCircularRegion(lat, lng, 1000f)
                                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                                    .build();
                            locationRequest = LocationRequest.create()
                                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                                    .setInterval(1000)
                                    .setFastestInterval(1000);
                            /*Intent intent = new Intent(getApplicationContext(), UserService.class);
                            PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 1, intent, 0);*/
                            if (PermissionsManager.areLocationPermissionsGranted(MapLookup.this)) {
                                mGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Log.e("test", "" + isMyServiceRunning(MyService.class));
                                            }
                                        });
                            } else {
                                permissionsManager = new PermissionsManager(MapLookup.this);
                                permissionsManager.requestLocationPermissions(MapLookup.this);
                                mGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Log.e("test", "" + isMyServiceRunning(MyService.class));
                                            }
                                        });
                            }
                            startUserService();

                            // Create a NavigationLauncherOptions object to package everything together
                            NavigationLauncherOptions options = NavigationLauncherOptions.builder()
                                    .origin(origin)
                                    .destination(destination)
                                    .shouldSimulateRoute(true)
                                    .build();

                            //NavigationLauncher.startNavigation(MapLookup.this, options);

                        }
                    });

                }

                ;
            });
        }
    }

    private void startUserService() {
        startService(new Intent(this, UserService.class));
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofence(geofence);
        return builder.build();
    }
    private void getRoute(Point origin, Point destination) {
        NavigationRoute.builder()
                .accessToken(Mapbox.getAccessToken())
                .origin(origin)
                .destination(destination)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        // You can get the generic HTTP info about the response
                        Log.d(TAG, "Response code: " + response.code());
                        if (response.body() == null) {
                            Log.e(TAG, "No routes found, make sure you set the right user and access token.");
                            return;
                        } else if (response.body().routes().size() < 1) {
                            Log.e(TAG, "No routes found");
                            return;
                        }

                        currentRoute = response.body().routes().get(0);

                        // Draw the route on the map
                        if (navigationMapRoute != null) {
                            navigationMapRoute.removeRoute();
                        } else {
                            navigationMapRoute = new NavigationMapRoute(null, map, mMap, R.style.NavigationMapRoute);
                        }
                        navigationMapRoute.addRoute(currentRoute);
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                        Log.e(TAG, "Error: " + throwable.getMessage());
                    }
                });
    }

    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationPlugin() {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            initializeLocationEngine();

            locationPlugin = new LocationLayerPlugin(map, mMap, locationEngine);
            locationPlugin.setLocationLayerEnabled(true);
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @SuppressWarnings( {"MissingPermission"})
    private void initializeLocationEngine() {
        locationEngine = new LocationEngineProvider(this).obtainBestLocationEngineAvailable();
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        locationEngine.activate();


        Location lastLocation = locationEngine.getLastLocation();
        if (lastLocation != null) {
            originLocation = lastLocation;
            setCameraPosition(lastLocation);
        } else {
            locationEngine.addLocationEngineListener(this);
        }
    }

    private void setCameraPosition(Location location) {
         mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(location.getLatitude(), location.getLongitude()), 13));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {

    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocationPlugin();
        } else {
            finish();
        }
    }

    @Override
    @SuppressWarnings( {"MissingPermission"})
    public void onConnected() {
        locationEngine.requestLocationUpdates();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            originLocation = location;
            setCameraPosition(location);
            locationEngine.removeLocationEngineListener(this);
        }
    }

    @Override
    @SuppressWarnings( {"MissingPermission"})
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
        if (locationEngine != null) {
            locationEngine.requestLocationUpdates();
        }
        if (locationPlugin != null) {
            locationPlugin.onStart();
        }
        map.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
        if (locationEngine != null) {
            locationEngine.removeLocationUpdates();
        }
        if (locationPlugin != null) {
            locationPlugin.onStop();
        }
        map.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        map.onDestroy();
        if (locationEngine != null) {
            locationEngine.deactivate();
        }
    }

// gets the lat and lng from a location
    public void getLatLongFromPlace(String place) {
        try {
            Geocoder selected_place_geocoder = new Geocoder(getApplicationContext());
            List<Address> address;

            address = selected_place_geocoder.getFromLocationName(place, 5);

            if (address == null) {
            } else {
                Address location = address.get(0);
                lat = location.getLatitude();
                lng = location.getLongitude();


            }

        } catch (Exception e) {
            e.printStackTrace();


        }

    }



    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
    }


    @Override
    public void onLowMemory() {
        super.onLowMemory();
        map.onLowMemory();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        map.onSaveInstanceState(outState);
    }
    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, MyService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        mGeofencePendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
        return mGeofencePendingIntent;
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}

