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

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdminTelemetry.installCrashHandler(applicationContext)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        // Match the Admin shell while WebView content is attaching/restoring so task switching
        // never exposes the platform's default light/white backing surface.
        window.decorView.setBackgroundColor(Color.rgb(4, 9, 18))
        if (BuildConfig.IS_RECOVERY_BUILD) title = "Morley Admin Recovery"

        webView = WebView(this).apply {
            setBackgroundColor(Color.rgb(4, 9, 18))
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            // Samsung/Android WebView can leave a programmatically-created WebView without
            // touch focus. The page remains tappable (including Turnstile), but HTML text
            // controls cannot acquire the IME. Explicitly make the WebView a touch-focus
            // target and hand focus to it on ACTION_DOWN without consuming the event.
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
                // Keep reusable, versioned CSS/JS assets cached for fast login/startup. The
                // top-level Admin document is explicitly revalidated below so canonical and
                // Recovery cannot remain on different cached shell generations.
                cacheMode = WebSettings.LOAD_DEFAULT
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

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(4, 9, 18))
            addView(webView)
            if (BuildConfig.IS_RECOVERY_BUILD) addView(createRecoveryIdentityBadge())
        }
        setContentView(root)

        webView.requestFocus(View.FOCUS_DOWN)
        webView.post { webView.requestFocusFromTouch() }
        if (savedInstanceState == null) loadFreshAdminShell()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else finish()
            }
        })
    }

    private fun loadFreshAdminShell() {
        // Revalidate only the HTML shell. Static assets retain normal WebView caching and their
        // own cache-busting versions, avoiding the global LOAD_NO_CACHE startup regression.
        webView.loadUrl(
            AdminWebParityPolicy.HOME_URL,
            mapOf("Cache-Control" to "no-cache", "Pragma" to "no-cache")
        )
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
