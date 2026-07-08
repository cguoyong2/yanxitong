<template>
  <div>
    <el-alert
      class="plan-tip"
      title="版本价格说明"
      type="info"
      show-icon
      :closable="false"
      description="这里维护的版本名称、价格、单位、上下架和排序会实时影响小程序选择版本页；用户创建版本订单时，订单金额会按当时后台价格写入订单快照。"
    />
    <ResourceTable
      title="版本配置"
      endpoint="/admin/plans"
      :fields="planFields"
      :defaults="{ price: 0, recommended: 0, sortOrder: 0, status: 'ACTIVE' }"
      export-reserved
    />
    <ResourceTable
      title="版本权益配置"
      endpoint="/admin/plan-rights"
      :fields="rightFields"
    />
  </div>
</template>

<script setup lang="ts">
import ResourceTable, { type ResourceField } from '../../components/ResourceTable.vue';

const planFields: ResourceField[] = [
  { prop: 'id', label: 'ID', readonly: true },
  { prop: 'planCode', label: '版本编码', required: true },
  { prop: 'name', label: '版本名称', required: true },
  { prop: 'price', label: '价格', type: 'money', required: true },
  { prop: 'priceUnit', label: '单位', type: 'select', required: true, options: [
    { label: '场', value: '场' },
    { label: '月', value: '月' },
    { label: '年', value: '年' }
  ] },
  { prop: 'recommended', label: '推荐', type: 'boolean' },
  { prop: 'sortOrder', label: '排序', type: 'number' },
  { prop: 'status', label: '状态', type: 'select', required: true, options: [
    { label: '上架', value: 'ACTIVE' },
    { label: '下架', value: 'INACTIVE' },
    { label: '草稿', value: 'DRAFT' }
  ] }
];

const rightFields: ResourceField[] = [
  { prop: 'id', label: 'ID', readonly: true },
  { prop: 'planId', label: '版本ID', type: 'number', required: true },
  { prop: 'rightCode', label: '权益编码', required: true },
  { prop: 'rightName', label: '权益名称', required: true },
  { prop: 'rightValue', label: '权益值' }
];
</script>

<style scoped>
.plan-tip {
  margin-bottom: 16px;
}
</style>
