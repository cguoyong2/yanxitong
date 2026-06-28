<template>
  <view class="page">
    <view class="hero" :class="currentDesign.tone">
      <view class="hero-art">
        <text class="hero-mark">{{ currentDesign.mark }}</text>
      </view>
      <text class="hero-brand">宴席通</text>
      <text class="hero-title">{{ currentDesign.title }}</text>
      <text class="hero-desc">{{ currentDesign.desc }}</text>
      <view class="hero-tags">
        <text v-for="tag in currentDesign.tags" :key="tag">{{ tag }}</text>
      </view>
    </view>

    <view class="content">
      <view class="form-card">
        <view class="form-tools">
          <text class="form-title">填写宴席信息</text>
          <button class="sample-button" @tap="fillSampleData()">填入体验数据</button>
        </view>
        <view class="form-row">
          <text class="row-icon">▤</text>
          <text class="row-label">宴席名称</text>
          <input v-model="form.name" class="row-input" placeholder="请输入宴席名称" />
          <text class="row-arrow">›</text>
        </view>
        <view class="form-row">
          <text class="row-icon">♙</text>
          <text class="row-label">主家姓名</text>
          <input v-model="displayForm.hostName" class="row-input" placeholder="请输入主家姓名" />
        </view>
        <view class="form-row">
          <text class="row-icon">☎</text>
          <text class="row-label">联系电话</text>
          <input
            v-model="displayForm.phone"
            class="row-input"
            type="number"
            maxlength="11"
            placeholder="请输入11位手机号"
            @blur="validatePhone(true)"
          />
        </view>
        <view class="form-row picker-row">
          <text class="row-icon">▣</text>
          <text class="row-label">宴席时间</text>
          <view class="datetime-field">
            <picker mode="date" :value="selectedDate" :start="dateStart" :end="dateEnd" @change="onDateChange">
              <view class="datetime-picker-cell">
                <text class="datetime-display" :class="{ placeholder: !selectedDate }">{{ selectedDate || '选择日期' }}</text>
              </view>
            </picker>
            <picker mode="time" :value="selectedTime" @change="onTimeChange">
              <view class="datetime-picker-cell time">
                <text class="datetime-display" :class="{ placeholder: !selectedTime }">{{ selectedTime || '选择时间' }}</text>
              </view>
            </picker>
            <view class="picker-button" @tap="fillDefaultTime">默认18:00</view>
            <view class="picker-button strong" @tap="openTimePanel">手动填写</view>
          </view>
        </view>
        <view v-if="banquetTimeDisplay" class="selected-time-row">
          <text>已选择：{{ banquetTimeDisplay }}</text>
        </view>
        <view class="form-row location-row" @tap="focusLocationInput">
          <text class="row-icon">⌖</text>
          <text class="row-label">宴席地点</text>
          <view class="location-field">
            <input
              v-model="form.location"
              class="row-input"
              placeholder="可手动输入酒店或宴会厅"
              :focus="locationInputFocused"
              @blur="locationInputFocused = false"
            />
            <button class="map-button" @tap.stop="chooseBanquetLocation">地图选点</button>
            <button class="map-button secondary" @tap.stop="openLocationPanel">手动填写</button>
          </view>
          <text class="row-arrow" @tap.stop="chooseBanquetLocation">›</text>
        </view>
        <view class="map-tip-row">
          <text>可手动输入，也可点“地图选点”搜索酒店、宴会厅或地址。</text>
        </view>
      </view>

      <view class="section-card">
        <view class="section-title-line">
          <text class="red-bar"></text>
          <text class="section-title">宴席类型</text>
        </view>
        <view class="type-grid">
          <view
            v-for="(item, index) in eventTypes"
            :key="item.eventTypeCode"
            class="type-pill"
            :class="{ active: form.eventTypeCode === item.eventTypeCode }"
            @tap="selectEventType(index)"
          >
            <text class="type-icon">{{ designFor(item.eventTypeCode).mark }}</text>
            <text>{{ item.name }}</text>
          </view>
        </view>
      </view>

      <view class="section-card cover-card">
        <view class="section-title-line">
          <text class="red-bar"></text>
          <text class="section-title">宴席封面</text>
        </view>
        <view class="upload-box" @tap="showUploadTip()">
          <text class="upload-icon">▣</text>
          <text class="upload-title">上传封面图</text>
          <text class="upload-desc">建议尺寸 750*500，支持 JPG/PNG 格式</text>
        </view>
      </view>

      <view class="section-card template-card" v-if="filteredTemplates.length">
        <view class="section-head">
          <view class="section-title-line">
            <text class="red-bar"></text>
            <text class="section-title">请柬模板</text>
          </view>
          <text class="section-more">{{ selectedTemplate ? selectedTemplate.name : '请选择' }}</text>
        </view>
        <scroll-view scroll-x class="template-scroll" show-scrollbar="false">
          <view class="template-list">
            <view
              v-for="item in filteredTemplates"
              :key="item.id"
              class="template-item"
              :class="{ selected: form.templateId === item.id }"
              @tap="selectTemplate(item)"
            >
              <view class="template-cover" :style="templateCoverStyle(item)">
                <image v-if="item.coverUrl" :src="item.coverUrl" mode="aspectFill" />
                <text v-else>{{ item.presentation?.fallbackCoverLabel || item.name.slice(0, 2) }}</text>
              </view>
              <text class="template-name">{{ item.name }}</text>
              <text class="template-price">{{ templatePrice(item) }}</text>
            </view>
          </view>
        </scroll-view>
      </view>
    </view>

    <view class="bottom-bar">
      <button class="primary-button" :loading="submitting" @tap="submit()">
        {{ submitting ? '创建中...' : '创建宴席' }}
      </button>
    </view>

    <view v-if="showTimePanel" class="modal-mask" @tap="closeTimePanel">
      <view class="modal-panel" @tap.stop>
        <view class="modal-head">
          <text class="modal-title">填写宴席时间</text>
          <text class="modal-close" @tap="closeTimePanel">×</text>
        </view>
        <text class="modal-desc">如果系统日期选择器没有弹出，可直接在这里填写日期和时间。</text>
        <view class="quick-grid">
          <view v-for="item in quickTimeOptions" :key="item.label" class="quick-chip" @tap="applyQuickTime(item)">
            <text>{{ item.label }}</text>
            <text>{{ item.date }} {{ item.time }}</text>
          </view>
        </view>
        <view class="manual-form">
          <view class="manual-row">
            <text>日期</text>
            <input v-model="manualTime.date" class="manual-input" placeholder="YYYY-MM-DD" />
          </view>
          <view class="manual-row">
            <text>时间</text>
            <input v-model="manualTime.time" class="manual-input" placeholder="HH:mm" />
          </view>
        </view>
        <view class="modal-actions">
          <button class="plain-button" @tap="closeTimePanel">取消</button>
          <button class="confirm-button" @tap="applyManualDateTime">确认填入</button>
        </view>
      </view>
    </view>

    <view v-if="showLocationPanel" class="modal-mask" @tap="closeLocationPanel">
      <view class="modal-panel" @tap.stop>
        <view class="modal-head">
          <text class="modal-title">填写宴席地点</text>
          <text class="modal-close" @tap="closeLocationPanel">×</text>
        </view>
        <text class="modal-desc">可以手动输入酒店、宴会厅或详细地址；地图权限可用时也能继续选点。</text>
        <input v-model="manualLocation" class="manual-input location-input" placeholder="请输入酒店、宴会厅或地址" />
        <view class="quick-grid location">
          <view v-for="item in locationSuggestions" :key="item" class="quick-chip" @tap="applyLocationSuggestion(item)">
            <text>{{ item }}</text>
          </view>
        </view>
        <view class="modal-actions">
          <button class="plain-button" @tap="chooseLocationFromPanel">地图选点</button>
          <button class="confirm-button" @tap="applyManualLocation">确认填入</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { request } from '../../../api/client';
import { writeLastBanquetContext } from '../../../utils/banquet';
import { writeActiveEventType } from '../../../utils/event-theme';

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
  bg: string;
  tone: string;
  title: string;
  desc: string;
  tags: string[];
  greeting: string;
}

const typeDesigns: Record<string, TypeDesign> = {
  WEDDING: { mark: '囍', bg: 'linear-gradient(135deg, #e60012, #c40005)', tone: 'tone-wedding', title: '创建婚宴', desc: '轻松办好每一场婚宴', tags: ['红金礼序', '喜庆体面'], greeting: '诚邀您拨冗赴宴，共同见证我们的幸福时刻' },
  BIRTHDAY: { mark: '寿', bg: 'linear-gradient(135deg, #fff7eb, #ffffff)', tone: 'tone-birthday', title: '创建寿宴', desc: '福寿安康，亲友同贺', tags: ['寿礼安排', '亲友祝寿'], greeting: '诚邀您拨冗赴宴，共祝福寿安康' },
  BABY: { mark: '满', bg: 'linear-gradient(135deg, #fff4f5, #ffffff)', tone: 'tone-baby', title: '创建满月宴', desc: '喜迎新生，满月同庆', tags: ['满月喜礼', '亲友同喜'], greeting: '诚邀您参加满月宴，共同见证新生命的喜悦' },
  HOUSEWARMING: { mark: '福', bg: 'linear-gradient(135deg, #f1fbf4, #ffffff)', tone: 'tone-house', title: '创建乔迁宴', desc: '乔迁新居，福至新门', tags: ['新居入伙', '亲友同贺'], greeting: '诚邀您莅临乔迁宴，共贺新居之喜' },
  SCHOOL: { mark: '学', bg: 'linear-gradient(135deg, #f0f6ff, #ffffff)', tone: 'tone-school', title: '创建升学宴', desc: '金榜题名，前程似锦', tags: ['升学庆贺', '谢师亲友'], greeting: '诚邀您参加升学宴，共同分享金榜题名的喜悦' },
  MEMORIAL: { mark: '念', bg: 'linear-gradient(135deg, #f4f4f5, #ffffff)', tone: 'tone-memorial', title: '创建追思会', desc: '慎终追远，思念长存', tags: ['追思故人', '缅怀永存'], greeting: '诚邀您参加追思会，共同追忆往昔，寄托哀思' },
  OTHER: { mark: '宴', bg: 'linear-gradient(135deg, #fff7eb, #ffffff)', tone: 'tone-other', title: '创建宴席', desc: '按场景配置宴席流程', tags: ['灵活类型', '有序管理'], greeting: '诚邀您拨冗赴宴，共同见证这份重要时刻' }
};

const eventTypes = ref<EventType[]>([]);
const templates = ref<InvitationTemplate[]>([]);
const selectedIndex = ref(0);
const submitting = ref(false);
const customGiftSuccess = ref('');
const selectedDate = ref('');
const selectedTime = ref('');
const locationInputFocused = ref(false);
const showTimePanel = ref(false);
const showLocationPanel = ref(false);
const manualTime = reactive({
  date: '',
  time: ''
});
const manualLocation = ref('');
const displayForm = reactive({
  hostName: '',
  phone: ''
});
const form = reactive({
  name: '',
  eventTypeCode: '',
  banquetTime: '',
  location: '',
  templateId: undefined as number | undefined
});
const initialEventTypeCode = ref('');
const initialTemplateId = ref<number>();

const selectedTemplate = computed(() => templates.value.find((item) => item.id === form.templateId));
const currentDesign = computed(() => designFor(form.eventTypeCode || 'OTHER'));
const yearOptions = computed(() => {
  const currentYear = new Date().getFullYear();
  return Array.from({ length: 8 }, (_, index) => String(currentYear + index));
});
const dateStart = computed(() => `${yearOptions.value[0]}-01-01`);
const dateEnd = computed(() => `${yearOptions.value[yearOptions.value.length - 1]}-12-31`);
const quickTimeOptions = computed(() => {
  const today = new Date();
  const tomorrow = new Date(today);
  tomorrow.setDate(today.getDate() + 1);
  const afterSevenDays = new Date(today);
  afterSevenDays.setDate(today.getDate() + 7);
  return [
    { label: '今天晚宴', date: formatDateInput(today), time: '18:00' },
    { label: '明天晚宴', date: formatDateInput(tomorrow), time: '18:00' },
    { label: '一周后中午', date: formatDateInput(afterSevenDays), time: '12:00' },
    { label: '国庆晚宴', date: `${today.getFullYear()}-10-01`, time: '18:00' }
  ];
});
const banquetTimeDisplay = computed(() => {
  if (!selectedDate.value && !selectedTime.value) {
    return '';
  }
  return `${selectedDate.value || '请选择日期'} ${selectedTime.value || '请选择时间'}`;
});
const filteredTemplates = computed(() => {
  const rows = templates.value.filter((item) => matchesEventType(item, form.eventTypeCode));
  return (rows.length ? rows : templates.value).slice(0, 8);
});
const locationSuggestions = ['幸福大酒店宴会厅', '体验宴会厅', '福泽园宴会厅A厅', '清风园礼仪厅'];

function defaultBanquetName() {
  const now = new Date();
  const stamp = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}${String(now.getHours()).padStart(2, '0')}${String(now.getMinutes()).padStart(2, '0')}`;
  return `体验宴席 ${stamp}`;
}

function fillSampleData() {
  form.name = defaultBanquetName();
  selectedDate.value = '2026-10-01';
  selectedTime.value = '18:00';
  syncBanquetTime();
  form.location = '体验宴会厅';
  displayForm.hostName = '宴席通用户';
  displayForm.phone = '13800000000';
}

function syncBanquetTime() {
  form.banquetTime = selectedDate.value && selectedTime.value ? `${selectedDate.value}T${selectedTime.value}:00` : '';
}

function onDateChange(event: { detail: { value: string } }) {
  selectedDate.value = event.detail.value;
  syncBanquetTime();
  uni.showToast({ title: selectedTime.value ? '时间已更新' : '请选择时间', icon: 'none' });
}

function onTimeChange(event: { detail: { value: string } }) {
  selectedTime.value = event.detail.value;
  syncBanquetTime();
  uni.showToast({ title: selectedDate.value ? '时间已更新' : '请选择日期', icon: 'none' });
}

function fillDefaultTime() {
  if (!selectedDate.value) {
    selectedDate.value = formatDateInput(new Date());
  }
  selectedTime.value = '18:00';
  syncBanquetTime();
  uni.showToast({ title: '已填入默认时间', icon: 'none' });
}

function openTimePanel() {
  manualTime.date = selectedDate.value || formatDateInput(new Date());
  manualTime.time = selectedTime.value || '18:00';
  showTimePanel.value = true;
}

function closeTimePanel() {
  showTimePanel.value = false;
}

function applyQuickTime(item: { label: string; date: string; time: string }) {
  selectedDate.value = item.date;
  selectedTime.value = item.time;
  syncBanquetTime();
  showTimePanel.value = false;
  uni.showToast({ title: '时间已填入', icon: 'success' });
}

function applyManualDateTime() {
  const date = manualTime.date.trim();
  const time = manualTime.time.trim();
  if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) {
    uni.showToast({ title: '日期格式应为YYYY-MM-DD', icon: 'none' });
    return;
  }
  if (!/^([01]\d|2[0-3]):[0-5]\d$/.test(time)) {
    uni.showToast({ title: '时间格式应为HH:mm', icon: 'none' });
    return;
  }
  const parsed = new Date(`${date}T${time}:00`);
  if (Number.isNaN(parsed.getTime())) {
    uni.showToast({ title: '请填写有效日期时间', icon: 'none' });
    return;
  }
  selectedDate.value = date;
  selectedTime.value = time;
  syncBanquetTime();
  showTimePanel.value = false;
  uni.showToast({ title: '时间已填入', icon: 'success' });
}

function formatDateInput(date: Date) {
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

function validatePhone(showToast = false) {
  const phone = displayForm.phone.trim();
  if (!phone) {
    return true;
  }
  if (/^1[3-9]\d{9}$/.test(phone)) {
    return true;
  }
  if (showToast) {
    uni.showToast({ title: '请输入正确的11位手机号', icon: 'none' });
  }
  return false;
}

async function chooseBanquetLocation() {
  locationInputFocused.value = false;
  const allowed = await ensureLocationPermission();
  if (!allowed) {
    return;
  }
  uni.showLoading({ title: '打开地图' });
  try {
    const result = await callChooseLocation();
    applyChosenLocation(result);
  } catch (error) {
    const message = error instanceof Error ? error.message : String((error as { errMsg?: string })?.errMsg || '');
    if (/auth|authorize|permission|denied/i.test(message)) {
      showLocationSettingTip();
      return;
    }
    uni.showToast({ title: '地图未打开，请检查定位权限或手动输入', icon: 'none' });
  } finally {
    uni.hideLoading();
  }
}

function focusLocationInput() {
  locationInputFocused.value = true;
}

function openLocationPanel() {
  manualLocation.value = form.location;
  showLocationPanel.value = true;
}

function closeLocationPanel() {
  showLocationPanel.value = false;
}

function applyLocationSuggestion(value: string) {
  manualLocation.value = value;
}

function applyManualLocation() {
  const value = manualLocation.value.trim();
  if (!value) {
    uni.showToast({ title: '请填写宴席地点', icon: 'none' });
    return;
  }
  form.location = value;
  showLocationPanel.value = false;
  uni.showToast({ title: '地点已填入', icon: 'success' });
}

async function chooseLocationFromPanel() {
  await chooseBanquetLocation();
  if (form.location) {
    manualLocation.value = form.location;
    showLocationPanel.value = false;
  }
}

function ensureLocationPermission() {
  return new Promise<boolean>((resolve) => {
    uni.getSetting({
      success: (setting) => {
        if (setting.authSetting?.['scope.userLocation'] === false) {
          showLocationSettingTip();
          resolve(false);
          return;
        }
        uni.authorize({
          scope: 'scope.userLocation',
          success: () => resolve(true),
          fail: () => {
            showLocationSettingTip();
            resolve(false);
          }
        });
      },
      fail: () => resolve(true)
    });
  });
}

function callChooseLocation() {
  return new Promise<UniApp.ChooseLocationSuccess>((resolve, reject) => {
    const wxApi = typeof wx !== 'undefined' ? wx : undefined;
    if (wxApi?.chooseLocation) {
      wxApi.chooseLocation({
        success: resolve,
        fail: (error) => {
          uni.chooseLocation({ success: resolve, fail: () => reject(error) });
        }
      });
      return;
    }
    uni.chooseLocation({ success: resolve, fail: reject });
  });
}

function applyChosenLocation(result: UniApp.ChooseLocationSuccess) {
  const name = result.name || '';
  const address = result.address || '';
  form.location = name && address ? `${name} ${address}` : name || address || form.location;
  if (form.location) {
    uni.showToast({ title: '地点已填入', icon: 'success' });
  }
}

function showLocationSettingTip() {
  uni.showModal({
    title: '需要定位权限',
    content: '地图选点需要开启定位权限。你也可以先手动输入酒店或地址。',
    confirmText: '去开启',
    cancelText: '手动输入',
    success: (result) => {
      if (result.confirm) {
        uni.openSetting();
      }
    }
  });
}

function designFor(eventTypeCode: string) {
  return typeDesigns[eventTypeCode] || typeDesigns.OTHER;
}

function selectEventType(index: number) {
  selectedIndex.value = index;
  form.eventTypeCode = eventTypes.value[selectedIndex.value]?.eventTypeCode || '';
  pickDefaultTemplate();
}

function selectTemplate(item: InvitationTemplate) {
  form.templateId = item.id;
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

function templatePrice(item: InvitationTemplate) {
  if (item.priceType === 'FREE') {
    return '免费';
  }
  if (item.priceType === 'PLAN_INCLUDED') {
    return '权益包含';
  }
  return `¥${Number(item.price || 0).toFixed(0)}`;
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
  return { background: palettes[item.presentation?.styleCode || ''] || '#e60012' };
}

function showUploadTip() {
  uni.showToast({ title: '当前使用模板封面，自定义上传稍后开放', icon: 'none' });
}

async function loadEventTypes() {
  try {
    eventTypes.value = await request<EventType[]>('/meta/event-types');
    if (eventTypes.value.length > 0) {
      const initialIndex = eventTypes.value.findIndex((item) => item.eventTypeCode === initialEventTypeCode.value);
      selectedIndex.value = initialIndex >= 0 ? initialIndex : 0;
      form.eventTypeCode = eventTypes.value[selectedIndex.value].eventTypeCode;
    }
    templates.value = await request<InvitationTemplate[]>('/meta/invitation-templates');
    if (initialTemplateId.value && templates.value.some((item) => item.id === initialTemplateId.value)) {
      form.templateId = initialTemplateId.value;
    } else {
      pickDefaultTemplate();
    }
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '加载创建配置失败', icon: 'none' });
  }
}

async function submit() {
  if (submitting.value) {
    uni.showToast({ title: '正在创建宴席', icon: 'none' });
    return;
  }
  if (!form.name || !form.eventTypeCode) {
    uni.showToast({ title: '请填写宴席名称和类型', icon: 'none' });
    return;
  }
  if (!validatePhone(true)) {
    return;
  }
  submitting.value = true;
  uni.showLoading({ title: '创建中' });
  try {
    const result = await request<{ banquet: { id: number; name?: string; eventTypeCode?: string; themeCode?: string; banquetTime?: string; location?: string }; invitation?: { id: number; shareSlug?: string } }>('/banquets', {
      method: 'POST',
      data: {
        ...form,
        banquetTime: form.banquetTime || undefined,
        customCopywriting: customGiftSuccess.value
          ? JSON.stringify({ gift_success: customGiftSuccess.value, gift_success_speaker_text: customGiftSuccess.value })
          : undefined
      }
    });
    await syncInvitationBasic(result.invitation?.id).catch(() => {
      uni.showToast({ title: '宴席已创建，请柬信息稍后可编辑', icon: 'none' });
    });
    writeLastBanquetContext({
      id: result.banquet.id,
      name: result.banquet.name || form.name,
      eventTypeCode: result.banquet.eventTypeCode || form.eventTypeCode,
      themeCode: result.banquet.themeCode,
      banquetTime: result.banquet.banquetTime || form.banquetTime,
      location: result.banquet.location || form.location,
      invitationId: result.invitation?.id,
      shareSlug: result.invitation?.shareSlug
    });
    writeActiveEventType(result.banquet.eventTypeCode || form.eventTypeCode);
    uni.showToast({ title: '创建成功', icon: 'success' });
    setTimeout(() => {
      uni.redirectTo({ url: `/pages/banquet/detail/index?id=${result.banquet.id}` });
    }, 450);
  } catch (error) {
    uni.showToast({ title: error instanceof Error ? error.message : '创建失败', icon: 'none' });
  } finally {
    uni.hideLoading();
    submitting.value = false;
  }
}

async function syncInvitationBasic(invitationId?: number) {
  if (!invitationId) {
    return;
  }
  await request(`/invitations/${invitationId}/basic`, {
    method: 'PUT',
    data: {
      title: `${form.name}邀请函`,
      hostName: displayForm.hostName,
      contactPhone: displayForm.phone,
      addressDetail: form.location,
      scheduleText: '',
      greeting: currentDesign.value.greeting,
      showGiftEntry: true,
      showDeviceEntry: true
    }
  });
}

onMounted(() => {
  const pages = getCurrentPages();
  const current = pages[pages.length - 1] as unknown as { options?: Record<string, string> };
  initialEventTypeCode.value = current.options?.eventTypeCode || '';
  initialTemplateId.value = current.options?.templateId ? Number(current.options.templateId) : undefined;
  loadEventTypes();
});
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding-bottom: 126rpx;
  background: #fff8ef;
  color: #151823;
}

.hero {
  position: relative;
  overflow: hidden;
  min-height: 430rpx;
  padding: calc(var(--status-bar-height) + 58rpx) 42rpx 92rpx;
  box-sizing: border-box;
  background:
    radial-gradient(circle at 78% 22%, rgba(255, 214, 146, 0.26), transparent 190rpx),
    linear-gradient(135deg, #e71921 0%, #c9161c 62%, #9b0e13 100%);
  color: #fff8df;
}

.hero.tone-birthday {
  background:
    radial-gradient(circle at 78% 22%, rgba(255, 230, 176, 0.28), transparent 190rpx),
    linear-gradient(135deg, #c15b10 0%, #9d4308 62%, #743005 100%);
}

.hero.tone-baby {
  background:
    radial-gradient(circle at 78% 22%, rgba(255, 221, 230, 0.34), transparent 190rpx),
    linear-gradient(135deg, #e7566f 0%, #c73655 62%, #932742 100%);
}

.hero.tone-house {
  background:
    radial-gradient(circle at 78% 22%, rgba(190, 245, 208, 0.25), transparent 190rpx),
    linear-gradient(135deg, #1b8a58 0%, #116943 62%, #0b4b31 100%);
}

.hero.tone-school {
  background:
    radial-gradient(circle at 78% 22%, rgba(190, 220, 255, 0.25), transparent 190rpx),
    linear-gradient(135deg, #2563eb 0%, #1d4ed8 62%, #1e3a8a 100%);
}

.hero.tone-memorial {
  background:
    radial-gradient(circle at 78% 22%, rgba(255, 255, 255, 0.1), transparent 190rpx),
    linear-gradient(135deg, #202124 0%, #111315 62%, #050607 100%);
}

.hero.tone-other {
  background:
    radial-gradient(circle at 78% 22%, rgba(218, 200, 255, 0.24), transparent 190rpx),
    linear-gradient(135deg, #7c3aed 0%, #5b21b6 62%, #3b0764 100%);
}

.hero-art {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.hero-mark {
  position: absolute;
  right: 46rpx;
  bottom: 18rpx;
  color: rgba(255, 239, 206, 0.18);
  font-size: 210rpx;
  font-weight: 900;
}

.hero-brand,
.hero-title,
.hero-desc,
.hero-tags {
  position: relative;
  z-index: 2;
  display: block;
}

.hero-brand {
  color: #ffe4bd;
  font-size: 28rpx;
  font-weight: 900;
}

.hero-title {
  margin-top: 26rpx;
  font-family: serif;
  font-size: 68rpx;
  font-weight: 900;
  line-height: 1.1;
}

.hero-desc {
  margin-top: 18rpx;
  color: rgba(255, 248, 232, 0.96);
  font-size: 31rpx;
  font-weight: 800;
}

.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 14rpx;
  margin-top: 28rpx;
}

.hero-tags text {
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.16);
  color: #fff2d7;
  font-size: 24rpx;
  font-weight: 800;
}

.content {
  position: relative;
  z-index: 2;
  margin-top: -96rpx;
  padding: 0 40rpx 28rpx;
}

.form-card,
.section-card {
  margin-top: 20rpx;
  padding: 24rpx;
  border-radius: 24rpx;
  background: #fff;
  box-shadow: 0 10rpx 30rpx rgba(43, 35, 31, 0.06);
}

.form-card {
  margin-top: 0;
}

.form-tools {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
  padding-bottom: 18rpx;
  border-bottom: 1rpx solid #efe6df;
}

.form-title {
  color: #171923;
  font-size: 30rpx;
  font-weight: 900;
}

.sample-button {
  flex: 0 0 auto;
  height: 58rpx;
  padding: 0 22rpx;
  border: 1rpx solid #f0cfb6;
  border-radius: 999rpx;
  background: #fffaf4;
  color: #a65a28;
  font-size: 23rpx;
  font-weight: 800;
  line-height: 58rpx;
}

.form-row {
  display: grid;
  grid-template-columns: 46rpx 168rpx 1fr 32rpx;
  align-items: center;
  min-height: 82rpx;
  border-bottom: 1rpx solid #efe6df;
}

.form-row:last-child {
  border-bottom: 0;
}

.row-icon {
  color: #e60012;
  font-size: 32rpx;
  font-weight: 900;
}

.row-label {
  color: #171923;
  font-size: 28rpx;
  font-weight: 900;
}

.row-input {
  min-width: 0;
  color: #171923;
  font-size: 26rpx;
}

.picker-row {
  min-height: 96rpx;
}

.picker-row picker {
  min-width: 0;
}

.datetime-field,
.location-field {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12rpx;
  min-width: 0;
}

.datetime-field {
  width: 100%;
}

.datetime-picker-cell {
  min-width: 156rpx;
  height: 58rpx;
  padding: 0 16rpx;
  border: 1rpx solid #efe1d5;
  border-radius: 999rpx;
  background: #fffaf5;
  line-height: 58rpx;
  box-sizing: border-box;
}

.datetime-picker-cell.time {
  min-width: 132rpx;
}

.datetime-display {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  color: #171923;
  font-size: 26rpx;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.datetime-display.placeholder {
  color: #9aa0aa;
  font-weight: 600;
}

.datetime-actions {
  display: flex;
  flex: 0 0 auto;
  gap: 10rpx;
}

.selected-time-row,
.map-tip-row {
  padding: 0 28rpx 20rpx 144rpx;
  border-bottom: 1rpx solid #efe6df;
  color: #9a5a2c;
  font-size: 23rpx;
  line-height: 1.45;
}

.map-tip-row {
  margin-top: -10rpx;
  color: #8a7768;
}

.picker-button,
.map-button {
  overflow: hidden;
  height: 58rpx;
  padding: 0 16rpx;
  border: 1rpx solid #efe1d5;
  border-radius: 999rpx;
  background: #fffaf5;
  color: #a65a28;
  font-size: 25rpx;
  font-weight: 700;
  line-height: 58rpx;
}

.picker-button.strong,
.map-button.secondary {
  border-color: #e8c09b;
  background: #fff2e6;
  color: #b42318;
}

.map-button {
  flex: 0 0 auto;
  min-width: 92rpx;
  margin: 0;
  padding: 0 18rpx;
}

.map-button.secondary {
  min-width: 104rpx;
}

.map-button::after {
  border: 0;
}

.row-arrow,
.unit {
  color: #7d828d;
  font-size: 34rpx;
  text-align: right;
}

.unit {
  font-size: 26rpx;
}

.section-head,
.section-title-line {
  display: flex;
  align-items: center;
}

.section-head {
  justify-content: space-between;
  gap: 18rpx;
}

.section-title-line {
  gap: 12rpx;
}

.red-bar {
  display: block;
  width: 7rpx;
  height: 34rpx;
  border-radius: 999rpx;
  background: #e60012;
}

.section-title,
.type-pill text,
.upload-title,
.upload-desc,
.template-name,
.template-price,
.section-more {
  display: block;
}

.section-title {
  color: #171923;
  font-size: 30rpx;
  font-weight: 900;
}

.section-more {
  color: #7d828d;
  font-size: 23rpx;
}

.type-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 18rpx;
  margin-top: 22rpx;
}

.type-pill {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10rpx;
  min-width: 132rpx;
  height: 62rpx;
  padding: 0 18rpx;
  border: 1rpx solid #efd9bd;
  border-radius: 999rpx;
  background: #fffaf4;
  color: #9a6a2e;
  font-size: 26rpx;
  font-weight: 800;
}

.type-pill.active {
  border-color: transparent;
  background: linear-gradient(135deg, #e60012, #c40005);
  color: #fff;
}

.type-icon {
  font-size: 30rpx;
  line-height: 1;
}

.upload-box {
  margin-top: 22rpx;
  padding: 32rpx 20rpx;
  border: 2rpx dashed #f0cfb6;
  border-radius: 16rpx;
  background: #fffdf9;
  text-align: center;
}

.upload-icon {
  display: block;
  color: #e60012;
  font-size: 38rpx;
}

.upload-title {
  margin-top: 8rpx;
  color: #e60012;
  font-size: 28rpx;
  font-weight: 900;
}

.upload-desc {
  margin-top: 8rpx;
  color: #8a8f99;
  font-size: 21rpx;
}

.template-scroll {
  width: 100%;
  margin-top: 22rpx;
  white-space: nowrap;
}

.template-list {
  display: inline-flex;
  gap: 18rpx;
  padding-bottom: 2rpx;
}

.template-item {
  width: 158rpx;
  padding: 10rpx;
  border: 2rpx solid transparent;
  border-radius: 16rpx;
  background: #fffaf4;
}

.template-item.selected {
  border-color: #e60012;
}

.template-cover {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 138rpx;
  height: 100rpx;
  overflow: hidden;
  border-radius: 12rpx;
  color: #ffe8bf;
  font-size: 32rpx;
  font-weight: 900;
}

.template-cover image {
  width: 100%;
  height: 100%;
}

.template-name {
  overflow: hidden;
  margin-top: 10rpx;
  color: #171923;
  font-size: 22rpx;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.template-price {
  margin-top: 5rpx;
  color: #e60012;
  font-size: 20rpx;
}

.bottom-bar {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 20;
  padding: 18rpx 40rpx calc(env(safe-area-inset-bottom) + 18rpx);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 -8rpx 26rpx rgba(43, 35, 31, 0.08);
}

button {
  margin: 0;
  padding: 0;
  border: 0;
}

button::after {
  border: 0;
}

.primary-button {
  width: 100%;
  height: 88rpx;
  border-radius: 12rpx;
  background: linear-gradient(135deg, #e60012, #c40005);
  color: #fff;
  font-size: 30rpx;
  font-weight: 900;
  line-height: 88rpx;
}

.modal-mask {
  position: fixed;
  z-index: 40;
  inset: 0;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding: 28rpx;
  box-sizing: border-box;
  background: rgba(17, 24, 39, 0.42);
}

.modal-panel {
  width: 100%;
  max-height: 78vh;
  overflow-y: auto;
  padding: 30rpx;
  border-radius: 30rpx;
  background: #fffaf4;
  box-shadow: 0 -18rpx 48rpx rgba(17, 24, 39, 0.22);
  box-sizing: border-box;
}

.modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
}

.modal-title {
  color: #171923;
  font-size: 34rpx;
  font-weight: 900;
}

.modal-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 58rpx;
  height: 58rpx;
  border-radius: 999rpx;
  background: #f4e8dc;
  color: #7b4a2b;
  font-size: 38rpx;
  font-weight: 700;
}

.modal-desc {
  display: block;
  margin-top: 12rpx;
  color: #7d6b5f;
  font-size: 25rpx;
  line-height: 1.5;
}

.quick-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16rpx;
  margin-top: 24rpx;
}

.quick-grid.location {
  grid-template-columns: 1fr;
}

.quick-chip {
  min-height: 82rpx;
  padding: 18rpx;
  border: 1rpx solid #ecd8c7;
  border-radius: 18rpx;
  background: #fff;
  box-sizing: border-box;
}

.quick-chip text {
  display: block;
  color: #171923;
  font-size: 26rpx;
  font-weight: 800;
  line-height: 1.35;
}

.quick-chip text + text {
  margin-top: 6rpx;
  color: #8a7768;
  font-size: 22rpx;
  font-weight: 600;
}

.manual-form {
  margin-top: 24rpx;
  border: 1rpx solid #ecd8c7;
  border-radius: 20rpx;
  background: #fff;
}

.manual-row {
  display: grid;
  grid-template-columns: 110rpx 1fr;
  align-items: center;
  min-height: 84rpx;
  padding: 0 22rpx;
  border-bottom: 1rpx solid #f0e4da;
  box-sizing: border-box;
}

.manual-row:last-child {
  border-bottom: 0;
}

.manual-row text {
  color: #171923;
  font-size: 27rpx;
  font-weight: 900;
}

.manual-input {
  min-height: 74rpx;
  color: #171923;
  font-size: 27rpx;
}

.manual-input.location-input {
  width: 100%;
  min-height: 84rpx;
  margin-top: 22rpx;
  padding: 0 22rpx;
  border: 1rpx solid #ecd8c7;
  border-radius: 18rpx;
  background: #fff;
  box-sizing: border-box;
}

.modal-actions {
  display: grid;
  grid-template-columns: 1fr 1.35fr;
  gap: 18rpx;
  margin-top: 28rpx;
}

.plain-button,
.confirm-button {
  height: 84rpx;
  border-radius: 18rpx;
  font-size: 28rpx;
  font-weight: 900;
  line-height: 84rpx;
}

.plain-button {
  border: 1rpx solid #e7d4c3;
  background: #fff;
  color: #7b4a2b;
}

.confirm-button {
  border: 0;
  background: linear-gradient(135deg, #d71920, #b91c1c);
  color: #fff8df;
}
</style>
