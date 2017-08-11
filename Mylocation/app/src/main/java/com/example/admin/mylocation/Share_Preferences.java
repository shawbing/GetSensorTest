package com.example.admin.mylocation;

/**
 * Created by admin on 2017/8/4.
 * 遵循开源的准则，即GNU
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import com.example.admin.mylocation.providers.Share_Provider;
import com.example.admin.mylocation.ui.Share_Activity;
//import com.github.machinarius.preferencefragment.PreferenceFragment;

//主要用于显示主界面，并继承于Share_Activity主活动类
// 并进行页面配置的过程

public class Share_Preferences extends Share_Activity {
    //对话框显示连接错误
    private static final int DIALOG_ERROR_ACCESSIBILITY = 1;
    //对话框显示找不到参数，或参数丢失
    private static final int DIALOG_ERROR_MISSING_PARAMETERS = 2;
    //对话框显示错误提示，找不到传感器
    private static final int DIALOG_ERROR_MISSING_SENSOR = 3;

    //查看手机传感器是否可用，如果不能用，就关掉相应的事件
    // 避免软件出错
    // 初始值为false
    private static boolean is_watch = false;

//这几个怎么没有用到，难道被删掉了
    //  记录程序运行的log日志
    public static final String DEBUG_FLAG = "debug_flag";
    //消除错误的标签？？？？？
    public static final String DEBUG_TAG = "debug_tag";
//数据库处理缓慢，就将其停止
    public static final String DEBUG_DB_SLOW = "debug_db_slow";
//定义设备编号的字段的字符串
    public static final String DEVICE_ID="device_id";

//GPS定位的激活状态
    public static final String STATUS_LOCATION_GPS = "status_location_gps";
    //频率默认为180秒
    public static final String FREQUENCY_LOCATION_GPS = "frequency_location_gps";
    //精度默认为150米
    public static final String MIN_LOCATION_GPS_ACCURACY = "min_location_gps_accuracy";

//网络定位服务的激活状态，是一个勾选框
    public static final String STATUS_LOCATION_NETWORK = "status_location_network";
    //网络定位的频率，默认为300秒，其中0表示服务一直打开
    public static final String FREQUENCY_LOCATION_NETWORK = "frequency_location_network";
    //网络定位的最小精度，默认为1500米
    public static final String MIN_LOCATION_NETWORK_ACCURACY = "min_location_network_accuracy";
    //定位服务取消的时间，默认为300秒
    public static final String LOCATION_EXPIRATION_TIME="location_expiration_time";

//初始化必要的对象
    private static final Share framework = Share.getService();
    private static SensorManager mSensorMgr;
    private static Context sContext;
    private static Activity sPreferences;

    //加载对话框，将错误提示对话框集中起来，统一调用
    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        //允许自定义对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        switch(id) {
            case DIALOG_ERROR_ACCESSIBILITY:
                builder.setMessage("Please activate Share on the Accessibility Services!");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        //进入手机设置界面，进行设置，
                        // 允许本软件拥有设置权限
                        Intent accessibilitySettings = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
                        accessibilitySettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT );
                        startActivity(accessibilitySettings);
                    }
                });
                dialog = builder.create();
                break;
            case DIALOG_ERROR_MISSING_PARAMETERS:
                builder.setMessage("Some parameters are missing...");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                dialog = builder.create();
                break;
            case DIALOG_ERROR_MISSING_SENSOR:
                builder.setMessage("This device is missing this sensor.");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                dialog = builder.create();
                break;
        }
        return dialog;
    }

//加载主页面，并设定相应的动作监听
// 这也算一个主要的类，用于设置页面片段，即fragment，这里填充设置页面
    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            //加载设置页面
            addPreferencesFromResource(R.xml.share_preferences);
            servicesOptions();
        }
//看这里还需要将设备的编号加进来Device_ID 的Preferences设置界面加入

        //所有的页面组件
        private void servicesOptions() {
            locations();
        }

//、、定位选项的UI设计
//要找的定位信息在这里
//要找的定位信息在这里
//要找的定位信息在这里
        private void locations() {
            final PreferenceScreen locations = (PreferenceScreen) findPreference("locations");
            if( is_watch ) {
                locations.setEnabled(false);
                return;
            }

            final CheckBoxPreference location_gps = (CheckBoxPreference) findPreference(Share_Preferences.STATUS_LOCATION_GPS);
            //检查定位设置是否成功，是否启用GPS
            location_gps.setChecked(Share.getSetting(sContext, Share_Preferences.STATUS_LOCATION_GPS).equals("true"));
            location_gps.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {

                    LocationManager localMng = (LocationManager) sContext.getSystemService(LOCATION_SERVICE);
                    List<String> providers = localMng.getAllProviders();

                    if( ! providers.contains(LocationManager.GPS_PROVIDER) ) {
                        sPreferences.showDialog(DIALOG_ERROR_MISSING_SENSOR);
                        location_gps.setChecked(false);
                        Share.setSetting(sContext, Share_Preferences.STATUS_LOCATION_GPS, false);
                        return false;
                    }

                    Share.setSetting(sContext, Share_Preferences.STATUS_LOCATION_GPS,location_gps.isChecked());
                    if(location_gps.isChecked()) {
                        framework.startLocations();
                    }else {
                        framework.stopLocations();
                    }
                    return true;
                }
            });
//、勾选，检查是否启用网络定位 即NETWORK
            final CheckBoxPreference location_network = (CheckBoxPreference) findPreference(Share_Preferences.STATUS_LOCATION_NETWORK);
            location_network.setChecked(Share.getSetting(sContext, Share_Preferences.STATUS_LOCATION_NETWORK).equals("true"));
            location_network.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {

                    LocationManager localMng = (LocationManager) sContext.getSystemService(LOCATION_SERVICE);
                    //获取系统所有的信息提供接口
                    List<String> providers = localMng.getAllProviders();
                //如果检查到手机中没有网络服务，就调用DIALOG
                    // 提示没有网络服务，
                    // 进而前去引导用户打开网络服务
                    if( ! providers.contains(LocationManager.NETWORK_PROVIDER) ) {
                        sPreferences.showDialog(DIALOG_ERROR_MISSING_SENSOR);
                        //然后将这个勾选框设置为空，即不勾选状态
                        location_gps.setChecked(false);
                        Share.setSetting(sContext, Share_Preferences.STATUS_LOCATION_NETWORK, false);
                        return false;
                    }

                    Share.setSetting(sContext, Share_Preferences.STATUS_LOCATION_NETWORK, location_network.isChecked());
                    //要是网络打开了，
                    // 就通过Share主服务类来启动定位，开始定位服务
                    if(location_network.isChecked()) {
                        framework.startLocations();
                    }else {
                        framework.stopLocations();
                    }
                    return true;
                }
            });
        //编辑请求获取定位信息的频率
            final EditTextPreference gpsInterval = (EditTextPreference) findPreference(Share_Preferences.FREQUENCY_LOCATION_GPS);
            //如果又返回值
            if( Share.getSetting(sContext, Share_Preferences.FREQUENCY_LOCATION_GPS).length() > 0 ) {
                gpsInterval.setSummary(Share.getSetting(sContext, Share_Preferences.FREQUENCY_LOCATION_GPS) + " seconds");
            }
            //设置文本
            gpsInterval.setText(Share.getSetting(sContext, Share_Preferences.FREQUENCY_LOCATION_GPS));
            gpsInterval.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Share.setSetting(sContext, Share_Preferences.FREQUENCY_LOCATION_GPS, (String) newValue);
                    gpsInterval.setSummary((String) newValue + " seconds");
                    //但是那里有计时器，用于计算时间的流动，间断性的获取定位信息
                    framework.startLocations();
                    return true;
                }
            });
        //设置网络定位服务的获取频率
            final EditTextPreference networkInterval = (EditTextPreference) findPreference(Share_Preferences.FREQUENCY_LOCATION_NETWORK);
            if( Share.getSetting(sContext, Share_Preferences.FREQUENCY_LOCATION_NETWORK).length() > 0 ) {
                //设置编辑框的总结说明栏，同于提示当前信息
                networkInterval.setSummary(Share.getSetting(sContext, Share_Preferences.FREQUENCY_LOCATION_NETWORK) + " seconds");
            }
            networkInterval.setText(Share.getSetting(sContext, Share_Preferences.FREQUENCY_LOCATION_NETWORK));
            networkInterval.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Share.setSetting(sContext, Share_Preferences.FREQUENCY_LOCATION_NETWORK, (String) newValue);
                    networkInterval.setSummary((String) newValue + " seconds");
                    framework.startLocations();
                    return true;
                }
            });
        //设置GPS定位的精度
            final EditTextPreference gpsAccuracy = (EditTextPreference) findPreference(Share_Preferences.MIN_LOCATION_GPS_ACCURACY);
            //如果已经有设置信息了，就以总结说明的形式表现出来
            if( Share.getSetting(sContext, Share_Preferences.MIN_LOCATION_GPS_ACCURACY).length() > 0 ) {
                gpsAccuracy.setSummary(Share.getSetting(sContext, Share_Preferences.MIN_LOCATION_GPS_ACCURACY) + " meters");
            }
            gpsAccuracy.setText(Share.getSetting(sContext, Share_Preferences.MIN_LOCATION_GPS_ACCURACY));
            gpsAccuracy.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Share.setSetting(sContext, Share_Preferences.MIN_LOCATION_GPS_ACCURACY, (String) newValue);
                    gpsAccuracy.setSummary((String) newValue + " meters");
                    //、、类似于重新启动
                    framework.startLocations();
                    return true;
                }
            });
        //设置网络定位服务的精度
            final EditTextPreference networkAccuracy = (EditTextPreference) findPreference(Share_Preferences.MIN_LOCATION_NETWORK_ACCURACY);
            if( Share.getSetting(sContext, Share_Preferences.MIN_LOCATION_NETWORK_ACCURACY).length() > 0 ) {
                networkAccuracy.setSummary(Share.getSetting(sContext, Share_Preferences.MIN_LOCATION_NETWORK_ACCURACY) + " meters");
            }
            networkAccuracy.setText(Share.getSetting(sContext, Share_Preferences.MIN_LOCATION_NETWORK_ACCURACY));
            //监听这个文本框的改变
            // 一旦改变，就重新启动定位服务
            networkAccuracy.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Share.setSetting(sContext, Share_Preferences.MIN_LOCATION_NETWORK_ACCURACY, (String) newValue);
                    networkAccuracy.setSummary((String) newValue + " meters");
                    framework.startLocations();
                    return true;
                }
            });
        //编辑定位服务失效的时间
            final EditTextPreference expirateTime = (EditTextPreference) findPreference(Share_Preferences.LOCATION_EXPIRATION_TIME);
            if( Share.getSetting(sContext, Share_Preferences.LOCATION_EXPIRATION_TIME).length() > 0 ) {
                expirateTime.setSummary(Share.getSetting(sContext, Share_Preferences.LOCATION_EXPIRATION_TIME) + " seconds");
            }
            expirateTime.setText(Share.getSetting(sContext, Share_Preferences.LOCATION_EXPIRATION_TIME));
            expirateTime.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Share.setSetting(sContext, Share_Preferences.LOCATION_EXPIRATION_TIME, (String) newValue);
                    expirateTime.setSummary((String) newValue + " seconds");
                    framework.startLocations();
                    return true;
                }
            });
        }

    }


    //这个相当于主函数，用于初始加载页面
    // 并实例化一些需要用到的参数，如：SensorManager,Context,
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
        //检查是够能够监听
        is_watch = Share.is_watch(this);
        sContext = getApplicationContext();
        sPreferences = this;

        //开始启动Share主服务
        Intent startAware = new Intent( getApplicationContext(), Share.class );
        startService(startAware);

        SharedPreferences prefs = getSharedPreferences( getPackageName(), Context.MODE_PRIVATE );

        //如果页面里的设置状态全为空，那就要进行默认值的设置
        if( prefs.getAll().isEmpty() && Share.getSetting(getApplicationContext(), Share_Preferences.DEVICE_ID).length() == 0 ) {
            PreferenceManager.setDefaultValues(getApplicationContext(), getPackageName(), Context.MODE_PRIVATE, R.xml.share_preferences, true);
            //提交更改
            prefs.edit().commit();
        } else {
            //否则还要用到
            PreferenceManager.setDefaultValues(getApplicationContext(), getPackageName(), Context.MODE_PRIVATE, R.xml.share_preferences, false);
        }

        Map<String,?> defaults = prefs.getAll();
        for(Map.Entry<String, ?> entry : defaults.entrySet()) {
            if( Share.getSetting(getApplicationContext(), entry.getKey()).length() == 0 ) {
                Share.setSetting(getApplicationContext(), entry.getKey(), entry.getValue());
            }
        }
//要用到生成的设备ID，进行针对性的设置
        if( Share.getSetting(getApplicationContext(), Share_Preferences.DEVICE_ID).length() == 0 ) {
            UUID uuid = UUID.randomUUID();
            Share.setSetting(getApplicationContext(), Share_Preferences.DEVICE_ID, uuid.toString());
        }
    //加载主布局文件
        setContentView(R.layout.share_ui);
//下面这句是啥？？？？？执行事务
        getSupportFragmentManager().executePendingTransactions();
    }
    //又要进行设置
//这里要综合用到Share.getSetting 以及
// 本文件中的APPLICATIONS和KEYBOARD。不过好像已将删了
// 怎样才能调用这个函数？？？
    @Override
    protected void onResume() {
        super.onResume();

        if( Share.getSetting( getApplicationContext(), Share_Preferences.STATUS_APPLICATIONS).equals("true") || Share.getSetting( getApplicationContext(), Share_Preferences.STATUS_KEYBOARD).equals("true") && ! Applications.isAccessibilityServiceActive(getApplicationContext()) ) {
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext());
            mBuilder.setSmallIcon(R.drawable.ic_stat_aware_accessibility);
            mBuilder.setContentTitle("aware configuration");
            mBuilder.setContentText(getResources().getString(R.string.aware_activate_accessibility));
            mBuilder.setDefaults(Notification.DEFAULT_ALL);
            mBuilder.setAutoCancel(true);

            Intent accessibilitySettings = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
            accessibilitySettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            PendingIntent clickIntent = PendingIntent.getActivity(getApplicationContext(), 0, accessibilitySettings, PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(clickIntent);
            NotificationManager notManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notManager.notify(Applications.ACCESSIBILITY_NOTIFICATION_ID, mBuilder.build());
        }
//这里的Async_StudyData()下落不明？？？
        if( Share.getSetting(getApplicationContext(), "study_id").length() > 0 ) {
            new Async_StudyData().execute(Share.getSetting(this, Share_Preferences.WEBSERVICE_SERVER));
        }
    }

    //请求加入学习，
    // 启动本文件中的StudyConfig服务
    // 并将数据上传，还要求返回返回结果值
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if( requestCode == REQUEST_JOIN_STUDY ) {
            if( resultCode == RESULT_OK ) {
                //这里的StudyConfig在上面
                // 引用SharePreferences中的，
                // 这个服务允许插件和应用
                // 向网上相应的学习平台传输数据
                Intent study_config = new Intent(this, StudyConfig.class);
                study_config.putExtra("study_url", data.getStringExtra("study_url"));
                startService(study_config);
            }
        }
    }

    //为客户进行设置
    // 包括默认设置  貌似要联网？？？
    // 不过要找出哪些是插件
    // 这里要用到Share_Provider类
    protected static void applySettings(Context context,
                                        JSONArray configs )
    {
        //已经获取到数据，都已经在施行设置了
        // 这里的插件到底指的是哪些？？？？
        // ？？？？？要把插件找出来
        //默认的设置效果，即初始值或缺省值
        Share.reset(context);

        //先从网络？？？（也可以是本地吧）中获取到用户的设置历史，
        // 然后
        // 利用JSON数据组，应用或加载新的设置
        JSONArray plugins = new JSONArray();
        JSONArray sensors = new JSONArray();

        for( int i = 0; i<configs.length(); i++ ) {
            try {
                JSONObject element = configs.getJSONObject(i);
                if( element.has("plugins") ) {
                    plugins = element.getJSONArray("plugins");
                }
                if( element.has("sensors")) {
                    sensors = element.getJSONArray("sensors");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        //这里要引用到share的设置函数
        //先开始传感器的设置
        for( int i=0; i < sensors.length(); i++ ) {
            try {
                JSONObject sensor_config = sensors.getJSONObject(i);
                Share.setSetting( context, sensor_config.getString("setting"), sensor_config.get("value") );
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        //接着再进行插件的设置
        // 这里引用Shar_Provider里面的Share_Setting的
        // :Setting_KEY  SETTING_VALUES 以及SETTING_PACKAGE_NAME
        //以及其中的Share_Provider.Aware_Settings.CONTENT_URI
        //
        ArrayList<String> active_plugins = new ArrayList<String>();
        for( int i=0; i < plugins.length(); i++ ) {
            try{
                JSONObject plugin_config = plugins.getJSONObject(i);
                String package_name = plugin_config.getString("plugin");
                active_plugins.add(package_name);

                JSONArray plugin_settings = plugin_config.getJSONArray("settings");
                for(int j=0; j<plugin_settings.length(); j++) {
                    JSONObject plugin_setting = plugin_settings.getJSONObject(j);

                    ContentValues newSettings = new ContentValues();
                    newSettings.put(Share_Provider.Share_Settings
                            .SETTING_KEY, plugin_setting.getString("setting"));
                    newSettings.put(Share_Provider.Aware_Settings
                            .SETTING_VALUE, plugin_setting.get("value").toString() );
                    newSettings.put(Share_Provider.Aware_Settings
                            .SETTING_PACKAGE_NAME, package_name);
                    context.getContentResolver().insert(Share_Provider
                            .Aware_Settings.CONTENT_URI, newSettings);
                }
            }catch( JSONException e ) {
                e.printStackTrace();
            }
        }

        //现在开始启动插件
        for( String package_name : active_plugins ) {
            Share.startPlugin(context, package_name);
        }
    }

    //下面删除获取网上学习功能模块的类
}
