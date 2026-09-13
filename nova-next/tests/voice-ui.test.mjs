import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const voiceUiUrl = new URL('../src/voice-ui.mjs', import.meta.url);
const app = fs.readFileSync(new URL('../app.js', import.meta.url), 'utf8');
const sw = fs.readFileSync(new URL('../service-worker.js', import.meta.url), 'utf8');

await test('voice UI exists and binds the explicit Voice input control', () => {
  assert.equal(fs.existsSync(voiceUiUrl), true, 'voice-ui.mjs should exist');
  const ui = fs.readFileSync(voiceUiUrl, 'utf8');
  assert.match(ui, /createVoiceUi/);
  assert.match(ui, /Voice input/);
  assert.match(ui, /createVoiceInput/);
});

await test('voice transcript only fills the chat input and never sends it', () => {
  assert.equal(fs.existsSync(voiceUiUrl), true, 'voice-ui.mjs should exist');
  const ui = fs.readFileSync(voiceUiUrl, 'utf8');
  assert.match(ui, /input\.value/);
  assert.doesNotMatch(ui, /requestSubmit\(|submit\(|sendMessage\(|\.click\(/);
});

await test('app and offline shell wire voice UI without changing current Nova', () => {
  assert.match(app, /createVoiceUi/);
  assert.match(app, /voiceUi\.bind\(\)/);
  assert.match(sw, /src\/voice-input\.mjs/);
  assert.match(sw, /src\/voice-ui\.mjs/);
  assert.doesNotMatch(app, /\.\.\/nova\//);
});

console.log('voice-ui: ok');
