#!/usr/bin/env node
import { createRequire } from 'node:module';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';

const require = createRequire(new URL('../../admin/package.json', import.meta.url));
const { chromium } = require('playwright-core');

const adminUrl = process.env.ADMIN_URL || 'http://127.0.0.1:5173';
const username = process.env.ADMIN_USERNAME || 'admin';
const password = process.env.ADMIN_PASSWORD || 'admin123';
const banquetId = process.env.BANQUET_ID || '';
const runId = new Date().toISOString().replace(/[-:TZ.]/g, '').slice(0, 14);
const artifactsDir = process.env.ARTIFACTS_DIR || path.join(os.tmpdir(), `yanxitong-admin-smoke-${runId}`);
const captureScreenshots = process.env.CAPTURE_SUCCESS_SCREENSHOTS !== '0';
const repoRoot = path.resolve(new URL('../..', import.meta.url).pathname);

const sourceAssertions = [
  ['admin/src/views/orders/OrdersView.vue', 'deviceStatus', 'orders page can filter device fulfillment status'],
  ['admin/src/views/orders/OrdersView.vue', 'planOrderNextStep', 'plan orders show operational next step'],
  ['admin/src/views/orders/OrdersView.vue', 'deviceOrderNextStep', 'device orders show operational next step'],
  ['admin/src/views/orders/OrdersView.vue', 'displayLabel(row.deliveryMethod)', 'device delivery method is localized for operators'],
  ['admin/src/views/orders/OrdersView.vue', "updateDeviceStatus(row.orderNo as string, 'CONFIRMED')", 'paid device orders can be confirmed by admin'],
  ['admin/src/views/orders/OrdersView.vue', '设备订单已更新为', 'device status update has clear admin feedback'],
  ['admin/src/views/orders/OrdersView.vue', 'goPayments(row.banquetId as number)', 'orders page links order rows to payment troubleshooting'],
  ['admin/src/views/payments/PaymentsView.vue', 'paymentOrderAdvice', 'payment orders show troubleshooting advice'],
  ['admin/src/views/payments/PaymentsView.vue', 'goOrderCenter(row)', 'payment orders link back to order center'],
  ['admin/src/views/payments/PaymentsView.vue', 'goPaymentOrderLog(row)', 'callback rows link to payment operation logs'],
  ['admin/src/views/operation-logs/OperationLogsView.vue', 'isPaymentContext', 'operation logs recognize payment troubleshooting context'],
  ['admin/src/views/operation-logs/OperationLogsView.vue', 'goPaymentTroubleshoot(row)', 'operation log rows link back to payment troubleshooting'],
  ['admin/src/views/business/BusinessView.vue', 'goAuditInvitations', 'business audit can drill into invitation analytics'],
  ['admin/src/views/business/BusinessView.vue', 'goAuditOrders', 'business audit can drill into order center'],
  ['admin/src/views/business/BusinessView.vue', 'goAuditBroadcast', 'business audit can drill into broadcast logs'],
  ['admin/src/views/business/BusinessView.vue', 'auditFailedCallbacks', 'business audit surfaces payment callback risks'],
  ['admin/src/views/business/BusinessView.vue', 'auditDeliveredDeviceCount', 'business audit surfaces device delivery progress'],
  ['deploy/scripts/production-acceptance-suite.sh', 'RUN_PRODUCTION_API', 'production acceptance suite can opt into API acceptance'],
  ['deploy/scripts/production-acceptance-suite.sh', 'RUN_MINIAPP_PREVIEW', 'production acceptance suite can opt into miniapp preview'],
  ['deploy/scripts/production-acceptance-suite.sh', 'summary.json', 'production acceptance suite writes consolidated summary']
];

const routes = [
  { path: '/dashboard', name: 'dashboard', text: '配置中心' },
  { path: '/config', name: 'config', text: '配置' },
  { path: '/event-types', name: 'event-types', text: '宴席' },
  { path: '/themes', name: 'themes', text: '主题' },
  { path: '/theme-copywriting', name: 'theme-copywriting', text: '文案' },
  { path: '/plans', name: 'plans', text: '版本' },
  { path: '/templates', name: 'templates', text: '模板' },
  { path: '/invitations', name: 'invitations', text: '请柬管理' },
  { path: '/devices', name: 'devices', text: '设备' },
  { path: banquetId ? `/banquets?banquetId=${encodeURIComponent(banquetId)}` : '/banquets', name: 'banquets', text: '宴席' },
  { path: '/business', name: 'business', text: '宴席核对总览' },
  { path: '/orders', name: 'orders', text: '订单' },
  { path: '/payments', name: 'payments', text: '支付' },
  { path: '/broadcast-logs', name: 'broadcast-logs', text: '播报' },
  { path: '/operation-logs', name: 'operation-logs', text: '操作' }
];

const rawEnumPatterns = [
  /\bONLINE_GIFT\b/,
  /\bONSITE_QR\b/,
  /\bCONFIRM_SCREEN\b/,
  /\bCLOUD_SPEAKER\b/,
  /\bEXPRESS\b/,
  /\bSELF_PICKUP\b/,
  /\bSTAFF_DELIVERY\b/,
  /\bONSITE\b/,
  /\bUNPAID\b/,
  /\bPAID\b/,
  /\bPENDING\b/,
  /\bFAILED\b/,
  /\bSIMULATED\b/,
  /\bATTENDING\b/,
  /\bDECLINED\b/,
  /\bRECEIVED\b/,
  /\bGIVEN\b/
];

function chromeExecutablePath() {
  if (process.env.CHROME_PATH) {
    return process.env.CHROME_PATH;
  }
  const candidates = [
    '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
    '/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge',
    '/Applications/Chromium.app/Contents/MacOS/Chromium',
    '/usr/bin/google-chrome',
    '/usr/bin/chromium',
    '/usr/bin/chromium-browser'
  ];
  return candidates.find((candidate) => fs.existsSync(candidate));
}

async function waitForSettledPage(page) {
  await page.waitForLoadState('domcontentloaded');
  try {
    await page.waitForLoadState('networkidle', { timeout: 5000 });
  } catch {
    // Long-lived connections are acceptable; core DOM checks below are authoritative.
  }
  await page.waitForTimeout(300);
}

async function pageSnapshot(page) {
  return page.evaluate(() => {
    const bodyText = document.body?.innerText || '';
    const documentWidth = document.documentElement.scrollWidth;
    const viewportWidth = document.documentElement.clientWidth;
    const visibleElements = Array.from(document.querySelectorAll('body *')).filter((element) => {
      const rect = element.getBoundingClientRect();
      const style = window.getComputedStyle(element);
      return rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden' && style.display !== 'none';
    }).length;
    return {
      title: document.title,
      bodyText,
      bodyTextLength: bodyText.trim().length,
      visibleElements,
      documentWidth,
      viewportWidth,
      hasHorizontalOverflow: documentWidth > viewportWidth + 2,
      url: window.location.href
    };
  });
}

function assertPage(route, snapshot, consoleErrors, requestFailures) {
  const failures = [];
  if (snapshot.bodyTextLength < 10 || snapshot.visibleElements < 5) {
    failures.push('page appears blank');
  }
  if (!snapshot.bodyText.includes(route.text)) {
    failures.push(`expected text not found: ${route.text}`);
  }
  if (snapshot.hasHorizontalOverflow) {
    failures.push(`document overflow: ${snapshot.documentWidth}px > ${snapshot.viewportWidth}px`);
  }
  for (const pattern of rawEnumPatterns) {
    if (pattern.test(snapshot.bodyText)) {
      failures.push(`raw enum leaked: ${pattern.source}`);
    }
  }
  const relevantConsoleErrors = consoleErrors.filter((item) => item.type === 'error');
  if (relevantConsoleErrors.length > 0) {
    failures.push(`console errors: ${relevantConsoleErrors.map((item) => item.text).join(' | ')}`);
  }
  const relevantRequestFailures = requestFailures.filter((item) => {
    return item.url.includes('/api/') || item.url.startsWith(adminUrl);
  });
  if (relevantRequestFailures.length > 0) {
    failures.push(`request failures: ${relevantRequestFailures.map((item) => `${item.status || item.error} ${item.url}`).join(' | ')}`);
  }
  return failures;
}

function assertSourceExperience() {
  const failures = [];
  for (const [file, text, reason] of sourceAssertions) {
    const fullPath = path.join(repoRoot, file);
    const source = fs.readFileSync(fullPath, 'utf8');
    if (!source.includes(text)) {
      failures.push(`${file}: missing "${text}" (${reason})`);
    }
  }
  if (failures.length > 0) {
    throw new Error(`Admin source smoke failed:\n${failures.join('\n')}`);
  }
}

async function main() {
  assertSourceExperience();
  if (process.env.ADMIN_SOURCE_ONLY === '1') {
    console.log(`Admin source smoke passed. Assertions: ${sourceAssertions.length}.`);
    return;
  }

  const executablePath = chromeExecutablePath();
  if (!executablePath) {
    throw new Error('Chrome executable not found. Set CHROME_PATH to a Chrome/Chromium executable.');
  }

  fs.mkdirSync(artifactsDir, { recursive: true });
  const screenshotDir = path.join(artifactsDir, 'screenshots');
  fs.mkdirSync(screenshotDir, { recursive: true });

  const browser = await chromium.launch({
    executablePath,
    headless: process.env.HEADLESS !== '0'
  });
  const context = await browser.newContext({
    viewport: { width: 1280, height: 900 },
    ignoreHTTPSErrors: true
  });
  const page = await context.newPage();
  const results = [];

  try {
    await page.goto(`${adminUrl}/login`, { waitUntil: 'domcontentloaded' });
    await page.getByPlaceholder('admin').first().fill(username);
    await page.getByPlaceholder('admin123').first().fill(password);
    await Promise.all([
      page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 10000 }),
      page.getByRole('button', { name: '登录' }).click()
    ]);
    await waitForSettledPage(page);

    for (const route of routes) {
      const consoleErrors = [];
      const requestFailures = [];
      const onConsole = (message) => {
        if (message.type() === 'error') {
          consoleErrors.push({ type: message.type(), text: message.text() });
        }
      };
      const onRequestFailed = (request) => {
        requestFailures.push({ url: request.url(), error: request.failure()?.errorText || 'failed' });
      };
      const onResponse = (response) => {
        if (response.status() >= 400) {
          requestFailures.push({ url: response.url(), status: response.status() });
        }
      };

      page.on('console', onConsole);
      page.on('requestfailed', onRequestFailed);
      page.on('response', onResponse);

      await page.goto(`${adminUrl}${route.path}`, { waitUntil: 'domcontentloaded' });
      await waitForSettledPage(page);
      const snapshot = await pageSnapshot(page);
      const failures = assertPage(route, snapshot, consoleErrors, requestFailures);
      const result = {
        name: route.name,
        path: route.path,
        url: snapshot.url,
        ok: failures.length === 0,
        failures,
        screenshot: null,
        bodyTextLength: snapshot.bodyTextLength,
        visibleElements: snapshot.visibleElements,
        documentWidth: snapshot.documentWidth,
        viewportWidth: snapshot.viewportWidth
      };
      results.push(result);

      if (captureScreenshots || !result.ok) {
        result.screenshot = path.join(screenshotDir, `${route.name}.png`);
        await page.screenshot({ path: result.screenshot, fullPage: true });
      }

      page.off('console', onConsole);
      page.off('requestfailed', onRequestFailed);
      page.off('response', onResponse);
    }
  } finally {
    await browser.close();
  }

  const summary = {
    adminUrl,
    banquetId: banquetId || null,
    artifactsDir,
    total: results.length,
    passed: results.filter((result) => result.ok).length,
    failed: results.filter((result) => !result.ok).length,
    screenshots: results.map((result) => result.screenshot).filter(Boolean),
    results
  };
  fs.writeFileSync(path.join(artifactsDir, 'summary.json'), JSON.stringify(summary, null, 2));

  if (summary.failed > 0) {
    console.error(`Admin frontend smoke failed. Artifacts: ${artifactsDir}`);
    for (const result of results.filter((item) => !item.ok)) {
      console.error(`- ${result.name}: ${result.failures.join('; ')}`);
    }
    process.exit(1);
  }

  console.log(`Admin frontend smoke passed. Artifacts: ${artifactsDir}`);
  for (const result of results) {
    console.log(`- ${result.name}: ${result.url}`);
  }
}

main().catch((error) => {
  console.error(error instanceof Error ? error.message : String(error));
  process.exit(1);
});
