const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api').replace(/\/$/, '');

interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export interface RuntimeFeatures {
  mockPaymentEnabled: boolean;
}

type RequestOptions = UniApp.RequestOptions & {
  silent?: boolean;
};

export function request<T>(url: string, options: RequestOptions = {}): Promise<T> {
  return new Promise((resolve, reject) => {
    const { silent, ...requestOptions } = options;
    uni.request({
      ...requestOptions,
      url: `${API_BASE_URL}${url}`,
      success: (response) => {
        const body = response.data as ApiResponse<T>;
        if (body && body.code === 0) {
          resolve(body.data);
          return;
        }
        const message = body?.message || 'request failed';
        if (!silent) {
          uni.showToast({ title: message, icon: 'none' });
        }
        reject(new Error(message));
      },
      fail: (error) => {
        if (!silent) {
          uni.showToast({ title: '网络请求失败', icon: 'none' });
        }
        reject(error);
      }
    });
  });
}

export function loadRuntimeFeatures(): Promise<RuntimeFeatures> {
  return request<RuntimeFeatures>('/runtime/features');
}
