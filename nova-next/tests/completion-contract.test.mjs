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
const completionCss = read('completion.css');
const completionUi = read('src/completion-ui.mjs');
const manifest = JSON.parse(read('manifest.webmanifest'));

const styleBundle = css + liveCss + completionCss;
assert.match(styleBundle, /safe-area-inset-top/);
assert.match(styleBundle, /safe-area-inset-bottom/);
assert.match(styleBundle, /--nova-bottom-nav-height/);
assert.match(styleBundle, /--nova-composer-height/);
assert.match(html, /class="chat-layout"/);
assert.match(html, /class="chat-scroll"/);
assert.match(html, /class="chat-empty-state is-active"/);
assert.match(completionUi, /chat-empty-state/);
assert.match(completionUi, /is-active/);
assert.doesNotMatch(styleBundle, /chat-message\.assistant[^}]*#(?:[89a-fA-F][0-9a-fA-F]){5}/);

assert.match(html, /data-tool-category="productivity"/);
assert.match(html, /data-tool-category="content"/);
assert.match(html, /data-tool-category="analysis"/);
assert.match(completionUi, /filterTools/);
assert.match(completionUi, /novaNextToolSearch/);

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

const androidManifest = read('android/app/src/main/AndroidManifest.xml');
const gradle = read('android/app/build.gradle');
assert.match(androidManifest, /@mipmap\/ic_launcher/);
assert.match(androidManifest, /REQUEST_INSTALL_PACKAGES/);
assert.match(gradle, /NOVA_OTA_MANIFEST_URL/);
assert.match(gradle, /NOVA_OTA_CHANNEL/);

const serviceWorker = read('service-worker.js');
assert.match(serviceWorker, /nova-next-shell-v4/);
assert.match(serviceWorker, /NOVA_WEB_UPDATE_READY/);
assert.match(completionUi, /NOVA_WEB_UPDATE_READY/);

console.log('completion-contract: ok');
