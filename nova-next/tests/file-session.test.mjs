import test from 'node:test';
import assert from 'node:assert/strict';
import { createFileSession, MAX_FILES, MAX_FILE_BYTES, MAX_TEXT_BYTES } from '../src/file-session.mjs';

const makeFile = ({ name='note.txt', type='text/plain', size=4, text='test' } = {}) => ({
  name, type, size,
  text: async () => text
});

test('adds supported metadata without claiming persistence', () => {
  const session = createFileSession({ now: () => new Date('2026-09-13T01:00:00Z'), idFactory: () => 'f1' });
  const [item] = session.addFiles([makeFile()]);
  assert.equal(item.id, 'f1');
  assert.equal(item.name, 'note.txt');
  assert.equal(item.canReadText, true);
  assert.equal(item.canAnalyseImage, false);
  assert.equal(item.addedAt, '2026-09-13T01:00:00.000Z');
  assert.equal('file' in item, false);
  assert.equal('uploaded' in item, false);
  assert.equal('synced' in item, false);
  assert.deepEqual(session.list(), [item]);
});

test('enforces exact file and session caps before reading', () => {
  const session = createFileSession({ idFactory: (() => { let n=0; return () => `f${++n}`; })() });
  assert.throws(() => session.addFiles([makeFile({ size: MAX_FILE_BYTES + 1 })]), /FILE_TOO_LARGE/);
  const twenty = Array.from({ length: MAX_FILES }, (_, i) => makeFile({ name:`${i}.txt` }));
  session.addFiles(twenty);
  assert.equal(session.list().length, 20);
  assert.throws(() => session.addFiles([makeFile({ name:'21.txt' })]), /FILE_SESSION_LIMIT/);
});

test('validates the whole incoming batch before retaining references', () => {
  const session = createFileSession({ idFactory: (() => { let n=0; return () => `f${++n}`; })() });
  assert.throws(() => session.addFiles([makeFile({ name:'ok.txt' }), makeFile({ name:'bad.bin', size:MAX_FILE_BYTES+1 })]), /FILE_TOO_LARGE/);
  assert.equal(session.list().length, 0);
});

test('text read is explicit and capped at one MiB before reading', async () => {
  let reads = 0;
  const file = makeFile({ size: MAX_TEXT_BYTES + 1, text:'x' });
  file.text = async () => { reads += 1; return 'x'; };
  const session = createFileSession({ idFactory: () => 'f1' });
  session.addFiles([file]);
  await assert.rejects(() => session.readText('f1'), /FILE_TEXT_TOO_LARGE/);
  assert.equal(reads, 0);
});

test('supported text types and extensions are readable only on request', async () => {
  let reads = 0;
  const file = makeFile({ name:'notes.md', type:'application/octet-stream', size:8, text:'# Notes' });
  file.text = async () => { reads += 1; return '# Notes'; };
  const session = createFileSession({ idFactory: () => 'f1' });
  const [item] = session.addFiles([file]);
  assert.equal(item.canReadText, true);
  assert.equal(reads, 0);
  assert.equal(await session.readText('f1'), '# Notes');
  assert.equal(reads, 1);
});

test('unsupported text read fails without reading', async () => {
  let reads = 0;
  const file = makeFile({ name:'archive.zip', type:'application/zip', size:8 });
  file.text = async () => { reads += 1; return 'no'; };
  const session = createFileSession({ idFactory: () => 'f1' });
  session.addFiles([file]);
  await assert.rejects(() => session.readText('f1'), /FILE_TEXT_UNSUPPORTED/);
  assert.equal(reads, 0);
});

test('image metadata is eligible only for supported Vision types', () => {
  const session = createFileSession({ idFactory: () => 'img' });
  const [item] = session.addFiles([makeFile({ name:'phone.webp', type:'image/webp' })]);
  assert.equal(item.canAnalyseImage, true);
  assert.equal(item.canReadText, false);
});

test('get exposes raw file only for explicit same-session handoff and remove clears it', () => {
  const file = makeFile();
  const session = createFileSession({ idFactory: () => 'f1' });
  session.addFiles([file]);
  assert.equal(session.get('f1').file, file);
  assert.equal(session.remove('f1'), true);
  assert.equal(session.get('f1'), null);
  assert.equal(session.remove('missing'), false);
});

test('clear drops all tracked entries', () => {
  const session = createFileSession({ idFactory: (() => { let n=0; return () => `f${++n}`; })() });
  session.addFiles([makeFile({name:'a.txt'}), makeFile({name:'b.txt'})]);
  session.clear();
  assert.deepEqual(session.list(), []);
});
