<template>
  <view class="page" v-if="detail">
    <text class="title">{{ detail.banquet.name }}</text>
    <view class="meta">类型：{{ detail.banquet.eventTypeCode }}</view>
    <view class="meta">主题：{{ detail.banquet.themeCode }}</view>
    <view class="meta">地点：{{ detail.banquet.location || '-' }}</view>
    <view class="panel">
      <text class="panel-title">基础请柬</text>
      <text class="meta">分享码：{{ detail.invitation?.shareSlug }}</text>
      <text class="meta">分享路径：{{ invitationShareUrl }}</text>
      <button size="mini" @click="openInvite">查看公开页</button>
      <button size="mini" @click="copyInviteLink">复制分享路径</button>
      <button size="mini" @click="editInvite">编辑基础字段</button>
    </view>
    <view class="panel">
      <text class="panel-title">版本与设备</text>
      <text class="meta">当前版本：{{ entitlements.currentPlan?.name || '基础版' }}</text>
      <text class="meta">设备租赁：{{ hasDeviceRight ? '已开通' : '未开通' }}</text>
      <text class="meta">Excel 导出：{{ hasExportRight ? 'P1 预留' : '未包含' }}</text>
      <button size="mini" @click="openPlan">选择版本</button>
      <button size="mini" @click="openDevice">设备选择</button>
    </view>
    <view class="panel">
      <text class="panel-title">回执与收礼</text>
      <button size="mini" @click="openRsvpStats">回执统计</button>
      <button size="mini" @click="openGiftPay('ONLINE_GIFT')">线上随礼</button>
      <button size="mini" @click="openGiftPay('ONSITE_QR')">现场扫码</button>
      <button size="mini" @click="openOfflineGift">线下记礼</button>
      <button size="mini" @click="openGiftList">收礼记录</button>
      <button size="mini" @click="openFavor">人情账本</button>
    </view>
    <view class="panel">
      <text class="panel-title">收礼文案</text>
      <text>{{ detail.giftSuccessCopywriting.content }}</text>
    </view>
  </view>
  <view class="page" v-else>加载中</view>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { request } from '../../../api/client';

interface BanquetDetail {
  banquet: {
    id: number;
    name: string;
    eventTypeCode: string;
    themeCode: string;
    location?: string;
  };
  invitation?: {
    id: number;
    title: string;
    shareSlug: string;
  };
  giftSuccessCopywriting: {
    content: string;
  };
}

interface Entitlements {
  currentPlan?: {
    name: string;
  };
  rightValues: Record<string, string>;
}

const detail = ref<BanquetDetail>();
const entitlements = reactive<Entitlements>({
  rightValues: {}
});
const hasDeviceRight = computed(() => Boolean(entitlements.rightValues.DEVICE_RENTAL));
const hasExportRight = computed(() => Boolean(entitlements.rightValues.EXCEL_EXPORT));
const invitationShareUrl = computed(() => {
  const slug = detail.value?.invitation?.shareSlug;
  return slug ? `/pages/invite/public/index?slug=${slug}` : '-';
});

async function load(id: string) {
  detail.value = await request<BanquetDetail>(`/banquets/${id}`);
  const result = await request<Entitlements>(`/plans/banquets/${id}/entitlements`);
  entitlements.currentPlan = result.currentPlan;
  entitlements.rightValues = result.rightValues || {};
}

function openInvite() {
  const slug = detail.value?.invitation?.shareSlug;
  if (slug) {
    uni.navigateTo({ url: `/pages/invite/public/index?slug=${slug}` });
  }
}

function editInvite() {
  const invitation = detail.value?.invitation;
  if (invitation) {
    uni.navigateTo({
      url: `/pages/invite/edit-basic/index?invitationId=${invitation.id}`
    });
  }
}

function copyInviteLink() {
  if (invitationShareUrl.value === '-') {
    return;
  }
  uni.setClipboardData({
    data: invitationShareUrl.value,
    success: () => uni.showToast({ title: '已复制', icon: 'success' })
  });
}

function openPlan() {
  if (detail.value?.banquet.id) {
    uni.navigateTo({ url: `/pages/order/plan/index?banquetId=${detail.value.banquet.id}` });
  }
}

function openDevice() {
  if (detail.value?.banquet.id) {
    uni.navigateTo({ url: `/pages/device/select/index?banquetId=${detail.value.banquet.id}` });
  }
}

function openRsvpStats() {
  if (detail.value?.banquet.id) {
    uni.navigateTo({ url: `/pages/rsvp/stats/index?banquetId=${detail.value.banquet.id}` });
  }
}

function openGiftPay(entrySource: string) {
  if (detail.value?.banquet.id) {
    uni.navigateTo({ url: `/pages/gift/pay/index?banquetId=${detail.value.banquet.id}&entrySource=${entrySource}` });
  }
}

function openOfflineGift() {
  if (detail.value?.banquet.id) {
    uni.navigateTo({ url: `/pages/gift/offline/index?banquetId=${detail.value.banquet.id}` });
  }
}

function openGiftList() {
  if (detail.value?.banquet.id) {
    uni.navigateTo({ url: `/pages/gift/list/index?banquetId=${detail.value.banquet.id}` });
  }
}

function openFavor() {
  uni.navigateTo({ url: '/pages/favor/index/index' });
}

onMounted(() => {
  const pages = getCurrentPages();
  const current = pages[pages.length - 1] as unknown as { options?: Record<string, string> };
  const id = current.options?.id;
  if (id) {
    load(id);
  }
});
</script>

<style scoped>
.page {
  padding: 24rpx;
}

.title {
  display: block;
  margin-bottom: 20rpx;
  font-size: 40rpx;
  font-weight: 600;
}

.meta {
  display: block;
  margin-bottom: 12rpx;
  color: #555;
}

.panel {
  margin-top: 24rpx;
  padding: 20rpx;
  border: 1px solid #e5e7eb;
  border-radius: 8rpx;
}

.panel-title {
  display: block;
  margin-bottom: 12rpx;
  font-weight: 600;
}
</style>
