package com.ketchup.toiot;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class SettingActivity extends AppCompatActivity {
    private EditText urlEditText;
    public static String baseIP = "172.18.9.88";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        // 获取编辑框和按钮的引用
        urlEditText = findViewById(R.id.urlEditText);
        urlEditText.setText(SettingActivity.baseIP);
    }
    // 返回按钮的点击事件
    public void goBack(View view) {
        finish(); // 如果不能返回，关闭当前Activity
    }

    public void OnClickSaveUrl(View view) {
        // 获取编辑框中的文本
        String url = urlEditText.getText().toString();
        Log.d("Setting TAG",url);
        SettingActivity.baseIP = url;
        MainActivity.baseUrl = "http://" + url +":18099/";
        WebViewActivity.baseUrl = "http://"+url+":8080/";
    }
}