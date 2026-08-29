package com.buysloans.hub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val HelpAccent = Color(0xFF16C7FF)
private val HelpStrong = Color(0xFF2684FF)
private val HelpBg = Color(0xFF030712)
private val HelpCard = Color(0xFF0B1528)
private val HelpMuted = Color(0xFF8EA6C4)

class HelpGuideActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(3, 7, 18)
        window.navigationBarColor = android.graphics.Color.rgb(3, 7, 18)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = HelpAccent, secondary = HelpStrong, background = HelpBg, surface = HelpCard)) {
                HelpGuideScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HelpGuideScreen(onBack: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    Scaffold(
        containerColor = HelpBg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF050B16), titleContentColor = Color.White),
                navigationIcon = { IconButton(onClick = onBack) { Text("‹", fontSize = 34.sp, color = HelpAccent) } },
                title = { Text("Help & Guide", fontWeight = FontWeight.Black) }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("B&L Morley guide", fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text("Learn the buying workflow, grade rules, automatic Max Buy and how to read valuation results.", color = HelpMuted, lineHeight = 20.sp)
            TabRow(selectedTabIndex = tab, containerColor = HelpCard, contentColor = HelpAccent) {
                listOf("How to use", "Features", "FAQ").forEachIndexed { index, label ->
                    Tab(selected = tab == index, onClick = { tab = index }, text = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) })
                }
            }
            when (tab) {
                0 -> HowToSection()
                1 -> FeatureBreakdown()
                else -> FaqSection()
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HowToSection() {
    HelpIntro("Quick start", "Enter the item and seller ask, choose the correct grade, obtain or enter the current market value, then let B&L Morley calculate the Max Buy before you make a decision.")
    StepCard("1", "Choose the right workflow", "Use Quick Deal for a fast manual decision, Laptop for complete laptops/MacBooks, Desktop for complete PCs or component builds, Test & Buy when hardware checks are required, and GP for general merchandise.")
    StepCard("2", "Choose the item grade", "A Grade targets 30% GP, B Grade 50%, C Grade 70%, and Luxury 30%. The selected grade controls the buying limit; do not type a separate Max Buy where automatic pricing is available.")
    StepCard("3", "Enter the seller ask", "Seller Ask is the price quoted by the seller. B&L Morley does not invent this figure. It is compared with the calculated Max Buy to show whether the deal is within limit, needs negotiation, or should be passed/rejected.")
    StepCard("4", "Set or verify market value", "Quick Deal and Test & Buy use the current market/expected sale value you enter. Laptop and Desktop use live Australian market evidence. Review exact matches, confidence and comparable titles before relying on the result.")
    StepCard("5", "Read the automatic Max Buy", "Max Buy is derived from the market value and selected GP target: A/Luxury use 70% of sale value, B uses 50%, and C uses 30%. It is a buying ceiling, not the price you must offer.")
    StepCard("6", "Complete Test & Buy checks", "For tested hardware, complete every applicable checklist item and record faults. An incomplete checklist now shows completion guidance rather than incorrectly labelling the deal as rejected.")
    StepCard("7", "Save or action the deal", "Quick Deal only enables Save Deal once the item, seller ask and market value are valid. Save useful valuations, add purchased items to Inventory, then record the sale later so realised profit is tracked.")
    StepCard("8", "Keep the app current", "Open Menu → Updates to check for the latest verified Android release. Use System Diagnostics if account, network, notification or update readiness does not look right.")
}

@Composable
private fun FeatureBreakdown() {
    FeatureCard("⚡ Quick Deal Mode", "Enter item, Seller Ask and market value, select A/B/C/Luxury, and receive an automatically calculated Max Buy plus instant deal guidance. Invalid/incomplete deals cannot be saved.")
    FeatureCard("✓ Test & Buy", "Runs a device-category hardware checklist, records faults and calculates Max Buy from current valuation plus the selected grade. Readiness and rejection states explain exactly what is incomplete or over limit.")
    FeatureCard("💻 Laptop / MacBook", "Whole-device valuation using live Australian market evidence. Model codes can be resolved to a known product identity before matching, with the selected grade determining the buying target.")
    FeatureCard("🖥 Desktop / Gaming PC", "Values complete OEM desktops where possible, or prices detected components such as CPU, GPU, RAM and storage for custom PCs. Grade-driven GP determines Max Buy.")
    FeatureCard("💰 General Buys / GP", "Calculates a maximum buy price from the expected sale price using A 30%, B 50%, C 70% or Luxury 30% GP targets.")
    FeatureCard("◷ Valuations & Deals", "Stores useful valuation results so you can compare opportunities, review past pricing and reopen saved deals.")
    FeatureCard("▣ Inventory", "Tracks stock quantity, barcode/serial, cost, expected resale and potential profit.")
    FeatureCard("↗ Sales History", "Records completed sales and shows revenue, cost and realised gross profit.")
    FeatureCard("⌗ Barcode Scanner", "Scans or searches a barcode/serial to find existing inventory quickly or start adding a new item.")
    FeatureCard("Smart Workspace", "Surfaces saved-deal opportunities, Quick Deal and summary signals from your current workspace so strong deals are easier to spot.")
    FeatureCard("Market evidence", "Shows the listings used to support a valuation. Whole-device filters reject obvious accessories, scrap, faulty and parts-only results where they are detected.")
    FeatureCard("Confidence & match quality", "Confidence reflects the amount and quality of usable evidence. Exact matches are strongest; similar/spec matches support the result but should not replace exact evidence.")
    FeatureCard("Backup & Data", "Exports or restores supported local workspace data so inventory and other local records can be protected.")
    FeatureCard("Notifications", "Controls app/update notifications and provides a notification centre for messages delivered to the app.")
    FeatureCard("Updates", "Checks the verified OTA feed for a newer signed APK and guides installation when a release is available.")
    FeatureCard("System Diagnostics", "Checks app, account, network, notification and update readiness when something does not look right.")
}

@Composable
private fun FaqSection() {
    FaqCard("What do A, B, C and Luxury mean?", "They are target gross-profit rules. A is 30% GP, B is 50%, C is 70%, and Luxury is 30%. The grade affects Max Buy, so select the grade that matches the item and condition before relying on the buying limit.")
    FaqCard("Why is Max Buy automatic?", "Max Buy should be derived consistently from the current market/expected sale value and selected grade. Automatic calculation avoids staff entering a Max Buy that conflicts with the chosen GP target.")
    FaqCard("How is Max Buy calculated?", "The app uses market value × (1 − target GP). For example, A/Luxury use 70% of market value, B uses 50%, and C uses 30%.")
    FaqCard("What is Seller Ask?", "Seller Ask is the price the seller actually wants. It remains manually entered and is compared with Max Buy; the app should never invent a seller price.")
    FaqCard("Why is Save Deal disabled in Quick Deal?", "Quick Deal requires an item/model, a valid Seller Ask and a valid market value. Once those are present, Max Buy is calculated automatically and the deal can be saved.")
    FaqCard("Why does Test & Buy say Complete Test & Pricing?", "It means required information or applicable hardware checks are still incomplete. This is not a rejected deal; finish the missing test/pricing steps and the guidance will update.")
    FaqCard("What do BUY, NEGOTIATE and PASS mean?", "BUY means the ask is within the protected buying target. NEGOTIATE means it is close enough to review for a lower price. PASS/REJECT means the ask is above the permitted Max Buy or testing has identified a blocker. Always review the supporting evidence and recorded faults.")
    FaqCard("What does Exact mean?", "The listing matches the required brand/product family and enough model identity to be treated as the same device. Exact comparables are the strongest evidence for a protected used-market value.")
    FaqCard("What does Similar mean?", "The listing is related and may share family/specs, but it is not safe enough to drive the primary exact value by itself.")
    FaqCard("Why can a valuation say unavailable?", "The app may not have enough trustworthy exact evidence. This is safer than inventing a price from unrelated or low-quality listings.")
    FaqCard("Why are some cheap listings excluded?", "Listings that appear to be parts, scrap, faulty devices, replacement components or accessories can be rejected so they do not drag down a whole-device valuation.")
    FaqCard("What should I enter for a MacBook model code?", "Enter the code directly, such as A1932. When the catalogue recognises it, B&L Morley can resolve it to the relevant MacBook family before searching for evidence.")
    FaqCard("Why does the used value differ from new retail?", "Used value is based on used-market evidence when available. New retail is a separate cross-check and can also be used to detect obviously unrealistic used results.")
    FaqCard("Should I trust one comparable?", "Treat a single comparable cautiously. More consistent exact results across reliable sources normally justify higher confidence.")
    FaqCard("Can I use the app without internet?", "Local data may remain visible, but live market searches, account checks and update checks require a network connection.")
    FaqCard("How do I back up my data?", "Open Menu → Backup & Data and export a backup. Keep the exported file somewhere safe. Use Import/Restore from the same area when needed.")
    FaqCard("How do I update the Android app?", "Open Menu → Updates and check for updates. Only install the verified release presented by the B&L Morley updater.")
    FaqCard("What if a result looks wrong?", "Check the comparable titles, selected grade and confidence first. If unrelated listings are being counted, use Menu → Report an Issue and include the model/search term and a screenshot when possible.")
}

@Composable
private fun HelpIntro(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0B2035)), border = BorderStroke(1.dp, HelpAccent.copy(alpha = .35f)), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Black, color = HelpAccent)
            Text(body, color = Color.White, lineHeight = 21.sp)
        }
    }
}

@Composable
private fun StepCard(number: String, title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = HelpCard), border = BorderStroke(1.dp, HelpAccent.copy(alpha = .18f)), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(15.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(color = HelpStrong.copy(alpha = .18f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, HelpAccent.copy(alpha = .25f))) {
                Text(number, Modifier.padding(horizontal = 13.dp, vertical = 9.dp), color = HelpAccent, fontWeight = FontWeight.Black)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text(body, color = HelpMuted, lineHeight = 19.sp, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun FeatureCard(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = HelpCard), border = BorderStroke(1.dp, HelpAccent.copy(alpha = .18f)), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Text(body, color = HelpMuted, lineHeight = 19.sp, fontSize = 13.sp)
        }
    }
}

@Composable
private fun FaqCard(question: String, answer: String) {
    var expanded by remember { mutableStateOf(false) }
    Card(onClick = { expanded = !expanded }, colors = CardDefaults.cardColors(containerColor = HelpCard), border = BorderStroke(1.dp, HelpAccent.copy(alpha = .18f)), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(question, Modifier.weight(1f), fontWeight = FontWeight.Black, fontSize = 15.sp)
                Text(if (expanded) "−" else "+", color = HelpAccent, fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
            if (expanded) Text(answer, color = HelpMuted, lineHeight = 19.sp, fontSize = 13.sp)
        }
    }
}
