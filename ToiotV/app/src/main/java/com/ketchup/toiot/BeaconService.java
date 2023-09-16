package com.ketchup.toiot;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.support.annotation.RequiresApi;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class BeaconService extends Service {
    private MyBinder mBinder = new MyBinder();
    String TAG = "Service_TAG";
    String TAG2 = "Sensor_TAG";
    ArrayList<Double> average_rssi_30AEA40894C2 = new ArrayList<>();
    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    int n = 3;//環境變數
    int txpower = -70;//bluetooth txpower
    Double distance=0.0;
    SensorManager mSersorManager;
    Sensor mAccrlerometers;
    Sensor mGyroscope;
    private int ongoingNotificationID = 42;
    String notifiation_content = "背景程式正在執行";
    NotificationManager mNotificationManager;

    private Handler handler = new Handler();
    private Runnable runnable = new Runnable()
    {
        @Override
        public void run()
        {
            Log.d("Run","run");
            //這是開始掃bluetooth low energy的
            scaniDevice(true);
//            handler.postDelayed(this, 2000);//每五秒掃一次
        }
    };

    public void scaniDevice(boolean en)
    {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        final Handler mHandler = new Handler();
        final ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setReportDelay(200).build();
        final BluetoothLeScanner bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (en){
            try{
                mHandler.postDelayed(new Runnable(){
                    @Override
                    public void run(){
                        ScanCallback scallback = new SampleScanCallback();
                        bluetoothLeScanner.stopScan(scallback);
                    }
                },200);
                ScanCallback scallback = new SampleScanCallback();
                bluetoothLeScanner.startScan(null,settings,scallback);
            }
            catch (Exception e){}
        }else{
            ScanCallback scallback = new SampleScanCallback();
            bluetoothLeScanner.stopScan(scallback); //停止
        }
    }

    public class SampleScanCallback extends ScanCallback
    {
        @Override
        public void onScanResult(int callbackType, ScanResult scanResult)
        {
            super.onScanResult(callbackType, scanResult);
        }

        @TargetApi(Build.VERSION_CODES.O)
        @Override
        public void onBatchScanResults(List<ScanResult> results)
        {
            super.onBatchScanResults(results);
            if(true){
//                return;
            }
            String myBeacon = "20:23:08:07:11:08";
            for(ScanResult scanResult : results)
            {
                Double rssi = Double.valueOf(scanResult.getRssi());

//                MainActivity.logMessage(msg);
//                function.PassData(rssi,1);//把rssi上傳server
                if (scanResult.getDevice().getAddress() == myBeacon || true)
                {
                    String deviceInfo = scanResult.getDevice().toString();
                    String deviceAddr = scanResult.getDevice().getAddress();
                    String deviceName = scanResult.getDevice().getName();

                    ParcelUuid[] uuid =  scanResult.getDevice().getUuids();
                    String deviceUUID = uuid != null && uuid.length > 0 ? uuid.toString() : "--";

                    int[] majorMinor = parseScanResult(scanResult);

                    if(!(majorMinor[0] == 11 )){ //&& majorMinor[1] == 8
                        continue;
                    }
                    String msg = "scan mac => "+ "\n";
                    msg =  msg + "deviceAddr = " + deviceAddr+ "\n";
                    msg =  msg + "deviceDist = " + calculateDistance(rssi)+ "\n";
                    msg =  msg + "deviceName = " + (deviceName != null && !deviceName.isEmpty() ? deviceName:"--")+ "\n";
                    msg =  msg + "deviceUUID = " + (deviceUUID != null && !deviceUUID.isEmpty() ? deviceUUID:getUUIDFromScanRecord(scanResult.getScanRecord().getBytes()))+ "\n";
                    msg =  msg + "deviceInfo = " + deviceInfo+ "\n";
                    // 打印 Major 和 Minor 值
                    msg = msg + "Major = " + majorMinor[0] +"\n" ;
                    msg = msg + "Minor =  " + majorMinor[1] +"\n" ;
//                    msg = msg + "scanResult =  " +scanResult.toString() +"\n" ;

                    Log.d(TAG,msg);
                    Log.d(TAG,"mac=20:23:08:07:11:08 , rssi = "+rssi);
                    average_rssi_30AEA40894C2.add(rssi);
                    function.PassData(rssi,1);//把rssi上傳server
                    msg = msg+ " rssi = " + rssi;
                    MainActivity.logMessage(msg);
                    //**30AEA40894C2 Beacon的**//
                    if(average_rssi_30AEA40894C2.size()!=0)
                    {
                        String macAddrTxt = "MAC=>20:23:08:07:11:08";
                        msg = macAddrTxt + '\n' +"distance = "+distance+"m";
                        Log.d(TAG,msg);//秀出收到的rssi數據及距離
                        MainActivity.logBeaconInfo(msg ,deviceAddr, rssi);

                    }
                    //Log.d(TAG,"不是我們的bluetooth");
                }
            }
        }
        public int[] parseScanResult(ScanResult result) {
            int[] majorMinor = new int[2];
            try {
                BluetoothDevice device = result.getDevice();
                byte[] scanRecord = result.getScanRecord().getBytes();

                // 从扫描记录中获取 Major 和 Minor 值的位置
                int startByte = 2;
                boolean patternFound = false;
                while (startByte <= 5) {
                    if (((int) scanRecord[startByte + 2] & 0xff) == 0x02 && // iBeacon
                            ((int) scanRecord[startByte + 3] & 0xff) == 0x15) {

                        patternFound = true;
                        break;
                    }
                    startByte++;
                }

                if (patternFound) {
                    // 解析 Major 和 Minor 值
                    byte[] majorBytes = new byte[2];
                    byte[] minorBytes = new byte[2];
                    System.arraycopy(scanRecord, startByte + 20, majorBytes, 0, 2);
                    System.arraycopy(scanRecord, startByte + 22, minorBytes, 0, 2);

                    int major = (majorBytes[0] & 0xff) * 0x100 + (majorBytes[1] & 0xff);
                    int minor = (minorBytes[0] & 0xff) * 0x100 + (minorBytes[1] & 0xff);

                    // 打印 Major 和 Minor 值
                    String msg = "Major = " + major +"\n" ;
                    msg = msg + "Minor =  " + minor ;
                    Log.d("Beacon Info", msg);
                     majorMinor[0] = major;
                     majorMinor[1] = minor;
                }
            } catch (Exception e) {
                //e.printStackTrace();
            }
            return majorMinor;
        }

        public String getUUIDFromScanRecord(byte[] scanRecord) {
            String uuid = null;

            // 从扫描记录中获取 UUID 值的位置
            int startByte = 2;
            boolean patternFound = false;
            while (startByte <= 5) {
                if (((int) scanRecord[startByte + 2] & 0xff) == 0x02 && // iBeacon
                        ((int) scanRecord[startByte + 3] & 0xff) == 0x15) {

                    patternFound = true;
                    break;
                }
                startByte++;
            }

            if (patternFound) {
                // 解析 UUID 值
                byte[] uuidBytes = new byte[16];
                System.arraycopy(scanRecord, startByte + 4, uuidBytes, 0, 16);

                // 将字节数组转换为 UUID 字符串
                uuid = bytesToUUIDString(uuidBytes);
            }

            return uuid;
        }

        private String bytesToUUIDString(byte[] bytes) {
            ByteBuffer bb = ByteBuffer.wrap(bytes);
            UUID uuid = new UUID(bb.getLong(), bb.getLong());
            return uuid.toString();
        }
        // Path loss exponent
        private static final double nPow = 2.0;

        // Reference RSSI at 1 meter distance
        private static final double rssiAt1Meter = -85; // Adjust this value based on your beacon's specifications

        public  double calculateDistance(double rssi) {
            if (rssi == 0) {
                return -1.0;
            }

            double ratio = rssi * 1.0 / rssiAt1Meter;
            double distance = Math.pow(ratio, 1.0 / nPow);
            // 保留四位有效数字
            double roundedDistance = Math.round(distance * 10000.0) / 10000.0;
            return roundedDistance;
        }

        @Override
        public void onScanFailed(int errorCode)//如果掃描失敗
        {
            super.onScanFailed(errorCode);
            MainActivity.logMessage("== onScanFailed ==");

            Log.d(TAG,"onScanFailed 進入");
            final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            Log.d(TAG,"onScanFailed errorCode = "+errorCode);

            if (mBluetoothAdapter != null)
            {
                Log.d(TAG,"mBluetoothAdapter != null");
                // 一旦发生错误，除了重启蓝牙再没有其它解决办法
                mBluetoothAdapter.disable();
                new Thread(new Runnable(){
                    @Override
                    public void run() {
                        while(true) {
                            try {
                                Log.d(TAG,"Thread.sleep(500)");
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            //要等待蓝牙彻底关闭，然后再打开，才能实现重启效果
                            if(mBluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF)
                            {
                                Log.d(TAG,"mBluetoothAdapter.enable");
                                mBluetoothAdapter.enable();
                                break;
                            }
                        }
                    }
                }).start();
            }
            Log.d(TAG,"onScanFailed 出去");
        }
    }

    public void fun_distance(Double data)//將rssi值套入公式轉成距離(m)
    {
        distance = 0.0;
        distance = Math.pow(10,Math.abs(data-txpower)/(10*n));
    }


    class MyBinder extends Binder //宣告一個繼承 Binder 的類別 MyBinder
    {
        public void startDownload()
        {
            Log.d(TAG, "================startDownload() executed================");
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "================onBind================");
        return mBinder;
    }

    @Override
    public void onCreate()
    {
        MainActivity.logMessage("=====onCreate====");
        Log.d(TAG, "================onCreate================");
        mSersorManager = (SensorManager) getSystemService(SENSOR_SERVICE);//從系統服務中獲得感測器管理器
        mAccrlerometers = mSersorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);//管理器取得加速度感測器
        mGyroscope = mSersorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);//管理器取得陀螺儀感測器
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        MainActivity.logMessage("======onStartCommand====");
        Log.d(TAG, "================onStartCommand================");
        new Thread() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void run() {
                super.run();
                //...要做的事在這執行...
                Log.d(TAG,"run");

                startForeground(ongoingNotificationID, getOngoingNotification(notifiation_content+"is running"));

                handler.postDelayed(runnable, 1000);//開啟掃描 傳統藍芽 及sensor 及 BLE 的handler，x秒後開始運作
            }
        }.start();
        return super.onStartCommand(intent, flags, startId);
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    private Notification getOngoingNotification(String text)
    {
        Notification.BigTextStyle bigTextStyle = new Notification.BigTextStyle();
        bigTextStyle.setBigContentTitle(notifiation_content);
        bigTextStyle.bigText(text);

        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        String channelId = getString(R.string.app_name);
        NotificationChannel notificationChannel = new NotificationChannel(channelId, channelId, NotificationManager.IMPORTANCE_DEFAULT);
        notificationChannel.setDescription(channelId);
        notificationChannel.setSound(null, null);

        mNotificationManager.createNotificationChannel(notificationChannel);
        Notification.Builder notification = new Notification.Builder(this,channelId);

        return notification.setContentTitle(notifiation_content)
                .setContentText("正在將資訊傳至Adslab Server")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setPriority(Notification.PRIORITY_DEFAULT)
                .build();
    }
    @Override
    public void onDestroy()
    {
        Log.d(TAG, "================onDestroy================");
        super.onDestroy();
    }

}
