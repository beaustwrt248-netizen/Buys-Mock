package com.buysloans.admin

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback

class AdminActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private var rendererGone = false

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdminTelemetry.installCrashHandler(applicationContext)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        window.decorView.setBackgroundColor(Color.rgb(4, 9, 18))
        if (BuildConfig.IS_RECOVERY_BUILD) title = "Morley Admin Recovery"

        webView = WebView(this).apply {
            setBackgroundColor(Color.rgb(4, 9, 18))
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
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
                cacheMode = WebSettings.LOAD_DEFAULT
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

                override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                    rendererGone = true
                    runCatching {
                        (view.parent as? ViewGroup)?.removeView(view)
                        view.destroy()
                    }
                    window.decorView.post {
                        if (!isFinishing && !isDestroyed) recreate()
                    }
                    return true
                }
            }

            setDownloadListener { url, _, _, _, _ -> openExternal(Uri.parse(url)) }
        }

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(4, 9, 18))
            addView(webView)
            if (BuildConfig.IS_RECOVERY_BUILD) addView(createRecoveryIdentityBadge())
        }
        setContentView(root)

        webView.requestFocus(View.FOCUS_DOWN)
        webView.post { webView.requestFocusFromTouch() }
        // 0.1.31 added custom request headers here to force shell revalidation. Real Samsung
        // testing showed the login/Turnstile WebView could become unstable after that change.
        // Return to the previously stable plain navigation path; versioned assets remain cached.
        if (savedInstanceState == null) webView.loadUrl(AdminWebParityPolicy.HOME_URL)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else finish()
            }
        })
    }

    private fun createRecoveryIdentityBadge(): TextView {
        val horizontal = dp(10)
        val vertical = dp(7)
        val background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(18).toFloat()
            setColor(Color.rgb(127, 29, 29))
            setStroke(dp(1), Color.argb(90, 255, 255, 255))
        }
        return TextView(this).apply {
            text = "RECOVERY ADMIN"
            contentDescription = "Morley Admin Recovery build"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(horizontal, vertical, horizontal, vertical)
            this.background = background
            isClickable = false
            isFocusable = false
            elevation = dp(8).toFloat()
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.END
            ).apply {
                topMargin = dp(10)
                marginEnd = dp(10)
            }
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun openExternal(uri: Uri) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: ActivityNotFoundException) {
            // Fail closed: unsupported external links never get loaded inside the privileged Admin WebView.
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (!rendererGone) webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        if (!rendererGone) webView.restoreState(savedInstanceState)
    }

    override fun onDestroy() {
        if (!rendererGone) {
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
