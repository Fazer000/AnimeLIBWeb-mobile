package com.example.animelib.dialogs;

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
 * Диалог для выбора темы приложения
 */
public class ThemeSelectionDialog {
    
    private static final String TAG = "ThemeSelectionDialog";
    
    private final Context context;
    private final ApiService apiService;
    
    /**
     * Конструктор ThemeSelectionDialog
     * @param context Контекст приложения
     * @param apiService Сервис для API запросов
     */
    public ThemeSelectionDialog(Context context, ApiService apiService) {
        this.context = context;
        this.apiService = apiService;
    }
    
    /**
     * Показывает диалог выбора темы
     */
    public void show() {
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
            
            // Сохраняем в SharedPreferences для немедленного применения
            ThemeUtils.saveThemePreference(context, themeValue);
            Log.d(TAG, "Theme saved to SharedPreferences: " + themeValue);
            
            // Применяем тему без перезагрузки активности
            if (context instanceof android.app.Activity) {
                ThemeUtils.applyThemeToActivity((android.app.Activity) context, themeValue);
            } else {
                ThemeUtils.applyTheme(themeValue);
            }
            Log.d(TAG, "Theme applied: " + themeValue);
            
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
}
