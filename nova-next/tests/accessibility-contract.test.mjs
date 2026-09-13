import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const app = readFileSync(new URL('../app.js', import.meta.url), 'utf8');

assert.match(app, /function setDrawer\(open, \{ restoreFocus = true \} = \{\}\)/,
  'drawer close must support deterministic focus restoration');
assert.match(app, /menuButton\.focus\(\{ preventScroll: true \}\)/,
  'closing the drawer must restore focus to the menu trigger');
assert.match(app, /if \(event\.key === 'Tab' && drawer\.classList\.contains\('is-open'\)\)/,
  'open drawer must contain Tab focus');
assert.match(app, /tabList\.setAttribute\('role', 'group'\)/,
  'filter controls must use button-group semantics rather than incomplete tab semantics');
assert.match(app, /candidate\.setAttribute\('aria-pressed', String\(selected\)\)/,
  'filter buttons must expose their selected state');

console.log('accessibility-contract: ok');
