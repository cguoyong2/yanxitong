<template>
  <view class="page" :class="activeTheme.tone">
    <view class="hero-card">
      <view class="hero-art">
        <text class="hero-knot">{{ activeTheme.mark }}</text>
      </view>
      <text class="hero-label">宴席通</text>
      <text class="hero-title">回执统计</text>
      <text class="hero-desc">{{ activeTheme.rsvpSubtitle }}，出席、用餐、住宿数据实时汇总</text>
      <view class="hero-main">
        <view>
          <text class="hero-number">{{ stats?.totalRecords || 0 }}</text>
          <text class="hero-number-label">已回执</text>
        </view>
        <view class="rate-ring">
          <text class="rate-value">{{ attendingRate }}%</text>
          <text class="rate-label">出席率</text>
        </view>
      </view>
    </view>

    <view class="summary-grid">
      <view class="summary-item">
        <text class="summary-icon red">人</text>
        <text class="summary-value">{{ stats?.totalGuests || 0 }}</text>
        <text class="summary-label">参加人数</text>
      </view>
      <view class="summary-item">
        <text class="summary-icon orange">餐</text>
        <text class="summary-value">{{ stats?.mealRequiredGuests || 0 }}</text>
        <text class="summary-label">用餐人数</text>
      </view>
      <view class="summary-item">
        <text class="summary-icon gold">宿</text>
        <text class="summary-value">{{ stats?.accommodationRequiredGuests || 0 }}</text>
        <text class="summary-label">住宿人数</text>
      </view>
    </view>

    <view class="section-card">
      <view class="section-head">
        <text class="section-title">状态分布</text>
        <text v-if="lastSyncText" class="section-note">{{ lastSyncText }}</text>
        <button class="refresh-btn" :loading="loading" @tap="refreshStats">刷新</button>
      </view>
      <view class="progress-list">
        <view v-for="item in statusItems" :key="item.label" class="progress-row">
          <view class="progress-top">
            <view class="progress-name">
              <text class="legend" :class="item.tone"></text>
              <text>{{ item.label }}</text>
            </view>
            <text class="progress-count">{{ item.value }} 条 · {{ item.rate }}%</text>
          </view>
          <view class="progress-track">
            <view class="progress-fill" :class="item.tone" :style="{ width: item.rate + '%' }"></view>
          </view>
        </view>
      </view>
    </view>

    <view class="section-card">
      <view class="section-head">
        <text class="section-title">回执明细</text>
        <text class="section-note">{{ filteredRecords.length }} 条</text>
      </view>
      <view class="filter-tabs">
        <view
          v-for="item in filterItems"
          :key="item.value"
          class="filter-tab"
          :class="{ active: activeFilter === item.value }"
          @tap="activeFilter = item.value"
        >
          {{ item.label }}
        </view>
      </view>
      <view v-if="filteredRecords.length" class="record-list">
        <view v-for="record in filteredRecords" :key="record.id" class="record-row">
          <view class="record-main">
            <view class="record-title-line">
              <text class="record-name">{{ record.guestName || '未填写姓名' }}</text>
              <text class="record-status" :class="statusTone(record.attendanceStatus)">{{ statusLabel(record.attendanceStatus) }}</text>
            </view>
            <text class="record-meta">
              {{ record.guestCount || 1 }} 人 · {{ record.mealRequired ? '用餐' : '不用餐' }} · {{ record.accommodationRequired ? '住宿' : '不住宿' }}
            </text>
            <text v-if="record.message" class="record-message">{{ record.message }}</text>
          </view>
          <text class="record-time">{{ formatDate(record.updatedAt || record.createdAt) }}</text>
        </view>
      </view>
      <view v-else class="record-empty">
        <text>当前筛选暂无回执。</text>
        <view class="empty-actions">
          <button class="ghost-button" @tap="shareInvite">继续发送请柬</button>
          <button class="ghost-button" @tap="openBanquetDetail">返回管理台</button>
        </view>
      </view>
    </view>

    <view class="section-card">
      <view class="section-head">
        <text class="section-title">{{ activeTheme.prepTitle }}</text>
        <text class="section-note">按当前回执估算</text>
      </view>
      <view class="prep-list">
        <view class="prep-row">
          <text class="prep-title">桌席预估</text>
          <text class="prep-value">{{ tableEstimate }} 桌</text>
        </view>
        <view class="prep-row">
          <text class="prep-title">待确认人数</text>
          <text class="prep-value">{{ stats?.pendingRecords || 0 }} 人</text>
        </view>
        <view class="prep-row">
          <text class="prep-title">未出席记录</text>
          <text class="prep-value muted">{{ stats?.declinedRecords || 0 }} 条</text>
        </view>
      </view>
    </view>

    <view class="action-card">
      <button class="ghost-button wide" @tap="copyInvitePath">复制请柬路径</button>
      <button class="primary-button" @tap="shareInvite">继续发送请柬</button>
      <button class="ghost-button" @tap="openBanquetDetail">返回宴席管理台</button>
    </view>

    <view v-if="!stats && !loading" class="empty-card">
      <text>暂无回执统计，请先发送请柬并邀请宾客填写。</text>
      <button class="primary-button" @tap="shareInvite">去发送请柬</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { request } from '../../../api/client';
import { requireBanquetToast, resolveBanquetId, writeLastBanquetContext } from '../../../utils/banquet';
import { eventThemeFor, fetchBanquetEventType, readActiveEventType, writeActiveEventType } from '../../../utils/event-theme';

interface RsvpStats {
  shareSlug?: string;
  banquetId?: number;
  totalRecords: number;
  attendingRecords: number;
  pendingRecords: number;
  declinedRecords: number;
  totalGuests: number;
  mealRequiredGuests: number;
  accommodationRequiredGuests: number;
}

interface RsvpRecord {
  id: number;
  guestName?: string;
  phone?: string;
  attendanceStatus: string;
  mealRequired?: number;
  accommodationRequired?: number;
  guestCount?: number;
  message?: string;
  createdAt?: string;
  updatedAt?: string;
}

const stats = ref<RsvpStats>();
const records = ref<RsvpRecord[]>([]);
const banquetId = ref('');
const shareSlug = ref('');
const loading = ref(false);
const lastSyncText = ref('');
const activeFilter = ref('ALL');
const eventType = ref(readActiveEventType());
const activeTheme = computed(() => eventThemeFor(eventType.value));
const filterItems = computed(() => [
  { label: `全部 ${records.value.length}`, value: 'ALL' },
  { label: `出席 ${stats.value?.attendingRecords || 0}`, value: 'ATTENDING' },
  { label: `待定 ${stats.value?.pendingRecords || 0}`, value: 'PENDING' },
  { label: `不出席 ${stats.value?.declinedRecords || 0}`, value: 'DECLINED' }
]);
const invitePath = computed(() => shareSlug.value ? `/pages/invite/public/index?slug=${shareSlug.value}` : '');
const attendingRate = computed(() => {
  const total = Number(stats.value?.totalRecords || 0);
  if (!total) return 0;
  return Math.round((Number(stats.value?.attendingRecords || 0) / total) * 100);
});
const tableEstimate = computed(() => {
  const guests = Number(stats.value?.totalGuests || 0);
  if (!guests) return 0;
  return Math.ceil(guests / 10);
});
const statusItems = computed(() => {
  const total = Number(stats.value?.totalRecords || 0);
  const rate = (value: number) => total ? Math.round((value / total) * 100) : 0;
  const attending = Number(stats.value?.attendingRecords || 0);
  const pending = Number(stats.value?.pendingRecords || 0);
  const declined = Number(stats.value?.declinedRecords || 0);
  return [
    { label: '确认出席', value: attending, rate: rate(attending), tone: 'red' },
    { label: '暂未确定', value: pending, rate: rate(pending), tone: 'orange' },
    { label: '不便出席', value: declined, rate: rate(declined), tone: 'gray' }
  ];
});
const filteredRecords = computed(() => {
  if (activeFilter.value === 'ALL') {
    return records.value;
  }
  return records.value.filter((record) => normalizeStatus(record.attendanceStatus) === activeFilter.value);
});

async function load() {
  if (!banquetId.value) {
    return;
  }
  loading.value = true;
  try {
    const [nextStats, nextRecords] = await Promise.all([
      request<RsvpStats>(`/rsvp/stats?banquetId=${banquetId.value}`),
      request<RsvpRecord[]>(`/rsvp/list?banquetId=${banquetId.value}`).catch(() => [])
    ]);
    stats.value = nextStats;
    records.value = nextRecords || [];
    lastSyncText.value = `已同步 ${formatDate(new Date().toISOString())}`;
  } finally {
    loading.value = false;
  }
}

async function refreshStats() {
  await load();
  uni.showToast({ title: '回执统计已刷新', icon: 'success' });
}

function shareInvite() {
  if (shareSlug.value) {
    safeNavigate(`/pages/invite/public/index?slug=${shareSlug.value}`, '请柬页面打开失败');
    return;
  }
  if (!banquetId.value) {
    uni.showToast({ title: '缺少宴席信息', icon: 'none' });
    return;
  }
  safeNavigate(`/pages/banquet/detail/index?id=${banquetId.value}`, '宴席管理台打开失败');
}

function openBanquetDetail() {
  if (!banquetId.value) {
    uni.showToast({ title: '缺少宴席信息', icon: 'none' });
    return;
  }
  safeNavigate(`/pages/banquet/detail/index?id=${banquetId.value}`, '宴席管理台打开失败');
}

function copyInvitePath() {
  if (!invitePath.value) {
    uni.showToast({ title: '暂无请柬路径', icon: 'none' });
    return;
  }
  uni.setClipboardData({
    data: invitePath.value,
    success: () => uni.showToast({ title: '已复制请柬路径', icon: 'success' }),
    fail: () => uni.showToast({ title: '复制失败', icon: 'none' })
  });
}

function normalizeStatus(status?: string) {
  if (status === 'ATTEND') {
    return 'ATTENDING';
  }
  return status || 'PENDING';
}

function statusLabel(status?: string) {
  const value = normalizeStatus(status);
  if (value === 'ATTENDING') return '确认出席';
  if (value === 'DECLINED') return '不便出席';
  return '暂未确定';
}

function statusTone(status?: string) {
  const value = normalizeStatus(status);
  if (value === 'ATTENDING') return 'red';
  if (value === 'DECLINED') return 'gray';
  return 'orange';
}

function formatDate(value?: string) {
  if (!value) {
    return '';
  }
  return value.replace('T', ' ').slice(5, 16);
}

function safeNavigate(url: string, failTitle: string) {
  uni.navigateTo({
    url,
    fail: () => {
      uni.redirectTo({
        url,
        fail: () => uni.showToast({ title: failTitle, icon: 'none' })
      });
    }
  });
}

onMounted(async () => {
  const pages = getCurrentPages();
  const current = pages[pages.length - 1] as unknown as { options?: Record<string, string> };
  banquetId.value = await resolveBanquetId(current.options?.banquetId);
  if (!banquetId.value) {
    requireBanquetToast();
  }
  if (banquetId.value) {
    const detail = await request<{
      banquet?: { id?: number; name?: string; eventTypeCode?: string; themeCode?: string; banquetTime?: string; location?: string };
      eventTypeCode?: string;
      invitation?: { shareSlug?: string };
    }>(`/banquets/${banquetId.value}`).catch(() => undefined);
    const resolvedEventType = detail?.banquet?.eventTypeCode || detail?.eventTypeCode || await fetchBanquetEventType(banquetId.value, request, eventType.value);
    eventType.value = writeActiveEventType(resolvedEventType);
    if (detail?.banquet?.id) {
      writeLastBanquetContext({
        id: detail.banquet.id,
        name: detail.banquet.name,
        eventTypeCode: resolvedEventType,
        themeCode: detail.banquet.themeCode,
        banquetTime: detail.banquet.banquetTime,
        location: detail.banquet.location,
        shareSlug: detail.invitation?.shareSlug
      });
    }
    shareSlug.value = detail?.invitation?.shareSlug || '';
  }
  await load();
});
</script>

<style scoped>
.page {
  --accent: #e60012;
  --accent-dark: #c40005;
  min-height: 100vh;
  padding: 24rpx;
  background: #fff8ef;
  box-sizing: border-box;
  color: #171c2a;
}

.page.orange {
  --accent: #d96a11;
  --accent-dark: #a64209;
}

.page.pink {
  --accent: #e7566f;
  --accent-dark: #b52d4c;
}

.page.green {
  --accent: #188356;
  --accent-dark: #0c5f3e;
}

.page.blue {
  --accent: #2563eb;
  --accent-dark: #1d4ed8;
}

.page.black {
  --accent: #2f3338;
  --accent-dark: #0d0f12;
}

.page.purple {
  --accent: #7c3aed;
  --accent-dark: #5b21b6;
}

.hero-card {
  position: relative;
  overflow: hidden;
  padding: 34rpx;
  border-radius: 28rpx;
  background:
    radial-gradient(circle at 86% 20%, rgba(255, 217, 150, 0.38), transparent 180rpx),
    linear-gradient(135deg, var(--accent) 0%, var(--accent-dark) 62%, var(--accent-dark) 100%);
  box-shadow: 0 16rpx 42rpx rgba(184, 17, 21, 0.24);
}

.hero-art {
  position: absolute;
  right: -38rpx;
  top: -34rpx;
  width: 240rpx;
  height: 240rpx;
  border-radius: 50%;
  background: rgba(255, 224, 170, 0.16);
}

.hero-knot {
  position: absolute;
  right: 72rpx;
  top: 66rpx;
  color: rgba(255, 239, 206, 0.34);
  font-family: serif;
  font-size: 94rpx;
  font-weight: 900;
}

.hero-label,
.hero-title,
.hero-desc,
.hero-main {
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
  margin-top: 12rpx;
  color: rgba(255, 248, 232, 0.94);
  font-size: 27rpx;
  line-height: 1.5;
}

.hero-main {
  display: flex;
  align-items: end;
  justify-content: space-between;
  margin-top: 30rpx;
}

.hero-number {
  display: block;
  color: #fff4cf;
  font-size: 76rpx;
  font-weight: 900;
}

.hero-number-label {
  display: block;
  margin-top: 6rpx;
  color: rgba(255, 245, 224, 0.82);
  font-size: 24rpx;
  font-weight: 700;
}

.rate-ring {
  display: grid;
  place-items: center;
  width: 150rpx;
  height: 150rpx;
  border: 8rpx solid rgba(255, 244, 207, 0.36);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.12);
}

.rate-value,
.rate-label {
  display: block;
}

.rate-value {
  color: #fff7dd;
  font-size: 34rpx;
  font-weight: 900;
}

.rate-label {
  margin-top: -30rpx;
  color: rgba(255, 245, 224, 0.82);
  font-size: 20rpx;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16rpx;
  margin-top: 24rpx;
}

.summary-item,
.section-card,
.action-card,
.empty-card {
  border: 1rpx solid #f0dfcf;
  border-radius: 24rpx;
  background: #fff;
  box-shadow: 0 12rpx 32rpx rgba(82, 45, 24, 0.07);
}

.summary-item {
  display: grid;
  justify-items: center;
  gap: 8rpx;
  padding: 24rpx 10rpx;
}

.summary-icon {
  display: grid;
  place-items: center;
  width: 50rpx;
  height: 50rpx;
  border-radius: 50%;
  font-size: 22rpx;
  font-weight: 900;
}

.summary-icon.red {
  background: #fff0f0;
  color: var(--accent);
}

.summary-icon.orange {
  background: #fff3e5;
  color: #c45a16;
}

.summary-icon.gold {
  background: #fff6d8;
  color: #a87308;
}

.summary-value {
  color: #171c2a;
  font-size: 38rpx;
  font-weight: 900;
}

.summary-label {
  color: #8a7768;
  font-size: 23rpx;
  font-weight: 700;
}

.section-card {
  margin-top: 24rpx;
  padding: 28rpx;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24rpx;
}

.section-title {
  color: #171c2a;
  font-size: 34rpx;
  font-weight: 900;
}

.section-note {
  color: #93877d;
  font-size: 24rpx;
}

.refresh-btn {
  height: 58rpx;
  margin: 0;
  padding: 0 22rpx;
  border: 1rpx solid #ead8ca;
  border-radius: 999rpx;
  background: #fffaf5;
  color: var(--accent);
  font-size: 24rpx;
  font-weight: 800;
  line-height: 58rpx;
}

.refresh-btn::after {
  border: 0;
}

.progress-list {
  display: grid;
  gap: 24rpx;
}

.progress-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12rpx;
}

.progress-name {
  display: flex;
  align-items: center;
  gap: 12rpx;
  color: #303442;
  font-size: 27rpx;
  font-weight: 800;
}

.legend {
  width: 18rpx;
  height: 18rpx;
  border-radius: 50%;
}

.legend.red,
.progress-fill.red {
  background: var(--accent);
}

.legend.orange,
.progress-fill.orange {
  background: #ee8a23;
}

.legend.gray,
.progress-fill.gray {
  background: #9ca3af;
}

.progress-count {
  color: #8d929d;
  font-size: 24rpx;
}

.progress-track {
  overflow: hidden;
  height: 16rpx;
  border-radius: 999rpx;
  background: #f4ebe3;
}

.progress-fill {
  height: 100%;
  border-radius: 999rpx;
  transition: width 0.2s ease;
}

.filter-tabs {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12rpx;
  margin-bottom: 20rpx;
}

.filter-tab {
  min-width: 0;
  height: 62rpx;
  border: 1rpx solid #ead8ca;
  border-radius: 999rpx;
  background: #fffaf5;
  color: #7e7168;
  font-size: 23rpx;
  font-weight: 800;
  line-height: 62rpx;
  text-align: center;
}

.filter-tab.active {
  border-color: transparent;
  background: linear-gradient(135deg, var(--accent), var(--accent-dark));
  color: #fff;
}

.record-list {
  display: grid;
  gap: 14rpx;
}

.record-row {
  display: flex;
  gap: 18rpx;
  justify-content: space-between;
  padding: 20rpx;
  border: 1rpx solid #f1e4d6;
  border-radius: 18rpx;
  background: #fffdfb;
}

.record-main {
  min-width: 0;
  flex: 1;
}

.record-title-line {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.record-name {
  overflow: hidden;
  max-width: 220rpx;
  color: #171c2a;
  font-size: 29rpx;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.record-status {
  padding: 5rpx 12rpx;
  border-radius: 999rpx;
  font-size: 20rpx;
  font-weight: 800;
}

.record-status.red {
  background: #fff0f0;
  color: var(--accent);
}

.record-status.orange {
  background: #fff3e5;
  color: #c45a16;
}

.record-status.gray {
  background: #f3f4f6;
  color: #6b7280;
}

.record-meta,
.record-message {
  display: block;
  margin-top: 9rpx;
  color: #83766d;
  font-size: 24rpx;
  line-height: 1.45;
}

.record-message {
  color: var(--accent);
}

.record-time {
  flex: none;
  color: #9ca3af;
  font-size: 22rpx;
}

.record-empty {
  padding: 30rpx 0 8rpx;
  color: #9ca3af;
  font-size: 25rpx;
  text-align: center;
}

.empty-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
  margin-top: 22rpx;
}

.prep-list {
  display: grid;
  gap: 4rpx;
}

.prep-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 74rpx;
  border-bottom: 1rpx solid #f0dfcf;
}

.prep-row:last-child {
  border-bottom: 0;
}

.prep-title {
  color: #6d5848;
  font-size: 27rpx;
  font-weight: 700;
}

.prep-value {
  color: var(--accent);
  font-size: 29rpx;
  font-weight: 900;
}

.prep-value.muted {
  color: #6b7280;
}

.action-card {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
  margin-top: 24rpx;
  padding: 22rpx;
}

.action-card .wide {
  grid-column: 1 / -1;
}

.primary-button,
.ghost-button {
  height: 86rpx;
  margin: 0;
  border-radius: 18rpx;
  font-size: 28rpx;
  font-weight: 900;
  line-height: 86rpx;
}

.primary-button {
  background: linear-gradient(135deg, var(--accent), var(--accent-dark));
  color: #fff;
}

.ghost-button {
  border: 1rpx solid #ead8ca;
  background: #fffaf5;
  color: var(--accent);
}

.primary-button::after,
.ghost-button::after {
  border: 0;
}

.empty-card {
  margin-top: 24rpx;
  padding: 34rpx 28rpx;
  color: #8a7768;
  font-size: 26rpx;
  line-height: 1.5;
  text-align: center;
}
</style>
