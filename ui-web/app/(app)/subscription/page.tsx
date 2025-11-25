'use client';

import { useState, useEffect } from 'react';
import { useRequireRole } from '@/hooks/useRequireRole';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { Check, X, AlertCircle, TrendingUp } from 'lucide-react';
import { cn } from '@/lib/utils';

// TODO: Obter companyId do contexto do usuário ou token
const MOCK_COMPANY_ID = '00000000-0000-0000-0000-000000000000';
const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

interface Subscription {
  id: string;
  companyId: string;
  planType: 'FREE' | 'ENTERPRISE';
  pricePerUser: number;
  totalUsers: number;
  billingCycle: 'MONTHLY' | 'YEARLY';
  nextBillingDate: string;
  status: string;
  monthlyBill: number;
}

interface UsageInfo {
  activeUsers: number;
  maxUsers: number;
  atLimit: boolean;
  remainingSlots: number;
}

export default function SubscriptionPage() {
  const { hasAccess, isChecking } = useRequireRole({ 
    companyRole: ['OWNER', 'ADMIN'],
    redirectTo: '/chat'
  });
  
  if (isChecking || !hasAccess) {
    return null;
  }
  
  const [subscription, setSubscription] = useState<Subscription | null>(null);
  const [usageInfo, setUsageInfo] = useState<UsageInfo | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [upgrading, setUpgrading] = useState(false);

  useEffect(() => {
    fetchSubscriptionData();
  }, []);

  const fetchSubscriptionData = async () => {
    try {
      setLoading(true);
      setError(null);
      
      const token = localStorage.getItem('auth_token');
      const headers: Record<string, string> = {
        'Content-Type': 'application/json',
      };
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }

      const [subscriptionRes, usageRes] = await Promise.all([
        fetch(`${API_BASE_URL}/api/subscriptions/companies/${MOCK_COMPANY_ID}`, { headers }),
        fetch(`${API_BASE_URL}/api/subscriptions/companies/${MOCK_COMPANY_ID}/usage`, { headers }),
      ]);

      if (subscriptionRes.ok) {
        const subData = await subscriptionRes.json();
        setSubscription(subData);
      }

      if (usageRes.ok) {
        const usageData = await usageRes.json();
        setUsageInfo(usageData);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao carregar dados da assinatura');
    } finally {
      setLoading(false);
    }
  };

  const handleUpgrade = async () => {
    try {
      setUpgrading(true);
      const token = localStorage.getItem('auth_token');
      
      const response = await fetch(
        `${API_BASE_URL}/api/subscriptions/companies/${MOCK_COMPANY_ID}/upgrade`,
        {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
            ...(token && { 'Authorization': `Bearer ${token}` }),
          },
          body: JSON.stringify({ planType: 'ENTERPRISE' }),
        }
      );

      if (response.ok) {
        await fetchSubscriptionData();
      } else {
        const errorText = await response.text();
        setError(errorText || 'Erro ao fazer upgrade');
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao fazer upgrade');
    } finally {
      setUpgrading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex-1 overflow-y-auto p-6 space-y-6">
        <Skeleton className="h-10 w-64" />
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  return (
    <div className="flex-1 overflow-y-auto p-6 space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Assinatura</h1>
        <p className="text-muted-foreground mt-1">
          Gerencie seu plano e visualize informações de uso
        </p>
      </div>

      {error && (
        <Card className="border-red-200 dark:border-red-800 bg-red-50 dark:bg-red-950">
          <CardContent className="p-4">
            <div className="flex items-center gap-2 text-red-600 dark:text-red-400">
              <AlertCircle className="w-5 h-5" />
              <span>{error}</span>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Current Subscription */}
      {subscription && (
        <Card>
          <CardHeader>
            <CardTitle>Plano Atual</CardTitle>
            <CardDescription>
              Informações sobre sua assinatura atual
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <div className="text-2xl font-bold">
                  {subscription.planType === 'FREE' ? 'Plano Gratuito' : 'Plano Enterprise'}
                </div>
                <div className="text-sm text-muted-foreground mt-1">
                  Status: {subscription.status}
                </div>
              </div>
              <div className="text-right">
                <div className="text-2xl font-bold">
                  €{subscription.monthlyBill.toFixed(2)}
                </div>
                <div className="text-sm text-muted-foreground">
                  por mês
                </div>
              </div>
            </div>

            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 pt-4 border-t">
              <div>
                <div className="text-sm text-muted-foreground">Usuários Ativos</div>
                <div className="text-lg font-semibold">{subscription.totalUsers}</div>
              </div>
              <div>
                <div className="text-sm text-muted-foreground">Preço por Usuário</div>
                <div className="text-lg font-semibold">
                  €{subscription.pricePerUser.toFixed(2)}
                </div>
              </div>
              <div>
                <div className="text-sm text-muted-foreground">Ciclo de Cobrança</div>
                <div className="text-lg font-semibold">
                  {subscription.billingCycle === 'MONTHLY' ? 'Mensal' : 'Anual'}
                </div>
              </div>
              <div>
                <div className="text-sm text-muted-foreground">Próxima Cobrança</div>
                <div className="text-lg font-semibold">
                  {new Date(subscription.nextBillingDate).toLocaleDateString('pt-BR')}
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Usage Info */}
      {usageInfo && (
        <Card>
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
                <span className="text-lg font-semibold">
                  {usageInfo.activeUsers} / {usageInfo.maxUsers}
                </span>
              </div>
              
              <div className="w-full bg-muted rounded-full h-2">
                <div
                  className={cn(
                    'h-2 rounded-full transition-all',
                    usageInfo.atLimit
                      ? 'bg-red-500'
                      : usageInfo.activeUsers / usageInfo.maxUsers > 0.8
                      ? 'bg-yellow-500'
                      : 'bg-green-500'
                  )}
                  style={{
                    width: `${Math.min(100, (usageInfo.activeUsers / usageInfo.maxUsers) * 100)}%`,
                  }}
                />
              </div>

              {usageInfo.atLimit && (
                <div className="flex items-center gap-2 p-3 bg-yellow-50 dark:bg-yellow-950 border border-yellow-200 dark:border-yellow-800 rounded-lg">
                  <AlertCircle className="w-5 h-5 text-yellow-600 dark:text-yellow-400" />
                  <div className="text-sm text-yellow-800 dark:text-yellow-200">
                    Limite de usuários atingido. Faça upgrade para adicionar mais usuários.
                  </div>
                </div>
              )}

              {!usageInfo.atLimit && (
                <div className="text-sm text-muted-foreground">
                  {usageInfo.remainingSlots} vagas disponíveis
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Plan Comparison */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <Card className={cn(
          subscription?.planType === 'FREE' && 'border-primary'
        )}>
          <CardHeader>
            <CardTitle>Plano Gratuito</CardTitle>
            <CardDescription>Ideal para pequenas equipes</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="text-3xl font-bold">Grátis</div>
            <ul className="space-y-2">
              <li className="flex items-center gap-2">
                <Check className="w-5 h-5 text-green-600" />
                <span>Até 7 usuários</span>
              </li>
              <li className="flex items-center gap-2">
                <Check className="w-5 h-5 text-green-600" />
                <span>Dashboard básico</span>
              </li>
              <li className="flex items-center gap-2">
                <Check className="w-5 h-5 text-green-600" />
                <span>Análise emocional</span>
              </li>
              <li className="flex items-center gap-2">
                <X className="w-5 h-5 text-gray-400" />
                <span className="text-muted-foreground">Integração Slack/Teams</span>
              </li>
              <li className="flex items-center gap-2">
                <X className="w-5 h-5 text-gray-400" />
                <span className="text-muted-foreground">Suporte prioritário</span>
              </li>
            </ul>
            {subscription?.planType === 'FREE' && (
              <div className="pt-4 border-t">
                <div className="text-sm font-medium text-primary">Plano Atual</div>
              </div>
            )}
          </CardContent>
        </Card>

        <Card className={cn(
          subscription?.planType === 'ENTERPRISE' && 'border-primary'
        )}>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <TrendingUp className="w-5 h-5" />
              Plano Enterprise
            </CardTitle>
            <CardDescription>Para empresas que precisam de mais</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div>
              <div className="text-3xl font-bold">R$ 49.90</div>
              <div className="text-sm text-muted-foreground">por usuário/mês</div>
            </div>
            <ul className="space-y-2">
              <li className="flex items-center gap-2">
                <Check className="w-5 h-5 text-green-600" />
                <span>Usuários ilimitados</span>
              </li>
              <li className="flex items-center gap-2">
                <Check className="w-5 h-5 text-green-600" />
                <span>Dashboard completo</span>
              </li>
              <li className="flex items-center gap-2">
                <Check className="w-5 h-5 text-green-600" />
                <span>Análise emocional avançada</span>
              </li>
              <li className="flex items-center gap-2">
                <Check className="w-5 h-5 text-green-600" />
                <span>Integração Slack/Teams</span>
              </li>
              <li className="flex items-center gap-2">
                <Check className="w-5 h-5 text-green-600" />
                <span>Suporte prioritário</span>
              </li>
            </ul>
            {subscription?.planType === 'ENTERPRISE' ? (
              <div className="pt-4 border-t">
                <div className="text-sm font-medium text-primary">Plano Atual</div>
              </div>
            ) : (
              <Button
                onClick={handleUpgrade}
                disabled={upgrading}
                className="w-full mt-4"
              >
                {upgrading ? 'Processando...' : 'Upgrade para Enterprise'}
              </Button>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
