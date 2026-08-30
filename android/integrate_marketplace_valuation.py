from pathlib import Path

main = Path(__file__).resolve().parent / 'app' / 'src' / 'main' / 'java' / 'com' / 'buysloans' / 'hub' / 'MainActivity.kt'
text = main.read_text(encoding='utf-8')

if 'IntegratedMarketValueEngine.calculateLive' in text:
    print('Protected marketplace valuation already integrated')
    raise SystemExit(0)

old = '''@Composable fun Valuation(r:MarketResult,ask:String,gp:Double,estimateRate:Double){val exactNew=median(r.exactGoogle.map{it.price});val exactUsed=median(r.exactEbay.map{it.price});val exactEvidence=r.exactGoogle.isNotEmpty()||r.exactEbay.isNotEmpty();val used=when{exactUsed>0->exactUsed;exactNew>0->exactNew*estimateRate;else->0.0};val componentEstimate=r.components.sumOf{it.value};val primary=if(exactEvidence)used else componentEstimate;val max=if(primary>0)primary*(1-gp) else 0.0;val a=ask.toDoubleOrNull()?:0.0;if(exactEvidence&&used>0){Metrics(listOf("NEW RETAIL" to exactNew,"USED VALUE" to used,"MAX BUY" to max,"AVERAGE RESULT" to used));Verdict(if(a<=0)"PRICE READY" else if(a<=max)"BUY" else if(a<=max*1.1)"NEGOTIATE" else "PASS")}else{Block("EXACT MARKET VALUE UNAVAILABLE","No verified exact-brand/model listing was found. Broader searches may provide similar evidence, but they cannot drive the primary value. A conservative component fallback is used only when CPU/GPU/RAM/storage evidence can be independently validated.");if(componentEstimate>0){Metrics(listOf("COMPONENT ESTIMATE" to componentEstimate,"COMPONENT MAX BUY" to max));ComponentBreakdown(r.components);Verdict(if(a<=0)"LOW CONFIDENCE" else if(a<=max)"BUY — LOW CONFIDENCE" else if(a<=max*1.1)"NEGOTIATE — LOW CONFIDENCE" else "PASS — LOW CONFIDENCE")}else Verdict("LOW CONFIDENCE")};Confidence(r);MultiSourceEvidence(r,estimateRate);if(r.searches.size>1)Block("SEARCH COVERAGE",r.searches.joinToString("\\n") { "• $it" });Evidence(r)}'''

new = '''@Composable fun Valuation(r:MarketResult,ask:String,gp:Double,estimateRate:Double){
val context=LocalContext.current
var integrated by remember(r,estimateRate){mutableStateOf<IntegratedMarketValue?>(null)}
var marketplaceLoading by remember(r){mutableStateOf(true)}
LaunchedEffect(r,estimateRate){
 marketplaceLoading=true
 integrated=runCatching{IntegratedMarketValueEngine.calculateLive(context,r.searches.firstOrNull().orEmpty(),r.exactEbay.map{it.price},r.exactGoogle.map{it.price},estimateRate)}.getOrElse{IntegratedMarketValueEngine.calculate(r.exactEbay.map{it.price},r.exactGoogle.map{it.price},null,estimateRate)}
 marketplaceLoading=false
}
val exactNew=median(r.exactGoogle.map{it.price})
val exactUsed=median(r.exactEbay.map{it.price})
val baseUsed=when{exactUsed>0->exactUsed;exactNew>0->exactNew*estimateRate;else->0.0}
val protectedUsed=when{marketplaceLoading->0.0;integrated!=null->integrated!!.usedValue;else->baseUsed}
val marketplaceExact=integrated?.marketplaceEvidence?.let{e->e.gumtree.count{it.exact}+e.facebook.count{it.exact}}?:0
val exactEvidence=r.exactGoogle.isNotEmpty()||r.exactEbay.isNotEmpty()||marketplaceExact>0
val componentEstimate=r.components.sumOf{it.value}
val primary=if(exactEvidence&&protectedUsed>0)protectedUsed else componentEstimate
val max=if(primary>0)primary*(1-gp) else 0.0
val a=ask.toDoubleOrNull()?.takeIf{it>0.0}?:0.0
if(exactEvidence&&protectedUsed>0){
 Metrics(listOf("NEW RETAIL" to exactNew,"USED VALUE" to protectedUsed,"MAX BUY" to max,"SELLER ASK" to a))
 Verdict(if(a<=0)"MARKET VALUE READY" else if(a<=max)"BUY" else if(a<=max*1.1)"NEGOTIATE" else "PASS")
 if(a<=0)Block("DEAL CHECK PENDING","Enter the seller asking price to compare the deal against Morley's protected max-buy target.")
}else{
 val lowReference=integrated?.confidence=="LOW"&&integrated?.sources?.isNotEmpty()==true
 Block(if(lowReference)"MARKET DATA INSUFFICIENT" else "EXACT MARKET VALUE UNAVAILABLE",if(lowReference)"Exact reference evidence was found, but there is not enough independent evidence to set a protected used value or max buy." else "No sufficiently verified exact configuration evidence was found across the available sources. Similar or mismatched listings remain reference-only and cannot drive the primary value.")
 if(componentEstimate>0){Metrics(listOf("COMPONENT ESTIMATE" to componentEstimate,"COMPONENT MAX BUY" to max));ComponentBreakdown(r.components);Verdict(if(a<=0)"LOW CONFIDENCE" else if(a<=max)"BUY — LOW CONFIDENCE" else if(a<=max*1.1)"NEGOTIATE — LOW CONFIDENCE" else "PASS — LOW CONFIDENCE")}else Verdict("INSUFFICIENT MARKET DATA")
}
Confidence(r)
if(marketplaceLoading)Block("MARKET EVIDENCE","Checking Australian marketplace exact-configuration evidence…") else integrated?.let{IntegratedMarketEvidencePanel(it)}
MultiSourceEvidence(r,estimateRate)
if(r.searches.size>1)Block("SEARCH COVERAGE",r.searches.joinToString("\\n") { "• $it" })
Evidence(r)
}'''

if old in text:
    main.write_text(text.replace(old, new), encoding='utf-8')
    print('Integrated protected marketplace consensus into live valuation')
else:
    print('Valuation function has moved or been refactored; skipping marketplace migration safely')
