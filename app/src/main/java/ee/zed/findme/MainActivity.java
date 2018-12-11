package ee.zed.findme;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Movie;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import android.text.InputType;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.Geofence;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import ee.zed.findme.model.LocationModel;
import io.nlopez.smartlocation.OnActivityUpdatedListener;
import io.nlopez.smartlocation.OnGeofencingTransitionListener;
import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.geofencing.model.GeofenceModel;
import io.nlopez.smartlocation.geofencing.utils.TransitionGeofence;
import io.nlopez.smartlocation.location.config.LocationAccuracy;
import io.nlopez.smartlocation.location.config.LocationParams;
import io.nlopez.smartlocation.location.providers.LocationGooglePlayServicesProvider;
import lombok.val;

public class MainActivity extends AppCompatActivity implements OnLocationUpdatedListener, OnActivityUpdatedListener, OnGeofencingTransitionListener {

    private List<LocationModel> locationList = new ArrayList<>();
    private List<GeofenceModel> fenceList = new ArrayList<>();

    private RecyclerView recyclerView;
    private LocationAdapter mAdapter;
    private static final int PERMISSIONS_ACCESS_FINE_LOCATION = 124;
    private View mLayout;
    private LocationGooglePlayServicesProvider provider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mLayout = findViewById(R.id.main_layout);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               // Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
               //         .setAction("Action", null).show();
                Intent showMap = new Intent(getApplicationContext(), MapsActivity.class);
                showMap.putExtra("new", true);
                startActivity(showMap);
            }
        });

        recyclerView = findViewById(R.id.items);

        mAdapter = new LocationAdapter(locationList);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));

        recyclerView.setAdapter(mAdapter);
        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                LocationModel location = locationList.get(position);
                Intent showMap = new Intent(getApplicationContext(), MapsActivity.class);
                showMap.putExtra("new", false);
                showMap.putExtra("title", location.name);
                showMap.putExtra("lat", location.lat);
                showMap.putExtra("lng", location.lng);
                startActivity(showMap);
            }

            @Override
            public void onLongClick(View view, int position) {
                TextView textView = view.findViewById(R.id.title);
                textView.setCursorVisible(true);
                textView.setFocusableInTouchMode(true);
                textView.setInputType(InputType.TYPE_CLASS_TEXT);
                textView.requestFocus(); //to trigger the soft input
            }
        }));

        prepareLocationData();

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

    private void prepareLocationData() {
        LocationModel loc_1 = LocationModel.builder().name("Kaarsild, raekoja pool").lat(58.380665).lng(26.725371).build();
        locationList.add(loc_1);
        LocationModel loc_2 = LocationModel.builder().name("Kaarsild, ülejõe").lat(58.380955).lng(26.726559).build();
        locationList.add(loc_2);
        LocationModel loc_3 = LocationModel.builder().name("Tartu Saksa Kultuuri Instituut").lat(58.378937).lng(26.709121).build();
        locationList.add(loc_3);
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
                            new String[]{Manifest.permission.CAMERA},
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

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (provider != null) {
            provider.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void startTracking() {
        provider = new LocationGooglePlayServicesProvider();
        provider.setCheckLocationSettings(true);

        SmartLocation smartLocation = new SmartLocation.Builder(this).logging(true).build();

        smartLocation.location(provider).config(LocationParams.NAVIGATION).start(this);
        smartLocation.activity().start(this);

        //SmartLocation.with(getApplicationContext()).location().start(this);

        locationList.forEach(new Consumer<LocationModel>() {
            @Override
            public void accept(LocationModel locationModel) {
                GeofenceModel fence = new GeofenceModel.Builder(locationModel.name)
                        .setTransition(Geofence.GEOFENCE_TRANSITION_ENTER)
                        .setLatitude(locationModel.lat)
                        .setLongitude(locationModel.lng)
                        .setRadius(10)
                        .build();
                fenceList.add(fence);
            }
        });

        SmartLocation.with(getApplicationContext()).geofencing().addAll(fenceList).start(this);

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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLocationUpdated(Location location) {
        showLocation(location);
    }

    @Override
    public void onActivityUpdated(DetectedActivity detectedActivity) {
        showActivity(detectedActivity);
    }

    private void showActivity(DetectedActivity detectedActivity) {
        val activityText = (TextView) findViewById(R.id.current_activity);
        if (detectedActivity != null) {
            activityText.setText(
                    String.format("Activity %s with %d%% confidence",
                            getNameFromType(detectedActivity),
                            detectedActivity.getConfidence())
            );
        } else {
            activityText.setText("Null activity");
        }
    }


    @Override
    public void onGeofenceTransition(TransitionGeofence transitionGeofence) {
        int type = transitionGeofence.getTransitionType();
        GeofenceModel model = transitionGeofence.getGeofenceModel();
        final String title = model.getRequestId();
        locationList.forEach(new Consumer<LocationModel>() {
            @Override
            public void accept(LocationModel locationModel) {
                if (locationModel.name  == title) {
                    locationModel.inFence = true;
                }
            }
        });

    }

    public void showLocation(final Location location) {
        TextView textBox = findViewById(R.id.current_location);
        textBox.setText(String.format("%1$,.5f, %2$,.5f\nAltitude:%3$,.2f", location.getLatitude(), location.getLongitude(), location.getAltitude() ));
        locationList.forEach(new Consumer<LocationModel>() {
            @Override
            public void accept(LocationModel locationModel) {
                val loc = new Location(locationModel.name);
                loc.setLatitude(locationModel.lat);
                loc.setLongitude(locationModel.lng);
                locationModel.distance = location.distanceTo(loc);
            }
        });
        mAdapter.notifyDataSetChanged();

    }

    private void showLast() {
        TextView textBox = findViewById(R.id.current_location);
        String text = "";
        Location lastLocation = SmartLocation.with(this).location().getLastLocation();
        if (lastLocation != null) {
            text =
                    String.format("[From Cache] Latitude %.6f, Longitude %.6f",
                            lastLocation.getLatitude(),
                            lastLocation.getLongitude())
            ;
        }
        TextView activityText = findViewById(R.id.current_activity);

        DetectedActivity detectedActivity = SmartLocation.with(this).activity().getLastActivity();
        if (detectedActivity != null) {
            text +=
                    String.format("[From Cache] Activity %s with %d%% confidence",
                            getNameFromType(detectedActivity),
                            detectedActivity.getConfidence())
            ;
        }
        textBox.setText(text);
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
                return "unknown";
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
