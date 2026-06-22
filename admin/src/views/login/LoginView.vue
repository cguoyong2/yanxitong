<template>
  <main class="login-page">
    <section class="login-panel">
      <p class="eyebrow">宴席通运营后台</p>
      <h1>平台管理员登录</h1>
      <el-form class="login-form" label-position="top" @submit.prevent="submit">
        <el-form-item label="账号">
          <el-input v-model.trim="username" autocomplete="username" placeholder="admin" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="password"
            autocomplete="current-password"
            placeholder="admin123"
            show-password
            type="password"
            @keyup.enter="submit"
          />
        </el-form-item>
        <el-button class="login-button" :loading="loading" type="primary" @click="submit">登录</el-button>
        <p v-if="error" class="error">{{ error }}</p>
      </el-form>
    </section>
  </main>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { login } from '../../api/client';

const route = useRoute();
const router = useRouter();
const username = ref('admin');
const password = ref('admin123');
const loading = ref(false);
const error = ref('');

async function submit() {
  if (!username.value || !password.value) {
    error.value = '请输入账号和密码';
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    await login(username.value, password.value);
    await router.replace(String(route.query.redirect || '/dashboard'));
  } catch (err) {
    error.value = err instanceof Error ? err.message : '登录失败';
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 32px;
  background: #f6f7fb;
}

.login-panel {
  width: min(420px, 100%);
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 32px;
  box-shadow: 0 18px 60px rgba(15, 23, 42, 0.08);
}

.eyebrow {
  margin: 0 0 8px;
  color: #64748b;
  font-size: 14px;
}

h1 {
  margin: 0;
  color: #111827;
  font-size: 28px;
}

.login-form {
  margin-top: 28px;
}

.login-button {
  width: 100%;
  min-height: 42px;
}

.error {
  margin: 14px 0 0;
  color: #dc2626;
}
</style>
