<template>
  <view class="page">
    <view class="hero">
      <view class="hero-mark">客</view>
      <text class="hero-eyebrow">情礼记专属客服</text>
      <text class="hero-title">一对一为您处理问题</text>
      <text class="hero-desc">企业微信客服将为您提供产品使用与宴席服务支持</text>
    </view>

    <view class="service-card">
      <view v-if="loading" class="loading-state">
        <view class="loading-ring"></view>
        <text>正在连接专属客服</text>
      </view>

      <template v-else-if="customerServiceLink">
        <view class="card-head">
          <view>
            <text class="card-title">添加专属客服</text>
            <text class="card-subtitle">企业微信获客助手</text>
          </view>
          <text class="ready-badge">可联系</text>
        </view>

        <view class="qr-shell">
          <customer-service-qr-code
            :value="customerServiceLink"
            :size="230"
            @ready="handleQrReady"
            @error="handleQrError"
            @tap="previewQrCode()"
          />
        </view>

        <view class="service-points">
          <view><text class="point-dot"></text><text>快速响应</text></view>
          <view><text class="point-dot"></text><text>专业解答</text></view>
          <view><text class="point-dot"></text><text>企业微信认证</text></view>
        </view>

        <button class="primary-button" :disabled="!qrImage" @tap="previewQrCode()">打开客服二维码</button>
        <button class="secondary-button" @tap="copyCustomerServiceLink()">复制客服链接</button>
      </template>

      <view v-else class="empty-state">
        <view class="empty-mark">!</view>
        <text class="empty-title">专属客服暂未连接</text>
        <text class="empty-desc">客服配置可能正在更新，请稍后重新加载。</text>
        <button class="primary-button" @tap="loadCustomerService()">重新加载</button>
      </view>
    </view>

    <text v-if="customerServiceLink" class="page-note">点击二维码或按钮进入微信预览后，可识别并添加专属客服</text>
  </view>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import CustomerServiceQrCode from '../../../components/customer-service-qr-code.vue';
import { request } from '../../../api/client';

interface CustomerServiceConfig {
  acquireLink: string;
  enabled: boolean;
}

const loading = ref(true);
const customerServiceLink = ref('');
const qrImage = ref('');

function isAllowedAcquireLink(value: string) {
  return /^https:\/\/work\.weixin\.qq\.com\/ca\/[A-Za-z0-9_-]+(?:[?#].*)?$/.test(value);
}

async function loadCustomerService() {
  loading.value = true;
  customerServiceLink.value = '';
  qrImage.value = '';
  try {
    const config = await request<CustomerServiceConfig>('/meta/customer-service', {
      auth: false,
      silent: true
    });
    const link = String(config?.acquireLink || '').trim();
    if (!config?.enabled || !isAllowedAcquireLink(link)) {
      uni.showToast({ title: '专属客服暂未配置', icon: 'none' });
      return;
    }
    customerServiceLink.value = link;
  } catch {
    uni.showToast({ title: '客服链接加载失败', icon: 'none' });
  } finally {
    loading.value = false;
  }
}

function handleQrReady(image: string) {
  qrImage.value = image || '';
}

function handleQrError() {
  qrImage.value = '';
  uni.showToast({ title: '二维码生成失败，请复制链接', icon: 'none' });
}

function previewQrCode() {
  if (!qrImage.value) {
    uni.showToast({ title: '二维码正在生成', icon: 'none' });
    return;
  }
  uni.previewImage({
    current: qrImage.value,
    urls: [qrImage.value],
    fail: () => uni.showToast({ title: '二维码预览失败，请复制链接', icon: 'none' })
  });
}

function copyCustomerServiceLink() {
  if (!customerServiceLink.value) {
    return;
  }
  uni.setClipboardData({
    data: customerServiceLink.value,
    success: () => uni.showToast({ title: '客服链接已复制', icon: 'success' }),
    fail: () => uni.showToast({ title: '复制失败，请稍后重试', icon: 'none' })
  });
}

onMounted(loadCustomerService);
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 0 36rpx 64rpx;
  box-sizing: border-box;
  background: linear-gradient(180deg, #eaf5ef 0, #f4f8f5 420rpx, #f7f8f8 100%);
}

.hero {
  height: 350rpx;
  padding: 64rpx 28rpx 0;
  box-sizing: border-box;
  position: relative;
  overflow: hidden;
  color: #173224;
}

.hero-mark {
  width: 220rpx;
  height: 220rpx;
  position: absolute;
  top: 34rpx;
  right: -10rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2rpx solid rgba(22, 101, 52, 0.1);
  border-radius: 50%;
  color: rgba(22, 101, 52, 0.1);
  font-size: 122rpx;
  font-weight: 700;
}

.hero-eyebrow,
.hero-title,
.hero-desc,
.card-title,
.card-subtitle,
.empty-title,
.empty-desc {
  display: block;
}

.hero-eyebrow {
  color: #166534;
  font-size: 27rpx;
  font-weight: 700;
}

.hero-title {
  margin-top: 18rpx;
  font-size: 46rpx;
  font-weight: 700;
}

.hero-desc {
  width: 470rpx;
  margin-top: 16rpx;
  color: #66766c;
  font-size: 26rpx;
  line-height: 1.65;
}

.service-card {
  min-height: 740rpx;
  margin-top: -42rpx;
  padding: 40rpx;
  position: relative;
  box-sizing: border-box;
  border: 1rpx solid rgba(19, 78, 43, 0.08);
  border-radius: 8rpx;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 24rpx 56rpx rgba(31, 72, 48, 0.1);
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  color: #171d1a;
  font-size: 34rpx;
  font-weight: 700;
}

.card-subtitle {
  margin-top: 8rpx;
  color: #7b857f;
  font-size: 24rpx;
}

.ready-badge {
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  background: #edf8f1;
  color: #166534;
  font-size: 23rpx;
  font-weight: 600;
}

.qr-shell {
  width: 500rpx;
  height: 500rpx;
  margin: 42rpx auto 30rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1rpx solid #dce8e0;
  border-radius: 8rpx;
  background: #fff;
  box-shadow: inset 0 0 0 16rpx #f7faf8;
}

.service-points {
  margin-bottom: 30rpx;
  display: flex;
  justify-content: center;
  gap: 24rpx;
  color: #68736c;
  font-size: 23rpx;
}

.service-points view {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.point-dot {
  width: 8rpx;
  height: 8rpx;
  border-radius: 50%;
  background: #2f8f59;
}

.primary-button,
.secondary-button {
  width: 100%;
  height: 88rpx;
  border-radius: 8rpx;
  font-size: 29rpx;
  font-weight: 600;
  line-height: 88rpx;
}

.primary-button {
  background: #166534;
  color: #fff;
}

.primary-button[disabled] {
  background: #92ad9c;
  color: #fff;
}

.secondary-button {
  margin-top: 18rpx;
  border: 1rpx solid #d6dfd9;
  background: #fff;
  color: #31513d;
}

.loading-state,
.empty-state {
  min-height: 660rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  text-align: center;
}

.loading-state {
  gap: 28rpx;
  color: #66766c;
  font-size: 27rpx;
}

.loading-ring {
  width: 64rpx;
  height: 64rpx;
  border: 6rpx solid #d9e7de;
  border-top-color: #166534;
  border-radius: 50%;
  animation: spin 0.9s linear infinite;
}

.empty-mark {
  width: 104rpx;
  height: 104rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #f2f5f3;
  color: #7d8a82;
  font-size: 48rpx;
  font-weight: 700;
}

.empty-title {
  margin-top: 32rpx;
  color: #202622;
  font-size: 34rpx;
  font-weight: 700;
}

.empty-desc {
  margin: 16rpx 0 42rpx;
  color: #778079;
  font-size: 25rpx;
}

.page-note {
  display: block;
  margin: 28rpx 32rpx 0;
  color: #7d8580;
  font-size: 23rpx;
  line-height: 1.6;
  text-align: center;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
