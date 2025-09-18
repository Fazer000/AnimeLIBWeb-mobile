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
        try {
            AppSettings settings = db.appSettingsDao().getSettingsSync();
            return settings != null ? settings.getThemeMode() : 0; // Default to system theme
        } catch (Exception e) {
            Log.e(TAG, "Failed to load theme setting", e);
            return 0; // Default to system theme
        }
    }
    
    // ========== CurrentEpisode операции ==========
    
    /**
     * Сохраняет текущий эпизод для аниме
     */
    public void saveCurrentEpisode(String animeId, EpisodesListResponse.EpisodeItem episode) {
        try {
            CurrentEpisodeEntity entity = new CurrentEpisodeEntity(
                    animeId,
                    episode.getId(),
                    episode.getNumber(),
                    System.currentTimeMillis()
            );
            db.currentEpisodeDao().upsert(entity);
            Log.d(TAG, "Saved current episode to DB: animeId=" + animeId + ", episodeId=" + episode.getId() + ", episodeNumber=" + episode.getNumber());
        } catch (Exception e) {
            Log.e(TAG, "DB save error for animeId=" + animeId + ", episode=" + episode.getNumber(), e);
        }
    }
    
    /**
     * Загружает текущий эпизод для аниме
     */
    public EpisodesListResponse.EpisodeItem loadCurrentEpisode(String animeId) {
        try {
            Log.d(TAG, "Loading current episode for animeId: " + animeId);
            CurrentEpisodeEntity entity = db.currentEpisodeDao().getByAnimeId(animeId);
            if (entity == null) {
                Log.d(TAG, "No saved episode found for animeId: " + animeId);
                return null;
            }
            
            Log.d(TAG, "Found saved episode: episodeId=" + entity.episodeId + ", episodeNumber=" + entity.episodeNumber + ", updatedAt=" + entity.updatedAt);
            
            EpisodesListResponse.EpisodeItem item = new EpisodesListResponse.EpisodeItem();
            item.setId(entity.episodeId);
            item.setNumber(entity.episodeNumber);
            return item;
        } catch (Exception e) {
            Log.e(TAG, "DB load error for animeId=" + animeId, e);
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
