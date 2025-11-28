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

  const requiredCompanyRoles = useMemo(() => {
    if (!companyRole) return [];
    return Array.isArray(companyRole) ? companyRole : [companyRole];
  }, [companyRole]);

  const requiredSystemRoles = useMemo(() => {
    if (!systemRole) return [];
    return Array.isArray(systemRole) ? systemRole : [systemRole];
  }, [systemRole]);

  const permissionCheck = useMemo(() => {

    if (isLoading) {
      return { hasAccess: false, isChecking: true };
    }

    if (!user) {
      return { hasAccess: false, isChecking: false };
    }

    if (requiredCompanyRoles.length === 0 && requiredSystemRoles.length === 0) {
      return { hasAccess: true, isChecking: false };
    }

    let hasCompanyRole = false;
    if (requiredCompanyRoles.length === 0) {

      hasCompanyRole = true;
    } else {

      if (user.companyRole == null || user.companyRole === undefined) {
        hasCompanyRole = false;
      } else {
        hasCompanyRole = requiredCompanyRoles.includes(user.companyRole as CompanyRole);
      }
    }

    let hasSystemRole = false;
    if (requiredSystemRoles.length === 0) {

      hasSystemRole = true;
    } else {

      if (user.systemRole == null || user.systemRole === undefined) {
        hasSystemRole = false;
      } else {
        hasSystemRole = requiredSystemRoles.includes(user.systemRole as SystemRole);
      }
    }

    if (requiredCompanyRoles.length > 0 && requiredSystemRoles.length > 0) {

      return {
        hasAccess: requireAll
          ? hasCompanyRole && hasSystemRole
          : hasCompanyRole || hasSystemRole,
        isChecking: false
      };
    } else if (requiredCompanyRoles.length > 0) {

      return { hasAccess: hasCompanyRole, isChecking: false };
    } else if (requiredSystemRoles.length > 0) {

      return { hasAccess: hasSystemRole, isChecking: false };
    } else {

      return { hasAccess: true, isChecking: false };
    }
  }, [user, isLoading, requiredCompanyRoles, requiredSystemRoles, requireAll]);

  useLayoutEffect(() => {

    redirectExecuted.current = false;
  }, [user?.id, isLoading, companyRole, systemRole]);

  useLayoutEffect(() => {

    if (redirectExecuted.current) {
      return;
    }

    if (permissionCheck.isChecking) {
      return;
    }

    if (!user) {
      return;
    }

    if (!permissionCheck.hasAccess) {
      redirectExecuted.current = true;

      router.replace(redirectTo);

      setTimeout(() => {
        if (window.location.pathname !== redirectTo) {
          window.location.href = redirectTo;
        }
      }, 100);
    }
  }, [permissionCheck.hasAccess, permissionCheck.isChecking, user, redirectTo, router]);

  return permissionCheck;
}

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
