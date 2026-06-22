#!/usr/bin/env node
import { createRequire } from 'node:module';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';

const require = createRequire(new URL('../../admin/package.json', import.meta.url));
const { chromium } = require('playwright-core');

const baseUrl = process.env.BASE_URL || 'http://127.0.0.1:8080';
const shareSlug = process.env.SHARE_SLUG || '';
const runId = new Date().toISOString().replace(/[-:TZ.]/g, '').slice(0, 14);
const artifactsDir = process.env.ARTIFACTS_DIR || path.join(os.tmpdir(), `yanxitong-public-invitation-smoke-${runId}`);

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

function escapeHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function invitationHtml(data) {
  const basic = data.basicFields || {};
  const banquet = data.banquet || {};
  const template = data.template || {};
  const presentation = data.templatePresentation || {};
  const theme = data.theme || {};
  const copywriting = data.giftSuccessCopywriting || {};
  const scheduleItems = String(basic.scheduleText || presentation.defaultScheduleText || '').split(/\r?\n/).map((item) => item.trim()).filter(Boolean);
  const showGiftEntry = basic.showGiftEntry !== '0';
  const showDeviceEntry = basic.showDeviceEntry !== '0';
  const disabledEntries = [
    showGiftEntry ? '' : '<div class="notice">随礼入口暂未开放</div>',
    showDeviceEntry ? '' : '<div class="notice">设备租赁入口暂未开放</div>'
  ].join('');
  const primary = theme.primaryColor || '#b91c1c';
  const secondary = theme.secondaryColor || '#facc15';
  const cover = data.invitation?.coverUrl || template.coverUrl || '';
  const greeting = basic.greeting || presentation.defaultGreeting || '诚邀您拨冗赴宴，共同见证这份重要时刻';
  return `<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>${escapeHtml(data.invitation?.title || '公开请柬')}</title>
  <style>
    * { box-sizing: border-box; }
    body { margin: 0; background: #f8fafc; color: #111827; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif; }
    main { width: min(720px, 100%); margin: 0 auto; padding: 28px; }
    .hero { overflow: hidden; color: #fff; border-radius: 8px; background: linear-gradient(135deg, ${primary}, #334155); }
    .cover { width: 100%; height: 280px; object-fit: cover; display: block; }
    .hero-body { padding: 34px 28px 38px; }
    .template { color: rgba(255,255,255,.82); font-size: 14px; }
    h1 { margin: 12px 0; font-size: 34px; line-height: 1.2; }
    .subtitle { margin: 0; line-height: 1.7; color: rgba(255,255,255,.92); }
    .grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; margin: 22px 0; }
    .section, .copy, .timeline, .share { padding: 18px; border: 1px solid #e5e7eb; border-radius: 8px; background: #fff; }
    .notice { margin-top: 18px; padding: 14px 16px; border: 1px solid #dbeafe; border-radius: 8px; background: #eff6ff; color: #1e40af; }
    .warning { border-color: #fde68a; background: #fffbeb; color: #92400e; }
    .label { display: block; margin-bottom: 8px; color: #64748b; font-size: 13px; }
    .value { line-height: 1.55; }
    .timeline { margin-bottom: 22px; }
    .timeline h2, .copy h2 { margin: 0 0 12px; font-size: 20px; }
    .timeline-item { padding: 12px 0 12px 18px; border-left: 4px solid ${secondary}; }
    .timeline-item + .timeline-item { border-top: 1px solid #f1f5f9; }
    .copy { border-left: 6px solid ${secondary}; }
    .copy h2 { color: ${primary}; }
    .share { margin-top: 18px; color: #64748b; word-break: break-all; }
    .actions { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; margin-top: 24px; }
    button { height: 42px; border: 0; border-radius: 6px; color: #fff; background: ${primary}; font-size: 15px; }
    button.secondary { background: #334155; }
    @media (max-width: 540px) { main { padding: 18px; } .grid, .actions { grid-template-columns: 1fr; } h1 { font-size: 28px; } }
  </style>
</head>
<body>
  <main>
    <section class="hero">
      ${cover ? `<img class="cover" src="${escapeHtml(cover)}" alt="">` : ''}
      ${cover ? '' : `<div class="cover" style="display:grid;place-items:center;font-size:72px;font-weight:700;">${escapeHtml(presentation.fallbackCoverLabel || '宴')}</div>`}
      <div class="hero-body">
        <span class="template">${escapeHtml(template.name || '基础请柬')}</span>
        <h1>${escapeHtml(data.invitation?.title || '公开请柬')}</h1>
        <p class="subtitle">${escapeHtml(greeting)}</p>
      </div>
    </section>
    <section class="grid">
      <div class="section"><span class="label">主办人</span><div class="value">${escapeHtml(basic.hostName || '-')}</div></div>
      <div class="section"><span class="label">联系电话</span><div class="value">${escapeHtml(basic.contactPhone || '-')}</div></div>
      <div class="section"><span class="label">宴席时间</span><div class="value">${escapeHtml(String(banquet.banquetTime || '时间待定').replace('T', ' '))}</div></div>
      <div class="section"><span class="label">宴席地点</span><div class="value">${escapeHtml(banquet.location || '敬请光临')}</div></div>
      <div class="section"><span class="label">地址详情</span><div class="value">${escapeHtml(basic.addressDetail || '-')}</div></div>
      <div class="section"><span class="label">宴席类型</span><div class="value">${escapeHtml(banquet.eventTypeCode || '-')}</div></div>
    </section>
    ${scheduleItems.length ? `<section class="timeline"><h2>宴席流程</h2>${scheduleItems.map((item) => `<div class="timeline-item">${escapeHtml(item)}</div>`).join('')}</section>` : ''}
    <section class="copy"><h2>${escapeHtml(copywriting.title || '心意文案')}</h2><div>${escapeHtml(copywriting.content || '')}</div></section>
    <section class="share">分享路径：${escapeHtml(data.shareUrl || '')}</section>
    ${data.templateAvailable === false ? `<section class="notice warning">${escapeHtml(data.templateMessage || '原请柬模板已不可用，当前使用基础样式展示')}</section>` : ''}
    ${disabledEntries}
    <section class="actions">
      <button>填写回执</button>
      ${showGiftEntry ? '<button class="secondary">线上随礼</button><button class="secondary">现场扫码</button>' : ''}
      ${showDeviceEntry ? '<button class="secondary">设备租赁</button>' : ''}
    </section>
  </main>
</body>
</html>`;
}

function cloneData(data) {
  return JSON.parse(JSON.stringify(data));
}

function addRenderChecks(results, data) {
  const withoutCover = cloneData(data);
  withoutCover.invitation = { ...(withoutCover.invitation || {}), coverUrl: '' };
  withoutCover.template = { ...(withoutCover.template || {}), coverUrl: '' };
  const fallbackHtml = invitationHtml(withoutCover);
  const fallbackLabel = withoutCover.templatePresentation?.fallbackCoverLabel || '宴';
  results.push({
    name: 'fallback-cover-rendering',
    ok: fallbackHtml.includes(fallbackLabel),
    failures: fallbackHtml.includes(fallbackLabel) ? [] : ['fallback cover label missing']
  });

  const disabledEntries = cloneData(data);
  disabledEntries.basicFields = {
    ...(disabledEntries.basicFields || {}),
    showGiftEntry: '0',
    showDeviceEntry: '0'
  };
  const disabledHtml = invitationHtml(disabledEntries);
  const disabledFailures = [];
  for (const expected of ['随礼入口暂未开放', '设备租赁入口暂未开放']) {
    if (!disabledHtml.includes(expected)) {
      disabledFailures.push(`missing disabled entry notice: ${expected}`);
    }
  }
  results.push({
    name: 'disabled-entry-rendering',
    ok: disabledFailures.length === 0,
    failures: disabledFailures
  });

  const unavailableTemplate = cloneData(data);
  unavailableTemplate.templateAvailable = false;
  unavailableTemplate.template = null;
  unavailableTemplate.templateMessage = '原请柬模板已下架，当前使用基础样式展示';
  const unavailableHtml = invitationHtml(unavailableTemplate);
  results.push({
    name: 'unavailable-template-rendering',
    ok: unavailableHtml.includes(unavailableTemplate.templateMessage),
    failures: unavailableHtml.includes(unavailableTemplate.templateMessage)
      ? []
      : ['unavailable template notice missing']
  });
}

async function main() {
  if (!shareSlug) {
    throw new Error('SHARE_SLUG is required');
  }
  const executablePath = chromeExecutablePath();
  if (!executablePath) {
    throw new Error('Chrome executable not found. Set CHROME_PATH to a Chrome/Chromium executable.');
  }
  fs.mkdirSync(artifactsDir, { recursive: true });
  const response = await fetch(`${baseUrl}/api/invitations/public/${encodeURIComponent(shareSlug)}`);
  if (!response.ok) {
    throw new Error(`Public invitation API failed: ${response.status}`);
  }
  const body = await response.json();
  if (body.code !== 0) {
    throw new Error(body.message || 'Public invitation API returned non-zero code');
  }
  const html = invitationHtml(body.data);
  const htmlPath = path.join(artifactsDir, 'public-invitation.html');
  fs.writeFileSync(htmlPath, html);

  const browser = await chromium.launch({ executablePath, headless: process.env.HEADLESS !== '0' });
  const results = [];
  try {
    for (const viewport of [
      { name: 'desktop-public-invitation', width: 960, height: 900 },
      { name: 'mobile-public-invitation', width: 390, height: 844 }
    ]) {
      const page = await browser.newPage({ viewport: { width: viewport.width, height: viewport.height } });
      await page.goto(`file://${htmlPath}`);
      const text = await page.locator('body').innerText();
      const failures = [];
      for (const required of ['填写回执', '宴席流程', '分享路径']) {
        if (!text.includes(required)) {
          failures.push(`missing text: ${required}`);
        }
      }
      if (!text.includes('宴') && !(body.data.invitation?.coverUrl || body.data.template?.coverUrl)) {
        failures.push('fallback cover label missing');
      }
      if (body.data.basicFields?.showDeviceEntry === '0' && !text.includes('设备租赁入口暂未开放')) {
        failures.push('disabled device entry notice missing');
      }
      const screenshot = path.join(artifactsDir, `${viewport.name}.png`);
      await page.screenshot({ path: screenshot, fullPage: true });
      await page.close();
      results.push({ name: viewport.name, ok: failures.length === 0, failures, screenshot });
    }
  } finally {
    await browser.close();
  }

  addRenderChecks(results, body.data);

  const missingResponse = await fetch(`${baseUrl}/api/invitations/public/${encodeURIComponent(`missing-${runId}`)}`);
  const missingBody = await missingResponse.json().catch(() => null);
  const missingSlugCheck = {
    name: 'missing-public-invitation-slug',
    ok: missingResponse.status === 404 && Boolean(missingBody?.message),
    failures: [
      missingResponse.status === 404 ? '' : `expected 404 for missing slug, got ${missingResponse.status}`,
      missingBody?.message ? '' : 'missing readable 404 message'
    ].filter(Boolean)
  };
  results.push(missingSlugCheck);

  const summary = {
    baseUrl,
    shareSlug,
    artifactsDir,
    html: htmlPath,
    screenshots: results.map((item) => item.screenshot).filter(Boolean),
    total: results.length,
    passed: results.filter((item) => item.ok).length,
    failed: results.filter((item) => !item.ok).length,
    results
  };
  fs.writeFileSync(path.join(artifactsDir, 'summary.json'), JSON.stringify(summary, null, 2));
  if (summary.failed > 0) {
    console.error(`Public invitation smoke failed. Artifacts: ${artifactsDir}`);
    for (const result of results.filter((item) => !item.ok)) {
      console.error(`- ${result.name}: ${result.failures.join('; ')}`);
    }
    process.exit(1);
  }
  console.log(`Public invitation smoke passed. Artifacts: ${artifactsDir}`);
  for (const result of results) {
    console.log(`- ${result.name}: ${result.screenshot || 'no screenshot'}`);
  }
}

main().catch((error) => {
  console.error(error instanceof Error ? error.message : String(error));
  process.exit(1);
});
