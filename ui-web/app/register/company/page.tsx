'use client';

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import Link from "next/link";
import { useState, useTransition } from "react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Field, FieldDescription, FieldError, FieldGroup, FieldLabel } from "@/components/ui/field";
import { apiService, RegisterCompanyRequest } from "@/lib/api";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { z } from "zod";
import { Building2, ArrowLeft, ArrowRight, Loader2, CheckCircle2 } from "lucide-react";
import { Stepper } from "@/components/ui/stepper";
import { PasswordStrength } from "@/components/ui/password-strength";
import { motion, AnimatePresence } from "framer-motion";
import { cn } from "@/lib/utils";

const registerCompanySchema = z.object({
  companyName: z
    .string()
    .min(2, 'O nome da empresa deve ter pelo menos 2 caracteres')
    .max(255, 'O nome da empresa deve ter no máximo 255 caracteres')
    .regex(/^[a-zA-ZÀ-ÿ0-9\s&.,-]+$/, 'Nome da empresa contém caracteres inválidos'),
  companyDomain: z
    .string()
    .optional()
    .refine((val) => !val || val.length === 0 || /^[a-zA-Z0-9][a-zA-Z0-9-]*[a-zA-Z0-9]*\.[a-zA-Z]{2,}$/.test(val), {
      message: 'Domínio inválido (ex: exemplo.com)',
    }),
  ownerName: z
    .string()
    .min(2, 'O nome deve ter pelo menos 2 caracteres')
    .max(100, 'O nome deve ter no máximo 100 caracteres')
    .regex(/^[a-zA-ZÀ-ÿ\s'-]+$/, 'O nome deve conter apenas letras, espaços, hífens e apóstrofos'),
  ownerEmail: z
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

type RegisterCompanyFormData = z.infer<typeof registerCompanySchema>;

const steps = ['Empresa', 'Proprietário', 'Segurança'];

export default function RegisterCompanyPage() {
  const router = useRouter();
  const [currentStep, setCurrentStep] = useState(1);
  const [formData, setFormData] = useState<RegisterCompanyFormData>({
    companyName: '',
    companyDomain: '',
    ownerName: '',
    ownerEmail: '',
    password: '',
    confirmPassword: '',
  });
  const [errors, setErrors] = useState<Partial<Record<keyof RegisterCompanyFormData, string>>>({});
  const [isPending, startTransition] = useTransition();
  const [touchedFields, setTouchedFields] = useState<Set<keyof RegisterCompanyFormData>>(new Set());

  const validateStep = (step: number): boolean => {
    const stepErrors: Partial<Record<keyof RegisterCompanyFormData, string>> = {};
    
    if (step === 1) {
      const companyResult = registerCompanySchema.shape.companyName.safeParse(formData.companyName);
      if (!companyResult.success) {
        const firstError = companyResult.error.issues?.[0];
        stepErrors.companyName = firstError?.message || 'Nome da empresa inválido';
      }
      if (formData.companyDomain) {
        const domainResult = registerCompanySchema.shape.companyDomain.safeParse(formData.companyDomain);
        if (!domainResult.success) {
          const firstError = domainResult.error.issues?.[0];
          stepErrors.companyDomain = firstError?.message || 'Domínio inválido';
        }
      }
    } else if (step === 2) {
      const ownerResult = registerCompanySchema.shape.ownerName.safeParse(formData.ownerName);
      const emailResult = registerCompanySchema.shape.ownerEmail.safeParse(formData.ownerEmail);
      if (!ownerResult.success) {
        const firstError = ownerResult.error.issues?.[0];
        stepErrors.ownerName = firstError?.message || 'Nome do proprietário inválido';
      }
      if (!emailResult.success) {
        const firstError = emailResult.error.issues?.[0];
        stepErrors.ownerEmail = firstError?.message || 'Email inválido';
      }
    } else if (step === 3) {
      const passwordResult = registerCompanySchema.shape.password.safeParse(formData.password);
      if (!passwordResult.success) {
        const firstError = passwordResult.error.issues?.[0];
        stepErrors.password = firstError?.message || 'Senha inválida';
      }
      if (formData.password !== formData.confirmPassword) {
        stepErrors.confirmPassword = 'As senhas não coincidem';
      }
    }

    setErrors(prev => ({ ...prev, ...stepErrors }));
    return Object.keys(stepErrors).length === 0;
  };

  const handleNext = () => {
    if (validateStep(currentStep)) {
      setCurrentStep(prev => Math.min(prev + 1, 3));
    }
  };

  const handleBack = () => {
    setCurrentStep(prev => Math.max(prev - 1, 1));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setErrors({});

    const result = registerCompanySchema.safeParse(formData);

    if (!result.success) {
      const fieldErrors: Partial<Record<keyof RegisterCompanyFormData, string>> = {};
      result.error.issues.forEach((issue) => {
        const field = issue.path[0] as keyof RegisterCompanyFormData;
        if (field) {
          fieldErrors[field] = issue.message;
        }
      });
      setErrors(fieldErrors);
      if (Object.keys(fieldErrors).some(f => ['companyName', 'companyDomain'].includes(f))) {
        setCurrentStep(1);
      } else if (Object.keys(fieldErrors).some(f => ['ownerName', 'ownerEmail'].includes(f))) {
        setCurrentStep(2);
      } else {
        setCurrentStep(3);
      }
      return;
    }

    startTransition(() => {
      (async () => {
        try {
          const request: RegisterCompanyRequest = {
            companyName: formData.companyName,
            companyDomain: formData.companyDomain || undefined,
            ownerName: formData.ownerName,
            ownerEmail: formData.ownerEmail,
            password: formData.password,
          };

          await apiService.registerCompany(request);
          toast.success('Empresa cadastrada com sucesso!', {
            description: 'Redirecionando para login...',
            icon: <CheckCircle2 className="w-5 h-5" />,
          });
          
          setTimeout(() => {
            router.push('/login/company');
          }, 1500);
        } catch (err) {
          const errorMessage = err instanceof Error ? err.message : 'Erro ao cadastrar empresa. Tente novamente.';
          toast.error('Erro ao cadastrar empresa', {
            description: errorMessage,
          });
          setErrors({ ownerEmail: errorMessage });
        }
      })();
    });
  };

  const updateField = <K extends keyof RegisterCompanyFormData>(
    field: K,
    value: RegisterCompanyFormData[K]
  ) => {
    setFormData(prev => ({ ...prev, [field]: value }));
    setTouchedFields(prev => new Set(prev).add(field));
    
    if (errors[field]) {
      const fieldSchema = registerCompanySchema.shape[field];
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

  const isFieldValid = (field: keyof RegisterCompanyFormData): boolean => {
    return !errors[field] && touchedFields.has(field) && formData[field] !== '';
  };

  const slideVariants = {
    enter: (direction: number) => ({
      x: direction > 0 ? 300 : -300,
      opacity: 0,
    }),
    center: {
      x: 0,
      opacity: 1,
    },
    exit: (direction: number) => ({
      x: direction < 0 ? 300 : -300,
      opacity: 0,
    }),
  };

  return (
    <div className="flex min-h-svh w-full flex-col items-center justify-center p-6 md:p-10 bg-gradient-to-b from-background to-muted/20">
      <div className="w-full max-w-2xl space-y-6">
        {/* Header */}
        <motion.div
          className="text-center space-y-2"
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5 }}
        >
          <div className="flex items-center justify-center gap-2 mb-2">
            <Building2 className="w-8 h-8 text-primary" />
            <h1 className="text-4xl font-bold bg-gradient-to-r from-primary to-purple-500 bg-clip-text text-transparent">
              tideflow
            </h1>
          </div>
          <p className="text-muted-foreground">Cadastre sua empresa em poucos passos</p>
        </motion.div>

        {/* Stepper */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.2 }}
        >
          <Card>
            <CardContent className="pt-6">
              <Stepper steps={steps} currentStep={currentStep} />
            </CardContent>
          </Card>
        </motion.div>

        {/* Form Card */}
        <Card className="shadow-lg">
          <CardHeader>
            <CardTitle className="text-2xl">
              {currentStep === 1 && 'Informações da Empresa'}
              {currentStep === 2 && 'Dados do Proprietário'}
              {currentStep === 3 && 'Criar Senha de Acesso'}
            </CardTitle>
            <CardDescription>
              {currentStep === 1 && 'Preencha os dados da sua empresa'}
              {currentStep === 2 && 'Informe os dados do responsável'}
              {currentStep === 3 && 'Defina uma senha segura para sua conta'}
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit}>
              <AnimatePresence mode="wait" custom={currentStep}>
                {/* Step 1: Company Info */}
                {currentStep === 1 && (
                  <motion.div
                    key="step1"
                    custom={1}
                    variants={slideVariants}
                    initial="enter"
                    animate="center"
                    exit="exit"
                    transition={{ duration: 0.3 }}
                  >
                    <FieldGroup className="space-y-4">
                      <Field>
                        <FieldLabel htmlFor="companyName">
                          Nome da Empresa <span className="text-destructive">*</span>
                        </FieldLabel>
                        <div className="relative">
                          <Input
                            id="companyName"
                            type="text"
                            placeholder="Tech Solutions Inc"
                            value={formData.companyName}
                            onChange={(e) => updateField('companyName', e.target.value)}
                            required
                            disabled={isPending}
                            aria-invalid={!!errors.companyName}
                            className={cn(
                              errors.companyName && "border-destructive",
                              isFieldValid('companyName') && "border-green-500"
                            )}
                          />
                          {isFieldValid('companyName') && (
                            <CheckCircle2 className="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 text-green-500" />
                          )}
                        </div>
                        <FieldError>{errors.companyName}</FieldError>
                      </Field>
                      <Field>
                        <FieldLabel htmlFor="companyDomain">Domínio da Empresa</FieldLabel>
                        <div className="relative">
                          <Input
                            id="companyDomain"
                            type="text"
                            placeholder="exemplo.com"
                            value={formData.companyDomain}
                            onChange={(e) => updateField('companyDomain', e.target.value)}
                            disabled={isPending}
                            aria-invalid={!!errors.companyDomain}
                            className={cn(
                              errors.companyDomain && "border-destructive",
                              isFieldValid('companyDomain') && formData.companyDomain && "border-green-500"
                            )}
                          />
                          {isFieldValid('companyDomain') && formData.companyDomain && (
                            <CheckCircle2 className="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 text-green-500" />
                          )}
                        </div>
                        <FieldError>{errors.companyDomain}</FieldError>
                        <FieldDescription>
                          Opcional. Usado para validação de emails de funcionários
                        </FieldDescription>
                      </Field>
                    </FieldGroup>
                  </motion.div>
                )}

                {/* Step 2: Owner Info */}
                {currentStep === 2 && (
                  <motion.div
                    key="step2"
                    custom={2}
                    variants={slideVariants}
                    initial="enter"
                    animate="center"
                    exit="exit"
                    transition={{ duration: 0.3 }}
                  >
                    <FieldGroup className="space-y-4">
                      <Field>
                        <FieldLabel htmlFor="ownerName">
                          Nome do Proprietário <span className="text-destructive">*</span>
                        </FieldLabel>
                        <div className="relative">
                          <Input
                            id="ownerName"
                            type="text"
                            placeholder="João Silva"
                            value={formData.ownerName}
                            onChange={(e) => updateField('ownerName', e.target.value)}
                            required
                            disabled={isPending}
                            aria-invalid={!!errors.ownerName}
                            className={cn(
                              errors.ownerName && "border-destructive",
                              isFieldValid('ownerName') && "border-green-500"
                            )}
                          />
                          {isFieldValid('ownerName') && (
                            <CheckCircle2 className="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 text-green-500" />
                          )}
                        </div>
                        <FieldError>{errors.ownerName}</FieldError>
                      </Field>
                      <Field>
                        <FieldLabel htmlFor="ownerEmail">
                          Email do Proprietário <span className="text-destructive">*</span>
                        </FieldLabel>
                        <div className="relative">
                          <Input
                            id="ownerEmail"
                            type="email"
                            placeholder="joao@exemplo.com"
                            value={formData.ownerEmail}
                            onChange={(e) => updateField('ownerEmail', e.target.value)}
                            required
                            disabled={isPending}
                            aria-invalid={!!errors.ownerEmail}
                            className={cn(
                              errors.ownerEmail && "border-destructive",
                              isFieldValid('ownerEmail') && "border-green-500"
                            )}
                          />
                          {isFieldValid('ownerEmail') && (
                            <CheckCircle2 className="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 text-green-500" />
                          )}
                        </div>
                        <FieldError>{errors.ownerEmail}</FieldError>
                        <FieldDescription>
                          Este será seu email de login e de cobrança
                        </FieldDescription>
                      </Field>
                    </FieldGroup>
                  </motion.div>
                )}

                {/* Step 3: Password */}
                {currentStep === 3 && (
                  <motion.div
                    key="step3"
                    custom={3}
                    variants={slideVariants}
                    initial="enter"
                    animate="center"
                    exit="exit"
                    transition={{ duration: 0.3 }}
                  >
                    <FieldGroup className="space-y-4">
                      <Field>
                        <FieldLabel htmlFor="password">
                          Senha <span className="text-destructive">*</span>
                        </FieldLabel>
                        <Input
                          id="password"
                          type="password"
                          value={formData.password}
                          onChange={(e) => updateField('password', e.target.value)}
                          required
                          disabled={isPending}
                          minLength={8}
                          aria-invalid={!!errors.password}
                          className={cn(
                            errors.password && "border-destructive",
                            isFieldValid('password') && formData.password.length >= 8 && "border-green-500"
                          )}
                        />
                        <FieldError>{errors.password}</FieldError>
                        {formData.password && (
                          <PasswordStrength password={formData.password} className="mt-3" />
                        )}
                      </Field>
                      <Field>
                        <FieldLabel htmlFor="confirmPassword">
                          Confirmar Senha <span className="text-destructive">*</span>
                        </FieldLabel>
                        <div className="relative">
                          <Input
                            id="confirmPassword"
                            type="password"
                            value={formData.confirmPassword}
                            onChange={(e) => updateField('confirmPassword', e.target.value)}
                            required
                            disabled={isPending}
                            aria-invalid={!!errors.confirmPassword}
                            className={cn(
                              errors.confirmPassword && "border-destructive",
                              isFieldValid('confirmPassword') && formData.password === formData.confirmPassword && formData.confirmPassword && "border-green-500"
                            )}
                          />
                          {isFieldValid('confirmPassword') && formData.password === formData.confirmPassword && formData.confirmPassword && (
                            <CheckCircle2 className="absolute right-3 top-1/2 -translate-y-1/2 w-5 h-5 text-green-500" />
                          )}
                        </div>
                        <FieldError>{errors.confirmPassword}</FieldError>
                        {formData.confirmPassword && formData.password === formData.confirmPassword && (
                          <FieldDescription className="text-green-600 flex items-center gap-1 mt-1">
                            <CheckCircle2 className="w-4 h-4" />
                            As senhas coincidem
                          </FieldDescription>
                        )}
                      </Field>
                    </FieldGroup>
                  </motion.div>
                )}
              </AnimatePresence>

              {/* Navigation Buttons */}
              <div className="flex items-center justify-between mt-6 pt-6 border-t">
                <Button
                  type="button"
                  variant="ghost"
                  onClick={handleBack}
                  disabled={currentStep === 1 || isPending}
                  className="flex items-center gap-2"
                >
                  <ArrowLeft className="w-4 h-4" />
                  Voltar
                </Button>
                {currentStep < 3 ? (
                  <Button
                    type="button"
                    onClick={handleNext}
                    disabled={isPending}
                    className="ml-auto"
                  >
                    Próximo
                    <ArrowRight className="w-4 h-4 ml-2" />
                  </Button>
                ) : (
                  <Button
                    type="submit"
                    disabled={isPending}
                    className="ml-auto"
                  >
                    {isPending ? (
                      <>
                        <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                        Cadastrando...
                      </>
                    ) : (
                      <>
                        Cadastrar Empresa
                        <CheckCircle2 className="w-4 h-4 ml-2" />
                      </>
                    )}
                  </Button>
                )}
              </div>
            </form>
          </CardContent>
        </Card>

        {/* Footer */}
        <div className="text-center">
          <p className="text-sm text-muted-foreground">
            Já tem uma conta?{' '}
            <Link href="/login/company" className="text-primary hover:underline font-medium transition-colors">
              Faça login
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}


