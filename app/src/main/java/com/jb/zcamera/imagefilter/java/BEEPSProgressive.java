package com.jb.zcamera.imagefilter.java;

import static java.lang.Math.exp;

public class BEEPSProgressive/* implements Runnable*/{
//	private double[] data;
//	private int length;
//	private int startIndex;
	private static float c;
	private static float rho;
	private static float spatialContraDecay;

//	protected BEEPSProgressive(final double[] data, final int startIndex,
//			final int length) {
//		this.data = data;
//		this.startIndex = startIndex;
//		this.length = length;
//	}

	protected static void setup(
			final float photometricStandardDeviation,
			final float sharedSpatialContraDecay) {
		spatialContraDecay = sharedSpatialContraDecay;
		rho = 1.0f + spatialContraDecay;
		c = -0.5f
				/ (photometricStandardDeviation * photometricStandardDeviation);
	}

//	public void run() {
//		double mu = 0.0;
//		data[startIndex] /= rho;
//		for (int k = startIndex + 1, K = startIndex + length; (k < K); k++) {
//			mu = data[k] - rho * data[k - 1];
//			mu = spatialContraDecay * exp(c * mu * mu);
//			data[k] = data[k - 1] * mu + data[k] * (1.0 - mu) / rho;
//		}
//	}
	
	public static void run(final int[] data, final int startIndex,
			final int length) {
		float mu = 0.0f;
		data[startIndex] /= rho;
		for (int k = startIndex + 1, K = startIndex + length; (k < K); k++) {
			mu = data[k] - rho * data[k - 1];
			mu = (float)(spatialContraDecay * exp(c * mu * mu));
			data[k] = (int)(data[k - 1] * mu + data[k] * (1.0f - mu) / rho);
		}
	}
}