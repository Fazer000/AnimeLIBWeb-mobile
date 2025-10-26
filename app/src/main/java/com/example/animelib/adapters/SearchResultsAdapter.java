package com.example.animelib.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.animelib.R;
import com.example.animelib.models.SearchResponse;
import com.example.animelib.util.ImageLoader;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

/**
 * Адаптер для отображения результатов поиска аниме
 */
public class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.SearchViewHolder> {
    
    private List<SearchResponse.AnimeSearchItem> items = new ArrayList<>();
    private OnItemClickListener listener;
    
    public interface OnItemClickListener {
        void onItemClick(SearchResponse.AnimeSearchItem item);
        void onItemLongClick(SearchResponse.AnimeSearchItem item);
    }
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
    
    public void setItems(List<SearchResponse.AnimeSearchItem> items) {
        this.items = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    public void clearItems() {
        this.items.clear();
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public SearchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);
        return new SearchViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull SearchViewHolder holder, int position) {
        SearchResponse.AnimeSearchItem item = items.get(position);
        holder.bind(item, listener);
    }
    
    @Override
    public int getItemCount() {
        return items.size();
    }
    
    static class SearchViewHolder extends RecyclerView.ViewHolder {
        private final ImageView cover;
        private final TextView title;
        private final Chip typeChip;
        private final Chip statusChip;
        private final Chip yearChip;
        private final TextView rating;
        private final TextView votes;
        
        public SearchViewHolder(@NonNull View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.searchItemCover);
            title = itemView.findViewById(R.id.searchItemTitle);
            typeChip = itemView.findViewById(R.id.searchItemTypeChip);
            statusChip = itemView.findViewById(R.id.searchItemStatusChip);
            yearChip = itemView.findViewById(R.id.searchItemYearChip);
            rating = itemView.findViewById(R.id.searchItemRating);
            votes = itemView.findViewById(R.id.searchItemVotes);
        }
        
        public void bind(SearchResponse.AnimeSearchItem item, OnItemClickListener listener) {
            // Title
            if (item.getRusName() != null && !item.getRusName().isEmpty()) {
                title.setText(item.getRusName());
            } else {
                title.setText(item.getName());
            }
            
            // Cover
            if (item.getCover() != null && item.getCover().getThumbnail() != null) {
                ImageLoader.getInstance().loadInto(cover, item.getCover().getThumbnail(), R.drawable.ic_logo_non_back_light);
            }
            
            // Type chip
            if (item.getType() != null && item.getType().getLabel() != null) {
                typeChip.setText(item.getType().getLabel());
                typeChip.setVisibility(View.VISIBLE);
            } else {
                typeChip.setVisibility(View.GONE);
            }
            
            // Status chip
            if (item.getStatus() != null && item.getStatus().getLabel() != null) {
                statusChip.setText(item.getStatus().getLabel());
                statusChip.setVisibility(View.VISIBLE);
            } else {
                statusChip.setVisibility(View.GONE);
            }
            
            // Year chip
            if (item.getReleaseDate() != null && !item.getReleaseDate().isEmpty()) {
                String year = item.getReleaseDate().split("-")[0];
                yearChip.setText(year);
                yearChip.setVisibility(View.VISIBLE);
            } else {
                yearChip.setVisibility(View.GONE);
            }
            
            // Rating
            if (item.getRating() != null) {
                rating.setText(item.getRating().getAverageFormated());
                votes.setText("(" + item.getRating().getVotesFormated() + ")");
            } else {
                rating.setText("—");
                votes.setText("");
            }
            
            // Click listener - открывает страницу аниме в WebView
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                }
            });
            
            // Long click listener - открывает VideoPlayerActivity
            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onItemLongClick(item);
                    return true;
                }
                return false;
            });
        }
    }
}

