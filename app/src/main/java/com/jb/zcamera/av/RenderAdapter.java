package com.jb.zcamera.av;

import android.graphics.SurfaceTexture;

/**
 * Created by oujingwen on 15-7-15.
 */
public interface RenderAdapter {
    void drawFrame(boolean eosRequested);

    void realse();

    void requestRender();

    SurfaceTexture getSurfaceTexture();
}
