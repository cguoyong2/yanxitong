<template>
  <main class="dashboard">
    <header>
      <h1>宴席通运营后台</h1>
      <el-button :loading="loadingAlerts" @click="loadAlerts">刷新告警</el-button>
    </header>

    <section v-if="readiness" class="readiness" :class="readinessClass">
      <div class="readiness-head">
        <div>
          <span class="eyebrow">运行安全</span>
          <strong>{{ readinessTitle }}</strong>
          <p>{{ readiness.environment }} / {{ readiness.activeProfiles?.length ? readiness.activeProfiles.join(', ') : 'no-profile' }}</p>
        </div>
        <el-tag :type="readinessTagType" effect="dark">{{ readiness.status }}</el-tag>
      </div>
      <div v-if="readiness.blockers?.length || readiness.warnings?.length" class="issue-grid">
        <article v-if="readiness.blockers?.length" class="issue">
          <span>阻塞项</span>
          <el-tag v-for="item in readiness.blockers" :key="item" type="danger" effect="plain">{{ item }}</el-tag>
        </article>
        <article v-if="readiness.warnings?.length" class="issue">
          <span>警告项</span>
          <el-tag v-for="item in readiness.warnings" :key="item" type="warning" effect="plain">{{ item }}</el-tag>
        </article>
      </div>
      <div class="readiness-actions">
        <RouterLink class="text-link" to="/payments">支付上线检查</RouterLink>
        <button class="text-button" type="button" @click="showDetails = !showDetails">{{ showDetails ? '收起明细' : '查看明细' }}</button>
      </div>
      <div v-if="showDetails" class="check-list">
        <article v-for="item in readiness.items" :key="item.code" class="check">
          <el-tag :type="item.passed ? 'success' : item.severity === 'BLOCKER' ? 'danger' : 'warning'" effect="plain">
            {{ item.passed ? '通过' : item.severity }}
          </el-tag>
          <div>
            <strong>{{ item.label }}</strong>
            <p>{{ item.detail }}</p>
          </div>
        </article>
      </div>
    </section>

    <section class="alert-grid">
      <article class="alert-card" :class="{ danger: blockerCount > 0 }">
        <span>Readiness 阻塞</span>
        <strong>{{ blockerCount }}</strong>
        <small>{{ blockerCount > 0 ? '上线前必须处理' : '暂无阻塞' }}</small>
      </article>
      <article class="alert-card" :class="{ warning: warningCount > 0 }">
        <span>Readiness 警告</span>
        <strong>{{ warningCount }}</strong>
        <small>{{ warningCount > 0 ? '建议试点前处理' : '暂无警告' }}</small>
      </article>
      <RouterLink class="alert-card link-card" :class="{ danger: failedCallbackTotal > 0 }" :to="{ path: '/payments', query: { processStatus: 'FAILED' } }">
        <span>失败支付回调</span>
        <strong>{{ failedCallbackTotal }}</strong>
        <small>{{ failedCallbackTotal > 0 ? '进入异常筛选' : '暂无异常' }}</small>
      </RouterLink>
    </section>

    <section class="grid">
      <RouterLink v-for="item in items" :key="item.path" class="entry" :to="item.path">
        <strong>{{ item.title }}</strong>
        <span>{{ item.desc }}</span>
      </RouterLink>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { http, type ApiResponse, type PageResult } from '../../api/client';

interface SecurityReadinessItem {
  code: string;
  label: string;
  passed: boolean;
  severity: 'BLOCKER' | 'WARNING';
  detail: string;
}

interface SecurityReadiness {
  status: 'READY' | 'WARN' | 'BLOCKED';
  productionReady: boolean;
  environment: string;
  activeProfiles: string[];
  blockers: string[];
  warnings: string[];
  items: SecurityReadinessItem[];
}

const items = [
  { path: '/config', title: '配置中心', desc: '通用配置项与系统开关' },
  { path: '/event-types', title: '宴席类型', desc: '类型、别名、默认主题与默认文案' },
  { path: '/themes', title: '主题配置', desc: '颜色、图标风格和确认屏模板' },
  { path: '/theme-copywriting', title: '主题文案', desc: '页面文案与云喇叭播报文案' },
  { path: '/plans', title: '版本权益', desc: '版本价格、单位、权益和排序' },
  { path: '/templates', title: '模板管理', desc: '模板类型、封面、价格类型和上下架' },
  { path: '/devices', title: '设备配置', desc: '云喇叭和确认屏价格、单位、交付方式' },
  { path: '/banquets', title: '宴席管理', desc: '宴席列表、主题编码和公开请柬入口' },
  { path: '/business', title: '业务数据', desc: 'RSVP、礼金记录和人情账本管理' },
  { path: '/payments', title: '支付管理', desc: '支付订单、回调日志和异常处理闭环' },
  { path: '/broadcast-logs', title: '播报日志', desc: '确认屏事件和云喇叭模拟播报记录' },
  { path: '/operation-logs', title: '操作日志', desc: '关键配置和业务操作记录' }
];

const readiness = ref<SecurityReadiness | null>(null);
const failedCallbackTotal = ref(0);
const loadingAlerts = ref(false);
const showDetails = ref(false);

const readinessTitle = computed(() => {
  if (!readiness.value) {
    return '安全检查未加载';
  }
  if (readiness.value.status === 'READY') {
    return '生产安全检查通过';
  }
  if (readiness.value.status === 'WARN') {
    return '存在本地默认配置警告';
  }
  return '存在生产阻塞风险';
});

const readinessTagType = computed(() => {
  if (readiness.value?.status === 'READY') {
    return 'success';
  }
  if (readiness.value?.status === 'WARN') {
    return 'warning';
  }
  return 'danger';
});

const readinessClass = computed(() => ({
  ready: readiness.value?.status === 'READY',
  warn: readiness.value?.status === 'WARN',
  blocked: readiness.value?.status === 'BLOCKED'
}));
const blockerCount = computed(() => readiness.value?.blockers?.length || 0);
const warningCount = computed(() => readiness.value?.warnings?.length || 0);

async function loadAlerts() {
  loadingAlerts.value = true;
  try {
    const [readinessResponse, failedCallbacksResponse] = await Promise.all([
      http.get<ApiResponse<SecurityReadiness>>('/health/readiness'),
      http.get<ApiResponse<PageResult<Record<string, unknown>>>>('/admin/payments/callbacks?processStatus=FAILED&pageSize=1')
    ]);
    readiness.value = readinessResponse.data.data;
    failedCallbackTotal.value = Number(failedCallbacksResponse.data.data?.total || 0);
    showDetails.value = readinessResponse.data.data?.status === 'BLOCKED';
  } finally {
    loadingAlerts.value = false;
  }
}

onMounted(loadAlerts);
</script>

<style scoped>
.dashboard {
  min-height: 100vh;
  padding: 28px;
  background: #f6f7f9;
}

header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

h1 {
  margin: 0 0 20px;
  font-size: 22px;
}

header h1 {
  margin: 0;
}

.readiness {
  display: grid;
  gap: 14px;
  margin-bottom: 18px;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.alert-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.alert-card {
  display: grid;
  gap: 6px;
  min-height: 92px;
  padding: 14px;
  color: #111827;
  text-decoration: none;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.alert-card.warning {
  border-color: #fde68a;
}

.alert-card.danger {
  border-color: #fecaca;
}

.alert-card span {
  color: #6b7280;
  font-size: 12px;
}

.alert-card strong {
  font-size: 28px;
  line-height: 1;
}

.alert-card small {
  color: #6b7280;
}

.link-card:hover {
  border-color: #409eff;
}

.readiness.ready {
  border-color: #a7f3d0;
}

.readiness.warn {
  border-color: #fde68a;
}

.readiness.blocked {
  border-color: #fecaca;
}

.readiness-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.eyebrow,
.issue span {
  display: block;
  margin-bottom: 6px;
  color: #6b7280;
  font-size: 12px;
}

.readiness-head strong {
  display: block;
  color: #111827;
  font-size: 18px;
}

.readiness-head p,
.check p {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 13px;
}

.issue-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 10px;
}

.issue {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  padding: 10px;
  background: #f9fafb;
  border: 1px solid #eef0f3;
  border-radius: 6px;
}

.issue span {
  width: 100%;
}

.readiness-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.text-link,
.text-button {
  color: #2563eb;
  font: inherit;
  text-decoration: none;
  background: transparent;
  border: 0;
  cursor: pointer;
  padding: 0;
}

.text-link:hover,
.text-button:hover {
  color: #1d4ed8;
}

.check-list {
  display: grid;
  gap: 8px;
}

.check {
  display: grid;
  grid-template-columns: 92px 1fr;
  gap: 10px;
  align-items: flex-start;
  padding: 10px;
  background: #f9fafb;
  border: 1px solid #eef0f3;
  border-radius: 6px;
}

.check strong {
  color: #111827;
}

.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 12px;
}

.entry {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 88px;
  padding: 16px;
  color: #1f2937;
  text-decoration: none;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.entry:hover {
  border-color: #409eff;
}

.entry span {
  color: #6b7280;
  font-size: 13px;
  line-height: 1.45;
}

@media (max-width: 760px) {
  header,
  .readiness-head,
  .readiness-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .check {
    grid-template-columns: 1fr;
  }
}
</style>
