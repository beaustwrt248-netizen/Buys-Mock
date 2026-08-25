package com.buysloans.hub

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
import com.google.android.gms.codescanner.GmsBarcodeScanning
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val MFAccent=Color(0xFF16C7FF)
private val MFStrong=Color(0xFF2684FF)
private val MFBg=Color(0xFF030712)
private val MFCard=Color(0xFF0B1528)
private val MFMuted=Color(0xFF8EA6C4)

class MenuFeatureActivity:ComponentActivity(){
    companion object{const val EXTRA_FEATURE="feature"}
    private var notice by mutableStateOf("")

    private val notificationPermission=registerForActivityResult(ActivityResultContracts.RequestPermission()){
        if(it){FirebaseMessaging.getInstance().token.addOnSuccessListener{token->DeviceRegistrar.register(this,token)};notice="Notifications enabled."}
        else notice="Notification permission was not granted."
    }
    private val createBackup=registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->
        if(uri!=null)runCatching{contentResolver.openOutputStream(uri)?.bufferedWriter()?.use{it.write(WorkspaceStore.exportJson(this))}}.onSuccess{notice="Backup exported successfully."}.onFailure{notice="Backup failed: ${it.message}"}
    }
    private val openBackup=registerForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null)runCatching{contentResolver.openInputStream(uri)?.bufferedReader()?.use{WorkspaceStore.importJson(this,it.readText())}}.onSuccess{notice="Backup imported successfully."}.onFailure{notice="Import failed: ${it.message}"}
    }

    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        window.statusBarColor=android.graphics.Color.rgb(3,7,18);window.navigationBarColor=android.graphics.Color.rgb(3,7,18)
        val feature=intent.getStringExtra(EXTRA_FEATURE).orEmpty()
        setContent{MaterialTheme(colorScheme=darkColorScheme(primary=MFAccent,secondary=MFStrong,background=MFBg,surface=MFCard)){FeatureScreen(feature)}}
    }

    private fun openNotificationSettings(){startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,packageName))}
    private fun requestNotifications(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) else {FirebaseMessaging.getInstance().token.addOnSuccessListener{DeviceRegistrar.register(this,it)};notice="Notifications are enabled."}}
    private fun emailIssue(){val body="B&L Morley issue report\n\nVersion: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\nDevice: ${Build.MANUFACTURER} ${Build.MODEL}\nAndroid: ${Build.VERSION.RELEASE}\nSigned in as: ${AuthManager.accountLabel(this)}\n\nWhat happened:\n";val i=Intent(Intent.ACTION_SENDTO,Uri.parse("mailto:")).putExtra(Intent.EXTRA_SUBJECT,"B&L Morley issue - ${BuildConfig.VERSION_NAME}").putExtra(Intent.EXTRA_TEXT,body);runCatching{startActivity(Intent.createChooser(i,"Report B&L Morley issue"))}.onFailure{notice="No email app is available on this device."}}

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable private fun FeatureScreen(feature:String){
        val title=when(feature){"inventory"->"Inventory";"sales"->"Sales History";"scanner"->"Barcode Scanner";"account"->"Account & Profile";"privacy"->"Privacy & Security";"backup"->"Backup & Data";"notifications"->"Notifications";"display"->"Display";"report"->"Report an Issue";"legal"->"Legal & Privacy";"about"->"About B&L Morley";else->"B&L Morley"}
        Scaffold(containerColor=MFBg,topBar={TopAppBar(colors=TopAppBarDefaults.topAppBarColors(containerColor=Color(0xFF050B16),titleContentColor=Color.White),navigationIcon={IconButton(onClick={finish()}){Text("‹",fontSize=34.sp,color=MFAccent)}},title={Text(title,fontWeight=FontWeight.Black)})}){pad->
            Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                if(notice.isNotBlank())Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF0E2B35)),border=BorderStroke(1.dp,MFAccent.copy(alpha=.4f)),shape=RoundedCornerShape(16.dp),modifier=Modifier.fillMaxWidth()){Text(notice,Modifier.padding(14.dp),color=Color.White)}
                when(feature){
                    "inventory"->InventoryFeature()
                    "sales"->SalesFeature()
                    "scanner"->ScannerFeature()
                    "account"->AccountFeature()
                    "privacy"->PrivacyFeature()
                    "backup"->BackupFeature()
                    "notifications"->NotificationsFeature()
                    "display"->DisplayFeature()
                    "report"->ReportFeature()
                    "legal"->LegalFeature()
                    "about"->AboutFeature()
                    else->InfoCard("Unknown option","This menu option is not available in this build.")
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    @Composable private fun InventoryFeature(){
        var name by remember{mutableStateOf("")};var barcode by remember{mutableStateOf("")};var cost by remember{mutableStateOf("")};var resale by remember{mutableStateOf("")};var qty by remember{mutableStateOf("1")};var refresh by remember{mutableIntStateOf(0)}
        val items=remember(refresh){WorkspaceStore.inventory(this)}
        InfoCard("Stock control","Add stock, keep barcode/serial details, track cost and resale value, then mark an item sold directly into Sales History.")
        CardBlock{
            Text("Add inventory item",fontSize=20.sp,fontWeight=FontWeight.Black)
            OutlinedTextField(name,{name=it},label={Text("Item name")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(barcode,{barcode=it},label={Text("Barcode / serial")},modifier=Modifier.fillMaxWidth())
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(cost,{cost=it},label={Text("Cost")},modifier=Modifier.weight(1f));OutlinedTextField(resale,{resale=it},label={Text("Resale")},modifier=Modifier.weight(1f));OutlinedTextField(qty,{qty=it},label={Text("Qty")},modifier=Modifier.weight(.7f))}
            Button(onClick={runCatching{WorkspaceStore.addInventory(this,name,barcode,cost.toDoubleOrNull()?:0.0,resale.toDoubleOrNull()?:0.0,qty.toIntOrNull()?:1)}.onSuccess{name="";barcode="";cost="";resale="";qty="1";refresh++;notice="Item added to inventory."}.onFailure{notice=it.message.orEmpty()}},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=MFStrong)){Text("Add Item",fontWeight=FontWeight.Black)}
        }
        Text("Current stock (${items.sumOf{it.quantity}} units)",fontSize=20.sp,fontWeight=FontWeight.Black)
        if(items.isEmpty())Text("No stock yet.",color=MFMuted) else items.forEach{item->
            CardBlock{
                Text(item.name,fontWeight=FontWeight.Black,fontSize=17.sp);if(item.barcode.isNotBlank())Text("Barcode / serial: ${item.barcode}",color=MFMuted,fontSize=12.sp)
                Text("Qty ${item.quantity}  •  Cost ${money(item.cost)}  •  Resale ${money(item.resale)}",color=Color.White)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={WorkspaceStore.sellOne(this,item.id,item.resale);refresh++;notice="${item.name} moved to Sales History."},modifier=Modifier.weight(1f),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF168E61))){Text("Mark Sold")};OutlinedButton(onClick={WorkspaceStore.deleteInventory(this,item.id);refresh++},modifier=Modifier.weight(1f)){Text("Delete")}}
            }
        }
    }

    @Composable private fun SalesFeature(){var refresh by remember{mutableIntStateOf(0)};val sales=remember(refresh){WorkspaceStore.sales(this)};val revenue=sales.sumOf{it.salePrice*it.quantity};val cost=sales.sumOf{it.cost*it.quantity};InfoCard("Sales summary","Revenue ${money(revenue)}  •  Cost ${money(cost)}  •  Gross profit ${money(revenue-cost)}");if(sales.isEmpty())Text("No completed sales yet.",color=MFMuted) else sales.forEach{s->CardBlock{Text(s.name,fontWeight=FontWeight.Black,fontSize=17.sp);Text("Sold ${date(s.soldAt)}",color=MFMuted,fontSize=12.sp);Text("Sale ${money(s.salePrice)}  •  Cost ${money(s.cost)}  •  Profit ${money(s.salePrice-s.cost)}");OutlinedButton(onClick={WorkspaceStore.deleteSale(this,s.id);refresh++},modifier=Modifier.fillMaxWidth()){Text("Remove sale record")}}}}

    @Composable private fun ScannerFeature(){var code by remember{mutableStateOf("")};var match by remember{mutableStateOf<StockItem?>(null)};InfoCard("Barcode scanner","Scan a barcode with Google Play services or enter a barcode/serial manually, then search current inventory.");Button(onClick={GmsBarcodeScanning.getClient(this).startScan().addOnSuccessListener{b->code=b.rawValue.orEmpty();match=WorkspaceStore.findByBarcode(this,code);notice=if(match==null)"Scanned $code — not currently in inventory." else "Found ${match!!.name}."}.addOnFailureListener{notice="Scanner failed: ${it.message}"}},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=MFStrong)){Text("Scan Barcode",fontWeight=FontWeight.Black)};OutlinedTextField(code,{code=it},label={Text("Barcode / serial")},modifier=Modifier.fillMaxWidth());Button(onClick={match=WorkspaceStore.findByBarcode(this,code);notice=if(match==null)"No matching stock found." else "Found ${match!!.name}."},modifier=Modifier.fillMaxWidth()){Text("Find in Inventory")};match?.let{InfoCard(it.name,"Qty ${it.quantity} • Cost ${money(it.cost)} • Resale ${money(it.resale)}")}}

    @Composable private fun AccountFeature(){val scope=rememberCoroutineScope();var refreshing by remember{mutableStateOf(false)};InfoCard("Signed-in account",AuthManager.accountLabel(this));CardBlock{Text("Email",color=MFMuted,fontSize=12.sp);Text(AuthManager.email(this).ifBlank{"Not available"});Spacer(Modifier.height(4.dp));Text("Display name",color=MFMuted,fontSize=12.sp);Text(AuthManager.displayName(this).ifBlank{"Not set"});Button(onClick={refreshing=true;scope.launch{runCatching{AuthManager.validAccessToken(this@MenuFeatureActivity)}.onSuccess{notice="Account profile refreshed."}.onFailure{notice=it.message.orEmpty()};refreshing=false}},enabled=!refreshing,modifier=Modifier.fillMaxWidth()){Text(if(refreshing)"Refreshing…" else "Refresh Account")}}

    @Composable private fun PrivacyFeature(){val scope=rememberCoroutineScope();var checking by remember{mutableStateOf(false)};InfoCard("Session security","B&L Morley uses an authorised Supabase session. Authentication tokens are not included in workspace backups.");Button(onClick={checking=true;scope.launch{runCatching{AuthManager.validAccessToken(this@MenuFeatureActivity)}.onSuccess{notice="Secure session is valid."}.onFailure{notice=it.message.orEmpty()};checking=false}},enabled=!checking,modifier=Modifier.fillMaxWidth()){Text(if(checking)"Checking…" else "Check Secure Session")};OutlinedButton(onClick={AuthManager.signOut(this);startActivity(Intent(this,AuthActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK));finish()},modifier=Modifier.fillMaxWidth()){Text("Sign out this device")}}

    @Composable private fun BackupFeature(){InfoCard("Workspace backup","Export inventory and sales data to a JSON backup. Login credentials and authentication tokens are deliberately excluded.");Button(onClick={createBackup.launch("BL-Morley-backup-${SimpleDateFormat("yyyy-MM-dd",Locale.US).format(Date())}.json")},modifier=Modifier.fillMaxWidth()){Text("Export Backup")};OutlinedButton(onClick={openBackup.launch(arrayOf("application/json","text/plain"))},modifier=Modifier.fillMaxWidth()){Text("Import Backup")};OutlinedButton(onClick={WorkspaceStore.clearWorkspace(this);notice="Local workspace data cleared."},modifier=Modifier.fillMaxWidth()){Text("Clear Local Workspace Data")}}

    @Composable private fun NotificationsFeature(){InfoCard("Notifications","Use Android's notification permission for update alerts and registered-device notifications.");Button(onClick={requestNotifications()},modifier=Modifier.fillMaxWidth()){Text("Enable Notifications")};OutlinedButton(onClick={openNotificationSettings()},modifier=Modifier.fillMaxWidth()){Text("Open Android Notification Settings")}}

    @Composable private fun DisplayFeature(){val prefs=getSharedPreferences("display_settings",MODE_PRIVATE);var keepAwake by remember{mutableStateOf(prefs.getBoolean("keep_awake",false))};InfoCard("Display preferences","Choose whether B&L Morley should keep the screen awake while you're actively pricing or managing stock.");CardBlock{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column(Modifier.weight(1f)){Text("Keep screen awake",fontWeight=FontWeight.Black);Text("Useful during valuation and stock entry.",color=MFMuted,fontSize=12.sp)};Switch(checked=keepAwake,onCheckedChange={keepAwake=it;prefs.edit().putBoolean("keep_awake",it).apply();if(it)window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);notice=if(it)"Keep-awake enabled." else "Keep-awake disabled."})}}}

    @Composable private fun ReportFeature(){InfoCard("Report an issue","Create a pre-filled issue email containing the app version and device details. Add what happened before sending it.");Button(onClick={emailIssue()},modifier=Modifier.fillMaxWidth()){Text("Create Issue Report")}}
    @Composable private fun LegalFeature(){InfoCard("Privacy","B&L Morley is a private business system for authorised accounts. Workspace backups exclude authentication tokens. Pricing requests may use configured external pricing services to return market evidence.");InfoCard("Local data","Inventory and sales created in the native workspace are stored locally on this device unless you explicitly export them.")}
    @Composable private fun AboutFeature(){InfoCard("B&L Morley","Buys & Loans Hub\nVersion ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n\nNative valuation, inventory, barcode, sales and account workspace.")}

    @Composable private fun CardBlock(content:@Composable ColumnScope.()->Unit){Card(colors=CardDefaults.cardColors(containerColor=MFCard),border=BorderStroke(1.dp,MFAccent.copy(alpha=.18f)),shape=RoundedCornerShape(20.dp),modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(15.dp),verticalArrangement=Arrangement.spacedBy(10.dp),content=content)}}
    @Composable private fun InfoCard(title:String,body:String){CardBlock{Text(title,fontSize=18.sp,fontWeight=FontWeight.Black);Text(body,color=MFMuted,lineHeight=20.sp)}}
    private fun money(v:Double)=NumberFormat.getCurrencyInstance(Locale("en","AU")).apply{maximumFractionDigits=0}.format(v)
    private fun date(v:Long)=SimpleDateFormat("dd MMM yyyy, h:mm a",Locale.getDefault()).format(Date(v))
}
