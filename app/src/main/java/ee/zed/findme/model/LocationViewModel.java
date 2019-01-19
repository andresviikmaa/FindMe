package ee.zed.findme.model;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;

import java.util.List;

public class LocationViewModel extends AndroidViewModel {
    private LocationRepository mRepository;

    private LiveData<List<LocationEntity>> mAllLocations;

    public LocationViewModel (Application application) {
        super(application);
        mRepository = new LocationRepository(application);
        mAllLocations = mRepository.getAllLocations();
    }

    public LiveData<List<LocationEntity>> getAllLocations() { return mAllLocations; }

    public void insert(LocationEntity locationEntity) {
        mRepository.insert(locationEntity);
    }

    public void delete(LocationEntity locationEntity) {
        mRepository.delete(locationEntity);
    }
}
