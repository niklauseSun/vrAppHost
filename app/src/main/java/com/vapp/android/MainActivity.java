package com.vapp.android;

import androidx.annotation.NonNull;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.donkingliang.imageselector.utils.ImageSelector;
import com.qmuiteam.qmui.arch.QMUISwipeBackActivityManager;
import com.qmuiteam.qmui.widget.dialog.QMUIDialog;
import com.qmuiteam.qmui.widget.dialog.QMUIDialogAction;
import com.quick.core.baseapp.baseactivity.FrmBaseActivity;
import com.quick.jsbridge.bean.QuickBean;
import com.quick.jsbridge.view.QuickWebLoader;
import com.vapp.android.activitys.CallActivity;
import com.vapp.android.activitys.MessageSend;
import com.silversea.activity.LoginActivity;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends FrmBaseActivity implements EasyPermissions.PermissionCallbacks {

    private static final int REQUEST_CODE_QRCODE_PERMISSIONS = 1;

    private Button inputButton = null;
    private Button defaultButton = null;
    private Button prevButton = null;
    private Button scanButton = null;
    private Button callTest = null;
    private Button goToMessageButton = null;
    private Button selectImageButton = null;
    private Button jumpToShowVr = null;
    private ImageView showImage = null;

    private Context mContext = this;

    public static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");

    private static String prevUrlKey = "prevUrl";
    private static String defaultUrl = "http://10.12.254.231:8080/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (BuildConfig.DEBUG) {
            nomalInit("http://10.12.254.140:8080/");
        } else {
            requestBaseUrl();
        }
//        testInit();
        pageControl.getNbBar().hide();
    }

    private void nomalInit(String url) {
//        String url = "https://m.mspace.com.sg/mobile/";
        Intent mintent = new Intent(MainActivity.this, QuickWebLoader.class);

        QuickBean bean = new QuickBean(url);
        bean.pageStyle = -1;
        mintent.putExtra("bean", bean);
        mintent.setFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME);

        startActivity(mintent);
        pageControl.getNbBar().hide();

        requestCodeQRCodePermissions();
        this.finish();
    }

    private void compareUrl(String newUrl) {
        SharedPreferences sharedPreferences= getSharedPreferences("data", Context .MODE_PRIVATE);
        String oldUrl = sharedPreferences.getString("url","https://m.mspace.com.sg/mobile/");
        pageControl.getNbBar().hide();

        if (!newUrl.equals(oldUrl)) {
            nomalInit(newUrl);

            //步骤1：创建一个SharedPreferences对象
            SharedPreferences share = getSharedPreferences("data", Context.MODE_PRIVATE);
            //步骤2： 实例化SharedPreferences.Editor对象
            SharedPreferences.Editor editor = share.edit();
            //步骤3：将获取过来的值放入文件
            editor.putString("baseReqUrl", newUrl);
            //步骤4：提交
            editor.commit();
        } else {
            nomalInit(newUrl);
        }
    }

    private void testInit() {
        setContentView(R.layout.activity_main);

        requestCodeQRCodePermissions();

        pageControl.getNbBar().hide();

        inputButton = findViewById(R.id.inputButton);
        defaultButton = findViewById(R.id.defaultButton);
        prevButton = findViewById(R.id.prevButton);
        scanButton = findViewById(R.id.scan_button);
//        callTest = findViewById(R.id.goToCall);
//        goToMessageButton = findViewById(R.id.goToMessage);
        selectImageButton = findViewById(R.id.selectImage);
        showImage = findViewById(R.id.showImage);
        jumpToShowVr = findViewById(R.id.jumpToShowVr);

        inputButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInputDialog();
            }
        });

        defaultButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                jumpToWebView(mContext, defaultUrl);
            }
        });

        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = getPrveUrl();
                if (!url.isEmpty()) {
                    jumpToWebView(mContext, url);
                } else {
                    Toast.makeText(mContext, "未输入过网址！",Toast.LENGTH_SHORT).show();
                }
            }
        });

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mintent = new Intent(MainActivity.this, QuickWebLoader.class);
                QuickBean bean = new QuickBean("https://www.baidu.com");
                mintent.putExtra("bean", bean);
                startActivity(mintent);
            }
        });

//        callTest.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent mintent = new Intent(MainActivity.this, CallActivity.class);
//
//                startActivity(mintent);
//            }
//        });

//        goToMessageButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent mintent = new Intent(MainActivity.this, MessageSend.class);
//
//                startActivity(mintent);
//            }
//        });

        selectImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //单选
                ImageSelector.builder()
                        .useCamera(true) // 设置是否使用拍照
                        .setSingle(true)  //设置是否单选
                        .canPreview(true) //是否可以预览图片，默认为true
                        .start(getActivity(), ImageSelector.RESULT_CODE); // 打开相册

            }
        });

        jumpToShowVr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent mIntent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(mIntent);

//                String url = "https://beyond.3dnest.biz/silversea_dev/takelook/?m=7051c064_o0fM_b6f9";
//                jumpToWebView(mContext, url);
            }
        });
    }


    private void showInputDialog() {
        final QMUIDialog.EditTextDialogBuilder builder = new QMUIDialog.EditTextDialogBuilder(getContext());
        builder.setTitle("输入网址")
                .setPlaceholder("在此输入您要跳转的网址")
                .setInputType(InputType.TYPE_CLASS_TEXT)
                .addAction("取消", new QMUIDialogAction.ActionListener() {
                    @Override
                    public void onClick(QMUIDialog dialog, int index) {
                        dialog.dismiss();
                    }
                })
                .addAction("确定", new QMUIDialogAction.ActionListener() {
                    @Override
                    public void onClick(QMUIDialog dialog, int index) {
                        CharSequence text = builder.getEditText().getText();
                        if (isUrl(text.toString())) {
                            // 如果是URL 需要将这个保存下来并跳转

                            saveUrl(text.toString());
                            Log.d("MainActivity", text.toString());
                            jumpToWebView(mContext, text.toString());
                        } else {
                            // 提示不是
                            Toast.makeText(mContext, "请输入正确的地址！", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        builder.show();
    }

    private void saveUrl(String url) {
        // 步骤1：创建一个SharedPreferences对象
        SharedPreferences sharedPreferences= getSharedPreferences("data",Context.MODE_PRIVATE);
        // 步骤2： 实例化SharedPreferences.Editor对象
        SharedPreferences.Editor editor = sharedPreferences.edit();
        // 步骤3：将获取过来的值放入文件
        editor.putString(prevUrlKey, url);
        // 提交
        editor.commit();
    }

    private String getPrveUrl() {
        //步骤1：创建一个SharedPreferences对象
        SharedPreferences sharedPreferences= getSharedPreferences("data",Context.MODE_PRIVATE);
        //步骤2： 实例化SharedPreferences.Editor对象
        String url = sharedPreferences.getString(prevUrlKey, "");
        return url;
    }

    private void jumpToWebView(Context context, String url) {
        Intent mintent = new Intent(MainActivity.this, QuickWebLoader.class);
        QuickBean bean = new QuickBean(url);
        mintent.putExtra("bean", bean);
        startActivity(mintent);

        requestCodeQRCodePermissions();
//        Intent starter = new Intent(context, VWebView.class);
//        starter.putExtra("loadUrl", url);
//        startActivity(starter);
    }

    private static String pattern = "^([hH][tT]{2}[pP]://|[hH][tT]{2}[pP][sS]://)(([A-Za-z0-9-~]+).)+([A-Za-z0-9-~\\/])+$";

    /**
     * 判断 url 是否合法
     */
    public static boolean isUrl(String url) {
        Pattern httpPattern = Pattern.compile(pattern);
        if (httpPattern.matcher(url).matches()) {
            return true;
        }
        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
    }

    @AfterPermissionGranted(REQUEST_CODE_QRCODE_PERMISSIONS)
    private void requestCodeQRCodePermissions() {
        String [] perms = {Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (!EasyPermissions.hasPermissions(this, perms)) {
            EasyPermissions.requestPermissions(this, "扫描二维码需要打开相机和散光灯的权限", REQUEST_CODE_QRCODE_PERMISSIONS, perms);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ImageSelector.RESULT_CODE) {
            //选择或预览图片回传值
            ArrayList<String> photos = null;
            if (data != null) {
                photos = data.getStringArrayListExtra(ImageSelector.SELECT_RESULT);
                Uri uri = Uri.parse(photos.get(0));
                showImage.setImageURI(uri);
            }
        }
    }

    private void requestBaseUrl() {

        new Thread(new Runnable(){
            @Override
            public void run() {
                // Do network action in this function
                String url = "https://jiance.99rongle.com/prod-api/mate-component/config/get-h5-url";
                OkHttpClient client = new OkHttpClient().newBuilder()
                        .readTimeout(60, TimeUnit.SECONDS) // 设置读取超时时间
                        .writeTimeout(60, TimeUnit.SECONDS) // 设置写的超时时间
                        .connectTimeout(60, TimeUnit.SECONDS) // 设置连接超时时间
                        .build();
                RequestBody body = RequestBody.create("", JSON);
                Request request = new Request.Builder().url(url).post(body).build();
                Log.i("test", "requestBaseUrl");
                try (Response response= client.newCall(request).execute()) {
                    JSONObject obj = new JSONObject(response.body().string());
                    String st = obj.optString("data");
                    Log.e("download url", st);
                    compareUrl(st);
                } catch (Exception e) {
                    Log.i("test", "test");
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
