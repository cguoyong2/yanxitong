#!/usr/bin/env node

import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(scriptDir, '..', '..');
const miniappRoot = path.join(repoRoot, 'miniapp');
const srcRoot = path.join(miniappRoot, 'src');
const pagesJsonPath = path.join(srcRoot, 'pages.json');
const failures = [];
let checkedHandlers = 0;
let checkedContracts = 0;

const criticalContracts = [
  {
    file: 'miniapp/src/pages/banquet/detail/index.vue',
    name: 'banquet management actions',
    events: [
      '@tap="publishBanquet()"',
      '@tap="openInvite()"',
      '@tap="editInvite()"',
      '@tap="openPlan()"',
      '@tap="openDevice()"'
    ],
    markers: [
      'function publishBanquet()',
      'function openInvite()',
      'function editInvite()',
      'function openPlan()',
      'function openDevice()',
      '/pages/invite/public/index',
      '/pages/invite/edit-basic/index',
      '/pages/order/plan/index',
      '/pages/device/select/index'
    ]
  },
  {
    file: 'miniapp/src/pages/invite/edit-basic/index.vue',
    name: 'invitation editing actions',
    events: [
      '@tap.stop="previewInvite()"',
      '@tap.stop="copyShareUrl()"',
      '@tap.stop="returnBanquetDetail()"',
      '@tap.stop="saveAndPreview()"',
      '@tap.stop="submit()"'
    ],
    markers: [
      'function previewInvite()',
      'function copyShareUrl()',
      'function returnBanquetDetail()',
      'function saveAndPreview()',
      'async function submit()',
      'safeNavigate(shareUrl.value,',
      '/pages/banquet/detail/index'
    ]
  },
  {
    file: 'miniapp/src/pages/rsvp/stats/index.vue',
    name: 'RSVP statistics actions',
    events: [
      '@tap="refreshStats()"',
      '@tap="shareInvite()"',
      '@tap="openBanquetDetail()"',
      '@tap="copyInvitePath()"'
    ],
    markers: [
      'async function refreshStats()',
      'function shareInvite()',
      'function openBanquetDetail()',
      'function copyInvitePath()',
      '/pages/invite/public/index',
      '/pages/banquet/detail/index'
    ]
  },
  {
    file: 'miniapp/src/pages/rsvp/submit/index.vue',
    name: 'RSVP submission actions',
    events: [
      '@tap="submit()"',
      '@tap="openGift()"',
      '@tap="openRsvpStats()"',
      '@tap="backToInvitation()"'
    ],
    markers: [
      'async function submit()',
      'function openGift()',
      'function openRsvpStats()',
      'function backToInvitation()',
      '/pages/gift/pay/index',
      '/pages/rsvp/stats/index',
      'safeNavigate(shareUrl.value,'
    ]
  },
  {
    file: 'miniapp/src/pages/gift/offline/index.vue',
    name: 'offline gift actions',
    events: [
      '@tap="submit()"',
      '@tap="openGiftList()"',
      '@tap="openLastFavor()"',
      '@tap="continueRegistration()"'
    ],
    markers: [
      'async function submit()',
      'function openGiftList()',
      'async function openLastFavor()',
      'function continueRegistration()',
      '/pages/gift/list/index',
      '/pages/favor/index/index'
    ]
  },
  {
    file: 'miniapp/src/pages/order/plan/index.vue',
    name: 'plan order real payment actions',
    events: [
      '@tap="openPaymentPanel(pendingOrder)"',
      '@tap="payOrder(paymentPanel.order)"',
      '@tap="returnBanquetDetail()"'
    ],
    markers: [
      'async function createOrder(',
      'function openPaymentPanel(',
      'async function payOrder(',
      '/plans/orders/${order.orderNo}/payment',
      'requestWechatPayment',
      'function returnBanquetDetail()',
      '/plans/orders',
      '/pages/banquet/detail/index'
    ]
  },
  {
    file: 'miniapp/src/pages/device/select/index.vue',
    name: 'device order real payment actions',
    events: [
      '@tap="refreshOrders()"',
      '@tap="openPaymentPanel(order)"',
      '@tap="payOrder(paymentPanel.order)"',
      '@tap="returnBanquetDetail()"'
    ],
    markers: [
      'async function createOrder(',
      'async function refreshOrders()',
      'function openPaymentPanel(',
      'async function payOrder(',
      '/devices/orders/${order.orderNo}/payment',
      'requestWechatPayment',
      'function returnBanquetDetail()',
      '/devices/orders',
      '/pages/banquet/detail/index'
    ]
  },
  {
    file: 'miniapp/src/pages/favor/index/index.vue',
    name: 'favor overview actions',
    events: [
      "@tap=\"setManualDirection('RECEIVED')\"",
      "@tap=\"setManualDirection('GIVEN')\"",
      '@tap="scrollToRecent()"',
      '@tap="setCompareFromKeyword()"',
      '@tap="openCompareMore()"',
      '@tap="openCompareDetail()"'
    ],
    markers: [
      'function setManualDirection(',
      'function scrollToRecent()',
      'function setCompareFromKeyword()',
      'function openCompareMore()',
      'async function openCompareDetail()',
      '/pages/favor/detail/index'
    ]
  },
  {
    file: 'miniapp/src/pages/mine/index/index.vue',
    name: 'mine order and service actions',
    events: [
      '@tap="refreshOrderSummary()"',
      '@tap="openPlanOrders()"',
      '@tap="openDeviceOrders()"',
      '@tap="openRecentOrder(item)"',
      '@tap="openOrderSource()"',
      '@tap="openCustomerService()"'
    ],
    markers: [
      'async function refreshOrderSummary()',
      'function openPlanOrders(',
      'function openDeviceOrders(',
      'function openRecentOrder(',
      'function openOrderSource()',
      'function openCustomerService()',
      '/pages/order/plan/index',
      '/pages/device/select/index',
      '/pages/support/customer-service/index'
    ]
  },
  {
    file: 'miniapp/src/pages/support/customer-service/index.vue',
    name: 'enterprise wechat acquisition fallback',
    events: [
      '@tap="previewQrCode()"',
      '@tap="copyCustomerServiceLink()"',
      '@tap="loadCustomerService()"'
    ],
    markers: [
      'function previewQrCode()',
      'function copyCustomerServiceLink()',
      'async function loadCustomerService()',
      "uni.previewImage({",
      "uni.setClipboardData({",
      '<customer-service-qr-code',
      '/meta/customer-service'
    ]
  }
];

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

function relative(file) {
  return path.relative(repoRoot, file);
}

function normalizeRoute(route) {
  return route.split('?')[0].split('${')[0].replace(/^\/+/, '');
}

function lineNumber(source, index) {
  return source.slice(0, index).split(/\r?\n/).length;
}

function hasDeclaration(source, name) {
  const escaped = name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  return [
    new RegExp(`\\b(?:async\\s+)?function\\s+${escaped}\\s*\\(`),
    new RegExp(`\\b(?:const|let|var)\\s+${escaped}\\b`),
    new RegExp(`\\bimport\\s*\\{[^}]*\\b${escaped}\\b[^}]*\\}`)
  ].some((pattern) => pattern.test(source));
}

const pagesJson = readJson(pagesJsonPath);
const registeredPages = new Set((pagesJson.pages || []).map((item) => item.path));

for (const file of walk(srcRoot)) {
  const source = fs.readFileSync(file, 'utf8');
  const filePath = relative(file);

  const eventPattern = /@(tap|click)(?:\.[\w-]+)*="([^"]+)"/g;
  let eventMatch;
  while ((eventMatch = eventPattern.exec(source)) !== null) {
    const expression = eventMatch[2].trim();
    if (/^[A-Za-z_$][\w$]*$/.test(expression)) {
      failures.push({
        file: filePath,
        line: lineNumber(source, eventMatch.index),
        reason: `@${eventMatch[1]} uses bare method "${expression}"; use "${expression}()" so mp-weixin executes the handler`
      });
    }

    const calledHandlers = [...expression.matchAll(/\b([A-Za-z_$][\w$]*)\s*\(/g)]
      .map((match) => match[1])
      .filter((name) => !['if', 'for', 'while', 'switch', 'catch'].includes(name));
    for (const handler of new Set(calledHandlers)) {
      checkedHandlers += 1;
      if (!hasDeclaration(source, handler)) {
        failures.push({
          file: filePath,
          line: lineNumber(source, eventMatch.index),
          reason: `template event calls "${handler}()" but no matching function or binding exists`
        });
      }
    }
  }

  const routePattern = /['"`](\/pages\/[^'"`?\s]+)(?:\?[^'"`]*)?['"`]/g;
  let routeMatch;
  while ((routeMatch = routePattern.exec(source)) !== null) {
    const route = routeMatch[1];
    const normalized = normalizeRoute(route);
    if (!registeredPages.has(normalized)) {
      failures.push({
        file: filePath,
        line: lineNumber(source, routeMatch.index),
        reason: `route "${route}" is not registered in miniapp/src/pages.json`
      });
    }
  }
}

for (const contract of criticalContracts) {
  const fullPath = path.join(repoRoot, contract.file);
  if (!fs.existsSync(fullPath)) {
    failures.push({
      file: contract.file,
      line: 1,
      reason: `critical interaction contract "${contract.name}" page does not exist`
    });
    continue;
  }

  const source = fs.readFileSync(fullPath, 'utf8');
  for (const expected of [...contract.events, ...contract.markers]) {
    checkedContracts += 1;
    if (!source.includes(expected)) {
      failures.push({
        file: contract.file,
        line: 1,
        reason: `critical interaction contract "${contract.name}" is missing "${expected}"`
      });
    }
  }
}

if (failures.length) {
  console.error('Miniapp interaction check failed:');
  for (const failure of failures) {
    console.error(`- ${failure.file}:${failure.line}: ${failure.reason}`);
  }
  process.exit(1);
}

console.log(
  `Miniapp interaction check passed. Checked ${registeredPages.size} registered pages, `
  + `${checkedHandlers} template handler calls and ${checkedContracts} critical interaction assertions.`
);
