import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const app = fs.readFileSync('android/app/src/main/java/com/buysloans/hub/MorleyApplication.kt', 'utf8');
const gradle = fs.readFileSync('android/app/build.gradle', 'utf8');
const realtimePath = 'android/app/src/main/java/com/buysloans/hub/CatalogueRealtimeSync.kt';

test('Android catalogue sync uses authenticated Realtime with bounded recovery instead of one-second polling', () => {
  assert.equal(fs.existsSync(realtimePath), true, 'CatalogueRealtimeSync.kt must exist');
  const realtime = fs.readFileSync(realtimePath, 'utf8');

  assert.match(app, /CatalogueRealtimeSync\.ensure\(this\)/);
  assert.match(app, /CatalogueRealtimeSync\.stop\(\)/);
  assert.doesNotMatch(app, /delay\(1_000L\)/);

  assert.match(realtime, /RECOVERY_RECONCILE_MS\s*=\s*60_000L/);
  assert.match(realtime, /table\s*=\s*"catalog_sync_state"/);
  assert.match(realtime, /AuthManager\.validAccessToken\(appContext\)/);
  assert.match(realtime, /BuildConfig\.SUPABASE_PUBLISHABLE_KEY/);
  assert.doesNotMatch(realtime, /service[_-]?role/i);

  assert.match(gradle, /supabase:bom:/);
  assert.match(gradle, /realtime-kt/);
  assert.match(gradle, /ktor-client-websockets/);
});
