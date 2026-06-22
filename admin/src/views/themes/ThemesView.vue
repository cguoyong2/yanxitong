<template>
  <ResourceTable
    title="主题配置"
    endpoint="/admin/themes"
    :fields="fields"
    :defaults="{ enabled: 1 }"
  />
</template>

<script setup lang="ts">
import ResourceTable, { type ResourceField } from '../../components/ResourceTable.vue';

const fields: ResourceField[] = [
  { prop: 'id', label: 'ID', readonly: true },
  { prop: 'themeCode', label: '主题编码', required: true },
  { prop: 'name', label: '名称', required: true },
  { prop: 'primaryColor', label: '主色', type: 'color', required: true },
  { prop: 'secondaryColor', label: '辅色', type: 'color' },
  { prop: 'iconStyle', label: '图标风格', type: 'select', options: [
    { label: '喜庆', value: 'celebration' },
    { label: '寿宴', value: 'birthday' },
    { label: '家庭', value: 'family' },
    { label: '家宅', value: 'home' },
    { label: '书卷', value: 'school' },
    { label: '追思', value: 'memorial' },
    { label: '通用', value: 'general' }
  ] },
  { prop: 'confirmScreenTemplate', label: '确认屏模板', type: 'select', options: [
    { label: '默认成功页', value: 'default_success' }
  ] },
  { prop: 'enabled', label: '启用', type: 'boolean' }
];
</script>
