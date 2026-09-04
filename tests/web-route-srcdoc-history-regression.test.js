'use strict';

const fs = require('fs');
const assert = require('assert');

const source = fs.readFileSync('web-route-contract-fix.js', 'utf8');

assert(
  source.includes("const canWriteHistory=location.protocol==='http:'||location.protocol==='https:';"),
  'route contract must explicitly restrict History API writes to HTTP(S) documents'
);
assert(
  source.includes('if(canWriteHistory&&location.hash!==hash)'),
  'srcdoc/about documents must skip pushState and replaceState'
);
assert(
  source.includes("history.pushState({morleyPage:page},'',hash)") &&
    source.includes("history.replaceState({morleyPage:page},'',hash)"),
  'normal top-level HTTP(S) route history behavior must remain intact'
);

console.log('Web srcdoc History API regression contract verified');
