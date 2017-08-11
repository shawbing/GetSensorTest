package com.example.admin.mylocation.Test;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Time;
import android.widget.EditText;

import com.example.admin.mylocation.R;

import java.text.SimpleDateFormat;

public class MainActivity extends AppCompatActivity {

    //
    LocationManager locationManager;
    //
    EditText showtime;
    EditText show;
    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //展示当前动态时间
        showtime=(EditText) findViewById(R.id.showtime);

        CountDownTimer timer=new CountDownTimer(Integer.MAX_VALUE,1000) {
            @Override
            public void onTick(long l) {
                //showtime.setText(getTime());
                //Time time=new Time("UMT+8");
                //time.setToNow();
                showtime.setText(""+new java.util.Date());
            }

            @Override
            public void onFinish() {

            }
        };
        timer.start();
        //
        show=(EditText) findViewById(R.id.show);
        //
        locationManager=(LocationManager)
                            getSystemService(Context.LOCATION_SERVICE);
        //
        // 此处就需要定位的权限    怎么还是不行？？？？？

        Location location=locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        //、、
        updateView(location);
        //
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                3000, 8, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        //
                        updateView(location);
                    }

                    @Override
                    public void onStatusChanged(String s, int i, Bundle bundle) {
                    }
                    @Override
                    public void onProviderEnabled(String s) {
                        //
                        updateView(locationManager.getLastKnownLocation(s));
                    }

                    @Override
                    public void onProviderDisabled(String s) {
                        //
                        updateView(null);
                    }
                });

    }
    /*
    public static String getTime()
    {
        //
        Time time=new Time();
        time.setToNow();
        //SimpleDateFormat format=new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        //String strtime= format.format(time);
        return strtime;
    }
    */
    /*
        //判断手机是否获取到GPS的权限，即定位服务是否可用，用户是否打开GPS
        public final static boolean isOpen(Context context)
        {
            LocationManager locationManager;
            locationManager=(LocationManager)
                                    context.getSystemService(Context.LOCATION_SERVICE);
            //
            boolean isGPS=locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetWork=locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            if (isNetWork ||isGPS) {
                return true;
            }
          else
                return false;
        }
        /*

        */
    // 强制打开GPS服务
    public final static void openGPS(Context context)
    {
        Intent GPSintent=new Intent();
        GPSintent.setClassName("com.android.settings",
                "com.android.settings.widget.SettingsAppWidgetProvider");
        GPSintent.addCategory("android.intent.category.ALTERNATIVE");
        //
        GPSintent.setData(Uri.parse("custom.3"));
        try {
            PendingIntent.getBroadcast(context,0,GPSintent,0).send();
        }catch (PendingIntent.CanceledException ce)
        {   ce.printStackTrace();}
    }
    //
    public void updateView(Location newLocation)
    {
        if (newLocation!=null)
        {
            StringBuilder information=new StringBuilder();
            information.append("当前的位置信息：\n");
            information.append("经度：");
            information.append(newLocation.getLongitude());
            information.append("\n纬度：");
            information.append(newLocation.getLatitude());
            information.append("\n高度：");
            information.append(newLocation.getAltitude());
            information.append("\n速度：");
            information.append(newLocation.getSpeed());
            information.append("\n方向：");
            information.append(newLocation.getBearing());
            show.setText(information);
        }
        else
            show.setText("");
    }
}
