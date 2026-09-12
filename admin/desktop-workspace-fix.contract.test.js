const fs=require('fs');
const css=fs.readFileSync('admin/desktop-workspace-fix.css','utf8');
const workspace=fs.readFileSync('admin/workspace.html','utf8');
for(const token of [
  '@media (min-width:961px)',
  'overflow-x:hidden!important',
  'padding-left:268px!important',
  'width:232px!important',
  '.admin-user-card{display:grid!important',
  '.admin-user-access{display:grid!important',
  '.admin-user-name-editor{display:grid!important',
  '.admin-user-button-grid{display:grid!important',
  'minmax(0,1fr)'
])if(!css.includes(token))throw new Error('missing Admin desktop layout rule: '+token);
if(!workspace.includes('desktop-workspace-fix.css?v=1'))throw new Error('final desktop workspace authority is not loaded');
if(/@media\s*\(max-width:960px\)/.test(css))throw new Error('desktop workspace fix must not override the mobile workspace authority');
console.log('Admin desktop workspace contract passed');
