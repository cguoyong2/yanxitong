<template>
  <view class="page">
    <view class="hero">
      <view class="hero-symbol">
        <view class="bubble-dot"></view>
        <view class="bubble-dot"></view>
        <view class="bubble-dot"></view>
      </view>
      <text class="hero-eyebrow">情礼记服务中心</text>
      <text class="hero-title">专属客服</text>
      <text class="hero-desc">产品使用、订单支付和设备服务问题，都可以在这里联系我们。</text>
    </view>

    <view class="service-sheet">
      <view class="sheet-head">
        <view class="service-icon">客</view>
        <view class="sheet-heading">
          <text class="sheet-title">情礼记专属客服</text>
          <text class="sheet-subtitle">小程序用户服务</text>
        </view>
        <text class="online-badge">可咨询</text>
      </view>

      <text class="service-intro">遇到系统或操作问题，可直接进入微信客服咨询，或拨打全国免费服务热线。</text>

      <view class="service-info">
        <view class="info-row">
          <view class="info-icon clock-icon">
            <view class="clock-hand clock-hand-hour"></view>
            <view class="clock-hand clock-hand-minute"></view>
          </view>
          <text class="info-label">服务时间</text>
          <text class="info-value">工作日 08:30-18:00</text>
        </view>
        <view class="info-row" @tap="callServiceHotline()">
          <text class="info-icon phone-icon">☎</text>
          <text class="info-label">全国热线</text>
          <text class="info-value hotline">4000-535-836</text>
          <text class="row-arrow">›</text>
        </view>
      </view>

      <view class="privacy-note">
        <text class="privacy-mark">i</text>
        <text>请勿发送身份证照片、支付密码、验证码等敏感资料。</text>
      </view>

      <button
        class="primary-button"
        open-type="contact"
        session-from="qingliji-customer-service"
        show-message-card
        send-message-title="情礼记客服咨询"
        send-message-path="/pages/support/customer-service/index"
      >
        <view class="button-content">
          <text class="button-icon">•••</text>
          <text>添加微信咨询</text>
        </view>
      </button>

      <button class="secondary-button" @tap="callServiceHotline()">
        <view class="button-content">
          <text class="call-icon">☎</text>
          <text>拨打 4000-535-836</text>
        </view>
      </button>
    </view>

    <text class="page-note">全国免费服务热线 · 工作日 08:30-18:00</text>
  </view>
</template>

<script setup lang="ts">
const SERVICE_HOTLINE = '4000535836';

function callServiceHotline() {
  uni.makePhoneCall({
    phoneNumber: SERVICE_HOTLINE,
    fail: () => uni.showToast({ title: '拨号失败，请手动拨打 4000-535-836', icon: 'none' })
  });
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding-bottom: 56rpx;
  box-sizing: border-box;
  background: #f3f6fa;
}

.hero {
  height: 430rpx;
  padding: 54rpx 54rpx 0;
  position: relative;
  overflow: hidden;
  box-sizing: border-box;
  background: #1769e0;
  color: #fff;
}

.hero::after {
  content: '';
  width: 350rpx;
  height: 350rpx;
  position: absolute;
  top: -170rpx;
  right: -130rpx;
  border: 2rpx solid rgba(255, 255, 255, 0.16);
  border-radius: 50%;
}

.hero-symbol {
  width: 108rpx;
  height: 108rpx;
  margin-bottom: 28rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  position: relative;
  z-index: 1;
  border: 2rpx solid rgba(255, 255, 255, 0.48);
  border-radius: 16rpx;
  background: rgba(255, 255, 255, 0.13);
}

.hero-symbol::after {
  content: '';
  width: 18rpx;
  height: 18rpx;
  position: absolute;
  right: 16rpx;
  bottom: -10rpx;
  border-right: 2rpx solid rgba(255, 255, 255, 0.48);
  border-bottom: 2rpx solid rgba(255, 255, 255, 0.48);
  background: #397ee5;
  transform: rotate(45deg);
}

.bubble-dot {
  width: 10rpx;
  height: 10rpx;
  border-radius: 50%;
  background: #fff;
}

.hero-eyebrow,
.hero-title,
.hero-desc,
.sheet-title,
.sheet-subtitle {
  display: block;
}

.hero-eyebrow {
  font-size: 25rpx;
  font-weight: 600;
  opacity: 0.84;
}

.hero-title {
  margin-top: 10rpx;
  font-size: 48rpx;
  font-weight: 700;
}

.hero-desc {
  width: 570rpx;
  margin-top: 14rpx;
  font-size: 25rpx;
  line-height: 1.6;
  opacity: 0.82;
}

.service-sheet {
  margin: -58rpx 30rpx 0;
  padding: 40rpx 34rpx 34rpx;
  position: relative;
  z-index: 2;
  box-sizing: border-box;
  border: 1rpx solid #e3e8ef;
  border-radius: 16rpx;
  background: #fff;
  box-shadow: 0 20rpx 48rpx rgba(35, 58, 91, 0.1);
}

.sheet-head {
  display: flex;
  align-items: center;
}

.service-icon {
  width: 84rpx;
  height: 84rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: none;
  border-radius: 16rpx;
  background: #eaf2ff;
  color: #1769e0;
  font-size: 32rpx;
  font-weight: 700;
}

.sheet-heading {
  min-width: 0;
  margin-left: 22rpx;
  flex: 1;
}

.sheet-title {
  color: #172033;
  font-size: 32rpx;
  font-weight: 700;
}

.sheet-subtitle {
  margin-top: 6rpx;
  color: #7a8495;
  font-size: 23rpx;
}

.online-badge {
  padding: 9rpx 16rpx;
  flex: none;
  border-radius: 999rpx;
  background: #edf8f2;
  color: #16875d;
  font-size: 22rpx;
  font-weight: 600;
}

.service-intro {
  display: block;
  margin-top: 34rpx;
  color: #465268;
  font-size: 27rpx;
  line-height: 1.7;
}

.service-info {
  margin-top: 30rpx;
  border-top: 1rpx solid #e7ebf1;
  border-bottom: 1rpx solid #e7ebf1;
}

.info-row {
  min-height: 94rpx;
  display: flex;
  align-items: center;
}

.info-row + .info-row {
  border-top: 1rpx solid #edf0f4;
}

.info-icon {
  width: 36rpx;
  height: 36rpx;
  margin-right: 16rpx;
  position: relative;
  box-sizing: border-box;
  flex: none;
  color: #61708a;
}

.clock-icon {
  border: 3rpx solid #61708a;
  border-radius: 50%;
}

.clock-hand {
  height: 3rpx;
  position: absolute;
  left: 16rpx;
  top: 15rpx;
  border-radius: 3rpx;
  background: #61708a;
  transform-origin: left center;
}

.clock-hand-hour {
  width: 8rpx;
  transform: rotate(-90deg);
}

.clock-hand-minute {
  width: 11rpx;
  transform: rotate(28deg);
}

.phone-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32rpx;
}

.info-label {
  width: 132rpx;
  flex: none;
  color: #59667c;
  font-size: 25rpx;
}

.info-value {
  flex: 1;
  color: #202a3d;
  font-size: 25rpx;
  font-weight: 600;
  text-align: right;
}

.hotline {
  color: #1769e0;
}

.row-arrow {
  margin-left: 12rpx;
  color: #9aa4b3;
  font-size: 36rpx;
}

.privacy-note {
  margin: 28rpx 0;
  display: flex;
  align-items: flex-start;
  gap: 12rpx;
  color: #a66a12;
  font-size: 23rpx;
  line-height: 1.55;
}

.privacy-mark {
  width: 30rpx;
  height: 30rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: none;
  border: 2rpx solid #c18425;
  border-radius: 50%;
  font-size: 20rpx;
  font-weight: 700;
}

.primary-button,
.secondary-button {
  width: 100%;
  height: 92rpx;
  border-radius: 12rpx;
  font-size: 29rpx;
  font-weight: 600;
  line-height: 92rpx;
}

.primary-button {
  background: #1769e0;
  color: #fff;
}

.secondary-button {
  margin-top: 18rpx;
  border: 1rpx solid #d9e0e9;
  background: #fff;
  color: #28354a;
}

.button-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16rpx;
}

.button-icon {
  height: 38rpx;
  padding: 0 10rpx;
  display: flex;
  align-items: center;
  border: 2rpx solid currentColor;
  border-radius: 10rpx;
  font-size: 18rpx;
  letter-spacing: 3rpx;
  line-height: 1;
}

.call-icon {
  font-size: 30rpx;
}

.page-note {
  display: block;
  margin-top: 28rpx;
  color: #8a94a5;
  font-size: 22rpx;
  text-align: center;
}
</style>
