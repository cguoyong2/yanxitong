<template>
  <view class="page">
    <view class="success-card">
      <view class="lantern"></view>
      <view class="flower"></view>
      <text class="brand">宴席通</text>
      <text class="success-title">{{ features.mockPaymentEnabled ? '模拟支付完成' : '支付成功' }}</text>
      <text class="guest-name">{{ guestName || '宾客' }}</text>
      <text class="gift-label">{{ activeTheme.giftLabel }}</text>
      <view class="amount-row">
        <text class="currency">¥</text>
        <text class="amount">{{ displayAmount }}</text>
      </view>
      <text class="time-text">{{ currentTime }}</text>
      <text class="hint-text">{{ successHint }}</text>
    </view>

    <view class="order-card">
      <view class="order-row">
        <text class="order-label">订单号</text>
        <text class="order-value">{{ orderNo || '-' }}</text>
      </view>
      <view class="order-row">
        <text class="order-label">处理状态</text>
        <text class="status" :class="{ done: confirmed }">{{ confirmed ? '已入账' : '待确认' }}</text>
      </view>
    </view>

    <view class="actions">
      <button v-if="features.mockPaymentEnabled" class="primary-button" :loading="submitting" @tap="confirmSuccess">
        {{ confirmed ? '已模拟入账' : '模拟支付成功入账' }}
      </button>
      <button v-if="banquetId" class="ghost-button" @tap="openGiftList">查看{{ activeTheme.giftRecordLabel }}</button>
      <button v-if="shareUrl" class="ghost-button" @tap="backToInvitation">返回请柬</button>
      <button class="text-button" @tap="goBack">返回</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { loadRuntimeFeatures, request, type RuntimeFeatures } from '../../../api/client';
import { eventThemeFor, fetchBanquetEventType, readActiveEventType, writeActiveEventType } from '../../../utils/event-theme';

const orderNo = ref('');
const banquetId = ref('');
const guestName = ref('');
const shareUrl = ref('');
const amount = ref(0);
const submitting = ref(false);
const confirmed = ref(false);
const features = ref<RuntimeFeatures>({ mockPaymentEnabled: false });
const currentTime = ref('');
const eventType = ref(readActiveEventType());
const activeTheme = computed(() => eventThemeFor(eventType.value));
const displayAmount = computed(() => Number(amount.value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 }));
const successHint = computed(() => features.value.mockPaymentEnabled
  ? `点击下方按钮后写入${activeTheme.value.giftRecordLabel}，并触发确认屏/云喇叭模拟日志。`
  : `真实支付完成后，系统会自动写入${activeTheme.value.giftRecordLabel}并推送确认屏。`);

async function confirmSuccess() {
  if (!orderNo.value) {
    uni.showToast({ title: '缺少订单号', icon: 'none' });
    return;
  }
  if (confirmed.value) {
    uni.showToast({ title: '已入账', icon: 'success' });
    return;
  }
  submitting.value = true;
  try {
    await request(`/gifts/payment-orders/${orderNo.value}/mock-success`, { method: 'POST' });
    confirmed.value = true;
    uni.showToast({ title: '已入账', icon: 'success' });
  } finally {
    submitting.value = false;
  }
}

function openGiftList() {
  safeNavigate(`/pages/gift/list/index?banquetId=${banquetId.value}`, `${activeTheme.value.giftRecordLabel}打开失败`);
}

function backToInvitation() {
  if (!shareUrl.value) {
    goBack();
    return;
  }
  safeNavigate(shareUrl.value, '请柬页面打开失败');
}

function goBack() {
  uni.navigateBack();
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

function formatNow() {
  const now = new Date();
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;
}

onMounted(async () => {
  const pages = getCurrentPages();
  const current = pages[pages.length - 1] as unknown as { options?: Record<string, string> };
  orderNo.value = current.options?.orderNo || '';
  banquetId.value = current.options?.banquetId || '';
  guestName.value = current.options?.guestName ? decodeURIComponent(current.options.guestName) : '';
  shareUrl.value = current.options?.shareUrl ? decodeURIComponent(current.options.shareUrl) : '';
  amount.value = Number(current.options?.amount || 0);
  currentTime.value = formatNow();
  if (banquetId.value) {
    eventType.value = writeActiveEventType(await fetchBanquetEventType(banquetId.value, request, eventType.value));
  }
  features.value = await loadRuntimeFeatures().catch(() => ({ mockPaymentEnabled: false }));
});
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 24rpx;
  background: #fff8ef;
  box-sizing: border-box;
  color: #171c2a;
}

.success-card {
  position: relative;
  overflow: hidden;
  min-height: 620rpx;
  padding: 54rpx 34rpx 42rpx;
  border-radius: 28rpx;
  background:
    radial-gradient(circle at 50% 38%, rgba(255, 216, 144, 0.22), transparent 250rpx),
    linear-gradient(135deg, #e71921 0%, #c9161c 62%, #9b0e13 100%);
  box-shadow: 0 18rpx 46rpx rgba(184, 17, 21, 0.24);
  text-align: center;
}

.lantern {
  position: absolute;
  left: 44rpx;
  top: -18rpx;
  width: 82rpx;
  height: 112rpx;
  border-radius: 50%;
  background: linear-gradient(145deg, #ffb164, #d6251d 70%);
  opacity: 0.75;
}

.flower {
  position: absolute;
  right: -76rpx;
  bottom: -80rpx;
  width: 270rpx;
  height: 270rpx;
  border-radius: 50%;
  background: rgba(255, 224, 170, 0.18);
}

.brand,
.success-title,
.guest-name,
.gift-label,
.amount-row,
.time-text,
.hint-text {
  position: relative;
  z-index: 2;
  display: block;
}

.brand {
  color: #ffe4bd;
  font-size: 28rpx;
  font-weight: 900;
}

.success-title {
  margin-top: 28rpx;
  color: #ffe8bd;
  font-family: serif;
  font-size: 56rpx;
  font-weight: 900;
}

.guest-name {
  margin-top: 34rpx;
  color: #fff;
  font-size: 68rpx;
  font-weight: 900;
}

.gift-label {
  margin-top: 20rpx;
  color: rgba(255, 248, 232, 0.92);
  font-size: 34rpx;
  font-weight: 800;
}

.amount-row {
  margin-top: 10rpx;
  color: #fff4cf;
  font-weight: 900;
}

.currency {
  font-size: 44rpx;
}

.amount {
  font-size: 96rpx;
}

.time-text {
  margin-top: 16rpx;
  color: #fff;
  font-size: 42rpx;
  font-weight: 800;
  letter-spacing: 0;
}

.hint-text {
  width: 86%;
  margin: 30rpx auto 0;
  color: rgba(255, 248, 232, 0.88);
  font-size: 25rpx;
  line-height: 1.55;
}

.order-card {
  margin-top: 24rpx;
  padding: 4rpx 28rpx;
  border: 1rpx solid #f0dfcf;
  border-radius: 24rpx;
  background: #fff;
  box-shadow: 0 12rpx 32rpx rgba(82, 45, 24, 0.07);
}

.order-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 92rpx;
  border-bottom: 1rpx solid #f0dfcf;
}

.order-row:last-child {
  border-bottom: 0;
}

.order-label {
  color: #7a6250;
  font-size: 26rpx;
  font-weight: 800;
}

.order-value {
  max-width: 460rpx;
  overflow: hidden;
  color: #171c2a;
  font-size: 24rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status {
  padding: 8rpx 18rpx;
  border-radius: 999rpx;
  background: #fff7ed;
  color: #c25a22;
  font-size: 24rpx;
  font-weight: 900;
}

.status.done {
  background: #ecfdf3;
  color: #138a45;
}

.actions {
  display: grid;
  gap: 18rpx;
  margin-top: 28rpx;
}

.primary-button,
.ghost-button,
.text-button {
  margin: 0;
  border-radius: 18rpx;
  font-size: 30rpx;
  font-weight: 900;
}

.primary-button {
  height: 92rpx;
  background: linear-gradient(135deg, #e83a32, #c91419);
  color: #fff;
  line-height: 92rpx;
}

.ghost-button {
  height: 88rpx;
  border: 1rpx solid #ead8ca;
  background: #fff;
  color: #9e4d32;
  line-height: 88rpx;
}

.text-button {
  height: 76rpx;
  background: transparent;
  color: #8d929d;
  line-height: 76rpx;
}

.primary-button::after,
.ghost-button::after,
.text-button::after {
  border: 0;
}
</style>
