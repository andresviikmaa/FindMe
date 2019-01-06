package ee.zed.findme;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Looper;
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

import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;


import ee.zed.findme.model.LocationModel;

import io.nlopez.smartlocation.SmartLocation;
import io.nlopez.smartlocation.activity.ActivityStore;
import io.nlopez.smartlocation.geofencing.model.GeofenceModel;
import io.nlopez.smartlocation.geofencing.utils.TransitionGeofence;
import io.nlopez.smartlocation.location.config.LocationParams;
import io.nlopez.smartlocation.location.providers.LocationGooglePlayServicesProvider;
import lombok.val;

public class MainActivity extends AppCompatActivity implements OnCompleteListener<Void>, OnSuccessListener<Location>, LocationListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private List<LocationModel> locationList = new ArrayList<>();
    private List<Geofence> fenceList = new ArrayList<>();

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }


    /**
     * Tracks whether the user requested to add or remove geofences, or to do neither.
     */
    private enum PendingGeofenceTask {
        ADD, REMOVE, NONE
    }

    private RecyclerView recyclerView;
    private LocationAdapter mAdapter;
    private static final int PERMISSIONS_ACCESS_FINE_LOCATION = 124;
    private View mLayout;
    private LocationGooglePlayServicesProvider provider;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private GeofencingClient geofencingClient;

    private PendingGeofenceTask pendingGeofenceTask = PendingGeofenceTask.NONE;
    private PendingIntent geofencePendingIntent = null;;
    private PendingIntent activityPendingIntent = null;;
    private ActivityStore activityStore;


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

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        activityStore = new ActivityStore(this);

        showLast();


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            requestLocationPermission();
        } else {
            startTracking();
        }

    }
    private void prepareLocationData() {
        LocationModel loc_1 = LocationModel.builder().name("Kaarsild, raekoja pool").lat(58.380665).lng(26.725371).build();
        locationList.add(loc_1);
        LocationModel loc_2 = LocationModel.builder().name("Kaarsild, ülejõe").lat(58.380955).lng(26.726559).build();
        locationList.add(loc_2);
        LocationModel loc_3 = LocationModel.builder().name("Tartu Saksa Kultuuri Instituut").lat(58.378937).lng(26.709121).build();
        locationList.add(loc_3);
        LocationModel loc_4 = LocationModel.builder().name("Kodu").lat(58.37420).lng(26.71837).build();
        locationList.add(loc_4);

        populateGeofenceList();
    }

    /**
     * This sample hard codes geofence data. A real app might dynamically create geofences based on
     * the user's location.
     */
    private void populateGeofenceList() {
        for (LocationModel entry : locationList) {

            fenceList.add(new Geofence.Builder()
                    // Set the request ID of the geofence. This is a string to identify this
                    // geofence.
                    .setRequestId(entry.name)

                    // Set the circular region of this geofence.
                    .setCircularRegion(
                            entry.getLat(),
                            entry.getLng(),
                            50
                    )

                    // Set the expiration duration of the geofence. This geofence gets automatically
                    // removed after this period of time.
                    .setExpirationDuration(-1)
                    .setLoiteringDelay(1000)
                    // Set the transition types of interest. Alerts are only generated for these
                    // transition. We track entry and exit transitions in this sample.
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                            Geofence.GEOFENCE_TRANSITION_EXIT | Geofence.GEOFENCE_TRANSITION_DWELL)

                    // Create the geofence.
                    .build());
        }
    }

    @SuppressLint("MissingPermission")
    private void startTracking() {
        final val that = this;
        geofencingClient = LocationServices.getGeofencingClient(that);
        geofencingClient.removeGeofences(getGeofencePendingIntent()).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                geofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                        .addOnCompleteListener(that)
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, e.getMessage());
                            }
                        });
            }
        });

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this);
        final Intent locationIntent = new Intent();
        //PendingIntent locationPendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        final LocationRequest locationRequest = new LocationRequest()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(3000);

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, new LocationCallback(){
            public void onLocationResult(LocationResult locationResult) {
                val location = locationResult.getLastLocation();
                showLocation(location);
            }

        }, null);

        List<ActivityTransition> transitions = new ArrayList<>();

        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.IN_VEHICLE)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build());
/*
        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.WALKING)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                        .build());

        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.WALKING)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build());

        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.STILL)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build());

        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.STILL)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                        .build());
        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.TILTING)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build());

        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.TILTING)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                        .build());
*/
        ActivityTransitionRequest acivityRequest = new ActivityTransitionRequest(transitions);
        ActivityRecognition.getClient(this)
                .requestActivityTransitionUpdates(acivityRequest, getActivityPendingIntent())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(that, "activities onSuccess", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {

                            Toast.makeText(that, "activities added", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.w(TAG, task.getException().getMessage());
                        }

                    }
                });

    }
    /**
     * Builds and returns a GeofencingRequest. Specifies the list of geofences to be monitored.
     * Also specifies how the geofence notifications are initially triggered.
     */
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);

        // Add the geofences to be monitored by geofencing service.
        builder.addGeofences(fenceList);

        // Return a GeofencingRequest.
        return builder.build();
    }

    @Override
    public void onSuccess(Location location) {
        showLocation(location);
    }

    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        geofencePendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return geofencePendingIntent;
    }

    private PendingIntent getActivityPendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (activityPendingIntent != null) {
            return activityPendingIntent;
        }
        Intent intent = new Intent(this, ActivityRecognitionService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        activityPendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return activityPendingIntent;
    }

    private BroadcastReceiver activityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BROADCAST_INTENT_ACTION.equals(intent.getAction()) && intent.hasExtra(DETECTED_ACTIVITY_EXTRA_ID)) {
                logger.d("sending new activity");
                DetectedActivity detectedActivity = intent.getParcelableExtra(DETECTED_ACTIVITY_EXTRA_ID);
                notifyActivity(detectedActivity);
            }
        }
    };

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



    public void showLocation(final Location location) {
        TextView textBox = findViewById(R.id.current_location);
        textBox.setText(String.format("%1$,.5f, %2$,.5f\nAltitude:%3$,.2f, speed:%3$,.2f", location.getLatitude(), location.getLongitude(), location.getAltitude(), location.getSpeed() ));
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
        //Location lastLocation = SmartLocation.with(this).location().getLastLocation();
        //if (lastLocation != null) {
        //    text =
        //            String.format("[From Cache] Latitude %.6f, Longitude %.6f",
        //                    lastLocation.getLatitude(),
        //                    lastLocation.getLongitude())
        //    ;
        //}
        TextView activityText = findViewById(R.id.current_activity);

        DetectedActivity detectedActivity = activityStore.get("GMS_ID");
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

    @Override
    public void onComplete(@NonNull Task<Void> task) {
        if (task.isSuccessful()) {

            Toast.makeText(this, "geofences_added", Toast.LENGTH_SHORT).show();
        } else {
            // Get the status code for the error and log it using a user-friendly message.
            String errorMessage = GeofenceErrorMessages.getErrorString(this, task.getException());
            Log.w(TAG, errorMessage);
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

}
