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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch

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

private enum class BottomDestination(val label: String, val icon: ImageVector) {
    HOME("Home", MorleyIcons.Home),
    CATEGORIES("Categories", MorleyIcons.Categories),
    GP("General Buys", MorleyIcons.Money),
    MORE("More", MorleyIcons.More)
}

@Composable
private fun CompactDashboardNavigation(page: Page, showMenu: Boolean, onSelect: (BottomDestination) -> Unit) {
    NavigationBar(containerColor = MorleySurface, tonalElevation = 0.dp, modifier = Modifier.fillMaxWidth()) {
        BottomDestination.entries.forEach { destination ->
            val selected = when (destination) {
                BottomDestination.HOME -> !showMenu && page == Page.Home
                BottomDestination.CATEGORIES -> !showMenu && page == Page.Laptop
                BottomDestination.GP -> !showMenu && page == Page.GP
                BottomDestination.MORE -> showMenu
            }
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(destination) },
                icon = { MorleyIcon(destination.icon, destination.label, if (selected) MorleyAccent else MorleyTextSecondary, Modifier.size(21.dp)) },
                label = { Text(destination.label, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MorleyAccentSoft,
                    selectedIconColor = MorleyAccent,
                    selectedTextColor = MorleyAccent,
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
            BottomDestination.CATEGORIES -> { showMenu = false; page = Page.Laptop }
            BottomDestination.GP -> { showMenu = false; page = Page.GP }
            BottomDestination.MORE -> openMenu()
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

    val adaptiveNavItems = BottomDestination.entries.map { destination ->
        val selected = when (destination) {
            BottomDestination.HOME -> !showMenu && page == Page.Home
            BottomDestination.CATEGORIES -> !showMenu && page == Page.Laptop
            BottomDestination.GP -> !showMenu && page == Page.GP
            BottomDestination.MORE -> showMenu
        }
        AdaptiveNavItem(destination.label, destination.icon, selected) { selectDestination(destination) }
    }

    Scaffold(
        containerColor = MorleyBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MorleyBackground.copy(alpha = .98f), titleContentColor = MorleyTextPrimary),
                navigationIcon = {
                    IconButton(onClick = { if (showMenu) closeMenu() else openMenu() }) {
                        MorleyIcon(MorleyIcons.Menu, if (showMenu) "Close menu" else "Open menu", MorleyAccent, Modifier.size(26.dp))
                    }
                },
                title = { Text("B&L Morley", fontSize = 22.sp, fontWeight = FontWeight.Black, color = MorleyTextPrimary) },
                actions = {
                    Surface(color = MorleyAccentSoft, border = BorderStroke(1.dp, MorleyBorder), shape = RoundedCornerShape(999.dp)) {
                        Text(AuthManager.accountLabel(context), Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MorleyTextPrimary)
                    }
                    Spacer(Modifier.width(10.dp))
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
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SmartWorkspaceSection()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatusTile("LIVE PRICING", "READY", Modifier.weight(1f))
            StatusTile("ONLINE STATUS", "ONLINE", Modifier.weight(1f))
        }
        NavCard(MorleyIcons.Money, "General Buys / GP", "A / B / C / Luxury buying targets", onGp)
        Spacer(Modifier.height(4.dp))
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
    var privileged by remember { mutableStateOf(AuthManager.canUseAdminMode(context)) }
    var roleRefreshComplete by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runCatching { AuthManager.validAccessToken(context) }
        privileged = AuthManager.canUseAdminMode(context)
        roleRefreshComplete = true
    }

    fun open(feature: String) {
        context.startActivity(Intent(context, MenuFeatureActivity::class.java).putExtra(MenuFeatureActivity.EXTRA_FEATURE, feature))
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Menu", fontSize = 30.sp, fontWeight = FontWeight.Black, color = MorleyTextPrimary)
        MenuSection("Workspace") {
            MenuRow("◷", "Valuations & deals", "Saved valuations and deal history.") { context.startActivity(Intent(context, ValuationHistoryActivity::class.java)) }
            MenuRow("✓", "Test & buy", "Run a hardware checklist and compare the seller ask with Max Buy guidance.") { context.startActivity(Intent(context, TestBuyActivity::class.java)) }
            MenuRow("▣", "Inventory", "Stock, costs and resale values.") { open("inventory") }
            MenuRow("↗", "Sales history", "Revenue and realised profit.") { open("sales") }
            MenuRow("⌗", "Barcode scanner", "Find or add stock quickly.") { open("scanner") }
        }
        MenuSection("Your account") {
            MenuRow("●", "Account & profile", AuthManager.accountLabel(context)) { open("account") }
            MenuRow("⌁", "Privacy & security", "Account privacy and session security.") { open("privacy") }
        }
        if (privileged) {
            MenuSection("Administration") {
                MenuRow("◆", "Admin mode", "Manage support, users, devices and operational controls for ${AuthManager.role(context).replaceFirstChar { it.uppercase() }} accounts.") { context.startActivity(Intent(context, EmbeddedAdminActivity::class.java)) }
            }
        } else if (!roleRefreshComplete) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
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
