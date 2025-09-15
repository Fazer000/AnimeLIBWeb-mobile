package com.example.animelib.dialogs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.Toast;
import com.example.animelib.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SkipDurationDialog extends BottomSheetDialog {
    private int currentDuration; // in seconds
    private OnDurationChangedListener listener;
    private Spinner minutesSpinner;
    private Spinner secondsSpinner;
    private OnBackPressedListener onBackPressedListener;

    public SkipDurationDialog(Context context, int currentDuration, OnDurationChangedListener listener) {
        super(context, com.google.android.material.R.style.ThemeOverlay_Material3_BottomSheetDialog);
        this.currentDuration = currentDuration;
        this.listener = listener;
    }
    
    public interface OnBackPressedListener {
        void onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        View view = LayoutInflater.from(getContext()).inflate(R.layout.bs_skip_duration, null);
        setContentView(view);
        Objects.requireNonNull(getWindow()).setLayout(1000, ViewGroup.LayoutParams.WRAP_CONTENT);
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
        
        minutesSpinner = view.findViewById(R.id.minutesSpinner);
        secondsSpinner = view.findViewById(R.id.secondsSpinner);
        ImageButton backButton = view.findViewById(R.id.backButton);
        
        // Setup minutes spinner (0-10 minutes)
        List<String> minutesList = new ArrayList<>();
        for (int i = 0; i <= 10; i++) {
            minutesList.add(i + " мин");
        }
        ArrayAdapter<String> minutesAdapter = new ArrayAdapter<String>(getContext(), R.layout.beautiful_spinner_item, minutesList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;

                TypedValue typedValue = new TypedValue();
                Resources.Theme theme = getContext().getTheme();
                theme.resolveAttribute(R.attr.primaryTextColor, typedValue, true);

                textView.setTextColor(typedValue.data);
                textView.setTextSize(16);
                return view;
            }
        };
        minutesAdapter.setDropDownViewResource(R.layout.beautiful_spinner_dropdown_item);
        minutesSpinner.setAdapter(minutesAdapter);
        
        // Setup seconds spinner (0-59 seconds)
        List<String> secondsList = new ArrayList<>();
        for (int i = 0; i < 60; i += 5) { // 0, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55
            secondsList.add(i + " сек");
        }
        ArrayAdapter<String> secondsAdapter = new ArrayAdapter<String>(getContext(), R.layout.beautiful_spinner_item, secondsList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;

                TypedValue typedValue = new TypedValue();
                Resources.Theme theme = getContext().getTheme();
                theme.resolveAttribute(R.attr.primaryTextColor, typedValue, true);
                textView.setTextSize(16);
                return view;
            }
        };
        secondsAdapter.setDropDownViewResource(R.layout.beautiful_spinner_dropdown_item);
        secondsSpinner.setAdapter(secondsAdapter);
        
        // Set current values
        int minutes = currentDuration / 60;
        int seconds = currentDuration % 60;
        
        // Round seconds to nearest 5
        int roundedSeconds = ((seconds + 2) / 5) * 5;
        if (roundedSeconds >= 60) {
            roundedSeconds = 0;
            minutes++;
        }
        
        minutesSpinner.setSelection(Math.min(minutes, 10));
        secondsSpinner.setSelection(roundedSeconds / 5);
        
        // Set up click listeners
        backButton.setOnClickListener(v -> {
            // Call back listener to show main settings
            if (onBackPressedListener != null) {
                onBackPressedListener.onBackPressed();
            }
            dismiss();
        });
        
        // Add confirm button
        ImageButton confirmButton = view.findViewById(R.id.confirmButton);
        confirmButton.setOnClickListener(v -> {
            int selectedMinutes = minutesSpinner.getSelectedItemPosition();
            int selectedSeconds = secondsSpinner.getSelectedItemPosition() * 5;
            int totalSeconds = selectedMinutes * 60 + selectedSeconds;
            
            if (totalSeconds < 10) {
                Toast.makeText(getContext(), "Минимальное время: 10 секунд", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (listener != null) {
                listener.onDurationChanged(totalSeconds);
            }
            dismiss();
        });
        
        setCancelable(true);
    }
    
    public interface OnDurationChangedListener {
        void onDurationChanged(int durationInSeconds);
    }
    
    public void setOnBackPressedListener(OnBackPressedListener listener) {
        this.onBackPressedListener = listener;
    }
}
