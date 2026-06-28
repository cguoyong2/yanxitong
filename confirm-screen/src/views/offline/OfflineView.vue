<template>
  <main class="screen">
    <section class="panel offline-panel">
      <div class="offline-mark">
        <span class="dot warn"></span>
      </div>
      <div>
        <p class="eyebrow">连接已断开</p>
        <h1 class="title">确认屏离线</h1>
        <p class="subtitle">请检查网络或服务端连接，然后重新进入待机监听。系统仍可通过最近事件接口恢复最新到账记录。</p>
      </div>
      <div class="status-card">
        <span>{{ binding ? `宴席 ${binding.banquetId}` : '尚未绑定确认屏' }}</span>
        <strong>{{ binding?.bindCode || '需要绑定码' }}</strong>
      </div>
      <div class="actions">
        <button class="primary action" @click="retry">{{ binding ? '重新连接' : '去绑定' }}</button>
        <button v-if="binding" class="secondary" @click="rebind">重新绑定</button>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router';
import { clearBinding, readBinding } from '../../state/confirmScreen';

const router = useRouter();
const binding = readBinding();

async function retry() {
  await router.replace(binding ? '/standby' : '/bind');
}

async function rebind() {
  clearBinding();
  await router.replace('/bind');
}
</script>

<style scoped>
.actions {
  display: flex;
  gap: 14px;
  margin-top: 32px;
  flex-wrap: wrap;
}

.offline-panel {
  display: grid;
  gap: 24px;
}

.offline-mark {
  width: 88px;
  height: 88px;
  display: grid;
  place-items: center;
  border: 1px solid rgba(245, 158, 11, 0.32);
  border-radius: 24px;
  background: rgba(245, 158, 11, 0.12);
}

.offline-mark .dot {
  width: 24px;
  height: 24px;
}

.status-card {
  display: grid;
  gap: 8px;
  padding: 18px;
  border: 1px solid rgba(255, 230, 180, 0.16);
  border-radius: 18px;
  background: rgba(255, 244, 220, 0.08);
}

.status-card span {
  color: #f1d8b5;
}

.status-card strong {
  font-size: 24px;
}

.action {
  min-width: 160px;
  padding: 0 22px;
}
</style>
