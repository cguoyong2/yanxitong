<template>
  <main class="page">
    <header>
      <h1>播报日志</h1>
      <el-button @click="load">刷新</el-button>
    </header>

    <section class="filters">
      <el-input v-model="filters.banquetId" clearable placeholder="宴席 ID" />
      <el-input v-model="filters.giftRecordId" clearable placeholder="礼金 ID" />
      <el-select v-model="filters.deviceType" clearable placeholder="设备类型">
        <el-option label="云喇叭" value="CLOUD_SPEAKER" />
        <el-option label="确认屏" value="CONFIRM_SCREEN" />
      </el-select>
      <el-select v-model="filters.eventType" clearable placeholder="事件类型">
        <el-option label="礼金成功" value="GIFT_PAID" />
      </el-select>
      <el-select v-model="filters.status" clearable placeholder="状态">
        <el-option label="模拟" value="SIMULATED" />
        <el-option label="已推送" value="PUSHED" />
        <el-option label="离线" value="OFFLINE" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button @click="resetFilters">重置</el-button>
    </section>

    <section class="metric-grid">
      <article class="metric">
        <span>日志条数</span>
        <strong>{{ rows.length }}</strong>
      </article>
      <article class="metric">
        <span>云喇叭模拟</span>
        <strong>{{ cloudSpeakerCount }}</strong>
      </article>
      <article class="metric">
        <span>确认屏已推送</span>
        <strong>{{ pushedCount }}</strong>
      </article>
      <article class="metric">
        <span>确认屏离线</span>
        <strong class="warning">{{ offlineCount }}</strong>
      </article>
    </section>

    <el-table v-loading="loading" :data="rows" border stripe empty-text="暂无播报日志">
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="banquetId" label="宴席ID" width="110" />
      <el-table-column prop="giftRecordId" label="礼金ID" width="110" />
      <el-table-column label="设备类型" min-width="150">
        <template #default="{ row }">{{ displayLabel(row.deviceType) }}</template>
      </el-table-column>
      <el-table-column label="事件类型" min-width="140">
        <template #default="{ row }">{{ displayLabel(row.eventType) }}</template>
      </el-table-column>
      <el-table-column prop="content" label="播报内容" min-width="260" />
      <el-table-column label="状态" width="120">
        <template #default="{ row }"><el-tag :type="tagType(row.status)">{{ displayLabel(row.status) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="创建时间" min-width="170">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="goBanquet(row.banquetId)">宴席视图</el-button>
        </template>
      </el-table-column>
    </el-table>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { http, recordsOf, type ApiResponse, type PageResult } from '../../api/client';
import { displayLabel, formatDateTime, tagType } from '../../utils/display';

interface BroadcastLog {
  id: number;
  banquetId: number;
  giftRecordId?: number;
  deviceType: string;
  eventType: string;
  content: string;
  status: string;
  createdAt: string;
}

const loading = ref(false);
const rows = ref<BroadcastLog[]>([]);
const route = useRoute();
const router = useRouter();
const filters = reactive({
  banquetId: String(route.query.banquetId || ''),
  giftRecordId: String(route.query.giftRecordId || ''),
  deviceType: '',
  eventType: '',
  status: ''
});
const cloudSpeakerCount = computed(() => rows.value.filter((row) => row.deviceType === 'CLOUD_SPEAKER').length);
const pushedCount = computed(() => rows.value.filter((row) => row.deviceType === 'CONFIRM_SCREEN' && row.status === 'PUSHED').length);
const offlineCount = computed(() => rows.value.filter((row) => row.deviceType === 'CONFIRM_SCREEN' && row.status === 'OFFLINE').length);

async function load() {
  loading.value = true;
  try {
    const params = new URLSearchParams();
    if (filters.banquetId) {
      params.set('banquetId', filters.banquetId);
    }
    if (filters.giftRecordId) {
      params.set('giftRecordId', filters.giftRecordId);
    }
    if (filters.deviceType) {
      params.set('deviceType', filters.deviceType);
    }
    if (filters.eventType) {
      params.set('eventType', filters.eventType);
    }
    if (filters.status) {
      params.set('status', filters.status);
    }
    params.set('pageSize', '100');
    const suffix = params.toString() ? `?${params.toString()}` : '';
    const response = await http.get<ApiResponse<BroadcastLog[] | PageResult<BroadcastLog>>>(`/admin/broadcast-logs${suffix}`);
    rows.value = recordsOf(response.data.data);
  } finally {
    loading.value = false;
  }
}

function resetFilters() {
  filters.banquetId = String(route.query.banquetId || '');
  filters.giftRecordId = String(route.query.giftRecordId || '');
  filters.deviceType = '';
  filters.eventType = '';
  filters.status = '';
  load();
}

async function goBanquet(banquetId: number) {
  await router.push({ path: '/banquets', query: { banquetId } });
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

.filters {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(136px, max-content));
  gap: 10px;
  margin-bottom: 14px;
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

.warning {
  color: #b45309 !important;
}

@media (max-width: 760px) {
  .filters {
    grid-template-columns: 1fr;
  }

  .filters .el-input,
  .filters .el-select {
    width: 100%;
  }
}
</style>
