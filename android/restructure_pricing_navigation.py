from pathlib import Path

root = Path(__file__).resolve().parent / 'app' / 'src' / 'main' / 'java' / 'com' / 'buysloans' / 'hub'
dashboard = root / 'DashboardActivity.kt'
main = root / 'MainActivity.kt'

# Keep this pre-build migration idempotent and aligned with the current product
# contract: Laptop / MacBook and Desktop / Gaming PC are first-class pricing
# destinations. Console pricing must not be reintroduced into primary navigation.
text = dashboard.read_text(encoding='utf-8')
replacements = {
    'Page.Laptop->ComputerPricingScreen()': 'Page.Laptop->LaptopGuidedScreen()',
    'Page.Desktop->ConsolePricingScreen()': 'Page.Desktop->Desktop()',
    'private fun ParityHome(onComputer:()->Unit,onConsole:()->Unit,onGp:()->Unit)':
        'private fun ParityHome(onLaptop:()->Unit,onDesktop:()->Unit,onGp:()->Unit)',
    'NavCard("▱","Computer Pricing","Laptop / MacBook or Desktop / Gaming PC",onComputer)':
        'NavCard(MorleyIcons.Laptop,"Laptop / MacBook","Guided exact-model laptop and MacBook valuation",onLaptop)',
    'NavCard("◫","Console Pricing","PS4, PS5, Xbox and Nintendo grade pricing",onConsole)':
        'NavCard(MorleyIcons.Computer,"Desktop / Gaming PC","Desktop and gaming PC component-based valuation",onDesktop)',
    'NavCard("$","General buys & GP","A / B / C / Luxury buying targets",onGp)':
        'NavCard(MorleyIcons.Money,"General Buys / GP","A / B / C / Luxury buying targets",onGp)',
}
for old, new in replacements.items():
    text = text.replace(old, new)

dashboard.write_text(text, encoding='utf-8')

text = main.read_text(encoding='utf-8')
legacy_enums = (
    'enum class Page(val label:String,val icon:String){Home("Home","⌂"),Laptop("Computer","▱"),Desktop("Console","◫"),GP("GP","$"),More("More","⚙")}',
    'enum class Page(val label:String,val icon:String){Home("Home","⌂"),Laptop("Computer","💻"),Desktop("Console","🎮"),GP("GP","$"),More("More","⚙")}',
    'enum class Page(val label:String,val icon:String){Home("Home","home"),Laptop("Computer","computer"),Desktop("Console","console"),GP("GP","money"),More("More","menu")}',
)
current_enum = 'enum class Page(val label:String,val icon:String){Home("Home","⌂"),Laptop("Laptop","💻"),Desktop("Desktop","🖥"),GP("GP","$"),More("More","⚙")}'
for old in legacy_enums:
    text = text.replace(old, current_enum)
main.write_text(text, encoding='utf-8')

print('Applied Laptop / MacBook and Desktop / Gaming PC pricing navigation contract')
