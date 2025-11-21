'use client';

import { useAuth } from "@/contexts/AuthContext";
import { useRouter } from "next/navigation";
import { useEffect } from "react";
import {
  Sidebar,
  SidebarProvider,
  SidebarHeader,
  SidebarFooter,
  SidebarContent,
  SidebarInset,
  SidebarTrigger,
} from "@/components/ui/sidebar";
import { NavUser } from "@/components/nav-user";
import { ThemeToggle } from "@/components/theme-toggle";
import { HeartIcon } from "lucide-react";

export default function AppLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { isLoading, isAuthenticated, user, logout } = useAuth();
  const router = useRouter();

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
          <SidebarContent />
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

