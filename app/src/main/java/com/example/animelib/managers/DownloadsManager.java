package com.example.animelib.managers;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;

import com.example.animelib.ui.VideoUrlHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Менеджер скачивания серий в папку «Загрузки»
 */
public class DownloadsManager {
    private static final String TAG = "DownloadsManager";
    private static final String RELATIVE_PATH = Environment.DIRECTORY_DOWNLOADS + "/AnimeLIB";
    private static final int BUFFER_SIZE = 64 * 1024;

    public interface DownloadCallback {
        void onProgress(int percent);
        void onFinished(String fileName);
        void onError(String message);
    }

    private final Context context;
    private final OkHttpClient client;
    private final ExecutorService executor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private volatile boolean running;
    private volatile boolean cancelled;

    public DownloadsManager(Context context) {
        this.context = context.getApplicationContext();
        this.executor = Executors.newSingleThreadExecutor();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Идёт ли скачивание прямо сейчас
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Отменяет текущее скачивание
     */
    public void cancel() {
        cancelled = true;
    }

    /**
     * Скачивает файл по ссылке в «Загрузки/AnimeLIB»
     */
    public void download(String url, String fileName, String referer, DownloadCallback callback) {
        if (running) {
            callback.onError("Скачивание уже идёт");
            return;
        }
        running = true;
        cancelled = false;

        executor.execute(() -> {
            ContentResolver resolver = context.getContentResolver();
            Uri uri = null;

            try {
                uri = createPendingFile(resolver, fileName);
                if (uri == null) {
                    finish(callback, null, "Не удалось создать файл в «Загрузках»");
                    return;
                }

                Request.Builder request = new Request.Builder().url(url);
                for (Map.Entry<String, String> header : VideoUrlHelper.getVideoHeaders(referer).entrySet()) {
                    request.header(header.getKey(), header.getValue());
                }

                try (Response response = client.newCall(request.build()).execute()) {
                    if (!response.isSuccessful()) {
                        deleteQuietly(resolver, uri);
                        finish(callback, null, "HTTP " + response.code());
                        return;
                    }

                    ResponseBody body = response.body();
                    if (body == null) {
                        deleteQuietly(resolver, uri);
                        finish(callback, null, "Пустой ответ сервера");
                        return;
                    }

                    writeToUri(resolver, uri, body, callback);
                }

                if (cancelled) {
                    deleteQuietly(resolver, uri);
                    finish(callback, null, "Скачивание отменено");
                    return;
                }

                publish(resolver, uri);
                Log.d(TAG, "Downloaded to Downloads/AnimeLIB: " + fileName);
                finish(callback, fileName, null);

            } catch (Exception e) {
                Log.e(TAG, "Download failed", e);
                deleteQuietly(resolver, uri);
                finish(callback, null, e.getMessage() != null ? e.getMessage() : "неизвестная ошибка");
            }
        });
    }

    /**
     * Освобождает ресурсы менеджера
     */
    public void cleanup() {
        cancel();
        if (!executor.isShutdown()) {
            executor.shutdown();
        }
    }

    /**
     * Создаёт скрытую запись в «Загрузках» до конца записи файла
     */
    private Uri createPendingFile(ContentResolver resolver, String fileName) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        values.put(MediaStore.Downloads.MIME_TYPE, "video/mp4");
        values.put(MediaStore.Downloads.RELATIVE_PATH, RELATIVE_PATH);
        values.put(MediaStore.Downloads.IS_PENDING, 1);
        return resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
    }

    /**
     * Делает готовый файл видимым для пользователя
     */
    private void publish(ContentResolver resolver, Uri uri) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.IS_PENDING, 0);
        resolver.update(uri, values, null, null);
    }

    /**
     * Пишет тело ответа в файл и сообщает прогресс
     */
    private void writeToUri(ContentResolver resolver, Uri uri, ResponseBody body, DownloadCallback callback) throws IOException {
        long total = body.contentLength();
        long written = 0;
        int lastPercent = -1;
        byte[] buffer = new byte[BUFFER_SIZE];

        try (InputStream in = body.byteStream(); OutputStream out = resolver.openOutputStream(uri)) {
            if (out == null) {
                throw new IOException("Нет доступа к файлу в «Загрузках»");
            }

            int read;
            while ((read = in.read(buffer)) != -1) {
                if (cancelled) {
                    return;
                }
                out.write(buffer, 0, read);
                written += read;

                if (total > 0) {
                    int percent = (int) (written * 100 / total);
                    if (percent != lastPercent) {
                        lastPercent = percent;
                        int value = percent;
                        mainHandler.post(() -> callback.onProgress(value));
                    }
                }
            }
            out.flush();
        }
    }

    /**
     * Завершает скачивание и возвращает результат в главный поток
     */
    private void finish(DownloadCallback callback, String fileName, String error) {
        running = false;
        mainHandler.post(() -> {
            if (error != null) {
                callback.onError(error);
            } else {
                callback.onFinished(fileName);
            }
        });
    }

    /**
     * Удаляет незавершённую запись из «Загрузок»
     */
    private void deleteQuietly(ContentResolver resolver, Uri uri) {
        if (uri == null) {
            return;
        }
        try {
            resolver.delete(uri, null, null);
        } catch (Exception e) {
            Log.w(TAG, "Failed to delete pending file", e);
        }
    }
}