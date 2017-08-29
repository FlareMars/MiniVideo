package com.jb.zcamera.imagefilter.java;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;

@SuppressLint("NewApi")
public class BEEPS_2 {
	private static float spatialDecay = 0.02f;
	private static int photometricStandardDeviation = 8;

	 public static Bitmap run (Bitmap ip) {
	
		 final int width = ip.getWidth();
		 final int height = ip.getHeight();
		 int[] data = new int[width*height];
		 int[] rData = new int[width*height];
		 int[] gData = new int[width*height];
		 int[] bData = new int[width*height];
		 
		 ip.getPixels(data, 0, width, 0, 0, width, height);
		 for(int i = 0; i<height ; i++){
			 for(int j = 0 ; j< width ;j++){
				 rData[i * width + j] = (data[i * width + j] >> 16) & 0xff;
				 gData[i * width + j] = (data[i * width + j] >> 8) & 0xff;
				 bData[i * width + j] = data[i * width + j] & 0xff;
			 }
		 }
//		 float[] duplicate = (Arrays.copyOf(rData, rData.length));
//		 Thread h = new Thread(new BEEPSHorizontalVertical(duplicate, width, height, photometricStandardDeviation, spatialDecay));
		 Thread v = new Thread(new BEEPSVerticalHorizontal(rData, width, height, photometricStandardDeviation, spatialDecay));
		 
//		 float[] duplicate1 = (Arrays.copyOf(gData, gData.length));
//		 Thread h1 = new Thread(new BEEPSHorizontalVertical(duplicate1, width, height, photometricStandardDeviation, spatialDecay));
		 Thread v1 = new Thread(new BEEPSVerticalHorizontal(gData, width, height, photometricStandardDeviation, spatialDecay));
		 
//		 float[] duplicate2 = (Arrays.copyOf(bData, bData.length));
//		 Thread h2 = new Thread(new BEEPSHorizontalVertical(duplicate2, width, height, photometricStandardDeviation, spatialDecay));
		 Thread v2 = new Thread(new BEEPSVerticalHorizontal(bData, width, height, photometricStandardDeviation, spatialDecay));
		 
//		 h.start();
		 v.start();
//		 h1.start();
		 v1.start();
//		 h2.start();
		 v2.start();
		 try {
//			 h.join();
			 v.join();
//			 h1.join();
			 v1.join();
//			 h2.join();
			 v2.join();
		 }
		 catch (InterruptedException e) {
		 }
		 for (int k = 0, K = data.length; (k < K); k++) {
//			 data[k] = 0xff000000 | (int)(0.5F * (rData[k] + duplicate[k])) << 16  | (int)(0.5F * (gData[k] + duplicate1[k])) << 8 | (int)(0.5F * (bData[k] + duplicate2[k]));
			 data[k] = 0xff000000 | (int)(rData[k]) << 16  | (int)(gData[k]) << 8 | (int)(bData[k]);
		 }
		 return Bitmap.createBitmap(data, width, height, Config.ARGB_8888);
	 }

}




