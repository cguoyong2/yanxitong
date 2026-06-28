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
    mineText: '婚宴订单、请柬、设备一站管理'
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
    mineText: '寿宴服务、请柬、礼金一站管理'
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
    mineText: '满月宴请、回执、礼金一站管理'
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
    mineText: '乔迁宴席、请柬、服务一站管理'
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
    mineText: '升学宴席、请柬、礼金一站管理'
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
    mineText: '追思会安排、回执、服务一站管理'
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
    mineText: '宴席订单、请柬、设备一站管理'
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
