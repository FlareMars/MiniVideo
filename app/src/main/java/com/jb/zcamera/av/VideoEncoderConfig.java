package com.jb.zcamera.av;

/**
 * @hide
 */
public class VideoEncoderConfig {
    protected final int mWidth;
    protected final int mHeight;
    protected final int mDegrees;
    protected final int mBitRate;

    public VideoEncoderConfig(int width, int height, int degrees, int bitRate) {
        mWidth = width;
        mHeight = height;
        mDegrees = degrees;
        mBitRate = bitRate;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public int getBitRate() {
        return mBitRate;
    }

    public int getDegrees() {
        return mDegrees;
    }

    @Override
    public String toString() {
        return "VideoEncoderConfig: " + mWidth + "x" + mHeight +  " " + mDegrees + " @" + mBitRate + " bps";
    }
}