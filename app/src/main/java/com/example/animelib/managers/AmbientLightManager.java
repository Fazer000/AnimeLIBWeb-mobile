package com.example.animelib.managers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.PixelCopy;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.example.animelib.ui.AmbientLightView;

/**
 * Менеджер для управления ambient-подсветкой видеоплеера.
 * Анализирует цвета с краев видео и создает эффект свечения вокруг плеера.
 */
public class AmbientLightManager {
    private static final String TAG = "AmbientLight";
    
    // Интервал обновления ambient эффекта (мс) - увеличен для производительности
    private static final long UPDATE_INTERVAL = 500;
    
    // Размер сэмпла для анализа цветов (меньше = быстрее)
    private static final int SAMPLE_SIZE = 40;
    
    // Количество источников света на каждой стороне - только боковые
    private static final int LIGHTS_PER_SIDE = 3;
    
    private final Context context;
    private final PlayerView playerView;
    private final AmbientLightView ambientView;
    private final Handler handler;
    private final Runnable updateRunnable;
    
    private ExoPlayer player;
    private boolean isEnabled = false;
    private boolean isRunning = false;
    private boolean isPaused = false;
    private boolean isSuspended = false; // Временная приостановка во время UI interactions
    
    // Массивы цветов для каждой стороны (несколько источников света)
    private final int[] leftColors = new int[LIGHTS_PER_SIDE];
    private final int[] topColors = new int[LIGHTS_PER_SIDE];
    private final int[] rightColors = new int[LIGHTS_PER_SIDE];
    private final int[] bottomColors = new int[LIGHTS_PER_SIDE];
    
    // Player listener для отслеживания play/pause
    private Player.Listener playerListener;

    public AmbientLightManager(@NonNull Context context, 
                               @NonNull PlayerView playerView, 
                               @NonNull AmbientLightView ambientView) {
        this.context = context;
        this.playerView = playerView;
        this.ambientView = ambientView;
        this.handler = new Handler(Looper.getMainLooper());
        
        this.updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRunning && isEnabled) {
                    updateAmbientColors();
                    handler.postDelayed(this, UPDATE_INTERVAL);
                }
            }
        };
    }

    /**
     * Устанавливает ExoPlayer для захвата кадров
     */
    public void setPlayer(ExoPlayer player) {
        // Удаляем старый listener если был
        if (this.player != null && playerListener != null) {
            this.player.removeListener(playerListener);
        }
        
        this.player = player;
        
        // Добавляем listener для отслеживания play/pause
        if (player != null) {
            playerListener = new Player.Listener() {
                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    updatePauseState(!isPlaying);
                }
            };
            player.addListener(playerListener);
        }
        
        // Если ambient light уже был включен до установки player, стартуем его сейчас
        if (isEnabled && player != null && !isRunning) {
            start();
            Log.d(TAG, "Started ambient light after player set");
        }
    }

    /**
     * Включает/выключает ambient эффект
     */
    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
        
        if (enabled) {
            ambientView.setVisibility(View.VISIBLE);
            start();
        } else {
            ambientView.setVisibility(View.GONE);
            stop();
        }
        
        Log.d(TAG, "Ambient light " + (enabled ? "enabled" : "disabled"));
    }

    /**
     * Проверяет, включен ли ambient эффект
     */
    public boolean isEnabled() {
        return isEnabled;
    }

    /**
     * Запускает обновление ambient эффекта
     */
    private void start() {
        if (!isRunning && player != null) {
            isRunning = true;
            handler.post(updateRunnable);
            Log.d(TAG, "Ambient light started");
        }
    }

    /**
     * Останавливает обновление ambient эффекта
     */
    private void stop() {
        isRunning = false;
        handler.removeCallbacks(updateRunnable);
        Log.d(TAG, "Ambient light stopped");
    }

    /**
     * Обновляет цвета ambient эффекта на основе текущего кадра
     */
    private void updateAmbientColors() {
        // Не обновляем если видео на паузе, приостановлен или идет UI interaction
        if (player == null || !player.isPlaying() || isPaused || isSuspended) {
            return;
        }

        try {
            // Находим SurfaceView внутри PlayerView
            SurfaceView surfaceView = findSurfaceView(playerView);
            if (surfaceView == null) {
                Log.w(TAG, "SurfaceView not found");
                return;
            }

            // Создаем bitmap для захвата кадра - используем меньший размер
            Bitmap bitmap = Bitmap.createBitmap(SAMPLE_SIZE, SAMPLE_SIZE, Bitmap.Config.RGB_565); // RGB_565 вместо ARGB_8888
            
            // Захватываем кадр с SurfaceView (асинхронно)
            PixelCopy.request(surfaceView, bitmap, result -> {
                if (result == PixelCopy.SUCCESS) {
                    // Обрабатываем в фоновом потоке
                    new Thread(() -> {
                        extractColorsFromBitmap(bitmap);
                        updateAmbientView();
                        bitmap.recycle();
                    }).start();
                } else {
                    bitmap.recycle();
                }
            }, handler);
            
        } catch (Exception e) {
            Log.e(TAG, "Error updating ambient colors", e);
        }
    }

    /**
     * Извлекает доминирующие цвета с краев bitmap
     * Берет несколько точек вдоль каждого края для создания множественных источников света
     */
    private void extractColorsFromBitmap(@NonNull Bitmap bitmap) {
        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            
            // Ширина/высота одного сегмента для анализа
            int segmentWidth = width / LIGHTS_PER_SIDE;
            int segmentHeight = height / LIGHTS_PER_SIDE;
            int edgeThickness = Math.max(2, Math.min(width, height) / 20); // 5% от размера
            
            // Левый край - несколько точек сверху вниз
            for (int i = 0; i < LIGHTS_PER_SIDE; i++) {
                int y = i * segmentHeight;
                int segHeight = Math.min(segmentHeight, height - y);
                Bitmap segment = Bitmap.createBitmap(bitmap, 0, y, edgeThickness, segHeight);
                leftColors[i] = getDominantColor(segment);
                segment.recycle();
            }
            
            // Верхний край - несколько точек слева направо
            for (int i = 0; i < LIGHTS_PER_SIDE; i++) {
                int x = i * segmentWidth;
                int segWidth = Math.min(segmentWidth, width - x);
                Bitmap segment = Bitmap.createBitmap(bitmap, x, 0, segWidth, edgeThickness);
                topColors[i] = getDominantColor(segment);
                segment.recycle();
            }
            
            // Правый край - несколько точек сверху вниз
            for (int i = 0; i < LIGHTS_PER_SIDE; i++) {
                int y = i * segmentHeight;
                int segHeight = Math.min(segmentHeight, height - y);
                Bitmap segment = Bitmap.createBitmap(bitmap, width - edgeThickness, y, edgeThickness, segHeight);
                rightColors[i] = getDominantColor(segment);
                segment.recycle();
            }
            
            // Нижний край - несколько точек слева направо
            for (int i = 0; i < LIGHTS_PER_SIDE; i++) {
                int x = i * segmentWidth;
                int segWidth = Math.min(segmentWidth, width - x);
                Bitmap segment = Bitmap.createBitmap(bitmap, x, height - edgeThickness, segWidth, edgeThickness);
                bottomColors[i] = getDominantColor(segment);
                segment.recycle();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting colors", e);
        }
    }

    /**
     * Получает доминирующий цвет из bitmap используя простое усреднение (быстрее Palette API)
     */
    private int getDominantColor(@NonNull Bitmap bitmap) {
        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            
            if (width == 0 || height == 0) {
                return 0xFF000000;
            }
            
            // Простое усреднение цветов - намного быстрее Palette API
            long redSum = 0, greenSum = 0, blueSum = 0;
            int pixelCount = 0;
            
            // Берем каждый 2-й пиксель для скорости
            for (int y = 0; y < height; y += 2) {
                for (int x = 0; x < width; x += 2) {
                    int pixel = bitmap.getPixel(x, y);
                    redSum += Color.red(pixel);
                    greenSum += Color.green(pixel);
                    blueSum += Color.blue(pixel);
                    pixelCount++;
                }
            }
            
            if (pixelCount == 0) {
                return 0xFF000000;
            }
            
            int avgRed = (int) (redSum / pixelCount);
            int avgGreen = (int) (greenSum / pixelCount);
            int avgBlue = (int) (blueSum / pixelCount);
            
            return Color.rgb(avgRed, avgGreen, avgBlue);
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting dominant color", e);
            return 0xFF000000;
        }
    }

    /**
     * Обновляет ambient view с новыми цветами (выполняется в UI потоке)
     */
    private void updateAmbientView() {
        // Проверяем что view еще жив
        if (ambientView.getHandler() != null) {
            handler.post(() -> {
                try {
                    ambientView.setColors(leftColors, topColors, rightColors, bottomColors);
                } catch (Exception e) {
                    Log.e(TAG, "Error updating ambient view", e);
                }
            });
        }
    }

    /**
     * Находит SurfaceView внутри ViewGroup
     */
    private SurfaceView findSurfaceView(ViewGroup viewGroup) {
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);
            
            if (child instanceof SurfaceView) {
                return (SurfaceView) child;
            } else if (child instanceof ViewGroup) {
                SurfaceView result = findSurfaceView((ViewGroup) child);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * Обновляет состояние паузы и изменяет интенсивность подсветки
     */
    private void updatePauseState(boolean paused) {
        if (isPaused == paused) {
            return; // Состояние не изменилось
        }
        
        isPaused = paused;
        
        // Плавно изменяем интенсивность подсветки
        if (paused) {
            ambientView.dimToIntensity(0.3f); // Затухаем до 30%
            Log.d(TAG, "Video paused - dimming ambient light");
        } else {
            ambientView.dimToIntensity(1.0f); // Возвращаем к 100%
            Log.d(TAG, "Video playing - restoring ambient light");
        }
    }
    
    /**
     * Временно приостанавливает ambient подсветку (для UI interactions)
     * Вызывается при drag событиях, открытии bottom sheets и т.д.
     * Останавливает обновления и анимации, но оставляет текущую подсветку видимой
     */
    public void suspend() {
        if (!isSuspended) {
            isSuspended = true;
            // Останавливаем анимации в view (замораживаем текущее состояние)
            if (ambientView != null) {
                ambientView.pauseAnimations();
            }
            Log.d(TAG, "Ambient light suspended (animations paused)");
        }
    }
    
    /**
     * Возобновляет ambient подсветку после UI interaction
     */
    public void resume() {
        if (isSuspended) {
            isSuspended = false;
            // Возобновляем анимации
            if (ambientView != null) {
                ambientView.resumeAnimations();
            }
            Log.d(TAG, "Ambient light resumed (animations resumed)");
        }
    }
    
    /**
     * Освобождает ресурсы
     */
    public void cleanup() {
        stop();
        
        // Удаляем listener
        if (player != null && playerListener != null) {
            player.removeListener(playerListener);
            playerListener = null;
        }
        
        player = null;
        Log.d(TAG, "Cleanup completed");
    }
}

