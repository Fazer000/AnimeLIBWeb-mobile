(function() {
    'use strict';
    
    console.log('[Search Button Listener] Initializing...');
    
    let observerActive = false;
    let clickHandlersAttached = new WeakSet();
    
    // Функция для установки обработчика клика на кнопку поиска
    function attachSearchButtonHandler(button) {
        if (!button || clickHandlersAttached.has(button)) {
            return;
        }
        
        console.log('[Search Button Listener] Attaching click handler to search button');
        
        button.addEventListener('click', function(e) {
            e.preventDefault();
            e.stopPropagation();
            
            console.log('[Search Button Listener] Search button clicked!');
            
            // Вызываем метод Android интерфейса
            if (typeof AndroidInterface !== 'undefined' && AndroidInterface.onSearchButtonClicked) {
                AndroidInterface.onSearchButtonClicked();
            } else {
                console.error('[Search Button Listener] AndroidInterface not found');
            }
        }, true);
        
        clickHandlersAttached.add(button);
        console.log('[Search Button Listener] Click handler attached successfully');
    }
    
    // Функция для поиска и обработки всех кнопок поиска
    function findAndAttachSearchButtons() {
        // Ищем элементы с классом cm_ct, которые содержат SVG с иконкой поиска
        const searchButtons = document.querySelectorAll('.cm_ct');
        
        console.log('[Search Button Listener] Found ' + searchButtons.length + ' potential search buttons');
        
        searchButtons.forEach(function(button) {
            // Проверяем, содержит ли кнопка SVG с иконкой лупы
            const searchIcon = button.querySelector('svg.fa-magnifying-glass');
            const searchText = button.querySelector('span');
            
            if (searchIcon && searchText && searchText.textContent.includes('Быстрый поиск')) {
                console.log('[Search Button Listener] Found valid search button');
                attachSearchButtonHandler(button);
            }
        });
    }
    
    // Наблюдатель за изменениями DOM для SPA
    function startObserver() {
        if (observerActive) {
            console.log('[Search Button Listener] Observer already active');
            return;
        }
        
        const observer = new MutationObserver(function(mutations) {
            let shouldCheck = false;
            
            mutations.forEach(function(mutation) {
                if (mutation.addedNodes.length > 0) {
                    mutation.addedNodes.forEach(function(node) {
                        if (node.nodeType === 1) { // ELEMENT_NODE
                            if (node.classList && node.classList.contains('cm_ct')) {
                                shouldCheck = true;
                            } else if (node.querySelector && node.querySelector('.cm_ct')) {
                                shouldCheck = true;
                            }
                        }
                    });
                }
            });
            
            if (shouldCheck) {
                console.log('[Search Button Listener] DOM changed, checking for search buttons...');
                findAndAttachSearchButtons();
            }
        });
        
        observer.observe(document.body, {
            childList: true,
            subtree: true
        });
        
        observerActive = true;
        console.log('[Search Button Listener] Observer started');
    }
    
    // Инициализация при загрузке
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function() {
            console.log('[Search Button Listener] DOM loaded, initializing...');
            findAndAttachSearchButtons();
            startObserver();
        });
    } else {
        console.log('[Search Button Listener] DOM already loaded, initializing immediately...');
        findAndAttachSearchButtons();
        startObserver();
    }
    
    // Дополнительная проверка через небольшой таймаут на случай SPA
    setTimeout(function() {
        console.log('[Search Button Listener] Delayed check for search buttons...');
        findAndAttachSearchButtons();
    }, 1000);
    
    console.log('[Search Button Listener] Initialization complete');
})();

