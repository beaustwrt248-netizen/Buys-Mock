from pathlib import Path

root = Path(__file__).resolve().parent / 'app' / 'src' / 'main' / 'java' / 'com' / 'buysloans' / 'hub'


def replace(path: Path, replacements: dict[str, str]) -> None:
    text = path.read_text(encoding='utf-8')
    updated = text
    for old, new in replacements.items():
        updated = updated.replace(old, new)
    if updated != text:
        path.write_text(updated, encoding='utf-8')

# The video review exposed two screens that still carried the pre-2.15.8 blue
# palette. Keep this migration presentation-only and deterministic.
for name in ('MenuFeatureActivity.kt', 'NotificationCentreActivity.kt'):
    replace(root / name, {
        'private val MFAccent = Color(0xFF16C7FF)': 'private val MFAccent = MorleyAccent',
        'private val MFStrong = Color(0xFF2684FF)': 'private val MFStrong = MorleyAccentStrong',
        'private val MFBg = Color(0xFF030712)': 'private val MFBg = MorleyBackground',
        'private val MFCard = Color(0xFF0B1528)': 'private val MFCard = MorleySurfaceRaised',
        'private val MFMuted = Color(0xFF8EA6C4)': 'private val MFMuted = MorleyTextSecondary',
        'private val NCAccent = Color(0xFF16C7FF)': 'private val NCAccent = MorleyAccent',
        'private val NCBg = Color(0xFF030712)': 'private val NCBg = MorleyBackground',
        'private val NCCard = Color(0xFF0B1528)': 'private val NCCard = MorleySurfaceRaised',
        'private val NCMuted = Color(0xFF8EA6C4)': 'private val NCMuted = MorleyTextSecondary',
        'android.graphics.Color.rgb(3, 7, 18)': 'android.graphics.Color.rgb(8, 11, 13)',
        'containerColor = Color(0xFF050B16)': 'containerColor = MorleySurfaceSoft',
        'containerColor = if (item.read) NCCard else Color(0xFF0E2038)': 'containerColor = if (item.read) NCCard else MorleyAccentSoft',
        'if (item.body.isNotBlank()) Text(item.body, color = Color.White)': 'if (item.body.isNotBlank()) Text(item.body, color = MorleyTextPrimary)',
        'colors = ButtonDefaults.buttonColors(containerColor = MFStrong)': 'colors = ButtonDefaults.buttonColors(containerColor = MorleyAccentStrong, contentColor = Color.White)',
    })

replace(root / 'MainActivity.kt', {
    'contentColor=if(grade==g)Color(0xFF06251B) else Color.White':
        'contentColor=if(grade==g)Color.White else MorleyTextPrimary',
})

# Keep Smart Workspace actions readable, canonicalise Seller Ask copy, and give
# the Quick Deal verdict card enough bottom breathing room on compact phones.
replace(root / 'SmartWorkspaceSection.kt', {
    'colors = ButtonDefaults.buttonColors(containerColor = SWStrong, contentColor = MorleyTextPrimary)':
        'colors = ButtonDefaults.buttonColors(containerColor = SWStrong, contentColor = androidx.compose.ui.graphics.Color.White)',
    'label = { Text("Seller asking price") }': 'label = { Text("Seller Ask") }',
    'Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {\n                        Text(verdict.first':
        'Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {\n                        Text(verdict.first',
    'if (headroom != null) Text(if (headroom >= 0) "${swMoney(headroom)} below max buy" else "${swMoney(-headroom)} above max buy", color = if (headroom >= 0) SWGood else SWWarn, fontSize = 11.sp)':
        'if (headroom != null) Text(if (headroom >= 0) "${swMoney(headroom)} below max buy" else "${swMoney(-headroom)} above max buy", color = if (headroom >= 0) SWGood else SWWarn, fontSize = 11.sp, modifier = Modifier.padding(bottom = 2.dp))',
})

replace(root / 'TestBuyActivity.kt', {
    'labelColor = Color.White.copy(alpha = .86f),': 'labelColor = Color(0xFF1C2B26),',
    'selectedLabelColor = Color.White': 'selectedLabelColor = Color(0xFF1C2B26)',
})

history = root / 'ValuationHistoryActivity.kt'
replace(history, {
    'import androidx.compose.foundation.BorderStroke\n': 'import androidx.compose.foundation.BorderStroke\nimport androidx.compose.foundation.background\n',
    'MaterialTheme(colorScheme=lightColorScheme(primary=HistAccent,secondary=HistStrong,background=HistBg,surface=HistCard)){': 'MaterialTheme(colorScheme=lightColorScheme(primary=HistAccent,secondary=HistStrong,background=HistBg,surface=Color.White,onBackground=Color(0xFF17332C),onSurface=Color(0xFF17332C))){',
    'Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){': 'Column(Modifier.fillMaxSize().background(HistBg).verticalScroll(rememberScrollState()).padding(horizontal=18.dp,vertical=20.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){',
    'Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)){listOf("all","watch","quoted","bought","sold","passed").forEach{s->FilterChip(selected=filter==s,onClick={filter=s},label={Text(s.replaceFirstChar{it.uppercase()},fontSize=9.sp)})}}': 'Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("all","watch","quoted","bought","sold","passed").forEach{s->FilterChip(selected=filter==s,onClick={filter=s},modifier=Modifier.weight(1f),label={Text(s.replaceFirstChar{it.uppercase()},fontSize=9.sp,maxLines=1,softWrap=false)},colors=FilterChipDefaults.filterChipColors(containerColor=Color.White,labelColor=Color(0xFF46564F),selectedContainerColor=Color(0xFFDDF4E9),selectedLabelColor=HistStrong),border=FilterChipDefaults.filterChipBorder(enabled=true,selected=filter==s,borderColor=Color(0xFFD6E1DC),selectedBorderColor=HistAccent,borderWidth=1.dp,selectedBorderWidth=1.dp))}}',
    'if(!loading&&shown.isEmpty())Card(colors=CardDefaults.cardColors(containerColor=HistCard),border=BorderStroke(1.dp,HistAccent.copy(alpha=.18f)),shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth()){Text(if(search.isBlank()&&filter=="all")"No saved valuations yet." else "No valuations match this view.",Modifier.padding(18.dp),color=HistMuted)}': 'if(!loading&&shown.isEmpty())Card(colors=CardDefaults.cardColors(containerColor=Color.White),border=BorderStroke(1.dp,Color(0xFFDCE6E1)),shape=RoundedCornerShape(24.dp),modifier=Modifier.fillMaxWidth()){Column(Modifier.fillMaxWidth().padding(horizontal=24.dp,vertical=34.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){Text(if(search.isBlank()&&filter=="all")"No saved valuations yet." else "No valuations match this view.",fontSize=18.sp,fontWeight=FontWeight.Black,color=Color(0xFF17332C));Text(if(search.isBlank()&&filter=="all")"Your saved valuations and deals will appear here." else "Try a different status or search term.",color=HistMuted);if(search.isBlank()&&filter=="all")Text("Get started by saving your first valuation.",color=HistAccent,fontWeight=FontWeight.Bold)}}',
    'label={Text("Seller asking price")}': 'label={Text("Seller Ask")}',
})

# Stale update filtering is now authoritative checked-in Kotlin. Keep the
# build-time migration from silently restoring or depending on that behaviour.
store = root / 'NotificationInboxStore.kt'
store_text = store.read_text(encoding='utf-8')
if 'item.versionCode <= BuildConfig.VERSION_CODE' not in store_text:
    raise SystemExit('NotificationInboxStore.kt is missing authoritative stale-update filtering')

print('Applied video-review UI, GP contrast, Smart Workspace Quick Deal spacing, Test & Buy and Valuation History corrections; verified checked-in stale-update filtering')
