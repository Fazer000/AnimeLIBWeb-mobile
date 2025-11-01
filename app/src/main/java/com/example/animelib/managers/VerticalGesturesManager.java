package com.example.animelib.managers;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;

/**
 * Менеджер для обработки вертикальных жестов (эпизоды и панель с информацией)
 * Работает независимо от горизонтальных жестов
 */
public class VerticalGesturesManager {
    private static final String TAG = "VerticalGestures";
    
    // Типы вертикальных панелей
    public enum PanelType {
        NONE,
        EPISODES,
        RELATED_INFO
    }
    
    // Callback интерфейс
    public interface VerticalGestureCallback {
        void onEpisodesDragProgress(float progress);
        void onRelatedInfoDragProgress(float progress);
        void onEpisodesDragComplete(boolean shouldOpen);
        void onRelatedInfoDragComplete(boolean shouldOpen);
        boolean isEpisodesOpen();
        boolean isRelatedInfoOpen();
    }
    
    private final Context context;
    private VerticalGestureCallback callback;
    private GesturesManager gesturesManager; // Для отмены hold-to-speed таймера
    
    // Размеры экрана
    private int screenHeight;
    private int screenWidth;
    private int bottomZoneHeight; // Зона для эпизодов снизу
    
    // Состояние drag
    private boolean isDragging = false;
    private PanelType currentPanel = PanelType.NONE;
    private float dragStartY;
    private float dragStartX;
    
    // Пороги
    private static final float OPEN_THRESHOLD = 0.5f; // 50% для открытия
    private static final float DRAG_SENSITIVITY = 0.20f; // 20% высоты экрана - чувствительность для эпизодов
    private static final float RELATED_INFO_DRAG_SENSITIVITY = 0.6f; // 25% высоты экрана - для панели с инфо (336px на экране 1344px)
    private static final float MIN_DRAG_DISTANCE = 20f; // Минимальное расстояние для начала drag
    
    public VerticalGesturesManager(Context context) {
        this.context = context;
        initializeScreenDimensions();
    }
    
    public void setCallback(VerticalGestureCallback callback) {
        this.callback = callback;
    }
    
    public void setGesturesManager(GesturesManager gesturesManager) {
        this.gesturesManager = gesturesManager;
    }
    
    private void initializeScreenDimensions() {
        screenHeight = context.getResources().getDisplayMetrics().heightPixels;
        screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        bottomZoneHeight = screenHeight / 3; // 33% снизу для эпизодов (увеличено с 25%)
        
        Log.d(TAG, "Screen: " + screenWidth + "x" + screenHeight + ", bottomZone: " + bottomZoneHeight + "px");
    }
    
    /**
     * Обрабатывает touch событие
     * @return true если событие было обработано
     */
    public boolean onTouchEvent(MotionEvent event) {
        if (callback == null) return false;
        
        // ВАЖНО: Если панель с инфо уже открыта, НЕ обрабатываем события
        // Закрытие панели обрабатывается через OnTouchListener на relatedTitlesOverlay
        if (callback.isRelatedInfoOpen()) {
            return false; // Пропускаем событие
        }
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return handleDown(event);
                
            case MotionEvent.ACTION_MOVE:
                return handleMove(event);
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                return handleUp(event);
        }
        
        return false;
    }
    
    private boolean handleDown(MotionEvent event) {
        // Используем getRawY/getRawX для абсолютных координат экрана
        dragStartY = event.getRawY();
        dragStartX = event.getRawX();
        isDragging = false;
        currentPanel = PanelType.NONE;
        return false; // Не захватываем событие на DOWN
    }
    
    private boolean handleMove(MotionEvent event) {
        // Используем getRawY/getRawX для абсолютных координат экрана
        // Это позволяет продолжать drag даже если палец вышел за границы View
        float currentY = event.getRawY();
        float currentX = event.getRawX();
        float deltaY = currentY - dragStartY;
        float deltaX = currentX - dragStartX;
        float absDeltaY = Math.abs(deltaY);
        float absDeltaX = Math.abs(deltaX);
        
        // Проверяем минимальное расстояние для начала drag
        if (!isDragging && absDeltaY < MIN_DRAG_DISTANCE) {
            // Log.d(TAG, "Movement too small: " + absDeltaY + " < " + MIN_DRAG_DISTANCE);
            return false;
        }
        
        // Определяем тип панели если еще не определен
        if (!isDragging) {
            // ВАЖНО: Блокируем вертикальные жесты если активен горизонтальный
            if (gesturesManager != null && gesturesManager.isHorizontalGestureActive()) {
                Log.d(TAG, "Horizontal gesture is active, blocking vertical gesture");
                return false;
            }
            
            Log.d(TAG, "Checking vertical gesture: deltaY=" + deltaY + ", deltaX=" + deltaX + 
                  ", absDeltaY=" + absDeltaY + ", absDeltaX=" + absDeltaX);
            
            // ВАЖНО: Проверяем что это преимущественно вертикальное движение
            // Если горизонтальный компонент слишком большой, не обрабатываем
            if (absDeltaX > absDeltaY * 0.7f) {
                Log.d(TAG, "Too much horizontal movement, ignoring: deltaX=" + deltaX + ", deltaY=" + deltaY);
                return false;
            }
            
            currentPanel = detectPanelType(deltaY);
            if (currentPanel == PanelType.NONE) {
                return false;
            }
            isDragging = true;
            
            // ВАЖНО: Отменяем таймер hold-to-speed в GesturesManager
            if (gesturesManager != null) {
                gesturesManager.cancelHoldToSpeedTimer();
            }
            
            Log.d(TAG, "Started dragging: " + currentPanel + ", deltaY=" + deltaY);
        }
        
        // Вычисляем прогресс используя абсолютные координаты
        float progress = calculateProgress(currentPanel, currentY);
        
        // Отправляем прогресс
        if (currentPanel == PanelType.EPISODES) {
            callback.onEpisodesDragProgress(progress);
        } else if (currentPanel == PanelType.RELATED_INFO) {
            callback.onRelatedInfoDragProgress(progress);
        }
        
        return true; // Захватываем событие во время drag
    }
    
    private boolean handleUp(MotionEvent event) {
        if (!isDragging || currentPanel == PanelType.NONE) {
            isDragging = false;
            currentPanel = PanelType.NONE;
            return false;
        }
        
        // Вычисляем финальный прогресс используя getRawY() для абсолютных координат
        float finalProgress = calculateProgress(currentPanel, event.getRawY());
        boolean shouldOpen = finalProgress > OPEN_THRESHOLD;
        
        Log.d(TAG, "Drag completed: " + currentPanel + ", progress=" + finalProgress + ", shouldOpen=" + shouldOpen);
        
        // Отправляем событие завершения
        if (currentPanel == PanelType.EPISODES) {
            callback.onEpisodesDragComplete(shouldOpen);
        } else if (currentPanel == PanelType.RELATED_INFO) {
            callback.onRelatedInfoDragComplete(shouldOpen);
        }
        
        isDragging = false;
        currentPanel = PanelType.NONE;
        return true;
    }
    
    /**
     * Определяет тип панели на основе направления свайпа и текущего состояния
     * РАБОТАЕТ ПО ВСЕМУ ЭКРАНУ без зон
     */
    private PanelType detectPanelType(float deltaY) {
        boolean isEpisodesOpen = callback.isEpisodesOpen();
        boolean isRelatedOpen = callback.isRelatedInfoOpen();
        
        Log.d(TAG, "detectPanelType: deltaY=" + deltaY + ", dragStartY=" + dragStartY + 
              ", isEpisodesOpen=" + isEpisodesOpen + ", isRelatedOpen=" + isRelatedOpen);
        
        if (deltaY > 0) {
            // Свайп ВНИЗ по всему экрану
            if (isRelatedOpen) {
                // Панель с инфо открыта - НЕ закрываем свайпом вниз (только через overlay)
                Log.d(TAG, "→ NONE (related is open, use overlay to close)");
                return PanelType.NONE;
            } else if (isEpisodesOpen) {
                // Эпизоды открыты - закрываем их свайпом вниз
                Log.d(TAG, "→ EPISODES (closing episodes)");
                return PanelType.EPISODES;
            } else {
                // Обе закрыты - открываем панель с инфо свайпом вниз
                Log.d(TAG, "→ RELATED_INFO (opening related)");
                return PanelType.RELATED_INFO;
            }
        } else {
            // Свайп ВВЕРХ по всему экрану
            if (isRelatedOpen) {
                // Панель с инфо открыта - НЕ обрабатываем (закрытие через overlay)
                Log.d(TAG, "→ NONE (related is open, use overlay to close)");
                return PanelType.NONE;
            } else if (isEpisodesOpen) {
                // Эпизоды уже открыты - работаем с ними
                Log.d(TAG, "→ EPISODES (already open)");
                return PanelType.EPISODES;
            } else {
                // Обе закрыты - открываем эпизоды свайпом вверх ПО ВСЕМУ ЭКРАНУ
                Log.d(TAG, "→ EPISODES (opening episodes from anywhere)");
                return PanelType.EPISODES;
            }
        }
    }
    
    /**
     * Вычисляет прогресс (0.0 - 1.0) на основе текущего положения
     */
    private float calculateProgress(PanelType panel, float currentY) {
        float distance;
        float maxDistance;
        
        if (panel == PanelType.EPISODES) {
            // Для эпизодов используем более высокую чувствительность
            maxDistance = screenHeight * DRAG_SENSITIVITY;
            boolean isOpen = callback.isEpisodesOpen();
            distance = dragStartY - currentY; // Вверх положительное
            
            if (isOpen) {
                // Открыта: начинаем с 1.0, движение вниз (отрицательное) закрывает
                float rawProgress = distance / maxDistance;
                return Math.max(0f, Math.min(1f, 1.0f + rawProgress));
            } else {
                // Закрыта: начинаем с 0.0, движение вверх (положительное) открывает
                // НЕ ограничиваем максимум - позволяем overscroll для плавного drag
                float rawProgress = distance / maxDistance;
                return Math.max(0f, rawProgress);
            }
            
        } else if (panel == PanelType.RELATED_INFO) {
            // Для панели с инфо используем меньшую чувствительность (требуется больше движения)
            maxDistance = screenHeight * RELATED_INFO_DRAG_SENSITIVITY;
            boolean isOpen = callback.isRelatedInfoOpen();
            distance = currentY - dragStartY; // Вниз положительное
            
            if (isOpen) {
                // Открыта: начинаем с 1.0, любое движение закрывает
                float rawProgress = Math.abs(distance) / maxDistance;
                return Math.max(0f, Math.min(1f, 1.0f - rawProgress));
            } else {
                // Закрыта: начинаем с 0.0, движение вниз (положительное) открывает
                // НЕ ограничиваем максимум - позволяем overscroll для плавного drag
                float rawProgress = distance / maxDistance;
                return Math.max(0f, rawProgress);
            }
        }
        
        return 0f;
    }
    
    /**
     * Проверяет, обрабатывается ли сейчас вертикальный жест
     */
    public boolean isDragging() {
        return isDragging;
    }
    
    /**
     * Сбрасывает состояние
     */
    public void reset() {
        isDragging = false;
        currentPanel = PanelType.NONE;
    }
}

