package com.example.animelib;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;

public class UrlInputDialog extends Dialog {
    private EditText urlEditText;
    private Button saveButton;
    private Button cancelButton;
    private TextView errorTextView;
    private OnUrlSaveListener listener;

    public interface OnUrlSaveListener {
        void onUrlSaved(String url);
    }

    public UrlInputDialog(@NonNull Context context, OnUrlSaveListener listener) {
        super(context, R.style.Theme_AnimeLIB_Dialog);
        this.listener = listener;
        initDialog();
    }

    private void initDialog() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_url_input, null);
        setContentView(view);

        // Получаем ссылки на элементы интерфейса
        urlEditText = view.findViewById(R.id.urlEditText);
        saveButton = view.findViewById(R.id.saveButton);
        cancelButton = view.findViewById(R.id.cancelButton);
        errorTextView = view.findViewById(R.id.errorTextView);

        // Настройки диалога
        setCancelable(false);
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // Обработчик кнопки "Сохранить"
        saveButton.setOnClickListener(v -> {
            String url = urlEditText.getText().toString().trim();

            if (url.isEmpty()) {
                errorTextView.setText("❌ URL не может быть пустым");
                errorTextView.setVisibility(View.VISIBLE);
                return;
            }

            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }

            errorTextView.setVisibility(View.GONE);

            if (listener != null) {
                listener.onUrlSaved(url);
                dismiss();
            }
        });

        // Обработчик кнопки "Отмена"
        cancelButton.setOnClickListener(v -> {
            dismiss();
        });

        // Скрываем ошибку при начале ввода
        urlEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                errorTextView.setVisibility(View.GONE);
            }
        });
    }
}
