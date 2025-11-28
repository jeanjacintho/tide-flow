'use client';

import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { cn } from '@/lib/utils';

export interface CurrentPlanCardProps {
  planName: string;
  price: string;
  priceUnit?: string;
  description: string;
  status: string;
  billingCycle: string;
  nextBillingDate?: string;
  totalUsers: number;
  pricePerUser: string;
  monthlyBill: string;
  onManage?: () => void;
  className?: string;
  isFreePlan?: boolean;
}

export function CurrentPlanCard({
  planName,
  price,
  priceUnit = '/mo.',
  description,
  status,
  billingCycle,
  nextBillingDate,
  totalUsers,
  pricePerUser,
  monthlyBill,
  onManage,
  className,
  isFreePlan = false,
}: CurrentPlanCardProps) {
  return (
    <Card className={className}>
      <CardHeader>
        <div className="flex items-center gap-2">
          <CardTitle>{planName}</CardTitle>
          <div className="inline-flex items-center rounded-md border border-border/50 px-1.5 py-0.5 text-xs font-medium transition-colors bg-muted/30">
            <span className="text-xs">Atual</span>
          </div>
          <span className="flex items-end gap-0.5 ml-1 text-muted-foreground">
            <span className="text-base font-medium">{price}</span>
            {priceUnit && (
              <span className="text-xs mb-[3px]">{priceUnit}</span>
            )}
          </span>
        </div>
        <CardDescription>{description}</CardDescription>
      </CardHeader>
      <CardContent>
        <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
          <div className="flex flex-col justify-between gap-4">
            {onManage && (
              <Button
                variant="outline"
                className="w-fit border-border/50 hover:bg-muted/50 rounded-md px-4 py-2 h-auto"
                onClick={onManage}
              >
                <span className="text-sm font-medium">Gerenciar Assinatura</span>
              </Button>
            )}
          </div>
          <div className="flex flex-col gap-4">
            <div className={`grid gap-4 ${isFreePlan ? 'grid-cols-2' : 'grid-cols-2'}`}>
              <div>
                <div className="text-xs text-muted-foreground mb-1">
                  Total de Usuários
                </div>
                <div className="text-base font-medium">{totalUsers}</div>
              </div>
              <div>
                <div className="text-xs text-muted-foreground mb-1">
                  Preço por Usuário
                </div>
                <div className="text-base font-medium">{pricePerUser}</div>
              </div>
              {!isFreePlan && (
                <>
                  <div>
                    <div className="text-xs text-muted-foreground mb-1">
                      Ciclo de Cobrança
                    </div>
                    <div className="text-base font-medium">{billingCycle}</div>
                  </div>
                  <div>
                    <div className="text-xs text-muted-foreground mb-1">
                      Próxima Cobrança
                    </div>
                    <div className="text-base font-medium">
                      {nextBillingDate || 'Não disponível'}
                    </div>
                  </div>
                </>
              )}
            </div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}
