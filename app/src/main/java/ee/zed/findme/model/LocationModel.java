package ee.zed.findme.model;

import com.google.android.gms.location.Geofence;

import io.nlopez.smartlocation.geofencing.model.GeofenceModel;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class LocationModel {
    public String name;
    public double lat;
    public double lng;
    public double distance = -1;
    public boolean inFence = false;
}
