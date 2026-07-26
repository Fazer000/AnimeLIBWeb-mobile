package com.example.animelib.data;

import android.content.Context;
import android.util.Log;

import com.example.animelib.data.entity.TokenEntity;
import com.example.animelib.models.EpisodesListResponse;
import com.example.animelib.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Менеджер для всех операций с базой данных Room
 * Выделен из ApiService для разделения ответственности
 */
public class DatabaseManager {
    private static final String TAG = "DatabaseManager";

    private final AppDatabase db;
    private final ExecutorService executor;
    private final Context context;

    public DatabaseManager(Context context) {
        this.context = context.getApplicationContext();
        this.db = AppDatabase.getDatabase(this.context);
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    // ========== AppSettings операции ==========

    /**
     * Получает URL сайта из базы данных. Вызывать вне главного потока
     */
    public String getSiteUrl() {
        try {
            AppSettings settings = db.appSettingsDao().getSettingsSync();
            if (settings != null && settings.getSiteUrl() != null) {
                String url = settings.getSiteUrl();
                if (url.endsWith("/")) {
                    url = url.substring(0, url.length() - 1);
                }
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://" + url;
                }
                return url;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get site URL from DB", e);
        }
        return "https://" + context.getString(R.string.site_url);
    }
    
    /**
     * Сохраняет настройку 4K
     */
    public void save4KSetting(boolean enable4K) {
        executor.execute(() -> {
            try {
                AppSettings settings = db.appSettingsDao().getSettingsSync();
                if (settings == null) {
                    settings = new AppSettings();
                }
                settings.setEnable4K(enable4K);
                db.appSettingsDao().upsert(settings);
                Log.d(TAG, "Saved 4K setting: " + enable4K);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save 4K setting", e);
            }
        });
    }
    
    /**
     * Загружает настройку 4K
     */
    public boolean load4KSetting() {
        try {
            AppSettings settings = db.appSettingsDao().getSettingsSync();
            return settings != null && settings.isEnable4K();
        } catch (Exception e) {
            Log.e(TAG, "Failed to load 4K setting", e);
            return false;
        }
    }
    
    /**
     * Сохраняет настройку ambient light
     */
    public void saveAmbientLightSetting(boolean enableAmbientLight) {
        executor.execute(() -> {
            try {
                AppSettings settings = db.appSettingsDao().getSettingsSync();
                if (settings == null) {
                    settings = new AppSettings();
                }
                settings.setEnableAmbientLight(enableAmbientLight);
                db.appSettingsDao().upsert(settings);
                Log.d(TAG, "Saved ambient light setting: " + enableAmbientLight);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save ambient light setting", e);
            }
        });
    }
    
    /**
     * Загружает настройку ambient light
     */
    public boolean loadAmbientLightSetting() {
        try {
            AppSettings settings = db.appSettingsDao().getSettingsSync();
            return settings != null && settings.isEnableAmbientLight();
        } catch (Exception e) {
            Log.e(TAG, "Failed to load ambient light setting", e);
            return false;
        }
    }
    
    /**
     * Сохраняет настройку автовоспроизведения
     */
    public void saveAutoPlaySetting(boolean autoPlay) {
        executor.execute(() -> {
            try {
                AppSettings settings = db.appSettingsDao().getSettingsSync();
                if (settings == null) {
                    settings = new AppSettings();
                }
                settings.setAutoPlay(autoPlay);
                db.appSettingsDao().upsert(settings);
                Log.d(TAG, "Saved autoPlay setting: " + autoPlay);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save autoPlay setting", e);
            }
        });
    }
    
    /**
     * Загружает настройку автовоспроизведения
     */
    public boolean loadAutoPlaySetting() {
        try {
            AppSettings settings = db.appSettingsDao().getSettingsSync();
            return settings != null && settings.isAutoPlay();
        } catch (Exception e) {
            Log.e(TAG, "Failed to load autoPlay setting", e);
            return true; // Default to true
        }
    }
    
    /**
     * Сохраняет настройку длительности длинного пропуска
     */
    public void saveLongSkipDurationSetting(int duration) {
        executor.execute(() -> {
            try {
                AppSettings settings = db.appSettingsDao().getSettingsSync();
                if (settings == null) {
                    settings = new AppSettings();
                }
                settings.setLongSkipDuration(duration);
                db.appSettingsDao().upsert(settings);
                Log.d(TAG, "Saved longSkipDuration setting: " + duration);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save longSkipDuration setting", e);
            }
        });
    }
    
    /**
     * Загружает настройку длительности длинного пропуска
     */
    public int loadLongSkipDurationSetting() {
        try {
            AppSettings settings = db.appSettingsDao().getSettingsSync();
            return settings != null ? settings.getLongSkipDuration() : 85; // Default to 85 seconds
        } catch (Exception e) {
            Log.e(TAG, "Failed to load longSkipDuration setting", e);
            return 85; // Default to 85 seconds
        }
    }
    
    /**
     * Сохраняет настройку темы
     */
    public void saveThemeSetting(int themeMode) {
        executor.execute(() -> {
            try {
                AppSettings settings = db.appSettingsDao().getSettingsSync();
                if (settings == null) {
                    settings = new AppSettings();
                }
                settings.setThemeMode(themeMode);
                db.appSettingsDao().upsert(settings);
                Log.d(TAG, "Saved theme setting: " + themeMode);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save theme setting", e);
            }
        });
    }
    
    /**
     * Загружает настройку темы
     */
    public int loadThemeSetting() {

        AppSettings settings = db.appSettingsDao().getSettingsSync();
        return settings != null ? settings.getThemeMode() : 0; // Default to light theme (0)
    }
    
    // ========== Token операции ==========
    
    /**
     * Сохраняет токен в базу данных
     */
    public void saveToken(TokenEntity token) {
        executor.execute(() -> {
            try {
                db.tokenDao().insertOrUpdateToken(token);
                Log.d(TAG, "Saved token to database successfully");
            } catch (Exception e) {
                Log.e(TAG, "Failed to save token", e);
            }
        });
    }
    
    /**
     * Получает токен из базы данных
     */
    public TokenEntity getToken() {
        try {
            return db.tokenDao().getToken();
        } catch (Exception e) {
            Log.e(TAG, "Failed to get token from database", e);
            return null;
        }
    }
    
    /**
     * Проверяет есть ли токен в базе данных
     */
    public boolean hasToken() {
        try {
            return db.tokenDao().getTokenCount() > 0;
        } catch (Exception e) {
            Log.e(TAG, "Failed to check token existence", e);
            return false;
        }
    }
    
    /**
     * Удаляет токен из базы данных
     */
    public void deleteToken() {
        executor.execute(() -> {
            try {
                db.tokenDao().deleteToken();
                Log.d(TAG, "Deleted token from database");
            } catch (Exception e) {
                Log.e(TAG, "Failed to delete token", e);
            }
        });
    }
    
    // ========== CurrentEpisode операции ==========
    
    // ========== PlayerPreferences операции ==========
    
    /**
     * Сохраняет предпочтения по выбору плеера и озвучки
     */
    public void savePlayerPreferences(String player, Integer teamId) {
        executor.execute(() -> {
            try {
                // Загружаем существующую запись или создаем новую
                com.example.animelib.data.entity.PlayerPreferences preferences = 
                    db.playerPreferencesDao().getPreferencesSync();
                
                if (preferences == null) {
                    preferences = new com.example.animelib.data.entity.PlayerPreferences();
                }
                
                // Обновляем данные
                preferences.setPlayer(player);
                preferences.setTeamId(teamId);
                
                db.playerPreferencesDao().upsert(preferences);
                Log.d(TAG, "Saved player preferences: player=" + player + ", teamId=" + teamId);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save player preferences", e);
            }
        });
    }
    
    /**
     * Сохраняет предпочтения по выбору плеера, озвучки и качества
     */
    public void savePlayerPreferences(String player, Integer teamId, String preferredQuality) {
        executor.execute(() -> {
            try {
                // Загружаем существующую запись или создаем новую
                com.example.animelib.data.entity.PlayerPreferences preferences = 
                    db.playerPreferencesDao().getPreferencesSync();
                
                if (preferences == null) {
                    preferences = new com.example.animelib.data.entity.PlayerPreferences();
                }
                
                // Обновляем данные
                preferences.setPlayer(player);
                preferences.setTeamId(teamId);
                preferences.setPreferredQuality(preferredQuality);
                
                db.playerPreferencesDao().upsert(preferences);
                Log.d(TAG, "Saved player preferences: player=" + player + ", teamId=" + teamId + 
                      ", quality=" + preferredQuality);
            } catch (Exception e) {
                Log.e(TAG, "Failed to save player preferences", e);
            }
        });
    }
    
    /**
     * Загружает предпочтения по выбору плеера и озвучки
     */
    public com.example.animelib.data.entity.PlayerPreferences loadPlayerPreferences() {
        try {
            com.example.animelib.data.entity.PlayerPreferences prefs = db.playerPreferencesDao().getPreferencesSync();
            if (prefs != null) {
                Log.d(TAG, "Loaded player preferences: player=" + prefs.getPlayer() + ", teamId=" + prefs.getTeamId());
            } else {
                Log.d(TAG, "No player preferences found in database");
            }
            return prefs;
        } catch (Exception e) {
            Log.e(TAG, "Failed to load player preferences", e);
            return null;
        }
    }
    
    /**
     * Закрывает executor при завершении работы
     */
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
