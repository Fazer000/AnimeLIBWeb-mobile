package com.example.animelib;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface AppSettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(AppSettings settings);

    @Update
    void update(AppSettings settings);

    @Query("SELECT * FROM app_settings LIMIT 1")
    LiveData<AppSettings> getSettings();

    @Query("SELECT COUNT(*) FROM app_settings")
    int getSettingsCount();
}
