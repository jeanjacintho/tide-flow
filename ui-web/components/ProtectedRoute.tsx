'use client';

import { ReactNode } from 'react';
import { useRequireRole, RequireRoleOptions } from '@/hooks/useRequireRole';
import { Loader2 } from 'lucide-react';

interface ProtectedRouteProps extends RequireRoleOptions {
  children: ReactNode;
  fallback?: ReactNode;
}

/**
 * Componente para proteger rotas baseado em roles do usuário.
 * Não renderiza o conteúdo até verificar a permissão.
 * 
 * @example
 * <ProtectedRoute companyRole="OWNER">
 *   <ManagementPage />
 * </ProtectedRoute>
 * 
 * @example
 * <ProtectedRoute companyRole={['OWNER', 'ADMIN']} fallback={<AccessDenied />}>
 *   <AdminPage />
 * </ProtectedRoute>
 */
export function ProtectedRoute({
  children,
  fallback = null,
  ...roleOptions
}: ProtectedRouteProps) {
  const { hasAccess, isChecking } = useRequireRole(roleOptions);

  // Mostra loading enquanto verifica permissão
  if (isChecking) {
    return (
      <div className="flex items-center justify-center h-screen">
        <Loader2 className="w-8 h-8 animate-spin" />
      </div>
    );
  }

  // Se não tem permissão, mostra fallback ou nada (o hook já redirecionou)
  if (!hasAccess) {
    return <>{fallback}</>;
  }

  // Tem permissão, renderiza o conteúdo
  return <>{children}</>;
}

