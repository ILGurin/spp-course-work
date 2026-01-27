import { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { fileApi, folderApi, authApi, type FileResponse, type FolderResponse } from '../services/api';
import { getUserId, setUserId } from '../services/auth';

export default function Files() {
  const [files, setFiles] = useState<FileResponse[]>([]);
  const [folders, setFolders] = useState<FolderResponse[]>([]);
  const [currentFolderId, setCurrentFolderId] = useState<string | undefined>();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [newFolderName, setNewFolderName] = useState('');
  const [showCreateFolder, setShowCreateFolder] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [selectedItems, setSelectedItems] = useState<Set<string>>(new Set());
  const [viewMode, setViewMode] = useState<'details' | 'tiles'>('details');
  const [sortBy, setSortBy] = useState<'name' | 'size' | 'date'>('name');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('asc');
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [isLoadingData, setIsLoadingData] = useState(false);
  const abortControllerRef = useRef<AbortController | null>(null);
  const lastLoadParamsRef = useRef<string>('');

  useEffect(() => {
    // Получаем userId из токена при первой загрузке
    const loadUserId = async () => {
      let userId = getUserId();
      if (!userId) {
        // Пытаемся получить из API
        try {
          const response = await fetch(`${import.meta.env.VITE_API_URL || 'http://localhost:8091'}/v1/auth/me`, {
            headers: {
              'Authorization': `Bearer ${localStorage.getItem('token')}`,
            },
          });
          if (response.ok) {
            const data = await response.json();
            // Обрабатываем ответ - может быть напрямую UserInfoResponse или обернутый
            const userData = data.data || data;
            if (userData.id) {
              setUserId(userData.id);
            }
          }
        } catch (err) {
          console.error('Failed to get user info:', err);
        }
      } else {
      }
    };
    loadUserId();
  }, []);
  
  useEffect(() => {
    const folderIdFromUrl = searchParams.get('directoryId') || searchParams.get('folderId');
    if (folderIdFromUrl && folderIdFromUrl !== currentFolderId) {
      setCurrentFolderId(folderIdFromUrl);
    }
  }, [searchParams, currentFolderId]);

  const loadData = async () => {
    // Создаем уникальный ключ для текущего запроса
    const loadKey = `${currentFolderId || 'root'}`;
    
    // Если уже загружаются те же данные, пропускаем
    if (isLoadingData && lastLoadParamsRef.current === loadKey) {
      return;
    }
    
    // Отменяем предыдущий запрос, если он еще выполняется
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }
    
    // Создаем новый AbortController
    const abortController = new AbortController();
    abortControllerRef.current = abortController;
    lastLoadParamsRef.current = loadKey;
    
    setIsLoadingData(true);
    setLoading(true);
    setError('');
    try {
      const userId = getUserId();
      if (!userId) {
        setError('Не удалось определить пользователя');
        setIsLoadingData(false);
        setLoading(false);
        return;
      }

      const [filesResponse, foldersResponse] = await Promise.all([
        fileApi.getFiles(currentFolderId),
        folderApi.getFolders(currentFolderId),
      ]);
      
      // Проверяем, не был ли запрос отменен
      if (abortController.signal.aborted) {
        setIsLoadingData(false);
        setLoading(false);
        return;
      }
      
      // Убираем дубликаты по ID
      // handleResponse оборачивает ответ в {data: {...}}, так что данные в filesResponse.data
      const filesItems = filesResponse?.data?.items || [];
      const foldersItems = foldersResponse?.data?.items || [];
      
      const filesMap = new Map();
      if (Array.isArray(filesItems)) {
        filesItems.forEach((file: any) => {
          if (file && file.id) {
            filesMap.set(file.id, file);
          }
        });
      }
      const uniqueFiles = Array.from(filesMap.values());
      
      const foldersMap = new Map();
      if (Array.isArray(foldersItems)) {
        foldersItems.forEach((folder: any) => {
          if (folder && folder.id) {
            foldersMap.set(folder.id, folder);
          }
        });
      }
      const uniqueFolders = Array.from(foldersMap.values());
      
      // Устанавливаем данные
      setFiles(uniqueFiles);
      setFolders(uniqueFolders);
      setLoading(false);
      setIsLoadingData(false);
    } catch (err: any) {
      console.error('Error in loadData:', err);
      // Игнорируем ошибки отмены запроса
      if (err.name === 'AbortError' || abortController.signal.aborted) {
        setIsLoadingData(false);
        setLoading(false);
        return;
      }
      // Убрана проверка isMountedRef из-за проблем со StrictMode
      if (err.response?.status === 401) {
        console.error('Unauthorized error, logging out...');
        setError('Сессия истекла. Пожалуйста, войдите снова.');
        await authApi.logout();
        navigate('/login');
      } else {
        const errorMessage = err.response?.data?.message || err.message || 'Ошибка загрузки данных';
        console.error('Error message:', errorMessage);
        setError(errorMessage);
      }
    } finally {
      // Всегда сбрасываем состояние загрузки для текущего запроса
      if (abortControllerRef.current === abortController) {
        abortControllerRef.current = null;
      }
      // Всегда сбрасываем состояние
      setLoading(false);
      setIsLoadingData(false);
    }
  };

  useEffect(() => {
    // Вызываем loadData при изменении currentFolderId
    loadData();
  }, [currentFolderId]);

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const filesList = e.target.files;
    if (!filesList || filesList.length === 0) return;

    setUploading(true);
    setError('');
    try {
      const filesArray = Array.from(filesList);
      await fileApi.uploadFiles(filesArray, currentFolderId);
      
      // Добавляем небольшую задержку перед обновлением данных,
      // чтобы дать время транзакции зафиксироваться и кэшу обновиться
      await new Promise(resolve => setTimeout(resolve, 300));
      
      // Очищаем input перед обновлением данных
      e.target.value = '';
      
      // Загружаем данные только один раз
      await loadData();
    } catch (err: any) {
      if (err.response?.status === 401) {
        await authApi.logout();
        navigate('/login');
      } else {
        setError(err.response?.data?.message || 'Ошибка загрузки файла');
      }
    } finally {
      setUploading(false);
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
      if (err.response?.status === 401) {
        await authApi.logout();
        navigate('/login');
      } else {
        setError(err.response?.data?.message || 'Ошибка создания папки');
      }
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
      if (err.response?.status === 401) {
        await authApi.logout();
        navigate('/login');
      } else {
        setError(err.response?.data?.message || 'Ошибка удаления файла');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteFolder = async (id: string) => {
    if (!confirm('Удалить папку? Все файлы внутри также будут удалены.')) return;

    setLoading(true);
    setError('');
    try {
      await folderApi.deleteFolder(id);
      loadData();
    } catch (err: any) {
      if (err.response?.status === 401) {
        await authApi.logout();
        navigate('/login');
      } else {
        setError(err.response?.data?.message || 'Ошибка удаления папки');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleDownloadFile = async (id: string, fileName: string) => {
    try {
      const blob = await fileApi.downloadFile(id);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = fileName;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Ошибка скачивания файла');
    }
  };

  const handleLogout = async () => {
    try {
      await authApi.logout();
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      navigate('/login');
    }
  };

  const formatFileSize = (bytes: number) => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
  };

  const formatDate = (dateString: string) => {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleDateString('ru-RU', { 
      year: 'numeric', 
      month: '2-digit', 
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const getFileIcon = (mimeType: string) => {
    if (mimeType?.startsWith('image/')) return '🖼️';
    if (mimeType?.startsWith('video/')) return '🎥';
    if (mimeType?.startsWith('audio/')) return '🎵';
    if (mimeType?.includes('pdf')) return '📕';
    if (mimeType?.includes('word') || mimeType?.includes('document')) return '📝';
    if (mimeType?.includes('excel') || mimeType?.includes('spreadsheet')) return '📊';
    if (mimeType?.includes('zip') || mimeType?.includes('archive')) return '📦';
    return '📄';
  };

  const getFileType = (mimeType: string) => {
    if (!mimeType) return 'Файл';
    const parts = mimeType.split('/');
    if (parts.length > 1) {
      return parts[1].toUpperCase() + ' файл';
    }
    return 'Файл';
  };

  const handleSort = (column: 'name' | 'size' | 'date') => {
    if (sortBy === column) {
      setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
    } else {
      setSortBy(column);
      setSortOrder('asc');
    }
  };

  const sortedFolders = [...folders].sort((a, b) => {
    const comparison = a.name.localeCompare(b.name, 'ru');
    return sortOrder === 'asc' ? comparison : -comparison;
  });

  const sortedFiles = [...files].sort((a, b) => {
    let comparison = 0;
    switch (sortBy) {
      case 'name':
        comparison = a.fileName.localeCompare(b.fileName, 'ru');
        break;
      case 'size':
        comparison = a.fileSize - b.fileSize;
        break;
      case 'date':
        // Сортировка по дате не доступна, так как createdAt не входит в DTO
        comparison = 0;
        break;
    }
    return sortOrder === 'asc' ? comparison : -comparison;
  });

  const handleItemSelect = (id: string, event: React.MouseEvent) => {
    if (event.ctrlKey || event.metaKey) {
      setSelectedItems(prev => {
        const newSet = new Set(prev);
        if (newSet.has(id)) {
          newSet.delete(id);
        } else {
          newSet.add(id);
        }
        return newSet;
      });
    } else {
      setSelectedItems(new Set([id]));
    }
  };

  const handleItemDoubleClick = (item: FolderResponse | FileResponse) => {
    if ('name' in item) {
      // Это папка
      setCurrentFolderId(item.id);
      navigate(`/files?directoryId=${item.id}`);
    }
  };

  const totalSize = files.reduce((sum, file) => sum + file.fileSize, 0);
  const totalItems = folders.length + files.length;

  return (
    <div style={{ 
      display: 'flex', 
      flexDirection: 'column', 
      height: '100vh', 
      backgroundColor: '#f5f5f5',
      fontFamily: 'Segoe UI, system-ui, sans-serif'
    }}>
      {/* Панель заголовка */}
      <div style={{
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        padding: '12px 16px',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        color: 'white',
        boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
      }}>
        <h1 style={{ margin: 0, fontSize: '1.2em', fontWeight: 500 }}>☁️ Облачное хранилище</h1>
        <button 
          onClick={handleLogout}
          style={{ 
            background: 'rgba(255,255,255,0.2)',
            border: '1px solid rgba(255,255,255,0.3)',
            padding: '6px 16px',
            fontSize: '0.9em',
            color: 'white',
            borderRadius: '4px',
            cursor: 'pointer',
            transition: 'background 0.2s'
          }}
          onMouseEnter={(e) => e.currentTarget.style.background = 'rgba(255,255,255,0.3)'}
          onMouseLeave={(e) => e.currentTarget.style.background = 'rgba(255,255,255,0.2)'}
        >
          Выйти
        </button>
      </div>

      {/* Панель инструментов */}
      <div style={{
        background: 'white',
        borderBottom: '1px solid #e0e0e0',
        padding: '8px 12px',
        display: 'flex',
        gap: '8px',
        alignItems: 'center',
        flexWrap: 'wrap'
      }}>
        <button
          onClick={() => {
            if (currentFolderId) {
              setCurrentFolderId(undefined);
              navigate('/files');
            }
          }}
          disabled={!currentFolderId || loading}
          style={{
            padding: '6px 12px',
            border: '1px solid #d0d0d0',
            background: 'white',
            borderRadius: '4px',
            cursor: (!currentFolderId || loading) ? 'not-allowed' : 'pointer',
            fontSize: '0.9em',
            opacity: (!currentFolderId || loading) ? 0.5 : 1,
            lineHeight: '1',
            display: 'inline-flex',
            alignItems: 'center'
          }}
        >
          ← Назад
        </button>
        <button
          onClick={() => loadData()}
          disabled={loading}
          style={{
            padding: '6px 12px',
            border: '1px solid #d0d0d0',
            background: 'white',
            borderRadius: '4px',
            cursor: loading ? 'not-allowed' : 'pointer',
            fontSize: '0.9em',
            opacity: loading ? 0.5 : 1,
            lineHeight: '1',
            display: 'inline-flex',
            alignItems: 'center'
          }}
        >
          ↻ Обновить
        </button>
        <div style={{ flex: 1, minWidth: '200px' }}>
          <input
            type="text"
            value={currentFolderId ? `Папка: ${folders.find(f => f.id === currentFolderId)?.name || 'Текущая'}` : 'Главная'}
            readOnly
            style={{
              width: '100%',
              padding: '6px 12px',
              border: '1px solid #d0d0d0',
              borderRadius: '4px',
              fontSize: '0.9em',
              background: '#f9f9f9'
            }}
          />
        </div>
        <label
          style={{
            padding: '6px 12px',
            background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
            color: 'white',
            borderRadius: '4px',
            cursor: uploading ? 'not-allowed' : 'pointer',
            fontSize: '0.9em',
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            lineHeight: '1.2',
            opacity: uploading ? 0.7 : 1,
            border: 'none',
            margin: 0,
            verticalAlign: 'middle',
            boxSizing: 'border-box'
          }}
        >
          {uploading ? '⏳ Загрузка...' : '📤 Загрузить'}
          <input
            type="file"
            multiple
            onChange={handleFileUpload}
            disabled={uploading || loading}
            style={{ display: 'none' }}
          />
        </label>
        <button
          onClick={() => setShowCreateFolder(!showCreateFolder)}
          disabled={loading}
          style={{
            padding: '6px 12px',
            background: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: loading ? 'not-allowed' : 'pointer',
            fontSize: '0.9em',
            opacity: loading ? 0.7 : 1,
            lineHeight: '1.2',
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            margin: 0,
            verticalAlign: 'middle',
            boxSizing: 'border-box'
          }}
        >
          📁 Новая папка
        </button>
        <div style={{ display: 'flex', gap: '4px', borderLeft: '1px solid #e0e0e0', paddingLeft: '8px' }}>
          <button
            onClick={() => setViewMode('details')}
            style={{
              padding: '6px 10px',
              border: viewMode === 'details' ? '2px solid #667eea' : '1px solid #d0d0d0',
              background: viewMode === 'details' ? 'rgba(102, 126, 234, 0.1)' : 'white',
              borderRadius: '4px',
              cursor: 'pointer',
              fontSize: '0.9em'
            }}
            title="Таблица"
          >
            ☰
          </button>
          <button
            onClick={() => setViewMode('tiles')}
            style={{
              padding: '6px 10px',
              border: viewMode === 'tiles' ? '2px solid #667eea' : '1px solid #d0d0d0',
              background: viewMode === 'tiles' ? 'rgba(102, 126, 234, 0.1)' : 'white',
              borderRadius: '4px',
              cursor: 'pointer',
              fontSize: '0.9em'
            }}
            title="Плитки"
          >
            ⊞
          </button>
        </div>
      </div>

      {/* Форма создания папки */}
      {showCreateFolder && (
        <div style={{ 
          background: 'white',
          borderBottom: '1px solid #e0e0e0',
          padding: '12px',
          display: 'flex', 
          gap: '8px', 
          alignItems: 'center'
        }}>
          <input
            type="text"
            placeholder="Введите название папки"
            value={newFolderName}
            onChange={(e) => setNewFolderName(e.target.value)}
            onKeyPress={(e) => e.key === 'Enter' && handleCreateFolder()}
            style={{ 
              flex: 1,
              padding: '6px 12px',
              border: '1px solid #d0d0d0',
              borderRadius: '4px',
              fontSize: '0.9em'
            }}
          />
          <button 
            onClick={handleCreateFolder} 
            disabled={loading || !newFolderName.trim()}
            style={{
              padding: '6px 16px',
              background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
              color: 'white',
              border: 'none',
              borderRadius: '4px',
              cursor: (loading || !newFolderName.trim()) ? 'not-allowed' : 'pointer',
              fontSize: '0.9em',
              opacity: (loading || !newFolderName.trim()) ? 0.7 : 1
            }}
          >
            Создать
          </button>
          <button 
            onClick={() => { setShowCreateFolder(false); setNewFolderName(''); }}
            style={{
              padding: '6px 16px',
              background: '#f5f5f5',
              border: '1px solid #d0d0d0',
              borderRadius: '4px',
              cursor: 'pointer',
              fontSize: '0.9em'
            }}
          >
            Отмена
          </button>
        </div>
      )}

      {/* Сообщение об ошибке */}
      {error && (
        <div style={{
          background: '#ffebee',
          color: '#d32f2f',
          padding: '12px 16px',
          borderBottom: '1px solid #ffcdd2',
          fontSize: '0.9em'
        }}>
          ⚠️ {error}
        </div>
      )}

      {/* Основная область */}
      <div style={{ flex: 1, overflow: 'auto', background: 'white' }}>
        {loading && !uploading && (
          <div style={{ 
            textAlign: 'center', 
            padding: '60px', 
            color: '#666',
            fontSize: '1.1em'
          }}>
            <div style={{ fontSize: '48px', marginBottom: '20px' }}>⏳</div>
            Загрузка данных...
          </div>
        )}

        {!loading && viewMode === 'details' && (
          <div style={{ width: '100%' }}>
            {/* Заголовки таблицы */}
            <div style={{
              display: 'grid',
              gridTemplateColumns: '30px 2fr 1fr 1fr 120px',
              background: '#f5f5f5',
              borderBottom: '2px solid #e0e0e0',
              padding: '8px 4px',
              fontSize: '0.85em',
              fontWeight: 600,
              color: '#666',
              position: 'sticky',
              top: 0,
              zIndex: 10
            }}>
              <div></div>
              <div
                onClick={() => handleSort('name')}
                style={{
                  cursor: 'pointer',
                  padding: '4px 8px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '4px',
                  userSelect: 'none'
                }}
              >
                Имя {sortBy === 'name' && (sortOrder === 'asc' ? '↑' : '↓')}
              </div>
              <div
                onClick={() => handleSort('date')}
                style={{
                  cursor: 'pointer',
                  padding: '4px 8px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '4px',
                  userSelect: 'none'
                }}
              >
                Дата изменения {sortBy === 'date' && (sortOrder === 'asc' ? '↑' : '↓')}
              </div>
              <div
                onClick={() => handleSort('size')}
                style={{
                  cursor: 'pointer',
                  padding: '4px 8px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '4px',
                  userSelect: 'none'
                }}
              >
                Тип {sortBy === 'size' && (sortOrder === 'asc' ? '↑' : '↓')}
              </div>
              <div style={{ padding: '4px 8px' }}>Размер</div>
            </div>

            {/* Папки */}
            {sortedFolders.map((folder) => (
              <div
                key={folder.id}
                onClick={(e) => handleItemSelect(folder.id, e)}
                onDoubleClick={() => handleItemDoubleClick(folder)}
                style={{
                  display: 'grid',
                  gridTemplateColumns: '30px 2fr 1fr 1fr 120px',
                  padding: '8px 4px',
                  borderBottom: '1px solid #f0f0f0',
                  cursor: 'pointer',
                  backgroundColor: selectedItems.has(folder.id) ? 'rgba(102, 126, 234, 0.1)' : 'transparent',
                  transition: 'background 0.15s'
                }}
                onMouseEnter={(e) => {
                  if (!selectedItems.has(folder.id)) {
                    e.currentTarget.style.backgroundColor = '#f9f9f9';
                  }
                }}
                onMouseLeave={(e) => {
                  if (!selectedItems.has(folder.id)) {
                    e.currentTarget.style.backgroundColor = 'transparent';
                  }
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <input
                    type="checkbox"
                    checked={selectedItems.has(folder.id)}
                    onChange={() => {}}
                    onClick={(e) => e.stopPropagation()}
                    style={{ cursor: 'pointer' }}
                  />
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '4px 8px' }}>
                  <span style={{ fontSize: '1.2em' }}>📁</span>
                  <span>{folder.name}</span>
                </div>
                <div style={{ padding: '4px 8px', color: '#666', fontSize: '0.9em' }}>
                  —
                </div>
                <div style={{ padding: '4px 8px', color: '#666', fontSize: '0.9em' }}>
                  Папка с файлами
                </div>
                <div style={{ padding: '4px 8px', color: '#666', fontSize: '0.9em' }}>
                  —
                </div>
              </div>
            ))}

            {/* Файлы */}
            {sortedFiles.map((file) => (
              <div
                key={file.id}
                onClick={(e) => handleItemSelect(file.id, e)}
                style={{
                  display: 'grid',
                  gridTemplateColumns: '30px 2fr 1fr 1fr 120px',
                  padding: '8px 4px',
                  borderBottom: '1px solid #f0f0f0',
                  cursor: 'pointer',
                  backgroundColor: selectedItems.has(file.id) ? 'rgba(102, 126, 234, 0.1)' : 'transparent',
                  transition: 'background 0.15s'
                }}
                onMouseEnter={(e) => {
                  if (!selectedItems.has(file.id)) {
                    e.currentTarget.style.backgroundColor = '#f9f9f9';
                  }
                }}
                onMouseLeave={(e) => {
                  if (!selectedItems.has(file.id)) {
                    e.currentTarget.style.backgroundColor = 'transparent';
                  }
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                  <input
                    type="checkbox"
                    checked={selectedItems.has(file.id)}
                    onChange={() => {}}
                    onClick={(e) => e.stopPropagation()}
                    style={{ cursor: 'pointer' }}
                  />
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '4px 8px' }}>
                  <span style={{ fontSize: '1.2em' }}>{getFileIcon(file.mimeType)}</span>
                  <span>{file.fileName}</span>
                </div>
                <div style={{ padding: '4px 8px', color: '#666', fontSize: '0.9em' }}>
                  —
                </div>
                <div style={{ padding: '4px 8px', color: '#666', fontSize: '0.9em' }}>
                  {getFileType(file.mimeType)}
                </div>
                <div style={{ padding: '4px 8px', color: '#666', fontSize: '0.9em' }}>
                  {formatFileSize(file.fileSize)}
                </div>
              </div>
            ))}

            {!loading && folders.length === 0 && files.length === 0 && (
              <div style={{ 
                textAlign: 'center', 
                padding: '80px 20px', 
                color: '#999',
                fontSize: '1.2em'
              }}>
                <div style={{ fontSize: '64px', marginBottom: '20px' }}>📂</div>
                <div>Папка пуста</div>
                <div style={{ fontSize: '0.9em', marginTop: '10px', color: '#bbb' }}>
                  Загрузите файлы или создайте папку
                </div>
              </div>
            )}
          </div>
        )}

        {!loading && viewMode === 'tiles' && (
          <div style={{ 
            display: 'grid', 
            gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', 
            gap: '16px',
            padding: '16px'
          }}>
            {sortedFolders.map((folder) => (
              <div
                key={folder.id}
                onClick={(e) => handleItemSelect(folder.id, e)}
                onDoubleClick={() => handleItemDoubleClick(folder)}
                style={{
                  padding: '16px',
                  border: selectedItems.has(folder.id) ? '2px solid #667eea' : '1px solid #e0e0e0',
                  borderRadius: '8px',
                  background: selectedItems.has(folder.id) 
                    ? 'rgba(102, 126, 234, 0.1)' 
                    : 'linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%)',
                  cursor: 'pointer',
                  transition: 'all 0.2s',
                  textAlign: 'center'
                }}
                onMouseEnter={(e) => {
                  if (!selectedItems.has(folder.id)) {
                    e.currentTarget.style.borderColor = '#667eea';
                    e.currentTarget.style.transform = 'translateY(-2px)';
                  }
                }}
                onMouseLeave={(e) => {
                  if (!selectedItems.has(folder.id)) {
                    e.currentTarget.style.borderColor = '#e0e0e0';
                    e.currentTarget.style.transform = 'translateY(0)';
                  }
                }}
              >
                <div style={{ fontSize: '3em', marginBottom: '8px' }}>📁</div>
                <div style={{ fontWeight: 500, wordBreak: 'break-word' }}>{folder.name}</div>
              </div>
            ))}

            {sortedFiles.map((file) => (
              <div
                key={file.id}
                onClick={(e) => handleItemSelect(file.id, e)}
                style={{
                  padding: '16px',
                  border: selectedItems.has(file.id) ? '2px solid #667eea' : '1px solid #e0e0e0',
                  borderRadius: '8px',
                  background: selectedItems.has(file.id) 
                    ? 'rgba(102, 126, 234, 0.1)' 
                    : 'linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%)',
                  cursor: 'pointer',
                  transition: 'all 0.2s',
                  textAlign: 'center'
                }}
                onMouseEnter={(e) => {
                  if (!selectedItems.has(file.id)) {
                    e.currentTarget.style.borderColor = '#667eea';
                    e.currentTarget.style.transform = 'translateY(-2px)';
                  }
                }}
                onMouseLeave={(e) => {
                  if (!selectedItems.has(file.id)) {
                    e.currentTarget.style.borderColor = '#e0e0e0';
                    e.currentTarget.style.transform = 'translateY(0)';
                  }
                }}
              >
                <div style={{ fontSize: '3em', marginBottom: '8px' }}>{getFileIcon(file.mimeType)}</div>
                <div style={{ fontWeight: 500, wordBreak: 'break-word', marginBottom: '4px' }}>{file.fileName}</div>
                <div style={{ fontSize: '0.85em', color: '#666' }}>{formatFileSize(file.fileSize)}</div>
              </div>
            ))}

            {!loading && folders.length === 0 && files.length === 0 && (
              <div style={{ 
                gridColumn: '1 / -1',
                textAlign: 'center', 
                padding: '80px 20px', 
                color: '#999',
                fontSize: '1.2em'
              }}>
                <div style={{ fontSize: '64px', marginBottom: '20px' }}>📂</div>
                <div>Папка пуста</div>
                <div style={{ fontSize: '0.9em', marginTop: '10px', color: '#bbb' }}>
                  Загрузите файлы или создайте папку
                </div>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Контекстное меню для выбранных элементов */}
      {selectedItems.size > 0 && (
        <div style={{
          position: 'fixed',
          bottom: '40px',
          left: '50%',
          transform: 'translateX(-50%)',
          background: 'white',
          border: '1px solid #d0d0d0',
          borderRadius: '8px',
          padding: '8px',
          display: 'flex',
          gap: '8px',
          boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
          zIndex: 1000
        }}>
          {selectedItems.size === 1 && (
            <>
              {Array.from(selectedItems).some(id => folders.some(f => f.id === id)) && (
                <button
                  onClick={() => {
                    const folderId = Array.from(selectedItems)[0];
                    const folder = folders.find(f => f.id === folderId);
                    if (folder) {
                      setCurrentFolderId(folder.id);
                      navigate(`/files?directoryId=${folder.id}`);
                    }
                    setSelectedItems(new Set());
                  }}
                  style={{
                    padding: '8px 16px',
                    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                    color: 'white',
                    border: 'none',
                    borderRadius: '4px',
                    cursor: 'pointer',
                    fontSize: '0.9em'
                  }}
                >
                  Открыть
                </button>
              )}
              {Array.from(selectedItems).some(id => files.some(f => f.id === id)) && (
                <button
                  onClick={() => {
                    const fileId = Array.from(selectedItems)[0];
                    const file = files.find(f => f.id === fileId);
                    if (file) {
                      handleDownloadFile(file.id, file.fileName);
                    }
                    setSelectedItems(new Set());
                  }}
                  style={{
                    padding: '8px 16px',
                    background: 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
                    color: 'white',
                    border: 'none',
                    borderRadius: '4px',
                    cursor: 'pointer',
                    fontSize: '0.9em'
                  }}
                >
                  Скачать
                </button>
              )}
            </>
          )}
          <button
            onClick={() => {
              selectedItems.forEach(id => {
                const folder = folders.find(f => f.id === id);
                const file = files.find(f => f.id === id);
                if (folder) {
                  handleDeleteFolder(folder.id);
                } else if (file) {
                  handleDeleteFile(file.id);
                }
              });
              setSelectedItems(new Set());
            }}
            style={{
              padding: '8px 16px',
              background: 'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
              color: 'white',
              border: 'none',
              borderRadius: '4px',
              cursor: 'pointer',
              fontSize: '0.9em'
            }}
          >
            Удалить ({selectedItems.size})
          </button>
          <button
            onClick={() => setSelectedItems(new Set())}
            style={{
              padding: '8px 16px',
              background: '#f5f5f5',
              border: '1px solid #d0d0d0',
              borderRadius: '4px',
              cursor: 'pointer',
              fontSize: '0.9em'
            }}
          >
            Отмена
          </button>
        </div>
      )}

      {/* Строка состояния */}
      <div style={{
        background: '#f5f5f5',
        borderTop: '1px solid #e0e0e0',
        padding: '8px 16px',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        fontSize: '0.85em',
        color: '#666'
      }}>
        <div>
          {selectedItems.size > 0 
            ? `Выбрано: ${selectedItems.size} из ${totalItems}`
            : `Всего: ${totalItems} ${totalItems === 1 ? 'элемент' : totalItems < 5 ? 'элемента' : 'элементов'}`
          }
        </div>
        <div>
          {files.length > 0 && `Размер: ${formatFileSize(totalSize)}`}
        </div>
      </div>
    </div>
  );
}
