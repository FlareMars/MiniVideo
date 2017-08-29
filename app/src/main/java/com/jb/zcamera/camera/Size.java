package com.jb.zcamera.camera;

/**
 * Created by ruanjiewei on 2017/8/27
 */

public class Size {

    public int width = 0;
    public int height = 0;

    public Size(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public boolean equals(Size that) {
        return this.width == that.width && this.height == that.height;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }
}
