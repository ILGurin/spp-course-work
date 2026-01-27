export const getUserIdFromToken = (): string | null => {
  const token = localStorage.getItem('token');
  if (!token) return null;
  
  try {
    // JWT токен состоит из трех частей, разделенных точками
    const payload = token.split('.')[1];
    if (!payload) return null;
    
    // Декодируем base64 (может потребоваться padding)
    let base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    while (base64.length % 4) {
      base64 += '=';
    }
    
    const decoded = JSON.parse(atob(base64));
    console.log('Decoded JWT payload:', decoded);
    
    // В Keycloak userId находится в поле 'sub'
    const userId = decoded.sub || decoded.userId || decoded.user_id || null;
    if (userId) {
      console.log('Extracted userId from token:', userId);
    }
    return userId;
  } catch (error) {
    console.error('Error decoding token:', error);
    return null;
  }
};

export const setUserId = (userId: string) => {
  localStorage.setItem('userId', userId);
};

export const getUserId = (): string | null => {
  return localStorage.getItem('userId') || getUserIdFromToken();
};

