'use client';

import { AlertCircle } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { cn } from '@/lib/utils';

export interface UsageCardProps {
  activeUsers: number;
  maxUsers: number;
  atLimit: boolean;
  remainingSlots: number;
  className?: string;
}

export function UsageCard({
  activeUsers,
  maxUsers,
  atLimit,
  remainingSlots,
  className,
}: UsageCardProps) {
  const isUnlimited = maxUsers === 2147483647 || maxUsers === Number.MAX_SAFE_INTEGER;
  const usagePercentage = isUnlimited ? 0 : Math.min(100, (activeUsers / maxUsers) * 100);

  return (
    <Card className={className}>
      <CardHeader>
        <CardTitle>Uso Atual</CardTitle>
        <CardDescription>
          Informações sobre o uso da sua empresa
        </CardDescription>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <span className="text-sm text-muted-foreground">Usuários Ativos</span>
            <span className="text-base font-medium">
              {activeUsers} / {isUnlimited ? 'Ilimitado' : maxUsers}
            </span>
          </div>

          {!isUnlimited && (
            <div className="w-full bg-muted rounded-full h-1">
              <div
                className={cn(
                  'h-1 rounded-full transition-all',
                  atLimit
                    ? 'bg-red-500'
                    : usagePercentage > 80
                    ? 'bg-yellow-500'
                    : 'bg-green-500'
                )}
                style={{
                  width: `${usagePercentage}%`,
                }}
              />
            </div>
          )}

          {isUnlimited && (
            <div className="w-full bg-muted rounded-full h-1">
              <div className="h-1 rounded-full bg-green-500 w-full" />
            </div>
          )}

          {atLimit && !isUnlimited && (
            <div className="flex items-center gap-2 p-3 bg-yellow-50 dark:bg-yellow-950/30 border border-yellow-200 dark:border-yellow-800 rounded-lg">
              <AlertCircle className="w-5 h-5 text-yellow-600 dark:text-yellow-400 flex-shrink-0" />
              <div className="text-sm text-yellow-800 dark:text-yellow-200">
                Limite de usuários atingido. Faça upgrade para adicionar mais usuários.
              </div>
            </div>
          )}

          {!atLimit && !isUnlimited && (
            <div className="text-sm text-muted-foreground">
              {remainingSlots} vagas disponíveis
            </div>
          )}

          {isUnlimited && (
            <div className="text-sm text-muted-foreground">
              Usuários ilimitados disponíveis
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
