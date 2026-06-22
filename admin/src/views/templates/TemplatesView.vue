<template>
  <div>
    <section class="template-preview">
      <header class="preview-header">
        <div>
          <h1>模板预览</h1>
          <p>运营可快速检查封面、类型、价格与上下架状态</p>
        </div>
        <el-button @click="loadTemplates">刷新预览</el-button>
      </header>
      <div class="template-grid">
        <article v-for="item in templates" :key="item.id" class="template-card">
          <div class="cover" :style="coverStyle(item)">
            <img v-if="item.coverUrl" :src="item.coverUrl" :alt="item.name" />
            <span v-else>{{ item.name.slice(0, 4) }}</span>
          </div>
          <div class="card-body">
            <div class="card-title">
              <strong>{{ item.name }}</strong>
              <el-tag :type="tagType(item.status)" size="small">{{ displayLabel(item.status) }}</el-tag>
            </div>
            <p>{{ item.templateCode }}</p>
            <div class="card-meta">
              <el-tag size="small">{{ displayLabel(item.typeCode) }}</el-tag>
              <el-tag size="small" :type="item.priceType === 'FREE' ? 'success' : 'warning'">
                {{ item.priceType === 'FREE' ? '免费' : formatMoney(item.price) }}
              </el-tag>
            </div>
            <el-button class="preview-button" plain @click="openPreview(item)">预览</el-button>
          </div>
        </article>
      </div>
    </section>
    <ResourceTable
      title="模板类型"
      endpoint="/admin/template-types"
      :fields="typeFields"
      :defaults="{ enabled: 1, sortOrder: 0 }"
    />
    <ResourceTable
      title="请柬模板"
      endpoint="/admin/invitation-templates"
      :fields="templateFields"
      :defaults="{ priceType: 'FREE', price: 0, sortOrder: 0, status: 'ACTIVE' }"
    />
    <el-dialog v-model="previewVisible" title="模板公开页预览" width="420px">
      <article v-if="previewTemplate" class="phone-preview" :class="previewClass(previewTemplate)">
        <div class="phone-cover" :style="coverStyle(previewTemplate)">
          <img v-if="previewTemplate.coverUrl" :src="previewTemplate.coverUrl" :alt="previewTemplate.name" />
          <strong v-else>{{ previewTemplate.name }}</strong>
        </div>
        <div class="phone-body">
          <span>{{ previewTemplate.name }}</span>
          <h2>{{ previewPresentation(previewTemplate).headline }}</h2>
          <p>{{ previewPresentation(previewTemplate).greeting }}</p>
          <div class="phone-row"><b>主办人</b><em>陈先生 & 林女士</em></div>
          <div class="phone-row"><b>宴席时间</b><em>2026-10-01 18:00</em></div>
          <div class="phone-row"><b>宴席地点</b><em>演示酒店 宴会厅A</em></div>
          <div class="phone-timeline">
            <i v-for="item in previewPresentation(previewTemplate).schedule" :key="item">{{ item }}</i>
          </div>
          <div class="phone-actions">
            <button>填写回执</button>
            <button>线上随礼</button>
          </div>
        </div>
      </article>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { http, type ApiResponse } from '../../api/client';
import ResourceTable, { type ResourceField } from '../../components/ResourceTable.vue';
import { displayLabel, formatMoney, tagType } from '../../utils/display';

interface InvitationTemplate {
  id: number;
  templateCode: string;
  typeCode: string;
  name: string;
  coverUrl?: string;
  priceType: string;
  price: number;
  status: string;
}

const templates = ref<InvitationTemplate[]>([]);
const previewVisible = ref(false);
const previewTemplate = ref<InvitationTemplate | null>(null);

async function loadTemplates() {
  const response = await http.get<ApiResponse<InvitationTemplate[]>>('/admin/invitation-templates');
  templates.value = response.data.data || [];
}

function coverStyle(item: InvitationTemplate) {
  const palettes: Record<string, string> = {
    FREE: 'linear-gradient(135deg, #b91c1c, #facc15)',
    PAID: 'linear-gradient(135deg, #111827, #b91c1c)',
    CUSTOM: 'linear-gradient(135deg, #0f766e, #f59e0b)'
  };
  return { background: palettes[item.typeCode] || palettes.FREE };
}

function openPreview(item: InvitationTemplate) {
  previewTemplate.value = item;
  previewVisible.value = true;
}

function previewClass(item: InvitationTemplate) {
  return `preview-${previewPresentation(item).style}`;
}

function previewPresentation(item: InvitationTemplate) {
  if (item.templateCode.includes('WEDDING')) {
    return {
      style: 'wedding',
      headline: '良辰喜宴',
      greeting: '诚邀您拨冗赴宴，共同见证这份喜悦。',
      schedule: ['17:30 来宾签到', '18:00 仪式开始', '18:30 喜宴开席']
    };
  }
  if (item.templateCode.includes('BIRTHDAY')) {
    return {
      style: 'birthday',
      headline: '福寿喜宴',
      greeting: '诚邀亲友同聚一堂，共祝福寿安康。',
      schedule: ['17:30 来宾签到', '18:00 寿宴仪式', '18:30 宴席开席']
    };
  }
  if (item.templateCode.includes('BABY')) {
    return {
      style: 'baby',
      headline: '满月之喜',
      greeting: '诚邀您一同分享宝宝成长的温暖时刻。',
      schedule: ['11:00 来宾签到', '11:30 满月仪式', '12:00 午宴开席']
    };
  }
  if (item.templateCode.includes('HOUSE')) {
    return {
      style: 'house',
      headline: '乔迁雅宴',
      greeting: '新居落成，诚邀您莅临相聚，共叙情谊。',
      schedule: ['17:30 来宾签到', '18:00 乔迁仪式', '18:30 晚宴开席']
    };
  }
  if (item.templateCode.includes('SCHOOL')) {
    return {
      style: 'school',
      headline: '升学答谢',
      greeting: '感谢一路关怀与陪伴，诚邀您共赴升学答谢宴。',
      schedule: ['17:30 来宾签到', '18:00 答谢致辞', '18:30 宴席开席']
    };
  }
  if (item.templateCode.includes('MEMORIAL')) {
    return {
      style: 'memorial',
      headline: '追思相聚',
      greeting: '谨以素心相邀，共同缅怀与追忆。',
      schedule: ['09:30 来宾签到', '10:00 追思仪式', '11:00 礼成送别']
    };
  }
  return {
    style: item.typeCode === 'PAID' || item.typeCode === 'CUSTOM' ? 'premium' : 'general',
    headline: '诚挚邀请',
    greeting: '诚邀您拨冗赴宴，共同见证这份重要时刻。',
    schedule: ['17:30 来宾签到', '18:00 宴席开始', '18:30 宾主同欢']
  };
}

onMounted(loadTemplates);

const typeFields: ResourceField[] = [
  { prop: 'id', label: 'ID', readonly: true },
  { prop: 'typeCode', label: '类型编码', required: true },
  { prop: 'name', label: '名称', required: true },
  { prop: 'sortOrder', label: '排序', type: 'number' },
  { prop: 'enabled', label: '启用', type: 'boolean' }
];

const templateFields: ResourceField[] = [
  { prop: 'id', label: 'ID', readonly: true },
  { prop: 'templateCode', label: '模板编码', required: true },
  { prop: 'typeCode', label: '类型编码', type: 'select', required: true, options: [
    { label: '免费模板', value: 'FREE' },
    { label: '收费模板', value: 'PAID' },
    { label: '定制模板', value: 'CUSTOM' }
  ] },
  { prop: 'name', label: '名称', required: true },
  { prop: 'coverUrl', label: '封面URL' },
  { prop: 'priceType', label: '价格类型', type: 'select', required: true, options: [
    { label: '免费', value: 'FREE' },
    { label: '付费', value: 'PAID' },
    { label: '权益包含', value: 'PLAN_INCLUDED' }
  ] },
  { prop: 'price', label: '价格', type: 'money' },
  { prop: 'sortOrder', label: '排序', type: 'number' },
  { prop: 'status', label: '状态', type: 'select', required: true, options: [
    { label: '上架', value: 'ACTIVE' },
    { label: '下架', value: 'INACTIVE' },
    { label: '草稿', value: 'DRAFT' }
  ] }
];
</script>

<style scoped>
.template-preview {
  min-height: 0;
  padding: 24px;
  background: #f6f7f9;
}

.preview-header {
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

p {
  margin: 6px 0 0;
  color: #64748b;
}

.template-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 14px;
}

.template-card {
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.cover {
  height: 132px;
  display: grid;
  place-items: center;
  color: #fff;
  font-size: 22px;
  font-weight: 700;
}

.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.card-body {
  padding: 14px;
}

.card-title,
.card-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.card-title strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-body p {
  margin: 8px 0 12px;
  font-size: 13px;
}

.preview-button {
  width: 100%;
  margin-top: 12px;
}

.phone-preview {
  overflow: hidden;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.phone-cover {
  height: 180px;
  display: grid;
  place-items: center;
  color: #fff;
  text-align: center;
}

.phone-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.phone-body {
  padding: 18px;
}

.phone-body span {
  color: #64748b;
  font-size: 13px;
}

.phone-body h2 {
  margin: 8px 0;
  font-size: 24px;
}

.phone-body p {
  margin: 0 0 14px;
  line-height: 1.6;
}

.phone-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 0;
  border-top: 1px solid #eef2f7;
}

.phone-row b {
  color: #64748b;
  font-weight: 500;
}

.phone-row em {
  color: #111827;
  font-style: normal;
  text-align: right;
}

.phone-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-top: 16px;
}

.phone-actions button {
  height: 36px;
  border: 0;
  border-radius: 6px;
  background: #b91c1c;
  color: #fff;
}

.phone-timeline {
  display: grid;
  gap: 6px;
  margin-top: 12px;
  padding: 12px;
  border-radius: 6px;
  background: #f8fafc;
}

.phone-timeline i {
  color: #374151;
  font-style: normal;
  font-size: 13px;
}

.preview-wedding .phone-actions button,
.preview-birthday .phone-actions button {
  background: #b91c1c;
}

.preview-baby .phone-actions button {
  background: #0f766e;
}

.preview-house .phone-actions button {
  background: #ea580c;
}

.preview-school .phone-actions button {
  background: #1d4ed8;
}

.preview-memorial .phone-actions button {
  background: #111827;
}

.preview-premium .phone-actions button,
.preview-general .phone-actions button:last-child {
  background: #334155;
}
</style>
