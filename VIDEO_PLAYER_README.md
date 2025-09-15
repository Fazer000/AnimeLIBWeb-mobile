# AnimeLIB Video Player

Современный видеоплеер для Android приложения AnimeLIB, построенный на базе Media3 (новая версия ExoPlayer).

## Возможности

- ✅ **HTTP Progressive Download** - потоковое воспроизведение без загрузки всего файла
- ✅ **Современные технологии** - Media3, OkHttp, Hardware Acceleration
- ✅ **Полноэкранный режим** - с автоматическим переключением ориентации
- ✅ **Кастомные заголовки** - поддержка referer, user-agent для обхода ограничений
- ✅ **Обработка ошибок** - автоматическое восстановление при проблемах с сетью
- ✅ **Буферизация** - умное управление буфером для плавного воспроизведения
- ✅ **JavaScript интеграция** - перехват кликов на видео-кнопки

## Архитектура

### Основные компоненты

1. **VideoPlayerActivity** - основной плеер с UI
2. **VideoUrlHelper** - утилиты для работы с URL видео
3. **JavaScriptInterface** - мост между WebView и Android
4. **Media3 (ExoPlayer)** - ядро видеоплеера

### Поддерживаемые форматы

- MP4 (рекомендуемый)
- WebM
- HLS (.m3u8)
- DASH (.mpd)

## Использование

### Автоматический запуск

Плеер автоматически запускается при клике на кнопки с классами:
- `btn`
- `is-filled`
- `variant-primary`
- `rw_ci`

И href содержащим `/ru/anime`

### Ручной запуск

```java
Intent intent = new Intent(context, VideoPlayerActivity.class);
intent.putExtra("video_url", "https://example.com/video.mp4");
intent.putExtra("video_title", "Название видео");
intent.putExtra("referer", "https://example.com");
startActivity(intent);
```

## Настройка URL видео

### VideoUrlHelper

Класс `VideoUrlHelper` содержит логику для извлечения URL видео из ссылок аниме.

```java
String videoUrl = VideoUrlHelper.extractVideoUrl(href, currentUrl);
```

### Примеры URL паттернов

Нужно настроить в `VideoUrlHelper.extractVideoUrl()`:

```java
// Извлечение ID аниме
Pattern pattern = Pattern.compile("/ru/anime/(\\d+)");
Matcher matcher = pattern.matcher(href);
if (matcher.find()) {
    String animeId = matcher.group(1);
    // Конструкция URL видео
    return "https://site.com/videos/" + animeId + ".mp4";
}
```

## Управление плеером

### Полноэкранный режим

- **Вход**: двойной тап по экрану или кнопка fullscreen
- **Выход**: кнопка back или повторный двойной тап

### Управление воспроизведением

- Play/Pause: тап по центру экрана
- Перемотка: свайп влево/вправо
- Громкость: свайп вверх/вниз
- Настройки качества: доступны в меню плеера

## Установка зависимостей

Зависимости уже добавлены в `build.gradle`:

```gradle
implementation libs.media3.exoplayer
implementation libs.media3.ui
implementation libs.media3.common
implementation libs.media3.datasource.okhttp
```

## Тестирование

### Тестовый метод

```java
private void testVideoPlayer() {
    String testVideoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4";
    launchVideoPlayer(testVideoUrl, "Test Video");
}
```

### Публичные тестовые видео

- Big Buck Bunny: `https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4`
- Elephants Dream: `https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4`

## Отладка

### Логи

- `VideoPlayerActivity`: основные события плеера
- `VideoUrlHelper`: извлечение URL видео
- `AnimeButton`: перехват кликов кнопок

### Возможные проблемы

1. **Ошибка сети**: Проверить интернет и заголовки
2. **Формат не поддерживается**: Проверить MIME-type и формат видео
3. **Referer блокируется**: Настроить правильные заголовки

## API Интеграция

### Получение видео через API

Приложение теперь поддерживает автоматическое получение видео URL через API:

1. **Извлечение ID**: Из href кнопки `/ru/anime/24653--sakamoto-days-part-2-anime/watch` извлекается `24653--sakamoto-days-part-2-anime`
2. **API Episodes**: `GET https://api.cdnlibs.org/api/episodes?anime_id=24653`
3. **API Episode Details**: `GET https://api.cdnlibs.org/api/episodes/{episode_id}` (с заголовком `Authorization: Bearer {token}`)
4. **Извлечение видео**: Из массива `players` выбирается запись с `"player": "Animelib"`, затем берется `video.quality.href` с наивысшим качеством (предпочтительно 1080p)

### Структура API запросов

```java
// 1. Получить список эпизодов
animeApiService.getEpisodes(animeId)
    .thenCompose(episodesData -> {
        String episodeId = AnimeApiService.getFirstEpisodeId(episodesData);
        return animeApiService.getEpisodeDetails(episodeId);
    })
    .thenAccept(episodeData -> {
        // Извлекает видео URL из плеера с "player": "Animelib"
        String videoUrl = AnimeApiService.extractVideoUrl(episodeData);
        // Запустить плеер с videoUrl
    });
```

## Настройка Bearer токена

Для работы с API требуется Bearer токен авторизации:

### Добавление токена в код

```java
// В MainActivity.onCreate()
String bearerToken = getBearerToken(); // Получить токен
if (bearerToken != null && !bearerToken.isEmpty()) {
    animeApiService.setBearerToken(bearerToken);
}
```

### Способы получения токена

1. **Из SharedPreferences**:
```java
private String getBearerToken() {
    return getSharedPreferences("auth", MODE_PRIVATE)
            .getString("bearer_token", null);
}
```

2. **Из защищенного хранилища**:
```java
private String getBearerToken() {
    // Используйте EncryptedSharedPreferences или KeyStore
    return secureStorage.getString("bearer_token");
}
```

3. **Через вход в систему**:
```java
// После успешной авторизации сохранить токен
getSharedPreferences("auth", MODE_PRIVATE)
    .edit()
    .putString("bearer_token", receivedToken)
    .apply();
```

4. **Из конфигурации приложения**:
```java
private String getBearerToken() {
    return BuildConfig.BEARER_TOKEN; // Из build.gradle
}
```

### Важные замечания

- **Безопасность**: Никогда не храните токен в открытом виде
- **Обновление**: Реализуйте логику обновления токена при истечении
- **Обработка ошибок**: Добавьте обработку случаев, когда токен недействителен

## Оптимизации производительности

### Предотвращение ошибок AppStandbyController

Приложение включает оптимизации для предотвращения системных ошибок:

#### Проверки жизненного цикла активности:
- ✅ Проверка активности перед запуском фоновых задач
- ✅ Двойная проверка в фоновых потоках
- ✅ Безопасное взаимодействие с UI только при активной активности
- ✅ Правильная обработка `onStart()`, `onStop()`, `onDestroy()`

#### Управление сетевыми запросами:
```java
// Проверка перед запуском
if (!isActivityActive()) {
    return; // Не выполняем запрос
}

// Двойная проверка в фоне
if (!isActivityActive()) {
    return; // Активность уничтожена
}
```

#### UI операции только при активной активности:
```java
if (isActivityActive()) {
    runOnUiThread(() -> {
        if (isActivityActive()) {
            // Безопасная UI операция
        }
    });
}
```

### Диагностика проблем

#### Логи для отладки:
- `Activity destroyed during background execution` - активность уничтожена
- `Activity not active, skipping API check` - пропуск проверки API
- `Activity destroyed, skipping UI operations` - пропуск UI операций

#### Возможные решения:
1. **Увеличить timeout** для сетевых запросов
2. **Добавить retry логику** для неудачных запросов
3. **Использовать WorkManager** для критических задач
4. **Оптимизировать** частоту API проверок

## Отладка JavaScript перехвата кнопок

### Включение отладки WebView

Приложение включает отладку WebView для Chrome DevTools:

```java
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
    WebView.setWebContentsDebuggingEnabled(true);
}
```

### Подключение Chrome DevTools

1. **Запустите приложение** на эмуляторе
2. **Откройте Chrome** на компьютере
3. **Перейдите** по адресу: `chrome://inspect/#devices`
4. **Найдите** ваше приложение в списке
5. **Нажмите** "Inspect" для открытия DevTools

### Проверка работы JavaScript

#### Логи в консоли:

При запуске страницы вы увидите:
```
JavaScript injected successfully
AndroidInterface is available
Setting up additional click handlers
Found X anime links
```

#### Тестовые функции:

В консоли Chrome DevTools можно вызвать:
```javascript
// Тест интерфейса
window.testAnimeButton('/ru/anime/12345--test-anime/watch');

// Проверка найденных ссылок
document.querySelectorAll('a[href*="/ru/anime"]').length;
```

#### Проверка селекторов:

```javascript
// Найти кнопки с нужными классами (старый вариант)
document.querySelectorAll('a.btn.is-filled.variant-primary.rw_ci');

// Найти кнопки с альтернативными классами (новый вариант)
document.querySelectorAll('a.btn.is-filled.variant-primary.pt_c0');

// Найти все кнопки аниме (объединенный селектор)
document.querySelectorAll('a.btn.is-filled.variant-primary.rw_ci, a.btn.is-filled.variant-primary.pt_c0');

// Найти все ссылки аниме
document.querySelectorAll('a[href*="/ru/anime"]');
```

### Диагностика проблем

#### Если клики не перехватываются:

1. **Проверьте логи** в Android Studio (тег "JavaScriptInterface")
2. **Откройте DevTools** и проверьте консоль JavaScript
3. **Убедитесь**, что селекторы совпадают с реальными элементами
4. **Проверьте**, что `AndroidInterface` доступен

#### Возможные проблемы:

- **Селекторы не совпадают** - проверьте CSS классы на странице. Возможно используются альтернативные классы (`pt_c0` вместо `rw_ci`)
- **JavaScript не выполняется** - проверьте настройки WebView
- **Интерфейс недоступен** - проверьте порядок инициализации
- **События не доходят** - проверьте структуру DOM
- **Разные варианты кнопок** - сайт может использовать разные CSS классы для разных типов кнопок
- **Rate limiting (429)** - сервер блокирует частые запросы. Приложение автоматически повторяет с задержкой

## HTTP Range Requests и ExoPlayer

### Как работает Progressive Download

ExoPlayer автоматически управляет HTTP Range requests для эффективного progressive download:

#### Автоматическое управление диапазонами:
- ✅ **Начальный запрос**: ExoPlayer запрашивает начало файла
- ✅ **Буферизация**: Постепенно загружает дополнительные диапазоны
- ✅ **Оптимизация**: Управляет скоростью загрузки на основе скорости воспроизведения
- ✅ **Восстановление**: Перезапрашивает диапазоны при ошибках

#### Почему НЕ нужно устанавливать Range вручную:
- ❌ **Конфликт**: Ручной Range может конфликтовать с внутренней логикой ExoPlayer
- ❌ **Неэффективность**: ExoPlayer лучше знает, какие диапазоны нужны
- ❌ **Проблемы**: Может вызывать 429 ошибки или проблемы с воспроизведением

#### Правильный подход:
```java
// НЕ ДЕЛАТЬ:
headers.put("Range", "bytes=0-"); // ❌ Плохо

// ДАТЬ ExoPlayer самому управлять:
dataSource.setDefaultRequestProperties(headers); // ✅ Хорошо
```

## Система борьбы с Rate Limiting

### Автоматическая обработка ошибок 429

Приложение включает интеллектуальную систему для обхода ограничений сервера:

#### Задержки между запросами:
- ✅ **5-7 секунд** перед первым API запросом (рандомизировано)
- ✅ **4-6 секунд** перед вторым API запросом (рандомизировано)
- ✅ **3-5 секунд** перед запуском видеоплеера (рандомизировано)
- ✅ **Экспоненциальная задержка** при retry: 8-10 сек, 9-11 сек, 13-15 сек

#### Retry логика:
- ✅ **Автоматическое повторение** при ошибке 429
- ✅ **Максимум 3 попытки** с возрастающей задержкой
- ✅ **Умное распознавание** rate limit ошибок
- ✅ **Информативные сообщения** пользователю

#### Улучшенные HTTP заголовки:
```java
// Dynamic User-Agents (rotating)
"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36..."
"Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/121.0"
"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36... Edg/120.0.0.0"

// Dynamic Sec-CH-UA headers
"\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\""
"\"Not/A)Brand\";v=\"99\", \"Google Chrome\";v=\"120\", \"Chromium\";v=\"120\""
"\"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\", \"Not-A.Brand\";v=\"99\""

// Anti-rate-limit headers
headers.put("Accept-Encoding", "identity;q=1, *;q=0");
headers.put("Cache-Control", "no-cache");
headers.put("Pragma", "no-cache");
headers.put("DNT", "1");
// Note: Range header is handled by ExoPlayer automatically
// Don't set Range manually - it can interfere with ExoPlayer's internal range management
// headers.put("Range", "bytes=0-");
```

### Диагностика Rate Limiting

#### Логи успешной работы:
```
VideoPlayer: Waiting 6XXXms to avoid rate limiting...
VideoPlayer: Additional delay 5XXXms before episode details request
VideoPlayer: Delay 4XXXms before launching video player
VideoPlayer: Launching player with URL: https://...
```

#### Логи при rate limiting:
```
Rate limit detected, retrying in 9 seconds (attempt 1/3)
Rate limit detected, retrying in 10 seconds (attempt 2/3)
Rate limit detected, retrying in 14 seconds (attempt 3/3)
```

### Ручное тестирование

Если автоматический перехват не работает, можно добавить временный код для тестирования:

```javascript
// В консоли DevTools
let testLink = '/ru/anime/12345--test-anime/watch';
AndroidInterface.onAnimeButtonClick(testLink);
```

## Расширение функционала

## Системные требования

### Activity наследование

VideoPlayerActivity наследуется от `ComponentActivity` для обеспечения совместимости с:

- ✅ **Современными AndroidX API**
- ✅ **Material 3 компонентами**
- ✅ **Обработкой кнопки назад**
- ✅ **Жизненным циклом активности**

### Постоянный полноэкранный режим

Видеоплеер всегда работает в полноэкранном режиме:

- ✅ **Автоматический запуск** в landscape ориентации
- ✅ **Скрытые системные панели** (навигация, статус-бар)
- ✅ **Отсутствие кнопки** переключения полноэкранного режима
- ✅ **Кнопка "назад"** закрывает плеер
- ✅ **Черный фон** для лучшего видео-опыта

### Тема оформления

Специальная тема `Theme.AnimeLIB.VideoPlayer` обеспечивает:

- ✅ **Всегда полноэкранный режим**
- ✅ **Landscape ориентация** по умолчанию
- ✅ **Черный фон для лучшего видео-опыта**
- ✅ **Скрытые системные панели**
- ✅ **Совместимость с Material 3**

### Добавление субтитров

```java
MediaItem.SubtitleConfiguration subtitle =
    new MediaItem.SubtitleConfiguration.Builder(uri)
        .setMimeType(MimeTypes.TEXT_VTT)
        .setLanguage("en")
        .build();
```

### Качество видео

```java
TrackSelectionParameters parameters = player.getTrackSelectionParameters()
    .buildUpon()
    .setMaxVideoSizeSd()
    .build();
player.setTrackSelectionParameters(parameters);
```

### DRM защита

```java
HttpMediaDrmCallback drmCallback = new HttpMediaDrmCallback("license_url", okHttpClient);
MediaItem mediaItem = new MediaItem.Builder()
    .setUri(videoUri)
    .setDrmConfiguration(new MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
        .setLicenseUri("license_url")
        .setLicenseRequestHeaders(headers)
        .build())
    .build();
```
