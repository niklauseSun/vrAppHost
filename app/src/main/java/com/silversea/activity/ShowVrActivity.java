package com.silversea.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.silversea.model.MessageBean;
import com.silversea.rtmtutorial.AGApplication;
import com.silversea.rtmtutorial.ChatManager;
import com.vapp.android.R;
import com.silversea.utils.MessageUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
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


public class ShowVrActivity extends Activity {

    private static final String LOG_TAG = ShowVrActivity.class.getSimpleName();

    private String modelID = "7051c064_o0fM_b6f9";
    private String modelURL = "https://beyond.3dnest.biz/silversea_dev/takelook/?m="+modelID;
    private WebView mWebView;


    private String mPeerId = "";
    private String mUserId = "";


    private RtmClient mRtmClient;
    private ChatManager mChatManager;
    private RtmClientListener mClientListener;

    private Handler mHandler;

    // voice
    private RtcEngine mRtcEngine; // Tutorial Step 1
    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 22;
    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() { // Tutorial Step 1

        /**
         * Occurs when a remote user (Communication)/host (Live Broadcast) leaves the channel.
         *
         * There are two reasons for users to become offline:
         *
         *     Leave the channel: When the user/host leaves the channel, the user/host sends a goodbye message. When this message is received, the SDK determines that the user/host leaves the channel.
         *     Drop offline: When no data packet of the user or host is received for a certain period of time (20 seconds for the communication profile, and more for the live broadcast profile), the SDK assumes that the user/host drops offline. A poor network connection may lead to false detections, so we recommend using the Agora RTM SDK for reliable offline detection.
         *
         * @param uid ID of the user or host who
         * leaves
         * the channel or goes offline.
         * @param reason Reason why the user goes offline:
         *
         *     USER_OFFLINE_QUIT(0): The user left the current channel.
         *     USER_OFFLINE_DROPPED(1): The SDK timed out and the user dropped offline because no data packet was received within a certain period of time. If a user quits the call and the message is not passed to the SDK (due to an unreliable channel), the SDK assumes the user dropped offline.
         *     USER_OFFLINE_BECOME_AUDIENCE(2): (Live broadcast only.) The client role switched from the host to the audience.
         */
        @Override
        public void onUserOffline(final int uid, final int reason) { // Tutorial Step 4
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onRemoteUserLeft(uid, reason);
                }
            });
        }

        /**
         * Occurs when a remote user stops/resumes sending the audio stream.
         * The SDK triggers this callback when the remote user stops or resumes sending the audio stream by calling the muteLocalAudioStream method.
         *
         * @param uid ID of the remote user.
         * @param muted Whether the remote user's audio stream is muted/unmuted:
         *
         *     true: Muted.
         *     false: Unmuted.
         */
        @Override
        public void onUserMuteAudio(final int uid, final boolean muted) { // Tutorial Step 6
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onRemoteUserVoiceMuted(uid, muted);
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_vr);

        mHandler = new Handler();

        mChatManager = AGApplication.the().getChatManager();
        mRtmClient = mChatManager.getRtmClient();
        mClientListener = new MyRtmClientListener();
        mChatManager.registerListener(mClientListener);

        // 获取双方id
        mPeerId=getmTargetName();
        mUserId=getUserId();

        Log.d(LOG_TAG,"onCreate-----mPeerId:"+mPeerId+">>>mUserId:"+mUserId);

        mWebView = (WebView) findViewById(R.id.webview);

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO)) {
            initAgoraEngineAndJoinChannel();
        }
        // 加载网页
        showWebView();
    }

    // 加载WebView VR页面
    private void showWebView() {
        // 隐藏键盘 todo:
        // https://www.it1352.com/1918487.html
        // ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(ShowVrActivity.this.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);


        // js interface, 用于js调用native
        mWebView.addJavascriptInterface(new JavaScriptInterface(), "WebBridge");
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        // 用于适配webview界面
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setTextZoom(100);

        mWebView.loadUrl(modelURL + "&showtakelook=on");
        Log.i(LOG_TAG,modelURL + "&showtakelook=on");
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                Log.i(LOG_TAG,modelURL + "&showtakelook=on2222222");
                return true;
            }
        });
    }

    //用于js调用native
    public class JavaScriptInterface {
        // 传输数据
        @JavascriptInterface
        public void sendData(String data) {
//            mITRTCAudioCall.sendIMMsg(data);
            Log.d(LOG_TAG,"sendData(String data)0 "+data);
            RtmMessage message = mRtmClient.createMessage();
            message.setText(data);
            sendPeerMessage(message);
            Log.d(LOG_TAG,"sendData(String data)1 "+data);
        }

        // 获取用户信息
        @JavascriptInterface
        public String getUserInfo() {
            Log.d(LOG_TAG,"getUserInfo() ");
            // 测试用信息，生产环境下请使用真实信息
            JSONObject customer = new JSONObject();
            try {
                customer.put("customerHeadImage", "./images/default_avator.png");
                customer.put("customerIdentity", "4");
                customer.put("customerNickname", "小A");
                customer.put("customerAccid", "15261805000");
            } catch (JSONException e) {
                e.printStackTrace();
            }


            JSONObject bussiness = new JSONObject();
            try {
                bussiness.put("bussinessNickname", "小B");
                bussiness.put("bussinessHeadImage", "./images/default_avator.png");
                bussiness.put("bussinessIdentity", "3");
                bussiness.put("bussinessAccid", "15261805001");
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
            Log.d(LOG_TAG, "getLog(String msg):" + msg + " ");
        }

        // 挂断
        @JavascriptInterface
        public void hangup() {
            Log.d(LOG_TAG,"hangup() ");
//            mITRTCAudioCall.hangup();
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
            RtmMessage message = mRtmClient.createMessage();
            message.setText(hangupMsg.toString());
            sendPeerMessage(message);
            // 主动挂断，关闭当前activity
            finish();
        }

        // 呼叫
        @JavascriptInterface
        public void call() {
            Log.d(LOG_TAG,">>>>>>呼叫call() ");
//            Log.i(LOG_TAG,">>>consultID:"+consultID+">>>callData:"+callData);
//            mITRTCAudioCall.call(consultID, callData);

//            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO)) {
//                initAgoraEngineAndJoinChannel();
                Log.d(LOG_TAG,"call<<<Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO");
//            }

            JSONObject callData = new JSONObject();
            try {
                callData.put("type", "app-call");
                callData.put("roomid", Integer.parseInt(getUserId()));
                callData.put("houseid", modelID);
                callData.put("houseurl", modelURL);
                SharedPreferences pref = getSharedPreferences("data",MODE_PRIVATE);
                callData.put("channelName", pref.getString("channelName", ""));

                RtmMessage message = mRtmClient.createMessage();
                message.setText(callData.toString());
                sendPeerMessage(message);
            } catch (JSONException e) {
                e.printStackTrace();
            }


        }
    }


    // send message begin
    /**
     * API CALL: send message to peer
     */
    private void sendPeerMessage(final RtmMessage message) {
        Log.d(LOG_TAG,"sendPeerMessage>>>message:"+message.getText());
        mRtmClient.sendMessageToPeer(mPeerId, message, mChatManager.getSendMessageOptions(), new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // do nothing
                Log.d(LOG_TAG,"sendPeerMessage>>>onSuccess:");
            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                Log.d(LOG_TAG,"sendPeerMessage>>>onFailure:");
                // refer to RtmStatusCode.PeerMessageState for the message state
                final int errorCode = errorInfo.getErrorCode();
                runOnUiThread(() -> {
                    switch (errorCode) {
                        case RtmStatusCode.PeerMessageError.PEER_MESSAGE_ERR_TIMEOUT:
                        case RtmStatusCode.PeerMessageError.PEER_MESSAGE_ERR_FAILURE:
                            showToast(getString(R.string.send_msg_failed));
                            break;
                        case RtmStatusCode.PeerMessageError.PEER_MESSAGE_ERR_PEER_UNREACHABLE:
                            showToast(getString(R.string.peer_offline));
                            break;
                        case RtmStatusCode.PeerMessageError.PEER_MESSAGE_ERR_CACHED_BY_SERVER:
                            showToast(getString(R.string.message_cached));
                            break;
                    }
                });
            }
        });
    }
    private String getmTargetName(){
        SharedPreferences pref = getSharedPreferences("data",MODE_PRIVATE);
        String mTargetName = pref.getString("mTargetName", "");
        return mTargetName;
    }
    private String getUserId(){
        SharedPreferences pref = getSharedPreferences("data",MODE_PRIVATE);
        String consultId = pref.getString("consultId", "");
        return consultId;
    }

    // native调用js
    public void callOnData(final String data) {
        mWebView.post(new Runnable() {
            @Override
            public void run() {
                String url = "javascript:onData('" + data + "')";
                mWebView.loadUrl(url);
            }
        });
    }

    // native调用js
    public void callUpdateChatStatus(final String status, String data) {
        mWebView.post(new Runnable() {
            @Override
            public void run() {
                String url = "javascript:updateChatStatus('" + status + "')";
                mWebView.loadUrl(url);
            }
        });

        // 7：被叫方挂断；8：主叫方挂断
        if (status == "8" || status == "7") {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    hangupViewRefresh();
                }
            },3000);
        }
    }
    // 挂断更新页面
    public void hangupViewRefresh() {
        // 界面更新，暂时不需要更新

        //         Toast.makeText(getApplicationContext(), "请关闭、重启应用继续测试",
        //            Toast.LENGTH_LONG).show();
        // 被动挂断，关闭当前activity
        finish();
    }


    /**
     * API CALLBACK: rtm event listener
     */
    class MyRtmClientListener implements RtmClientListener {

        @Override
        public void onConnectionStateChanged(final int state, int reason) {
            runOnUiThread(() -> {
                switch (state) {
                    case RtmStatusCode.ConnectionState.CONNECTION_STATE_RECONNECTING:
                        showToast(getString(R.string.reconnecting));
                        break;
                    case RtmStatusCode.ConnectionState.CONNECTION_STATE_ABORTED:
                        showToast(getString(R.string.account_offline));
                        setResult(MessageUtil.ACTIVITY_RESULT_CONN_ABORTED);
                        finish();
                        break;
                }
            });
        }

        @Override
        public void onMessageReceived(final RtmMessage message, final String peerId) {
            runOnUiThread(() -> {
                try {
                    Log.e("TAG", "===========onMessageReceived<<<<<=data==========" + message.getText());
                    JSONObject jsonObject = new JSONObject(message.getText());
                    if (jsonObject.has("data")
                            && jsonObject.getJSONObject("data").has("state")
                            && jsonObject.getJSONObject("data").getString("state").equals("initdone")){ // webview消息
                        // 暂时仅支持1V1，拒绝其他客户带看请求
                        if (peerId != "" && !mPeerId.equals(peerId)) {
//                        if (peerId.equals(mPeerId)) {
//                            callOnData(message.getText());
                            Log.d(LOG_TAG,"=========================接收消息处理-peerId.equals(mPeerId)");
                            Log.d(LOG_TAG,message.getText());
                        }else{
//                            return true;
                            Log.d(LOG_TAG,"=========================接收消息处理-!peerId.equals(mPeerId)");
                            Log.d(LOG_TAG,message.getText());
                            callOnData(message.getText());
                            callUpdateChatStatus("3", null);
                        }
//                        if (mUserId != "" && !msg.getSender().equals(mUserId)) {
//                            return true;
//                        } else {
//                            callOnData(message.getText());
//                        }
//                        return true;
                    } else if (jsonObject.has("type")
                            && jsonObject.getString("type").equals("app-hangup")) {    // app 经纪人挂断消息
                        // 临时通过消息接收主叫方挂断电话
                        // 暂时仅支持1V1，拒绝其他客户带看请求
//                        if (mUserId != "" && !msg.getSender().equals(peerAudioUserId)) {
//                            return true;
//                        } else {
//                            preExitRoom();
//                        }
//                        return true;
                        Log.d(LOG_TAG,"=========================接收消息处理-经纪人挂断消息-type-app-hangup");
                        Log.d(LOG_TAG,message.getText());
//                        callOnData(message.getText());
                        if (jsonObject.has("hangupType")
                                && jsonObject.getString("hangupType").equals("7")){
                            callUpdateChatStatus("7", null);
                        }else{
                            // hangupType=5
                            // 被叫⽅拒接语⾳呼叫（顾问拒接）
                            callUpdateChatStatus("3", null);
                            callUpdateChatStatus("5", null);
//                        showToast("经纪人忙");
                        }

                    } else {    // webview消息
                        // 暂时仅支持1V1，拒绝其他客户带看请求
                        if (peerId != "" && !mPeerId.equals(peerId)) {
//                            return true;
                            Log.d(LOG_TAG,"=========================接收消息处理-aaaa");
                            Log.d(LOG_TAG,message.getText());
                        } else {
                            if(message.getText().indexOf("initstatedone")>0){
//                                callUpdateChatStatus("103", null);
                                Log.d(LOG_TAG,"=========================接收消息处理-bbbbcccccc");
                            }
                            Log.d(LOG_TAG,"=========================接收消息处理-bbbb");
                            Log.d(LOG_TAG,message.getText());
                            callOnData(message.getText());
                        }
                        Log.d(LOG_TAG,"=========================接收消息处理-type-app-else-hangup");
                        Log.d(LOG_TAG,message.getText());
//                        return true;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.d(LOG_TAG,"=========================接收消息处理-catch");
                    Log.d(LOG_TAG,e.getMessage());
//                    return false;
                }
                /*
                if (peerId.equals(mPeerId)) {
                    Log.d(LOG_TAG,"onMessageReceived>>>>>>peerId.equals(mPeerId):"+message.getText());
                    if(message.getText().indexOf("initstatedone")>0){
                        Log.d(LOG_TAG,"=========================接收消息处理<<<<<<<<<对方 initstatedone----->end");
                        callUpdateChatStatus("101", null);
                    }
                    showToast(message.getText());
                    // 接收消息处理 begin
                    try {
                        JSONObject jsonObject = new JSONObject(message.getText());
//                        callData.put("type", "app-call");
//                        callData.put("roomid", Integer.parseInt(getUserId()));
//                        callData.put("houseid", modelID);
//                        callData.put("houseurl", modelURL);
//                        modelURL=jsonObject.getString("houseurl");
                        mPeerId=""+jsonObject.getInt("roomid");
                        Log.d(LOG_TAG,"接收消息处理-----");
                        Log.d(LOG_TAG,message.getText());
//                        Log.d(LOG_TAG,"onMessageReceived-modelURL:"+modelURL);
                        Log.d(LOG_TAG,"onMessageReceived-mPeerId:"+mPeerId);
                        Log.d(LOG_TAG,"接收消息处理----->end");

                    } catch (JSONException e) {
                        Log.d(LOG_TAG,"接收消息处理----->error");
                        Log.d(LOG_TAG,e.getMessage());
                        Log.d(LOG_TAG,"接收消息处理----->error");
                        e.printStackTrace();
                    }
                    // 接收消息处理 end

                } else {
                    Log.d(LOG_TAG,"接收消息处理----->2222");
                    Log.d(LOG_TAG,"onMessageReceived=======peerId.equals(mPeerId):"+message.getText());
                    // 接收消息处理 begin
                    try {
                        JSONObject jsonObject = new JSONObject(message.getText());
                        Log.d(LOG_TAG,"接收消息处理----->2222");
                        Log.d(LOG_TAG,message.getText());
//                        callData.put("type", "app-call");
//                        callData.put("roomid", Integer.parseInt(getUserId()));
//                        callData.put("houseid", modelID);
//                        callData.put("houseurl", modelURL);
                        //modelURL=jsonObject.getString("houseurl");
                        String type=jsonObject.getString("type");
                        mPeerId=""+jsonObject.getInt("roomid");
                        mPeerId=""+jsonObject.getInt("roomid");
                        Log.d(LOG_TAG,"接收消息处理-----222");
//                        Log.d(LOG_TAG,"onMessageReceived-222modelURL:"+modelURL);
                        Log.d(LOG_TAG,"onMessageReceived-222mPeerId:"+mPeerId);
                        Log.d(LOG_TAG,"接收消息处理----->222end");

                    } catch (JSONException e) {
                        Log.d(LOG_TAG,"接收消息处理----->222error");
                        Log.d(LOG_TAG,e.getMessage());
                        Log.d(LOG_TAG,"接收消息处理----->222error");
                        e.printStackTrace();
                    }
                    // 接收消息处理 end
                }
                */
            });
        }

        @Override
        public void onImageMessageReceivedFromPeer(final RtmImageMessage rtmImageMessage, final String peerId) {
            runOnUiThread(() -> {
                if (peerId.equals(mPeerId)) {
                    Log.d(LOG_TAG,"onImageMessageReceivedFromPeer>>>>>>peerId.equals(mPeerId)");
                } else {
                    Log.d(LOG_TAG,"onImageMessageReceivedFromPeer=======peerId.equals(mPeerId)");
                }
            });
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
    // send message end


//    voice begin
public boolean checkSelfPermission(String permission, int requestCode) {
    Log.i(LOG_TAG, "checkSelfPermission " + permission + " " + requestCode);
    if (ContextCompat.checkSelfPermission(this,
            permission)
            != PackageManager.PERMISSION_GRANTED) {

        ActivityCompat.requestPermissions(this,
                new String[]{permission},
                requestCode);
        return false;
    }
    return true;
}
private void initAgoraEngineAndJoinChannel() {
    initializeAgoraEngine();     // Tutorial Step 1
    joinChannel();               // Tutorial Step 2
}
// Tutorial Step 7
public void onLocalAudioMuteClicked(View view) {
    ImageView iv = (ImageView) view;
    if (iv.isSelected()) {
        iv.setSelected(false);
        iv.clearColorFilter();
    } else {
        iv.setSelected(true);
        iv.setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
    }

    // Stops/Resumes sending the local audio stream.
    mRtcEngine.muteLocalAudioStream(iv.isSelected());
}

    // Tutorial Step 5
    public void onSwitchSpeakerphoneClicked(View view) {
        ImageView iv = (ImageView) view;
        if (iv.isSelected()) {
            iv.setSelected(false);
            iv.clearColorFilter();
        } else {
            iv.setSelected(true);
            iv.setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
        }

        // Enables/Disables the audio playback route to the speakerphone.
        //
        // This method sets whether the audio is routed to the speakerphone or earpiece. After calling this method, the SDK returns the onAudioRouteChanged callback to indicate the changes.
        mRtcEngine.setEnableSpeakerphone(view.isSelected());
    }

    // Tutorial Step 3
    public void onEncCallClicked(View view) {
        finish();
    }

    // Tutorial Step 1
    private void initializeAgoraEngine() {
        try {
            mRtcEngine = RtcEngine.create(getBaseContext(), getString(R.string.agora_app_id), mRtcEventHandler);

        } catch (Exception e) {
            Log.e(LOG_TAG, Log.getStackTraceString(e));

            throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
        }
    }

    // Tutorial Step 2
    private void joinChannel() {
        String channelName=createChannel(mUserId);
        String accessToken = getToken(Integer.parseInt(mUserId),channelName);//getString(R.string.agora_access_token);
        Log.d(LOG_TAG,"joinChannel>>>>>:"+mUserId+"["+channelName+"]");
        Log.d(LOG_TAG,"token>>>>>:"+accessToken);
        if (TextUtils.equals(accessToken, "") || TextUtils.equals(accessToken, "#YOUR ACCESS TOKEN#")) {
            accessToken = null; // default, no token
        }

        mRtcEngine.setLogFilter(0x080f);

        String ts = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String filepath = "/sdcard/" + ts + ".log";
        File file = new File(filepath);
        mRtcEngine.setLogFile(filepath);

        mRtcEngine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
        mRtcEngine.setAudioProfile(Constants.AUDIO_PROFILE_MUSIC_HIGH_QUALITY_STEREO,Constants.AUDIO_SCENARIO_GAME_STREAMING);
        mRtcEngine.setDefaultAudioRoutetoSpeakerphone(true);
        // Sets the channel profile of the Agora RtcEngine.
        // CHANNEL_PROFILE_COMMUNICATION(0): (Default) The Communication profile. Use this profile in one-on-one calls or group calls, where all users can talk freely.
        // CHANNEL_PROFILE_LIVE_BROADCASTING(1): The Live-Broadcast profile. Users in a live-broadcast channel have a role as either broadcaster or audience. A broadcaster can both send and receive streams; an audience can only receive streams.
        mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION);

        // Allows a user to join a channel.
        mRtcEngine.joinChannel(accessToken, channelName, "Extra Optional Data", Integer.parseInt(mUserId)); // if you do not specify the uid, we will generate the uid for you
        Log.d(LOG_TAG,"joinChannel>>>>:"+mUserId);
    }

    // Tutorial Step 3
    private void leaveChannel() {
        mRtcEngine.leaveChannel();
    }

    // Tutorial Step 4
    private void onRemoteUserLeft(int uid, int reason) {
        showToast(String.format(Locale.US, "user %d left %d", (uid & 0xFFFFFFFFL), reason));
    }

    // Tutorial Step 6
    private void onRemoteUserVoiceMuted(int uid, boolean muted) {
        showToast(String.format(Locale.US, "user %d muted or unmuted %b", (uid & 0xFFFFFFFFL), muted));
    }
//    voice end
    private void showToast(final String text) {
        runOnUiThread(() -> Toast.makeText(ShowVrActivity.this, text, Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        leaveChannel();
        RtcEngine.destroy();
        mRtcEngine = null;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        Log.i(LOG_TAG, "onRequestPermissionsResult " + grantResults[0] + " " + requestCode);

        switch (requestCode) {
            case PERMISSION_REQ_ID_RECORD_AUDIO: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initAgoraEngineAndJoinChannel();
                } else {
                    showToast("No permission for " + Manifest.permission.RECORD_AUDIO);
                    finish();
                }
                break;
            }
        }
    }

    private String getToken(Integer fromId,String channelName)  {
        final String[] result = {""};
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    OkHttpClient client = new OkHttpClient();//创建OkHttpClient对象
                    Map<String, Object> bodyMap = new HashMap<>();
                    bodyMap.put("channelName",channelName);
                    bodyMap.put("uid", fromId);
                    bodyMap.put("role", 1);
                    //第二步，将封装好的数据转换为json格式
                    String jsonParams = new JSONObject(bodyMap).toString();
                    //第三步，创建requestbody，post请求需要提交一个表单application/json代表数据是个json
                    RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8")
                            , jsonParams);

                    Request request = new Request.Builder()
                            .url("https://pm.shcobol.com/agora/fetch_rtc_token")
                            .post(body)//传递请求体
                            .build();
                    Response response = null;
                    response = client.newCall(request).execute();//得到Response 对象
                    if (response.isSuccessful()) {
                        Log.d(LOG_TAG,"获取数据成功了");
                        Log.d(LOG_TAG,"response.code()=="+response.code());
                        String rt = response.body().string();
                        Log.d(LOG_TAG,"response.body().string()==" + rt);
                        result[0] =rt;
                        JSONObject jsonObject = new JSONObject(rt);
                        if (jsonObject.has("token")){
                            result[0]=jsonObject.getString("token");
                        }else{
                            result[0]="-";
                        }

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d(LOG_TAG,"Exception==" + e.getMessage());
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
        } catch(InterruptedException e){
            e.printStackTrace();
        }
        return result[0];
    }

    private  String createChannel(String mUserId) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dataStr = format.format(new Date())+mUserId;
        final String slat = "&%5123***&&%%$$#@";
        try {
            dataStr = dataStr + slat;
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(dataStr.getBytes("UTF8"));
            byte s[] = m.digest();
            String result = "";
            for (int i = 0; i < s.length; i++) {
                result += Integer.toHexString((0x000000FF & s[i]) | 0xFFFFFF00).substring(6);
            }
            SharedPreferences.Editor editor = getSharedPreferences("data",MODE_PRIVATE).edit();
            editor.putString("channelName",result);
            editor.commit();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }
}