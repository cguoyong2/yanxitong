<script setup lang="ts">
import { Download, Delete, Edit, Plus, Refresh } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { onMounted, reactive, ref } from 'vue';
import { http, type ApiResponse } from '../api/client';
import { displayLabel, formatDateTime, formatMoney, tagType } from '../utils/display';

export interface ResourceField {
  prop: string;
  label: string;
  type?: 'text' | 'number' | 'money' | 'boolean' | 'textarea' | 'select' | 'color';
  options?: { label: string; value: string | number }[];
  readonly?: boolean;
  required?: boolean;
}

const props = defineProps<{
  title: string;
  endpoint: string;
  fields: ResourceField[];
  defaults?: Record<string, unknown>;
  readonly?: boolean;
  exportReserved?: boolean;
}>();

const rows = ref<Record<string, unknown>[]>([]);
const loading = ref(false);
const dialogVisible = ref(false);
const form = reactive<Record<string, unknown>>({});

const editableFields = () => props.fields.filter((field) => !field.readonly && field.prop !== 'id');

async function load() {
  loading.value = true;
  try {
    const response = await http.get<ApiResponse<Record<string, unknown>[]>>(props.endpoint);
    rows.value = response.data.data || [];
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  Object.keys(form).forEach((key) => delete form[key]);
  Object.assign(form, props.defaults || {});
  dialogVisible.value = true;
}

function openEdit(row: Record<string, unknown>) {
  Object.keys(form).forEach((key) => delete form[key]);
  Object.assign(form, row);
  dialogVisible.value = true;
}

async function save() {
  for (const field of editableFields()) {
    const value = form[field.prop];
    if (field.required && (value === undefined || value === null || value === '')) {
      ElMessage.warning(`请填写${field.label}`);
      return;
    }
  }
  await http.post(props.endpoint, form);
  ElMessage.success('已保存');
  dialogVisible.value = false;
  await load();
}

async function quickPatch(row: Record<string, unknown>, patch: Record<string, unknown>, message: string) {
  await http.post(props.endpoint, { ...row, ...patch });
  ElMessage.success(message);
  await load();
}

async function remove(row: Record<string, unknown>) {
  await ElMessageBox.confirm('确认删除这条配置？', '删除确认', { type: 'warning' });
  await http.delete(`${props.endpoint}/${row.id}`);
  ElMessage.success('已删除');
  await load();
}

function showExportReserved() {
  ElMessage.info('业务数据 CSV/XLSX 导出已在业务数据页提供，此处仅保留配置列表入口提示');
}

function statusType(value: unknown) {
  return tagType(value);
}

function displayValue(row: Record<string, unknown>, field: ResourceField) {
  const value = row[field.prop];
  if (field.prop === 'detail') {
    return formatDetailValue(value);
  }
  if (field.type === 'boolean') {
    return value === 1 ? '启用' : '停用';
  }
  if (field.type === 'money') {
    return formatMoney(value);
  }
  if (field.prop.endsWith('At') || field.prop.endsWith('Time')) {
    return formatDateTime(value);
  }
  const option = field.options?.find((item) => item.value === value);
  return option?.label ?? displayLabel(value);
}

function formatDetailValue(value: unknown): string {
  if (!value) {
    return '-';
  }
  try {
    const parsed = typeof value === 'string' ? JSON.parse(value) : value;
    return JSON.stringify(translateDetailValue(parsed));
  } catch {
    return displayLabel(value);
  }
}

function translateDetailValue(value: unknown): unknown {
  if (Array.isArray(value)) {
    return value.map(translateDetailValue);
  }
  if (value && typeof value === 'object') {
    return Object.fromEntries(Object.entries(value as Record<string, unknown>).map(([key, item]) => [key, translateDetailValue(item)]));
  }
  if (typeof value === 'string') {
    return displayLabel(value);
  }
  return value;
}

onMounted(load);
</script>

<template>
  <section class="resource-page">
    <header class="toolbar">
      <h1>{{ title }}</h1>
      <div class="actions">
        <el-button v-if="exportReserved" :icon="Download" @click="showExportReserved">导出</el-button>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button v-if="!readonly" type="primary" :icon="Plus" @click="openCreate">新增</el-button>
      </div>
    </header>

    <el-table v-loading="loading" :data="rows" border stripe empty-text="暂无数据">
      <el-table-column v-for="field in fields" :key="field.prop" :prop="field.prop" :label="field.label" min-width="140">
        <template #default="{ row }">
          <el-tag v-if="field.type === 'boolean' || field.prop === 'status' || field.prop.endsWith('Status')" :type="statusType(row[field.prop])">
            {{ displayValue(row, field) }}
          </el-tag>
          <span v-else-if="field.type === 'color'" class="color-cell">
            <span class="swatch" :style="{ background: String(row[field.prop] || '#fff') }"></span>
            {{ row[field.prop] }}
          </span>
          <span v-else>{{ displayValue(row, field) }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="!readonly" label="操作" width="250" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="Edit" @click="openEdit(row)">编辑</el-button>
          <el-button
            v-if="'enabled' in row"
            link
            :type="row.enabled === 1 ? 'warning' : 'success'"
            @click="quickPatch(row, { enabled: row.enabled === 1 ? 0 : 1 }, row.enabled === 1 ? '已停用' : '已启用')"
          >
            {{ row.enabled === 1 ? '停用' : '启用' }}
          </el-button>
          <el-button
            v-if="'status' in row"
            link
            :type="row.status === 'ACTIVE' ? 'warning' : 'success'"
            @click="quickPatch(row, { status: row.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE' }, row.status === 'ACTIVE' ? '已下架' : '已上架')"
          >
            {{ row.status === 'ACTIVE' ? '下架' : '上架' }}
          </el-button>
          <el-button link type="danger" :icon="Delete" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="`${title}配置`" width="640px">
      <el-form label-width="120px">
        <el-form-item v-for="field in editableFields()" :key="field.prop" :label="field.label">
          <el-switch v-if="field.type === 'boolean'" v-model="form[field.prop]" :active-value="1" :inactive-value="0" />
          <el-select v-else-if="field.type === 'select'" v-model="form[field.prop]" filterable clearable>
            <el-option v-for="option in field.options || []" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
          <el-color-picker v-else-if="field.type === 'color'" v-model="form[field.prop] as string" />
          <el-input-number v-else-if="field.type === 'number' || field.type === 'money'" v-model="form[field.prop] as number" :precision="field.type === 'money' ? 2 : 0" />
          <el-input v-else-if="field.type === 'textarea'" v-model="form[field.prop] as string" type="textarea" :rows="4" />
          <el-input v-else v-model="form[field.prop] as string" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.resource-page {
  min-height: 100vh;
  padding: 24px;
  background: #f6f7f9;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.actions {
  display: flex;
  gap: 8px;
}

.color-cell {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.swatch {
  width: 18px;
  height: 18px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}
</style>
