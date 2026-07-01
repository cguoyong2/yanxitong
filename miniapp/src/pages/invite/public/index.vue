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
        <image v-if="heroImage" class="cover-image" :src="heroImage" mode="aspectFill" />
        <view v-else class="cover-fallback">
          <text class="cover-fallback-mark">{{ coverMark }}</text>
        </view>
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
        <view class="info-row" @tap="showMapTip">
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
        <view v-if="basicFields.contactPhone" class="info-row" @tap="showContactTip">
          <text class="info-icon">电</text>
          <view class="info-main">
            <text class="info-label">联系电话</text>
            <text class="info-value">{{ basicFields.contactPhone }}</text>
            <text class="info-sub">点击可复制电话</text>
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

      <view class="entry-guide">
        <view class="guide-head">
          <text class="section-title">宾客操作</text>
          <text class="guide-note">先回执，再表达心意</text>
        </view>
        <view class="guide-row" @tap="openRsvp">
          <text class="guide-index">1</text>
          <view class="guide-main">
            <text class="guide-title">回执出席</text>
            <text class="guide-desc">填写姓名、人数、用餐和住宿需求，主办方可实时统计。</text>
          </view>
          <text class="guide-action">去回执</text>
        </view>
        <view class="guide-row" :class="{ disabled: !showGiftEntry }" @tap="showGiftEntry ? openGift('ONLINE_GIFT') : showGiftDisabled()">
          <text class="guide-index">2</text>
          <view class="guide-main">
            <text class="guide-title">{{ activeTheme.onlineGiftLabel }}</text>
            <text class="guide-desc">{{ giftGuideDesc }}</text>
          </view>
          <text class="guide-action">{{ giftGuideAction }}</text>
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
        <button class="primary-action" @tap="openRsvp">回执出席</button>
        <button class="secondary-action" v-if="showGiftEntry" @tap="openGift('ONLINE_GIFT')">{{ activeTheme.onlineGiftLabel }}</button>
        <button class="secondary-action disabled" v-else @tap="showGiftDisabled">{{ activeTheme.onlineGiftLabel }}</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { onShareAppMessage } from '@dcloudio/uni-app';
import { loadRuntimeFeatures, request, type RuntimeFeatures } from '../../../api/client';
import { eventThemeFor, eventToneClass } from '../../../utils/event-theme';

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
const activeTheme = computed(() => eventThemeFor(eventType.value));
const eventTone = computed(() => eventToneClass(eventType.value));
const greeting = computed(() => basicFields.value.greeting || data.value?.templatePresentation?.defaultGreeting || activeTheme.value.defaultBlessing);
const invitationCopy = computed(() => activeTheme.value.invitationCopy);
const pageHeadline = computed(() => data.value?.templatePresentation?.headline || activeTheme.value.invitationTitle);
const coverMark = computed(() => data.value?.templatePresentation?.fallbackCoverLabel || activeTheme.value.mark);
const scheduleItems = computed(() => (basicFields.value.scheduleText || data.value?.templatePresentation?.defaultScheduleText || '')
  .split(/\r?\n/)
  .map((item) => item.trim())
  .filter(Boolean));
const showGiftEntry = computed(() => basicFields.value.showGiftEntry !== '0' && features.value.mockPaymentEnabled);
const giftGuideDesc = computed(() => showGiftEntry.value
  ? `${activeTheme.value.onlineGiftLabel}和现场扫码共用统一支付能力。`
  : `当前先开放回执流程，${activeTheme.value.onlineGiftLabel}待支付配置完成后开启。`);
const giftGuideAction = computed(() => showGiftEntry.value ? activeTheme.value.giftActionLabel : '未开放');
const disabledEntryMessages = computed(() => {
  const messages: string[] = [];
  if (!showGiftEntry.value) {
    messages.push(`${activeTheme.value.onlineGiftLabel}暂未开放，可先提交回执。`);
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
  '--primary': data.value?.theme?.primaryColor || undefined,
  '--secondary': data.value?.theme?.secondaryColor || undefined
}));

function defaultCover(type: string) {
  return '';
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
    uni.showToast({ title: '请柬信息未加载', icon: 'none' });
    return;
  }
  const fallbackUrl = `/pages/rsvp/submit/index?banquetId=${data.value.banquet.id}&invitationId=${data.value.invitation.id}&shareUrl=${encodeURIComponent(currentSharePath())}`;
  safeNavigate(
    withShareUrl(data.value.actionUrls?.rsvp || fallbackUrl),
    '回执页面打开失败'
  );
}

function openGift(entrySource: string) {
  if (!data.value) {
    uni.showToast({ title: '请柬信息未加载', icon: 'none' });
    return;
  }
  const url = entrySource === 'ONSITE_QR' ? data.value.actionUrls?.onsiteGift : data.value.actionUrls?.onlineGift;
  safeNavigate(withShareUrl(url || `/pages/gift/pay/index?banquetId=${data.value.banquet.id}&entrySource=${entrySource}`), `${activeTheme.value.giftLabel}页面打开失败`);
}

function safeNavigate(url: string, failTitle: string) {
  uni.navigateTo({
    url,
    fail: () => {
      uni.redirectTo({
        url,
        fail: () => uni.showToast({ title: failTitle, icon: 'none' })
      });
    }
  });
}

function currentSharePath() {
  return slug.value ? `/pages/invite/public/index?slug=${encodeURIComponent(slug.value)}` : '/pages/home/index/index';
}

function withShareUrl(url: string) {
  if (url.includes('shareUrl=')) {
    return url;
  }
  const separator = url.includes('?') ? '&' : '?';
  return `${url}${separator}shareUrl=${encodeURIComponent(currentSharePath())}`;
}

function showGiftDisabled() {
  uni.showToast({ title: `${activeTheme.value.onlineGiftLabel}需完成微信支付配置后开放`, icon: 'none' });
}

function showMapTip() {
  const address = basicFields.value.addressDetail || data.value?.banquet.location;
  if (!address) {
    uni.showToast({ title: '暂无可复制地址', icon: 'none' });
    return;
  }
  uni.setClipboardData({
    data: address,
    success: () => uni.showToast({ title: '地址已复制，可打开地图导航', icon: 'success' })
  });
}

function showContactTip() {
  const phone = basicFields.value.contactPhone;
  if (!phone) {
    uni.showToast({ title: '暂无联系电话', icon: 'none' });
    return;
  }
  uni.setClipboardData({
    data: phone,
    success: () => uni.showToast({ title: '电话已复制', icon: 'success' }),
    fail: () => uni.showToast({ title: '复制失败', icon: 'none' })
  });
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
  --primary: #e60012;
  --primary-dark: #c40005;
  --secondary: #f6c26b;
  --soft-bg: #fff8ef;
  --accent-shadow: rgba(230, 0, 18, 0.2);
  min-height: 100vh;
  background: var(--soft-bg);
  color: #171c2a;
}

.tone-birthday {
  --primary: #d96a11;
  --primary-dark: #a64209;
  --secondary: #ffd892;
  --soft-bg: #fff9f0;
  --accent-shadow: rgba(217, 106, 17, 0.2);
}

.tone-baby {
  --primary: #e7566f;
  --primary-dark: #b52d4c;
  --secondary: #ffc4d1;
  --soft-bg: #fff8fa;
  --accent-shadow: rgba(231, 86, 111, 0.2);
}

.tone-house {
  --primary: #188356;
  --primary-dark: #0c5f3e;
  --secondary: #b7ebc8;
  --soft-bg: #f7fcf8;
  --accent-shadow: rgba(24, 131, 86, 0.2);
}

.tone-school {
  --primary: #2563eb;
  --primary-dark: #1d4ed8;
  --secondary: #bdd7ff;
  --soft-bg: #f7fbff;
  --accent-shadow: rgba(37, 99, 235, 0.2);
}

.tone-memorial {
  --primary: #2f3338;
  --primary-dark: #0d0f12;
  --secondary: #d8d0c2;
  --soft-bg: #0f1113;
  --accent-shadow: rgba(0, 0, 0, 0.28);
}

.tone-other {
  --primary: #7c3aed;
  --primary-dark: #5b21b6;
  --secondary: #dac8ff;
  --soft-bg: #fbf8ff;
  --accent-shadow: rgba(124, 58, 237, 0.2);
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
  background: linear-gradient(135deg, var(--primary), var(--primary));
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
    radial-gradient(circle at 50% -120rpx, var(--accent-shadow), transparent 420rpx),
    var(--soft-bg);
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6rpx 4rpx 22rpx;
}

.brand {
  color: var(--primary);
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
  color: var(--primary);
  font-size: 24rpx;
  font-weight: 800;
  line-height: 58rpx;
}

.cover-card {
  position: relative;
  overflow: hidden;
  height: 760rpx;
  border-radius: 28rpx;
  background: linear-gradient(135deg, var(--primary), var(--primary-dark));
  box-shadow: 0 18rpx 48rpx var(--accent-shadow);
}

.cover-image {
  width: 100%;
  height: 100%;
  display: block;
}

.cover-fallback {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  background:
    radial-gradient(circle at 78% 20%, rgba(255, 255, 255, 0.2), transparent 220rpx),
    linear-gradient(135deg, var(--primary), var(--primary-dark));
}

.cover-fallback-mark {
  color: rgba(255, 255, 255, 0.18);
  font-family: serif;
  font-size: 260rpx;
  font-weight: 900;
}

.cover-mask {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(180deg, rgba(0, 0, 0, 0.08), rgba(0, 0, 0, 0.38)),
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
  color: var(--primary);
  background: rgba(255, 247, 225, 0.86);
  font-family: serif;
  font-size: 96rpx;
  font-weight: 900;
}

.headline {
  display: block;
  color: var(--primary-dark);
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
  color: var(--primary-dark);
  font-family: serif;
  font-size: 48rpx;
  font-weight: 900;
  line-height: 1.25;
}

.intro-card,
.info-card,
.quick-card,
.entry-guide,
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
  background: var(--soft-bg);
  color: var(--primary);
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
  background: var(--soft-bg);
  color: var(--primary);
  font-size: 22rpx;
  font-weight: 900;
}

.entry-guide {
  padding: 28rpx;
}

.guide-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  margin-bottom: 18rpx;
}

.guide-head .section-title {
  margin-bottom: 0;
}

.guide-note {
  color: #9b806a;
  font-size: 23rpx;
  font-weight: 800;
}

.guide-row {
  display: grid;
  grid-template-columns: 48rpx 1fr auto;
  gap: 16rpx;
  align-items: center;
  min-height: 94rpx;
  padding: 18rpx 0;
  border-top: 1rpx solid #f0dfcf;
}

.guide-row.disabled {
  opacity: 0.76;
}

.guide-index {
  width: 42rpx;
  height: 42rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--primary), var(--primary));
  color: #fff;
  font-size: 22rpx;
  font-weight: 900;
  line-height: 42rpx;
  text-align: center;
}

.guide-main {
  min-width: 0;
}

.guide-title,
.guide-desc {
  display: block;
}

.guide-title {
  color: #171c2a;
  font-size: 28rpx;
  font-weight: 900;
}

.guide-desc {
  margin-top: 6rpx;
  color: #7f7167;
  font-size: 23rpx;
  line-height: 1.4;
}

.guide-action {
  color: var(--primary);
  font-size: 24rpx;
  font-weight: 900;
  white-space: nowrap;
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
  background: var(--primary);
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
  background: linear-gradient(135deg, var(--primary), var(--primary));
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
