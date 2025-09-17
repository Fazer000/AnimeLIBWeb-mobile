package com.example.animelib.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.animelib.R;

/**
 * Компонент для отображения спойлеров в комментариях
 */
public class SpoilerView extends LinearLayout {
    
    private TextView titleView;
    private TextView contentView;
    private View lineView;
    private boolean isExpanded = false;
    private String spoilerTitle;
    private String spoilerContent;
    
    public SpoilerView(Context context) {
        super(context);
        init();
    }
    
    public SpoilerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public SpoilerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        setOrientation(VERTICAL);
//        setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        
        // Устанавливаем фон с скругленными углами
        setBackgroundResource(R.drawable.spoiler_background_light);
        
        // Добавляем скругленные углы (если поддерживается)
        setElevation(dpToPx(2));

        // Создаем заголовок спойлера
        titleView = new TextView(getContext());
        titleView.setTextColor(ContextCompat.getColor(getContext(), R.color.dt_secondary_text_color));
        titleView.setTextSize(13);
        titleView.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        titleView.setText("Спойлер");
        titleView.setOnClickListener(v -> toggleExpanded());
        
        // Добавляем отступы для заголовка
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleView.setLayoutParams(titleParams);
        
        addView(titleView);

        lineView = new View(getContext());
        lineView.setBackgroundResource(R.color.dt_line_color);
        lineView.setVisibility(View.GONE);

        int height = getResources().getDimensionPixelSize(R.dimen.line_height);

        // Добавляем отступы для заголовка
        LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                height
        );
        lineView.setLayoutParams(lineParams);

        addView(lineView);

        // Создаем содержимое спойлера
        contentView = new TextView(getContext());
        contentView.setTextColor(ContextCompat.getColor(getContext(), R.color.dt_primary_text_color));
        contentView.setTextSize(13);
        contentView.setLineSpacing(dpToPx(2), 1.0f);
        contentView.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        contentView.setVisibility(GONE);
        
        // Добавляем отступы для содержимого
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        contentView.setLayoutParams(contentParams);
        
        addView(contentView);
        
        // Добавляем отступы для всего контейнера
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dpToPx(8), 0, dpToPx(8));
        setLayoutParams(params);
    }
    
    /**
     * Установить данные спойлера
     * @param title Заголовок спойлера
     * @param content Содержимое спойлера
     */
    @SuppressLint("SetTextI18n")
    public void setSpoilerData(String title, String content) {
        this.spoilerTitle = title;
        this.spoilerContent = content;

        if (isExpanded) {
            titleView.setText(title);
        } else {
            titleView.setText(title);
        }

        if (content != null) {
            contentView.setText(content);
        }
    }
    
    /**
     * Переключить состояние спойлера
     */
    @SuppressLint("SetTextI18n")
    private void toggleExpanded() {
        isExpanded = !isExpanded;
        
        if (isExpanded) {
            // Плавное появление содержимого
            lineView.setVisibility(View.VISIBLE);
            contentView.setVisibility(VISIBLE);
            contentView.setAlpha(0f);
            contentView.animate()
                .alpha(1f)
                .setDuration(200)
                .start();

            titleView.setText(spoilerTitle);

            // Анимация заголовка
            titleView.animate()
                .scaleX(1.02f)
                .scaleY(1.02f)
                .setDuration(100)
                .withEndAction(() -> titleView.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start())
                .start();
        } else {
            lineView.setVisibility(View.GONE);
            // Плавное скрытие содержимого
            contentView.animate()
                .alpha(0f)
                .setDuration(150)
                .withEndAction(() -> contentView.setVisibility(GONE))
                .start();

            // Обновляем заголовок - показываем название спойлера
            titleView.setText(spoilerTitle);
        }
    }
    
    /**
     * Преобразовать dp в px
     */
    private int dpToPx(int dp) {
        return (int) (dp * getContext().getResources().getDisplayMetrics().density);
    }
}