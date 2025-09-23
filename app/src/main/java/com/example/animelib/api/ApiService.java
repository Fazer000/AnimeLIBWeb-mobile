package com.example.animelib.api;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

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


    public interface ToastCheckCallback {
        void onToastReceived(String message, String newUrl);
        void onError(String error);
    }
    
    public interface BookmarkCallback {
        void onSuccess(String message);
        void onError(String error);
    }
    
    public interface AnimeBookmarkCallback {
        void onBookmarkReceived(AnimeBookmarkResponse response);
        void onError(String error);
    }
    
    public interface BookmarksListCallback {
        void onBookmarksReceived(BookmarksListResponse response);
        void onError(String error);
    }

    private final OkHttpClient httpClient;
    private final Gson gson;
    private final ExecutorService executor;
    private final Context context;

    // Database manager for all DB operations
    private final com.example.animelib.data.DatabaseManager databaseManager;

    public ApiService(Context context) {
        this.context = context; // Сохраняем оригинальный context для runOnUiThread
        this.httpClient = new OkHttpClient();
        this.gson = new Gson();
        this.executor = Executors.newSingleThreadExecutor();
        this.databaseManager = new com.example.animelib.data.DatabaseManager(context.getApplicationContext());
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
        return databaseManager.getSiteUrl();
    }

    public void fetchAnimeInfo(String animeSlugOrId, AnimeInfoCallback callback) {
        safeExecute(() -> {
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
        safeExecute(() -> {
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
        safeExecute(() -> {
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
        safeExecute(() -> {
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
        safeExecute(() -> {
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

        safeExecute(() -> {
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
        safeExecute(() -> {
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

    

    public void save4KSetting(boolean enable4K) {
        databaseManager.save4KSetting(enable4K);
    }
    
    public boolean load4KSetting() {
        return databaseManager.load4KSetting();
    }
    
    public void saveAutoPlaySetting(boolean autoPlay) {
        databaseManager.saveAutoPlaySetting(autoPlay);
    }
    
    public boolean loadAutoPlaySetting() {
        return databaseManager.loadAutoPlaySetting();
    }
    
    public void saveLongSkipDurationSetting(int duration) {
        databaseManager.saveLongSkipDurationSetting(duration);
    }
    
    public int loadLongSkipDurationSetting() {
        return databaseManager.loadLongSkipDurationSetting();
    }
    
    public void saveThemeSetting(int themeMode) {
        databaseManager.saveThemeSetting(themeMode);
    }
    
    public int loadThemeSetting() {
        return databaseManager.loadThemeSetting();
    }
    
    public com.example.animelib.data.DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public OkHttpClient getHttpClient() {
        return httpClient;
    }
    
    public Gson getGson() {
        return gson;
    }

    public void checkApiForToast(ToastCheckCallback callback) {
        safeExecute(() -> {
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

                                    ToastData.ButtonData button = apiResponse.getData().getToast().getButtons().get(0);
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

    /**
     * Добавляет серию в закладки
     * @param mediaSlug Слаг медиа (например: "23811--kaijuu-8-gou-2nd-season-anime")
     * @param episodeId ID эпизода
     * @param teamId ID команды перевода
     * @param episodeNumber Номер эпизода
     * @param currentTimecode Текущее время в формате "12:01"
     * @param callback Колбэк для результата операции
     */
    public void addBookmark(String mediaSlug, int episodeId, int teamId, int episodeNumber, 
                           String currentTimecode, BookmarkCallback callback) {
        
        Log.d("ApiService", "Adding bookmark - mediaSlug: " + mediaSlug + 
                   ", episodeId: " + episodeId + 
                   ", teamId: " + teamId + 
                   ", episodeNumber: " + episodeNumber + 
                   ", timecode: " + currentTimecode);
        
        safeExecute(() -> {
            try {
                // Создаем JSON объект для запроса
                com.google.gson.JsonObject requestBody = createBookmarkRequestBody(
                    mediaSlug, episodeId, teamId, episodeNumber, currentTimecode
                );
                
                String jsonString = gson.toJson(requestBody);
                Log.d("ApiService", "Request body: " + jsonString);
                
                // Создаем HTTP запрос
                okhttp3.RequestBody body = okhttp3.RequestBody.create(jsonString, okhttp3.MediaType.get("application/json; charset=utf-8"));
                Request request = new Request.Builder()
                    .url("https://api.cdnlibs.org/api/bookmarks")
                    .post(body)
                    .addHeader("Authorization", "Bearer " + CDNLIBS_BEARER_TOKEN)
                    .addHeader("Accept", "*/*")
                    .addHeader("Accept-Language", "ru,en;q=0.9,de;q=0.8,zh;q=0.7")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Origin", getSiteUrlFromDb())
                    .addHeader("Referer", getSiteUrlFromDb() + "/")
                    .addHeader("Sec-Ch-Ua", "\"Google Chrome\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"")
                    .addHeader("Sec-Ch-Ua-Mobile", "?1")
                    .addHeader("Sec-Ch-Ua-Platform", "\"Android\"")
                    .addHeader("Sec-Fetch-Dest", "empty")
                    .addHeader("Sec-Fetch-Mode", "cors")
                    .addHeader("Sec-Fetch-Site", "cross-site")
                    .addHeader("Site-Id", "5")
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14; SM-G998B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36")
                    .addHeader("Client-Time-Zone", "Europe/Samara")
                    .addHeader("Priority", "u=1, i")
                    .build();
                
                // Выполняем запрос
                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Log.e("ApiService", "Bookmark request failed", e);
                        // Вызываем колбэк в главном потоке
                        safeRunOnUiThread(() -> callback.onError("Ошибка сети: " + e.getMessage()));
                    }
                    
                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        try (response) {
                            if (response.isSuccessful()) {
                                String responseBody = response.body() != null ? response.body().string() : "";
                                Log.d("ApiService", "Bookmark added successfully: " + responseBody);
                                // Вызываем колбэк в главном потоке
                                safeRunOnUiThread(() -> callback.onSuccess("Закладка добавлена успешно"));
                            } else {
                                String errorBody = response.body() != null ? response.body().string() : "";
                                Log.e("ApiService", "Failed to add bookmark. Code: " + response.code() + ", Body: " + errorBody);
                                // Вызываем колбэк в главном потоке
                                safeRunOnUiThread(() -> callback.onError("Ошибка при добавлении закладки: " + response.code()));
                            }
                        } catch (Exception e) {
                            Log.e("ApiService", "Error processing bookmark response", e);
                            // Вызываем колбэк в главном потоке
                            safeRunOnUiThread(() -> callback.onError("Ошибка обработки ответа: " + e.getMessage()));
                        }
                    }
                });
                
            } catch (Exception e) {
                Log.e("ApiService", "Unexpected error while adding bookmark", e);
                // Вызываем колбэк в главном потоке
                safeRunOnUiThread(() -> callback.onError("Неожиданная ошибка: " + e.getMessage()));
            }
        });
    }
    
    /**
     * Создает JSON объект для запроса добавления закладки
     */
    private com.google.gson.JsonObject createBookmarkRequestBody(String mediaSlug, int episodeId, int teamId, 
                                               int episodeNumber, String currentTimecode) {
        
        com.google.gson.JsonObject requestBody = new com.google.gson.JsonObject();
        requestBody.addProperty("media_type", "anime");
        requestBody.addProperty("media_slug", mediaSlug);
        
        // Создаем объект bookmark
        com.google.gson.JsonObject bookmark = new com.google.gson.JsonObject();
        bookmark.addProperty("item_id", episodeId);
        bookmark.addProperty("status", 21);
        bookmark.addProperty("progress", currentTimecode);
        requestBody.add("bookmark", bookmark);
        
        // Создаем объект meta
        com.google.gson.JsonObject meta = new com.google.gson.JsonObject();
        meta.addProperty("team", teamId);
        meta.addProperty("translation_type", 2);
        meta.addProperty("player", "Animelib");
        meta.addProperty("item_number", episodeNumber);
        requestBody.add("meta", meta);
        
        return requestBody;
    }
    
    /**
     * Извлекает media_slug из URL аниме
     * @param animeUrl URL аниме
     * @return media_slug или null если не удалось извлечь
     */
    public static String extractMediaSlugFromUrl(String animeUrl) {
        if (animeUrl == null || animeUrl.isEmpty()) {
            return null;
        }
        
        try {
            // Ищем паттерн /anime/{slug} в URL
            String pattern = "/anime/([^/\\?]+)";
            java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher matcher = regex.matcher(animeUrl);
            
            if (matcher.find()) {
                String slug = matcher.group(1);
                Log.d("ApiService", "Extracted media slug: " + slug);
                return slug;
            }
            
            Log.w("ApiService", "Could not extract media slug from URL: " + animeUrl);
            return null;
            
        } catch (Exception e) {
            Log.e("ApiService", "Error extracting media slug from URL: " + animeUrl, e);
            return null;
        }
    }
    
    /**
     * Форматирует время в формат "MM:SS"
     * @param milliseconds Время в миллисекундах
     * @return Отформатированное время
     */
    public static String formatTimecode(long milliseconds) {
        if (milliseconds < 0) {
            return "00:00";
        }
        
        long totalSeconds = milliseconds / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        
        return String.format("%02d:%02d", minutes, seconds);
    }
    
    /**
     * Безопасно выполняет задачу в executor
     */
    private void safeExecute(Runnable task) {
        try {
            if (executor != null && !executor.isShutdown()) {
                executor.execute(task);
            } else {
                Log.w("ApiService", "Executor is shutdown, skipping task execution");
            }
        } catch (Exception e) {
            Log.e("ApiService", "Error executing task", e);
        }
    }

    /**
     * Получает закладку аниме
     * @param mediaSlug Слаг медиа (например: "23811--kaijuu-8-gou-2nd-season-anime")
     * @param callback Колбэк для результата операции
     */
    public void fetchAnimeBookmark(String mediaSlug, AnimeBookmarkCallback callback) {
        Log.d("ApiService", "Fetching anime bookmark for mediaSlug: " + mediaSlug);
        
        safeExecute(() -> {
            try {
                String apiUrl = "https://api.cdnlibs.org/api/anime/" + mediaSlug + "/bookmark";
                Request request = buildApiRequest(apiUrl).build();
                
                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Log.e("ApiService", "Anime bookmark request failed", e);
                        // Вызываем колбэк в главном потоке
                        safeRunOnUiThread(() -> callback.onError("Ошибка сети: " + e.getMessage()));
                    }
                    
                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        try (response) {
                            if (response.isSuccessful()) {
                                String responseBody = response.body() != null ? response.body().string() : "";
                                Log.d("ApiService", "Anime bookmark response: " + responseBody);
                                
                                AnimeBookmarkResponse bookmarkResponse = gson.fromJson(responseBody, AnimeBookmarkResponse.class);
                                if (bookmarkResponse != null) {
                                    // Вызываем колбэк в главном потоке (даже если data == null, это нормально - нет закладки)
                                    safeRunOnUiThread(() -> callback.onBookmarkReceived(bookmarkResponse));
                                } else {
                                    // Вызываем колбэк в главном потоке
                                    safeRunOnUiThread(() -> callback.onError("Неверный формат ответа закладки"));
                                }
                            } else {
                                String errorBody = response.body() != null ? response.body().string() : "";
                                Log.e("ApiService", "Failed to fetch anime bookmark. Code: " + response.code() + ", Body: " + errorBody);
                                // Вызываем колбэк в главном потоке
                                safeRunOnUiThread(() -> callback.onError("Ошибка при получении закладки: " + response.code()));
                            }
                        } catch (Exception e) {
                            Log.e("ApiService", "Error processing anime bookmark response", e);
                            // Вызываем колбэк в главном потоке
                            safeRunOnUiThread(() -> callback.onError("Ошибка обработки ответа: " + e.getMessage()));
                        }
                    }
                });
                
            } catch (Exception e) {
                Log.e("ApiService", "Unexpected error while fetching anime bookmark", e);
                // Вызываем колбэк в главном потоке
                safeRunOnUiThread(() -> callback.onError("Неожиданная ошибка: " + e.getMessage()));
            }
        });
    }

    /**
     * Получает список закладок пользователя
     * @param callback Колбэк для результата операции
     */
    public void fetchBookmarksList(BookmarksListCallback callback) {
        Log.d("ApiService", "Fetching bookmarks list");
        
        safeExecute(() -> {
            try {
                String apiUrl = "https://api.cdnlibs.org/api/bookmarks?page=1&sort_by=name&sort_type=desc&status=21&user_id=9439321";
                Request request = buildApiRequest(apiUrl).build();
                
                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Log.e("ApiService", "Bookmarks list request failed", e);
                        // Вызываем колбэк в главном потоке
                        safeRunOnUiThread(() -> callback.onError("Ошибка сети: " + e.getMessage()));
                    }
                    
                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        try (response) {
                            if (response.isSuccessful()) {
                                String responseBody = response.body() != null ? response.body().string() : "";
                                Log.d("ApiService", "Bookmarks list response: " + responseBody);
                                
                                BookmarksListResponse bookmarksResponse = gson.fromJson(responseBody, BookmarksListResponse.class);
                                if (bookmarksResponse != null) {
                                    // Вызываем колбэк в главном потоке
                                    safeRunOnUiThread(() -> callback.onBookmarksReceived(bookmarksResponse));
                                } else {
                                    // Вызываем колбэк в главном потоке
                                    safeRunOnUiThread(() -> callback.onError("Неверный формат ответа закладок"));
                                }
                            } else {
                                String errorBody = response.body() != null ? response.body().string() : "";
                                Log.e("ApiService", "Failed to fetch bookmarks list. Code: " + response.code() + ", Body: " + errorBody);
                                // Вызываем колбэк в главном потоке
                                safeRunOnUiThread(() -> callback.onError("Ошибка при получении закладок: " + response.code()));
                            }
                        } catch (Exception e) {
                            Log.e("ApiService", "Error processing bookmarks list response", e);
                            // Вызываем колбэк в главном потоке
                            safeRunOnUiThread(() -> callback.onError("Ошибка обработки ответа: " + e.getMessage()));
                        }
                    }
                });
                
            } catch (Exception e) {
                Log.e("ApiService", "Unexpected error while fetching bookmarks list", e);
                // Вызываем колбэк в главном потоке
                safeRunOnUiThread(() -> callback.onError("Неожиданная ошибка: " + e.getMessage()));
            }
        });
    }

    /**
     * Безопасно вызывает колбэк в главном потоке
     */
    private void safeRunOnUiThread(Runnable runnable) {
        try {
            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).runOnUiThread(runnable);
            } else {
                runnable.run();
            }
        } catch (Exception e) {
            Log.e("ApiService", "Error calling UI thread callback", e);
            // Fallback - вызываем в текущем потоке
            try {
                runnable.run();
            } catch (Exception ex) {
                Log.e("ApiService", "Error in fallback callback", ex);
            }
        }
    }

    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                // Ждем завершения текущих задач максимум 2 секунды
                if (!executor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                    Log.w("ApiService", "Executor did not terminate gracefully, forcing shutdown");
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Log.e("ApiService", "Interrupted while waiting for executor termination", e);
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
    }
}
