<template>
  <view class="page">
    <view class="hero-card">
      <view class="hero-ornament lantern"></view>
      <view class="hero-ornament flower"></view>
      <text class="hero-knot">囍</text>
      <text class="hero-brand">宴席通</text>
      <text class="hero-title">办宴席，用宴席通</text>
      <text class="hero-subtitle">轻松办好每一场宴席</text>
      <view class="hero-divider">
        <text></text>
        <view class="divider-dot"></view>
        <text></text>
      </view>
    </view>

    <button class="create-button" @tap="createBanquet()">
      <text class="plus">+</text>
      <text>创建宴席</text>
    </button>

    <view class="stats-grid">
      <view class="stats-card" @tap="openLatestOrCreate()">
        <view class="stats-icon table">桌</view>
        <text class="stats-label">宴席数</text>
        <text class="stats-value">{{ banquets.length }}</text>
      </view>
      <view class="stats-card" @tap="openLatestRsvpStats()">
        <view class="stats-icon gift">礼</view>
        <text class="stats-label">累计礼金</text>
        <text class="stats-value">¥12,800</text>
      </view>
      <view class="stats-card" @tap="openLatestRsvpStats()">
        <view class="stats-icon people">人</view>
        <text class="stats-label">回执人数</text>
        <text class="stats-value">86</text>
      </view>
    </view>

    <view v-if="!hasBanquet && !loading" class="empty-banquet-card">
      <text class="empty-title">还没有宴席</text>
      <text class="empty-desc">先创建宴席，系统会根据宴席类型自动推荐主题、请柬和回执入口。</text>
      <button class="empty-button" @tap="createBanquet()">立即创建</button>
    </view>

    <view v-if="hasBanquet" class="current-card">
      <view class="section-head">
        <view>
          <text class="section-title">我的宴席</text>
          <text class="section-subtitle">最近一场宴席</text>
        </view>
        <text class="status-pill">已创建</text>
      </view>
      <view class="current-main" @tap="openBanquet(latestBanquet.id)">
        <view class="current-mark">{{ eventTypeLabel(latestBanquet.eventTypeCode).slice(0, 1) }}</view>
        <view class="current-info">
          <text class="current-name">{{ latestBanquet.name }}</text>
          <text class="current-meta">{{ formatTime(latestBanquet.banquetTime) }}</text>
          <text class="current-meta">{{ latestBanquet.location || '地点待定' }}</text>
        </view>
        <text class="arrow">›</text>
      </view>
    </view>

    <view class="guide-card">
      <view class="section-head">
        <view>
          <text class="section-title">开席引导</text>
          <text class="section-subtitle">按步骤完成核心流程</text>
        </view>
        <button class="refresh" :loading="loading" @tap="refresh()">刷新</button>
      </view>
      <view class="guide-list">
        <view class="guide-row" @tap="createBanquet()">
          <text class="guide-step">1</text>
          <view class="guide-copy">
            <text class="guide-title">创建宴席</text>
            <text class="guide-desc">填写宴席信息，选择类型和主题</text>
          </view>
          <text class="arrow">›</text>
        </view>
        <view class="guide-row" @tap="openInvitationTab()">
          <text class="guide-step">2</text>
          <view class="guide-copy">
            <text class="guide-title">发送请柬 / 收回执</text>
            <text class="guide-desc">预览公开页，邀请宾客确认出席</text>
          </view>
          <text class="arrow">›</text>
        </view>
        <view class="guide-row" @tap="openLatestOfflineGift()">
          <text class="guide-step">3</text>
          <view class="guide-copy">
            <text class="guide-title">现场收礼确认</text>
            <text class="guide-desc">现金记礼，自动沉淀到人情账本</text>
          </view>
          <text class="arrow">›</text>
        </view>
        <view class="guide-row" @tap="openFavorTab()">
          <text class="guide-step">4</text>
          <view class="guide-copy">
            <text class="guide-title">人情往来管理</text>
            <text class="guide-desc">查看收送记录和往来差额</text>
          </view>
          <text class="arrow">›</text>
        </view>
      </view>
    </view>

    <view class="notice-card">
      <text class="notice-title">体验版说明</text>
      <text class="notice-text">当前先验证创建宴席、请柬、回执、线下记礼和后台查看；线上随礼与现场扫码付款将在真实微信支付配置后开放。</text>
    </view>

    <view class="recent-card">
      <view class="section-head">
        <view>
          <text class="section-title">最近宴席</text>
          <text class="section-subtitle">{{ loading ? '同步中' : `${banquets.length} 条` }}</text>
        </view>
        <text class="small-link" @tap="createBanquet()">新建</text>
      </view>
      <view
        v-for="item in banquets"
        :key="item.id"
        class="banquet-row"
        @tap="openBanquet(item.id)"
      >
        <view class="banquet-avatar">{{ eventTypeLabel(item.eventTypeCode).slice(0, 1) }}</view>
        <view class="banquet-main">
          <text class="banquet-name">{{ item.name }}</text>
          <text class="banquet-meta">{{ formatTime(item.banquetTime) }} · {{ item.location || '地点待定' }}</text>
        </view>
        <text class="arrow">›</text>
      </view>
      <view v-if="!loading && banquets.length === 0" class="empty-row">暂无宴席记录</view>
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
  return labels[code] || '宴席';
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

function openLatestOrCreate() {
  if (latestBanquetId.value) {
    openBanquet(latestBanquetId.value);
    return;
  }
  createBanquet();
}

function openLatestRsvpStats() {
  if (!latestBanquetId.value) {
    uni.showToast({ title: '请先创建宴席', icon: 'none' });
    return;
  }
  uni.navigateTo({ url: `/pages/rsvp/stats/index?banquetId=${latestBanquetId.value}` });
}

function openLatestOfflineGift() {
  if (!latestBanquetId.value) {
    uni.showToast({ title: '请先创建宴席', icon: 'none' });
    return;
  }
  uni.navigateTo({ url: `/pages/gift/offline/index?banquetId=${latestBanquetId.value}` });
}

function openFavorTab() {
  uni.switchTab({ url: '/pages/favor/index/index' });
}

function openInvitationTab() {
  uni.switchTab({ url: '/pages/invitation/index/index' });
}

onMounted(refresh);
</script>

<style scoped>
.page {
  box-sizing: border-box;
  min-height: 100vh;
  padding: 24rpx 24rpx 32rpx;
  background:
    linear-gradient(180deg, #fffaf4 0, #fffaf4 340rpx, #f7f7f7 560rpx),
    #f7f7f7;
  color: #151823;
}

.hero-card {
  position: relative;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  padding: 34rpx 32rpx;
  border-radius: 16rpx;
  background:
    radial-gradient(circle at 75% 20%, rgba(255, 229, 160, 0.28), transparent 28%),
    radial-gradient(circle at 10% 110%, rgba(255, 195, 102, 0.18), transparent 35%),
    linear-gradient(135deg, #e8372d 0%, #d71318 45%, #b80000 100%);
  box-shadow: 0 20rpx 42rpx rgba(184, 0, 0, 0.18);
}

.hero-card::before,
.hero-card::after {
  position: absolute;
  border: 1rpx solid rgba(255, 232, 190, 0.22);
  border-radius: 50%;
  content: '';
}

.hero-card::before {
  right: -80rpx;
  top: -60rpx;
  width: 260rpx;
  height: 260rpx;
}

.hero-card::after {
  right: 38rpx;
  bottom: -106rpx;
  width: 260rpx;
  height: 260rpx;
}

.hero-ornament {
  position: absolute;
  pointer-events: none;
}

.lantern {
  top: 28rpx;
  left: 30rpx;
  width: 58rpx;
  height: 72rpx;
  border: 3rpx solid rgba(255, 236, 186, 0.82);
  border-radius: 50% 50% 44% 44%;
  background: linear-gradient(135deg, #ffdf9c, #e53828 52%, #a80000);
  box-shadow: inset 0 0 0 10rpx rgba(255, 236, 186, 0.12);
}

.lantern::before,
.lantern::after {
  position: absolute;
  left: 50%;
  width: 2rpx;
  background: rgba(255, 236, 186, 0.8);
  transform: translateX(-50%);
  content: '';
}

.lantern::before {
  top: -32rpx;
  height: 32rpx;
}

.lantern::after {
  bottom: -24rpx;
  height: 24rpx;
}

.flower {
  right: 22rpx;
  bottom: 22rpx;
  width: 136rpx;
  height: 94rpx;
  border-radius: 70% 40% 70% 50%;
  background:
    radial-gradient(circle at 70% 44%, rgba(255, 236, 186, 0.68) 0 15rpx, transparent 16rpx),
    radial-gradient(circle at 45% 52%, rgba(255, 202, 124, 0.58) 0 22rpx, transparent 23rpx),
    radial-gradient(circle at 25% 66%, rgba(255, 236, 186, 0.42) 0 18rpx, transparent 19rpx);
  opacity: 0.8;
}

.hero-knot {
  position: absolute;
  left: 42rpx;
  bottom: 8rpx;
  color: rgba(255, 236, 186, 0.13);
  font-size: 150rpx;
  font-weight: 900;
  line-height: 1;
}

.hero-brand,
.hero-title,
.hero-subtitle,
.section-title,
.section-subtitle,
.stats-label,
.stats-value,
.empty-title,
.empty-desc,
.current-name,
.current-meta,
.guide-title,
.guide-desc,
.notice-title,
.notice-text,
.banquet-name,
.banquet-meta {
  display: block;
}

.hero-brand {
  position: relative;
  z-index: 1;
  margin-left: 84rpx;
  color: #ffe8bf;
  font-size: 30rpx;
  font-weight: 800;
  letter-spacing: 0;
}

.hero-title {
  position: relative;
  z-index: 1;
  margin-top: 42rpx;
  color: #fff1ca;
  font-size: 48rpx;
  font-weight: 900;
  line-height: 1.12;
  text-align: center;
  text-shadow: 0 6rpx 16rpx rgba(85, 0, 0, 0.22);
}

.hero-subtitle {
  position: relative;
  z-index: 1;
  margin-top: 16rpx;
  color: #fff7df;
  font-size: 27rpx;
  text-align: center;
}

.hero-divider {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14rpx;
  margin: 18rpx auto 0;
}

.hero-divider text {
  display: block;
  width: 86rpx;
  height: 1rpx;
  background: rgba(255, 232, 190, 0.68);
}

.divider-dot {
  width: 12rpx;
  height: 12rpx;
  border: 2rpx solid #ffe8bf;
  border-radius: 50%;
}

button {
  margin: 0;
  padding: 0;
  border: 0;
}

button::after {
  border: 0;
}

.create-button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 18rpx;
  height: 104rpx;
  margin-top: 22rpx;
  border-radius: 14rpx;
  background: linear-gradient(135deg, #e7352b 0%, #cf171b 100%);
  color: #fff;
  font-size: 34rpx;
  font-weight: 900;
  line-height: 104rpx;
  box-shadow: 0 16rpx 28rpx rgba(207, 23, 27, 0.22);
}

.plus {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  background: #fff;
  color: #cf171b;
  font-size: 40rpx;
  line-height: 48rpx;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18rpx;
  margin-top: 22rpx;
}

.stats-card {
  min-height: 158rpx;
  padding: 22rpx 8rpx 18rpx;
  border: 1rpx solid #f0e4dc;
  border-radius: 14rpx;
  background: #fff;
  text-align: center;
  box-shadow: 0 12rpx 28rpx rgba(99, 56, 28, 0.06);
}

.stats-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56rpx;
  height: 56rpx;
  margin: 0 auto 12rpx;
  border-radius: 50%;
  font-size: 24rpx;
  font-weight: 900;
}

.stats-icon.table {
  background: #fff0ee;
  color: #d32620;
}

.stats-icon.gift {
  background: #fff3df;
  color: #c68a31;
}

.stats-icon.people {
  background: #fff0ee;
  color: #d32620;
}

.stats-label {
  color: #7a6f68;
  font-size: 23rpx;
}

.stats-value {
  margin-top: 9rpx;
  color: #c71916;
  font-size: 31rpx;
  font-weight: 900;
  line-height: 1.15;
}

.empty-banquet-card,
.current-card,
.guide-card,
.notice-card,
.recent-card {
  margin-top: 22rpx;
  padding: 24rpx;
  border: 1rpx solid #f0e4dc;
  border-radius: 16rpx;
  background: #fff;
  box-shadow: 0 12rpx 30rpx rgba(99, 56, 28, 0.06);
}

.empty-banquet-card {
  background: linear-gradient(180deg, #fffaf3, #fff);
  text-align: center;
}

.empty-title {
  color: #151823;
  font-size: 32rpx;
  font-weight: 900;
}

.empty-desc {
  margin-top: 10rpx;
  color: #7a6f68;
  font-size: 24rpx;
  line-height: 1.6;
}

.empty-button {
  width: 260rpx;
  height: 72rpx;
  margin: 22rpx auto 0;
  border-radius: 999rpx;
  background: #d32620;
  color: #fff;
  font-size: 26rpx;
  font-weight: 800;
  line-height: 72rpx;
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18rpx;
}

.section-title {
  color: #151823;
  font-size: 31rpx;
  font-weight: 900;
}

.section-subtitle {
  margin-top: 7rpx;
  color: #8a8179;
  font-size: 23rpx;
}

.status-pill,
.small-link {
  flex: 0 0 auto;
  color: #c71916;
  font-size: 24rpx;
  font-weight: 800;
}

.status-pill {
  padding: 8rpx 14rpx;
  border-radius: 999rpx;
  background: #fff0ee;
}

.current-main {
  display: flex;
  align-items: center;
  gap: 18rpx;
  margin-top: 20rpx;
  padding: 20rpx;
  border: 1rpx solid #f3e2d6;
  border-radius: 14rpx;
  background: #fffaf5;
}

.current-mark,
.banquet-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
  border-radius: 50%;
  background: linear-gradient(135deg, #ef6a62, #d32620);
  color: #fff;
  font-weight: 900;
}

.current-mark {
  width: 68rpx;
  height: 68rpx;
  font-size: 30rpx;
}

.current-info,
.banquet-main,
.guide-copy {
  flex: 1;
  min-width: 0;
}

.current-name {
  overflow: hidden;
  color: #151823;
  font-size: 29rpx;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.current-meta {
  overflow: hidden;
  margin-top: 7rpx;
  color: #7a6f68;
  font-size: 23rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.refresh {
  flex: 0 0 auto;
  width: 112rpx;
  height: 56rpx;
  border: 1rpx solid #f0d4bd;
  border-radius: 999rpx;
  background: #fff8ef;
  color: #9b3e26;
  font-size: 23rpx;
  line-height: 56rpx;
}

.guide-list {
  margin-top: 18rpx;
}

.guide-row {
  display: flex;
  align-items: center;
  gap: 18rpx;
  min-height: 88rpx;
  padding: 18rpx 0;
  border-bottom: 1rpx solid #f1e8df;
}

.guide-row:last-child {
  border-bottom: 0;
}

.guide-step {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #d32620, #a90000);
  color: #fff4cf;
  font-size: 24rpx;
  font-weight: 900;
}

.guide-title {
  color: #1c2231;
  font-size: 28rpx;
  font-weight: 900;
}

.guide-desc {
  margin-top: 6rpx;
  color: #8a8179;
  font-size: 22rpx;
}

.arrow {
  flex: 0 0 auto;
  color: #b7aca4;
  font-size: 42rpx;
}

.notice-card {
  border-color: #f2d4b3;
  background: #fff8ef;
}

.notice-title {
  color: #151823;
  font-size: 29rpx;
  font-weight: 900;
}

.notice-text {
  margin-top: 10rpx;
  color: #9b5427;
  font-size: 24rpx;
  line-height: 1.65;
}

.banquet-row {
  display: flex;
  align-items: center;
  gap: 18rpx;
  padding: 22rpx 0;
  border-bottom: 1rpx solid #f1e8df;
}

.banquet-row:last-child {
  border-bottom: 0;
}

.banquet-avatar {
  width: 58rpx;
  height: 58rpx;
  font-size: 25rpx;
}

.banquet-name {
  overflow: hidden;
  color: #151823;
  font-size: 28rpx;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.banquet-meta {
  overflow: hidden;
  margin-top: 8rpx;
  color: #7a6f68;
  font-size: 23rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-row {
  padding: 34rpx 0 12rpx;
  color: #8a8179;
  font-size: 24rpx;
  text-align: center;
}
</style>
