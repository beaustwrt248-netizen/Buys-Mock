import assert from 'node:assert/strict';
import { createAuthController } from '../src/auth-controller.mjs';

function store(initial=null){let value=initial;return{load:()=>value,save:v=>(value=v),clear:()=>{value=null},peek:()=>value};}

const valid={access_token:'a',refresh_token:'r',user:{id:'u1',email:'admin@example.com'}};
const states=[];
const sessionStore=store(valid);
const controller=createAuthController({
  authClient:{
    validateAdminProfile:async s=>s.access_token==='a',
    refreshSession:async()=>null,
    signInPassword:async()=>valid
  },
  sessionStore,
  onState:s=>states.push(s.status)
});
const restored=await controller.restore();
assert.equal(restored.status,'authenticated');
assert.equal(controller.getAccessToken(),'a');
assert.deepEqual(states,['checking','authenticated']);
controller.signOut();
assert.equal(controller.getAccessToken(),'');
assert.equal(sessionStore.peek(),null);

const refreshStore=store({access_token:'expired',refresh_token:'r',user:{id:'u1'}});
const refreshController=createAuthController({
  authClient:{
    validateAdminProfile:async s=>s.access_token==='fresh',
    refreshSession:async()=>({access_token:'fresh',refresh_token:'r2',user:{id:'u1'}}),
    signInPassword:async()=>null
  },
  sessionStore:refreshStore
});
assert.equal((await refreshController.restore()).status,'authenticated');
assert.equal(refreshStore.peek().access_token,'fresh');

const deniedStore=store();
const denied=createAuthController({
  authClient:{
    validateAdminProfile:async()=>false,
    refreshSession:async()=>null,
    signInPassword:async()=>({access_token:'x',refresh_token:'r',user:{id:'u2'}})
  },
  sessionStore:deniedStore
});
await assert.rejects(()=>denied.signIn({email:'staff@example.com',password:'secret',captchaToken:'human'}),/AUTH_FORBIDDEN/);
assert.equal(deniedStore.peek(),null);

const signedStore=store();
const signed=createAuthController({
  authClient:{
    validateAdminProfile:async()=>true,
    refreshSession:async()=>null,
    signInPassword:async({captchaToken})=>{assert.equal(captchaToken,'human');return valid;}
  },
  sessionStore:signedStore
});
assert.equal((await signed.signIn({email:'admin@example.com',password:'secret',captchaToken:'human'})).status,'authenticated');
assert.equal(signedStore.peek().access_token,'a');
console.log('auth-controller: ok');
