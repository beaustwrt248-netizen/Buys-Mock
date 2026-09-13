import test from 'node:test';
import assert from 'node:assert/strict';
import { createVoiceInput } from '../src/voice-input.mjs';

class FakeRecognition {
  constructor(){ FakeRecognition.last = this; this.started = false; this.stopped = false; }
  start(){ this.started = true; }
  stop(){ this.stopped = true; this.onend?.(); }
  abort(){ this.stopped = true; }
}

test('unsupported speech recognition is truthful and inert', () => {
  const voice = createVoiceInput({ SpeechRecognitionCtor: null });
  assert.equal(voice.supported, false);
  assert.equal(voice.start({}), false);
});

test('voice starts only explicitly, is non-continuous and never auto-restarts', () => {
  const states=[]; const transcripts=[];
  const voice = createVoiceInput({ SpeechRecognitionCtor: FakeRecognition });
  assert.equal(voice.supported, true);
  assert.equal(FakeRecognition.last, undefined);
  assert.equal(voice.start({ onState:s=>states.push(s), onTranscript:t=>transcripts.push(t) }), true);
  const recognition = FakeRecognition.last;
  assert.equal(recognition.started, true);
  assert.equal(recognition.continuous, false);
  recognition.onresult?.({ results:[{ 0:{ transcript:'hello nova' }, isFinal:true }] });
  assert.deepEqual(transcripts, ['hello nova']);
  recognition.onend?.();
  assert.equal(recognition.started, true);
  assert.ok(states.includes('listening'));
  assert.ok(states.includes('idle'));
});

test('stop and destroy terminate recognition', () => {
  const voice = createVoiceInput({ SpeechRecognitionCtor: FakeRecognition });
  voice.start({});
  voice.stop();
  assert.equal(FakeRecognition.last.stopped, true);
  voice.start({});
  voice.destroy();
  assert.equal(FakeRecognition.last.stopped, true);
});
