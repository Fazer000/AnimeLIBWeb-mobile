package com.example.animelib.models;

import com.google.gson.annotations.SerializedName;

public class AnimeBookmarkResponse {
    private BookmarkData data;

    public AnimeBookmarkResponse() {}

    public BookmarkData getData() {
        return data;
    }

    public void setData(BookmarkData data) {
        this.data = data;
    }

    public static class BookmarkData {
        private int id;
        private String type;
        @SerializedName("media_id")
        private int mediaId;
        @SerializedName("item_id")
        private int itemId;
        private String progress;
        private int status;
        @SerializedName("created_at")
        private String createdAt;
        @SerializedName("updated_at")
        private String updatedAt;
        private MetaData meta;
        private ItemData item;

        public BookmarkData() {}

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public int getMediaId() {
            return mediaId;
        }

        public void setMediaId(int mediaId) {
            this.mediaId = mediaId;
        }

        public int getItemId() {
            return itemId;
        }

        public void setItemId(int itemId) {
            this.itemId = itemId;
        }

        public String getProgress() {
            return progress;
        }

        public void setProgress(String progress) {
            this.progress = progress;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }

        public String getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(String updatedAt) {
            this.updatedAt = updatedAt;
        }

        public MetaData getMeta() {
            return meta;
        }

        public void setMeta(MetaData meta) {
            this.meta = meta;
        }

        public ItemData getItem() {
            return item;
        }

        public void setItem(ItemData item) {
            this.item = item;
        }
    }

    public static class MetaData {
        @SerializedName("item_number")
        private int itemNumber;
        private int rewatches;
        @SerializedName("rewatches_history")
        private java.util.List<String> rewatchesHistory;
        private String comment;
        private String player;
        private int team;
        @SerializedName("translation_type")
        private int translationType;
        private int notes;

        public MetaData() {}

        public int getItemNumber() {
            return itemNumber;
        }

        public void setItemNumber(int itemNumber) {
            this.itemNumber = itemNumber;
        }

        public int getRewatches() {
            return rewatches;
        }

        public void setRewatches(int rewatches) {
            this.rewatches = rewatches;
        }

        public java.util.List<String> getRewatchesHistory() {
            return rewatchesHistory;
        }

        public void setRewatchesHistory(java.util.List<String> rewatchesHistory) {
            this.rewatchesHistory = rewatchesHistory;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public String getPlayer() {
            return player;
        }

        public void setPlayer(String player) {
            this.player = player;
        }

        public int getTeam() {
            return team;
        }

        public void setTeam(int team) {
            this.team = team;
        }

        public int getTranslationType() {
            return translationType;
        }

        public void setTranslationType(int translationType) {
            this.translationType = translationType;
        }

        public int getNotes() {
            return notes;
        }

        public void setNotes(int notes) {
            this.notes = notes;
        }
    }

    public static class ItemData {
        private int id;
        private String model;
        private String name;
        private String number;
        @SerializedName("number_secondary")
        private String numberSecondary;
        private String season;
        @SerializedName("anime_id")
        private Integer animeId;
        @SerializedName("created_at")
        private String createdAt;
        private String type;

        public ItemData() {}

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getNumber() {
            return number;
        }

        public void setNumber(String number) {
            this.number = number;
        }

        public String getNumberSecondary() {
            return numberSecondary;
        }

        public void setNumberSecondary(String numberSecondary) {
            this.numberSecondary = numberSecondary;
        }

        public String getSeason() {
            return season;
        }

        public void setSeason(String season) {
            this.season = season;
        }

        public Integer getAnimeId() {
            return animeId;
        }

        public void setAnimeId(Integer animeId) {
            this.animeId = animeId;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
}
