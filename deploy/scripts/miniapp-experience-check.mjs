#!/usr/bin/env node

import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(scriptDir, '..', '..');
const miniappSrc = path.join(repoRoot, 'miniapp', 'src');
const pagesJsonPath = path.join(miniappSrc, 'pages.json');
const failures = [];

function read(file) {
  return fs.readFileSync(path.join(repoRoot, file), 'utf8');
}

function readJson(file) {
  return JSON.parse(fs.readFileSync(file, 'utf8'));
}

function walk(dir, files = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      walk(fullPath, files);
    } else if (/\.(vue|ts)$/.test(entry.name)) {
      files.push(fullPath);
    }
  }
  return files;
}

function requirePage(page, reason) {
  const pagesJson = readJson(pagesJsonPath);
  const registered = new Set((pagesJson.pages || []).map((item) => item.path));
  if (!registered.has(page)) {
    failures.push(`${page}: ${reason}`);
  }
}

function requireText(file, text, reason) {
  const content = read(file);
  if (!content.includes(text)) {
    failures.push(`${file}: missing "${text}" (${reason})`);
  }
}

function forbidPattern(file, pattern, reason) {
  const content = read(file);
  if (pattern.test(content)) {
    failures.push(`${file}: forbidden pattern ${pattern} (${reason})`);
  }
}

[
  'pages/home/index/index',
  'pages/banquet/create/index',
  'pages/banquet/detail/index',
  'pages/invitation/index/index',
  'pages/invite/public/index',
  'pages/invite/edit-basic/index',
  'pages/rsvp/submit/index',
  'pages/rsvp/stats/index',
  'pages/gift/offline/index',
  'pages/gift/list/index',
  'pages/favor/index/index',
  'pages/favor/detail/index',
  'pages/mine/index/index'
].forEach((page) => requirePage(page, 'required non-payment MVP experience page'));

const requiredTexts = [
  ['miniapp/src/pages/home/index/index.vue', '创建宴席', 'home create entry'],
  ['miniapp/src/pages/home/index/index.vue', 'selectType(type.code)', 'home type switching action'],
  ['miniapp/src/pages/home/index/index.vue', 'eventTypeCode=${activeType.value}', 'create page receives selected type'],
  ['miniapp/src/pages/home/index/index.vue', '{{ activeDesign.mark }}', 'home hero mark follows selected event type'],
  ['miniapp/src/pages/home/index/index.vue', 'writeActiveEventType(code)', 'home persists selected event type'],
  ['miniapp/src/pages/home/index/index.vue', 'readLastBanquetContext', 'home can recover latest banquet from local context'],
  ['miniapp/src/pages/home/index/index.vue', 'writeLastBanquetContext', 'home stores latest banquet context'],
  ['miniapp/src/pages/home/index/index.vue', 'safeNavigate', 'home workflow entries have navigation failure feedback'],
  ['miniapp/src/pages/home/index/index.vue', '--accent', 'home tab uses event theme accent variables'],
  ['miniapp/src/pages/banquet/create/index.vue', '填入体验数据', 'create page sample-data helper'],
  ['miniapp/src/pages/banquet/create/index.vue', '慎终追远，思念长存', 'create page memorial copy'],
  ['miniapp/src/pages/banquet/create/index.vue', 'currentDesign.mark', 'create page hero mark follows event type'],
  ['miniapp/src/pages/banquet/create/index.vue', ':class="currentDesign.tone"', 'create page root follows selected event tone'],
  ['miniapp/src/pages/banquet/create/index.vue', '--accent', 'create page uses selected event accent variables'],
  ['miniapp/src/pages/banquet/create/index.vue', 'linear-gradient(135deg, var(--accent), var(--accent-dark))', 'create page selected type and submit buttons use event accent'],
  ['miniapp/src/pages/banquet/create/index.vue', '当前使用模板封面，自定义上传稍后开放', 'cover upload boundary'],
  ['miniapp/src/pages/banquet/create/index.vue', 'writeLastBanquetContext', 'create page stores latest banquet context after success'],
  ['miniapp/src/pages/banquet/create/index.vue', 'validatePhone(true)', 'create page validates contact phone before submit'],
  ['miniapp/src/pages/banquet/create/index.vue', 'mode="date"', 'create page supports native date picker'],
  ['miniapp/src/pages/banquet/create/index.vue', 'mode="time"', 'create page supports native time picker'],
  ['miniapp/src/pages/banquet/create/index.vue', 'fillDefaultTime', 'create page has default time fallback'],
  ['miniapp/src/pages/banquet/create/index.vue', 'openTimePanel', 'create page has manual time fallback panel'],
  ['miniapp/src/pages/banquet/create/index.vue', 'applyManualDateTime', 'create page can apply manually typed date time'],
  ['miniapp/src/pages/banquet/create/index.vue', 'openLocationPanel', 'create page has manual location fallback panel'],
  ['miniapp/src/pages/banquet/create/index.vue', 'applyManualLocation', 'create page can apply manually typed location'],
  ['miniapp/src/pages/banquet/create/index.vue', 'locationSuggestions', 'create page offers location quick-fill suggestions'],
  ['miniapp/src/pages/banquet/create/index.vue', 'uni.chooseLocation', 'create page supports map location selection'],
  ['miniapp/src/pages/banquet/create/index.vue', 'wxApi.chooseLocation', 'create page falls back to native wechat map picker'],
  ['miniapp/src/pages/banquet/create/index.vue', 'uni.openSetting', 'create page can recover denied location permission'],
  ['miniapp/src/pages/banquet/create/index.vue', 'banquetTimeDisplay', 'create page shows selected date and time in the form row'],
  ['miniapp/src/pages/banquet/create/index.vue', '请选择宴席日期和时间', 'create page has visible date-time value placeholder'],
  ['miniapp/src/pages/banquet/create/index.vue', '日期已填入', 'create page confirms native date picker selection'],
  ['miniapp/src/pages/banquet/create/index.vue', '时间已填入', 'create page confirms native time picker selection'],
  ['miniapp/src/pages/banquet/create/index.vue', 'map-button', 'create page has explicit map selection trigger'],
  ['miniapp/src/pages/banquet/create/index.vue', '地图未打开，请手动填写地点', 'create page falls back to manual location when map picker fails'],
  ['miniapp/src/manifest.json', 'requiredPrivateInfos', 'wechat chooseLocation private API declared'],
  ['miniapp/src/manifest.json', 'scope.userLocation', 'wechat location permission description declared'],
  ['miniapp/src/pages/banquet/detail/index.vue', 'activeTheme.value.onlineGiftLabel', 'detail non-payment boundary follows event type'],
  ['miniapp/src/pages/banquet/detail/index.vue', '请柬公开页打开失败', 'detail invitation navigation feedback'],
  ['miniapp/src/pages/banquet/detail/index.vue', 'detailDesign.mark', 'detail hero mark follows banquet event type'],
  ['miniapp/src/pages/banquet/detail/index.vue', 'writeLastBanquetContext', 'detail page updates latest banquet context'],
  ['miniapp/src/pages/banquet/detail/index.vue', '管理进度', 'detail page shows management progress checklist'],
  ['miniapp/src/pages/banquet/detail/index.vue', 'progressItems', 'detail page derives progress from invitation RSVP gift and rights'],
  ['miniapp/src/pages/banquet/detail/index.vue', 'nextStep', 'detail page gives a visible next-step recommendation'],
  ['miniapp/src/pages/banquet/detail/index.vue', '下一步建议', 'detail page labels recommended next action'],
  ['miniapp/src/pages/banquet/detail/index.vue', '办席看板', 'detail page shows banquet management dashboard'],
  ['miniapp/src/pages/banquet/detail/index.vue', 'dashboardItems', 'detail page derives dashboard items from live records'],
  ['miniapp/src/pages/banquet/detail/index.vue', '--accent', 'detail page uses event theme accent variables'],
  ['miniapp/src/pages/banquet/detail/index.vue', '/favor/contacts?banquetId=', 'detail page loads favor contacts for current banquet'],
  ['miniapp/src/pages/banquet/detail/index.vue', 'devicePaidCount', 'detail page summarizes paid device orders'],
  ['miniapp/src/pages/banquet/detail/index.vue', '/devices/orders?banquetId=', 'detail page refreshes device order progress'],
  ['miniapp/src/pages/banquet/detail/index.vue', 'deviceOrders', 'detail page tracks device orders separately from rights'],
  ['miniapp/src/pages/banquet/detail/index.vue', 'safeNavigate', 'detail page uses safe navigation for workflow entries'],
  ['miniapp/src/pages/banquet/detail/index.vue', 'onShow', 'detail page refreshes after returning from child flows'],
  ['miniapp/src/pages/invitation/index/index.vue', '暂无符合条件的模板', 'template empty state'],
  ['miniapp/src/pages/invitation/index/index.vue', '定制请柬服务将在后续版本开放', 'custom invitation boundary'],
  ['miniapp/src/pages/invite/edit-basic/index.vue', '复制路径', 'share path copy'],
  ['miniapp/src/pages/invite/edit-basic/index.vue', '保存请柬失败', 'invitation edit save feedback'],
  ['miniapp/src/pages/invite/edit-basic/index.vue', 'applyThemeCopy', 'invitation edit can apply recommended copy'],
  ['miniapp/src/pages/invite/edit-basic/index.vue', 'applyDefaultSchedule', 'invitation edit can fill schedule'],
  ['miniapp/src/pages/invite/edit-basic/index.vue', 'validateContactPhone', 'invitation edit validates contact phone'],
  ['miniapp/src/pages/invite/edit-basic/index.vue', 'saveAndPreview', 'invitation edit can save and preview'],
  ['miniapp/src/pages/invite/edit-basic/index.vue', 'returnBanquetDetail', 'invitation edit can return to banquet detail'],
  ['miniapp/src/pages/invite/edit-basic/index.vue', ':class="activeTheme.tone"', 'invitation edit page follows event theme tone'],
  ['miniapp/src/pages/invite/edit-basic/index.vue', '--accent', 'invitation edit page uses event theme accent variables'],
  ['miniapp/src/pages/invite/public/index.vue', 'activeTheme.value.onlineGiftLabel', 'public invitation payment boundary follows event type'],
  ['miniapp/src/pages/invite/public/index.vue', '回执页面打开失败', 'public invitation RSVP navigation feedback'],
  ['miniapp/src/pages/invite/public/index.vue', '宾客操作', 'public invitation shows guest action guide'],
  ['miniapp/src/pages/invite/public/index.vue', 'withShareUrl', 'public invitation passes return path to child flows'],
  ['miniapp/src/pages/invite/public/index.vue', '地址已复制，可打开地图导航', 'public invitation has useful address fallback'],
  ['miniapp/src/pages/invite/public/index.vue', 'showContactTip', 'public invitation can copy contact phone'],
  ['miniapp/src/pages/rsvp/submit/index.vue', '返回请柬', 'RSVP return action'],
  ['miniapp/src/pages/rsvp/submit/index.vue', 'shareUrl', 'RSVP can return to originating public invitation'],
  ['miniapp/src/pages/rsvp/submit/index.vue', 'activeTheme.value.giftActionLabel', 'RSVP success action follows event type'],
  ['miniapp/src/pages/rsvp/submit/index.vue', '回执摘要', 'RSVP success page shows submitted summary'],
  ['miniapp/src/pages/rsvp/submit/index.vue', 'openRsvpStats', 'RSVP success can open stats page'],
  ['miniapp/src/pages/rsvp/submit/index.vue', 'safeNavigate', 'RSVP success actions have navigation failure feedback'],
  ['miniapp/src/pages/rsvp/submit/index.vue', '查看随礼说明', 'RSVP success avoids sending guests to offline admin gift entry when payment is disabled'],
  ['miniapp/src/pages/rsvp/submit/index.vue', 'ONLINE_GIFT&guestName=', 'RSVP success routes disabled payment to guest-facing payment notice'],
  ['miniapp/src/pages/rsvp/submit/index.vue', ':class="activeTheme.tone"', 'RSVP submit page follows event theme tone'],
  ['miniapp/src/pages/rsvp/submit/index.vue', '--accent', 'RSVP submit page uses event theme accent variables'],
  ['miniapp/src/pages/rsvp/submit/index.vue', '^1[3-9]\\d{9}$', 'RSVP phone validation uses mainland mobile prefix range'],
  ['miniapp/src/pages/rsvp/stats/index.vue', 'shareSlug', 'RSVP stats can return to public invitation'],
  ['miniapp/src/pages/rsvp/stats/index.vue', 'safeNavigate(`/pages/banquet/detail/index?id=${banquetId.value}`', 'RSVP stats returns to banquet detail'],
  ['miniapp/src/pages/rsvp/stats/index.vue', '回执明细', 'RSVP stats shows record details'],
  ['miniapp/src/pages/rsvp/stats/index.vue', '/rsvp/list?banquetId=', 'RSVP stats loads real records'],
  ['miniapp/src/pages/rsvp/stats/index.vue', 'copyInvitePath', 'RSVP stats can copy invitation path'],
  ['miniapp/src/pages/rsvp/stats/index.vue', 'filterItems', 'RSVP stats supports status filters'],
  ['miniapp/src/pages/rsvp/stats/index.vue', 'refreshStats', 'RSVP stats has explicit refresh feedback'],
  ['miniapp/src/pages/rsvp/stats/index.vue', 'writeLastBanquetContext', 'RSVP stats writes banquet event context for theme sync'],
  ['miniapp/src/pages/rsvp/stats/index.vue', '去发送请柬', 'RSVP empty state guides invitation sharing'],
  ['miniapp/src/pages/rsvp/stats/index.vue', ':class="activeTheme.tone"', 'RSVP stats page follows event theme tone'],
  ['miniapp/src/pages/rsvp/stats/index.vue', '--accent', 'RSVP stats page uses event theme accent variables'],
  ['miniapp/src/pages/gift/offline/index.vue', '继续登记', 'offline gift success modal'],
  ['miniapp/src/pages/gift/offline/index.vue', '查看记录', 'offline gift record navigation'],
  ['miniapp/src/pages/gift/offline/index.vue', '保存记礼失败', 'offline gift save feedback'],
  ['miniapp/src/pages/gift/offline/index.vue', '最近保存成功', 'offline gift visible success state'],
  ['miniapp/src/pages/gift/offline/index.vue', 'recentSaved', 'offline gift keeps current-session saved records visible'],
  ['miniapp/src/pages/gift/offline/index.vue', 'highlightId', 'offline gift passes saved record highlight to list'],
  ['miniapp/src/pages/gift/offline/index.vue', 'openLastFavor', 'offline gift success can open linked favor ledger'],
  ['miniapp/src/pages/gift/offline/index.vue', 'favor-focus-contact', 'offline gift preserves favor focus fallback'],
  ['miniapp/src/pages/gift/offline/index.vue', 'continueRegistration', 'offline gift has explicit continue registration action'],
  ['miniapp/src/pages/gift/offline/index.vue', 'activeTheme.giftAmountLabel', 'offline gift amount label follows event type'],
  ['miniapp/src/pages/gift/offline/index.vue', ':class="activeTheme.tone"', 'offline gift page follows event theme tone'],
  ['miniapp/src/pages/gift/offline/index.vue', '--accent', 'offline gift page uses event theme accent variables'],
  ['miniapp/src/pages/gift/list/index.vue', 'activeTheme.giftRecordLabel', 'gift list title follows event type'],
  ['miniapp/src/pages/gift/list/index.vue', 'openGiftDetail', 'gift list rows can open detail modal'],
  ['miniapp/src/pages/gift/list/index.vue', 'openFavorFromGift', 'gift list detail can open linked favor ledger'],
  ['miniapp/src/pages/gift/list/index.vue', 'copyGiftDetail', 'gift list detail can be copied'],
  ['miniapp/src/pages/gift/list/index.vue', 'current.options?.source', 'gift list can receive source preset from entry page'],
  ['miniapp/src/pages/gift/list/index.vue', 'highlightId', 'gift list can highlight latest saved record'],
  ['miniapp/src/pages/gift/list/index.vue', 'openBanquetDetail', 'gift list can return to banquet management detail'],
  ['miniapp/src/pages/gift/list/index.vue', 'refreshList', 'gift list has explicit refresh feedback'],
  ['miniapp/src/pages/gift/list/index.vue', 'onShow', 'gift list refreshes after returning from offline entry'],
  ['miniapp/src/pages/gift/list/index.vue', ':class="activeTheme.tone"', 'gift list page follows event theme tone'],
  ['miniapp/src/pages/gift/list/index.vue', '--accent', 'gift list page uses event theme accent variables'],
  ['miniapp/src/pages/gift/pay/index.vue', 'activeTheme.onlineGiftLabel', 'payment page labels follow event type'],
  ['miniapp/src/pages/gift/pay/index.vue', 'backToInvitation', 'payment disabled state can return to invitation'],
  ['miniapp/src/pages/gift/pay/index.vue', 'notice-actions', 'payment disabled state exposes explicit actions'],
  ['miniapp/src/pages/gift/pay/index.vue', 'showPaymentNotice', 'payment disabled state explains launch boundary'],
  ['miniapp/src/pages/gift/pay/index.vue', 'safeNavigate', 'payment page navigation has failure feedback'],
  ['miniapp/src/pages/gift/pay/index.vue', ':class="activeTheme.tone"', 'payment page follows event theme tone'],
  ['miniapp/src/pages/gift/pay/index.vue', '--accent', 'payment page uses event theme accent variables'],
  ['miniapp/src/pages/gift/success/index.vue', 'activeTheme.giftRecordLabel', 'payment success copy follows event type'],
  ['miniapp/src/pages/gift/success/index.vue', 'backToInvitation', 'payment success can return to originating invitation'],
  ['miniapp/src/pages/gift/success/index.vue', ':class="activeTheme.tone"', 'payment success page follows event theme tone'],
  ['miniapp/src/pages/gift/success/index.vue', '--accent', 'payment success page uses event theme accent variables'],
  ['miniapp/src/pages/favor/family/index.vue', '家庭协作功能将在后续版本开放', 'family favor boundary'],
  ['miniapp/src/pages/favor/index/index.vue', "setManualDirection('RECEIVED')", 'favor received card action'],
  ['miniapp/src/pages/favor/index/index.vue', "setManualDirection('GIVEN')", 'favor given card action'],
  ['miniapp/src/pages/favor/index/index.vue', 'grid-template-columns: 1fr', 'favor cards do not overflow'],
  ['miniapp/src/pages/favor/index/index.vue', 'display: block;', 'favor manual form is visible'],
  ['miniapp/src/pages/favor/index/index.vue', 'showAllRecent', 'favor recent more expands list'],
  ['miniapp/src/pages/favor/index/index.vue', 'openCompareDetail', 'favor compare can open detail'],
  ['miniapp/src/pages/favor/index/index.vue', 'autoSyncText', 'favor page shows gift-to-favor auto sync status'],
  ['miniapp/src/pages/favor/index/index.vue', 'openLatestAutoContact', 'favor auto sync tip can open latest contact detail'],
  ['miniapp/src/pages/favor/index/index.vue', 'consumeFocusedContact', 'favor page consumes gift-to-favor focus context'],
  ['miniapp/src/pages/favor/index/index.vue', 'lastManualText', 'favor manual entry shows visible success state'],
  ['miniapp/src/pages/favor/index/index.vue', 'compareCandidates', 'favor compare offers quick contact choices'],
  ['miniapp/src/pages/favor/index/index.vue', '--accent', 'favor tab uses event theme accent variables'],
  ['miniapp/src/pages/favor/detail/index.vue', 'copySummary', 'favor detail can copy compare summary'],
  ['miniapp/src/pages/favor/detail/index.vue', 'copyEntry', 'favor detail can copy a single entry'],
  ['miniapp/src/pages/favor/index/index.vue', '{{ activeTheme.mark }}', 'favor tab hero follows selected event type'],
  ['miniapp/src/pages/favor/index/index.vue', '{{ activeTheme.favorText }}', 'favor tab copy follows selected event type'],
  ['miniapp/src/pages/favor/index/index.vue', 'manualNotePlaceholder', 'favor note placeholder follows selected event type'],
  ['miniapp/src/pages/invitation/index/index.vue', '{{ activeTheme.mark }}', 'invitation tab hero follows selected event type'],
  ['miniapp/src/pages/invitation/index/index.vue', '{{ activeTheme.invitationText }}', 'invitation tab copy follows selected event type'],
  ['miniapp/src/pages/invitation/index/index.vue', 'writeActiveEventType(code)', 'invitation type selector persists event type'],
  ['miniapp/src/pages/invitation/index/index.vue', 'readLastBanquetContext', 'invitation tab can recover latest invitation from local context'],
  ['miniapp/src/pages/invitation/index/index.vue', 'eventTypeCode=${activeType.value}', 'invitation create entry keeps selected event type'],
  ['miniapp/src/pages/invitation/index/index.vue', 'safeNavigate', 'invitation tab workflow entries have navigation failure feedback'],
  ['miniapp/src/pages/invitation/index/index.vue', '--accent', 'invitation tab uses event theme accent variables'],
  ['miniapp/src/pages/invitation/index/index.vue', '--page-bg', 'invitation tab background follows event theme'],
  ['miniapp/src/pages/invitation/index/index.vue', 'theme-banner', 'invitation banner is rendered from selected event theme'],
  ['miniapp/src/pages/invitation/index/index.vue', 'theme-cover', 'invitation template covers are rendered from selected event theme'],
  ['miniapp/src/pages/mine/index/index.vue', '{{ activeTheme.mark }}', 'mine tab hero follows selected event type'],
  ['miniapp/src/pages/mine/index/index.vue', '{{ activeTheme.mineText }}', 'mine tab copy follows selected event type'],
  ['miniapp/src/pages/mine/index/index.vue', 'readLastBanquetContext', 'mine tab can recover latest services from local context'],
  ['miniapp/src/pages/mine/index/index.vue', '--page-bg', 'mine tab background follows event theme'],
  ['miniapp/src/pages/mine/index/index.vue', 'theme-banner', 'mine banner is rendered from selected event theme'],
  ['miniapp/src/pages/mine/index/index.vue', 'theme-mini-cover', 'mine service visuals are rendered from selected event theme'],
  ['miniapp/src/pages/order/plan/index.vue', '/plans/orders?banquetId=', 'plan orders are visible after creation'],
  ['miniapp/src/pages/order/plan/index.vue', '版本订单', 'plan page shows order list section'],
  ['miniapp/src/pages/order/plan/index.vue', 'planOrders', 'plan page stores visible order history'],
  ['miniapp/src/pages/order/plan/index.vue', 'cacheOrder(order)', 'plan orders remain visible if list API is unavailable'],
  ['miniapp/src/pages/order/plan/index.vue', 'returnBanquetDetail', 'plan page can return to banquet management detail'],
  ['miniapp/src/pages/order/plan/index.vue', ':class="activeTheme.tone"', 'plan page follows event theme tone'],
  ['miniapp/src/pages/order/plan/index.vue', '--accent', 'plan page uses event theme accent variables'],
  ['miniapp/src/pages/order/plan/index.vue', 'fetchBanquetEventType', 'plan page resolves event theme from current banquet'],
  ['miniapp/src/pages/order/plan/index.vue', '{{ activeTheme.mark }}', 'plan page hero symbol follows current banquet type'],
  ['miniapp/src/pages/device/select/index.vue', 'returnBanquetDetail', 'device page can return to banquet management detail'],
  ['miniapp/src/pages/device/select/index.vue', '设备订单跟踪', 'device page has visible order tracking section'],
  ['miniapp/src/pages/device/select/index.vue', 'formatRentWindow', 'device page shows rental window on orders'],
  ['miniapp/src/pages/device/select/index.vue', 'cacheOrder(order)', 'device orders remain visible if list API is unavailable'],
  ['miniapp/src/pages/device/select/index.vue', 'lastOrderText', 'device page shows latest order success state'],
  ['miniapp/src/pages/device/select/index.vue', 'refreshOrders', 'device page can explicitly refresh orders'],
  ['miniapp/src/pages/device/select/index.vue', ':class="activeTheme.tone"', 'device page follows event theme tone'],
  ['miniapp/src/pages/device/select/index.vue', '--accent', 'device page uses event theme accent variables'],
  ['miniapp/src/pages/device/select/index.vue', 'fetchBanquetEventType', 'device page resolves event theme from current banquet'],
  ['miniapp/src/pages/device/select/index.vue', '{{ activeTheme.mark }}', 'device page hero symbol follows current banquet type'],
  ['miniapp/src/pages/mine/index/index.vue', '绑定记录', 'mine device label'],
  ['miniapp/src/pages/mine/index/index.vue', '交付说明', 'mine delivery label'],
  ['miniapp/src/pages/mine/index/index.vue', 'openLatestBanquet', 'mine service opens latest banquet'],
  ['miniapp/src/pages/mine/index/index.vue', 'openLatestInvitation', 'mine service opens latest invitation'],
  ['miniapp/src/pages/mine/index/index.vue', 'openGiftRecords', 'mine gift records require latest banquet context'],
  ['miniapp/src/pages/mine/index/index.vue', 'safeNavigate', 'mine workflow entries have navigation failure feedback'],
  ['miniapp/src/pages/mine/index/index.vue', '--accent', 'mine tab uses event theme accent variables']
];

for (const [file, text, reason] of requiredTexts) {
  requireText(file, text, reason);
}

for (const file of [
  'miniapp/src/pages/home/index/index.vue',
  'miniapp/src/pages/favor/index/index.vue',
  'miniapp/src/pages/invitation/index/index.vue',
  'miniapp/src/pages/mine/index/index.vue'
]) {
  requireText(file, '<swiper class="banner-card"', 'tab banner supports swipe');
  requireText(file, '@tap="handleBanner', 'tab banner is tappable');
}

for (const file of walk(path.join(miniappSrc, 'pages'))) {
  const rel = path.relative(repoRoot, file);
  forbidPattern(rel, /功能完善中|后续接入|P1/, 'avoid vague engineering-stage copy in user-facing miniapp pages');
}

for (const file of [
  'miniapp/src/pages/banquet/detail/index.vue',
  'miniapp/src/pages/gift/offline/index.vue',
  'miniapp/src/pages/gift/pay/index.vue',
  'miniapp/src/pages/gift/list/index.vue',
  'miniapp/src/pages/gift/success/index.vue',
  'miniapp/src/pages/rsvp/submit/index.vue',
  'miniapp/src/pages/rsvp/stats/index.vue',
  'miniapp/src/pages/invite/public/index.vue'
]) {
  requireText(file, 'event-theme', 'secondary flow imports shared event theme semantics');
}

if (failures.length) {
  console.error('Miniapp experience check failed:');
  for (const failure of failures) {
    console.error(`- ${failure}`);
  }
  process.exit(1);
}

console.log(`Miniapp experience check passed. Assertions: ${requiredTexts.length}.`);
