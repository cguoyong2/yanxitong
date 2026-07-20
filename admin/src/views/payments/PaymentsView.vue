<template>
  <main class="page">
    <header>
      <h1>支付管理</h1>
      <el-button @click="load">刷新</el-button>
    </header>

    <section v-if="orderFilters.banquetId" class="context-panel">
      <div>
        <span>当前钻取宴席</span>
        <strong>{{ contextBanquetTitle }}</strong>
        <p>支付订单和相关回调已按该宴席过滤；当前页签：{{ activeTab === 'callbacks' ? '回调与异常' : activeTab === 'orders' ? '支付订单' : '支付配置' }}。</p>
      </div>
      <div class="context-actions">
        <el-button @click="goBanquet(Number(orderFilters.banquetId))">返回宴席工作台</el-button>
        <el-button @click="goBusiness">业务数据</el-button>
        <el-button @click="goOperationLog('banquet', Number(orderFilters.banquetId))">查看日志</el-button>
        <el-button @click="clearContext">清除筛选</el-button>
      </div>
    </section>

    <section class="payment-command">
      <div class="command-main">
        <span class="command-eyebrow">Payment Command Center</span>
        <h2>支付运营工作台</h2>
        <p>先确认通道可用，再跟踪订单状态，最后集中处理回调异常和人工补偿。</p>
      </div>
      <div class="command-steps">
        <button class="command-step" type="button" :class="{ active: activeTab === 'config' }" @click="activeTab = 'config'">
          <span>01</span>
          <strong>配置检查</strong>
          <small>{{ paymentBlockerCount > 0 ? `${paymentBlockerCount} 个阻塞` : '生产可用' }}</small>
        </button>
        <button class="command-step" type="button" :class="{ active: activeTab === 'orders' }" @click="activeTab = 'orders'">
          <span>02</span>
          <strong>订单跟踪</strong>
          <small>{{ orderSummary.count }} 笔订单</small>
        </button>
        <button class="command-step" type="button" :class="{ active: activeTab === 'callbacks', danger: callbackSummary.failed > 0 }" @click="activeTab = 'callbacks'">
          <span>03</span>
          <strong>异常处理</strong>
          <small>{{ callbackSummary.failed > 0 ? `${callbackSummary.failed} 个失败` : '暂无异常' }}</small>
        </button>
      </div>
    </section>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="支付配置" name="config">
        <section class="tab-intro">
          <div>
            <span>配置检查</span>
            <h2>确认真实支付通道是否可以上线</h2>
            <p>这里用于核对生产安全、微信支付配置、证书和回调地址。状态为可进入真实联调后，再做小额支付验证。</p>
          </div>
          <el-button type="primary" @click="load">重新检查</el-button>
        </section>

        <section class="launch-gate-grid">
          <article class="launch-gate" :class="{ danger: systemBlockerCount > 0, warning: systemBlockerCount === 0 && systemWarningCount > 0 }">
            <span>系统安全阻塞</span>
            <strong>{{ systemBlockerCount }}</strong>
            <small>{{ systemBlockerCount > 0 ? '生产前必须处理' : systemWarningCount > 0 ? `${systemWarningCount} 个警告` : '暂无阻塞' }}</small>
          </article>
          <article class="launch-gate" :class="{ danger: paymentBlockerCount > 0 }">
            <span>支付配置阻塞</span>
            <strong>{{ paymentBlockerCount }}</strong>
            <small>{{ paymentBlockerCount > 0 ? '补齐通道配置' : '可进入联调' }}</small>
          </article>
          <article class="launch-gate" :class="{ danger: callbackSummary.failed > 0 }">
            <span>运营异常阻塞</span>
            <strong>{{ callbackSummary.failed }}</strong>
            <small>{{ callbackSummary.failed > 0 ? '先处理失败回调' : '暂无失败回调' }}</small>
          </article>
        </section>

        <section v-if="securityReadiness" class="readiness-panel">
          <div class="readiness-head">
            <div>
              <h2>系统安全检查</h2>
              <p>{{ securityReadiness.environment }} / {{ securityReadiness.activeProfiles?.length ? securityReadiness.activeProfiles.join(', ') : 'no-profile' }}</p>
            </div>
            <el-tag :type="securityReadiness.status === 'READY' ? 'success' : securityReadiness.status === 'WARN' ? 'warning' : 'danger'">
              {{ securityReadiness.status }}
            </el-tag>
          </div>
          <div v-if="securityReadiness.blockers?.length || securityReadiness.warnings?.length" class="blocking-list">
            <span>阻塞/警告</span>
            <el-tag v-for="item in securityReadiness.blockers || []" :key="`blocker-${item}`" type="danger" effect="plain">{{ item }}</el-tag>
            <el-tag v-for="item in securityReadiness.warnings || []" :key="`warning-${item}`" type="warning" effect="plain">{{ item }}</el-tag>
          </div>
        </section>

        <section v-if="launchReadiness" class="readiness-panel">
          <div class="readiness-head">
            <div>
              <h2>支付上线检查</h2>
              <p>{{ launchReadiness.provider }}</p>
            </div>
            <el-tag :type="launchReadiness.ready ? 'success' : 'danger'">
              {{ launchReadiness.ready ? '可进入真实联调' : '存在阻塞项' }}
            </el-tag>
          </div>
          <div v-if="launchReadiness.blockers?.length" class="blocking-list">
            <span>阻塞项</span>
            <el-tag v-for="item in launchReadiness.blockers" :key="String(item)" type="danger" effect="plain">
              {{ item }}
            </el-tag>
          </div>
          <div v-if="launchReadiness.groups?.length" class="readiness-groups">
            <article v-for="group in launchReadiness.groups" :key="String(group.code)" class="readiness-group" :class="{ danger: !group.ready }">
              <div class="readiness-group-head">
                <strong>{{ group.label }}</strong>
                <el-tag :type="group.ready ? 'success' : 'danger'" effect="plain">{{ group.ready ? '通过' : '阻塞' }}</el-tag>
              </div>
              <p>{{ group.blockers?.length ? group.blockers.join('、') : '无阻塞项' }}</p>
            </article>
          </div>
          <div class="readiness-grid">
            <article v-for="item in launchReadiness.checklist" :key="String(item.code)" class="readiness-item">
              <el-tag :type="item.passed ? 'success' : 'danger'" effect="plain">
                {{ item.passed ? '通过' : '未通过' }}
              </el-tag>
              <div>
                <h3>{{ item.label }}</h3>
                <p>{{ item.detail }}</p>
              </div>
            </article>
          </div>
          <el-collapse>
            <el-collapse-item title="商户侧资料清单" name="merchant">
              <ul>
                <li v-for="item in launchReadiness.merchantInformation" :key="String(item)">{{ item }}</li>
              </ul>
            </el-collapse-item>
            <el-collapse-item title="联调步骤" name="rollout">
              <ol>
                <li v-for="item in launchReadiness.rolloutSteps" :key="String(item)">{{ item }}</li>
              </ol>
            </el-collapse-item>
            <el-collapse-item title="回滚步骤" name="rollback">
              <ol>
                <li v-for="item in launchReadiness.rollbackSteps" :key="String(item)">{{ item }}</li>
              </ol>
            </el-collapse-item>
          </el-collapse>
        </section>

        <section class="provider-grid">
          <article v-for="provider in providers" :key="String(provider.provider)" class="provider-card">
            <div class="provider-head">
              <div>
                <h2>{{ provider.provider }}</h2>
                <p>{{ provider.defaultProvider ? '当前默认支付通道' : '备用支付通道' }}</p>
              </div>
              <el-tag :type="provider.productionReady ? 'success' : provider.enabled ? 'warning' : 'info'">
                {{ provider.productionReady ? '生产可用' : provider.enabled ? '待补配置' : '未启用' }}
              </el-tag>
            </div>
            <dl class="provider-fields">
              <div>
                <dt>{{ isDirectProvider(provider) ? '直连商户号' : '商户号' }}</dt>
                <dd>{{ provider.merchantId || '-' }}</dd>
              </div>
              <div>
                <dt>{{ isDirectProvider(provider) ? '小程序 AppID' : 'AppID' }}</dt>
                <dd>{{ provider.appId || '-' }}</dd>
              </div>
              <div v-if="isServiceProvider(provider)">
                <dt>服务商号</dt>
                <dd>{{ provider.serviceProviderId || '-' }}</dd>
              </div>
              <div v-if="isServiceProvider(provider)">
                <dt>子商户号</dt>
                <dd>{{ provider.subMerchantId || '-' }}</dd>
              </div>
              <div>
                <dt>证书模式</dt>
                <dd>{{ provider.certificateMode || '-' }}</dd>
              </div>
              <div>
                <dt>通知地址</dt>
                <dd>{{ provider.notifyUrlConfigured ? '已配置' : '缺失' }}</dd>
              </div>
              <div>
                <dt>API v3 Key</dt>
                <dd>{{ provider.apiV3KeyConfigured ? '已配置' : '缺失' }}</dd>
              </div>
              <div>
                <dt>私钥文件</dt>
                <dd>{{ provider.privateKeyConfigured ? '已配置' : '缺失' }}</dd>
              </div>
            </dl>
            <div class="missing">
              <span>缺失项</span>
              <template v-if="provider.missingItems?.length">
                <el-tag v-for="item in provider.missingItems" :key="String(item)" type="danger" effect="plain">
                  {{ item }}
                </el-tag>
              </template>
              <el-tag v-else type="success" effect="plain">无</el-tag>
            </div>
          </article>
        </section>
      </el-tab-pane>

      <el-tab-pane label="支付订单" name="orders">
        <section class="tab-intro">
          <div>
            <span>支付订单</span>
            <h2>跟踪用户发起的每一笔支付</h2>
            <p>订单列表用于核对支付状态、金额、机构交易号和履约情况；待支付或状态异常时，再进入回调异常或人工核销。</p>
          </div>
          <div class="tab-actions">
            <el-button @click="showFailures">查看异常回调</el-button>
            <el-button :loading="maintenanceRunning" @click="runMaintenance">立即查单</el-button>
            <el-button type="primary" @click="load">刷新订单</el-button>
          </div>
        </section>

        <el-alert
          v-if="lastMaintenanceResult"
          class="maintenance-result"
          :type="lastMaintenanceResult.failed > 0 ? 'warning' : 'success'"
          :closable="false"
          show-icon
        >
          <template #title>
            本次查单处理 {{ lastMaintenanceResult.candidates }} 笔：支付成功 {{ lastMaintenanceResult.paid }} 笔，
            已关闭 {{ lastMaintenanceResult.closed }} 笔，待支付 {{ lastMaintenanceResult.pending }} 笔，
            失败 {{ lastMaintenanceResult.failed }} 笔
          </template>
        </el-alert>

        <section class="filters">
          <el-input v-model="orderFilters.banquetId" clearable placeholder="宴席 ID" @change="load" />
          <el-input v-model="orderFilters.orderNo" clearable placeholder="订单号" @change="load" />
          <el-select v-model="orderFilters.payStatus" clearable placeholder="支付状态" @change="load">
            <el-option label="未支付" value="UNPAID" />
            <el-option label="已支付" value="PAID" />
          </el-select>
          <el-select v-model="orderFilters.scene" clearable placeholder="场景" @change="load">
            <el-option label="礼金支付" value="GIFT" />
          </el-select>
          <el-select v-model="orderFilters.entrySource" clearable placeholder="入口">
            <el-option label="线上随礼" value="ONLINE_GIFT" />
            <el-option label="现场扫码" value="ONSITE_QR" />
          </el-select>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="resetOrderFilters">重置</el-button>
        </section>
        <section class="metric-grid">
          <article class="metric">
            <span>支付订单</span>
            <strong>{{ orderSummary.count }}</strong>
          </article>
          <article class="metric">
            <span>已支付</span>
            <strong>{{ orderSummary.paid }}</strong>
          </article>
          <article class="metric">
            <span>待支付</span>
            <strong>{{ orderSummary.unpaid }}</strong>
          </article>
          <article class="metric">
            <span>金额合计</span>
            <strong>{{ formatMoney(orderSummary.amount) }}</strong>
          </article>
        </section>
        <el-table v-loading="loading" :data="displayedOrders" border stripe empty-text="暂无支付订单">
          <el-table-column prop="id" label="ID" width="90" />
          <el-table-column prop="orderNo" label="订单号" min-width="180" />
          <el-table-column prop="provider" label="Provider" min-width="170" />
          <el-table-column label="场景" width="130">
            <template #default="{ row }">{{ displayLabel(row.scene) }}</template>
          </el-table-column>
          <el-table-column label="入口" width="130">
            <template #default="{ row }">{{ displayLabel(row.entrySource) }}</template>
          </el-table-column>
          <el-table-column prop="banquetId" label="宴席ID" width="100" />
          <el-table-column label="金额" width="110">
            <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }"><el-tag :type="tagType(row.payStatus)">{{ displayLabel(row.payStatus) }}</el-tag></template>
          </el-table-column>
          <el-table-column label="排障建议" min-width="260">
            <template #default="{ row }">{{ paymentOrderAdvice(row) }}</template>
          </el-table-column>
          <el-table-column prop="providerTradeNo" label="机构交易号" min-width="180" />
          <el-table-column prop="prepayId" label="预支付ID" min-width="180" show-overflow-tooltip />
          <el-table-column label="创建时间" min-width="170">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="210" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.banquetId" link type="primary" @click="goBanquet(row.banquetId as number)">宴席视图</el-button>
              <el-button v-if="row.banquetId" link type="primary" @click="goOrderCenter(row)">订单中心</el-button>
              <el-button link type="primary" @click="goOperationLog('payment_order', row.id as number)">日志</el-button>
              <el-button link type="primary" @click="goBroadcast()">播报</el-button>
              <el-button
                v-if="row.payStatus === 'PAID'"
                link
                type="success"
                @click="compensateOrder(row)"
              >
                补履约
              </el-button>
              <el-button
                v-if="row.payStatus !== 'PAID'"
                link
                type="warning"
                @click="manualSettle(row)"
              >
                人工核销
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="回调与异常" name="callbacks">
        <section class="tab-intro danger-intro">
          <div>
            <span>回调与异常</span>
            <h2>集中处理支付回调失败、验签异常和人工补偿</h2>
            <p>优先处理验签失败、金额不一致、订单不存在和交易号冲突。每次处理必须填写备注，便于后续审计。</p>
          </div>
          <div class="tab-actions">
            <el-button @click="showFailures">只看异常</el-button>
            <el-button type="primary" @click="load">刷新回调</el-button>
          </div>
        </section>

        <section class="filters">
          <el-input v-model="orderFilters.banquetId" clearable placeholder="宴席 ID" @change="load" />
          <el-select v-model="processStatus" clearable placeholder="处理状态" @change="load">
            <el-option label="失败" value="FAILED" />
            <el-option label="成功" value="SUCCESS" />
            <el-option label="已处理" value="HANDLED" />
            <el-option label="已忽略" value="IGNORED" />
          </el-select>
          <el-select v-model="verifyStatus" clearable placeholder="验签状态" @change="load">
            <el-option label="已验证" value="VERIFIED" />
            <el-option label="失败" value="FAILED" />
          </el-select>
          <el-input v-model="callbackOrderNo" clearable placeholder="订单号" />
          <el-button @click="showFailures">只看异常</el-button>
          <el-button @click="resetCallbackFilters">重置</el-button>
        </section>
        <section class="metric-grid">
          <article class="metric">
            <span>回调记录</span>
            <strong>{{ callbackSummary.count }}</strong>
          </article>
          <article class="metric">
            <span>失败</span>
            <strong class="danger">{{ callbackSummary.failed }}</strong>
          </article>
          <article class="metric">
            <span>已处理/忽略</span>
            <strong>{{ callbackSummary.resolved }}</strong>
          </article>
          <article class="metric">
            <span>验签失败</span>
            <strong class="danger">{{ callbackSummary.verifyFailed }}</strong>
          </article>
        </section>
        <section v-if="failedCallbacks.length" class="incident-panel">
          <div class="incident-head">
            <h2>当前异常处理建议</h2>
            <el-button link type="primary" @click="showFailures">只看异常</el-button>
          </div>
          <div class="incident-grid">
            <article v-for="row in failedCallbacks.slice(0, 4)" :key="String(row.id)" class="incident-card">
              <div>
                <strong>{{ row.orderNo || `回调 ${row.id}` }}</strong>
                <p>{{ row.errorMessage || '未知异常' }}</p>
              </div>
              <el-tag :type="callbackAdvice(row).type" effect="plain">{{ callbackAdvice(row).action }}</el-tag>
              <small>{{ callbackAdvice(row).detail }}</small>
            </article>
          </div>
        </section>
        <el-table v-loading="loading" :data="displayedCallbacks" border stripe empty-text="暂无回调记录">
          <el-table-column prop="id" label="ID" width="90" />
          <el-table-column prop="provider" label="Provider" min-width="170" />
          <el-table-column prop="orderNo" label="订单号" min-width="180" />
          <el-table-column prop="providerTradeNo" label="机构交易号" min-width="160" />
          <el-table-column prop="eventType" label="事件类型" min-width="170" show-overflow-tooltip />
          <el-table-column prop="providerEventId" label="事件ID" min-width="160" show-overflow-tooltip />
          <el-table-column label="验签" width="100">
            <template #default="{ row }"><el-tag :type="tagType(row.verifyStatus)">{{ displayLabel(row.verifyStatus) }}</el-tag></template>
          </el-table-column>
          <el-table-column label="处理" width="100">
            <template #default="{ row }"><el-tag :type="tagType(row.processStatus)">{{ displayLabel(row.processStatus) }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="errorMessage" label="异常" min-width="220" show-overflow-tooltip />
          <el-table-column prop="handleRemark" label="处理备注" min-width="180" show-overflow-tooltip />
          <el-table-column label="排障建议" min-width="240">
            <template #default="{ row }">{{ callbackAdvice(row).detail }}</template>
          </el-table-column>
          <el-table-column label="创建时间" min-width="170">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="170" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openCallback(row)">详情</el-button>
              <el-button link type="primary" @click="focusOrder(row.orderNo as string)">订单</el-button>
              <el-button link type="primary" @click="goPaymentOrderLog(row)">订单日志</el-button>
              <el-button link type="primary" @click="goOperationLog('payment_callback_log', row.id as number)">日志</el-button>
              <el-button
                v-if="row.processStatus === 'FAILED'"
                link
                type="success"
                @click="retryCallback(row.id as number)"
              >
                重试
              </el-button>
              <el-button
                v-if="row.processStatus === 'FAILED'"
                link
                type="primary"
                @click="resolve(row.id as number, 'HANDLED')"
              >
                标记已处理
              </el-button>
              <el-button
                v-if="row.processStatus === 'FAILED'"
                link
                type="warning"
                @click="resolve(row.id as number, 'IGNORED')"
              >
                忽略
              </el-button>
              <span v-if="row.processStatus !== 'FAILED'" class="muted">无需处理</span>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-drawer v-model="callbackDrawerVisible" title="回调详情" size="48%">
      <section v-if="selectedCallback" class="callback-detail">
        <dl>
          <div><dt>ID</dt><dd>{{ selectedCallback.id }}</dd></div>
          <div><dt>Provider</dt><dd>{{ selectedCallback.provider }}</dd></div>
          <div><dt>订单号</dt><dd>{{ selectedCallback.orderNo || '-' }}</dd></div>
          <div><dt>机构交易号</dt><dd>{{ selectedCallback.providerTradeNo || '-' }}</dd></div>
          <div><dt>事件ID</dt><dd>{{ selectedCallback.providerEventId || '-' }}</dd></div>
          <div><dt>事件类型</dt><dd>{{ selectedCallback.eventType || '-' }}</dd></div>
          <div><dt>资源类型</dt><dd>{{ selectedCallback.resourceType || '-' }}</dd></div>
          <div><dt>证书序列号</dt><dd>{{ selectedCallback.providerSerialNo || '-' }}</dd></div>
          <div><dt>处理备注</dt><dd>{{ selectedCallback.handleRemark || '-' }}</dd></div>
        </dl>
        <h3>Headers</h3>
        <pre>{{ selectedCallback.headers || '-' }}</pre>
        <h3>解密内容</h3>
        <pre>{{ selectedCallback.decryptedBody || '-' }}</pre>
        <h3>原始内容</h3>
        <pre>{{ selectedCallback.rawBody || '-' }}</pre>
      </section>
    </el-drawer>
  </main>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus';
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { http, recordsOf, type ApiResponse, type PageResult } from '../../api/client';
import { displayLabel, formatDateTime, formatMoney, tagType } from '../../utils/display';

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const orders = ref<Record<string, unknown>[]>([]);
const callbacks = ref<Record<string, unknown>[]>([]);
const banquetOptions = ref<{ id: number; name: string }[]>([]);
const providers = ref<Record<string, any>[]>([]);
const launchReadiness = ref<Record<string, any> | null>(null);
const securityReadiness = ref<Record<string, any> | null>(null);
const activeTab = ref(route.query.tab === 'callbacks' ? 'callbacks' : route.query.tab === 'config' ? 'config' : 'orders');
const processStatus = ref(String(route.query.processStatus || ''));
const verifyStatus = ref(String(route.query.verifyStatus || ''));
const callbackOrderNo = ref(String(route.query.orderNo || ''));
const callbackDrawerVisible = ref(false);
const selectedCallback = ref<Record<string, unknown> | null>(null);
const maintenanceRunning = ref(false);
const lastMaintenanceResult = ref<PaymentMaintenanceRunResult | null>(null);
const orderFilters = ref({
  banquetId: String(route.query.banquetId || ''),
  orderNo: String(route.query.orderNo || ''),
  payStatus: String(route.query.payStatus || ''),
  scene: String(route.query.scene || ''),
  entrySource: String(route.query.entrySource || '')
});
const contextBanquet = computed(() => banquetOptions.value.find((item) => String(item.id) === String(orderFilters.value.banquetId)));
const contextBanquetTitle = computed(() => contextBanquet.value ? `${contextBanquet.value.id} · ${contextBanquet.value.name}` : `宴席 ID ${orderFilters.value.banquetId}`);

function isDirectProvider(provider: Record<string, any>) {
  return provider.provider === 'WECHAT_DIRECT';
}

function isServiceProvider(provider: Record<string, any>) {
  return provider.provider === 'WECHAT_SERVICE_PROVIDER';
}

const displayedOrders = computed(() => {
  return orders.value.filter((item) => {
    if (orderFilters.value.banquetId && Number(item.banquetId) !== Number(orderFilters.value.banquetId)) {
      return false;
    }
    if (orderFilters.value.orderNo && !String(item.orderNo || '').includes(orderFilters.value.orderNo)) {
      return false;
    }
    if (orderFilters.value.entrySource && item.entrySource !== orderFilters.value.entrySource) {
      return false;
    }
    return true;
  });
});
const displayedCallbacks = computed(() => {
  const orderNos = new Set(displayedOrders.value.map((item) => String(item.orderNo)));
  return callbacks.value.filter((item) => {
    if (orderFilters.value.banquetId && !orderNos.has(String(item.orderNo))) {
      return false;
    }
    if (callbackOrderNo.value && !String(item.orderNo || '').includes(callbackOrderNo.value)) {
      return false;
    }
    return true;
  });
});
const orderSummary = computed(() => ({
  count: displayedOrders.value.length,
  paid: displayedOrders.value.filter((item) => item.payStatus === 'PAID').length,
  unpaid: displayedOrders.value.filter((item) => item.payStatus !== 'PAID').length,
  amount: displayedOrders.value.reduce((total, item) => total + Number(item.amount || 0), 0)
}));
const callbackSummary = computed(() => ({
  count: displayedCallbacks.value.length,
  failed: displayedCallbacks.value.filter((item) => item.processStatus === 'FAILED').length,
  resolved: displayedCallbacks.value.filter((item) => ['HANDLED', 'IGNORED'].includes(String(item.processStatus))).length,
  verifyFailed: displayedCallbacks.value.filter((item) => item.verifyStatus === 'FAILED').length
}));
const failedCallbacks = computed(() => displayedCallbacks.value.filter((item) => item.processStatus === 'FAILED'));
const systemBlockerCount = computed(() => Number(securityReadiness.value?.blockers?.length || 0));
const systemWarningCount = computed(() => Number(securityReadiness.value?.warnings?.length || 0));
const paymentBlockerCount = computed(() => Number(launchReadiness.value?.blockers?.length || 0));

interface PaymentMaintenanceRunResult {
  candidates: number;
  paid: number;
  closed: number;
  pending: number;
  failed: number;
  skipped: boolean;
}

async function load() {
  loading.value = true;
  try {
    const callbackQuery = new URLSearchParams();
    if (processStatus.value) {
      callbackQuery.set('processStatus', processStatus.value);
    }
    if (verifyStatus.value) {
      callbackQuery.set('verifyStatus', verifyStatus.value);
    }
    callbackQuery.set('pageSize', '100');
    const orderQuery = new URLSearchParams();
    if (orderFilters.value.payStatus) {
      orderQuery.set('payStatus', orderFilters.value.payStatus);
    }
    if (orderFilters.value.scene) {
      orderQuery.set('scene', orderFilters.value.scene);
    }
    orderQuery.set('pageSize', '100');
    const suffix = callbackQuery.toString() ? `?${callbackQuery}` : '';
    const orderSuffix = orderQuery.toString() ? `?${orderQuery}` : '';
    const [providerResponse, readinessResponse, securityResponse, banquetResponse, orderResponse, callbackResponse] = await Promise.all([
      http.get<ApiResponse<Record<string, unknown>[]>>('/admin/payments/providers'),
      http.get<ApiResponse<Record<string, unknown>>>('/admin/payments/launch-readiness'),
      http.get<ApiResponse<Record<string, unknown>>>('/health/readiness'),
      http.get<ApiResponse<{ id: number; name: string }[]>>('/admin/banquets'),
      http.get<ApiResponse<Record<string, unknown>[] | PageResult<Record<string, unknown>>>>(`/admin/payments/orders${orderSuffix}`),
      http.get<ApiResponse<Record<string, unknown>[] | PageResult<Record<string, unknown>>>>(`/admin/payments/callbacks${suffix}`)
    ]);
    providers.value = providerResponse.data.data || [];
    launchReadiness.value = readinessResponse.data.data || null;
    securityReadiness.value = securityResponse.data.data || null;
    banquetOptions.value = banquetResponse.data.data || [];
    orders.value = recordsOf(orderResponse.data.data);
    callbacks.value = recordsOf(callbackResponse.data.data);
    syncPaymentQuery();
  } finally {
    loading.value = false;
  }
}

async function resolve(id: number, status: 'HANDLED' | 'IGNORED') {
  const label = status === 'HANDLED' ? '已处理' : '忽略';
  const { value } = await ElMessageBox.prompt(`请输入${label}备注`, '支付异常处理', {
    inputPlaceholder: '例如：已与支付机构核对，人工处理完成',
    confirmButtonText: '确认',
    cancelButtonText: '取消'
  });
  await http.post(`/admin/payments/callbacks/${id}/resolve`, {
    processStatus: status,
    handleRemark: value
  });
  ElMessage.success(`已标记${label}`);
  await load();
}

async function runMaintenance() {
  maintenanceRunning.value = true;
  try {
    const response = await http.post<ApiResponse<PaymentMaintenanceRunResult>>('/admin/payments/maintenance/run');
    const result = response.data.data;
    lastMaintenanceResult.value = result;
    if (result.skipped) {
      ElMessage.warning('支付自动查单任务当前未启用');
      return;
    }
    ElMessage.success(
      `查单完成：处理 ${result.candidates} 笔，支付成功 ${result.paid} 笔，关闭 ${result.closed} 笔，失败 ${result.failed} 笔`
    );
    await load();
  } finally {
    maintenanceRunning.value = false;
  }
}

async function retryCallback(id: number) {
  const { value } = await ElMessageBox.prompt('请输入重试备注', '回调重试', {
    inputPlaceholder: '例如：修复订单后重新处理回调',
    confirmButtonText: '重试',
    cancelButtonText: '取消'
  });
  await http.post(`/admin/payments/callbacks/${id}/retry`, {
    handleRemark: value
  });
  ElMessage.success('已发起回调重试');
  await load();
}

async function compensateOrder(row: Record<string, unknown>) {
  const { value } = await ElMessageBox.prompt('请输入补履约备注', '支付补偿', {
    inputPlaceholder: '例如：订单已支付但礼金记录缺失，执行补偿',
    confirmButtonText: '补履约',
    cancelButtonText: '取消'
  });
  await http.post(`/admin/payments/orders/${row.id}/compensate-fulfillment`, {
    handleRemark: value
  });
  ElMessage.success('已执行补履约');
  await load();
}

async function manualSettle(row: Record<string, unknown>) {
  const tradeNoPrompt = await ElMessageBox.prompt('请输入机构交易号或人工核销凭证号', '人工核销', {
    inputPlaceholder: '例如：WX420000... 或 OFFLINE-CONFIRMED-001',
    confirmButtonText: '下一步',
    cancelButtonText: '取消'
  });
  const remarkPrompt = await ElMessageBox.prompt('请输入核销备注', '人工核销备注', {
    inputPlaceholder: '例如：已在支付机构后台确认到账',
    confirmButtonText: '确认核销',
    cancelButtonText: '取消'
  });
  await http.post(`/admin/payments/orders/${row.id}/manual-settle`, {
    providerTradeNo: tradeNoPrompt.value,
    handleRemark: remarkPrompt.value
  });
  ElMessage.success('已人工核销并触发履约');
  await load();
}

function showFailures() {
  activeTab.value = 'callbacks';
  processStatus.value = 'FAILED';
  verifyStatus.value = '';
  syncPaymentQuery();
  void load();
}

function resetOrderFilters() {
  orderFilters.value = {
    banquetId: orderFilters.value.banquetId,
    orderNo: '',
    payStatus: '',
    scene: '',
    entrySource: ''
  };
  void load();
}

function resetCallbackFilters() {
  processStatus.value = '';
  verifyStatus.value = '';
  callbackOrderNo.value = '';
  syncPaymentQuery();
  void load();
}

function openCallback(row: Record<string, unknown>) {
  selectedCallback.value = row;
  callbackDrawerVisible.value = true;
}

function focusOrder(orderNo: string) {
  activeTab.value = 'orders';
  orderFilters.value = {
    ...orderFilters.value,
    orderNo
  };
  callbackOrderNo.value = orderNo;
  syncPaymentQuery();
}

async function goBanquet(banquetId: number) {
  await router.push({ path: '/banquets', query: { banquetId, focus: 'overview' } });
}

async function goOperationLog(targetType: string, targetId: number) {
  await router.push({ path: '/operation-logs', query: { targetType, targetId } });
}

async function goPaymentOrderLog(row: Record<string, unknown>) {
  if (!row.orderNo) {
    await router.push({ path: '/operation-logs', query: { module: 'PAYMENT' } });
    return;
  }
  await router.push({ path: '/operation-logs', query: { module: 'PAYMENT', keyword: String(row.orderNo) } });
}

async function goBroadcast() {
  await router.push({ path: '/broadcast-logs' });
}

async function goOrderCenter(row: Record<string, unknown>) {
  const query: Record<string, string> = {};
  if (row.banquetId) {
    query.banquetId = String(row.banquetId);
  }
  if (row.payStatus) {
    query.payStatus = String(row.payStatus);
  }
  await router.push({ path: '/orders', query });
}

async function goBusiness() {
  if (!orderFilters.value.banquetId) {
    return;
  }
  await router.push({ path: '/business', query: { banquetId: orderFilters.value.banquetId, tab: 'gifts' } });
}

function syncPaymentQuery() {
  const nextQuery: Record<string, string> = { tab: activeTab.value };
  if (orderFilters.value.banquetId) {
    nextQuery.banquetId = orderFilters.value.banquetId;
  }
  if (orderFilters.value.orderNo) {
    nextQuery.orderNo = orderFilters.value.orderNo;
  }
  if (orderFilters.value.payStatus) {
    nextQuery.payStatus = orderFilters.value.payStatus;
  }
  if (orderFilters.value.scene) {
    nextQuery.scene = orderFilters.value.scene;
  }
  if (orderFilters.value.entrySource) {
    nextQuery.entrySource = orderFilters.value.entrySource;
  }
  if (processStatus.value) {
    nextQuery.processStatus = processStatus.value;
  }
  if (verifyStatus.value) {
    nextQuery.verifyStatus = verifyStatus.value;
  }
  void router.replace({ path: '/payments', query: nextQuery });
}

function clearContext() {
  orderFilters.value = {
    banquetId: '',
    orderNo: '',
    payStatus: '',
    scene: '',
    entrySource: ''
  };
  callbackOrderNo.value = '';
  syncPaymentQuery();
}

function callbackAdvice(row: Record<string, unknown>) {
  const error = String(row.errorMessage || '').toLowerCase();
  if (String(row.verifyStatus) === 'FAILED') {
    return {
      action: '核对验签',
      detail: '检查证书/公钥、API v3 Key、原始报文和回调头；修复后再重试。',
      type: 'danger' as const
    };
  }
  if (error.includes('amount mismatch')) {
    return {
      action: '人工核对',
      detail: '先与支付机构后台核对金额；确认到账后再人工核销或标记处理。',
      type: 'warning' as const
    };
  }
  if (error.includes('order not found')) {
    return {
      action: '查订单',
      detail: '核对 out_trade_no/orderNo 是否属于当前环境；无效回调可忽略并备注。',
      type: 'warning' as const
    };
  }
  if (error.includes('trade no mismatch')) {
    return {
      action: '暂停处理',
      detail: '交易号冲突需要人工审计，确认前不要覆盖已支付订单。',
      type: 'danger' as const
    };
  }
  return {
    action: '重试/标记',
    detail: '修复根因后重试；已外部处理则标记已处理，非业务回调可忽略。',
    type: 'primary' as const
  };
}

function paymentOrderAdvice(row: Record<string, unknown>) {
  if (row.payStatus === 'PAID') {
    if (!row.providerTradeNo) {
      return '已支付但缺少机构交易号，建议查看回调详情并补齐支付凭证。';
    }
    return '已支付，可核对礼金记录、人情账本和确认屏/云喇叭播报是否完成。';
  }
  if (row.providerTradeNo) {
    return '未支付但已有机构交易号，建议先查回调与支付机构后台，再决定人工核销。';
  }
  return '待支付，若用户已付款但状态未变，请先查回调异常，再人工核销或重试回调。';
}

onMounted(load);

watch(activeTab, () => {
  syncPaymentQuery();
});
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 24px;
  background: #f6f7f9;
}

header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

h1 {
  margin: 0;
  font-size: 20px;
}

.payment-command {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) minmax(520px, 1.4fr);
  gap: 18px;
  align-items: stretch;
  margin-bottom: 18px;
  padding: 20px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background:
    linear-gradient(135deg, rgba(15, 23, 42, 0.96), rgba(127, 29, 29, 0.9)),
    #111827;
  color: #fff;
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.12);
}

.command-main {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.command-eyebrow,
.tab-intro span {
  color: #fed7aa;
  font-size: 12px;
  font-weight: 900;
}

.command-main h2,
.tab-intro h2 {
  margin: 6px 0 8px;
  font-size: 24px;
  font-weight: 900;
}

.command-main p,
.tab-intro p {
  max-width: 620px;
  margin: 0;
  line-height: 1.65;
}

.command-main p {
  color: rgba(255, 255, 255, 0.74);
}

.command-steps {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.command-step {
  display: grid;
  gap: 8px;
  min-height: 124px;
  padding: 16px;
  text-align: left;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
  cursor: pointer;
}

.command-step:hover,
.command-step.active {
  border-color: #fed7aa;
  background: rgba(255, 247, 237, 0.16);
}

.command-step.danger {
  border-color: rgba(252, 165, 165, 0.8);
}

.command-step span {
  color: #fdba74;
  font-size: 12px;
  font-weight: 900;
}

.command-step strong {
  font-size: 18px;
}

.command-step small {
  color: rgba(255, 255, 255, 0.7);
}

.tab-intro {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 14px;
  padding: 18px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: linear-gradient(180deg, #fff 0%, #f8fafc 100%);
}

.tab-intro span {
  color: #b91c1c;
}

.tab-intro h2 {
  color: #0f172a;
  font-size: 20px;
}

.tab-intro p {
  color: #64748b;
  font-size: 14px;
}

.danger-intro {
  border-color: #fecaca;
  background: linear-gradient(180deg, #fffafa 0%, #fff 100%);
}

.tab-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.maintenance-result {
  margin-bottom: 14px;
}

.context-panel {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 14px;
  padding: 14px;
  border: 1px solid #fecaca;
  border-radius: 8px;
  background: #fff7f7;
}

.context-panel span {
  color: #b91c1c;
  font-size: 12px;
  font-weight: 700;
}

.context-panel strong {
  display: block;
  margin-top: 4px;
  color: #111827;
  font-size: 18px;
}

.context-panel p {
  margin: 6px 0 0;
  color: #475569;
}

.context-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.filters {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(136px, max-content));
  gap: 10px;
  margin-bottom: 12px;
}

.filters .el-input,
.filters .el-select {
  width: 180px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.metric {
  display: grid;
  gap: 6px;
  min-height: 74px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.metric span {
  color: #64748b;
  font-size: 12px;
}

.metric strong {
  color: #111827;
  font-size: 20px;
  line-height: 1.2;
}

.launch-gate-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.launch-gate {
  display: grid;
  gap: 6px;
  min-height: 86px;
  padding: 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.launch-gate.warning {
  border-color: #fde68a;
}

.launch-gate.danger {
  border-color: #fecaca;
}

.launch-gate span,
.launch-gate small {
  color: #64748b;
  font-size: 12px;
}

.launch-gate strong {
  font-size: 24px;
  line-height: 1;
}

.danger {
  color: #b91c1c !important;
}

.readiness-panel {
  margin-bottom: 14px;
  padding: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fff;
}

.readiness-head,
.readiness-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.readiness-head {
  justify-content: space-between;
  margin-bottom: 12px;
}

.readiness-head h2,
.readiness-item h3 {
  margin: 0 0 4px;
  font-size: 16px;
}

.readiness-head p,
.readiness-item p {
  margin: 0;
  color: #606266;
  font-size: 13px;
}

.blocking-list {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.blocking-list span {
  color: #606266;
  font-size: 13px;
}

.readiness-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.readiness-groups {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 10px;
  margin-bottom: 12px;
}

.readiness-group {
  min-width: 0;
  padding: 10px;
  border: 1px solid #d1fae5;
  border-radius: 6px;
  background: #f8fffb;
}

.readiness-group.danger {
  border-color: #fecaca;
  background: #fffafa;
}

.readiness-group-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}

.readiness-group p {
  margin: 0;
  color: #606266;
  font-size: 13px;
  overflow-wrap: anywhere;
}

.readiness-item {
  min-width: 0;
  padding: 10px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fafafa;
}

.readiness-item div {
  min-width: 0;
}

.readiness-panel ul,
.readiness-panel ol {
  margin: 0;
  padding-left: 20px;
}

.readiness-panel li {
  margin: 4px 0;
}

.provider-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(360px, 1fr));
  gap: 14px;
}

.provider-card {
  padding: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fff;
}

.provider-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.provider-head h2 {
  margin: 0 0 4px;
  font-size: 16px;
}

.provider-head p {
  margin: 0;
  color: #606266;
  font-size: 13px;
}

.provider-fields,
.callback-detail dl {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 16px;
  margin: 0;
}

.provider-fields div,
.callback-detail dl div {
  min-width: 0;
}

.provider-fields dt,
.callback-detail dt {
  color: #909399;
  font-size: 12px;
}

.provider-fields dd,
.callback-detail dd {
  margin: 3px 0 0;
  overflow-wrap: anywhere;
}

.missing {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.missing span {
  color: #606266;
  font-size: 13px;
}

.incident-panel {
  margin-bottom: 14px;
  padding: 14px;
  border: 1px solid #fecaca;
  border-radius: 8px;
  background: #fffafa;
}

.incident-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.incident-head h2 {
  margin: 0;
  font-size: 16px;
}

.incident-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 10px;
}

.incident-card {
  display: grid;
  gap: 8px;
  min-width: 0;
  padding: 10px;
  border: 1px solid #fee2e2;
  border-radius: 6px;
  background: #fff;
}

.incident-card p {
  margin: 4px 0 0;
  color: #606266;
  font-size: 13px;
  overflow-wrap: anywhere;
}

.incident-card small {
  color: #64748b;
  line-height: 1.45;
}

.callback-detail h3 {
  margin: 18px 0 8px;
  font-size: 14px;
}

.callback-detail pre {
  max-height: 220px;
  margin: 0;
  padding: 12px;
  overflow: auto;
  border-radius: 6px;
  background: #f5f7fa;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.muted {
  color: #909399;
}

@media (max-width: 860px) {
  .payment-command {
    grid-template-columns: 1fr;
  }

  .command-steps {
    grid-template-columns: 1fr;
  }

  .tab-intro {
    display: grid;
  }

  .context-panel,
  .filters {
    grid-template-columns: 1fr;
    display: grid;
  }

  .context-actions {
    justify-content: flex-start;
  }

  .filters .el-input,
  .filters .el-select {
    width: 100%;
  }
}
</style>
