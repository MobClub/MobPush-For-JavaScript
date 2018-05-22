package com.mob.js.demo;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.mob.js.MobPushUtils;


/**
 * Created by yyfu on 2018/5/21.
 */

public class MainActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebView wvBody = new WebView(this);
        WebViewClient wvClient = new WebViewClient();
        wvBody.setWebViewClient(wvClient);
        wvBody.setWebChromeClient(new WebChromeClient() {
            public boolean onJsAlert(WebView view, String url, String message,
                                     JsResult result) {
                return super.onJsAlert(view, url, message, result);
            }
        });

        // you must call the following line after the webviewclient is set into the webview
        MobPushUtils.prepare(wvBody, wvClient);
        setContentView(wvBody);
        wvBody.loadUrl("file:///android_asset/Sample.html");
    }
}
