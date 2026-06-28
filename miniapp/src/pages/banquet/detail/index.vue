<template>
  <view class="page" v-if="detail" :class="detailDesign.tone">
    <view class="red-stage">
      <view class="hero-art">
        <text class="firework">✦</text>
        <text class="hero-knot">{{ detailDesign.mark }}</text>
      </view>
      <view class="hero-top">
        <view>
          <text class="hero-label">宴席通</text>
          <text class="hero-title">{{ detail.banquet.name }}</text>
        </view>
        <text class="status">{{ statusLabel }}</text>
      </view>
      <view class="hero-meta">
        <text>{{ eventTypeLabel(detail.banquet.eventTypeCode) }}</text>
        <text>{{ formatTime(detail.banquet.banquetTime) }}</text>
      </view>
      <text class="hero-location">{{ detail.banquet.location || '地点待定' }}</text>
    </view>

    <view class="content">
      <view class="overview-card">
        <image class="overview-cover" src="/static/home/banquet_cover.png" mode="aspectFill" />
        <view class="overview-body">
          <view class="overview-title-row">
            <text class="overview-title">{{ detail.banquet.name }}</text>
            <text class="overview-tag">{{ statusLabel }}</text>
          </view>
          <view class="overview-meta">
            <text>◷ {{ formatTime(detail.banquet.banquetTime) }}</text>
            <text>⌖ {{ detail.banquet.location || '地点待定' }}</text>
          </view>
        </view>
      </view>

      <view class="summary-card">
        <view class="summary-item">
          <text class="summary-value">{{ rsvpStats?.totalGuests || 0 }}</text>
          <text class="summary-label">已回执</text>
        </view>
        <view class="summary-line"></view>
        <view class="summary-item">
          <text class="summary-value red">{{ formatMoney(giftSummary?.totalAmount || 0) }}</text>
          <text class="summary-label">已{{ activeTheme.giftLabel }}</text>
        </view>
        <view class="summary-line"></view>
        <view class="summary-item">
          <text class="summary-value">{{ entitlements.currentPlan?.name || '基础版' }}</text>
          <text class="summary-label">当前版本</text>
        </view>
      </view>

      <view class="section-card">
        <view class="section-head">
          <text class="section-title">宴席操作</text>
          <text class="section-more">按办席流程推进</text>
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
        <view class="section-head">
          <text class="section-title">基础请柬</text>
          <text class="section-more">分享访问</text>
        </view>
        <view class="invite-main">
          <view class="invite-seal">请</view>
          <view class="invite-info">
            <text class="invite-title">{{ detail.invitation?.title || '宴席请柬' }}</text>
            <text class="invite-desc">分享码：{{ detail.invitation?.shareSlug || '-' }}</text>
            <text class="invite-path">{{ invitationShareUrl }}</text>
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
        <text class="section-title">{{ activeTheme.detailGiftCopyTitle }}</text>
        <text class="copy-text">{{ detail.giftSuccessCopywriting.content }}</text>
      </view>
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
import { requireBanquetToast, resolveBanquetId, writeLastBanquetContext } from '../../../utils/banquet';
import { eventThemeFor, eventToneClass, type EventTheme, writeActiveEventType } from '../../../utils/event-theme';

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

interface RsvpStats {
  totalGuests: number;
}

interface GiftSummary {
  totalAmount: number;
}

const detail = ref<BanquetDetail>();
const pageState = ref<'loading' | 'ready' | 'error'>('loading');
const features = ref<RuntimeFeatures>({ mockPaymentEnabled: false });
const rsvpStats = ref<RsvpStats>();
const giftSummary = ref<GiftSummary>();
const entitlements = reactive<Entitlements>({
  rightValues: {}
});
const activeTheme = computed<EventTheme>(() => eventThemeFor(detail.value?.banquet.eventTypeCode || 'WEDDING'));
const actionItems = computed(() => [
  { title: '发请柬', desc: '公开页与分享', icon: '✉', tone: 'red', action: 'invite' },
  { title: '回执统计', desc: '宾客与人数', icon: '◔', tone: 'orange', action: 'rsvp' },
  { title: activeTheme.value.offlineGiftLabel, desc: `${activeTheme.value.giftLabel}现场登记`, icon: '▣', tone: 'orange', action: 'offlineGift' },
  { title: activeTheme.value.giftRecordLabel, desc: `${activeTheme.value.giftLabel}明细`, icon: '▤', tone: 'red', action: 'giftList' },
  { title: '人情账本', desc: '自动沉淀往来', icon: '账', tone: 'green', action: 'favor' },
  { title: activeTheme.value.onlineGiftLabel, desc: paymentTip(activeTheme.value), icon: '¥', tone: 'purple', action: 'onlineGift' }
]);
const hasDeviceRight = computed(() => Boolean(entitlements.rightValues.DEVICE_RENTAL));
const hasExportRight = computed(() => Boolean(entitlements.rightValues.EXCEL_EXPORT));
const paymentEntryEnabled = computed(() => features.value.mockPaymentEnabled);
const detailDesign = computed(() => ({
  mark: activeTheme.value.mark,
  tone: eventToneClass(activeTheme.value.code)
}));
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
  const [runtimeFeatures, banquetDetail, result, rsvp, gifts] = await Promise.all([
    loadRuntimeFeatures().catch(() => ({ mockPaymentEnabled: false })),
    request<BanquetDetail>(`/banquets/${id}`),
    request<Entitlements>(`/plans/banquets/${id}/entitlements`),
    request<RsvpStats>(`/rsvp/stats?banquetId=${id}`).catch(() => ({ totalGuests: 0 })),
    request<GiftSummary>(`/gifts/summary?banquetId=${id}`).catch(() => ({ totalAmount: 0 }))
  ]);
  features.value = runtimeFeatures;
  detail.value = banquetDetail;
  writeLastBanquetContext({
    id: banquetDetail.banquet.id,
    name: banquetDetail.banquet.name,
    eventTypeCode: banquetDetail.banquet.eventTypeCode,
    themeCode: banquetDetail.banquet.themeCode,
    banquetTime: banquetDetail.banquet.banquetTime,
    location: banquetDetail.banquet.location,
    invitationId: banquetDetail.invitation?.id,
    shareSlug: banquetDetail.invitation?.shareSlug
  });
  writeActiveEventType(banquetDetail.banquet.eventTypeCode);
  entitlements.currentPlan = result.currentPlan;
  entitlements.rightValues = result.rightValues || {};
  rsvpStats.value = rsvp;
  giftSummary.value = gifts;
  pageState.value = 'ready';
}

function paymentTip(theme = activeTheme.value) {
  return `${theme.giftLabel}入口`;
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

function formatMoney(value: unknown) {
  return `¥${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })}`;
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
      uni.showToast({ title: `${activeTheme.value.onlineGiftLabel}暂未开放`, icon: 'none' });
      return;
    }
    openGiftPay('ONLINE_GIFT');
  }
}

function openInvite() {
  const slug = detail.value?.invitation?.shareSlug;
  if (slug) {
    uni.navigateTo({
      url: `/pages/invite/public/index?slug=${slug}`,
      fail: () => uni.showToast({ title: '请柬公开页打开失败', icon: 'none' })
    });
    return;
  }
  uni.showToast({ title: '暂无请柬分享链接', icon: 'none' });
}

function editInvite() {
  const invitation = detail.value?.invitation;
  if (invitation) {
    uni.navigateTo({
      url: `/pages/invite/edit-basic/index?invitationId=${invitation.id}`,
      fail: () => uni.showToast({ title: '请柬编辑页打开失败', icon: 'none' })
    });
    return;
  }
  uni.showToast({ title: '暂无可编辑请柬', icon: 'none' });
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
    uni.navigateTo({
      url: `/pages/rsvp/stats/index?banquetId=${detail.value.banquet.id}`,
      fail: () => uni.showToast({ title: '回执统计打开失败', icon: 'none' })
    });
  }
}

function openGiftPay(entrySource: string) {
  if (detail.value?.banquet.id) {
    uni.navigateTo({ url: `/pages/gift/pay/index?banquetId=${detail.value.banquet.id}&entrySource=${entrySource}` });
  }
}

function openOfflineGift() {
  if (detail.value?.banquet.id) {
    uni.navigateTo({
      url: `/pages/gift/offline/index?banquetId=${detail.value.banquet.id}`,
      fail: () => uni.showToast({ title: `${activeTheme.value.offlineGiftLabel}打开失败`, icon: 'none' })
    });
  }
}

function openGiftList() {
  if (detail.value?.banquet.id) {
    uni.navigateTo({
      url: `/pages/gift/list/index?banquetId=${detail.value.banquet.id}`,
      fail: () => uni.showToast({ title: `${activeTheme.value.giftRecordLabel}打开失败`, icon: 'none' })
    });
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
  background: #f7f3ee;
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

.red-stage {
  position: relative;
  overflow: hidden;
  min-height: 320rpx;
  padding: 66rpx 40rpx 112rpx;
  background:
    radial-gradient(circle at 76% 12%, rgba(255, 164, 91, 0.42), transparent 30%),
    radial-gradient(circle at 100% 100%, rgba(255, 219, 168, 0.26), transparent 26%),
    linear-gradient(142deg, #e40012 0%, #cf0710 48%, #a80008 100%);
  color: #fff;
}

.tone-birthday .red-stage {
  background:
    radial-gradient(circle at 76% 12%, rgba(255, 218, 138, 0.3), transparent 30%),
    radial-gradient(circle at 100% 100%, rgba(255, 228, 176, 0.2), transparent 26%),
    linear-gradient(142deg, #c15b10 0%, #9d4308 48%, #743005 100%);
}

.tone-baby .red-stage {
  background:
    radial-gradient(circle at 76% 12%, rgba(255, 198, 212, 0.3), transparent 30%),
    radial-gradient(circle at 100% 100%, rgba(255, 224, 232, 0.18), transparent 26%),
    linear-gradient(142deg, #e7566f 0%, #c73655 48%, #932742 100%);
}

.tone-house .red-stage {
  background:
    radial-gradient(circle at 76% 12%, rgba(185, 245, 202, 0.22), transparent 30%),
    radial-gradient(circle at 100% 100%, rgba(210, 245, 220, 0.16), transparent 26%),
    linear-gradient(142deg, #1b8a58 0%, #116943 48%, #0b4b31 100%);
}

.tone-school .red-stage {
  background:
    radial-gradient(circle at 76% 12%, rgba(186, 220, 255, 0.24), transparent 30%),
    radial-gradient(circle at 100% 100%, rgba(215, 232, 255, 0.16), transparent 26%),
    linear-gradient(142deg, #2563eb 0%, #1d4ed8 48%, #1e3a8a 100%);
}

.tone-memorial .red-stage {
  background:
    radial-gradient(circle at 76% 12%, rgba(255, 255, 255, 0.08), transparent 30%),
    radial-gradient(circle at 100% 100%, rgba(255, 255, 255, 0.06), transparent 26%),
    linear-gradient(142deg, #202124 0%, #111315 48%, #050607 100%);
}

.tone-other .red-stage {
  background:
    radial-gradient(circle at 76% 12%, rgba(218, 200, 255, 0.22), transparent 30%),
    radial-gradient(circle at 100% 100%, rgba(235, 224, 255, 0.12), transparent 26%),
    linear-gradient(142deg, #7c3aed 0%, #5b21b6 48%, #3b0764 100%);
}

.hero-art {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.firework {
  position: absolute;
  top: 46rpx;
  right: 154rpx;
  color: rgba(255, 235, 198, 0.52);
  font-size: 76rpx;
  font-weight: 900;
}

.hero-knot {
  position: absolute;
  right: 58rpx;
  bottom: 26rpx;
  color: rgba(255, 229, 195, 0.16);
  font-size: 190rpx;
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
  font-size: 28rpx;
  font-weight: 800;
}

.hero-title {
  max-width: 510rpx;
  margin-top: 20rpx;
  overflow: hidden;
  color: #fff7df;
  font-size: 50rpx;
  font-weight: 900;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status {
  flex: 0 0 auto;
  padding: 10rpx 20rpx;
  border: 1rpx solid rgba(255, 239, 208, 0.68);
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.14);
  color: #fff4d6;
  font-size: 23rpx;
  font-weight: 800;
}

.hero-meta {
  display: flex;
  gap: 12rpx;
  flex-wrap: wrap;
  margin-top: 28rpx;
}

.hero-meta text {
  padding: 9rpx 18rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.16);
  color: rgba(255, 255, 255, 0.92);
  font-size: 24rpx;
  font-weight: 700;
}

.hero-location {
  display: block;
  max-width: 560rpx;
  margin-top: 20rpx;
  overflow: hidden;
  color: rgba(255, 247, 223, 0.9);
  font-size: 26rpx;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.content {
  position: relative;
  z-index: 2;
  margin-top: -74rpx;
  padding: 0 24rpx 44rpx;
}

.overview-card {
  display: flex;
  gap: 22rpx;
  align-items: center;
  min-height: 168rpx;
  padding: 22rpx;
  border: 1rpx solid #f3ded2;
  border-radius: 28rpx;
  background: linear-gradient(180deg, #fffaf4, #fff);
  box-shadow: 0 18rpx 42rpx rgba(89, 37, 28, 0.1);
}

.overview-cover {
  flex: 0 0 auto;
  width: 190rpx;
  height: 132rpx;
  border-radius: 20rpx;
}

.overview-body {
  flex: 1;
  min-width: 0;
}

.overview-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14rpx;
}

.overview-title,
.overview-tag,
.overview-meta text,
.invite-title,
.invite-path {
  display: block;
}

.overview-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  color: #171923;
  font-size: 32rpx;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.overview-tag {
  flex: 0 0 auto;
  padding: 7rpx 14rpx;
  border-radius: 999rpx;
  background: #fff0e9;
  color: #d61d24;
  font-size: 22rpx;
  font-weight: 900;
}

.overview-meta {
  margin-top: 18rpx;
}

.overview-meta text {
  overflow: hidden;
  margin-top: 9rpx;
  color: #7a7f8c;
  font-size: 24rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary-card,
.section-card,
.invite-card,
.copy-card {
  margin-top: 22rpx;
  padding: 24rpx;
  border: 1rpx solid #f3e5dc;
  border-radius: 24rpx;
  background: #fff;
  box-shadow: 0 12rpx 32rpx rgba(43, 35, 31, 0.06);
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
  font-size: 32rpx;
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
  font-size: 23rpx;
}

.summary-line {
  width: 1rpx;
  height: 58rpx;
  background: #efe4dd;
}

.section-title {
  color: #171923;
  font-size: 32rpx;
  font-weight: 900;
}

.section-more {
  color: #7a7f8c;
  font-size: 22rpx;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16rpx;
  margin-top: 22rpx;
}

.action-item {
  display: grid;
  grid-template-columns: 60rpx 1fr;
  column-gap: 16rpx;
  align-items: center;
  min-height: 104rpx;
  padding: 18rpx;
  border: 1rpx solid #f1dfd5;
  border-radius: 18rpx;
  background: linear-gradient(180deg, #fffaf6, #fff);
}

.action-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 54rpx;
  height: 54rpx;
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
  grid-column: 2;
  grid-row: 1;
  color: #171923;
  font-size: 26rpx;
  font-weight: 900;
}

.action-desc {
  grid-column: 2;
  grid-row: 1;
  align-self: end;
  margin-top: 38rpx;
  color: #7a7f8c;
  font-size: 21rpx;
}

.invite-main {
  justify-content: flex-start;
  margin-top: 22rpx;
}

.invite-seal {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 104rpx;
  height: 104rpx;
  border: 1rpx solid #f5cfb5;
  border-radius: 24rpx;
  background: linear-gradient(135deg, #fff3dc, #d82222);
  color: #fff1c8;
  font-size: 42rpx;
  font-weight: 900;
  box-shadow: inset 0 1rpx 0 rgba(255, 255, 255, 0.45);
}

.invite-info {
  flex: 1;
  min-width: 0;
}

.invite-title {
  overflow: hidden;
  color: #171923;
  font-size: 28rpx;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.invite-desc {
  overflow: hidden;
  margin-top: 8rpx;
  color: #6f7480;
  font-size: 22rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.invite-path {
  overflow: hidden;
  margin-top: 6rpx;
  color: #a9755d;
  font-size: 21rpx;
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
  height: 68rpx;
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
  padding: 20rpx;
  border-radius: 16rpx;
  background: linear-gradient(180deg, #fff8ef, #fff);
  color: #8a4d20;
  font-size: 24rpx;
  line-height: 1.6;
}
</style>
