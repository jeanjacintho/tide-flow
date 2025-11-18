'use client';

import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import Link from "next/link";
import { Label } from "@radix-ui/react-label";
import { useAuth } from "@/contexts/AuthContext";
import { useState } from "react";
import { useRouter } from "next/navigation";

export default function Login() {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const { login } = useAuth();
    const router = useRouter();

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');
        setIsLoading(true);

        try {
            await login(email, password);
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Erro ao fazer login. Verifique suas credenciais.');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="flex flex-col items-center justify-center h-screen">
            <div className="flex flex-col w-[400px] bg-white rounded-lg p-4 border border-border gap-4">
                <div className="flex flex-col gap-2">
                    <h1 className="text-2xl font-bold">Faça login para continuar</h1>
                    <span className="text-sm text-muted-foreground">Digite seu email e senha para continuar</span>
                </div>
                <form onSubmit={handleSubmit} className="flex flex-col gap-4">
                    {error && (
                        <div className="p-3 text-sm text-red-600 bg-red-50 border border-red-200 rounded-md">
                            {error}
                        </div>
                    )}
                    <div className="flex flex-col gap-2">
                        <Label htmlFor="email">Email</Label>
                        <Input 
                            type="email" 
                            id="email" 
                            placeholder="Email" 
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                            disabled={isLoading}
                        />
                    </div>
                    <div className="flex flex-col gap-2">
                        <div className="flex justify-between">
                            <Label htmlFor="password">Senha</Label>
                            <Link href="/forgot-password" className="text-sm text-primary hover:underline">
                                Esqueceu sua senha?
                            </Link>
                        </div>
                        <Input 
                            type="password" 
                            id="password" 
                            placeholder="Senha" 
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                            disabled={isLoading}
                        />
                    </div>
                    <Button className="w-full" size={"lg"} type="submit" disabled={isLoading}>
                        {isLoading ? 'Entrando...' : 'Entrar'}
                    </Button>
                </form>
                <span className="text-sm text-muted-foreground text-center">
                    Não tem uma conta? <Link href="/register" className="text-primary hover:underline">Registre-se</Link>
                </span>
            </div>
        </div>
    )
}