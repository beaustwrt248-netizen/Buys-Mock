from pathlib import Path

root = Path(__file__).resolve().parent / 'app' / 'src' / 'main' / 'java' / 'com' / 'buysloans' / 'hub'
dashboard = root / 'DashboardActivity.kt'
main = root / 'MainActivity.kt'


def replace_any(text: str, candidates: tuple[str, ...], new: str, label: str) -> str:
    """Apply one deterministic migration while accepting already-polished source copy."""
    if new in text:
        return text
    for old in candidates:
        if old in text:
            return text.replace(old, new)
    raise SystemExit(f'Expected dashboard navigation pattern not found: {label}')


text = dashboard.read_text(encoding='utf-8')
text = replace_any(
    text,
    ('Page.Laptop->LaptopGuidedScreen()',),
    'Page.Laptop->ComputerPricingScreen()',
    'computer pricing destination',
)
text = replace_any(
    text,
    ('Page.Desktop->Desktop()',),
    'Page.Desktop->ConsolePricingScreen()',
    'console pricing destination',
)
text = replace_any(
    text,
    ('private fun ParityHome(onLaptop:()->Unit,onDesktop:()->Unit,onGp:()->Unit)',),
    'private fun ParityHome(onComputer:()->Unit,onConsole:()->Unit,onGp:()->Unit)',
    'home navigation callbacks',
)
text = replace_any(
    text,
    (
        'NavCard("💻","Laptops / MacBooks","Guided exact-model Google + eBay AU valuation",onLaptop)',
        'NavCard("💻","Laptops & MacBooks","Guided exact-model Google + eBay AU valuation",onLaptop)',
        'NavCard("💻","Computer Pricing","Choose Laptop / MacBook or Desktop / Gaming PC, then complete the matching valuation flow.",onComputer)',
    ),
    'NavCard("computer","Computer Pricing","Choose Laptop / MacBook or Desktop / Gaming PC, then complete the matching valuation flow.",onComputer)',
    'computer pricing card',
)
text = replace_any(
    text,
    (
        'NavCard("🖥","Desktops / Gaming PCs","Component-based live pricing",onDesktop)',
        'NavCard("🖥","Desktops & gaming PCs","Component-based live pricing",onDesktop)',
        'NavCard("🎮","Console Pricing","Dedicated console pricing workspace — pricing dataset coming next.",onConsole)',
    ),
    'NavCard("console","Console Pricing","Choose a supported console and condition grade for an automatic buy-price target.",onConsole)',
    'console pricing card',
)
dashboard.write_text(text, encoding='utf-8')

text = main.read_text(encoding='utf-8')
old_variants = (
    'enum class Page(val label:String,val icon:String){Home("Home","⌂"),Laptop("Laptop","💻"),Desktop("Desktop","🖥"),GP("GP","$"),More("More","⚙")}',
    'enum class Page(val label:String,val icon:String){Home("Home","⌂"),Laptop("Computer","💻"),Desktop("Console","🎮"),GP("GP","$"),More("More","⚙")}',
)
new = 'enum class Page(val label:String,val icon:String){Home("Home","home"),Laptop("Computer","computer"),Desktop("Console","console"),GP("GP","money"),More("More","menu")}'
if new not in text:
    for old in old_variants:
        if old in text:
            text = text.replace(old, new)
            break
    else:
        raise SystemExit('Expected Page enum not found')
main.write_text(text, encoding='utf-8')
print('Applied combined computer pricing navigation and console workspace')
