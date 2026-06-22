<template>
  <view class="page">
    <text class="title">收礼记录</text>
    <view v-if="summary" class="summary">
      <view class="summary-main">
        <text class="summary-label">总金额</text>
        <text class="summary-amount">{{ formatMoney(summary.totalAmount) }}</text>
      </view>
      <view class="summary-grid">
        <text>总笔数：{{ summary.totalRecords }}</text>
        <text>线上随礼：{{ formatMoney(summary.sourceAmounts.ONLINE_GIFT || 0) }}</text>
        <text>现场扫码：{{ formatMoney(summary.sourceAmounts.ONSITE_QR || 0) }}</text>
        <text>现金记礼：{{ formatMoney(summary.sourceAmounts.CASH || 0) }}</text>
      </view>
    </view>
    <input v-model="keyword" class="input" placeholder="按来宾姓名搜索" />
    <picker :range="sources" range-key="label" @change="onSourceChange">
      <view class="input">来源：{{ sources[sourceIndex].label }}</view>
    </picker>
    <view class="actions">
      <button size="mini" @click="load">筛选</button>
      <button size="mini" @click="resetFilters">重置</button>
    </view>
    <view v-if="!loading && gifts.length === 0" class="empty">
      <text>{{ emptyText }}</text>
    </view>
    <view v-for="gift in gifts" :key="gift.id" class="row">
      <view>
        <text class="name">{{ gift.guestName }}</text>
        <text class="meta">{{ sourceLabel(gift.giftSource) }} / {{ formatTime(gift.receivedAt) }}</text>
        <text v-if="gift.blessing" class="blessing">{{ gift.blessing }}</text>
      </view>
      <text class="amount">{{ formatMoney(gift.amount) }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { request } from '../../../api/client';

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

function onSourceChange(event: { detail: { value: number | string } }) {
  sourceIndex.value = Number(event.detail.value);
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ') : '';
}

function formatMoney(value: unknown) {
  return `¥${Number(value || 0).toFixed(2)}`;
}

function sourceLabel(value: string) {
  const item = sources.find((source) => source.value === value);
  return item?.label || value;
}

function resetFilters() {
  keyword.value = '';
  sourceIndex.value = 0;
  load();
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

onMounted(() => {
  const pages = getCurrentPages();
  const current = pages[pages.length - 1] as unknown as { options?: Record<string, string> };
  banquetId.value = current.options?.banquetId || '';
  load();
});
</script>

<style scoped>
.page { padding: 24rpx; }
.title { display: block; margin-bottom: 24rpx; font-size: 40rpx; font-weight: 600; }
.summary { display: grid; gap: 16rpx; margin-bottom: 20rpx; padding: 20rpx; border: 1px solid #e5e7eb; border-radius: 8rpx; background: #fff; }
.summary-main { display: grid; gap: 6rpx; }
.summary-label { color: #64748b; font-size: 24rpx; }
.summary-amount { color: #b91c1c; font-size: 44rpx; font-weight: 700; }
.summary-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10rpx; color: #374151; font-size: 24rpx; }
.input { box-sizing: border-box; width: 100%; margin-bottom: 16rpx; padding: 18rpx; border: 1px solid #ddd; border-radius: 8rpx; }
.actions { display: flex; gap: 12rpx; margin-bottom: 14rpx; }
.actions button { margin: 0; }
.empty { padding: 36rpx 20rpx; border: 1px dashed #d1d5db; border-radius: 8rpx; color: #64748b; text-align: center; }
.row { display: flex; justify-content: space-between; gap: 12rpx; padding: 18rpx 0; border-bottom: 1px solid #eee; }
.name, .meta, .blessing { display: block; }
.meta { margin-top: 6rpx; color: #666; font-size: 24rpx; }
.blessing { margin-top: 10rpx; padding: 10rpx 12rpx; border-radius: 8rpx; background: #f8fafc; color: #374151; font-size: 24rpx; line-height: 1.5; }
.amount { color: #b91c1c; font-weight: 600; }
</style>
