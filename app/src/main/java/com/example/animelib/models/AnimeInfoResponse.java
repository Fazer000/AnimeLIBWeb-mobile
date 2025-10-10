package com.example.animelib.models;

import com.google.gson.annotations.SerializedName;

public class AnimeInfoResponse {
    private Data data;

    public Data getData() { return data; }

    public static class Data {
        private int id;
        private String name;
        private String rus_name;
        private String eng_name;
        private String slug_url;
        private Cover cover;
        private Type type;
        private Status status;
        private String releaseDate;
        private String releaseDateString;
        private Rating rating;
        private AgeRestriction ageRestriction;
        private ItemsCount items_count;
        private String shikimori_href;
        private double shiki_rate;

        public int getId() { return id; }
        public String getName() { return name; }
        public String getRus_name() { return rus_name; }
        public String getEng_name() { return eng_name; }
        public String getSlug_url() { return slug_url; }
        public Cover getCover() { return cover; }
        public Type getType() { return type; }
        public Status getStatus() { return status; }
        public String getReleaseDate() { return releaseDate; }
        public String getReleaseDateString() { return releaseDateString; }
        public Rating getRating() { return rating; }
        public AgeRestriction getAgeRestriction() { return ageRestriction; }
        public ItemsCount getItems_count() { return items_count; }
        public String getShikimori_href() { return shikimori_href; }
        public double getShiki_rate() { return shiki_rate; }
    }

    public static class Cover {
        private String filename;
        private String thumbnail;
        
        @SerializedName("default")
        private String defaultUrl; // "default" is a keyword in Java
        
        private String md;

        public String getFilename() { return filename; }
        public String getThumbnail() { return thumbnail; }
        public String getDefaultUrl() { return defaultUrl; }
        public String getMd() { return md; }
    }

    public static class Type {
        private int id;
        private String label;

        public int getId() { return id; }
        public String getLabel() { return label; }
    }

    public static class Status {
        private int id;
        private String label;

        public int getId() { return id; }
        public String getLabel() { return label; }
    }

    public static class Rating {
        private String average;
        
        @SerializedName("averageFormated")
        private String averageFormated;
        
        private int votes;
        
        @SerializedName("votesFormated")
        private String votesFormated;

        public String getAverage() { return average; }
        public String getAverageFormated() { return averageFormated; }
        public int getVotes() { return votes; }
        public String getVotesFormated() { return votesFormated; }
    }

    public static class AgeRestriction {
        private int id;
        private String label;

        public int getId() { return id; }
        public String getLabel() { return label; }
    }

    public static class ItemsCount {
        private int uploaded;
        private int total;

        public int getUploaded() { return uploaded; }
        public int getTotal() { return total; }
    }
}


