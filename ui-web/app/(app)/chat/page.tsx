'use client';

import { useAuth } from "@/contexts/AuthContext";
import { Button } from "@/components/ui/button";

export default function Chat() {
  const { user, logout } = useAuth();

  return (
    <div className="flex items-center justify-center min-h-[calc(100vh-4rem)]">
      <div className="flex flex-col items-center gap-6">
        <div className="flex flex-col items-center gap-2">
          <h1 className="text-5xl font-bold bg-gradient-to-r from-black via-purple-500 to-blue-500 bg-clip-text text-transparent">
            Olá {user?.name},
          </h1>
          <p className="text-lg text-muted-foreground">
            Bem vindo ao Tideflow, um aplicativo para você cuidar da sua saúde e bem-estar.
          </p>
        </div>
        <Button 
          onClick={logout}
          variant="outline"
          size="lg"
        >
          Deslogar
        </Button>
      </div>
    </div>
  );
}

