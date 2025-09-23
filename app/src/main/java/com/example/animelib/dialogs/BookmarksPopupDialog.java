package com.example.animelib.dialogs;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.animation.ValueAnimator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;
import androidx.viewpager2.widget.ViewPager2;

import android.widget.ProgressBar;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.example.animelib.R;
import com.example.animelib.VideoPlayerActivity;
import com.example.animelib.adapters.BookmarksViewPagerAdapter;
import com.example.animelib.data.DatabaseManager;
import com.example.animelib.models.BookmarksListResponse;

import java.util.List;

public class BookmarksPopupDialog extends Dialog {
    private final Context context;
    private final BookmarksListResponse bookmarksResponse;
    private final Handler autoCloseHandler;
    private Runnable autoCloseRunnable;
    private View dialogView;
    private ProgressBar progressBar;
    private ObjectAnimator progressAnimator;
    private final int autoCloseDelay; // 5 секунд

    // Swipe down variables
    private float startY;
    private float startX;
    private int screenHeight;

    public BookmarksPopupDialog(@NonNull Context context, BookmarksListResponse bookmarksResponse, int autoCloseDelay) {
        super(context);
        this.context = context;
        this.bookmarksResponse = bookmarksResponse;
        this.autoCloseDelay = autoCloseDelay;
        this.autoCloseHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Убираем заголовок диалога
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Устанавливаем прозрачный фон
        setContentView(R.layout.dialog_bookmarks_popup);

        // Получаем ссылку на корневой view для анимации
        dialogView = findViewById(android.R.id.content).getRootView();

        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setGravity(Gravity.BOTTOM);

            // Получаем текущие параметры окна
            WindowManager.LayoutParams params = window.getAttributes();

            // Устанавливаем match_parent по ширине
            params.width = WindowManager.LayoutParams.MATCH_PARENT;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            params.gravity = Gravity.BOTTOM;

            // Убираем затемнение фона
            params.dimAmount = 0.0f;

            // Разрешаем клики вне диалога
            params.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
            params.flags |= WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
            params.flags |= WindowManager.LayoutParams.FLAG_SPLIT_TOUCH;

            // Применяем параметры
            window.setAttributes(params);
        }

        // Получаем высоту экрана
        android.util.DisplayMetrics displayMetrics = new android.util.DisplayMetrics();
        ((android.app.Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenHeight = displayMetrics.heightPixels;

        setupViews();
        setupAutoClose();
        setupSwipeToDismiss();

        // Запускаем анимацию появления
        startSlideInAnimation();
    }

    private void setupViews() {
        ViewPager2 viewPager = findViewById(R.id.bookmarks_viewpager);
        TabLayout tabLayout = findViewById(R.id.bookmarks_tab_layout);
        progressBar = findViewById(R.id.bookmarkProgressBar);

        // Настраиваем ViewPager2
        if (bookmarksResponse != null && bookmarksResponse.getData() != null) {
            List<BookmarksListResponse.BookmarkItem> bookmarksList = bookmarksResponse.getData();

            if (bookmarksList.isEmpty()) {
                // Если закладок нет, скрываем карусель
                viewPager.setVisibility(View.GONE);
                tabLayout.setVisibility(View.GONE);
            } else {
                // Конвертируем List в Array для ViewPager2
                BookmarksListResponse.BookmarkItem[] bookmarksArray = bookmarksList.toArray(new BookmarksListResponse.BookmarkItem[0]);

                BookmarksViewPagerAdapter adapter = new BookmarksViewPagerAdapter(bookmarksArray, new BookmarksViewPagerAdapter.OnBookmarkClickListener() {
                    @OptIn(markerClass = UnstableApi.class)
                    @Override
                    public void onBookmarkClick(BookmarksListResponse.BookmarkItem bookmark) {
                        // Переходим в плеер с URL аниме из закладки
                        if (bookmark.getMedia() != null && bookmark.getMedia().getSlugUrl() != null) {
                            // Строим полный URL аниме из slug_url используя домен из БД
                            String slugUrl = bookmark.getMedia().getSlugUrl();
                            DatabaseManager dbManager = new DatabaseManager(context);
                            String siteUrl = dbManager.getSiteUrl();
                            String animeUrl = siteUrl + "/ru/anime/" + slugUrl;
 
                            // Запускаем VideoPlayerActivity с URL аниме
                            if (context instanceof android.app.Activity) {
                                VideoPlayerActivity.startFromAnimePage((android.app.Activity) context, animeUrl);
                            }
                            dismiss();
                        }
                    }
                });

                viewPager.setAdapter(adapter);

                // Настраиваем горизонтальную ориентацию
                viewPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);

                // Связываем TabLayout с ViewPager2
                new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
                    // Устанавливаем пустые табы, так как у нас есть индикаторы
                }).attach();
            }
        } else {
            viewPager.setVisibility(View.GONE);
            tabLayout.setVisibility(View.GONE);
        }
    }

    private void setupAutoClose() {
        if (progressBar != null) {
            // Инициализируем прогресс-бар
            progressBar.setMax(100);
            progressBar.setProgress(0);
            progressBar.setVisibility(View.VISIBLE);

            // Запускаем анимацию прогресса на 5 секунд
            setProgressSmoothly(progressBar, autoCloseDelay);
        }
    }

    private void setProgressSmoothly(ProgressBar progressBar, int duration) {
        // Отменяем предыдущую анимацию, если она есть
        if (progressAnimator != null && progressAnimator.isRunning()) {
            progressAnimator.cancel();
        }

        progressAnimator = ObjectAnimator.ofInt(progressBar, "progress", 100);
        progressAnimator.setDuration(duration);
        progressAnimator.setInterpolator(new LinearInterpolator());

        progressAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (isShowing()) {
                    dialogView.post(() -> {
                        if (isShowing()) {
                            BookmarksPopupDialog.super.dismiss();
                        }
                    });
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                // Анимация была отменена (например, при касании)
                android.util.Log.d("BookmarksPopup", "Progress animation cancelled");
            }
        });

        progressAnimator.start();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupSwipeToDismiss() {
        if (dialogView != null) {
            dialogView.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startY = event.getY();
                        startX = event.getX();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float deltaY = event.getY() - startY;
                        float deltaX = event.getX() - startX;

                        // Проверяем, что это вертикальный свайп вниз
                        if (Math.abs(deltaY) > Math.abs(deltaX) && deltaY > 0) {
                            // Ограничиваем смещение так, чтобы диалог не ушел под панель навигации
                            // Максимальное смещение = 200dp от низа экрана
                            float maxTranslation = screenHeight * 0.3f; // 30% от высоты экрана
                            float translation = Math.min(deltaY, maxTranslation);
                            v.setTranslationY(translation);
                            return true;
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        float finalDeltaY = event.getY() - startY;
                        float maxTranslation = screenHeight * 0.3f;
                        float currentTranslation = Math.min(finalDeltaY, maxTranslation);

                        // Если свайп достаточно большой (больше половины от максимального смещения), закрываем диалог
                        if (currentTranslation > maxTranslation * 0.1f) {
                            // Анимируем полное скрытие вниз
                            v.animate()
                                    .translationY(screenHeight)
                                    .alpha(0f)
                                    .setDuration(300)
                                    .withEndAction(() -> {
                                        if (isShowing()) {
                                            BookmarksPopupDialog.super.dismiss();
                                        }
                                    })
                                    .start();
                        } else {
                            // Возвращаем диалог на место
                            v.animate()
                                    .translationY(0)
                                    .alpha(1f)
                                    .setDuration(200)
                                    .start();
                        }
                        return true;
                }
                return false;
            });
        }
    }

    private void stopAutoCloseTimer() {
        // Отменяем анимацию прогресса
        if (progressAnimator != null && progressAnimator.isRunning()) {
            progressAnimator.cancel();
            android.util.Log.d("BookmarksPopup", "Auto-close timer stopped");
        }

        // Также отменяем handler если он используется
        if (autoCloseHandler != null && autoCloseRunnable != null) {
            autoCloseHandler.removeCallbacks(autoCloseRunnable);
        }
    }

    @Override
    public void dismiss() {
        android.util.Log.d("BookmarksPopup", "Dismissing dialog");

        // Останавливаем таймер при закрытии
        stopAutoCloseTimer();

        // Отменяем все pending callbacks при ручном закрытии
        if (autoCloseHandler != null) {
            autoCloseHandler.removeCallbacksAndMessages(null);
        }

        // Запускаем анимацию исчезновения
        startSlideOutAnimation();
    }


    @Override
    protected void onStop() {
        super.onStop();
        // Очищаем обработчики при закрытии диалога
        if (autoCloseHandler != null) {
            autoCloseHandler.removeCallbacksAndMessages(null);
        }
    }

    /**
     * Запускает анимацию появления диалога слева
     */
    private void startSlideInAnimation() {
        if (dialogView != null) {
            Animation slideInAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_in_from_left);
            dialogView.startAnimation(slideInAnimation);
        }
    }

    /**
     * Запускает анимацию исчезновения диалога влево
     */
    private void startSlideOutAnimation() {
        if (dialogView != null) {
            Animation slideOutAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_out_to_left);
            slideOutAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    // Анимация началась
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    // Анимация закончилась, закрываем диалог
                    BookmarksPopupDialog.super.dismiss();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                    // Повтор анимации
                }
            });
            dialogView.startAnimation(slideOutAnimation);
        } else {
            // Если view не найден, закрываем диалог сразу
            super.dismiss();
        }
    }
}
