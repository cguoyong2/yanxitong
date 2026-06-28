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
  ['miniapp/src/pages/banquet/create/index.vue', '填入体验数据', 'create page sample-data helper'],
  ['miniapp/src/pages/banquet/create/index.vue', '慎终追远，思念长存', 'create page memorial copy'],
  ['miniapp/src/pages/banquet/create/index.vue', 'currentDesign.mark', 'create page hero mark follows event type'],
  ['miniapp/src/pages/banquet/create/index.vue', '当前使用模板封面，自定义上传稍后开放', 'cover upload boundary'],
  ['miniapp/src/pages/banquet/detail/index.vue', '线上随礼暂未开放', 'detail non-payment boundary'],
  ['miniapp/src/pages/banquet/detail/index.vue', '请柬公开页打开失败', 'detail invitation navigation feedback'],
  ['miniapp/src/pages/banquet/detail/index.vue', 'detailDesign.mark', 'detail hero mark follows banquet event type'],
  ['miniapp/src/pages/invitation/index/index.vue', '暂无符合条件的模板', 'template empty state'],
  ['miniapp/src/pages/invitation/index/index.vue', '定制请柬服务将在后续版本开放', 'custom invitation boundary'],
  ['miniapp/src/pages/invite/edit-basic/index.vue', '复制路径', 'share path copy'],
  ['miniapp/src/pages/invite/edit-basic/index.vue', '保存请柬失败', 'invitation edit save feedback'],
  ['miniapp/src/pages/invite/public/index.vue', '在线随礼需完成微信支付配置后开放', 'public invitation payment boundary'],
  ['miniapp/src/pages/invite/public/index.vue', '回执页面打开失败', 'public invitation RSVP navigation feedback'],
  ['miniapp/src/pages/rsvp/submit/index.vue', '返回请柬', 'RSVP return action'],
  ['miniapp/src/pages/rsvp/submit/index.vue', '去线下记礼', 'non-payment RSVP success action'],
  ['miniapp/src/pages/rsvp/stats/index.vue', 'shareSlug', 'RSVP stats can return to public invitation'],
  ['miniapp/src/pages/rsvp/stats/index.vue', 'safeNavigate(`/pages/banquet/detail/index?id=${banquetId.value}`', 'RSVP stats returns to banquet detail'],
  ['miniapp/src/pages/gift/offline/index.vue', '继续登记', 'offline gift success modal'],
  ['miniapp/src/pages/gift/offline/index.vue', '查看记录', 'offline gift record navigation'],
  ['miniapp/src/pages/gift/offline/index.vue', '保存记礼失败', 'offline gift save feedback'],
  ['miniapp/src/pages/gift/offline/index.vue', '最近保存成功', 'offline gift visible success state'],
  ['miniapp/src/pages/favor/family/index.vue', '家庭协作功能将在后续版本开放', 'family favor boundary'],
  ['miniapp/src/pages/favor/index/index.vue', "setManualDirection('RECEIVED')", 'favor received card action'],
  ['miniapp/src/pages/favor/index/index.vue', "setManualDirection('GIVEN')", 'favor given card action'],
  ['miniapp/src/pages/favor/index/index.vue', 'grid-template-columns: 1fr', 'favor cards do not overflow'],
  ['miniapp/src/pages/favor/index/index.vue', 'display: block;', 'favor manual form is visible'],
  ['miniapp/src/pages/favor/index/index.vue', 'showAllRecent', 'favor recent more expands list'],
  ['miniapp/src/pages/favor/index/index.vue', 'openCompareDetail', 'favor compare can open detail'],
  ['miniapp/src/pages/favor/index/index.vue', '{{ activeTheme.mark }}', 'favor tab hero follows selected event type'],
  ['miniapp/src/pages/favor/index/index.vue', '{{ activeTheme.favorText }}', 'favor tab copy follows selected event type'],
  ['miniapp/src/pages/invitation/index/index.vue', '{{ activeTheme.mark }}', 'invitation tab hero follows selected event type'],
  ['miniapp/src/pages/invitation/index/index.vue', '{{ activeTheme.invitationText }}', 'invitation tab copy follows selected event type'],
  ['miniapp/src/pages/invitation/index/index.vue', 'writeActiveEventType(code)', 'invitation type selector persists event type'],
  ['miniapp/src/pages/mine/index/index.vue', '{{ activeTheme.mark }}', 'mine tab hero follows selected event type'],
  ['miniapp/src/pages/mine/index/index.vue', '{{ activeTheme.mineText }}', 'mine tab copy follows selected event type'],
  ['miniapp/src/pages/order/plan/index.vue', '/plans/orders?banquetId=', 'plan orders are visible after creation'],
  ['miniapp/src/pages/order/plan/index.vue', 'cacheOrder(order)', 'plan orders remain visible if list API is unavailable'],
  ['miniapp/src/pages/mine/index/index.vue', '绑定记录', 'mine device label'],
  ['miniapp/src/pages/mine/index/index.vue', '交付说明', 'mine delivery label'],
  ['miniapp/src/pages/mine/index/index.vue', 'openLatestBanquet', 'mine service opens latest banquet'],
  ['miniapp/src/pages/mine/index/index.vue', 'openLatestInvitation', 'mine service opens latest invitation']
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

if (failures.length) {
  console.error('Miniapp experience check failed:');
  for (const failure of failures) {
    console.error(`- ${failure}`);
  }
  process.exit(1);
}

console.log(`Miniapp experience check passed. Assertions: ${requiredTexts.length}.`);
