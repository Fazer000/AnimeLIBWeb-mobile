package com.example.animelib.models;

public class AnimeInfoResponse {
    private Data data;

    public Data getData() { return data; }

    public static class Data {
        private String rus_name;
        private Status status;

        public String getRus_name() { return rus_name; }
        public Status getStatus() { return status; }
    }

    public static class Status {
        private int id;
        private String label;

        public int getId() { return id; }
        public String getLabel() { return label; }
    }
}


