<template>
  <RouterView v-if="isLoginPage" />
  <div v-else class="admin-shell">
    <aside class="sidebar">
      <RouterLink class="brand" to="/dashboard">
        <span class="brand-mark">宴</span>
        <span>
          <strong>宴席通</strong>
          <small>运营后台</small>
        </span>
      </RouterLink>

      <nav class="nav">
        <RouterLink
          v-for="item in navItems"
          :key="item.path"
          class="nav-item"
          :class="{ active: activePath === item.path }"
          :to="item.path"
        >
          <component :is="item.icon" />
          <span>{{ item.label }}</span>
        </RouterLink>
      </nav>
    </aside>

    <section class="workspace">
      <header class="topbar">
        <div>
          <p class="top-eyebrow">Yanxitong Admin</p>
          <h1>{{ currentTitle }}</h1>
        </div>
        <div class="top-actions">
          <el-tag effect="plain" type="success">production / prod</el-tag>
          <span class="profile">{{ profileName }}</span>
          <el-button @click="logout">退出</el-button>
        </div>
      </header>
      <main class="content">
        <RouterView />
      </main>
    </section>
  </div>
</template>

<script setup lang="ts">
import {
  Bell,
  Calendar,
  Collection,
  DataAnalysis,
  Document,
  House,
  List,
  Monitor,
  Money,
  Operation,
  Setting,
  Tickets,
  Tools
} from '@element-plus/icons-vue';
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { clearAuth, getProfile } from './api/client';

const route = useRoute();
const router = useRouter();

const navItems = [
  { path: '/dashboard', label: '工作台', icon: Monitor },
  { path: '/config', label: '配置中心', icon: Setting },
  { path: '/event-types', label: '宴席类型', icon: Collection },
  { path: '/themes', label: '主题配置', icon: Operation },
  { path: '/theme-copywriting', label: '主题文案', icon: Document },
  { path: '/plans', label: '版本权益', icon: Tickets },
  { path: '/templates', label: '模板管理', icon: List },
  { path: '/invitations', label: '请柬管理', icon: Calendar },
  { path: '/devices', label: '设备配置', icon: Tools },
  { path: '/orders', label: '订单管理', icon: Tickets },
  { path: '/banquets', label: '宴席管理', icon: House },
  { path: '/business', label: '业务数据', icon: DataAnalysis },
  { path: '/payments', label: '支付管理', icon: Money },
  { path: '/broadcast-logs', label: '播报日志', icon: Bell },
  { path: '/operation-logs', label: '操作日志', icon: Document }
];

const isLoginPage = computed(() => route.path === '/login');
const activePath = computed(() => {
  const matched = navItems.find((item) => route.path === item.path || route.path.startsWith(`${item.path}/`));
  return matched?.path || '/dashboard';
});
const currentTitle = computed(() => navItems.find((item) => item.path === activePath.value)?.label || '运营后台');
const profileName = computed(() => getProfile()?.displayName || getProfile()?.username || '平台管理员');

function logout() {
  clearAuth();
  router.replace('/login');
}
</script>

<style scoped>
:global(body) {
  margin: 0;
  color: #172033;
  background: #eef2f7;
  font-family:
    Inter,
    ui-sans-serif,
    system-ui,
    -apple-system,
    BlinkMacSystemFont,
    "Segoe UI",
    sans-serif;
}

:global(*) {
  box-sizing: border-box;
}

.admin-shell {
  display: grid;
  grid-template-columns: 236px minmax(0, 1fr);
  min-height: 100vh;
  background:
    linear-gradient(180deg, #f4f7fb 0%, #eef2f7 100%);
}

.sidebar {
  position: sticky;
  top: 0;
  height: 100vh;
  padding: 18px 14px;
  background: #111827;
  color: #d1d5db;
  border-right: 1px solid rgba(255, 255, 255, 0.08);
}

.brand {
  display: flex;
  gap: 12px;
  align-items: center;
  min-height: 56px;
  padding: 8px 10px 18px;
  color: #fff;
  text-decoration: none;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: linear-gradient(135deg, #dc2626, #f59e0b);
  color: #fff7ed;
  font-size: 20px;
  font-weight: 900;
}

.brand strong,
.brand small {
  display: block;
}

.brand strong {
  font-size: 17px;
}

.brand small {
  margin-top: 2px;
  color: #9ca3af;
  font-size: 12px;
}

.nav {
  display: grid;
  gap: 4px;
  padding-top: 8px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.nav-item {
  display: flex;
  gap: 10px;
  align-items: center;
  min-height: 38px;
  padding: 0 12px;
  border-radius: 8px;
  color: #cbd5e1;
  text-decoration: none;
  font-size: 14px;
}

.nav-item svg {
  width: 17px;
  height: 17px;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}

.nav-item.active {
  background: #fff7ed;
  color: #9a3412;
  font-weight: 700;
}

.workspace {
  min-width: 0;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  min-height: 76px;
  padding: 14px 28px;
  background: rgba(255, 255, 255, 0.92);
  border-bottom: 1px solid #e5e7eb;
  backdrop-filter: blur(12px);
}

.top-eyebrow {
  margin: 0 0 4px;
  color: #94a3b8;
  font-size: 12px;
  letter-spacing: 0;
}

.topbar h1 {
  margin: 0;
  color: #111827;
  font-size: 22px;
  font-weight: 800;
}

.top-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.profile {
  color: #475569;
  font-size: 14px;
  font-weight: 700;
}

.content {
  padding: 24px;
}

.content :deep(.page),
.content :deep(.resource-page) {
  min-height: auto;
  padding: 0;
  background: transparent;
}

.content :deep(.page > header),
.content :deep(.resource-page > .toolbar) {
  min-height: 70px;
  margin-bottom: 18px;
  padding: 18px 20px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.04);
}

.content :deep(.page > header h1),
.content :deep(.resource-page > .toolbar h1) {
  color: #0f172a;
  font-size: 22px;
  font-weight: 850;
}

.content :deep(.el-tabs),
.content :deep(.el-table) {
  border-radius: 8px;
}

.content :deep(.el-tabs) {
  padding: 18px;
  border: 1px solid #e2e8f0;
  background: #fff;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.04);
}

.content :deep(.el-tabs__header) {
  margin-bottom: 18px;
}

.content :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background: #e2e8f0;
}

.content :deep(.el-tabs__item) {
  color: #64748b;
  font-weight: 800;
}

.content :deep(.el-tabs__item.is-active) {
  color: #b91c1c;
}

.content :deep(.el-tabs__active-bar) {
  background: linear-gradient(90deg, #dc2626, #f97316);
}

.content :deep(.el-table) {
  overflow: hidden;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.04);
}

.content :deep(.el-table th.el-table__cell) {
  background: #f8fafc;
  color: #334155;
  font-weight: 850;
}

.content :deep(.el-table td.el-table__cell),
.content :deep(.el-table th.el-table__cell) {
  border-color: #eef2f7;
}

.content :deep(.filters),
.content :deep(.toolbar) {
  padding: 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.035);
}

.content :deep(.filters) {
  align-items: center;
}

.content :deep(.metric-grid),
.content :deep(.launch-gate-grid) {
  gap: 12px;
}

.content :deep(.metric),
.content :deep(.launch-gate) {
  min-height: 92px;
  padding: 16px;
  border-color: #e2e8f0;
  background:
    linear-gradient(180deg, #fff 0%, #f8fafc 100%);
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.045);
}

.content :deep(.metric span),
.content :deep(.metric small),
.content :deep(.launch-gate span),
.content :deep(.launch-gate small) {
  color: #64748b;
  font-weight: 750;
}

.content :deep(.metric strong),
.content :deep(.launch-gate strong) {
  color: #0f172a;
  font-size: 28px;
  font-weight: 900;
}

.content :deep(.context-panel),
.content :deep(.readiness-panel),
.content :deep(.audit-panel),
.content :deep(.funnel-panel) {
  border-radius: 8px;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.045);
}

.content :deep(.context-panel) {
  padding: 18px;
}

.content :deep(.provider-card),
.content :deep(.readiness-group),
.content :deep(.readiness-item),
.content :deep(.funnel-step) {
  border-radius: 8px;
}

.content :deep(.el-button) {
  font-weight: 750;
}

.content :deep(.el-button--primary) {
  border-color: #dc2626;
  background: linear-gradient(135deg, #dc2626, #f97316);
}

.content :deep(.el-button--primary:hover),
.content :deep(.el-button--primary:focus) {
  border-color: #b91c1c;
  background: linear-gradient(135deg, #b91c1c, #ea580c);
}

@media (max-width: 980px) {
  .admin-shell {
    grid-template-columns: 1fr;
  }

  .sidebar {
    position: static;
    height: auto;
  }

  .nav {
    grid-template-columns: repeat(auto-fit, minmax(112px, 1fr));
  }

  .topbar {
    position: static;
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
