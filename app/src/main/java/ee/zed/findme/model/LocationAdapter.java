package ee.zed.findme.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.Geofence;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.Region;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import ee.zed.findme.R;
import io.nlopez.smartlocation.geofencing.utils.TransitionGeofence;
import lombok.val;

public class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.MyViewHolder> {

    List<LocationModel> locationList;
    Location currentLocation = new Location("dummy");
    private final LayoutInflater mInflater;
    private double geofenceRadius = 12.0;
    private double minAccuracy = 12.0;

    public LocationAdapter(Context context) { mInflater = LayoutInflater.from(context); }
    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = mInflater.inflate(R.layout.location_list_row, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder myViewHolder, int i) {

        val locationEntity = locationList.get(i);
        myViewHolder.title.setText(locationEntity.getName());
        myViewHolder.lat_lng.setText(String.format("%1$,.5f, %2$,.5f", locationEntity.getLat(), locationEntity.getLng() ));
        double geoDistance = currentLocation.distanceTo(locationEntity.getLocation());
        double beaconDistance = locationEntity.getBeacon() != null ? locationEntity.getBeacon().getDistance() : 999.0;
        myViewHolder.geo_distance.setText(String.format("%1$,.2f m", geoDistance ));
        myViewHolder.beacon_distance.setText(String.format("%1$,.2f m", beaconDistance ));

        if (geoDistance < locationEntity.getRadius() || beaconDistance < locationEntity.getRadius()){
            if(currentLocation.getAccuracy() < minAccuracy || beaconDistance < locationEntity.getRadius()) {
                myViewHolder.itemView.setBackgroundColor(Color.GREEN);
                if (locationEntity.fenceEnterTime == null) {
                    locationEntity.fenceEnterTime = LocalDateTime.now();
                }
            } else {
                myViewHolder.itemView.setBackgroundColor(Color.YELLOW);
            }
        } else {
            myViewHolder.itemView.setBackgroundColor(Color.TRANSPARENT);
            locationEntity.fenceEnterTime = null;

        }
        if(locationEntity.fenceEnterTime != null){
            val duration =  Duration.between(locationEntity.getFenceEnterTime(), LocalDateTime.now());
            myViewHolder.time.setText(Long.toString(duration.getSeconds()) + " s");
        } else {
            myViewHolder.time.setText("");
        }

    }

    public void setLocations(List<LocationModel> locations){
        locationList = locations;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        if (locationList != null)
            return locationList.size();
        else return 0;
    }
    
    public LocationModel getItem(int position) {
        return locationList.get(position);
    }

    public void locationChanged(Location location) {
        this.currentLocation = location;
        notifyDataSetChanged();
    }


    public void activityChanged(DetectedActivity detectedActivity) {

    }

    public void geofenceTransition(TransitionGeofence transitionGeofence) {
        //val id = Integer.parseInt(transitionGeofence.getGeofenceModel().getRequestId());
        //for (val loc : locationList){
        //    if (id == loc.getId() && transitionGeofence.getTransitionType() == Geofence.GEOFENCE_TRANSITION_ENTER){
        //        loc.fenceEnterTime = LocalDateTime.now();
        //    } else if (loc.fenceEnterTime != null){
        //        loc.fenceEnterTime = null;
        //    }
        //}
    }

    public void setGeofenceRadius(double geofenceRadius) {
        this.geofenceRadius = geofenceRadius;
    }

    public void setMinAccuracy(double minAccuracy) {
        this.minAccuracy = minAccuracy;
    }

    public void beaconsChanged(Collection<Beacon> collection, Region region) {
        for(Beacon beacon: collection){
            for(LocationModel location: locationList) {
                if (location.bt_address.equals(beacon.getBluetoothAddress())) {
                    location.setBeacon(beacon);
                }
            }
        }
        notifyDataSetChanged();
    }


    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView title;
        public TextView lat_lng;
        public TextView geo_distance;
        public TextView beacon_distance;
        public TextView time;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            title =  itemView.findViewById(R.id.title);
            lat_lng =  itemView.findViewById(R.id.lat_lng);
            geo_distance =  itemView.findViewById(R.id.geo_distance);
            beacon_distance =  itemView.findViewById(R.id.beacon_distance);
            time =  itemView.findViewById(R.id.time);
        }
    }
}
