package com.example.animelib.managers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.media3.common.Player;
import androidx.media3.ui.PlayerView;

/**
 * Менеджер для управления жестами плеера
 * Обеспечивает функциональность свайпа для перемотки и удержания для ускорения
 */
public class GesturesManager {
    private static final String TAG = "GesturesManager";
    
    // Контекст и зависимости
    private final Context context;
    private PlayerView playerView;
    private Player player;
    
    // UI компоненты
    private View holdSpeedToast;
    private View seekPreviewText;
    
    // Состояние жестов
    private boolean isSwipingSeek = false;
    private boolean isHoldToSpeed = false;
    private float swipeStartX = 0f;
    private float swipeStartY = 0f;
    private int swipeTouchSlopPx = 0;
    
    // Переменные для hold-to-speed
    private long holdStartTime = 0L;
    private Runnable holdToSpeedRunnable = null;
    
    // Переменные для свайпа (как в оригинале)
    private float swipeAccumulatedDx = 0f;
    private long basePositionMs = 0L;
    private Float lastSwipeX = null;
    
    // Callback интерфейсы
    public interface GestureCallback {
        void onSeekGesture(long seekPosition);
        void onSpeedChange(float speed);
        void updatePlayLoadingIndicator(int playbackState);
    }
    
    private GestureCallback gestureCallback;
    
    /**
     * Конструктор GesturesManager
     * @param context Контекст приложения
     */
    public GesturesManager(Context context) {
        this.context = context;
        
        // Получаем минимальное расстояние для распознавания свайпа
        ViewConfiguration config = ViewConfiguration.get(context);
        swipeTouchSlopPx = config.getScaledTouchSlop();
        
        Log.d(TAG, "GesturesManager initialized with touch slop: " + swipeTouchSlopPx);
    }
    
    /**
     * Инициализация с UI компонентами
     * @param playerView PlayerView для обработки жестов
     * @param player ExoPlayer для управления воспроизведением
     * @param holdSpeedToast Toast для отображения ускорения
     * @param seekPreviewText TextView для отображения превью перемотки
     */
    public void initializeViews(PlayerView playerView, Player player, View holdSpeedToast, View seekPreviewText) {
        this.playerView = playerView;
        this.player = player;
        this.holdSpeedToast = holdSpeedToast;
        this.seekPreviewText = seekPreviewText;
        
        setupGestures();
    }
    
    /**
     * Настройка всех жестов
     */
    @SuppressLint("ClickableViewAccessibility")
    private void setupGestures() {
        if (playerView == null) {
            Log.w(TAG, "PlayerView is null, cannot setup gestures");
            return;
        }
        
        setupCombinedGestures();
        
        Log.d(TAG, "Gestures setup completed");
    }
    
    /**
     * Настройка объединенных жестов (swipe seek + hold to speed)
     */
    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
    private void setupCombinedGestures() {
        if (playerView == null) return;
        
        // Убираем OnLongClickListener - будем обрабатывать все через OnTouchListener
        playerView.setOnLongClickListener(null);
        
        // Setup combined touch listener for both swipe seek and hold-to-speed
        setupSwipeSeek();
    }
    
    /**
     * Настройка свайпа для перемотки (оригинальная реализация)
     */
    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
    private void setupSwipeSeek() {
        if (playerView == null) return;
        
        playerView.setOnTouchListener((v, event) -> {
            if (player == null) return false;
            
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    // Принудительно сбрасываем все состояния жестов
                    if (isHoldToSpeed && player != null) {
                        Log.d(TAG, "Force reset hold-to-speed on new touch");
                        isHoldToSpeed = false;
                        player.setPlaybackSpeed(1.0f);
                        if (holdSpeedToast != null) {
                            holdSpeedToast.setVisibility(View.GONE);
                        }
                        if (gestureCallback != null) {
                            gestureCallback.onSpeedChange(1.0f);
                        }
                    }
                    
                    // Не перехватываем сразу — даём кликам/контролам работать
                    isSwipingSeek = false;
                    swipeAccumulatedDx = 0f;
                    basePositionMs = player.getCurrentPosition();
                    lastSwipeX = event.getX();
                    swipeStartX = event.getX();
                    swipeStartY = event.getY();
                    holdStartTime = System.currentTimeMillis();
                    
                    // Запускаем таймер для hold-to-speed
                    if (holdToSpeedRunnable != null) {
                        v.removeCallbacks(holdToSpeedRunnable);
                    }
                    holdToSpeedRunnable = () -> {
                        if (!isSwipingSeek && !isHoldToSpeed && player != null) {
                            Log.d(TAG, "Hold to speed activated");
                            isHoldToSpeed = true;
                            
                            // Set playback speed to 2x
                            float speedMultiplier = 2.0f;
                            player.setPlaybackSpeed(speedMultiplier);
                            
                            // Show speed toast
                            if (holdSpeedToast != null) {
                                holdSpeedToast.setVisibility(View.VISIBLE);
                                holdSpeedToast.setAlpha(0f);
                                holdSpeedToast.animate().alpha(1f).setDuration(120).start();
                            }
                            
                            // Notify callback about speed change
                            if (gestureCallback != null) {
                                gestureCallback.onSpeedChange(speedMultiplier);
                            }
                        }
                    };
                    v.postDelayed(holdToSpeedRunnable, 500); // 500ms для активации hold-to-speed
                    
                    if (seekPreviewText != null) {
                        seekPreviewText.setVisibility(View.GONE);
                        if (seekPreviewText instanceof android.widget.TextView) {
                            ((android.widget.TextView) seekPreviewText).setText("0 c");
                        }
                    }
                    return false;
                    
                case MotionEvent.ACTION_MOVE:
                    float currentX = event.getX();
                    float currentY = event.getY();
                    float totalDx = Math.abs(currentX - swipeStartX);
                    float totalDy = Math.abs(currentY - swipeStartY);
                    boolean passedDeadZone = totalDx > swipeTouchSlopPx && totalDx > totalDy * 1.5f;

                    // Если есть движение, отменяем hold-to-speed
                    if (passedDeadZone && holdToSpeedRunnable != null) {
                        v.removeCallbacks(holdToSpeedRunnable);
                        holdToSpeedRunnable = null;
                    }

                    // Don't interfere with hold-to-speed если он уже активен
                    if (isHoldToSpeed) return false;

                    if (!isSwipingSeek) {
                        if (passedDeadZone) {
                            v.postDelayed(() -> isSwipingSeek = true, 60); // задержка 60мс, чтобы не срабатывало мгновенно
                            // блокируем перехват родителями (ViewPager и т.п.)
                            android.view.ViewParent p = v.getParent();
                            if (p != null) p.requestDisallowInterceptTouchEvent(true);
                            // показать превью
                            if (seekPreviewText != null)
                                seekPreviewText.setVisibility(View.VISIBLE);
                            if (gestureCallback != null && player != null) {
                                gestureCallback.updatePlayLoadingIndicator(player.getPlaybackState());
                            }
                            return true;
                        } else {
                            return false; // пока не прошли dead zone — не трогаем
                        }
                    }

                    // уже в режиме свайпа — накапливаем дельту
                    if (lastSwipeX == null) lastSwipeX = currentX;
                    float delta = currentX - lastSwipeX;
                    lastSwipeX = currentX;
                    swipeAccumulatedDx += delta;
                    long offsetSec = Math.round(swipeAccumulatedDx / 12f);
                    if (seekPreviewText != null && seekPreviewText instanceof android.widget.TextView) {
                        String sign = offsetSec >= 0 ? "+" : "";
                        ((android.widget.TextView) seekPreviewText).setText(sign + offsetSec + " c");
                    }
                    if (gestureCallback != null && player != null) {
                        gestureCallback.updatePlayLoadingIndicator(player.getPlaybackState());
                    }
                    return true;
                    
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    // Отменяем таймер hold-to-speed
                    if (holdToSpeedRunnable != null) {
                        v.removeCallbacks(holdToSpeedRunnable);
                        holdToSpeedRunnable = null;
                    }
                    
                    // Принудительно скрываем seek preview если он видим
                    if (seekPreviewText != null && seekPreviewText.getVisibility() == View.VISIBLE) {
                        seekPreviewText.setVisibility(View.GONE);
                    }
                    
                    // Handle swipe seek completion
                    if (isSwipingSeek) {
                        long finalOffsetMs = Math.round(swipeAccumulatedDx / 12f) * 1000L;
                        long newPos = Math.max(0, basePositionMs + finalOffsetMs);
                        long dur = player.getDuration();
                        if (dur > 0) newPos = Math.min(newPos, dur);
                        player.seekTo(newPos);
                        v.postDelayed(() -> isSwipingSeek = false, 60);
                        swipeAccumulatedDx = 0f;
                        lastSwipeX = null;
                        if (gestureCallback != null && player != null) {
                            gestureCallback.updatePlayLoadingIndicator(player.getPlaybackState());
                        }
                        return true; // съедаем up, чтобы не кликалось
                    }
                    
                    // Handle hold-to-speed release
                    if (isHoldToSpeed) {
                        Log.d(TAG, "Hold to speed deactivated");
                        isHoldToSpeed = false;
                        
                        // Reset to normal speed
                        if (player != null) {
                            player.setPlaybackSpeed(1.0f);
                        }
                        
                        // Hide speed toast
                        if (holdSpeedToast != null) {
                            holdSpeedToast.animate().alpha(0f).setDuration(120)
                                    .withEndAction(() -> holdSpeedToast.setVisibility(View.GONE))
                                    .start();
                        }
                        
                        // Notify callback about speed reset
                        if (gestureCallback != null) {
                            gestureCallback.onSpeedChange(1.0f);
                        }
                        
                        if (gestureCallback != null && player != null) {
                            gestureCallback.updatePlayLoadingIndicator(player.getPlaybackState());
                        }
                        return true;
                    }
                    
                    // не было свайпа или hold-to-speed — передаём дальше, чтобы сработали клики
                    isSwipingSeek = false;
                    swipeAccumulatedDx = 0f;
                    lastSwipeX = null;
                    return false;
            }
            return false;
        });
    }
    
    // handleSeekSwipe logic is now integrated into setupSwipeSeek
    
    // setupHoldToSpeed logic is now integrated into setupCombinedGestures
    
    /**
     * Обновление ссылки на плеер
     * @param player Новый экземпляр плеера
     */
    public void updatePlayer(Player player) {
        // Принудительно сбрасываем hold-to-speed при смене плеера
        if (isHoldToSpeed && this.player != null) {
            Log.d(TAG, "Resetting hold-to-speed on player update");
            isHoldToSpeed = false;
            this.player.setPlaybackSpeed(1.0f);
            if (holdSpeedToast != null) {
                holdSpeedToast.setVisibility(View.GONE);
            }
            if (gestureCallback != null) {
                gestureCallback.onSpeedChange(1.0f);
            }
        }
        
        this.player = player;
        Log.d(TAG, "Player updated");
    }
    
    /**
     * Обновление ссылки на PlayerView
     * @param playerView Новый экземпляр PlayerView
     */
    public void updatePlayerView(PlayerView playerView) {
        // Принудительно сбрасываем hold-to-speed при смене PlayerView
        if (isHoldToSpeed && player != null) {
            Log.d(TAG, "Resetting hold-to-speed on PlayerView update");
            isHoldToSpeed = false;
            player.setPlaybackSpeed(1.0f);
            if (holdSpeedToast != null) {
                holdSpeedToast.setVisibility(View.GONE);
            }
            if (gestureCallback != null) {
                gestureCallback.onSpeedChange(1.0f);
            }
        }
        
        this.playerView = playerView;
        setupCombinedGestures(); // Re-setup gestures with new PlayerView
        Log.d(TAG, "PlayerView updated and gestures re-setup");
    }
    
    /**
     * Проверка, выполняется ли свайп для перемотки
     * @return true если выполняется свайп
     */
    public boolean isSwipingSeek() {
        return isSwipingSeek;
    }
    
    /**
     * Проверка, активно ли ускорение удержанием
     * @return true если активно ускорение
     */
    public boolean isHoldToSpeed() {
        return isHoldToSpeed;
    }
    
    /**
     * Принудительная остановка всех жестов
     */
    public void stopAllGestures() {
        Log.d(TAG, "Stopping all gestures");
        
        isSwipingSeek = false;
        
        // Принудительно отменяем таймер
        if (holdToSpeedRunnable != null && playerView != null) {
            playerView.removeCallbacks(holdToSpeedRunnable);
            holdToSpeedRunnable = null;
        }
        
        // Принудительно скрываем seek preview
        if (seekPreviewText != null) {
            seekPreviewText.setVisibility(View.GONE);
        }
        
        // Принудительно сбрасываем hold-to-speed
        if (isHoldToSpeed && player != null) {
            Log.d(TAG, "Force stopping hold-to-speed");
            isHoldToSpeed = false;
            player.setPlaybackSpeed(1.0f);
            
            if (holdSpeedToast != null) {
                holdSpeedToast.setVisibility(View.GONE);
            }
            
            if (gestureCallback != null) {
                gestureCallback.onSpeedChange(1.0f);
            }
        }
        
        // Re-setup gestures to restore normal functionality
        setupCombinedGestures();
    }
    
    /**
     * Скрытие всех UI элементов жестов (для PiP режима)
     */
    public void hideAllGesturesUI() {
        // Принудительно сбрасываем hold-to-speed
        if (isHoldToSpeed && player != null) {
            Log.d(TAG, "Force resetting hold-to-speed on UI hide");
            isHoldToSpeed = false;
            player.setPlaybackSpeed(1.0f);
            if (gestureCallback != null) {
                gestureCallback.onSpeedChange(1.0f);
            }
        }
        
        if (holdSpeedToast != null) {
            holdSpeedToast.setVisibility(View.GONE);
        }
        
        // Принудительно скрываем seek preview
        if (seekPreviewText != null) {
            seekPreviewText.setVisibility(View.GONE);
        }
        
        // Stop any active gestures
        stopAllGestures();
        
        Log.d(TAG, "All gestures UI hidden");
    }
    
    /**
     * Показ всех UI элементов жестов (выход из PiP режима)
     */
    public void showAllGesturesUI() {
        // Gestures are automatically restored when PlayerView is available
        setupCombinedGestures();
        
        Log.d(TAG, "All gestures UI shown");
    }
    
    // Getters and Setters
    public void setGestureCallback(GestureCallback callback) {
        this.gestureCallback = callback;
    }
    
    public void setHoldSpeedToast(View holdSpeedToast) {
        this.holdSpeedToast = holdSpeedToast;
    }
    
    public void setSeekPreviewText(View seekPreviewText) {
        this.seekPreviewText = seekPreviewText;
    }
    
    /**
     * Очистка ресурсов
     */
    public void cleanup() {
        stopAllGestures();
        
        if (playerView != null) {
            playerView.setOnTouchListener(null);
            playerView.setOnLongClickListener(null);
            if (holdToSpeedRunnable != null) {
                playerView.removeCallbacks(holdToSpeedRunnable);
                holdToSpeedRunnable = null;
            }
        }
        
        playerView = null;
        player = null;
        holdSpeedToast = null;
        gestureCallback = null;
        
        Log.d(TAG, "GesturesManager cleaned up");
    }
}
