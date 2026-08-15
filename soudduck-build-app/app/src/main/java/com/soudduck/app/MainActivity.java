package com.soudduck.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MainActivity extends Activity {
    private WebView web;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setStatusBarColor(0xFF0C0B0A);
        getWindow().setNavigationBarColor(0xFF0C0B0A);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        web = new WebView(this);
        web.setBackgroundColor(0xFF0C0B0A);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setDatabaseEnabled(true);
        web.getSettings().setMediaPlaybackRequiresUserGesture(false);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(false);
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) { return false; }
        });
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);
        setContentView(web);
        web.loadDataWithBaseURL("https://soudduck.local/", readAsset("index.html"), "text/html", "UTF-8", null);
    }
    private String readAsset(String name) {
        StringBuilder out = new StringBuilder();
        try (InputStream in = getAssets().open(name); BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"))) {
            String line; while ((line = br.readLine()) != null) out.append(line).append('\n');
        } catch (Exception e) { return "<h1>SoudDuck</h1><p>Falha ao carregar a interface.</p>"; }
        return out.toString();
    }
    @Override public void onBackPressed() {
        if (web != null) web.evaluateJavascript("(function(){var n=document.getElementById('nowSheet');if(n&&n.classList.contains('open')){n.classList.remove('open');return 'closed'}return 'none'})()", value -> { if ("\"none\"".equals(value)) super.onBackPressed(); });
        else super.onBackPressed();
    }
    @Override protected void onDestroy(){ if(web!=null){web.destroy();web=null;} super.onDestroy(); }
}
