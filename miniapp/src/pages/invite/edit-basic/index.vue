<template>
  <view class="page">
    <view class="hero-card">
      <view class="hero-art">
        <text class="hero-symbol">柬</text>
      </view>
      <text class="hero-label">基础请柬</text>
      <text class="hero-title">编辑请柬</text>
      <text class="hero-desc">填写公开页展示信息，保存后分享链接会同步更新。</text>
    </view>

    <view class="form-card">
      <view class="field-row">
        <text class="field-icon">题</text>
        <text class="field-label">请柬标题</text>
        <input v-model="form.title" class="field-input" placeholder="请输入请柬标题" placeholder-class="placeholder" />
      </view>
      <view class="field-row">
        <text class="field-icon">主</text>
        <text class="field-label">主办人</text>
        <input v-model="form.hostName" class="field-input" placeholder="主办人，可选" placeholder-class="placeholder" />
      </view>
      <view class="field-row">
        <text class="field-icon">电</text>
        <text class="field-label">联系电话</text>
        <input v-model="form.contactPhone" class="field-input" placeholder="联系电话，可选" placeholder-class="placeholder" />
      </view>
      <view class="field-row">
        <text class="field-icon">图</text>
        <text class="field-label">封面 URL</text>
        <input v-model="form.coverUrl" class="field-input" placeholder="封面 URL，可选" placeholder-class="placeholder" />
      </view>
      <view class="field-row">
        <text class="field-icon">址</text>
        <text class="field-label">地址详情</text>
        <input v-model="form.addressDetail" class="field-input" placeholder="楼层、厅号、路线说明" placeholder-class="placeholder" />
      </view>
    </view>

    <view class="section-card">
      <text class="section-title">邀请文案</text>
      <textarea v-model="form.greeting" class="textarea" placeholder="欢迎语" placeholder-class="placeholder" />
    </view>

    <view class="section-card">
      <text class="section-title">宴席流程</text>
      <textarea v-model="form.scheduleText" class="textarea tall" placeholder="每行一个节点，如：17:30 签到" placeholder-class="placeholder" />
    </view>

    <view class="section-card">
      <text class="section-title">入口设置</text>
      <view class="switch-row">
        <view>
          <text class="switch-title">显示随礼入口</text>
          <text class="switch-desc">非支付体验版仍会显示暂未开放提示</text>
        </view>
        <switch :checked="form.showGiftEntry" color="#d71920" @change="form.showGiftEntry = Boolean($event.detail.value)" />
      </view>
      <view class="switch-row">
        <view>
          <text class="switch-title">显示设备入口</text>
          <text class="switch-desc">用于确认屏、云喇叭租赁入口</text>
        </view>
        <switch :checked="form.showDeviceEntry" color="#d71920" @change="form.showDeviceEntry = Boolean($event.detail.value)" />
      </view>
    </view>

    <view v-if="shareUrl" class="share-card">
      <text class="section-title">分享路径</text>
      <text class="share-url">{{ shareUrl }}</text>
      <view class="share-actions">
        <button class="ghost-button" @tap="previewInvite">预览请柬</button>
        <button class="ghost-button" @tap="copyShareUrl">复制路径</button>
      </view>
    </view>

    <view class="footer-safe"></view>
    <view class="sticky-submit">
      <button class="primary-button" :loading="submitting" @tap="submit">保存请柬</button>
    </view>
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
  if (!invitationId.value || !form.title.trim()) {
    uni.showToast({ title: '请填写标题', icon: 'none' });
    return;
  }
  submitting.value = true;
  try {
    await request(`/invitations/${invitationId.value}/basic`, {
      method: 'PUT',
      data: form
    });
    await loadInvitation();
    uni.showToast({ title: '已保存', icon: 'success' });
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '保存请柬失败', icon: 'none' });
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

function previewInvite() {
  if (!shareUrl.value) {
    uni.showToast({ title: '暂无分享路径', icon: 'none' });
    return;
  }
  uni.navigateTo({ url: shareUrl.value });
}

onMounted(async () => {
  const pages = getCurrentPages();
  const current = pages[pages.length - 1] as unknown as { options?: Record<string, string> };
  invitationId.value = current.options?.invitationId || '';
  form.title = current.options?.title ? decodeURIComponent(current.options.title) : '';
  if (!invitationId.value) {
    uni.showToast({ title: '缺少请柬信息', icon: 'none' });
    setTimeout(() => uni.navigateBack(), 700);
    return;
  }
  await loadInvitation();
});
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 24rpx 24rpx 0;
  background: #fff8ef;
  box-sizing: border-box;
  color: #171c2a;
}

.hero-card {
  position: relative;
  overflow: hidden;
  padding: 34rpx;
  border-radius: 28rpx;
  background:
    radial-gradient(circle at 84% 18%, rgba(255, 217, 150, 0.38), transparent 180rpx),
    linear-gradient(135deg, #e71921 0%, #c9161c 62%, #9b0e13 100%);
  box-shadow: 0 16rpx 42rpx rgba(184, 17, 21, 0.24);
}

.hero-art {
  position: absolute;
  right: -34rpx;
  bottom: -62rpx;
  width: 250rpx;
  height: 250rpx;
  border-radius: 50%;
  background: rgba(255, 224, 170, 0.16);
}

.hero-symbol {
  position: absolute;
  right: 82rpx;
  bottom: 76rpx;
  color: rgba(255, 239, 206, 0.34);
  font-family: serif;
  font-size: 86rpx;
  font-weight: 900;
}

.hero-label,
.hero-title,
.hero-desc {
  position: relative;
  z-index: 2;
  display: block;
}

.hero-label {
  color: #ffe2ba;
  font-size: 26rpx;
  font-weight: 800;
}

.hero-title {
  margin-top: 14rpx;
  color: #fff8df;
  font-family: serif;
  font-size: 58rpx;
  font-weight: 900;
}

.hero-desc {
  width: 80%;
  margin-top: 12rpx;
  color: rgba(255, 248, 232, 0.94);
  font-size: 27rpx;
  line-height: 1.5;
}

.form-card,
.section-card,
.share-card {
  margin-top: 24rpx;
  border: 1rpx solid #f0dfcf;
  border-radius: 24rpx;
  background: #fff;
  box-shadow: 0 12rpx 32rpx rgba(82, 45, 24, 0.07);
}

.form-card {
  overflow: hidden;
}

.field-row {
  display: grid;
  grid-template-columns: 54rpx 156rpx 1fr;
  align-items: center;
  min-height: 104rpx;
  padding: 0 28rpx;
  border-bottom: 1rpx solid #f0dfcf;
}

.field-row:last-child {
  border-bottom: 0;
}

.field-icon {
  display: grid;
  place-items: center;
  width: 34rpx;
  height: 34rpx;
  border-radius: 50%;
  background: #fff0ea;
  color: #d52322;
  font-size: 20rpx;
  font-weight: 900;
}

.field-label,
.section-title,
.switch-title {
  color: #171c2a;
  font-size: 30rpx;
  font-weight: 900;
}

.field-input {
  height: 84rpx;
  color: #171c2a;
  font-size: 27rpx;
}

.placeholder {
  color: #b8afa7;
}

.section-card,
.share-card {
  padding: 28rpx;
}

.section-title {
  display: block;
  margin-bottom: 18rpx;
}

.textarea {
  box-sizing: border-box;
  width: 100%;
  min-height: 170rpx;
  padding: 22rpx;
  border: 1rpx solid #ead8ca;
  border-radius: 18rpx;
  background: #fffdfb;
  color: #171c2a;
  font-size: 27rpx;
  line-height: 1.55;
}

.textarea.tall {
  min-height: 220rpx;
}

.switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  padding: 22rpx 0;
  border-bottom: 1rpx solid #f0dfcf;
}

.switch-row:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.switch-title,
.switch-desc {
  display: block;
}

.switch-desc {
  margin-top: 6rpx;
  color: #8a7768;
  font-size: 24rpx;
}

.share-url {
  display: block;
  padding: 18rpx;
  border-radius: 16rpx;
  background: #fff8ef;
  color: #7b5a45;
  font-size: 24rpx;
  line-height: 1.5;
  word-break: break-all;
}

.share-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
  margin-top: 18rpx;
}

.ghost-button {
  height: 78rpx;
  margin: 0;
  border: 1rpx solid #ead8ca;
  border-radius: 18rpx;
  background: #fffaf5;
  color: #9e4d32;
  font-size: 27rpx;
  font-weight: 900;
  line-height: 78rpx;
}

.footer-safe {
  height: 132rpx;
}

.sticky-submit {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 20;
  padding: 18rpx 24rpx calc(18rpx + env(safe-area-inset-bottom));
  background: rgba(255, 248, 239, 0.96);
  box-shadow: 0 -8rpx 28rpx rgba(72, 45, 24, 0.08);
}

.primary-button {
  height: 92rpx;
  margin: 0;
  border-radius: 18rpx;
  background: linear-gradient(135deg, #e83a32, #c91419);
  color: #fff;
  font-size: 30rpx;
  font-weight: 900;
  line-height: 92rpx;
}

.primary-button::after,
.ghost-button::after {
  border: 0;
}
</style>
