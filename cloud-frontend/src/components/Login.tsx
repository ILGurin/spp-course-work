import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { authApi } from '../services/api';

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
      const { access_token, refresh_token } = response.data;
      localStorage.setItem('token', access_token);
      localStorage.setItem('refreshToken', refresh_token);
      navigate('/files');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Ошибка входа');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="card" style={{ maxWidth: '450px', width: '100%', margin: '0 auto' }}>
      <h2 style={{ textAlign: 'center', marginBottom: '30px', color: '#333' }}>Вход</h2>
      {error && (
        <div style={{
          color: '#d32f2f',
          marginBottom: '20px',
          padding: '12px',
          backgroundColor: '#ffebee',
          borderRadius: '8px',
          border: '1px solid #ffcdd2',
          textAlign: 'center'
        }}>
          {error}
        </div>
      )}
      <form onSubmit={handleSubmit}>
        <div style={{ marginBottom: '20px' }}>
          <label>Email:</label>
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
            style={{ width: '100%', marginTop: '8px' }}
            placeholder="Введите ваш email"
          />
        </div>
        <div style={{ marginBottom: '24px' }}>
          <label>Пароль:</label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            style={{ width: '100%', marginTop: '8px' }}
            placeholder="Введите пароль"
          />
        </div>
        <button type="submit" disabled={loading} style={{ width: '100%', marginBottom: '20px' }}>
          {loading ? 'Вход...' : 'Войти'}
        </button>
      </form>
      <p style={{ marginTop: '20px', textAlign: 'center', color: '#666' }}>
        Нет аккаунта? <Link to="/register" style={{ fontWeight: 600 }}>Зарегистрироваться</Link>
      </p>
    </div>
  );
}

