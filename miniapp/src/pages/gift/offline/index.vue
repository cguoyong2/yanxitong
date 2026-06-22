<template>
  <view class="page">
    <text class="title">线下记礼</text>
    <input v-model="form.guestName" class="input" placeholder="姓名" />
    <input v-model.number="form.amount" class="input" type="digit" placeholder="金额" />
    <textarea v-model="form.blessing" class="textarea" placeholder="备注，可选" />
    <button type="primary" :loading="submitting" @click="submit">保存</button>
  </view>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { request } from '../../../api/client';

const banquetId = ref('');
const submitting = ref(false);
const form = reactive({ guestName: '', amount: 0, blessing: '' });

async function submit() {
  submitting.value = true;
  try {
    await request('/gifts/offline', { method: 'POST', data: { ...form, banquetId: Number(banquetId.value) } });
    uni.showToast({ title: '已保存', icon: 'success' });
  } finally {
    submitting.value = false;
  }
}

onMounted(() => {
  const pages = getCurrentPages();
  const current = pages[pages.length - 1] as unknown as { options?: Record<string, string> };
  banquetId.value = current.options?.banquetId || '';
});
</script>

<style scoped>
.page { padding: 24rpx; }
.title { display: block; margin-bottom: 24rpx; font-size: 40rpx; font-weight: 600; }
.input, .textarea { box-sizing: border-box; width: 100%; margin-bottom: 20rpx; padding: 20rpx; border: 1px solid #ddd; border-radius: 8rpx; }
.textarea { min-height: 140rpx; }
</style>
