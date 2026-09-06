'use strict';

const fs = require('fs');
const assert = require('assert');

const source = fs.readFileSync('web-route-contract-fix.js', 'utf8');
const shell = fs.readFileSync('index.html', 'utf8');

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

assert(
  shell.includes('const requiredRecentRender='),
  'srcdoc bootstrap must identify the source recent-activity render write before iframe creation'
);
assert(
  shell.includes('Morley render invariant failed: missing #recent target in embedded workspace.'),
  'a missing required srcdoc render target must produce a controlled invariant diagnostic'
);
assert(
  shell.includes('html=html.replace(requiredRecentRender,guardedRecentRender);'),
  'srcdoc bootstrap must install the guarded recent-activity renderer before assigning srcdoc'
);

console.log('Web srcdoc History API and required render-target regression contracts verified');
