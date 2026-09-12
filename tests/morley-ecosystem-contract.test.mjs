import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const source=fs.readFileSync(new URL('../morley-core.js',import.meta.url),'utf8');
const architecture=fs.readFileSync(new URL('../docs/MORLEY_ECOSYSTEM_ARCHITECTURE.md',import.meta.url),'utf8');
const webIndex=fs.readFileSync(new URL('../index.html',import.meta.url),'utf8');
const novaApp=fs.readFileSync(new URL('../nova/app.js',import.meta.url),'utf8');
const adminPresentation=fs.readFileSync(new URL('../admin/ecosystem-presentation.js',import.meta.url),'utf8');
const guardianBranding=fs.readFileSync(new URL('../admin/guardian-branding.js',import.meta.url),'utf8');
const guardianHtml=fs.readFileSync(new URL('../admin/guardian.html',import.meta.url),'utf8');
const adminWorkspace=fs.readFileSync(new URL('../admin/workspace.html',import.meta.url),'utf8');
const adminHome=fs.readFileSync(new URL('../admin/admin-home.js',import.meta.url),'utf8');
const adminIntelligence=fs.readFileSync(new URL('../admin/intelligence-command-centre.js',import.meta.url),'utf8');
const adminDownloadInvites=fs.readFileSync(new URL('../admin/download-invites.js',import.meta.url),'utf8');
const morleyEmail=fs.readFileSync(new URL('../supabase/functions/send-morley-email/index.ts',import.meta.url),'utf8');
const autoReviewWorkflow=fs.readFileSync(new URL('../.github/workflows/auto-review-merge.yml',import.meta.url),'utf8');

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

test('Admin desktop authority loads last and home boot is bounded',()=>{
  assert.doesNotMatch(adminWorkspace,/desktop-workspace-fix\.css/);
  assert.match(adminWorkspace,/admin-home\.js\?v=8/);
  assert.match(adminHome,/id='adminDesktopWorkspaceFixCss'/);
  assert.match(adminHome,/desktop-workspace-fix\.css\?v=2/);
  assert.doesNotMatch(adminHome,/setInterval\(/);
  assert.doesNotMatch(adminHome,/MutationObserver\([^)]*\)\.observe\(q\('#appView'\)\|\|document\.body,\{subtree:true,childList:true,attributes:true/);
  assert.doesNotThrow(()=>new Function(adminHome));
});

test('Admin Intelligence bootstrap observes only appView readiness',()=>{
  assert.doesNotMatch(adminIntelligence,/observe\(document\.documentElement,\{subtree:true,attributes:true,attributeFilter:\['class'\]\}\)/);
  assert.match(adminIntelligence,/observe\(app,\{attributes:true,attributeFilter:\['class'\]\}\)/);
  assert.doesNotThrow(()=>new Function(adminIntelligence));
});

test('Admin app download invite is emailed through the existing audited mail service',()=>{
  assert.match(adminDownloadInvites,/textContent='Email app download invite'/);
  assert.match(adminDownloadInvites,/action:'send_download_invite'/);
  assert.match(adminDownloadInvites,/Invitation emailed to/);
  assert.match(morleyEmail,/action === "send_download_invite"/);
  assert.match(morleyEmail,/app_download_invite_sent/);
  assert.match(morleyEmail,/Download \/ Open invitation/);
});

test('guarded auto review exempts only documentation service-role wording',()=>{
  assert.match(autoReviewWorkflow,/service_role_pattern=re\.compile\(r'\\bservice\[_-\]\?role\\b'/);
  assert.match(autoReviewWorkflow,/document_path=re\.compile\(r'\(\^\|\/\)\(docs\?\/\|\[\^\/\]\+\\\.md\$\)'/);
  assert.match(autoReviewWorkflow,/if service_role_pattern\.search\(file_added\) and not document_path\.search\(name\):/);
  assert.doesNotMatch(autoReviewWorkflow,/def safe_security_reference/);
});

test('guarded auto review keeps every other critical pattern global',()=>{
  assert.match(autoReviewWorkflow,/added='\\n'\.join\(added_by_file\.values\(\)\)/);
  assert.match(autoReviewWorkflow,/for pattern in critical_added:/);
  assert.match(autoReviewWorkflow,/re\.search\(pattern, added, re\.I\|re\.M\)/);
  assert.match(autoReviewWorkflow,/persist-credentials/);
  assert.match(autoReviewWorkflow,/disable\\s\+row\\s\+level\\s\+security/);
});
