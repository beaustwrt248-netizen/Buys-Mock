from pathlib import Path

root = Path(__file__).resolve().parent / 'app' / 'src' / 'main' / 'java' / 'com' / 'buysloans' / 'hub'


def apply(path: Path, replacements: dict[str, str]) -> bool:
    text = path.read_text(encoding='utf-8')
    updated = text
    for old, new in replacements.items():
        updated = updated.replace(old, new)
    if updated != text:
        path.write_text(updated, encoding='utf-8')
        return True
    return False


# Presentation-only migration. Keep legacy tokens so repeated preBuild execution
# converges on the current light, welcoming Morley visual contract.
main_changed = apply(root / 'MainActivity.kt', {
    '0xFFFFD400': '0xFF38D6A3',
    '0xFF2F7CFF': '0xFF38D6A3',
    '0xFF111111': '0xFF080B0D',
    '0xFF030712': '0xFF080B0D',
    '0xFF222222': '0xFF101619',
    '0xFF07172C': '0xFF101619',
    '0xFF101010': '0xFF0C1214',
    '0xFF041024': '0xFF0C1214',
    '0xFF1B1B1B': '0xFF151D20',
    '0xFF081A31': '0xFF151D20',
    '0xFF171717': '0xFF101619',
    '0xFF061327': '0xFF101619',
    '0xFF57E389': '0xFF63E6A6',
    '0xFF25D991': '0xFF63E6A6',
    'android.graphics.Color.rgb(17,17,17)': 'android.graphics.Color.rgb(8,11,13)',
    'android.graphics.Color.rgb(3,7,18)': 'android.graphics.Color.rgb(8,11,13)',
    'MaterialTheme(colorScheme=darkColorScheme(primary=Yellow,background=Bg,surface=Card))': 'MaterialTheme(colorScheme=MorleyColorScheme)',
    'Text("B&L Morley",fontSize=28.sp,fontWeight=FontWeight.Black);Text(title,fontSize=24.sp,fontWeight=FontWeight.Bold,color=Yellow)': 'Text(title,fontSize=26.sp,fontWeight=FontWeight.Black,color=MorleyTextPrimary)',
    'containerColor=if(grade==g)Yellow else Card,contentColor=if(grade==g)Color.Black else Color.White': 'containerColor=if(grade==g)Yellow else Card,contentColor=if(grade==g)Color(0xFF06251B) else Color.White',
    'colors=ButtonDefaults.buttonColors(containerColor=Yellow,contentColor=Color.Black)': 'colors=ButtonDefaults.buttonColors(containerColor=Yellow,contentColor=Color(0xFF06251B))',
})

nav_icon = 'icon={MorleyIcon(when(p){Page.Home->MorleyIcons.Home;Page.Laptop->MorleyIcons.Computer;Page.Desktop->MorleyIcons.Console;Page.GP->MorleyIcons.Money;Page.More->MorleyIcons.Menu},p.label,if(page==p) MorleyAccent else MorleyTextMuted)}'

dashboard_changed = apply(root / 'DashboardActivity.kt', {
    'private val DashAccent = Color(0xFF16C7FF)': 'private val DashAccent = MorleyAccent',
    'private val DashAccentStrong = Color(0xFF2684FF)': 'private val DashAccentStrong = MorleyAccentStrong',
    'private val DashBg = Color(0xFF030712)': 'private val DashBg = MorleyBackground',
    'private val DashCard = Color(0xFF0B1528)': 'private val DashCard = MorleySurfaceRaised',
    'private val DashMuted = Color(0xFF8EA6C4)': 'private val DashMuted = MorleyTextSecondary',
    'android.graphics.Color.rgb(3,7,18)': 'android.graphics.Color.rgb(8,11,13)',
    'MaterialTheme(colorScheme = darkColorScheme(primary = DashAccent, secondary = DashAccentStrong, background = DashBg, surface = DashCard))': 'MaterialTheme(colorScheme = MorleyColorScheme)',
    'containerColor=Color(0xFF050B16).copy(alpha=.96f)': 'containerColor=MorleySurfaceSoft.copy(alpha=.98f)',
    'Text(if(showMenu)"×" else "☰",fontSize=28.sp,color=DashAccent,fontWeight=FontWeight.Black)': 'if(showMenu) Text("×",fontSize=28.sp,color=MorleyTextPrimary,fontWeight=FontWeight.Black) else MorleyIcon(MorleyIcons.Menu,"Menu",MorleyAccent)',
    'Surface(color=DashAccent.copy(alpha=.08f),border=BorderStroke(1.dp,DashAccent.copy(alpha=.35f)),shape=RoundedCornerShape(999.dp))': 'Surface(color=MorleyAccentSoft.copy(alpha=.75f),border=BorderStroke(1.dp,MorleyBorder),shape=RoundedCornerShape(999.dp))',
    'if(!showMenu) NavigationBar(containerColor=Color(0xFF07101F).copy(alpha=.97f)){': 'if(!showMenu) NavigationBar(containerColor=MorleySurfaceSoft.copy(alpha=.98f),modifier=Modifier.height(72.dp)){',
    'icon={Text(p.icon,fontSize=20.sp)}': nav_icon,
    'icon={Text(p.icon,fontSize=18.sp)}': nav_icon,
    'indicatorColor=DashAccent.copy(alpha=.18f),selectedIconColor=DashAccent,selectedTextColor=DashAccent,unselectedTextColor=DashMuted': 'indicatorColor=MorleyAccentSoft,selectedIconColor=MorleyAccent,selectedTextColor=MorleyAccent,unselectedIconColor=MorleyTextMuted,unselectedTextColor=MorleyTextMuted',
    'NavCard("💰","General buys & GP","A / B / C / Luxury buying targets",onGp)': 'NavCard("money","General buys & GP","A / B / C / Luxury buying targets",onGp)',
    'Text(title,fontSize=27.sp,fontWeight=FontWeight.Black);Text(body,color=Color.LightGray,lineHeight=22.sp)': 'Text(title,color=MorleyTextPrimary,fontSize=27.sp,fontWeight=FontWeight.Black);Text(body,color=MorleyTextSecondary,lineHeight=22.sp)',
    'Text(label,color=Color.Gray,fontSize=10.sp,fontWeight=FontWeight.Bold)': 'Text(label,color=MorleyTextMuted,fontSize=10.sp,fontWeight=FontWeight.Bold)',
    'Text(value,color=Color(0xFF57E389),fontSize=18.sp,fontWeight=FontWeight.Black)': 'Text(value,color=MorleySuccess,fontSize=18.sp,fontWeight=FontWeight.Black)',
    'Text(icon,fontSize=24.sp);Text(title,fontSize=19.sp,fontWeight=FontWeight.Black);Text(subtitle,color=Color.LightGray,fontSize=13.sp)': 'MorleyIcon(when(icon){"computer"->MorleyIcons.Computer;"console"->MorleyIcons.Console;"money"->MorleyIcons.Money;else->MorleyIcons.Menu},title,MorleyAccent);Text(title,color=MorleyTextPrimary,fontSize=19.sp,fontWeight=FontWeight.Black);Text(subtitle,color=MorleyTextSecondary,fontSize=13.sp)',
})

smart_changed = apply(root / 'SmartWorkspaceSection.kt', {
    'private val SWAccent = Color(0xFF16C7FF)': 'private val SWAccent = MorleyAccent',
    'private val SWStrong = Color(0xFF2684FF)': 'private val SWStrong = MorleyAccentStrong',
    'private val SWCard = Color(0xFF0B1528)': 'private val SWCard = MorleySurfaceRaised',
    'private val SWMuted = Color(0xFF8EA6C4)': 'private val SWMuted = MorleyTextSecondary',
    'private val SWGood = Color(0xFF57E389)': 'private val SWGood = MorleySuccess',
    'private val SWWarn = Color(0xFFFFC857)': 'private val SWWarn = MorleyWarning',
    'private val SWBad = Color(0xFFFF6B7A)': 'private val SWBad = MorleyDanger',
    'Surface(color = Color(0xFF07101F)': 'Surface(color = MorleySurfaceSoft',
    'colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF168E61))': 'colors = ButtonDefaults.buttonColors(containerColor = MorleyAccentStrong, contentColor = MorleyTextPrimary)',
    ') { Text("⚡ Quick Deal Mode", fontWeight = FontWeight.Black) }': ') { Text("Quick Deal Mode", fontWeight = FontWeight.Black) }',
    ') { Text("⌁ NFC Scanner", color = SWAccent, fontWeight = FontWeight.Black) }': ') { Text("NFC Scanner", color = SWAccent, fontWeight = FontWeight.Black) }',
    'Text("Welcome, ${AuthManager.displayName(context).ifBlank { "back" }.substringBefore(\' \')}", fontSize = 24.sp, fontWeight = FontWeight.Black)': 'Text("Welcome, ${AuthManager.displayName(context).ifBlank { "back" }.substringBefore(\' \')}", color = MorleyTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Black)',
})

# Catch older one-off surfaces that pre-date the shared theme. This generic pass
# is intentionally presentation-only and keeps every Android screen inside the
# same light/emerald family without changing valuation, auth, NFC or data logic.
legacy_palette = {
    '0xFFFFD400': '0xFF38D6A3',
    '0xFFC99A27': '0xFF38D6A3',
    '0xFFDDB347': '0xFF38D6A3',
    '0xFF2F7CFF': '0xFF38D6A3',
    '0xFF12C9FF': '0xFF77E9C4',
    '0xFF16C7FF': '0xFF38D6A3',
    '0xFF2684FF': '0xFF1FB887',
    '0xFF030712': '0xFF080B0D',
    '0xFF07172C': '0xFF101619',
    '0xFF041024': '0xFF0C1214',
    '0xFF081A31': '0xFF151D20',
    '0xFF061327': '0xFF101619',
    '0xFF0A1B33': '0xFF151D20',
    '0xFF0B1528': '0xFF151D20',
    '0xFF8EA6C4': '0xFFB2C0BC',
    '0xFF57E389': '0xFF63E6A6',
    '0xFFFFC857': '0xFFF5C76B',
    '0xFFFF6B7A': '0xFFFF7B86',
}
legacy_changed = False
for kotlin_file in root.glob('*.kt'):
    legacy_changed = apply(kotlin_file, legacy_palette) or legacy_changed

light_palette = {
    'import androidx.compose.material3.darkColorScheme': 'import androidx.compose.material3.lightColorScheme',
    'darkColorScheme(': 'lightColorScheme(',
    '0xFF080B0D': '0xFFF5F7F4',
    '0xFF101619': '0xFFFFFFFF',
    '0xFF151D20': '0xFFEEF4F0',
    '0xFF0C1214': '0xFFE5EFEA',
    '0xFF173C32': '0xFFD8EFE5',
    '0xFF38D6A3': '0xFF167A5A',
    '0xFF1FB887': '0xFF0F684C',
    '0xFFF4F7F6': '0xFF1C2B26',
    '0xFFB2C0BC': '0xFF52645D',
    '0xFF81918C': '0xFF71827B',
    '0xFF2B4540': '0xFFCEDBD5',
    '0xFF63E6A6': '0xFF238A63',
    '0xFFF5C76B': '0xFFA86A12',
    '0xFFFF7B86': '0xFFC74755',
    'android.graphics.Color.rgb(8,11,13)': 'android.graphics.Color.rgb(245,247,244)',
    'Color.LightGray': 'MorleyTextSecondary',
    'Color.Gray': 'MorleyTextMuted',
    'Color(0xFFA7BAD3)': 'MorleyTextSecondary',
    'Color(0xFF8FA6C6)': 'MorleyTextMuted',
    'Color(0xFFE2E2E2)': 'MorleyTextPrimary',
}
light_changed = False
for kotlin_file in root.glob('*.kt'):
    light_changed = apply(kotlin_file, light_palette) or light_changed

changed = main_changed or dashboard_changed or smart_changed or legacy_changed or light_changed
print('Applied Morley light/emerald visual contract' if changed else 'Morley light/emerald visual contract already applied')
