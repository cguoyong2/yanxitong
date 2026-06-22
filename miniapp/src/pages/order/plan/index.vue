<template>
  <view class="page">
    <text class="title">选择版本</text>
    <view v-if="entitlements.currentPlan" class="status">
      <text class="status-title">当前版本：{{ entitlements.currentPlan.name }}</text>
      <text class="meta">状态：{{ entitlements.freeDefault ? '默认基础版' : '已开通' }}</text>
      <text class="meta">设备权益：{{ hasDeviceRight ? '已包含' : '未包含' }}</text>
      <text class="meta">Excel 导出：{{ hasExportRight ? 'P1 预留' : '未包含' }}</text>
    </view>
    <view v-if="pendingOrder" class="status">
      <text class="status-title">待支付订单：{{ pendingOrder.orderNo }}</text>
      <text class="meta">金额：{{ pendingOrder.amount }} 元 / {{ pendingOrder.priceUnit }}</text>
      <button v-if="features.mockPaymentEnabled" size="mini" type="primary" :loading="paying" @click="mockPay(pendingOrder.orderNo)">模拟支付并开通</button>
      <text v-else class="meta">请在真实支付完成后等待系统开通</text>
    </view>
    <view v-for="plan in plans" :key="plan.id" class="card">
      <view class="row">
        <text class="name">{{ plan.name }}</text>
        <text class="price">{{ plan.price }} 元 / {{ plan.priceUnit }}</text>
      </view>
      <text class="meta">版本编码：{{ plan.planCode }}</text>
      <button size="mini" type="primary" :loading="submittingId === plan.id" @click="createOrder(plan.id)">
        {{ Number(plan.price) === 0 ? '启用基础版' : '创建版本订单' }}
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { loadRuntimeFeatures, request, type RuntimeFeatures } from '../../../api/client';

interface Plan {
  id: number;
  planCode: string;
  name: string;
  price: number;
  priceUnit: string;
}

interface PlanOrder {
  orderNo: string;
  amount: number;
  priceUnit: string;
  payStatus: string;
}

interface Entitlements {
  currentPlan?: Plan;
  rightValues: Record<string, string>;
  paidPlanActive: boolean;
  freeDefault: boolean;
}

const plans = ref<Plan[]>([]);
const banquetId = ref('');
const submittingId = ref<number>();
const paying = ref(false);
const pendingOrder = ref<PlanOrder>();
const features = ref<RuntimeFeatures>({ mockPaymentEnabled: false });
const entitlements = reactive<Entitlements>({
  rightValues: {},
  paidPlanActive: false,
  freeDefault: true
});
const hasDeviceRight = computed(() => Boolean(entitlements.rightValues.DEVICE_RENTAL));
const hasExportRight = computed(() => Boolean(entitlements.rightValues.EXCEL_EXPORT));

async function load() {
  const [runtimeFeatures, planList] = await Promise.all([
    loadRuntimeFeatures(),
    request<Plan[]>('/plans')
  ]);
  features.value = runtimeFeatures;
  plans.value = planList;
  await loadEntitlements();
}

async function loadEntitlements() {
  if (!banquetId.value) {
    return;
  }
  const result = await request<Entitlements>(`/plans/banquets/${banquetId.value}/entitlements`);
  entitlements.currentPlan = result.currentPlan;
  entitlements.rightValues = result.rightValues || {};
  entitlements.paidPlanActive = result.paidPlanActive;
  entitlements.freeDefault = result.freeDefault;
}

async function createOrder(planId: number) {
  if (!banquetId.value) {
    uni.showToast({ title: '缺少宴席ID', icon: 'none' });
    return;
  }
  submittingId.value = planId;
  try {
    const order = await request<PlanOrder>('/plans/orders', {
      method: 'POST',
      data: { banquetId: Number(banquetId.value), planId }
    });
    if (order.payStatus === 'PAID') {
      pendingOrder.value = undefined;
      await loadEntitlements();
      uni.showToast({ title: '版本已启用', icon: 'success' });
    } else {
      pendingOrder.value = order;
      uni.showToast({ title: '订单已创建', icon: 'success' });
    }
  } finally {
    submittingId.value = undefined;
  }
}

async function mockPay(orderNo: string) {
  paying.value = true;
  try {
    await request(`/plans/orders/${orderNo}/mock-success`, { method: 'POST' });
    pendingOrder.value = undefined;
    await loadEntitlements();
    uni.showToast({ title: '版本已开通', icon: 'success' });
  } finally {
    paying.value = false;
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

.status-title {
  display: block;
  margin-bottom: 12rpx;
  font-weight: 600;
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
  margin-bottom: 16rpx;
  color: #666;
}
</style>
