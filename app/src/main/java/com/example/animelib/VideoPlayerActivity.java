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
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Rational;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.C;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.LoadControl;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.trackselection.TrackSelector;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.animelib.models.AnimeInfoResponse;

import com.example.animelib.api.ApiService;
import com.example.animelib.settings.SettingsBottomSheet;
import com.example.animelib.managers.BookmarkManager;
import com.example.animelib.managers.CommentsManager;
import com.example.animelib.managers.EpisodesManager;
import com.example.animelib.managers.PlayersManager;
import com.example.animelib.managers.GesturesManager;
import com.example.animelib.managers.VerticalGesturesManager;
import com.example.animelib.managers.TimecodeManager;
import com.example.animelib.managers.AmbientLightManager;
import com.example.animelib.adapters.HorizontalRelatedTitlesAdapter;
import com.example.animelib.managers.RelatedTitlesManager;
import com.example.animelib.util.ThemeUtils;
import com.example.animelib.models.EpisodeResponse;
import com.example.animelib.models.EpisodesListResponse;
import com.example.animelib.models.KodikResponse;
import com.example.animelib.models.RelatedTitlesResponse;
import com.google.android.material.button.MaterialButton;
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
    private ImageButton menuToggleFullscreen;
    private LinearLayout slidingMenuPanel;
    private View menuLoadingIndicator;
    private View menuLoadingOverlay;
    
    // Draggable panels
    private com.example.animelib.ui.DraggableSidePanel menuPanelContainer;
    private com.example.animelib.ui.DraggableSidePanel commentsPanelContainer;
    
    // Anime info placeholder
    private View animeInfoPlaceholder;
    private ImageView animeInfoPoster;
    private TextView animeInfoTitle;
    private TextView animeInfoOriginalTitle;
    private TextView animeInfoYear;
    private TextView animeInfoType;
    private TextView animeInfoStatus;
    private TextView animeInfoRating;
    private TextView animeInfoEpisodes;
    private TextView animeInfoAge;
    private TextView animeInfoReleaseDate;
    private TextView animeInfoShikimori;
    
    // Next episode overlay
    private View nextEpisodeOverlay;
    private TextView nextEpisodeNumber;
    private TextView nextEpisodeCountdown;
    private com.google.android.material.button.MaterialButton cancelNextEpisodeButton;
    private com.google.android.material.button.MaterialButton playNextEpisodeButton;
    private Handler nextEpisodeHandler;
    private Runnable nextEpisodeRunnable;
    private int countdownSeconds = 7;
    
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
    private TextView emptyCommentsText;
    private TextView seekPreviewText;
    private TextView holdSpeedToast;
    private ImageButton pipButton;
    private RecyclerView episodesHorizontalRecyclerView;
    private ImageButton commentsButton;
    private ImageButton bookmarkButton;
    private View skipIndicatorLeft;
    private View skipIndicatorRight;

    // Comments manager
    private CommentsManager commentsManager;
    private SettingsBottomSheet currentSettingsBottomSheet;

    // Episodes manager
    private EpisodesManager episodesManager;
    
    // Episodes UI components (for EpisodesManager)
    private ImageButton episodesMenuButton;
    
    // Related titles components
    private FrameLayout relatedTitlesOverlay;
    private View relatedTitlesDimOverlay;
    private RecyclerView relatedTitlesRecyclerView;
    private HorizontalRelatedTitlesAdapter relatedTitlesAdapter;
    private RelatedTitlesManager relatedTitlesManager;

    // Players manager
    private PlayersManager playersManager;
    
    // Gestures manager
    private GesturesManager gesturesManager;
    private VerticalGesturesManager verticalGesturesManager;
    
    // Timecode manager
    private TimecodeManager timecodeManager;
    
    // Ambient light manager
    private AmbientLightManager ambientLightManager;
    private com.example.animelib.ui.AmbientLightView ambientLightView;

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
    
    // Fullscreen state
    private boolean isFullscreenMode = false;

    // Player data is now managed by PlayersManager
    private KodikResponse currentKodikResponse;
    private String currentAnimeId;
    private long bookmarkTimecode = 0; // Таймкод из закладки в миллисекундах
    private long savedPlayerPosition = 0; // Сохраненная позиция при смене плеера
    private boolean autoBookmarkSaved = false; // Флаг для предотвращения дублирования автосохранения

    // User preferences are now managed by PlayersManager
    private String preferredQuality; // preferred quality (e.g., "720", "480", etc.)
    private boolean enable4K = false;
    private boolean enableAmbientLight = false;
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
        ambientLightView = findViewById(R.id.ambientLightView);

        // Устанавливаем fitsSystemWindows программно для предотвращения сброса при рестарте
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            rootView.setFitsSystemWindows(false);
        }

        // Configure PlayerView to show controls for shorter time
        updateControllerAutoHide();
        
        // Disable ExoPlayer's default controller animations to use our custom alpha animation
        playerView.setControllerAnimationEnabled(false);

        // Initialize network components
        executor = Executors.newSingleThreadExecutor();
        apiService = new ApiService(this);

        // Load and apply theme
        loadAndApplyTheme();

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
        verticalGesturesManager = new VerticalGesturesManager(this);
        
        // Устанавливаем взаимные ссылки для координации
        gesturesManager.setVerticalGesturesManager(verticalGesturesManager);
        verticalGesturesManager.setGesturesManager(gesturesManager);
        
        // Initialize timecode manager
        timecodeManager = new TimecodeManager(this);

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
            enableAmbientLight = apiService.loadAmbientLightSetting();
            autoPlay = apiService.loadAutoPlaySetting();
            longSkipDuration = apiService.loadLongSkipDurationSetting();
            currentTheme = apiService.loadThemeSetting();
            Log.d("VideoPlayer", "Loaded settings - 4K: " + enable4K + ", AmbientLight: " + enableAmbientLight + ", AutoPlay: " + autoPlay + ", SkipDuration: " + longSkipDuration + ", Theme: " + currentTheme);
            
            // Update skip indicators text, 4K setting, and ambient light on UI thread
            runOnUiThread(() -> {
                if (gesturesManager != null) {
                    gesturesManager.updateSkipDurationText(longSkipDuration);
                }
                if (playersManager != null) {
                    playersManager.setEnable4K(enable4K);
                }
                if (ambientLightManager != null) {
                    ambientLightManager.setEnabled(enableAmbientLight);
                }
            });
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

    /**
     * Безопасно вызывает код в главном потоке
     */
    private void safeRunOnUiThread(Runnable runnable) {
        try {
            runOnUiThread(runnable);
        } catch (Exception e) {
            Log.e("VideoPlayer", "Error calling UI thread", e);
            // Fallback - вызываем в текущем потоке
            try {
                runnable.run();
            } catch (Exception ex) {
                Log.e("VideoPlayer", "Error in fallback callback", ex);
            }
        }
    }

    private void loadAndApplyTheme() {
        executor.execute(() -> {
            try {
                // Получаем тему из базы данных
                int themeMode = apiService.loadThemeSetting();
                Log.d("VideoPlayerTheme", "Loaded theme from database: " + themeMode);
                
                // Также проверяем SharedPreferences
                int sharedPrefTheme = ThemeUtils.getSavedThemePreference(this);
                Log.d("VideoPlayerTheme", "Loaded theme from SharedPreferences: " + sharedPrefTheme);
                
                // Используем тему из базы данных, если она есть, иначе из SharedPreferences
                int finalTheme = themeMode != 0 ? themeMode : sharedPrefTheme;
                
                // Применяем тему в главном потоке
                safeRunOnUiThread(() -> {
                    ThemeUtils.applyThemeToActivity(VideoPlayerActivity.this, finalTheme);
                    Log.d("VideoPlayerTheme", "Theme applied on startup: " + finalTheme);
                });
                
            } catch (Exception e) {
                Log.e("VideoPlayerTheme", "Failed to load and apply theme", e);
                // Применяем тему по умолчанию в главном потоке
                safeRunOnUiThread(() -> {
                    ThemeUtils.applyThemeToActivity(VideoPlayerActivity.this, 0);
                });
            }
        });
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, android.content.res.Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        this.isInPictureInPictureMode = isInPictureInPictureMode;

        if (isInPictureInPictureMode) {
            // Entering PiP mode
            if (player != null && !player.isPlaying()) {
                player.play();
                Log.d("VideoPlayer", "Resumed playback in PiP mode");
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
            // Автоматически сохраняем закладку перед переходом в PiP
            autoSaveBookmark();
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
     * Переключает режим полноэкранного видео (растягивание по ширине с обрезкой краев)
     */
    private void toggleFullscreenMode() {
        isFullscreenMode = !isFullscreenMode;
        
        if (playerView != null) {
            if (isFullscreenMode) {
                // Включаем режим обрезки - растягиваем по ширине экрана
                playerView.setResizeMode(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
                
                // Меняем иконку на exit fullscreen
                if (menuToggleFullscreen != null) {
                    menuToggleFullscreen.setImageResource(R.drawable.ic_fullscreen_exit);
                }
                
                Log.d("VideoPlayer", "Fullscreen mode enabled - video zoomed to fit width");
            } else {
                // Выключаем режим обрезки - показываем видео полностью
                playerView.setResizeMode(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT);
                
                // Меняем иконку на обычный fullscreen
                if (menuToggleFullscreen != null) {
                    menuToggleFullscreen.setImageResource(R.drawable.ic_fullscreen);
                }
                
                Log.d("VideoPlayer", "Fullscreen mode disabled - video fitted to screen");
            }
        }
    }

    /**
     * Обновление настроек автоматического скрытия контроллера
     */
    private void updateControllerAutoHide() {
        updateControllerAutoHide(shouldAutoHideControls);
    }
    
    /**
     * Обновление настроек автоматического скрытия контроллера с принудительным значением
     */
    private void updateControllerAutoHide(boolean enableAutoHide) {
        if (playerView != null) {
            if (enableAutoHide) {
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

    /**
     * Открыть панель меню плееров
     */
    public void openMenuPanel() {
        if (menuPanelContainer != null) {
            menuPanelContainer.openPanel();
        }
    }
    
    /**
     * Закрыть панель меню плееров
     */
    public void closeMenuPanel() {
        if (menuPanelContainer != null) {
            menuPanelContainer.closePanel();
        }
    }
    
    /**
     * Открыть панель комментариев
     */
    public void openCommentsPanel() {
        if (commentsPanelContainer != null) {
            commentsPanelContainer.openPanel();
        }
    }
    
    /**
     * Закрыть панель комментариев
     */
    public void closeCommentsPanel() {
        if (commentsPanelContainer != null) {
            commentsPanelContainer.closePanel();
        }
    }
    
    
    /**
     * Проверить открыта ли панель меню
     */
    public boolean isMenuPanelOpen() {
        return menuPanelContainer != null && menuPanelContainer.isOpen();
    }
    
    /**
     * Проверить открыта ли панель комментариев
     */
    public boolean isCommentsPanelOpen() {
        return commentsPanelContainer != null && commentsPanelContainer.isOpen();
    }
    
    
    private void hideAllUI() {
        // Hide all UI elements except the player
        if (playerView != null) {
            playerView.setUseController(false);
        }

        // Hide menu and other panels
        if (menuPanelContainer != null) {
            menuPanelContainer.closePanel();
        }
        // Hide comments panel if it's currently visible
        if (commentsManager.isCommentsVisible()) {
            commentsManager.hideCommentsPanel();
        }
        // Episodes are now managed by EpisodesManager
        if (episodesManager != null) {
            // НЕ скрываем playersControlBar в PiP режиме, только остальные элементы
            episodesManager.hideEpisodesUIForPiP();
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

        // Menu panel is shown only when user requests it
        // (don't auto-open after PiP)

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

        // Initialize draggable panels
        menuPanelContainer = findViewById(R.id.menuPanelContainer);
        commentsPanelContainer = findViewById(R.id.commentsPanelContainer);
        
        slidingMenuPanel = findViewById(R.id.slidingMenuPanel);
        menuOverlay = findViewById(R.id.menuOverlay);
        menuLoadingIndicator = findViewById(R.id.menuLoadingIndicator);
        menuLoadingOverlay = findViewById(R.id.menuLoadingOverlay);
        
        // Setup menu overlay click to close panels
        if (menuOverlay != null) {
            menuOverlay.setOnClickListener(v -> {
                // Закрываем открытые панели при клике на overlay
                if (menuPanelContainer != null && menuPanelContainer.isOpen()) {
                    closeMenuPanel();
                }
                if (commentsPanelContainer != null && commentsPanelContainer.isOpen()) {
                    closeCommentsPanel();
                }
            });
        }
        
        // Initialize anime info placeholder
        animeInfoPlaceholder = findViewById(R.id.animeInfoPlaceholder);
        animeInfoPoster = findViewById(R.id.animeInfoPoster);
        animeInfoTitle = findViewById(R.id.animeInfoTitle);
        animeInfoOriginalTitle = findViewById(R.id.animeInfoOriginalTitle);
        animeInfoYear = findViewById(R.id.animeInfoYear);
        animeInfoType = findViewById(R.id.animeInfoType);
        animeInfoStatus = findViewById(R.id.animeInfoStatus);
        animeInfoRating = findViewById(R.id.animeInfoRating);
        animeInfoEpisodes = findViewById(R.id.animeInfoEpisodes);
        animeInfoAge = findViewById(R.id.animeInfoAge);
        animeInfoReleaseDate = findViewById(R.id.animeInfoReleaseDate);
        animeInfoShikimori = findViewById(R.id.animeInfoShikimori);
        
        // Next episode overlay
        nextEpisodeOverlay = findViewById(R.id.nextEpisodeOverlay);
        nextEpisodeNumber = findViewById(R.id.nextEpisodeNumber);
        nextEpisodeCountdown = findViewById(R.id.nextEpisodeCountdown);
        cancelNextEpisodeButton = findViewById(R.id.cancelNextEpisodeButton);
        playNextEpisodeButton = findViewById(R.id.playNextEpisodeButton);
        nextEpisodeHandler = new Handler(Looper.getMainLooper());
        
        // Setup next episode overlay buttons
        if (cancelNextEpisodeButton != null) {
            cancelNextEpisodeButton.setOnClickListener(v -> cancelNextEpisode());
        }
        if (playNextEpisodeButton != null) {
            playNextEpisodeButton.setOnClickListener(v -> playNextEpisodeNow());
        }
        
        // Gesture components
        TextView seekPreviewText = findViewById(R.id.seekPreviewText);
        TextView holdSpeedToast = findViewById(R.id.holdSpeedToast);
        View skipIndicatorLeft = findViewById(R.id.skipIndicatorLeft);
        View skipIndicatorRight = findViewById(R.id.skipIndicatorRight);
        
        // Comments components
        View commentsPanel = findViewById(R.id.commentsPanel);
        ImageButton closeCommentsButton = findViewById(R.id.closeCommentsButton);
        RecyclerView commentsRecyclerView = findViewById(R.id.commentsRecyclerView);
        View commentsLoadingOverlay = findViewById(R.id.commentsLoadingOverlay);
        ImageButton commentsOptionsButton = findViewById(R.id.commentsOptionsButton);
        TextView emptyCommentsText = findViewById(R.id.emptyCommentsText);
        
        // Related titles components
        relatedTitlesOverlay = findViewById(R.id.relatedTitlesOverlay);
        relatedTitlesDimOverlay = findViewById(R.id.relatedTitlesDimOverlay);
        relatedTitlesRecyclerView = findViewById(R.id.relatedTitlesRecyclerView);
        
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
        this.emptyCommentsText = emptyCommentsText;
        this.seekPreviewText = seekPreviewText;
        this.holdSpeedToast = holdSpeedToast;
        this.pipButton = pipButton;
        this.skipIndicatorLeft = skipIndicatorLeft;
        this.skipIndicatorRight = skipIndicatorRight;
    }
    
    /**
     * Инициализация компонентов контроллера плеера
     */
    private void initializeControllerComponents() {
        // Player info components
        ibClosePlayer = controllerView.findViewById(R.id.ibClosePlayer);
        settingsButton = controllerView.findViewById(R.id.settingsButton);
        menuToggleFullscreen = controllerView.findViewById(R.id.menuToggleFullscreen);
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
        bookmarkButton = controllerView.findViewById(R.id.bookmarkButton);
        
        // Store for manager initialization
        this.episodesHorizontalRecyclerView = episodesHorizontalRecyclerView;
        this.commentsButton = commentsButton;
    }
    
    /**
     * Настройка драггабельных панелей
     */
    private void setupDraggablePanels() {
        final float density = getResources().getDisplayMetrics().density;
        final float panelWidthPx = 320 * density;
        final float videoOffsetPx = panelWidthPx / 2; // Сдвигаем только на половину ширины панели
        
        if (menuPanelContainer != null) {
            menuPanelContainer.setOnPanelStateChangeListener(new com.example.animelib.ui.DraggableSidePanel.OnPanelStateChangeListener() {
                @Override
                public void onPanelOpened() {
                    // Показываем overlay для возможности закрытия панели кликом вне её
                    if (menuOverlay != null) {
                        menuOverlay.setVisibility(View.VISIBLE);
                        menuOverlay.setAlpha(0.5f);
                    }
                }

                @Override
                public void onPanelClosed() {
                    // onPanelSliding уже установил финальную позицию
                    // Скрываем overlay
                    if (menuOverlay != null) {
                        menuOverlay.setVisibility(View.GONE);
                    }
                    // Уведомляем PlayersManager что панель закрыта через драг
                    if (playersManager != null) {
                        playersManager.onPanelClosedByDrag();
                    }
                }

                @Override
                public void onPanelSliding(float slideOffset) {
                    // slideOffset: 0 = открыто, 1 = закрыто
                    // Сдвигаем видео на половину ширины панели для центрирования
                    if (playerView != null) {
                        float offset = -videoOffsetPx * (1f - slideOffset);
                        playerView.setTranslationX(offset);
                    }
                }
                
                @Override
                public boolean canClosePanel() {
                    // Запрещаем закрытие панели озвучки, если не выбрана озвучка
                    if (playersManager != null && playersManager.getCurrentPlayerData() == null) {
                        return false;
                    }
                    return true;
                }
            });
        }
        
        if (commentsPanelContainer != null) {
            commentsPanelContainer.setOnPanelStateChangeListener(new com.example.animelib.ui.DraggableSidePanel.OnPanelStateChangeListener() {
                @Override
                public void onPanelOpened() {
                    // Показываем overlay для возможности закрытия панели кликом вне её
                    if (menuOverlay != null) {
                        menuOverlay.setVisibility(View.VISIBLE);
                        menuOverlay.setAlpha(0.5f);
                    }
                }

                @Override
                public void onPanelClosed() {
                    // onPanelSliding уже установил финальную позицию
                    // Скрываем overlay
                    if (menuOverlay != null) {
                        menuOverlay.setVisibility(View.GONE);
                    }
                    // Уведомляем CommentsManager что панель закрыта через драг
                    if (commentsManager != null) {
                        commentsManager.onPanelClosedByDrag();
                    }
                }

                @Override
                public void onPanelSliding(float slideOffset) {
                    // Сдвигаем видео на половину ширины панели для центрирования
                    if (playerView != null) {
                        float offset = -videoOffsetPx * (1f - slideOffset);
                        playerView.setTranslationX(offset);
                    }
                }
            });
        }
    }
    
    /**
     * Инициализация всех менеджеров
     */
    private void initializeManagers() {
        // Initialize gestures manager
        gesturesManager.initializeViews(playerView, null, holdSpeedToast, seekPreviewText,
                skipIndicatorLeft, skipIndicatorRight);
        
        // Setup draggable panels
        setupDraggablePanels();
        
        // Initialize comments manager
        commentsManager.initializeViews(commentsPanel, closeCommentsButton, commentsRecyclerView,
                commentsLoadingOverlay, commentsButton, commentsOptionsButton, menuOverlay, emptyCommentsText);
        
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
        
        // Initialize ambient light manager
        ambientLightManager = new AmbientLightManager(this, playerView, ambientLightView);
        
        // Initialize related titles
        initializeRelatedTitles();
    }
    
    /**
     * Инициализация связанных тайтлов
     */
    private void initializeRelatedTitles() {
        if (relatedTitlesRecyclerView == null) {
            Log.w("VideoPlayer", "Related titles RecyclerView is null");
            return;
        }
        
        // Setup RecyclerView
        relatedTitlesRecyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this, 
                androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false));
        
        // Initialize adapter
        relatedTitlesAdapter = new HorizontalRelatedTitlesAdapter(
                new java.util.ArrayList<>(), 
                this::onRelatedTitleSelected
        );
        
        // Initialize RelatedTitlesManager
        relatedTitlesManager = new RelatedTitlesManager();
        TextView relatedTitlesHeader = findViewById(R.id.relatedTitlesHeader);
        LinearLayout relatedAnimeInfoContainer = findViewById(R.id.relatedAnimeInfoContainer);
        ImageView relatedAnimeCover = findViewById(R.id.relatedAnimeCover);
        TextView relatedAnimeTitle = findViewById(R.id.relatedAnimeTitle);
        TextView relatedAnimeEngTitle = findViewById(R.id.relatedAnimeEngTitle);
        com.google.android.material.chip.Chip relatedAnimeTypeChip = findViewById(R.id.relatedAnimeTypeChip);
        com.google.android.material.chip.Chip relatedAnimeStatusChip = findViewById(R.id.relatedAnimeStatusChip);
        com.google.android.material.chip.Chip relatedAnimeYearChip = findViewById(R.id.relatedAnimeYearChip);
        com.google.android.material.chip.Chip relatedAnimeAgeChip = findViewById(R.id.relatedAnimeAgeChip);
        TextView relatedAnimeRating = findViewById(R.id.relatedAnimeRating);
        TextView relatedAnimeVotes = findViewById(R.id.relatedAnimeVotes);
        TextView relatedAnimeEpisodes = findViewById(R.id.relatedAnimeEpisodes);
        relatedTitlesManager.initialize(relatedTitlesOverlay, relatedTitlesDimOverlay, relatedTitlesRecyclerView,
                                        relatedTitlesHeader, relatedAnimeInfoContainer, relatedAnimeCover,
                                        relatedAnimeTitle, relatedAnimeEngTitle, relatedAnimeTypeChip, 
                                        relatedAnimeStatusChip, relatedAnimeYearChip, relatedAnimeAgeChip,
                                        relatedAnimeRating, relatedAnimeVotes, relatedAnimeEpisodes);
        relatedTitlesManager.setAdapter(relatedTitlesAdapter);
        
        // Устанавливаем listener для управления интерфейсом плеера
        relatedTitlesManager.setPlayerInterfaceControlListener(new RelatedTitlesManager.OnPlayerInterfaceControlListener() {
            @Override
            public void onHidePlayerInterface() {
                // НЕ скрываем интерфейс полностью, только меняем alpha через onPlayerInterfaceAlpha
            }
            
            @Override
            public void onShowPlayerInterface() {
                // Показываем интерфейс плеера
                if (playerView != null) {
                    playerView.showController();
                }
            }
            
            @Override
            public void onPlayerInterfaceAlpha(float alpha) {
                // Плавно меняем прозрачность интерфейса плеера
                View controller = findViewById(R.id.exo_controller);
                if (controller != null) {
                    controller.setAlpha(alpha);
                }
            }
        });
        
        // Настраиваем drag для закрытия панели связанных тайтлов
        setupRelatedTitlesDragToClose();
        
        // Load related titles if we have anime info
        if (currentAnimeId != null) {
            loadRelatedTitles();
        }
    }
    
    /**
     * Настраивает drag-to-close для панели связанных тайтлов (BottomSheet style)
     */
    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private void setupRelatedTitlesDragToClose() {
        if (relatedTitlesOverlay == null) return;
        
        final float[] initialY = {0f};
        final float[] lastY = {0f};
        final boolean[] isDragging = {false};
        final int touchSlop = android.view.ViewConfiguration.get(this).getScaledTouchSlop();
        
        relatedTitlesOverlay.setOnTouchListener((v, event) -> {
            // Обрабатываем только если панель открыта
            if (relatedTitlesManager == null || !relatedTitlesManager.isRelatedTitlesVisible()) {
                return false;
            }
            
            float currentY = event.getRawY();
            
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    initialY[0] = currentY;
                    lastY[0] = currentY;
                    isDragging[0] = false;
                    // Останавливаем текущие анимации
                    relatedTitlesOverlay.animate().cancel();
                    return true;
                    
                case android.view.MotionEvent.ACTION_MOVE:
                    float deltaY = currentY - initialY[0];
                    float moveDelta = currentY - lastY[0];
                    
                    // Начинаем drag если прошли touchSlop
                    if (!isDragging[0] && Math.abs(deltaY) > touchSlop) {
                        isDragging[0] = true;
                        // Запрещаем родителю перехватывать события
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    
                    if (isDragging[0]) {
                        // ПАНЕЛЬ ОТКРЫВАЕТСЯ СВЕРХУ ВНИЗ (translationY: -height -> 0)
                        // Закрываем её тягой ВВЕРХ (deltaY < 0)
                        
                        if (deltaY < 0) {
                            // Тянем ВВЕРХ (закрытие) - translationY становится отрицательным
                            relatedTitlesOverlay.setTranslationY(deltaY);
                        } else {
                            // Тянем ВНИЗ - не даём тянуть дальше (панель уже открыта)
                            relatedTitlesOverlay.setTranslationY(0);
                        }
                        
                        // Вычисляем прогресс (1.0 = открыто на месте, 0.0 = закрыто наверху)
                        int screenHeight = getResources().getDisplayMetrics().heightPixels;
                        float currentTranslation = relatedTitlesOverlay.getTranslationY();
                        // currentTranslation: 0 (открыто) -> -screenHeight (закрыто)
                        float progress = 1.0f + (currentTranslation / screenHeight);
                        progress = Math.max(0f, Math.min(1f, progress));
                        
                        // Обновляем затемнение
                        if (relatedTitlesDimOverlay != null) {
                            relatedTitlesDimOverlay.setAlpha(progress);
                        }
                        
                        // Обновляем прозрачность интерфейса плеера
                        View controller = findViewById(R.id.exo_controller);
                        if (controller != null) {
                            controller.setAlpha(1f - progress);
                        }
                    }
                    
                    lastY[0] = currentY;
                    return true;
                    
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    if (isDragging[0]) {
                        float finalDeltaY = currentY - initialY[0];
                        float velocity = lastY[0] - currentY; // Скорость вверх = положительная
                        
                        int screenHeight = getResources().getDisplayMetrics().heightPixels;
                        float dismissThreshold = screenHeight * 0.15f; // Уменьшен порог с 0.3 до 0.15
                        
                        // ПАНЕЛЬ ЗАКРЫВАЕТСЯ ТЯГОЙ ВВЕРХ (finalDeltaY < 0)
                        // Решаем закрывать или нет на основе расстояния и скорости
                        boolean shouldDismiss = (finalDeltaY < -dismissThreshold) || (velocity > 50 && finalDeltaY < 0);
                        
                        if (shouldDismiss) {
                            // Закрываем панель
                            relatedTitlesManager.hideRelatedTitles();
                            if (ambientLightManager != null) {
                                ambientLightManager.resume();
                            }
                        } else {
                            // Возвращаем на место
                            relatedTitlesOverlay.animate()
                                    .translationY(0f)
                                    .setDuration(200)
                                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                                    .setUpdateListener(animation -> {
                                        float currentTranslation = relatedTitlesOverlay.getTranslationY();
                                        float progress = 1.0f + (currentTranslation / screenHeight);
                                        
                                        if (relatedTitlesDimOverlay != null) {
                                            relatedTitlesDimOverlay.setAlpha(progress);
                                        }
                                        
                                        View controller = findViewById(R.id.exo_controller);
                                        if (controller != null) {
                                            controller.setAlpha(1f - progress);
                                        }
                                    })
                                    .start();
                        }
                        
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                    }
                    isDragging[0] = false;
                    return true;
            }
            
            return false;
        });
    }
    
    /**
     * Загрузка связанных тайтлов
     */
    private void loadRelatedTitles() {
        if (currentAnimeId == null) {
            Log.w("VideoPlayer", "No anime ID available for loading related titles");
            return;
        }
        
        // Extract anime slug from current anime ID or URL
        String animeSlug = currentAnimeId;
        if (animeUrl != null && !animeUrl.isEmpty()) {
            animeSlug = ApiService.extractMediaSlugFromUrl(animeUrl);
        }
        
        if (animeSlug == null || animeSlug.isEmpty()) {
            Log.w("VideoPlayer", "No anime slug available for loading related titles");
            return;
        }
        
        Log.d("VideoPlayer", "Loading related titles for anime: " + animeSlug);
        
        apiService.getRelatedTitles(animeSlug, new ApiService.RelatedTitlesCallback() {
            @Override
            public void onRelatedTitlesReceived(RelatedTitlesResponse response) {
                runOnUiThread(() -> {
                    if (response.getData() != null && !response.getData().isEmpty()) {
                        Log.d("VideoPlayer", "Related titles loaded: " + response.getData().size());
                        showRelatedTitles(response.getData());
                    } else {
                        Log.d("VideoPlayer", "No related titles found");
                    }
                });
            }
            
            @Override
            public void onError(String error) {
                Log.e("VideoPlayer", "Error loading related titles: " + error);
            }
        });
    }
    
    /**
     * Показать связанные тайтлы
     */
    private void showRelatedTitles(List<RelatedTitlesResponse.RelatedTitle> relatedTitles) {
        if (relatedTitlesManager != null) {
            relatedTitlesManager.updateRelatedTitles(relatedTitles);
        }
    }
    
    /**
     * Обработчик выбора связанного тайтла
     */
    /**
     * Устанавливает информацию об аниме в панель связанных тайтлов
     */
    private void setAnimeInfoToRelatedPanel(AnimeInfoResponse.Data animeData) {
        if (relatedTitlesManager == null) {
            Log.w("VideoPlayer", "relatedTitlesManager is null");
            return;
        }

        // Получаем обложку
        String coverUrl = null;
        if (animeData.getCover() != null && animeData.getCover().getDefaultUrl() != null) {
            coverUrl = animeData.getCover().getDefaultUrl();
        }

        // Получаем название (приоритет: русское -> английское -> оригинальное)
        String title = animeData.getRus_name();
        if (title == null || title.trim().isEmpty()) {
            title = animeData.getEng_name();
        }
        if (title == null || title.trim().isEmpty()) {
            title = animeData.getName();
        }
        if (title == null) {
            title = "Без названия";
        }

        // Английское название (если русское название используется)
        String engTitle = null;
        if (animeData.getRus_name() != null && !animeData.getRus_name().trim().isEmpty()) {
            engTitle = animeData.getEng_name();
        }

        // Тип
        String type = null;
        if (animeData.getType() != null && animeData.getType().getLabel() != null) {
            type = animeData.getType().getLabel();
        }

        // Статус
        String status = null;
        if (animeData.getStatus() != null && animeData.getStatus().getLabel() != null) {
            status = animeData.getStatus().getLabel();
        }

        // Год выхода
        String year = null;
        if (animeData.getReleaseDateString() != null && !animeData.getReleaseDateString().isEmpty()) {
            // Извлекаем год из даты (например "2024" из "15.10.2024" или просто "2024")
            try {
                String dateStr = animeData.getReleaseDateString();
                if (dateStr.contains(".")) {
                    // Формат "DD.MM.YYYY"
                    String[] parts = dateStr.split("\\.");
                    if (parts.length >= 3) {
                        year = parts[2];
                    }
                } else if (dateStr.matches("\\d{4}")) {
                    // Формат "YYYY"
                    year = dateStr;
                }
            } catch (Exception e) {
                Log.w("VideoPlayer", "Failed to parse year from: " + animeData.getReleaseDateString());
            }
        }

        // Возрастной рейтинг
        String ageRating = null;
        if (animeData.getAgeRestriction() != null && animeData.getAgeRestriction().getLabel() != null) {
            ageRating = animeData.getAgeRestriction().getLabel();
        }

        // Рейтинг
        String rating = "";
        String votes = "";
        if (animeData.getRating() != null) {
            if (animeData.getRating().getAverageFormated() != null) {
                rating = animeData.getRating().getAverageFormated();
            }
            if (animeData.getRating().getVotesFormated() != null) {
                votes = "(" + animeData.getRating().getVotesFormated() + ")";
            }
        }

        // Количество эпизодов
        String episodes = null;
        if (animeData.getItems_count() != null) {
            episodes = "Эпизоды: " + animeData.getItems_count().getUploaded() +
                      " / " + animeData.getItems_count().getTotal();
        }

        Log.d("VideoPlayer", "Setting anime info to related panel: title=" + title + 
              ", type=" + type + ", status=" + status + ", year=" + year);
        
        relatedTitlesManager.setAnimeInfo(coverUrl, title, engTitle, type, status, year, 
                                         ageRating, rating, votes, episodes);
    }
    
    /**
     * Обработчик выбора связанного тайтла
     * @param slugUrl URL выбранного аниме (только для model="anime")
     */
    private void onRelatedTitleSelected(String slugUrl) {
        if (slugUrl == null || slugUrl.trim().isEmpty()) {
            Log.w("VideoPlayer", "Related title slug URL is null or empty");
            return;
        }
        
        Log.d("VideoPlayer", "Related title selected with slug URL: " + slugUrl);
        
        // Hide related titles
        if (relatedTitlesManager != null) {
            relatedTitlesManager.hideRelatedTitles();
        }
        
        // Stop current player
        if (player != null) {
            player.stop();
            player.clearMediaItems();
        }
        
        // Reset state
        currentVideoUrl = null;
        currentAnimeId = null;
        
        // Show loading
        showLoading("Загрузка аниме...");
        
        // Load new anime
        animeUrl = slugUrl;
        loadAnimeFromUrl(slugUrl);
        
        Log.d("VideoPlayer", "Loading new anime from slug URL: " + slugUrl);
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
                    .withStartAction(() -> {
                        // Уведомляем TimecodeManager что контроллер стал видимым
                        if (timecodeManager != null) {
                            timecodeManager.setControllerVisibility(true);
                        }
                    })
                    .start();
        } else {
            // Hide controller with fade out
            controllerView.animate()
                    .alpha(0.0f)
                    .setDuration(150)
                    .setInterpolator(new android.view.animation.AccelerateInterpolator())
                    .withStartAction(() -> {
                        // Уведомляем TimecodeManager что контроллер стал скрытым
                        if (timecodeManager != null) {
                            timecodeManager.setControllerVisibility(false);
                        }
                    })
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
        if (menuToggleFullscreen != null) menuToggleFullscreen.setVisibility(visibility);
        
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
        
        if (menuToggleFullscreen != null) {
            menuToggleFullscreen.setOnClickListener(v -> toggleFullscreenMode());
        }
        
        if (pipButton != null) {
            pipButton.setOnClickListener(v -> {
                // Автоматически сохраняем закладку перед переходом в PiP
                autoSaveBookmark();
                enterPictureInPictureMode();
            });
        }
        
        if (ibClosePlayer != null) {
            ibClosePlayer.setOnClickListener(v -> {
                // Автоматически сохраняем закладку перед закрытием
                autoSaveBookmark();
                finish();
            });
        }
        
        if (episodesMenuButton != null) {
            episodesMenuButton.setOnClickListener(v -> toggleEpisodesInController());
        }
        
        if (bookmarkButton != null) {
            Log.d("VideoPlayer", "Bookmark button found, visibility: " + bookmarkButton.getVisibility() + 
                               ", enabled: " + bookmarkButton.isEnabled() + 
                               ", clickable: " + bookmarkButton.isClickable());
            
            // Изначально отключаем кнопку до готовности плеера
            bookmarkButton.setEnabled(false);
            bookmarkButton.setClickable(false);
            
            bookmarkButton.setOnClickListener(v -> {
                Log.d("VideoPlayer", "Bookmark button clicked!");
                addBookmark();
            });
        } else {
            Log.e("VideoPlayer", "Bookmark button is null!");
        }
        
        // Setup player control buttons
        setupPlayerControlButtons();
    }
    
    /**
     * Настройка touch listener для overlay
     */
    @SuppressLint("ClickableViewAccessibility")
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
            
            @Override
            public void onEpisodesSwipeUp() {
                Log.d("VideoPlayer", "Episodes swipe up detected");
                // Открываем панель эпизодов
                if (episodesManager != null) {
                    episodesManager.showEpisodesMenu();
                }
            }
            
            @Override
            public void onEpisodesSwipeDown() {
                Log.d("VideoPlayer", "Episodes swipe down detected");
                // Закрываем панель эпизодов
                if (episodesManager != null) {
                    episodesManager.hideEpisodesMenu();
                }
            }
            
            @Override
            public void onCommentsSwipeFromRight() {
                Log.d("VideoPlayer", "Comments swipe from right detected");
                // Открываем панель комментариев
                if (commentsManager != null) {
                    commentsManager.showCommentsPanel();
                }
            }
            
            @Override
            public void onPlayersSwipeFromRight() {
                Log.d("VideoPlayer", "Players swipe from right detected");
                // Открываем панель озвучек
                if (playersManager != null) {
                    playersManager.showMenu();
                }
            }
            
            @Override
            public void onEpisodesDragProgress(float progress) {
                // ВЕРТИКАЛЬНЫЕ ЖЕСТЫ: Эпизоды теперь обрабатываются в VerticalGesturesManager
                // Этот метод больше не вызывается для вертикальных свайпов
            }
            
            @Override
            public void onRelatedTitlesDragProgress(float progress) {
                // ВЕРТИКАЛЬНЫЕ ЖЕСТЫ: Related titles теперь обрабатываются в VerticalGesturesManager
                // Этот метод больше не вызывается для вертикальных свайпов
            }
            
            @Override
            public void onCommentsDragProgress(float progress) {
                // Приостанавливаем ambient подсветку при начале drag
                if (progress > 0 && ambientLightManager != null) {
                    ambientLightManager.suspend();
                }
                
                // Показываем интерфейс плеера при начале drag
                if (progress > 0 && playerView != null && !playerView.isControllerFullyVisible()) {
                    playerView.showController();
                    Log.d("VideoPlayer", "Showing controller on comments drag start");
                }
                
                // Обновляем прогресс вытягивания панели комментариев
                if (commentsPanelContainer != null) {
                    commentsPanelContainer.setDragProgress(progress);
                }
            }
            
            @Override
            public void onPlayersDragProgress(float progress) {
                // Приостанавливаем ambient подсветку при начале drag
                if (progress > 0 && ambientLightManager != null) {
                    ambientLightManager.suspend();
                }
                
                // Показываем интерфейс плеера при начале drag
                if (progress > 0 && playerView != null && !playerView.isControllerFullyVisible()) {
                    playerView.showController();
                    Log.d("VideoPlayer", "Showing controller on players drag start");
                }
                
                // Обновляем прогресс вытягивания панели озвучек
                if (menuPanelContainer != null) {
                    menuPanelContainer.setDragProgress(progress);
                }
            }
            
            @Override
            public void onPanelDragComplete(GesturesManager.EdgeSwipeType type, boolean shouldOpen) {
                Log.d("VideoPlayer", "Panel drag complete: " + type + ", shouldOpen=" + shouldOpen);
                
                // ВЕРТИКАЛЬНЫЕ ЖЕСТЫ (эпизоды и связанные тайтлы) ОБРАБАТЫВАЮТСЯ В VerticalGesturesManager
                // Здесь только горизонтальные (комментарии и озвучки)
                
                switch (type) {
                    case COMMENTS_RIGHT:
                        // Даем DraggableSidePanel завершить анимацию, затем обновляем состояние
                        if (commentsPanelContainer != null) {
                            commentsPanelContainer.completeDrag(shouldOpen);
                        }
                        // Обновляем состояние менеджера после завершения анимации
                        if (commentsManager != null) {
                            commentsManager.updateDragState(shouldOpen);
                        }
                        // Возобновляем ambient подсветку только если панель закрыта
                        if (!shouldOpen && ambientLightManager != null) {
                            ambientLightManager.resume();
                        }
                        break;
                        
                    case PLAYERS_RIGHT:
                        // Даем DraggableSidePanel завершить анимацию, затем обновляем состояние
                        if (menuPanelContainer != null) {
                            menuPanelContainer.completeDrag(shouldOpen);
                        }
                        // Обновляем состояние менеджера после завершения анимации
                        if (playersManager != null) {
                            playersManager.updateDragState(shouldOpen);
                        }
                        // Возобновляем ambient подсветку только если панель закрыта
                        if (!shouldOpen && ambientLightManager != null) {
                            ambientLightManager.resume();
                        }
                        break;
                }
            }
            
            @Override
            public boolean isEpisodesMenuVisible() {
                return episodesManager != null && episodesManager.isEpisodesMenuVisible();
            }
            
            @Override
            public boolean isRelatedTitlesMenuVisible() {
                return relatedTitlesManager != null && relatedTitlesManager.isRelatedTitlesVisible();
            }
            
            @Override
            public void onDoubleTapSkip(boolean isForward, int skipDurationSeconds) {
                if (player == null) {
                    Log.w("VideoPlayer", "Player is null, cannot perform skip");
                    return;
                }
                
                long currentPosition = player.getCurrentPosition();
                long skipDuration = longSkipDuration * 1000L; // Используем настройку из VideoPlayerActivity
                long newPosition = isForward ? currentPosition + skipDuration : currentPosition - skipDuration;
                long duration = player.getDuration();
                
                // Ограничиваем позицию границами видео
                newPosition = Math.max(0, newPosition);
                if (duration > 0) {
                    newPosition = Math.min(newPosition, duration);
                }
                
                player.seekTo(newPosition);
                Log.d("VideoPlayer", "Double tap skip: " + (isForward ? "forward" : "backward") + 
                      " to " + (newPosition / 1000) + "s (skip=" + (skipDuration / 1000) + "s)");
            }
        });
        
        // Setup vertical gestures callback
        verticalGesturesManager.setCallback(new VerticalGesturesManager.VerticalGestureCallback() {
            @Override
            public void onEpisodesDragProgress(float progress) {
                // Приостанавливаем ambient подсветку при начале drag
                if (progress > 0 && ambientLightManager != null) {
                    ambientLightManager.suspend();
                }
                
                // Показываем интерфейс плеера при начале drag (первый вызов с progress > 0)
                if (progress > 0 && playerView != null && !playerView.isControllerFullyVisible()) {
                    playerView.showController();
                    Log.d("VideoPlayer", "Showing controller on episodes drag start");
                }
                
                if (episodesManager != null) {
                    episodesManager.setDragProgress(progress);
                }
            }
            
            @Override
            public void onRelatedInfoDragProgress(float progress) {
                // Приостанавливаем ambient подсветку при начале drag
                if (progress > 0 && ambientLightManager != null) {
                    ambientLightManager.suspend();
                }
                
                // Показываем интерфейс плеера при начале drag
                if (progress > 0 && playerView != null && !playerView.isControllerFullyVisible()) {
                    playerView.showController();
                    Log.d("VideoPlayer", "Showing controller on related titles drag start");
                }
                
                if (relatedTitlesManager != null) {
                    relatedTitlesManager.setDragProgress(progress);
                }
            }
            
            @Override
            public void onEpisodesDragComplete(boolean shouldOpen) {
                Log.d("VideoPlayer", "Episodes drag complete: shouldOpen=" + shouldOpen);
                if (episodesManager != null) {
                    // Используем updateDragState вместо completeDrag чтобы избежать перезапуска анимации
                    episodesManager.updateDragState(shouldOpen);
                }
                // Возобновляем ambient подсветку только если панель закрыта
                if (!shouldOpen && ambientLightManager != null) {
                    ambientLightManager.resume();
                }
            }
            
            @Override
            public void onRelatedInfoDragComplete(boolean shouldOpen) {
                Log.d("VideoPlayer", "Related info drag complete: shouldOpen=" + shouldOpen);
                // Сначала сбрасываем эпизоды если они открыты
                if (episodesManager != null && episodesManager.isEpisodesMenuVisible()) {
                    episodesManager.resetControllerPosition();
                }
                if (relatedTitlesManager != null) {
                    relatedTitlesManager.completeDrag(shouldOpen);
                }
                // Возобновляем ambient подсветку только если панель закрыта
                if (!shouldOpen && ambientLightManager != null) {
                    ambientLightManager.resume();
                }
            }
            
            @Override
            public boolean isEpisodesOpen() {
                return episodesManager != null && episodesManager.isEpisodesMenuVisible();
            }
            
            @Override
            public boolean isRelatedInfoOpen() {
                return relatedTitlesManager != null && relatedTitlesManager.isRelatedTitlesVisible();
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
                
                // Сначала пытаемся загрузить эпизод из закладки
                if (currentAnimeId != null) {
                    Log.d("VideoPlayer", "Trying to load episode from bookmark for anime: " + currentAnimeId);
                    
                    // Получаем media_slug для закладки
                    String animeUrl = getIntent().getStringExtra("anime_url");
                    String mediaSlug = null;
                    if (animeUrl != null && !animeUrl.isEmpty()) {
                        mediaSlug = ApiService.extractMediaSlugFromUrl(animeUrl);
                    }
                    
                    if (mediaSlug != null) {
                        episodesManager.getBookmarkManager().getLastEpisodeFromBookmark(mediaSlug, episodes, 
                            new BookmarkManager.LastEpisodeCallback() {
                                @Override
                                public void onLastEpisodeFound(EpisodesListResponse.EpisodeItem episode, String progress) {
                                    Log.d("VideoPlayer", "Found bookmarked episode: " + episode.getNumber() + 
                                                         ", progress: " + progress);
                                    
                                    // Сохраняем таймкод из закладки
                                    bookmarkTimecode = episodesManager.getBookmarkManager().parseTimecodeToMilliseconds(progress);
                                    Log.d("VideoPlayer", "Parsed bookmark timecode: " + progress + " -> " + bookmarkTimecode + "ms");
                                    
                                    // Устанавливаем красный цвет кнопки для эпизода с закладкой
                                    updateBookmarkButtonColor(true);
                                    
                                    episodesManager.setCurrentEpisode(episode);
                                    commentsManager.setCurrentEpisode(episode);
                                    
                                    // СРАЗУ обновляем заголовок с номером эпизода
                                    updateEpisodeHeaderQuick();
                                    
                                    // ВАЖНО: Загружаем плееры для ПРАВИЛЬНОГО эпизода
                                    Log.d("VideoPlayer", "Loading players for bookmarked episode: " + episode.getId());
                                    playersManager.loadPlayersForEpisode(episode.getId());
                                }
                                
                                @Override
                                public void onNoBookmarkFound() {
                                    Log.d("VideoPlayer", "No bookmark found, loading first episode");
                                    loadFirstEpisode();
                                }
                            });
                    } else {
                        Log.d("VideoPlayer", "No media slug available, falling back to URL detection");
                        fallbackToUrlDetection();
                    }
                } else {
                    // Нет anime ID, используем URL detection
                    fallbackToUrlDetection();
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
        
        // Устанавливаем callback для загрузки плееров
        playersManager.setDataCallback(new PlayersManager.PlayersDataCallback() {
            @Override
            public void onPlayersLoaded(List<EpisodeResponse.PlayerData> players) {
                Log.d("VideoPlayer", "Players loaded: " + players.size() + " for episode: " + 
                      (episodesManager.getCurrentEpisode() != null ? episodesManager.getCurrentEpisode().getNumber() : "unknown"));
                
                // Проверяем нужно ли показывать меню
                // Показываем только если нет выбранной озвучки (первая загрузка)
                if (playersManager.getCurrentPlayerData() == null) {
                    Log.d("VideoPlayer", "No player selected yet, showing menu for first time");
                    showPlayerSelectionDialogWithAutoSelect(players);
                } else {
                    Log.d("VideoPlayer", "Player already selected, skipping menu (episode switch)");
                    // При переключении эпизода меню не показываем
                    // Автовыбор уже сработал в PlayersManager
                    if (menuLoadingOverlay != null) menuLoadingOverlay.setVisibility(View.GONE);
                }
            }
            
            @Override
            public void onPlayersError(String error) {
                Log.e("VideoPlayer", "Error loading players: " + error);
                Toast.makeText(VideoPlayerActivity.this, "Ошибка загрузки плееров: " + error, Toast.LENGTH_SHORT).show();
            }
        });
        
        // Показываем placeholder когда нет выбранной озвучки
        playersManager.setVisibilityCallback(isVisible -> {
            if (!isVisible && playersManager.getCurrentPlayerData() == null) {
                // Меню закрыто и нет выбранной озвучки - показываем placeholder
                showAnimeInfoPlaceholder();
            }
            
            // НЕ показываем интерфейс плеера при drag - это неправильно
            // Интерфейс должен показываться только при обычном открытии панели плееров
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
        
        // Показываем anime info placeholder и загружаем информацию
        // Placeholder будет скрыт автоматически при выборе озвучки
        showAnimeInfoPlaceholder();
        loadAnimeInfoForPlaceholder();
    }
    
    /**
     * Показывает placeholder с информацией об аниме
     */
    private void showAnimeInfoPlaceholder() {
        if (animeInfoPlaceholder != null) {
            animeInfoPlaceholder.setVisibility(View.VISIBLE);
            animeInfoPlaceholder.setTranslationX(0); // Показываем справа
        }
    }
    
    /**
     * Скрывает placeholder с информацией об аниме
     */
    private void hideAnimeInfoPlaceholder() {
        if (animeInfoPlaceholder != null) {
            animeInfoPlaceholder.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> {
                    animeInfoPlaceholder.setVisibility(View.GONE);
                    animeInfoPlaceholder.setAlpha(1f);
                })
                .start();
        }
    }
    
    /**
     * Загружает информацию об аниме для placeholder
     */
    private void loadAnimeInfoForPlaceholder() {
        if (animeUrl == null) return;
        
        // Извлекаем slug из URL для API
        String animeSlug = apiService.extractAnimeSlug(animeUrl);
        if (animeSlug == null) {
            Log.e("VideoPlayer", "Could not extract anime slug from URL: " + animeUrl);
            return;
        }
        
        Log.d("VideoPlayer", "Loading anime info for slug: " + animeSlug);
        apiService.fetchAnimeInfo(animeSlug, new ApiService.AnimeInfoCallback() {
            @Override
            public void onAnimeInfoReceived(AnimeInfoResponse response) {
                runOnUiThread(() -> displayAnimeInfo(response));
            }

            @Override
            public void onError(String errorMessage) {
                Log.e("VideoPlayer", "Failed to load anime info: " + errorMessage);
            }
        });
    }
    
    /**
     * Отображает информацию об аниме в placeholder
     */
    @SuppressLint("SetTextI18n")
    private void displayAnimeInfo(AnimeInfoResponse animeInfo) {
        if (animeInfo == null || animeInfo.getData() == null) {
            Log.e("VideoPlayer", "displayAnimeInfo: animeInfo or data is null");
            return;
        }
        
        AnimeInfoResponse.Data data = animeInfo.getData();
        Log.d("VideoPlayer", "Displaying anime info: " + data.getRus_name());
        
        // Устанавливаем информацию об аниме в панель связанных тайтлов
        setAnimeInfoToRelatedPanel(data);
        
        // Название
        if (animeInfoTitle != null && data.getRus_name() != null) {
            animeInfoTitle.setText(data.getRus_name());
        }
        
        // Оригинальное название
        if (animeInfoOriginalTitle != null && data.getEng_name() != null) {
            animeInfoOriginalTitle.setText(data.getEng_name());
            animeInfoOriginalTitle.setVisibility(View.VISIBLE);
        }
        
        // Год из releaseDate
        if (animeInfoYear != null && data.getReleaseDate() != null && data.getReleaseDate().length() >= 4) {
            String year = data.getReleaseDate().substring(0, 4);
            animeInfoYear.setText(year);
        } else if (animeInfoYear != null) {
            animeInfoYear.setText("—");
        }
        
        // Тип
        if (animeInfoType != null && data.getType() != null && data.getType().getLabel() != null) {
            String type = data.getType().getLabel();
            // Убираем "TV " из "TV Сериал"
            type = type.replace("TV ", "");
            animeInfoType.setText(type);
        } else if (animeInfoType != null) {
            animeInfoType.setText("—");
        }
        
        // Статус
        if (animeInfoStatus != null && data.getStatus() != null && data.getStatus().getLabel() != null) {
            animeInfoStatus.setText(data.getStatus().getLabel());
        } else if (animeInfoStatus != null) {
            animeInfoStatus.setText("—");
        }
        
        // Рейтинг
        Log.d("VideoPlayer", "Rating view: " + (animeInfoRating != null) + ", data: " + (data.getRating() != null));
        if (animeInfoRating != null) {
            if (data.getRating() != null && data.getRating().getAverageFormated() != null) {
                animeInfoRating.setText(data.getRating().getAverageFormated());
                Log.d("VideoPlayer", "Set rating: " + data.getRating().getAverageFormated());
            } else {
                animeInfoRating.setText("—");
                Log.d("VideoPlayer", "Rating data is null");
            }
        }
        
        // Количество эпизодов
        Log.d("VideoPlayer", "Episodes view: " + (animeInfoEpisodes != null) + ", data: " + (data.getItems_count() != null));
        if (animeInfoEpisodes != null) {
            if (data.getItems_count() != null && data.getItems_count().getUploaded() > 0) {
                animeInfoEpisodes.setText(data.getItems_count().getUploaded() + " эпизодов");
                Log.d("VideoPlayer", "Set episodes: " + data.getItems_count().getUploaded());
            } else {
                animeInfoEpisodes.setText("—");
                Log.d("VideoPlayer", "Episodes data is null or 0");
            }
        }
        
        // Возрастное ограничение
        Log.d("VideoPlayer", "Age view: " + (animeInfoAge != null) + ", data: " + (data.getAgeRestriction() != null));
        if (animeInfoAge != null) {
            if (data.getAgeRestriction() != null && data.getAgeRestriction().getLabel() != null) {
                animeInfoAge.setText(data.getAgeRestriction().getLabel());
                Log.d("VideoPlayer", "Set age: " + data.getAgeRestriction().getLabel());
            } else {
                animeInfoAge.setText("—");
                Log.d("VideoPlayer", "Age data is null");
            }
        }
        
        // Дата выхода
        Log.d("VideoPlayer", "ReleaseDate view: " + (animeInfoReleaseDate != null) + ", data: " + (data.getReleaseDateString() != null));
        if (animeInfoReleaseDate != null) {
            if (data.getReleaseDateString() != null) {
                animeInfoReleaseDate.setText(data.getReleaseDateString());
                Log.d("VideoPlayer", "Set release date: " + data.getReleaseDateString());
            } else {
                animeInfoReleaseDate.setText("—");
                Log.d("VideoPlayer", "Release date data is null");
            }
        }
        
        // Shikimori рейтинг
        Log.d("VideoPlayer", "Shikimori view: " + (animeInfoShikimori != null) + ", data: " + (data.getShikimori_href() != null));
        if (animeInfoShikimori != null) {
            if (data.getShikimori_href() != null) {
                animeInfoShikimori.setText(String.valueOf(data.getShiki_rate()));
                Log.d("VideoPlayer", "Set shikimori: " + data.getShiki_rate());
            } else {
                animeInfoShikimori.setText("—");
                Log.d("VideoPlayer", "Shikimori data is null");
            }
        }
        
        // Постер - загружаем через ImageLoader
        if (animeInfoPoster != null && data.getCover() != null) {
            String posterUrl = data.getCover().getDefaultUrl();
            if (posterUrl != null && !posterUrl.isEmpty()) {
                com.example.animelib.util.ImageLoader.getInstance()
                    .loadInto(animeInfoPoster, posterUrl, R.drawable.ic_image_placeholder);
            } else {
                animeInfoPoster.setImageResource(R.drawable.ic_image_placeholder);
            }
        }
    }

    private void toggleMenu() {
        playersManager.toggleMenu();
    }
    
    /**
     * Показывает оверлей следующего эпизода с обратным отсчетом
     */
    private void showNextEpisodeOverlay() {
        if (nextEpisodeOverlay == null || episodesManager == null) return;
        
        // Проверяем есть ли следующий эпизод
        EpisodesListResponse.EpisodeItem nextEpisode = episodesManager.getNextEpisode();
        if (nextEpisode == null) {
            Log.d("VideoPlayer", "No next episode available");
            return;
        }
        
        Log.d("VideoPlayer", "Showing next episode overlay for episode: " + nextEpisode.getNumber());
        
        // Устанавливаем номер эпизода
        if (nextEpisodeNumber != null) {
            nextEpisodeNumber.setText("эпизод " + nextEpisode.getNumber());
        }
        
        // Сбрасываем счетчик
        countdownSeconds = 7;
        if (nextEpisodeCountdown != null) {
            nextEpisodeCountdown.setText(String.valueOf(countdownSeconds));
        }
        
        // Показываем оверлей
        nextEpisodeOverlay.setVisibility(View.VISIBLE);
        nextEpisodeOverlay.setAlpha(0f);
        nextEpisodeOverlay.animate()
            .alpha(1f)
            .setDuration(300)
            .start();
        
        // Запускаем обратный отсчет
        startCountdown();
    }
    
    /**
     * Запускает обратный отсчет до следующего эпизода
     */
    private void startCountdown() {
        if (nextEpisodeHandler == null) return;
        
        nextEpisodeRunnable = new Runnable() {
            @Override
            public void run() {
                countdownSeconds--;
                
                if (nextEpisodeCountdown != null) {
                    nextEpisodeCountdown.setText(String.valueOf(countdownSeconds));
                }
                
                if (countdownSeconds > 0) {
                    nextEpisodeHandler.postDelayed(this, 1000);
                } else {
                    // Время вышло - запускаем следующий эпизод
                    playNextEpisodeNow();
                }
            }
        };
        
        nextEpisodeHandler.postDelayed(nextEpisodeRunnable, 1000);
    }
    
    /**
     * Отменяет автовоспроизведение следующего эпизода
     */
    private void cancelNextEpisode() {
        Log.d("VideoPlayer", "Next episode cancelled by user");
        
        // Останавливаем обратный отсчет
        if (nextEpisodeHandler != null && nextEpisodeRunnable != null) {
            nextEpisodeHandler.removeCallbacks(nextEpisodeRunnable);
        }
        
        // Скрываем оверлей
        hideNextEpisodeOverlay();
    }
    
    /**
     * Немедленно запускает следующий эпизод
     */
    private void playNextEpisodeNow() {
        Log.d("VideoPlayer", "Playing next episode now");
        
        // Останавливаем обратный отсчет
        if (nextEpisodeHandler != null && nextEpisodeRunnable != null) {
            nextEpisodeHandler.removeCallbacks(nextEpisodeRunnable);
        }
        
        // Скрываем оверлей
        hideNextEpisodeOverlay();
        
        // Переключаемся на следующий эпизод
        if (episodesManager != null) {
            episodesManager.navigateToNextEpisode();
        }
    }
    
    /**
     * Скрывает оверлей следующего эпизода
     */
    private void hideNextEpisodeOverlay() {
        if (nextEpisodeOverlay == null) return;
        
        nextEpisodeOverlay.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction(() -> {
                if (nextEpisodeOverlay != null) {
                    nextEpisodeOverlay.setVisibility(View.GONE);
                }
            })
            .start();
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
                View spinner = controllerView.findViewById(R.id.playLoadingIndicator);

                // Если показывается индикатор загрузки, не меняем видимость кнопок
                if (spinner != null && spinner.getVisibility() == View.VISIBLE) {
                    Log.d("PlayerControls", "Loading spinner is visible - not updating button visibility");
                    return;
                }

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
        
        // Скрываем anime info placeholder при выборе озвучки
        hideAnimeInfoPlaceholder();
        
        // СРАЗУ обновляем заголовок с номером эпизода (синхронно, без API запроса)
        updateEpisodeHeaderQuick();
        
        // НЕ сохраняем предпочтения здесь! Сохранение происходит позже вместе с качеством
        // чтобы не перезаписывать сохраненное качество на null

        // Сохраняем текущую позицию перед сменой плеера
        if (player != null) {
            savedPlayerPosition = player.getCurrentPosition();
            Log.d("VideoPlayer", "Saved player position: " + savedPlayerPosition + "ms before switching to: " + playerData.getPlayer());
        }

        // Stop current playback before starting new one
        stopCurrentPlayback();

        // Update preferred quality for new player
        List<String> newQualities = playersManager.getAvailableQualities();
        
        // Включаем кнопку закладки когда плеер выбран
        enableBookmarkButton();
        
        // Обновляем currentPlayerData в PlayersManager для правильной подсветки
        playersManager.setCurrentPlayerData(playerData);

        // Don't hide menu automatically - let user control it
        Log.d("VideoPlayer", "Player selected, ready to start playback");
        
        if (!newQualities.isEmpty()) {
            // Пытаемся загрузить сохраненное качество
            executor.execute(() -> {
                com.example.animelib.data.entity.PlayerPreferences prefs = apiService.loadPlayerPreferences();
                String savedQuality = (prefs != null) ? prefs.getPreferredQuality() : null;
                
                safeRunOnUiThread(() -> {
                    String newPreferredQuality = null;
                    
                    // Если есть сохраненное качество и оно доступно - используем его
                    if (savedQuality != null && newQualities.contains(savedQuality)) {
                        newPreferredQuality = savedQuality;
                        Log.d("VideoPlayer", "Using saved quality: " + savedQuality);
                    } else {
                        // Иначе выбираем максимальное доступное качество
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                            newPreferredQuality = newQualities.getFirst();
                        }
                        Log.d("VideoPlayer", "Saved quality not found, using max quality: " + newPreferredQuality);
                    }
                    
                    preferredQuality = newPreferredQuality;
                    Log.d("VideoPlayer", "Updated preferred quality to: " + newPreferredQuality + " for player: " + playerData.getPlayer());
                    
                    // Сохраняем предпочтения ПОСЛЕ выбора качества
                    if (playerData.getPlayer() != null && playerData.getTeam() != null) {
                        apiService.savePlayerPreferences(playerData.getPlayer(), playerData.getTeam().getId(), preferredQuality);
                        Log.d("VideoPlayer", "Saved player preferences with quality: player=" + playerData.getPlayer() + 
                              ", teamId=" + playerData.getTeam().getId() + ", quality=" + preferredQuality);
                    }
                    
                    // Update settings dialog if it's open
                    if (currentSettingsBottomSheet != null) {
                        currentSettingsBottomSheet.updateQualities(newQualities, preferredQuality);
                    }
                    
                    // ВАЖНО: Запускаем плеер ПОСЛЕ установки качества
                    long startPosition = bookmarkTimecode > 0 ? bookmarkTimecode : savedPlayerPosition;
                    Log.d("VideoPlayer", "Starting player with position: " + startPosition + "ms (bookmark: " + bookmarkTimecode + "ms, saved: " + savedPlayerPosition + "ms)");
                    
                    if (playerData.getPlayer() != null && "animelib".equalsIgnoreCase(playerData.getPlayer())) {
                        handleAnimelibPlayer(playerData, startPosition);
                    } else if (playerData.getPlayer() != null && "kodik".equalsIgnoreCase(playerData.getPlayer())) {
                        handleKodikPlayer(playerData, startPosition);
                    }
                    
                    // Обновляем полную информацию об аниме (асинхронно с API)
                    updateAnimeInfoHeaderFull();
                });
            });
        } else {
            // Если качества недоступны, запускаем плеер сразу
            long startPosition = bookmarkTimecode > 0 ? bookmarkTimecode : savedPlayerPosition;
            Log.d("VideoPlayer", "Starting player with position: " + startPosition + "ms (no qualities available)");
            
            if (playerData.getPlayer() != null && "animelib".equalsIgnoreCase(playerData.getPlayer())) {
                handleAnimelibPlayer(playerData, startPosition);
            } else if (playerData.getPlayer() != null && "kodik".equalsIgnoreCase(playerData.getPlayer())) {
                handleKodikPlayer(playerData, startPosition);
            }
            
            // Обновляем полную информацию об аниме (асинхронно с API)
            updateAnimeInfoHeaderFull();
        }
    }

    private void onEpisodeSelected(EpisodesListResponse.EpisodeItem episode) {
        Log.d("VideoPlayer", "Episode selected: " + episode.getNumber() + " (ID: " + episode.getId() + ")");

        // Reset bookmark timecode for new episode selection
        bookmarkTimecode = 0;
        Log.d("VideoPlayer", "Reset bookmark timecode for new episode");
        
        // Reset saved player position for new episode
        savedPlayerPosition = 0;
        Log.d("VideoPlayer", "Reset saved player position for new episode");
        
        // Reset auto-bookmark flag for new episode
        autoBookmarkSaved = false;
        Log.d("VideoPlayer", "Reset auto-bookmark flag for new episode");
        
        // Reset bookmark button color for new episode
        updateBookmarkButtonColor(false);

        // Stop current playback if playing
        stopCurrentPlayback();

        // Hide episodes in controller
        hideEpisodesInController();

        // Update current episode in both managers
        episodesManager.setCurrentEpisode(episode);
        commentsManager.setCurrentEpisode(episode);
        commentsManager.resetCommentsOnEpisodeChange(true);

        // Эпизод теперь сохраняется автоматически через закладки при добавлении
        Log.d("EpisodeMemory", "Episode " + episode.getNumber() + " is now current episode");

        // Update navigation buttons visibility
        episodesManager.updateEpisodeNavigationButtonsVisibility();

        // Update episodes RecyclerView to highlight current episode
        episodesManager.updateEpisodesRecyclerView();
        
        // СРАЗУ обновляем заголовок с номером эпизода (синхронно)
        updateEpisodeHeaderQuick();

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
                    
                    // Сохраняем выбранное качество в БД вместе с плеером и озвучкой
                    EpisodeResponse.PlayerData currentPlayer = playersManager.getCurrentPlayerData();
                    if (currentPlayer != null && currentPlayer.getPlayer() != null && currentPlayer.getTeam() != null) {
                        apiService.savePlayerPreferences(currentPlayer.getPlayer(), 
                                                        currentPlayer.getTeam().getId(), 
                                                        quality);
                        Log.d("VideoPlayer", "Saved quality preference: " + quality);
                    }

                    // If quality changed, restart with new quality (regardless of playback state)
                    if (!quality.equals(oldQuality) && player != null) {
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
                    // Update PlayersManager with new 4K setting
                    playersManager.setEnable4K(enabled);
                    Log.d("VideoPlayer", "4K setting changed to: " + enabled);
                    // Refresh qualities when 4K setting changes
                    List<String> newQualities = playersManager.getAvailableQualities();
                    Log.d("VideoPlayer", "New qualities after 4K toggle: " + newQualities);
                    if (!newQualities.isEmpty()) {
                        // Update preferred quality if current is not available
                        if (!newQualities.contains(preferredQuality)) {
                            String oldQuality = preferredQuality;
                            preferredQuality = newQualities.get(0);
                            Log.d("VideoPlayer", "Preferred quality changed from " + oldQuality + " to " + preferredQuality);
                        }
                        // Use currentSettingsDialog instead of dialog
                        if (currentSettingsBottomSheet != null) {
                            Log.d("VideoPlayer", "Updating SettingsBottomSheet with new qualities");
                            currentSettingsBottomSheet.updateQualities(newQualities, preferredQuality);
                        } else {
                            Log.w("VideoPlayer", "currentSettingsBottomSheet is null, cannot update");
                        }
                    } else {
                        Log.w("VideoPlayer", "New qualities list is empty!");
                    }
                },
                enableAmbientLight,
                enabled -> {
                    enableAmbientLight = enabled;
                    // Save to database
                    apiService.saveAmbientLightSetting(enabled);
                    // Update ambient light manager
                    if (ambientLightManager != null) {
                        ambientLightManager.setEnabled(enabled);
                    }
                    Log.d("VideoPlayer", "Ambient light setting changed to: " + enabled);
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
                    // Update skip indicators text
                    if (gesturesManager != null) {
                        gesturesManager.updateSkipDurationText(duration);
                    }
                    Log.d("VideoPlayer", "LongSkipDuration changed: " + duration);
                },
                currentTheme,
                themeMode -> {
                    currentTheme = themeMode;
                    // Apply theme immediately without recreating activity
                    ThemeUtils.applyThemeToActivity(VideoPlayerActivity.this, themeMode);
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
        
        // Приостанавливаем ambient подсветку при открытии bottom sheet
        if (ambientLightManager != null) {
            ambientLightManager.suspend();
        }
        
        // Возобновляем ambient подсветку при закрытии bottom sheet
        dialog.setOnDismissListener(dialogInterface -> {
            if (ambientLightManager != null) {
                ambientLightManager.resume();
            }
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
            // For Animelib, we need to restart with new quality URL
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
        
        // Обновляем currentPlayerData в PlayersManager для правильной подсветки после смены качества
        playersManager.setCurrentPlayerData(currentPlayerData);
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
        safeRunOnUiThread(() -> {
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
        safeRunOnUiThread(() -> {
            if (loadingOverlay != null) {
                loadingOverlay.setVisibility(View.GONE);
            }
        });
    }

    private void initializePlayer() {
        // Create LoadControl with larger buffer for 4K support
        LoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                        50000,  // min buffer (50s для 4K)
                        120000, // max buffer (120s для 4K)
                        2500,   // buffer for playback
                        5000    // buffer for playback after rebuffer
                )
                .build();
        
        // Create TrackSelector with 4K support
        TrackSelector trackSelector = new DefaultTrackSelector(this);
        
        // Create ExoPlayer with custom data source and 4K support
        player = new ExoPlayer.Builder(this)
                .setSeekBackIncrementMs(10000)
                .setSeekForwardIncrementMs(10000)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(httpDataSourceFactory))
                .setLoadControl(loadControl)
                .setTrackSelector(trackSelector)
                .setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                .build();

        playerView.setPlayer(player);
        
        // Set player for ambient light manager
        if (ambientLightManager != null) {
            ambientLightManager.setPlayer(player);
        }

        // Ensure controller is properly configured for play/pause buttons
        playerView.setUseController(true);
        updateControllerAutoHide();

        Log.d("PlayerInit", "ExoPlayer bound to PlayerView with controller enabled");
        
        // Update gestures manager with new player
        gesturesManager.updatePlayer(player);
        
        // Initialize timecode manager with UI components
        MaterialButton skipSegmentButton = findViewById(R.id.skipSegmentButton);
        timecodeManager.initializeViews(player, playerView, skipSegmentButton);

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
                Log.e("VideoPlayer", "Error type: " + error.errorCode + ", current quality: " + preferredQuality);
                String errorMsg = "Ошибка воспроизведения";

                // Check if this is a 4K playback error (Source error, decoder error, etc.)
                boolean is4KError = (preferredQuality != null && (preferredQuality.equals("2160p") || preferredQuality.equals("4Kp"))) &&
                        (error.getMessage().contains("Source error") || 
                         error.getMessage().contains("Decoder") ||
                         error.getMessage().contains("Video decoder error") ||
                         error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED ||
                         error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED);
                
                if (is4KError) {
                    Log.w("VideoPlayer", "4K playback failed, attempting fallback to 1080p");
                    errorMsg = "4K не поддерживается на этом устройстве. Переключаемся на 1080p...";
                    Toast.makeText(VideoPlayerActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    
                    // Try to fallback to 1080p
                    List<String> availableQualities = playersManager.getAvailableQualities();
                    if (availableQualities.contains("1080p")) {
                        preferredQuality = "1080p";
                        // Restart player with lower quality
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            long savedPosition = player.getCurrentPosition();
                            restartPlayerWithNewQuality();
                        }, 500);
                        return; // Don't show error toast
                    } else if (!availableQualities.isEmpty()) {
                        // Use any available quality
                        preferredQuality = availableQualities.get(0);
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            restartPlayerWithNewQuality();
                        }, 500);
                        return;
                    }
                } else if (error.getMessage().contains("403")) {
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
        safeRunOnUiThread(() -> {
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
        // Извлекаем anime ID из URL
        currentAnimeId = apiService.extractAnimeId(url);
        Log.d("VideoPlayer", "Extracted anime ID from URL: " + currentAnimeId);
        
        if (currentAnimeId == null) {
            Toast.makeText(this, "Не удалось извлечь ID аниме из URL", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Load related titles
        loadRelatedTitles();
        
        // ВАЖНО: Сначала загружаем эпизоды, потом плееры!
        // Это позволит правильно определить эпизод из закладки
        Log.d("VideoPlayer", "Loading episodes first, then players will be loaded for correct episode");
        loadEpisodes(currentAnimeId);
    }

    private void showPlayerSelectionDialogWithAutoSelect(List<EpisodeResponse.PlayerData> players) {
        hideLoading();

        Log.d("VideoPlayer", "showPlayerSelectionDialogWithAutoSelect called with " + players.size() + " players");
        Log.d("VideoPlayer", "NOTE: setPlayersData already called by PlayersManager, only checking for auto-select");

        // Попытка автоматического выбора плеера на основе сохраненных предпочтений
        // ВАЖНО: setPlayersData уже вызван в PlayersManager.loadPlayersForEpisode()!
        executor.execute(() -> {
            com.example.animelib.data.entity.PlayerPreferences prefs = apiService.loadPlayerPreferences();
            
            Log.d("VideoPlayer", "Loaded preferences from DB: " + (prefs != null ? 
                  ("player=" + prefs.getPlayer() + ", teamId=" + prefs.getTeamId() + ", quality=" + prefs.getPreferredQuality()) : 
                  "null"));
            
            EpisodeResponse.PlayerData matchingPlayer = null;
            
            if (prefs != null && prefs.getPlayer() != null && prefs.getTeamId() != null) {
                Log.d("VideoPlayer", "Found saved preferences: player=" + prefs.getPlayer() + ", teamId=" + prefs.getTeamId());
                
                // Сначала ищем точное совпадение (сохраненный плеер + озвучка)
                for (EpisodeResponse.PlayerData player : players) {
                    if (player.getPlayer() != null && 
                        player.getPlayer().equals(prefs.getPlayer()) && 
                        player.getTeam() != null && 
                        player.getTeam().getId() == prefs.getTeamId()) {
                        matchingPlayer = player;
                        Log.d("VideoPlayer", "Found exact match in saved player: " + player.getPlayer() + 
                              ", team: " + player.getTeam().getName());
                        break;
                    }
                }
                
                // Если не найдено в сохраненном плеере, ищем озвучку в других плеерах
                if (matchingPlayer == null) {
                    Log.d("VideoPlayer", "Team not found in saved player, searching in other players");
                    for (EpisodeResponse.PlayerData player : players) {
                        if (player.getTeam() != null && 
                            player.getTeam().getId() == prefs.getTeamId()) {
                            matchingPlayer = player;
                            Log.d("VideoPlayer", "Found team in different player: " + player.getPlayer() + 
                                  ", team: " + player.getTeam().getName());
                            break;
                        }
                    }
                }
            } else {
                Log.d("VideoPlayer", "No saved player preferences found");
            }
            
            // Финальный результат в UI потоке
            EpisodeResponse.PlayerData finalMatchingPlayer = matchingPlayer;
            safeRunOnUiThread(() -> {
                if (finalMatchingPlayer != null) {
                    // Автоматически выбираем найденный плеер
                    Log.d("VideoPlayer", "Auto-selecting player based on preferences (menu already shown by PlayersManager)");
                    
                    // Меню уже показано через PlayersManager.setPlayersData()
                    // Просто запускаем плеер
                    onPlayerSelected(finalMatchingPlayer);
                } else {
                    // Меню уже показано через PlayersManager.setPlayersData()
                    Log.d("VideoPlayer", "No matching player found, menu already visible from PlayersManager");
                }
                
                // Скрываем loading overlay в любом случае
                if (menuLoadingOverlay != null) menuLoadingOverlay.setVisibility(View.GONE);
            });
        });
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
            
            // Set timecodes from player data
            timecodeManager.setTimecodes(playerData);
            
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
        
        // Set timecodes from player data
        timecodeManager.setTimecodes(playerData);
        
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
        safeRunOnUiThread(() -> showLoading("Получение HLS ссылок..."));

        apiService.fetchKodikVideoLinksUnsafe(kodikSrc, new ApiService.KodikVideoCallback() {
            @Override
            public void onKodikVideoReceived(KodikResponse response) {
                safeRunOnUiThread(() -> {
                    hideLoading();
                    startHlsPlayer(response, seekToPosition);
                });
            }

            @Override
            public void onError(String error) {
                safeRunOnUiThread(() -> {
                    hideLoading();
                    Toast.makeText(VideoPlayerActivity.this, error, Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void loadEpisodes(String animeId) {
        Log.d("EpisodesAPI", "Loading episodes for anime_id: " + animeId);
        
        // Получаем media_slug из URL аниме для загрузки закладки
        String animeUrl = getIntent().getStringExtra("anime_url");
        String mediaSlug = null;
        if (animeUrl != null && !animeUrl.isEmpty()) {
            mediaSlug = ApiService.extractMediaSlugFromUrl(animeUrl);
        }
        
        // Use EpisodesManager to load episodes with bookmark
        if (mediaSlug != null) {
            episodesManager.loadEpisodesWithBookmark(animeId, mediaSlug);
        } else {
            episodesManager.loadEpisodes(animeId);
        }
    }

    /**
     * Загружает первый эпизод когда нет закладки
     */
    private void loadFirstEpisode() {
        // Reset bookmark timecode when loading first episode
        bookmarkTimecode = 0;
        Log.d("VideoPlayer", "Reset bookmark timecode for first episode");
        
        // Reset saved player position when loading first episode
        savedPlayerPosition = 0;
        Log.d("VideoPlayer", "Reset saved player position for first episode");
        
        // Reset auto-bookmark flag when loading first episode
        autoBookmarkSaved = false;
        Log.d("VideoPlayer", "Reset auto-bookmark flag for first episode");
        
        // Reset bookmark button color for first episode
        updateBookmarkButtonColor(false);
        
        // Получаем первый эпизод из списка
        List<EpisodesListResponse.EpisodeItem> episodes = episodesManager.getEpisodes();
        if (episodes != null && !episodes.isEmpty()) {
            EpisodesListResponse.EpisodeItem firstEpisode = episodes.get(0);
            Log.d("VideoPlayer", "Loading first episode: " + firstEpisode.getNumber());
            
            episodesManager.setCurrentEpisode(firstEpisode);
            commentsManager.setCurrentEpisode(firstEpisode);
            
            // СРАЗУ обновляем заголовок с номером эпизода
            updateEpisodeHeaderQuick();
            
            playersManager.loadPlayersForEpisode(firstEpisode.getId());
        } else {
            Log.d("VideoPlayer", "No episodes available, initializing menu without auto play");
            initializeMenuWithoutAutoPlay();
        }
    }
    
    private void fallbackToUrlDetection() {
        // Reset bookmark timecode when falling back to URL detection
        bookmarkTimecode = 0;
        Log.d("VideoPlayer", "Reset bookmark timecode for URL detection fallback");
        
        // Reset saved player position when falling back to URL detection
        savedPlayerPosition = 0;
        Log.d("VideoPlayer", "Reset saved player position for URL detection fallback");
        
        // Reset auto-bookmark flag when falling back to URL detection
        autoBookmarkSaved = false;
        Log.d("VideoPlayer", "Reset auto-bookmark flag for URL detection fallback");
        
        // Reset bookmark button color for URL detection fallback
        updateBookmarkButtonColor(false);
        
        String animeUrl = getIntent().getStringExtra("anime_url");
        episodesManager.findAndSetCurrentEpisodeFromUrl(animeUrl);
        EpisodesListResponse.EpisodeItem currentEpisode = episodesManager.getCurrentEpisode();
        if (currentEpisode != null) {
            commentsManager.setCurrentEpisode(currentEpisode);
            
            // СРАЗУ обновляем заголовок с номером эпизода
            updateEpisodeHeaderQuick();
            
            playersManager.loadPlayersForEpisode(currentEpisode.getId());
        } else {
            // Если не найден эпизод по URL, загружаем первый
            loadFirstEpisode();
        }
    }

    /**
     * Включает кнопку закладки когда плеер готов
     */
    private void enableBookmarkButton() {
        if (bookmarkButton != null) {
            bookmarkButton.setEnabled(true);
            bookmarkButton.setClickable(true);
            Log.d("VideoPlayer", "Bookmark button enabled");
        }
    }
    
    /**
     * Автоматически сохраняет закладку с текущим таймкодом
     */
    private void autoSaveBookmark() {
        // Проверяем что закладка еще не сохранена
        if (autoBookmarkSaved) {
            Log.d("VideoPlayer", "Auto-bookmark already saved, skipping");
            return;
        }
        
        Log.d("VideoPlayer", "Auto-saving bookmark on exit");
        
        // Получаем текущие данные эпизода
        EpisodeResponse.PlayerData currentPlayer = playersManager.getCurrentPlayerData();
        EpisodesListResponse.EpisodeItem currentEpisode = episodesManager.getCurrentEpisode();
        
        // Проверяем что данные готовы
        if (currentPlayer == null || currentEpisode == null) {
            Log.d("VideoPlayer", "Cannot auto-save bookmark - player or episode not ready");
            return;
        }
        
        // Получаем media_slug из URL аниме
        String animeUrl = getIntent().getStringExtra("anime_url");
        String mediaSlug = null;
        if (animeUrl != null && !animeUrl.isEmpty()) {
            mediaSlug = ApiService.extractMediaSlugFromUrl(animeUrl);
        }
        
        if (mediaSlug == null) {
            Log.d("VideoPlayer", "Cannot auto-save bookmark - media slug not available");
            return;
        }
        
        // Получаем текущее время воспроизведения и длительность
        long currentPosition = player != null ? player.getCurrentPosition() : 0;
        long duration = player != null ? player.getDuration() : 0;
        
        // Проверяем что позиция больше 10 секунд (чтобы не сохранять случайные клики)
        if (currentPosition < 10000) {
            Log.d("VideoPlayer", "Position too small for auto-save: " + currentPosition + "ms");
            return;
        }
        
        // Проверяем что просмотрено минимум 20% эпизода
        if (duration > 0) {
            double watchedPercentage = (double) currentPosition / duration * 100;
            if (watchedPercentage < 20.0) {
                Log.d("VideoPlayer", "Watched percentage too low for auto-save: " + 
                    String.format("%.1f", watchedPercentage) + "% (minimum 20%)");
                return;
            }
            Log.d("VideoPlayer", "Watched percentage: " + String.format("%.1f", watchedPercentage) + "%");
        } else {
            Log.d("VideoPlayer", "Duration not available, using position-based check only");
        }
        
        Log.d("VideoPlayer", "Auto-saving bookmark with data:");
        Log.d("VideoPlayer", "  - mediaSlug: " + mediaSlug);
        Log.d("VideoPlayer", "  - episodeId: " + currentEpisode.getId());
        Log.d("VideoPlayer", "  - episodeNumber: " + currentEpisode.getNumber());
        Log.d("VideoPlayer", "  - teamId: " + (currentPlayer.getTeam() != null ? currentPlayer.getTeam().getId() : "null"));
        Log.d("VideoPlayer", "  - currentPosition: " + currentPosition + "ms");
        Log.d("VideoPlayer", "  - duration: " + duration + "ms");
        
        // Используем BookmarkManager для добавления закладки (без UI обновлений)
        episodesManager.getBookmarkManager().addBookmark(
            mediaSlug,
            currentPlayer,
            currentEpisode,
            currentPosition,
            new BookmarkManager.BookmarkAddCallback() {
                @Override
                public void onBookmarkAdded(int episodeId) {
                    Log.d("VideoPlayer", "Auto-bookmark saved successfully for episode: " + episodeId);
                    autoBookmarkSaved = true; // Устанавливаем флаг что закладка сохранена
                }
                
                @Override
                public void onBookmarkError(String error) {
                    Log.e("VideoPlayer", "Failed to auto-save bookmark: " + error);
                    // Не устанавливаем флаг при ошибке, чтобы можно было повторить попытку
                }
            },
            false // Не показываем Toast при успехе для автосохранения
        );
    }
    
    /**
     * Обновляет цвет кнопки закладки
     * @param isBookmarked true если закладка добавлена, false если нет
     */
    private void updateBookmarkButtonColor(boolean isBookmarked) {
        if (bookmarkButton != null) {
            runOnUiThread(() -> {
                if (isBookmarked) {
                    // Красный цвет для добавленной закладки
                    bookmarkButton.setColorFilter(getResources().getColor(R.color.bookmark_color));
                    Log.d("VideoPlayer", "Bookmark button color changed to red");
                } else {
                    // Белый цвет для обычного состояния
                    bookmarkButton.setColorFilter(getResources().getColor(R.color.white_color));
                    Log.d("VideoPlayer", "Bookmark button color changed to white");
                }
            });
        }
    }
    
    /**
     * Обновляет список эпизодов после добавления закладки
     */
    private void updateEpisodesListAfterBookmark() {
        safeRunOnUiThread(() -> {
            // Получаем media_slug для обновления закладки
            String animeUrl = getIntent().getStringExtra("anime_url");
            String mediaSlug = null;
            if (animeUrl != null && !animeUrl.isEmpty()) {
                mediaSlug = ApiService.extractMediaSlugFromUrl(animeUrl);
            }
            
            if (mediaSlug != null) {
                // Обновляем закладку в EpisodesManager
                episodesManager.getBookmarkManager().fetchAnimeBookmark(mediaSlug, 
                    new BookmarkManager.AnimeBookmarkCallback() {
                        @Override
                        public void onBookmarkReceived(com.example.animelib.models.AnimeBookmarkResponse response) {
                            safeRunOnUiThread(() -> {
                                if (response != null && response.getData() != null) {
                                    // Обновляем закладку в адаптере
                                    episodesManager.updateBookmarkInAdapter(response.getData());
                                    Log.d("VideoPlayer", "Episodes list updated with new bookmark");
                                }
                            });
                        }
                        
                        @Override
                        public void onError(String error) {
                            Log.e("VideoPlayer", "Failed to update bookmark in episodes list: " + error);
                        }
                    });
            }
        });
    }

    private void initializeMenuWithoutAutoPlay() {
        // Players are now managed by PlayersManager
        episodesManager.updateEpisodeNavigationButtonsVisibility();
        episodesManager.updateEpisodesRecyclerView(); // Call this last to ensure currentEpisode is set

        // Загружаем плееры для текущего эпизода
        EpisodesListResponse.EpisodeItem currentEpisode = episodesManager.getCurrentEpisode();
        if (currentEpisode != null) {
            Log.d("VideoPlayer", "Loading players for episode in initializeMenuWithoutAutoPlay: " + currentEpisode.getNumber());
            playersManager.loadPlayersForEpisode(currentEpisode.getId());
        } else {
            Log.d("VideoPlayer", "No current episode available for loading players");
        }

        // PlayersManager handles auto-selection and menu display
        // Ensure loading overlay is hidden once players are available
        if (menuLoadingOverlay != null) menuLoadingOverlay.setVisibility(View.GONE);
        if (menuLoadingIndicator != null) menuLoadingIndicator.setVisibility(View.GONE);
    }

    /**
     * Быстрое обновление заголовка с номером эпизода (синхронно, без API)
     */
    private void updateEpisodeHeaderQuick() {
        safeRunOnUiThread(() -> {
            EpisodeResponse.PlayerData currentPlayerData = playersManager.getCurrentPlayerData();
            EpisodesListResponse.EpisodeItem currentEpisode = episodesManager.getCurrentEpisode();
            
            String tm = (currentPlayerData != null && currentPlayerData.getTeam() != null)
                    ? currentPlayerData.getTeam().getName() : null;
            String ep = (currentEpisode != null) ? currentEpisode.getNumber() : null;
            String em = (currentEpisode != null && currentEpisode.getName() != null && !Objects.equals(currentEpisode.getName(), ""))
                    ? currentEpisode.getName() : null;

            if (currentTeamName != null) currentTeamName.setText(tm != null ? tm : "");
            if (currentEpisodeNumberView != null) {
                currentEpisodeNumberView.setText(ep != null ? (ep + " серия") : "");
                Log.d("VideoPlayer", "Quick header update: episode " + ep);
            }
            if (currentEpisodeName != null)
                currentEpisodeName.setText(em != null ? (", " + em) : "");
        });
    }
    
    /**
     * Полное обновление заголовка с названием аниме (асинхронно с API)
     */
    private void updateAnimeInfoHeaderFull() {
        // Сначала быстро обновляем эпизод
        updateEpisodeHeaderQuick();
        
        // Затем асинхронно загружаем название аниме
        if (animeTitleView == null) return;
        String slugOrId = apiService.extractAnimeSlug(animeUrl);
        if (slugOrId == null) return;

        apiService.fetchAnimeInfo(slugOrId, new ApiService.AnimeInfoCallback() {
            @Override
            public void onAnimeInfoReceived(AnimeInfoResponse response) {
                safeRunOnUiThread(() -> {
                    if (response != null && response.getData() != null) {
                        String rus = response.getData().getRus_name();
                        animeTitleView.setText(rus != null ? rus : "");
                        Log.d("VideoPlayer", "Full header update: anime title set");
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.w("VideoPlayer", "Failed to load anime title: " + error);
            }
        });
    }
    
    /**
     * Старый метод для обратной совместимости
     */
    private void updateAnimeInfoHeader() {
        updateAnimeInfoHeaderFull();
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
                .setAllowChunklessPreparation(true) // Для лучшей совместимости с 4K HLS
                .createMediaSource(MediaItem.fromUri(hlsUrl));

        // Create LoadControl with larger buffer for 4K support
        LoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                        50000,  // min buffer (50s для 4K)
                        120000, // max buffer (120s для 4K)
                        2500,   // buffer for playback
                        5000    // buffer for playback after rebuffer
                )
                .build();
        
        // Create TrackSelector with 4K support
        TrackSelector trackSelector = new DefaultTrackSelector(this);
        
        // Create ExoPlayer with 4K support
        player = new ExoPlayer.Builder(this)
                .setSeekBackIncrementMs(10000)
                .setSeekForwardIncrementMs(10000)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(okHttpDataSourceFactory))
                .setLoadControl(loadControl)
                .setTrackSelector(trackSelector)
                .setVideoScalingMode(C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                .build();

        playerView.setPlayer(player);
        
        // Set player for ambient light manager
        if (ambientLightManager != null) {
            ambientLightManager.setPlayer(player);
        }

        // Ensure controller is properly configured for play/pause buttons
        playerView.setUseController(true);
        updateControllerAutoHide();

        Log.d("HlsPlayerInit", "HLS ExoPlayer bound to PlayerView with controller enabled");
        
        // Update gestures manager with new player
        gesturesManager.updatePlayer(player);
        
        // Initialize timecode manager with UI components
        MaterialButton skipSegmentButton = findViewById(R.id.skipSegmentButton);
        timecodeManager.initializeViews(player, playerView, skipSegmentButton);

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
                Log.e("HlsPlayer", "Error type: " + error.errorCode + ", current quality: " + preferredQuality);
                String errorMsg = "Ошибка HLS воспроизведения";

                // Check if this is a 4K playback error
                boolean is4KError = (preferredQuality != null && (preferredQuality.equals("2160p") || preferredQuality.equals("4Kp"))) &&
                        (error.getMessage().contains("Source error") || 
                         error.getMessage().contains("Decoder") ||
                         error.getMessage().contains("Video decoder error") ||
                         error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED ||
                         error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED);
                
                if (is4KError) {
                    Log.w("HlsPlayer", "4K HLS playback failed, attempting fallback to 720p");
                    errorMsg = "4K не поддерживается на этом устройстве. Переключаемся на 720p...";
                    Toast.makeText(VideoPlayerActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    
                    // For Kodik HLS, fallback to 720p (standard Kodik quality)
                    preferredQuality = "720p";
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        restartPlayerWithNewQuality();
                    }, 500);
                    return; // Don't show error toast
                } else if (error.getMessage().contains("403")) {
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
                        
                        // Проверяем есть ли следующий эпизод
                        if (episodesManager != null && episodesManager.getNextEpisode() != null) {
                            // Показываем оверлей с обратным отсчетом
                            showNextEpisodeOverlay();
                        } else {
                            Log.d("PlayerControls", "No next episode available");
                        }
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

        // Показываем индикатор загрузки только при буферизации
        boolean isBuffering = playbackState == Player.STATE_BUFFERING;

        if (isBuffering) {
            // Показываем спиннер, скрываем ОБЕ кнопки play/pause
            spinner.setVisibility(View.VISIBLE);
            if (play != null) {
                play.setVisibility(View.GONE);
                pause.setVisibility(View.GONE);
                Log.d("PlayLoadingIndicator", "Hiding PLAY button");
            }
            if (pause != null) {
                pause.setVisibility(View.GONE);
                assert play != null;
                play.setVisibility(View.GONE);
                Log.d("PlayLoadingIndicator", "Hiding PAUSE button");
            }
            Log.d("PlayLoadingIndicator", "Showing loading spinner - hiding BOTH play/pause buttons");
        } else {
            // Скрываем спиннер, восстанавливаем play/pause согласно состоянию плеера
            spinner.setVisibility(View.GONE);
            if (player != null) {
                boolean isPlaying = player.isPlaying();
                if (play != null) {
                    play.setVisibility(isPlaying ? View.GONE : View.VISIBLE);
                    Log.d("PlayLoadingIndicator", "Showing PLAY button: " + !isPlaying);
                }
                if (pause != null) {
                    pause.setVisibility(isPlaying ? View.VISIBLE : View.GONE);
                    Log.d("PlayLoadingIndicator", "Showing PAUSE button: " + isPlaying);
                }
                Log.d("PlayLoadingIndicator", "Hiding loading spinner - showing " + (isPlaying ? "pause" : "play") + " button");
            }
        }
    }
    
    /**
     * Добавляет текущую серию в закладки
     */
    private void addBookmark() {
        Log.d("VideoPlayer", "Add bookmark method called");

        // Получаем текущие данные эпизода
        EpisodeResponse.PlayerData currentPlayer = playersManager.getCurrentPlayerData();
        EpisodesListResponse.EpisodeItem currentEpisode = episodesManager.getCurrentEpisode();
        
        // Проверяем что данные готовы
        if (currentPlayer == null) {
            Log.e("VideoPlayer", "Current player is null, cannot add bookmark");
            return;
        }
        
        if (currentEpisode == null) {
            Log.e("VideoPlayer", "Current episode is null, cannot add bookmark");
            Toast.makeText(this, "Эпизод не выбран", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Получаем media_slug из URL аниме
        String animeUrl = getIntent().getStringExtra("anime_url");
        String mediaSlug = null;
        if (animeUrl != null && !animeUrl.isEmpty()) {
            mediaSlug = ApiService.extractMediaSlugFromUrl(animeUrl);
        }
        
        if (mediaSlug == null) {
            Log.e("VideoPlayer", "Media slug is null, cannot add bookmark");
            Toast.makeText(this, "Не удалось определить аниме", Toast.LENGTH_SHORT).show();
            return;
        }

        // Получаем текущее время воспроизведения
        long currentPosition = player.getCurrentPosition();
        
        Log.d("VideoPlayer", "Adding bookmark with data:");
        Log.d("VideoPlayer", "  - mediaSlug: " + mediaSlug);
        Log.d("VideoPlayer", "  - episodeId: " + currentEpisode.getId());
        Log.d("VideoPlayer", "  - episodeNumber: " + currentEpisode.getNumber());
        Log.d("VideoPlayer", "  - teamId: " + (currentPlayer.getTeam() != null ? currentPlayer.getTeam().getId() : "null"));
        Log.d("VideoPlayer", "  - currentPosition: " + currentPosition + "ms");

        // Используем BookmarkManager для добавления закладки
        episodesManager.getBookmarkManager().addBookmark(
            mediaSlug,
            currentPlayer,
            currentEpisode,
            currentPosition,
            new BookmarkManager.BookmarkAddCallback() {
                @Override
                public void onBookmarkAdded(int episodeId) {
                    // Меняем цвет кнопки на красный
                    updateBookmarkButtonColor(true);
                    
                    // Обновляем список эпизодов
                    updateEpisodesListAfterBookmark();
                }
                
                @Override
                public void onBookmarkError(String error) {
                    // Кнопка остается белой при ошибке
                    updateBookmarkButtonColor(false);
                }
            },
            true // Показываем Toast при успехе для ручного добавления
        );
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
    protected void onPause() {
        super.onPause();
        
        // Автоматически сохраняем закладку при сворачивании приложения
        autoSaveBookmark();
        
        // Pause playback
        if (player != null && player.isPlaying()) {
            player.pause();
        }
    }
    
    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        
        // Убеждаемся что fitsSystemWindows установлен правильно после изменения конфигурации
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            rootView.setFitsSystemWindows(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Убеждаемся что fitsSystemWindows установлен правильно
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            rootView.setFitsSystemWindows(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Автоматически сохраняем закладку при закрытии плеера
        autoSaveBookmark();
        
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
        if (timecodeManager != null) {
            timecodeManager.cleanup();
        }
        if (ambientLightManager != null) {
            ambientLightManager.cleanup();
        }
        
        // Останавливаем обратный отсчет следующего эпизода
        if (nextEpisodeHandler != null && nextEpisodeRunnable != null) {
            nextEpisodeHandler.removeCallbacks(nextEpisodeRunnable);
        }
    }

    @Override
    public void onBackPressed() {
        // If video is playing and not in PiP mode, enter PiP instead of closing
        if (player != null && player.isPlaying() && !isInPictureInPictureMode) {
            // Автоматически сохраняем закладку перед переходом в PiP
            autoSaveBookmark();
            enterPictureInPictureMode();
            return;
        }

        // Автоматически сохраняем закладку перед закрытием
        autoSaveBookmark();
        
        // Otherwise, close the activity
        super.onBackPressed();
    }
}


