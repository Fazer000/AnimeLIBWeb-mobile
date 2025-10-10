package com.example.animelib.managers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.OptIn;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.ui.PlayerView;

import java.util.Locale;

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
    private boolean isGestureCooldown = false;
    private float swipeStartX = 0f;
    private float swipeStartY = 0f;
    private int swipeTouchSlopPx = 0;
    
    // Переменные для hold-to-speed с регулировкой
    private long holdStartTime = 0L;
    private Runnable holdToSpeedRunnable = null;
    private float currentSpeedMultiplier = 1.0f;
    private boolean isSpeedAdjustmentMode = false;
    private float speedAdjustmentStartX = 0f;
    private float speedAdjustmentSensitivity = 0.5f; // Чувствительность регулировки скорости
    
    // Переменные для свайпа (как в оригинале)
    private float swipeAccumulatedDx = 0f;
    private long basePositionMs = 0L;
    private Float lastSwipeX = null;
    
    // Переменные для edge swipes
    private boolean isEdgeSwipe = false;
    private int screenWidth = 0;
    private int screenHeight = 0;
    private static final int EDGE_SWIPE_THRESHOLD = 50; // dp для определения края экрана
    private static final int BOTTOM_ZONE_HEIGHT = 150; // dp для нижней зоны (над таймбаром)
    private static final int TOP_ZONE_HEIGHT = 150; // dp для верхней зоны (для закрытия эпизодов)
    private int edgeSwipeThresholdPx = 0;
    private int bottomZoneHeightPx = 0;
    private int topZoneHeightPx = 0;
    
    // Callback интерфейсы
    public interface GestureCallback {
        void onSeekGesture(long seekPosition);
        void onSpeedChange(float speed);
        void updatePlayLoadingIndicator(int playbackState);
        void onEpisodesSwipeUp(); // Свайп снизу вверх для открытия эпизодов
        void onEpisodesSwipeDown(); // Свайп сверху вниз для закрытия эпизодов
        void onCommentsSwipeFromRight(); // Свайп справа для комментариев
        void onPlayersSwipeFromRight(); // Свайп справа снизу для озвучек
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
        
        // Конвертируем dp в пиксели для edge swipes
        float density = context.getResources().getDisplayMetrics().density;
        edgeSwipeThresholdPx = (int) (EDGE_SWIPE_THRESHOLD * density);
        bottomZoneHeightPx = (int) (BOTTOM_ZONE_HEIGHT * density);
        topZoneHeightPx = (int) (TOP_ZONE_HEIGHT * density);
        
        // Получаем размеры экрана
        screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        screenHeight = context.getResources().getDisplayMetrics().heightPixels;
        
        Log.d(TAG, "GesturesManager initialized - touch slop: " + swipeTouchSlopPx + 
                ", edge threshold: " + edgeSwipeThresholdPx + 
                ", bottom zone: " + bottomZoneHeightPx + ", top zone: " + topZoneHeightPx +
                ", screen: " + screenWidth + "x" + screenHeight);
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
    @OptIn(markerClass = UnstableApi.class)
    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
    private void setupSwipeSeek() {
        if (playerView == null) return;
        
        playerView.setOnTouchListener((v, event) -> {
            if (player == null) return false;
            
            // Если активен cooldown после жестов, не перехватываем события
            if (isGestureCooldown) {
                return false;
            }
            
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    // Принудительно сбрасываем все состояния жестов
                    if (isHoldToSpeed && player != null) {
                        Log.d(TAG, "Force reset hold-to-speed on new touch");
                        resetHoldToSpeed();
                    }
                    
                     // Инициализируем переменные для жестов
                     isSwipingSeek = false;
                     isEdgeSwipe = false;
                     isSpeedAdjustmentMode = false;
                     swipeAccumulatedDx = 0f;
                     assert player != null;
                     basePositionMs = player.getCurrentPosition();
                     lastSwipeX = event.getX();
                     swipeStartX = event.getX();
                     swipeStartY = event.getY();
                     speedAdjustmentStartX = event.getX();
                     holdStartTime = System.currentTimeMillis();
                     currentSpeedMultiplier = 1.0f; // Сбрасываем скорость на 1.0x при новом касании
                    
                    // НЕ проверяем edge swipe в ACTION_DOWN - это блокирует обычные жесты
                    // Проверка будет в ACTION_MOVE
                    
                    // Запускаем таймер для hold-to-speed
                    if (holdToSpeedRunnable != null) {
                        v.removeCallbacks(holdToSpeedRunnable);
                    }
                    holdToSpeedRunnable = () -> {
                        if (!isSwipingSeek && !isHoldToSpeed && player != null) {
                            Log.d(TAG, "Hold to speed activated");
                            activateHoldToSpeed();
                        }
                    };
                    v.postDelayed(holdToSpeedRunnable, 500); // 500ms для активации hold-to-speed
                    
                    if (seekPreviewText != null) {
                        seekPreviewText.setVisibility(View.GONE);
                        if (seekPreviewText instanceof android.widget.TextView) {
                            ((android.widget.TextView) seekPreviewText).setText("0 c");
                        }
                    }
                    return false; // НЕ перехватываем событие, позволяем контролам работать
                    
                case MotionEvent.ACTION_MOVE:
                    float currentX = event.getX();
                    float currentY = event.getY();
                    float totalDx = Math.abs(currentX - swipeStartX);
                    float totalDy = Math.abs(currentY - swipeStartY);
                    boolean passedDeadZone = totalDx > swipeTouchSlopPx && totalDx > totalDy * 1.5f;

                    // Проверяем edge swipe ТОЛЬКО если начало было с края И есть движение
                    if (!isEdgeSwipe && !isSwipingSeek && !isHoldToSpeed && 
                        (totalDx > swipeTouchSlopPx || totalDy > swipeTouchSlopPx)) {
                        // Проверяем, начался ли жест с края экрана
                        if (isEdgeSwipeStart(swipeStartX, swipeStartY)) {
                            EdgeSwipeType swipeType = detectEdgeSwipeType(swipeStartX, swipeStartY, currentX, currentY);
                            if (swipeType != EdgeSwipeType.NONE) {
                                isEdgeSwipe = true;
                                handleEdgeSwipe(swipeType);
                                return true;
                            }
                        }
                    }

                    // Если есть движение, отменяем hold-to-speed
                    if (passedDeadZone && holdToSpeedRunnable != null) {
                        v.removeCallbacks(holdToSpeedRunnable);
                        holdToSpeedRunnable = null;
                    }

                    // Обработка регулировки скорости при активном hold-to-speed
                    if (isHoldToSpeed) {
                        handleSpeedAdjustment(currentX);
                        return true;
                    }

                    // Если уже обработали edge swipe, не продолжаем
                    if (isEdgeSwipe) {
                        return true;
                    }

                    if (!isSwipingSeek) {
                        if (passedDeadZone) {
                            isSwipingSeek = true; // Устанавливаем сразу без задержки
                            // блокируем перехват родителями (ViewPager и т.п.)
                            android.view.ViewParent p = v.getParent();
                            if (p != null) p.requestDisallowInterceptTouchEvent(true);
                            // показать превью
                            if (seekPreviewText != null)
                                seekPreviewText.setVisibility(View.VISIBLE);
                            if (gestureCallback != null && player != null) {
                                gestureCallback.updatePlayLoadingIndicator(player.getPlaybackState());
                            }
                            
                            // Показываем контролы при начале свайпа
                            if (playerView != null) {
                                playerView.showController();
                            }
                            
                            return true;
                        } else {
                            return false; // НЕ перехватываем событие для обычных касаний
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
                    
                    boolean handledGesture = false;
                    
                    // Handle swipe seek completion
                    if (isSwipingSeek) {
                        long finalOffsetMs = Math.round(swipeAccumulatedDx / 12f) * 1000L;
                        long newPos = Math.max(0, basePositionMs + finalOffsetMs);
                        long dur = player.getDuration();
                        if (dur > 0) newPos = Math.min(newPos, dur);
                        player.seekTo(newPos);
                        Log.d(TAG, "Seek completed: " + finalOffsetMs + "ms");
                        
                        // Показываем контролы плеера после перемотки
                        if (playerView != null) {
                            playerView.showController();
                        }
                        
                        // Активируем cooldown для восстановления кликабельности
                        activateGestureCooldown();
                        
                        handledGesture = true;
                    }
                    
                    // Handle hold-to-speed release
                    if (isHoldToSpeed) {
                        Log.d(TAG, "Hold to speed deactivated");
                        resetHoldToSpeed();
                        
                        // Показываем контролы плеера после завершения ускорения
                        if (playerView != null) {
                            playerView.showController();
                        }
                        
                        // Активируем cooldown для восстановления кликабельности
                        activateGestureCooldown();
                        
                        handledGesture = true;
                    }
                    
                    // Сбрасываем состояние
                    isSwipingSeek = false;
                    isEdgeSwipe = false;
                    swipeAccumulatedDx = 0f;
                    lastSwipeX = null;
                    
                    // Если был какой-то жест, перехватываем событие
                    // Если не было жеста, позволяем клику пройти для показа контролов
                    return handledGesture;
            }
            return false;
        });
    }
    
    /**
     * Типы edge swipes
     */
    private enum EdgeSwipeType {
        NONE,
        EPISODES_UP,        // Свайп снизу вверх для открытия эпизодов
        EPISODES_DOWN,      // Свайп сверху вниз для закрытия эпизодов
        COMMENTS_RIGHT,     // Свайп справа сверху для комментариев
        PLAYERS_RIGHT       // Свайп справа снизу для озвучек
    }
    
    /**
     * Проверяет, начался ли жест с края экрана
     */
    private boolean isEdgeSwipeStart(float x, float y) {
        // Проверяем правый край (для комментариев и озвучек)
        boolean isRightEdge = x > (screenWidth - edgeSwipeThresholdPx);
        
        // Проверяем нижний край (для открытия эпизодов)
        boolean isBottomEdge = y > (screenHeight - bottomZoneHeightPx);
        
        // Проверяем верхний край (для закрытия эпизодов)
        boolean isTopEdge = y < topZoneHeightPx;
        
        return isRightEdge || isBottomEdge || isTopEdge;
    }
    
    /**
     * Определяет тип edge swipe на основе начальной и конечной позиции
     */
    private EdgeSwipeType detectEdgeSwipeType(float startX, float startY, float endX, float endY) {
        float deltaX = endX - startX;
        float deltaY = endY - startY;
        float absDeltaX = Math.abs(deltaX);
        float absDeltaY = Math.abs(deltaY);
        
        // Свайп снизу вверх для открытия эпизодов
        if (startY > (screenHeight - bottomZoneHeightPx) && deltaY < -swipeTouchSlopPx && absDeltaY > absDeltaX) {
            Log.d(TAG, "Detected EPISODES_UP swipe");
            return EdgeSwipeType.EPISODES_UP;
        }
        
        // Свайп сверху вниз для закрытия эпизодов
        if (startY < topZoneHeightPx && deltaY > swipeTouchSlopPx && absDeltaY > absDeltaX) {
            Log.d(TAG, "Detected EPISODES_DOWN swipe");
            return EdgeSwipeType.EPISODES_DOWN;
        }
        
        // Свайпы справа налево
        if (startX > (screenWidth - edgeSwipeThresholdPx) && deltaX < -swipeTouchSlopPx && absDeltaX > absDeltaY) {
            // Определяем верх или низ экрана
            boolean isTopHalf = startY < (screenHeight / 2);
            
            if (isTopHalf) {
                Log.d(TAG, "Detected COMMENTS_RIGHT swipe");
                return EdgeSwipeType.COMMENTS_RIGHT;
            } else {
                Log.d(TAG, "Detected PLAYERS_RIGHT swipe");
                return EdgeSwipeType.PLAYERS_RIGHT;
            }
        }
        
        return EdgeSwipeType.NONE;
    }
    
    /**
     * Обрабатывает edge swipe и вызывает соответствующий callback
     */
    private void handleEdgeSwipe(EdgeSwipeType swipeType) {
        if (gestureCallback == null) {
            Log.w(TAG, "GestureCallback is null, cannot handle edge swipe");
            return;
        }
        
        switch (swipeType) {
            case EPISODES_UP:
                Log.d(TAG, "Opening episodes panel");
                gestureCallback.onEpisodesSwipeUp();
                break;
            case EPISODES_DOWN:
                Log.d(TAG, "Closing episodes panel");
                gestureCallback.onEpisodesSwipeDown();
                break;
            case COMMENTS_RIGHT:
                Log.d(TAG, "Opening comments panel");
                gestureCallback.onCommentsSwipeFromRight();
                break;
            case PLAYERS_RIGHT:
                Log.d(TAG, "Opening players panel");
                gestureCallback.onPlayersSwipeFromRight();
                break;
            case NONE:
                // Ничего не делаем
                break;
        }
    }
    
    /**
     * Активирует режим ускорения с начальной скоростью 2x
     */
    private void activateHoldToSpeed() {
        isHoldToSpeed = true;
        currentSpeedMultiplier = 2.0f;
        
        // Set playback speed to 2x
        if (player != null) {
            player.setPlaybackSpeed(currentSpeedMultiplier);
        }
        
        // Show speed toast
        if (holdSpeedToast != null) {
            holdSpeedToast.setVisibility(View.VISIBLE);
            holdSpeedToast.setAlpha(0f);
            holdSpeedToast.animate().alpha(1f).setDuration(120).start();
        }
        
        // Обновляем текст в toast
        updateSpeedToast();
        
        // Notify callback about speed change
        if (gestureCallback != null) {
            gestureCallback.onSpeedChange(currentSpeedMultiplier);
        }
        
        Log.d(TAG, "Hold to speed activated with speed: " + currentSpeedMultiplier);
    }
    
    /**
     * Сбрасывает режим ускорения
     */
    private void resetHoldToSpeed() {
        isHoldToSpeed = false;
        isSpeedAdjustmentMode = false;
        currentSpeedMultiplier = 1.0f;
        
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
        
        Log.d(TAG, "Hold to speed reset to 1.0x");
    }
    
    /**
     * Обрабатывает регулировку скорости движением влево/вправо
     */
    private void handleSpeedAdjustment(float currentX) {
        if (!isSpeedAdjustmentMode) {
            // Активируем режим регулировки скорости при первом движении
            isSpeedAdjustmentMode = true;
            speedAdjustmentStartX = currentX;
            Log.d(TAG, "Speed adjustment mode activated");
            return;
        }
        
        // Вычисляем изменение позиции
        float deltaX = currentX - speedAdjustmentStartX;
        
        // Вычисляем новую скорость на основе движения
        // Движение вправо = увеличение скорости, влево = уменьшение
        float speedChange = deltaX * speedAdjustmentSensitivity / 100f; // Чувствительность
        float newSpeed = Math.max(0.25f, Math.min(4.0f, 2.0f + speedChange)); // Ограничиваем от 0.25x до 4x
        
        // Обновляем скорость только если она изменилась значительно
        if (Math.abs(newSpeed - currentSpeedMultiplier) > 0.1f) {
            currentSpeedMultiplier = newSpeed;
            
            // Применяем новую скорость
            if (player != null) {
                player.setPlaybackSpeed(currentSpeedMultiplier);
            }
            
            // Обновляем toast с новой скоростью
            updateSpeedToast();
            
            // Notify callback about speed change
            if (gestureCallback != null) {
                gestureCallback.onSpeedChange(currentSpeedMultiplier);
            }
            
            Log.d(TAG, "Speed adjusted to: " + currentSpeedMultiplier + "x (deltaX: " + deltaX + ")");
        }
    }
    
    /**
     * Обновляет отображение скорости в toast
     */
    private void updateSpeedToast() {
        if (holdSpeedToast != null && holdSpeedToast instanceof android.widget.TextView) {
            @SuppressLint("DefaultLocale")
            String speedText = String.format(Locale.US, "%.1f", currentSpeedMultiplier) + "x";

            ((android.widget.TextView) holdSpeedToast).setText(speedText);
        }
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
            resetHoldToSpeed();
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
            resetHoldToSpeed();
        }
        
        this.playerView = playerView;
        setupCombinedGestures(); // Re-setup gestures with new PlayerView
        Log.d(TAG, "PlayerView updated and gestures re-setup");
    }
    
    /**
     * Активирует cooldown для восстановления кликабельности контролов
     */
    private void activateGestureCooldown() {
        isGestureCooldown = true;
        Log.d(TAG, "Gesture cooldown activated");
        
        // Отключаем cooldown через 300мс
        if (playerView != null) {
            playerView.postDelayed(() -> {
                isGestureCooldown = false;
                Log.d(TAG, "Gesture cooldown deactivated");
            }, 300);
        }
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
        isEdgeSwipe = false;
        isGestureCooldown = false; // Сбрасываем cooldown
        
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
            resetHoldToSpeed();
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
            resetHoldToSpeed();
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
