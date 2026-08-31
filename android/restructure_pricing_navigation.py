from pathlib import Path

root = Path(__file__).resolve().parent / 'app' / 'src' / 'main' / 'java' / 'com' / 'buysloans' / 'hub'
dashboard = root / 'DashboardActivity.kt'
main = root / 'MainActivity.kt'

text = dashboard.read_text(encoding='utf-8')
replacements = {
    'Page.Laptop->LaptopGuidedScreen()': 'Page.Laptop->ComputerPricingScreen()',
    'Page.Desktop->Desktop()': 'Page.Desktop->ConsolePricingScreen()',
    'private fun ParityHome(onLaptop:()->Unit,onDesktop:()->Unit,onGp:()->Unit)': 'private fun ParityHome(onComputer:()->Unit,onConsole:()->Unit,onGp:()->Unit)',
    'NavCard("💻","Laptops / MacBooks","Guided exact-model Google + eBay AU valuation",onLaptop)': 'NavCard("💻","Computer Pricing","Choose Laptop / MacBook or Desktop / Gaming PC, then complete the matching valuation flow.",onComputer)',
    'NavCard("🖥","Desktops / Gaming PCs","Component-based live pricing",onDesktop)': 'NavCard("🎮","Console Pricing","Dedicated console pricing workspace — pricing dataset coming next.",onConsole)',
}
for old, new in replacements.items():
    if old not in text and new not in text:
        raise SystemExit(f'Expected dashboard navigation pattern not found: {old}')
    text = text.replace(old, new)
dashboard.write_text(text, encoding='utf-8')

text = main.read_text(encoding='utf-8')
old = 'enum class Page(val label:String,val icon:String){Home("Home","⌂"),Laptop("Laptop","💻"),Desktop("Desktop","🖥"),GP("GP","$"),More("More","⚙")}'
new = 'enum class Page(val label:String,val icon:String){Home("Home","⌂"),Laptop("Computer","💻"),Desktop("Console","🎮"),GP("GP","$"),More("More","⚙")}'
if old not in text and new not in text:
    raise SystemExit('Expected Page enum not found')
main.write_text(text.replace(old, new), encoding='utf-8')
print('Applied combined computer pricing navigation and console workspace')
