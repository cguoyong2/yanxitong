<template>
  <view class="page">
    <view class="brand-top">
      <view>
        <text class="brand">宴席通</text>
        <text class="headline">办宴席，用宴席通</text>
        <text class="subline">创建宴席、分享请柬、收集回执、现场礼金和人情往来统一管理。</text>
      </view>
      <view class="badge">体验版</view>
    </view>

    <view class="banner">
      <view class="banner-art">
        <text class="banner-knot">囍</text>
        <text class="banner-flower">花</text>
      </view>
      <view class="banner-copy">
        <text class="banner-eyebrow">宴席管理</text>
        <text class="banner-title">轻松办好每一场宴席</text>
        <text class="banner-text">从创建到回执、收礼、账本，按流程推进。</text>
      </view>
    </view>

    <view class="primary-actions">
      <button class="create-btn" @tap="createBanquet()">+ 创建宴席</button>
      <button class="ghost-btn" :loading="loading" @tap="refresh()">刷新</button>
    </view>

    <view class="stats-row">
      <view class="stat-card">
        <text class="stat-value">{{ banquets.length }}</text>
        <text class="stat-label">宴席</text>
      </view>
      <view class="stat-card">
        <text class="stat-value">{{ latestEventType }}</text>
        <text class="stat-label">最近类型</text>
      </view>
      <view class="stat-card">
        <text class="stat-value">未开放</text>
        <text class="stat-label">在线支付</text>
      </view>
    </view>

    <view class="state-card">
      <view class="section-head">
        <view>
          <text class="section-title">{{ hasBanquet ? '我的宴席' : '还没有宴席' }}</text>
          <text class="section-desc">{{ hasBanquet ? '继续管理最近一场宴席' : '先创建一场宴席，生成请柬与回执入口' }}</text>
        </view>
        <text class="count">{{ loading ? '同步中' : `${banquets.length} 条` }}</text>
      </view>

      <view v-if="loading" class="empty">正在同步宴席数据</view>
      <view v-else-if="!hasBanquet" class="empty-state">
        <text class="empty-title">创建后即可开始邀请宾客</text>
        <text class="empty-text">体验版先开放创建宴席、回执、线下记礼和后台查看。</text>
        <button class="mini-primary" @tap="createBanquet()">马上创建</button>
      </view>
      <view v-else class="latest-card" @tap="openBanquet(latestBanquet.id)">
        <view class="latest-top">
          <view>
            <text class="latest-name">{{ latestBanquet.name }}</text>
            <text class="latest-meta">{{ formatTime(latestBanquet.banquetTime) }}</text>
            <text class="latest-meta">{{ latestBanquet.location || '地点待定' }}</text>
          </view>
          <text class="latest-tag">{{ eventTypeLabel(latestBanquet.eventTypeCode) }}</text>
        </view>
        <view class="latest-actions">
          <button @tap.stop="openInvite(latestBanquet.id)">发请柬</button>
          <button @tap.stop="openRsvp(latestBanquet.id)">回执统计</button>
          <button @tap.stop="openOfflineGift(latestBanquet.id)">收礼记账</button>
        </view>
      </view>
    </view>

    <view class="tool-card">
      <text class="section-title">办席工具</text>
      <view class="tool-grid">
        <view class="tool-item active" @tap="createBanquet()">
          <text class="tool-icon">办</text>
          <text class="tool-title">创建宴席</text>
          <text class="tool-desc">类型、主题、请柬</text>
        </view>
        <view class="tool-item" @tap="openLatestRsvpStats()">
          <text class="tool-icon">回</text>
          <text class="tool-title">回执统计</text>
          <text class="tool-desc">宾客与人数</text>
        </view>
        <view class="tool-item" @tap="openLatestOfflineGift()">
          <text class="tool-icon">礼</text>
          <text class="tool-title">线下记礼</text>
          <text class="tool-desc">现金礼金登记</text>
        </view>
        <view class="tool-item" @tap="openFavorTab()">
          <text class="tool-icon">账</text>
          <text class="tool-title">人情账本</text>
          <text class="tool-desc">往来对比</text>
        </view>
      </view>
    </view>

    <view class="recent-card">
      <view class="section-head">
        <text class="section-title">最近宴席</text>
        <text class="section-desc">点击进入管理台</text>
      </view>
      <view
        v-for="item in banquets"
        :key="item.id"
        class="banquet-row"
        @tap="openBanquet(item.id)"
      >
        <view class="avatar">{{ eventTypeLabel(item.eventTypeCode).slice(0, 1) }}</view>
        <view class="row-main">
          <text class="row-title">{{ item.name }}</text>
          <text class="row-meta">{{ formatTime(item.banquetTime) }} · {{ item.location || '地点待定' }}</text>
        </view>
        <text class="arrow">›</text>
      </view>
      <view v-if="!loading && banquets.length === 0" class="empty">暂无宴席</view>
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
const hasBanquet = computed(() => banquets.value.length > 0);
const latestBanquet = computed(() => banquets.value[0]);
const latestBanquetId = computed(() => latestBanquet.value?.id || 0);
const latestEventType = computed(() => eventTypeLabel(latestBanquet.value?.eventTypeCode || ''));

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

function openInvite(id: number) {
  uni.navigateTo({ url: `/pages/invite/edit-basic/index?banquetId=${id}` });
}

function openRsvp(id: number) {
  uni.navigateTo({ url: `/pages/rsvp/stats/index?banquetId=${id}` });
}

function openOfflineGift(id: number) {
  uni.navigateTo({ url: `/pages/gift/offline/index?banquetId=${id}` });
}

function openLatestRsvpStats() {
  if (!latestBanquetId.value) {
    uni.showToast({ title: '请先创建宴席', icon: 'none' });
    return;
  }
  openRsvp(latestBanquetId.value);
}

function openLatestOfflineGift() {
  if (!latestBanquetId.value) {
    uni.showToast({ title: '请先创建宴席', icon: 'none' });
    return;
  }
  openOfflineGift(latestBanquetId.value);
}

function openFavorTab() {
  uni.switchTab({ url: '/pages/favor/index/index' });
}

onMounted(refresh);
</script>

<style scoped>
.page {
  box-sizing: border-box;
  min-height: 100vh;
  padding: 24rpx;
  background: #f7f7f7;
  color: #151823;
}

.brand-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24rpx;
  padding: 28rpx 28rpx 96rpx;
  border-radius: 0 0 28rpx 28rpx;
  background: linear-gradient(135deg, #e60012 0%, #b80000 62%, #8f0008 100%);
  color: #fff;
}

.brand,
.headline,
.subline,
.banner-eyebrow,
.banner-title,
.banner-text,
.section-title,
.section-desc,
.latest-name,
.latest-meta,
.row-title,
.row-meta {
  display: block;
}

.brand {
  font-size: 26rpx;
  font-weight: 700;
  color: #ffe8bf;
}

.headline {
  margin-top: 18rpx;
  font-size: 43rpx;
  font-weight: 800;
  line-height: 1.18;
}

.subline {
  margin-top: 14rpx;
  max-width: 520rpx;
  color: rgba(255, 255, 255, 0.88);
  font-size: 25rpx;
  line-height: 1.6;
}

.badge {
  flex: 0 0 auto;
  padding: 9rpx 16rpx;
  border: 1rpx solid rgba(255, 232, 190, 0.72);
  border-radius: 999rpx;
  color: #fff2cf;
  font-size: 24rpx;
}

.banner {
  position: relative;
  z-index: 2;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  margin-top: -72rpx;
  padding: 28rpx;
  border-radius: 16rpx;
  background:
    radial-gradient(circle at 78% 28%, rgba(255, 231, 169, 0.34), transparent 29%),
    linear-gradient(138deg, #fff7e8 0%, #ffe4c3 42%, #df3b25 100%);
  box-shadow: 0 18rpx 40rpx rgba(184, 0, 0, 0.16);
}

.banner-art {
  position: absolute;
  inset: 0;
  color: rgba(184, 0, 0, 0.08);
  font-weight: 900;
}

.banner-knot {
  position: absolute;
  right: 42rpx;
  top: 34rpx;
  color: rgba(184, 0, 0, 0.16);
  font-size: 120rpx;
}

.banner-flower {
  position: absolute;
  right: 34rpx;
  bottom: 16rpx;
  color: rgba(255, 255, 255, 0.24);
  font-size: 98rpx;
}

.banner-copy {
  position: relative;
  max-width: 410rpx;
}

.banner-eyebrow {
  color: #b80000;
  font-size: 24rpx;
  font-weight: 700;
}

.banner-title {
  margin-top: 16rpx;
  color: #7e130b;
  font-size: 42rpx;
  font-weight: 800;
  line-height: 1.15;
}

.banner-text {
  margin-top: 12rpx;
  color: #8a5a37;
  font-size: 24rpx;
  line-height: 1.5;
}

.primary-actions {
  display: grid;
  grid-template-columns: 1fr 160rpx;
  gap: 14rpx;
  margin-top: 20rpx;
}

button {
  margin: 0;
  padding: 0;
  border: 0;
  border-radius: 12rpx;
  font-size: 28rpx;
  line-height: 86rpx;
}

button::after {
  border: 0;
}

.create-btn {
  background: linear-gradient(135deg, #e60012, #c71916);
  color: #fff;
  font-weight: 800;
  box-shadow: 0 14rpx 24rpx rgba(230, 0, 18, 0.18);
}

.ghost-btn {
  border: 1rpx solid #f0d4bd;
  background: #fff8ef;
  color: #a54b26;
  font-weight: 700;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14rpx;
  margin-top: 18rpx;
}

.stat-card {
  padding: 24rpx 12rpx;
  border: 1rpx solid #eeeeee;
  border-radius: 12rpx;
  background: #fff;
  text-align: center;
  box-shadow: 0 10rpx 24rpx rgba(30, 18, 12, 0.04);
}

.stat-value {
  display: block;
  color: #c71916;
  font-size: 31rpx;
  font-weight: 800;
}

.stat-label {
  display: block;
  margin-top: 8rpx;
  color: #7a7f8c;
  font-size: 23rpx;
}

.state-card,
.tool-card,
.recent-card {
  margin-top: 20rpx;
  padding: 24rpx;
  border: 1rpx solid #eeeeee;
  border-radius: 12rpx;
  background: #fff;
  box-shadow: 0 10rpx 24rpx rgba(30, 18, 12, 0.04);
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18rpx;
}

.section-title {
  color: #161b2a;
  font-size: 31rpx;
  font-weight: 800;
}

.section-desc,
.count {
  margin-top: 8rpx;
  color: #7a7f8c;
  font-size: 23rpx;
}

.empty,
.empty-state {
  margin-top: 18rpx;
  padding: 26rpx;
  border: 1rpx dashed #f0d4bd;
  border-radius: 12rpx;
  background: #fffaf4;
  color: #8b6250;
  font-size: 25rpx;
  text-align: center;
}

.empty-title,
.empty-text {
  display: block;
}

.empty-title {
  color: #151823;
  font-size: 29rpx;
  font-weight: 800;
}

.empty-text {
  margin-top: 10rpx;
  color: #7a7f8c;
  font-size: 24rpx;
  line-height: 1.55;
}

.mini-primary {
  width: 260rpx;
  margin: 22rpx auto 0;
  border-radius: 999rpx;
  background: #e60012;
  color: #fff;
  line-height: 72rpx;
}

.latest-card {
  margin-top: 20rpx;
  padding: 22rpx;
  border-radius: 12rpx;
  background: linear-gradient(180deg, #fff9f0, #fff);
  border: 1rpx solid #f2dfce;
}

.latest-top {
  display: flex;
  justify-content: space-between;
  gap: 18rpx;
}

.latest-name {
  color: #151823;
  font-size: 31rpx;
  font-weight: 800;
}

.latest-meta {
  margin-top: 8rpx;
  color: #7a7f8c;
  font-size: 24rpx;
}

.latest-tag {
  align-self: flex-start;
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  background: #ffe9d0;
  color: #b35d1f;
  font-size: 22rpx;
  font-weight: 700;
}

.latest-actions {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12rpx;
  margin-top: 18rpx;
}

.latest-actions button {
  border: 1rpx solid #f0d4bd;
  background: #fff;
  color: #a54b26;
  font-size: 23rpx;
  font-weight: 700;
  line-height: 64rpx;
}

.tool-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14rpx;
  margin-top: 18rpx;
}

.tool-item {
  padding: 20rpx;
  border: 1rpx solid #eeeeee;
  border-radius: 12rpx;
  background: #fff;
}

.tool-item.active {
  border-color: #f0c9c2;
  background: #fff4f2;
}

.tool-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 46rpx;
  height: 46rpx;
  border-radius: 50%;
  background: #f9e9e7;
  color: #c71916;
  font-size: 23rpx;
  font-weight: 800;
}

.tool-title,
.tool-desc {
  display: block;
}

.tool-title {
  margin-top: 12rpx;
  color: #161b2a;
  font-size: 27rpx;
  font-weight: 800;
}

.tool-desc {
  margin-top: 6rpx;
  color: #7a7f8c;
  font-size: 22rpx;
}

.banquet-row {
  display: flex;
  align-items: center;
  gap: 18rpx;
  padding: 22rpx 0;
  border-bottom: 1rpx solid #eeeeee;
}

.banquet-row:last-child {
  border-bottom: 0;
}

.avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 58rpx;
  height: 58rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #ef6a62, #d8271f);
  color: #fff;
  font-weight: 800;
}

.row-main {
  flex: 1;
  min-width: 0;
}

.row-title {
  overflow: hidden;
  color: #151823;
  font-size: 28rpx;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row-meta {
  overflow: hidden;
  margin-top: 8rpx;
  color: #7a7f8c;
  font-size: 23rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.arrow {
  color: #b6bbc7;
  font-size: 42rpx;
}
</style>
