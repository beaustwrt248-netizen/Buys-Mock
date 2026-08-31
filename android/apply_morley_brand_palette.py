from pathlib import Path

main = Path(__file__).resolve().parent / 'app' / 'src' / 'main' / 'java' / 'com' / 'buysloans' / 'hub' / 'MainActivity.kt'
text = main.read_text(encoding='utf-8')
# Presentation-only migration. Keep both legacy and previous blue tokens here so
# repeated preBuild execution converges on the current graphite/emerald palette.
replacements = {
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
    'containerColor=if(grade==g)Yellow else Card,contentColor=if(grade==g)Color.Black else Color.White': 'containerColor=if(grade==g)Yellow else Card,contentColor=if(grade==g)Color(0xFF06251B) else Color.White',
    'colors=ButtonDefaults.buttonColors(containerColor=Yellow,contentColor=Color.Black)': 'colors=ButtonDefaults.buttonColors(containerColor=Yellow,contentColor=Color(0xFF06251B))',
}
updated = text
for old, new in replacements.items():
    updated = updated.replace(old, new)
if updated != text:
    main.write_text(updated, encoding='utf-8')
    print('Applied Morley graphite/emerald palette to legacy valuation workspace')
else:
    print('Morley graphite/emerald palette already applied')
