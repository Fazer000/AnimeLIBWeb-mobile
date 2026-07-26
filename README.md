# AnimeLIB

Android приложение для просмотра аниме с удобным интерфейсом и современными возможностями воспроизведения видео.

## 📱 Описание

AnimeLIB - это мобильное приложение для Android, представляющее собой веб-порт сайта animelib.org через WebView с интегрированным кастомным видеоплеером. Приложение предоставляет пользователям доступ к обширной библиотеке аниме с удобной навигацией, качественным воспроизведением видео и дополнительными функциями, специально адаптированными для мобильных устройств.

## ✨ Основные возможности

- 🌐 **WebView интеграция** - полный доступ к сайту animelib.org через встроенный браузер
- 🎥 **Кастомный видеоплеер** - собственный плеер с расширенными возможностями управления
- 📱 **Мобильная адаптация** - оптимизированный интерфейс для сенсорных устройств
- 🎬 **Просмотр аниме** - доступ к обширной библиотеке аниме
- 📖 **Управление эпизодами** - удобная навигация по сериям
- 💾 **Локальное хранение** - сохранение настроек и закладок
- 🔄 **Обновление контента** - автоматическое обновление списков
- 📝 **Комментарии** - система комментариев к эпизодам
- ⚙️ **Настройки** - гибкая настройка качества видео и других параметров
- 🎮 **Жесты управления** - управление плеером с помощью жестов
- ⬇️ **Скачивание серии** - сохранение текущей серии в выбранном качестве

## 🛠 Технические характеристики

### Минимальные требования
- **Android**: 10.0 (API 29) и выше
- **RAM**: 2GB рекомендуется
- **Место на диске**: 50MB для установки

### Используемые технологии
- **Язык**: Java
- **Минимальная версия SDK**: 29
- **Целевая версия SDK**: 35
- **Архитектура**: MVVM с использованием Android Architecture Components
- **WebView**: Встроенный браузер для отображения веб-контента
- **JavaScript Bridge**: Связь между WebView и нативным кодом
- **Кастомный плеер**: ExoPlayer/Media3 для воспроизведения видео
- **Сборка**: Gradle 8.13 (wrapper), Android Gradle Plugin 8.13.0
- **Зависимости**: version catalog — `gradle/libs.versions.toml`

## 📦 Зависимости

### Основные библиотеки
- **AndroidX Core** - основные компоненты Android
- **Material Design** - современный дизайн интерфейса
- **Room** - локальная база данных
- **ExoPlayer/Media3** - воспроизведение видео
- **OkHttp** - сетевые запросы
- **Gson** - парсинг JSON
- **JSoup** - парсинг HTML
- **SwipeRefreshLayout** - обновление контента

### Полный список зависимостей
```gradle
implementation 'androidx.core:core:1.13.1'
implementation 'androidx.appcompat:appcompat:1.6.1'
implementation 'com.google.android.material:material:1.10.0'
implementation 'androidx.room:room-runtime:2.6.1'
implementation 'androidx.lifecycle:lifecycle-viewmodel:2.7.0'
implementation 'androidx.lifecycle:lifecycle-livedata:2.7.0'
implementation 'androidx.media3:media3-exoplayer:1.4.1'
implementation 'androidx.media3:media3-ui:1.4.1'
implementation 'androidx.media3:media3-common:1.4.1'
implementation 'androidx.media3:media3-datasource-okhttp:1.4.1'
implementation 'androidx.media3:media3-exoplayer-hls:1.8.0'
implementation 'com.squareup.okhttp3:okhttp:4.12.0'
implementation 'com.google.code.gson:gson:2.10.1'
implementation 'org.jsoup:jsoup:1.17.2'
implementation 'androidx.swiperefreshlayout:swiperefreshlayout:1.1.0'
implementation 'androidx.core:core-splashscreen:1.0.1'
annotationProcessor 'androidx.room:room-compiler:2.6.1'
```

> Версии объявлены в `gradle/libs.versions.toml`, подключение — в `app/build.gradle`.

## 🚀 Установка и запуск

### Предварительные требования
- Android Studio с поддержкой AGP 8.13 (Narwhal 3 Feature Drop / 2025.1.3 или новее)
- JDK 17 или выше (требование AGP 8.13; проект собирался на JDK 21)
- Android SDK Platform 35 и SDK Build-Tools 35.0.0

### Установка
1. Клонируйте репозиторий:
```bash
git clone https://github.com/Fazer000/AnimeLib-Mobile.git
cd AnimeLib-Mobile
```

2. Откройте проект в Android Studio

3. Синхронизируйте Gradle файлы

4. Соберите проект:
```bash
./gradlew assembleDebug
```
На Windows используйте `.\gradlew.bat`, на Linux/macOS может потребоваться `chmod +x gradlew`.

Готовый APK: `app/build/outputs/apk/debug/AnimeLib-vX.X.X-debug.apk`

5. Установите APK на устройство:
```bash
./gradlew installDebug
```

## 📁 Структура проекта

```
app/
├── src/main/
│   ├── java/com/example/animelib/
│   │   ├── adapters/          # Адаптеры для RecyclerView
│   │   ├── api/               # API сервисы и модели
│   │   ├── data/              # База данных и настройки
│   │   ├── dialogs/           # Диалоговые окна
│   │   ├── managers/          # Менеджеры функциональности
│   │   ├── models/            # Модели данных
│   │   ├── settings/          # Настройки приложения
│   │   ├── ui/                # UI компоненты и WebView интеграция
│   │   ├── util/              # Утилиты
│   │   ├── viewmodel/         # ViewModels
│   │   ├── MainActivity.java  # Главная активность с WebView
│   │   ├── VideoPlayerActivity.java # Активность кастомного видеоплеера
│   │   ├── SearchFragment.java      # Поиск по каталогу
│   │   └── UrlInputActivity.java    # Ручной ввод адреса сайта
│   ├── res/                   # Ресурсы приложения
│   ├── assets/js/             # JavaScript файлы для WebView (bridge, обработчики кнопок)
│   └── assets/html/           # Разметка, вставляемая в страницу
```

## 🔧 Конфигурация

### Настройка API
В файле `app/src/main/res/values/strings.xml` задаётся:
- `site_url` — адрес сайта (может быть переопределён пользователем в приложении)
- `bearer_token` — устаревший резервный токен

Рабочий токен приложение получает само: после входа на сайте внутри WebView скрипт
`assets/js/auth-handler.js` забирает `auth` из `localStorage` и передаёт его в Android,
откуда токен сохраняется в Room. **До входа в аккаунт запросы к API будут отклоняться.**

### Разрешения
Приложение запрашивает следующие разрешения:
- `INTERNET` - для сетевых запросов
- `ACCESS_NETWORK_STATE` - проверка состояния сети
- `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` - запросы геолокации со стороны сайта
- `WRITE_EXTERNAL_STORAGE` (до API 28), `READ_EXTERNAL_STORAGE` (до API 32) - сохранение файлов
- `CAMERA` - доступ к камере
- `RECORD_AUDIO` - запись аудио
- `MODIFY_AUDIO_SETTINGS` - управление громкостью в плеере
- `POST_NOTIFICATIONS` - уведомление о прогрессе скачивания (Android 13+)
- `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC` - скачивание в фоне

## 🎮 Использование

1. **Запуск приложения** - откройте AnimeLIB на вашем устройстве
2. **Навигация по сайту** - используйте WebView для просмотра сайта animelib.org
3. **Поиск аниме** - используйте поиск для нахождения интересующих сериалов
4. **Выбор эпизода** - выберите серию для просмотра
5. **Переход к плееру** - нажмите кнопку "Смотреть" для запуска кастомного плеера
6. **Настройка качества** - настройте качество видео в настройках плеера
7. **Управление воспроизведением** - используйте жесты для управления плеером
8. **Скачивание серии** - кнопка со стрелкой в нижней панели плеера: выберите
      качество, повторное нажатие во время загрузки отменяет её

### Скачивание
Файлы кладутся в системную папку «Загрузки», подпапка `AnimeLIB`, через
`MediaStore.Downloads` — разрешения на запись не нужны, файл виден в любом
файловом менеджере. Одновременно качается одна серия: повторный `ACTION_START` при активной загрузке
игнорируется. Доступно только для озвучек AnimeLib (прямые файлы по качеству);
Kodik отдаёт HLS и требует отдельного загрузчика с получением сегментов.

## 🐛 Известные проблемы

- Некоторые видео могут не воспроизводиться из-за региональных ограничений
- При медленном интернете может потребоваться время для загрузки

## 🤝 Вклад в проект

Мы приветствуем вклад в развитие проекта! Для этого:

1. Форкните репозиторий
2. Создайте ветку для новой функции (`git checkout -b feature/AmazingFeature`)
3. Зафиксируйте изменения (`git commit -m 'Add some AmazingFeature'`)
4. Отправьте в ветку (`git push origin feature/AmazingFeature`)
5. Откройте Pull Request в ветку `dev` (основная ветка разработки)

## 📄 Лицензия

Этот проект распространяется под лицензией MIT. См. файл `LICENSE` для получения дополнительной информации.

## 📞 Поддержка

Если у вас возникли вопросы или проблемы:
- Создайте Issue в репозитории
- Обратитесь к документации
- Проверьте раздел "Известные проблемы"

## 🔄 Обновления

Следите за обновлениями проекта:
- Проверяйте релизы в GitHub

---

**Разработчик**: Fazer  
**Сайт принадлежит**: animelib.org (ООО "Мангалиб")
