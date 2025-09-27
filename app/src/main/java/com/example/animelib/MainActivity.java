package com.example.animelib;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.example.animelib.data.DatabaseManager;
import com.example.animelib.data.entity.TokenEntity;
import com.example.animelib.dialogs.ThemeSelectionDialog;
import com.example.animelib.models.TokenResponse;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.util.UnstableApi;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.animelib.ui.PlayerButtonHandler;
import com.example.animelib.util.ThemeUtils;
import com.example.animelib.viewmodel.AppSettingsViewModel;
import com.example.animelib.api.ApiService;
import com.example.animelib.dialogs.BookmarksPopupDialog;
import com.example.animelib.models.BookmarksListResponse;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import com.google.gson.Gson;

import okhttp3.OkHttpClient;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private CircularProgressIndicator spinner;
    private FrameLayout spinnerBackground;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FrameLayout fullscreenContainer;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private boolean isFirstLoad = true;
    private String currentDomain = null;
    private long lastBackPressTime = 0;
    private static final int BACK_PRESS_INTERVAL = 2000; // 2 секунды
    private static final int REQUEST_URL_INPUT = 1001;
    private OkHttpClient httpClient;
    private Executor executor;
    private Gson gson;
    private AppSettingsViewModel viewModel;
    private ApiService apiService;
    private PlayerButtonHandler playerButtonHandler;
    private BookmarksPopupDialog currentBookmarksDialog;
    private ThemeSelectionDialog themeDialog;
    private DatabaseManager databaseManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        spinner = findViewById(R.id.spinner);
        spinnerBackground = findViewById(R.id.spinnerBackground);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        fullscreenContainer = findViewById(R.id.fullscreenContainer);

        // Initialize HTTP client and Gson
        httpClient = new OkHttpClient();
        executor = Executors.newSingleThreadExecutor();
        gson = new Gson();

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(AppSettingsViewModel.class);

        // Initialize API service
        apiService = new ApiService(this);

        databaseManager = new DatabaseManager(this);

        // Initialize player button handler
        playerButtonHandler = new PlayerButtonHandler(this);

        // Initialize theme manager
        themeDialog = new ThemeSelectionDialog(this, apiService);

        // Clear WebView cache to avoid Chromium errors
        try {
            android.webkit.WebView tempWebView = new android.webkit.WebView(this);
            tempWebView.clearCache(true);
            tempWebView.clearHistory();
            tempWebView.destroy();
        } catch (Exception e) {
            Log.w("MainActivity", "Failed to clear WebView cache", e);
        }

        // Включаем аппаратное ускорение для всего приложения
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        );

        // Load and apply theme
        loadAndApplyTheme();

        // Обновляем токены при запуске
        updateTokensOnStartup();

//        showBookmarksPopupIfNeeded();

        setupWebView();
        setupRefreshLayout();
        setupBackPressHandler();

        checkAndLoadUrl();
    }

    private void loadAndApplyTheme() {
        executor.execute(() -> {
            try {
                // Получаем тему из базы данных
                int themeMode = databaseManager.loadThemeSetting();
                Log.d("Theme", "Loaded theme from database: " + themeMode);
                
                // Также проверяем SharedPreferences
                int sharedPrefTheme = ThemeUtils.getSavedThemePreference(this);
                Log.d("Theme", "Loaded theme from SharedPreferences: " + sharedPrefTheme);
                
                // Используем тему из базы данных, если она есть, иначе из SharedPreferences
                int finalTheme = themeMode != 0 ? themeMode : sharedPrefTheme;
                
                // Применяем тему в главном потоке
                runOnUiThread(() -> {
                    ThemeUtils.applyThemeToActivity(MainActivity.this, finalTheme);
                    Log.d("Theme", "Theme applied on startup: " + finalTheme);
                });
                
            } catch (Exception e) {
                Log.e("Theme", "Failed to load and apply theme", e);
                // Применяем тему по умолчанию в главном потоке
                runOnUiThread(() -> {
                    ThemeUtils.applyThemeToActivity(MainActivity.this, 0);
                });
            }
        });
    }
    
    /**
     * Обновляет токены при запуске приложения
     */
    private void updateTokensOnStartup() {
        executor.execute(() -> {
            try {
                // Проверяем есть ли токен в БД
                boolean hasToken = databaseManager.hasToken();
                Log.d("MainActivity", "Has token in DB: " + hasToken);
                
                if (hasToken) {
                    TokenEntity token = databaseManager.getToken();
                    if (token != null) {
                        Log.d("MainActivity", "Token found in DB: " + token.getAccessToken().substring(0, 20) + "...");
                        
                        // Проверяем не истек ли токен
                        long currentTime = System.currentTimeMillis();
                        long tokenExpiry = token.getTimestamp() + (token.getExpiresIn() * 1000);
                        
                        if (currentTime < tokenExpiry) {
                            Log.d("MainActivity", "Token is still valid, expires at: " + new java.util.Date(tokenExpiry));
                        } else {
                            Log.d("MainActivity", "Token expired, will update from localStorage");
                        }
                    }
                } else {
                    Log.d("MainActivity", "No token in DB, will load from localStorage when available");
                }
                
            } catch (Exception e) {
                Log.e("MainActivity", "Error checking tokens on startup", e);
            }
        });
    }

    private void checkAndLoadUrl() {
        viewModel.getSettings().observe(this, settings -> {
            if (settings != null && settings.getSiteUrl() != null) {
                // URL найден в базе данных, загружаем его
                loadUrl(settings.getSiteUrl());
            } else {
                // URL не найден, показываем активность для ввода
                showUrlInputActivity();
            }
        });
    }

    private void loadUrl(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        // Сохраняем домен первой загрузки
        if (currentDomain == null) {
            currentDomain = extractDomain(url);
        }

        Map<String, String> headers = getStringStringMap();
        webView.loadUrl(url, headers);

        // Check API for toast messages
        checkApiForToast();
    }

    private void showUrlInputActivity() {
        Intent intent = new Intent(this, UrlInputActivity.class);
        startActivityForResult(intent, REQUEST_URL_INPUT);
    }

    private void checkApiForToast() {
        apiService.checkApiForToast(new ApiService.ToastCheckCallback() {
            @Override
            public void onToastReceived(String message, String newUrl) {
                runOnUiThread(() -> {
                    if (newUrl != null && message.contains("Перейти на зеркало")) {
                        // Обновляем URL в базе данных
                        viewModel.updateSettings(newUrl);
                        Toast.makeText(MainActivity.this,
                                "Зеркало обновлено: " + newUrl,
                                Toast.LENGTH_LONG).show();
                        Log.d("ApiCheck", "URL updated to: " + newUrl);
                    } else {
                        Log.d("checkApiForToast", message);
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.d("checkApiForToast", error);
            }
        });
    }


    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();

        // Основные настройки JavaScript и DOM
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);

        // Улучшенные настройки кэширования
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // Оптимизация viewport и масштабирования
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportZoom(false);

        // Улучшение качества текста и изображений
        webSettings.setTextZoom(100);
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);

        // Современные настройки безопасности и контента
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(false);
        webSettings.setAllowUniversalAccessFromFileURLs(false);

        // Медиа и геолокация
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setGeolocationEnabled(true);

        // Улучшенный User Agent
        webSettings.setUserAgentString(getRandomUserAgent());

        // Дополнительные настройки производительности
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        // Настройки для лучшего отображения
        webSettings.setNeedInitialFocus(false);

        // Оптимизация загрузки ресурсов
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setBlockNetworkImage(false);
        webSettings.setBlockNetworkLoads(false);

        // Дополнительные оптимизации для современных устройств
        webSettings.setSafeBrowsingEnabled(true);

        // Принудительная темная тема для WebView (если поддерживается)
        webSettings.setForceDark(WebSettings.FORCE_DARK_AUTO);

        // Современная оптимизация рендера и производительности
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        // Включаем рендеринг вне основного потока для лучшей производительности
        webSettings.setOffscreenPreRaster(true);

        // Отключаем отладку в production для лучшей производительности
        WebView.setWebContentsDebuggingEnabled(false);

        // Дополнительные оптимизации WebView
        webView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(true);
        webView.setVerticalScrollBarEnabled(true);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);

        // Оптимизация памяти и производительности
        webView.setInitialScale(0);
        webView.getSettings().setMinimumFontSize(8);
        webView.getSettings().setMinimumLogicalFontSize(8);
        webView.getSettings().setDefaultFontSize(16);
        webView.getSettings().setDefaultFixedFontSize(13);

        // Настройки cookies для лучшей совместимости
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        // Дополнительные настройки для работы с cookies
        cookieManager.flush();

        // Включаем cookies для всех доменов
        CookieManager.setAcceptFileSchemeCookies(true);

        // Add JavaScript interface for video detection
        playerButtonHandler.addJavaScriptInterface(webView);

        // Добавляем JavaScript для работы с cookies
        webView.addJavascriptInterface(new Object() {
            @android.webkit.JavascriptInterface
            public void setCookie(String name, String value, String domain) {
                CookieManager.getInstance().setCookie(domain, name + "=" + value);
            }

            @android.webkit.JavascriptInterface
            public String getCookie(String name, String domain) {
                String cookies = CookieManager.getInstance().getCookie(domain);
                if (cookies != null) {
                    String[] cookieArray = cookies.split(";");
                    for (String cookie : cookieArray) {
                        String[] parts = cookie.trim().split("=");
                        if (parts.length == 2 && parts[0].equals(name)) {
                            return parts[1];
                        }
                    }
                }
                return null;
            }
        }, "CookieManager");


        Map<String, String> headers = getStringStringMap();
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        // Оптимизация скроллинга
        webView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(true);
        webView.setVerticalScrollBarEnabled(true);
        webView.setHorizontalScrollBarEnabled(false);

        webView.setWebViewClient(new WebViewClient() {
            @OptIn(markerClass = UnstableApi.class)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                Log.d("WebView", "Checking URL for redirect: " + url);

                // Проверяем переход на другой домен
                String newDomain = extractDomain(url);
                if (newDomain != null && currentDomain != null && !currentDomain.equals(newDomain)) {
                    Log.d("WebView", "Domain change detected in shouldOverrideUrlLoading: " + currentDomain + " -> " + newDomain);
                    showDomainChangeSpinner();
                }

                // Allow other URLs to load normally
                view.loadUrl(url, headers);
                return true;
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, android.webkit.WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);

                String url = request.getUrl().toString();
                int statusCode = errorResponse.getStatusCode();

                Log.w("WebView", "HTTP Error " + statusCode + " for URL: " + url);

                // Автоматически возвращаемся назад при 404 ошибке
                if (statusCode == 404) {
                    runOnUiThread(() -> {
                        if (webView.canGoBack()) {
                            webView.goBack();
                            Log.d("WebView", "Auto-back from 404 error");
                        }
                    });
                }
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                String newDomain = extractDomain(url);

                // Показываем спиннер если это первая загрузка или переход на другой домен
                if (isFirstLoad || (currentDomain != null && !currentDomain.equals(newDomain))) {
                    spinner.setVisibility(View.VISIBLE);
                    spinnerBackground.setVisibility(View.VISIBLE);
                    Log.d("WebView", "Spinner shown - First load: " + isFirstLoad +
                          ", Domain changed: " + (currentDomain != null && !currentDomain.equals(newDomain)) +
                          ", URL: " + url);
                }

                // Обновляем текущий домен
                currentDomain = newDomain;
                swipeRefreshLayout.setRefreshing(false);

                // Always setup listeners - let JavaScript determine if it's needed
                Log.d("WebView", "Setting up player button listeners for SPA");
                playerButtonHandler.setupPlayerButtonListeners(view);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                spinner.setVisibility(View.GONE);
                spinnerBackground.setVisibility(View.GONE);
                
                // Скрываем спиннер домена через JavaScript интерфейс
                hideDomainChangeSpinner();
                
                if (isFirstLoad) {
                    isFirstLoad = false;
                }
                swipeRefreshLayout.setRefreshing(false);
                Log.d("WebView", "Finished loading: " + url);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, android.webkit.WebResourceError error) {
                super.onReceivedError(view, request, error);

                spinner.setVisibility(View.GONE);
                spinnerBackground.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }

                customView = view;
                customViewCallback = callback;

                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                );
                fullscreenContainer.addView(customView, params);
                fullscreenContainer.setVisibility(View.VISIBLE);

                webView.setVisibility(View.GONE);
            }

            @Override
            public void onHideCustomView() {
                if (customView == null) {
                    return;
                }

                fullscreenContainer.removeView(customView);
                customView = null;

                fullscreenContainer.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
                customViewCallback.onCustomViewHidden();
            }
        });
    }


    private void setupRefreshLayout() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            webView.reload();
            spinner.setVisibility(View.VISIBLE);
            spinnerBackground.setVisibility(View.VISIBLE);
            Log.d("WebView", "Refresh triggered, spinner shown");
        });
        swipeRefreshLayout.setOnChildScrollUpCallback((parent, child) -> webView.getScrollY() > 0);
        swipeRefreshLayout.setOverScrollMode(View.OVER_SCROLL_NEVER);
    }

    private String getRandomUserAgent() {
        String[] userAgents = {
                // Современные Chrome на Android
                "Mozilla/5.0 (Linux; Android 14; SM-G998B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
                "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
                "Mozilla/5.0 (Linux; Android 14; SM-S928B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
                "Mozilla/5.0 (Linux; Android 13; SM-A546B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36",
                // Samsung Internet
                "Mozilla/5.0 (Linux; Android 14; SM-G998B) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/23.0 Chrome/115.0.0.0 Mobile Safari/537.36",
                // Edge Mobile
                "Mozilla/5.0 (Linux; Android 14; SM-G998B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36 EdgA/131.0.0.0"
        };
        return userAgents[new Random().nextInt(userAgents.length)];
    }

    public String getCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.ENGLISH);
        dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
        return dateFormat.format(new Date());
    }

    @NonNull
    private Map<String, String> getStringStringMap() {
        Map<String, String> headers = new HashMap<>();

        // Основные заголовки
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8");
        headers.put("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Cache-Control", "max-age=0");
        headers.put("Connection", "keep-alive");

        // Современные заголовки безопасности
        headers.put("Sec-Fetch-Dest", "document");
        headers.put("Sec-Fetch-Mode", "navigate");
        headers.put("Sec-Fetch-Site", "none");
        headers.put("Sec-Fetch-User", "?1");
        headers.put("Upgrade-Insecure-Requests", "1");

        // Client Hints для лучшей оптимизации
        headers.put("Sec-CH-UA", "\"Google Chrome\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"");
        headers.put("Sec-CH-UA-Mobile", "?1");
        headers.put("Sec-CH-UA-Platform", "\"Android\"");
        headers.put("Sec-CH-UA-Platform-Version", "\"14.0.0\"");
        headers.put("Sec-CH-UA-Arch", "\"arm\"");
        headers.put("Sec-CH-UA-Bitness", "\"64\"");
        headers.put("Sec-CH-UA-Model", "\"SM-G998B\"");

        // Динамический User-Agent
        headers.put("User-Agent", getRandomUserAgent());

        // Дата для кэширования
        headers.put("Date", getCurrentDate());

        return headers;
    }

    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (customView != null) {
                    // Выход из полноэкранного режима
                    Objects.requireNonNull(webView.getWebChromeClient()).onHideCustomView();
                } else if (webView.canGoBack()) {
                    // Возврат в WebView
                    webView.goBack();
                } else {
                    // Двойное нажатие для выхода из приложения
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastBackPressTime < BACK_PRESS_INTERVAL) {
                        // Второе нажатие - выходим из приложения
                        setEnabled(false);
                        getOnBackPressedDispatcher().onBackPressed();
                    } else {
                        // Первое нажатие - показываем сообщение
                        lastBackPressTime = currentTime;
                        Toast.makeText(MainActivity.this, "Нажмите еще раз для выхода", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    /**
     * Показывает диалог выбора темы
     */
    public void showThemeDialog() {
        themeDialog.show();
    }
    
    /**
     * Получает значение auth из localStorage WebView и показывает в Toast
     */
    public void getAuthFromLocalStorage() {
        if (webView != null) {
            webView.evaluateJavascript(
                "localStorage.getItem('auth')",
                value -> {
                    runOnUiThread(() -> {
                        // Убираем внешние кавычки и экранируем внутренние кавычки
                        String authValue = value != null ? value.replaceAll("^\"|\"$", "") : "null";
                        
                        // Декодируем экранированные кавычки
                        if (authValue != null && !"null".equals(authValue)) {
                            authValue = authValue.replace("\\\"", "\"");
                        }
                        
                        if ("null".equals(authValue)) {
                            Log.d("MainActivity", "Auth не найден в localStorage");
                        } else {
                            Log.d("MainActivity", "Auth получен из localStorage");
                            
                            // Парсим и сохраняем токены
                            try {
                                // Проверяем что это валидный JSON
                                if (authValue.startsWith("{") && authValue.endsWith("}")) {
                                    Gson gson = new Gson();
                                    TokenResponse tokenResponse = gson.fromJson(authValue, TokenResponse.class);
                                    if (tokenResponse != null && tokenResponse.getToken() != null) {
                                        saveTokensToDatabase(tokenResponse.getToken());
                                        Log.d("MainActivity", "Successfully parsed and saved tokens");
                                    } else {
                                        Log.w("MainActivity", "Token data not found in auth response");
                                    }
                                } else {
                                    Log.w("MainActivity", "Auth value is not valid JSON: " + authValue.substring(0, Math.min(50, authValue.length())));
                                }
                            } catch (Exception e) {
                                Log.e("MainActivity", "Error parsing auth JSON: " + e.getMessage());
                                Log.e("MainActivity", "Auth value: " + authValue.substring(0, Math.min(100, authValue.length())));
                            }
                        }
                        Log.d("MainActivity", "Auth from localStorage processed");
                    });
                }
            );
        } else {
            Log.d("MainActivity", "WebView не готов");
        }
    }
    
    /**
     * Сохраняет токены в базу данных
     */
    private void saveTokensToDatabase(TokenResponse.TokenData tokenData) {
        executor.execute(() -> {
            try {
                TokenEntity tokenEntity = new TokenEntity(
                    tokenData.getTokenType(),
                    tokenData.getExpiresIn(),
                    tokenData.getAccessToken(),
                    tokenData.getRefreshToken(),
                    tokenData.getTimestamp()
                );
                
                databaseManager.saveToken(tokenEntity);
                Log.d("MainActivity", "Tokens saved to database successfully");
                
            } catch (Exception e) {
                Log.e("MainActivity", "Error saving tokens to database", e);
            }
        });
    }

    /**
     * Показывает кастомный диалог выбора для HTML select элементов
     */
    public void showCustomSelectDialog(String dialogDataJson) {
        try {
            // Парсим JSON данные
            Gson gson = new Gson();
            SelectDialogData dialogData = gson.fromJson(dialogDataJson, SelectDialogData.class);

            if (dialogData == null || dialogData.options == null || dialogData.values == null) {
                Log.e("MainActivity", "Invalid dialog data received");
                return;
            }

            // Создаем и показываем диалог
            com.example.animelib.dialogs.CustomSelectDialog dialog =
                new com.example.animelib.dialogs.CustomSelectDialog(this);

            dialog.show(
                dialogData.title,
                dialogData.options,
                dialogData.values,
                dialogData.currentValue,
                (value, text) -> {
                    // Обновляем кнопку в WebView
                    updateSelectButton(dialogData.selectId, value, text);
                }
            );

            Log.d("MainActivity", "Custom select dialog shown for: " + dialogData.selectId);

        } catch (Exception e) {
            Log.e("MainActivity", "Error showing custom select dialog", e);
        }
    }

    /**
     * Обновляет кнопку select в WebView после выбора опции
     */
    public void updateSelectButton(String selectId, String selectedValue, String selectedText) {
        if (webView != null) {
            String jsCode = String.format(
                "if (window.customSelectHandler) { " +
                "  window.customSelectHandler.updateButtonAfterSelection('%s', '%s', '%s'); " +
                "}",
                selectId, selectedValue, selectedText
            );

            webView.evaluateJavascript(jsCode, result -> {
                Log.d("MainActivity", "Select button updated: " + selectId + " = " + selectedText);
            });
        }
    }

    /**
     * Класс для парсинга данных диалога select
     */
    private static class SelectDialogData {
        public String title;
        public java.util.List<String> options;
        public java.util.List<String> values;
        public String currentValue;
        public String selectId; // ID кнопки
    }

    /**
     * Извлекает домен из URL
     */
    private String extractDomain(String url) {
        try {
            if (url == null || url.isEmpty()) {
                return null;
            }

            // Добавляем протокол если его нет
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }

            java.net.URL urlObj = new java.net.URL(url);
            return urlObj.getHost();
        } catch (Exception e) {
            Log.w("MainActivity", "Failed to extract domain from URL: " + url, e);
            return null;
        }
    }

    /**
     * Показывает спиннер при смене домена
     */
    public void showDomainChangeSpinner() {
        runOnUiThread(() -> {
            Log.d("MainActivity", "Showing domain change spinner");
            if (spinnerBackground != null) {
                spinnerBackground.setVisibility(View.VISIBLE);
                spinnerBackground.setAlpha(0f);
                spinnerBackground.animate()
                        .alpha(1f)
                        .setDuration(200)
                        .setInterpolator(new android.view.animation.DecelerateInterpolator())
                        .start();
            }
            if (spinner != null) {
                spinner.setVisibility(View.VISIBLE);
                spinner.setAlpha(0f);
                spinner.setScaleX(0.8f);
                spinner.setScaleY(0.8f);
                spinner.animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(300)
                        .setInterpolator(new android.view.animation.OvershootInterpolator(0.6f))
                        .start();
            }
        });
    }

    /**
     * Скрывает спиннер при смене домена
     */
    public void hideDomainChangeSpinner() {
        runOnUiThread(() -> {
            Log.d("MainActivity", "Hiding domain change spinner");
            if (spinner != null) {
                spinner.animate()
                        .alpha(0f)
                        .scaleX(0.8f)
                        .scaleY(0.8f)
                        .setDuration(200)
                        .setInterpolator(new android.view.animation.AccelerateInterpolator())
                        .withEndAction(() -> spinner.setVisibility(View.GONE))
                        .start();
            }
            if (spinnerBackground != null) {
                spinnerBackground.animate()
                        .alpha(0f)
                        .setDuration(200)
                        .setInterpolator(new android.view.animation.AccelerateInterpolator())
                        .withEndAction(() -> spinnerBackground.setVisibility(View.GONE))
                        .start();
            }
        });
    }

//    /**
//     * Показывает popup с закладками при запуске приложения (один раз за сессию)
//     */
//    private void showBookmarksPopupIfNeeded() {
//            apiService.fetchBookmarksList(new ApiService.BookmarksListCallback() {
//                @Override
//                public void onBookmarksReceived(BookmarksListResponse response) {
//                    runOnUiThread(() -> {
//                        currentBookmarksDialog = new BookmarksPopupDialog(MainActivity.this, response, 10000);
//                        currentBookmarksDialog.show();
//                        currentBookmarksDialog.setOnDismissListener(dialog -> currentBookmarksDialog = null);
//                    });
//                }
//
//                @Override
//                public void onError(String error) {
//                    Log.e("MainActivity", "Failed to load bookmarks: " + error);
//                }
//            });
//    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_URL_INPUT) {
            if (resultCode == RESULT_OK && data != null) {
                String siteUrl = data.getStringExtra("site_url");
                if (siteUrl != null && !siteUrl.isEmpty()) {
                    // Сохраняем URL в ViewModel
                    viewModel.saveSettings(siteUrl);
                    // Загружаем URL
                    loadUrl(siteUrl);
                    Log.d("MainActivity", "URL received from UrlInputActivity: " + siteUrl);
                }
            } else {
                // Пользователь отменил ввод, показываем сообщение
                Toast.makeText(this, "Для работы приложения необходимо указать URL сайта", Toast.LENGTH_LONG).show();
                // Показываем активность снова
                showUrlInputActivity();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        if (apiService != null) {
            apiService.shutdown();
        }
        currentBookmarksDialog = null;

//        // Закрываем диалог если он открыт
//        if (currentBookmarksDialog == null) {
//            currentBookmarksDialog.dismiss();
//            Log.d("BookmarkDialog", "Пиздец диалог не нулль ебать");
//        } else {
//            Log.d("BookmarkDialog", "Пиздец диалог нулль ебать");
//        }

        super.onDestroy();
    }
}
