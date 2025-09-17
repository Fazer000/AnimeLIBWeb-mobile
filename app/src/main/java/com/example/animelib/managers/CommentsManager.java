package com.example.animelib.managers;

import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.animelib.R;
import com.example.animelib.adapters.CommentsAdapter;
import com.example.animelib.api.ApiService;
import com.example.animelib.models.CommentsResponse;
import com.example.animelib.models.EpisodesListResponse;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Менеджер для работы с комментариями к эпизодам
 * Обеспечивает загрузку, отображение и управление комментариями
 */
public class CommentsManager {
    
    // UI компоненты
    private View commentsPanel;
    private ImageButton closeCommentsButton;
    private RecyclerView commentsRecyclerView;
    private View commentsLoadingOverlay;
    private ImageButton commentsButton;
    private ImageButton commentsOptionsButton;
    private View menuOverlay;
    
    // Адаптер и состояние
    private CommentsAdapter commentsAdapter;
    private boolean isCommentsVisible = false;
    private int commentsPanelWidth = 320; // dp
    private int commentsCurrentPage = 1;
    private boolean commentsHasNextPage = true;
    private boolean isLoadingComments = false;
    private String commentsSortType = "desc";
    
    // Контекст и сервисы
    private Context context;
    private ApiService apiService;
    private EpisodesListResponse.EpisodeItem currentEpisode;
    
    // Callback интерфейсы
    public interface CommentsVisibilityCallback {
        void onCommentsVisibilityChanged(boolean isVisible);
    }
    
    public interface CommentsDataCallback {
        void onCommentsLoaded(List<CommentsResponse.CommentItem> comments);
        void onCommentsError(String error);
    }
    
    private CommentsVisibilityCallback visibilityCallback;
    private CommentsDataCallback dataCallback;
    
    /**
     * Конструктор CommentsManager
     * @param context Контекст приложения
     * @param apiService Сервис для API запросов
     */
    public CommentsManager(Context context, ApiService apiService) {
        this.context = context;
        this.apiService = apiService;
    }
    
    /**
     * Инициализация UI компонентов комментариев
     * @param commentsPanel Панель комментариев
     * @param closeCommentsButton Кнопка закрытия
     * @param commentsRecyclerView RecyclerView для списка комментариев
     * @param commentsLoadingOverlay Индикатор загрузки
     * @param commentsButton Кнопка открытия комментариев
     * @param commentsOptionsButton Кнопка опций сортировки
     * @param menuOverlay Overlay для закрытия по клику вне панели
     */
    public void initializeViews(View commentsPanel, ImageButton closeCommentsButton,
                               RecyclerView commentsRecyclerView, View commentsLoadingOverlay,
                               ImageButton commentsButton, ImageButton commentsOptionsButton,
                               View menuOverlay) {
        this.commentsPanel = commentsPanel;
        this.closeCommentsButton = closeCommentsButton;
        this.commentsRecyclerView = commentsRecyclerView;
        this.commentsLoadingOverlay = commentsLoadingOverlay;
        this.commentsButton = commentsButton;
        this.commentsOptionsButton = commentsOptionsButton;
        this.menuOverlay = menuOverlay;
        
        setupCommentsViews();
        initializePanelPosition();
    }
    
    /**
     * Настройка обработчиков событий для комментариев
     */
    private void setupCommentsViews() {
        if (closeCommentsButton != null) {
            closeCommentsButton.setOnClickListener(v -> hideCommentsPanel());
        }
        
        if (commentsButton != null) {
            commentsButton.setOnClickListener(v -> {
                Log.d("CommentsManager", "Comments button clicked!");
                toggleCommentsPanel();
            });
            Log.d("CommentsManager", "Comments button initialized successfully");
        } else {
            Log.w("CommentsManager", "Comments button is null!");
        }
        
        setupCommentsOptionsButton();
        setupCommentsRecyclerView();
    }
    
    /**
     * Инициализация начальной позиции панели комментариев
     */
    private void initializePanelPosition() {
        if (commentsPanel == null) return;
        
        // Устанавливаем начальную позицию панели (за экраном справа)
        int panelWidth = (int) (commentsPanelWidth * context.getResources().getDisplayMetrics().density);
        commentsPanel.setTranslationX(panelWidth);
        Log.d("CommentsManager", "Initialized comments panel position: " + panelWidth);
    }
    
    /**
     * Настройка кнопки опций сортировки комментариев
     */
    private void setupCommentsOptionsButton() {
        if (commentsOptionsButton == null) return;
        
        commentsOptionsButton.setOnClickListener(v -> showSortOptionsDialog());
    }
    
    /**
     * Показать диалог выбора сортировки комментариев
     */
    private void showSortOptionsDialog() {
        Log.d("CommentsManager", "Showing sort options dialog");
        Dialog dialog = new Dialog(context);
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

        // Создаем варианты сортировки
        String[] sortOptions = {"Новые", "Старые", "Популярные"};
        String[] sortValues = {"desc", "asc", "votes_up"};

        // Добавляем варианты
        for (int i = 0; i < sortOptions.length; i++) {
            MaterialButton button = new MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            button.setText(sortOptions[i]);

            // Получаем цвета из темы
            TypedValue typedValue = new TypedValue();
            context.getTheme().resolveAttribute(R.attr.primaryTextColor, typedValue, true);
            int primaryTextColor = typedValue.data;

            context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true);
            android.graphics.drawable.Drawable selectableItemBackground = 
                ContextCompat.getDrawable(context, typedValue.resourceId);

            button.setTextColor(primaryTextColor);
            button.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            button.setStrokeWidth(0);
            button.setRippleColor(android.content.res.ColorStateList.valueOf(0x20FFFFFF));
            button.setForeground(selectableItemBackground);
            button.setCornerRadius(0);
            button.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            button.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    dpToPx(48) // Фиксированная высота
            ));

            final String sortValue = sortValues[i];
            button.setOnClickListener(v -> {
                changeCommentsSort(sortValue);
                dialog.dismiss();
            });

            optionsLayout.addView(button);
        }

        dialog.show();
    }
    
    /**
     * Вспомогательный метод для преобразования dp в px
     */
    private int dpToPx(int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }
    
    /**
     * Настройка RecyclerView для комментариев
     */
    private void setupCommentsRecyclerView() {
        if (commentsRecyclerView == null) return;
        
        commentsAdapter = new CommentsAdapter();
        commentsRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        commentsRecyclerView.setAdapter(commentsAdapter);
        
        // Добавляем слушатель прокрутки для пагинации
        commentsRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy <= 0) return;
                
                LinearLayoutManager layoutManager = (LinearLayoutManager) rv.getLayoutManager();
                if (layoutManager == null) return;
                
                int total = layoutManager.getItemCount();
                int last = layoutManager.findLastVisibleItemPosition();
                
                if (!isLoadingComments && commentsHasNextPage && last >= total - 3) {
                    loadCommentsPage(commentsCurrentPage + 1);
                }
            }
        });
    }
    
    /**
     * Переключение видимости панели комментариев
     */
    public void toggleCommentsPanel() {
        Log.d("CommentsManager", "toggleCommentsPanel called, isCommentsVisible: " + isCommentsVisible);
        if (isCommentsVisible) {
            hideCommentsPanel();
        } else {
            showCommentsPanel();
        }
    }
    
    /**
     * Показать панель комментариев
     */
    public void showCommentsPanel() {
        Log.d("CommentsManager", "showCommentsPanel called, commentsPanel: " + (commentsPanel != null) + ", isCommentsVisible: " + isCommentsVisible);
        if (commentsPanel == null || isCommentsVisible) {
            Log.w("CommentsManager", "Cannot show comments panel - panel is null or already visible");
            return;
        }
        
        isCommentsVisible = true;
        
        // Получаем ширину панели в пикселях
        int panelWidth = commentsPanel.getWidth();
        if (panelWidth == 0) {
            // Если ширина еще не измерена, используем значение по умолчанию
            panelWidth = (int) (commentsPanelWidth * context.getResources().getDisplayMetrics().density);
        }
        
        Log.d("CommentsManager", "Showing comments panel, width: " + panelWidth);
        
        // Устанавливаем начальную позицию панели (за экраном справа)
        commentsPanel.setTranslationX(panelWidth);
        
        // Анимация появления панели
        commentsPanel.animate().cancel();
        commentsPanel.animate()
            .translationX(0)
            .setDuration(260)
            .withEndAction(() -> {
                Log.d("CommentsManager", "Show animation completed, final translationX: " + commentsPanel.getTranslationX());
            })
            .start();
        
        // Показать overlay
        if (menuOverlay != null) {
            menuOverlay.setVisibility(View.VISIBLE);
            menuOverlay.animate().alpha(1f).setDuration(200).start();
            commentsPanel.bringToFront();
        }
        
        // Загрузить первую страницу если комментарии пустые
        if (!isLoadingComments && (commentsAdapter == null || commentsAdapter.getItemCount() == 0)) {
            commentsCurrentPage = 1;
            commentsHasNextPage = true;
            loadCommentsPage(1);
        }
        
        // Уведомить о изменении видимости
        if (visibilityCallback != null) {
            visibilityCallback.onCommentsVisibilityChanged(true);
        }
    }
    
    /**
     * Скрыть панель комментариев
     */
    public void hideCommentsPanel() {
        if (commentsPanel == null || !isCommentsVisible) return;
        
        isCommentsVisible = false;
        
        // Получаем ширину панели в пикселях
        int panelWidth = commentsPanel.getWidth();
        if (panelWidth == 0) {
            // Если ширина еще не измерена, используем значение по умолчанию
            panelWidth = (int) (commentsPanelWidth * context.getResources().getDisplayMetrics().density);
        }
        
        Log.d("CommentsManager", "Hiding comments panel, width: " + panelWidth);
        
        // Анимация скрытия панели
        commentsPanel.animate().cancel();
        commentsPanel.animate()
            .translationX(panelWidth)
            .setDuration(220)
            .withEndAction(() -> {
                Log.d("CommentsManager", "Hide animation completed, final translationX: " + commentsPanel.getTranslationX());
                if (!isCommentsVisible && menuOverlay != null) {
                    menuOverlay.setVisibility(View.GONE);
                }
            })
            .start();
        
        // Скрыть overlay если меню тоже не видно
        if (menuOverlay != null) {
            menuOverlay.animate().alpha(0f).setDuration(160).start();
        }
        
        // Уведомить о изменении видимости
        if (visibilityCallback != null) {
            visibilityCallback.onCommentsVisibilityChanged(false);
        }
    }
    
    /**
     * Загрузить страницу комментариев
     * @param page Номер страницы
     */
    public void loadCommentsPage(int page) {
        if (currentEpisode == null) return;
        
        isLoadingComments = true;
        if (commentsLoadingOverlay != null) {
            commentsLoadingOverlay.setVisibility(View.VISIBLE);
        }
        
        long episodeId = currentEpisode.getId();
        apiService.fetchEpisodeComments(episodeId, commentsSortType, page, 
            new ApiService.EpisodeCommentsCallback() {
            @Override
            public void onCommentsReceived(CommentsResponse response) {
                // Выполняем обновление UI в главном потоке
                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        if (commentsLoadingOverlay != null) {
                            commentsLoadingOverlay.setVisibility(View.GONE);
                        }
                        isLoadingComments = false;
                        
                        if (response != null && commentsAdapter != null) {
                            commentsAdapter.appendResponse(response, page > 1);
                            
                            // Уведомить о загрузке данных
                            if (dataCallback != null && response.getData() != null) {
                                // Объединяем root и replies комментарии
                                List<CommentsResponse.CommentItem> allComments = new ArrayList<>();
                                if (response.getData().getRoot() != null) {
                                    allComments.addAll(response.getData().getRoot());
                                }
                                if (response.getData().getReplies() != null) {
                                    allComments.addAll(response.getData().getReplies());
                                }
                                dataCallback.onCommentsLoaded(allComments);
                            }
                        }
                        
                        if (response != null && response.getMeta() != null) {
                            commentsHasNextPage = response.getMeta().isHas_next_page();
                            if (commentsHasNextPage) {
                                commentsCurrentPage = page;
                            }
                        }
                    });
                }
            }
                
            @Override
            public void onError(String error) {
                isLoadingComments = false;
                if (commentsLoadingOverlay != null) {
                    commentsLoadingOverlay.setVisibility(View.GONE);
                }
                
                // Показать Toast в UI потоке
                if (context instanceof android.app.Activity) {
                    ((android.app.Activity) context).runOnUiThread(() -> {
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
                    });
                }
                
                // Уведомить об ошибке
                if (dataCallback != null) {
                    dataCallback.onCommentsError(error);
                }
            }
            });
    }
    
    /**
     * Сбросить состояние комментариев при смене эпизода
     * @param reloadIfVisible Перезагрузить если панель видна
     */
    public void resetCommentsOnEpisodeChange(boolean reloadIfVisible) {
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
    
    /**
     * Изменить сортировку комментариев
     * @param sort Новый тип сортировки
     */
    public void changeCommentsSort(String sort) {
        if (sort == null || sort.equals(commentsSortType)) return;
        
        commentsSortType = sort;
        resetCommentsOnEpisodeChange(isCommentsVisible);
        
        if (isCommentsVisible) {
            loadCommentsPage(1);
        }
    }
    
    /**
     * Установить текущий эпизод
     * @param episode Текущий эпизод
     */
    public void setCurrentEpisode(EpisodesListResponse.EpisodeItem episode) {
        this.currentEpisode = episode;
    }
    
    /**
     * Получить текущий эпизод
     * @return Текущий эпизод
     */
    public EpisodesListResponse.EpisodeItem getCurrentEpisode() {
        return currentEpisode;
    }
    
    /**
     * Проверить видимость панели комментариев
     * @return true если панель видна
     */
    public boolean isCommentsVisible() {
        return isCommentsVisible;
    }
    
    /**
     * Получить текущий тип сортировки
     * @return Тип сортировки
     */
    public String getCommentsSortType() {
        return commentsSortType;
    }
    
    /**
     * Установить callback для изменения видимости
     * @param callback Callback для видимости
     */
    public void setVisibilityCallback(CommentsVisibilityCallback callback) {
        this.visibilityCallback = callback;
    }
    
    /**
     * Установить callback для данных комментариев
     * @param callback Callback для данных
     */
    public void setDataCallback(CommentsDataCallback callback) {
        this.dataCallback = callback;
    }
    
    /**
     * Обновить видимость кнопки комментариев
     * @param isVisible Видима ли кнопка
     */
    public void updateCommentsButtonVisibility(boolean isVisible) {
        if (commentsButton != null) {
            commentsButton.setVisibility(isVisible ? View.VISIBLE : View.GONE);
            Log.d("CommentsManager", "Comments button visibility set to: " + (isVisible ? "VISIBLE" : "GONE"));
        } else {
            Log.w("CommentsManager", "Cannot update comments button visibility - button is null!");
        }
    }
    
    /**
     * Скрыть все UI элементы комментариев (для PiP режима)
     */
    public void hideAllCommentsUI() {
        if (isCommentsVisible) {
            hideCommentsPanel();
        }
        updateCommentsButtonVisibility(false);
    }
    
    /**
     * Показать все UI элементы комментариев (для выхода из PiP режима)
     * @param wasVisibleBeforePiP Была ли панель видна до PiP
     */
    public void showAllCommentsUI(boolean wasVisibleBeforePiP) {
        updateCommentsButtonVisibility(true);
        
        if (wasVisibleBeforePiP && !isCommentsVisible) {
            showCommentsPanel();
        }
    }
    
    /**
     * Очистить ресурсы менеджера комментариев
     */
    public void cleanup() {
        if (commentsAdapter != null) {
            commentsAdapter.clearAll();
        }
        
        commentsCurrentPage = 1;
        commentsHasNextPage = true;
        isLoadingComments = false;
        isCommentsVisible = false;
        
        visibilityCallback = null;
        dataCallback = null;
    }
}
