#!/usr/bin/env node

import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDir = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(scriptDir, '..', '..');
const miniappRoot = path.join(repoRoot, 'miniapp');
const pagesJsonPath = path.join(miniappRoot, 'src', 'pages.json');
const srcRoot = path.join(miniappRoot, 'src');

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

function normalizeRoute(route) {
  const withoutQuery = route.split('?')[0];
  return withoutQuery.replace(/^\/+/, '');
}

function extractNavigations(source) {
  const navigations = [];
  const patterns = [
    /uni\.(navigateTo|redirectTo|switchTab|reLaunch)\s*\(\s*\{\s*url:\s*['"]([^'"]+)['"]/g,
    /uni\.(navigateTo|redirectTo|switchTab|reLaunch)\s*\(\s*\{\s*url:\s*`([^`$]+)(?:\?|`)/g
  ];
  for (const pattern of patterns) {
    let match;
    while ((match = pattern.exec(source)) !== null) {
      navigations.push({ method: match[1], route: match[2] });
    }
  }
  return navigations;
}

const pagesJson = readJson(pagesJsonPath);
const registeredPages = new Set((pagesJson.pages || []).map((item) => item.path));
const tabPages = new Set((pagesJson.tabBar?.list || []).map((item) => item.pagePath));
const failures = [];

for (const file of walk(srcRoot)) {
  const source = fs.readFileSync(file, 'utf8');
  for (const { method, route } of extractNavigations(source)) {
    if (!route.startsWith('/pages/')) {
      continue;
    }
    const normalized = normalizeRoute(route);
    if (!registeredPages.has(normalized)) {
      failures.push({
        file: path.relative(repoRoot, file),
        route,
        reason: 'route is not registered in miniapp/src/pages.json'
      });
    }
    if (method === 'switchTab' && !tabPages.has(normalized)) {
      failures.push({
        file: path.relative(repoRoot, file),
        route,
        reason: 'switchTab target is not registered in tabBar'
      });
    }
  }
}

if (failures.length) {
  console.error('Miniapp route check failed:');
  for (const failure of failures) {
    console.error(`- ${failure.file}: ${failure.route} (${failure.reason})`);
  }
  process.exit(1);
}

console.log(`Miniapp route check passed. Registered pages: ${registeredPages.size}.`);
