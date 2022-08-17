package com.quick.jsbridge.view;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class PreloadService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
