'use client';

import { useState, useEffect, useMemo } from 'react';
import { useRequireRole } from '@/hooks/useRequireRole';
import { useAuth } from '@/contexts/AuthContext';
import { 
  useDashboardData, 
  useStressTimeline, 
  useDepartmentHeatmap, 
  useTurnoverPrediction 
} from '@/hooks/use-dashboard';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Skeleton } from '@/components/ui/skeleton';
import { Badge } from '@/components/ui/badge';
import { StressSeismograph } from '@/components/dashboard/stress-seismograph';
import { DepartmentHeatmap } from '@/components/dashboard/department-heatmap';
import { TurnoverPrediction } from '@/components/dashboard/turnover-prediction';
import { ImpactAnalysis } from '@/components/dashboard/impact-analysis';
import { AccountInfoCard } from '@/components/dashboard/account-info-card';
import { CompanyInfoCard } from '@/components/dashboard/company-info-card';
import { ReportsSection } from '@/components/dashboard/reports-section';
import { apiService, Company, UsageInfo } from '@/lib/api';
import { subDays, subMonths, format } from 'date-fns';

export default function DashboardPage() {
  const { hasAccess, isChecking } = useRequireRole({ 
    companyRole: ['HR_MANAGER', 'ADMIN', 'OWNER'],
    redirectTo: '/chat'
  });
  
  const { user } = useAuth();
  const companyId = user?.companyId;
  
  const [company, setCompany] = useState<Company | null>(null);
  const [usageInfo, setUsageInfo] = useState<UsageInfo | null>(null);
  const [timeRange, setTimeRange] = useState<'7d' | '30d' | '90d' | '1y'>('30d');
  const [selectedDepartment, setSelectedDepartment] = useState<string | undefined>(undefined);
  
  // Memoiza a data atual para evitar recriações desnecessárias
  const currentDate = useMemo(() => new Date(), []);
  
  // Dashboard overview
  const { data: dashboardData, loading: dashboardLoading } = useDashboardData(companyId || null, currentDate);
  
  // Stress timeline - memoiza as datas para evitar recriações desnecessárias
  const startDate = useMemo(() => {
    const now = new Date();
    switch (timeRange) {
      case '7d': return subDays(now, 7);
      case '30d': return subDays(now, 30);
      case '90d': return subDays(now, 90);
      case '1y': return subMonths(now, 12);
      default: return subDays(now, 30);
    }
  }, [timeRange]);
  
  const endDate = useMemo(() => new Date(), []); // Data atual - não muda durante a sessão
  const granularity = useMemo(() => timeRange === '1y' ? 'month' : timeRange === '7d' ? 'day' : 'day', [timeRange]);
  
  const { data: stressTimeline, loading: stressLoading } = useStressTimeline(
    companyId || null,
    startDate,
    endDate,
    granularity
  );
  
  // Department heatmap
  const { data: heatmapData, loading: heatmapLoading } = useDepartmentHeatmap(companyId || null, currentDate);
  
  // Turnover prediction
  const { data: turnoverData, loading: turnoverLoading } = useTurnoverPrediction(
    companyId || null,
    selectedDepartment
  );
  
  useEffect(() => {
    if (companyId) {
      loadCompanyInfo();
    }
  }, [companyId]);

  const loadCompanyInfo = async () => {
    if (!companyId) return;
    
    try {
      const [companyData, usage] = await Promise.all([
        apiService.getCompanyById(companyId).catch(() => null),
        apiService.getUsageInfo(companyId).catch(() => null),
      ]);
      setCompany(companyData);
      setUsageInfo(usage);
    } catch (error) {
      console.error('Erro ao carregar informações da empresa:', error);
    }
  };

  if (isChecking || !hasAccess) {
    return null;
  }

  // Calcular métricas principais
  const totalEmployees = dashboardData?.totalActiveUsers ?? 0;
  const totalConversations = dashboardData?.totalConversations ?? 0;
  const totalMessages = dashboardData?.totalMessages ?? 0;
  const riskAlerts = dashboardData?.riskAlertsCount ?? 0;
  const avgStress = dashboardData?.averageStressLevel !== null && dashboardData?.averageStressLevel !== undefined
    ? Math.round((dashboardData.averageStressLevel * 100)) 
    : null;
  
  // Top keywords e triggers
  const topKeywords = dashboardData?.topKeywords 
    ? Object.entries(dashboardData.topKeywords)
        .sort(([, a], [, b]) => (b as number) - (a as number))
        .slice(0, 5)
    : [];
  
  const topTriggers = dashboardData?.topTriggers 
    ? Object.keys(dashboardData.topTriggers).slice(0, 3)
    : [];

  return (
    <div className="flex-1 overflow-y-auto p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">The Corporate Map</h1>
          <p className="text-muted-foreground mt-1">
            Dashboard de gestão emocional em tempo real - Dados agregados por departamento
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Select value={timeRange} onValueChange={(v) => setTimeRange(v as any)}>
            <SelectTrigger className="w-[140px]">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="7d">Últimos 7 dias</SelectItem>
              <SelectItem value="30d">Últimos 30 dias</SelectItem>
              <SelectItem value="90d">Últimos 90 dias</SelectItem>
              <SelectItem value="1y">Último ano</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      {/* Key Metrics */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Usuários Ativos</CardTitle>
          </CardHeader>
          <CardContent>
            {dashboardLoading ? (
              <Skeleton className="h-8 w-20" />
            ) : (
              <>
                <div className="text-2xl font-bold">{totalEmployees}</div>
                <p className="text-xs text-muted-foreground mt-1">
                  {usageInfo ? `${usageInfo.activeUsers} de ${usageInfo.maxUsers} licenças` : 'Total de colaboradores'}
                </p>
              </>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Conversas</CardTitle>
          </CardHeader>
          <CardContent>
            {dashboardLoading ? (
              <Skeleton className="h-8 w-20" />
            ) : (
              <>
                <div className="text-2xl font-bold">{totalConversations}</div>
                <p className="text-xs text-muted-foreground mt-1">
                  {totalMessages} mensagens no período
                </p>
              </>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Nível de Stress Médio</CardTitle>
          </CardHeader>
          <CardContent>
            {dashboardLoading ? (
              <Skeleton className="h-8 w-20" />
            ) : (
              <>
                <div className="text-2xl font-bold">
                  {avgStress !== null ? `${avgStress}%` : 'N/A'}
                </div>
                <p className="text-xs text-muted-foreground mt-1">
                  {avgStress !== null 
                    ? avgStress < 30 ? 'Baixo stress' 
                    : avgStress < 70 ? 'Stress moderado' 
                    : 'Alto stress - Atenção necessária'
                    : 'Sem dados disponíveis'}
                </p>
              </>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Alertas de Risco</CardTitle>
          </CardHeader>
          <CardContent>
            {dashboardLoading ? (
              <Skeleton className="h-8 w-20" />
            ) : (
              <>
                <div className="text-2xl font-bold">{riskAlerts}</div>
                <p className="text-xs text-muted-foreground mt-1">
                  {riskAlerts > 0 ? 'Requer atenção imediata' : 'Nenhum alerta ativo'}
                </p>
              </>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Insights e Palavras-chave */}
      {(topKeywords.length > 0 || topTriggers.length > 0) && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {topKeywords.length > 0 && (
            <Card>
              <CardHeader>
                <CardTitle className="text-sm">Palavras-chave Mais Citadas</CardTitle>
                <CardDescription>Principais temas mencionados nas conversas</CardDescription>
              </CardHeader>
              <CardContent>
                <div className="flex flex-wrap gap-2">
                  {topKeywords.map(([keyword, count]) => (
                    <Badge key={keyword} variant="secondary" className="text-xs">
                      {keyword} ({count})
                    </Badge>
                  ))}
                </div>
              </CardContent>
            </Card>
          )}
          
          {topTriggers.length > 0 && (
            <Card>
              <CardHeader>
                <CardTitle className="text-sm">Principais Gatilhos de Stress</CardTitle>
                <CardDescription>Fatores que mais impactam o bem-estar</CardDescription>
              </CardHeader>
              <CardContent>
                <div className="space-y-2">
                  {topTriggers.map((trigger, index) => (
                    <div key={index} className="text-sm">
                      <span>{trigger}</span>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          )}
        </div>
      )}

      {/* Sismógrafo de Stress */}
      <StressSeismograph data={stressTimeline} loading={stressLoading} />

      {/* Mapa de Calor por Departamento */}
      <DepartmentHeatmap 
        departments={heatmapData?.departments || []} 
        loading={heatmapLoading} 
      />

      {/* Predição de Turnover */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <TurnoverPrediction 
          data={turnoverData} 
          loading={turnoverLoading} 
        />
        
        {/* Análise de Impacto */}
        <ImpactAnalysis companyId={companyId || null} />
      </div>

      {/* Informações da Conta e Empresa */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <AccountInfoCard user={user} loading={!user} />
        <CompanyInfoCard 
          company={company} 
          usageInfo={usageInfo} 
          loading={!companyId} 
        />
      </div>

      {/* Relatórios */}
      {companyId && (
        <ReportsSection companyId={companyId} />
      )}

      {/* Aviso de Privacidade */}
      <Card className="border-primary/20 bg-primary/5">
        <CardHeader>
          <CardTitle className="text-sm">
            Garantia de Privacidade
          </CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground">
            <strong>A sua empresa NÃO sabe quem você é, nem o que você escreve.</strong> Ela recebe apenas dados agregados do setor. 
            Todos os dados individuais são protegidos e nunca expostos no dashboard corporativo. 
            Este sistema garante compliance total com LGPD/GDPR.
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
