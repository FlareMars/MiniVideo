package com.jb.zcamera.imagefilter;

import android.graphics.SurfaceTexture;

/**
 * Render回调接口
 *
 * Created by oujingwen on 15-12-16.
 */
public interface IRenderCallback {
    void onSurfaceTextureCreated(SurfaceTexture surfaceTexture);
    void onFrameAvaliable(long frameTimeNanos);
}
