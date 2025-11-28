'use client';

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import Link from "next/link";
import { useAuth } from "@/contexts/AuthContext";
import { useState, useTransition } from "react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field";
import { cn } from "@/lib/utils";
import { z } from "zod";
import { Building2, User, Loader2, Eye, EyeOff, CheckCircle2, AlertCircle } from "lucide-react";
import { motion } from "framer-motion";
import { DotPattern } from "@/components/ui/dot-pattern";

const loginSchema = z.object({
    username: z
        .string()
        .min(1, 'O email ou username é obrigatório'),
    password: z
        .string()
        .min(1, 'A senha é obrigatória'),
});

type LoginFormData = z.infer<typeof loginSchema>;

export default function CompanyLoginPage() {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [errors, setErrors] = useState<Partial<Record<keyof LoginFormData, string>>>({});
    const [isPending, startTransition] = useTransition();
    const [touchedFields, setTouchedFields] = useState<Set<keyof LoginFormData>>(new Set());
    const { login } = useAuth();

    const updateField = <K extends keyof LoginFormData>(
        field: K,
        value: LoginFormData[K]
    ) => {
        if (field === 'username') setUsername(value as string);
        if (field === 'password') setPassword(value as string);

        setTouchedFields(prev => new Set(prev).add(field));

        if (errors[field]) {
            const fieldSchema = loginSchema.shape[field];
            if (fieldSchema) {
                const result = fieldSchema.safeParse(value);
                if (result.success) {
                    setErrors(prev => {
                        const newErrors = { ...prev };
                        delete newErrors[field];
                        return newErrors;
                    });
                }
            }
        }
    };

    const isFieldValid = (field: keyof LoginFormData): boolean => {
        return !errors[field] && touchedFields.has(field) &&
               (field === 'username' ? username !== '' : password !== '');
    };

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
        <div className="relative flex min-h-svh w-full flex-col items-center justify-center p-6 md:p-10 bg-background-secondary overflow-hidden">
            {}
            <div className="absolute inset-0 opacity-30 pointer-events-none">
                <DotPattern
                    className="h-full w-full [mask-image:radial-gradient(ellipse_at_center,white,transparent)]"
                    width={10}
                    height={10}
                    cx={1}
                    cy={1}
                    cr={1}
                />
            </div>
            {}
            <motion.div
                className="relative z-10 w-full max-w-md space-y-6"
                style={{ willChange: 'transform' }}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5 }}
            >
                {}
                <motion.div
                    className="text-center"
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    transition={{ delay: 0.1 }}
                >
                    <h1 className="text-3xl font-bold">
                        tideflow
                    </h1>
                    <p className="text-muted-foreground mt-1">Acesso para administradores</p>
                </motion.div>

                {}
                <motion.div
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    transition={{ delay: 0.2 }}
                >
                    <Card>
                        <CardHeader className="gap-0 pb-0">
                            <CardTitle>Login como Empresa</CardTitle>
                            <CardDescription>
                                Digite seu email ou username e senha para acessar o painel administrativo
                            </CardDescription>
                        </CardHeader>
                        <CardContent className="pt-6">
                            <form onSubmit={handleSubmit}>
                                <FieldGroup className="space-y-2">
                                    <Field>
                                        <FieldLabel htmlFor="username">
                                            Email ou Username <span className="text-destructive">*</span>
                                        </FieldLabel>
                                        <div className="relative">
                                            <Input
                                                id="username"
                                                type="text"
                                                placeholder="admin@empresa.com ou admin.empresa"
                                                value={username}
                                                onChange={(e) => updateField('username', e.target.value)}
                                                required
                                                disabled={isPending}
                                                aria-invalid={!!errors.username}
                                                className={cn(
                                                    errors.username && "border-destructive pr-10",
                                                    isFieldValid('username') && "border-green-500 pr-10",
                                                    !errors.username && !isFieldValid('username') && "pr-3"
                                                )}
                                            />
                                            {errors.username && (
                                                <AlertCircle className="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 text-destructive" />
                                            )}
                                            {isFieldValid('username') && (
                                                <CheckCircle2 className="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 text-green-500" />
                                            )}
                                        </div>
                                        <FieldError>{errors.username}</FieldError>
                                    </Field>
                                    <Field>
                                        <div className="flex justify-between items-center">
                                            <FieldLabel htmlFor="password">
                                                Senha <span className="text-destructive">*</span>
                                            </FieldLabel>
                                            <Link
                                                href="/forgot-password"
                                                className="text-sm text-primary hover:underline font-medium transition-colors"
                                            >
                                                Esqueceu sua senha?
                                            </Link>
                                        </div>
                                        <div className="relative">
                                            <Input
                                                id="password"
                                                type={showPassword ? "text" : "password"}
                                                placeholder="Senha"
                                                value={password}
                                                onChange={(e) => updateField('password', e.target.value)}
                                                required
                                                disabled={isPending}
                                                aria-invalid={!!errors.password}
                                                className={cn(
                                                    errors.password && "border-destructive pr-10",
                                                    isFieldValid('password') && "border-green-500 pr-10",
                                                    !errors.password && !isFieldValid('password') && "pr-10"
                                                )}
                                            />
                                            <button
                                                type="button"
                                                onClick={() => setShowPassword(!showPassword)}
                                                className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
                                                tabIndex={-1}
                                            >
                                                {showPassword ? (
                                                    <EyeOff className="w-5 h-5" />
                                                ) : (
                                                    <Eye className="w-5 h-5" />
                                                )}
                                            </button>
                                        </div>
                                        <FieldError>{errors.password}</FieldError>
                                    </Field>
                                    <FieldGroup>
                                        <Field>
                                            <Button
                                                type="submit"
                                                disabled={isPending}
                                                className="w-full"
                                                size="lg"
                                            >
                                                {isPending ? (
                                                    <>
                                                        <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                                                    </>
                                                ) : (
                                                    <>
                                                        Entrar
                                                    </>
                                                )}
                                            </Button>
                                            <FieldDescription className="px-6 text-center mt-4">
                                                Precisa de uma conta empresa?{' '}
                                                <Link href="/register/company" className="text-primary hover:underline font-medium transition-colors">
                                                    Cadastre sua empresa
                                                </Link>
                                            </FieldDescription>
                                        </Field>
                                    </FieldGroup>
                                </FieldGroup>
                            </form>
                            <div className="mt-6 pt-6 border-t">
                                <Link
                                    href="/login/user"
                                    className="flex items-center justify-center gap-2 text-sm text-muted-foreground hover:text-foreground transition-colors group"
                                >
                                    <User className="h-4 w-4 group-hover:text-primary transition-colors" />
                                    Sou funcionário
                                </Link>
                            </div>
                        </CardContent>
                    </Card>
                </motion.div>
            </motion.div>
        </div>
    );
}
