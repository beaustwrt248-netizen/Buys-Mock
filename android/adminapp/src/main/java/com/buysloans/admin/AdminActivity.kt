package com.buysloans.admin

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.ServiceWorkerController
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback

class AdminActivity : ComponentActivity() {
    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdminTelemetry.installCrashHandler(applicationContext)

        webView = WebView(this).apply {
            setBackgroundColor(Color.rgb(237, 243, 239))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                cacheMode = WebSettings.LOAD_NO_CACHE
                loadWithOverviewMode = true
                useWideViewPort = true
                allowFileAccess = false
                allowContentAccess = false
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                javaScriptCanOpenWindowsAutomatically = false
                setSupportMultipleWindows(false)
                // Turnstile relies on stable, standard browser characteristics in native WebViews.
                // Keep Android WebView's stock UA rather than appending an application token.
                userAgentString = WebSettings.getDefaultUserAgent(this@AdminActivity)
            }

            // Admin is a remote parity shell. Do not let an old WebView/service-worker cache
            // keep serving pre-parity HTML, CSS or JavaScript after a protected web release.
            clearCache(true)
            ServiceWorkerController.getInstance().serviceWorkerWebSettings.cacheMode = WebSettings.LOAD_NO_CACHE

            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    if (!request.isForMainFrame) return false
                    val target = request.url.toString()
                    if (AdminWebParityPolicy.isTrustedAdminUrl(target)) return false

                    if (AdminWebParityPolicy.isExternallyRoutableScheme(request.url.scheme)) {
                        openExternal(request.url)
                    }
                    return true
                }
            }

            setDownloadListener { url, _, _, _, _ -> openExternal(Uri.parse(url)) }
        }

        setContentView(webView)
        if (savedInstanceState == null) webView.loadUrl(AdminWebParityPolicy.HOME_URL)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else finish()
            }
        })
    }

    private fun openExternal(uri: Uri) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: ActivityNotFoundException) {
            // Fail closed: unsupported external links never get loaded inside the privileged Admin WebView.
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        webView.restoreState(savedInstanceState)
    }

    override fun onDestroy() {
        webView.apply {
            stopLoading()
            loadUrl("about:blank")
            clearHistory()
            removeAllViews()
            destroy()
        }
        super.onDestroy()
    }
}
