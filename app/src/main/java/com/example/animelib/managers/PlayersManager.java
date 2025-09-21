package com.example.animelib.managers;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.viewpager2.widget.ViewPager2;

import com.example.animelib.adapters.PlayerTabsAdapter;
import com.example.animelib.api.ApiService;
import com.example.animelib.models.EpisodeResponse;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Менеджер для управления плеерами и озвучками
 */
public class PlayersManager {
    private static final String TAG = "PlayersManager";
    
    // Контекст и зависимости
    private final Context context;
    private final ApiService apiService;
    
    // UI компоненты
    private View slidingMenuPanel;
    private ImageButton closeMenuButton;
    private TabLayout playerTabLayout;
    private ViewPager2 playersViewPager;
    private View menuOverlay;
    private View menuLoadingOverlay;
    private View menuLoadingIndicator;
    
    // Состояние меню
    private boolean isMenuVisible = false;
    private float menuWidth;
    
    // Данные плееров
    private List<EpisodeResponse.PlayerData> allPlayers = new ArrayList<>();
    private List<EpisodeResponse.PlayerData> animelibPlayers = new ArrayList<>();
    private List<EpisodeResponse.PlayerData> kodikPlayers = new ArrayList<>();
    private EpisodeResponse.PlayerData currentPlayerData;
    
    // Предпочтения пользователя
    private String preferredPlayerType; // "animelib" or "kodik"
    private EpisodeResponse.PlayerData preferredAnimelibPlayer;
    private EpisodeResponse.PlayerData preferredKodikPlayer;
    
    // Адаптер для табов
    private PlayerTabsAdapter playerTabsAdapter;
    
    // Callback интерфейсы
    public interface PlayerSelectionCallback {
        void onPlayerSelected(EpisodeResponse.PlayerData playerData);
    }
    
    public interface PlayersVisibilityCallback {
        void onPlayersVisibilityChanged(boolean isVisible);
    }
    
    public interface PlayersDataCallback {
        void onPlayersLoaded(List<EpisodeResponse.PlayerData> players);
        void onPlayersError(String error);
    }
    
    private PlayerSelectionCallback playerSelectionCallback;
    private PlayersVisibilityCallback visibilityCallback;
    private PlayersDataCallback dataCallback;
    
    public PlayersManager(Context context, ApiService apiService) {
        this.context = context;
        this.apiService = apiService;
    }
    
    /**
     * Инициализация UI компонентов
     */
    public void initializeViews(View slidingMenuPanel, ImageButton closeMenuButton,
                               TabLayout playerTabLayout, ViewPager2 playersViewPager,
                               View menuOverlay, View menuLoadingOverlay, View menuLoadingIndicator) {
        this.slidingMenuPanel = slidingMenuPanel;
        this.closeMenuButton = closeMenuButton;
        this.playerTabLayout = playerTabLayout;
        this.playersViewPager = playersViewPager;
        this.menuOverlay = menuOverlay;
        this.menuLoadingOverlay = menuLoadingOverlay;
        this.menuLoadingIndicator = menuLoadingIndicator;
        
        setupPlayersViews();
        initializePanelPosition();
    }
    
    /**
     * Инициализация начальной позиции панели
     */
    private void initializePanelPosition() {
        if (slidingMenuPanel != null) {
            // Set initial position off-screen to the right
            slidingMenuPanel.post(() -> {
                float panelWidth = slidingMenuPanel.getWidth();
                if (panelWidth > 0) {
                    slidingMenuPanel.setTranslationX(panelWidth);
                    Log.d(TAG, "Initialized panel position with width: " + panelWidth);
                } else {
                    // Fallback to set width
                    slidingMenuPanel.setTranslationX(menuWidth);
                    Log.d(TAG, "Initialized panel position with fallback width: " + menuWidth);
                }
            });
        }
    }
    
    /**
     * Настройка UI компонентов плееров
     */
    private void setupPlayersViews() {
        if (closeMenuButton != null) {
            closeMenuButton.setOnClickListener(v -> {
                if (currentPlayerData != null) {
                    hideMenu();
                } else {
                    Toast.makeText(context, "Сначала выберите озвучку", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        setupPlayersViewPager();
    }
    
    /**
     * Настройка ViewPager с табами плееров
     */
    private void setupPlayersViewPager() {
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
    }
    
    /**
     * Обработка выбора плеера
     */
    private void onPlayerSelected(EpisodeResponse.PlayerData playerData) {
        Log.d(TAG, "Player selected: " + playerData.getPlayer());
        
        // Update current player data
        currentPlayerData = playerData;
        
        // Save user preference
        if (playerData.getPlayer() != null) {
            preferredPlayerType = playerData.getPlayer().toLowerCase();
            if ("animelib".equalsIgnoreCase(playerData.getPlayer())) {
                preferredAnimelibPlayer = playerData;
            } else if ("kodik".equalsIgnoreCase(playerData.getPlayer())) {
                preferredKodikPlayer = playerData;
            }
        }
        
        // Hide menu
        hideMenu();
        
        // Notify callback
        if (playerSelectionCallback != null) {
            playerSelectionCallback.onPlayerSelected(playerData);
        }
    }
    
    /**
     * Показать меню плееров
     */
    public void showMenu() {
        if (slidingMenuPanel != null && !isMenuVisible) {
            Log.d(TAG, "Showing players menu");
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
            
            if (visibilityCallback != null) {
                visibilityCallback.onPlayersVisibilityChanged(true);
            }
        }
    }
    
    /**
     * Скрыть меню плееров
     */
    public void hideMenu() {
        if (slidingMenuPanel != null && isMenuVisible) {
            // Block closing if no episode selected
            if (currentPlayerData == null) {
                Toast.makeText(context, "Сначала выберите озвучку", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Log.d(TAG, "Hiding players menu");
            isMenuVisible = false;
            
            // Cancel any ongoing animation
            slidingMenuPanel.animate().cancel();
            
            // Get actual panel width for proper hiding
            float panelWidth = slidingMenuPanel.getWidth();
            if (panelWidth == 0) {
                panelWidth = menuWidth; // fallback to set width
            }
            
            Log.d(TAG, "Hiding panel with width: " + panelWidth);
            
            slidingMenuPanel.animate()
                    .translationX(panelWidth)
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
            
            if (visibilityCallback != null) {
                visibilityCallback.onPlayersVisibilityChanged(false);
            }
        }
    }
    
    /**
     * Переключение видимости меню
     */
    public void toggleMenu() {
        if (isMenuVisible) {
            hideMenu();
        } else {
            showMenu();
        }
    }
    
    /**
     * Загрузка плееров для эпизода
     */
    public void loadPlayersForEpisode(int episodeId) {
        Log.d(TAG, "Loading players for episode ID: " + episodeId);
        
        // Show loading
        showLoading();
        
        apiService.fetchEpisodeData(episodeId, new ApiService.EpisodeDataCallback() {
            @Override
            public void onEpisodeDataReceived(EpisodeResponse response) {
                ((android.app.Activity) context).runOnUiThread(() -> {
                    hideLoading();
                    
                    if (response.getData() != null && response.getData().getPlayers() != null) {
                        setPlayersData(response.getData().getPlayers());
                        
                        if (dataCallback != null) {
                            dataCallback.onPlayersLoaded(allPlayers);
                        }
                    } else {
                        Log.e(TAG, "No players found in response");
                        if (dataCallback != null) {
                            dataCallback.onPlayersError("Плееры не найдены");
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {
                ((android.app.Activity) context).runOnUiThread(() -> {
                    hideLoading();
                    Log.e(TAG, "Error loading players: " + error);
                    
                    if (dataCallback != null) {
                        dataCallback.onPlayersError(error);
                    }
                });
            }
        });
    }
    
    /**
     * Установка данных плееров
     */
    public void setPlayersData(List<EpisodeResponse.PlayerData> players) {
        Log.d(TAG, "Setting players data: " + players.size() + " players");
        
        // Store all players data
        allPlayers.clear();
        allPlayers.addAll(players);
        
        // Separate players by type (case-insensitive)
        animelibPlayers = allPlayers.stream()
                .filter(p -> p.getPlayer() != null && "animelib".equalsIgnoreCase(p.getPlayer()))
                .collect(Collectors.toList());

        kodikPlayers = allPlayers.stream()
                .filter(p -> p.getPlayer() != null && "kodik".equalsIgnoreCase(p.getPlayer()))
                .collect(Collectors.toList());
        
        Log.d(TAG, "AnimeLib players: " + animelibPlayers.size() + ", Kodik players: " + kodikPlayers.size());
        
        // Update menu with players
        updateMenuWithData();
        
        // Try to auto-select preferred player if available
        EpisodeResponse.PlayerData preferredPlayer = findPreferredPlayer();
        if (preferredPlayer != null) {
            Log.d(TAG, "Auto-selecting preferred player: " + preferredPlayer.getPlayer());
            // Добавляем небольшую задержку чтобы пользователь мог увидеть меню
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                onPlayerSelected(preferredPlayer);
            }, 500);
        } else {
            // Show menu for user to select player manually
            showMenu();
        }
    }
    
    /**
     * Обновление меню с данными плееров
     */
    private void updateMenuWithData() {
        if (playerTabsAdapter != null) {
            playerTabsAdapter.updateData(
                    animelibPlayers != null ? animelibPlayers : new ArrayList<>(),
                    kodikPlayers != null ? kodikPlayers : new ArrayList<>(),
                    currentPlayerData
            );
        }
    }
    
    /**
     * Поиск предпочитаемого плеера
     */
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
    
    /**
     * Сравнение плееров
     */
    private boolean isSamePlayer(EpisodeResponse.PlayerData player1, EpisodeResponse.PlayerData player2) {
        if (player1 == null || player2 == null) return false;
        
        // Compare by team name and player type
        boolean sameTeam = (player1.getTeam() != null && player2.getTeam() != null) ?
                player1.getTeam().getName().equals(player2.getTeam().getName()) :
                (player1.getTeam() == null && player2.getTeam() == null);
        
        boolean samePlayer = (player1.getPlayer() != null && player2.getPlayer() != null) ?
                player1.getPlayer().equalsIgnoreCase(player2.getPlayer()) :
                (player1.getPlayer() == null && player2.getPlayer() == null);
        
        return sameTeam && samePlayer;
    }
    
    /**
     * Получение доступных качеств для текущего плеера
     */
    public List<String> getAvailableQualities() {
        List<String> qualities = new ArrayList<>();

        if (currentPlayerData == null) {
            return qualities;
        }

        if ("animelib".equalsIgnoreCase(currentPlayerData.getPlayer())) {
            // AnimeLib qualities are in video.quality array
            if (currentPlayerData.getVideo() != null && currentPlayerData.getVideo().getQuality() != null) {
                for (EpisodeResponse.QualityData qualityData : currentPlayerData.getVideo().getQuality()) {
                    String quality = String.valueOf(qualityData.getQuality());
                    // Skip 4K unless enabled
                    if ("2160".equals(quality) || "4K".equals(quality)) {
                        // Skip 4K quality as it's disabled
                        continue;
                    }
                    qualities.add(quality + "p");
                }
            }
        } else if ("kodik".equalsIgnoreCase(currentPlayerData.getPlayer())) {
            // Kodik qualities are usually standard
            qualities.add("720p");
            qualities.add("480p");
            qualities.add("360p");
        }

        return qualities;
    }
    
    /**
     * Показать индикатор загрузки
     */
    private void showLoading() {
        if (menuLoadingOverlay != null) {
            menuLoadingOverlay.setVisibility(View.VISIBLE);
        }
        if (menuLoadingIndicator != null) {
            menuLoadingIndicator.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * Скрыть индикатор загрузки
     */
    private void hideLoading() {
        if (menuLoadingOverlay != null) {
            menuLoadingOverlay.setVisibility(View.GONE);
        }
        if (menuLoadingIndicator != null) {
            menuLoadingIndicator.setVisibility(View.GONE);
        }
    }
    
    /**
     * Скрытие всех UI элементов плееров (для PiP режима)
     */
    public void hideAllPlayersUI() {
        if (slidingMenuPanel != null) {
            slidingMenuPanel.setVisibility(View.GONE);
        }
        if (menuOverlay != null) {
            menuOverlay.setVisibility(View.GONE);
        }
    }
    
    /**
     * Показ всех UI элементов плееров (выход из PiP режима)
     */
    public void showAllPlayersUI() {
        // Menu is shown only when user requests it
    }
    
    // Getters
    public List<EpisodeResponse.PlayerData> getAllPlayers() {
        return allPlayers;
    }
    
    public List<EpisodeResponse.PlayerData> getAnimelibPlayers() {
        return animelibPlayers;
    }
    
    public List<EpisodeResponse.PlayerData> getKodikPlayers() {
        return kodikPlayers;
    }
    
    public EpisodeResponse.PlayerData getCurrentPlayerData() {
        return currentPlayerData;
    }
    
    public boolean isMenuVisible() {
        return isMenuVisible;
    }
    
    public String getPreferredPlayerType() {
        return preferredPlayerType;
    }
    
    public EpisodeResponse.PlayerData getPreferredAnimelibPlayer() {
        return preferredAnimelibPlayer;
    }
    
    public EpisodeResponse.PlayerData getPreferredKodikPlayer() {
        return preferredKodikPlayer;
    }
    
    // Setters
    public void setMenuWidth(float menuWidth) {
        this.menuWidth = menuWidth;
    }
    
    public void setCurrentPlayerData(EpisodeResponse.PlayerData currentPlayerData) {
        this.currentPlayerData = currentPlayerData;
        updateMenuWithData();
    }
    
    public void setPreferredPlayerType(String preferredPlayerType) {
        this.preferredPlayerType = preferredPlayerType;
    }
    
    public void setPreferredAnimelibPlayer(EpisodeResponse.PlayerData preferredAnimelibPlayer) {
        this.preferredAnimelibPlayer = preferredAnimelibPlayer;
    }
    
    public void setPreferredKodikPlayer(EpisodeResponse.PlayerData preferredKodikPlayer) {
        this.preferredKodikPlayer = preferredKodikPlayer;
    }
    
    // Callback setters
    public void setPlayerSelectionCallback(PlayerSelectionCallback callback) {
        this.playerSelectionCallback = callback;
    }
    
    public void setVisibilityCallback(PlayersVisibilityCallback callback) {
        this.visibilityCallback = callback;
    }
    
    public void setDataCallback(PlayersDataCallback callback) {
        this.dataCallback = callback;
    }
    
    /**
     * Очистка ресурсов
     */
    public void cleanup() {
        allPlayers.clear();
        animelibPlayers.clear();
        kodikPlayers.clear();
        currentPlayerData = null;
        playerSelectionCallback = null;
        visibilityCallback = null;
        dataCallback = null;
    }
}
