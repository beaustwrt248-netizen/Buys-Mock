// Guardian regression: non-HTTP(S) embedded documents must never own browser history.
const fs=require('fs');
const assert=require('assert');

const source=fs.readFileSync('desktop-parity.js','utf8');

assert.match(source,/const canWriteHistory=location\.protocol==='http:'\|\|location\.protocol==='https:'/,'desktop parity must restrict History API writes to HTTP(S) documents');
assert.match(source,/if\(canWriteHistory && !handlingPop && location\.hash!==hash\)/,'route changes must skip pushState/replaceState in about:srcdoc');
assert.match(source,/if\(canWriteHistory\) history\.replaceState\(\{morleyPage:page\},'',`#\$\{page\}`\);/,'route restoration must skip replaceState in about:srcdoc');

const guardedWrites=(source.match(/canWriteHistory/g)||[]).length;
assert.ok(guardedWrites>=4,'expected both route-change and restore-route History API guards');

console.log('desktop parity srcdoc history regression: PASS');
