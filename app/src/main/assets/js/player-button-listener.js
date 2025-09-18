// Слушатель для кнопок плеера (ссылки /watch, /episode)
try {
  console.log('[AnimeLIB] Starting player button listener');

  if (window.animelibPlayerSetup) {
    console.log('[AnimeLIB] Player listener already setup');
  } else {
    window.animelibPlayerSetup = true;

    document.addEventListener('click', function (e) {
      console.log('[AnimeLIB] Player click detected on: ' + e.target.tagName);

      let el = e.target;
      for (let i = 0; i < 5 && el; i++) {
        if (el.tagName === 'A') {
          let href = el.href || el.getAttribute('href') || '';
          console.log('[AnimeLIB] Player link found: ' + href);

          if (href.includes('/watch') || href.includes('episode')) {
            console.log('[AnimeLIB] Player button clicked: ' + href);
            e.preventDefault();
            e.stopPropagation();
            AndroidInterface.onPlayerButtonClicked(href);
            break;
          }
        }
        el = el.parentElement;
      }
    }, true);

    console.log('[AnimeLIB] Player listener setup complete');
  }
  'player_setup_ok';
} catch (e) {
  console.error('[AnimeLIB] Player listener error: ' + e.message);
  'player_error: ' + e.message;
}
