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
import { User, ArrowLeft, Loader2, CheckCircle2, Eye, EyeOff, AlertCircle } from "lucide-react";
import { PasswordStrength } from "@/components/ui/password-strength";
import { motion } from "framer-motion";
import { DotPattern } from "@/components/ui/dot-pattern";

const registerSchema = z.object({
    name: z
        .string()
        .min(2, 'O nome deve ter pelo menos 2 caracteres')
        .max(100, 'O nome deve ter no máximo 100 caracteres')
        .regex(/^[a-zA-ZÀ-ÿ\s'-]+$/, 'O nome deve conter apenas letras, espaços, hífens e apóstrofos'),
    email: z
        .string()
        .min(1, 'O email é obrigatório')
        .email('Email inválido'),
    password: z
        .string()
        .min(8, 'A senha deve ter pelo menos 8 caracteres')
        .regex(/[a-z]/, 'A senha deve conter pelo menos uma letra minúscula')
        .regex(/[A-Z]/, 'A senha deve conter pelo menos uma letra maiúscula')
        .regex(/[0-9]/, 'A senha deve conter pelo menos um número')
        .regex(/[^a-zA-Z0-9]/, 'A senha deve conter pelo menos um caractere especial'),
    confirmPassword: z.string(),
}).refine((data) => data.password === data.confirmPassword, {
    message: 'As senhas não coincidem',
    path: ['confirmPassword'],
});

type RegisterFormData = z.infer<typeof registerSchema>;

export default function RegisterUserPage() {
    const [name, setName] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);
    const [errors, setErrors] = useState<Partial<Record<keyof RegisterFormData, string>>>({});
    const [isPending, startTransition] = useTransition();
    const [touchedFields, setTouchedFields] = useState<Set<keyof RegisterFormData>>(new Set());
    const { register } = useAuth();

    const updateField = <K extends keyof RegisterFormData>(
        field: K,
        value: RegisterFormData[K]
    ) => {
        if (field === 'name') setName(value as string);
        if (field === 'email') setEmail(value as string);
        if (field === 'password') setPassword(value as string);
        if (field === 'confirmPassword') setConfirmPassword(value as string);
        
        setTouchedFields(prev => new Set(prev).add(field));
        
        if (errors[field]) {
            const fieldSchema = registerSchema.shape[field];
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

    const isFieldValid = (field: keyof RegisterFormData): boolean => {
        return !errors[field] && touchedFields.has(field) && 
               (field === 'name' ? name !== '' :
                field === 'email' ? email !== '' :
                field === 'password' ? password !== '' :
                confirmPassword !== '');
    };

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        setErrors({});

        const formData: RegisterFormData = {
            name,
            email,
            password,
            confirmPassword,
        };

        const result = registerSchema.safeParse(formData);

        if (!result.success) {
            const fieldErrors: Partial<Record<keyof RegisterFormData, string>> = {};
            result.error.issues.forEach((issue) => {
                const field = issue.path[0] as keyof RegisterFormData;
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
                    await register(name, email, password);
                } catch (err) {
                    const errorMessage = err instanceof Error ? err.message : 'Erro ao criar conta. Tente novamente.';
                    setErrors({ email: errorMessage });
                }
            })();
        });
    };

    return (
        <div className="relative flex min-h-svh w-full flex-col items-center justify-center p-6 md:p-10 bg-background-secondary overflow-hidden">
            {/* Dot Pattern Background */}
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
            
            {/* Content */}
            <motion.div
                className="relative z-10 w-full max-w-md space-y-6"
                style={{ willChange: 'transform' }}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5 }}
            >
                {/* Header */}
                <motion.div
                    className="text-center"
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    transition={{ delay: 0.1 }}
                >
                    <h1 className="text-3xl font-bold">
                        tideflow
                    </h1>
                    <p className="text-muted-foreground mt-1">Crie sua conta pessoal</p>
                </motion.div>

                {/* Form Card */}
                <motion.div
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    transition={{ delay: 0.2 }}
                >
                    <Card>
                        <CardHeader className="gap-0 pb-0">
                            <CardTitle>Criar uma conta</CardTitle>
                            <CardDescription>
                                Digite suas informações abaixo para criar sua conta
                            </CardDescription>
                        </CardHeader>
                        <CardContent className="pt-6">
                            <form onSubmit={handleSubmit}>
                                <FieldGroup className="space-y-2">
                                    <Field>
                                        <FieldLabel htmlFor="name">
                                            Nome completo <span className="text-destructive">*</span>
                                        </FieldLabel>
                                        <div className="relative">
                                            <Input
                                                id="name"
                                                type="text"
                                                placeholder="João Silva"
                                                value={name}
                                                onChange={(e) => updateField('name', e.target.value)}
                                                required
                                                disabled={isPending}
                                                aria-invalid={!!errors.name}
                                                className={cn(
                                                    errors.name && "border-destructive",
                                                    isFieldValid('name') && "border-green-500"
                                                )}
                                            />
                                            {isFieldValid('name') && (
                                                <CheckCircle2 className="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 text-green-500" />
                                            )}
                                        </div>
                                        <FieldError>{errors.name}</FieldError>
                                    </Field>
                                    <Field>
                                        <FieldLabel htmlFor="email">
                                            Email <span className="text-destructive">*</span>
                                        </FieldLabel>
                                        <div className="relative">
                                            <Input
                                                id="email"
                                                type="email"
                                                placeholder="joao@exemplo.com"
                                                value={email}
                                                onChange={(e) => updateField('email', e.target.value)}
                                                required
                                                disabled={isPending}
                                                aria-invalid={!!errors.email}
                                                className={cn(
                                                    errors.email && "border-destructive",
                                                    isFieldValid('email') && "border-green-500"
                                                )}
                                            />
                                            {isFieldValid('email') && (
                                                <CheckCircle2 className="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 text-green-500" />
                                            )}
                                        </div>
                                        <FieldError>{errors.email}</FieldError>
                                        {!errors.email && (
                                            <FieldDescription>
                                                Usaremos isso para entrar em contato. Não compartilharemos seu email
                                                com ninguém.
                                            </FieldDescription>
                                        )}
                                    </Field>
                                    <Field>
                                        <FieldLabel htmlFor="password">
                                            Senha <span className="text-destructive">*</span>
                                        </FieldLabel>
                                        <div className="relative">
                                            <Input
                                                id="password"
                                                type={showPassword ? "text" : "password"}
                                                value={password}
                                                onChange={(e) => updateField('password', e.target.value)}
                                                required
                                                disabled={isPending}
                                                minLength={8}
                                                aria-invalid={!!errors.password}
                                                className={cn(
                                                    errors.password && "border-destructive",
                                                    isFieldValid('password') && password.length >= 8 && "border-green-500",
                                                    "pr-10"
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
                                        {password && (
                                            <PasswordStrength password={password} className="mt-3" />
                                        )}
                                    </Field>
                                    <Field>
                                        <FieldLabel htmlFor="confirm-password">
                                            Confirmar Senha <span className="text-destructive">*</span>
                                        </FieldLabel>
                                        <div className="relative">
                                            <Input
                                                id="confirm-password"
                                                type={showConfirmPassword ? "text" : "password"}
                                                value={confirmPassword}
                                                onChange={(e) => updateField('confirmPassword', e.target.value)}
                                                required
                                                disabled={isPending}
                                                aria-invalid={!!errors.confirmPassword}
                                                className={cn(
                                                    errors.confirmPassword && "border-destructive",
                                                    isFieldValid('confirmPassword') && password === confirmPassword && confirmPassword && "border-green-500",
                                                    "pr-10"
                                                )}
                                            />
                                            <button
                                                type="button"
                                                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                                                className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
                                                tabIndex={-1}
                                            >
                                                {showConfirmPassword ? (
                                                    <EyeOff className="w-5 h-5" />
                                                ) : (
                                                    <Eye className="w-5 h-5" />
                                                )}
                                            </button>
                                            {isFieldValid('confirmPassword') && password === confirmPassword && confirmPassword && (
                                                <CheckCircle2 className="absolute right-10 top-1/2 -translate-y-1/2 w-5 h-5 text-green-500" />
                                            )}
                                        </div>
                                        <FieldError>{errors.confirmPassword}</FieldError>
                                        {confirmPassword && password === confirmPassword && password && !errors.confirmPassword && (
                                            <FieldDescription className="text-green-600 flex items-center gap-1 mt-1">
                                                <CheckCircle2 className="w-4 h-4" />
                                                As senhas coincidem
                                            </FieldDescription>
                                        )}
                                    </Field>
                                    <FieldGroup>
                                        <Field>
                                            <Button type="submit" disabled={isPending} className="w-full" size="lg">
                                                {isPending ? (
                                                    <>
                                                        <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                                                    </>
                                                ) : (
                                                    <>
                                                        Criar Conta
                                                    </>
                                                )}
                                            </Button>
                                            <FieldDescription className="px-6 text-center mt-4">
                                                Já tem uma conta?{' '}
                                                <Link href="/login/user" className="text-primary hover:underline font-medium transition-colors">
                                                    Faça login
                                                </Link>
                                            </FieldDescription>
                                        </Field>
                                    </FieldGroup>
                                </FieldGroup>
                            </form>
                        </CardContent>
                    </Card>
                </motion.div>
            </motion.div>
        </div>
    );
}


