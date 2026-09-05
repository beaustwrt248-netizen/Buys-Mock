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

assert.match(source, /ticketDetailVersion=0/, 'ticket detail must track the newest open request');
assert.match(source, /const requestVersion=\+\+ticketDetailVersion;/, 'each ticket detail load must claim a new request version');
const detailGuards = source.match(/if\(requestVersion!==ticketDetailVersion\)return;/g) || [];
assert.ok(detailGuards.length >= 2, 'ticket detail must reject stale results after the main fetch and after profile attachment');

const detailStart = source.indexOf('async function openTicket(id)');
const detailFetch = source.indexOf('await Promise.all', detailStart);
const detailFirstGuard = source.indexOf('if(requestVersion!==ticketDetailVersion)return;', detailFetch);
const detailAttach = source.indexOf('await attachTicketProfiles', detailFirstGuard);
const detailSecondGuard = source.indexOf('if(requestVersion!==ticketDetailVersion)return;', detailAttach);
const detailCommit = source.indexOf('currentTicket=t;', detailSecondGuard);
assert.ok(detailStart >= 0 && detailFirstGuard > detailFetch, 'stale ticket fetches must be rejected before rendering errors or details');
assert.ok(detailSecondGuard > detailAttach && detailSecondGuard < detailCommit, 'stale enriched ticket details must be rejected before becoming current');

console.log('admin support async freshness regression: PASS');
