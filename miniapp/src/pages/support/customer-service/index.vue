<template>
  <view class="page">
    <web-view v-if="customerServiceLink" :src="customerServiceLink" @error="handleLoadError" />
    <view v-else class="state-card">
      <view class="service-mark">客</view>
      <text class="state-title">{{ loading ? '正在打开专属客服' : '专属客服暂未连接' }}</text>
      <text class="state-desc">{{ loading ? '请稍候' : '请稍后重试，或复制客服链接后在微信中打开。' }}</text>
      <button v-if="!loading" class="primary-button" @tap="loadCustomerService()">重新加载</button>
      <button v-if="fallbackLink" class="secondary-button" @tap="copyCustomerServiceLink()">复制客服链接</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { request } from '../../../api/client';

interface CustomerServiceConfig {
  acquireLink: string;
  enabled: boolean;
}

const loading = ref(true);
const customerServiceLink = ref('');
const fallbackLink = ref('');

function isAllowedAcquireLink(value: string) {
  return /^https:\/\/work\.weixin\.qq\.com\/ca\/[A-Za-z0-9_-]+(?:[?#].*)?$/.test(value);
}

async function loadCustomerService() {
  loading.value = true;
  customerServiceLink.value = '';
  try {
    const config = await request<CustomerServiceConfig>('/meta/customer-service', {
      auth: false,
      silent: true
    });
    const link = String(config?.acquireLink || '').trim();
    fallbackLink.value = isAllowedAcquireLink(link) ? link : '';
    if (!config?.enabled || !fallbackLink.value) {
      uni.showToast({ title: '专属客服暂未配置', icon: 'none' });
      return;
    }
    customerServiceLink.value = fallbackLink.value;
  } catch {
    uni.showToast({ title: '客服链接加载失败', icon: 'none' });
  } finally {
    loading.value = false;
  }
}

function handleLoadError() {
  customerServiceLink.value = '';
  uni.showToast({ title: '微信未能直接打开客服', icon: 'none' });
}

function copyCustomerServiceLink() {
  if (!fallbackLink.value) {
    return;
  }
  uni.setClipboardData({
    data: fallbackLink.value,
    success: () => uni.showToast({ title: '客服链接已复制', icon: 'success' })
  });
}

onMounted(loadCustomerService);
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f6f8;
}

.state-card {
  min-height: calc(100vh - 88rpx);
  padding: 180rpx 60rpx 80rpx;
  display: flex;
  align-items: center;
  flex-direction: column;
  box-sizing: border-box;
  text-align: center;
}

.service-mark {
  width: 132rpx;
  height: 132rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #166534;
  box-shadow: 0 18rpx 42rpx rgba(22, 101, 52, 0.18);
  color: #fff;
  font-size: 56rpx;
  font-weight: 700;
}

.state-title {
  margin-top: 42rpx;
  color: #171923;
  font-size: 38rpx;
  font-weight: 700;
}

.state-desc {
  max-width: 560rpx;
  margin-top: 18rpx;
  color: #737987;
  font-size: 27rpx;
  line-height: 1.7;
}

.primary-button,
.secondary-button {
  width: 420rpx;
  height: 88rpx;
  margin-top: 40rpx;
  border-radius: 8rpx;
  font-size: 30rpx;
  line-height: 88rpx;
}

.primary-button {
  background: #166534;
  color: #fff;
}

.secondary-button {
  margin-top: 20rpx;
  border: 1rpx solid #d5d8de;
  background: #fff;
  color: #272b33;
}
</style>
