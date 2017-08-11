package com.example.admin.mylocation;

/**
 * Created by admin on 2017/8/4.
 */

import android.app.AlarmManager;
import android.app.DownloadManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.net.ParseException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
//import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

//import org.apache.http.HttpResponse;
import org.apache.http.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/*引用自己创建的包，看哪些是必须的？
aware_providers主要用于存储基本的状态信息：设备信息，框架设置，插件（感觉插件用得多）
import com.aware.providers.Aware_Provider;
import com.aware.providers.Aware_Provider.Aware_Device;
import com.aware.providers.Aware_Provider.Aware_Plugins;
import com.aware.providers.Aware_Provider.Aware_Settings;
import com.aware.ui.Plugins_Manager;
import com.aware.ui.Stream_UI;
import com.aware.utils.Aware_Plugin;
*/

//本软件的核心类，相当于信息控制中心，
// 用于启动和管理所有的服务
public class Share extends Service {

//消除错误的标记，默认值为否
    public static boolean DEBUG = false;

//消除错误的标签TAG，默认名为SHARE
// 可能这里有误
    public static String TAG = "SHARE";

//申明 广播动作：可获取到 软件的内容信息。主要是往外广播信息
    public static final String ACTION_SHARE_DEVICE_INFORMATION = "ACTION_SHARE_DEVICE_INFORMATION";

//接收所有模式组件的数据
// 并将这些数据传送给已经定义的网络浏览器
    public static final String ACTION_SHARE_SYNC_DATA = "ACTION_SHARE_SYNC_DATA";

//接收所有模式组件发出的广播
// 通知删除已经收集到关于设备的数据
    public static final String ACTION_SHARE_CLEAR_DATA = "ACTION_SHARE_CLEAR_DATA";

//接收广播通知：刷新界面中活动的传感器
    public static final String ACTION_SHARE_REFRESH = "ACTION_SHARE_REFRESH";

//接收广播通知：插件一定要应用内容广播接收器
// 去分享当前的状态信息
    public static final String ACTION_SHARE_CURRENT_CONTEXT = "ACTION_SHARE_CURRENT_CONTEXT";

//关闭所有的插件
    public static final String ACTION_SHARE_STOP_PLUGINS = "ACTION_SHARE_STOP_PLUGINS";

//停止所有的传感器服务
    public static final String ACTION_SHARE_STOP_SENSORS = "ACTION_SHARE_STOP_SENSORS";

//接收所有模式的广播通知：清除相应的内容提供者存储的历史数据
    public static final String ACTION_SHARE_SPACE_MAINTENANCE = "ACTION_SHARE_SPACE_MAINTENANCE";

//通过插件管理器来更新界面（UI）
    public static final String ACTION_SHARE_PLUGIN_MANAGER_REFRESH = "ACTION_SHARE_PLUGIN_MANAGER_REFRESH";

//只能手表？？？感觉不像是手表的意思
// 通过Wear来了解那个传感器服务处于激活状态，能够与手机同步
// 来进行配置的改变
// 另外还有两个额外的变量：用于设置的 关键字段 和 相应的值
    public static final String ACTION_SHARE_CONFIG_CHANGED = "ACTION_SHARE_CONFIG_CHANGED";
    public static final String EXTRA_CONFIG_SETTING = "extra_config_setting";
    public static final String EXTRA_CONFIG_VALUE = "extra_config_value";

//用于记录用户所更新下载的，以便能帮助用户进行安装，更新功能
    private static long SHARE_FRAMEWORK_DOWNLOAD_ID = 0;

    //万一有许多插件依赖需要下载安装，
// 需要一个插件的下载队列来统一所有下载，编排顺序
    private static ArrayList<Long> SHARE_PLUGIN_DOWNLOAD_IDS = new ArrayList<Long>();

//初始化主要的对象
    //第一个alarmmanager是干啥的？？？
    // 百度了一下，原来是闹钟，即：计时器
    private static AlarmManager alarmManager = null;
    //已经封装好的intent，包含动作。看对象就晓得是要“重复
    private static PendingIntent repeatingIntent = null;
    private static Context shareContext = null;

    //用于转换（或是说启动）到网络上传数据的服务
    private static PendingIntent webserviceUploadIntent = null;

//监测程序状态
    private static Intent shareStatusMonitor = null;

    private static Intent locationsSrv = null;

    private static Intent networkSrv = null;
    private static Intent trafficSrv = null;

    private static Intent wifiSrv = null;

//上次的监视频率
    private final String PREF_FREQUENCY_WATCHDOG = "frequency_watchdog";
    private final String PREF_LAST_UPDATE = "last_update";
    //上次的同步
    private final String PREF_LAST_SYNC = "last_sync";
    //定义固定频率  为 五分钟
    private final int CONST_FREQUENCY_WATCHDOG = 5 * 60;

    //SharedPreferences这是干啥的？？？？？
    private SharedPreferences share_preferences;

//单例化对象，这里只是一个对象，还没看见单例化的标记singleton
    // 单例模式只允许创建一个对象，因此节省内存，加快对象访问速度，
// 因此对象需要被公用的场合适合使用，如多个模块使用同一个数据源连接对象等等
    private static Share shareSrv = Share.getService();

//获取到单例化的框架对象
    public static Share getService() {
        if( shareSrv == null ) shareSrv = new Share();
        return shareSrv;
    }

//、、绑定本服务，避免销毁
    private final IBinder serviceBinder = new ServiceBinder();
    public class ServiceBinder extends Binder {
//里面这个就有点不懂了？？？而且还有点问题
        Share getService() {
            return Share.getService();
        }
    }
//必须要的回调方法，因为上面已对IBinder初始化
    @Override
    public IBinder onBind(Intent intent) {
        return serviceBinder;
    }
//本类必须回调的初始化方法
    @Override
    public void onCreate()
    {
        super.onCreate();
//实例化主要的对象
        shareContext = getApplicationContext();
        //实例化计时器，闹钟管理器
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
//添加动作转接命令接口
//但是这个会转到哪里去？？？
        //用于打开内存卡
        IntentFilter filter = new IntentFilter();
//插入SD卡并且已正确安装（识别）时发出广播：扩展介质被插入，而且已经被挂载。
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        //MOUNTED表示挂载的意思，即外接设备已连接
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addDataScheme("file");//表明数据的样式
//本类注册存储卡的监听，用于获取信息
        shareContext.registerReceiver(storage_BR, filter);

// 这些清理和更新数据，居然和 下载管理器 绑在一起
// 可能不需要这个
        filter = new IntentFilter();
        filter.addAction(Share.ACTION_SHARE_CLEAR_DATA);
        filter.addAction(Share.ACTION_SHARE_REFRESH);
        filter.addAction(Share.ACTION_SHARE_SYNC_DATA);
        filter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        shareContext.registerReceiver(aware_BR, filter);//进行注册

///同步数据，即数据上传
/// 又要用到网络了，也有可能暂时不需要这个
        Intent synchronise = new Intent(Share.ACTION_SHARE_SYNC_DATA);
        webserviceUploadIntent = PendingIntent.getBroadcast(
                            getApplicationContext(), 0, synchronise, 0);
//要是获取不到外接存储器，就直接关掉这个功能
        if( ! Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) ) {
            stopSelf();//貌似关掉整个服务
            return;
        }
//双引号里面那个是什么鬼？？？
// 是要加载核心，主要用于保存用户的设置参数，新建一个文件
// 保存格式为xml，其名称为引号里面的，即第一个参数
        share_preferences = getSharedPreferences("share_core_prefs", MODE_PRIVATE);
        if( share_preferences.getAll().isEmpty() ) {
            //检查细节类是否为空
            //就要开始获取编辑器，记录信息
            SharedPreferences.Editor editor = share_preferences.edit();
            editor.putInt(PREF_FREQUENCY_WATCHDOG, CONST_FREQUENCY_WATCHDOG);
            //将上次同步和更新的信息填入，默认值为0
            editor.putLong(PREF_LAST_SYNC, 0);
            editor.putLong(PREF_LAST_UPDATE, 0);
            editor.commit();//提交成功，完成编辑
        }

//为所有插件 进行默认设置
        SharedPreferences prefs = getSharedPreferences(
                            getPackageName(), Context.MODE_PRIVATE );
        if( prefs.getAll().isEmpty() &&
                Share.getSetting(getApplicationContext(),
                        Share_Preferences.DEVICE_ID).length() == 0 ) {
            //要是所有的节点值没有，那么就为所有 进行默认值 设置
            // 先是获取包名，再则定义为私有模式，加载布局文件
            // 最后用true来表示，启用默认值，也称为缺省值
            PreferenceManager.setDefaultValues(getApplicationContext(),
                    getPackageName(), Context.MODE_PRIVATE,
                    R.xml.share_preferences, true);
            //提交更改信息
            prefs.edit().commit();
        }
        else {
            PreferenceManager.setDefaultValues(getApplicationContext(),
                    getPackageName(), Context.MODE_PRIVATE,
                    R.xml.share_preferences, false);
        }
//真正开始实行默认值的设置
//真正开始实行默认值的设置
        Map<String,?> defaults = prefs.getAll();
        for(Map.Entry<String, ?> entry : defaults.entrySet()) {
            if( Share.getSetting(getApplicationContext(),
                    entry.getKey()).length() == 0 )
            //要是当前该节点设置的长度为零，即为空。不过返回的什么
            {
                //那就进行默认值设置
                Share.setSetting(getApplicationContext(),
                        entry.getKey(), entry.getValue());
            }
        }
//检测设备设置中的编号有没有，若没有就直接新建一个（一个随机值）
        if( Share.getSetting(getApplicationContext(),
                    Share_Preferences.DEVICE_ID).length() == 0 )
        {
            UUID uuid = UUID.randomUUID();//然后就对其进行设置
            Share.setSetting(getApplicationContext(),
                    Share_Preferences.DEVICE_ID, uuid.toString());
        }
//DEBUG是bool类型的
        DEBUG = Share.getSetting(shareContext,
                    Share_Preferences.DEBUG_FLAG).equals("true");
//而TAG是String类型的，初始值为SHARE。如果设置中存在，那就用设置中的值
        TAG = Share.getSetting(shareContext,
                Share_Preferences.DEBUG_TAG).length()>0
                ? Share.getSetting(shareContext,
                Share_Preferences.DEBUG_TAG):TAG;
//调用函数，获取设备的信息
        get_device_info();

//要是主界面函数的DEBUG_FLAG为true，说明程序运行没有错误，能进入主界面
        if( Share.DEBUG ) Log.d(TAG,"主框架已经成功创立");

//只有官方版本的软件才能运行这一步。可以不要了
// 定时重复执行，开始同步数据的操作
/*
*/
    //onCreate函数到此为止
    }

//同步网络数据，这里也可以不要了
/*
*/
    //查询移动设备的信息，即时间，ID，平台，版本，设备，表现ID，硬件……
    private void get_device_info() {
        //这里的Share_Device是在providers中的Share_Provider
        Cursor shareContextDevice = shareContext.getContentResolver()
                .query(Share_Device.CONTENT_URI, null, null, null, null);
        //要是查询到设备的信息内容的话
        if( shareContextDevice == null ||
                ! shareContextDevice.moveToFirst() )
        {
            ContentValues rowData = new ContentValues();
            rowData.put("timestamp", System.currentTimeMillis());//时间戳
            rowData.put("device_id", Share.getSetting(shareContext,
                    Share_Preferences.DEVICE_ID));//设备编号
            rowData.put("board", Build.BOARD);//平台
            rowData.put("brand", Build.BRAND);//版本
            rowData.put("device", Build.DEVICE);//设备
            rowData.put("build_id", Build.DISPLAY);//表现id
            rowData.put("hardware", Build.HARDWARE);//硬件
            rowData.put("manufacturer", Build.MANUFACTURER);//制造商
            rowData.put("model", Build.MODEL);//模式
            rowData.put("product", Build.PRODUCT);//产品
            rowData.put("serial", Build.SERIAL);//系列
            rowData.put("release", Build.VERSION.RELEASE);//发布
            rowData.put("release_type", Build.TYPE);//发布的型号
            rowData.put("sdk", Build.VERSION.SDK_INT);//软件开发工具包

            try {
                shareContext.getContentResolver().insert(
                        Share_Device.CONTENT_URI, rowData);
//怎么就进行信息广播了呢???哪里需要这些信息？
                Intent deviceData = new Intent(ACTION_SHARE_DEVICE_INFORMATION);
                sendBroadcast(deviceData);
                //如果操作成功，就编写到数据库日志中，记录软件的操作
//DEBUG的值会在哪里被改变
                if( Share.DEBUG ) Log.d(TAG, "Device information:"
                                                + rowData.toString());
//怎么还会涉及到数据库的操作问题、、
            }catch( SQLiteException e ) {
                if(Share.DEBUG) Log.d(TAG,e.getMessage());
            }catch( SQLException e ) {
                if(Share.DEBUG) Log.d(TAG,e.getMessage());
            }
        }
        //但凡用到数据库操作
        // 都要随时注意到是否关闭的问题，
        // 出于安全和节约资源的考虑
        if( shareContextDevice != null && !shareContextDevice.isClosed())
            shareContextDevice.close();
    }

    //检查获取手机比较隐私的信息的权限   而非传感器的信息
//检查获取手机比较隐私的信息的权限  其他类中也会经常调用的
    public static boolean is_watch(Context c) {
        boolean is_watch = false;
        //又要用到查询语句，难道是只限制返回一条数据
        Cursor device = c.getContentResolver().query(
                Aware_Provider.Aware_Device.CONTENT_URI,
                null, null, null, "1 LIMIT 1");
        if( device != null && device.moveToFirst() ) {
//判定的方法就是：是否包含版本号，一般以W,W.1,W.2……来呈现
            is_watch = device.getString(device.getColumnIndex(
                    Aware_Device.RELEASE)).contains("W");
        }
        if( device != null && ! device.isClosed() ) device.close();
        return is_watch;
    }

    //回调“开始命令”,里面涉及到网络和插件
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //首先检查存储卡是否成功挂载（能否利用）
        if( Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) )
        {
            DEBUG = Share.getSetting(shareContext,
                    Share_Preferences.DEBUG_FLAG).equals("true");
            //要是设置里有标签的话，就用这个标签；没有的话，就用初始值
            TAG = Share.getSetting(shareContext,
                    Share_Preferences.DEBUG_TAG).length()>0
                    ? Share.getSetting(
                            shareContext, Share_Preferences.DEBUG_TAG)
                    :TAG;
            //若能调试成功，那就记录在日志中
            if( Share.DEBUG ) Log.d(TAG,"share framework is active...");

            //插件希望也能够打开所有的服务，，这在其设置中已经申明了的
            startAllServices();

       //官方版本的软件需要注意到：保证插件一直处于运行的状态
            if( getPackageName().equals("com.example.admin.mylocation") )
            {
                //查询那些还在运行状态的插件
                // 又要调用到Share_Provider
                Cursor enabled_plugins = getContentResolver().query(
                        Share_Plugins.CONTENT_URI, null,
                        Aware_Plugins.PLUGIN_STATUS + "="
                                        + Aware_Plugin.STATUS_PLUGIN_ON,
                                null, null);
                if( enabled_plugins != null && enabled_plugins.moveToFirst() ) {
                    //一个一个的找，一旦没打开，就马上根据名字打开
                    do {
                        startPlugin(getApplicationContext(),
                                enabled_plugins.getString(enabled_plugins
                                        .getColumnIndex(Aware_Plugins
                                                .PLUGIN_PACKAGE_NAME)));
                    }while(enabled_plugins.moveToNext());
                }
                if( enabled_plugins != null && ! enabled_plugins.isClosed() )
                    enabled_plugins.close();

//如果这里已经进行了自动更新了，那就
                if( Share.getSetting(getApplicationContext(),
                        Share_Preferences.ShARE_AUTO_UPDATE).equals("true") )
                {
                    //每六个小时检查一下更新状态
                    if( share_preferences.getLong(PREF_LAST_UPDATE, 0) == 0
                            || (share_preferences.getLong(PREF_LAST_UPDATE, 0) > 0
                                && System.currentTimeMillis()-share_preferences
                                    .getLong(PREF_LAST_UPDATE, 0)
                                > 6*60*60*1000)
                       )
                    { //要是没有上次更新 或者已更新了六个小时，就马上执行，
//联网检查服务器端是否有更新的版本
                        new Update_Check().execute();
                        //然后将这次更新操作写入到记录文档中
                        SharedPreferences.Editor editor =
                                        share_preferences.edit();
                        editor.putLong(PREF_LAST_UPDATE,
                                        System.currentTimeMillis());
                        editor.commit();
                    }
                }
            }
//要是网络服务处于打开的状态
            if( Share.getSetting(getApplicationContext(),
                    Share_Preferences.STATUS_WEBSERVICE).equals("true") )
            {
                int frequency_webservice = Integer.parseInt(
                        Share.getSetting(getApplicationContext(),
                                Share_Preferences.FREQUENCY_WEBSERVICE));
                if( frequency_webservice == 0 ) {
                    if(DEBUG) {
                        Log.d(TAG,"Data sync is disabled.");
                    }
                    alarmManager.cancel(webserviceUploadIntent);
                } else if( frequency_webservice > 0 ) {
                    //这里进行修正，要是以前没设置闹钟的话，
                    // 那就进行一次吧  网络服务的频率按分钟算
                    if( share_preferences.getLong(PREF_LAST_SYNC, 0) == 0
                            || (share_preferences.getLong(PREF_LAST_SYNC, 0) > 0
                                && System.currentTimeMillis() - share_preferences
                            .getLong(PREF_LAST_SYNC, 0)
                            > frequency_webservice * 60 * 1000 )
                      )
                    {
                        if( DEBUG ) {
                            Log.d(TAG,"Data sync every "
                                    + frequency_webservice + " minute(s)");
                        }
                        //这样相当于记录每一步历史操作，便于以后查阅
                        SharedPreferences.Editor editor = share_preferences.edit();
                        editor.putLong(PREF_LAST_SYNC, System.currentTimeMillis());
                        editor.commit();
                        alarmManager.setInexactRepeating(AlarmManager.RTC,
                                share_preferences.getLong(PREF_LAST_SYNC, 0),
                                frequency_webservice * 60 * 1000,
                                webserviceUploadIntent);
                    }
                }

                //客户端检查 学习栏 是否处于运行状态
                if( getPackageName().equals("com.example.admin.mylocation") )
                {
                    new Study_Check().execute();
                }
            }
            //清理历史数据
            if( ! Share.getSetting(getApplicationContext(),
                    Share_Preferences.FREQUENCY_CLEAN_OLD_DATA).equals("0") ) {
                Intent dataCleaning = new Intent(
                        ACTION_AWARE_SPACE_MAINTENANCE);
                shareContext.sendBroadcast(dataCleaning);
            }
        }
        else //如果不能用存储卡，那就关掉所有的服务和插件吧
            {
            //关掉所有服务    、、
            stopAllServices();
            //关掉所有插件。要先查询哪些处于打开状态
            Cursor enabled_plugins = getContentResolver().query(
                    Share_Plugins.CONTENT_URI, null,
                    Share_Plugins.PLUGIN_STATUS + "="
                       + Share_Plugin.STATUS_PLUGIN_ON, null, null);
            if( enabled_plugins != null && enabled_plugins.moveToFirst() )
            {
                do {
                    stopPlugin(getApplicationContext(),
                            enabled_plugins.getString(enabled_plugins
                                    .getColumnIndex(Aware_Plugins
                                            .PLUGIN_PACKAGE_NAME)));
                }while(enabled_plugins.moveToNext());
                if( Share.DEBUG ) Log.w(TAG,"Mylocation plugins disabled...");
            }
            if( enabled_plugins != null && ! enabled_plugins.isClosed())
                enabled_plugins.close();
        }
        return START_STICKY;//一直处于启动的状态
    }

//根据 包名（packagename） 来利用 环境（context） 停止插件的运行
    public static void stopPlugin(Context context, String package_name )
    {
        //检查 插件 是否与 应用 绑定
        Intent bundled = new Intent();
        bundled.setClassName(context.getPackageName(),
                                        package_name + ".Plugin");
        //利用Inrent也可以针对性的操控，停止服务
        boolean result = context.stopService(bundled);
        if( result == true ) {
            if( Share.DEBUG ) Log.d(TAG, "Bundled " + package_name
                    + ".Plugin stopped...");
            return;
        }

        Cursor cached = context.getContentResolver().query(
                Share_Plugins.CONTENT_URI, null,
                Aware_Plugins.PLUGIN_PACKAGE_NAME + " LIKE '"//查询条件
                        + package_name + "'", null, null);
        if( cached != null && cached.moveToFirst() ) {
          //要是能查到的话，那就把它停止吧
            Intent plugin = new Intent();
            plugin.setClassName(package_name, package_name + ".Plugin");
            context.stopService(plugin);
            //并用日志显示出来
            if( Share.DEBUG ) Log.d(TAG, package_name + " stopped...");

            ContentValues rowData = new ContentValues();
            rowData.put(Aware_Plugins.PLUGIN_STATUS,
                    Aware_Plugin.STATUS_PLUGIN_OFF);
            context.getContentResolver().update(Aware_Plugins.CONTENT_URI,
                    rowData, Aware_Plugins.PLUGIN_PACKAGE_NAME +
                            " LIKE '" + package_name + "'", null);
        }
        if( cached != null && ! cached.isClosed() )
            //关闭游标（Cursor），释放资源
            cached.close();
    }

//根据包名来 启动 插件 ，用户要是没有安装插件，那就请求用户安装
    //在上面 class：onStartCommand里调用，用于打开插件
    public static void startPlugin(Context context, String package_name ) {

        if( shareContext == null ) shareContext = context;

        //检查插件是否与一个 应用和插件 绑定
        Intent bundled = new Intent();
        bundled.setClassName(context.getPackageName(), package_name
                                        + ".Plugin");
        //ComponentName组件名，可用来打开应用或服务
        ComponentName result = context.startService(bundled);
        if( result != null ) {
            if( Share.DEBUG ) Log.d(TAG, "Bundled " + package_name
                    + ".Plugin started...");
            return;
        }

        //检查插件是否已经 缓存
        Cursor cached = context.getContentResolver().query(
                Aware_Plugins.CONTENT_URI, null,
                Aware_Plugins.PLUGIN_PACKAGE_NAME +
                        " LIKE '" + package_name + "'", null, null);
        if( cached != null && cached.moveToFirst() ) {
            //要是有，那就把那些插件，安装在手机上，其实就是启动
            if( isClassAvailable(context, package_name, "Plugin") ) {
                Intent plugin = new Intent();
                plugin.setClassName(package_name, package_name + ".Plugin");
                context.startService(plugin);
                if( Share.DEBUG ) Log.d(TAG, package_name + " started...");

            //向基本状态信息数据库Share_Provider中的
                // Plugins添加插件 开启状态 的数据
                ContentValues rowData = new ContentValues();
                rowData.put(Aware_Plugins.PLUGIN_STATUS,
                                Aware_Plugin.STATUS_PLUGIN_ON);
                context.getContentResolver().update(
                        Aware_Plugins.CONTENT_URI, rowData,
                        Aware_Plugins.PLUGIN_PACKAGE_NAME +
                        " LIKE '" + package_name + "'", null);
                //关闭游标，释放资源
                cached.close();
                return;
            }
        }
        if( cached != null && ! cached.isClosed() )//要是游标还没关闭，那就……
            cached.close();

        //要是没有下载或绑定的插件的话，那就向服务器端请求下载那些没有的
        new PluginDependencyTask().execute(package_name);
    }

//紧接着上面两行代码，调用
//另外用一个线程来下载手机里面没有的插件
    private static class PluginDependencyTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params)
        {

            String package_name = params[0];
    //调用网络连接类，向网站寻求服务，下载手机没有的插件
            HttpResponse response = new Https(awareContext).dataGET(
                    "https://api.awareframework.com/index.php/" +
                            "plugins/get_plugin/" + package_name, true);
            if( response != null && response.getStatusLine()
                                                .getStatusCode() == 200 )
            {
                try {   //还会解压文件
                    String data = Https.undoGZIP(response);
                    if( data.length() < 10 ) return null;
                //利用JSON对象来打包
                    JSONObject json_package = new JSONObject(data);

                    NotificationCompat.Builder mBuilder = new NotificationCompat
                                                            .Builder(shareContext);
                    mBuilder.setSmallIcon(R.drawable.ic_stat_aware_plugin_dependency);
                    mBuilder.setContentTitle("SHARE");
                    mBuilder.setContentText("Missing: " +
                                    json_package.getString("title")+". Install?");
                    mBuilder.setDefaults(Notification.DEFAULT_ALL);
                    mBuilder.setAutoCancel(true);

                    Intent pluginIntent = new Intent(shareContext,
                                                        DownloadPluginService.class);
                    pluginIntent.putExtra("package_name", package_name);
                    pluginIntent.putExtra("is_update", false);

                    PendingIntent clickIntent = PendingIntent.getService(shareContext,
                            0, pluginIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                    mBuilder.setContentIntent(clickIntent);
                    //通知
                    NotificationManager notManager =
                            (NotificationManager) shareContext.getSystemService(
                                    Context.NOTIFICATION_SERVICE);
                    notManager.notify( json_package.getInt("id"), mBuilder.build());
            //还可以对应多个异常拦截处理
                } catch (ParseException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }

//根据网络服务的包名，来请求下载相应的插件
// 可要可不要，没有调用的
    public static void downloadPlugin(Context context, String package_name, boolean is_update )
    {
        Intent pluginIntent = new Intent(context, DownloadPluginService.class);
        pluginIntent.putExtra("package_name", package_name);
        pluginIntent.putExtra("is_update", is_update);
        context.startService(pluginIntent);
    }

//通过插件的包名，拿到  环境卡？？？来进行复用
// 这里需要定义 卡片 视图，但上面引用有问题
    public static View getContextCard(final Context context,
                                      final String package_name )
    {

        if( ! isClassAvailable(context, package_name, "ContextCard") ) {
            return null;
        }

        String ui_class = package_name + ".ContextCard";
        CardView card = new CardView(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams( LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT );
        params.setMargins( 0,0,0,10 );
        card.setLayoutParams(params);//设置布局参数

        try {
            Context packageContext = context.createPackageContext(package_name, Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);

            Class<?> fragment_loader = packageContext.getClassLoader()
                                                            .loadClass(ui_class);
            Object fragment = fragment_loader.newInstance();
            Method[] allMethods = fragment_loader.getDeclaredMethods();
            Method m = null;
            for( Method mItem : allMethods ) {
                String mName = mItem.getName();
                if( mName.contains("getContextCard") ) {
                    mItem.setAccessible(true);
                    m = mItem;
                    break;
                }
            }

            View ui = (View) m.invoke( fragment, packageContext );
            if( ui != null ) {
                //检查插件是否已设置好了，要是弄好了，就用 卡片视图 显示出来
                if( isClassAvailable(context, package_name, "Settings") ) {
                    ui.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent open_settings = new Intent();
                            open_settings.setClassName(package_name, package_name + ".Settings");
                            open_settings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(open_settings);
                        }
                    });
                }

                //设置卡片视图
                ui.setBackgroundColor(Color.WHITE);
                ui.setPadding(20, 20, 20, 20);
                card.addView(ui);

                return card;
            } else {
                return null;
            }
        }       //try
        catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        return null;
    }

//给一个包名和类，来检查这个到底存不存在。
// 要是存在，那就返回true
// 这个有用
    private static boolean isClassAvailable(Context context
                                        , String package_name, String class_name )
    {
        try{
            //创建一个指定包的环境（这个环境忽略安全，包含代码）
            Context package_context = context.createPackageContext(package_name
                    , Context.CONTEXT_IGNORE_SECURITY
                            | Context.CONTEXT_INCLUDE_CODE);

            //然后用包的环境来加载对应的类
            package_context.getClassLoader().loadClass(package_name+"."+class_name);
        }
        catch ( ClassNotFoundException e ) {
            return false;   //没找到类的异常
        }
        catch ( PackageManager.NameNotFoundException e ) {
            return false;   //没找到包名的异常
        }
        return true;
    }

//查询   根据键（Key）来检索对应的 设置值（setting value）
    public static String getSetting(Context context, String key )
    {
        boolean is_restricted_package = true;
//全局，所有的设置信息
        ArrayList<String> global_settings = new ArrayList<String>();
        global_settings.add(Share_Preferences.DEBUG_FLAG);
        global_settings.add(Share_Preferences.DEBUG_TAG);

        global_settings.add(Share_Preferences.DEVICE_ID);
    /*这里基本都要用到 网络服务 功能。特别是MQTT
         global_settings.add("study_id");
        global_settings.add("study_start");

        global_settings.add(Share_Preferences.STATUS_WEBSERVICE);
        global_settings.add(Share_Preferences.FREQUENCY_WEBSERVICE);
        global_settings.add(Share_Preferences.WEBSERVICE_WIFI_ONLY);
        global_settings.add(Share_Preferences.WEBSERVICE_SERVER);
        global_settings.add(Share_Preferences.STATUS_MQTT);
        global_settings.add(Share_Preferences.MQTT_SERVER);
        global_settings.add(Share_Preferences.MQTT_KEEP_ALIVE);
        global_settings.add(Share_Preferences.MQTT_PORT);
        global_settings.add(Share_Preferences.MQTT_PROTOCOL);
        global_settings.add(Share_Preferences.MQTT_USERNAME);
        global_settings.add(Share_Preferences.MQTT_PASSWORD);
    */
        //要是没有这个键，那么说明，这是个受到限制的包，属于局外，无权获取信息
        // 另外一个关键在于，这个是不是全部
        if( global_settings.contains(key) ) {
            is_restricted_package = false;
        }
//这里的Share_Settings是来自Share_Provider类，
// 里面的第二个方法，定义了基本信息的列
        String value = "";
        Cursor qry = context.getContentResolver().query(
                Share_Settings.CONTENT_URI,
                null,
                Aware_Settings.SETTING_KEY + " LIKE '" + key + "'"
//这里这句有点不懂了
//要是之前这个
                            + ( is_restricted_package
                                ?   " AND " + Aware_Settings.SETTING_PACKAGE_NAME
                                        + " LIKE '" + context.getPackageName() + "'"

                                :   "")
                , null
                , null);
        //要是能够查到有值，那就把这个值给value来返回
        if( qry != null && qry.moveToFirst() )
            value = qry.getString(qry.getColumnIndex(Share_Settings.SETTING_VALUE));

        if( qry != null && ! qry.isClosed() )
            qry.close();    //每一次查询都不忘释放资源

        return value;
    }

//设置    根据键（key）来设置相应的设置值（value），用对象（object）来定义 值
    public static void setSetting(Context context, String key, Object value )
    {

        boolean is_restricted_package = true;

        ArrayList<String> global_settings = new ArrayList<String>();
        global_settings.add(Share_Preferences.DEBUG_FLAG);
        global_settings.add(Share_Preferences.DEBUG_TAG);

        global_settings.add(Share_Preferences.DEVICE_ID);
/*
        global_settings.add("study_id");
        global_settings.add("study_start");

        global_settings.add(Aware_Preferences.STATUS_WEBSERVICE);
        global_settings.add(Aware_Preferences.FREQUENCY_WEBSERVICE);
        global_settings.add(Aware_Preferences.WEBSERVICE_WIFI_ONLY);
        global_settings.add(Aware_Preferences.WEBSERVICE_SERVER);
        global_settings.add(Aware_Preferences.STATUS_MQTT);
        global_settings.add(Aware_Preferences.MQTT_SERVER);
        global_settings.add(Aware_Preferences.MQTT_KEEP_ALIVE);
        global_settings.add(Aware_Preferences.MQTT_PORT);
        global_settings.add(Aware_Preferences.MQTT_PROTOCOL);
        global_settings.add(Aware_Preferences.MQTT_USERNAME);
        global_settings.add(Aware_Preferences.MQTT_PASSWORD);
*/
        if( global_settings.contains(key) ) {
            is_restricted_package = false;
        }

        //只有客户端才能设置 设备编号，不然就直接返回结束
        if( key.equals(Share_Preferences.DEVICE_ID) && ! context.getPackageName()
                .equals("com.example.admin.mylocation") )
            return;
//方法里面设置三个参数（params）在这里都用到
        ContentValues setting = new ContentValues();
        setting.put(Aware_Settings.SETTING_KEY, key);
        setting.put(Aware_Settings.SETTING_VALUE, value.toString());
        setting.put(Aware_Settings.SETTING_PACKAGE_NAME, context.getPackageName());

        Cursor qry = context.getContentResolver().query(
                Aware_Settings.CONTENT_URI, null,
                Aware_Settings.SETTING_KEY + " LIKE '" + key + "'"
                        + (is_restricted_package
                            ? " AND " + Aware_Settings.SETTING_PACKAGE_NAME
                                    + " LIKE '" + context.getPackageName() + "'"
                            : "")
                , null, null);
        //要是有设置值，那就进行 更新 或修改 操作，不然就执行 插入 操作
        if( qry != null && qry.moveToFirst() ) {
            try {
                if( ! qry.getString(qry.getColumnIndex(Aware_Settings.SETTING_VALUE))
                                                    .equals(value.toString()) )
                {//可以直接用这个方法 修改数据 或添加
                    context.getContentResolver().update(Aware_Settings.CONTENT_URI,
                            setting,    //这就是上面ContentValue的东西
                            Aware_Settings.SETTING_ID + "="
                                    + qry.getInt(qry.getColumnIndex(
                                        Aware_Settings.SETTING_ID))
                            , null);
                    if( Share.DEBUG)    //将更新操作记录出来
                        Log.d(Share.TAG,"Updated: "+key+"="+value);
                }
            }catch( SQLiteException e ) {
                if(Share.DEBUG)     Log.d(TAG,e.getMessage());
            }catch( SQLException e ) {
                if(Share.DEBUG)     Log.d(TAG,e.getMessage());
            }

        }
        else //要是没有这个设置值，就执行插入操作。不过，这一步比较简单，只用两个参数
        {
            try {
                context.getContentResolver().insert(Aware_Settings.CONTENT_URI, setting);
                if( Share.DEBUG)
                    Log.d(Share.TAG,"Added: " + key + "=" + value);
            }catch( SQLiteException e ) {
                if(Share.DEBUG)         Log.d(TAG,e.getMessage());
            }catch( SQLException e ) {
                if(Share.DEBUG)         Log.d(TAG,e.getMessage());
            }
        }
        if( qry != null && ! qry.isClosed() )
            qry.close();

//启动广播，通知配置参数的改变
        Intent wearBroadcast = new Intent(ACTION_SHARE_CONFIG_CHANGED);
        wearBroadcast.putExtra(EXTRA_CONFIG_SETTING, key);
        wearBroadcast.putExtra(EXTRA_CONFIG_VALUE, value.toString());
        context.sendBroadcast(wearBroadcast);
    }

//用于释放所有资源、应用退出时
//用于释放所有资源
//用于释放所有资源。因为好多服务还在用
    @Override
    public void onDestroy() {
        super.onDestroy();

        if( repeatingIntent != null ) alarmManager.cancel(repeatingIntent);
        if( webserviceUploadIntent != null) alarmManager.cancel(webserviceUploadIntent);

        if( aware_BR != null ) shareContext.unregisterReceiver(aware_BR);
        if( storage_BR != null ) shareContext.unregisterReceiver(storage_BR);
    }

//检查客户端的 学习Study 是否还在处于 启用的状态，要不然，就重新设置终端
// 这又要涉及到 服务器 了。运用HTTP协议，来访问
    //干脆直接不要了/**/
    private class Study_Check extends AsyncTask<Void, Void, Boolean>
    {
        @Override
        //子线程中具体的逻辑操作
        protected Boolean doInBackground(Void... voids) {
            ArrayList<NameValuePair> data = new ArrayList<NameValuePair>();
//名字和值的配对列表单
            data.add(new BasicNameValuePair(Share_Preferences.DEVICE_ID,
                                            Share.getSetting(getApplicationContext(),
                                            Share_Preferences.DEVICE_ID)));
        //获取网络反映
            HttpResponse response = new Https(getApplicationContext()).dataPOST(
                    Share.getSetting ( getApplicationContext()
                                             ,Share_Preferences.WEBSERVICE_SERVER),
                    data, true);

            if( response != null && response.getStatusLine().getStatusCode() == 200)
            {
                try {
                    String json_str = Https.undoGZIP(response);
                    JSONArray j_array = new JSONArray(json_str);
                    return j_array.getJSONObject(0).has("message");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return false;
        }
        @Override
        //在上面执行完过后
        // 返回操作结果，并进行界面反馈，或提示
        protected void onPostExecute(Boolean is_closed) {
            super.onPostExecute(is_closed);
            if( is_closed ) {
                NotificationCompat.Builder mBuilder = new NotificationCompat
                                                    .Builder(getApplicationContext());
                mBuilder.setSmallIcon(R.drawable.ic_action_aware_studies);
                mBuilder.setContentTitle("SHARE");
                mBuilder.setContentText("The study has ended! Thanks!");
                mBuilder.setDefaults(Notification.DEFAULT_ALL);
                mBuilder.setAutoCancel(true);//自动取消

                NotificationManager notManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                notManager.notify(new Random(System.currentTimeMillis()).nextInt(), mBuilder.build());

                reset(getApplicationContext());
            }
        }
    }

    public static void reset(Context c)
    {
        if( ! c.getPackageName().equals("com.aware") ) return;

        String device_id = Aware.getSetting( c, Aware_Preferences.DEVICE_ID );

        //Remove all settings
        c.getContentResolver().delete( Aware_Settings.CONTENT_URI, null, null );

        //Read default client settings
        SharedPreferences prefs = c.getSharedPreferences( c.getPackageName(), Context.MODE_PRIVATE );
        PreferenceManager.setDefaultValues(c, c.getPackageName(), Context.MODE_PRIVATE, R.xml.aware_preferences, true);
        prefs.edit().commit();

        Map<String,?> defaults = prefs.getAll();
        for(Map.Entry<String, ?> entry : defaults.entrySet()) {
            Aware.setSetting(c, entry.getKey(), entry.getValue());
        }

        //Keep previous AWARE Device ID
        Aware.setSetting(c, Aware_Preferences.DEVICE_ID, device_id);

        //Turn off all active plugins
        Cursor active_plugins = c.getContentResolver().query(Aware_Plugins.CONTENT_URI, null, Aware_Plugins.PLUGIN_STATUS + "=" + Plugins_Manager.PLUGIN_ACTIVE, null, null);
        if( active_plugins != null && active_plugins.moveToFirst() ) {
            do {
                Aware.stopPlugin(c, active_plugins.getString(active_plugins.getColumnIndex(Aware_Plugins.PLUGIN_PACKAGE_NAME)));
            } while(active_plugins.moveToNext());
        }
        if( active_plugins != null && ! active_plugins.isClosed() ) active_plugins.close();

        //Apply fresh state
        Intent aware_apply = new Intent( Aware.ACTION_AWARE_REFRESH );
        c.sendBroadcast(aware_apply);
    }

    /**
     * Client: check if there is an update to the client.
     */
    private class Update_Check extends AsyncTask<Void, Void, Boolean> {
        String filename = "", whats_new = "";
        int version = 0;
        PackageInfo awarePkg = null;

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                awarePkg = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA);
            } catch (PackageManager.NameNotFoundException e1) {
                e1.printStackTrace();
                return false;
            }

            HttpResponse response = new Https(getApplicationContext()).dataGET("https://api.awareframework.com/index.php/awaredev/framework_latest", true);
            if( response != null && response.getStatusLine().getStatusCode() == 200 ) {
                try {
                    JSONArray data = new JSONArray(Https.undoGZIP(response));
                    JSONObject latest_framework = data.getJSONObject(0);

                    if( Aware.DEBUG ) Log.d(Aware.TAG, "Latest: " + latest_framework.toString());

                    filename = latest_framework.getString("filename");
                    version = latest_framework.getInt("version");
                    whats_new = latest_framework.getString("whats_new");

                    if( version > awarePkg.versionCode ) {
                        return true;
                    }
                    return false;
                } catch (ParseException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                if( Aware.DEBUG ) Log.d(Aware.TAG, "Unable to fetch latest framework from AWARE repository...");
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if( result ) {
                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext());
                mBuilder.setSmallIcon(R.drawable.ic_stat_aware_update);
                mBuilder.setContentTitle("AWARE update");
                mBuilder.setContentText("New: " + whats_new + "\nVersion: " + version + "\nTap to install...");
                mBuilder.setDefaults(Notification.DEFAULT_ALL);
                mBuilder.setAutoCancel(true);

                Intent updateIntent = new Intent(getApplicationContext(), UpdateFrameworkService.class);
                updateIntent.putExtra("filename", filename);

                PendingIntent clickIntent = PendingIntent.getService(getApplicationContext(), 0, updateIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                mBuilder.setContentIntent(clickIntent);
                NotificationManager notManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                notManager.notify(version, mBuilder.build());
            }
        }
    }

    /**
     * Client's plugin monitor
     * - Installs a plugin that was just downloaded
     * - Checks if a package is a plugin or not
     * @author denzilferreira
     */
    public static class PluginMonitor extends BroadcastReceiver {
        private static PackageManager mPkgManager;

        @Override
        public void onReceive(Context context, Intent intent) {
            mPkgManager = context.getPackageManager();

            Bundle extras = intent.getExtras();
            Uri packageUri = intent.getData();
            if( packageUri == null ) return;
            String packageName = packageUri.getSchemeSpecificPart();
            if( packageName == null ) return;

            if( ! packageName.matches("com.aware.plugin.*") ) return;

            if( intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED) ) {
                //Updating for new package
                if( extras.getBoolean(Intent.EXTRA_REPLACING) ) {
                    if(Aware.DEBUG) Log.d(TAG, packageName + " is updating!");

                    ContentValues rowData = new ContentValues();
                    rowData.put(Aware_Plugins.PLUGIN_VERSION, getVersion(packageName));

                    Cursor current_status = context.getContentResolver().query(Aware_Plugins.CONTENT_URI, new String[]{Aware_Plugins.PLUGIN_STATUS}, Aware_Plugins.PLUGIN_PACKAGE_NAME + " LIKE '" + packageName + "'", null, null);
                    if( current_status != null && current_status.moveToFirst() ) {
                        if( current_status.getInt(current_status.getColumnIndex(Aware_Plugins.PLUGIN_STATUS)) == Aware_Plugin.STATUS_PLUGIN_ON ) {
                            Intent aware = new Intent(Aware.ACTION_AWARE_REFRESH);
                            context.sendBroadcast(aware);
                        }
                        if( current_status.getInt(current_status.getColumnIndex(Aware_Plugins.PLUGIN_STATUS)) == Plugins_Manager.PLUGIN_NOT_INSTALLED || current_status.getInt(current_status.getColumnIndex(Aware_Plugins.PLUGIN_STATUS)) == Plugins_Manager.PLUGIN_UPDATED ) {
                            rowData.put(Aware_Plugins.PLUGIN_STATUS, Plugins_Manager.PLUGIN_ACTIVE);
                        }
                    }
                    if( current_status != null && ! current_status.isClosed() ) current_status.close();

                    context.getContentResolver().update(Aware_Plugins.CONTENT_URI, rowData, Aware_Plugins.PLUGIN_PACKAGE_NAME + " LIKE '" + packageName + "'", null);

                    //Refresh plugin manager UI if visible
                    context.sendBroadcast(new Intent(ACTION_AWARE_PLUGIN_MANAGER_REFRESH));

                    //Refresh stream UI if visible
                    context.sendBroadcast(new Intent(Stream_UI.ACTION_AWARE_UPDATE_STREAM));

                    //Apply fresh state
                    Intent aware_apply = new Intent( Aware.ACTION_AWARE_REFRESH );
                    context.sendBroadcast(aware_apply);
                    return;
                }

                //Installing new
                try {
                    ApplicationInfo appInfo = mPkgManager.getApplicationInfo( packageName, PackageManager.GET_ACTIVITIES );
                    //Check if this is a package for which we have more info from the server
                    new Plugin_Info_Async().execute(appInfo);
                } catch( final PackageManager.NameNotFoundException e ) {
                    e.printStackTrace();
                }
            }

            if( intent.getAction().equals(Intent.ACTION_PACKAGE_REMOVED) ) {
                //Updating
                if( extras.getBoolean(Intent.EXTRA_REPLACING) ) {
                    //this is an update, bail out.
                    return;
                }
                //Deleting
                context.getContentResolver().delete(Aware_Plugins.CONTENT_URI, Aware_Plugins.PLUGIN_PACKAGE_NAME + " LIKE '" + packageName + "'", null);
                if( Aware.DEBUG ) Log.d(TAG,"AWARE plugin removed:" + packageName);

                //Apply fresh state
                Intent aware_apply = new Intent( Aware.ACTION_AWARE_REFRESH );
                context.sendBroadcast(aware_apply);

                //Refresh stream UI if visible
                context.sendBroadcast(new Intent(Stream_UI.ACTION_AWARE_UPDATE_STREAM));

                //Refresh Plugin manager UI if visible
                context.sendBroadcast(new Intent(ACTION_AWARE_PLUGIN_MANAGER_REFRESH));
            }
        }

        private int getVersion( String package_name ) {
            try {
                PackageInfo pkgInfo = mPkgManager.getPackageInfo(package_name, PackageManager.GET_META_DATA);
                return pkgInfo.versionCode;
            } catch (PackageManager.NameNotFoundException e) {
                if( Aware.DEBUG ) Log.d( Aware.TAG, e.getMessage());
            }
            return 0;
        }
    }

    /**
     * Fetches info from webservices on installed plugins.
     * @author denzilferreira
     *又需要联网进行检查
     */
    private static class Plugin_Info_Async extends AsyncTask<ApplicationInfo, Void, JSONObject> {
        private ApplicationInfo app;
        private byte[] icon;

        @Override
        protected JSONObject doInBackground(ApplicationInfo... params) {

            app = params[0];

            JSONObject json_package = null;
            HttpResponse http_request = new Https(awareContext).dataGET("https://api.awareframework.com/index.php/plugins/get_plugin/" + app.packageName, true);
            if( http_request != null && http_request.getStatusLine().getStatusCode() == 200 ) {
                try {
                    String json_string = Https.undoGZIP(http_request);
                    if( ! json_string.equals("[]") ) {
                        json_package = new JSONObject(json_string);
                        icon = Plugins_Manager.cacheImage("http://api.awareframework.com" + json_package.getString("iconpath"), awareContext);
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return json_package;
        }

        @Override
        protected void onPostExecute(JSONObject json_package) {
            super.onPostExecute(json_package);

            //If we already have cached information for this package, just update it
            boolean is_cached = false;
            Cursor plugin_cached = awareContext.getContentResolver().query(Aware_Plugins.CONTENT_URI, null, Aware_Plugins.PLUGIN_PACKAGE_NAME + " LIKE '" + app.packageName + "'", null, null);
            if( plugin_cached != null && plugin_cached.moveToFirst() ) {
                is_cached = true;
            }
            if( plugin_cached != null && ! plugin_cached.isClosed()) plugin_cached.close();

            ContentValues rowData = new ContentValues();
            rowData.put(Aware_Plugins.PLUGIN_PACKAGE_NAME, app.packageName);
            rowData.put(Aware_Plugins.PLUGIN_NAME, app.loadLabel(awareContext.getPackageManager()).toString());
            rowData.put(Aware_Plugins.PLUGIN_VERSION, getVersion(app.packageName));
            rowData.put(Aware_Plugins.PLUGIN_STATUS, Aware_Plugin.STATUS_PLUGIN_ON);
            if( json_package != null ) {
                try {
                    rowData.put(Aware_Plugins.PLUGIN_ICON, icon);
                    rowData.put(Aware_Plugins.PLUGIN_AUTHOR, json_package.getString("first_name") + " " + json_package.getString("last_name") + " - " + json_package.getString("email"));
                    rowData.put(Aware_Plugins.PLUGIN_DESCRIPTION, json_package.getString("desc"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            if( ! is_cached ) {
                awareContext.getContentResolver().insert(Aware_Plugins.CONTENT_URI, rowData);
            } else {
                awareContext.getContentResolver().update(Aware_Plugins.CONTENT_URI, rowData, Aware_Plugins.PLUGIN_PACKAGE_NAME + " LIKE '" + app.packageName + "'", null);
            }

            if( Aware.DEBUG ) Log.d(TAG,"aware plugin added and activated:" + app.packageName);

            //Refresh stream UI if visible
            awareContext.sendBroadcast(new Intent(Stream_UI.ACTION_AWARE_UPDATE_STREAM));

            //Refresh Plugin Manager UI if visible
            awareContext.sendBroadcast(new Intent(ACTION_AWARE_PLUGIN_MANAGER_REFRESH));

            Intent aware = new Intent(Aware.ACTION_AWARE_REFRESH);
            awareContext.sendBroadcast(aware);
        }

        private int getVersion( String package_name ) {
            try {
                PackageInfo pkgInfo = awareContext.getPackageManager().getPackageInfo(package_name, PackageManager.GET_META_DATA);
                return pkgInfo.versionCode;
            } catch (PackageManager.NameNotFoundException e) {
                if( Aware.DEBUG ) Log.d( Aware.TAG, e.getMessage());
            }
            return 0;
        }
    }

    /**
     * Background service to download missing plugins
     * @author denzilferreira
     *
     */
    public static class DownloadPluginService extends IntentService {
        public DownloadPluginService() {
            super("Download Plugin service");
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            String package_name = intent.getStringExtra("package_name");
            boolean is_update = intent.getBooleanExtra("is_update", false);

            HttpResponse response = new Https(awareContext).dataGET("https://api.awareframework.com/index.php/plugins/get_plugin/" + package_name, true);
            if( response != null && response.getStatusLine().getStatusCode() == 200 ) {
                try {
                    JSONObject json_package = new JSONObject(Https.undoGZIP(response));

                    //Create the folder where all the databases will be stored on external storage
                    File folders = new File(Environment.getExternalStorageDirectory()+"/aware/plugins/");
                    folders.mkdirs();

                    String package_url = "http://plugins.awareframework.com/" + json_package.getString("package_path").replace("/uploads/", "") + json_package.getString("package_name");
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(package_url));
                    if( ! is_update ) {
                        request.setDescription("Downloading " + json_package.getString("title") );
                    } else {
                        request.setDescription("Updating " + json_package.getString("title") );
                    }
                    request.setTitle("aware");
                    request.setDestinationInExternalPublicDir("/", "aware/plugins/" + json_package.getString("package_name"));

                    DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                    AWARE_PLUGIN_DOWNLOAD_IDS.add(manager.enqueue(request));
                } catch (ParseException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Background service to download latest version of AWARE
     * @author denzilferreira
     *
     */
    public static class UpdateFrameworkService extends IntentService {
        public UpdateFrameworkService() {
            super("Update Framework service");
        }
        @Override
        protected void onHandleIntent(Intent intent) {
            String filename = intent.getStringExtra("filename");

            //Make sure we have the releases folder
            File releases = new File(Environment.getExternalStorageDirectory()+"/AWARE/releases/");
            releases.mkdirs();

            String url = "http://www.awareframework.com/" + filename;

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setDescription("Downloading newest aware... please wait...");
            request.setTitle("aware Update");
            request.setDestinationInExternalPublicDir("/", "aware/releases/"+filename);
            DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            AWARE_FRAMEWORK_DOWNLOAD_ID = manager.enqueue(request);
        }
    }

    /**
     * BroadcastReceiver that monitors for AWARE framework actions:
     * - ACTION_AWARE_SYNC_DATA = upload data to remote webservice server.
     * - ACTION_AWARE_CLEAR_DATA = clears local device's AWARE modules databases.
     * - ACTION_AWARE_REFRESH - apply changes to the configuration.
     * - {@link DownloadManager#ACTION_DOWNLOAD_COMPLETE} - when AWARE framework update has been downloaded.
     * @author denzil
     *
     */
    public static class Aware_Broadcaster extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            //We are only synching the device information, not aware's settings and active plugins.
            String[] DATABASE_TABLES = Aware_Provider.DATABASE_TABLES;
            String[] TABLES_FIELDS = Aware_Provider.TABLES_FIELDS;
            Uri[] CONTEXT_URIS = new Uri[]{ Aware_Device.CONTENT_URI };

            if( intent.getAction().equals(Aware.ACTION_AWARE_SYNC_DATA) && Aware.getSetting(context, Aware_Preferences.STATUS_WEBSERVICE).equals("true") ) {
                Intent webserviceHelper = new Intent( context, WebserviceHelper.class );
                webserviceHelper.setAction( WebserviceHelper.ACTION_AWARE_WEBSERVICE_SYNC_TABLE );
                webserviceHelper.putExtra( WebserviceHelper.EXTRA_TABLE, DATABASE_TABLES[0] );
                webserviceHelper.putExtra( WebserviceHelper.EXTRA_FIELDS, TABLES_FIELDS[0] );
                webserviceHelper.putExtra( WebserviceHelper.EXTRA_CONTENT_URI, CONTEXT_URIS[0].toString() );
                context.startService(webserviceHelper);
            }

            if( intent.getAction().equals(Aware.ACTION_AWARE_CLEAR_DATA) ) {
                context.getContentResolver().delete(Aware_Provider.Aware_Device.CONTENT_URI, null, null);
                if( Aware.DEBUG ) Log.d(TAG,"Cleared " + CONTEXT_URIS[0]);

                //Clear remotely if webservices are active
                if( Aware.getSetting(context, Aware_Preferences.STATUS_WEBSERVICE).equals("true") ) {
                    Intent webserviceHelper = new Intent( context, WebserviceHelper.class );
                    webserviceHelper.setAction(WebserviceHelper.ACTION_AWARE_WEBSERVICE_CLEAR_TABLE );
                    webserviceHelper.putExtra( WebserviceHelper.EXTRA_TABLE, DATABASE_TABLES[0] );
                    context.startService(webserviceHelper);
                }
            }

            if( intent.getAction().equals(Aware.ACTION_AWARE_REFRESH)) {
                Intent refresh = new Intent(context, Aware.class);
                context.startService(refresh);
            }

            if( intent.getAction().equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE) ) {
                DownloadManager manager = (DownloadManager) context.getSystemService(DOWNLOAD_SERVICE);
                long downloaded_id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);

                if( downloaded_id == AWARE_FRAMEWORK_DOWNLOAD_ID ) {
                    if( Aware.DEBUG ) Log.d(Aware.TAG, "aware framework update received...");
                    DownloadManager.Query qry = new DownloadManager.Query();
                    qry.setFilterById(AWARE_FRAMEWORK_DOWNLOAD_ID);
                    Cursor data = manager.query(qry);
                    if( data != null && data.moveToFirst() ) {
                        if( data.getInt(data.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL ) {
                            String filePath = data.getString(data.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                            File mFile = new File( Uri.parse(filePath).getPath() );
                            Intent promptUpdate = new Intent(Intent.ACTION_VIEW);
                            promptUpdate.setDataAndType(Uri.fromFile(mFile), "application/vnd.android.package-archive");
                            promptUpdate.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(promptUpdate);
                        }
                    }
                    if( data != null && ! data.isClosed() ) data.close();
                }

                if( AWARE_PLUGIN_DOWNLOAD_IDS.size() > 0 ) {
                    for( int i = 0; i < AWARE_PLUGIN_DOWNLOAD_IDS.size(); i++ ) {
                        long queue = AWARE_PLUGIN_DOWNLOAD_IDS.get(i);
                        if( downloaded_id == queue ) {
                            Cursor cur = manager.query(new DownloadManager.Query().setFilterById(queue));
                            if( cur != null && cur.moveToFirst() ) {
                                if( cur.getInt(cur.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL ) {
                                    String filePath = cur.getString(cur.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));

                                    if( Aware.DEBUG ) Log.d(Aware.TAG, "Plugin to install: " + filePath);

                                    File mFile = new File( Uri.parse(filePath).getPath() );
                                    Intent promptInstall = new Intent(Intent.ACTION_VIEW);
                                    promptInstall.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    promptInstall.setDataAndType(Uri.fromFile(mFile), "application/vnd.android.package-archive");
                                    context.startActivity(promptInstall);
                                }
                            }
                            if( cur != null && ! cur.isClosed() ) cur.close();
                        }
                    }
                    AWARE_PLUGIN_DOWNLOAD_IDS.remove(downloaded_id); //dequeue
                }
            }
        }
    }
    private static final Aware_Broadcaster aware_BR = new Aware_Broadcaster();

    /**还要 查看能否使用存储设备
     * Checks if we have access to the storage of the device. Turns off AWARE when we don't, turns it back on when available again.
     */
    public static class Storage_Broadcaster extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if( intent.getAction().equals(Intent.ACTION_MEDIA_MOUNTED) ) {
                if( Aware.DEBUG ) Log.d(TAG,"Resuming aware data logging...");
            }
            if ( intent.getAction().equals(Intent.ACTION_MEDIA_UNMOUNTED) ) {
                if( Aware.DEBUG ) Log.w(TAG,"Stopping aware data logging until the SDCard is available again...");
            }
            Intent aware = new Intent(context, Aware.class);
            context.startService(aware);
        }
    }
    //存储信息广播？？？
    private static final Storage_Broadcaster storage_BR = new Storage_Broadcaster();
//从这里开始的

//打开所有的服务，其实是事先检查一下用户是否勾选，再打开相应的服务
// 至于这个服务所对应的网络要求就不清楚了？？？
// 都不引导用户打开定位所需的条件（GPS或网络）
    protected void startAllServices() {

//如果应用的GPS定位或网络定位勾选状态为真的话，就打开定位服务
// 相反就直接关闭定位服务
        if( Aware.getSetting(awareContext, Share_Preferences.STATUS_LOCATION_GPS).equals("true")
                || Aware.getSetting(awareContext, Share_Preferences.STATUS_LOCATION_NETWORK).equals("true") ) {
            startLocations();
        }else stopLocations();

        if( Aware.getSetting(awareContext, Share_Preferences.STATUS_NETWORK_EVENTS).equals("true") ) {
            startNetwork();
        }else stopNetwork();


        if( Aware.getSetting(awareContext, Share_Preferences.STATUS_WIFI).equals("true") ) {
            startWiFi();
        }else stopWiFi();

    }

//停止或关闭所有服务
    protected void stopAllServices() {
        stopLocations();
        stopNetwork();
        stopWiFi();
    }

//打开和关闭WiFi模式
    protected void startWiFi() {
        if( wifiSrv == null ) wifiSrv = new Intent(awareContext, WiFi.class);
        awareContext.startService(wifiSrv);
    }

    protected void stopWiFi() {
        if( wifiSrv != null ) awareContext.stopService(wifiSrv);
    }

//打开和关闭定位模式，用于打开和关闭相应的服务
    protected void startLocations() {
        if( locationsSrv == null) locationsSrv = new Intent(awareContext, Locations.class);
        awareContext.startService(locationsSrv);
    }

    protected void stopLocations() {
        if( Aware.getSetting(awareContext, Share_Preferences.STATUS_LOCATION_GPS).equals("false")
                && Aware.getSetting(awareContext, Share_Preferences.STATUS_LOCATION_NETWORK).equals("false") ) {
            if(locationsSrv != null) awareContext.stopService(locationsSrv);
        }
    }

//打开和停止或关闭移动网络访问模式
    protected void startNetwork() {
        if( networkSrv == null ) networkSrv = new Intent(awareContext, Network.class);
        awareContext.startService(networkSrv);
    }

    protected void stopNetwork() {
        if(networkSrv != null) awareContext.stopService(networkSrv);
    }


  }

