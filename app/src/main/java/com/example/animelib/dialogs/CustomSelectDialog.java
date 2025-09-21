package com.example.animelib.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.animelib.R;
import com.google.android.material.button.MaterialButton;

import java.util.List;

/**
 * Кастомный диалог для замены стандартных HTML select элементов
 * Использует тот же стиль что и диалог сортировки комментариев
 */
public class CustomSelectDialog {
    
    private static final String TAG = "CustomSelectDialog";
    
    private Context context;
    private Dialog dialog;
    private OnOptionSelectedListener listener;
    
    /**
     * Интерфейс для обработки выбора опции
     */
    public interface OnOptionSelectedListener {
        void onOptionSelected(String value, String text);
    }
    
    /**
     * Конструктор
     * @param context Контекст приложения
     */
    public CustomSelectDialog(Context context) {
        this.context = context;
    }
    
    /**
     * Показать диалог выбора опций
     * @param title Заголовок диалога
     * @param options Список опций (текст)
     * @param values Список значений (value)
     * @param currentValue Текущее выбранное значение
     * @param listener Обработчик выбора
     */
    public void show(String title, List<String> options, List<String> values, String currentValue, OnOptionSelectedListener listener) {
        if (options == null || values == null || options.size() != values.size()) {
            Log.e(TAG, "Invalid options or values provided");
            return;
        }
        
        this.listener = listener;
        
        // Создаем диалог
        dialog = new Dialog(context);
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
            // Применяем параметры
            window.setAttributes(params);
        }
        
        // Находим элементы
        TextView titleView = dialog.findViewById(R.id.dialog_title);
        titleView.setText(title != null ? title : "Выберите опцию");
        
        LinearLayout optionsLayout = dialog.findViewById(R.id.options_layout);
        
        // Очищаем предыдущие опции
        optionsLayout.removeAllViews();
        
        // Создаем варианты
        for (int i = 0; i < options.size(); i++) {
            String optionText = options.get(i);
            String optionValue = values.get(i);
            
            MaterialButton button = createOptionButton(optionText, optionValue, currentValue);
            optionsLayout.addView(button);
        }
        
        dialog.show();
        Log.d(TAG, "Custom select dialog shown with " + options.size() + " options");
    }
    
    /**
     * Создать кнопку опции
     * @param text Текст кнопки
     * @param value Значение кнопки
     * @param currentValue Текущее выбранное значение
     * @return MaterialButton
     */
    private MaterialButton createOptionButton(String text, String value, String currentValue) {
        MaterialButton button = new MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        button.setText(text);
        
        // Получаем цвета из темы
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.primaryTextColor, typedValue, true);
        int primaryTextColor = typedValue.data;
        
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true);
        android.graphics.drawable.Drawable selectableItemBackground = 
            ContextCompat.getDrawable(context, typedValue.resourceId);
        
        // Настройка стиля кнопки
        button.setTextColor(primaryTextColor);
        button.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        button.setAllCaps(false);
        button.setStrokeWidth(0);
        button.setLineHeight(0);
        button.setForeground(selectableItemBackground);
        button.setCornerRadius(0);
        button.setLetterSpacing(0.0f);
        button.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        button.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(40) // Фиксированная высота
        ));

        
        // Выделяем текущую выбранную опцию
        if (value != null && value.equals(currentValue)) {
            button.setTextColor(context.getResources().getColor(R.color.dt_secondary_text_color)); // Полупрозрачный фон для выбранной опции
            Log.d(TAG, "Highlighting current option: " + text);
        }
        
        // Обработчик клика
        button.setOnClickListener(v -> {
            if (listener != null) {
                listener.onOptionSelected(value, text);
            }
            dialog.dismiss();
            Log.d(TAG, "Option selected: " + text + " (value: " + value + ")");
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
     * Закрыть диалог
     */
    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
    
    /**
     * Проверить показывается ли диалог
     * @return true если диалог показывается
     */
    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }
}
