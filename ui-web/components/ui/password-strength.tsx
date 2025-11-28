'use client';

import { cn } from "@/lib/utils";
import { CheckCircle2, XCircle } from "lucide-react";

type PasswordStrength = 'weak' | 'fair' | 'good' | 'strong';

interface PasswordStrengthProps {
  password: string;
  className?: string;
}

interface Requirement {
  test: (password: string) => boolean;
  label: string;
}

const requirements: Requirement[] = [
  { test: (p) => p.length >= 8, label: 'Pelo menos 8 caracteres' },
  { test: (p) => /[a-z]/.test(p), label: 'Uma letra minúscula' },
  { test: (p) => /[A-Z]/.test(p), label: 'Uma letra maiúscula' },
  { test: (p) => /[0-9]/.test(p), label: 'Um número' },
  { test: (p) => /[^a-zA-Z0-9]/.test(p), label: 'Um caractere especial' },
  { test: (p) => p.length >= 12, label: 'Pelo menos 12 caracteres (recomendado)' },
];

function calculateStrength(password: string): PasswordStrength {
  if (!password) return 'weak';

  const metRequirements = requirements.filter(req => req.test(password)).length;

  if (metRequirements <= 2) return 'weak';
  if (metRequirements <= 3) return 'fair';
  if (metRequirements <= 5) return 'good';
  return 'strong';
}

function getStrengthColor(strength: PasswordStrength): string {
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
}

function getStrengthLabel(strength: PasswordStrength): string {
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
}

export function PasswordStrength({ password, className }: PasswordStrengthProps) {
  if (!password) return null;

  const strength = calculateStrength(password);
  const metRequirements = requirements.filter(req => req.test(password));
  const progress = (metRequirements / requirements.length) * 100;

  return (
    <div className={cn("space-y-3", className)}>
      {}
      <div className="space-y-2">
        <div className="flex items-center justify-between">
          <span className="text-xs font-medium text-muted-foreground">Força da senha</span>
          <span className={cn(
            "text-xs font-semibold",
            strength === 'weak' && 'text-red-600',
            strength === 'fair' && 'text-orange-600',
            strength === 'good' && 'text-yellow-600',
            strength === 'strong' && 'text-green-600'
          )}>
            {getStrengthLabel(strength)}
          </span>
        </div>
        <div className="h-2 w-full bg-muted rounded-full overflow-hidden">
          <div
            className={cn(
              "h-full transition-all duration-500 ease-out",
              getStrengthColor(strength)
            )}
            style={{ width: `${progress}%` }}
          />
        </div>
      </div>

      {}
      <div className="space-y-1.5">
        {requirements.map((req, index) => {
          const isMet = req.test(password);
          return (
            <div
              key={index}
              className={cn(
                "flex items-center gap-2 text-xs transition-all duration-200",
                isMet ? "text-green-600" : "text-muted-foreground"
              )}
            >
              {isMet ? (
                <CheckCircle2 className="w-3.5 h-3.5 shrink-0" />
              ) : (
                <XCircle className="w-3.5 h-3.5 shrink-0 opacity-50" />
              )}
              <span>{req.label}</span>
            </div>
          );
        })}
      </div>
    </div>
  );
}
