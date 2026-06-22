<template>
  <ResourceTable
    title="配置中心"
    endpoint="/admin/config-items"
    :fields="fields"
    :defaults="{ valueType: 'STRING', enabled: 1 }"
    export-reserved
  />
</template>

<script setup lang="ts">
import ResourceTable, { type ResourceField } from '../../components/ResourceTable.vue';

const fields: ResourceField[] = [
  { prop: 'id', label: 'ID', readonly: true },
  { prop: 'configKey', label: '配置键', required: true },
  { prop: 'configValue', label: '配置值', type: 'textarea' },
  { prop: 'valueType', label: '值类型', type: 'select', required: true, options: [
    { label: '文本', value: 'STRING' },
    { label: '数字', value: 'NUMBER' },
    { label: '布尔', value: 'BOOLEAN' },
    { label: 'JSON', value: 'JSON' }
  ] },
  { prop: 'description', label: '说明', type: 'textarea' },
  { prop: 'enabled', label: '启用', type: 'boolean' }
];
</script>
