<template>
  <view class="page">
    <text class="title">支付成功</text>
    <text class="meta">订单号：{{ orderNo }}</text>
    <button v-if="features.mockPaymentEnabled" type="primary" :loading="submitting" @click="confirmSuccess">模拟支付成功入账</button>
    <text v-else class="meta">真实支付完成后，系统会自动入账并推送确认屏</text>
    <button v-if="banquetId" @click="openGiftList">查看收礼记录</button>
    <button @click="goBack">返回</button>
  </view>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { loadRuntimeFeatures, request, type RuntimeFeatures } from '../../../api/client';

const orderNo = ref('');
const banquetId = ref('');
const submitting = ref(false);
const features = ref<RuntimeFeatures>({ mockPaymentEnabled: false });

async function confirmSuccess() {
  submitting.value = true;
  try {
    await request(`/gifts/payment-orders/${orderNo.value}/mock-success`, { method: 'POST' });
    uni.showToast({ title: '已入账', icon: 'success' });
  } finally {
    submitting.value = false;
  }
}

function openGiftList() {
  uni.navigateTo({ url: `/pages/gift/list/index?banquetId=${banquetId.value}` });
}

function goBack() {
  uni.navigateBack();
}

onMounted(async () => {
  const pages = getCurrentPages();
  const current = pages[pages.length - 1] as unknown as { options?: Record<string, string> };
  orderNo.value = current.options?.orderNo || '';
  banquetId.value = current.options?.banquetId || '';
  features.value = await loadRuntimeFeatures();
});
</script>

<style scoped>
.page { padding: 24rpx; }
.title { display: block; margin-bottom: 24rpx; font-size: 40rpx; font-weight: 600; }
.meta { display: block; margin-bottom: 24rpx; color: #666; }
</style>
