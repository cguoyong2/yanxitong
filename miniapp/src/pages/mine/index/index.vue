<template>
  <view class="page">
    <view class="profile-card">
      <view class="avatar">宴</view>
      <view class="profile-main">
        <text class="name">宴席通用户</text>
        <text class="meta">体验版 · 非支付流程测试</text>
      </view>
      <button class="profile-btn" @tap="showComingSoon()">完善资料</button>
    </view>

    <view class="banner">
      <text class="banner-title">运营与办席服务</text>
      <text class="banner-text">版本权益、设备租赁、请柬模板和客服支持会逐步接入。</text>
    </view>

    <view class="grid-card">
      <view class="grid-item" @tap="openOrders()">
        <text class="grid-icon">版</text>
        <text class="grid-title">版本订单</text>
      </view>
      <view class="grid-item" @tap="openDevices()">
        <text class="grid-icon">屏</text>
        <text class="grid-title">设备订单</text>
      </view>
      <view class="grid-item" @tap="showComingSoon()">
        <text class="grid-icon">票</text>
        <text class="grid-title">发票信息</text>
      </view>
      <view class="grid-item" @tap="showComingSoon()">
        <text class="grid-icon">服</text>
        <text class="grid-title">专属客服</text>
      </view>
    </view>

    <view class="panel">
      <text class="section-title">常用服务</text>
      <view v-for="item in services" :key="item.title" class="service-row" @tap="handleService(item.action)">
        <view>
          <text class="service-title">{{ item.title }}</text>
          <text class="service-desc">{{ item.desc }}</text>
        </view>
        <text class="arrow">›</text>
      </view>
    </view>

    <view class="panel">
      <text class="section-title">系统</text>
      <view class="service-row" @tap="showVersion()">
        <view>
          <text class="service-title">当前版本</text>
          <text class="service-desc">MVP 体验版</text>
        </view>
        <text class="arrow">›</text>
      </view>
      <view class="service-row" @tap="showComingSoon()">
        <view>
          <text class="service-title">隐私与安全</text>
          <text class="service-desc">账号、数据和操作日志说明</text>
        </view>
        <text class="arrow">›</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
const services = [
  { title: '我的宴席', desc: '查看已创建宴席', action: 'banquet' },
  { title: '收礼记录', desc: '查看线上和线下礼金', action: 'gift' },
  { title: '人情账本', desc: '维护往来联系人', action: 'favor' },
  { title: '请柬模板', desc: '管理模板和基础请柬', action: 'invitation' }
];

function openOrders() {
  uni.navigateTo({ url: '/pages/order/plan/index' });
}

function openDevices() {
  uni.navigateTo({ url: '/pages/device/select/index' });
}

function handleService(action: string) {
  if (action === 'banquet') {
    uni.switchTab({ url: '/pages/home/index/index' });
    return;
  }
  if (action === 'favor') {
    uni.switchTab({ url: '/pages/favor/index/index' });
    return;
  }
  if (action === 'invitation') {
    uni.switchTab({ url: '/pages/invitation/index/index' });
    return;
  }
  if (action === 'gift') {
    uni.navigateTo({ url: '/pages/gift/list/index' });
    return;
  }
  showComingSoon();
}

function showVersion() {
  uni.showModal({
    title: '宴席通',
    content: '当前为 MVP 体验版，优先验证非支付流程。',
    showCancel: false,
    confirmText: '知道了'
  });
}

function showComingSoon() {
  uni.showToast({ title: '后续版本开放', icon: 'none' });
}
</script>

<style scoped>
.page {
  box-sizing: border-box;
  min-height: 100vh;
  padding: 24rpx;
  background: #f7f7f7;
  color: #151823;
}

.profile-card {
  display: flex;
  align-items: center;
  gap: 18rpx;
  padding: 26rpx;
  border-radius: 16rpx;
  background: #fff;
  box-shadow: 0 10rpx 24rpx rgba(30, 18, 12, 0.04);
}

.avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 84rpx;
  height: 84rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #e60012, #b80000);
  color: #fff;
  font-size: 34rpx;
  font-weight: 800;
}

.profile-main {
  flex: 1;
  min-width: 0;
}

.name,
.meta,
.banner-title,
.banner-text,
.grid-title,
.section-title,
.service-title,
.service-desc {
  display: block;
}

.name {
  font-size: 32rpx;
  font-weight: 800;
}

.meta {
  margin-top: 8rpx;
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

.profile-btn {
  padding: 0 18rpx;
  border: 1rpx solid #f0d4bd;
  border-radius: 999rpx;
  background: #fff8ef;
  color: #b80000;
  font-size: 23rpx;
  line-height: 56rpx;
}

.banner {
  position: relative;
  overflow: hidden;
  margin-top: 20rpx;
  padding: 30rpx;
  border-radius: 16rpx;
  background:
    radial-gradient(circle at 88% 18%, rgba(255, 232, 190, 0.33), transparent 25%),
    linear-gradient(135deg, #e60012, #b80000);
  color: #fff;
}

.banner-title {
  color: #fff1ca;
  font-size: 36rpx;
  font-weight: 800;
}

.banner-text {
  margin-top: 12rpx;
  max-width: 500rpx;
  color: rgba(255, 255, 255, 0.86);
  font-size: 24rpx;
  line-height: 1.55;
}

.grid-card {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12rpx;
  margin-top: 20rpx;
}

.grid-item,
.panel {
  border: 1rpx solid #eeeeee;
  border-radius: 12rpx;
  background: #fff;
  box-shadow: 0 10rpx 24rpx rgba(30, 18, 12, 0.04);
}

.grid-item {
  padding: 22rpx 8rpx;
  text-align: center;
}

.grid-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 54rpx;
  height: 54rpx;
  margin: 0 auto;
  border-radius: 50%;
  background: #fff0ee;
  color: #c71916;
  font-weight: 800;
}

.grid-title {
  margin-top: 10rpx;
  font-size: 22rpx;
  font-weight: 700;
}

.panel {
  margin-top: 20rpx;
  padding: 24rpx;
}

.section-title {
  font-size: 31rpx;
  font-weight: 800;
}

.service-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  padding: 22rpx 0;
  border-bottom: 1rpx solid #eeeeee;
}

.service-row:last-child {
  border-bottom: 0;
}

.service-title {
  font-size: 28rpx;
  font-weight: 800;
}

.service-desc {
  margin-top: 7rpx;
  color: #7a7f8c;
  font-size: 23rpx;
}

.arrow {
  color: #b6bbc7;
  font-size: 40rpx;
}
</style>
