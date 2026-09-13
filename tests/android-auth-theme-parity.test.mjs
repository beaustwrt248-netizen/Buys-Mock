import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const auth = readFileSync(
  new URL('../android/app/src/main/java/com/buysloans/hub/AuthActivity.kt', import.meta.url),
  'utf8',
);

test('Android auth reuses the shared Morley blue visual tokens', () => {
  assert.match(auth, /private val AuthPrimary\s*=\s*MorleyAccent\b/);
  assert.match(auth, /private val AuthAccent\s*=\s*MorleyAccent\b/);
  assert.match(auth, /private val AuthBg\s*=\s*MorleyBackground\b/);
  assert.match(auth, /private val AuthCard\s*=\s*MorleySurface\b/);
  assert.doesNotMatch(auth, /Color\(0xFF167A5A\)|Color\(0xFF77E9C4\)/);
});
