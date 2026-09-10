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
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        // Match the Admin shell while WebView content is attaching/restoring so task switching
        // never exposes the platform's default light/white backing surface.
        window.decorView.setBackgroundColor(Color.rgb(4, 9, 18))

        webView = WebView(this).apply {
            setBackgroundColor(Color.rgb(4, 9, 18))
            layoutParams = ViewGroup.LayoutParams(
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
                // Keep static Admin resources reusable between launches. Authenticated API requests
                // remain explicitly no-store in the shared Morley auth client, so this improves
                // startup/login responsiveness without weakening session or data freshness rules.
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

                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    if (BuildConfig.IS_RECOVERY_BUILD && AdminWebParityPolicy.isTrustedAdminUrl(url)) {
                        injectRecoveryIdentity(view)
                    }
                }
            }

            setDownloadListener { url, _, _, _, _ -> openExternal(Uri.parse(url)) }
        }

        setContentView(webView)
        webView.requestFocus(View.FOCUS_DOWN)
        webView.post { webView.requestFocusFromTouch() }
        if (savedInstanceState == null) webView.loadUrl(AdminWebParityPolicy.HOME_URL)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else finish()
            }
        })
    }

    private fun injectRecoveryIdentity(view: WebView) {
        // Presentation-only marker. It grants no capability and intentionally does not bridge
        // Java/Kotlin objects into page JavaScript.
        view.evaluateJavascript(
            """
            (() => {
              document.documentElement.dataset.morleyAdminBuild = 'recovery';
              document.title = 'Morley Admin Recovery';
              if (document.getElementById('morleyAdminRecoveryIdentity')) return;
              const badge = document.createElement('div');
              badge.id = 'morleyAdminRecoveryIdentity';
              badge.textContent = 'RECOVERY ADMIN';
              badge.setAttribute('aria-label', 'Morley Admin Recovery build');
              Object.assign(badge.style, {
                position: 'fixed', top: 'max(8px, env(safe-area-inset-top))', right: '8px',
                zIndex: '2147483647', pointerEvents: 'none', padding: '6px 9px',
                borderRadius: '999px', background: '#7f1d1d', color: '#fff',
                border: '1px solid rgba(255,255,255,.28)', font: '800 10px/1 system-ui,sans-serif',
                letterSpacing: '.08em', boxShadow: '0 4px 14px rgba(0,0,0,.35)'
              });
              document.body.appendChild(badge);
            })();
            """.trimIndent(),
            null
        )
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
