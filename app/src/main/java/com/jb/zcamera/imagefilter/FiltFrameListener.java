package com.jb.zcamera.imagefilter;

import android.graphics.Bitmap;

public interface FiltFrameListener {
    boolean needCallback();
    void onFiltFrameDraw(Bitmap bitmap);
}
