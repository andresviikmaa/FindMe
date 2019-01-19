package ee.zed.findme.model;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.os.AsyncTask;

import java.util.List;

public class LocationRepository {
    private LocationDao mWordDao;
    private LiveData<List<LocationEntity>> mAllLocations;

    LocationRepository(Application application) {
        LocationDatabase db = LocationDatabase.getDatabase(application);
        mWordDao = db.locationDao();
        mAllLocations = mWordDao.getAllWords();
    }

    LiveData<List<LocationEntity>> getAllLocations() {
        return mAllLocations;
    }


    public void insert (LocationEntity location) {
        new insertAsyncTask(mWordDao).execute(location);
    }

    public void delete(LocationEntity location) {
        new deleteAsyncTask(mWordDao).execute(location);
    }

    private static class insertAsyncTask extends AsyncTask<LocationEntity, Void, Void> {

        private LocationDao mAsyncTaskDao;

        insertAsyncTask(LocationDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final LocationEntity... params) {
            mAsyncTaskDao.insert(params[0]);
            return null;
        }
    }

    private static class deleteAsyncTask extends AsyncTask<LocationEntity, Void, Void> {

        private LocationDao mAsyncTaskDao;

        deleteAsyncTask(LocationDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final LocationEntity... params) {
            mAsyncTaskDao.delete(params[0]);
            return null;
        }
    }
}
