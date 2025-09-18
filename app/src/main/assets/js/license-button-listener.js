try {
  console.log('[AnimeLIB] Starting simple setup');

  if (window.animelibSetup) {
    console.log('[AnimeLIB] Already setup');
  } else {
    window.animelibSetup = true;

    // Слушатель для кликов, как было
    document.addEventListener('click', function (e) {
      console.log('[AnimeLIB] Click detected on: ' + e.target.tagName);

      let el = e.target;
      for (let i = 0; i < 5 && el; i++) {
        if (el.tagName === 'BUTTON') {
          // Проверяем текст внутри <span>
          let span = el.querySelector('span');
          let text = '';
          if (span) {
              text = span.textContent || span.innerText || '';
          } else {
              // Если <span> нет, проверяем текст самой кнопки
              text = el.textContent || el.innerText || '';
          }
          console.log('[AnimeLIB] Button or span text:', text);
          if (text.trim() === 'Лицензирован') {
              console.log('Licensed clicked');
              var currentPageUrl = window.location.href;
              e.preventDefault();
              e.stopPropagation();
              AndroidInterface.onPlayerButtonClicked(currentPageUrl);
              break;
          }
        }
        el = el.parentElement;
      }
    }, true);

    // Новый слушатель для DOMContentLoaded, чтобы поменять текст
    document.addEventListener('DOMContentLoaded', function () {
      console.log('[AnimeLIB] DOMContentLoaded fired');
      // Ищем все кнопки
      const buttons = document.querySelectorAll('button');
      buttons.forEach(button => {
        let span = button.querySelector('span');
        let text = '';
        if (span) {
          text = span.textContent || span.innerText || '';
          if (text.trim() === 'Лицензирован') {
            span.textContent = 'Смотреть';
            console.log('[AnimeLIB] Changed span text to "Смотреть" in button:', button.outerHTML);
          }
        } else {
          text = button.textContent || button.innerText || '';
          if (text.trim() === 'Лицензирован') {
            button.textContent = 'Смотреть';
            console.log('[AnimeLIB] Changed button text to "Смотреть":', button.outerHTML);
          }
        }
      });
    });

    console.log('[AnimeLIB] Simple setup complete');
  }
  'setup_ok';
} catch (e) {
  console.error('[AnimeLIB] Error: ' + e.message);
  'error: ' + e.message;
}