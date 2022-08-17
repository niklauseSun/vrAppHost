package com.vapp.android.webView;

import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.Nullable;

import com.vapp.android.R;
public class VWebView extends AppCompatActivity {

    private WebView mWebView = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.v_webview);
        mWebView = findViewById(R.id.webViewWebFrameModule);

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }
        });
        // 需要删掉
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        mWebView.getSettings().setDomStorageEnabled(true);

        // 打开调试模式 发布时要去掉
        WebView.setWebContentsDebuggingEnabled(true);
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        String url = bundle.getString("loadUrl");

        mWebView.loadUrl(url);
    }
}
