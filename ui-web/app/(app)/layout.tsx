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
  SidebarGroup,
  SidebarGroupLabel,
  SidebarGroupContent,
} from "@/components/ui/sidebar";
import { NavUser } from "@/components/nav-user";
import { ThemeToggle } from "@/components/theme-toggle";
import { SubscriptionCard } from "@/components/subscription-card";
import {
  LayoutDashboard,
  MessageSquare,
  Users,
  Calendar,
  Wallet,
  PieChart,
  Settings,
  HelpCircle,
  Building2,
  MessageSquareLock,
} from "lucide-react";
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

  const mainMenuItems = useMemo(() => {
    const items = [];
    
    // Dashboard sempre aparece primeiro para HR_MANAGER, ADMIN e OWNER
    if (user?.companyRole === 'HR_MANAGER' || user?.companyRole === 'ADMIN' || user?.companyRole === 'OWNER') {
      items.push({ 
        href: '/dashboard', 
        icon: LayoutDashboard, 
        label: 'Dashboard', 
        pathname: '/dashboard' 
      });
    }
    
    // Employee Directory - apenas OWNER
    if (user?.companyRole === 'OWNER') {
      items.push({ 
        href: '/employees', 
        icon: Users, 
        label: 'Employee Directory', 
        pathname: '/employees' 
      });
    }
    
    // Attendance & Leave (Departamentos) - apenas OWNER
    if (user?.companyRole === 'OWNER') {
      items.push({ 
        href: '/attendance', 
        icon: Calendar, 
        label: 'Attendance & Leave', 
        pathname: '/attendance' 
      });
    }
    
    // Payroll (Subscription) - OWNER e ADMIN
    if (user?.companyRole === 'OWNER' || user?.companyRole === 'ADMIN') {
      items.push({ 
        href: '/subscription', 
        icon: Wallet, 
        label: 'Payroll', 
        pathname: '/subscription' 
      });
    }
    
    // Reports & Analytics - HR_MANAGER, ADMIN e OWNER
    if (user?.companyRole === 'HR_MANAGER' || user?.companyRole === 'ADMIN' || user?.companyRole === 'OWNER') {
      items.push({ 
        href: '/reports', 
        icon: PieChart, 
        label: 'Reports & Analytics', 
        pathname: '/reports' 
      });
    }
    
    // Chat sempre disponível
    items.push({ 
      href: '/chat', 
      icon: MessageSquare, 
      label: 'Chat', 
      pathname: '/chat' 
    });
    
    return items;
  }, [user]);

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
        <Sidebar>
          <SidebarHeader className="!flex-row px-4 py-2 flex items-center justify-start shrink-0 h-[52px] border-b border-border">
            <div className="flex items-center gap-2">
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary text-primary-foreground">
                <MessageSquareLock className="h-5 w-5" />
              </div>
              <h1 className="text-lg font-bold">tideflow</h1>
            </div>
          </SidebarHeader>
          <SidebarContent className="px-4">
            {user && (
              <div className="mt-4 mb-2">
                <NavUser
                  user={{
                    name: user.name || 'Usuário',
                    email: user.email,
                    avatarUrl: user.avatarUrl,
                  }}
                  onLogout={logout}
                />
              </div>
            )}
            <SidebarGroup>
              <SidebarGroupLabel>MAIN MENU</SidebarGroupLabel>
              <SidebarGroupContent>
                <SidebarMenu>
                  {mainMenuItems.map((item) => (
                    <SidebarMenuItem key={item.href}>
                      <SidebarMenuButton asChild isActive={pathname === item.pathname}>
                        <Link href={item.href}>
                          <item.icon className="w-4 h-4" />
                          <span>{item.label}</span>
                        </Link>
                      </SidebarMenuButton>
                    </SidebarMenuItem>
                  ))}
                </SidebarMenu>
              </SidebarGroupContent>
            </SidebarGroup>
            <SidebarGroup>
              <SidebarGroupLabel>OTHER</SidebarGroupLabel>
              <SidebarGroupContent>
                <SidebarMenu>
                  <SidebarMenuItem>
                    <SidebarMenuButton asChild isActive={pathname === '/settings'}>
                      <Link href="/settings">
                        <Settings className="w-4 h-4" />
                        <span>Settings</span>
                      </Link>
                    </SidebarMenuButton>
                  </SidebarMenuItem>
                  <SidebarMenuItem>
                    <SidebarMenuButton asChild isActive={pathname === '/help'}>
                      <Link href="/help">
                        <HelpCircle className="w-4 h-4" />
                        <span>Help Center</span>
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
              </SidebarGroupContent>
            </SidebarGroup>
          </SidebarContent>
          <SidebarFooter className="p-4 space-y-4">
            {(user?.companyRole === 'OWNER' || user?.companyRole === 'ADMIN') && (
              <SubscriptionCard />
            )}
          </SidebarFooter>
        </Sidebar>
        <SidebarInset className="flex flex-col h-screen overflow-hidden">
          <div className="sticky top-0 z-10 flex items-center justify-between border-b border-border bg-background p-2 shrink-0">
            <SidebarTrigger />
            <ThemeToggle />
          </div>
          <main className="flex-1 overflow-y-auto min-h-0">
            {children}
          </main>
        </SidebarInset>
      </SidebarProvider>
    </div>
  );
}

function HeartIcon({ className }: { className?: string }) {
  return (
    <svg
      className={className}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.29 1.51 4.04 3 5.5l7 7Z" />
    </svg>
  );
}
