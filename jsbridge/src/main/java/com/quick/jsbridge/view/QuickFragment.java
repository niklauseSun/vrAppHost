package com.quick.jsbridge.view;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import com.donkingliang.imageselector.utils.ImageSelector;
import com.quick.core.baseapp.baseactivity.FrmBaseFragment;
import com.quick.core.baseapp.baseactivity.control.PageControl;
import com.quick.core.ui.app.IPageControl;
import com.quick.core.util.common.JsonUtil;
import com.quick.jsbridge.bean.QuickBean;
import com.quick.jsbridge.control.AutoCallbackDefined;
import com.quick.jsbridge.control.WebloaderControl;
import com.quick.jsbridge.takeToSee.AGChatManager;
import com.quick.jsbridge.takeToSee.AgApplication;
import com.quick.jsbridge.view.webview.QuickWebView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtm.ErrorInfo;
import io.agora.rtm.ResultCallback;
import io.agora.rtm.RtmClient;
import io.agora.rtm.RtmClientListener;
import io.agora.rtm.RtmFileMessage;
import io.agora.rtm.RtmImageMessage;
import io.agora.rtm.RtmMediaOperationProgress;
import io.agora.rtm.RtmMessage;
import io.agora.rtm.RtmStatusCode;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import pub.devrel.easypermissions.EasyPermissions;
import quick.com.jsbridge.R;

/**
 * Created by dailichun on 2017/12/7.
 * quick的fragment容器，如果要加载H5页面请使用{@link QuickWebLoader}
 */
public class QuickFragment extends FrmBaseFragment implements IQuickFragment {

    private String modelID = "7051c064_o0fM_b6f9";
    private String modelURL = "https://beyond.3dnest.biz/silversea_dev/takelook/?m="+modelID;

    private final String MESSAGE_TAG = "RTM_MESSAGE_TAG";

    /**
     * 浏览器控件
     */
    private QuickWebView wv;
    /**
     * 初始化属性
     */
    private QuickBean bean;

    /**
     * 控制器
     */
    private WebloaderControl control;

    /**
     * H5加载进度条
     */
//    private ProgressBar pb;

    private Handler mHandler;
    /**
     * 声网代码
     */
    private RtmClient rtmClient;
    private AGChatManager chatManager;
    private RtmClientListener rtmClientListener;

    // 目标用户
    private String mPeerId = "369369";
    private String mUserId = "123333";

    private String channelName = "";

    private RtcEngine rtcEngine;
    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 22;

    /**
     * 客服头像
     */
    private String bussinessHeadImage;
    /**
     * 客服身份码
     */
    private String bussinessIdentity;

    /**
     * 客服昵称
     */
    private String bussinessNickname;

    /**
     * 客服id
     */
    private String bussinessUid;

    /**
     * 客服手机号
     */
    private String bussinessAccid;

    /**
     * 客户头像
     */
    private String customerHeadImage;

    /**
     * 客户码
     */
    private String customerIdentity;

    /**
     * 客户昵称
     */
    private String customerNickname;

    /**
     * 客户id
     */
    private String customerUid;

    /**
     * 客户手机号
     */
    private String customerAccid;

    /**
     * 声网监听
     */
    private final IRtcEngineEventHandler rtcEngineEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            super.onJoinChannelSuccess(channel, uid, elapsed);
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            super.onUserOffline(uid, reason);
        }
    };

    public QuickFragment() {
    }

    public static QuickFragment newInstance(QuickBean bean) {
        QuickFragment fragment = new QuickFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable("bean", bean);
        bundle.putInt(PageControl.PAGE_STYLE, bean.pageStyle);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getActivity().setTheme(R.style.ActionSheetStyleIOS7);
        setLayout(R.layout.quick_fragment);

        bean = (QuickBean) getArguments().getSerializable("bean");

        mHandler = new Handler();

        // 初始化聊天
        initChat();
        // 初始化控件
        initView();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * 初始化布局控件
     */
    protected void initView() {
//        pb = (ProgressBar) findViewById(R.id.pb);
        wv = (QuickWebView) findViewById(R.id.wv);

        //初始化控制器
        control = new WebloaderControl(this, bean, wv);


        //设置错误状态页点击事件
        pageControl.getStatusPage().setClickButton(getString(R.string.status_page_reload),new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //加载历史记录最近一页
                control.loadLastPage(true);
            }
        });

        wv.addJavascriptInterface(new JavaScriptInterface(), "WebBridge");

        //加载页面
        control.loadPage();
    }

    protected void initChat() {
        chatManager = AgApplication.getInstance(this.getActivity()).getChatManager();
        rtmClient = chatManager.getRtmClient();

        rtmClientListener = new MyRtmClientListener();
        chatManager.registerListener(rtmClientListener);
    }

    // native调用js
    public void callOnData(final String data) {
        wv.post(new Runnable() {
            @Override
            public void run() {
                String url = "javascript:onData('" + data + "')";
                wv.loadUrl(url);
            }
        });
    }

    // native 调用js
    public void callUpdateChatStatus(final String status, String data) {
        wv.post(new Runnable() {
            @Override
            public void run() {
                String url = "javascript:updateChatStatus('" + status + "')";
                wv.loadUrl(url);
            }
        });

        // 7：被叫方挂断；8：主叫方挂断
        if (status == "8" || status == "7") {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // 直接reload
                    hangupViewRefresh();
                }
            }, 3000);
        }
    }

    public void hangupViewRefresh() {
        wv.reload();
    }

    @Override
    public IPageControl getPageControl() {
        return pageControl;
    }

    @Override
    public WebloaderControl getWebloaderControl() {
        return control;
    }

    @Override
    public QuickWebView getQuickWebView() {
        return wv;
    }

    @Override
    public void setQuickBean(QuickBean bean) {
        this.bean = bean;
    }

    @Override
    public QuickBean getQuickBean() {
        return bean;
    }

//    @Override
//    public ProgressBar getProgressBar() {
//        return pb;
//    }

    @Override
    public QuickFragment getQuickFragment() {
        return this;
    }

    public void startCamera(JSONObject param) {
        Boolean crop = "1".equals(param.optString("corp", "0"));
        ImageSelector.builder()
                .setCrop(crop) // 设置是否使用图片剪切功能。
                .setCropRatio(1.0f) // 图片剪切的宽高比,默认1.0f。宽固定为手机屏幕的宽。
                .onlyTakePhoto(true)  // 仅拍照，不打开相册
                .start(this, ImageSelector.RESULT_CODE);
    }

    public void selectImage(JSONObject param) {
        int photoCount = param.optInt("photoCount", 9);
        boolean showCamera = "1".equals(param.optString("showCamera", "0"));
        boolean previewEnabled = "1".equals(param.optString("previewEnabled", "1"));
        String[] items = new String[]{};
        JSONArray itemsJsonObject = param.optJSONArray("selectedPhotos");
        items = JsonUtil.parseJSONArray(itemsJsonObject, items);
        ArrayList<String> selectedPhotos = new ArrayList<>(Arrays.asList(items));
        ImageSelector.builder()
                .useCamera(showCamera) // 设置是否使用拍照
                .setSelected(selectedPhotos)
                .setSingle(false)  //设置是否单选
                .setMaxSelectCount(photoCount) // 图片的最大选择数量，小于等于0时，不限数量。
                .canPreview(previewEnabled) //是否可以预览图片，默认为true
                .start(this, ImageSelector.RESULT_CODE); // 打开相册
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == PERMISSION_REQ_ID_RECORD_AUDIO) {
            initAgoraEngineAndJoinChannel();
            joinChannel();
        } else {
            control.onResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        control.onResume();

    }

    @Override
    public void onPause() {
        super.onPause();
        control.onPause();
    }

    @Override
    public void onDestroyView() {
        control.onDestroy();
        super.onDestroyView();

        leaveChannel();
        RtcEngine.destroy();
        rtcEngine = null;
    }

    @Override
    public void onNbRight(View view, int which) {
        super.onNbRight(view, which);
        control.autoCallbackEvent.onClickNbRight(which);
    }

    @Override
    public void onNbLeft(View view) {
        super.onNbLeft(view);
        if (view.getTag() != null && "close".equals(view.getTag().toString())) {
            super.onNbBack();
        } else {
            control.autoCallbackEvent.onClickNbLeft();
        }
    }

    @Override
    public void onNbTitle(View view) {
        super.onNbTitle(view);
        control.autoCallbackEvent.onClickNbTitle(0);
    }

    @Override
    public void onNbBack() {
        if (control.autoCallbackEvent.isRegist(AutoCallbackDefined.OnClickNbBack)) {
            control.autoCallbackEvent.onClickNbBack();
        } else{
            control.loadLastPage(false);
        }
    }

    @Override
    public void onNbSearch(String keyWord) {
        super.onNbSearch(keyWord);
        keyWord = keyWord.replace("\\", "\\\\").replace("'", "\\'");
        Map<String, Object> object = new HashMap<>();
        object.put("keyword", keyWord);
        control.autoCallbackEvent.onSearch(object);
    }

    /**
     * RTM Event listener
     */

    class MyRtmClientListener implements RtmClientListener {
        @Override
        public void onConnectionStateChanged(final int state, int reason) {
            switch (state) {
                case RtmStatusCode.ConnectionState.CONNECTION_STATE_RECONNECTING:
                    Log.i(MESSAGE_TAG, getString(R.string.reconnecting));
                    break;
                case RtmStatusCode.ConnectionState.CONNECTION_STATE_ABORTED:
                    Log.i(MESSAGE_TAG, getString(R.string.account_offline));
                    break;
            }
        }

        @Override
        public void onMessageReceived(RtmMessage rtmMessage, String peerId) {
            Log.e(MESSAGE_TAG, "onMessageReceived === " + rtmMessage.getText() + "peerId:" + peerId);
            try {
                JSONObject jsonObject = new JSONObject(rtmMessage.getText());

                if (jsonObject.has("data")
                        && jsonObject.getJSONObject("data").has("state")
                        && jsonObject.getJSONObject("data").getString("state").equals("initdone")
                ) {
                    // webview 初始化消息
                    // 仅支持1v1，拒绝其他客户带看请求
                    if (mPeerId.equals("") && !mPeerId.equals(peerId)) {
                        Log.d(MESSAGE_TAG,"=========================接收消息处理-peerId.equals(mPeerId)");
                        Log.d(MESSAGE_TAG,rtmMessage.getText());
                    } else {
                        Log.d(MESSAGE_TAG,"=========================接收消息处理-!peerId.equals(mPeerId)");
                        Log.d(MESSAGE_TAG,rtmMessage.getText());
                        callOnData(rtmMessage.getText());
                        callUpdateChatStatus("3", null);
                    }
                } else if (jsonObject.has("type")
                    && jsonObject.getString("type").equals("app-hangup")
                ) {
                    // 临时通过消息接收主叫方挂断电话
                    // 暂时仅支持1V1，拒绝其他客户带看请求
                    Log.d(MESSAGE_TAG,"=========================接收消息处理-经纪人挂断消息-type-app-hangup");
                    Log.d(MESSAGE_TAG,rtmMessage.getText());
                    if (jsonObject.has("hangupType")
                            && jsonObject.getString("hangupType").equals("7")){
                        callUpdateChatStatus("7", null);
                    }else{
                        // hangupType=5
                        // 被叫⽅拒接语⾳呼叫（顾问拒接）
                        callUpdateChatStatus("3", null);
                        callUpdateChatStatus("5", null);
                    }
                } else {
                    if (!peerId.isEmpty() && !mPeerId.equals(peerId)) {
//                            return true;
                        Log.d(MESSAGE_TAG,"=========================接收消息处理-aaaa");
                        Log.d(MESSAGE_TAG,rtmMessage.getText());
                    } else {
                        if(rtmMessage.getText().indexOf("initstatedone")>0){
//                                callUpdateChatStatus("103", null);
                            Log.d(MESSAGE_TAG,"=========================接收消息处理-bbbbcccccc");
                        }
                        Log.d(MESSAGE_TAG,"=========================接收消息处理-bbbb");
                        Log.d(MESSAGE_TAG,rtmMessage.getText());
                        callOnData(rtmMessage.getText());
                    }
                    Log.d(MESSAGE_TAG,"=========================接收消息处理-type-app-else-hangup");
                    Log.d(MESSAGE_TAG,rtmMessage.getText());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onImageMessageReceivedFromPeer(RtmImageMessage rtmImageMessage, String s) {

        }

        @Override
        public void onFileMessageReceivedFromPeer(RtmFileMessage rtmFileMessage, String s) {

        }

        @Override
        public void onMediaUploadingProgress(RtmMediaOperationProgress rtmMediaOperationProgress, long l) {

        }

        @Override
        public void onMediaDownloadingProgress(RtmMediaOperationProgress rtmMediaOperationProgress, long l) {

        }

        @Override
        public void onTokenExpired() {

        }

        @Override
        public void onPeersOnlineStatusChanged(Map<String, Integer> map) {

        }
    }


    // 用于JS调用Native
    public class JavaScriptInterface {
        // 传输数据
        @JavascriptInterface
        public void sendData(String data) {
            RtmMessage message = rtmClient.createMessage();

            message.setText(data);
            sendPeerMessage(message);
        }

        @JavascriptInterface
        public void sendUserInfo(String data) {
            try {
                JSONObject obj = new JSONObject(data);
                if (obj.has("bussiness")) {
                    JSONObject business = obj.getJSONObject("bussiness");
                    bussinessHeadImage = business.getString("bussinessHeadImage");
                    bussinessIdentity = business.getString("bussinessIdentity");
                    bussinessNickname = business.getString("bussinessNickname");
                    bussinessUid = business.getString("bussinessUid");
                    bussinessAccid = business.getString("bussinessAccid");
                    mPeerId = bussinessUid;
                }

                if (obj.has("modelUrl")) {
                    modelURL = obj.getString("modelUrl");
                }

                if (obj.has("customer")) {
                    JSONObject customer = obj.getJSONObject("customer");
                    customerHeadImage = customer.getString("customerHeadImage");
                    customerIdentity = customer.getString("customerIdentity");
                    customerNickname = customer.getString("customerNickname");
                    customerUid = customer.getString("customerUid");
                    customerAccid = customer.getString("customerAccid");
                    mUserId = customerUid;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        // 获取用户信息
        @JavascriptInterface
        public String getUserInfo() {
            // 测试用信息，生产环境下请使用真实信息
            JSONObject customer = new JSONObject();
            try {
                customer.put("customerHeadImage", customerHeadImage);//"./images/default_avator.png"
                customer.put("customerIdentity", customerIdentity);//"4"
                customer.put("customerNickname", customerNickname);//"小A"
                customer.put("customerUid", customerUid);//"1"
                customer.put("customerAccid", customerAccid);//"15261805000"
            } catch (JSONException e) {
                e.printStackTrace();
            }


            JSONObject bussiness = new JSONObject();
            try {
                bussiness.put("bussinessHeadImage", bussinessHeadImage);//"./images/default_avator.png"
                bussiness.put("bussinessIdentity", bussinessIdentity);//"3"
                bussiness.put("bussinessNickname", bussinessNickname);//"小B"
                bussiness.put("bussinessUid", bussinessUid);//"2"
                bussiness.put("bussinessAccid", bussinessAccid);//"15261805001"
            } catch (JSONException e) {
                e.printStackTrace();
            }

            JSONObject userInfo = new JSONObject();
            try {
                userInfo.put("customer", customer);
                // 注意此处设置值，区分客户端跟经纪人端
                userInfo.put("currentIdentity", "4");
                userInfo.put("bussiness", bussiness);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return userInfo.toString();
        }

        // 打印webview log
        @JavascriptInterface
        public void getLog(String msg) {
            Log.d(MESSAGE_TAG, "getLog: " + msg);
        }

        // 挂断
        @JavascriptInterface
        public void hangup() {
            Log.d(MESSAGE_TAG, "hangup() ");
            // 退出语音
            leaveChannel();
            // 更新界面
            callUpdateChatStatus("8", null);
            // 通知对方
            JSONObject hangupMsg = new JSONObject();
            try {
                hangupMsg.put("type", "mini-hangup");
                hangupMsg.put("hangupType", 8);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            RtmMessage message = rtmClient.createMessage();
            message.setText(hangupMsg.toString());
            sendPeerMessage(message);
            // 主动挂断，关闭当前activity
        }

        @JavascriptInterface
        public void call() {
            Log.d(MESSAGE_TAG,">>>>>>呼叫call() ");
            Log.d(MESSAGE_TAG,"call<<<Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO");

            JSONObject callData = new JSONObject();
            try {
                callData.put("type", "app-call");
                callData.put("roomid", Integer.parseInt(getUserId()));
                callData.put("houseid", modelID);
                callData.put("houseurl", modelURL);
//                SharedPreferences pref = getSharedPreferences("data",MODE_PRIVATE);
//                callData.put("channelName", pref.getString("channelName", ""));
                callData.put("channelName", getUserId());

                // 传送额外字段
                // bussiness
                callData.put("bussinessHeadImage", bussinessHeadImage);
                callData.put("bussinessIdentity", bussinessIdentity);
                callData.put("bussinessNickname", bussinessNickname);
                callData.put("bussinessUid", bussinessUid);
                callData.put("bussinessAccid", bussinessAccid);

                // customer->从getUserId()方法获取
                callData.put("customerHeadImage", customerHeadImage);
                callData.put("customerIdentity", customerIdentity);
                callData.put("customerNickname", customerNickname);
                callData.put("customerUid", customerUid);
                callData.put("customerAccid", customerAccid);

                RtmMessage message = rtmClient.createMessage();
                message.setText(callData.toString());
                sendPeerMessage(message);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @JavascriptInterface
        public void initMessageAction(String msg) {
            try {
                JSONObject obj = new JSONObject(msg);
                String userId = obj.optString("userId");
                if (userId.isEmpty()) {
                    userId = getUserId();
                }
                mUserId = userId;
                String[] perms = {Manifest.permission.RECORD_AUDIO};
                if (EasyPermissions.hasPermissions(getContext(), perms)) {
                    initAgoraEngineAndJoinChannel();
                    joinChannel();
                } else {
                    EasyPermissions.requestPermissions(getActivity(),"请求语音权限进行通话",PERMISSION_REQ_ID_RECORD_AUDIO, perms);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        @JavascriptInterface
        public void testJs() {
            Toast.makeText(getContext() , "testJS", Toast.LENGTH_SHORT).show();
        }

        @JavascriptInterface
        public void jumpToWebView(String url) {
            Intent intent = new Intent(Intent.ACTION_VIEW,Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setPackage("com.android.chrome");
            try {
                getContext().startActivity(intent);
            } catch (ActivityNotFoundException ex) {
                // Chrome browser presumably not installed so allow user to choose instead
                intent.setPackage(null);
                getContext().startActivity(intent);
                Toast.makeText(getContext() , "Need Chrome to experience AR feature", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String getUserId() {
        return mUserId;
    }

    private void initAgoraEngineAndJoinChannel() {
        try {
            rtcEngine = RtcEngine.create(getContext(), getString(R.string.agora_app_id), rtcEngineEventHandler);
            rtmClient.login(null, mUserId, new ResultCallback<Void>() {
                @Override
                public void onSuccess(Void unused) {
                    HashMap map = new HashMap();
                    map.put("type", "initMessageActionSuccess");
                    map.put("userId", mUserId);
                    callOnData(map.toString());
                }

                @Override
                public void onFailure(ErrorInfo errorInfo) {
                    HashMap map = new HashMap();
                    map.put("type", "initMessageActionFail");
                    map.put("errorInfo", errorInfo.getErrorDescription());
                    map.put("userId", mUserId);

                    callOnData(map.toString());
                }
            });
        } catch (Exception e) {
            Log.e(MESSAGE_TAG, Log.getStackTraceString(e));
            HashMap map = new HashMap();
            map.put("type", "initMessageActionFail");
            map.put("errorInfo", Log.getStackTraceString(e));
            map.put("userId", mUserId);

            callOnData(map.toString());
            throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
        }
    }

    private void sendPeerMessage(final RtmMessage message) {
        Log.d(MESSAGE_TAG, "sendPeerMessage >>> userId = " + mPeerId);
        rtmClient.sendMessageToPeer(mPeerId, message, chatManager.getSendMessageOptions(), new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Log.d(MESSAGE_TAG, "sendPeerMessage >>> success == " + message.getText());
            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                final String errDes = errorInfo.getErrorDescription();
               Log.d(MESSAGE_TAG, "seedPeerMessage >>> fail == " + errDes);
            }
        });
    }

    private String getToken(final String fromId, final String channelName) {
        final String[] result = {""};
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    OkHttpClient client = new OkHttpClient();
                    Map<String, Object> bodyMap = new HashMap<>();

                    bodyMap.put("channeName", channelName);
                    bodyMap.put("uid", fromId);
                    bodyMap.put("role",1);

                    String jsonParams = new JSONObject(bodyMap).toString();

                    RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8")
                            , jsonParams);

                    Request request = new Request.Builder()
                            .url("https://pm.shcobol.com/agora/fetch_rtc_token")
                            .post(body)//传递请求体
                            .build();
                    Response response = null;
                    response = client.newCall(request).execute();//得到Response 对象
                    if (response.isSuccessful()) {
                        Log.d(MESSAGE_TAG,"获取数据成功了");
                        Log.d(MESSAGE_TAG,"response.code()=="+response.code());
                        String rt = response.body().string();
                        Log.d(MESSAGE_TAG,"response.body().string()==" + rt);
                        result[0] =rt;
                        JSONObject jsonObject = new JSONObject(rt);
                        if (jsonObject.has("token")){
                            result[0]=jsonObject.getString("token");
                        }else{
                            result[0]="-";
                        }

                    }
                } catch (Exception e) {

                }
            }
        }).start();
        try {
            int waitCount = 0;
            while (result[0].equals("")) {
                waitCount++;
                Thread.sleep(100);
                if (waitCount > 10) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return  result[0];
    }

    private String createChannel(String mUserId) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateStr = format.format(new Date()) + mUserId;
        final String slat = "xxxxx";
        try {
            dateStr = dateStr + slat;
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(dateStr.getBytes(StandardCharsets.UTF_8));
            byte s[] = m.digest();
            String result = "";
            for (int i = 0; i < s.length; i++) {
                result += Integer.toHexString((0x000000FF & s[i]) | 0xFFFFFF00).substring(6);
            }
            SharedPreferences.Editor editor = getActivity().getSharedPreferences("data", Context.MODE_PRIVATE).edit();
            editor.putString("channelName", result);
            editor.commit();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    private void joinChannel() {
        String channel = createChannel(mUserId);

        channelName = channel;
        String accessToken = getToken(mUserId, channelName);
        rtcEngine.setLogFilter(0x080f);
        String ts = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String filePath = "/sdcard/" + ts + ".log";
        rtcEngine.setLogFile(filePath);
        rtcEngine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
        rtcEngine.setAudioProfile(Constants.AUDIO_PROFILE_MUSIC_HIGH_QUALITY, Constants.AUDIO_SCENARIO_GAME_STREAMING);
        rtcEngine.setDefaultAudioRoutetoSpeakerphone(true);

        rtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION);

        rtcEngine.joinChannel(accessToken, channelName, "", Integer.parseInt(mUserId));
        Log.d(MESSAGE_TAG, "joinChannel >>> " + mUserId);
    }

    private void leaveChannel() {
        if (rtcEngine != null) {
            rtcEngine.leaveChannel();
        }
    }

    private void onRemoteUserLeft(int uid, int reason) {

    }

    private void onRemoteUserVoiceMuted(int uid, boolean muted) {
    }

}


