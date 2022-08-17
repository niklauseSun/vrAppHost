package com.vapp.taketosee;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

//import io.agora.rtc.Constants;
//import io.agora.rtc.IRtcEngineEventHandler;
//import io.agora.rtc.RtcEngine;

public class TakeToSee implements IRtcImpl {

    private static final String LOG_TAG = "TakeToSeePACKAGE";
    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 22;

    private Context baseContext = null;
    private Activity baseActivity = null;

//    private RtcEngine mRtcEngine;
//
//    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
//        @Override
//        public void onUserJoined(int uid, int elapsed) {
//            super.onUserJoined(uid, elapsed);
//            // 用户进入
//            Log.i(LOG_TAG, "enter" + uid);
//        }
//
//        @Override
//        public void onUserOffline(int uid, int reason) {
//            super.onUserOffline(uid, reason);
//            // 用户掉线
//
//            Log.i(LOG_TAG, "offline" + reason);
//        }
//
//
//        @Override
//        public void onUserMuteAudio(int uid, boolean muted) {
//            super.onUserMuteAudio(uid, muted);
//            // 静音
//
//            Log.i(LOG_TAG, "mute" + uid);
//
//        }
//    };

    public void initAgoraEngine(Context mContext, String appId) {
        baseContext = mContext;
//        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO)) {
            initializeAgoraEngine(mContext, appId);
//        }
    }

    public void joinChannel(String accessToken, String channelName, int uid, String extraInfo) {
        joinChannelAction(accessToken, channelName, uid, extraInfo);
    }

//    public void muteChannel() {
//        mRtcEngine.adjustRecordingSignalVolume(0);
//    }


    public void adjustRecordingVolume(int volume) {
//        mRtcEngine.adjustRecordingSignalVolume(volume);
    }

//    public void muteRemoteUser() {
//        mRtcEngine.adjustPlaybackSignalVolume(0);
//    }

//    public void adjustPlayerVolume(int volume) {
//        mRtcEngine.adjustPlaybackSignalVolume(0);
//    }

//    public void leaveChannle() {
//        mRtcEngine.leaveChannel();
//    }

    public void setActivity(Activity activity) {
        baseActivity = activity;
    }

    public boolean checkSelfPermission(String permission, int requestCode) {
        Log.i(LOG_TAG, "checkSelfPermission " + permission + " " + requestCode);

        if (ContextCompat.checkSelfPermission(baseContext, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(baseActivity, new String[]{permission}, requestCode);
            return false;
        }
        return true;
    }

    private void initializeAgoraEngine(Context context, String appId) {
        try {
//            mRtcEngine = RtcEngine.create(context, appId, mRtcEventHandler);
        } catch (Exception e) {
            Log.e(LOG_TAG, Log.getStackTraceString(e));

            throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
        }
    }

    private void joinChannelAction(String accessToken, String channelName, int uid, String extraInfo) {
//        mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION);

//        mRtcEngine.joinChannel(accessToken, channelName, extraInfo , uid);
    }
}
