<template>
  <view class="page" :class="activeTone">
    <view class="red-stage">
      <view class="stage-art">
        <text class="firework">✦</text>
        <text class="stage-knot">{{ activeDesign.mark }}</text>
      </view>
      <view class="topbar">
        <view class="brand-row">
          <text class="brand">宴席通</text>
          <text class="hello">{{ activeDesign.homeText }}</text>
        </view>
        <view class="top-actions">
          <view class="top-action" @tap="showServiceTip()">
            <text class="top-icon">☊</text>
            <text>客服</text>
          </view>
          <view class="top-action" @tap="showServiceTip()">
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

      <view class="type-card">
        <view class="section-head">
          <text class="section-title">选择宴席类型</text>
          <view class="section-note">
            <text>请选择宴席类型，系统将自动匹配主题色与风格</text>
            <text class="info">i</text>
          </view>
        </view>
        <scroll-view scroll-x class="type-scroll" show-scrollbar="false">
          <view class="type-list">
            <view
              v-for="type in eventTypes"
              :key="type.code"
              class="type-item"
              :class="[type.tone, { active: type.code === activeType }]"
              @tap="selectType(type.code)"
            >
              <text class="type-icon">{{ type.icon }}</text>
              <text class="type-name">{{ type.name }}</text>
              <text class="type-subtitle">{{ type.subtitle }}</text>
            </view>
          </view>
        </scroll-view>
      </view>

      <view v-if="hasBanquet" class="my-card">
        <view class="section-head">
          <text class="section-title">我的宴席</text>
          <view class="head-actions">
            <text class="more primary" @tap="createBanquet()">创建宴席</text>
            <text class="more" @tap="openLatestOrCreate()">全部宴席 ›</text>
          </view>
        </view>
        <view class="banquet-box">
          <view class="banquet-main" @tap="openBanquet(latestBanquet.id)">
            <image class="banquet-cover" src="/static/home/banquet_cover.png" mode="aspectFill" />
            <view class="banquet-info">
              <text class="banquet-title">{{ latestBanquet.name }}</text>
              <view class="meta-row">
                <text class="meta-icon">◷</text>
                <text>{{ formatTime(latestBanquet.banquetTime) }}</text>
              </view>
              <view class="meta-row">
                <text class="meta-icon">⌖</text>
                <text>{{ latestBanquet.location || '地点待定' }}</text>
              </view>
              <text class="status">已发布</text>
            </view>
            <view class="banquet-stats">
              <text class="stats-label">已回执</text>
              <text class="stats-number">{{ latestRsvpGuests }}</text>
              <text class="stats-unit">人</text>
              <view class="stats-line"></view>
              <text class="stats-label">已收礼</text>
              <text class="gift-number">{{ formatMoney(latestGiftAmount) }}</text>
            </view>
          </view>
          <view class="banquet-actions">
            <view class="action-item" @tap="openInvitationTab()">
              <text class="action-icon">✉</text>
              <text>发请柬</text>
            </view>
            <view class="action-item" @tap="openLatestRsvpStats()">
              <text class="action-icon orange">◔</text>
              <text>回执统计</text>
            </view>
            <view class="action-item" @tap="openLatestOfflineGift()">
              <text class="action-icon orange">▣</text>
              <text>收礼记账</text>
            </view>
            <view class="action-item" @tap="openBanquet(latestBanquet.id)">
              <text class="action-icon">▤</text>
              <text>查看详情</text>
            </view>
          </view>
        </view>
      </view>

      <view v-else class="empty-card">
        <text class="empty-title">还没有宴席</text>
        <text class="empty-desc">选择宴席类型后创建，系统会自动匹配主题、请柬与回执入口。</text>
        <button class="empty-button" @tap="createBanquet()">立即创建</button>
      </view>

      <view class="guide-card">
        <view class="section-head">
          <text class="section-title">办席指南</text>
          <text class="more">更多 ›</text>
        </view>
        <view class="guide-list">
          <view v-for="item in guides" :key="item.title" class="guide-item" @tap="handleGuide(item.action)">
            <text class="guide-icon" :class="item.tone">{{ item.icon }}</text>
            <view>
              <text class="guide-title">{{ item.title }}</text>
              <text class="guide-desc">{{ item.desc }}</text>
            </view>
          </view>
        </view>
      </view>

      <view class="package-card">
        <view class="section-head">
          <text class="section-title">推荐套餐</text>
          <text class="more">更多 ›</text>
        </view>
        <scroll-view scroll-x class="package-scroll" show-scrollbar="false">
          <view class="package-list">
            <view v-for="item in packages" :key="item.name" class="package-item" @tap="showServiceTip()">
              <image class="package-image" :src="item.image" mode="aspectFill" />
              <text class="package-name">{{ item.name }}</text>
              <text class="package-price">¥{{ item.price }} 起</text>
            </view>
          </view>
        </scroll-view>
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

interface RsvpStats {
  totalGuests: number;
}

interface GiftSummary {
  totalAmount: number;
}

const eventTypes = [
  { code: 'WEDDING', name: '婚宴', subtitle: '喜结良缘', icon: '囍', tone: 'red', mark: '囍', homeText: '您好，办婚宴，用宴席通' },
  { code: 'BIRTHDAY', name: '寿宴', subtitle: '福寿安康', icon: '寿', tone: 'orange', mark: '寿', homeText: '寿宴筹备，福寿有序' },
  { code: 'BABY', name: '满月', subtitle: '喜迎新生', icon: '满', tone: 'pink', mark: '满', homeText: '满月礼成，亲友同喜' },
  { code: 'HOUSEWARMING', name: '乔迁', subtitle: '乔迁之喜', icon: '福', tone: 'green', mark: '福', homeText: '乔迁新居，邀亲友同贺' },
  { code: 'SCHOOL', name: '升学', subtitle: '金榜题名', icon: '学', tone: 'blue', mark: '学', homeText: '升学庆贺，前程有光' },
  { code: 'MEMORIAL', name: '追思会', subtitle: '追思缅怀', icon: '念', tone: 'black', mark: '念', homeText: '慎终追远，思念长存' },
  { code: 'OTHER', name: '其他', subtitle: '更多类型', icon: '宴', tone: 'purple', mark: '宴', homeText: '办宴席，用宴席通' }
];
const guides = [
  { title: '办席流程', desc: '了解完整办席步骤', icon: '▰', tone: 'red', action: 'create' },
  { title: '筹备清单', desc: '重要事项不遗漏', icon: '▤', tone: 'orange', action: 'create' },
  { title: '场地推荐', desc: '精选优质场地', icon: '●', tone: 'green', action: 'service' },
  { title: '注意事项', desc: '办席常见问题', icon: '▣', tone: 'purple', action: 'service' }
];
const packages = [
  { name: '浪漫粉韵', price: '10,999', image: '/static/home/package_pink.png' },
  { name: '中式喜宴', price: '12,999', image: '/static/home/package_red.png' },
  { name: '星空之恋', price: '13,999', image: '/static/home/package_blue.png' },
  { name: '简约时光', price: '9,999', image: '/static/home/package_gold.png' }
];
const banners = [
  { image: '/static/home/home_banner.png', action: 'create' },
  { image: '/static/home/package_red.png', action: 'plan' },
  { image: '/static/home/package_gold.png', action: 'invitation' }
];
const banquets = ref<Banquet[]>([]);
const latestRsvpGuests = ref(0);
const latestGiftAmount = ref(0);
const loading = ref(false);
const activeType = ref('WEDDING');
const bannerIndex = ref(0);
const hasBanquet = computed(() => banquets.value.length > 0);
const latestBanquet = computed(() => banquets.value[0] || { id: 0, name: '', eventTypeCode: '', themeCode: '', banquetTime: '', location: '' });
const latestBanquetId = computed(() => latestBanquet.value?.id || 0);
const activeTone = computed(() => eventTypes.find((item) => item.code === activeType.value)?.tone || 'red');
const activeDesign = computed(() => eventTypes.find((item) => item.code === activeType.value) || eventTypes[0]);

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '时间待定';
}

function formatMoney(value: unknown) {
  return `¥${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })}`;
}

async function refresh() {
  loading.value = true;
  try {
    banquets.value = await request<Banquet[]>('/banquets');
    await loadLatestStats();
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '加载失败', icon: 'none' });
  } finally {
    loading.value = false;
  }
}

async function loadLatestStats() {
  latestRsvpGuests.value = 0;
  latestGiftAmount.value = 0;
  const id = banquets.value[0]?.id;
  if (!id) {
    return;
  }
  const [rsvp, gifts] = await Promise.all([
    request<RsvpStats>(`/rsvp/stats?banquetId=${id}`).catch(() => ({ totalGuests: 0 })),
    request<GiftSummary>(`/gifts/summary?banquetId=${id}`).catch(() => ({ totalAmount: 0 }))
  ]);
  latestRsvpGuests.value = Number(rsvp.totalGuests || 0);
  latestGiftAmount.value = Number(gifts.totalAmount || 0);
}

function selectType(code: string) {
  activeType.value = code;
}

function createBanquet() {
  uni.navigateTo({ url: `/pages/banquet/create/index?eventTypeCode=${activeType.value}` });
}

function handleBanner(action: string) {
  if (action === 'create') {
    createBanquet();
    return;
  }
  if (action === 'plan') {
    if (latestBanquetId.value) {
      uni.navigateTo({ url: `/pages/order/plan/index?banquetId=${latestBanquetId.value}` });
      return;
    }
    createBanquet();
    return;
  }
  openInvitationTab();
}

function openBanquet(id: number) {
  if (!id) {
    createBanquet();
    return;
  }
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

function openInvitationTab() {
  uni.switchTab({ url: '/pages/invitation/index/index' });
}

function handleGuide(action: string) {
  if (action === 'create') {
    createBanquet();
    return;
  }
  showServiceTip();
}

function showServiceTip() {
  uni.showToast({ title: '客服和消息中心将在正式运营版开放', icon: 'none' });
}

onMounted(refresh);
</script>

<style scoped>
.page {
  --accent: #e60012;
  --accent-dark: #c40005;
  --accent-soft: #fff0ee;
  min-height: 100vh;
  background: #f7f7f7;
  color: #151823;
}

.page.orange {
  --accent: #d96a11;
  --accent-dark: #a64209;
  --accent-soft: #fff3e3;
}

.page.pink {
  --accent: #e7566f;
  --accent-dark: #b52d4c;
  --accent-soft: #fff0f4;
}

.page.green {
  --accent: #188356;
  --accent-dark: #0c5f3e;
  --accent-soft: #edf9f1;
}

.page.blue {
  --accent: #2563eb;
  --accent-dark: #1d4ed8;
  --accent-soft: #edf4ff;
}

.page.black {
  --accent: #2f3338;
  --accent-dark: #0d0f12;
  --accent-soft: #f1f2f4;
}

.page.purple {
  --accent: #7c3aed;
  --accent-dark: #5b21b6;
  --accent-soft: #f4efff;
}

.red-stage {
  position: relative;
  overflow: hidden;
  height: 330rpx;
  padding: calc(var(--status-bar-height) + 34rpx) 40rpx 0;
  background:
    radial-gradient(circle at 72% 38%, rgba(255, 190, 80, 0.18), transparent 26%),
    linear-gradient(135deg, #d8000f 0%, #c40005 58%, #a80000 100%);
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
  background: #f7f7f7;
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
.type-name,
.type-subtitle,
.banquet-title,
.meta-row,
.status,
.stats-label,
.stats-number,
.stats-unit,
.gift-number,
.guide-title,
.guide-desc,
.package-name,
.package-price,
.empty-title,
.empty-desc {
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

.head-actions {
  display: flex;
  gap: 18rpx;
  align-items: center;
}

.more.primary {
  color: var(--accent);
  font-weight: 900;
}

.type-card,
.my-card,
.empty-card,
.guide-card,
.package-card {
  margin-top: 24rpx;
  padding: 24rpx;
  border-radius: 24rpx;
  background: #fff;
  box-shadow: 0 10rpx 30rpx rgba(43, 35, 31, 0.06);
}

.section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18rpx;
}

.section-title {
  flex: 0 0 auto;
  color: #171923;
  font-size: 30rpx;
  font-weight: 900;
  line-height: 1.2;
}

.section-note {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10rpx;
  min-width: 0;
  color: #5f626a;
  font-size: 22rpx;
  line-height: 1.3;
  text-align: right;
}

.info {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26rpx;
  height: 26rpx;
  flex: 0 0 auto;
  border: 2rpx solid #7b7e85;
  border-radius: 50%;
  color: #7b7e85;
  font-size: 20rpx;
  font-weight: 800;
}

.type-scroll {
  width: 100%;
  margin-top: 22rpx;
  white-space: nowrap;
}

.type-list {
  display: inline-flex;
  gap: 18rpx;
  padding: 0 0 2rpx;
}

.type-item {
  box-sizing: border-box;
  width: 102rpx;
  min-height: 174rpx;
  padding: 20rpx 8rpx 16rpx;
  border: 1rpx solid #eadfd9;
  border-radius: 18rpx;
  text-align: center;
}

.type-item.red {
  background: #fff1f0;
  color: #e60012;
}

.type-item.orange {
  background: #fff7eb;
  color: #ff7a00;
}

.type-item.pink {
  background: #fff4f5;
  color: #ff6d7e;
}

.type-item.green {
  background: #f1fbf4;
  color: #36b96a;
}

.type-item.blue {
  background: #f0f6ff;
  color: #3e8bff;
}

.type-item.black {
  background: #242424;
  color: #fff;
}

.type-item.purple {
  background: #f6f1ff;
  color: #7b61ff;
}

.type-item.active {
  border-color: var(--accent);
  box-shadow: inset 0 0 0 2rpx rgba(230, 0, 18, 0.22);
}

.type-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 54rpx;
  font-size: 44rpx;
  font-weight: 900;
  line-height: 1;
}

.type-name {
  margin-top: 16rpx;
  color: #171923;
  font-size: 25rpx;
  font-weight: 900;
}

.type-item.black .type-name,
.type-item.black .type-subtitle {
  color: #fff;
}

.type-subtitle {
  margin-top: 8rpx;
  color: #6f7480;
  font-size: 20rpx;
}

.more {
  flex: 0 0 auto;
  color: #5f626a;
  font-size: 23rpx;
}

.banquet-box {
  overflow: hidden;
  margin-top: 20rpx;
  border: 1rpx solid #f2dcd6;
  border-radius: 20rpx;
  background: linear-gradient(90deg, #fff 0%, #fff8f6 100%);
}

.banquet-main {
  display: grid;
  grid-template-columns: 182rpx 1fr 170rpx;
  gap: 24rpx;
  padding: 22rpx;
}

.banquet-cover {
  width: 182rpx;
  height: 154rpx;
  border-radius: 12rpx;
}

.banquet-info {
  min-width: 0;
}

.banquet-title {
  overflow: hidden;
  color: #171923;
  font-size: 29rpx;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.meta-row {
  display: flex;
  align-items: center;
  gap: 8rpx;
  overflow: hidden;
  margin-top: 12rpx;
  color: #626773;
  font-size: 23rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.meta-icon {
  flex: 0 0 auto;
  color: #6f7480;
}

.status {
  width: fit-content;
  margin-top: 13rpx;
  padding: 5rpx 12rpx;
  border-radius: 8rpx;
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 21rpx;
  font-weight: 700;
}

.banquet-stats {
  padding-left: 28rpx;
  border-left: 1rpx solid #f0d7d0;
}

.stats-label,
.stats-unit {
  color: #686c75;
  font-size: 23rpx;
}

.stats-number,
.gift-number {
  display: inline;
  color: var(--accent);
  font-weight: 900;
}

.stats-number {
  margin-top: 8rpx;
  font-size: 34rpx;
}

.stats-unit {
  display: inline;
  margin-left: 6rpx;
}

.gift-number {
  margin-top: 8rpx;
  font-size: 31rpx;
}

.stats-line {
  height: 1rpx;
  margin: 18rpx 0;
  background: #f0d7d0;
}

.banquet-actions {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  border-top: 1rpx solid #f2dcd6;
}

.action-item {
  position: relative;
  padding: 20rpx 0 18rpx;
  color: #171923;
  font-size: 23rpx;
  text-align: center;
}

.action-item::after {
  position: absolute;
  top: 24rpx;
  right: 0;
  bottom: 24rpx;
  width: 1rpx;
  background: #f0d7d0;
  content: '';
}

.action-item:last-child::after {
  display: none;
}

.action-icon {
  display: block;
  margin-bottom: 8rpx;
  color: var(--accent);
  font-size: 34rpx;
}

.action-icon.orange {
  color: #ff7a00;
}

.empty-card {
  text-align: center;
}

.empty-title {
  color: #171923;
  font-size: 32rpx;
  font-weight: 900;
}

.empty-desc {
  margin-top: 12rpx;
  color: #6f7480;
  font-size: 24rpx;
  line-height: 1.55;
}

button {
  margin: 0;
  padding: 0;
  border: 0;
}

button::after {
  border: 0;
}

.empty-button {
  width: 260rpx;
  height: 76rpx;
  margin: 22rpx auto 0;
  border-radius: 999rpx;
  background: linear-gradient(135deg, var(--accent), var(--accent-dark));
  color: #fff;
  font-size: 27rpx;
  font-weight: 900;
  line-height: 76rpx;
}

.guide-list {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10rpx;
  margin-top: 22rpx;
}

.guide-item {
  display: flex;
  align-items: center;
  gap: 10rpx;
  min-width: 0;
}

.guide-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 54rpx;
  height: 54rpx;
  flex: 0 0 auto;
  border-radius: 14rpx;
  color: #fff;
  font-size: 28rpx;
  font-weight: 900;
}

.guide-icon.red {
  background: linear-gradient(135deg, #ff6a5f, var(--accent));
}

.guide-icon.orange {
  background: linear-gradient(135deg, #ffbb58, #ff7a00);
}

.guide-icon.green {
  background: linear-gradient(135deg, #74d88e, #36b96a);
}

.guide-icon.purple {
  background: linear-gradient(135deg, #a890ff, #7b61ff);
}

.guide-title {
  overflow: hidden;
  color: #171923;
  font-size: 22rpx;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.guide-desc {
  overflow: hidden;
  margin-top: 4rpx;
  color: #777d89;
  font-size: 18rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.package-scroll {
  width: 100%;
  margin-top: 22rpx;
  white-space: nowrap;
}

.package-list {
  display: inline-flex;
  gap: 18rpx;
  padding-bottom: 2rpx;
}

.package-item {
  overflow: hidden;
  width: 190rpx;
  border: 1rpx solid #f1ded8;
  border-radius: 16rpx;
  background: #fff;
  text-align: center;
}

.package-image {
  display: block;
  width: 190rpx;
  height: 135rpx;
}

.package-name {
  margin-top: 12rpx;
  color: #171923;
  font-size: 24rpx;
  font-weight: 800;
}

.package-price {
  margin: 7rpx 0 14rpx;
  color: var(--accent);
  font-size: 23rpx;
}
</style>
