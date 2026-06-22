<template>
  <main class="screen success-screen">
    <section v-if="event" class="success">
      <div class="celebration">
        <span></span>
        <span></span>
        <span></span>
      </div>
      <p class="eyebrow">礼金到账</p>
      <h1 class="guest">{{ event.guestName || '来宾' }}</h1>
      <div class="amount">¥{{ formatAmount(event.amount) }}</div>
      <p v-if="event.message" class="message">{{ event.message }}</p>
      <p class="subtitle">{{ formatTime(event.paidAt) }}</p>
      <p class="countdown">{{ secondsLeft }} 秒后返回待机页</p>
    </section>
    <section v-else class="panel">
      <p class="eyebrow">暂无礼金事件</p>
      <h1 class="title">返回待机</h1>
      <button class="primary back" @click="back">回到待机页</button>
    </section>
  </main>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import type { ConfirmScreenGiftEvent } from '../../api/client';
import { readBinding, readGiftEvent } from '../../state/confirmScreen';

const router = useRouter();
const event = ref<ConfirmScreenGiftEvent | null>(readGiftEvent());
const secondsLeft = ref(8);
let timer: number | undefined;
let countdown: number | undefined;

onMounted(() => {
  timer = window.setTimeout(back, 8000);
  countdown = window.setInterval(() => {
    secondsLeft.value = Math.max(0, secondsLeft.value - 1);
  }, 1000);
});

onBeforeUnmount(() => {
  window.clearTimeout(timer);
  window.clearInterval(countdown);
});

async function back() {
  const binding = readBinding();
  await router.replace(binding ? '/standby' : '/bind');
}

function formatAmount(amount: number): string {
  return Number(amount || 0).toFixed(2);
}

function formatTime(value: string): string {
  if (!value) {
    return '';
  }
  return value.replace('T', ' ');
}
</script>

<style scoped>
.success-screen {
  text-align: center;
  background:
    radial-gradient(circle at center, rgba(250, 204, 21, 0.34), transparent 30%),
    radial-gradient(circle at 22% 18%, rgba(239, 68, 68, 0.26), transparent 28%),
    linear-gradient(135deg, #260f0f 0%, #111827 100%);
}

.success {
  width: min(980px, 100%);
  position: relative;
  min-width: 0;
}

.guest {
  margin: 0;
  font-size: clamp(48px, 10vw, 120px);
  line-height: 0.95;
  overflow-wrap: anywhere;
}

.amount {
  margin-top: 28px;
  max-width: 100%;
  color: #facc15;
  font-size: clamp(64px, 14vw, 170px);
  font-weight: 800;
  line-height: 0.95;
  text-shadow: 0 0 36px rgba(250, 204, 21, 0.32);
  overflow-wrap: anywhere;
}

.message {
  width: min(760px, 100%);
  margin: 30px auto 0;
  color: #fff7ed;
  font-size: clamp(24px, 4vw, 44px);
  line-height: 1.24;
  overflow-wrap: anywhere;
}

.back {
  margin-top: 28px;
  width: 180px;
}

.countdown {
  margin: 24px 0 0;
  color: #fde68a;
  font-size: 18px;
}

.celebration {
  height: 80px;
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-bottom: 10px;
}

.celebration span {
  width: 14px;
  height: 72px;
  border-radius: 999px;
  background: linear-gradient(#facc15, #ef4444);
  transform-origin: bottom center;
  animation: rise 1.1s ease-in-out infinite alternate;
}

.celebration span:nth-child(2) {
  animation-delay: 0.16s;
}

.celebration span:nth-child(3) {
  animation-delay: 0.32s;
}

@keyframes rise {
  from {
    transform: translateY(14px) scaleY(0.72);
    opacity: 0.58;
  }
  to {
    transform: translateY(-8px) scaleY(1);
    opacity: 1;
  }
}

@media (max-width: 760px) {
  .celebration {
    height: 64px;
  }

  .celebration span {
    height: 58px;
  }
}
</style>
