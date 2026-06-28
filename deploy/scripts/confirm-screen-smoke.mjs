#!/usr/bin/env node
import { createRequire } from 'node:module';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';

const require = createRequire(new URL('../../confirm-screen/package.json', import.meta.url));
const { chromium } = require('playwright-core');

const confirmScreenUrl = process.env.CONFIRM_SCREEN_URL || 'http://127.0.0.1:5174/confirm-screen';
const banquetId = process.env.BANQUET_ID;
const bindCode = process.env.CONFIRM_SCREEN_BIND_CODE;
const runId = new Date().toISOString().replace(/[-:TZ.]/g, '').slice(0, 14);
const artifactsDir = process.env.ARTIFACTS_DIR || path.join(os.tmpdir(), `yanxitong-confirm-screen-smoke-${runId}`);

const viewports = [
  { name: 'desktop', width: 1280, height: 900 },
  { name: 'mobile', width: 390, height: 844 }
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
    await page.waitForLoadState('networkidle', { timeout: 2500 });
  } catch {
    // The standby page keeps a WebSocket open; DOM checks below are authoritative.
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

function assertStep(step, snapshot, expectedText, extraChecks = []) {
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
  for (const check of extraChecks) {
    if (!snapshot.bodyText.includes(check)) {
      failures.push(`expected text not found: ${check}`);
    }
  }
  return {
    step,
    url: snapshot.url,
    ok: failures.length === 0,
    failures,
    bodyTextLength: snapshot.bodyTextLength,
    visibleElements: snapshot.visibleElements,
    documentWidth: snapshot.documentWidth,
    viewportWidth: snapshot.viewportWidth
  };
}

function assertRuntimeFailures(result, consoleErrors, requestFailures) {
  const relevantConsoleErrors = consoleErrors.filter((item) => {
    return item.type === 'error' && !item.text.includes('Failed to load resource');
  });
  if (relevantConsoleErrors.length > 0) {
    result.ok = false;
    result.failures.push(`console errors: ${relevantConsoleErrors.map((item) => item.text).join(' | ')}`);
  }
  const relevantRequestFailures = requestFailures.filter((item) => {
    return (
      item.url.startsWith(confirmScreenUrl) &&
      !item.url.includes('/ws/confirm-screen') &&
      !item.url.endsWith('/favicon.ico')
    );
  });
  if (relevantRequestFailures.length > 0) {
    result.ok = false;
    result.failures.push(`request failures: ${relevantRequestFailures.map((item) => `${item.status || item.error} ${item.url}`).join(' | ')}`);
  }
}

async function captureStep(page, viewportName, step, expectedText, extraChecks = []) {
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

  await waitForSettledPage(page);
  const snapshot = await pageSnapshot(page);
  const result = assertStep(`${viewportName}-${step}`, snapshot, expectedText, extraChecks);
  assertRuntimeFailures(result, consoleErrors, requestFailures);
  await page.screenshot({ path: path.join(artifactsDir, `${viewportName}-${step}.png`), fullPage: true });

  page.off('console', onConsole);
  page.off('requestfailed', onRequestFailed);
  page.off('response', onResponse);
  return result;
}

async function verifyViewport(browser, viewport) {
  const context = await browser.newContext({
    viewport: { width: viewport.width, height: viewport.height },
    ignoreHTTPSErrors: true
  });
  const page = await context.newPage();
  const results = [];

  try {
    await page.goto(`${confirmScreenUrl}/bind`, { waitUntil: 'domcontentloaded' });
    results.push(await captureStep(page, viewport.name, 'bind', '绑定确认屏'));

    await page.goto(
      `${confirmScreenUrl}/standby?banquetId=${encodeURIComponent(banquetId)}&bindCode=${encodeURIComponent(bindCode)}`,
      { waitUntil: 'domcontentloaded' }
    );
    results.push(await captureStep(page, viewport.name, 'standby', '等待来宾随礼', [`宴席 ${banquetId}`, bindCode]));

    const latestButton = page.getByRole('button', { name: '查看最近成功事件' });
    if ((await latestButton.count()) === 0) {
      results.push({
        step: `${viewport.name}-success`,
        url: page.url(),
        ok: false,
        failures: ['latest gift event button not found'],
        bodyTextLength: 0,
        visibleElements: 0,
        documentWidth: 0,
        viewportWidth: viewport.width
      });
    } else {
      await latestButton.first().click();
      results.push(await captureStep(page, viewport.name, 'success', '礼金到账', ['秒后返回待机页']));
    }

    await page.goto(`${confirmScreenUrl}/offline`, { waitUntil: 'domcontentloaded' });
    results.push(await captureStep(page, viewport.name, 'offline', '确认屏离线', [`宴席 ${banquetId}`, bindCode]));
  } finally {
    await context.close();
  }

  return results;
}

async function main() {
  if (!banquetId || !bindCode) {
    throw new Error('BANQUET_ID and CONFIRM_SCREEN_BIND_CODE are required.');
  }

  const executablePath = chromeExecutablePath();
  if (!executablePath) {
    throw new Error('Chrome executable not found. Set CHROME_PATH to a Chrome/Chromium executable.');
  }

  fs.mkdirSync(artifactsDir, { recursive: true });

  const browser = await chromium.launch({
    executablePath,
    headless: process.env.HEADLESS !== '0'
  });

  const results = [];
  try {
    for (const viewport of viewports) {
      results.push(...(await verifyViewport(browser, viewport)));
    }
  } finally {
    await browser.close();
  }

  const summary = {
    confirmScreenUrl,
    banquetId,
    bindCode,
    artifactsDir,
    total: results.length,
    passed: results.filter((result) => result.ok).length,
    failed: results.filter((result) => !result.ok).length,
    results
  };
  fs.writeFileSync(path.join(artifactsDir, 'summary.json'), JSON.stringify(summary, null, 2));

  if (summary.failed > 0) {
    console.error(`Confirm-screen smoke failed. Artifacts: ${artifactsDir}`);
    for (const result of results.filter((item) => !item.ok)) {
      console.error(`- ${result.step}: ${result.failures.join('; ')}`);
    }
    process.exit(1);
  }

  console.log(`Confirm-screen smoke passed. Artifacts: ${artifactsDir}`);
  for (const result of results) {
    console.log(`- ${result.step}: ${result.url}`);
  }
}

main().catch((error) => {
  console.error(error instanceof Error ? error.message : String(error));
  process.exit(1);
});
