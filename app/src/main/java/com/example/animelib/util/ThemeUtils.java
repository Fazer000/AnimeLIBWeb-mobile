package com.example.animelib.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemeUtils {
    public static final String THEME_PREFERENCE = "theme_preference";
    public static final String THEME_MODE = "theme_mode";
    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;
    public static final int THEME_SYSTEM = 2;

    public static void applyTheme(int themeMode) {
        switch (themeMode) {
            case THEME_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case THEME_SYSTEM:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }
    
    /**
     * Применяет тему к конкретной активности без перезагрузки
     * @param activity Активность для применения темы
     * @param themeMode Режим темы
     */
    public static void applyThemeToActivity(Activity activity, int themeMode) {
        // Сохраняем тему для будущих активностей
        saveThemePreference(activity, themeMode);
        
        // Принудительно обновляем текущую активность БЕЗ пересоздания
        if (activity instanceof androidx.appcompat.app.AppCompatActivity) {
            androidx.appcompat.app.AppCompatDelegate delegate = 
                ((androidx.appcompat.app.AppCompatActivity) activity).getDelegate();
            
            // Устанавливаем локальную тему для этой активности
            delegate.setLocalNightMode(getAppCompatNightMode(themeMode));
            
            // Принудительно обновляем UI
            delegate.applyDayNight();
        }
    }
    
    /**
     * Преобразует режим темы в AppCompat режим
     */
    private static int getAppCompatNightMode(int themeMode) {
        switch (themeMode) {
            case THEME_LIGHT:
                return AppCompatDelegate.MODE_NIGHT_NO;
            case THEME_DARK:
                return AppCompatDelegate.MODE_NIGHT_YES;
            default:
                return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
    }

    public static void saveThemePreference(Context context, int themeMode) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putInt(THEME_MODE, themeMode).apply();
    }

    public static int getSavedThemePreference(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getInt(THEME_MODE, THEME_SYSTEM); // по умолчанию системная тема
    }
}