package com.jb.zcamera.imagefilter.java;

import java.util.Arrays;

public class BEEPSVerticalHorizontal implements Runnable {
	private float photometricStandardDeviation;
	private float spatialDecay;
	private int[] data;
	private int height;
	private int width;

	protected BEEPSVerticalHorizontal(final int[] data, final int width,
			final int height,
			final int photometricStandardDeviation, final float spatialDecay) {
		this.data = data;
		this.width = width;
		this.height = height;
		this.photometricStandardDeviation = photometricStandardDeviation;
		this.spatialDecay = spatialDecay;
	}

	public void run() {
		BEEPSProgressive.setup(photometricStandardDeviation,
				1.0f - spatialDecay);
		BEEPSGain.setup(1.0f - spatialDecay);
		BEEPSRegressive.setup(photometricStandardDeviation,
				1.0f - spatialDecay);
		int[] g = new int[height * width];
		int m = 0;
		for (int k2 = 0; (k2 < height); k2++) {//data是一行一行的 从左到右  通过这个转变后g变成一列一列 从上到下
			int n = k2;
			for (int k1 = 0; (k1 < width); k1++) {
				g[n] = data[m++];
				n += height;
			}
		}
//		ExecutorService verticalExecutor = Executors.newFixedThreadPool(Runtime
//				.getRuntime().availableProcessors());
		int[] p = Arrays.copyOf(g, height * width);
		int[] r = Arrays.copyOf(g, height * width);
		for (int k1 = 0; (k1 < width); k1++) {
//			Runnable progressive = new BEEPSProgressive(p, k1 * height, height);
//			Runnable gain = new BEEPSGain(g, k1 * height, height);
//			Runnable regressive = new BEEPSRegressive(r, k1 * height, height);
//			verticalExecutor.execute(progressive);
//			verticalExecutor.execute(gain);
//			verticalExecutor.execute(regressive);
			
			BEEPSProgressive.run(p, k1 * height, height);
			BEEPSGain.run(g, k1 * height, height);
			BEEPSRegressive.run(r, k1 * height, height);
		}
//		try {
//			verticalExecutor.shutdown();
//			verticalExecutor.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);
//		} catch (InterruptedException ignored) {
//		}
		for (int k = 0, K = data.length; (k < K); k++) {
			r[k] += p[k] - g[k];
		}
		m = 0;
		for (int k1 = 0; (k1 < width); k1++) {//r是一列一列 从上到下  通过这个转变后g变成一行一行的 从左到右
			int n = k1;
			for (int k2 = 0; (k2 < height); k2++) {
				g[n] = r[m++];
				n += width;
			}
		}
//		ExecutorService horizontalExecutor = Executors
//				.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		p = Arrays.copyOf(g, width * height);
		r = Arrays.copyOf(g, width * height);
		for (int k2 = 0; (k2 < height); k2++) {
//			Runnable progressive = new BEEPSProgressive(p, k2 * width, width);
//			Runnable gain = new BEEPSGain(g, k2 * width, width);
//			Runnable regressive = new BEEPSRegressive(r, k2 * width, width);
//			horizontalExecutor.execute(progressive);
//			horizontalExecutor.execute(gain);
//			horizontalExecutor.execute(regressive);
			
			BEEPSProgressive.run(p, k2 * width, width);
			BEEPSGain.run(g, k2 * width, width);
			BEEPSRegressive.run(r, k2 * width, width);
		}
//		try {
//			horizontalExecutor.shutdown();
//			horizontalExecutor.awaitTermination(Integer.MAX_VALUE,
//					TimeUnit.DAYS);
//		} catch (InterruptedException ignored) {
//		}
		for (int k = 0, K = data.length; (k < K); k++) {
			data[k] = (int) (p[k] - g[k] + r[k]);
		}
	}

}
