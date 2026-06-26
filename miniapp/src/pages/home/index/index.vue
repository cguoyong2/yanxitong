<template>
  <view class="page">
    <view class="hero">
      <text class="hero-pattern left">宴</text>
      <text class="hero-pattern right">囍</text>
      <text class="brand">宴席通</text>
      <text class="title">办宴席，用宴席通</text>
      <text class="subtitle">创建请柬、收集回执、登记礼金，人情往来一处管理</text>
      <view class="hero-foot">
        <text>非支付体验版</text>
        <text>请柬 · 回执 · 线下记礼</text>
      </view>
    </view>

    <navigator url="/pages/banquet/create/index" open-type="navigate" class="create-banner">
      <text class="create-plus">+</text>
      <text>创建宴席</text>
    </navigator>

    <view class="metric-grid">
      <view class="metric-card tappable" @tap="openLatestOrCreate">
        <text class="metric-icon">桌</text>
        <text class="metric-label">宴席数</text>
        <text class="metric-value">{{ banquets.length }}</text>
      </view>
      <view class="metric-card tappable" @tap="openLatestRsvpStats">
        <text class="metric-icon">回</text>
        <text class="metric-label">最近类型</text>
        <text class="metric-value compact">{{ latestEventType }}</text>
      </view>
      <view class="metric-card tappable" @tap="showPaymentDisabled">
        <text class="metric-icon">礼</text>
        <text class="metric-label">在线支付</text>
        <text class="metric-value compact">未开放</text>
      </view>
    </view>

    <view class="guide-card">
      <view class="section-head">
        <view class="title-wrap">
          <text class="red-bar"></text>
          <text class="section-title">开席引导</text>
        </view>
        <button class="refresh-btn" size="mini" @tap="refresh" :loading="loading">刷新</button>
      </view>
      <view class="guide-list">
        <navigator url="/pages/banquet/create/index" open-type="navigate" class="guide-item">
          <text class="step">1</text>
          <text class="guide-text">创建宴席</text>
          <text class="guide-arrow">›</text>
        </navigator>
        <view class="guide-item" @tap="openLatestRsvpStats">
          <text class="step">2</text>
          <text class="guide-text">查看回执统计</text>
          <text class="guide-arrow">›</text>
        </view>
        <view class="guide-item" @tap="openLatestOfflineGift">
          <text class="step">3</text>
          <text class="guide-text">线下记礼</text>
          <text class="guide-arrow">›</text>
        </view>
        <view class="guide-item" @tap="openFavor">
          <text class="step">4</text>
          <text class="guide-text">人情账本</text>
          <text class="guide-arrow">›</text>
        </view>
      </view>
    </view>

    <view class="notice" @tap="showPilotScope">
      <text class="notice-title">体验版说明</text>
      <text class="notice-text">当前先测试请柬、回执、线下记礼和后台查看；线上随礼和现场扫码付款将在真实微信支付配置后开放。</text>
    </view>

    <view class="recent-card">
      <view class="section-head">
        <view class="title-wrap">
          <text class="red-bar"></text>
          <text class="section-title">最近宴席</text>
        </view>
        <text class="section-meta">{{ loading ? '同步中' : `${banquets.length} 条` }}</text>
      </view>
      <view v-if="loading" class="empty">正在同步宴席</view>
      <navigator v-else-if="banquets.length === 0" url="/pages/banquet/create/index" open-type="navigate" class="empty-action">
        暂无宴席，立即创建
      </navigator>
      <view
        v-for="item in banquets"
        :key="item.id"
        class="banquet-card"
        @tap="openBanquet(item.id)"
      >
        <view class="banquet-avatar">{{ eventTypeLabel(item.eventTypeCode).slice(0, 1) }}</view>
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

function openLatestOrCreate() {
  if (latestBanquetId.value) {
    openBanquet(latestBanquetId.value);
    return;
  }
  createBanquet();
}

function openFavor() {
  uni.navigateTo({ url: '/pages/favor/index/index' });
}

function openLatestRsvpStats() {
  if (latestBanquetId.value) {
    uni.navigateTo({ url: `/pages/rsvp/stats/index?banquetId=${latestBanquetId.value}` });
    return;
  }
  uni.showToast({ title: '请先创建宴席', icon: 'none' });
}

function openLatestOfflineGift() {
  if (latestBanquetId.value) {
    uni.navigateTo({ url: `/pages/gift/offline/index?banquetId=${latestBanquetId.value}` });
    return;
  }
  uni.showToast({ title: '请先创建宴席', icon: 'none' });
}

function showPaymentDisabled() {
  uni.showToast({ title: '线上支付暂未开放', icon: 'none' });
}

function showPilotScope() {
  uni.showModal({
    title: '体验版说明',
    content: '当前可测试创建宴席、请柬、回执、线下记礼和后台查看。线上随礼和现场扫码付款将在真实微信支付配置后开放。',
    showCancel: false,
    confirmText: '知道了'
  });
}

onMounted(refresh);
</script>

<style scoped>
.page {
  box-sizing: border-box;
  min-height: 100vh;
  padding: 24rpx;
  background: linear-gradient(180deg, #fff7ed 0%, #fffaf5 340rpx, #f7f0e8 100%);
  color: #171923;
}

.hero {
  position: relative;
  min-height: 315rpx;
  overflow: hidden;
  padding: 34rpx 30rpx 30rpx;
  border-radius: 8rpx;
  background:
    radial-gradient(circle at 82% 24%, rgba(255, 220, 135, 0.34), transparent 26%),
    linear-gradient(135deg, #d9231f 0%, #b91315 58%, #7d0808 100%);
  color: #fff;
  box-shadow: 0 18rpx 42rpx rgba(125, 8, 8, 0.2);
}

.hero::after {
  position: absolute;
  right: -78rpx;
  bottom: -92rpx;
  width: 230rpx;
  height: 230rpx;
  border: 2rpx solid rgba(255, 232, 170, 0.26);
  border-radius: 50%;
  content: '';
}

.hero-pattern {
  position: absolute;
  color: rgba(255, 238, 190, 0.12);
  font-size: 150rpx;
  font-weight: 900;
  line-height: 1;
}

.hero-pattern.left {
  left: 24rpx;
  bottom: 22rpx;
}

.hero-pattern.right {
  right: 46rpx;
  top: 42rpx;
  font-size: 108rpx;
}

.brand,
.title,
.subtitle,
.hero-foot,
.metric-icon,
.metric-label,
.metric-value,
.section-title,
.notice-title,
.notice-text,
.banquet-name,
.banquet-meta {
  display: block;
}

.brand {
  color: #ffe8b5;
  font-size: 28rpx;
  font-weight: 800;
}

.title {
  margin-top: 34rpx;
  color: #ffe9b5;
  font-size: 52rpx;
  font-weight: 900;
  line-height: 1.16;
  text-shadow: 0 6rpx 18rpx rgba(67, 7, 7, 0.28);
}

.subtitle {
  max-width: 560rpx;
  margin-top: 18rpx;
  color: rgba(255, 248, 224, 0.9);
  font-size: 26rpx;
  line-height: 1.55;
}

.hero-foot {
  display: flex;
  gap: 12rpx;
  flex-wrap: wrap;
  margin-top: 28rpx;
}

.hero-foot text {
  padding: 8rpx 14rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.16);
  color: #fff8e0;
  font-size: 22rpx;
}

.create-banner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 18rpx;
  height: 104rpx;
  margin-top: 22rpx;
  border-radius: 8rpx;
  background: linear-gradient(135deg, #e2362d, #b90f12);
  color: #fff;
  font-size: 34rpx;
  font-weight: 900;
  box-shadow: 0 12rpx 28rpx rgba(185, 15, 18, 0.2);
}

.create-plus {
  display: grid;
  width: 46rpx;
  height: 46rpx;
  place-items: center;
  border-radius: 50%;
  background: #fff;
  color: #c91f1f;
  font-size: 38rpx;
  line-height: 46rpx;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18rpx;
  margin-top: 22rpx;
}

.metric-card,
.guide-card,
.notice,
.recent-card {
  border: 1rpx solid rgba(120, 81, 48, 0.12);
  border-radius: 8rpx;
  background: rgba(255, 253, 250, 0.98);
  box-shadow: 0 12rpx 30rpx rgba(87, 62, 41, 0.07);
}

.metric-card {
  min-height: 158rpx;
  padding: 20rpx 10rpx;
  text-align: center;
}

.metric-icon {
  width: 58rpx;
  height: 58rpx;
  margin: 0 auto 12rpx;
  border-radius: 50%;
  background: #fff0e9;
  color: #c51f1f;
  font-size: 24rpx;
  font-weight: 900;
  line-height: 58rpx;
}

.metric-label {
  color: #6f6258;
  font-size: 23rpx;
}

.metric-value {
  margin-top: 8rpx;
  color: #c51f1f;
  font-size: 34rpx;
  font-weight: 900;
}

.metric-value.compact {
  font-size: 27rpx;
}

.guide-card,
.notice,
.recent-card {
  margin-top: 22rpx;
  padding: 24rpx;
}

.section-head,
.title-wrap,
.guide-item,
.banquet-card,
.banquet-title-row {
  display: flex;
  align-items: center;
}

.section-head {
  justify-content: space-between;
  margin-bottom: 18rpx;
}

.title-wrap {
  gap: 12rpx;
}

.red-bar {
  display: block;
  width: 8rpx;
  height: 32rpx;
  border-radius: 999rpx;
  background: #d8231f;
}

.section-title {
  color: #171923;
  font-size: 31rpx;
  font-weight: 900;
}

.section-meta {
  color: #7b6a5b;
  font-size: 24rpx;
}

.refresh-btn {
  margin: 0;
  border: 1rpx solid #ead8c4;
  color: #8f2d20;
  background: #fff7ed;
}

.guide-list {
  display: grid;
  gap: 14rpx;
}

.guide-item {
  min-height: 76rpx;
  padding: 0 18rpx;
  border: 1rpx solid #eadfd3;
  border-radius: 8rpx;
  background: linear-gradient(180deg, #fffdfa, #fff8ef);
}

.step {
  display: grid;
  width: 44rpx;
  height: 44rpx;
  place-items: center;
  border-radius: 50%;
  background: linear-gradient(135deg, #d9231f, #a80e10);
  color: #fff7df;
  font-size: 24rpx;
  font-weight: 900;
}

.guide-text {
  flex: 1;
  margin-left: 20rpx;
  color: #28231f;
  font-size: 27rpx;
  font-weight: 800;
}

.guide-arrow,
.chevron {
  color: #9a8d82;
  font-size: 44rpx;
}

.notice {
  border-color: #f0c9a4;
  background: #fff7ed;
}

.notice-title {
  color: #172033;
  font-size: 29rpx;
  font-weight: 900;
}

.notice-text {
  margin-top: 10rpx;
  color: #8a4d20;
  font-size: 24rpx;
  line-height: 1.58;
}

.empty,
.empty-action {
  display: block;
  padding: 26rpx 0;
  color: #8a4d20;
  font-size: 25rpx;
  text-align: center;
}

.empty-action {
  border: 1rpx dashed #e6b894;
  border-radius: 8rpx;
  background: #fff7ed;
  font-weight: 800;
}

.banquet-card {
  gap: 18rpx;
  padding: 22rpx 0;
  border-top: 1rpx solid #f0e6dc;
}

.banquet-avatar {
  display: grid;
  width: 58rpx;
  height: 58rpx;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 50%;
  background: linear-gradient(135deg, #f08a75, #d9231f);
  color: #fff7df;
  font-size: 25rpx;
  font-weight: 900;
}

.banquet-main {
  min-width: 0;
  flex: 1;
}

.banquet-title-row {
  gap: 10rpx;
  min-width: 0;
}

.banquet-name {
  overflow: hidden;
  color: #172033;
  font-size: 29rpx;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.banquet-tag {
  flex: 0 0 auto;
  padding: 5rpx 12rpx;
  border-radius: 999rpx;
  background: #fff0e9;
  color: #a93224;
  font-size: 20rpx;
}

.banquet-meta {
  margin-top: 5rpx;
  color: #756a61;
  font-size: 23rpx;
}

.tappable:active,
.create-banner:active,
.guide-item:active,
.banquet-card:active,
.refresh-btn:active {
  opacity: 0.78;
}
</style>
