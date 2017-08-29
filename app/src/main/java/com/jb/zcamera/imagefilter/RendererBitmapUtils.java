package com.jb.zcamera.imagefilter;

import android.graphics.Bitmap;

import com.jb.zcamera.imagefilter.filter.GPUImageFilter;

import junit.framework.Assert;

/**
 * Created by chenfangyi on 17-4-6.
 */

public class RendererBitmapUtils {
    private GPUImageRenderer mRenderer;
    private PixelBuffer mBuffer;
    private Bitmap mBaseBitmap;
    private FilterAdjuster mFilterAdjuster;
    private GPUImageFilter mFilter;
    private boolean mIsDestroy;

    /**
     * 这个Bitmap相当于原始的图片 整个操作的过程中是不会修改到它的
     * @param bitmap
     */
    public RendererBitmapUtils(Bitmap bitmap){
        this(null, bitmap);
    }

    /**
     * 这个Bitmap相当于原始的图片 整个操作的过程中是不会修改到它的
     * @param bitmap
     */
    public RendererBitmapUtils(GPUImageFilter filter, Bitmap bitmap){
        if(filter == null) filter = new GPUImageFilter();
        mFilter = filter;
        mFilterAdjuster = new FilterAdjuster(mFilter);
        mBaseBitmap = bitmap;
        mRenderer = new GPUImageRenderer(mFilter, null, false);
        mRenderer.setImageBitmap(mBaseBitmap, false);
        mBuffer = new PixelBuffer(mBaseBitmap.getWidth(), mBaseBitmap.getHeight());
        mBuffer.setRenderer(mRenderer);
        mRenderer.setFilter(mFilter, null);
        mIsDestroy = false;
    }

    /**
     * 应用效果到这个bitmap上
     * @param bitmap
     */
    public void applyFilter(Bitmap bitmap){
        Assert.assertTrue(!mIsDestroy);
        //baseBitmap和bitmap一定不能相等
        Assert.assertTrue(bitmap != mBaseBitmap);
        mBuffer.applyToBitmap(bitmap);
    }

    /**
     * 应用效果到这个bitmap上
     * @param bitmap
     * @param progress
     */
    public void applyFilter(Bitmap bitmap, int progress){
        Assert.assertTrue(!mIsDestroy);
        adjustFilterIntensity(progress);
        //baseBitmap和bitmap一定不能相等
        Assert.assertTrue(bitmap != mBaseBitmap);
        mBuffer.applyToBitmap(bitmap);
    }

    /**
     * 更换滤镜
     * @param filter
     */
    public void changeFilter(GPUImageFilter filter){
        Assert.assertTrue(!mIsDestroy);
        mFilter = filter;
        mFilterAdjuster = new FilterAdjuster(mFilter);
        mRenderer.setFilter(mFilter, null);
    }

    /**
     * @param progress  范围0-100
     */
    private void adjustFilterIntensity(int progress){
        mFilterAdjuster.adjust(progress);
    }

    /**
     * 退出的时候调用这个
     */
    public void destroy(){
        if(mIsDestroy) return;
        mFilter.destroy();
        mRenderer.deleteImage();
        mBuffer.destroy();
        mBaseBitmap = null;
        mIsDestroy = true;
    }
}
