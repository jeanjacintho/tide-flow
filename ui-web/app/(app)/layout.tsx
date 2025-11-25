'use client';

import { useAuth } from "@/contexts/AuthContext";
import { useRouter, usePathname } from "next/navigation";
import { useEffect, useMemo } from "react";
import {
  Sidebar,
  SidebarProvider,
  SidebarHeader,
  SidebarFooter,
  SidebarContent,
  SidebarInset,
  SidebarTrigger,
  SidebarMenu,
  SidebarMenuItem,
  SidebarMenuButton,
} from "@/components/ui/sidebar";
import { NavUser } from "@/components/nav-user";
import { ThemeToggle } from "@/components/theme-toggle";
import { SubscriptionCard } from "@/components/subscription-card";
import { HeartIcon, LayoutDashboard, MessageSquare, User, TrendingUp, Building2, Settings } from "lucide-react";
import Link from "next/link";

export default function AppLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { isLoading, isAuthenticated, user, logout } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      router.push('/login/user');
    }
  }, [isLoading, isAuthenticated]);

  const menuItems = useMemo(() => [
    { href: '/dashboard', icon: LayoutDashboard, label: 'Dashboard', pathname: '/dashboard' },
    { href: '/chat', icon: MessageSquare, label: 'Chat', pathname: '/chat' },
    { href: '/subscription', icon: TrendingUp, label: 'Assinatura', pathname: '/subscription' },
    { href: '/profile', icon: User, label: 'Perfil', pathname: '/profile' },
  ], []);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-screen">
        <div className="text-lg">Carregando...</div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return null;
  }

  return (
    <div className="flex min-h-screen w-full">
      <SidebarProvider>
        <Sidebar variant="inset" collapsible="offcanvas">
          <SidebarHeader className="p-4 flex flex-row items-center">
            <HeartIcon className="w-6 h-6" />
            <h1 className="text-lg font-semibold">tideflow</h1>
          </SidebarHeader>
          <SidebarContent>
            <SidebarMenu>
              {menuItems.map((item) => (
                <SidebarMenuItem key={item.href}>
                  <SidebarMenuButton asChild isActive={pathname === item.pathname}>
                    <Link href={item.href}>
                      <item.icon className="w-4 h-4" />
                      <span>{item.label}</span>
                    </Link>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              ))}
              {user?.companyRole === 'OWNER' && (
                <SidebarMenuItem>
                  <SidebarMenuButton asChild isActive={pathname === '/management'}>
                    <Link href="/management">
                      <Settings className="w-4 h-4" />
                      <span>Gerenciamento</span>
                    </Link>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              )}
              {user?.systemRole === 'SYSTEM_ADMIN' && (
                <SidebarMenuItem>
                  <SidebarMenuButton asChild isActive={pathname === '/companies'}>
                    <Link href="/companies">
                      <Building2 className="w-4 h-4" />
                      <span>Empresas</span>
                    </Link>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              )}
            </SidebarMenu>
          </SidebarContent>
          <SidebarFooter>
            <SubscriptionCard />
            {user && (
              <NavUser
                user={{
                  name: user.name || 'Usuário',
                  email: user.email,
                  avatarUrl: user.avatarUrl,
                }}
                onLogout={logout}
              />
            )}
          </SidebarFooter>
        </Sidebar>
        <SidebarInset className="flex flex-col overflow-hidden border border-border">
          <div className="sticky top-0 z-10 flex items-center justify-between border-b border-border bg-background p-2 shrink-0">
            <SidebarTrigger />
            <ThemeToggle />
          </div>
          <main className="flex-1 overflow-hidden">
            {children}
          </main>
        </SidebarInset>
      </SidebarProvider>
    </div>
  );
}
