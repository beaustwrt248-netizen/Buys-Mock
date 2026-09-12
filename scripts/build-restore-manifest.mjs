#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';

const SHA_RE = /^[a-f0-9]{40}$/i;

export function classifyComponents(files = []) {
  const out = new Set();
  for (const raw of files) {
    const file = String(raw || '').trim();
    if (!file) continue;
    if (file.startsWith('android/')) out.add('morley_buys_android');
    if (file.startsWith('admin/')) out.add('morley_admin_web');
    if (file.startsWith('nova/')) out.add('nova');
    if (file.startsWith('supabase/functions/')) out.add('supabase_functions');
    if (file.startsWith('supabase/migrations/')) out.add('supabase_schema');
    if (file.startsWith('.github/workflows/')) out.add('deployment_workflows');
    if (/^(index\.html|web-base\.html|[^/]+\.(?:js|css)|web-assets\/)/.test(file)) out.add('morley_buys_web');
    if (/guardian/i.test(file)) out.add('guardian');
  }
  return [...out].sort();
}

export function latestMigrationHead(root = process.cwd()) {
  const dir = path.join(root, 'supabase', 'migrations');
  if (!fs.existsSync(dir)) return null;
  const names = fs.readdirSync(dir).filter((name) => /^\d+.*\.sql$/.test(name)).sort();
  return names.length ? names.at(-1).replace(/\.sql$/, '') : null;
}

export function readReleaseRefs(root = process.cwd()) {
  const latest = path.join(root, 'ota', 'latest.json');
  if (!fs.existsSync(latest)) return {};
  try {
    const data = JSON.parse(fs.readFileSync(latest, 'utf8'));
    return {
      morleyBuys: data.versionName ? `v${data.versionName}` : null,
      versionCode: Number.isFinite(Number(data.versionCode)) ? Number(data.versionCode) : null,
      apkSha256: typeof data.sha256 === 'string' ? data.sha256 : null,
    };
  } catch {
    return {};
  }
}

export function buildManifest({ sourceSha, changeRef, files, createdAt = new Date().toISOString(), root = process.cwd() }) {
  if (!SHA_RE.test(String(sourceSha || ''))) throw new Error('A 40-character source SHA is required.');
  const components = classifyComponents(files);
  if (!components.length) components.push('repository');
  return Object.freeze({
    rulesVersion: 'restore-v1',
    sourceSha,
    changeRef: String(changeRef || '').trim() || 'unknown-change',
    components: Object.freeze(components),
    releaseRefs: Object.freeze(readReleaseRefs(root)),
    webRefs: Object.freeze({ sourceSha }),
    functionRefs: Object.freeze({}),
    migrationHead: latestMigrationHead(root),
    configFingerprints: Object.freeze({
      repositoryTree: `git:${sourceSha}`,
    }),
    createdAt,
  });
}

function parseArgs(argv) {
  const args = {};
  for (let i = 0; i < argv.length; i += 1) {
    const key = argv[i];
    if (!key.startsWith('--')) continue;
    args[key.slice(2)] = argv[i + 1] && !argv[i + 1].startsWith('--') ? argv[++i] : 'true';
  }
  return args;
}

if (import.meta.url === `file://${process.argv[1]}`) {
  const args = parseArgs(process.argv.slice(2));
  const filesPath = args.files;
  const files = filesPath && fs.existsSync(filesPath)
    ? fs.readFileSync(filesPath, 'utf8').split(/\r?\n/).map((value) => value.trim()).filter(Boolean)
    : [];
  const manifest = buildManifest({ sourceSha: args['source-sha'], changeRef: args['change-ref'], files });
  const output = args.output || 'restore-point.json';
  fs.writeFileSync(output, `${JSON.stringify(manifest, null, 2)}\n`, { mode: 0o600 });
  process.stdout.write(`Created restore manifest ${output} for ${manifest.sourceSha} (${manifest.components.join(', ')})\n`);
}
