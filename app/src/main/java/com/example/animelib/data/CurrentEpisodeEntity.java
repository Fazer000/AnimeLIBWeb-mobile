package com.example.animelib.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "current_episode")
public class CurrentEpisodeEntity {
    @PrimaryKey
    @NonNull
    public String animeId;

    public int episodeId;
    public String episodeNumber;
    public long updatedAt;

    public CurrentEpisodeEntity(@NonNull String animeId, int episodeId, String episodeNumber, long updatedAt) {
        this.animeId = animeId;
        this.episodeId = episodeId;
        this.episodeNumber = episodeNumber;
        this.updatedAt = updatedAt;
    }
}


