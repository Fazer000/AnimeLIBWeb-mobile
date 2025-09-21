/**
 * Класс для обработки кнопки "Назад" на сайте
 */
class BackButtonHandler {
    constructor() {
        this.isSetup = false;
        this.init();
    }

    /**
     * Инициализация обработчика
     */
    init() {
        if (this.isSetup) {
            console.log('[BackButtonHandler] Already setup');
            return;
        }

        this.isSetup = true;
        this.setupClickListener();
        this.setupMutationObserver();
        this.findAndSetupExistingButtons();

        console.log('[BackButtonHandler] Setup completed');
    }

    /**
     * Настраивает обработчик кликов
     */
    setupClickListener() {
        document.addEventListener('click', (e) => {
            console.log('[BackButtonHandler] Click detected on:', e.target.tagName);

            let element = e.target;
            for (let i = 0; i < 5 && element; i++) {
                if (element.tagName === 'BUTTON') {
                    if (this.isBackButton(element)) {
                        console.log('[BackButtonHandler] Back button clicked');
                        e.preventDefault();
                        e.stopPropagation();
                        AndroidInterface.handleOnBackPressed();
                        break;
                    }
                }
                element = element.parentElement;
            }
        }, true);

        console.log('[BackButtonHandler] Click listener setup completed');
    }

    /**
     * Настраивает MutationObserver для отслеживания новых кнопок
     */
    setupMutationObserver() {
        const observer = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
                if (mutation.type === 'childList') {
                    mutation.addedNodes.forEach((node) => {
                        if (node.nodeType === Node.ELEMENT_NODE) {
                            // Проверяем добавленные кнопки
                            const buttons = node.querySelectorAll ? node.querySelectorAll('button') : [];
                            buttons.forEach((button) => {
                                this.setupBackButton(button);
                            });
                            
                            // Проверяем саму добавленную ноду если это кнопка
                            if (node.tagName === 'BUTTON') {
                                this.setupBackButton(node);
                            }
                        }
                    });
                }
            });
        });

        observer.observe(document.body, {
            childList: true,
            subtree: true
        });

        console.log('[BackButtonHandler] MutationObserver setup completed');
    }

    /**
     * Находит и настраивает существующие кнопки "Назад"
     */
    findAndSetupExistingButtons() {
        const buttons = document.querySelectorAll('button');
        let backButtonCount = 0;
        
        buttons.forEach((button) => {
            if (this.isBackButton(button)) {
                this.setupBackButton(button);
                backButtonCount++;
            }
        });

        console.log('[BackButtonHandler] Existing back buttons found and setup:', backButtonCount);
    }

    /**
     * Настраивает кнопку "Назад" для перехвата
     */
    setupBackButton(button) {
        if (this.isBackButton(button)) {
            // Добавляем обработчик клика напрямую к кнопке
            button.addEventListener('click', (e) => {
                console.log('[BackButtonHandler] Direct back button click intercepted');
                e.preventDefault();
                e.stopPropagation();
                AndroidInterface.handleOnBackPressed();
            }, true);

            console.log('[BackButtonHandler] Back button setup completed');
        }
    }

    /**
     * Проверяет, является ли кнопка кнопкой "Назад"
     */
    isBackButton(button) {
        // Проверяем классы кнопки
        const classList = button.classList;
        if (!classList.contains('btn') || !classList.contains('is-icon') || !classList.contains('is-rounded') || !classList.contains('variant-header')) {
            return false;
        }

        // Проверяем наличие SVG с иконкой стрелки влево
        const svg = button.querySelector('svg');
        if (!svg) {
            return false;
        }

        // Проверяем класс SVG или data-icon
        const svgClass = svg.classList.contains('svg-inline--fa');
        const dataIcon = svg.getAttribute('data-icon') === 'arrow-left';
        const dataPrefix = svg.getAttribute('data-prefix') === 'fas';

        if (svgClass && dataIcon && dataPrefix) {
            return true;
        }

        // Дополнительная проверка по path в SVG
        const path = svg.querySelector('path');
        if (path) {
            const pathData = path.getAttribute('d');
            // Проверяем характерный path для иконки arrow-left
            if (pathData && pathData.includes('M9.4 233.4c-12.5 12.5-12.5 32.8 0 45.3l160 160c12.5 12.5 32.8 12.5 45.3 0s12.5-32.8 0-45.3L109.2 288 416 288c17.7 0 32-14.3 32-32s-14.3-32-32-32l-306.7 0L214.6 118.6c12.5-12.5 12.5-32.8 0-45.3s-32.8-12.5-45.3 0l-160 160z')) {
                return true;
            }
        }

        return false;
    }
}

// Инициализация
try {
    console.log('[BackButtonHandler] Starting initialization');
    
    if (window.backButtonHandler) {
        console.log('[BackButtonHandler] Already initialized');
    } else {
        window.backButtonHandler = new BackButtonHandler();
    }
    
    'setup_ok';
} catch (e) {
    console.error('[BackButtonHandler] Error:', e.message);
    'error: ' + e.message;
}
