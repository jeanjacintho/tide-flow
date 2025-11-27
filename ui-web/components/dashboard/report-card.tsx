'use client';

import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import {
  FileText,
  Calendar,
  Download,
  Trash2,
  Eye,
  TrendingUp,
  AlertTriangle,
  Users,
} from 'lucide-react';
import { CorporateReportResponse } from '@/lib/api';
import { format } from 'date-fns';
import { ptBR } from 'date-fns/locale';

interface ReportCardProps {
  report: CorporateReportResponse;
  onView?: (reportId: string) => void;
  onDownload?: (reportId: string) => void;
  onDelete?: (reportId: string) => void;
  loading?: boolean;
}

export function ReportCard({
  report,
  onView,
  onDownload,
  onDelete,
  loading = false,
}: ReportCardProps) {
  if (loading) {
    return (
      <Card>
        <CardHeader>
          <Skeleton className="h-6 w-32" />
          <Skeleton className="h-4 w-48 mt-2" />
        </CardHeader>
        <CardContent>
          <Skeleton className="h-20 w-full" />
        </CardContent>
      </Card>
    );
  }

  const getReportTypeBadge = (type: string) => {
    switch (type) {
      case 'STRESS_ANALYSIS':
        return { label: 'Análise de Stress', variant: 'destructive' as const };
      case 'TURNOVER_PREDICTION':
        return { label: 'Predição de Turnover', variant: 'default' as const };
      case 'DEPARTMENT_HEATMAP':
        return { label: 'Mapa de Calor', variant: 'secondary' as const };
      case 'IMPACT_ANALYSIS':
        return { label: 'Análise de Impacto', variant: 'outline' as const };
      default:
        return { label: type, variant: 'secondary' as const };
    }
  };

  const reportType = getReportTypeBadge(report.reportType);

  return (
    <Card className="hover:shadow-md transition-shadow">
      <CardHeader>
        <div className="flex items-start justify-between">
          <div className="flex-1">
            <CardTitle className="text-lg">
              {report.title || 'Relatório Sem Título'}
            </CardTitle>
            <CardDescription className="mt-1">
              {report.generatedAt
                ? format(new Date(report.generatedAt), "dd 'de' MMMM 'de' yyyy 'às' HH:mm", {
                    locale: ptBR,
                  })
                : 'Data não disponível'}
            </CardDescription>
          </div>
          <Badge variant={reportType.variant}>{reportType.label}</Badge>
        </div>
      </CardHeader>
      <CardContent>
        {/* Report Summary */}
        {report.executiveSummary && (
          <p className="text-sm text-muted-foreground mb-4 line-clamp-2">{report.executiveSummary}</p>
        )}

        {/* Key Metrics */}
        {report.metrics && (
          <div className="grid grid-cols-2 gap-4 mb-4">
            {report.metrics.totalUsers !== undefined && (
              <div className="text-sm">
                <p className="text-muted-foreground">Usuários</p>
                <p className="font-medium">{report.metrics.totalUsers}</p>
              </div>
            )}
            {report.metrics.avgStressLevel !== undefined && (
              <div className="text-sm">
                <p className="text-muted-foreground">Stress Médio</p>
                <p className="font-medium">{Number(report.metrics.avgStressLevel).toFixed(1)}%</p>
              </div>
            )}
            {report.metrics.riskAlertsCount !== undefined && Number(report.metrics.riskAlertsCount) > 0 && (
              <div className="text-sm col-span-2">
                <p className="text-destructive font-medium">
                  {report.metrics.riskAlertsCount} alerta{Number(report.metrics.riskAlertsCount) > 1 ? 's' : ''} de risco
                </p>
              </div>
            )}
          </div>
        )}

        {/* Status Badge */}
        <div className="mb-4">
          <Badge
            variant={
              report.status === 'COMPLETED'
                ? 'default'
                : report.status === 'FAILED'
                ? 'destructive'
                : report.status === 'GENERATING'
                ? 'secondary'
                : 'outline'
            }
          >
            {report.status}
          </Badge>
        </div>

        {/* Actions */}
        <div className="flex gap-2 pt-4 border-t">
          {onView && (
            <Button
              variant="outline"
              size="sm"
              className="flex-1"
              onClick={() => onView(report.id)}
            >
              <Eye className="h-4 w-4 mr-2" />
              Visualizar
            </Button>
          )}
          {onDownload && (
            <Button
              variant="outline"
              size="sm"
              className="flex-1"
              onClick={() => onDownload(report.id)}
            >
              <Download className="h-4 w-4 mr-2" />
              Download
            </Button>
          )}
          {onDelete && (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => onDelete(report.id)}
              className="text-destructive hover:text-destructive"
            >
              <Trash2 className="h-4 w-4" />
            </Button>
          )}
        </div>
      </CardContent>
    </Card>
  );
}

