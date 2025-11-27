'use client';

import Link from "next/link";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Building2, User } from "lucide-react";
import { LightRays } from "@/components/ui/light-rays";
import { AnimatedGradientText } from "@/components/ui/animated-gradient-text";
import { cn } from "@/lib/utils";

export default function Home() {
  return (
    <div className="relative flex flex-col min-h-screen overflow-hidden">
      <LightRays className="fixed inset-0 z-0" />
      <header className="relative z-10 border border-border mt-4 rounded-xl mx-12 bg-foreground/10 backdrop-blur-sm">
        <div className="container mx-auto px-4 py-2 flex justify-between items-center">
          <h1 className="text-2xl font-bold">
            Tideflow
          </h1>
          <nav>
            <Link href="/">
              <Button variant="ghost" size="lg" className="gap-2 h-12">
                Início
              </Button>
            </Link>
            <Link href="/about">
              <Button variant="ghost" size="lg" className="gap-2 h-12">
                Sobre
              </Button>
            </Link>
            <Link href="/contact">
              <Button variant="ghost" size="lg" className="gap-2 h-12">
                Contato
              </Button>
            </Link>
            <Link href="/pricing">
              <Button variant="ghost" size="lg" className="gap-2 h-12">
                Preço
              </Button>
            </Link>
          </nav>
          <nav className="flex gap-4">
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="default" size="lg" className="gap-2 h-12">
                  Entrar
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-56">
                <DropdownMenuItem asChild>
                  <Link href="/login/user" className="flex items-center gap-2 w-full">
                    Login como Usuário
                  </Link>
                </DropdownMenuItem>
                <DropdownMenuItem asChild>
                  <Link href="/login/company" className="flex items-center gap-2 w-full">
                    Login como Empresa
                  </Link>
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
            <Link href="/register">
              <Button size="lg" className="gap-2 h-12">
                Registrar
              </Button>
            </Link>
          </nav>
        </div>
      </header>
      
      <main className="relative z-10 flex-1 flex items-center justify-center">
        <div className="container mx-auto px-4 flex">
          <div className="w-1/2">
            
            <div className="group relative flex items-center justify-center rounded-full px-4 py-1.5">
              <span
                className={cn(
                  "animate-gradient absolute inset-0 block h-full rounded-[inherit] bg-gradient-to-r from-[#ffaa40]/50 via-[#9c40ff]/50 to-[#ffaa40]/50 bg-[length:300%_100%]"
                )}
                style={{
                  WebkitMask:
                    "linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0)",
                  WebkitMaskComposite: "destination-out",
                  mask: "linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0)",
                  maskComposite: "subtract",
                  WebkitClipPath: "padding-box",
                }}
              />
              <AnimatedGradientText className="text-sm font-medium">
                TideFlow
              </AnimatedGradientText>
            </div>
            <h2 className="text-6xl font-bold mb-6 ">
              Cuide da sua saúde e bem-estar
            </h2>
            <p className="text-xl text-muted-foreground mb-8 max-w-2xl mx-auto">
              Tideflow é um aplicativo para gerenciar 
            </p>
            <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <Link href="/register">
                <Button size="lg" className="gap-2 h-12">
                  Teste grátis por 7 dias
                </Button>
              </Link>
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button size="lg" className="gap-2 h-12">
                    Solicitar um orçamento
                  </Button>
                </DropdownMenuTrigger>
                <DropdownMenuContent align="end" className="w-56">
                  <DropdownMenuItem asChild>
                    <Link href="/register/user" className="flex items-center gap-2 w-full">
                      <User className="h-4 w-4" />
                      Registrar como Usuário
                    </Link>
                  </DropdownMenuItem>
                  <DropdownMenuItem asChild>
                    <Link href="/register/company" className="flex items-center gap-2 w-full">
                      <Building2 className="h-4 w-4" />
                      Registrar como Empresa
                    </Link>
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
              
            </div>
          </div>
          <div className="w-1/2">

          </div>
          
        </div>
      </main>
    </div>
  );
}
