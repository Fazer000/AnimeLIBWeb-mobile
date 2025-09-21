/**
 * Класс для обработки кнопки "Лицензирован"
 */
class LicenseButtonHandler {
    constructor() {
        this.isSetup = false;
        this.init();
    }

    /**
     * Инициализация обработчика
     */
    init() {
        if (this.isSetup) {
            console.log('[LicenseButtonHandler] Already setup');
            return;
        }

        this.isSetup = true;
        this.setupClickListener();
        this.setupTextReplacement();
        this.replaceExistingButtons();

        console.log('[LicenseButtonHandler] Setup completed');
    }

    /**
     * Настраивает обработчик кликов
     */
    setupClickListener() {
        document.addEventListener('click', (e) => {
            console.log('[LicenseButtonHandler] Click detected on:', e.target.tagName);

            let element = e.target;
            for (let i = 0; i < 5 && element; i++) {
                if (element.tagName === 'BUTTON') {
                    const buttonText = this.getButtonText(element);
                    console.log('[LicenseButtonHandler] Button text:', buttonText);
                    
                    if (buttonText.trim() === 'Лицензирован') {
                        console.log('[LicenseButtonHandler] Licensed button clicked');
                        const currentPageUrl = window.location.href;
                        e.preventDefault();
                        e.stopPropagation();
                        AndroidInterface.onPlayerButtonClicked(currentPageUrl);
                        break;
                    }
                }
                element = element.parentElement;
            }
        }, true);

        console.log('[LicenseButtonHandler] Click listener setup completed');
    }

    /**
     * Настраивает замену текста для динамически добавляемых кнопок
     */
    setupTextReplacement() {
        // MutationObserver для отслеживания новых кнопок
        const observer = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
                if (mutation.type === 'childList') {
                    mutation.addedNodes.forEach((node) => {
                        if (node.nodeType === Node.ELEMENT_NODE) {
                            // Проверяем добавленные кнопки
                            const buttons = node.querySelectorAll ? node.querySelectorAll('button') : [];
                            buttons.forEach((button) => {
                                this.replaceButtonText(button);
                            });
                            
                            // Проверяем саму добавленную ноду если это кнопка
                            if (node.tagName === 'BUTTON') {
                                this.replaceButtonText(node);
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

        console.log('[LicenseButtonHandler] Text replacement observer setup completed');
    }

    /**
     * Заменяет текст в существующих кнопках
     */
    replaceExistingButtons() {
        const buttons = document.querySelectorAll('button');
        buttons.forEach((button) => {
            this.replaceButtonText(button);
        });

        console.log('[LicenseButtonHandler] Existing buttons processed:', buttons.length);
    }

    /**
     * Заменяет текст кнопки "Лицензирован" на "Смотреть"
     */
    replaceButtonText(button) {
        const buttonText = this.getButtonText(button);
        
        if (buttonText.trim() === 'Лицензирован') {
            const span = button.querySelector('span');
            if (span) {
                span.textContent = 'Смотреть';
                console.log('[LicenseButtonHandler] Changed span text to "Смотреть"');
            } else {
                button.textContent = 'Смотреть';
                console.log('[LicenseButtonHandler] Changed button text to "Смотреть"');
            }
        }
    }

    /**
     * Получает текст кнопки (из span или самой кнопки)
     */
    getButtonText(button) {
        const span = button.querySelector('span');
        if (span) {
            return span.textContent || span.innerText || '';
        } else {
            return button.textContent || button.innerText || '';
        }
    }
}

// Инициализация
try {
    console.log('[LicenseButtonHandler] Starting initialization');
    
    if (window.licenseButtonHandler) {
        console.log('[LicenseButtonHandler] Already initialized');
    } else {
        window.licenseButtonHandler = new LicenseButtonHandler();
    }
    
    'setup_ok';
} catch (e) {
    console.error('[LicenseButtonHandler] Error:', e.message);
    'error: ' + e.message;
}