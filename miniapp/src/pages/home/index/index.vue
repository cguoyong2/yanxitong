<template>
  <view class="page">
    <view class="hero">
      <view class="hero-top">
        <view>
          <text class="brand">宴席通</text>
          <text class="title">宴席收礼与回执管家</text>
        </view>
        <text class="status-pill">体验版</text>
      </view>
      <text class="subtitle">创建宴席、分享请柬、收集回执，现场礼金和人情往来统一管理。</text>
      <view class="hero-actions">
        <button class="primary-action" @click="createBanquet">创建宴席</button>
        <button class="ghost-action" @click="refresh" :loading="loading">刷新</button>
      </view>
    </view>

    <view class="summary-grid">
      <view class="summary-card">
        <text class="summary-value">{{ banquets.length }}</text>
        <text class="summary-label">宴席</text>
      </view>
      <view class="summary-card">
        <text class="summary-value">{{ latestEventType }}</text>
        <text class="summary-label">最近类型</text>
      </view>
      <view class="summary-card">
        <text class="summary-value">未开放</text>
        <text class="summary-label">在线支付</text>
      </view>
    </view>

    <view class="notice">
      <text class="notice-title">体验版说明</text>
      <text class="notice-text">当前先测试请柬、回执、线下记礼和后台查看；线上随礼和现场扫码付款将在真实微信支付配置后开放。</text>
    </view>

    <view class="section">
      <view class="section-head">
        <text class="section-title">常用操作</text>
      </view>
      <view class="action-grid">
        <button class="tool-button primary-tool" @click="createBanquet">
          <text class="tool-title">创建宴席</text>
          <text class="tool-desc">类型、主题、请柬</text>
        </button>
        <button class="tool-button" @click="openLatestRsvpStats" :disabled="!latestBanquetId">
          <text class="tool-title">回执统计</text>
          <text class="tool-desc">宾客与人数</text>
        </button>
        <button class="tool-button" @click="openLatestOfflineGift" :disabled="!latestBanquetId">
          <text class="tool-title">线下记礼</text>
          <text class="tool-desc">现金礼金登记</text>
        </button>
        <button class="tool-button" @click="openFavor">
          <text class="tool-title">人情账本</text>
          <text class="tool-desc">往来对比</text>
        </button>
      </view>
    </view>

    <view class="section">
      <view class="section-head">
        <text class="section-title">最近宴席</text>
        <text class="section-meta">{{ loading ? '同步中' : `${banquets.length} 条` }}</text>
      </view>
      <view v-if="loading" class="empty">正在同步宴席</view>
      <view v-else-if="banquets.length === 0" class="empty">暂无宴席，先创建一个宴席</view>
      <view
        v-for="item in banquets"
        :key="item.id"
        class="banquet-card"
        @click="openBanquet(item.id)"
      >
        <view class="banquet-main">
          <view class="banquet-title-row">
            <text class="banquet-name">{{ item.name }}</text>
            <text class="banquet-tag">{{ eventTypeLabel(item.eventTypeCode) }}</text>
          </view>
          <text class="banquet-meta">{{ formatTime(item.banquetTime) }}</text>
          <text class="banquet-meta">{{ item.location || '地点待定' }}</text>
        </view>
        <text class="chevron">›</text>
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
const latestEventType = computed(() => eventTypeLabel(banquets.value[0]?.eventTypeCode || ''));

function eventTypeLabel(code: string) {
  const labels: Record<string, string> = {
    WEDDING: '婚宴',
    BIRTHDAY: '寿宴',
    BABY: '满月',
    HOUSEWARMING: '乔迁',
    SCHOOL: '升学',
    MEMORIAL: '追思',
    OTHER: '其他'
  };
  return labels[code] || '待定';
}

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

onMounted(refresh);
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 28rpx;
  background: #f7f2ec;
  color: #172033;
}

.hero {
  padding: 34rpx 30rpx;
  border: 1rpx solid rgba(166, 68, 38, 0.14);
  border-radius: 8rpx;
  background: linear-gradient(135deg, #9f2f22 0%, #c96a2e 52%, #e0b66a 100%);
  color: #fff;
  box-shadow: 0 16rpx 34rpx rgba(127, 43, 27, 0.16);
}

.hero-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20rpx;
}

.brand {
  display: block;
  margin-bottom: 10rpx;
  color: #ffe9bd;
  font-size: 24rpx;
  font-weight: 700;
}

.title {
  display: block;
  font-size: 44rpx;
  font-weight: 800;
  line-height: 1.25;
}

.status-pill {
  flex: 0 0 auto;
  padding: 8rpx 16rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.44);
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.16);
  font-size: 22rpx;
}

.subtitle {
  display: block;
  margin-top: 18rpx;
  color: #fff7e8;
  font-size: 26rpx;
  line-height: 1.62;
}

.hero-actions {
  display: grid;
  grid-template-columns: 1fr 150rpx;
  gap: 16rpx;
  margin-top: 28rpx;
}

.primary-action,
.ghost-action {
  height: 80rpx;
  margin: 0;
  border-radius: 8rpx;
  font-size: 28rpx;
  line-height: 80rpx;
}

.primary-action {
  border: 0;
  background: #fff;
  color: #9f2f22;
  font-weight: 700;
}

.ghost-action {
  border: 1rpx solid rgba(255, 255, 255, 0.58);
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14rpx;
  margin: 20rpx 0;
}

.summary-card,
.notice,
.section {
  border: 1rpx solid #eadfd3;
  border-radius: 8rpx;
  background: #fffdfa;
}

.summary-card {
  min-height: 112rpx;
  padding: 20rpx 12rpx;
  text-align: center;
}

.summary-value,
.summary-label {
  display: block;
}

.summary-value {
  color: #8f2d20;
  font-size: 30rpx;
  font-weight: 800;
}

.summary-label {
  margin-top: 8rpx;
  color: #7b6a5b;
  font-size: 22rpx;
}

.notice {
  margin-bottom: 20rpx;
  padding: 22rpx 24rpx;
  border-color: #efd7b5;
  background: #fff8ec;
}

.notice-title,
.section-title {
  display: block;
  color: #172033;
  font-weight: 800;
}

.notice-text {
  display: block;
  margin-top: 10rpx;
  color: #8a4d20;
  font-size: 25rpx;
  line-height: 1.6;
}

.section {
  margin-bottom: 20rpx;
  padding: 24rpx;
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
  color: #756a61;
  font-size: 24rpx;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14rpx;
}

.tool-button {
  height: auto;
  margin: 0;
  padding: 22rpx 18rpx;
  border: 1rpx solid #eadfd3;
  border-radius: 8rpx;
  background: #fff;
  color: #172033;
  line-height: 1.35;
  text-align: left;
}

.tool-button[disabled] {
  opacity: 0.52;
}

.primary-tool {
  border-color: rgba(159, 47, 34, 0.22);
  background: #fff4ed;
}

.tool-title,
.tool-desc {
  display: block;
}

.tool-title {
  font-size: 28rpx;
  font-weight: 800;
}

.tool-desc {
  margin-top: 8rpx;
  color: #827266;
  font-size: 22rpx;
}

.empty {
  padding: 28rpx 0;
  text-align: center;
}

.banquet-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  padding: 22rpx 0;
  border-top: 1rpx solid #f0e6dc;
}

.banquet-main {
  min-width: 0;
}

.banquet-title-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 8rpx;
}

.banquet-name {
  overflow: hidden;
  color: #172033;
  font-size: 30rpx;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.banquet-tag {
  flex: 0 0 auto;
  padding: 5rpx 12rpx;
  border-radius: 999rpx;
  background: #f2e8dd;
  color: #8a4d20;
  font-size: 20rpx;
}

.banquet-meta {
  display: block;
  margin-top: 4rpx;
}

.chevron {
  color: #b7a99a;
  font-size: 44rpx;
}
</style>
