<template>
  <view class="page">
    <view class="hero-card">
      <view class="coin coin-a">¥</view>
      <view class="coin coin-b">礼</view>
      <text class="hero-label">宴席通</text>
      <text class="hero-title">{{ activeTheme.offlineGiftLabel }}</text>
      <text class="hero-desc">现金、转账备注和现场补录统一登记</text>
      <view class="hero-tags">
        <text>现金{{ activeTheme.giftLabel }}</text>
        <text>自动入账</text>
        <text>同步人情</text>
      </view>
    </view>

    <view class="amount-card">
      <text class="amount-label">{{ activeTheme.giftAmountLabel }}</text>
      <view class="amount-input-row">
        <text class="currency">¥</text>
        <input v-model.number="form.amount" class="amount-input" type="digit" placeholder="0" placeholder-class="amount-placeholder" />
      </view>
      <view class="quick-amounts">
        <view v-for="item in quickAmounts" :key="item" class="quick-amount" @tap="form.amount = item">¥{{ item }}</view>
      </view>
    </view>

    <view class="form-card">
      <view class="form-row">
        <text class="row-icon">人</text>
        <text class="row-label">宾客姓名</text>
        <input v-model="form.guestName" class="row-input" placeholder="请输入宾客姓名" placeholder-class="placeholder" />
      </view>
      <view class="form-row">
        <text class="row-icon">备</text>
        <text class="row-label">{{ activeTheme.blessingLabel }}</text>
        <input v-model="form.blessing" class="row-input" :placeholder="activeTheme.blessingPlaceholder" placeholder-class="placeholder" />
      </view>
      <view class="tip-box">
        <text>保存后会写入{{ activeTheme.giftRecordLabel }}，并按规则沉淀到人情账本。</text>
      </view>
    </view>

    <view v-if="lastSavedText" class="saved-card">
      <text class="saved-title">最近保存成功</text>
      <text class="saved-desc">{{ lastSavedText }}</text>
      <view class="saved-actions">
        <button class="saved-link secondary" @tap="continueRegistration">继续登记</button>
        <button class="saved-link" @tap="openGiftList">查看{{ activeTheme.giftRecordLabel }}</button>
      </view>
      <view v-if="recentSaved.length > 1" class="recent-saved">
        <text class="recent-title">本次已保存 {{ recentSaved.length }} 笔</text>
        <view v-for="item in recentSaved" :key="item.id" class="recent-row">
          <text>{{ item.guestName }}</text>
          <text>¥{{ Number(item.amount || 0).toLocaleString('zh-CN') }}</text>
        </view>
      </view>
    </view>

    <view class="footer-safe"></view>
    <view class="sticky-submit">
      <button class="primary-button" :loading="submitting" @tap="submit">保存记礼</button>
      <button class="ghost-button" @tap="openGiftList">查看{{ activeTheme.giftRecordLabel }}</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { request } from '../../../api/client';
import { requireBanquetToast, resolveBanquetId } from '../../../utils/banquet';
import { eventThemeFor, fetchBanquetEventType, readActiveEventType, writeActiveEventType } from '../../../utils/event-theme';

interface GiftRecord {
  id: number;
  guestName: string;
  amount: number;
  blessing?: string;
}

const banquetId = ref('');
const submitting = ref(false);
const lastSavedText = ref('');
const lastSavedId = ref<number>();
const recentSaved = ref<GiftRecord[]>([]);
const eventType = ref(readActiveEventType());
const quickAmounts = [200, 500, 800, 1000, 1200, 2000];
const form = reactive({ guestName: '', amount: undefined as number | undefined, blessing: '' });
const activeTheme = computed(() => eventThemeFor(eventType.value));

async function submit() {
  if (!validate()) {
    return;
  }
  submitting.value = true;
  try {
    const saved = await request<GiftRecord>('/gifts/offline', { method: 'POST', data: { ...form, banquetId: Number(banquetId.value) } });
    lastSavedId.value = saved.id;
    lastSavedText.value = `${saved.guestName} · ¥${Number(saved.amount || 0).toLocaleString('zh-CN')}`;
    recentSaved.value = [saved, ...recentSaved.value].slice(0, 5);
    uni.showToast({ title: '已保存', icon: 'success' });
    clearForm();
    uni.showModal({
      title: '记礼已保存',
      content: `已写入${activeTheme.value.giftRecordLabel}，并同步沉淀到人情账本。`,
      cancelText: '继续登记',
      confirmText: '查看记录',
      success: (result) => {
        if (result.confirm) {
          openGiftList();
        }
      }
    });
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '保存记礼失败', icon: 'none' });
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
    uni.showToast({ title: '请填写宾客姓名', icon: 'none' });
    return false;
  }
  if (!form.amount || Number(form.amount) <= 0) {
    uni.showToast({ title: `请填写${activeTheme.value.giftAmountLabel}`, icon: 'none' });
    return false;
  }
  return true;
}

function openGiftList() {
  if (!banquetId.value) {
    requireBanquetToast();
    return;
  }
  const highlight = lastSavedId.value ? `&highlightId=${lastSavedId.value}` : '';
  safeNavigate(`/pages/gift/list/index?banquetId=${banquetId.value}&source=CASH${highlight}`, `${activeTheme.value.giftRecordLabel}打开失败`);
}

function continueRegistration() {
  clearForm();
  uni.showToast({ title: '可继续登记', icon: 'none' });
}

function clearForm() {
  form.guestName = '';
  form.amount = undefined;
  form.blessing = '';
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
    return;
  }
  eventType.value = writeActiveEventType(await fetchBanquetEventType(banquetId.value, request, eventType.value));
});
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 24rpx 24rpx 0;
  background: #fff8ef;
  box-sizing: border-box;
  color: #161a28;
}

.hero-card {
  position: relative;
  overflow: hidden;
  padding: 36rpx;
  border-radius: 28rpx;
  background:
    radial-gradient(circle at 82% 18%, rgba(255, 220, 156, 0.35), transparent 180rpx),
    linear-gradient(135deg, #e71921 0%, #c71118 64%, #991012 100%);
  box-shadow: 0 16rpx 42rpx rgba(184, 17, 21, 0.24);
}

.hero-label,
.hero-title,
.hero-desc {
  position: relative;
  z-index: 2;
  display: block;
}

.hero-label {
  color: #ffe4bd;
  font-size: 26rpx;
  font-weight: 800;
}

.hero-title {
  margin-top: 16rpx;
  color: #fff7df;
  font-family: serif;
  font-size: 58rpx;
  font-weight: 900;
}

.hero-desc {
  margin-top: 12rpx;
  color: rgba(255, 248, 232, 0.94);
  font-size: 28rpx;
  line-height: 1.5;
}

.hero-tags {
  position: relative;
  z-index: 2;
  display: flex;
  flex-wrap: wrap;
  gap: 14rpx;
  margin-top: 28rpx;
}

.hero-tags text {
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.18);
  color: #fff2d7;
  font-size: 24rpx;
  font-weight: 700;
}

.coin {
  position: absolute;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: rgba(255, 213, 139, 0.22);
  color: rgba(255, 238, 200, 0.48);
  font-family: serif;
  font-weight: 900;
}

.coin-a {
  right: 42rpx;
  top: 34rpx;
  width: 120rpx;
  height: 120rpx;
  font-size: 54rpx;
}

.coin-b {
  right: -34rpx;
  bottom: -46rpx;
  width: 210rpx;
  height: 210rpx;
  font-size: 84rpx;
}

.amount-card,
.form-card,
.saved-card {
  margin-top: 24rpx;
  border: 1rpx solid #f0dfcf;
  border-radius: 24rpx;
  background: #fff;
  box-shadow: 0 12rpx 32rpx rgba(82, 45, 24, 0.07);
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
  color: #c7191e;
  font-size: 44rpx;
  font-weight: 900;
}

.amount-input {
  flex: 1;
  height: 104rpx;
  color: #c7191e;
  font-size: 70rpx;
  font-weight: 900;
}

.amount-placeholder {
  color: #e9c8be;
}

.quick-amounts {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16rpx;
  margin-top: 24rpx;
}

.quick-amount {
  height: 62rpx;
  border: 1rpx solid #efd9c7;
  border-radius: 999rpx;
  background: #fffaf5;
  color: #9c4b31;
  font-size: 26rpx;
  font-weight: 800;
  line-height: 62rpx;
  text-align: center;
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
  background: #fff0ea;
  color: #d52322;
  font-size: 20rpx;
  font-weight: 800;
}

.row-label {
  color: #171c2a;
  font-size: 30rpx;
  font-weight: 800;
}

.row-input {
  height: 86rpx;
  color: #171c2a;
  font-size: 28rpx;
}

.placeholder {
  color: #b8afa7;
}

.tip-box {
  margin: 26rpx 28rpx;
  padding: 20rpx 22rpx;
  border-radius: 16rpx;
  background: #fff7ec;
  color: #9a5b30;
  font-size: 25rpx;
  line-height: 1.5;
}

.saved-card {
  padding: 26rpx 28rpx;
  border-color: #bfe6c9;
  background: #f4fff6;
}

.saved-title,
.saved-desc {
  display: block;
}

.saved-title {
  color: #187a42;
  font-size: 28rpx;
  font-weight: 900;
}

.saved-desc {
  margin-top: 10rpx;
  color: #35423a;
  font-size: 25rpx;
}

.saved-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14rpx;
  margin-top: 18rpx;
}

.saved-link {
  width: 100%;
  height: 62rpx;
  margin: 0;
  border-radius: 999rpx;
  background: #18a058;
  color: #fff;
  font-size: 24rpx;
  font-weight: 900;
  line-height: 62rpx;
}

.saved-link.secondary {
  border: 1rpx solid #bfe6c9;
  background: #fff;
  color: #187a42;
}

.saved-link::after {
  border: 0;
}

.recent-saved {
  margin-top: 18rpx;
  padding-top: 18rpx;
  border-top: 1rpx solid #d9f0df;
}

.recent-title {
  display: block;
  margin-bottom: 10rpx;
  color: #187a42;
  font-size: 24rpx;
  font-weight: 800;
}

.recent-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 46rpx;
  color: #35423a;
  font-size: 24rpx;
}

.footer-safe {
  height: 178rpx;
}

.sticky-submit {
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

.primary-button,
.ghost-button {
  height: 92rpx;
  margin: 0;
  border-radius: 18rpx;
  font-size: 30rpx;
  font-weight: 900;
  line-height: 92rpx;
}

.primary-button {
  background: linear-gradient(135deg, #e83a32, #c91419);
  color: #fff;
}

.ghost-button {
  border: 1rpx solid #e8cdbc;
  background: #fff;
  color: #a83a27;
}

.primary-button::after,
.ghost-button::after {
  border: 0;
}
</style>
