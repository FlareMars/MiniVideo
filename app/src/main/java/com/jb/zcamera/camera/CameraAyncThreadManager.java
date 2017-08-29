
package com.jb.zcamera.camera;

import android.os.HandlerThread;
import android.os.Looper;

/**
 * 用于摄像头处理的异步线程单例
 * 
 * @author oujingwen
 */
public class CameraAyncThreadManager {

    private static CameraAyncThreadManager sInstance;

    private Looper mLooper;

    private CameraAyncThreadManager() {
        init();
    }

    public static synchronized CameraAyncThreadManager getInstance() {
        if (sInstance == null) {
            sInstance = new CameraAyncThreadManager();
        }
        return sInstance;
    }

    private void init() {
        HandlerThread thread = new HandlerThread("Async Handler");
        thread.start();
        mLooper = thread.getLooper();
    }

    public Looper getLooper() {
        return mLooper;
    }
}
