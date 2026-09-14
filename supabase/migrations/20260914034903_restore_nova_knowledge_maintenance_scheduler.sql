-- Production emergency restore applied on 2026-09-14 after live verification showed
-- the Nova knowledge queue was healthy but no recurring maintenance trigger existed.
-- The follow-up reconciliation migration replaces this temporary scheduler identity
-- with the canonical repository-owned name.

do $block$
declare
  existing_job record;
begin
  for existing_job in
    select jobid from cron.job where jobname = 'nova-knowledge-maintenance'
  loop
    perform cron.unschedule(existing_job.jobid);
  end loop;
end
$block$;

select cron.schedule(
  'nova-knowledge-maintenance',
  '*/5 * * * *',
  $cron$
    select net.http_post(
      url := 'https://ghdhairijqjqivqriigi.supabase.co/functions/v1/nova-knowledge-maintenance',
      headers := jsonb_build_object(
        'Content-Type','application/json',
        'x-maintenance-secret',(
          select decrypted_secret
          from vault.decrypted_secrets
          where name='morley_backup_scheduler_secret'
          limit 1
        )
      ),
      body := jsonb_build_object('action','run','embedding_limit',4,'ingest_limit',4),
      timeout_milliseconds := 60000
    ) as request_id;
  $cron$
)
where exists (
  select 1 from vault.secrets where name = 'morley_backup_scheduler_secret'
);
