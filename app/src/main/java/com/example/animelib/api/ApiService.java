package com.example.animelib.api;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.animelib.data.AppSettings;
import com.example.animelib.models.*;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;

public class ApiService {
    private static final String CDNLIBS_BEARER_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJhdWQiOiIxIiwianRpIjoiOTZkYjliMDI4NGM0OWQ1Yzc2NTIxMzkxZTRlNDJkNjAwNTFmMDUzMDU2NjBjZGQzYTRjYmEzN2FjMmRmYTZhNjEyM2VmNDgxZDBjMGU0Y2MiLCJpYXQiOjE3NTc0MzEyNDcuOTg2MjE5LCJuYmYiOjE3NTc0MzEyNDcuOTg2MjIxLCJleHAiOjE3NjAwMjMyNDcuOTgyNTM3LCJzdWIiOiI5NDM5MzIxIiwic2NvcGVzIjpbXX0.FG2bBdeF0328Prrsr9Q_SL-VkQyeJMqE9b9uQ1E74JsCnJPveeMMLYNuJt_cTp5XpkvFK3XHltfCM7wi4Gg-x3rlpG-sTELMaoMNWv-4TmNcQbrKwSnTSVJfUFlnguVA7kpGHBgfAaL3NVKSwu_Pu1xqq6UwqpV9hBSJ6iTHG7T3vz7e_HxhGWQ7AZ47xmoo76aOnWQ2vIceF-zq6gF0peKBsHXuG8Prl-88xyltkT2SSnAJrTl4xmPQsM0F0OntkkFZGU6XPdFwXw-orxvtpCfsv556ra5fdbACMjqfZ3euwqXEHGRtkjMJpmku1-sV_xubQvCgbwuO8WRc-ukuWv3x2WTffkXypFKviEdNTXLBFki5ex4sblvaYhDUd4IrZwIjL-GRPQ9_X6WZITz7Lic5faKs1kr3mxXDSuK7u7tC2WSCom_I_CYR9_aIytJ_XkxixG-aa3LP9-jaOn0n7iZS8XNjaIlLHyqr2Of9wPvJ-A1NVv41EeaptXWs7VcSWg42-fUkofNyS2Qn1Qdo9DzVKmqzO9jMpe-8suwBVGl3gpr4nCwn4J8tIKOTzWX--xHkotH5w1TYaQAtzKs6ocyptylNdAD8WRm_FU3E3pdY5Ecarem7SK8ij5rh724GMiBXN9y9s6jBSwPoIAD9W-R4UoXo1mhsRNGiJ4EkC0U";

    public interface EpisodesCallback {
        void onEpisodesReceived(EpisodesListResponse response);
        void onError(String error);
    }

    public interface EpisodeDataCallback {
        void onEpisodeDataReceived(EpisodeResponse response);
        void onError(String error);
    }

    public interface KodikVideoCallback {
        void onKodikVideoReceived(KodikResponse response);
        void onError(String error);
    }
    
    public interface AnimeInfoCallback {
        void onAnimeInfoReceived(AnimeInfoResponse response);
        void onError(String error);
    }
    
    public interface EpisodeCommentsCallback {
        void onCommentsReceived(CommentsResponse response);
        void onError(String error);
    }

    public interface CurrentEpisodeCallback {
        void onCurrentEpisodeReceived(EpisodesListResponse.EpisodeItem episode);
        void onError(String error);
    }

    public interface ToastCheckCallback {
        void onToastReceived(String message, String newUrl);
        void onError(String error);
    }

    private final OkHttpClient httpClient;
    private final Gson gson;
    private final ExecutorService executor;
    private final Context context;

    // Room DB (uses existing data/ AppDatabase)
    private final com.example.animelib.data.AppDatabase db;

    public ApiService(Context context) {
        this.context = context.getApplicationContext();
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();
        this.executor = Executors.newSingleThreadExecutor();
        this.db = com.example.animelib.data.AppDatabase.getDatabase(this.context);
    }

    private Request.Builder buildApiRequest(String url) {
        // Получаем URL из базы данных
        String siteUrl = getSiteUrlFromDb();
        
        return new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + CDNLIBS_BEARER_TOKEN)
                .addHeader("Accept", "*/*")
                .addHeader("Accept-Language", "ru,en;q=0.9,de;q=0.8,zh;q=0.7")
                .addHeader("Content-Type", "application/json")
                .addHeader("Origin", siteUrl)
                .addHeader("Referer", siteUrl + "/")
                .addHeader("Sec-Ch-Ua", "\"Google Chrome\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"")
                .addHeader("Sec-Ch-Ua-Mobile", "?1")
                .addHeader("Sec-Ch-Ua-Platform", "\"Android\"")
                .addHeader("Sec-Fetch-Dest", "empty")
                .addHeader("Sec-Fetch-Mode", "cors")
                .addHeader("Sec-Fetch-Site", "cross-site")
                .addHeader("Site-Id", "5")
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14; SM-G998B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36")
                .addHeader("Client-Time-Zone", "Europe/Samara")
                .addHeader("Priority", "u=1, i");
    }

    private String getSiteUrlFromDb() {
        try {
            AppSettings settings = db.appSettingsDao().getSettingsSync();
            if (settings != null && settings.getSiteUrl() != null) {
                String url = settings.getSiteUrl();
                // Убираем trailing slash если есть
                if (url.endsWith("/")) {
                    url = url.substring(0, url.length() - 1);
                }
                // Добавляем https:// если нет протокола
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://" + url;
                }
                return url;
            }
        } catch (Exception e) {
            Log.e("AnimeApiService", "Failed to get site URL from DB", e);
        }
        // Fallback на дефолтный URL
        return "https://v3.animelib.org";
    }

    public void fetchAnimeInfo(String animeSlugOrId, AnimeInfoCallback callback) {
        executor.execute(() -> {
            try {
                String apiUrl = "https://api.cdnlibs.org/api/anime/" + animeSlugOrId;
                Request request = buildApiRequest(apiUrl).build();

                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        callback.onError("Ошибка сети: " + e.getMessage());
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        if (!response.isSuccessful()) {
                            callback.onError("HTTP " + response.code());
                            return;
                        }
                        assert response.body() != null;
                        String body = response.body().string();
                        try {
                            AnimeInfoResponse info = gson.fromJson(body, AnimeInfoResponse.class);
                            callback.onAnimeInfoReceived(info);
                        } catch (Exception ex) {
                            callback.onError("Ошибка парсинга");
                        }
                    }
                });
            } catch (Exception e) {
                callback.onError("Ошибка запроса: " + e.getMessage());
            }
        });
    }

    public void fetchEpisodesList(String animeId, EpisodesCallback callback) {
        Log.d("AnimeApiService", "fetchEpisodesList called with animeId: " + animeId);
        executor.execute(() -> {
            try {
                String apiUrl = "https://api.cdnlibs.org/api/episodes?anime_id=" + animeId;
                Log.d("AnimeApiService", "Fetching episodes list for anime_id: " + animeId);

                Request request = buildApiRequest(apiUrl).build();

                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Log.e("AnimeApiService", "Episodes list request failed", e);
                        callback.onError("Ошибка загрузки эпизодов: " + e.getMessage());
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) {
                        try (response) {
                            if (response.isSuccessful()) {
                                assert response.body() != null;
                                String responseBody = response.body().string();
                                Log.d("AnimeApiService", "Episodes list response received");

                                EpisodesListResponse episodesResponse = gson.fromJson(responseBody, EpisodesListResponse.class);
                                if (episodesResponse != null && episodesResponse.getData() != null) {
                                    callback.onEpisodesReceived(episodesResponse);
                                } else {
                                    callback.onError("Неверный формат ответа эпизодов");
                                }
                            } else {
                                callback.onError("HTTP ошибка: " + response.code());
                            }
                        } catch (Exception e) {
                            Log.e("AnimeApiService", "Error processing episodes response", e);
                            callback.onError("Ошибка обработки данных эпизодов: " + e.getMessage());
                        }
                    }
                });
            } catch (Exception e) {
                Log.e("AnimeApiService", "Error in episodes API call", e);
                callback.onError("Ошибка выполнения запроса: " + e.getMessage());
            }
        });
    }

    public void fetchEpisodeData(int episodeId, EpisodeDataCallback callback) {
        executor.execute(() -> {
            try {
                String apiUrl = "https://api.cdnlibs.org/api/episodes/" + episodeId;
                Log.d("AnimeApiService", "Fetching episode data for episode_id: " + episodeId);

                Request request = buildApiRequest(apiUrl).build();

                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Log.e("AnimeApiService", "Episode data request failed", e);
                        callback.onError("Ошибка загрузки данных эпизода: " + e.getMessage());
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) {
                        try (response) {
                            if (response.isSuccessful()) {
                                assert response.body() != null;
                                String responseBody = response.body().string();
                                Log.d("AnimeApiService", "Episode data response received");

                                EpisodeResponse episodeResponse = gson.fromJson(responseBody, EpisodeResponse.class);
                                if (episodeResponse != null && episodeResponse.getData() != null) {
                                    callback.onEpisodeDataReceived(episodeResponse);
                                } else {
                                    callback.onError("Неверный формат ответа эпизода");
                                }
                            } else {
                                callback.onError("HTTP ошибка: " + response.code());
                            }
                        } catch (Exception e) {
                            Log.e("AnimeApiService", "Error processing episode data response", e);
                            callback.onError("Ошибка обработки данных эпизода: " + e.getMessage());
                        }
                    }
                });
            } catch (Exception e) {
                Log.e("AnimeApiService", "Error in episode data API call", e);
                callback.onError("Ошибка выполнения запроса: " + e.getMessage());
            }
        });
    }

    public void fetchKodikVideoLinks(String kodikSrc, KodikVideoCallback callback) {
        executor.execute(() -> {
            try {
                String apiUrl = "https://anilib-kodik-api.burntv.ru/api/video-links?link=" + kodikSrc;
                Log.d("AnimeApiService", "Fetching Kodik video links for src: " + kodikSrc);

                Request request = new Request.Builder()
                        .url(apiUrl)
                        .addHeader("Authorization", "Bearer " + CDNLIBS_BEARER_TOKEN)
                        .build();

                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Log.e("AnimeApiService", "Kodik video links request failed", e);
                        callback.onError("Ошибка загрузки HLS ссылок: " + e.getMessage());
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) {
                        try (response) {
                            if (response.isSuccessful()) {
                                assert response.body() != null;
                                String responseBody = response.body().string();
                                Log.d("AnimeApiService", "Kodik video links response received");

                                KodikResponse kodikResponse = gson.fromJson(responseBody, KodikResponse.class);
                                if (kodikResponse != null && kodikResponse.isSuccess() && kodikResponse.getData() != null) {
                                    callback.onKodikVideoReceived(kodikResponse);
                                } else {
                                    callback.onError("Неверный формат ответа Kodik");
                                }
                            } else {
                                callback.onError("HTTP ошибка: " + response.code());
                            }
                        } catch (Exception e) {
                            Log.e("AnimeApiService", "Error processing Kodik response", e);
                            callback.onError("Ошибка обработки HLS данных: " + e.getMessage());
                        }
                    }
                });
            } catch (Exception e) {
                Log.e("AnimeApiService", "Error in Kodik API call", e);
                callback.onError("Ошибка выполнения запроса: " + e.getMessage());
            }
        });
    }

    public void fetchEpisodeComments(long episodeId, String sortType, EpisodeCommentsCallback callback) {
        fetchEpisodeComments(episodeId, sortType, 1, callback);
    }

    public void fetchEpisodeComments(long episodeId, String sortType, int page, EpisodeCommentsCallback callback) {
        executor.execute(() -> {
            try {
                String apiUrl = getSort(episodeId, sortType, page);

                Request request = buildApiRequest(apiUrl).build();

                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        callback.onError("Ошибка сети: " + e.getMessage());
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        if (!response.isSuccessful()) {
                            callback.onError("HTTP " + response.code());
                            return;
                        }
                        assert response.body() != null;
                        String body = response.body().string();
                        try {
                            CommentsResponse comments = gson.fromJson(body, CommentsResponse.class);
                            callback.onCommentsReceived(comments);
                        } catch (Exception ex) {
                            callback.onError("Ошибка парсинга");
                        }
                    }
                });
            } catch (Exception e) {
                callback.onError("Ошибка запроса: " + e.getMessage());
            }
        });
    }

    @NonNull
    private static String getSort(long episodeId, String sortType, int page) {
        String safeSort = (sortType == null || sortType.isEmpty()) ? "desc" : sortType;
        int safePage = Math.max(1, page);
        // API: для популярного нужна сортировка по полю голосов
        boolean byVotes = "votes_up".equalsIgnoreCase(safeSort);
        String sortBy = byVotes ? "votes_up" : "id";
        String sortDir = byVotes ? "desc" : safeSort;
        return "https://api.cdnlibs.org/api/comments?page=" + safePage +
                "&post_id=" + episodeId +
                "&post_type=episodes&sort_by=" + sortBy + "&sort_type=" + sortDir;
    }

    /**
     * Creates OkHttpClient with disabled SSL verification for domains with certificate issues
     */
    private OkHttpClient getUnsafeOkHttpClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            @SuppressLint("CustomX509TrustManager") final TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    @SuppressLint("TrustAllX509TrustManager")
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    @SuppressLint("TrustAllX509TrustManager")
                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }
                }
            };

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager)trustAllCerts[0]);
            builder.hostnameVerifier(new HostnameVerifier() {
                @SuppressLint("BadHostnameVerifier")
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });

            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Extract anime_id from URL like "/ru/anime/23875--kanojo-okarishimasu-4th-season-anime/watch"
     */
    public String extractAnimeId(String url) {
        try {
            String[] parts = url.split("/");
            for (String part : parts) {
                if (part.contains("--")) {
                    return part.split("--")[0];
                }
            }
        } catch (Exception e) {
            Log.e("AnimeApiService", "Error extracting anime_id from URL: " + url, e);
        }
        return null;
    }

    /**
     * Extract full anime slug segment e.g. "24653--sakamoto-days-part-2-anime" from URL
     */
    public String extractAnimeSlug(String url) {
        try {
            String[] parts = url.split("/");
            for (String part : parts) {
                if (part.contains("--")) {
                    return part; // return full slug with id and name
                }
            }
        } catch (Exception e) {
            Log.e("AnimeApiService", "Error extracting anime slug from URL: " + url, e);
        }
        return null;
    }

    /**
     * Load episodes for anime and get first episode data
     */
    public void loadAnimeFromUrl(String animeUrl, EpisodeDataCallback callback) {
        String animeId = extractAnimeId(animeUrl);
        if (animeId == null) {
            callback.onError("Не удалось извлечь ID аниме из URL");
            return;
        }

        Log.d("AnimeApiService", "Extracted anime_id: " + animeId + " from URL: " + animeUrl);

        executor.execute(() -> {
            try {
                String apiUrl = "https://api.cdnlibs.org/api/episodes?anime_id=" + animeId;
                Log.d("AnimeApiService", "Making direct API request to: " + apiUrl);

                Request request = buildApiRequest(apiUrl).build();

                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Log.e("AnimeApiService", "Direct API request failed", e);
                        callback.onError("Ошибка API: " + e.getMessage());
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) {
                        try (response) {
                            if (response.isSuccessful()) {
                                assert response.body() != null;
                                String responseBody = response.body().string();
                                Log.d("AnimeApiService", "Direct API response: " + responseBody);

                                EpisodesListResponse episodesResponse = gson.fromJson(responseBody, EpisodesListResponse.class);
                                if (episodesResponse != null && episodesResponse.getData() != null && !episodesResponse.getData().isEmpty()) {
                                    // Select first episode (index 0)
                                    int episodeId = episodesResponse.getData().get(0).getId();
                                    Log.d("AnimeApiService", "Selected episode ID: " + episodeId);
                                    fetchEpisodeData(episodeId, callback);
                                } else {
                                    callback.onError("Эпизоды не найдены");
                                }
                            } else {
                                Log.e("AnimeApiService", "API response not successful: " + response.code());
                                callback.onError("Ошибка API: " + response.code());
                            }
                        } catch (Exception e) {
                            Log.e("AnimeApiService", "Error processing API response", e);
                            callback.onError("Ошибка обработки ответа");
                        }
                    }
                });
            } catch (Exception e) {
                Log.e("AnimeApiService", "Error in API call", e);
                callback.onError("Ошибка запроса");
            }
        });
    }

    /**
     * Fetch Kodik video links using unsafe HTTP client
     */
    public void fetchKodikVideoLinksUnsafe(String kodikSrc, KodikVideoCallback callback) {
        executor.execute(() -> {
            try {
                String apiUrl = "https://anilib-kodik-api.burntv.ru/api/video-links?link=" + kodikSrc;
                Log.d("AnimeApiService", "Making direct API request to: " + apiUrl);

                Request request = new Request.Builder()
                        .url(apiUrl)
                        .addHeader("Authorization", "Bearer " + CDNLIBS_BEARER_TOKEN)
                        .build();

                // Create OkHttpClient with disabled SSL verification for Kodik API
                OkHttpClient kodikClient = getUnsafeOkHttpClient();
                kodikClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Log.e("AnimeApiService", "Direct API request failed", e);
                        callback.onError("Ошибка HLS API: " + e.getMessage());
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) {
                        try (response) {
                            if (response.isSuccessful()) {
                                assert response.body() != null;
                                String responseBody = response.body().string();
                                Log.d("AnimeApiService", "Direct API response: " + responseBody);

                                KodikResponse kodikResponse = gson.fromJson(responseBody, KodikResponse.class);
                                if (kodikResponse != null && kodikResponse.isSuccess() && kodikResponse.getData() != null) {
                                    Log.d("AnimeApiService", "Starting HLS player with Kodik response");
                                    callback.onKodikVideoReceived(kodikResponse);
                                } else {
                                    callback.onError("HLS ссылки недоступны");
                                }
                            } else {
                                Log.e("AnimeApiService", "API response not successful: " + response.code());
                                callback.onError("Ошибка HLS API: " + response.code());
                            }
                        } catch (Exception e) {
                            Log.e("AnimeApiService", "Error processing HLS response", e);
                            callback.onError("Ошибка обработки HLS ответа");
                        }
                    }
                });
            } catch (Exception e) {
                Log.e("AnimeApiService", "Error in HLS API call", e);
                callback.onError("Ошибка HLS запроса");
            }
        });
    }

    /**
     * Сохраняет текущий эпизод для аниме через API-подобный подход
     * Получает список эпизодов из API и находит эпизод по номеру
     */
    public void saveCurrentEpisode(String animeId, String episodeNumber, CurrentEpisodeCallback callback) {
        Log.d("AnimeApiService", "Saving current episode " + episodeNumber + " for anime " + animeId);
        
        // Сначала получаем список эпизодов из API
        fetchEpisodesList(animeId, new EpisodesCallback() {
            @Override
            public void onEpisodesReceived(EpisodesListResponse response) {
                if (response.getData() != null) {
                    // Ищем эпизод по номеру
                    EpisodesListResponse.EpisodeItem foundEpisode = null;
                    for (EpisodesListResponse.EpisodeItem episode : response.getData()) {
                        if (episode.getNumber() != null && episode.getNumber().equals(episodeNumber)) {
                            foundEpisode = episode;
                            break;
                        }
                    }
                    
                    if (foundEpisode != null) {
                        // Сохраняем в Room
                        saveCurrentEpisodeToDb(animeId, foundEpisode);
                        callback.onCurrentEpisodeReceived(foundEpisode);
                        Log.d("AnimeApiService", "Successfully saved episode " + episodeNumber + " for anime " + animeId);
                    } else {
                        callback.onError("Эпизод с номером " + episodeNumber + " не найден");
                    }
                } else {
                    callback.onError("Не удалось получить список эпизодов");
                }
            }
            
            @Override
            public void onError(String error) {
                callback.onError("Ошибка при получении эпизодов: " + error);
            }
        });
    }
    
    /**
     * Загружает текущий эпизод для аниме через API
     * Сначала проверяет локальное хранилище, затем получает актуальные данные из API
     */
    public void loadCurrentEpisode(String animeId, CurrentEpisodeCallback callback) {
        Log.d("AnimeApiService", "Loading current episode for anime " + animeId);
        
        // Получаем список эпизодов из API
        fetchEpisodesList(animeId, new EpisodesCallback() {
            @Override
            public void onEpisodesReceived(EpisodesListResponse response) {
                if (response.getData() != null && !response.getData().isEmpty()) {
                    // Пытаемся загрузить сохраненный эпизод из Room
                    EpisodesListResponse.EpisodeItem savedEpisode = loadCurrentEpisodeFromDb(animeId);
                    if (savedEpisode != null) {
                        // Проверяем, существует ли этот эпизод в актуальном списке
                        for (EpisodesListResponse.EpisodeItem episode : response.getData()) {
                            if (episode.getId() == savedEpisode.getId() ||
                                    (episode.getNumber() != null && episode.getNumber().equals(savedEpisode.getNumber()))) {
                                callback.onCurrentEpisodeReceived(episode);
                                Log.d("AnimeApiService", "Loaded saved episode " + episode.getNumber() + " for anime " + animeId);
                                return;
                            }
                        }
                        // Сохраненный не найден в актуальном списке
                        callback.onError("SAVED_NOT_IN_LIST");
                        return;
                    }
                    // Нет сохранённого эпизода
                    callback.onError("NO_SAVED");
                } else {
                    callback.onError("Список эпизодов пуст");
                }
            }
            
            @Override
            public void onError(String error) {
                callback.onError("Ошибка при загрузке эпизодов: " + error);
            }
        });
    }
    
    private void saveCurrentEpisodeToDb(String animeId, EpisodesListResponse.EpisodeItem episode) {
        try {
            com.example.animelib.data.CurrentEpisodeEntity entity = new com.example.animelib.data.CurrentEpisodeEntity(
                    animeId,
                    episode.getId(),
                    episode.getNumber(),
                    System.currentTimeMillis()
            );
            db.currentEpisodeDao().upsert(entity);
            Log.d("AnimeApiService", "Saved current episode to DB: " + episode.getNumber());
        } catch (Exception e) {
            Log.e("AnimeApiService", "DB save error", e);
        }
    }

    private EpisodesListResponse.EpisodeItem loadCurrentEpisodeFromDb(String animeId) {
        try {
            com.example.animelib.data.CurrentEpisodeEntity entity = db.currentEpisodeDao().getByAnimeId(animeId);
            if (entity == null) return null;
            EpisodesListResponse.EpisodeItem item = new EpisodesListResponse.EpisodeItem();
            item.setId(entity.episodeId);
            item.setNumber(entity.episodeNumber);
            return item;
        } catch (Exception e) {
            Log.e("AnimeApiService", "DB load error", e);
            return null;
        }
    }

    public void save4KSetting(boolean enable4K) {
        executor.execute(() -> {
            try {
                AppSettings settings = db.appSettingsDao().getSettingsSync();
                if (settings == null) {
                    settings = new AppSettings();
                }
                settings.setEnable4K(enable4K);
                db.appSettingsDao().upsert(settings);
                Log.d("AnimeApiService", "Saved 4K setting: " + enable4K);
            } catch (Exception e) {
                Log.e("AnimeApiService", "Failed to save 4K setting", e);
            }
        });
    }
    
    public boolean load4KSetting() {
        try {
            AppSettings settings = db.appSettingsDao().getSettingsSync();
            return settings != null && settings.isEnable4K();
        } catch (Exception e) {
            Log.e("AnimeApiService", "Failed to load 4K setting", e);
            return false;
        }
    }
    
    public void saveAutoPlaySetting(boolean autoPlay) {
        executor.execute(() -> {
            try {
                AppSettings settings = db.appSettingsDao().getSettingsSync();
                if (settings == null) {
                    settings = new AppSettings();
                }
                settings.setAutoPlay(autoPlay);
                db.appSettingsDao().upsert(settings);
                Log.d("AnimeApiService", "Saved autoPlay setting: " + autoPlay);
            } catch (Exception e) {
                Log.e("AnimeApiService", "Failed to save autoPlay setting", e);
            }
        });
    }
    
    public boolean loadAutoPlaySetting() {
        try {
            AppSettings settings = db.appSettingsDao().getSettingsSync();
            return settings != null && settings.isAutoPlay();
        } catch (Exception e) {
            Log.e("AnimeApiService", "Failed to load autoPlay setting", e);
            return true; // Default to true
        }
    }
    
    public void saveLongSkipDurationSetting(int duration) {
        executor.execute(() -> {
            try {
                AppSettings settings = db.appSettingsDao().getSettingsSync();
                if (settings == null) {
                    settings = new AppSettings();
                }
                settings.setLongSkipDuration(duration);
                db.appSettingsDao().upsert(settings);
                Log.d("AnimeApiService", "Saved longSkipDuration setting: " + duration);
            } catch (Exception e) {
                Log.e("AnimeApiService", "Failed to save longSkipDuration setting", e);
            }
        });
    }
    
    public int loadLongSkipDurationSetting() {
        try {
            AppSettings settings = db.appSettingsDao().getSettingsSync();
            return settings != null ? settings.getLongSkipDuration() : 85; // Default to 85 seconds
        } catch (Exception e) {
            Log.e("AnimeApiService", "Failed to load longSkipDuration setting", e);
            return 85; // Default to 85 seconds
        }
    }
    
    public void saveThemeSetting(int themeMode) {
        executor.execute(() -> {
            try {
                AppSettings settings = db.appSettingsDao().getSettingsSync();
                if (settings == null) {
                    settings = new AppSettings();
                }
                settings.setThemeMode(themeMode);
                db.appSettingsDao().upsert(settings);
                Log.d("AnimeApiService", "Saved theme setting: " + themeMode);
            } catch (Exception e) {
                Log.e("AnimeApiService", "Failed to save theme setting", e);
            }
        });
    }
    
    public int loadThemeSetting() {
        try {
            AppSettings settings = db.appSettingsDao().getSettingsSync();
            return settings != null ? settings.getThemeMode() : 0; // Default to system theme
        } catch (Exception e) {
            Log.e("AnimeApiService", "Failed to load theme setting", e);
            return 0; // Default to system theme
        }
    }

    public void checkApiForToast(ToastCheckCallback callback) {
        executor.execute(() -> {
            String apiUrl = "https://api.cdnlibs.org/api/";
            Request request = buildApiRequest(apiUrl).build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e("AnimeApiService", "Toast API request failed", e);
                    callback.onError("Хуй там - " + e);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    try (response) {
                        if (response.isSuccessful()) {
                            assert response.body() != null;
                            String responseBody = response.body().string();
                            Log.d("AnimeApiService", "Toast API Response: " + responseBody);

                            // Проверяем, что ответ является валидным JSON объектом
                            if (responseBody.trim().startsWith("{") && responseBody.trim().endsWith("}")) {
                                ApiResponse apiResponse = gson.fromJson(responseBody, ApiResponse.class);

                                if (apiResponse != null && apiResponse.getData() != null
                                    && apiResponse.getData().getToast() != null
                                    && apiResponse.getData().getToast().getButtons() != null
                                    && !apiResponse.getData().getToast().getButtons().isEmpty()) {

                                    com.example.animelib.data.ButtonData button = apiResponse.getData().getToast().getButtons().get(0);
                                    String message = button.getText();

                                    if (message != null && message.contains("Перейти на зеркало")) {
                                        // Извлекаем новый URL из href
                                        String newUrl = button.getHref();
                                        if (newUrl != null && !newUrl.isEmpty()) {
                                            callback.onToastReceived(message, newUrl);
                                            Log.d("AnimeApiService", "Mirror URL found: " + newUrl);
                                        } else {
                                            callback.onError("Хуй там нет URL");
                                        }
                                    } else if (message != null) {
                                        // Показываем обычное сообщение
                                        callback.onToastReceived(message, null);
                                    } else {
                                        callback.onError("Хуй там нет текста");
                                    }
                                } else {
                                    callback.onError("Хуй там нет текста");
                                }
                            } else {
                                // Ответ не является JSON объектом
                                Log.w("AnimeApiService", "Toast API returned non-JSON response: " + responseBody);
                                callback.onError("Неверный формат ответа API");
                            }
                        } else {
                            Log.e("AnimeApiService", "Toast API request failed with code: " + response.code());
                            callback.onError("HTTP " + response.code());
                        }
                    } catch (Exception e) {
                        Log.e("AnimeApiService", "Error parsing toast API response", e);
                        callback.onError("Хуй там - " + e);
                    }
                }
            });
        });
    }

    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}
