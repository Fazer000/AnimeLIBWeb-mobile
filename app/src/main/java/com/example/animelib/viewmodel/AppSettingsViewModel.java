package com.example.animelib.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.animelib.data.AppDatabase;
import com.example.animelib.data.AppSettings;
import com.example.animelib.data.AppSettingsDao;

public class AppSettingsViewModel extends AndroidViewModel {
    private AppSettingsDao appSettingsDao;
    private LiveData<AppSettings> settings;
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public AppSettingsViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getDatabase(application);
        appSettingsDao = db.appSettingsDao();
        settings = appSettingsDao.getSettings();
    }

    public LiveData<AppSettings> getSettings() {
        return settings;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public void saveSettings(String siteUrl) {
        isLoading.setValue(true);
        new Thread(() -> {
            try {
                AppSettings settings = new AppSettings();
                settings.setSiteUrl(siteUrl);
                appSettingsDao.insert(settings);
                isLoading.postValue(false);
            } catch (Exception e) {
                isLoading.postValue(false);
            }
        }).start();
    }

    public void updateSettings(String siteUrl) {
        isLoading.setValue(true);
        new Thread(() -> {
            try {
                AppSettings currentSettings = settings.getValue();
                if (currentSettings != null) {
                    currentSettings.setSiteUrl(siteUrl);
                    appSettingsDao.update(currentSettings);
                } else {
                    AppSettings newSettings = new AppSettings();
                    newSettings.setSiteUrl(siteUrl);
                    appSettingsDao.insert(newSettings);
                }
                isLoading.postValue(false);
            } catch (Exception e) {
                isLoading.postValue(false);
            }
        }).start();
    }

    public int getSettingsCount() {
        return appSettingsDao.getSettingsCount();
    }
}
