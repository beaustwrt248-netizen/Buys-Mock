import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, '..');
const read = rel => fs.readFileSync(path.join(root, rel), 'utf8');

const html = read('index.html');
const css = read('styles.css');
const liveCss = read('live.css');
const app = read('app.js');
const runtime = read('src/live-runtime.mjs');
const featureUi = read('src/feature-ui.mjs');
const manifest = JSON.parse(read('manifest.webmanifest'));

assert.match(css + liveCss, /safe-area-inset-top/);
assert.match(css + liveCss, /safe-area-inset-bottom/);
assert.match(css + liveCss, /--nova-bottom-nav-height/);
assert.match(css + liveCss, /--nova-composer-height/);
assert.match(html, /class="chat-layout"/);
assert.match(html, /class="chat-scroll"/);
assert.match(html, /class="chat-empty-state"/);
assert.match(runtime, /chat-empty-state/);
assert.match(runtime, /is-active/);
assert.doesNotMatch(liveCss, /chat-message\.assistant[^}]*#(?:[89a-fA-F][0-9a-fA-F]){5}/);

assert.match(html, /data-tool-category="productivity"/);
assert.match(html, /data-tool-category="content"/);
assert.match(html, /data-tool-category="analysis"/);
assert.match(app, /filterTools/);
assert.match(app, /novaNextToolSearch/);

const iconPurposes = manifest.icons?.map(icon => icon.purpose || '').join(' ') || '';
assert.ok(manifest.icons?.some(icon => icon.sizes === '192x192'));
assert.ok(manifest.icons?.some(icon => icon.sizes === '512x512'));
assert.match(iconPurposes, /maskable/);

const ota = read('src/ota-config.mjs');
assert.match(ota, /nova-next/);
assert.match(ota, /com\.buysloans\.novanext/);
assert.match(ota, /channel/);
assert.match(ota, /sha256/);
assert.match(ota, /validateNovaRelease/);
assert.doesNotMatch(ota, /com\.buysloans\.morley/);

console.log('completion-contract: ok');
