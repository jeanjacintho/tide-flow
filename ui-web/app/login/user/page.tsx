'use client';

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import Link from "next/link";
import { useAuth } from "@/contexts/AuthContext";
import { useState, useTransition } from "react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field";
import { z } from "zod";
import { Building2, User } from "lucide-react";

const loginSchema = z.object({
    username: z
        .string()
        .min(1, 'O email ou username é obrigatório'),
    password: z
        .string()
        .min(1, 'A senha é obrigatória'),
});

type LoginFormData = z.infer<typeof loginSchema>;

export default function UserLoginPage() {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [errors, setErrors] = useState<Partial<Record<keyof LoginFormData, string>>>({});
    const [isPending, startTransition] = useTransition();
    const { login } = useAuth();

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        setErrors({});

        const formData: LoginFormData = {
            username,
            password,
        };

        const result = loginSchema.safeParse(formData);

        if (!result.success) {
            const fieldErrors: Partial<Record<keyof LoginFormData, string>> = {};
            result.error.issues.forEach((issue) => {
                const field = issue.path[0] as keyof LoginFormData;
                if (field) {
                    fieldErrors[field] = issue.message;
                }
            });
            setErrors(fieldErrors);
            return;
        }

        startTransition(() => {
            (async () => {
                try {
                    await login(username, password);
                } catch (err) {
                    const errorMessage = err instanceof Error ? err.message : 'Erro ao fazer login. Verifique suas credenciais.';
                    setErrors({ username: errorMessage });
                }
            })();
        });
    };

    return (
        <div className="flex min-h-svh w-full flex-col items-center justify-center p-6 md:p-10 bg-gradient-to-br from-background to-muted/20">
            <div className="w-full max-w-sm space-y-6">
                <div className="text-center space-y-2">
                    <div className="flex items-center justify-center gap-2 mb-4">
                        <User className="h-8 w-8 text-primary" />
                        <h1 className="text-4xl font-bold bg-gradient-to-r from-black via-purple-500 to-blue-500 bg-clip-text text-transparent">
                            tideflow
                        </h1>
                    </div>
                    <p className="text-sm text-muted-foreground">
                        Acesso para funcionários
                    </p>
                </div>
                <Card>
                    <CardHeader>
                        <CardTitle>Login como Usuário</CardTitle>
                        <CardDescription>
                            Digite seu email ou username e senha para acessar sua conta
                        </CardDescription>
                    </CardHeader>
                    <CardContent>
                        <form onSubmit={handleSubmit}>
                            <FieldGroup>
                                <Field>
                                    <FieldLabel htmlFor="username">Email ou Username</FieldLabel>
                                    <Input
                                        id="username"
                                        type="text"
                                        placeholder="joao@exemplo.com ou joao.silva"
                                        value={username}
                                        onChange={(e) => {
                                            setUsername(e.target.value);
                                            if (errors.username) {
                                                setErrors(prev => ({ ...prev, username: undefined }));
                                            }
                                        }}
                                        required
                                        disabled={isPending}
                                        aria-invalid={!!errors.username}
                                    />
                                    <FieldError>{errors.username}</FieldError>
                                </Field>
                                <Field>
                                    <div className="flex justify-between items-center">
                                        <FieldLabel htmlFor="password">Senha</FieldLabel>
                                        <Link href="/forgot-password" className="text-sm text-primary hover:underline">
                                            Esqueceu sua senha?
                                        </Link>
                                    </div>
                                    <Input
                                        id="password"
                                        type="password"
                                        placeholder="Senha"
                                        value={password}
                                        onChange={(e) => {
                                            setPassword(e.target.value);
                                            if (errors.password) {
                                                setErrors(prev => ({ ...prev, password: undefined }));
                                            }
                                        }}
                                        required
                                        disabled={isPending}
                                        aria-invalid={!!errors.password}
                                    />
                                    <FieldError>{errors.password}</FieldError>
                                </Field>
                                <FieldGroup>
                                    <Field>
                                        <Button type="submit" disabled={isPending} className="w-full">
                                            {isPending ? 'Entrando...' : 'Entrar'}
                                        </Button>
                                        <FieldDescription className="px-6 text-center">
                                            Não tem uma conta? <Link href="/register" className="text-primary hover:underline">Registre-se</Link>
                                        </FieldDescription>
                                    </Field>
                                </FieldGroup>
                            </FieldGroup>
                        </form>
                        <div className="mt-4 pt-4 border-t">
                            <Link 
                                href="/login/company" 
                                className="flex items-center justify-center gap-2 text-sm text-muted-foreground hover:text-foreground transition-colors"
                            >
                                <Building2 className="h-4 w-4" />
                                Sou administrador da empresa
                            </Link>
                        </div>
                    </CardContent>
                </Card>
            </div>
        </div>
    )
}
