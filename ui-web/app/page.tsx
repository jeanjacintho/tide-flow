'use client';

import Link from "next/link";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Building2, User, ChevronDown } from "lucide-react";

export default function Home() {
  return (
    <div className="flex flex-col min-h-screen">
      <header className="border-b">
        <div className="container mx-auto px-4 py-4 flex justify-between items-center">
          <h1 className="text-2xl font-bold bg-gradient-to-r from-black via-purple-500 to-blue-500 bg-clip-text text-transparent">
            Tideflow
          </h1>
          <nav className="flex gap-4">
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" className="gap-2">
                  Entrar
                  <ChevronDown className="h-4 w-4" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-56">
                <DropdownMenuItem asChild>
                  <Link href="/login/user" className="flex items-center gap-2 w-full">
                    <User className="h-4 w-4" />
                    Login como Usuário
                  </Link>
                </DropdownMenuItem>
                <DropdownMenuItem asChild>
                  <Link href="/login/company" className="flex items-center gap-2 w-full">
                    <Building2 className="h-4 w-4" />
                    Login como Empresa
                  </Link>
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
            <Link href="/register">
              <Button>Registrar</Button>
            </Link>
          </nav>
        </div>
      </header>
      
      <main className="flex-1 flex items-center justify-center">
        <div className="container mx-auto px-4 text-center">
          <h2 className="text-6xl font-bold mb-6 bg-gradient-to-r from-black via-purple-500 to-blue-500 bg-clip-text text-transparent">
            Cuide da sua saúde e bem-estar
          </h2>
          <p className="text-xl text-muted-foreground mb-8 max-w-2xl mx-auto">
            Tideflow é um aplicativo completo para você gerenciar sua saúde e bem-estar de forma simples e eficiente.
          </p>
          <div className="flex gap-4 justify-center">
            <Link href="/register">
              <Button size="lg">Começar agora</Button>
            </Link>
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button size="lg" variant="outline" className="gap-2">
                  Já tenho conta
                  <ChevronDown className="h-4 w-4" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-56">
                <DropdownMenuItem asChild>
                  <Link href="/login/user" className="flex items-center gap-2 w-full">
                    <User className="h-4 w-4" />
                    Login como Usuário
                  </Link>
                </DropdownMenuItem>
                <DropdownMenuItem asChild>
                  <Link href="/login/company" className="flex items-center gap-2 w-full">
                    <Building2 className="h-4 w-4" />
                    Login como Empresa
                  </Link>
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </div>
      </main>
    </div>
  );
}
