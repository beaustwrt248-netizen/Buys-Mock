import assert from 'node:assert/strict';
import fs from 'node:fs';

const html=fs.readFileSync('nova/index.html','utf8');
const js=fs.readFileSync('nova/app.js','utf8');
const css=fs.readFileSync('nova/styles.css','utf8');

assert.match(html,/app\.js\?v=2/);
assert.doesNotMatch(html,/<script src="app-core\.js/);
assert.match(css,/html:not\(\.nova-auth-unlocked\) \.app-shell\{visibility:hidden\}/);
assert.match(js,/networkReady\.then/);
assert.match(js,/function authorisedProfile\(session\)/);
assert.match(js,/profile\.role==='admin'\|\|profile\.role==='manager'/);
assert.match(js,/profile\?\.is_enabled/);
assert.match(js,/if\(!captchaToken\)/);
assert.match(js,/app-core\.js\?v=4/);
assert.match(js,/sessionStorage\.setItem/);
assert.doesNotMatch(js,/localStorage\.setItem/);
console.log('Nova standalone auth/runtime gate contract passed');
