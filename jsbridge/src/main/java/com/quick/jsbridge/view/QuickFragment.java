package com.quick.jsbridge.view;

import android.Manifest;
import android.app.SharedElementCallback;
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

import androidx.annotation.NonNull;

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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
public class QuickFragment extends FrmBaseFragment implements IQuickFragment, EasyPermissions.PermissionCallbacks {

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
    private String mPeerId = "";
    private String mUserId = "";

    private String channelName = "";

    private String businessType = "";

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
            Log.i("joinChannel", channel);
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            super.onUserOffline(uid, reason);
            Log.i("userOffline", uid + " " + reason);
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            super.onUserJoined(uid, elapsed);
            Log.i("joined",uid + "");
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
        // 初始化一对一聊天
        initChat();
        // 初始化控件
        initView();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.i("test","fff");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * 初始化布局控件
     */
    protected void initView() {
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
            loginRtm();
//            initAgoraEngineAndJoinChannel();
//            joinChannel();
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
        logoutAndLeaveChannel();
        updateUserStatus(100);
        rtmClient.release();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i("onStop", "fff");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        Log.i("requestCode", requestCode + "");
        if (requestCode == PERMISSION_REQ_ID_RECORD_AUDIO) {
            leaveChannel();
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {

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
                case RtmStatusCode.ConnectionState.CONNECTION_STATE_CONNECTED:
                    Log.i(MESSAGE_TAG, getString(R.string.connected));
                    break;
                case RtmStatusCode.ConnectionState.CONNECTION_STATE_DISCONNECTED:
                    Log.i(MESSAGE_TAG, getString(R.string.disconnect));
                    break;
                case RtmStatusCode.ConnectionState.CONNECTION_STATE_CONNECTING:
                    Log.i(MESSAGE_TAG, "连接中");
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
                        if (!businessType.equals("bussiness")) {
                            callUpdateChatStatus("3", null);
                        }
                    }
                } else if (jsonObject.has("type")
                        && jsonObject.getString("type").equals("mini-hangup")) {    // 小程序消息
                    // 临时通过消息接收主叫方挂断电话
                    // 对方挂断，更新界面
                    callUpdateChatStatus("8", null);
                    logoutAndLeaveChannel();
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
                        callUpdateChatStatus("5", null);
                    }
                } else if (jsonObject.has("type")
                        && jsonObject.getString("type").equals("app-call")) {
                    // 获得呼叫之后先登录
                    loginRtc();
                    String houseUrl = jsonObject.getString("houseurl");

                    HashMap map = new HashMap();
                    map.put("URL", houseUrl);
                    callOnData(new JSONObject(map).toString());

                    String businessId = jsonObject.getString("bussinessUid");
                    if (!jsonObject.getString("bussinessHeadImage").isEmpty()) {
                        bussinessHeadImage = jsonObject.getString("bussinessHeadImage");
                    }
                    if (!jsonObject.getString("bussinessIdentity").isEmpty()) {
                        bussinessIdentity = jsonObject.getString("bussinessIdentity");
                    }
                    if (!jsonObject.getString("bussinessNickName").isEmpty()) {
                        bussinessNickname = jsonObject.getString("bussinessNickName");
                    }
                    if (!jsonObject.getString("bussinessUid").isEmpty()) {
                        bussinessUid = jsonObject.getString("bussinessUid");
                    }
                    if (!jsonObject.getString("bussinessAccid").isEmpty()) {
                        bussinessAccid = jsonObject.getString("bussinessAccid");
                    }
                    if (!jsonObject.getString("customerHeadImage").isEmpty()) {
                        customerHeadImage = jsonObject.getString("customerHeadImage");
                    }
                    if (!jsonObject.getString("customerIdentity").isEmpty()) {
                        customerIdentity = jsonObject.getString("customerIdentity");
                    }

                    if (!jsonObject.getString("customerUid").equals("0")) {
                        customerUid = jsonObject.getString("customerUid");
                    }
                    if (!jsonObject.getString("customerAccid").isEmpty()) {
                        customerAccid = jsonObject.getString("customerAccid");
                    }

                    if (!jsonObject.getString("customerNickName").isEmpty()) {
                        customerNickname = jsonObject.getString("customerNickName");
                    }
                    bussinessUid = businessId;
                    updateBUid(rtmMessage.getText());
                } else if (jsonObject.has("type")
                        && jsonObject.getString("type").equals("accept")) {
                    String businessId = jsonObject.getString("bussinessUid");
                    loginRtc();
                    Log.i("receive accept", "ddd");
                    joinChannelWithUid(mUserId, businessId);
                    updateBUid(rtmMessage.getText());
                    callUpdateChatStatus("3", null);
                } else {
                    if (!peerId.isEmpty() && !mPeerId.equals(peerId)) {
                        Log.d(MESSAGE_TAG,"=========================接收消息处理-aaaa");
                        Log.d(MESSAGE_TAG,rtmMessage.getText());
                    } else {
                        Log.d(MESSAGE_TAG,"=========================接收消息处理-bbbb");
                        Log.d(MESSAGE_TAG,rtmMessage.getText());
                        callOnData(rtmMessage.getText());
                    }
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
    public class JavaScriptInterface implements EasyPermissions.PermissionCallbacks {
        // 传输数据
        @JavascriptInterface
        public void sendData(String data) {
            Log.i(MESSAGE_TAG, data.toString());
            RtmMessage message = rtmClient.createMessage();

            message.setText(data);
            sendPeerMessage(message);
        }

        @JavascriptInterface
        public void sendUserInfo(String data) {
            Log.i(MESSAGE_TAG,"sendUserInfo" + data);
            try {
                JSONObject obj = new JSONObject(data);
                if (obj.has("type")) {
                    businessType = obj.getString("type");
                }
                if (obj.has("bussiness")) {
                    JSONObject business = obj.getJSONObject("bussiness");
                    if (!business.getString("bussinessHeadImage").isEmpty()) {
                        bussinessHeadImage = business.getString("bussinessHeadImage");
                    }
                    if (!business.getString("bussinessIdentity").isEmpty()) {
                        bussinessIdentity = business.getString("bussinessIdentity");
                    }
                    if (!business.getString("bussinessNickName").isEmpty()) {
                        bussinessNickname = business.getString("bussinessNickName");
                    }
                    if (!business.getString("bussinessUid").isEmpty()) {
                        bussinessUid = business.getString("bussinessUid");
                    }
                    if (!business.getString("bussinessAccid").isEmpty()) {
                        bussinessAccid = business.getString("bussinessAccid");
                    }

                    if (businessType.equals("bussiness")) {
                        if (!bussinessUid.isEmpty()) {
                            mUserId = bussinessUid;
                        }
                    } else {
                        if (!bussinessUid.isEmpty()) {
                            mPeerId = bussinessUid;
                        }
                    }
                }

                if (obj.has("modelUrl")) {
                    modelURL = obj.getString("modelUrl");
                }

                if (obj.has("customer")) {
                    JSONObject customer = obj.getJSONObject("customer");

                    if (!customer.getString("customerHeadImage").isEmpty()) {
                        customerHeadImage = customer.getString("customerHeadImage");
                    }
                    if (!customer.getString("customerIdentity").isEmpty()) {
                        customerIdentity = customer.getString("customerIdentity");
                    }

                    if (!customer.getString("customerUid").equals("0")) {
                        customerUid = customer.getString("customerUid");
                    }
                    if (!customer.getString("customerAccid").isEmpty()) {
                        customerAccid = customer.getString("customerAccid");
                    }

                    if (!businessType.equals("bussiness")) {
                        if (!customerUid.isEmpty()) {
                            mUserId = customerUid;
                        }
                    } else {
                        if (!customerUid.isEmpty()) {
                            mPeerId = customerUid;
                        }
                    }

                    if (!customer.getString("customerNickName").isEmpty()) {
                        customerNickname = customer.getString("customerNickName");
                    }


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
                customer.put("customerNickName", customerNickname);//"小A"
                customer.put("customerUid", customerUid);//"1"
                customer.put("customerAccid", customerAccid);//"15261805000"
            } catch (JSONException e) {
                e.printStackTrace();
            }

            JSONObject bussiness = new JSONObject();
            try {
                bussiness.put("bussinessHeadImage", bussinessHeadImage);//"./images/default_avator.png"
                bussiness.put("bussinessIdentity", bussinessIdentity);//"3"
                bussiness.put("bussinessNickName", bussinessNickname);//"小B"
                bussiness.put("bussinessUid", bussinessUid);//"2"
                bussiness.put("bussinessAccid", bussinessAccid);//"15261805001"
            } catch (JSONException e) {
                e.printStackTrace();
            }

            JSONObject userInfo = new JSONObject();
            try {
                userInfo.put("customer", customer);
                // 注意此处设置值，区分客户端跟经纪人端
                if (businessType.equals("bussiness")) {
                    userInfo.put("currentIdentity", bussinessIdentity);
                } else {
                    userInfo.put("currentIdentity", customerIdentity);
                }
                userInfo.put("bussiness", bussiness);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Log.i("getUserInfo", userInfo.toString());

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
            rtcEngine = null;
            // 更新界面
            callUpdateChatStatus("7", null);
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
                callData.put("roomid", Long.parseLong(getUserId()));
                callData.put("houseid", modelID);
                callData.put("houseurl", modelURL);
                callData.put("channelName", getUserId());

                // 传送额外字段
                // bussiness
                callData.put("bussinessHeadImage", bussinessHeadImage);
                callData.put("bussinessIdentity", bussinessIdentity);
                callData.put("bussinessNickName", bussinessNickname);
                callData.put("bussinessUid", bussinessUid);
                callData.put("bussinessAccid", bussinessAccid);

                // customer->从getUserId()方法获取
                callData.put("customerHeadImage", customerHeadImage);
                callData.put("customerIdentity", customerIdentity);
                callData.put("customerNickName", customerNickname);
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
                String[] perms = {Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};
                if (EasyPermissions.hasPermissions(getContext(), perms)) {
                    loginRtm();
                } else {
                    EasyPermissions.requestPermissions(getActivity(),"请求语音权限进行通话",PERMISSION_REQ_ID_RECORD_AUDIO, perms);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @JavascriptInterface
        public void mute() {
            Log.i("mute", "fff");
            rtcEngine.muteLocalAudioStream(true);
            // 对对方静音
        }

        @JavascriptInterface
        public void changeUserMuteInfo(String str) {
            Log.i("changeUserMute", str);
        }

        @JavascriptInterface
        public void changeBrokerMuteInfo(String str) {
            Log.i("changeBrokerMute", str);
        }

        @JavascriptInterface
        public void unmute() {
            Log.i("unMute", "");
            rtcEngine.muteLocalAudioStream(false);
        }

        @JavascriptInterface
        public void testJs() {
            Toast.makeText(getContext() , "testJS", Toast.LENGTH_SHORT).show();
        }

        @JavascriptInterface
        public void accept() {
            // 调用推送音频
            joinChannelWithUid(mUserId, bussinessUid);
            callUpdateChatStatus("3", null);
            Log.d(MESSAGE_TAG,"accept---真正接听");
            isAccept();
        }

        @JavascriptInterface
        public void joinChannelFromJs(String data) {
            try {
                JSONObject obj = new JSONObject(data);
                String userId = obj.optString("userId");
                String channelName = obj.optString("channelName");
                joinChannelWithUid(userId, channelName);
            } catch (Exception e) {
                e.printStackTrace();
            }
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

        @JavascriptInterface
        public void logout(String data) {
            Log.i("test logout from web", data);
            rtmClient.logout(new ResultCallback<Void>() {
                @Override
                public void onSuccess(Void unused) {
                    updateUserStatus(100);
                }

                @Override
                public void onFailure(ErrorInfo errorInfo) {

                }
            });
            if (rtcEngine != null) {
                rtcEngine.leaveChannel();
            }
            rtcEngine = null;
            rtmClient.release();
        }

        @JavascriptInterface
        public void isRealOnline(String data) {
            try {
                JSONObject obj = new JSONObject(data);
                final String bUid = obj.optString("bussinessUid");
                Set set = new HashSet();
                set.add(bUid);
                rtmClient.queryPeersOnlineStatus(set, new ResultCallback<Map<String, Boolean>>() {
                    @Override
                    public void onSuccess(Map<String, Boolean> stringBooleanMap) {

                        getUserChannelStatus(bUid);
                    }
                    @Override
                    public void onFailure(ErrorInfo errorInfo) {
                        updateUserStatusWithId(100, bUid);
                    }
                });
            } catch (Exception e) {

            }
        }

        private void sendAcceptStatus() {

        }

        @Override
        public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
            Log.i("onPermissionsGranted", "fff");
        }

        @Override
        public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
            Log.i("onPermissionsDenied", "fff");
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        }
    }

    private String getUserId() {
        return mUserId;
    }

    private void updateBUid(String data) {
        Log.i("updateBUid", data);
        Log.i("updateType", businessType);
        try {
            JSONObject obj = new JSONObject(data);

            if (obj.has("customerUid")) {
                String cid = obj.getString("customerUid");
                if (!cid.isEmpty()) {
                    if (businessType.equals("bussiness")) {
                        mPeerId = cid;
                    } else {
                        mUserId = cid;
                    }
                }
            }

            if (obj.has("bussinessUid")) {
                String bid = obj.getString("bussinessUid");
                if (!bid.isEmpty()) {
                    if (businessType.equals("bussiness")) {
                        mUserId = bid;
                    } else {
                        mPeerId = bid;
                    }
                }
            }

            Log.i("update uid", mUserId);
            Log.i("update pid", mPeerId);
//            joinChannelWithUid(mUserId, bussinessUid);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initAgoraEngineAndJoinChannel() {
        loginRtm();
    }

    /**
     * 登录语音
     */
    private void loginRtc() {
        try {
            rtcEngine = RtcEngine.create(getContext(), getString(R.string.agora_app_id), rtcEngineEventHandler);
            rtcEngine.setLogFilter(0x0f);
            String ts = new SimpleDateFormat("yyyyMMdd").format(new Date());
            String filePath = "/sdcard/" + ts + "/agorartm.log";
            rtcEngine.setLogFile(filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 登录通信
     */
    private void loginRtm() {
        rtmClient.setLogFilter(0x0f);
        String ts = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String filePath = "/sdcard/" + ts + "/agorartm.log";
        rtmClient.setLogFile(filePath);

        SharedPreferences sp = getActivity().getSharedPreferences("rtmToken", Context.MODE_PRIVATE);
        final String token = sp.getString("token", "");
        Long getTime = sp.getLong("tokenTime",0);

        if (!token.isEmpty()) {
            Long currentTime = new Date().getTime();

            if ((currentTime - getTime)/ 1000 / 60 <= 60) {
                // token 还在有效期内
//                String loginToken = getMessageToken(mUserId);
//                    Log.i("login Token", loginToken);
//                    Log.i("login userId", mUserId);
                    rtmClient.login(token, mUserId, new ResultCallback<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            HashMap map = new HashMap();
                            map.put("type", "initMessageActionSuccess");
                            map.put("userId", mUserId);
                            callOnData(new JSONObject(map).toString());
                            Log.i("init", "login success");
                            updateUserStatus(101);
                        }

                        @Override
                        public void onFailure(ErrorInfo errorInfo) {
                            HashMap map = new HashMap();
                            map.put("type", "initMessageActionFail");
                            map.put("errorInfo", errorInfo.getErrorDescription());
                            map.put("userId", mUserId);
                            callOnData(new JSONObject(map).toString());
                            Log.i("init", "login fail");
//                            updateUserStatus(100);

//                            if (errorInfo.getErrorCode() == 4) {
                                loginWithToken();
//                            }
                        }
                    });
            } else {
                // token不在有效期内
                loginWithToken();
            }
        } else {
            loginWithToken();
        }

//        try {
//            rtmClient.logout(new ResultCallback<Void>() {
//                @Override
//                public void onSuccess(Void unused) {
//                    Log.i("logout", "success");
//                    final String loginToken = getMessageToken(mUserId);
//                    Log.i("login Token", loginToken);
//                    Log.i("login userId", mUserId);
//
//                    Handler handler = new Handler();
//                    handler.postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            rtmClient.login(loginToken, mUserId, new ResultCallback<Void>() {
//                                @Override
//                                public void onSuccess(Void unused) {
//                                    HashMap map = new HashMap();
//                                    map.put("type", "initMessageActionSuccess");
//                                    map.put("userId", mUserId);
//                                    callOnData(new JSONObject(map).toString());
//                                    Log.i("init", "login success");
//                                    updateUserStatus(101);
//                                }
//
//                                @Override
//                                public void onFailure(ErrorInfo errorInfo) {
//                                    HashMap map = new HashMap();
//                                    map.put("type", "initMessageActionFail");
//                                    map.put("errorInfo", errorInfo.getErrorDescription());
//                                    map.put("userId", mUserId);
//
//                                    callOnData(new JSONObject(map).toString());
//                                    Log.i("init", "login fail");
//                                    updateUserStatus(100);
//                                }
//                            });
//                        }
//                    }, 4000);
//
//
//                }
//
//                @Override
//                public void onFailure(ErrorInfo errorInfo) {
//                    Log.i("logout", "fail");
//                    String loginToken = getMessageToken(mUserId);
//                    Log.i("login Token", loginToken);
//                    Log.i("login userId", mUserId);
//                    rtmClient.login(loginToken, mUserId, new ResultCallback<Void>() {
//                        @Override
//                        public void onSuccess(Void unused) {
//                            HashMap map = new HashMap();
//                            map.put("type", "initMessageActionSuccess");
//                            map.put("userId", mUserId);
//                            callOnData(new JSONObject(map).toString());
//                            Log.i("init", "login success");
//                            updateUserStatus(101);
//                        }
//
//                        @Override
//                        public void onFailure(ErrorInfo errorInfo) {
//                            HashMap map = new HashMap();
//                            map.put("type", "initMessageActionFail");
//                            map.put("errorInfo", errorInfo.getErrorDescription());
//                            map.put("userId", mUserId);
//
//                            callOnData(new JSONObject(map).toString());
//                            Log.i("init", "login fail");
//                            updateUserStatus(100);
//                        }
//                    });
//                }
//            });
//
//        } catch (Exception e) {
//            Log.e(MESSAGE_TAG, Log.getStackTraceString(e));
//            HashMap map = new HashMap();
//            map.put("type", "initMessageActionFail");
//            map.put("errorInfo", Log.getStackTraceString(e));
//            map.put("userId", mUserId);
//
//            callOnData(new JSONObject(map).toString());
//            throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
//        }
    }

    private void loginWithToken() {
        final String loginToken = getMessageToken(mUserId);
        Log.i("login Token", loginToken);
        Log.i("login userId", mUserId);
        rtmClient.login(loginToken, mUserId, new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void unused) {
                HashMap map = new HashMap();
                map.put("type", "initMessageActionSuccess");
                map.put("userId", mUserId);
                callOnData(new JSONObject(map).toString());
                Log.i("init", "login success");
                updateUserStatus(101);
                saveToken(loginToken);
            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                HashMap map = new HashMap();
                map.put("type", "initMessageActionFail");
                map.put("errorInfo", errorInfo.getErrorDescription());
                map.put("userId", mUserId);

                callOnData(new JSONObject(map).toString());
                Log.i("init", "login fail");
//                updateUserStatus(100);
            }
        });
    }

    private void saveToken(String token) {
        SharedPreferences.Editor edit = getActivity().getSharedPreferences("rtmToken", Context.MODE_PRIVATE).edit();
        edit.putString("token", token);
        edit.putLong("tokenTime", new Date().getTime());
        edit.commit();
    }

    private void sendPeerMessage(final RtmMessage message) {
        Log.d(MESSAGE_TAG, "sendPeerMessage >>> peerId = " + mPeerId);

        rtmClient.sendMessageToPeer(mPeerId, message, chatManager.getSendMessageOptions(), new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void unused) {
                Log.d(MESSAGE_TAG, "sendPeerMessage >>> success == " + message.getText());
            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                final String errDes = errorInfo.getErrorDescription();
               Log.d(MESSAGE_TAG, "seedPeerMessage >>> fail == " + errDes);
               Toast.makeText(getContext(), "对方未登录！", Toast.LENGTH_SHORT).show();
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
                    Log.i("get Token Channel Name", channelName);
                    Log.i("get Token uid", fromId);

                    bodyMap.put("channelName", "channel" + channelName);
                    bodyMap.put("uid", fromId);
                    bodyMap.put("role",1);

                    String jsonParams = new JSONObject(bodyMap).toString();

                    RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8")
                            , jsonParams);

                    Request request = new Request.Builder()
                            .url("https://console.mspaco.com.sg/prod-api/mall-daogou/vragora/token")
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
                        if (jsonObject.has("data")){
                            result[0]=jsonObject.getString("data");
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

    private void getUserChannelStatus(final String bid) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    OkHttpClient client = new OkHttpClient();
                    Map<String, Object> bodyMap = new HashMap<>();

                    String jsonParams = new JSONObject(bodyMap).toString();

                    String url = "https://api.agora.io/dev/v1/channel/user/property/" +getString(R.string.agora_app_id) +  "/"+ bid +"/channel" +bid;

                    RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8")
                            , jsonParams);

                    Request request = new Request.Builder()
                            .url("url")
                            .get()
                            .build();
                    Response response = null;
                    response = client.newCall(request).execute();
                    Log.i("response", response.toString());
                    if (response.isSuccessful()) {
                        String rt = response.body().string();
                        Log.d(MESSAGE_TAG,"response.body().string()==" + rt);
                        JSONObject jsonObject = new JSONObject(rt);
                        if (jsonObject.has("data")){
                            JSONObject obj = jsonObject.getJSONObject("data");
                            Boolean isChannel = obj.getBoolean("in_channel");
                            if (isChannel) {
                                updateUserStatusWithId(102, bid);
                            } else {
                                updateUserStatusWithId(101, bid);
                            }
                        }
                    }
                } catch (Exception e) {

                }
            }
        }).start();


    }

    private String getMessageToken(final String uid) {
        final String[] result = {""};
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    OkHttpClient client = new OkHttpClient();
                    Map<String, Object> bodyMap = new HashMap<>();
                    Log.i("get message Token uid", uid);

                    bodyMap.put("userId", uid);
                    String jsonParams = new JSONObject(bodyMap).toString();

                    RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8")
                            , jsonParams);

                    Request request = new Request.Builder()
                            .url("https://console.mspaco.com.sg/prod-api/mall-daogou/vragora/rtm-token")
                            .post(body)//传递请求体
                            .build();
                    Response response = null;
                    response = client.newCall(request).execute();//得到Response 对象
                    Log.i("response", response.toString());
                    if (response.isSuccessful()) {
                        Log.d(MESSAGE_TAG,"获取数据成功了");
                        Log.d(MESSAGE_TAG,"response.code()=="+response.code());
                        String rt = response.body().string();
                        Log.d(MESSAGE_TAG,"response.body().string()==" + rt);
                        result[0] =rt;
                        JSONObject jsonObject = new JSONObject(rt);
                        if (jsonObject.has("data")){
                            result[0]=jsonObject.getString("data");
                        }else{
                            result[0]="-";
                        }
                    }
                } catch (Exception e) {
                    Log.i("getMessageToken", "error");
                    e.printStackTrace();
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
        String channel = mUserId;

        channelName = channel;
        rtcEngine.setLogFilter(0x0f);
        String ts = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String filePath = "/sdcard/" + ts + "/agorartc.log";
        rtcEngine.setLogFile(filePath);
        rtcEngine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
        rtcEngine.setAudioProfile(Constants.AUDIO_PROFILE_MUSIC_HIGH_QUALITY, Constants.AUDIO_SCENARIO_GAME_STREAMING);
        rtcEngine.setDefaultAudioRoutetoSpeakerphone(true);
        rtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION);

        if (businessType.equals("bussiness")) {
            joinChannelWithUid(mUserId, channelName);
        }
    }

    private void isAccept() {
        HashMap map = new HashMap();
        map.put("type", "accept");
        map.put("bussinessUid", bussinessUid);
        map.put("customerUid", customerUid);
        RtmMessage message = rtmClient.createMessage();

        message.setText(new JSONObject(map).toString());
        sendPeerMessage(message);
    }

    private void joinChannelWithUid(String uid, String channelName) {

        String accessToken = getToken(uid, channelName);
        rtcEngine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
        rtcEngine.setAudioProfile(Constants.AUDIO_PROFILE_MUSIC_HIGH_QUALITY, Constants.AUDIO_SCENARIO_GAME_STREAMING);
        rtcEngine.setDefaultAudioRoutetoSpeakerphone(true);

        rtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION);
        Log.i("joinChannel channelName", channelName + "");
        Log.i("joinChannel accessToken", accessToken);
        Log.i("joinChannel uid", uid);
        updateUserStatus(102);

        rtcEngine.joinChannel(accessToken, "channel" + channelName, "", Integer.parseInt(uid));
    }

    private void updateUserStatusWithId(final Integer status, final String uid) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    OkHttpClient client = new OkHttpClient();
                    Map<String, Object> bodyMap = new HashMap<>();

                    bodyMap.put("status", status);
                    bodyMap.put("uid", uid);

                    String jsonParams = new JSONObject(bodyMap).toString();

                    RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8")
                            , jsonParams);

                    Request request = new Request.Builder()
                            .url("https://console.mspaco.com.sg/prod-api/mall-daogou/customer_status/status")
                            .post(body)//传递请求体
                            .build();
                    Response response = null;
                    response = client.newCall(request).execute();//得到Response 对象
                    if (response.isSuccessful()) {
                        Log.d(MESSAGE_TAG,uid + "用户状态更新成功了" + status);

                    }
                } catch (Exception e) {

                }
            }
        }).start();
    }

    private void updateUserStatus(final Integer status) {
        updateUserStatusWithId(status, mUserId);
    }

    private void leaveChannel() {
        updateUserStatus(101);
        if (rtcEngine != null) {
            rtcEngine.leaveChannel();
        }
    }

    private void logoutAndLeaveChannel() {
//        leaveChannel();
        if (rtcEngine != null) {
            rtcEngine.leaveChannel();
        }
        rtcEngine = null;
        RtcEngine.destroy();
    }

    private void onRemoteUserLeft(int uid, int reason) {

    }

    private void onRemoteUserVoiceMuted(int uid, boolean muted) {
    }

}


