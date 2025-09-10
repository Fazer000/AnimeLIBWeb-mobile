package com.example.animelib;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "app_settings")
public class AppSettings {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String siteUrl;

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
}
