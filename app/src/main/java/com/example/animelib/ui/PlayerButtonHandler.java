package com.example.animelib.ui;

import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;

import com.example.animelib.VideoPlayerActivity;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Класс для обработки нажатий кнопок плеера в WebView
 * Содержит JavaScript интерфейс и логику настройки слушателей
 */
public class PlayerButtonHandler {
    private static final String TAG = "PlayerButtonHandler";
    private static final String JS_INTERFACE_NAME = "AndroidInterface";
    
    private final Context context;
    
    public PlayerButtonHandler(Context context) {
        this.context = context;
    }
    
    /**
     * Добавляет JavaScript интерфейс к WebView
     */
    public void addJavaScriptInterface(WebView webView) {
        webView.addJavascriptInterface(new PlayerButtonJSInterface(), JS_INTERFACE_NAME);
        Log.d(TAG, "JavaScript interface added to WebView");
    }
    
    /**
     * Настраивает слушатели кнопок плеера для SPA приложений
     */
    public void setupPlayerButtonListeners(WebView webView) {
        Log.d(TAG, "Setting up SPA-aware player button listeners");

        // Загружаем и выполняем JavaScript файлы
        loadAndExecuteJS(webView, "js/license-button-listener.js", "License button listener");
        loadAndExecuteJS(webView, "js/player-button-listener.js", "Player button listener");
        loadAndExecuteJS(webView, "js/debug-info.js", "Debug info");
        loadAndExecuteJS(webView, "js/button-checker.js", "Button checker");

    }

    /**
     * Загружает и выполняет JavaScript файл из assets
     */
    private void loadAndExecuteJS(WebView webView, String assetPath, String description) {
        try {
            InputStream inputStream = context.getAssets().open(assetPath);
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            inputStream.close();
            
            String jsCode = new String(buffer, StandardCharsets.UTF_8);
            
            webView.evaluateJavascript(jsCode, value -> {
                Log.d(TAG, description + " result: " + value);
                if (value == null || "null".equals(value)) {
                    Log.e(TAG, "JavaScript returned null - syntax error in " + description);
                }
            });
            
            Log.d(TAG, "Loaded and executed: " + assetPath);
        } catch (IOException e) {
            Log.e(TAG, "Failed to load JS file: " + assetPath, e);
        }
    }
    
    
    /**
     * JavaScript интерфейс для обработки нажатий кнопок плеера
     */
    private class PlayerButtonJSInterface {
        @OptIn(markerClass = UnstableApi.class)
        @JavascriptInterface
        public void onPlayerButtonClicked(String buttonHref) {
            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).runOnUiThread(() -> {
                    Log.d(TAG, "Player button clicked: " + buttonHref);
                    Log.d("PlayerHandler", "Starting VideoPlayerActivity for URL: " + buttonHref);
                    VideoPlayerActivity.startFromAnimePage((android.app.Activity) context, buttonHref);
                });
            } else {
                Log.e(TAG, "Context is not an Activity, cannot start VideoPlayerActivity");
            }
        }
    }
}
