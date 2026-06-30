<template>
  <view class="page" :class="activeTheme.tone">
    <view class="red-stage">
      <view class="stage-art">
        <text class="firework">✦</text>
        <text class="stage-knot">{{ activeTheme.mark }}</text>
      </view>
      <view class="topbar">
        <view class="brand-row">
          <text class="brand">宴席通</text>
          <text class="hello">{{ activeTheme.mineText }}</text>
        </view>
        <view class="top-actions">
          <view class="top-action" @tap="showComingSoon()">
            <text class="top-icon">☊</text>
            <text>客服</text>
          </view>
          <view class="top-action" @tap="showComingSoon()">
            <text class="top-icon">⋯</text>
            <text>消息</text>
          </view>
        </view>
      </view>
    </view>

    <view class="content">
      <swiper class="banner-card" circular :indicator-dots="false" autoplay @change="bannerIndex = Number($event.detail.current)">
        <swiper-item v-for="banner in banners" :key="banner.image">
          <image class="banner-image" :src="banner.image" mode="aspectFill" @tap="handleBanner(banner.action)" />
        </swiper-item>
      </swiper>
        <view class="banner-dots">
          <text v-for="(_, index) in banners" :key="index" class="dot" :class="{ active: index === bannerIndex }"></text>
        </view>

      <view class="profile-card">
        <image class="avatar" src="/static/mine/avatar_user.png" mode="aspectFill" />
        <view class="profile-main">
          <view class="name-line">
            <text class="user-name">宴席通用户</text>
            <text class="real-badge">已实名</text>
          </view>
          <text class="phone">登录后展示手机号</text>
          <view class="profile-stats">
            <view>
              <text class="profile-number">{{ banquetCount }}</text>
              <text class="profile-label">我的宴席</text>
            </view>
            <view class="stat-divider"></view>
            <view>
              <text class="profile-number">{{ invitationCount }}</text>
              <text class="profile-label">我的请柬</text>
            </view>
            <view class="stat-divider"></view>
            <view>
              <text class="profile-number">{{ pendingCount }}</text>
              <text class="profile-label">待处理</text>
            </view>
          </view>
        </view>
        <button class="edit-btn" @tap="showComingSoon()">✎ 编辑资料</button>
      </view>

      <view class="section-card">
        <view class="section-head">
          <text class="section-title">我的订单</text>
          <text class="more">查看全部 ›</text>
        </view>
        <view class="order-grid">
          <view v-for="item in orders" :key="item.title" class="order-item" @tap="handleAction(item.action)">
            <text class="order-icon" :class="item.tone">{{ item.icon }}</text>
            <text class="order-title">{{ item.title }}</text>
          </view>
        </view>
      </view>

      <view class="section-card compact">
        <view class="section-head">
          <text class="section-title">我的设备</text>
          <text class="more">查看全部 ›</text>
        </view>
        <view class="device-grid">
          <view v-for="item in devices" :key="item.title" class="device-item" @tap="handleAction(item.action)">
            <text class="device-icon" :class="item.tone">{{ item.icon }}</text>
            <text>{{ item.title }}</text>
          </view>
        </view>
      </view>

      <view class="section-card">
        <text class="section-title">我的服务</text>
        <view class="service-grid">
          <view v-for="item in services" :key="item.title" class="service-item" @tap="handleAction(item.action)">
            <text class="service-icon" :class="item.tone">{{ item.icon }}</text>
            <text>{{ item.title }}</text>
          </view>
        </view>
      </view>

      <view class="section-card settings-card">
        <text class="section-title">帮助与设置</text>
        <view class="settings-grid">
          <view v-for="item in settings" :key="item.title" class="setting-item" @tap="showComingSoon()">
            <text class="setting-icon">{{ item.icon }}</text>
            <text>{{ item.title }}</text>
            <text class="setting-arrow">›</text>
          </view>
        </view>
      </view>

      <view class="agent-card">
        <image class="agent-image" src="/static/mine/service_agent.png" mode="aspectFill" />
        <view class="agent-main">
          <view class="agent-title-line">
            <text class="agent-title">专属客服</text>
            <text class="agent-desc">为您提供一对一贴心服务</text>
          </view>
          <view class="agent-tags">
            <text>快速响应</text>
            <text>专业解答</text>
            <text>贴心服务</text>
          </view>
        </view>
        <button class="agent-btn" @tap="showComingSoon()">立即联系</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { request } from '../../../api/client';
import { eventThemeFor, readActiveEventType, writeActiveEventType } from '../../../utils/event-theme';
import { readLastBanquetContext, writeLastBanquetContext } from '../../../utils/banquet';

interface Banquet {
  id: number;
  name?: string;
}

const banquetCount = ref(0);
const invitationCount = ref(0);
const pendingCount = ref(0);
const latestBanquetId = ref(0);
const latestInvitationSlug = ref('');
const bannerIndex = ref(0);
const activeType = ref(readActiveEventType());
const activeTheme = computed(() => eventThemeFor(activeType.value));
const banners = [
  { image: '/static/mine/mine_banner.png', action: 'banquet' },
  { image: '/static/home/home_banner.png', action: 'invitation' },
  { image: '/static/favor/favor_banner.png', action: 'favor' }
];
const orders = [
  { title: '版本订单', icon: '▤', tone: 'red', action: 'plan' },
  { title: '模板订单', icon: '▦', tone: 'orange', action: 'invitation' },
  { title: '设备订单', icon: '◔', tone: 'blue', action: 'device' },
  { title: '定制订单', icon: '✎', tone: 'purple', action: 'custom' }
];
const devices = [
  { title: '确认屏租赁', icon: '▣', tone: 'red', action: 'device' },
  { title: '云喇叭租赁', icon: '◖', tone: 'orange', action: 'device' },
  { title: '绑定记录', icon: '↗', tone: 'green', action: 'device' },
  { title: '交付说明', icon: '▰', tone: 'blue', action: 'device' }
];
const services = [
  { title: '我的宴席', icon: '囍', tone: 'red', action: 'banquet' },
  { title: '我的请柬', icon: '✉', tone: 'red', action: 'invitation' },
  { title: '收礼记录', icon: '▣', tone: 'orange', action: 'gift' },
  { title: '人情账本', icon: '▤', tone: 'orange', action: 'favor' },
  { title: '使用教程', icon: '▶', tone: 'green', action: 'help' },
  { title: '联系客服', icon: '☊', tone: 'blue', action: 'service' }
];
const settings = [
  { title: '常见问题', icon: '?' },
  { title: '消息通知', icon: '♧' },
  { title: '隐私设置', icon: '▣' },
  { title: '账号设置', icon: '♙' },
  { title: '意见反馈', icon: '✎' }
];

function handleAction(action: string) {
  if (action === 'banquet') {
    openLatestBanquet();
    return;
  }
  if (action === 'favor') {
    uni.switchTab({ url: '/pages/favor/index/index' });
    return;
  }
  if (action === 'invitation') {
    openLatestInvitation();
    return;
  }
  if (action === 'gift') {
    openGiftRecords();
    return;
  }
  if (action === 'plan') {
    openPlanOrders();
    return;
  }
  if (action === 'device') {
    openDeviceOrders();
    return;
  }
  showComingSoon();
}

function handleBanner(action: string) {
  handleAction(action);
}

function openLatestBanquet() {
  if (latestBanquetId.value) {
    safeNavigate(`/pages/banquet/detail/index?id=${latestBanquetId.value}`, '宴席详情打开失败');
    return;
  }
  uni.switchTab({ url: '/pages/home/index/index' });
}

function openLatestInvitation() {
  if (latestInvitationSlug.value) {
    safeNavigate(`/pages/invite/public/index?slug=${latestInvitationSlug.value}`, '请柬公开页打开失败');
    return;
  }
  uni.switchTab({ url: '/pages/invitation/index/index' });
}

function openGiftRecords() {
  if (!latestBanquetId.value) {
    uni.showToast({ title: '请先创建宴席', icon: 'none' });
    return;
  }
  safeNavigate(`/pages/gift/list/index?banquetId=${latestBanquetId.value}`, '收礼记录打开失败');
}

function openPlanOrders() {
  const query = latestBanquetId.value ? `?banquetId=${latestBanquetId.value}` : '';
  safeNavigate(`/pages/order/plan/index${query}`, '版本订单打开失败');
}

function openDeviceOrders() {
  const query = latestBanquetId.value ? `?banquetId=${latestBanquetId.value}` : '';
  safeNavigate(`/pages/device/select/index${query}`, '设备订单打开失败');
}

function safeNavigate(url: string, failTitle: string) {
  uni.navigateTo({
    url,
    fail: () => uni.showToast({ title: failTitle, icon: 'none' })
  });
}

function showComingSoon() {
  uni.showToast({ title: '该设置将在后续运营版本开放', icon: 'none' });
}

async function loadProfileStats() {
  const banquets = await request<Banquet[]>('/banquets').catch(() => []);
  banquetCount.value = banquets.length;
  invitationCount.value = banquets.length;
  pendingCount.value = 0;
  latestBanquetId.value = banquets[0]?.id || 0;
  if (!latestBanquetId.value) {
    const cached = readLastBanquetContext();
    if (cached?.id) {
      banquetCount.value = 1;
      invitationCount.value = cached.shareSlug ? 1 : 0;
      latestBanquetId.value = cached.id;
      latestInvitationSlug.value = cached.shareSlug || '';
      activeType.value = writeActiveEventType(cached.eventTypeCode || activeType.value);
    }
    return;
  }
  writeLastBanquetContext({ id: latestBanquetId.value, name: banquets[0]?.name });
  if (latestBanquetId.value) {
    const detail = await request<{ banquet?: { eventTypeCode?: string; name?: string; banquetTime?: string; location?: string; themeCode?: string }; invitation?: { id?: number; shareSlug?: string } }>(`/banquets/${latestBanquetId.value}`).catch(() => undefined);
    latestInvitationSlug.value = detail?.invitation?.shareSlug || '';
    writeLastBanquetContext({
      id: latestBanquetId.value,
      name: detail?.banquet?.name || banquets[0]?.name,
      eventTypeCode: detail?.banquet?.eventTypeCode,
      themeCode: detail?.banquet?.themeCode,
      banquetTime: detail?.banquet?.banquetTime,
      location: detail?.banquet?.location,
      invitationId: detail?.invitation?.id,
      shareSlug: detail?.invitation?.shareSlug
    });
    if (detail?.banquet?.eventTypeCode) {
      activeType.value = writeActiveEventType(detail.banquet.eventTypeCode);
    }
  }
}

onMounted(loadProfileStats);
onShow(() => {
  activeType.value = readActiveEventType();
  loadProfileStats();
});
</script>

<style scoped>
.page {
  --accent: #e60012;
  --accent-dark: #c40005;
  --accent-soft: #fff0ee;
  --page-bg: linear-gradient(180deg, #fff0ee 0%, #fff8f2 42%, #f7f7f7 100%);
  min-height: 100vh;
  background: var(--page-bg);
  color: #151823;
}

.page.orange {
  --accent: #d96a11;
  --accent-dark: #a64209;
  --accent-soft: #fff3e3;
  --page-bg: linear-gradient(180deg, #fff3e3 0%, #fff9f0 42%, #f8f4ef 100%);
}

.page.pink {
  --accent: #e7566f;
  --accent-dark: #b52d4c;
  --accent-soft: #fff0f4;
  --page-bg: linear-gradient(180deg, #fff0f4 0%, #fff8fa 42%, #f8f2f4 100%);
}

.page.green {
  --accent: #188356;
  --accent-dark: #0c5f3e;
  --accent-soft: #edf9f1;
  --page-bg: linear-gradient(180deg, #edf9f1 0%, #f7fcf8 42%, #f1f7f3 100%);
}

.page.blue {
  --accent: #2563eb;
  --accent-dark: #1d4ed8;
  --accent-soft: #edf4ff;
  --page-bg: linear-gradient(180deg, #edf4ff 0%, #f7fbff 42%, #f1f5fb 100%);
}

.page.black {
  --accent: #2f3338;
  --accent-dark: #0d0f12;
  --accent-soft: #f1f2f4;
  --page-bg: linear-gradient(180deg, #1f2226 0%, #f1f2f4 40%, #f7f7f7 100%);
}

.page.purple {
  --accent: #7c3aed;
  --accent-dark: #5b21b6;
  --accent-soft: #f4efff;
  --page-bg: linear-gradient(180deg, #f4efff 0%, #fbf8ff 42%, #f6f2fb 100%);
}

.red-stage {
  position: relative;
  overflow: hidden;
  height: 330rpx;
  padding: calc(var(--status-bar-height) + 34rpx) 40rpx 0;
  background:
    radial-gradient(circle at 72% 38%, rgba(255, 190, 80, 0.18), transparent 26%),
    linear-gradient(135deg, var(--accent) 0%, var(--accent-dark) 58%, var(--accent-dark) 100%);
  color: #fff;
}

.page.orange .red-stage {
  background:
    radial-gradient(circle at 72% 38%, rgba(255, 218, 138, 0.2), transparent 26%),
    linear-gradient(135deg, #c15b10 0%, #a64209 58%, #7a2d08 100%);
}

.page.pink .red-stage {
  background:
    radial-gradient(circle at 72% 38%, rgba(255, 198, 212, 0.26), transparent 26%),
    linear-gradient(135deg, #e7566f 0%, #c73655 58%, #932742 100%);
}

.page.green .red-stage {
  background:
    radial-gradient(circle at 72% 38%, rgba(185, 245, 202, 0.22), transparent 26%),
    linear-gradient(135deg, #1b8a58 0%, #116943 58%, #0b4b31 100%);
}

.page.blue .red-stage {
  background:
    radial-gradient(circle at 72% 38%, rgba(186, 220, 255, 0.24), transparent 26%),
    linear-gradient(135deg, #2563eb 0%, #1d4ed8 58%, #1e3a8a 100%);
}

.page.black .red-stage {
  background:
    radial-gradient(circle at 72% 38%, rgba(255, 255, 255, 0.08), transparent 26%),
    linear-gradient(135deg, #202124 0%, #111315 58%, #050607 100%);
}

.page.purple .red-stage {
  background:
    radial-gradient(circle at 72% 38%, rgba(218, 200, 255, 0.24), transparent 26%),
    linear-gradient(135deg, #7c3aed 0%, #5b21b6 58%, #3b0764 100%);
}

.red-stage::after {
  position: absolute;
  right: -60rpx;
  bottom: -34rpx;
  left: -60rpx;
  height: 118rpx;
  border-radius: 0 0 50% 50%;
  background: var(--page-bg);
  transform: rotate(7deg);
  transform-origin: left top;
  content: '';
}

.stage-art {
  position: absolute;
  inset: 0;
  opacity: 0.32;
  pointer-events: none;
}

.firework {
  position: absolute;
  right: 245rpx;
  top: 46rpx;
  color: #f8b24e;
  font-size: 78rpx;
}

.stage-knot {
  position: absolute;
  right: 215rpx;
  top: 126rpx;
  color: rgba(255, 232, 190, 0.4);
  font-size: 92rpx;
  font-weight: 900;
}

.topbar {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 22rpx;
  min-height: 96rpx;
}

.brand-row {
  display: flex;
  align-items: baseline;
  gap: 18rpx;
  min-width: 0;
  padding-top: 10rpx;
  padding-right: 190rpx;
}

.brand,
.hello,
.top-action text,
.section-title,
.user-name,
.phone,
.profile-number,
.profile-label,
.order-title,
.custom-title,
.agent-title,
.agent-desc,
.mine-title {
  display: block;
}

.brand {
  flex: 0 0 auto;
  color: #fff;
  font-size: 45rpx;
  font-weight: 900;
  line-height: 1.1;
}

.hello {
  overflow: hidden;
  color: rgba(255, 255, 255, 0.93);
  font-size: 25rpx;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.top-actions {
  position: absolute;
  top: 0;
  right: 0;
  display: flex;
  gap: 30rpx;
  flex: 0 0 auto;
  padding-top: 72rpx;
}

.top-action {
  color: #fff;
  font-size: 22rpx;
  text-align: center;
}

.top-icon {
  font-size: 40rpx;
  font-weight: 800;
  line-height: 1;
}

.content {
  position: relative;
  z-index: 2;
  margin-top: -168rpx;
  padding: 0 40rpx 26rpx;
}

.banner-card {
  position: relative;
  overflow: hidden;
  height: 386rpx;
  border-radius: 24rpx;
  background: transparent;
  box-shadow: 0 16rpx 34rpx rgba(170, 36, 20, 0.2);
}

.banner-image {
  display: block;
  width: 100%;
  height: 386rpx;
}

.banner-dots {
  position: relative;
  z-index: 2;
  display: flex;
  justify-content: center;
  gap: 18rpx;
  height: 24rpx;
  margin-top: -44rpx;
  margin-bottom: 20rpx;
}

.dot {
  display: block;
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.72);
}

.dot.active {
  background: var(--accent);
}

.profile-card,
.section-card,
.agent-card {
  margin-top: 24rpx;
  padding: 24rpx;
  border-radius: 24rpx;
  background: #fff;
  box-shadow: 0 10rpx 30rpx rgba(43, 35, 31, 0.06);
}

.profile-card {
  display: grid;
  grid-template-columns: 114rpx 1fr 150rpx;
  align-items: start;
  gap: 22rpx;
}

.avatar {
  width: 114rpx;
  height: 114rpx;
  border-radius: 50%;
}

.profile-main {
  min-width: 0;
}

.name-line {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.user-name {
  color: #171923;
  font-size: 32rpx;
  font-weight: 900;
}

.real-badge {
  padding: 4rpx 10rpx;
  border-radius: 8rpx;
  background: #e9f8ec;
  color: #36b96a;
  font-size: 20rpx;
  font-weight: 800;
}

.phone {
  margin-top: 10rpx;
  color: #171923;
  font-size: 25rpx;
}

.profile-stats {
  display: grid;
  grid-template-columns: 1fr 1rpx 1fr 1rpx 1fr;
  align-items: center;
  margin-top: 20rpx;
}

.profile-stats view {
  text-align: center;
}

.profile-number {
  color: #171923;
  font-size: 31rpx;
  font-weight: 900;
}

.profile-label {
  margin-top: 6rpx;
  color: #6f7480;
  font-size: 21rpx;
}

.stat-divider {
  width: 1rpx;
  height: 40rpx;
  background: #ece2dc;
}

button {
  margin: 0;
  padding: 0;
  border: 0;
}

button::after {
  border: 0;
}

.edit-btn {
  height: 58rpx;
  border: 1rpx solid #f6d8c9;
  border-radius: 999rpx;
  background: #fff6ef;
  color: #8a4a21;
  font-size: 23rpx;
  line-height: 58rpx;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
}

.section-title {
  color: #171923;
  font-size: 30rpx;
  font-weight: 900;
}

.more {
  color: #5f626a;
  font-size: 23rpx;
}

.order-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20rpx;
  margin-top: 22rpx;
}

.order-item {
  min-height: 102rpx;
  padding-top: 18rpx;
  border: 1rpx solid #ece4df;
  border-radius: 14rpx;
  text-align: center;
}

.order-icon,
.device-icon,
.service-icon {
  display: block;
  font-size: 36rpx;
  font-weight: 900;
}

.order-title {
  margin-top: 12rpx;
  color: #171923;
  font-size: 23rpx;
}

.red {
  color: var(--accent);
}

.orange {
  color: #ff7a00;
}

.blue {
  color: #3e8bff;
}

.purple {
  color: #7b61ff;
}

.green {
  color: #36b96a;
}

.compact {
  padding-bottom: 18rpx;
}

.device-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16rpx;
  margin-top: 18rpx;
}

.device-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  min-height: 72rpx;
  border: 1rpx solid #ece4df;
  border-radius: 12rpx;
  color: #171923;
  font-size: 22rpx;
}

.device-icon {
  font-size: 28rpx;
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16rpx;
  margin-top: 22rpx;
}

.service-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14rpx;
  min-height: 74rpx;
  border: 1rpx solid #ece4df;
  border-radius: 12rpx;
  color: #171923;
  font-size: 24rpx;
}

.service-icon {
  font-size: 30rpx;
}

.settings-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  row-gap: 24rpx;
  margin-top: 24rpx;
}

.setting-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  color: #3f4652;
  font-size: 23rpx;
}

.setting-icon {
  color: #8a909a;
}

.setting-arrow {
  color: #8a909a;
  font-size: 28rpx;
}

.agent-card {
  display: grid;
  grid-template-columns: 126rpx 1fr 154rpx;
  align-items: center;
  gap: 20rpx;
  background: linear-gradient(90deg, #fff8f1, #fff);
  border: 1rpx solid #f7e0d2;
}

.agent-image {
  width: 126rpx;
  height: 120rpx;
  border-radius: 12rpx;
}

.agent-main {
  min-width: 0;
}

.agent-title-line {
  display: flex;
  align-items: baseline;
  gap: 16rpx;
}

.agent-title {
  flex: 0 0 auto;
  color: #171923;
  font-size: 31rpx;
  font-weight: 900;
}

.agent-desc {
  overflow: hidden;
  color: #3f4652;
  font-size: 22rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.agent-tags {
  display: flex;
  gap: 18rpx;
  margin-top: 18rpx;
}

.agent-tags text {
  color: #e65a42;
  font-size: 20rpx;
}

.agent-btn {
  height: 68rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, var(--accent), var(--accent-dark));
  color: #fff;
  font-size: 26rpx;
  font-weight: 900;
  line-height: 68rpx;
}
</style>
