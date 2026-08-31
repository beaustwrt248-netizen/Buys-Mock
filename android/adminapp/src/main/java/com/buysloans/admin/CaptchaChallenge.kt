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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

private const val TURNSTILE_URL = "https://buyshub.me/admin/turnstile.html"
private const val TURNSTILE_HOST = "buyshub.me"
private const val TURNSTILE_PATH = "/admin/turnstile.html"

internal class TurnstileBridge(
    private val emitToken: (String) -> Unit,
    private val emitFailure: (String) -> Unit,
    private val postToMain: ((() -> Unit) -> Unit)
) {
    constructor(
        emitToken: (String) -> Unit,
        emitFailure: (String) -> Unit
    ) : this(
        emitToken = emitToken,
        emitFailure = emitFailure,
        postToMain = { action -> Handler(Looper.getMainLooper()).post(action) }
    )

    @JavascriptInterface
    fun onToken(token: String) {
        if (token.isBlank()) return
        postToMain { emitToken(token) }
    }

    @JavascriptInterface
    fun onExpired(ignored: String) {
        postToMain { emitFailure("Security check expired. Complete it again.") }
    }

    @JavascriptInterface
    fun onError(code: String) {
        val suffix = code.trim().takeIf { it.isNotEmpty() }?.let { " ($it)" }.orEmpty()
        postToMain { emitFailure("Security check failed$suffix. Retry the challenge.") }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun CaptchaChallenge(
    modifier: Modifier = Modifier,
    onToken: (String) -> Unit,
    onFailure: (String) -> Unit
) {
    val latestOnToken = rememberUpdatedState(onToken)
    val latestOnFailure = rememberUpdatedState(onFailure)

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                addJavascriptInterface(
                    TurnstileBridge(
                        emitToken = { token -> latestOnToken.value(token) },
                        emitFailure = { message -> latestOnFailure.value(message) }
                    ),
                    "AndroidBridge"
                )
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        if (request == null || !request.isForMainFrame) return false
                        val uri: Uri = request.url
                        return !(uri.scheme == "https" && uri.host == TURNSTILE_HOST && uri.path == TURNSTILE_PATH)
                    }
                }
                loadUrl(TURNSTILE_URL)
            }
        }
    )
}
