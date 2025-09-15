package com.example.animelib.models;

import java.util.List;

public class CommentsResponse {
    private Data data;
    private Meta meta;

    public Data getData() { return data; }
    public Meta getMeta() { return meta; }

    public static class Data {
        private List<CommentItem> replies;
        private List<CommentItem> root;

        public List<CommentItem> getReplies() { return replies; }
        public List<CommentItem> getRoot() { return root; }
    }

    public static class Meta {
        private boolean has_next_page;
        private int page;
        private int per_page;

        public boolean isHas_next_page() { return has_next_page; }
        public int getPage() { return page; }
        public int getPer_page() { return per_page; }
    }

    public static class CommentItem {
        private long id;
        private Long root_id;
        private Long parent_comment;
        private int comment_level;
        private String post_page;
        private String comment; // HTML строка
        private String created_at;
        private long created_at_ts;
        private String relation_type;
        private long relation_id;
        private User user;
        private Votes votes;

        public long getId() { return id; }
        public Long getRoot_id() { return root_id; }
        public Long getParent_comment() { return parent_comment; }
        public int getComment_level() { return comment_level; }
        public String getPost_page() { return post_page; }
        public String getComment() { return comment; }
        public String getCreated_at() { return created_at; }
        public long getCreated_at_ts() { return created_at_ts; }
        public String getRelation_type() { return relation_type; }
        public long getRelation_id() { return relation_id; }
        public User getUser() { return user; }
        public Votes getVotes() { return votes; }
    }

    public static class User {
        private String username;
        private long id;
        private Avatar avatar;
        private Premium premium;

        public String getUsername() { return username; }
        public long getId() { return id; }
        public Avatar getAvatar() { return avatar; }
        public Premium getPremium() { return premium; }
    }

    public static class Avatar {
        private String filename;
        private String url;

        public String getFilename() { return filename; }
        public String getUrl() { return url; }
    }

    public static class Premium {
        private boolean enabled;

        public boolean isEnabled() { return enabled; }
    }

    public static class Votes {
        private int up;
        private int down;
        private Object user;

        public int getUp() { return up; }
        public int getDown() { return down; }
        public Object getUser() { return user; }
    }
}


