'use strict';

const fs = require('fs');
const assert = require('assert');

const source = fs.readFileSync('web-laptop-fair-buy-zone.js', 'utf8');
const index = fs.readFileSync('index.html', 'utf8');

for (const needle of [
  'FAIR BUY ZONE • SHADOW MODE',
  'Laptop Buy Intelligence',
  'Read-only shadow guidance',
  'Existing valuation and Guardian/human approval remain authoritative.',
  'INTELLIGENCE MARKET VALUE',
  'QUICK-SALE VALUE',
  'CONFIDENCE',
  'OPENING',
  'TARGET',
  'COMPETITIVE',
  'HARD MAX',
  'exact · ${similar} similar · ${rejected} rejected',
  'Why listings were included',
  'Why listings were rejected',
  'No verified sold-history evidence is present in this shadow result. Active asking prices do not authorise an automatic buy.',
  "decision:'MANUAL REVIEW'"
]) {
  assert(source.includes(needle), `missing Fair Buy Zone parity contract: ${needle}`);
}

assert(index.includes('web-laptop-fair-buy-zone.js?v=1'), 'Fair Buy Zone parity asset must be loaded with a cache key');
assert(index.indexOf('workflow.js?v=6') < index.indexOf('web-laptop-fair-buy-zone.js?v=1'), 'Fair Buy Zone parity must load after the existing valuation workflow');

for (const forbidden of [
  "$('lapMax').value=",
  '$(' + "'lapMax'" + ').value=',
  'lapMax.value=',
  "$('lapOffer').value=",
  'lapOffer.value=',
  'morley-central-pricing',
  'supabase.from(',
  'insert(',
  'update(',
  'upsert(',
  'delete('
]) {
  assert(!source.includes(forbidden), `shadow panel must not cross authoritative write boundary: ${forbidden}`);
}

assert(source.includes("const anchor=$('decisionEngine')||sec.querySelector('.stats')"), 'Fair Buy Zone must remain additive to the existing Buying Decision');
assert(source.includes("source,sold:false"), 'web asking-price evidence must not be misrepresented as verified sold history');

console.log('Web laptop Fair Buy Zone parity contract verified');
