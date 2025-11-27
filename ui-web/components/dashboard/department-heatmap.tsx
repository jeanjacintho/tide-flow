'use client';

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { useRouter } from 'next/navigation';
import { DepartmentHeatmapItem } from '@/lib/api';
import { cn } from '@/lib/utils';

interface DepartmentHeatmapProps {
  departments: DepartmentHeatmapItem[];
  loading?: boolean;
}

const getStressColorClasses = (color: string) => {
  switch (color) {
    case 'GREEN':
      return 'bg-green-500/20 border-green-500/50 hover:bg-green-500/30';
    case 'YELLOW':
      return 'bg-yellow-500/20 border-yellow-500/50 hover:bg-yellow-500/30';
    case 'ORANGE':
      return 'bg-orange-500/20 border-orange-500/50 hover:bg-orange-500/30';
    case 'RED':
      return 'bg-red-500/20 border-red-500/50 hover:bg-red-500/30';
    default:
      return 'bg-gray-500/20 border-gray-500/50 hover:bg-gray-500/30';
  }
};

const getStressLabel = (color: string) => {
  switch (color) {
    case 'GREEN':
      return 'Baixo';
    case 'YELLOW':
      return 'Médio';
    case 'ORANGE':
      return 'Alto';
    case 'RED':
      return 'Crítico';
    default:
      return 'Indefinido';
  }
};

export function DepartmentHeatmap({ departments, loading }: DepartmentHeatmapProps) {
  const router = useRouter();

  if (loading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Mapa de Calor por Departamento</CardTitle>
          <CardDescription>Carregando dados...</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {[1, 2, 3].map((i) => (
              <div key={i} className="h-32 bg-muted animate-pulse rounded-lg" />
            ))}
          </div>
        </CardContent>
      </Card>
    );
  }

  if (!departments || departments.length === 0) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Mapa de Calor por Departamento</CardTitle>
          <CardDescription>Nenhum departamento encontrado</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="text-center text-muted-foreground py-8">
            Nenhum dado disponível
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Mapa de Calor por Departamento</CardTitle>
        <CardDescription>
          Níveis de stress por setor - Clique para ver detalhes
        </CardDescription>
      </CardHeader>
      <CardContent>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {departments.map((dept) => (
            <Card
              key={dept.departmentId}
              className={cn(
                'cursor-pointer transition-all hover:shadow-lg',
                getStressColorClasses(dept.stressColor)
              )}
              onClick={() => router.push(`/dashboard/department/${dept.departmentId}`)}
            >
              <CardHeader className="pb-3">
                <div className="flex items-center justify-between">
                  <CardTitle className="text-lg">
                    {dept.departmentName || `Departamento ${dept.departmentId.slice(0, 8)}`}
                  </CardTitle>
                  <span className={cn(
                    'text-xs px-2 py-1 rounded-full font-medium',
                    dept.stressColor === 'GREEN' && 'bg-green-500/30 text-green-700 dark:text-green-300',
                    dept.stressColor === 'YELLOW' && 'bg-yellow-500/30 text-yellow-700 dark:text-yellow-300',
                    dept.stressColor === 'ORANGE' && 'bg-orange-500/30 text-orange-700 dark:text-orange-300',
                    dept.stressColor === 'RED' && 'bg-red-500/30 text-red-700 dark:text-red-300'
                  )}>
                    {getStressLabel(dept.stressColor)}
                  </span>
                </div>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-sm text-muted-foreground">Stress</span>
                    <span className="text-lg font-semibold">
                      {dept.stressLevel?.toFixed(1) ?? 'N/D'}%
                    </span>
                  </div>
                  
                  <div className="grid grid-cols-3 gap-2 text-sm">
                    <div>
                      <span className="text-muted-foreground">{dept.activeUsers ?? 0}</span>
                    </div>
                    <div>
                      <span className="text-muted-foreground">{dept.conversations ?? 0}</span>
                    </div>
                    <div>
                      <span className="text-muted-foreground">{dept.riskAlerts ?? 0}</span>
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}
