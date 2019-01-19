package ee.zed.findme.model;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;
@Dao
public interface LocationDao {
    @Insert
    void insert(LocationEntity location);

    @Query("DELETE FROM location")
    void deleteAll();

    @Query("SELECT * from location ORDER BY name ASC")
    LiveData<List<LocationEntity>> getAllWords();

    @Delete
    void delete(LocationEntity location);
}
