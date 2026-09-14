import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const app = readFileSync('nova-next/app.js', 'utf8');

test('password visibility toggle exposes the current action and pressed state', () => {
  assert.match(app, /querySelector\(['"]\[data-action=[^\]]*toggle-password[^\]]*\]['"]\)[\s\S]*?setAttribute\(['"]aria-pressed['"],\s*['"]false['"]\)/,
    'password visibility toggle must expose its initial unpressed state when Nova Next boots');
  assert.match(app, /toggle-password[\s\S]*?setAttribute\(['"]aria-label['"],[\s\S]*?Hide password[\s\S]*?Show password/,
    'password visibility toggle must change its accessible label between Show password and Hide password');
  assert.match(app, /toggle-password[\s\S]*?setAttribute\(['"]aria-pressed['"],/,
    'password visibility toggle must expose its pressed state');
});

test('route changes move focus to the active page heading for announcement', () => {
  assert.match(app, /function\s+focusRouteHeading\s*\(/,
    'route rendering needs a dedicated focus helper');
  assert.match(app, /focusRouteHeading\(currentRoute\)/,
    'renderRoute must focus the newly active route heading');
  assert.match(app, /heading\.setAttribute\(['"]tabindex['"],\s*['"]-1['"]\)/,
    'route headings must become programmatically focusable');
  assert.match(app, /heading\.focus\(\{\s*preventScroll:\s*true\s*\}\)/,
    'route changes must focus the heading without unexpected scrolling');
});
