package com.example.animelib.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class EpisodeResponse {
    private EpisodeData data;

    public EpisodeResponse() {}

    public EpisodeData getData() {
        return data;
    }

    public void setData(EpisodeData data) {
        this.data = data;
    }

    public static class EpisodeData {
        private int id;
        private List<PlayerData> players;

        public EpisodeData() {}

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public List<PlayerData> getPlayers() {
            return players;
        }

        public void setPlayers(List<PlayerData> players) {
            this.players = players;
        }
    }

    public static class PlayerData {
        private String player;
        @SerializedName("translation_type")
        private TranslationType translationType;
        private Team team;
        private String src;
        private VideoData video;

        public PlayerData() {}

        public String getPlayer() {
            return player;
        }

        public void setPlayer(String player) {
            this.player = player;
        }

        public TranslationType getTranslationType() {
            return translationType;
        }

        public void setTranslationType(TranslationType translationType) {
            this.translationType = translationType;
        }

        public Team getTeam() {
            return team;
        }

        public void setTeam(Team team) {
            this.team = team;
        }

        public String getSrc() {
            return src;
        }

        public void setSrc(String src) {
            this.src = src;
        }

        public VideoData getVideo() {
            return video;
        }

        public void setVideo(VideoData video) {
            this.video = video;
        }
    }

    public static class TranslationType {
        private int id;
        private String label;

        public TranslationType() {}

        public int getId() { return id; };

        public void setId(int id) { this.id = id; }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }

    public static class Team {
        private int id;
        private String name;

        public Team() {}

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
    }

    public static class VideoData {
        private List<QualityData> quality;

        public VideoData() {}

        public List<QualityData> getQuality() {
            return quality;
        }

        public void setQuality(List<QualityData> quality) {
            this.quality = quality;
        }
    }

    public static class QualityData {
        private String href;
        private int quality;

        public QualityData() {}

        public String getHref() {
            return href;
        }

        public void setHref(String href) {
            this.href = href;
        }

        public int getQuality() {
            return quality;
        }

        public void setQuality(int quality) {
            this.quality = quality;
        }
    }
}
