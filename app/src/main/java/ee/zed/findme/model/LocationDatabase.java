package ee.zed.findme.model;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

@Database(entities = {LocationEntity.class}, version = 1)
public abstract class LocationDatabase extends RoomDatabase {
    public abstract LocationDao locationDao();

    private static volatile LocationDatabase INSTANCE;

    static LocationDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (LocationDatabase.class) {
                if (INSTANCE == null) {
                    // Create database here
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            LocationDatabase.class, "location_database")
                            .fallbackToDestructiveMigration()
                            .addCallback(sRoomDatabaseCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static LocationDatabase.Callback sRoomDatabaseCallback =
            new LocationDatabase.Callback(){

                @Override
                public void onCreate (@NonNull SupportSQLiteDatabase db){
                    super.onCreate(db);
                    new PopulateDbAsync(INSTANCE).execute();
                }
            };

    /**
     * Populate the database in the background.
     * If you want to start with more words, just add them.
     */
    private static class PopulateDbAsync extends AsyncTask<Void, Void, Void> {

        private final LocationDao mDao;

        PopulateDbAsync(LocationDatabase db) {
            mDao = db.locationDao();
        }

        void addLocationEntity(String name, double lat, double lng, double alt) {
           LocationEntity locationEntity = new LocationEntity();
           locationEntity.name = name;
           locationEntity.lat = lat;
           locationEntity.lng = lng;
           locationEntity.altitude = alt;
           mDao.insert(locationEntity);
        }
        @Override
        protected Void doInBackground(final Void... params) {
            // Start the app with a clean database every time.
            // Not needed if you only populate on creation.
            mDao.deleteAll();
            addLocationEntity("Kaarsild, raekoja pool", 58.380665, 26.725371, 0);
            addLocationEntity("Kaarsild, ülejõe, Atlantise pool", 58.3809, 26.7264, 0);
            addLocationEntity("Kaarsild, ülejõe, Koidula", 58.381, 26.7265, 0);
            addLocationEntity("Tartu Saksa Kultuuri Instituut", 58.378937, 26.709121, 0);
            addLocationEntity("Kodu", 58.37420, 26.71837, 0);
            return null;
        }
    }

}
