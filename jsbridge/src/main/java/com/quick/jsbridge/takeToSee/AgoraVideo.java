package com.quick.jsbridge.takeToSee;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.quick.jsbridge.view.IQuickFragment;

import java.util.HashMap;

import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;

public class AgoraVideo implements IRtcImpl{

    private final String LOG_TAG = "AgoraVideo";

    private static AgoraVideo single = null;


    private RtcEngine mRtcEngine;

    private IQuickFragment mWebLoader = null;

    public static AgoraVideo getInstance() {
        if (single == null) {
            single = new AgoraVideo();  //在第一次调用getInstance()时才实例化，实现懒加载,所以叫懒汉式
        }
        return single;
    }

    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onUserJoined(int uid, int elapsed) {
            super.onUserJoined(uid, elapsed);
            // 用户进入
            Log.i(LOG_TAG, "enter" + uid);

            HashMap map = new HashMap();

            map.put("userId", uid + "");
            map.put("status", "join");
            mWebLoader.getWebloaderControl().autoCallbackEvent.onCallUserState(map);
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            super.onUserOffline(uid, reason);
            // 用户掉线

            Log.i(LOG_TAG, "offline" + reason);

            HashMap map = new HashMap();

            map.put("userId", uid + "");
            map.put("status", "leave");
            map.put("reason", reason + "");
            mWebLoader.getWebloaderControl().autoCallbackEvent.onCallUserState(map);
        }


        @Override
        public void onUserMuteAudio(int uid, boolean muted) {
            super.onUserMuteAudio(uid, muted);
            // 静音

            Log.i(LOG_TAG, "mute" + uid);

            HashMap map = new HashMap();

            map.put("userId", uid + "");
            map.put("muted", muted);
            mWebLoader.getWebloaderControl().autoCallbackEvent.onCallUserMute(map);
        }
    };

    public void initAgoraVideoEngine(IQuickFragment webLoader, String appId) {
        mWebLoader = webLoader;
        initializeAgoraEngine(webLoader.getPageControl().getContext(), appId);
    }

    public void joinAgoraVideoChannel(String accessToken, String channelName, int uid, String extraInfo) {
        joinChannel(accessToken, channelName, uid, extraInfo);
    }

    public void leaveChannle() {
        mRtcEngine.leaveChannel();
        HashMap map = new HashMap();

        map.put("type", "success");
        mWebLoader.getWebloaderControl().autoCallbackEvent.onLeaveCallChannel(map);
    }

    public void adjustPlayerVolume(int volume) {
        mRtcEngine.adjustPlaybackSignalVolume(volume);
    }

    public void adjustRecordingVolume(int volume) {
        mRtcEngine.adjustRecordingSignalVolume(volume);
    }



    private void initializeAgoraEngine(Context context, String appId) {
        try {
            mRtcEngine = RtcEngine.create(context, appId, mRtcEventHandler);

            HashMap map = new HashMap();

            map.put("type", "success");

            mWebLoader.getWebloaderControl().autoCallbackEvent.onInitCall(map);

        } catch (Exception e) {
            Log.e(LOG_TAG, Log.getStackTraceString(e));

            HashMap map = new HashMap();

            map.put("type", "fail");

            mWebLoader.getWebloaderControl().autoCallbackEvent.onInitCall(map);
            throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
        }
    }

    private void joinChannel(String accessToken, String channelName, int uid, String extraInfo) {
        mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION);

        mRtcEngine.joinChannel(accessToken, channelName, extraInfo , uid);
        HashMap map = new HashMap();

        map.put("type", "success");
        mWebLoader.getWebloaderControl().autoCallbackEvent.onJoinCallChannel(map);
    }

}
