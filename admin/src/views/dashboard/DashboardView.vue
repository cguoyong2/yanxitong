<template>
  <main class="dashboard">
    <section class="hero">
      <div>
        <p class="eyebrow">运营总览</p>
        <h2>情礼记运营后台</h2>
        <p class="hero-copy">集中管理宴席配置、版本权益、业务数据、设备订单和支付异常。</p>
      </div>
      <div class="hero-actions">
        <RouterLink class="primary-action" to="/payments">支付上线检查</RouterLink>
        <el-button :loading="loadingAlerts" @click="loadAlerts">刷新告警</el-button>
      </div>
    </section>

    <section class="status-grid">
      <article v-if="readiness" class="status-card readiness-card" :class="readinessClass">
        <div class="status-head">
          <span>运行安全</span>
          <el-tag :type="readinessTagType" effect="dark">{{ readiness.status }}</el-tag>
        </div>
        <strong>{{ readinessTitle }}</strong>
        <p>{{ readiness.environment }} / {{ readiness.activeProfiles?.length ? readiness.activeProfiles.join(', ') : 'no-profile' }}</p>
        <div class="readiness-actions">
          <button class="text-button" type="button" @click="showDetails = !showDetails">{{ showDetails ? '收起明细' : '查看明细' }}</button>
          <RouterLink class="text-link" to="/payments">支付配置</RouterLink>
        </div>
      </article>

      <article class="metric-card" :class="{ danger: blockerCount > 0 }">
        <span>Readiness 阻塞</span>
        <strong>{{ blockerCount }}</strong>
        <small>{{ blockerCount > 0 ? '上线前必须处理' : '暂无阻塞' }}</small>
      </article>
      <article class="metric-card" :class="{ warning: warningCount > 0 }">
        <span>Readiness 警告</span>
        <strong>{{ warningCount }}</strong>
        <small>{{ warningCount > 0 ? '建议试点前处理' : '暂无警告' }}</small>
      </article>
      <RouterLink class="metric-card link-card" :class="{ danger: failedCallbackTotal > 0 }" :to="{ path: '/payments', query: { processStatus: 'FAILED' } }">
        <span>失败支付回调</span>
        <strong>{{ failedCallbackTotal }}</strong>
        <small>{{ failedCallbackTotal > 0 ? '进入异常筛选' : '暂无异常' }}</small>
      </RouterLink>
    </section>

    <section v-if="showDetails && readiness" class="check-list">
      <article v-for="item in readiness.items" :key="item.code" class="check">
        <el-tag :type="item.passed ? 'success' : item.severity === 'BLOCKER' ? 'danger' : 'warning'" effect="plain">
          {{ item.passed ? '通过' : item.severity }}
        </el-tag>
        <div>
          <strong>{{ item.label }}</strong>
          <p>{{ item.detail }}</p>
        </div>
      </article>
    </section>

    <section class="section-block">
      <div class="section-title">
        <div>
          <span class="eyebrow">Quick Actions</span>
          <h3>高频操作</h3>
        </div>
      </div>
      <div class="quick-grid">
        <RouterLink v-for="item in quickActions" :key="item.path" class="quick-card" :to="item.path">
          <span class="quick-icon">{{ item.icon }}</span>
          <strong>{{ item.title }}</strong>
          <small>{{ item.desc }}</small>
        </RouterLink>
      </div>
    </section>

    <section class="section-block">
      <div class="section-title">
        <div>
          <span class="eyebrow">Configuration</span>
          <h3>配置与资源</h3>
        </div>
      </div>
      <div class="entry-grid">
        <RouterLink v-for="item in configItems" :key="item.path" class="entry" :to="item.path">
          <strong>{{ item.title }}</strong>
          <span>{{ item.desc }}</span>
        </RouterLink>
      </div>
    </section>

    <section class="section-block">
      <div class="section-title">
        <div>
          <span class="eyebrow">Operations</span>
          <h3>业务运营</h3>
        </div>
      </div>
      <div class="entry-grid">
        <RouterLink v-for="item in operationItems" :key="item.path" class="entry" :to="item.path">
          <strong>{{ item.title }}</strong>
          <span>{{ item.desc }}</span>
        </RouterLink>
      </div>
    </section>

    <section class="section-block">
      <div class="section-title">
        <div>
          <span class="eyebrow">Audit</span>
          <h3>支付与审计</h3>
        </div>
      </div>
      <div class="entry-grid compact">
        <RouterLink v-for="item in auditItems" :key="item.path" class="entry" :to="item.path">
          <strong>{{ item.title }}</strong>
          <span>{{ item.desc }}</span>
        </RouterLink>
      </div>
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

const quickActions = [
  { path: '/banquets', title: '宴席管理', desc: '查看宴席、请柬入口和主题状态', icon: '宴' },
  { path: '/plans', title: '版本权益', desc: '维护版本价格、单位和权益', icon: '权' },
  { path: '/payments', title: '支付管理', desc: '检查支付订单和异常回调', icon: '支' },
  { path: '/business', title: '业务数据', desc: 'RSVP、礼金和人情账本', icon: '数' }
];

const configItems = [
  { path: '/config', title: '配置中心', desc: '通用配置项与系统开关' },
  { path: '/event-types', title: '宴席类型', desc: '类型、别名、默认主题与默认文案' },
  { path: '/themes', title: '主题配置', desc: '颜色、图标风格和确认屏模板' },
  { path: '/theme-copywriting', title: '主题文案', desc: '页面文案与云喇叭播报文案' },
  { path: '/templates', title: '模板管理', desc: '模板类型、封面、价格类型和上下架' },
  { path: '/devices', title: '设备配置', desc: '云喇叭和确认屏价格、单位、交付方式' }
];

const operationItems = [
  { path: '/invitations', title: '请柬管理', desc: '请柬实例、模板引用、分享路径和访问数据' },
  { path: '/banquets', title: '宴席管理', desc: '宴席列表、主题编码和公开请柬入口' },
  { path: '/business', title: '业务数据', desc: 'RSVP、礼金记录和人情账本管理' },
  { path: '/orders', title: '订单管理', desc: '版本订单和设备订单基础闭环' }
];

const auditItems = [
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
    return '存在配置警告';
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
  display: grid;
  gap: 20px;
}

.hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  min-height: 190px;
  padding: 28px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(153, 27, 27, 0.92), rgba(17, 24, 39, 0.96)),
    #111827;
  color: #fff;
  overflow: hidden;
  position: relative;
}

.hero::after {
  content: "宴";
  position: absolute;
  right: 34px;
  top: -22px;
  color: rgba(255, 255, 255, 0.07);
  font-size: 210px;
  font-weight: 900;
  line-height: 1;
}

.hero > * {
  position: relative;
  z-index: 1;
}

.eyebrow {
  display: block;
  margin: 0 0 8px;
  color: #94a3b8;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0;
}

.hero .eyebrow {
  color: #fed7aa;
}

.hero h2 {
  margin: 0;
  font-size: 36px;
  font-weight: 900;
}

.hero-copy {
  max-width: 620px;
  margin: 12px 0 0;
  color: rgba(255, 255, 255, 0.78);
  font-size: 15px;
  line-height: 1.7;
}

.hero-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.primary-action {
  display: inline-flex;
  align-items: center;
  height: 32px;
  padding: 0 14px;
  border-radius: 6px;
  background: #fff7ed;
  color: #9a3412;
  font-size: 14px;
  font-weight: 800;
  text-decoration: none;
}

.status-grid {
  display: grid;
  grid-template-columns: minmax(280px, 1.6fr) repeat(3, minmax(160px, 1fr));
  gap: 14px;
}

.status-card,
.metric-card,
.section-block {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.05);
}

.status-card,
.metric-card {
  min-height: 128px;
  padding: 18px;
}

.status-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}

.status-head span,
.metric-card span {
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
}

.status-card strong {
  display: block;
  color: #0f172a;
  font-size: 22px;
}

.status-card p {
  margin: 8px 0 0;
  color: #64748b;
}

.readiness-card.ready {
  border-color: #86efac;
}

.readiness-card.warn {
  border-color: #fde68a;
}

.readiness-card.blocked {
  border-color: #fecaca;
}

.readiness-actions {
  display: flex;
  gap: 14px;
  margin-top: 16px;
}

.text-link,
.text-button {
  padding: 0;
  border: 0;
  background: transparent;
  color: #2563eb;
  cursor: pointer;
  font: inherit;
  font-weight: 800;
  text-decoration: none;
}

.metric-card {
  display: grid;
  gap: 8px;
  color: #111827;
  text-decoration: none;
}

.metric-card strong {
  font-size: 36px;
  line-height: 1;
}

.metric-card small {
  color: #64748b;
}

.metric-card.warning {
  border-color: #fde68a;
}

.metric-card.danger {
  border-color: #fecaca;
}

.link-card:hover {
  border-color: #93c5fd;
}

.check-list {
  display: grid;
  gap: 8px;
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
}

.check {
  display: grid;
  grid-template-columns: 92px 1fr;
  gap: 12px;
  align-items: flex-start;
  padding: 10px;
  border: 1px solid #eef2f7;
  border-radius: 6px;
  background: #f8fafc;
}

.check strong {
  color: #111827;
}

.check p {
  margin: 6px 0 0;
  color: #64748b;
}

.section-block {
  padding: 18px;
}

.section-title {
  display: flex;
  justify-content: space-between;
  margin-bottom: 14px;
}

.section-title h3 {
  margin: 0;
  color: #0f172a;
  font-size: 18px;
}

.quick-grid,
.entry-grid {
  display: grid;
  gap: 12px;
}

.quick-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.entry-grid {
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
}

.entry-grid.compact {
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
}

.quick-card,
.entry {
  color: #172033;
  text-decoration: none;
  border: 1px solid #e2e8f0;
  background: #fff;
  transition: border-color 0.16s ease, transform 0.16s ease, box-shadow 0.16s ease;
}

.quick-card {
  display: grid;
  gap: 10px;
  min-height: 140px;
  padding: 18px;
  border-radius: 8px;
}

.quick-icon {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 8px;
  background: #fff7ed;
  color: #b91c1c;
  font-size: 20px;
  font-weight: 900;
}

.quick-card strong,
.entry strong {
  color: #0f172a;
  font-size: 16px;
}

.quick-card small,
.entry span {
  color: #64748b;
  font-size: 13px;
  line-height: 1.55;
}

.entry {
  display: grid;
  gap: 8px;
  min-height: 96px;
  padding: 16px;
  border-radius: 8px;
}

.quick-card:hover,
.entry:hover {
  transform: translateY(-1px);
  border-color: #fb923c;
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.08);
}

@media (max-width: 1260px) {
  .status-grid,
  .quick-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .hero,
  .hero-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .status-grid,
  .quick-grid {
    grid-template-columns: 1fr;
  }

  .check {
    grid-template-columns: 1fr;
  }
}
</style>
