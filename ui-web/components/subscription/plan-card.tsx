'use client';

import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';
import { Check, Minus } from 'lucide-react';

export interface PlanFeature {
  text: string;
  included: boolean;
}

export interface PlanCardProps {
  title: string;
  price: string;
  priceUnit?: string;
  description: string;
  features: PlanFeature[];
  isCurrent?: boolean;
  onUpgrade?: () => void;
  upgradeLabel?: string;
  upgrading?: boolean;
  className?: string;
}

export function PlanCard({
  title,
  price,
  priceUnit,
  description,
  features,
  isCurrent = false,
  onUpgrade,
  upgradeLabel = 'Upgrade',
  upgrading = false,
  className,
}: PlanCardProps) {
  return (
    <Card
      className={cn(
        'border-border/60 bg-card transition-all hover:border-border',
        isCurrent && 'border-primary/60 shadow-lg shadow-primary/10',
        className
      )}
    >
      <CardContent className="p-6 flex flex-col gap-6 h-full">
        <div className="space-y-2">
          <div className="flex items-center gap-2">
            <h3 className="text-xl font-semibold">{title}</h3>
            {isCurrent && (
              <Badge variant="secondary" className="text-xs">
                Current
              </Badge>
            )}
          </div>
          <div className="flex items-baseline gap-1">
            <span className="text-3xl font-bold">{price}</span>
            {priceUnit && (
              <span className="text-sm text-muted-foreground">{priceUnit}</span>
            )}
          </div>
          <p className="text-sm text-muted-foreground">{description}</p>
        </div>

        <div className="space-y-3 flex-1">
          {features.map((feature) => (
            <div key={feature.text} className="flex items-start gap-2">
              <div
                className={cn(
                  'h-5 w-5 rounded-full flex items-center justify-center border text-[10px]',
                  feature.included
                    ? 'border-primary/80 bg-primary/10 text-primary'
                    : 'border-muted text-muted-foreground'
                )}
              >
                {feature.included ? (
                  <Check className="h-3 w-3" />
                ) : (
                  <Minus className="h-3 w-3" />
                )}
              </div>
              <span
                className={cn(
                  'text-sm',
                  feature.included
                    ? 'text-foreground'
                    : 'text-muted-foreground line-through'
                )}
              >
                {feature.text}
              </span>
            </div>
          ))}
        </div>

        {onUpgrade && (
          <Button
            onClick={onUpgrade}
            disabled={upgrading}
            className="w-full mt-auto"
          >
            {upgrading ? 'Redirecting…' : upgradeLabel}
          </Button>
        )}
      </CardContent>
    </Card>
  );
}

