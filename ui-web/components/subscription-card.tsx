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
  
  // Apenas OWNER e ADMIN podem ver o card de assinatura
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
      // Se não conseguir buscar, assume que não tem assinatura (null)
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
    // Mostra o card se:
    // 1. Usuário pode ver (OWNER ou ADMIN)
    // 2. Não está carregando
    // 3. Não tem assinatura OU a assinatura é FREE
    return canViewSubscription && !loading && (!subscription || subscription.planType === 'FREE');
  }, [canViewSubscription, loading, subscription]);

  if (!shouldShow) {
    return null;
  }

  return (
    <Card className="rounded-lg p-4 border-border/50 bg-card">
      <div className="space-y-2">
        <p className="text-sm font-semibold leading-none">
          Upgrade to Premium Plan
        </p>
        <p className="text-xs text-muted-foreground">
          Unlock advanced AI analytics, priority support, and exclusive HR tools.
        </p>
      </div>
      <Button
        asChild
        size="sm"
        className="mt-4 w-full"
      >
        <Link href="/subscription">
          Upgrade Now
        </Link>
      </Button>
    </Card>
  );
}
