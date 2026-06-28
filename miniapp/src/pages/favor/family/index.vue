<template>
  <view class="page">
    <view class="hero">
      <text class="eyebrow">家庭共享</text>
      <text class="title">家庭人情簿</text>
      <text class="subtitle">家庭协作功能将在后续版本开放，当前可先查看入口结构。</text>
    </view>

    <view class="summary-card">
      <view class="summary-item">
        <text class="summary-label">家庭总收礼</text>
        <text class="summary-value">¥0</text>
      </view>
      <view class="summary-item">
        <text class="summary-label">家庭总送礼</text>
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
        <text class="empty-desc">家庭协作开放后，可按成员汇总收礼、送礼和差额。</text>
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
const members: Array<{ name: string; role: string }> = [];
const records: Array<{ id: number; type: string; name: string; event: string; date: string; owner: string; amount: number }> = [];

function showComingSoon() {
  uni.showToast({ title: '家庭协作功能将在后续版本开放', icon: 'none' });
}
</script>

<style scoped>
.page {
  box-sizing: border-box;
  min-height: 100vh;
  padding: 24rpx 24rpx 140rpx;
  background: #f7f7f7;
  color: #151823;
}

.hero {
  overflow: hidden;
  padding: 34rpx 28rpx;
  border-radius: 16rpx;
  background:
    radial-gradient(circle at 84% 16%, rgba(255, 232, 190, 0.32), transparent 26%),
    linear-gradient(135deg, #e60012, #b80000);
  color: #fff;
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
  border: 1rpx solid #eeeeee;
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
  color: #c71916;
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
  border: 1rpx solid #f0d4bd;
  border-radius: 999rpx;
  background: #fff8ef;
  color: #b80000;
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
  background: linear-gradient(135deg, #ef6a62, #d8271f);
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
  background: #e60012;
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
  background: linear-gradient(135deg, #e60012, #c71916);
  color: #fff;
  font-size: 29rpx;
  font-weight: 800;
  line-height: 88rpx;
}
</style>
