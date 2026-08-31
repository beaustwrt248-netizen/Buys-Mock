from pathlib import Path

main = Path(__file__).resolve().parent / 'app' / 'src' / 'main' / 'java' / 'com' / 'buysloans' / 'hub' / 'MainActivity.kt'
text = main.read_text(encoding='utf-8')
updated = text

# UI-only cleanup: keep the protected valuation rules unchanged while removing
# the second oversized low-confidence verdict card from the evidence stack.
updated = updated.replace(
    'Block(if(lowReference)"MARKET DATA INSUFFICIENT" else "EXACT MARKET VALUE UNAVAILABLE",if(lowReference)',
    'Block(if(lowReference)"MARKET CONFIDENCE • LOW" else "EXACT MARKET VALUE UNAVAILABLE",if(lowReference)'
)
updated = updated.replace(
    '}else Verdict("INSUFFICIENT MARKET DATA")',
    '}else {}'
)

if updated == text:
    if 'MARKET CONFIDENCE • LOW' in text and 'Verdict("INSUFFICIENT MARKET DATA")' not in text:
        print('Compact market-confidence UI already applied')
    else:
        raise SystemExit('Expected protected valuation UI pattern was not found')
else:
    main.write_text(updated, encoding='utf-8')
    print('Compacted low-confidence valuation UI without changing pricing logic')
