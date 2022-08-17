package com.silversea.rtmtutorial;

import android.app.Application;
import android.content.Context;

public class AGApplication extends Application {
//    private static AGApplication sInstance;
    private static AGApplication sInstance = null;

    private ChatManager mChatManager;

    public static AGApplication getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new AGApplication(context);
        }
        return sInstance;
    }


    private AGApplication(Context context) {
        mChatManager = new ChatManager(context);
        mChatManager.init();
    }

    public ChatManager getChatManager() {
        return mChatManager;
    }
}

