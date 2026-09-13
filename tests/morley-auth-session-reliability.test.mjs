import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const webAuth=readFileSync(new URL('../web-auth.js',import.meta.url),'utf8');
const signout=readFileSync(new URL('../secure-current-signout.js',import.meta.url),'utf8');
const adminBootstrap=readFileSync(new URL('../admin/browser-auth-bootstrap.js',import.meta.url),'utf8');
const adminWorkspace=readFileSync(new URL('../admin/workspace.html',import.meta.url),'utf8');
const adminApp=readFileSync(new URL('../admin/app.js',import.meta.url),'utf8');

const states=['booting','signed_out','challenge_required','authenticated_pending_context','authenticated_ready','offline_recoverable','error_recoverable'];

test('Morley web auth exposes finite startup states and a single-flight bootstrap owner',()=>{
  for(const state of states)assert.ok(webAuth.includes(state),`missing auth state: ${state}`);
  assert.match(webAuth,/bootstrapPromise/);
  assert.match(webAuth,/function setAuthState/);
  assert.match(webAuth,/function bootstrapAuth/);
});

test('Morley web restore distinguishes recoverable provider failure from confirmed sign-out',()=>{
  assert.match(webAuth,/offline_recoverable/);
  assert.match(webAuth,/error_recoverable/);
  assert.match(webAuth,/isRecoverableAuthError/);
  assert.match(webAuth,/verifyAuthorised/);
  assert.doesNotMatch(webAuth,/async function validSession\(\)[\s\S]*?catch\s*\{\s*clearSession\(\);return null\s*\}/);
});

test('Morley web signout only clears local auth after provider outcome is known',()=>{
  const fetchAt=signout.indexOf('await fetch(');
  const clearAt=signout.indexOf('localStorage.removeItem(STORE)');
  assert.ok(fetchAt>=0&&clearAt>fetchAt,'provider logout must be attempted before local session clear');
  assert.match(signout,/response\.ok/);
  assert.match(signout,/morley:signout-error/);
  assert.match(signout,/handling=false/);
});

test('Admin browser login bootstrap is single-flight and status driven',()=>{
  assert.match(adminBootstrap,/bootstrapPromise/);
  assert.match(adminBootstrap,/function setBootstrapState/);
  assert.match(adminBootstrap,/error_recoverable/);
  assert.match(adminBootstrap,/offline_recoverable/);
  assert.match(adminBootstrap,/window\.loadSession=loadSession/);
});

test('Admin workspace publishes authorised context before privileged app loads',()=>{
  const contextAt=adminWorkspace.indexOf('window.__morleyAdminAuthContext');
  const appAt=adminWorkspace.indexOf("'app.js?");
  assert.ok(contextAt>=0,'workspace must publish authorised context');
  assert.ok(appAt<0||contextAt<appAt,'authorised context must exist before app.js loads');
  assert.match(adminWorkspace,/authenticated_ready/);
});

test('Admin app consumes workspace-authorised startup context instead of bootstrapping again',()=>{
  assert.match(adminApp,/window\.__morleyAdminAuthContext/);
  assert.match(adminApp,/authenticated_ready/);
  assert.doesNotMatch(adminApp,/loadSession\(\);\s*$/m);
  assert.match(adminApp,/logoutInFlight/);
});
