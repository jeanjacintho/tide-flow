'use client';

import { useState, useEffect } from 'react';
import { useRequireRole } from '@/hooks/useRequireRole';
import { useAuth } from '@/contexts/AuthContext';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { AlertCircle } from 'lucide-react';
import { apiService } from '@/lib/api';
import { PlanCard } from '@/components/subscription/plan-card';
import { CurrentPlanCard } from '@/components/subscription/current-plan-card';
import { UsageCard } from '@/components/subscription/usage-card';
import { PaymentHistoryTable } from '@/components/subscription/payment-history-table';


interface Subscription {
  id: string;
  companyId: string;
  planType: 'FREE' | 'ENTERPRISE';
  pricePerUser: number;
  totalUsers: number;
  billingCycle: string;
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
  const { user } = useAuth();
  
  if (isChecking || !hasAccess) {
    return null;
  }
  
  const [subscription, setSubscription] = useState<Subscription | null>(null);
  const [usageInfo, setUsageInfo] = useState<UsageInfo | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [upgrading, setUpgrading] = useState(false);

  useEffect(() => {
    if (user?.companyId) {
      fetchSubscriptionData();
    }
  }, [user?.companyId]);

  const fetchSubscriptionData = async () => {
    if (!user?.companyId) return;
    
    try {
      setLoading(true);
      setError(null);

      const [subData, usageData] = await Promise.all([
        apiService.getSubscription(user.companyId),
        apiService.getUsageInfo(user.companyId),
      ]);

      setSubscription(subData as Subscription);
      setUsageInfo(usageData);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao carregar dados da assinatura');
    } finally {
      setLoading(false);
    }
  };

  const handleUpgrade = async () => {
    if (!user?.companyId) return;
    
    try {
      setUpgrading(true);
      setError(null);
      
      const successUrl = `${window.location.origin}/subscription?success=true`;
      const cancelUrl = `${window.location.origin}/subscription?canceled=true`;
      
      const { checkoutUrl } = await apiService.createCheckoutSession(
        user.companyId,
        successUrl,
        cancelUrl
      );
      
      // Redireciona para o Stripe Checkout
      window.location.href = checkoutUrl;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao criar sessão de checkout');
      setUpgrading(false);
    }
  };
  
  // Verifica se há parâmetros de sucesso ou cancelamento na URL
  useEffect(() => {
    if (typeof window === 'undefined') return;
    
    const params = new URLSearchParams(window.location.search);
    if (params.get('success') === 'true') {
      setError(null);
      setLoading(true);
      
      // Remove o parâmetro da URL
      window.history.replaceState({}, '', window.location.pathname);
      
      // Faz polling para verificar se o plano foi atualizado
      if (user?.companyId) {
        let attempts = 0;
        const maxAttempts = 10; // Tenta por até 20 segundos (10 tentativas * 2 segundos)
        
        const checkSubscription = async () => {
          try {
            const subData = await apiService.getSubscription(user.companyId!);
            
            // Se o plano foi atualizado para ENTERPRISE, para o polling
            if (subData.planType === 'ENTERPRISE') {
              setSubscription(subData as Subscription);
              setLoading(false);
              return;
            }
            
            // Se ainda não atualizou e não excedeu tentativas, tenta novamente
            attempts++;
            if (attempts < maxAttempts) {
              setTimeout(checkSubscription, 2000);
            } else {
              // Após todas as tentativas, atualiza mesmo assim
              setSubscription(subData as Subscription);
              setLoading(false);
              setError('O pagamento foi processado, mas pode levar alguns minutos para o plano ser atualizado. Recarregue a página em alguns instantes.');
            }
          } catch (err) {
            attempts++;
            if (attempts < maxAttempts) {
              setTimeout(checkSubscription, 2000);
            } else {
              setLoading(false);
              setError('Erro ao verificar atualização do plano. Recarregue a página.');
            }
          }
        };
        
        // Inicia a verificação após 2 segundos
        setTimeout(checkSubscription, 2000);
      }
    } else if (params.get('canceled') === 'true') {
      setError('Checkout cancelado');
      window.history.replaceState({}, '', window.location.pathname);
    }
  }, [user?.companyId]);

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
        <CurrentPlanCard
          planName={
            subscription.planType === 'FREE' ? 'Plano Gratuito' : 'Plano Enterprise'
          }
          price={`R$ ${subscription.monthlyBill.toFixed(2).replace('.', ',')}`}
          priceUnit="/mês"
          description={`Status: ${subscription.status}`}
          status={subscription.status}
          billingCycle={
            subscription.billingCycle === 'MONTHLY' ? 'Mensal' : 'Anual'
          }
          nextBillingDate={
            subscription.planType === 'FREE' 
              ? undefined 
              : subscription.nextBillingDate
                ? new Date(subscription.nextBillingDate).toLocaleDateString('pt-BR', {
                    day: '2-digit',
                    month: 'short',
                    year: 'numeric',
                  })
                : undefined
          }
          totalUsers={subscription.totalUsers}
          pricePerUser={`R$ ${subscription.pricePerUser.toFixed(2).replace('.', ',')}`}
          monthlyBill={`R$ ${subscription.monthlyBill.toFixed(2).replace('.', ',')}`}
          isFreePlan={subscription.planType === 'FREE'}
        />
      )}

      {/* Usage Info */}
      {usageInfo && (
        <UsageCard
          activeUsers={usageInfo.activeUsers}
          maxUsers={usageInfo.maxUsers}
          atLimit={usageInfo.atLimit}
          remainingSlots={usageInfo.remainingSlots}
        />
      )}

      {/* Plan Comparison - Only show when plan is FREE */}
      {subscription?.planType === 'FREE' && (
        <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
          <PlanCard
            title="Gratuito"
            price="Gratuito"
            priceUnit=""
            description="Ideal para pequenas equipes"
            features={[
              { text: 'Até 7 usuários', included: true },
              { text: 'Dashboard básico', included: true },
              { text: 'Análise emocional', included: true },
              { text: 'Integração Slack/Teams', included: false },
              { text: 'Suporte prioritário', included: false },
            ]}
            isCurrent={true}
          />

          <PlanCard
            title="Enterprise"
            price="R$ 199,90"
            priceUnit="/usuário/mês"
            description="Para empresas que precisam de mais"
            features={[
              { text: 'Usuários ilimitados', included: true },
              { text: 'Dashboard completo', included: true },
              { text: 'Análise emocional avançada', included: true },
              { text: 'Integração Slack/Teams', included: true },
              { text: 'Suporte prioritário', included: true },
            ]}
            isCurrent={false}
            onUpgrade={handleUpgrade}
            upgradeLabel="Fazer upgrade para Enterprise"
            upgrading={upgrading}
          />
        </div>
      )}

      {/* Payment History */}
      {user?.companyId && (
        <PaymentHistoryTable companyId={user.companyId} />
      )}
    </div>
  );
}
