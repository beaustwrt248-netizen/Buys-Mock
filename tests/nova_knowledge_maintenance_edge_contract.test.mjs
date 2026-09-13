import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const workerPath = new URL('../supabase/functions/nova-knowledge-maintenance/index.ts', import.meta.url);
const sharedIngestPath = new URL('../supabase/functions/_shared/nova_internal_ingestion.mjs', import.meta.url);
const adminIngestPath = new URL('../supabase/functions/nova-knowledge-ingest/index.ts', import.meta.url);

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

test('maintenance and admin ingesters share one privacy-preserving ingestion engine', () => {
  assert.equal(fs.existsSync(sharedIngestPath), true, 'shared ingestion engine must exist');
  const shared = fs.readFileSync(sharedIngestPath, 'utf8');
  const worker = source();
  const adminIngest = fs.readFileSync(adminIngestPath, 'utf8');

  assert.match(worker, /nova_internal_ingestion\.mjs/);
  assert.match(adminIngest, /nova_internal_ingestion\.mjs/);
  assert.match(shared, /MAX_INGESTION_BATCH\s*=\s*50/);
  assert.match(shared, /support_tickets/);
  assert.match(shared, /"id,category,status,priority,app_version,app_version_code,device_model,android_version,created_at,updated_at,resolved_at,closed_at"/);
  assert.doesNotMatch(shared, /support_tickets[\s\S]{0,900}(description|diagnostics|user_id|assigned_to)/i);
});

test('hourly internal ingestion is allowlisted, bounded, checkpointed and independent from embedding', () => {
  const text = source();
  assert.match(text, /INGESTION_INTERVAL_MS\s*=\s*60\s*\*\s*60\s*\*\s*1000/);
  assert.match(text, /SAFE_INGESTION_ADAPTERS/);
  assert.match(text, /runInternalIngestion/);
  assert.match(text, /Math\.min\([^\n]*50/);
  assert.match(text, /latestIngestion/);
  assert.match(text, /ingestion_error/);
  assert.match(text, /embed_pending/);
});