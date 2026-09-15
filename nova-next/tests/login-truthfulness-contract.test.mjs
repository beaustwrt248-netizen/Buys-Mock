import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const index = await readFile(new URL('../index.html', import.meta.url), 'utf8');
const runtime = await readFile(new URL('../src/live-runtime.mjs', import.meta.url), 'utf8');

for (const unsupported of ['Forgot password?', 'Continue with Google', 'Continue with Apple', 'Sign up']) {
  assert.equal(index.includes(unsupported), false, `login must not advertise unsupported control: ${unsupported}`);
}

assert.equal(index.includes('Enabled Morley Admin accounts only'), true, 'login must state the supported account boundary in static markup');
assert.equal(runtime.includes("documentObj.querySelector('.social-row')?.classList.add('is-hidden')"), false, 'runtime must not need to hide unsupported social auth markup');
assert.equal(runtime.includes("documentObj.querySelector('.signup-copy')?.classList.add('is-hidden')"), false, 'runtime must not need to hide unsupported signup markup');

console.log('login-truthfulness-contract: ok');
