<template>
  <view class="page">
    <text class="title">人情账本</text>
    <view class="summary">
      <view class="summary-main">
        <text class="summary-label">当前差额</text>
        <text class="summary-amount" :class="balanceClass(totalBalance)">{{ formatMoney(totalBalance) }}</text>
        <text class="summary-note">{{ balanceText(totalBalance) }}</text>
      </view>
      <view class="summary-grid">
        <text>联系人：{{ contacts.length }} 位</text>
        <text>收礼：{{ formatMoney(totalReceived) }}</text>
        <text>回礼：{{ formatMoney(totalGiven) }}</text>
        <text>净额：{{ formatMoney(totalBalance) }}</text>
      </view>
    </view>
    <view class="search">
      <input v-model="keyword" class="input" placeholder="搜索对象姓名" />
      <view class="actions">
        <button size="mini" @click="load">搜索</button>
        <button size="mini" @click="resetSearch">重置</button>
      </view>
    </view>
    <view class="panel">
      <text class="section-title">双向对比</text>
      <input v-model="compareName" class="input" placeholder="输入姓名做双向对比" />
      <button size="mini" @click="compare">对比</button>
      <view v-if="compareResult" class="compare">
        <text class="compare-name">{{ compareResult.contact.contactName }}</text>
        <text>他送我的：{{ formatMoney(compareResult.receivedAmount) }}</text>
        <text>我送他的：{{ formatMoney(compareResult.givenAmount) }}</text>
        <text :class="balanceClass(compareResult.balance)">差额：{{ formatMoney(compareResult.balance) }}，{{ balanceText(compareResult.balance) }}</text>
      </view>
    </view>
    <view class="panel">
      <text class="section-title">手动补录</text>
      <input v-model="manual.contactName" class="input" placeholder="对象姓名" />
      <input v-model.number="manual.amount" class="input" type="digit" placeholder="金额" />
      <picker :range="directions" range-key="label" @change="onDirectionChange">
        <view class="input">方向：{{ directions[directionIndex].label }}</view>
      </picker>
      <input v-model="manual.note" class="input" placeholder="备注，例如：补录朋友婚礼回礼" />
      <button size="mini" @click="addManual">手动补录</button>
    </view>
    <view v-if="!loading && contacts.length === 0" class="empty">
      <text>{{ keyword ? `没有找到“${keyword}”的人情联系人` : '暂无人情联系人' }}</text>
    </view>
    <view v-for="contact in contacts" :key="contact.contactId" class="row" @click="openDetail(contact.contactId)">
      <view>
        <text class="name">{{ contact.contactName }}</text>
        <text class="meta">收 {{ formatMoney(contact.receivedAmount) }} / 给 {{ formatMoney(contact.givenAmount) }}</text>
        <text class="meta">{{ balanceText(contact.balance) }}</text>
      </view>
      <text class="amount" :class="balanceClass(contact.balance)">{{ formatMoney(contact.balance) }}</text>
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

function resetSearch() {
  keyword.value = '';
  load();
}

function sum(values: number[]) {
  return values.reduce((total, value) => total + Number(value || 0), 0);
}

function formatMoney(value: unknown) {
  return `¥${Number(value || 0).toFixed(2)}`;
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
.page { padding: 24rpx; }
.title { display: block; margin-bottom: 24rpx; font-size: 40rpx; font-weight: 600; }
.summary { display: grid; gap: 16rpx; margin-bottom: 20rpx; padding: 20rpx; border: 1px solid #e5e7eb; border-radius: 8rpx; background: #fff; }
.summary-main { display: grid; gap: 6rpx; }
.summary-label { color: #64748b; font-size: 24rpx; }
.summary-amount { font-size: 44rpx; font-weight: 700; }
.summary-note { color: #64748b; font-size: 24rpx; }
.summary-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10rpx; color: #374151; font-size: 24rpx; }
.search, .panel { margin-bottom: 24rpx; padding: 20rpx; border: 1px solid #e5e7eb; border-radius: 8rpx; background: #fff; }
.section-title { display: block; margin-bottom: 14rpx; font-weight: 600; }
.actions { display: flex; gap: 12rpx; }
.actions button { margin: 0; }
.compare { display: flex; flex-direction: column; gap: 8rpx; margin-top: 16rpx; padding: 16rpx; border-radius: 8rpx; background: #f8fafc; color: #555; }
.compare-name { color: #111827; font-weight: 600; }
.input { box-sizing: border-box; width: 100%; margin-bottom: 16rpx; padding: 18rpx; border: 1px solid #ddd; border-radius: 8rpx; }
.empty { padding: 36rpx 20rpx; border: 1px dashed #d1d5db; border-radius: 8rpx; color: #64748b; text-align: center; }
.row { display: flex; justify-content: space-between; gap: 12rpx; padding: 20rpx 0; border-bottom: 1px solid #eee; }
.name, .meta { display: block; }
.name { font-weight: 600; }
.meta { margin-top: 6rpx; color: #666; font-size: 24rpx; }
.amount { font-weight: 700; }
.positive { color: #b91c1c; }
.negative { color: #2563eb; }
.neutral { color: #64748b; }
</style>
