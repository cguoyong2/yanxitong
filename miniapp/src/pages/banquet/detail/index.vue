<template>
  <view class="page" v-if="detail">
    <view class="hero-card">
      <view class="hero-art">
        <text class="hero-knot">囍</text>
      </view>
      <view class="hero-top">
        <view>
          <text class="hero-label">宴席管理台</text>
          <text class="hero-title">{{ detail.banquet.name }}</text>
        </view>
        <text class="status">{{ statusLabel }}</text>
      </view>
      <view class="hero-meta">
        <text>{{ eventTypeLabel(detail.banquet.eventTypeCode) }}</text>
        <text>{{ formatTime(detail.banquet.banquetTime) }}</text>
        <text>{{ detail.banquet.location || '地点待定' }}</text>
      </view>
    </view>

    <view class="summary-card">
      <view class="summary-item">
        <text class="summary-value">86</text>
        <text class="summary-label">已回执</text>
      </view>
      <view class="summary-line"></view>
      <view class="summary-item">
        <text class="summary-value red">¥12,800</text>
        <text class="summary-label">已收礼</text>
      </view>
      <view class="summary-line"></view>
      <view class="summary-item">
        <text class="summary-value">{{ entitlements.currentPlan?.name || '基础版' }}</text>
        <text class="summary-label">当前版本</text>
      </view>
    </view>

    <view class="section-card">
      <view class="section-head">
        <text class="section-title">核心操作</text>
        <text class="section-more">按流程推进</text>
      </view>
      <view class="action-grid">
        <view v-for="item in actionItems" :key="item.title" class="action-item" @tap="handleAction(item.action)">
          <text class="action-icon" :class="item.tone">{{ item.icon }}</text>
          <text class="action-title">{{ item.title }}</text>
          <text class="action-desc">{{ item.desc }}</text>
        </view>
      </view>
    </view>

    <view class="invite-card">
      <view class="invite-main">
        <image class="invite-cover" src="/static/home/banquet_cover.png" mode="aspectFill" />
        <view class="invite-info">
          <text class="section-title">基础请柬</text>
          <text class="invite-desc">分享码：{{ detail.invitation?.shareSlug || '-' }}</text>
          <text class="invite-desc">{{ invitationShareUrl }}</text>
        </view>
      </view>
      <view class="invite-actions">
        <button @tap="openInvite()">查看公开页</button>
        <button @tap="copyInviteLink()">复制路径</button>
        <button @tap="editInvite()">编辑字段</button>
      </view>
    </view>

    <view class="section-card">
      <view class="section-head">
        <text class="section-title">版本与设备</text>
        <text class="section-more">权益校验</text>
      </view>
      <view class="rights-list">
        <view class="right-row">
          <text class="right-name">当前版本</text>
          <text class="right-value">{{ entitlements.currentPlan?.name || '基础版' }}</text>
        </view>
        <view class="right-row">
          <text class="right-name">设备租赁</text>
          <text class="right-value" :class="{ ok: hasDeviceRight }">{{ hasDeviceRight ? '已开通' : '未开通' }}</text>
        </view>
        <view class="right-row">
          <text class="right-name">Excel 导出</text>
          <text class="right-value" :class="{ ok: hasExportRight }">{{ hasExportRight ? '已包含' : '未包含' }}</text>
        </view>
      </view>
      <view class="dual-buttons">
        <button @tap="openPlan()">选择版本</button>
        <button @tap="openDevice()">设备选择</button>
      </view>
    </view>

    <view class="copy-card">
      <text class="section-title">收礼文案</text>
      <text class="copy-text">{{ detail.giftSuccessCopywriting.content }}</text>
    </view>
  </view>
  <view class="loading" v-else-if="pageState === 'loading'">加载中</view>
  <view class="state-page" v-else>
    <text class="state-title">宴席加载失败</text>
    <text class="state-desc">宴席可能已删除，或当前网络不可用。</text>
    <button class="state-button" @tap="bootstrap">重新加载</button>
    <button class="state-link" @tap="goHome">返回首页</button>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { loadRuntimeFeatures, request, type RuntimeFeatures } from '../../../api/client';
import { requireBanquetToast, resolveBanquetId } from '../../../utils/banquet';

interface BanquetDetail {
  banquet: {
    id: number;
    name: string;
    eventTypeCode: string;
    themeCode: string;
    banquetTime?: string;
    location?: string;
    status?: string;
  };
  invitation?: {
    id: number;
    title: string;
    shareSlug: string;
  };
  giftSuccessCopywriting: {
    content: string;
  };
}

interface Entitlements {
  currentPlan?: {
    name: string;
  };
  rightValues: Record<string, string>;
}

const detail = ref<BanquetDetail>();
const pageState = ref<'loading' | 'ready' | 'error'>('loading');
const features = ref<RuntimeFeatures>({ mockPaymentEnabled: false });
const entitlements = reactive<Entitlements>({
  rightValues: {}
});
const actionItems = [
  { title: '发请柬', desc: '公开页与分享', icon: '✉', tone: 'red', action: 'invite' },
  { title: '回执统计', desc: '宾客与人数', icon: '◔', tone: 'orange', action: 'rsvp' },
  { title: '线下记礼', desc: '现场收礼登记', icon: '▣', tone: 'orange', action: 'offlineGift' },
  { title: '收礼记录', desc: '礼金明细', icon: '▤', tone: 'red', action: 'giftList' },
  { title: '人情账本', desc: '自动沉淀往来', icon: '账', tone: 'green', action: 'favor' },
  { title: '线上随礼', desc: paymentTip(), icon: '¥', tone: 'purple', action: 'onlineGift' }
];
const hasDeviceRight = computed(() => Boolean(entitlements.rightValues.DEVICE_RENTAL));
const hasExportRight = computed(() => Boolean(entitlements.rightValues.EXCEL_EXPORT));
const paymentEntryEnabled = computed(() => features.value.mockPaymentEnabled);
const statusLabel = computed(() => {
  const status = detail.value?.banquet.status;
  if (status === 'PUBLISHED') return '已发布';
  if (status === 'DRAFT') return '草稿';
  return status || '已创建';
});
const invitationShareUrl = computed(() => {
  const slug = detail.value?.invitation?.shareSlug;
  return slug ? `/pages/invite/public/index?slug=${slug}` : '-';
});

async function load(id: string) {
  pageState.value = 'loading';
  const [runtimeFeatures, banquetDetail, result] = await Promise.all([
    loadRuntimeFeatures().catch(() => ({ mockPaymentEnabled: false })),
    request<BanquetDetail>(`/banquets/${id}`),
    request<Entitlements>(`/plans/banquets/${id}/entitlements`)
  ]);
  features.value = runtimeFeatures;
  detail.value = banquetDetail;
  entitlements.currentPlan = result.currentPlan;
  entitlements.rightValues = result.rightValues || {};
  pageState.value = 'ready';
}

function paymentTip() {
  return '支付入口';
}

function eventTypeLabel(code: string) {
  const labels: Record<string, string> = {
    WEDDING: '婚宴',
    BIRTHDAY: '寿宴',
    BABY: '满月',
    HOUSEWARMING: '乔迁',
    SCHOOL: '升学',
    MEMORIAL: '追思会',
    OTHER: '其他'
  };
  return labels[code] || code || '宴席';
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '时间待定';
}

function handleAction(action: string) {
  if (action === 'invite') {
    openInvite();
    return;
  }
  if (action === 'rsvp') {
    openRsvpStats();
    return;
  }
  if (action === 'offlineGift') {
    openOfflineGift();
    return;
  }
  if (action === 'giftList') {
    openGiftList();
    return;
  }
  if (action === 'favor') {
    openFavor();
    return;
  }
  if (action === 'onlineGift') {
    if (!paymentEntryEnabled.value) {
      uni.showToast({ title: '线上随礼暂未开放', icon: 'none' });
      return;
    }
    openGiftPay('ONLINE_GIFT');
  }
}

function openInvite() {
  const slug = detail.value?.invitation?.shareSlug;
  if (slug) {
    uni.navigateTo({ url: `/pages/invite/public/index?slug=${slug}` });
  }
}

function editInvite() {
  const invitation = detail.value?.invitation;
  if (invitation) {
    uni.navigateTo({
      url: `/pages/invite/edit-basic/index?invitationId=${invitation.id}`
    });
  }
}

function copyInviteLink() {
  if (invitationShareUrl.value === '-') {
    return;
  }
  uni.setClipboardData({
    data: invitationShareUrl.value,
    success: () => uni.showToast({ title: '已复制', icon: 'success' })
  });
}

function openPlan() {
  if (detail.value?.banquet.id) {
    uni.navigateTo({ url: `/pages/order/plan/index?banquetId=${detail.value.banquet.id}` });
  }
}

function openDevice() {
  if (detail.value?.banquet.id) {
    uni.navigateTo({ url: `/pages/device/select/index?banquetId=${detail.value.banquet.id}` });
  }
}

function openRsvpStats() {
  if (detail.value?.banquet.id) {
    uni.navigateTo({ url: `/pages/rsvp/stats/index?banquetId=${detail.value.banquet.id}` });
  }
}

function openGiftPay(entrySource: string) {
  if (detail.value?.banquet.id) {
    uni.navigateTo({ url: `/pages/gift/pay/index?banquetId=${detail.value.banquet.id}&entrySource=${entrySource}` });
  }
}

function openOfflineGift() {
  if (detail.value?.banquet.id) {
    uni.navigateTo({ url: `/pages/gift/offline/index?banquetId=${detail.value.banquet.id}` });
  }
}

function openGiftList() {
  if (detail.value?.banquet.id) {
    uni.navigateTo({ url: `/pages/gift/list/index?banquetId=${detail.value.banquet.id}` });
  }
}

function openFavor() {
  uni.switchTab({ url: '/pages/favor/index/index' });
}

async function bootstrap() {
  const pages = getCurrentPages();
  const current = pages[pages.length - 1] as unknown as { options?: Record<string, string> };
  const id = await resolveBanquetId(current.options?.id);
  if (id) {
    try {
      await load(id);
    } catch {
      pageState.value = 'error';
    }
    return;
  }
  requireBanquetToast();
  pageState.value = 'error';
}

function goHome() {
  uni.switchTab({ url: '/pages/home/index/index' });
}

onMounted(bootstrap);
</script>

<style scoped>
.page {
  box-sizing: border-box;
  min-height: 100vh;
  padding: 24rpx 24rpx 44rpx;
  background: #fff8ef;
  color: #151823;
}

.loading {
  min-height: 100vh;
  padding: 60rpx 24rpx;
  background: #fff8ef;
  color: #7a7f8c;
  text-align: center;
}

.state-page {
  box-sizing: border-box;
  min-height: 100vh;
  padding: 120rpx 48rpx;
  background: #fff8ef;
  color: #171c2a;
  text-align: center;
}

.state-title,
.state-desc {
  display: block;
}

.state-title {
  font-size: 40rpx;
  font-weight: 900;
}

.state-desc {
  margin-top: 18rpx;
  color: #8a7768;
  font-size: 27rpx;
  line-height: 1.6;
}

.state-button,
.state-link {
  margin-top: 34rpx;
}

.state-button {
  height: 88rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #e71921, #c7191e);
  color: #fff8df;
  font-size: 30rpx;
  font-weight: 900;
}

.state-link {
  background: transparent;
  color: #9c4b31;
  font-size: 27rpx;
}

.hero-card {
  position: relative;
  overflow: hidden;
  padding: 30rpx;
  border-radius: 24rpx;
  background:
    radial-gradient(circle at 82% 18%, rgba(255, 232, 190, 0.35), transparent 28%),
    linear-gradient(135deg, #e60012, #b80000);
  color: #fff;
  box-shadow: 0 16rpx 34rpx rgba(184, 0, 0, 0.18);
}

.hero-art {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.hero-knot {
  position: absolute;
  right: 36rpx;
  bottom: -8rpx;
  color: rgba(255, 232, 190, 0.15);
  font-size: 154rpx;
  font-weight: 900;
}

.hero-top,
.section-head,
.invite-main,
.dual-buttons,
.right-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
}

.hero-label,
.hero-title,
.hero-meta,
.summary-value,
.summary-label,
.section-title,
.section-more,
.action-title,
.action-desc,
.invite-desc,
.right-name,
.right-value,
.copy-text {
  display: block;
}

.hero-label {
  color: #ffe8bf;
  font-size: 24rpx;
  font-weight: 800;
}

.hero-title {
  max-width: 520rpx;
  margin-top: 14rpx;
  overflow: hidden;
  color: #fff7df;
  font-size: 40rpx;
  font-weight: 900;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status {
  flex: 0 0 auto;
  padding: 8rpx 18rpx;
  border: 1rpx solid rgba(255, 232, 190, 0.62);
  border-radius: 999rpx;
  color: #fff7df;
  font-size: 23rpx;
  font-weight: 800;
}

.hero-meta {
  display: flex;
  gap: 12rpx;
  flex-wrap: wrap;
  margin-top: 26rpx;
}

.hero-meta text {
  padding: 7rpx 14rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.16);
  color: rgba(255, 255, 255, 0.92);
  font-size: 22rpx;
}

.summary-card,
.section-card,
.invite-card,
.copy-card {
  margin-top: 22rpx;
  padding: 24rpx;
  border-radius: 24rpx;
  background: #fff;
  box-shadow: 0 10rpx 30rpx rgba(43, 35, 31, 0.06);
}

.summary-card {
  display: grid;
  grid-template-columns: 1fr 1rpx 1fr 1rpx 1fr;
  align-items: center;
}

.summary-item {
  text-align: center;
}

.summary-value {
  overflow: hidden;
  color: #171923;
  font-size: 30rpx;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary-value.red {
  color: #e60012;
}

.summary-label {
  margin-top: 8rpx;
  color: #6f7480;
  font-size: 22rpx;
}

.summary-line {
  width: 1rpx;
  height: 58rpx;
  background: #efe4dd;
}

.section-title {
  color: #171923;
  font-size: 30rpx;
  font-weight: 900;
}

.section-more {
  color: #7a7f8c;
  font-size: 22rpx;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16rpx;
  margin-top: 22rpx;
}

.action-item {
  min-height: 144rpx;
  padding: 18rpx 10rpx;
  border: 1rpx solid #f0e2dc;
  border-radius: 16rpx;
  background: #fffaf6;
  text-align: center;
}

.action-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 54rpx;
  height: 54rpx;
  margin: 0 auto;
  border-radius: 14rpx;
  color: #fff;
  font-size: 28rpx;
  font-weight: 900;
}

.action-icon.red {
  background: linear-gradient(135deg, #ff6a5f, #e60012);
}

.action-icon.orange {
  background: linear-gradient(135deg, #ffbb58, #ff7a00);
}

.action-icon.green {
  background: linear-gradient(135deg, #74d88e, #36b96a);
}

.action-icon.purple {
  background: linear-gradient(135deg, #a890ff, #7b61ff);
}

.action-title {
  margin-top: 12rpx;
  color: #171923;
  font-size: 24rpx;
  font-weight: 900;
}

.action-desc {
  margin-top: 6rpx;
  color: #7a7f8c;
  font-size: 20rpx;
}

.invite-main {
  justify-content: flex-start;
}

.invite-cover {
  width: 150rpx;
  height: 118rpx;
  border-radius: 14rpx;
}

.invite-info {
  flex: 1;
  min-width: 0;
}

.invite-desc {
  overflow: hidden;
  margin-top: 8rpx;
  color: #6f7480;
  font-size: 22rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.invite-actions,
.dual-buttons {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14rpx;
  margin-top: 22rpx;
}

button {
  margin: 0;
  padding: 0;
  border: 0;
}

button::after {
  border: 0;
}

.invite-actions button,
.dual-buttons button {
  height: 64rpx;
  border: 1rpx solid #ffd6ca;
  border-radius: 999rpx;
  background: #fff6f2;
  color: #e60012;
  font-size: 23rpx;
  font-weight: 800;
  line-height: 64rpx;
}

.dual-buttons {
  grid-template-columns: repeat(2, 1fr);
}

.rights-list {
  margin-top: 18rpx;
  border-top: 1rpx solid #f0e6e0;
}

.right-row {
  min-height: 72rpx;
  border-bottom: 1rpx solid #f0e6e0;
}

.right-name {
  color: #4b5563;
  font-size: 24rpx;
}

.right-value {
  color: #7a7f8c;
  font-size: 24rpx;
  font-weight: 800;
}

.right-value.ok {
  color: #36b96a;
}

.copy-text {
  margin-top: 14rpx;
  padding: 18rpx;
  border-radius: 16rpx;
  background: #fff8ef;
  color: #8a4d20;
  font-size: 24rpx;
  line-height: 1.6;
}
</style>
