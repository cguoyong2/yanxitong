<template>
  <view class="page">
    <text class="title">设备选择</text>
    <view class="status" :class="{ blocked: !hasDeviceRight }">
      <text class="status-title">设备租赁权益：{{ hasDeviceRight ? '已开通' : '未开通' }}</text>
      <text class="meta">当前版本：{{ entitlements.currentPlan?.name || '基础版' }}</text>
      <button v-if="!hasDeviceRight" size="mini" type="primary" @click="openPlan">去开通版本</button>
    </view>
    <view v-if="orders.length" class="status">
      <text class="status-title">已创建设备订单</text>
      <view v-for="order in orders" :key="order.orderNo" class="order-row">
        <text>{{ order.deviceType }} / {{ order.payStatus }} / {{ order.orderStatus }}</text>
        <button v-if="features.mockPaymentEnabled && order.payStatus !== 'PAID'" size="mini" :loading="payingOrderNo === order.orderNo" @click="mockPay(order.orderNo)">模拟支付</button>
      </view>
    </view>
    <view v-for="config in configs" :key="config.id" class="card">
      <view class="row">
        <text class="name">{{ config.name }}</text>
        <text class="price">{{ config.price }} 元 / {{ config.priceUnit }}</text>
      </view>
      <text class="meta">设备类型：{{ config.deviceType }}</text>
      <text class="meta">交付方式：{{ config.deliveryMethod }}</text>
      <button size="mini" type="primary" :loading="submittingId === config.id" @click="createOrder(config)">租用此设备</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { loadRuntimeFeatures, request, type RuntimeFeatures } from '../../../api/client';

interface DeviceConfig {
  id: number;
  deviceType: string;
  name: string;
  price: number;
  priceUnit: string;
  deliveryMethod: string;
}

interface Entitlements {
  currentPlan?: {
    name: string;
  };
  rightValues: Record<string, string>;
}

interface DeviceOrder {
  orderNo: string;
  deviceType: string;
  payStatus: string;
  orderStatus: string;
}

const configs = ref<DeviceConfig[]>([]);
const orders = ref<DeviceOrder[]>([]);
const banquetId = ref('');
const submittingId = ref<number>();
const payingOrderNo = ref('');
const entitlements = reactive<Entitlements>({
  rightValues: {}
});
const features = ref<RuntimeFeatures>({ mockPaymentEnabled: false });
const hasDeviceRight = computed(() => Boolean(entitlements.rightValues.DEVICE_RENTAL));

async function load() {
  const [runtimeFeatures, deviceConfigs] = await Promise.all([
    loadRuntimeFeatures(),
    request<DeviceConfig[]>('/devices/configs')
  ]);
  features.value = runtimeFeatures;
  configs.value = deviceConfigs;
  await Promise.all([loadEntitlements(), loadOrders()]);
}

async function loadEntitlements() {
  if (!banquetId.value) {
    return;
  }
  const result = await request<Entitlements>(`/plans/banquets/${banquetId.value}/entitlements`);
  entitlements.currentPlan = result.currentPlan;
  entitlements.rightValues = result.rightValues || {};
}

async function loadOrders() {
  if (!banquetId.value) {
    return;
  }
  orders.value = await request<DeviceOrder[]>(`/devices/orders?banquetId=${banquetId.value}`);
}

async function createOrder(config: DeviceConfig) {
  if (!banquetId.value) {
    uni.showToast({ title: '缺少宴席ID', icon: 'none' });
    return;
  }
  if (!hasDeviceRight.value) {
    uni.showToast({ title: '请先开通包含设备租赁的版本', icon: 'none' });
    return;
  }
  submittingId.value = config.id;
  try {
    const order = await request<DeviceOrder>('/devices/orders', {
      method: 'POST',
      data: {
        banquetId: Number(banquetId.value),
        deviceType: config.deviceType,
        deliveryMethod: config.deliveryMethod
      }
    });
    orders.value = [order, ...orders.value];
    uni.showToast({ title: '设备订单已创建', icon: 'success' });
  } finally {
    submittingId.value = undefined;
  }
}

async function mockPay(orderNo: string) {
  payingOrderNo.value = orderNo;
  try {
    await request(`/devices/orders/${orderNo}/mock-success`, { method: 'POST' });
    await loadOrders();
    uni.showToast({ title: '设备订单已确认', icon: 'success' });
  } finally {
    payingOrderNo.value = '';
  }
}

function openPlan() {
  if (banquetId.value) {
    uni.navigateTo({ url: `/pages/order/plan/index?banquetId=${banquetId.value}` });
  }
}

onMounted(() => {
  const pages = getCurrentPages();
  const current = pages[pages.length - 1] as unknown as { options?: Record<string, string> };
  banquetId.value = current.options?.banquetId || '';
  load();
});
</script>

<style scoped>
.page {
  padding: 24rpx;
}

.title {
  display: block;
  margin-bottom: 24rpx;
  font-size: 40rpx;
  font-weight: 600;
}

.card {
  margin-bottom: 20rpx;
  padding: 24rpx;
  border: 1px solid #e5e7eb;
  border-radius: 8rpx;
}

.status {
  margin-bottom: 20rpx;
  padding: 24rpx;
  background: #f8fafc;
  border: 1px solid #dbe3ea;
  border-radius: 8rpx;
}

.status.blocked {
  background: #fff7ed;
  border-color: #fed7aa;
}

.status-title {
  display: block;
  margin-bottom: 12rpx;
  font-weight: 600;
}

.order-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin-top: 12rpx;
}

.row {
  display: flex;
  justify-content: space-between;
  gap: 16rpx;
  margin-bottom: 12rpx;
}

.name {
  font-weight: 600;
}

.price {
  color: #b91c1c;
}

.meta {
  display: block;
  margin-bottom: 12rpx;
  color: #666;
}
</style>
