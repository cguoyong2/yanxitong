import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vite';

const proxyTarget = process.env.VITE_PROXY_TARGET || 'http://localhost:8080';

export default defineConfig({
  base: '/confirm-screen/',
  plugins: [vue()],
  server: {
    port: 5174,
    proxy: {
      '/api': proxyTarget,
      '/ws': {
        target: proxyTarget.replace(/^http/, 'ws'),
        ws: true
      }
    }
  }
});
