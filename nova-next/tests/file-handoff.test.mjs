import test from 'node:test';
import assert from 'node:assert/strict';
import { createFileSession } from '../src/file-session.mjs';

test('chat handoff returns text only after explicit request', async () => {
  let reads = 0;
  const file = { name:'notes.md', type:'text/markdown', size:10, text:async () => { reads += 1; return '# Notes'; } };
  const session = createFileSession({ idFactory: () => 'f1' });
  session.addFiles([file]);
  assert.equal(reads, 0);
  assert.equal(await session.toChatText('f1'), '# Notes');
  assert.equal(reads, 1);
});

test('vision handoff is an explicit alias of validated image data read', async () => {
  const file = { name:'phone.jpg', type:'image/jpeg', size:10 };
  const oldReader = globalThis.FileReader;
  class FakeReader {
    readAsDataURL(input) { this.result = input === file ? 'data:image/jpeg;base64,abc' : ''; queueMicrotask(() => this.onload()); }
  }
  globalThis.FileReader = FakeReader;
  try {
    const session = createFileSession({ idFactory: () => 'img' });
    session.addFiles([file]);
    assert.equal(await session.toVisionDataUrl('img'), 'data:image/jpeg;base64,abc');
  } finally {
    if (oldReader === undefined) delete globalThis.FileReader;
    else globalThis.FileReader = oldReader;
  }
});

test('handoff aliases preserve unsupported-type validation', async () => {
  const session = createFileSession({ idFactory: () => 'f1' });
  session.addFiles([{ name:'archive.zip', type:'application/zip', size:10, text:async () => 'x' }]);
  await assert.rejects(() => session.toChatText('f1'), /FILE_TEXT_UNSUPPORTED/);
  await assert.rejects(() => session.toVisionDataUrl('f1'), /FILE_IMAGE_UNSUPPORTED/);
});
