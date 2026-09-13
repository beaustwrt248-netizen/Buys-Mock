import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const voiceUiUrl = new URL('../src/voice-ui.mjs', import.meta.url);
const app = fs.readFileSync(new URL('../app.js', import.meta.url), 'utf8');
const sw = fs.readFileSync(new URL('../service-worker.js', import.meta.url), 'utf8');

await test('voice UI exists and creates an explicit Voice input control beside Chat send', () => {
  assert.equal(fs.existsSync(voiceUiUrl), true, 'voice-ui.mjs should exist');
  const ui = fs.readFileSync(voiceUiUrl, 'utf8');
  assert.match(ui, /createVoiceUi/);
  assert.match(ui, /Voice input/);
  assert.match(ui, /createVoiceInput/);
  assert.match(ui, /novaNextChatSend/);
  assert.match(ui, /insertBefore/);
});

await test('voice transcript only fills the chat input and never sends it', () => {
  assert.equal(fs.existsSync(voiceUiUrl), true, 'voice-ui.mjs should exist');
  const ui = fs.readFileSync(voiceUiUrl, 'utf8');
  assert.match(ui, /input\.value/);
  assert.doesNotMatch(ui, /requestSubmit\(|submit\(|sendMessage\(|sendChat\(|\.click\(/);
});

await test('app binds voice only after guarded Chat has prepared its send control', () => {
  assert.match(app, /createVoiceUi/);
  assert.match(app, /voiceUi\.bind\(\)/);
  assert.ok(app.indexOf('await liveRuntime.start()') < app.indexOf('voiceUi.bind()'));
});

await test('offline shell precaches both bounded voice modules', () => {
  assert.match(sw, /src\/voice-input\.mjs/);
  assert.match(sw, /src\/voice-ui\.mjs/);
  assert.doesNotMatch(app, /\.\.\/nova\//);
});

console.log('voice-ui: ok');
