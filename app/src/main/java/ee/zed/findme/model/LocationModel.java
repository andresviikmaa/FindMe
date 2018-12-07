package ee.zed.findme.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class LocationModel {
    public String name;
    public double lat;
    public double lng;
    public double distance = -1;
}
