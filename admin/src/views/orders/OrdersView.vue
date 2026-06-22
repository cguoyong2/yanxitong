<template>
  <main class="page">
    <header>
      <h1>订单管理</h1>
      <el-button @click="load">刷新</el-button>
    </header>
    <section class="filters">
      <el-input v-model="filters.banquetId" clearable placeholder="宴席 ID" />
      <el-select v-model="filters.payStatus" clearable placeholder="支付状态">
        <el-option label="未支付" value="UNPAID" />
        <el-option label="已支付" value="PAID" />
      </el-select>
      <el-input v-model="filters.keyword" clearable placeholder="订单号" />
      <el-button type="primary" @click="applyFilters">查询</el-button>
      <el-button @click="resetFilters">重置</el-button>
    </section>
    <el-tabs>
      <el-tab-pane label="版本订单">
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
      <el-tab-pane label="设备订单">
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
          <el-table-column label="操作" width="310" fixed="right">
            <template #default="{ row }">
              <el-button v-if="features.mockPaymentEnabled && row.payStatus !== 'PAID'" link type="primary" @click="mockDevicePay(row.orderNo as string)">模拟支付</el-button>
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
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { http, loadRuntimeFeatures, recordsOf, type ApiResponse, type PageResult, type RuntimeFeatures } from '../../api/client';
import { displayLabel, formatDateTime, formatMoney, tagType } from '../../utils/display';

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const planOrders = ref<Record<string, unknown>[]>([]);
const deviceOrders = ref<Record<string, unknown>[]>([]);
const features = ref<RuntimeFeatures>({ mockPaymentEnabled: false });
const filters = ref({
  banquetId: String(route.query.banquetId || ''),
  payStatus: '',
  keyword: ''
});
const displayedPlanOrders = computed(() => filterOrders(planOrders.value));
const displayedDeviceOrders = computed(() => filterOrders(deviceOrders.value));
const planSummary = computed(() => summarizeOrders(displayedPlanOrders.value, 'amount'));
const deviceSummary = computed(() => ({
  ...summarizeOrders(displayedDeviceOrders.value, 'price'),
  fulfillment: displayedDeviceOrders.value.filter((item) => ['DELIVERING', 'DELIVERED'].includes(String(item.orderStatus))).length
}));

function filterOrders(rows: Record<string, unknown>[]) {
  return rows.filter((item) => {
    if (filters.value.banquetId && Number(item.banquetId) !== Number(filters.value.banquetId)) {
      return false;
    }
    if (filters.value.payStatus && item.payStatus !== filters.value.payStatus) {
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
}

function resetFilters() {
  filters.value = {
    banquetId: String(route.query.banquetId || ''),
    payStatus: '',
    keyword: ''
  };
}

async function load() {
  loading.value = true;
  try {
    const [runtimeFeatures, plans, devices] = await Promise.all([
      loadRuntimeFeatures(),
      http.get<ApiResponse<Record<string, unknown>[] | PageResult<Record<string, unknown>>>>('/admin/orders/plans?pageSize=100'),
      http.get<ApiResponse<Record<string, unknown>[] | PageResult<Record<string, unknown>>>>('/admin/orders/devices?pageSize=100')
    ]);
    features.value = runtimeFeatures;
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
  ElMessage.success('设备订单状态已更新');
  await load();
}

async function goBanquet(banquetId: number) {
  await router.push({ path: '/banquets', query: { banquetId } });
}

async function goOperationLog(targetType: string, targetId: number) {
  await router.push({ path: '/operation-logs', query: { targetType, targetId } });
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

.muted {
  color: #909399;
}

@media (max-width: 860px) {
  .filters {
    grid-template-columns: 1fr;
  }

  .filters .el-input,
  .filters .el-select {
    width: 100%;
  }
}
</style>
