'use client';

import { useState, useEffect } from 'react';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Skeleton } from '@/components/ui/skeleton';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';
import { apiService, PaymentHistory } from '@/lib/api';
import { CheckCircle2, XCircle, Clock, RefreshCw } from 'lucide-react';

interface PaymentHistoryTableProps {
  companyId: string;
  className?: string;
}

export function PaymentHistoryTable({ companyId, className }: PaymentHistoryTableProps) {
  const [payments, setPayments] = useState<PaymentHistory[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (companyId) {
      fetchPaymentHistory();
    }
  }, [companyId]);

  const fetchPaymentHistory = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await apiService.getPaymentHistory(companyId, 0, 50);
      // Handle both paginated response and array response
      if (Array.isArray(data)) {
        setPayments(data);
      } else if (data.content) {
        setPayments(data.content);
      } else {
        setPayments([]);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao carregar histórico de pagamentos');
      console.error('Error fetching payment history:', err);
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (amount: number, currency: string = 'EUR') => {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: currency,
    }).format(amount);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('pt-BR', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
    });
  };

  const getStatusBadge = (status: PaymentHistory['status']) => {
    const statusConfig = {
      SUCCEEDED: {
        label: 'Paid',
        variant: 'default' as const,
        icon: CheckCircle2,
        className: 'bg-green-50 text-green-700 border-green-200 dark:bg-green-950/30 dark:text-green-400 dark:border-green-800',
      },
      PENDING: {
        label: 'Pending',
        variant: 'secondary' as const,
        icon: Clock,
        className: 'bg-yellow-50 text-yellow-700 border-yellow-200 dark:bg-yellow-950/30 dark:text-yellow-400 dark:border-yellow-800',
      },
      FAILED: {
        label: 'Failed',
        variant: 'destructive' as const,
        icon: XCircle,
        className: 'bg-red-50 text-red-700 border-red-200 dark:bg-red-950/30 dark:text-red-400 dark:border-red-800',
      },
      REFUNDED: {
        label: 'Refunded',
        variant: 'outline' as const,
        icon: RefreshCw,
        className: 'bg-gray-50 text-gray-700 border-gray-200 dark:bg-gray-800 dark:text-gray-400 dark:border-gray-700',
      },
      PARTIALLY_REFUNDED: {
        label: 'Partially Refunded',
        variant: 'outline' as const,
        icon: RefreshCw,
        className: 'bg-gray-50 text-gray-700 border-gray-200 dark:bg-gray-800 dark:text-gray-400 dark:border-gray-700',
      },
    };

    const config = statusConfig[status];
    const Icon = config.icon;

    return (
      <Badge
        variant="outline"
        className={cn(
          'flex items-center gap-1.5 text-xs font-medium border',
          config.className
        )}
      >
        <Icon className="w-3 h-3" />
        {config.label}
      </Badge>
    );
  };

  if (loading) {
    return (
      <Card className={cn('rounded-lg', className)}>
        <CardHeader>
          <CardTitle>Payment History</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-2">
            {[1, 2, 3].map((i) => (
              <Skeleton key={i} className="h-12 w-full" />
            ))}
          </div>
        </CardContent>
      </Card>
    );
  }

  if (error) {
    return (
      <Card className={cn('rounded-lg', className)}>
        <CardHeader>
          <CardTitle>Payment History</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="text-sm text-muted-foreground">{error}</div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className={cn('rounded-lg', className)}>
      <CardHeader>
        <CardTitle>Payment History</CardTitle>
      </CardHeader>
      <CardContent>
        {payments.length === 0 ? (
          <div className="text-center py-8 text-sm text-muted-foreground">
            No payment history available
          </div>
        ) : (
          <div className="rounded-lg border border-border/50 overflow-hidden">
            <Table>
              <TableHeader>
                <TableRow className="border-b border-border/50 hover:bg-transparent">
                  <TableHead className="h-10 px-4 text-xs font-medium text-muted-foreground uppercase tracking-wider">
                    Date
                  </TableHead>
                  <TableHead className="h-10 px-4 text-xs font-medium text-muted-foreground uppercase tracking-wider">
                    Invoice
                  </TableHead>
                  <TableHead className="h-10 px-4 text-xs font-medium text-muted-foreground uppercase tracking-wider">
                    Amount
                  </TableHead>
                  <TableHead className="h-10 px-4 text-xs font-medium text-muted-foreground uppercase tracking-wider">
                    Status
                  </TableHead>
                  <TableHead className="h-10 px-4 text-xs font-medium text-muted-foreground uppercase tracking-wider text-right">
                    Period
                  </TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {payments.map((payment, index) => (
                  <TableRow
                    key={payment.id}
                    className={cn(
                      'border-b border-border/50 transition-colors',
                      index === payments.length - 1 && 'border-b-0',
                      'hover:bg-muted/30'
                    )}
                  >
                    <TableCell className="px-4 py-3 text-sm">
                      <div className="font-medium text-foreground">
                        {formatDate(payment.paymentDate)}
                      </div>
                    </TableCell>
                    <TableCell className="px-4 py-3">
                      <div className="flex flex-col gap-0.5">
                        <span className="text-sm font-medium text-foreground">
                          {payment.invoiceNumber || payment.stripeInvoiceId?.slice(-8) || 'N/A'}
                        </span>
                        {payment.description && (
                          <span className="text-xs text-muted-foreground">
                            {payment.description}
                          </span>
                        )}
                      </div>
                    </TableCell>
                    <TableCell className="px-4 py-3">
                      <span className="text-sm font-medium text-foreground">
                        {formatCurrency(payment.amount, payment.currency)}
                      </span>
                    </TableCell>
                    <TableCell className="px-4 py-3">{getStatusBadge(payment.status)}</TableCell>
                    <TableCell className="px-4 py-3 text-right">
                      {payment.billingPeriodStart && payment.billingPeriodEnd ? (
                        <div className="flex flex-col items-end gap-0.5">
                          <span className="text-sm text-muted-foreground">
                            {formatDate(payment.billingPeriodStart)}
                          </span>
                          <span className="text-xs text-muted-foreground">
                            to {formatDate(payment.billingPeriodEnd)}
                          </span>
                        </div>
                      ) : (
                        <span className="text-sm text-muted-foreground">-</span>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
