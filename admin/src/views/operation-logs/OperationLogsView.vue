<template>
  <main class="page">
    <header>
      <div>
        <h1>操作日志</h1>
        <p>配置变更、线下记礼、人情补录、设备绑定、支付异常处理等关键动作</p>
      </div>
      <el-button @click="load">刷新</el-button>
    </header>

    <section v-if="hasContext" class="context-panel">
      <div>
        <span>当前排查对象</span>
        <strong>{{ contextTitle }}</strong>
        <p>操作日志已按该对象过滤，可从这里反查业务、支付、播报和宴席工作台。</p>
      </div>
      <div class="context-actions">
        <el-button v-if="isBanquetContext" @click="goBanquet(Number(filters.targetId))">返回宴席工作台</el-button>
        <el-button v-if="isBanquetContext" @click="goBusiness">业务数据</el-button>
        <el-button v-if="isBanquetContext" @click="goPayments">支付排障</el-button>
        <el-button @click="goBroadcastByContext">播报日志</el-button>
        <el-button @click="clearContext">清除筛选</el-button>
      </div>
    </section>

    <section class="filters">
      <el-select v-model="filters.module" clearable placeholder="模块">
        <el-option v-for="item in modules" :key="item" :label="displayLabel(item)" :value="item" />
      </el-select>
      <el-input v-model="filters.action" clearable placeholder="动作关键词" />
      <el-input v-model="filters.targetType" clearable placeholder="对象类型" />
      <el-input v-model="filters.targetId" clearable placeholder="对象 ID" />
      <el-input v-model="filters.keyword" clearable placeholder="摘要/详情关键词" />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button @click="resetFilters">重置</el-button>
    </section>

    <section class="metric-grid">
      <article class="metric">
        <span>日志条数</span>
        <strong>{{ rows.length }}</strong>
      </article>
      <article class="metric">
        <span>涉及模块</span>
        <strong>{{ moduleCount }}</strong>
      </article>
      <article class="metric">
        <span>系统动作</span>
        <strong>{{ systemCount }}</strong>
      </article>
      <article class="metric">
        <span>管理员动作</span>
        <strong>{{ adminCount }}</strong>
      </article>
    </section>

    <el-table v-loading="loading" :data="rows" border stripe empty-text="暂无操作日志">
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column label="模块" width="120">
        <template #default="{ row }"><el-tag>{{ displayLabel(row.module) }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="action" label="动作" min-width="190" show-overflow-tooltip />
      <el-table-column label="操作者" width="120">
        <template #default="{ row }">{{ displayLabel(row.operatorType) }}</template>
      </el-table-column>
      <el-table-column prop="targetType" label="对象类型" min-width="130" />
      <el-table-column prop="targetId" label="对象ID" width="110" />
      <el-table-column label="摘要" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">{{ humanizeText(row.summary) }}</template>
      </el-table-column>
      <el-table-column label="详情" min-width="260" show-overflow-tooltip>
        <template #default="{ row }">{{ humanizeText(row.detail) }}</template>
      </el-table-column>
      <el-table-column prop="ipAddress" label="IP" min-width="130" />
      <el-table-column label="时间" min-width="170">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="排查" width="190" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.targetType === 'banquet'" link type="primary" @click="goBanquet(row.targetId)">宴席视图</el-button>
          <el-button v-if="row.targetType === 'banquet'" link type="primary" @click="goBusiness(row.targetId)">业务</el-button>
          <el-button v-if="row.module === 'GIFT' || row.module === 'PAYMENT'" link type="primary" @click="goBroadcast(row)">播报</el-button>
        </template>
      </el-table-column>
    </el-table>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { http, recordsOf, type ApiResponse, type PageResult } from '../../api/client';
import { displayLabel, formatDateTime } from '../../utils/display';

interface OperationLog {
  id: number;
  operatorType?: string;
  module: string;
  action: string;
  targetType?: string;
  targetId?: number;
  summary?: string;
  detail?: string;
  ipAddress?: string;
  createdAt?: string;
}

const modules = ['CONFIG', 'PLAN', 'EVENT_TYPE', 'THEME', 'TEMPLATE', 'DEVICE', 'GIFT', 'FAVOR', 'PAYMENT', 'EXPORT', 'AUTH', 'BANQUET', 'INVITATION'];
const rows = ref<OperationLog[]>([]);
const loading = ref(false);
const route = useRoute();
const router = useRouter();
const filters = reactive({
  module: String(route.query.module || ''),
  action: String(route.query.action || ''),
  targetType: String(route.query.targetType || ''),
  targetId: String(route.query.targetId || ''),
  keyword: String(route.query.keyword || '')
});

const moduleCount = computed(() => new Set(rows.value.map((row) => row.module)).size);
const systemCount = computed(() => rows.value.filter((row) => row.operatorType === 'SYSTEM').length);
const adminCount = computed(() => rows.value.filter((row) => row.operatorType === 'ADMIN').length);
const hasContext = computed(() => Boolean(filters.targetType || filters.targetId || filters.module || filters.action));
const isBanquetContext = computed(() => filters.targetType === 'banquet' && Boolean(filters.targetId));
const contextTitle = computed(() => {
  if (filters.targetType && filters.targetId) {
    return `${displayLabel(filters.targetType)} ${filters.targetId}`;
  }
  if (filters.targetType) {
    return displayLabel(filters.targetType);
  }
  if (filters.module) {
    return displayLabel(filters.module);
  }
  if (filters.action) {
    return `动作：${filters.action}`;
  }
  return '全部日志';
});

async function load() {
  loading.value = true;
  try {
    const params = new URLSearchParams();
    Object.entries(filters).forEach(([key, value]) => {
      if (value) {
        params.set(key, value);
      }
    });
    params.set('pageSize', '100');
    const suffix = params.toString() ? `?${params}` : '';
    const response = await http.get<ApiResponse<OperationLog[] | PageResult<OperationLog>>>(`/admin/operation-logs${suffix}`);
    rows.value = recordsOf(response.data.data);
    syncQuery();
  } finally {
    loading.value = false;
  }
}

function resetFilters() {
  filters.module = '';
  filters.action = '';
  filters.targetType = '';
  filters.targetId = '';
  filters.keyword = '';
  void load();
}

function clearContext() {
  filters.module = '';
  filters.action = '';
  filters.targetType = '';
  filters.targetId = '';
  filters.keyword = '';
  void load();
}

function syncQuery() {
  const query: Record<string, string> = {};
  Object.entries(filters).forEach(([key, value]) => {
    if (value) {
      query[key] = value;
    }
  });
  void router.replace({ path: '/operation-logs', query });
}

function humanizeText(value?: string) {
  if (!value) {
    return '-';
  }
  return String(value).replace(/\b[A-Z][A-Z0-9_]*\b/g, (token) => displayLabel(token));
}

async function goBanquet(targetId?: number) {
  if (!targetId) {
    return;
  }
  await router.push({ path: '/banquets', query: { banquetId: targetId, focus: 'overview' } });
}

async function goBusiness(targetId?: number) {
  const banquetId = targetId || Number(filters.targetId);
  if (!banquetId) {
    return;
  }
  await router.push({ path: '/business', query: { banquetId, tab: 'gifts' } });
}

async function goPayments() {
  if (!isBanquetContext.value) {
    return;
  }
  await router.push({ path: '/payments', query: { banquetId: filters.targetId, tab: 'orders' } });
}

async function goBroadcastByContext() {
  if (filters.targetType === 'gift_record' && filters.targetId) {
    await router.push({ path: '/broadcast-logs', query: { giftRecordId: filters.targetId } });
    return;
  }
  if (isBanquetContext.value) {
    await router.push({ path: '/broadcast-logs', query: { banquetId: filters.targetId } });
    return;
  }
  await router.push({ path: '/broadcast-logs' });
}

async function goBroadcast(row: OperationLog) {
  if (row.module === 'GIFT' && row.targetType === 'gift_record' && row.targetId) {
    await router.push({ path: '/broadcast-logs', query: { giftRecordId: row.targetId } });
    return;
  }
  if (row.targetType === 'banquet' && row.targetId) {
    await router.push({ path: '/broadcast-logs', query: { banquetId: row.targetId } });
    return;
  }
  if (row.module === 'PAYMENT') {
    await router.push({ path: '/broadcast-logs' });
    return;
  }
  await router.push({ path: '/broadcast-logs' });
}

onMounted(load);
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 24px;
  background: #f6f7f9;
}

header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

h1 {
  margin: 0;
  font-size: 20px;
}

p {
  margin: 6px 0 0;
  color: #64748b;
}

.filters {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(136px, max-content));
  gap: 10px;
  margin-bottom: 14px;
}

.context-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
  padding: 14px;
  border: 1px solid #ddd6fe;
  border-radius: 8px;
  background: #f5f3ff;
}

.context-panel span {
  display: block;
  color: #6d28d9;
  font-size: 12px;
  font-weight: 700;
}

.context-panel strong {
  display: block;
  margin-top: 4px;
  color: #111827;
  font-size: 16px;
}

.context-panel p {
  margin: 4px 0 0;
  color: #475569;
}

.context-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.filters .el-input,
.filters .el-select {
  width: 160px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.metric {
  display: grid;
  gap: 6px;
  min-height: 74px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.metric span {
  color: #64748b;
  font-size: 12px;
}

.metric strong {
  color: #111827;
  font-size: 20px;
  line-height: 1.2;
}

@media (max-width: 860px) {
  .filters,
  .context-panel,
  .context-actions {
    display: grid;
    grid-template-columns: 1fr;
  }

  .filters .el-input,
  .filters .el-select {
    width: 100%;
  }
}
</style>
