import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { fileApi, folderApi, authApi, FileResponse, FolderResponse } from '../services/api';

export default function Files() {
  const [files, setFiles] = useState<FileResponse[]>([]);
  const [folders, setFolders] = useState<FolderResponse[]>([]);
  const [currentFolderId, setCurrentFolderId] = useState<string | undefined>();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [newFolderName, setNewFolderName] = useState('');
  const [showCreateFolder, setShowCreateFolder] = useState(false);
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  useEffect(() => {
    const folderIdFromUrl = searchParams.get('folderId');
    if (folderIdFromUrl && folderIdFromUrl !== currentFolderId) {
      setCurrentFolderId(folderIdFromUrl);
    }
  }, [searchParams]);

  useEffect(() => {
    loadData();
  }, [currentFolderId]);

  const loadData = async () => {
    setLoading(true);
    setError('');
    try {
      const [filesResponse, foldersResponse] = await Promise.all([
        fileApi.getFiles(currentFolderId),
        folderApi.getFolders(currentFolderId),
      ]);
      setFiles(filesResponse.data.items);
      setFolders(foldersResponse.data.items);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Ошибка загрузки данных');
    } finally {
      setLoading(false);
    }
  };

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setLoading(true);
    setError('');
    try {
      await fileApi.uploadFile(file, currentFolderId);
      loadData();
      e.target.value = '';
    } catch (err: any) {
      setError(err.response?.data?.message || 'Ошибка загрузки файла');
    } finally {
      setLoading(false);
    }
  };

  const handleCreateFolder = async () => {
    if (!newFolderName.trim()) return;

    setLoading(true);
    setError('');
    try {
      await folderApi.createFolder({ name: newFolderName, parentId: currentFolderId });
      setNewFolderName('');
      setShowCreateFolder(false);
      loadData();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Ошибка создания папки');
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteFile = async (id: string) => {
    if (!confirm('Удалить файл?')) return;

    setLoading(true);
    setError('');
    try {
      await fileApi.deleteFile(id);
      loadData();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Ошибка удаления файла');
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteFolder = async (id: string) => {
    if (!confirm('Удалить папку?')) return;

    setLoading(true);
    setError('');
    try {
      await folderApi.deleteFolder(id);
      loadData();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Ошибка удаления папки');
    } finally {
      setLoading(false);
    }
  };

  const handleGetFileLink = async (id: string) => {
    try {
      const response = await fileApi.getFileDownloadLink(id);
      // Копируем ссылку в буфер обмена
      await navigator.clipboard.writeText(response.data.link);
      alert('Ссылка скопирована в буфер обмена');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Ошибка получения ссылки');
    }
  };

  const handleGetFolderLink = async (id: string) => {
    try {
      const response = await folderApi.getFolderLink(id);
      // Копируем ссылку в буфер обмена
      await navigator.clipboard.writeText(response.data.link);
      alert('Ссылка скопирована в буфер обмена');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Ошибка получения ссылки');
    }
  };

  const handleLogout = () => {
    authApi.logout();
    navigate('/login');
  };

  const formatFileSize = (bytes: number) => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
  };

  return (
    <div className="container">
      <div className="card" style={{ padding: '30px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '30px' }}>
          <h1 style={{ color: '#333', margin: 0 }}>Мои файлы</h1>
          <button 
            onClick={handleLogout}
            style={{ 
              background: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
              padding: '10px 20px'
            }}
          >
            Выйти
          </button>
        </div>

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

        <div style={{ marginBottom: '20px', display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
          <label style={{
            display: 'inline-flex',
            alignItems: 'center',
            padding: '12px 20px',
            background: 'white',
            border: '2px solid #667eea',
            borderRadius: '8px',
            cursor: 'pointer',
            transition: 'all 0.3s ease',
            fontWeight: 500,
            color: '#667eea'
          }}>
            📤 Загрузить файл
            <input
              type="file"
              onChange={handleFileUpload}
              disabled={loading}
              style={{ display: 'none' }}
            />
          </label>
          <button 
            onClick={() => setShowCreateFolder(!showCreateFolder)} 
            disabled={loading}
            style={{ padding: '12px 20px' }}
          >
            📁 Создать папку
          </button>
        {currentFolderId && (
          <button 
            onClick={() => {
              setCurrentFolderId(undefined);
              navigate('/files');
            }} 
            disabled={loading}
            style={{ 
              background: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
              padding: '12px 20px'
            }}
          >
            ← Назад
          </button>
        )}
        </div>

        {showCreateFolder && (
          <div style={{ 
            marginBottom: '20px', 
            display: 'flex', 
            gap: '10px', 
            flexWrap: 'wrap',
            padding: '15px',
            backgroundColor: '#f5f5f5',
            borderRadius: '8px'
          }}>
            <input
              type="text"
              placeholder="Название папки"
              value={newFolderName}
              onChange={(e) => setNewFolderName(e.target.value)}
              style={{ flex: 1, minWidth: '200px' }}
            />
            <button 
              onClick={handleCreateFolder} 
              disabled={loading || !newFolderName.trim()}
              style={{ padding: '12px 20px' }}
            >
              Создать
            </button>
            <button 
              onClick={() => { setShowCreateFolder(false); setNewFolderName(''); }}
              style={{ 
                background: 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
                padding: '12px 20px'
              }}
            >
              Отмена
            </button>
          </div>
        )}

        {loading && (
          <div style={{ 
            textAlign: 'center', 
            padding: '40px', 
            color: '#666',
            fontSize: '18px'
          }}>
            Загрузка...
          </div>
        )}

        <div style={{ 
          display: 'grid', 
          gridTemplateColumns: 'repeat(auto-fill, minmax(250px, 1fr))', 
          gap: '20px',
          marginTop: '20px'
        }}>
          {folders.map((folder) => (
            <div
              key={folder.id}
              className="card"
              style={{
                padding: '20px',
                cursor: 'pointer',
                transition: 'all 0.3s ease',
                border: '2px solid #e0e0e0'
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.borderColor = '#667eea';
                e.currentTarget.style.transform = 'translateY(-4px)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.borderColor = '#e0e0e0';
                e.currentTarget.style.transform = 'translateY(0)';
              }}
            >
              <div style={{ 
                fontWeight: 'bold', 
                marginBottom: '12px', 
                fontSize: '18px',
                color: '#333'
              }}>
                📁 {folder.name}
              </div>
              <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
              <button
                onClick={() => {
                  setCurrentFolderId(folder.id);
                  navigate(`/files?folderId=${folder.id}`);
                }}
                style={{ 
                  fontSize: '13px', 
                  padding: '8px 12px',
                  flex: 1,
                  minWidth: '80px'
                }}
              >
                Открыть
              </button>
                <button
                  onClick={() => handleGetFolderLink(folder.id)}
                  style={{ 
                    fontSize: '13px', 
                    padding: '8px 12px',
                    background: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
                    flex: 1,
                    minWidth: '80px'
                  }}
                >
                  Ссылка
                </button>
                <button
                  onClick={() => handleDeleteFolder(folder.id)}
                  style={{ 
                    fontSize: '13px', 
                    padding: '8px 12px',
                    background: 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
                    flex: 1,
                    minWidth: '80px'
                  }}
                >
                  Удалить
                </button>
              </div>
            </div>
          ))}

          {files.map((file) => (
            <div
              key={file.id}
              className="card"
              style={{
                padding: '20px',
                transition: 'all 0.3s ease',
                border: '2px solid #e0e0e0'
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.borderColor = '#667eea';
                e.currentTarget.style.transform = 'translateY(-4px)';
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.borderColor = '#e0e0e0';
                e.currentTarget.style.transform = 'translateY(0)';
              }}
            >
              <div style={{ 
                fontWeight: 'bold', 
                marginBottom: '8px', 
                fontSize: '16px',
                color: '#333',
                wordBreak: 'break-word'
              }}>
                📄 {file.name}
              </div>
              <div style={{ 
                fontSize: '13px', 
                color: '#666', 
                marginBottom: '15px',
                padding: '6px',
                backgroundColor: '#f5f5f5',
                borderRadius: '4px'
              }}>
                {formatFileSize(file.size)}
              </div>
              <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                <button
                  onClick={() => handleGetFileLink(file.id)}
                  style={{ 
                    fontSize: '13px', 
                    padding: '8px 12px',
                    flex: 1,
                    minWidth: '100px'
                  }}
                >
                  Скачать
                </button>
                <button
                  onClick={() => handleDeleteFile(file.id)}
                  style={{ 
                    fontSize: '13px', 
                    padding: '8px 12px',
                    background: 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
                    flex: 1,
                    minWidth: '100px'
                  }}
                >
                  Удалить
                </button>
              </div>
            </div>
          ))}
        </div>

        {!loading && folders.length === 0 && files.length === 0 && (
          <div style={{ 
            textAlign: 'center', 
            padding: '60px 20px', 
            color: '#999',
            fontSize: '18px'
          }}>
            📂 Папка пуста
          </div>
        )}
      </div>
    </div>
  );
}

