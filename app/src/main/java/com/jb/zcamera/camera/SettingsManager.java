package com.jb.zcamera.camera;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.gomo.minivideo.CameraApp;
import com.jb.zcamera.folder.ExtSdcardUtils;
import com.jb.zcamera.folder.FolderHelper;
import com.jb.zcamera.utils.PhoneInfo;

import java.io.File;

public class SettingsManager {

	public static final String preference_mirror_front_camera = "preference_mirror_front_camera";
	public static final String preference_shutter_sound = "preference_shutter_sound";
	public static final String preference_thumbnail_animation = "preference_thumbnail_animation";
	public static final String preference_gps_direction = "preference_gps_direction";
	public static final String preference_timer = "preference_timer";
	public static final String preference_save_location = "preference_save_location";
	public static final String preference_rotate_preview = "preference_rotate_preview";
	public static final String preference_preview_size = "preference_preview_size";
	public static final String preference_grid = "preference_grid";
	public static final String preference_quality = "preference_quality";
	public static final String preference_video_fps = "preference_video_fps";
	public static final String preference_video_bitrate = "preference_video_bitrate";
	public static final String preference_record_audio_src = "preference_record_audio_src";
	public static final String preference_touch_to_take_picture = "preference_touch_to_take_picture";
	public static final String preference_hdr_on = "preference_hdr_on";
	public static final String preference_flash_value = "preference_flash_value";
	public static final String preference_square = "preference_square";
	public static final String preference_rect = "preference_rect";

	public static final String preference_recently_video_file = "preference_recently_video_file";

	public static final String PRE_KEY_MAX_TEXTURE_SIZE = "max_texture_size";
	
	private static SharedPreferences getSharedPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(CameraApp.getApplication());
	}

	public static boolean getPreferenceMirrorFrontCamera() {
		SharedPreferences sharedPreferences = getSharedPreferences();
		return sharedPreferences.getBoolean(preference_mirror_front_camera, true);
	}

	public static boolean getPreferenceShutterSound() {
		SharedPreferences sharedPreferences = getSharedPreferences();
		return sharedPreferences.getBoolean(preference_shutter_sound, true);
	}
	
	public static boolean getPreferenceThumbnailAnimation() {
		SharedPreferences sharedPreferences = getSharedPreferences();
		return sharedPreferences.getBoolean(preference_thumbnail_animation, true);
	}
	
	public static boolean getPreferenceGpsDirection() {
		SharedPreferences sharedPreferences = getSharedPreferences();
		return sharedPreferences.getBoolean(preference_gps_direction, false);
	}

	public static String getPreferenceTimer() {
		SharedPreferences sharedPreferences = getSharedPreferences();
		return sharedPreferences.getString(preference_timer, "0");
	}

	/**
	 * default OpenCamera
	 * @param s
	 */
	public static void setPreferenceSaveLocation(String s) {
		SharedPreferences sharedPreferences = getSharedPreferences();
		sharedPreferences.edit().putString(preference_save_location, s).commit();
	}
	
	public static String getPreferenceSaveLocation() {
		SharedPreferences sharedPreferences = getSharedPreferences();
		String location = sharedPreferences.getString(preference_save_location, FolderHelper.DICM_ROOT_PATH + File.separator + "Camera");
		if(PhoneInfo.isSupportWriteExtSdCard() && ExtSdcardUtils.isExtSdcardPath(location)){//如果是5.0及以上  外置sd卡是可以读写的
			boolean hasPermission = ExtSdcardUtils.hasExtSdcardPermission();
			File f = new File(location);
			if(!hasPermission || !f.exists()){//没有权限
				location = FolderHelper.DICM_ROOT_PATH + File.separator + "Camera";
				setPreferenceSaveLocation(location);
				return location;
			}
		} else{
			File f = new File(location);
			if(!f.exists() || !f.canRead() || !f.canWrite()){//如果当前路径不存在 或者不能读  或者不能写 则设置为默认路径
				location = FolderHelper.DICM_ROOT_PATH + File.separator + "Camera";
				setPreferenceSaveLocation(location);
				return location;
			}
		}
		return location;
	}

	public static String getPreferenceRotatePreview() {
		SharedPreferences sharedPreferences = getSharedPreferences();
		return sharedPreferences.getString(preference_rotate_preview, "0");
	}

	public static String getPreferencePreviewSize() {
		SharedPreferences sharedPreferences = getSharedPreferences();
		return sharedPreferences.getString(preference_preview_size, "preference_preview_size_wysiwyg");
	}

	/**
	 * arrays.xml
	 * preference_grid_values
	 * @param s
	 */
	public static void setPreferenceGrid(String s) {
		SharedPreferences sharedPreferences = getSharedPreferences();
		sharedPreferences.edit().putString(preference_grid, s).commit();
	}
	
    public static String getPreferenceGrid() {
        SharedPreferences sharedPreferences = getSharedPreferences();
        return sharedPreferences.getString(preference_grid, "");
    }

	public static String getPreferenceQuality() {
		SharedPreferences sharedPreferences = getSharedPreferences();
		return sharedPreferences.getString(preference_quality, "100");
	}

	public static String getPreferenceVideoBitrate() {
		SharedPreferences sharedPreferences = getSharedPreferences();
		return sharedPreferences.getString(preference_video_bitrate, "default");
	}
	
	public static String getPreferenceVideoFps() {
		SharedPreferences sharedPreferences = getSharedPreferences();
		return sharedPreferences.getString(preference_video_fps, "default");
	}
	
	public static String getPreferenceRecordAudioSrc() {
		SharedPreferences sharedPreferences = getSharedPreferences();
		return sharedPreferences.getString(preference_record_audio_src, "audio_src_camcorder");
	}
	
	public static boolean getPreferenceTouchToTakePic() {
	    SharedPreferences sharedPreferences = getSharedPreferences();
        return sharedPreferences.getBoolean(preference_touch_to_take_picture, false);
	}
	
	public static boolean getPreferenceSquare() {
	    SharedPreferences sharedPreferences = getSharedPreferences();
        return sharedPreferences.getBoolean(preference_square, false);
	}

	public static boolean getPreferenceRect() {
		SharedPreferences sharedPreferences = getSharedPreferences();
		return sharedPreferences.getBoolean(preference_rect, false);
	}
	
	public static boolean getPreferenceHdrOn() {
	    SharedPreferences sharedPreferences = getSharedPreferences();
        return sharedPreferences.getBoolean(preference_hdr_on, false);
	}
	
	public static void setPreferenceHdrOn(boolean value) {
	    SharedPreferences sharedPreferences = getSharedPreferences();
        sharedPreferences.edit().putBoolean(preference_hdr_on, value).commit();
	}

    public static String getFlashValue() {
        SharedPreferences sharedPreferences = getSharedPreferences();
        return sharedPreferences.getString(preference_flash_value, "flash_off");
    }
    
    public static void setFlashValue(String value) {
        SharedPreferences sharedPreferences = getSharedPreferences();
        sharedPreferences.edit().putString(preference_flash_value, value).commit();
    }

	public static String getResolutionPreferenceKey(int cameraId) {
		return "camera_resolution_" + cameraId;
	}

	public static String getVideoQualityPreferenceKey(int cameraId) {
		return "video_quality_" + cameraId;
	}

	public static String getRecentlyVideoFile() {
		SharedPreferences sharedPreferences = getSharedPreferences();
		return sharedPreferences.getString(preference_recently_video_file, "");
	}


	public static void saveRecentlyVideoFile(String videoFile) {
		SharedPreferences sharedPreferences = getSharedPreferences();
		sharedPreferences.edit().putString(preference_recently_video_file, videoFile).apply();
	}
}
