import assert from 'node:assert/strict';
import { createTurnstileController } from '../src/turnstile.mjs';

let rendered=null, resetId=null;
const api={
  render(container,opts){rendered={container,opts};return 17;},
  reset(id){resetId=id;}
};
const controller=createTurnstileController({siteKey:'site_test',loader:async()=>api});
let token='',expired=false,failed=false;
const id=await controller.mount('#captcha',{onToken:value=>token=value,onExpired:()=>expired=true,onError:()=>failed=true});
assert.equal(id,17);
assert.equal(rendered.container,'#captcha');
assert.equal(rendered.opts.sitekey,'site_test');
rendered.opts.callback('abc');
assert.equal(token,'abc');
rendered.opts['expired-callback']();
assert.equal(expired,true);
rendered.opts['error-callback']();
assert.equal(failed,true);
controller.reset();
assert.equal(resetId,17);
console.log('turnstile: ok');
