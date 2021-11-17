package com.quick.jsbridge.takeToSee;

import android.app.Application;
import android.content.Context;

public class AgApplication extends Application {


    private static AgApplication sInstance = null;

    private AGChatManager mChatManager;

    public static AgApplication getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new AgApplication(context);
        }
        return sInstance;
    }


    private AgApplication(Context context) {
        mChatManager = new AGChatManager(context);
        mChatManager.init();
    }

    public AGChatManager getChatManager() {
        return mChatManager;
    }
}
