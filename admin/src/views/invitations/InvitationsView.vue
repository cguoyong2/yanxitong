<template>
  <main class="page">
    <header>
      <div>
        <h1>请柬管理</h1>
        <p>集中核对请柬实例、模板引用、分享路径、访问数据和基础字段。</p>
      </div>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </header>

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
import { displayLabel, formatDateTime, tagType } from '../../utils/display';

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

const route = useRoute();
const router = useRouter();
const rows = ref<AdminInvitationSummary[]>([]);
const templates = ref<InvitationTemplate[]>([]);
const loading = ref(false);
const saving = ref(false);
const detailVisible = ref(false);
const editVisible = ref(false);
const selected = ref<AdminInvitationSummary | null>(null);
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

async function loadTemplates() {
  const response = await http.get<ApiResponse<InvitationTemplate[]>>('/admin/invitation-templates');
  templates.value = response.data.data || [];
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

async function goBanquet(banquetId?: number) {
  if (!banquetId) {
    return;
  }
  await router.push({ path: '/banquets', query: { banquetId, focus: 'overview' } });
}

async function goTemplates() {
  await router.push('/templates');
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

onMounted(async () => {
  await Promise.all([loadTemplates(), load()]);
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
  .filters {
    display: grid;
    grid-template-columns: 1fr;
  }

  .filters .el-input,
  .filters .el-select {
    width: 100%;
  }
}
</style>
