package com.buysloans.admin

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback

class AdminActivity : ComponentActivity() {
    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdminTelemetry.installCrashHandler(applicationContext)
        if (BuildConfig.IS_RECOVERY_BUILD) title = "Morley Admin Recovery"
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        webView = WebView(this).apply {
            setBackgroundColor(Color.rgb(237, 243, 239))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            // Samsung WebView may leave a programmatically-created WebView without touch focus.
            // The page still renders and Turnstile can animate, but HTML email/password controls
            // cannot acquire the IME. Restore the proven focus handoff without consuming taps.
            isFocusable = true
            isFocusableInTouchMode = true
            setOnTouchListener { view, event ->
                if (event.action == MotionEvent.ACTION_DOWN && !view.hasFocus()) {
                    view.requestFocusFromTouch()
                }
                false
            }

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true

                // Admin is a live control surface. LOAD_DEFAULT plus WebView state restoration can
                // resurrect an old /admin/ shell after a deployment and leave only the page
                // background visible. Always request the current document instead.
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
        webView.requestFocus(View.FOCUS_DOWN)
        webView.post { webView.requestFocusFromTouch() }

        // Do not restore a previously saved WebView document here. That state can contain the
        // exact stale/blank Admin shell we are recovering from. Cookies and DOM storage still
        // preserve the signed-in session; only the document itself is forced fresh.
        webView.clearCache(true)
        webView.loadUrl(AdminWebParityPolicy.freshHomeUrl(BuildConfig.VERSION_CODE))

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
