package com.buysloans.hub

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch

private val ReferenceNavy = Color(0xFF032A4F)
private val ReferenceBlue = Color(0xFF0878F9)
private val ReferencePaleBlue = Color(0xFFEAF4FF)

class DashboardActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { FirebaseMessaging.getInstance().token.addOnSuccessListener { token -> DeviceRegistrar.register(this, token) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (getSharedPreferences("display_settings", MODE_PRIVATE).getBoolean("keep_awake", false)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        NotificationHelper.createChannels(this)
        if (!AuthManager.isSignedIn(this)) {
            startActivity(Intent(this, AuthActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            })
            finish()
            return
        }
        val prefs = getSharedPreferences("app_state", MODE_PRIVATE)
        val previous = prefs.getInt("last_seen_version_code", 0)
        val updated = previous > 0 && previous < BuildConfig.VERSION_CODE
        prefs.edit().putInt("last_seen_version_code", BuildConfig.VERSION_CODE).apply()
        setContent { RootApp(updated) }
    }

    fun enableNotificationsAndRegister() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token -> DeviceRegistrar.register(this, token) }
        }
    }
}

@Composable
private fun RootApp(showUpdatedInitially: Boolean) {
    MaterialTheme(colorScheme = MorleyColorScheme) { DashboardApp(showUpdatedInitially) }
}

// Compact phone navigation follows the approved mobile reference.
private enum class BottomDestination(val label: String, val icon: ImageVector) {
    HOME("Home", MorleyIcons.Home),
    CATALOGUE("Catalogue", MorleyIcons.Categories),
    SCAN("Scan", MorleyIcons.Phone),
    TRADE("Trade", MorleyIcons.Money)
}

// Larger screens keep the established Categories and General Buys routes instead of
// losing functionality just because the compact phone navigation is scan-first.
private enum class ExpandedDestination(val label: String, val icon: ImageVector) {
    HOME("Home", MorleyIcons.Home),
    CATEGORIES("Categories", MorleyIcons.Categories),
    GP("General Buys", MorleyIcons.Money),
    MORE("More", MorleyIcons.More)
}

@Composable
private fun CompactDashboardNavigation(page: Page, showMenu: Boolean, onSelect: (BottomDestination) -> Unit) {
    NavigationBar(containerColor = Color.White, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        BottomDestination.entries.forEach { destination ->
            val selected = when (destination) {
                BottomDestination.HOME -> !showMenu && page == Page.Home
                BottomDestination.CATALOGUE -> !showMenu && page == Page.Laptop
                BottomDestination.SCAN -> false
                BottomDestination.TRADE -> !showMenu && page == Page.GP
            }
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(destination) },
                icon = { MorleyIcon(destination.icon, destination.label, if (selected) ReferenceBlue else MorleyTextSecondary, Modifier.size(21.dp)) },
                label = { Text(destination.label, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = ReferencePaleBlue,
                    selectedIconColor = ReferenceBlue,
                    selectedTextColor = ReferenceBlue,
                    unselectedIconColor = MorleyTextSecondary,
                    unselectedTextColor = MorleyTextSecondary
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardApp(showUpdatedInitially: Boolean = false) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val adaptiveSize = morleyAdaptiveSize()
    var page by remember { mutableStateOf(Page.Home) }
    var previousPage by remember { mutableStateOf(Page.Home) }
    var showMenu by remember { mutableStateOf(false) }
    var showUpdated by remember { mutableStateOf(showUpdatedInitially) }
    var confirmSignOut by remember { mutableStateOf(false) }

    fun openMenu() {
        if (!showMenu) previousPage = page
        showMenu = true
    }

    fun closeMenu() {
        showMenu = false
        page = previousPage
    }

    fun selectDestination(destination: BottomDestination) {
        when (destination) {
            BottomDestination.HOME -> { showMenu = false; page = Page.Home }
            BottomDestination.CATALOGUE -> { showMenu = false; page = Page.Laptop }
            BottomDestination.SCAN -> {
                showMenu = false
                context.startActivity(Intent(context, DeviceLensActivity::class.java))
            }
            BottomDestination.TRADE -> { showMenu = false; page = Page.GP }
        }
    }

    fun selectExpandedDestination(destination: ExpandedDestination) {
        when (destination) {
            ExpandedDestination.HOME -> { showMenu = false; page = Page.Home }
            ExpandedDestination.CATEGORIES -> { showMenu = false; page = Page.Laptop }
            ExpandedDestination.GP -> { showMenu = false; page = Page.GP }
            ExpandedDestination.MORE -> openMenu()
        }
    }

    AdaptiveBackHandler(enabled = showMenu || page != Page.Home) {
        if (showMenu) closeMenu() else page = Page.Home
    }

    if (showUpdated) {
        AlertDialog(
            onDismissRequest = { showUpdated = false },
            title = { Text("Update installed") },
            text = { Text("B&L Morley has been updated successfully to v${BuildConfig.VERSION_NAME}.") },
            confirmButton = { Button(onClick = { showUpdated = false }) { Text("Continue") } }
        )
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sign out?") },
            text = { Text("You are signed in as ${AuthManager.accountLabel(context)}. You will need to sign in again to use B&L Morley.") },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") } },
            confirmButton = {
                Button(
                    onClick = {
                        AuthManager.signOut(context)
                        context.startActivity(Intent(context, AuthActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) })
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7D2B38), contentColor = Color.White)
                ) { Text("Sign out", fontWeight = FontWeight.Black) }
            }
        )
    }

    val adaptiveNavItems = ExpandedDestination.entries.map { destination ->
        val selected = when (destination) {
            ExpandedDestination.HOME -> !showMenu && page == Page.Home
            ExpandedDestination.CATEGORIES -> !showMenu && page == Page.Laptop
            ExpandedDestination.GP -> !showMenu && page == Page.GP
            ExpandedDestination.MORE -> showMenu
        }
        AdaptiveNavItem(destination.label, destination.icon, selected) { selectExpandedDestination(destination) }
    }

    Scaffold(
        containerColor = MorleyBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ReferenceNavy, titleContentColor = Color.White),
                navigationIcon = {
                    IconButton(onClick = { if (showMenu) closeMenu() else openMenu() }) {
                        MorleyIcon(MorleyIcons.Menu, if (showMenu) "Close menu" else "Open menu", Color.White, Modifier.size(26.dp))
                    }
                },
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        Text("MORLEY BUYS", fontSize = 21.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text("Buy  •  Sell  •  Trade", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = .70f))
                    }
                },
                actions = {
                    Surface(color = Color.White.copy(alpha = .12f), border = BorderStroke(1.dp, Color.White.copy(alpha = .22f)), shape = RoundedCornerShape(999.dp)) {
                        Text(AuthManager.accountLabel(context), Modifier.padding(horizontal = 10.dp, vertical = 7.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(Modifier.width(8.dp))
                }
            )
        },
        bottomBar = {
            if (adaptiveSize == MorleyAdaptiveSize.Compact) CompactDashboardNavigation(page, showMenu, ::selectDestination)
        }
    ) { pad ->
        Row(Modifier.padding(pad).consumeWindowInsets(pad).fillMaxSize()) {
            if (adaptiveSize != MorleyAdaptiveSize.Compact) {
                MorleyAdaptiveNavigation(size = adaptiveSize, items = adaptiveNavItems, compact = {})
            }
            AdaptiveContentFrame {
                Box(Modifier.fillMaxSize()) {
                    if (showMenu) {
                        MoreHub(onSignOut = { confirmSignOut = true })
                    } else {
                        when (page) {
                            Page.Home -> ParityHome { page = Page.GP }
                            Page.Laptop -> CategoriesPricingScreen()
                            Page.Desktop -> ConsolePricingScreen()
                            Page.GP -> GPFix()
                            Page.More -> ParityHome { page = Page.GP }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParityHome(onGp: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current

    fun openFeature(feature: String) {
        context.startActivity(Intent(context, MenuFeatureActivity::class.java).putExtra(MenuFeatureActivity.EXTRA_FEATURE, feature))
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            onClick = { context.startActivity(Intent(context, UniversalBuySearchActivity::class.java)) },
            color = Color.White,
            border = BorderStroke(1.dp, LensHomeBorder),
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("⌕", color = ReferenceBlue, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(10.dp))
                Text("Search devices, stock or scan...", color = MorleyTextSecondary, fontSize = 14.sp)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            ReferenceQuickTile(
                icon = "▣",
                title = "Scan Device",
                subtitle = "2-photo AI check",
                modifier = Modifier.weight(1f),
                onClick = { context.startActivity(Intent(context, DeviceLensActivity::class.java)) }
            )
            ReferenceQuickTile(
                icon = "⌕",
                title = "Manual Search",
                subtitle = "Find by model",
                modifier = Modifier.weight(1f),
                onClick = { context.startActivity(Intent(context, UniversalBuySearchActivity::class.java)) }
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            ReferenceQuickTile(
                icon = "+",
                title = "Add to Catalogue",
                subtitle = "Scan then add",
                modifier = Modifier.weight(1f),
                onClick = { context.startActivity(Intent(context, DeviceLensActivity::class.java)) }
            )
            ReferenceQuickTile(
                icon = "$",
                title = "Price Check",
                subtitle = "Compare prices",
                modifier = Modifier.weight(1f),
                onClick = { context.startActivity(Intent(context, UniversalBuySearchActivity::class.java)) }
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            ReferenceQuickTile(
                icon = "▦",
                title = "View Stock",
                subtitle = "Current inventory",
                modifier = Modifier.weight(1f),
                onClick = { openFeature("inventory") }
            )
            ReferenceQuickTile(
                icon = "⚙",
                title = "Settings",
                subtitle = "App preferences",
                modifier = Modifier.weight(1f),
                onClick = { openFeature("display") }
            )
        }

        Card(
            onClick = { context.startActivity(Intent(context, UniversalBuySearchActivity::class.java)) },
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, LensHomeBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(12.dp), color = ReferencePaleBlue, modifier = Modifier.size(46.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text("G", color = ReferenceBlue, fontWeight = FontWeight.Black, fontSize = 21.sp) }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Google camera search", color = MorleyTextPrimary, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    Text("Scan a barcode, QR code or encoded model/stock label", color = MorleyTextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
                }
                Text("›", color = ReferenceBlue, fontWeight = FontWeight.Black, fontSize = 24.sp)
            }
        }

        Surface(color = ReferencePaleBlue, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Text(
                "Device Scan uses Morley Vision for the two-photo model, condition and damage assessment. Google Play services is used separately for code scanning in Morley Search.",
                Modifier.padding(11.dp),
                color = Color(0xFF28577E),
                fontSize = 10.sp,
                lineHeight = 14.sp,
                textAlign = TextAlign.Center
            )
        }

        TextButton(onClick = onGp, modifier = Modifier.fillMaxWidth()) {
            Text("General Buys / GP • Trade-in tools", color = ReferenceBlue, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(2.dp))
    }
}

private val LensHomeBorder = Color(0xFFD8E2EE)

@Composable
private fun ReferenceQuickTile(
    icon: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, LensHomeBorder),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.height(106.dp)
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(icon, color = ReferenceBlue, fontSize = 27.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(3.dp))
            Text(title, color = MorleyTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Text(subtitle, color = MorleyTextSecondary, fontSize = 10.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun MoreHub(onSignOut: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var updateStatus by remember { mutableStateOf("Check app version and update status.") }
    var availableUpdate by remember { mutableStateOf<AppUpdate?>(null) }
    val notificationUnread = NotificationInboxStore.unreadCount(context)

    fun open(feature: String) {
        context.startActivity(Intent(context, MenuFeatureActivity::class.java).putExtra(MenuFeatureActivity.EXTRA_FEATURE, feature))
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("More", fontSize = 30.sp, fontWeight = FontWeight.Black, color = MorleyTextPrimary)
        MenuSection("Workspace") {
            MenuRow("◉", "Device scan", "Take front and back photos for model, damage and condition analysis.") { context.startActivity(Intent(context, DeviceLensActivity::class.java)) }
            MenuRow("⌕", "Morley search", "Search by name/model or scan a code with the Google camera.") { context.startActivity(Intent(context, UniversalBuySearchActivity::class.java)) }
            MenuRow("◷", "Valuations & deals", "Saved valuations and deal history.") { context.startActivity(Intent(context, ValuationHistoryActivity::class.java)) }
            MenuRow("✓", "Test & buy", "Run a hardware checklist and compare the seller ask with Max Buy guidance.") { context.startActivity(Intent(context, TestBuyActivity::class.java)) }
            MenuRow("▣", "Stock", "Current inventory, costs and resale values.") { open("inventory") }
            MenuRow("↗", "Sales history", "Revenue and realised profit.") { open("sales") }
            MenuRow("⌗", "Barcode scanner", "Find or add stock quickly.") { open("scanner") }
        }
        MenuSection("Your account") {
            MenuRow("●", "Account & profile", AuthManager.accountLabel(context)) { open("account") }
            MenuRow("⌁", "Privacy & security", "Account privacy and session security.") { open("privacy") }
        }
        MenuSection("Data & preferences") {
            MenuRow("☁", "Backup & data", "Export, import and local app data.") { open("backup") }
            MenuRow("✦", "Notification centre", if (notificationUnread == 0) "No unread notifications." else "$notificationUnread unread notification${if (notificationUnread == 1) "" else "s"}.") { context.startActivity(Intent(context, NotificationCentreActivity::class.java)) }
            MenuRow("♢", "Notifications", "Update and app notification preferences.") { open("notifications") }
            MenuRow("◐", "Display", "Interface and display preferences.") { open("display") }
        }
        MenuSection("Help & guidance") {
            MenuRow("?", "How-to guide & FAQ", "Quick start, feature breakdown and common questions.") { context.startActivity(Intent(context, HelpGuideActivity::class.java)) }
        }
        MenuSection("App") {
            MenuRow("↻", "Updates", updateStatus) {
                checking = true
                availableUpdate = null
                updateStatus = "Checking for updates…"
                scope.launch {
                    runCatching { UpdateManager.check() }
                        .onSuccess { u ->
                            availableUpdate = u
                            updateStatus = if (u == null) "You're up to date on v${BuildConfig.VERSION_NAME}." else "${u.versionName} is available. ${u.notes}"
                        }
                        .onFailure { updateStatus = "Update check failed: ${it.message ?: "network error"}" }
                    checking = false
                }
            }
            if (checking) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            availableUpdate?.let { u ->
                OutlinedButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) UpdateManager.openInstallerPermission(context)
                        else UpdateManager.openDownload(context, u)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Download ${u.versionName}") }
            }
            MenuRow("◇", "System diagnostics", "Check app, account, network, notification and OTA readiness.") { context.startActivity(Intent(context, DiagnosticsActivity::class.java)) }
            MenuRow("⚑", "Support", "View your tickets, support replies and securely reply from the app.") { context.startActivity(Intent(context, SupportTicketActivity::class.java)) }
            MenuRow("§", "Legal & privacy", "Privacy and application information.") { open("legal") }
            MenuRow("ⓘ", "About B&L Morley", "Version ${BuildConfig.VERSION_NAME}") { open("about") }
        }
        Button(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF57202A), contentColor = Color(0xFFFFD9DE)),
            shape = RoundedCornerShape(16.dp)
        ) { Text("Sign out", fontWeight = FontWeight.Black) }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun MenuSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = MorleyTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Card(
            colors = CardDefaults.cardColors(containerColor = MorleySurface),
            border = BorderStroke(1.dp, MorleyBorder),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) { Column(content = content) }
    }
}

@Composable
private fun MenuRow(icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 13.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(color = MorleyAccentSoft, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MorleyBorder)) {
                Text(icon, Modifier.padding(10.dp), color = MorleyAccent, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = MorleyTextPrimary, fontWeight = FontWeight.Black, fontSize = 15.sp)
                Text(subtitle, color = MorleyTextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
            }
            Text("›", color = MorleyAccent, fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun StatusTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MorleySurfaceRaised),
        border = BorderStroke(1.dp, MorleyBorder.copy(alpha = .7f)),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
    ) {
        Column(Modifier.padding(13.dp)) {
            Text(label, color = MorleyTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(value, color = MorleySuccess, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun NavCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MorleySurface),
        border = BorderStroke(1.dp, MorleyBorder),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(15.dp), horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            Surface(color = MorleyAccentSoft, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MorleyBorder)) {
                MorleyIcon(icon, title, MorleyAccent, Modifier.padding(10.dp).size(25.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = MorleyTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text(subtitle, color = MorleyTextPrimary.copy(alpha = .82f), fontSize = 13.sp, lineHeight = 18.sp)
            }
            Text("›", color = MorleyAccent, fontSize = 25.sp, fontWeight = FontWeight.Black)
        }
    }
}