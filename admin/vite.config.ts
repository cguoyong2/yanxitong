import vue from '@vitejs/plugin-vue';
import { defineConfig } from 'vite';

const proxyTarget = process.env.VITE_PROXY_TARGET || 'http://localhost:8080';

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': proxyTarget
    }
  }
});
