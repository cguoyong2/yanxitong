<template>
  <view class="page" :class="activeTheme.tone">
    <view class="hero">
      <text class="hero-mark">{{ activeTheme.mark }}</text>
      <text class="eyebrow">家庭共享</text>
      <text class="title">家庭人情簿</text>
      <text class="subtitle">{{ activeTheme.favorText }}，家庭协作功能将在后续版本开放。</text>
    </view>

    <view class="summary-card">
      <view class="summary-item">
        <text class="summary-label">家庭总收到{{ activeTheme.giftLabel }}</text>
        <text class="summary-value">¥0</text>
      </view>
      <view class="summary-item">
        <text class="summary-label">家庭总送出{{ activeTheme.giftLabel }}</text>
        <text class="summary-value">¥0</text>
      </view>
      <view class="summary-item">
        <text class="summary-label">联系人</text>
        <text class="summary-value">0</text>
      </view>
      <view class="summary-item">
        <text class="summary-label">总差额</text>
        <text class="summary-value positive">持平</text>
      </view>
    </view>

    <view class="member-card">
      <view class="section-head">
        <text class="section-title">家庭成员</text>
        <button class="plain-btn" @tap="showComingSoon()">邀请成员</button>
      </view>
      <view class="member-list">
        <view v-for="member in members" :key="member.name" class="member">
          <text class="avatar">{{ member.name.slice(0, 1) }}</text>
          <text class="member-name">{{ member.name }}</text>
          <text class="member-role">{{ member.role }}</text>
        </view>
      </view>
      <view v-if="members.length === 0" class="empty">
        <text class="empty-title">暂无家庭成员</text>
        <text class="empty-desc">后续可邀请家人共同维护人情往来。</text>
      </view>
    </view>

    <view class="panel">
      <view class="section-head">
        <text class="section-title">家庭往来记录</text>
        <text class="section-meta">{{ records.length }} 条</text>
      </view>
      <view v-if="records.length === 0" class="empty">
        <text class="empty-title">暂无家庭往来记录</text>
        <text class="empty-desc">家庭协作开放后，可按成员汇总{{ activeTheme.giftLabel }}收送和差额。</text>
      </view>
      <view v-for="record in records" :key="record.id" class="record-row">
        <text class="record-badge" :class="record.type">{{ record.type === 'receive' ? '收' : '送' }}</text>
        <view class="record-main">
          <text class="record-title">{{ record.name }} · {{ record.event }}</text>
          <text class="record-meta">{{ record.date }} · {{ record.owner }}</text>
        </view>
        <text class="record-amount" :class="record.type">{{ record.type === 'receive' ? '+' : '-' }}¥{{ record.amount }}</text>
      </view>
    </view>

    <button class="bottom-btn" @tap="showComingSoon()">补录家庭人情</button>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { eventThemeFor, readActiveEventType } from '../../../utils/event-theme';

const members: Array<{ name: string; role: string }> = [];
const records: Array<{ id: number; type: string; name: string; event: string; date: string; owner: string; amount: number }> = [];
const activeType = ref(readActiveEventType());
const activeTheme = computed(() => eventThemeFor(activeType.value));

function showComingSoon() {
  uni.showToast({ title: '家庭协作功能将在后续版本开放', icon: 'none' });
}

onShow(() => {
  activeType.value = readActiveEventType();
});
</script>

<style scoped>
.page {
  --accent: #e60012;
  --accent-dark: #c40005;
  --accent-soft: #fff0ee;
  --page-bg: #f7f3ee;
  --accent-shadow: rgba(184, 17, 21, 0.22);
  box-sizing: border-box;
  min-height: 100vh;
  padding: 24rpx 24rpx 140rpx;
  background: var(--page-bg);
  color: #151823;
}

.page.orange {
  --accent: #d96a11;
  --accent-dark: #a64209;
  --accent-soft: #fff3e3;
  --page-bg: #fbf4eb;
  --accent-shadow: rgba(166, 86, 17, 0.2);
}

.page.pink {
  --accent: #e7566f;
  --accent-dark: #b52d4c;
  --accent-soft: #fff0f4;
  --page-bg: #fff6f8;
  --accent-shadow: rgba(183, 45, 76, 0.18);
}

.page.green {
  --accent: #188356;
  --accent-dark: #0c5f3e;
  --accent-soft: #edf9f1;
  --page-bg: #f2f8f4;
  --accent-shadow: rgba(12, 95, 62, 0.17);
}

.page.blue {
  --accent: #2563eb;
  --accent-dark: #1d4ed8;
  --accent-soft: #edf4ff;
  --page-bg: #f2f6ff;
  --accent-shadow: rgba(29, 78, 216, 0.17);
}

.page.black {
  --accent: #2f3338;
  --accent-dark: #0d0f12;
  --accent-soft: #f1f2f4;
  --page-bg: #f3f4f5;
  --accent-shadow: rgba(13, 15, 18, 0.2);
}

.page.purple {
  --accent: #7c3aed;
  --accent-dark: #5b21b6;
  --accent-soft: #f4efff;
  --page-bg: #f7f3ff;
  --accent-shadow: rgba(91, 33, 182, 0.18);
}

.hero {
  position: relative;
  overflow: hidden;
  padding: 34rpx 28rpx;
  border-radius: 16rpx;
  background:
    radial-gradient(circle at 84% 16%, rgba(255, 232, 190, 0.32), transparent 26%),
    linear-gradient(135deg, var(--accent), var(--accent-dark));
  color: #fff;
  box-shadow: 0 16rpx 42rpx var(--accent-shadow);
}

.hero-mark {
  position: absolute;
  right: 28rpx;
  bottom: 12rpx;
  color: rgba(255, 239, 206, 0.28);
  font-family: serif;
  font-size: 118rpx;
  font-weight: 900;
}

.eyebrow,
.title,
.subtitle,
.summary-label,
.summary-value,
.section-title,
.section-meta,
.member-name,
.member-role,
.record-title,
.record-meta {
  display: block;
}

.eyebrow {
  color: #ffe8bf;
  font-size: 24rpx;
  font-weight: 700;
}

.title {
  margin-top: 18rpx;
  font-size: 44rpx;
  font-weight: 800;
}

.subtitle {
  margin-top: 12rpx;
  color: rgba(255, 255, 255, 0.86);
  font-size: 24rpx;
}

.summary-card {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14rpx;
  margin-top: 20rpx;
}

.summary-item,
.member-card,
.panel {
  border: 1rpx solid #f0dfcf;
  border-radius: 12rpx;
  background: #fff;
  box-shadow: 0 10rpx 24rpx rgba(30, 18, 12, 0.04);
}

.summary-item {
  padding: 24rpx;
}

.summary-label {
  color: #7a7f8c;
  font-size: 23rpx;
}

.summary-value {
  margin-top: 10rpx;
  color: #151823;
  font-size: 34rpx;
  font-weight: 800;
}

.positive,
.record-amount.receive {
  color: var(--accent);
}

.member-card,
.panel {
  margin-top: 20rpx;
  padding: 24rpx;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.section-title {
  font-size: 31rpx;
  font-weight: 800;
}

.section-meta {
  color: #7a7f8c;
  font-size: 23rpx;
}

button {
  margin: 0;
  padding: 0;
  border: 0;
}

button::after {
  border: 0;
}

.plain-btn {
  padding: 0 18rpx;
  border: 1rpx solid var(--accent-soft);
  border-radius: 999rpx;
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 23rpx;
  line-height: 56rpx;
}

.member-list {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14rpx;
  margin-top: 20rpx;
}

.member {
  text-align: center;
}

.avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64rpx;
  height: 64rpx;
  margin: 0 auto;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--accent), var(--accent-dark));
  color: #fff;
  font-weight: 800;
}

.member-name {
  margin-top: 10rpx;
  font-size: 24rpx;
  font-weight: 800;
}

.member-role {
  margin-top: 4rpx;
  color: #7a7f8c;
  font-size: 21rpx;
}

.empty {
  padding: 34rpx 12rpx 10rpx;
  text-align: center;
}

.empty-title,
.empty-desc {
  display: block;
}

.empty-title {
  color: #151823;
  font-size: 28rpx;
  font-weight: 800;
}

.empty-desc {
  margin-top: 10rpx;
  color: #7a7f8c;
  font-size: 23rpx;
  line-height: 1.5;
}

.record-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 22rpx 0;
  border-bottom: 1rpx solid #eeeeee;
}

.record-row:last-child {
  border-bottom: 0;
}

.record-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 50rpx;
  height: 50rpx;
  border-radius: 50%;
  color: #fff;
  font-weight: 800;
}

.record-badge.receive {
  background: var(--accent);
}

.record-badge.give {
  background: #d6a55d;
}

.record-main {
  flex: 1;
  min-width: 0;
}

.record-title {
  font-size: 27rpx;
  font-weight: 800;
}

.record-meta {
  margin-top: 7rpx;
  color: #7a7f8c;
  font-size: 22rpx;
}

.record-amount {
  font-size: 28rpx;
  font-weight: 800;
}

.record-amount.give {
  color: #24824d;
}

.bottom-btn {
  position: fixed;
  right: 24rpx;
  bottom: 34rpx;
  left: 24rpx;
  border-radius: 12rpx;
  background: linear-gradient(135deg, var(--accent), var(--accent-dark));
  color: #fff;
  font-size: 29rpx;
  font-weight: 800;
  line-height: 88rpx;
}
</style>
