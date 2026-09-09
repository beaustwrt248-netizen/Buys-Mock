-- Advance user sync revisions atomically so concurrent clients cannot both
-- successfully write the same next revision.
create or replace function public.user_sync_compare_and_set(
  p_user_id uuid,
  p_expected_revision bigint,
  p_state jsonb,
  p_installation_id text default null
)
returns table (
  revision bigint,
  state jsonb,
  updated_by_installation text,
  updated_at timestamptz,
  conflict boolean
)
language plpgsql
security definer
set search_path = 'public', 'pg_temp'
as $$
declare
  v_revision bigint;
  v_state jsonb;
  v_installation text;
  v_updated_at timestamptz;
begin
  if p_user_id is null or p_expected_revision < 0 or p_state is null then
    raise exception 'SYNC_CAS_INVALID_INPUT';
  end if;

  update public.user_sync_state
  set revision = p_expected_revision + 1,
      state = p_state,
      updated_by_installation = nullif(left(coalesce(p_installation_id, ''), 200), ''),
      updated_at = now()
  where user_id = p_user_id
    and revision = p_expected_revision
  returning user_sync_state.revision,
            user_sync_state.state,
            user_sync_state.updated_by_installation,
            user_sync_state.updated_at
    into v_revision, v_state, v_installation, v_updated_at;

  if found then
    return query select v_revision, v_state, v_installation, v_updated_at, false;
    return;
  end if;

  if p_expected_revision = 0 then
    insert into public.user_sync_state (
      user_id,
      revision,
      state,
      updated_by_installation,
      updated_at
    ) values (
      p_user_id,
      1,
      p_state,
      nullif(left(coalesce(p_installation_id, ''), 200), ''),
      now()
    )
    on conflict (user_id) do nothing
    returning user_sync_state.revision,
              user_sync_state.state,
              user_sync_state.updated_by_installation,
              user_sync_state.updated_at
      into v_revision, v_state, v_installation, v_updated_at;

    if found then
      return query select v_revision, v_state, v_installation, v_updated_at, false;
      return;
    end if;
  end if;

  select s.revision, s.state, s.updated_by_installation, s.updated_at
    into v_revision, v_state, v_installation, v_updated_at
  from public.user_sync_state s
  where s.user_id = p_user_id;

  if not found then
    return query
      select 0::bigint,
             '{"bm_inv":[],"bm_sales":[],"bm_recent":[]}'::jsonb,
             null::text,
             null::timestamptz,
             true;
    return;
  end if;

  return query select v_revision, v_state, v_installation, v_updated_at, true;
end;
$$;

revoke all on function public.user_sync_compare_and_set(uuid, bigint, jsonb, text) from public, anon, authenticated;
grant execute on function public.user_sync_compare_and_set(uuid, bigint, jsonb, text) to service_role;
