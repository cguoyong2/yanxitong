<template>
  <view class="page">
    <view class="hero-card">
      <view class="coin coin-a">¥</view>
      <view class="coin coin-b">礼</view>
      <text class="hero-label">宴席通</text>
      <text class="hero-title">线下记礼</text>
      <text class="hero-desc">现金礼金、转账备注和现场补录统一登记</text>
      <view class="hero-tags">
        <text>现金收礼</text>
        <text>自动入账</text>
        <text>同步人情</text>
      </view>
    </view>

    <view class="amount-card">
      <text class="amount-label">礼金金额</text>
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
        <text class="row-label">备注祝福</text>
        <input v-model="form.blessing" class="row-input" placeholder="如：新婚快乐、现金礼金" placeholder-class="placeholder" />
      </view>
      <view class="tip-box">
        <text>保存后会写入收礼记录，并按规则沉淀到人情账本。</text>
      </view>
    </view>

    <view class="footer-safe"></view>
    <view class="sticky-submit">
      <button class="primary-button" :loading="submitting" @tap="submit">保存记礼</button>
      <button class="ghost-button" @tap="openGiftList">查看收礼记录</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { request } from '../../../api/client';
import { requireBanquetToast, resolveBanquetId } from '../../../utils/banquet';

const banquetId = ref('');
const submitting = ref(false);
const quickAmounts = [200, 500, 800, 1000, 1200, 2000];
const form = reactive({ guestName: '', amount: undefined as number | undefined, blessing: '' });

async function submit() {
  if (!validate()) {
    return;
  }
  submitting.value = true;
  try {
    await request('/gifts/offline', { method: 'POST', data: { ...form, banquetId: Number(banquetId.value) } });
    uni.showToast({ title: '已保存', icon: 'success' });
    form.guestName = '';
    form.amount = undefined;
    form.blessing = '';
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
    uni.showToast({ title: '请填写礼金金额', icon: 'none' });
    return false;
  }
  return true;
}

function openGiftList() {
  if (!banquetId.value) {
    return;
  }
  uni.navigateTo({ url: `/pages/gift/list/index?banquetId=${banquetId.value}` });
}

onMounted(async () => {
  const pages = getCurrentPages();
  const current = pages[pages.length - 1] as unknown as { options?: Record<string, string> };
  banquetId.value = await resolveBanquetId(current.options?.banquetId);
  if (!banquetId.value) {
    requireBanquetToast();
  }
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
.form-card {
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
