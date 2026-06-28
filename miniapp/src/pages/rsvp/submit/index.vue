<template>
  <view class="page" v-if="!submitted">
    <view class="hero">
      <view class="hero-lantern"></view>
      <view class="hero-flower flower-a"></view>
      <view class="hero-flower flower-b"></view>
      <text class="hero-title">宾客回执</text>
      <text class="hero-subtitle">{{ activeTheme.rsvpSubtitle }}</text>
      <view class="hero-divider"></view>
    </view>

    <view class="form-card">
      <view class="form-row">
        <text class="field-label">姓名 <text class="required">*</text></text>
        <input v-model="form.guestName" class="field-input" placeholder="请输入您的姓名" placeholder-class="placeholder" />
      </view>
      <view class="form-row">
        <text class="field-label">手机号</text>
        <input v-model="form.phone" class="field-input" type="number" maxlength="11" placeholder="用于识别重复提交" placeholder-class="placeholder" />
      </view>
      <view class="form-row">
        <text class="field-label">是否出席 <text class="required">*</text></text>
        <view class="segmented">
          <view
            v-for="item in statuses"
            :key="item.value"
            class="segment"
            :class="{ active: form.attendanceStatus === item.value }"
            @tap="selectStatus(item.value)"
          >
            {{ item.shortLabel }}
          </view>
        </view>
      </view>

      <view v-if="isAttending" class="form-row">
        <text class="field-label">是否用餐 <text class="required">*</text></text>
        <view class="segmented two">
          <view class="segment" :class="{ active: form.mealRequired === 1 }" @tap="form.mealRequired = 1">用餐</view>
          <view class="segment" :class="{ active: form.mealRequired === 0 }" @tap="form.mealRequired = 0">不用餐</view>
        </view>
      </view>

      <view v-if="isAttending" class="form-row">
        <text class="field-label">用餐人数 <text class="required">*</text></text>
        <view class="stepper">
          <view class="step" @tap="changeGuestCount(-1)">-</view>
          <input v-model.number="form.guestCount" class="step-input" type="number" />
          <view class="step" @tap="changeGuestCount(1)">+</view>
        </view>
      </view>

      <view v-if="isAttending" class="form-row">
        <text class="field-label">是否住宿 <text class="required">*</text></text>
        <view class="segmented two">
          <view class="segment" :class="{ active: form.accommodationRequired === 1 }" @tap="form.accommodationRequired = 1">住宿</view>
          <view class="segment" :class="{ active: form.accommodationRequired === 0 }" @tap="form.accommodationRequired = 0">不住宿</view>
        </view>
      </view>

      <view v-if="!isAttending" class="decline-note">
        <text>{{ declineText }}</text>
      </view>

      <view class="message-block">
        <text class="field-label">留言</text>
        <textarea
          v-model="form.message"
          class="message-input"
          maxlength="200"
          placeholder="请输入您的留言（选填）"
          placeholder-class="placeholder"
        />
        <text class="counter">{{ form.message.length }}/200</text>
      </view>
    </view>

    <view class="privacy-tip">
      <text>信息仅用于宴席管理，主办方可查看您的回执。</text>
    </view>

    <view class="footer-safe"></view>
    <view class="sticky-submit">
      <button class="primary-button" :loading="submitting" @tap="submit">提交回执</button>
    </view>
  </view>

  <view class="page success-page" v-else>
    <view class="success-hero">
      <text class="success-icon">✓</text>
      <text class="success-title">{{ submitResult?.created ? '回执已提交' : '回执已更新' }}</text>
      <text class="success-text">{{ successText }}</text>
    </view>
    <view class="result-card">
      <view class="result-head">
        <text class="result-title">回执摘要</text>
        <text class="result-status">{{ currentStatusLabel }}</text>
      </view>
      <view class="result-row">
        <text class="result-label">宾客姓名</text>
        <text class="result-value">{{ form.guestName }}</text>
      </view>
      <view class="result-row" v-if="form.phone">
        <text class="result-label">联系电话</text>
        <text class="result-value">{{ form.phone }}</text>
      </view>
      <view class="result-row" v-if="isSubmittedAttending">
        <text class="result-label">出席人数</text>
        <text class="result-value">{{ submitResult?.guestCount || form.guestCount }} 人</text>
      </view>
      <view class="result-row" v-if="isSubmittedAttending">
        <text class="result-label">用餐 / 住宿</text>
        <text class="result-value">{{ form.mealRequired ? '用餐' : '不用餐' }} · {{ form.accommodationRequired ? '住宿' : '不住宿' }}</text>
      </view>
      <view class="result-message" v-if="form.message">
        <text>{{ form.message }}</text>
      </view>
    </view>
    <view class="success-actions">
      <button class="primary-button" @tap="openGift">{{ giftActionText }}</button>
      <button class="ghost-button" @tap="openRsvpStats">查看回执统计</button>
      <button class="ghost-button" @tap="backToInvitation">返回请柬</button>
      <button class="text-button" @tap="editAgain">修改回执</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { loadRuntimeFeatures, request, type RuntimeFeatures } from '../../../api/client';
import { requireBanquetToast, resolveBanquetId } from '../../../utils/banquet';
import { eventThemeFor, fetchBanquetEventType, readActiveEventType, writeActiveEventType } from '../../../utils/event-theme';

const statuses = [
  { label: '参加', shortLabel: '出席', value: 'ATTENDING' },
  { label: '待定', shortLabel: '待定', value: 'PENDING' },
  { label: '不参加', shortLabel: '不出席', value: 'DECLINED' }
];
const banquetId = ref('');
const invitationId = ref('');
const shareUrl = ref('');
const submitting = ref(false);
const submitted = ref(false);
const features = ref<RuntimeFeatures>({ mockPaymentEnabled: false });
const submitResult = ref<{ id: number; created?: boolean; attendanceStatus: string; guestCount: number }>();
const eventType = ref(readActiveEventType());
const activeTheme = computed(() => eventThemeFor(eventType.value));
const form = reactive({
  guestName: '',
  phone: '',
  attendanceStatus: 'ATTENDING',
  mealRequired: 1,
  accommodationRequired: 0,
  guestCount: 2,
  message: ''
});
const isAttending = computed(() => form.attendanceStatus === 'ATTENDING' || form.attendanceStatus === 'ATTEND');
const declineText = computed(() => {
  if (form.attendanceStatus === 'PENDING') {
    return '已为您记录为待定，之后可再次进入页面更新回执。';
  }
  return '已为您记录为不出席，不会统计用餐与住宿人数。';
});
const successText = computed(() => {
  if (!submitResult.value) {
    return '';
  }
  if (submitResult.value.attendanceStatus === 'DECLINED') {
    return '已记录您的回复，感谢告知。';
  }
  if (submitResult.value.attendanceStatus === 'PENDING') {
    return '已记录为待定，之后可再次提交更新。';
  }
  return activeTheme.value.rsvpSuccessText || `已记录 ${submitResult.value.guestCount || 1} 位来宾出席。`;
});
const giftActionText = computed(() => features.value.mockPaymentEnabled ? activeTheme.value.giftActionLabel : `去${activeTheme.value.offlineGiftLabel}`);
const currentStatusLabel = computed(() => {
  const value = submitResult.value?.attendanceStatus || form.attendanceStatus;
  return statuses.find((item) => item.value === value)?.label || '已提交';
});
const isSubmittedAttending = computed(() => {
  const value = submitResult.value?.attendanceStatus || form.attendanceStatus;
  return value === 'ATTENDING' || value === 'ATTEND';
});

function selectStatus(value: string) {
  form.attendanceStatus = value;
  if (!isAttending.value) {
    form.mealRequired = 0;
    form.accommodationRequired = 0;
    form.guestCount = 1;
    return;
  }
  form.mealRequired = 1;
  if (!form.guestCount || Number(form.guestCount) < 1) {
    form.guestCount = 1;
  }
}

function changeGuestCount(delta: number) {
  const next = Math.max(1, Number(form.guestCount || 1) + delta);
  form.guestCount = next;
}

async function submit() {
  if (!validate()) {
    return;
  }
  submitting.value = true;
  try {
    const result = await request<{ id: number; created?: boolean; attendanceStatus: string; guestCount: number }>('/rsvp/submit', {
      method: 'POST',
      data: {
        ...form,
        banquetId: Number(banquetId.value),
        invitationId: invitationId.value ? Number(invitationId.value) : undefined
      }
    });
    submitResult.value = result;
    submitted.value = true;
    uni.showToast({ title: result.created ? '已提交' : '已更新', icon: 'success' });
  } finally {
    submitting.value = false;
  }
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
  if (form.phone && !/^1[3-9]\d{9}$/.test(form.phone)) {
    uni.showToast({ title: '手机号格式不正确', icon: 'none' });
    return false;
  }
  if (isAttending.value && (!form.guestCount || Number(form.guestCount) < 1)) {
    uni.showToast({ title: '出席人数至少为 1', icon: 'none' });
    return false;
  }
  return true;
}

function openGift() {
  if (!banquetId.value) {
    return;
  }
  if (!features.value.mockPaymentEnabled) {
    safeNavigate(`/pages/gift/offline/index?banquetId=${banquetId.value}`, `${activeTheme.value.offlineGiftLabel}打开失败`);
    return;
  }
  safeNavigate(`/pages/gift/pay/index?banquetId=${banquetId.value}&entrySource=ONLINE_GIFT&guestName=${encodeURIComponent(form.guestName)}`, `${activeTheme.value.onlineGiftLabel}打开失败`);
}

function backToInvitation() {
  if (shareUrl.value) {
    safeNavigate(shareUrl.value, '请柬页面打开失败');
    return;
  }
  uni.navigateBack();
}

function safeNavigate(url: string, failTitle: string) {
  uni.navigateTo({
    url,
    fail: () => uni.showToast({ title: failTitle, icon: 'none' })
  });
}

function openRsvpStats() {
  if (!banquetId.value) {
    uni.showToast({ title: '缺少宴席信息', icon: 'none' });
    return;
  }
  uni.navigateTo({
    url: `/pages/rsvp/stats/index?banquetId=${banquetId.value}`,
    fail: () => uni.showToast({ title: '回执统计打开失败', icon: 'none' })
  });
}

function editAgain() {
  submitted.value = false;
}

async function loadInvitationShareUrl() {
  if (!invitationId.value) {
    return;
  }
  const detail = await request<{ shareUrl?: string }>(`/invitations/${invitationId.value}`);
  shareUrl.value = detail.shareUrl || '';
}

onMounted(async () => {
  const pages = getCurrentPages();
  const current = pages[pages.length - 1] as unknown as { options?: Record<string, string> };
  banquetId.value = await resolveBanquetId(current.options?.banquetId);
  if (!banquetId.value) {
    requireBanquetToast();
  }
  if (banquetId.value) {
    eventType.value = writeActiveEventType(await fetchBanquetEventType(banquetId.value, request, eventType.value));
  }
  invitationId.value = current.options?.invitationId || '';
  await Promise.all([
    loadInvitationShareUrl(),
    loadRuntimeFeatures().then((result) => {
      features.value = result;
    }).catch(() => ({ mockPaymentEnabled: false }))
  ]);
});
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 24rpx 24rpx 0;
  background:
    radial-gradient(circle at 50% -120rpx, rgba(230, 0, 18, 0.1), transparent 420rpx),
    #fffaf5;
  box-sizing: border-box;
  color: #151824;
}

.hero {
  position: relative;
  overflow: hidden;
  height: 292rpx;
  margin-bottom: 28rpx;
  padding-top: 48rpx;
  border-radius: 24rpx;
  background:
    radial-gradient(circle at 82% 24%, rgba(255, 210, 150, 0.34), transparent 150rpx),
    linear-gradient(135deg, #d90b12 0%, #be1016 56%, #a1060b 100%);
  box-shadow: 0 18rpx 42rpx rgba(172, 16, 17, 0.24);
  text-align: center;
  box-sizing: border-box;
}

.hero-title {
  position: relative;
  z-index: 2;
  display: block;
  color: #ffe6bd;
  font-family: serif;
  font-size: 58rpx;
  font-weight: 800;
  line-height: 1.2;
}

.hero-subtitle {
  position: relative;
  z-index: 2;
  display: block;
  margin-top: 14rpx;
  color: #fff7e7;
  font-size: 30rpx;
  font-weight: 600;
  letter-spacing: 0;
}

.hero-divider {
  position: relative;
  z-index: 2;
  width: 160rpx;
  height: 2rpx;
  margin: 28rpx auto 0;
  background: linear-gradient(90deg, transparent, rgba(255, 230, 189, 0.92), transparent);
}

.hero-lantern {
  position: absolute;
  left: 40rpx;
  top: -18rpx;
  width: 78rpx;
  height: 108rpx;
  border-radius: 50%;
  background: linear-gradient(145deg, #ffb164, #d6251d 70%);
  opacity: 0.7;
}

.hero-lantern::after {
  content: "";
  position: absolute;
  left: 34rpx;
  bottom: -28rpx;
  width: 10rpx;
  height: 36rpx;
  border-radius: 10rpx;
  background: #f8c77d;
}

.hero-flower {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 224, 184, 0.22);
}

.flower-a {
  right: -40rpx;
  bottom: -76rpx;
  width: 230rpx;
  height: 230rpx;
}

.flower-b {
  left: -78rpx;
  bottom: -112rpx;
  width: 250rpx;
  height: 250rpx;
}

.form-card {
  overflow: hidden;
  border-radius: 24rpx;
  background: #fff;
  box-shadow: 0 12rpx 36rpx rgba(92, 48, 30, 0.08);
}

.form-row {
  display: grid;
  grid-template-columns: 172rpx 1fr;
  align-items: center;
  min-height: 104rpx;
  padding: 0 30rpx;
  border-bottom: 1rpx solid #f1e4d6;
  box-sizing: border-box;
}

.field-label {
  color: #171c2a;
  font-size: 30rpx;
  font-weight: 700;
}

.required {
  color: #d92820;
}

.field-input {
  height: 84rpx;
  color: #171c2a;
  font-size: 30rpx;
  text-align: left;
}

.placeholder {
  color: #b9afa6;
}

.segmented {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16rpx;
}

.segmented.two {
  grid-template-columns: repeat(2, 1fr);
}

.segment {
  height: 68rpx;
  border: 1rpx solid #ead8ca;
  border-radius: 999rpx;
  background: #fffdfb;
  color: #242633;
  font-size: 28rpx;
  font-weight: 700;
  line-height: 68rpx;
  text-align: center;
}

.segment.active {
  border-color: transparent;
  background: linear-gradient(135deg, #ee4038, #cf191e);
  color: #fff;
  box-shadow: 0 8rpx 18rpx rgba(214, 28, 28, 0.2);
}

.stepper {
  display: grid;
  grid-template-columns: 72rpx 1fr 72rpx;
  overflow: hidden;
  height: 68rpx;
  border: 1rpx solid #ead8ca;
  border-radius: 999rpx;
  background: #fffdfb;
}

.step {
  color: #886e58;
  font-size: 38rpx;
  line-height: 68rpx;
  text-align: center;
}

.step-input {
  height: 68rpx;
  color: #151824;
  font-size: 30rpx;
  font-weight: 800;
  line-height: 68rpx;
  text-align: center;
}

.decline-note {
  margin: 24rpx 30rpx 0;
  padding: 20rpx 22rpx;
  border: 1rpx solid #f1d8c0;
  border-radius: 16rpx;
  background: #fff8ef;
  color: #9a5a2c;
  font-size: 26rpx;
  line-height: 1.5;
}

.message-block {
  position: relative;
  padding: 30rpx;
}

.message-block .field-label {
  display: block;
  margin-bottom: 16rpx;
}

.message-input {
  box-sizing: border-box;
  width: 100%;
  min-height: 190rpx;
  padding: 24rpx;
  border: 1rpx solid #ead8ca;
  border-radius: 20rpx;
  background: #fffdfb;
  color: #171c2a;
  font-size: 28rpx;
  line-height: 1.6;
}

.counter {
  position: absolute;
  right: 54rpx;
  bottom: 50rpx;
  color: #b9afa6;
  font-size: 24rpx;
}

.privacy-tip {
  padding: 20rpx 0;
  color: #b2804e;
  font-size: 24rpx;
  line-height: 1.5;
  text-align: center;
}

.footer-safe {
  height: 148rpx;
}

.sticky-submit {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 20;
  padding: 18rpx 28rpx calc(18rpx + env(safe-area-inset-bottom));
  background: rgba(255, 250, 245, 0.96);
  box-shadow: 0 -8rpx 28rpx rgba(72, 45, 24, 0.08);
}

.primary-button,
.ghost-button,
.text-button {
  margin: 0;
  border-radius: 18rpx;
  font-size: 32rpx;
  font-weight: 800;
}

.primary-button {
  height: 96rpx;
  background: linear-gradient(135deg, #e83a32, #c91419);
  color: #fff;
  line-height: 96rpx;
  box-shadow: 0 12rpx 26rpx rgba(213, 24, 26, 0.2);
}

.primary-button::after,
.ghost-button::after,
.text-button::after {
  border: 0;
}

.success-page {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding-bottom: 80rpx;
}

.success-hero {
  display: grid;
  justify-items: center;
  gap: 18rpx;
  padding: 72rpx 40rpx;
  border-radius: 28rpx;
  background: #fff;
  box-shadow: 0 12rpx 36rpx rgba(92, 48, 30, 0.08);
}

.result-card {
  margin-top: 24rpx;
  padding: 28rpx;
  border: 1rpx solid #f0dfcf;
  border-radius: 24rpx;
  background: #fff;
  box-shadow: 0 12rpx 36rpx rgba(92, 48, 30, 0.08);
}

.result-head,
.result-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.result-head {
  margin-bottom: 18rpx;
}

.result-title {
  color: #151824;
  font-size: 32rpx;
  font-weight: 900;
}

.result-status {
  padding: 8rpx 18rpx;
  border-radius: 999rpx;
  background: #fff1ee;
  color: #c7191e;
  font-size: 23rpx;
  font-weight: 800;
}

.result-row {
  min-height: 58rpx;
  border-bottom: 1rpx solid #f4ebe3;
}

.result-row:last-child {
  border-bottom: 0;
}

.result-label {
  color: #8a7768;
  font-size: 25rpx;
  font-weight: 700;
}

.result-value {
  max-width: 420rpx;
  color: #171c2a;
  font-size: 27rpx;
  font-weight: 800;
  text-align: right;
}

.result-message {
  margin-top: 16rpx;
  padding: 20rpx;
  border-radius: 16rpx;
  background: #fff8ef;
  color: #7a5a44;
  font-size: 25rpx;
  line-height: 1.6;
}

.success-icon {
  width: 104rpx;
  height: 104rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #e83a32, #c91419);
  color: #fff;
  font-size: 64rpx;
  font-weight: 800;
  line-height: 104rpx;
  text-align: center;
}

.success-title {
  color: #151824;
  font-size: 42rpx;
  font-weight: 900;
}

.success-text {
  color: #6d7280;
  font-size: 28rpx;
  line-height: 1.6;
}

.success-actions {
  display: grid;
  gap: 18rpx;
  margin-top: 30rpx;
}

.ghost-button {
  height: 88rpx;
  border: 1rpx solid #ead8ca;
  background: #fff;
  color: #9e2c23;
  line-height: 88rpx;
}

.text-button {
  height: 76rpx;
  background: transparent;
  color: #8b8f99;
  line-height: 76rpx;
}
</style>
