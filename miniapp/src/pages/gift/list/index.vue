<template>
  <view class="page">
    <view class="summary-card">
      <view class="summary-art">
        <text class="summary-knot">囍</text>
      </view>
      <text class="summary-label">累计收礼</text>
      <text class="summary-amount">{{ formatMoney(summary?.totalAmount || 0) }}</text>
      <view class="summary-stats">
        <view>
          <text class="stat-value">{{ summary?.totalRecords || 0 }}</text>
          <text class="stat-label">总笔数</text>
        </view>
        <view>
          <text class="stat-value">{{ formatMoney(summary?.sourceAmounts.CASH || 0) }}</text>
          <text class="stat-label">现金记礼</text>
        </view>
        <view>
          <text class="stat-value">{{ formatMoney(onlineTotal) }}</text>
          <text class="stat-label">在线收礼</text>
        </view>
      </view>
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
        <button class="mini-button primary" @tap="openOfflineGift">线下记礼</button>
        <button class="mini-button" @tap="resetFilters">重置筛选</button>
      </view>
    </view>

    <view class="list-card">
      <view class="section-head">
        <text class="section-title">收礼明细</text>
        <text class="section-note">{{ gifts.length }} 条</text>
      </view>
      <view v-if="loading" class="state-box">同步收礼记录中</view>
      <view v-else-if="gifts.length === 0" class="state-box">{{ emptyText }}</view>
      <view v-for="gift in gifts" :key="gift.id" class="gift-row">
        <view class="avatar">{{ gift.guestName.slice(0, 1) || '礼' }}</view>
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
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { request } from '../../../api/client';
import { requireBanquetToast, resolveBanquetId } from '../../../utils/banquet';

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
  sourceAmounts: Record<string, number>;
}

const gifts = ref<GiftRecord[]>([]);
const summary = ref<GiftSummary>();
const banquetId = ref('');
const keyword = ref('');
const sourceIndex = ref(0);
const loading = ref(false);
const sources = [
  { label: '全部', value: '' },
  { label: '线上随礼', value: 'ONLINE_GIFT' },
  { label: '现场扫码', value: 'ONSITE_QR' },
  { label: '现金记礼', value: 'CASH' }
];
const onlineTotal = computed(() => Number(summary.value?.sourceAmounts.ONLINE_GIFT || 0) + Number(summary.value?.sourceAmounts.ONSITE_QR || 0));
const emptyText = computed(() => {
  const source = sources[sourceIndex.value].label;
  if (keyword.value && sources[sourceIndex.value].value) {
    return `没有找到“${keyword.value}”的${source}记录`;
  }
  if (keyword.value) {
    return `没有找到“${keyword.value}”的收礼记录`;
  }
  if (sources[sourceIndex.value].value) {
    return `暂无${source}记录`;
  }
  return '暂无收礼记录';
});

function selectSource(index: number) {
  sourceIndex.value = index;
  load();
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '时间待定';
}

function formatMoney(value: unknown) {
  return `¥${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })}`;
}

function sourceLabel(value: string) {
  const item = sources.find((source) => source.value === value);
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
  load();
}

function openOfflineGift() {
  if (!banquetId.value) {
    return;
  }
  uni.navigateTo({ url: `/pages/gift/offline/index?banquetId=${banquetId.value}` });
}

async function load() {
  if (!banquetId.value) {
    return;
  }
  loading.value = true;
  try {
    const params = [
      `banquetId=${banquetId.value}`,
      sources[sourceIndex.value].value ? `source=${sources[sourceIndex.value].value}` : '',
      keyword.value ? `keyword=${encodeURIComponent(keyword.value)}` : ''
    ].filter(Boolean).join('&');
    const [list, stat] = await Promise.all([
      request<GiftRecord[]>(`/gifts?${params}`),
      request<GiftSummary>(`/gifts/summary?banquetId=${banquetId.value}`)
    ]);
    gifts.value = list;
    summary.value = stat;
  } finally {
    loading.value = false;
  }
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

.summary-card {
  position: relative;
  overflow: hidden;
  padding: 34rpx;
  border-radius: 28rpx;
  background:
    radial-gradient(circle at 86% 22%, rgba(255, 214, 150, 0.36), transparent 170rpx),
    linear-gradient(135deg, #e71921 0%, #c9161c 60%, #9b0e13 100%);
  box-shadow: 0 16rpx 42rpx rgba(184, 17, 21, 0.24);
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
.list-card {
  margin-top: 24rpx;
  padding: 26rpx;
  border: 1rpx solid #f0dfcf;
  border-radius: 24rpx;
  background: #fff;
  box-shadow: 0 12rpx 32rpx rgba(82, 45, 24, 0.07);
}

.search-box {
  display: flex;
  align-items: center;
  gap: 12rpx;
  height: 78rpx;
  padding: 0 22rpx;
  border-radius: 999rpx;
  background: #fff6ee;
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
  background: linear-gradient(135deg, #e83a32, #c91419);
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
  background: #fff1ea;
  color: #c7191e;
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

.state-box {
  padding: 54rpx 20rpx;
  border: 1rpx dashed #ead8ca;
  border-radius: 18rpx;
  background: #fffaf6;
  color: #9a6a4c;
  font-size: 26rpx;
  text-align: center;
}

.gift-row {
  display: grid;
  grid-template-columns: 72rpx 1fr auto;
  gap: 18rpx;
  align-items: start;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f0dfcf;
}

.gift-row:last-child {
  border-bottom: 0;
}

.avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #f07a64, #dc2e2a);
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
  background: #fff0f0;
  color: #c7191e;
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
  background: #fff8ef;
  color: #865b3e;
  font-size: 24rpx;
  line-height: 1.5;
}

.gift-amount {
  color: #c7191e;
  font-size: 30rpx;
  font-weight: 900;
  white-space: nowrap;
}
</style>
