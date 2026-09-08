import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';

const source=await readFile(new URL('../admin/support-intelligence-live.js',import.meta.url),'utf8');
assert.match(source,/from\('support_tickets'\)\.select\('\*'\)/,'reads authoritative support tickets');
assert.match(source,/ingest\('support',evidence,\{source:SOURCE,checkedAt,authoritative:true\}\)/,'publishes support evidence as authoritative runtime data');
assert.match(source,/postgres_changes/,'subscribes to support ticket changes');
assert.match(source,/REFRESH_MS=60000/,'keeps a polling fallback');
assert.doesNotMatch(source,/\.insert\(|\.update\(|\.delete\(/,'live intelligence adapter remains read-only');
console.log('support-intelligence-live contract: ok');
