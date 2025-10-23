package com.example.animelib.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.animelib.data.dao.TokenDao;
import com.example.animelib.data.dao.PlayerPreferencesDao;
import com.example.animelib.data.entity.TokenEntity;
import com.example.animelib.data.entity.PlayerPreferences;

@Database(entities = {AppSettings.class, TokenEntity.class, PlayerPreferences.class}, version = 12, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;

    public abstract AppSettingsDao appSettingsDao();
    public abstract TokenDao tokenDao();
    public abstract PlayerPreferencesDao playerPreferencesDao();

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "animelib_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
