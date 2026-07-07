import { request } from '../api/client';

interface OpenIdResult {
  openId: string;
}

export interface PaymentOrderResult {
  order: {
    orderNo: string;
  };
  payPayload?: string | WechatPayPayload;
}

interface WechatPayPayload {
  appId?: string;
  timeStamp?: string;
  nonceStr?: string;
  package?: string;
  packageVal?: string;
  signType?: string;
  paySign?: string;
}

const OPEN_ID_CACHE_KEY = 'wechat-miniapp-openid';

export async function getWechatOpenId() {
  const cached = uni.getStorageSync(OPEN_ID_CACHE_KEY);
  if (cached) {
    return String(cached);
  }
  const code = await loginCode();
  const result = await request<OpenIdResult>('/wechat/miniapp/openid', {
    method: 'POST',
    data: { code }
  });
  if (!result.openId) {
    throw new Error('微信 openid 获取失败');
  }
  uni.setStorageSync(OPEN_ID_CACHE_KEY, result.openId);
  return result.openId;
}

export async function requestWechatPayment(payPayload?: string | WechatPayPayload) {
  const payload = parsePayPayload(payPayload);
  if (!payload) {
    throw new Error('微信支付参数缺失，请检查支付配置');
  }
  await new Promise<void>((resolve, reject) => {
    uni.requestPayment({
      provider: 'wxpay',
      timeStamp: payload.timeStamp,
      nonceStr: payload.nonceStr,
      package: payload.packageVal || payload.package,
      signType: payload.signType || 'RSA',
      paySign: payload.paySign,
      success: () => resolve(),
      fail: (error) => reject(error)
    } as unknown as UniApp.RequestPaymentOptions);
  });
}

export async function createBusinessPayment(endpoint: string) {
  const payerOpenId = await getWechatOpenId();
  return request<PaymentOrderResult>(endpoint, {
    method: 'POST',
    data: { payerOpenId }
  });
}

function parsePayPayload(payPayload?: string | WechatPayPayload) {
  if (!payPayload) {
    return undefined;
  }
  const payload = typeof payPayload === 'string' ? JSON.parse(payPayload || '{}') as WechatPayPayload : payPayload;
  if (!payload.timeStamp || !payload.nonceStr || !(payload.packageVal || payload.package) || !payload.paySign) {
    return undefined;
  }
  return payload;
}

function loginCode() {
  return new Promise<string>((resolve, reject) => {
    uni.login({
      provider: 'weixin',
      success: (result) => {
        if (result.code) {
          resolve(result.code);
        } else {
          reject(new Error('微信登录 code 为空'));
        }
      },
      fail: (error) => reject(error)
    });
  });
}
