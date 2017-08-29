package com.gomo.minivideo;

import android.app.Application;

/**
 * Created by ruanjiewei on 2017/8/27
 */

public class CameraApp extends Application {

    private static CameraApp sInstant;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstant = this;
    }

    public static CameraApp getApplication() {
        return sInstant;
    }
}
