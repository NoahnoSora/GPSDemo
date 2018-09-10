package com.hty.gpsdemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private TextView loctext;
    private TextView stutext;
    private final String TAG = "MainActivity";
    private int ttff;
    private int lastreal;
    private int count = 100;
    private long starttime;
    private String towrite;
    private boolean doonetime = true;
    private boolean ready1 = false;
    private boolean ready2 = false;
    private List<GpsSatellite> numSatelliteList = new ArrayList<>();
    @SuppressLint("SdCardPath")
    String filePath = "/sdcard/";
    String fileName = "data.txt";
    FileOp fo = new FileOp();
    Timer timer = new Timer();
    private LocationManager locationManager;
    private LocationListener locationListener;
    @SuppressLint("HandlerLeak")
    private Handler mhandler = new Handler(){
        public void handleMessage(android.os.Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    endLocation();
                    fo.writeTxtToFile(towrite,filePath,fileName);
                    count--;
                    /*try {
                        Thread.sleep(120000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if(count>0){
                        startLocation();
                    }*/
                    if(count>0){
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                Log.i(TAG,"剩余次数:count");
                                Message msg = new Message();
                                msg.what = 2;
                                mhandler.sendMessage(msg);
                            }
                        },10000);
                    }
                    break;
                case 1://将信息打印到文本文件中
                    fo.writeTxtToFile(towrite,filePath,fileName);
                    break;
                case 2:
                    startLocation();
                default:
                    break;
            }
        }
    };
    private final GpsStatus.Listener statusListener = new GpsStatus.Listener(){
        public void onGpsStatusChanged(int event){
            @SuppressLint("MissingPermission") GpsStatus status = locationManager.getGpsStatus(null);
            updateGpsStatus(event,status);
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "onCreate");
        initLocation();
    }

    @Override
    protected void onStart(){
        super.onStart();
        Log.i(TAG,"onStart");
        startLocation();
    }

    @Override
    protected void onStop(){
        super.onStop();
        Log.i(TAG,"onStop");
        timer.cancel();
        endLocation();
    }

    private void initLocation(){
        initView();
        initListener();
    }

    private void startLocation(){
        doonetime = true;
        if(checkPermission()){
            setLocationEnable();
        }
        if(checkFilePermission()){
            Log.i(TAG,"申请文件权限");
        }
    }

    private void endLocation(){
        locationManager.removeGpsStatusListener(statusListener);
        locationManager.removeUpdates(locationListener);
    }

    private void firstFix(int type){
        switch (type){
            case 0:
                ready1 = true;
                break;
            case 1:
                ready2 = true;
                break;
            default:
                break;
            }
        if(ready1 && ready2) {
            ready1 = false;
            ready2 = false;
            Message msg = Message.obtain();
            msg.what = 0;
            mhandler.sendMessage(msg);
        }
}

    private void initView(){
        loctext = findViewById(R.id.loctext);
        stutext = findViewById(R.id.stutext);
    }

    private void checkLocation(){
        List<String> providers = locationManager.getProviders(true);
        Log.i(TAG, "Provider列表"+String.valueOf(providers));
    }

    private void initListener(){
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                updateShow(location);
                /*Log.i(TAG, "时间："+location.getTime());
                Log.i(TAG, "经度："+location.getLongitude());
                Log.i(TAG, "纬度："+location.getLatitude());
                Log.i(TAG, "海拔："+location.getAltitude());*/
            }
            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
                switch (i) {
                    //GPS状态为可见时
                    case LocationProvider.AVAILABLE:
                        Log.i(TAG, "当前GPS状态为可见状态");
                        break;
                    //GPS状态为服务区外时
                    case LocationProvider.OUT_OF_SERVICE:
                        Log.i(TAG, "当前GPS状态为服务区外状态");
                        break;
                    //GPS状态为暂停服务时
                    case LocationProvider.TEMPORARILY_UNAVAILABLE:
                        Log.i(TAG, "当前GPS状态为暂停服务状态");
                        break;
                }
            }

            @Override
            public void onProviderEnabled(String s) {
                Log.i(TAG, "GPS使能");
            }

            @Override
            public void onProviderDisabled(String s) {
                Log.i(TAG,"GPS失能");
            }
        };
    }

    private boolean checkPermission(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG,"无权限，申请");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return false;
        }else {
            return true;
        }
    }

    private boolean checkFilePermission(){
        if (ActivityCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            return false;
        }else{
            return true;
        }
    }

    private void updateShow(Location location) {
        if(location!=null){
            String sb = "首次定位时间：" +
                    ttff +
                    "\n经度：" +
                    location.getLongitude() +
                    "\n纬度：" +
                    location.getLatitude() +
                    "\n高度：" +
                    location.getAltitude() +
                    "\n速度：" +
                    location.getSpeed() +
                    "\n定位精度：" +
                    location.getAccuracy();
            loctext.setText(sb);
            if(doonetime){
                towrite = "\n经度：" +
                        location.getLongitude() +
                        "\n纬度：" +
                        location.getLatitude() +
                        "\n高度：" +
                        location.getAltitude() +
                        "\n速度：" +
                        location.getSpeed() +
                        "\n定位精度：" +
                        location.getAccuracy();
                Message msg = Message.obtain();
                msg.what = 1;
                mhandler.sendMessage(msg);
                doonetime = false;
                firstFix(0);
            }
        }
        /*else{
            StringBuilder sb = new StringBuilder();
            sb.append("首次定位时间：");
            sb.append("还没好");
            sb.append("\n经度：");
            sb.append("还没好");
            sb.append("\n纬度：");
            sb.append("还没好");
            sb.append("\n高度：");
            sb.append("还没好");
            sb.append("\n速度：");
            sb.append("还没好");
            sb.append("\n定位精度：");
            sb.append("还没好");
            loctext.setText(sb.toString());
        }*/
    }


    private void updateGpsStatus(int event,GpsStatus status){
        boolean needwrite = false;
        StringBuilder stringBuilder = new StringBuilder();
        if(status == null){
            stringBuilder.append("没有捕捉到卫星!");
        }else if(event == GpsStatus.GPS_EVENT_FIRST_FIX){
            ttff = status.getTimeToFirstFix();
            stringBuilder.append("\r\n").append("定位用时:").append(ttff).append("ms");
            stringBuilder.append("\r\n").append("定位成功!=====================================================================");
            towrite = stringBuilder.toString();
            firstFix(1);
        }else if(event == GpsStatus.GPS_EVENT_SATELLITE_STATUS){
            long nowtime = new Date().getTime();
            Log.i(TAG, "Time stamp[GPS_EVENT_SATELLITE_STATUS]->" + nowtime);
            int maxSatellites = status.getMaxSatellites();
            Iterator<GpsSatellite> it = status.getSatellites().iterator();
            numSatelliteList.clear();
            int count = 0;//记录搜索到的实际卫星数
            int real = 0;//记录有信号的卫星数
            while(it.hasNext()&& count < maxSatellites){
                GpsSatellite s = it.next();
                numSatelliteList.add(s);//将卫星信息存入队列
                count++;
                if(Float.compare(s.getSnr(),0.0f) > 0) {
                    real++;
                    stringBuilder.append("\r\n").append("卫星").append(count).append(":").append(s.getSnr()).append("db");
                }
            }
            stringBuilder.append("\r\n").append("时间:").append(nowtime - starttime).append("ms");
            stringBuilder.append("\r\n").append("卫星总数:").append(numSatelliteList.size());
            stringBuilder.append("\r\n").append("有信号卫星:").append(real);
            if(real!=lastreal) {
                needwrite = true;
                lastreal = real;
            }
            else{
                needwrite = false;
            }
            if((nowtime - starttime)>30000){
                towrite = "超过30s，定位失败" +
                        "\r\n定位结束!=====================================================================\r\n";
                stringBuilder.append("\r\n超过30s，定位结束\r\n剩余次数:").append(this.count);
                Message msg = Message.obtain();
                msg.what = 0;
                mhandler.sendMessage(msg);
            }
        }else if(event == GpsStatus.GPS_EVENT_STARTED){
            starttime = new Date().getTime();
            stringBuilder.append("定位启动!=====================================================================").append("\r\n第").append(count).append("次");
            needwrite = true;
            lastreal = 0;
            //定位启动
        }else if(event == GpsStatus.GPS_EVENT_STOPPED) {
            Log.i(TAG,"定位结束!=====================================================================");
            //定位结束
        }
        stutext.setText(stringBuilder.toString());
        if(doonetime&&needwrite){
            towrite = stringBuilder.toString();
            Message msg = Message.obtain();
            msg.what = 1;
            mhandler.sendMessage(msg);
        }
        //return stringBuilder.toString();
    }

    @SuppressLint("MissingPermission")
    private void setLocationEnable(){
        checkLocation();
        locationManager.addGpsStatusListener(statusListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        Log.i(TAG,"注册监听");
        Location mlocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(mlocation == null){
            Log.i(TAG,"没有上次的值");
        }
        else{
            Log.i(TAG, "上次时间："+mlocation.getTime());
            Log.i(TAG, "上次经度："+mlocation.getLongitude());
            Log.i(TAG, "上次纬度："+mlocation.getLatitude());
            Log.i(TAG, "上次海拔："+mlocation.getAltitude());
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG,"onRequestPermissionsResult");
        if (requestCode == 1) {
            Log.d(TAG,"requestCode == 1"+grantResults[0]+grantResults[1]);
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED){
                Log.i(TAG,"申请成功");
                setLocationEnable();
            }
        }
        else Log.d(TAG,"申请被拒绝");
    }
}
