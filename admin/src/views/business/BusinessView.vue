<template>
  <main class="page">
    <header>
      <h1>业务数据</h1>
      <el-button @click="loadAll">刷新</el-button>
    </header>

    <el-alert
      class="export-notice"
      type="info"
      :closable="false"
      show-icon
      title="导出超过配置上限会被拒绝；上限由配置中心 export.max_rows 维护。"
    />

    <section v-if="contextBanquetId" class="context-panel">
      <div>
        <span>当前钻取宴席</span>
        <strong>{{ contextBanquetTitle }}</strong>
        <p>下方礼金、回执和人情账本已按该宴席过滤；当前页签：{{ tabLabel(activeTab) }}。</p>
      </div>
      <div class="context-actions">
        <el-button @click="goAuditBanquet">返回宴席工作台</el-button>
        <el-button @click="goAuditPayments">查看支付</el-button>
        <el-button @click="goAuditOperationLogs">查看日志</el-button>
        <el-button @click="clearContext">清除筛选</el-button>
      </div>
    </section>

    <section class="audit-panel">
      <div class="audit-head">
        <div>
          <h2>宴席核对总览</h2>
          <p>按宴席汇总小程序产生的回执、收礼、人情和操作日志，方便运营快速核对闭环。</p>
        </div>
        <div class="audit-actions">
          <el-select v-model="auditBanquetId" filterable clearable placeholder="选择宴席" class="audit-select">
            <el-option
              v-for="item in banquetOptions"
              :key="item.id"
              :label="`${item.id} · ${item.name}`"
              :value="String(item.id)"
            />
          </el-select>
          <el-button type="primary" :disabled="!auditBanquetId" :loading="loading.audit" @click="loadAudit">核对</el-button>
          <el-button :disabled="!auditBanquetId" @click="applyAuditFilters">套用到下方表格</el-button>
        </div>
      </div>

      <section class="metric-grid audit-metrics">
        <article class="metric">
          <span>回执记录</span>
          <strong>{{ audit.rsvpStats?.totalRecords ?? '-' }}</strong>
          <small>{{ audit.rsvpStats ? `预计 ${audit.rsvpStats.totalGuests || 0} 人` : '待核对' }}</small>
        </article>
        <article class="metric">
          <span>收礼记录</span>
          <strong>{{ audit.gifts.length }}</strong>
          <small>{{ formatMoney(auditGiftTotal) }}</small>
        </article>
        <article class="metric">
          <span>人情对象</span>
          <strong>{{ audit.favorContacts.length }}</strong>
          <small>{{ balanceText(auditFavorBalance) }}</small>
        </article>
        <article class="metric">
          <span>关键操作</span>
          <strong>{{ audit.operationLogs.length }}</strong>
          <small>创建、回执、记礼、补录可追踪</small>
        </article>
        <article class="metric">
          <span>核对状态</span>
          <strong :class="auditStatusClass">{{ auditStatusText }}</strong>
          <small>{{ auditStatusHint }}</small>
        </article>
      </section>

      <section class="audit-quick-actions" aria-label="宴席核对快捷处理">
        <el-button :disabled="!auditBanquetId" @click="goAuditBanquet">宴席详情</el-button>
        <el-button :disabled="!auditBanquetId" @click="focusAuditSection('rsvp')">处理回执</el-button>
        <el-button :disabled="!auditBanquetId" @click="focusAuditSection('gifts')">处理收礼</el-button>
        <el-button :disabled="!auditBanquetId" @click="focusAuditSection('favor')">处理人情</el-button>
        <el-button :disabled="!auditBanquetId" @click="goAuditOperationLogs">查看操作日志</el-button>
        <el-button :disabled="!auditBanquetId" @click="goAuditPayments">排查支付</el-button>
      </section>

      <el-table :data="auditRows" border stripe empty-text="请选择宴席后核对">
        <el-table-column prop="name" label="核对项" width="150" />
        <el-table-column prop="value" label="结果" min-width="180" />
        <el-table-column prop="hint" label="说明" min-width="280" show-overflow-tooltip />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.ok ? 'success' : 'warning'">{{ row.ok ? '通过' : '待补' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="处理入口" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="!auditBanquetId" @click="handleAuditRow(row.key)">去处理</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="礼金记录" name="gifts">
        <section class="toolbar">
          <el-input v-model="giftFilters.banquetId" clearable placeholder="宴席 ID" />
          <el-select v-model="giftFilters.source" clearable placeholder="来源">
            <el-option label="线上随礼" value="ONLINE_GIFT" />
            <el-option label="现场扫码" value="ONSITE_QR" />
            <el-option label="现金记礼" value="CASH" />
          </el-select>
          <el-input v-model="giftFilters.keyword" clearable placeholder="来宾姓名" />
          <el-button type="primary" @click="loadGifts">查询</el-button>
          <el-button @click="resetGiftFilters">重置</el-button>
          <el-button @click="openOfflineGift">现金记礼</el-button>
          <el-button :disabled="!giftFilters.banquetId" :loading="isExporting('gifts', 'csv')" @click="downloadExport('gifts', 'csv', giftFilters.banquetId)">导出CSV</el-button>
          <el-button :disabled="!giftFilters.banquetId" :loading="isExporting('gifts', 'xlsx')" @click="downloadExport('gifts', 'xlsx', giftFilters.banquetId)">导出XLSX</el-button>
        </section>
        <section class="metric-grid">
          <article class="metric">
            <span>礼金笔数</span>
            <strong>{{ giftSummary.count }}</strong>
          </article>
          <article class="metric">
            <span>礼金总额</span>
            <strong>{{ formatMoney(giftSummary.totalAmount) }}</strong>
          </article>
          <article class="metric">
            <span>线上随礼</span>
            <strong>{{ formatMoney(giftSummary.onlineAmount) }}</strong>
          </article>
          <article class="metric">
            <span>现场扫码</span>
            <strong>{{ formatMoney(giftSummary.onsiteAmount) }}</strong>
          </article>
          <article class="metric">
            <span>现金记礼</span>
            <strong>{{ formatMoney(giftSummary.cashAmount) }}</strong>
          </article>
        </section>
        <el-table v-loading="loading.gifts" :data="gifts" border stripe empty-text="暂无礼金记录">
          <el-table-column prop="id" label="ID" width="90" />
          <el-table-column prop="banquetId" label="宴席ID" width="100" />
          <el-table-column label="来源" width="130">
            <template #default="{ row }"><el-tag :type="tagType(row.giftSource)">{{ displayLabel(row.giftSource) }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="guestName" label="来宾" min-width="150" />
          <el-table-column label="金额" width="120">
            <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
          </el-table-column>
          <el-table-column prop="blessing" label="祝福/备注" min-width="220" show-overflow-tooltip />
          <el-table-column prop="paymentOrderId" label="支付订单ID" width="120" />
          <el-table-column label="收礼时间" min-width="170">
            <template #default="{ row }">{{ formatDateTime(row.receivedAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="110" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="goBanquet(row.banquetId as number)">宴席视图</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="回执 RSVP" name="rsvp">
        <section class="toolbar">
          <el-input v-model="rsvpFilters.banquetId" clearable placeholder="宴席 ID" />
          <el-select v-model="rsvpFilters.status" clearable placeholder="状态">
            <el-option label="出席" value="ATTEND" />
            <el-option label="出席（兼容）" value="ATTENDING" />
            <el-option label="待定" value="PENDING" />
            <el-option label="婉拒" value="DECLINED" />
          </el-select>
          <el-input v-model="rsvpFilters.keyword" clearable placeholder="姓名/手机号" />
          <el-button type="primary" @click="loadRsvp">查询</el-button>
          <el-button @click="resetRsvpFilters">重置</el-button>
          <el-button :disabled="!rsvpFilters.banquetId" @click="loadRsvpStats">统计</el-button>
          <el-button :disabled="!rsvpFilters.banquetId" :loading="isExporting('rsvp', 'csv')" @click="downloadExport('rsvp', 'csv', rsvpFilters.banquetId)">导出CSV</el-button>
          <el-button :disabled="!rsvpFilters.banquetId" :loading="isExporting('rsvp', 'xlsx')" @click="downloadExport('rsvp', 'xlsx', rsvpFilters.banquetId)">导出XLSX</el-button>
        </section>
        <section class="metric-grid">
          <article class="metric">
            <span>回执记录</span>
            <strong>{{ effectiveRsvpStats.totalRecords }}</strong>
          </article>
          <article class="metric">
            <span>确认出席</span>
            <strong>{{ effectiveRsvpStats.attendingRecords }}</strong>
          </article>
          <article class="metric">
            <span>预计人数</span>
            <strong>{{ effectiveRsvpStats.totalGuests }}</strong>
          </article>
          <article class="metric">
            <span>需要用餐</span>
            <strong>{{ effectiveRsvpStats.mealRequiredGuests }}</strong>
          </article>
          <article class="metric">
            <span>需要住宿</span>
            <strong>{{ effectiveRsvpStats.accommodationRequiredGuests }}</strong>
          </article>
        </section>
        <el-table v-loading="loading.rsvp" :data="rsvpRows" border stripe empty-text="暂无 RSVP">
          <el-table-column prop="id" label="ID" width="90" />
          <el-table-column prop="banquetId" label="宴席ID" width="100" />
          <el-table-column prop="guestName" label="姓名" min-width="140" />
          <el-table-column prop="phone" label="手机号" min-width="140" />
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
          <el-table-column label="操作" width="110" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="goBanquet(row.banquetId as number)">宴席视图</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="人情账本" name="favor">
        <section class="toolbar">
          <el-input v-model="favorKeyword" clearable placeholder="联系人姓名" />
          <el-input v-model="favorExportBanquetId" clearable placeholder="导出宴席 ID" />
          <el-button type="primary" @click="loadFavorContacts">查询</el-button>
          <el-button @click="resetFavorFilters">重置</el-button>
          <el-button @click="openFavorManual">手动补录</el-button>
          <el-button :disabled="!favorExportBanquetId" :loading="isExporting('favor', 'csv')" @click="downloadExport('favor', 'csv', favorExportBanquetId)">导出CSV</el-button>
          <el-button :disabled="!favorExportBanquetId" :loading="isExporting('favor', 'xlsx')" @click="downloadExport('favor', 'xlsx', favorExportBanquetId)">导出XLSX</el-button>
        </section>
        <section class="metric-grid">
          <article class="metric">
            <span>联系人</span>
            <strong>{{ favorSummary.count }}</strong>
          </article>
          <article class="metric">
            <span>收礼合计</span>
            <strong>{{ formatMoney(favorSummary.receivedAmount) }}</strong>
          </article>
          <article class="metric">
            <span>回礼合计</span>
            <strong>{{ formatMoney(favorSummary.givenAmount) }}</strong>
          </article>
          <article class="metric">
            <span>当前差额</span>
            <strong :class="balanceClass(favorSummary.balance)">{{ formatMoney(favorSummary.balance) }}</strong>
            <small>{{ balanceText(favorSummary.balance) }}</small>
          </article>
        </section>
        <el-table v-loading="loading.favor" :data="favorContacts" border stripe empty-text="暂无人情联系人">
          <el-table-column prop="contactId" label="联系人ID" width="110" />
          <el-table-column prop="contactName" label="联系人" min-width="160" />
          <el-table-column label="收礼合计" width="130">
            <template #default="{ row }">{{ formatMoney(row.receivedAmount) }}</template>
          </el-table-column>
          <el-table-column label="回礼合计" width="130">
            <template #default="{ row }">{{ formatMoney(row.givenAmount) }}</template>
          </el-table-column>
          <el-table-column label="差额" width="130">
            <template #default="{ row }">
              <span :class="balanceClass(row.balance)">{{ formatMoney(row.balance) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="差额说明" min-width="150">
            <template #default="{ row }">{{ balanceText(row.balance) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openFavorDetail(row.contactId as number)">明细</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="offlineGiftVisible" title="现金记礼" width="520px">
      <el-form label-width="110px">
        <el-form-item label="宴席 ID" required>
          <el-input v-model="offlineGiftForm.banquetId" />
        </el-form-item>
        <el-form-item label="来宾姓名" required>
          <el-input v-model="offlineGiftForm.guestName" />
        </el-form-item>
        <el-form-item label="金额" required>
          <el-input-number v-model="offlineGiftForm.amount" :min="0.01" :precision="2" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="offlineGiftForm.blessing" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="offlineGiftVisible = false">取消</el-button>
        <el-button type="primary" @click="submitOfflineGift">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="favorManualVisible" title="人情补录" width="560px">
      <el-form label-width="110px">
        <el-form-item label="联系人" required>
          <el-input v-model="favorForm.contactName" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="favorForm.phone" />
        </el-form-item>
        <el-form-item label="方向" required>
          <el-select v-model="favorForm.direction">
            <el-option label="收到" value="RECEIVED" />
            <el-option label="给出" value="GIVEN" />
          </el-select>
        </el-form-item>
        <el-form-item label="宴席 ID">
          <el-input v-model="favorForm.banquetId" />
        </el-form-item>
        <el-form-item label="金额" required>
          <el-input-number v-model="favorForm.amount" :min="0.01" :precision="2" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="favorForm.note" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="favorManualVisible = false">取消</el-button>
        <el-button type="primary" @click="submitFavorManual">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="favorDetailVisible" title="人情明细" size="560px">
      <template v-if="favorDetail">
        <section class="detail-summary">
          <h2>{{ favorDetail.contact.contactName }}</h2>
          <span>收 {{ formatMoney(favorDetail.receivedAmount) }}</span>
          <span>给 {{ formatMoney(favorDetail.givenAmount) }}</span>
          <span :class="balanceClass(favorDetail.balance)">差额 {{ formatMoney(favorDetail.balance) }}</span>
          <span>{{ balanceText(favorDetail.balance) }}</span>
        </section>
        <el-table :data="favorDetail.entries" border stripe empty-text="暂无明细">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column label="方向" width="100">
            <template #default="{ row }"><el-tag :type="tagType(row.direction)">{{ displayLabel(row.direction) }}</el-tag></template>
          </el-table-column>
          <el-table-column label="来源" width="120">
            <template #default="{ row }">{{ displayLabel(row.sourceType) }}</template>
          </el-table-column>
          <el-table-column label="金额" width="110">
            <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
          </el-table-column>
          <el-table-column prop="banquetId" label="宴席ID" width="100" />
          <el-table-column prop="note" label="备注" min-width="180" show-overflow-tooltip />
          <el-table-column label="时间" min-width="170">
            <template #default="{ row }">{{ formatDateTime(row.occurredAt) }}</template>
          </el-table-column>
        </el-table>
      </template>
    </el-drawer>
  </main>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus';
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { http, recordsOf, type ApiResponse, type PageResult } from '../../api/client';
import { displayLabel, formatDateTime, formatMoney, tagType } from '../../utils/display';

interface RsvpStats {
  totalRecords: number;
  attendingRecords: number;
  totalGuests: number;
  mealRequiredGuests: number;
  accommodationRequiredGuests: number;
}

interface GiftSummary {
  count: number;
  totalAmount: number;
  onlineAmount: number;
  onsiteAmount: number;
  cashAmount: number;
}

interface FavorSummary {
  count: number;
  receivedAmount: number;
  givenAmount: number;
  balance: number;
}

interface FavorDetail {
  contact: { contactName: string };
  receivedAmount: number;
  givenAmount: number;
  balance: number;
  entries: Record<string, unknown>[];
}

interface BanquetOption {
  id: number;
  name: string;
}

interface AuditState {
  rsvpStats: RsvpStats | null;
  gifts: Record<string, unknown>[];
  favorContacts: Record<string, unknown>[];
  operationLogs: Record<string, unknown>[];
}

type BusinessTab = 'gifts' | 'rsvp' | 'favor';
type AuditRowKey = BusinessTab | 'operationLogs';

const gifts = ref<Record<string, unknown>[]>([]);
const route = useRoute();
const router = useRouter();
const rsvpRows = ref<Record<string, unknown>[]>([]);
const favorContacts = ref<Record<string, unknown>[]>([]);
const favorDetail = ref<FavorDetail | null>(null);
const rsvpStats = ref<RsvpStats | null>(null);
const banquetOptions = ref<BanquetOption[]>([]);
const auditBanquetId = ref(String(route.query.banquetId || ''));
const favorKeyword = ref('');
const favorExportBanquetId = ref(String(route.query.banquetId || ''));
const offlineGiftVisible = ref(false);
const favorManualVisible = ref(false);
const favorDetailVisible = ref(false);
const activeTab = ref<BusinessTab>(tabFromQuery(route.query.tab));
const loading = reactive({ gifts: false, rsvp: false, favor: false, audit: false });
const audit = reactive<AuditState>({
  rsvpStats: null,
  gifts: [],
  favorContacts: [],
  operationLogs: []
});

const giftFilters = reactive({ banquetId: String(route.query.banquetId || ''), source: '', keyword: '' });
const rsvpFilters = reactive({ banquetId: String(route.query.banquetId || ''), status: '', keyword: '' });
const offlineGiftForm = reactive({ banquetId: '', guestName: '', amount: 100, blessing: '' });
const favorForm = reactive({ contactName: '', phone: '', direction: 'RECEIVED', banquetId: '', amount: 100, note: '' });
const exportLoading = reactive<Record<string, boolean>>({});

const giftSummary = computed<GiftSummary>(() => {
  const summary = {
    count: gifts.value.length,
    totalAmount: 0,
    onlineAmount: 0,
    onsiteAmount: 0,
    cashAmount: 0
  };
  gifts.value.forEach((row) => {
    const amount = toNumber(row.amount);
    summary.totalAmount += amount;
    if (row.giftSource === 'ONLINE_GIFT') {
      summary.onlineAmount += amount;
    }
    if (row.giftSource === 'ONSITE_QR') {
      summary.onsiteAmount += amount;
    }
    if (row.giftSource === 'CASH') {
      summary.cashAmount += amount;
    }
  });
  return summary;
});

const localRsvpStats = computed<RsvpStats>(() => {
  const attending = rsvpRows.value.filter((row) => ['ATTEND', 'ATTENDING'].includes(String(row.attendanceStatus)));
  return {
    totalRecords: rsvpRows.value.length,
    attendingRecords: attending.length,
    totalGuests: sum(attending.map((row) => row.guestCount)),
    mealRequiredGuests: sum(attending.filter((row) => Boolean(row.mealRequired)).map((row) => row.guestCount)),
    accommodationRequiredGuests: sum(attending.filter((row) => Boolean(row.accommodationRequired)).map((row) => row.guestCount))
  };
});

const effectiveRsvpStats = computed<RsvpStats>(() => rsvpStats.value || localRsvpStats.value);

const favorSummary = computed<FavorSummary>(() => {
  const receivedAmount = sum(favorContacts.value.map((row) => row.receivedAmount));
  const givenAmount = sum(favorContacts.value.map((row) => row.givenAmount));
  return {
    count: favorContacts.value.length,
    receivedAmount,
    givenAmount,
    balance: receivedAmount - givenAmount
  };
});
const auditGiftTotal = computed(() => sum(audit.gifts.map((row) => row.amount)));
const auditFavorBalance = computed(() => {
  const receivedAmount = sum(audit.favorContacts.map((row) => row.receivedAmount));
  const givenAmount = sum(audit.favorContacts.map((row) => row.givenAmount));
  return receivedAmount - givenAmount;
});
const auditRows = computed(() => [
  {
    key: 'rsvp' as const,
    name: '回执',
    value: audit.rsvpStats ? `${audit.rsvpStats.totalRecords} 条 / ${audit.rsvpStats.totalGuests} 人` : '未核对',
    hint: '来自小程序请柬回执，返回统计页后会自动刷新。',
    ok: Boolean(audit.rsvpStats && audit.rsvpStats.totalRecords >= 0)
  },
  {
    key: 'gifts' as const,
    name: '收礼',
    value: `${audit.gifts.length} 笔 / ${formatMoney(auditGiftTotal.value)}`,
    hint: '线上随礼、现场扫码和线下记礼统一写入收礼记录。',
    ok: audit.gifts.length > 0
  },
  {
    key: 'favor' as const,
    name: '人情',
    value: `${audit.favorContacts.length} 个对象 / ${balanceText(auditFavorBalance.value)}`,
    hint: '收礼和手动补录会沉淀到人情账本，用于往来对比。',
    ok: audit.favorContacts.length > 0
  },
  {
    key: 'operationLogs' as const,
    name: '操作日志',
    value: `${audit.operationLogs.length} 条`,
    hint: '关键创建、回执、记礼、补录和配置动作应可追踪。',
    ok: audit.operationLogs.length > 0
  }
]);
const auditStatusText = computed(() => auditRows.value.every((row) => row.ok) ? '闭环完整' : '存在待补');
const auditStatusHint = computed(() => auditRows.value.every((row) => row.ok) ? '该宴席关键数据均可追踪' : '建议查看下方明细或操作日志补齐数据');
const auditStatusClass = computed(() => auditRows.value.every((row) => row.ok) ? 'positive' : 'negative');
const contextBanquetId = computed(() => auditBanquetId.value || giftFilters.banquetId || rsvpFilters.banquetId || favorExportBanquetId.value);
const contextBanquet = computed(() => banquetOptions.value.find((item) => String(item.id) === String(contextBanquetId.value)));
const contextBanquetTitle = computed(() => {
  const banquet = contextBanquet.value;
  return banquet ? `${banquet.id} · ${banquet.name}` : `宴席 ID ${contextBanquetId.value}`;
});

function tabFromQuery(value: unknown): BusinessTab {
  return value === 'rsvp' || value === 'favor' ? value : 'gifts';
}

function tabLabel(tab: BusinessTab) {
  const labels: Record<BusinessTab, string> = {
    gifts: '礼金记录',
    rsvp: '回执 RSVP',
    favor: '人情账本'
  };
  return labels[tab];
}

function query(params: Record<string, string>) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value) {
      search.set(key, value);
    }
  });
  return search.toString() ? `?${search}` : '';
}

async function loadGifts() {
  loading.gifts = true;
  try {
    const response = await http.get<ApiResponse<Record<string, unknown>[] | PageResult<Record<string, unknown>>>>(`/admin/gifts${query({ ...giftFilters, pageSize: '100' })}`);
    gifts.value = recordsOf(response.data.data);
    if (activeTab.value === 'gifts') {
      syncBusinessQuery('gifts');
    }
  } finally {
    loading.gifts = false;
  }
}

async function loadRsvp() {
  loading.rsvp = true;
  try {
    const response = await http.get<ApiResponse<Record<string, unknown>[] | PageResult<Record<string, unknown>>>>(`/admin/rsvp${query({ ...rsvpFilters, pageSize: '100' })}`);
    rsvpRows.value = recordsOf(response.data.data);
    if (activeTab.value === 'rsvp') {
      syncBusinessQuery('rsvp');
    }
  } finally {
    loading.rsvp = false;
  }
}

async function loadRsvpStats() {
  if (!rsvpFilters.banquetId) {
    return;
  }
  const response = await http.get<ApiResponse<RsvpStats>>(`/admin/rsvp/stats?banquetId=${encodeURIComponent(rsvpFilters.banquetId)}`);
  rsvpStats.value = response.data.data;
}

async function loadFavorContacts() {
  loading.favor = true;
  try {
    const response = await http.get<ApiResponse<Record<string, unknown>[]>>(`/admin/favor/contacts${query({
      keyword: favorKeyword.value,
      banquetId: favorExportBanquetId.value
    })}`);
    favorContacts.value = response.data.data || [];
    if (activeTab.value === 'favor') {
      syncBusinessQuery('favor');
    }
  } finally {
    loading.favor = false;
  }
}

async function loadAll() {
  await Promise.all([loadBanquetOptions(), loadGifts(), loadRsvp(), loadFavorContacts()]);
  if (auditBanquetId.value) {
    await loadAudit();
  }
}

async function loadBanquetOptions() {
  const response = await http.get<ApiResponse<BanquetOption[]>>('/admin/banquets');
  banquetOptions.value = response.data.data || [];
}

async function loadAudit() {
  if (!auditBanquetId.value) {
    ElMessage.warning('请先选择宴席');
    return;
  }
  loading.audit = true;
  try {
    const [rsvpResponse, giftResponse, favorResponse, operationLogResponse] = await Promise.all([
      http.get<ApiResponse<RsvpStats>>(`/admin/rsvp/stats?banquetId=${encodeURIComponent(auditBanquetId.value)}`),
      http.get<ApiResponse<Record<string, unknown>[] | PageResult<Record<string, unknown>>>>(`/admin/gifts?banquetId=${encodeURIComponent(auditBanquetId.value)}&pageSize=100`),
      http.get<ApiResponse<Record<string, unknown>[]>>(`/admin/favor/contacts?banquetId=${encodeURIComponent(auditBanquetId.value)}`),
      http.get<ApiResponse<Record<string, unknown>[] | PageResult<Record<string, unknown>>>>(`/admin/operation-logs?targetType=banquet&targetId=${encodeURIComponent(auditBanquetId.value)}&pageSize=100`)
    ]);
    audit.rsvpStats = rsvpResponse.data.data;
    audit.gifts = recordsOf(giftResponse.data.data);
    audit.favorContacts = favorResponse.data.data || [];
    audit.operationLogs = recordsOf(operationLogResponse.data.data);
  } finally {
    loading.audit = false;
  }
}

async function applyAuditFilters() {
  if (!auditBanquetId.value) {
    return;
  }
  giftFilters.banquetId = auditBanquetId.value;
  rsvpFilters.banquetId = auditBanquetId.value;
  favorExportBanquetId.value = auditBanquetId.value;
  await Promise.all([loadGifts(), loadRsvp(), loadRsvpStats(), loadFavorContacts()]);
  void router.replace({ path: '/business', query: { ...route.query, banquetId: auditBanquetId.value, tab: activeTab.value } });
  ElMessage.success('已套用宴席筛选');
}

async function focusAuditSection(tab: BusinessTab) {
  if (!auditBanquetId.value) {
    ElMessage.warning('请先选择宴席');
    return;
  }
  activeTab.value = tab;
  await applyAuditFilters();
  if (tab === 'gifts' && audit.gifts.length === 0) {
    offlineGiftForm.banquetId = auditBanquetId.value;
    offlineGiftForm.guestName = '';
    offlineGiftForm.amount = 100;
    offlineGiftForm.blessing = '';
    offlineGiftVisible.value = true;
  }
  if (tab === 'favor' && audit.favorContacts.length === 0) {
    favorForm.banquetId = auditBanquetId.value;
    favorManualVisible.value = true;
  }
  requestAnimationFrame(() => {
    document.querySelector('.el-tabs')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  });
}

async function handleAuditRow(key: AuditRowKey) {
  if (key === 'operationLogs') {
    await goAuditOperationLogs();
    return;
  }
  await focusAuditSection(key);
}

async function goAuditBanquet() {
  const banquetId = contextBanquetId.value;
  if (!banquetId) {
    return;
  }
  await router.push({ path: '/banquets', query: { banquetId, focus: 'overview' } });
}

async function goAuditOperationLogs() {
  const banquetId = contextBanquetId.value;
  if (!banquetId) {
    return;
  }
  await router.push({
    path: '/operation-logs',
    query: { targetType: 'banquet', targetId: banquetId }
  });
}

async function goAuditPayments() {
  const banquetId = contextBanquetId.value;
  if (!banquetId) {
    return;
  }
  await router.push({ path: '/payments', query: { banquetId } });
}

function resetGiftFilters() {
  giftFilters.banquetId = auditBanquetId.value;
  giftFilters.source = '';
  giftFilters.keyword = '';
  loadGifts();
}

function resetRsvpFilters() {
  rsvpFilters.banquetId = auditBanquetId.value;
  rsvpFilters.status = '';
  rsvpFilters.keyword = '';
  rsvpStats.value = null;
  loadRsvp();
}

function resetFavorFilters() {
  favorKeyword.value = '';
  favorExportBanquetId.value = auditBanquetId.value;
  loadFavorContacts();
}

function syncBusinessQuery(tab: BusinessTab) {
  const banquetId = tab === 'gifts'
    ? giftFilters.banquetId
    : tab === 'rsvp'
      ? rsvpFilters.banquetId
      : favorExportBanquetId.value;
  const nextQuery: Record<string, string> = { tab };
  if (banquetId) {
    nextQuery.banquetId = banquetId;
  }
  void router.replace({ path: '/business', query: nextQuery });
}

async function clearContext() {
  auditBanquetId.value = '';
  giftFilters.banquetId = '';
  rsvpFilters.banquetId = '';
  favorExportBanquetId.value = '';
  await Promise.all([loadGifts(), loadRsvp(), loadFavorContacts()]);
  void router.replace({ path: '/business', query: { tab: activeTab.value } });
}

async function downloadExport(kind: 'gifts' | 'rsvp' | 'favor', format: 'csv' | 'xlsx', banquetId: string) {
  if (!banquetId) {
    ElMessage.warning('请先填写宴席 ID');
    return;
  }
  const key = exportKey(kind, format);
  exportLoading[key] = true;
  try {
    const response = await http.get(`/admin/exports/banquets/${encodeURIComponent(banquetId)}/${kind}.${format}`, {
      responseType: 'blob'
    });
    const contentDisposition = String(response.headers['content-disposition'] || '');
    const matched = contentDisposition.match(/filename="?([^"]+)"?/i);
    const filename = matched?.[1] || `banquet-${banquetId}-${kind}.${format}`;
    const url = URL.createObjectURL(response.data as Blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = filename;
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    URL.revokeObjectURL(url);
    ElMessage.success('导出文件已生成');
  } catch (error) {
    ElMessage.error(await exportErrorMessage(error));
  } finally {
    exportLoading[key] = false;
  }
}

function exportKey(kind: 'gifts' | 'rsvp' | 'favor', format: 'csv' | 'xlsx') {
  return `${kind}:${format}`;
}

function isExporting(kind: 'gifts' | 'rsvp' | 'favor', format: 'csv' | 'xlsx') {
  return Boolean(exportLoading[exportKey(kind, format)]);
}

async function exportErrorMessage(error: unknown): Promise<string> {
  const responseData = (error as { response?: { data?: unknown } })?.response?.data;
  if (responseData instanceof Blob) {
    const text = await responseData.text();
    try {
      const body = JSON.parse(text) as { message?: string };
      return body.message || '导出失败';
    } catch {
      return text || '导出失败';
    }
  }
  if (error instanceof Error) {
    return error.message || '导出失败';
  }
  return '导出失败';
}

function openOfflineGift() {
  offlineGiftForm.banquetId = giftFilters.banquetId;
  offlineGiftForm.guestName = '';
  offlineGiftForm.amount = 100;
  offlineGiftForm.blessing = '';
  offlineGiftVisible.value = true;
}

async function submitOfflineGift() {
  if (!offlineGiftForm.banquetId || !offlineGiftForm.guestName) {
    ElMessage.warning('请填写宴席 ID 和来宾姓名');
    return;
  }
  if (!Number(offlineGiftForm.banquetId) || Number(offlineGiftForm.banquetId) <= 0) {
    ElMessage.warning('请输入有效宴席 ID');
    return;
  }
  if (!Number(offlineGiftForm.amount) || Number(offlineGiftForm.amount) <= 0) {
    ElMessage.warning('请输入有效金额');
    return;
  }
  await http.post('/admin/gifts/offline', {
    banquetId: Number(offlineGiftForm.banquetId),
    guestName: offlineGiftForm.guestName,
    amount: offlineGiftForm.amount,
    blessing: offlineGiftForm.blessing
  });
  ElMessage.success('现金记礼已保存');
  offlineGiftVisible.value = false;
  await Promise.all([loadGifts(), loadFavorContacts()]);
}

function openFavorManual() {
  favorForm.contactName = '';
  favorForm.phone = '';
  favorForm.direction = 'RECEIVED';
  favorForm.banquetId = favorExportBanquetId.value;
  favorForm.amount = 100;
  favorForm.note = '';
  favorManualVisible.value = true;
}

async function submitFavorManual() {
  if (!favorForm.contactName) {
    ElMessage.warning('请填写联系人');
    return;
  }
  if (!Number(favorForm.amount) || Number(favorForm.amount) <= 0) {
    ElMessage.warning('请输入有效金额');
    return;
  }
  if (favorForm.banquetId && (!Number(favorForm.banquetId) || Number(favorForm.banquetId) <= 0)) {
    ElMessage.warning('请输入有效宴席 ID');
    return;
  }
  await http.post('/admin/favor/manual', {
    contactName: favorForm.contactName,
    phone: favorForm.phone || undefined,
    banquetId: favorForm.banquetId ? Number(favorForm.banquetId) : undefined,
    direction: favorForm.direction,
    amount: favorForm.amount,
    note: favorForm.note
  });
  ElMessage.success('人情补录已保存');
  favorManualVisible.value = false;
  await loadFavorContacts();
}

async function openFavorDetail(contactId: number) {
  const response = await http.get<ApiResponse<FavorDetail>>(`/admin/favor/contacts/${contactId}`);
  favorDetail.value = response.data.data;
  favorDetailVisible.value = true;
}

async function goBanquet(banquetId: number) {
  await router.push({ path: '/banquets', query: { banquetId, focus: 'overview' } });
}

function toNumber(value: unknown): number {
  return Number(value || 0);
}

function sum(values: unknown[]): number {
  return values.reduce((total: number, value) => total + toNumber(value), 0);
}

function balanceText(value: unknown) {
  const amount = toNumber(value);
  if (amount > 0) {
    return '对方累计送入更多';
  }
  if (amount < 0) {
    return '我方累计送出更多';
  }
  return '双方往来持平';
}

function balanceClass(value: unknown) {
  const amount = toNumber(value);
  if (amount > 0) {
    return 'positive';
  }
  if (amount < 0) {
    return 'negative';
  }
  return 'neutral';
}

onMounted(loadAll);

watch(activeTab, (tab) => {
  syncBusinessQuery(tab);
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

.export-notice {
  margin-bottom: 14px;
}

.context-panel {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 14px;
  padding: 14px;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  background: #eff6ff;
}

.context-panel span {
  color: #2563eb;
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

.audit-panel {
  margin-bottom: 16px;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
}

.audit-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.audit-head h2 {
  margin: 0;
  color: #111827;
  font-size: 18px;
}

.audit-head p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
}

.audit-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.audit-select {
  width: 260px;
}

.audit-metrics {
  margin-bottom: 14px;
}

.audit-quick-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
  padding: 10px;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  background: #f8fafc;
}

h1 {
  margin: 0;
  font-size: 20px;
}

.toolbar {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(136px, max-content));
  gap: 10px;
  margin-bottom: 14px;
}

.toolbar .el-input,
.toolbar .el-select {
  width: 150px;
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

.metric span,
.metric small {
  color: #64748b;
  font-size: 12px;
}

.metric strong {
  color: #111827;
  font-size: 20px;
  line-height: 1.2;
}

.summary,
.detail-summary {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 14px;
  color: #374151;
}

.detail-summary {
  align-items: baseline;
}

.detail-summary h2 {
  margin: 0;
  font-size: 18px;
}

.positive {
  color: #b91c1c !important;
}

.negative {
  color: #2563eb !important;
}

.neutral {
  color: #64748b !important;
}

@media (max-width: 860px) {
  .toolbar {
    grid-template-columns: 1fr;
  }

  .toolbar .el-input,
  .toolbar .el-select,
  .audit-select {
    width: 100%;
  }

  .context-panel,
  .audit-head,
  .audit-actions {
    display: grid;
    grid-template-columns: 1fr;
  }

  .context-actions {
    justify-content: flex-start;
  }
}
</style>
