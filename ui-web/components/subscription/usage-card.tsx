'use client';

import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { AlertCircle } from 'lucide-react';
import { cn } from '@/lib/utils';

export interface UsageCardProps {
  activeUsers: number;
  maxUsers: number;
  atLimit: boolean;
  remainingSlots: number;
  className?: string;
}

export function UsageCard({
  activeUsers,
  maxUsers,
  atLimit,
  remainingSlots,
  className,
}: UsageCardProps) {
  const usagePercentage = Math.min(100, (activeUsers / maxUsers) * 100);

  return (
    <Card
      className={cn(
        'rounded-lg',
        'bg-card',
        'border border-border/50',
        className
      )}
    >
      <CardHeader>
        <CardTitle>Current Usage</CardTitle>
        <CardDescription>
          Information about your company's usage
        </CardDescription>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <span className="text-sm text-muted-foreground">Active Users</span>
            <span className="text-lg font-semibold">
              {activeUsers} / {maxUsers}
            </span>
          </div>

          <div className="w-full bg-muted rounded-full h-2">
            <div
              className={cn(
                'h-2 rounded-full transition-all',
                atLimit
                  ? 'bg-red-500'
                  : usagePercentage > 80
                  ? 'bg-yellow-500'
                  : 'bg-green-500'
              )}
              style={{
                width: `${usagePercentage}%`,
              }}
            />
          </div>

          {atLimit && (
            <div className="flex items-center gap-2 p-3 bg-yellow-50 dark:bg-yellow-950 border border-yellow-200 dark:border-yellow-800 rounded-lg">
              <AlertCircle className="w-5 h-5 text-yellow-600 dark:text-yellow-400" />
              <div className="text-sm text-yellow-800 dark:text-yellow-200">
                User limit reached. Upgrade to add more users.
              </div>
            </div>
          )}

          {!atLimit && (
            <div className="text-sm text-muted-foreground">
              {remainingSlots} slots available
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
