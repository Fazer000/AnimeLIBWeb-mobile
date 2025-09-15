package com.example.animelib.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "app_settings")
public class AppSettings {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String siteUrl;
    private boolean enable4K = false;
    private boolean autoPlay = true;
    private int longSkipDuration = 85; // seconds
    private int themeMode = 0; // 0 = system, 1 = light, 2 = dark

    public AppSettings() {
    }

    public AppSettings(String siteUrl) {
        this.siteUrl = siteUrl;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSiteUrl() {
        return siteUrl;
    }

    public void setSiteUrl(String siteUrl) {
        this.siteUrl = siteUrl;
    }

    public boolean isEnable4K() {
        return enable4K;
    }

    public void setEnable4K(boolean enable4K) {
        this.enable4K = enable4K;
    }

    public boolean isAutoPlay() {
        return autoPlay;
    }

    public void setAutoPlay(boolean autoPlay) {
        this.autoPlay = autoPlay;
    }

    public int getLongSkipDuration() {
        return longSkipDuration;
    }

    public void setLongSkipDuration(int longSkipDuration) {
        this.longSkipDuration = longSkipDuration;
    }

    public int getThemeMode() {
        return themeMode;
    }

    public void setThemeMode(int themeMode) {
        this.themeMode = themeMode;
    }
}
