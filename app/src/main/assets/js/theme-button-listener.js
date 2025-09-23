// Слушатель для кнопки "Тема" в мобильном меню
try {
  console.log('[AnimeLIB] Starting theme button listener');

  if (window.animelibThemeButtonSetup) {
    console.log('[AnimeLIB] Theme button listener already setup');
  } else {
    window.animelibThemeButtonSetup = true;

    document.addEventListener('click', function (e) {
      // Используем closest для поиска родительского элемента с классом ue_ui
      const themeButton = e.target.closest('.ub_ug');

      // Проверяем, содержит ли кнопка span с текстом "Тема"
      const themeSpan = themeButton.querySelector('span');
      if (themeSpan && themeSpan.textContent.trim() === 'Тема') {
        console.log('[AnimeLIB] Theme button clicked!');
        console.log('Button details:', themeButton)
        e.preventDefault();
        e.stopPropagation();
        AndroidInterface.onThemeButtonClicked();
        return;
      }
    }, true); // Используем capture: true для перехвата событий

    console.log('[AnimeLIB] Theme button listener setup complete');
  }
  'theme_setup_ok';
} catch (e) {
  console.error('[AnimeLIB] Theme button listener error: ' + e.message);
  'theme_error: ' + e.message;
}