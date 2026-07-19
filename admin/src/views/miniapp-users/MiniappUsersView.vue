<template>
  <main class="page">
    <header class="page-header">
      <div>
        <p class="eyebrow">Identity & Ownership</p>
        <h1>小程序用户</h1>
        <p>查看微信登录用户，并将升级前没有所有者的数据安全迁移到指定账号。</p>
      </div>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </header>

    <el-alert
      title="历史数据迁移只需执行一次"
      description="请先用实际主账号进入小程序，再选择该账号执行迁移。系统只处理尚未归属的数据，不会覆盖已有所有者。"
      type="warning"
      show-icon
      :closable="false"
    />

    <section class="metrics">
      <article>
        <span>用户总数</span>
        <strong>{{ rows.length }}</strong>
      </article>
      <article>
        <span>正常账号</span>
        <strong>{{ activeCount }}</strong>
      </article>
      <article>
        <span>最近登录</span>
        <strong class="date-value">{{ formatDateTime(rows[0]?.lastLoginAt) }}</strong>
      </article>
    </section>

    <el-table v-loading="loading" :data="rows" border stripe empty-text="暂无小程序登录用户">
      <el-table-column prop="id" label="用户 ID" width="100" />
      <el-table-column label="微信标识" min-width="220">
        <template #default="{ row }">{{ maskOpenId(row.openId) }}</template>
      </el-table-column>
      <el-table-column label="用户资料" min-width="180">
        <template #default="{ row }">
          <strong>{{ row.nickname || '未授权昵称' }}</strong>
          <small class="secondary">{{ row.phone || '未绑定手机号' }}</small>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'">{{ row.status === 'ACTIVE' ? '正常' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="最近登录" min-width="180">
        <template #default="{ row }">{{ formatDateTime(row.lastLoginAt) }}</template>
      </el-table-column>
      <el-table-column label="创建时间" min-width="180">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="数据归属" width="170" fixed="right">
        <template #default="{ row }">
          <el-button
            type="primary"
            plain
            :disabled="row.status !== 'ACTIVE'"
            :loading="claimingUserId === row.id"
            @click="claimLegacy(row)"
          >接管历史数据</el-button>
        </template>
      </el-table-column>
    </el-table>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { http, type ApiResponse } from '../../api/client';
import { formatDateTime } from '../../utils/display';

interface MiniappUser {
  id: number;
  openId: string;
  nickname?: string;
  phone?: string;
  status: string;
  lastLoginAt?: string;
  createdAt?: string;
}

interface ClaimResult {
  banquetCount: number;
  banquetMemberCount: number;
  favorContactCount: number;
  familyBookCount: number;
}

const rows = ref<MiniappUser[]>([]);
const loading = ref(false);
const claimingUserId = ref<number>();
const activeCount = computed(() => rows.value.filter((item) => item.status === 'ACTIVE').length);

async function load() {
  loading.value = true;
  try {
    const response = await http.get<ApiResponse<MiniappUser[]>>('/admin/miniapp-users');
    rows.value = response.data.data || [];
  } finally {
    loading.value = false;
  }
}

async function claimLegacy(user: MiniappUser) {
  await ElMessageBox.confirm(
    `确认将尚未归属的宴席、人情联系人和家庭账本迁移给用户 ${user.id}？`,
    '确认历史数据归属',
    { type: 'warning', confirmButtonText: '确认迁移', cancelButtonText: '取消' }
  );
  claimingUserId.value = user.id;
  try {
    const response = await http.post<ApiResponse<ClaimResult>>(`/admin/miniapp-users/${user.id}/claim-legacy`);
    const result = response.data.data;
    ElMessage.success(`迁移完成：宴席 ${result.banquetCount}，联系人 ${result.favorContactCount}，家庭账本 ${result.familyBookCount}`);
  } finally {
    claimingUserId.value = undefined;
  }
}

function maskOpenId(value?: string) {
  if (!value || value.length < 12) {
    return value || '-';
  }
  return `${value.slice(0, 6)}...${value.slice(-6)}`;
}

onMounted(load);
</script>

<style scoped>
.page {
  display: grid;
  gap: 18px;
}

.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
}

.page-header h1,
.page-header p {
  margin: 0;
}

.page-header h1 {
  margin-top: 4px;
  font-size: 28px;
}

.page-header p:last-child {
  margin-top: 8px;
  color: #64748b;
}

.eyebrow {
  color: #991b1b;
  font-size: 12px;
  font-weight: 800;
  text-transform: uppercase;
}

.metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.metrics article {
  display: grid;
  gap: 8px;
  padding: 18px;
  border: 1px solid #dbe3ee;
  border-radius: 8px;
  background: #fff;
}

.metrics span,
.secondary {
  display: block;
  color: #64748b;
}

.metrics strong {
  font-size: 28px;
}

.metrics .date-value {
  font-size: 18px;
}

@media (max-width: 860px) {
  .metrics {
    grid-template-columns: 1fr;
  }
}
</style>
