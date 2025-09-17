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
import android.webkit.JavascriptInterface;
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

    @Override
    protected void onResume() {
        super.onResume();
//        startTestPlayer();
    }

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
                "https://v3.animelib.org/ru/anime/18858--sono-bisque-doll-wa-koi-wo-suru-anime/watch");
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

        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        
        // Современные настройки кэширования
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setGeolocationEnabled(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setUserAgentString(getRandomUserAgent());
        
        // Улучшение качества рендера
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setBlockNetworkImage(false);
        webSettings.setBlockNetworkLoads(false);
        webSettings.setPluginState(WebSettings.PluginState.OFF);
        webSettings.setAllowFileAccessFromFileURLs(false);
        webSettings.setAllowUniversalAccessFromFileURLs(false);
        webSettings.setSaveFormData(false);
        webSettings.setSavePassword(false);

        webSettings.setSupportZoom(false);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);

        // Оптимизация рендера
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webView.setDrawingCacheEnabled(true);
        webView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        
        // Включаем аппаратное ускорение
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        // Add JavaScript interface for video detection
        webView.addJavascriptInterface(new Object() {
            @OptIn(markerClass = UnstableApi.class)
            @JavascriptInterface
            public void onPlayerButtonClicked(String buttonHref) {
                runOnUiThread(() -> {
                    Log.d("JSInterface", "Player button clicked: " + buttonHref);
                    Log.d("PlayerHandler", "Starting VideoPlayerActivity for URL: " + buttonHref);
                    VideoPlayerActivity.startFromAnimePage(MainActivity.this, buttonHref);
                });
            }
        }, "AndroidInterface");

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

                // Allow other URLs to load normally
                view.loadUrl(url, headers);
                return true;
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
                setupPlayerButtonListeners(view);
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
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.e("WebView", "Error: " + errorCode + " - " + description + " - URL: " + failingUrl);
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

    private void setupPlayerButtonListeners(WebView webView) {
        Log.d("WebView", "Setting up SPA-aware player button listeners");

        // First test basic JavaScript
        webView.evaluateJavascript("'test'", value -> {
            Log.d("WebView", "Basic JS test: " + value);
        });

        // Simple and working JavaScript code
        webView.evaluateJavascript(
                "console.log('[AnimeLIB] Test log'); " +
                        "window.animelibTest = 'working'; " +
                        "'basic_test_ok'",
                value -> Log.d("WebView", "Basic test result: " + value)
        );

        // Simplified working JavaScript
        String jsCode =
                "try {" +
                        "  console.log('[AnimeLIB] Starting simple setup');" +
                        "  " +
                        "  if (window.animelibSetup) {" +
                        "    console.log('[AnimeLIB] Already setup');" +
                        "  } else {" +
                        "    window.animelibSetup = true;" +
                        "    " +
                        "    document.addEventListener('click', function(e) {" +
                        "      console.log('[AnimeLIB] Click detected on: ' + e.target.tagName);" +
                        "      " +
                        "      var el = e.target;" +
                        "      for (var i = 0; i < 5 && el; i++) {" +
                        "        if (el.tagName === 'A') {" +
                        "          var href = el.href || el.getAttribute('href') || '';" +
                        "          console.log('[AnimeLIB] Link found: ' + href);" +
                        "          " +
                        "          if (href.includes('/watch') || href.includes('episode')) {" +
                        "            console.log('[AnimeLIB] Player button clicked: ' + href);" +
                        "            e.preventDefault();" +
                        "            e.stopPropagation();" +
                        "            AndroidInterface.onPlayerButtonClicked(href);" +
                        "            break;" +
                        "          }" +
                        "        }" +
                        "        el = el.parentElement;" +
                        "      }" +
                        "    }, true);" +
                        "    " +
                        "    console.log('[AnimeLIB] Simple setup complete');" +
                        "  }" +
                        "  'setup_ok';" +
                        "} catch (e) {" +
                        "  console.error('[AnimeLIB] Error: ' + e.message);" +
                        "  'error: ' + e.message;" +
                        "}";

        // Execute the simplified JavaScript
        webView.evaluateJavascript(jsCode, value -> {
            Log.d("WebView", "Simple setup result: " + value);
            if (value == null || "null".equals(value)) {
                Log.e("WebView", "JavaScript returned null - syntax error!");
            }
        });

        // Also test with delay
        webView.postDelayed(() -> {
            Log.d("WebView", "Running delayed simple setup");
            webView.evaluateJavascript(
                    "console.log('[AnimeLIB] Delayed test at: ' + window.location.href); 'delayed_ok'",
                    value -> Log.d("WebView", "Delayed test result: " + value)
            );
        }, 2000);
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
                "Mozilla/5.0 (Linux; Android 14; SM-G998B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
                "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
                "Mozilla/5.0 (Linux; Android 14; SM-S928B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36",
                "Mozilla/5.0 (Linux; Android 14; SM-A546B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
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
        headers.put("cache-control", "private, must-revalidate");
        headers.put("content-encoding", "gzip");
        headers.put("content-security-policy", "upgrade-insecure-requests;");
        headers.put("content-type", "text/html; charset=UTF-8");

        // Генерируем реальную дату в формате "Wed, 17 Sep 2025 06:30:04 GMT"

        headers.put("date", getCurrentDate());

        headers.put("expires", "-1");
        headers.put("pragma", "no-cache");
        headers.put("server", "ddos-guard");
        headers.put("vary", "Accept-Encoding, Accept-Encoding, Origin");
        headers.put("x-xss-protection", "1; mode=block, 1; mode=block");

        headers.put("Sec-CH-UA", "\"Google Chrome\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"");
        headers.put("Sec-CH-UA-Mobile", "?1");
        headers.put("Sec-CH-UA-Platform", "\"Android\"");
        headers.put("Upgrade-Insecure-Requests", "1");

        headers.put("User-Agent", getRandomUserAgent());

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
                    setEnabled(false);
                    MainActivity.super.onBackPressed();
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
