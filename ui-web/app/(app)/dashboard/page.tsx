'use client';

import { useState } from 'react';
import { useRequireRole } from '@/hooks/useRequireRole';
import { useDashboardData, useStressTimeline, useDepartmentHeatmap, useTurnoverPrediction } from '@/hooks/use-dashboard';
import { StressSeismograph } from '@/components/dashboard/stress-seismograph';
import { DepartmentHeatmap } from '@/components/dashboard/department-heatmap';
import { TurnoverPrediction } from '@/components/dashboard/turnover-prediction';
import { ImpactAnalysis } from '@/components/dashboard/impact-analysis';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Skeleton } from '@/components/ui/skeleton';
import { format, subDays, startOfWeek, endOfWeek } from 'date-fns';
import { Calendar, Users, MessageSquare, AlertTriangle, TrendingUp } from 'lucide-react';

// TODO: Obter companyId do contexto do usuário ou token
const MOCK_COMPANY_ID = '00000000-0000-0000-0000-000000000000';

export default function DashboardPage() {
  // PROTEÇÃO DA ROTA: Apenas HR_MANAGER, ADMIN e OWNER podem acessar
  // Este hook DEVE ser o primeiro a ser chamado
  const { hasAccess, isChecking } = useRequireRole({ 
    companyRole: ['HR_MANAGER', 'ADMIN', 'OWNER'],
    redirectTo: '/chat'
  });
  
  // BLOQUEIO TOTAL: Não renderiza NADA enquanto verifica ou se não tiver acesso
  // O hook já fez o redirecionamento, mas garantimos que nada seja renderizado
  if (isChecking || !hasAccess) {
    return null;
  }
  
  // Todos os hooks devem ser chamados após a verificação de acesso
  const [selectedDate, setSelectedDate] = useState<Date>(new Date());
  const [dateRange, setDateRange] = useState<'7d' | '30d' | '90d'>('30d');
  const [selectedDepartment, setSelectedDepartment] = useState<string | undefined>(undefined);

  const startDate = dateRange === '7d' 
    ? subDays(selectedDate, 7)
    : dateRange === '30d'
    ? subDays(selectedDate, 30)
    : subDays(selectedDate, 90);

  const { data: dashboardData, loading: dashboardLoading } = useDashboardData(MOCK_COMPANY_ID, selectedDate);
  const { data: stressTimeline, loading: timelineLoading } = useStressTimeline(
    MOCK_COMPANY_ID,
    startDate,
    selectedDate,
    'day'
  );
  const { data: heatmapData, loading: heatmapLoading } = useDepartmentHeatmap(MOCK_COMPANY_ID, selectedDate);
  const { data: turnoverData, loading: turnoverLoading } = useTurnoverPrediction(
    MOCK_COMPANY_ID,
    selectedDepartment
  );

  return (
    <div className="flex-1 overflow-y-auto p-6 space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">Dashboard Corporativo</h1>
          <p className="text-muted-foreground mt-1">
            Visão geral do bem-estar emocional da empresa
          </p>
        </div>
        <div className="flex items-center gap-4">
          <div>
            <Label htmlFor="date">Data</Label>
            <Input
              id="date"
              type="date"
              value={format(selectedDate, 'yyyy-MM-dd')}
              onChange={(e) => setSelectedDate(new Date(e.target.value))}
              className="w-40"
            />
          </div>
          <div>
            <Label htmlFor="range">Período</Label>
            <Select value={dateRange} onValueChange={(value) => setDateRange(value as '7d' | '30d' | '90d')}>
              <SelectTrigger id="range" className="w-32">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="7d">7 dias</SelectItem>
                <SelectItem value="30d">30 dias</SelectItem>
                <SelectItem value="90d">90 dias</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>
      </div>

      {/* Overview Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Stress Médio</CardTitle>
            <TrendingUp className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            {dashboardLoading ? (
              <Skeleton className="h-8 w-20" />
            ) : (
              <>
                <div className="text-2xl font-bold">
                  {dashboardData?.averageStressLevel?.toFixed(1) ?? 'N/A'}%
                </div>
                <p className="text-xs text-muted-foreground mt-1">
                  {dashboardData?.averageStressLevel && dashboardData.averageStressLevel > 70
                    ? 'Nível crítico'
                    : dashboardData?.averageStressLevel && dashboardData.averageStressLevel > 50
                    ? 'Nível médio'
                    : 'Nível baixo'}
                </p>
              </>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Usuários Ativos</CardTitle>
            <Users className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            {dashboardLoading ? (
              <Skeleton className="h-8 w-20" />
            ) : (
              <>
                <div className="text-2xl font-bold">
                  {dashboardData?.totalActiveUsers ?? 0}
                </div>
                <p className="text-xs text-muted-foreground mt-1">
                  Usuários no período
                </p>
              </>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Conversas</CardTitle>
            <MessageSquare className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            {dashboardLoading ? (
              <Skeleton className="h-8 w-20" />
            ) : (
              <>
                <div className="text-2xl font-bold">
                  {dashboardData?.totalConversations ?? 0}
                </div>
                <p className="text-xs text-muted-foreground mt-1">
                  Total de conversas
                </p>
              </>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Alertas de Risco</CardTitle>
            <AlertTriangle className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            {dashboardLoading ? (
              <Skeleton className="h-8 w-20" />
            ) : (
              <>
                <div className="text-2xl font-bold">
                  {dashboardData?.riskAlertsCount ?? 0}
                </div>
                <p className="text-xs text-muted-foreground mt-1">
                  Requerem atenção
                </p>
              </>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Main Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <StressSeismograph data={stressTimeline} loading={timelineLoading} />
        <TurnoverPrediction data={turnoverData} loading={turnoverLoading} />
      </div>

      {/* Department Heatmap */}
      <DepartmentHeatmap 
        departments={heatmapData?.departments ?? []} 
        loading={heatmapLoading}
      />

      {/* Impact Analysis */}
      <ImpactAnalysis companyId={MOCK_COMPANY_ID} />
    </div>
  );
}
