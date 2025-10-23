package com.example.animelib.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entity для хранения предпочтений пользователя по выбору плеера и озвучки
 */
@Entity(tableName = "player_preferences")
public class PlayerPreferences {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    /**
     * Тип плеера ("animelib" или "kodik")
     */
    private String player;
    
    /**
     * ID команды озвучки
     */
    private Integer teamId;
    
    /**
     * Предпочитаемое качество видео (например "1080p", "720p")
     */
    private String preferredQuality;
    
    public PlayerPreferences() {
    }
    
    public PlayerPreferences(String player, Integer teamId) {
        this.player = player;
        this.teamId = teamId;
    }
    
    public PlayerPreferences(String player, Integer teamId, String preferredQuality) {
        this.player = player;
        this.teamId = teamId;
        this.preferredQuality = preferredQuality;
    }
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getPlayer() {
        return player;
    }
    
    public void setPlayer(String player) {
        this.player = player;
    }
    
    public Integer getTeamId() {
        return teamId;
    }
    
    public void setTeamId(Integer teamId) {
        this.teamId = teamId;
    }
    
    public String getPreferredQuality() {
        return preferredQuality;
    }
    
    public void setPreferredQuality(String preferredQuality) {
        this.preferredQuality = preferredQuality;
    }
}

