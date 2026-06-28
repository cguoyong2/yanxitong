<template>
  <view class="page">
    <view class="hero-card">
      <view class="hero-art">
        <text class="hero-symbol">屏</text>
      </view>
      <text class="hero-label">宴席通设备服务</text>
      <text class="hero-title">设备租赁</text>
      <text class="hero-desc">确认屏、云喇叭按后台配置展示价格、单位与交付方式。</text>
      <view class="right-pill" :class="{ blocked: !hasDeviceRight }">
        {{ hasDeviceRight ? '已开通设备权益' : '暂未开通设备权益' }}
      </view>
    </view>

    <view class="rights-card" :class="{ blocked: !hasDeviceRight }">
      <view>
        <text class="card-title">当前版本</text>
        <text class="card-desc">{{ entitlements.currentPlan?.name || '基础版' }}</text>
      </view>
      <button v-if="!hasDeviceRight" class="small-button primary" @tap="openPlan">去开通版本</button>
      <text v-else class="right-ok">可租用</text>
    </view>

    <view class="time-card">
      <text class="section-title">租用时间</text>
      <view class="time-grid">
        <picker mode="date" :value="rentDate" @change="rentDate = String($event.detail.value)">
          <view class="time-cell">
            <text class="time-label">租用日期</text>
            <text class="time-value">{{ rentDate }}</text>
          </view>
        </picker>
        <picker mode="time" :value="rentStartTime" @change="rentStartTime = String($event.detail.value)">
          <view class="time-cell">
            <text class="time-label">开始时间</text>
            <text class="time-value">{{ rentStartTime }}</text>
          </view>
        </picker>
        <picker mode="time" :value="rentEndTime" @change="rentEndTime = String($event.detail.value)">
          <view class="time-cell">
            <text class="time-label">结束时间</text>
            <text class="time-value">{{ rentEndTime }}</text>
          </view>
        </picker>
      </view>
    </view>

    <view v-if="orders.length" class="orders-card">
      <view class="section-head">
        <text class="section-title">已租设备</text>
        <text class="section-note">{{ orders.length }} 单</text>
      </view>
      <view v-for="order in orders" :key="order.orderNo" class="order-row">
        <view class="order-main">
          <text class="order-title">{{ deviceTypeLabel(order.deviceType) }}</text>
          <text class="order-meta">{{ order.orderNo }}</text>
          <text class="order-meta">{{ statusLabel(order.payStatus) }} · {{ orderStatusLabel(order.orderStatus) }}</text>
        </view>
        <view class="order-side">
          <text class="order-price">{{ formatMoney(order.price) }}</text>
          <button
            v-if="features.mockPaymentEnabled && order.payStatus !== 'PAID'"
            class="pay-button"
            :loading="payingOrderNo === order.orderNo"
            @tap="mockPay(order.orderNo)"
          >
            模拟支付
          </button>
        </view>
      </view>
    </view>

    <view class="configs-card">
      <view class="section-head">
        <text class="section-title">设备方案</text>
        <text class="section-note">后台配置</text>
      </view>
      <view v-if="configs.length === 0" class="empty">暂无可租设备</view>
      <view v-for="config in configs" :key="config.id" class="device-card">
        <view class="device-head">
          <view class="device-icon" :class="deviceTone(config.deviceType)">{{ deviceIcon(config.deviceType) }}</view>
          <view class="device-main">
            <text class="device-name">{{ config.name }}</text>
            <text class="device-desc">{{ deviceDesc(config.deviceType) }}</text>
          </view>
          <view class="price-box">
            <text class="price">{{ formatMoney(config.price) }}</text>
            <text class="unit">/{{ config.priceUnit }}</text>
          </view>
        </view>
        <view class="device-meta">
          <text>{{ deviceTypeLabel(config.deviceType) }}</text>
          <text>{{ deliveryLabel(config.deliveryMethod) }}</text>
        </view>
        <button
          class="rent-button"
          :class="{ disabled: !hasDeviceRight }"
          :loading="submittingId === config.id"
          @tap="createOrder(config)"
        >
          {{ hasDeviceRight ? '租用此设备' : '需先开通权益' }}
        </button>
      </view>
    </view>

    <view class="scope-card">
      <text class="section-title">MVP 服务范围</text>
      <text class="scope-line">仅包含设备需求、租用时间、价格单位、交付方式、支付状态和后台查看。</text>
      <text class="scope-line">暂不包含库存排期、押金、维修、归还和结算流程。</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { loadRuntimeFeatures, request, type RuntimeFeatures } from '../../../api/client';
import { requireBanquetToast, resolveBanquetId } from '../../../utils/banquet';

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
  price?: number;
  priceUnit?: string;
  deliveryMethod?: string;
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
const rentDate = ref('');
const rentStartTime = ref('10:00');
const rentEndTime = ref('22:00');
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
        deliveryMethod: config.deliveryMethod,
        rentStartAt: toLocalDateTime(rentDate.value, rentStartTime.value),
        rentEndAt: toLocalDateTime(rentDate.value, rentEndTime.value)
      }
    });
    orders.value = [order, ...orders.value.filter((item) => item.orderNo !== order.orderNo)];
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

function toLocalDateTime(date: string, time: string) {
  return date && time ? `${date}T${time}:00` : undefined;
}

function openPlan() {
  if (banquetId.value) {
    uni.navigateTo({ url: `/pages/order/plan/index?banquetId=${banquetId.value}` });
  }
}

function setDefaultDate() {
  const now = new Date();
  const pad = (value: number) => String(value).padStart(2, '0');
  rentDate.value = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

function formatMoney(value: unknown) {
  return `¥${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })}`;
}

function deviceTypeLabel(value: string) {
  const labels: Record<string, string> = {
    CONFIRM_SCREEN: '确认屏',
    CLOUD_SPEAKER: '云喇叭'
  };
  return labels[value] || value;
}

function deviceIcon(value: string) {
  return value === 'CLOUD_SPEAKER' ? '喇' : '屏';
}

function deviceTone(value: string) {
  return value === 'CLOUD_SPEAKER' ? 'orange' : 'red';
}

function deviceDesc(value: string) {
  if (value === 'CLOUD_SPEAKER') return '礼金到账后模拟播报祝福语';
  if (value === 'CONFIRM_SCREEN') return '礼金成功后大屏展示宾客与金额';
  return '适用于宴席现场设备服务';
}

function deliveryLabel(value?: string) {
  const labels: Record<string, string> = {
    EXPRESS: '快递交付',
    SELF_PICKUP: '到店自提',
    STAFF_DELIVERY: '工作人员配送',
    ONSITE: '现场交付'
  };
  return value ? labels[value] || value : '交付方式待定';
}

function statusLabel(value: string) {
  const labels: Record<string, string> = {
    UNPAID: '待支付',
    PAID: '已支付',
    REFUNDED: '已退款'
  };
  return labels[value] || value;
}

function orderStatusLabel(value: string) {
  const labels: Record<string, string> = {
    CREATED: '已创建',
    CONFIRMED: '已确认',
    DELIVERING: '配送中',
    DELIVERED: '已交付',
    CANCELLED: '已取消'
  };
  return labels[value] || value;
}

onMounted(async () => {
  const pages = getCurrentPages();
  const current = pages[pages.length - 1] as unknown as { options?: Record<string, string> };
  banquetId.value = await resolveBanquetId(current.options?.banquetId);
  if (!banquetId.value) {
    requireBanquetToast();
  }
  setDefaultDate();
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
  right: 80rpx;
  bottom: 72rpx;
  color: rgba(255, 239, 206, 0.34);
  font-family: serif;
  font-size: 90rpx;
  font-weight: 900;
}

.hero-label,
.hero-title,
.hero-desc,
.right-pill {
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

.right-pill {
  display: inline-block;
  margin-top: 28rpx;
  padding: 12rpx 22rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.18);
  color: #fff2d7;
  font-size: 24rpx;
  font-weight: 800;
}

.right-pill.blocked {
  background: rgba(255, 255, 255, 0.12);
}

.rights-card,
.time-card,
.orders-card,
.configs-card,
.scope-card {
  margin-top: 24rpx;
  border: 1rpx solid #f0dfcf;
  border-radius: 24rpx;
  background: #fff;
  box-shadow: 0 12rpx 32rpx rgba(82, 45, 24, 0.07);
}

.rights-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 26rpx 28rpx;
}

.rights-card.blocked {
  background: #fff7ed;
}

.card-title,
.section-title {
  display: block;
  color: #171c2a;
  font-size: 32rpx;
  font-weight: 900;
}

.card-desc,
.section-note {
  display: block;
  margin-top: 8rpx;
  color: #8a7768;
  font-size: 24rpx;
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
.pay-button::after,
.rent-button::after {
  border: 0;
}

.right-ok {
  padding: 10rpx 20rpx;
  border-radius: 999rpx;
  background: #ecfdf3;
  color: #138a45;
  font-size: 24rpx;
  font-weight: 900;
}

.time-card,
.orders-card,
.configs-card,
.scope-card {
  padding: 28rpx;
}

.time-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14rpx;
  margin-top: 20rpx;
}

.time-cell {
  padding: 18rpx 12rpx;
  border: 1rpx solid #ead8ca;
  border-radius: 18rpx;
  background: #fffaf5;
  text-align: center;
}

.time-label,
.time-value {
  display: block;
}

.time-label {
  color: #9b806a;
  font-size: 22rpx;
  font-weight: 700;
}

.time-value {
  margin-top: 8rpx;
  color: #171c2a;
  font-size: 25rpx;
  font-weight: 900;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.order-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  padding: 22rpx 0;
  border-bottom: 1rpx solid #f0dfcf;
}

.order-row:last-child {
  border-bottom: 0;
}

.order-title,
.order-price {
  display: block;
  color: #171c2a;
  font-size: 29rpx;
  font-weight: 900;
}

.order-meta {
  display: block;
  margin-top: 6rpx;
  color: #8a7768;
  font-size: 23rpx;
}

.order-side {
  display: grid;
  justify-items: end;
  gap: 10rpx;
}

.order-price {
  color: #c7191e;
}

.pay-button {
  height: 56rpx;
  margin: 0;
  padding: 0 18rpx;
  border-radius: 999rpx;
  background: #fff0ea;
  color: #c7191e;
  font-size: 23rpx;
  font-weight: 900;
  line-height: 56rpx;
}

.empty {
  padding: 48rpx 20rpx;
  border: 1rpx dashed #ead8ca;
  border-radius: 18rpx;
  background: #fffaf6;
  color: #9a6a4c;
  text-align: center;
}

.device-card {
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f0dfcf;
}

.device-card:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.device-head {
  display: grid;
  grid-template-columns: 76rpx 1fr auto;
  gap: 18rpx;
  align-items: center;
}

.device-icon {
  width: 76rpx;
  height: 76rpx;
  border-radius: 22rpx;
  color: #fff;
  font-size: 28rpx;
  font-weight: 900;
  line-height: 76rpx;
  text-align: center;
}

.device-icon.red {
  background: linear-gradient(135deg, #e83a32, #c91419);
}

.device-icon.orange {
  background: linear-gradient(135deg, #f2994a, #d45b1f);
}

.device-name {
  display: block;
  color: #171c2a;
  font-size: 31rpx;
  font-weight: 900;
}

.device-desc {
  display: block;
  margin-top: 8rpx;
  color: #7f7167;
  font-size: 24rpx;
  line-height: 1.4;
}

.price-box {
  text-align: right;
}

.price {
  color: #c7191e;
  font-size: 32rpx;
  font-weight: 900;
}

.unit {
  color: #9b806a;
  font-size: 22rpx;
}

.device-meta {
  display: flex;
  gap: 14rpx;
  margin-top: 20rpx;
}

.device-meta text {
  padding: 8rpx 14rpx;
  border-radius: 999rpx;
  background: #fff7ed;
  color: #9a5b30;
  font-size: 22rpx;
  font-weight: 800;
}

.rent-button {
  height: 82rpx;
  margin: 22rpx 0 0;
  border-radius: 18rpx;
  background: linear-gradient(135deg, #e83a32, #c91419);
  color: #fff;
  font-size: 28rpx;
  font-weight: 900;
  line-height: 82rpx;
}

.rent-button.disabled {
  background: #e5d8ca;
  color: #8b735f;
}

.scope-card {
  margin-bottom: 30rpx;
}

.scope-line {
  display: block;
  margin-top: 14rpx;
  color: #7f7167;
  font-size: 25rpx;
  line-height: 1.55;
}
</style>
