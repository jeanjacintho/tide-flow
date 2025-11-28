'use client';

import { ReactNode } from 'react';
import { useRequireRole, RequireRoleOptions } from '@/hooks/useRequireRole';
import { Loader2 } from 'lucide-react';

interface ProtectedRouteProps extends RequireRoleOptions {
  children: ReactNode;
  fallback?: ReactNode;
}

export function ProtectedRoute({
  children,
  fallback = null,
  ...roleOptions
}: ProtectedRouteProps) {
  const { hasAccess, isChecking } = useRequireRole(roleOptions);

  if (isChecking) {
    return (
      <div className="flex items-center justify-center h-screen">
        <Loader2 className="w-8 h-8 animate-spin" />
      </div>
    );
  }

  if (!hasAccess) {
    return <>{fallback}</>;
  }

  return <>{children}</>;
}
