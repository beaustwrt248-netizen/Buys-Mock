package com.buysloans.novanext;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;

public final class MainActivity extends Activity {
    private static final int FILE_CHOOSER_REQUEST = 4107;
    private static final String ALLOWED_HOST = "buyshub.me";
    private static final String ALLOWED_HOST_WWW = "www.buyshub.me";
    private static final String ALLOWED_PATH_PREFIX = "/nova-next/";

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private Uri pendingCameraUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureWebView();
        webView.loadUrl(BuildConfig.NOVA_NEXT_URL);
    }

    private void configureWebView() {
        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setUserAgentString(settings.getUserAgentString() + " NovaNextAndroid/0.1");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(true);
        }

        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleTopLevelNavigation(request.getUrl());
            }

            @Override
            @SuppressWarnings("deprecation")
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleTopLevelNavigation(Uri.parse(url));
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = callback;
                pendingCameraUri = null;

                Intent contentIntent;
                try {
                    contentIntent = params.createIntent();
                } catch (RuntimeException error) {
                    contentIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    contentIntent.addCategory(Intent.CATEGORY_OPENABLE);
                }
                contentIntent.setType("image/*");
                contentIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, params.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE);

                Intent cameraIntent = buildCameraIntent();
                Intent chooser = Intent.createChooser(contentIntent, "Choose image");
                if (cameraIntent != null) chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{cameraIntent});
                try {
                    startActivityForResult(chooser, FILE_CHOOSER_REQUEST);
                    return true;
                } catch (RuntimeException error) {
                    filePathCallback.onReceiveValue(null);
                    filePathCallback = null;
                    pendingCameraUri = null;
                    return false;
                }
            }
        });
    }

    private Intent buildCameraIntent() {
        Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (camera.resolveActivity(getPackageManager()) == null) return null;
        try {
            File directory = new File(getCacheDir(), "nova-next-camera");
            if (!directory.exists() && !directory.mkdirs()) return null;
            File image = File.createTempFile("nova-next-", ".jpg", directory);
            pendingCameraUri = FileProvider.getUriForFile(this, getPackageName() + ".files", image);
            camera.putExtra(MediaStore.EXTRA_OUTPUT, pendingCameraUri);
            camera.setClipData(ClipData.newRawUri("Nova Next capture", pendingCameraUri));
            camera.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            return camera;
        } catch (IOException | IllegalArgumentException error) {
            pendingCameraUri = null;
            return null;
        }
    }

    private boolean handleTopLevelNavigation(Uri uri) {
        if (isAllowedTopLevelUrl(uri)) return false;
        if (uri != null && "https".equalsIgnoreCase(uri.getScheme())) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            } catch (RuntimeException ignored) {
                // Keep the protected WebView on Nova Next if no external handler exists.
            }
        }
        return true;
    }

    static boolean isAllowedTopLevelUrl(Uri uri) {
        if (uri == null || !"https".equalsIgnoreCase(uri.getScheme())) return false;
        String host = uri.getHost();
        if (!ALLOWED_HOST.equalsIgnoreCase(host) && !ALLOWED_HOST_WWW.equalsIgnoreCase(host)) return false;
        String path = uri.getPath();
        return path != null && (path.equals("/nova-next") || path.startsWith(ALLOWED_PATH_PREFIX));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != FILE_CHOOSER_REQUEST) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        if (filePathCallback == null) return;

        Uri[] result = null;
        if (resultCode == RESULT_OK) {
            if (data == null || (data.getData() == null && data.getClipData() == null)) {
                if (pendingCameraUri != null) result = new Uri[]{pendingCameraUri};
            } else {
                result = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            }
        }
        filePathCallback.onReceiveValue(result);
        filePathCallback = null;
        pendingCameraUri = null;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (filePathCallback != null) {
            filePathCallback.onReceiveValue(null);
            filePathCallback = null;
        }
        if (webView != null) {
            webView.stopLoading();
            webView.loadUrl("about:blank");
            webView.clearHistory();
            webView.removeAllViews();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
