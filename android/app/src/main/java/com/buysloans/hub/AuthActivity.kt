package com.buysloans.hub

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
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

private val AuthYellow=Color(0xFFFFD400); private val AuthBg=Color(0xFF111111); private val AuthCard=Color(0xFF222222)

class AuthActivity:ComponentActivity(){
 private val notificationPermissionLauncher=registerForActivityResult(ActivityResultContracts.RequestPermission()){registerDevice()}
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);window.statusBarColor=android.graphics.Color.rgb(17,17,17);window.navigationBarColor=android.graphics.Color.rgb(17,17,17);NotificationHelper.createChannels(this);if(AuthManager.isSignedIn(this)){continueToApp();return};setContent{AuthRoot{continueToApp()}}}
 private fun continueToApp(){requestNotificationsAndRegister();startActivity(Intent(this,DashboardActivity::class.java));finish()}
 private fun requestNotificationsAndRegister(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) else registerDevice()}
 private fun registerDevice(){FirebaseMessaging.getInstance().token.addOnSuccessListener{DeviceRegistrar.register(this,it)}}
}
private enum class AuthMode{SIGN_IN,SIGN_UP,RESET}
@Composable private fun AuthRoot(onSignedIn:()->Unit){
 var mode by remember{mutableStateOf(AuthMode.SIGN_IN)};var email by remember{mutableStateOf("")};var inviteCode by remember{mutableStateOf("")};var password by remember{mutableStateOf("")};var confirmPassword by remember{mutableStateOf("")};var busy by remember{mutableStateOf(false)};var message by remember{mutableStateOf("")};var isError by remember{mutableStateOf(false)};val scope=rememberCoroutineScope();val context=androidx.compose.ui.platform.LocalContext.current
 MaterialTheme(colorScheme=darkColorScheme(primary=AuthYellow,background=AuthBg,surface=AuthCard)){Column(Modifier.fillMaxSize().padding(28.dp),verticalArrangement=Arrangement.Center){
 Text("B&L Morley",fontSize=34.sp,fontWeight=FontWeight.Black);Spacer(Modifier.height(8.dp));Text(when(mode){AuthMode.SIGN_IN->"Sign in";AuthMode.SIGN_UP->"Private sign up";AuthMode.RESET->"Forgot password"},color=AuthYellow,fontSize=26.sp,fontWeight=FontWeight.Bold);if(mode==AuthMode.SIGN_UP)Text("Invite only — contact an administrator for access.",color=Color.LightGray)
 Spacer(Modifier.height(18.dp));OutlinedTextField(email,{email=it},label={Text("Email")},singleLine=true,modifier=Modifier.fillMaxWidth())
 if(mode==AuthMode.SIGN_UP){Spacer(Modifier.height(12.dp));OutlinedTextField(inviteCode,{inviteCode=it.uppercase()},label={Text("Invite code")},singleLine=true,modifier=Modifier.fillMaxWidth())}
 if(mode!=AuthMode.RESET){Spacer(Modifier.height(12.dp));OutlinedTextField(password,{password=it},label={Text("Password")},singleLine=true,visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())}
 if(mode==AuthMode.SIGN_UP){Spacer(Modifier.height(12.dp));OutlinedTextField(confirmPassword,{confirmPassword=it},label={Text("Confirm password")},singleLine=true,visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())}
 if(message.isNotBlank()){Spacer(Modifier.height(10.dp));Text(message,color=if(isError)MaterialTheme.colorScheme.error else Color(0xFF57E389))}
 Spacer(Modifier.height(18.dp));Button(onClick={busy=true;message="";isError=false;scope.launch{runCatching{when(mode){AuthMode.SIGN_IN->AuthManager.signIn(context,email,password);AuthMode.SIGN_UP->{require(password==confirmPassword){"Passwords do not match."};AuthManager.signUp(email,password,inviteCode)};AuthMode.RESET->AuthManager.sendPasswordReset(email)}}.onSuccess{when(mode){AuthMode.SIGN_IN->onSignedIn();AuthMode.SIGN_UP->{message="Account created. You can sign in now.";mode=AuthMode.SIGN_IN;inviteCode="";password="";confirmPassword=""};AuthMode.RESET->{message="Password reset email sent. Check your inbox.";mode=AuthMode.SIGN_IN}}}.onFailure{message=it.message?:"Something went wrong.";isError=true};busy=false}},enabled=!busy&&email.isNotBlank()&&(mode==AuthMode.RESET||password.isNotBlank())&&(mode!=AuthMode.SIGN_UP||inviteCode.isNotBlank()),modifier=Modifier.fillMaxWidth().height(56.dp),colors=ButtonDefaults.buttonColors(containerColor=AuthYellow,contentColor=Color.Black)){Text(if(busy)"Please wait…" else when(mode){AuthMode.SIGN_IN->"Sign in";AuthMode.SIGN_UP->"Create authorised account";AuthMode.RESET->"Send reset email"},fontWeight=FontWeight.Black)}
 Spacer(Modifier.height(12.dp));if(mode!=AuthMode.SIGN_IN)TextButton(onClick={mode=AuthMode.SIGN_IN;message=""},modifier=Modifier.fillMaxWidth()){Text("Back to Sign in")}else{OutlinedButton(onClick={mode=AuthMode.SIGN_UP;message=""},modifier=Modifier.fillMaxWidth()){Text("Sign Up with Invite")};TextButton(onClick={mode=AuthMode.RESET;message=""},modifier=Modifier.fillMaxWidth()){Text("Forgot Password")}}
 Spacer(Modifier.height(10.dp));Text("Private B&L Morley system. Access is limited to authorised accounts.",color=Color.LightGray)
 }}
}
