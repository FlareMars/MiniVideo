package com.jb.zcamera.utils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import ly.kite.KiteSDK;
//import ly.kite.catalogue.Asset;
//import ly.kite.catalogue.AssetHelper;
//import ly.kite.journey.selection.ProductSelectionActivity;

/**
 * 放置一些通用的方法
 * @author ouyongqiang
 *
 */
public class ZCameraUtil {
	
	private static final String LOG_TAG = "ZCameraUtil";

	/**
	 * Returns whether the current device is running Android 4.4, KitKat, or newer
	 *
	 * KitKat is required for certain Kickflip features like Adaptive bitrate streaming
	 */
	public static boolean isKitKat() {
		return Build.VERSION.SDK_INT >= 19;
	}

	/**
	 * 系统版本是否高于5.0
	 *
	 * @return
	 */
	public static boolean isLollipop() {
		return Build.VERSION.SDK_INT >= 21;
	}

	public static <T> T checkNotNull(T reference) {
		if(reference == null) {
			throw new NullPointerException();
		} else {
			return reference;
		}
	}

}
