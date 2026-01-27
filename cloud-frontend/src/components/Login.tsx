import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { authApi } from '../services/api';
import { setUserId } from '../services/auth';

export default function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const response = await authApi.login({ email, password });
      // Обрабатываем ответ - может быть напрямую TokenResponse или обернутый в data
      const tokenData = response.data || response;
      const access_token = tokenData.access_token || tokenData.accessToken;
      const refresh_token = tokenData.refresh_token || tokenData.refreshToken;
      localStorage.setItem('token', access_token);
      localStorage.setItem('refreshToken', refresh_token);
      
      // Получаем информацию о пользователе
      try {
        const userResponse = await fetch(`${import.meta.env.VITE_API_URL || 'http://localhost:8091'}/v1/auth/me`, {
          headers: {
            'Authorization': `Bearer ${access_token}`,
          },
        });
        if (userResponse.ok) {
          const data = await userResponse.json();
          console.log('User info from /me:', data);
          // Обрабатываем ответ - может быть напрямую UserInfoResponse или обернутый
          const userData = data.data || data;
          if (userData.id) {
            setUserId(userData.id);
            console.log('UserId set to:', userData.id);
          }
        }
      } catch (err) {
        console.error('Failed to get user info:', err);
      }
      
      navigate('/files');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Ошибка входа. Проверьте email и пароль.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ 
      display: 'flex', 
      alignItems: 'center', 
      justifyContent: 'center', 
      minHeight: '100vh',
      padding: '20px'
    }}>
      <div className="card" style={{ 
        maxWidth: '480px', 
        width: '100%', 
        margin: '0 auto',
        padding: '40px',
        boxShadow: '0 20px 60px rgba(0, 0, 0, 0.15)'
      }}>
        <div style={{ textAlign: 'center', marginBottom: '30px' }}>
          <div style={{ fontSize: '64px', marginBottom: '20px' }}>☁️</div>
          <h2 style={{ 
            marginBottom: '10px', 
            color: '#333',
            fontSize: '2em',
            fontWeight: 700
          }}>
            Добро пожаловать
          </h2>
          <p style={{ color: '#666', fontSize: '1em' }}>
            Войдите в ваше облачное хранилище
          </p>
        </div>
        
        {error && (
          <div style={{
            color: '#d32f2f',
            marginBottom: '24px',
            padding: '14px 18px',
            backgroundColor: '#ffebee',
            borderRadius: '12px',
            border: '2px solid #ffcdd2',
            textAlign: 'center',
            fontSize: '0.95em',
            fontWeight: 500
          }}>
            ⚠️ {error}
          </div>
        )}
        
        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: '24px' }}>
            <label style={{ 
              display: 'block',
              fontWeight: 600,
              marginBottom: '10px',
              color: '#333',
              fontSize: '1em'
            }}>
              Email:
            </label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              style={{ 
                width: '100%', 
                marginTop: '8px',
                padding: '14px 18px',
                fontSize: '1em',
                border: '2px solid #e0e0e0',
                borderRadius: '10px',
                transition: 'all 0.3s ease'
              }}
              placeholder="your.email@example.com"
            />
          </div>
          
          <div style={{ marginBottom: '30px' }}>
            <label style={{ 
              display: 'block',
              fontWeight: 600,
              marginBottom: '10px',
              color: '#333',
              fontSize: '1em'
            }}>
              Пароль:
            </label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              style={{ 
                width: '100%', 
                marginTop: '8px',
                padding: '14px 18px',
                fontSize: '1em',
                border: '2px solid #e0e0e0',
                borderRadius: '10px',
                transition: 'all 0.3s ease'
              }}
              placeholder="Введите пароль"
            />
          </div>
          
          <button 
            type="submit" 
            disabled={loading} 
            className="btn-primary"
            style={{ 
              width: '100%', 
              marginBottom: '24px',
              padding: '16px',
              fontSize: '1.1em',
              fontWeight: 600,
              boxShadow: '0 4px 15px rgba(102, 126, 234, 0.4)'
            }}
          >
            {loading ? '⏳ Вход...' : '🚀 Войти'}
          </button>
        </form>
        
        <p style={{ 
          marginTop: '24px', 
          textAlign: 'center', 
          color: '#666',
          fontSize: '0.95em'
        }}>
          Нет аккаунта?{' '}
          <Link 
            to="/register" 
            style={{ 
              fontWeight: 700, 
              color: '#667eea',
              textDecoration: 'none',
              transition: 'color 0.3s ease'
            }}
            onMouseEnter={(e) => e.currentTarget.style.color = '#764ba2'}
            onMouseLeave={(e) => e.currentTarget.style.color = '#667eea'}
          >
            Зарегистрироваться
          </Link>
        </p>
      </div>
    </div>
  );
}
