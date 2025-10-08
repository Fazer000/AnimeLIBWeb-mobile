package com.example.animelib.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Кастомная панель с возможностью drag-to-dismiss справа налево
 */
public class DraggableSidePanel extends FrameLayout {
    private static final float COLLAPSE_THRESHOLD = 0.3f;
    private static final int ANIMATION_DURATION = 250;
    
    private View dragView; // Основная панель (slidingMenuPanel или commentsPanel)
    private View dragZone; // Зона для перетаскивания
    private View animatedView; // View который будет анимироваться (корневой контейнер из include)
    private boolean isOpen = false;
    private OnPanelStateChangeListener listener;
    
    private float initialTouchX;
    private float initialTranslationX;
    private boolean isDragging = false;
    
    public interface OnPanelStateChangeListener {
        void onPanelOpened();
        void onPanelClosed();
        void onPanelSliding(float slideOffset);
        
        /**
         * Проверяет можно ли закрыть панель
         * @return true если можно закрыть, false если нельзя
         */
        default boolean canClosePanel() {
            return true;
        }
    }
    
    public DraggableSidePanel(@NonNull Context context) {
        super(context);
    }
    
    public DraggableSidePanel(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }
    
    public DraggableSidePanel(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() > 0) {
            // Первый дочерний элемент - это корневой элемент из include (CardView или LinearLayout)
            animatedView = getChildAt(0);
            dragView = animatedView; // Для совместимости
            
            // Ищем dragZone внутри animatedView
            if (animatedView instanceof android.view.ViewGroup) {
                dragZone = animatedView.findViewById(com.example.animelib.R.id.dragZone);
            }
            
            android.util.Log.d("DraggableSidePanel", "onFinishInflate: animatedView=" + animatedView + 
                ", dragZone=" + dragZone);
            
            setupTouchListener();
        }
    }
    
    @SuppressLint("ClickableViewAccessibility")
    private void setupTouchListener() {
        if (animatedView == null) {
            android.util.Log.e("DraggableSidePanel", "animatedView is null!");
            return;
        }
        
        android.util.Log.d("DraggableSidePanel", "Setting up touch listeners. dragZone=" + (dragZone != null ? "found" : "null"));
        
        // Touch listener для drag zone
        if (dragZone != null) {
            dragZone.setOnTouchListener((v, event) -> {
                if (!isOpen) return false;
                return handleTouchEvent(event);
            });
        }
        
        // Touch listener для всей панели
        animatedView.setOnTouchListener((v, event) -> {
            if (!isOpen) return false;
            return handleTouchEvent(event);
        });
    }
    
    private boolean handleTouchEvent(MotionEvent event) {
        if (!isOpen || animatedView == null) return false;
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialTouchX = event.getRawX();
                initialTranslationX = animatedView.getTranslationX();
                isDragging = true;
                
                // Останавливаем текущую анимацию если есть
                animatedView.animate().cancel();
                return true;
                
            case MotionEvent.ACTION_MOVE:
                if (!isDragging) return false;
                
                float deltaX = event.getRawX() - initialTouchX;
                float newTranslationX = initialTranslationX + deltaX;
                
                // Ограничиваем: от 0 (открыто) до width (закрыто)
                newTranslationX = Math.max(0, Math.min(newTranslationX, animatedView.getWidth()));
                animatedView.setTranslationX(newTranslationX);
                
                // Уведомляем о скольжении
                if (listener != null) {
                    float slideOffset = newTranslationX / animatedView.getWidth();
                    listener.onPanelSliding(slideOffset);
                }
                return true;
                
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (!isDragging) return false;
                    isDragging = false;
                    
                    float currentTranslationX = animatedView.getTranslationX();
                    float panelWidth = animatedView.getWidth();
                    
                    if (currentTranslationX > panelWidth * COLLAPSE_THRESHOLD) {
                        // Проверяем можно ли закрыть
                        if (listener != null && !listener.canClosePanel()) {
                            // Нельзя закрыть - показываем "тряску" и возвращаем в открытое положение
                            shakePanel();
                        } else {
                            // Закрываем
                            animateClose();
                        }
                    } else {
                        // Возвращаем в открытое положение
                        animateToOpen();
                    }
                    return true;
        }
        return false;
    }
    
    public void setOnPanelStateChangeListener(OnPanelStateChangeListener listener) {
        this.listener = listener;
    }
    
    public void openPanel() {
        if (animatedView == null) return;
        
        android.util.Log.d("DraggableSidePanel", "openPanel called. isOpen=" + isOpen + ", visibility=" + getVisibility());
        
        // Если уже открыто И анимация завершена - ничего не делаем
        if (isOpen && getVisibility() == VISIBLE && animatedView.getTranslationX() == 0) {
            android.util.Log.d("DraggableSidePanel", "Panel already open, skipping");
            return;
        }
        
        setVisibility(VISIBLE);
        
        // Ждем когда view будет измерен
        if (animatedView.getWidth() == 0) {
            animatedView.post(this::performOpen);
        } else {
            performOpen();
        }
    }
    
    private void performOpen() {
        if (animatedView == null) return;
        
        // Устанавливаем начальную позицию за экраном
        animatedView.setTranslationX(animatedView.getWidth());
        
        // Запускаем анимацию открытия
        isOpen = true;
        animateToOpen();
        
        if (listener != null) {
            listener.onPanelOpened();
        }
    }
    
    private void animateToOpen() {
        if (animatedView == null) return;
        
        animatedView.animate()
            .translationX(0)
            .setDuration(ANIMATION_DURATION)
            .setInterpolator(new android.view.animation.DecelerateInterpolator(2.5f))
            .setUpdateListener(animation -> {
                if (listener != null && animatedView.getWidth() > 0) {
                    float slideOffset = animatedView.getTranslationX() / animatedView.getWidth();
                    listener.onPanelSliding(Math.max(0f, Math.min(1f, slideOffset)));
                }
            })
            .start();
    }
    
    public void closePanel() {
        if (animatedView == null || !isOpen) return;
        
        // Проверяем можно ли закрыть
        if (listener != null && !listener.canClosePanel()) {
            android.util.Log.d("DraggableSidePanel", "Panel close blocked by listener");
            shakePanel();
            return;
        }
        
        animateClose();
    }
    
    /**
     * Анимация "тряски" панели когда её нельзя закрыть
     */
    private void shakePanel() {
        if (animatedView == null) return;
        
        // Быстрая анимация влево-вправо
        animatedView.animate()
            .translationX(20)
            .setDuration(50)
            .setInterpolator(new android.view.animation.LinearInterpolator())
            .withEndAction(() -> {
                animatedView.animate()
                    .translationX(-20)
                    .setDuration(50)
                    .withEndAction(() -> {
                        animatedView.animate()
                            .translationX(0)
                            .setDuration(50)
                            .start();
                    })
                    .start();
            })
            .start();
    }
    
    private void animateClose() {
        if (animatedView == null) return;
        
        animatedView.animate()
            .translationX(animatedView.getWidth())
            .setDuration(ANIMATION_DURATION)
            .setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f))
            .setUpdateListener(animation -> {
                if (listener != null && animatedView.getWidth() > 0) {
                    float slideOffset = animatedView.getTranslationX() / animatedView.getWidth();
                    listener.onPanelSliding(Math.max(0f, Math.min(1f, slideOffset)));
                }
            })
            .withEndAction(() -> {
                isOpen = false;
                setVisibility(GONE);
                
                // Вызываем callback ПОСЛЕ установки флага
                if (listener != null) {
                    listener.onPanelClosed();
                }
            })
            .start();
    }
    
    public boolean isOpen() {
        return isOpen;
    }
    
    public void setPanelOpen(boolean open) {
        if (open) {
            openPanel();
        } else {
            closePanel();
        }
    }
}
