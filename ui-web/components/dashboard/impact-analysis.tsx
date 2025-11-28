'use client';

import { useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { ImpactAnalysisDTO, apiService } from '@/lib/api';
import { format } from 'date-fns';

interface ImpactAnalysisProps {
  companyId: string | null;
}

export function ImpactAnalysis({ companyId }: ImpactAnalysisProps) {
  const [eventDate, setEventDate] = useState(format(new Date(), 'yyyy-MM-dd'));
  const [eventDescription, setEventDescription] = useState('');
  const [data, setData] = useState<ImpactAnalysisDTO | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleAnalyze = async () => {
    if (!eventDescription.trim() || !companyId) return;

    try {
      setLoading(true);
      setError(null);
      const result = await apiService.getImpactAnalysis(
        companyId,
        eventDate,
        eventDescription
      );
      setData(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao analisar impacto');
    } finally {
      setLoading(false);
    }
  };

  if (!companyId) {
    return null;
  }

  const chartData = data ? [
    {
      métrica: 'Stress',
      antes: data.beforeMetrics.averageStressLevel,
      depois: data.afterMetrics.averageStressLevel,
    },
    {
      métrica: 'Moral',
      antes: data.beforeMetrics.moraleScore,
      depois: data.afterMetrics.moraleScore,
    },
    {
      métrica: 'Engajamento',
      antes: data.beforeMetrics.engagementScore,
      depois: data.afterMetrics.engagementScore,
    },
  ] : [];

  return (
    <Card>
      <CardHeader>
        <CardTitle>Análise de Impacto de Decisões</CardTitle>
        <CardDescription>
          Compare métricas antes e depois de um evento ou decisão
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <Label htmlFor="eventDate">Data do Evento</Label>
            <Input
              id="eventDate"
              type="date"
              value={eventDate}
              onChange={(e) => setEventDate(e.target.value)}
            />
          </div>
          <div>
            <Label htmlFor="eventDescription">Descrição do Evento</Label>
            <Input
              id="eventDescription"
              placeholder="Ex: Fim do Home Office"
              value={eventDescription}
              onChange={(e) => setEventDescription(e.target.value)}
            />
          </div>
        </div>

        <Button onClick={handleAnalyze} disabled={!eventDescription.trim() || loading}>
          {loading ? 'Analisando...' : 'Analisar Impacto'}
        </Button>

        {error && (
          <div className="p-4 bg-red-50 dark:bg-red-950 border border-red-200 dark:border-red-800 rounded-lg text-red-600 dark:text-red-400">
            {error}
          </div>
        )}

        {data && (
          <div className="space-y-6">
            <div>
              <h4 className="text-sm font-semibold mb-3">Comparação de Métricas</h4>
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={chartData}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="métrica" />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Bar dataKey="antes" fill="hsl(var(--muted-foreground))" name="Antes" />
                  <Bar dataKey="depois" fill="hsl(var(--primary))" name="Depois" />
                </BarChart>
              </ResponsiveContainer>
            </div>

            <div>
              <h4 className="text-sm font-semibold mb-3">Mudanças Percentuais</h4>
              <div className="grid grid-cols-3 gap-4">
                <div className="p-4 bg-muted rounded-lg">
                  <div className="text-sm text-muted-foreground">Stress</div>
                  <div className="mt-1">
                    <span className="text-lg font-semibold">
                      {data.changes.stressChangePercentage > 0 ? '+' : ''}
                      {data.changes.stressChangePercentage.toFixed(1)}%
                    </span>
                  </div>
                </div>
                <div className="p-4 bg-muted rounded-lg">
                  <div className="text-sm text-muted-foreground">Moral</div>
                  <div className="mt-1">
                    <span className="text-lg font-semibold">
                      {data.changes.moraleChangePercentage > 0 ? '+' : ''}
                      {data.changes.moraleChangePercentage.toFixed(1)}%
                    </span>
                  </div>
                </div>
                <div className="p-4 bg-muted rounded-lg">
                  <div className="text-sm text-muted-foreground">Engajamento</div>
                  <div className="mt-1">
                    <span className="text-lg font-semibold">
                      {data.changes.engagementChangePercentage > 0 ? '+' : ''}
                      {data.changes.engagementChangePercentage.toFixed(1)}%
                    </span>
                  </div>
                </div>
              </div>
            </div>

            {data.overallAssessment && (
              <div className="p-4 bg-muted rounded-lg">
                <h4 className="text-sm font-semibold mb-2">Avaliação Geral</h4>
                <p className="text-sm">{data.overallAssessment}</p>
              </div>
            )}

            {data.departmentImpacts && data.departmentImpacts.length > 0 && (
              <div>
                <h4 className="text-sm font-semibold mb-3">Impacto por Departamento</h4>
                <div className="space-y-2">
                  {data.departmentImpacts.map((impact, index) => (
                    <div key={index} className="p-3 bg-muted rounded-lg">
                      <div className="flex items-center justify-between">
                        <div>
                          <div className="font-medium">
                            {impact.departmentName || `Departamento ${impact.departmentId.slice(0, 8)}`}
                          </div>
                          <div className="text-sm text-muted-foreground">
                            Stress: {impact.stressChangePercentage > 0 ? '+' : ''}
                            {impact.stressChangePercentage.toFixed(1)}%
                          </div>
                        </div>
                        <span className={`text-xs px-2 py-1 rounded-full ${
                          impact.impactLevel === 'HIGH' ? 'bg-red-500/30 text-red-700 dark:text-red-300' :
                          impact.impactLevel === 'MEDIUM' ? 'bg-yellow-500/30 text-yellow-700 dark:text-yellow-300' :
                          'bg-green-500/30 text-green-700 dark:text-green-300'
                        }`}>
                          {impact.impactLevel}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
