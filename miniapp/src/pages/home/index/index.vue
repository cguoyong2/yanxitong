<template>
  <view class="page">
    <view class="hero">
      <text class="kicker">宴席通</text>
      <text class="title">非支付体验版</text>
      <text class="subtitle">先验证宴席、请柬、回执、线下记礼和后台查看流程。</text>
    </view>

    <view class="notice">
      <text class="notice-title">当前测试范围</text>
      <text class="notice-text">线上随礼和现场扫码暂未开放，请使用线下记礼完成收礼流程。</text>
    </view>

    <view class="actions">
      <button type="primary" @click="createBanquet">创建宴席</button>
      <button @click="refresh" :loading="loading">刷新宴席</button>
    </view>

    <view class="section">
      <view class="section-head">
        <text class="section-title">最近宴席</text>
        <text class="section-meta">{{ banquets.length }} 条</text>
      </view>
      <view v-if="loading" class="empty">加载中</view>
      <view v-else-if="banquets.length === 0" class="empty">暂无宴席，先创建一个</view>
      <view
        v-for="item in banquets"
        :key="item.id"
        class="banquet-card"
        @click="openBanquet(item.id)"
      >
        <view>
          <text class="banquet-name">{{ item.name }}</text>
          <text class="banquet-meta">{{ item.eventTypeCode }} / {{ item.themeCode }}</text>
          <text class="banquet-meta">{{ formatTime(item.banquetTime) }} · {{ item.location || '地点待定' }}</text>
        </view>
        <text class="chevron">›</text>
      </view>
    </view>

    <view class="section">
      <text class="section-title">验收入口</text>
      <view class="shortcut-grid">
        <button size="mini" @click="openFavor">人情账本</button>
        <button size="mini" @click="openLatestRsvpStats" :disabled="!latestBanquetId">回执统计</button>
        <button size="mini" @click="openLatestOfflineGift" :disabled="!latestBanquetId">线下记礼</button>
        <button size="mini" @click="openLatestGiftList" :disabled="!latestBanquetId">收礼记录</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { request } from '../../../api/client';

interface Banquet {
  id: number;
  name: string;
  eventTypeCode: string;
  themeCode: string;
  banquetTime?: string;
  location?: string;
}

const banquets = ref<Banquet[]>([]);
const loading = ref(false);
const latestBanquetId = computed(() => banquets.value[0]?.id || 0);

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '时间待定';
}

async function refresh() {
  loading.value = true;
  try {
    banquets.value = await request<Banquet[]>('/banquets');
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '加载失败', icon: 'none' });
  } finally {
    loading.value = false;
  }
}

function createBanquet() {
  uni.navigateTo({ url: '/pages/banquet/create/index' });
}

function openBanquet(id: number) {
  uni.navigateTo({ url: `/pages/banquet/detail/index?id=${id}` });
}

function openFavor() {
  uni.navigateTo({ url: '/pages/favor/index/index' });
}

function openLatestRsvpStats() {
  if (latestBanquetId.value) {
    uni.navigateTo({ url: `/pages/rsvp/stats/index?banquetId=${latestBanquetId.value}` });
  }
}

function openLatestOfflineGift() {
  if (latestBanquetId.value) {
    uni.navigateTo({ url: `/pages/gift/offline/index?banquetId=${latestBanquetId.value}` });
  }
}

function openLatestGiftList() {
  if (latestBanquetId.value) {
    uni.navigateTo({ url: `/pages/gift/list/index?banquetId=${latestBanquetId.value}` });
  }
}

onMounted(refresh);
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 28rpx;
  background: #f8fafc;
  color: #111827;
}

.hero {
  display: grid;
  gap: 12rpx;
  margin-bottom: 22rpx;
  padding: 34rpx 30rpx;
  border-radius: 8rpx;
  background: #111827;
  color: #fff;
}

.kicker {
  color: #bfdbfe;
  font-size: 24rpx;
}

.title {
  font-size: 44rpx;
  font-weight: 700;
}

.subtitle {
  color: #d1d5db;
  font-size: 26rpx;
  line-height: 1.55;
}

.notice,
.section {
  margin-bottom: 22rpx;
  padding: 24rpx;
  border: 1rpx solid #e5e7eb;
  border-radius: 8rpx;
  background: #fff;
}

.notice {
  border-color: #fed7aa;
  background: #fff7ed;
}

.notice-title,
.section-title {
  display: block;
  font-weight: 700;
}

.notice-text {
  display: block;
  margin-top: 10rpx;
  color: #9a3412;
  font-size: 26rpx;
  line-height: 1.6;
}

.actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
  margin-bottom: 22rpx;
}

.actions button {
  margin: 0;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16rpx;
}

.section-meta,
.banquet-meta,
.empty {
  color: #64748b;
  font-size: 24rpx;
}

.empty {
  padding: 24rpx 0;
  text-align: center;
}

.banquet-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  padding: 20rpx 0;
  border-top: 1rpx solid #eef2f7;
}

.banquet-name {
  display: block;
  margin-bottom: 8rpx;
  font-weight: 700;
}

.banquet-meta {
  display: block;
  margin-top: 4rpx;
}

.chevron {
  color: #94a3b8;
  font-size: 44rpx;
}

.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14rpx;
  margin-top: 18rpx;
}

.shortcut-grid button {
  margin: 0;
}
</style>
