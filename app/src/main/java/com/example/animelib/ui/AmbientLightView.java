package com.example.animelib.ui;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Custom View для отображения ambient-подсветки вокруг видео.
 * Рисует градиенты с цветами краев видео для создания эффекта ambient light.
 */
public class AmbientLightView extends View {
    private static final String TAG = "AmbientLightView";
    
    // Интенсивность blur-эффекта (больше = больше размытие)
    private static final float BLUR_INTENSITY = 1f;
    
    // Длительность анимации смены цвета
    private static final long COLOR_TRANSITION_DURATION = 1000;
    
    // Количество источников света на каждой стороне (синхронизировано с AmbientLightManager)
    private static final int LIGHTS_PER_SIDE = 3;
    
    // Отступ от края видео (в dp) - источники света прямо у края видео
    private static final int LIGHT_OFFSET_DP = -10;
    
    // Радиус распространения света (в dp) - больше радиус = более размытые границы
    private static final int LIGHT_RADIUS_DP = 300;
    
    private final Paint lightPaint;
    private final View playerView;

    // Массивы текущих цветов для каждой стороны
    private final int[] currentLeftColors = new int[LIGHTS_PER_SIDE];
    private final int[] currentTopColors = new int[LIGHTS_PER_SIDE];
    private final int[] currentRightColors = new int[LIGHTS_PER_SIDE];
    private final int[] currentBottomColors = new int[LIGHTS_PER_SIDE];
    
    // Массивы целевых цветов для плавной анимации
    private final int[] targetLeftColors = new int[LIGHTS_PER_SIDE];
    private final int[] targetTopColors = new int[LIGHTS_PER_SIDE];
    private final int[] targetRightColors = new int[LIGHTS_PER_SIDE];
    private final int[] targetBottomColors = new int[LIGHTS_PER_SIDE];
    
    private final ArgbEvaluator colorEvaluator;
    private ValueAnimator colorAnimator;
    private ValueAnimator intensityAnimator;
    private final int lightOffset;
    private final float lightRadius;
    private float currentIntensity = 1.0f; // Текущая интенсивность подсветки (0.0 - 1.0)

    public AmbientLightView(Context context) {
        this(context, null);
    }

    public AmbientLightView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AmbientLightView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        
        lightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        lightPaint.setDither(true); // Улучшает качество градиентов
        colorEvaluator = new ArgbEvaluator();
        
        // Конвертируем dp в pixels
        float density = context.getResources().getDisplayMetrics().density;
        lightOffset = (int) (LIGHT_OFFSET_DP * density);
        lightRadius = LIGHT_RADIUS_DP * density;
        
        // Находим PlayerView для определения границ видео
        playerView = null; // будет установлено через setPlayerView()

        // Прозрачный фон
        setBackgroundColor(Color.TRANSPARENT);
        
        // Hardware acceleration для лучшей производительности
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }
    
    /**
     * Устанавливает PlayerView для определения границ видео
     */
    public void setPlayerView(View playerView) {
        // Сохраняем ссылку для использования в onDraw
    }

    /**
     * Устанавливает новые цвета для ambient эффекта с плавной анимацией
     */
    public void setColors(int[] leftColors, int[] topColors, int[] rightColors, int[] bottomColors) {
        for (int i = 0; i < LIGHTS_PER_SIDE; i++) {
            targetLeftColors[i] = addAlpha(leftColors[i]);
            targetTopColors[i] = addAlpha(topColors[i]);
            targetRightColors[i] = addAlpha(rightColors[i]);
            targetBottomColors[i] = addAlpha(bottomColors[i]);
        }
        
        animateToTargetColors();
    }

    /**
     * Анимирует переход от текущих цветов к целевым
     */
    private void animateToTargetColors() {
        if (colorAnimator != null && colorAnimator.isRunning()) {
            colorAnimator.cancel();
        }
        
        // Проверяем что colors действительно изменились (оптимизация)
        boolean hasChanges = false;
        for (int i = 0; i < LIGHTS_PER_SIDE; i++) {
            if (currentLeftColors[i] != targetLeftColors[i] || 
                currentTopColors[i] != targetTopColors[i] ||
                currentRightColors[i] != targetRightColors[i] ||
                currentBottomColors[i] != targetBottomColors[i]) {
                hasChanges = true;
                break;
            }
        }
        
        if (!hasChanges) {
            return; // Ничего не изменилось, не запускаем анимацию
        }
        
        colorAnimator = ValueAnimator.ofFloat(0f, 1f);
        colorAnimator.setDuration(COLOR_TRANSITION_DURATION);
        colorAnimator.setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f));
        
        // Сохраняем начальные цвета
        final int[] startLeftColors = currentLeftColors.clone();
        final int[] startTopColors = currentTopColors.clone();
        final int[] startRightColors = currentRightColors.clone();
        final int[] startBottomColors = currentBottomColors.clone();
        
        colorAnimator.addUpdateListener(animation -> {
            float fraction = animation.getAnimatedFraction();
            
            for (int i = 0; i < LIGHTS_PER_SIDE; i++) {
                currentLeftColors[i] = (int) colorEvaluator.evaluate(fraction, startLeftColors[i], targetLeftColors[i]);
                currentTopColors[i] = (int) colorEvaluator.evaluate(fraction, startTopColors[i], targetTopColors[i]);
                currentRightColors[i] = (int) colorEvaluator.evaluate(fraction, startRightColors[i], targetRightColors[i]);
                currentBottomColors[i] = (int) colorEvaluator.evaluate(fraction, startBottomColors[i], targetBottomColors[i]);
            }
            
            postInvalidateOnAnimation(); // Оптимизированный invalidate
        });
        
        colorAnimator.start();
    }

    /**
     * Добавляет прозрачность к цвету для эффекта ambient
     */
    private int addAlpha(int color) {
        int alpha = (int) (255 * BLUR_INTENSITY);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        
        if (getWidth() == 0 || getHeight() == 0) {
            return;
        }
        
        // Получаем размеры экрана
        int screenWidth = getWidth();
        int screenHeight = getHeight();
        
        // Вычисляем границы видео (центрированное, aspect ratio 16:9)
        float videoAspect = 16f / 9f;
        float screenAspect = (float) screenWidth / screenHeight;
        
        int videoLeft, videoTop, videoRight, videoBottom;
        
        if (screenAspect > videoAspect) {
            // Экран шире - черные полосы слева и справа
            int videoWidth = (int) (screenHeight * videoAspect);
            videoLeft = (screenWidth - videoWidth) / 2;
            videoTop = 0;
            videoRight = videoLeft + videoWidth;
            videoBottom = screenHeight;
        } else {
            // Экран выше - черные полосы сверху и снизу
            int videoHeight = (int) (screenWidth / videoAspect);
            videoLeft = 0;
            videoTop = (screenHeight - videoHeight) / 2;
            videoRight = screenWidth;
            videoBottom = videoTop + videoHeight;
        }
        
        // Рисуем источники света только по бокам (слева и справа)
        drawLeftLights(canvas, videoLeft, videoTop, videoBottom);
        drawRightLights(canvas, videoRight, videoTop, videoBottom);
    }

    /**
     * Рисует источники света вдоль левого края видео
     */
    private void drawLeftLights(Canvas canvas, int videoLeft, int videoTop, int videoBottom) {
        int videoHeight = videoBottom - videoTop;
        float segmentHeight = videoHeight / (float) LIGHTS_PER_SIDE;
        
        for (int i = 0; i < LIGHTS_PER_SIDE; i++) {
            float centerY = videoTop + (i + 0.5f) * segmentHeight;
            float centerX = videoLeft - lightOffset;
            
            // Создаем мягкий градиент с несколькими точками для плавного перехода
            int[] colors = new int[] {
                currentLeftColors[i],
                adjustAlpha(currentLeftColors[i], 0.85f),
                adjustAlpha(currentLeftColors[i], 0.5f),
                Color.TRANSPARENT
            };
            float[] positions = new float[] { 0f, 0.25f, 0.55f, 1f };
            
            RadialGradient gradient = new RadialGradient(
                    centerX, centerY, lightRadius,
                    colors, positions,
                    Shader.TileMode.CLAMP
            );
            
            lightPaint.setShader(gradient);
            canvas.drawCircle(centerX, centerY, lightRadius, lightPaint);
        }
    }
    
    /**
     * Рисует источники света вдоль верхнего края видео
     */
    private void drawTopLights(Canvas canvas, int videoLeft, int videoTop, int videoRight) {
        int videoWidth = videoRight - videoLeft;
        float segmentWidth = videoWidth / (float) LIGHTS_PER_SIDE;
        
        for (int i = 0; i < LIGHTS_PER_SIDE; i++) {
            float centerX = videoLeft + (i + 0.5f) * segmentWidth;
            float centerY = videoTop - lightOffset;
            
            int[] colors = new int[] {
                currentTopColors[i],
                adjustAlpha(currentTopColors[i], 0.7f),
                adjustAlpha(currentTopColors[i], 0.3f),
                Color.TRANSPARENT
            };
            float[] positions = new float[] { 0f, 0.3f, 0.6f, 1f };
            
            RadialGradient gradient = new RadialGradient(
                    centerX, centerY, lightRadius,
                    colors, positions,
                    Shader.TileMode.CLAMP
            );
            
            lightPaint.setShader(gradient);
            canvas.drawCircle(centerX, centerY, lightRadius, lightPaint);
        }
    }
    
    /**
     * Рисует источники света вдоль правого края видео
     */
    private void drawRightLights(Canvas canvas, int videoRight, int videoTop, int videoBottom) {
        int videoHeight = videoBottom - videoTop;
        float segmentHeight = videoHeight / (float) LIGHTS_PER_SIDE;
        
        for (int i = 0; i < LIGHTS_PER_SIDE; i++) {
            float centerY = videoTop + (i + 0.5f) * segmentHeight;
            float centerX = videoRight + lightOffset;
            
            int[] colors = new int[] {
                currentRightColors[i],
                adjustAlpha(currentRightColors[i], 0.85f),
                adjustAlpha(currentRightColors[i], 0.5f),
                Color.TRANSPARENT
            };
            float[] positions = new float[] { 0f, 0.25f, 0.55f, 1f };
            
            RadialGradient gradient = new RadialGradient(
                    centerX, centerY, lightRadius,
                    colors, positions,
                    Shader.TileMode.CLAMP
            );
            
            lightPaint.setShader(gradient);
            canvas.drawCircle(centerX, centerY, lightRadius, lightPaint);
        }
    }
    
    /**
     * Рисует источники света вдоль нижнего края видео
     */
    private void drawBottomLights(Canvas canvas, int videoLeft, int videoBottom, int videoRight) {
        int videoWidth = videoRight - videoLeft;
        float segmentWidth = videoWidth / (float) LIGHTS_PER_SIDE;
        
        for (int i = 0; i < LIGHTS_PER_SIDE; i++) {
            float centerX = videoLeft + (i + 0.5f) * segmentWidth;
            float centerY = videoBottom + lightOffset;
            
            int[] colors = new int[] {
                currentBottomColors[i],
                adjustAlpha(currentBottomColors[i], 0.7f),
                adjustAlpha(currentBottomColors[i], 0.3f),
                Color.TRANSPARENT
            };
            float[] positions = new float[] { 0f, 0.3f, 0.6f, 1f };
            
            RadialGradient gradient = new RadialGradient(
                    centerX, centerY, lightRadius,
                    colors, positions,
                    Shader.TileMode.CLAMP
            );
            
            lightPaint.setShader(gradient);
            canvas.drawCircle(centerX, centerY, lightRadius, lightPaint);
        }
    }
    
    /**
     * Изменяет альфа-канал цвета
     */
    private int adjustAlpha(int color, float factor) {
        int alpha = Color.alpha(color);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        // Учитываем текущую интенсивность подсветки
        return Color.argb((int)(alpha * factor * currentIntensity), red, green, blue);
    }
    
    /**
     * Плавно изменяет интенсивность подсветки (для эффекта затухания на паузе)
     * @param targetIntensity целевая интенсивность от 0.0 (выключено) до 1.0 (полная яркость)
     */
    public void dimToIntensity(float targetIntensity) {
        if (intensityAnimator != null && intensityAnimator.isRunning()) {
            intensityAnimator.cancel();
        }
        
        final float startIntensity = currentIntensity;
        
        intensityAnimator = ValueAnimator.ofFloat(startIntensity, targetIntensity);
        intensityAnimator.setDuration(500); // 500ms для плавного перехода
        intensityAnimator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        
        intensityAnimator.addUpdateListener(animation -> {
            currentIntensity = (float) animation.getAnimatedValue();
            postInvalidateOnAnimation(); // Оптимизированный invalidate для анимации
        });
        
        intensityAnimator.start();
    }
    
    /**
     * Приостанавливает все анимации (для UI interactions)
     */
    public void pauseAnimations() {
        if (colorAnimator != null && colorAnimator.isRunning()) {
            colorAnimator.pause();
        }
        if (intensityAnimator != null && intensityAnimator.isRunning()) {
            intensityAnimator.pause();
        }
    }
    
    /**
     * Возобновляет приостановленные анимации
     */
    public void resumeAnimations() {
        if (colorAnimator != null && colorAnimator.isPaused()) {
            colorAnimator.resume();
        }
        if (intensityAnimator != null && intensityAnimator.isPaused()) {
            intensityAnimator.resume();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (colorAnimator != null) {
            colorAnimator.cancel();
            colorAnimator = null;
        }
        if (intensityAnimator != null) {
            intensityAnimator.cancel();
            intensityAnimator = null;
        }
    }
}

