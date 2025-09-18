package com.example.animelib.ui;

import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;

import com.example.animelib.VideoPlayerActivity;

/**
 * Класс для обработки нажатий кнопок плеера в WebView
 * Содержит JavaScript интерфейс и логику настройки слушателей
 */
public class PlayerButtonHandler {
    private static final String TAG = "PlayerButtonHandler";
    private static final String JS_INTERFACE_NAME = "AndroidInterface";
    
    private final Context context;
    
    public PlayerButtonHandler(Context context) {
        this.context = context;
    }
    
    /**
     * Добавляет JavaScript интерфейс к WebView
     */
    public void addJavaScriptInterface(WebView webView) {
        webView.addJavascriptInterface(new PlayerButtonJSInterface(), JS_INTERFACE_NAME);
        Log.d(TAG, "JavaScript interface added to WebView");
    }
    
    /**
     * Настраивает слушатели кнопок плеера для SPA приложений
     */
    public void setupPlayerButtonListeners(WebView webView) {
        Log.d(TAG, "Setting up SPA-aware player button listeners");

        // First test basic JavaScript
        webView.evaluateJavascript("'test'", value -> {
            Log.d(TAG, "Basic JS test: " + value);
        });

        // Simple and working JavaScript code
        webView.evaluateJavascript(
                "console.log('[AnimeLIB] Test log'); " +
                        "window.animelibTest = 'working'; " +
                        "'basic_test_ok'",
                value -> Log.d(TAG, "Basic test result: " + value)
        );

        // Simplified working JavaScript
        String jsCode = buildPlayerButtonListenerJS();

        // Execute the simplified JavaScript
        webView.evaluateJavascript(jsCode, value -> {
            Log.d(TAG, "Simple setup result: " + value);
            if (value == null || "null".equals(value)) {
                Log.e(TAG, "JavaScript returned null - syntax error!");
            }
        });

        // Setup aggressive button replacement system
        setupAggressiveButtonReplacement(webView);
    }
    
    /**
     * Настраивает агрессивную систему замены кнопок
     */
    private void setupAggressiveButtonReplacement(WebView webView) {
        Log.d(TAG, "Setting up aggressive button replacement system");
        
        // Setup DOM observer first
        String domObserverJS = buildImprovedDOMObserverJS();
        webView.evaluateJavascript(domObserverJS, value -> {
            Log.d(TAG, "Improved DOM observer setup result: " + value);
        });
        
        // Multiple replacement attempts with different delays
        int[] delays = {500, 1000, 2000, 3000, 5000};
        
        for (int delay : delays) {
            webView.postDelayed(() -> {
                String replacementJS = buildImprovedLicensedButtonReplacementJS();
                webView.evaluateJavascript(replacementJS, value -> {
                    Log.d(TAG, "Button replacement attempt (delay " + delay + "ms): " + value);
                });
            }, delay);
        }
        
        // Setup interval for continuous checking
        String intervalJS = buildContinuousReplacementJS();
        webView.evaluateJavascript(intervalJS, value -> {
            Log.d(TAG, "Continuous replacement setup: " + value);
        });
    }
    
    /**
     * Строит JavaScript код для перехвата нажатий кнопок плеера
     */
    private String buildPlayerButtonListenerJS() {
        return "try {" +
                "  console.log('[AnimeLIB] Starting simple setup');" +
                "  " +
                "  if (window.animelibSetup) {" +
                "    console.log('[AnimeLIB] Already setup');" +
                "  } else {" +
                "    window.animelibSetup = true;" +
                "    " +
                "    document.addEventListener('click', function(e) {" +
                "      console.log('[AnimeLIB] Click detected on: ' + e.target.tagName);" +
                "      " +
                "      var el = e.target;" +
                "      for (var i = 0; i < 5 && el; i++) {" +
                "        if (el.tagName === 'A') {" +
                "          var href = el.href || el.getAttribute('href') || '';" +
                "          console.log('[AnimeLIB] Link found: ' + href);" +
                "          " +
                "          if (href.includes('/watch') || href.includes('episode')) {" +
                "            console.log('[AnimeLIB] Player button clicked: ' + href);" +
                "            e.preventDefault();" +
                "            e.stopPropagation();" +
                "            " + JS_INTERFACE_NAME + ".onPlayerButtonClicked(href);" +
                "            break;" +
                "          }" +
                "        }" +
                "        el = el.parentElement;" +
                "      }" +
                "    }, true);" +
                "    " +
                "    console.log('[AnimeLIB] Simple setup complete');" +
                "  }" +
                "  'setup_ok';" +
                "} catch (e) {" +
                "  console.error('[AnimeLIB] Error: ' + e.message);" +
                "  'error: ' + e.message;" +
                "}";
    }
    
    /**
     * Улучшенная замена кнопок "Лицензирован" на "Смотреть"
     */
    private String buildImprovedLicensedButtonReplacementJS() {
        return "try {" +
                "  console.log('[AnimeLIB] 🔥 AGGRESSIVE button replacement started');" +
                "  " +
                "  var replacedCount = 0;" +
                "  var foundButtons = [];" +
                "  " +
                "  // Множественные селекторы для поиска кнопок" +
                "  var selectors = [" +
                "    'button'," +
                "    'button[type=\"button\"]'," +
                "    '.btn'," +
                "    '[class*=\"btn\"]'," +
                "    '*[class*=\"p4_cq\"]'," +
                "    '*'" +
                "  ];" +
                "  " +
                "  for (var s = 0; s < selectors.length; s++) {" +
                "    var elements = document.querySelectorAll(selectors[s]);" +
                "    console.log('[AnimeLIB] Selector \"' + selectors[s] + '\" found ' + elements.length + ' elements');" +
                "    " +
                "    for (var i = 0; i < elements.length; i++) {" +
                "      var element = elements[i];" +
                "      " +
                "      // Проверяем текст элемента и его детей" +
                "      var textContent = element.textContent || element.innerText || '';" +
                "      var innerHTML = element.innerHTML || '';" +
                "      " +
                "      if (textContent.includes('Лицензирован') || innerHTML.includes('Лицензирован')) {" +
                "        console.log('[AnimeLIB] 🎯 FOUND licensed element:', element.tagName, textContent.trim());" +
                "        foundButtons.push(element);" +
                "      }" +
                "    }" +
                "  }" +
                "  " +
                "  console.log('[AnimeLIB] Total found buttons: ' + foundButtons.length);" +
                "  " +
                "  // Заменяем найденные кнопки" +
                "  for (var j = 0; j < foundButtons.length; j++) {" +
                "    var button = foundButtons[j];" +
                "    " +
                "    try {" +
                "      var currentPath = window.location.pathname;" +
                "      var watchHref = currentPath + '/watch';" +
                "      " +
                "      console.log('[AnimeLIB] 🔄 Replacing button with href: ' + watchHref);" +
                "      " +
                "      // Создаем новую кнопку-ссылку" +
                "      var newButton = document.createElement('a');" +
                "      newButton.href = watchHref;" +
                "      newButton.className = 'btn is-filled variant-primary p4_cq';" +
                "      newButton.setAttribute('disabled', 'false');" +
                "      newButton.setAttribute('type', 'button');" +
                "      newButton.setAttribute('media', '[object Object]');" +
                "      newButton.style.cssText = button.style.cssText;" +
                "      " +
                "      // Копируем SVG иконку" +
                "      var svg = button.querySelector('svg');" +
                "      if (svg) {" +
                "        newButton.appendChild(svg.cloneNode(true));" +
                "      } else {" +
                "        // Создаем SVG если его нет" +
                "        var newSvg = document.createElement('svg');" +
                "        newSvg.className = 'svg-inline--fa fa-play fa-fw';" +
                "        newSvg.setAttribute('aria-hidden', 'true');" +
                "        newSvg.setAttribute('focusable', 'false');" +
                "        newSvg.setAttribute('data-prefix', 'fas');" +
                "        newSvg.setAttribute('data-icon', 'play');" +
                "        newSvg.setAttribute('role', 'img');" +
                "        newSvg.setAttribute('xmlns', 'http://www.w3.org/2000/svg');" +
                "        newSvg.setAttribute('viewBox', '0 0 384 512');" +
                "        var path = document.createElement('path');" +
                "        path.setAttribute('fill', 'currentColor');" +
                "        path.setAttribute('d', 'M73 39c-14.8-9.1-33.4-9.4-48.5-.9S0 62.6 0 80L0 432c0 17.4 9.4 33.4 24.5 41.9s33.7 8.1 48.5-.9L361 297c14.3-8.7 23-24.2 23-41s-8.7-32.2-23-41L73 39z');" +
                "        newSvg.appendChild(path);" +
                "        newButton.appendChild(newSvg);" +
                "      }" +
                "      " +
                "      // Создаем новый span с текстом 'Смотреть'" +
                "      var newSpan = document.createElement('span');" +
                "      newSpan.className = 'p4_gr p4_p6';" +
                "      newSpan.setAttribute('data-bookmark', 'false');" +
                "      newSpan.textContent = 'Смотреть';" +
                "      newButton.appendChild(newSpan);" +
                "      " +
                "      // Заменяем старую кнопку на новую" +
                "      if (button.parentNode) {" +
                "        button.parentNode.replaceChild(newButton, button);" +
                "        replacedCount++;" +
                "        console.log('[AnimeLIB] ✅ Successfully replaced button #' + replacedCount);" +
                "      }" +
                "    } catch (replaceError) {" +
                "      console.error('[AnimeLIB] ❌ Error replacing individual button:', replaceError.message);" +
                "    }" +
                "  }" +
                "  " +
                "  console.log('[AnimeLIB] 🎉 AGGRESSIVE replacement complete. Replaced: ' + replacedCount);" +
                "  'AGGRESSIVE_replaced_' + replacedCount;" +
                "} catch (e) {" +
                "  console.error('[AnimeLIB] ❌ AGGRESSIVE replacement error: ' + e.message);" +
                "  'AGGRESSIVE_error: ' + e.message;" +
                "}";
    }
    
    /**
     * Улучшенный DOM observer с агрессивным поиском
     */
    private String buildImprovedDOMObserverJS() {
        return "try {" +
                "  console.log('[AnimeLIB] 🔥 Setting up IMPROVED DOM observer');" +
                "  " +
                "  if (window.animelibObserver) {" +
                "    console.log('[AnimeLIB] Observer already exists, disconnecting old one');" +
                "    window.animelibObserver.disconnect();" +
                "  }" +
                "  " +
                "  // Агрессивная функция замены кнопок" +
                "  window.aggressiveReplaceLicensedButtons = function() {" +
                "    var replacedCount = 0;" +
                "    var foundButtons = [];" +
                "    " +
                "    // Ищем по всем возможным селекторам" +
                "    var selectors = ['button', '.btn', '[class*=\"btn\"]', '*[class*=\"p4_cq\"]', '*'];" +
                "    " +
                "    for (var s = 0; s < selectors.length; s++) {" +
                "      var elements = document.querySelectorAll(selectors[s]);" +
                "      for (var i = 0; i < elements.length; i++) {" +
                "        var element = elements[i];" +
                "        var textContent = element.textContent || element.innerText || '';" +
                "        var innerHTML = element.innerHTML || '';" +
                "        " +
                "        if ((textContent.includes('Лицензирован') || innerHTML.includes('Лицензирован')) && " +
                "            !element.hasAttribute('data-animelib-replaced')) {" +
                "          foundButtons.push(element);" +
                "          element.setAttribute('data-animelib-replaced', 'processing');" +
                "        }" +
                "      }" +
                "    }" +
                "    " +
                "    // Заменяем найденные кнопки" +
                "    for (var j = 0; j < foundButtons.length; j++) {" +
                "      var button = foundButtons[j];" +
                "      try {" +
                "        var currentPath = window.location.pathname;" +
                "        var watchHref = currentPath + '/watch';" +
                "        " +
                "        var newButton = document.createElement('a');" +
                "        newButton.href = watchHref;" +
                "        newButton.className = 'btn is-filled variant-primary p4_cq';" +
                "        newButton.setAttribute('disabled', 'false');" +
                "        newButton.setAttribute('type', 'button');" +
                "        newButton.setAttribute('media', '[object Object]');" +
                "        newButton.setAttribute('data-animelib-replaced', 'true');" +
                "        newButton.style.cssText = button.style.cssText;" +
                "        " +
                "        var svg = button.querySelector('svg');" +
                "        if (svg) {" +
                "          newButton.appendChild(svg.cloneNode(true));" +
                "        }" +
                "        " +
                "        var newSpan = document.createElement('span');" +
                "        newSpan.className = 'p4_gr p4_p6';" +
                "        newSpan.setAttribute('data-bookmark', 'false');" +
                "        newSpan.textContent = 'Смотреть';" +
                "        newButton.appendChild(newSpan);" +
                "        " +
                "        if (button.parentNode) {" +
                "          button.parentNode.replaceChild(newButton, button);" +
                "          replacedCount++;" +
                "          console.log('[AnimeLIB] 🔄 Observer replaced button: ' + watchHref);" +
                "        }" +
                "      } catch (replaceError) {" +
                "        console.error('[AnimeLIB] Observer replace error:', replaceError.message);" +
                "        button.setAttribute('data-animelib-replaced', 'error');" +
                "      }" +
                "    }" +
                "    return replacedCount;" +
                "  };" +
                "  " +
                "  // Создаем улучшенный MutationObserver" +
                "  window.animelibObserver = new MutationObserver(function(mutations) {" +
                "    var shouldReplace = false;" +
                "    " +
                "    mutations.forEach(function(mutation) {" +
                "      if (mutation.type === 'childList') {" +
                "        if (mutation.addedNodes.length > 0) {" +
                "          shouldReplace = true;" +
                "        }" +
                "      } else if (mutation.type === 'attributes') {" +
                "        shouldReplace = true;" +
                "      }" +
                "    });" +
                "    " +
                "    if (shouldReplace) {" +
                "      setTimeout(function() {" +
                "        var replaced = window.aggressiveReplaceLicensedButtons();" +
                "        if (replaced > 0) {" +
                "          console.log('[AnimeLIB] 🎯 Observer replaced ' + replaced + ' buttons');" +
                "        }" +
                "      }, 50);" +
                "    }" +
                "  });" +
                "  " +
                "  // Запускаем наблюдение с расширенными настройками" +
                "  window.animelibObserver.observe(document.documentElement, {" +
                "    childList: true," +
                "    subtree: true," +
                "    attributes: true," +
                "    attributeFilter: ['class', 'style']" +
                "  });" +
                "  " +
                "  console.log('[AnimeLIB] 🎉 IMPROVED DOM observer setup complete');" +
                "  'improved_observer_setup';" +
                "} catch (e) {" +
                "  console.error('[AnimeLIB] ❌ Improved observer error: ' + e.message);" +
                "  'improved_observer_error: ' + e.message;" +
                "}";
    }
    
    /**
     * Непрерывная проверка и замена кнопок
     */
    private String buildContinuousReplacementJS() {
        return "try {" +
                "  console.log('[AnimeLIB] 🔄 Setting up continuous replacement system');" +
                "  " +
                "  if (window.animelibInterval) {" +
                "    clearInterval(window.animelibInterval);" +
                "  }" +
                "  " +
                "  var checkCount = 0;" +
                "  " +
                "  window.animelibInterval = setInterval(function() {" +
                "    checkCount++;" +
                "    " +
                "    if (typeof window.aggressiveReplaceLicensedButtons === 'function') {" +
                "      var replaced = window.aggressiveReplaceLicensedButtons();" +
                "      if (replaced > 0) {" +
                "        console.log('[AnimeLIB] 🔄 Interval check #' + checkCount + ' replaced ' + replaced + ' buttons');" +
                "      }" +
                "    }" +
                "    " +
                "    // Останавливаем через 2 минуты чтобы не грузить систему" +
                "    if (checkCount > 120) {" +
                "      clearInterval(window.animelibInterval);" +
                "      console.log('[AnimeLIB] 🛑 Continuous replacement stopped after 2 minutes');" +
                "    }" +
                "  }, 1000);" +
                "  " +
                "  console.log('[AnimeLIB] 🎯 Continuous replacement started (1 second intervals)');" +
                "  'continuous_setup';" +
                "} catch (e) {" +
                "  console.error('[AnimeLIB] ❌ Continuous replacement error: ' + e.message);" +
                "  'continuous_error: ' + e.message;" +
                "}";
    }
    
    /**
     * JavaScript интерфейс для обработки нажатий кнопок плеера
     */
    private class PlayerButtonJSInterface {
        @OptIn(markerClass = UnstableApi.class)
        @JavascriptInterface
        public void onPlayerButtonClicked(String buttonHref) {
            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).runOnUiThread(() -> {
                    Log.d(TAG, "Player button clicked: " + buttonHref);
                    Log.d("PlayerHandler", "Starting VideoPlayerActivity for URL: " + buttonHref);
                    VideoPlayerActivity.startFromAnimePage((android.app.Activity) context, buttonHref);
                });
            } else {
                Log.e(TAG, "Context is not an Activity, cannot start VideoPlayerActivity");
            }
        }
    }
}
