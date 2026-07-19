import { getMiniappSession, request } from '../api/client';

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

export async function getWechatOpenId() {
  const session = await getMiniappSession();
  if (!session.openId) {
    throw new Error('微信 openid 获取失败');
  }
  return session.openId;
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
      fail: (error) => reject(new Error(normalizePaymentError(error)))
    } as unknown as UniApp.RequestPaymentOptions);
  });
}

export function normalizePaymentFlowError(error: unknown, fallback = '支付暂未完成，请稍后重试') {
  const message = error instanceof Error
    ? error.message
    : typeof error === 'object' && error && 'errMsg' in error
      ? String((error as { errMsg?: string }).errMsg || '')
      : String(error || '');

  if (/cancel/i.test(message)) {
    return '已取消支付';
  }
  if (/openid|code2session|login code|微信登录|code 为空/i.test(message)) {
    return '微信登录状态异常，请重新进入小程序后再试';
  }
  if (/Last unit does not have enough valid bits|private key|apiclient|certificate|cert|API ?v3|Wechat service-provider|service-provider|merchant|支付通道|pay config|signature/i.test(message)) {
    return '支付配置异常，请联系管理员处理';
  }
  if (/requestPayment:fail/i.test(message) || /\bfail\b/i.test(message)) {
    return '微信支付未完成，请稍后重试';
  }
  return message || fallback;
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

function normalizePaymentError(error: unknown) {
  return normalizePaymentFlowError(error, '微信支付未完成，请稍后重试');
}
