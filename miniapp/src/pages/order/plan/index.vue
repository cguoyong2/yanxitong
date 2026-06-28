<template>
  <view class="page">
    <view class="hero-card">
      <view class="hero-art">
        <text class="hero-symbol">冠</text>
      </view>
      <text class="hero-label">宴席通权益中心</text>
      <text class="hero-title">选择版本</text>
      <text class="hero-desc">价格、单位和权益均由后台配置，开通后立即影响设备、导出和业务入口。</text>
    </view>

    <view v-if="entitlements.currentPlan" class="current-card">
      <view>
        <text class="card-title">当前版本</text>
        <text class="current-plan">{{ entitlements.currentPlan.name }}</text>
        <text class="card-desc">{{ entitlements.freeDefault ? '当前为默认基础版' : '当前版本已开通' }}</text>
      </view>
      <view class="right-tags">
        <text :class="{ active: hasDeviceRight }">设备租赁</text>
        <text :class="{ active: hasExportRight }">Excel 导出</text>
      </view>
    </view>

    <view v-if="pendingOrder" class="pending-card">
      <view>
        <text class="card-title">待支付订单</text>
        <text class="order-no">{{ pendingOrder.orderNo }}</text>
        <text class="card-desc">金额：{{ formatMoney(pendingOrder.amount) }} / {{ pendingOrder.priceUnit }}</text>
      </view>
      <button v-if="features.mockPaymentEnabled" class="small-button primary" :loading="paying" @tap="mockPay(pendingOrder.orderNo)">模拟支付</button>
      <text v-else class="pending-text">等待真实支付回调</text>
    </view>

    <button v-if="banquetId" class="return-button" @tap="returnBanquetDetail">返回宴席管理台</button>

    <view class="plans-list">
      <view
        v-for="plan in plans"
        :key="plan.id"
        class="plan-card"
        :class="{ recommended: isRecommended(plan), current: isCurrent(plan) }"
      >
        <view class="plan-head">
          <view class="plan-icon">{{ planIcon(plan) }}</view>
          <view class="plan-main">
            <view class="name-line">
              <text class="plan-name">{{ plan.name }}</text>
              <text v-if="isRecommended(plan)" class="badge">推荐</text>
              <text v-if="isCurrent(plan)" class="badge current-badge">当前</text>
            </view>
            <text class="plan-desc">{{ planDesc(plan) }}</text>
          </view>
          <view class="price-box">
            <text class="price">{{ formatMoney(plan.price) }}</text>
            <text class="unit">/{{ plan.priceUnit }}</text>
          </view>
        </view>
        <view class="rights-preview">
          <text v-for="item in rightsPreview(plan)" :key="item">{{ item }}</text>
        </view>
        <button
          class="open-button"
          :class="{ ghost: isCurrent(plan) }"
          :disabled="isCurrent(plan)"
          :loading="submittingId === plan.id"
          @tap="createOrder(plan.id)"
        >
          {{ buttonText(plan) }}
        </button>
      </view>
    </view>

    <view class="scope-card">
      <text class="section-title">MVP 权益说明</text>
      <text class="scope-line">Excel 正式导出将在后续版本开放，本阶段先预留权益与提示。</text>
      <text class="scope-line">设备租赁只做订单基础闭环，复杂库存、押金、维修、归还后续迭代。</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { loadRuntimeFeatures, request, type RuntimeFeatures } from '../../../api/client';
import { requireBanquetToast, resolveBanquetId } from '../../../utils/banquet';

interface Plan {
  id: number;
  planCode: string;
  name: string;
  price: number;
  priceUnit: string;
  recommended?: number;
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
const localOrderKey = computed(() => banquetId.value ? `plan-order:${banquetId.value}` : '');

async function load() {
  const [runtimeFeatures, planList] = await Promise.all([
    loadRuntimeFeatures(),
    request<Plan[]>('/plans')
  ]);
  features.value = runtimeFeatures;
  plans.value = planList;
  await Promise.all([loadEntitlements(), loadOrders()]);
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

async function loadOrders() {
  if (!banquetId.value) {
    return;
  }
  const orders = await request<PlanOrder[]>(`/plans/orders?banquetId=${banquetId.value}`).catch(() => cachedOrders());
  pendingOrder.value = orders.find((item) => item.payStatus !== 'PAID');
}

async function createOrder(planId: number) {
  if (!banquetId.value) {
    uni.showToast({ title: '缺少宴席ID', icon: 'none' });
    return;
  }
  const plan = plans.value.find((item) => item.id === planId);
  if (plan && isCurrent(plan)) {
    uni.showToast({ title: '当前版本已启用', icon: 'none' });
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
      cacheOrder(order);
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
    clearCachedOrder(orderNo);
    await loadEntitlements();
    uni.showToast({ title: '版本已开通', icon: 'success' });
  } finally {
    paying.value = false;
  }
}

function cachedOrders(): PlanOrder[] {
  if (!localOrderKey.value) {
    return [];
  }
  return uni.getStorageSync(localOrderKey.value) || [];
}

function cacheOrder(order: PlanOrder) {
  if (!localOrderKey.value) {
    return;
  }
  const orders = [order, ...cachedOrders().filter((item) => item.orderNo !== order.orderNo)];
  uni.setStorageSync(localOrderKey.value, orders);
}

function clearCachedOrder(orderNo: string) {
  if (!localOrderKey.value) {
    return;
  }
  const orders = cachedOrders().filter((item) => item.orderNo !== orderNo);
  uni.setStorageSync(localOrderKey.value, orders);
}

function formatMoney(value: unknown) {
  return `¥${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })}`;
}

function isRecommended(plan: Plan) {
  return Number(plan.recommended || 0) === 1 || /STANDARD|PRO/i.test(plan.planCode);
}

function isCurrent(plan: Plan) {
  return entitlements.currentPlan?.id === plan.id;
}

function planIcon(plan: Plan) {
  if (Number(plan.price) === 0) return '礼';
  if (/PREMIUM|VIP|DIAMOND/i.test(plan.planCode)) return '钻';
  if (/STANDARD|PRO/i.test(plan.planCode)) return '冠';
  return '箱';
}

function planDesc(plan: Plan) {
  if (Number(plan.price) === 0) return '基础功能体验，适合小型宴席';
  if (/PREMIUM|VIP|DIAMOND/i.test(plan.planCode)) return '高阶权益组合，适合更完整的办宴流程';
  if (/STANDARD|PRO/i.test(plan.planCode)) return '核心功能齐全，覆盖收礼、设备和数据';
  return '满足日常宴席管理需求';
}

function rightsPreview(plan: Plan) {
  if (Number(plan.price) === 0) return ['创建宴席', '基础请柬', '回执管理'];
  if (/PREMIUM|VIP|DIAMOND/i.test(plan.planCode)) return ['包含设备租赁', '数据导出权益', '高级配置预留'];
  if (/STANDARD|PRO/i.test(plan.planCode)) return ['收礼记录', '设备租赁', '人情账本'];
  return ['请柬管理', '回执统计', '线下记礼'];
}

function buttonText(plan: Plan) {
  if (isCurrent(plan)) return '当前版本';
  if (Number(plan.price) === 0) return '启用基础版';
  return '立即开通';
}

function returnBanquetDetail() {
  if (!banquetId.value) {
    requireBanquetToast();
    return;
  }
  uni.navigateTo({
    url: `/pages/banquet/detail/index?id=${banquetId.value}`,
    fail: () => uni.redirectTo({ url: `/pages/banquet/detail/index?id=${banquetId.value}` })
  });
}

onMounted(async () => {
  const pages = getCurrentPages();
  const current = pages[pages.length - 1] as unknown as { options?: Record<string, string> };
  banquetId.value = await resolveBanquetId(current.options?.banquetId);
  if (!banquetId.value) {
    requireBanquetToast();
  }
  await load();
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

.hero-card {
  position: relative;
  overflow: hidden;
  padding: 34rpx;
  border-radius: 28rpx;
  background:
    radial-gradient(circle at 84% 18%, rgba(255, 217, 150, 0.38), transparent 180rpx),
    linear-gradient(135deg, #e71921 0%, #c9161c 62%, #9b0e13 100%);
  box-shadow: 0 16rpx 42rpx rgba(184, 17, 21, 0.24);
}

.hero-art {
  position: absolute;
  right: -34rpx;
  bottom: -62rpx;
  width: 250rpx;
  height: 250rpx;
  border-radius: 50%;
  background: rgba(255, 224, 170, 0.16);
}

.hero-symbol {
  position: absolute;
  right: 78rpx;
  bottom: 76rpx;
  color: rgba(255, 239, 206, 0.34);
  font-family: serif;
  font-size: 84rpx;
  font-weight: 900;
}

.hero-label,
.hero-title,
.hero-desc {
  position: relative;
  z-index: 2;
  display: block;
}

.hero-label {
  color: #ffe2ba;
  font-size: 26rpx;
  font-weight: 800;
}

.hero-title {
  margin-top: 14rpx;
  color: #fff8df;
  font-family: serif;
  font-size: 58rpx;
  font-weight: 900;
}

.hero-desc {
  width: 80%;
  margin-top: 12rpx;
  color: rgba(255, 248, 232, 0.94);
  font-size: 27rpx;
  line-height: 1.5;
}

.current-card,
.pending-card,
.plan-card,
.scope-card {
  margin-top: 24rpx;
  border: 1rpx solid #f0dfcf;
  border-radius: 24rpx;
  background: #fff;
  box-shadow: 0 12rpx 32rpx rgba(82, 45, 24, 0.07);
}

.current-card,
.pending-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  padding: 26rpx 28rpx;
}

.pending-card {
  border-color: #ffd9ad;
  background: #fff7ed;
}

.card-title,
.section-title {
  display: block;
  color: #171c2a;
  font-size: 31rpx;
  font-weight: 900;
}

.current-plan {
  display: block;
  margin-top: 8rpx;
  color: #c7191e;
  font-size: 38rpx;
  font-weight: 900;
}

.card-desc,
.order-no,
.pending-text {
  display: block;
  margin-top: 8rpx;
  color: #8a7768;
  font-size: 24rpx;
}

.right-tags {
  display: grid;
  gap: 10rpx;
  min-width: 150rpx;
}

.right-tags text {
  padding: 8rpx 14rpx;
  border-radius: 999rpx;
  background: #f3eee8;
  color: #9b8a7c;
  font-size: 22rpx;
  font-weight: 800;
  text-align: center;
}

.right-tags text.active {
  background: #ecfdf3;
  color: #138a45;
}

.small-button {
  height: 66rpx;
  margin: 0;
  padding: 0 24rpx;
  border-radius: 16rpx;
  font-size: 25rpx;
  font-weight: 900;
  line-height: 66rpx;
}

.small-button.primary {
  background: linear-gradient(135deg, #e83a32, #c91419);
  color: #fff;
}

.small-button::after,
.open-button::after,
.return-button::after {
  border: 0;
}

.return-button {
  height: 76rpx;
  margin: 24rpx 0 0;
  border: 1rpx solid #ead8ca;
  border-radius: 18rpx;
  background: #fff;
  color: #9e4d32;
  font-size: 27rpx;
  font-weight: 900;
  line-height: 76rpx;
}

.plans-list {
  margin-top: 24rpx;
}

.plan-card {
  padding: 26rpx;
}

.plan-card.recommended {
  border-color: #e83a32;
}

.plan-card.current {
  background: #fffdf8;
}

.plan-head {
  display: grid;
  grid-template-columns: 76rpx 1fr auto;
  gap: 18rpx;
  align-items: center;
}

.plan-icon {
  width: 76rpx;
  height: 76rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #ffe3c0, #d9a24b);
  color: #9a421a;
  font-size: 28rpx;
  font-weight: 900;
  line-height: 76rpx;
  text-align: center;
}

.name-line {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.plan-name {
  color: #171c2a;
  font-size: 32rpx;
  font-weight: 900;
}

.badge {
  padding: 5rpx 10rpx;
  border-radius: 999rpx;
  background: #e83a32;
  color: #fff;
  font-size: 20rpx;
  font-weight: 900;
}

.current-badge {
  background: #ecfdf3;
  color: #138a45;
}

.plan-desc {
  display: block;
  margin-top: 10rpx;
  color: #7f7167;
  font-size: 24rpx;
  line-height: 1.4;
}

.price-box {
  text-align: right;
}

.price {
  color: #c7191e;
  font-size: 38rpx;
  font-weight: 900;
}

.unit {
  color: #9b806a;
  font-size: 22rpx;
}

.rights-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 22rpx;
}

.rights-preview text {
  padding: 9rpx 14rpx;
  border-radius: 999rpx;
  background: #fff7ed;
  color: #9a5b30;
  font-size: 22rpx;
  font-weight: 800;
}

.open-button {
  height: 86rpx;
  margin: 24rpx 0 0;
  border-radius: 18rpx;
  background: linear-gradient(135deg, #e83a32, #c91419);
  color: #fff;
  font-size: 29rpx;
  font-weight: 900;
  line-height: 86rpx;
}

.open-button.ghost {
  border: 1rpx solid #ead8ca;
  background: #fffaf5;
  color: #9e4d32;
}

.scope-card {
  margin-bottom: 30rpx;
  padding: 28rpx;
}

.scope-line {
  display: block;
  margin-top: 14rpx;
  color: #7f7167;
  font-size: 25rpx;
  line-height: 1.55;
}
</style>
