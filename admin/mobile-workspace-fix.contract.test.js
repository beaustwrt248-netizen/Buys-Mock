const fs=require('fs');
const css=fs.readFileSync('admin/mobile-workspace-fix.css','utf8');
const js=fs.readFileSync('admin/mobile-workspace-fix.js','utf8');
const home=fs.readFileSync('admin/admin-home.js','utf8');
for(const token of ['#tab-overview.hidden{display:none!important}','#adminHomeQuick{display:none!important}','#usersList>.row .actions{display:none!important}','#usersList>.row.mobile-expanded .actions{display:flex!important}','#tab-release .grid{grid-template-columns:repeat(2,minmax(0,1fr))!important','padding-bottom:calc(92px + env(safe-area-inset-bottom))!important','.tabs{position:fixed!important'])if(!css.includes(token))throw new Error('missing Admin mobile workspace rule: '+token);
for(const token of ['enforceSingleWorkspace','panel.id!==id','admin-mobile-user-toggle','enhanceReleaseIntegrity','moveToWorkspaceTop'])if(!js.includes(token))throw new Error('missing Admin mobile workspace behavior: '+token);
if(!home.includes('mobile-workspace-fix.css?v=1')||!home.includes('mobile-workspace-fix.js?v=1'))throw new Error('final mobile workspace authority is not loaded');
if(css.includes('#tab-overview{display:block!important}'))throw new Error('mobile workspace must never force Overview visible');
console.log('Admin mobile workspace contract passed');
