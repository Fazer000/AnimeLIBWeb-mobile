package com.example.animelib.models;

import java.util.Map;

public class KodikResponse {
    private boolean success;
    private Map<String, VideoQuality[]> data;

    public KodikResponse() {}

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Map<String, VideoQuality[]> getData() {
        return data;
    }

    public void setData(Map<String, VideoQuality[]> data) {
        this.data = data;
    }

    public static class VideoQuality {
        private String src;
        private String type;

        public VideoQuality() {}

        public String getSrc() {
            return src;
        }

        public void setSrc(String src) {
            this.src = src;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
}
