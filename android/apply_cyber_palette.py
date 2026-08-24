from pathlib import Path

root = Path(__file__).resolve().parent / 'app' / 'src' / 'main' / 'java' / 'com' / 'buysloans' / 'hub'
replacements = {
    '0xFFFFD400': '0xFF2F7CFF',
    '0xFFC99A27': '0xFF2F7CFF',
    '0xFFDDB347': '0xFF12C9FF',
    '0xFF111111': '0xFF030712',
    '0xFF222222': '0xFF07172C',
    '0xFF101010': '0xFF041024',
    '0xFF1B1B1B': '0xFF081A31',
    '0xFF171717': '0xFF061327',
    '0xFF303030': '0xFF0A1B33',
    '0xFF2B2B2B': '0xFF0B1C35',
    '0xFF252525': '0xFF09182F',
    '0xFF1E1E1E': '0xFF061326',
    '0xFF57E389': '0xFF25D991',
    '0xFF57D68D': '0xFF25D991',
    'android.graphics.Color.rgb(17,17,17)': 'android.graphics.Color.rgb(3,7,18)',
    'window.statusBarColor=android.graphics.Color.BLACK': 'window.statusBarColor=android.graphics.Color.rgb(3,7,18)',
    'window.navigationBarColor=android.graphics.Color.BLACK': 'window.navigationBarColor=android.graphics.Color.rgb(3,7,18)',
    'window.statusBarColor = android.graphics.Color.BLACK': 'window.statusBarColor = android.graphics.Color.rgb(3,7,18)',
    'window.navigationBarColor = android.graphics.Color.BLACK': 'window.navigationBarColor = android.graphics.Color.rgb(3,7,18)',
}

for path in root.glob('*.kt'):
    text = path.read_text(encoding='utf-8')
    updated = text
    for old, new in replacements.items():
        updated = updated.replace(old, new)
    if updated != text:
        path.write_text(updated, encoding='utf-8')
        print(f'Applied blue/cyan cyber palette: {path.name}')
