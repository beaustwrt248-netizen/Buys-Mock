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
        'colors = ButtonDefaults.buttonColors(containerColor = MFStrong)': 'colors = ButtonDefaults.buttonColors(containerColor = MorleyAccentStrong, contentColor = Color.White)',
    })

# FVP-005: keep every GP grade label readable in both interaction states.
# Selected uses white on emerald; unselected uses dark text on the white card.
replace(root / 'MainActivity.kt', {
    'contentColor=if(grade==g)Color(0xFF06251B) else Color.White':
        'contentColor=if(grade==g)Color.White else MorleyTextPrimary',
})

# FVP-006: Smart Workspace primary actions use the same strong emerald surface
# as the rest of Morley, so keep their labels white and preserve canonical
# Seller Ask wording in Quick Deal Mode. No deal calculation logic is changed.
replace(root / 'SmartWorkspaceSection.kt', {
    'colors = ButtonDefaults.buttonColors(containerColor = SWStrong, contentColor = MorleyTextPrimary)':
        'colors = ButtonDefaults.buttonColors(containerColor = SWStrong, contentColor = androidx.compose.ui.graphics.Color.White)',
    'label = { Text("Seller asking price") }': 'label = { Text("Seller Ask") }',
})

# FVP-007: Test & Buy checklist chips sit on very light status tints. Use the
# canonical dark text for both selected and unselected states so labels remain
# readable without changing green/red/yellow status mapping or test behaviour.
replace(root / 'TestBuyActivity.kt', {
    'labelColor = Color.White.copy(alpha = .86f),': 'labelColor = Color(0xFF1C2B26),',
    'selectedLabelColor = Color.White': 'selectedLabelColor = Color(0xFF1C2B26)',
})

# FVP-008: MenuFeature primary actions are migrated onto the strong emerald
# surface. Keep their labels white rather than reintroducing dark-on-emerald
# contrast. Inventory and scanner behaviour remain unchanged.

# Installed update notices must never continue presenting themselves as available
# updates. Preserve non-update history and future update notices.
store = root / 'NotificationInboxStore.kt'
text = store.read_text(encoding='utf-8')
old = '''        }.getOrDefault(emptyList())\n    }\n\n    fun unreadCount(context: Context): Int = items(context).count { !it.read }'''
new = '''        }.getOrDefault(emptyList()).filterNot { item ->\n            item.type.equals("update", ignoreCase = true) &&\n                item.versionCode > 0 &&\n                item.versionCode <= BuildConfig.VERSION_CODE\n        }\n    }\n\n    fun unreadCount(context: Context): Int = items(context).count { !it.read }'''
if old in text:
    store.write_text(text.replace(old, new), encoding='utf-8')

print('Applied video-review UI, GP contrast, Smart Workspace, Test & Buy, MenuFeature contrast and stale-update corrections')
