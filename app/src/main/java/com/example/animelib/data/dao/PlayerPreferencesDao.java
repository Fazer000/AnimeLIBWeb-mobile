package com.example.animelib.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.animelib.data.entity.PlayerPreferences;

/**
 * DAO для работы с предпочтениями плеера и озвучки
 */
@Dao
public interface PlayerPreferencesDao {
    
    /**
     * Вставка или обновление предпочтений (upsert)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(PlayerPreferences preferences);
    
    /**
     * Получение предпочтений синхронно
     */
    @Query("SELECT * FROM player_preferences LIMIT 1")
    PlayerPreferences getPreferencesSync();
    
    /**
     * Получение количества записей (для проверки существования)
     */
    @Query("SELECT COUNT(*) FROM player_preferences")
    int getCount();
}





