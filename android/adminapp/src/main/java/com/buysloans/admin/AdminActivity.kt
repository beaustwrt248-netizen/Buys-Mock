package com.buysloans.admin

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import org.json.JSONObject

class AdminActivity : ComponentActivity() {
    companion object {
        const val EXTRA_ACCESS_TOKEN = "admin_access_token"
        const val EXTRA_REFRESH_TOKEN = "admin_refresh_token"
        private const val NATIVE_LOGOUT_PATH = "/admin/native-logout"
    }

    private lateinit var webView: WebView
    private var accessToken = ""
    private var refreshToken = ""

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdminTelemetry.installCrashHandler(applicationContext)
        if (BuildConfig.IS_RECOVERY_BUILD) title = "Morley Admin Recovery"

        accessToken = intent.getStringExtra(EXTRA_ACCESS_TOKEN).orEmpty()
        refreshToken = intent.getStringExtra(EXTRA_REFRESH_TOKEN).orEmpty()
        if (accessToken.isBlank() || refreshToken.isBlank()) {
            returnToNativeLogin()
            return
        }

        webView = WebView(this).apply {
            setBackgroundColor(Color.rgb(4, 9, 18))
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                cacheMode = WebSettings.LOAD_NO_CACHE
                loadWithOverviewMode = true
                useWideViewPort = true
                allowFileAccess = false
                allowContentAccess = false
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                javaScriptCanOpenWindowsAutomatically = false
                setSupportMultipleWindows(false)
                userAgentString = WebSettings.getDefaultUserAgent(this@AdminActivity)
            }

            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    if (!request.isForMainFrame) return false
                    if (request.url.path == NATIVE_LOGOUT_PATH) {
                        returnToNativeLogin()
                        return true
                    }
                    val target = request.url.toString()
                    if (AdminWebParityPolicy.isTrustedAdminUrl(target)) return false
                    if (AdminWebParityPolicy.isExternallyRoutableScheme(request.url.scheme)) openExternal(request.url)
                    return true
                }

                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    if (AdminWebParityPolicy.isTrustedAdminUrl(url)) injectNativeSession(view)
                }
            }

            setDownloadListener { url, _, _, _, _ -> openExternal(Uri.parse(url)) }
        }

        setContentView(webView)
        webView.clearCache(true)
        webView.loadUrl(AdminWebParityPolicy.freshHomeUrl(BuildConfig.VERSION_CODE) + "&nativeAuth=1")

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else finish()
            }
        })
    }

    private fun injectNativeSession(view: WebView) {
        val access = JSONObject.quote(accessToken)
        val refresh = JSONObject.quote(refreshToken)
        val logoutUrl = JSONObject.quote("https://buyshub.me$NATIVE_LOGOUT_PATH")
        val script = """
            (function(){
              if(!window.sb || !window.sb.auth) return;
              var exitToNative=function(){ window.location.replace($logoutUrl); };
              window.sb.auth.setSession({access_token:$access,refresh_token:$refresh}).then(function(result){
                if(result && result.error){ exitToNative(); return; }
                var logout=document.getElementById('logoutBtn');
                if(logout){ logout.onclick=function(){ window.sb.auth.signOut().finally(exitToNative); }; }
                if(typeof window.loadSession==='function') window.loadSession();
              }).catch(exitToNative);
            })();
        """.trimIndent()
        view.evaluateJavascript(script, null)
    }

    private fun returnToNativeLogin() {
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.clearHistory()
        }
        accessToken = ""
        refreshToken = ""
        startActivity(Intent(this, AdminLoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        finish()
    }

    private fun openExternal(uri: Uri) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: ActivityNotFoundException) {
            // Unsupported external links never enter the privileged Admin WebView.
        }
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.apply {
                stopLoading()
                loadUrl("about:blank")
                clearHistory()
                removeAllViews()
                destroy()
            }
        }
        super.onDestroy()
    }
}
