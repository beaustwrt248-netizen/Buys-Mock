import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const html = await readFile(new URL('../index.html', import.meta.url), 'utf8');
const ui = await readFile(new URL('../src/workspace-ui.mjs', import.meta.url), 'utf8');
const app = await readFile(new URL('../app.js', import.meta.url), 'utf8');
const featureUi = await readFile(new URL('../src/feature-ui.mjs', import.meta.url), 'utf8');

test('Files is no longer placeholder-only', () => {
  assert.ok(html.includes('id="novaNextFilesList"'));
  assert.ok(html.includes('id="novaNextAddFiles"'));
  assert.ok(html.includes('id="novaNextFilePicker"'));
  assert.ok(html.includes('Files stay on this device unless you explicitly send a supported file'));
});

test('workspace UI owns explicit Files actions', () => {
  for (const token of ['renderFiles', 'analyseFileImage', 'sendFileTextToChat', 'removeFile', 'fileSession.toVisionDataUrl', 'fileSession.toChatText']) {
    assert.ok(ui.includes(token), token);
  }
});

test('Files UI contains exact limit feedback and does not claim upload or sync', () => {
  assert.ok(ui.includes('Maximum 20 files per session.'));
  assert.ok(ui.includes('Files must be 20 MiB or smaller.'));
  assert.ok(ui.includes('Text handoff is limited to 1 MiB.'));
  assert.equal(/uploaded|synced|cloud upload/i.test(html), false);
});

test('text handoff prefills guarded Chat without auto-send', () => {
  assert.ok(ui.includes('Please analyse this file content:'));
  assert.ok(ui.includes("onNavigate('chat')"));
  assert.ok(ui.includes("documentObj.getElementById('novaNextChatInput')"));
  assert.equal(ui.includes('liveRuntime.sendChat'), false);
});

test('app wires one session file service into the workspace UI', () => {
  assert.ok(app.includes("import { createFileSession } from './src/file-session.mjs'"));
  assert.ok(app.includes('const fileSession = createFileSession()'));
  assert.ok(app.includes('fileSession,'));
  assert.ok(app.includes('featureRuntime,'));
  assert.ok(app.includes('onVisionResult: result => featureUi.renderVision(result)'));
});

test('existing feature UI exposes only its renderer for Files Vision reuse', () => {
  assert.ok(featureUi.includes('renderVision'));
  assert.ok(featureUi.includes('Object.freeze({ bind, routeChanged, loadKnowledge, loadControlCentre, pickImages, openCodeProposal, renderVision })'));
});
