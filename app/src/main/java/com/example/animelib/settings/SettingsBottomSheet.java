package com.example.animelib.settings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.animelib.R;
import com.example.animelib.adapters.QualityAdapter;
import com.example.animelib.util.ThemeUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;
//import com.google.android.material.bottomsheet.BottomSheetDialogThemeUtils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.materialswitch.MaterialSwitch;

import android.widget.FrameLayout;
import android.view.ViewGroup;
import android.util.TypedValue;

import androidx.annotation.Nullable;

import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

public class SettingsBottomSheet extends BottomSheetDialog {
    private final List<String> qualities;
    private String currentQuality;
    private final QualityAdapter.OnQualitySelectedListener listener;
    private QualityAdapter qualityAdapter;
    private QualityBottomSheet currentQualityBottomSheet;
    private float currentPlaybackSpeed = 1.0f;
    private final SpeedBottomSheet.OnSpeedChangedListener speedListener;
    private boolean enable4K = false;
    private final On4KToggledListener on4KToggledListener;
    private boolean autoPlay = true;
    private final OnAutoPlayToggledListener onAutoPlayToggledListener;
    private int longSkipDuration = 85; // seconds
    private final OnSkipDurationChangedListener onSkipDurationChangedListener;
    private int currentTheme = ThemeUtils.THEME_SYSTEM;
    private final OnThemeChangedListener onThemeChangedListener;

    public SettingsBottomSheet(Context context,
                               List<String> qualities,
                               String currentQuality,
                               QualityAdapter.OnQualitySelectedListener listener,
                               float initialSpeed,
                               SpeedBottomSheet.OnSpeedChangedListener speedListener,
                               boolean enable4K,
                               On4KToggledListener on4KToggledListener,
                               boolean autoPlay,
                               OnAutoPlayToggledListener onAutoPlayToggledListener,
                               int longSkipDuration,
                               OnSkipDurationChangedListener onSkipDurationChangedListener,
                               int currentTheme,
                               OnThemeChangedListener onThemeChangedListener) {
        super(context, com.google.android.material.R.style.ThemeOverlay_Material3_BottomSheetDialog);
        this.qualities = qualities;
        this.currentQuality = currentQuality;
        this.listener = listener;
        this.currentPlaybackSpeed = initialSpeed;
        this.speedListener = speedListener;
        this.enable4K = enable4K;
        this.on4KToggledListener = on4KToggledListener;
        this.autoPlay = autoPlay;
        this.onAutoPlayToggledListener = onAutoPlayToggledListener;
        this.longSkipDuration = longSkipDuration;
        this.onSkipDurationChangedListener = onSkipDurationChangedListener;
        this.currentTheme = currentTheme;
        this.onThemeChangedListener = onThemeChangedListener;
    }

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        @SuppressLint("InflateParams") View view = LayoutInflater.from(getContext()).inflate(R.layout.bs_settings, null);
        setContentView(view);
        Objects.requireNonNull(getWindow()).setLayout(1000, ViewGroup.LayoutParams.WRAP_CONTENT);
        // Expand like YouTube
        setDismissWithAnimation(true);
        try {
            setOnShowListener(d -> {
                FrameLayout bottom = findViewById(com.google.android.material.R.id.design_bottom_sheet);
                if (bottom != null) {
                    BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottom);

                    // Устанавливаем фиксированную высоту (примерно 300dp)
                    int fixedHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 400, getContext().getResources().getDisplayMetrics());

                    ViewGroup.LayoutParams layoutParams = bottom.getLayoutParams();
                    if (layoutParams != null) {
                        layoutParams.height = fixedHeight;
                        bottom.setLayoutParams(layoutParams);
                    }

                    behavior.setFitToContents(true);
                    behavior.setSkipCollapsed(true);
                    behavior.setExpandedOffset(0);
                    behavior.setState(BottomSheetBehavior.STATE_EXPANDED);

                    // Reduce width via side margins
                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) bottom.getLayoutParams();
                    int side = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getContext().getResources().getDisplayMetrics());
                    lp.leftMargin = side;
                    lp.rightMargin = side;
                    bottom.setLayoutParams(lp);
                }
            });
        } catch (Throwable ignored) {
        }

        // Setup option click listeners
        LinearLayout qualityOption = view.findViewById(R.id.qualityOption);
        LinearLayout speedOption = view.findViewById(R.id.speedOption);
        LinearLayout fourKOption = view.findViewById(R.id.fourKOption);
        LinearLayout autoPlayOption = view.findViewById(R.id.autoPlayOption);
        LinearLayout skipDurationOption = view.findViewById(R.id.skipDurationOption);
        LinearLayout themeOption = view.findViewById(R.id.themeOption);

        TextView currentQualityText = view.findViewById(R.id.currentQualityText);
        TextView currentSpeedText = view.findViewById(R.id.currentSpeedText);
        TextView currentSkipDurationText = view.findViewById(R.id.currentSkipDurationText);
        TextView currentThemeText = view.findViewById(R.id.currentThemeText);
        MaterialSwitch fourKSwitch = view.findViewById(R.id.fourKSwitch);
        MaterialSwitch autoPlaySwitch = view.findViewById(R.id.autoPlaySwitch);

        ImageView ivTheme = findViewById(R.id.ivTheme);
        ImageView exitBtn = view.findViewById(R.id.bs_exit);

        // Set current values
        currentQualityText.setText(currentQuality != null ? currentQuality : "1080p");
        currentSpeedText.setText(String.format("%.1fx", currentPlaybackSpeed));
        fourKSwitch.setChecked(enable4K);
        autoPlaySwitch.setChecked(autoPlay);
        currentSkipDurationText.setText(formatDuration(longSkipDuration));
        currentThemeText.setText(getThemeText(currentTheme));

        if (ivTheme != null) {
            int iconResId;
            float scale;
            switch (currentTheme) {
                case ThemeUtils.THEME_SYSTEM:
                    iconResId = R.drawable.ic_auto; // Иконка для автотемы
                    scale = 0.9F;
                    break;
                case ThemeUtils.THEME_LIGHT:
                    iconResId = R.drawable.ic_light; // Иконка для светлой темы
                    scale = 1F;
                    break;
                case ThemeUtils.THEME_DARK:
                    iconResId = R.drawable.ic_night; // Иконка для темной темы (исправлено написание)
                    scale = 0.8F;
                    break;
                default:
                    iconResId = R.drawable.ic_auto; // Иконка по умолчанию
                    scale = 0.9F;
                    break;
            }
            ivTheme.setImageResource(iconResId);
            ivTheme.setScaleX(scale);
            ivTheme.setScaleY(scale);
        }

        exitBtn.setOnClickListener(v -> dismiss());

        // Quality option click
        qualityOption.setOnClickListener(v -> {
            dismiss();
            showQualityDialog();
        });

        // 4K option click
        fourKOption.setOnClickListener(v -> fourKSwitch.setChecked(!fourKSwitch.isChecked()));

        // 4K switch listener
        fourKSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            enable4K = isChecked;
            if (on4KToggledListener != null) {
                on4KToggledListener.on4KToggled(isChecked);
            }
        });

        // AutoPlay option click
        autoPlayOption.setOnClickListener(v -> autoPlaySwitch.setChecked(!autoPlaySwitch.isChecked()));

        // AutoPlay switch listener
        autoPlaySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            autoPlay = isChecked;
            if (onAutoPlayToggledListener != null) {
                onAutoPlayToggledListener.onAutoPlayToggled(isChecked);
            }
        });

        // Skip duration option click
        skipDurationOption.setOnClickListener(v -> {
            dismiss();
            showSkipDurationDialog();
        });

        // Theme option click
        themeOption.setOnClickListener(v -> {
            dismiss();
            showThemeDialog();
        });

        // Speed option click
        speedOption.setOnClickListener(v -> {
            dismiss();
            showSpeedDialog();
        });

        setCancelable(true);
    }

    @Nullable
    private Activity getActivity() {
        Activity activity = null;
        Context context = getContext();
        if (context instanceof Activity) {
            activity = (Activity) context;
        } else if (context instanceof ContextWrapper) {
            Context baseContext = ((ContextWrapper) context).getBaseContext();
            if (baseContext instanceof Activity) {
                activity = (Activity) baseContext;
            }
        }
        return activity;
    }

    private void showQualityDialog() {
        // Create dialog only if it doesn't exist
        if (currentQualityBottomSheet == null) {
            currentQualityBottomSheet = new QualityBottomSheet(getContext(), new ArrayList<>(qualities), currentQuality, quality -> {
                if (listener != null) {
                    listener.onQualitySelected(quality);
                }
                // Update current quality and refresh UI
                currentQuality = quality;
                TextView currentQualityText = findViewById(R.id.currentQualityText);
                if (currentQualityText != null) {
                    currentQualityText.setText(quality);
                }
                // Update the quality dialog itself
                if (currentQualityBottomSheet != null) {
                    currentQualityBottomSheet.updateCurrentQuality(quality);
                }
            });

            // Set up back button listener
            // Show main settings when back button is pressed
            currentQualityBottomSheet.setOnBackPressedListener(this::show);
        } else {
            // Update existing dialog with current data
            currentQualityBottomSheet.updateCurrentQuality(currentQuality);
        }

        currentQualityBottomSheet.show();
    }

    private void showSpeedDialog() {
        @SuppressLint("DefaultLocale") SpeedBottomSheet dialog = new SpeedBottomSheet(getContext(), currentPlaybackSpeed, speed -> {
            currentPlaybackSpeed = speed;
            // Update UI in settings
            TextView currentSpeedText = findViewById(R.id.currentSpeedText);
            if (currentSpeedText != null) {
                currentSpeedText.setText(String.format("%.1fx", speed));
            }
            // Propagate to owner immediately
            if (speedListener != null) {
                speedListener.onSpeedChanged(speed);
            }
        });

        // Set up back button listener
        // Show main settings when back button is pressed
        dialog.setOnBackPressedListener(this::show);

        dialog.show();
    }

    private void showSkipDurationDialog() {
        SkipDurationBottomSheet dialog = new SkipDurationBottomSheet(getContext(), longSkipDuration, duration -> {
            longSkipDuration = duration;
            // Update UI in settings
            TextView currentSkipDurationText = findViewById(R.id.currentSkipDurationText);
            if (currentSkipDurationText != null) {
                currentSkipDurationText.setText(formatDuration(duration));
            }
            // Propagate to owner immediately
            if (onSkipDurationChangedListener != null) {
                onSkipDurationChangedListener.onSkipDurationChanged(duration);
            }
        });

        // Set up back button listener
        // Show main settings when back button is pressed
        dialog.setOnBackPressedListener(this::show);

        dialog.show();
    }

    @SuppressLint("DefaultLocale")
    private String formatDuration(int seconds) {
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        return String.format("%d:%02d", minutes, remainingSeconds);
    }

    public float getCurrentPlaybackSpeed() {
        return currentPlaybackSpeed;
    }

    public void updateQualities(List<String> newQualities, String newCurrentQuality) {
        android.util.Log.d("SettingsDialog", "updateQualities called - newQualities: " + newQualities + ", newCurrentQuality: " + newCurrentQuality);
        this.qualities.clear();
        this.qualities.addAll(newQualities);
        this.currentQuality = newCurrentQuality;

        // Update UI
        TextView currentQualityText = findViewById(R.id.currentQualityText);
        if (currentQualityText != null) {
            currentQualityText.setText(newCurrentQuality != null ? newCurrentQuality : "1080p");
        }

        // Update quality dialog if it exists - обновляем весь список, а не только текущее качество
        if (currentQualityBottomSheet != null) {
            currentQualityBottomSheet.updateQualities(newQualities, newCurrentQuality);
        }
    }

    public interface On4KToggledListener {
        void on4KToggled(boolean enabled);
    }

    public interface OnAutoPlayToggledListener {
        void onAutoPlayToggled(boolean enabled);
    }

    public interface OnSkipDurationChangedListener {
        void onSkipDurationChanged(int durationInSeconds);
    }

    public interface OnThemeChangedListener {
        void onThemeChanged(int themeMode);
    }

    private void showThemeDialog() {
        ThemeSelectionBottomSheet bottomSheet = new ThemeSelectionBottomSheet(getContext(), currentTheme, themeMode -> {
            currentTheme = themeMode;
            // Update UI in settings
            TextView currentThemeText = findViewById(R.id.currentThemeText);

            if (currentThemeText != null) {
                currentThemeText.setText(getThemeText(themeMode));
            }

            // Propagate to owner
            if (onThemeChangedListener != null) {
                onThemeChangedListener.onThemeChanged(themeMode);
            }
        });

        // Set up back button listener
        // Show main settings when back button is pressed
        bottomSheet.setOnBackPressedListener(this::show);

        bottomSheet.show();
    }

    private String getThemeText(int themeMode) {
        switch (themeMode) {
            case ThemeUtils.THEME_SYSTEM:
                return "Авто";
            case ThemeUtils.THEME_LIGHT:
                return "Светлая";
            case ThemeUtils.THEME_DARK:
                return "Темная";
            default:
                return "Авто";
        }
    }
}
