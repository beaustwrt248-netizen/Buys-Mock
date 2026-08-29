package com.buysloans.admin

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONTokener

private const val TURNSTILE_URL = "https://beaustwrt248-netizen.github.io/Buys-Mock/admin/turnstile.html"
private const val TURNSTILE_HOST = "beaustwrt248-netizen.github.io"
private const val TOKEN_PROBE_DELAY_MS = 250L

private class TurnstileBridge(
    private val onToken: (String) -> Unit,
    private val onFailure: (String) -> Unit
) {
    private val main = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onToken(token: String) {
        if (token.isBlank()) return
        main.post { onToken(token) }
    }

    @JavascriptInterface
    fun onExpired(ignored: String) {
        main.post { onFailure("Security check expired. Complete it again.") }
    }

    @JavascriptInterface
    fun onError(code: String) {
        val suffix = code.trim().takeIf { it.isNotEmpty() }?.let { " ($it)" }.orEmpty()
        main.post { onFailure("Security check failed$suffix. Retry the challenge.") }
    }
}

private fun decodeJavascriptString(value: String?): String {
    if (value.isNullOrBlank() || value == "null") return ""
    return runCatching { JSONTokener(value).nextValue() as? String }.getOrNull().orEmpty()
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun CaptchaChallenge(
    modifier: Modifier = Modifier,
    onToken: (String) -> Unit,
    onFailure: (String) -> Unit
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                addJavascriptInterface(TurnstileBridge(onToken, onFailure), "AndroidBridge")
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        if (request == null || !request.isForMainFrame) return false
                        val uri: Uri = request.url
                        return !(uri.scheme == "https" && uri.host == TURNSTILE_HOST && uri.path == "/Buys-Mock/admin/turnstile.html")
                    }

                    override fun onPageFinished(view: WebView, url: String?) {
                        super.onPageFinished(view, url)
                        var deliveredToken = ""
                        fun probeToken() {
                            if (!view.isAttachedToWindow) return
                            view.evaluateJavascript("window.__morleyTurnstileToken || ''") { raw ->
                                val token = decodeJavascriptString(raw)
                                if (token.isNotBlank() && token != deliveredToken) {
                                    deliveredToken = token
                                    onToken(token)
                                }
                                if (view.isAttachedToWindow) view.postDelayed({ probeToken() }, TOKEN_PROBE_DELAY_MS)
                            }
                        }
                        probeToken()
                    }
                }
                loadUrl(TURNSTILE_URL)
            }
        }
    )
}
