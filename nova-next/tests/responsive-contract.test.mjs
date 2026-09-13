import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const styles = await readFile(new URL('../styles.css', import.meta.url), 'utf8');
const settings = await readFile(new URL('../src/settings-ui.mjs', import.meta.url), 'utf8');
const app = await readFile(new URL('../app.js', import.meta.url), 'utf8');

test('shell uses dynamic viewport and safe-area sizing', () => {
  assert.match(styles, /100dvh/);
  assert.match(styles, /safe-area-inset-bottom/);
  assert.match(styles, /safe-area-inset-top/);
});

test('interactive shell has visible keyboard focus and reduced motion support', () => {
  assert.match(styles, /:focus-visible/);
  assert.match(settings, /prefers-reduced-motion/);
  assert.match(settings, /animation:none/);
});

test('runtime status announcements are polite', () => {
  assert.match(app, /aria-live/);
  assert.match(app, /role', 'status/);
});

test('settings dialogs restore focus and close on Escape', () => {
  assert.match(settings, /returnFocus/);
  assert.match(settings, /event\.key === 'Escape'/);
  assert.match(settings, /aria-modal/);
});
