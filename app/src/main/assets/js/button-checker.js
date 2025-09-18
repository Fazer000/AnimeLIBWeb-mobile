// Проверка кнопок при загрузке DOM
document.addEventListener('DOMContentLoaded', function () {
    try {
        console.log('[AnimeLIB] DOMContentLoaded: Checking buttons');
        let buttons = document.querySelectorAll('button');
        console.log('[AnimeLIB] DOMContentLoaded: Found ' + buttons.length + ' buttons');

        for (let i = 0; i < buttons.length; i++) {
            let button = buttons[i];
            let text = button.textContent || button.innerText || '';
            console.log('[AnimeLIB] DOMContentLoaded: Button ' + i + ' text: "' + text.trim() + '" class: "' + button.className + '"');
        }
    } catch (e) {
        console.error('[AnimeLIB] DOMContentLoaded error: ' + e.message);
    }
});
'DOMContentLoaded_listener_added';
