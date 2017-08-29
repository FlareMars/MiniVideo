package com.jb.zcamera.camera;

import android.annotation.TargetApi;
import android.graphics.Rect;
import android.hardware.Camera;
import android.location.Location;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Pair;
import android.util.SparseArray;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.util.List;
import java.util.Vector;

public abstract class CameraController {

	private static final String TAG = CameraController.class.getSimpleName();

	// for testing:
	public int count_camera_parameters_exception = 0;

	public static class CameraFeatures {
		public boolean is_zoom_supported = false;
		public int max_zoom = 0;
		public List<Integer> zoom_ratios = null;
		public boolean supports_face_detection = false;
		public List<Size> picture_sizes = null;
		public List<Size> video_sizes = null;
		public List<Size> preview_sizes = null;
		public boolean has_current_fps_range = false;
		public int [] current_fps_range = new int[2];
		public List<String> supported_flash_values = null;
		public List<String> supported_focus_values = null;
		public List<String> supported_whiteblance_values = null;
		public List<String> supported_iso_values = null;
		public int max_num_focus_areas = 0;
		public boolean is_exposure_lock_supported = false;
		public boolean is_video_stabilization_supported = false;
		public int min_exposure = 0;
		public int max_exposure = 0;
		public float exposure_step = 0.0f;
		public boolean can_disable_shutter_sound = false;
	}

	static interface FaceDetectionListener {
		public abstract void onFaceDetection(Face[] faces);
	}

	static interface PictureCallback {
		public abstract void onPictureTaken(byte[] data);
	}

	static interface AutoFocusCallback {
		public abstract void onAutoFocus(boolean success);
	}

	static interface AutoFocusMoveCallback {
	    public abstract void onAutoFocusMoving(boolean start);
	}

	static class Face {
		public int score = 0;
		public Rect rect = null;

		Face(int score, Rect rect) {
			this.score = score;
			this.rect = rect;
		}
	}

	class SupportedValues {
		List<String> values = null;
		String selected_value = null;
		SupportedValues(List<String> values, String selected_value) {
			this.values = values;
			this.selected_value = selected_value;
		}
	}

	public String getDefaultSceneMode() {
		return "auto"; // chosen to match Camera.Parameters.SCENE_MODE_AUTO, but we also use compatible values for Camera2 API
	}
	public String getDefaultColorEffect() {
		return "none"; // chosen to match Camera.Parameters.EFFECT_NONE, but we also use compatible values for Camera2 API
	}
	public String getDefaultWhiteBalance() {
		return "auto"; // chosen to match Camera.Parameters.WHITE_BALANCE_AUTO, but we also use compatible values for Camera2 API
	}
	public String getDefaultISO() {
		return "auto";
	}

	abstract void release();

	abstract CameraFeatures getCameraFeatures();
	abstract SupportedValues setSceneMode(String value);
	public abstract String getSceneMode();
    public abstract Size getPictureSize();
    abstract void setPictureSize(int width, int height);
    public abstract Size getPreviewSize();
    abstract void setPreviewSize(int width, int height);
	abstract public int getZoom();
	abstract void setZoom(int value);
	abstract void setPreviewFpsRange(int min, int max);
	abstract void getPreviewFpsRange(int [] fps_range);
	abstract List<int []> getSupportedPreviewFpsRange();

	abstract void setFocusValue(String focus_value);
	abstract public String getFocusValue();
	abstract void setFlashValue(String flash_value);
	abstract public String getFlashValue();
	abstract void setRecordingHint(boolean hint);
	abstract void setRotation(int rotation);
	abstract void setLocationInfo(Location location);
	abstract void removeLocationInfo();
	abstract void enableShutterSound(boolean enabled);
	abstract boolean setFocusAndMeteringArea(List<Area> focusAreas, List<Area> meterAreas);
	public abstract List<Area> getFocusAreas();
	public abstract List<Area> getMeteringAreas();
	abstract void reconnect() throws IOException;
	abstract void setPreviewDisplay(SurfaceHolder holder) throws IOException;
	abstract void startPreview();
	abstract void stopPreview();
	public abstract boolean startFaceDetection();
	public abstract boolean stopFaceDetection();
	abstract void setFaceDetectionListener(final CameraController.FaceDetectionListener listener);
	abstract void autoFocus(final CameraController.AutoFocusCallback cb);

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	abstract void setAutoFocusMoveCallback(final CameraController.AutoFocusMoveCallback cb);

	abstract void cancelAutoFocus();
	abstract void takePicture(final CameraController.PictureCallback raw, final CameraController.PictureCallback jpeg, boolean shutterSound);
	abstract void setDisplayOrientation(int degrees);
	abstract int getDisplayOrientation();
	abstract int getCameraOrientation();
	abstract boolean isFrontFacing();
	abstract void unlock();
	abstract void initVideoRecorder(MediaRecorder video_recorder);
	abstract Camera getCamera();
	abstract boolean supportedHDR();
	abstract boolean isAutoFocus();

	public abstract boolean setWhiteBalance(String value);
	public abstract String getWhiteBalance();
	public abstract int getExposureCompensation();
	public abstract boolean setExposureCompensation(int new_exposure);
	public abstract List<String> getSupportedISO();
	public abstract boolean setISO(String value);
	public abstract String getISO();

	public static List<VideoQuality> initialiseVideoQuality(int cameraId, List<Size> video_sizes) {
		SparseArray<Pair<Integer, Integer>> profiles = new SparseArray<Pair<Integer, Integer>>();
		if( CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_HIGH) ) {
			CamcorderProfile profile = CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH);
			profiles.put(CamcorderProfile.QUALITY_HIGH, new Pair<Integer, Integer>(profile.videoFrameWidth, profile.videoFrameHeight));
		}
		if( CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_1080P) ) {
			CamcorderProfile profile = CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_1080P);
			profiles.put(CamcorderProfile.QUALITY_1080P, new Pair<Integer, Integer>(profile.videoFrameWidth, profile.videoFrameHeight));
		}
		if( CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_720P) ) {
			CamcorderProfile profile = CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_720P);
			profiles.put(CamcorderProfile.QUALITY_720P, new Pair<Integer, Integer>(profile.videoFrameWidth, profile.videoFrameHeight));
		}
		if( CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_480P) ) {
			CamcorderProfile profile = CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_480P);
			profiles.put(CamcorderProfile.QUALITY_480P, new Pair<Integer, Integer>(profile.videoFrameWidth, profile.videoFrameHeight));
		}
		if( CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_CIF) ) {
			CamcorderProfile profile = CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_CIF);
			profiles.put(CamcorderProfile.QUALITY_CIF, new Pair<Integer, Integer>(profile.videoFrameWidth, profile.videoFrameHeight));
		}
		if( CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_QVGA) ) {
			CamcorderProfile profile = CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_QVGA);
			profiles.put(CamcorderProfile.QUALITY_QVGA, new Pair<Integer, Integer>(profile.videoFrameWidth, profile.videoFrameHeight));
		}
		if( CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_QCIF) ) {
			CamcorderProfile profile = CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_QCIF);
			profiles.put(CamcorderProfile.QUALITY_QCIF, new Pair<Integer, Integer>(profile.videoFrameWidth, profile.videoFrameHeight));
		}
		if (CamcorderProfile.hasProfile(cameraId, CamcorderProfile.QUALITY_LOW) ) {
			CamcorderProfile profile = CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_LOW);
			profiles.put(CamcorderProfile.QUALITY_LOW, new Pair<Integer, Integer>(profile.videoFrameWidth, profile.videoFrameHeight));
		}
		return initialiseVideoQualityFromProfiles(profiles, video_sizes);
	}

	private static List<VideoQuality> initialiseVideoQualityFromProfiles(SparseArray<Pair<Integer, Integer>> profiles, List<Size> video_sizes) {
		List<VideoQuality> video_quality = new Vector<VideoQuality>();
		boolean done_video_size[] = null;
		if( video_sizes != null ) {
			done_video_size = new boolean[video_sizes.size()];
			for(int i=0;i<video_sizes.size();i++)
				done_video_size[i] = false;
		}
		if( profiles.get(CamcorderProfile.QUALITY_HIGH) != null ) {
			Pair<Integer, Integer> pair = profiles.get(CamcorderProfile.QUALITY_HIGH);
			addVideoResolutions(video_quality, video_sizes, done_video_size, CamcorderProfile.QUALITY_HIGH, pair.first, pair.second);
		}
		if( profiles.get(CamcorderProfile.QUALITY_1080P) != null ) {
			Pair<Integer, Integer> pair = profiles.get(CamcorderProfile.QUALITY_1080P);
			addVideoResolutions(video_quality, video_sizes, done_video_size, CamcorderProfile.QUALITY_1080P, pair.first, pair.second);
		}
		if( profiles.get(CamcorderProfile.QUALITY_720P) != null ) {
			Pair<Integer, Integer> pair = profiles.get(CamcorderProfile.QUALITY_720P);
			addVideoResolutions(video_quality, video_sizes, done_video_size, CamcorderProfile.QUALITY_720P, pair.first, pair.second);
		}
		if( profiles.get(CamcorderProfile.QUALITY_480P) != null ) {
			Pair<Integer, Integer> pair = profiles.get(CamcorderProfile.QUALITY_480P);
			addVideoResolutions(video_quality, video_sizes, done_video_size, CamcorderProfile.QUALITY_480P, pair.first, pair.second);
		}
		if( profiles.get(CamcorderProfile.QUALITY_CIF) != null ) {
			Pair<Integer, Integer> pair = profiles.get(CamcorderProfile.QUALITY_CIF);
			addVideoResolutions(video_quality, video_sizes, done_video_size, CamcorderProfile.QUALITY_CIF, pair.first, pair.second);
		}
		if( profiles.get(CamcorderProfile.QUALITY_QVGA) != null ) {
			Pair<Integer, Integer> pair = profiles.get(CamcorderProfile.QUALITY_QVGA);
			addVideoResolutions(video_quality, video_sizes, done_video_size, CamcorderProfile.QUALITY_QVGA, pair.first, pair.second);
		}
		if( profiles.get(CamcorderProfile.QUALITY_QCIF) != null ) {
			Pair<Integer, Integer> pair = profiles.get(CamcorderProfile.QUALITY_QCIF);
			addVideoResolutions(video_quality, video_sizes, done_video_size, CamcorderProfile.QUALITY_QCIF, pair.first, pair.second);
		}
		if( profiles.get(CamcorderProfile.QUALITY_LOW) != null ) {
			Pair<Integer, Integer> pair = profiles.get(CamcorderProfile.QUALITY_LOW);
			addVideoResolutions(video_quality, video_sizes, done_video_size, CamcorderProfile.QUALITY_LOW, pair.first, pair.second);
		}
		return video_quality;
	}

	private static void addVideoResolutions(List<VideoQuality> video_quality, List<Size> video_sizes, boolean done_video_size[], int base_profile, int min_resolution_w, int min_resolution_h) {
		if( video_sizes == null ) {
			return;
		}
		for(int i=0;i<video_sizes.size();i++) {
			if( done_video_size[i] )
				continue;
			Size size = video_sizes.get(i);
			if( size.width == min_resolution_w && size.height == min_resolution_h ) {
				String str = "" + base_profile;
				video_quality.add(new VideoQuality(str, size));
				done_video_size[i] = true;
			}
			else if( base_profile == CamcorderProfile.QUALITY_LOW || size.width * size.height >= min_resolution_w*min_resolution_h ) {
				String str = "" + base_profile + "_r" + size.width + "x" + size.height;
				video_quality.add(new VideoQuality(str, size));
				done_video_size[i] = true;
			}
		}
	}
}
