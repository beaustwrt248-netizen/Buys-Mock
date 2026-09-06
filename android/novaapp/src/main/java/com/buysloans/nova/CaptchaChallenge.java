package com.buysloans.nova;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.View;
import android.widget.LinearLayout;

final class CaptchaChallenge {
    interface Listener {
        void onState(boolean ready, String message);
    }

    private final Activity activity;
    private final Listener listener;
    private final WebView webView;
    private volatile String token = "";

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    CaptchaChallenge(Activity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
        webView = new WebView(activity);
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.setMinimumHeight(dp(126));
        webView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(132)));
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        webView.setWebViewClient(new WebViewClient());
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        webView.addJavascriptInterface(new Bridge(), "AndroidBridge");
        load();
    }

    View view() { return webView; }
    String token() { return token; }
    boolean isReady() { return token != null && !token.isBlank(); }

    void reset() {
        token = "";
        listener.onState(false, "Complete the security check to continue.");
        load();
    }

    void destroy() {
        token = "";
        webView.removeJavascriptInterface("AndroidBridge");
        webView.stopLoading();
        webView.destroy();
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private void load() {
        listener.onState(false, "Security check loading…");
        String siteKey = escapeJs(BuildConfig.TURNSTILE_SITE_KEY);
        String html = "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no'>"
                + "<meta name='color-scheme' content='dark'><style>html,body{margin:0;background:#131b2b;color:#eee;font-family:system-ui,sans-serif}"
                + "main{min-height:112px;display:flex;align-items:center;justify-content:center;padding:8px}#status{font-size:12px;color:#a6b2c7;text-align:center;margin-top:5px}</style></head>"
                + "<body><main><div><div id='challenge'></div><div id='status'>Security check loading…</div></div></main>"
                + "<script src='https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit' async defer></script><script>"
                + "let tries=0;function boot(){if(window.turnstile){document.getElementById('status').textContent='Complete the security check.';"
                + "turnstile.render('#challenge',{sitekey:'" + siteKey + "',theme:'dark',size:'flexible',action:'blmorley_auth',"
                + "callback:t=>{document.getElementById('status').textContent='Security check complete.';AndroidBridge.onToken(t)},"
                + "'expired-callback':()=>{document.getElementById('status').textContent='Security check expired.';AndroidBridge.onExpired('')},"
                + "'error-callback':e=>{document.getElementById('status').textContent='Security check failed.';AndroidBridge.onError(String(e||''));return true}});return;}"
                + "if(++tries<40)setTimeout(boot,250);else AndroidBridge.onError('bootstrap_timeout')}setTimeout(boot,100);</script></body></html>";
        webView.loadDataWithBaseURL(BuildConfig.CAPTCHA_ORIGIN, html, "text/html", "UTF-8", null);
    }

    private static String escapeJs(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    private final class Bridge {
        @JavascriptInterface public void onToken(String value) {
            activity.runOnUiThread(() -> {
                token = value == null ? "" : value;
                listener.onState(!token.isBlank(), token.isBlank() ? "Complete the security check to continue." : "Security check complete.");
            });
        }
        @JavascriptInterface public void onExpired(String ignored) {
            activity.runOnUiThread(() -> {
                token = "";
                listener.onState(false, "Security check expired. Complete it again.");
            });
        }
        @JavascriptInterface public void onError(String ignored) {
            activity.runOnUiThread(() -> {
                token = "";
                listener.onState(false, "Security check failed. Tap sign in after completing a new check.");
            });
        }
    }
}
