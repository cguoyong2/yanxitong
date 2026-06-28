<template>
  <view class="page">
    <view class="red-stage">
      <view class="stage-art">
        <text class="firework">✦</text>
        <text class="stage-knot">囍</text>
      </view>
      <view class="topbar">
        <view class="brand-row">
          <text class="brand">宴席通</text>
          <text class="hello">选请柬，发请柬，更体面</text>
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
      <view class="banner-card">
        <image class="banner-image" src="/static/invitation/invitation_banner.png" mode="widthFix" />
        <view class="banner-dots">
          <text class="dot active"></text>
          <text class="dot"></text>
          <text class="dot"></text>
        </view>
      </view>

      <view class="type-card">
        <view class="section-head">
          <text class="section-title">请柬类型</text>
          <view class="section-note">
            <text>按类型查看模板</text>
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
              @tap="activeType = type.code"
            >
              <text class="type-icon">{{ type.icon }}</text>
              <text class="type-name">{{ type.name }}</text>
              <text class="type-subtitle">{{ type.subtitle }}</text>
            </view>
          </view>
        </scroll-view>

        <view class="filter-row">
          <text class="filter-title">模板筛选</text>
          <view class="filter-tabs">
            <text
              v-for="filter in filters"
              :key="filter"
              class="filter"
              :class="{ active: filter === activeFilter }"
              @tap="activeFilter = filter"
            >{{ filter }}</text>
          </view>
        </view>
      </view>

      <view class="template-card">
        <view class="section-head">
          <text class="section-title">精选模板</text>
          <text class="more">查看更多 ›</text>
        </view>
        <view class="template-list">
          <view v-if="loadingTemplates" class="template-empty">模板加载中</view>
          <view v-else-if="filteredTemplates.length === 0" class="template-empty">暂无符合条件的模板</view>
          <view v-for="item in filteredTemplates" :key="item.id" class="template-item">
            <image class="template-image" :src="templateImage(item)" mode="aspectFill" />
            <view class="template-info">
              <text class="template-name">{{ item.name }}</text>
              <text class="template-badge" :class="{ paid: item.priceType !== 'FREE' }">{{ templatePrice(item) }}</text>
            </view>
            <view class="template-actions">
              <button class="preview-btn" @tap="previewTemplate(item)">预览</button>
              <button class="use-btn" @tap="useTemplate(item)">使用</button>
            </view>
          </view>
        </view>
      </view>

      <view class="custom-card">
        <image class="custom-image" src="/static/invitation/custom_invite.png" mode="aspectFill" />
        <view class="custom-main">
          <text class="custom-title">定制请柬</text>
          <text class="custom-desc">没有合适模板？可申请专属定制请柬</text>
          <view class="custom-tags">
            <text>专属设计</text>
            <text>独一无二</text>
            <text>人工服务</text>
          </view>
        </view>
        <button class="custom-btn" @tap="showComingSoon()">申请定制</button>
      </view>

      <view class="mine-card">
        <view class="section-head">
          <text class="section-title">我的请柬</text>
          <text class="more" @tap="openMyInvitation()">全部请柬 ›</text>
        </view>
        <view v-if="myInvitation" class="mine-row" @tap="openMyInvitation()">
          <image class="mine-cover" src="/static/invitation/my_invite_cover.png" mode="aspectFill" />
          <view class="mine-main">
            <view class="mine-title-line">
              <text class="mine-title">{{ myInvitation.title }}</text>
              <text class="published">{{ myInvitation.status }}</text>
            </view>
            <view class="mine-meta">
              <text>{{ eventTypeLabel(myInvitation.eventTypeCode) }}</text>
              <text>{{ formatTime(myInvitation.banquetTime) }}</text>
            </view>
          </view>
          <view class="mine-stats">
            <view>
              <text class="mine-number">{{ myInvitation.visitCount }}</text>
              <text class="mine-label">浏览</text>
            </view>
            <view>
              <text class="mine-number">{{ myInvitation.rsvpGuests }}</text>
              <text class="mine-label">已回执</text>
            </view>
            <view>
              <text class="mine-number">一键</text>
              <text class="mine-label">分享</text>
            </view>
            <view>
              <text class="mine-number">⋯</text>
              <text class="mine-label">更多</text>
            </view>
          </view>
        </view>
        <view v-else class="mine-empty" @tap="openCreateEntry()">
          <text class="empty-title">还没有可分享的请柬</text>
          <text class="empty-desc">创建宴席后，系统会自动生成基础请柬。</text>
        </view>
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
  banquetTime?: string;
}

interface BanquetDetail {
  banquet: Banquet;
  invitation?: {
    id: number;
    title: string;
    shareSlug: string;
    visitCount?: number;
  };
}

interface RsvpStats {
  totalGuests: number;
}

interface InvitationTemplate {
  id: number;
  templateCode: string;
  typeCode: string;
  name: string;
  coverUrl?: string;
  priceType: string;
  price: number;
}

interface MyInvitation {
  id: number;
  title: string;
  shareSlug: string;
  eventTypeCode: string;
  banquetTime?: string;
  visitCount: number;
  rsvpGuests: number;
  status: string;
}

const activeType = ref('WEDDING');
const activeFilter = ref('全部');
const myInvitation = ref<MyInvitation>();
const templates = ref<InvitationTemplate[]>([]);
const loadingTemplates = ref(false);
const filters = ['全部', '免费', '付费', '定制', '热门'];
const eventTypes = [
  { code: 'WEDDING', name: '婚宴', subtitle: '喜结良缘', icon: '囍', tone: 'red' },
  { code: 'BIRTHDAY', name: '寿宴', subtitle: '福寿安康', icon: '寿', tone: 'orange' },
  { code: 'BABY', name: '满月', subtitle: '喜迎新生', icon: '🍼', tone: 'pink' },
  { code: 'HOUSEWARMING', name: '乔迁', subtitle: '乔迁之喜', icon: '⌂', tone: 'green' },
  { code: 'SCHOOL', name: '升学', subtitle: '金榜题名', icon: '◆', tone: 'blue' },
  { code: 'MEMORIAL', name: '追思会', subtitle: '追思缅怀', icon: '✿', tone: 'black' },
  { code: 'OTHER', name: '其他', subtitle: '更多类型', icon: '▦', tone: 'purple' }
];
const filteredTemplates = computed(() => templates.value
  .filter((item) => matchesEventType(item, activeType.value))
  .filter((item) => matchesFilter(item, activeFilter.value))
  .slice(0, 8));

function previewTemplate(item: InvitationTemplate) {
  uni.showToast({ title: `${item.name} 可在创建页预览`, icon: 'none' });
}

function useTemplate(item: InvitationTemplate) {
  uni.navigateTo({ url: `/pages/banquet/create/index?eventTypeCode=${activeType.value}&templateId=${item.id}` });
}

function openCreateEntry() {
  uni.navigateTo({ url: '/pages/banquet/create/index' });
}

function openMyInvitation() {
  if (!myInvitation.value) {
    openCreateEntry();
    return;
  }
  uni.navigateTo({ url: `/pages/invite/public/index?slug=${myInvitation.value.shareSlug}` });
}

function showComingSoon() {
  uni.showToast({ title: '后续版本开放', icon: 'none' });
}

function eventTypeLabel(code: string) {
  const labels: Record<string, string> = {
    WEDDING: '婚宴',
    BIRTHDAY: '寿宴',
    BABY: '满月',
    HOUSEWARMING: '乔迁',
    SCHOOL: '升学',
    MEMORIAL: '追思会',
    OTHER: '其他'
  };
  return labels[code] || code || '宴席';
}

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '时间待定';
}

function matchesEventType(item: InvitationTemplate, eventTypeCode: string) {
  const code = item.templateCode || '';
  if (eventTypeCode === 'WEDDING') return code.includes('WEDDING');
  if (eventTypeCode === 'BIRTHDAY') return code.includes('BIRTHDAY');
  if (eventTypeCode === 'BABY') return code.includes('BABY');
  if (eventTypeCode === 'HOUSEWARMING') return code.includes('HOUSE');
  if (eventTypeCode === 'SCHOOL') return code.includes('SCHOOL');
  if (eventTypeCode === 'MEMORIAL') return code.includes('MEMORIAL');
  return code.includes('GENERAL') || code.includes('CEREMONY') || code.includes('CUSTOM');
}

function matchesFilter(item: InvitationTemplate, filter: string) {
  if (filter === '免费') return item.priceType === 'FREE';
  if (filter === '付费') return item.priceType !== 'FREE';
  if (filter === '定制') return item.priceType === 'CUSTOM' || item.templateCode.includes('CUSTOM');
  if (filter === '热门') return item.templateCode.includes('WEDDING') || item.templateCode.includes('ELEGANT');
  return true;
}

function templatePrice(item: InvitationTemplate) {
  if (item.priceType === 'FREE') return '免费';
  if (item.priceType === 'PLAN_INCLUDED') return '权益包含';
  if (item.priceType === 'CUSTOM') return '定制';
  return `¥${Number(item.price || 0).toFixed(0)}`;
}

function templateImage(item: InvitationTemplate) {
  if (item.coverUrl) return item.coverUrl;
  if (item.templateCode.includes('GOLD')) return '/static/invitation/tpl_gold.png';
  if (item.templateCode.includes('CHINESE')) return '/static/invitation/tpl_chinese.png';
  if (item.templateCode.includes('MEMORIAL')) return '/static/invitation/tpl_simple.png';
  return '/static/invitation/tpl_red.png';
}

async function loadTemplates() {
  loadingTemplates.value = true;
  try {
    templates.value = await request<InvitationTemplate[]>('/meta/invitation-templates');
  } catch {
    templates.value = [];
  } finally {
    loadingTemplates.value = false;
  }
}

async function loadMyInvitation() {
  const banquets = await request<Banquet[]>('/banquets').catch(() => []);
  const latest = banquets[0];
  if (!latest?.id) {
    myInvitation.value = undefined;
    return;
  }
  const [detail, rsvp] = await Promise.all([
    request<BanquetDetail>(`/banquets/${latest.id}`).catch(() => undefined),
    request<RsvpStats>(`/rsvp/stats?banquetId=${latest.id}`).catch(() => ({ totalGuests: 0 }))
  ]);
  if (!detail?.invitation?.id) {
    myInvitation.value = undefined;
    return;
  }
  myInvitation.value = {
    id: detail.invitation.id,
    title: detail.invitation.title || `${detail.banquet.name}邀请函`,
    shareSlug: detail.invitation.shareSlug,
    eventTypeCode: detail.banquet.eventTypeCode,
    banquetTime: detail.banquet.banquetTime,
    visitCount: Number(detail.invitation.visitCount || 0),
    rsvpGuests: Number(rsvp.totalGuests || 0),
    status: '已发布'
  };
}

onMounted(() => {
  loadMyInvitation();
  loadTemplates();
});
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f7f7f7;
  color: #151823;
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
.filter-title,
.template-name,
.template-badge,
.custom-title,
.custom-desc,
.mine-title,
.published,
.mine-meta,
.mine-number,
.mine-label {
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
  border-radius: 24rpx;
  background: transparent;
  box-shadow: 0 16rpx 34rpx rgba(170, 36, 20, 0.2);
}

.banner-image {
  display: block;
  width: 100%;
  height: auto;
}

.banner-dots {
  position: absolute;
  right: 50%;
  bottom: 20rpx;
  display: flex;
  gap: 18rpx;
  transform: translateX(50%);
}

.dot {
  display: block;
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.72);
}

.dot.active {
  background: #e60012;
}

.type-card,
.template-card,
.custom-card,
.mine-card {
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
  padding-bottom: 18rpx;
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
  border-color: #e60012;
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

.filter-row {
  display: flex;
  align-items: center;
  gap: 24rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid #f0e6e0;
}

.filter-title {
  flex: 0 0 auto;
  color: #171923;
  font-size: 29rpx;
  font-weight: 900;
}

.filter-tabs {
  display: flex;
  gap: 18rpx;
  flex: 1;
  min-width: 0;
}

.filter {
  min-width: 78rpx;
  padding: 8rpx 17rpx;
  border-radius: 999rpx;
  background: #f4f4f4;
  color: #747984;
  font-size: 23rpx;
  text-align: center;
}

.filter.active {
  background: linear-gradient(135deg, #e60012, #c40005);
  color: #fff;
  font-weight: 800;
}

.more {
  flex: 0 0 auto;
  color: #5f626a;
  font-size: 23rpx;
}

.template-list {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 18rpx;
  margin-top: 22rpx;
}

.template-item {
  min-width: 0;
}

.template-empty {
  grid-column: 1 / -1;
  padding: 48rpx 20rpx;
  border: 1rpx dashed #ead8ca;
  border-radius: 18rpx;
  background: #fffaf6;
  color: #9a6a4c;
  font-size: 25rpx;
  text-align: center;
}

.template-image {
  display: block;
  width: 100%;
  aspect-ratio: 192 / 173;
  border-radius: 14rpx 14rpx 0 0;
}

.template-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8rpx;
  margin-top: 12rpx;
}

.template-name {
  overflow: hidden;
  color: #171923;
  font-size: 22rpx;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.template-badge {
  flex: 0 0 auto;
  padding: 3rpx 9rpx;
  border-radius: 999rpx;
  background: #e9f8ec;
  color: #36b96a;
  font-size: 18rpx;
}

.template-badge.paid {
  background: #fff0e5;
  color: #ff7a00;
}

.template-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8rpx;
  margin-top: 12rpx;
}

button {
  margin: 0;
  padding: 0;
  border: 0;
}

button::after {
  border: 0;
}

.preview-btn,
.use-btn {
  height: 48rpx;
  border-radius: 999rpx;
  font-size: 21rpx;
  line-height: 48rpx;
}

.preview-btn {
  border: 1rpx solid #dfe2e8;
  background: #fff;
  color: #5f6673;
}

.use-btn {
  background: linear-gradient(135deg, #e60012, #c40005);
  color: #fff;
  font-weight: 800;
}

.custom-card {
  display: grid;
  grid-template-columns: 132rpx 1fr 166rpx;
  align-items: center;
  gap: 24rpx;
  background: linear-gradient(90deg, #fff6ef, #fff);
  border: 1rpx solid #f6d8c9;
}

.custom-image {
  width: 132rpx;
  height: 107rpx;
}

.custom-title {
  color: #171923;
  font-size: 31rpx;
  font-weight: 900;
}

.custom-desc {
  margin-top: 8rpx;
  color: #5f6673;
  font-size: 23rpx;
}

.custom-tags {
  display: flex;
  gap: 12rpx;
  margin-top: 14rpx;
}

.custom-tags text {
  padding: 4rpx 10rpx;
  border: 1rpx solid #ffd6ca;
  border-radius: 999rpx;
  color: #e60012;
  font-size: 19rpx;
}

.custom-btn {
  height: 72rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #e60012, #c40005);
  color: #fff;
  font-size: 27rpx;
  font-weight: 900;
  line-height: 72rpx;
}

.mine-row {
  display: grid;
  grid-template-columns: 112rpx 1fr 314rpx;
  align-items: center;
  gap: 20rpx;
  margin-top: 22rpx;
}

.mine-cover {
  width: 112rpx;
  height: 88rpx;
  border-radius: 10rpx;
}

.mine-main {
  min-width: 0;
}

.mine-title-line {
  display: flex;
  align-items: center;
  gap: 10rpx;
}

.mine-title {
  overflow: hidden;
  color: #171923;
  font-size: 27rpx;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.published {
  flex: 0 0 auto;
  padding: 4rpx 10rpx;
  border-radius: 999rpx;
  background: #e9f8ec;
  color: #36b96a;
  font-size: 20rpx;
}

.mine-meta {
  display: flex;
  gap: 18rpx;
  margin-top: 13rpx;
  color: #6f7480;
  font-size: 22rpx;
}

.mine-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  border-left: 1rpx solid #f0e2dc;
}

.mine-stats view {
  min-width: 0;
  border-right: 1rpx solid #f0e2dc;
  text-align: center;
}

.mine-stats view:last-child {
  border-right: 0;
}

.mine-number {
  overflow: hidden;
  color: #171923;
  font-size: 25rpx;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mine-label {
  margin-top: 8rpx;
  color: #6f7480;
  font-size: 20rpx;
}

.mine-empty {
  margin-top: 22rpx;
  padding: 28rpx;
  border: 1rpx dashed #f0cfc3;
  border-radius: 18rpx;
  background: #fffaf6;
}

.empty-title,
.empty-desc {
  display: block;
}

.empty-title {
  color: #171923;
  font-size: 28rpx;
  font-weight: 900;
}

.empty-desc {
  margin-top: 10rpx;
  color: #6f7480;
  font-size: 23rpx;
  line-height: 1.5;
}
</style>
