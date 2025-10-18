/**
 * Обработчик для получения auth из localStorage
 * Вызывается при загрузке страницы для синхронизации токена с приложением
 */

console.log('Auth handler loaded');

// Функция для получения auth из localStorage
function syncAuthToken() {

    try {
        // Проверяем что localStorage доступен
        if (typeof localStorage !== 'undefined') {
            const auth = localStorage.getItem('auth');
            console.log('Auth from localStorage:', auth ? 'Found' : 'Not found');
            
            if (auth && auth !== 'null' && auth !== 'undefined') {
                // Проверяем что это валидный JSON
                try {
                    const parsedAuth = JSON.parse(auth);
                    console.log('Parsed auth token');
                    
                    // Вызываем Android метод через интерфейс
                    if (window.AndroidInterface && typeof window.AndroidInterface.getAuthFromLocalStorage === 'function') {
                        window.AndroidInterface.getAuthFromLocalStorage();
                        console.log('Auth token sync requested');
                    } else {
                        console.error('AndroidInterface.getAuthFromLocalStorage not available');
                    }
                } catch (parseError) {
                    console.error('Error parsing auth JSON:', parseError);
                    console.log('Raw auth value:', auth.substring(0, 50));
                }
            } else {
                console.log('No auth found in localStorage');
                // Все равно вызываем Android метод для синхронизации состояния
                if (window.AndroidInterface && typeof window.AndroidInterface.getAuthFromLocalStorage === 'function') {
                    window.AndroidInterface.getAuthFromLocalStorage();
                }
            }
        } else {
            console.error('localStorage is not available');
        }
    } catch (error) {
        console.error('Error getting auth from localStorage:', error);
    }
}

// Вызываем синхронизацию при загрузке
syncAuthToken();

// Периодическая синхронизация токена (каждые 30 секунд)
setInterval(syncAuthToken, 30000);

console.log('Auth handler setup complete with periodic sync');
