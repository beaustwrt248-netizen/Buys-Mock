-- Restore the canonical full-system Google Drive backup scheduler after live production
-- verification showed the OAuth credential and backup function are healthy but the cron
-- job itself is absent. Keep the scheduler credential in Vault and fail closed when the
-- expected secret is unavailable.

do $block$
declare
  existing_job record;
begin
  for existing_job in
    select jobid
    from cron.job
    where jobname = 'morley-google-drive-backup-daily'
  loop
    perform cron.unschedule(existing_job.jobid);
  end loop;
end
$block$;

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
      body := '{"action":"backup"}'::jsonb,
      timeout_milliseconds := 120000
    ) as request_id;
  $cron$
)
where exists (
  select 1
  from vault.secrets
  where name = 'morley_backup_scheduler_secret'
);
