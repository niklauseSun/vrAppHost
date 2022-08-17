package com.vapp.android;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.quick.core.baseapp.baseactivity.FrmBaseActivity;
import com.quick.jsbridge.bean.QuickBean;
import com.quick.jsbridge.view.QuickWebLoader;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class SplashActivity extends FrmBaseActivity implements EasyPermissions.PermissionCallbacks {

    private static final int REQUEST_CODE_QRCODE_PERMISSIONS = 1;
    private Handler mHandler;
    private Handler handler;
    private int timeCount = 5;
    private Button countButton;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestBaseUrl();

        setContentView(R.layout.splash_view);

        ImageView imageView = (ImageView) findViewById(R.id.imageView);
//        countButton = (Button) findViewById(R.id.countButtonId);
        SharedPreferences share = getSharedPreferences("data", Context.MODE_PRIVATE);
        handler = new Handler();
        mHandler = new Handler();


        handlerPostDelayed();

        handler.postDelayed(runnable, 3000);

//        countButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                String jumpUrl = share.getString("baseReqUrl","https://m.mspace.com.sg/mobile/pages/client/home");
//                nomalInit(jumpUrl);
//                handler.removeCallbacks(runnable);
//                mHandler.removeCallbacks(mRunnable);
//            }
//        });


        String guideUrl = share.getString("guideImage","");
        Log.i("guideUrl", guideUrl);
        if (!guideUrl.isEmpty()) {
            Glide.with(this).load(guideUrl).into(imageView);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
        mHandler.removeCallbacks(mRunnable);
    }

    private Runnable runnable = new Runnable(){
        @Override
        public void run() {
            // TODO Auto-generated method stub
            //要做的事情，这里再次调用此Runnable对象，以实现每两秒实现一次的定时器操作
            SharedPreferences share = getSharedPreferences("data", Context.MODE_PRIVATE);
            String baseUrl = share.getString("baseReqUrl", "https://m.mspace.com.sg/mobile/pages/client/home");
//            nomalInit(baseUrl);
        }
    };

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
//            timeCount--;
//            String str = timeCount + "s";
//            countButton.setText(str);
            handlerPostDelayed();
        }

    };
    // handler+postDelayed 方式，反复发送延时消息
    private void handlerPostDelayed() {
        mHandler.postDelayed(mRunnable, 1000);
    }

    private void nomalInit() {
//        String url = "https://m.mspace.com.sg/mobile/";
        SharedPreferences share = getSharedPreferences("data", Context.MODE_PRIVATE);
        String baseUrl = share.getString("baseReqUrl", "https://m.mspace.com.sg/mobile/pages/client/home");
        Intent mintent = new Intent(SplashActivity.this, QuickWebLoader.class);

        QuickBean bean = new QuickBean(baseUrl);
        bean.pageStyle = -1;
        mintent.putExtra("bean", bean);
        mintent.setFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME);

        startActivity(mintent);
        pageControl.getNbBar().hide();

        requestCodeQRCodePermissions();
        this.finish();
    }

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
//        nomalInit(newUrl);
    }

    private void requestBaseUrl() {
        new Thread(new Runnable(){
            @Override
            public void run() {
                String url = "https://console.mspace.com.sg/prod-api/mate-system/dict/list-value?code=appconf";
                OkHttpClient client = new OkHttpClient().newBuilder()
                        .readTimeout(60, TimeUnit.SECONDS) // 设置读取超时时间
                        .writeTimeout(60, TimeUnit.SECONDS) // 设置写的超时时间
                        .connectTimeout(60, TimeUnit.SECONDS) // 设置连接超时时间
                        .build();
                Request request = new Request.Builder().url(url).get().build();
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
                    nomalInit();
                } catch (Exception e) {
                    Log.i("test", "test");
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
