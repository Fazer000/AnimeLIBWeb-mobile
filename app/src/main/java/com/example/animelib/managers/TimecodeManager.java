package com.example.animelib.managers;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.example.animelib.R;
import com.example.animelib.models.EpisodeResponse;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Менеджер для обработки таймкодов и сегментов видео
 * Показывает одну кнопку для пропуска текущего сегмента
 * - Короткое нажатие: переход к концу сегмента (to)
 * - Кнопка показывается только если текущее время в интервале [from, to]
 */
public class TimecodeManager {
    private static final String TAG = "TimecodeManager";
    
    private final Context context;
    private ExoPlayer player;
    private PlayerView playerView;
    private List<EpisodeResponse.TimecodeData> timecodes = new ArrayList<>();
    private Player.Listener playerListener;
    private Handler updateHandler;
    private Runnable updateRunnable;
    private MaterialButton skipSegmentButton;
    private boolean isControllerVisible = false;
    
    public TimecodeManager(Context context) {
        this.context = context;
        this.updateHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * Инициализация с UI компонентами
     */
    public void initializeViews(ExoPlayer player, PlayerView playerView, MaterialButton skipSegmentButton) {
        this.player = player;
        this.playerView = playerView;
        this.skipSegmentButton = skipSegmentButton;
        
        // Настраиваем обработчик нажатия
        if (skipSegmentButton != null) {
            skipSegmentButton.setOnClickListener(v -> {
                if (player != null) {
                    long currentPositionMs = player.getCurrentPosition();
                    int currentPositionSeconds = (int) (currentPositionMs / 1000);
                    
                    // Ищем активный сегмент
                    for (EpisodeResponse.TimecodeData timecode : timecodes) {
                        if (currentPositionSeconds >= timecode.getFrom() && 
                            currentPositionSeconds <= timecode.getTo()) {
                            long seekPosition = timecode.getTo() * 1000L; // Конвертируем секунды в миллисекунды
                            player.seekTo(seekPosition);
                            Log.d(TAG, "Skipping segment: " + timecode.getType() + " from " + timecode.getFrom() + "s to " + timecode.getTo() + "s at " + seekPosition + "ms");
                            break;
                        }
                    }
                }
            });
        }
        
        // Добавляем слушатель для отслеживания позиции воспроизведения
        if (player != null) {
            playerListener = new Player.Listener() {
                @Override
                public void onPositionDiscontinuity(Player.PositionInfo oldPosition, Player.PositionInfo newPosition, int reason) {
                    updateTimecodeButtonsVisibility();
                }
                
                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    if (isPlaying) {
                        // Запускаем периодическое обновление видимости кнопок
                        startPeriodicUpdate();
                    } else {
                        stopPeriodicUpdate();
                    }
                    // Обновляем видимость кнопки при изменении состояния воспроизведения
                    updateTimecodeButtonsVisibility();
                }
            };
            player.addListener(playerListener);
        }
    }
    
    /**
     * Устанавливает таймкоды из PlayerData
     */
    public void setTimecodes(EpisodeResponse.PlayerData playerData) {
        if (playerData != null && playerData.getTimecode() != null) {
            this.timecodes = playerData.getTimecode();
            Log.d(TAG, "Timecodes set: " + timecodes.size() + " items");
            updateTimecodeButtons();
        } else {
            this.timecodes.clear();
            hideTimecodeButtons();
            Log.d(TAG, "No timecodes found");
        }
    }
    
    /**
     * Обновляет видимость кнопки пропуска сегмента в зависимости от текущей позиции
     */
    private void updateTimecodeButtonsVisibility() {
        if (player == null || timecodes.isEmpty()) {
            return;
        }
        
        long currentPositionMs = player.getCurrentPosition();
        int currentPositionSeconds = (int) (currentPositionMs / 1000);
        
        // Ищем активный сегмент
        EpisodeResponse.TimecodeData activeSegment = null;
        for (EpisodeResponse.TimecodeData timecode : timecodes) {
            if (currentPositionSeconds >= timecode.getFrom() && 
                currentPositionSeconds <= timecode.getTo()) {
                activeSegment = timecode;
                break;
            }
        }
        
        if (activeSegment != null && skipSegmentButton != null) {
            // Обновляем текст кнопки
            String buttonText = getSkipButtonText(activeSegment);
            skipSegmentButton.setText(buttonText);
            
            // Показываем кнопку только если контроллер скрыт (инвертированная логика)
            if (!isControllerVisible) {
                skipSegmentButton.setVisibility(View.VISIBLE);
            } else {
                skipSegmentButton.setVisibility(View.GONE);
            }
        } else {
            // Скрываем кнопку
            if (skipSegmentButton != null) {
                skipSegmentButton.setVisibility(View.GONE);
            }
        }
    }
    
    /**
     * Устанавливает видимость контроллера (вызывается извне)
     */
    public void setControllerVisibility(boolean visible) {
        this.isControllerVisible = visible;
        updateTimecodeButtonsVisibility();
    }
    
    /**
     * Запускает периодическое обновление видимости кнопок
     */
    private void startPeriodicUpdate() {
        stopPeriodicUpdate(); // Останавливаем предыдущий, если есть
        
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateTimecodeButtonsVisibility();
                updateHandler.postDelayed(this, 1000); // Обновляем каждую секунду
            }
        };
        updateHandler.post(updateRunnable);
    }
    
    /**
     * Останавливает периодическое обновление
     */
    private void stopPeriodicUpdate() {
        if (updateRunnable != null) {
            updateHandler.removeCallbacks(updateRunnable);
            updateRunnable = null;
        }
    }
    
    /**
     * Обновляет кнопки таймкодов
     */
    private void updateTimecodeButtons() {
        if (timecodes.isEmpty()) {
            if (skipSegmentButton != null) {
                skipSegmentButton.setVisibility(View.GONE);
            }
            return;
        }
        
        // Кнопка уже создана в разметке, просто скрываем её
        if (skipSegmentButton != null) {
            skipSegmentButton.setVisibility(View.GONE);
        }
        Log.d(TAG, "Skip segment button ready");
    }
    
    /**
     * Возвращает текст для кнопки пропуска сегмента
     */
    private String getSkipButtonText(EpisodeResponse.TimecodeData timecode) {
        String type = timecode.getType();
        
        switch (type.toLowerCase()) {
            case "opening":
                return "Пропустить оппенинг";
            case "ending":
                return "Пропустить эндинг";
            case "splashscreen":
                return "Пропустить заставку";
            case "compilation":
                return "Пропустить компиляцию";
            default:
                return "Пропустить " + type;
        }
    }
    
    /**
     * Форматирует время в формат MM:SS
     */
    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        return String.format("%d:%02d", minutes, remainingSeconds);
    }
    
    /**
     * Скрывает кнопки таймкодов
     */
    public void hideTimecodeButtons() {
        if (skipSegmentButton != null) {
            skipSegmentButton.setVisibility(View.GONE);
        }
    }
    
    /**
     * Показывает кнопки таймкодов
     */
    public void showTimecodeButtons() {
        if (skipSegmentButton != null && !timecodes.isEmpty()) {
            skipSegmentButton.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * Проверяет, есть ли таймкоды
     */
    public boolean hasTimecodes() {
        return !timecodes.isEmpty();
    }
    
    /**
     * Возвращает список таймкодов
     */
    public List<EpisodeResponse.TimecodeData> getTimecodes() {
        return timecodes;
    }
    
    /**
     * Очистка ресурсов
     */
    public void cleanup() {
        stopPeriodicUpdate();
        
        if (player != null && playerListener != null) {
            player.removeListener(playerListener);
        }
        
        if (skipSegmentButton != null) {
            skipSegmentButton.setVisibility(View.GONE);
        }
        
        timecodes.clear();
        player = null;
        playerListener = null;
        updateHandler = null;
        updateRunnable = null;
        skipSegmentButton = null;
        
        Log.d(TAG, "TimecodeManager cleaned up");
    }
}
