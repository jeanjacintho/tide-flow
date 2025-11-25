import AsyncStorage from '@react-native-async-storage/async-storage';

const API_BASE_URL = process.env.EXPO_PUBLIC_API_URL || 'http://localhost:8080';
const AI_SERVICE_URL = process.env.EXPO_PUBLIC_AI_SERVICE_URL || 'http://localhost:8082';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
}

export interface UserResponse {
  id: string;
  name: string;
  username: string;
  email: string;
  phone?: string;
  avatarUrl?: string;
  companyId?: string;
  departmentId?: string;
  systemRole?: string;
  companyRole?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ConversationRequest {
  userId: string;
  message: string;
  conversationId?: string | null;
}

export interface ConversationResponse {
  aiResponse: string;
  conversationId: string;
  isComplete: boolean;
  analisys?: any;
}

export interface Message {
  id: string;
  role: 'USER' | 'ASSISTANT';
  content: string;
  createdAt: string;
  sequenceNumber: number;
}

export interface ConversationHistoryResponse {
  conversationId: string;
  userId: string;
  createdAt: string;
  updatedAt: string;
  messages: Message[];
}

class ApiService {
  private async getAuthToken(): Promise<string | null> {
    try {
      return await AsyncStorage.getItem('auth_token');
    } catch (error) {
      console.error('Error getting auth token:', error);
      return null;
    }
  }

  private async getUserId(): Promise<string | null> {
    try {
      const userData = await AsyncStorage.getItem('user_data');
      if (userData) {
        const user = JSON.parse(userData);
        return user.id || null;
      }
      return null;
    } catch (error) {
      console.error('Error getting user ID:', error);
      return null;
    }
  }

  private async request<T>(
    url: string,
    options: RequestInit = {}
  ): Promise<T> {
    const token = await this.getAuthToken();
    
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      ...(options.headers as Record<string, string>),
    };

    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    console.log('Making request to:', url);
    console.log('Request method:', options.method || 'GET');
    console.log('Request headers:', headers);

    try {
      const response = await fetch(url, {
        ...options,
        headers: headers as HeadersInit,
      });

      console.log('Response status:', response.status);
      console.log('Response ok:', response.ok);

      if (!response.ok) {
        const errorText = await response.text();
        console.error('Error response text:', errorText);
        let errorMessage = `HTTP error! status: ${response.status}`;
        
        try {
          const errorJson = JSON.parse(errorText);
          errorMessage = errorJson.message || errorJson.error || errorMessage;
          console.error('Parsed error message:', errorMessage);
        } catch {
          errorMessage = errorText || errorMessage;
        }
        
        throw new Error(errorMessage);
      }

      const contentType = response.headers.get('content-type');
      const contentLength = response.headers.get('content-length');
      
      if (
        response.status === 204 ||
        contentLength === '0' ||
        !contentType?.includes('application/json')
      ) {
        console.log('Empty response, returning empty object');
        return {} as T;
      }

      const text = await response.text();
      console.log('Response text length:', text.length);
      console.log('Response text preview:', text.substring(0, 200));
      
      if (!text || text.trim() === '') {
        console.warn('Empty response text received');
        // Para login, não podemos retornar objeto vazio
        if (url.includes('/auth/login')) {
          throw new Error('Resposta vazia do servidor de autenticação');
        }
        return {} as T;
      }

      try {
        const parsed = JSON.parse(text) as T;
        console.log('Response parsed successfully:', parsed);
        return parsed;
      } catch (error) {
        console.error('Error parsing JSON response:', error);
        console.error('Response text:', text);
        // Para login, não podemos retornar objeto vazio
        if (url.includes('/auth/login')) {
          throw new Error(`Erro ao processar resposta do servidor: ${error instanceof Error ? error.message : 'Erro desconhecido'}`);
        }
        return {} as T;
      }
    } catch (error) {
      console.error('Fetch error:', error);
      if (error instanceof Error) {
        throw error;
      }
      throw new Error('Erro desconhecido na requisição');
    }
  }

  async sendMessage(
    message: string,
    conversationId?: string,
    userId?: string
  ): Promise<ConversationResponse> {
    const currentUserId = userId || (await this.getUserId());
    
    if (!currentUserId) {
      throw new Error('User ID não encontrado. Faça login novamente.');
    }

    const requestBody: ConversationRequest = {
      userId: currentUserId,
      message,
      conversationId: conversationId || null,
    };

    const response = await this.request<ConversationResponse>(
      `${AI_SERVICE_URL}/api/conversations`,
      {
        method: 'POST',
        body: JSON.stringify(requestBody),
      }
    );

    return {
      aiResponse: response.aiResponse || '',
      conversationId: response.conversationId || '',
      isComplete: response.isComplete || response.complete || false,
      analisys: response.analisys,
    };
  }

  async getConversationHistory(
    conversationId: string,
    userId?: string
  ): Promise<ConversationHistoryResponse> {
    const currentUserId = userId || (await this.getUserId());
    
    if (!currentUserId) {
      throw new Error('User ID não encontrado. Faça login novamente.');
    }

    const response = await this.request<ConversationHistoryResponse>(
      `${AI_SERVICE_URL}/api/conversations/${conversationId}`,
      {
        method: 'GET',
        headers: {
          'X-User-Id': currentUserId,
        },
      }
    );

    return response;
  }

  async getUserConversations(userId?: string): Promise<any[]> {
    const currentUserId = userId || (await this.getUserId());
    
    if (!currentUserId) {
      throw new Error('User ID não encontrado. Faça login novamente.');
    }

    const response = await this.request<any[]>(
      `${AI_SERVICE_URL}/api/conversations/user/${currentUserId}`,
      {
        method: 'GET',
      }
    );

    return response;
  }

  async login(username: string, password: string): Promise<LoginResponse> {
    const requestBody: LoginRequest = {
      username: username.trim(),
      password,
    };

    console.log('Attempting login to:', `${API_BASE_URL}/auth/login`);
    console.log('Request body:', { username: requestBody.username, password: '***' });

    try {
      const response = await this.request<LoginResponse>(
        `${API_BASE_URL}/auth/login`,
        {
          method: 'POST',
          body: JSON.stringify(requestBody),
        }
      );

      console.log('Login response received:', response);
      console.log('Login response has token:', !!response.token);
      console.log('Login response type:', typeof response);
      console.log('Login response keys:', Object.keys(response));

      if (!response || !response.token) {
        console.error('Invalid login response:', response);
        throw new Error('Resposta inválida do servidor. Token não encontrado.');
      }

      await AsyncStorage.setItem('auth_token', response.token);
      console.log('Token saved to AsyncStorage');
      return response;
    } catch (error) {
      console.error('Login error details:', error);
      throw error;
    }
  }

  async getCurrentUser(): Promise<UserResponse> {
    const token = await this.getAuthToken();
    
    if (!token) {
      throw new Error('Token não encontrado. Faça login novamente.');
    }

    // Decodifica o token JWT para obter o user ID
    // O token JWT tem formato: header.payload.signature
    try {
      const parts = token.split('.');
      if (parts.length !== 3) {
        throw new Error('Token inválido');
      }

      // Decodifica o payload (segunda parte do token)
      const payload = JSON.parse(atob(parts[1]));
      
      // Tenta obter o user ID do payload
      // O backend usa 'user_id' como claim no token JWT
      const userId = payload.user_id || payload.sub || payload.userId || payload.id;
      
      if (!userId) {
        throw new Error('User ID não encontrado no token');
      }

      return await this.getUserById(userId);
    } catch (error) {
      console.error('Error decoding token:', error);
      if (error instanceof Error) {
        throw new Error(`Erro ao decodificar token: ${error.message}`);
      }
      throw new Error('Não foi possível obter informações do usuário');
    }
  }

  async getUserById(userId: string): Promise<UserResponse> {
    const response = await this.request<UserResponse>(
      `${API_BASE_URL}/users/${userId}`,
      {
        method: 'GET',
      }
    );

    return response;
  }
}

export const apiService = new ApiService();

