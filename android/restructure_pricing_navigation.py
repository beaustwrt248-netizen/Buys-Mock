from pathlib import Path

# This pre-build guard protects the intended primary pricing structure:
# Computer Pricing contains Laptop / MacBook and Desktop / Gaming PC,
# Console Pricing remains a separate primary category.
dashboard = Path(__file__).resolve().parent / 'app' / 'src' / 'main' / 'java' / 'com' / 'buysloans' / 'hub' / 'DashboardActivity.kt'
text = dashboard.read_text(encoding='utf-8')
required = (
    'Page.Laptop->ComputerPricingScreen()',
    'Page.Desktop->ConsolePricingScreen()',
    'private fun ParityHome(onComputer:()->Unit,onConsole:()->Unit,onGp:()->Unit)',
    'NavCard(MorleyIcons.Computer,"Computer Pricing","Laptop / MacBook or Desktop / Gaming PC",onComputer)',
    'NavCard(MorleyIcons.Console,"Console Pricing","PS4, PS5, Xbox and Nintendo grade pricing",onConsole)',
    'NavCard(MorleyIcons.Money,"General Buys / GP","A / B / C / Luxury buying targets",onGp)',
)
missing = [needle for needle in required if needle not in text]
if missing:
    raise SystemExit('Pricing navigation drifted; missing: ' + ' | '.join(missing))

print('Computer Pricing and Console Pricing navigation aligned')
