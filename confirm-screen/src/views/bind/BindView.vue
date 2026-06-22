<template>
  <main class="screen">
    <section class="panel">
      <p class="eyebrow">确认屏绑定</p>
      <h1 class="title">连接宴席现场</h1>
      <p class="subtitle">输入宴席 ID 和后台生成的绑定码，确认屏将监听该宴席的礼金成功事件。</p>

      <form class="form" @submit.prevent="submit">
        <label class="field">
          宴席 ID
          <input v-model.trim="banquetId" inputmode="numeric" placeholder="例如 10001" required />
        </label>
        <label class="field">
          绑定码
          <input v-model.trim="bindCode" autocomplete="off" placeholder="例如 CS-20260622" required />
        </label>
        <button class="primary" :disabled="loading">{{ loading ? '绑定中...' : '绑定确认屏' }}</button>
        <p v-if="error" class="error">{{ error }}</p>
      </form>
    </section>
  </main>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { bindConfirmScreen } from '../../api/client';
import { readBinding, saveBinding } from '../../state/confirmScreen';

const router = useRouter();
const existing = readBinding();
const banquetId = ref(existing?.banquetId ? String(existing.banquetId) : '');
const bindCode = ref(existing?.bindCode ?? '');
const loading = ref(false);
const error = ref('');

async function submit() {
  const parsedBanquetId = Number(banquetId.value);
  if (!Number.isInteger(parsedBanquetId) || parsedBanquetId <= 0) {
    error.value = '宴席 ID 必须是正整数';
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    const binding = await bindConfirmScreen({
      banquetId: parsedBanquetId,
      bindCode: bindCode.value
    });
    saveBinding(binding);
    await router.push({ path: '/standby', query: { banquetId: binding.banquetId, bindCode: binding.bindCode } });
  } catch (err) {
    error.value = err instanceof Error ? err.message : '绑定失败';
  } finally {
    loading.value = false;
  }
}
</script>
