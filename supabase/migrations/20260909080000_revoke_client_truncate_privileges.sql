-- Client roles must never be able to bypass RLS through TRUNCATE.
-- PostgreSQL row-level security does not protect TRUNCATE, so remove this
-- table-level privilege from both browser/client roles across public tables.
do $$
declare
  r record;
begin
  for r in
    select schemaname, tablename
    from pg_tables
    where schemaname = 'public'
  loop
    execute format(
      'revoke truncate on table %I.%I from anon, authenticated',
      r.schemaname,
      r.tablename
    );
  end loop;
end
$$;
