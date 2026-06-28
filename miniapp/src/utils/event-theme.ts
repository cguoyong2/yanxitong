export interface EventTheme {
  code: string;
  name: string;
  subtitle: string;
  icon: string;
  mark: string;
  tone: string;
  homeText: string;
  favorText: string;
  invitationText: string;
  mineText: string;
  giftLabel: string;
  giftActionLabel: string;
  onlineGiftLabel: string;
  offlineGiftLabel: string;
  giftRecordLabel: string;
  giftAmountLabel: string;
  blessingLabel: string;
  blessingPlaceholder: string;
  defaultBlessing: string;
  invitationTitle: string;
  invitationCopy: string;
  rsvpSubtitle: string;
  rsvpSuccessText: string;
  prepTitle: string;
  detailGiftCopyTitle: string;
}

export const EVENT_THEME_STORAGE_KEY = 'yanxitong-active-event-type';

export const EVENT_THEMES: EventTheme[] = [
  {
    code: 'WEDDING',
    name: '婚宴',
    subtitle: '喜结良缘',
    icon: '囍',
    mark: '囍',
    tone: 'red',
    homeText: '您好，办婚宴，用宴席通',
    favorText: '婚宴人情，收送有数',
    invitationText: '婚礼请柬，喜庆体面',
    mineText: '婚宴订单、请柬、设备一站管理',
    giftLabel: '随礼',
    giftActionLabel: '去随礼',
    onlineGiftLabel: '线上随礼',
    offlineGiftLabel: '线下记礼',
    giftRecordLabel: '收礼记录',
    giftAmountLabel: '礼金金额',
    blessingLabel: '祝福语',
    blessingPlaceholder: '如：新婚快乐、现金礼金',
    defaultBlessing: '百年好合，永结同心',
    invitationTitle: '婚礼请柬',
    invitationCopy: '诚邀您拨冗赴宴，共同见证这份重要时刻。您的到来，是我们最珍贵的祝福。',
    rsvpSubtitle: '感谢您的祝福与赴约',
    rsvpSuccessText: '已记录您的回执，期待您的光临。',
    prepTitle: '办宴准备',
    detailGiftCopyTitle: '收礼文案'
  },
  {
    code: 'BIRTHDAY',
    name: '寿宴',
    subtitle: '福寿安康',
    icon: '寿',
    mark: '寿',
    tone: 'orange',
    homeText: '寿宴筹备，福寿有序',
    favorText: '寿礼往来，亲友有序',
    invitationText: '寿宴请柬，福寿安康',
    mineText: '寿宴服务、请柬、礼金一站管理',
    giftLabel: '寿礼',
    giftActionLabel: '敬送寿礼',
    onlineGiftLabel: '线上寿礼',
    offlineGiftLabel: '寿礼登记',
    giftRecordLabel: '寿礼记录',
    giftAmountLabel: '寿礼金额',
    blessingLabel: '祝寿语',
    blessingPlaceholder: '如：福寿安康、寿礼现金',
    defaultBlessing: '福寿安康，阖家欢乐',
    invitationTitle: '寿宴请柬',
    invitationCopy: '诚邀您拨冗赴宴，共贺寿辰。您的到来，是这场寿宴最珍贵的心意。',
    rsvpSubtitle: '感谢您的祝寿与赴约',
    rsvpSuccessText: '已记录您的回执，感谢您的祝寿心意。',
    prepTitle: '寿宴准备',
    detailGiftCopyTitle: '寿礼文案'
  },
  {
    code: 'BABY',
    name: '满月',
    subtitle: '喜迎新生',
    icon: '满',
    mark: '满',
    tone: 'pink',
    homeText: '满月礼成，亲友同喜',
    favorText: '满月礼金，祝福清楚',
    invitationText: '满月请柬，喜迎新生',
    mineText: '满月宴请、回执、礼金一站管理',
    giftLabel: '满月礼',
    giftActionLabel: '送满月礼',
    onlineGiftLabel: '线上满月礼',
    offlineGiftLabel: '满月礼登记',
    giftRecordLabel: '满月礼记录',
    giftAmountLabel: '礼金金额',
    blessingLabel: '祝福语',
    blessingPlaceholder: '如：健康成长、满月礼金',
    defaultBlessing: '健康成长，平安喜乐',
    invitationTitle: '满月请柬',
    invitationCopy: '诚邀您参加满月宴，共同见证新生命的喜悦与祝福。',
    rsvpSubtitle: '感谢您的祝福与赴约',
    rsvpSuccessText: '已记录您的回执，感谢您的满月祝福。',
    prepTitle: '满月宴准备',
    detailGiftCopyTitle: '满月礼文案'
  },
  {
    code: 'HOUSEWARMING',
    name: '乔迁',
    subtitle: '乔迁之喜',
    icon: '福',
    mark: '福',
    tone: 'green',
    homeText: '乔迁新居，邀亲友同贺',
    favorText: '乔迁往来，礼数分明',
    invitationText: '乔迁请柬，新居同贺',
    mineText: '乔迁宴席、请柬、服务一站管理',
    giftLabel: '乔迁礼',
    giftActionLabel: '送乔迁礼',
    onlineGiftLabel: '线上乔迁礼',
    offlineGiftLabel: '乔迁礼登记',
    giftRecordLabel: '乔迁礼记录',
    giftAmountLabel: '礼金金额',
    blessingLabel: '祝贺语',
    blessingPlaceholder: '如：乔迁大吉、乔迁礼金',
    defaultBlessing: '乔迁大吉，万事顺意',
    invitationTitle: '乔迁请柬',
    invitationCopy: '诚邀您拨冗莅临，共贺新居落成，同享乔迁之喜。',
    rsvpSubtitle: '感谢您的祝贺与赴约',
    rsvpSuccessText: '已记录您的回执，感谢您的乔迁祝贺。',
    prepTitle: '乔迁宴准备',
    detailGiftCopyTitle: '乔迁礼文案'
  },
  {
    code: 'SCHOOL',
    name: '升学',
    subtitle: '金榜题名',
    icon: '学',
    mark: '学',
    tone: 'blue',
    homeText: '升学庆贺，前程有光',
    favorText: '升学礼账，亲友明细',
    invitationText: '升学请柬，金榜题名',
    mineText: '升学宴席、请柬、礼金一站管理',
    giftLabel: '升学礼',
    giftActionLabel: '送升学礼',
    onlineGiftLabel: '线上升学礼',
    offlineGiftLabel: '升学礼登记',
    giftRecordLabel: '升学礼记录',
    giftAmountLabel: '礼金金额',
    blessingLabel: '祝贺语',
    blessingPlaceholder: '如：金榜题名、升学礼金',
    defaultBlessing: '学业有成，前程似锦',
    invitationTitle: '升学请柬',
    invitationCopy: '诚邀您参加升学宴，共同分享金榜题名的喜悦与祝福。',
    rsvpSubtitle: '感谢您的祝贺与赴约',
    rsvpSuccessText: '已记录您的回执，感谢您的升学祝贺。',
    prepTitle: '升学宴准备',
    detailGiftCopyTitle: '升学礼文案'
  },
  {
    code: 'MEMORIAL',
    name: '追思会',
    subtitle: '追思缅怀',
    icon: '念',
    mark: '念',
    tone: 'black',
    homeText: '慎终追远，思念长存',
    favorText: '追思礼账，庄重记录',
    invitationText: '追思会请柬，缅怀永存',
    mineText: '追思会安排、回执、服务一站管理',
    giftLabel: '心意',
    giftActionLabel: '敬献心意',
    onlineGiftLabel: '线上敬献',
    offlineGiftLabel: '心意登记',
    giftRecordLabel: '心意记录',
    giftAmountLabel: '心意金额',
    blessingLabel: '留言',
    blessingPlaceholder: '如：深切缅怀、追思心意',
    defaultBlessing: '深切缅怀，永远怀念',
    invitationTitle: '追思会请柬',
    invitationCopy: '我们怀着沉痛而感恩的心情，诚邀您参加追思会，共同追忆往昔，寄托哀思。',
    rsvpSubtitle: '感谢您的缅怀与告知',
    rsvpSuccessText: '已记录您的回执，感谢您的缅怀与告知。',
    prepTitle: '追思会安排',
    detailGiftCopyTitle: '心意文案'
  },
  {
    code: 'OTHER',
    name: '其他',
    subtitle: '更多类型',
    icon: '宴',
    mark: '宴',
    tone: 'purple',
    homeText: '办宴席，用宴席通',
    favorText: '人情往来，清楚记录',
    invitationText: '宴席请柬，灵活配置',
    mineText: '宴席订单、请柬、设备一站管理',
    giftLabel: '心意',
    giftActionLabel: '送心意',
    onlineGiftLabel: '线上心意',
    offlineGiftLabel: '线下记礼',
    giftRecordLabel: '礼账记录',
    giftAmountLabel: '金额',
    blessingLabel: '留言',
    blessingPlaceholder: '备注或留言',
    defaultBlessing: '顺遂圆满，万事如意',
    invitationTitle: '宴席请柬',
    invitationCopy: '诚邀您拨冗赴宴，共同见证这份重要时刻。',
    rsvpSubtitle: '感谢您的回复与赴约',
    rsvpSuccessText: '已记录您的回执，感谢您的回复。',
    prepTitle: '宴席准备',
    detailGiftCopyTitle: '心意文案'
  }
];

export function eventThemeFor(code?: string) {
  return EVENT_THEMES.find((item) => item.code === code) || EVENT_THEMES[0];
}

export function readActiveEventType() {
  const stored = uni.getStorageSync(EVENT_THEME_STORAGE_KEY);
  return eventThemeFor(stored || 'WEDDING').code;
}

export function writeActiveEventType(code: string) {
  const theme = eventThemeFor(code);
  uni.setStorageSync(EVENT_THEME_STORAGE_KEY, theme.code);
  return theme.code;
}

export function eventToneClass(code?: string) {
  const tone = eventThemeFor(code).tone;
  const map: Record<string, string> = {
    red: 'tone-wedding',
    orange: 'tone-birthday',
    pink: 'tone-baby',
    green: 'tone-house',
    blue: 'tone-school',
    black: 'tone-memorial',
    purple: 'tone-other'
  };
  return map[tone] || 'tone-wedding';
}

export async function fetchBanquetEventType(
  banquetId: string,
  requestFn: <T>(url: string) => Promise<T>,
  fallback = readActiveEventType()
) {
  if (!banquetId) {
    return fallback;
  }
  const detail = await requestFn<{ banquet?: { eventTypeCode?: string }; eventTypeCode?: string }>(`/banquets/${banquetId}`).catch(() => undefined);
  return detail?.banquet?.eventTypeCode || detail?.eventTypeCode || fallback;
}
