<template>
  <view class="page" :class="activeTheme.tone">
    <view class="summary-card">
      <view class="summary-art">
        <text class="summary-knot">{{ activeTheme.mark }}</text>
      </view>
      <text class="summary-label">累计{{ activeTheme.giftLabel }}</text>
      <text class="summary-amount">{{ formatMoney(summary?.totalAmount || 0) }}</text>
      <view class="summary-stats">
        <view>
          <text class="stat-value">{{ summary?.totalRecords || 0 }}</text>
          <text class="stat-label">总笔数</text>
        </view>
        <view>
          <text class="stat-value">{{ formatMoney(sourceAmount('CASH')) }}</text>
          <text class="stat-label">{{ activeTheme.offlineGiftLabel }}</text>
        </view>
        <view>
          <text class="stat-value">{{ formatMoney(onlineTotal) }}</text>
          <text class="stat-label">在线{{ activeTheme.giftLabel }}</text>
        </view>
      </view>
    </view>

    <view v-if="banquetId" class="manage-card">
      <view>
        <text class="manage-title">{{ activeTheme.giftRecordLabel }}管理</text>
        <text class="manage-desc">{{ lastSyncText || '从宴席管理台进入后，可随时返回继续办席。' }}</text>
      </view>
      <button class="mini-button" @tap="openBanquetDetail">返回管理台</button>
    </view>

    <view class="tool-card">
      <view class="search-box">
        <text class="search-icon">⌕</text>
        <input v-model="keyword" placeholder="搜索来宾姓名" confirm-type="search" @confirm="load()" />
      </view>
      <view class="source-tabs">
        <view
          v-for="(source, index) in sources"
          :key="source.value || 'ALL'"
          class="source-tab"
          :class="{ active: sourceIndex === index }"
          @tap="selectSource(index)"
        >
          {{ source.label }}
        </view>
      </view>
      <view class="tool-actions">
        <button class="mini-button primary" @tap="openOfflineGift">{{ activeTheme.offlineGiftLabel }}</button>
        <button class="mini-button" @tap="resetFilters">重置筛选</button>
      </view>
    </view>

    <view class="list-card">
      <view class="section-head">
        <text class="section-title">{{ activeTheme.giftRecordLabel }}</text>
        <button class="refresh-btn" :loading="loading" @tap="refreshList">刷新 {{ gifts.length }} 条</button>
      </view>
      <view v-if="loading" class="state-box">同步{{ activeTheme.giftRecordLabel }}中</view>
      <view v-else-if="gifts.length === 0" class="state-box">
        <text class="state-title">{{ emptyText }}</text>
        <view class="empty-actions">
          <button class="mini-button primary" @tap="openOfflineGift">{{ activeTheme.offlineGiftLabel }}</button>
          <button class="mini-button" @tap="openBanquetDetail">返回管理台</button>
        </view>
      </view>
      <view
        v-for="gift in gifts"
        :key="gift.id"
        class="gift-row"
        :class="{ highlighted: String(gift.id) === highlightId }"
        @tap="openGiftDetail(gift)"
      >
        <view class="avatar">{{ guestInitial(gift.guestName) }}</view>
        <view class="gift-main">
          <view class="gift-title-line">
            <text class="gift-name">{{ gift.guestName }}</text>
            <text class="source-badge" :class="sourceTone(gift.giftSource)">{{ sourceLabel(gift.giftSource) }}</text>
          </view>
          <text class="gift-meta">{{ formatTime(gift.receivedAt) }}</text>
          <text v-if="gift.blessing" class="blessing">{{ gift.blessing }}</text>
        </view>
        <text class="gift-amount">{{ formatMoney(gift.amount) }}</text>
      </view>
    </view>

    <view v-if="selectedGift" class="detail-mask" @tap="closeGiftDetail">
      <view class="detail-panel" @tap.stop>
        <view class="detail-head">
          <text class="detail-title">{{ selectedGift.guestName }}</text>
          <text class="source-badge" :class="sourceTone(selectedGift.giftSource)">{{ sourceLabel(selectedGift.giftSource) }}</text>
        </view>
        <view class="detail-amount">{{ formatMoney(selectedGift.amount) }}</view>
        <view class="detail-row">
          <text>收礼时间</text>
          <text>{{ formatTime(selectedGift.receivedAt) }}</text>
        </view>
        <view class="detail-row">
          <text>来源</text>
          <text>{{ sourceLabel(selectedGift.giftSource) }}</text>
        </view>
        <view class="detail-note">
          <text>{{ selectedGift.blessing || '暂无备注' }}</text>
        </view>
        <view class="detail-actions">
          <button class="mini-button" @tap="copyGiftDetail">复制记录</button>
          <button class="mini-button" @tap="openFavorFromGift">查看人情</button>
          <button class="mini-button primary" @tap="closeGiftDetail">关闭</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { request } from '../../../api/client';
import { requireBanquetToast, resolveBanquetId } from '../../../utils/banquet';
import { eventThemeFor, fetchBanquetEventType, readActiveEventType, writeActiveEventType } from '../../../utils/event-theme';

interface GiftRecord {
  id: number;
  guestName: string;
  amount: number;
  giftSource: string;
  blessing?: string;
  receivedAt?: string;
}

interface GiftSummary {
  totalRecords: number;
  totalAmount: number;
  sourceCounts: Record<string, number>;
  sourceAmounts: Record<string, number>;
}

const gifts = ref<GiftRecord[]>([]);
const summary = ref<GiftSummary>();
const banquetId = ref('');
const keyword = ref('');
const sourceIndex = ref(0);
const loading = ref(false);
const highlightId = ref('');
const selectedGift = ref<GiftRecord>();
const lastSyncText = ref('');
const eventType = ref(readActiveEventType());
const activeTheme = computed(() => eventThemeFor(eventType.value));
const sources = computed(() => [
  { label: '全部', value: '' },
  { label: activeTheme.value.onlineGiftLabel, value: 'ONLINE_GIFT' },
  { label: '现场扫码', value: 'ONSITE_QR' },
  { label: activeTheme.value.offlineGiftLabel, value: 'CASH' }
]);
const onlineTotal = computed(() => sourceAmount('ONLINE_GIFT') + sourceAmount('ONSITE_QR'));
const emptyText = computed(() => {
  const source = sources.value[sourceIndex.value].label;
  if (keyword.value && sources.value[sourceIndex.value].value) {
    return `没有找到“${keyword.value}”的${source}记录`;
  }
  if (keyword.value) {
    return `没有找到“${keyword.value}”的${activeTheme.value.giftRecordLabel}`;
  }
  if (sources.value[sourceIndex.value].value) {
    return `暂无${source}记录`;
  }
  return `暂无${activeTheme.value.giftRecordLabel}`;
});

function selectSource(index: number) {
  sourceIndex.value = index;
  highlightId.value = '';
  load();
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '时间待定';
}

function formatMoney(value: unknown) {
  return `¥${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })}`;
}

function sourceAmount(source: string) {
  return Number(summary.value?.sourceAmounts?.[source] || 0);
}

function guestInitial(name?: string) {
  return (name || '礼').slice(0, 1);
}

function sourceLabel(value: string) {
  const item = sources.value.find((source) => source.value === value);
  return item?.label || value;
}

function sourceTone(value: string) {
  if (value === 'CASH') return 'cash';
  if (value === 'ONSITE_QR') return 'qr';
  return 'online';
}

function resetFilters() {
  keyword.value = '';
  sourceIndex.value = 0;
  highlightId.value = '';
  load();
}

function openOfflineGift() {
  if (!banquetId.value) {
    return;
  }
  safeNavigate(`/pages/gift/offline/index?banquetId=${banquetId.value}`, `${activeTheme.value.offlineGiftLabel}打开失败`);
}

function openBanquetDetail() {
  if (!banquetId.value) {
    requireBanquetToast();
    return;
  }
  safeNavigate(`/pages/banquet/detail/index?id=${banquetId.value}`, '宴席管理台打开失败');
}

function openGiftDetail(gift: GiftRecord) {
  selectedGift.value = gift;
}

function closeGiftDetail() {
  selectedGift.value = undefined;
}

function copyGiftDetail() {
  if (!selectedGift.value) {
    return;
  }
  const gift = selectedGift.value;
  uni.setClipboardData({
    data: `${gift.guestName} ${sourceLabel(gift.giftSource)} ${formatMoney(gift.amount)} ${formatTime(gift.receivedAt)} ${gift.blessing || ''}`.trim(),
    success: () => uni.showToast({ title: '已复制记录', icon: 'success' }),
    fail: () => uni.showToast({ title: '复制失败', icon: 'none' })
  });
}

async function openFavorFromGift() {
  if (!selectedGift.value) {
    return;
  }
  uni.setStorageSync('favor-focus-contact', selectedGift.value.guestName);
  try {
    const result = await request<{ contact?: { id?: number } }>(`/favor/compare?contactName=${encodeURIComponent(selectedGift.value.guestName)}`);
    const id = result.contact?.id;
    if (id) {
      closeGiftDetail();
      safeNavigate(`/pages/favor/detail/index?id=${id}`, '人情详情打开失败');
      return;
    }
  } catch {
    // Fall back to the tab when the comparison cannot be loaded.
  }
  closeGiftDetail();
  uni.switchTab({ url: '/pages/favor/index/index' });
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

async function load() {
  if (!banquetId.value) {
    return;
  }
  loading.value = true;
  try {
    const params = [
      `banquetId=${banquetId.value}`,
      sources.value[sourceIndex.value].value ? `source=${sources.value[sourceIndex.value].value}` : '',
      keyword.value ? `keyword=${encodeURIComponent(keyword.value)}` : ''
    ].filter(Boolean).join('&');
    const [list, stat] = await Promise.all([
      request<GiftRecord[]>(`/gifts?${params}`),
      request<GiftSummary>(`/gifts/summary?banquetId=${banquetId.value}`)
    ]);
    gifts.value = list;
    summary.value = stat;
    lastSyncText.value = `已同步 ${formatTime(new Date().toISOString()).slice(5)}`;
  } finally {
    loading.value = false;
  }
}

async function refreshList() {
  await load();
  uni.showToast({ title: `${activeTheme.value.giftRecordLabel}已刷新`, icon: 'success' });
}

onMounted(async () => {
  const pages = getCurrentPages();
  const current = pages[pages.length - 1] as unknown as { options?: Record<string, string> };
  banquetId.value = await resolveBanquetId(current.options?.banquetId);
  if (!banquetId.value) {
    requireBanquetToast();
    return;
  }
  const source = current.options?.source || '';
  const presetIndex = sources.value.findIndex((item) => item.value === source);
  if (presetIndex >= 0) {
    sourceIndex.value = presetIndex;
  }
  keyword.value = current.options?.keyword || '';
  highlightId.value = current.options?.highlightId || '';
  eventType.value = writeActiveEventType(await fetchBanquetEventType(banquetId.value, request, eventType.value));
  await load();
});

onShow(() => {
  if (banquetId.value) {
    load();
  }
});
</script>

<style scoped>
.page {
  --accent: #e60012;
  --accent-dark: #c40005;
  --accent-soft: #fff0ee;
  --page-bg: #fff8ef;
  --accent-shadow: rgba(230, 0, 18, 0.2);
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
  --page-bg: #fff9f0;
  --accent-shadow: rgba(217, 106, 17, 0.2);
}

.page.pink {
  --accent: #e7566f;
  --accent-dark: #b52d4c;
  --accent-soft: #fff0f4;
  --page-bg: #fff8fa;
  --accent-shadow: rgba(231, 86, 111, 0.2);
}

.page.green {
  --accent: #188356;
  --accent-dark: #0c5f3e;
  --accent-soft: #edf9f1;
  --page-bg: #f7fcf8;
  --accent-shadow: rgba(24, 131, 86, 0.2);
}

.page.blue {
  --accent: #2563eb;
  --accent-dark: #1d4ed8;
  --accent-soft: #edf4ff;
  --page-bg: #f7fbff;
  --accent-shadow: rgba(37, 99, 235, 0.2);
}

.page.black {
  --accent: #2f3338;
  --accent-dark: #0d0f12;
  --accent-soft: #f1f2f4;
  --page-bg: #f7f7f7;
  --accent-shadow: rgba(47, 51, 56, 0.2);
}

.page.purple {
  --accent: #7c3aed;
  --accent-dark: #5b21b6;
  --accent-soft: #f4efff;
  --page-bg: #fbf8ff;
  --accent-shadow: rgba(124, 58, 237, 0.2);
}

.summary-card {
  position: relative;
  overflow: hidden;
  padding: 34rpx;
  border-radius: 28rpx;
  background:
    radial-gradient(circle at 86% 22%, rgba(255, 214, 150, 0.36), transparent 170rpx),
    linear-gradient(135deg, var(--accent) 0%, var(--accent-dark) 60%, var(--accent-dark) 100%);
  box-shadow: 0 16rpx 42rpx var(--accent-shadow);
}

.summary-art {
  position: absolute;
  right: -30rpx;
  top: -24rpx;
  width: 230rpx;
  height: 230rpx;
  border-radius: 50%;
  background: rgba(255, 224, 170, 0.16);
}

.summary-knot {
  position: absolute;
  right: 58rpx;
  top: 54rpx;
  color: rgba(255, 239, 206, 0.34);
  font-family: serif;
  font-size: 94rpx;
  font-weight: 900;
}

.summary-label,
.summary-amount,
.summary-stats {
  position: relative;
  z-index: 2;
}

.summary-label {
  display: block;
  color: #ffe2ba;
  font-size: 26rpx;
  font-weight: 800;
}

.summary-amount {
  display: block;
  margin-top: 10rpx;
  color: #fff7dd;
  font-size: 64rpx;
  font-weight: 900;
}

.summary-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12rpx;
  margin-top: 28rpx;
}

.summary-stats view {
  padding: 18rpx 10rpx;
  border-radius: 18rpx;
  background: rgba(255, 255, 255, 0.16);
  text-align: center;
}

.stat-value,
.stat-label {
  display: block;
}

.stat-value {
  color: #fff8e8;
  font-size: 26rpx;
  font-weight: 900;
}

.stat-label {
  margin-top: 8rpx;
  color: rgba(255, 245, 224, 0.82);
  font-size: 22rpx;
}

.tool-card,
.list-card,
.manage-card {
  margin-top: 24rpx;
  padding: 26rpx;
  border: 1rpx solid #f0dfcf;
  border-radius: 24rpx;
  background: #fff;
  box-shadow: 0 12rpx 32rpx rgba(82, 45, 24, 0.07);
}

.manage-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
}

.manage-title,
.manage-desc,
.state-title {
  display: block;
}

.manage-title {
  color: #171c2a;
  font-size: 30rpx;
  font-weight: 900;
}

.manage-desc {
  margin-top: 8rpx;
  color: #8a7768;
  font-size: 23rpx;
  line-height: 1.4;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 12rpx;
  height: 78rpx;
  padding: 0 22rpx;
  border-radius: 999rpx;
  background: var(--accent-soft);
}

.search-icon {
  color: #a98a74;
  font-size: 30rpx;
}

.search-box input {
  flex: 1;
  height: 78rpx;
  color: #171c2a;
  font-size: 26rpx;
}

.source-tabs {
  display: flex;
  gap: 14rpx;
  margin-top: 20rpx;
  overflow-x: auto;
}

.source-tab {
  flex: 0 0 auto;
  min-width: 128rpx;
  height: 62rpx;
  padding: 0 20rpx;
  border: 1rpx solid #ead8ca;
  border-radius: 999rpx;
  background: #fffdfb;
  color: #7c6a5e;
  font-size: 25rpx;
  font-weight: 800;
  line-height: 62rpx;
  text-align: center;
}

.source-tab.active {
  border-color: transparent;
  background: linear-gradient(135deg, var(--accent), var(--accent-dark));
  color: #fff;
}

.tool-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
  margin-top: 22rpx;
}

.mini-button {
  height: 72rpx;
  margin: 0;
  border: 1rpx solid #ead8ca;
  border-radius: 16rpx;
  background: #fff;
  color: #9e4d32;
  font-size: 26rpx;
  font-weight: 900;
  line-height: 72rpx;
}

.mini-button.primary {
  border-color: transparent;
  background: var(--accent-soft);
  color: var(--accent);
}

.mini-button::after {
  border: 0;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18rpx;
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
  background: var(--accent-soft);
  color: var(--accent-dark);
  font-size: 24rpx;
  font-weight: 800;
  line-height: 58rpx;
}

.refresh-btn::after {
  border: 0;
}

.state-box {
  padding: 54rpx 20rpx;
  border: 1rpx dashed #ead8ca;
  border-radius: 18rpx;
  background: #fffaf6;
  color: #9a6a4c;
  font-size: 26rpx;
  text-align: center;
}

.state-title {
  color: #9a6a4c;
  font-size: 26rpx;
}

.empty-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
  margin-top: 24rpx;
}

.gift-row {
  display: grid;
  grid-template-columns: 72rpx 1fr auto;
  gap: 18rpx;
  align-items: start;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f0dfcf;
}

.gift-row.highlighted {
  margin: 8rpx -14rpx;
  padding: 24rpx 14rpx;
  border: 2rpx solid #f0b17b;
  border-radius: 18rpx;
  background: var(--accent-soft);
}

.gift-row:last-child {
  border-bottom: 0;
}

.avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--accent), var(--accent-dark));
  color: #fff;
  font-size: 30rpx;
  font-weight: 900;
  line-height: 72rpx;
  text-align: center;
}

.gift-main {
  min-width: 0;
}

.gift-title-line {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.gift-name {
  overflow: hidden;
  color: #171c2a;
  font-size: 30rpx;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.source-badge {
  flex: 0 0 auto;
  padding: 5rpx 12rpx;
  border-radius: 999rpx;
  font-size: 20rpx;
  font-weight: 800;
}

.source-badge.cash {
  background: #fff1e7;
  color: #bf5b25;
}

.source-badge.qr {
  background: #eef7ff;
  color: #2563eb;
}

.source-badge.online {
  background: var(--accent-soft);
  color: var(--accent);
}

.gift-meta {
  display: block;
  margin-top: 8rpx;
  color: #8d929d;
  font-size: 24rpx;
}

.blessing {
  display: block;
  margin-top: 12rpx;
  padding: 12rpx 16rpx;
  border-radius: 14rpx;
  background: var(--accent-soft);
  color: #865b3e;
  font-size: 24rpx;
  line-height: 1.5;
}

.gift-amount {
  color: var(--accent);
  font-size: 30rpx;
  font-weight: 900;
  white-space: nowrap;
}

.detail-mask {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: flex;
  align-items: flex-end;
  background: rgba(17, 24, 39, 0.48);
}

.detail-panel {
  width: 100%;
  padding: 32rpx 28rpx calc(28rpx + env(safe-area-inset-bottom));
  border-radius: 28rpx 28rpx 0 0;
  background: var(--page-bg);
  box-sizing: border-box;
}

.detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
}

.detail-title {
  overflow: hidden;
  color: #171c2a;
  font-size: 38rpx;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-amount {
  margin-top: 18rpx;
  color: var(--accent);
  font-size: 58rpx;
  font-weight: 900;
}

.detail-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 72rpx;
  border-bottom: 1rpx solid #f0dfcf;
  color: #6f625b;
  font-size: 27rpx;
  font-weight: 700;
}

.detail-row text:last-child {
  color: #171c2a;
  font-weight: 800;
}

.detail-note {
  margin-top: 20rpx;
  padding: 22rpx;
  border-radius: 18rpx;
  background: #fff;
  color: #865b3e;
  font-size: 26rpx;
  line-height: 1.55;
}

.detail-actions {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16rpx;
  margin-top: 24rpx;
}
</style>
