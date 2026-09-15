import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const html = await readFile(new URL('../index.html', import.meta.url), 'utf8');
const app = await readFile(new URL('../app.js', import.meta.url), 'utf8');

test('Nova shell does not hard-code a signed-in person before session hydration', () => {
  assert.doesNotMatch(html, /Beau John/);
  assert.doesNotMatch(html, /beau@example\.com/);
  assert.doesNotMatch(html, /Good morning, Beau/);
  assert.doesNotMatch(html, /Hi Beau!/);
  assert.match(html, /id="novaNextProfileName"/);
  assert.match(html, /id="novaNextProfileEmail"/);
  assert.match(html, /id="novaNextWelcomeName"/);
  assert.match(html, /id="novaNextChatWelcomeName"/);
});

test('authenticated session hydrates profile, welcome name, chat greeting and avatars from validated user data', () => {
  for (const token of [
    "user_metadata?.full_name",
    "user_metadata?.name",
    "novaNextProfileName",
    "novaNextProfileEmail",
    "novaNextWelcomeName",
    "novaNextChatWelcomeName",
    "novaNextProfileAvatar",
    "novaNextTopbarAvatar"
  ]) assert.ok(app.includes(token), token);
});
