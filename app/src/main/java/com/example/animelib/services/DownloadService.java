package com.example.animelib.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

import com.example.animelib.R;
import com.example.animelib.managers.DownloadsManager;

/**
 * Фоновый сервис скачивания серии с уведомлением о прогрессе
 */
public class DownloadService extends Service {
    private static final String TAG = "DownloadService";
    private static final String CHANNEL_ID = "animelib_downloads";
    private static final int PROGRESS_NOTIFICATION_ID = 4201;
    private static final int RESULT_NOTIFICATION_ID = 4202;

    public static final String ACTION_START = "com.example.animelib.action.DOWNLOAD_START";
    public static final String ACTION_CANCEL = "com.example.animelib.action.DOWNLOAD_CANCEL";
    private static final String EXTRA_URL = "url";
    private static final String EXTRA_FILE_NAME = "file_name";
    private static final String EXTRA_REFERER = "referer";

    public interface ProgressListener {
        void onProgress(int percent);
        void onFinished(String fileName);
        void onError(String message);
    }

    private static volatile ProgressListener listener;
    private static volatile boolean running;
    private static volatile int progress;

    private DownloadsManager downloadsManager;
    private NotificationManager notificationManager;
    private String currentFileName;
    private boolean cancelRequested;

    /**
     * Запускает скачивание в фоне
     */
    public static void start(Context context, String url, String fileName, String referer) {
        Intent intent = new Intent(context, DownloadService.class);
        intent.setAction(ACTION_START);
        intent.putExtra(EXTRA_URL, url);
        intent.putExtra(EXTRA_FILE_NAME, fileName);
        intent.putExtra(EXTRA_REFERER, referer);
        context.startForegroundService(intent);
    }

    /**
     * Отменяет текущее скачивание
     */
    public static void cancel(Context context) {
        if (!running) {
            return;
        }
        Intent intent = new Intent(context, DownloadService.class);
        intent.setAction(ACTION_CANCEL);
        context.startService(intent);
    }

    /**
     * Идёт ли скачивание прямо сейчас
     */
    public static boolean isRunning() {
        return running;
    }

    /**
     * Текущий прогресс в процентах
     */
    public static int getProgress() {
        return progress;
    }

    /**
     * Подписывает UI на события скачивания
     */
    public static void setListener(ProgressListener value) {
        listener = value;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        downloadsManager = new DownloadsManager(this);
        createChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;

        if (ACTION_CANCEL.equals(action)) {
            Log.d(TAG, "Cancel requested");
            cancelRequested = true;
            downloadsManager.cancel();
            return START_NOT_STICKY;
        }

        if (!ACTION_START.equals(action)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (running) {
            Log.d(TAG, "Download already running, ignoring start");
            return START_NOT_STICKY;
        }

        String url = intent.getStringExtra(EXTRA_URL);
        String fileName = intent.getStringExtra(EXTRA_FILE_NAME);
        String referer = intent.getStringExtra(EXTRA_REFERER);

        if (url == null || fileName == null) {
            Log.e(TAG, "Missing url or file name");
            stopSelf();
            return START_NOT_STICKY;
        }

        currentFileName = fileName;
        cancelRequested = false;
        running = true;
        progress = 0;

        startForeground(PROGRESS_NOTIFICATION_ID, buildProgressNotification(0));
        startDownload(url, fileName, referer);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        running = false;
        if (downloadsManager != null) {
            downloadsManager.cleanup();
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Запускает загрузку и транслирует прогресс в UI и уведомление
     */
    private void startDownload(String url, String fileName, String referer) {
        downloadsManager.download(url, fileName, referer, new DownloadsManager.DownloadCallback() {
            @Override
            public void onProgress(int percent) {
                progress = percent;
                notificationManager.notify(PROGRESS_NOTIFICATION_ID, buildProgressNotification(percent));

                ProgressListener current = listener;
                if (current != null) {
                    current.onProgress(percent);
                }
            }

            @Override
            public void onFinished(String name) {
                running = false;

                ProgressListener current = listener;
                if (current != null) {
                    current.onFinished(name);
                }

                showResultNotification("Серия сохранена в «Загрузки»", name);
                stopAfterResult();
            }

            @Override
            public void onError(String message) {
                running = false;

                ProgressListener current = listener;
                if (current != null) {
                    current.onError(message);
                }

                if (!cancelRequested) {
                    showResultNotification("Скачивание не удалось", message);
                }
                stopAfterResult();
            }
        });
    }

    /**
     * Создаёт канал уведомлений о скачивании
     */
    private void createChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Скачивание", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Прогресс скачивания серий");
        notificationManager.createNotificationChannel(channel);
    }

    /**
     * Собирает уведомление с прогрессом и кнопкой отмены
     */
    private Notification buildProgressNotification(int percent) {
        Intent cancelIntent = new Intent(this, DownloadService.class).setAction(ACTION_CANCEL);
        PendingIntent cancelPending = PendingIntent.getService(this, 0, cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_download)
                .setContentTitle("Скачивание серии")
                .setContentText(currentFileName)
                .setProgress(100, percent, percent == 0)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .addAction(R.drawable.ic_close, "Отмена", cancelPending)
                .build();
    }

    /**
     * Показывает итоговое уведомление
     */
    private void showResultNotification(String title, String text) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_download)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .build();
        notificationManager.notify(RESULT_NOTIFICATION_ID, notification);
    }

    /**
     * Снимает foreground и останавливает сервис
     */
    private void stopAfterResult() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
        stopSelf();
    }
}