package com.gomo.minivideo;

import android.app.Application;

import com.facebook.FacebookSdk;

/**
 * Created by ruanjiewei on 2017/8/27
 */

public class CameraApp extends Application {

    private static CameraApp sInstant;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstant = this;
        FacebookSdk.sdkInitialize( getApplicationContext() );
    }

    public static CameraApp getApplication() {
        return sInstant;
    }
}
