import assert from 'node:assert/strict';
import fs from 'node:fs';
import test from 'node:test';

const css = fs.readFileSync(new URL('../admin/admin-v2.css', import.meta.url), 'utf8');
const support = fs.readFileSync(new URL('../admin/support-tickets.js', import.meta.url), 'utf8');
const contract = fs.readFileSync(new URL('../admin/support-ticket-card-contract.txt', import.meta.url), 'utf8');

test('support queue remains a neutral bounded card layout across Admin v2', () => {
  assert.match(css, /#ticketsList \.ticket-row\{display:grid!important;grid-template-columns:minmax\(0,1fr\) auto!important/);
  assert.match(css, /#ticketsList \.ticket-row[^}]*max-width:100%!important/);
  assert.match(css, /#ticketsList \.ticket-row[^}]*overflow:hidden!important/);
  assert.match(css, /#ticketsList \.ticket-open-btn\{display:none!important\}/);
  assert.match(css, /@media\(max-width:900px\)[\s\S]*#ticketsList \.ticket-row\{grid-template-columns:minmax\(0,1fr\)!important/);
});

test('the card itself preserves keyboard activation and status remains independent', () => {
  assert.match(support, /role="button" tabindex="0"/);
  assert.match(support, /\['Enter',' '\]\.includes\(e\.key\)/);
  assert.match(support, /<span class="pill /);
  assert.match(contract, /neutral interactive cards/);
  assert.match(contract, /360px, 390px, and 412-430px/);
});
