import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

const shared=fs.readFileSync('morley-auth-client.js','utf8');
const nova=fs.readFileSync('nova/app.js','utf8');

function factory(){
  const window={dispatchEvent(){}};
  const context={window,CustomEvent:function(){},URL,console};
  vm.runInNewContext(shared,context,{filename:'morley-auth-client.js'});
  return window.MorleyAuthClient;
}
function response(data,{ok=true,status=200}={}){return{ok,status,json:async()=>data}}

test('shared auth client is policy-free and carries no privileged secret',()=>{
  assert.doesNotMatch(shared,/service[_ -]?role/i);
  assert.doesNotMatch(shared,/openai|anthropic|gemini|api[_ -]?secret/i);
  assert.doesNotMatch(shared,/roles:\s*\['admin'\]/);
  assert.match(shared,/roles=\[\]/);
  assert.match(shared,/roles\.length===0\)return false/);
});

test('profile validation fails closed and preserves explicit role policy',async()=>{
  const calls=[];
  const client=factory().create({url:'https://example.supabase.co',publishableKey:'public-key',fetchImpl:async url=>{
    calls.push(String(url));
    if(String(url).endsWith('/auth/v1/user'))return response({id:'user-1'});
    return response([{role:'admin',is_enabled:true}]);
  }});
  const session={access_token:'token',user:{id:'user-1'}};
  assert.equal(await client.validateProfile(session),false);
  assert.equal(calls.length,0,'missing role policy must fail before network access');
  assert.equal(await client.validateProfile(session,{roles:['admin']}),true);
  assert.equal(calls[0],'https://example.supabase.co/auth/v1/user');
  assert.match(calls[1],/\/rest\/v1\/profiles\?select=role,is_enabled&id=eq.user-1$/);
  assert.equal(await client.validateProfile(session,{roles:['manager']}),false);
});

test('password exchange requires and forwards the Turnstile captcha token',async()=>{
  const calls=[];
  const client=factory().create({url:'https://example.supabase.co',publishableKey:'public-key',fetchImpl:async(url,options)=>{calls.push({url:String(url),options});return response({access_token:'a',refresh_token:'r',user:{id:'u'}})}});
  await assert.rejects(()=>client.signInPassword({email:'a@example.com',password:'pw'}),/Security check is required/);
  assert.equal(calls.length,0);
  await client.signInPassword({email:'a@example.com',password:'pw',captchaToken:'captcha-123'});
  assert.match(calls[0].url,/\/auth\/v1\/token\?grant_type=password$/);
  const body=JSON.parse(calls[0].options.body);
  assert.equal(body.gotrue_meta_security.captcha_token,'captcha-123');
  assert.equal(calls[0].options.headers.apikey,'public-key');
});

test('refresh uses the existing refresh-token grant and returns null on failure',async()=>{
  let seen;
  const client=factory().create({url:'https://example.supabase.co',publishableKey:'public-key',fetchImpl:async(url,options)=>{seen={url:String(url),options};return response({access_token:'new',refresh_token:'next',user:{id:'u'}})}});
  const refreshed=await client.refreshSession({refresh_token:'old'});
  assert.equal(refreshed.access_token,'new');
  assert.match(seen.url,/grant_type=refresh_token$/);
  assert.deepEqual(JSON.parse(seen.options.body),{refresh_token:'old'});
  assert.equal(await client.refreshSession({}),null);
});

test('Nova delegates primitives but retains Admin-only Turnstile and network gates',()=>{
  assert.match(nova,/\.\.\/morley-auth-client\.js\?v=1/);
  assert.doesNotMatch(nova,/function apiHeaders\(/);
  assert.doesNotMatch(nova,/function readJson\(/);
  assert.doesNotMatch(nova,/function authorisedProfile\(/);
  assert.doesNotMatch(nova,/function refreshSession\(/);
  assert.equal((nova.match(/validateProfile\([^\n]+roles:\['admin'\]/g)||[]).length,3);
  assert.match(nova,/client\.signInPassword\(\{email,password,captchaToken\}\)/);
  assert.match(nova,/const TURNSTILE_SITE_KEY=/);
  assert.match(nova,/function loadTurnstile\(/);
  assert.match(nova,/networkReady/);
  assert.match(nova,/shouldGateFetch/);
  assert.match(nova,/sessionStorage\.setItem\(SESSION_KEY/);
  assert.match(nova,/This account is not authorised for Nova AI\./);
});