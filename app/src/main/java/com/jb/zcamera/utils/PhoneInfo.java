package com.jb.zcamera.utils;

import android.hardware.Camera;
import android.os.Build;

/**
 * 机型判断接口
 * 
 * @author oujingwen
 *
 */
public class PhoneInfo {

	public static boolean isBadPictureSize(Camera.Size size) {
		if (!"SCH-I435".equals(android.os.Build.MODEL) || size == null) {
			return false;
		}
		if (size.width == 1920 && size.height == 1080) {
			return true;
		}
		return false;
	}

	public static boolean isBadVideoSize(Camera.Size size, boolean frontFacing) {
		// 不支持2K分辨率
		if (/*(isMeizu() || isMoto()) && */size.width * size.height >= 2560 * 1440) {
			return true;
		}
		if ("SM-N910F".equals(Build.MODEL) && frontFacing) {
			if (Math.abs(size.width / (float)size.height - 1.7f) > 0.1f) {
				return true;
			}
		}
		if ("MOT-XT788".equals(Build.MODEL) && size.width == 1280 && size.height == 720) {
			return true;
		}
		return false;
	}

	public static boolean isNotSupportVideoRender() {
//		Loger.d("Test", Build.MODEL +  " " + Build.BRAND);
		if ("GT-I9300".equals(Build.MODEL)
				|| "HUAWEI C8950D".equals(Build.MODEL)
				|| Build.MODEL.contains("HTC Sensation XE")
				|| "C2305".equals(Build.MODEL)
				|| "GT-S7270".equals(Build.MODEL)
				|| "GT-S7275".equals(Build.MODEL)
				|| "GT-S7272".equals(Build.MODEL)
				|| "SM-T210".equals(Build.MODEL)
				|| "ST26a".equals(Build.MODEL)
				|| Build.MODEL.toLowerCase().contains("htc desire 310")
				|| ("Coolpad".equals(Build.BRAND) && "9900".equals(Build.MODEL))
				|| "XT910".equals(Build.MODEL)
				|| Build.MODEL.toLowerCase().contains("vs840")
				|| "SCH-I435".equals(Build.MODEL)
				|| ("SM-N910F".equals(Build.MODEL))) {
			return true;
		}
		return false;
	}

	public static boolean isSupportWriteExtSdCard(){
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
	}

	/**
	 * 华为 MT7不支持从mount读取外置SDCARD的路径
	 * @return
	 */
	public static boolean isNotSupportReadExtSdcardPathFromMount(){
		return (isHuawei() && Build.MODEL.toLowerCase().contains("mt7")) || (isSamsung() && Build.MODEL.contains("SM-G930P"));
	}

	/**
	 * 是否是华为机型
	 *
	 * @return
	 */
	public static boolean isHuawei() {
		return Build.BRAND.toLowerCase().contains("huawei");
	}

	/**
	 * 是否是三星机型
	 *
	 * @return
	 */
	public static boolean isSamsung() {
		return "samsung".equalsIgnoreCase(android.os.Build.BRAND);
	}

	/**
	 * 是否是索尼机型
	 *
	 * @return
	 */
	public static boolean isSony() {
		return Build.BRAND.equalsIgnoreCase("SEMC") || Build.BRAND.equalsIgnoreCase("Sony");
	}

	public static boolean isNotSupportOES() {
		return
				"Galaxy Nexus".equals(Build.MODEL)
						|| (isSony() && Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2)
						|| "GT-I8552".equals(Build.MODEL)
						|| "SM-T110".equals(Build.MODEL)
						|| "SM-T211".equals(Build.MODEL)
//						|| "GT-I9502".equals(Build.MODEL)
						|| "XT910".equals(Build.MODEL)
						|| "SM-G3818".equals(Build.MODEL);
	}

	/**
	 * 是否是魅族机型
	 *
	 * @return
	 */
	public static boolean isMeizu() {
		return "Meizu".equals(Build.BRAND);
	}

	/**
	 * 是否停止录像需要重启摄像头机型
	 *
	 * @return
	 */
	public static boolean isVideoIssueDevice() {
		return ("SM-N9108V".equals(Build.MODEL) || "SM-N910F".equals(Build.MODEL));
	}

	public static boolean hasJellyBeanMR2() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
	}

	public static boolean isSupportVideoFilter() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2
				|| isNotSupportVideoRender() || isMeizu()) {
			return false;
		}
		return true;
	}
}
