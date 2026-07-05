<template>
  <view class="page" v-if="detail" :class="activeTheme.tone">
    <view class="hero-card">
      <text class="hero-mark">{{ activeTheme.mark }}</text>
      <view class="avatar">{{ contactInitial(detail.contact?.contactName) }}</view>
      <text class="hero-label">{{ activeTheme.favorText }}</text>
      <text class="hero-name">{{ detail.contact?.contactName || '未命名联系人' }}</text>
      <text class="hero-note">{{ balanceText(detail.balance) }}</text>
      <view class="balance-box">
        <text class="balance-label">当前差额</text>
        <text class="balance-amount" :class="balanceClass(detail.balance)">{{ signedMoney(detail.balance) }}</text>
      </view>
    </view>

    <view class="summary-grid">
      <view class="summary-item">
        <text class="summary-label">他送我的{{ activeTheme.giftLabel }}</text>
        <text class="summary-value red">{{ formatMoney(detail.receivedAmount) }}</text>
      </view>
      <view class="summary-item">
        <text class="summary-label">我送他的{{ activeTheme.giftLabel }}</text>
        <text class="summary-value green">{{ formatMoney(detail.givenAmount) }}</text>
      </view>
      <view class="summary-item">
        <text class="summary-label">往来笔数</text>
        <text class="summary-value">{{ entries.length }}</text>
      </view>
    </view>

    <view class="action-card">
      <button class="ghost-button" @tap="copySummary()">复制往来摘要</button>
      <button class="ghost-button" @tap="goFavor()">返回人情账本</button>
    </view>

    <view class="section-card">
      <view class="section-head">
        <text class="section-title">往来明细</text>
        <text class="section-note">{{ entries.length }} 条</text>
      </view>
      <view v-if="entries.length === 0" class="empty">
        <text>暂无人情往来明细</text>
      </view>
      <view v-for="entry in entries" :key="entry.id" class="entry-row" @tap="copyEntry(entry)">
        <text class="entry-badge" :class="entry.direction === 'GIVEN' ? 'given' : 'received'">{{ entry.direction === 'GIVEN' ? '送' : '收' }}</text>
        <view class="entry-main">
          <text class="entry-title">{{ directionLabel(entry.direction) }}</text>
          <text class="entry-meta">{{ sourceLabel(entry.sourceType) }} · {{ formatTime(entry.occurredAt) }}</text>
          <text v-if="entry.banquetId" class="entry-meta">宴席 ID：{{ entry.banquetId }}</text>
          <text v-if="entry.note" class="entry-note">{{ entry.note }}</text>
        </view>
        <text class="entry-amount" :class="entry.direction === 'GIVEN' ? 'given' : 'received'">
          {{ entry.direction === 'GIVEN' ? '-' : '+' }}{{ formatMoney(entry.amount) }}
        </text>
      </view>
    </view>
  </view>
  <view class="page loading" :class="activeTheme.tone" v-else-if="pageState === 'loading'">加载中</view>
  <view class="page state-page" :class="activeTheme.tone" v-else>
    <text class="state-title">人情详情加载失败</text>
    <text class="state-desc">该联系人可能不存在，或网络暂时不可用。</text>
    <button class="state-button" @tap="bootstrap()">重新加载</button>
    <button class="state-link" @tap="goFavor()">返回人情账本</button>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { request } from '../../../api/client';
import { eventThemeFor, fetchBanquetEventType, readActiveEventType, writeActiveEventType } from '../../../utils/event-theme';

interface FavorDetail {
  contact: { contactName: string };
  receivedAmount: number;
  givenAmount: number;
  balance: number;
  entries: Array<{ id: number; direction: string; amount: number; sourceType: string; banquetId?: number; occurredAt?: string; note?: string }>;
}

const detail = ref<FavorDetail>();
const pageState = ref<'loading' | 'ready' | 'error'>('loading');
const eventType = ref(readActiveEventType());
const activeTheme = computed(() => eventThemeFor(eventType.value));
const entries = computed(() => detail.value?.entries || []);

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '时间待定';
}

function formatMoney(value: unknown) {
  return `¥${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })}`;
}

function signedMoney(value: unknown) {
  const amount = Number(value || 0);
  if (amount > 0) return `+${formatMoney(amount)}`;
  if (amount < 0) return `-${formatMoney(Math.abs(amount))}`;
  return formatMoney(0);
}

function directionLabel(value: string) {
  return value === 'GIVEN' ? `我送他的${activeTheme.value.giftLabel}` : `他送我的${activeTheme.value.giftLabel}`;
}

function sourceLabel(value: string) {
  const labels: Record<string, string> = {
    ONLINE_GIFT: activeTheme.value.onlineGiftLabel,
    ONSITE_QR: '现场扫码',
    CASH: activeTheme.value.offlineGiftLabel,
    MANUAL: '手动补录'
  };
  return labels[value] || value;
}

function balanceText(value: unknown) {
  const amount = Number(value || 0);
  if (amount > 0) {
    return '对方累计送入更多';
  }
  if (amount < 0) {
    return '我方累计送出更多';
  }
  return '双方往来持平';
}

function balanceClass(value: unknown) {
  const amount = Number(value || 0);
  if (amount > 0) {
    return 'positive';
  }
  if (amount < 0) {
    return 'negative';
  }
  return 'neutral';
}

function contactInitial(name?: string) {
  return (name || '人').slice(0, 1);
}

function copySummary() {
  if (!detail.value) {
    return;
  }
  const name = detail.value.contact?.contactName || '未命名联系人';
  const text = `${name} 人情往来：他送我的${activeTheme.value.giftLabel} ${formatMoney(detail.value.receivedAmount)}，我送他的${activeTheme.value.giftLabel} ${formatMoney(detail.value.givenAmount)}，差额 ${signedMoney(detail.value.balance)}。`;
  uni.setClipboardData({
    data: text,
    success: () => uni.showToast({ title: '已复制摘要', icon: 'success' }),
    fail: () => uni.showToast({ title: '复制失败', icon: 'none' })
  });
}

function copyEntry(entry: FavorDetail['entries'][number]) {
  const text = `${directionLabel(entry.direction)} ${formatMoney(entry.amount)} ${sourceLabel(entry.sourceType)} ${formatTime(entry.occurredAt)} ${entry.note || ''}`.trim();
  uni.setClipboardData({
    data: text,
    success: () => uni.showToast({ title: '已复制明细', icon: 'success' }),
    fail: () => uni.showToast({ title: '复制失败', icon: 'none' })
  });
}

async function bootstrap() {
  const pages = getCurrentPages();
  const current = pages[pages.length - 1] as unknown as { options?: Record<string, string> };
  const id = current.options?.id;
  if (id) {
    pageState.value = 'loading';
    try {
      detail.value = await request<FavorDetail>(`/favor/contacts/${id}`);
      const banquetId = detail.value.entries.find((entry) => entry.banquetId)?.banquetId;
      if (banquetId) {
        eventType.value = writeActiveEventType(await fetchBanquetEventType(String(banquetId), request, eventType.value));
      }
      pageState.value = 'ready';
    } catch {
      pageState.value = 'error';
    }
    return;
  }
  uni.showToast({ title: '缺少联系人信息', icon: 'none' });
  pageState.value = 'error';
}

function goFavor() {
  uni.switchTab({ url: '/pages/favor/index/index' });
}

onMounted(bootstrap);
</script>

<style scoped>
.page {
  --accent: #e60012;
  --accent-dark: #c40005;
  --accent-soft: #fff0ee;
  --page-bg: #fff8ef;
  --accent-shadow: rgba(184, 17, 21, 0.22);
  min-height: 100vh;
  padding: 24rpx;
  background: var(--page-bg);
  box-sizing: border-box;
  color: #171c2a;
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

.loading {
  display: grid;
  place-items: center;
  color: #8a7768;
}

.state-page {
  padding-top: 120rpx;
  text-align: center;
}

.state-title,
.state-desc {
  display: block;
}

.state-title {
  font-size: 40rpx;
  font-weight: 900;
}

.state-desc {
  margin-top: 18rpx;
  color: #8a7768;
  font-size: 27rpx;
  line-height: 1.6;
}

.state-button,
.state-link {
  margin-top: 34rpx;
}

.state-button {
  height: 88rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, var(--accent), var(--accent-dark));
  color: #fff8df;
  font-size: 30rpx;
  font-weight: 900;
}

.state-link {
  background: transparent;
  color: var(--accent);
  font-size: 27rpx;
}

.hero-card {
  position: relative;
  overflow: hidden;
  padding: 34rpx;
  border-radius: 28rpx;
  background:
    radial-gradient(circle at 84% 18%, rgba(255, 217, 150, 0.38), transparent 180rpx),
    linear-gradient(135deg, var(--accent) 0%, var(--accent-dark) 62%, var(--accent-dark) 100%);
  box-shadow: 0 16rpx 42rpx var(--accent-shadow);
}

.hero-mark {
  position: absolute;
  right: 40rpx;
  top: 30rpx;
  color: rgba(255, 239, 206, 0.3);
  font-family: serif;
  font-size: 100rpx;
  font-weight: 900;
}

.avatar {
  display: grid;
  place-items: center;
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  background: rgba(255, 247, 225, 0.92);
  color: var(--accent);
  font-size: 42rpx;
  font-weight: 900;
}

.hero-label,
.hero-name,
.hero-note {
  display: block;
}

.hero-label {
  margin-top: 24rpx;
  color: #ffe2ba;
  font-size: 25rpx;
  font-weight: 800;
}

.hero-name {
  margin-top: 10rpx;
  color: #fff8df;
  font-size: 54rpx;
  font-weight: 900;
}

.hero-note {
  margin-top: 10rpx;
  color: rgba(255, 248, 232, 0.94);
  font-size: 27rpx;
}

.balance-box {
  margin-top: 26rpx;
  padding: 22rpx;
  border-radius: 20rpx;
  background: rgba(255, 255, 255, 0.16);
}

.balance-label,
.balance-amount {
  display: block;
}

.balance-label {
  color: rgba(255, 248, 232, 0.82);
  font-size: 24rpx;
}

.balance-amount {
  margin-top: 8rpx;
  font-size: 46rpx;
  font-weight: 900;
}

.balance-amount.positive {
  color: #fff2cc;
}

.balance-amount.negative {
  color: #cfe7ff;
}

.balance-amount.neutral {
  color: #fff;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14rpx;
  margin-top: 24rpx;
}

.action-card {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
  margin-top: 24rpx;
}

.ghost-button {
  height: 78rpx;
  margin: 0;
  border: 1rpx solid #ead8ca;
  border-radius: 16rpx;
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 26rpx;
  font-weight: 900;
  line-height: 78rpx;
}

.ghost-button::after {
  border: 0;
}

.summary-item,
.section-card {
  border: 1rpx solid #f0dfcf;
  border-radius: 22rpx;
  background: #fff;
  box-shadow: 0 12rpx 32rpx rgba(82, 45, 24, 0.07);
}

.summary-item {
  padding: 22rpx 10rpx;
  text-align: center;
}

.summary-label,
.summary-value {
  display: block;
}

.summary-label {
  color: #8a7768;
  font-size: 22rpx;
  font-weight: 700;
}

.summary-value {
  margin-top: 10rpx;
  color: #171c2a;
  font-size: 30rpx;
  font-weight: 900;
}

.summary-value.red {
  color: var(--accent);
}

.summary-value.green {
  color: #168447;
}

.section-card {
  margin-top: 24rpx;
  padding: 28rpx;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18rpx;
}

.section-title {
  color: #171c2a;
  font-size: 34rpx;
  font-weight: 900;
}

.section-note {
  color: #8a7768;
  font-size: 24rpx;
}

.empty {
  padding: 48rpx 20rpx;
  border: 1rpx dashed #ead8ca;
  border-radius: 18rpx;
  background: var(--accent-soft);
  color: var(--accent);
  text-align: center;
}

.entry-row {
  display: grid;
  grid-template-columns: 58rpx 1fr auto;
  gap: 18rpx;
  align-items: start;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f0dfcf;
}

.entry-row:last-child {
  border-bottom: 0;
}

.entry-badge {
  display: grid;
  place-items: center;
  width: 58rpx;
  height: 58rpx;
  border-radius: 50%;
  color: #fff;
  font-size: 24rpx;
  font-weight: 900;
}

.entry-badge.received {
  background: var(--accent);
}

.entry-badge.given {
  background: #d6a55d;
}

.entry-title,
.entry-meta,
.entry-note {
  display: block;
}

.entry-title {
  color: #171c2a;
  font-size: 29rpx;
  font-weight: 900;
}

.entry-meta {
  margin-top: 7rpx;
  color: #8d929d;
  font-size: 23rpx;
}

.entry-note {
  margin-top: 12rpx;
  padding: 12rpx 16rpx;
  border-radius: 14rpx;
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 24rpx;
  line-height: 1.5;
}

.entry-amount {
  color: var(--accent);
  font-size: 28rpx;
  font-weight: 900;
  white-space: nowrap;
}

.entry-amount.given {
  color: #168447;
}
</style>
