package com.example.animelib.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class BookmarksListResponse {
    private List<BookmarkItem> data;
    private Links links;
    private Meta meta;

    public BookmarksListResponse() {}

    public List<BookmarkItem> getData() {
        return data;
    }

    public void setData(List<BookmarkItem> data) {
        this.data = data;
    }

    public Links getLinks() {
        return links;
    }

    public void setLinks(Links links) {
        this.links = links;
    }

    public Meta getMeta() {
        return meta;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }

    public static class BookmarkItem {
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
        private MediaData media;
        private ItemData item;

        public BookmarkItem() {}

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

        public MediaData getMedia() {
            return media;
        }

        public void setMedia(MediaData media) {
            this.media = media;
        }

        public ItemData getItem() {
            return item;
        }

        public void setItem(ItemData item) {
            this.item = item;
        }
    }

    public static class MediaData {
        private int id;
        private String name;
        @SerializedName("rus_name")
        private String rusName;
        @SerializedName("eng_name")
        private String engName;
        private String model;
        private String slug;
        @SerializedName("slug_url")
        private String slugUrl;
        private CoverData cover;
        private TypeData type;
        @SerializedName("items_count")
        private ItemsCount itemsCount;

        public MediaData() {}

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getRusName() {
            return rusName;
        }

        public void setRusName(String rusName) {
            this.rusName = rusName;
        }

        public String getEngName() {
            return engName;
        }

        public void setEngName(String engName) {
            this.engName = engName;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getSlug() {
            return slug;
        }

        public void setSlug(String slug) {
            this.slug = slug;
        }

        public String getSlugUrl() {
            return slugUrl;
        }

        public void setSlugUrl(String slugUrl) {
            this.slugUrl = slugUrl;
        }

        public CoverData getCover() {
            return cover;
        }

        public void setCover(CoverData cover) {
            this.cover = cover;
        }

        public TypeData getType() {
            return type;
        }

        public void setType(TypeData type) {
            this.type = type;
        }

        public ItemsCount getItemsCount() {
            return itemsCount;
        }

        public void setItemsCount(ItemsCount itemsCount) {
            this.itemsCount = itemsCount;
        }
    }

    public static class CoverData {
        private String filename;
        private String thumbnail;
        private String default_;
        private String md;

        public CoverData() {}

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public String getThumbnail() {
            return thumbnail;
        }

        public void setThumbnail(String thumbnail) {
            this.thumbnail = thumbnail;
        }

        public String getDefault() {
            return default_;
        }

        public void setDefault(String default_) {
            this.default_ = default_;
        }

        public String getMd() {
            return md;
        }

        public void setMd(String md) {
            this.md = md;
        }
    }

    public static class TypeData {
        private int id;
        private String label;

        public TypeData() {}

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }

    public static class ItemsCount {
        private int uploaded;
        private Integer total;

        public ItemsCount() {}

        public int getUploaded() {
            return uploaded;
        }

        public void setUploaded(int uploaded) {
            this.uploaded = uploaded;
        }

        public Integer getTotal() {
            return total;
        }

        public void setTotal(Integer total) {
            this.total = total;
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

    public static class Links {
        private String first;
        private String last;
        private String prev;
        private String next;

        public Links() {}

        public String getFirst() {
            return first;
        }

        public void setFirst(String first) {
            this.first = first;
        }

        public String getLast() {
            return last;
        }

        public void setLast(String last) {
            this.last = last;
        }

        public String getPrev() {
            return prev;
        }

        public void setPrev(String prev) {
            this.prev = prev;
        }

        public String getNext() {
            return next;
        }

        public void setNext(String next) {
            this.next = next;
        }
    }

    public static class Meta {
        @SerializedName("current_page")
        private int currentPage;
        private int from;
        private String path;
        @SerializedName("per_page")
        private List<Integer> perPage;
        private int to;
        private int page;
        @SerializedName("next_page_url")
        private boolean nextPageUrl;

        public Meta() {}

        public int getCurrentPage() {
            return currentPage;
        }

        public void setCurrentPage(int currentPage) {
            this.currentPage = currentPage;
        }

        public int getFrom() {
            return from;
        }

        public void setFrom(int from) {
            this.from = from;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public List<Integer> getPerPage() {
            return perPage;
        }

        public void setPerPage(List<Integer> perPage) {
            this.perPage = perPage;
        }

        public int getTo() {
            return to;
        }

        public void setTo(int to) {
            this.to = to;
        }

        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = page;
        }

        public boolean isNextPageUrl() {
            return nextPageUrl;
        }

        public void setNextPageUrl(boolean nextPageUrl) {
            this.nextPageUrl = nextPageUrl;
        }
    }
}
