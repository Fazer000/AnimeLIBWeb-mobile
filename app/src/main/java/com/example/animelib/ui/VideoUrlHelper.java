package com.example.animelib.ui;

import android.util.Log;
import android.webkit.WebView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for extracting video URLs from anime pages
 */
public class VideoUrlHelper {
    private static final String TAG = "VideoUrlHelper";
    private static final String VIDEO_CDN = "https://video1.cdnlibs.org/.%D0%B0s";

    /**
     * Достраивает относительную ссылку на видео доменом CDN
     */
    public static String toAbsoluteVideoUrl(String href) {
        if (href == null || href.isEmpty()) {
            return null;
        }
        if (href.startsWith("http")) {
            return href;
        }
        return VIDEO_CDN + href;
    }

    /**
     * Extract anime identifier from href (including slug)
     * @param href The href attribute from the anime button
     * @return Anime identifier like "24653--sakamoto-days-part-2-anime" or null
     */
    public static String extractAnimeIdentifier(String href) {
        if (href == null || href.isEmpty()) {
            return null;
        }

        // Extract anime identifier from href like "/ru/anime/24653--sakamoto-days-part-2-anime/watch"
        Pattern pattern = Pattern.compile("/ru/anime/([^/]+)/watch");
        Matcher matcher = pattern.matcher(href);

        if (matcher.find()) {
            String animeIdentifier = matcher.group(1);
            Log.d(TAG, "Found anime identifier: " + animeIdentifier);
            return animeIdentifier;
        }

        Log.w(TAG, "Could not extract anime identifier from href: " + href);
        return null;
    }

    /**
     * Extract numeric anime ID from full identifier
     * @param animeIdentifier Full identifier like "24653--sakamoto-days-part-2-anime"
     * @return Numeric ID like "24653" or null
     */
    public static String extractAnimeId(String animeIdentifier) {
        if (animeIdentifier == null || animeIdentifier.isEmpty()) {
            return null;
        }

        Pattern pattern = Pattern.compile("^(\\d+)");
        Matcher matcher = pattern.matcher(animeIdentifier);

        if (matcher.find()) {
            String animeId = matcher.group(1);
            Log.d(TAG, "Extracted numeric anime ID: " + animeId);
            return animeId;
        }

        return null;
    }

    /**
     * Extract video URL from anime watch page href
     * @param href The href attribute from the anime button
     * @param currentUrl Current page URL for context
     * @return Video URL or null if not found
     */
    public static String extractVideoUrl(String href, String currentUrl) {
        String animeIdentifier = extractAnimeIdentifier(href);
        if (animeIdentifier != null) {
            String animeId = extractAnimeId(animeIdentifier);
            Log.d(TAG, "Anime identifier: " + animeIdentifier + ", Numeric ID: " + animeId);
            // Return null for now - actual video URL will be fetched via API
            return null;
        }
        return null;
    }

    /**
     * Get video headers for the request
     * @param referer The referer URL
     * @return Map of headers
     */
    public static java.util.Map<String, String> getVideoHeaders(String referer) {
        java.util.Map<String, String> headers = new java.util.HashMap<>();
        headers.put("Accept", "video/webm,video/ogg,video/*;q=0.9,application/ogg;q=0.7,audio/*;q=0.6,*/*;q=0.5");
        headers.put("Accept-Language", "en-US,en;q=0.5");
        headers.put("Connection", "keep-alive");
        headers.put("Range", "bytes=0-");
        if (referer != null) {
            headers.put("Referer", referer);
        }
        headers.put("Sec-Fetch-Dest", "video");
        headers.put("Sec-Fetch-Mode", "no-cors");
        headers.put("Sec-Fetch-Site", "cross-site");
        headers.put("User-Agent", getRandomUserAgent());
        return headers;
    }

    /**
     * Get a random user agent for video requests
     * @return Random user agent string
     */
    public static String getRandomUserAgent() {
        String[] userAgents = {
            "Mozilla/5.0 (Linux; Android 10; SM-G975F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Mobile Safari/537.36",
            "Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.159 Mobile Safari/537.36",
            "Mozilla/5.0 (Linux; Android 12; SM-A525F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.62 Mobile Safari/537.36",
            "Mozilla/5.0 (Linux; Android 13; SM-N986B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.71 Mobile Safari/537.36"
        };
        return userAgents[(int) (Math.random() * userAgents.length)];
    }

    /**
     * Validate if URL is a supported video format
     * @param url Video URL
     * @return true if supported
     */
    public static boolean isSupportedVideoFormat(String url) {
        if (url == null) return false;

        String lowerUrl = url.toLowerCase();
        return lowerUrl.endsWith(".mp4") ||
               lowerUrl.endsWith(".webm") ||
               lowerUrl.endsWith(".m3u8") ||
               lowerUrl.endsWith(".mpd") ||
               lowerUrl.contains(".m3u8") ||
               lowerUrl.contains(".mpd");
    }
}
