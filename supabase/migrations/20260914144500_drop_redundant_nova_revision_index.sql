-- Approved narrow #2040 performance cleanup.
--
-- The UNIQUE index below is the authoritative identity/integrity index for
-- (knowledge_id, revision). PostgreSQL B-tree indexes can be scanned backward,
-- so it also supports ORDER BY revision DESC for a fixed knowledge_id.
-- This migration removes only the redundant non-unique sort twin.
--
-- Rollback / recreate SQL:
-- create index nova_knowledge_revisions_item_idx
--   on public.nova_knowledge_revisions (knowledge_id, revision desc);

do $$
declare
  v_unique boolean;
  v_valid boolean;
begin
  select i.indisunique, i.indisvalid
    into v_unique, v_valid
  from pg_catalog.pg_index i
  join pg_catalog.pg_class c on c.oid = i.indexrelid
  join pg_catalog.pg_namespace n on n.oid = c.relnamespace
  where n.nspname = 'public'
    and c.relname = 'nova_knowledge_revisions_knowledge_id_revision_key';

  if v_unique is distinct from true or v_valid is distinct from true then
    raise exception 'required UNIQUE Nova revision identity index is missing or invalid';
  end if;
end
$$;

drop index if exists public.nova_knowledge_revisions_item_idx;
