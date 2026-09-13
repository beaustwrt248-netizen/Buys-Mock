import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

const source=fs.readFileSync('morley-auth-client.js','utf8');
function factory(fetchImpl){
  const window={dispatchEvent(){}};
  class TestAbortController{constructor(){this.signal={aborted:false}}abort(){this.signal.aborted=true}}
  const context={window,CustomEvent:function(){},URL,console,setTimeout,clearTimeout,AbortController:TestAbortController};
  vm.runInNewContext(source,context,{filename:'morley-auth-client.js'});
  return window.MorleyAuthClient.create({url:'https://example.supabase.co',publishableKey:'public-key',fetchImpl});
}

test('refresh preserves provider/network failure instead of converting it to signed-out null',async()=>{
  const client=factory(async()=>{throw new TypeError('network down')});
  await assert.rejects(()=>client.refreshSession({refresh_token:'existing'}),error=>{
    assert.equal(error.code,'AUTH_NETWORK');
    return true;
  });
});

test('shared auth client exports stable failure classifier without embedding session data',()=>{
  assert.match(source,/function classifyError/);
  assert.match(source,/AUTH_TIMEOUT/);
  assert.match(source,/AUTH_NETWORK/);
  assert.doesNotMatch(source,/console\.(log|error).*access_token/);
});
