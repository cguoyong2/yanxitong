<template>
  <ResourceTable
    title="主题文案配置"
    endpoint="/admin/theme-copywriting"
    :fields="fields"
    :defaults="{ sceneCode: 'GIFT_SUCCESS', enabled: 1 }"
  />
</template>

<script setup lang="ts">
import ResourceTable, { type ResourceField } from '../../components/ResourceTable.vue';

const fields: ResourceField[] = [
  { prop: 'id', label: 'ID', readonly: true },
  { prop: 'themeCode', label: '主题编码', required: true },
  { prop: 'eventTypeCode', label: '宴席类型', type: 'select', required: true, options: [
    { label: '婚宴', value: 'WEDDING' },
    { label: '寿宴', value: 'BIRTHDAY' },
    { label: '满月', value: 'BABY' },
    { label: '乔迁', value: 'HOUSEWARMING' },
    { label: '升学', value: 'SCHOOL' },
    { label: '追思会', value: 'MEMORIAL' },
    { label: '其他', value: 'OTHER' }
  ] },
  { prop: 'sceneCode', label: '场景编码', type: 'select', required: true, options: [
    { label: '礼金成功', value: 'GIFT_SUCCESS' }
  ] },
  { prop: 'title', label: '标题' },
  { prop: 'content', label: '页面文案', type: 'textarea' },
  { prop: 'speakerText', label: '播报文案', type: 'textarea' },
  { prop: 'enabled', label: '启用', type: 'boolean' }
];
</script>
