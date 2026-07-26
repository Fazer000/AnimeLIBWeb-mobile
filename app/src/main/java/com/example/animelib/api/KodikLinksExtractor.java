package com.example.animelib.api;

import android.util.Base64;
import android.util.Log;

import com.example.animelib.models.KodikResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Получает прямые ссылки на видео Kodik без внешнего сервиса.
 * Порт логики kodikwrapper VideoLinks.getLinks().
 */
public class KodikLinksExtractor {

    private static final String TAG = "KodikExtractor";
    private static final String PLAYER_DOMAIN = "kodikplayer.com";
    private static final String DEFAULT_ENDPOINT = "/ftor";
    private static final String REFERER = "https://kodik.info/";
    private static final String ORIGIN = "https://kodik.info";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36";

    private static final Pattern LINK_PATTERN = Pattern.compile(
            "^(?:http[s]?:)?//(?<host>[a-z0-9.\\-]+\\.[a-z]+)/(?<type>[a-z]+)/(?<id>\\d+)/(?<hash>[0-9a-z]+)/(?<quality>\\d+p)");
    private static final Pattern ENDPOINT_PATTERN = Pattern.compile(
            "type:\"POST\",url:atob\\(\"([^\"]+)\"\\)");

    private final OkHttpClient httpClient;
    private final Gson gson;

    public KodikLinksExtractor(OkHttpClient httpClient, Gson gson) {
        this.httpClient = httpClient;
        this.gson = gson;
    }

    /**
     * Возвращает ссылки по качествам для ссылки на плеер Kodik.
     */
    public KodikResponse getLinks(String kodikLink) throws IOException {
        String link = normalizeLink(kodikLink);
        Matcher matcher = LINK_PATTERN.matcher(link);
        if (!matcher.find()) {
            throw new IOException("Ссылка Kodik не распознана: " + kodikLink);
        }

        String host = matcher.group("host");
        String type = matcher.group("type");
        String id = matcher.group("id");
        String hash = matcher.group("hash");

        JsonObject payload = requestVideoInfo(host, DEFAULT_ENDPOINT, hash, id, type);
        if (payload == null) {
            String endpoint = discoverEndpoint(link);
            Log.d(TAG, "Fallback endpoint: " + endpoint);
            payload = requestVideoInfo(host, endpoint, hash, id, type);
        }
        if (payload == null || !payload.has("links")) {
            throw new IOException("Kodik не вернул ссылки (возможно, изменился протокол)");
        }

        return buildResponse(payload.getAsJsonObject("links"));
    }

    /**
     * Приводит ссылку к абсолютному виду.
     */
    private String normalizeLink(String link) {
        if (link == null || link.isEmpty()) {
            return "";
        }
        if (link.startsWith("//")) {
            return "https:" + link;
        }
        if (link.startsWith("http")) {
            return link;
        }
        return "https://" + PLAYER_DOMAIN + (link.startsWith("/") ? link : "/" + link);
    }

    /**
     * Запрашивает информацию о видео, возвращает null если ответ не JSON.
     */
    private JsonObject requestVideoInfo(String host, String endpoint, String hash, String id, String type)
            throws IOException {
        String url = "https://" + host + endpoint
                + "?hash=" + hash + "&id=" + id + "&type=" + type;
        Log.d(TAG, "Video info request: " + url);

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Referer", REFERER)
                .header("Origin", ORIGIN)
                .header("Accept", "application/json, text/plain, */*")
                .header("X-Requested-With", "XMLHttpRequest")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String contentType = response.header("Content-Type", "");
            if (!response.isSuccessful() || contentType == null || !contentType.contains("json")) {
                Log.w(TAG, "Unexpected response: HTTP " + response.code() + ", type " + contentType);
                return null;
            }
            String body = response.body() != null ? response.body().string() : "";
            return gson.fromJson(body, JsonObject.class);
        }
    }

    /**
     * Извлекает актуальный endpoint со страницы плеера.
     */
    private String discoverEndpoint(String link) throws IOException {
        Request request = new Request.Builder()
                .url(link)
                .header("User-Agent", USER_AGENT)
                .header("Referer", REFERER)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String page = response.body() != null ? response.body().string() : "";
            Matcher matcher = ENDPOINT_PATTERN.matcher(page);
            if (matcher.find()) {
                String decoded = decodeBase64(matcher.group(1));
                if (!decoded.isEmpty()) {
                    return decoded;
                }
            }
        }
        return DEFAULT_ENDPOINT;
    }

    /**
     * Собирает KodikResponse из карты качеств, расшифровывая ссылки.
     */
    private KodikResponse buildResponse(JsonObject links) {
        Map<String, KodikResponse.VideoQuality[]> data = new HashMap<>();

        for (String quality : links.keySet()) {
            KodikResponse.VideoQuality[] parsed =
                    gson.fromJson(links.get(quality), KodikResponse.VideoQuality[].class);
            if (parsed == null) {
                continue;
            }
            for (KodikResponse.VideoQuality item : parsed) {
                if (item != null && item.getSrc() != null) {
                    item.setSrc(decodeBase64(rot18(item.getSrc())));
                }
            }
            data.put(quality, parsed);
        }

        KodikResponse response = new KodikResponse();
        response.setSuccess(!data.isEmpty());
        response.setData(data);
        return response;
    }

    /**
     * Сдвигает латинские буквы на 18 позиций внутри своего регистра.
     */
    private static String rot18(String value) {
        StringBuilder result = new StringBuilder(value.length());
        for (char symbol : value.toCharArray()) {
            if ((symbol >= 'a' && symbol <= 'z') || (symbol >= 'A' && symbol <= 'Z')) {
                int limit = symbol <= 'Z' ? 'Z' : 'z';
                int shifted = symbol + 18;
                result.append((char) (shifted <= limit ? shifted : shifted - 26));
            } else {
                result.append(symbol);
            }
        }
        return result.toString();
    }

    /**
     * Декодирует base64, дополняя строку до кратности четырём.
     */
    private static String decodeBase64(String value) {
        try {
            StringBuilder padded = new StringBuilder(value);
            while (padded.length() % 4 != 0) {
                padded.append('=');
            }
            return new String(Base64.decode(padded.toString(), Base64.DEFAULT), StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e(TAG, "Base64 decode failed", e);
            return "";
        }
    }
}