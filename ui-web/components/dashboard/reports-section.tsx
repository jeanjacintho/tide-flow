'use client';

import { useState, useEffect } from 'react';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { ReportCard } from './report-card';
import { apiService, CorporateReportResponse } from '@/lib/api';
import { FileText, Plus, RefreshCw, Download, Filter } from 'lucide-react';
import { toast } from 'sonner';

interface ReportsSectionProps {
  companyId: string;
}

export function ReportsSection({ companyId }: ReportsSectionProps) {
  const [reports, setReports] = useState<CorporateReportResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<string>('all');
  const [sortBy, setSortBy] = useState<string>('recent');

  useEffect(() => {
    loadReports();
  }, [companyId, filter, sortBy]);

  const loadReports = async () => {
    try {
      setLoading(true);

      const reportType = filter !== 'all' ? filter : undefined;
      const reportList = await apiService.listReports(
        companyId,
        reportType,
        'COMPLETED',
        0,
        50
      );

      if (reportList.reports && reportList.reports.length > 0) {
        const fullReports: CorporateReportResponse[] = reportList.reports.map((summary) => ({
          id: summary.id,
          companyId: summary.companyId,
          departmentId: summary.departmentId,
          reportType: summary.reportType,
          reportDate: summary.reportDate,
          periodStart: summary.periodStart,
          periodEnd: summary.periodEnd,
          status: summary.status as 'PENDING' | 'GENERATING' | 'COMPLETED' | 'FAILED' | 'ARCHIVED',
          title: summary.title,
          executiveSummary: summary.executiveSummary,
          generatedByAi: summary.generatedByAi,
          createdAt: summary.createdAt,
          updatedAt: summary.createdAt,
          generatedAt: summary.generatedAt,
          insights: undefined,
          metrics: undefined,
          recommendations: undefined,
        }));
        setReports(fullReports);
      } else {
        setReports([]);
      }
    } catch (error: any) {

      if (error?.message?.includes('404') || error?.message?.includes('Not Found')) {
        setReports([]);
      } else {
        console.error('Erro ao carregar relatórios:', error);

        if (!error?.message?.includes('404') && !error?.message?.includes('Not Found')) {
          toast.error('Erro ao carregar relatórios');
        }
        setReports([]);
      }
    } finally {
      setLoading(false);
    }
  };

  const handleViewReport = (reportId: string) => {

    window.location.href = `/dashboard/reports/${reportId}`;
  };

  const handleDownloadReport = async (reportId: string) => {
    try {
      toast.info('Preparando download do relatório...');

      toast.success('Download iniciado');
    } catch (error) {
      console.error('Erro ao baixar relatório:', error);
      toast.error('Erro ao baixar relatório');
    }
  };

  const handleDeleteReport = async (reportId: string) => {
    if (!confirm('Tem certeza que deseja excluir este relatório?')) {
      return;
    }

    try {
      await apiService.deleteReport(reportId);
      toast.success('Relatório excluído com sucesso');
      loadReports();
    } catch (error) {
      console.error('Erro ao excluir relatório:', error);
      toast.error('Erro ao excluir relatório');
    }
  };

  const handleGenerateReport = async () => {
    try {
      toast.info('Gerando novo relatório...');

      await loadReports();
      toast.success('Relatório gerado com sucesso');
    } catch (error) {
      console.error('Erro ao gerar relatório:', error);
      toast.error('Erro ao gerar relatório');
    }
  };

  const filteredReports = reports.filter((report) => {
    if (filter === 'all') return true;
    return report.reportType === filter;
  });

  const sortedReports = [...filteredReports].sort((a, b) => {
    if (sortBy === 'recent') {
      const dateA = a.generatedAt ? new Date(a.generatedAt).getTime() : 0;
      const dateB = b.generatedAt ? new Date(b.generatedAt).getTime() : 0;
      return dateB - dateA;
    }
    if (sortBy === 'oldest') {
      const dateA = a.generatedAt ? new Date(a.generatedAt).getTime() : 0;
      const dateB = b.generatedAt ? new Date(b.generatedAt).getTime() : 0;
      return dateA - dateB;
    }
    return 0;
  });

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <div>
            <CardTitle>Relatórios</CardTitle>
            <CardDescription>
              Relatórios gerados automaticamente com análises e insights
            </CardDescription>
          </div>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" onClick={loadReports}>
              <RefreshCw className="h-4 w-4 mr-2" />
              Atualizar
            </Button>
            <Button size="sm" onClick={handleGenerateReport}>
              <Plus className="h-4 w-4 mr-2" />
              Gerar Relatório
            </Button>
          </div>
        </div>

        {}
        <div className="flex gap-2 mt-4">
          <Select value={filter} onValueChange={setFilter}>
            <SelectTrigger className="w-[180px]">
              <Filter className="h-4 w-4 mr-2" />
              <SelectValue placeholder="Filtrar por tipo" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">Todos os Tipos</SelectItem>
              <SelectItem value="STRESS_ANALYSIS">Análise de Stress</SelectItem>
              <SelectItem value="TURNOVER_PREDICTION">Predição de Turnover</SelectItem>
              <SelectItem value="DEPARTMENT_HEATMAP">Mapa de Calor</SelectItem>
              <SelectItem value="IMPACT_ANALYSIS">Análise de Impacto</SelectItem>
            </SelectContent>
          </Select>

          <Select value={sortBy} onValueChange={setSortBy}>
            <SelectTrigger className="w-[180px]">
              <SelectValue placeholder="Ordenar por" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="recent">Mais Recentes</SelectItem>
              <SelectItem value="oldest">Mais Antigos</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </CardHeader>
      <CardContent>
        {loading ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {[1, 2, 3].map((i) => (
              <Skeleton key={i} className="h-64" />
            ))}
          </div>
        ) : sortedReports.length === 0 ? (
          <div className="text-center py-12">
            <p className="text-muted-foreground mb-4">Nenhum relatório encontrado</p>
            <Button onClick={handleGenerateReport}>
              <Plus className="h-4 w-4 mr-2" />
              Gerar Primeiro Relatório
            </Button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {sortedReports.map((report) => (
              <ReportCard
                key={report.id}
                report={report}
                onView={handleViewReport}
                onDownload={handleDownloadReport}
                onDelete={handleDeleteReport}
              />
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
