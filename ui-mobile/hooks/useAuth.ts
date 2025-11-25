import { useState, useEffect } from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { apiService, UserResponse } from '../services/api';

export interface User {
  id: string;
  name: string;
  email: string;
  phone?: string;
  avatarUrl?: string;
  companyId?: string;
  departmentId?: string;
  systemRole?: string;
  companyRole?: string;
}

interface UseAuthReturn {
  user: User | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  login: (username: string, password: string) => Promise<void>;
  setUser: (user: User | null) => Promise<void>;
  logout: () => Promise<void>;
}

export const useAuth = (): UseAuthReturn => {
  const [user, setUserState] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    loadUser();
  }, []);

  const loadUser = async () => {
    try {
      const token = await AsyncStorage.getItem('auth_token');
      if (token) {
        // Tenta carregar dados do usuário do AsyncStorage primeiro
        const userData = await AsyncStorage.getItem('user_data');
        if (userData) {
          try {
            const parsedUser = JSON.parse(userData);
            if (parsedUser && parsedUser.id && parsedUser.name && parsedUser.email) {
              setUserState(parsedUser);
            } else {
              // Dados inválidos, busca do backend
              await loadUserFromAPI();
            }
          } catch (error) {
            console.error('Error parsing saved user data:', error);
            await loadUserFromAPI();
          }
        } else {
          // Se não tiver dados salvos, busca do backend
          await loadUserFromAPI();
        }
      }
    } catch (error) {
      console.error('Error loading user:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const loadUserFromAPI = async () => {
    try {
      const userResponse = await apiService.getCurrentUser();
      
      if (!userResponse) {
        throw new Error('Resposta do servidor inválida');
      }
      
      if (!userResponse.id || !userResponse.name || !userResponse.email) {
        throw new Error('Dados do usuário incompletos');
      }
      
      const user: User = {
        id: userResponse.id,
        name: userResponse.name || '',
        email: userResponse.email || '',
        phone: userResponse.phone,
        avatarUrl: userResponse.avatarUrl,
        companyId: userResponse.companyId,
        departmentId: userResponse.departmentId,
        systemRole: userResponse.systemRole,
        companyRole: userResponse.companyRole,
      };
      
      await setUser(user);
    } catch (error) {
      console.error('Error loading user from API:', error);
      // Se falhar, limpa o token inválido
      await AsyncStorage.removeItem('auth_token');
    }
  };

  const login = async (username: string, password: string) => {
    try {
      console.log('useAuth.login called with username:', username);
      setIsLoading(true);
      
      console.log('Calling apiService.login...');
      const loginResponse = await apiService.login(username, password);
      console.log('apiService.login completed, response:', { hasToken: !!loginResponse.token });
      
      if (loginResponse.token) {
        console.log('Token received, fetching user data...');
        // Busca dados do usuário após login
        const userResponse = await apiService.getCurrentUser();
        
        if (!userResponse) {
          throw new Error('Resposta do servidor inválida. Dados do usuário não encontrados.');
        }
        
        console.log('User data fetched:', { 
          id: userResponse.id, 
          name: userResponse.name,
          hasName: !!userResponse.name 
        });
        
        if (!userResponse.id || !userResponse.name || !userResponse.email) {
          throw new Error('Dados do usuário incompletos recebidos do servidor.');
        }
        
        const user: User = {
          id: userResponse.id,
          name: userResponse.name || '',
          email: userResponse.email || '',
          phone: userResponse.phone,
          avatarUrl: userResponse.avatarUrl,
          companyId: userResponse.companyId,
          departmentId: userResponse.departmentId,
          systemRole: userResponse.systemRole,
          companyRole: userResponse.companyRole,
        };
        
        console.log('Setting user state...');
        await setUser(user);
        console.log('User state set, login completed successfully');
      } else {
        console.error('No token in login response');
        throw new Error('Token não recebido do servidor');
      }
    } catch (error) {
      console.error('Error during login in useAuth:', error);
      if (error instanceof Error) {
        console.error('Error message:', error.message);
        console.error('Error stack:', error.stack);
      }
      throw error;
    } finally {
      setIsLoading(false);
      console.log('Login process finished, isLoading set to false');
    }
  };

  const setUser = async (newUser: User | null) => {
    try {
      if (newUser) {
        await AsyncStorage.setItem('user_data', JSON.stringify(newUser));
        setUserState(newUser);
      } else {
        await AsyncStorage.removeItem('user_data');
        setUserState(null);
      }
    } catch (error) {
      console.error('Error saving user:', error);
    }
  };

  const logout = async () => {
    try {
      await AsyncStorage.multiRemove(['auth_token', 'user_data']);
      setUserState(null);
    } catch (error) {
      console.error('Error logging out:', error);
    }
  };

  return {
    user,
    isLoading,
    isAuthenticated: !!user,
    login,
    setUser,
    logout,
  };
};

