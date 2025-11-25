'use client';

import { useAuth } from "@/contexts/AuthContext";
import { useRouter, usePathname } from "next/navigation";
import { useEffect } from "react";
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
import { HeartIcon, LayoutDashboard, MessageSquare, User, TrendingUp, Building2 } from "lucide-react";
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
      router.push('/login');
    }
  }, [isLoading, isAuthenticated, router]);

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
              <SidebarMenuItem>
                <SidebarMenuButton asChild isActive={pathname === '/dashboard'}>
                  <Link href="/dashboard">
                    <LayoutDashboard className="w-4 h-4" />
                    <span>Dashboard</span>
                  </Link>
                </SidebarMenuButton>
              </SidebarMenuItem>
              <SidebarMenuItem>
                <SidebarMenuButton asChild isActive={pathname === '/chat'}>
                  <Link href="/chat">
                    <MessageSquare className="w-4 h-4" />
                    <span>Chat</span>
                  </Link>
                </SidebarMenuButton>
              </SidebarMenuItem>
              <SidebarMenuItem>
                <SidebarMenuButton asChild isActive={pathname === '/subscription'}>
                  <Link href="/subscription">
                    <TrendingUp className="w-4 h-4" />
                    <span>Assinatura</span>
                  </Link>
                </SidebarMenuButton>
              </SidebarMenuItem>
              <SidebarMenuItem>
                <SidebarMenuButton asChild isActive={pathname === '/profile'}>
                  <Link href="/profile">
                    <User className="w-4 h-4" />
                    <span>Perfil</span>
                  </Link>
                </SidebarMenuButton>
              </SidebarMenuItem>
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

