package com.example.animelib.adapters;

import android.content.res.Resources;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.animelib.R;
import com.example.animelib.models.EpisodesListResponse;

import java.util.List;

public class HorizontalEpisodesAdapter extends RecyclerView.Adapter<HorizontalEpisodesAdapter.EpisodeViewHolder> {

    private final List<EpisodesListResponse.EpisodeItem> episodes;
    private final EpisodesListResponse.EpisodeItem currentEpisode;
    private final OnEpisodeSelectedListener listener;

    public interface OnEpisodeSelectedListener {
        void onEpisodeSelected(EpisodesListResponse.EpisodeItem episode);
    }

    public HorizontalEpisodesAdapter(List<EpisodesListResponse.EpisodeItem> episodes,
                                   EpisodesListResponse.EpisodeItem currentEpisode,
                                   OnEpisodeSelectedListener listener) {
        this.episodes = episodes;
        this.currentEpisode = currentEpisode;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EpisodeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_horizontal_episode, parent, false);
        return new EpisodeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EpisodeViewHolder holder, int position) {
        EpisodesListResponse.EpisodeItem episode = episodes.get(position);

        // Set episode number + "серия"
        String episodeText = episode.getNumber() + " серия";
        holder.episodeText.setText(episodeText);

        // Check if this is the current episode
        boolean isCurrentEpisode = false;
        if (currentEpisode != null) {
            // Try ID first
            if (currentEpisode.getId() == episode.getId()) {
                isCurrentEpisode = true;
            } else {
                // Compare numbers (string or numeric-equivalent)
                String a = currentEpisode.getNumber();
                String b = episode.getNumber();
                if (a != null && b != null) {
                    if (a.equals(b)) {
                        isCurrentEpisode = true;
                    } else {
                        try {
                            int ai = Integer.parseInt(a.trim());
                            int bi = Integer.parseInt(b.trim());
                            isCurrentEpisode = ai == bi;
                        } catch (Exception ignore) {
                        }
                    }
                }
            }
        }
        
        // Debug logging
        android.util.Log.d("EpisodesAdapter", "Episode " + episode.getNumber() + 
            " (ID: " + episode.getId() + ") at position " + position + 
            ": isCurrentEpisode=" + isCurrentEpisode + 
            ", currentEpisode=" + (currentEpisode != null ? 
                currentEpisode.getNumber() + " (ID: " + currentEpisode.getId() + ")" : "null"));

        // Set selected state and colors
        holder.itemView.setSelected(isCurrentEpisode);

        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = holder.itemView.getContext().getTheme();

        if (isCurrentEpisode) {
            theme.resolveAttribute(R.attr.secondaryTextColor, typedValue, true);
            holder.episodeText.setTextColor(typedValue.data);
            holder.itemView.setBackgroundResource(R.drawable.episode_item_selected);
        } else {
            holder.episodeText.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.gray_color));

        }

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEpisodeSelected(episode);
            }
        });
    }

    @Override
    public int getItemCount() {
        return episodes != null ? episodes.size() : 0;
    }

    public static class EpisodeViewHolder extends RecyclerView.ViewHolder {
        TextView episodeText;

        EpisodeViewHolder(@NonNull View itemView) {
            super(itemView);
            episodeText = itemView.findViewById(R.id.episodeText);
        }
    }
}
