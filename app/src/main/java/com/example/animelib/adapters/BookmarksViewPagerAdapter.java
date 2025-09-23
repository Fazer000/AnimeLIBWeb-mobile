package com.example.animelib.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.animelib.R;
import com.example.animelib.models.BookmarksListResponse;
import com.example.animelib.util.ImageLoader;

public class BookmarksViewPagerAdapter extends RecyclerView.Adapter<BookmarksViewPagerAdapter.BookmarkViewHolder> {
    private BookmarksListResponse.BookmarkItem[] bookmarks;
    private OnBookmarkClickListener clickListener;

    public interface OnBookmarkClickListener {
        void onBookmarkClick(BookmarksListResponse.BookmarkItem bookmark);
    }

    public BookmarksViewPagerAdapter(BookmarksListResponse.BookmarkItem[] bookmarks, OnBookmarkClickListener clickListener) {
        this.bookmarks = bookmarks;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public BookmarkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bookmark_wrapper, parent, false);
        return new BookmarkViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookmarkViewHolder holder, int position) {
        BookmarksListResponse.BookmarkItem bookmark = bookmarks[position];
        holder.bind(bookmark);
    }

    @Override
    public int getItemCount() {
        return bookmarks != null ? bookmarks.length : 0;
    }

    class BookmarkViewHolder extends RecyclerView.ViewHolder {
        private ImageView animeCover;
        private TextView animeTitle;
        private TextView episodeInfo;
        private TextView progressInfo;

        public BookmarkViewHolder(@NonNull View itemView) {
            super(itemView);
            // Находим элементы в wrapper layout
            animeCover = itemView.findViewById(R.id.anime_cover);
            animeTitle = itemView.findViewById(R.id.anime_title);
            episodeInfo = itemView.findViewById(R.id.episode_info);
            progressInfo = itemView.findViewById(R.id.progress_info);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && clickListener != null) {
                    clickListener.onBookmarkClick(bookmarks[position]);
                }
            });
        }

        public void bind(BookmarksListResponse.BookmarkItem bookmark) {
            // Устанавливаем название аниме
            if (bookmark.getMedia() != null) {
                String title = bookmark.getMedia().getRusName();
                if (title == null || title.isEmpty()) {
                    title = bookmark.getMedia().getName();
                }
                if (title == null || title.isEmpty()) {
                    title = bookmark.getMedia().getEngName();
                }
                animeTitle.setText(title != null ? title : "Неизвестное аниме");
            } else {
                animeTitle.setText("Неизвестное аниме");
            }

            // Устанавливаем информацию об эпизоде
            if (bookmark.getItem() != null) {
                String episodeText = "Эпизод " + bookmark.getItem().getNumber();
                if (bookmark.getItem().getSeason() != null && !bookmark.getItem().getSeason().isEmpty()) {
                    episodeText += " (Сезон " + bookmark.getItem().getSeason() + ")";
                }
                episodeInfo.setText(episodeText);
            } else {
                episodeInfo.setText("Эпизод неизвестен");
            }

            // Устанавливаем прогресс
            String progress = bookmark.getProgress();
            if (progress != null && !progress.isEmpty()) {
                progressInfo.setText("Прогресс: " + progress);
            } else {
                progressInfo.setText("Прогресс: 00:00");
            }

            // Загружаем обложку
            if (bookmark.getMedia() != null && bookmark.getMedia().getCover() != null) {
                String coverUrl = bookmark.getMedia().getCover().getThumbnail();
                if (coverUrl != null && !coverUrl.isEmpty()) {
                    ImageLoader.getInstance().loadInto(animeCover, coverUrl, R.drawable.placeholder_image);
                } else {
                    animeCover.setImageResource(R.drawable.placeholder_image);
                }
            } else {
                animeCover.setImageResource(R.drawable.placeholder_image);
            }
        }
    }
}
