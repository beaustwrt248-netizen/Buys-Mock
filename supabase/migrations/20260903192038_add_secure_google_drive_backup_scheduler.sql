do $$
begin
  if not exists (select 1 from vault.decrypted_secrets where name = 'morley_backup_scheduler_secret') then
    perform vault.create_secret(
      encode(gen_random_bytes(48), 'hex'),
      'morley_backup_scheduler_secret',
      'Internal scheduler credential for the Morley Google Drive backup Edge Function',
      null
    );
  end if;
end
$$;

create or replace function public.morley_backup_scheduler_secret_matches(candidate text)
returns boolean
language sql
security definer
set search_path = pg_catalog, vault
as $$
  select coalesce(
    candidate <> '' and exists (
      select 1
      from vault.decrypted_secrets
      where name = 'morley_backup_scheduler_secret'
        and decrypted_secret = candidate
    ),
    false
  );
$$;

revoke all on function public.morley_backup_scheduler_secret_matches(text) from public;
grant execute on function public.morley_backup_scheduler_secret_matches(text) to service_role;

do $$
declare
  existing_job bigint;
begin
  select jobid into existing_job from cron.job where jobname = 'morley-google-drive-backup-daily' limit 1;
  if existing_job is not null then
    perform cron.unschedule(existing_job);
  end if;
end
$$;

select cron.schedule(
  'morley-google-drive-backup-daily',
  '0 19 * * *',
  $cron$
    select net.http_post(
      url := 'https://ghdhairijqjqivqriigi.supabase.co/functions/v1/google-drive-backup',
      headers := jsonb_build_object(
        'Content-Type', 'application/json',
        'x-morley-backup-secret', (
          select decrypted_secret
          from vault.decrypted_secrets
          where name = 'morley_backup_scheduler_secret'
          order by created_at desc
          limit 1
        )
      ),
      body := '{"action":"backup"}'::jsonb
    );
  $cron$
);
