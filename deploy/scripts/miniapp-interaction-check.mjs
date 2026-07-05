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

if (failures.length) {
  console.error('Miniapp interaction check failed:');
  for (const failure of failures) {
    console.error(`- ${failure.file}:${failure.line}: ${failure.reason}`);
  }
  process.exit(1);
}

console.log(`Miniapp interaction check passed. Checked ${registeredPages.size} registered pages.`);
