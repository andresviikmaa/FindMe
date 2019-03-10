package ee.zed.findme.model;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;

import java.util.List;

public class LocationViewModel extends AndroidViewModel {

    private List<LocationModel> mAllLocations;

    public LocationViewModel (Application application, List<LocationModel> locationModelList) {
        super(application);
        mAllLocations = locationModelList;
    }

    public List<LocationModel> getAllLocations() { return mAllLocations; }

}
