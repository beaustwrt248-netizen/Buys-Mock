alter table public.valuation_history add column if not exists item_grade text;

do $$ begin
  if not exists (
    select 1 from pg_constraint where conname = 'valuation_history_item_grade_check'
  ) then
    alter table public.valuation_history
      add constraint valuation_history_item_grade_check
      check (item_grade is null or item_grade in ('A','B','C'));
  end if;
end $$;

-- Microsoft Surface devices are laptop-class / detachable computers for valuation.
insert into public.laptop_models (brand,family,model_name,model_number,year_from,year_to,aliases,cpu_platform,screen_size,notes,active)
select * from (values
('Microsoft','Surface Pro','Surface Pro 4','1724',2015,2017,array['surface pro4','surface pro 4','model 1724']::text[],'Intel','12.3','Detachable 2-in-1',true),
('Microsoft','Surface Pro','Surface Pro (5th Gen)','1796',2017,2018,array['surface pro 5','surface pro 2017','surface pro 5th gen','model 1796','1807']::text[],'Intel','12.3','Detachable 2-in-1',true),
('Microsoft','Surface Pro','Surface Pro 6','1796',2018,2019,array['surface pro6','surface pro 6','model 1796']::text[],'Intel','12.3','Detachable 2-in-1; regulatory ID shared with 5th Gen',true),
('Microsoft','Surface Pro','Surface Pro 7','1866',2019,2020,array['surface pro7','surface pro 7','model 1866']::text[],'Intel','12.3','Detachable 2-in-1',true),
('Microsoft','Surface Pro','Surface Pro 7+','1960',2021,2021,array['surface pro 7 plus','surface pro7+','model 1960','1961']::text[],'Intel','12.3','Detachable 2-in-1',true),
('Microsoft','Surface Pro','Surface Pro 8','1983',2021,2022,array['surface pro8','surface pro 8','model 1983','1982']::text[],'Intel','13','Detachable 2-in-1',true),
('Microsoft','Surface Pro','Surface Pro 9','2038',2022,2024,array['surface pro9','surface pro 9','model 2038','1996','1997']::text[],'Intel / SQ3','13','Detachable 2-in-1',true),
('Microsoft','Surface Go','Surface Go','1824',2018,2020,array['surface go 1','surface go1','model 1824','1825']::text[],'Intel','10','Detachable 2-in-1',true),
('Microsoft','Surface Go','Surface Go 2','1901',2020,2021,array['surface go2','surface go 2','model 1901','1926','1927']::text[],'Intel','10.5','Detachable 2-in-1',true),
('Microsoft','Surface Go','Surface Go 3','2022',2021,2023,array['surface go3','surface go 3','model 2022','1901','1926']::text[],'Intel','10.5','Detachable 2-in-1; aliases include shared regulatory IDs',true),
('Microsoft','Surface Go','Surface Go 4','2067',2023,2026,array['surface go4','surface go 4','model 2067']::text[],'Intel','10.5','Detachable 2-in-1',true),
('Microsoft','Surface Book','Surface Book','1703',2015,2017,array['surface book 1','surface book1','model 1703','1704','1705','1785','1803']::text[],'Intel','13.5','Detachable laptop',true),
('Microsoft','Surface Book','Surface Book 2','1832',2017,2020,array['surface book2','surface book 2','model 1832','1793','1806','1834','1835','1792','1813']::text[],'Intel','13.5 / 15','Detachable laptop',true),
('Microsoft','Surface Book','Surface Book 3','1900',2020,2023,array['surface book3','surface book 3','model 1900','1899','1908','1909','1907']::text[],'Intel','13.5 / 15','Detachable laptop',true),
('Microsoft','Surface Laptop','Surface Laptop','1782',2017,2018,array['surface laptop 1','surface laptop1','model 1782']::text[],'Intel','13.5',null,true),
('Microsoft','Surface Laptop','Surface Laptop 2','1769',2018,2019,array['surface laptop2','surface laptop 2','model 1769']::text[],'Intel','13.5',null,true),
('Microsoft','Surface Laptop','Surface Laptop 3 13.5-inch','1867',2019,2021,array['surface laptop3','surface laptop 3','surface laptop 3 13.5','model 1867']::text[],'Intel','13.5',null,true),
('Microsoft','Surface Laptop','Surface Laptop 3 15-inch','1873',2019,2021,array['surface laptop 3 15','model 1873']::text[],'Intel / AMD','15',null,true),
('Microsoft','Surface Laptop','Surface Laptop 4 13.5-inch','1950',2021,2022,array['surface laptop4','surface laptop 4','surface laptop 4 13.5','model 1950']::text[],'Intel / AMD','13.5',null,true),
('Microsoft','Surface Laptop','Surface Laptop 4 15-inch','1953',2021,2022,array['surface laptop 4 15','model 1953']::text[],'Intel / AMD','15',null,true),
('Microsoft','Surface Laptop','Surface Laptop 5 13.5-inch','1979',2022,2024,array['surface laptop5','surface laptop 5','surface laptop 5 13.5','model 1979']::text[],'Intel','13.5',null,true),
('Microsoft','Surface Laptop','Surface Laptop 5 15-inch','1979',2022,2024,array['surface laptop 5 15','model 1979']::text[],'Intel','15','Regulatory ID may be shared by configuration',true),
('Microsoft','Surface Laptop Studio','Surface Laptop Studio','1964',2021,2023,array['surface laptop studio 1','surface studio laptop','model 1964']::text[],'Intel','14.4',null,true),
('Microsoft','Surface Laptop Studio','Surface Laptop Studio 2','2029',2023,2026,array['surface laptop studio2','surface laptop studio 2','model 2029']::text[],'Intel','14.4',null,true),
('Microsoft','Surface Laptop Go','Surface Laptop Go','1943',2020,2022,array['surface laptop go 1','surface laptop go1','model 1943']::text[],'Intel','12.4',null,true),
('Microsoft','Surface Laptop Go','Surface Laptop Go 2','2013',2022,2023,array['surface laptop go2','surface laptop go 2','model 2013']::text[],'Intel','12.4',null,true),
('Microsoft','Surface Laptop Go','Surface Laptop Go 3','2013',2023,2026,array['surface laptop go3','surface laptop go 3','model 2013']::text[],'Intel','12.4','Regulatory ID shared with prior generation',true)
) as v(brand,family,model_name,model_number,year_from,year_to,aliases,cpu_platform,screen_size,notes,active)
where not exists (
  select 1 from public.laptop_models lm
  where lower(lm.model_name)=lower(v.model_name)
    and lower(coalesce(lm.model_number,''))=lower(coalesce(v.model_number,''))
);
