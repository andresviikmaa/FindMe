package ee.zed.findme.model;
import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.location.Location;
import android.support.annotation.NonNull;

import java.time.LocalDateTime;
import java.util.Date;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.val;


@Entity(tableName = "location")
@Data
@NoArgsConstructor
public class LocationEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    @NonNull
    @ColumnInfo(name = "name")
    public String name;
    @NonNull
    @ColumnInfo(name = "latitude")
    public double lat;
    @NonNull
    @ColumnInfo(name = "longitude")
    public double lng;
    @NonNull
    @ColumnInfo(name = "altitude")
    public double altitude;
    @Ignore
    private Location location = null;
    @Ignore
    public LocalDateTime fenceEnterTime = null;

    Location getLocation() {
        if (location == null){
            location = new Location(name);
            location.setLatitude(lat);
            location.setLongitude(lng);
        }
        return location;
    }
}
