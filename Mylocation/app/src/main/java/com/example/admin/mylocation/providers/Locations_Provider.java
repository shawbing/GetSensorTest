package com.example.admin.mylocation.providers;

/**
 * Created by admin on 2017/8/4.
 */

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Environment;
import android.provider.BaseColumns;
import android.util.Log;

import com.example.admin.mylocation.Share;
import com.example.admin.mylocation.utils.DataBaseHelper;

import java.util.HashMap;

//定位提供类，允许访问数据库中定位数据的记录。而数据库位于本地存储卡中

public class Locations_Provider extends ContentProvider {
//数据库版本
    public static final int DATABASE_VERSION = 2;
//定位内容提供的端口,
// 为什么本类的操作还要申明认证路径？？？
// 是为了其他类直接调用这个？
    public static String AUTHORITY = "com.example.admin.mylocation.provider.locations";

    // 定位提供者的检索路径
    private static final int LOCATIONS = 1;
    private static final int LOCATIONS_ID = 2;

    //
    // 定位信息内容及其存储的表结构的呈现
    public static final class Locations_Data implements BaseColumns {
        private Locations_Data() {  };

        public static final Uri CONTENT_URI = Uri.parse("content://"
                + Locations_Provider.AUTHORITY + "/locations");

        //接下来这两句有问题
//接下来这两句有问题
        //接下来这两句有问题
        //分别是：内容类型  节点（单个数据项）类型
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.aware.locations";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.aware.locations";
//统一定义列名。包括：主键ID（系统自动给的？？？）
//时间戳，设备ID，纬度，经度，方向，速度，高度，提供者，精度，标签
        public static final String _ID = "_id";
        public static final String TIMESTAMP = "timestamp";
        public static final String DEVICE_ID = "device_id";
        public static final String LATITUDE = "double_latitude";
        public static final String LONGITUDE = "double_longitude";
        public static final String BEARING = "double_bearing";
        public static final String SPEED = "double_speed";
        public static final String ALTITUDE = "double_altitude";
        public static final String PROVIDER = "provider";
        public static final String ACCURACY = "accuracy";
        public static final String LABEL = "label";
    }
//数据库的连接路径
    public static String DATABASE_NAME = Environment
            .getExternalStorageDirectory() + "/mylocation/" + "locations.db";
//数据库的表名，即locations
    public static final String[] DATABASE_TABLES = { "locations" };
//定位信息表中的各个列名，并设置初始值
    public static final String[] TABLES_FIELDS = {
            Locations_Data._ID + " integer primary key autoincrement,"
            + Locations_Data.TIMESTAMP + " real default 0,"
            + Locations_Data.DEVICE_ID + " text default '',"
            + Locations_Data.LATITUDE + " real default 0,"
            + Locations_Data.LONGITUDE + " real default 0,"
            + Locations_Data.BEARING + " real default 0,"
            + Locations_Data.SPEED + " real default 0,"
            + Locations_Data.ALTITUDE + " real default 0,"
            + Locations_Data.PROVIDER + " text default '',"
            + Locations_Data.ACCURACY + " real default 0,"
            + Locations_Data.LABEL + " text default '',"
             //独一无二的标识：时间加上设备编号
                    + "UNIQUE("
            + Locations_Data.TIMESTAMP + "," + Locations_Data.DEVICE_ID + ")" };
//初始化必要的对象
    private static UriMatcher sUriMatcher = null;//用于筛选出具体的数据
    //用于后面，设置数据库中字段的别名
    private static HashMap<String, String> locationsProjectionMap = null;
    private static DataBaseHelper databaseHelper = null;
    private static SQLiteDatabase database = null;

//初始化数据库
    private boolean initializeDB() {
        if (databaseHelper == null) {
            //相当于新建一个数据库
            databaseHelper = new DataBaseHelper( getContext(), DATABASE_NAME
                    , null, DATABASE_VERSION, DATABASE_TABLES, TABLES_FIELDS );
        }
        if( databaseHelper != null && ( database == null || ! database.isOpen() )) {
            //获取读写数据库的权限
            database = databaseHelper.getWritableDatabase();
        }
        return( database != null && databaseHelper != null);
    }

//删除数据库中的数据
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return 0;
        }

        int count = 0;

        switch (sUriMatcher.match(uri)) {
            case LOCATIONS:
                database.beginTransaction();
                count = database.delete(DATABASE_TABLES[0], selection,
                        selectionArgs);
                database.setTransactionSuccessful();
                database.endTransaction();
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        //通知数据已经改变
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    //获取类型
    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case LOCATIONS:
                return Locations_Data.CONTENT_TYPE;
            case LOCATIONS_ID:
                return Locations_Data.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

//插入数据
    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {

        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return null;
        }
//？？？？？？？为什么有这一步不直接用initialValues，
// 而要判断其值是否为空，
// 如果不为空就将其赋给新同类型的变量
// 若为空，就重新定义一个
        ContentValues values = (initialValues != null) ? new ContentValues(
                initialValues) : new ContentValues();

        switch (sUriMatcher.match(uri)) {
            case LOCATIONS:
                database.beginTransaction();
                long location_id = database.insertWithOnConflict(DATABASE_TABLES[0],
                        Locations_Data.PROVIDER, values, SQLiteDatabase.CONFLICT_IGNORE);
                database.setTransactionSuccessful();
                database.endTransaction();
                if (location_id > 0) {
                    Uri locationUri = ContentUris.withAppendedId(
                            Locations_Data.CONTENT_URI, location_id);
                    getContext().getContentResolver().notifyChange(locationUri,
                            null);
                    return locationUri;
                }

                throw new SQLException("Failed to insert row into " + uri);
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }
//这个类的主函数，主要用于实例化对象
    @Override
    public boolean onCreate() {
        //主要对象的实例化
        AUTHORITY = getContext().getPackageName() + ".provider.locations";
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(Locations_Provider.AUTHORITY, DATABASE_TABLES[0],
                LOCATIONS);
//ProjectionMap允许字段名及其值 一样‘
// 不过这里重复的好处在哪里？？？？？
        locationsProjectionMap = new HashMap<String, String>();
        locationsProjectionMap.put(Locations_Data._ID, Locations_Data._ID);
        locationsProjectionMap.put(Locations_Data.TIMESTAMP,
                Locations_Data.TIMESTAMP);
        locationsProjectionMap.put(Locations_Data.DEVICE_ID,
                Locations_Data.DEVICE_ID);
        locationsProjectionMap.put(Locations_Data.LATITUDE,
                Locations_Data.LATITUDE);
        locationsProjectionMap.put(Locations_Data.LONGITUDE,
                Locations_Data.LONGITUDE);
        locationsProjectionMap.put(Locations_Data.BEARING,
                Locations_Data.BEARING);
        locationsProjectionMap.put(Locations_Data.SPEED, Locations_Data.SPEED);
        locationsProjectionMap.put(Locations_Data.ALTITUDE,
                Locations_Data.ALTITUDE);
        locationsProjectionMap.put(Locations_Data.PROVIDER,
                Locations_Data.PROVIDER);
        locationsProjectionMap.put(Locations_Data.ACCURACY,
                Locations_Data.ACCURACY);
        locationsProjectionMap.put(Locations_Data.LABEL, Locations_Data.LABEL);

        return true;
    }

//查询数据库中的数据
    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        if( ! initializeDB() ) {
            //如果不能初始化数据库，将这个 异常 放入 日志中
            Log.w(AUTHORITY,"Database unavailable...");
            return null;
        }

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (sUriMatcher.match(uri)) {
            case LOCATIONS:
                //只有一张表
                qb.setTables(DATABASE_TABLES[0]);
                //这是什么？提供一种映射，对象必须是HASHMAP
                // 可设置数据库中字段的别名（有点像注释），
                // 用户自定义的列名
                qb.setProjectionMap(locationsProjectionMap);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {
            Cursor c = qb.query(database, projection, selection, selectionArgs,
                    null, null, sortOrder);
            //貌似是通知，已调用相应的Uri，查询了数据
            c.setNotificationUri(getContext().getContentResolver(), uri);
            return c;
        } catch (IllegalStateException e) {
            //将 异常 记录到系统日志中
            if (Share.DEBUG)
                Log.e(Share.TAG, e.getMessage());

            return null;
        }
    }

//进入数据库，更新数据
    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {

        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return 0;
        }

        int count = 0;
        switch (sUriMatcher.match(uri)) {
            case LOCATIONS:
                database.beginTransaction();
                count = database.update(DATABASE_TABLES[0], values, selection,
                        selectionArgs);
                database.setTransactionSuccessful();
                database.endTransaction();
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
}
