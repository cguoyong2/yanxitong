<template>
  <view class="page">
    <view class="banner">
      <text class="banner-knot">囍</text>
      <text class="eyebrow">请柬中心</text>
      <text class="title">选模板，发邀请</text>
      <text class="subtitle">按宴席类型推荐模板，先支持基础请柬编辑与公开页分享。</text>
    </view>

    <view class="type-card">
      <view class="section-head">
        <text class="section-title">宴席类型</text>
        <text class="section-meta">选择类型查看模板</text>
      </view>
      <view class="type-grid">
        <view v-for="type in eventTypes" :key="type.code" class="type-item" :class="{ active: type.code === activeType }" @tap="activeType = type.code">
          <text class="type-icon">{{ type.icon }}</text>
          <text class="type-label">{{ type.label }}</text>
        </view>
      </view>
    </view>

    <view class="panel">
      <view class="section-head">
        <text class="section-title">推荐模板</text>
        <text class="section-meta">{{ activeTypeLabel }}</text>
      </view>
      <view class="template-grid">
        <view v-for="item in templates" :key="item.id" class="template-card" @tap="openTemplate(item)">
          <view class="template-cover" :class="item.tone">
            <text class="template-mark">{{ item.mark }}</text>
          </view>
          <text class="template-title">{{ item.name }}</text>
          <text class="template-price">{{ item.price === 0 ? '免费' : `¥${item.price}` }}</text>
        </view>
      </view>
    </view>

    <view class="panel">
      <view class="section-head">
        <text class="section-title">我的请柬</text>
        <button class="plain-btn" @tap="createInvitation()">创建请柬</button>
      </view>
      <view class="invite-row">
        <view>
          <text class="invite-title">基础请柬编辑</text>
          <text class="invite-meta">编辑标题、时间、地点和祝福文案</text>
        </view>
        <text class="arrow">›</text>
      </view>
      <view class="invite-row" @tap="openPublicDemo()">
        <view>
          <text class="invite-title">请柬公开页</text>
          <text class="invite-meta">分享访问、回执、随礼入口预留</text>
        </view>
        <text class="arrow">›</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';

const activeType = ref('WEDDING');
const eventTypes = [
  { code: 'WEDDING', label: '婚宴', icon: '囍' },
  { code: 'BIRTHDAY', label: '寿宴', icon: '寿' },
  { code: 'BABY', label: '满月', icon: '满' },
  { code: 'HOUSEWARMING', label: '乔迁', icon: '乔' },
  { code: 'SCHOOL', label: '升学', icon: '学' },
  { code: 'MEMORIAL', label: '追思', icon: '念' }
];
const templates = [
  { id: 1, name: '喜结良缘', price: 0, mark: '囍', tone: 'red' },
  { id: 2, name: '轻奢婚礼', price: 29, mark: '礼', tone: 'gold' },
  { id: 3, name: '中式国风', price: 49, mark: '宴', tone: 'dark' },
  { id: 4, name: '简约极简', price: 0, mark: '简', tone: 'light' }
];
const activeTypeLabel = computed(() => eventTypes.find((type) => type.code === activeType.value)?.label || '婚宴');

function openTemplate(item: { name: string }) {
  uni.showToast({ title: `${item.name} 模板已选`, icon: 'none' });
}

function createInvitation() {
  uni.navigateTo({ url: '/pages/banquet/create/index' });
}

function openPublicDemo() {
  uni.showToast({ title: '请从宴席管理台打开公开页', icon: 'none' });
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

.banner {
  position: relative;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  padding: 32rpx;
  border-radius: 16rpx;
  background:
    radial-gradient(circle at 80% 24%, rgba(255, 232, 190, 0.35), transparent 29%),
    linear-gradient(135deg, #e60012, #b80000);
  color: #fff;
  box-shadow: 0 18rpx 34rpx rgba(184, 0, 0, 0.16);
}

.banner-knot {
  position: absolute;
  right: 44rpx;
  bottom: 8rpx;
  color: rgba(255, 255, 255, 0.16);
  font-size: 150rpx;
  font-weight: 900;
}

.eyebrow,
.title,
.subtitle,
.section-title,
.section-meta,
.type-label,
.template-title,
.template-price,
.invite-title,
.invite-meta {
  display: block;
}

.eyebrow {
  color: #ffe8bf;
  font-size: 24rpx;
  font-weight: 700;
}

.title {
  margin-top: 20rpx;
  font-size: 44rpx;
  font-weight: 800;
}

.subtitle {
  margin-top: 14rpx;
  max-width: 490rpx;
  color: rgba(255, 255, 255, 0.86);
  font-size: 24rpx;
  line-height: 1.55;
}

.type-card,
.panel {
  margin-top: 20rpx;
  padding: 24rpx;
  border: 1rpx solid #eeeeee;
  border-radius: 12rpx;
  background: #fff;
  box-shadow: 0 10rpx 24rpx rgba(30, 18, 12, 0.04);
}

.section-head,
.invite-row {
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

.type-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14rpx;
  margin-top: 18rpx;
}

.type-item {
  padding: 20rpx 10rpx;
  border: 1rpx solid #f0d4bd;
  border-radius: 12rpx;
  background: #fffaf4;
  text-align: center;
}

.type-item.active {
  border-color: #e60012;
  background: #fff0ee;
}

.type-icon {
  display: block;
  color: #c71916;
  font-size: 34rpx;
  font-weight: 800;
}

.type-label {
  margin-top: 6rpx;
  color: #151823;
  font-size: 24rpx;
  font-weight: 700;
}

.template-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16rpx;
  margin-top: 18rpx;
}

.template-card {
  padding: 14rpx;
  border: 1rpx solid #eeeeee;
  border-radius: 12rpx;
  background: #fff;
}

.template-cover {
  display: flex;
  align-items: center;
  justify-content: center;
  aspect-ratio: 4 / 3;
  border-radius: 10rpx;
}

.template-cover.red {
  background: linear-gradient(135deg, #e60012, #b80000);
}

.template-cover.gold {
  background: linear-gradient(135deg, #fff2d3, #d9a759);
}

.template-cover.dark {
  background: linear-gradient(135deg, #2a2d35, #0d0f14);
}

.template-cover.light {
  background: linear-gradient(135deg, #fff, #f4eee7);
}

.template-mark {
  color: #ffe8bf;
  font-size: 52rpx;
  font-weight: 900;
}

.template-cover.light .template-mark {
  color: #c71916;
}

.template-title {
  margin-top: 12rpx;
  font-size: 26rpx;
  font-weight: 800;
}

.template-price {
  margin-top: 5rpx;
  color: #c71916;
  font-size: 22rpx;
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

.invite-row {
  padding: 22rpx 0;
  border-bottom: 1rpx solid #eeeeee;
}

.invite-row:last-child {
  border-bottom: 0;
}

.invite-title {
  font-size: 28rpx;
  font-weight: 800;
}

.invite-meta {
  margin-top: 7rpx;
  color: #7a7f8c;
  font-size: 23rpx;
}

.arrow {
  color: #b6bbc7;
  font-size: 40rpx;
}
</style>
