package com.example.animelib;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PictureInPictureParams;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Build;
import android.util.Log;
import android.util.Rational;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.animelib.models.AnimeInfoResponse;

import com.example.animelib.api.ApiService;
import com.example.animelib.settings.SettingsBottomSheet;
import com.example.animelib.managers.CommentsManager;
import com.example.animelib.managers.EpisodesManager;
import com.example.animelib.managers.PlayersManager;
import com.example.animelib.managers.GesturesManager;
import com.example.animelib.util.ThemeUtils;
import com.example.animelib.models.EpisodeResponse;
import com.example.animelib.models.EpisodesListResponse;
import com.example.animelib.models.KodikResponse;
import com.google.android.material.tabs.TabLayout;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;


@UnstableApi
public class VideoPlayerActivity extends AppCompatActivity {
    public static final String EXTRA_VIDEO_URL = "video_url";
    public static final String EXTRA_ANIME_URL = "anime_url";

    // Bearer token for cdnlibs API - replace with actual token
    private static final String CDNLIBS_BEARER_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJhdWQiOiIxIiwianRpIjoiOTZkYjliMDI4NGM0OWQ1Yzc2NTIxMzkxZTRlNDJkNjAwNTFmMDUzMDU2NjBjZGQzYTRjYmEzN2FjMmRmYTZhNjEyM2VmNDgxZDBjMGU0Y2MiLCJpYXQiOjE3NTc0MzEyNDcuOTg2MjE5LCJuYmYiOjE3NTc0MzEyNDcuOTg2MjIxLCJleHAiOjE3NjAwMjMyNDcuOTgyNTM3LCJzdWIiOiI5NDM5MzIxIiwic2NvcGVzIjpbXX0.FG2bBdeF0328Prrsr9Q_SL-VkQyeJMqE9b9uQ1E74JsCnJPveeMMLYNuJt_cTp5XpkvFK3XHltfCM7wi4Gg-x3rlpG-sTELMaoMNWv-4TmNcQbrKwSnTSVJfUFlnguVA7kpGHBgfAaL3NVKSwu_Pu1xqq6UwqpV9hBSJ6iTHG7T3vz7e_HxhGWQ7AZ47xmoo76aOnWQ2vIceF-zq6gF0peKBsHXuG8Prl-88xyltkT2SSnAJrTl4xmPQsM0F0OntkkFZGU6XPdFwXw-orxvtpCfsv556ra5fdbACMjqfZ3euwqXEHGRtkjMJpmku1-sV_xubQvCgbwuO8WRc-ukuWv3x2WTffkXypFKviEdNTXLBFki5ex4sblvaYhDUd4IrZwIjL-GRPQ9_X6WZITz7Lic5faKs1kr3mxXDSuK7u7tC2WSCom_I_CYR9_aIytJ_XkxixG-aa3LP9-jaOn0n7iZS8XNjaIlLHyqr2Of9wPvJ-A1NVv41EeaptXWs7VcSWg42-fUkofNyS2Qn1Qdo9DzVKmqzO9jMpe-8suwBVGl3gpr4nCwn4J8tIKOTzWX--xHkotH5w1TYaQAtzKs6ocyptylNdAD8WRm_FU3E3pdY5Ecarem7SK8ij5rh724GMiBXN9y9s6jBSwPoIAD9W-R4UoXo1mhsRNGiJ4EkC0U";

    private PlayerView playerView;
    private ExoPlayer player;
    private View loadingOverlay;
    private androidx.media3.ui.DefaultTimeBar timeBar;
    private View controllerView;

    private String currentVideoUrl;
    private String animeUrl;
    private Map<String, String> videoQualities;

    private ExecutorService executor;
    private DefaultHttpDataSource.Factory httpDataSourceFactory;
    private ApiService apiService;

    // Menu components
    private ImageButton ibClosePlayer;
    private ImageButton menuToggleButton;
    private ImageButton settingsButton;
    private View slidingMenuPanel;
    private View menuLoadingIndicator;
    private View menuLoadingOverlay;
    
    // UI components for managers
    private View menuOverlay;
    private ImageButton closeMenuButton;
    private TabLayout playerTabLayout;
    private ViewPager2 playersViewPager;
    private View commentsPanel;
    private ImageButton closeCommentsButton;
    private RecyclerView commentsRecyclerView;
    private View commentsLoadingOverlay;
    private ImageButton commentsOptionsButton;
    private TextView seekPreviewText;
    private TextView holdSpeedToast;
    private ImageButton pipButton;
    private RecyclerView episodesHorizontalRecyclerView;
    private ImageButton commentsButton;

    // Comments manager
    private CommentsManager commentsManager;
    private SettingsBottomSheet currentSettingsBottomSheet;

    // Episodes manager
    private EpisodesManager episodesManager;
    
    // Episodes UI components (for EpisodesManager)
    private ImageButton episodesMenuButton;

    // Players manager
    private PlayersManager playersManager;
    
    // Gestures manager
    private GesturesManager gesturesManager;

    // Picture-in-Picture support
    private boolean isInPictureInPictureMode = false;
    private boolean wasCommentsVisibleBeforePiP = false;

    // Episode navigation buttons (for EpisodesManager)
    private ImageButton prevEpisodeButton;
    private ImageButton nextEpisodeButton;
    
    // Other UI components
    private View playersControlBar;
    private TextView animeTitleView;
    private TextView currentEpisodeNumberView;
    private TextView currentTeamName;
    private TextView currentEpisodeName;

    // Controller visibility state
    private boolean isControllerVisible = false;

    // Player data is now managed by PlayersManager
    private KodikResponse currentKodikResponse;
    private String currentAnimeId;

    // User preferences are now managed by PlayersManager
    private String preferredQuality; // preferred quality (e.g., "720", "480", etc.)
    private boolean enable4K = false;
    private boolean autoPlay = true;
    private int longSkipDuration = 85; // seconds
    private int currentTheme = ThemeUtils.THEME_SYSTEM;

    // Menu state
    // isMenuVisible is now managed by PlayersManager
    private int menuWidth = 300; // dp

    private final int controllerShowTimeoutMs = 4000;
    private boolean shouldAutoHideControls = true; // Контроль автоматического скрытия

    public static void startFromAnimePage(Activity context, String animeUrl) {
        Intent intent = new Intent(context, VideoPlayerActivity.class);
        intent.putExtra(EXTRA_ANIME_URL, animeUrl);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_AnimeLIB_VideoPlayer);
        setContentView(R.layout.activity_video_player);

        // Keep screen on during video playback
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Initialize views
        playerView = findViewById(R.id.playerView);
        loadingOverlay = findViewById(R.id.loadingOverlay);

        // Configure PlayerView to show controls for shorter time
        updateControllerAutoHide();
        
        // Disable ExoPlayer's default controller animations to use our custom alpha animation
        playerView.setControllerAnimationEnabled(false);

        // Initialize network components
        executor = Executors.newSingleThreadExecutor();
        apiService = new ApiService(this);

        // Initialize comments manager
        commentsManager = new CommentsManager(this, apiService);
        
        // Initialize episodes manager
        episodesManager = new EpisodesManager(this, apiService);
        
        // Устанавливаем callback для управления автоматическим скрытием контроллера
        episodesManager.setPlayerControlsCallback(shouldAutoHide -> {
            shouldAutoHideControls = shouldAutoHide;
            updateControllerAutoHide();
        });
        
        // Initialize players manager
        playersManager = new PlayersManager(this, apiService);
        
        // Initialize gestures manager
        gesturesManager = new GesturesManager(this);

        // Clear WebView cache to avoid Chromium errors
        try {
            android.webkit.WebView webView = new android.webkit.WebView(this);
            webView.clearCache(true);
            webView.clearHistory();
            webView.destroy();
        } catch (Exception e) {
            Log.w("VideoPlayer", "Failed to clear WebView cache", e);
        }

        // Load settings from database asynchronously
        executor.execute(() -> {
            enable4K = apiService.load4KSetting();
            autoPlay = apiService.loadAutoPlaySetting();
            longSkipDuration = apiService.loadLongSkipDurationSetting();
            currentTheme = apiService.loadThemeSetting();
            Log.d("VideoPlayer", "Loaded settings - 4K: " + enable4K + ", AutoPlay: " + autoPlay + ", SkipDuration: " + longSkipDuration + ", Theme: " + currentTheme);
        });

        // Initialize HTTP data source with custom headers for video requests
        httpDataSourceFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Linux; Android 14; SM-G998B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36")
                .setDefaultRequestProperties(Map.of(
                        "Referer", "https://v3.animelib.org/",
                        "Accept", "video/mp4,video/*,*/*",
                        "Accept-Encoding", "identity;q=1, *;q=0",
                        "Accept-Language", "ru,en;q=0.9,de;q=0.8,zh;q=0.7",
                        "Origin", "https://v3.animelib.org",
                        "Sec-Fetch-Dest", "video",
                        "Sec-Fetch-Mode", "cors",
                        "Sec-Fetch-Site", "cross-site",
                        "Priority", "i"
                ));

        Log.d("VideoPlayer", "Initialized HTTP data source with custom headers: User-Agent, Referer, Accept, Accept-Encoding, Accept-Language, Origin, Sec-Fetch-*");

        // Get data from intent
        currentVideoUrl = getIntent().getStringExtra(EXTRA_VIDEO_URL);
        animeUrl = getIntent().getStringExtra(EXTRA_ANIME_URL);

        // Setup fullscreen
        setupFullscreen();

        // Setup menu
        setupMenu();

        if (currentVideoUrl != null) {
            // Direct video URL provided - start player immediately
            initializePlayer();
        } else if (animeUrl != null) {
            // Anime page URL provided - fetch video links first
            showLoading("Загрузка видео...");
            loadAnimeFromUrl(animeUrl);
        } else {
            Toast.makeText(this, "Ошибка: Не указан URL видео или страницы аниме", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, android.content.res.Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        this.isInPictureInPictureMode = isInPictureInPictureMode;

        if (isInPictureInPictureMode) {
            // Entering PiP mode
            if (player != null) {
                player.isPlaying();
            }
            wasCommentsVisibleBeforePiP = commentsManager.isCommentsVisible();
            hideAllUI();
            Log.d("VideoPlayer", "Entered Picture-in-Picture mode");
        } else {
            // Exiting PiP mode
            showAllUI();
            Log.d("VideoPlayer", "Exited Picture-in-Picture mode");
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        // Enter PiP mode when user presses home button
        if (player != null && player.isPlaying() && !isInPictureInPictureMode) {
            enterPictureInPictureMode();
        }
    }

    public void enterPictureInPictureMode() {
        try {
            PictureInPictureParams params = new PictureInPictureParams.Builder()
                    .setAspectRatio(new Rational(16, 9)) // 16:9 aspect ratio
                    .build();
            enterPictureInPictureMode(params);
        } catch (Exception e) {
            Log.e("VideoPlayer", "Failed to enter Picture-in-Picture mode", e);
        }
    }

    /**
     * Обновление настроек автоматического скрытия контроллера
     */
    private void updateControllerAutoHide() {
        if (playerView != null) {
            if (shouldAutoHideControls) {
                // Включаем автоматическое скрытие
                playerView.setControllerShowTimeoutMs(controllerShowTimeoutMs);
                playerView.setControllerAutoShow(true);
                playerView.setControllerHideOnTouch(true);
                Log.d("VideoPlayer", "Controller auto-hide enabled");
            } else {
                // Отключаем автоматическое скрытие
                playerView.setControllerShowTimeoutMs(0); // Никогда не скрывать
                playerView.setControllerAutoShow(false);
                playerView.setControllerHideOnTouch(false);
                Log.d("VideoPlayer", "Controller auto-hide disabled");
            }
        }
    }

    private void hideAllUI() {
        // Hide all UI elements except the player
        if (playerView != null) {
            playerView.setUseController(false);
        }

        // Hide menu and other panels
        if (slidingMenuPanel != null) {
            slidingMenuPanel.setVisibility(View.GONE);
        }
        // Hide comments panel if it's currently visible
        if (commentsManager.isCommentsVisible()) {
            commentsManager.hideCommentsPanel();
        }
        // Episodes are now managed by EpisodesManager
        if (episodesManager != null) {
            episodesManager.hideAllEpisodesUI();
        }
        
        // Players are now managed by PlayersManager
        if (playersManager != null) {
            playersManager.hideAllPlayersUI();
        }
        
        // Gestures are now managed by GesturesManager
        if (gesturesManager != null) {
            gesturesManager.hideAllGesturesUI();
        }

        // Hide PiP button in PiP mode
        ImageButton pipButton = findViewById(R.id.pipButton);
        if (pipButton != null) {
            pipButton.setVisibility(View.GONE);
        }

        // Hide settings dialog if open
        if (currentSettingsBottomSheet != null && currentSettingsBottomSheet.isShowing()) {
            currentSettingsBottomSheet.dismiss();
        }
    }

    private void showAllUI() {
        // Show UI elements back
        if (playerView != null) {
            playerView.setUseController(true);
        }

        // Show menu panel
        if (slidingMenuPanel != null) {
            slidingMenuPanel.setVisibility(View.VISIBLE);
        }

        // Show PiP button back
        ImageButton pipButton = findViewById(R.id.pipButton);
        if (pipButton != null) {
            pipButton.setVisibility(View.VISIBLE);
        }

        // Restore episodes UI state
        if (episodesManager != null) {
            episodesManager.showAllEpisodesUI();
        }
        
        // Restore players UI state
        if (playersManager != null) {
            playersManager.showAllPlayersUI();
        }
        
        // Restore gestures UI state
        if (gesturesManager != null) {
            gesturesManager.showAllGesturesUI();
        }

        // Restore comments panel state if it was visible before PiP
        if (wasCommentsVisibleBeforePiP && !commentsManager.isCommentsVisible()) {
            commentsManager.showCommentsPanel();
        }
    }

    private void setupFullscreen() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @SuppressLint({"ClickableViewAccessibility", "RtlHardcoded"})
    private void setupMenu() {
        // === 1. Initialize UI Components ===
        initializeUIComponents();
        
        // === 2. Initialize Managers ===
        initializeManagers();
        
        // === 3. Setup Event Listeners ===
        setupEventListeners();
        
        // === 4. Configure Initial State ===
        configureInitialState();
        
        // === 5. Setup Manager Callbacks ===
        setupManagerCallbacks();
        
        // === 6. Start Initial Loading ===
        startInitialLoading();
    }
    
    /**
     * Инициализация всех UI компонентов
     */
    private void initializeUIComponents() {

        // Main menu components
        controllerView = playerView.findViewById(R.id.exo_controller);
        timeBar = controllerView.findViewById(R.id.exo_progress);
        
        // Set initial alpha for controller (will be animated by our custom logic)
        if (controllerView != null) {
            controllerView.setAlpha(1.0f);
        }

        slidingMenuPanel = findViewById(R.id.slidingMenuPanel);
        View menuOverlay = findViewById(R.id.menuOverlay);
        menuLoadingIndicator = findViewById(R.id.menuLoadingIndicator);
        menuLoadingOverlay = findViewById(R.id.menuLoadingOverlay);
        
        // Gesture components
        TextView seekPreviewText = findViewById(R.id.seekPreviewText);
        TextView holdSpeedToast = findViewById(R.id.holdSpeedToast);
        
        // Comments components
        View commentsPanel = findViewById(R.id.commentsPanel);
        ImageButton closeCommentsButton = findViewById(R.id.closeCommentsButton);
        RecyclerView commentsRecyclerView = findViewById(R.id.commentsRecyclerView);
        View commentsLoadingOverlay = findViewById(R.id.commentsLoadingOverlay);
        ImageButton commentsOptionsButton = findViewById(R.id.commentsOptionsButton);
        
        // Player control components
        if (controllerView != null) {
            initializeControllerComponents();
        }
        
        // Menu control components
        ImageButton closeMenuButton = findViewById(R.id.closeMenuButton);
        TabLayout playerTabLayout = findViewById(R.id.playerTabLayout);
        ViewPager2 playersViewPager = findViewById(R.id.playersViewPager);
        ImageButton pipButton = findViewById(R.id.pipButton);
        
        // Store components for manager initialization
        this.menuOverlay = menuOverlay;
        this.closeMenuButton = closeMenuButton;
        this.playerTabLayout = playerTabLayout;
        this.playersViewPager = playersViewPager;
        this.commentsPanel = commentsPanel;
        this.closeCommentsButton = closeCommentsButton;
        this.commentsRecyclerView = commentsRecyclerView;
        this.commentsLoadingOverlay = commentsLoadingOverlay;
        this.commentsOptionsButton = commentsOptionsButton;
        this.seekPreviewText = seekPreviewText;
        this.holdSpeedToast = holdSpeedToast;
        this.pipButton = pipButton;
    }
    
    /**
     * Инициализация компонентов контроллера плеера
     */
    private void initializeControllerComponents() {
        // Player info components
        ibClosePlayer = controllerView.findViewById(R.id.ibClosePlayer);
        settingsButton = controllerView.findViewById(R.id.settingsButton);
        animeTitleView = controllerView.findViewById(R.id.animeTitle);
        currentTeamName = controllerView.findViewById(R.id.currentTeamName);
        currentEpisodeName = controllerView.findViewById(R.id.currentEpisodeName);
        currentEpisodeNumberView = controllerView.findViewById(R.id.currentEpisodeNumber);
        
        // Navigation components
        episodesMenuButton = controllerView.findViewById(R.id.episodesMenuButton);
        prevEpisodeButton = controllerView.findViewById(R.id.prevEpisodeButton);
        nextEpisodeButton = controllerView.findViewById(R.id.nextEpisodeButton);
        
        // Control components
        menuToggleButton = controllerView.findViewById(R.id.menuToggleButton);
        playersControlBar = controllerView.findViewById(R.id.playersControlBar);
        
        // Episode list component
        RecyclerView episodesHorizontalRecyclerView = controllerView.findViewById(R.id.episodesHorizontalRecyclerView);
        ImageButton commentsButton = controllerView.findViewById(R.id.commentsButton);
        
        // Store for manager initialization
        this.episodesHorizontalRecyclerView = episodesHorizontalRecyclerView;
        this.commentsButton = commentsButton;
    }
    
    /**
     * Инициализация всех менеджеров
     */
    private void initializeManagers() {
        // Initialize gestures manager
        gesturesManager.initializeViews(playerView, null, holdSpeedToast, seekPreviewText);
        
        // Initialize comments manager
        commentsManager.initializeViews(commentsPanel, closeCommentsButton, commentsRecyclerView,
                commentsLoadingOverlay, commentsButton, commentsOptionsButton, menuOverlay);
        
        // Initialize players manager
        playersManager.initializeViews(slidingMenuPanel, closeMenuButton, playerTabLayout,
                playersViewPager, menuOverlay, menuLoadingOverlay, menuLoadingIndicator);
        
        // Initialize episodes manager
        episodesManager.initializeViews(null, episodesMenuButton, episodesHorizontalRecyclerView,
                null, prevEpisodeButton, nextEpisodeButton, menuOverlay, playersControlBar);
        
        // Configure menu width
        float density = getResources().getDisplayMetrics().density;
        menuWidth = (int) (menuWidth * density);
        playersManager.setMenuWidth(menuWidth);
    }

    /**
     * Настройка всех event listeners
     */
    private void setupEventListeners() {
        // Setup controller visibility listener
        setupControllerVisibilityListener();
        
        // Setup button click listeners
        setupButtonClickListeners();
        
        // Setup overlay touch listener
        setupOverlayTouchListener();
        
        // Setup player listener
        setupPlayerListener();
        
        // Setup gestures callback
        setupGesturesCallback();
    }
    
    /**
     * Настройка listener'а видимости контроллера
     */
    private void setupControllerVisibilityListener() {
        playerView.setControllerVisibilityListener((PlayerView.ControllerVisibilityListener) visibility -> {
            boolean shouldBeVisible = visibility == View.VISIBLE;
            
            // Animate controller visibility with alpha
            animateControllerVisibility(shouldBeVisible);
            
            isControllerVisible = shouldBeVisible;
            
            // Update navigation buttons visibility
            episodesManager.updateEpisodeNavigationButtonsVisibility();
            
            // Update button visibility
            updateControllerButtonsVisibility(isControllerVisible);
        });
    }
    
    /**
     * Анимация видимости контроллера через alpha
     */
    private void animateControllerVisibility(boolean visible) {
        if (controllerView == null) return;
        
        // Cancel any existing animation
        controllerView.animate().cancel();
        
        if (visible) {
            // Show controller with fade in
            controllerView.setVisibility(View.VISIBLE);
            controllerView.animate()
                    .alpha(1.0f)
                    .setDuration(200)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
        } else {
            // Hide controller with fade out
            controllerView.animate()
                    .alpha(0.0f)
                    .setDuration(150)
                    .setInterpolator(new android.view.animation.AccelerateInterpolator())
                    .withEndAction(() -> {
                        // Set visibility to GONE after animation completes
                        if (controllerView.getAlpha() == 0.0f) {
                            controllerView.setVisibility(View.GONE);
                        }
                    })
                    .start();
        }
    }
    
    
    /**
     * Обновление видимости кнопок контроллера
     */
    private void updateControllerButtonsVisibility(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.GONE;
        
        if (settingsButton != null) settingsButton.setVisibility(visibility);
        if (episodesMenuButton != null) episodesMenuButton.setVisibility(visibility);
        if (menuToggleButton != null) menuToggleButton.setVisibility(visibility);
        
        commentsManager.updateCommentsButtonVisibility(visible);
    }
    
    /**
     * Настройка click listeners для кнопок
     */
    private void setupButtonClickListeners() {
        // Navigation buttons
        if (prevEpisodeButton != null) {
            prevEpisodeButton.setOnClickListener(v -> {
                if (prevEpisodeButton.isEnabled()) {
                    episodesManager.navigateToPreviousEpisode();
                }
            });
        }
        
        if (nextEpisodeButton != null) {
            nextEpisodeButton.setOnClickListener(v -> {
                if (nextEpisodeButton.isEnabled()) {
                    episodesManager.navigateToNextEpisode();
                }
            });
        }
        
        // Menu buttons
        if (menuToggleButton != null) {
            menuToggleButton.setOnClickListener(v -> toggleMenu());
        }
        
        if (closeMenuButton != null) {
            closeMenuButton.setOnClickListener(v -> {
                if (playersManager.getCurrentPlayerData() != null) {
                    playersManager.hideMenu();
                } else {
                    finish();
                }
            });
        }
        
        // Control buttons
        if (settingsButton != null) {
            settingsButton.setOnClickListener(v -> showSettingsDialog());
        }
        
        if (pipButton != null) {
            pipButton.setOnClickListener(v -> enterPictureInPictureMode());
        }
        
        if (ibClosePlayer != null) {
            ibClosePlayer.setOnClickListener(v -> finish());
        }
        
        if (episodesMenuButton != null) {
            episodesMenuButton.setOnClickListener(v -> toggleEpisodesInController());
        }
        
        // Setup player control buttons
        setupPlayerControlButtons();
    }
    
    /**
     * Настройка touch listener для overlay
     */
    private void setupOverlayTouchListener() {
        if (menuOverlay == null) return;
        
        menuOverlay.setVisibility(View.GONE);
        menuOverlay.setAlpha(0f);
        menuOverlay.setClickable(false);
        
        menuOverlay.setOnTouchListener((v, event) -> {
            if (!playersManager.isMenuVisible() && !commentsManager.isCommentsVisible()) return false;
            if (event.getAction() != MotionEvent.ACTION_DOWN) return false;
            
            int x = (int) event.getRawX();
            int y = (int) event.getRawY();
            boolean insidePanel = false;
            
            if (slidingMenuPanel != null && playersManager.isMenuVisible()) {
                Rect panelRect = new Rect();
                slidingMenuPanel.getGlobalVisibleRect(panelRect);
                insidePanel = panelRect.contains(x, y);
            }
            
            if (!insidePanel) {
                if (playersManager.isMenuVisible()) playersManager.hideMenu();
                if (commentsManager.isCommentsVisible()) commentsManager.hideCommentsPanel();
                return true;
            }
            return false;
        });
    }
    
    /**
     * Настройка player listener
     */
    private void setupPlayerListener() {
        if (player != null) {
            player.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    episodesManager.updateEpisodeNavigationButtonsVisibility();
                }
            });
        }
    }
    
    /**
     * Настройка gestures callback
     */
    private void setupGesturesCallback() {
        gesturesManager.setGestureCallback(new GesturesManager.GestureCallback() {
            @Override
            public void onSeekGesture(long seekPosition) {
                Log.d("VideoPlayer", "Seek gesture: " + seekPosition);
            }
            
            @Override
            public void onSpeedChange(float speed) {
                Log.d("VideoPlayer", "Speed change: " + speed);
            }
            
            @Override
            public void updatePlayLoadingIndicator(int playbackState) {
                VideoPlayerActivity.this.updatePlayLoadingIndicator(playbackState);
            }
        });
    }
    
    /**
     * Настройка начального состояния UI
     */
    private void configureInitialState() {
        // Hide controller buttons initially
        updateControllerButtonsVisibility(false);
        
        // Set initial episode navigation buttons visibility
        if (prevEpisodeButton != null) prevEpisodeButton.setVisibility(View.VISIBLE);
        if (nextEpisodeButton != null) nextEpisodeButton.setVisibility(View.VISIBLE);
        
        // Players control bar position is now managed by EpisodesManager
    }
    
    /**
     * Настройка callbacks для менеджеров
     */
    private void setupManagerCallbacks() {
        // Comments manager callbacks
        commentsManager.setVisibilityCallback(isVisible -> {
            if (episodesManager != null) {
                episodesManager.updateEpisodeNavigationButtonsVisibility();
            }
        });
        
        // Episodes manager callbacks
        episodesManager.setEpisodeSelectionCallback(this::onEpisodeSelected);
        episodesManager.setVisibilityCallback(isVisible -> {
            // Update controller visibility if needed
        });
        episodesManager.setDataCallback(new EpisodesManager.EpisodesDataCallback() {
            @Override
            public void onEpisodesLoaded(List<EpisodesListResponse.EpisodeItem> episodes) {
                Log.d("VideoPlayer", "Episodes loaded: " + episodes.size());
                
                episodesManager.findAndSetCurrentEpisodeFromUrl(animeUrl);
                
                EpisodesListResponse.EpisodeItem currentEpisode = episodesManager.getCurrentEpisode();
                if (currentEpisode != null) {
                    commentsManager.setCurrentEpisode(currentEpisode);
                    playersManager.loadPlayersForEpisode(currentEpisode.getId());
                } else {
                    initializeMenuWithoutAutoPlay();
                }
            }
            
            @Override
            public void onEpisodesError(String error) {
                Log.e("VideoPlayer", "Error loading episodes: " + error);
                Toast.makeText(VideoPlayerActivity.this, "Ошибка загрузки эпизодов: " + error, Toast.LENGTH_SHORT).show();
                initializeMenuWithoutAutoPlay();
            }
        });
        
        // Players manager callbacks
        playersManager.setPlayerSelectionCallback(this::onPlayerSelected);
        playersManager.setVisibilityCallback(isVisible -> {
            // Update controller visibility if needed
        });
        playersManager.setDataCallback(new PlayersManager.PlayersDataCallback() {
            @Override
            public void onPlayersLoaded(List<EpisodeResponse.PlayerData> players) {
                Log.d("VideoPlayer", "Players loaded: " + players.size());
            }
            
            @Override
            public void onPlayersError(String error) {
                Log.e("VideoPlayer", "Error loading players: " + error);
                Toast.makeText(VideoPlayerActivity.this, "Ошибка загрузки плееров: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * Запуск начальной загрузки
     */
    private void startInitialLoading() {
        playersManager.showMenu();
        if (menuLoadingOverlay != null) {
            menuLoadingOverlay.setVisibility(View.VISIBLE);
        }
    }

    private void toggleMenu() {
        playersManager.toggleMenu();
    }

    private void toggleEpisodesInController() {
        // Episodes are now managed by EpisodesManager through the side menu
        if (episodesManager != null) {
            episodesManager.toggleEpisodesMenu();
        }
    }

    private void hideEpisodesInController() {
        // Episodes are now managed by EpisodesManager through the side menu
        if (episodesManager != null) {
            episodesManager.hideEpisodesMenu();
        }
    }

    private void updatePlayerControlsState() {
        if (player == null) return;

        runOnUiThread(() -> {
            View controllerView = playerView.findViewById(R.id.exo_controller);
            if (controllerView != null) {
                ImageButton skipForwardButton = controllerView.findViewById(R.id.skipForwardButton);

                // Включаем/выключаем кнопки в зависимости от состояния плеера
                boolean canSeek = player.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM);

                if (skipForwardButton != null) {
                    skipForwardButton.setEnabled(canSeek);
                    skipForwardButton.setAlpha(canSeek ? 1.0f : 0.5f);
                }

                // Обновляем видимость play/pause кнопок
                updatePlayPauseButtonsVisibility();
            }
        });
    }

    private void updatePlayPauseButtonsVisibility() {
        if (player == null) return;

        runOnUiThread(() -> {
            View controllerView = playerView.findViewById(R.id.exo_controller);
            if (controllerView != null) {
                ImageButton playButton = controllerView.findViewById(R.id.exo_play);
                ImageButton pauseButton = controllerView.findViewById(R.id.exo_pause);

                boolean isPlaying = player.isPlaying();

                if (playButton != null) {
                    playButton.setVisibility(isPlaying ? View.GONE : View.VISIBLE);
                }

                if (pauseButton != null) {
                    pauseButton.setVisibility(isPlaying ? View.VISIBLE : View.GONE);
                }

                Log.d("PlayerControls", "Updated button visibility - isPlaying: " + isPlaying +
                        ", play visible: " + (playButton != null ? playButton.getVisibility() : "null") +
                        ", pause visible: " + (pauseButton != null ? pauseButton.getVisibility() : "null"));
            }
        });
    }

    private void onPlayerSelected(EpisodeResponse.PlayerData playerData) {
        Log.d("VideoPlayer", "Player selected: " + playerData.getPlayer());

        // Stop current playback before starting new one
        stopCurrentPlayback();

        // Update preferred quality for new player
        List<String> newQualities = playersManager.getAvailableQualities();
        if (!newQualities.isEmpty()) {
            // Set preferred quality to the highest available for new player
            String newPreferredQuality = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                newPreferredQuality = newQualities.getFirst();
            }
            preferredQuality = newPreferredQuality;
            Log.d("VideoPlayer", "Updated preferred quality to: " + newPreferredQuality + " for player: " + playerData.getPlayer());
        }

        // Update settings dialog if it's open
        if (currentSettingsBottomSheet != null) {
            if (!newQualities.isEmpty()) {
                currentSettingsBottomSheet.updateQualities(newQualities, preferredQuality);
            }
        }

        // Hide menu
        playersManager.hideMenu();

        // Route to appropriate player handler (start from beginning for new player)
        if (playerData.getPlayer() != null && "animelib".equalsIgnoreCase(playerData.getPlayer())) {
            handleAnimelibPlayer(playerData, 0);
        } else if (playerData.getPlayer() != null && "kodik".equalsIgnoreCase(playerData.getPlayer())) {
            handleKodikPlayer(playerData, 0);
        }

        updateAnimeInfoHeader();
    }

    private void onEpisodeSelected(EpisodesListResponse.EpisodeItem episode) {
        Log.d("VideoPlayer", "Episode selected: " + episode.getName());

        // Stop current playback if playing
        stopCurrentPlayback();

        // Hide episodes in controller
        hideEpisodesInController();

        // Update current episode in both managers
        episodesManager.setCurrentEpisode(episode);
        commentsManager.setCurrentEpisode(episode);
        commentsManager.resetCommentsOnEpisodeChange(true);

        // Save current episode through API
        if (currentAnimeId != null && episode.getNumber() != null) {
            apiService.saveCurrentEpisode(currentAnimeId, episode.getNumber(), new ApiService.CurrentEpisodeCallback() {
                @Override
                public void onCurrentEpisodeReceived(EpisodesListResponse.EpisodeItem episode) {
                    Log.d("EpisodeMemory", "Successfully saved current episode via API");
                }

                @Override
                public void onError(String error) {
                    Log.e("EpisodeMemory", "Failed to save current episode via API: " + error);
                }
            });
        }

        // Update navigation buttons visibility
        episodesManager.updateEpisodeNavigationButtonsVisibility();

        // Update episodes RecyclerView to highlight current episode
        episodesManager.updateEpisodesRecyclerView();
        updateAnimeInfoHeader();

        // Show loading and load players for this episode
        runOnUiThread(() -> {
            showLoading("Загрузка плееров для эпизода...");
            if (menuLoadingOverlay != null) menuLoadingOverlay.setVisibility(View.VISIBLE);
        });

        // Load players for the selected episode
        playersManager.loadPlayersForEpisode(episode.getId());
    }

    private void stopCurrentPlayback() {
        if (player != null) {
            Log.d("VideoPlayer", "Stopping current playback");
            player.stop();
            player.clearMediaItems();
        }
    }

    private void showSettingsDialog() {
        if (playersManager.getCurrentPlayerData() == null) {
            Toast.makeText(this, "Сначала выберите плеер", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> availableQualities = playersManager.getAvailableQualities();
        if (availableQualities.isEmpty()) {
            Toast.makeText(this, "Качества недоступны", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create dialog reference first
        SettingsBottomSheet dialog = new SettingsBottomSheet(this, availableQualities, preferredQuality,
                quality -> {
                    String oldQuality = preferredQuality;
                    preferredQuality = quality;
                    Log.d("VideoPlayer", "Selected quality: " + quality);

                    // If quality changed and player is currently playing, restart with new quality
                    if (!quality.equals(oldQuality) && player != null && player.isPlaying()) {
                        Log.d("VideoPlayer", "Restarting player with new quality");
                        restartPlayerWithNewQuality();
                    }
                },
                player != null ? player.getPlaybackParameters().speed : 1.0f,
                speed -> {
                    if (player != null) {
                        player.setPlaybackSpeed(speed);
                        Log.d("VideoPlayer", "Speed changed via dialog: " + speed);
                    }
                },
                enable4K,
                enabled -> {
                    enable4K = enabled;
                    // Save to database
                    apiService.save4KSetting(enabled);
                    Log.d("VideoPlayer", "4K enabled: " + enabled);
                    // Refresh qualities when 4K setting changes
                    List<String> newQualities = playersManager.getAvailableQualities();
                    if (!newQualities.isEmpty()) {
                        // Update preferred quality if current is not available
                        if (!newQualities.contains(preferredQuality)) {
                            preferredQuality = newQualities.get(0);
                        }
                        // Use currentSettingsDialog instead of dialog
                        if (currentSettingsBottomSheet != null) {
                            currentSettingsBottomSheet.updateQualities(newQualities, preferredQuality);
                        }
                    }
                },
                autoPlay,
                enabled -> {
                    autoPlay = enabled;
                    // Save to database
                    apiService.saveAutoPlaySetting(enabled);
                    Log.d("VideoPlayer", "AutoPlay enabled: " + enabled);
                },
                longSkipDuration,
                duration -> {
                    longSkipDuration = duration;
                    // Save to database
                    apiService.saveLongSkipDurationSetting(duration);
                    Log.d("VideoPlayer", "LongSkipDuration changed: " + duration);
                },
                currentTheme,
                themeMode -> {
                    currentTheme = themeMode;
                    // Apply theme immediately
                    ThemeUtils.applyTheme(themeMode);
                    apiService.saveThemeSetting(themeMode);
                    Log.d("VideoPlayer", "Theme changed: " + themeMode);
                });

        // Store reference to current dialog
        currentSettingsBottomSheet = dialog;

        // Apply settings when dialog is shown
        dialog.setOnShowListener(dialogInterface -> {
            // Apply current playback speed and volume
            applySettingsFromDialog(dialog);
        });

        dialog.show();
    }

    private void restartPlayerWithNewQuality() {
        EpisodeResponse.PlayerData currentPlayerData = playersManager.getCurrentPlayerData();
        if (currentPlayerData == null) {
            return;
        }

        // Save current position
        long currentPosition = player != null ? player.getCurrentPosition() : 0;

        // Stop current playback
        stopCurrentPlayback();

        // Update video URL with new quality for Animelib
        if ("animelib".equalsIgnoreCase(currentPlayerData.getPlayer())) {
            if (currentPlayerData.getVideo() != null && currentPlayerData.getVideo().getQuality() != null) {
                // Find the new quality URL
                for (EpisodeResponse.QualityData ignored : currentPlayerData.getVideo().getQuality()) {
                    if (preferredQuality != null) {
                        preferredQuality.replace("p", "");
                    }
                }
            }
            handleAnimelibPlayer(currentPlayerData, currentPosition);
        } else if ("kodik".equalsIgnoreCase(currentPlayerData.getPlayer())) {
            // For Kodik, we need to restart with new HLS URL
            if (currentKodikResponse != null && currentKodikResponse.getData() != null) {
                String qualityKey = preferredQuality != null ? preferredQuality.replace("p", "") : "1080";
                if (currentKodikResponse.getData().containsKey(qualityKey) &&
                        Objects.requireNonNull(currentKodikResponse.getData().get(qualityKey)).length > 0) {
                    String newHlsUrl = Objects.requireNonNull(currentKodikResponse.getData().get(qualityKey))[0].getSrc();
                    if (newHlsUrl != null && !newHlsUrl.isEmpty()) {
                        currentVideoUrl = newHlsUrl;
                        Log.d("VideoPlayer", "Updated HLS URL for quality " + preferredQuality + ": " + currentVideoUrl);
                    }
                }
            }
            handleKodikPlayer(currentPlayerData, currentPosition);
        }
    }

    private void applySettingsFromDialog(SettingsBottomSheet dialog) {
        if (player != null) {
            // Apply playback speed
            float speed = dialog.getCurrentPlaybackSpeed();
            player.setPlaybackSpeed(speed);
            Log.d("VideoPlayer", "Applied playback speed: " + speed);
        }
    }

    private void showLoading(String message) {
        runOnUiThread(() -> {
            if (loadingOverlay != null) {
                loadingOverlay.setVisibility(View.VISIBLE);
                TextView textView = loadingOverlay.findViewById(R.id.loadingText);
                if (textView != null) {
                    textView.setText(message);
                }
            }
        });
    }

    private void hideLoading() {
        runOnUiThread(() -> {
            if (loadingOverlay != null) {
                loadingOverlay.setVisibility(View.GONE);
            }
        });
    }

    private void initializePlayer() {
        // Create ExoPlayer with custom data source for video requests
        player = new ExoPlayer.Builder(this)
                .setSeekBackIncrementMs(10000)
                .setSeekForwardIncrementMs(10000)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(httpDataSourceFactory))
                .build();

        playerView.setPlayer(player);

        // Ensure controller is properly configured for play/pause buttons
        playerView.setUseController(true);
        updateControllerAutoHide();

        Log.d("PlayerInit", "ExoPlayer bound to PlayerView with controller enabled");
        
        // Update gestures manager with new player
        gesturesManager.updatePlayer(player);

        // Setup all player control buttons
        setupPlayerControlButtons();

        // Create media item
        MediaItem mediaItem = MediaItem.fromUri(currentVideoUrl);
        player.setMediaItem(mediaItem);
        player.prepare();

        // Start playback
        player.play();

        // Add listener for errors
        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                Log.e("VideoPlayer", "Playback error: " + error.getMessage(), error);
                String errorMsg = "Ошибка воспроизведения";

                if (error.getMessage().contains("403")) {
                    errorMsg += ": доступ запрещен (403). Пробуем без токена...";
                    Log.d("VideoPlayer", "403 Forbidden - trying URL without auth token");

                    // Try loading video without auth token
                    if (currentVideoUrl.contains("video1.cdnlibs.org/.%D0%B0s")) {
                        String urlWithoutToken = currentVideoUrl.replace("https://video1.cdnlibs.org/.%D0%B0s", "https://video1.cdnlibs.org");
                        Log.d("VideoPlayer", "Retrying with URL: " + urlWithoutToken);
                        retryWithUrl(urlWithoutToken);
                        return; // Don't show error toast yet
                    }
                } else if (error.getMessage().contains("404")) {
                    errorMsg += ": видео не найдено (404). Проверьте URL: " + currentVideoUrl;
                } else {
                    errorMsg += ": " + error.getMessage();
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Ошибка воспроизведения", errorMsg);
                    clipboard.setPrimaryClip(clip);
                }

                Toast.makeText(VideoPlayerActivity.this, errorMsg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void retryWithUrl(String newUrl) {
        runOnUiThread(() -> {
            Log.d("VideoPlayer", "Retrying playback with new URL: " + newUrl);
            currentVideoUrl = newUrl;

            // Check if this is an HLS URL (contains .m3u8 or hls in path)
            boolean isHls = newUrl.contains(".m3u8") || newUrl.contains(":hls:");

            if (isHls) {
                // For HLS, reinitialize the player with HLS support
                Log.d("VideoPlayer", "Retrying with HLS player");
                initializeHlsPlayer(newUrl);
            } else {
                // For regular video, just change the media item
                Log.d("VideoPlayer", "Retrying with regular player");
                MediaItem mediaItem = MediaItem.fromUri(newUrl);
                if (player != null) {
                    player.setMediaItem(mediaItem);
                    player.prepare();
                    player.play();
                } else {
                    initializePlayer();
                }
            }

            Toast.makeText(this, "Повторная попытка загрузки видео...", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadAnimeFromUrl(String url) {
        apiService.loadAnimeFromUrl(url, new ApiService.EpisodeDataCallback() {
            @Override
            public void onEpisodeDataReceived(EpisodeResponse response) {
                runOnUiThread(() -> {
                    hideLoading();
                    if (response.getData() != null && response.getData().getPlayers() != null) {
                        showPlayerSelectionDialog(response.getData().getPlayers());
                    } else {
                        Toast.makeText(VideoPlayerActivity.this, "Плееры не найдены", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    hideLoading();
                    Toast.makeText(VideoPlayerActivity.this, error, Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void showPlayerSelectionDialog(List<EpisodeResponse.PlayerData> players) {
        hideLoading();

        // Store all players data in PlayersManager
        playersManager.setPlayersData(players);
        currentAnimeId = apiService.extractAnimeId(animeUrl);

        // Load episodes only if not loaded yet; otherwise init menu with currentEpisode
        if (episodesManager.getEpisodes().isEmpty() && currentAnimeId != null) {
            loadEpisodes(currentAnimeId);
        } else {
            // Episodes already loaded, ensure CommentsManager has current episode
            EpisodesListResponse.EpisodeItem currentEpisode = episodesManager.getCurrentEpisode();
            if (currentEpisode != null) {
                Log.d("VideoPlayer", "Setting current episode in CommentsManager (episodes already loaded): " + currentEpisode.getNumber());
                commentsManager.setCurrentEpisode(currentEpisode);
            }
            initializeMenuWithoutAutoPlay();
        }
    }

    private void handleAnimelibPlayer(EpisodeResponse.PlayerData playerData, long seekToPosition) {
        Log.d("AnimelibPlayer", "Handling Animelib player");
        if (playerData.getVideo() != null && playerData.getVideo().getQuality() != null && !playerData.getVideo().getQuality().isEmpty()) {
            // Select quality based on preference or best available
            EpisodeResponse.QualityData selectedQuality = null;
            String preferredQualityValue = preferredQuality != null ? preferredQuality.replace("p", "") : null;

            if (preferredQualityValue != null) {
                try {
                    int preferredQualityInt = Integer.parseInt(preferredQualityValue);
                    // Find exact quality match (include 4K if enabled)
                    for (EpisodeResponse.QualityData quality : playerData.getVideo().getQuality()) {
                        if (quality.getQuality() == preferredQualityInt) {
                            // Include 4K only if enabled
                            if (preferredQualityInt == 2160 && !enable4K) {
                                continue;
                            }
                            selectedQuality = quality;
                            Log.d("AnimelibPlayer", "Using preferred quality: " + preferredQualityInt + "p");
                            break;
                        }
                    }
                } catch (NumberFormatException e) {
                    Log.w("AnimelibPlayer", "Invalid preferred quality format: " + preferredQuality);
                }
            }

            // If no preferred quality found or no preference set, select best quality
            if (selectedQuality == null) {
                selectedQuality = null;
                Log.d("AnimelibPlayer", "Available qualities:");
                for (EpisodeResponse.QualityData quality : playerData.getVideo().getQuality()) {
                    String q = String.valueOf(quality.getQuality());
                    Log.d("AnimelibPlayer", "  - " + quality.getQuality() + "p: " + quality.getHref());
                    // Include 4K only if enabled
                    if (enable4K || !"2160".equals(q)) {
                        if (selectedQuality == null || quality.getQuality() > selectedQuality.getQuality()) {
                            selectedQuality = quality;
                        }
                    }
                }
                // Set preferred quality to the best available
                if (preferredQuality == null && selectedQuality != null) {
                    String quality = String.valueOf(selectedQuality.getQuality());
                    preferredQuality = quality + "p";
                }
            }

            if (selectedQuality == null) {
                Log.e("AnimelibPlayer", "No suitable quality found");
                return;
            }

            String videoUrl = selectedQuality.getHref();
            Log.d("AnimelibPlayer", "Selected quality: " + selectedQuality.getQuality() + "p, URL: " + videoUrl);

            // Ensure URL is absolute - use video CDN domain
            if (!videoUrl.startsWith("http")) {
                // Use video1.cdnlibs.org for video files with access token
                String originalPath = videoUrl;
                videoUrl = "https://video1.cdnlibs.org/.%D0%B0s" + videoUrl;
                Log.d("AnimelibPlayer", "Converted relative path '" + originalPath + "' to '" + videoUrl + "'");

                // Alternative: try without token if 403 occurs
                // videoUrl = "https://video1.cdnlibs.org" + videoUrl;
                // Log.d("AnimelibPlayer", "Alternative URL without token: " + videoUrl);
            } else {
                Log.d("AnimelibPlayer", "URL is already absolute: " + videoUrl);
            }

            Log.d("AnimelibPlayer", "Final video URL: " + videoUrl);
            currentVideoUrl = videoUrl;
            initializePlayer();
            if (seekToPosition > 0) {
                player.seekTo(seekToPosition);
            }
        } else {
            Toast.makeText(this, "Видео недоступно", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void handleKodikPlayer(EpisodeResponse.PlayerData playerData, long seekToPosition) {
        Log.d("KodikPlayer", "Handling Kodik player");
        if (playerData.getSrc() != null && !playerData.getSrc().isEmpty()) {
            String kodikSrc = playerData.getSrc();
            if (!kodikSrc.startsWith("http")) {
                kodikSrc = "https:" + kodikSrc;
            }
            Log.d("KodikPlayer", "Kodik src: " + kodikSrc);
            fetchKodikVideoLinks(kodikSrc, seekToPosition);
        } else {
            Log.w("KodikPlayer", "No src found in Kodik player data");
            Toast.makeText(this, "Ошибка: ссылка Kodik недоступна", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void fetchKodikVideoLinks(String kodikSrc, long seekToPosition) {
        Log.d("KodikAPI", "Fetching HLS links for Kodik src: " + kodikSrc);
        runOnUiThread(() -> showLoading("Получение HLS ссылок..."));

        apiService.fetchKodikVideoLinksUnsafe(kodikSrc, new ApiService.KodikVideoCallback() {
            @Override
            public void onKodikVideoReceived(KodikResponse response) {
                runOnUiThread(() -> {
                    hideLoading();
                    startHlsPlayer(response, seekToPosition);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    hideLoading();
                    Toast.makeText(VideoPlayerActivity.this, error, Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void loadEpisodes(String animeId) {
        Log.d("EpisodesAPI", "Loading episodes for anime_id: " + animeId);
        
        // Use EpisodesManager to load episodes
        episodesManager.loadEpisodes(animeId);
    }

    private void initializeMenuWithoutAutoPlay() {
        // Players are now managed by PlayersManager
        episodesManager.updateEpisodeNavigationButtonsVisibility();
        episodesManager.updateEpisodesRecyclerView(); // Call this last to ensure currentEpisode is set

        // PlayersManager handles auto-selection and menu display
        // Ensure loading overlay is hidden once players are available
        if (menuLoadingOverlay != null) menuLoadingOverlay.setVisibility(View.GONE);
        if (menuLoadingIndicator != null) menuLoadingIndicator.setVisibility(View.GONE);
    }

    private void updateAnimeInfoHeader() {
        if (animeTitleView == null || currentEpisodeNumberView == null) return;
        String slugOrId = apiService.extractAnimeSlug(animeUrl);
        if (slugOrId == null) return;

        apiService.fetchAnimeInfo(slugOrId, new ApiService.AnimeInfoCallback() {
            @Override
            public void onAnimeInfoReceived(AnimeInfoResponse response) {
                runOnUiThread(() -> {
                    if (response != null && response.getData() != null) {
                        String rus = response.getData().getRus_name();
                        animeTitleView.setText(rus != null ? rus : "");
                    }

                    EpisodeResponse.PlayerData currentPlayerData = playersManager.getCurrentPlayerData();
                    String tm = (currentPlayerData != null && currentPlayerData.getTeam() != null)
                            ? currentPlayerData.getTeam().getName() : null;
                    EpisodesListResponse.EpisodeItem currentEpisode = episodesManager.getCurrentEpisode();
                    String ep = (currentEpisode != null) ? currentEpisode.getNumber() : null;
                    String em = (currentEpisode != null && currentEpisode.getName() != null && !Objects.equals(currentEpisode.getName(), ""))
                            ? currentEpisode.getName() : null;

                    if (currentTeamName != null) currentTeamName.setText(tm != null ? tm : "");
                    if (currentEpisodeNumberView != null)
                        currentEpisodeNumberView.setText(ep != null ? (ep + " серия") : "");
                    if (currentEpisodeName != null)
                        currentEpisodeName.setText(em != null ? (", " + em) : "");
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    EpisodesListResponse.EpisodeItem currentEpisode = episodesManager.getCurrentEpisode();
                    String ep = currentEpisode != null ? currentEpisode.getNumber() : null;
                    currentEpisodeNumberView.setText(ep != null ? (ep + " серия") : "");
                });
            }
        });
    }

    private void startHlsPlayer(KodikResponse kodikResponse, long seekToPosition) {
        // Save Kodik response for quality selection
        currentKodikResponse = kodikResponse;

        // Select quality based on preference or best available
        String hlsUrl = null;
        String preferredQualityKey = preferredQuality != null ? preferredQuality.replace("p", "") : null;

        if (preferredQualityKey != null && kodikResponse.getData().containsKey(preferredQualityKey) &&
                kodikResponse.getData().get(preferredQualityKey).length > 0) {
            hlsUrl = kodikResponse.getData().get(preferredQualityKey)[0].getSrc();
            Log.d("KodikPlayer", "Using preferred quality: " + preferredQualityKey + "p");
        } else {
            // Fallback to best quality available (prefer 720p, then 480p, then 360p)
            if (kodikResponse.getData().containsKey("720") && kodikResponse.getData().get("720").length > 0) {
                hlsUrl = kodikResponse.getData().get("720")[0].getSrc();
                if (preferredQuality == null) preferredQuality = "720p";
            } else if (kodikResponse.getData().containsKey("480") && kodikResponse.getData().get("480").length > 0) {
                hlsUrl = kodikResponse.getData().get("480")[0].getSrc();
                if (preferredQuality == null) preferredQuality = "480p";
            } else if (kodikResponse.getData().containsKey("360") && kodikResponse.getData().get("360").length > 0) {
                hlsUrl = kodikResponse.getData().get("360")[0].getSrc();
                if (preferredQuality == null) preferredQuality = "360p";
            }
        }

        if (hlsUrl != null) {
            // Ensure URL is absolute
            if (!hlsUrl.startsWith("http")) {
                hlsUrl = "https:" + hlsUrl;
            }

            Log.d("HlsPlayer", "Starting HLS playback with URL: " + hlsUrl);
            currentVideoUrl = hlsUrl;
            initializeHlsPlayer(hlsUrl);
            if (seekToPosition > 0) {
                player.seekTo(seekToPosition);
            }
        } else {
            Toast.makeText(this, "HLS видео недоступно", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeHlsPlayer(String hlsUrl) {
        // Create HLS media source with OkHttp data source
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request.Builder requestBuilder = original.newBuilder()
                            .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1")
                            .header("Referer", "https://v3.animelib.org/")
                            .header("Accept", "video/mp4,video/*,*/*")
                            .header("Accept-Encoding", "identity;q=1, *;q=0")
                            .header("Accept-Language", "ru,en;q=0.9,de;q=0.8,zh;q=0.7")
                            .header("Origin", "https://v3.animelib.org")
                            .header("Sec-Fetch-Dest", "video")
                            .header("Sec-Fetch-Mode", "cors")
                            .header("Sec-Fetch-Site", "cross-site")
                            .header("Priority", "i");

                    Request request = requestBuilder.build();
                    return chain.proceed(request);
                })
                .build();

        OkHttpDataSource.Factory okHttpDataSourceFactory = new OkHttpDataSource.Factory(okHttpClient);

        HlsMediaSource hlsMediaSource = new HlsMediaSource.Factory(okHttpDataSourceFactory)
                .createMediaSource(MediaItem.fromUri(hlsUrl));

        // Create ExoPlayer
        player = new ExoPlayer.Builder(this)
                .setSeekBackIncrementMs(10000)
                .setSeekForwardIncrementMs(10000)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(okHttpDataSourceFactory))
                .build();

        playerView.setPlayer(player);

        // Ensure controller is properly configured for play/pause buttons
        playerView.setUseController(true);
        updateControllerAutoHide();

        Log.d("HlsPlayerInit", "HLS ExoPlayer bound to PlayerView with controller enabled");

        player.setMediaSource(hlsMediaSource);
        player.prepare();
        player.play();

        // Re-setup all player control buttons for the new player
        setupPlayerControlButtons();

        // Add listener for errors
        player.addListener(new Player.Listener() {
            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                Log.e("HlsPlayer", "HLS playback error: " + error.getMessage(), error);
                String errorMsg = "Ошибка HLS воспроизведения";

                if (error.getMessage().contains("403")) {
                    errorMsg += ": доступ запрещен (403). Проверьте HLS ссылку: " + currentVideoUrl;
                } else if (error.getMessage().contains("404")) {
                    errorMsg += ": HLS плейлист не найден (404)";
                } else {
                    errorMsg += ": " + error.getMessage();
                }

                Toast.makeText(VideoPlayerActivity.this, errorMsg, Toast.LENGTH_LONG).show();
            }
        });

        Log.d("HlsPlayer", "HLS player initialized and started");
    }

    private void setupPlayerControlButtons() {
        View controllerView = playerView.findViewById(R.id.exo_controller);
        if (controllerView == null) {
            Log.w("PlayerControls", "Controller view not found");
            return;
        }

        // НЕ трогаем стандартные ExoPlayer кнопки (exo_play, exo_pause)
        // ExoPlayer сам их обрабатывает автоматически

        // Настраиваем только наши кастомные кнопки
        ImageButton skipForwardButton = controllerView.findViewById(R.id.skipForwardButton);

        // Setup skip forward button (1 минута 25 секунд)
        if (skipForwardButton != null) {
            skipForwardButton.setOnClickListener(v -> {
                Log.d("PlayerControls", "Skip forward clicked (1:25)");
                if (player != null && player.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)) {
                    long currentPosition = player.getCurrentPosition();
                    long skipDuration = longSkipDuration * 1000L; // Convert seconds to milliseconds
                    long newPosition = currentPosition + skipDuration;
                    long duration = player.getDuration();

                    // Не даем перемотать дальше конца видео
                    if (newPosition > duration && duration > 0) {
                        newPosition = duration;
                    }

                    player.seekTo(newPosition);
                    Log.d("PlayerControls", "Skipped forward to: " + (newPosition / 1000) + "s (+" + (skipDuration / 1000) + "s)");
                } else {
                    Log.w("PlayerControls", "Skip forward not available");
                }
            });
        }

        // Добавляем listener для синхронизации состояния всех кнопок
        if (player != null) {
            player.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    Log.d("PlayerControls", "Playback state changed: " + playbackState);
                    updatePlayerControlsState();
                    episodesManager.updateEpisodeNavigationButtonsVisibility();
                    updatePlayLoadingIndicator(playbackState);

                    // Handle auto-play next episode
                    if (playbackState == Player.STATE_ENDED && autoPlay) {
                        Log.d("PlayerControls", "Video ended, checking for next episode");
                        episodesManager.navigateToNextEpisode();
                    }
                }

                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    Log.d("PlayerControls", "Is playing changed: " + isPlaying);
                    updatePlayerControlsState();
                }

                @Override
                public void onPlayerError(@NonNull PlaybackException error) {
                    Log.e("PlayerControls", "Player error: " + error.getMessage());
                    updatePlayerControlsState();
                }
            });
        }

        // Инициализируем состояние кнопок
        updatePlayerControlsState();

        // Устанавливаем начальную видимость play/pause кнопок
        updatePlayPauseButtonsVisibility();

        // sync loading indicator initially if needed
        if (player != null) updatePlayLoadingIndicator(player.getPlaybackState());

        // Проверяем, что ExoPlayer нашел стандартные кнопки
        ImageButton exoPlayButton = controllerView.findViewById(R.id.exo_play);
        ImageButton exoPauseButton = controllerView.findViewById(R.id.exo_pause);

        Log.d("PlayerControls", "ExoPlayer buttons found - Play: " + (exoPlayButton != null) +
                ", Pause: " + (exoPauseButton != null));

        if (exoPlayButton != null) {
            Log.d("PlayerControls", "Play button visibility: " + exoPlayButton.getVisibility() +
                    ", enabled: " + exoPlayButton.isEnabled() +
                    ", clickable: " + exoPlayButton.isClickable());

            // Добавляем обработчик с управлением видимостью
            exoPlayButton.setOnClickListener(v -> {
                Log.d("PlayerControls", "Play button clicked!");
                if (player != null) {
                    player.play();
                    // Обновляем видимость кнопок с небольшой задержкой
                    playerView.postDelayed(this::updatePlayPauseButtonsVisibility, 100);
                }
            });
        }

        if (exoPauseButton != null) {
            Log.d("PlayerControls", "Pause button visibility: " + exoPauseButton.getVisibility() +
                    ", enabled: " + exoPauseButton.isEnabled() +
                    ", clickable: " + exoPauseButton.isClickable());

            // Добавляем обработчик с управлением видимостью
            exoPauseButton.setOnClickListener(v -> {
                Log.d("PlayerControls", "Pause button clicked!");
                if (player != null) {
                    player.pause();
                    // Обновляем видимость кнопок с небольшой задержкой
                    playerView.postDelayed(this::updatePlayPauseButtonsVisibility, 100);
                }
            });
        }

        Log.d("PlayerControls", "Player control buttons setup completed");
    }

    private void updatePlayLoadingIndicator(int playbackState) {
        View controllerView = playerView.findViewById(R.id.exo_controller);
        if (controllerView == null) return;
        View play = controllerView.findViewById(R.id.exo_play);
        View pause = controllerView.findViewById(R.id.exo_pause);
        View spinner = controllerView.findViewById(R.id.playLoadingIndicator);
        if (spinner == null) return;

        boolean buffering = playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_READY && player != null && !player.isPlaying();

        if (playbackState == Player.STATE_BUFFERING) {
            // show spinner, hide play/pause
            spinner.setVisibility(View.VISIBLE);
            if (play != null) play.setVisibility(View.GONE);
            if (pause != null) pause.setVisibility(View.GONE);
        } else {
            spinner.setVisibility(View.GONE);
            // restore according to isPlaying
            if (player != null) {
                boolean isPlaying = player.isPlaying();
                if (play != null) play.setVisibility(isPlaying ? View.GONE : View.VISIBLE);
                if (pause != null) pause.setVisibility(isPlaying ? View.VISIBLE : View.GONE);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (player != null) {
            player.play();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clear screen keep flag
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (player != null) {
            player.release();
            player = null;
        }
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        if (apiService != null) {
            apiService.shutdown();
        }
        if (commentsManager != null) {
            commentsManager.cleanup();
        }
        if (playersManager != null) {
            playersManager.cleanup();
        }
        if (gesturesManager != null) {
            gesturesManager.cleanup();
        }
    }

    @Override
    public void onBackPressed() {
        // If video is playing and not in PiP mode, enter PiP instead of closing
        if (player != null && player.isPlaying() && !isInPictureInPictureMode) {
            enterPictureInPictureMode();
            return;
        }

        // Otherwise, close the activity
        super.onBackPressed();
    }
}


