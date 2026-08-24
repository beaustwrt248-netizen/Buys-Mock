package com.buysloans.hub

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch

private val DashYellow = Color(0xFFFFD400)
private val DashBg = Color(0xFF111111)
private val DashCard = Color(0xFF222222)

class DashboardActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { FirebaseMessaging.getInstance().token.addOnSuccessListener { token -> DeviceRegistrar.register(this, token) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(17,17,17)
        window.navigationBarColor = android.graphics.Color.rgb(17,17,17)
        NotificationHelper.createChannels(this)
        val prefs=getSharedPreferences("app_state",MODE_PRIVATE)
        val previous=prefs.getInt("last_seen_version_code",0)
        val updated=previous>0 && previous<BuildConfig.VERSION_CODE
        prefs.edit().putInt("last_seen_version_code",BuildConfig.VERSION_CODE).apply()
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
private fun RootApp(showUpdatedInitially:Boolean) {
    val activity = androidx.compose.ui.platform.LocalContext.current as DashboardActivity
    var signedIn by remember { mutableStateOf(AuthManager.isSignedIn(activity)) }
    MaterialTheme(colorScheme = darkColorScheme(primary = DashYellow, background = DashBg, surface = DashCard)) {
        if (!signedIn) LoginScreen {
            signedIn = true
            activity.enableNotificationsAndRegister()
        } else DashboardApp(showUpdatedInitially)
    }
}

@Composable
private fun LoginScreen(onSignedIn: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center) {
        Text("B&L Morley", fontSize=34.sp, fontWeight=FontWeight.Black)
        Spacer(Modifier.height(8.dp)); Text("Sign in", color=DashYellow, fontSize=26.sp, fontWeight=FontWeight.Bold)
        Spacer(Modifier.height(22.dp))
        OutlinedTextField(email,{email=it},label={Text("Email")},singleLine=true,modifier=Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(password,{password=it},label={Text("Password")},singleLine=true,visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())
        if(error.isNotBlank()){Spacer(Modifier.height(10.dp));Text(error,color=MaterialTheme.colorScheme.error)}
        Spacer(Modifier.height(18.dp))
        Button(onClick={
            busy=true;error=""
            scope.launch {
                runCatching { AuthManager.signIn(context,email,password) }
                    .onSuccess { onSignedIn() }
                    .onFailure { error=it.message?:"Sign in failed" }
                busy=false
            }
        },enabled=!busy&&email.isNotBlank()&&password.isNotBlank(),modifier=Modifier.fillMaxWidth().height(56.dp),colors=ButtonDefaults.buttonColors(containerColor=DashYellow,contentColor=Color.Black)) { Text(if(busy)"Signing in…" else "Sign in",fontWeight=FontWeight.Black) }
        Spacer(Modifier.height(14.dp));Text("Use your authorised B&L Morley account. Notification permission is requested after a successful sign-in.",color=Color.LightGray)
    }
}

@Composable
private fun DashboardApp(showUpdatedInitially:Boolean=false) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var page by remember { mutableStateOf(Page.Home) }
    var showUpdated by remember { mutableStateOf(showUpdatedInitially) }
    var confirmSignOut by remember { mutableStateOf(false) }
    if(showUpdated) AlertDialog(onDismissRequest={showUpdated=false},title={Text("Update installed")},text={Text("B&L Morley has been updated successfully to v${BuildConfig.VERSION_NAME}.")},confirmButton={Button(onClick={showUpdated=false}){Text("Continue")}})
    if(confirmSignOut) AlertDialog(
        onDismissRequest={confirmSignOut=false},
        title={Text("Sign out?")},
        text={Text("You are signed in as ${AuthManager.email(context)}. You will need to sign in again to use B&L Morley.")},
        dismissButton={TextButton(onClick={confirmSignOut=false}){Text("Cancel")}},
        confirmButton={Button(onClick={
            AuthManager.signOut(context)
            val intent=Intent(context,AuthActivity::class.java).apply{addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)}
            context.startActivity(intent)
        },colors=ButtonDefaults.buttonColors(containerColor=DashYellow,contentColor=Color.Black)){Text("Sign out",fontWeight=FontWeight.Black)}}
    )
    Scaffold(
        containerColor=DashBg,
        bottomBar={NavigationBar(containerColor=Color(0xFF101010)){Page.entries.forEach{p->NavigationBarItem(selected=page==p,onClick={page=p},icon={Text(p.icon,fontSize=20.sp)},label={Text(p.label)},colors=NavigationBarItemDefaults.colors(indicatorColor=DashYellow.copy(alpha=.25f),selectedTextColor=DashYellow))}}},
        floatingActionButton={if(page==Page.More) ExtendedFloatingActionButton(onClick={confirmSignOut=true},containerColor=DashYellow,contentColor=Color.Black,text={Text("Sign out",fontWeight=FontWeight.Black)},icon={Text("↪")})}
    ){pad->Box(Modifier.padding(pad).fillMaxSize()){when(page){Page.Home->ParityHome({page=Page.Laptop},{page=Page.Desktop},{page=Page.GP});Page.Laptop->Laptop();Page.Desktop->Desktop();Page.GP->GPFix();Page.More->MoreHub()}}}
}

@Composable
private fun ParityHome(onLaptop:()->Unit,onDesktop:()->Unit,onGp:()->Unit){
    var quickText by remember{mutableStateOf("")};var ask by remember{mutableStateOf("")};var type by remember{mutableStateOf("Auto detect")}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("B&L Morley",fontSize=25.sp,fontWeight=FontWeight.Black);Surface(color=DashYellow.copy(alpha=.14f),border=BorderStroke(1.dp,DashYellow.copy(alpha=.45f)),shape=RoundedCornerShape(999.dp)){Text("PRODUCTION",Modifier.padding(horizontal=12.dp,vertical=7.dp),color=DashYellow,fontSize=11.sp,fontWeight=FontWeight.Black)}}
        DashboardCard("WELCOME","Buys and Loans Calculator","Live valuation, buying targets and native pricing tools in one workspace.")
        Card(colors=CardDefaults.cardColors(containerColor=DashCard),border=BorderStroke(2.dp,DashYellow),shape=RoundedCornerShape(24.dp),modifier=Modifier.fillMaxWidth()){
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                Text("⚡ Quick Deal Capture",fontSize=24.sp,fontWeight=FontWeight.Black);Text("Paste a seller listing, specs or product details. We'll route it to the right valuation tool.",color=Color.LightGray)
                OutlinedTextField(quickText,{quickText=it},label={Text("Listing / specs / URL")},modifier=Modifier.fillMaxWidth(),minLines=4);OutlinedTextField(ask,{ask=it},label={Text("Seller asking price")},modifier=Modifier.fillMaxWidth(),singleLine=true)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf("Auto detect","Laptop","Desktop").forEach{option->FilterChip(selected=type==option,onClick={type=option},label={Text(option,fontSize=11.sp)})}}
                Button(onClick={val text=quickText.lowercase();if(type=="Laptop"||(type=="Auto detect"&&listOf("laptop","macbook","notebook").any{text.contains(it)}))onLaptop() else onDesktop()},enabled=quickText.isNotBlank(),colors=ButtonDefaults.buttonColors(containerColor=DashYellow,contentColor=Color.Black),shape=RoundedCornerShape(16.dp),modifier=Modifier.fillMaxWidth().height(56.dp)){Text("Analyse Now",fontWeight=FontWeight.Black,fontSize=17.sp)}
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){OutlinedButton(onLaptop,modifier=Modifier.weight(1f)){Text("💻 Laptop")};OutlinedButton(onDesktop,modifier=Modifier.weight(1f)){Text("🖥 Desktop")}}
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){StatusTile("LIVE PRICING","READY",Modifier.weight(1f));StatusTile("ONLINE STATUS","ONLINE",Modifier.weight(1f))}
        NavCard("💻","Laptops / MacBooks","Whole-device Google + eBay AU valuation",onLaptop);NavCard("🖥","Desktops / Gaming PCs","Component-based live pricing",onDesktop);NavCard("💰","General Buys / GP","A / B / C / Luxury buying targets",onGp)
    }
}

@Composable
private fun MoreHub(){
    val context=androidx.compose.ui.platform.LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Text("More",fontSize=27.sp,fontWeight=FontWeight.Black)
        Text("Less-used tools and account options live here to keep the main workspace clean.",color=Color.LightGray)
        NavCard("◷","Valuations & Deals","Search saved valuations, convert quotes into deals, and track bought / sold / passed status."){
            context.startActivity(Intent(context,ValuationHistoryActivity::class.java))
        }
        DashboardCard("ACCOUNT","Signed in",AuthManager.email(context).ifBlank{"Authorised B&L Morley account"})
        DashboardCard("APP VERSION",BuildConfig.VERSION_NAME,"Secure private build • notifications • OTA updates")
        Spacer(Modifier.height(80.dp))
    }
}

@Composable private fun DashboardCard(kicker:String,title:String,body:String){Card(colors=CardDefaults.cardColors(containerColor=DashCard),shape=RoundedCornerShape(24.dp),modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(kicker,color=DashYellow,fontSize=12.sp,fontWeight=FontWeight.Black);Text(title,fontSize=27.sp,fontWeight=FontWeight.Black);Text(body,color=Color.LightGray,lineHeight=22.sp)}}}
@Composable private fun StatusTile(label:String,value:String,modifier:Modifier=Modifier){Card(colors=CardDefaults.cardColors(containerColor=DashCard),shape=RoundedCornerShape(18.dp),modifier=modifier){Column(Modifier.padding(14.dp)){Text(label,color=Color.Gray,fontSize=10.sp,fontWeight=FontWeight.Bold);Text(value,color=Color(0xFF57E389),fontSize=18.sp,fontWeight=FontWeight.Black)}}}
@Composable private fun NavCard(icon:String,title:String,subtitle:String,onClick:()->Unit){Card(onClick=onClick,colors=CardDefaults.cardColors(containerColor=DashCard),shape=RoundedCornerShape(20.dp),modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(icon,fontSize=24.sp);Text(title,fontSize=19.sp,fontWeight=FontWeight.Black);Text(subtitle,color=Color.LightGray,fontSize=13.sp);HorizontalDivider(color=DashYellow,thickness=3.dp)}}}
