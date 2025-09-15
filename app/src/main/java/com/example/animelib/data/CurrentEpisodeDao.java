package com.example.animelib.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface CurrentEpisodeDao {
    @Query("SELECT * FROM current_episode WHERE animeId = :animeId LIMIT 1")
    CurrentEpisodeEntity getByAnimeId(String animeId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(CurrentEpisodeEntity entity);
}


