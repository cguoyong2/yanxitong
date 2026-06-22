const API_BASE_URL = 'http://localhost:8080/api';

interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export interface RuntimeFeatures {
  mockPaymentEnabled: boolean;
}

export function request<T>(url: string, options: UniApp.RequestOptions = {}): Promise<T> {
  return new Promise((resolve, reject) => {
    uni.request({
      ...options,
      url: `${API_BASE_URL}${url}`,
      success: (response) => {
        const body = response.data as ApiResponse<T>;
        if (body && body.code === 0) {
          resolve(body.data);
          return;
        }
        const message = body?.message || 'request failed';
        uni.showToast({ title: message, icon: 'none' });
        reject(new Error(message));
      },
      fail: (error) => {
        uni.showToast({ title: '网络请求失败', icon: 'none' });
        reject(error);
      }
    });
  });
}

export function loadRuntimeFeatures(): Promise<RuntimeFeatures> {
  return request<RuntimeFeatures>('/runtime/features');
}
