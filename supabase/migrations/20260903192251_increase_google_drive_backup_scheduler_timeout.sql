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
      body := '{"action":"backup"}'::jsonb,
      timeout_milliseconds := 120000
    );
  $cron$
);
