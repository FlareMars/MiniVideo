package com.jb.zcamera.imagefilter.filter;

import android.graphics.PointF;

/**
 * 
 * @author chenfangyi
 * 
 * 调整的Fiter
 *
 */
public class GPUImageAdjustFilter extends GPUImageFilterGroup{
	
	private GPUImageBaseAdjustFilter mBaseAdjustFilter;

	private GPUImageTemperatureAndToneCurveFilter mOtherFilter;

	private GPUImageSharpenFilter mSharpenFilter;
	
	private GPUImageVignetteFilter mVignetteFilter;
	
	public GPUImageAdjustFilter(){
		mBaseAdjustFilter = new GPUImageBaseAdjustFilter(1.0f, 0.0f, 1.0f);
		PointF centerPoint = new PointF(0.5f, 0.5f);
		mVignetteFilter = new GPUImageVignetteFilter(centerPoint, new float[] {0.0f, 0.0f, 0.0f}, 0.7f, 1f);

		mOtherFilter = new GPUImageTemperatureAndToneCurveFilter();

		mSharpenFilter = new GPUImageSharpenFilter(0.0f);

		this.addFilter(mBaseAdjustFilter);
		this.addFilter(mOtherFilter);
		this.addFilter(mSharpenFilter);
		this.addFilter(mVignetteFilter);
	}
	
	public void setContrast(final float contrast) {
		mBaseAdjustFilter.setContrast(contrast);
    }
    
    public void setBrightness(final float brightness) {
    	mBaseAdjustFilter.setBrightness(brightness);
    }
    
    public void setSaturation(final float saturation) {
    	mBaseAdjustFilter.setSaturation(saturation);
    }
    
    public void setVignetteStart(final float vignetteStart) {
    	mVignetteFilter.setVignetteStart(vignetteStart);
    }

	public void setCurve(final float temperature){
		mOtherFilter.setCurve(temperature);
	}

	public void setTemperature(final float temperature){
		mOtherFilter.setTemperature(temperature);
	}

	public void setSharpness(final float sharpness){
		mSharpenFilter.setSharpness(sharpness);
	}
}
