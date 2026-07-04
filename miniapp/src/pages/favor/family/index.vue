<template>
  <view class="page" :class="activeTheme.tone">
    <view class="hero">
      <text class="hero-mark">{{ activeTheme.mark }}</text>
      <text class="eyebrow">家庭共享</text>
      <text class="title">{{ currentBook?.book.bookName || '家庭人情簿' }}</text>
      <text class="subtitle">家人共同维护{{ activeTheme.favorText }}，收到、送出和往来差额统一汇总。</text>
    </view>

    <view v-if="loading" class="panel center">同步家庭账本中...</view>

    <template v-else>
      <view v-if="!currentBook" class="panel empty-state">
        <text class="empty-title">还没有家庭人情簿</text>
        <text class="empty-desc">创建后可邀请家人共同记录人情往来，宴席也可归属到家庭账本。</text>
        <button class="primary-btn" :loading="creating" @tap="createDefaultBook()">创建家庭人情簿</button>
      </view>

      <template v-else>
        <view class="summary-card">
          <view class="summary-item">
            <text class="summary-label">家庭总收到{{ activeTheme.giftLabel }}</text>
            <text class="summary-value">{{ formatMoney(currentBook.receivedAmount) }}</text>
          </view>
          <view class="summary-item">
            <text class="summary-label">家庭总送出{{ activeTheme.giftLabel }}</text>
            <text class="summary-value">{{ formatMoney(currentBook.givenAmount) }}</text>
          </view>
          <view class="summary-item">
            <text class="summary-label">联系人</text>
            <text class="summary-value">{{ currentBook.contactCount }}</text>
          </view>
          <view class="summary-item">
            <text class="summary-label">总差额</text>
            <text class="summary-value" :class="Number(currentBook.balance) >= 0 ? 'positive' : 'negative'">{{ signedMoney(currentBook.balance) }}</text>
          </view>
        </view>

        <view class="member-card">
          <view class="section-head">
            <text class="section-title">家庭成员</text>
            <button class="plain-btn" @tap="showMemberForm = !showMemberForm">{{ showMemberForm ? '收起' : '邀请成员' }}</button>
          </view>
          <view v-if="showMemberForm" class="inline-form">
            <input v-model="memberForm.memberName" class="input" placeholder="成员姓名" />
            <input v-model="memberForm.relationship" class="input" placeholder="家庭关系，如：配偶、父母" />
            <input v-model="memberForm.phone" class="input" type="number" maxlength="11" placeholder="手机号（选填）" />
            <button class="small-primary" :loading="submittingMember" @tap="inviteMember()">加入成员</button>
          </view>
          <view class="member-list">
            <view v-for="member in currentBook.members" :key="member.id" class="member">
              <text class="avatar">{{ member.memberName.slice(0, 1) }}</text>
              <view class="member-copy">
                <text class="member-name">{{ member.memberName }}</text>
                <text class="member-role">{{ member.relationship || roleLabel(member.role) }}</text>
              </view>
              <text class="role-badge">{{ roleLabel(member.role) }}</text>
            </view>
          </view>
        </view>

        <view class="panel">
          <view class="section-head">
            <text class="section-title">家庭手动记账</text>
            <text class="section-meta">{{ directions[directionIndex].label }}</text>
          </view>
          <view class="record-form">
            <input v-model="manual.contactName" class="input" placeholder="对象姓名" />
            <input v-model.number="manual.amount" class="input" type="digit" placeholder="金额" />
            <picker :range="directions" range-key="label" @change="onDirectionChange">
              <view class="input picker">方向：{{ directions[directionIndex].label }}</view>
            </picker>
            <input v-model="manual.note" class="input" placeholder="备注，例如：家庭共同补录" />
            <button class="primary-btn" :loading="submittingManual" @tap="addManual()">添加家庭记录</button>
          </view>
        </view>

        <view class="panel">
          <view class="section-head">
            <text class="section-title">家庭往来对象</text>
            <text class="section-meta">{{ contacts.length }} 人</text>
          </view>
          <view class="search-box">
            <input v-model="keyword" placeholder="搜索姓名" confirm-type="search" @confirm="loadContacts()" />
            <button class="plain-btn" @tap="loadContacts()">搜索</button>
          </view>
          <view v-if="contacts.length === 0" class="empty">
            <text class="empty-title">暂无家庭往来记录</text>
            <text class="empty-desc">添加家庭手动记录或把宴席归属到家庭账本后，会在这里汇总。</text>
          </view>
          <view v-for="item in contacts" :key="item.contactId" class="record-row" @tap="selectCompareContact(item.contactName)">
            <text class="record-badge">{{ item.contactName.slice(0, 1) }}</text>
            <view class="record-main">
              <text class="record-title">{{ item.contactName }}</text>
              <text class="record-meta">收 {{ formatMoney(item.receivedAmount) }} · 送 {{ formatMoney(item.givenAmount) }}</text>
            </view>
            <text class="record-amount" :class="Number(item.balance) >= 0 ? 'receive' : 'give'">{{ signedMoney(item.balance) }}</text>
          </view>
        </view>

        <view class="panel">
          <view class="section-head">
            <text class="section-title">家庭往来对比</text>
            <text class="section-meta">{{ compareName || '选择对象' }}</text>
          </view>
          <view v-if="compareResult" class="compare-box">
            <text class="compare-name">{{ compareResult.contact.contactName }}</text>
            <text>他送我：{{ formatMoney(compareResult.receivedAmount) }}</text>
            <text>我送他：{{ formatMoney(compareResult.givenAmount) }}</text>
            <text class="compare-balance">差额：{{ signedMoney(compareResult.balance) }}</text>
          </view>
          <view v-else class="empty compact">点击上方家庭往来对象查看对比。</view>
        </view>
      </template>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { onShow } from '@dcloudio/uni-app';
import { request } from '../../../api/client';
import { eventThemeFor, readActiveEventType } from '../../../utils/event-theme';

interface FamilyMember {
  id: number;
  memberName: string;
  relationship?: string;
  role: string;
}

interface FamilyBookSummary {
  book: { id: number; bookName: string; description?: string };
  members: FamilyMember[];
  receivedAmount: number;
  givenAmount: number;
  balance: number;
  contactCount: number;
}

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

const activeType = ref(readActiveEventType());
const activeTheme = computed(() => eventThemeFor(activeType.value));
const books = ref<FamilyBookSummary[]>([]);
const selectedBookId = ref<number>();
const contacts = ref<FavorContact[]>([]);
const compareResult = ref<FavorCompare>();
const compareName = ref('');
const keyword = ref('');
const loading = ref(false);
const creating = ref(false);
const submittingMember = ref(false);
const submittingManual = ref(false);
const showMemberForm = ref(false);
const directionIndex = ref(0);
const memberForm = reactive({ memberName: '', relationship: '', phone: '' });
const manual = reactive({ contactName: '', amount: 0, direction: 'RECEIVED', note: '' });
const currentBook = computed(() => books.value.find((item) => item.book.id === selectedBookId.value) || books.value[0]);

function onDirectionChange(event: { detail: { value: number | string } }) {
  directionIndex.value = Number(event.detail.value);
  manual.direction = directions[directionIndex.value].value;
}

async function load() {
  loading.value = true;
  try {
    books.value = await request<FamilyBookSummary[]>('/favor/family-books');
    selectedBookId.value = currentBook.value?.book.id;
    await loadContacts();
  } finally {
    loading.value = false;
  }
}

async function loadContacts() {
  const bookId = currentBook.value?.book.id;
  if (!bookId) {
    contacts.value = [];
    return;
  }
  const query = keyword.value ? `?keyword=${encodeURIComponent(keyword.value)}` : '';
  contacts.value = await request<FavorContact[]>(`/favor/family-books/${bookId}/contacts${query}`);
}

async function createDefaultBook() {
  creating.value = true;
  try {
    const result = await request<FamilyBookSummary>('/favor/family-books', {
      method: 'POST',
      data: { bookName: '我的家庭人情簿', ownerName: '我' }
    });
    books.value = [result];
    selectedBookId.value = result.book.id;
    await loadContacts();
    uni.showToast({ title: '已创建家庭账本', icon: 'success' });
  } finally {
    creating.value = false;
  }
}

async function inviteMember() {
  const bookId = currentBook.value?.book.id;
  if (!bookId || !memberForm.memberName.trim()) {
    uni.showToast({ title: '请输入成员姓名', icon: 'none' });
    return;
  }
  submittingMember.value = true;
  try {
    await request(`/favor/family-books/${bookId}/members`, {
      method: 'POST',
      data: {
        memberName: memberForm.memberName.trim(),
        relationship: memberForm.relationship.trim(),
        phone: memberForm.phone.trim(),
        role: 'MEMBER'
      }
    });
    memberForm.memberName = '';
    memberForm.relationship = '';
    memberForm.phone = '';
    showMemberForm.value = false;
    await load();
    uni.showToast({ title: '成员已加入', icon: 'success' });
  } finally {
    submittingMember.value = false;
  }
}

async function addManual() {
  const bookId = currentBook.value?.book.id;
  if (!bookId) {
    uni.showToast({ title: '请先创建家庭账本', icon: 'none' });
    return;
  }
  if (!manual.contactName.trim()) {
    uni.showToast({ title: '请输入对象姓名', icon: 'none' });
    return;
  }
  if (!Number(manual.amount) || Number(manual.amount) <= 0) {
    uni.showToast({ title: '请输入有效金额', icon: 'none' });
    return;
  }
  const contactName = manual.contactName.trim();
  submittingManual.value = true;
  try {
    await request(`/favor/family-books/${bookId}/manual`, {
      method: 'POST',
      data: { ...manual, contactName, amount: Number(manual.amount) }
    });
    manual.contactName = '';
    manual.amount = 0;
    manual.note = '';
    await load();
    await selectCompareContact(contactName);
    uni.showToast({ title: '家庭记录已添加', icon: 'success' });
  } finally {
    submittingManual.value = false;
  }
}

async function selectCompareContact(name: string) {
  const bookId = currentBook.value?.book.id;
  if (!bookId) {
    return;
  }
  compareName.value = name;
  compareResult.value = await request<FavorCompare>(`/favor/family-books/${bookId}/compare?contactName=${encodeURIComponent(name)}`).catch(() => undefined);
}

function roleLabel(role?: string) {
  if (role === 'OWNER') return '户主';
  if (role === 'ADMIN') return '管理员';
  return '成员';
}

function formatMoney(value: unknown) {
  return `¥${Number(value || 0).toLocaleString('zh-CN', { maximumFractionDigits: 0 })}`;
}

function signedMoney(value: unknown) {
  const amount = Number(value || 0);
  if (amount > 0) return `+${formatMoney(amount)}`;
  if (amount < 0) return `-${formatMoney(Math.abs(amount))}`;
  return '持平';
}

onShow(() => {
  activeType.value = readActiveEventType();
  load();
});
</script>

<style scoped>
.page {
  --accent: #e60012;
  --accent-dark: #c40005;
  --accent-soft: #fff0ee;
  --page-bg: #f7f3ee;
  box-sizing: border-box;
  min-height: 100vh;
  padding: 24rpx 24rpx 140rpx;
  background: var(--page-bg);
  color: #151823;
}

.page.orange { --accent: #d96a11; --accent-dark: #a64209; --accent-soft: #fff3e3; --page-bg: #fbf4eb; }
.page.pink { --accent: #e7566f; --accent-dark: #b52d4c; --accent-soft: #fff0f4; --page-bg: #fff6f8; }
.page.green { --accent: #188356; --accent-dark: #0c5f3e; --accent-soft: #edf9f1; --page-bg: #f2f8f4; }
.page.blue { --accent: #2563eb; --accent-dark: #1d4ed8; --accent-soft: #edf4ff; --page-bg: #f2f6ff; }
.page.black { --accent: #2f3338; --accent-dark: #0d0f12; --accent-soft: #f1f2f4; --page-bg: #f3f4f5; }
.page.purple { --accent: #7c3aed; --accent-dark: #5b21b6; --accent-soft: #f4efff; --page-bg: #f7f3ff; }

.hero {
  position: relative;
  overflow: hidden;
  min-height: 250rpx;
  padding: 42rpx 34rpx;
  border-radius: 34rpx;
  background: linear-gradient(135deg, var(--accent), var(--accent-dark));
  color: #fff7e7;
}

.hero-mark {
  position: absolute;
  right: 34rpx;
  bottom: -20rpx;
  opacity: 0.16;
  font-size: 180rpx;
  font-weight: 900;
}

.eyebrow,
.title,
.subtitle {
  position: relative;
  z-index: 2;
  display: block;
}

.eyebrow {
  font-size: 25rpx;
  font-weight: 800;
  opacity: 0.88;
}

.title {
  margin-top: 18rpx;
  font-size: 48rpx;
  font-weight: 900;
}

.subtitle {
  margin-top: 14rpx;
  max-width: 560rpx;
  font-size: 26rpx;
  line-height: 1.55;
}

.summary-card,
.member-card,
.panel {
  margin-top: 24rpx;
  padding: 26rpx;
  border-radius: 26rpx;
  background: #fff;
  box-shadow: 0 12rpx 34rpx rgba(43, 35, 31, 0.06);
}

.summary-card {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 18rpx;
}

.summary-item {
  padding: 20rpx;
  border-radius: 22rpx;
  background: var(--accent-soft);
}

.summary-label,
.section-meta,
.record-meta,
.member-role,
.empty-desc {
  color: #7a8292;
  font-size: 24rpx;
}

.summary-value {
  display: block;
  margin-top: 10rpx;
  color: var(--accent);
  font-size: 34rpx;
  font-weight: 900;
}

.negative,
.give {
  color: #188356;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  margin-bottom: 18rpx;
}

.section-title {
  font-size: 31rpx;
  font-weight: 900;
}

.plain-btn,
.small-primary,
.primary-btn {
  height: 64rpx;
  padding: 0 24rpx;
  border-radius: 999rpx;
  font-size: 25rpx;
  line-height: 64rpx;
}

.plain-btn {
  border: 1rpx solid #ead8ca;
  background: #fffaf5;
  color: #9a4f28;
}

.primary-btn,
.small-primary {
  background: linear-gradient(135deg, var(--accent), var(--accent-dark));
  color: #fff;
  font-weight: 900;
}

.inline-form,
.record-form {
  display: grid;
  gap: 14rpx;
  margin-bottom: 18rpx;
}

.input,
.picker,
.search-box {
  min-height: 72rpx;
  padding: 0 22rpx;
  border: 1rpx solid #efe4da;
  border-radius: 18rpx;
  background: #fffdfb;
  font-size: 26rpx;
  line-height: 72rpx;
  box-sizing: border-box;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 14rpx;
  padding: 10rpx;
}

.search-box input {
  flex: 1;
}

.member {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 18rpx 0;
  border-top: 1rpx solid #f0e8e1;
}

.avatar,
.record-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 62rpx;
  height: 62rpx;
  border-radius: 50%;
  background: var(--accent-soft);
  color: var(--accent);
  font-weight: 900;
}

.member-copy,
.record-main {
  flex: 1;
  min-width: 0;
}

.member-name,
.record-title {
  display: block;
  color: #151823;
  font-size: 28rpx;
  font-weight: 900;
}

.role-badge {
  padding: 8rpx 14rpx;
  border-radius: 999rpx;
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 22rpx;
  font-weight: 800;
}

.record-row {
  display: flex;
  align-items: center;
  gap: 18rpx;
  padding: 18rpx 0;
  border-top: 1rpx solid #f0e8e1;
}

.record-amount {
  color: var(--accent);
  font-size: 28rpx;
  font-weight: 900;
}

.compare-box {
  display: grid;
  gap: 12rpx;
  padding: 22rpx;
  border-radius: 22rpx;
  background: var(--accent-soft);
  color: #252936;
  font-size: 27rpx;
}

.compare-name,
.compare-balance {
  color: var(--accent);
  font-size: 30rpx;
  font-weight: 900;
}

.center,
.empty-state,
.empty {
  display: grid;
  gap: 14rpx;
  justify-items: start;
  color: #7a8292;
  font-size: 26rpx;
}

.empty-title {
  color: #151823;
  font-size: 30rpx;
  font-weight: 900;
}
</style>
