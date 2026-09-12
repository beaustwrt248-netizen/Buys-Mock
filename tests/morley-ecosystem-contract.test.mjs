import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import {spawnSync} from 'node:child_process';

const source=fs.readFileSync(new URL('../morley-core.js',import.meta.url),'utf8');
const architecture=fs.readFileSync(new URL('../docs/MORLEY_ECOSYSTEM_ARCHITECTURE.md',import.meta.url),'utf8');
const webIndex=fs.readFileSync(new URL('../index.html',import.meta.url),'utf8');
const novaApp=fs.readFileSync(new URL('../nova/app.js',import.meta.url),'utf8');
const adminPresentation=fs.readFileSync(new URL('../admin/ecosystem-presentation.js',import.meta.url),'utf8');
const guardianBranding=fs.readFileSync(new URL('../admin/guardian-branding.js',import.meta.url),'utf8');
const guardianHtml=fs.readFileSync(new URL('../admin/guardian.html',import.meta.url),'utf8');

test('ecosystem exposes exactly three user-facing product definitions',()=>{
  assert.match(source,/id:'morley-buys'/);
  assert.match(source,/id:'morley-admin'/);
  assert.match(source,/id:'nova'/);
  assert.match(source,/product:false/);
});

test('Guardian remains a Nova enforcement layer and fails closed',()=>{
  assert.match(source,/parent:'nova'/);
  assert.match(source,/Nova cannot disable, bypass or weaken Guardian\./);
  assert.match(source,/Missing enforcement evidence fails closed\./);
  assert.match(architecture,/Guardian is not a fourth product or competing assistant\./);
});

test('Morley Core owns the shared source-of-truth domains',()=>{
  for(const domain of ['catalogue','pricing','identity','roles','media','audit-events','search','integrations','notifications','realtime-events']){
    assert.ok(source.includes(`'${domain}'`),`missing canonical domain: ${domain}`);
  }
  assert.match(architecture,/Realtime is the default propagation mechanism/);
  assert.match(architecture,/existing realtime implementation is the canonical propagation path/i);
});

test('destructive and privileged actions remain human gated',()=>{
  assert.match(architecture,/destructive deletes/);
  assert.match(architecture,/user\/role changes/);
  assert.match(architecture,/release\/deployment/);
  assert.match(architecture,/protected pricing writes\/approval/);
});

test('Morley Buys, Nova and Admin consume the ecosystem contract at runtime',()=>{
  assert.match(webIndex,/'morley-core\.js\?v=1'/);
  assert.match(novaApp,/\.\.\/morley-core\.js\?v=1/);
  assert.match(adminPresentation,/\.\.\/morley-core\.js\?v=1/);
  assert.match(guardianBranding,/\.\.\/morley-core\.js\?v=1/);
});

test('Guardian is presented as Nova Security without renaming protected internals',()=>{
  assert.match(novaApp,/nav\.textContent='Security'/);
  assert.match(novaApp,/heading\.textContent='Guardian Enforcement'/);
  assert.match(adminPresentation,/link\.textContent='Nova Security'/);
  assert.match(guardianBranding,/Nova Security · Guardian Enforcement/);
  assert.match(guardianHtml,/guardian-branding\.js\?v=1/);
  assert.match(guardianHtml,/id="guardianKillSwitch"/);
  assert.match(guardianHtml,/guardian\.js\?v=6/);
});

test('Guardian compatibility surface validates the canonical Nova parent boundary at runtime',()=>{
  assert.match(guardianBranding,/guardian\?\.parent==='nova'/);
  assert.match(guardianBranding,/guardian\?\.product===false/);
  assert.match(guardianBranding,/dataset\.guardianContract=contractValid\(\)\?'validated':'pending'/);
  assert.match(guardianBranding,/morley:ecosystem-ready/);
});

test('live Admin browser auth controller actually executes instead of leaving the static placeholder', {timeout:45000}, ()=>{
  const lookup=spawnSync('bash',['-lc','command -v google-chrome || command -v google-chrome-stable || command -v chromium || command -v chromium-browser'],{encoding:'utf8'});
  assert.equal(lookup.status,0,`No headless Chrome/Chromium available on runner: ${lookup.stderr||lookup.stdout}`);
  const chrome=lookup.stdout.trim().split(/\r?\n/)[0];
  const url=`https://buyshub.me/admin/?morley_runtime_diag=${Date.now()}`;
  const result=spawnSync(chrome,[
    '--headless=new','--disable-gpu','--no-sandbox','--disable-dev-shm-usage',
    '--virtual-time-budget=18000','--dump-dom',url
  ],{encoding:'utf8',timeout:30000,maxBuffer:16*1024*1024});
  assert.equal(result.status,0,`Headless Admin load failed. stderr: ${result.stderr}`);
  const dom=result.stdout;
  assert.match(dom,/login-security\.js\?v=10/,`Live Admin HTML is not serving the expected v10 auth controller. DOM: ${dom.slice(0,3000)}`);
  assert.doesNotMatch(dom,/id="adminTurnstileFrame"/,`Admin auth controller never replaced its static iframe placeholder, so login-security.js did not execute. stderr: ${result.stderr}\nDOM: ${dom.slice(0,5000)}`);
  assert.doesNotMatch(dom,/id="challengeStatus"[^>]*>Security check loading…<\/div>/,`Admin auth controller remained stuck in its initial loading state after 18s. stderr: ${result.stderr}\nDOM: ${dom.slice(0,5000)}`);
});
