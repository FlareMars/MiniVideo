package com.jb.zcamera.camera;

/**
 * Created by oujingwen on 15-11-30.
 */
public class VideoQuality {
    public String mQuality;
    public Size mSize;

    public VideoQuality(String quality, Size size) {
        this.mSize = size;
        this.mQuality = quality;
    }

    @Override
    public String toString() {
        return mQuality;
    }
}
