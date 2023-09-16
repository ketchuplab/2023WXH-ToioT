package com.ketchup.toiot;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.util.Random;

import android.util.Log;
import android.view.View;

public class WebViewActivity extends AppCompatActivity {
    private WebView webView;
    public static String baseUrl = "http://172.18.9.88:8012/";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);


        webView = findViewById(R.id.webView);
        webView.clearCache(true);
        webView.getSettings().setJavaScriptEnabled(true); // 启用JavaScript支持
        webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
//        webView.loadUrl("http://172.18.9.88:8012/"); // 加载你要显示的网页
//        webView.loadUrl("https://www.baidu.com"); // 加载你要显示的网页

        // get version
        int versionCode = BuildConfig.VERSION_CODE;
        String versionName = BuildConfig.VERSION_NAME;

        Random random = new Random();
        // 生成一个随机整数
        int randomNumber = random.nextInt();

        // my_app_1_1.0.apk
        String reqParam = "?version="+versionCode+"_"+versionName+"&randomNumber="+randomNumber;
        webView.loadUrl(baseUrl+reqParam);
        // 初始化返回按钮
//        backButton = findViewById(R.id.backButton);
    }

    // 返回按钮的点击事件
    public void goBack(View view) {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            finish(); // 如果不能返回，关闭当前Activity
        }
    }
}