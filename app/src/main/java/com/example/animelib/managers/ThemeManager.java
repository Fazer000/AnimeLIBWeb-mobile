package com.example.animelib.managers;

import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.animelib.R;
import com.example.animelib.api.ApiService;
import com.example.animelib.util.ThemeUtils;
import com.google.android.material.button.MaterialButton;

/**
 * Менеджер для управления темами приложения
 * Обеспечивает показ диалога выбора темы, сохранение и применение настроек
 */
public class ThemeManager {
    
    private static final String TAG = "ThemeManager";
    
    private final Context context;
    private final ApiService apiService;
    
    /**
     * Конструктор ThemeManager
     * @param context Контекст приложения
     * @param apiService Сервис для API запросов
     */
    public ThemeManager(Context context, ApiService apiService) {
        this.context = context;
        this.apiService = apiService;
    }
    
    /**
     * Показывает диалог выбора темы
     */
    public void showThemeDialog() {
        Log.d(TAG, "Showing theme dialog");
        
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.custom_select_dialog);

        // Настраиваем окно
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setGravity(Gravity.BOTTOM);
            // Получаем текущие параметры окна
            WindowManager.LayoutParams params = window.getAttributes();
            // Устанавливаем match_parent по ширине
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
        }

        // Находим элементы
        TextView title = dialog.findViewById(R.id.dialog_title);
        title.setText("Выберите тему");

        LinearLayout optionsLayout = dialog.findViewById(R.id.options_layout);

        // Создаем варианты темы
        String[] themeOptions = {"Светлая", "Тёмная", "Авто"};
        int[] themeValues = {0, 1, 2};

        // Добавляем варианты
        for (int i = 0; i < themeOptions.length; i++) {
            MaterialButton button = createThemeButton(themeOptions[i], themeValues[i], dialog);
            optionsLayout.addView(button);
        }

        dialog.show();
    }
    
    /**
     * Создает кнопку для выбора темы
     * @param themeName Название темы
     * @param themeValue Значение темы
     * @param dialog Диалог для закрытия
     * @return MaterialButton
     */
    private MaterialButton createThemeButton(String themeName, int themeValue, Dialog dialog) {
        MaterialButton button = new MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        button.setText(themeName);

        // Получаем цвета из темы
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.primaryTextColor, typedValue, true);
        int primaryTextColor = typedValue.data;

        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true);
        android.graphics.drawable.Drawable selectableItemBackground = 
            ContextCompat.getDrawable(context, typedValue.resourceId);

        button.setTextColor(primaryTextColor);
        button.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        button.setStrokeWidth(0);
        button.setForeground(selectableItemBackground);
        button.setCornerRadius(0);
        button.setLetterSpacing(0.0f);
        button.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        button.setAllCaps(false); // Отключаем капс
        
        button.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(48) // Фиксированная высота
        ));

        // Обработчик клика
        button.setOnClickListener(v -> {
            Log.d(TAG, "Theme selected: " + themeValue);
            
            // Сохраняем в базу данных
            apiService.saveThemeSetting(themeValue);
            Log.d(TAG, "Theme saved to database: " + themeValue);
            
            // Применяем тему
            ThemeUtils.applyTheme(themeValue);
            Log.d(TAG, "Theme applied: " + themeValue);
            
            // Сохраняем в SharedPreferences для немедленного применения
            ThemeUtils.saveThemePreference(context, themeValue);
            Log.d(TAG, "Theme saved to SharedPreferences: " + themeValue);
            
            // Перезагружаем активность
            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).recreate();
            }
            
            dialog.dismiss();
        });
        
        return button;
    }
    
    /**
     * Вспомогательный метод для преобразования dp в px
     */
    private int dpToPx(int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }
    
    /**
     * Загружает и применяет тему при запуске приложения
     */
    public void loadAndApplyTheme() {
        Log.d(TAG, "Loading and applying theme");
        
        try {
            // Получаем тему из базы данных
            int themeMode = apiService.loadThemeSetting();
            Log.d(TAG, "Loaded theme from database: " + themeMode);
            
            // Также проверяем SharedPreferences
            int sharedPrefTheme = ThemeUtils.getSavedThemePreference(context);
            Log.d(TAG, "Loaded theme from SharedPreferences: " + sharedPrefTheme);
            
            // Используем тему из базы данных, если она есть, иначе из SharedPreferences
            int finalTheme = themeMode != 0 ? themeMode : sharedPrefTheme;
            
            // Применяем тему в главном потоке
            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).runOnUiThread(() -> {
                    ThemeUtils.applyTheme(finalTheme);
                    Log.d(TAG, "Theme applied on startup: " + finalTheme);
                });
            } else {
                ThemeUtils.applyTheme(finalTheme);
                Log.d(TAG, "Theme applied on startup: " + finalTheme);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to load and apply theme", e);
            // Применяем тему по умолчанию в главном потоке
            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).runOnUiThread(() -> {
                    ThemeUtils.applyTheme(0);
                });
            } else {
                ThemeUtils.applyTheme(0);
            }
        }
    }
    
    /**
     * Получает текущую тему
     * @return Значение текущей темы
     */
    public int getCurrentTheme() {
        try {
            return apiService.loadThemeSetting();
        } catch (Exception e) {
            Log.e(TAG, "Failed to get current theme", e);
            return ThemeUtils.getSavedThemePreference(context);
        }
    }
    
    /**
     * Сохраняет тему
     * @param themeMode Значение темы для сохранения
     */
    public void saveTheme(int themeMode) {
        Log.d(TAG, "Saving theme: " + themeMode);
        
        // Сохраняем в базу данных
        apiService.saveThemeSetting(themeMode);
        
        // Сохраняем в SharedPreferences
        ThemeUtils.saveThemePreference(context, themeMode);
        
        Log.d(TAG, "Theme saved successfully");
    }
    
    /**
     * Применяет тему
     * @param themeMode Значение темы для применения
     */
    public void applyTheme(int themeMode) {
        Log.d(TAG, "Applying theme: " + themeMode);
        ThemeUtils.applyTheme(themeMode);
        
        // Перезагружаем активность если это возможно
        if (context instanceof android.app.Activity) {
            ((android.app.Activity) context).recreate();
        }
    }
}
