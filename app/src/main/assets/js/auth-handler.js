/**
 * Обработчик для получения auth из localStorage
 */

console.log('Auth handler loaded');

// Функция для получения auth из localStorage
try {
    // Проверяем что localStorage доступен
    if (typeof localStorage !== 'undefined') {
        const auth = localStorage.getItem('auth');
        console.log('Auth from localStorage:', auth);
        
        if (auth && auth !== 'null' && auth !== 'undefined') {
            // Проверяем что это валидный JSON
            try {
                const parsedAuth = JSON.parse(auth);
                console.log('Parsed auth:', parsedAuth);
                
                // Вызываем Android метод через интерфейс
                if (window.AndroidInterface && typeof window.AndroidInterface.getAuthFromLocalStorage === 'function') {
                    window.AndroidInterface.getAuthFromLocalStorage();
                } else {
                    console.error('AndroidInterface.getAuthFromLocalStorage not available');
                }
            } catch (parseError) {
                console.error('Error parsing auth JSON:', parseError);
                console.log('Raw auth value:', auth);
            }
        } else {
            console.log('No auth found in localStorage');
            // Все равно вызываем Android метод для показа сообщения
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

console.log('Auth handler setup complete');
