'use client';

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import Link from "next/link";
import { useAuth } from "@/contexts/AuthContext";
import { useState, useTransition, useMemo } from "react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field";
import { cn } from "@/lib/utils";
import { z } from "zod";

type PasswordStrength = 'weak' | 'fair' | 'good' | 'strong';

interface PasswordStrengthResult {
    strength: PasswordStrength;
    score: number;
    feedback: string[];
}

function calculatePasswordStrength(password: string): PasswordStrengthResult {
    if (!password) {
        return { strength: 'weak', score: 0, feedback: [] };
    }

    let score = 0;
    const feedback: string[] = [];

    if (password.length >= 8) score += 1;
    else feedback.push('Pelo menos 8 caracteres');

    if (password.length >= 12) score += 1;

    if (/[a-z]/.test(password)) score += 1;
    else feedback.push('Adicione letras minúsculas');

    if (/[A-Z]/.test(password)) score += 1;
    else feedback.push('Adicione letras maiúsculas');

    if (/[0-9]/.test(password)) score += 1;
    else feedback.push('Adicione números');

    if (/[^a-zA-Z0-9]/.test(password)) score += 1;
    else feedback.push('Adicione caracteres especiais');

    if (password.length >= 16) score += 1;

    let strength: PasswordStrength;
    if (score <= 2) {
        strength = 'weak';
    } else if (score <= 3) {
        strength = 'fair';
    } else if (score <= 5) {
        strength = 'good';
    } else {
        strength = 'strong';
    }

    return { strength, score, feedback };
}

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
}).refine((data) => {
    const strength = calculatePasswordStrength(data.password);
    return strength.strength !== 'weak';
}, {
    message: 'A senha é muito fraca. Por favor, escolha uma senha mais forte',
    path: ['password'],
});

type RegisterFormData = z.infer<typeof registerSchema>;

export default function Register(props: React.ComponentProps<typeof Card>) {
    const [name, setName] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [errors, setErrors] = useState<Partial<Record<keyof RegisterFormData, string>>>({});
    const [isPending, startTransition] = useTransition();
    const { register } = useAuth();

    const passwordStrength = useMemo(() => calculatePasswordStrength(password), [password]);

    const cardProps = Object.fromEntries(
        Object.entries(props).filter(([key]) => key !== 'params' && key !== 'searchParams')
    ) as React.ComponentProps<typeof Card>;

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

    const getStrengthColor = (strength: PasswordStrength) => {
        switch (strength) {
            case 'weak':
                return 'bg-red-500';
            case 'fair':
                return 'bg-orange-500';
            case 'good':
                return 'bg-yellow-500';
            case 'strong':
                return 'bg-green-500';
            default:
                return 'bg-gray-500';
        }
    };

    const getStrengthLabel = (strength: PasswordStrength) => {
        switch (strength) {
            case 'weak':
                return 'Fraca';
            case 'fair':
                return 'Razoável';
            case 'good':
                return 'Boa';
            case 'strong':
                return 'Forte';
            default:
                return '';
        }
    };

    const progressValue = (passwordStrength.score / 7) * 100;

    return (
        <div className="flex min-h-svh w-full flex-col items-center justify-center p-6 md:p-10">
      <div className="w-full max-w-sm space-y-6">
        <div className="text-center">
          <h1 className="text-4xl font-bold">tideflow</h1>
        </div>
        <Card {...cardProps}>
      <CardHeader>
        <CardTitle>Criar uma conta</CardTitle>
        <CardDescription>
          Digite suas informações abaixo para criar sua conta
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit}>
          <FieldGroup>
            <Field>
              <FieldLabel htmlFor="name">Nome completo</FieldLabel>
              <Input 
                id="name" 
                type="text" 
                placeholder="João Silva" 
                value={name}
                onChange={(e) => {
                  setName(e.target.value);
                  if (errors.name) {
                    setErrors(prev => ({ ...prev, name: undefined }));
                  }
                }}
                required 
                disabled={isPending}
                aria-invalid={!!errors.name}
              />
              <FieldError>{errors.name}</FieldError>
            </Field>
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
              {!errors.email && (
                <FieldDescription>
                  Usaremos isso para entrar em contato. Não compartilharemos seu email
                  com ninguém.
                </FieldDescription>
              )}
            </Field>
            <Field>
              <FieldLabel htmlFor="password">Senha</FieldLabel>
              <Input 
                id="password" 
                type="password" 
                value={password}
                onChange={(e) => {
                  setPassword(e.target.value);
                  if (errors.password) {
                    setErrors(prev => ({ ...prev, password: undefined }));
                  }
                }}
                required 
                disabled={isPending}
                minLength={8}
                aria-invalid={!!errors.password}
              />
              <FieldError>{errors.password}</FieldError>
              {password && !errors.password && (
                <div className="space-y-2">
                  <div className="flex items-center justify-between gap-2">
                    <div className="flex-1">
                      <div className="h-2 w-full bg-secondary rounded-full overflow-hidden">
                        <div 
                          className={cn(
                            "h-full transition-all duration-300",
                            getStrengthColor(passwordStrength.strength)
                          )}
                          style={{ width: `${progressValue}%` }}
                        />
                      </div>
                    </div>
                    <span className={cn(
                      "text-xs font-medium min-w-[60px] text-right",
                      passwordStrength.strength === 'weak' && 'text-red-600',
                      passwordStrength.strength === 'fair' && 'text-orange-600',
                      passwordStrength.strength === 'good' && 'text-yellow-600',
                      passwordStrength.strength === 'strong' && 'text-green-600'
                    )}>
                      {getStrengthLabel(passwordStrength.strength)}
                    </span>
                  </div>
                  {passwordStrength.feedback.length > 0 && (
                    <div className="text-xs text-muted-foreground">
                      <span className="font-medium">Sugestões:</span>
                      <ul className="list-disc list-inside mt-1 space-y-0.5">
                        {passwordStrength.feedback.map((item, index) => (
                          <li key={index}>{item}</li>
                        ))}
                      </ul>
                    </div>
                  )}
                </div>
              )}
              {!password && !errors.password && (
                <FieldDescription>
                  Deve ter pelo menos 8 caracteres.
                </FieldDescription>
              )}
            </Field>
            <Field>
              <FieldLabel htmlFor="confirm-password">
                Confirmar Senha
              </FieldLabel>
              <Input 
                id="confirm-password" 
                type="password" 
                value={confirmPassword}
                onChange={(e) => {
                  setConfirmPassword(e.target.value);
                  if (errors.confirmPassword) {
                    setErrors(prev => ({ ...prev, confirmPassword: undefined }));
                  }
                }}
                required 
                disabled={isPending}
                aria-invalid={!!errors.confirmPassword}
              />
              <FieldError>{errors.confirmPassword}</FieldError>
              {confirmPassword && password === confirmPassword && password && !errors.confirmPassword && (
                <FieldDescription className="text-green-600 text-xs">
                  As senhas coincidem.
                </FieldDescription>
              )}
              {!confirmPassword && !errors.confirmPassword && (
                <FieldDescription>Por favor, confirme sua senha.</FieldDescription>
              )}
            </Field>
            <FieldGroup>
              <Field>
                <Button type="submit" disabled={isPending} className="w-full">
                  {isPending ? 'Criando conta...' : 'Criar Conta'}
                </Button>
                <FieldDescription className="px-6 text-center">
                  Já tem uma conta? <Link href="/login" className="text-primary hover:underline">Faça login</Link>
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