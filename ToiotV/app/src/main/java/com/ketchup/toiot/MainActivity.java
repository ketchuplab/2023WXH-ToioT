package com.ketchup.toiot;


import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

import android.content.Intent;

public class MainActivity extends AppCompatActivity {

    private static TextView logTextView;
    private static TextView beaconTextView;
    private static TextView bonusTextView;

    private static TextView walletTextView;
    GifImageView gifImageView;
    GifDrawable gifDrawable;

    private final static int REQUEST_ENABLE_BT = 1;
    String TAG = "MainActivity_TAG";
    Button btnScan;
    Button btnSetting;
    Button btnLocate;

    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private static MainActivity instance;

    private static long lastCallbackTime = 0;
    private static float counter = 0;
    private static final long MIN_CALLBACK_INTERVAL = 300; // 3秒

    // 一些核心配置
    public static  float addBonuse = 0.05f;
    public static String walletAddr = "0x70997970C51812dc3A010C7d01b50e0d17dc79C8";
    public static String baseUrl = "http://172.18.9.65:18099/";


    private static SoundPool soundPool;
    private static int sndIDCoin;
    private static int sndIDDropCoin;
    private static int sndIDPowerUP;
    private static int sndID12428;
    private static int sndID14131;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnScan = findViewById(R.id.btnScan);
        btnSetting = findViewById(R.id.btnSetting);
        btnLocate = findViewById(R.id.btnLocate);

        logTextView = findViewById(R.id.logTextView);
        logTextView.setText("Log messages will be displayed here.");
        beaconTextView = findViewById(R.id.beaconTextView);
        bonusTextView = findViewById(R.id.bonusTextView);
        walletTextView = findViewById(R.id.walletTextView);

        // 设置文本内容
        // 0x70997970C51812dc3A010C7d01b50e0d17dc79C8
        //walletTextView.setText("Wallet: 0xf39Fd6**************92266");
        walletTextView.setText("Wallet: 0x709979**************c79C8");

        // gif
        gifImageView = findViewById(R.id.gif_iamge_view);
        gifDrawable = (GifDrawable) gifImageView.getDrawable();
        gifDrawable.setLoopCount(5); // 设置具体播放次数
        gifDrawable.setLoopCount(0); // 设置无限循环播放

        gifImageView.setVisibility(View.GONE);// 设置隐藏

        // sound
        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        sndIDCoin = soundPool.load(this, R.raw.coin, 1);
        sndIDDropCoin = soundPool.load(this, R.raw.dropcoin, 1);
        sndIDPowerUP = soundPool.load(this, R.raw.one_up, 1);
        sndID12428 = soundPool.load(this, R.raw.m12428, 1);
        sndID14131 = soundPool.load(this, R.raw.m14131, 1);

        MainActivity.logMessage("onCreate finished...");
        instance = this;
        if (mBluetoothAdapter == null)
        { Log.d(TAG,"設備不支持藍牙"); }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        {
            Toast.makeText(this, "Does not support BLE", Toast.LENGTH_SHORT).show();
            finish();
        }
        // 设置
        btnSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OnClickSetting();
            }
        });
        // 定位
        btnLocate.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            OnClickLocation();
            }
        });        // 扫描
        btnScan.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onCheckBluetoothAuth();
            }
        });
    }
    public static  void logMessage(String message) {
        String timeStamp = (String) DateFormat.format("yyyy-MM-dd HH:mm:ss", System.currentTimeMillis());
        String currentLog = logTextView.getText().toString();
        logTextView.setText("[" + timeStamp + "] " + message +"\n" + currentLog);
    }
    public static  void logBeaconInfo(String message,String macAddr,double rssi) {
        beaconTextView.setText( message);
        int progress = 100-Math.abs((int)rssi); // 这只是一个示例，具体根据 RSSI 范围调整
        MainActivity.instance.lerpColor(progress);

        MainActivity.instance.gifDrawable.setSpeed(1+(int)(progress/10));
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCallbackTime >= MIN_CALLBACK_INTERVAL && rssi > -70) {
            lastCallbackTime = currentTime;

            if (counter == (int) counter) {
                soundPool.play(sndIDPowerUP, 1, 1, 1, 0, 1);
            }
            if(progress >= 0 && progress < 30) {
                counter+= addBonuse;
                soundPool.play(sndID12428, 1, 1, 1, 0, 1);
            }
            else if(progress >= 30 && progress < 60) {
                counter+= addBonuse;
                soundPool.play(sndIDDropCoin, 1, 1, 1, 0, 1);
            }else {

            }
            makeRequestToken(macAddr,walletAddr);
            updateUserInfo();
//            bonusTextView.setText("current bonus: " + counter + " ETH");
        }
    }
    private void request_permissions()//檢查權限
    {
        // 創建一個權限列表，把需要使用而沒有授權的的權限存放在這裡
        List<String> permissionList = new ArrayList<>();

        // 判斷權限是否已經授予，没有就把該權限添加到列表中
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        // 如果列表為空，代表全部權限都有了，反之則代表有權限還沒有被加到list，要申请權限
        if (!permissionList.isEmpty())//列表不是空的
        {
            ActivityCompat.requestPermissions(this,permissionList.toArray(new String[permissionList.size()]), 1002);
        }
    }
    // 請求權限回調方法
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onDestroy()
    {
        Log.d(TAG, "================Activity onDestroy================");

        super.onDestroy();
    }
    public static void makeRequestToken(String macAddr,String walletAddr){

//        String walletAddr = "0x70997970C51812dc3A010C7d01b50e0d17dc79C8";
//        String macAddr = "20:23:08:07:11:08";

        ApiClient apiClient = new ApiClient();
//      String apiUrl = "http://172.18.9.65/Login/login";
        String apiUrl = baseUrl;
        apiUrl += "reqToken?walletAddr="+walletAddr+"&macAddr="+macAddr;
        apiClient.requestToken(apiUrl);

    }
    public static void ShowBonusInfo(String bonusTxt){
        bonusTextView.setText("current bonus: " + bonusTxt);
    }
    public static void updateUserInfo(){
        ApiClient apiClient = new ApiClient();
        String apiUrl = baseUrl;
        apiUrl += "getUserInfo?walletAddr="+walletAddr;
        apiClient.getUserInfo(apiUrl);

    }
    public void OnClickSetting() {
//        Toast.makeText(this, "message", Toast.LENGTH_SHORT).show();
//        updateUserInfo();
        Intent intent = new Intent(this, SettingActivity.class);
        startActivity(intent);
    }
    public void OnClickLocation() {
        Intent intent = new Intent(this, WebViewActivity.class);
        startActivity(intent);
    }
    public  void onCheckBluetoothAuth(){
        if(mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled())//檢查是否開啟藍芽
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, REQUEST_ENABLE_BT);
        }
        else if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                ||ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)//檢查是否開啟該應用程式的位置權限
        {
            request_permissions();//沒有開啟的話就在跳出開啟該應用程式位置權限視窗
        }
        else {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("after auth it\nsend data to server")
                    .setPositiveButton("agree", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            gifImageView.setVisibility(View.VISIBLE);

                            btnScan.setText("OpenScan");
                            btnScan.setEnabled(false);
                            Intent service = new Intent(MainActivity.this, BeaconService.class);
                            //service.putExtra("startType", 1);
                            if (Build.VERSION.SDK_INT >= 26) {
                                Log.d("MyService", "SDK_INT>26, startforegroundService");
                                MainActivity.this.startForegroundService(service);
                            } else {
                                Log.d("MyService", "SDK_INT<26, startService");
                                MainActivity.this.startService(service);
                            }
                        }
                    })
                    .setNegativeButton("Deny", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .show();
        }
    }
    @Override
    protected void onResume()
    {
        Log.d(TAG, "================Activity onResume123================");
        super.onResume();
    }
    public static void showToast(String message) {
        if (MainActivity.instance != null) {
            Toast.makeText(MainActivity.instance, message, Toast.LENGTH_SHORT).show();
        }
    }
    public void UpdateBackground(int newStartColor,int newEndColor) {

        int redStart = Color.red(newStartColor);
        int greenStart = Color.green(newStartColor);
        int blueStart = Color.green(newStartColor);

        int redEnd = Color.red(newStartColor);
        int greenEnd = Color.green(newStartColor);
        int blueEnd = Color.green(newStartColor);

        Log.d("ColorDebug", "redStart: " + redStart + " greenStart: " + greenStart + " blueStart: " + blueStart);
        Log.d("ColorDebug", "redEnd: " + redEnd + " greenEnd: " + greenEnd + " blueEnd: " + blueEnd);

        // 获取背景资源ID
        int drawableResourceId = getResources().getIdentifier("shape_gradient", "drawable", getPackageName());
        // 获取GradientDrawable对象
        GradientDrawable gradientDrawable = (GradientDrawable) getResources().getDrawable(drawableResourceId);
        gradientDrawable.setColors(new int[]{newStartColor, newEndColor});
        // 将GradientDrawable设置为视图的背景
        View view = findViewById(R.id.main_home);
        view.setBackground(gradientDrawable);
    }
    public void lerpColor(int progress) {
        // RGB                0       ->      125     ->      255
        // Start是白色  (255,255,255)  ->   (0,255,0)  ->   (0,255,0)
        // End是白色    (255,255,255)  ->   (255,0,0)  ->   (0,255,0)
        Log.d("ColorDebug","progress ===========>" + progress);

        if(progress >= 0 && progress < 50){
//            int startColor = (int) new ArgbEvaluator().evaluate(progress / 128.f, Color.rgb(255,255,255), Color.rgb(0,255,0));
//            int endColor = (int) new ArgbEvaluator().evaluate(progress / 128.f, Color.rgb(255,255,255), Color.rgb(255,0,0));
            int curV = (int)255f/50* progress;
            int startColor = Color.rgb(255-curV, 255, 255-curV);
            int endColor = Color.rgb(255, 255-curV, 255-curV);

            UpdateBackground(startColor,endColor);
        }
        else {
//            int startColor = (int) new ArgbEvaluator().evaluate(progress / 128.f, Color.rgb(0,255,0), Color.rgb(0,255,0));
//            int endColor = (int) new ArgbEvaluator().evaluate(progress / 128.f, Color.rgb(255,0,0), Color.rgb(0,255,0));
            int curV = (int)255f/50* (progress-50);
            int startColor = Color.rgb(0, 255, 0);
            int endColor = Color.rgb(255-curV, curV,  0);
            UpdateBackground(startColor,endColor);
        }
    }

}





