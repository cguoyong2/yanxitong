const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api').replace(/\/$/, '');

interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export interface MiniappSession {
  token: string;
  expiresInSeconds: number;
  userId: number;
  openId: string;
  roleCode: string;
}

export interface RuntimeFeatures {
  mockPaymentEnabled: boolean;
}

type RequestOptions = UniApp.RequestOptions & {
  silent?: boolean;
  auth?: boolean;
};

const SESSION_KEY = 'qingliji-miniapp-session';
let loginPromise: Promise<MiniappSession> | null = null;

function storedSession(): MiniappSession | null {
  const value = uni.getStorageSync(SESSION_KEY) as MiniappSession | undefined;
  return value?.token ? value : null;
}

function clearSession(): void {
  uni.removeStorageSync(SESSION_KEY);
}

function login(): Promise<MiniappSession> {
  if (loginPromise) {
    return loginPromise;
  }
  loginPromise = new Promise((resolve, reject) => {
    uni.login({
      provider: 'weixin',
      success: (loginResult) => {
        if (!loginResult.code) {
          reject(new Error('未获取到微信登录凭证'));
          return;
        }
        uni.request({
          url: `${API_BASE_URL}/wechat/miniapp/login`,
          method: 'POST',
          data: { code: loginResult.code },
          success: (response) => {
            const body = response.data as ApiResponse<MiniappSession>;
            if (body?.code === 0 && body.data?.token) {
              uni.setStorageSync(SESSION_KEY, body.data);
              resolve(body.data);
              return;
            }
            reject(new Error(body?.message || '微信登录失败'));
          },
          fail: () => reject(new Error('微信登录网络请求失败'))
        });
      },
      fail: () => reject(new Error('无法调用微信登录'))
    });
  }).finally(() => {
    loginPromise = null;
  });
  return loginPromise;
}

async function ensureSession(): Promise<MiniappSession> {
  return storedSession() || login();
}

export function getMiniappSession(): Promise<MiniappSession> {
  return ensureSession();
}

function dispatch<T>(url: string, options: RequestOptions, retried = false): Promise<T> {
  return new Promise((resolve, reject) => {
    const { silent, auth, ...requestOptions } = options;
    const session = storedSession();
    uni.request({
      ...requestOptions,
      url: `${API_BASE_URL}${url}`,
      header: {
        ...(requestOptions.header || {}),
        ...(session?.token ? { Authorization: `Bearer ${session.token}` } : {})
      },
      success: (response) => {
        const body = response.data as ApiResponse<T>;
        if (body && body.code === 0) {
          resolve(body.data);
          return;
        }
        if (!retried && (response.statusCode === 401 || body?.code === 401)) {
          clearSession();
          login()
            .then(() => dispatch<T>(url, options, true))
            .then(resolve)
            .catch(reject);
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

export async function request<T>(url: string, options: RequestOptions = {}): Promise<T> {
  if (options.auth !== false) {
    try {
      await ensureSession();
    } catch (error) {
      const message = error instanceof Error ? error.message : '微信登录失败';
      if (!options.silent) {
        uni.showToast({ title: message, icon: 'none' });
      }
      throw error;
    }
  }
  return dispatch<T>(url, options);
}

export function loadRuntimeFeatures(): Promise<RuntimeFeatures> {
  return request<RuntimeFeatures>('/runtime/features', { auth: false });
}
