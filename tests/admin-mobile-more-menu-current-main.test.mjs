import fs from 'node:fs';
import assert from 'node:assert/strict';

const css=fs.readFileSync('admin/admin-mobile-more.css','utf8');
const linkingCss=fs.readFileSync('admin/admin-lifecycle-linking.css','utf8');

assert.match(linkingCss,/@import url\('admin-mobile-more\.css\?v=1'\)/);
assert.match(css,/body\.admin-v2 \.admin-more-menu\{[^}]*width:min\(360px,calc\(100vw - 28px\)\)!important/);
assert.match(css,/body\.admin-v2 \.admin-more-menu\{[^}]*max-height:min\(44vh,300px\)!important/);
assert.match(css,/body\.admin-v2 \.admin-more-menu button,body\.admin-v2 \.admin-more-menu a\{[^}]*display:flex!important/);
assert.match(css,/body\.admin-v2 \.admin-more-menu button,body\.admin-v2 \.admin-more-menu a\{[^}]*color:#fff!important/);
assert.match(css,/body\.admin-v2 \.admin-more-menu button,body\.admin-v2 \.admin-more-menu a\{[^}]*text-decoration:none!important/);
assert.match(css,/body\.admin-v2 \.admin-more-menu a:visited\{color:#fff!important\}/);
assert.match(css,/@media\(max-width:520px\)[\s\S]*width:min\(340px,calc\(100vw - 20px\)\)!important/);
