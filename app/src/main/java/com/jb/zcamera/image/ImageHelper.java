package com.jb.zcamera.image;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import com.gomo.minivideo.CameraApp;
import com.gomo.minivideo.R;

/**
 * 
 * @author chenfangyi
 * 这个类用于在Content provider中获取图片的URI
 * 还有将URI转化为BItmap的方法
 */
public class ImageHelper {

	public static int SCREEN_WIDTH = 0;
	public static int SCREEN_HEIGHT = 0;
	private static int HEIGHT = 0;
	private static int mMaxMemory;

	static{
		DisplayMetrics dm = CameraApp.getApplication().getResources().getDisplayMetrics();
		boolean isLandscape = false;
		if(CameraApp.getApplication().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
			isLandscape = true;
		}
		if(isLandscape){//这样是为了保证 SCREEN_WIDTH一定是width  SCREEN_HEIGHT一定是height
			SCREEN_WIDTH = dm.heightPixels;
			SCREEN_HEIGHT = dm.widthPixels;
		} else{
			SCREEN_WIDTH = dm.widthPixels;
			SCREEN_HEIGHT = dm.heightPixels;
		}
		HEIGHT = SCREEN_HEIGHT - CameraApp.getApplication().getResources().getDimensionPixelSize(R.dimen.image_eidt_select_bar_height)
				- CameraApp.getApplication().getResources().getDimensionPixelSize(R.dimen.image_edit_bottom_bar_height);
		mMaxMemory = (int) Runtime.getRuntime().maxMemory();
//		if(mMaxMemory >= 536870912){
//			QUALITY = 0.7f;
//		} else{
//			QUALITY = 0.4f;
//		}
	}


	public static int dpToPx(Resources res, int dp){
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
				res.getDisplayMetrics());
	}

	/**
	 * 新策略用于图片预览 和 编辑的图片压缩
	 * 向下取
	 * @param width
	 * @param height
	 * @return
	 */
	public static float getFitSampleSizeLarger(int width, int height){
//		int nw, nh;
//
//		//保证屏幕分辨率的质量
//		if(width * 1.0f / height >= SCREEN_WIDTH * 1.0f / SCREEN_HEIGHT){//宽顶着
//			nw = SCREEN_WIDTH;
//			nh = (int)(height * SCREEN_WIDTH * 1.0f / width + 0.5f);
//		} else{//高顶着
//			nh = SCREEN_HEIGHT;
//			nw = (int)(width * SCREEN_HEIGHT * 1.0f / height + 0.5f);
//		}
//
//		//nw nh是显示在屏幕上的像素宽高
		int size = width * height * 4;//原来的大小

//		int minSize = nw * nh * 4;
//		int midSize = SCREEN_WIDTH * SCREEN_HEIGHT * 4;

		float maxSize;
		if(is1080PResolution()){//高分辨率
			if(isHighMemory()){
				maxSize = 1920 * 1080 * 4 * 2;
				if(size > maxSize){//这时候才需要压缩
					return (size / maxSize);
				}
			} else{
				maxSize = 1920 * 1080 * 4 * 1.5f;
				if(size > maxSize){//这时候才需要压缩
					return (size / maxSize);
				}
			}
		} else if(is720PResolution()){//中分辨率
			if(isHighMemory()){
				maxSize = 1280 * 720 * 4 * 3.5f;
				if(size > maxSize){//这时候才需要压缩
					return (size / maxSize);
				}
			} else{
				maxSize = 1280 * 720 * 4 * 2.5f;
				if(size > maxSize){//这时候才需要压缩
					return (size / maxSize);
				}
			}
		} else{//低分辨率
			if(isHighMemory()){
				maxSize = SCREEN_WIDTH * SCREEN_HEIGHT * 4 * 4;
				if(size > maxSize){//这时候才需要压缩
					return (size / maxSize);
				}
			} else{
				maxSize = SCREEN_WIDTH * SCREEN_HEIGHT * 4 * 2f;
				if(size > maxSize){//这时候才需要压缩
					return (size / maxSize);
				}
			}
		}

		return 1;

//		float count = size / memory;
//		if(count <= 1.0f){
//			return 1;
//		} else{
//
//		}
//
//		int result = 1;
//		while((size / (result * result)) > minSize){
//			result = result * 2;
//		}
//		if(result != 1){
//			result /= 2;
////			while(result != 1 && (size * 1.0f / (result * result / 4)) < midSize ){
////				result /= 2;
////			}
//		}
//		return result;
	}

	public static float checkCanvasAndTextureSize(int width, int height, float scale){
		float result = scale;
		Canvas canvas = new Canvas();
		int maxW = canvas.getMaximumBitmapWidth() / 8;
		int maxH = canvas.getMaximumBitmapHeight() / 8;
		int max2 = 1024;
		int max = 0;
		if (max2 != 0) {
			max = Math.min(Math.min(maxW, maxH), max2);
		} else {
			max = Math.min(maxW, maxH);
		}
		if (width * width / scale >= max * max || height * height / scale > max * max) {
			result = Math.max(width * 1.0f / max, height * 1.0f / max);
			result = result * result;
		}
		return result;
	}

	/**
	 * 新策略用于图片预览 和 编辑的图片压缩
	 * @param width
	 * @param height
	 * @return
	 */
	public static int getFitSampleSizeNew(int width, int height) {
		int nw, nh;
		if(width * 1.0f / height >= SCREEN_WIDTH * 1.0f / SCREEN_HEIGHT){//宽顶着
			nw = SCREEN_WIDTH;
			nh = (int)(height * SCREEN_WIDTH * 1.0f / width + 0.5f);
		} else{
			nh = SCREEN_HEIGHT;
			nw = (int)(width * SCREEN_HEIGHT * 1.0f / height + 0.5f);
		}
		//nw nh是显示在屏幕上的像素宽高
		int size = width * height * 4;
		int minSize = nw * nh * 4;
//		int midSize = SCREEN_WIDTH * SCREEN_HEIGHT * 4;
		int result = 1;
		while((size / (result * result)) > minSize){
			result = result * 2;
		}
		if(result != 1){
			result /= 2;
//			while(result != 1 && (size * 1.0f / (result * result / 4)) < midSize ){
//				result /= 2;
//			}
		}
		return result;
	}

	public static int getFitSampleSize(int width, int height) {
		int size = width * height * 4;
		int result = 1;
		int value1, value2;
		value1 = 15;
		value2 = 60;
		if (size > mMaxMemory / value1) {
			float f = size * 1.0f / mMaxMemory * value1;
			f = (float) Math.sqrt(f * 4);
			result = (int) (f + 0.5f);
			int i = 1;
			double p = 2;
			while (result > p) {
				i++;
				p = Math.pow(2, i);
			}
			result = (int) p;
			float flag = (width * 1.0f / result) * (height * 1.0f / result) * 4;
			if (flag > mMaxMemory / value2) {
				return result;
			} else {
				return result / 2;
			}
		} else {
			result = 1;
		}
		return result;
	}

	/**
	 * 用于图片预览 和 编辑的图片压缩
	 * @param width
	 * @param height
	 * @param isHighQuality
	 * @return
	 */
	public static int getFitSampleSize(int width, int height, boolean isHighQuality) {
		int size = width * height * 4;
		int result = 1;
		int value1, value2;
		if(isHighQuality){
			value1 = 15;
			value2 = 60;
		} else{
			value1 = 18;
			value2 = 72;
		}
		if(size > mMaxMemory / value1){
			float f = size * 1.0f / mMaxMemory * value1;
			f = (float)Math.sqrt(f * 4);
			result = (int)(f + 0.5f);
			int i = 1;
			double p = 2;
			while(result > p){
				i++;
				p = Math.pow(2, i);
			}
			result = (int)p;
		} else if(size < mMaxMemory / value2){
			result = 1;
		} else{
			result = 2;
		}
		return result;
	}

	/**
	 * 旋转
	 * @param bitmap
	 * @param degree
	 * @return
	 */
	public static Bitmap rotating(Bitmap bitmap, float degree) {
		try {
			int width=bitmap.getWidth(),height=bitmap.getHeight();
			Matrix m=new Matrix();
			m.postRotate(degree, 0.5f, 0.5f);
			Bitmap result = Bitmap.createBitmap(bitmap, 0, 0, width, height, m, true);
			if(result != bitmap){
				bitmap.recycle();
			}
			return result;
		} catch (Throwable e) {
			System.gc();
			e.printStackTrace();
		}
		return null;
	}

	public static Bitmap rotating(Bitmap bitmap, float degree,
								  boolean flipHorizontal, boolean flipVertical) {
		try {
			int width = bitmap.getWidth(), height = bitmap.getHeight();
			Matrix m = new Matrix();
			if (degree != 0) {
				m.postRotate(degree, 0.5f, 0.5f);
			}
			if (flipHorizontal) {
				m.postScale(-1, 1);
			}
			if (flipVertical) {
				m.postScale(1, -1);
			}
			Bitmap result = Bitmap.createBitmap(bitmap, 0, 0, width, height, m, true);
			if (result != bitmap) {
				bitmap.recycle();
			}
			return result;
		} catch (Throwable e) {
			System.gc();
			e.printStackTrace();
		}
		return null;
	}

	private static boolean is720PResolution(){
		int modeSize = 1280 * 720;
		int size = SCREEN_WIDTH * SCREEN_HEIGHT;
		if(size >= modeSize * 0.7){
			return true;
		} else{
			return false;
		}
	}

	private static boolean is1080PResolution(){
		int modeSize = 1920 * 1080;
		int size = SCREEN_WIDTH * SCREEN_HEIGHT;
		if(size >= modeSize * 0.7){
			return true;
		} else{
			return false;
		}
	}

	private static boolean isHighMemory(){
		if(mMaxMemory >= 536870912){//512
			return true;
		} else{
			return false;
		}
	}
}
