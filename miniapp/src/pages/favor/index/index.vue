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
          <text class="hello">{{ activeTheme.favorText }}</text>
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

      <view class="summary-card">
        <view class="section-head">
          <text class="section-title">人情总览</text>
          <view class="section-note">
            <text>仅供参考，以当地习俗为准</text>
            <text class="info">i</text>
          </view>
        </view>
        <view class="summary-grid">
          <view class="summary-item" :class="{ selected: manual.direction === 'RECEIVED' }" @tap="setManualDirection('RECEIVED')">
            <text class="summary-icon receive">♥</text>
            <text class="summary-label">累计收到</text>
            <text class="summary-value red">{{ formatMoney(totalReceived) }}</text>
          </view>
          <view class="summary-item" :class="{ selected: manual.direction === 'GIVEN' }" @tap="setManualDirection('GIVEN')">
            <text class="summary-icon give">▣</text>
            <text class="summary-label">累计送出</text>
            <text class="summary-value red">{{ formatMoney(totalGiven) }}</text>
          </view>
          <view class="summary-item" @tap="scrollToRecent()">
            <text class="summary-icon people">●●</text>
            <text class="summary-label">往来对象</text>
            <text class="summary-value dark">{{ contacts.length }} 人</text>
          </view>
          <view class="summary-item" @tap="setCompareFromKeyword()">
            <text class="summary-icon balance">¥</text>
            <text class="summary-label">收支差额</text>
            <text class="summary-value red">{{ signedMoney(totalBalance) }}</text>
          </view>
        </view>
        <view class="search-box">
          <text class="search-icon">⌕</text>
          <input v-model="keyword" placeholder="搜索姓名、手机号、关系" confirm-type="search" @confirm="load()" />
        </view>
      </view>

      <view class="mine-card">
        <view class="title-line">
          <text class="section-title">我的人情</text>
          <button class="family-link" @tap="openFamily()">家庭人情 ›</button>
        </view>
        <view class="favor-card-list">
          <view class="favor-card receive-card" :class="{ selected: manual.direction === 'RECEIVED' }" @tap="setManualDirection('RECEIVED')">
            <text class="favor-card-icon">▰</text>
            <view class="favor-card-copy">
              <text class="favor-card-title">我收到的人情</text>
              <text class="favor-card-desc">查看别人送我的记录</text>
            </view>
            <text class="chevron">›</text>
          </view>
          <view class="favor-card give-card" :class="{ selected: manual.direction === 'GIVEN' }" @tap="setManualDirection('GIVEN')">
            <text class="favor-card-icon">▣</text>
            <view class="favor-card-copy">
              <text class="favor-card-title">我送出的人情</text>
              <text class="favor-card-desc">记录我给别人送的人情</text>
            </view>
            <text class="chevron">›</text>
          </view>
          <view class="favor-card compare-card" @tap="setCompareFromKeyword()">
            <text class="favor-card-icon">▤</text>
            <view class="favor-card-copy">
              <text class="favor-card-title">人情往来账</text>
              <text class="favor-card-desc">按亲友查看双方往来</text>
            </view>
            <text class="chevron">›</text>
          </view>
        </view>

        <view class="quick-head">
          <text class="quick-title">快捷操作</text>
          <text class="vip-badge">尊享</text>
        </view>
        <view class="quick-list">
          <view class="quick-item" :class="{ selected: manual.direction === 'RECEIVED' }" @tap="setManualDirection('RECEIVED')">
            <text class="quick-icon red">✓</text>
            <text>记收到</text>
          </view>
          <view class="quick-item" :class="{ selected: manual.direction === 'GIVEN' }" @tap="setManualDirection('GIVEN')">
            <text class="quick-icon orange">✓</text>
            <text>记送出</text>
          </view>
          <view class="quick-item" @tap="setCompareFromKeyword()">
            <text class="quick-icon green">⌕</text>
            <text>查往来</text>
          </view>
          <view class="quick-item" @tap="showComingSoon()">
            <text class="quick-icon purple">↥</text>
            <text>导入旧账</text>
          </view>
        </view>
      </view>

      <view class="two-column">
          <view id="recent-list" class="recent-card">
          <view class="section-head">
            <text class="section-title small">最近往来</text>
            <text class="more" @tap="showAllRecent()">更多 ›</text>
          </view>
          <view v-if="loading" class="empty">同步中</view>
          <view v-else-if="displayContacts.length === 0" class="empty">
            <text class="empty-title">暂无往来记录</text>
            <text class="empty-desc">线下记礼或手动补录后，会在这里形成亲友往来账。</text>
          </view>
          <view v-for="item in displayContacts" :key="item.contactId" class="recent-row" @tap="openDetail(item.contactId)">
            <text class="avatar">{{ contactInitial(item.contactName) }}</text>
            <view class="recent-main">
              <text class="recent-name">{{ item.contactName }}</text>
              <text class="recent-meta">{{ activeTheme.name }} · 今天 · 亲友</text>
            </view>
            <text class="direction" :class="Number(item.balance) >= 0 ? 'in' : 'out'">{{ Number(item.balance) >= 0 ? '收到' : '送出' }}</text>
            <text class="recent-amount" :class="Number(item.balance) >= 0 ? 'in' : 'out'">{{ formatMoney(Math.abs(Number(item.balance || 0))) }}</text>
          </view>
        </view>

        <view id="compare-panel" class="compare-card-panel">
          <view class="section-head">
            <text class="section-title small">往来对比</text>
            <text class="more" @tap="openCompareMore()">更多 ›</text>
          </view>
          <view v-if="compareResult" class="compare-person">
            <text class="avatar large">{{ contactInitial(compareResult.contact?.contactName) }}</text>
            <text class="compare-name">{{ compareResult.contact?.contactName || '未命名联系人' }}</text>
          </view>
          <template v-if="compareResult">
            <text class="compare-line">他送我合计 <text class="red-text">{{ formatMoney(compareResult.receivedAmount) }}</text></text>
            <text class="compare-line">我送他合计 <text class="green-text">{{ formatMoney(compareResult.givenAmount) }}</text></text>
            <view class="compare-divider"></view>
            <text class="compare-line">差额：<text class="red-text">{{ signedMoney(compareResult.balance) }}</text></text>
          </template>
          <view v-else class="empty compact">
            <text class="empty-title">暂无对比对象</text>
            <text class="empty-desc">输入姓名或先添加一条人情记录。</text>
          </view>
          <button class="detail-btn" @tap="openCompareDetail()">查看对比</button>
        </view>
      </view>

      <view id="manual-form" class="record-panel">
        <view class="section-head">
          <text class="section-title">手动记账</text>
          <text class="mode-badge">{{ directions[directionIndex].label }}</text>
        </view>
        <view class="record-form">
          <input v-model="manual.contactName" class="input" placeholder="对象姓名" />
          <input v-model.number="manual.amount" class="input" type="digit" placeholder="金额" />
          <picker :range="directions" range-key="label" @change="onDirectionChange">
            <view class="input picker">方向：{{ directions[directionIndex].label }}</view>
          </picker>
          <input v-model="manual.note" class="input" :placeholder="manualNotePlaceholder" />
          <button class="submit-btn" @tap="addManual()">添加记录</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { request } from '../../../api/client';
import { eventThemeFor, readActiveEventType } from '../../../utils/event-theme';

interface FavorContact {
  contactId: number;
  contactName: string;
  receivedAmount: number;
  givenAmount: number;
  balance: number;
}

interface FavorCompare {
  contact: { id?: number; contactName: string };
  receivedAmount: number;
  givenAmount: number;
  balance: number;
}

const directions = [
  { label: '他送我的', value: 'RECEIVED' },
  { label: '我送他的', value: 'GIVEN' }
];
const contacts = ref<FavorContact[]>([]);
const keyword = ref('');
const compareName = ref('');
const compareResult = ref<FavorCompare>();
const loading = ref(false);
const directionIndex = ref(0);
const bannerIndex = ref(0);
const showAllContacts = ref(false);
const activeType = ref(readActiveEventType());
const manual = reactive({ contactName: '', amount: 0, direction: 'RECEIVED', note: '' });
const activeTheme = computed(() => eventThemeFor(activeType.value));
const manualNotePlaceholder = computed(() => activeTheme.value.code === 'MEMORIAL' ? '备注，例如：亲友追思心意' : `备注，例如：朋友${activeTheme.value.name}往来`);
const banners = [
  { image: '/static/favor/favor_banner.png', action: 'manual-received' },
  { image: '/static/home/package_gold.png', action: 'manual-given' },
  { image: '/static/home/package_red.png', action: 'compare' }
];
const sourceContacts = computed(() => contacts.value);
const displayContacts = computed(() => showAllContacts.value ? sourceContacts.value : sourceContacts.value.slice(0, 4));
const totalReceived = computed(() => sum(sourceContacts.value.map((contact) => contact.receivedAmount)));
const totalGiven = computed(() => sum(sourceContacts.value.map((contact) => contact.givenAmount)));
const totalBalance = computed(() => totalReceived.value - totalGiven.value);

function onDirectionChange(event: { detail: { value: number | string } }) {
  directionIndex.value = Number(event.detail.value);
  manual.direction = directions[directionIndex.value].value;
}

function setManualDirection(direction: string) {
  const index = directions.findIndex((item) => item.value === direction);
  if (index >= 0) {
    directionIndex.value = index;
    manual.direction = direction;
  }
  setTimeout(() => uni.pageScrollTo({ selector: '#manual-form', duration: 260 }), 30);
  uni.showToast({ title: direction === 'GIVEN' ? '已切换为记送出' : '已切换为记收到', icon: 'none' });
}

function setCompareFromKeyword() {
  compareName.value = keyword.value || sourceContacts.value[0]?.contactName || '';
  runDefaultCompare();
  uni.pageScrollTo({ selector: '#recent-list', duration: 220 });
}

function scrollToRecent() {
  uni.pageScrollTo({ selector: '#recent-list', duration: 220 });
}

function showAllRecent() {
  showAllContacts.value = true;
  scrollToRecent();
  uni.showToast({ title: `已展开 ${sourceContacts.value.length} 条往来`, icon: 'none' });
}

function openCompareMore() {
  setCompareFromKeyword();
  uni.pageScrollTo({ selector: '#compare-panel', duration: 220 });
}

function handleBanner(action: string) {
  if (action === 'manual-given') {
    setManualDirection('GIVEN');
    return;
  }
  if (action === 'compare') {
    setCompareFromKeyword();
    return;
  }
  setManualDirection('RECEIVED');
}

async function load() {
  const query = keyword.value ? `?keyword=${encodeURIComponent(keyword.value)}` : '';
  loading.value = true;
  try {
    contacts.value = await request<FavorContact[]>(`/favor/contacts${query}`);
    showAllContacts.value = false;
  } finally {
    loading.value = false;
  }
}

async function addManual() {
  if (!manual.contactName.trim()) {
    uni.showToast({ title: '请输入对象姓名', icon: 'none' });
    return;
  }
  if (!Number(manual.amount) || Number(manual.amount) <= 0) {
    uni.showToast({ title: '请输入有效金额', icon: 'none' });
    return;
  }
  await request('/favor/manual', { method: 'POST', data: { ...manual, amount: Number(manual.amount) } });
  uni.showToast({ title: '补录成功', icon: 'success' });
  manual.contactName = '';
  manual.amount = 0;
  manual.note = '';
  await load();
}

async function runDefaultCompare() {
  const name = (compareName.value || sourceContacts.value[0]?.contactName || '').trim();
  if (!name) {
    uni.showToast({ title: '请先输入或选择对象', icon: 'none' });
    return;
  }
  compareName.value = name;
  try {
    compareResult.value = await request<FavorCompare>(`/favor/compare?contactName=${encodeURIComponent(name)}`);
  } catch {
    compareResult.value = undefined;
  }
}

async function openCompareDetail() {
  if (!compareResult.value) {
    await runDefaultCompare();
  }
  const id = compareResult.value?.contact?.id || sourceContacts.value.find((item) => item.contactName === compareName.value)?.contactId;
  if (id) {
    openDetail(Number(id));
    return;
  }
  uni.showToast({ title: '请先选择有往来的对象', icon: 'none' });
}

function openDetail(id: number) {
  uni.navigateTo({ url: `/pages/favor/detail/index?id=${id}` });
}

function openFamily() {
  uni.navigateTo({ url: '/pages/favor/family/index' });
}

function showComingSoon() {
  uni.showToast({ title: '批量导入和协作能力将在后续版本开放', icon: 'none' });
}

function sum(values: number[]) {
  return values.reduce((total, value) => total + Number(value || 0), 0);
}

function formatMoney(value: unknown) {
  return `¥${Number(value || 0).toLocaleString('zh-CN', { maximumFractionDigits: 0 })}`;
}

function signedMoney(value: unknown) {
  const amount = Number(value || 0);
  if (amount > 0) {
    return `+${formatMoney(amount)}`;
  }
  if (amount < 0) {
    return `-${formatMoney(Math.abs(amount))}`;
  }
  return '持平';
}

function contactInitial(name?: string) {
  return (name || '人').slice(0, 1);
}

onMounted(load);
onShow(() => {
  activeType.value = readActiveEventType();
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
.summary-label,
.summary-value,
.type-name,
.favor-card-title,
.favor-card-desc,
.quick-title,
.quick-item text,
.recent-name,
.recent-meta,
.compare-name,
.compare-line,
.empty,
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
  background: #e60012;
}

.summary-card,
.mine-card,
.recent-card,
.compare-card-panel,
.record-panel {
  margin-top: 24rpx;
  padding: 24rpx;
  border-radius: 24rpx;
  background: #fff;
  box-shadow: 0 10rpx 30rpx rgba(43, 35, 31, 0.06);
}

.section-head,
.title-line {
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

.section-title.small {
  font-size: 28rpx;
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

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  margin-top: 18rpx;
  padding: 18rpx 0;
  border: 1rpx solid #f1e6df;
  border-radius: 18rpx;
}

.summary-item {
  position: relative;
  padding: 10rpx 0;
  border-radius: 16rpx;
  text-align: center;
}

.summary-item.selected {
  background: #fff0ee;
  box-shadow: inset 0 0 0 2rpx rgba(230, 0, 18, 0.16);
}

.summary-item::after {
  position: absolute;
  top: 12rpx;
  right: 0;
  bottom: 12rpx;
  width: 1rpx;
  background: #f0e2dc;
  content: '';
}

.summary-item:last-child::after {
  display: none;
}

.summary-icon {
  display: block;
  height: 44rpx;
  font-size: 36rpx;
  font-weight: 900;
  line-height: 44rpx;
}

.summary-icon.receive,
.red,
.red-text {
  color: #e60012;
}

.summary-icon.give {
  color: #ff7a00;
}

.summary-icon.people,
.green-text {
  color: #36b96a;
}

.summary-icon.balance {
  color: #7b61ff;
}

.summary-label {
  margin-top: 8rpx;
  color: #585f6c;
  font-size: 21rpx;
}

.summary-value {
  margin-top: 10rpx;
  font-size: 29rpx;
  font-weight: 900;
  line-height: 1.1;
}

.summary-value.dark {
  color: #171923;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 12rpx;
  height: 70rpx;
  margin-top: 18rpx;
  padding: 0 22rpx;
  border: 1rpx solid #e8e0dc;
  border-radius: 999rpx;
  background: #fff;
}

.search-icon {
  color: #a0a6b0;
  font-size: 34rpx;
}

.search-box input {
  flex: 1;
  min-width: 0;
  color: #171923;
  font-size: 25rpx;
}

.family-link {
  margin: -4rpx 0 0;
  padding: 0 18rpx;
  border: 1rpx solid #ffe0dc;
  border-radius: 999rpx;
  background: #fff4f2;
  color: #e60012;
  font-size: 24rpx;
  font-weight: 800;
  line-height: 52rpx;
}

button {
  padding: 0;
  border: 0;
}

button::after {
  border: 0;
}

.favor-card-list {
  display: grid;
  grid-template-columns: 1fr;
  gap: 18rpx;
  margin-top: 22rpx;
}

.favor-card {
  display: flex;
  align-items: center;
  gap: 16rpx;
  min-height: 132rpx;
  padding: 18rpx;
  border: 1rpx solid #f1ded8;
  border-radius: 16rpx;
}

.favor-card.selected {
  border-color: #e60012;
  box-shadow: inset 0 0 0 2rpx rgba(230, 0, 18, 0.12);
}

.receive-card {
  background: #fff4f3;
}

.give-card {
  background: #fff8ec;
}

.compare-card {
  background: #f8f5ff;
}

.favor-card-icon {
  flex: 0 0 auto;
  color: #e60012;
  font-size: 46rpx;
}

.give-card .favor-card-icon {
  color: #ff7a00;
}

.compare-card .favor-card-icon {
  color: #7b61ff;
}

.favor-card-copy {
  flex: 1;
  min-width: 0;
}

.favor-card-title {
  overflow: hidden;
  color: #171923;
  font-size: 23rpx;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.favor-card-desc {
  overflow: hidden;
  margin-top: 8rpx;
  color: #5f6673;
  font-size: 19rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chevron,
.more {
  flex: 0 0 auto;
  color: #8a909a;
  font-size: 34rpx;
}

.quick-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 28rpx;
}

.quick-title {
  font-size: 29rpx;
  font-weight: 900;
}

.vip-badge {
  padding: 5rpx 14rpx;
  border-radius: 999rpx;
  background: #fff2db;
  color: #a36b1e;
  font-size: 22rpx;
}

.quick-list {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16rpx;
  margin-top: 18rpx;
}

.quick-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  color: #171923;
  font-size: 24rpx;
}

.quick-item.selected {
  border-radius: 999rpx;
  background: #fff0ee;
  color: #e60012;
  font-weight: 900;
}

.quick-icon {
  display: flex !important;
  align-items: center;
  justify-content: center;
  width: 54rpx;
  height: 54rpx;
  border-radius: 14rpx;
  color: #fff;
  font-size: 28rpx;
  font-weight: 900;
}

.quick-icon.red {
  background: linear-gradient(135deg, #ff6a5f, #e60012);
}

.quick-icon.orange {
  background: linear-gradient(135deg, #ffbb58, #ff7a00);
}

.quick-icon.green {
  background: linear-gradient(135deg, #74d88e, #36b96a);
}

.quick-icon.purple {
  background: linear-gradient(135deg, #a890ff, #7b61ff);
}

.two-column {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18rpx;
}

.recent-card,
.compare-card-panel {
  min-width: 0;
}

.recent-row {
  display: grid;
  grid-template-columns: 50rpx 1fr auto auto;
  align-items: center;
  gap: 10rpx;
  padding: 14rpx 0;
  border-bottom: 1rpx solid #f0e6e0;
}

.recent-row:last-child {
  border-bottom: 0;
}

.avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 50rpx;
  height: 50rpx;
  border-radius: 50%;
  background: #fde2d9;
  color: #8d421f;
  font-size: 24rpx;
  font-weight: 900;
}

.avatar.large {
  width: 70rpx;
  height: 70rpx;
  font-size: 30rpx;
}

.recent-main {
  min-width: 0;
}

.recent-name {
  overflow: hidden;
  color: #171923;
  font-size: 24rpx;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recent-meta {
  overflow: hidden;
  margin-top: 5rpx;
  color: #9398a3;
  font-size: 19rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.direction {
  padding: 4rpx 9rpx;
  border-radius: 999rpx;
  font-size: 19rpx;
  font-weight: 800;
}

.direction.in {
  background: #fff0ee;
  color: #e60012;
}

.direction.out {
  background: #edf9f0;
  color: #168a45;
}

.recent-amount {
  font-size: 23rpx;
  font-weight: 900;
}

.recent-amount.in {
  color: #e60012;
}

.recent-amount.out {
  color: #168a45;
}

.compare-person {
  display: flex;
  align-items: center;
  gap: 18rpx;
  margin-top: 18rpx;
}

.compare-name {
  color: #171923;
  font-size: 28rpx;
  font-weight: 900;
}

.compare-line {
  margin-top: 16rpx;
  color: #3f4652;
  font-size: 23rpx;
}

.compare-divider {
  height: 1rpx;
  margin: 18rpx 0 0;
  background: #f0e6e0;
}

.detail-btn {
  width: 100%;
  height: 58rpx;
  margin-top: 18rpx;
  border: 1rpx solid #ff8a80;
  border-radius: 999rpx;
  background: #fff;
  color: #e60012;
  font-size: 24rpx;
  font-weight: 800;
  line-height: 58rpx;
}

.record-panel {
  display: block;
}

.mode-badge {
  flex: 0 0 auto;
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  background: #fff0ee;
  color: #e60012;
  font-size: 22rpx;
  font-weight: 900;
}

.record-form {
  display: grid;
  gap: 14rpx;
  margin-top: 18rpx;
}

.input {
  box-sizing: border-box;
  min-height: 72rpx;
  padding: 0 20rpx;
  border: 1rpx solid #e8e0dc;
  border-radius: 12rpx;
  background: #fff;
  font-size: 24rpx;
}

.picker {
  line-height: 72rpx;
}

.submit-btn {
  height: 78rpx;
  border-radius: 12rpx;
  background: linear-gradient(135deg, #e60012, #c40005);
  color: #fff;
  font-size: 27rpx;
  font-weight: 900;
  line-height: 78rpx;
}

.empty {
  padding: 30rpx 0;
  color: #8a909a;
  text-align: center;
}
</style>
