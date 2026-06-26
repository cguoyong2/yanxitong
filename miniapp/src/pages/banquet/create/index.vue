<template>
  <view class="page" :style="{ background: activeDesign.pageBg }">
    <view class="hero" :style="{ background: activeDesign.heroBg }">
      <text class="hero-pattern hero-pattern-left">{{ activeDesign.mark }}</text>
      <text class="hero-pattern hero-pattern-right">{{ activeDesign.mark }}</text>
      <view class="hero-head">
        <view>
          <text class="eyebrow">宴席通</text>
          <text class="hero-title">创建宴席</text>
        </view>
        <text class="hero-mark">{{ activeDesign.mark }}</text>
      </view>
      <text class="hero-copy">{{ activeDesign.headline }}</text>
      <text class="hero-subcopy">{{ activeDesign.copy }}</text>
      <view class="hero-meta">
        <text>{{ activeDesign.mood }}</text>
        <text>{{ selectedType?.defaultThemeName || '主题待选' }}</text>
      </view>
    </view>

    <view class="section type-section">
      <view class="section-head">
        <view class="section-title-wrap">
          <text class="step-badge">1</text>
          <text class="section-title">选择宴席类型</text>
        </view>
        <text class="section-note">切换后同步调整主题、色彩和推荐模板</text>
      </view>
      <scroll-view scroll-x class="type-scroll" show-scrollbar="false">
        <view
          v-for="(item, index) in eventTypes"
          :key="item.eventTypeCode"
          class="type-chip"
          :class="{ active: form.eventTypeCode === item.eventTypeCode }"
          :style="eventTypeCardStyle(item)"
          @tap="selectEventType(index)"
        >
          <text class="type-mark">{{ designFor(item.eventTypeCode).mark }}</text>
          <text class="type-name">{{ item.name }}</text>
          <text v-if="form.eventTypeCode === item.eventTypeCode" class="type-check">✓</text>
        </view>
      </scroll-view>
      <view class="theme-tip" :style="{ borderColor: activeDesign.lightBorder, background: activeDesign.lightBg }">
        <text class="theme-tip-icon">◉</text>
        <text>已根据宴席类型自动切换主题</text>
        <text class="theme-tip-current">当前主题：{{ selectedType?.name }} · {{ selectedType?.defaultThemeName }}</text>
      </view>
    </view>

    <view class="section form-section">
      <view class="section-head">
        <view class="section-title-wrap">
          <text class="step-badge">2</text>
          <text class="section-title">填写宴席信息</text>
        </view>
        <button class="mini-action" size="mini" @tap="fillDemoData">体验数据</button>
      </view>
      <view class="form-card">
        <view class="form-row">
          <text class="form-icon">▤</text>
          <text class="form-label">宴席名称</text>
          <input v-model="form.name" class="form-input" placeholder="请输入宴席名称" />
        </view>
        <view class="form-row">
          <text class="form-icon">◷</text>
          <text class="form-label">宴席时间</text>
          <input v-model="form.banquetTime" class="form-input" placeholder="2026-10-01T18:00:00" />
        </view>
        <view class="form-row">
          <text class="form-icon">⌖</text>
          <text class="form-label">宴席地点</text>
          <input v-model="form.location" class="form-input" placeholder="请输入宴席地点" />
        </view>
      </view>
      <view class="theme-preview" :style="themePreviewStyle">
        <view class="swatch" :style="{ background: activeDesign.swatch }"></view>
        <view class="theme-copy-block">
          <text class="selected-type-name">{{ selectedTypeName }}</text>
          <text class="theme-name">{{ selectedType?.defaultThemeName }}</text>
          <text class="theme-copy">{{ selectedType?.defaultCopywriting }}</text>
        </view>
      </view>
    </view>

    <view class="section template-section">
      <view class="section-head">
        <view class="section-title-wrap">
          <text class="step-badge">3</text>
          <text class="section-title">选择请柬模板</text>
        </view>
        <text class="section-note">{{ selectedTemplate ? selectedTemplate.name : '请选择模板' }}</text>
      </view>
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
      <scroll-view class="template-scroll" scroll-x show-scrollbar="false">
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
          <view class="template-body">
            <text class="template-name">{{ item.name }}</text>
            <text class="template-desc">{{ item.presentation?.headline || '诚挚邀请' }}</text>
            <view class="template-foot">
              <text class="template-price">{{ templatePrice(item) }}</text>
              <button size="mini" class="preview-btn" @tap.stop="openTemplatePreview(item)">预览</button>
            </view>
          </view>
        </view>
      </scroll-view>
      <view v-if="selectedTemplate" class="selected-template">
        <text class="selected-template-title">当前模板</text>
        <text class="selected-template-copy">{{ selectedTemplate.presentation?.defaultGreeting }}</text>
      </view>
    </view>

    <view class="section copy-section">
      <view class="section-head">
        <view class="section-title-wrap">
          <text class="step-badge">4</text>
          <text class="section-title">收礼文案</text>
        </view>
        <text class="section-note">可选</text>
      </view>
      <textarea v-model="customGiftSuccess" class="textarea" placeholder="自定义收礼成功文案，例如：感谢您的祝福，喜宴现场见。" />
    </view>

    <view class="bottom-bar">
      <button class="secondary-create" @tap="fillDemoData">重填</button>
      <button class="primary-create" :loading="submitting" @tap="submit" :style="{ background: activeDesign.buttonBg }">创建宴席</button>
    </view>

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

interface TypeDesign {
  mark: string;
  eyebrow: string;
  headline: string;
  copy: string;
  mood: string;
  pageBg: string;
  heroBg: string;
  buttonBg: string;
  swatch: string;
  lightBg: string;
  lightBorder: string;
}

const typeDesigns: Record<string, TypeDesign> = {
  WEDDING: {
    mark: '囍',
    eyebrow: '红金婚宴',
    headline: '轻松办好每一场婚宴',
    copy: '红金礼序 · 喜庆体面',
    mood: '喜庆 / 礼序 / 祝福',
    pageBg: 'linear-gradient(180deg, #d91f1b 0%, #d91f1b 315rpx, #fff8ef 316rpx, #fffaf5 100%)',
    heroBg: 'radial-gradient(circle at 78% 18%, rgba(255, 211, 120, 0.36), transparent 26%), linear-gradient(135deg, #d91f1b 0%, #b40f12 55%, #7e0b0b 100%)',
    buttonBg: 'linear-gradient(135deg, #a51f1f, #d45135)',
    swatch: 'linear-gradient(135deg, #9f1d1d, #e4b456)',
    lightBg: '#fff1ea',
    lightBorder: '#f3c6b7'
  },
  BIRTHDAY: {
    mark: '寿',
    eyebrow: '暖金寿宴',
    headline: '福寿绵长，亲友同聚',
    copy: '暖金贺寿 · 稳重温情',
    mood: '福寿 / 团圆 / 感恩',
    pageBg: 'linear-gradient(180deg, #9a2c1d 0%, #9a2c1d 315rpx, #fff7ea 316rpx, #fffaf2 100%)',
    heroBg: 'radial-gradient(circle at 78% 18%, rgba(255, 217, 142, 0.42), transparent 26%), linear-gradient(135deg, #8f1d1d 0%, #9b4b1e 58%, #d7a84a 100%)',
    buttonBg: 'linear-gradient(135deg, #7f1d1d, #b36b2c)',
    swatch: 'linear-gradient(135deg, #7f1d1d, #d7a84a)',
    lightBg: '#fff4e2',
    lightBorder: '#efcc92'
  },
  BABY: {
    mark: '满',
    eyebrow: '满月暖礼',
    headline: '满月之喜，温暖相聚',
    copy: '橙绿暖礼 · 家庭温度',
    mood: '可爱 / 温暖 / 新生命',
    pageBg: 'linear-gradient(180deg, #0f766e 0%, #0f766e 315rpx, #fff7ed 316rpx, #f8fbf4 100%)',
    heroBg: 'radial-gradient(circle at 78% 18%, rgba(255, 214, 158, 0.45), transparent 26%), linear-gradient(135deg, #0f766e 0%, #f08a3c 66%, #ffd59e 100%)',
    buttonBg: 'linear-gradient(135deg, #0f766e, #e8792e)',
    swatch: 'linear-gradient(135deg, #0f766e, #f08a3c)',
    lightBg: '#f0fdfa',
    lightBorder: '#99d8ce'
  },
  HOUSEWARMING: {
    mark: '乔',
    eyebrow: '乔迁新居',
    headline: '新居落成，好运常伴',
    copy: '现代灰橙 · 新居质感',
    mood: '新居 / 邻里 / 好兆头',
    pageBg: 'linear-gradient(180deg, #334155 0%, #334155 315rpx, #fff7ed 316rpx, #f4f6f8 100%)',
    heroBg: 'radial-gradient(circle at 78% 18%, rgba(244, 182, 95, 0.42), transparent 26%), linear-gradient(135deg, #334155 0%, #c65f25 62%, #f4b65f 100%)',
    buttonBg: 'linear-gradient(135deg, #334155, #d56527)',
    swatch: 'linear-gradient(135deg, #334155, #ea8a3a)',
    lightBg: '#fff7ed',
    lightBorder: '#efc49d'
  },
  SCHOOL: {
    mark: '学',
    eyebrow: '升学答谢',
    headline: '金榜题名，答谢亲友',
    copy: '蓝金书卷 · 荣誉成长',
    mood: '荣誉 / 成长 / 答谢',
    pageBg: 'linear-gradient(180deg, #1d4ed8 0%, #1d4ed8 315rpx, #f4f8ff 316rpx, #fffaf0 100%)',
    heroBg: 'radial-gradient(circle at 78% 18%, rgba(240, 180, 41, 0.46), transparent 26%), linear-gradient(135deg, #1d4ed8 0%, #2776d8 58%, #f0b429 100%)',
    buttonBg: 'linear-gradient(135deg, #1d4ed8, #2277c9)',
    swatch: 'linear-gradient(135deg, #1d4ed8, #f0b429)',
    lightBg: '#eef5ff',
    lightBorder: '#b8cdf7'
  },
  MEMORIAL: {
    mark: '忆',
    eyebrow: '素雅追思',
    headline: '慎终追远，思念长存',
    copy: '素雅追思 · 安静庄重',
    mood: '庄重 / 追忆 / 素雅',
    pageBg: 'linear-gradient(180deg, #111827 0%, #111827 315rpx, #f5f5f5 316rpx, #eeeeee 100%)',
    heroBg: 'radial-gradient(circle at 78% 18%, rgba(156, 163, 175, 0.38), transparent 26%), linear-gradient(135deg, #111827 0%, #374151 62%, #6b7280 100%)',
    buttonBg: 'linear-gradient(135deg, #111827, #4b5563)',
    swatch: 'linear-gradient(135deg, #111827, #9ca3af)',
    lightBg: '#f3f4f6',
    lightBorder: '#c5c9cf'
  },
  OTHER: {
    mark: '宴',
    eyebrow: '通用宴席',
    headline: '办宴席，用宴席通',
    copy: '通用暖金 · 清楚高效',
    mood: '通用 / 亲友 / 答谢',
    pageBg: 'linear-gradient(180deg, #92400e 0%, #92400e 315rpx, #fff7ed 316rpx, #fffaf5 100%)',
    heroBg: 'radial-gradient(circle at 78% 18%, rgba(245, 210, 135, 0.46), transparent 26%), linear-gradient(135deg, #92400e 0%, #c27803 58%, #f5d287 100%)',
    buttonBg: 'linear-gradient(135deg, #92400e, #b7791f)',
    swatch: 'linear-gradient(135deg, #92400e, #f5d287)',
    lightBg: '#fff7ed',
    lightBorder: '#efc78d'
  }
};

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

const selectedTypeName = computed(() => eventTypes.value[selectedIndex.value]?.name || '创建宴席');
const selectedType = computed(() => eventTypes.value[selectedIndex.value]);
const selectedTemplate = computed(() => templates.value.find((item) => item.id === form.templateId));
const activeDesign = computed(() => designFor(form.eventTypeCode));
const themePreviewStyle = computed(() => ({
  borderColor: selectedType.value?.primaryColor || '#eadfd3',
  background: '#fffdfa'
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

function designFor(eventTypeCode: string) {
  return typeDesigns[eventTypeCode] || typeDesigns.OTHER;
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
  const design = designFor(item.eventTypeCode);
  return {
    borderColor: selected ? 'transparent' : '#eee1d5',
    background: selected ? design.buttonBg : '#fffdfa',
    color: selected ? '#ffffff' : '#1f2937'
  };
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
    'wedding-red-gold': 'linear-gradient(135deg, #991b1b, #dc6b2f 55%, #e9bf5a)',
    'birthday-warm': 'linear-gradient(135deg, #7f1d1d, #a85d2b 58%, #d7a84a)',
    'baby-garden': 'linear-gradient(135deg, #0f766e, #f08a3c 60%, #ffd59e)',
    'house-modern': 'linear-gradient(135deg, #334155, #d56527 58%, #f4b65f)',
    'school-honor': 'linear-gradient(135deg, #1d4ed8, #2776d8 58%, #f0b429)',
    'memorial-simple': 'linear-gradient(135deg, #111827, #4b5563 60%, #9ca3af)',
    'general-warm': 'linear-gradient(135deg, #92400e, #c27803 58%, #f5d287)'
  };
  return { background: palettes[item.presentation?.styleCode || ''] || activeDesign.value.heroBg };
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
  padding: 0 24rpx 152rpx;
  color: #172033;
}

.hero {
  position: relative;
  min-height: 360rpx;
  margin: 0 -24rpx;
  overflow: hidden;
  padding: 52rpx 48rpx 74rpx;
  color: #fff;
}

.hero::after {
  position: absolute;
  right: -90rpx;
  bottom: -112rpx;
  width: 280rpx;
  height: 280rpx;
  border: 2rpx solid rgba(255, 232, 170, 0.32);
  border-radius: 50%;
  content: '';
}

.hero-pattern {
  position: absolute;
  color: rgba(255, 236, 182, 0.12);
  font-size: 188rpx;
  font-weight: 900;
  line-height: 1;
}

.hero-pattern-left {
  left: 34rpx;
  bottom: 34rpx;
}

.hero-pattern-right {
  right: 50rpx;
  top: 86rpx;
  font-size: 126rpx;
}

.hero-head,
.hero-meta,
.section-head,
.template-foot,
.field-row,
.section-title-wrap,
.form-row,
.theme-tip {
  display: flex;
}

.hero-head,
.section-head,
.template-foot,
.section-title-wrap,
.form-row,
.theme-tip {
  align-items: center;
}

.hero-head,
.section-head,
.template-foot {
  justify-content: space-between;
}

.eyebrow,
.hero-title,
.hero-copy,
.hero-meta,
.section-title,
.section-note,
.field-label,
.type-mark,
.type-name,
.type-theme,
.selected-type-name,
.theme-name,
.theme-copy,
.template-name,
.template-desc,
.template-price,
.selected-template-title,
.selected-template-copy {
  display: block;
}

.eyebrow {
  color: rgba(255, 238, 197, 0.86);
  font-size: 28rpx;
  font-weight: 700;
}

.hero-title {
  margin-top: 26rpx;
  color: #ffe9b5;
  font-size: 62rpx;
  font-weight: 900;
  line-height: 1.16;
  text-shadow: 0 6rpx 20rpx rgba(61, 7, 7, 0.28);
}

.hero-mark {
  display: grid;
  width: 92rpx;
  height: 92rpx;
  place-items: center;
  border: 1rpx solid rgba(255, 255, 255, 0.42);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.14);
  font-size: 42rpx;
  font-weight: 900;
}

.hero-copy {
  margin-top: 18rpx;
  color: #fff7df;
  font-size: 30rpx;
  font-weight: 700;
  letter-spacing: 0;
  line-height: 1.62;
}

.hero-subcopy {
  display: block;
  margin-top: 8rpx;
  color: rgba(255, 247, 223, 0.82);
  font-size: 24rpx;
}

.hero-meta {
  gap: 12rpx;
  flex-wrap: wrap;
  margin-top: 32rpx;
}

.hero-meta text {
  padding: 8rpx 14rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.16);
  color: rgba(255, 255, 255, 0.92);
  font-size: 22rpx;
}

.section {
  margin-top: 20rpx;
  padding: 24rpx;
  border: 1rpx solid rgba(120, 81, 48, 0.12);
  border-radius: 8rpx;
  background: rgba(255, 253, 250, 0.98);
  box-shadow: 0 14rpx 34rpx rgba(81, 50, 29, 0.08);
}

.type-section {
  margin-top: 18rpx;
}

.section-head {
  gap: 18rpx;
  margin-bottom: 18rpx;
}

.section-title-wrap {
  gap: 12rpx;
  min-width: 0;
}

.step-badge {
  display: grid;
  width: 34rpx;
  height: 34rpx;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 8rpx;
  background: linear-gradient(135deg, #c51f1f, #8f1414);
  color: #fff;
  font-size: 22rpx;
  font-weight: 900;
}

.section-title {
  color: #172033;
  font-size: 30rpx;
  font-weight: 900;
}

.section-note {
  min-width: 0;
  overflow: hidden;
  color: #7b6a5b;
  font-size: 22rpx;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.type-scroll {
  width: 100%;
  white-space: nowrap;
}

.type-chip {
  position: relative;
  box-sizing: border-box;
  display: inline-block;
  width: 126rpx;
  min-height: 132rpx;
  margin-right: 16rpx;
  padding: 16rpx 12rpx;
  border: 2rpx solid #ead7be;
  border-radius: 8rpx;
  text-align: center;
  vertical-align: top;
}

.type-chip.active {
  box-shadow: 0 12rpx 26rpx rgba(73, 45, 31, 0.16);
}

.type-chip:active,
.template-card:active,
.mini-action:active,
.primary-create:active,
.secondary-create:active {
  opacity: 0.78;
}

.type-mark {
  font-size: 32rpx;
  font-weight: 900;
  line-height: 1;
}

.type-name {
  margin-top: 14rpx;
  font-size: 25rpx;
  font-weight: 900;
}

.type-check {
  position: absolute;
  right: -8rpx;
  bottom: -8rpx;
  display: grid;
  width: 34rpx;
  height: 34rpx;
  place-items: center;
  border-radius: 50%;
  background: #fff4d8;
  color: #9f1d1d;
  font-size: 22rpx;
  font-weight: 900;
}

.theme-tip {
  gap: 12rpx;
  margin: 24rpx 24rpx 0 0;
  padding: 18rpx;
  border: 1rpx solid #f3c6b7;
  border-radius: 8rpx;
  color: #9f1d1d;
  font-size: 23rpx;
}

.theme-tip-icon,
.theme-tip-current {
  flex: 0 0 auto;
}

.theme-tip-current {
  margin-left: auto;
}

.form-card {
  overflow: hidden;
  border: 1rpx solid #eadfd3;
  border-radius: 8rpx;
  background: #fff;
}

.form-row {
  min-height: 100rpx;
  padding: 0 20rpx;
  border-bottom: 1rpx solid #f1e6da;
}

.form-row:last-child {
  border-bottom: 0;
}

.form-icon {
  width: 46rpx;
  flex: 0 0 auto;
  color: #c51f1f;
  font-size: 30rpx;
  font-weight: 900;
}

.form-label {
  width: 150rpx;
  flex: 0 0 auto;
  color: #26211d;
  font-size: 28rpx;
  font-weight: 800;
}

.form-input {
  min-width: 0;
  flex: 1;
  height: 100rpx;
  color: #172033;
  font-size: 28rpx;
}

.field-row {
  gap: 16rpx;
}

.field-label {
  margin-bottom: 10rpx;
  color: #65584e;
  font-size: 24rpx;
  font-weight: 700;
}

.input,
.textarea {
  box-sizing: border-box;
  width: 100%;
  border: 1rpx solid #eadfd3;
  border-radius: 8rpx;
  background: #fff;
  color: #172033;
  font-size: 26rpx;
}

.input {
  height: 78rpx;
  padding: 0 22rpx;
}

.textarea {
  min-height: 170rpx;
  padding: 20rpx 22rpx;
  line-height: 1.5;
}

.mini-action {
  flex: 0 0 auto;
  margin: 0;
  border: 1rpx solid #eadfd3;
  color: #7c2d12;
  background: #fff7ed;
}

.theme-preview {
  display: flex;
  gap: 18rpx;
  align-items: center;
  margin-top: 22rpx;
  padding: 20rpx;
  border: 1rpx solid #eadfd3;
  border-radius: 8rpx;
}

.swatch {
  flex: 0 0 auto;
  width: 72rpx;
  height: 72rpx;
  border-radius: 8rpx;
}

.theme-copy-block {
  min-width: 0;
}

.selected-type-name {
  color: #172033;
  font-size: 30rpx;
  font-weight: 900;
}

.theme-name {
  margin-top: 6rpx;
  color: #4b5563;
  font-size: 24rpx;
  font-weight: 700;
}

.theme-copy {
  margin-top: 6rpx;
  color: #756a61;
  font-size: 23rpx;
  line-height: 1.45;
}

.template-section {
  padding-right: 0;
}

.template-tabs {
  display: flex;
  gap: 12rpx;
  margin: 0 24rpx 18rpx 0;
}

.template-tabs button {
  flex: 1;
  margin: 0;
  border: 1rpx solid #eadfd3;
  border-radius: 8rpx;
  background: #fff;
  color: #67564a;
}

.template-tabs button.active {
  border-color: #172033;
  color: #172033;
  font-weight: 800;
}

.template-scroll {
  width: 100%;
  white-space: nowrap;
}

.template-card {
  display: inline-block;
  width: 284rpx;
  margin-right: 18rpx;
  overflow: hidden;
  border: 2rpx solid transparent;
  border-radius: 8rpx;
  background: #fff;
  box-shadow: 0 10rpx 24rpx rgba(87, 62, 41, 0.08);
  vertical-align: top;
}

.template-card.selected {
  border-color: #172033;
}

.template-cover {
  display: grid;
  width: 100%;
  height: 172rpx;
  place-items: center;
  color: #fff;
  font-size: 58rpx;
  font-weight: 900;
}

.template-cover image {
  width: 100%;
  height: 100%;
}

.template-body {
  padding: 18rpx;
}

.template-name {
  overflow: hidden;
  color: #172033;
  font-size: 26rpx;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.template-desc {
  margin-top: 8rpx;
  overflow: hidden;
  color: #756a61;
  font-size: 22rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.template-foot {
  gap: 10rpx;
  margin-top: 16rpx;
}

.template-price {
  color: #9f2f22;
  font-size: 23rpx;
  font-weight: 900;
}

.preview-btn {
  margin: 0;
  padding: 0 14rpx;
  border: 1rpx solid #eadfd3;
  color: #172033;
  background: #fffdfa;
  font-size: 21rpx;
}

.selected-template {
  display: grid;
  gap: 8rpx;
  margin: 20rpx 24rpx 0 0;
  padding: 20rpx;
  border-radius: 8rpx;
  background: #f8f1ea;
}

.selected-template-title {
  color: #172033;
  font-size: 24rpx;
  font-weight: 900;
}

.selected-template-copy {
  color: #67564a;
  font-size: 23rpx;
  line-height: 1.5;
}

.bottom-bar {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 10;
  display: grid;
  grid-template-columns: 1fr;
  gap: 18rpx;
  padding: 18rpx 24rpx 34rpx;
  border-top: 1rpx solid rgba(120, 81, 48, 0.12);
  background: rgba(255, 253, 250, 0.96);
}

.secondary-create,
.primary-create {
  height: 82rpx;
  margin: 0;
  border-radius: 8rpx;
  font-size: 28rpx;
  font-weight: 900;
  line-height: 82rpx;
}

.secondary-create {
  display: none;
  border: 1rpx solid #eadfd3;
  color: #67564a;
  background: #fff;
}

.primary-create {
  border: 0;
  color: #fff;
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
  border-radius: 8rpx;
  background: #fff;
}

.preview-hero {
  display: grid;
  height: 260rpx;
  place-items: center;
  color: #fff;
  font-size: 72rpx;
  font-weight: 900;
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
  font-weight: 900;
}

.preview-copy {
  color: #374151;
  line-height: 1.6;
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
