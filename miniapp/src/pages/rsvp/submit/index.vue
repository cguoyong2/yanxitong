<template>
  <view class="page" v-if="!submitted">
    <text class="title">填写回执</text>
    <text class="hint">手机号会用于识别重复提交；同一来宾再次提交会更新原回执。</text>
    <input v-model="form.guestName" class="input" placeholder="姓名" />
    <input v-model="form.phone" class="input" type="number" maxlength="11" placeholder="手机号，建议填写" />
    <picker :range="statuses" range-key="label" @change="onStatusChange">
      <view class="input">{{ selectedStatusLabel }}</view>
    </picker>
    <input v-if="isAttending" v-model.number="form.guestCount" class="input" type="number" placeholder="人数" />
    <view v-if="isAttending" class="check-group">
      <label class="check"><checkbox :checked="form.mealRequired === 1" @click="toggleMeal" />需要用餐</label>
      <label class="check"><checkbox :checked="form.accommodationRequired === 1" @click="toggleAccommodation" />需要住宿</label>
    </view>
    <view v-else class="decline-note">不参加或待定时，不统计用餐和住宿人数。</view>
    <textarea v-model="form.message" class="textarea" placeholder="留言，可选" />
    <button type="primary" :loading="submitting" @click="submit">提交回执</button>
  </view>
  <view class="page" v-else>
    <view class="success-card">
      <text class="success-title">{{ submitResult?.created ? '回执已提交' : '回执已更新' }}</text>
      <text class="success-text">{{ successText }}</text>
      <button type="primary" @click="openGift">去随礼</button>
      <button @click="backToInvitation">返回请柬</button>
      <button @click="editAgain">修改回执</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { request } from '../../../api/client';

const statuses = [
  { label: '参加', value: 'ATTENDING' },
  { label: '待定', value: 'PENDING' },
  { label: '不参加', value: 'DECLINED' }
];
const selectedIndex = ref(0);
const banquetId = ref('');
const invitationId = ref('');
const shareUrl = ref('');
const submitting = ref(false);
const submitted = ref(false);
const submitResult = ref<{ id: number; created?: boolean; attendanceStatus: string; guestCount: number }>();
const form = reactive({
  guestName: '',
  phone: '',
  attendanceStatus: 'ATTENDING',
  mealRequired: 1,
  accommodationRequired: 0,
  guestCount: 1,
  message: ''
});
const selectedStatusLabel = computed(() => statuses[selectedIndex.value].label);
const isAttending = computed(() => form.attendanceStatus === 'ATTENDING' || form.attendanceStatus === 'ATTEND');
const successText = computed(() => {
  if (!submitResult.value) {
    return '';
  }
  if (submitResult.value.attendanceStatus === 'DECLINED') {
    return '已记录您的回复，感谢告知。';
  }
  if (submitResult.value.attendanceStatus === 'PENDING') {
    return '已记录为待定，之后可再次提交更新。';
  }
  return `已记录 ${submitResult.value.guestCount || 1} 位来宾出席。`;
});

function onStatusChange(event: { detail: { value: number | string } }) {
  selectedIndex.value = Number(event.detail.value);
  form.attendanceStatus = statuses[selectedIndex.value].value;
  if (!isAttending.value) {
    form.mealRequired = 0;
    form.accommodationRequired = 0;
    form.guestCount = 1;
  } else {
    form.mealRequired = 1;
  }
}

function toggleMeal() {
  form.mealRequired = form.mealRequired === 1 ? 0 : 1;
}

function toggleAccommodation() {
  form.accommodationRequired = form.accommodationRequired === 1 ? 0 : 1;
}

async function submit() {
  if (!validate()) {
    return;
  }
  submitting.value = true;
  try {
    const result = await request<{ id: number; created?: boolean; attendanceStatus: string; guestCount: number }>('/rsvp/submit', {
      method: 'POST',
      data: {
        ...form,
        banquetId: Number(banquetId.value),
        invitationId: invitationId.value ? Number(invitationId.value) : undefined
      }
    });
    submitResult.value = result;
    submitted.value = true;
    uni.showToast({ title: result.created ? '已提交' : '已更新', icon: 'success' });
  } finally {
    submitting.value = false;
  }
}

function validate() {
  if (!banquetId.value) {
    uni.showToast({ title: '缺少宴席信息', icon: 'none' });
    return false;
  }
  if (!form.guestName.trim()) {
    uni.showToast({ title: '请填写姓名', icon: 'none' });
    return false;
  }
  if (form.phone && !/^1\d{10}$/.test(form.phone)) {
    uni.showToast({ title: '手机号格式不正确', icon: 'none' });
    return false;
  }
  if (isAttending.value && (!form.guestCount || Number(form.guestCount) < 1)) {
    uni.showToast({ title: '出席人数至少为 1', icon: 'none' });
    return false;
  }
  return true;
}

function openGift() {
  if (banquetId.value) {
    uni.navigateTo({ url: `/pages/gift/pay/index?banquetId=${banquetId.value}&entrySource=ONLINE_GIFT&guestName=${encodeURIComponent(form.guestName)}` });
  }
}

function backToInvitation() {
  if (shareUrl.value) {
    uni.navigateTo({ url: shareUrl.value });
    return;
  }
  uni.navigateBack();
}

function editAgain() {
  submitted.value = false;
}

async function loadInvitationShareUrl() {
  if (!invitationId.value) {
    return;
  }
  const detail = await request<{ shareUrl?: string }>(`/invitations/${invitationId.value}`);
  shareUrl.value = detail.shareUrl || '';
}

onMounted(async () => {
  const pages = getCurrentPages();
  const current = pages[pages.length - 1] as unknown as { options?: Record<string, string> };
  banquetId.value = current.options?.banquetId || '';
  invitationId.value = current.options?.invitationId || '';
  await loadInvitationShareUrl();
});
</script>

<style scoped>
.page { padding: 24rpx; }
.title { display: block; margin-bottom: 24rpx; font-size: 40rpx; font-weight: 600; }
.hint { display: block; margin-bottom: 20rpx; color: #64748b; font-size: 24rpx; line-height: 1.6; }
.input, .textarea { box-sizing: border-box; width: 100%; margin-bottom: 20rpx; padding: 20rpx; border: 1px solid #ddd; border-radius: 8rpx; }
.textarea { min-height: 140rpx; }
.check-group { display: grid; gap: 14rpx; margin-bottom: 20rpx; }
.check { display: block; }
.decline-note { margin-bottom: 20rpx; padding: 18rpx; border-radius: 8rpx; background: #f8fafc; color: #64748b; font-size: 24rpx; }
.success-card { display: grid; gap: 18rpx; padding: 28rpx; border: 1px solid #e5e7eb; border-radius: 8rpx; background: #fff; }
.success-title { color: #111827; font-size: 42rpx; font-weight: 700; }
.success-text { color: #374151; line-height: 1.6; }
</style>
