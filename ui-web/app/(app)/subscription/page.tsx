'use client';

import { useState, useEffect } from 'react';
import { useRequireRole } from '@/hooks/useRequireRole';
import { useAuth } from '@/contexts/AuthContext';
import { Card, CardContent } from '@/components/ui/card';
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
        <h1 className="text-3xl font-bold">Subscription</h1>
        <p className="text-muted-foreground mt-1">
          Manage your plan and view usage information
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
            subscription.planType === 'FREE' ? 'Free Plan' : 'Enterprise Plan'
          }
          price={`€${subscription.monthlyBill.toFixed(2)}`}
          priceUnit="/mo."
          description={`Status: ${subscription.status}`}
          status={subscription.status}
          billingCycle={
            subscription.billingCycle === 'MONTHLY' ? 'Monthly' : 'Yearly'
          }
          nextBillingDate={new Date(
            subscription.nextBillingDate
          ).toLocaleDateString('en-US', {
            month: 'short',
            day: 'numeric',
            year: 'numeric',
          })}
          totalUsers={subscription.totalUsers}
          pricePerUser={`€${subscription.pricePerUser.toFixed(2)}`}
          monthlyBill={`€${subscription.monthlyBill.toFixed(2)}`}
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

      {/* Plan Comparison */}
      <div className="grid grid-cols-1 gap-6 md:grid-cols-2">
        <PlanCard
          title="Free"
          price="Free"
          priceUnit=""
          description="Ideal for small teams"
          features={[
            { text: 'Up to 7 users', included: true },
            { text: 'Basic dashboard', included: true },
            { text: 'Emotional analysis', included: true },
            { text: 'Slack/Teams integration', included: false },
            { text: 'Priority support', included: false },
          ]}
          isCurrent={subscription?.planType === 'FREE'}
        />

        <PlanCard
          title="Enterprise"
          price="€49.90"
          priceUnit="/user/mo."
          description="For companies that need more"
          features={[
            { text: 'Unlimited users', included: true },
            { text: 'Complete dashboard', included: true },
            { text: 'Advanced emotional analysis', included: true },
            { text: 'Slack/Teams integration', included: true },
            { text: 'Priority support', included: true },
          ]}
          isCurrent={subscription?.planType === 'ENTERPRISE'}
          onUpgrade={subscription?.planType !== 'ENTERPRISE' ? handleUpgrade : undefined}
          upgradeLabel="Upgrade to Enterprise"
          upgrading={upgrading}
        />
      </div>

      {/* Payment History */}
      {user?.companyId && (
        <PaymentHistoryTable companyId={user.companyId} />
      )}
    </div>
  );
}
