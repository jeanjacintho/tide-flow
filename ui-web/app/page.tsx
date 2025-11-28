'use client';

import Link from "next/link";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Building2, User, ArrowRight, Zap, Lock, TrendingUp, Users } from "lucide-react";
import { cn } from "@/lib/utils";
import { AnimatedGradientText } from "@/components/ui/animated-gradient-text";
import { AuroraText } from "@/components/ui/aurora-text";

export default function Home() {
  return (
    <div className="relative flex flex-col min-h-screen overflow-hidden bg-background">
      {/* Header */}
      <header className="relative z-10 border-b border-border">
        <div className="container mx-auto px-4 py-4 flex justify-between items-center">
          <h1 className="text-2xl font-bold text-foreground">
            TideFlow
          </h1>
          <div className="flex items-center gap-6">
            <nav className="hidden md:flex items-center gap-6">
              <Link href="/about" className="text-sm text-muted-foreground hover:text-foreground transition-colors">
                Sobre
              </Link>
              <Link href="/pricing" className="text-sm text-muted-foreground hover:text-foreground transition-colors">
                Preços
              </Link>
              <Link href="/docs" className="text-sm text-muted-foreground hover:text-foreground transition-colors">
                Documentação
              </Link>
              <Link href="/blog" className="text-sm text-muted-foreground hover:text-foreground transition-colors">
                Blog
              </Link>
            </nav>
            <div className="flex items-center gap-3">
              <DropdownMenu>
                <DropdownMenuTrigger asChild>
                  <Button variant="ghost" size="sm" className="text-sm">
                    Entrar
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
                <Button size="sm" className="text-sm">
                  Cadastre-se
                </Button>
              </Link>
            </div>
          </div>
        </div>
      </header>
      
      {/* Main Content */}
      <main className="relative z-10 flex-1">
        <div className="container mx-auto px-4 py-8 lg:py-12">
          <div className="grid lg:grid-cols-2 gap-12 items-center">
            {/* Left Side - Hero Content */}
            <div className="space-y-8">
              {/* Social Proof */}
              <div className="group relative mx-auto flex items-center justify-center rounded-full px-4 py-1.5 shadow-[inset_0_-8px_10px_oklch(86.06%_0.1766_134.52/0.1)] transition-shadow duration-500 ease-out hover:shadow-[inset_0_-5px_10px_oklch(86.06%_0.1766_134.52/0.2)] w-fit">
                <span
                  className={cn(
                    "animate-gradient absolute inset-0 block h-full w-full rounded-[inherit] bg-gradient-to-r from-[oklch(86.06%_0.1766_134.52/0.5)] via-[oklch(86.06%_0.1766_134.52/0.7)] to-[oklch(86.06%_0.1766_134.52/0.5)] bg-[length:300%_100%] p-[1px]"
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
                <div className="flex items-center gap-2">
                  <div className="flex -space-x-2">
                    <div className="w-6 h-6 rounded-full bg-primary/20 border-2 border-background"></div>
                    <div className="w-6 h-6 rounded-full bg-primary/30 border-2 border-background"></div>
                    <div className="w-6 h-6 rounded-full bg-primary/40 border-2 border-background"></div>
                  </div>
                  <AnimatedGradientText className="text-sm font-medium">
                    Escolhido por +6000 empresas
                  </AnimatedGradientText>
                </div>
              </div>

              {/* Headline */}
              <h1 className="text-4xl lg:text-5xl font-bold leading-tight">
                <span className="text-foreground">Identifique o burnout</span>{' '}
                <AuroraText>antes que ele destrua sua equipe</AuroraText>
              </h1>

              {/* Subtitle */}
              <p className="text-lg text-muted-foreground max-w-xl">
                Infraestrutura de bem-estar emocional em poucas linhas. Feito para RH, gestores e empresas que valorizam prevenção.
              </p>

              {/* CTA Buttons */}
              <div className="flex items-center gap-4">
                <Link href="/register">
                  <Button size="lg" className="gap-2">
                    Integre ao seu negócio
                    <ArrowRight className="h-4 w-4" />
                  </Button>
                </Link>
                <Link href="/about" className="text-sm text-muted-foreground hover:text-foreground transition-colors">
                  Por que usar?
                </Link>
              </div>

              {/* Features */}
              <div className="grid grid-cols-2 gap-6 pt-4">
                <div className="flex items-start gap-3">
                  <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center flex-shrink-0">
                    <Zap className="h-5 w-5 text-primary" />
                  </div>
                  <div>
                    <p className="font-semibold text-sm mb-1">Rápido</p>
                    <p className="text-xs text-muted-foreground">Integre em minutos, não dias</p>
                  </div>
                </div>
                <div className="flex items-start gap-3">
                  <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center flex-shrink-0">
                    <Lock className="h-5 w-5 text-primary" />
                  </div>
                  <div>
                    <p className="font-semibold text-sm mb-1">Seguro</p>
                    <p className="text-xs text-muted-foreground">Privacidade garantida</p>
                  </div>
                </div>
                <div className="flex items-start gap-3">
                  <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center flex-shrink-0">
                    <TrendingUp className="h-5 w-5 text-primary" />
                  </div>
                  <div>
                    <p className="font-semibold text-sm mb-1">Inteligente</p>
                    <p className="text-xs text-muted-foreground">IA proativa e preditiva</p>
                  </div>
                </div>
                <div className="flex items-start gap-3">
                  <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center flex-shrink-0">
                    <Users className="h-5 w-5 text-primary" />
                  </div>
                  <div>
                    <p className="font-semibold text-sm mb-1">Escalável</p>
                    <p className="text-xs text-muted-foreground">Para empresas de todos os tamanhos</p>
                  </div>
                </div>
              </div>
            </div>

            {/* Right Side - Dashboard Mockup */}
            <div className="relative hidden lg:block">
              <div className="relative">
                {/* Background Dashboard */}
                <div className="absolute inset-0 bg-muted/30 rounded-2xl blur-2xl transform scale-105"></div>
                
                {/* Main Dashboard Card */}
                <div className="relative bg-card border border-border rounded-2xl p-6 shadow-xl">
                  <div className="space-y-4">
                    <div className="flex items-center justify-between">
                      <h3 className="font-semibold text-sm">Dashboard</h3>
                      <div className="text-xs text-muted-foreground">Total em bem-estar: 8.283</div>
                    </div>
                    
                    {/* Time Filters */}
                    <div className="flex gap-2">
                      <Button variant="default" size="sm" className="text-xs">Hoje</Button>
                      <Button variant="ghost" size="sm" className="text-xs">Ontem</Button>
                      <Button variant="ghost" size="sm" className="text-xs">Essa semana</Button>
                      <Button variant="ghost" size="sm" className="text-xs">Esse mês</Button>
                    </div>

                    {/* Chart Area */}
                    <div className="h-48 bg-muted/30 rounded-lg flex items-end justify-center gap-1 p-4">
                      {[40, 60, 45, 70, 55, 80, 65, 90, 75, 85, 70, 95].map((height, i) => (
                        <div
                          key={i}
                          className="flex-1 bg-primary rounded-t"
                          style={{ height: `${height}%` }}
                        />
                      ))}
                    </div>

                    {/* Metrics */}
                    <div className="grid grid-cols-2 gap-4 pt-2">
                      <div>
                        <p className="text-xs text-muted-foreground mb-1">Colaboradores ativos</p>
                        <div className="flex items-center gap-2">
                          <span className="text-lg font-semibold">652</span>
                          <span className="text-xs text-primary bg-primary/10 px-2 py-0.5 rounded">+12</span>
                        </div>
                      </div>
                      <div>
                        <p className="text-xs text-muted-foreground mb-1">Risco detectado</p>
                        <div className="flex items-center gap-2">
                          <span className="text-lg font-semibold">23</span>
                          <span className="text-xs text-destructive bg-destructive/10 px-2 py-0.5 rounded">-4</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
