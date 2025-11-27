'use client';

import { ReactNode } from 'react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';
import { Check } from 'lucide-react';

export interface PlanCardProps {
  title: string;
  price: string;
  priceUnit?: string;
  description: string;
  features: Array<{
    text: string;
    included: boolean;
  }>;
  isCurrent?: boolean;
  isPopular?: boolean;
  onUpgrade?: () => void;
  upgradeLabel?: string;
  upgrading?: boolean;
  className?: string;
  actionButton?: ReactNode;
  badge?: string;
}

export function PlanCard({
  title,
  price,
  priceUnit = '/mo.',
  description,
  features,
  isCurrent = false,
  isPopular = false,
  onUpgrade,
  upgradeLabel = 'Upgrade',
  upgrading = false,
  className,
  actionButton,
  badge,
}: PlanCardProps) {
  return (
    <div
      className={cn(
        'rounded-lg p-4 h-full flex flex-col justify-between',
        'bg-card',
        'border border-border/50',
        isCurrent && 'ring-2 ring-primary/20',
        isPopular && 'ring-2 ring-primary',
        className
      )}
    >
      <div className="flex flex-col gap-0.5">
        <div className="flex items-center gap-2">
          <div className="text-base font-medium">{title}</div>
          {badge && (
            <Badge variant="secondary" className="text-xs">
              {badge}
            </Badge>
          )}
          {isCurrent && (
            <Badge variant="secondary" className="text-xs">
              Current
            </Badge>
          )}
          <span className="flex items-end gap-0.5 ml-1 text-muted-foreground">
            <span className="text-base font-medium">{price}</span>
            {priceUnit && (
              <span className="text-xs mb-[3px]">{priceUnit}</span>
            )}
          </span>
        </div>
        <div className="text-sm text-muted-foreground mt-1">{description}</div>
      </div>

      <div className="mt-4 space-y-2">
        {features.map((feature, index) => (
          <div
            key={index}
            className={cn(
              'flex items-center gap-2 text-sm',
              !feature.included && 'text-muted-foreground'
            )}
          >
            {feature.included ? (
              <Check className="w-4 h-4 text-green-600 dark:text-green-400 flex-shrink-0" />
            ) : (
              <div className="w-4 h-4 flex-shrink-0" />
            )}
            <span>{feature.text}</span>
          </div>
        ))}
      </div>

      <div className="mt-4">
        {actionButton || (
          <>
            {isCurrent ? (
              <Button
                variant="outline"
                className="w-full"
                disabled
              >
                Current Plan
              </Button>
            ) : (
              <Button
                onClick={onUpgrade}
                disabled={upgrading || !onUpgrade}
                className="w-full"
              >
                {upgrading ? 'Processing...' : upgradeLabel}
              </Button>
            )}
          </>
        )}
      </div>
    </div>
  );
}
