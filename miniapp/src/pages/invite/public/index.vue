<template>
  <view class="page state-page" v-if="pageState !== 'ready'">
    <view class="state-card">
      <text class="state-title">{{ stateTitle }}</text>
      <text class="state-text">{{ stateText }}</text>
      <button v-if="pageState === 'error'" @click="loadInvitation">重新加载</button>
    </view>
  </view>
  <view class="page" v-else-if="data" :class="templateClass" :style="pageStyle">
    <view class="hero">
      <image v-if="coverUrl" class="cover" :src="coverUrl" mode="aspectFill" />
      <view v-else class="cover-fallback">
        <text>{{ data.templatePresentation?.fallbackCoverLabel || '宴' }}</text>
      </view>
      <view class="hero-content">
        <text class="template-name">{{ data.template?.name || '基础请柬' }}</text>
        <text class="title">{{ data.invitation.title }}</text>
        <text class="subtitle">{{ greeting }}</text>
      </view>
    </view>
    <view class="notice warning" v-if="data.templateAvailable === false">
      <text>{{ data.templateMessage || '原请柬模板已不可用，当前使用基础样式展示' }}</text>
    </view>
    <view class="meta-grid">
      <view class="section" v-if="basicFields.hostName">
        <text class="label">主办人</text>
        <text class="value">{{ basicFields.hostName }}</text>
      </view>
      <view class="section" v-if="basicFields.contactPhone">
        <text class="label">联系电话</text>
        <text class="value">{{ basicFields.contactPhone }}</text>
      </view>
      <view class="section">
        <text class="label">宴席类型</text>
        <text class="value">{{ data.banquet.eventTypeCode }}</text>
      </view>
      <view class="section">
        <text class="label">宴席时间</text>
        <text class="value">{{ formatTime(data.banquet.banquetTime) }}</text>
      </view>
      <view class="section">
        <text class="label">宴席地点</text>
        <text class="value">{{ data.banquet.location || '敬请光临' }}</text>
      </view>
      <view class="section" v-if="basicFields.addressDetail">
        <text class="label">地址详情</text>
        <text class="value">{{ basicFields.addressDetail }}</text>
      </view>
    </view>
    <view class="timeline" v-if="scheduleItems.length">
      <text class="section-title">宴席流程</text>
      <view v-for="item in scheduleItems" :key="item" class="timeline-item">
        <text>{{ item }}</text>
      </view>
    </view>
    <view class="copywriting">
      <text class="copy-title">{{ data.giftSuccessCopywriting.title || '心意文案' }}</text>
      <text class="copy-content">{{ data.giftSuccessCopywriting.content }}</text>
    </view>
    <view class="share-line" v-if="data.shareUrl">
      <text>分享路径：{{ data.shareUrl }}</text>
    </view>
    <view class="notice" v-if="disabledEntryMessages.length">
      <text v-for="item in disabledEntryMessages" :key="item">{{ item }}</text>
    </view>
    <view class="actions">
      <button type="primary" @click="openRsvp">填写回执</button>
      <button v-if="showGiftEntry" @click="openGift('ONLINE_GIFT')">线上随礼</button>
      <button v-if="showGiftEntry" @click="openGift('ONSITE_QR')">现场扫码</button>
      <button v-if="showDeviceEntry" @click="openDevice">设备租赁</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { request } from '../../../api/client';

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
const greeting = computed(() => basicFields.value.greeting || data.value?.templatePresentation?.defaultGreeting || '诚邀您拨冗赴宴，共同见证这份重要时刻');
const scheduleItems = computed(() => (basicFields.value.scheduleText || data.value?.templatePresentation?.defaultScheduleText || '')
  .split(/\r?\n/)
  .map((item) => item.trim())
  .filter(Boolean));
const showGiftEntry = computed(() => basicFields.value.showGiftEntry !== '0');
const showDeviceEntry = computed(() => basicFields.value.showDeviceEntry !== '0');
const disabledEntryMessages = computed(() => {
  const messages: string[] = [];
  if (!showGiftEntry.value) {
    messages.push('随礼入口暂未开放');
  }
  if (!showDeviceEntry.value) {
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
const coverUrl = computed(() => data.value?.invitation.coverUrl || data.value?.template?.coverUrl || '');
const templateClass = computed(() => {
  const style = data.value?.templatePresentation?.styleCode || '';
  if (style) {
    return `template-${style}`;
  }
  const code = data.value?.template?.templateCode || '';
  const type = data.value?.template?.typeCode || 'FREE';
  if (code.includes('WEDDING')) {
    return 'template-wedding';
  }
  if (type === 'PAID' || type === 'CUSTOM') {
    return 'template-premium';
  }
  return 'template-general';
});
const pageStyle = computed(() => ({
  '--primary': data.value?.theme?.primaryColor || '#b91c1c',
  '--secondary': data.value?.theme?.secondaryColor || '#facc15'
}));

function formatTime(value?: string) {
  return value ? value.replace('T', ' ') : '时间待定';
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

function openDevice() {
  if (!data.value) {
    return;
  }
  uni.navigateTo({ url: data.value.actionUrls?.device || `/pages/device/select/index?banquetId=${data.value.banquet.id}` });
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
    data.value = await request<PublicInvitation>(`/invitations/public/${encodeURIComponent(slug.value)}`);
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
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 28rpx;
  background: #f8fafc;
  color: #111827;
}

.state-page {
  display: grid;
  place-items: center;
}

.state-card {
  display: grid;
  gap: 18rpx;
  width: 100%;
  padding: 48rpx 34rpx;
  border: 1rpx solid #e5e7eb;
  border-radius: 8rpx;
  background: #fff;
  text-align: center;
}

.state-title {
  color: #111827;
  font-size: 36rpx;
  font-weight: 600;
}

.state-text {
  color: #64748b;
  line-height: 1.6;
}

.notice {
  display: grid;
  gap: 8rpx;
  margin-top: 20rpx;
  padding: 20rpx 24rpx;
  border: 1rpx solid #dbeafe;
  border-radius: 8rpx;
  background: #eff6ff;
  color: #1e40af;
  font-size: 24rpx;
}

.notice.warning {
  border-color: #fde68a;
  background: #fffbeb;
  color: #92400e;
}

.hero {
  overflow: hidden;
  color: #fff;
  border-radius: 8rpx;
  background: var(--primary);
}

.cover {
  width: 100%;
  height: 320rpx;
  display: block;
}

.cover-fallback {
  height: 320rpx;
  display: grid;
  place-items: center;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.18), rgba(255, 255, 255, 0)),
    linear-gradient(135deg, var(--primary), #334155);
}

.cover-fallback text {
  width: 150rpx;
  height: 150rpx;
  display: grid;
  place-items: center;
  border: 4rpx solid rgba(255, 255, 255, 0.72);
  border-radius: 50%;
  color: #fff;
  font-size: 72rpx;
  font-weight: 700;
}

.hero-content {
  padding: 42rpx 28rpx 46rpx;
}

.template-name {
  display: block;
  margin-bottom: 18rpx;
  color: rgba(255, 255, 255, 0.82);
  font-size: 24rpx;
}

.title {
  display: block;
  font-size: 44rpx;
  font-weight: 600;
}

.subtitle {
  display: block;
  margin-top: 16rpx;
  line-height: 1.6;
  color: rgba(255, 255, 255, 0.92);
}

.meta-grid {
  display: grid;
  gap: 18rpx;
  margin: 28rpx 0;
}

.section {
  padding: 24rpx;
  border: 1rpx solid #e5e7eb;
  border-radius: 8rpx;
  background: #fff;
}

.section-title {
  display: block;
  margin-bottom: 18rpx;
  color: #111827;
  font-size: 30rpx;
  font-weight: 600;
}

.timeline {
  margin-bottom: 28rpx;
  padding: 26rpx;
  border: 1rpx solid #e5e7eb;
  border-radius: 8rpx;
  background: #fff;
}

.timeline-item {
  padding: 16rpx 0 16rpx 24rpx;
  border-left: 6rpx solid var(--secondary);
  color: #374151;
}

.timeline-item + .timeline-item {
  border-top: 1rpx solid #f1f5f9;
}

.label {
  display: block;
  margin-bottom: 8rpx;
  color: #64748b;
  font-size: 24rpx;
}

.value {
  display: block;
  line-height: 1.5;
  color: #111827;
}

.copywriting {
  padding: 30rpx 26rpx;
  border-left: 8rpx solid var(--secondary);
  border-radius: 8rpx;
  background: #fff;
}

.copy-title {
  display: block;
  color: var(--primary);
  font-size: 28rpx;
  font-weight: 600;
}

.copy-content {
  display: block;
  margin-top: 12rpx;
  line-height: 1.7;
}

.share-line {
  margin-top: 20rpx;
  padding: 20rpx 24rpx;
  border: 1rpx solid #e5e7eb;
  border-radius: 8rpx;
  background: #fff;
  color: #64748b;
  font-size: 24rpx;
  word-break: break-all;
}

.actions {
  display: grid;
  gap: 16rpx;
  margin-top: 36rpx;
}

.template-wedding .hero {
  background: linear-gradient(135deg, var(--primary), #7f1d1d);
}

.template-wedding-red-gold .hero,
.template-birthday-warm .hero {
  background: linear-gradient(135deg, var(--primary), #7f1d1d);
}

.template-baby-garden .hero {
  background: linear-gradient(135deg, #0f766e, #f97316);
}

.template-house-modern .hero {
  background: linear-gradient(135deg, #334155, #ea580c);
}

.template-school-honor .hero {
  background: linear-gradient(135deg, #1d4ed8, #f59e0b);
}

.template-memorial-simple .hero {
  background: linear-gradient(135deg, #111827, #6b7280);
}

.template-premium .hero {
  background: linear-gradient(135deg, #111827, var(--primary));
}

.template-general .hero {
  background: linear-gradient(135deg, var(--primary), #334155);
}
</style>
