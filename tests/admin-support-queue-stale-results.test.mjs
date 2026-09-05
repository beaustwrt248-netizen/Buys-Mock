import fs from 'node:fs';
import assert from 'node:assert/strict';

const source = fs.readFileSync('admin/support-tickets.js', 'utf8');

assert.match(source, /ticketLoadVersion=0/, 'support queue must track the newest load request');
assert.match(source, /const requestVersion=\+\+ticketLoadVersion;/, 'each support queue load must claim a new request version');

const staleGuards = source.match(/if\(requestVersion!==ticketLoadVersion\)return;/g) || [];
assert.ok(staleGuards.length >= 2, 'support queue must reject stale results both before rendering errors and after profile attachment');

const firstGuard = source.indexOf('if(requestVersion!==ticketLoadVersion)return;');
const errorRender = source.indexOf("if(error){q('ticketsList').textContent=");
assert.ok(firstGuard >= 0 && firstGuard < errorRender, 'a stale failed request must not overwrite a newer queue state');

const attachProfiles = source.indexOf('const tickets=await attachTicketProfiles(data);');
const secondGuard = source.indexOf('if(requestVersion!==ticketLoadVersion)return;', firstGuard + 1);
const renderRows = source.indexOf("q('ticketsList').innerHTML=rows.map");
assert.ok(attachProfiles >= 0 && secondGuard > attachProfiles && secondGuard < renderRows, 'a stale profile-enriched request must not render over newer filters');

console.log('admin support queue stale-result regression: PASS');
