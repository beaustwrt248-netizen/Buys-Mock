import fs from 'node:fs';
import assert from 'node:assert/strict';

const source = fs.readFileSync('admin/support-tickets.js', 'utf8');

const saveStart = source.indexOf('async function saveTicket()');
const replyStart = source.indexOf('async function reply()');
assert.ok(saveStart >= 0 && replyStart > saveStart, 'support save and reply actions must exist');

const saveSource = source.slice(saveStart, replyStart);
assert.match(saveSource, /const ticketId=currentTicket\.id/, 'save must snapshot the selected ticket id before awaiting the update');
assert.match(saveSource, /\.eq\('id',ticketId\)/, 'save must target the snapshotted ticket id');
assert.doesNotMatch(saveSource, /\.eq\('id',currentTicket\.id\)/, 'save must not re-read mutable currentTicket for its database target');
assert.match(saveSource, /if\(isCurrent\(\)\)await openTicket\(ticketId\)/, 'save must not repaint a different ticket after completion');

const replySource = source.slice(replyStart, source.indexOf('ensureSupportStyles();ensureQueueControls();ensureTicketControls();', replyStart));
assert.match(replySource, /const ticket=currentTicket,ticketId=ticket\.id/, 'reply must snapshot the selected ticket before the first await');
assert.match(replySource, /ticket_id:ticketId/, 'reply insert and email payload must use the snapshotted ticket id');
assert.match(replySource, /\.eq\('id',ticketId\)/, 'reply status transition must use the snapshotted ticket id');
assert.doesNotMatch(replySource, /ticket_id:currentTicket\.id/, 'reply must never redirect to a later currentTicket selection');
assert.match(replySource, /if\(isCurrent\(\)\)q\('ticketReply'\)\.value=''/, 'an older reply must not clear a newly selected ticket reply field');
assert.match(replySource, /if\(isCurrent\(\)\)await openTicket\(ticketId\)/, 'an older reply must not repaint a newly selected ticket detail');

console.log('admin support action ticket binding regression: PASS');
