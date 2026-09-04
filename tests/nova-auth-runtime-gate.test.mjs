import assert from 'node:assert/strict';
import fs from 'node:fs';

const html=fs.readFileSync('admin/morley-ai.html','utf8');
const js=fs.readFileSync('admin/nova-control-centre.js','utf8');
const bootstrap=fs.readFileSync('admin/nova-auth-bootstrap.js','utf8');
const css=fs.readFileSync('admin/nova-control-centre.css','utf8');

assert.match(html,/nova-auth-bootstrap\.js\?v=1/);
assert.doesNotMatch(html,/<script src="morley-ai-core\.js/);
assert.match(bootstrap,/dataset\.novaAuthorised='true'/);
assert.match(bootstrap,/for\(const src of \['morley-ai-core\.js\?v=1'/);
assert.match(bootstrap,/withTimeout/);
assert.match(bootstrap,/Nova could not start safely/);
assert.match(js,/!document\.documentElement\.dataset\.novaAuthorised/);
assert.doesNotMatch(js,/setTimeout\(refresh,250\)/);
assert.match(css,/#edf3ef/);
assert.match(css,/#194f41/);
console.log('Nova auth/runtime gating and Morley Admin theme contract passed');
