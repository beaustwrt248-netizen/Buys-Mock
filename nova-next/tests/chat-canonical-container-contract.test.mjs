import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, '..');
const html = fs.readFileSync(path.join(root, 'index.html'), 'utf8');
const runtime = fs.readFileSync(path.join(root, 'src/live-runtime.mjs'), 'utf8');

const htmlContainers = html.match(/id=["']novaNextChatMessages["']/g) || [];
assert.equal(htmlContainers.length, 1,
  'Nova Next shell must define exactly one canonical chat message container');
assert.doesNotMatch(runtime, /messages\.id\s*=\s*['"]novaNextChatMessages['"]/,
  'live runtime must not create a second chat container with the canonical ID');
assert.match(runtime, /getElementById\(['"]novaNextChatMessages['"]\)/,
  'live runtime must reuse the canonical chat message container');

console.log('chat-canonical-container-contract: ok');
