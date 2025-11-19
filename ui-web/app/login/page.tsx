'use client';

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import Link from "next/link";
import { useAuth } from "@/contexts/AuthContext";
import { useState, useTransition } from "react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field";
import { z } from "zod";

const loginSchema = z.object({
    email: z
        .string()
        .min(1, 'O email é obrigatório')
        .email('Email inválido'),
    password: z
        .string()
        .min(1, 'A senha é obrigatória'),
});

type LoginFormData = z.infer<typeof loginSchema>;

export default function Login(props: React.ComponentProps<typeof Card>) {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [errors, setErrors] = useState<Partial<Record<keyof LoginFormData, string>>>({});
    const [isPending, startTransition] = useTransition();
    const { login } = useAuth();

    const cardProps = Object.fromEntries(
        Object.entries(props).filter(([key]) => key !== 'params' && key !== 'searchParams')
    ) as React.ComponentProps<typeof Card>;

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        setErrors({});

        const formData: LoginFormData = {
            email,
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
                    await login(email, password);
                } catch (err) {
                    const errorMessage = err instanceof Error ? err.message : 'Erro ao fazer login. Verifique suas credenciais.';
                    setErrors({ email: errorMessage });
                }
            })();
        });
    };

    return (
        <div className="flex min-h-svh w-full flex-col items-center justify-center p-6 md:p-10">
            <div className="w-full max-w-sm space-y-6">
                <div className="text-center">
                    <h1 className="text-4xl font-bold">tideflow</h1>
                </div>
                <Card {...cardProps}>
                    <CardHeader>
                        <CardTitle>Faça login para continuar</CardTitle>
                        <CardDescription>
                            Digite seu email e senha para continuar
                        </CardDescription>
                    </CardHeader>
                    <CardContent>
                        <form onSubmit={handleSubmit}>
                            <FieldGroup>
                                <Field>
                                    <FieldLabel htmlFor="email">Email</FieldLabel>
                                    <Input
                                        id="email"
                                        type="email"
                                        placeholder="joao@exemplo.com"
                                        value={email}
                                        onChange={(e) => {
                                            setEmail(e.target.value);
                                            if (errors.email) {
                                                setErrors(prev => ({ ...prev, email: undefined }));
                                            }
                                        }}
                                        required
                                        disabled={isPending}
                                        aria-invalid={!!errors.email}
                                    />
                                    <FieldError>{errors.email}</FieldError>
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
                    </CardContent>
                </Card>
            </div>
        </div>
    )
}