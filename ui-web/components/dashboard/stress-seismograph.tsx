'use client';

import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, ReferenceLine } from 'recharts';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { AlertTriangle, TrendingUp } from 'lucide-react';
import { format } from 'date-fns';
import { StressTimelineDTO } from '@/lib/api';

interface StressSeismographProps {
  data: StressTimelineDTO | null;
  loading?: boolean;
}

export function StressSeismograph({ data, loading }: StressSeismographProps) {
  if (loading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Sismógrafo de Stress</CardTitle>
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

  if (!data || !data.points || data.points.length === 0) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Sismógrafo de Stress</CardTitle>
          <CardDescription>Nenhum dado disponível</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="h-[400px] flex items-center justify-center">
            <div className="text-muted-foreground">Nenhum dado disponível para o período selecionado</div>
          </div>
        </CardContent>
      </Card>
    );
  }

  const chartData = data.points.map(point => ({
    timestamp: format(new Date(point.timestamp), 'dd/MM'),
    stress: point.stressLevel ?? 0,
    users: point.activeUsers ?? 0,
    conversations: point.conversations ?? 0,
  }));

  const alertPoints = data.alerts || [];

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <TrendingUp className="w-5 h-5" />
          Sismógrafo de Stress
        </CardTitle>
        <CardDescription>
          Evolução do nível de stress ao longo do tempo
        </CardDescription>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          {alertPoints.length > 0 && (
            <div className="space-y-2">
              {alertPoints.map((alert, index) => (
                <div
                  key={index}
                  className={`flex items-center gap-2 p-2 rounded-md ${
                    alert.type === 'PEAK'
                      ? 'bg-red-50 dark:bg-red-950 border border-red-200 dark:border-red-800'
                      : 'bg-yellow-50 dark:bg-yellow-950 border border-yellow-200 dark:border-yellow-800'
                  }`}
                >
                  <AlertTriangle className={`w-4 h-4 ${
                    alert.type === 'PEAK' ? 'text-red-600 dark:text-red-400' : 'text-yellow-600 dark:text-yellow-400'
                  }`} />
                  <div className="text-sm">
                    <strong>{format(new Date(alert.timestamp), 'dd/MM HH:mm')}:</strong> {alert.message}
                  </div>
                </div>
              ))}
            </div>
          )}
          
          <ResponsiveContainer width="100%" height={400}>
            <LineChart data={chartData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis 
                dataKey="timestamp" 
                tick={{ fontSize: 12 }}
              />
              <YAxis 
                domain={[0, 100]}
                label={{ value: 'Nível de Stress', angle: -90, position: 'insideLeft' }}
              />
              <Tooltip 
                contentStyle={{ backgroundColor: 'hsl(var(--background))', border: '1px solid hsl(var(--border))' }}
                formatter={(value: number) => [`${value.toFixed(1)}%`, 'Stress']}
              />
              <Legend />
              <ReferenceLine y={70} stroke="red" strokeDasharray="3 3" label="Alto Stress" />
              <ReferenceLine y={50} stroke="yellow" strokeDasharray="3 3" label="Médio Stress" />
              <Line 
                type="monotone" 
                dataKey="stress" 
                stroke="hsl(var(--primary))" 
                strokeWidth={2}
                dot={{ r: 4 }}
                activeDot={{ r: 6 }}
                name="Nível de Stress"
              />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </CardContent>
    </Card>
  );
}
