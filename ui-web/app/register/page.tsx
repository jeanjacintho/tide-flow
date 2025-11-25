'use client';

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import Link from "next/link";
import { Building2, User, ArrowRight, Sparkles, Shield, BarChart3, Users, MessageSquare, Heart, TrendingUp } from "lucide-react";
import { motion } from "framer-motion";

const containerVariants = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: {
      staggerChildren: 0.1,
    },
  },
};

const itemVariants = {
  hidden: { opacity: 0, y: 20 },
  visible: {
    opacity: 1,
    y: 0,
    transition: {
      duration: 0.5,
      ease: "easeOut",
    },
  },
};

const featureVariants = {
  hidden: { opacity: 0, x: -10 },
  visible: {
    opacity: 1,
    x: 0,
    transition: {
      duration: 0.3,
    },
  },
};

export default function RegisterPage() {
  return (
    <div className="flex min-h-svh w-full flex-col items-center justify-center p-6 md:p-10 bg-gradient-to-b from-background to-muted/20">
      <motion.div
        className="w-full max-w-5xl space-y-8"
        variants={containerVariants}
        initial="hidden"
        animate="visible"
      >
        {/* Header */}
        <motion.div className="text-center space-y-2" variants={itemVariants}>
          <h1 className="text-5xl font-bold bg-gradient-to-r from-primary via-purple-500 to-blue-500 bg-clip-text text-transparent">
            tideflow
          </h1>
          <p className="text-lg text-muted-foreground">
            Escolha como deseja começar sua jornada
          </p>
        </motion.div>

        {/* Cards Grid */}
        <div className="grid md:grid-cols-2 gap-6">
          {/* Card de Registro de Empresa */}
          <motion.div variants={itemVariants}>
            <Card className="h-full hover:shadow-lg transition-all duration-300 border-2 hover:border-primary/50 group">
              <CardHeader className="pb-4">
                <div className="flex items-center gap-3 mb-2">
                  <div className="p-3 rounded-xl bg-primary/10 group-hover:bg-primary/20 transition-colors">
                    <Building2 className="w-6 h-6 text-primary" />
                  </div>
                  <div>
                    <CardTitle className="text-xl">Cadastrar Empresa</CardTitle>
                    <CardDescription className="mt-1">
                      Solução completa para gestão corporativa
                    </CardDescription>
                  </div>
                </div>
              </CardHeader>
              <CardContent className="space-y-4">
                <ul className="space-y-3 text-sm">
                  <motion.li
                    className="flex items-start gap-3 text-muted-foreground"
                    variants={featureVariants}
                  >
                    <BarChart3 className="w-4 h-4 mt-0.5 text-primary shrink-0" />
                    <span>Dashboard corporativo com métricas agregadas</span>
                  </motion.li>
                  <motion.li
                    className="flex items-start gap-3 text-muted-foreground"
                    variants={featureVariants}
                  >
                    <TrendingUp className="w-4 h-4 mt-0.5 text-primary shrink-0" />
                    <span>Análise de padrões emocionais por departamento</span>
                  </motion.li>
                  <motion.li
                    className="flex items-start gap-3 text-muted-foreground"
                    variants={featureVariants}
                  >
                    <Shield className="w-4 h-4 mt-0.5 text-primary shrink-0" />
                    <span>Predição de turnover e alertas de risco</span>
                  </motion.li>
                  <motion.li
                    className="flex items-start gap-3 text-muted-foreground"
                    variants={featureVariants}
                  >
                    <Users className="w-4 h-4 mt-0.5 text-primary shrink-0" />
                    <span>Gerenciamento de funcionários e convites</span>
                  </motion.li>
                </ul>
                <Button asChild className="w-full group-hover:scale-[1.02] transition-transform" size="lg">
                  <Link href="/register/company" className="flex items-center justify-center gap-2">
                    Cadastrar Empresa
                    <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
                  </Link>
                </Button>
              </CardContent>
            </Card>
          </motion.div>

          {/* Card de Registro de Usuário */}
          <motion.div variants={itemVariants}>
            <Card className="h-full hover:shadow-lg transition-all duration-300 border-2 hover:border-primary/50 group">
              <CardHeader className="pb-4">
                <div className="flex items-center gap-3 mb-2">
                  <div className="p-3 rounded-xl bg-primary/10 group-hover:bg-primary/20 transition-colors">
                    <User className="w-6 h-6 text-primary" />
                  </div>
                  <div>
                    <CardTitle className="text-xl">Cadastrar como Usuário</CardTitle>
                    <CardDescription className="mt-1">
                      Experiência pessoal de bem-estar
                    </CardDescription>
                  </div>
                </div>
              </CardHeader>
              <CardContent className="space-y-4">
                <ul className="space-y-3 text-sm">
                  <motion.li
                    className="flex items-start gap-3 text-muted-foreground"
                    variants={featureVariants}
                  >
                    <MessageSquare className="w-4 h-4 mt-0.5 text-primary shrink-0" />
                    <span>Chat conversacional com IA</span>
                  </motion.li>
                  <motion.li
                    className="flex items-start gap-3 text-muted-foreground"
                    variants={featureVariants}
                  >
                    <Sparkles className="w-4 h-4 mt-0.5 text-primary shrink-0" />
                    <span>Análise emocional personalizada</span>
                  </motion.li>
                  <motion.li
                    className="flex items-start gap-3 text-muted-foreground"
                    variants={featureVariants}
                  >
                    <Heart className="w-4 h-4 mt-0.5 text-primary shrink-0" />
                    <span>Acompanhamento do seu bem-estar</span>
                  </motion.li>
                  <motion.li
                    className="flex items-start gap-3 text-muted-foreground"
                    variants={featureVariants}
                  >
                    <BarChart3 className="w-4 h-4 mt-0.5 text-primary shrink-0" />
                    <span>Histórico de conversas e insights</span>
                  </motion.li>
                </ul>
                <Button asChild variant="outline" className="w-full group-hover:scale-[1.02] transition-transform" size="lg">
                  <Link href="/register/user" className="flex items-center justify-center gap-2">
                    Cadastrar como Usuário
                    <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
                  </Link>
                </Button>
              </CardContent>
            </Card>
          </motion.div>
        </div>

        {/* Footer Links */}
        <motion.div className="text-center" variants={itemVariants}>
          <p className="text-sm text-muted-foreground">
            Já tem uma conta?{' '}
            <Link href="/login/user" className="text-primary hover:underline font-medium transition-colors">
              Login como Usuário
            </Link>
            {' ou '}
            <Link href="/login/company" className="text-primary hover:underline font-medium transition-colors">
              Login como Empresa
            </Link>
          </p>
        </motion.div>
      </motion.div>
    </div>
  );
}
