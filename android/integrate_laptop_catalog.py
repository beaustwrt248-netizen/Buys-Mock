from pathlib import Path

main = Path(__file__).resolve().parent / 'app' / 'src' / 'main' / 'java' / 'com' / 'buysloans' / 'hub' / 'MainActivity.kt'
text = main.read_text(encoding='utf-8')

replacement = r'''private suspend fun market(q:String,useCatalog:Boolean=false):MarketResult=coroutineScope{val clean=q.trim();val resolution=if(useCatalog)runCatching{LaptopModelCatalog.resolve(AppRuntime.context,clean)}.getOrElse{LaptopCatalogResolution(clean,clean)} else LaptopCatalogResolution(clean,clean);val canonical=if(useCatalog)LaptopModelCatalog.preferredQuery(clean,resolution) else clean;val queries=(broadenQueries(canonical)+broadenQueries(clean)).distinct().filter{it.isNotBlank()}.take(5);val roots=queries.map{query->async{query to request(query)}}.awaitAll();val eg=mutableListOf<Listing>();val ee=mutableListOf<Listing>();val sg=mutableListOf<Listing>();val se=mutableListOf<Listing>();val rej=mutableListOf<Listing>();for((query,root) in roots){val g=parse(root.optJSONObject("google"),canonical);val e=parse(root.optJSONObject("ebay"),canonical);eg+=g.filter{it.tier==MatchTier.EXACT};ee+=e.filter{it.tier==MatchTier.EXACT};sg+=g.filter{it.tier==MatchTier.SIMILAR};se+=e.filter{it.tier==MatchTier.SIMILAR};rej+=(g+e).filter{it.tier==MatchTier.REJECTED}};val f=features(canonical);val comps=listOf(async{componentValue("CPU",f.cpu)},async{componentValue("GPU",f.gpu)},async{componentValue("RAM",f.ram)},async{componentValue("STORAGE",f.storage)}).awaitAll();MarketResult(dedupe(eg),dedupe(ee),dedupe(sg),dedupe(se),dedupe(rej),comps,queries)}'''

lines = text.splitlines()
market_indices = [i for i, line in enumerate(lines) if line.startswith('private suspend fun market(q:String):MarketResult=') or line.startswith('private suspend fun market(q:String,useCatalog:Boolean=false):MarketResult=')]
if len(market_indices) != 1:
    raise SystemExit(f'Expected exactly one market function, found {len(market_indices)}')
lines[market_indices[0]] = replacement

laptop_indices = [i for i, line in enumerate(lines) if line.startswith('@Composable fun Laptop()=')]
desktop_indices = [i for i, line in enumerate(lines) if line.startswith('@Composable fun Desktop()=')]
if len(laptop_indices) != 1 or len(desktop_indices) != 1:
    raise SystemExit(f'Expected one Laptop and one Desktop composable, found laptop={len(laptop_indices)} desktop={len(desktop_indices)}')

# Only the Laptop screen opts into catalogue resolution. Desktop continues to use
# the raw/OEM pricing path so a fuzzy laptop catalogue hit cannot rewrite a PC query.
lines[laptop_indices[0]] = lines[laptop_indices[0]].replace('runCatching{market(q)}', 'runCatching{market(q,true)}')
lines[desktop_indices[0]] = lines[desktop_indices[0]].replace('runCatching{market(q,true)}', 'runCatching{market(q)}')

main.write_text('\n'.join(lines) + '\n', encoding='utf-8')
print('Integrated laptop-only Supabase catalogue resolution into live market search')
