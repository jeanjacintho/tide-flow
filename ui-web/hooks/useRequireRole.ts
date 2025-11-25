'use client';

import { useEffect, useLayoutEffect, useMemo, useRef } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/contexts/AuthContext';

type CompanyRole = 'OWNER' | 'ADMIN' | 'HR_MANAGER' | 'NORMAL';
type SystemRole = 'SYSTEM_ADMIN' | 'NORMAL';

interface RequireRoleOptions {
  companyRole?: CompanyRole | CompanyRole[];
  systemRole?: SystemRole | SystemRole[];
  redirectTo?: string;
  requireAll?: boolean;
}

interface UseRequireRoleResult {
  hasAccess: boolean;
  isChecking: boolean;
}

/**
 * Hook robusto para proteger rotas baseado em roles do usuário.
 * Bloqueia completamente a renderização até confirmar a permissão.
 * 
 * IMPORTANTE: Este hook deve ser usado no início do componente, antes de qualquer outro hook.
 */
export function useRequireRole(options: RequireRoleOptions = {}): UseRequireRoleResult {
  const { user, isLoading } = useAuth();
  const router = useRouter();
  const redirectExecuted = useRef(false);
  
  const {
    companyRole,
    systemRole,
    redirectTo = '/chat',
    requireAll = false,
  } = options;

  // Normaliza roles para arrays
  const requiredCompanyRoles = useMemo(() => {
    if (!companyRole) return [];
    return Array.isArray(companyRole) ? companyRole : [companyRole];
  }, [companyRole]);

  const requiredSystemRoles = useMemo(() => {
    if (!systemRole) return [];
    return Array.isArray(systemRole) ? systemRole : [systemRole];
  }, [systemRole]);

  // Calcula permissão de forma síncrona
  const permissionCheck = useMemo(() => {
    // Caso 1: Ainda está carregando o usuário
    if (isLoading) {
      return { hasAccess: false, isChecking: true };
    }

    // Caso 2: Não há usuário autenticado
    if (!user) {
      return { hasAccess: false, isChecking: false };
    }

    // Caso 3: Não há roles especificadas na validação - permite acesso
    if (requiredCompanyRoles.length === 0 && requiredSystemRoles.length === 0) {
      return { hasAccess: true, isChecking: false };
    }

    // Caso 4: Verifica roles de empresa
    let hasCompanyRole = false;
    if (requiredCompanyRoles.length === 0) {
      // Se não especificou companyRole, não valida
      hasCompanyRole = true;
    } else {
      // Se especificou, verifica se o usuário tem uma das roles
      // CRÍTICO: Se user.companyRole for null/undefined, não tem acesso
      if (user.companyRole == null || user.companyRole === undefined) {
        hasCompanyRole = false;
      } else {
        hasCompanyRole = requiredCompanyRoles.includes(user.companyRole as CompanyRole);
      }
    }

    // Caso 5: Verifica roles de sistema
    let hasSystemRole = false;
    if (requiredSystemRoles.length === 0) {
      // Se não especificou systemRole, não valida
      hasSystemRole = true;
    } else {
      // Se especificou, verifica se o usuário tem uma das roles
      // CRÍTICO: Se user.systemRole for null/undefined, não tem acesso
      if (user.systemRole == null || user.systemRole === undefined) {
        hasSystemRole = false;
      } else {
        hasSystemRole = requiredSystemRoles.includes(user.systemRole as SystemRole);
      }
    }

    // Determina permissão final
    if (requiredCompanyRoles.length > 0 && requiredSystemRoles.length > 0) {
      // Se especificou ambos os tipos de roles
      return {
        hasAccess: requireAll
          ? hasCompanyRole && hasSystemRole
          : hasCompanyRole || hasSystemRole,
        isChecking: false
      };
    } else if (requiredCompanyRoles.length > 0) {
      // Se especificou apenas companyRole
      return { hasAccess: hasCompanyRole, isChecking: false };
    } else if (requiredSystemRoles.length > 0) {
      // Se especificou apenas systemRole
      return { hasAccess: hasSystemRole, isChecking: false };
    } else {
      // Se não especificou nada (já tratado no início, mas por segurança)
      return { hasAccess: true, isChecking: false };
    }
  }, [user, isLoading, requiredCompanyRoles, requiredSystemRoles, requireAll]);

  // Redireciona ANTES da renderização usando useLayoutEffect
  useLayoutEffect(() => {
    // Reset do flag quando as dependências mudam
    redirectExecuted.current = false;
  }, [user?.id, isLoading, companyRole, systemRole]);

  // Redireciona imediatamente se não tem permissão
  useLayoutEffect(() => {
    // Só executa uma vez por mudança de estado
    if (redirectExecuted.current) {
      return;
    }

    // Não redireciona se ainda está verificando
    if (permissionCheck.isChecking) {
      return;
    }

    // Não redireciona se não há usuário (o layout já cuida disso)
    if (!user) {
      return;
    }

    // Redireciona se não tem acesso
    if (!permissionCheck.hasAccess) {
      redirectExecuted.current = true;
      // Usa replace para não adicionar ao histórico e forçar navegação
      router.replace(redirectTo);
      // Força um reload se o replace não funcionar imediatamente
      setTimeout(() => {
        if (window.location.pathname !== redirectTo) {
          window.location.href = redirectTo;
        }
      }, 100);
    }
  }, [permissionCheck.hasAccess, permissionCheck.isChecking, user, redirectTo, router]);

  return permissionCheck;
}

/**
 * Hook simplificado para verificar se o usuário tem uma role específica.
 * Útil para renderização condicional de componentes.
 */
export function useHasRole(
  companyRole?: CompanyRole,
  systemRole?: SystemRole
): boolean {
  const { user } = useAuth();

  if (!user) {
    return false;
  }

  const hasCompanyRole = !companyRole || (user.companyRole != null && user.companyRole === companyRole);
  const hasSystemRole = !systemRole || (user.systemRole != null && user.systemRole === systemRole);

  return hasCompanyRole && hasSystemRole;
}

