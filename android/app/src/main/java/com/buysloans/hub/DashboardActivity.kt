package com.buysloans.hub

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch

private val DashAccent = Color(0xFF16C7FF)
private val DashAccentStrong = Color(0xFF2684FF)
private val DashBg = Color(0xFF030712)
private val DashCard = Color(0xFF0B1528)
private val DashMuted = Color(0xFF8EA6C4)

class DashboardActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { FirebaseMessaging.getInstance().token.addOnSuccessListener { token -> DeviceRegistrar.register(this, token) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(3,7,18)
        window.navigationBarColor = android.graphics.Color.rgb(3,7,18)
        if(getSharedPreferences("display_settings",MODE_PRIVATE).getBoolean("keep_awake",false)) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        NotificationHelper.createChannels(this)
        if (!AuthManager.isSignedIn(this)) {
            startActivity(Intent(this, AuthActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) })
            finish(); return
        }
        val prefs=getSharedPreferences("app_state",MODE_PRIVATE)
        val previous=prefs.getInt("last_seen_version_code",0)
        val updated=previous>0 && previous<BuildConfig.VERSION_CODE
        prefs.edit().putInt("last_seen_version_code",BuildConfig.VERSION_CODE).apply()
        setContent { RootApp(updated) }
    }

    fun enableNotificationsAndRegister() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else FirebaseMessaging.getInstance().token.addOnSuccessListener { token -> DeviceRegistrar.register(this, token) }
    }
}

@Composable
private fun RootApp(showUpdatedInitially:Boolean) {
    MaterialTheme(colorScheme = darkColorScheme(primary = DashAccent, secondary = DashAccentStrong, background = DashBg, surface = DashCard)) {
        DashboardApp(showUpdatedInitially)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardApp(showUpdatedInitially:Boolean=false) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var page by remember { mutableStateOf(Page.Home) }
    var previousPage by remember { mutableStateOf(Page.Home) }
    var showMenu by remember { mutableStateOf(false) }
    var showUpdated by remember { mutableStateOf(showUpdatedInitially) }
    var confirmSignOut by remember { mutableStateOf(false) }

    fun openMenu(){ if(!showMenu) previousPage=page; showMenu=true }
    fun closeMenu(){ showMenu=false; page=previousPage }

    if(showUpdated) AlertDialog(onDismissRequest={showUpdated=false},title={Text("Update installed")},text={Text("B&L Morley has been updated successfully to v${BuildConfig.VERSION_NAME}.")},confirmButton={Button(onClick={showUpdated=false}){Text("Continue")}})
    if(confirmSignOut) AlertDialog(
        onDismissRequest={confirmSignOut=false},
        title={Text("Sign out?")},
        text={Text("You are signed in as ${AuthManager.accountLabel(context)}. You will need to sign in again to use B&L Morley.")},
        dismissButton={TextButton(onClick={confirmSignOut=false}){Text("Cancel")}},
        confirmButton={Button(onClick={
            AuthManager.signOut(context)
            context.startActivity(Intent(context,AuthActivity::class.java).apply{addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)})
        },colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF8E3040),contentColor=Color.White)){Text("Sign out",fontWeight=FontWeight.Black)}}
    )

    Scaffold(
        containerColor=DashBg,
        topBar={
            TopAppBar(
                colors=TopAppBarDefaults.topAppBarColors(containerColor=Color(0xFF050B16),titleContentColor=Color.White),
                navigationIcon={
                    IconButton(onClick={ if(showMenu) closeMenu() else openMenu() }){
                        Text(if(showMenu)"×" else "☰",fontSize=28.sp,color=DashAccent,fontWeight=FontWeight.Black)
                    }
                },
                title={Text("Buys & Loans Hub",fontSize=21.sp,fontWeight=FontWeight.Black)},
                actions={
                    Surface(color=DashAccent.copy(alpha=.08f),border=BorderStroke(1.dp,DashAccent.copy(alpha=.35f)),shape=RoundedCornerShape(999.dp)){
                        Text(AuthManager.accountLabel(context),Modifier.padding(horizontal=10.dp,vertical=7.dp),fontSize=10.sp,fontWeight=FontWeight.Bold,color=Color.White)
                    }
                    Spacer(Modifier.width(8.dp))
                }
            )
        },
        bottomBar={
            if(!showMenu) NavigationBar(containerColor=Color(0xFF07101F)){
                Page.entries.filter{it!=Page.More}.forEach{p->
                    NavigationBarItem(selected=page==p,onClick={page=p},icon={Text(p.icon,fontSize=20.sp)},label={Text(p.label)},colors=NavigationBarItemDefaults.colors(indicatorColor=DashAccent.copy(alpha=.18f),selectedIconColor=DashAccent,selectedTextColor=DashAccent,unselectedTextColor=DashMuted))
                }
            }
        }
    ){pad->
        Box(Modifier.padding(pad).fillMaxSize()){
            if(showMenu) MoreHub(onSignOut={confirmSignOut=true})
            else when(page){
                Page.Home->ParityHome({page=Page.Laptop},{page=Page.Desktop},{page=Page.GP})
                Page.Laptop->Laptop()
                Page.Desktop->Desktop()
                Page.GP->GPFix()
                Page.More->ParityHome({page=Page.Laptop},{page=Page.Desktop},{page=Page.GP})
            }
        }
    }
}

@Composable
private fun ParityHome(onLaptop:()->Unit,onDesktop:()->Unit,onGp:()->Unit){
    var quickText by remember{mutableStateOf("")};var ask by remember{mutableStateOf("")};var type by remember{mutableStateOf("Auto detect")}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        DashboardCard("WELCOME","Buys and Loans Calculator","Live valuation, buying targets and native pricing tools in one workspace.")
        Card(colors=CardDefaults.cardColors(containerColor=DashCard),border=BorderStroke(1.dp,DashAccent.copy(alpha=.55f)),shape=RoundedCornerShape(24.dp),modifier=Modifier.fillMaxWidth()){
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                Text("⚡ Quick Deal Capture",fontSize=24.sp,fontWeight=FontWeight.Black)
                Text("Paste a seller listing, specs or product details. We'll route it to the right valuation tool.",color=Color.LightGray)
                OutlinedTextField(quickText,{quickText=it},label={Text("Listing / specs / URL")},modifier=Modifier.fillMaxWidth(),minLines=4)
                OutlinedTextField(ask,{ask=it},label={Text("Seller asking price")},modifier=Modifier.fillMaxWidth(),singleLine=true)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("Auto detect","Laptop","Desktop").forEach{option->FilterChip(selected=type==option,onClick={type=option},label={Text(option,fontSize=11.sp)})}}
                Button(onClick={val text=quickText.lowercase();if(type=="Laptop"||(type=="Auto detect"&&listOf("laptop","macbook","notebook").any{text.contains(it)}))onLaptop() else onDesktop()},enabled=quickText.isNotBlank(),colors=ButtonDefaults.buttonColors(containerColor=DashAccentStrong,contentColor=Color.White),shape=RoundedCornerShape(16.dp),modifier=Modifier.fillMaxWidth().height(56.dp)){Text("Analyse Now",fontWeight=FontWeight.Black,fontSize=17.sp)}
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){OutlinedButton(onLaptop,modifier=Modifier.weight(1f)){Text("💻 Laptop")};OutlinedButton(onDesktop,modifier=Modifier.weight(1f)){Text("🖥 Desktop")}}
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){StatusTile("LIVE PRICING","READY",Modifier.weight(1f));StatusTile("ONLINE STATUS","ONLINE",Modifier.weight(1f))}
        NavCard("💻","Laptops / MacBooks","Whole-device Google + eBay AU valuation",onLaptop)
        NavCard("🖥","Desktops / Gaming PCs","Component-based live pricing",onDesktop)
        NavCard("💰","General Buys / GP","A / B / C / Luxury buying targets",onGp)
    }
}

@Composable
private fun MoreHub(onSignOut:()->Unit){
    val context=androidx.compose.ui.platform.LocalContext.current
    val scope=rememberCoroutineScope()
    var checking by remember{mutableStateOf(false)}
    var updateStatus by remember{mutableStateOf("Check app version and update status.")}
    var availableUpdate by remember{mutableStateOf<AppUpdate?>(null)}

    fun open(feature:String){context.startActivity(Intent(context,MenuFeatureActivity::class.java).putExtra(MenuFeatureActivity.EXTRA_FEATURE,feature))}

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=14.dp,vertical=10.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
        Text("Menu",fontSize=30.sp,fontWeight=FontWeight.Black)
        MenuSection("Workspace"){
            MenuRow("◷","Valuations & Deals","Saved valuations and deal history."){context.startActivity(Intent(context,ValuationHistoryActivity::class.java))}
            MenuRow("▣","Inventory","Stock, costs and resale values."){open("inventory")}
            MenuRow("↗","Sales History","Revenue and realised profit."){open("sales")}
            MenuRow("⌗","Barcode Scanner","Find or add stock quickly."){open("scanner")}
        }
        MenuSection("Your account"){
            MenuRow("●","Account & Profile",AuthManager.accountLabel(context)){open("account")}
            MenuRow("⌁","Privacy & Security","Account privacy and session security."){open("privacy")}
        }
        MenuSection("Data & preferences"){
            MenuRow("☁","Backup & Data","Export, import and local app data."){open("backup")}
            MenuRow("♢","Notifications","Update and app notification preferences."){open("notifications")}
            MenuRow("◐","Display","Interface and display preferences."){open("display")}
        }
        MenuSection("App"){
            MenuRow("↻","Updates",updateStatus){
                checking=true;availableUpdate=null;updateStatus="Checking for updates…"
                scope.launch{
                    runCatching{UpdateManager.check()}.onSuccess{u->availableUpdate=u;updateStatus=if(u==null)"You're up to date on v${BuildConfig.VERSION_NAME}." else "${u.versionName} is available. ${u.notes}"}.onFailure{updateStatus="Update check failed: ${it.message?:"network error"}"};checking=false
                }
            }
            if(checking) LinearProgressIndicator(modifier=Modifier.fillMaxWidth())
            availableUpdate?.let{u->OutlinedButton(onClick={if(Build.VERSION.SDK_INT>=26&&!context.packageManager.canRequestPackageInstalls())UpdateManager.openInstallerPermission(context) else UpdateManager.openDownload(context,u)},modifier=Modifier.fillMaxWidth()){Text("Download ${u.versionName}")}}
            MenuRow("⚑","Report an Issue","Record an app problem for follow-up."){open("report")}
            MenuRow("§","Legal & Privacy","Privacy and application information."){open("legal")}
            MenuRow("ⓘ","About B&L Morley","Version ${BuildConfig.VERSION_NAME}"){open("about")}
        }
        Button(onClick=onSignOut,modifier=Modifier.fillMaxWidth().height(54.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF57202A),contentColor=Color(0xFFFFC0C8)),shape=RoundedCornerShape(16.dp)){Text("↪  Sign out",fontWeight=FontWeight.Black)}
        Spacer(Modifier.height(8.dp))
    }
}

@Composable private fun MenuSection(title:String,content:@Composable ColumnScope.()->Unit){
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
        Text(title,color=DashMuted,fontSize=12.sp,fontWeight=FontWeight.Bold)
        Card(colors=CardDefaults.cardColors(containerColor=DashCard),border=BorderStroke(1.dp,DashAccent.copy(alpha=.18f)),shape=RoundedCornerShape(20.dp),modifier=Modifier.fillMaxWidth()){
            Column(content=content)
        }
    }
}

@Composable private fun MenuRow(icon:String,title:String,subtitle:String,onClick:()->Unit){
    Surface(onClick=onClick,color=Color.Transparent,modifier=Modifier.fillMaxWidth()){
        Row(Modifier.fillMaxWidth().padding(horizontal=15.dp,vertical=13.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)){
            Surface(color=DashAccentStrong.copy(alpha=.16f),shape=RoundedCornerShape(12.dp),border=BorderStroke(1.dp,DashAccent.copy(alpha=.18f))){Text(icon,Modifier.padding(10.dp),color=DashAccent,fontSize=18.sp,fontWeight=FontWeight.Black)}
            Column(Modifier.weight(1f)){Text(title,fontWeight=FontWeight.Black,fontSize=15.sp);Text(subtitle,color=DashMuted,fontSize=12.sp,lineHeight=17.sp)}
            Text("›",color=DashAccent,fontSize=24.sp,fontWeight=FontWeight.Black)
        }
    }
}

@Composable private fun DashboardCard(kicker:String,title:String,body:String){Card(colors=CardDefaults.cardColors(containerColor=DashCard),shape=RoundedCornerShape(24.dp),modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(kicker,color=DashAccent,fontSize=12.sp,fontWeight=FontWeight.Black);Text(title,fontSize=27.sp,fontWeight=FontWeight.Black);Text(body,color=Color.LightGray,lineHeight=22.sp)}}}
@Composable private fun StatusTile(label:String,value:String,modifier:Modifier=Modifier){Card(colors=CardDefaults.cardColors(containerColor=DashCard),shape=RoundedCornerShape(18.dp),modifier=modifier){Column(Modifier.padding(14.dp)){Text(label,color=Color.Gray,fontSize=10.sp,fontWeight=FontWeight.Bold);Text(value,color=Color(0xFF57E389),fontSize=18.sp,fontWeight=FontWeight.Black)}}}
@Composable private fun NavCard(icon:String,title:String,subtitle:String,onClick:()->Unit){Card(onClick=onClick,colors=CardDefaults.cardColors(containerColor=DashCard),border=BorderStroke(1.dp,DashAccent.copy(alpha=.18f)),shape=RoundedCornerShape(20.dp),modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(icon,fontSize=24.sp);Text(title,fontSize=19.sp,fontWeight=FontWeight.Black);Text(subtitle,color=Color.LightGray,fontSize=13.sp);HorizontalDivider(color=DashAccent.copy(alpha=.70f),thickness=2.dp)}}}
