package com.example.animelib.managers;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.example.animelib.api.ApiService;
import com.example.animelib.models.AnimeBookmarkResponse;
import com.example.animelib.models.EpisodesListResponse;
import com.example.animelib.models.EpisodeResponse;

/**
 * Менеджер для управления закладками аниме
 */
public class BookmarkManager {
    private static final String TAG = "BookmarkManager";
    
    private final Context context;
    private final ApiService apiService;
    
    public BookmarkManager(Context context, ApiService apiService) {
        this.context = context;
        this.apiService = apiService;
    }
    
    /**
     * Добавляет закладку для текущего эпизода
     * @param mediaSlug Слаг медиа
     * @param currentPlayer Данные текущего плеера
     * @param currentEpisode Текущий эпизод
     * @param currentPosition Текущая позиция воспроизведения в миллисекундах
     * @param callback Колбэк для обновления UI
     * @param showSuccessToast Показывать ли Toast при успехе
     */
    public void addBookmark(String mediaSlug, EpisodeResponse.PlayerData currentPlayer, 
                           EpisodesListResponse.EpisodeItem currentEpisode, long currentPosition,
                           BookmarkAddCallback callback, boolean showSuccessToast) {
        
        Log.d(TAG, "Adding bookmark for episode: " + currentEpisode.getNumber());
        
        if (currentPlayer == null) {
            showToast("Нет данных о текущем плеере");
            return;
        }

        if (mediaSlug == null || mediaSlug.isEmpty()) {
            showToast("Не удалось извлечь slug аниме");
            return;
        }
        
        // Получаем текущее время воспроизведения
        String timecode = ApiService.formatTimecode(currentPosition);
        
        // Получаем team ID
        int teamId = (currentPlayer.getTeam() != null) ? currentPlayer.getTeam().getId() : 0;
        
        // Получаем номер эпизода как int
        int episodeNumber = 0;
        try {
            episodeNumber = Integer.parseInt(currentEpisode.getNumber());
        } catch (NumberFormatException e) {
            Log.e(TAG, "Failed to parse episode number: " + currentEpisode.getNumber(), e);
        }
        
        Log.d(TAG, "Adding bookmark - mediaSlug: " + mediaSlug +
                   ", episodeId: " + currentEpisode.getId() +
                   ", teamId: " + teamId +
                   ", episodeNumber: " + episodeNumber +
                   ", timecode: " + timecode);
        
        // Добавляем закладку через ApiService
        apiService.addBookmark(
            mediaSlug,
            currentEpisode.getId(),
            teamId,
            episodeNumber,
            timecode,
            new ApiService.BookmarkCallback() {
                @Override
                public void onSuccess(String message) {
                    if (showSuccessToast) {
                        showToast(message);
                    }
                    if (callback != null) {
                        callback.onBookmarkAdded(currentEpisode.getId());
                    }
                }
                
                @Override
                public void onError(String error) {
                    showToast(error); // Всегда показываем ошибки
                    if (callback != null) {
                        callback.onBookmarkError(error);
                    }
                }
            }
        );
    }
    
    /**
     * Получает закладку аниме
     * @param mediaSlug Слаг медиа
     * @param callback Колбэк для результата
     */
    public void fetchAnimeBookmark(String mediaSlug, AnimeBookmarkCallback callback) {
        Log.d(TAG, "Fetching anime bookmark for mediaSlug: " + mediaSlug);
        
        apiService.fetchAnimeBookmark(mediaSlug, new ApiService.AnimeBookmarkCallback() {
            @Override
            public void onBookmarkReceived(AnimeBookmarkResponse response) {
                if (callback != null) {
                    callback.onBookmarkReceived(response);
                }
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Error fetching anime bookmark: " + error);
                if (callback != null) {
                    callback.onError(error);
                }
            }
        });
    }
    
    /**
     * Извлекает media_slug из URL аниме
     * @param animeUrl URL аниме
     * @return media_slug или null если не удалось извлечь
     */
    public String extractMediaSlugFromUrl(String animeUrl) {
        return ApiService.extractMediaSlugFromUrl(animeUrl);
    }
    
    /**
     * Проверяет, есть ли закладка для указанного эпизода
     * @param bookmarkData Данные закладки
     * @param episodeId ID эпизода
     * @return true если есть закладка для этого эпизода
     */
    public boolean hasBookmarkForEpisode(AnimeBookmarkResponse.BookmarkData bookmarkData, int episodeId) {
        return bookmarkData != null && bookmarkData.getItemId() == episodeId;
    }
    
    /**
     * Получает прогресс закладки для указанного эпизода
     * @param bookmarkData Данные закладки
     * @param episodeId ID эпизода
     * @return Прогресс в формате "MM:SS" или null если закладки нет
     */
    public String getBookmarkProgress(AnimeBookmarkResponse.BookmarkData bookmarkData, int episodeId) {
        if (hasBookmarkForEpisode(bookmarkData, episodeId)) {
            return bookmarkData.getProgress();
        }
        return null;
    }
    
    /**
     * Показывает Toast сообщение
     * @param message Сообщение для показа
     */
    private void showToast(String message) {
        if (context instanceof android.app.Activity) {
            ((android.app.Activity) context).runOnUiThread(() -> 
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            );
        } else {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Получает последний эпизод из закладки для указанного аниме
     * @param mediaSlug Слаг медиа
     * @param episodesList Список всех эпизодов аниме
     * @param callback Колбэк для результата
     */
    public void getLastEpisodeFromBookmark(String mediaSlug, java.util.List<EpisodesListResponse.EpisodeItem> episodesList, 
                                         LastEpisodeCallback callback) {
        Log.d(TAG, "Getting last episode from bookmark for mediaSlug: " + mediaSlug);
        
        fetchAnimeBookmark(mediaSlug, new AnimeBookmarkCallback() {
            @Override
            public void onBookmarkReceived(AnimeBookmarkResponse response) {
                if (response != null && response.getData() != null) {
                    AnimeBookmarkResponse.BookmarkData bookmark = response.getData();
                    int bookmarkedEpisodeId = bookmark.getItemId();
                    
                    // Ищем эпизод с таким ID в списке
                    EpisodesListResponse.EpisodeItem foundEpisode = null;
                    for (EpisodesListResponse.EpisodeItem episode : episodesList) {
                        if (episode.getId() == bookmarkedEpisodeId) {
                            foundEpisode = episode;
                            break;
                        }
                    }
                    
                    if (foundEpisode != null) {
                        Log.d(TAG, "Found bookmarked episode: " + foundEpisode.getNumber() + 
                                   ", progress: " + bookmark.getProgress());
                        callback.onLastEpisodeFound(foundEpisode, bookmark.getProgress());
                    } else {
                        Log.d(TAG, "Bookmarked episode not found in episodes list");
                        callback.onNoBookmarkFound();
                    }
                } else {
                    Log.d(TAG, "No bookmark data received");
                    callback.onNoBookmarkFound();
                }
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Error getting bookmark: " + error);
                callback.onNoBookmarkFound();
            }
        });
    }
    
    /**
     * Интерфейс для получения закладки аниме
     */
    public interface AnimeBookmarkCallback {
        void onBookmarkReceived(AnimeBookmarkResponse response);
        void onError(String error);
    }
    
    /**
     * Парсит таймкод в формате "MM:SS" или "HH:MM:SS" в миллисекунды
     * @param timecode Таймкод в формате "12:01" или "1:12:01"
     * @return Миллисекунды или 0 если не удалось распарсить
     */
    public long parseTimecodeToMilliseconds(String timecode) {
        if (timecode == null || timecode.isEmpty()) {
            return 0;
        }
        
        try {
            String[] parts = timecode.split(":");
            if (parts.length == 2) {
                // Формат MM:SS
                int minutes = Integer.parseInt(parts[0]);
                int seconds = Integer.parseInt(parts[1]);
                return (minutes * 60 + seconds) * 1000L;
            } else if (parts.length == 3) {
                // Формат HH:MM:SS
                int hours = Integer.parseInt(parts[0]);
                int minutes = Integer.parseInt(parts[1]);
                int seconds = Integer.parseInt(parts[2]);
                return (hours * 3600 + minutes * 60 + seconds) * 1000L;
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Failed to parse timecode: " + timecode, e);
        }
        
        return 0;
    }
    
    /**
     * Интерфейс для получения последнего эпизода из закладки
     */
    public interface LastEpisodeCallback {
        void onLastEpisodeFound(EpisodesListResponse.EpisodeItem episode, String progress);
        void onNoBookmarkFound();
    }
    
    /**
     * Интерфейс для обновления UI после добавления закладки
     */
    public interface BookmarkAddCallback {
        void onBookmarkAdded(int episodeId);
        void onBookmarkError(String error);
    }
}
