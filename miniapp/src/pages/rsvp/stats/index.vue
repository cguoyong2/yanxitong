<template>
  <view class="page">
    <text class="title">RSVP 统计</text>
    <view v-if="stats" class="grid">
      <view>回执数：{{ stats.totalRecords }}</view>
      <view>参加记录：{{ stats.attendingRecords }}</view>
      <view>待定记录：{{ stats.pendingRecords }}</view>
      <view>不参加记录：{{ stats.declinedRecords }}</view>
      <view>参加人数：{{ stats.totalGuests }}</view>
      <view>用餐人数：{{ stats.mealRequiredGuests }}</view>
      <view>住宿人数：{{ stats.accommodationRequiredGuests }}</view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { request } from '../../../api/client';

interface RsvpStats {
  totalRecords: number;
  attendingRecords: number;
  pendingRecords: number;
  declinedRecords: number;
  totalGuests: number;
  mealRequiredGuests: number;
  accommodationRequiredGuests: number;
}

const stats = ref<RsvpStats>();

onMounted(async () => {
  const pages = getCurrentPages();
  const current = pages[pages.length - 1] as unknown as { options?: Record<string, string> };
  const banquetId = current.options?.banquetId;
  if (banquetId) {
    stats.value = await request<RsvpStats>(`/rsvp/stats?banquetId=${banquetId}`);
  }
});
</script>

<style scoped>
.page { padding: 24rpx; }
.title { display: block; margin-bottom: 24rpx; font-size: 40rpx; font-weight: 600; }
.grid { display: flex; flex-direction: column; gap: 16rpx; }
</style>
