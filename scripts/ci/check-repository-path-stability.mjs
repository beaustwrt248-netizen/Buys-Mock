#!/usr/bin/env node

import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const failures = [];

const criticalPaths = [
  'admin/index.html',
  'admin/workspace.html',
  'admin/workspace-template.html',
  'admin/browser-auth-bootstrap.js',
  'admin/login-security.js',
  'admin/app.js',
  '.github/workflows/admin-android-check.yml',
  '.github/workflows/deploy-admin-pages.yml',
  '.github/workflows/morley-ecosystem-autopilot.yml',
];

for (const relativePath of criticalPaths) {
  if (!fs.existsSync(path.join(root, relativePath))) {
    failures.push(`Missing canonical path: ${relativePath}`);
  }
}

function localAssetTargets(file) {
  const absolute = path.join(root, file);
  if (!fs.existsSync(absolute)) return [];
  const source = fs.readFileSync(absolute, 'utf8');
  const matches = source.matchAll(/(?:src|href)=["']([^"']+)["']/g);
  return [...matches]
    .map((match) => match[1])
    .filter((value) => !/^(?:https?:|data:|about:|#)/i.test(value))
    .map((value) => value.split(/[?#]/, 1)[0])
    .filter(Boolean)
    .map((value) => path.posix.normalize(path.posix.join(path.posix.dirname(file), value)));
}

for (const owner of ['admin/index.html', 'admin/workspace.html']) {
  for (const target of localAssetTargets(owner)) {
    if (!fs.existsSync(path.join(root, target))) {
      failures.push(`${owner} references missing local asset: ${target}`);
    }
  }
}

if (failures.length) {
  console.error('Repository path stability check failed:');
  for (const failure of failures) console.error(`- ${failure}`);
  console.error('\nDo not silently move or rename these paths. Update callers and this contract in the same reviewed change.');
  process.exit(1);
}

console.log(`Repository path stability check passed (${criticalPaths.length} canonical paths).`);
