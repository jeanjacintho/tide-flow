'use client';

import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
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
    <div
      className={cn(
        'rounded-lg p-6',
        'bg-card',
        'border border-border/50',
        className
      )}
    >
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <div className="flex flex-col justify-between gap-4">
          <div className="flex flex-col gap-0.5">
            <div className="flex items-center gap-2">
              <div className="text-base font-medium">{planName}</div>
              <Badge variant="secondary" className="text-xs">
                Current
              </Badge>
              <span className="flex items-end gap-0.5 ml-1 text-muted-foreground">
                <span className="text-base font-medium">{price}</span>
                {priceUnit && (
                  <span className="text-xs mb-[3px]">{priceUnit}</span>
                )}
              </span>
            </div>
            <div className="text-sm text-muted-foreground mt-1">
              {description}
            </div>
          </div>
          {onManage && (
            <Button variant="outline" className="w-fit" onClick={onManage}>
              Manage Subscription
            </Button>
          )}
        </div>

        <div className="flex flex-col gap-4">
          <div className={`grid gap-4 ${isFreePlan ? 'grid-cols-2' : 'grid-cols-2'}`}>
            <div>
              <div className="text-xs text-muted-foreground mb-1">
                Total Users
              </div>
              <div className="text-base font-medium">{totalUsers}</div>
            </div>
            <div>
              <div className="text-xs text-muted-foreground mb-1">
                Price per User
              </div>
              <div className="text-base font-medium">{pricePerUser}</div>
            </div>
            {!isFreePlan && (
              <>
                <div>
                  <div className="text-xs text-muted-foreground mb-1">
                    Billing Cycle
                  </div>
                  <div className="text-base font-medium">{billingCycle}</div>
                </div>
                <div>
                  <div className="text-xs text-muted-foreground mb-1">
                    Next Billing
                  </div>
                  <div className="text-base font-medium">
                    {nextBillingDate || 'Not available'}
                  </div>
                </div>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}


