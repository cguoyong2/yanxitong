<template>
  <ResourceTable
    title="宴席类型配置"
    endpoint="/admin/event-types"
    :fields="fields"
    :defaults="{ enabled: 1, sortOrder: 0 }"
  />
</template>

<script setup lang="ts">
import ResourceTable, { type ResourceField } from '../../components/ResourceTable.vue';

const fields: ResourceField[] = [
  { prop: 'id', label: 'ID', readonly: true },
  { prop: 'eventTypeCode', label: '类型编码', required: true },
  { prop: 'name', label: '名称', required: true },
  { prop: 'alias', label: '别名' },
  { prop: 'defaultThemeCode', label: '默认主题', type: 'select', required: true, options: [
    { label: '红金喜庆', value: 'wedding_red_gold' },
    { label: '深红暖金', value: 'birthday_warm_gold' },
    { label: '暖橙温馨', value: 'baby_warm_orange' },
    { label: '橙金家宅', value: 'house_orange_gold' },
    { label: '蓝金书卷', value: 'school_blue_gold' },
    { label: '灰黑素雅', value: 'memorial_gray_black' },
    { label: '中性暖色', value: 'neutral_warm' }
  ] },
  { prop: 'defaultCopywriting', label: '默认文案', type: 'textarea' },
  { prop: 'sortOrder', label: '排序', type: 'number' },
  { prop: 'enabled', label: '启用', type: 'boolean' }
];
</script>
