package com.jb.zcamera.camera;

import android.graphics.Rect;

/**
 * Created by ruanjiewei on 2017/8/27
 */

public class Area {

    public Rect rect = null;
    public int weight = 0;

    public Area(Rect rect, int weight) {
        this.rect = rect;
        this.weight = weight;
    }
}
