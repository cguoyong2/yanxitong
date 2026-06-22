<template>
  <main class="screen standby-screen">
    <section class="standby">
      <header class="topbar">
        <div>
          <p class="eyebrow">确认屏待机</p>
          <p class="binding">宴席 {{ binding?.banquetId ?? '-' }} · 绑定码 {{ binding?.bindCode ?? '-' }}</p>
        </div>
        <div class="status-pill" :class="{ online: connected }">
          <span class="dot" :class="{ online: connected, warn: !connected }"></span>
          <span>{{ connected ? `实时在线 ${binding?.onlineSessions ?? 1}` : connectionText }}</span>
        </div>
      </header>
      <div class="hero-state">
        <div class="pulse-ring"></div>
        <h1 class="standby-title">等待来宾随礼</h1>
        <p class="subtitle">礼金到账后将自动切换至成功展示页，并同步记录确认屏播报日志。</p>
      </div>
      <div v-if="latestEvent" class="latest">
        <span class="latest-label">最近到账</span>
        <strong>{{ latestEvent.guestName }} · ¥{{ formatAmount(latestEvent.amount) }}</strong>
        <small>{{ formatTime(latestEvent.paidAt) }}</small>
      </div>
      <div class="actions">
        <button class="secondary" @click="goBind">重新绑定</button>
        <button class="secondary" @click="reconnect">重新连接</button>
        <button class="secondary" :disabled="!latestEvent" @click="showLatest">查看最近成功事件</button>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import type { ConfirmScreenGiftEvent, ConfirmScreenStatus } from '../../api/client';
import { getConfirmScreenStatus, getLatestConfirmScreenEvent } from '../../api/client';
import { readBinding, saveBinding, saveGiftEvent } from '../../state/confirmScreen';

const route = useRoute();
const router = useRouter();
const binding = ref<ConfirmScreenStatus | null>(readBinding());
const latestEvent = ref<ConfirmScreenGiftEvent | null>(null);
const connected = ref(false);
const connectionText = ref('正在连接');
let socket: WebSocket | null = null;
let heartbeat: number | undefined;

const routeBanquetId = computed(() => Number(route.query.banquetId));
const routeBindCode = computed(() => String(route.query.bindCode ?? ''));

onMounted(async () => {
  if (hasRouteBinding()) {
    binding.value = {
      banquetId: routeBanquetId.value,
      bindCode: routeBindCode.value,
      bindStatus: 'BOUND',
      deviceType: 'CONFIRM_SCREEN',
      online: false,
      onlineSessions: 0
    };
    saveBinding(binding.value);
  }
  if (!binding.value) {
    await router.replace('/bind');
    return;
  }
  await refreshStatus();
  await refreshLatest();
  connect();
});

onBeforeUnmount(() => {
  closeSocket();
});

async function refreshStatus() {
  if (!binding.value?.bindCode) {
    return;
  }
  try {
    const latest = await getConfirmScreenStatus(binding.value.bindCode);
    binding.value = latest;
    saveBinding(latest);
  } catch {
    connectionText.value = '绑定状态读取失败，继续尝试实时连接';
  }
}

async function refreshLatest() {
  if (!binding.value?.banquetId) {
    return;
  }
  try {
    latestEvent.value = await getLatestConfirmScreenEvent(binding.value.banquetId);
  } catch {
    latestEvent.value = null;
  }
}

function connect() {
  if (!binding.value) {
    return;
  }
  closeSocket();
  connectionText.value = '正在连接';
  socket = new WebSocket(wsUrl(binding.value.banquetId));

  socket.onopen = () => {
    connected.value = true;
    connectionText.value = '实时连接中';
    heartbeat = window.setInterval(() => socket?.send('PING'), 15000);
  };

  socket.onmessage = async (message) => {
    const event = parseEvent(message.data);
    if (event?.type === 'GIFT_PAID') {
      latestEvent.value = event;
      saveGiftEvent(event);
      await router.push('/success');
    }
  };

  socket.onerror = () => {
    connected.value = false;
    connectionText.value = '实时连接异常';
  };

  socket.onclose = async () => {
    connected.value = false;
    window.clearInterval(heartbeat);
    heartbeat = undefined;
    if (router.currentRoute.value.path === '/standby') {
      await router.replace('/offline');
    }
  };
}

function closeSocket() {
  window.clearInterval(heartbeat);
  heartbeat = undefined;
  if (socket) {
    socket.onclose = null;
    socket.close();
    socket = null;
  }
}

function reconnect() {
  connect();
}

function hasRouteBinding(): boolean {
  return Number.isInteger(routeBanquetId.value) && routeBanquetId.value > 0 && Boolean(routeBindCode.value);
}

async function goBind() {
  await router.push('/bind');
}

async function showLatest() {
  if (!latestEvent.value) {
    return;
  }
  saveGiftEvent(latestEvent.value);
  await router.push('/success');
}

function parseEvent(data: string): ConfirmScreenGiftEvent | null {
  try {
    return JSON.parse(data) as ConfirmScreenGiftEvent;
  } catch {
    return null;
  }
}

function wsUrl(banquetId: number): string {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocol}//${window.location.host}/ws/confirm-screen?banquetId=${encodeURIComponent(banquetId)}`;
}

function formatAmount(amount: number): string {
  return Number(amount || 0).toFixed(2);
}

function formatTime(value: string): string {
  return value ? value.replace('T', ' ') : '';
}
</script>

<style scoped>
.standby-screen {
  align-items: stretch;
}

.standby {
  width: min(1180px, 100%);
  min-height: calc(100vh - 96px);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.standby-title {
  margin: 0;
  font-size: clamp(54px, 10vw, 126px);
  line-height: 0.96;
  text-align: center;
}

.topbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
}

.binding {
  margin: 0;
  color: #cbd5e1;
}

.status-pill {
  min-height: 42px;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  border: 1px solid rgba(245, 158, 11, 0.38);
  border-radius: 8px;
  background: rgba(245, 158, 11, 0.1);
  color: #fde68a;
  padding: 0 16px;
}

.status-pill.online {
  border-color: rgba(34, 197, 94, 0.38);
  background: rgba(34, 197, 94, 0.1);
  color: #bbf7d0;
}

.hero-state {
  display: grid;
  place-items: center;
  gap: 26px;
  padding: 48px 0 28px;
}

.pulse-ring {
  width: clamp(160px, 22vw, 260px);
  aspect-ratio: 1;
  border: 1px solid rgba(250, 204, 21, 0.32);
  border-radius: 999px;
  background:
    radial-gradient(circle, rgba(250, 204, 21, 0.38), transparent 34%),
    radial-gradient(circle, rgba(185, 28, 28, 0.28), transparent 62%);
  box-shadow:
    0 0 80px rgba(250, 204, 21, 0.18),
    inset 0 0 60px rgba(255, 255, 255, 0.08);
  animation: breathe 2.8s ease-in-out infinite;
}

.hero-state .subtitle {
  width: min(720px, 100%);
  margin: 0;
  text-align: center;
}

.actions {
  display: flex;
  justify-content: center;
  gap: 14px;
  margin-top: 32px;
  flex-wrap: wrap;
}

.latest {
  display: grid;
  gap: 8px;
  width: min(520px, 100%);
  margin: 0 auto;
  padding: 18px;
  border: 1px solid rgba(250, 204, 21, 0.28);
  border-radius: 8px;
  background: rgba(250, 204, 21, 0.08);
}

.latest-label,
.latest small {
  color: #cbd5e1;
}

.latest strong {
  color: #facc15;
  font-size: 26px;
}

@keyframes breathe {
  0%,
  100% {
    transform: scale(0.96);
    opacity: 0.72;
  }
  50% {
    transform: scale(1.04);
    opacity: 1;
  }
}

@media (max-width: 760px) {
  .standby {
    min-height: calc(100vh - 48px);
  }

  .topbar {
    flex-direction: column;
  }

  .actions {
    justify-content: stretch;
  }

  .actions button {
    flex: 1 1 100%;
  }
}
</style>
