package com.buysloans.hub

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch

private val AuthYellow=Color(0xFFFFD400)
private val AuthBg=Color(0xFF111111)
private val AuthCard=Color(0xFF222222)
private const val TURNSTILE_PAGE="https://beaustwrt248-netizen.github.io/Buys-Mock/admin/turnstile.html"
private const val LOGIN_VIDEO_RESOURCE="morley_buys_login_bg_app"

class AuthActivity:ComponentActivity(){
 private val notificationPermissionLauncher=registerForActivityResult(ActivityResultContracts.RequestPermission()){registerDevice()}
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState)
  window.statusBarColor=android.graphics.Color.BLACK
  window.navigationBarColor=android.graphics.Color.BLACK
  NotificationHelper.createChannels(this)
  if(AuthManager.isSignedIn(this)){continueToApp();return}
  setContent{AuthRoot{continueToApp()}}
 }
 private fun continueToApp(){requestNotificationsAndRegister();startActivity(Intent(this,DashboardActivity::class.java));finish()}
 private fun requestNotificationsAndRegister(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) else registerDevice()}
 private fun registerDevice(){FirebaseMessaging.getInstance().token.addOnSuccessListener{DeviceRegistrar.register(this,it)}}
}

private enum class AuthMode{SIGN_IN,SIGN_UP,RESET}

@Composable
private fun LoginVideoBackground(){
 val context=androidx.compose.ui.platform.LocalContext.current
 AndroidView(
  modifier=Modifier.fillMaxSize(),
  factory={ctx->
   VideoView(ctx).apply{
    setBackgroundColor(android.graphics.Color.BLACK)
    val resId=ctx.resources.getIdentifier(LOGIN_VIDEO_RESOURCE,"raw",ctx.packageName)
    if(resId!=0){
     setVideoURI(Uri.parse("android.resource://${ctx.packageName}/$resId"))
     setOnPreparedListener{player->
      player.isLooping=true
      player.setVolume(0f,0f)
      if(Build.VERSION.SDK_INT>=16)player.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
      start()
     }
     setOnCompletionListener{start()}
     setOnErrorListener{_,_,_->true}
    }
   }
  },
  update={video->if(!video.isPlaying)runCatching{video.start()}},
  onRelease={video->runCatching{video.stopPlayback()}}
 )
}

private class TurnstileBridge(
 private val onToken:(String)->Unit,
 private val onExpired:()->Unit,
 private val onError:(String)->Unit
){
 private val main=Handler(Looper.getMainLooper())
 @JavascriptInterface fun onToken(token:String){main.post{onToken.invoke(token)}}
 @JavascriptInterface fun onExpired(ignored:String){main.post{onExpired.invoke()}}
 @JavascriptInterface fun onError(code:String){main.post{onError.invoke(code)}}
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun TurnstileChallenge(refreshKey:Int,onToken:(String)->Unit,onExpired:()->Unit,onError:(String)->Unit){
 key(refreshKey){
  AndroidView(
   modifier=Modifier.fillMaxWidth().height(118.dp),
   factory={ctx->
    WebView(ctx).apply{
     setBackgroundColor(android.graphics.Color.TRANSPARENT)
     settings.javaScriptEnabled=true
     settings.domStorageEnabled=false
     settings.allowFileAccess=false
     settings.allowContentAccess=false
     settings.databaseEnabled=false
     settings.setSupportMultipleWindows(false)
     if(Build.VERSION.SDK_INT>=26)settings.safeBrowsingEnabled=true
     addJavascriptInterface(TurnstileBridge(onToken,onExpired,onError),"AndroidBridge")
     webViewClient=object:WebViewClient(){}
     loadUrl(TURNSTILE_PAGE)
    }
   }
  )
 }
}

@Composable private fun AuthRoot(onSignedIn:()->Unit){
 var mode by remember{mutableStateOf(AuthMode.SIGN_IN)}
 var email by remember{mutableStateOf("")}
 var inviteCode by remember{mutableStateOf("")}
 var password by remember{mutableStateOf("")}
 var confirmPassword by remember{mutableStateOf("")}
 var captchaToken by remember{mutableStateOf("")}
 var captchaRefresh by remember{mutableIntStateOf(0)}
 var busy by remember{mutableStateOf(false)}
 var message by remember{mutableStateOf("")}
 var isError by remember{mutableStateOf(false)}
 val scope=rememberCoroutineScope()
 val context=androidx.compose.ui.platform.LocalContext.current

 fun resetCaptcha(){captchaToken="";captchaRefresh++}
 fun changeMode(next:AuthMode){mode=next;message="";isError=false;resetCaptcha()}

 MaterialTheme(colorScheme=darkColorScheme(primary=AuthYellow,background=AuthBg,surface=AuthCard)){
  Box(Modifier.fillMaxSize().background(AuthBg)){
   LoginVideoBackground()
   Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=0.58f)))
   Column(Modifier.fillMaxSize().padding(28.dp),verticalArrangement=Arrangement.Center){
    Text("B&L Morley",fontSize=34.sp,fontWeight=FontWeight.Black,color=Color.White)
    Spacer(Modifier.height(8.dp))
    Text(when(mode){AuthMode.SIGN_IN->"Sign in";AuthMode.SIGN_UP->"Private sign up";AuthMode.RESET->"Forgot password"},color=AuthYellow,fontSize=26.sp,fontWeight=FontWeight.Bold)
    if(mode==AuthMode.SIGN_UP)Text("Invite only — contact an administrator for access.",color=Color.LightGray)
    Spacer(Modifier.height(18.dp))
    OutlinedTextField(email,{email=it},label={Text("Email")},singleLine=true,modifier=Modifier.fillMaxWidth())
    if(mode==AuthMode.SIGN_UP){Spacer(Modifier.height(12.dp));OutlinedTextField(inviteCode,{inviteCode=it.uppercase()},label={Text("Invite code")},singleLine=true,modifier=Modifier.fillMaxWidth())}
    if(mode!=AuthMode.RESET){Spacer(Modifier.height(12.dp));OutlinedTextField(password,{password=it},label={Text("Password")},singleLine=true,visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())}
    if(mode==AuthMode.SIGN_UP){Spacer(Modifier.height(12.dp));OutlinedTextField(confirmPassword,{confirmPassword=it},label={Text("Confirm password")},singleLine=true,visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth())}

    Spacer(Modifier.height(10.dp))
    TurnstileChallenge(
     refreshKey=captchaRefresh,
     onToken={captchaToken=it;message="Security check complete.";isError=false},
     onExpired={captchaToken="";message="Security check expired. Complete it again.";isError=true},
     onError={captchaToken="";message="Security check failed. Please retry.";isError=true}
    )

    if(message.isNotBlank()){Spacer(Modifier.height(8.dp));Text(message,color=if(isError)MaterialTheme.colorScheme.error else Color(0xFF57E389))}
    Spacer(Modifier.height(14.dp))
    Button(
     onClick={
      busy=true;message="";isError=false
      val token=captchaToken
      scope.launch{
       runCatching{
        when(mode){
         AuthMode.SIGN_IN->AuthManager.signIn(context,email,password,token)
         AuthMode.SIGN_UP->{require(password==confirmPassword){"Passwords do not match."};AuthManager.signUp(email,password,inviteCode,token)}
         AuthMode.RESET->AuthManager.sendPasswordReset(email,token)
        }
       }.onSuccess{
        when(mode){
         AuthMode.SIGN_IN->onSignedIn()
         AuthMode.SIGN_UP->{message="Account created. You can sign in now.";mode=AuthMode.SIGN_IN;inviteCode="";password="";confirmPassword="";resetCaptcha()}
         AuthMode.RESET->{message="Password reset email sent. Check your inbox.";mode=AuthMode.SIGN_IN;resetCaptcha()}
        }
       }.onFailure{message=it.message?:"Something went wrong.";isError=true;resetCaptcha()}
       busy=false
      }
     },
     enabled=!busy&&captchaToken.isNotBlank()&&email.isNotBlank()&&(mode==AuthMode.RESET||password.isNotBlank())&&(mode!=AuthMode.SIGN_UP||inviteCode.isNotBlank()),
     modifier=Modifier.fillMaxWidth().height(56.dp),
     colors=ButtonDefaults.buttonColors(containerColor=AuthYellow,contentColor=Color.Black)
    ){Text(if(busy)"Please wait…" else when(mode){AuthMode.SIGN_IN->"Sign in";AuthMode.SIGN_UP->"Create authorised account";AuthMode.RESET->"Send reset email"},fontWeight=FontWeight.Black)}

    Spacer(Modifier.height(12.dp))
    if(mode!=AuthMode.SIGN_IN)TextButton(onClick={changeMode(AuthMode.SIGN_IN)},modifier=Modifier.fillMaxWidth()){Text("Back to Sign in")}
    else{
     OutlinedButton(onClick={changeMode(AuthMode.SIGN_UP)},modifier=Modifier.fillMaxWidth()){Text("Sign Up with Invite")}
     TextButton(onClick={changeMode(AuthMode.RESET)},modifier=Modifier.fillMaxWidth()){Text("Forgot Password")}
    }
    Spacer(Modifier.height(8.dp))
    Text("Private B&L Morley system. Access is limited to authorised accounts.",color=Color.LightGray)
   }
  }
 }
}
