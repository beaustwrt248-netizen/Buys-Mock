package com.buysloans.hub

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Surface
import android.view.TextureView
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch

private val AuthAccent=Color(0xFF12C9FF)
private val AuthPrimary=Color(0xFF2F7CFF)
private val AuthBg=Color(0xFF030712)
private val AuthCard=Color(0xFF07172C)
private const val TURNSTILE_PAGE="https://buyshub.me/admin/turnstile.html"
private const val LOGIN_VIDEO_RESOURCE="morley_buys_login_bg_app"
private const val LOGIN_PREFS="morley_login_preferences"
private const val PREF_REMEMBER_ME="remember_me"
private const val PREF_REMEMBERED_EMAIL="remembered_email"

class AuthActivity:ComponentActivity(){
 private val notificationPermissionLauncher=registerForActivityResult(ActivityResultContracts.RequestPermission()){registerDevice()}
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState)
  WindowCompat.setDecorFitsSystemWindows(window,false)
  window.statusBarColor=android.graphics.Color.TRANSPARENT
  window.navigationBarColor=android.graphics.Color.TRANSPARENT
  NotificationHelper.createChannels(this)
  val rememberMe=getSharedPreferences(LOGIN_PREFS,Context.MODE_PRIVATE).getBoolean(PREF_REMEMBER_ME,false)
  if(!rememberMe && AuthManager.isSignedIn(this)) AuthManager.signOut(this)
  if(AuthManager.isSignedIn(this)){
   setContent{SessionCheckScreen()}
   lifecycleScope.launch{
    runCatching{AuthManager.validAccessToken(this@AuthActivity)}
     .onSuccess{continueToApp()}
     .onFailure{AuthManager.signOut(this@AuthActivity);setContent{AuthRoot{continueToApp()}}}
   }
   return
  }
  setContent{AuthRoot{continueToApp()}}
 }
 private fun continueToApp(){requestNotificationsAndRegister();startActivity(Intent(this,DashboardActivity::class.java));finish()}
 private fun requestNotificationsAndRegister(){if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) else registerDevice()}
 private fun registerDevice(){FirebaseMessaging.getInstance().token.addOnSuccessListener{DeviceRegistrar.register(this,it)}}
}

@Composable private fun SessionCheckScreen(){MaterialTheme(colorScheme=darkColorScheme(primary=AuthPrimary,secondary=AuthAccent,background=AuthBg,surface=AuthCard)){Surface(color=AuthBg,modifier=Modifier.fillMaxSize()){Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(28.dp),verticalArrangement=Arrangement.Center){CircularProgressIndicator(color=AuthAccent);Spacer(Modifier.height(18.dp));Text("B&L Morley",fontSize=30.sp,fontWeight=FontWeight.Black);Spacer(Modifier.height(6.dp));Text("Verifying your secure session…",color=Color(0xFFA7BAD3))}}}}

private enum class AuthMode{SIGN_IN,SIGN_UP,RESET}

private class LoginVideoTextureView(context:Context):TextureView(context),TextureView.SurfaceTextureListener{
 private var player:MediaPlayer?=null
 private var videoWidth=0
 private var videoHeight=0
 init{surfaceTextureListener=this;isOpaque=true}
 private fun prepare(surfaceTexture:SurfaceTexture){
  releasePlayer();val resId=resources.getIdentifier(LOGIN_VIDEO_RESOURCE,"raw",context.packageName);if(resId==0)return
  val owner=this@LoginVideoTextureView;val surface=Surface(surfaceTexture);val mediaPlayer=MediaPlayer();player=mediaPlayer
  mediaPlayer.setSurface(surface);surface.release();mediaPlayer.setDataSource(context,Uri.parse("android.resource://${context.packageName}/$resId"));mediaPlayer.isLooping=true;mediaPlayer.setVolume(0f,0f)
  mediaPlayer.setOnVideoSizeChangedListener{_,w,h->owner.videoWidth=w;owner.videoHeight=h;owner.applyCenterCrop()}
  mediaPlayer.setOnPreparedListener{mp->owner.videoWidth=mp.videoWidth;owner.videoHeight=mp.videoHeight;owner.applyCenterCrop();mp.start()}
  mediaPlayer.setOnErrorListener{mp,_,_->runCatching{mp.reset()};true};mediaPlayer.prepareAsync()
 }
 private fun applyCenterCrop(){if(width<=0||height<=0||videoWidth<=0||videoHeight<=0)return;val viewW=width.toFloat();val viewH=height.toFloat();val videoW=videoWidth.toFloat();val videoH=videoHeight.toFloat();val coverScale=maxOf(viewW/videoW,viewH/videoH);val scaledW=videoW*coverScale;val scaledH=videoH*coverScale;val matrix=Matrix();matrix.setScale(scaledW/viewW,scaledH/viewH,viewW/2f,viewH/2f);setTransform(matrix)}
 override fun onSizeChanged(w:Int,h:Int,oldw:Int,oldh:Int){super.onSizeChanged(w,h,oldw,oldh);applyCenterCrop()}
 override fun onSurfaceTextureAvailable(surface:SurfaceTexture,width:Int,height:Int){prepare(surface)}
 override fun onSurfaceTextureSizeChanged(surface:SurfaceTexture,width:Int,height:Int){applyCenterCrop()}
 override fun onSurfaceTextureDestroyed(surface:SurfaceTexture):Boolean{releasePlayer();return true}
 override fun onSurfaceTextureUpdated(surface:SurfaceTexture){}
 fun resumePlayback(){player?.let{if(!it.isPlaying)runCatching{it.start()}}}
 fun releasePlayer(){player?.let{runCatching{it.stop()};runCatching{it.release()}};player=null}
}

@Composable private fun LoginVideoBackground(){AndroidView(modifier=Modifier.fillMaxSize(),factory={ctx->LoginVideoTextureView(ctx)},update={it.resumePlayback()},onRelease={it.releasePlayer()})}

private class TurnstileBridge(private val onToken:(String)->Unit,private val onExpired:()->Unit,private val onError:(String)->Unit){private val main=Handler(Looper.getMainLooper());@JavascriptInterface fun onToken(token:String){main.post{onToken.invoke(token)}};@JavascriptInterface fun onExpired(ignored:String){main.post{onExpired.invoke()}};@JavascriptInterface fun onError(code:String){main.post{onError.invoke(code)}}}

@SuppressLint("SetJavaScriptEnabled")
@Composable private fun TurnstileChallenge(refreshKey:Int,onToken:(String)->Unit,onExpired:()->Unit,onError:(String)->Unit){key(refreshKey){AndroidView(modifier=Modifier.fillMaxWidth().height(118.dp),factory={ctx->WebView(ctx).apply{setBackgroundColor(android.graphics.Color.TRANSPARENT);settings.javaScriptEnabled=true;settings.domStorageEnabled=false;settings.allowFileAccess=false;settings.allowContentAccess=false;settings.databaseEnabled=false;settings.setSupportMultipleWindows(false);if(Build.VERSION.SDK_INT>=26)settings.safeBrowsingEnabled=true;addJavascriptInterface(TurnstileBridge(onToken,onExpired,onError),"AndroidBridge");webViewClient=object:WebViewClient(){};loadUrl(TURNSTILE_PAGE)}})}}

@Composable private fun AuthRoot(onSignedIn:()->Unit){
 val context=LocalContext.current
 val loginPrefs=remember{context.getSharedPreferences(LOGIN_PREFS,Context.MODE_PRIVATE)}
 var mode by remember{mutableStateOf(AuthMode.SIGN_IN)}
 var rememberMe by remember{mutableStateOf(loginPrefs.getBoolean(PREF_REMEMBER_ME,false))}
 var email by remember{mutableStateOf(if(rememberMe)loginPrefs.getString(PREF_REMEMBERED_EMAIL,"").orEmpty() else "")}
 var inviteCode by remember{mutableStateOf("")};var password by remember{mutableStateOf("")};var confirmPassword by remember{mutableStateOf("")}
 var captchaToken by remember{mutableStateOf("")};var captchaRefresh by remember{mutableIntStateOf(0)};var busy by remember{mutableStateOf(false)};var message by remember{mutableStateOf("")};var isError by remember{mutableStateOf(false)}
 val scope=rememberCoroutineScope()
 fun resetCaptcha(){captchaToken="";captchaRefresh++};fun changeMode(next:AuthMode){mode=next;message="";isError=false;resetCaptcha()}
 fun persistRememberChoice(){loginPrefs.edit().apply{putBoolean(PREF_REMEMBER_ME,rememberMe);if(rememberMe)putString(PREF_REMEMBERED_EMAIL,email.trim().lowercase()) else remove(PREF_REMEMBERED_EMAIL)}.apply()}
 MaterialTheme(colorScheme=darkColorScheme(primary=AuthPrimary,secondary=AuthAccent,background=AuthBg,surface=AuthCard)){
  Box(Modifier.fillMaxSize().background(AuthBg)){
   LoginVideoBackground();Box(Modifier.fillMaxSize().background(Color(0xFF020611).copy(alpha=0.58f)))
   Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).background(Color(0xFF030712).copy(alpha=0.06f)).padding(28.dp),verticalArrangement=Arrangement.Center){
    Text("B&L Morley",fontSize=34.sp,fontWeight=FontWeight.Black,color=Color.White);Spacer(Modifier.height(8.dp))
    Text(when(mode){AuthMode.SIGN_IN->"Sign in";AuthMode.SIGN_UP->"Private sign up";AuthMode.RESET->"Forgot password"},color=AuthAccent,fontSize=26.sp,fontWeight=FontWeight.Bold)
    if(mode==AuthMode.SIGN_UP)Text("Invite only — contact an administrator for access.",color=Color(0xFFA7BAD3))
    Spacer(Modifier.height(18.dp));OutlinedTextField(email,{email=it},label={Text("Email")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Email,imeAction=ImeAction.Next),modifier=Modifier.fillMaxWidth())
    if(mode==AuthMode.SIGN_UP){Spacer(Modifier.height(12.dp));OutlinedTextField(inviteCode,{inviteCode=it.uppercase()},label={Text("Invite code")},singleLine=true,modifier=Modifier.fillMaxWidth())}
    if(mode!=AuthMode.RESET){Spacer(Modifier.height(12.dp));OutlinedTextField(password,{password=it},label={Text("Password")},singleLine=true,visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password,imeAction=if(mode==AuthMode.SIGN_IN)ImeAction.Done else ImeAction.Next),modifier=Modifier.fillMaxWidth())}
    if(mode==AuthMode.SIGN_UP){Spacer(Modifier.height(12.dp));OutlinedTextField(confirmPassword,{confirmPassword=it},label={Text("Confirm password")},singleLine=true,visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password,imeAction=ImeAction.Done),modifier=Modifier.fillMaxWidth())}
    if(mode==AuthMode.SIGN_IN){Spacer(Modifier.height(8.dp));Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){Checkbox(checked=rememberMe,onCheckedChange={rememberMe=it;persistRememberChoice()},colors=CheckboxDefaults.colors(checkedColor=AuthPrimary));Text("Remember me",color=Color.White,fontWeight=FontWeight.SemiBold)}}
    Spacer(Modifier.height(10.dp));TurnstileChallenge(captchaRefresh,{captchaToken=it;message="Security check complete.";isError=false},{captchaToken="";message="Security check expired. Complete it again.";isError=true},{captchaToken="";message="Security check failed. Please retry.";isError=true})
    if(message.isNotBlank()){Spacer(Modifier.height(8.dp));Text(message,color=if(isError)MaterialTheme.colorScheme.error else Color(0xFF25D991))}
    Spacer(Modifier.height(14.dp))
    Button(onClick={busy=true;message="";isError=false;val token=captchaToken;scope.launch{runCatching{when(mode){AuthMode.SIGN_IN->{AuthManager.signIn(context,email,password,token);persistRememberChoice()};AuthMode.SIGN_UP->{require(password==confirmPassword){"Passwords do not match."};AuthManager.signUp(email,password,inviteCode,token)};AuthMode.RESET->AuthManager.sendPasswordReset(email,token)}}.onSuccess{when(mode){AuthMode.SIGN_IN->onSignedIn();AuthMode.SIGN_UP->{message="Account created. You can sign in now.";mode=AuthMode.SIGN_IN;inviteCode="";password="";confirmPassword="";resetCaptcha()};AuthMode.RESET->{message="Password reset email sent. Check your inbox.";mode=AuthMode.SIGN_IN;resetCaptcha()}}}.onFailure{message=it.message?:"Something went wrong.";isError=true;resetCaptcha()};busy=false}},enabled=!busy&&captchaToken.isNotBlank()&&email.isNotBlank()&&(mode==AuthMode.RESET||password.isNotBlank())&&(mode!=AuthMode.SIGN_UP||inviteCode.isNotBlank()),modifier=Modifier.fillMaxWidth().height(56.dp),colors=ButtonDefaults.buttonColors(containerColor=AuthPrimary,contentColor=Color.White)){Text(if(busy)"Please wait…" else when(mode){AuthMode.SIGN_IN->"Sign in";AuthMode.SIGN_UP->"Create authorised account";AuthMode.RESET->"Send reset email"},fontWeight=FontWeight.Black)}
    Spacer(Modifier.height(12.dp));if(mode!=AuthMode.SIGN_IN)TextButton(onClick={changeMode(AuthMode.SIGN_IN)},modifier=Modifier.fillMaxWidth()){Text("Back to sign in")} else{OutlinedButton(onClick={changeMode(AuthMode.SIGN_UP)},modifier=Modifier.fillMaxWidth()){Text("Sign up with invite")};TextButton(onClick={changeMode(AuthMode.RESET)},modifier=Modifier.fillMaxWidth()){Text("Forgot password")}}
    Spacer(Modifier.height(8.dp));Text("Private B&L Morley system. Access is limited to authorised accounts.",color=Color(0xFF8FA6C6))
   }
  }
 }
}
