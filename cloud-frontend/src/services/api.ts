import { getUserId } from './auth';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8091';

interface ApiResponse<T> {
  data: T;
}

interface LoginRequest {
  email: string;
  password: string;
}

interface RegisterRequest {
  email: string;
  password: string;
  confirm_password: string;
  firstName: string;
  lastName: string;
  phone_number: string;
}

interface TokenResponse {
  access_token: string;
  refresh_token: string;
}

interface FileDto {
  id: string;
  userId: string;
  directoryId: string;
  fileName: string;
  fileSize: number;
  mimeType: string;
  downloadUrl?: string;
}

interface FilePageDto {
  items: FileDto[];
  total: number;
  limit: number;
  offset: number;
}

interface FileUploadResponseDto {
  files: FileDto[];
}

interface FileDeleteResponseDto {
  id: string;
}

interface DirectoryDto {
  id: string;
  userId: string;
  parentId?: string;
  name: string;
  path?: string;
}

interface DirectoryPageDto {
  items: DirectoryDto[];
}

interface DirectoryWriteDto {
  userId: string;
  parentId?: string;
  name: string;
  path?: string;
}

interface DirectoryWriteResponseDto {
  id: string;
}

export type FileResponse = FileDto;
export type FolderResponse = DirectoryDto;

const getAuthHeaders = () => {
  const token = localStorage.getItem('token');
  return {
    'Authorization': token ? `Bearer ${token}` : '',
    'Content-Type': 'application/json',
  };
};

const getAuthHeadersMultipart = () => {
  const token = localStorage.getItem('token');
  return {
    'Authorization': token ? `Bearer ${token}` : '',
  };
};

const handleResponse = async <T>(response: Response): Promise<ApiResponse<T>> => {
  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: 'Ошибка сервера' }));
    throw { response: { data: error, status: response.status } };
  }
  
  // Проверяем, есть ли тело ответа
  const contentType = response.headers.get('content-type');
  const text = await response.text();
  
  if (!text || text.trim() === '') {
    // Если тело пустое, но статус успешный (например, 201), возвращаем пустой объект
    return { data: {} as T };
  }
  
  if (contentType && contentType.includes('application/json')) {
    try {
      const json = JSON.parse(text);
      // Если ответ уже обернут в data, возвращаем как есть, иначе оборачиваем
      if (json.data !== undefined) {
        return json;
      }
      // Проверяем, является ли ответ уже нужным типом (например, TokenResponse)
      if (json.access_token || json.accessToken || json.access_token === null) {
        return { data: json as T };
      }
      return { data: json };
    } catch (e) {
      console.error('Failed to parse JSON:', e, text);
      // Если не удалось распарсить, возвращаем как есть
      return { data: text as any };
    }
  }
  
  // Если не JSON, возвращаем текст
  return { data: text as any };
};

export const authApi = {
  async login(credentials: LoginRequest): Promise<ApiResponse<TokenResponse>> {
    const response = await fetch(`${API_URL}/v1/auth/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(credentials),
    });
    return handleResponse<TokenResponse>(response);
  },

  async register(data: RegisterRequest): Promise<ApiResponse<TokenResponse>> {
    const response = await fetch(`${API_URL}/v1/auth/registration`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(data),
    });
    
    // Логирование для отладки
    
    return handleResponse<TokenResponse>(response);
  },

  async logout(): Promise<void> {
    const token = localStorage.getItem('token');
    
    // Отправляем запрос на сервер для добавления токена в blacklist
    if (token) {
      try {
        const response = await fetch(`${API_URL}/v1/auth/logout`, {
          method: 'POST',
          headers: getAuthHeaders(),
        });
        
        if (response.ok) {
        } else {
          const errorText = await response.text();
          console.warn('Logout response error:', response.status, errorText);
        }
      } catch (error) {
        // Игнорируем ошибки при logout, так как токен все равно будет удален локально
        console.error('Failed to logout on server:', error);
      }
    } else {
      console.warn('No token found in localStorage, skipping server logout');
    }
    
    // Удаляем токены из localStorage
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('userId');
  },
};

export const fileApi = {
  async getFiles(folderId?: string, limit = 20, offset = 0): Promise<ApiResponse<FilePageDto>> {
    const userId = getUserId();
    if (!userId) {
      throw { response: { data: { message: 'User ID not found' }, status: 401 } };
    }
    
    const params = new URLSearchParams({
      userId,
      limit: limit.toString(),
      offset: offset.toString(),
    });
    if (folderId) {
      params.append('directoryId', folderId);
    }
    const response = await fetch(`${API_URL}/v1/files?${params}`, {
      headers: getAuthHeaders(),
    });
    return handleResponse<FilePageDto>(response);
  },

  async uploadFile(file: File, folderId?: string): Promise<ApiResponse<FileUploadResponseDto>> {
    const userId = getUserId();
    if (!userId) {
      throw { response: { data: { message: 'User ID not found' }, status: 401 } };
    }
    
    const formData = new FormData();
    formData.append('files', file);

    const params = new URLSearchParams();
    params.append('userId', userId);
    if (folderId) {
      params.append('directoryId', folderId);
    }

    const response = await fetch(`${API_URL}/v1/files?${params}`, {
      method: 'POST',
      headers: getAuthHeadersMultipart(),
      body: formData,
    });
    return handleResponse<FileUploadResponseDto>(response);
  },

  async uploadFiles(files: File[], folderId?: string): Promise<ApiResponse<FileUploadResponseDto>> {
    const userId = getUserId();
    if (!userId) {
      throw { response: { data: { message: 'User ID not found' }, status: 401 } };
    }
    
    const formData = new FormData();
    files.forEach(file => {
      formData.append('files', file);
    });

    const params = new URLSearchParams();
    params.append('userId', userId);
    if (folderId) {
      params.append('directoryId', folderId);
    }

    const response = await fetch(`${API_URL}/v1/files?${params}`, {
      method: 'POST',
      headers: getAuthHeadersMultipart(),
      body: formData,
    });
    return handleResponse<FileUploadResponseDto>(response);
  },

  async deleteFile(id: string): Promise<ApiResponse<FileDeleteResponseDto>> {
    const response = await fetch(`${API_URL}/v1/files/${id}`, {
      method: 'DELETE',
      headers: getAuthHeaders(),
    });
    return handleResponse<FileDeleteResponseDto>(response);
  },

  async getFileDownloadLink(id: string): Promise<string> {
    return `${API_URL}/v1/files/download/${id}`;
  },

  async downloadFile(id: string): Promise<Blob> {
    const response = await fetch(`${API_URL}/v1/files/download/${id}`, {
      headers: getAuthHeadersMultipart(),
    });
    if (!response.ok) {
      throw new Error('Ошибка загрузки файла');
    }
    return response.blob();
  },
};

export const folderApi = {
  async getFolders(parentId?: string): Promise<ApiResponse<DirectoryPageDto>> {
    const userId = getUserId();
    const params = new URLSearchParams();
    if (userId) {
      params.append('userId', userId);
    }
    if (parentId) {
      params.append('parentId', parentId);
    }
    const response = await fetch(`${API_URL}/v1/directories?${params}`, {
      headers: getAuthHeaders(),
    });
    return handleResponse<DirectoryPageDto>(response);
  },

  async createFolder(data: { name: string; parentId?: string }): Promise<ApiResponse<DirectoryWriteResponseDto>> {
    const userId = getUserId();
    if (!userId) {
      throw { response: { data: { message: 'User ID not found' }, status: 401 } };
    }
    
    const directoryData: DirectoryWriteDto = {
      userId,
      name: data.name,
      parentId: data.parentId,
    };
    const response = await fetch(`${API_URL}/v1/directories`, {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(directoryData),
    });
    return handleResponse<DirectoryWriteResponseDto>(response);
  },

  async deleteFolder(id: string): Promise<void> {
    const response = await fetch(`${API_URL}/v1/directories/${id}`, {
      method: 'DELETE',
      headers: getAuthHeaders(),
    });
    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Ошибка сервера' }));
      throw { response: { data: error, status: response.status } };
    }
  },

  async getFolderLink(id: string): Promise<string> {
    return `${window.location.origin}/folders/${id}`;
  },
};

