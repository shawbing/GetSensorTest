package com.example.admin.mylocation.ui;

/**
 * Created by admin on 2017/8/4.
 */

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.admin.mylocation.Share;
import com.example.admin.mylocation.Share_Preferences;
//import com.example.admin.mylocation.Share_Preferences.StudyConfig;
import com.example.admin.mylocation.R;

//暂时不需要网络访问
//import com.example.admin.mylocation.utils.Https;
//import org.apache.http.HttpResponse;
//import org.apache.http.ParseException;

import org.json.JSONException;
import org.json.JSONObject;

//主要用于管理 标题栏，导航窗口（抽屉），菜单

public class Share_Activity extends ActionBarActivity {

    private DrawerLayout navigationDrawer;
    private ListView navigationList;
    private ActionBarDrawerToggle navigationToggle;
    public static Toolbar toolbar;

    @Override
    public void setContentView(int layoutResID) {
        ViewGroup contentView = (ViewGroup) LayoutInflater.from(this).inflate(layoutResID, (ViewGroup) getWindow().getDecorView().getRootView(), false);
        toolbar = (Toolbar) contentView.findViewById(R.id.share_toolbar);
        toolbar.setTitle(getTitle());
        setSupportActionBar(toolbar);

        navigationDrawer = (DrawerLayout) contentView.findViewById(R.id.share_ui_main);
        navigationList = (ListView) contentView.findViewById(R.id.share_navigation);
//导航窗口相当于一个开关转换按钮（Toggle）一样，也似抽屉
        navigationToggle = new ActionBarDrawerToggle( Share_Activity.this, navigationDrawer, toolbar, R.string.drawer_open, R.string.drawer_close );
        navigationDrawer.setDrawerListener(navigationToggle);

        String[] options = {"Sensors"};
        NavigationAdapter nav_adapter = new NavigationAdapter( getApplicationContext(), options);
        navigationList.setAdapter(nav_adapter);
//定义选项的监听事件
        navigationList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                LinearLayout item_container = (LinearLayout) view.findViewById(R.id.nav_container);
                item_container.setBackgroundColor(Color.DKGRAY);

                for( int i=0; i< navigationList.getChildCount(); i++ ) {
                    if( i != position ) {
                        LinearLayout other = (LinearLayout) navigationList.getChildAt(i);
                        LinearLayout other_item = (LinearLayout) other.findViewById(R.id.nav_container);
                        other_item.setBackgroundColor(Color.TRANSPARENT);
                    }
                }

                // Bundle animations = makeCustomAnimation(Aware_Activity.this, R.anim.anim_slide_in_left, R.anim.anim_slide_out_left).toBundle();
                ActivityOptionsCompat options =ActivityOptionsCompat.makeCustomAnimation(Share_Activity.this, R.anim.slide_in_left, R.anim.slide_out_left);//切换动画
                Bundle animations = options.toBundle();

                switch( position ) {
                    case 0: //Sensors  启动Sensor设置界面(主界面)
                        Intent sensors_ui = new Intent( Share_Activity.this, Share_Preferences.class );
                        ActivityCompat.startActivity(Share_Activity.this,sensors_ui,animations);//切换到Sensors界面
                        break;
                }
                //关闭抽屉式列表（或称为隐藏）
                navigationDrawer.closeDrawer(navigationList);
            }
        });
        getWindow().setContentView(contentView);
    }

//学习配置（study_config）？？？？？？并获取相应的结果.是什么样的结果？
// 暂时消失
    /*
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if( requestCode == Share_Preferences.REQUEST_JOIN_STUDY ) {
            if( resultCode == RESULT_OK ) {
    //这里的StudyConfig.class是Share_Preference里面的Service服务
                // 还需要弄清这个service是干啥的才行
                Intent study_config = new Intent(this, StudyConfig.class);
                study_config.putExtra("study_url", data.getStringExtra("study_url"));
                startService(study_config);
            }
        }
    }*/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.share_menu, menu);

//考虑挺周全的，要是手机没有相机，就不能让用户看见这个按钮，
// 只可惜这里不需要
// 不然还需要获取打开摄像头的权限
// 也可值得一试
/*          //Most watches don't have a camera
        if( Aware.is_watch(this) ) {
            MenuItem qrcode = menu.findItem(R.id.aware_qrcode);
            qrcode.setVisible(false);
        }*/
       return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        navigationToggle.syncState();
    }

//配置改变的监听
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        navigationToggle.onConfigurationChanged(newConfig);
    }

//菜单选项的事件
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
 /*       if( item != null && item.getTitle() != null ){
            if( item.getTitle().equals(getString(R.string.aware_qrcode)) ) {
                Intent join_study = new Intent(Aware_Activity.this, CameraStudy.class);
                startActivityForResult(join_study, Aware_Preferences.REQUEST_JOIN_STUDY);
            }
            if( item.getTitle().equals(getString(R.string.aware_team)) ) {
                Intent about_us = new Intent(Aware_Activity.this, About.class);
                startActivity(about_us);
            }
        }
        switch (item.getItemId()) {
            case android.R.id.home: onBackPressed(); return true;
            default: return super.onOptionsItemSelected(item);
        }*/
        return super.onOptionsItemSelected(item);
    }

//加载定义，配置导航窗口，并为其添加监听事件
    public class NavigationAdapter extends ArrayAdapter<String> {
        private final String[] items;
        private final LayoutInflater inflater;//用于加载布局文件
        private final Context context;
    //导航窗口的加载适配器定义
        public NavigationAdapter(Context context, String[] items) {
            super(context, R.layout.share_navigation_item, items);
            this.context = context;
            this.items = items;
            this.inflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        }
//页面展示，获取到每个节点显示的组件
// 首先是页面移动效果，
// 其次根据用户选择的选项，打开相应的活动，
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            LinearLayout row = (LinearLayout) inflater.inflate(R.layout.share_navigation_item, parent, false);
            row.setFocusable(false);
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ActivityOptionsCompat options =ActivityOptionsCompat.makeCustomAnimation(Share_Activity.this, R.anim.slide_in_left, R.anim.slide_out_left);//切换动画
                    Bundle animations = options.toBundle();

                    switch( position ) {
                        case 0: //Sensors
                            Intent sensors_ui = new Intent( Share_Activity.this, Share_Preferences.class );
                            ActivityCompat.startActivity(Share_Activity.this,sensors_ui,animations);//切换到Sensors界面
                            break;
                    }
                    navigationDrawer.closeDrawer(navigationList);
                }
            });
            ImageView nav_icon = (ImageView) row.findViewById(R.id.nav_item_icon);
            TextView nav_title = (TextView) row.findViewById(R.id.nav_item_text);

            switch( position ) {
                case 0:
                    //获取图标显示   针对性的图标显示
                    nav_icon.setImageResource(R.drawable.ic_action_nav_sensors);
                    if( context.getClass().getSimpleName().equals("Aware_Preferences")) {
                        row.setBackgroundColor(Color.DKGRAY);
                    }
                    break;
            }
            String item = items[position];
            nav_title.setText(item);

            return row;
        }
    }
//结果还是用于获取学习数据
    /*
    public class Async_StudyData extends AsyncTask<String, Void, JSONObject> {
        private String study_url = "";
        private ProgressDialog loader;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loader = new ProgressDialog(Aware_Activity.this);
            loader.setTitle("Loading study");
            loader.setMessage("Please wait...");
            loader.setCancelable(false);
            loader.setIndeterminate(true);
            loader.show();
        }
//后台网络解析，传输服务
/*       @Override
        protected JSONObject doInBackground(String... params) {
            study_url = params[0];
            String study_api_key = study_url.substring(study_url.lastIndexOf("/")+1, study_url.length());

            if( study_api_key.length() == 0 ) return null;

            HttpResponse request = new Https(getApplicationContext()).dataGET("https://api.awareframework.com/index.php/webservice/client_get_study_info/" + study_api_key, true);
            if( request != null && request.getStatusLine().getStatusCode() == 200 ) {
                try {
                    String json_str = Https.undoGZIP(request);
                    if( json_str.equals("[]") ) {
                        return null;
                    }
                    JSONObject study_data = new JSONObject(json_str);
                    return study_data;
                } catch (ParseException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
*/
//主要是用于study类，即学习模块
/*        @Override
        protected void onPostExecute(JSONObject result) {
            super.onPostExecute(result);

            try{
                loader.dismiss();
            }catch( IllegalArgumentException e ) {
                //It's ok, we might get here if we couldn't get study info.
                return;
            }

            if( result == null ) {
                AlertDialog.Builder builder = new AlertDialog.Builder(Aware_Activity.this);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        //if part of a study, you can't change settings.
                        if( Aware.getSetting(getApplicationContext(), "study_id").length() > 0 ) {
                            Toast.makeText(getApplicationContext(), "As part of a study, no changes are allowed.", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    }
                });
                builder.setTitle("Study information");
                builder.setMessage("Unable to retrieve study's information. Please, try again later.");
                builder.setNegativeButton("Quit study!", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Toast.makeText(getApplicationContext(), "Clearing settings... please wait", Toast.LENGTH_LONG).show();
                        Aware.reset(getApplicationContext());

                        Intent preferences = new Intent(getApplicationContext(), Aware_Preferences.class);
                        preferences.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(preferences);
                    }
                });
                builder.setCancelable(false);
                builder.show();
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(Aware_Activity.this);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        //if part of a study, you can't change settings.
                        if( Aware.getSetting(getApplicationContext(), "study_id").length() > 0 ) {
                            Toast.makeText(getApplicationContext(), "As part of a study, no changes are allowed.", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    }
                });
                builder.setNegativeButton("Quit study!", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Toast.makeText(getApplicationContext(), "Clearing settings... please wait", Toast.LENGTH_LONG).show();
                        Aware.reset(getApplicationContext());

                        Intent preferences = new Intent(getApplicationContext(), Aware_Preferences.class);
                        preferences.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(preferences);
                    }
                });
                builder.setTitle("Study information");
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View study_ui = inflater.inflate(R.layout.study_info, null);
                TextView study_name = (TextView) study_ui.findViewById(R.id.study_name);
                TextView study_description = (TextView) study_ui.findViewById(R.id.study_description);
                TextView study_pi = (TextView) study_ui.findViewById(R.id.study_pi);

                try {
                    study_name.setText((result.getString("study_name").length()>0 ? result.getString("study_name"): "Not available"));
                    study_description.setText((result.getString("study_description").length()>0?result.getString("study_description"):"Not available."));
                    study_pi.setText("PI: " + result.getString("researcher_first") + " " + result.getString("researcher_last") + "\nContact: " + result.getString("researcher_contact"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                builder.setView(study_ui);
                builder.setCancelable(false);
                builder.show();
            }
        }
    }*/
}
