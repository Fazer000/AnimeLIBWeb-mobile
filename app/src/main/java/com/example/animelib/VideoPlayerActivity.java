package com.example.animelib;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.PictureInPictureParams;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Build;
import android.util.Log;
import android.util.Rational;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.view.ViewConfiguration;
import android.view.animation.AlphaAnimation;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.DefaultTimeBar;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.animelib.models.AnimeInfoResponse;

import com.example.animelib.adapters.HorizontalEpisodesAdapter;
import com.example.animelib.adapters.PlayerTabsAdapter;
import com.example.animelib.adapters.CommentsAdapter;
import com.example.animelib.api.AnimeApiService;
import com.example.animelib.dialogs.SettingsDialog;
import com.example.animelib.util.ThemeUtils;
import com.example.animelib.models.EpisodeResponse;
import com.example.animelib.models.EpisodesListResponse;
import com.example.animelib.models.KodikResponse;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;


@UnstableApi
public class VideoPlayerActivity extends AppCompatActivity {
    public static final String EXTRA_VIDEO_URL = "video_url";
    public static final String EXTRA_ANIME_URL = "anime_url";
    private static final String EXTRA_VIDEO_QUALITIES = "video_qualities";

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
    private AnimeApiService apiService;

    // Menu components
    private ImageButton ibClosePlayer;
    private ImageButton menuToggleButton;
    private ImageButton settingsButton;
    private View slidingMenuPanel;
    private View menuOverlay; // scrim for outside-tap to close
    private View menuLoadingIndicator;
    private View menuLoadingOverlay;
    private TextView seekPreviewText;
    private TextView holdSpeedToast;
    private float swipeAccumulatedDx = 0f;
    private boolean isSwipingSeek = false;
    private long basePositionMs = 0L;
    private Float lastSwipeX = null;
    private float swipeStartX = 0f;
    private float swipeStartY = 0f;
    private int swipeTouchSlopPx = 0;
    private ImageButton closeMenuButton;
    private TabLayout playerTabLayout;
    private ViewPager2 playersViewPager;
    private PlayerTabsAdapter playerTabsAdapter;

    // Comments panel
    private View commentsPanel;
    private ImageButton closeCommentsButton;
    private RecyclerView commentsRecyclerView;
    private View commentsLoadingOverlay;
    private ImageButton commentsButton;
    private CommentsAdapter commentsAdapter;
    private boolean isCommentsVisible = false;
    private int commentsPanelWidth = 320; // dp
    private int commentsCurrentPage = 1;
    private boolean commentsHasNextPage = true;
    private boolean isLoadingComments = false;
    private String commentsSortType = "desc";
    private TextView sortDesc;
    private TextView sortAsc;
    private SettingsDialog currentSettingsDialog;
    private TextView sortVotes;
    private ImageButton commentsOptionsButton;
    
    // Picture-in-Picture support
    private boolean isInPictureInPictureMode = false;
    private boolean wasPlayingBeforePiP = false;
    private boolean wasCommentsVisibleBeforePiP = false;

    // Episodes in controller
    private ImageButton episodesMenuButton;
    private RecyclerView episodesHorizontalRecyclerView;
    private View playersControlBar;
    private boolean isEpisodesMenuVisible = false;
    private TextView animeTitleView;
    private TextView animeStatusView;
    private TextView currentEpisodeNumberView;
    private TextView currentTeamName;
    private TextView currentEpisodeName;

    // Episode navigation buttons
    private ImageButton prevEpisodeButton;
    private ImageButton nextEpisodeButton;

    // Controller visibility state
    private boolean isControllerVisible = false;

    // Player data
    private List<EpisodeResponse.PlayerData> allPlayers;
    private List<EpisodesListResponse.EpisodeItem> episodes;
    private List<EpisodeResponse.PlayerData> animelibPlayers;
    private List<EpisodeResponse.PlayerData> kodikPlayers;
    private EpisodesListResponse.EpisodeItem currentEpisode;
    private EpisodeResponse.PlayerData currentPlayerData;
    private KodikResponse currentKodikResponse;
    private String currentAnimeId;

    // User preferences
    private String preferredPlayerType; // "animelib" or "kodik"
    private EpisodeResponse.PlayerData preferredAnimelibPlayer;
    private EpisodeResponse.PlayerData preferredKodikPlayer;
    private String preferredQuality; // preferred quality (e.g., "720", "480", etc.)
    private boolean enable4K = false;
    private boolean autoPlay = true;
    private int longSkipDuration = 85; // seconds
    private int currentTheme = ThemeUtils.THEME_SYSTEM;

    // Menu state
    private boolean isMenuVisible = false;
    private int menuWidth = 300; // dp

    private int controllerShowTimeoutMs = 4000;

    private boolean isHoldToSpeed = false;
    
    // Episode memory now handled through AnimeApiService

    public static void start(Activity context, String videoUrl) {
        Intent intent = new Intent(context, VideoPlayerActivity.class);
        intent.putExtra(EXTRA_VIDEO_URL, videoUrl);
        context.startActivity(intent);
    }

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
        controllerView = playerView.findViewById(R.id.exo_controller);
        timeBar = controllerView.findViewById(R.id.exo_progress);

        
        // Configure PlayerView to show controls for shorter time
        playerView.setControllerShowTimeoutMs(controllerShowTimeoutMs); // Show for 2 seconds instead of default 3
        playerView.setControllerAutoShow(true);
        playerView.setControllerHideOnTouch(true);
        playerView.setControllerAnimationEnabled(false);
        playerView.setAnimation(new AlphaAnimation(0.0f, 1.0f));

        // Initialize network components
        executor = Executors.newSingleThreadExecutor();
        apiService = new AnimeApiService(this);
        
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
                .setUserAgent("Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1")
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
            return;
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, android.content.res.Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        this.isInPictureInPictureMode = isInPictureInPictureMode;
        
        if (isInPictureInPictureMode) {
            // Entering PiP mode
            wasPlayingBeforePiP = player != null && player.isPlaying();
            wasCommentsVisibleBeforePiP = isCommentsVisible;
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                PictureInPictureParams params = new PictureInPictureParams.Builder()
                    .setAspectRatio(new Rational(16, 9)) // 16:9 aspect ratio
                    .build();
                enterPictureInPictureMode(params);
            } catch (Exception e) {
                Log.e("VideoPlayer", "Failed to enter Picture-in-Picture mode", e);
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
        if (isCommentsVisible) {
            hideCommentsPanel();
        }
        if (episodesHorizontalRecyclerView != null) {
            episodesHorizontalRecyclerView.setVisibility(View.GONE);
        }
        
        // Hide PiP button in PiP mode
        ImageButton pipButton = findViewById(R.id.pipButton);
        if (pipButton != null) {
            pipButton.setVisibility(View.GONE);
        }
        
        // Hide settings dialog if open
        if (currentSettingsDialog != null && currentSettingsDialog.isShowing()) {
            currentSettingsDialog.dismiss();
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
        
        // Restore episodes list state (visible but transparent, in final position)
        if (episodesHorizontalRecyclerView != null) {
            episodesHorizontalRecyclerView.setVisibility(View.VISIBLE);
            episodesHorizontalRecyclerView.setAlpha(0.0f);
            episodesHorizontalRecyclerView.setTranslationY(0f);
        }
        
        // Restore comments panel state if it was visible before PiP
        if (wasCommentsVisibleBeforePiP && !isCommentsVisible) {
            showCommentsPanel();
        }
        
        // Restore players control bar position (shifted down by episodes height)
        if (playersControlBar != null && episodesHorizontalRecyclerView != null) {
            int episodesHeight = episodesHorizontalRecyclerView.getHeight();
            if (episodesHeight > 0) {
                playersControlBar.setTranslationY(episodesHeight);
            }
        }
    }

    private void setupFullscreen() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                           WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    // Вспомогательный метод для преобразования dp в px
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @SuppressLint({"ClickableViewAccessibility", "RtlHardcoded"})
    private void setupMenu() {
        // Initialize menu views
        slidingMenuPanel = findViewById(R.id.slidingMenuPanel);
        commentsPanel = findViewById(R.id.commentsPanel);
        closeMenuButton = findViewById(R.id.closeMenuButton);
        closeCommentsButton = findViewById(R.id.closeCommentsButton);
        playerTabLayout = findViewById(R.id.playerTabLayout);
        playersViewPager = findViewById(R.id.playersViewPager);
        menuOverlay = findViewById(R.id.menuOverlay);
        menuLoadingIndicator = findViewById(R.id.menuLoadingIndicator);
        menuLoadingOverlay = findViewById(R.id.menuLoadingOverlay);
        commentsRecyclerView = findViewById(R.id.commentsRecyclerView);
        commentsLoadingOverlay = findViewById(R.id.commentsLoadingOverlay);
        commentsOptionsButton = findViewById(R.id.commentsOptionsButton);
        seekPreviewText = findViewById(R.id.seekPreviewText);
        holdSpeedToast = findViewById(R.id.holdSpeedToast);

        // Find buttons in player controller
        if (controllerView != null) {
            ibClosePlayer = controllerView.findViewById(R.id.ibClosePlayer);
            settingsButton = controllerView.findViewById(R.id.settingsButton);
            animeTitleView = controllerView.findViewById(R.id.animeTitle);
            currentTeamName = controllerView.findViewById(R.id.currentTeamName);
            currentEpisodeName = controllerView.findViewById(R.id.currentEpisodeName);
            currentEpisodeNumberView = controllerView.findViewById(R.id.currentEpisodeNumber);
            episodesMenuButton = controllerView.findViewById(R.id.episodesMenuButton);
            commentsButton = controllerView.findViewById(R.id.commentsButton);
            menuToggleButton = controllerView.findViewById(R.id.menuToggleButton);
            prevEpisodeButton = controllerView.findViewById(R.id.prevEpisodeButton);
            nextEpisodeButton = controllerView.findViewById(R.id.nextEpisodeButton);
            episodesHorizontalRecyclerView = controllerView.findViewById(R.id.episodesHorizontalRecyclerView);
            playersControlBar = controllerView.findViewById(R.id.playersControlBar);
            
            // Setup all player control buttons
            setupPlayerControlButtons();
            
            // Configure episode navigation buttons
            if (prevEpisodeButton != null) {
                prevEpisodeButton.setOnClickListener(v -> {
                    Log.d("EpisodeNav", "Previous button clicked");
                    if (prevEpisodeButton.isEnabled()) {
                    navigateToPreviousEpisode();
                    } else {
                        Log.d("EpisodeNav", "Previous button is disabled");
                    }
                });
            }
            
            if (nextEpisodeButton != null) {
                nextEpisodeButton.setOnClickListener(v -> {
                    Log.d("EpisodeNav", "Next button clicked");
                    if (nextEpisodeButton.isEnabled()) {
                    navigateToNextEpisode();
                    } else {
                        Log.d("EpisodeNav", "Next button is disabled");
                    }
                });
            }
        }

        // Setup controller visibility listener to hide/show our buttons
        playerView.setControllerVisibilityListener((PlayerView.ControllerVisibilityListener) visibility -> {
            isControllerVisible = visibility == View.VISIBLE;

            // Episode navigation buttons visibility depends on controller visibility and episode availability
            updateEpisodeNavigationButtonsVisibility();
            
            // Animate DefaultTimeBar with smooth fade and scale
            if (controllerView != null) {
                if (timeBar != null) {
                    // Disable default slide animation by setting translationY to 0
                    timeBar.setTranslationY(0);
                    if (isControllerVisible) {
                        // Show with fade in and slight scale up
                    timeBar.animate()
                            .alpha(1.0f)
                            .scaleY(1.0f)
                            .translationY(0)
                            .setDuration(200)
                            .start();
                    } else {
                        // Hide with fade out and slight scale down
                        timeBar.animate()
                            .alpha(0.0f)
                            .scaleY(0.8f)
                            .translationY(0)
                        .setDuration(150)
                        .start();
                    }
                }
            }
            if (settingsButton != null) {
                settingsButton.setVisibility(isControllerVisible ? View.VISIBLE : View.GONE);
            }
            if (episodesMenuButton != null) {
                episodesMenuButton.setVisibility(isControllerVisible ? View.VISIBLE : View.GONE);
            }
            if (menuToggleButton != null) {
                menuToggleButton.setVisibility(isControllerVisible ? View.VISIBLE : View.GONE);
            }
            if (commentsButton != null) {
                commentsButton.setVisibility(isControllerVisible ? View.VISIBLE : View.GONE);
            }
        });

        // Initially hide our buttons since controller is hidden by default
        if (episodesMenuButton != null) {
            episodesMenuButton.setVisibility(View.GONE);
        }
        if (menuToggleButton != null) {
            menuToggleButton.setVisibility(View.GONE);
        }
        if (commentsButton != null) {
            commentsButton.setVisibility(View.GONE);
        }
        // Don't hide episode navigation buttons initially - let updateEpisodeNavigationButtonsVisibility handle them
        if (prevEpisodeButton != null) {
            prevEpisodeButton.setVisibility(View.VISIBLE);
        }
        if (nextEpisodeButton != null) {
            nextEpisodeButton.setVisibility(View.VISIBLE);
        }

        if (settingsButton != null) {
            settingsButton.setVisibility(View.GONE);
        }

        // Setup player listener to show/hide navigation buttons based on episode availability
        if (player != null) {
            player.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    updateEpisodeNavigationButtonsVisibility();
                }
            });
        }

        // Calculate menu width in pixels
        float density = getResources().getDisplayMetrics().density;
        menuWidth = (int) (menuWidth * density);
        commentsPanelWidth = (int) (commentsPanelWidth * density);

        // Set initial menu position (hidden)
        if (slidingMenuPanel != null) {
            slidingMenuPanel.setTranslationX(menuWidth);
        }
        if (commentsPanel != null) {
            commentsPanel.setTranslationX(commentsPanelWidth);
        }

        // Prepare overlay (click outside to close)
        if (menuOverlay != null) {
            menuOverlay.setVisibility(View.GONE);
            menuOverlay.setAlpha(0f);
            // Important: allow clicks to проходить к панели, когда тап внутри панели
            menuOverlay.setClickable(false);
            // Close only when tapping outside panel area
            menuOverlay.setOnTouchListener((v, event) -> {
                if (!isMenuVisible && !isCommentsVisible) return false;
                if (event.getAction() != MotionEvent.ACTION_DOWN) return false;

                int x = (int) event.getRawX();
                int y = (int) event.getRawY();
                boolean insideAny = false;
                if (slidingMenuPanel != null && isMenuVisible) {
                    Rect panelRect = new Rect();
                    slidingMenuPanel.getGlobalVisibleRect(panelRect);
                    insideAny |= panelRect.contains(x, y);
                }
                if (commentsPanel != null && isCommentsVisible) {
                    Rect cRect = new Rect();
                    commentsPanel.getGlobalVisibleRect(cRect);
                    insideAny |= cRect.contains(x, y);
                }
                if (!insideAny) {
                    if (isMenuVisible) hideMenu();
                    if (isCommentsVisible) hideCommentsPanel();
                    return true; // consume outside tap
                }
                return false; // let touches on panel pass through
            });
        }

        // Set initial episodes RecyclerView state (visible but transparent, in final position)
        if (episodesHorizontalRecyclerView != null) {
            episodesHorizontalRecyclerView.setVisibility(View.VISIBLE);
            episodesHorizontalRecyclerView.setAlpha(0.0f);
            episodesHorizontalRecyclerView.setTranslationY(0f);
            
            // Set up ViewTreeObserver to position players control bar after episodes are measured
            episodesHorizontalRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (playersControlBar != null && episodesHorizontalRecyclerView != null) {
                        int episodesHeight = episodesHorizontalRecyclerView.getHeight();
                        if (episodesHeight > 0) {
                            playersControlBar.setTranslationY(episodesHeight);
                            // Remove listener after first measurement
                            episodesHorizontalRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                    }
                }
            });
        }
        
        // Set initial players control bar state
        if (playersControlBar != null) {
            playersControlBar.setAlpha(1.0f);
            playersControlBar.setTranslationY(0f); // Will be adjusted by ViewTreeObserver
        }

        // Setup menu toggle button
        if (menuToggleButton != null) {
            menuToggleButton.setOnClickListener(v -> toggleMenu());
        }

        // Setup comments button
        if (commentsButton != null) {
            commentsButton.setOnClickListener(v -> toggleCommentsPanel());
        }



// Setup header 3-dots menu for sorting
        if (commentsOptionsButton != null) {
            commentsOptionsButton.setOnClickListener(v -> {
                String[] items = {"Новые", "Старые", "Популярные"};

                // Создаем кастомный диалог
                Dialog dialog = new Dialog(this);
                dialog.setContentView(R.layout.custom_alert_dialog);

                // Настраиваем окно
                Window window = dialog.getWindow();
                if (window != null) {
                    window.setBackgroundDrawableResource(R.drawable.non_stroke_background);
                }

                // Находим элементы
                TextView title = dialog.findViewById(R.id.dialog_title);
                title.setText("Сортировка комментариев");

                LinearLayout optionsLayout = dialog.findViewById(R.id.options_layout);

                // Добавляем варианты
                for (int i = 0; i < items.length; i++) {
                    MaterialButton button = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
                    button.setText(items[i]);
                    button.setTextColor(Color.WHITE);
                    button.setBackgroundColor(Color.TRANSPARENT);
                    button.setStrokeWidth(0);
                    button.setRippleColor(ColorStateList.valueOf(0x20FFFFFF)); // Ripple эффект
                    button.setCornerRadius(0);
                    button.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
                    button.setLayoutParams(new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            dpToPx(48) // Фиксированная высота
                    ));

                    final int which = i;
                    button.setOnClickListener(v1 -> {
                        switch (which) {
                            case 0: changeCommentsSort("desc"); break;
                            case 1: changeCommentsSort("asc"); break;
                            case 2: changeCommentsSort("votes_up"); break;
                        }
                        dialog.dismiss();
                    });

                    optionsLayout.addView(button);
                }

                dialog.show();
            });
        }

        // Setup settings button
        if (settingsButton != null) {
            settingsButton.setOnClickListener(v -> showSettingsDialog());
        }

        // Setup Picture-in-Picture button
        ImageButton pipButton = findViewById(R.id.pipButton);
        if (pipButton != null) {
            pipButton.setOnClickListener(v -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    enterPictureInPictureMode();
                } else {
                    Toast.makeText(this, "Picture-in-Picture поддерживается только на Android 8.0+", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (ibClosePlayer != null) {
            ibClosePlayer.setOnClickListener(v -> finish());
        }

        // Setup episodes menu button
        if (episodesMenuButton != null) {
            episodesMenuButton.setOnClickListener(v -> toggleEpisodesInController());
        }

        // Setup episode navigation buttons
        if (prevEpisodeButton != null) {
            prevEpisodeButton.setOnClickListener(v -> navigateToPreviousEpisode());
        }
        if (nextEpisodeButton != null) {
            nextEpisodeButton.setOnClickListener(v -> navigateToNextEpisode());
        }

        // Setup close menu button
        if (closeMenuButton != null) {
            closeMenuButton.setOnClickListener(v -> {
                if (currentPlayerData != null) {
                    hideMenu();
                } else {
                    finish();
                }
            });
        }

        // Setup close comments button
        if (closeCommentsButton != null) {
            closeCommentsButton.setOnClickListener(v -> hideCommentsPanel());
        }

        // Setup episodes RecyclerView
        setupEpisodesRecyclerView();

        // Setup swipe-to-seek on PlayerView
        setupSwipeSeek();
        setupHoldToSpeed();

        // Setup ViewPager with tabs
        if (playersViewPager != null && playerTabLayout != null) {
            playerTabsAdapter = new PlayerTabsAdapter(
                new ArrayList<>(), // animelibPlayers will be set later
                new ArrayList<>(), // kodikPlayers will be set later
                null, // currentPlayerData will be set later
                this::onPlayerSelected
            );
            playersViewPager.setAdapter(playerTabsAdapter);

            // Setup TabLayout with ViewPager
            new TabLayoutMediator(playerTabLayout, playersViewPager,
                (tab, position) -> {
                    if (position == 0) {
                        tab.setText("AnimeLib");
                    } else {
                        tab.setText("Kodik");
                    }
                }).attach();
        }

        // Open menu immediately on activity start and show inline loading
        showMenu();
        if (menuLoadingOverlay != null) menuLoadingOverlay.setVisibility(View.VISIBLE);
    }

    private void setupHoldToSpeed() {
        if (playerView == null || isSwipingSeek) return;
        playerView.setOnLongClickListener(v -> {
            if (isSwipingSeek) return false;
            isHoldToSpeed = true;
            if (player == null) return false;
            player.setPlaybackSpeed(2.0f);
            if (holdSpeedToast != null) {
                holdSpeedToast.setVisibility(View.VISIBLE);
                holdSpeedToast.setAlpha(0f);
                holdSpeedToast.animate().alpha(1f).setDuration(120).start();
            }
            isHoldToSpeed = false;
            return true; // consume long press
        });
    }

    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
    private void setupSwipeSeek() {
        if (playerView == null) return;
        if (swipeTouchSlopPx == 0) swipeTouchSlopPx = ViewConfiguration.get(this).getScaledTouchSlop();
        playerView.setOnTouchListener((v, event) -> {
            if (player == null || isHoldToSpeed) return false;
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    // Не перехватываем сразу — даём кликам/контролам работать
                    isSwipingSeek = false;
                    swipeAccumulatedDx = 0f;
                    basePositionMs = player.getCurrentPosition();
                    lastSwipeX = event.getX();
                    swipeStartX = event.getX();
                    swipeStartY = event.getY();
                    if (seekPreviewText != null) {
                        seekPreviewText.setVisibility(View.GONE);
                        seekPreviewText.setText("0 c");
                    }
                    return false;
                case MotionEvent.ACTION_MOVE:
                    float currentX = event.getX();
                    float currentY = event.getY();
                    float totalDx = Math.abs(currentX - swipeStartX);
                    float totalDy = Math.abs(currentY - swipeStartY);
                    boolean passedDeadZone = totalDx > swipeTouchSlopPx && totalDx > totalDy * 1.5f;

                    if (!isSwipingSeek) {
                        if (passedDeadZone) {
                            v.postDelayed(() -> isSwipingSeek = true, 60); // ебашим задержку 60мс, чтобы не срабатывало мгновенно
                            // блокируем перехват родителями (ViewPager и т.п.)
                            ViewParent p = v.getParent();
                            if (p != null) p.requestDisallowInterceptTouchEvent(true);
                            // показать превью
                            if (seekPreviewText != null) seekPreviewText.setVisibility(View.VISIBLE);
                            updatePlayLoadingIndicator(player.getPlaybackState());
                            return true;
                        } else {
                            return false; // пока не прошли dead zone — не трогаем
                        }
                    }

                    // уже в режиме свайпа — накапливаем дельту
                    if (lastSwipeX == null) lastSwipeX = currentX;
                    float delta = currentX - lastSwipeX;
                    lastSwipeX = currentX;
                    swipeAccumulatedDx += delta;
                    long offsetSec = Math.round(swipeAccumulatedDx / 12f);
                    if (seekPreviewText != null) {
                        String sign = offsetSec >= 0 ? "+" : "";
                        seekPreviewText.setText(sign + offsetSec + " c");
                    }
                    updatePlayLoadingIndicator(player.getPlaybackState());
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (isSwipingSeek) {
                        long finalOffsetMs = Math.round(swipeAccumulatedDx / 12f) * 1000L;
                        long newPos = Math.max(0, basePositionMs + finalOffsetMs);
                        long dur = player.getDuration();
                        if (dur > 0) newPos = Math.min(newPos, dur);
                        player.seekTo(newPos);
                        if (seekPreviewText != null) seekPreviewText.setVisibility(View.GONE);
                        v.postDelayed(() -> isSwipingSeek = true, 60);
                        swipeAccumulatedDx = 0f;
                        lastSwipeX = null;
                        updatePlayLoadingIndicator(player.getPlaybackState());
                        return true; // съедаем up, чтобы не кликалось
                    }
                    // если был long press (плашка видна) — вернуть скорость и съесть up
                    if (holdSpeedToast != null && holdSpeedToast.getVisibility() == View.VISIBLE) {
                        if (player != null) player.setPlaybackSpeed(1.0f);
                        holdSpeedToast.animate().alpha(0f).setDuration(120).withEndAction(() -> holdSpeedToast.setVisibility(View.GONE)).start();
                        updatePlayLoadingIndicator(player.getPlaybackState());
                        return true;
                    }
                    // не было свайпа — передаём дальше, чтобы сработали клики
                    isSwipingSeek = false;
                    swipeAccumulatedDx = 0f;
                    lastSwipeX = null;
                    return false;
            }
            return false;
        });
    }

    private void setupEpisodesRecyclerView() {
        if (episodesHorizontalRecyclerView != null) {
            // Set horizontal layout manager
            LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
            episodesHorizontalRecyclerView.setLayoutManager(layoutManager);

            if (episodes != null && !episodes.isEmpty()) {
                HorizontalEpisodesAdapter adapter = new HorizontalEpisodesAdapter(episodes, currentEpisode, this::onEpisodeSelected);
                episodesHorizontalRecyclerView.setAdapter(adapter);
            }
        }
    }

    private void updateEpisodesRecyclerView() {
        if (episodesHorizontalRecyclerView != null && episodes != null) {
            Log.d("EpisodesUI", "Updating episodes RecyclerView. Current episode: " + 
                  (currentEpisode != null ? currentEpisode.getNumber() + " (ID: " + currentEpisode.getId() + ")" : "null"));
            
            // Ensure currentEpisode points to the instance from episodes list
            if (currentEpisode != null) {
                EpisodesListResponse.EpisodeItem normalized = findEpisodeInList(currentEpisode, episodes);
                if (normalized != null && normalized != currentEpisode) {
                    Log.d("EpisodesUI", "Normalizing currentEpisode reference to list instance");
                    currentEpisode = normalized;
                }
            }

            HorizontalEpisodesAdapter adapter = new HorizontalEpisodesAdapter(episodes, currentEpisode, this::onEpisodeSelected);
            episodesHorizontalRecyclerView.setAdapter(adapter);
            
            // Scroll to current episode if available (prefer by id, then by number)
            if (currentEpisode != null) {
                int targetIndex = -1;
                // Try by id first
                for (int i = 0; i < episodes.size(); i++) {
                    if (episodes.get(i).getId() == currentEpisode.getId()) {
                        targetIndex = i;
                        break;
                    }
                }
                // Fallback by number
                if (targetIndex == -1 && currentEpisode.getNumber() != null) {
                    for (int i = 0; i < episodes.size(); i++) {
                        if (episodes.get(i).getNumber() != null &&
                                episodes.get(i).getNumber().equals(currentEpisode.getNumber())) {
                            targetIndex = i;
                            break;
                        }
                    }
                }

                if (targetIndex >= 0) {
                    Log.d("EpisodesUI", "Scrolling to episode " + currentEpisode.getNumber() + " at position " + targetIndex);
                    episodesHorizontalRecyclerView.smoothScrollToPosition(targetIndex);
                } else {
                    Log.d("EpisodesUI", "Current episode not found in list for scrolling");
                }
            } else {
                Log.d("EpisodesUI", "No current episode to scroll to");
            }
        }
    }

    private EpisodesListResponse.EpisodeItem findEpisodeInList(EpisodesListResponse.EpisodeItem needle,
                                                               List<EpisodesListResponse.EpisodeItem> haystack) {
        if (needle == null || haystack == null) return null;
        // Try by id first
        for (EpisodesListResponse.EpisodeItem item : haystack) {
            if (item.getId() == needle.getId()) {
                return item;
            }
        }
        // Fallback by number
        if (needle.getNumber() != null) {
            for (EpisodesListResponse.EpisodeItem item : haystack) {
                if (item.getNumber() != null && item.getNumber().equals(needle.getNumber())) {
                    return item;
                }
            }
        }
        return null;
    }

    private void updateEpisodeNavigationButtonsVisibility() {
        if (!isControllerVisible) {
            // Hide all navigation buttons when controller is hidden
            if (prevEpisodeButton != null) {
                prevEpisodeButton.setVisibility(View.GONE);
            }
            if (nextEpisodeButton != null) {
                nextEpisodeButton.setVisibility(View.GONE);
            }
            return;
        }

        // Controller is visible, determine button states based on episode availability
        boolean hasPrevious = false;
        boolean hasNext = false;
        
        if (episodes != null && currentEpisode != null) {
            int currentIndex = -1;
            for (int i = 0; i < episodes.size(); i++) {
                if (episodes.get(i).getId() == currentEpisode.getId()) {
                    currentIndex = i;
                    break;
                }
            }
            
            if (currentIndex >= 0) {
                hasPrevious = currentIndex > 0;
                hasNext = currentIndex < episodes.size() - 1;
            }
        }

        // Update previous episode button
        if (prevEpisodeButton != null) {
            prevEpisodeButton.setVisibility(View.VISIBLE);
            prevEpisodeButton.setEnabled(hasPrevious);
            prevEpisodeButton.setAlpha(hasPrevious ? 1.0f : 0.5f);
        }
        
        // Update next episode button
        if (nextEpisodeButton != null) {
            nextEpisodeButton.setVisibility(View.VISIBLE);
            nextEpisodeButton.setEnabled(hasNext);
            nextEpisodeButton.setAlpha(hasNext ? 1.0f : 0.5f);
        }

        Log.d("EpisodeNav", "Controller visible: " + isControllerVisible + 
              ", episodes count: " + (episodes != null ? episodes.size() : 0) +
              ", current episode: " + (currentEpisode != null ? currentEpisode.getId() : "null") +
              ", hasPrevious: " + hasPrevious + ", hasNext: " + hasNext);
    }


    private void toggleMenu() {
        if (isMenuVisible) {
            hideMenu();
        } else {
            showMenu();
        }
    }

    private void showMenu() {
        if (slidingMenuPanel != null && !isMenuVisible) {
            isMenuVisible = true;
            // Cancel any ongoing animation
            slidingMenuPanel.animate().cancel();
            slidingMenuPanel.animate()
                .translationX(0)
                .setInterpolator(new android.view.animation.OvershootInterpolator(0.8f))
                .setDuration(320)
                .withLayer()
                .start();

            // Show overlay
            if (menuOverlay != null) {
                // ensure overlay margins reflect panel width
                ViewGroup.LayoutParams lp = menuOverlay.getLayoutParams();
                if (lp instanceof FrameLayout.LayoutParams) {
                    FrameLayout.LayoutParams flp = (FrameLayout.LayoutParams) lp;
                    flp.setMargins(0, 0, 0, 0);
                    flp.gravity = Gravity.START;
                    menuOverlay.setLayoutParams(flp);
                }
                menuOverlay.setVisibility(View.VISIBLE);
                menuOverlay.animate().cancel();
                menuOverlay.animate()
                    .alpha(1f)
                    .setInterpolator(new androidx.interpolator.view.animation.FastOutSlowInInterpolator())
                    .setDuration(220)
                    .withLayer()
                    .start();
                if (slidingMenuPanel != null) {
                    slidingMenuPanel.bringToFront();
                }
            }
        }
    }

    private void hideMenu() {
        if (slidingMenuPanel != null && isMenuVisible) {
            // Block closing if no episode selected
            if (currentPlayerData == null) {
                Toast.makeText(this, "Сначала выберите озвучку", Toast.LENGTH_SHORT).show();
                return;
            }
            isMenuVisible = false;
            // Cancel any ongoing animation
            slidingMenuPanel.animate().cancel();
            slidingMenuPanel.animate()
                .translationX(menuWidth)
                .setInterpolator(new androidx.interpolator.view.animation.FastOutSlowInInterpolator())
                .setDuration(240)
                .withLayer()
                .start();

            // Hide overlay
            if (menuOverlay != null) {
                menuOverlay.animate().cancel();
                menuOverlay.animate()
                    .alpha(0f)
                    .setInterpolator(new androidx.interpolator.view.animation.FastOutSlowInInterpolator())
                    .setDuration(160)
                    .withEndAction(() -> {
                        if (!isMenuVisible) menuOverlay.setVisibility(View.GONE);
                    })
                    .withLayer()
                .start();
            }
        }
    }

    private void toggleEpisodesInController() {
        if (episodesHorizontalRecyclerView != null) {
            if (isEpisodesMenuVisible) {
                hideEpisodesInController();
            } else {
                showEpisodesInController();
            }
        }
    }

    private void showEpisodesInController() {
        if (episodesHorizontalRecyclerView != null && !isEpisodesMenuVisible) {
            isEpisodesMenuVisible = true;
            episodesHorizontalRecyclerView.setVisibility(View.VISIBLE);
            
            // Prevent controller from hiding while episodes are visible
            playerView.setControllerShowTimeoutMs(0); // 0 = never hide
            // playerView.setControllerHideOnTouch(false); // Don't hide on touch
            
            // Cancel any ongoing animations
            episodesHorizontalRecyclerView.animate().cancel();
            if (playersControlBar != null) {
                playersControlBar.animate().cancel();
            }
            
            // Episodes list is already in position, just fade in
            episodesHorizontalRecyclerView.animate()
                .alpha(1.0f)
                .setDuration(300)
                .setInterpolator(new android.view.animation.OvershootInterpolator(0.8f))
                .start();
            
            // Slide up animation for players control bar (to make room for episodes)
            if (playersControlBar != null) {
                playersControlBar.animate()
                    .translationY(0f)
                .setDuration(300)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(0.8f))
                .start();
        }
    }
    }

    private void hideEpisodesInController() {
        if (episodesHorizontalRecyclerView != null && isEpisodesMenuVisible) {
            isEpisodesMenuVisible = false;
            
            // Restore normal controller timeout when episodes are hidden
            playerView.setControllerShowTimeoutMs(controllerShowTimeoutMs); // Back to original timeout
            // playerView.setControllerHideOnTouch(true); // Restore hide on touch
            
            // Cancel any ongoing animations
            episodesHorizontalRecyclerView.animate().cancel();
            if (playersControlBar != null) {
                playersControlBar.animate().cancel();
            }
            
            // Get episodes height for players control bar animation
            int episodesHeight = episodesHorizontalRecyclerView.getHeight();
            
            // Fade out animation for episodes list
            episodesHorizontalRecyclerView.animate()
                .alpha(0.0f)
                .setDuration(250)
                .setInterpolator(new androidx.interpolator.view.animation.FastOutSlowInInterpolator())
                .start();
            
            // Slide down animation for players control bar (back to shifted position)
            if (playersControlBar != null) {
                playersControlBar.animate()
                    .translationY(episodesHeight)
                    .setDuration(250)
                    .setInterpolator(new androidx.interpolator.view.animation.FastOutSlowInInterpolator())
                    .start();
            }
        }
    }


    private void navigateToPreviousEpisode() {
        Log.d("EpisodeNav", "navigateToPreviousEpisode called");
        
        // Hide episodes panel if visible
        if (isEpisodesMenuVisible) {
            hideEpisodesInController();
        }
        
        if (episodes == null || episodes.isEmpty()) {
            Log.d("EpisodeNav", "Cannot navigate: episodes=" + (episodes != null ? episodes.size() : 0));
            return;
        }
        
        // If currentEpisode is null, we can't navigate
        if (currentEpisode == null) {
            Log.d("EpisodeNav", "No current episode, cannot navigate to previous");
            return;
        }

        int currentIndex = -1;
        for (int i = 0; i < episodes.size(); i++) {
            if (currentEpisode.getNumber() != null && episodes.get(i).getNumber() != null &&
                currentEpisode.getNumber().equals(episodes.get(i).getNumber())) {
                currentIndex = i;
                break;
            }
        }

        Log.d("EpisodeNav", "Current index: " + currentIndex + ", episodes count: " + episodes.size() + 
              ", current episode number: " + (currentEpisode != null ? currentEpisode.getNumber() : "null"));

        if (currentIndex > 0) {
            EpisodesListResponse.EpisodeItem prevEpisode = episodes.get(currentIndex - 1);
            Log.d("EpisodeNav", "Switching to previous episode: " + prevEpisode.getId());
            
            // Use the same logic as onEpisodeSelected but don't hide menu
            stopCurrentPlayback();
            currentEpisode = prevEpisode;
            resetCommentsOnEpisodeChange(true);
            
            // Save current episode through API
            if (currentAnimeId != null && prevEpisode.getNumber() != null) {
                apiService.saveCurrentEpisode(currentAnimeId, prevEpisode.getNumber(), new AnimeApiService.CurrentEpisodeCallback() {
                    @Override
                    public void onCurrentEpisodeReceived(EpisodesListResponse.EpisodeItem episode) {
                        Log.d("EpisodeMemory", "Successfully saved previous episode via API");
                    }
                    
                    @Override
                    public void onError(String error) {
                        Log.e("EpisodeMemory", "Failed to save previous episode via API: " + error);
                    }
                });
            }
            
            updateEpisodeNavigationButtonsVisibility();
            updateEpisodesRecyclerView(); // Update highlighting
            updateAnimeInfoHeader();
            runOnUiThread(() -> showLoading("Загрузка предыдущего эпизода..."));
            loadPlayersForEpisode(prevEpisode.getId());
        } else {
            Log.d("EpisodeNav", "No previous episode available, currentIndex=" + currentIndex + ", episodes.size()=" + episodes.size());
        }
    }

    private void navigateToNextEpisode() {
        Log.d("EpisodeNav", "navigateToNextEpisode called");
        
        // Hide episodes panel if visible
        if (isEpisodesMenuVisible) {
            hideEpisodesInController();
        }
        
        if (episodes == null || episodes.isEmpty()) {
            Log.d("EpisodeNav", "Cannot navigate: episodes=" + (episodes != null ? episodes.size() : 0));
            return;
        }
        
        // If currentEpisode is null, we can't navigate
        if (currentEpisode == null) {
            Log.d("EpisodeNav", "No current episode, cannot navigate to next");
            return;
        }

        int currentIndex = -1;
        for (int i = 0; i < episodes.size(); i++) {
            if (currentEpisode.getNumber() != null && episodes.get(i).getNumber() != null &&
                currentEpisode.getNumber().equals(episodes.get(i).getNumber())) {
                currentIndex = i;
                break;
            }
        }

        Log.d("EpisodeNav", "Current index: " + currentIndex + ", episodes count: " + episodes.size() + 
              ", current episode number: " + (currentEpisode != null ? currentEpisode.getNumber() : "null"));

        if (currentIndex >= 0 && currentIndex < episodes.size() - 1) {
            EpisodesListResponse.EpisodeItem nextEpisode = episodes.get(currentIndex + 1);
            Log.d("EpisodeNav", "Switching to next episode: " + nextEpisode.getId());
            
            // Use the same logic as onEpisodeSelected but don't hide menu
            stopCurrentPlayback();
            currentEpisode = nextEpisode;
            resetCommentsOnEpisodeChange(true);
            
            // Save current episode through API
            if (currentAnimeId != null && nextEpisode.getNumber() != null) {
                apiService.saveCurrentEpisode(currentAnimeId, nextEpisode.getNumber(), new AnimeApiService.CurrentEpisodeCallback() {
                    @Override
                    public void onCurrentEpisodeReceived(EpisodesListResponse.EpisodeItem episode) {
                        Log.d("EpisodeMemory", "Successfully saved next episode via API");
                    }
                    
                    @Override
                    public void onError(String error) {
                        Log.e("EpisodeMemory", "Failed to save next episode via API: " + error);
                    }
                });
            }
            
            updateEpisodeNavigationButtonsVisibility();
            updateEpisodesRecyclerView(); // Update highlighting
            updateAnimeInfoHeader();
            runOnUiThread(() -> showLoading("Загрузка следующего эпизода..."));
            loadPlayersForEpisode(nextEpisode.getId());
        } else {
            Log.d("EpisodeNav", "No next episode available, currentIndex=" + currentIndex + ", episodes.size()=" + episodes.size());
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

        // Update current player data
        currentPlayerData = playerData;

        // Update preferred quality for new player
        List<String> newQualities = getAvailableQualities();
        if (!newQualities.isEmpty()) {
            // Set preferred quality to the highest available for new player
            String newPreferredQuality = null; // First is usually highest
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                if ("animelib".equalsIgnoreCase(playerData.getPlayer())) {
                    newPreferredQuality = newQualities.getFirst();
                } else {
                    newPreferredQuality = newQualities.getLast();
                }
            }
            preferredQuality = newPreferredQuality;
            Log.d("VideoPlayer", "Updated preferred quality to: " + newPreferredQuality + " for player: " + playerData.getPlayer());
        }

        // Update settings dialog if it's open
        if (currentSettingsDialog != null) {
            if (!newQualities.isEmpty()) {
                currentSettingsDialog.updateQualities(newQualities, preferredQuality);
            }
        }

        // Hide menu
        hideMenu();



        // Save user preference
        if (playerData.getPlayer() != null) {
            preferredPlayerType = playerData.getPlayer().toLowerCase();
            if ("animelib".equalsIgnoreCase(playerData.getPlayer())) {
                preferredAnimelibPlayer = playerData;
            } else if ("kodik".equalsIgnoreCase(playerData.getPlayer())) {
                preferredKodikPlayer = playerData;
            }
        }

        // Route to appropriate player handler (start from beginning for new player)
        if (playerData.getPlayer() != null && "animelib".equalsIgnoreCase(playerData.getPlayer())) {
            handleAnimelibPlayer(playerData, 0);
        } else if (playerData.getPlayer() != null && "kodik".equalsIgnoreCase(playerData.getPlayer())) {
            handleKodikPlayer(playerData, 0);
        }

        updateAnimeInfoHeader();

        // Update menu to reflect current selection after animation completes
        slidingMenuPanel.postDelayed(() -> updateMenuWithData(), 350);
    }

    private void onEpisodeSelected(EpisodesListResponse.EpisodeItem episode) {
        Log.d("VideoPlayer", "Episode selected: " + episode.getName());

        // Stop current playback if playing
        stopCurrentPlayback();

        // Hide episodes in controller
        hideEpisodesInController();

        // Update current episode
        currentEpisode = episode;
        resetCommentsOnEpisodeChange(true);
        
        // Save current episode through API
        if (currentAnimeId != null && episode.getNumber() != null) {
            apiService.saveCurrentEpisode(currentAnimeId, episode.getNumber(), new AnimeApiService.CurrentEpisodeCallback() {
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
        updateEpisodeNavigationButtonsVisibility();
        
        // Update episodes RecyclerView to highlight current episode
        updateEpisodesRecyclerView();
        updateAnimeInfoHeader();

        // Show loading and load players for this episode
        runOnUiThread(() -> {
            showLoading("Загрузка плееров для эпизода...");
            if (menuLoadingOverlay != null) menuLoadingOverlay.setVisibility(View.VISIBLE);
        });

        // Load players for the selected episode
        loadPlayersForEpisode(episode.getId());
    }

    private void loadPlayersForEpisode(int episodeId) {
        apiService.fetchEpisodeData(episodeId, new AnimeApiService.EpisodeDataCallback() {
                    @Override
            public void onEpisodeDataReceived(EpisodeResponse response) {
                        runOnUiThread(() -> {
                            hideLoading();
                            if (menuLoadingOverlay != null) menuLoadingOverlay.setVisibility(View.GONE);
                            if (response.getData() != null && response.getData().getPlayers() != null) {
                                showPlayerSelectionDialog(response.getData().getPlayers());
                                } else {
                                        Toast.makeText(VideoPlayerActivity.this, "Плееры не найдены", Toast.LENGTH_SHORT).show();
                                }
                                });
                            }

            @Override
            public void onError(String error) {
                            runOnUiThread(() -> {
                                hideLoading();
                            if (menuLoadingOverlay != null) menuLoadingOverlay.setVisibility(View.GONE);
                            Toast.makeText(VideoPlayerActivity.this, error, Toast.LENGTH_SHORT).show();
                        });
            }
        });
    }

    private void stopCurrentPlayback() {
        if (player != null) {
            Log.d("VideoPlayer", "Stopping current playback");
            player.stop();
            player.clearMediaItems();
        }
    }

    private void showSettingsDialog() {
        if (currentPlayerData == null) {
            Toast.makeText(this, "Сначала выберите плеер", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> availableQualities = getAvailableQualities();
        if (availableQualities.isEmpty()) {
            Toast.makeText(this, "Качества недоступны", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create dialog reference first
        SettingsDialog dialog = new SettingsDialog(this, availableQualities, preferredQuality,
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
                List<String> newQualities = getAvailableQualities();
                if (!newQualities.isEmpty()) {
                    // Update preferred quality if current is not available
                    if (!newQualities.contains(preferredQuality)) {
                        preferredQuality = newQualities.get(0);
                    }
                    // Use currentSettingsDialog instead of dialog
                    if (currentSettingsDialog != null) {
                        currentSettingsDialog.updateQualities(newQualities, preferredQuality);
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
        currentSettingsDialog = dialog;
        
        // Apply settings when dialog is shown
        dialog.setOnShowListener(dialogInterface -> {
            // Apply current playback speed and volume
            applySettingsFromDialog(dialog);
        });
        
        dialog.show();
    }
    

    private List<String> getAvailableQualities() {
        List<String> qualities = new ArrayList<>();

        if (currentPlayerData == null) {
            return qualities;
        }

        String playerType = currentPlayerData.getPlayer();
        if (playerType == null) {
            return qualities;
        }

        if ("animelib".equalsIgnoreCase(playerType)) {
            // For AnimeLib, qualities are in video.quality array
            if (currentPlayerData.getVideo() != null && currentPlayerData.getVideo().getQuality() != null) {
                for (EpisodeResponse.QualityData qualityData : currentPlayerData.getVideo().getQuality()) {
                    String quality = String.valueOf(qualityData.getQuality());
                    // Include 4K quality only if enabled
                    if (enable4K || !"2160".equals(quality)) {
                        qualities.add(quality + "p");
                    }
                }
            }
        } else if ("kodik".equalsIgnoreCase(playerType)) {
            // For Kodik, get qualities from the API response
            if (currentKodikResponse != null && currentKodikResponse.getData() != null) {
                for (String qualityKey : currentKodikResponse.getData().keySet()) {
                    // Include 4K quality only if enabled
                    if (enable4K || !"2160".equals(qualityKey)) {
                        qualities.add(qualityKey + "p");
                    }
                }
                // Sort qualities by resolution (highest first)
                qualities.sort((q1, q2) -> {
                    try {
                        int res1 = Integer.parseInt(q1.replace("p", ""));
                        int res2 = Integer.parseInt(q2.replace("p", ""));
                        return Integer.compare(res2, res1); // descending order
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                });
            } else {
                // Fallback if no Kodik response available
                qualities.add("360p");
                qualities.add("480p");
                qualities.add("720p");
            }
        }

        return qualities;
    }

    private void restartPlayerWithNewQuality() {
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



    private void applySettingsFromDialog(SettingsDialog dialog) {
        if (player != null) {
            // Apply playback speed
            float speed = dialog.getCurrentPlaybackSpeed();
            player.setPlaybackSpeed(speed);
            Log.d("VideoPlayer", "Applied playback speed: " + speed);
        }
    }

    private void updateMenuWithData() {
        if (playerTabsAdapter != null) {
            playerTabsAdapter.updateData(
                animelibPlayers != null ? animelibPlayers : new ArrayList<>(),
                kodikPlayers != null ? kodikPlayers : new ArrayList<>(),
                currentPlayerData
            );
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
        playerView.setControllerShowTimeoutMs(controllerShowTimeoutMs);
        playerView.setControllerAutoShow(true);
        playerView.setControllerHideOnTouch(true);

        Log.d("PlayerInit", "ExoPlayer bound to PlayerView with controller enabled");

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
        apiService.loadAnimeFromUrl(url, new AnimeApiService.EpisodeDataCallback() {
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

        // Store all players data
        allPlayers = players;
        currentAnimeId = apiService.extractAnimeId(animeUrl);

        // Load episodes only if not loaded yet; otherwise init menu with currentEpisode
        if (episodes == null && currentAnimeId != null) {
            loadEpisodes(currentAnimeId);
        } else {
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

        apiService.fetchKodikVideoLinksUnsafe(kodikSrc, new AnimeApiService.KodikVideoCallback() {
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

        apiService.fetchEpisodesList(animeId, new AnimeApiService.EpisodesCallback() {
            @Override
            public void onEpisodesReceived(EpisodesListResponse response) {
                if (response.getData() != null) {
                    episodes = response.getData();
                    Log.d("EpisodesAPI", "Loaded " + episodes.size() + " episodes");

                    // NEW PRIORITY:
                    // 1) Load from DB via API service
                    apiService.loadCurrentEpisode(animeId, new AnimeApiService.CurrentEpisodeCallback() {
                    @Override
                        public void onCurrentEpisodeReceived(EpisodesListResponse.EpisodeItem episode) {
                            // Normalize to list instance if possible
                            EpisodesListResponse.EpisodeItem normalized = findEpisodeInList(episode, episodes);
                            currentEpisode = normalized != null ? normalized : episode;
                            Log.d("EpisodesAPI", "Using saved episode from DB: " + currentEpisode.getNumber());
                            
                            // Update UI immediately
                            resetCommentsOnEpisodeChange(false);
                            updateEpisodeNavigationButtonsVisibility();
                            updateEpisodesRecyclerView();
                            updateAnimeInfoHeader();
                            
                            // Fetch players for the saved episode to ensure playback uses it
                            runOnUiThread(() -> loadPlayersForEpisode(currentEpisode.getId()));
                    }

                    @Override
                        public void onError(String error) {
                            Log.w("EpisodesAPI", "No saved episode: " + error + ", trying URL match");
                            // 2) Try from URL
                            EpisodesListResponse.EpisodeItem fromUrl = findCurrentEpisodeFromUrl(animeUrl, episodes);
                            if (fromUrl != null) {
                                currentEpisode = fromUrl;
                                EpisodesListResponse.EpisodeItem normalized = findEpisodeInList(fromUrl, episodes);
                                currentEpisode = normalized != null ? normalized : fromUrl;
                                
                                // Update UI immediately with found episode
                                resetCommentsOnEpisodeChange(false);
                                updateEpisodeNavigationButtonsVisibility();
                                updateEpisodesRecyclerView();
                                updateAnimeInfoHeader();
                                
                                // Save found episode to DB
                                if (currentAnimeId != null && currentEpisode.getNumber() != null) {
                                    apiService.saveCurrentEpisode(currentAnimeId, currentEpisode.getNumber(), new AnimeApiService.CurrentEpisodeCallback() {
                                        @Override
                                        public void onCurrentEpisodeReceived(EpisodesListResponse.EpisodeItem episode) {
                                            Log.d("EpisodeMemory", "Successfully saved episode from URL to DB: " + episode.getNumber());
                                        }
                                        
                                        @Override
                                        public void onError(String error) {
                                            Log.e("EpisodeMemory", "Failed to save episode from URL to DB: " + error);
            }
        });
    }

                                runOnUiThread(() -> loadPlayersForEpisode(currentEpisode.getId()));
                                } else {
                                // 3) If no episode found by URL, take first episode from API and save it
                                if (!episodes.isEmpty()) {
                                    currentEpisode = episodes.get(0);
                                    Log.d("EpisodesAPI", "No episode found by URL, using first episode: " + currentEpisode.getNumber());
                                    
                                    // Update UI immediately
                                    resetCommentsOnEpisodeChange(false);
                                    updateEpisodeNavigationButtonsVisibility();
                                    updateEpisodesRecyclerView();
                                    updateAnimeInfoHeader();
                                    
                                    // Save first episode to DB
                                    if (currentAnimeId != null && currentEpisode.getNumber() != null) {
                                        apiService.saveCurrentEpisode(currentAnimeId, currentEpisode.getNumber(), new AnimeApiService.CurrentEpisodeCallback() {
                                            @Override
                                            public void onCurrentEpisodeReceived(EpisodesListResponse.EpisodeItem episode) {
                                                Log.d("EpisodeMemory", "Successfully saved first episode to DB: " + episode.getNumber());
                                            }
                                            
                    @Override
                                            public void onError(String error) {
                                                Log.e("EpisodeMemory", "Failed to save first episode to DB: " + error);
                                            }
                                        });
                                    }
                                    
                                    runOnUiThread(() -> loadPlayersForEpisode(currentEpisode.getId()));
                                } else {
                                    runOnUiThread(VideoPlayerActivity.this::initializeMenuWithoutAutoPlay);
                                }
                                }
                        }
                    });
                    return; // callback path continues
                    
//                    Log.d("EpisodesAPI", "Current episode set to: " + (currentEpisode != null ? currentEpisode.getNumber() + " (ID: " + currentEpisode.getId() + ")" : "null"));
                            } else {
                            episodes = new ArrayList<>();
                        }

                        runOnUiThread(() -> {
                            initializeMenuWithoutAutoPlay();
                        });
                    }

            @Override
            public void onError(String error) {
                Log.e("EpisodesAPI", "Failed to load episodes: " + error);
                episodes = new ArrayList<>();
                runOnUiThread(() -> {
                    // Continue with menu even if episodes failed to load
                    initializeMenuWithoutAutoPlay();
                });
            }
        });
    }

    private EpisodesListResponse.EpisodeItem findCurrentEpisodeFromUrl(String url, List<EpisodesListResponse.EpisodeItem> episodes) {
        if (url == null || episodes == null) {
            Log.d("EpisodeFinder", "URL or episodes is null");
            return null;
        }

        Log.d("EpisodeFinder", "Looking for current episode in URL: " + url);

        // Try to extract episode number from URL
        // URL format: .../episode-123 or .../watch?episode=123
        String episodePattern = "episode[-=](\\d+)";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(episodePattern);
        java.util.regex.Matcher matcher = pattern.matcher(url);

        if (matcher.find()) {
            String episodeNumber = matcher.group(1);
            Log.d("EpisodeFinder", "Found episode number in URL: " + episodeNumber);
            try {
                // First try to find by episode ID
                int epNum = Integer.parseInt(episodeNumber);
                for (EpisodesListResponse.EpisodeItem episode : episodes) {
                    Log.d("EpisodeFinder", "Checking episode ID: " + episode.getId() + " vs " + epNum);
                    if (episode.getId() == epNum) {
                        Log.d("EpisodeFinder", "Found matching episode by ID: " + episode.getNumber());
                        return episode;
                    }
                }
                
                // If not found by ID, try to find by episode number
                String epNumStr = String.valueOf(epNum);
                for (EpisodesListResponse.EpisodeItem episode : episodes) {
                    Log.d("EpisodeFinder", "Checking episode number: " + episode.getNumber() + " vs " + epNumStr);
                    if (episode.getNumber() != null && episode.getNumber().equals(epNumStr)) {
                        Log.d("EpisodeFinder", "Found matching episode by number: " + episode.getNumber());
                        return episode;
                    }
                }
            } catch (NumberFormatException e) {
                Log.w("EpisodeFinder", "Could not parse episode number: " + episodeNumber);
            }
        } else {
            Log.d("EpisodeFinder", "No episode pattern found in URL");
        }

        // If no episode found in URL, return null instead of first episode
        // This allows the user to manually select the episode they want
        Log.d("EpisodeFinder", "No episode found in URL, returning null to allow manual selection");
        return null;
    }
    
    // Old SharedPreferences methods removed - now using AnimeApiService

    private void initializeMenuWithoutAutoPlay() {
        // Separate players by type (case-insensitive)
        animelibPlayers = allPlayers.stream()
                .filter(p -> p.getPlayer() != null && "animelib".equalsIgnoreCase(p.getPlayer()))
                .collect(Collectors.toList());

        kodikPlayers = allPlayers.stream()
                .filter(p -> p.getPlayer() != null && "kodik".equalsIgnoreCase(p.getPlayer()))
                .collect(Collectors.toList());

        // Update menu with players and episodes
        updateMenuWithData();
        updateEpisodeNavigationButtonsVisibility();
        updateEpisodesRecyclerView(); // Call this last to ensure currentEpisode is set

        // Try to auto-select preferred player if available
        EpisodeResponse.PlayerData preferredPlayer = findPreferredPlayer();
        if (preferredPlayer != null) {
            Log.d("VideoPlayer", "Auto-selecting preferred player: " + preferredPlayer.getPlayer());
            onPlayerSelected(preferredPlayer);
            return;
        }

        // Show menu for user to select episode/player manually
        showMenu();
        // Ensure loading overlay is hidden once players are available
        if (menuLoadingOverlay != null) menuLoadingOverlay.setVisibility(View.GONE);
        if (menuLoadingIndicator != null) menuLoadingIndicator.setVisibility(View.GONE);
    }

    private void updateAnimeInfoHeader() {
        if (animeTitleView == null || currentEpisodeNumberView == null) return;
        String slugOrId = apiService.extractAnimeSlug(animeUrl);
        if (slugOrId == null) return;

        apiService.fetchAnimeInfo(slugOrId, new AnimeApiService.AnimeInfoCallback() {
            @Override
            public void onAnimeInfoReceived(AnimeInfoResponse response) {
                runOnUiThread(() -> {
                    if (response != null && response.getData() != null) {
                        String rus = response.getData().getRus_name();
                        animeTitleView.setText(rus != null ? rus : "");
                    }

                    String tm = (currentPlayerData != null && currentPlayerData.getTeam() != null)
                            ? currentPlayerData.getTeam().getName() : null;
                    String ep = (currentEpisode != null) ? currentEpisode.getNumber() : null;
                    String em = (currentEpisode != null && currentEpisode.getName() != null && !Objects.equals(currentEpisode.getName(), ""))
                            ? currentEpisode.getName() : null;

                    if (currentTeamName != null) currentTeamName.setText(tm != null ? tm : "");
                    if (currentEpisodeNumberView != null) currentEpisodeNumberView.setText(ep != null ? (ep + " серия") : "");
                    if (currentEpisodeName != null) currentEpisodeName.setText(em != null ? (", " + em) : "");
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    String ep = currentEpisode != null ? currentEpisode.getNumber() : null;
                    currentEpisodeNumberView.setText(ep != null ? (ep + " серия") : "");
                });
            }
        });
    }

    private EpisodeResponse.PlayerData findPreferredPlayer() {
        if (preferredPlayerType == null) {
            return null;
        }

        // Try to find exact match for preferred player
        if ("animelib".equals(preferredPlayerType) && preferredAnimelibPlayer != null) {
            for (EpisodeResponse.PlayerData player : animelibPlayers) {
                if (isSamePlayer(preferredAnimelibPlayer, player)) {
                    return player;
                }
            }
            // If exact match not found, return first available AnimeLib player
            return animelibPlayers.isEmpty() ? null : animelibPlayers.get(0);
        } else if ("kodik".equals(preferredPlayerType) && preferredKodikPlayer != null) {
            for (EpisodeResponse.PlayerData player : kodikPlayers) {
                if (isSamePlayer(preferredKodikPlayer, player)) {
                    return player;
                }
            }
            // If exact match not found, return first available Kodik player
            return kodikPlayers.isEmpty() ? null : kodikPlayers.get(0);
        }

        return null;
    }

    private boolean isSamePlayer(EpisodeResponse.PlayerData player1, EpisodeResponse.PlayerData player2) {
        if (player1 == null || player2 == null) return false;

        // Compare by translation and team (most specific match)
        boolean sameTranslation = (player1.getTranslationType() == null && player2.getTranslationType() == null) ||
                (player1.getTranslationType() != null && player2.getTranslationType() != null &&
                 player1.getTranslationType().getLabel() != null &&
                 player1.getTranslationType().getLabel().equals(player2.getTranslationType().getLabel()));

        boolean sameTeam = (player1.getTeam() == null && player2.getTeam() == null) ||
                (player1.getTeam() != null && player2.getTeam() != null &&
                 player1.getTeam().getName() != null &&
                 player1.getTeam().getName().equals(player2.getTeam().getName()));

        return sameTranslation && sameTeam;
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
        playerView.setControllerShowTimeoutMs(controllerShowTimeoutMs);
        playerView.setControllerAutoShow(true);
        playerView.setControllerHideOnTouch(true);

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
                    long skipDuration = longSkipDuration * 1000; // Convert seconds to milliseconds
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
                    updateEpisodeNavigationButtonsVisibility();
                    updatePlayLoadingIndicator(playbackState);
                    
                    // Handle auto-play next episode
                    if (playbackState == Player.STATE_ENDED && autoPlay) {
                        Log.d("PlayerControls", "Video ended, checking for next episode");
                        navigateToNextEpisode();
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
                    playerView.postDelayed(() -> updatePlayPauseButtonsVisibility(), 100);
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
                    playerView.postDelayed(() -> updatePlayPauseButtonsVisibility(), 100);
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

    private void toggleCommentsPanel() {
        if (isCommentsVisible) hideCommentsPanel(); else showCommentsPanel();
    }

    private void showCommentsPanel() {
        if (commentsPanel == null || isCommentsVisible) return;
        isCommentsVisible = true;
        commentsPanel.animate().cancel();
        commentsPanel.animate()
            .translationX(0)
            .setInterpolator(new androidx.interpolator.view.animation.FastOutSlowInInterpolator())
            .setDuration(260)
            .withLayer()
            .start();
        if (menuOverlay != null) {
            menuOverlay.setVisibility(View.VISIBLE);
            menuOverlay.animate().alpha(1f).setDuration(200).start();
            commentsPanel.bringToFront();
        }
        // init list
        if (commentsRecyclerView != null) {
            if (commentsAdapter == null) {
                commentsAdapter = new com.example.animelib.adapters.CommentsAdapter();
                commentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
                commentsRecyclerView.setAdapter(commentsAdapter);
                commentsRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                        if (dy <= 0) return;
                        RecyclerView.LayoutManager lm = rv.getLayoutManager();
                        if (!(lm instanceof LinearLayoutManager)) return;
                        LinearLayoutManager llm = (LinearLayoutManager) lm;
                        int total = llm.getItemCount();
                        int last = llm.findLastVisibleItemPosition();
                        if (!isLoadingComments && commentsHasNextPage && last >= total - 3) {
                            loadCommentsPage(commentsCurrentPage + 1);
                        }
                    }
                });
            }
        }
        // load first page if empty
        if (!isLoadingComments && (commentsAdapter == null || commentsAdapter.getItemCount() == 0)) {
            commentsCurrentPage = 1;
            commentsHasNextPage = true;
            loadCommentsPage(1);
        }
    }

    private void hideCommentsPanel() {
        if (commentsPanel == null || !isCommentsVisible) return;
        isCommentsVisible = false;
        commentsPanel.animate().cancel();
        commentsPanel.animate()
            .translationX(commentsPanelWidth)
            .setInterpolator(new androidx.interpolator.view.animation.FastOutSlowInInterpolator())
            .setDuration(220)
            .withEndAction(() -> {
                if (!isMenuVisible && menuOverlay != null) menuOverlay.setVisibility(View.GONE);
            })
            .withLayer()
            .start();
        if (!isMenuVisible && menuOverlay != null) {
            menuOverlay.animate().alpha(0f).setDuration(160).start();
        }
    }

    private void loadCommentsPage(int page) {
        if (currentEpisode == null) return;
        isLoadingComments = true;
        if (commentsLoadingOverlay != null) commentsLoadingOverlay.setVisibility(View.VISIBLE);
        long episodeId = currentEpisode.getId();
        apiService.fetchEpisodeComments(episodeId, commentsSortType, page, new AnimeApiService.EpisodeCommentsCallback() {
            @Override
            public void onCommentsReceived(com.example.animelib.models.CommentsResponse response) {
                runOnUiThread(() -> {
                    if (commentsLoadingOverlay != null) commentsLoadingOverlay.setVisibility(View.GONE);
                    isLoadingComments = false;
                    if (response != null && commentsAdapter != null) {
                        commentsAdapter.appendResponse(response, page > 1);
                    }
                    if (response != null && response.getMeta() != null) {
                        commentsHasNextPage = response.getMeta().isHas_next_page();
                        if (commentsHasNextPage) commentsCurrentPage = page;
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    isLoadingComments = false;
                    if (commentsLoadingOverlay != null) commentsLoadingOverlay.setVisibility(View.GONE);
                    Toast.makeText(VideoPlayerActivity.this, error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void resetCommentsOnEpisodeChange(boolean reloadIfVisible) {
        commentsCurrentPage = 1;
        commentsHasNextPage = true;
        isLoadingComments = false;
        if (commentsAdapter != null) {
            commentsAdapter.clearAll();
        }
        if (reloadIfVisible && isCommentsVisible) {
            loadCommentsPage(1);
        }
    }

    private void changeCommentsSort(String sort) {
        if (sort == null) return;
        if (!sort.equals(commentsSortType)) {
            commentsSortType = sort;
            resetCommentsOnEpisodeChange(isCommentsVisible);
            if (isCommentsVisible) loadCommentsPage(1);
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
    }

    @Override
    public void onBackPressed() {
        // If video is playing and not in PiP mode, enter PiP instead of closing
        if (player != null && player.isPlaying() && !isInPictureInPictureMode) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                enterPictureInPictureMode();
                return;
            }
        }
        
        // Otherwise, close the activity
        super.onBackPressed();
    }
}


