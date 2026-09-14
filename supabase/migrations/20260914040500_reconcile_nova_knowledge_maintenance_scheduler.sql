-- Reconcile the emergency Nova maintenance scheduler repair with the canonical
-- repository-owned scheduler identity. This migration is intentionally limited to
-- Nova knowledge maintenance orchestration and leaves all unrelated cron jobs alone.

do $block$
declare
  existing_job record;
begin
  for existing_job in
    select jobid
    from cron.job
    where jobname in (
      'nova-knowledge-maintenance',
      'nova-knowledge-maintenance-every-5-minutes'
    )
  loop
    perform cron.unschedule(existing_job.jobid);
  end loop;
end
$block$;

select cron.schedule(
  'nova-knowledge-maintenance-every-5-minutes',
  '*/5 * * * *',
  $cron$
    select net.http_post(
      url := 'https://ghdhairijqjqivqriigi.supabase.co/functions/v1/nova-knowledge-maintenance',
      headers := jsonb_build_object(
        'Content-Type', 'application/json',
        'x-maintenance-secret', (
          select decrypted_secret
          from vault.decrypted_secrets
          where name = 'morley_backup_scheduler_secret'
          limit 1
        )
      ),
      body := '{"action":"run","embedding_limit":4,"ingest_limit":4}'::jsonb,
      timeout_milliseconds := 50000
    ) as request_id;
  $cron$
)
where exists (
  select 1
  from vault.secrets
  where name = 'morley_backup_scheduler_secret'
);
