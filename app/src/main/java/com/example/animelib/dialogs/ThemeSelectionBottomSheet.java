package com.example.animelib.dialogs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.example.animelib.R;
import com.example.animelib.util.ThemeUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import android.widget.FrameLayout;
import android.view.ViewGroup;
import android.util.TypedValue;

import java.util.Objects;

public class ThemeSelectionBottomSheet extends BottomSheetDialog {
    
    public interface OnThemeChangedListener {
        void onThemeChanged(int themeMode);
    }
    
    public interface OnBackPressedListener {
        void onBackPressed();
    }
    
    private OnThemeChangedListener listener;
    private OnBackPressedListener onBackPressedListener;
    private int currentTheme;
    
    private ImageView themeAutoCheck;
    private ImageView themeLightCheck;
    private ImageView themeDarkCheck;
    
    public ThemeSelectionBottomSheet(@NonNull Context context, int currentTheme, OnThemeChangedListener listener) {
        super(context, com.google.android.material.R.style.ThemeOverlay_Material3_BottomSheetDialog);
        this.currentTheme = currentTheme;
        this.listener = listener;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View view = LayoutInflater.from(getContext()).inflate(R.layout.bs_theme_selection, null);
        setContentView(view);
        Objects.requireNonNull(getWindow()).setLayout(1000, ViewGroup.LayoutParams.WRAP_CONTENT);

        // Expand like YouTube
        setDismissWithAnimation(true);
        try {
            setOnShowListener(d -> {
                FrameLayout bottom = findViewById(com.google.android.material.R.id.design_bottom_sheet);
                if (bottom != null) {
                    BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottom);
                    behavior.setFitToContents(true);
                    behavior.setSkipCollapsed(true);
                    behavior.setExpandedOffset(0);
                    behavior.setState(BottomSheetBehavior.STATE_EXPANDED);

                    // Reduce width via side margins
                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) bottom.getLayoutParams();
                    int side = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getContext().getResources().getDisplayMetrics());
                    lp.leftMargin = side;
                    lp.rightMargin = side;
                    bottom.setLayoutParams(lp);
                }
            });
        } catch (Throwable ignored) {}

        // Находим элементы
        ImageButton closeButton = view.findViewById(R.id.bs_theme_back);
        LinearLayout themeAutoOption = view.findViewById(R.id.themeAutoOption);
        LinearLayout themeLightOption = view.findViewById(R.id.themeLightOption);
        LinearLayout themeDarkOption = view.findViewById(R.id.themeDarkOption);
        
        themeAutoCheck = view.findViewById(R.id.themeAutoCheck);
        themeLightCheck = view.findViewById(R.id.themeLightCheck);
        themeDarkCheck = view.findViewById(R.id.themeDarkCheck);
        
        // Устанавливаем текущую тему
        updateThemeSelection();
        
        // Обработчики кликов
        closeButton.setOnClickListener(v -> dismiss());
        
        themeAutoOption.setOnClickListener(v -> selectTheme(ThemeUtils.THEME_SYSTEM));
        themeLightOption.setOnClickListener(v -> selectTheme(ThemeUtils.THEME_LIGHT));
        themeDarkOption.setOnClickListener(v -> selectTheme(ThemeUtils.THEME_DARK));
        
        // Закрытие при клике вне диалога
        setCancelable(true);
        setCanceledOnTouchOutside(true);
    }
    
    private void selectTheme(int themeMode) {
        if (themeMode != currentTheme) {
            currentTheme = themeMode;
            updateThemeSelection();
            
            // Применяем тему
            ThemeUtils.applyTheme(themeMode);
            // Сохранение в Room базу данных будет выполнено в VideoPlayerActivity
            
            // Уведомляем слушателя
            if (listener != null) {
                listener.onThemeChanged(themeMode);
            }
        }
        dismiss();
    }
    
    private void updateThemeSelection() {
        // Скрываем все галочки
        themeAutoCheck.setVisibility(View.GONE);
        themeLightCheck.setVisibility(View.GONE);
        themeDarkCheck.setVisibility(View.GONE);
        
        // Показываем галочку для текущей темы
        switch (currentTheme) {
            case ThemeUtils.THEME_SYSTEM:
                themeAutoCheck.setVisibility(View.VISIBLE);
                break;
            case ThemeUtils.THEME_LIGHT:
                themeLightCheck.setVisibility(View.VISIBLE);
                break;
            case ThemeUtils.THEME_DARK:
                themeDarkCheck.setVisibility(View.VISIBLE);
                break;
        }
    }
    
    public int getCurrentTheme() {
        return currentTheme;
    }
    
    public String getCurrentThemeText() {
        switch (currentTheme) {
            case ThemeUtils.THEME_SYSTEM:
                return "Авто";
            case ThemeUtils.THEME_LIGHT:
                return "Светлая";
            case ThemeUtils.THEME_DARK:
                return "Темная";
            default:
                return "Авто";
        }
    }
    
    public void setOnBackPressedListener(OnBackPressedListener listener) {
        this.onBackPressedListener = listener;
    }
    
    @Override
    public void onBackPressed() {
        if (onBackPressedListener != null) {
            onBackPressedListener.onBackPressed();
        } else {
            super.onBackPressed();
        }
    }
}
