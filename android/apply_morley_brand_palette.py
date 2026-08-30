from pathlib import Path

main = Path(__file__).resolve().parent / 'app' / 'src' / 'main' / 'java' / 'com' / 'buysloans' / 'hub' / 'MainActivity.kt'
text = main.read_text(encoding='utf-8')
replacements = {
    '0xFFFFD400': '0xFF2F7CFF',
    '0xFF111111': '0xFF030712',
    '0xFF222222': '0xFF07172C',
    '0xFF101010': '0xFF041024',
    '0xFF1B1B1B': '0xFF081A31',
    '0xFF171717': '0xFF061327',
    '0xFF57E389': '0xFF25D991',
    'android.graphics.Color.rgb(17,17,17)': 'android.graphics.Color.rgb(3,7,18)',
    'containerColor=if(grade==g)Yellow else Card,contentColor=if(grade==g)Color.Black else Color.White': 'containerColor=if(grade==g)Yellow else Card,contentColor=Color.White',
    'colors=ButtonDefaults.buttonColors(containerColor=Yellow,contentColor=Color.Black)': 'colors=ButtonDefaults.buttonColors(containerColor=Yellow,contentColor=Color.White)',
}
updated = text
for old, new in replacements.items():
    updated = updated.replace(old, new)
if updated != text:
    main.write_text(updated, encoding='utf-8')
    print('Applied Morley crowned-blue palette to legacy valuation workspace')
else:
    print('Morley crowned-blue palette already applied')
