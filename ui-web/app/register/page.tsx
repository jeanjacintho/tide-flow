'use client';

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@radix-ui/react-label";
import Link from "next/link";
import { useAuth } from "@/contexts/AuthContext";
import { useState } from "react";

export default function Register() {
    const [name, setName] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const { register } = useAuth();

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');
        setIsLoading(true);

        try {
            await register(name, email, password);
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Erro ao criar conta. Tente novamente.');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="flex flex-col items-center justify-center h-screen">
            <div className="flex flex-col w-[400px] bg-white rounded-lg p-4 border border-border gap-4">
                <div className="flex flex-col gap-2">
                    <h1 className="text-2xl font-bold">Crie sua conta</h1>
                    <span className="text-sm text-muted-foreground">Digite seu nome, email e senha para continuar</span>
                </div>
                <form onSubmit={handleSubmit} className="flex flex-col gap-4">
                    {error && (
                        <div className="p-3 text-sm text-red-600 bg-red-50 border border-red-200 rounded-md">
                            {error}
                        </div>
                    )}
                    <div className="flex flex-col gap-2">
                        <Label htmlFor="name">Nome</Label>
                        <Input 
                            type="text" 
                            id="name" 
                            placeholder="Nome" 
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                            required
                            disabled={isLoading}
                        />
                    </div>
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
                        <Label htmlFor="password">Senha</Label>
                        <Input 
                            type="password" 
                            id="password" 
                            placeholder="Senha" 
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                            disabled={isLoading}
                            minLength={6}
                        />
                    </div>
                    <Button className="w-full" size={"lg"} type="submit" disabled={isLoading}>
                        {isLoading ? 'Criando conta...' : 'Criar conta'}
                    </Button>
                </form>
                <span className="text-sm text-muted-foreground text-center">
                    Já tem uma conta? <Link href="/login" className="text-primary hover:underline">Faça login</Link>
                </span>
            </div>
        </div>
    )
}