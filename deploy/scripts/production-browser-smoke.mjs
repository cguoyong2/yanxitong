#!/usr/bin/env node
import { createRequire } from 'node:module';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';

const require = createRequire(new URL('../../admin/package.json', import.meta.url));
const { chromium } = require('playwright-core');

const baseUrl = (process.env.BASE_URL || 'https://yxt.yqej.cn').replace(/\/$/, '');
const adminUsername = process.env.ADMIN_USERNAME || 'admin';
const adminPassword = process.env.ADMIN_PASSWORD;
const shareSlug = process.env.SHARE_SLUG || '';
const runId = new Date().toISOString().replace(/[-:TZ.]/g, '').slice(0, 14);
const artifactsDir = process.env.ARTIFACTS_DIR || path.join(os.tmpdir(), `yanxitong-production-browser-smoke-${runId}`);

const adminRoutes = [
  { path: '/dashboard', name: 'dashboard', text: '情礼记运营后台' },
  { path: '/config', name: 'config', text: '配置' },
  { path: '/event-types', name: 'event-types', text: '宴席' },
  { path: '/themes', name: 'themes', text: '主题' },
  { path: '/theme-copywriting', name: 'theme-copywriting', text: '文案' },
  { path: '/plans', name: 'plans', text: '版本' },
  { path: '/templates', name: 'templates', text: '模板' },
  { path: '/devices', name: 'devices', text: '设备' },
  { path: '/banquets', name: 'banquets', text: '宴席' },
  { path: '/business', name: 'business', text: '业务' },
  { path: '/payments', name: 'payments', text: '支付' },
  { path: '/broadcast-logs', name: 'broadcast-logs', text: '播报' },
  { path: '/operation-logs', name: 'operation-logs', text: '操作' }
];

function requireEnv(name, value) {
  if (!value) {
    throw new Error(`${name} is required for production browser smoke.`);
  }
}

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
    await page.waitForLoadState('networkidle', { timeout: 6000 });
  } catch {
    // Some pages may keep long polling or websocket connections open.
  }
  await page.waitForTimeout(300);
}

async function fillLogin(page) {
  const usernameByLabel = page.getByRole('textbox', { name: '账号' });
  const passwordByLabel = page.getByRole('textbox', { name: '密码' });
  if ((await usernameByLabel.count()) > 0 && (await passwordByLabel.count()) > 0) {
    await usernameByLabel.first().fill(adminUsername);
    await passwordByLabel.first().fill(adminPassword);
    return;
  }
  await page.getByPlaceholder('admin').first().fill(adminUsername);
  await page.getByPlaceholder('admin123').first().fill(adminPassword);
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

function collectRuntime(page) {
  const consoleEvents = [];
  const requestEvents = [];
  page.on('console', (message) => {
    if (['error', 'warning'].includes(message.type())) {
      consoleEvents.push({ type: message.type(), text: message.text().slice(0, 500), url: page.url() });
    }
  });
  page.on('requestfailed', (request) => {
    requestEvents.push({ url: request.url(), error: request.failure()?.errorText || 'failed' });
  });
  page.on('response', (response) => {
    if (response.status() >= 400) {
      requestEvents.push({ url: response.url(), status: response.status() });
    }
  });
  return { consoleEvents, requestEvents };
}

function result(name, snapshot, failures, extra = {}) {
  return {
    name,
    ok: failures.length === 0,
    failures,
    url: snapshot.url,
    bodyTextLength: snapshot.bodyTextLength,
    visibleElements: snapshot.visibleElements,
    documentWidth: snapshot.documentWidth,
    viewportWidth: snapshot.viewportWidth,
    ...extra
  };
}

function pageFailures(snapshot, expectedText) {
  const failures = [];
  if (snapshot.bodyTextLength < 10 || snapshot.visibleElements < 5) {
    failures.push('page appears blank');
  }
  if (!snapshot.bodyText.includes(expectedText)) {
    failures.push(`expected text not found: ${expectedText}`);
  }
  if (snapshot.hasHorizontalOverflow) {
    failures.push(`document overflow: ${snapshot.documentWidth}px > ${snapshot.viewportWidth}px`);
  }
  return failures;
}

async function main() {
  requireEnv('ADMIN_PASSWORD', adminPassword);
  const executablePath = chromeExecutablePath();
  if (!executablePath) {
    throw new Error('Chrome executable not found. Set CHROME_PATH to a Chrome/Chromium executable.');
  }

  fs.mkdirSync(artifactsDir, { recursive: true });
  const browser = await chromium.launch({
    executablePath,
    headless: process.env.HEADLESS !== '0'
  });
  const context = await browser.newContext({
    viewport: { width: 1440, height: 1000 },
    ignoreHTTPSErrors: true
  });
  const page = await context.newPage();
  const { consoleEvents, requestEvents } = collectRuntime(page);
  const results = [];

  try {
    await page.goto(`${baseUrl}/login`, { waitUntil: 'domcontentloaded', timeout: 30000 });
    await fillLogin(page);
    await Promise.all([
      page.waitForURL((url) => !url.pathname.includes('/login'), { timeout: 15000 }),
      page.getByRole('button', { name: '登录' }).click()
    ]);
    await waitForSettledPage(page);
    const loginSnapshot = await pageSnapshot(page);
    results.push(result('admin-login', loginSnapshot, pageFailures(loginSnapshot, '情礼记运营后台')));

    for (const route of adminRoutes) {
      await page.goto(`${baseUrl}${route.path}`, { waitUntil: 'domcontentloaded', timeout: 30000 });
      await waitForSettledPage(page);
      const snapshot = await pageSnapshot(page);
      results.push(result(`admin-${route.name}`, snapshot, pageFailures(snapshot, route.text)));
    }

    await page.goto(`${baseUrl}/confirm-screen/bind`, { waitUntil: 'domcontentloaded', timeout: 30000 });
    await waitForSettledPage(page);
    const confirmSnapshot = await pageSnapshot(page);
    results.push(result('confirm-screen-bind', confirmSnapshot, pageFailures(confirmSnapshot, '绑定确认屏')));

    if (shareSlug) {
      const response = await page.goto(`${baseUrl}/api/invitations/public/${encodeURIComponent(shareSlug)}`, {
        waitUntil: 'domcontentloaded',
        timeout: 30000
      });
      const snapshot = await pageSnapshot(page);
      const failures = [];
      if (!response || response.status() >= 400) {
        failures.push(`public invitation api status: ${response?.status() || 'missing'}`);
      }
      if (!snapshot.bodyText.includes(shareSlug)) {
        failures.push('public invitation payload does not include share slug');
      }
      results.push(result('public-invitation-api', snapshot, failures, { status: response?.status() }));
    }
  } finally {
    await browser.close();
  }

  const blockingConsole = consoleEvents.filter((item) => item.type === 'error');
  const blockingRequests = requestEvents.filter((item) => {
    if (item.url.endsWith('/favicon.ico')) {
      return false;
    }
    return item.status >= 400 || item.error;
  });
  if (blockingConsole.length > 0 || blockingRequests.length > 0) {
    results.push({
      name: 'runtime-events',
      ok: false,
      failures: [
        ...blockingConsole.map((item) => `console error: ${item.text}`),
        ...blockingRequests.map((item) => `request failure: ${item.status || item.error} ${item.url}`)
      ]
    });
  }

  const summary = {
    baseUrl,
    shareSlug: shareSlug || null,
    artifactsDir,
    total: results.length,
    passed: results.filter((item) => item.ok).length,
    failed: results.filter((item) => !item.ok).length,
    consoleEvents,
    requestEvents,
    results
  };
  fs.writeFileSync(path.join(artifactsDir, 'summary.json'), JSON.stringify(summary, null, 2));

  if (summary.failed > 0) {
    console.error(`Production browser smoke failed. Artifacts: ${artifactsDir}`);
    for (const item of results.filter((entry) => !entry.ok)) {
      console.error(`- ${item.name}: ${(item.failures || []).join('; ')}`);
    }
    process.exit(1);
  }

  console.log(`Production browser smoke passed. Artifacts: ${artifactsDir}`);
  for (const item of results) {
    console.log(`- ${item.name}: ${item.url || 'runtime'}`);
  }
}

main().catch((error) => {
  console.error(error instanceof Error ? error.message : String(error));
  process.exit(1);
});
