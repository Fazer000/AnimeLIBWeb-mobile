package com.example.animelib.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Html;
import android.text.Spanned;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.animelib.R;

public class SpoilerView extends LinearLayout {
    private TextView spoilerButton;
    private TextView spoilerContent;
    private boolean isExpanded = false;
    private String spoilerTitle;
    private String spoilerText;

    public SpoilerView(Context context) {
        super(context);
        init();
    }

    public SpoilerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SpoilerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
        
        // Создаем кнопку спойлера
        spoilerButton = new TextView(getContext());
        spoilerButton.setPadding(16, 12, 16, 12);
        spoilerButton.setTextSize(13);
        spoilerButton.setTextColor(getContext().getColor(R.color.white_color));
        spoilerButton.setBackground(createSpoilerButtonBackground());
        spoilerButton.setOnClickListener(v -> toggleSpoiler());
        
        // Создаем контент спойлера
        spoilerContent = new TextView(getContext());
        spoilerContent.setPadding(16, 8, 16, 8);
        spoilerContent.setTextSize(13);
        spoilerContent.setTextColor(getContext().getColor(R.color.white_color));
        spoilerContent.setBackground(createSpoilerContentBackground());
        
        addView(spoilerButton);
        addView(spoilerContent);
        
        // Изначально скрываем контент
        spoilerContent.setVisibility(GONE);
    }

    private GradientDrawable createSpoilerButtonBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(8);
        drawable.setColor(Color.parseColor("#FF6B6B")); // Красноватый цвет для спойлера
        return drawable;
    }

    private GradientDrawable createSpoilerContentBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(8);
        drawable.setColor(Color.parseColor("#2C2C2C")); // Темный фон для контента
        return drawable;
    }

    public void setSpoilerData(String title, String text) {
        this.spoilerTitle = title;
        this.spoilerText = text;
        
        // Устанавливаем текст кнопки
        String buttonText = (title != null && !title.trim().isEmpty()) ? title : "Спойлер";
        spoilerButton.setText(buttonText + " ▼");
        
        // Устанавливаем текст контента
        Spanned spannedText = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY);
        spoilerContent.setText(spannedText);
    }

    private void toggleSpoiler() {
        isExpanded = !isExpanded;
        
        if (isExpanded) {
            spoilerContent.setVisibility(VISIBLE);
            String buttonText = (spoilerTitle != null && !spoilerTitle.trim().isEmpty()) ? spoilerTitle : "Спойлер";
            spoilerButton.setText(buttonText + " ▲");
        } else {
            spoilerContent.setVisibility(GONE);
            String buttonText = (spoilerTitle != null && !spoilerTitle.trim().isEmpty()) ? spoilerTitle : "Спойлер";
            spoilerButton.setText(buttonText + " ▼");
        }
    }
}

