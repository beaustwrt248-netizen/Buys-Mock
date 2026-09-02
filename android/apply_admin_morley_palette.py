from pathlib import Path

root = Path(__file__).resolve().parent / 'adminapp' / 'src' / 'main' / 'java' / 'com' / 'buysloans' / 'admin'

palette = {
    '0xFF030712': '0xFFF1F6F3',
    '0xFF050B16': '0xFFF7FAF8',
    '0xFF07101F': '0xFFFFFFFF',
    '0xFF07172C': '0xFFFFFFFF',
    '0xFF0B1528': '0xFFFFFFFF',
    '0xFF0A1B33': '0xFFF7FAF8',
    '0xFF16C7FF': '0xFF0D8463',
    '0xFF2684FF': '0xFF0D8463',
    '0xFF2F7CFF': '0xFF0D8463',
    '0xFF12C9FF': '0xFF3DAE89',
    '0xFFFFD400': '0xFF0D8463',
    '0xFFC99A27': '0xFF0D8463',
    '0xFFDDB347': '0xFF0D8463',
    '0xFF8EA6C4': '0xFF6E7F77',
    '0xFF57E389': '0xFF17734F',
    '0xFFFFC857': '0xFF9A6A12',
    '0xFFFF6B7A': '0xFFB33A43',
    '0xFFF4F7FB': '0xFF1D2B26',
}

changed = []
for path in root.glob('*.kt'):
    text = path.read_text(encoding='utf-8')
    updated = text.replace('darkColorScheme(', 'lightColorScheme(')
    for old, new in palette.items():
        updated = updated.replace(old, new)
    if updated != text:
        path.write_text(updated, encoding='utf-8')
        changed.append(path.name)

print('Admin UI v2 palette migrated: ' + ', '.join(changed) if changed else 'Admin light/emerald palette already applied')
