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

    // Handle empty responses (204 No Content or 200 OK with no body)
    const contentType = response.headers.get('content-type');
    const contentLength = response.headers.get('content-length');
    
    if (
      response.status === 204 || 
      contentLength === '0' ||
      !contentType?.includes('application/json')
    ) {
      return {} as T;
    }

    // Check if response has content before parsing
    const text = await response.text();
    if (!text || text.trim() === '') {
      return {} as T;
    }

    try {
      return JSON.parse(text) as T;
    } catch (error) {
      // If JSON parsing fails, return empty object for void responses
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
          // Ignore parsing errors
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
          // Ignore parsing errors
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
          // Ignore parsing errors
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

  // Corporate Dashboard APIs
  async getDashboardOverview(companyId: string, date?: string): Promise<DashboardOverviewDTO> {
    const AI_SERVICE_URL = process.env.NEXT_PUBLIC_AI_SERVICE_URL || 'http://localhost:8082';
    const params = date ? `?date=${date}` : '';
    const token = this.getAuthToken();
    
    const response = await fetch(`${AI_SERVICE_URL}/api/corporate/dashboard/${companyId}${params}`, {
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

  async getStressTimeline(
    companyId: string,
    startDate: string,
    endDate: string,
    granularity: string = 'day'
  ): Promise<StressTimelineDTO> {
    const AI_SERVICE_URL = process.env.NEXT_PUBLIC_AI_SERVICE_URL || 'http://localhost:8082';
    const token = this.getAuthToken();
    
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

    if (!response.ok) {
      const error = await response.text();
      throw new Error(error || `HTTP error! status: ${response.status}`);
    }

    return response.json();
  }

  async getDepartmentHeatmap(companyId: string, date?: string): Promise<DepartmentHeatmapDTO> {
    const AI_SERVICE_URL = process.env.NEXT_PUBLIC_AI_SERVICE_URL || 'http://localhost:8082';
    const params = date ? `?date=${date}` : '';
    const token = this.getAuthToken();
    
    const response = await fetch(`${AI_SERVICE_URL}/api/corporate/department-heatmap/${companyId}${params}`, {
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

  async getTurnoverPrediction(companyId: string, departmentId?: string): Promise<TurnoverPredictionDTO> {
    const AI_SERVICE_URL = process.env.NEXT_PUBLIC_AI_SERVICE_URL || 'http://localhost:8082';
    const params = departmentId ? `?departmentId=${departmentId}` : '';
    const token = this.getAuthToken();
    
    const response = await fetch(`${AI_SERVICE_URL}/api/corporate/turnover-prediction/${companyId}${params}`, {
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

  // Company management APIs
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

  // Subscription APIs
  async getSubscription(companyId: string): Promise<SubscriptionResponse> {
    return this.request<SubscriptionResponse>(`/api/subscriptions/companies/${companyId}`, {
      method: 'GET',
    });
  }

  // Public company registration
  async registerCompany(data: RegisterCompanyRequest): Promise<Company> {
    return this.request<Company>('/api/public/register-company', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  // Department management APIs
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

  // User management APIs
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

  // Usage tracking APIs
  async getUsageInfo(companyId: string): Promise<UsageInfo> {
    return this.request<UsageInfo>(`/api/subscriptions/companies/${companyId}/usage`, {
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

// Dashboard DTOs
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

// Company DTOs
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

