package com.example.animelib.adapters;

import android.annotation.SuppressLint;
import android.util.Log;
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
    private static final String TAG = "PlayerTabsAdapter";

    public interface OnEpisodeSelectedListener {
        void onEpisodeSelected(EpisodesListResponse.EpisodeItem episode);
    }

    private List<EpisodeResponse.PlayerData> animelibPlayers;
    private List<EpisodeResponse.PlayerData> kodikPlayers;
    private EpisodeResponse.PlayerData currentPlayer;
    private final PlayerOptionsAdapter.OnPlayerSelectedListener playerListener;
    
    // Список активных вкладок (только с озвучками)
    private List<String> activeTabs;

    public PlayerTabsAdapter(List<EpisodeResponse.PlayerData> animelibPlayers,
                           List<EpisodeResponse.PlayerData> kodikPlayers,
                           EpisodeResponse.PlayerData currentPlayer,
                           PlayerOptionsAdapter.OnPlayerSelectedListener playerListener) {
        this.animelibPlayers = animelibPlayers;
        this.kodikPlayers = kodikPlayers;
        this.currentPlayer = currentPlayer;
        this.playerListener = playerListener;
        updateActiveTabs();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateData(List<EpisodeResponse.PlayerData> animelibPlayers,
                          List<EpisodeResponse.PlayerData> kodikPlayers,
                          EpisodeResponse.PlayerData currentPlayer) {
        Log.d(TAG, "updateData called - AnimeLib: " + (animelibPlayers != null ? animelibPlayers.size() : "null") + 
              ", Kodik: " + (kodikPlayers != null ? kodikPlayers.size() : "null"));
        
        this.animelibPlayers = animelibPlayers;
        this.kodikPlayers = kodikPlayers;
        this.currentPlayer = currentPlayer;
        updateActiveTabs();
        
        Log.d(TAG, "Active tabs: " + (activeTabs != null ? activeTabs.size() : "null") + 
              ", items: " + (activeTabs != null ? activeTabs : "null"));
        
        notifyDataSetChanged();
    }
    
    /**
     * Обновляет список активных вкладок (только с озвучками)
     */
    private void updateActiveTabs() {
        activeTabs = new java.util.ArrayList<>();
        if (animelibPlayers != null && !animelibPlayers.isEmpty()) {
            activeTabs.add("animelib");
        }
        if (kodikPlayers != null && !kodikPlayers.isEmpty()) {
            activeTabs.add("kodik");
        }
    }
    
    /**
     * Получает тип плеера по позиции
     */
    public String getPlayerTypeAtPosition(int position) {
        if (position >= 0 && position < activeTabs.size()) {
            return activeTabs.get(position);
        }
        return null;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updatePlayers(List<EpisodeResponse.PlayerData> animelibPlayers,
                              List<EpisodeResponse.PlayerData> kodikPlayers,
                              EpisodeResponse.PlayerData currentPlayer) {
        this.animelibPlayers = animelibPlayers;
        this.kodikPlayers = kodikPlayers;
        this.currentPlayer = currentPlayer;
        updateActiveTabs();
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
        String playerType = getPlayerTypeAtPosition(position);
        Log.d(TAG, "onBindViewHolder position=" + position + ", playerType=" + playerType);
        
        if ("animelib".equals(playerType)) {
            Log.d(TAG, "Setting up AnimeLib tab with " + (animelibPlayers != null ? animelibPlayers.size() : "null") + " players");
            setupPlayerTab(holder, animelibPlayers, "animelib");
        } else if ("kodik".equals(playerType)) {
            Log.d(TAG, "Setting up Kodik tab with " + (kodikPlayers != null ? kodikPlayers.size() : "null") + " players");
            setupPlayerTab(holder, kodikPlayers, "kodik");
        }
    }

    @SuppressLint("SetTextI18n")
    private void setupPlayerTab(PlayerTabViewHolder holder,
                                List<EpisodeResponse.PlayerData> players,
                                String playerType) {
        Log.d(TAG, "setupPlayerTab for " + playerType + " with " + (players != null ? players.size() : "null") + " players");
        
        // Setup RecyclerView with LayoutManager
        if (holder.playersRecyclerView.getLayoutManager() == null) {
            Log.d(TAG, "Setting up LayoutManager for " + playerType);
            holder.playersRecyclerView.setLayoutManager(
                new androidx.recyclerview.widget.LinearLayoutManager(holder.itemView.getContext())
            );
        }
        
        PlayerOptionsAdapter adapter = new PlayerOptionsAdapter(players, currentPlayer, playerListener);
        holder.playersRecyclerView.setAdapter(adapter);
        
        Log.d(TAG, "Adapter set for " + playerType + ", adapter itemCount=" + adapter.getItemCount());
    }

    @Override
    public int getItemCount() {
        return activeTabs != null ? activeTabs.size() : 0;
    }

    public static class PlayerTabViewHolder extends RecyclerView.ViewHolder {
        RecyclerView playersRecyclerView;

        PlayerTabViewHolder(@NonNull View itemView) {
            super(itemView);
            playersRecyclerView = itemView.findViewById(R.id.playersRecyclerView);
        }
    }

}
