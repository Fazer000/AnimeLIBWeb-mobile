/**
 * Domain Change Handler
 * Отслеживает переходы на другие домены и показывает спиннер
 */

class DomainChangeHandler {
    constructor() {
        this.currentDomain = null;
        this.isInitialized = false;
        this.init();
    }

    init() {
        if (this.isInitialized) return;
        
        // Получаем текущий домен
        this.currentDomain = this.extractDomain(window.location.href);
        console.log('DomainChangeHandler: Initialized with domain:', this.currentDomain);
        
        // Отслеживаем изменения URL
        this.setupUrlChangeListener();
        
        // Отслеживаем клики по ссылкам
        this.setupLinkClickListener();
        
        this.isInitialized = true;
    }

    /**
     * Извлекает домен из URL
     */
    extractDomain(url) {
        try {
            const urlObj = new URL(url);
            return urlObj.hostname;
        } catch (e) {
            console.warn('DomainChangeHandler: Failed to extract domain from:', url);
            return null;
        }
    }

    /**
     * Настраивает отслеживание изменений URL
     */
    setupUrlChangeListener() {
        // Отслеживаем изменения через history API
        const originalPushState = history.pushState;
        const originalReplaceState = history.replaceState;

        history.pushState = function(...args) {
            originalPushState.apply(history, args);
            // Небольшая задержка чтобы дать время на изменение URL
            setTimeout(() => this.handleUrlChange(), 10);
        }.bind(this);

        history.replaceState = function(...args) {
            originalReplaceState.apply(history, args);
            // Небольшая задержка чтобы дать время на изменение URL
            setTimeout(() => this.handleUrlChange(), 10);
        }.bind(this);

        // Отслеживаем событие popstate (навигация назад/вперед)
        window.addEventListener('popstate', () => {
            setTimeout(() => this.handleUrlChange(), 10);
        });

        // Отслеживаем изменения через hashchange
        window.addEventListener('hashchange', () => {
            setTimeout(() => this.handleUrlChange(), 10);
        });

        // Отслеживаем изменения через MutationObserver для динамических изменений
        this.setupMutationObserver();

        console.log('DomainChangeHandler: URL change listener setup completed');
    }

    /**
     * Настраивает отслеживание кликов по ссылкам
     */
    setupLinkClickListener() {
        // Используем capture: true для перехвата событий раньше
        document.addEventListener('click', (event) => {
            const link = event.target.closest('a');
            if (!link || !link.href) return;

            const targetDomain = this.extractDomain(link.href);
            if (!targetDomain) return;

            // Проверяем переход на другой домен
            if (targetDomain !== this.currentDomain) {
                console.log('DomainChangeHandler: Domain change detected:', this.currentDomain, '->', targetDomain);
                // Показываем спиннер немедленно
                this.showSpinner();
                // Обновляем текущий домен
                this.currentDomain = targetDomain;
            }
        }, true); // capture: true

        // Также отслеживаем события touchstart для мобильных устройств
        document.addEventListener('touchstart', (event) => {
            const link = event.target.closest('a');
            if (!link || !link.href) return;

            const targetDomain = this.extractDomain(link.href);
            if (!targetDomain) return;

            // Проверяем переход на другой домен
            if (targetDomain !== this.currentDomain) {
                console.log('DomainChangeHandler: Touch domain change detected:', this.currentDomain, '->', targetDomain);
                this.showSpinner();
                this.currentDomain = targetDomain;
            }
        }, true);

        console.log('DomainChangeHandler: Link click listener setup completed');
    }

    /**
     * Настраивает MutationObserver для отслеживания динамических изменений
     */
    setupMutationObserver() {
        const observer = new MutationObserver((mutations) => {
            mutations.forEach((mutation) => {
                if (mutation.type === 'childList') {
                    mutation.addedNodes.forEach((node) => {
                        if (node.nodeType === Node.ELEMENT_NODE) {
                            // Проверяем добавленные ссылки
                            const links = node.querySelectorAll ? node.querySelectorAll('a[href]') : [];
                            links.forEach((link) => {
                                const href = link.getAttribute('href');
                                if (href) {
                                    const targetDomain = this.extractDomain(href);
                                    if (targetDomain && targetDomain !== this.currentDomain) {
                                        console.log('DomainChangeHandler: External link detected in dynamic content:', href);
                                        // Добавляем обработчик клика к новой ссылке
                                        link.addEventListener('click', (e) => {
                                            console.log('DomainChangeHandler: Dynamic link clicked:', href);
                                            this.showSpinner();
                                        });
                                    }
                                }
                            });
                        }
                    });
                }
            });
        });

        observer.observe(document.body, {
            childList: true,
            subtree: true
        });

        console.log('DomainChangeHandler: MutationObserver setup completed');
    }

    /**
     * Обрабатывает изменение URL
     */
    handleUrlChange() {
        const newDomain = this.extractDomain(window.location.href);
        if (!newDomain) return;

        if (newDomain !== this.currentDomain) {
            console.log('DomainChangeHandler: URL change detected:', this.currentDomain, '->', newDomain);
            this.currentDomain = newDomain;
            this.showSpinner();
        }
    }

    /**
     * Показывает спиннер через Android интерфейс
     */
    showSpinner() {
        if (window.AndroidInterface && window.AndroidInterface.showDomainChangeSpinner) {
            console.log('DomainChangeHandler: Showing spinner for domain change');
            window.AndroidInterface.showDomainChangeSpinner();
        } else {
            console.warn('DomainChangeHandler: AndroidInterface.showDomainChangeSpinner not available');
        }
    }

    /**
     * Скрывает спиннер через Android интерфейс
     */
    hideSpinner() {
        if (window.AndroidInterface && window.AndroidInterface.hideDomainChangeSpinner) {
            console.log('DomainChangeHandler: Hiding spinner');
            window.AndroidInterface.hideDomainChangeSpinner();
        } else {
            console.warn('DomainChangeHandler: AndroidInterface.hideDomainChangeSpinner not available');
        }
    }

    /**
     * Обновляет текущий домен (для внешнего использования)
     */
    updateCurrentDomain(url) {
        const newDomain = this.extractDomain(url);
        if (newDomain && newDomain !== this.currentDomain) {
            console.log('DomainChangeHandler: Manually updating domain:', this.currentDomain, '->', newDomain);
            this.currentDomain = newDomain;
        }
    }
}

// Создаем глобальный экземпляр
window.domainChangeHandler = new DomainChangeHandler();

console.log('DomainChangeHandler: Script loaded and initialized');
