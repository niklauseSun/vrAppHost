package com.vapp.android;
import android.Manifest;
import android.content.Context;
import android.content.Intent;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.quick.core.baseapp.baseactivity.FrmBaseActivity;
import com.quick.jsbridge.bean.QuickBean;
import com.quick.jsbridge.control.AutoCallbackDefined;
import com.quick.jsbridge.control.WebloaderControl;
import com.quick.jsbridge.view.QuickFragment;


import org.json.JSONArray;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;
import quick.com.jsbridge.R;


/**
 * Created by dailichun on 2017/12/7.
 * 如果需要自定义quick容器请继承QuickWebLoader，在布局文件中必须定义QuickFragment的容器FrameLayout控件
 */
public class QuickLoader extends FrmBaseActivity implements EasyPermissions.PermissionCallbacks {
    private static final int REQUEST_CODE_QRCODE_PERMISSIONS = 1;

    private String baseRequestUrl = "https://console.mspaco.com.sg/prod-api/mate-system/dict/list-value?code=appconf";

    public QuickFragment fragment;

    public QuickBean bean;

    private Handler mHandler;
    private Handler handler;
    private int timeCount = 6;
    private Button countButton;

    private ImageView spImageView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //隐藏Activity的导航栏
        pageControl.getNbBar().hide();

//        initQuickBean(savedInstanceState);
        requestBaseUrl();

        setContentView(R.layout.quick_activity);

//        addFragment(R.id.frgContent);


        spImageView = (ImageView) findViewById(R.id.imageView);
        countButton = (Button) findViewById(R.id.countButtonId);
        SharedPreferences share = getSharedPreferences("data", Context.MODE_PRIVATE);
        handler = new Handler();
        mHandler = new Handler();


        handlerPostDelayed();
        handler.postDelayed(runnable, timeCount * 1000);
        countButton.setVisibility(View.GONE);

        countButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                String jumpUrl = share.getString("baseReqUrl","https://m.mspace.com.sg/mobile/pages/client/home");
//                nomalInit(jumpUrl);
                countButton.setVisibility(View.GONE);
                spImageView.setVisibility(View.GONE);
                handler.removeCallbacks(runnable);
                mHandler.removeCallbacks(mRunnable);
                requestCodeQRCodePermissions();
            }
        });


        String guideUrl = share.getString("guideImage","");
        Log.i("guideUrl", guideUrl);
        if (!guideUrl.isEmpty()) {
            Glide.with(this).load(guideUrl).placeholder(R.drawable.bg_splash).error(R.drawable.bg_splash).into(spImageView);
//            Glide.with(this).load(R.drawable.bg_splash).into(spImageView);
        } else {
            Glide.with(this).load(R.drawable.bg_splash).into(spImageView);
        }

    }

    /**
     * 初始化页面参数
     *
     * @param savedInstanceState
     */
    public void initQuickBean(Bundle savedInstanceState) {
        if (null != savedInstanceState && savedInstanceState.containsKey("bean")) {
            bean = (QuickBean) savedInstanceState.getSerializable("bean");
        } else if (getIntent().hasExtra("bean")) {
            bean = (QuickBean) getIntent().getSerializableExtra("bean");
        }

        if (bean == null) {
            toast(getString(R.string.status_data_error));
            finish();
            return;
        }
    }

    /**
     * 添加QuickFragment容器
     *
     * @param containerViewId FrameLayout的视图id
     */
    public void addFragment(int containerViewId) {
        //初始化页面
        fragment = QuickFragment.newInstance(bean);
        getFragmentManager().beginTransaction().add(containerViewId, fragment).commit();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
        mHandler.removeCallbacks(mRunnable);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (bean != null) {
            outState.putSerializable("bean", bean);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState.containsKey("bean")) {
            bean = (QuickBean) savedInstanceState.getSerializable("bean");
        }
    }

    @Override
    public void onBackPressed() {
        WebloaderControl control = fragment.getWebloaderControl();
        if (control != null) {
            if (control.autoCallbackEvent.isRegist(AutoCallbackDefined.OnClickBack)) {
                control.autoCallbackEvent.onClickBack();
            } else {
                control.loadLastPage(false);
            }
        } else {
            super.onNbBack();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (fragment.getQuickWebView().canGoBack()) {
                fragment.getQuickWebView().goBack();
            } else {
                SharedPreferences pref = getSharedPreferences("clickData",MODE_PRIVATE);
                long current = new Date().getTime();
                long lastTime = pref.getLong("lastClick", 0);
                if ((current - lastTime) < 3000) {
                    this.onBackPressed();
                } else {
                    SharedPreferences.Editor editor = getSharedPreferences("clickData",MODE_PRIVATE).edit();
                    editor.putLong("lastClick",current);
                    editor.commit();
                    Toast.makeText(fragment.getContext(), "Press back button again to exit program", Toast.LENGTH_SHORT).show();
                }

            }
        }
        return false;
    }

    public static void go(Context context, QuickBean bean) {
        Intent intent = new Intent(context, com.quick.jsbridge.view.QuickWebLoader.class);
        intent.putExtra("bean", bean);
        context.startActivity(intent);
    }

    public static void go(Context context, String url) {
        go(context, new QuickBean(url));
    }

    // 白屏
    private void requestBaseUrl() {
        new Thread(new Runnable(){
            @Override
            public void run() {
                OkHttpClient client = new OkHttpClient().newBuilder()
                        .readTimeout(60, TimeUnit.SECONDS) // 设置读取超时时间
                        .writeTimeout(60, TimeUnit.SECONDS) // 设置写的超时时间
                        .connectTimeout(60, TimeUnit.SECONDS) // 设置连接超时时间
                        .build();
                Request request = new Request.Builder().url(baseRequestUrl).get().build();
                Log.i("test", "requestBaseUrl");
                try (Response response= client.newCall(request).execute()) {
                    JSONObject obj = new JSONObject(response.body().string());
                    Log.i("test", obj.toString());
                    JSONArray array = obj.optJSONArray("data");
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject st = array.getJSONObject(i);
                        String dictKey = st.optString("dictKey");
                        String dictValue = st.optString("dictValue");

                        if (dictKey.equals("home")) {
                            compareUrl(dictValue);
                        } else if (dictKey.equals("pic")) {
                            // 更改guide
                            //步骤1：创建一个SharedPreferences对象
                            SharedPreferences share = getSharedPreferences("data", Context.MODE_PRIVATE);
                            //步骤2： 实例化SharedPreferences.Editor对象
                            SharedPreferences.Editor editor = share.edit();
                            //步骤3：将获取过来的值放入文件
                            editor.putString("guideImage", dictValue);
                            //步骤4：提交
                            editor.commit();
                        }

                        Log.i("key", dictKey);
                        Log.i("value", dictValue);
                    }
                } catch (Exception e) {
                    Log.i("test", "test");
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void compareUrl(String newUrl) {
        SharedPreferences sharedPreferences= getSharedPreferences("data", Context .MODE_PRIVATE);
        String oldUrl = sharedPreferences.getString("url","https://m.mspace.com.sg/mobile/pages/client/home");
        pageControl.getNbBar().hide();

        if (!newUrl.equals(oldUrl)) {

            //步骤1：创建一个SharedPreferences对象
            SharedPreferences share = getSharedPreferences("data", Context.MODE_PRIVATE);
            //步骤2： 实例化SharedPreferences.Editor对象
            SharedPreferences.Editor editor = share.edit();
            //步骤3：将获取过来的值放入文件
            editor.putString("baseReqUrl", newUrl);
            //步骤4：提交
            editor.commit();
        }

        initBean(newUrl);

    }

    private void initBean(String baseUrl) {
        QuickBean be = new QuickBean(baseUrl);
        be.pageStyle = -1;
        bean = be;
        addFragment(R.id.frgContent);
    }

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            timeCount--;
            String str = timeCount + "s";
            countButton.setText(str);
            handlerPostDelayed();
            if (timeCount <= 5 && timeCount >= 0) {
                countButton.setVisibility(View.VISIBLE);
            }
        }
    };
    // handler+postDelayed 方式，反复发送延时消息
    private void handlerPostDelayed() {
        mHandler.postDelayed(mRunnable, 1000);
    }

    private Runnable runnable = new Runnable(){
        @Override
        public void run() {
            // TODO Auto-generated method stub
            //要做的事情，这里再次调用此Runnable对象，以实现每两秒实现一次的定时器操作
            spImageView.setVisibility(View.GONE);
            countButton.setVisibility(View.GONE);
            requestCodeQRCodePermissions();
            mHandler.removeCallbacks(mRunnable);
        }
    };

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {

    }

    @AfterPermissionGranted(REQUEST_CODE_QRCODE_PERMISSIONS)
    private void requestCodeQRCodePermissions() {
        String [] perms = {Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (!EasyPermissions.hasPermissions(this, perms)) {
            EasyPermissions.requestPermissions(this, "扫描二维码需要打开相机和散光灯的权限", REQUEST_CODE_QRCODE_PERMISSIONS, perms);
        }
    }
}

