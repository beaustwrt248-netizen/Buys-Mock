import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';

const source=await readFile(new URL('../admin/release-intelligence-live.js',import.meta.url),'utf8');
assert.match(source,/ota\/latest\.json/,'reads the authoritative OTA manifest');
assert.match(source,/releases\/tags/,'reads GitHub release metadata');
assert.match(source,/row\?\.digest/,'uses GitHub release asset digest evidence');
assert.match(source,/android\/app\/build\.gradle/,'compares Android source version');
assert.match(source,/from\('devices'\)\.select\('app_version,last_seen_at'\)/,'reads rollout evidence');
assert.match(source,/ingest\('release',\[evidence\]/,'publishes release evidence to runtime');
assert.doesNotMatch(source,/\.insert\(|\.update\(|\.delete\(|admin_set_config/,'release evidence adapter remains read-only');
console.log('release-intelligence-live contract: ok');
