import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

// Regression coverage for the Samsung browser/Admin WebView input failure.
const login = fs.readFileSync(new URL('../admin/login-security.js', import.meta.url), 'utf8');
const index = fs.readFileSync(new URL('../admin/index.html', import.meta.url), 'utf8');

test('Admin login keeps email and password editable on mobile browsers and WebView', () => {
  assert.match(login, /input\.readOnly=false/);
  assert.match(login, /input\.disabled=false/);
  assert.match(login, /beforeinput/);
  assert.match(login, /inputType!=='insertText'/);
  assert.match(login, /input\.dispatchEvent\(new Event\('input'/);
  assert.match(login, /-webkit-text-fill-color:#1d2b26/);
});

test('Admin loads the cache-busted mobile input repair', () => {
  assert.match(index, /login-security\.js\?v=3/);
});
