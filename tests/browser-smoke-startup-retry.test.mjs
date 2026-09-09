import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';

const src=()=>readFile(new URL('./browser-responsive-smoke.js',import.meta.url),'utf8');

test('browser responsive smoke retries transient Chrome startup only',async()=>{
  const code=await src();
  assert.match(code,/const CHROME_START_ATTEMPTS=2/);
  assert.match(code,/async function launchChrome\(chrome\)/);
  assert.match(code,/await stopChrome\(started\.proc\)/);
  assert.match(code,/fs\.rmSync\(started\.profile,\{recursive:true,force:true\}\)/);
  assert.match(code,/if\(attempt<CHROME_START_ATTEMPTS\)await sleep\(750\)/);
});

test('browser smoke still fails after bounded retry exhaustion',async()=>{
  const code=await src();
  assert.match(code,/throw new Error\(`\$\{lastError\?\.message\|\|'Chrome failed to start'\} after \$\{CHROME_START_ATTEMPTS\} attempts`\)/);
  assert.doesNotMatch(code,/while\s*\(true\)/);
});
