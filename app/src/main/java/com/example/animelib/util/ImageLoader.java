package com.example.animelib.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageLoader {
    private static volatile ImageLoader instance;
    private final LruCache<String, Bitmap> memoryCache;
    private final ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ImageLoader() {
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8; // 1/8 памяти под кэш
        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };
        executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    public static ImageLoader getInstance() {
        if (instance == null) {
            synchronized (ImageLoader.class) {
                if (instance == null) instance = new ImageLoader();
            }
        }
        return instance;
    }

    public void loadInto(ImageView imageView, String url, int placeholderResId) {
        if (placeholderResId != 0) imageView.setImageResource(placeholderResId);
        if (url == null || url.isEmpty()) return;

        imageView.setTag(url);
        Bitmap cached = memoryCache.get(url);
        if (cached != null) {
            imageView.setImageBitmap(cached);
            return;
        }

        executor.submit(() -> {
            Bitmap bitmap = downloadBitmap(url);
            if (bitmap != null) memoryCache.put(url, bitmap);
            mainHandler.post(() -> {
                Object tag = imageView.getTag();
                if (tag != null && tag.equals(url) && bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                }
            });
        });
    }

    private Bitmap downloadBitmap(String src) {
        HttpURLConnection connection = null;
        InputStream input = null;
        try {
            URL url = new URL(src);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(7000);
            connection.setReadTimeout(10000);
            connection.setInstanceFollowRedirects(true);
            connection.connect();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) return null;
            input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (Exception ignored) {
            return null;
        } finally {
            try { if (input != null) input.close(); } catch (Exception ignored) {}
            if (connection != null) connection.disconnect();
        }
    }
}


