<template>
  <view class="page">
    <text class="title">{{ pageTitle }}</text>
    <view v-if="!paymentEntryEnabled" class="notice">
      <text>线上随礼和现场扫码暂未开放，请先使用线下记礼流程。</text>
    </view>
    <template v-else>
    <text class="hint">{{ pageHint }}</text>
    <input v-model="form.guestName" class="input" placeholder="姓名" />
    <view class="quick-amounts">
      <button
        v-for="amount in quickAmounts"
        :key="amount"
        size="mini"
        :class="{ active: Number(form.amount) === amount }"
        @click="selectAmount(amount)"
      >
        ¥{{ amount }}
      </button>
    </view>
    <input v-model.number="form.amount" class="input" type="digit" placeholder="金额" />
    <view class="blessing-list">
      <button
        v-for="item in blessingTemplates"
        :key="item"
        size="mini"
        @click="form.blessing = item"
      >
        {{ item }}
      </button>
    </view>
    <textarea v-model="form.blessing" class="textarea" placeholder="祝福语，可选" />
    <picker :range="sources" range-key="label" @change="onSourceChange">
      <view class="input">入口：{{ selectedSourceLabel }}</view>
    </picker>
    <button type="primary" :loading="submitting" @click="submit">{{ submitText }}</button>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { loadRuntimeFeatures, request, type RuntimeFeatures } from '../../../api/client';

const sources = [
  { label: '线上随礼', value: 'ONLINE_GIFT' },
  { label: '现场扫码', value: 'ONSITE_QR' }
];
const selectedIndex = ref(0);
const banquetId = ref('');
const submitting = ref(false);
const clientRequestId = ref('');
const features = ref<RuntimeFeatures>({ mockPaymentEnabled: false });
const quickAmounts = [66, 88, 100, 188, 288, 520, 666, 888];
const blessingTemplates = ['祝福满满，喜乐长久', '百年好合，万事顺意', '福寿安康，阖家欢乐', '学业有成，前程似锦'];
const form = reactive({
  guestName: '',
  amount: 0,
  blessing: '',
  entrySource: 'ONLINE_GIFT'
});
const selectedSourceLabel = computed(() => sources[selectedIndex.value].label);
const isOnsiteQr = computed(() => form.entrySource === 'ONSITE_QR');
const paymentEntryEnabled = computed(() => features.value.mockPaymentEnabled);
const pageTitle = computed(() => isOnsiteQr.value ? '现场扫码随礼' : '线上随礼');
const pageHint = computed(() => isOnsiteQr.value
  ? '现场扫码与线上随礼共用同一支付能力，到账后会推送确认屏并模拟云喇叭播报。'
  : '填写姓名、金额和祝福语，生成统一的在线随礼支付订单。');
const submitText = computed(() => isOnsiteQr.value ? '创建现场扫码订单' : '创建线上随礼订单');

function onSourceChange(event: { detail: { value: number | string } }) {
  selectedIndex.value = Number(event.detail.value);
  form.entrySource = sources[selectedIndex.value].value;
}

function selectAmount(amount: number) {
  form.amount = amount;
}

async function submit() {
  if (!paymentEntryEnabled.value) {
    uni.showToast({ title: '支付入口暂未开放', icon: 'none' });
    return;
  }
  if (!validate()) {
    return;
  }
  submitting.value = true;
  try {
    const result = await request<{ order: { orderNo: string } }>('/gifts/payment-orders', {
      method: 'POST',
      data: { ...form, banquetId: Number(banquetId.value), clientRequestId: ensureClientRequestId() }
    });
    uni.navigateTo({ url: `/pages/gift/success/index?orderNo=${result.order.orderNo}&banquetId=${banquetId.value}` });
  } finally {
    submitting.value = false;
  }
}

function ensureClientRequestId() {
  if (!clientRequestId.value) {
    clientRequestId.value = `gift-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
  }
  return clientRequestId.value;
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
  if (!form.amount || Number(form.amount) <= 0) {
    uni.showToast({ title: '请选择或填写金额', icon: 'none' });
    return false;
  }
  return true;
}

onMounted(async () => {
  const pages = getCurrentPages();
  const current = pages[pages.length - 1] as unknown as { options?: Record<string, string> };
  banquetId.value = current.options?.banquetId || '';
  form.entrySource = current.options?.entrySource || 'ONLINE_GIFT';
  form.guestName = current.options?.guestName ? decodeURIComponent(current.options.guestName) : '';
  form.amount = Number(current.options?.amount || 0);
  selectedIndex.value = form.entrySource === 'ONSITE_QR' ? 1 : 0;
  if (!form.blessing) {
    form.blessing = isOnsiteQr.value ? '现场祝福，万事顺遂' : '祝福满满，喜乐长久';
  }
  features.value = await loadRuntimeFeatures().catch(() => ({ mockPaymentEnabled: false }));
});
</script>

<style scoped>
.page { padding: 24rpx; }
.title { display: block; margin-bottom: 24rpx; font-size: 40rpx; font-weight: 600; }
.hint { display: block; margin-bottom: 20rpx; color: #64748b; font-size: 24rpx; line-height: 1.6; }
.input, .textarea { box-sizing: border-box; width: 100%; margin-bottom: 20rpx; padding: 20rpx; border: 1px solid #ddd; border-radius: 8rpx; }
.textarea { min-height: 140rpx; }
.quick-amounts, .blessing-list { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12rpx; margin-bottom: 20rpx; }
.quick-amounts button, .blessing-list button { margin: 0; border: 1px solid #e5e7eb; background: #fff; }
.quick-amounts button.active { border-color: #b91c1c; color: #b91c1c; }
.blessing-list { grid-template-columns: repeat(2, 1fr); }
.notice { margin-bottom: 20rpx; padding: 20rpx; border: 1px solid #fed7aa; border-radius: 8rpx; background: #fff7ed; color: #9a3412; line-height: 1.6; }
</style>
