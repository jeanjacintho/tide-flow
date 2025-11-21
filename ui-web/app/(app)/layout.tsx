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
    <SidebarProvider>
      <div className="flex min-h-screen w-full">
        <Sidebar>
          <SidebarHeader className="border-b p-4 flex flex-row items-center h-[70px]">
            <h1 className="text-lg font-semibold">tideflow</h1>
          </SidebarHeader>
          <SidebarContent />
          <SidebarFooter className="border-t">
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
        <SidebarInset>
          <div className="sticky top-0 z-10 flex items-center justify-between border-b bg-background p-4 h-[70px]">
            <SidebarTrigger />
            <ThemeToggle />
          </div>
          <main className="flex-1">
            {children}
          </main>
        </SidebarInset>
      </div>
    </SidebarProvider>
  );
}

