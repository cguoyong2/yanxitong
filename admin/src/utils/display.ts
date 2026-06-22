export function formatMoney(value: unknown): string {
  const amount = Number(value || 0);
  return `¥${amount.toFixed(2)}`;
}

export function formatDateTime(value: unknown): string {
  if (!value) {
    return '-';
  }
  const text = String(value).replace('T', ' ');
  return text.length > 19 ? text.slice(0, 19) : text;
}

const labels: Record<string, string> = {
  ACTIVE: '上架',
  INACTIVE: '下架',
  DRAFT: '草稿',
  CREATED: '已创建',
  UNPAID: '未支付',
  PAID: '已支付',
  CONFIRMED: '已确认',
  DELIVERING: '配送中',
  DELIVERED: '已交付',
  CANCELLED: '已取消',
  ONLINE_GIFT: '线上随礼',
  ONSITE_QR: '现场扫码',
  CASH: '现金记礼',
  ATTEND: '出席',
  ATTENDING: '出席',
  PENDING: '待定',
  DECLINED: '婉拒',
  RECEIVED: '收到',
  GIVEN: '给出',
  MANUAL: '手动补录',
  VERIFIED: '已验证',
  FAILED: '失败',
  SUCCESS: '成功',
  HANDLED: '已处理',
  IGNORED: '已忽略',
  PROCESSING: '处理中',
  SIMULATED: '模拟',
  PUSHED: '已推送',
  OFFLINE: '离线',
  CLOUD_SPEAKER: '云喇叭',
  CONFIRM_SCREEN: '确认屏',
  DEVICE_RENTAL: '设备租赁',
  INCLUDED: '包含',
  CONFIG: '配置',
  PLAN: '版本权益',
  EVENT_TYPE: '宴席类型',
  THEME: '主题',
  TEMPLATE: '模板',
  DEVICE: '设备',
  GIFT: '礼金',
  JSAPI: '小程序支付',
  FAVOR: '人情',
  PAYMENT: '支付',
  EXPORT: '导出',
  AUTH: '登录鉴权',
  BANQUET: '宴席',
  INVITATION: '请柬',
  ADMIN: '管理员',
  SYSTEM: '系统',
  GIFT_PAID: '礼金成功'
};

export function displayLabel(value: unknown): string {
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  return labels[String(value)] || String(value);
}

export function tagType(value: unknown): 'success' | 'warning' | 'info' | 'danger' | 'primary' {
  const text = String(value);
  if (['ACTIVE', 'PAID', 'CONFIRMED', 'DELIVERED', 'ATTEND', 'ATTENDING', 'SUCCESS', 'HANDLED', 'VERIFIED', 'PUSHED', 'INCLUDED'].includes(text)) {
    return 'success';
  }
  if (['DRAFT', 'UNPAID', 'PENDING', 'PROCESSING', 'SIMULATED', 'DELIVERING'].includes(text)) {
    return 'warning';
  }
  if (['FAILED', 'DECLINED', 'CANCELLED'].includes(text)) {
    return 'danger';
  }
  if (['INACTIVE', 'IGNORED', 'OFFLINE', 'DISABLED'].includes(text) || value === 0) {
    return 'info';
  }
  return 'primary';
}
