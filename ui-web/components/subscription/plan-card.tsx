'use client';

import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
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
            upgradeLabel = 'Fazer Upgrade',
  upgrading = false,
  className,
}: PlanCardProps) {
  return (
    <Card className={cn('h-full flex flex-col', className)}>
      <CardHeader>
        <div className="flex items-center gap-2">
          <CardTitle>{title}</CardTitle>
          {isCurrent && (
            <div className="inline-flex items-center rounded-md border border-border/50 px-1.5 py-0.5 text-xs font-medium transition-colors bg-muted/30">
              <span className="text-xs">Atual</span>
            </div>
          )}
          {priceUnit && (
            <span className="flex items-end gap-0.5 ml-1 text-muted-foreground">
              <span className="text-base font-medium">{price}</span>
              <span className="text-xs mb-[3px]">{priceUnit}</span>
            </span>
          )}
          {!priceUnit && (
            <span className="text-base font-medium ml-1">{price}</span>
          )}
        </div>
        <CardDescription>{description}</CardDescription>
      </CardHeader>
      <CardContent className="flex h-full flex-col justify-between">
        <div className="space-y-3 flex-1 mt-4">
          {features.map((feature) => (
            <div key={feature.text} className="flex items-start gap-2">
              <div
                className={cn(
                  'h-5 w-5 rounded-full flex items-center justify-center border text-[10px] flex-shrink-0',
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
            className={cn(
              'mt-4 w-fit',
              'bg-foreground text-background',
              'hover:bg-foreground/90',
              'disabled:opacity-50 disabled:cursor-not-allowed',
              'transition-colors',
              'rounded-md px-4 py-2 h-auto'
            )}
          >
            <span className="text-sm font-medium">
              {upgrading ? 'Redirecionando…' : upgradeLabel}
            </span>
          </Button>
        )}
      </CardContent>
    </Card>
  );
}

