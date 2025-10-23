package com.example.animelib.managers;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;
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
    private LinearLayout slidingMenuPanel;
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
    private boolean enable4K = false; // Настройка 4K
    
    // Адаптер для табов
    private PlayerTabsAdapter playerTabsAdapter;
    private TabLayoutMediator tabLayoutMediator;
    
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
    public void initializeViews(LinearLayout slidingMenuPanel, ImageButton closeMenuButton,
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
    }
    
    /**
     * Настройка UI компонентов плееров
     */
    private void setupPlayersViews() {
        if (closeMenuButton != null) {
            closeMenuButton.setOnClickListener(v -> {
                if (currentPlayerData != null) {
                    hideMenu();
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

            // Setup TabLayout with ViewPager - используем динамические названия
            setupTabLayoutMediator();
        }
    }
    
    /**
     * Настройка TabLayoutMediator (создается заново при обновлении данных)
     */
    private void setupTabLayoutMediator() {
        // Отключаем старый медиатор если он есть
        if (tabLayoutMediator != null) {
            tabLayoutMediator.detach();
        }
        
        if (playerTabLayout != null && playersViewPager != null) {
            tabLayoutMediator = new TabLayoutMediator(playerTabLayout, playersViewPager,
                    (tab, position) -> {
                        String playerType = playerTabsAdapter.getPlayerTypeAtPosition(position);
                        if ("animelib".equals(playerType)) {
                            tab.setText("AnimeLib");
                        } else if ("kodik".equals(playerType)) {
                            tab.setText("Kodik");
                        }
                    });
            tabLayoutMediator.attach();
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
        if (!isMenuVisible) {
            Log.d(TAG, "Showing players menu");
            isMenuVisible = true;
            
            // Use VideoPlayerActivity's method to open draggable panel
            if (context instanceof com.example.animelib.VideoPlayerActivity) {
                ((com.example.animelib.VideoPlayerActivity) context).openMenuPanel();
            }
            
            if (visibilityCallback != null) {
                visibilityCallback.onPlayersVisibilityChanged(true);
            }
        }
    }
    
    /**
     * Скрыть меню плееров
     */
    @OptIn(markerClass = UnstableApi.class)
    public void hideMenu() {
        // Block closing if no episode selected
        if (currentPlayerData == null) {
            return;
        }
        
        if (isMenuVisible) {
            Log.d(TAG, "Hiding players menu");
            isMenuVisible = false;
            
            // Use VideoPlayerActivity's method to close draggable panel
            if (context instanceof com.example.animelib.VideoPlayerActivity) {
                ((com.example.animelib.VideoPlayerActivity) context).closeMenuPanel();
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
                safeRunOnUiThread(() -> {
                    hideLoading();
                    
                    if (response.getData() != null && response.getData().getPlayers() != null) {
                        List<EpisodeResponse.PlayerData> players = response.getData().getPlayers();
                        
                        // Попытка автоматического выбора плеера на основе сохраненных предпочтений
                        com.example.animelib.data.entity.PlayerPreferences prefs = apiService.loadPlayerPreferences();
                        
                        Log.d(TAG, "Loaded preferences from DB: " + (prefs != null ? 
                              ("player=" + prefs.getPlayer() + ", teamId=" + prefs.getTeamId() + ", quality=" + prefs.getPreferredQuality()) : 
                              "null"));
                        
                        EpisodeResponse.PlayerData matchingPlayer = null;
                        
                        if (prefs != null && prefs.getPlayer() != null && prefs.getTeamId() != null) {
                            Log.d(TAG, "Found saved preferences: player=" + prefs.getPlayer() + ", teamId=" + prefs.getTeamId());
                            
                            // Сначала ищем точное совпадение (сохраненный плеер + озвучка)
                            for (EpisodeResponse.PlayerData player : players) {
                                if (player.getPlayer() != null && 
                                    player.getPlayer().equals(prefs.getPlayer()) && 
                                    player.getTeam() != null && 
                                    player.getTeam().getId() == prefs.getTeamId()) {
                                    matchingPlayer = player;
                                    Log.d(TAG, "Found exact match in saved player: " + player.getPlayer() + 
                                          ", team: " + player.getTeam().getName());
                                    break;
                                }
                            }
                            
                            // Если не найдено в сохраненном плеере, ищем озвучку в других плеерах
                            if (matchingPlayer == null) {
                                Log.d(TAG, "Team not found in saved player, searching in other players");
                                for (EpisodeResponse.PlayerData player : players) {
                                    if (player.getTeam() != null && 
                                        player.getTeam().getId() == prefs.getTeamId()) {
                                        matchingPlayer = player;
                                        Log.d(TAG, "Found team in different player: " + player.getPlayer() + 
                                              ", team: " + player.getTeam().getName());
                                        break;
                                    }
                                }
                            }
                        } else {
                            Log.d(TAG, "No saved player preferences found");
                        }
                        
                        if (matchingPlayer != null) {
                            // Автоматически выбираем найденный плеер БЕЗ показа меню
                            Log.d(TAG, "Auto-selecting player for episode change");
                            
                            // Сохраняем данные плееров БЕЗ показа меню
                            setPlayersDataSilent(players);
                            
                            // Вызываем callback для выбора плеера
                            if (playerSelectionCallback != null) {
                                playerSelectionCallback.onPlayerSelected(matchingPlayer);
                            }
                        } else {
                            // Показываем меню выбора только если автовыбор не сработал
                            Log.d(TAG, "No matching player found for episode, showing selection menu");
                            setPlayersData(players);
                        }
                        
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
                safeRunOnUiThread(() -> {
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
     * Устанавливает данные плееров БЕЗ показа меню
     * Используется при автовыборе через сохраненные предпочтения
     */
    public void setPlayersDataSilent(List<EpisodeResponse.PlayerData> players) {
        Log.d(TAG, "Setting players data silently (no menu): " + players.size() + " players");
        
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
        
        // Update menu with players (but don't show it)
        updateMenuWithData();
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
            // Пересоздаем TabLayoutMediator для обновления табов
            setupTabLayoutMediator();
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
                    // Skip 4K if not enabled
                    if (("2160".equals(quality) || "4K".equals(quality)) && !enable4K) {
                        Log.d(TAG, "Skipping 4K quality (not enabled)");
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
     * Установка настройки 4K
     */
    public void setEnable4K(boolean enable4K) {
        this.enable4K = enable4K;
        Log.d(TAG, "4K setting updated: " + enable4K);
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
        if (context instanceof com.example.animelib.VideoPlayerActivity) {
            ((com.example.animelib.VideoPlayerActivity) context).closeMenuPanel();
        }
        if (menuOverlay != null) {
            menuOverlay.setVisibility(View.GONE);
        }
        isMenuVisible = false;
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
    
    /**
     * Вызывается когда панель закрывается через драг
     */
    public void onPanelClosedByDrag() {
        Log.d(TAG, "Panel closed by drag, updating isMenuVisible flag");
        isMenuVisible = false;
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
    /**
     * Безопасно вызывает код в главном потоке
     */
    private void safeRunOnUiThread(Runnable runnable) {
        try {
            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).runOnUiThread(runnable);
            } else {
                runnable.run();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calling UI thread", e);
            // Fallback - вызываем в текущем потоке
            try {
                runnable.run();
            } catch (Exception ex) {
                Log.e(TAG, "Error in fallback callback", ex);
            }
        }
    }

    public void cleanup() {
        allPlayers.clear();
        animelibPlayers.clear();
        kodikPlayers.clear();
        currentPlayerData = null;
        playerSelectionCallback = null;
        visibilityCallback = null;
        dataCallback = null;
    }
    
    /**
     * Завершает drag жест с решением открыть или закрыть панель плееров
     */
    public void completeDrag(boolean shouldOpen) {
        Log.d(TAG, "Complete players drag: shouldOpen=" + shouldOpen);
        
        if (shouldOpen) {
            // При drag открытии НЕ вызываем openMenuPanel() - панель уже открывается через DraggableSidePanel
            // Только обновляем флаг
            if (isMenuVisible) {
                Log.w(TAG, "Players menu already visible, skipping");
                return;
            }
            
            isMenuVisible = true;
            
            // Уведомить о изменении видимости
            if (visibilityCallback != null) {
                visibilityCallback.onPlayersVisibilityChanged(true);
            }
        } else {
            hideMenu();
        }
    }
    
    /**
     * Обновляет состояние после drag (вызывается после завершения анимации DraggableSidePanel)
     */
    public void updateDragState(boolean isOpen) {
        Log.d(TAG, "Update drag state: isOpen=" + isOpen);
        
        if (isOpen) {
            isMenuVisible = true;
            
            if (visibilityCallback != null) {
                visibilityCallback.onPlayersVisibilityChanged(true);
            }
        } else {
            isMenuVisible = false;
            if (visibilityCallback != null) {
                visibilityCallback.onPlayersVisibilityChanged(false);
            }
        }
    }
}
