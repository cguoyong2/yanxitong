<template>
  <view class="page" :class="activeTheme.tone">
    <view class="hero-card">
      <view class="hero-art">
        <text class="hero-symbol">{{ activeTheme.mark }}</text>
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
        <text class="card-desc">{{ pendingOrderTip }}</text>
      </view>
      <button v-if="features.mockPaymentEnabled" class="small-button primary" :loading="paying" @tap="mockPay(pendingOrder.orderNo)">模拟支付</button>
      <button v-else class="small-button primary" @tap="openPaymentPanel(pendingOrder)">去支付</button>
    </view>

    <button v-if="banquetId" class="return-button" @tap="returnBanquetDetail()">返回宴席管理台</button>

    <view v-if="planOrders.length" class="orders-card">
      <view class="section-head">
        <text class="section-title">版本订单</text>
        <text class="section-note">{{ planOrders.length }} 单</text>
      </view>
      <view v-for="order in planOrders" :key="order.orderNo" class="order-row" :class="{ highlight: isHighlightedOrder(order.orderNo) }">
        <view class="order-main">
          <text class="order-title">{{ order.orderNo }}</text>
          <text v-if="isHighlightedOrder(order.orderNo)" class="order-highlight">当前查看的订单</text>
          <text class="order-meta">{{ formatTime(order.createdAt) }} · {{ orderStatusLabel(order.payStatus) }}</text>
          <text class="order-next">{{ planOrderTip(order) }}</text>
        </view>
        <view class="order-side">
          <text class="order-price">{{ formatMoney(order.amount) }}</text>
          <text class="status-tag" :class="{ paid: order.payStatus === 'PAID' }">{{ orderStatusLabel(order.payStatus) }}</text>
          <button
            v-if="features.mockPaymentEnabled && order.payStatus !== 'PAID'"
            class="small-button pay"
            :loading="paying && pendingOrder?.orderNo === order.orderNo"
            @tap="mockPay(order.orderNo)"
          >
            模拟支付
          </button>
          <button
            v-else-if="order.payStatus === 'PAID' && hasDeviceRight"
            class="small-button pay"
            @tap="openDevice()"
          >
            去选设备
          </button>
          <button
            v-else-if="order.payStatus !== 'PAID'"
            class="small-button pay"
            @tap="openPaymentPanel(order)"
          >
            去支付
          </button>
        </view>
      </view>
    </view>
    <view v-else class="orders-card order-empty-card">
      <text class="empty-title">还没有版本订单</text>
      <text class="empty-desc">选择下方版本后，会在这里显示订单编号、金额、支付状态和后续处理入口。</text>
      <view class="empty-actions">
        <button class="small-button primary" @tap="scrollToPlans()">选择版本</button>
        <button v-if="banquetId" class="small-button ghost" @tap="returnBanquetDetail()">返回管理台</button>
      </view>
    </view>

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

    <view v-if="paymentPanel.visible" class="payment-mask" @tap="closePaymentPanel()">
      <view class="payment-sheet" @tap.stop>
        <view class="sheet-handle"></view>
        <view class="payment-head">
          <view>
            <text class="payment-label">版本支付</text>
            <text class="payment-title">{{ paymentPlanName }}</text>
          </view>
          <button class="close-button" @tap="closePaymentPanel()">×</button>
        </view>
        <view class="amount-card">
          <text class="amount-label">应付金额</text>
          <text class="amount-value">{{ formatMoney(paymentPanel.order?.amount) }}</text>
          <text class="amount-unit">/{{ paymentPanel.order?.priceUnit || '场' }}</text>
        </view>
        <view class="pay-info">
          <view>
            <text>订单编号</text>
            <text>{{ paymentPanel.order?.orderNo || '-' }}</text>
          </view>
          <view>
            <text>支付方式</text>
            <text>微信支付</text>
          </view>
          <view>
            <text>开通说明</text>
            <text>支付完成后自动开通版本权益</text>
          </view>
        </view>
        <button class="confirm-pay-button" @tap="showPaymentUnavailable()">确认支付</button>
        <text class="payment-note">当前仅展示支付确认流程，真实支付接口上线后将从这里完成付款。</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { loadRuntimeFeatures, request, type RuntimeFeatures } from '../../../api/client';
import { requireBanquetToast, resolveBanquetId } from '../../../utils/banquet';
import { eventThemeFor, fetchBanquetEventType, readActiveEventType, writeActiveEventType } from '../../../utils/event-theme';

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
  planId?: number;
  amount: number;
  priceUnit: string;
  payStatus: string;
  createdAt?: string;
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
const planOrders = ref<PlanOrder[]>([]);
const highlightOrderNo = ref('');
const features = ref<RuntimeFeatures>({ mockPaymentEnabled: false });
const eventType = ref(readActiveEventType());
const paymentPanel = reactive<{ visible: boolean; order?: PlanOrder; plan?: Plan }>({
  visible: false
});
const activeTheme = computed(() => eventThemeFor(eventType.value));
const entitlements = reactive<Entitlements>({
  rightValues: {},
  paidPlanActive: false,
  freeDefault: true
});
const hasDeviceRight = computed(() => Boolean(entitlements.rightValues.DEVICE_RENTAL));
const hasExportRight = computed(() => Boolean(entitlements.rightValues.EXCEL_EXPORT));
const localOrderKey = computed(() => banquetId.value ? `plan-order:${banquetId.value}` : '');
const pendingOrderTip = computed(() => {
  if (!pendingOrder.value) {
    return '';
  }
  return features.value.mockPaymentEnabled
    ? '体验环境可点“模拟支付”立即开通权益。'
    : '点击“去支付”可查看支付确认界面，真实支付暂未开通。';
});
const paymentPlanName = computed(() => paymentPanel.plan?.name || planByOrder(paymentPanel.order)?.name || '付费版本');

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
  planOrders.value = prioritizeHighlightedOrders(mergeOrders(orders, cachedOrders()));
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
      if (shouldShowPaymentPanel(plan)) {
        openPaymentPanel(order, plan);
      }
    }
    highlightOrderNo.value = order.orderNo;
    planOrders.value = prioritizeHighlightedOrders(mergeOrders([order], planOrders.value));
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
    await Promise.all([loadEntitlements(), loadOrders()]);
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

function mergeOrders(primary: PlanOrder[], fallback: PlanOrder[]) {
  const byOrderNo = new Map<string, PlanOrder>();
  for (const item of [...primary, ...fallback]) {
    byOrderNo.set(item.orderNo, item);
  }
  return Array.from(byOrderNo.values()).sort((a, b) => String(b.createdAt || b.orderNo).localeCompare(String(a.createdAt || a.orderNo)));
}

function prioritizeHighlightedOrders(orders: PlanOrder[]) {
  if (!highlightOrderNo.value) {
    return orders;
  }
  return [...orders].sort((a, b) => Number(isHighlightedOrder(b.orderNo)) - Number(isHighlightedOrder(a.orderNo)));
}

function isHighlightedOrder(orderNo: string) {
  return Boolean(highlightOrderNo.value && orderNo === highlightOrderNo.value);
}

function formatMoney(value: unknown) {
  return `¥${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })}`;
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '刚刚创建';
}

function orderStatusLabel(value: string) {
  const labels: Record<string, string> = {
    UNPAID: '待支付',
    PAID: '已支付',
    REFUNDED: '已退款'
  };
  return labels[value] || value;
}

function planOrderTip(order: PlanOrder) {
  if (order.payStatus === 'PAID') {
    if (hasDeviceRight.value) {
      return '已支付，版本权益已生效，可继续选择确认屏或云喇叭。';
    }
    return '已支付，当前版本权益已生效，可返回宴席管理台继续办席。';
  }
  if (features.value.mockPaymentEnabled) {
    return '待支付，体验环境可模拟支付完成开通。';
  }
  return '待支付，真实支付上线后会从这里继续完成付款。';
}

function shouldShowPaymentPanel(plan?: Plan) {
  if (!plan || Number(plan.price) <= 0) {
    return false;
  }
  return /PRO|PREMIUM|VIP|DIAMOND/i.test(plan.planCode) || /专业|至尊|尊享/.test(plan.name);
}

function planByOrder(order?: PlanOrder) {
  if (!order?.planId) {
    return undefined;
  }
  return plans.value.find((item) => item.id === order.planId);
}

function openPaymentPanel(order?: PlanOrder, plan?: Plan) {
  if (!order) {
    return;
  }
  paymentPanel.order = order;
  paymentPanel.plan = plan || planByOrder(order);
  paymentPanel.visible = true;
}

function closePaymentPanel() {
  paymentPanel.visible = false;
}

function showPaymentUnavailable() {
  uni.showToast({ title: '还没有开通支付功能', icon: 'none' });
}

function openDevice() {
  if (!banquetId.value) {
    requireBanquetToast();
    return;
  }
  uni.navigateTo({
    url: `/pages/device/select/index?banquetId=${banquetId.value}`,
    fail: () => uni.showToast({ title: '设备页面打开失败', icon: 'none' })
  });
}

function scrollToPlans() {
  uni.pageScrollTo({
    selector: '.plans-list',
    duration: 220,
    fail: () => uni.showToast({ title: '请选择下方版本', icon: 'none' })
  });
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
  if (/STANDARD|PRO/i.test(plan.planCode)) return `核心功能齐全，覆盖${activeTheme.value.giftRecordLabel}、设备和数据`;
  return '满足日常宴席管理需求';
}

function rightsPreview(plan: Plan) {
  if (Number(plan.price) === 0) return ['创建宴席', '基础请柬', '回执管理'];
  if (/PREMIUM|VIP|DIAMOND/i.test(plan.planCode)) return ['包含设备租赁', '数据导出权益', '高级配置预留'];
  if (/STANDARD|PRO/i.test(plan.planCode)) return [activeTheme.value.giftRecordLabel, '设备租赁', '人情账本'];
  return ['请柬管理', '回执统计', activeTheme.value.offlineGiftLabel];
}

function buttonText(plan: Plan) {
  if (isCurrent(plan)) return '当前版本';
  if (Number(plan.price) === 0) return '启用基础版';
  if (/PRO|PREMIUM|VIP|DIAMOND/i.test(plan.planCode) || /专业|至尊|尊享/.test(plan.name)) return '选择并支付';
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
  highlightOrderNo.value = current.options?.highlightOrderNo ? decodeURIComponent(current.options.highlightOrderNo) : '';
  if (!banquetId.value) {
    requireBanquetToast();
  } else {
    eventType.value = writeActiveEventType(await fetchBanquetEventType(banquetId.value, request, eventType.value));
  }
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
.orders-card,
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
  border-color: var(--accent-soft);
  background: var(--accent-soft);
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
  color: var(--accent);
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

.status-tag {
  padding: 7rpx 12rpx;
  border-radius: 999rpx;
  background: #fff7ed;
  color: #b45309;
  font-size: 21rpx;
  font-weight: 900;
  white-space: nowrap;
}

.status-tag.paid {
  background: #ecfdf3;
  color: #138a45;
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
  background: linear-gradient(135deg, var(--accent), var(--accent-dark));
  color: #fff;
}

.small-button.ghost {
  border: 1rpx solid #ead8ca;
  background: #fffaf6;
  color: var(--accent-dark);
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
  color: var(--accent);
  font-size: 27rpx;
  font-weight: 900;
  line-height: 76rpx;
}

.orders-card {
  padding: 26rpx;
}

.order-empty-card {
  display: grid;
  gap: 16rpx;
  background: linear-gradient(90deg, #fff, var(--accent-soft));
}

.empty-title,
.empty-desc {
  display: block;
}

.empty-title {
  color: #171c2a;
  font-size: 30rpx;
  font-weight: 900;
}

.empty-desc {
  color: #8a7768;
  font-size: 25rpx;
  line-height: 1.5;
}

.empty-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
  margin-top: 6rpx;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  margin-bottom: 12rpx;
}

.section-note {
  color: #8a7768;
  font-size: 24rpx;
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

.order-row:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.order-main {
  min-width: 0;
}

.order-title,
.order-meta,
.order-price {
  display: block;
}

.order-title {
  overflow: hidden;
  color: #171c2a;
  font-size: 27rpx;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-meta {
  margin-top: 8rpx;
  color: #8a7768;
  font-size: 23rpx;
}

.order-next {
  display: block;
  max-width: 400rpx;
  margin-top: 8rpx;
  color: #6f6259;
  font-size: 22rpx;
  line-height: 1.4;
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
  flex: 0 0 auto;
  justify-items: end;
  gap: 10rpx;
}

.order-price {
  color: var(--accent);
  font-size: 29rpx;
  font-weight: 900;
}

.small-button.pay {
  height: 54rpx;
  padding: 0 18rpx;
  border: 1rpx solid var(--accent-soft);
  border-radius: 999rpx;
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 22rpx;
  line-height: 54rpx;
}

.wait-pay {
  color: #b45309;
  font-size: 22rpx;
  font-weight: 900;
}

.plans-list {
  margin-top: 24rpx;
}

.plan-card {
  padding: 26rpx;
}

.plan-card.recommended {
  border-color: var(--accent);
}

.plan-card.current {
  background: linear-gradient(180deg, var(--accent-soft), #fff);
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
  background: var(--accent);
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
  color: var(--accent);
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
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 22rpx;
  font-weight: 800;
}

.open-button {
  height: 86rpx;
  margin: 24rpx 0 0;
  border-radius: 18rpx;
  background: linear-gradient(135deg, var(--accent), var(--accent-dark));
  color: #fff;
  font-size: 29rpx;
  font-weight: 900;
  line-height: 86rpx;
}

.open-button.ghost {
  border: 1rpx solid #ead8ca;
  background: var(--accent-soft);
  color: var(--accent);
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

.close-button::after,
.confirm-pay-button::after {
  border: 0;
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
