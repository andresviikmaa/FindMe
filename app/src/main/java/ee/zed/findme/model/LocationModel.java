package ee.zed.findme.model;

import android.arch.persistence.room.Ignore;
import android.location.Location;

import com.google.android.gms.location.Geofence;

import org.altbeacon.beacon.Beacon;

import java.time.LocalDateTime;

import io.nlopez.smartlocation.geofencing.model.GeofenceModel;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class LocationModel {
    public String name;
    public double lat;
    public double lng;
    public double radius;
    public String bt_address;
    public String video;

    @Builder.Default
    public double distance = -1;
    @Builder.Default
    public boolean inFence = false;
    @Ignore
    public LocalDateTime fenceEnterTime;

    @Builder.Default
    private Location location = null;
    @Builder.Default
    private Beacon beacon = null;

    public Location getLocation() {
        if (location == null){
            location = new Location(name);
            location.setLatitude(lat);
            location.setLongitude(lng);
        }
        return location;
    }

}

