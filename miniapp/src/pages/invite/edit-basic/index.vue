<template>
  <view class="page">
    <text class="title">基础请柬编辑</text>
    <input v-model="form.title" class="input" placeholder="请柬标题" />
    <input v-model="form.hostName" class="input" placeholder="主办人，可选" />
    <input v-model="form.contactPhone" class="input" placeholder="联系电话，可选" />
    <input v-model="form.coverUrl" class="input" placeholder="封面 URL，可选" />
    <input v-model="form.addressDetail" class="input" placeholder="地址详情，可选" />
    <textarea v-model="form.greeting" class="textarea" placeholder="欢迎语" />
    <textarea v-model="form.scheduleText" class="textarea" placeholder="宴席流程，每行一个节点，如 17:30 签到" />
    <view class="switch-row">
      <text>显示随礼入口</text>
      <switch :checked="form.showGiftEntry" @change="form.showGiftEntry = Boolean($event.detail.value)" />
    </view>
    <view class="switch-row">
      <text>显示设备入口</text>
      <switch :checked="form.showDeviceEntry" @change="form.showDeviceEntry = Boolean($event.detail.value)" />
    </view>
    <button type="primary" :loading="submitting" @click="submit">保存</button>
    <button v-if="shareUrl" @click="copyShareUrl">复制分享路径</button>
  </view>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { request } from '../../../api/client';

const invitationId = ref('');
const submitting = ref(false);
const shareUrl = ref('');
const form = reactive({
  title: '',
  hostName: '',
  contactPhone: '',
  coverUrl: '',
  addressDetail: '',
  greeting: '',
  scheduleText: '',
  showGiftEntry: true,
  showDeviceEntry: true
});

interface InvitationDetail {
  invitation: {
    title: string;
    coverUrl?: string;
  };
  basicFields?: Record<string, string>;
  shareUrl?: string;
}

function fillForm(detail: InvitationDetail) {
  const fields = detail.basicFields || {};
  form.title = detail.invitation.title || form.title;
  form.coverUrl = detail.invitation.coverUrl || '';
  form.hostName = fields.hostName || '';
  form.contactPhone = fields.contactPhone || '';
  form.addressDetail = fields.addressDetail || '';
  form.greeting = fields.greeting || '';
  form.scheduleText = fields.scheduleText || '';
  form.showGiftEntry = fields.showGiftEntry !== '0';
  form.showDeviceEntry = fields.showDeviceEntry !== '0';
  shareUrl.value = detail.shareUrl || '';
}

async function loadInvitation() {
  if (!invitationId.value) {
    return;
  }
  const detail = await request<InvitationDetail>(`/invitations/${invitationId.value}`);
  fillForm(detail);
}

async function submit() {
  if (!invitationId.value || !form.title) {
    uni.showToast({ title: '请填写标题', icon: 'none' });
    return;
  }
  submitting.value = true;
  try {
    await request(`/invitations/${invitationId.value}/basic`, {
      method: 'PUT',
      data: form
    });
    uni.showToast({ title: '已保存', icon: 'success' });
    setTimeout(() => uni.navigateBack(), 600);
  } finally {
    submitting.value = false;
  }
}

function copyShareUrl() {
  uni.setClipboardData({
    data: shareUrl.value,
    success: () => uni.showToast({ title: '已复制', icon: 'success' })
  });
}

onMounted(async () => {
  const pages = getCurrentPages();
  const current = pages[pages.length - 1] as unknown as { options?: Record<string, string> };
  invitationId.value = current.options?.invitationId || '';
  form.title = current.options?.title ? decodeURIComponent(current.options.title) : '';
  await loadInvitation();
});
</script>

<style scoped>
.page { padding: 24rpx; }
.title { display: block; margin-bottom: 24rpx; font-size: 40rpx; font-weight: 600; }
.input, .textarea { box-sizing: border-box; width: 100%; margin-bottom: 20rpx; padding: 20rpx; border: 1px solid #ddd; border-radius: 8rpx; }
.textarea { min-height: 160rpx; }
.switch-row { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20rpx; padding: 20rpx; border: 1px solid #ddd; border-radius: 8rpx; }
</style>
