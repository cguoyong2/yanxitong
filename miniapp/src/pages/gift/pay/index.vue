<template>
  <view class="page" :class="activeTheme.tone">
    <view class="hero-card" :class="{ onsite: isOnsiteQr }">
      <view class="hero-art">
        <text class="hero-symbol">礼</text>
      </view>
      <text class="hero-label">情礼记</text>
      <text class="hero-title">{{ pageTitle }}</text>
      <text class="hero-desc">{{ pageHint }}</text>
      <view class="entry-switch">
        <view
          v-for="(source, index) in sources"
          :key="source.value"
          class="entry-tab"
          :class="{ active: selectedIndex === index }"
          @tap="selectSource(index)"
        >
          {{ source.label }}
        </view>
      </view>
    </view>

    <view v-if="features.mockPaymentEnabled" class="notice-card compact">
      <text class="notice-title">体验支付模式</text>
      <text class="notice-text">当前可创建模拟支付订单，点击成功页按钮后写入{{ activeTheme.giftRecordLabel }}。</text>
    </view>

    <view class="amount-card">
      <text class="amount-label">{{ activeTheme.giftAmountLabel }}</text>
      <view class="amount-input-row">
        <text class="currency">¥</text>
        <input v-model.number="form.amount" class="amount-input" type="digit" placeholder="0" placeholder-class="amount-placeholder" />
      </view>
      <view class="quick-amounts">
        <view
          v-for="amount in quickAmounts"
          :key="amount"
          class="quick-amount"
          :class="{ active: Number(form.amount) === amount }"
          @tap="selectAmount(amount)"
        >
          ¥{{ amount }}
        </view>
      </view>
    </view>

    <view class="form-card">
      <view class="form-row">
        <text class="row-icon">人</text>
        <text class="row-label">宾客姓名</text>
        <input v-model="form.guestName" class="row-input" placeholder="请输入姓名" placeholder-class="placeholder" />
      </view>
      <view class="blessing-panel">
        <text class="panel-title">{{ activeTheme.blessingLabel }}</text>
        <view class="blessing-list">
          <view v-for="item in blessingTemplates" :key="item" class="blessing-chip" @tap="form.blessing = item">{{ item }}</view>
        </view>
        <textarea v-model="form.blessing" class="textarea" maxlength="120" :placeholder="activeTheme.blessingPlaceholder" placeholder-class="placeholder" />
        <text class="counter">{{ form.blessing.length }}/120</text>
      </view>
    </view>

    <view class="flow-card">
      <view class="flow-item">
        <text class="flow-dot">1</text>
        <text>创建统一支付订单</text>
      </view>
      <view class="flow-line"></view>
      <view class="flow-item">
        <text class="flow-dot">2</text>
        <text>支付回调写入{{ activeTheme.giftRecordLabel }}</text>
      </view>
      <view class="flow-line"></view>
      <view class="flow-item">
        <text class="flow-dot">3</text>
        <text>确认屏与云喇叭同步播报</text>
      </view>
    </view>

    <view class="footer-safe"></view>
    <view class="sticky-submit">
      <button class="primary-button" :loading="submitting" @tap="submit()">{{ submitText }}</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { loadRuntimeFeatures, request, type RuntimeFeatures } from '../../../api/client';
import { requireBanquetToast, resolveBanquetId } from '../../../utils/banquet';
import { eventThemeFor, fetchBanquetEventType, readActiveEventType, writeActiveEventType } from '../../../utils/event-theme';
import { getWechatOpenId, requestWechatPayment, type PaymentOrderResult } from '../../../utils/wechat-payment';

const eventType = ref(readActiveEventType());
const activeTheme = computed(() => eventThemeFor(eventType.value));
const sources = computed(() => [
  { label: activeTheme.value.onlineGiftLabel, value: 'ONLINE_GIFT' },
  { label: '现场扫码', value: 'ONSITE_QR' }
]);
const selectedIndex = ref(0);
const banquetId = ref('');
const shareUrl = ref('');
const submitting = ref(false);
const clientRequestId = ref('');
const features = ref<RuntimeFeatures>({ mockPaymentEnabled: false });
const quickAmounts = [66, 88, 188, 288, 520, 666, 888, 1314];
const blessingTemplates = computed(() => [
  activeTheme.value.defaultBlessing,
  activeTheme.value.blessingPlaceholder.replace(/^如：/, '').split('、')[0],
  activeTheme.value.rsvpSuccessText,
  '顺遂圆满，万事如意'
].filter(Boolean));
const form = reactive({
  guestName: '',
  amount: undefined as number | undefined,
  blessing: '',
  entrySource: 'ONLINE_GIFT'
});
const isOnsiteQr = computed(() => form.entrySource === 'ONSITE_QR');
const pageTitle = computed(() => isOnsiteQr.value ? `现场扫码${activeTheme.value.giftLabel}` : activeTheme.value.onlineGiftLabel);
const pageHint = computed(() => isOnsiteQr.value
  ? `现场扫码与${activeTheme.value.onlineGiftLabel}共用同一套在线支付能力。`
  : `填写姓名、金额和${activeTheme.value.blessingLabel}，生成统一${activeTheme.value.onlineGiftLabel}订单。`);
const submitText = computed(() => isOnsiteQr.value ? '确认现场扫码支付' : '确认支付');

function selectSource(index: number) {
  selectedIndex.value = index;
  form.entrySource = sources.value[index].value;
  if (!form.blessing) {
    form.blessing = activeTheme.value.defaultBlessing;
  }
}

function selectAmount(amount: number) {
  form.amount = amount;
}

async function submit() {
  if (!validate()) {
    return;
  }
  submitting.value = true;
  try {
    const payerOpenId = features.value.mockPaymentEnabled ? undefined : await getWechatOpenId();
    const result = await request<PaymentOrderResult>('/gifts/payment-orders', {
      method: 'POST',
      data: { ...form, banquetId: Number(banquetId.value), payerOpenId, clientRequestId: ensureClientRequestId() }
    });
    if (!features.value.mockPaymentEnabled) {
      await requestWechatPayment(result.payPayload);
      uni.showToast({ title: '支付已提交', icon: 'success' });
    }
    const share = shareUrl.value ? `&shareUrl=${encodeURIComponent(shareUrl.value)}` : '';
    safeNavigate(
      `/pages/gift/success/index?orderNo=${result.order.orderNo}&banquetId=${banquetId.value}&amount=${form.amount || 0}&guestName=${encodeURIComponent(form.guestName)}${share}`,
      `${activeTheme.value.giftLabel}成功页打开失败`
    );
  } finally {
    submitting.value = false;
  }
}

function ensureClientRequestId() {
  if (!clientRequestId.value) {
    clientRequestId.value = `gift-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
  }
  return clientRequestId.value;
}

function validate() {
  if (!banquetId.value) {
    uni.showToast({ title: '缺少宴席信息', icon: 'none' });
    return false;
  }
  if (!form.guestName.trim()) {
    uni.showToast({ title: '请填写姓名', icon: 'none' });
    return false;
  }
  if (!form.amount || Number(form.amount) <= 0) {
    uni.showToast({ title: '请选择或填写金额', icon: 'none' });
    return false;
  }
  return true;
}

function backToInvitation() {
  if (!shareUrl.value) {
    uni.navigateBack();
    return;
  }
  safeNavigate(shareUrl.value, '请柬页面打开失败');
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

onMounted(async () => {
  const pages = getCurrentPages();
  const current = pages[pages.length - 1] as unknown as { options?: Record<string, string> };
  banquetId.value = await resolveBanquetId(current.options?.banquetId);
  if (!banquetId.value) {
    requireBanquetToast();
  }
  form.entrySource = current.options?.entrySource || 'ONLINE_GIFT';
  form.guestName = current.options?.guestName ? decodeURIComponent(current.options.guestName) : '';
  form.amount = current.options?.amount ? Number(current.options.amount) : undefined;
  shareUrl.value = current.options?.shareUrl ? decodeURIComponent(current.options.shareUrl) : '';
  selectedIndex.value = form.entrySource === 'ONSITE_QR' ? 1 : 0;
  if (banquetId.value) {
    eventType.value = writeActiveEventType(await fetchBanquetEventType(banquetId.value, request, eventType.value));
  }
  if (!form.blessing) {
    form.blessing = activeTheme.value.defaultBlessing;
  }
  features.value = await loadRuntimeFeatures().catch(() => ({ mockPaymentEnabled: false }));
});
</script>

<style scoped>
.page {
  --accent: #e60012;
  --accent-dark: #c40005;
  --accent-soft: #fff0ee;
  --page-bg: #fff8ef;
  --accent-shadow: rgba(230, 0, 18, 0.2);
  min-height: 100vh;
  padding: 24rpx 24rpx 0;
  background: var(--page-bg);
  box-sizing: border-box;
  color: #171c2a;
}

.page.orange {
  --accent: #d96a11;
  --accent-dark: #a64209;
  --accent-soft: #fff3e3;
  --page-bg: #fff9f0;
  --accent-shadow: rgba(217, 106, 17, 0.2);
}

.page.pink {
  --accent: #e7566f;
  --accent-dark: #b52d4c;
  --accent-soft: #fff0f4;
  --page-bg: #fff8fa;
  --accent-shadow: rgba(231, 86, 111, 0.2);
}

.page.green {
  --accent: #188356;
  --accent-dark: #0c5f3e;
  --accent-soft: #edf9f1;
  --page-bg: #f7fcf8;
  --accent-shadow: rgba(24, 131, 86, 0.2);
}

.page.blue {
  --accent: #2563eb;
  --accent-dark: #1d4ed8;
  --accent-soft: #edf4ff;
  --page-bg: #f7fbff;
  --accent-shadow: rgba(37, 99, 235, 0.2);
}

.page.black {
  --accent: #2f3338;
  --accent-dark: #0d0f12;
  --accent-soft: #f1f2f4;
  --page-bg: #f7f7f7;
  --accent-shadow: rgba(47, 51, 56, 0.2);
}

.page.purple {
  --accent: #7c3aed;
  --accent-dark: #5b21b6;
  --accent-soft: #f4efff;
  --page-bg: #fbf8ff;
  --accent-shadow: rgba(124, 58, 237, 0.2);
}

.hero-card {
  position: relative;
  overflow: hidden;
  padding: 34rpx;
  border-radius: 28rpx;
  background:
    radial-gradient(circle at 84% 20%, rgba(255, 217, 150, 0.38), transparent 180rpx),
    linear-gradient(135deg, var(--accent) 0%, var(--accent-dark) 62%, var(--accent-dark) 100%);
  box-shadow: 0 16rpx 42rpx var(--accent-shadow);
}

.hero-card.onsite {
  background:
    radial-gradient(circle at 84% 20%, rgba(255, 229, 180, 0.42), transparent 180rpx),
    linear-gradient(135deg, var(--accent) 0%, var(--accent-dark) 62%, var(--accent-dark) 100%);
}

.hero-art {
  position: absolute;
  right: -36rpx;
  bottom: -60rpx;
  width: 250rpx;
  height: 250rpx;
  border-radius: 50%;
  background: rgba(255, 224, 170, 0.16);
}

.hero-symbol {
  position: absolute;
  right: 76rpx;
  bottom: 70rpx;
  color: rgba(255, 239, 206, 0.34);
  font-family: serif;
  font-size: 92rpx;
  font-weight: 900;
}

.hero-label,
.hero-title,
.hero-desc,
.entry-switch {
  position: relative;
  z-index: 2;
}

.hero-label {
  display: block;
  color: #ffe2ba;
  font-size: 26rpx;
  font-weight: 800;
}

.hero-title {
  display: block;
  margin-top: 14rpx;
  color: #fff8df;
  font-family: serif;
  font-size: 58rpx;
  font-weight: 900;
}

.hero-desc {
  display: block;
  width: 78%;
  margin-top: 12rpx;
  color: rgba(255, 248, 232, 0.94);
  font-size: 27rpx;
  line-height: 1.5;
}

.entry-switch {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14rpx;
  margin-top: 28rpx;
  padding: 8rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.15);
}

.entry-tab {
  height: 62rpx;
  border-radius: 999rpx;
  color: rgba(255, 245, 224, 0.82);
  font-size: 26rpx;
  font-weight: 800;
  line-height: 62rpx;
  text-align: center;
}

.entry-tab.active {
  background: #fff7e9;
  color: var(--accent);
}

.notice-card,
.amount-card,
.form-card,
.flow-card {
  margin-top: 24rpx;
  border: 1rpx solid #f0dfcf;
  border-radius: 24rpx;
  background: #fff;
  box-shadow: 0 12rpx 32rpx rgba(82, 45, 24, 0.07);
}

.notice-card {
  padding: 34rpx;
}

.notice-card.compact {
  padding: 24rpx 28rpx;
}

.notice-title {
  display: block;
  color: #171c2a;
  font-size: 34rpx;
  font-weight: 900;
}

.notice-text {
  display: block;
  margin-top: 14rpx;
  color: #9a5b30;
  font-size: 27rpx;
  line-height: 1.6;
}

.primary-button::after {
  border: 0;
}

.amount-card {
  padding: 30rpx;
}

.amount-label {
  display: block;
  color: #7b5a45;
  font-size: 26rpx;
  font-weight: 700;
}

.amount-input-row {
  display: flex;
  align-items: center;
  gap: 14rpx;
  height: 112rpx;
  border-bottom: 1rpx solid #f0dfcf;
}

.currency {
  color: var(--accent);
  font-size: 44rpx;
  font-weight: 900;
}

.amount-input {
  flex: 1;
  height: 104rpx;
  color: var(--accent);
  font-size: 70rpx;
  font-weight: 900;
}

.amount-placeholder {
  color: #e9c8be;
}

.quick-amounts {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14rpx;
  margin-top: 24rpx;
}

.quick-amount {
  height: 62rpx;
  border: 1rpx solid #efd9c7;
  border-radius: 999rpx;
  background: var(--accent-soft);
  color: var(--accent-dark);
  font-size: 25rpx;
  font-weight: 800;
  line-height: 62rpx;
  text-align: center;
}

.quick-amount.active {
  border-color: transparent;
  background: var(--accent-soft);
  color: var(--accent);
}

.form-card {
  overflow: hidden;
}

.form-row {
  display: grid;
  grid-template-columns: 54rpx 156rpx 1fr;
  align-items: center;
  min-height: 106rpx;
  padding: 0 28rpx;
  border-bottom: 1rpx solid #f0dfcf;
  box-sizing: border-box;
}

.row-icon {
  display: grid;
  place-items: center;
  width: 34rpx;
  height: 34rpx;
  border-radius: 50%;
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 20rpx;
  font-weight: 800;
}

.row-label,
.panel-title {
  color: #171c2a;
  font-size: 30rpx;
  font-weight: 900;
}

.row-input {
  height: 86rpx;
  color: #171c2a;
  font-size: 28rpx;
}

.placeholder {
  color: #b8afa7;
}

.blessing-panel {
  position: relative;
  padding: 28rpx;
}

.panel-title {
  display: block;
}

.blessing-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14rpx;
  margin-top: 18rpx;
}

.blessing-chip {
  min-height: 62rpx;
  padding: 12rpx 14rpx;
  border: 1rpx solid #efd9c7;
  border-radius: 16rpx;
  background: var(--accent-soft);
  color: var(--accent-dark);
  font-size: 24rpx;
  line-height: 1.35;
  text-align: center;
}

.textarea {
  box-sizing: border-box;
  width: 100%;
  min-height: 150rpx;
  margin-top: 18rpx;
  padding: 20rpx;
  border: 1rpx solid #efd9c7;
  border-radius: 18rpx;
  background: #fffdfb;
  color: #171c2a;
  font-size: 26rpx;
  line-height: 1.5;
}

.counter {
  position: absolute;
  right: 48rpx;
  bottom: 46rpx;
  color: #b8afa7;
  font-size: 22rpx;
}

.flow-card {
  padding: 26rpx 28rpx;
}

.flow-item {
  display: flex;
  align-items: center;
  gap: 16rpx;
  color: #5f6573;
  font-size: 26rpx;
  font-weight: 700;
}

.flow-dot {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 22rpx;
  font-weight: 900;
  line-height: 40rpx;
  text-align: center;
}

.flow-line {
  width: 2rpx;
  height: 24rpx;
  margin: 6rpx 0 6rpx 19rpx;
  background: #f0dfcf;
}

.footer-safe {
  height: 142rpx;
}

.sticky-submit {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 20;
  padding: 18rpx 24rpx calc(18rpx + env(safe-area-inset-bottom));
  background: rgba(255, 248, 239, 0.96);
  box-shadow: 0 -8rpx 28rpx rgba(72, 45, 24, 0.08);
}

.primary-button {
  height: 94rpx;
  margin: 0;
  border-radius: 18rpx;
  background: linear-gradient(135deg, var(--accent), var(--accent-dark));
  color: #fff;
  font-size: 31rpx;
  font-weight: 900;
  line-height: 94rpx;
}
</style>
