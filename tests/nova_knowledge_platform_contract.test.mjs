import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const migrationUrl = new URL('../supabase/migrations/20260913001000_nova_knowledge_platform_expansion.sql', import.meta.url);

async function sql() {
  return (await readFile(migrationUrl, 'utf8')).toLowerCase();
}

test('Nova knowledge expansion is additive and service-role only', async () => {
  const text = await sql();
  for (const table of ['nova_knowledge_sources', 'nova_knowledge_chunks', 'nova_knowledge_evidence', 'nova_knowledge_ingestion_runs']) {
    assert.match(text, new RegExp(`create table if not exists public\\.${table}`));
    assert.match(text, new RegExp(`alter table public\\.${table} enable row level security`));
  }
  assert.doesNotMatch(text, /\b(drop table|truncate|delete from public\.nova_knowledge_items)\b/);
  assert.match(text, /revoke all on table public\.nova_knowledge_chunks from anon, authenticated/);
  assert.match(text, /grant all on table public\.nova_knowledge_chunks to service_role/);
});

test('hybrid search RPC cannot bypass RLS or become a public privileged endpoint', async () => {
  const text = await sql();
  assert.match(text, /embedding extensions\.vector\(384\)/);
  assert.match(text, /security invoker/);
  assert.doesNotMatch(text, /security definer/);
  assert.match(text, /revoke all on function public\.nova_search_knowledge_chunks/);
  assert.match(text, /grant execute on function public\.nova_search_knowledge_chunks[\s\S]+to service_role/);
});

test('knowledge health telemetry reports coverage, failures, staleness and ingestion without public execute', async () => {
  const text = await sql();
  assert.match(text, /function public\.nova_knowledge_health\(\)/);
  assert.match(text, /embedding_ready/);
  assert.match(text, /embedding_error/);
  assert.match(text, /stale_sources/);
  assert.match(text, /latest_ingestion_at/);
  assert.match(text, /revoke all on function public\.nova_knowledge_health\(\) from public, anon, authenticated/);
  assert.match(text, /grant execute on function public\.nova_knowledge_health\(\) to service_role/);
  assert.doesNotMatch(text, /security definer/);
});
