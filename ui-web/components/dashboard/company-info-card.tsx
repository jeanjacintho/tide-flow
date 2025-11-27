'use client';

import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { Company, UsageInfo } from '@/lib/api';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';

interface CompanyInfoCardProps {
  company: Company | null;
  usageInfo?: UsageInfo | null;
  loading?: boolean;
}

export function CompanyInfoCard({ company, usageInfo, loading = false }: CompanyInfoCardProps) {
  if (loading) {
    return (
      <Card>
        <CardHeader>
          <Skeleton className="h-6 w-32" />
          <Skeleton className="h-4 w-48 mt-2" />
        </CardHeader>
        <CardContent className="space-y-4">
          <Skeleton className="h-20 w-full" />
          <Skeleton className="h-16 w-full" />
        </CardContent>
      </Card>
    );
  }

  if (!company) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Informações da Empresa</CardTitle>
          <CardDescription>Nenhuma informação disponível</CardDescription>
        </CardHeader>
      </Card>
    );
  }

  const getStatusBadgeVariant = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return 'default';
      case 'TRIAL':
        return 'secondary';
      case 'SUSPENDED':
        return 'destructive';
      case 'CANCELLED':
        return 'outline';
      default:
        return 'secondary';
    }
  };

  const getPlanBadgeVariant = (plan: string) => {
    switch (plan) {
      case 'ENTERPRISE':
        return 'default';
      case 'PROFESSIONAL':
        return 'secondary';
      case 'FREE':
        return 'outline';
      default:
        return 'secondary';
    }
  };

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle>{company.name}</CardTitle>
            <CardDescription className="mt-1">Informações da empresa e assinatura</CardDescription>
          </div>
          <div className="flex gap-2">
            <Badge variant={getStatusBadgeVariant(company.status)}>
              {company.status}
            </Badge>
            <Badge variant={getPlanBadgeVariant(company.subscriptionPlan)}>
              {company.subscriptionPlan}
            </Badge>
          </div>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        {/* Company Details */}
        <div className="space-y-3">
          {company.domain && (
            <div className="text-sm">
              <p className="text-muted-foreground">Domínio</p>
              <p className="font-medium">{company.domain}</p>
            </div>
          )}

          {company.billingEmail && (
            <div className="text-sm">
              <p className="text-muted-foreground">Email de Cobrança</p>
              <p className="font-medium">{company.billingEmail}</p>
            </div>
          )}

          {company.billingAddress && (
            <div className="text-sm">
              <p className="text-muted-foreground">Endereço de Cobrança</p>
              <p className="font-medium">{company.billingAddress}</p>
            </div>
          )}

          {company.taxId && (
            <div className="text-sm">
              <p className="text-muted-foreground">CNPJ/ID Fiscal</p>
              <p className="font-medium">{company.taxId}</p>
            </div>
          )}

          <div className="text-sm">
            <p className="text-muted-foreground">Criada em</p>
            <p className="font-medium">
              {company.createdAt
                ? format(new Date(company.createdAt), "dd 'de' MMMM 'de' yyyy", { locale: ptBR })
                : 'Não disponível'}
            </p>
          </div>
        </div>

        {/* Usage Information */}
        {usageInfo && (
          <div className="pt-4 border-t">
            <div className="mb-3">
              <p className="text-sm font-medium">Uso de Licenças</p>
            </div>
            <div className="space-y-2">
              <div className="flex items-center justify-between text-sm">
                <span className="text-muted-foreground">Usuários Ativos</span>
                <span className="font-medium">
                  {usageInfo.activeUsers} / {usageInfo.maxUsers}
                </span>
              </div>
              <div className="w-full bg-secondary rounded-full h-2">
                <div
                  className={`h-2 rounded-full ${
                    usageInfo.atLimit ? 'bg-destructive' : 'bg-primary'
                  }`}
                  style={{
                    width: `${Math.min((usageInfo.activeUsers / usageInfo.maxUsers) * 100, 100)}%`,
                  }}
                />
              </div>
              <div className="flex items-center justify-between text-xs text-muted-foreground">
                <span>
                  {usageInfo.remainingSlots > 0
                    ? `${usageInfo.remainingSlots} vagas disponíveis`
                    : 'Limite atingido'}
                </span>
                {usageInfo.atLimit && (
                  <Badge variant="destructive" className="text-xs">
                    Limite Atingido
                  </Badge>
                )}
              </div>
            </div>
          </div>
        )}

        {/* Actions */}
        <div className="pt-4 border-t flex gap-2">
          <Button variant="outline" size="sm" className="flex-1">
            Ver Detalhes
          </Button>
          <Button variant="outline" size="sm" className="flex-1">
            Gerenciar Assinatura
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}

