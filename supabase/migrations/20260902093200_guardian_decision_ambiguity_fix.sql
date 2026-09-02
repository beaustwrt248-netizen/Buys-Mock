create or replace function public.guardian_decide_incident(incident_id uuid, decision text)
returns void
language plpgsql
security definer
set search_path to 'pg_catalog', 'public', 'private'
as $function$
declare
  target public.guardian_incidents%rowtype;
  repair_id uuid;
  v_incident_id uuid := incident_id;
  v_decision text := decision;
begin
  if not private.is_admin_or_manager() then
    raise exception 'Admin or Manager access required';
  end if;

  if v_decision not in ('approve','reject','retry') then
    raise exception 'Invalid Guardian decision';
  end if;

  select gi.* into target
  from public.guardian_incidents gi
  where gi.id = v_incident_id
  for update;

  if not found then
    raise exception 'Guardian incident not found';
  end if;

  if v_decision = 'approve' then
    if target.state not in ('proposed','awaiting_approval') then
      raise exception 'Incident is not awaiting approval';
    end if;

    insert into public.guardian_repairs(incident_id,requested_by,status,base_ref)
    values(v_incident_id,auth.uid(),'requested','main')
    on conflict on constraint guardian_repairs_incident_id_key do update
      set status = case when public.guardian_repairs.status in ('failed','cancelled','tests_failed') then 'requested' else public.guardian_repairs.status end,
          requested_by = auth.uid(),
          requested_at = case when public.guardian_repairs.status in ('failed','cancelled','tests_failed') then now() else public.guardian_repairs.requested_at end,
          last_error_code = case when public.guardian_repairs.status in ('failed','cancelled','tests_failed') then null else public.guardian_repairs.last_error_code end,
          updated_at = now()
    returning id into repair_id;

    update public.guardian_incidents gi
      set state='applying',approved_by=auth.uid(),approved_at=now(),last_error_code=null
      where gi.id=v_incident_id;

    insert into public.guardian_activity(incident_id,phase,status,summary,detail,visibility,progress,actor)
    values(v_incident_id,'applying','waiting','Repair approved. Guardian is preparing an isolated candidate patch.','No repository write occurs during candidate generation. Branch/test execution requires a second approval.','admin',68,'approval');
  elsif v_decision = 'reject' then
    update public.guardian_incidents gi
      set state='ignored',approved_by=auth.uid(),approved_at=now()
      where gi.id=v_incident_id;

    update public.guardian_repairs gr
      set status='cancelled',completed_at=now(),updated_at=now()
      where gr.incident_id=v_incident_id
        and gr.status not in ('merged','cancelled');
  else
    update public.guardian_incidents gi
      set state='queued',last_error_code=null,attempt_count=gi.attempt_count+1
      where gi.id=v_incident_id;
  end if;
end;
$function$;

revoke all on function public.guardian_decide_incident(uuid,text) from public, anon;
grant execute on function public.guardian_decide_incident(uuid,text) to authenticated;
