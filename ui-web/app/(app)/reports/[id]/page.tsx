'use client';

import { useState, useEffect } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { useRequireRole } from '@/hooks/useRequireRole';
import { apiService, CorporateReportResponse } from '@/lib/api';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { Separator } from '@/components/ui/separator';
import { ArrowLeft, FileText, Calendar, Clock, Sparkles, TrendingUp } from 'lucide-react';
import { format } from 'date-fns';
import { toast } from 'sonner';

const STATUS_COLORS: Record<string, string> = {
  PENDING: 'bg-yellow-500/10 text-yellow-500',
  GENERATING: 'bg-blue-500/10 text-blue-500',
  COMPLETED: 'bg-green-500/10 text-green-500',
  FAILED: 'bg-red-500/10 text-red-500',
  ARCHIVED: 'bg-gray-500/10 text-gray-500',
};

export default function ReportViewPage() {
  const { hasAccess, isChecking } = useRequireRole({
    companyRole: ['HR_MANAGER', 'ADMIN', 'OWNER'],
    redirectTo: '/chat'
  });

  const params = useParams();
  const router = useRouter();
  const reportId = params.id as string;

  const [report, setReport] = useState<CorporateReportResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (hasAccess && reportId) {
      loadReport();
    }
  }, [hasAccess, reportId]);

  const loadReport = async () => {
    try {
      setLoading(true);
      const data = await apiService.getReport(reportId);
      setReport(data);

      if (data.status === 'GENERATING' || data.status === 'PENDING') {
        setTimeout(() => loadReport(), 5000);
      }
    } catch (error) {
      console.error('Erro ao carregar relatório:', error);
      toast.error('Erro ao carregar relatório');
    } finally {
      setLoading(false);
    }
  };

  if (isChecking || !hasAccess) {
    return null;
  }

  if (loading) {
    return (
      <div className="container mx-auto py-8 px-4">
        <Skeleton className="h-8 w-64 mb-4" />
        <Skeleton className="h-96 w-full" />
      </div>
    );
  }

  if (!report) {
    return (
      <div className="container mx-auto py-8 px-4">
        <Card>
          <CardContent className="py-12 text-center">
            <p className="text-muted-foreground">Relatório não encontrado</p>
            <Button onClick={() => router.push('/reports')} className="mt-4">
              Voltar para Relatórios
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="container mx-auto py-8 px-4">
      <div className="mb-6">
        <Button
          variant="ghost"
          onClick={() => router.push('/reports')}
          className="mb-4"
        >
          <ArrowLeft className="mr-2 h-4 w-4" />
          Voltar
        </Button>

        <div className="flex justify-between items-start">
          <div>
            <h1 className="text-3xl font-bold">{report.title}</h1>
            <div className="flex items-center space-x-4 mt-2 text-muted-foreground">
              <div className="flex items-center space-x-1">
                <Calendar className="h-4 w-4" />
                <span>
                  {format(new Date(report.periodStart), 'dd/MM/yyyy')} - {format(new Date(report.periodEnd), 'dd/MM/yyyy')}
                </span>
              </div>
              {report.generatedAt && (
                <div className="flex items-center space-x-1">
                  <Clock className="h-4 w-4" />
                  <span>Gerado em {format(new Date(report.generatedAt), 'dd/MM/yyyy HH:mm')}</span>
                </div>
              )}
            </div>
          </div>
          <Badge className={STATUS_COLORS[report.status] || STATUS_COLORS.PENDING}>
            {report.status}
          </Badge>
        </div>
      </div>

      {report.status === 'GENERATING' || report.status === 'PENDING' ? (
        <Card>
          <CardContent className="py-12 text-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto mb-4"></div>
            <p className="text-muted-foreground">Gerando relatório... Isso pode levar alguns minutos.</p>
          </CardContent>
        </Card>
      ) : report.status === 'FAILED' ? (
        <Card>
          <CardContent className="py-12 text-center">
            <p className="text-destructive mb-4">Falha ao gerar o relatório</p>
            <Button onClick={() => router.push('/reports')}>
              Voltar para Relatórios
            </Button>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-6">
          {report.executiveSummary && (
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center space-x-2">
                  <FileText className="h-5 w-5" />
                  <span>Resumo Executivo</span>
                </CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-muted-foreground whitespace-pre-wrap">{report.executiveSummary}</p>
              </CardContent>
            </Card>
          )}

          {report.sections && report.sections.length > 0 && (
            <div className="space-y-4">
              {report.sections.map((section) => (
                <Card key={section.id}>
                  <CardHeader>
                    <CardTitle>{section.title}</CardTitle>
                  </CardHeader>
                  <CardContent>
                    {section.content && (
                      <div className="prose max-w-none mb-4">
                        <p className="text-muted-foreground whitespace-pre-wrap">{section.content}</p>
                      </div>
                    )}
                    {section.data && Object.keys(section.data).length > 0 && (
                      <div className="mt-4">
                        <pre className="bg-muted p-4 rounded-lg overflow-auto text-sm">
                          {JSON.stringify(section.data, null, 2)}
                        </pre>
                      </div>
                    )}
                  </CardContent>
                </Card>
              ))}
            </div>
          )}

          {report.insights && Object.keys(report.insights).length > 0 && (
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center space-x-2">
                  <Sparkles className="h-5 w-5" />
                  <span>Insights</span>
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="prose max-w-none">
                  <pre className="bg-muted p-4 rounded-lg overflow-auto text-sm">
                    {JSON.stringify(report.insights, null, 2)}
                  </pre>
                </div>
              </CardContent>
            </Card>
          )}

          {report.recommendations && (
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center space-x-2">
                  <TrendingUp className="h-5 w-5" />
                  <span>Recomendações</span>
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="prose max-w-none">
                  <p className="text-muted-foreground whitespace-pre-wrap">{report.recommendations}</p>
                </div>
              </CardContent>
            </Card>
          )}

          {report.metrics && Object.keys(report.metrics).length > 0 && (
            <Card>
              <CardHeader>
                <CardTitle>Métricas Detalhadas</CardTitle>
              </CardHeader>
              <CardContent>
                <pre className="bg-muted p-4 rounded-lg overflow-auto text-sm">
                  {JSON.stringify(report.metrics, null, 2)}
                </pre>
              </CardContent>
            </Card>
          )}

          {report.generatedByAi && (
            <Card>
              <CardContent className="py-4">
                <div className="flex items-center justify-between text-sm text-muted-foreground">
                  <span>Gerado por IA</span>
                  {report.generationTimeMs && (
                    <span>Tempo de geração: {(report.generationTimeMs / 1000).toFixed(2)}s</span>
                  )}
                </div>
              </CardContent>
            </Card>
          )}
        </div>
      )}
    </div>
  );
}
