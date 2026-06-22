<template>
  <ResourceTable
    title="设备配置"
    endpoint="/admin/device-configs"
    :fields="fields"
    :defaults="{ price: 0, priceUnit: '场', enabled: 1 }"
  />
</template>

<script setup lang="ts">
import ResourceTable, { type ResourceField } from '../../components/ResourceTable.vue';

const fields: ResourceField[] = [
  { prop: 'id', label: 'ID', readonly: true },
  { prop: 'deviceType', label: '设备类型', type: 'select', required: true, options: [
    { label: '云喇叭', value: 'CLOUD_SPEAKER' },
    { label: '收礼确认屏', value: 'CONFIRM_SCREEN' }
  ] },
  { prop: 'name', label: '名称', required: true },
  { prop: 'price', label: '价格', type: 'money', required: true },
  { prop: 'priceUnit', label: '单位', type: 'select', required: true, options: [
    { label: '场', value: '场' },
    { label: '天', value: '天' }
  ] },
  { prop: 'deliveryMethod', label: '交付方式', type: 'select', required: true, options: [
    { label: '同城配送', value: '同城配送' },
    { label: '现场自提', value: '现场自提' },
    { label: '快递寄送', value: '快递寄送' }
  ] },
  { prop: 'enabled', label: '启用', type: 'boolean' }
];
</script>
