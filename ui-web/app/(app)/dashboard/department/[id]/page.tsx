'use client';

import { useParams } from 'next/navigation';
import { useDepartmentInsights } from '@/hooks/use-dashboard';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Skeleton } from '@/components/ui/skeleton';
import { format } from 'date-fns';
import { useState } from 'react';
import { ArrowLeft, Users, MessageSquare, AlertTriangle, TrendingUp } from 'lucide-react';
import { Button } from '@/components/ui/button';
import Link from 'next/link';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

export default function DepartmentInsightsPage() {
  const params = useParams();
  const departmentId = params.id as string;
  const [selectedDate, setSelectedDate] = useState<Date>(new Date());
  
  const { data, loading, error } = useDepartmentInsights(departmentId, selectedDate);

  if (loading) {
    return (
      <div className="flex-1 overflow-y-auto p-6 space-y-6">
        <Skeleton className="h-10 w-64" />
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex-1 overflow-y-auto p-6">
        <Card>
          <CardContent className="p-6">
            <div className="text-center text-red-600 dark:text-red-400">
              {error}
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (!data) {
    return (
      <div className="flex-1 overflow-y-auto p-6">
        <Card>
          <CardContent className="p-6">
            <div className="text-center text-muted-foreground">
              Nenhum dado disponível para este departamento
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  const getStressColorClasses = (color: string) => {
    switch (color) {
      case 'GREEN':
        return 'bg-green-500/20 border-green-500/50 text-green-700 dark:text-green-300';
      case 'YELLOW':
        return 'bg-yellow-500/20 border-yellow-500/50 text-yellow-700 dark:text-yellow-300';
      case 'ORANGE':
        return 'bg-orange-500/20 border-orange-500/50 text-orange-700 dark:text-orange-300';
      case 'RED':
        return 'bg-red-500/20 border-red-500/50 text-red-700 dark:text-red-300';
      default:
        return 'bg-gray-500/20 border-gray-500/50 text-gray-700 dark:text-gray-300';
    }
  };

  const keywordsData = data.topKeywords
    ? Object.entries(data.topKeywords)
        .slice(0, 10)
        .map(([keyword, count]) => ({
          keyword,
          count: typeof count === 'number' ? count : 0,
        }))
        .sort((a, b) => b.count - a.count)
    : [];

  return (
    <div className="flex-1 overflow-y-auto p-6 space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" asChild>
          <Link href="/dashboard">
            <ArrowLeft className="w-4 h-4" />
          </Link>
        </Button>
        <div>
          <h1 className="text-3xl font-bold">
            {data.departmentName || `Departamento ${data.departmentId.slice(0, 8)}`}
          </h1>
          <p className="text-muted-foreground mt-1">Insights e análises detalhadas</p>
        </div>
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
      </div>

      {/* Overview Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <Card className={getStressColorClasses(data.stressColor)}>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Nível de Stress</CardTitle>
            <TrendingUp className="h-4 w-4" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {data.stressLevel?.toFixed(1) ?? 'N/A'}%
            </div>
            <p className="text-xs mt-1 opacity-80">
              {data.stressColor === 'GREEN' && 'Baixo'}
              {data.stressColor === 'YELLOW' && 'Médio'}
              {data.stressColor === 'ORANGE' && 'Alto'}
              {data.stressColor === 'RED' && 'Crítico'}
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Usuários Ativos</CardTitle>
            <Users className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {data.activeUsers ?? 0}
            </div>
            <p className="text-xs text-muted-foreground mt-1">
              No período selecionado
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Conversas</CardTitle>
            <MessageSquare className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {data.conversations ?? 0}
            </div>
            <p className="text-xs text-muted-foreground mt-1">
              Total de conversas
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Alertas de Risco</CardTitle>
            <AlertTriangle className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {data.riskAlerts ?? 0}
            </div>
            <p className="text-xs text-muted-foreground mt-1">
              Requerem atenção
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Keywords Chart */}
      {keywordsData.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Palavras-chave Mais Citadas</CardTitle>
            <CardDescription>
              Palavras mais mencionadas nas conversas (anônimas)
            </CardDescription>
          </CardHeader>
          <CardContent>
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={keywordsData}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis 
                  dataKey="keyword" 
                  angle={-45}
                  textAnchor="end"
                  height={100}
                  tick={{ fontSize: 12 }}
                />
                <YAxis />
                <Tooltip />
                <Bar dataKey="count" fill="hsl(var(--primary))" />
              </BarChart>
            </ResponsiveContainer>
          </CardContent>
        </Card>
      )}

      {/* Top Triggers */}
      {data.topTriggers && Object.keys(data.topTriggers).length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Triggers Mais Comuns</CardTitle>
            <CardDescription>
              Gatilhos emocionais mais frequentes no departamento
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-2">
              {Object.entries(data.topTriggers).slice(0, 10).map(([trigger, info], index) => (
                <div key={index} className="p-3 bg-muted rounded-lg">
                  <div className="flex items-center justify-between">
                    <div>
                      <div className="font-medium">{trigger}</div>
                      {typeof info === 'object' && info !== null && 'frequency' in info && (
                        <div className="text-sm text-muted-foreground">
                          Frequência: {String(info.frequency)}
                        </div>
                      )}
                    </div>
                    {typeof info === 'object' && info !== null && 'averageIntensity' in info && (
                      <div className="text-sm font-semibold">
                        Intensidade: {String(info.averageIntensity).slice(0, 4)}
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
