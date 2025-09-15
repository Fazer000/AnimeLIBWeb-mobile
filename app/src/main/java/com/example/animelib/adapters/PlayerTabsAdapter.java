package com.example.animelib.adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.animelib.R;
import com.example.animelib.models.EpisodeResponse;
import com.example.animelib.models.EpisodesListResponse;

import java.util.List;

public class PlayerTabsAdapter extends RecyclerView.Adapter<PlayerTabsAdapter.PlayerTabViewHolder> {

    public interface OnEpisodeSelectedListener {
        void onEpisodeSelected(EpisodesListResponse.EpisodeItem episode);
    }

    private List<EpisodeResponse.PlayerData> animelibPlayers;
    private List<EpisodeResponse.PlayerData> kodikPlayers;
    private EpisodeResponse.PlayerData currentPlayer;
    private final PlayerOptionsAdapter.OnPlayerSelectedListener playerListener;

    public PlayerTabsAdapter(List<EpisodeResponse.PlayerData> animelibPlayers,
                           List<EpisodeResponse.PlayerData> kodikPlayers,
                           EpisodeResponse.PlayerData currentPlayer,
                           PlayerOptionsAdapter.OnPlayerSelectedListener playerListener) {
        this.animelibPlayers = animelibPlayers;
        this.kodikPlayers = kodikPlayers;
        this.currentPlayer = currentPlayer;
        this.playerListener = playerListener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateData(List<EpisodeResponse.PlayerData> animelibPlayers,
                          List<EpisodeResponse.PlayerData> kodikPlayers,
                          EpisodeResponse.PlayerData currentPlayer) {
        this.animelibPlayers = animelibPlayers;
        this.kodikPlayers = kodikPlayers;
        this.currentPlayer = currentPlayer;
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updatePlayers(List<EpisodeResponse.PlayerData> animelibPlayers,
                              List<EpisodeResponse.PlayerData> kodikPlayers,
                              EpisodeResponse.PlayerData currentPlayer) {
        this.animelibPlayers = animelibPlayers;
        this.kodikPlayers = kodikPlayers;
        this.currentPlayer = currentPlayer;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PlayerTabViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.tab_player_options, parent, false);
        return new PlayerTabViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlayerTabViewHolder holder, int position) {
        if (position == 0) {
            // AnimeLib tab
            setupPlayerTab(holder, animelibPlayers, "animelib");
        } else {
            // Kodik tab
            setupPlayerTab(holder, kodikPlayers, "kodik");
        }
    }

    @SuppressLint("SetTextI18n")
    private void setupPlayerTab(PlayerTabViewHolder holder,
                                List<EpisodeResponse.PlayerData> players,
                                String playerType) {
        // Setup RecyclerView
        PlayerOptionsAdapter adapter = new PlayerOptionsAdapter(players, currentPlayer, playerListener);
        holder.playersRecyclerView.setAdapter(adapter);
    }

    @SuppressLint("SetTextI18n")

    @Override
    public int getItemCount() {
        return 2; // Two tabs: AnimeLib and Kodik
    }

    public static class PlayerTabViewHolder extends RecyclerView.ViewHolder {
        RecyclerView playersRecyclerView;

        PlayerTabViewHolder(@NonNull View itemView) {
            super(itemView);
            playersRecyclerView = itemView.findViewById(R.id.playersRecyclerView);
        }
    }

}
