'use client';

import { useEffect, useState, useCallback, useMemo } from 'react';
import { useAuth } from '@/contexts/AuthContext';
import { apiService, SubscriptionResponse } from '@/lib/api';
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Sparkles } from 'lucide-react';
import Link from 'next/link';

export function SubscriptionCard() {
  const { user } = useAuth();
  const [subscription, setSubscription] = useState<SubscriptionResponse | null>(null);
  const [loading, setLoading] = useState(true);

  const companyId = user?.companyId;

  const canViewSubscription = user?.companyRole === 'OWNER' || user?.companyRole === 'ADMIN';

  const fetchSubscription = useCallback(async () => {
    if (!companyId || !canViewSubscription) {
      setLoading(false);
      return;
    }

    try {
      const data = await apiService.getSubscription(companyId);
      setSubscription(data);
    } catch (error) {
      console.error('Erro ao buscar assinatura:', error);

      setSubscription(null);
    } finally {
      setLoading(false);
    }
  }, [companyId, canViewSubscription]);

  useEffect(() => {
    if (companyId && canViewSubscription) {
      fetchSubscription();
    } else {
      setLoading(false);
    }
  }, [companyId, canViewSubscription, fetchSubscription]);

  const shouldShow = useMemo(() => {

    return canViewSubscription && !loading && (!subscription || subscription.planType === 'FREE');
  }, [canViewSubscription, loading, subscription]);

  if (!shouldShow) {
    return null;
  }

  return (
    <Card className="rounded-lg p-4 border-border/50 bg-card">
      <div className="space-y-2">
        <div className="flex items-center gap-2">
          <Sparkles className="h-4 w-4 text-primary" />
          <p className="text-sm font-semibold leading-none">
            Upgrade para Plano Premium
          </p>
        </div>
        <p className="text-xs text-muted-foreground">
          Desbloqueie análises de IA avançadas, mapas de calor emocional em tempo real e predição de turnover. Prevenção proativa para sua equipe.
        </p>
        <p className="text-xs font-medium text-primary">
          Apenas R$ 199,90/mês
        </p>
      </div>
      <Button
        asChild
        size="sm"
        className="mt-4 w-full"
      >
        <Link href="/subscription">
          Fazer Upgrade
        </Link>
      </Button>
    </Card>
  );
}
