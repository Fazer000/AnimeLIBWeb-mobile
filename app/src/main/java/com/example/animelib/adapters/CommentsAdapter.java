package com.example.animelib.adapters;

import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.animelib.R;
import com.example.animelib.models.CommentsResponse;
import com.example.animelib.util.ImageLoader;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentVH> {

    private static class DisplayItem {
        final CommentsResponse.CommentItem item;
        final int level;
        DisplayItem(CommentsResponse.CommentItem item, int level) { this.item = item; this.level = level; }
    }

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

    // Source storage
    private final Map<Long, CommentsResponse.CommentItem> allById = new HashMap<>();
    private final List<CommentsResponse.CommentItem> roots = new ArrayList<>();
    private final Map<Long, List<CommentsResponse.CommentItem>> childrenByParentId = new HashMap<>();

    // Flattened for RecyclerView
    private final List<DisplayItem> flat = new ArrayList<>();

    public void clearAll() {
        allById.clear();
        roots.clear();
        childrenByParentId.clear();
        flat.clear();
        notifyDataSetChanged();
    }

    public void appendResponse(CommentsResponse response, boolean append) {
        if (!append) clearAll();
        if (response == null || response.getData() == null) {
            rebuildFlat();
            return;
        }

        List<CommentsResponse.CommentItem> newRoots = response.getData().getRoot();
        List<CommentsResponse.CommentItem> replies = response.getData().getReplies();

        if (newRoots != null) {
            for (CommentsResponse.CommentItem r : newRoots) {
                allById.put(r.getId(), r);
                roots.add(r);
            }
        }
        if (replies != null) {
            for (CommentsResponse.CommentItem c : replies) {
                allById.put(c.getId(), c);
                Long parentId = c.getParent_comment();
                if (parentId == null) parentId = c.getRoot_id();
                if (parentId == null) continue;
                List<CommentsResponse.CommentItem> list = childrenByParentId.get(parentId);
                if (list == null) {
                    list = new ArrayList<>();
                    childrenByParentId.put(parentId, list);
                }
                list.add(c);
            }
        }

        rebuildFlat();
    }

    private void rebuildFlat() {
        flat.clear();
        for (CommentsResponse.CommentItem root : roots) {
            addWithChildren(root, 0);
        }
        notifyDataSetChanged();
    }

    private void addWithChildren(CommentsResponse.CommentItem node, int level) {
        flat.add(new DisplayItem(node, level));
        List<CommentsResponse.CommentItem> kids = childrenByParentId.get(node.getId());
        if (kids == null) return;
        for (CommentsResponse.CommentItem child : kids) {
            addWithChildren(child, level + 1);
        }
    }

    @NonNull
    @Override
    public CommentVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentVH holder, int position) {
        DisplayItem di = flat.get(position);
        CommentsResponse.CommentItem item = di.item;

        holder.usernameView.setText(item.getUser() != null ? item.getUser().getUsername() : "");
        String commentText = item.getComment() != null ? item.getComment() : "";
        
        // Обрабатываем спойлеры
        if (commentText.contains("<div class=\"comment__spoiler\">")) {
            holder.commentHtmlView.setVisibility(View.GONE);
            holder.spoilerContainer.setVisibility(View.VISIBLE);
            
            // Парсим спойлеры
            parseAndCreateSpoilers(holder, commentText);
        } else {
            holder.commentHtmlView.setVisibility(View.VISIBLE);
            holder.spoilerContainer.setVisibility(View.GONE);
            
            // Очищаем от лишних переносов строк и пробелов
            commentText = commentText.replaceAll("\\n\\s*\\n", "\n").trim();
            Spanned sp = Html.fromHtml(commentText, Html.FROM_HTML_MODE_LEGACY);
            holder.commentHtmlView.setText(sp);
        }
        holder.dateView.setText(item.getCreated_at_ts() > 0 ? dateFormat.format(new Date(item.getCreated_at_ts())) : "");
        if (item.getVotes() != null) {
            holder.votesView.setText("↑" + item.getVotes().getUp() + "  ↓" + item.getVotes().getDown());
        } else {
            holder.votesView.setText("");
        }

        // Indent by level
        float density = holder.itemView.getResources().getDisplayMetrics().density;
        int basePad = (int) (8 * density);
        int leftPad = (int) (basePad + di.level * 20 * density);
        holder.itemView.setPadding(leftPad, holder.itemView.getPaddingTop(), holder.itemView.getPaddingRight(), holder.itemView.getPaddingBottom());

        String avatarUrl = null;
        if (item.getUser() != null && item.getUser().getAvatar() != null) {
            avatarUrl = item.getUser().getAvatar().getUrl();
        }
        ImageLoader.getInstance().loadInto(holder.avatarView, avatarUrl, R.drawable.ic_avatar_placeholder);
    }
    
    private void parseAndCreateSpoilers(CommentVH holder, String commentText) {
        // Очищаем контейнер спойлеров
        holder.spoilerContainer.removeAllViews();
        
        // Парсим HTML для поиска спойлеров
        String remainingText = commentText;
        
        while (remainingText.contains("<div class=\"comment__spoiler\">")) {
            int startIndex = remainingText.indexOf("<div class=\"comment__spoiler\">");
            int endIndex = remainingText.indexOf("</div>", startIndex);
            
            if (endIndex == -1) break;
            
            // Добавляем текст до спойлера
            String beforeSpoiler = remainingText.substring(0, startIndex).trim();
            if (!beforeSpoiler.isEmpty()) {
                TextView textView = new TextView(holder.itemView.getContext());
                textView.setTextSize(13);
                textView.setTextColor(holder.itemView.getContext().getColor(R.color.white_color));
                Spanned spannedText = Html.fromHtml(beforeSpoiler, Html.FROM_HTML_MODE_LEGACY);
                textView.setText(spannedText);
                holder.spoilerContainer.addView(textView);
            }
            
            // Извлекаем спойлер
            String spoilerHtml = remainingText.substring(startIndex, endIndex + 6);
            
            // Парсим title и text из спойлера
            String title = extractSpoilerTitle(spoilerHtml);
            String text = extractSpoilerText(spoilerHtml);
            
            // Создаем SpoilerView
            com.example.animelib.ui.SpoilerView spoilerView = new com.example.animelib.ui.SpoilerView(holder.itemView.getContext());
            spoilerView.setSpoilerData(title, text);
            holder.spoilerContainer.addView(spoilerView);
            
            // Обновляем remainingText
            remainingText = remainingText.substring(endIndex + 6);
        }
        
        // Добавляем оставшийся текст после последнего спойлера
        remainingText = remainingText.trim();
        if (!remainingText.isEmpty()) {
            TextView textView = new TextView(holder.itemView.getContext());
            textView.setTextSize(13);
            textView.setTextColor(holder.itemView.getContext().getColor(R.color.white_color));
            Spanned spannedText = Html.fromHtml(remainingText, Html.FROM_HTML_MODE_LEGACY);
            textView.setText(spannedText);
            holder.spoilerContainer.addView(textView);
        }
    }
    
    private String extractSpoilerTitle(String spoilerHtml) {
        try {
            int titleStart = spoilerHtml.indexOf("<span class=\"spoiler-title\">");
            if (titleStart == -1) return null;
            
            int titleEnd = spoilerHtml.indexOf("</span>", titleStart);
            if (titleEnd == -1) return null;
            
            String title = spoilerHtml.substring(titleStart + 30, titleEnd); // 30 = length of "<span class=\"spoiler-title\">"
            return Html.fromHtml(title, Html.FROM_HTML_MODE_LEGACY).toString().trim();
        } catch (Exception e) {
            return null;
        }
    }
    
    private String extractSpoilerText(String spoilerHtml) {
        try {
            int textStart = spoilerHtml.indexOf("<span class=\"spoiler-text\">");
            if (textStart == -1) return "";
            
            int textEnd = spoilerHtml.indexOf("</span>", textStart);
            if (textEnd == -1) return "";
            
            String text = spoilerHtml.substring(textStart + 29, textEnd); // 29 = length of "<span class=\"spoiler-text\">"
            return text.trim();
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public int getItemCount() {
        return flat.size();
    }

    public static class CommentVH extends RecyclerView.ViewHolder {
        ImageView avatarView;
        TextView usernameView;
        TextView commentHtmlView;
        LinearLayout spoilerContainer;
        TextView dateView;
        TextView votesView;

        public CommentVH(@NonNull View itemView) {
            super(itemView);
            avatarView = itemView.findViewById(R.id.avatarView);
            usernameView = itemView.findViewById(R.id.usernameView);
            commentHtmlView = itemView.findViewById(R.id.commentHtmlView);
            spoilerContainer = itemView.findViewById(R.id.spoilerContainer);
            dateView = itemView.findViewById(R.id.dateView);
            votesView = itemView.findViewById(R.id.votesView);
        }
    }
}


