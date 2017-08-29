package com.jb.zcamera.imagefilter.java;


public class BEEPSGain/* implements Runnable*/

{
//	private double[] data;
//	private int length;
//	private int startIndex;
	private static float mu;

//	protected BEEPSGain(final double[] data, final int startIndex,
//			final int length) {
//		this.data = data;
//		this.startIndex = startIndex;
//		this.length = length;
//	}

	protected static void setup(final float spatialContraDecay) {
		mu = (1.0f - spatialContraDecay) / (1.0f + spatialContraDecay);
	}

//	public void run() {
//		for (int k = startIndex, K = startIndex + length; (k < K); k++) {
//			data[k] *= mu;
//		}
//	}

	public static void run(final int[] data, final int startIndex,
			final int length) {
		for (int k = startIndex, K = startIndex + length; (k < K); k++) {
			data[k] *= mu;
		}
	}
}