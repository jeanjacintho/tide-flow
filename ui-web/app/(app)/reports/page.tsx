'use client';

import { useState, useEffect } from 'react';
import { useRequireRole } from '@/hooks/useRequireRole';
import { useAuth } from '@/contexts/AuthContext';
import { apiService, ReportListResponse, ReportSummary, ReportGenerationRequest } from '@/lib/api';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { FileText, Plus, Calendar, Filter, Download, Eye, Trash2, Loader2 } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { toast } from 'sonner';
import { format } from 'date-fns';

const REPORT_TYPES = [
  { value: 'STRESS_TIMELINE', label: 'Sismógrafo de Stress' },
  { value: 'DEPARTMENT_HEATMAP', label: 'Mapa de Calor por Setor' },
  { value: 'TURNOVER_PREDICTION', label: 'Predição de Turnover' },
  { value: 'IMPACT_ANALYSIS', label: 'Análise de Impacto' },
  { value: 'COMPREHENSIVE', label: 'Relatório Abrangente' },
];

const STATUS_COLORS: Record<string, string> = {
  PENDING: 'bg-yellow-500/10 text-yellow-500',
  GENERATING: 'bg-blue-500/10 text-blue-500',
  COMPLETED: 'bg-green-500/10 text-green-500',
  FAILED: 'bg-red-500/10 text-red-500',
  ARCHIVED: 'bg-gray-500/10 text-gray-500',
};

export default function ReportsPage() {
  const { hasAccess, isChecking } = useRequireRole({ 
    companyRole: ['HR_MANAGER', 'ADMIN', 'OWNER'],
    redirectTo: '/chat'
  });
  
  const { user } = useAuth();
  const router = useRouter();
  
  const [reports, setReports] = useState<ReportSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [filters, setFilters] = useState({
    reportType: '',
    status: '',
  });
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [openDialog, setOpenDialog] = useState(false);
  const [newReport, setNewReport] = useState<Partial<ReportGenerationRequest>>({
    reportType: 'COMPREHENSIVE',
    generateInsights: true,
    generateRecommendations: true,
  });

  const companyId = user?.companyId || '';

  useEffect(() => {
    if (hasAccess && companyId) {
      loadReports();
    }
  }, [hasAccess, companyId, page, filters]);

  const loadReports = async () => {
    if (!companyId) return;
    
    try {
      setLoading(true);
      const response = await apiService.listReports(
        companyId,
        filters.reportType || undefined,
        filters.status || undefined,
        page,
        20
      );
      setReports(response.reports);
      setTotalPages(response.totalPages);
    } catch (error) {
      console.error('Erro ao carregar relatórios:', error);
      toast.error('Erro ao carregar relatórios');
    } finally {
      setLoading(false);
    }
  };

  const handleGenerateReport = async () => {
    if (!companyId || !newReport.reportType) {
      toast.error('Preencha todos os campos obrigatórios');
      return;
    }

    try {
      setGenerating(true);
      const request: ReportGenerationRequest = {
        companyId,
        reportType: newReport.reportType as any,
        periodStart: newReport.periodStart,
        periodEnd: newReport.periodEnd,
        title: newReport.title,
        generateInsights: newReport.generateInsights ?? true,
        generateRecommendations: newReport.generateRecommendations ?? true,
        includeSections: newReport.includeSections,
      };

      const report = await apiService.generateReport(request);
      toast.success('Relatório gerado com sucesso!');
      setOpenDialog(false);
      setNewReport({
        reportType: 'COMPREHENSIVE',
        generateInsights: true,
        generateRecommendations: true,
      });
      router.push(`/reports/${report.id}`);
    } catch (error) {
      console.error('Erro ao gerar relatório:', error);
      toast.error('Erro ao gerar relatório');
    } finally {
      setGenerating(false);
    }
  };

  const handleDeleteReport = async (reportId: string) => {
    if (!confirm('Tem certeza que deseja deletar este relatório?')) {
      return;
    }

    try {
      await apiService.deleteReport(reportId);
      toast.success('Relatório deletado com sucesso');
      loadReports();
    } catch (error) {
      console.error('Erro ao deletar relatório:', error);
      toast.error('Erro ao deletar relatório');
    }
  };

  if (isChecking || !hasAccess) {
    return null;
  }

  return (
    <div className="container mx-auto py-8 px-4">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-3xl font-bold">Relatórios Corporativos</h1>
          <p className="text-muted-foreground mt-2">
            Visualize e gere relatórios de bem-estar corporativo
          </p>
        </div>
        <Dialog open={openDialog} onOpenChange={setOpenDialog}>
          <DialogTrigger asChild>
            <Button>
              <Plus className="mr-2 h-4 w-4" />
              Novo Relatório
            </Button>
          </DialogTrigger>
          <DialogContent className="max-w-2xl">
            <DialogHeader>
              <DialogTitle>Gerar Novo Relatório</DialogTitle>
              <DialogDescription>
                Configure os parâmetros para gerar um novo relatório corporativo
              </DialogDescription>
            </DialogHeader>
            <div className="space-y-4">
              <div>
                <Label htmlFor="reportType">Tipo de Relatório</Label>
                <Select
                  value={newReport.reportType}
                  onValueChange={(value) => setNewReport({ ...newReport, reportType: value as any })}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Selecione o tipo" />
                  </SelectTrigger>
                  <SelectContent>
                    {REPORT_TYPES.map((type) => (
                      <SelectItem key={type.value} value={type.value}>
                        {type.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div>
                <Label htmlFor="title">Título (opcional)</Label>
                <Input
                  id="title"
                  value={newReport.title || ''}
                  onChange={(e) => setNewReport({ ...newReport, title: e.target.value })}
                  placeholder="Título do relatório"
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <Label htmlFor="periodStart">Data Inicial</Label>
                  <Input
                    id="periodStart"
                    type="date"
                    value={newReport.periodStart || ''}
                    onChange={(e) => setNewReport({ ...newReport, periodStart: e.target.value })}
                  />
                </div>
                <div>
                  <Label htmlFor="periodEnd">Data Final</Label>
                  <Input
                    id="periodEnd"
                    type="date"
                    value={newReport.periodEnd || ''}
                    onChange={(e) => setNewReport({ ...newReport, periodEnd: e.target.value })}
                  />
                </div>
              </div>
              <div className="flex items-center space-x-4">
                <div className="flex items-center space-x-2">
                  <input
                    type="checkbox"
                    id="generateInsights"
                    checked={newReport.generateInsights ?? true}
                    onChange={(e) => setNewReport({ ...newReport, generateInsights: e.target.checked })}
                    className="rounded"
                  />
                  <Label htmlFor="generateInsights">Gerar Insights com IA</Label>
                </div>
                <div className="flex items-center space-x-2">
                  <input
                    type="checkbox"
                    id="generateRecommendations"
                    checked={newReport.generateRecommendations ?? true}
                    onChange={(e) => setNewReport({ ...newReport, generateRecommendations: e.target.checked })}
                    className="rounded"
                  />
                  <Label htmlFor="generateRecommendations">Gerar Recomendações</Label>
                </div>
              </div>
              <div className="flex justify-end space-x-2">
                <Button variant="outline" onClick={() => setOpenDialog(false)}>
                  Cancelar
                </Button>
                <Button onClick={handleGenerateReport} disabled={generating}>
                  {generating ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      Gerando...
                    </>
                  ) : (
                    'Gerar Relatório'
                  )}
                </Button>
              </div>
            </div>
          </DialogContent>
        </Dialog>
      </div>

      <Card>
        <CardHeader>
          <div className="flex justify-between items-center">
            <div>
              <CardTitle>Relatórios</CardTitle>
              <CardDescription>Lista de todos os relatórios gerados</CardDescription>
            </div>
            <div className="flex space-x-2">
              <Select
                value={filters.reportType || undefined}
                onValueChange={(value) => setFilters({ ...filters, reportType: value === 'all' ? '' : value })}
              >
                <SelectTrigger className="w-[200px]">
                  <SelectValue placeholder="Filtrar por tipo" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">Todos os tipos</SelectItem>
                  {REPORT_TYPES.map((type) => (
                    <SelectItem key={type.value} value={type.value}>
                      {type.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <Select
                value={filters.status || undefined}
                onValueChange={(value) => setFilters({ ...filters, status: value === 'all' ? '' : value })}
              >
                <SelectTrigger className="w-[180px]">
                  <SelectValue placeholder="Filtrar por status" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">Todos os status</SelectItem>
                  <SelectItem value="COMPLETED">Completos</SelectItem>
                  <SelectItem value="GENERATING">Gerando</SelectItem>
                  <SelectItem value="PENDING">Pendentes</SelectItem>
                  <SelectItem value="FAILED">Falhados</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="space-y-2">
              {[...Array(5)].map((_, i) => (
                <Skeleton key={i} className="h-16 w-full" />
              ))}
            </div>
          ) : reports.length === 0 ? (
            <div className="text-center py-12">
              <FileText className="mx-auto h-12 w-12 text-muted-foreground mb-4" />
              <p className="text-muted-foreground">Nenhum relatório encontrado</p>
            </div>
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Título</TableHead>
                    <TableHead>Tipo</TableHead>
                    <TableHead>Período</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Data de Criação</TableHead>
                    <TableHead className="text-right">Ações</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {reports.map((report) => (
                    <TableRow key={report.id}>
                      <TableCell className="font-medium">{report.title}</TableCell>
                      <TableCell>
                        {REPORT_TYPES.find(t => t.value === report.reportType)?.label || report.reportType}
                      </TableCell>
                      <TableCell>
                        {format(new Date(report.periodStart), 'dd/MM/yyyy')} - {format(new Date(report.periodEnd), 'dd/MM/yyyy')}
                      </TableCell>
                      <TableCell>
                        <Badge className={STATUS_COLORS[report.status] || STATUS_COLORS.PENDING}>
                          {report.status}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        {format(new Date(report.createdAt), 'dd/MM/yyyy HH:mm')}
                      </TableCell>
                      <TableCell className="text-right">
                        <div className="flex justify-end space-x-2">
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => router.push(`/reports/${report.id}`)}
                          >
                            <Eye className="h-4 w-4" />
                          </Button>
                          {report.status === 'COMPLETED' && (
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => handleDeleteReport(report.id)}
                            >
                              <Trash2 className="h-4 w-4 text-destructive" />
                            </Button>
                          )}
                        </div>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
              {totalPages > 1 && (
                <div className="flex justify-center items-center space-x-2 mt-4">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setPage(p => Math.max(0, p - 1))}
                    disabled={page === 0}
                  >
                    Anterior
                  </Button>
                  <span className="text-sm text-muted-foreground">
                    Página {page + 1} de {totalPages}
                  </span>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                    disabled={page >= totalPages - 1}
                  >
                    Próxima
                  </Button>
                </div>
              )}
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
