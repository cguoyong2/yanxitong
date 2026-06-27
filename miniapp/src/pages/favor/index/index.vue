<template>
  <view class="page">
    <view class="title-row">
      <view>
        <text class="page-title">我的人情</text>
        <text class="page-subtitle">收礼、送礼、往来差额统一查看</text>
      </view>
      <button class="family-link" @tap="openFamily()">家庭人情 ›</button>
    </view>

    <view class="summary-card">
      <view class="summary-item">
        <text class="summary-label">总收礼</text>
        <text class="summary-value">{{ formatMoney(totalReceived) }}</text>
      </view>
      <view class="divider"></view>
      <view class="summary-item">
        <text class="summary-label">总送礼</text>
        <text class="summary-value">{{ formatMoney(totalGiven) }}</text>
      </view>
      <view class="divider"></view>
      <view class="summary-item">
        <text class="summary-label">总差额</text>
        <text class="summary-value" :class="balanceClass(totalBalance)">{{ signedMoney(totalBalance) }}</text>
      </view>
    </view>

    <view class="search-card">
      <input v-model="keyword" class="search-input" placeholder="搜索姓名、手机号" confirm-type="search" @confirm="load()" />
      <button class="search-btn" @tap="load()">搜索</button>
    </view>

    <view class="quick-card">
      <view class="quick-item">
        <text class="quick-icon receive">收</text>
        <text class="quick-title">我收到的人情</text>
        <text class="quick-value">{{ formatMoney(totalReceived) }}</text>
      </view>
      <view class="quick-item">
        <text class="quick-icon give">送</text>
        <text class="quick-title">我送出的人情</text>
        <text class="quick-value">{{ formatMoney(totalGiven) }}</text>
      </view>
      <view class="quick-item wide" @tap="compareName = keyword">
        <text class="quick-icon ledger">账</text>
        <view>
          <text class="quick-title">人情往来账</text>
          <text class="quick-desc">按联系人查看双向往来与差额</text>
        </view>
      </view>
    </view>

    <view class="panel">
      <view class="section-head">
        <text class="section-title">人情往来</text>
        <text class="section-meta">{{ contacts.length }} 位</text>
      </view>
      <view v-if="loading" class="empty">正在同步人情联系人</view>
      <view v-else-if="contacts.length === 0" class="empty">
        {{ keyword ? `没有找到“${keyword}”的人情联系人` : '暂无人情联系人' }}
      </view>
      <view v-for="contact in contacts" :key="contact.contactId" class="contact-row" @tap="openDetail(contact.contactId)">
        <view class="contact-avatar">{{ contact.contactName.slice(0, 1) }}</view>
        <view class="contact-main">
          <view class="contact-top">
            <text class="contact-name">{{ contact.contactName }}</text>
            <text class="balance" :class="balanceClass(contact.balance)">{{ signedMoney(contact.balance) }}</text>
          </view>
          <text class="contact-meta">收 {{ formatMoney(contact.receivedAmount) }} / 送 {{ formatMoney(contact.givenAmount) }}</text>
          <text class="contact-meta">{{ balanceText(contact.balance) }}</text>
        </view>
        <text class="arrow">›</text>
      </view>
    </view>

    <view class="panel">
      <text class="section-title">双向对比</text>
      <view class="form-row">
        <input v-model="compareName" class="input" placeholder="输入姓名做双向对比" />
        <button class="mini-btn" @tap="compare()">对比</button>
      </view>
      <view v-if="compareResult" class="compare-result">
        <text class="compare-name">{{ compareResult.contact.contactName }}</text>
        <text>他送我的：{{ formatMoney(compareResult.receivedAmount) }}</text>
        <text>我送他的：{{ formatMoney(compareResult.givenAmount) }}</text>
        <text :class="balanceClass(compareResult.balance)">差额：{{ signedMoney(compareResult.balance) }}，{{ balanceText(compareResult.balance) }}</text>
      </view>
    </view>

    <view class="panel">
      <text class="section-title">手动补录</text>
      <input v-model="manual.contactName" class="input" placeholder="对象姓名" />
      <input v-model.number="manual.amount" class="input" type="digit" placeholder="金额" />
      <picker :range="directions" range-key="label" @change="onDirectionChange">
        <view class="input picker">方向：{{ directions[directionIndex].label }}</view>
      </picker>
      <input v-model="manual.note" class="input" placeholder="备注，例如：补录朋友婚礼回礼" />
      <button class="submit-btn" @tap="addManual()">添加记录</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { request } from '../../../api/client';

interface FavorContact {
  contactId: number;
  contactName: string;
  receivedAmount: number;
  givenAmount: number;
  balance: number;
}

interface FavorCompare {
  contact: { contactName: string };
  receivedAmount: number;
  givenAmount: number;
  balance: number;
}

const directions = [
  { label: '他送我的', value: 'RECEIVED' },
  { label: '我送他的', value: 'GIVEN' }
];
const directionIndex = ref(0);
const contacts = ref<FavorContact[]>([]);
const keyword = ref('');
const compareName = ref('');
const compareResult = ref<FavorCompare>();
const loading = ref(false);
const manual = reactive({ contactName: '', amount: 0, direction: 'RECEIVED', note: '' });
const totalReceived = computed(() => sum(contacts.value.map((contact) => contact.receivedAmount)));
const totalGiven = computed(() => sum(contacts.value.map((contact) => contact.givenAmount)));
const totalBalance = computed(() => totalReceived.value - totalGiven.value);

function onDirectionChange(event: { detail: { value: number | string } }) {
  directionIndex.value = Number(event.detail.value);
  manual.direction = directions[directionIndex.value].value;
}

async function load() {
  const query = keyword.value ? `?keyword=${encodeURIComponent(keyword.value)}` : '';
  loading.value = true;
  try {
    contacts.value = await request<FavorContact[]>(`/favor/contacts${query}`);
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

async function compare() {
  if (!compareName.value.trim()) {
    uni.showToast({ title: '请输入姓名', icon: 'none' });
    return;
  }
  compareResult.value = await request<FavorCompare>(`/favor/compare?contactName=${encodeURIComponent(compareName.value.trim())}`);
}

function openDetail(id: number) {
  uni.navigateTo({ url: `/pages/favor/detail/index?id=${id}` });
}

function openFamily() {
  uni.navigateTo({ url: '/pages/favor/family/index' });
}

function sum(values: number[]) {
  return values.reduce((total, value) => total + Number(value || 0), 0);
}

function formatMoney(value: unknown) {
  return `¥${Number(value || 0).toFixed(0)}`;
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

onMounted(load);
</script>

<style scoped>
.page {
  box-sizing: border-box;
  min-height: 100vh;
  padding: 24rpx;
  background: #f7f7f7;
  color: #151823;
}

.title-row,
.section-head,
.contact-top,
.form-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
}

.page-title,
.page-subtitle,
.summary-label,
.summary-value,
.quick-title,
.quick-value,
.quick-desc,
.section-title,
.contact-name,
.contact-meta,
.compare-name {
  display: block;
}

.page-title {
  font-size: 42rpx;
  font-weight: 800;
}

.page-subtitle {
  margin-top: 8rpx;
  color: #7a7f8c;
  font-size: 24rpx;
}

button {
  margin: 0;
  padding: 0;
  border: 0;
}

button::after {
  border: 0;
}

.family-link {
  flex: 0 0 auto;
  padding: 0 20rpx;
  border: 1rpx solid #f0d4bd;
  border-radius: 999rpx;
  background: #fff8ef;
  color: #b80000;
  font-size: 24rpx;
  line-height: 58rpx;
}

.summary-card {
  display: grid;
  grid-template-columns: 1fr 1rpx 1fr 1rpx 1fr;
  align-items: center;
  margin-top: 22rpx;
  padding: 28rpx 18rpx;
  border-radius: 16rpx;
  background:
    radial-gradient(circle at 90% 20%, rgba(255, 232, 190, 0.26), transparent 28%),
    linear-gradient(135deg, #e60012, #b80000);
  color: #fff;
  box-shadow: 0 18rpx 34rpx rgba(184, 0, 0, 0.16);
}

.summary-item {
  min-width: 0;
  text-align: center;
}

.summary-label {
  color: rgba(255, 255, 255, 0.84);
  font-size: 23rpx;
}

.summary-value {
  margin-top: 10rpx;
  color: #fff1ca;
  font-size: 31rpx;
  font-weight: 800;
}

.divider {
  width: 1rpx;
  height: 62rpx;
  background: rgba(255, 255, 255, 0.24);
}

.search-card {
  display: grid;
  grid-template-columns: 1fr 116rpx;
  gap: 12rpx;
  margin-top: 20rpx;
}

.search-input,
.input {
  box-sizing: border-box;
  min-height: 76rpx;
  padding: 0 22rpx;
  border: 1rpx solid #eeeeee;
  border-radius: 999rpx;
  background: #fff;
  font-size: 25rpx;
}

.search-btn,
.mini-btn {
  border-radius: 999rpx;
  background: #151823;
  color: #fff;
  font-size: 24rpx;
  line-height: 76rpx;
}

.quick-card,
.panel {
  margin-top: 20rpx;
  padding: 22rpx;
  border: 1rpx solid #eeeeee;
  border-radius: 12rpx;
  background: #fff;
  box-shadow: 0 10rpx 24rpx rgba(30, 18, 12, 0.04);
}

.quick-card {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14rpx;
}

.quick-item {
  min-height: 138rpx;
  padding: 20rpx;
  border: 1rpx solid #f3e6dc;
  border-radius: 12rpx;
  background: linear-gradient(180deg, #fffaf5, #fff);
}

.quick-item.wide {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  gap: 18rpx;
  min-height: auto;
}

.quick-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 54rpx;
  height: 54rpx;
  border-radius: 50%;
  color: #fff;
  font-weight: 800;
}

.quick-icon.receive {
  background: #e60012;
}

.quick-icon.give {
  background: #d6a55d;
}

.quick-icon.ledger {
  background: #151823;
}

.quick-title {
  margin-top: 14rpx;
  font-size: 27rpx;
  font-weight: 800;
}

.quick-item.wide .quick-title {
  margin-top: 0;
}

.quick-value {
  margin-top: 10rpx;
  color: #c71916;
  font-size: 30rpx;
  font-weight: 800;
}

.quick-desc,
.section-meta {
  color: #7a7f8c;
  font-size: 23rpx;
}

.section-title {
  font-size: 31rpx;
  font-weight: 800;
}

.empty {
  margin-top: 18rpx;
  padding: 32rpx 20rpx;
  border: 1rpx dashed #f0d4bd;
  border-radius: 12rpx;
  background: #fffaf4;
  color: #8b6250;
  text-align: center;
}

.contact-row {
  display: flex;
  align-items: center;
  gap: 18rpx;
  padding: 22rpx 0;
  border-bottom: 1rpx solid #eeeeee;
}

.contact-row:last-child {
  border-bottom: 0;
}

.contact-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 62rpx;
  height: 62rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #ef6a62, #d8271f);
  color: #fff;
  font-size: 28rpx;
  font-weight: 800;
}

.contact-main {
  flex: 1;
  min-width: 0;
}

.contact-name {
  color: #151823;
  font-size: 29rpx;
  font-weight: 800;
}

.contact-meta {
  margin-top: 7rpx;
  color: #7a7f8c;
  font-size: 23rpx;
}

.balance {
  flex: 0 0 auto;
  font-size: 27rpx;
  font-weight: 800;
}

.positive {
  color: #c71916;
}

.negative {
  color: #24824d;
}

.neutral {
  color: #7a7f8c;
}

.arrow {
  color: #b6bbc7;
  font-size: 40rpx;
}

.form-row {
  margin-top: 16rpx;
}

.form-row .input {
  flex: 1;
  min-width: 0;
}

.mini-btn {
  width: 118rpx;
}

.compare-result {
  display: grid;
  gap: 8rpx;
  margin-top: 18rpx;
  padding: 18rpx;
  border-radius: 12rpx;
  background: #fffaf4;
  color: #6d7280;
  font-size: 24rpx;
}

.compare-name {
  color: #151823;
  font-weight: 800;
}

.panel > .input,
.picker {
  width: 100%;
  margin-top: 16rpx;
  border-radius: 12rpx;
}

.submit-btn {
  width: 100%;
  margin-top: 18rpx;
  border-radius: 12rpx;
  background: linear-gradient(135deg, #e60012, #c71916);
  color: #fff;
  font-size: 28rpx;
  font-weight: 800;
  line-height: 82rpx;
}
</style>
