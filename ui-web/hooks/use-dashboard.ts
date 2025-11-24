'use client';

import { useState, useEffect } from 'react';
import { apiService, DashboardOverviewDTO, StressTimelineDTO, DepartmentHeatmapDTO, TurnoverPredictionDTO, ImpactAnalysisDTO, DepartmentInsightsDTO } from '@/lib/api';
import { format } from 'date-fns';

export function useDashboardData(companyId: string | null, date?: Date) {
  const [data, setData] = useState<DashboardOverviewDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!companyId) {
      setLoading(false);
      return;
    }

    const fetchData = async () => {
      try {
        setLoading(true);
        setError(null);
        const dateStr = date ? format(date, 'yyyy-MM-dd') : undefined;
        const result = await apiService.getDashboardOverview(companyId, dateStr);
        setData(result);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Erro ao carregar dados do dashboard');
      } finally {
        setLoading(false);
      }
    };

    fetchData();
    
    // Refresh a cada 5 minutos
    const interval = setInterval(fetchData, 5 * 60 * 1000);
    return () => clearInterval(interval);
  }, [companyId, date]);

  return { data, loading, error };
}

export function useStressTimeline(
  companyId: string | null,
  startDate: Date,
  endDate: Date,
  granularity: string = 'day'
) {
  const [data, setData] = useState<StressTimelineDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!companyId) {
      setLoading(false);
      return;
    }

    const fetchData = async () => {
      try {
        setLoading(true);
        setError(null);
        const result = await apiService.getStressTimeline(
          companyId,
          format(startDate, 'yyyy-MM-dd'),
          format(endDate, 'yyyy-MM-dd'),
          granularity
        );
        setData(result);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Erro ao carregar timeline de stress');
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [companyId, startDate, endDate, granularity]);

  return { data, loading, error };
}

export function useDepartmentHeatmap(companyId: string | null, date?: Date) {
  const [data, setData] = useState<DepartmentHeatmapDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!companyId) {
      setLoading(false);
      return;
    }

    const fetchData = async () => {
      try {
        setLoading(true);
        setError(null);
        const dateStr = date ? format(date, 'yyyy-MM-dd') : undefined;
        const result = await apiService.getDepartmentHeatmap(companyId, dateStr);
        setData(result);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Erro ao carregar mapa de calor');
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [companyId, date]);

  return { data, loading, error };
}

export function useTurnoverPrediction(companyId: string | null, departmentId?: string) {
  const [data, setData] = useState<TurnoverPredictionDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!companyId) {
      setLoading(false);
      return;
    }

    const fetchData = async () => {
      try {
        setLoading(true);
        setError(null);
        const result = await apiService.getTurnoverPrediction(companyId, departmentId);
        setData(result);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Erro ao carregar predição de turnover');
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [companyId, departmentId]);

  return { data, loading, error };
}

export function useImpactAnalysis(
  companyId: string | null,
  eventDate: Date,
  eventDescription: string
) {
  const [data, setData] = useState<ImpactAnalysisDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!companyId || !eventDescription) {
      setLoading(false);
      return;
    }

    const fetchData = async () => {
      try {
        setLoading(true);
        setError(null);
        const result = await apiService.getImpactAnalysis(
          companyId,
          format(eventDate, 'yyyy-MM-dd'),
          eventDescription
        );
        setData(result);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Erro ao analisar impacto');
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [companyId, eventDate, eventDescription]);

  return { data, loading, error };
}

export function useDepartmentInsights(departmentId: string | null, date?: Date) {
  const [data, setData] = useState<DepartmentInsightsDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!departmentId) {
      setLoading(false);
      return;
    }

    const fetchData = async () => {
      try {
        setLoading(true);
        setError(null);
        const dateStr = date ? format(date, 'yyyy-MM-dd') : undefined;
        const result = await apiService.getDepartmentInsights(departmentId, dateStr);
        setData(result);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Erro ao carregar insights do departamento');
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [departmentId, date]);

  return { data, loading, error };
}
