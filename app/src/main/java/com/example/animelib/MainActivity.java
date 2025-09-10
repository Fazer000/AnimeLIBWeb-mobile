package com.example.animelib;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import com.google.gson.Gson;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(0xFF252527);

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

//        updateFitsSystemWindows();
        setupWebView();
        setupRefreshLayout();
        setupBackPressHandler();
        checkAndLoadUrl();
    } 

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
//        updateFitsSystemWindows();
    }

    private void updateFitsSystemWindows() {
        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        swipeRefreshLayout.setFitsSystemWindows(isPortrait);
    }

    private void setupRefreshLayout() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            webView.reload();
            spinner.setVisibility(View.VISIBLE);
            spinnerBackground.setVisibility(View.VISIBLE);
            Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
            spinner.startAnimation(fadeIn);
            spinnerBackground.startAnimation(fadeIn);
            Log.d("WebView", "Refresh triggered, spinner and background shown with fade-in");
        });
        swipeRefreshLayout.setOnChildScrollUpCallback((parent, child) -> webView.getScrollY() > 0);
        swipeRefreshLayout.setOverScrollMode(View.OVER_SCROLL_NEVER);
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

        Map<String, String> headers = getStringStringMap();
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                Log.d("WebView", "Redirect to: " + url);
                view.loadUrl(url, headers);
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (isFirstLoad && spinner.getVisibility() != View.VISIBLE) {
                    spinner.setVisibility(View.VISIBLE);
                    spinnerBackground.setVisibility(View.VISIBLE);
                    Animation fadeIn = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fade_in);
                    spinner.startAnimation(fadeIn);
                    spinnerBackground.startAnimation(fadeIn);
                    Log.d("WebView", "First load, spinner and background shown with fade-in: " + url);
                }
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                Animation fadeOut = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fade_out);
                fadeOut.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {}
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        spinner.setVisibility(View.GONE);
                        spinnerBackground.setVisibility(View.GONE);
                        if (isFirstLoad) {
                            isFirstLoad = false;
                        }
                    }
                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });
                if (spinner.getVisibility() == View.VISIBLE) {
                    spinner.startAnimation(fadeOut);
                    spinnerBackground.startAnimation(fadeOut);
                }
                swipeRefreshLayout.setRefreshing(false);
                view.evaluateJavascript(
                        "Object.defineProperty(navigator, 'webdriver', { get: () => false });" +
                                "Object.defineProperty(navigator, 'platform', { get: () => 'Android' });" +
                                "Object.defineProperty(navigator, 'maxTouchPoints', { get: () => 5 });" +
                                "if (document.querySelector('video')) { " +
                                "  let video = document.querySelector('video');" +
                                "  video.setAttribute('playsinline', '');" +
                                "  video.preload = 'auto';" +
                                "  video.play().catch(e => console.log('Autoplay error: ', e));" +
                                "}" +
                                "window.scrollTo(0, document.body.scrollHeight / 2);" +
                                "setTimeout(() => { window.scrollTo(0, 0); }, 1000);" +
                                "document.addEventListener('touchstart', function() { console.log('Touch event'); });" +
                                "if (window._cf_chl_opt) { window._cf_chl_opt.cfp(); }" +
                                "if (document.querySelector('iframe[src*=\"turnstile\"]')) { " +
                                "  console.log('Turnstile CAPTCHA detected');" +
                                "  window.postMessage({ type: 'TURNSTILE_CHALLENGE', data: 'solving' }, '*');" +
                                "}",
                        null);
                Log.d("WebView", "Finished, spinner and background hidden with fade-out: " + url);
            }
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.e("WebView", "Error: " + errorCode + " - " + description + " - URL: " + failingUrl);
                Animation fadeOut = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fade_out);
                fadeOut.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {}
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        spinner.setVisibility(View.GONE);
                        spinnerBackground.setVisibility(View.GONE);
                    }
                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });
                spinner.startAnimation(fadeOut);
                spinnerBackground.startAnimation(fadeOut);
                swipeRefreshLayout.setRefreshing(false);
                if (errorCode == WebViewClient.ERROR_TIMEOUT || errorCode == WebViewClient.ERROR_HOST_LOOKUP) {
                    Log.e("WebView", "Timeout or host lookup error, retrying...");
                    view.reload();
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100 && isFirstLoad && spinner.getVisibility() != View.VISIBLE) {
                    spinner.setVisibility(View.VISIBLE);
                    spinnerBackground.setVisibility(View.VISIBLE);
                    Animation fadeIn = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fade_in);
                    spinner.startAnimation(fadeIn);
                    spinnerBackground.startAnimation(fadeIn);
                    Log.d("WebView", "Progress: " + newProgress + "%, spinner and background shown with fade-in");
                } else if (newProgress == 100 && isFirstLoad) {
                    Animation fadeOut = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fade_out);
                    fadeOut.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {}
                        @Override
                        public void onAnimationEnd(Animation animation) {
                            spinner.setVisibility(View.GONE);
                            spinnerBackground.setVisibility(View.GONE);
                            isFirstLoad = false;
                        }
                        @Override
                        public void onAnimationRepeat(Animation animation) {}
                    });
                    if (spinner.getVisibility() == View.VISIBLE) {
                        spinner.startAnimation(fadeOut);
                        spinnerBackground.startAnimation(fadeOut);
                    }
                    Log.d("WebView", "Progress: 100%, spinner and background hidden with fade-out");
                }
            }

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

                customView.setFitsSystemWindows(false);
                fullscreenContainer.setFitsSystemWindows(false);
                webView.setFitsSystemWindows(false);

                swipeRefreshLayout.setPadding(0, 0, 0,0);

                getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
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

                int paddingTopInPx = (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        45,
                        getResources().getDisplayMetrics()
                );

                swipeRefreshLayout.setPadding(0, paddingTopInPx, 0, 0);

                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
        });
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
                    onBackPressed();
                }
            }
        });
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

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}