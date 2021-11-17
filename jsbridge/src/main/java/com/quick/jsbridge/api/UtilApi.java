package com.quick.jsbridge.api;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Image;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.webkit.WebView;


import com.donkingliang.imageselector.ImageSelectorActivity;
import com.donkingliang.imageselector.utils.ImageSelector;
import com.google.zxing.integration.android.IntentIntegrator;
import com.quick.core.baseapp.component.FileChooseActivity;
import com.quick.core.baseapp.component.scan.ScanCaptureActivity;
import com.quick.core.util.common.JsonUtil;
import com.quick.core.util.device.PhotoSelector;
import com.quick.core.util.io.FileUtil;
import com.quick.jsbridge.bridge.Callback;
import com.quick.jsbridge.bridge.IBridgeImpl;
import com.quick.jsbridge.control.AutoCallbackDefined;
import com.quick.jsbridge.control.WebloaderControl;
import com.quick.jsbridge.takeToSee.FileSplit;
import com.quick.jsbridge.takeToSee.UploadInstance;
import com.quick.jsbridge.view.IQuickFragment;
import com.quick.jsbridge.view.QuickFragment;
import com.quick.jsbridge.view.QuickWebLoader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import me.iwf.photopicker.PhotoPicker;
import me.iwf.photopicker.PhotoPickerActivity;
import quick.com.jsbridge.R;

/**
 * Created by dailichun on 2017/12/6.
 */
public class UtilApi implements IBridgeImpl {

    /**
     * 注册API的别名
     */
    public static String RegisterName = "util";

    /**
     * 打开二维码
     * <p>
     * 参数：
     * videoUrl：视频地址
     */
    public static void scan(IQuickFragment webLoader, WebView wv, JSONObject param, Callback callback) {
        Object fragment = webLoader.getPageControl().getFragment();
        IntentIntegrator integrator = null;
        if (fragment instanceof Fragment) {
            integrator = IntentIntegrator.forFragment((Fragment) fragment);
        }
        if (integrator != null) {
            integrator.setCaptureActivity(ScanCaptureActivity.class);
            integrator.initiateScan();
            webLoader.getWebloaderControl().addPort(AutoCallbackDefined.OnScanCode, callback.getPort());
        }
    }


    /**
     * 选择文件
     */
    public static void selectFile(IQuickFragment webLoader, WebView wv, JSONObject param, Callback callback) {
        try {
            param.putOpt("className", FileChooseActivity.class.getName());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        PageApi.openLocal(webLoader, wv, param, callback);
    }


    /**
     * 多图片选择(配合上传文件API同时使用)
     * <p>
     * 参数：
     * photoCount:可选图片的最大数,默认为9
     * showCamera:是否允许拍照，1：允许；0：不允许，默认不允许
     * showGif：是否显示gif图片，1：显示；0：不显示，默认不显示
     * previewEnabled：是否允许预览，1：允许，0：不允许，默认允许
     * selectedPhotos:已选图片,json数组
     */
    public static void selectImage(IQuickFragment webLoader, WebView wv, JSONObject param, Callback callback) {
        int photoCount = param.optInt("photoCount", 9);
        if (photoCount < 1) {
            callback.applyFail(webLoader.getPageControl().getContext().getString(R.string.status_request_error));
        } else {
            webLoader.getWebloaderControl().addPort(AutoCallbackDefined.OnChoosePic, callback.getPort());
            webLoader.getQuickFragment().selectImage(param);
        }
    }



    /**
     * 打开摄像机拍照
     * <p>
     * 参数：
     * selectedPhotos:图片地址，支持网络图片，手机本地图片
     * index：默认显示图片序号
     * showDeleteButton:是否显示删除按钮，1：显示，0：不显示，默认不显示。如果显示按钮则自动注册回调事件。
     */
    public static void cameraImage(IQuickFragment webLoader, WebView wv, JSONObject param, Callback callback) {
        webLoader.getWebloaderControl().addPort(AutoCallbackDefined.OnChoosePic, callback.getPort());
        webLoader.getQuickFragment().startCamera(param);
    }

    /**
     * 打开文件
     * <p>
     * 参数：
     * path:文件本地路径
     */
    public static void openFile(IQuickFragment webLoader, WebView wv, JSONObject param, Callback callback) {
        String path = param.optString("path");
        File file = new File(path);
        if (!file.exists() || file.isDirectory()) {
            callback.applyFail(webLoader.getPageControl().getContext().getString(R.string.status_request_error));
        } else {
            FileUtil.openFile(webLoader.getPageControl().getActivity(), file);
            callback.applySuccess();
        }
    }

    // 以下是定义的接口

    /**
     * 打开指定的文件夹
     */
    public static void openFolder(IQuickFragment webLoader, WebView wv, JSONObject param, Callback callback) {
        try {
            param.putOpt("className", FileChooseActivity.class.getName());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        PageApi.openLocal(webLoader, wv, param, callback);
    }

    /**
     * 复制文件到指定文件夹
     */
    public static void copyFileToLocation(IQuickFragment webLoader, WebView wv, JSONObject param, Callback callback) throws IOException {
        String path = param.optString("oldpath");
        String desPath = param.optString("despath");
        FileUtil.copyFile(path,desPath);
    }

    /**
     * 新建文件夹
     */
    public static void createFolder(IQuickFragment webLoader, WebView wv, JSONObject param, Callback callback) {
        String path = param.optString("path");
        String rootPath = Environment.getExternalStorageDirectory().getPath();
        FileUtil.foldCreate(rootPath + path);
        callback.applySuccess();
    }

    /**
     * 转换path为file
     *
     */

    public static void pathToBaseImg(IQuickFragment webLoader, WebView wv, JSONObject param, Callback callback) {
        String path = param.optString("path");

        File file = new File(path);
        if (!file.exists() || file.isDirectory()) {
            callback.applyFail(webLoader.getPageControl().getContext().getString(R.string.status_request_error));
        } else {
            String base64 = FileUtil.file2Base64(file);
            HashMap map = new HashMap();
            map.put("result", base64);
            callback.applySuccess(map);
        }
    }

    // 保存数据到本地
    public static void saveFilePaths(IQuickFragment webLoader, WebView wv, JSONObject param, Callback callback) {
        JSONArray paths = param.optJSONArray("paths");
        String saveName = param.optString("saveName");

        SharedPreferences sharedPreferences= webLoader.getPageControl().getContext().getSharedPreferences(saveName, Context.MODE_PRIVATE);
        //步骤2： 实例化SharedPreferences.Editor对象
        SharedPreferences.Editor editor = sharedPreferences.edit();


        try {
            editor.putString("paths", paths.join(","));

            editor.apply();

            callback.applySuccess();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    // 获取保存在本地的数据
    public static void getFileList(IQuickFragment webLoader, WebView wv, JSONObject param, Callback callback) {
        String saveName = param.optString("saveName");

        SharedPreferences sharedPreferences= webLoader.getPageControl().getContext().getSharedPreferences(saveName, Context.MODE_PRIVATE);
        String pathString = sharedPreferences.getString("paths","");

        String[] list = pathString.split(",");
        HashMap map = new HashMap();
        map.put("files", list);

        callback.applySuccess(map);
    }


    public static void getMd5List(IQuickFragment webLoader, WebView wv, JSONObject param, Callback callback) {

        String filePath = param.optString("filePath");
        HashMap md5Obj = FileSplit.calculateFile(filePath, webLoader.getPageControl().getContext());
        callback.applySuccess(md5Obj);
    }

    public static void uploadImage(IQuickFragment webLoader, WebView wv, JSONObject param, Callback callback) {
        webLoader.getWebloaderControl().addPort(AutoCallbackDefined.onUploadSuccess, callback.getPort());

        String reqUrl = param.optString("reqUrl");
        String type = param.optString("type");
        String filePath = param.optString("filePath");

        UploadInstance.UploadImage(reqUrl, type, filePath, webLoader);
    }

    public static void uploadMd5Part(IQuickFragment webLoader, WebView wv, JSONObject param, Callback callback) {
        webLoader.getWebloaderControl().addPort(AutoCallbackDefined.onUploadSuccess, callback.getPort());
        String reqUrl = param.optString("reqUrl");
        String type = param.optString("type");
        int index = param.optInt("index");

        FileSplit.uploadMd5(reqUrl, type, index, webLoader);
    }
}
