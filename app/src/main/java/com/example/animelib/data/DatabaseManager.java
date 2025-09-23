package com.example.animelib.data;

import android.content.Context;
import android.util.Log;

import com.example.animelib.models.EpisodesListResponse;

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
    
    public DatabaseManager(Context context) {
        this.db = AppDatabase.getDatabase(context.getApplicationContext());
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    // ========== AppSettings операции ==========
    
    /**
     * Получает URL сайта из базы данных
     */
    public String getSiteUrl() {
        try {
            AppSettings settings = db.appSettingsDao().getSettingsSync();
            if (settings != null && settings.getSiteUrl() != null) {
                String url = settings.getSiteUrl();
                // Убираем trailing slash если есть
                if (url.endsWith("/")) {
                    url = url.substring(0, url.length() - 1);
                }
                // Добавляем https:// если нет протокола
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://" + url;
                }
                return url;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get site URL from DB", e);
        }
        // Fallback на дефолтный URL
        return "https://v3.animelib.org";
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
    
    // ========== CurrentEpisode операции ==========
    
    
    /**
     * Закрывает executor при завершении работы
     */
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
