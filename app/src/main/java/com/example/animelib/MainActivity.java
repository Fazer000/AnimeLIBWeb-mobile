package com.example.animelib;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
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
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.util.UnstableApi;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.animelib.api.ApiResponse;
import com.example.animelib.data.AppSettings;
import com.example.animelib.data.ButtonData;
import com.example.animelib.ui.UrlInputDialog;
import com.example.animelib.viewmodel.AppSettingsViewModel;
import com.example.animelib.VideoPlayerActivity;
import com.example.animelib.util.ThemeUtils;
import com.example.animelib.api.AnimeApiService;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import com.google.gson.Gson;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.HashMap;
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
    private AnimeApiService apiService;

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
        apiService = new AnimeApiService(this);

        // Load and apply theme
        loadAndApplyTheme();

        setupWebView();
        setupRefreshLayout();
        setupBackPressHandler();

//         ТЕСТОВЫЙ РЕЖИМ - раскомментируйте строку ниже для тестирования плеера
//        startTestPlayer();

        checkAndLoadUrl();
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
        executor.execute(() -> {
            String refererUrl = "https://v2.animelib.org/";

            // Получаем текущий URL из базы данных
            AppSettings settings = viewModel.getSettings().getValue();
            if (settings != null && settings.getSiteUrl() != null) {
                refererUrl = settings.getSiteUrl();
            }

            Request request = new Request.Builder()
                    .url("https://api.cdnlibs.org/api/")
                    .addHeader("referer", refererUrl)
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e("ApiCheck", "API request failed", e);
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Хуй там - " + e, Toast.LENGTH_SHORT).show());
    } 

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    try (response) {
                        if (response.isSuccessful()) {
                            assert response.body() != null;
                            String responseBody = response.body().string();
                            Log.d("ApiCheck", "Response: " + responseBody);

                            ApiResponse apiResponse = gson.fromJson(responseBody, ApiResponse.class);

                            if (apiResponse != null && apiResponse.getData() != null
                                && apiResponse.getData().getToast() != null
                                && apiResponse.getData().getToast().getButtons() != null
                                && !apiResponse.getData().getToast().getButtons().isEmpty()) {

                                ButtonData button = apiResponse.getData().getToast().getButtons().get(0);
                                String message = button.getText();

                                if (message != null && message.contains("Перейти на зеркало")) {
                                    // Извлекаем новый URL из href
                                    String newUrl = button.getHref();
                                    if (newUrl != null && !newUrl.isEmpty()) {
                                        // Обновляем URL в базе данных
                                        runOnUiThread(() -> {
                                            viewModel.updateSettings(newUrl);
                                            Toast.makeText(MainActivity.this,
                                                "Зеркало обновлено: " + newUrl,
                                                Toast.LENGTH_LONG).show();
                                            Log.d("ApiCheck", "URL updated to: " + newUrl);
                                        });
                                    }
                                } else if (message != null) {
                                    // Показываем обычное сообщение
                                    runOnUiThread(() -> {
                                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                                    });
                                } else {
                                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Хуй там нет текста", Toast.LENGTH_SHORT).show());
                                }
                            }
                        } else {
                            Log.e("ApiCheck", "API request failed with code: " + response.code());
                        }
                    } catch (Exception e) {
                        Log.e("ApiCheck", "Error parsing API response", e);
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Хуй там - " + e, Toast.LENGTH_SHORT).show());
                    }
                }
            });
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
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setGeolocationEnabled(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setUserAgentString(getRandomUserAgent());

        webSettings.setSupportZoom(false);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);

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
                "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Mobile Safari/537.36",
                "Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.159 Mobile Safari/537.36",
                "Mozilla/5.0 (Linux; Android 12; SM-A525F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.62 Mobile Safari/537.36",
                "Mozilla/5.0 (Linux; Android 13; SM-N986B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.71 Mobile Safari/537.36"
        };
        return userAgents[new Random().nextInt(userAgents.length)];
    }

    @NonNull
    private Map<String, String> getStringStringMap() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8");
        headers.put("Accept-Language", "en-US,en;q=0.5");
        headers.put("Connection", "keep-alive");
        headers.put("Upgrade-Insecure-Requests", "1");
        headers.put("Sec-Fetch-Dest", "document");
        headers.put("Sec-Fetch-Mode", "navigate");
        headers.put("Sec-Fetch-Site", "none");
        headers.put("Sec-Fetch-User", "?1");
        headers.put("Referer", "https://www.google.com/");
        headers.put("DNT", "1");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("User-Agent", getRandomUserAgent());
        headers.put("Sec-CH-UA", "\"Chromium\";v=\"94\", \"Google Chrome\";v=\"94\", \";Not A Brand\";v=\"99\"");
        headers.put("Sec-CH-UA-Mobile", "?1");
        headers.put("Sec-CH-UA-Platform", "\"Android\"");
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
