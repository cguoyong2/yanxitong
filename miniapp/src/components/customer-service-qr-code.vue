<template>
  <canvas
    :id="canvasId"
    class="qr-canvas"
    :canvas-id="canvasId"
    type="2d"
    :style="{ width: `${size}px`, height: `${size}px` }"
    @tap="emit('tap')"
  />
</template>

<script setup lang="ts">
import { getCurrentInstance, nextTick, onMounted, watch } from 'vue';
import QRCode from 'uview-plus/components/u-qrcode/qrcode.js';

const props = withDefaults(defineProps<{
  value: string;
  size?: number;
}>(), {
  size: 230
});

const emit = defineEmits<{
  ready: [path: string];
  error: [];
  tap: [];
}>();

const instance = getCurrentInstance();
const canvasId = `customer-service-qr-${Math.floor(Math.random() * 1000000)}`;
let renderVersion = 0;

async function renderQrCode() {
  const value = String(props.value || '').trim();
  if (!value) {
    return;
  }

  const currentVersion = ++renderVersion;
  await nextTick();

  const query = uni.createSelectorQuery().in(instance?.proxy).select(`#${canvasId}`);
  query.fields({ node: true, size: true }).exec((result) => {
    const canvas = result?.[0]?.node;
    if (!canvas) {
      emit('error');
      return;
    }

    canvas.width = props.size;
    canvas.height = props.size;
    const context = canvas.getContext('2d');

    new QRCode({
      canvasId,
      ctx: context,
      isNvue: false,
      vuectx: instance?.proxy,
      usingComponents: true,
      showLoading: false,
      loadingText: '',
      text: value,
      size: props.size,
      background: '#ffffff',
      foreground: '#15261d',
      pdground: '#166534',
      quietZone: 3,
      correctLevel: 1,
      cbResult: () => undefined
    });

    setTimeout(() => {
      if (currentVersion !== renderVersion) {
        return;
      }
      uni.canvasToTempFilePath({
        canvas,
        x: 0,
        y: 0,
        width: props.size,
        height: props.size,
        destWidth: props.size * 2,
        destHeight: props.size * 2,
        fileType: 'png',
        quality: 1,
        success: (response) => emit('ready', response.tempFilePath),
        fail: () => emit('error')
      }, instance?.proxy);
    }, 240);
  });
}

onMounted(renderQrCode);
watch(() => props.value, renderQrCode);
</script>

<style scoped>
.qr-canvas {
  display: block;
}
</style>
