<script setup lang="ts">
import { ElMessage } from 'element-plus';
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { http, recordsOf, type ApiResponse, type PageResult } from '../../api/client';
import { displayLabel, formatDateTime, formatMoney, tagType } from '../../utils/display';

interface Banquet {
  id: number;
  banquetNo: string;
  name: string;
  eventTypeCode: string;
  themeCode: string;
  banquetTime?: string;
  location?: string;
  status: string;
}

interface EventTypeOption {
  eventTypeCode: string;
  name: string;
  defaultThemeCode: string;
  defaultThemeName: string;
  primaryColor?: string;
  defaultCopywriting?: string;
}

interface InvitationTemplate {
  id: number;
  name: string;
  priceType?: string;
}

interface BanquetDetail {
  banquet: Banquet;
  invitation?: {
    id: number;
    title: string;
    shareSlug: string;
    basicFields?: string;
  };
  theme?: {
    name: string;
    primaryColor: string;
  };
  giftSuccessCopywriting: {
    content: string;
    speakerText: string;
    source: string;
  };
}

interface RsvpStats {
  totalRecords: number;
  attendingRecords: number;
  pendingRecords: number;
  declinedRecords: number;
  totalGuests: number;
  mealRequiredGuests: number;
  accommodationRequiredGuests: number;
}

interface BanquetAggregate {
  rsvpRows: Record<string, unknown>[];
  rsvpStats?: RsvpStats;
  gifts: Record<string, unknown>[];
  favorContacts: Record<string, unknown>[];
  entitlements?: Record<string, unknown>;
  planOrders: Record<string, unknown>[];
  deviceOrders: Record<string, unknown>[];
  broadcastLogs: Record<string, unknown>[];
  paymentOrders: Record<string, unknown>[];
  paymentCallbacks: Record<string, unknown>[];
  operationLogs: Record<string, unknown>[];
}

type DetailTab = 'overview' | 'rsvp' | 'gifts' | 'favor' | 'devices' | 'payments' | 'broadcast' | 'logs';

const rows = ref<Banquet[]>([]);
const route = useRoute();
const router = useRouter();
const eventTypes = ref<EventTypeOption[]>([]);
const templates = ref<InvitationTemplate[]>([]);
const loading = ref(false);
const creating = ref(false);
const detailLoading = ref(false);
const createVisible = ref(false);
const detailVisible = ref(false);
const detailActiveTab = ref<DetailTab>(detailTabFromQuery(route.query.focus));
const detail = ref<BanquetDetail>();
const aggregate = ref<BanquetAggregate>({
  rsvpRows: [],
  gifts: [],
  favorContacts: [],
  planOrders: [],
  deviceOrders: [],
  broadcastLogs: [],
  paymentOrders: [],
  paymentCallbacks: [],
  operationLogs: []
});
const form = ref({
  name: '',
  eventTypeCode: '',
  templateId: undefined as number | undefined,
  banquetTime: '',
  location: '',
  customGiftSuccess: ''
});
const giftTotal = computed(() => sumAmount(aggregate.value.gifts, 'amount'));
const favorReceivedTotal = computed(() => sumAmount(aggregate.value.favorContacts, 'receivedAmount'));
const favorGivenTotal = computed(() => sumAmount(aggregate.value.favorContacts, 'givenAmount'));
const paidPaymentOrders = computed(() => aggregate.value.paymentOrders.filter((item) => item.payStatus === 'PAID').length);
const failedCallbacks = computed(() => aggregate.value.paymentCallbacks.filter((item) => item.processStatus === 'FAILED' || item.verifyStatus === 'FAILED').length);
const offlineConfirmLogs = computed(() => aggregate.value.broadcastLogs.filter((item) => item.deviceType === 'CONFIRM_SCREEN' && item.status === 'OFFLINE').length);
const paidPlanOrders = computed(() => aggregate.value.planOrders.filter((item) => item.payStatus === 'PAID').length);
const paidDeviceOrders = computed(() => aggregate.value.deviceOrders.filter((item) => item.payStatus === 'PAID').length);
const workbenchChecks = computed(() => [
  {
    key: 'invitation',
    title: '基础请柬',
    status: detail.value?.invitation?.shareSlug ? 'READY' : 'TODO',
    value: detail.value?.invitation?.shareSlug || '未生成分享码',
    action: '复制公开页',
    handler: copyInvite
  },
  {
    key: 'rsvp',
    title: '回执闭环',
    status: (aggregate.value.rsvpStats?.totalRecords || 0) > 0 ? 'READY' : 'TODO',
    value: `${aggregate.value.rsvpStats?.totalRecords || 0} 条 / ${aggregate.value.rsvpStats?.totalGuests || 0} 人`,
    action: '处理回执',
    handler: () => openBusinessTab('rsvp')
  },
  {
    key: 'gifts',
    title: '收礼闭环',
    status: aggregate.value.gifts.length > 0 ? 'READY' : 'TODO',
    value: `${aggregate.value.gifts.length} 笔 / ${formatMoney(giftTotal.value)}`,
    action: '处理收礼',
    handler: () => openBusinessTab('gifts')
  },
  {
    key: 'favor',
    title: '人情账本',
    status: aggregate.value.favorContacts.length > 0 ? 'READY' : 'TODO',
    value: `${aggregate.value.favorContacts.length} 人 / ${formatMoney(favorReceivedTotal.value - favorGivenTotal.value)}`,
    action: '处理人情',
    handler: () => openBusinessTab('favor')
  },
  {
    key: 'payment',
    title: '支付排障',
    status: failedCallbacks.value > 0 ? 'RISK' : 'READY',
    value: `${paidPaymentOrders.value}/${aggregate.value.paymentOrders.length} 已支付，${failedCallbacks.value} 个异常`,
    action: '排查支付',
    handler: () => goRelated('/payments')
  },
  {
    key: 'device',
    title: '设备状态',
    status: offlineConfirmLogs.value > 0 ? 'RISK' : 'READY',
    value: `${paidDeviceOrders.value}/${aggregate.value.deviceOrders.length} 已支付，离线 ${offlineConfirmLogs.value}`,
    action: '查看播报',
    handler: () => goRelated('/broadcast-logs')
  },
  {
    key: 'logs',
    title: '操作留痕',
    status: aggregate.value.operationLogs.length > 0 ? 'READY' : 'TODO',
    value: `${aggregate.value.operationLogs.length} 条关键操作`,
    action: '查看日志',
    handler: () => goOperationLog('banquet', detail.value?.banquet.id)
  }
]);
const riskChecks = computed(() => workbenchChecks.value.filter((item) => item.status !== 'READY'));
const workbenchStatus = computed(() => {
  if (riskChecks.value.some((item) => item.status === 'RISK')) {
    return { label: '需要排障', type: 'danger' as const };
  }
  if (riskChecks.value.length > 0) {
    return { label: '待补闭环', type: 'warning' as const };
  }
  return { label: '闭环完整', type: 'success' as const };
});

function detailTabFromQuery(value: unknown): DetailTab {
  const allowed: DetailTab[] = ['overview', 'rsvp', 'gifts', 'favor', 'devices', 'payments', 'broadcast', 'logs'];
  return allowed.includes(value as DetailTab) ? value as DetailTab : 'overview';
}

async function load() {
  loading.value = true;
  try {
    const response = await http.get<ApiResponse<Banquet[]>>('/admin/banquets');
    rows.value = response.data.data || [];
  } finally {
    loading.value = false;
  }
}

async function loadEventTypes() {
  const response = await http.get<ApiResponse<EventTypeOption[]>>('/meta/event-types');
  eventTypes.value = response.data.data || [];
  if (!form.value.eventTypeCode && eventTypes.value.length > 0) {
    form.value.eventTypeCode = eventTypes.value[0].eventTypeCode;
  }
}

async function loadTemplates() {
  const response = await http.get<ApiResponse<InvitationTemplate[]>>('/meta/invitation-templates');
  templates.value = response.data.data || [];
  if (!form.value.templateId && templates.value.length > 0) {
    form.value.templateId = templates.value[0].id;
  }
}

function openCreate() {
  form.value = {
    name: '',
    eventTypeCode: eventTypes.value[0]?.eventTypeCode || '',
    templateId: templates.value[0]?.id,
    banquetTime: '',
    location: '',
    customGiftSuccess: ''
  };
  createVisible.value = true;
}

async function createBanquet() {
  if (!form.value.name || !form.value.eventTypeCode) {
    ElMessage.warning('请填写宴席名称和类型');
    return;
  }
  creating.value = true;
  try {
    await http.post('/banquets', {
      name: form.value.name,
      eventTypeCode: form.value.eventTypeCode,
      banquetTime: form.value.banquetTime || undefined,
      location: form.value.location,
      customCopywriting: form.value.customGiftSuccess
        ? JSON.stringify({ gift_success: form.value.customGiftSuccess, gift_success_speaker_text: form.value.customGiftSuccess })
        : undefined,
      templateId: form.value.templateId
    });
    ElMessage.success('宴席已创建');
    createVisible.value = false;
    await load();
  } finally {
    creating.value = false;
  }
}

async function openDetail(row: Banquet, focus: DetailTab = detailTabFromQuery(route.query.focus)) {
  detailVisible.value = true;
  detailActiveTab.value = focus;
  detailLoading.value = true;
  try {
    const [
      detailResponse,
      rsvpResponse,
      rsvpStatsResponse,
      giftResponse,
      favorResponse,
      entitlementResponse,
      planOrderResponse,
      deviceOrderResponse,
      broadcastResponse,
      paymentOrderResponse,
      paymentCallbackResponse,
      operationLogResponse
    ] = await Promise.all([
      http.get<ApiResponse<BanquetDetail>>(`/admin/banquets/${row.id}`),
      http.get<ApiResponse<Record<string, unknown>[] | PageResult<Record<string, unknown>>>>(`/admin/rsvp?banquetId=${row.id}&pageSize=100`),
      http.get<ApiResponse<RsvpStats>>(`/admin/rsvp/stats?banquetId=${row.id}`),
      http.get<ApiResponse<Record<string, unknown>[] | PageResult<Record<string, unknown>>>>(`/admin/gifts?banquetId=${row.id}&pageSize=100`),
      http.get<ApiResponse<Record<string, unknown>[]>>(`/admin/favor/contacts?banquetId=${row.id}`),
      http.get<ApiResponse<Record<string, unknown>>>(`/plans/banquets/${row.id}/entitlements`),
      http.get<ApiResponse<Record<string, unknown>[] | PageResult<Record<string, unknown>>>>('/admin/orders/plans?pageSize=100'),
      http.get<ApiResponse<Record<string, unknown>[]>>(`/devices/orders?banquetId=${row.id}`),
      http.get<ApiResponse<Record<string, unknown>[] | PageResult<Record<string, unknown>>>>(`/admin/broadcast-logs?banquetId=${row.id}&pageSize=100`),
      http.get<ApiResponse<Record<string, unknown>[] | PageResult<Record<string, unknown>>>>('/admin/payments/orders?pageSize=100'),
      http.get<ApiResponse<Record<string, unknown>[] | PageResult<Record<string, unknown>>>>('/admin/payments/callbacks?pageSize=100'),
      http.get<ApiResponse<Record<string, unknown>[] | PageResult<Record<string, unknown>>>>(`/admin/operation-logs?targetType=banquet&targetId=${row.id}&pageSize=100`)
    ]);
    detail.value = detailResponse.data.data;
    const paymentOrders = recordsOf(paymentOrderResponse.data.data).filter((item) => Number(item.banquetId) === row.id);
    const paymentOrderNos = new Set(paymentOrders.map((item) => String(item.orderNo)));
    aggregate.value = {
      rsvpRows: recordsOf(rsvpResponse.data.data),
      rsvpStats: rsvpStatsResponse.data.data,
      gifts: recordsOf(giftResponse.data.data),
      favorContacts: favorResponse.data.data || [],
      entitlements: entitlementResponse.data.data,
      planOrders: recordsOf(planOrderResponse.data.data).filter((item) => Number(item.banquetId) === row.id),
      deviceOrders: deviceOrderResponse.data.data || [],
      broadcastLogs: recordsOf(broadcastResponse.data.data),
      paymentOrders,
      paymentCallbacks: recordsOf(paymentCallbackResponse.data.data).filter((item) => paymentOrderNos.has(String(item.orderNo))),
      operationLogs: recordsOf(operationLogResponse.data.data)
    };
  } finally {
    detailLoading.value = false;
  }
}

function inviteUrl() {
  const slug = detail.value?.invitation?.shareSlug;
  return slug ? `/pages/invite/public/index?slug=${slug}` : '';
}

function copyInvite() {
  const url = inviteUrl();
  if (!url) {
    return;
  }
  navigator.clipboard?.writeText(url);
  ElMessage.success('公开页路径已复制');
}

function copyBanquetId() {
  const id = detail.value?.banquet.id;
  if (!id) {
    return;
  }
  navigator.clipboard?.writeText(String(id));
  ElMessage.success('宴席 ID 已复制');
}

async function goRelated(path: string) {
  const id = detail.value?.banquet.id;
  if (!id) {
    return;
  }
  await router.push({ path, query: { banquetId: id } });
}

async function openBusinessTab(tab: 'gifts' | 'rsvp' | 'favor') {
  const id = detail.value?.banquet.id;
  if (!id) {
    return;
  }
  await router.push({ path: '/business', query: { banquetId: id, tab } });
}

async function refreshDetail() {
  const banquet = detail.value?.banquet;
  if (!banquet) {
    return;
  }
  await openDetail(banquet, detailActiveTab.value);
}

function moneyTotal(rows: Record<string, unknown>[]) {
  return formatMoney(sumAmount(rows, 'amount'));
}

function sumAmount(rows: Record<string, unknown>[], field: string) {
  return rows.reduce((total, item) => total + Number(item[field] || 0), 0);
}

function goPaymentOrder(orderNo: unknown) {
  if (!orderNo) {
    return;
  }
  router.push({ path: '/payments', query: { orderNo: String(orderNo) } });
}

function goOperationLog(targetType?: string, targetId?: unknown) {
  const query: Record<string, string> = {};
  if (targetType) {
    query.targetType = targetType;
  }
  if (targetId) {
    query.targetId = String(targetId);
  }
  router.push({ path: '/operation-logs', query });
}

onMounted(async () => {
  await Promise.all([load(), loadEventTypes(), loadTemplates()]);
  const queryBanquetId = Number(route.query.banquetId);
  if (Number.isInteger(queryBanquetId)) {
    const target = rows.value.find((item) => item.id === queryBanquetId);
    if (target) {
      await openDetail(target, detailTabFromQuery(route.query.focus));
    }
  }
});
</script>

<template>
  <section class="page">
    <header>
      <h1>宴席管理</h1>
      <div class="actions">
        <el-button @click="load">刷新</el-button>
        <el-button type="primary" @click="openCreate">创建宴席</el-button>
      </div>
    </header>
    <el-table v-loading="loading" :data="rows" border stripe empty-text="暂无宴席">
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="banquetNo" label="宴席编号" min-width="180" />
      <el-table-column prop="name" label="宴席名称" min-width="180" />
      <el-table-column prop="eventTypeCode" label="类型" min-width="130" />
      <el-table-column prop="themeCode" label="主题" min-width="160" />
      <el-table-column label="时间" min-width="170">
        <template #default="{ row }">{{ formatDateTime(row.banquetTime) }}</template>
      </el-table-column>
      <el-table-column prop="location" label="地点" min-width="180" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="tagType(row.status)">{{ displayLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row, 'overview')">工作台</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="createVisible" title="创建宴席" width="640px">
      <el-form label-width="120px">
        <el-form-item label="宴席名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="宴席类型" required>
          <el-select v-model="form.eventTypeCode">
            <el-option v-for="item in eventTypes" :key="item.eventTypeCode" :label="`${item.name} / ${item.defaultThemeName}`" :value="item.eventTypeCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="请柬模板">
          <el-select v-model="form.templateId" placeholder="请选择请柬模板">
            <el-option v-for="item in templates" :key="item.id" :label="`${item.name}${item.priceType ? ` / ${item.priceType}` : ''}`" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="宴席时间">
          <el-input v-model="form.banquetTime" placeholder="2026-10-01T18:00:00" />
        </el-form-item>
        <el-form-item label="宴席地点">
          <el-input v-model="form.location" />
        </el-form-item>
        <el-form-item label="收礼文案">
          <el-input v-model="form.customGiftSuccess" type="textarea" :rows="3" placeholder="宴席自定义收礼成功文案，可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="createBanquet">创建</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="宴席运营视图" size="82%">
      <template v-if="detail">
        <div v-loading="detailLoading">
          <section class="hero">
            <div>
              <h2>{{ detail.banquet.name }}</h2>
              <p>{{ detail.banquet.banquetNo }} · {{ detail.banquet.eventTypeCode }} · {{ detail.theme?.name || detail.banquet.themeCode }}</p>
              <p>{{ formatDateTime(detail.banquet.banquetTime) }} · {{ detail.banquet.location || '-' }}</p>
              <div class="workbench-status">
                <el-tag :type="workbenchStatus.type" effect="dark">{{ workbenchStatus.label }}</el-tag>
                <span>{{ riskChecks.length ? `${riskChecks.length} 项需要处理` : '关键流程已可追踪' }}</span>
              </div>
              <div class="quick-actions">
                <el-button size="small" @click="copyBanquetId">复制宴席 ID</el-button>
                <el-button size="small" @click="refreshDetail">刷新工作台</el-button>
                <el-button size="small" @click="goRelated('/business')">业务数据</el-button>
                <el-button size="small" @click="goRelated('/orders')">订单</el-button>
                <el-button size="small" @click="goRelated('/payments')">支付</el-button>
                <el-button size="small" @click="goRelated('/broadcast-logs')">播报</el-button>
                <el-button size="small" @click="goOperationLog('banquet', detail.banquet.id)">日志</el-button>
              </div>
            </div>
            <div class="metrics">
              <span>回执 {{ aggregate.rsvpStats?.totalRecords ?? 0 }}</span>
              <span>赴宴 {{ aggregate.rsvpStats?.totalGuests ?? 0 }}</span>
              <span>礼金 {{ formatMoney(giftTotal) }}</span>
              <span>人情 {{ formatMoney(favorReceivedTotal - favorGivenTotal) }}</span>
              <span>支付 {{ paidPaymentOrders }}/{{ aggregate.paymentOrders.length }}</span>
              <span>设备 {{ paidDeviceOrders }}/{{ aggregate.deviceOrders.length }}</span>
              <span :class="{ danger: failedCallbacks > 0 }">异常回调 {{ failedCallbacks }}</span>
              <span :class="{ warning: offlineConfirmLogs > 0 }">确认屏离线 {{ offlineConfirmLogs }}</span>
            </div>
          </section>

          <el-tabs v-model="detailActiveTab">
            <el-tab-pane label="概览" name="overview">
              <section class="workbench-grid">
                <article v-for="item in workbenchChecks" :key="item.key" class="workbench-card" :class="{ risk: item.status === 'RISK', todo: item.status === 'TODO' }">
                  <div>
                    <span>{{ item.title }}</span>
                    <strong>{{ item.value }}</strong>
                  </div>
                  <el-tag :type="item.status === 'READY' ? 'success' : item.status === 'RISK' ? 'danger' : 'warning'" effect="plain">
                    {{ item.status === 'READY' ? '通过' : item.status === 'RISK' ? '风险' : '待补' }}
                  </el-tag>
                  <el-button size="small" @click="item.handler">{{ item.action }}</el-button>
                </article>
              </section>
              <section v-if="riskChecks.length" class="advice-panel">
                <h3>处理建议</h3>
                <p v-for="item in riskChecks" :key="`advice-${item.key}`">
                  {{ item.title }}：{{ item.value }}。建议点击“{{ item.action }}”进入对应处理页面。
                </p>
              </section>
              <section class="overview-grid">
                <div class="panel">
                  <h3>基础请柬</h3>
                  <p>标题：{{ detail.invitation?.title || '-' }}</p>
                  <p>分享码：{{ detail.invitation?.shareSlug || '-' }}</p>
                  <p>公开页：{{ inviteUrl() || '-' }}</p>
                  <el-button size="small" @click="copyInvite">复制公开页路径</el-button>
                </div>
                <div class="panel">
                  <h3>版本权益</h3>
                  <p>当前版本：{{ (aggregate.entitlements?.currentPlan as Record<string, unknown> | undefined)?.name || '基础版' }}</p>
                  <p>付费状态：{{ aggregate.entitlements?.paidPlanActive ? '已购买' : '默认权益' }}</p>
                  <p>设备权益：{{ displayLabel((aggregate.entitlements?.rightValues as Record<string, string> | undefined)?.DEVICE_RENTAL) }}</p>
                </div>
                <div class="panel">
                  <h3>收礼文案</h3>
                  <p>{{ detail.giftSuccessCopywriting.content }}</p>
                  <p class="muted">来源：{{ detail.giftSuccessCopywriting.source }}</p>
                  <p class="muted">播报：{{ detail.giftSuccessCopywriting.speakerText }}</p>
                </div>
                <div class="panel">
                  <h3>订单与支付</h3>
                  <p>版本订单：{{ paidPlanOrders }}/{{ aggregate.planOrders.length }} 已支付</p>
                  <p>设备订单：{{ paidDeviceOrders }}/{{ aggregate.deviceOrders.length }} 已支付</p>
                  <p>礼金支付：{{ paidPaymentOrders }}/{{ aggregate.paymentOrders.length }} 已支付</p>
                  <p :class="{ danger: failedCallbacks > 0 }">回调异常：{{ failedCallbacks }}</p>
                </div>
                <div class="panel">
                  <h3>人情账本</h3>
                  <p>联系人：{{ aggregate.favorContacts.length }}</p>
                  <p>收礼：{{ formatMoney(favorReceivedTotal) }}</p>
                  <p>回礼：{{ formatMoney(favorGivenTotal) }}</p>
                  <p>差额：{{ formatMoney(favorReceivedTotal - favorGivenTotal) }}</p>
                </div>
                <div class="panel">
                  <h3>排障线索</h3>
                  <p>播报日志：{{ aggregate.broadcastLogs.length }}</p>
                  <p :class="{ warning: offlineConfirmLogs > 0 }">确认屏离线：{{ offlineConfirmLogs }}</p>
                  <p>宴席操作日志：{{ aggregate.operationLogs.length }}</p>
                  <el-button size="small" @click="goOperationLog('banquet', detail.banquet.id)">查看日志</el-button>
                </div>
              </section>
            </el-tab-pane>

            <el-tab-pane label="RSVP" name="rsvp">
              <section class="summary">
                <span>总记录 {{ aggregate.rsvpStats?.totalRecords ?? 0 }}</span>
                <span>出席 {{ aggregate.rsvpStats?.attendingRecords ?? 0 }}</span>
                <span>待定 {{ aggregate.rsvpStats?.pendingRecords ?? 0 }}</span>
                <span>婉拒 {{ aggregate.rsvpStats?.declinedRecords ?? 0 }}</span>
                <span>用餐 {{ aggregate.rsvpStats?.mealRequiredGuests ?? 0 }}</span>
                <span>住宿 {{ aggregate.rsvpStats?.accommodationRequiredGuests ?? 0 }}</span>
              </section>
              <el-table :data="aggregate.rsvpRows" border stripe empty-text="暂无 RSVP">
                <el-table-column prop="guestName" label="姓名" min-width="130" />
                <el-table-column prop="phone" label="手机号" min-width="130" />
                <el-table-column label="状态" width="110">
                  <template #default="{ row }"><el-tag :type="tagType(row.attendanceStatus)">{{ displayLabel(row.attendanceStatus) }}</el-tag></template>
                </el-table-column>
                <el-table-column prop="guestCount" label="人数" width="90" />
                <el-table-column prop="mealRequired" label="用餐" width="90" />
                <el-table-column prop="accommodationRequired" label="住宿" width="90" />
                <el-table-column prop="message" label="留言" min-width="220" show-overflow-tooltip />
                <el-table-column label="提交时间" min-width="170">
                  <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane label="礼金" name="gifts">
              <section class="summary">
                <span>记录 {{ aggregate.gifts.length }}</span>
                <span>合计 {{ moneyTotal(aggregate.gifts) }}</span>
              </section>
              <el-table :data="aggregate.gifts" border stripe empty-text="暂无礼金记录">
                <el-table-column prop="id" label="ID" width="80" />
                <el-table-column label="来源" width="130">
                  <template #default="{ row }"><el-tag :type="tagType(row.giftSource)">{{ displayLabel(row.giftSource) }}</el-tag></template>
                </el-table-column>
                <el-table-column prop="guestName" label="来宾" min-width="140" />
                <el-table-column label="金额" width="110">
                  <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
                </el-table-column>
                <el-table-column prop="blessing" label="祝福/备注" min-width="220" show-overflow-tooltip />
                <el-table-column prop="paymentOrderId" label="支付订单ID" width="120" />
                <el-table-column label="排查" width="90">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="goOperationLog('gift_record', row.id)">日志</el-button>
                  </template>
                </el-table-column>
                <el-table-column label="收礼时间" min-width="170">
                  <template #default="{ row }">{{ formatDateTime(row.receivedAt) }}</template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane label="人情" name="favor">
              <section class="summary">
                <span>联系人 {{ aggregate.favorContacts.length }}</span>
                <span>收礼 {{ formatMoney(favorReceivedTotal) }}</span>
                <span>回礼 {{ formatMoney(favorGivenTotal) }}</span>
                <span>差额 {{ formatMoney(favorReceivedTotal - favorGivenTotal) }}</span>
              </section>
              <el-table :data="aggregate.favorContacts" border stripe empty-text="暂无人情账本">
                <el-table-column prop="contactId" label="联系人ID" width="110" />
                <el-table-column prop="contactName" label="联系人" min-width="150" />
                <el-table-column label="收礼合计" width="130">
                  <template #default="{ row }">{{ formatMoney(row.receivedAmount) }}</template>
                </el-table-column>
                <el-table-column label="回礼合计" width="130">
                  <template #default="{ row }">{{ formatMoney(row.givenAmount) }}</template>
                </el-table-column>
                <el-table-column label="差额" width="130">
                  <template #default="{ row }">{{ formatMoney(row.balance) }}</template>
                </el-table-column>
                <el-table-column label="操作" width="110">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="goRelated('/business')">业务数据</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane label="设备订单" name="devices">
              <el-table :data="aggregate.deviceOrders" border stripe empty-text="暂无设备订单">
                <el-table-column prop="orderNo" label="订单号" min-width="170" />
                <el-table-column label="设备类型" min-width="130">
                  <template #default="{ row }">{{ displayLabel(row.deviceType) }}</template>
                </el-table-column>
                <el-table-column label="开始" min-width="160">
                  <template #default="{ row }">{{ formatDateTime(row.rentStartAt) }}</template>
                </el-table-column>
                <el-table-column label="结束" min-width="160">
                  <template #default="{ row }">{{ formatDateTime(row.rentEndAt) }}</template>
                </el-table-column>
                <el-table-column label="价格" width="100">
                  <template #default="{ row }">{{ formatMoney(row.price) }}</template>
                </el-table-column>
                <el-table-column prop="deliveryMethod" label="交付方式" min-width="130" />
                <el-table-column label="支付" width="100">
                  <template #default="{ row }"><el-tag :type="tagType(row.payStatus)">{{ displayLabel(row.payStatus) }}</el-tag></template>
                </el-table-column>
                <el-table-column label="订单" width="100">
                  <template #default="{ row }"><el-tag :type="tagType(row.orderStatus)">{{ displayLabel(row.orderStatus) }}</el-tag></template>
                </el-table-column>
                <el-table-column label="排查" width="90">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="goOperationLog('device_order', row.id)">日志</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane label="支付" name="payments">
              <section class="summary">
                <span>支付订单 {{ aggregate.paymentOrders.length }}</span>
                <span>已支付 {{ paidPaymentOrders }}</span>
                <span :class="{ danger: failedCallbacks > 0 }">异常回调 {{ failedCallbacks }}</span>
              </section>
              <el-table :data="aggregate.paymentOrders" border stripe empty-text="暂无支付订单">
                <el-table-column prop="orderNo" label="支付订单号" min-width="180" />
                <el-table-column prop="provider" label="Provider" min-width="160" />
                <el-table-column label="场景" width="130">
                  <template #default="{ row }">{{ displayLabel(row.scene) }}</template>
                </el-table-column>
                <el-table-column label="入口" width="130">
                  <template #default="{ row }">{{ displayLabel(row.entrySource) }}</template>
                </el-table-column>
                <el-table-column label="金额" width="100">
                  <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
                </el-table-column>
                <el-table-column label="状态" width="100">
                  <template #default="{ row }"><el-tag :type="tagType(row.payStatus)">{{ displayLabel(row.payStatus) }}</el-tag></template>
                </el-table-column>
                <el-table-column prop="providerTradeNo" label="机构交易号" min-width="170" />
                <el-table-column label="排查" width="110">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="goPaymentOrder(row.orderNo)">支付页</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <h3 class="sub-title">相关回调</h3>
              <el-table :data="aggregate.paymentCallbacks" border stripe empty-text="暂无支付回调">
                <el-table-column prop="id" label="ID" width="80" />
                <el-table-column prop="orderNo" label="订单号" min-width="180" />
                <el-table-column label="验签" width="100">
                  <template #default="{ row }"><el-tag :type="tagType(row.verifyStatus)">{{ displayLabel(row.verifyStatus) }}</el-tag></template>
                </el-table-column>
                <el-table-column label="处理" width="100">
                  <template #default="{ row }"><el-tag :type="tagType(row.processStatus)">{{ displayLabel(row.processStatus) }}</el-tag></template>
                </el-table-column>
                <el-table-column prop="errorMessage" label="异常" min-width="220" show-overflow-tooltip />
                <el-table-column label="时间" min-width="170">
                  <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane label="播报" name="broadcast">
              <section class="summary">
                <span>日志 {{ aggregate.broadcastLogs.length }}</span>
                <span>云喇叭 {{ aggregate.broadcastLogs.filter((item) => item.deviceType === 'CLOUD_SPEAKER').length }}</span>
                <span>确认屏 {{ aggregate.broadcastLogs.filter((item) => item.deviceType === 'CONFIRM_SCREEN').length }}</span>
                <span :class="{ warning: offlineConfirmLogs > 0 }">离线 {{ offlineConfirmLogs }}</span>
              </section>
              <el-table :data="aggregate.broadcastLogs" border stripe empty-text="暂无播报日志">
                <el-table-column prop="id" label="ID" width="80" />
                <el-table-column prop="giftRecordId" label="礼金ID" width="100" />
                <el-table-column label="设备" min-width="140">
                  <template #default="{ row }">{{ displayLabel(row.deviceType) }}</template>
                </el-table-column>
                <el-table-column label="事件" width="120">
                  <template #default="{ row }">{{ displayLabel(row.eventType) }}</template>
                </el-table-column>
                <el-table-column prop="content" label="内容" min-width="260" show-overflow-tooltip />
                <el-table-column label="状态" width="100">
                  <template #default="{ row }"><el-tag :type="tagType(row.status)">{{ displayLabel(row.status) }}</el-tag></template>
                </el-table-column>
                <el-table-column label="时间" min-width="170">
                  <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane label="操作日志" name="logs">
              <el-table :data="aggregate.operationLogs" border stripe empty-text="暂无宴席操作日志">
                <el-table-column prop="id" label="ID" width="80" />
                <el-table-column label="模块" width="120">
                  <template #default="{ row }">{{ displayLabel(row.module) }}</template>
                </el-table-column>
                <el-table-column prop="action" label="动作" min-width="170" show-overflow-tooltip />
                <el-table-column prop="summary" label="摘要" min-width="220" show-overflow-tooltip />
                <el-table-column label="操作者" width="100">
                  <template #default="{ row }">{{ displayLabel(row.operatorType) }}</template>
                </el-table-column>
                <el-table-column label="时间" min-width="170">
                  <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </div>
      </template>
    </el-drawer>
  </section>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: 24px;
  background: #f6f7f9;
}

header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

h1 {
  margin: 0;
  font-size: 20px;
}

.actions {
  display: flex;
  gap: 8px;
}

.detail-section {
  padding-bottom: 18px;
  margin-bottom: 18px;
  border-bottom: 1px solid #e5e7eb;
}

.detail-section h2,
.detail-section h3 {
  margin: 0 0 12px;
}

.detail-section p {
  margin: 8px 0;
}

.muted {
  color: #6b7280;
}

.danger {
  color: #b91c1c !important;
}

.warning {
  color: #b45309 !important;
}

.hero {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding-bottom: 16px;
  margin-bottom: 12px;
  border-bottom: 1px solid #e5e7eb;
}

.hero h2 {
  margin: 0 0 8px;
  font-size: 22px;
}

.hero p {
  margin: 6px 0;
  color: #4b5563;
}

.metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(110px, 1fr));
  gap: 10px;
  align-content: start;
  min-width: 260px;
}

.quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.workbench-status {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  color: #64748b;
  font-size: 13px;
}

.metrics span,
.summary span {
  padding: 8px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #f9fafb;
  color: #374151;
  font-size: 13px;
}

.workbench-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(230px, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.workbench-card {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  align-items: start;
  min-height: 112px;
  padding: 14px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #f8fbff;
}

.workbench-card.todo {
  border-color: #fed7aa;
  background: #fffaf5;
}

.workbench-card.risk {
  border-color: #fecaca;
  background: #fff7f7;
}

.workbench-card span {
  display: block;
  margin-bottom: 8px;
  color: #64748b;
  font-size: 12px;
}

.workbench-card strong {
  display: block;
  color: #111827;
  font-size: 16px;
  line-height: 1.35;
}

.workbench-card .el-button {
  grid-column: 1 / -1;
  justify-self: start;
}

.advice-panel {
  margin-bottom: 14px;
  padding: 14px;
  border: 1px solid #fed7aa;
  border-radius: 8px;
  background: #fff7ed;
}

.advice-panel h3 {
  margin: 0 0 8px;
  color: #9a3412;
  font-size: 16px;
}

.advice-panel p {
  margin: 6px 0;
  color: #7c2d12;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.panel {
  min-height: 170px;
  padding: 14px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
}

.panel h3,
.sub-title {
  margin: 0 0 12px;
  font-size: 16px;
}

.panel p {
  margin: 8px 0;
  color: #374151;
}

.summary {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 12px;
}

.sub-title {
  margin-top: 18px;
}

@media (max-width: 960px) {
  .hero {
    flex-direction: column;
  }

  .metrics,
  .overview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
