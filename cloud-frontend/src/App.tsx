import { BrowserRouter, Routes, Route, Navigate, useParams } from 'react-router-dom';
import Login from './components/Login';
import Register from './components/Register';
import Files from './components/Files';
import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

function FolderView() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  useEffect(() => {
    if (id) {
      navigate(`/files?directoryId=${id}`, { replace: true });
    }
  }, [id, navigate]);

  return <Files />;
}

function FileDownload() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  useEffect(() => {
    if (id) {
      const token = localStorage.getItem('token');
      if (!token) {
        navigate('/login');
        return;
      }

      // Скачиваем файл напрямую
      fetch(`${import.meta.env.VITE_API_URL || 'http://localhost:8091'}/v1/files/download/${id}`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      })
        .then(res => {
          if (res.ok) {
            return res.blob();
          }
          throw new Error('Failed to download');
        })
        .then(blob => {
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = `file-${id}`;
          document.body.appendChild(a);
          a.click();
          window.URL.revokeObjectURL(url);
          document.body.removeChild(a);
          navigate('/files');
        })
        .catch(() => navigate('/files'));
    }
  }, [id, navigate]);

  return (
    <div style={{ 
      display: 'flex', 
      alignItems: 'center', 
      justifyContent: 'center', 
      minHeight: '100vh',
      flexDirection: 'column',
      gap: '20px',
      padding: '20px'
    }}>
      <div className="card" style={{ 
        padding: '40px',
        textAlign: 'center',
        maxWidth: '400px',
        width: '100%'
      }}>
        <div style={{ 
          fontSize: '64px', 
          marginBottom: '20px',
          animation: 'pulse 1.5s ease-in-out infinite'
        }}>
          ⬇️
        </div>
        <h2 style={{ color: '#333', marginBottom: '10px' }}>Загрузка файла</h2>
        <p style={{ color: '#666' }}>Перенаправление на файл...</p>
      </div>
      <style>{`
        @keyframes pulse {
          0%, 100% { 
            transform: scale(1);
            opacity: 1;
          }
          50% { 
            transform: scale(1.1);
            opacity: 0.8;
          }
        }
      `}</style>
    </div>
  );
}

function App() {
  const isAuthenticated = () => {
    return !!localStorage.getItem('token');
  };

  const ProtectedRoute = ({ children }: { children: React.ReactNode }) => {
    return isAuthenticated() ? <>{children}</> : <Navigate to="/login" />;
  };

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route
          path="/files"
          element={
            <ProtectedRoute>
              <Files />
            </ProtectedRoute>
          }
        />
        <Route
          path="/folders/:id"
          element={
            <ProtectedRoute>
              <FolderView />
            </ProtectedRoute>
          }
        />
        <Route
          path="/files/:id/download"
          element={
            <ProtectedRoute>
              <FileDownload />
            </ProtectedRoute>
          }
        />
        <Route path="/" element={<Navigate to="/files" />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
