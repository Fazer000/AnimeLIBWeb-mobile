package com.example.animelib;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
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

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.util.UnstableApi;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.animelib.data.AppSettings;
import com.example.animelib.ui.UrlInputDialog;
import com.example.animelib.ui.PlayerButtonHandler;
import com.example.animelib.viewmodel.AppSettingsViewModel;
import com.example.animelib.util.ThemeUtils;
import com.example.animelib.api.ApiService;
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
    private OkHttpClient httpClient;
    private Executor executor;
    private Gson gson;
    private AppSettingsViewModel viewModel;
    private ApiService apiService;
    private PlayerButtonHandler playerButtonHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        
        // Initialize player button handler
        playerButtonHandler = new PlayerButtonHandler(this);

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

        setupWebView();
        setupRefreshLayout();
        setupBackPressHandler();

        checkAndLoadUrl();
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//        startTestPlayer();
//    }

    private void loadAndApplyTheme() {
        executor.execute(() -> {
            int themeMode = apiService.loadThemeSetting();
            runOnUiThread(() -> {
                ThemeUtils.applyTheme(themeMode);
                Log.d("MainActivity", "Theme applied: " + themeMode);
            });
        });
    }

    @OptIn(markerClass = UnstableApi.class)
    private void startTestPlayer() {
        // Тестовый запуск плеера с демо видео
        // Для тестирования интерфейса плеера
        Intent intent = new Intent(this, VideoPlayerActivity.class);
        intent.putExtra(VideoPlayerActivity.EXTRA_ANIME_URL,
                "https://v3.animelib.org/ru/anime/22934--saikyou-tank-no-meikyuu-kouryaku-tairyoku-9999-no-rare-skill-mochi-tank-yuusha-party-wo-tsuihou-sareru-anime/watch");
        startActivity(intent);
        finish(); // Закрываем MainActivity чтобы не было возможности вернуться
    }

    private void checkAndLoadUrl() {
        viewModel.getSettings().observe(this, settings -> {
            if (settings != null && settings.getSiteUrl() != null) {
                // URL найден в базе данных, загружаем его
                loadUrl(settings.getSiteUrl());
            } else {
                // URL не найден, показываем диалог для ввода
                showUrlInputDialog();
            }
        });
    }

    private void loadUrl(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        Map<String, String> headers = getStringStringMap();
        webView.loadUrl(url, headers);

        // Check API for toast messages
        checkApiForToast();
    }

    private void showUrlInputDialog() {
        UrlInputDialog dialog = new UrlInputDialog(this, url -> {
            viewModel.saveSettings(url);
            loadUrl(url);
        });
        dialog.show();
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
//        webSettings.setAppCacheEnabled(true);
//        webSettings.setAppCachePath(getCacheDir().getAbsolutePath());
//        webSettings.setAppCacheMaxSize(50 * 1024 * 1024); // 50MB
        
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
        webSettings.setSaveFormData(false);
        
        // Оптимизация загрузки ресурсов
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setBlockNetworkImage(false);
        webSettings.setBlockNetworkLoads(false);
        
        // Дополнительные оптимизации для современных устройств
        // Включаем предварительное кэширование для плавной прокрутки
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

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        // Add JavaScript interface for video detection
        playerButtonHandler.addJavaScriptInterface(webView);

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
                
                // Проверяем на 404 и другие ошибочные страницы
                if (url.contains("/404") || url.contains("/error") || url.contains("/not-found") || 
                    url.contains("404.html") || url.contains("error.html")) {
                    Log.w("WebView", "Blocked redirect to error page: " + url);

                    return true; // Блокируем переход
                }

                // Allow other URLs to load normally
                view.loadUrl(url, headers);
                return true;
            }
            
            @Override
            public android.webkit.WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                
                // Перехватываем запросы к страницам ошибок ДО их загрузки
                if (url.contains("/404") || url.contains("/error") || url.contains("/not-found") || 
                    url.contains("404.html") || url.contains("error.html")) {
                    Log.w("WebView", "Intercepted request to error page: " + url);

                    // Возвращаем пустой ответ чтобы заблокировать загрузку
                    return new android.webkit.WebResourceResponse("text/html", "UTF-8", null);
                }
                
                return super.shouldInterceptRequest(view, request);
            }
            
            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, android.webkit.WebResourceResponse errorResponse) {
                super.onReceivedHttpError(view, request, errorResponse);
                
                String url = request.getUrl().toString();
                int statusCode = errorResponse.getStatusCode();
                
                Log.w("WebView", "HTTP Error " + statusCode + " for URL: " + url);
                
                if (statusCode == 404) {
                    runOnUiThread(() -> {
                        // Возвращаемся на предыдущую страницу если возможно
                        if (webView.canGoBack()) {
                            webView.goBack();
                        }
                    });
                } else if (statusCode >= 400) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Ошибка загрузки: " + statusCode, Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (isFirstLoad && spinner.getVisibility() != View.VISIBLE) {
                    spinner.setVisibility(View.VISIBLE);
                    spinnerBackground.setVisibility(View.VISIBLE);
                    Log.d("WebView", "First load, spinner shown: " + url);
                }
                swipeRefreshLayout.setRefreshing(false);

                // Always setup listeners - let JavaScript determine if it's needed
                Log.d("WebView", "Setting up player button listeners for SPA");
                playerButtonHandler.setupPlayerButtonListeners(view);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                spinner.setVisibility(View.GONE);
                spinnerBackground.setVisibility(View.GONE);
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
                    Objects.requireNonNull(webView.getWebChromeClient()).onHideCustomView();
                } else if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    // Современный способ закрытия активности
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        if (apiService != null) {
            apiService.shutdown();
        }
        super.onDestroy();
    }
}
