package com.quick.jsbridge.takeToSee;

import android.util.Log;

import com.bumptech.glide.RequestBuilder;
import com.quick.jsbridge.view.IQuickFragment;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UploadInstance implements IRtcImpl {

    private static UploadInstance upload = null;
    private static IQuickFragment webLoader;

    public static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");

    public static UploadInstance getInstance() {
        if (upload == null) {
            upload = new UploadInstance();  //在第一次调用getInstance()时才实例化，实现懒加载,所以叫懒汉式
        }
        return upload;
    }

    public static void UploadImage(String reqUrl, String type, String filePath, IQuickFragment webLoader) {
        webLoader = webLoader;
        File file = new File(filePath);
        HashMap map = new HashMap();
        map.put("image", file);
        imageRequest(reqUrl, type, map.toString());
    }

    public static void uploadMd5String(String reqUrl, String type, String md5String, IQuickFragment webLoader) {
        HashMap map = new HashMap();
        map.put("md5String", md5String);
        webLoader = webLoader;
        imageRequest(reqUrl, type, map.toString());

    }

    private static void imageRequest(String url, String type, String json) {

        String reqType = type.toLowerCase(Locale.ROOT);

        if (reqType.equals("post")) {
            postImage(url, json);
        } else if(reqType.equals("put")) {
            putImage(url, json);
        } else if(reqType.equals("patch")) {
            updateImage(url, json);
        } else if (reqType.equals("delete")) {
            deleteImage(url, json);
        }
    }

    private static void putImage(String url, String json) {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .readTimeout(60, TimeUnit.SECONDS) // 设置读取超时时间
                .writeTimeout(60, TimeUnit.SECONDS) // 设置写的超时时间
                .connectTimeout(60, TimeUnit.SECONDS) // 设置连接超时时间
                .build();

        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder().url(url).put(body).build();
        try (Response response= client.newCall(request).execute()) {
            Log.i("UPLOAD INSTANCE", response.body().string());
            HashMap map = new HashMap();
            map.put("type", "success");
            map.put("url", url);
            webLoader.getWebloaderControl().autoCallbackEvent.onUploadSuccess(map);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void postImage(String url, String json) {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .readTimeout(60, TimeUnit.SECONDS) // 设置读取超时时间
                .writeTimeout(60, TimeUnit.SECONDS) // 设置写的超时时间
                .connectTimeout(60, TimeUnit.SECONDS) // 设置连接超时时间
                .build();

        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder().url(url).post(body).build();
        try (Response response = client.newCall(request).execute()) {
            Log.i("UPLOAD INSTANCE", response.body().string());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void updateImage(String url, String json) {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .readTimeout(60, TimeUnit.SECONDS) // 设置读取超时时间
                .writeTimeout(60, TimeUnit.SECONDS) // 设置写的超时时间
                .connectTimeout(60, TimeUnit.SECONDS) // 设置连接超时时间
                .build();

        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder().url(url).patch(body).build();
        try (Response response= client.newCall(request).execute()) {
            Log.i("UPLOAD INSTANCE", response.body().string());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static void deleteImage(String url, String json) {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .readTimeout(60, TimeUnit.SECONDS) // 设置读取超时时间
                .writeTimeout(60, TimeUnit.SECONDS) // 设置写的超时时间
                .connectTimeout(60, TimeUnit.SECONDS) // 设置连接超时时间
                .build();

        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder().url(url).delete(body).build();
        try (Response response= client.newCall(request).execute()) {
            Log.i("UPLOAD INSTANCE", response.body().string());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
