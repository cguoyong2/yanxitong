<template>
  <div class="plans-page">
    <section class="plan-hero">
      <div>
        <p class="eyebrow">版本权益</p>
        <h1>价格、订单与支付入口统一管理</h1>
        <p class="hero-desc">
          小程序选择版本页直接读取这里的版本价格。用户创建订单后，金额会写入订单快照，再进入微信支付或支付排障闭环。
        </p>
      </div>
      <div class="hero-actions">
        <RouterLink class="hero-button primary" :to="{ path: '/orders', query: { tab: 'plans' } }">查看版本订单</RouterLink>
        <RouterLink class="hero-button" :to="{ path: '/payments', query: { tab: 'orders' } }">支付管理</RouterLink>
      </div>
    </section>

    <section class="flow-grid">
      <article v-for="item in flowItems" :key="item.title" class="flow-card">
        <span>{{ item.step }}</span>
        <h3>{{ item.title }}</h3>
        <p>{{ item.desc }}</p>
      </article>
    </section>

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

const flowItems = [
  {
    step: '01',
    title: '后台价格源',
    desc: '版本名称、价格、单位、推荐和上下架由这里维护，小程序实时读取。'
  },
  {
    step: '02',
    title: '订单金额快照',
    desc: '用户下单时按当时价格生成版本订单，后续改价不会影响已创建订单。'
  },
  {
    step: '03',
    title: '真实支付入口',
    desc: '付费版本统一进入微信支付，不再按版本编码写死入口判断。'
  },
  {
    step: '04',
    title: '权益生效',
    desc: '支付回调成功后开通权益，并可在订单管理和支付管理中追踪。'
  }
];
</script>

<style scoped>
.plans-page {
  display: grid;
  gap: 16px;
}

.plan-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  padding: 26px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background:
    radial-gradient(circle at 92% 12%, rgba(37, 99, 235, 0.12), transparent 260px),
    linear-gradient(135deg, #f8fafc 0%, #fff 56%, #eff6ff 100%);
}

.eyebrow {
  margin: 0 0 8px;
  color: #2563eb;
  font-size: 13px;
  font-weight: 700;
}

.plan-hero h1 {
  margin: 0;
  color: #111827;
  font-size: 28px;
  line-height: 1.25;
}

.hero-desc {
  max-width: 720px;
  margin: 12px 0 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.7;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.hero-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 112px;
  height: 38px;
  padding: 0 16px;
  border: 1px solid #d8e0ec;
  border-radius: 6px;
  background: #fff;
  color: #334155;
  font-weight: 700;
  text-decoration: none;
}

.hero-button.primary {
  border-color: #2563eb;
  background: #2563eb;
  color: #fff;
}

.flow-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.flow-card {
  min-height: 126px;
  padding: 18px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.04);
}

.flow-card span {
  color: #2563eb;
  font-size: 13px;
  font-weight: 800;
}

.flow-card h3 {
  margin: 10px 0 8px;
  color: #111827;
  font-size: 17px;
}

.flow-card p {
  margin: 0;
  color: #64748b;
  font-size: 13px;
  line-height: 1.65;
}

.plan-tip {
  margin-bottom: 0;
}

@media (max-width: 1100px) {
  .plan-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .hero-actions {
    justify-content: flex-start;
  }

  .flow-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
