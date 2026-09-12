import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, '..');
const files = [];
function walk(dir) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      if (entry.name === 'tests') continue;
      walk(full);
    } else if (/\.(?:js|mjs|html|css|json|webmanifest)$/i.test(entry.name)) {
      files.push(full);
    }
  }
}
walk(root);
const text = files.map(file => fs.readFileSync(file, 'utf8')).join('\n');
assert.equal(/(?:\.\.\/)+nova\//.test(text), false, 'Nova Next must not import the existing nova/ frontend');
assert.equal(/service[_-]?role/i.test(text), false, 'Nova Next static source must not contain service-role credentials/references');
assert.equal(/sk-[A-Za-z0-9_-]{20,}/.test(text), false, 'Nova Next static source must not contain provider secret-like keys');
console.log('isolation: ok');
