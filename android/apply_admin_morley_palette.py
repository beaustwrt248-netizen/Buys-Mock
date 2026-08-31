from pathlib import Path

root = Path(__file__).resolve().parent / 'adminapp' / 'src' / 'main' / 'java' / 'com' / 'buysloans' / 'admin'

palette = {
    '0xFF030712': '0xFF080B0D',
    '0xFF050B16': '0xFF0C1214',
    '0xFF07101F': '0xFF101619',
    '0xFF07172C': '0xFF101619',
    '0xFF0B1528': '0xFF151D20',
    '0xFF0A1B33': '0xFF151D20',
    '0xFF16C7FF': '0xFF38D6A3',
    '0xFF2684FF': '0xFF1FB887',
    '0xFF2F7CFF': '0xFF38D6A3',
    '0xFF12C9FF': '0xFF77E9C4',
    '0xFFFFD400': '0xFF38D6A3',
    '0xFFC99A27': '0xFF38D6A3',
    '0xFFDDB347': '0xFF38D6A3',
    '0xFF8EA6C4': '0xFFB2C0BC',
    '0xFF57E389': '0xFF63E6A6',
    '0xFFFFC857': '0xFFF5C76B',
    '0xFFFF6B7A': '0xFFFF7B86',
    '0xFFF4F7FB': '0xFFF4F7F6',
}

changed = []
for path in root.glob('*.kt'):
    text = path.read_text(encoding='utf-8')
    updated = text
    for old, new in palette.items():
        updated = updated.replace(old, new)
    if updated != text:
        path.write_text(updated, encoding='utf-8')
        changed.append(path.name)

print('Admin palette migrated: ' + ', '.join(changed) if changed else 'Admin graphite/emerald palette already applied')
