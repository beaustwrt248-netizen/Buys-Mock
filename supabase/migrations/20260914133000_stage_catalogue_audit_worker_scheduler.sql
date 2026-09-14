-- Staged activation for the verified Nova catalogue-audit consumer.
-- This intentionally schedules only the existing bounded worker. It does not re-enable
-- catalogue enqueue, mutate catalogue/pricing facts, or change worker authorization.
-- The job remains Vault-backed and runs at a conservative hourly cadence with batch 10.

do $block$
declare
  existing_job record;
begin
  for existing_job in
    select jobid
    from cron.job
    where jobname = 'nova-catalog-audit-worker-hourly'
  loop
    perform cron.unschedule(existing_job.jobid);
  end loop;
end
$block$;

select cron.schedule(
  'nova-catalog-audit-worker-hourly',
  '37 * * * *',
  $cron$
    select net.http_post(
      url := 'https://ghdhairijqjqivqriigi.supabase.co/functions/v1/nova-catalog-audit',
      headers := jsonb_build_object(
        'Content-Type', 'application/json',
        'x-maintenance-secret', (
          select decrypted_secret
          from vault.decrypted_secrets
          where name = 'morley_backup_scheduler_secret'
          order by created_at desc
          limit 1
        )
      ),
      body := '{"limit":10}'::jsonb,
      timeout_milliseconds := 120000
    ) as request_id;
  $cron$
)
where exists (
  select 1
  from vault.secrets
  where name = 'morley_backup_scheduler_secret'
);
