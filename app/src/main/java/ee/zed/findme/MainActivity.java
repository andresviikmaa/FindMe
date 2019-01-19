package ee.zed.findme;

import android.Manifest;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.Geofence;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.function.Consumer;

import ee.zed.findme.model.LocationAdapter;
import ee.zed.findme.model.LocationEntity;
import ee.zed.findme.model.LocationViewModel;
import io.nlopez.smartlocation.OnActivityUpdatedListener;
import io.nlopez.smartlocation.OnGeofencingTransitionListener;
import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.activity.config.ActivityParams;
import io.nlopez.smartlocation.geofencing.model.GeofenceModel;
import io.nlopez.smartlocation.geofencing.utils.TransitionGeofence;
import io.nlopez.smartlocation.location.config.LocationParams;
import io.nlopez.smartlocation.location.providers.LocationGooglePlayServicesProvider;
import lombok.val;

public class MainActivity extends AppCompatActivity implements OnLocationUpdatedListener, OnActivityUpdatedListener, OnGeofencingTransitionListener {

    private List<GeofenceModel> fenceList = new ArrayList<>();
    private static final String TAG = MainActivity.class.getSimpleName();

    private RecyclerView recyclerView;
    private LocationAdapter mAdapter;
    private static final int PERMISSIONS_ACCESS_FINE_LOCATION = 124;
    private static final int PERMISSIONS_WRITE_EXTERNAL_STORAGE = 125;
    private View mLayout;
    private LocationGooglePlayServicesProvider provider;

    private LocationViewModel mLocationViewModel;
    public static final int NEW_LOCATION_ACTIVITY_REQUEST_CODE = 1;
    private File logFile = null;

    private double geofenceRadius = 12.0;
    private double minAccuracy = 12.0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mLayout = findViewById(R.id.main_layout);
        final val that = this;
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               // Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
               //         .setAction("Action", null).show();
                // Intent showMap = new Intent(getApplicationContext(), MapsActivity.class);
                // showMap.putExtra("new", true);
                // startActivity(showMap);

                Intent intent = new Intent(MainActivity.this, NewLocationActivity.class);
                Location lastLocation = SmartLocation.with(that).location().getLastLocation();;
                intent.putExtra("title", "new location");
                intent.putExtra("lat", lastLocation.getLatitude());
                intent.putExtra("lng", lastLocation.getLongitude());

                startActivityForResult(intent, NEW_LOCATION_ACTIVITY_REQUEST_CODE);
            }
        });

        recyclerView = findViewById(R.id.items);

        mAdapter = new LocationAdapter(this);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        geofenceRadius = Double.parseDouble(sharedPrefs.getString("geofence_radius", "12"));
        minAccuracy = Double.parseDouble(sharedPrefs.getString("geofence_radius", "12"));
        mAdapter.setGeofenceRadius(geofenceRadius);
        mAdapter.setMinAccuracy(minAccuracy);
        sharedPrefs.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if(key.equals("geofence_radius")) {
                    geofenceRadius = Double.parseDouble(sharedPreferences.getString(key, "12"));
                    mAdapter.setGeofenceRadius(geofenceRadius);
                }
                if(key.equals("min_accuracy")) {
                    minAccuracy = Double.parseDouble(sharedPreferences.getString(key, "12"));
                    mAdapter.setMinAccuracy(minAccuracy);
                }
            }
        });

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));

        recyclerView.setAdapter(mAdapter);
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                LocationEntity location = mAdapter.getItem(position);
                Intent showMap = new Intent(getApplicationContext(), MapsActivity.class);
                showMap.putExtra("new", false);
                showMap.putExtra("title", location.name);
                showMap.putExtra("lat", location.lat);
                showMap.putExtra("lng", location.lng);
                startActivity(showMap);
            }

            @Override
            public void onLongClick(View view, int position) {
                // TextView textView = view.findViewById(R.id.title);
                // textView.setCursorVisible(true);
                // textView.setFocusableInTouchMode(true);
                // textView.setInputType(InputType.TYPE_CLASS_TEXT);
                // textView.requestFocus(); //to trigger the soft input
                LocationEntity locationEntity = mAdapter.getItem(position);
                mLocationViewModel.delete(locationEntity);
            }
        }));

        mLocationViewModel = ViewModelProviders.of(this).get(LocationViewModel.class);
        mLocationViewModel.getAllLocations().observe(this, new Observer<List<LocationEntity>>() {
            @Override
            public void onChanged(@Nullable final List<LocationEntity> locations) {
                // Update the cached copy of the words in the adapter.
                mAdapter.setLocations(locations);
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            requestWritePermission();
        } else {
            startLogging();
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            requestLocationPermission();
        } else {
            startTracking();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        showLast();


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTracking();
        //SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        //sharedPrefs.unregisterOnSharedPreferenceChangeListener();
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    private void startLogging() {
        if(isExternalStorageWritable()){
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
            logFile = new File(Environment.getExternalStorageDirectory(), timeStamp+ ".log");
            try {
                logFile.createNewFile();
            }  catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
    }

    private void logLocation(Location l){
        if (logFile == null) return;
        try {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(String.format("L;%f;%f;%f;%f;%f;%f;%f;%f;%f", l.getAccuracy(), l.getBearingAccuracyDegrees(),
                    l.getVerticalAccuracyMeters(), l.getSpeedAccuracyMetersPerSecond(),
                    l.getLatitude(), l.getLongitude(), l.getBearing(), l.getAltitude(), l.getSpeed()));
            buf.newLine();
            buf.close();
        } catch (IOException e) {

            Log.e(TAG, e.getMessage(), e);
        }
    }
    private void logActivity(DetectedActivity a){
        if (logFile == null) return;
        try {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(String.format("A;%s;%d", getNameFromType(a), a.getConfidence()));
            buf.newLine();
            buf.close();
        } catch (IOException e) {

            Log.e(TAG, e.getMessage(), e);
        }
    }
    private void logGeofenceTransition(TransitionGeofence t) {
        if (logFile == null) return;
        try {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            val m = t.getGeofenceModel();
            buf.append(String.format("F;%s;%d;%f", m.getRequestId(), t.getTransitionType(), m.getRadius()));
            buf.newLine();
            buf.close();
        } catch (IOException e) {

            Log.e(TAG, e.getMessage(), e);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == NEW_LOCATION_ACTIVITY_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Location lastLocation = SmartLocation.with(this).location().getLastLocation();
                if (lastLocation != null) {
                LocationEntity location = new LocationEntity();
                    location.name = data.getStringExtra(NewLocationActivity.EXTRA_REPLY);
                    location.lat = lastLocation.getLatitude();
                    location.lng =   lastLocation.getLongitude();
                    location.altitude = lastLocation.getAltitude();

                    mLocationViewModel.insert(location);
                } else {
                    Toast.makeText(
                            getApplicationContext(),
                            R.string.location_unavailable,
                            Toast.LENGTH_LONG).show();

                }

            } else {
                Toast.makeText(
                        getApplicationContext(),
                        R.string.empty_not_saved,
                        Toast.LENGTH_LONG).show();
            }
        } else {
            if (provider != null) {
                provider.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    /**
     * Requests the {@link android.Manifest.permission#CAMERA} permission.
     * If an additional rationale should be displayed, the user has to launch the request from
     * a SnackBar that includes additional information.
     */
    private void requestLocationPermission() {
        // Permission has not been granted and must be requested.
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // Display a SnackBar with cda button to request the missing permission.
            Snackbar.make(mLayout, R.string.location_access_required,
                    Snackbar.LENGTH_INDEFINITE).setAction(R.string.ok, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Request the permission
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            PERMISSIONS_ACCESS_FINE_LOCATION);
                }
            }).show();

        } else {
            Snackbar.make(mLayout, R.string.location_unavailable, Snackbar.LENGTH_SHORT).show();
            // Request the permission. The result will be received in onRequestPermissionResult().
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_ACCESS_FINE_LOCATION);
        }
    }
    /**
     * Requests the {@link android.Manifest.permission#CAMERA} permission.
     * If an additional rationale should be displayed, the user has to launch the request from
     * a SnackBar that includes additional information.
     */
    private void requestWritePermission() {
        // Permission has not been granted and must be requested.
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // Display a SnackBar with cda button to request the missing permission.
            Snackbar.make(mLayout, R.string.location_access_required,
                    Snackbar.LENGTH_INDEFINITE).setAction(R.string.ok, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Request the permission
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            PERMISSIONS_WRITE_EXTERNAL_STORAGE);
                }
            }).show();

        } else {
            Snackbar.make(mLayout, R.string.location_unavailable, Snackbar.LENGTH_SHORT).show();
            // Request the permission. The result will be received in onRequestPermissionResult().
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_WRITE_EXTERNAL_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startTracking();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Snackbar.make(mLayout, "Please allow location sensing!", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
                return;
            }
            case PERMISSIONS_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLogging();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Snackbar.make(mLayout, "Please allow writing to sd card!", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    private void startTracking() {
        provider = new LocationGooglePlayServicesProvider();
        provider.setCheckLocationSettings(true);

        val activityParams = (new ActivityParams.Builder()).setInterval(0).build();
        val locationParams = LocationParams.NAVIGATION;

        SmartLocation smartLocation = new SmartLocation.Builder(this).logging(true).build();

        smartLocation.location(provider).config(locationParams).start(this);
        smartLocation.activity().config(activityParams).start(this);

        //SmartLocation.with(getApplicationContext()).location().start(this);
        final val that = this;
        mLocationViewModel.getAllLocations().observe(this, new Observer<List<LocationEntity>>() {
            @Override
            public void onChanged(@Nullable final List<LocationEntity> locations) {
                // Update the cached copy of the words in the adapter.
                locations.forEach(new Consumer<LocationEntity>() {
                    @Override
                    public void accept(LocationEntity location) {
                        GeofenceModel fence = new GeofenceModel.Builder(Integer.toString(location.getId()))
                                .setTransition(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                                .setLatitude(location.lat)
                                .setLongitude(location.lng)
                                .setRadius(12)
                                .build();
                        fenceList.add(fence);
                    }
                });
                SmartLocation.with(that).geofencing().addAll(fenceList).start(that);
            }
        });

    }

    private void stopTracking() {
        SmartLocation.with(this).location().stop();
        SmartLocation.with(this).activity().stop();
        SmartLocation.with(this).geofencing().stop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent showSettings = new Intent(this, SettingsActivity.class);
            startActivity(showSettings);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLocationUpdated(Location location) {
        showLocation(location);
        logLocation(location);
        mAdapter.locationChanged(location);
    }

    @Override
    public void onActivityUpdated(DetectedActivity detectedActivity) {
        showActivity(detectedActivity);
        mAdapter.activityChanged(detectedActivity);

    }

    private void showActivity(DetectedActivity detectedActivity) {
        val activityText = (TextView) findViewById(R.id.current_activity);
        if (detectedActivity != null) {
            activityText.setText(
                    String.format("Activity %s with %d%% confidence",
                            getNameFromType(detectedActivity),
                            detectedActivity.getConfidence())
            );
            logActivity(detectedActivity);
        } else {
            activityText.setText("Null activity");
        }
    }


    private void showGeofence(Geofence geofence, int transitionType) {
        TextView textBox = findViewById(R.id.current_location);
        String text = "Null geofence";
        if (geofence != null) {
            text = "Transition " + getTransitionNameFromType(transitionType) + " for Geofence with id = " + geofence.getRequestId();
        }
        val locations = mLocationViewModel.getAllLocations().getValue();
        if (locations != null) {
            boolean found = false;
            for (val location: locations) {
                if (location.getId() == Integer.parseInt(geofence.getRequestId())) {
                    textBox.setText(location.name + " " + text);
                    found = true;
                }
            }
            if (!found) {
                textBox.setText(text);
            }
        }

    }
    @Override
    public void onGeofenceTransition(TransitionGeofence transitionGeofence) {
        int type = transitionGeofence.getTransitionType();
        // GeofenceModel geofence = transitionGeofence.getGeofenceModel();
        showGeofence(transitionGeofence.getGeofenceModel().toGeofence(), transitionGeofence.getTransitionType());
        mAdapter.geofenceTransition(transitionGeofence);
        logGeofenceTransition(transitionGeofence);

        // locationList.forEach(new Consumer<LocationModel>() {
        //     @Override
        //     public void accept(LocationModel locationModel) {
        //         if (locationModel.name  == title) {
        //             locationModel.inFence = true;
        //         }
        //     }
        // });

    }


    public void showLocation(final Location location) {
        // val extras = location.getExtras();
        // for (val extra : extras.keySet()){
        //     Log.d(TAG, "Extra: " + extra);
        // }
        TextView textBox = findViewById(R.id.current_location);
        textBox.setText(String.format("lat: %1$,.5f, lng:  %2$,.5f, alt: %3$,.2f \nacc v: %4$,.2fm, h: %5$,.2fm, spd: %6$,.2fkmh",
                location.getLatitude(), location.getLongitude(), location.getAltitude(),
                location.getAccuracy(), location.getVerticalAccuracyMeters(), location.getSpeed()));
        //locationList.forEach(new Consumer<LocationModel>() {
        //    @Override
        //    public void accept(LocationModel locationModel) {
        //        val loc = new Location(locationModel.name);
        //        loc.setLatitude(locationModel.lat);
        //        loc.setLongitude(locationModel.lng);
        //        locationModel.distance = location.distanceTo(loc);
        //    }
        //});
//        mAdapter.notifyDataSetChanged();

    }

    private void showLast() {
        TextView textBox = findViewById(R.id.current_location);
        String text = "";
        Location lastLocation = SmartLocation.with(this).location().getLastLocation();
        mAdapter.locationChanged(lastLocation);
        if (lastLocation != null) {
            text =
                    String.format("[last] Lat: %.5f, Lng: %.5f, alt: %.5f",
                            lastLocation.getLatitude(),
                            lastLocation.getLongitude(),
                            lastLocation.getAltitude()
                    )
            ;
        }
        TextView activityText = findViewById(R.id.current_activity);

        DetectedActivity detectedActivity = SmartLocation.with(this).activity().getLastActivity();
        if (detectedActivity != null) {
            text =
                    String.format("[last] Activity %s with %d%% confidence",
                            getNameFromType(detectedActivity),
                            detectedActivity.getConfidence())
            ;
        }
        activityText.setText(text);
    }
    private String getNameFromType(DetectedActivity activityType) {
        switch (activityType.getType()) {
            case DetectedActivity.IN_VEHICLE:
                return "in_vehicle";
            case DetectedActivity.ON_BICYCLE:
                return "on_bicycle";
            case DetectedActivity.ON_FOOT:
                return "on_foot";
            case DetectedActivity.STILL:
                return "still";
            case DetectedActivity.TILTING:
                return "tilting";
            default:
                return "unknown (" + Integer.toString(activityType.getType()) +")";
        }
    }

    private String getTransitionNameFromType(int transitionType) {
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return "enter";
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return "exit";
            default:
                return "dwell";
        }
    }
}
