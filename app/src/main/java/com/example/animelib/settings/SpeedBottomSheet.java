package com.example.animelib.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import com.example.animelib.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.widget.FrameLayout;
import android.view.ViewGroup;
import android.util.TypedValue;

import java.util.Objects;

public class SpeedBottomSheet extends BottomSheetDialog {
    private float currentSpeed;
    private OnSpeedChangedListener listener;
    private SeekBar speedSeekBar;
    private TextView currentSpeedText;
    private OnBackPressedListener onBackPressedListener;

    public interface OnSpeedChangedListener {
        void onSpeedChanged(float speed);
    }
    
    public interface OnBackPressedListener {
        void onBackPressed();
    }

    public SpeedBottomSheet(Context context, float currentSpeed, OnSpeedChangedListener listener) {
        super(context, com.google.android.material.R.style.ThemeOverlay_Material3_BottomSheetDialog);
        this.currentSpeed = currentSpeed;
        this.listener = listener;
    }

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View view = LayoutInflater.from(getContext()).inflate(R.layout.bs_speed, null);
        setContentView(view);
        Objects.requireNonNull(getWindow()).setLayout(1000, ViewGroup.LayoutParams.WRAP_CONTENT);
        // Expand like YouTube
        setDismissWithAnimation(true);
        try {
            setOnShowListener(d -> {
                FrameLayout bottom = findViewById(com.google.android.material.R.id.design_bottom_sheet);
                if (bottom != null) {
                    BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottom);
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
        } catch (Throwable ignored) {}

        // Setup back button
        ImageButton backButton = view.findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            // Call back listener to show main settings
            if (onBackPressedListener != null) {
                onBackPressedListener.onBackPressed();
            }
            dismiss();
        });

        // Setup speed controls
        speedSeekBar = view.findViewById(R.id.speedSeekBar);
        currentSpeedText = view.findViewById(R.id.currentSpeedText);

        // Convert speed to progress (0.5x-2.0x -> 0-20)
        int progress = (int) ((currentSpeed - 0.5f) * 20f / 1.5f);
        speedSeekBar.setProgress(progress);
        currentSpeedText.setText(String.format("%.1fx", currentSpeed));

        speedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @SuppressLint("DefaultLocale")
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    currentSpeed = 0.5f + (progress * 1.5f / 20f);
                    currentSpeedText.setText(String.format("%.1fx", currentSpeed));
                    if (listener != null) {
                        listener.onSpeedChanged(currentSpeed);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Setup preset buttons
        setupPresetButton(view, R.id.speed05x, 0.5f);
        setupPresetButton(view, R.id.speed1x, 1.0f);
        setupPresetButton(view, R.id.speed15x, 1.5f);
        setupPresetButton(view, R.id.speed2x, 2.0f);
        setupPresetButton(view, R.id.speedSkip, 10.0f);

        setCancelable(true);
    }

    @SuppressLint("DefaultLocale")
    private void setupPresetButton(View view, int buttonId, float speed) {
        view.findViewById(buttonId).setOnClickListener(v -> {
            currentSpeed = speed;
            int progress = (int) ((speed - 0.5f) * 20f / 1.5f);
            speedSeekBar.setProgress(progress);
            currentSpeedText.setText(String.format("%.1fx", speed));
            if (listener != null) {
                listener.onSpeedChanged(speed);
            }
            // Don't dismiss, just update the speed
        });
    }
    
    public void setOnBackPressedListener(OnBackPressedListener listener) {
        this.onBackPressedListener = listener;
    }
}
