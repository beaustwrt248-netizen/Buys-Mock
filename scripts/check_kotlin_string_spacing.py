from pathlib import Path

TARGET = Path('android/app/src/main/java/com/buysloans/hub/ValuationHistoryActivity.kt')
text = TARGET.read_text(encoding='utf-8')
forbidden = [')"']
hits = [token for token in forbidden if token in text]
if hits:
    raise SystemExit(f'Unsafe Kotlin string adjacency remains in {TARGET}: {hits}')
print('Kotlin string-expression spacing check passed')
