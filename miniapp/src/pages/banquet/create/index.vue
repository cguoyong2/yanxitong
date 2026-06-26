<template>
  <view class="page" :style="{ background: pageBackground }">
    <text class="title">创建宴席</text>
    <input v-model="form.name" class="input" placeholder="宴席名称" />

    <view class="field-title">宴席类型</view>
    <view class="event-type-grid">
      <view
        v-for="(item, index) in eventTypes"
        :key="item.eventTypeCode"
        class="event-type-card"
        :class="{ active: form.eventTypeCode === item.eventTypeCode }"
        :style="eventTypeCardStyle(item)"
        @tap="selectEventType(index)"
      >
        <text class="event-type-name">{{ item.name }}</text>
        <text class="event-type-theme">{{ item.defaultThemeName }}</text>
      </view>
    </view>

    <view v-if="selectedType" class="theme-preview" :style="themePreviewStyle">
      <view class="swatch" :style="{ background: selectedType.primaryColor || '#b91c1c' }"></view>
      <view>
        <text class="selected-type-name">{{ selectedTypeName }}</text>
        <text class="theme-name">{{ selectedType.defaultThemeName }}</text>
        <text class="theme-copy">{{ selectedType.defaultCopywriting }}</text>
      </view>
    </view>
    <view class="field-title">请柬模板</view>
    <view class="template-tabs">
      <button
        v-for="option in filterOptions"
        :key="option.value"
        size="mini"
        :class="{ active: templateFilter === option.value }"
        @tap="setTemplateFilter(option.value)"
      >
        {{ option.label }}
      </button>
    </view>
    <scroll-view class="template-scroll" scroll-x>
      <view
        v-for="item in filteredTemplates"
        :key="item.id"
        class="template-card"
        :class="{ selected: form.templateId === item.id }"
        @tap="selectTemplate(item)"
      >
        <view class="template-cover" :style="templateCoverStyle(item)">
          <image v-if="item.coverUrl" :src="item.coverUrl" mode="aspectFill" />
          <text v-else>{{ item.presentation?.fallbackCoverLabel || item.name.slice(0, 2) }}</text>
        </view>
        <text class="template-name">{{ item.name }}</text>
        <text class="template-desc">{{ item.presentation?.headline || '诚挚邀请' }}</text>
        <text class="template-price">{{ templatePrice(item) }}</text>
        <button size="mini" class="preview-btn" @tap.stop="openTemplatePreview(item)">预览</button>
      </view>
    </scroll-view>
    <view v-if="selectedTemplate" class="selected-template">
      <text>已选：{{ selectedTemplate.name }}</text>
      <text>{{ selectedTemplate.presentation?.defaultGreeting }}</text>
    </view>
    <input v-model="form.banquetTime" class="input" placeholder="宴席时间，例如 2026-10-01T18:00:00" />
    <input v-model="form.location" class="input" placeholder="宴席地点" />
    <textarea v-model="customGiftSuccess" class="textarea" placeholder="自定义收礼成功文案，可选" />
    <button class="quick-fill" @tap="fillDemoData">填入体验数据</button>
    <button type="primary" :loading="submitting" @tap="submit">创建</button>
    <view v-if="previewTemplate" class="preview-mask" @tap="closeTemplatePreview">
      <view class="preview-panel" @tap.stop>
        <view class="preview-hero" :style="templateCoverStyle(previewTemplate)">
          <image v-if="previewTemplate.coverUrl" :src="previewTemplate.coverUrl" mode="aspectFill" />
          <text v-else>{{ previewTemplate.presentation?.fallbackCoverLabel || '宴' }}</text>
        </view>
        <view class="preview-body">
          <text class="preview-kicker">{{ previewTemplate.name }}</text>
          <text class="preview-title">{{ previewTemplate.presentation?.headline || '诚挚邀请' }}</text>
          <text class="preview-copy">{{ previewTemplate.presentation?.defaultGreeting }}</text>
          <view class="preview-schedule">
            <text v-for="item in scheduleItems(previewTemplate)" :key="item">{{ item }}</text>
          </view>
          <button type="primary" @tap="choosePreviewTemplate">选择此模板</button>
          <button @tap="closeTemplatePreview">关闭</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { request } from '../../../api/client';

interface EventType {
  eventTypeCode: string;
  name: string;
  defaultThemeCode: string;
  defaultThemeName: string;
  primaryColor?: string;
  secondaryColor?: string;
  defaultCopywriting?: string;
}

interface InvitationTemplate {
  id: number;
  templateCode: string;
  typeCode: string;
  name: string;
  coverUrl?: string;
  priceType: string;
  price: number;
  presentation?: {
    styleCode: string;
    headline: string;
    defaultGreeting: string;
    defaultScheduleText: string;
    fallbackCoverLabel: string;
  };
}

const eventTypes = ref<EventType[]>([]);
const templates = ref<InvitationTemplate[]>([]);
const selectedIndex = ref(0);
const templateFilter = ref('RECOMMENDED');
const previewTemplate = ref<InvitationTemplate | null>(null);
const submitting = ref(false);
const customGiftSuccess = ref('');
const form = reactive({
  name: defaultBanquetName(),
  eventTypeCode: '',
  banquetTime: '2026-10-01T18:00:00',
  location: '体验宴会厅',
  templateId: undefined as number | undefined
});

const selectedTypeName = computed(() => eventTypes.value[selectedIndex.value]?.name || '请选择宴席类型');
const selectedType = computed(() => eventTypes.value[selectedIndex.value]);
const selectedTemplate = computed(() => templates.value.find((item) => item.id === form.templateId));
const activePrimaryColor = computed(() => selectedType.value?.primaryColor || '#b91c1c');
const activeSecondaryColor = computed(() => selectedType.value?.secondaryColor || '#facc15');
const pageBackground = computed(() => `linear-gradient(180deg, ${softColor(activePrimaryColor.value)} 0%, #fffaf4 220rpx, #fffaf4 100%)`);
const themePreviewStyle = computed(() => ({
  borderColor: activePrimaryColor.value,
  background: `linear-gradient(135deg, ${softColor(activePrimaryColor.value)}, ${softColor(activeSecondaryColor.value)})`
}));
const filterOptions = computed(() => [
  { label: '推荐', value: 'RECOMMENDED' },
  { label: '免费', value: 'FREE' },
  { label: '付费', value: 'PAID' },
  { label: '全部', value: 'ALL' }
]);
const filteredTemplates = computed(() => {
  const eventCode = form.eventTypeCode;
  let rows = templates.value;
  if (templateFilter.value === 'RECOMMENDED') {
    rows = rows.filter((item) => matchesEventType(item, eventCode));
    if (rows.length === 0) {
      rows = templates.value;
    }
  } else if (templateFilter.value !== 'ALL') {
    rows = rows.filter((item) => item.typeCode === templateFilter.value || item.priceType === templateFilter.value);
  }
  return rows;
});

function defaultBanquetName() {
  const now = new Date();
  const stamp = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}${String(now.getHours()).padStart(2, '0')}${String(now.getMinutes()).padStart(2, '0')}`;
  return `体验宴席 ${stamp}`;
}

function fillDemoData() {
  form.name = defaultBanquetName();
  form.banquetTime = '2026-10-01T18:00:00';
  form.location = '体验宴会厅';
  if (!form.eventTypeCode && eventTypes.value.length > 0) {
    form.eventTypeCode = eventTypes.value[0].eventTypeCode;
  }
  pickDefaultTemplate();
  uni.showToast({ title: '已填入体验数据', icon: 'none' });
}

function setTemplateFilter(value: string) {
  templateFilter.value = value;
}

function selectEventType(index: number) {
  selectedIndex.value = index;
  form.eventTypeCode = eventTypes.value[selectedIndex.value]?.eventTypeCode || '';
  templateFilter.value = 'RECOMMENDED';
  pickDefaultTemplate();
}

function eventTypeCardStyle(item: EventType) {
  const selected = form.eventTypeCode === item.eventTypeCode;
  return {
    borderColor: selected ? item.primaryColor || '#b91c1c' : '#eadfd3',
    background: selected
      ? `linear-gradient(135deg, ${item.primaryColor || '#b91c1c'}, ${item.secondaryColor || '#facc15'})`
      : '#fff'
  };
}

function softColor(color: string) {
  const hex = color.replace('#', '');
  if (hex.length !== 6) return '#fff4ed';
  const value = Number.parseInt(hex, 16);
  const red = (value >> 16) & 255;
  const green = (value >> 8) & 255;
  const blue = value & 255;
  return `rgba(${red}, ${green}, ${blue}, 0.12)`;
}

function selectTemplate(item: InvitationTemplate) {
  form.templateId = item.id;
}

function openTemplatePreview(item: InvitationTemplate) {
  previewTemplate.value = item;
}

function closeTemplatePreview() {
  previewTemplate.value = null;
}

function choosePreviewTemplate() {
  if (previewTemplate.value) {
    selectTemplate(previewTemplate.value);
  }
  closeTemplatePreview();
}

function matchesEventType(item: InvitationTemplate, eventTypeCode: string) {
  const code = item.templateCode;
  if (eventTypeCode === 'WEDDING') return code.includes('WEDDING');
  if (eventTypeCode === 'BIRTHDAY') return code.includes('BIRTHDAY');
  if (eventTypeCode === 'BABY') return code.includes('BABY');
  if (eventTypeCode === 'HOUSEWARMING') return code.includes('HOUSE');
  if (eventTypeCode === 'SCHOOL') return code.includes('SCHOOL');
  if (eventTypeCode === 'MEMORIAL') return code.includes('MEMORIAL');
  return code.includes('GENERAL') || code.includes('CEREMONY') || code.includes('CUSTOM');
}

function pickDefaultTemplate() {
  const recommended = templates.value.find((item) => matchesEventType(item, form.eventTypeCode));
  form.templateId = recommended?.id || templates.value[0]?.id;
}

function scheduleItems(item: InvitationTemplate) {
  return (item.presentation?.defaultScheduleText || '')
    .split(/\r?\n/)
    .map((value) => value.trim())
    .filter(Boolean);
}

function templatePrice(item: InvitationTemplate) {
  if (item.priceType === 'FREE') {
    return '免费';
  }
  if (item.priceType === 'PLAN_INCLUDED') {
    return '权益包含';
  }
  return `¥${Number(item.price || 0).toFixed(2)}`;
}

function templateCoverStyle(item: InvitationTemplate) {
  const palettes: Record<string, string> = {
    'wedding-red-gold': 'linear-gradient(135deg, #b91c1c, #facc15)',
    'birthday-warm': 'linear-gradient(135deg, #8f1d1d, #d6a84f)',
    'baby-garden': 'linear-gradient(135deg, #0f766e, #f97316)',
    'house-modern': 'linear-gradient(135deg, #334155, #ea580c)',
    'school-honor': 'linear-gradient(135deg, #1d4ed8, #f59e0b)',
    'memorial-simple': 'linear-gradient(135deg, #111827, #6b7280)',
    'general-warm': 'linear-gradient(135deg, #a16207, #fde68a)'
  };
  return { background: palettes[item.presentation?.styleCode || ''] || palettes['general-warm'] };
}

async function loadEventTypes() {
  try {
    eventTypes.value = await request<EventType[]>('/meta/event-types');
    if (eventTypes.value.length > 0) {
      selectedIndex.value = 0;
      form.eventTypeCode = eventTypes.value[0].eventTypeCode;
    }
    templates.value = await request<InvitationTemplate[]>('/meta/invitation-templates');
    pickDefaultTemplate();
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '加载创建配置失败', icon: 'none' });
  }
}

async function submit() {
  if (!form.name || !form.eventTypeCode) {
    uni.showToast({ title: '请填写宴席名称和类型', icon: 'none' });
    return;
  }
  submitting.value = true;
  try {
    const result = await request<{ banquet: { id: number } }>('/banquets', {
      method: 'POST',
      data: {
        ...form,
        banquetTime: form.banquetTime || undefined,
        customCopywriting: customGiftSuccess.value
          ? JSON.stringify({ gift_success: customGiftSuccess.value, gift_success_speaker_text: customGiftSuccess.value })
          : undefined
      }
    });
    uni.navigateTo({ url: `/pages/banquet/detail/index?id=${result.banquet.id}` });
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '创建失败', icon: 'none' });
  } finally {
    submitting.value = false;
  }
}

onMounted(loadEventTypes);
</script>

<style scoped>
.page {
  box-sizing: border-box;
  min-height: 100vh;
  padding: 24rpx;
}

.title {
  display: block;
  margin-bottom: 24rpx;
  font-size: 40rpx;
  font-weight: 600;
}

.input,
.textarea {
  box-sizing: border-box;
  width: 100%;
  margin-bottom: 20rpx;
  padding: 20rpx;
  border: 1px solid #ddd;
  border-radius: 8rpx;
}

.textarea {
  min-height: 160rpx;
}

.theme-preview {
  display: flex;
  gap: 16rpx;
  align-items: center;
  margin-bottom: 20rpx;
  padding: 20rpx;
  border: 1px solid #ddd;
  border-radius: 8rpx;
}

.field-title {
  margin: 12rpx 0 14rpx;
  color: #111827;
  font-weight: 600;
}

.event-type-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14rpx;
  margin-bottom: 20rpx;
}

.event-type-card {
  box-sizing: border-box;
  min-height: 104rpx;
  padding: 18rpx;
  border: 2rpx solid #eadfd3;
  border-radius: 8rpx;
  color: #172033;
}

.event-type-card.active {
  color: #fff;
  box-shadow: 0 12rpx 28rpx rgba(127, 43, 27, 0.18);
}

.event-type-card:active {
  opacity: 0.78;
}

.event-type-name,
.event-type-theme {
  display: block;
}

.event-type-name {
  font-size: 30rpx;
  font-weight: 800;
}

.event-type-theme {
  margin-top: 8rpx;
  font-size: 22rpx;
  opacity: 0.88;
}

.template-tabs {
  display: flex;
  gap: 12rpx;
  margin-bottom: 14rpx;
}

.template-tabs button {
  margin: 0;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.template-tabs button.active {
  border-color: #b91c1c;
  color: #b91c1c;
}

.template-scroll {
  width: 100%;
  margin-bottom: 20rpx;
  white-space: nowrap;
}

.template-card {
  display: inline-block;
  width: 250rpx;
  margin-right: 18rpx;
  overflow: hidden;
  border: 2rpx solid #e5e7eb;
  border-radius: 8rpx;
  background: #fff;
  vertical-align: top;
}

.template-card.selected {
  border-color: #b91c1c;
}

.template-cover {
  width: 100%;
  height: 150rpx;
  display: grid;
  place-items: center;
  color: #fff;
  font-weight: 700;
}

.template-cover image {
  width: 100%;
  height: 100%;
}

.template-name,
.template-desc,
.template-price {
  display: block;
  padding: 12rpx 14rpx 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.template-name {
  color: #111827;
  font-weight: 600;
}

.template-price {
  padding-top: 6rpx;
  padding-bottom: 14rpx;
  color: #b91c1c;
  font-size: 24rpx;
}

.template-desc {
  padding-top: 4rpx;
  color: #64748b;
  font-size: 22rpx;
}

.preview-btn {
  width: calc(100% - 28rpx);
  margin: 0 14rpx 14rpx;
}

.quick-fill {
  margin: 0 0 20rpx;
  border: 1px solid #e5e7eb;
  color: #7c2d12;
  background: #fff7ed;
}

.selected-template {
  display: grid;
  gap: 6rpx;
  margin-bottom: 20rpx;
  padding: 20rpx;
  border: 1px solid #e5e7eb;
  border-radius: 8rpx;
  color: #374151;
  background: #fff;
  font-size: 24rpx;
}

.swatch {
  width: 52rpx;
  height: 52rpx;
  border-radius: 8rpx;
}

.theme-name,
.theme-copy,
.selected-type-name {
  display: block;
}

.selected-type-name {
  color: #111827;
  font-size: 30rpx;
  font-weight: 800;
}

.theme-name {
  margin-top: 6rpx;
  font-weight: 600;
}

.theme-copy {
  margin-top: 6rpx;
  color: #666;
  font-size: 24rpx;
}

.preview-mask {
  position: fixed;
  inset: 0;
  z-index: 20;
  display: grid;
  place-items: center;
  padding: 36rpx;
  background: rgba(17, 24, 39, 0.45);
}

.preview-panel {
  width: 100%;
  max-height: 90vh;
  overflow: hidden;
  border-radius: 12rpx;
  background: #fff;
}

.preview-hero {
  height: 260rpx;
  display: grid;
  place-items: center;
  color: #fff;
  font-size: 72rpx;
  font-weight: 700;
}

.preview-hero image {
  width: 100%;
  height: 100%;
}

.preview-body {
  display: grid;
  gap: 16rpx;
  padding: 24rpx;
}

.preview-kicker {
  color: #64748b;
  font-size: 24rpx;
}

.preview-title {
  color: #111827;
  font-size: 40rpx;
  font-weight: 700;
}

.preview-copy {
  line-height: 1.6;
  color: #374151;
}

.preview-schedule {
  display: grid;
  gap: 8rpx;
  padding: 18rpx;
  border-radius: 8rpx;
  background: #f8fafc;
  color: #374151;
  font-size: 24rpx;
}
</style>
