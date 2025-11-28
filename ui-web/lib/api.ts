const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
  role: 'USER' | 'ADMIN';
}

export interface LoginResponse {
  token: string;
}

export interface User {
  id: string;
  name: string;
  email: string;
  phone?: string;
  avatarUrl?: string;
  trustedEmail?: string;
  city?: string;
  state?: string;
  companyId?: string;
  departmentId?: string;
  systemRole?: string;
  companyRole?: string;
  createdAt: string;
  updatedAt: string;
}

class ApiService {
  private getAuthToken(): string | null {
    if (typeof window === 'undefined') return null;
    return localStorage.getItem('auth_token');
  }

  private async request<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<T> {
    const token = this.getAuthToken();
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      ...(options.headers as Record<string, string>),
    };

    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      ...options,
      headers: headers as HeadersInit,
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || `HTTP error! status: ${response.status}`);
    }

    const contentType = response.headers.get('content-type');
    const contentLength = response.headers.get('content-length');

    if (
      response.status === 204 ||
      contentLength === '0' ||
      !contentType?.includes('application/json')
    ) {
      return {} as T;
    }

    const text = await response.text();
    if (!text || text.trim() === '') {
      return {} as T;
    }

    try {
      return JSON.parse(text) as T;
    } catch (error) {

      return {} as T;
    }
  }

  async login(credentials: LoginRequest): Promise<LoginResponse> {
    return this.request<LoginResponse>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({
        username: credentials.username,
        password: credentials.password,
      }),
    });
  }

  async register(data: RegisterRequest): Promise<void> {
    return this.request<void>('/auth/register', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async getCurrentUser(): Promise<User> {
    const token = this.getAuthToken();
    if (!token) {
      throw new Error('No authentication token found');
    }

    try {
      const userId = this.decodeTokenUserId(token);
      return this.request<User>(`/users/${userId}`);
    } catch (error) {
      if (error instanceof Error) {
        throw error;
      }
      throw new Error('Failed to get current user');
    }
  }

  private decodeTokenUserId(token: string): string {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const userId = payload.user_id;
      if (!userId) {
        throw new Error('User ID not found in token');
      }
      return userId;
    } catch {
      throw new Error('Invalid token format');
    }
  }

  async sendMessage(message: string, conversationId?: string, userId?: string): Promise<ConversationResponse> {
    if (!userId) {
      throw new Error('User ID é obrigatório para enviar mensagens');
    }

    const AI_SERVICE_URL = process.env.NEXT_PUBLIC_AI_SERVICE_URL || 'http://localhost:8082';

    try {
      const response = await fetch(`${AI_SERVICE_URL}/api/conversations`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          message,
          conversationId: conversationId || null,
          userId: userId,
        }),
      });

      if (!response.ok) {
        let errorMessage = `Erro ao enviar mensagem (status: ${response.status})`;
        try {
          const errorText = await response.text();
          if (errorText) {
            errorMessage = errorText;
          }
        } catch {

        }
        throw new Error(errorMessage);
      }

      return response.json();
    } catch (error) {
      if (error instanceof TypeError && error.message.includes('fetch')) {
        throw new Error('Não foi possível conectar ao servidor. Verifique se o serviço está rodando.');
      }
      throw error;
    }
  }

  async getConversationHistory(conversationId: string, userId: string): Promise<ConversationHistoryResponse> {
    const AI_SERVICE_URL = process.env.NEXT_PUBLIC_AI_SERVICE_URL || 'http://localhost:8082';

    try {
      const response = await fetch(`${AI_SERVICE_URL}/api/conversations/${conversationId}`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          'X-User-Id': userId,
        },
      });

      if (!response.ok) {
        let errorMessage = `Erro ao buscar histórico (status: ${response.status})`;
        try {
          const errorText = await response.text();
          if (errorText) {
            errorMessage = errorText;
          }
        } catch {

        }
        throw new Error(errorMessage);
      }

      return response.json();
    } catch (error) {
      if (error instanceof TypeError && error.message.includes('fetch')) {
        throw new Error('Não foi possível conectar ao servidor. Verifique se o serviço está rodando.');
      }
      throw error;
    }
  }

  async updateUser(userId: string, data: Partial<User>): Promise<User> {
    const token = this.getAuthToken();
    if (!token) {
      throw new Error('No authentication token found');
    }

    try {
      const response = await fetch(`${API_BASE_URL}/users/${userId}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify(data),
      });

      if (!response.ok) {
        const error = await response.text();
        throw new Error(error || `HTTP error! status: ${response.status}`);
      }

      return response.json();
    } catch (error) {
      if (error instanceof TypeError && error.message.includes('fetch')) {
        throw new Error('Não foi possível conectar ao servidor. Verifique se o serviço está rodando.');
      }
      throw error;
    }
  }

  async getUserConversations(userId: string): Promise<ConversationSummaryResponse[]> {
    const AI_SERVICE_URL = process.env.NEXT_PUBLIC_AI_SERVICE_URL || 'http://localhost:8082';

    try {
      const response = await fetch(`${AI_SERVICE_URL}/api/conversations/user/${userId}`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
        },
      });

      if (!response.ok) {
        let errorMessage = `Erro ao buscar conversas (status: ${response.status})`;
        try {
          const errorText = await response.text();
          if (errorText) {
            errorMessage = errorText;
          }
        } catch {

        }
        throw new Error(errorMessage);
      }

      return response.json();
    } catch (error) {
      if (error instanceof TypeError && error.message.includes('fetch')) {
        throw new Error('Não foi possível conectar ao servidor. Verifique se o serviço está rodando.');
      }
      throw error;
    }
  }

  async getDashboardOverview(companyId: string, date?: string): Promise<DashboardOverviewDTO> {
    const AI_SERVICE_URL = process.env.NEXT_PUBLIC_AI_SERVICE_URL || 'http://localhost:8082';
    const params = date ? `?date=${date}` : '';
    const token = this.getAuthToken();

    try {
      const response = await fetch(`${AI_SERVICE_URL}/api/corporate/dashboard/${companyId}${params}`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          ...(token && { 'Authorization': `Bearer ${token}` }),
        },
      });

      if (response.status === 404) {

        return {
          companyId,
          date: date || new Date().toISOString().split('T')[0],
          averageStressLevel: null,
          averageEmotionalIntensity: null,
          totalActiveUsers: null,
          totalConversations: null,
          totalMessages: null,
          riskAlertsCount: null,
          departmentBreakdown: {},
          topKeywords: {},
          topTriggers: {},
        };
      }

      if (!response.ok) {
        const error = await response.text();
        throw new Error(error || `HTTP error! status: ${response.status}`);
      }

      return response.json();
    } catch (error) {

      console.warn('Erro ao buscar dashboard overview:', error);
      return {
        companyId,
        date: date || new Date().toISOString().split('T')[0],
        averageStressLevel: null,
        averageEmotionalIntensity: null,
        totalActiveUsers: null,
        totalConversations: null,
        totalMessages: null,
        riskAlertsCount: null,
        departmentBreakdown: {},
        topKeywords: {},
        topTriggers: {},
      };
    }
  }

  async getStressTimeline(
    companyId: string,
    startDate: string,
    endDate: string,
    granularity: string = 'day'
  ): Promise<StressTimelineDTO> {
    const AI_SERVICE_URL = process.env.NEXT_PUBLIC_AI_SERVICE_URL || 'http://localhost:8082';
    const token = this.getAuthToken();

    try {
      const response = await fetch(
        `${AI_SERVICE_URL}/api/corporate/stress-timeline/${companyId}?startDate=${startDate}&endDate=${endDate}&granularity=${granularity}`,
        {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json',
            ...(token && { 'Authorization': `Bearer ${token}` }),
          },
        }
      );

      if (response.status === 404) {
        return {
          companyId,
          startDate,
          endDate,
          granularity,
          points: [],
          alerts: [],
        };
      }

      if (!response.ok) {
        const error = await response.text();
        throw new Error(error || `HTTP error! status: ${response.status}`);
      }

      return response.json();
    } catch (error) {
      console.warn('Erro ao buscar stress timeline:', error);
      return {
        companyId,
        startDate,
        endDate,
        granularity,
        points: [],
        alerts: [],
      };
    }
  }

  async getDepartmentHeatmap(companyId: string, date?: string): Promise<DepartmentHeatmapDTO> {
    const AI_SERVICE_URL = process.env.NEXT_PUBLIC_AI_SERVICE_URL || 'http://localhost:8082';
    const params = date ? `?date=${date}` : '';
    const token = this.getAuthToken();

    try {
      const response = await fetch(`${AI_SERVICE_URL}/api/corporate/department-heatmap/${companyId}${params}`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          ...(token && { 'Authorization': `Bearer ${token}` }),
        },
      });

      if (response.status === 404) {
        return {
          companyId,
          date: date || new Date().toISOString().split('T')[0],
          departments: [],
        };
      }

      if (!response.ok) {
        const error = await response.text();
        throw new Error(error || `HTTP error! status: ${response.status}`);
      }

      return response.json();
    } catch (error) {
      console.warn('Erro ao buscar department heatmap:', error);
      return {
        companyId,
        date: date || new Date().toISOString().split('T')[0],
        departments: [],
      };
    }
  }

  async getTurnoverPrediction(companyId: string, departmentId?: string): Promise<TurnoverPredictionDTO | null> {
    const AI_SERVICE_URL = process.env.NEXT_PUBLIC_AI_SERVICE_URL || 'http://localhost:8082';
    const params = departmentId ? `?departmentId=${departmentId}` : '';
    const token = this.getAuthToken();

    try {
      const response = await fetch(`${AI_SERVICE_URL}/api/corporate/turnover-prediction/${companyId}${params}`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          ...(token && { 'Authorization': `Bearer ${token}` }),
        },
      });

      if (response.status === 404) {
        return null;
      }

      if (!response.ok) {
        const error = await response.text();
        throw new Error(error || `HTTP error! status: ${response.status}`);
      }

      return response.json();
    } catch (error) {
      console.warn('Erro ao buscar turnover prediction:', error);
      return null;
    }
  }

  async getImpactAnalysis(
    companyId: string,
    eventDate: string,
    eventDescription: string
  ): Promise<ImpactAnalysisDTO> {
    const AI_SERVICE_URL = process.env.NEXT_PUBLIC_AI_SERVICE_URL || 'http://localhost:8082';
    const token = this.getAuthToken();

    const response = await fetch(
      `${AI_SERVICE_URL}/api/corporate/impact-analysis/${companyId}?eventDate=${eventDate}&eventDescription=${encodeURIComponent(eventDescription)}`,
      {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          ...(token && { 'Authorization': `Bearer ${token}` }),
        },
      }
    );

    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || `HTTP error! status: ${response.status}`);
    }

    return response.json();
  }

  async getDepartmentInsights(departmentId: string, date?: string): Promise<DepartmentInsightsDTO> {
    const AI_SERVICE_URL = process.env.NEXT_PUBLIC_AI_SERVICE_URL || 'http://localhost:8082';
    const params = date ? `?date=${date}` : '';
    const token = this.getAuthToken();

    const response = await fetch(`${AI_SERVICE_URL}/api/corporate/department/${departmentId}/insights${params}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        ...(token && { 'Authorization': `Bearer ${token}` }),
      },
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || `HTTP error! status: ${response.status}`);
    }

    return response.json();
  }

  async generateReport(request: ReportGenerationRequest): Promise<CorporateReportResponse> {
    const AI_SERVICE_URL = process.env.NEXT_PUBLIC_AI_SERVICE_URL || 'http://localhost:8082';
    const token = this.getAuthToken();

    const response = await fetch(`${AI_SERVICE_URL}/api/corporate/reports/generate`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token && { 'Authorization': `Bearer ${token}` }),
      },
      body: JSON.stringify(request),
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || `HTTP error! status: ${response.status}`);
    }

    return response.json();
  }

  async getReport(reportId: string): Promise<CorporateReportResponse> {
    const AI_SERVICE_URL = process.env.NEXT_PUBLIC_AI_SERVICE_URL || 'http://localhost:8082';
    const token = this.getAuthToken();

    const response = await fetch(`${AI_SERVICE_URL}/api/corporate/reports/${reportId}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        ...(token && { 'Authorization': `Bearer ${token}` }),
      },
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || `HTTP error! status: ${response.status}`);
    }

    return response.json();
  }

  async listReports(
    companyId: string,
    reportType?: string,
    status?: string,
    page: number = 0,
    size: number = 20
  ): Promise<ReportListResponse> {
    const AI_SERVICE_URL = process.env.NEXT_PUBLIC_AI_SERVICE_URL || 'http://localhost:8082';
    const token = this.getAuthToken();

    const params = new URLSearchParams({
      companyId,
      page: page.toString(),
      size: size.toString(),
    });

    if (reportType) params.append('reportType', reportType);
    if (status) params.append('status', status);

    const response = await fetch(`${AI_SERVICE_URL}/api/corporate/reports?${params.toString()}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        ...(token && { 'Authorization': `Bearer ${token}` }),
      },
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || `HTTP error! status: ${response.status}`);
    }

    return response.json();
  }

  async getLatestReport(companyId: string, reportType?: string): Promise<CorporateReportResponse | null> {
    const AI_SERVICE_URL = process.env.NEXT_PUBLIC_AI_SERVICE_URL || 'http://localhost:8082';
    const token = this.getAuthToken();

    try {
      const params = reportType ? `?reportType=${reportType}` : '';
      const response = await fetch(`${AI_SERVICE_URL}/api/corporate/reports/company/${companyId}/latest${params}`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          ...(token && { 'Authorization': `Bearer ${token}` }),
        },
      });

      if (response.status === 404) {
        return null;
      }

      if (!response.ok) {

        return null;
      }

      return response.json();
    } catch (error) {

      return null;
    }
  }

  async listReports(
    companyId: string,
    reportType?: string,
    status?: string,
    page: number = 0,
    size: number = 20
  ): Promise<ReportListResponse> {
    const AI_SERVICE_URL = process.env.NEXT_PUBLIC_AI_SERVICE_URL || 'http://localhost:8082';
    const token = this.getAuthToken();

    try {
      const params = new URLSearchParams({
        companyId,
        page: page.toString(),
        size: size.toString(),
      });

      if (reportType) params.append('reportType', reportType);
      if (status) params.append('status', status);

      const response = await fetch(`${AI_SERVICE_URL}/api/corporate/reports?${params.toString()}`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          ...(token && { 'Authorization': `Bearer ${token}` }),
        },
      });

      if (response.status === 404) {
        return {
          reports: [],
          totalElements: 0,
          totalPages: 0,
          currentPage: 0,
          pageSize: size,
        };
      }

      if (!response.ok) {
        return {
          reports: [],
          totalElements: 0,
          totalPages: 0,
          currentPage: 0,
          pageSize: size,
        };
      }

      return response.json();
    } catch (error) {

      return {
        reports: [],
        totalElements: 0,
        totalPages: 0,
        currentPage: 0,
        pageSize: size,
      };
    }
  }

  async deleteReport(reportId: string): Promise<void> {
    const AI_SERVICE_URL = process.env.NEXT_PUBLIC_AI_SERVICE_URL || 'http://localhost:8082';
    const token = this.getAuthToken();

    const response = await fetch(`${AI_SERVICE_URL}/api/corporate/reports/${reportId}`, {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
        ...(token && { 'Authorization': `Bearer ${token}` }),
      },
    });

    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || `HTTP error! status: ${response.status}`);
    }
  }

  async getAllCompanies(): Promise<Company[]> {
    return this.request<Company[]>('/api/companies', {
      method: 'GET',
    });
  }

  async getCompanyById(id: string): Promise<Company> {
    return this.request<Company>(`/api/companies/${id}`, {
      method: 'GET',
    });
  }

  async createCompany(data: CreateCompanyRequest): Promise<Company> {
    return this.request<Company>('/api/companies', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async updateCompany(id: string, data: CreateCompanyRequest): Promise<Company> {
    return this.request<Company>(`/api/companies/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  }

  async getSubscription(companyId: string): Promise<SubscriptionResponse> {
    return this.request<SubscriptionResponse>(`/api/subscriptions/companies/${companyId}`, {
      method: 'GET',
    });
  }

  async createCheckoutSession(companyId: string, successUrl?: string, cancelUrl?: string): Promise<{ checkoutUrl: string; sessionId: string | null }> {
    return this.request<{ checkoutUrl: string; sessionId: string | null }>('/api/subscriptions/checkout-session', {
      method: 'POST',
      body: JSON.stringify({
        companyId,
        successUrl,
        cancelUrl,
      }),
    });
  }

  async registerCompany(data: RegisterCompanyRequest): Promise<Company> {
    return this.request<Company>('/api/public/register-company', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async getCompanyDepartments(companyId: string): Promise<Department[]> {
    return this.request<Department[]>(`/api/companies/${companyId}/departments`, {
      method: 'GET',
    });
  }

  async createDepartment(companyId: string, data: CreateDepartmentRequest): Promise<Department> {
    return this.request<Department>(`/api/companies/${companyId}/departments`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async updateDepartment(departmentId: string, data: CreateDepartmentRequest): Promise<Department> {
    return this.request<Department>(`/api/departments/${departmentId}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  }

  async deleteDepartment(departmentId: string): Promise<void> {
    return this.request<void>(`/api/departments/${departmentId}`, {
      method: 'DELETE',
    });
  }

  async getCompanyUsers(companyId: string): Promise<User[]> {
    return this.request<User[]>(`/api/companies/${companyId}/users`, {
      method: 'GET',
    });
  }

  async inviteUser(companyId: string, data: InviteUserRequest): Promise<InviteUserResponse> {
    return this.request<InviteUserResponse>(`/api/companies/${companyId}/invite`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async createCompanyUser(companyId: string, data: CreateCompanyUserRequest): Promise<User> {
    return this.request<User>(`/api/companies/${companyId}/users`, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async deleteUser(userId: string): Promise<void> {
    return this.request<void>(`/users/${userId}`, {
      method: 'DELETE',
    });
  }

  async getUsageInfo(companyId: string): Promise<UsageInfo> {
    return this.request<UsageInfo>(`/api/subscriptions/companies/${companyId}/usage`, {
      method: 'GET',
    });
  }

  async getPaymentHistory(companyId: string, page: number = 0, size: number = 20): Promise<{
    content: PaymentHistory[];
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
  }> {
    return this.request<{
      content: PaymentHistory[];
      totalElements: number;
      totalPages: number;
      number: number;
      size: number;
    }>(`/api/subscriptions/companies/${companyId}/payments?page=${page}&size=${size}`, {
      method: 'GET',
    });
  }
}

export interface UsageInfo {
  activeUsers: number;
  maxUsers: number;
  atLimit: boolean;
  remainingSlots: number;
}

export interface PaymentHistory {
  id: string;
  companyId: string;
  subscriptionId: string;
  amount: number;
  currency: string;
  status: 'PENDING' | 'SUCCEEDED' | 'FAILED' | 'REFUNDED' | 'PARTIALLY_REFUNDED';
  paymentDate: string;
  billingPeriodStart?: string;
  billingPeriodEnd?: string;
  stripeInvoiceId?: string;
  stripePaymentIntentId?: string;
  invoiceNumber?: string;
  description?: string;
  createdAt: string;
}

export interface ConversationResponse {
  response: string;
  conversationId: string;
  isComplete: boolean;
  emotionalAnalysis?: {
    emotion?: string;
    intensity?: number;
    sentiment?: string;
  };
}

export interface Message {
  id: string;
  role: 'USER' | 'ASSISTANT';
  content: string;
  createdAt: string;
  sequenceNumber: number;
}

export interface ConversationHistoryResponse {
  conversationId: string;
  userId: string;
  createdAt: string;
  updatedAt: string;
  messages: Message[];
}

export interface ConversationSummaryResponse {
  conversationId: string;
  userId: string;
  createdAt: string;
  updatedAt: string;
  messageCount: number;
  lastMessagePreview: string;
}

export const apiService = new ApiService();

export interface DashboardOverviewDTO {
  companyId: string;
  date: string;
  averageStressLevel: number | null;
  averageEmotionalIntensity: number | null;
  totalActiveUsers: number | null;
  totalConversations: number | null;
  totalMessages: number | null;
  riskAlertsCount: number | null;
  departmentBreakdown: Record<string, any> | null;
  topKeywords: Record<string, number> | null;
  topTriggers: Record<string, any> | null;
}

export interface StressTimelineDTO {
  companyId: string;
  startDate: string;
  endDate: string;
  granularity: string;
  points: StressTimelinePoint[];
  alerts: StressAlert[];
}

export interface StressTimelinePoint {
  timestamp: string;
  stressLevel: number | null;
  activeUsers: number | null;
  conversations: number | null;
}

export interface StressAlert {
  timestamp: string;
  type: string;
  message: string;
  stressLevel: number | null;
  changePercentage: number | null;
}

export interface DepartmentHeatmapDTO {
  companyId: string;
  date: string;
  departments: DepartmentHeatmapItem[];
}

export interface DepartmentHeatmapItem {
  departmentId: string;
  departmentName: string | null;
  stressLevel: number | null;
  stressColor: string;
  activeUsers: number | null;
  conversations: number | null;
  riskAlerts: number | null;
  topKeywords: Record<string, any> | null;
  topTriggers: Record<string, any> | null;
}

export interface TurnoverPredictionDTO {
  companyId: string;
  departmentId: string | null;
  departmentName: string | null;
  riskScore: number;
  riskLevel: string;
  probabilities: TurnoverProbability[];
  riskFactors: RiskFactor[];
  recommendations: string[];
}

export interface TurnoverProbability {
  days: number;
  probability: number;
}

export interface RiskFactor {
  factor: string;
  description: string;
  impact: number;
  weight: number;
}

export interface ImpactAnalysisDTO {
  companyId: string;
  eventDate: string;
  eventDescription: string;
  beforeMetrics: ImpactMetrics;
  afterMetrics: ImpactMetrics;
  changes: ImpactChanges;
  departmentImpacts: DepartmentImpact[];
  overallAssessment: string;
}

export interface ImpactMetrics {
  averageStressLevel: number;
  averageEmotionalIntensity: number;
  moraleScore: number;
  engagementScore: number;
  totalConversations: number;
  activeUsers: number;
}

export interface ImpactChanges {
  stressChangePercentage: number;
  moraleChangePercentage: number;
  engagementChangePercentage: number;
  trend: string;
}

export interface DepartmentImpact {
  departmentId: string;
  departmentName: string | null;
  stressChangePercentage: number;
  moraleChangePercentage: number;
  impactLevel: string;
}

export interface DepartmentInsightsDTO {
  departmentId: string;
  departmentName: string | null;
  stressLevel: number | null;
  stressColor: string;
  activeUsers: number | null;
  conversations: number | null;
  riskAlerts: number | null;
  topKeywords: Record<string, any> | null;
  topTriggers: Record<string, any> | null;
}

export interface Company {
  id: string;
  name: string;
  domain?: string;
  subscriptionPlan: string;
  maxEmployees: number;
  status: string;
  billingEmail?: string;
  billingAddress?: string;
  taxId?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCompanyRequest {
  name: string;
  domain?: string;
  billingEmail?: string;
  billingAddress?: string;
  taxId?: string;
}

export interface SubscriptionResponse {
  id: string;
  companyId: string;
  planType: 'FREE' | 'ENTERPRISE';
  pricePerUser: number;
  totalUsers: number;
  billingCycle: string;
  nextBillingDate: string;
  status: string;
  monthlyBill: number;
}

export interface RegisterCompanyRequest {
  companyName: string;
  companyDomain?: string;
  ownerName: string;
  ownerEmail: string;
  password: string;
}

export interface CreateCompanyUserRequest {
  name: string;
  username?: string;
  email: string;
  password: string;
  departmentId: string;
  employeeId?: string;
  phone?: string;
  city?: string;
  state?: string;
}

export interface Department {
  id: string;
  companyId: string;
  name: string;
  description?: string;
  createdAt: string;
}

export interface CreateDepartmentRequest {
  name: string;
  description?: string;
}

export interface InviteUserRequest {
  name: string;
  username?: string;
  email: string;
  departmentId: string;
  employeeId?: string;
}

export interface InviteUserResponse {
  userId: string;
  username: string;
  temporaryPassword: string;
  message: string;
}

export interface ReportGenerationRequest {
  companyId: string;
  departmentId?: string;
  reportType: 'STRESS_TIMELINE' | 'DEPARTMENT_HEATMAP' | 'TURNOVER_PREDICTION' | 'IMPACT_ANALYSIS' | 'COMPREHENSIVE' | 'CUSTOM';
  periodStart?: string;
  periodEnd?: string;
  title?: string;
  includeSections?: string[];
  generateInsights?: boolean;
  generateRecommendations?: boolean;
  customPrompt?: string;
  eventDescription?: string;
  eventDate?: string;
}

export interface CorporateReportResponse {
  id: string;
  companyId: string;
  departmentId?: string;
  reportType: string;
  reportDate: string;
  periodStart: string;
  periodEnd: string;
  status: 'PENDING' | 'GENERATING' | 'COMPLETED' | 'FAILED' | 'ARCHIVED';
  title: string;
  executiveSummary?: string;
  insights?: Record<string, any>;
  metrics?: Record<string, any>;
  recommendations?: string;
  generatedByAi: boolean;
  aiModelVersion?: string;
  generationTimeMs?: number;
  sections?: ReportSection[];
  createdAt: string;
  updatedAt: string;
  generatedAt?: string;
}

export interface ReportSection {
  id: string;
  sectionType: string;
  sectionOrder: number;
  title: string;
  content?: string;
  data?: Record<string, any>;
  visualizationConfig?: Record<string, any>;
}

export interface ReportListResponse {
  reports: ReportSummary[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
}

export interface ReportSummary {
  id: string;
  companyId: string;
  departmentId?: string;
  reportType: string;
  reportDate: string;
  periodStart: string;
  periodEnd: string;
  status: string;
  title: string;
  executiveSummary?: string;
  generatedByAi: boolean;
  createdAt: string;
  generatedAt?: string;
}
