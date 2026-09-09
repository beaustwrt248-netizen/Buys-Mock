import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';

const auth=()=>readFile(new URL('../android/app/src/main/java/com/buysloans/hub/AuthManager.kt',import.meta.url),'utf8');
const gate=()=>readFile(new URL('../android/app/src/main/java/com/buysloans/hub/TemporaryPasswordGateActivity.kt',import.meta.url),'utf8');
const manifest=()=>readFile(new URL('../android/app/src/main/AndroidManifest.xml',import.meta.url),'utf8');

test('profile verification reuse is process-local, token-bound and short lived',async()=>{
  const code=await auth();
  assert.match(code,/PROFILE_VERIFY_REUSE_MS = 15_000L/);
  assert.match(code,/@Volatile private var lastVerifiedProfileToken/);
  assert.match(code,/SystemClock\.elapsedRealtime\(\) - lastVerifiedProfileAtElapsedMs/);
  assert.match(code,/token != lastVerifiedProfileToken/);
  assert.match(code,/age in 0\.\.PROFILE_VERIFY_REUSE_MS/);
  assert.match(code,/lastVerifiedProfileToken = ""/);
  assert.match(code,/lastVerifiedProfileAtElapsedMs = 0L/);
});

test('current valid token may reuse only a just-completed profile check while refreshed tokens always reverify',async()=>{
  const code=await auth();
  assert.match(code,/if \(!profileWasJustVerified\(access\) && !verifyAndCacheProfile\(context, access\)\)/);
  assert.match(code,/saveSession\(context, body\)[\s\S]*val token = accessToken\(context\)[\s\S]*if \(!verifyAndCacheProfile\(context, token\)\)/);
  assert.match(code,/suspend fun signIn[\s\S]*if \(!verifyAndCacheProfile\(context, token\)\)/);
});

test('temporary password gate still performs the authenticated user check and remains non-exported',async()=>{
  const gateCode=await gate();
  const manifestCode=await manifest();
  assert.match(gateCode,/AuthManager\.validAccessToken\(this@TemporaryPasswordGateActivity\)/);
  assert.match(gateCode,/authUserRequest\("GET", token, null\)/);
  assert.match(gateCode,/\/auth\/v1\/user/);
  assert.match(manifestCode,/TemporaryPasswordGateActivity" android:exported="false"/);
});
