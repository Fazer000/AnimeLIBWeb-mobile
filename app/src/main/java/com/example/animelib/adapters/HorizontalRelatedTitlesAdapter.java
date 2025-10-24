package com.example.animelib.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.animelib.R;
import com.example.animelib.models.RelatedTitlesResponse;
import com.example.animelib.util.ImageLoader;

import java.util.List;

/**
 * Адаптер для горизонтальной карусели связанных тайтлов
 */
public class HorizontalRelatedTitlesAdapter extends RecyclerView.Adapter<HorizontalRelatedTitlesAdapter.RelatedTitleViewHolder> {

    private List<RelatedTitlesResponse.RelatedTitle> relatedTitles;
    private final OnRelatedTitleSelectedListener listener;

    public interface OnRelatedTitleSelectedListener {
        /**
         * Вызывается при клике на связанный тайтл
         * @param slugUrl URL тайтла для навигации
         */
        void onRelatedTitleSelected(String slugUrl);
    }

    public HorizontalRelatedTitlesAdapter(List<RelatedTitlesResponse.RelatedTitle> relatedTitles,
                                        OnRelatedTitleSelectedListener listener) {
        this.relatedTitles = filterValidTitles(relatedTitles);
        this.listener = listener;
    }

    @NonNull
    @Override
    public RelatedTitleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_related_title, parent, false);
        return new RelatedTitleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RelatedTitleViewHolder holder, int position) {
        RelatedTitlesResponse.RelatedTitle relatedTitle = relatedTitles.get(position);
        RelatedTitlesResponse.Media media = relatedTitle.getMedia();

        // media не может быть null, т.к. мы фильтруем список в filterValidTitles()
        String relatedType = getRelatedType(relatedTitle);
        holder.relatedTypeText.setText(relatedType);

        // Устанавливаем название (приоритет: русское -> английское -> оригинальное)
        String title = getDisplayTitle(media);
        holder.titleText.setText(title);

        // Устанавливаем тип и статус
        String typeStatus = buildTypeStatusString(media);
        holder.typeStatusText.setText(typeStatus);

        // Загружаем обложку
        loadCoverImage(holder.coverImage, media);

        // Проверяем является ли это аниме
        boolean isAnime = "anime".equalsIgnoreCase(media.getModel());
        String slugUrl = media.getSlugUrl();
        
        android.util.Log.d("HorizontalRelatedAdapter", "Binding item [" + position + "]: " +
                "title=" + title + 
                ", model=" + media.getModel() + 
                ", isAnime=" + isAnime +
                ", slugUrl=" + slugUrl);
        
        // Устанавливаем обработчик клика и внешний вид в зависимости от типа
        if (isAnime && slugUrl != null && !slugUrl.trim().isEmpty()) {
            // Аниме - активный элемент
            holder.itemView.setAlpha(1.0f);
            holder.itemView.setEnabled(true);
            holder.itemView.setOnClickListener(v -> {
                android.util.Log.d("HorizontalRelatedAdapter", "Related anime clicked: " + title + " -> " + slugUrl);
                if (listener != null) {
                    listener.onRelatedTitleSelected(slugUrl);
                }
            });
        } else {
            // Не аниме (манга и т.д.) - неактивный элемент
            android.util.Log.d("HorizontalRelatedAdapter", "Non-anime item (inactive): " + title + ", model=" + media.getModel());
            holder.itemView.setAlpha(0.5f);
            holder.itemView.setEnabled(false);
            holder.itemView.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return relatedTitles != null ? relatedTitles.size() : 0;
    }

    public void updateData(List<RelatedTitlesResponse.RelatedTitle> newRelatedTitles) {
        this.relatedTitles = filterValidTitles(newRelatedTitles);
        notifyDataSetChanged();
    }

    /**
     * Фильтрует список, оставляя только тайтлы с валидным media
     */
    private List<RelatedTitlesResponse.RelatedTitle> filterValidTitles(List<RelatedTitlesResponse.RelatedTitle> titles) {
        if (titles == null) {
            return null;
        }
        
        List<RelatedTitlesResponse.RelatedTitle> filtered = new java.util.ArrayList<>();
        for (RelatedTitlesResponse.RelatedTitle title : titles) {
            if (title != null && title.getMedia() != null) {
                filtered.add(title);
            }
        }
        return filtered;
    }

    private String getRelatedType(RelatedTitlesResponse.RelatedTitle relatedTitle) {
        if (relatedTitle.getRelatedType().getLabel() != null) {
            return relatedTitle.getRelatedType().getLabel();
        } else {
            return "";
        }
    }

    /**
     * Получает отображаемое название тайтла
     */
    private String getDisplayTitle(RelatedTitlesResponse.Media media) {
        if (media.getRusName() != null && !media.getRusName().trim().isEmpty()) {
            return media.getRusName();
        }
        if (media.getEngName() != null && !media.getEngName().trim().isEmpty()) {
            return media.getEngName();
        }
        if (media.getName() != null && !media.getName().trim().isEmpty()) {
            return media.getName();
        }
        return "Без названия";
    }

    /**
     * Строит строку с типом и статусом
     */
    private String buildTypeStatusString(RelatedTitlesResponse.Media media) {
        StringBuilder result = new StringBuilder();

        // Добавляем тип
        if (media.getType() != null && media.getType().getLabel() != null) {
            result.append(media.getType().getLabel());
        }

        // Добавляем статус
        if (media.getStatus() != null && media.getStatus().getLabel() != null) {
            if (result.length() > 0) {
                result.append(" · ");
            }
            result.append(media.getStatus().getLabel());
        }

        return result.toString();
    }

    /**
     * Загружает обложку тайтла
     */
    private void loadCoverImage(ImageView imageView, RelatedTitlesResponse.Media media) {
        if (media.getCover() == null) {
            imageView.setImageResource(R.color.gray_color);
            return;
        }

        String imageUrl = getImageUrl(media);

        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            ImageLoader.getInstance().loadInto(imageView, imageUrl, R.color.gray_color);
        } else {
            imageView.setImageResource(R.color.gray_color);
        }
    }

    @Nullable
    private static String getImageUrl(RelatedTitlesResponse.Media media) {
        String imageUrl = null;
        RelatedTitlesResponse.Cover cover = media.getCover();

        // Приоритет: default -> md -> thumbnail
        if (cover.getDefaultUrl() != null && !cover.getDefaultUrl().trim().isEmpty()) {
            imageUrl = cover.getDefaultUrl();
        } else if (cover.getMd() != null && !cover.getMd().trim().isEmpty()) {
            imageUrl = cover.getMd();
        } else if (cover.getThumbnail() != null && !cover.getThumbnail().trim().isEmpty()) {
            imageUrl = cover.getThumbnail();
        }
        return imageUrl;
    }

    /**
     * ViewHolder для элемента связанного тайтла
     */
    public static class RelatedTitleViewHolder extends RecyclerView.ViewHolder {
        ImageView coverImage;
        TextView titleText;
        TextView typeStatusText;
        TextView relatedTypeText;

        RelatedTitleViewHolder(@NonNull View itemView) {
            super(itemView);
            coverImage = itemView.findViewById(R.id.coverImage);
            titleText = itemView.findViewById(R.id.titleText);
            typeStatusText = itemView.findViewById(R.id.typeStatusText);
            relatedTypeText = itemView.findViewById(R.id.relatedTypeText);
        }
    }
}
