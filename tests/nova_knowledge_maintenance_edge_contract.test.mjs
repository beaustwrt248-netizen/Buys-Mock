import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const workerPath = new URL('../supabase/functions/nova-knowledge-maintenance/index.ts', import.meta.url);

function source() {
  assert.equal(fs.existsSync(workerPath), true, 'maintenance worker must exist');
  return fs.readFileSync(workerPath, 'utf8');
}

test('maintenance worker is internal-token protected and bounded', () => {
  const text = source();
  assert.match(text, /NOVA_KNOWLEDGE_MAINTENANCE_TOKEN/);
  assert.match(text, /x-nova-maintenance-token/i);
  assert.match(text, /nova_claim_embedding_chunks/);
  assert.match(text, /Math\.min\([^\n]*20/);
  assert.doesNotMatch(text, /admin\.auth\.getUser/);
  assert.doesNotMatch(text, /profiles.*role/s);
});

test('maintenance worker uses gte-small and validates 384 dimensions', () => {
  const text = source();
  assert.match(text, /Supabase\.ai\.Session\(['"]gte-small['"]\)/);
  assert.match(text, /vector\.length\s*!==\s*384/);
  assert.match(text, /embedding_status:\s*['"]ready['"]/);
  assert.match(text, /embedding_provider:\s*['"]supabase-ai['"]/);
  assert.match(text, /embedding_model:\s*['"]gte-small['"]/);
});

test('maintenance worker never logs chunk content or vectors', () => {
  const text = source();
  assert.doesNotMatch(text, /console\.(log|warn|error)\([^\n]*(chunk\.content|vector)/);
  assert.doesNotMatch(text, /JSON\.stringify\([^\n]*(chunk\.content|embedding)/);
});

test('embedding failures remain searchable and retry with bounded metadata', () => {
  const text = source();
  assert.match(text, /embedding_status:\s*['"]error['"]/);
  assert.match(text, /embedding_next_retry_at/);
  assert.match(text, /embedding_last_error_code/);
  assert.doesNotMatch(text, /content:\s*null/);
  assert.doesNotMatch(text, /status:\s*['"]archived['"]/);
});