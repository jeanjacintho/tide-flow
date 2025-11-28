'use client';

import React, { createContext, useContext, useState, useEffect, useMemo, useCallback, ReactNode } from 'react';
import { apiService, User } from '@/lib/api';
import { useRouter } from 'next/navigation';

interface AuthContextType {
  user: User | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  login: (username: string, password: string) => Promise<void>;
  register: (name: string, email: string, password: string) => Promise<void>;
  logout: () => void;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const router = useRouter();

  useEffect(() => {
    let isMounted = true;

    const loadUser = async () => {
      const token = localStorage.getItem('auth_token');
      if (!token) {
        if (isMounted) {
          setIsLoading(false);
        }
        return;
      }

      try {
        const userData = await apiService.getCurrentUser();
        if (isMounted) {
          setUser(userData);
        }
      } catch (error) {
        if (isMounted) {
          localStorage.removeItem('auth_token');
          setUser(null);
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    };

    loadUser();

    return () => {
      isMounted = false;
    };
  }, []);

  const login = useCallback(async (username: string, password: string) => {
    try {
      const response = await apiService.login({ username, password });
      localStorage.setItem('auth_token', response.token);

      const userData = await apiService.getCurrentUser();
      setUser(userData);

      if (userData.companyId) {

        if (userData.companyRole === 'HR_MANAGER' || userData.companyRole === 'ADMIN' || userData.companyRole === 'OWNER') {
          router.push('/dashboard');
        } else {

          router.push('/chat');
        }
      } else {
        router.push('/chat');
      }
    } catch (error) {
      throw error;
    }
  }, [router]);

  const register = useCallback(async (name: string, email: string, password: string) => {
    try {
      await apiService.register({
        name,
        email,
        password,
        role: 'USER',
      });

      await login(email, password);
    } catch (error) {
      throw error;
    }
  }, [login]);

  const logout = useCallback(() => {
    localStorage.removeItem('auth_token');
    setUser(null);
    router.push('/login/user');
  }, [router]);

  const refreshUser = useCallback(async () => {
    const token = localStorage.getItem('auth_token');
    if (!token) {
      setUser(null);
      return;
    }

    try {
      const userData = await apiService.getCurrentUser();
      setUser(userData);
    } catch (error) {
      localStorage.removeItem('auth_token');
      setUser(null);
    }
  }, []);

  const contextValue = useMemo(() => ({
    user,
    isLoading,
    isAuthenticated: !!user,
    login,
    register,
    logout,
    refreshUser,
  }), [user, isLoading, login, register, logout, refreshUser]);

  return (
    <AuthContext.Provider value={contextValue}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
