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
    return canViewSubscription && !loading && subscription && subscription.planType === 'FREE';
  }, [canViewSubscription, loading, subscription]);

  if (!shouldShow) {
    return null;
  }

  return (
    <Card className="border-primary/20 bg-gradient-to-br from-primary/5 to-primary/10 p-4">
      <div className="flex items-start gap-3">
        <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-primary/10">
          <Sparkles className="h-4 w-4 text-primary" />
        </div>
        <div className="flex-1 space-y-1">
          <p className="text-sm font-medium leading-none">
            Upgrade para Enterprise
          </p>
          <p className="text-xs text-muted-foreground">
            Desbloqueie recursos avançados e aumente seus limites
          </p>
        </div>
      </div>
      <Button
        asChild
        size="sm"
        className="mt-3 w-full"
      >
        <Link href="/subscription">
          Fazer Upgrade
        </Link>
      </Button>
    </Card>
  );
}
