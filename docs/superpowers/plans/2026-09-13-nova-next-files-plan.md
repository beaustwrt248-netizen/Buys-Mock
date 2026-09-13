# Nova Next Files Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the Files placeholder into an honest session-only file workspace with explicit Vision and guarded-Chat handoffs.

**Architecture:** Add a session service at `nova-next/src/file-session.mjs` that owns validation and in-memory tracking only. Extend `workspace-ui.mjs` to render Files and perform explicit user-triggered handoffs through existing `featureRuntime.analyseImages()` and Chat prefill navigation; no cloud storage or silent upload is introduced.

**Tech Stack:** Browser ES modules, File/FileReader APIs, Node 22 built-in tests, existing Nova Next Vision and guarded Chat runtime.

**Spec:** `docs/superpowers/specs/2026-09-13-nova-next-functionality-first-design.md`

## Global Constraints

- Maximum 20 selected files per Nova Next session.
- Maximum 20 MiB per file.
- UTF-8 text/Markdown/JSON/CSV local read maximum: 1 MiB.
- Image handoff remains subject to existing Vision limits.
- Files remain session-local unless the user explicitly invokes a supported handoff.
- No persistent cloud file storage, background upload, or fake sync state.
- Current production `nova/**` remains untouched.

---

### Task 1: Session-only file service

**Files:**
- Create: `nova-next/src/file-session.mjs`
- Create: `nova-next/tests/file-session.test.mjs`

**Interfaces:**
- Produces: `createFileSession({ now, idFactory })`.
- Produces methods: `addFiles(files)`, `list()`, `get(id)`, `remove(id)`, `clear()`, `readText(id)`, `readDataUrl(id)`.
- Metadata shape: `{ id, name, type, size, addedAt, canReadText, canAnalyseImage }`.

- [ ] **Step 1: Write failing limits/metadata tests**

```js
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
  assert.equal(item.canReadText, true);
  assert.equal(item.canAnalyseImage, false);
  assert.equal('uploaded' in item, false);
  assert.equal('synced' in item, false);
});

test('enforces exact file and session caps before reading', () => {
  const session = createFileSession();
  assert.throws(() => session.addFiles([makeFile({ size: MAX_FILE_BYTES + 1 })]), /FILE_TOO_LARGE/);
  const twenty = Array.from({ length: MAX_FILES }, (_, i) => makeFile({ name:`${i}.txt` }));
  session.addFiles(twenty);
  assert.throws(() => session.addFiles([makeFile({ name:'21.txt' })]), /FILE_SESSION_LIMIT/);
});
```

- [ ] **Step 2: Run and verify RED**

Run: `node --test nova-next/tests/file-session.test.mjs`

Expected: FAIL because the module does not exist.

- [ ] **Step 3: Implement constants, validation and in-memory tracking**

```js
export const MAX_FILES = 20;
export const MAX_FILE_BYTES = 20 * 1024 * 1024;
export const MAX_TEXT_BYTES = 1024 * 1024;
const TEXT_TYPES = new Set(['text/plain','text/markdown','application/json','text/csv','application/csv']);
const IMAGE_TYPES = new Set(['image/jpeg','image/png','image/webp']);

function extension(name='') { return name.toLowerCase().split('.').pop() || ''; }
function isTextLike(file) {
  return TEXT_TYPES.has(file.type) || ['txt','md','markdown','json','csv'].includes(extension(file.name));
}
function isImage(file) { return IMAGE_TYPES.has(file.type); }

export function createFileSession({ now = () => new Date(), idFactory = () => crypto.randomUUID() } = {}) {
  const entries = new Map();
  function addFiles(files) {
    const incoming = [...files];
    if (entries.size + incoming.length > MAX_FILES) throw new Error('FILE_SESSION_LIMIT');
    for (const file of incoming) if (file.size > MAX_FILE_BYTES) throw new Error('FILE_TOO_LARGE');
    return incoming.map(file => {
      const id = idFactory();
      entries.set(id, { file, addedAt: now().toISOString() });
      return metadata(id);
    });
  }
  // Implement metadata/list/get/remove/clear/readText/readDataUrl around the Map only.
  return Object.freeze({ addFiles, list, get, remove, clear, readText, readDataUrl });
}
```

Implementation requirements:
- `readText(id)` rejects non-text-like files with `FILE_TEXT_UNSUPPORTED`.
- `readText(id)` rejects size `> MAX_TEXT_BYTES` with `FILE_TEXT_TOO_LARGE` before calling `.text()`.
- `readDataUrl(id)` rejects non-images with `FILE_IMAGE_UNSUPPORTED` and uses `FileReader` only when called explicitly.
- `list()` returns metadata only, never raw File objects.
- `get(id)` may return `{ metadata, file }` for explicit handoff code inside the same app session.
- `remove()` and `clear()` drop object references immediately.

- [ ] **Step 4: Add text/image boundary tests**

```js
test('text read is explicit and capped at one MiB', async () => {
  let reads = 0;
  const file = makeFile({ size: MAX_TEXT_BYTES + 1, text:'x' });
  file.text = async () => { reads += 1; return 'x'; };
  const session = createFileSession({ idFactory: () => 'f1' });
  session.addFiles([file]);
  await assert.rejects(() => session.readText('f1'), /FILE_TEXT_TOO_LARGE/);
  assert.equal(reads, 0);
});

test('image metadata is eligible only for supported Vision types', () => {
  const session = createFileSession({ idFactory: () => 'img' });
  const [item] = session.addFiles([makeFile({ name:'phone.webp', type:'image/webp' })]);
  assert.equal(item.canAnalyseImage, true);
});
```

- [ ] **Step 5: Run to GREEN and commit**

Run: `node --test nova-next/tests/file-session.test.mjs`

Expected: PASS.

```bash
git add nova-next/src/file-session.mjs nova-next/tests/file-session.test.mjs
git commit -m "feat(nova-next): add session file service"
```

---

### Task 2: Explicit Vision and Chat handoff helpers

**Files:**
- Modify: `nova-next/src/file-session.mjs`
- Create: `nova-next/tests/file-handoff.test.mjs`

**Interfaces:**
- Consumes: explicit file ID.
- Produces: `toVisionDataUrl(id)` and `toChatText(id)` aliases with the same validation as `readDataUrl`/`readText`.

- [ ] **Step 1: Write failing handoff test**

```js
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
```

- [ ] **Step 2: Run RED**

Run: `node --test nova-next/tests/file-handoff.test.mjs`

Expected: FAIL because helper does not exist.

- [ ] **Step 3: Implement aliases without adding any automatic invocation**

```js
const toChatText = id => readText(id);
const toVisionDataUrl = id => readDataUrl(id);
```

Export them only as methods on the returned session object. Do not add timers, observers, background reads, fetch calls, or storage writes.

- [ ] **Step 4: Run GREEN and commit**

Run: `node --test nova-next/tests/file-handoff.test.mjs nova-next/tests/file-session.test.mjs`

Expected: PASS.

```bash
git add nova-next/src/file-session.mjs nova-next/tests/file-handoff.test.mjs
git commit -m "feat(nova-next): add explicit file handoff boundaries"
```

---

### Task 3: Files route UI

**Files:**
- Modify: `nova-next/src/workspace-ui.mjs`
- Modify: `nova-next/index.html`
- Modify: `nova-next/live.css`
- Create: `nova-next/tests/files-ui-contract.test.mjs`

**Interfaces:**
- Consumes: `fileSession`, `featureRuntime.analyseImages`, `onNavigate`, guarded Chat input `#novaNextChatInput`.
- Produces Files route actions: add, analyse image, send text to Chat, remove.

- [ ] **Step 1: Write failing Files UI contract**

```js
import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const html = await readFile(new URL('../index.html', import.meta.url), 'utf8');
const ui = await readFile(new URL('../src/workspace-ui.mjs', import.meta.url), 'utf8');

test('Files is no longer placeholder-only', () => {
  assert.ok(html.includes('novaNextFilesList'));
  assert.ok(html.includes('novaNextAddFiles'));
  for (const token of ['renderFiles', 'analyseFileImage', 'sendFileTextToChat', 'removeFile']) assert.ok(ui.includes(token));
});
```

- [ ] **Step 2: Run RED**

Run: `node --test nova-next/tests/files-ui-contract.test.mjs`

Expected: FAIL on missing containers/actions.

- [ ] **Step 3: Replace Files placeholder with stable shell**

```html
<section class="page" data-route="files" aria-labelledby="files-title">
  <div class="page-title-row"><div><h1 id="files-title">Files</h1><p>Session files for Nova tools</p></div><button id="novaNextAddFiles" class="small-primary" type="button">＋ Add files</button></div>
  <input id="novaNextFilePicker" class="sr-only" type="file" multiple />
  <p class="feature-boundary">Files stay on this device unless you explicitly send a supported file to Nova Vision or Chat.</p>
  <div id="novaNextFilesList" class="feature-stack"></div>
</section>
```

- [ ] **Step 4: Implement Files rendering/actions in workspace UI**

Requirements:
- Add button clicks hidden picker; picker accepts any file so unsupported metadata-only files can still be listed, but all files must pass 20-file/20 MiB limits.
- Render name, formatted size, MIME/type fallback, session-added time.
- Image action appears only when `canAnalyseImage`; click calls `await fileSession.toVisionDataUrl(id)` then `await featureRuntime.analyseImages([dataUrl], { hint:'Analyse this selected file conservatively.' })` and renders via an injected `onVisionResult` callback or existing shared Vision renderer.
- Text action appears only when `canReadText`; click calls `toChatText(id)`, navigates to Chat, sets `#novaNextChatInput` to `Please analyse this file content:\n\n${text}`, and focuses it. It does not auto-send.
- Remove is explicit and session-local.
- Validation failures show exact user feedback: `Maximum 20 files per session.`, `Files must be 20 MiB or smaller.`, or `Text handoff is limited to 1 MiB.`.

- [ ] **Step 5: Add focused styles and run GREEN**

Add `.file-card`, `.file-card-actions`, `.file-meta`, `.file-type-badge` to `live.css`, reusing existing spacing/radius tokens.

Run: `node --test nova-next/tests/files-ui-contract.test.mjs nova-next/tests/file-*.test.mjs`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add nova-next/src/workspace-ui.mjs nova-next/index.html nova-next/live.css nova-next/tests/files-ui-contract.test.mjs
git commit -m "feat(nova-next): add honest session files workspace"
```

---

### Task 4: App construction and regression verification

**Files:**
- Modify: `nova-next/app.js`

**Interfaces:**
- Consumes: `createFileSession`.
- Produces: one session service instance injected into `createWorkspaceUi`.

- [ ] **Step 1: Add failing wiring assertion**

Extend `files-ui-contract.test.mjs`:

```js
const app = await readFile(new URL('../app.js', import.meta.url), 'utf8');
assert.ok(app.includes('createFileSession'));
assert.ok(app.includes('fileSession'));
```

- [ ] **Step 2: Run RED**

Run: `node --test nova-next/tests/files-ui-contract.test.mjs`

Expected: FAIL on missing wiring.

- [ ] **Step 3: Wire the service**

```js
import { createFileSession } from './src/file-session.mjs';
const fileSession = createFileSession();
```

Inject `fileSession` and `featureRuntime` into the existing `createWorkspaceUi` construction. Preserve all existing Auth/Chat/Vision/Knowledge/Control Centre wiring.

- [ ] **Step 4: Run full verification**

```bash
node --test nova-next/tests/*.test.mjs
node --check nova-next/app.js
find nova-next/src -type f -name '*.mjs' -print0 | sort -z | xargs -0 -n1 node --check
git diff --name-only main...HEAD | grep '^nova/' && exit 1 || true
```

Expected: all tests PASS, syntax exits 0, no current `nova/**` files changed.

- [ ] **Step 5: Commit**

```bash
git add nova-next/app.js nova-next/tests/files-ui-contract.test.mjs
git commit -m "feat(nova-next): wire session files service"
```
