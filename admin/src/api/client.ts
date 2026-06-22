import axios from 'axios';

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  page: number;
  pageSize: number;
  pages: number;
}

export interface LoginResult {
  token: string;
  adminUserId: number;
  tenantId: number;
  username: string;
  displayName: string;
}

export interface RuntimeFeatures {
  mockPaymentEnabled: boolean;
}

const TOKEN_KEY = 'yanxitong.admin.token';
const PROFILE_KEY = 'yanxitong.admin.profile';

export const http = axios.create({
  baseURL: '/api',
  timeout: 10000
});

http.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

http.interceptors.response.use((response) => {
  const body = response.data as ApiResponse<unknown>;
  if (typeof body?.code === 'number' && body.code !== 0) {
    return Promise.reject(new Error(body.message || 'request failed'));
  }
  return response;
}, (error) => {
  if (error.response?.status === 401) {
    clearAuth();
    if (window.location.pathname !== '/login') {
      window.location.href = '/login';
    }
  }
  return Promise.reject(error);
});

export async function login(username: string, password: string): Promise<LoginResult> {
  const { data } = await http.post<ApiResponse<LoginResult>>('/auth/login', { username, password });
  saveAuth(data.data);
  return data.data;
}

export function saveAuth(profile: LoginResult): void {
  localStorage.setItem(TOKEN_KEY, profile.token);
  localStorage.setItem(PROFILE_KEY, JSON.stringify(profile));
}

export function getToken(): string {
  return localStorage.getItem(TOKEN_KEY) || '';
}

export function getProfile(): LoginResult | null {
  const raw = localStorage.getItem(PROFILE_KEY);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as LoginResult;
  } catch {
    clearAuth();
    return null;
  }
}

export function clearAuth(): void {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(PROFILE_KEY);
}

export async function loadRuntimeFeatures(): Promise<RuntimeFeatures> {
  const { data } = await http.get<ApiResponse<RuntimeFeatures>>('/runtime/features');
  return data.data;
}

export function recordsOf<T>(data: T[] | PageResult<T> | null | undefined): T[] {
  if (!data) {
    return [];
  }
  return Array.isArray(data) ? data : data.records || [];
}
