const fs = require('fs');
const test = require('node:test');
const assert = require('node:assert/strict');

test('main Morley website does not load embedded admin mode while dedicated admin remains', () => {
  const index = fs.readFileSync('index.html', 'utf8');
  const admin = fs.readFileSync('admin/index.html', 'utf8');

  assert.equal(index.includes('web-admin-mode.js'), false);
  assert.equal(fs.existsSync('web-admin-mode.js'), false);
  assert.equal(admin.includes('B&L Morley Admin'), true);
  assert.equal(fs.existsSync('android/adminapp'), true);
});
