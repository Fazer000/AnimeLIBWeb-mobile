package com.example.animelib.adapters;

import android.text.Html;
import android.text.Spanned;
import android.util.Log;
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
import com.example.animelib.util.CommentHtmlProcessor;

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

        // Используем новый HTML процессор для обработки комментария
        CommentHtmlProcessor processor = new CommentHtmlProcessor(holder.itemView.getContext());
        
        // Добавляем логирование для отладки
        Log.d("CommentsAdapter", "Comment text: " + commentText);
        Log.d("CommentsAdapter", "Contains spoiler-node: " + commentText.contains("spoiler-node"));
        Log.d("CommentsAdapter", "Contains blockquote: " + commentText.contains("<blockquote>"));
        Log.d("CommentsAdapter", "Contains strong: " + commentText.contains("<strong>"));
        
        // Проверяем есть ли спойлеры или сложное форматирование
        if (commentText.contains("spoiler-node") || 
            commentText.contains("<blockquote>") || 
            commentText.contains("<strong>") || 
            commentText.contains("<em>") || 
            commentText.contains("<u>") || 
            commentText.contains("<strike>")) {
            
            // Используем контейнер для сложного форматирования
            holder.commentHtmlView.setVisibility(View.GONE);
            holder.spoilerContainer.setVisibility(View.VISIBLE);
            
            // Обрабатываем HTML с помощью нового процессора
            processor.processCommentHtml(commentText, holder.spoilerContainer);
        } else {
            // Простое форматирование - используем обычный TextView
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