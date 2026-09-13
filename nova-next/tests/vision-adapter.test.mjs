import assert from 'node:assert/strict';
import { createVisionAdapter } from '../src/adapters/vision-adapter.mjs';

const calls=[];
const vision=createVisionAdapter({edgeClient:{invoke:async(name,body)=>{calls.push({name,body});return {ok:true,privacy:{stored:false},result:{likely_model:'Pixel'}};}}});
const jpeg='data:image/jpeg;base64,AAAA';
const png='data:image/png;base64,BBBB';
const result=await vision.analyse([jpeg,png],{hint:'front and rear'});
assert.equal(result.ok,true);
assert.deepEqual(calls[0],{name:'nova-vision',body:{image_data_urls:[jpeg,png],hint:'front and rear'}});
await assert.rejects(()=>vision.analyse([]),/IMAGE_REQUIRED/);
await assert.rejects(()=>vision.analyse(['data:text/plain;base64,QQ==']),/IMAGE_TYPE/);
await assert.rejects(()=>vision.analyse(Array(7).fill(jpeg)),/IMAGE_COUNT/);
await assert.rejects(()=>vision.analyse(['data:image/jpeg;base64,'+'A'.repeat(8_000_001)]),/IMAGE_SIZE/);
console.log('vision-adapter: ok');
