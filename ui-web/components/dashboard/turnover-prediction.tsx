'use client';

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { AlertTriangle, TrendingUp, Users } from 'lucide-react';
import { TurnoverPredictionDTO } from '@/lib/api';
import { cn } from '@/lib/utils';

interface TurnoverPredictionProps {
  data: TurnoverPredictionDTO | null;
  loading?: boolean;
}

const getRiskLevelColor = (level: string) => {
  switch (level) {
    case 'CRITICAL':
      return 'text-red-600 dark:text-red-400';
    case 'HIGH':
      return 'text-orange-600 dark:text-orange-400';
    case 'MEDIUM':
      return 'text-yellow-600 dark:text-yellow-400';
    default:
      return 'text-green-600 dark:text-green-400';
  }
};

const getRiskLevelBg = (level: string) => {
  switch (level) {
    case 'CRITICAL':
      return 'bg-red-50 dark:bg-red-950 border-red-200 dark:border-red-800';
    case 'HIGH':
      return 'bg-orange-50 dark:bg-orange-950 border-orange-200 dark:border-orange-800';
    case 'MEDIUM':
      return 'bg-yellow-50 dark:bg-yellow-950 border-yellow-200 dark:border-yellow-800';
    default:
      return 'bg-green-50 dark:bg-green-950 border-green-200 dark:border-green-800';
  }
};

export function TurnoverPrediction({ data, loading }: TurnoverPredictionProps) {
  if (loading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Predição de Turnover</CardTitle>
          <CardDescription>Carregando dados...</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="h-[400px] flex items-center justify-center">
            <div className="text-muted-foreground">Carregando...</div>
          </div>
        </CardContent>
      </Card>
    );
  }

  if (!data) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Predição de Turnover</CardTitle>
          <CardDescription>Nenhum dado disponível</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="h-[400px] flex items-center justify-center">
            <div className="text-muted-foreground">Nenhum dado disponível</div>
          </div>
        </CardContent>
      </Card>
    );
  }

  const chartData = data.probabilities.map(prob => ({
    dias: `${prob.days} dias`,
    probabilidade: prob.probability,
  }));

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <TrendingUp className="w-5 h-5" />
          Predição de Turnover
          {data.departmentName && (
            <span className="text-sm font-normal text-muted-foreground">
              - {data.departmentName}
            </span>
          )}
        </CardTitle>
        <CardDescription>
          Análise de risco de perda de talentos
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        <div className={cn(
          'p-4 rounded-lg border',
          getRiskLevelBg(data.riskLevel)
        )}>
          <div className="flex items-center justify-between">
            <div>
              <div className="text-sm text-muted-foreground">Score de Risco</div>
              <div className={cn('text-3xl font-bold', getRiskLevelColor(data.riskLevel))}>
                {data.riskScore}
              </div>
              <div className={cn('text-sm font-medium mt-1', getRiskLevelColor(data.riskLevel))}>
                {data.riskLevel}
              </div>
            </div>
            <AlertTriangle className={cn('w-12 h-12', getRiskLevelColor(data.riskLevel))} />
          </div>
        </div>

        {data.riskFactors && data.riskFactors.length > 0 && (
          <div>
            <h4 className="text-sm font-semibold mb-3">Fatores de Risco Identificados</h4>
            <div className="space-y-2">
              {data.riskFactors.map((factor, index) => (
                <div key={index} className="flex items-center justify-between p-2 bg-muted rounded">
                  <div>
                    <div className="text-sm font-medium">{factor.description}</div>
                    <div className="text-xs text-muted-foreground">{factor.factor}</div>
                  </div>
                  <div className="text-sm font-semibold">Impacto: {factor.impact}</div>
                </div>
              ))}
            </div>
          </div>
        )}

        <div>
          <h4 className="text-sm font-semibold mb-3">Probabilidade de Turnover</h4>
          <ResponsiveContainer width="100%" height={200}>
            <BarChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="dias" />
              <YAxis domain={[0, 100]} label={{ value: 'Probabilidade (%)', angle: -90, position: 'insideLeft' }} />
              <Tooltip formatter={(value: number) => `${value.toFixed(1)}%`} />
              <Bar dataKey="probabilidade" fill="hsl(var(--primary))" />
            </BarChart>
          </ResponsiveContainer>
        </div>

        {data.recommendations && data.recommendations.length > 0 && (
          <div>
            <h4 className="text-sm font-semibold mb-3 flex items-center gap-2">
              <Users className="w-4 h-4" />
              Recomendações
            </h4>
            <ul className="space-y-2">
              {data.recommendations.map((rec, index) => (
                <li key={index} className="text-sm p-2 bg-muted rounded flex items-start gap-2">
                  <span className="text-primary">•</span>
                  <span>{rec}</span>
                </li>
              ))}
            </ul>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
