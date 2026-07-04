<template>
  <main class="page">
    <header>
      <div>
        <h1>请柬管理</h1>
        <p>集中核对请柬实例、模板引用、分享路径、访问数据和基础字段。</p>
      </div>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </header>

    <section v-if="filters.banquetId" class="context-panel">
      <div>
        <span>当前钻取宴席</span>
        <strong>{{ contextBanquetTitle }}</strong>
        <p>请柬实例、模板引用、分享路径和访问转化已按该宴席过滤。</p>
      </div>
      <div class="context-actions">
        <el-button @click="goBanquet(Number(filters.banquetId))">返回宴席工作台</el-button>
        <el-button @click="goBusiness('rsvp')">回执明细</el-button>
        <el-button @click="goPayments">支付排障</el-button>
        <el-button @click="goOperationLog('banquet', Number(filters.banquetId))">查看日志</el-button>
        <el-button @click="clearContext">清除筛选</el-button>
      </div>
    </section>

    <section class="filters">
      <el-input v-model="filters.banquetId" clearable placeholder="宴席 ID" />
      <el-select v-model="filters.templateId" clearable filterable placeholder="模板">
        <el-option v-for="item in templates" :key="item.id" :label="`${item.id} · ${item.name}`" :value="String(item.id)" />
      </el-select>
      <el-select v-model="filters.status" clearable placeholder="状态">
        <el-option label="启用" value="ACTIVE" />
        <el-option label="停用" value="INACTIVE" />
        <el-option label="草稿" value="DRAFT" />
      </el-select>
      <el-input v-model="filters.keyword" clearable placeholder="标题/分享码" />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button @click="resetFilters">重置</el-button>
    </section>

    <section class="metric-grid">
      <article class="metric">
        <span>请柬实例</span>
        <strong>{{ rows.length }}</strong>
      </article>
      <article class="metric">
        <span>总访问</span>
        <strong>{{ visitTotal }}</strong>
      </article>
      <article class="metric">
        <span>模板正常</span>
        <strong>{{ availableTemplateCount }}</strong>
      </article>
      <article class="metric" :class="{ warning: unavailableTemplateCount > 0 }">
        <span>模板异常</span>
        <strong>{{ unavailableTemplateCount }}</strong>
      </article>
    </section>

    <el-table v-loading="loading" :data="rows" border stripe empty-text="暂无请柬">
      <el-table-column prop="invitation.id" label="ID" width="80" />
      <el-table-column label="宴席" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">
          <strong>{{ row.banquet?.name || '-' }}</strong>
          <small>{{ row.banquet ? `${row.banquet.id} · ${row.banquet.eventTypeCode}` : '-' }}</small>
        </template>
      </el-table-column>
      <el-table-column label="请柬" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">
          <strong>{{ row.invitation.title }}</strong>
          <small>{{ row.invitation.shareSlug }}</small>
        </template>
      </el-table-column>
      <el-table-column label="模板引用" min-width="210" show-overflow-tooltip>
        <template #default="{ row }">
          <strong>{{ row.template?.name || '基础样式' }}</strong>
          <small>{{ row.template ? `${row.template.id} · ${row.template.templateCode}` : '未绑定模板' }}</small>
          <el-tag v-if="!row.templateAvailable" type="warning" size="small" effect="plain">模板不可用</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="公开路径" min-width="260" show-overflow-tooltip>
        <template #default="{ row }">
          <span>{{ row.shareUrl }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="tagType(row.invitation.status)">{{ displayLabel(row.invitation.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="访问" width="130">
        <template #default="{ row }">
          <strong>{{ row.visitCount }}</strong>
          <small>{{ row.lastVisitedAt ? formatDateTime(row.lastVisitedAt) : '暂无访问' }}</small>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" min-width="170">
        <template #default="{ row }">{{ formatDateTime(row.invitation.updatedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button link type="primary" @click="openAnalytics(row)">数据</el-button>
          <el-button link type="primary" @click="openEdit(row)">编辑字段</el-button>
          <el-button link type="primary" @click="copyShare(row.shareUrl)">复制路径</el-button>
          <el-button link type="primary" @click="goBanquet(row.banquet?.id)">宴席工作台</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-drawer v-model="detailVisible" title="请柬详情" size="560px">
      <template v-if="selected">
        <section class="detail-card">
          <h2>{{ selected.invitation.title }}</h2>
          <p>分享码：{{ selected.invitation.shareSlug }}</p>
          <p>公开路径：{{ selected.shareUrl }}</p>
          <p>访问次数：{{ selected.visitCount }}</p>
          <p>最近访问：{{ selected.lastVisitedAt ? formatDateTime(selected.lastVisitedAt) : '暂无访问' }}</p>
          <el-alert v-if="!selected.templateAvailable" type="warning" :closable="false" :title="selected.templateWarning" />
        </section>
        <section class="detail-card">
          <h3>宴席引用</h3>
          <p>{{ selected.banquet?.name || '-' }}</p>
          <p>{{ selected.banquet ? `${selected.banquet.eventTypeCode} / ${selected.banquet.themeCode}` : '-' }}</p>
          <p>{{ selected.banquet ? `${formatDateTime(selected.banquet.banquetTime)} · ${selected.banquet.location || '-'}` : '-' }}</p>
        </section>
        <section class="detail-card">
          <h3>模板引用</h3>
          <p>{{ selected.template?.name || '基础样式' }}</p>
          <p>{{ selected.template ? `${selected.template.templateCode} / ${displayLabel(selected.template.status)}` : '未绑定模板' }}</p>
          <el-button size="small" @click="goTemplates">查看模板管理</el-button>
        </section>
        <section class="detail-card">
          <h3>基础字段</h3>
          <dl>
            <div v-for="(value, key) in selected.basicFields" :key="key">
              <dt>{{ basicFieldLabel(String(key)) }}</dt>
              <dd>{{ value || '-' }}</dd>
            </div>
          </dl>
        </section>
      </template>
    </el-drawer>

    <el-drawer v-model="analyticsVisible" title="请柬访问与转化" size="720px">
      <template v-if="analytics">
        <section class="detail-card">
          <div class="section-title">
            <h3>转化漏斗</h3>
            <small>访问、回执、随礼、设备选择按同一条链路归因</small>
          </div>
          <div class="funnel-list">
            <article
              v-for="step in invitationFunnelSteps(analytics)"
              :key="step.key"
              class="funnel-step clickable"
              :class="{ active: analyticsDrilldown === step.key }"
              role="button"
              tabindex="0"
              @click="openFunnelDrilldown(step.key)"
              @keyup.enter="openFunnelDrilldown(step.key)"
            >
              <div class="funnel-head">
                <span>{{ step.title }}</span>
                <strong>{{ step.value }}</strong>
              </div>
              <div class="funnel-track"><i :style="{ width: rateWidth(step.rate) }"></i></div>
              <p>{{ step.hint }}，访问转化率 {{ step.rate }}%</p>
              <em>{{ step.actionText }}</em>
            </article>
          </div>
        </section>

        <section class="analytics-grid">
          <article class="metric">
            <span>访问次数</span>
            <strong>{{ analytics.visitCount }}</strong>
            <small>独立 IP {{ analytics.uniqueIpCount }}</small>
          </article>
          <article class="metric">
            <span>回执转化</span>
            <strong>{{ analytics.rsvpConversionRate }}%</strong>
            <small>{{ analytics.rsvpCount }} 条 / {{ analytics.rsvpGuestCount }} 人</small>
          </article>
          <article class="metric">
            <span>随礼转化</span>
            <strong>{{ analytics.giftConversionRate }}%</strong>
            <small>{{ analytics.giftCount }} 笔 / {{ formatMoney(analytics.giftAmount) }}</small>
          </article>
          <article class="metric">
            <span>设备选择</span>
            <strong>{{ analytics.deviceConversionRate }}%</strong>
            <small>{{ analytics.deviceOrderCount }} 单 / 已支付 {{ analytics.paidDeviceOrderCount }}</small>
          </article>
        </section>

        <section class="detail-card">
          <h3>访问趋势</h3>
          <div class="trend-list">
            <div v-for="item in analytics.visitTrend" :key="item.date" class="trend-row">
              <span>{{ item.date }}</span>
              <div><i :style="{ width: trendWidth(item.count) }"></i></div>
              <strong>{{ item.count }}</strong>
            </div>
            <p v-if="analytics.visitTrend.length === 0">暂无访问趋势。</p>
          </div>
        </section>

        <section class="analytics-columns">
          <article class="detail-card">
            <h3>访问来源</h3>
            <p v-for="item in analytics.sourceBreakdown" :key="item.label">{{ displayLabel(item.label) }}：{{ item.count }}</p>
            <p v-if="analytics.sourceBreakdown.length === 0">暂无来源数据。</p>
          </article>
          <article class="detail-card">
            <h3>分享渠道</h3>
            <p v-for="item in analytics.shareChannelBreakdown" :key="item.label">{{ displayLabel(item.label) }}：{{ item.count }}</p>
            <p v-if="analytics.shareChannelBreakdown.length === 0">暂无分享记录。</p>
          </article>
          <article class="detail-card">
            <h3>回执分布</h3>
            <p v-for="item in analytics.rsvpBreakdown" :key="item.label">{{ displayLabel(item.label) }}：{{ item.count }}</p>
            <p v-if="analytics.rsvpBreakdown.length === 0">暂无回执记录。</p>
          </article>
          <article class="detail-card">
            <h3>设备选择</h3>
            <p v-for="item in analytics.deviceBreakdown" :key="item.label">{{ displayLabel(item.label) }}：{{ item.count }}</p>
            <p v-if="analytics.deviceBreakdown.length === 0">暂无设备订单。</p>
          </article>
        </section>

        <section ref="visitDetailRef" class="detail-card" :class="{ highlighted: analyticsDrilldown === 'visit' }">
          <h3>最近访问</h3>
          <el-table :data="analytics.recentVisits" border stripe empty-text="暂无访问">
            <el-table-column label="时间" min-width="170">
              <template #default="{ row }">{{ formatDateTime(row.visitedAt) }}</template>
            </el-table-column>
            <el-table-column prop="ipAddress" label="IP" min-width="130" />
            <el-table-column label="来源" width="140">
              <template #default="{ row }">{{ displayLabel(row.source) }}</template>
            </el-table-column>
            <el-table-column prop="userAgent" label="User-Agent" min-width="260" show-overflow-tooltip />
          </el-table>
        </section>
      </template>
    </el-drawer>

    <el-dialog v-model="editVisible" title="编辑基础请柬字段" width="620px">
      <el-form label-width="120px">
        <el-form-item label="请柬标题" required>
          <el-input v-model="editForm.title" />
        </el-form-item>
        <el-form-item label="封面URL">
          <el-input v-model="editForm.coverUrl" />
        </el-form-item>
        <el-form-item label="主办人">
          <el-input v-model="editForm.hostName" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="editForm.contactPhone" />
        </el-form-item>
        <el-form-item label="地址详情">
          <el-input v-model="editForm.addressDetail" />
        </el-form-item>
        <el-form-item label="日程文本">
          <el-input v-model="editForm.scheduleText" />
        </el-form-item>
        <el-form-item label="邀请文案">
          <el-input v-model="editForm.greeting" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="展示入口">
          <el-checkbox v-model="editForm.showGiftEntry">在线随礼</el-checkbox>
          <el-checkbox v-model="editForm.showDeviceEntry">设备入口</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveBasic">保存请柬</el-button>
      </template>
    </el-dialog>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { http, recordsOf, type ApiResponse, type PageResult } from '../../api/client';
import { displayLabel, formatDateTime, formatMoney, tagType } from '../../utils/display';

interface AdminInvitationSummary {
  invitation: Record<string, any>;
  banquet?: Record<string, any>;
  template?: Record<string, any>;
  basicFields: Record<string, string>;
  shareUrl: string;
  visitCount: number;
  lastVisitedAt?: string;
  templateAvailable: boolean;
  templateWarning?: string;
}

interface InvitationTemplate {
  id: number;
  name: string;
  templateCode: string;
}

interface InvitationAnalytics {
  invitationId: number;
  banquetId?: number;
  visitCount: number;
  uniqueIpCount: number;
  rsvpCount: number;
  rsvpGuestCount: number;
  giftCount: number;
  giftAmount: number;
  deviceOrderCount: number;
  paidDeviceOrderCount: number;
  rsvpConversionRate: number;
  giftConversionRate: number;
  deviceConversionRate: number;
  visitTrend: { date: string; count: number }[];
  sourceBreakdown: { label: string; count: number }[];
  shareChannelBreakdown: { label: string; count: number }[];
  rsvpBreakdown: { label: string; count: number }[];
  deviceBreakdown: { label: string; count: number }[];
  recentVisits: { visitedAt: string; ipAddress?: string; source: string; userAgent?: string }[];
}

type FunnelKey = 'visit' | 'rsvp' | 'gift' | 'device';

const route = useRoute();
const router = useRouter();
const rows = ref<AdminInvitationSummary[]>([]);
const templates = ref<InvitationTemplate[]>([]);
const banquetOptions = ref<{ id: number; name: string }[]>([]);
const loading = ref(false);
const saving = ref(false);
const detailVisible = ref(false);
const analyticsVisible = ref(false);
const editVisible = ref(false);
const selected = ref<AdminInvitationSummary | null>(null);
const analytics = ref<InvitationAnalytics | null>(null);
const analyticsDrilldown = ref<FunnelKey>('visit');
const visitDetailRef = ref<HTMLElement | null>(null);
const filters = reactive({
  banquetId: String(route.query.banquetId || ''),
  templateId: String(route.query.templateId || ''),
  status: String(route.query.status || ''),
  keyword: String(route.query.keyword || '')
});
const editForm = reactive({
  id: 0,
  title: '',
  coverUrl: '',
  hostName: '',
  contactPhone: '',
  addressDetail: '',
  scheduleText: '',
  greeting: '',
  showGiftEntry: true,
  showDeviceEntry: true
});

const visitTotal = computed(() => rows.value.reduce((total, item) => total + Number(item.visitCount || 0), 0));
const availableTemplateCount = computed(() => rows.value.filter((item) => item.templateAvailable).length);
const unavailableTemplateCount = computed(() => rows.value.length - availableTemplateCount.value);
const contextBanquet = computed(() => banquetOptions.value.find((item) => String(item.id) === String(filters.banquetId)));
const contextBanquetTitle = computed(() => contextBanquet.value ? `${contextBanquet.value.id} · ${contextBanquet.value.name}` : `宴席 ID ${filters.banquetId}`);

async function loadTemplates() {
  const response = await http.get<ApiResponse<InvitationTemplate[]>>('/admin/invitation-templates');
  templates.value = response.data.data || [];
}

async function loadBanquets() {
  const response = await http.get<ApiResponse<{ id: number; name: string }[]>>('/admin/banquets');
  banquetOptions.value = response.data.data || [];
}

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
    const response = await http.get<ApiResponse<PageResult<AdminInvitationSummary> | AdminInvitationSummary[]>>(`/admin/invitations?${params}`);
    rows.value = recordsOf(response.data.data);
    const query: Record<string, string> = {};
    params.forEach((value, key) => {
      query[key] = value;
    });
    void router.replace({ path: '/invitations', query });
  } finally {
    loading.value = false;
  }
}

function resetFilters() {
  filters.banquetId = filters.banquetId || String(route.query.banquetId || '');
  filters.templateId = '';
  filters.status = '';
  filters.keyword = '';
  void load();
}

function clearContext() {
  filters.banquetId = '';
  filters.templateId = '';
  filters.status = '';
  filters.keyword = '';
  void load();
}

function openDetail(row: AdminInvitationSummary) {
  selected.value = row;
  detailVisible.value = true;
}

async function openAnalytics(row: AdminInvitationSummary) {
  selected.value = row;
  const response = await http.get<ApiResponse<InvitationAnalytics>>(`/admin/invitations/${row.invitation.id}/analytics`);
  analytics.value = response.data.data;
  analyticsDrilldown.value = 'visit';
  analyticsVisible.value = true;
}

function openEdit(row: AdminInvitationSummary) {
  selected.value = row;
  editForm.id = Number(row.invitation.id);
  editForm.title = String(row.invitation.title || '');
  editForm.coverUrl = String(row.invitation.coverUrl || '');
  editForm.hostName = row.basicFields.hostName || '';
  editForm.contactPhone = row.basicFields.contactPhone || '';
  editForm.addressDetail = row.basicFields.addressDetail || '';
  editForm.scheduleText = row.basicFields.scheduleText || '';
  editForm.greeting = row.basicFields.greeting || '';
  editForm.showGiftEntry = row.basicFields.showGiftEntry !== '0';
  editForm.showDeviceEntry = row.basicFields.showDeviceEntry !== '0';
  editVisible.value = true;
}

async function saveBasic() {
  if (!editForm.title) {
    ElMessage.warning('请填写请柬标题');
    return;
  }
  saving.value = true;
  try {
    await http.put(`/invitations/${editForm.id}/basic`, {
      title: editForm.title,
      coverUrl: editForm.coverUrl,
      hostName: editForm.hostName,
      contactPhone: editForm.contactPhone,
      addressDetail: editForm.addressDetail,
      scheduleText: editForm.scheduleText,
      greeting: editForm.greeting,
      showGiftEntry: editForm.showGiftEntry,
      showDeviceEntry: editForm.showDeviceEntry
    });
    ElMessage.success('请柬已保存');
    editVisible.value = false;
    await load();
  } finally {
    saving.value = false;
  }
}

function copyShare(path: string) {
  navigator.clipboard?.writeText(path);
  ElMessage.success('公开路径已复制');
}

async function goBanquet(banquetId?: number, focus = 'overview') {
  if (!banquetId) {
    return;
  }
  await router.push({ path: '/banquets', query: { banquetId, focus } });
}

async function goTemplates() {
  await router.push('/templates');
}

async function goBusiness(tab: string) {
  if (!filters.banquetId) {
    return;
  }
  await router.push({ path: '/business', query: { banquetId: filters.banquetId, tab } });
}

async function goPayments() {
  if (!filters.banquetId) {
    return;
  }
  await router.push({ path: '/payments', query: { banquetId: filters.banquetId, tab: 'orders' } });
}

async function goOperationLog(targetType: string, targetId: number) {
  await router.push({ path: '/operation-logs', query: { targetType, targetId } });
}

function basicFieldLabel(key: string) {
  const labels: Record<string, string> = {
    hostName: '主办人',
    contactPhone: '联系电话',
    addressDetail: '地址详情',
    scheduleText: '日程文本',
    greeting: '邀请文案',
    showGiftEntry: '在线随礼入口',
    showDeviceEntry: '设备入口'
  };
  return labels[key] || key;
}

function trendWidth(count: number) {
  const max = Math.max(1, ...(analytics.value?.visitTrend || []).map((item) => item.count));
  return `${Math.max(6, Math.round((count / max) * 100))}%`;
}

function rateWidth(rate: number) {
  return `${Math.max(4, Math.min(100, Number(rate || 0)))}%`;
}

function invitationFunnelSteps(data: InvitationAnalytics) {
  return [
    {
      key: 'visit' as const,
      title: '访问请柬',
      value: data.visitCount,
      rate: data.visitCount > 0 ? 100 : 0,
      hint: `独立 IP ${data.uniqueIpCount}`,
      actionText: '查看最近访问'
    },
    {
      key: 'rsvp' as const,
      title: '提交回执',
      value: data.rsvpCount,
      rate: data.rsvpConversionRate,
      hint: `赴宴人数 ${data.rsvpGuestCount}`,
      actionText: '进入 RSVP 明细'
    },
    {
      key: 'gift' as const,
      title: '完成随礼',
      value: data.giftCount,
      rate: data.giftConversionRate,
      hint: `礼金合计 ${formatMoney(data.giftAmount)}`,
      actionText: '进入礼金明细'
    },
    {
      key: 'device' as const,
      title: '选择设备',
      value: data.deviceOrderCount,
      rate: data.deviceConversionRate,
      hint: `已支付设备订单 ${data.paidDeviceOrderCount}`,
      actionText: '进入设备订单'
    }
  ];
}

async function openFunnelDrilldown(key: FunnelKey) {
  analyticsDrilldown.value = key;
  if (key === 'visit') {
    requestAnimationFrame(() => {
      visitDetailRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
    return;
  }
  const banquetId = selected.value?.banquet?.id || analytics.value?.banquetId;
  const focus = key === 'rsvp' ? 'rsvp' : key === 'gift' ? 'gifts' : 'devices';
  await goBanquet(Number(banquetId), focus);
}

onMounted(async () => {
  await Promise.all([loadTemplates(), loadBanquets(), load()]);
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
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
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
  display: flex;
  flex-wrap: wrap;
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
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  background: #eff6ff;
}

.context-panel span {
  display: block;
  color: #2563eb;
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
  width: 170px;
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

.metric.warning {
  border-color: #fed7aa;
  background: #fff7ed;
}

.metric span,
small {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.metric strong {
  color: #111827;
  font-size: 20px;
}

.analytics-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.analytics-columns {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.detail-card {
  margin-bottom: 14px;
  padding: 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.detail-card h2,
.detail-card h3 {
  margin: 0 0 10px;
}

.section-title {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 12px;
}

.section-title h3 {
  margin: 0;
}

.funnel-list {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.funnel-step {
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: linear-gradient(180deg, #fff, #f8fafc);
}

.funnel-step.clickable {
  cursor: pointer;
  transition: border-color 0.16s ease, box-shadow 0.16s ease, transform 0.16s ease;
}

.funnel-step.clickable:hover,
.funnel-step.clickable:focus,
.funnel-step.active {
  border-color: #f97316;
  box-shadow: 0 8px 22px rgba(249, 115, 22, 0.14);
  outline: none;
  transform: translateY(-1px);
}

.funnel-head {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  align-items: baseline;
}

.funnel-head span {
  color: #64748b;
  font-size: 13px;
}

.funnel-head strong {
  color: #111827;
  font-size: 22px;
}

.funnel-track {
  height: 8px;
  margin: 10px 0;
  overflow: hidden;
  border-radius: 999px;
  background: #e5e7eb;
}

.funnel-track i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #dc2626, #f59e0b);
}

.funnel-step p {
  min-height: 32px;
  margin: 0;
  color: #64748b;
  font-size: 12px;
}

.funnel-step em {
  display: block;
  margin-top: 8px;
  color: #c2410c;
  font-size: 12px;
  font-style: normal;
  font-weight: 600;
}

.detail-card.highlighted {
  border-color: #fdba74;
  box-shadow: 0 8px 22px rgba(249, 115, 22, 0.12);
}

.trend-list {
  display: grid;
  gap: 10px;
}

.trend-row {
  display: grid;
  grid-template-columns: 120px 1fr 50px;
  gap: 10px;
  align-items: center;
}

.trend-row span {
  color: #64748b;
  font-size: 13px;
}

.trend-row div {
  height: 10px;
  overflow: hidden;
  border-radius: 999px;
  background: #e5e7eb;
}

.trend-row i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #2563eb, #60a5fa);
}

.trend-row strong {
  text-align: right;
}

dl {
  display: grid;
  gap: 10px;
}

dl div {
  display: grid;
  grid-template-columns: 120px 1fr;
  gap: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f1f5f9;
}

dt {
  color: #64748b;
}

dd {
  margin: 0;
  color: #111827;
}

@media (max-width: 860px) {
  header,
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

  .analytics-grid,
  .analytics-columns,
  .funnel-list {
    grid-template-columns: 1fr;
  }
}
</style>
