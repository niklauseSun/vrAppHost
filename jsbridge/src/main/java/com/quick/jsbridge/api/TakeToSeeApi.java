package com.quick.jsbridge.api;

import android.net.rtp.AudioGroup;
import android.webkit.WebView;

import com.quick.jsbridge.bridge.Callback;
import com.quick.jsbridge.bridge.IBridgeImpl;
import com.quick.jsbridge.control.AutoCallbackDefined;
import com.quick.jsbridge.takeToSee.AgoraMessage;
import com.quick.jsbridge.takeToSee.AgoraVideo;
import com.quick.jsbridge.view.IQuickFragment;

import org.json.JSONObject;

public class TakeToSeeApi implements IBridgeImpl {
    /**
     * 注册API的别名
     */
    public static String RegisterName = "taketosee";

    public static void loginWithUserName(IQuickFragment webLoader, WebView wv, JSONObject param, Callback callback) {
        String appId = param.optString("appId");
        String userId = param.optString("userId");
        String token = param.optString("token");

        AgoraMessage.getInstance().initRTMMessageClient(webLoader.getPageControl().getContext(), appId);
        AgoraMessage.getInstance().loginRTMClient(token, userId);

        webLoader.getWebloaderControl().addPort(AutoCallbackDefined.onLoginSuccess, callback.getPort());
    }

    public static void loginWithOutToken(IQuickFragment webLoader, WebView wv, JSONObject param, Callback callback) {
        String appId = param.optString("appId");
        String userId = param.optString("userId");
        AgoraMessage.getInstance().initRTMMessageClientWithH5(webLoader.getPageControl().getContext(), webLoader, appId);
        AgoraMessage.getInstance().loginRTMClientWithOutToken(userId);

        webLoader.getWebloaderControl().addPort(AutoCallbackDefined.onLoginSuccess, callback.getPort());
    }


    public static void logout(IQuickFragment webLoader, WebView wv, JSONObject param, Callback callback) {
        AgoraMessage.getInstance().logoutRtm();

        webLoader.getWebloaderControl().addPort(AutoCallbackDefined.onLogoutSuccess, callback.getPort());
    }

    public static void createChannelListener(IQuickFragment webLoader, WebView wv, JSONObject param, Callback callback) {
        AgoraMessage.getInstance().createChannerListener();
    }

    public static void joinChannel(IQuickFragment webLoader, WebView wv, JSONObject param, Callback callback){
        String channelName = param.optString("channelName");
        webLoader.getWebloaderControl().addPort(AutoCallbackDefined.onJoinChannel, callback.getPort());
        AgoraMessage.getInstance().joinChannel(channelName);

    }

    public static void leaveChannel(IQuickFragment webLoader, WebView wv, JSONObject param, Callback callback) {
        webLoader.getWebloaderControl().addPort(AutoCallbackDefined.onLeaveChannel, callback.getPort());
        AgoraMessage.getInstance().leaveChannel();
    }

    public static void sendGroupMessage(IQuickFragment webLoader, WebView wv, JSONObject param, Callback callback) {
        webLoader.getWebloaderControl().addPort(AutoCallbackDefined.onSendGroupMessage, callback.getPort());
        String channelText = param.optString("channelText");
        AgoraMessage.getInstance().sendChannelMessage(channelText);
    }

    public static void sendPeerMessage(IQuickFragment webLoader, WebView wv, JSONObject param, Callback callback) {
        webLoader.getWebloaderControl().addPort(AutoCallbackDefined.onSendPeerMessage, callback.getPort());
        String userId = param.optString("userId");
        String peerMessage = param.optString("peerMessage");
        AgoraMessage.getInstance().sendPeerMessage(peerMessage, userId);
    }

    // 语音相关
    public static void initAgoraCall(IQuickFragment webLoader, WebView wv, JSONObject param, Callback callback) {
        webLoader.getWebloaderControl().addPort(AutoCallbackDefined.onInitCall, callback.getPort());
        String appId = param.optString("appId");
        AgoraVideo.getInstance().initAgoraVideoEngine(webLoader, appId);
    }

    public static void joinAgoraCallChannel(IQuickFragment webLoader, WebView wv, JSONObject param, Callback callback) {
        webLoader.getWebloaderControl().addPort(AutoCallbackDefined.onJoinCallChannel, callback.getPort());
        String accessToken = param.optString("accessToken");
        String channelName = param.optString("channelName");
        String userId = param.optString("userId");
        String extraInfo = param.optString("extraInfo");

        int uid = Integer.parseInt(userId);

        AgoraVideo.getInstance().joinAgoraVideoChannel(accessToken, channelName, uid, extraInfo);
    }

    public static void leaveAgoraCallChannel(IQuickFragment webLoader, WebView wv, JSONObject param, Callback callback) {
        webLoader.getWebloaderControl().addPort(AutoCallbackDefined.onLeaveCallChannel, callback.getPort());
        AgoraVideo.getInstance().leaveChannle();
    }

    public static void adjustPlayerVolume(IQuickFragment webLoader, WebView wv, JSONObject param, Callback callback) {
        int volume = param.optInt("volume");
//        AgoraVideo.getInstance().adjustPlayerVolume(volume);
    }

    public static void adjustRecordingVolume(IQuickFragment webLoader, WebView wv, JSONObject param, Callback callback) {
        int volume = param.optInt("volume");
        AgoraVideo.getInstance().adjustRecordingVolume(volume);
    }
}
