package com.example.animelib.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Модель ответа API для получения связанных тайтлов
 */
public class RelatedTitlesResponse {
    
    @SerializedName("data")
    private List<RelatedTitle> data;
    
    public List<RelatedTitle> getData() {
        return data;
    }
    
    public void setData(List<RelatedTitle> data) {
        this.data = data;
    }
    
    /**
     * Модель связанного тайтла
     */
    public static class RelatedTitle {
        
        @SerializedName("order")
        private int order;
        
        @SerializedName("related_type")
        private RelatedType relatedType;
        
        @SerializedName("media")
        private Media media;
        
        public int getOrder() {
            return order;
        }
        
        public void setOrder(int order) {
            this.order = order;
        }
        
        public RelatedType getRelatedType() {
            return relatedType;
        }
        
        public void setRelatedType(RelatedType relatedType) {
            this.relatedType = relatedType;
        }
        
        public Media getMedia() {
            return media;
        }
        
        public void setMedia(Media media) {
            this.media = media;
        }
    }
    
    /**
     * Тип связи
     */
    public static class RelatedType {
        
        @SerializedName("id")
        private int id;
        
        @SerializedName("label")
        private String label;
        
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
    
    /**
     * Медиа информация
     */
    public static class Media {
        
        @SerializedName("id")
        private int id;
        
        @SerializedName("name")
        private String name;
        
        @SerializedName("rus_name")
        private String rusName;
        
        @SerializedName("eng_name")
        private String engName;
        
        @SerializedName("model")
        private String model;
        
        @SerializedName("slug")
        private String slug;
        
        @SerializedName("slug_url")
        private String slugUrl;
        
        @SerializedName("cover")
        private Cover cover;
        
        @SerializedName("ageRestriction")
        private AgeRestriction ageRestriction;
        
        @SerializedName("site")
        private int site;
        
        @SerializedName("type")
        private Type type;
        
        @SerializedName("status")
        private Status status;
        
        @SerializedName("releaseDateString")
        private String releaseDateString;
        
        @SerializedName("shiki_rate")
        private Object shikiRate;
        
        // Getters and Setters
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
        
        public Cover getCover() {
            return cover;
        }
        
        public void setCover(Cover cover) {
            this.cover = cover;
        }
        
        public AgeRestriction getAgeRestriction() {
            return ageRestriction;
        }
        
        public void setAgeRestriction(AgeRestriction ageRestriction) {
            this.ageRestriction = ageRestriction;
        }
        
        public int getSite() {
            return site;
        }
        
        public void setSite(int site) {
            this.site = site;
        }
        
        public Type getType() {
            return type;
        }
        
        public void setType(Type type) {
            this.type = type;
        }
        
        public Status getStatus() {
            return status;
        }
        
        public void setStatus(Status status) {
            this.status = status;
        }
        
        public String getReleaseDateString() {
            return releaseDateString;
        }
        
        public void setReleaseDateString(String releaseDateString) {
            this.releaseDateString = releaseDateString;
        }
        
        public Object getShikiRate() {
            return shikiRate;
        }
        
        public void setShikiRate(Object shikiRate) {
            this.shikiRate = shikiRate;
        }
    }
    
    /**
     * Обложка
     */
    public static class Cover {
        
        @SerializedName("filename")
        private String filename;
        
        @SerializedName("thumbnail")
        private String thumbnail;
        
        @SerializedName("default")
        private String defaultUrl;
        
        @SerializedName("md")
        private String md;
        
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
        
        public String getDefaultUrl() {
            return defaultUrl;
        }
        
        public void setDefaultUrl(String defaultUrl) {
            this.defaultUrl = defaultUrl;
        }
        
        public String getMd() {
            return md;
        }
        
        public void setMd(String md) {
            this.md = md;
        }
    }
    
    /**
     * Возрастные ограничения
     */
    public static class AgeRestriction {
        
        @SerializedName("id")
        private int id;
        
        @SerializedName("label")
        private String label;
        
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
    
    /**
     * Тип медиа
     */
    public static class Type {
        
        @SerializedName("id")
        private int id;
        
        @SerializedName("label")
        private String label;
        
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
    
    /**
     * Статус
     */
    public static class Status {
        
        @SerializedName("id")
        private int id;
        
        @SerializedName("label")
        private String label;
        
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
}
