<template>
  <view class="page state-page" v-if="pageState !== 'ready'">
    <view class="state-card">
      <text class="state-title">{{ stateTitle }}</text>
      <text class="state-text">{{ stateText }}</text>
      <button v-if="pageState === 'error'" class="state-button" @tap="loadInvitation">重新加载</button>
    </view>
  </view>

  <view class="page" v-else-if="data" :class="[templateClass, eventTone]" :style="pageStyle">
    <view class="invite-shell">
      <view class="topbar">
        <text class="brand">宴席通</text>
        <button class="share-button" open-type="share">分享请柬</button>
      </view>

      <view class="cover-card">
        <image class="cover-image" :src="heroImage" mode="aspectFill" />
        <view class="cover-mask"></view>
        <view class="cover-content">
          <text class="fallback-mark">{{ coverMark }}</text>
          <text class="headline">{{ pageHeadline }}</text>
          <text class="subline">{{ greeting }}</text>
          <text class="names">{{ data.invitation.title }}</text>
        </view>
      </view>

      <view class="notice warning" v-if="data.templateAvailable === false">
        <text>{{ data.templateMessage || '原请柬模板已不可用，当前使用基础样式展示' }}</text>
      </view>

      <view class="intro-card">
        <text class="intro-text">{{ invitationCopy }}</text>
      </view>

      <view class="info-card">
        <view class="info-row">
          <text class="info-icon">日</text>
          <view class="info-main">
            <text class="info-label">宴席时间</text>
            <text class="info-value">{{ formatDate(data.banquet.banquetTime) }}</text>
            <text class="info-sub">{{ formatClock(data.banquet.banquetTime) }}</text>
          </view>
        </view>
        <view class="info-row">
          <text class="info-icon">地</text>
          <view class="info-main">
            <text class="info-label">宴席地点</text>
            <text class="info-value">{{ data.banquet.location || '敬请光临' }}</text>
            <text v-if="basicFields.addressDetail" class="info-sub">{{ basicFields.addressDetail }}</text>
          </view>
        </view>
        <view v-if="basicFields.hostName" class="info-row">
          <text class="info-icon">主</text>
          <view class="info-main">
            <text class="info-label">主办方</text>
            <text class="info-value">{{ basicFields.hostName }}</text>
          </view>
        </view>
        <view v-if="basicFields.contactPhone" class="info-row">
          <text class="info-icon">电</text>
          <view class="info-main">
            <text class="info-label">联系电话</text>
            <text class="info-value">{{ basicFields.contactPhone }}</text>
          </view>
        </view>
      </view>

      <view class="quick-card">
        <view class="quick-item" @tap="showMapTip">
          <text class="quick-icon">⌖</text>
          <text>地图导航</text>
        </view>
        <view class="quick-item" @tap="showComingSoon">
          <text class="quick-icon">车</text>
          <text>交通路线</text>
        </view>
        <view class="quick-item" @tap="showComingSoon">
          <text class="quick-icon">P</text>
          <text>停车指引</text>
        </view>
        <view class="quick-item" @tap="showComingSoon">
          <text class="quick-icon">温</text>
          <text>温馨提示</text>
        </view>
      </view>

      <view class="timeline" v-if="scheduleItems.length">
        <text class="section-title">宴席流程</text>
        <view v-for="(item, index) in scheduleItems" :key="item" class="timeline-item">
          <text class="timeline-dot">{{ index + 1 }}</text>
          <text>{{ item }}</text>
        </view>
      </view>

      <view class="copy-card">
        <text class="section-title">{{ data.giftSuccessCopywriting.title || '心意文案' }}</text>
        <text class="copy-content">{{ data.giftSuccessCopywriting.content }}</text>
      </view>

      <view class="notice" v-if="disabledEntryMessages.length">
        <text v-for="item in disabledEntryMessages" :key="item">{{ item }}</text>
      </view>

      <view class="footer-safe"></view>
      <view class="sticky-actions">
        <button class="primary-action" @tap="openRsvp">{{ eventTone === 'tone-memorial' ? '回执出席' : '回执出席' }}</button>
        <button class="secondary-action" v-if="showGiftEntry" @tap="openGift('ONLINE_GIFT')">在线随礼</button>
        <button class="secondary-action disabled" v-else @tap="showGiftDisabled">在线随礼</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { onShareAppMessage } from '@dcloudio/uni-app';
import { loadRuntimeFeatures, request, type RuntimeFeatures } from '../../../api/client';

interface PublicInvitation {
  invitation: {
    id: number;
    title: string;
    coverUrl?: string;
    basicFields?: string;
  };
  template?: {
    templateCode: string;
    typeCode: string;
    name: string;
    coverUrl?: string;
    priceType: string;
  };
  templatePresentation?: {
    styleCode: string;
    headline: string;
    defaultGreeting: string;
    defaultScheduleText: string;
    fallbackCoverLabel: string;
  };
  banquet: {
    id: number;
    eventTypeCode: string;
    location?: string;
    banquetTime?: string;
  };
  theme?: {
    primaryColor: string;
    secondaryColor?: string;
    iconStyle?: string;
  };
  giftSuccessCopywriting: {
    title?: string;
    content: string;
  };
  basicFields?: {
    hostName?: string;
    contactPhone?: string;
    addressDetail?: string;
    scheduleText?: string;
    greeting?: string;
    showGiftEntry?: string;
    showDeviceEntry?: string;
  };
  shareUrl?: string;
  actionUrls?: {
    rsvp?: string;
    onlineGift?: string;
    onsiteGift?: string;
    device?: string;
  };
  templateAvailable?: boolean;
  templateMessage?: string;
}

const data = ref<PublicInvitation>();
const features = ref<RuntimeFeatures>({ mockPaymentEnabled: false });
const slug = ref('');
const pageState = ref<'loading' | 'ready' | 'error'>('loading');
const errorMessage = ref('');
const basicFields = computed(() => {
  if (data.value?.basicFields) {
    return data.value.basicFields;
  }
  const raw = data.value?.invitation.basicFields;
  if (!raw) {
    return {} as { hostName?: string; contactPhone?: string; addressDetail?: string; scheduleText?: string; greeting?: string; showGiftEntry?: string; showDeviceEntry?: string };
  }
  try {
    return JSON.parse(raw) as { hostName?: string; contactPhone?: string; addressDetail?: string; scheduleText?: string; greeting?: string; showGiftEntry?: string; showDeviceEntry?: string };
  } catch {
    return {};
  }
});
const eventType = computed(() => data.value?.banquet.eventTypeCode || 'WEDDING');
const eventTone = computed(() => {
  if (eventType.value === 'MEMORIAL') return 'tone-memorial';
  if (eventType.value === 'SCHOOL') return 'tone-school';
  if (eventType.value === 'HOUSEWARMING') return 'tone-house';
  if (eventType.value === 'BABY') return 'tone-baby';
  if (eventType.value === 'BIRTHDAY') return 'tone-birthday';
  return 'tone-wedding';
});
const greeting = computed(() => basicFields.value.greeting || data.value?.templatePresentation?.defaultGreeting || defaultGreeting(eventType.value));
const invitationCopy = computed(() => {
  if (eventType.value === 'MEMORIAL') {
    return '我们怀着沉痛而感恩的心情，诚邀您参加追思会，共同追忆往昔，寄托哀思。';
  }
  return '诚邀您拨冗赴宴，共同见证这份重要时刻。您的到来，是我们最珍贵的祝福。';
});
const pageHeadline = computed(() => data.value?.templatePresentation?.headline || eventTitle(eventType.value));
const coverMark = computed(() => data.value?.templatePresentation?.fallbackCoverLabel || fallbackMark(eventType.value));
const scheduleItems = computed(() => (basicFields.value.scheduleText || data.value?.templatePresentation?.defaultScheduleText || '')
  .split(/\r?\n/)
  .map((item) => item.trim())
  .filter(Boolean));
const showGiftEntry = computed(() => basicFields.value.showGiftEntry !== '0' && features.value.mockPaymentEnabled);
const disabledEntryMessages = computed(() => {
  const messages: string[] = [];
  if (!showGiftEntry.value) {
    messages.push('在线随礼暂未开放，可先提交回执。');
  }
  if (basicFields.value.showDeviceEntry === '0') {
    messages.push('设备租赁入口暂未开放');
  }
  return messages;
});
const stateTitle = computed(() => {
  if (pageState.value === 'loading') {
    return '请柬加载中';
  }
  return '请柬无法打开';
});
const stateText = computed(() => {
  if (pageState.value === 'loading') {
    return '正在读取分享信息';
  }
  return errorMessage.value || '请确认分享链接是否完整';
});
const heroImage = computed(() => data.value?.invitation.coverUrl || data.value?.template?.coverUrl || defaultCover(eventType.value));
const templateClass = computed(() => {
  const style = data.value?.templatePresentation?.styleCode || '';
  if (style) {
    return `template-${style}`;
  }
  return 'template-rich';
});
const pageStyle = computed(() => ({
  '--primary': data.value?.theme?.primaryColor || '#d71920',
  '--secondary': data.value?.theme?.secondaryColor || '#f6c26b'
}));

function defaultCover(type: string) {
  if (type === 'MEMORIAL') return '/static/invitation/tpl_simple.png';
  if (type === 'SCHOOL') return '/static/home/package_blue.png';
  if (type === 'BIRTHDAY') return '/static/home/package_gold.png';
  return '/static/invitation/tpl_red.png';
}

function eventTitle(type: string) {
  const labels: Record<string, string> = {
    WEDDING: '婚礼请柬',
    BIRTHDAY: '寿宴请柬',
    BABY: '满月请柬',
    HOUSEWARMING: '乔迁请柬',
    SCHOOL: '升学请柬',
    MEMORIAL: '追思会请柬'
  };
  return labels[type] || '宴席请柬';
}

function fallbackMark(type: string) {
  const labels: Record<string, string> = {
    WEDDING: '囍',
    BIRTHDAY: '寿',
    BABY: '满',
    HOUSEWARMING: '福',
    SCHOOL: '学',
    MEMORIAL: '念'
  };
  return labels[type] || '宴';
}

function defaultGreeting(type: string) {
  if (type === 'MEMORIAL') return '深切缅怀，永远怀念';
  if (type === 'SCHOOL') return '金榜题名，前程似锦';
  if (type === 'HOUSEWARMING') return '乔迁之喜，恭候光临';
  if (type === 'BABY') return '喜迎新生，满月同庆';
  if (type === 'BIRTHDAY') return '福寿安康，阖家欢乐';
  return '百年好合，永结同心';
}

function formatDate(value?: string) {
  if (!value) return '时间待定';
  const normalized = value.replace('T', ' ');
  const [date] = normalized.split(' ');
  return date || normalized;
}

function formatClock(value?: string) {
  if (!value) return '';
  const normalized = value.replace('T', ' ');
  const [, time] = normalized.split(' ');
  return time ? time.slice(0, 5) : '';
}

function openRsvp() {
  if (!data.value) {
    return;
  }
  uni.navigateTo({ url: data.value.actionUrls?.rsvp || `/pages/rsvp/submit/index?banquetId=${data.value.banquet.id}&invitationId=${data.value.invitation.id}` });
}

function openGift(entrySource: string) {
  if (!data.value) {
    return;
  }
  const url = entrySource === 'ONSITE_QR' ? data.value.actionUrls?.onsiteGift : data.value.actionUrls?.onlineGift;
  uni.navigateTo({ url: url || `/pages/gift/pay/index?banquetId=${data.value.banquet.id}&entrySource=${entrySource}` });
}

function showGiftDisabled() {
  uni.showToast({ title: '在线随礼需完成微信支付配置后开放', icon: 'none' });
}

function showMapTip() {
  uni.showToast({ title: '地图导航将在地址坐标配置后开放', icon: 'none' });
}

function showComingSoon() {
  uni.showToast({ title: '路线、停车和温馨提示将在请柬编辑中配置', icon: 'none' });
}

async function loadInvitation() {
  if (!slug.value) {
    pageState.value = 'error';
    errorMessage.value = '分享链接缺少 slug 参数';
    return;
  }
  pageState.value = 'loading';
  errorMessage.value = '';
  try {
    const [runtimeFeatures, invitation] = await Promise.all([
      loadRuntimeFeatures().catch(() => ({ mockPaymentEnabled: false })),
      request<PublicInvitation>(`/invitations/public/${encodeURIComponent(slug.value)}`)
    ]);
    features.value = runtimeFeatures;
    data.value = invitation;
    pageState.value = 'ready';
  } catch (error) {
    data.value = undefined;
    pageState.value = 'error';
    errorMessage.value = error instanceof Error ? error.message : '请柬不存在或已失效';
  }
}

onMounted(async () => {
  const pages = getCurrentPages();
  const current = pages[pages.length - 1] as unknown as { options?: Record<string, string> };
  slug.value = current.options?.slug || '';
  await loadInvitation();
});

onShareAppMessage(() => ({
  title: data.value?.invitation.title || pageHeadline.value || '宴席请柬',
  path: slug.value ? `/pages/invite/public/index?slug=${encodeURIComponent(slug.value)}` : '/pages/home/index/index',
  imageUrl: heroImage.value
}));
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #fff8ef;
  color: #171c2a;
}

.state-page {
  display: grid;
  place-items: center;
  padding: 32rpx;
  box-sizing: border-box;
}

.state-card {
  display: grid;
  gap: 18rpx;
  width: 100%;
  padding: 50rpx 34rpx;
  border: 1rpx solid #f0dfcf;
  border-radius: 24rpx;
  background: #fff;
  text-align: center;
  box-shadow: 0 12rpx 32rpx rgba(82, 45, 24, 0.07);
}

.state-title {
  color: #171c2a;
  font-size: 38rpx;
  font-weight: 900;
}

.state-text {
  color: #806b5c;
  font-size: 27rpx;
  line-height: 1.6;
}

.state-button {
  height: 80rpx;
  border-radius: 18rpx;
  background: linear-gradient(135deg, #e83a32, #c91419);
  color: #fff;
  font-weight: 900;
  line-height: 80rpx;
}

.state-button::after,
.share-button::after,
.primary-action::after,
.secondary-action::after {
  border: 0;
}

.invite-shell {
  min-height: 100vh;
  padding: 24rpx 24rpx 0;
  box-sizing: border-box;
  background:
    radial-gradient(circle at 50% -120rpx, rgba(230, 0, 18, 0.12), transparent 420rpx),
    #fff8ef;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6rpx 4rpx 22rpx;
}

.brand {
  color: #c7191e;
  font-size: 34rpx;
  font-weight: 900;
}

.share-button {
  height: 58rpx;
  margin: 0;
  padding: 0 24rpx;
  border: 1rpx solid #ead1b2;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.72);
  color: #9d2b22;
  font-size: 24rpx;
  font-weight: 800;
  line-height: 58rpx;
}

.cover-card {
  position: relative;
  overflow: hidden;
  height: 760rpx;
  border-radius: 28rpx;
  background: #a70d12;
  box-shadow: 0 18rpx 48rpx rgba(156, 38, 24, 0.22);
}

.cover-image {
  width: 100%;
  height: 100%;
  display: block;
}

.cover-mask {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(180deg, rgba(142, 8, 13, 0.14), rgba(142, 8, 13, 0.52)),
    radial-gradient(circle at 50% 72%, rgba(255, 246, 224, 0.92), rgba(255, 246, 224, 0.58) 33%, transparent 55%);
}

.cover-content {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  padding: 72rpx 42rpx 76rpx;
  box-sizing: border-box;
  text-align: center;
}

.fallback-mark {
  display: grid;
  place-items: center;
  width: 170rpx;
  height: 170rpx;
  margin-bottom: 18rpx;
  border: 4rpx solid rgba(255, 229, 176, 0.88);
  border-radius: 50%;
  color: #b51518;
  background: rgba(255, 247, 225, 0.86);
  font-family: serif;
  font-size: 96rpx;
  font-weight: 900;
}

.headline {
  display: block;
  color: #7d1615;
  font-family: serif;
  font-size: 42rpx;
  font-weight: 900;
}

.subline {
  display: block;
  margin-top: 12rpx;
  color: #8d5a32;
  font-size: 26rpx;
  font-weight: 700;
}

.names {
  display: block;
  margin-top: 28rpx;
  color: #7d1615;
  font-family: serif;
  font-size: 48rpx;
  font-weight: 900;
  line-height: 1.25;
}

.intro-card,
.info-card,
.quick-card,
.timeline,
.copy-card,
.notice {
  margin-top: 24rpx;
  border: 1rpx solid #f0dfcf;
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 12rpx 32rpx rgba(82, 45, 24, 0.07);
}

.intro-card {
  padding: 30rpx;
}

.intro-text {
  display: block;
  color: #6b4a35;
  font-size: 29rpx;
  line-height: 1.8;
  text-align: center;
}

.info-card {
  overflow: hidden;
}

.info-row {
  display: grid;
  grid-template-columns: 64rpx 1fr;
  gap: 20rpx;
  align-items: center;
  padding: 26rpx 28rpx;
  border-bottom: 1rpx solid #f0dfcf;
}

.info-row:last-child {
  border-bottom: 0;
}

.info-icon {
  display: grid;
  place-items: center;
  width: 56rpx;
  height: 56rpx;
  border-radius: 50%;
  background: #fff0ea;
  color: #c7191e;
  font-size: 24rpx;
  font-weight: 900;
}

.info-label,
.info-value,
.info-sub {
  display: block;
}

.info-label {
  color: #9b806a;
  font-size: 24rpx;
  font-weight: 700;
}

.info-value {
  margin-top: 6rpx;
  color: #241f1b;
  font-size: 31rpx;
  font-weight: 900;
  line-height: 1.4;
}

.info-sub {
  margin-top: 6rpx;
  color: #7f7167;
  font-size: 25rpx;
  line-height: 1.45;
}

.quick-card {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  overflow: hidden;
}

.quick-item {
  display: grid;
  justify-items: center;
  gap: 8rpx;
  padding: 24rpx 4rpx;
  border-right: 1rpx solid #f0dfcf;
  color: #5b4a3d;
  font-size: 23rpx;
  font-weight: 700;
}

.quick-item:last-child {
  border-right: 0;
}

.quick-icon {
  display: grid;
  place-items: center;
  width: 46rpx;
  height: 46rpx;
  border-radius: 50%;
  background: #fff0ea;
  color: #c7191e;
  font-size: 22rpx;
  font-weight: 900;
}

.timeline,
.copy-card,
.notice {
  padding: 28rpx;
}

.section-title {
  display: block;
  margin-bottom: 18rpx;
  color: #171c2a;
  font-size: 32rpx;
  font-weight: 900;
}

.timeline-item {
  display: flex;
  align-items: center;
  gap: 16rpx;
  min-height: 58rpx;
  color: #5b4a3d;
  font-size: 27rpx;
  font-weight: 700;
}

.timeline-dot {
  display: grid;
  place-items: center;
  width: 38rpx;
  height: 38rpx;
  border-radius: 50%;
  background: #c7191e;
  color: #fff;
  font-size: 20rpx;
  font-weight: 900;
}

.copy-content {
  display: block;
  color: #6b4a35;
  font-size: 27rpx;
  line-height: 1.7;
}

.notice {
  display: grid;
  gap: 8rpx;
  background: #fff7ec;
  color: #9a5b30;
  font-size: 25rpx;
  line-height: 1.5;
}

.notice.warning {
  border-color: #f5d9af;
  background: #fffaf1;
  color: #9a5b30;
}

.footer-safe {
  height: 152rpx;
}

.sticky-actions {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 20;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
  padding: 18rpx 24rpx calc(18rpx + env(safe-area-inset-bottom));
  background: rgba(255, 248, 239, 0.96);
  box-shadow: 0 -8rpx 28rpx rgba(72, 45, 24, 0.08);
}

.primary-action,
.secondary-action {
  height: 92rpx;
  margin: 0;
  border-radius: 20rpx;
  font-size: 30rpx;
  font-weight: 900;
  line-height: 92rpx;
}

.primary-action {
  background: linear-gradient(135deg, #e83a32, #c91419);
  color: #fff;
}

.secondary-action {
  border: 1rpx solid #e7bf83;
  background: #fff1d2;
  color: #9e4d12;
}

.secondary-action.disabled {
  opacity: 0.65;
}

.tone-memorial {
  background: #0f1113;
}

.tone-memorial .invite-shell {
  background:
    radial-gradient(circle at 50% -120rpx, rgba(255, 255, 255, 0.08), transparent 420rpx),
    #0f1113;
}

.tone-memorial .brand,
.tone-memorial .headline,
.tone-memorial .names {
  color: #e8e1d4;
}

.tone-memorial .cover-card {
  background: #111;
  box-shadow: 0 18rpx 48rpx rgba(0, 0, 0, 0.28);
}

.tone-memorial .cover-mask {
  background:
    linear-gradient(180deg, rgba(0, 0, 0, 0.1), rgba(0, 0, 0, 0.62)),
    radial-gradient(circle at 50% 72%, rgba(32, 32, 32, 0.88), rgba(32, 32, 32, 0.56) 33%, transparent 58%);
}

.tone-memorial .fallback-mark {
  border-color: rgba(225, 220, 210, 0.72);
  background: rgba(28, 28, 28, 0.8);
  color: #e8e1d4;
}

.tone-memorial .subline,
.tone-memorial .intro-text,
.tone-memorial .copy-content {
  color: #d8d0c2;
}

.tone-memorial .intro-card,
.tone-memorial .info-card,
.tone-memorial .quick-card,
.tone-memorial .timeline,
.tone-memorial .copy-card,
.tone-memorial .notice {
  border-color: rgba(255, 255, 255, 0.14);
  background: rgba(31, 31, 31, 0.92);
  box-shadow: none;
}

.tone-memorial .info-value,
.tone-memorial .section-title,
.tone-memorial .timeline-item {
  color: #f1eadf;
}

.tone-memorial .info-label,
.tone-memorial .info-sub,
.tone-memorial .quick-item {
  color: #bdb5aa;
}

.tone-memorial .primary-action {
  background: linear-gradient(135deg, #4c4c4c, #1f1f1f);
}

.tone-memorial .secondary-action {
  border-color: #ddd6cb;
  background: #f0ebe3;
  color: #222;
}
</style>
