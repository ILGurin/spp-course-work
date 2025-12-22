import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { authApi } from '../services/api';

export default function Register() {
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    firstName: '',
    lastName: '',
    phone_number: '',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const response = await authApi.register(formData);
      const { access_token, refresh_token } = response.data;
      localStorage.setItem('token', access_token);
      localStorage.setItem('refreshToken', refresh_token);
      navigate('/files');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Ошибка регистрации');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="card" style={{ maxWidth: '500px', width: '100%', margin: '0 auto' }}>
      <h2 style={{ textAlign: 'center', marginBottom: '30px', color: '#333' }}>Регистрация</h2>
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
            name="email"
            value={formData.email}
            onChange={handleChange}
            required
            style={{ width: '100%', marginTop: '8px' }}
            placeholder="Введите ваш email"
          />
        </div>
        <div style={{ marginBottom: '20px' }}>
          <label>Пароль:</label>
          <input
            type="password"
            name="password"
            value={formData.password}
            onChange={handleChange}
            required
            style={{ width: '100%', marginTop: '8px' }}
            placeholder="Введите пароль"
          />
        </div>
        <div style={{ marginBottom: '20px' }}>
          <label>Имя:</label>
          <input
            type="text"
            name="firstName"
            value={formData.firstName}
            onChange={handleChange}
            required
            style={{ width: '100%', marginTop: '8px' }}
            placeholder="Введите ваше имя"
          />
        </div>
        <div style={{ marginBottom: '20px' }}>
          <label>Фамилия:</label>
          <input
            type="text"
            name="lastName"
            value={formData.lastName}
            onChange={handleChange}
            required
            style={{ width: '100%', marginTop: '8px' }}
            placeholder="Введите вашу фамилию"
          />
        </div>
        <div style={{ marginBottom: '24px' }}>
          <label>Телефон:</label>
          <input
            type="text"
            name="phone_number"
            value={formData.phone_number}
            onChange={handleChange}
            required
            style={{ width: '100%', marginTop: '8px' }}
            placeholder="Введите номер телефона"
          />
        </div>
        <button type="submit" disabled={loading} style={{ width: '100%', marginBottom: '20px' }}>
          {loading ? 'Регистрация...' : 'Зарегистрироваться'}
        </button>
      </form>
      <p style={{ marginTop: '20px', textAlign: 'center', color: '#666' }}>
        Уже есть аккаунт? <Link to="/login" style={{ fontWeight: 600 }}>Войти</Link>
      </p>
    </div>
  );
}

