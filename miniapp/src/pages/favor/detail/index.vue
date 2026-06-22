<template>
  <view class="page" v-if="detail">
    <text class="title">{{ detail.contact.contactName }}</text>
    <view class="stats">
      <view class="stats-main">
        <text class="stats-label">当前差额</text>
        <text class="stats-amount" :class="balanceClass(detail.balance)">{{ formatMoney(detail.balance) }}</text>
        <text class="stats-note">{{ balanceText(detail.balance) }}</text>
      </view>
      <view class="stats-grid">
        <text>他送我的：{{ formatMoney(detail.receivedAmount) }}</text>
        <text>我送他的：{{ formatMoney(detail.givenAmount) }}</text>
        <text>往来笔数：{{ detail.entries.length }}</text>
        <text>联系人：{{ detail.contact.contactName }}</text>
      </view>
    </view>
    <view v-if="detail.entries.length === 0" class="empty">
      <text>暂无人情往来明细</text>
    </view>
    <view v-for="entry in detail.entries" :key="entry.id" class="row">
      <view>
        <text class="name">{{ directionLabel(entry.direction) }}：{{ formatMoney(entry.amount) }}</text>
        <text class="meta">{{ sourceLabel(entry.sourceType) }} / {{ formatTime(entry.occurredAt) }}</text>
        <text v-if="entry.banquetId" class="meta">宴席 ID：{{ entry.banquetId }}</text>
        <text v-if="entry.note" class="note">{{ entry.note }}</text>
      </view>
    </view>
  </view>
  <view class="page" v-else>加载中</view>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { request } from '../../../api/client';

interface FavorDetail {
  contact: { contactName: string };
  receivedAmount: number;
  givenAmount: number;
  balance: number;
  entries: Array<{ id: number; direction: string; amount: number; sourceType: string; banquetId?: number; occurredAt?: string; note?: string }>;
}

const detail = ref<FavorDetail>();

function formatTime(value?: string) {
  return value ? value.replace('T', ' ') : '';
}

function formatMoney(value: unknown) {
  return `¥${Number(value || 0).toFixed(2)}`;
}

function directionLabel(value: string) {
  return value === 'GIVEN' ? '我送他的' : '他送我的';
}

function sourceLabel(value: string) {
  const labels: Record<string, string> = {
    ONLINE_GIFT: '线上随礼',
    ONSITE_QR: '现场扫码',
    CASH: '现金记礼',
    MANUAL: '手动补录'
  };
  return labels[value] || value;
}

function balanceText(value: unknown) {
  const amount = Number(value || 0);
  if (amount > 0) {
    return '对方累计送入更多';
  }
  if (amount < 0) {
    return '我方累计送出更多';
  }
  return '双方往来持平';
}

function balanceClass(value: unknown) {
  const amount = Number(value || 0);
  if (amount > 0) {
    return 'positive';
  }
  if (amount < 0) {
    return 'negative';
  }
  return 'neutral';
}

onMounted(async () => {
  const pages = getCurrentPages();
  const current = pages[pages.length - 1] as unknown as { options?: Record<string, string> };
  const id = current.options?.id;
  if (id) {
    detail.value = await request<FavorDetail>(`/favor/contacts/${id}`);
  }
});
</script>

<style scoped>
.page { padding: 24rpx; }
.title { display: block; margin-bottom: 24rpx; font-size: 40rpx; font-weight: 600; }
.stats { display: grid; gap: 16rpx; margin-bottom: 24rpx; padding: 20rpx; border: 1px solid #e5e7eb; border-radius: 8rpx; background: #fff; }
.stats-main { display: grid; gap: 6rpx; }
.stats-label { color: #64748b; font-size: 24rpx; }
.stats-amount { font-size: 44rpx; font-weight: 700; }
.stats-note { color: #64748b; font-size: 24rpx; }
.stats-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10rpx; color: #374151; font-size: 24rpx; }
.empty { padding: 36rpx 20rpx; border: 1px dashed #d1d5db; border-radius: 8rpx; color: #64748b; text-align: center; }
.row { padding: 18rpx 0; border-bottom: 1px solid #eee; }
.name, .meta, .note { display: block; }
.meta { margin-top: 6rpx; color: #666; font-size: 24rpx; }
.note { margin-top: 10rpx; padding: 10rpx 12rpx; border-radius: 8rpx; background: #f8fafc; color: #374151; font-size: 24rpx; line-height: 1.5; }
.positive { color: #b91c1c; }
.negative { color: #2563eb; }
.neutral { color: #64748b; }
</style>
