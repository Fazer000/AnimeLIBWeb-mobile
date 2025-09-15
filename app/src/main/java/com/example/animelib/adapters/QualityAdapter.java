package com.example.animelib.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.animelib.R;

import java.util.List;

public class QualityAdapter extends RecyclerView.Adapter<QualityAdapter.QualityViewHolder> {

    public interface OnQualitySelectedListener {
        void onQualitySelected(String quality);
    }

    private final List<String> qualities;
    private String currentQuality;
    private final OnQualitySelectedListener listener;

    public QualityAdapter(List<String> qualities, String currentQuality, OnQualitySelectedListener listener) {
        this.qualities = qualities;
        this.currentQuality = currentQuality;
        this.listener = listener;
    }

    @NonNull
    @Override
    public QualityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_quality_option, parent, false);
        return new QualityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QualityViewHolder holder, int position) {
        String quality = qualities.get(position);
        holder.qualityText.setText(quality);

        boolean isCurrent = quality.equals(currentQuality);
        holder.currentIndicator.setVisibility(isCurrent ? View.VISIBLE : View.GONE);
        holder.itemView.setSelected(isCurrent);

        // На этот код:
        TypedValue typedValue = new TypedValue();
        Context context = holder.itemView.getContext();

        if (isCurrent) {
            context.getTheme().resolveAttribute(R.attr.secondaryTextColor, typedValue, true);
            holder.qualityText.setTextColor(typedValue.data);
        } else {
            context.getTheme().resolveAttribute(R.attr.primaryTextColor, typedValue, true);
            holder.qualityText.setTextColor(typedValue.data);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onQualitySelected(quality);
            }
        });
    }

    @Override
    public int getItemCount() {
        return qualities.size();
    }
    
    @SuppressLint("NotifyDataSetChanged")
    public void updateCurrentQuality(String newCurrentQuality) {
        this.currentQuality = newCurrentQuality;
        notifyDataSetChanged();
    }

    public static class QualityViewHolder extends RecyclerView.ViewHolder {
        TextView qualityText;
        ImageView currentIndicator;

        QualityViewHolder(@NonNull View itemView) {
            super(itemView);
            qualityText = itemView.findViewById(R.id.qualityText);
            currentIndicator = itemView.findViewById(R.id.currentIndicator);
        }
    }
}
