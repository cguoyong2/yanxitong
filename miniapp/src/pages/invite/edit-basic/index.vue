<template>
  <view class="page" :class="activeTheme.tone">
    <view class="hero-card">
      <view class="hero-art">
        <text class="hero-symbol">{{ activeTheme.mark }}</text>
      </view>
      <text class="hero-label">基础请柬</text>
      <text class="hero-title">编辑请柬</text>
      <text class="hero-desc">填写{{ activeTheme.invitationTitle }}展示信息，保存后分享链接会同步更新。</text>
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
      <view class="section-head">
        <text class="section-title">邀请文案</text>
        <view class="inline-button tap-button" @tap.stop="applyThemeCopy()">套用推荐</view>
      </view>
      <view class="text-preview" :class="{ empty: !form.greeting }" @tap.stop="openTextEditor('greeting')">
        <text>{{ form.greeting || '点击填写欢迎语' }}</text>
      </view>
    </view>

    <view class="section-card">
      <view class="section-head">
        <text class="section-title">宴席流程</text>
        <view class="inline-button tap-button" @tap.stop="applyDefaultSchedule()">填入流程</view>
      </view>
      <view class="text-preview tall" :class="{ empty: !form.scheduleText }" @tap.stop="openTextEditor('scheduleText')">
        <text>{{ form.scheduleText || '点击填写流程，每行一个节点，如：17:30 签到' }}</text>
      </view>
    </view>

    <view class="section-card">
      <text class="section-title">入口设置</text>
      <view class="switch-row">
        <view>
          <text class="switch-title">显示{{ activeTheme.onlineGiftLabel }}入口</text>
          <text class="switch-desc">非支付体验版仍会显示暂未开放提示</text>
        </view>
        <switch :checked="form.showGiftEntry" :color="activeThemeAccent" @change="form.showGiftEntry = Boolean($event.detail.value)" />
      </view>
      <view class="switch-row">
        <view>
          <text class="switch-title">显示设备入口</text>
          <text class="switch-desc">用于确认屏、云喇叭租赁入口</text>
        </view>
        <switch :checked="form.showDeviceEntry" :color="activeThemeAccent" @change="form.showDeviceEntry = Boolean($event.detail.value)" />
      </view>
    </view>

    <view v-if="shareUrl" class="share-card">
      <text class="section-title">分享路径</text>
      <text class="share-url">{{ shareUrl }}</text>
      <view class="share-actions">
        <view class="ghost-button tap-button" @tap.stop="previewInvite()">预览请柬</view>
        <view class="ghost-button tap-button" @tap.stop="copyShareUrl()">复制路径</view>
      </view>
    </view>

    <view v-if="lastSavedAt" class="saved-card">
      <text class="section-title">保存结果</text>
      <text class="share-url">已保存：{{ lastSavedAt }}</text>
    </view>

    <view class="action-card">
      <view class="action-grid">
        <view class="ghost-button compact tap-button" @tap.stop="returnBanquetDetail()">返回管理台</view>
        <view class="ghost-button compact tap-button" :class="{ disabled: submitting }" @tap.stop="saveAndPreview()">保存预览</view>
        <view class="primary-button tap-button" :class="{ disabled: submitting }" @tap.stop="submit()">保存请柬</view>
      </view>
    </view>
    <view class="footer-safe"></view>

    <view v-if="textEditor.visible" class="modal-mask" @tap="closeTextEditor()">
      <view class="modal-panel" @tap.stop>
        <view class="modal-head">
          <text class="modal-title">{{ textEditor.field === 'greeting' ? '编辑邀请文案' : '编辑宴席流程' }}</text>
          <text class="modal-close" @tap="closeTextEditor()">×</text>
        </view>
        <textarea
          v-model="textEditor.value"
          class="modal-textarea"
          :placeholder="textEditor.field === 'greeting' ? '请输入欢迎语' : '每行一个节点，如：17:30 签到'"
          placeholder-class="placeholder"
          :adjust-position="false"
          :show-confirm-bar="false"
          :disable-default-padding="true"
        />
        <view class="modal-actions">
          <view class="ghost-button tap-button" @tap.stop="closeTextEditor()">取消</view>
          <view class="primary-button tap-button" @tap.stop="confirmTextEditor()">确认填入</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { request } from '../../../api/client';
import { eventThemeFor, fetchBanquetEventType, readActiveEventType, writeActiveEventType } from '../../../utils/event-theme';
import { readLastBanquetContext, writeLastBanquetContext } from '../../../utils/banquet';

const invitationId = ref('');
const banquetId = ref('');
const submitting = ref(false);
const actionLock = ref('');
const shareUrl = ref('');
const lastSavedAt = ref('');
const eventType = ref(readActiveEventType());
const activeTheme = computed(() => eventThemeFor(eventType.value));
const activeThemeAccent = computed(() => {
  const palette: Record<string, string> = {
    red: '#e60012',
    orange: '#d96a11',
    pink: '#e7566f',
    green: '#188356',
    blue: '#2563eb',
    black: '#2f3338',
    purple: '#7c3aed'
  };
  return palette[activeTheme.value.tone] || '#e60012';
});
const defaultSchedule = computed(() => {
  if (activeTheme.value.code === 'MEMORIAL') {
    return ['09:30 来宾签到', '10:00 追思仪式', '10:30 缅怀致辞', '11:00 亲友致意'].join('\n');
  }
  if (activeTheme.value.code === 'SCHOOL') {
    return ['17:30 来宾签到', '18:00 开席致谢', '18:30 宴席用餐', '20:00 合影留念'].join('\n');
  }
  if (activeTheme.value.code === 'BIRTHDAY') {
    return ['17:30 来宾签到', '18:00 祝寿仪式', '18:30 宴席用餐', '20:00 合影留念'].join('\n');
  }
  return ['17:30 来宾签到', '18:00 仪式开始', '18:30 宴席用餐', '20:00 合影留念'].join('\n');
});
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
const textEditor = reactive({
  visible: false,
  field: 'greeting' as 'greeting' | 'scheduleText',
  value: ''
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
  const cached = readLastBanquetContext();
  if (!banquetId.value && cached?.id) {
    banquetId.value = String(cached.id);
  }
  if (cached?.id) {
    if (cached.eventTypeCode) {
      eventType.value = writeActiveEventType(cached.eventTypeCode);
    }
    writeLastBanquetContext({
      id: cached.id,
      invitationId: Number(invitationId.value) || cached.invitationId,
      shareSlug: shareUrl.value.split('slug=')[1] || cached.shareSlug
    });
  }
}

async function loadInvitation() {
  if (!invitationId.value) {
    return;
  }
  const detail = await request<InvitationDetail>(`/invitations/${invitationId.value}`);
  fillForm(detail);
}

async function submit() {
  if (!beginAction('submit')) {
    return false;
  }
  if (!invitationId.value || !form.title.trim()) {
    uni.showToast({ title: '请填写标题', icon: 'none' });
    return false;
  }
  if (!validateContactPhone()) {
    return false;
  }
  submitting.value = true;
  try {
    await request(`/invitations/${invitationId.value}/basic`, {
      method: 'PUT',
      data: form
    });
    await loadInvitation();
    lastSavedAt.value = new Date().toLocaleTimeString();
    uni.showToast({ title: '已保存', icon: 'success' });
    return true;
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '保存请柬失败', icon: 'none' });
    return false;
  } finally {
    submitting.value = false;
  }
}

function validateContactPhone() {
  const phone = form.contactPhone.trim();
  if (!phone) {
    return true;
  }
  if (/^1[3-9]\d{9}$/.test(phone)) {
    return true;
  }
  uni.showToast({ title: '联系电话需为11位手机号', icon: 'none' });
  return false;
}

function applyThemeCopy() {
  form.greeting = activeTheme.value.invitationCopy;
  uni.showToast({ title: '已套用推荐文案', icon: 'success' });
}

function applyDefaultSchedule() {
  form.scheduleText = defaultSchedule.value;
  uni.showToast({ title: '已填入默认流程', icon: 'success' });
}

function openTextEditor(field: 'greeting' | 'scheduleText') {
  textEditor.field = field;
  textEditor.value = form[field];
  textEditor.visible = true;
}

function closeTextEditor() {
  textEditor.visible = false;
}

function confirmTextEditor() {
  form[textEditor.field] = textEditor.value.trim();
  textEditor.visible = false;
  uni.showToast({ title: '已填入', icon: 'success' });
}

async function saveAndPreview() {
  if (!beginAction('saveAndPreview')) {
    return;
  }
  if (submitting.value) {
    uni.showToast({ title: '正在保存', icon: 'none' });
    return;
  }
  const saved = await submit();
  if (saved) {
    previewInvite();
  }
}

function copyShareUrl() {
  if (!beginAction('copyShareUrl')) {
    return;
  }
  if (!shareUrl.value) {
    uni.showToast({ title: '暂无分享路径', icon: 'none' });
    return;
  }
  uni.setClipboardData({
    data: shareUrl.value,
    success: () => uni.showToast({ title: '已复制', icon: 'success' })
  });
}

function previewInvite() {
  if (!beginAction('previewInvite')) {
    return;
  }
  if (!shareUrl.value) {
    uni.showToast({ title: '暂无分享路径', icon: 'none' });
    return;
  }
  safeNavigate(shareUrl.value, '请柬预览打开失败');
}

function returnBanquetDetail() {
  if (!beginAction('returnBanquetDetail')) {
    return;
  }
  const targetId = banquetId.value || String(readLastBanquetContext()?.id || '');
  if (!targetId) {
    uni.showToast({ title: '返回上一页', icon: 'none' });
    uni.navigateBack();
    return;
  }
  safeNavigate(`/pages/banquet/detail/index?id=${targetId}`, '宴席管理台打开失败');
}

function beginAction(name: string) {
  if (actionLock.value === name) {
    return false;
  }
  actionLock.value = name;
  setTimeout(() => {
    if (actionLock.value === name) {
      actionLock.value = '';
    }
  }, 500);
  return true;
}

function safeNavigate(url: string, failTitle: string) {
  uni.navigateTo({
    url,
    fail: () => {
      uni.redirectTo({
        url,
        fail: () => uni.showToast({ title: failTitle, icon: 'none' })
      });
    }
  });
}

onMounted(async () => {
  const pages = getCurrentPages();
  const current = pages[pages.length - 1] as unknown as { options?: Record<string, string> };
  invitationId.value = current.options?.invitationId || '';
  banquetId.value = current.options?.banquetId || String(readLastBanquetContext()?.id || '');
  form.title = current.options?.title ? decodeURIComponent(current.options.title) : '';
  if (!invitationId.value) {
    uni.showToast({ title: '缺少请柬信息', icon: 'none' });
    setTimeout(() => uni.navigateBack(), 700);
    return;
  }
  if (banquetId.value) {
    eventType.value = writeActiveEventType(await fetchBanquetEventType(banquetId.value, request, eventType.value));
  }
  await loadInvitation();
});
</script>

<style scoped>
.page {
  --accent: #e60012;
  --accent-dark: #c40005;
  --accent-soft: #fff0ee;
  --page-bg: #fff8ef;
  --accent-shadow: rgba(184, 17, 21, 0.22);
  min-height: 100vh;
  padding: 24rpx 24rpx 0;
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
.share-card,
.saved-card {
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
  background: var(--accent-soft);
  color: var(--accent);
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
.share-card,
.saved-card {
  padding: 28rpx;
}

.saved-card {
  border-color: #bfe6c9;
  background: #f4fff6;
}

.section-title {
  display: block;
  margin-bottom: 18rpx;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  margin-bottom: 18rpx;
}

.section-head .section-title {
  margin-bottom: 0;
}

.inline-button {
  box-sizing: border-box;
  flex: 0 0 auto;
  height: 58rpx;
  margin: 0;
  padding: 0 22rpx;
  border: 1rpx solid #ead8ca;
  border-radius: 999rpx;
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 24rpx;
  font-weight: 900;
  line-height: 58rpx;
  text-align: center;
}

.inline-button::after {
  border: 0;
}

.text-preview {
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
  white-space: pre-wrap;
}

.text-preview.empty {
  color: #9aa0aa;
}

.text-preview.tall {
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
  background: var(--accent-soft);
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
  box-sizing: border-box;
  height: 78rpx;
  margin: 0;
  border: 1rpx solid #ead8ca;
  border-radius: 18rpx;
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 27rpx;
  font-weight: 900;
  line-height: 78rpx;
  text-align: center;
}

.tap-button {
  cursor: pointer;
}

.tap-button.disabled {
  opacity: 0.58;
  pointer-events: none;
}

.footer-safe {
  height: calc(24rpx + env(safe-area-inset-bottom));
}

.action-card {
  margin-top: 24rpx;
  padding: 24rpx;
  border: 1rpx solid #f0dfcf;
  border-radius: 24rpx;
  background: #fff;
  box-shadow: 0 12rpx 32rpx rgba(82, 45, 24, 0.07);
}

.action-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1.35fr;
  gap: 12rpx;
}

.action-grid .ghost-button,
.action-grid .primary-button {
  height: 84rpx;
  border-radius: 16rpx;
  font-size: 26rpx;
  line-height: 84rpx;
}

.action-grid .compact {
  font-size: 24rpx;
}

.primary-button {
  box-sizing: border-box;
  height: 92rpx;
  margin: 0;
  border-radius: 18rpx;
  background: linear-gradient(135deg, var(--accent), var(--accent-dark));
  color: #fff;
  font-size: 30rpx;
  font-weight: 900;
  line-height: 92rpx;
  text-align: center;
}

.primary-button::after,
.ghost-button::after {
  border: 0;
}

.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: flex;
  align-items: flex-end;
  padding: 24rpx;
  background: rgba(0, 0, 0, 0.42);
  box-sizing: border-box;
}

.modal-panel {
  width: 100%;
  padding: 28rpx;
  border-radius: 28rpx;
  background: #fff;
  box-sizing: border-box;
}

.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
}

.modal-title {
  color: #171c2a;
  font-size: 32rpx;
  font-weight: 900;
}

.modal-close {
  width: 54rpx;
  height: 54rpx;
  border-radius: 50%;
  background: #f4f0ec;
  color: #7b5a45;
  font-size: 36rpx;
  font-weight: 800;
  line-height: 50rpx;
  text-align: center;
}

.modal-textarea {
  box-sizing: border-box;
  width: 100%;
  height: 320rpx;
  padding: 22rpx;
  border: 1rpx solid #ead8ca;
  border-radius: 18rpx;
  background: #fffdfb;
  color: #171c2a;
  font-size: 27rpx;
  line-height: 1.55;
}

.modal-actions {
  display: grid;
  grid-template-columns: 1fr 1.35fr;
  gap: 16rpx;
  margin-top: 22rpx;
}
</style>
