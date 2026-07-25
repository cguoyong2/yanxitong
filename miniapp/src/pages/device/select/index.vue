<template>
  <view class="page" :class="activeTheme.tone">
    <view class="hero-card">
      <view class="hero-art">
        <text class="hero-symbol">{{ activeTheme.mark }}</text>
      </view>
      <text class="hero-label">情礼记设备服务</text>
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
      <button v-if="!hasDeviceRight" class="small-button primary" @tap="openPlan()">去开通版本</button>
      <text v-else class="right-ok">可租用</text>
    </view>

    <button v-if="banquetId" class="return-button" @tap="returnBanquetDetail()">返回宴席管理台</button>

    <view v-if="lastOrderText" class="success-card">
      <text class="success-title">设备订单已记录</text>
      <text class="success-desc">{{ lastOrderText }}</text>
      <view class="success-actions">
        <button class="small-button" @tap="refreshOrders()">刷新订单</button>
        <button class="small-button primary" @tap="returnBanquetDetail()">返回管理台</button>
      </view>
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

    <view class="orders-card">
      <view class="section-head">
        <text class="section-title">设备订单跟踪</text>
        <button class="refresh-btn" :loading="loadingOrders" @tap="refreshOrders()">
          刷新 {{ orders.length ? `${orders.length} 单` : '暂无订单' }}
        </button>
      </view>
      <view v-if="!orders.length" class="order-empty">
        <text class="empty-title">还没有设备订单</text>
        <text class="empty-desc">选择下方确认屏或云喇叭后，会在这里显示订单、租用时间、交付方式和支付状态。</text>
      </view>
      <view v-for="order in orders" :key="order.orderNo" class="order-row" :class="{ highlight: isHighlightedOrder(order.orderNo) }">
        <view class="order-main">
          <text class="order-title">{{ deviceTypeLabel(order.deviceType) }}</text>
          <text v-if="isHighlightedOrder(order.orderNo)" class="order-highlight">当前查看的订单</text>
          <text class="order-meta">{{ order.orderNo }}</text>
          <text class="order-meta">{{ formatRentWindow(order) }}</text>
          <text class="order-meta">{{ deliveryLabel(order.deliveryMethod) }} · {{ formatTime(order.createdAt) }}</text>
          <text class="order-next">{{ deviceOrderTip(order) }}</text>
        </view>
        <view class="order-side">
          <text class="order-price">{{ formatMoney(order.price) }}</text>
          <text class="status-tag" :class="{ paid: order.payStatus === 'PAID' }">
            {{ statusLabel(order.payStatus) }} · {{ orderStatusLabel(order.orderStatus) }}
          </text>
          <button
            v-if="features.mockPaymentEnabled && order.payStatus !== 'PAID'"
            class="pay-button"
            :loading="payingOrderNo === order.orderNo"
            @tap="mockPay(order.orderNo)"
          >
            模拟支付
          </button>
          <button
            v-else-if="order.payStatus === 'PAID'"
            class="pay-button"
            @tap="returnBanquetDetail()"
          >
            返回管理台
          </button>
          <button
            v-else-if="order.payStatus !== 'PAID'"
            class="pay-button"
            @tap="openPaymentPanel(order)"
          >
            去支付
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

    <view v-if="paymentPanel.visible" class="payment-mask" @tap="closePaymentPanel()">
      <view class="payment-sheet" @tap.stop>
        <view class="sheet-handle"></view>
        <view class="payment-head">
          <view>
            <text class="payment-label">设备订单支付</text>
            <text class="payment-title">{{ paymentDeviceName }}</text>
          </view>
          <button class="close-button" @tap="closePaymentPanel()">×</button>
        </view>
        <view class="amount-card">
          <text class="amount-label">应付金额</text>
          <text class="amount-value">{{ formatMoney(paymentPanel.order?.price) }}</text>
          <text class="amount-unit">/{{ paymentPanel.order?.priceUnit || '场' }}</text>
        </view>
        <view class="pay-info">
          <view>
            <text>订单编号</text>
            <text>{{ paymentPanel.order?.orderNo || '-' }}</text>
          </view>
          <view>
            <text>租用时间</text>
            <text>{{ paymentPanel.order ? formatRentWindow(paymentPanel.order) : '-' }}</text>
          </view>
          <view>
            <text>交付方式</text>
            <text>{{ deliveryLabel(paymentPanel.order?.deliveryMethod) }}</text>
          </view>
          <view>
            <text>支付方式</text>
            <text>微信支付</text>
          </view>
        </view>
        <button class="confirm-pay-button" :loading="Boolean(payingOrderNo)" @tap="payOrder(paymentPanel.order)">确认支付</button>
        <text class="payment-note">支付完成后，微信回调会自动确认设备订单。</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { loadRuntimeFeatures, request, type RuntimeFeatures } from '../../../api/client';
import { requireBanquetToast, resolveBanquetId } from '../../../utils/banquet';
import { eventThemeFor, fetchBanquetEventType, readActiveEventType, writeActiveEventType } from '../../../utils/event-theme';
import { createBusinessPayment, normalizePaymentFlowError, requestWechatPayment } from '../../../utils/wechat-payment';

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
  rentStartAt?: string;
  rentEndAt?: string;
  price?: number;
  priceUnit?: string;
  deliveryMethod?: string;
  payStatus: string;
  orderStatus: string;
  createdAt?: string;
}

const configs = ref<DeviceConfig[]>([]);
const orders = ref<DeviceOrder[]>([]);
const banquetId = ref('');
const submittingId = ref<number>();
const payingOrderNo = ref('');
const loadingOrders = ref(false);
const lastOrderText = ref('');
const highlightOrderNo = ref('');
const eventType = ref(readActiveEventType());
const activeTheme = computed(() => eventThemeFor(eventType.value));
const paymentPanel = reactive<{ visible: boolean; order?: DeviceOrder }>({
  visible: false
});
const entitlements = reactive<Entitlements>({
  rightValues: {}
});
const features = ref<RuntimeFeatures>({ mockPaymentEnabled: false });
const rentDate = ref('');
const rentStartTime = ref('10:00');
const rentEndTime = ref('22:00');
const hasDeviceRight = computed(() => Boolean(entitlements.rightValues.DEVICE_RENTAL));
const paymentDeviceName = computed(() => paymentPanel.order ? deviceTypeLabel(paymentPanel.order.deviceType) : '设备租赁');

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
  loadingOrders.value = true;
  try {
    const remoteOrders = await request<DeviceOrder[]>(`/devices/orders?banquetId=${banquetId.value}`).catch(() => cachedOrders());
    const mergedOrders = prioritizeHighlightedOrders(mergeOrders(remoteOrders, cachedOrders()));
    orders.value = mergedOrders;
    syncCachedOrders(mergedOrders);
  } finally {
    loadingOrders.value = false;
  }
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
  if (!validateRentWindow()) {
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
    highlightOrderNo.value = order.orderNo;
    orders.value = prioritizeHighlightedOrders([order, ...orders.value.filter((item) => item.orderNo !== order.orderNo)]);
    cacheOrder(order);
    lastOrderText.value = `${deviceTypeLabel(order.deviceType)} · ${formatRentWindow(order)} · ${deliveryLabel(order.deliveryMethod)}`;
    uni.showToast({ title: '设备订单已创建', icon: 'success' });
    if (!features.value.mockPaymentEnabled && order.payStatus !== 'PAID') {
      openPaymentPanel(order);
    }
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '设备订单创建失败', icon: 'none' });
  } finally {
    submittingId.value = undefined;
  }
}

async function mockPay(orderNo: string) {
  payingOrderNo.value = orderNo;
  try {
    await request(`/devices/orders/${orderNo}/mock-success`, { method: 'POST' });
    await loadOrders();
    clearCachedOrder(orderNo);
    lastOrderText.value = '设备订单支付状态已确认，可返回管理台查看进度。';
    uni.showToast({ title: '设备订单已确认', icon: 'success' });
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '设备支付确认失败', icon: 'none' });
  } finally {
    payingOrderNo.value = '';
  }
}

async function refreshOrders() {
  try {
    await loadOrders();
    uni.showToast({ title: '设备订单已刷新', icon: 'success' });
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '设备订单刷新失败', icon: 'none' });
  }
}

function toLocalDateTime(date: string, time: string) {
  return date && time ? `${date}T${time}:00` : undefined;
}

function cachedOrders(): DeviceOrder[] {
  if (!banquetId.value) {
    return [];
  }
  return uni.getStorageSync(deviceOrderCacheKey()) || [];
}

function cacheOrder(order: DeviceOrder) {
  if (!banquetId.value) {
    return;
  }
  const rows = [order, ...cachedOrders().filter((item) => item.orderNo !== order.orderNo)];
  uni.setStorageSync(deviceOrderCacheKey(), rows);
}

function clearCachedOrder(orderNo: string) {
  if (!banquetId.value) {
    return;
  }
  const rows = cachedOrders().filter((item) => item.orderNo !== orderNo);
  uni.setStorageSync(deviceOrderCacheKey(), rows);
}

function syncCachedOrders(rows: DeviceOrder[]) {
  if (!banquetId.value) {
    return;
  }
  uni.setStorageSync(deviceOrderCacheKey(), rows);
}

function deviceOrderCacheKey() {
  return `device-order:${banquetId.value}`;
}

function mergeOrders(primary: DeviceOrder[], fallback: DeviceOrder[]) {
  const byOrderNo = new Map<string, DeviceOrder>();
  for (const item of [...fallback, ...primary]) {
    byOrderNo.set(item.orderNo, item);
  }
  return Array.from(byOrderNo.values()).sort((a, b) => String(b.createdAt || b.orderNo).localeCompare(String(a.createdAt || a.orderNo)));
}

function prioritizeHighlightedOrders(nextOrders: DeviceOrder[]) {
  if (!highlightOrderNo.value) {
    return nextOrders;
  }
  return [...nextOrders].sort((a, b) => Number(isHighlightedOrder(b.orderNo)) - Number(isHighlightedOrder(a.orderNo)));
}

function isHighlightedOrder(orderNo: string) {
  return Boolean(highlightOrderNo.value && orderNo === highlightOrderNo.value);
}

function validateRentWindow() {
  const start = toLocalDateTime(rentDate.value, rentStartTime.value);
  const end = toLocalDateTime(rentDate.value, rentEndTime.value);
  if (!start || !end) {
    uni.showToast({ title: '请选择完整租用时间', icon: 'none' });
    return false;
  }
  if (new Date(end).getTime() <= new Date(start).getTime()) {
    uni.showToast({ title: '结束时间需晚于开始时间', icon: 'none' });
    return false;
  }
  return true;
}

function openPlan() {
  if (banquetId.value) {
    uni.navigateTo({
      url: `/pages/order/plan/index?banquetId=${banquetId.value}`,
      fail: () => uni.showToast({ title: '版本页面打开失败', icon: 'none' })
    });
  }
}

function openPaymentPanel(order?: DeviceOrder) {
  if (!order) {
    return;
  }
  paymentPanel.order = order;
  paymentPanel.visible = true;
}

function closePaymentPanel() {
  paymentPanel.visible = false;
}

async function payOrder(order?: DeviceOrder) {
  if (!order) {
    uni.showToast({ title: '缺少订单信息', icon: 'none' });
    return;
  }
  if (features.value.mockPaymentEnabled) {
    await mockPay(order.orderNo);
    closePaymentPanel();
    return;
  }
  payingOrderNo.value = order.orderNo;
  try {
    const result = await createBusinessPayment(`/devices/orders/${order.orderNo}/payment`);
    await requestWechatPayment(result.payPayload);
    closePaymentPanel();
    lastOrderText.value = '支付已提交，微信回调确认后会自动更新设备订单状态。';
    uni.showToast({ title: '支付已提交', icon: 'success' });
    await loadOrders();
  } catch (error) {
    if (isAlreadyPaidError(error)) {
      closePaymentPanel();
      clearCachedOrder(order.orderNo);
      await loadOrders();
      lastOrderText.value = '订单已支付，设备服务已进入后续处理。';
      uni.showToast({ title: '订单已支付', icon: 'success' });
      return;
    }
    uni.showToast({ title: normalizePaymentFlowError(error, '设备支付失败'), icon: 'none' });
  } finally {
    payingOrderNo.value = '';
  }
}

function isAlreadyPaidError(error: unknown) {
  const message = error instanceof Error ? error.message : String(error || '');
  return /already paid|订单已支付/i.test(message);
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

function setDefaultDate() {
  const now = new Date();
  const pad = (value: number) => String(value).padStart(2, '0');
  rentDate.value = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

function formatMoney(value: unknown) {
  return `¥${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })}`;
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '刚刚创建';
}

function formatRentWindow(order: DeviceOrder) {
  const start = formatTime(order.rentStartAt);
  const end = order.rentEndAt ? formatTime(order.rentEndAt).slice(11) : '结束待定';
  return `${start} - ${end}`;
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
  if (value === 'CLOUD_SPEAKER') return `${activeTheme.value.giftLabel}到账后模拟播报${activeTheme.value.blessingLabel}`;
  if (value === 'CONFIRM_SCREEN') return `${activeTheme.value.giftLabel}成功后大屏展示宾客与金额`;
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

function deviceOrderTip(order: DeviceOrder) {
  if (order.payStatus !== 'PAID') {
    return features.value.mockPaymentEnabled
      ? '待支付，体验环境可模拟支付确认设备订单。'
      : '待支付，真实支付上线后会从这里继续完成付款。';
  }
  if (order.orderStatus === 'CONFIRMED') {
    return '已确认，运营后台可继续跟进交付方式和现场安排。';
  }
  if (order.orderStatus === 'DELIVERING') {
    return '配送中，请关注交付方式和现场接收。';
  }
  if (order.orderStatus === 'DELIVERED') {
    return '已交付，可在宴席现场使用对应设备。';
  }
  return '已支付，等待运营确认设备交付安排。';
}

onMounted(async () => {
  const pages = getCurrentPages();
  const current = pages[pages.length - 1] as unknown as { options?: Record<string, string> };
  banquetId.value = await resolveBanquetId(current.options?.banquetId);
  highlightOrderNo.value = current.options?.highlightOrderNo ? decodeURIComponent(current.options.highlightOrderNo) : '';
  if (!banquetId.value) {
    requireBanquetToast();
  } else {
    eventType.value = writeActiveEventType(await fetchBanquetEventType(banquetId.value, request, eventType.value));
  }
  setDefaultDate();
  await load();
});
</script>

<style scoped>
.page {
  --accent: #e60012;
  --accent-dark: #c40005;
  --accent-soft: #fff0ee;
  --page-bg: #fff8ef;
  --accent-shadow: rgba(184, 17, 21, 0.22);
  min-height: 100vh;
  padding: 24rpx;
  background: var(--page-bg);
  box-sizing: border-box;
  color: #171c2a;
}

.page.orange {
  --accent: #d96a11;
  --accent-dark: #a64209;
  --accent-soft: #fff3e3;
  --page-bg: #fbf4eb;
  --accent-shadow: rgba(166, 86, 17, 0.2);
}

.page.pink {
  --accent: #e7566f;
  --accent-dark: #b52d4c;
  --accent-soft: #fff0f4;
  --page-bg: #fff6f8;
  --accent-shadow: rgba(183, 45, 76, 0.18);
}

.page.green {
  --accent: #188356;
  --accent-dark: #0c5f3e;
  --accent-soft: #edf9f1;
  --page-bg: #f2f8f4;
  --accent-shadow: rgba(12, 95, 62, 0.17);
}

.page.blue {
  --accent: #2563eb;
  --accent-dark: #1d4ed8;
  --accent-soft: #edf4ff;
  --page-bg: #f2f6ff;
  --accent-shadow: rgba(29, 78, 216, 0.17);
}

.page.black {
  --accent: #2f3338;
  --accent-dark: #0d0f12;
  --accent-soft: #f1f2f4;
  --page-bg: #f3f4f5;
  --accent-shadow: rgba(13, 15, 18, 0.2);
}

.page.purple {
  --accent: #7c3aed;
  --accent-dark: #5b21b6;
  --accent-soft: #f4efff;
  --page-bg: #f7f3ff;
  --accent-shadow: rgba(91, 33, 182, 0.18);
}

.hero-card {
  position: relative;
  overflow: hidden;
  padding: 34rpx;
  border-radius: 28rpx;
  background:
    radial-gradient(circle at 84% 18%, rgba(255, 217, 150, 0.38), transparent 180rpx),
    linear-gradient(135deg, var(--accent) 0%, var(--accent-dark) 62%, var(--accent-dark) 100%);
  box-shadow: 0 16rpx 42rpx var(--accent-shadow);
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
.scope-card,
.success-card {
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
  background: var(--accent-soft);
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
  background: linear-gradient(135deg, var(--accent), var(--accent-dark));
  color: #fff;
}

.small-button::after,
.pay-button::after,
.rent-button::after,
.return-button::after,
.close-button::after,
.confirm-pay-button::after {
  border: 0;
}

.return-button {
  height: 76rpx;
  margin: 24rpx 0 0;
  border: 1rpx solid #ead8ca;
  border-radius: 18rpx;
  background: #fff;
  color: var(--accent);
  font-size: 27rpx;
  font-weight: 900;
  line-height: 76rpx;
}

.success-card {
  padding: 26rpx 28rpx;
  border-color: #b8ebc8;
  background: linear-gradient(180deg, #f0fff5, #fff);
}

.success-title,
.success-desc {
  display: block;
}

.success-title {
  color: #166534;
  font-size: 30rpx;
  font-weight: 900;
}

.success-desc {
  margin-top: 8rpx;
  color: #54705d;
  font-size: 24rpx;
  line-height: 1.5;
}

.success-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
  margin-top: 20rpx;
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
  background: var(--accent-soft);
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

.refresh-btn {
  height: 58rpx;
  margin: 0;
  padding: 0 22rpx;
  border: 1rpx solid #ead8ca;
  border-radius: 999rpx;
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 24rpx;
  font-weight: 800;
  line-height: 58rpx;
}

.refresh-btn::after {
  border: 0;
}

.order-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  padding: 22rpx 0;
  border-bottom: 1rpx solid #f0dfcf;
}

.order-row.highlight {
  margin: 14rpx 0;
  padding: 22rpx;
  border: 2rpx solid var(--accent);
  border-radius: 18rpx;
  background: linear-gradient(90deg, var(--accent-soft), #fff);
}

.order-empty {
  padding: 30rpx 24rpx;
  border: 1rpx dashed #ead8ca;
  border-radius: 18rpx;
  background: var(--accent-soft);
}

.empty-title,
.empty-desc {
  display: block;
}

.empty-title {
  color: #171c2a;
  font-size: 28rpx;
  font-weight: 900;
}

.empty-desc {
  margin-top: 10rpx;
  color: #8a7768;
  font-size: 24rpx;
  line-height: 1.5;
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

.order-main {
  min-width: 0;
}

.order-meta {
  display: block;
  overflow: hidden;
  margin-top: 6rpx;
  color: #8a7768;
  font-size: 23rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-next {
  display: block;
  max-width: 390rpx;
  margin-top: 8rpx;
  color: #6f6259;
  font-size: 22rpx;
  line-height: 1.4;
  white-space: normal;
}

.order-highlight {
  display: inline-block;
  margin-top: 8rpx;
  padding: 5rpx 10rpx;
  border-radius: 999rpx;
  background: var(--accent);
  color: #fff;
  font-size: 20rpx;
  font-weight: 900;
}

.order-side {
  display: grid;
  justify-items: end;
  gap: 10rpx;
  min-width: 176rpx;
}

.order-price {
  color: var(--accent);
}

.status-tag {
  padding: 7rpx 12rpx;
  border-radius: 999rpx;
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 21rpx;
  font-weight: 900;
  white-space: nowrap;
}

.status-tag.paid {
  background: #ecfdf3;
  color: #138a45;
}

.pay-button {
  height: 56rpx;
  margin: 0;
  padding: 0 18rpx;
  border-radius: 999rpx;
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 23rpx;
  font-weight: 900;
  line-height: 56rpx;
}

.empty {
  padding: 48rpx 20rpx;
  border: 1rpx dashed #ead8ca;
  border-radius: 18rpx;
  background: var(--accent-soft);
  color: var(--accent);
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
  background: linear-gradient(135deg, var(--accent), var(--accent-dark));
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
  color: var(--accent);
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
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 22rpx;
  font-weight: 800;
}

.rent-button {
  height: 82rpx;
  margin: 22rpx 0 0;
  border-radius: 18rpx;
  background: linear-gradient(135deg, var(--accent), var(--accent-dark));
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

.payment-mask {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  background: rgba(16, 18, 24, 0.46);
}

.payment-sheet {
  width: 100%;
  max-height: 82vh;
  padding: 18rpx 30rpx 42rpx;
  border-radius: 34rpx 34rpx 0 0;
  background: linear-gradient(180deg, #fffdf8 0%, #fff 44%, var(--accent-soft) 100%);
  box-shadow: 0 -18rpx 56rpx rgba(17, 24, 39, 0.18);
  box-sizing: border-box;
}

.sheet-handle {
  width: 76rpx;
  height: 8rpx;
  margin: 0 auto 24rpx;
  border-radius: 999rpx;
  background: #ead8ca;
}

.payment-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18rpx;
}

.payment-label,
.payment-title,
.amount-label,
.amount-value,
.amount-unit,
.payment-note {
  display: block;
}

.payment-label {
  color: var(--accent);
  font-size: 24rpx;
  font-weight: 900;
}

.payment-title {
  margin-top: 10rpx;
  color: #171c2a;
  font-size: 42rpx;
  font-weight: 900;
}

.close-button {
  width: 62rpx;
  height: 62rpx;
  margin: 0;
  padding: 0;
  border-radius: 50%;
  background: #f6efe8;
  color: #8a7768;
  font-size: 42rpx;
  line-height: 56rpx;
}

.amount-card {
  position: relative;
  overflow: hidden;
  margin-top: 28rpx;
  padding: 32rpx;
  border-radius: 28rpx;
  background:
    radial-gradient(circle at 88% 18%, rgba(255, 255, 255, 0.22), transparent 150rpx),
    linear-gradient(135deg, var(--accent), var(--accent-dark));
  box-shadow: 0 16rpx 42rpx var(--accent-shadow);
}

.amount-label {
  color: rgba(255, 248, 232, 0.82);
  font-size: 25rpx;
  font-weight: 800;
}

.amount-value {
  margin-top: 8rpx;
  color: #fff8df;
  font-size: 64rpx;
  font-weight: 900;
  line-height: 1.1;
}

.amount-unit {
  position: absolute;
  right: 32rpx;
  bottom: 34rpx;
  color: rgba(255, 248, 232, 0.86);
  font-size: 27rpx;
  font-weight: 900;
}

.pay-info {
  margin-top: 24rpx;
  padding: 8rpx 26rpx;
  border: 1rpx solid #f0dfcf;
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.9);
}

.pay-info view {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  padding: 23rpx 0;
  border-bottom: 1rpx solid #f2e7dc;
}

.pay-info view:last-child {
  border-bottom: 0;
}

.pay-info text:first-child {
  flex: 0 0 auto;
  color: #8a7768;
  font-size: 25rpx;
  font-weight: 800;
}

.pay-info text:last-child {
  min-width: 0;
  color: #171c2a;
  font-size: 25rpx;
  font-weight: 900;
  text-align: right;
  word-break: break-all;
}

.confirm-pay-button {
  height: 92rpx;
  margin: 28rpx 0 0;
  border-radius: 22rpx;
  background: linear-gradient(135deg, var(--accent), var(--accent-dark));
  color: #fff;
  font-size: 31rpx;
  font-weight: 900;
  line-height: 92rpx;
}

.payment-note {
  margin-top: 18rpx;
  color: #8a7768;
  font-size: 23rpx;
  line-height: 1.45;
  text-align: center;
}
</style>
