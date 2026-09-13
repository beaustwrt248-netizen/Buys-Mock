import test from 'node:test';
import assert from 'node:assert/strict';

let moduleUnderTest = null;
try {
  moduleUnderTest = await import('../src/voice-input.mjs');
} catch {}

await test('voice input module exists and is user-triggered', () => {
  assert.ok(moduleUnderTest, 'voice-input.mjs should exist');
  assert.equal(typeof moduleUnderTest.createVoiceInput, 'function');

  class FakeRecognition {
    constructor() { FakeRecognition.instance = this; this.started = 0; this.stopped = 0; }
    start() { this.started += 1; }
    stop() { this.stopped += 1; }
  }

  const transcripts = [];
  const voice = moduleUnderTest.createVoiceInput({
    RecognitionCtor: FakeRecognition,
    onTranscript: value => transcripts.push(value)
  });

  assert.equal(FakeRecognition.instance.started, 0, 'must not start automatically');
  voice.start();
  assert.equal(FakeRecognition.instance.started, 1, 'starts only after explicit action');
  FakeRecognition.instance.onresult?.({ results: [[{ transcript: 'Find Pixel 10 prices' }]] });
  assert.deepEqual(transcripts, ['Find Pixel 10 prices']);
});

await test('voice input never auto-submits chat', () => {
  assert.ok(moduleUnderTest, 'voice-input.mjs should exist');
  assert.doesNotMatch(String(moduleUnderTest.createVoiceInput), /submit\(|requestSubmit\(|\.click\(/);
});

console.log('voice-input: ok');
