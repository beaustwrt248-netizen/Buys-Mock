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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class HelpGuideActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(8, 11, 13)
        window.navigationBarColor = android.graphics.Color.rgb(8, 11, 13)
        setContent {
            MaterialTheme(colorScheme = MorleyColorScheme) {
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
        containerColor = MorleyBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MorleyBackground, titleContentColor = MorleyTextPrimary),
                navigationIcon = { IconButton(onClick = onBack) { Text("‹", fontSize = 34.sp, color = MorleyAccent) } },
                title = { Text("Help & FAQ", fontWeight = FontWeight.Black) }
            )
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("B&L Morley guide", fontSize = 28.sp, fontWeight = FontWeight.Black, color = MorleyTextPrimary)
            Text("The same Computer Pricing, Console Pricing, valuation and support workflow used across the Morley app and buyshub.me.", color = MorleyTextSecondary, lineHeight = 20.sp)
            TabRow(selectedTabIndex = tab, containerColor = MorleySurface, contentColor = MorleyAccent) {
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
    HelpIntro("Quick start", "Choose the correct pricing workflow, identify the item accurately, enter Seller Ask, select the correct grade, review market evidence and use the calculated Max Buy as the protected buying ceiling.")
    StepCard("1", "Choose the right pricing area", "Computer Pricing contains Laptop / MacBook and Desktop / Gaming PC. Console Pricing covers PS4, PS5, Xbox and Nintendo. General Buys / GP covers other merchandise. Test & Buy adds hardware checks when required.")
    StepCard("2", "Identify the item cleanly", "Use the clearest model, model code, console variant or hardware specification available. Exact identity improves comparable quality and protects the valuation from unrelated results.")
    StepCard("3", "Enter Seller Ask", "Seller Ask is the amount requested by the seller. It stays user-entered and must never be invented by the app.")
    StepCard("4", "Select the correct grade", "A targets 30% GP, B 50%, C 70%, and Luxury 30%. Grade changes Max Buy, so confirm it before making a decision.")
    StepCard("5", "Review valuation evidence", "Exact matches carry the most weight. Similar/spec matches are supporting evidence. Review confidence, comparable titles and source quality before relying on the result.")
    StepCard("6", "Use Max Buy correctly", "Max Buy is market value × (1 − target GP). It is a ceiling, not a mandatory offer. BUY, NEGOTIATE and PASS guidance compares Seller Ask with that protected limit.")
    StepCard("7", "Use NFC and scanning safely", "On NFC-capable Android devices, open the NFC scanner and present a tag. Morley reports unavailable or disabled NFC clearly, debounces repeated reads and can read supported text/URI NDEF payloads. Barcode/serial scanning remains available for stock lookup.")
    StepCard("8", "Save, stock and sell", "Save useful valuations, add purchased items to Inventory and record completed sales so realised gross profit is tracked.")
    StepCard("9", "Keep Morley current", "Use Menu → Updates for the latest verified Android release. buyshub.me updates from the production web build. Use System Diagnostics or Report an Issue if anything looks wrong.")
}

@Composable
private fun FeatureBreakdown() {
    FeatureCard("Computer Pricing", "One primary computer workflow containing Laptop / MacBook guided exact-model pricing and Desktop / Gaming PC component-based valuation.")
    FeatureCard("Console Pricing", "A separate primary pricing area for PS4, PS5, Xbox and Nintendo with protected grade rules.")
    FeatureCard("Quick Deal", "Fast item, Seller Ask, market value and grade capture with automatic Max Buy and deal guidance.")
    FeatureCard("Test & Buy", "Category-specific hardware checks, recorded faults, NFC evidence boundaries and valuation guidance before purchase.")
    FeatureCard("Valuation engine", "Combines model resolution, comparable quality, confidence, condition/risk controls and protected target-margin rules. When trustworthy evidence is insufficient, Morley should say so rather than invent a value.")
    FeatureCard("NFC scanner", "Detects NFC availability, reads supported NDEF text/URI payloads, captures tag IDs and prevents accidental rapid duplicate reads.")
    FeatureCard("General Buys / GP", "Calculates maximum buy for other merchandise from expected sale price using A 30%, B 50%, C 70% or Luxury 30% GP targets.")
    FeatureCard("Valuations & Deals", "Stores useful valuation results and saved opportunities for later review.")
    FeatureCard("Inventory", "Tracks stock quantity, barcode/serial, cost, expected resale and potential profit.")
    FeatureCard("Sales History", "Records completed sales and realised gross profit.")
    FeatureCard("Backup & Data", "Exports and restores supported local workspace data without including authentication credentials.")
    FeatureCard("Notifications & Updates", "Provides app messages and checks the verified OTA feed for newer signed Android releases.")
    FeatureCard("System Diagnostics", "Checks account, network, notification and update readiness when something does not look right.")
}

@Composable
private fun FaqSection() {
    FaqCard("Why do the app and website look the same?", "Morley is one product across Android and buyshub.me. Shared navigation, colours, wording, pricing workflows and supported business rules are kept in parity; only platform-specific capabilities such as Android NFC or APK installation differ.")
    FaqCard("Where are Laptop and Desktop?", "Both are inside Computer Pricing. Choose Laptop / MacBook for guided exact-model pricing or Desktop / Gaming PC for component-based valuation.")
    FaqCard("Where is Console Pricing?", "Console Pricing is a separate primary Morley category for PS4, PS5, Xbox and Nintendo.")
    FaqCard("What do A, B, C and Luxury mean?", "They are target gross-profit rules: A 30%, B 50%, C 70% and Luxury 30%. The selected grade controls Max Buy.")
    FaqCard("How is Max Buy calculated?", "Morley uses market or expected sale value × (1 − target GP). A/Luxury use 70% of value, B 50% and C 30%.")
    FaqCard("What is Seller Ask?", "Seller Ask is the price the seller actually wants. It is entered by the user and compared with Max Buy; Morley must not invent it.")
    FaqCard("What do BUY, NEGOTIATE and PASS mean?", "BUY is within the protected target. NEGOTIATE means a lower price may bring the deal inside target. PASS/REJECT means the ask or test result is outside the permitted buying boundary.")
    FaqCard("What does Exact mean?", "The comparable has enough brand, family and model identity to be treated as the same device. Exact matches are the strongest evidence.")
    FaqCard("Why can a valuation be unavailable?", "There may not be enough trustworthy exact evidence. Returning unavailable is safer than manufacturing a price from weak or unrelated listings.")
    FaqCard("Why are some listings excluded?", "Parts, scrap, faulty devices, replacement components and obvious accessories can be excluded so they do not distort a whole-device valuation.")
    FaqCard("Does NFC work on every phone?", "No. Android hardware support varies. Morley reports when NFC hardware is unavailable or switched off. NFC is an Android capability and is not emulated on the website.")
    FaqCard("What can the NFC scanner read?", "It captures the tag ID and supports common well-known NDEF text and URI records. Unsupported records are not treated as trusted valuation evidence by themselves.")
    FaqCard("Should I trust one comparable?", "Treat a single comparable cautiously. More consistent exact evidence normally supports higher confidence.")
    FaqCard("Can I use Morley offline?", "Some local data can remain visible, but live market searches, account checks and update checks require a network connection.")
    FaqCard("How do I back up my data?", "Open Menu → Backup & Data and export a backup. Store it safely and use the same area to restore supported local data.")
    FaqCard("How do I update Android?", "Open Menu → Updates. Install only the signed release presented by the verified B&L Morley updater.")
    FaqCard("What if a result or function looks wrong?", "Check identity, grade, Seller Ask, evidence and confidence first. Then use Menu → Report an Issue and include the model/search term, what you expected and a screenshot when possible.")
}

@Composable
private fun HelpIntro(title: String, body: String) = InfoCard(title, body, highlighted = true)

@Composable
private fun StepCard(number: String, title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MorleySurface), border = BorderStroke(1.dp, MorleyBorder), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(15.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(color = MorleyAccentSoft, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MorleyBorder)) {
                Text(number, Modifier.padding(horizontal = 13.dp, vertical = 9.dp), color = MorleyAccent, fontWeight = FontWeight.Black)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, fontWeight = FontWeight.Black, fontSize = 16.sp, color = MorleyTextPrimary)
                Text(body, color = MorleyTextSecondary, lineHeight = 19.sp, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun FeatureCard(title: String, body: String) = InfoCard(title, body)

@Composable
private fun InfoCard(title: String, body: String, highlighted: Boolean = false) {
    Card(colors = CardDefaults.cardColors(containerColor = if (highlighted) MorleyAccentSoft else MorleySurface), border = BorderStroke(1.dp, if (highlighted) MorleyAccent.copy(alpha = .45f) else MorleyBorder), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.Black, fontSize = 16.sp, color = if (highlighted) MorleyAccent else MorleyTextPrimary)
            Text(body, color = if (highlighted) MorleyTextPrimary else MorleyTextSecondary, lineHeight = 19.sp, fontSize = 13.sp)
        }
    }
}

@Composable
private fun FaqCard(question: String, answer: String) {
    var expanded by remember { mutableStateOf(false) }
    Card(onClick = { expanded = !expanded }, colors = CardDefaults.cardColors(containerColor = MorleySurface), border = BorderStroke(1.dp, MorleyBorder), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(question, Modifier.weight(1f), fontWeight = FontWeight.Black, fontSize = 15.sp, color = MorleyTextPrimary)
                Text(if (expanded) "−" else "+", color = MorleyAccent, fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
            if (expanded) Text(answer, color = MorleyTextSecondary, lineHeight = 19.sp, fontSize = 13.sp)
        }
    }
}
