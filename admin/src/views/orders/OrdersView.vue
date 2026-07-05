<template>
  <main class="page">
    <header>
      <h1>订单管理</h1>
      <el-button @click="load">刷新</el-button>
    </header>
    <section v-if="filters.banquetId" class="context-panel">
      <div>
        <span>当前钻取宴席</span>
        <strong>{{ contextBanquetTitle }}</strong>
        <p>版本订单和设备订单已按该宴席过滤；当前页签：{{ activeOrderTab === 'devices' ? '设备订单' : '版本订单' }}。</p>
      </div>
      <div class="context-actions">
        <el-button @click="goBanquet(Number(filters.banquetId))">返回宴席工作台</el-button>
        <el-button @click="goOperationLog('banquet', Number(filters.banquetId))">查看日志</el-button>
        <el-button @click="clearContext">清除筛选</el-button>
      </div>
    </section>
    <section class="filters">
      <el-input v-model="filters.banquetId" clearable placeholder="宴席 ID" />
      <el-select v-model="filters.payStatus" clearable placeholder="支付状态">
        <el-option label="未支付" value="UNPAID" />
        <el-option label="已支付" value="PAID" />
      </el-select>
      <el-select v-model="filters.deviceStatus" clearable placeholder="设备状态">
        <el-option label="已创建" value="CREATED" />
        <el-option label="已确认" value="CONFIRMED" />
        <el-option label="配送中" value="DELIVERING" />
        <el-option label="已交付" value="DELIVERED" />
        <el-option label="已取消" value="CANCELLED" />
      </el-select>
      <el-input v-model="filters.keyword" clearable placeholder="订单号" />
      <el-button type="primary" @click="applyFilters">查询</el-button>
      <el-button @click="resetFilters">重置</el-button>
    </section>
    <el-tabs v-model="activeOrderTab">
      <el-tab-pane label="版本订单" name="plans">
        <section class="metric-grid">
          <article class="metric">
            <span>版本订单</span>
            <strong>{{ planSummary.count }}</strong>
          </article>
          <article class="metric">
            <span>已支付</span>
            <strong>{{ planSummary.paid }}</strong>
          </article>
          <article class="metric">
            <span>待支付</span>
            <strong>{{ planSummary.unpaid }}</strong>
          </article>
          <article class="metric">
            <span>金额合计</span>
            <strong>{{ formatMoney(planSummary.amount) }}</strong>
          </article>
        </section>
        <el-table v-loading="loading" :data="displayedPlanOrders" border stripe empty-text="暂无版本订单">
          <el-table-column prop="id" label="ID" width="90" />
          <el-table-column prop="orderNo" label="订单号" min-width="180" />
          <el-table-column prop="banquetId" label="宴席ID" width="100" />
          <el-table-column prop="planId" label="版本ID" width="100" />
          <el-table-column label="金额" width="120">
            <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
          </el-table-column>
          <el-table-column prop="priceUnit" label="单位" width="100" />
          <el-table-column label="支付状态" width="120">
            <template #default="{ row }"><el-tag :type="tagType(row.payStatus)">{{ displayLabel(row.payStatus) }}</el-tag></template>
          </el-table-column>
          <el-table-column label="下一步" min-width="220">
            <template #default="{ row }">{{ planOrderNextStep(row) }}</template>
          </el-table-column>
          <el-table-column label="创建时间" min-width="170">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="210" fixed="right">
            <template #default="{ row }">
              <el-button v-if="features.mockPaymentEnabled && row.payStatus !== 'PAID'" link type="primary" @click="mockPlanPay(row.orderNo as string)">模拟支付</el-button>
              <el-button link type="primary" @click="goBanquet(row.banquetId as number)">宴席视图</el-button>
              <el-button link type="primary" @click="goOperationLog('plan_order', row.id as number)">日志</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="设备订单" name="devices">
        <section class="metric-grid">
          <article class="metric">
            <span>设备订单</span>
            <strong>{{ deviceSummary.count }}</strong>
          </article>
          <article class="metric">
            <span>已支付</span>
            <strong>{{ deviceSummary.paid }}</strong>
          </article>
          <article class="metric">
            <span>配送中/已交付</span>
            <strong>{{ deviceSummary.fulfillment }}</strong>
          </article>
          <article class="metric">
            <span>金额合计</span>
            <strong>{{ formatMoney(deviceSummary.amount) }}</strong>
          </article>
        </section>
        <el-table v-loading="loading" :data="displayedDeviceOrders" border stripe empty-text="暂无设备订单">
          <el-table-column prop="id" label="ID" width="90" />
          <el-table-column prop="orderNo" label="订单号" min-width="180" />
          <el-table-column prop="banquetId" label="宴席ID" width="100" />
          <el-table-column label="设备类型" min-width="140">
            <template #default="{ row }">{{ displayLabel(row.deviceType) }}</template>
          </el-table-column>
          <el-table-column label="租用开始" min-width="160">
            <template #default="{ row }">{{ formatDateTime(row.rentStartAt) }}</template>
          </el-table-column>
          <el-table-column label="租用结束" min-width="160">
            <template #default="{ row }">{{ formatDateTime(row.rentEndAt) }}</template>
          </el-table-column>
          <el-table-column label="价格" width="100">
            <template #default="{ row }">{{ formatMoney(row.price) }}</template>
          </el-table-column>
          <el-table-column prop="priceUnit" label="单位" width="100" />
          <el-table-column prop="deliveryMethod" label="交付方式" min-width="140" />
          <el-table-column label="支付状态" width="120">
            <template #default="{ row }"><el-tag :type="tagType(row.payStatus)">{{ displayLabel(row.payStatus) }}</el-tag></template>
          </el-table-column>
          <el-table-column label="订单状态" width="120">
            <template #default="{ row }"><el-tag :type="tagType(row.orderStatus)">{{ displayLabel(row.orderStatus) }}</el-tag></template>
          </el-table-column>
          <el-table-column label="下一步" min-width="250">
            <template #default="{ row }">{{ deviceOrderNextStep(row) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="360" fixed="right">
            <template #default="{ row }">
              <el-button v-if="features.mockPaymentEnabled && row.payStatus !== 'PAID'" link type="primary" @click="mockDevicePay(row.orderNo as string)">模拟支付</el-button>
              <el-button v-if="row.payStatus === 'PAID' && row.orderStatus === 'CREATED'" link type="primary" @click="updateDeviceStatus(row.orderNo as string, 'CONFIRMED')">确认</el-button>
              <el-button v-if="row.payStatus === 'PAID' && row.orderStatus !== 'DELIVERING' && row.orderStatus !== 'DELIVERED'" link type="primary" @click="updateDeviceStatus(row.orderNo as string, 'DELIVERING')">配送中</el-button>
              <el-button v-if="row.payStatus === 'PAID' && row.orderStatus !== 'DELIVERED'" link type="success" @click="updateDeviceStatus(row.orderNo as string, 'DELIVERED')">已交付</el-button>
              <el-button v-if="row.orderStatus !== 'CANCELLED' && row.orderStatus !== 'DELIVERED'" link type="danger" @click="updateDeviceStatus(row.orderNo as string, 'CANCELLED')">取消</el-button>
              <el-button link type="primary" @click="goBanquet(row.banquetId as number)">宴席视图</el-button>
              <el-button link type="primary" @click="goOperationLog('device_order', row.id as number)">日志</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </main>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus';
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { http, loadRuntimeFeatures, recordsOf, type ApiResponse, type PageResult, type RuntimeFeatures } from '../../api/client';
import { displayLabel, formatDateTime, formatMoney, tagType } from '../../utils/display';

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const planOrders = ref<Record<string, unknown>[]>([]);
const deviceOrders = ref<Record<string, unknown>[]>([]);
const features = ref<RuntimeFeatures>({ mockPaymentEnabled: false });
const activeOrderTab = ref(route.query.tab === 'devices' ? 'devices' : 'plans');
const banquetOptions = ref<{ id: number; name: string }[]>([]);
const filters = ref({
  banquetId: String(route.query.banquetId || ''),
  payStatus: String(route.query.payStatus || ''),
  deviceStatus: String(route.query.deviceStatus || ''),
  keyword: String(route.query.keyword || '')
});
const displayedPlanOrders = computed(() => filterOrders(planOrders.value));
const displayedDeviceOrders = computed(() => filterOrders(deviceOrders.value));
const planSummary = computed(() => summarizeOrders(displayedPlanOrders.value, 'amount'));
const deviceSummary = computed(() => ({
  ...summarizeOrders(displayedDeviceOrders.value, 'price'),
  fulfillment: displayedDeviceOrders.value.filter((item) => ['DELIVERING', 'DELIVERED'].includes(String(item.orderStatus))).length
}));
const contextBanquet = computed(() => banquetOptions.value.find((item) => String(item.id) === String(filters.value.banquetId)));
const contextBanquetTitle = computed(() => contextBanquet.value ? `${contextBanquet.value.id} · ${contextBanquet.value.name}` : `宴席 ID ${filters.value.banquetId}`);

function filterOrders(rows: Record<string, unknown>[]) {
  return rows.filter((item) => {
    if (filters.value.banquetId && Number(item.banquetId) !== Number(filters.value.banquetId)) {
      return false;
    }
    if (filters.value.payStatus && item.payStatus !== filters.value.payStatus) {
      return false;
    }
    if (filters.value.deviceStatus && item.orderStatus !== undefined && item.orderStatus !== filters.value.deviceStatus) {
      return false;
    }
    if (filters.value.keyword && !String(item.orderNo || '').includes(filters.value.keyword)) {
      return false;
    }
    return true;
  });
}

function summarizeOrders(rows: Record<string, unknown>[], amountField: string) {
  return {
    count: rows.length,
    paid: rows.filter((item) => item.payStatus === 'PAID').length,
    unpaid: rows.filter((item) => item.payStatus !== 'PAID').length,
    amount: rows.reduce((total, item) => total + Number(item[amountField] || 0), 0)
  };
}

function applyFilters() {
  filters.value = { ...filters.value };
  syncOrderQuery();
}

function resetFilters() {
  filters.value = {
    banquetId: String(route.query.banquetId || ''),
    payStatus: '',
    deviceStatus: '',
    keyword: ''
  };
  syncOrderQuery();
}

async function load() {
  loading.value = true;
  try {
    const [runtimeFeatures, banquets, plans, devices] = await Promise.all([
      loadRuntimeFeatures(),
      http.get<ApiResponse<{ id: number; name: string }[]>>('/admin/banquets'),
      http.get<ApiResponse<Record<string, unknown>[] | PageResult<Record<string, unknown>>>>('/admin/orders/plans?pageSize=100'),
      http.get<ApiResponse<Record<string, unknown>[] | PageResult<Record<string, unknown>>>>('/admin/orders/devices?pageSize=100')
    ]);
    features.value = runtimeFeatures;
    banquetOptions.value = banquets.data.data || [];
    planOrders.value = recordsOf(plans.data.data);
    deviceOrders.value = recordsOf(devices.data.data);
  } finally {
    loading.value = false;
  }
}

async function mockPlanPay(orderNo: string) {
  await http.post(`/plans/orders/${orderNo}/mock-success`);
  ElMessage.success('版本订单已标记支付成功');
  await load();
}

async function mockDevicePay(orderNo: string) {
  await http.post(`/devices/orders/${orderNo}/mock-success`);
  ElMessage.success('设备订单已确认');
  await load();
}

async function updateDeviceStatus(orderNo: string, orderStatus: string) {
  await http.post(`/admin/orders/devices/${orderNo}/status`, { orderStatus });
  ElMessage.success(`设备订单已更新为${displayLabel(orderStatus)}`);
  await load();
}

function planOrderNextStep(row: Record<string, unknown>) {
  if (row.payStatus === 'PAID') {
    return '权益已生效，可回到宴席视图继续请柬、回执、收礼或设备选择。';
  }
  if (features.value.mockPaymentEnabled) {
    return '待支付，体验环境可模拟支付；正式支付上线后由支付回调激活权益。';
  }
  return '待支付，等待真实支付完成或后台支付异常处理。';
}

function deviceOrderNextStep(row: Record<string, unknown>) {
  if (row.payStatus !== 'PAID') {
    return features.value.mockPaymentEnabled
      ? '待支付，体验环境可模拟支付后进入交付处理。'
      : '待支付，真实支付完成后再安排确认和交付。';
  }
  if (row.orderStatus === 'CREATED') {
    return '已支付，待运营确认租用时间、交付方式和现场联系人。';
  }
  if (row.orderStatus === 'CONFIRMED') {
    return '已确认，下一步安排配送或现场交付。';
  }
  if (row.orderStatus === 'DELIVERING') {
    return '配送中，等待设备送达后标记已交付。';
  }
  if (row.orderStatus === 'DELIVERED') {
    return '已交付，宴席现场可使用确认屏或云喇叭。';
  }
  if (row.orderStatus === 'CANCELLED') {
    return '订单已取消，保留日志用于后续追溯。';
  }
  return '待运营继续处理。';
}

async function goBanquet(banquetId: number) {
  await router.push({ path: '/banquets', query: { banquetId, focus: 'overview' } });
}

async function goOperationLog(targetType: string, targetId: number) {
  await router.push({ path: '/operation-logs', query: { targetType, targetId } });
}

function syncOrderQuery() {
  const nextQuery: Record<string, string> = { tab: activeOrderTab.value };
  if (filters.value.banquetId) {
    nextQuery.banquetId = filters.value.banquetId;
  }
  if (filters.value.payStatus) {
    nextQuery.payStatus = filters.value.payStatus;
  }
  if (filters.value.deviceStatus) {
    nextQuery.deviceStatus = filters.value.deviceStatus;
  }
  if (filters.value.keyword) {
    nextQuery.keyword = filters.value.keyword;
  }
  void router.replace({ path: '/orders', query: nextQuery });
}

function clearContext() {
  filters.value = {
    banquetId: '',
    payStatus: '',
    deviceStatus: '',
    keyword: ''
  };
  syncOrderQuery();
}

onMounted(load);

watch(activeOrderTab, () => {
  syncOrderQuery();
});
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

.context-panel {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 14px;
  padding: 14px;
  border: 1px solid #bbf7d0;
  border-radius: 8px;
  background: #f0fdf4;
}

.context-panel span {
  color: #15803d;
  font-size: 12px;
  font-weight: 700;
}

.context-panel strong {
  display: block;
  margin-top: 4px;
  color: #111827;
  font-size: 18px;
}

.context-panel p {
  margin: 6px 0 0;
  color: #475569;
}

.context-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
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

.muted {
  color: #909399;
}

@media (max-width: 860px) {
  .context-panel,
  .filters {
    grid-template-columns: 1fr;
    display: grid;
  }

  .context-actions {
    justify-content: flex-start;
  }

  .filters .el-input,
  .filters .el-select {
    width: 100%;
  }
}
</style>
