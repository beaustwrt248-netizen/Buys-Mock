from pathlib import Path

main = Path(__file__).resolve().parent / 'app' / 'src' / 'main' / 'java' / 'com' / 'buysloans' / 'hub' / 'MainActivity.kt'
text = main.read_text(encoding='utf-8')

replacement = r'''private suspend fun market(q:String):MarketResult=coroutineScope{val clean=q.trim();val resolution=runCatching{LaptopModelCatalog.resolve(AppRuntime.context,clean)}.getOrElse{LaptopCatalogResolution(clean,clean)};val canonical=LaptopModelCatalog.preferredQuery(clean,resolution);val queries=(broadenQueries(canonical)+broadenQueries(clean)).distinct().filter{it.isNotBlank()}.take(5);val roots=queries.map{query->async{query to request(query)}}.awaitAll();val eg=mutableListOf<Listing>();val ee=mutableListOf<Listing>();val sg=mutableListOf<Listing>();val se=mutableListOf<Listing>();val rej=mutableListOf<Listing>();for((query,root) in roots){val g=parse(root.optJSONObject("google"),canonical);val e=parse(root.optJSONObject("ebay"),canonical);eg+=g.filter{it.tier==MatchTier.EXACT};ee+=e.filter{it.tier==MatchTier.EXACT};sg+=g.filter{it.tier==MatchTier.SIMILAR};se+=e.filter{it.tier==MatchTier.SIMILAR};rej+=(g+e).filter{it.tier==MatchTier.REJECTED}};val f=features(canonical);val comps=listOf(async{componentValue("CPU",f.cpu)},async{componentValue("GPU",f.gpu)},async{componentValue("RAM",f.ram)},async{componentValue("STORAGE",f.storage)}).awaitAll();MarketResult(dedupe(eg),dedupe(ee),dedupe(sg),dedupe(se),dedupe(rej),comps,queries)}'''

lines = text.splitlines()
indices = [i for i, line in enumerate(lines) if line.startswith('private suspend fun market(q:String):MarketResult=')]
if len(indices) != 1:
    raise SystemExit(f'Expected exactly one market function, found {len(indices)}')
lines[indices[0]] = replacement
main.write_text('\n'.join(lines) + '\n', encoding='utf-8')
print('Integrated Supabase laptop catalogue resolution into live market search')
