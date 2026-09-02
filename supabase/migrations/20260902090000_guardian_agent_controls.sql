-- Guardian agent capability controls. These govern diagnosis/research capabilities only.
-- They do not grant merge, deploy, auth/RLS, secret access, or destructive database rights.

alter table public.guardian_settings
  add column if not exists ai_enabled boolean not null default true,
  add column if not exists repository_read_enabled boolean not null default true,
  add column if not exists external_research_enabled boolean not null default false,
  add column if not exists agent_model text not null default 'gpt-5.6-terra';

alter table public.guardian_settings drop constraint if exists guardian_settings_agent_model_check;
alter table public.guardian_settings add constraint guardian_settings_agent_model_check
  check (agent_model in ('gpt-5.6-luna','gpt-5.6-terra','gpt-5.6-sol'));

create or replace function public.guardian_set_agent_controls(
  p_ai_enabled boolean,
  p_repository_read_enabled boolean,
  p_external_research_enabled boolean,
  p_agent_model text
)
returns public.guardian_settings
language plpgsql
security definer
set search_path = pg_catalog, public, private
as $$
declare
  v_role text;
  v_result public.guardian_settings;
begin
  select role into v_role
  from public.profiles
  where id = auth.uid() and is_enabled = true;

  if v_role not in ('admin','manager') then
    raise exception 'Guardian agent controls require Admin or Manager access';
  end if;

  if p_agent_model not in ('gpt-5.6-luna','gpt-5.6-terra','gpt-5.6-sol') then
    raise exception 'Unsupported Guardian model';
  end if;

  update public.guardian_settings
  set ai_enabled = p_ai_enabled,
      repository_read_enabled = p_repository_read_enabled,
      external_research_enabled = p_external_research_enabled,
      agent_model = p_agent_model,
      require_human_for_code = true,
      updated_by = auth.uid(),
      updated_at = now()
  where singleton = true
  returning * into v_result;

  insert into public.admin_audit_log(actor_user_id,action,target_type,target_id,details)
  values(
    auth.uid(),'guardian_agent_controls_updated','guardian_settings','singleton',
    jsonb_build_object(
      'ai_enabled',v_result.ai_enabled,
      'repository_read_enabled',v_result.repository_read_enabled,
      'external_research_enabled',v_result.external_research_enabled,
      'agent_model',v_result.agent_model,
      'require_human_for_code',true
    )
  );

  return v_result;
end;
$$;

revoke all on function public.guardian_set_agent_controls(boolean,boolean,boolean,text) from public;
grant execute on function public.guardian_set_agent_controls(boolean,boolean,boolean,text) to authenticated;
