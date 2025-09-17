package com.example.animelib.managers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.animelib.adapters.HorizontalEpisodesAdapter;
import com.example.animelib.api.ApiService;
import com.example.animelib.models.EpisodesListResponse;
import com.example.animelib.util.DensityUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Менеджер для управления эпизодами и связанным UI
 */
public class EpisodesManager {
    private static final String TAG = "EpisodesManager";

    // Контекст и зависимости
    private final Context context;
    private final ApiService apiService;
    private DensityUtils densityUtils;

    // UI компоненты
    private View episodesMenuPanel;
    private ImageButton episodesMenuButton;
    private RecyclerView episodesRecyclerView;
    private TextView episodesCountText;
    private ImageButton prevEpisodeButton;
    private ImageButton nextEpisodeButton;
    private View menuOverlay;
    private View playersControlBar;

    // Состояние
    private boolean isEpisodesMenuVisible = false;
    private final List<EpisodesListResponse.EpisodeItem> episodes = new ArrayList<>();
    private EpisodesListResponse.EpisodeItem currentEpisode;
    private HorizontalEpisodesAdapter episodesAdapter;

    // Смещение в dp (понятное значение)
    private final float totalOffsetDp = 60f;
    private int totalOffsetPx; // Будет вычислено при инициализации

    // Callback интерфейсы
    public interface EpisodeSelectionCallback {
        void onEpisodeSelected(EpisodesListResponse.EpisodeItem episode);
    }

    public interface EpisodesVisibilityCallback {
        void onEpisodesVisibilityChanged(boolean isVisible);
    }

    public interface EpisodesDataCallback {
        void onEpisodesLoaded(List<EpisodesListResponse.EpisodeItem> episodes);

        void onEpisodesError(String error);
    }

    private EpisodeSelectionCallback episodeSelectionCallback;
    private EpisodesVisibilityCallback visibilityCallback;
    private EpisodesDataCallback dataCallback;

    public EpisodesManager(Context context, ApiService apiService) {
        this.context = context;
        this.apiService = apiService;
    }

    /**
     * Инициализация UI компонентов
     */
    public void initializeViews(View episodesMenuPanel, ImageButton episodesMenuButton,
                                RecyclerView episodesRecyclerView, TextView episodesCountText,
                                ImageButton prevEpisodeButton, ImageButton nextEpisodeButton,
                                View menuOverlay, View playersControlBar) {
        this.episodesMenuPanel = episodesMenuPanel;
        this.episodesMenuButton = episodesMenuButton;
        this.episodesRecyclerView = episodesRecyclerView;
        this.episodesCountText = episodesCountText;
        this.prevEpisodeButton = prevEpisodeButton;
        this.nextEpisodeButton = nextEpisodeButton;
        this.menuOverlay = menuOverlay;
        this.playersControlBar = playersControlBar;

        // Преобразуем dp в px один раз при инициализации
        totalOffsetPx = dpToPx(totalOffsetDp);

        setupEpisodesViews();
        initializeControlBarPosition();
    }

    /**
     * Преобразование dp в px
     */
    private int dpToPx(float dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f); // +0.5f для правильного округления
    }

    /**
     * Инициализация позиции playersControlBar
     */
    private void initializeControlBarPosition() {
        if (playersControlBar == null) return;

        // Изначально опускаем playersControlBar вниз на вычисленное смещение в px
        playersControlBar.setTranslationY(totalOffsetPx);

        Log.d(TAG, "Initialized playersControlBar position with offset: " + totalOffsetPx + "px (" + totalOffsetDp + "dp)");
    }

    /**
     * Настройка UI компонентов эпизодов
     */
    private void setupEpisodesViews() {
        if (episodesMenuButton != null) {
            episodesMenuButton.setOnClickListener(v -> toggleEpisodesMenu());
        }

        setupEpisodesRecyclerView();
        setupEpisodeNavigationButtons();
    }

    /**
     * Настройка RecyclerView для эпизодов (горизонтальный список)
     */
    private void setupEpisodesRecyclerView() {
        if (episodesRecyclerView == null) return;

        episodesAdapter = new HorizontalEpisodesAdapter(episodes, currentEpisode, episode -> {
            if (episodeSelectionCallback != null) {
                episodeSelectionCallback.onEpisodeSelected(episode);
            }
            hideEpisodesMenu();
        });

        // Горизонтальный layout manager для эпизодов в контроллере
        LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
        episodesRecyclerView.setLayoutManager(layoutManager);
        episodesRecyclerView.setAdapter(episodesAdapter);
    }

    /**
     * Настройка кнопок навигации по эпизодам
     */
    private void setupEpisodeNavigationButtons() {
        if (prevEpisodeButton != null) {
            prevEpisodeButton.setOnClickListener(v -> navigateToPreviousEpisode());
        }

        if (nextEpisodeButton != null) {
            nextEpisodeButton.setOnClickListener(v -> navigateToNextEpisode());
        }

        updateEpisodeNavigationButtonsVisibility();
    }

    /**
     * Переключение видимости меню эпизодов
     */
    public void toggleEpisodesMenu() {
        if (isEpisodesMenuVisible) {
            hideEpisodesMenu();
        } else {
            showEpisodesMenu();
        }
    }

    /**
     * Показать меню эпизодов (поднять весь playersControlBar)
     */
    public void showEpisodesMenu() {
        if (episodesRecyclerView == null || playersControlBar == null) return;

        Log.d(TAG, "Showing episodes horizontal list - lifting playersControlBar");
        isEpisodesMenuVisible = true;

        // Сначала показываем RecyclerView с анимацией появления
        episodesRecyclerView.setVisibility(View.VISIBLE);
        episodesRecyclerView.setAlpha(0f);
        episodesRecyclerView.setScaleX(0.95f);
        episodesRecyclerView.setScaleY(0.95f);

        // Анимация для playersControlBar - подъем с "пружинным" эффектом
        playersControlBar.animate()
                .translationY(0)
                .setDuration(300)
                .setInterpolator(new OvershootInterpolator(0.6f)) // Пружинный эффект
                .withStartAction(() -> {
                    // Параллельная анимация появления списка эпизодов
                    episodesRecyclerView.animate()
                            .alpha(1f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(250)
                            .setInterpolator(new DecelerateInterpolator())
                            .start();
                })
                .start();

        updateEpisodeNavigationButtonsVisibility();

        if (visibilityCallback != null) {
            visibilityCallback.onEpisodesVisibilityChanged(true);
        }
    }

    /**
     * Скрыть меню эпизодов (опустить весь playersControlBar)
     */
    public void hideEpisodesMenu() {
        if (episodesRecyclerView == null || playersControlBar == null) return;

        Log.d(TAG, "Hiding episodes horizontal list - lowering playersControlBar");
        isEpisodesMenuVisible = false;

        // Анимация исчезновения списка эпизодов
        episodesRecyclerView.animate()
                .alpha(0f)
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(150)
                .setInterpolator(new AccelerateInterpolator())
                .start();

        // Анимация для playersControlBar - опускание с "антиципацией"
        playersControlBar.animate()
                .translationY(totalOffsetPx)
                .setDuration(250)
                .setInterpolator(new AnticipateInterpolator(1f)) // Эффект anticipation
                .start();

        updateEpisodeNavigationButtonsVisibility();

        if (visibilityCallback != null) {
            visibilityCallback.onEpisodesVisibilityChanged(false);
        }
    }

    /**
     * Навигация к предыдущему эпизоду
     */
    public void navigateToPreviousEpisode() {
        Log.d(TAG, "navigateToPreviousEpisode called");

        if (episodes.isEmpty()) {
            Log.d(TAG, "No episodes available");
            return;
        }

        if (currentEpisode == null) {
            Log.d(TAG, "No current episode, cannot navigate to previous");
            return;
        }

        int currentIndex = -1;
        for (int i = 0; i < episodes.size(); i++) {
            if (Objects.equals(episodes.get(i).getNumber(), currentEpisode.getNumber())) {
                currentIndex = i;
                break;
            }
        }

        Log.d(TAG, "Current index: " + currentIndex + ", episodes count: " + episodes.size());

        if (currentIndex > 0) {
            EpisodesListResponse.EpisodeItem previousEpisode = episodes.get(currentIndex - 1);
            Log.d(TAG, "Navigating to previous episode: " + previousEpisode.getNumber());

            if (episodeSelectionCallback != null) {
                episodeSelectionCallback.onEpisodeSelected(previousEpisode);
            }
        } else {
            Log.d(TAG, "No previous episode available");
        }
    }

    /**
     * Навигация к следующему эпизоду
     */
    public void navigateToNextEpisode() {
        Log.d(TAG, "navigateToNextEpisode called");

        if (episodes.isEmpty()) {
            Log.d(TAG, "No episodes available");
            return;
        }

        if (currentEpisode == null) {
            Log.d(TAG, "No current episode, cannot navigate to next");
            return;
        }

        int currentIndex = -1;
        for (int i = 0; i < episodes.size(); i++) {
            if (Objects.equals(episodes.get(i).getNumber(), currentEpisode.getNumber())) {
                currentIndex = i;
                break;
            }
        }

        Log.d(TAG, "Current index: " + currentIndex + ", episodes count: " + episodes.size());

        if (currentIndex >= 0 && currentIndex < episodes.size() - 1) {
            EpisodesListResponse.EpisodeItem nextEpisode = episodes.get(currentIndex + 1);
            Log.d(TAG, "Navigating to next episode: " + nextEpisode.getNumber());

            if (episodeSelectionCallback != null) {
                episodeSelectionCallback.onEpisodeSelected(nextEpisode);
            }
        } else {
            Log.d(TAG, "No next episode available");
        }
    }

    /**
     * Обновление видимости кнопок навигации по эпизодам
     */
    public void updateEpisodeNavigationButtonsVisibility() {
        Log.d(TAG, "updateEpisodeNavigationButtonsVisibility called");

        if (prevEpisodeButton == null || nextEpisodeButton == null) {
            Log.d(TAG, "Navigation buttons not initialized");
            return;
        }

        // Показываем кнопки только если меню эпизодов скрыто и есть эпизоды
        boolean shouldShow = !episodes.isEmpty();
        int visibility = shouldShow ? View.VISIBLE : View.GONE;

        // Проверяем, есть ли текущий эпизод
        boolean hasCurrentEpisode = currentEpisode != null && currentEpisode.getNumber() != null;

        boolean isFirstEpisode = false;
        boolean isLastEpisode = false;

        if (hasCurrentEpisode && !episodes.isEmpty()) {
            try {
                int currentEpisodeNumber = Integer.parseInt(currentEpisode.getNumber());
                int totalEpisodes = episodes.size();

                isFirstEpisode = (currentEpisodeNumber == 1);
                isLastEpisode = (currentEpisodeNumber == totalEpisodes);

            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing episode number: " + currentEpisode.getNumber(), e);
            }
        }

        // Устанавливаем видимость
        prevEpisodeButton.setVisibility(visibility);
        nextEpisodeButton.setVisibility(visibility);

        // Активируем/деактивируем кнопки в зависимости от позиции
        prevEpisodeButton.setEnabled(!isFirstEpisode && shouldShow);
        nextEpisodeButton.setEnabled(!isLastEpisode && shouldShow);

        // Меняем прозрачность для неактивных кнопок
        float activeAlpha = 1.0f;
        float inactiveAlpha = 0.5f;

        prevEpisodeButton.setAlpha(isFirstEpisode || !shouldShow ? inactiveAlpha : activeAlpha);
        nextEpisodeButton.setAlpha(isLastEpisode || !shouldShow ? inactiveAlpha : activeAlpha);
    }

    /**
     * Загрузка эпизодов
     */
    public void loadEpisodes(String animeId) {
        Log.d(TAG, "Loading episodes for anime ID: " + animeId);

        apiService.fetchEpisodesList(animeId, new ApiService.EpisodesCallback() {
            @Override
            public void onEpisodesReceived(EpisodesListResponse response) {
                ((android.app.Activity) context).runOnUiThread(() -> {
                    if (response != null && response.getData() != null) {
                        episodes.clear();
                        episodes.addAll(response.getData());

                        Log.d(TAG, "Episodes loaded: " + episodes.size());

                        updateEpisodesRecyclerView();
                        updateEpisodesCount();
                        updateEpisodeNavigationButtonsVisibility();

                        if (dataCallback != null) {
                            dataCallback.onEpisodesLoaded(episodes);
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {
                ((android.app.Activity) context).runOnUiThread(() -> {
                    Log.e(TAG, "Error loading episodes: " + error);

                    if (dataCallback != null) {
                        dataCallback.onEpisodesError(error);
                    }
                });
            }
        });
    }

    /**
     * Обновление RecyclerView эпизодов
     */
    public void updateEpisodesRecyclerView() {
        if (episodesRecyclerView != null) {
            HorizontalEpisodesAdapter adapter = new HorizontalEpisodesAdapter(episodes, currentEpisode, episode -> {
                if (episodeSelectionCallback != null) {
                    episodeSelectionCallback.onEpisodeSelected(episode);
                }
                hideEpisodesMenu();
            });
            episodesRecyclerView.setAdapter(adapter);
            episodesAdapter = adapter;
        }
    }

    /**
     * Обновление счетчика эпизодов
     */
    @SuppressLint("SetTextI18n")
    private void updateEpisodesCount() {
        if (episodesCountText != null) {
            episodesCountText.setText(episodes.size() + " эпизодов");
        }
    }

    /**
     * Установка текущего эпизода
     */
    public void setCurrentEpisode(EpisodesListResponse.EpisodeItem episode) {
        Log.d(TAG, "Setting current episode: " + (episode != null ? episode.getNumber() : "null"));
        this.currentEpisode = episode;
        updateEpisodesRecyclerView();
        updateEpisodeNavigationButtonsVisibility();
    }

    /**
     * Поиск и установка текущего эпизода по URL
     */
    public void findAndSetCurrentEpisodeFromUrl(String url) {
        if (url == null || episodes.isEmpty()) {
            Log.d(TAG, "URL is null or episodes list is empty");
            return;
        }

        Log.d(TAG, "Finding current episode from URL: " + url);

        // Извлекаем episode ID из URL (например, ?episode=133642)
        String episodeIdStr = null;
        if (url.contains("episode=")) {
            try {
                episodeIdStr = url.substring(url.indexOf("episode=") + 8);
                if (episodeIdStr.contains("&")) {
                    episodeIdStr = episodeIdStr.substring(0, episodeIdStr.indexOf("&"));
                }
                if (episodeIdStr.contains("#")) {
                    episodeIdStr = episodeIdStr.substring(0, episodeIdStr.indexOf("#"));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error extracting episode ID from URL", e);
            }
        }

        if (episodeIdStr != null) {
            try {
                int episodeId = Integer.parseInt(episodeIdStr);
                Log.d(TAG, "Looking for episode with ID: " + episodeId);

                // Ищем эпизод по ID
                for (EpisodesListResponse.EpisodeItem episode : episodes) {
                    if (episode.getId() == episodeId) {
                        Log.d(TAG, "Found episode by ID: " + episode.getNumber());
                        setCurrentEpisode(episode);
                        return;
                    }
                }

                Log.d(TAG, "Episode with ID " + episodeId + " not found");
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid episode ID format: " + episodeIdStr, e);
            }
        }

        // Если не нашли по ID, берем первый эпизод
        if (!episodes.isEmpty()) {
            Log.d(TAG, "Using first episode as fallback");
            setCurrentEpisode(episodes.get(0));
        }
    }

    /**
     * Получение текущего эпизода
     */
    public EpisodesListResponse.EpisodeItem getCurrentEpisode() {
        return currentEpisode;
    }

    /**
     * Получение списка эпизодов
     */
    public List<EpisodesListResponse.EpisodeItem> getEpisodes() {
        return episodes;
    }

    /**
     * Проверка видимости меню эпизодов
     */
    public boolean isEpisodesMenuVisible() {
        return isEpisodesMenuVisible;
    }

    /**
     * Скрытие всех UI элементов эпизодов (для PiP режима)
     */
    public void hideAllEpisodesUI() {
        if (episodesRecyclerView != null) {
            episodesRecyclerView.setVisibility(View.GONE);
        }
        if (prevEpisodeButton != null) {
            prevEpisodeButton.setVisibility(View.GONE);
        }
        if (nextEpisodeButton != null) {
            nextEpisodeButton.setVisibility(View.GONE);
        }
    }

    /**
     * Показ всех UI элементов эпизодов (выход из PiP режима)
     */
    public void showAllEpisodesUI() {
        updateEpisodeNavigationButtonsVisibility();
        // Меню эпизодов показывается только по запросу пользователя
    }

    /**
     * Установка callback для выбора эпизода
     */
    public void setEpisodeSelectionCallback(EpisodeSelectionCallback callback) {
        this.episodeSelectionCallback = callback;
    }

    /**
     * Установка callback для изменения видимости
     */
    public void setVisibilityCallback(EpisodesVisibilityCallback callback) {
        this.visibilityCallback = callback;
    }

    /**
     * Установка callback для данных
     */
    public void setDataCallback(EpisodesDataCallback callback) {
        this.dataCallback = callback;
    }

    /**
     * Очистка ресурсов
     */
    public void cleanup() {
        episodes.clear();
        currentEpisode = null;
        episodeSelectionCallback = null;
        visibilityCallback = null;
        dataCallback = null;
    }
}
