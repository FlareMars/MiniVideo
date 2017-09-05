package com.jb.zcamera.camera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.CamcorderProfile;
import android.media.ExifInterface;
import android.media.MediaRecorder;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import com.gomo.minivideo.CameraApp;
import com.gomo.minivideo.R;
import com.gomo.minivideo.camera.CameraFragment;
import com.jb.zcamera.exif.Exif;
import com.jb.zcamera.exif.ExifTag;
import com.jb.zcamera.filterstore.bo.LocalFilterBO;
import com.jb.zcamera.folder.ExtSdcardUtils;
import com.jb.zcamera.folder.FolderHelper;
import com.jb.zcamera.gallery.util.FolderTools;
import com.jb.zcamera.image.ImageHelper;
import com.jb.zcamera.imagefilter.FiltFrameListener;
import com.jb.zcamera.imagefilter.GPUImage;
import com.jb.zcamera.imagefilter.filter.GPUImageBeautyFilter;
import com.jb.zcamera.imagefilter.filter.GPUImageFilter;
import com.jb.zcamera.imagefilter.filter.GPUImageFilterGroup;
import com.jb.zcamera.imagefilter.filter.GPUImageHDRFilter;
import com.jb.zcamera.imagefilter.filter.GPUImageHDROESFilter;
import com.jb.zcamera.imagefilter.filter.GPUImageNormalBlendFilter;
import com.jb.zcamera.imagefilter.filter.GPUImageOESFilter;
import com.jb.zcamera.imagefilter.util.ImageFilterTools;
import com.jb.zcamera.utils.BitmapUtils;
import com.jb.zcamera.utils.DateMaskUtil;
import com.jb.zcamera.utils.PhoneInfo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class Preview implements SurfaceHolder.Callback {
	private static final String TAG = "Preview";

	public static final int MESSAGE_SHOW_ZOOMLAYOUT = 3;

	private static final String TAG_GPS_IMG_DIRECTION = "GPSImgDirection";
	private static final String TAG_GPS_IMG_DIRECTION_REF = "GPSImgDirectionRef";
	
	// 滤镜模式下最高分辨率
	private static final long FILT_MODE_SIZE_LIMIT = 2 * 1000 * 1000;

	private static final int TARGET_MOTION_SIZE = 960 * 720;

	public static final int MODE_VIDEO = 0;
	public static final int MODE_PHOTO = 1;
	public static final int MODE_BEAUTY = 2;
	public static final int MODE_MOTION = 3;

	private static final int MAX_MOTION_TIME = 10000;
	private static final int MIN_MOTION_TIME = 1000;
	
	private CameraFragment activity;
	private SurfaceView surfaceView = null;
	private GLSurfaceView glSurfaceView = null;
	private SurfaceHolder mHolder = null;

    private Matrix camera_to_preview_matrix = new Matrix();
    private Matrix preview_to_camera_matrix = new Matrix();

	private boolean app_is_stoped = true;
	private boolean has_surface = false;
	private boolean has_aspect_ratio = false;
	private double aspect_ratio = 0.0f;
	private CameraControllerManager camera_controller_manager = null;
	private CameraController camera_controller = null;
	private int cameraId = 0;
	private int previousCameraId = 0;
	private MediaRecorder video_recorder = null;
	private boolean video_start_time_set = false;
	private long video_start_time = 0;
	private String video_name = null;
	private boolean has_current_fps_range = false;
	private int [] current_fps_range = new int[2];
	private ArrayList<File> filesList = new ArrayList<File>();
	private long savedRecordTime;

	private final Object PHASE_LOCK = new Object();
	private final int PHASE_NORMAL = 0;
	private final int PHASE_TIMER = 1;
	private final int PHASE_TAKING_PHOTO = 2;
	private final int PHASE_PREVIEW_PAUSED = 3; // the paused state after taking a photo
	private final int PHASE_PAUSEING_VIDEO = 4;
	private int phase = PHASE_NORMAL;
	private Timer takePictureTimer = new Timer();
	private TimerTask takePictureTimerTask = null;
	private long take_photo_time = 0;

	private boolean is_preview_started = false;
	//private boolean is_preview_paused = false; // whether we are in the paused state after taking a photo
	private int current_orientation = 0; // orientation received by onOrientationChanged
	private int capture_rotation = 0;
	private boolean has_level_angle = false;
	private double level_angle = 0.0f;
	private double orig_level_angle = 0.0f;

	private boolean has_zoom = false;
	private int zoom_factor = 0;
	private int max_zoom_factor = 0;
	private ScaleGestureDetector scaleGestureDetector;
	private List<Integer> zoom_ratios = null;
	private boolean touch_was_multitouch = false;

	private List<String> supported_flash_values = null; // our "values" format
	private String mFlashValue = "flash_off";

	private int max_num_focus_areas = 0;
	
	private List<String> scene_modes = null;

	private List<String> supported_white_balances = null;
	private List<String> supported_isos = null;

	private int mParamCameraId = -1;
	private String mWhiteBalanceValue;
	private String mIsoValue;
	private int mExposuresValue;

	private List<String> supported_ev_values = null;
	private String mEvValue;
	private int min_exposure = 0;
	private int max_exposure = 0;
	private float exposure_step = 0.0f;

	private List<Size> supported_preview_sizes = null;
	private Size mPreviewSize = null;

	private List<Size> sizes = null;
	private int current_size_index = -1; // this is an index into the sizes array, or -1 if sizes not yet set

	// video_quality can either be:
	// - an int, in which case it refers to a CamcorderProfile
	// - of the form [CamcorderProfile]_r[width]x[height] - we use the CamcorderProfile as a base, and override the video resolution - this is needed to support resolutions which don't have corresponding camcorder profiles
	private List<VideoQuality> video_quality = null;
	private int current_video_quality = -1; // this is an index into the video_quality array, or -1 if not found (though this shouldn't happen?)
	private List<Size> video_sizes = null;
	private int current_motion_quality = -1;

	private int ui_rotation = 0;

	private boolean supports_face_detection = false;
	private boolean supports_video_stabilization = false;
	private boolean can_disable_shutter_sound = false;
	
	private FocusHelper mFocusHelper;

	// accelerometer and geomagnetic sensor info
	private final float sensor_alpha = 0.8f; // for filter
    private boolean has_gravity = false;
    private float [] gravity = new float[3];
    private boolean has_geomagnetic = false;
    private float [] geomagnetic = new float[3];
    private float [] deviceRotation = new float[9];
    private float [] cameraRotation = new float[9];
    private float [] deviceInclination = new float[9];
    private boolean has_geo_direction = false;
    private float [] geo_direction = new float[3];

    // for testing:
	public int count_cameraStartPreview = 0;
	public int count_cameraAutoFocus = 0;
	public int count_cameraTakePicture = 0;
	public boolean test_fail_open_camera = false;
	public String test_last_saved_image = null;
	
	public GPUImage gpuImage;
	
	private ViewGroup viewGroup;

	private FocusOverlay mFocusOverlay;

	private int mFilterId = -1;

	private int mMode;
	
	private SoundManager mSoundManager;
	
	private boolean mKeyPressed;

	private boolean mMotionPressedInside;
	
	//用于实现拍照后的边框
	private boolean mCanDrawPhotoFrame = false;
	
	private float mCanDrawPhotoFrameWidth = 0;
	
	private float DrawPhotoFrameWidthMax;
	
	private float mSingleDp;

    private CameraController.PictureCallback jpegPictureCallback = new CameraController.PictureCallback() {
        public void onPictureTaken(final byte[] data) {

        }
    };
    
    private FolderHelper.OnScanCompletedListener onScanCompletedListener = new FolderHelper.OnScanCompletedListener() {
        
        @Override
        public void onScanCompleted(String path, Uri uri, int orientation) {

        }
    };
    
    private FocusDistanceChecker mFocusDistanceChecker;
    private FocusDistanceChecker.DistanceCheckerListener mDistanceCheckerListener = new FocusDistanceChecker.DistanceCheckerListener() {
        
        @Override
        public boolean preCheck() {
            return canAutoFocus() && !isTakeButtonPressed() && !gpuImage.hasEffect();
        }
        
        @Override
        public void onDistanceChanged() {
            synchronized (Preview.this) {
                if (canAutoFocus()) {
                    clearFocusAreas();
                    tryAutoFocus(false, false);
                }
            }
        }
    };
    
    private CameraController.AutoFocusCallback autoFocusCallback = new CameraController.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success) {
            mFocusDistanceChecker.updateLastSensorValues();
            autoFocusCompleted(false, success, false);
        }
    };
    
    private Handler mHandler;
	private static final int MSG_WHAT_CLEAR_AND_CHANGE_SURFACEVIEW = 1;
    private static final int HANDLER_WHAT_UPDATE_RECORD_TIME = 2;
    private static final int HANDLER_WHAT_UPDATE_DELAY_REMAINING_TIME = 3;
	private static final int HANDLER_WHAT_UPDATE_MOTION_PROGRESS = 4;
    private Handler.Callback mHandlerCallback = new Handler.Callback() {
        
        @Override
        public boolean handleMessage(Message msg) {
			if (msg.what == MSG_WHAT_CLEAR_AND_CHANGE_SURFACEVIEW) {
				changeSurfaceMode();
				startOverlayGone();
			} else if (msg.what == HANDLER_WHAT_UPDATE_RECORD_TIME) {
                if (isVideo()) {
                    long time = getVideoRecordTime();
					activity.updateRecordTime(time);
                    if (isTakingPhoto()) {
						mHandler.sendEmptyMessageDelayed(HANDLER_WHAT_UPDATE_RECORD_TIME, 1000);
                    }
                }
            } else if (msg.what == HANDLER_WHAT_UPDATE_DELAY_REMAINING_TIME) {
                if (isOnTimer()) {
                    activity.updateDelayRemainingTime(getDelayRemainingTime());
                    mHandler.sendEmptyMessageDelayed(HANDLER_WHAT_UPDATE_DELAY_REMAINING_TIME, 1000);
                }
            }
            return false;
        }
    };
    
    private Handler mAsyncHandler;
    private static final int ASYNC_MSG_WHAT_SWITCH_TO_PHOTO = 0;
    private static final int ASYNC_MSG_WHAT_SWITCH_TO_VIDEO = 1;
    private static final int ASYNC_MSG_WHAT_TAKE_CLICK = 2;
    private static final int ASYNC_MSG_WHAT_SWITCH_CAMERA = 3;
    private static final int ASYNC_MSG_WHAT_RESTAT_CAMERA = 4;
    private static final int ASYNC_MSG_WHAT_CYCLEFLASH = 5;
    private static final int ASYNC_MSG_WHAT_TOUCH = 6;
    private static final int ASYNC_MSG_WHAT_OPEN_CAMERA = 7;
    private static final int ASYNC_MSG_WHAT_UPDATE_FILTER = 8;
    private static final int ASYNC_MSG_WHAT_SWITCH_TO_BEAUTY = 9;
	private static final int ASYNC_MSG_WHAT_CLOSE_CAMERA = 10;
	private static final int ASYNC_MSG_WHAT_PAUSE_VIDEO = 11;
	private static final int ASYNC_MSG_WHAT_SWITCH_TO_MOTION = 12;
	private static final int ASYNC_MSG_WHAT_STOP_VIDEO = 13;
	private static final int ASYNC_MSG_WHAT_CLOSE_CAMERA_AND_OPEN_SETTING = 14;
	private static final int ASYNC_MSG_WHAT_OPEN_PIP_ACTIVITY = 15;
	private static final int ASYNC_MSG_WHAT_CLOSE_CAMERA_AND_OPEN_PIP_ACTIVITY = 18;
    private Handler.Callback mAsyncHandlerCallback = new Handler.Callback() {
        
        @Override
		public boolean handleMessage(Message msg) {
            if (msg.what == ASYNC_MSG_WHAT_SWITCH_TO_PHOTO) {
                switchToPhoto();
            } else if (msg.what == ASYNC_MSG_WHAT_SWITCH_TO_VIDEO) {
                switchToVideo();
            } else if (msg.what == ASYNC_MSG_WHAT_TAKE_CLICK) {
                takePicturePressed();
            } else if (msg.what == ASYNC_MSG_WHAT_SWITCH_CAMERA) {
                switchCamera(true, true, true);
            } else if (msg.what == ASYNC_MSG_WHAT_RESTAT_CAMERA) {
                closeCamera();
                openCamera(false);
            } else if (msg.what == ASYNC_MSG_WHAT_CYCLEFLASH) {
                cycleFlash();
            } else if (msg.what == ASYNC_MSG_WHAT_TOUCH) {
                processTouch(msg.arg1, msg.arg2);
            } else if (msg.what == ASYNC_MSG_WHAT_OPEN_CAMERA) {
				synchronized (Preview.this) {
					openCamera(false);
					if (!isFiltMode()) {
						if (camera_controller != null &&
								!isVideoOrMotion() && isHDROn() && !supportHDR() && !isFrontCamera()) {
							updateFilter();
						}
					}
				}
            } else if (msg.what == ASYNC_MSG_WHAT_UPDATE_FILTER) {
                updateFilter();
            } else if (msg.what == ASYNC_MSG_WHAT_SWITCH_TO_BEAUTY) {
                switchToBeauty();
            } else if (msg.what == ASYNC_MSG_WHAT_CLOSE_CAMERA) {
				closeCamera();
			} else if (msg.what == ASYNC_MSG_WHAT_PAUSE_VIDEO) {
				pauseOrResumeVideo();
			} else if (msg.what == ASYNC_MSG_WHAT_SWITCH_TO_MOTION) {
				switchToMotion();
			} else if (msg.what == ASYNC_MSG_WHAT_STOP_VIDEO) {
				stopVideo();
			}
            return false;
        }
    };
    
    private boolean captureFiltFrame = false;
    private boolean filtFrameProcessing = false;
    private FiltFrameListener filtFrameListener = new FiltFrameListener() {
        
        @Override
        public void onFiltFrameDraw(final Bitmap bitmap) {
            captureFiltFrame = false;
            filtFrameProcessing = true;
			if (!(isVideoRecording() || isVideoPausing())) {
				phase = PHASE_NORMAL; // need to set this even if remaining burst photos, so we can restart the preview
				mFocusHelper.clearFocusState(); // clear focus rectangle if not already done
			}
            mAsyncHandler.post(new Runnable() {
                
                @Override
                public void run() {
					boolean isExtSdcard = FolderHelper.isExtSdcardImagePath();
					if (isExtSdcard) {
						activity.showLoadingDialog();
					}
                    processPictureTaken(null, bitmap, mMode);
					if (isExtSdcard) {
						activity.hideLoadingDialog();
					}
                    filtFrameProcessing = false;
                }
            });
        }
        
        @Override
        public boolean needCallback() {
            return captureFiltFrame && !filtFrameProcessing;
        }
    };

	@SuppressWarnings("deprecation")
	public Preview(CameraFragment cameraFragment, Bundle savedInstanceState, ViewGroup viewGroup, int mode) {
		this.activity = cameraFragment;
		Context context = activity.getActivity();
		this.viewGroup = viewGroup;
		this.mMode = mode;
		if (PhoneInfo.isNotSupportVideoRender()) {
			this.surfaceView = new MySurfaceView(context, savedInstanceState, this);
		}
		this.glSurfaceView = new MyGLSurfaceView(context, savedInstanceState, this);
		camera_controller_manager = new CameraControllerManager1();
		gpuImage = new GPUImage(getContext(), true);
		gpuImage.setFilter(getDefalutFilter(), true);
		gpuImage.setGLSurfaceView(glSurfaceView);
		gpuImage.setFiltFrameListener(filtFrameListener);
		glSurfaceView.getHolder().addCallback(this);

		mFocusOverlay = new FocusOverlay(context, this);

		this.mHandler = new Handler(context.getMainLooper(), mHandlerCallback);
		mFocusHelper = new FocusHelper(context, mFocusOverlay);
		
	    scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
	    
	    mAsyncHandler = new Handler(CameraAyncThreadManager.getInstance().getLooper(), mAsyncHandlerCallback);;

	    mFocusDistanceChecker = new FocusDistanceChecker(context, mDistanceCheckerListener);

        mSoundManager = new SoundManager(getContext());

        if( savedInstanceState != null ) {
    		cameraId = savedInstanceState.getInt("cameraId", 0);
    		if( cameraId < 0 || cameraId >= camera_controller_manager.getNumberOfCameras() ) {
    			cameraId = 0;
    		}
    		zoom_factor = savedInstanceState.getInt("zoom_factor", 0);
        }
	}
	
	/*private void previewToCamera(float [] coords) {
		float alpha = coords[0] / (float)this.getWidth();
		float beta = coords[1] / (float)this.getHeight();
		coords[0] = 2000.0f * alpha - 1000.0f;
		coords[1] = 2000.0f * beta - 1000.0f;
	}*/

	/*private void cameraToPreview(float [] coords) {
		float alpha = (coords[0] + 1000.0f) / 2000.0f;
		float beta = (coords[1] + 1000.0f) / 2000.0f;
		coords[0] = alpha * (float)this.getWidth();
		coords[1] = beta * (float)this.getHeight();
	}*/

	private Resources getResources() {
		return activity.getResources();
	}
	
	private synchronized void calculateCameraToPreviewMatrix() {
		if( camera_controller == null )
			return;
		camera_to_preview_matrix.reset();
		// from http://developer.android.com/reference/android/hardware/Camera.Face.html#rect
		// Need mirror for front camera
		boolean mirror = camera_controller.isFrontFacing();
		camera_to_preview_matrix.setScale(mirror ? -1 : 1, 1);
		// This is the value for android.hardware.Camera.setDisplayOrientation.
		camera_to_preview_matrix.postRotate(camera_controller.getDisplayOrientation());
		// Camera driver coordinates range from (-1000, -1000) to (1000, 1000).
		// UI coordinates range from (0, 0) to (width, height).
		int width = getSurfaceView().getWidth();
		int height = getSurfaceView().getHeight();
		camera_to_preview_matrix.postScale(width / 2000f, height / 2000f);
		camera_to_preview_matrix.postTranslate(width / 2f, height / 2f);
	}

	private synchronized void calculatePreviewToCameraMatrix() {
		if( camera_controller == null )
			return;
		calculateCameraToPreviewMatrix();
		if( !camera_to_preview_matrix.invert(preview_to_camera_matrix) ) {
		}
	}

	private ArrayList<Area> getAreas2(float x, float y) {
		float [] coords = {x, y};
		calculatePreviewToCameraMatrix();
		preview_to_camera_matrix.mapPoints(coords);
		float focus_x = coords[0];
		float focus_y = coords[1];

		int focus_size = 50;
		Rect rect = new Rect();
		rect.left = (int)focus_x - focus_size;
		rect.right = (int)focus_x + focus_size;
		rect.top = (int)focus_y - focus_size;
		rect.bottom = (int)focus_y + focus_size;
		if( rect.left < -1000 ) {
			rect.left = -1000;
			rect.right = rect.left + 2*focus_size;
		}
		else if( rect.right > 1000 ) {
			rect.right = 1000;
			rect.left = rect.right - 2*focus_size;
		}
		if( rect.top < -1000 ) {
			rect.top = -1000;
			rect.bottom = rect.top + 2*focus_size;
		}
		else if( rect.bottom > 1000 ) {
			rect.bottom = 1000;
			rect.top = rect.bottom - 2*focus_size;
		}

	    ArrayList<Area> areas = new ArrayList<Area>();
	    areas.add(new Area(rect, 1000));
	    return areas;
	}

    private ArrayList<Area> getAreas(float x, float y, float areaMultiple) {
        calculatePreviewToCameraMatrix();

        Rect rect = TapAreaUtil.calculateTapArea((int)x, (int)y, areaMultiple, getSurfaceView().getWidth(),
                getSurfaceView().getHeight(), preview_to_camera_matrix);

        ArrayList<Area> areas = new ArrayList<Area>();
        areas.add(new Area(rect, 1));
        return areas;
    }
	
	public boolean touchEvent(MotionEvent event) {
		gpuImage.onTouchEvent(event);
		if (!gpuImage.isPressed()) {
			scaleGestureDetector.onTouchEvent(event);
		}
        if( camera_controller == null ) {
    		return true;
        }
        //invalidate();
		/* {
			Log.d(TAG, "touch event: " + event.getAction());
		}*/
		if( event.getPointerCount() != 1 ) {
			//multitouch_time = System.currentTimeMillis();
			touch_was_multitouch = true;
			return true;
		}
		if( event.getAction() != MotionEvent.ACTION_UP ) {
			if( event.getAction() == MotionEvent.ACTION_DOWN && event.getPointerCount() == 1 ) {
				touch_was_multitouch = false;
			}
			return true;
		}
		if( touch_was_multitouch ) {
			return true;
		}
		
		if (isTakeButtonPressed()) {
		    return true;
		}
		
		if (activity.isSeekbarTouching()) {
		    return true;
		}
		
		synchronized (mAsyncHandler) {
	        mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_TOUCH);
	        Message msg = mAsyncHandler.obtainMessage(ASYNC_MSG_WHAT_TOUCH, (int)event.getX(),
	                (int)event.getY());
			if ((gpuImage.isTiltShiftEnable()
					|| gpuImage.isSelectiveBlurEnable())
					&& !canAutoFocus()) {
				mAsyncHandler.sendMessageDelayed(msg, 500);
			} else {
				mAsyncHandler.sendMessage(msg);
			}
			activity.getShowTextHandler().sendEmptyMessage(CameraFragment.MESSAGE_SHOW_ZOOMLAYOUT);
        }
		return true;
	}
	
	private synchronized void processTouch(int x, int y) {
	    if (isTakeButtonPressed()) {
            return;
        }
	    
        if (isSwitching()) {
            return;
        }
        
        if( !this.isVideoOrMotion() && this.isTakingPhotoOrOnTimer() ) {
            // if video, okay to refocus when recording
            return;
        }
        
        if (this.isVideoOrMotion()) {
            tryVideoAutoFocus();
            return;
        }
        
        // note, we always try to force start the preview (in case is_preview_paused has become false)
        // except if recording video (firstly, the preview should be running; secondly, we don't want to reset the phase!)
        startCameraPreview();

        boolean touchToTackPic = SettingsManager.getPreferenceTouchToTakePic();
        if( camera_controller != null) {
            synchronized (mFocusHelper) {
                if (!mFocusHelper.isFocusWaiting()) {
                    mFocusHelper.setHasFocusArea(false);
                    ArrayList<Area> focusAreas = getAreas(x, y, 1.0f);
                    ArrayList<Area> meterAreas = getAreas(x, y, 1.5f);
                    if( camera_controller.setFocusAndMeteringArea(focusAreas, meterAreas) ) {
                        mFocusHelper.setFocusScreen(x, y);
                        mFocusHelper.setHasFocusArea(true);
                    }
                    else {
                    }
                }
            }
        }
        if (!touchToTackPic) {
            if (canAutoFocus()) {
                cancelAutoFocus();
                tryAutoFocus(false, true);
            }
        }
	}
	
    private boolean isSwitching() {
        synchronized (mAsyncHandler) {
            return mAsyncHandler.hasMessages(ASYNC_MSG_WHAT_SWITCH_TO_PHOTO)
                    || mAsyncHandler.hasMessages(ASYNC_MSG_WHAT_SWITCH_TO_VIDEO)
                    || mAsyncHandler.hasMessages(ASYNC_MSG_WHAT_SWITCH_CAMERA)
                    || mAsyncHandler.hasMessages(ASYNC_MSG_WHAT_TAKE_CLICK);
        }
    }
	
	//@SuppressLint("ClickableViewAccessibility") @Override

    private float zoomCurrentSpan = 1.0F;

    private int firstZoomValue = 0;

    private int lastZoomValue = 0;

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            int scale = zoomScale(firstZoomValue, zoomCurrentSpan, detector.getCurrentSpan());
            if ((scale != -1) && (lastZoomValue != scale)) {
                zoomTo(scale, true, true);
                lastZoomValue = scale;
            }
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            zoomCurrentSpan = detector.getCurrentSpan();
            firstZoomValue = zoom_factor;
            lastZoomValue = firstZoomValue;
            return true;
        }
    }
	
    private int zoomScale(int firstZoomValue, float zoomCurrentSpan, float zoomNewSpan) {
        if (!has_zoom || zoom_ratios.size() <= firstZoomValue) {
            return -1;
        }

        int i = (int)(zoomNewSpan / zoomCurrentSpan * zoom_ratios.get(firstZoomValue));
        int j = zoom_ratios.size();
        for (int k = 0; k < j; k++) {
            if (i <= zoom_ratios.get(k)) {
                return k;
            }
        }
        return j - 1;
    }
    
    private synchronized void clearFocusAreas() {
		if( camera_controller == null ) {
			return;
		}
//		if (!(using_face_detection && mFaceChecker.isFaceDetectionStarted())) {
//		    camera_controller.clearFocusAndMetering();
//		    Log.d("Test", "clearFocusAndMetering");
//		}
        cancelAutoFocus();
		mFocusHelper.setHasFocusArea(false);
		mFocusHelper.setFocusState(FocusHelper.FOCUS_DONE);
		mFocusHelper.setFocusWaitingState(FocusHelper.FOCUS_WAITING_STATE_NONE);
    }

	public void surfaceCreated(SurfaceHolder holder) {
		// The Surface has been created, acquire the camera and tell it where
		// to draw.
		this.has_surface = true;
		if (glSurfaceView != null) {
		    glSurfaceView.setWillNotDraw(true);
		}
		if (surfaceView != null) {
			surfaceView.setWillNotDraw(true);
		}
		asyncOpenCamera();
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		// Surface will be destroyed when we return, so stop the preview.
		// Because the CameraDevice object is not a shared resource, it's very
		// important to release it when the activity is paused.
		this.has_surface = false;
//		this.closeCamera();
		asyncCloseCamera();
	}

	public synchronized void stopVideo() {
		stopVideo(isMotion() && !mMotionPressedInside);
	}
	
	public synchronized void stopVideo(boolean cancel) {
		if (video_recorder == null && !isVideoPausing() && !gpuImage.isRecording()) {
			return;
		}
        /*is_taking_photo = false;
        is_taking_photo_on_timer = false;*/
        long recordTime = getVideoRecordTime();
		if (phase != PHASE_PREVIEW_PAUSED) {
			if (gpuImage.isRecording()) {
				gpuImage.stopRecording();
				boolean enable_sound = SettingsManager.getPreferenceShutterSound() && !isMotion();
				if (enable_sound && mSoundManager != null) {
					mSoundManager.playRecordSound(false);
				}
				if( video_name != null ) {
					filesList.add(new File(video_name));
					video_name = null;
				}
			} else if( video_recorder != null ) { // check again, just to be safe
    			try {
    				video_recorder.setOnErrorListener(null);
    				video_recorder.setOnInfoListener(null);
    				video_recorder.stop();
    			}
    			catch(RuntimeException e) {
    				// stop() can throw a RuntimeException if stop is called too soon after start - we have no way to detect this, so have to catch it
					Log.d(TAG, "runtime exception when stopping video");
    			}
        		video_recorder.reset();
        		video_recorder.release(); 
        		video_recorder = null;
				if (PhoneInfo.isVideoIssueDevice()) {
					closeCamera();
					openCamera(false);
				} else {
					reconnectCamera(false); // n.b., if something went wrong with video, then we reopen the camera - which may fail (or simply not reopen, e.g., if app is now paused)
				}
        		if( video_name != null ) {
        		    filesList.add(new File(video_name));
        			video_name = null;
        		}
    		}
		}
		this.phase = PHASE_NORMAL;
		this.savedRecordTime = 0;
		mHandler.removeMessages(HANDLER_WHAT_UPDATE_RECORD_TIME);
		mHandler.removeMessages(HANDLER_WHAT_UPDATE_MOTION_PROGRESS);
        activity.getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				activity.updateRecordTime(0);
				activity.changeUIForStartVideo(false);
				activity.changeUIForResumeVideo(true);
			}
		});
		if (cancel) {
			try {
				for (File file : filesList) {
					file.delete();
				}
			} catch (Throwable tr) {
				Log.e(TAG, "", tr);
			}
			filesList.clear();
		} else if (isMotion() && recordTime < MIN_MOTION_TIME) {
			try {
				for (File file : filesList) {
					file.delete();
				}
			} catch (Throwable tr) {
				Log.e(TAG, "", tr);
			}
			filesList.clear();
		} else {
			activity.getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {

				}
			});
			processVideoFile(recordTime);
		}
	}
	
	/**
	 * 处理录音文件
	 */
    private void processVideoFile(long recordTime) {
        final ArrayList<File> filesListToExport = new ArrayList<File>(filesList);
        filesList.clear();
        if (filesListToExport.size() > 0) {
            ProcessVideoService.post(activity.getActivity(), filesListToExport, recordTime, null, activity.getActivity().getIntent());
        }
    }

	public void togglePauseVideo() {
		synchronized (mAsyncHandler) {
			mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_TAKE_CLICK);
			mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_TOUCH);
			mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_PAUSE_VIDEO);
			mAsyncHandler.sendEmptyMessage(ASYNC_MSG_WHAT_PAUSE_VIDEO);
		}
	}

	private void pauseOrResumeVideo() {
		if (isVideoOrMotion()) {
			if (phase == PHASE_TAKING_PHOTO) {
				pauseVideo();
			} else if (phase == PHASE_PAUSEING_VIDEO) {
				takeVideo();
			} else {
				Log.e(TAG, "pause video pressed but is not video start");
			}
		} else {
			
				Log.d(TAG, "pause video pressed but is not video");
		}
	}
	
    private void pauseVideo() {
        
            Log.d(TAG, "stopVideo()");
        
        if (isVideoOrMotion()) { // check again, just to be safe
            if( !video_start_time_set || System.currentTimeMillis() - video_start_time < 1000 ) {
                // if user presses to stop too quickly, we ignore
                // firstly to reduce risk of corrupt video files when stopping too quickly (see RuntimeException we have to catch in stopVideo),
                // secondly, to reduce a backlog of events which slows things down, if user presses start/stop repeatedly too quickly
                
                    Log.d(TAG, "ignore pressing pause video too quickly after start");
                return;
            }
            
                Log.d(TAG, "pause video recording");
			if (gpuImage.isRecording()) {
				gpuImage.stopRecording();
				boolean enable_sound = SettingsManager.getPreferenceShutterSound();
				if (enable_sound && mSoundManager != null) {
					mSoundManager.playRecordSound(false);
				}
			} else if (video_recorder != null) {
            /*
             * is_taking_photo = false; is_taking_photo_on_timer = false;
             */
				try {
					video_recorder.setOnErrorListener(null);
					video_recorder.setOnInfoListener(null);
					video_recorder.stop();
				} catch (RuntimeException e) {
					// stop() can throw a RuntimeException if stop is called too
					// soon after start - we have no way to detect this, so have to
					// catch it
					
						Log.d(TAG, "runtime exception when stopping video");
				}
				video_recorder.reset();
				video_recorder.release();
				video_recorder = null;
				if (PhoneInfo.isVideoIssueDevice()) {
					closeCamera();
					this.phase = PHASE_PAUSEING_VIDEO;
					openCamera(false);
				} else {
					reconnectCamera(false); // n.b., if something went wrong with video,
					// then we reopen the camera - which may
					// fail (or simply not reopen, e.g., if app
					// is now paused)
				}
			}
			synchronized (PHASE_LOCK) {
				this.phase = PHASE_PAUSEING_VIDEO;
				if (video_name != null) {
					filesList.add(new File(video_name));
					savedRecordTime += System.currentTimeMillis() - video_start_time;
					video_name = null;
				}
			}
			activity.getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					activity.updateRecordTime(getVideoRecordTime());
					activity.changeUIForStartVideo(true);
					activity.changeUIForResumeVideo(false);
				}
			});
        }
    }

	public void toggleVideoTakePic() {
		if (isVideoRecording() || isVideoPausing()) {
			initDrawPhotoFrameData();
			enableDrawCaptureFrame();
			captureFiltFrame = true;
		}
	}
    
    /**
     * 是否摄像暂停中
     * 
     * @return
     */
    public boolean isVideoPausing() {
        return isVideoOrMotion() && phase == PHASE_PAUSEING_VIDEO;
    }
	
	private Context getContext() {
		return activity.getActivity();
	}

	private synchronized void reconnectCamera(boolean quiet) {
        if( camera_controller != null ) { // just to be safe
    		try {
    			camera_controller.reconnect();
		        this.startCameraPreview();
			}
    		catch (IOException e) {
        		
        			Log.e(TAG, "failed to reconnect to camera");
				e.printStackTrace();
	    	    closeCamera();
			}
    		try {
    			tryAutoFocus(false, false);
    		}
    		catch(RuntimeException e) {
    			
    				Log.e(TAG, "tryAutoFocus() threw exception: " + e.getMessage());
    			e.printStackTrace();
    			// this happens on Nexus 7 if trying to record video at bitrate 50Mbits or higher - it's fair enough that it fails, but we need to recover without a crash!
    			// not safe to call closeCamera, as any call to getParameters may cause a RuntimeException
    			this.is_preview_started = false;
    			camera_controller.release();
    			camera_controller = null;
    			openCamera(false);
    		}
		}
	}

	private synchronized void closeCamera() {
		 {
			Log.d(TAG, "closeCamera()");
		}
        mFocusHelper.setHasFocusArea(false);
        mFocusHelper.setFocusState(FocusHelper.FOCUS_DONE);
        mFocusHelper.setFocusWaitingState(FocusHelper.FOCUS_WAITING_STATE_NONE);
		// n.b., don't reset has_set_location, as we can remember the location when switching camera
		cancelTimer();
		if( camera_controller != null ) {
			stopVideo();
			// need to check for camera being non-null again - if an error occurred stopping the video, we will have closed the camera, and may not be able to reopen
			if( camera_controller != null ) {
				//camera.setPreviewCallback(null);
				pausePreview();
				camera_controller.release();
				camera_controller = null;
			}
		}
	}
	
	public void cancelTimer() {
		
			Log.d(TAG, "cancelTimer()");
		if( this.isOnTimer() ) {
			takePictureTimerTask.cancel();
			takePictureTimerTask = null;
			mHandler.removeMessages(HANDLER_WHAT_UPDATE_DELAY_REMAINING_TIME);
			activity.updateDelayRemainingTime(0);
			/*is_taking_photo_on_timer = false;
			is_taking_photo = false;*/
    		this.phase = PHASE_NORMAL;
			
				Log.d(TAG, "cancelled camera timer");
		}
	}
	
	synchronized void pausePreview() {
		
			Log.d(TAG, "pausePreview()");
		if( camera_controller == null ) {
			
				Log.d(TAG, "camera not opened!");
			return;
		}
		if( this.isVideoOrMotion() ) {
			// make sure we're into continuous video mode
			// workaround for bug on Samsung Galaxy S5 with UHD, where if the user switches to another (non-continuous-video) focus mode, then goes to Settings, then returns and records video, the preview freezes and the video is corrupted
			// so to be safe, we always reset to continuous video mode
			// although I've now fixed this at the level where we close the settings, I've put this guard here, just in case the problem occurs from elsewhere
			this.setUpFocus(false);
		}
		this.setPreviewPaused(false);
		camera_controller.stopPreview();
		this.phase = PHASE_NORMAL;
		this.is_preview_started = false;
	}
	
	//private int debug_count_opencamera = 0; // see usage below

	private synchronized void openCamera(boolean updateFilter) {
		long debug_time = 0;
		 {
			Log.d(TAG, "openCamera()");
			Log.d(TAG, "cameraId: " + cameraId);
			debug_time = System.currentTimeMillis();
		}
		if (camera_controller != null) {
		     {
                Log.d(TAG, "camera has opened");
            }
		    return;
		}
		// need to init everything now, in case we don't open the camera (but these may already be initialised from an earlier call - e.g., if we are now switching to another camera)
		// n.b., don't reset has_set_location, as we can remember the location when switching camera
        mFocusHelper.setHasFocusArea(false);
        mFocusHelper.setFocusState(FocusHelper.FOCUS_DONE);
        mFocusHelper.setFocusWaitingState(FocusHelper.FOCUS_WAITING_STATE_NONE);
		scene_modes = null;
		has_zoom = false;
		max_zoom_factor = 0;
		zoom_ratios = null;
		supports_face_detection = false;
		supports_video_stabilization = false;
		can_disable_shutter_sound = false;
		sizes = null;
		current_size_index = -1;
		video_quality = null;
		current_video_quality = -1;
		current_motion_quality = -1;
		supported_flash_values = null;
		supported_white_balances = null;
		supported_isos = null;
		supported_ev_values = null;
		min_exposure = 0;
		max_exposure = 0;
		exposure_step = 0.0f;
		max_num_focus_areas = 0;
		disableDrawCaptureFrame();
		
			Log.d(TAG, "done showGUI");
		if( !this.has_surface ) {
			 {
				Log.d(TAG, "preview surface not yet available");
			}
			return;
		}
		if( this.app_is_stoped ) {
			 {
				Log.d(TAG, "don't open camera as app is paused");
			}
			return;
		}
		
		/*{
			// debug
			if( debug_count_opencamera++ == 0 ) {
				
					Log.d(TAG, "debug: don't open camera yet");
				return;
			}
		}*/
		try {
			
				Log.d(TAG, "try to open camera: " + cameraId);
			if( test_fail_open_camera ) {
				
					Log.d(TAG, "test failing to open camera");
				throw new RuntimeException();
			}
            camera_controller = new CameraController1(cameraId, new Camera.ErrorCallback() {
                @Override
                public void onError(int error, Camera camera) {
                    if(error == Camera.CAMERA_ERROR_EVICTED){
                        Log.d(TAG, "Camera error Camera.CAMERA_ERROR_EVICTED");
                    } else if(error == Camera.CAMERA_ERROR_SERVER_DIED){
                        Log.d(TAG, "Camera error Camera.CAMERA_ERROR_SERVER_DIED");
                    } else if(error == Camera.CAMERA_ERROR_UNKNOWN){
                        Log.d(TAG, "Camera error Camera.CAMERA_ERROR_UNKNOWN");
                    }
                    if (PhoneInfo.isVideoIssueDevice()) {
                        closeCamera();
                        openCamera(false);
                    } else {
                        reconnectCamera(true); // n.b., if something went wrong with video, then we reopen the camera - which may fail (or simply not reopen, e.g., if app is now paused)
                    }
                }
            });
			//throw new RuntimeException(); // uncomment to test camera not opening
		}
		catch(RuntimeException e) {
			
				Log.e(TAG, "Failed to open camera: " + e.getMessage());
			e.printStackTrace();
			camera_controller = null;
		}
		 {
			Log.d(TAG, "time after opening camera: " + (System.currentTimeMillis() - debug_time));
		}
		if( camera_controller != null ) {
			Activity activity = (Activity)this.getContext();

	        this.setCameraDisplayOrientation();
			if (isVideoOrMotion() && PhoneInfo.isNotSupportVideoRender()) {
				try {
					camera_controller.setPreviewDisplay(mHolder);
				}
				catch(IOException e) {
					
						Log.e(TAG, "Failed to set preview display: " + e.getMessage());
					e.printStackTrace();
				}
			}

			if (updateFilter) {
				updateFilter();
			}
		    setupCamera();
		} else {
		    activity.showCameraErrorDialog();
		}

		 {
			Log.d(TAG, "total time: " + (System.currentTimeMillis() - debug_time));
		}

	}
	
	/* Should only be called after camera first opened, or after preview is paused.
	 */
	synchronized void setupCamera() {
		
			Log.d(TAG, "setupCamera()");
		/*long debug_time = 0;
		 {
			debug_time = System.currentTimeMillis();
		}*/
		if( camera_controller == null ) {
			
				Log.d(TAG, "camera not opened!");
			return;
		}
//		if( this.isVideoOrMotion() ) {
			// make sure we're into continuous video mode for reopening
			// workaround for bug on Samsung Galaxy S5 with UHD, where if the user switches to another (non-continuous-video) focus mode, then goes to Settings, then returns and records video, the preview freezes and the video is corrupted
			// so to be safe, we always reset to continuous video mode
			// although I've now fixed this at the level where we close the settings, I've put this guard here, just in case the problem occurs from elsewhere
//			this.updateFocusForVideo(false);
//		}

		setupCameraParameters();
		
		// Must set preview size before starting camera preview
		// and must do it after setting photo vs video mode
        setPreviewSize(); // need to call this when we switch cameras, not just
                          // when we run for the first time
		startCameraPreview();
		 {
			//Log.d(TAG, "time after starting camera preview: " + (System.currentTimeMillis() - debug_time));
		}

		// must be done after setting parameters, as this function may set parameters
		// also needs to be done after starting preview for some devices (e.g., Nexus 7)
		if( this.has_zoom && zoom_factor != 0 ) {
			int new_zoom_factor = zoom_factor;
			zoom_factor = 0; // force zoomTo to actually update the zoom!
			zoomTo(new_zoom_factor, true, false);
		}

		if (mFocusHelper.isAutoFocus()) {
	    	final Handler handler = new Handler();
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					
						Log.d(TAG, "do startup autofocus");
					tryAutoFocus(true, false); // so we get the autofocus when starting up - we do this on a delay, as calling it immediately means the autofocus doesn't seem to work properly sometimes (at least on Galaxy Nexus)
				}
			}, 500);
	    }
	}

	private synchronized void setupCameraParameters() {
		
			Log.d(TAG, "setupCameraParameters()");
		long debug_time = 0;
		 {
			debug_time = System.currentTimeMillis();
		}
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());

		if (!isFrontCamera()) {
            if (SettingsManager.getPreferenceHdrOn()) {
                CameraController.SupportedValues supported_values = camera_controller.setSceneMode("hdr");
                if (supported_values != null) {
                    scene_modes = supported_values.values;
                }
            }
		}
		
		{
			// grab all read-only info from parameters
			
				Log.d(TAG, "grab info from parameters");
			CameraController.CameraFeatures camera_features = camera_controller.getCameraFeatures();
			this.has_zoom = camera_features.is_zoom_supported;
			if( this.has_zoom ) {
				this.max_zoom_factor = camera_features.max_zoom;
				this.zoom_ratios = camera_features.zoom_ratios;
			}
			this.supports_face_detection = camera_features.supports_face_detection;
			this.sizes = camera_features.picture_sizes;
			this.has_current_fps_range = camera_features.has_current_fps_range;
			if( this.has_current_fps_range ) {
				this.current_fps_range = camera_features.current_fps_range;
			}
	        supported_flash_values = camera_features.supported_flash_values;
			supported_white_balances = camera_features.supported_whiteblance_values;
			supported_isos = camera_features.supported_iso_values;
	        this.max_num_focus_areas = camera_features.max_num_focus_areas;
	        this.supports_video_stabilization = camera_features.is_video_stabilization_supported;
	        this.can_disable_shutter_sound = camera_features.can_disable_shutter_sound;
			this.video_sizes = camera_features.video_sizes;
	        this.supported_preview_sizes = camera_features.preview_sizes;
			this.min_exposure = camera_features.min_exposure;
			this.max_exposure = camera_features.max_exposure;
			this.exposure_step = camera_features.exposure_step;
			mExposuresValue = camera_controller.getExposureCompensation();
		}

		if (min_exposure < 0 && max_exposure > 0 && exposure_step != 0f) {
			int minEv = (int) (min_exposure * exposure_step);
			int maxEv = (int) (max_exposure * exposure_step);
			int supportedEv = Math.min(-minEv, maxEv);
			if (supportedEv > 3) {
				supportedEv = 3;
			}
			supported_ev_values = new ArrayList<String>();
			for (int i = -supportedEv; i <= supportedEv; i ++) {
				if (i > 0) {
					supported_ev_values.add("+" + i);
				} else {
					supported_ev_values.add(String.valueOf(i));
				}
			}
		}

		{
			
				Log.d(TAG, "set up zoom");
			
				Log.d(TAG, "has_zoom? " + has_zoom);

		}

        {
			
				Log.d(TAG, "set up picture sizes");
			 {
				for(int i=0;i<sizes.size();i++) {
					Size size = sizes.get(i);
		        	Log.d(TAG, "supported picture size: " + size.width + " , " + size.height);
				}
			}
			current_size_index = -1;
			String resolution_value = sharedPreferences.getString(SettingsManager.getResolutionPreferenceKey(cameraId), "");
			
				Log.d(TAG, "resolution_value: " + resolution_value);
			if( resolution_value.length() > 0 ) {
				// parse the saved size, and make sure it is still valid
				int index = resolution_value.indexOf(' ');
				if( index == -1 ) {
					
						Log.d(TAG, "resolution_value invalid format, can't find space");
				}
				else {
					String resolution_w_s = resolution_value.substring(0, index);
					String resolution_h_s = resolution_value.substring(index+1);
					 {
						Log.d(TAG, "resolution_w_s: " + resolution_w_s);
						Log.d(TAG, "resolution_h_s: " + resolution_h_s);
					}
					try {
						int resolution_w = Integer.parseInt(resolution_w_s);
						
							Log.d(TAG, "resolution_w: " + resolution_w);
						int resolution_h = Integer.parseInt(resolution_h_s);
						
							Log.d(TAG, "resolution_h: " + resolution_h);
						// now find size in valid list
						for(int i=0;i<sizes.size() && current_size_index==-1;i++) {
							Size size = sizes.get(i);
				        	if( size.width == resolution_w && size.height == resolution_h ) {
				        		current_size_index = i;
								
									Log.d(TAG, "set current_size_index to: " + current_size_index);
				        	}
						}
						if( current_size_index == -1 ) {
							
								Log.e(TAG, "failed to find valid size");
						}
					}
					catch(NumberFormatException exception) {
						
							Log.d(TAG, "resolution_value invalid format, can't parse w or h to int");
					}
				}
			}

			if( current_size_index == -1 ) {
				// set to largest
				Size current_size = null;
				for(int i=0;i<sizes.size();i++) {
					Size size = sizes.get(i);
		        	if(current_size == null || (size.width*size.height > current_size.width*current_size.height &&
							(size.width != size.height || current_size.width == current_size.height))) {
		        		current_size_index = i;
		        		current_size = size;
		        	}
		        }
			}
			if( current_size_index != -1 ) {
				Size current_size = sizes.get(current_size_index);
	    		
	    			Log.d(TAG, "Current size index " + current_size_index + ": " + current_size.width + ", " + current_size.height);

	    		// now save, so it's available for PreferenceActivity
				resolution_value = current_size.width + " " + current_size.height;
				 {
					Log.d(TAG, "save new resolution_value: " + resolution_value);
				}
				SharedPreferences.Editor editor = sharedPreferences.edit();
				editor.putString(SettingsManager.getResolutionPreferenceKey(cameraId), resolution_value);
				editor.apply();
			}
        }

		{
			if (mParamCameraId == cameraId) {
				if (mWhiteBalanceValue != null) {
					setWhiteBlance(mWhiteBalanceValue);
				}
				if (mIsoValue != null) {
					setIso(mIsoValue);
				}
				if (mEvValue != null) {
					setEv(mEvValue);
				}
			} else {
				mWhiteBalanceValue = camera_controller.getWhiteBalance();
				mIsoValue = camera_controller.getISO();
				mEvValue = null;
			}
			mParamCameraId = cameraId;
		}

//		{
//			
//				Log.d(TAG, "set up jpeg quality");
//			int image_quality = getImageQuality();
//			camera_controller.setJpegQuality(image_quality);
//			
//				Log.d(TAG, "image quality: " + image_quality);
//		}

		// get available sizes
		initialiseVideoSizes();
		video_quality = CameraController.initialiseVideoQuality(cameraId, video_sizes);

		current_video_quality = -1;
		String video_quality_value_s = sharedPreferences.getString(SettingsManager.getVideoQualityPreferenceKey(cameraId), "");
		
			Log.d(TAG, "video_quality_value: " + video_quality_value_s);
		if( video_quality_value_s.length() > 0 ) {
			// parse the saved video quality, and make sure it is still valid
			// now find value in valid list
			for(int i=0;i<video_quality.size() && current_video_quality==-1;i++) {
	        	if( video_quality.get(i).mQuality.equals(video_quality_value_s) ) {
	        		current_video_quality = i;
					
						Log.d(TAG, "set current_video_quality to: " + current_video_quality);
	        	}
			}
			if( current_video_quality == -1 ) {
				
					Log.e(TAG, "failed to find valid video_quality");
			}
		}
		if( current_video_quality == -1 && video_quality.size() > 0 ) {
			// default to highest quality
			current_video_quality = 0;
			
				Log.d(TAG, "set video_quality value to " + video_quality.get(current_video_quality).mQuality);
		}
		if( current_video_quality != -1 ) {
    		// now save, so it's available for PreferenceActivity
			SharedPreferences.Editor editor = sharedPreferences.edit();
			editor.putString(SettingsManager.getVideoQualityPreferenceKey(cameraId), video_quality.get(current_video_quality).mQuality);
			editor.apply();
		}

		{
			current_motion_quality = -1;
			float targetRatio = 4f / 3f;
//			if (isFrontCamera()) {
//				VideoQuality quality = video_quality.get(current_video_quality);
//				targetRatio = quality.mSize.getWidth() / (float)quality.mSize.getHeight();
//			}
			int targetIndex = -1;
			VideoQuality targetQuality = null;
			int smallerIndex = -1;
			VideoQuality smallerQuality = null;
			int reservedIndex = -1;
			VideoQuality reservedQuality = null;
			for (int i = 0; i < video_quality.size(); i ++) {
				VideoQuality quality = video_quality.get(i);
				int distance = quality.mSize.getHeight() * quality.mSize.getWidth() - TARGET_MOTION_SIZE;
				if (Math.abs(targetRatio - quality.mSize.getWidth()
						/ (float)quality.mSize.getHeight()) < 0.01f) {
					if (distance >= 0) {
						if (targetQuality == null ||
								(Math.abs(targetQuality.mSize.getHeight() * targetQuality.mSize.getWidth() - TARGET_MOTION_SIZE) > distance)) {
							targetIndex = i;
							targetQuality = quality;
						}
					} else {
						if (smallerQuality == null ||
								(Math.abs(smallerQuality.mSize.getHeight() * smallerQuality.mSize.getWidth() - TARGET_MOTION_SIZE) > -distance)) {
							smallerIndex = i;
							smallerQuality = quality;
						}
					}

				}
				if (reservedQuality == null ||
						(Math.abs(reservedQuality.mSize.getHeight() * reservedQuality.mSize.getWidth() - TARGET_MOTION_SIZE) > Math.abs(distance))) {
					reservedIndex = i;
					reservedQuality = quality;
				}
			}

			if (targetQuality == null) {
				targetIndex = smallerIndex;
				targetQuality = smallerQuality;
			}

			if (targetQuality == null) {
				targetIndex = reservedIndex;
				targetQuality = reservedQuality;
			}
			current_motion_quality = targetIndex;
		}

		CameraSizesManager.getInstance().addCameraSizes(cameraId, sizes, video_quality);

		{
            
                Log.d(TAG, "set up flash");
			checkFlashState(true);
		}

		{
		    setUpFocus(false);
		}

		 {
			Log.d(TAG, "time after setting up camera parameters: " + (System.currentTimeMillis() - debug_time));
		}
	}
	
	private void setUpFocus(boolean autoFocus) {
        
            Log.d(TAG, "set up focus");
        List<String> supportedFocusValues = getSupportedFocusValues();
        if( supportedFocusValues != null && supportedFocusValues.size() > 0 ) {
            
                Log.d(TAG, "focus values: " + supportedFocusValues);
            if (isVideoOrMotion()) {
                if (supportedFocusValues.contains("focus_mode_continuous_video")) {
                    setFocusValue("focus_mode_continuous_video", true, false);
                    mFocusHelper.setAutoFocus(true);
                } else if (supportedFocusValues.contains("focus_mode_auto")) {
                    setFocusValue("focus_mode_auto", true, false);
                    mFocusHelper.setAutoFocus(true);
                    if (autoFocus) {
                        mHandler.postDelayed(new Runnable() {
                            
                            @Override
                            public void run() {
                                
                                    Log.d(TAG, "do startup video autofocus");
                                tryVideoAutoFocus(); // so we get the autofocus when starting up - we do this on a delay, as calling it immediately means the autofocus doesn't seem to work properly sometimes (at least on Galaxy Nexus)
                            }
                        }, 500);
                    }
                } else {
                    mFocusHelper.setAutoFocus(false);
                    
                        Log.d(TAG, "focus value no longer supported!");
                }
            } else {
                if (supportedFocusValues.contains("focus_mode_auto") 
                        && !(PhoneInfo.isSamsung() && isFrontCamera())) {
                    setFocusValue("focus_mode_auto", true, autoFocus);
                    mFocusHelper.setAutoFocus(true);
                } else {
                    mFocusHelper.setAutoFocus(false);
                    
                        Log.d(TAG, "focus value no longer supported!");
                }
            }
        } else {
            mFocusHelper.setAutoFocus(false);
        }
	}
	
	private synchronized List<String> getSupportedFocusValues() {
	    if (camera_controller == null) {
	        
	            Log.d(TAG, "getSupportedFocusValues camera closed");
	        return null;
	    }
	    CameraController.CameraFeatures camera_features = camera_controller.getCameraFeatures();
	    if (camera_features == null) {
	        
                Log.d(TAG, "getSupportedFocusValues camera features null");
	        return null;
	    }
	    return camera_features.supported_focus_values;
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		
			Log.d(TAG, "surfaceChanged " + w + ", " + h);
		activity.updatePreviewMask();
		/*
			Log.d(TAG, "surface frame " + mHolder.getSurfaceFrame().width() + ", " + mHolder.getSurfaceFrame().height());*/
		// surface size is now changed to match the aspect ratio of camera preview - so we shouldn't change the preview to match the surface size, so no need to restart preview here
		// update: except for Android L, where we must start the preview after the surface has changed size

        if( camera_controller == null ) {
			
				Log.d(TAG, "camera not opened!");
            return;
        }
//
//		CameraFragment main_activity = (CameraFragment)Preview.this.getContext();
//		main_activity.layoutUI(); // need to force a layoutUI update (e.g., so UI is oriented correctly when app goes idle, device is then rotated, and app is then resumed
	}

	public void refreshFocusOverlay() {
		ViewGroup.LayoutParams params = mFocusOverlay.getLayoutParams();
		params.width = getSurfaceView().getWidth();
		params.height = getSurfaceView().getHeight();
		mFocusOverlay.setLayoutParams(params);
	}
	
	private synchronized void setPreviewSize() {
		
			Log.d(TAG, "setPreviewSize()");
		// also now sets picture size
		if( camera_controller == null ) {
			
				Log.d(TAG, "camera not opened!");
			return;
		}
		if( is_preview_started ) {
			
				Log.e(TAG, "setPreviewSize() shouldn't be called when preview is running");
//			throw new RuntimeException();
		}
		cancelAutoFocus();
		// first set picture size (for photo mode, must be done now so we can set the picture size from this; for video, doesn't really matter when we set it)
		Size new_size = null;
    	if( this.isVideoOrMotion() ) {
    		// In theory, the picture size shouldn't matter in video mode, but the stock Android camera sets a picture size
    		// which is the largest that matches the video's aspect ratio.
    		// This seems necessary to work around an aspect ratio bug introduced in Android 4.4.3 (on Nexus 7 at least): http://code.google.com/p/android/issues/detail?id=70830
    		// which results in distorted aspect ratio on preview and recorded video!
        	CamcorderProfile profile = getCamcorderProfile();
        	
        		Log.d(TAG, "video size: " + profile.videoFrameWidth + " x " + profile.videoFrameHeight);
        	double targetRatio = ((double)profile.videoFrameWidth) / (double)profile.videoFrameHeight;
        	new_size = getOptimalVideoPictureSize(sizes, targetRatio);
    	}
    	else {
    		if( current_size_index != -1 ) {
    			new_size = sizes.get(current_size_index);
    		}
    	}
    	if( new_size != null /*&& !isFiltMode() */) {
    		camera_controller.setPictureSize(new_size.width, new_size.height);
    	}
		// set optimal preview size
        if( supported_preview_sizes != null && supported_preview_sizes.size() > 0 ) {
			mPreviewSize = getOptimalPreviewSize2(supported_preview_sizes);
        	camera_controller.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
    		this.setAspectRatio(((double) mPreviewSize.width) / (double) mPreviewSize.height);
        }
		//重新调整尺寸过后,也得将画贴纸层的尺寸给重置
		activity.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				activity.initCanvasEmojiView();
			}
		});
	}

	private void sortVideoSizes() {
		
			Log.d(TAG, "sortVideoSizes()");
		Collections.sort(this.video_sizes, new Comparator<Size>() {
			public int compare(final Size a, final Size b) {
				return b.width * b.height - a.width * a.height;
			}
		});
	}
	
	private synchronized void initialiseVideoSizes() {
		if( camera_controller == null ) {
			
				Log.d(TAG, "camera not opened!");
			return;
		}
		this.sortVideoSizes();
		 {
			for(Size size : video_sizes) {
    			Log.d(TAG, "    supported video size: " + size.width + ", " + size.height);
			}
        }
	}

	private CamcorderProfile getCamcorderProfile(String quality) {
		
			Log.d(TAG, "getCamcorderProfile(): " + quality);
		CamcorderProfile camcorder_profile = CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH); // default
		try {
			String profile_string = quality;
			int index = profile_string.indexOf('_');
			if( index != -1 ) {
				profile_string = quality.substring(0, index);
				
					Log.d(TAG, "    profile_string: " + profile_string);
			}
			int profile = Integer.parseInt(profile_string);
			camcorder_profile = CamcorderProfile.get(cameraId, profile);
			if( index != -1 && index+1 < quality.length() ) {
				String override_string = quality.substring(index+1);
				
					Log.d(TAG, "    override_string: " + override_string);
				if( override_string.charAt(0) == 'r' && override_string.length() >= 4 ) {
					index = override_string.indexOf('x');
					if( index == -1 ) {
						
							Log.d(TAG, "override_string invalid format, can't find x");
					}
					else {
						String resolution_w_s = override_string.substring(1, index); // skip first 'r'
						String resolution_h_s = override_string.substring(index+1);
						 {
							Log.d(TAG, "resolution_w_s: " + resolution_w_s);
							Log.d(TAG, "resolution_h_s: " + resolution_h_s);
						}
						// copy to local variable first, so that if we fail to parse height, we don't set the width either
						int resolution_w = Integer.parseInt(resolution_w_s);
						int resolution_h = Integer.parseInt(resolution_h_s);
						camcorder_profile.videoFrameWidth = resolution_w;
						camcorder_profile.videoFrameHeight = resolution_h;
					}
				}
				else {
					
						Log.d(TAG, "unknown override_string initial code, or otherwise invalid format");
				}
			}
		}
        catch(NumberFormatException e) {
    		
    			Log.e(TAG, "failed to parse video quality: " + quality);
    		e.printStackTrace();
        }
		return camcorder_profile;
	}
	
	private CamcorderProfile getCamcorderProfile() {
		// 4K UHD video is not yet supported by Android API (at least testing on Samsung S5 and Note 3, they do not return it via getSupportedVideoSizes(), nor via a CamcorderProfile (either QUALITY_HIGH, or anything else)
		// but it does work if we explicitly set the resolution (at least tested on an S5)
		CamcorderProfile profile = null;
		if (isCaptureLowVideo()) {
			profile = CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_LOW);
		} else if (isMotion() && current_motion_quality != -1) {
			profile = getCamcorderProfile(video_quality.get(current_motion_quality).mQuality);
		}
		else if( current_video_quality != -1 ) {
			profile = getCamcorderProfile(video_quality.get(current_video_quality).mQuality);
		}
		else {
			profile = CamcorderProfile.get(cameraId, CamcorderProfile.QUALITY_HIGH);
		}

		String bitrate_value = SettingsManager.getPreferenceVideoBitrate();
		if( !bitrate_value.equals("default") ) {
			try {
				int bitrate = Integer.parseInt(bitrate_value);
				
					Log.d(TAG, "bitrate: " + bitrate);
				profile.videoBitRate = bitrate;
			}
			catch(NumberFormatException exception) {
				
					Log.d(TAG, "bitrate invalid format, can't parse to int: " + bitrate_value);
			}
		}
		String fps_value = SettingsManager.getPreferenceVideoFps();
		if( !fps_value.equals("default") ) {
			try {
				int fps = Integer.parseInt(fps_value);
				
					Log.d(TAG, "fps: " + fps);
				profile.videoFrameRate = fps;
			}
			catch(NumberFormatException exception) {
				
					Log.d(TAG, "fps invalid format, can't parse to int: " + fps_value);
			}
		}		
		return profile;
	}

	/**
	 * 是否录制低质量视频
	 *
	 * @return
	 */
	private boolean isCaptureLowVideo() {
		Intent intent = activity.getActivity().getIntent();
		if (intent != null) {
			int quality = intent.getIntExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1);
			return quality == 0;
		}
		return false;
	}

	private synchronized double getTargetRatioForPreview(Point display_size) {
        double targetRatio = 0.0f;
		String preview_size = SettingsManager.getPreferencePreviewSize();
		// should always use wysiwig for video mode, otherwise we get incorrect aspect ratio shown when recording video (at least on Galaxy Nexus, e.g., at 640x480)
		// also not using wysiwyg mode with video caused corruption on Samsung cameras (tested with Samsung S3, Android 4.3, front camera, infinity focus)
		if( (preview_size.equals("preference_preview_size_wysiwyg")) || this.isVideoOrMotion() ) {
	        if( this.isVideoOrMotion() ) {
	        	
	        		Log.d(TAG, "set preview aspect ratio from video size (wysiwyg)");
	        	CamcorderProfile profile = getCamcorderProfile();
	        	
	        		Log.d(TAG, "video size: " + profile.videoFrameWidth + " x " + profile.videoFrameHeight);
	        	targetRatio = ((double)profile.videoFrameWidth) / (double)profile.videoFrameHeight;
	        }
	        else {
	        	
	        		Log.d(TAG, "set preview aspect ratio from photo size (wysiwyg)");
	        	Size picture_size = camera_controller.getPictureSize();
	        	
	        		Log.d(TAG, "picture_size: " + picture_size.width + " x " + picture_size.height);
	        	targetRatio = ((double)picture_size.width) / (double)picture_size.height;
	        }
		}
		else {
        	
        		Log.d(TAG, "set preview aspect ratio from display size");
        	// base target ratio from display size - means preview will fill the device's display as much as possible
        	// but if the preview's aspect ratio differs from the actual photo/video size, the preview will show a cropped version of what is actually taken
        	if (display_size.x >= display_size.y) {
        	    targetRatio = ((double)display_size.x) / (double)display_size.y;
        	} else {
        	    targetRatio = ((double)display_size.y) / (double)display_size.x;
        	}
		}
		
			Log.d(TAG, "targetRatio: " + targetRatio);
		return targetRatio;
	}

	private Size getClosestSize(List<Size> sizes, double targetRatio) {
		
			Log.d(TAG, "getClosestSize()");
		Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        for(Size size : sizes) {
            double ratio = (double)size.width / size.height;
            if( Math.abs(ratio - targetRatio) < minDiff ) {
                optimalSize = size;
                minDiff = Math.abs(ratio - targetRatio);
            }
        }
        return optimalSize;
	}

    private Size getOptimalPreviewSize2(List<Size> sizes) {
        
            Log.d(TAG, "getOptimalPreviewSize()");
        final double ASPECT_TOLERANCE = 0.05;
        if( sizes == null )
            return null;
        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        Point display_size = new Point();
        Activity activity = (Activity)this.getContext();
        {
            Display display = activity.getWindowManager().getDefaultDisplay();
            display.getSize(display_size);
            
                Log.d(TAG, "display_size: " + display_size.x + " x " + display_size.y);
        }
        double targetRatio = getTargetRatioForPreview(display_size);
        int targetHeight = Math.min(display_size.y, display_size.x);
        if( targetHeight <= 0 ) {
            targetHeight = display_size.y;
        }
        List<Size> matchRatio = new ArrayList<Size>();
        List<Size> matchLimit = new ArrayList<Size>();
		boolean sizeLimit = PhoneInfo.isNotSupportOES();
        // Try to find the size which matches the aspect ratio, and is closest match to display height
        for(Size size : sizes) {
            
                Log.d(TAG, "    supported preview size: " + size.width + ", " + size.height);
            double ratio = (double)size.width / size.height;
            if( Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE )
                continue;
            matchRatio.add(size);
            if (sizeLimit && size.width * size.height <= FILT_MODE_SIZE_LIMIT) {
                matchLimit.add(size);
            }
        }
        
        if (matchLimit.size() > 0) {
            for(Size size : matchLimit) {
                if( Math.abs(size.height - targetHeight) < minDiff ) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        } else {
            for(Size size : matchRatio) {
                if( Math.abs(size.height - targetHeight) < minDiff ) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        
        if( optimalSize == null ) {
            // can't find match for aspect ratio, so find closest one
            
                Log.d(TAG, "no preview size matches the aspect ratio");
            optimalSize = getClosestSize(sizes, targetRatio);
        }
         {
            Log.d(TAG, "chose optimalSize: " + optimalSize.width + " x " + optimalSize.height);
            Log.d(TAG, "optimalSize ratio: " + ((double) optimalSize.width / optimalSize.height));
        }
        return optimalSize;
    }

	private Size getOptimalVideoPictureSize(List<Size> sizes, double targetRatio) {
		
			Log.d(TAG, "getOptimalVideoPictureSize()");
		final double ASPECT_TOLERANCE = 0.05;
        if( sizes == null )
        	return null;
        Size optimalSize = null;
        // Try to find largest size that matches aspect ratio
        for(Size size : sizes) {
    		
    			Log.d(TAG, "    supported preview size: " + size.width + ", " + size.height);
            double ratio = (double)size.width / size.height;
            if( Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE )
            	continue;
            if( optimalSize == null || size.width > optimalSize.width ) {
                optimalSize = size;
            }
        }
        if( optimalSize == null ) {
        	// can't find match for aspect ratio, so find closest one
    		
    			Log.d(TAG, "no picture size matches the aspect ratio");
    		optimalSize = getClosestSize(sizes, targetRatio);
        }
		 {
			Log.d(TAG, "chose optimalSize: " + optimalSize.width + " x " + optimalSize.height);
			Log.d(TAG, "optimalSize ratio: " + ((double) optimalSize.width / optimalSize.height));
		}
        return optimalSize;
    }

    private void setAspectRatio(double ratio) {
        if( ratio <= 0.0 )
        	throw new IllegalArgumentException();

        has_aspect_ratio = true;
        if( aspect_ratio != ratio ) {
        	aspect_ratio = ratio;
    		
    			Log.d(TAG, "new aspect ratio: " + aspect_ratio);
    		activity.getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					getSurfaceView().requestLayout();
				}
			});
        }
//        activity.updatePreviewMask();
	}

	private boolean hasAspectRatio() {
    	return has_aspect_ratio;
    }

    public double getAspectRatio() {
    	return aspect_ratio;
    }
    
    public Size getSurfaceSize() {
        Size size = null;
        View view = getSurfaceView();
        if (view != null) {
            size = new Size(view.getWidth(), view.getHeight());
        }
        return size;
    }

    // for the Preview - from http://developer.android.com/reference/android/hardware/Camera.html#setDisplayOrientation(int)
	// note, if orientation is locked to landscape this is only called when setting up the activity, and will always have the same orientation
	public synchronized void setCameraDisplayOrientation() {
		
			Log.d(TAG, "setCameraDisplayOrientation()");
		if( camera_controller == null ) {
			
				Log.d(TAG, "camera not opened!");
			return;
		}
		Activity activity = (Activity)this.getContext();
	    int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
	    int degrees = 0;
	    switch (rotation) {
	    	case Surface.ROTATION_0: degrees = 0; break;
	        case Surface.ROTATION_90: degrees = 90; break;
	        case Surface.ROTATION_180: degrees = 180; break;
	        case Surface.ROTATION_270: degrees = 270; break;
	    }
		
			Log.d(TAG, "    degrees = " + degrees);

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());
		String rotate_preview = SettingsManager.getPreferenceRotatePreview();
		
			Log.d(TAG, "    rotate_preview = " + rotate_preview);
		if( rotate_preview.equals("180") ) {
			degrees = (degrees + 180) % 360;
		}
		
	    camera_controller.setDisplayOrientation(degrees);
	}
	
	// for taking photos - from http://developer.android.com/reference/android/hardware/Camera.Parameters.html#setRotation(int)
	public void onOrientationChanged(int orientation) {
	    try {
    		if( orientation == OrientationEventListener.ORIENTATION_UNKNOWN )
    			return;
    		if( camera_controller == null ) {
    			/*
    				Log.d(TAG, "camera not opened!");*/
    			return;
    		}
    	    orientation = (orientation + 45) / 90 * 90;
    	    this.current_orientation = orientation % 360;
	    } catch(Throwable tr) {
	        Log.e(TAG, "", tr);
	    }
	}

	public synchronized int getCurrentRotation() {
		// orientation relative to camera's orientation (used for parameters.setRotation())
		int new_rotation = 0;
		try {
			if( camera_controller != null ) {
				int camera_orientation = camera_controller.getCameraOrientation();
				if (camera_controller.isFrontFacing()) {
					new_rotation = (camera_orientation - current_orientation + 360) % 360;
				} else {
					new_rotation = (camera_orientation + current_orientation) % 360;
				}
			}
		} catch(Throwable tr) {
			Log.e(TAG, "", tr);
		}
		return new_rotation;
	}

	private int getDeviceDefaultOrientation() {
	    WindowManager windowManager = (WindowManager)this.getContext().getSystemService(Context.WINDOW_SERVICE);
	    Configuration config = getResources().getConfiguration();
	    int rotation = windowManager.getDefaultDisplay().getRotation();
	    if( ( (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) &&
	    		config.orientation == Configuration.ORIENTATION_LANDSCAPE )
	    		|| ( (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) &&
	            config.orientation == Configuration.ORIENTATION_PORTRAIT ) ) {
	    	return Configuration.ORIENTATION_LANDSCAPE;
	    }
	    else { 
	    	return Configuration.ORIENTATION_PORTRAIT;
	    }
	}
	
	private void initDrawPhotoFrameData(){
		if(mCanDrawPhotoFrameWidth == 0){
			Resources res = CameraApp.getApplication().getResources();
			mCanDrawPhotoFrameWidth = ImageHelper.dpToPx(res, 5);
			DrawPhotoFrameWidthMax = ImageHelper.dpToPx(res, 8);
			mSingleDp = ImageHelper.dpToPx(res, 1);
		} else{
			mCanDrawPhotoFrameWidth = ImageHelper.dpToPx(CameraApp.getApplication().getResources(), 5);
		}
	}
	
	public void draw(View view, Canvas canvas, Paint p) {
		/*
			Log.d(TAG, "draw()");*/
		if( this.app_is_stoped ) {
    		/*
    			Log.d(TAG, "draw(): app is paused");*/
			return;
		}
		
		if (camera_controller == null) {
		    return;
		}
		
		/*if( true ) // test
			return;*/
		/*
			Log.d(TAG, "ui_rotation: " + ui_rotation);*/
		/*
			Log.d(TAG, "canvas size " + width + " x " + hight);*/
		/*
			Log.d(TAG, "surface frame " + mHolder.getSurfaceFrame().width() + ", " + mHolder.getSurfaceFrame().height());*/

		Rect rect = canvas.getClipBounds();
		int width = rect.width();
		int hight = rect.height();

		String preference_grid = SettingsManager.getPreferenceGrid();
		if( preference_grid.equals("preference_grid_3x3") ) {
			p.setColor(Color.WHITE);
			canvas.drawLine(width/3.0f + rect.left, 0.0f + rect.top, width/3.0f + rect.left, hight-1.0f + rect.top, p);
			canvas.drawLine(2.0f*width/3.0f + rect.left, 0.0f + rect.top, 2.0f*width/3.0f + rect.left, hight-1.0f + rect.top, p);
			canvas.drawLine(0.0f + rect.left, hight/3.0f + rect.top, width-1.0f + rect.left, hight/3.0f + rect.top, p);
			canvas.drawLine(0.0f + rect.left, 2.0f*hight/3.0f + rect.top, width-1.0f + rect.left, 2.0f*hight/3.0f + rect.top, p);
		}

		p.setStyle(Paint.Style.FILL); // reset

		if (mCanDrawPhotoFrame) {
			canvas.save();
			p.setColor(Color.WHITE);
			p.setStyle(Paint.Style.STROKE);
			p.setStrokeWidth(mCanDrawPhotoFrameWidth > DrawPhotoFrameWidthMax ? DrawPhotoFrameWidthMax : mCanDrawPhotoFrameWidth);
			mCanDrawPhotoFrameWidth += mSingleDp;
			float ratio = 0f;
			if (SettingsManager.getPreferenceSquare()) {
				ratio = 1f;
			} else if (SettingsManager.getPreferenceRect()) {
				ratio = 3f / 4f;
			}
			Rect cropRect = rect;
			if (ratio > 0f && !isVideoOrMotion()) {
				cropRect = BitmapUtils.cropRect(cropRect, ratio);
			}
			canvas.drawRect(cropRect, p);

			p.setStrokeWidth(0f);
			p.setStyle(Paint.Style.FILL); // reset
			canvas.restore();
		}

		if (this.isVideoOrMotion()) {
			return;
		}
        
        mFocusHelper.draw(canvas, p, rect, isTakeButtonPressed());
        p.setStrokeWidth(0);
        p.setStyle(Paint.Style.FILL); // reset
	}


	public void zoomIn() {
		
			Log.d(TAG, "zoomIn()");
    	if( zoom_factor < max_zoom_factor ) {
			zoomTo(zoom_factor+1, true, false);
        }
	}
	
	public void zoomOut() {
		
			Log.d(TAG, "zoomOut()");
		if( zoom_factor > 0 ) {
			zoomTo(zoom_factor-1, true, true);
        }
	}
	
	public void zoomByPercent(float percent) {
		int new_zoom_factor;
		new_zoom_factor = Math.round(percent * max_zoom_factor);
		zoomTo(new_zoom_factor, false, false);
	}
	
	public synchronized void zoomTo(int new_zoom_factor, boolean update_seek_bar, boolean isScale) {
		
			Log.d(TAG, "ZoomTo(): " + new_zoom_factor);
		activity.getShowTextHandler().sendEmptyMessage(CameraFragment.MESSAGE_SHOW_ZOOMLAYOUT);
		if( new_zoom_factor < 0 )
			new_zoom_factor = 0;
		if( new_zoom_factor > max_zoom_factor )
			new_zoom_factor = max_zoom_factor;
		// problem where we crashed due to calling this function with null camera should be fixed now, but check again just to be safe
    	if(new_zoom_factor != zoom_factor && camera_controller != null) {
			if( this.has_zoom ) {
				camera_controller.setZoom(new_zoom_factor);
				zoom_factor = new_zoom_factor;
	    		clearFocusAreas();
	    		if (update_seek_bar) {
	    			activity.updateZoomBarByPercent((float)new_zoom_factor / max_zoom_factor);
	    		}
			}
        }
	}
	
	public void toggleSwitchCamera() {
	    synchronized (mAsyncHandler) {
	        mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_TOUCH);
	        mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_CYCLEFLASH);
	        mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_TAKE_CLICK);
	        mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_RESTAT_CAMERA);
	        mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_SWITCH_CAMERA);
	        mAsyncHandler.sendEmptyMessage(ASYNC_MSG_WHAT_SWITCH_CAMERA);
        }
	}
	
	public void switchCamera(boolean toast, boolean openCamera, boolean updateFilter) {
		
			Log.d(TAG, "switchCamera()");
		//if( is_taking_photo && !is_taking_photo_on_timer ) {
		if( this.phase == PHASE_TAKING_PHOTO ) {
			// just to be safe - risk of cancelling the autofocus before taking a photo, or otherwise messing things up
			
				Log.d(TAG, "currently taking a photo");
			return;
		}
		int n_cameras = camera_controller_manager.getNumberOfCameras();
		
			Log.d(TAG, "found " + n_cameras + " cameras");
		if( n_cameras > 1 ) {
			closeCamera();
			cameraId = (cameraId+1) % n_cameras;
//			if (toast) {
//				Message msg = new Message();
//				msg.what = CameraFragment.MESSAGE_SHOW_TEXT;
//				if (camera_controller_manager.isFrontFacing(cameraId)) {
//					msg.obj = getResources().getString(R.string.front_camera);
//				} else {
//					msg.obj = getResources().getString(R.string.back_camera);
//				}
//				activity.getShowTextHandler().sendMessage(msg);
//			}
//		    activity.updateFlashButton();
//			activity.updateHDRButton();

			if (openCamera) {
				//zoom_factor = 0; // reset zoom when switching camera
				this.openCamera(updateFilter);

				// we update the focus, in case we weren't able to do it when switching video with a camera that didn't support focus modes
				setUpFocus(true);
			}
		}
		if (gpuImage.isPressed()) {
			gpuImage.delayPressUp();
		}
		//重新切换摄像头后,也得将画贴纸层的尺寸重置,
		activity.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				activity.initCanvasEmojiView();
//				mPhotoStickerCanvasEditEmojiView.reset();
			}
		});
	}
	
	private synchronized void matchPreviewFpsToVideo() {
		
			Log.d(TAG, "matchPreviewFpsToVideo()");
		if( !has_current_fps_range ) {
			// exit, as we don't have a current fps to reset back to later
			
				Log.d(TAG, "current fps not available");
			return;
		}
		CamcorderProfile profile = getCamcorderProfile();
		List<int []> fps_ranges = camera_controller.getSupportedPreviewFpsRange();
		if (fps_ranges == null) {
			
				Log.d(TAG, "fps_ranges not available");
			return;
		}
		int selected_min_fps = -1, selected_max_fps = -1, selected_diff = -1;
        for(int [] fps_range : fps_ranges) {
	    	 {
    			Log.d(TAG, "    supported fps range: " + fps_range[0] + " to " + fps_range[1]);
	    	}
			int min_fps = fps_range[0];
			int max_fps = fps_range[1];
			if( min_fps <= profile.videoFrameRate*1000 && max_fps >= profile.videoFrameRate*1000 ) {
    			int diff = max_fps - min_fps;
    			if( selected_diff == -1 || diff < selected_diff ) {
    				selected_min_fps = min_fps;
    				selected_max_fps = max_fps;
    				selected_diff = diff;
    			}
			}
        }
        if( selected_min_fps == -1 ) {
        	selected_diff = -1;
        	int selected_dist = -1;
            for(int [] fps_range : fps_ranges) {
    			int min_fps = fps_range[0];
    			int max_fps = fps_range[1];
    			int diff = max_fps - min_fps;
    			int dist = -1;
    			if( max_fps < profile.videoFrameRate*1000 )
    				dist = profile.videoFrameRate*1000 - max_fps;
    			else
    				dist = min_fps - profile.videoFrameRate*1000;
    	    	 {
        			Log.d(TAG, "    supported fps range: " + min_fps + " to " + max_fps + " has dist " + dist + " and diff " + diff);
    	    	}
    			if( selected_dist == -1 || dist < selected_dist || ( dist == selected_dist && diff < selected_diff ) ) {
    				selected_min_fps = min_fps;
    				selected_max_fps = max_fps;
    				selected_dist = dist;
    				selected_diff = diff;
    			}
            }
	    	
	    		Log.d(TAG, "    can't find match for fps range, so choose closest: " + selected_min_fps + " to " + selected_max_fps);
	        camera_controller.setPreviewFpsRange(selected_min_fps, selected_max_fps);
        }
        else {
	    	 {
    			Log.d(TAG, "    chosen fps range: " + selected_min_fps + " to " + selected_max_fps);
	    	}
	    	camera_controller.setPreviewFpsRange(selected_min_fps, selected_max_fps);
        }
	}
	
	public void toggleSwitchToVideo() {
	    synchronized (mAsyncHandler) {
	        mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_TOUCH);
	        mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_CYCLEFLASH);
	        mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_TAKE_CLICK);
	        mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_RESTAT_CAMERA);
	        mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_SWITCH_CAMERA);
	        mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_SWITCH_TO_PHOTO);
	        mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_SWITCH_TO_VIDEO);
	        mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_SWITCH_TO_BEAUTY);
			mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_SWITCH_TO_MOTION);
	        mAsyncHandler.sendEmptyMessage(ASYNC_MSG_WHAT_SWITCH_TO_VIDEO);
        }
	}

	public void toggleSwitchToMotion() {
		synchronized (mAsyncHandler) {
			mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_TOUCH);
			mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_CYCLEFLASH);
			mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_TAKE_CLICK);
			mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_RESTAT_CAMERA);
			mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_SWITCH_CAMERA);
			mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_SWITCH_TO_PHOTO);
			mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_SWITCH_TO_VIDEO);
			mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_SWITCH_TO_BEAUTY);
			mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_SWITCH_TO_MOTION);
			mAsyncHandler.sendEmptyMessage(ASYNC_MSG_WHAT_SWITCH_TO_MOTION);
		}
	}
	
	public void toggleSwitchToPhoto() {
	    synchronized (mAsyncHandler) {
	        mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_TOUCH);
	        mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_CYCLEFLASH);
	        mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_TAKE_CLICK);
	        mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_RESTAT_CAMERA);
	        mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_SWITCH_CAMERA);
	        mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_SWITCH_TO_PHOTO);
	        mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_SWITCH_TO_VIDEO);
	        mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_SWITCH_TO_BEAUTY);
			mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_SWITCH_TO_MOTION);
	        mAsyncHandler.sendEmptyMessage(ASYNC_MSG_WHAT_SWITCH_TO_PHOTO);
        }
	}
	
	public void toggleSwitchToBeauty() {
	    synchronized (mAsyncHandler) {
	        mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_TOUCH);
	        mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_CYCLEFLASH);
	        mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_TAKE_CLICK);
	        mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_RESTAT_CAMERA);
	        mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_SWITCH_CAMERA);
	        mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_SWITCH_TO_PHOTO);
	        mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_SWITCH_TO_VIDEO);
	        mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_SWITCH_TO_BEAUTY);
			mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_SWITCH_TO_MOTION);
	        mAsyncHandler.sendEmptyMessage(ASYNC_MSG_WHAT_SWITCH_TO_BEAUTY); 
        }
    }
	
    public synchronized void switchToVideo() {
		switchToVideoOrMotion(MODE_VIDEO);
    }

	public synchronized void switchToMotion() {
		switchToVideoOrMotion(MODE_MOTION);
	}

	public synchronized void switchToVideoOrMotion(int mode) {
		
			Log.d(TAG, "switchToVideo()");
		int preMode = mMode;
		if (isVideoOrMotion()) {
			
				Log.d(TAG, "current is video");
			stopVideo();
			setMode(mode);
			updatePreviewSize();
			startOverlayGone();
			return;
		}

		if (camera_controller == null && !isBeauty()) {
			
				Log.d(TAG, "camera not opened!");
			setMode(mode);
			startOverlayGone();
			return;
		}

		if (this.isOnTimer()) {
			cancelTimer();
		}
		else if (this.phase == PHASE_TAKING_PHOTO) {
			
				Log.d(TAG, "wait until photo taken");
			synchronized (mAsyncHandler) {
				if (mode == MODE_VIDEO) {
					mAsyncHandler.sendEmptyMessageDelayed(ASYNC_MSG_WHAT_SWITCH_TO_VIDEO, 200);
				} else {
					mAsyncHandler.sendEmptyMessageDelayed(ASYNC_MSG_WHAT_SWITCH_TO_MOTION, 200);
				}
			}
			return;
		}

		setMode(mode);

		if (!"flash_off".equals(mFlashValue)) {
			updateFlash("flash_off", false, false);
		}

		activity.getActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				activity.changeUIForStartVideo(false);
				activity.changeUIForResumeVideo(true);
			}
		});

		mFocusHelper.setFocusWaitingState(FocusHelper.FOCUS_WAITING_STATE_NONE);

		if (gpuImage.hasEffect()) {
			gpuImage.clearEffect();
			activity.getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {

				}
			});
		}

	    if (SettingsManager.getPreferenceSquare() || SettingsManager.getPreferenceRect()) {
			activity.updatePreviewMask();
		}

		if (gpuImage.hasHDREffect() || (isFiltMode() && !PhoneInfo.isSupportVideoFilter())) {
			mFilterId = -1;
			updateFilter();
		}

		if (PhoneInfo.isNotSupportVideoRender()) {
			mHandler.sendEmptyMessage(MSG_WHAT_CLEAR_AND_CHANGE_SURFACEVIEW);
		} else {
			setUpFocus(false); // don't do autofocus, as it'll be cancelled when
			// restarting preview
			updatePreviewSize();
		}
	}
    
    public synchronized void switchToPhoto() {
        
		Log.d(TAG, "switchToPhoto()");
        
        if (!isVideoOrMotion()) {
            
                Log.d(TAG, "current is photo");
			startOverlayGone();
            return;
        }
        
        if( camera_controller == null ) {
            
                Log.d(TAG, "camera not opened!");
			setMode(MODE_PHOTO);
			startOverlayGone();
            return;
        }
        if( this.isVideoOrMotion() ) {
			stopVideo();
			setMode(MODE_PHOTO);
			checkFlashState(true);
			if (SettingsManager.getPreferenceSquare() || SettingsManager.getPreferenceRect()) {
				activity.updatePreviewMask();
			}
		}

		if (isHDROn() && !supportHDR()) {
			updateFilter();
		}

		if (PhoneInfo.isNotSupportVideoRender()) {
			mHandler.sendEmptyMessage(MSG_WHAT_CLEAR_AND_CHANGE_SURFACEVIEW);
		} else {
			setUpFocus(false); // don't do autofocus, as it'll be cancelled when restarting preview

			updatePreviewSize();

			tryAutoFocus(false, false);
		}
		//重新切换视频或者拍照模式过后,也得将画贴纸层的尺寸给重置
		activity.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				activity.initCanvasEmojiView();
			}
		});
    }
    
    public synchronized void switchToBeauty() {

    }
	
	/**
	 * 更新预览界面大小
	 */
	private synchronized void updatePreviewSize() {
	    if( this.is_preview_started ) {
            camera_controller.stopPreview();
            this.is_preview_started = false;
        }
        setPreviewSize();
        if( !isVideoOrMotion() && has_current_fps_range ) {
            // if isVideoOrMotion() is true, we set the preview fps range in startCameraPreview()
            
                Log.d(TAG, "    reset preview to current fps range: " + current_fps_range[0] + " to " + current_fps_range[1]);
            camera_controller.setPreviewFpsRange(current_fps_range[0], current_fps_range[1]);
        }
        // always start the camera preview, even if it was previously paused
        this.startCameraPreview();
	}
	
	private String getErrorFeatures(CamcorderProfile profile) {
		boolean was_4k = false, was_bitrate = false, was_fps = false;
		if( profile.videoFrameWidth == 3840 && profile.videoFrameHeight == 2160 ) {
			was_4k = true;
		}
		String bitrate_value = SettingsManager.getPreferenceVideoBitrate();
		if( !bitrate_value.equals("default") ) {
			was_bitrate = true;
		}
		String fps_value = SettingsManager.getPreferenceVideoFps();
		if( !fps_value.equals("default") ) {
			was_fps = true;
		}
		String features = "";
		if( was_4k || was_bitrate || was_fps ) {
			if( was_4k ) {
				features = "4K UHD";
			}
			if( was_bitrate ) {
				if( features.length() == 0 )
					features = "Bitrate";
				else
					features += "/Bitrate";
			}
			if( was_fps ) {
				if( features.length() == 0 )
					features = "Frame rate";
				else
					features += "/Frame rate";
			}
		}
		return features;
	}

	public void toggleSycleFlash() {
        synchronized (mAsyncHandler) {
            if (!isSwitching()) {
                mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_CYCLEFLASH);
                mAsyncHandler.sendEmptyMessage(ASYNC_MSG_WHAT_CYCLEFLASH);
            }
        }
	}
	
	public void cycleFlash() {
	    if (isFrontCamera()) {
	        return;
	    }
	    if (isTakingPhotoOrOnTimer()) {
	        return;
	    }
	    String newValue;
	    String flashValue = mFlashValue;

		if (isFiltMode() || SettingsManager.getPreferenceHdrOn() || isVideoOrMotion()) {
			if ("flash_off".equals(flashValue)) {
				newValue = "flash_torch";
			} else {
				newValue = "flash_off";
			}
		} else {
			if ("flash_off".equals(flashValue)) {
				newValue = "flash_torch";
			} else if ("flash_torch".equals(flashValue)) {
				newValue = "flash_on";
			} else if ("flash_on".equals(flashValue)) {
				newValue = "flash_auto";
			} else if ("flash_auto".equals(flashValue)) {
				newValue = "flash_off";
			} else {
				newValue = "flash_off";
			}
		}
		if ("flash_torch".equals(newValue)) {
			if (updateFlash(newValue, true, false)) {
				if (!isVideoOrMotion()) {
					SettingsManager.setFlashValue("flash_off");
				}
			} else if (!isVideoOrMotion()) {
					updateFlash("flash_off", true, true);
			}
		} else if (!updateFlash(newValue, true, true)) {
			updateFlash("flash_off", true, true);
		}
	}

	private boolean checkFlashState(boolean restore) {
		if (isVideoOrMotion() && !"flash_torch".equals(mFlashValue)) {
			updateFlash("flash_off", false, false);
			return true;
		}
		if (isFrontCamera()) {
			updateFlash("flash_off", false, false);
			return true;
		}
		if ((SettingsManager.getPreferenceHdrOn() || isFiltMode())) {
			if (!"flash_torch".equals(mFlashValue)) {
				updateFlash("flash_off", false, false);
				return true;
			}
		}
		if (restore) {
			String flashValue = SettingsManager.getFlashValue();
			if (!updateFlash(flashValue, false, true)) {
				updateFlash("flash_off", false, true);
			}
		}
		return false;
	}

    private boolean updateFlash(String flash_value, boolean toast, boolean save) {
        
            Log.d(TAG, "updateFlash(): " + flash_value);

        // updates the Flash button, and Flash camera mode
        if (supported_flash_values != null && supported_flash_values.contains(flash_value)) {
            this.setFlash(flash_value, toast, save);
            return true;
        }
        return false;
    }

	public String getCurrentFlashValue() {
		return mFlashValue;
	}

	private synchronized void setFlash(String flash_value, boolean showToast, boolean save) {
		
			Log.d(TAG, "setFlash() " + flash_value);
		if( camera_controller == null ) {
			
				Log.d(TAG, "camera not opened!");
			return;
		}
		cancelAutoFocus();
		if (save) {
		    SettingsManager.setFlashValue(flash_value);
		}
		if (mFlashValue != null) {
			// 常亮切换到其他模式需要先设置为关闭，并且等待一段时间
			if (!mFlashValue.equals(flash_value) && mFlashValue.equals("flash_torch") && !flash_value.equals("flash_off")) {
				camera_controller.setFlashValue("flash_off");
				try {
					Thread.sleep(100);
				} catch (Throwable tr) {
				}
			}
		}
		mFlashValue = flash_value;
        camera_controller.setFlashValue(flash_value);
        activity.updateFlashValue(flash_value, showToast);
	}

	// this returns the flash mode indicated by the UI, rather than from the camera parameters (may be different, e.g., in startup autofocus!)
	/*public String getCurrentFlashMode() {
		if( current_flash_index == -1 )
			return null;
		String flash_value = supported_flash_values.get(current_flash_index);
		String flash_mode = convertFlashValueToMode(flash_value);
		return flash_mode;
	}*/

	// this returns the flash mode indicated by the UI, rather than from the camera parameters
	public synchronized String getCurrentFocusValue() {
		
			Log.d(TAG, "getCurrentFocusValue()");
		if( camera_controller == null ) {
			
				Log.d(TAG, "camera not opened!");
			return null;
		}
		String focous = camera_controller.getFocusValue();
		
            Log.d(TAG, "current focus value = " + focous);
		return focous;
	}

	private synchronized void setFocusValue(String focus_value, boolean clear, boolean auto_focus) {
		
			Log.d(TAG, "setFocusValue() " + focus_value);
		if( camera_controller == null ) {
			
				Log.d(TAG, "camera not opened!");
			return;
		}
		cancelAutoFocus();
        camera_controller.setFocusValue(focus_value);
        if (clear) {
            clearFocusAreas();
        }
		// n.b., we reset even for manual focus mode
		if( auto_focus ) {
			tryAutoFocus(false, false);
		}
	}

	private synchronized void takePicturePressed() {
		
			Log.d(TAG, "takePicturePressed");
		if( camera_controller == null ) {
			
				Log.d(TAG, "camera not opened!");
			/*is_taking_photo_on_timer = false;
			is_taking_photo = false;*/
			this.phase = PHASE_NORMAL;
			return;
		}
		if( !this.has_surface ) {
			
				Log.d(TAG, "preview surface not yet available");
			/*is_taking_photo_on_timer = false;
			is_taking_photo = false;*/
			this.phase = PHASE_NORMAL;
			return;
		}
		//if( is_taking_photo_on_timer ) {
		if( this.isOnTimer() ) {
			cancelTimer();
			return;
		}

		if (isVideoOrMotion()) {
			if (this.phase == PHASE_TAKING_PHOTO
					|| this.phase == PHASE_PAUSEING_VIDEO) {
				if( !video_start_time_set || System.currentTimeMillis() - video_start_time < 1000 ) {
					// if user presses to stop too quickly, we ignore
					// firstly to reduce risk of corrupt video files when stopping too quickly (see RuntimeException we have to catch in stopVideo),
					// secondly, to reduce a backlog of events which slows things down, if user presses start/stop repeatedly too quickly
					
						Log.d(TAG, "ignore pressing stop video too quickly after start");
				} else {
					stopVideo();
				}
				return;
			}
			// make sure that preview running (also needed to hide trash/share icons)
			this.startCameraPreview();
			takeVideo();
		} else {
			// make sure that preview running (also needed to hide trash/share icons)
			this.startCameraPreview();
			takePictureOrPending();
		}
	}
	
	private void takePictureOrPending() {
        //is_taking_photo = true;
        String timer_value = SettingsManager.getPreferenceTimer();
        long timer_delay = 0;
        try {
            timer_delay = Integer.parseInt(timer_value) * 1000;
        }
        catch(NumberFormatException e) {
            
                Log.e(TAG, "failed to parse preference_timer value: " + timer_value);
            e.printStackTrace();
            timer_delay = 0;
        }

		if( timer_delay == 0) {
            takePicture();
        } else {
            takePictureOnTimer(timer_delay);
        }
	}
	
	private void takePictureOnTimer(long timer_delay) {
		 {
			Log.d(TAG, "takePictureOnTimer");
			Log.d(TAG, "timer_delay: " + timer_delay);
		}
        this.phase = PHASE_TIMER;
		class TakePictureTimerTask extends TimerTask {
			public void run() {
				activity.getActivity().runOnUiThread(new Runnable() {
					public void run() {
						activity.updateDelayRemainingTime(0);
						// we run on main thread to avoid problem of camera closing at the same time
						// but still need to check that the camera hasn't closed or the task halted, since TimerTask.run() started
						if (camera_controller != null && takePictureTimerTask != null && !isVideoOrMotion())
							takePicture();
						else {
							
								Log.d(TAG, "takePictureTimerTask: don't take picture, as already cancelled");
						}
					}
				});
			}
		}
		take_photo_time = System.currentTimeMillis() + timer_delay;
		
			Log.d(TAG, "take photo at: " + take_photo_time);
		/*if( !repeated ) {
			showToast(take_photo_toast, R.string.started_timer);
		}*/
    	takePictureTimer.schedule(takePictureTimerTask = new TakePictureTimerTask(), timer_delay);
		mHandler.sendEmptyMessage(HANDLER_WHAT_UPDATE_DELAY_REMAINING_TIME);
	}
	
	private synchronized void takePicture() {
		
			Log.d(TAG, "takePicture");
        this.phase = PHASE_TAKING_PHOTO;
		if( camera_controller == null ) {
			
				Log.d(TAG, "camera not opened!");
			/*is_taking_photo_on_timer = false;
			is_taking_photo = false;*/
			this.phase = PHASE_NORMAL;
			return;
		}
		if( !this.has_surface ) {
			
				Log.d(TAG, "preview surface not yet available");
			/*is_taking_photo_on_timer = false;
			is_taking_photo = false;*/
			this.phase = PHASE_NORMAL;
			return;
		}

		updateParametersFromLocation();

        if (mFocusHelper.isAutoFocus() && mFocusHelper.isFocusWaiting()) {
            mFocusHelper.setFocusWaitingState(FocusHelper.FOCUS_WAITING_STATE_TAKE_PIC);
        } else {
            takePictureWhenFocused();
        }
	}

	private synchronized void takeVideo() {
		Log.d(TAG, "takeVideo");

		this.phase = PHASE_TAKING_PHOTO;
		if( camera_controller == null ) {
			Log.d(TAG, "camera not opened!");
			/*is_taking_photo_on_timer = false;
			is_taking_photo = false;*/
			this.phase = PHASE_NORMAL;
			return;
		}
		if( !this.has_surface ) {
			
				Log.d(TAG, "preview surface not yet available");
			/*is_taking_photo_on_timer = false;
			is_taking_photo = false;*/
			this.phase = PHASE_NORMAL;
			return;
		}

		updateParametersFromLocation();
		Log.d(TAG, "start video recording");
		File videoFile = FolderHelper.getOutputMediaFile(getContext(),
				isMotion() ? FolderHelper.MEDIA_TYPE_DYNAMIC : FolderHelper.MEDIA_TYPE_VIDEO);
		if (videoFile == null) {
			Log.e(TAG, "Couldn't create media video file; check storage permissions?");
			this.phase = PHASE_NORMAL;
		} else if ((isMotion() || isFiltMode()) && PhoneInfo.isSupportVideoFilter()) {
			boolean enable_sound = SettingsManager.getPreferenceShutterSound() && !isMotion();
			if (enable_sound && mSoundManager != null) {
				mSoundManager.playRecordSound(true);
			}
			video_name = videoFile.getAbsolutePath();
			SettingsManager.saveRecentlyVideoFile(video_name);
			
				Log.d(TAG, "save to: " + video_name);
			CamcorderProfile profile = getCamcorderProfile();
			gpuImage.startRecording(createFilter(true), isDadTwoInputFilter(), ui_rotation, videoFile,
					profile, null, !isMotion());
			
			Log.d(TAG, "video recorder started");
			video_start_time = System.currentTimeMillis();
			video_start_time_set = true;
			activity.getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					activity.changeUIForStartVideo(true);
					activity.changeUIForResumeVideo(true);
				}
			});
			if (isMotion()) {
				mHandler.sendEmptyMessage(HANDLER_WHAT_UPDATE_MOTION_PROGRESS);
			} else {
				mHandler.sendEmptyMessage(HANDLER_WHAT_UPDATE_RECORD_TIME);
			}
		} else {
			video_name = videoFile.getAbsolutePath();
			SettingsManager.saveRecentlyVideoFile(video_name);
			
				Log.d(TAG, "save to: " + video_name);

			CamcorderProfile profile = getCamcorderProfile();
			 {
				Log.d(TAG, "current_video_quality: " + current_video_quality);
				if (current_video_quality != -1)
					Log.d(TAG, "current_video_quality value: " + video_quality.get(current_video_quality).mQuality);
				Log.d(TAG, "resolution " + profile.videoFrameWidth + " x " + profile.videoFrameHeight);
				Log.d(TAG, "bit rate " + profile.videoBitRate);
				 {
					int[] fps_range = new int[2];
					camera_controller.getPreviewFpsRange(fps_range);
				}
			}

			video_recorder = new MediaRecorder();
			this.camera_controller.stopPreview(); // although not documented, we need to stop preview to prevent device freeze or video errors shortly after video recording starts on some devices (e.g., device freeze on Samsung Galaxy S2 - I could reproduce this on Samsung RTL; also video recording fails and preview becomes corrupted on Galaxy S3 variant "SGH-I747-US2"); also see http://stackoverflow.com/questions/4244999/problem-with-video-recording-after-auto-focus-in-android
			this.camera_controller.unlock();
			
				Log.d(TAG, "set video listeners");
			video_recorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
				@Override
				public void onInfo(MediaRecorder mr, int what, int extra) {
					
						Log.d(TAG, "MediaRecorder info: " + what + " extra: " + extra);
					if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED || what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
						final int final_what = what;
						final int final_extra = extra;
						// we run on main thread to avoid problem of camera closing at the same time
						String debug_value = "info_" + final_what + "_" + final_extra;
						Log.e(TAG, debug_value);
						stopVideo();
					}
				}
			});
			video_recorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
				public void onError(MediaRecorder mr, int what, int extra) {
					 {
						Log.e(TAG, "MediaRecorder error: " + what + " extra: " + extra);
					}
					pauseVideo();
				}
			});
			camera_controller.initVideoRecorder(video_recorder);
			if (!isMotion()) {
				String pref_audio_src = SettingsManager.getPreferenceRecordAudioSrc();
				
					Log.d(TAG, "pref_audio_src: " + pref_audio_src);
				int audio_source = MediaRecorder.AudioSource.CAMCORDER;
				if (pref_audio_src.equals("audio_src_mic")) {
					audio_source = MediaRecorder.AudioSource.MIC;
				}
				
					Log.d(TAG, "audio_source: " + audio_source);
				video_recorder.setAudioSource(audio_source);
			}
			
				Log.d(TAG, "set video source");
			video_recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

				Log.d(TAG, "set video profile");
			if (!isMotion()) {
				video_recorder.setProfile(profile);
			} else {
				// from http://stackoverflow.com/questions/5524672/is-it-possible-to-use-camcorderprofile-without-audio-source
				video_recorder.setOutputFormat(profile.fileFormat);
				video_recorder.setVideoFrameRate(profile.videoFrameRate);
				video_recorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
				video_recorder.setVideoEncodingBitRate(profile.videoBitRate);
				video_recorder.setVideoEncoder(profile.videoCodec);
			}
			 {
				Log.d(TAG, "video fileformat: " + profile.fileFormat);
				Log.d(TAG, "video framerate: " + profile.videoFrameRate);
				Log.d(TAG, "video size: " + profile.videoFrameWidth + " x " + profile.videoFrameHeight);
				Log.d(TAG, "video bitrate: " + profile.videoBitRate);
				Log.d(TAG, "video codec: " + profile.videoCodec);
			}

			video_recorder.setOutputFile(video_name);
			try {
					/*if( true ) // test
	        			throw new IOException();*/
				if (PhoneInfo.isNotSupportVideoRender()) {
					video_recorder.setPreviewDisplay(mHolder.getSurface());
				}
				video_recorder.setOrientationHint(getCurrentRotation());
				video_recorder.prepare();
				
					Log.d(TAG, "about to start video recorder");
				video_recorder.start();
				
					Log.d(TAG, "video recorder started");
				video_start_time = System.currentTimeMillis();
				video_start_time_set = true;
				activity.getActivity().runOnUiThread(new Runnable() {

					@Override
					public void run() {
						activity.changeUIForStartVideo(true);
						activity.changeUIForResumeVideo(true);
					}
				});
				if (isMotion()) {
					mHandler.sendEmptyMessage(HANDLER_WHAT_UPDATE_MOTION_PROGRESS);
				} else {
					mHandler.sendEmptyMessage(HANDLER_WHAT_UPDATE_RECORD_TIME);
				}
			} catch (IOException e) {
				
					Log.e(TAG, "failed to save video");
				e.printStackTrace();
				video_recorder.reset();
				video_recorder.release();
				video_recorder = null;
					/*is_taking_photo = false;
					is_taking_photo_on_timer = false;*/
				this.phase = PHASE_NORMAL;
				this.reconnectCamera(true);
			} catch (RuntimeException e) {
				// needed for emulator at least - although MediaRecorder not meant to work with emulator, it's good to fail gracefully
				
					Log.e(TAG, "runtime exception starting video recorder", e);
				video_recorder.reset();
				video_recorder.release();
				video_recorder = null;
					/*is_taking_photo = false;
					is_taking_photo_on_timer = false;*/
				this.phase = PHASE_NORMAL;
				this.reconnectCamera(true);
			}
			if (!PhoneInfo.isNotSupportVideoRender() && PhoneInfo.isNotSupportOES()) {
				gpuImage.setUpCamera(camera_controller.getCamera());
			}
		}
		if (isVideo()) {
		}
	}

	private synchronized void takePictureWhenFocused() {
		// should be called when auto-focused
		
			Log.d(TAG, "takePictureWhenFocused");
		if( camera_controller == null ) {
			
				Log.d(TAG, "camera not opened!");
			/*is_taking_photo_on_timer = false;
			is_taking_photo = false;*/
			this.phase = PHASE_NORMAL;
			return;
		}
		if( !this.has_surface ) {
			
				Log.d(TAG, "preview surface not yet available");
			/*is_taking_photo_on_timer = false;
			is_taking_photo = false;*/
			this.phase = PHASE_NORMAL;
			return;
		}

		String focus_value = getCurrentFocusValue();
		 {
			Log.d(TAG, "focus_value is " + focus_value);
			Log.d(TAG, "focus_success is " + mFocusHelper.getFocusState());
		}

        mFocusHelper.setFocusWaitingState(FocusHelper.FOCUS_WAITING_STATE_NONE);
    	{
    		camera_controller.setRotation(getCurrentRotation());
    		capture_rotation = current_orientation;
    		
			boolean enable_sound = SettingsManager.getPreferenceShutterSound();
    		
    			Log.d(TAG, "about to call takePicture, enable_sound? " + enable_sound);
    		try {
    			initDrawPhotoFrameData();
    			enableDrawCaptureFrame();
                if (isFiltMode()) {
					if (gpuImage.isPressed()) {
						gpuImage.pressUp();
						try {
							Thread.sleep(100);
						} catch (Throwable tr) {
						}
					}
                    if (enable_sound && mSoundManager != null) {
                        mSoundManager.playShutterSound();
                    }
                    captureFiltFrame = true;
                } else {
                    boolean ringerMute = mSoundManager.isRingerMute();
                    if (can_disable_shutter_sound) {
                        camera_controller.enableShutterSound(enable_sound);
                    } else if (!enable_sound) {
                        mSoundManager.muteRinger();
                    }
                    if (isFiltMode()) {
                        try {
                            Thread.sleep(100);
                        } catch (Throwable tr) {
                        }
                    }
                    camera_controller.takePicture(null, jpegPictureCallback, enable_sound
                            && !ringerMute);
                }
        		count_cameraTakePicture++;
    			//showToast(take_photo_toast, toast_text);
    		}
    		catch(RuntimeException e) {
    			// just in case? We got a RuntimeException report here from 1 user on Google Play; I also encountered it myself once of Galaxy Nexus when starting up
    			
					Log.e(TAG, "runtime exception from takePicture");
    			e.printStackTrace();
				this.phase = PHASE_NORMAL;
				disableDrawCaptureFrame();
	            startCameraPreview();
    		}
    	}
		
			Log.d(TAG, "takePicture exit");
    }
	
	private void processPictureTaken(byte[] data, Bitmap bitmap, int mode) {
        // n.b., this is automatically run in a different thread
        System.gc();
        disableDrawCaptureFrame();

		boolean image_capture_intent = false;
		String action = activity.getActivity().getIntent().getAction();
		if(MediaStore.ACTION_IMAGE_CAPTURE.equals(action) ||
				MediaStore.ACTION_IMAGE_CAPTURE_SECURE.equals(action)) {
			image_capture_intent = true;
		}

		int uiRotation = 360 - ui_rotation;
        int rotation = uiRotation;
		boolean flipHorizontal = false;

		/**
		 * 前置摄像头自动镜像
		 */
		if (isFrontCamera()) {
			if (!SettingsManager.getPreferenceMirrorFrontCamera()
					&& bitmap != null) {
				flipHorizontal = true;
			}
			if (SettingsManager.getPreferenceMirrorFrontCamera()
					&& bitmap == null) {
				rotation = Exif.getOrientation(data);
				bitmap = BitmapUtils.decodeJpegData(data);
				flipHorizontal = true;
			}
		}

        /**
         * 图片旋转
         */
        if (bitmap != null && (rotation != 0 || flipHorizontal)) {
            Bitmap tempBp = ImageHelper.rotating(bitmap, rotation, flipHorizontal, false);
            if (tempBp != null) {
                bitmap = tempBp;
                rotation = 0;
            }
        }

		/**
		 * 1:1裁剪
		 */
		if (((SettingsManager.getPreferenceSquare() && Math.abs(aspect_ratio - 1f) > 0.01f)
				|| (SettingsManager.getPreferenceRect() && Math.abs(aspect_ratio - 4f / 3f) > 0.01f))
				&& mode != MODE_VIDEO) {
			float ratio = 1f;
			if (SettingsManager.getPreferenceRect()) {
				ratio = 3f/ 4f;
			}
			if (uiRotation == 90 || uiRotation == 270) {
				ratio = 1f / ratio;
			}
			if (bitmap != null) {
				bitmap = BitmapUtils.cropBitmap(bitmap, ratio);
			} else {
				if (mode != MODE_BEAUTY && !image_capture_intent) {
					BitmapRegionDecoder decoder = null;
					try {
						rotation = Exif.getOrientation(data);
						decoder = BitmapRegionDecoder.newInstance(data, 0, data.length, false);
						int width = decoder.getWidth();
						int height = decoder.getHeight();
						if (rotation == 90 || rotation == 270) {
							ratio = 1f/ ratio;
						}
						Rect rect = BitmapUtils.cropRect(new Rect(0, 0, width, height), ratio);
						bitmap = decoder.decodeRegion(rect, null);
					} catch (Throwable tr) {
						Log.d(TAG, "", tr);
					} finally {
						if (decoder != null) {
							decoder.recycle();
						}
					}
				}
				if (bitmap == null) {
					rotation = Exif.getOrientation(data);
					bitmap = BitmapUtils.decodeJpegDataBig(data, rotation);
					bitmap = BitmapUtils.cropBitmap(bitmap, ratio);
				}
			}
		}

		/**
		 * 添加时间水印
		 */
		if (DateMaskUtil.getDataMarkOpen()) {
			if (bitmap == null) {
				rotation = Exif.getOrientation(data);
				bitmap = BitmapUtils.decodeJpegDataBig(data, rotation);
			}
			if (bitmap != null) {
				bitmap = BitmapUtils.getWaterMarkBitmap(bitmap, DateMaskUtil.getCurSettingFormat(), rotation);
			}
		}

        /**
         * 获取图片宽高
         */
        int width, height;
        if (bitmap == null) {
            com.jb.zcamera.exif.ExifInterface exif = Exif.getExif(data);
            rotation = Exif.getOrientation(exif);
            width = Exif.getWidth(exif);
            height = Exif.getHeight(exif);
			if (width == 0 || height == 0) {
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inSampleSize = 1;
				options.inJustDecodeBounds = true;
				BitmapFactory.decodeStream(new ByteArrayInputStream(data), null, options);
				width = options.outWidth;
				height = options.outHeight;
			}
        } else {
            width = bitmap.getWidth();
            height = bitmap.getHeight();
        }

        Uri image_capture_intent_uri = null;
        if(image_capture_intent) {
            
                Log.d(TAG, "from image capture intent");
			if (activity != null && activity.getActivity() != null
					&& activity.getActivity().getIntent() != null) {
				Bundle myExtras = activity.getActivity().getIntent().getExtras();
				if (myExtras != null) {
					image_capture_intent_uri = (Uri) myExtras.getParcelable(MediaStore.EXTRA_OUTPUT);
					
						Log.d(TAG, "save to: " + image_capture_intent_uri);
				}
			}
        }

        boolean success = false;
        
        String picFileName = null;
        File picFile = null;

		boolean isExtSdcard = false;
        try {
            OutputStream outputStream = null;
            if( image_capture_intent ) {
                
                    Log.d(TAG, "image_capture_intent");
                if( image_capture_intent_uri != null )
                {
                    // Save the bitmap to the specified URI (use a try/catch block)
                    
                        Log.d(TAG, "save to: " + image_capture_intent_uri);
                    outputStream = activity.getActivity().getContentResolver().openOutputStream(image_capture_intent_uri);
                }
                else
                {
                    // If the intent doesn't contain an URI, send the bitmap as a parcel
                    // (it is a good idea to reduce its size to ~50k pixels before)
                    
                        Log.d(TAG, "sent to intent via parcel");
                    if( bitmap == null ) {
                        
                            Log.d(TAG, "create bitmap");
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        //options.inMutable = true;
                        options.inPurgeable = true;
                        bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
                    }
                    if( bitmap != null ) {
                         {
                            Log.d(TAG, "decoded bitmap size " + width + ", " + height);
                            Log.d(TAG, "bitmap size: " + width*height*4);
                        }
                        final int small_size_c = 128;
                        if( width > small_size_c ) {
                            float scale = ((float)small_size_c)/(float)width;
                            
                                Log.d(TAG, "scale to " + scale);
                            Matrix matrix = new Matrix();
                            matrix.postScale(scale, scale);
                            Bitmap new_bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
                            // careful, as new_bitmap is sometimes not a copy!
                            if( new_bitmap != bitmap ) {
                                bitmap.recycle();
                                bitmap = new_bitmap;
                            }
                        }
                    }
                     {
                        Log.d(TAG, "returned bitmap size " + bitmap.getWidth() + ", " + bitmap.getHeight());
                        Log.d(TAG, "returned bitmap size: " + bitmap.getWidth()*bitmap.getHeight()*4);
                    }
                    finishActivity(Activity.RESULT_OK, new Intent("inline-data").putExtra("data", bitmap));
                }
            }
            else {
				if(FolderHelper.isExtSdcardImagePath()){
					isExtSdcard = true;
					picFile = FolderHelper.getOutputMediaFile(getContext(), FolderHelper.MEDIA_TYPE_IMAGE);
					picFileName = picFile.getAbsolutePath();
					outputStream = ExtSdcardUtils.getExtCardOutputStream(getContext(), picFileName, "");//不传入mimeType防止它直接加入Media数据库
				} else {
					picFile = FolderHelper.getOutputMediaFile(getContext(), FolderHelper.MEDIA_TYPE_IMAGE);

					if (picFile == null) {
						Log.e(TAG, "Couldn't create media image file; check storage permissions?");
					} else {
						picFileName = picFile.getAbsolutePath();
						
							Log.d(TAG, "save to: " + picFileName);
						outputStream = new FileOutputStream(picFile);
					}
				}
            }

			if (outputStream != null) {
				if (isExtSdcard) {
					if (bitmap != null) {
						int image_quality = getImageQuality();
						byte[] jpeg = BitmapUtils.bmpToJPGByteArray(bitmap, image_quality, false);
						ByteArrayOutputStream output = new ByteArrayOutputStream();

						com.jb.zcamera.exif.ExifInterface exif = new com.jb.zcamera.exif.ExifInterface();
						ExifTag widthTg = exif.buildTag(com.jb.zcamera.exif.ExifInterface.TAG_IMAGE_WIDTH, bitmap.getWidth());
						ExifTag heightTg = exif.buildTag(com.jb.zcamera.exif.ExifInterface.TAG_IMAGE_LENGTH, bitmap.getHeight());
						ExifTag orientationTg = exif.buildTag(com.jb.zcamera.exif.ExifInterface.TAG_ORIENTATION, com.jb.zcamera.exif.ExifInterface.getOrientationValueForRotation(rotation));
						String timeStamp = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).format(new Date(System.currentTimeMillis()));
						ExifTag dataTimeTg = exif.buildTag(com.jb.zcamera.exif.ExifInterface.TAG_DATE_TIME, timeStamp);
						ExifTag makeTg = exif.buildTag(com.jb.zcamera.exif.ExifInterface.TAG_MAKE, "S Photo Editor");

						exif.setTag(widthTg);
						exif.setTag(heightTg);
						exif.setTag(orientationTg);
						exif.setTag(dataTimeTg);
						exif.setTag(makeTg);
						exif.writeExif(jpeg, output);

						byte[] buffer = output.toByteArray();
						outputStream.write(buffer);

					} else {
						com.jb.zcamera.exif.ExifInterface exif = new com.jb.zcamera.exif.ExifInterface();
						exif.readExif(data);//需要先读取原来的
						ByteArrayOutputStream output = new ByteArrayOutputStream();

						String timeStamp = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).format(new Date(System.currentTimeMillis()));
						ExifTag dataTimeTg = exif.buildTag(com.jb.zcamera.exif.ExifInterface.TAG_DATE_TIME, timeStamp);
						ExifTag makeTg = exif.buildTag(com.jb.zcamera.exif.ExifInterface.TAG_MAKE, "S Photo Editor");
						exif.setTag(dataTimeTg);
						exif.setTag(makeTg);
						exif.writeExif(data, output);

						byte[] buffer = output.toByteArray();
						outputStream.write(buffer);
					}
					outputStream.flush();
					outputStream.close();
					
						Log.d(TAG, "onPictureTaken saved photo");

					success = true;
					if (picFile != null) {
						long nowTime = System.currentTimeMillis();
						activity.startGallaryLoading();
						FolderHelper.asynAddImage(activity.getActivity(), picFile.getName(),
								"image/jpeg", nowTime, null, rotation,
								(int) picFile.length(), picFile.getAbsolutePath(), width, height,
								onScanCompletedListener);
						test_last_saved_image = picFileName;
					}
				} else{
					if (bitmap != null) {
						int image_quality = getImageQuality();
						bitmap.compress(Bitmap.CompressFormat.JPEG, image_quality, outputStream);
					} else {
						outputStream.write(data);
					}
					outputStream.close();
					
						Log.d(TAG, "onPictureTaken saved photo");

					success = true;
					if (picFile != null) {
						long nowTime = System.currentTimeMillis();
						if (bitmap != null) {
							FolderHelper.setExif(picFile, width, height, rotation, nowTime, null);
						} else {
							FolderHelper.setExif(picFile, nowTime);
						}
						activity.startGallaryLoading();
						FolderHelper.asynAddImage(activity.getActivity(), picFile.getName(),
								"image/jpeg", nowTime, null, rotation,
								(int) picFile.length(), picFile.getAbsolutePath(), width, height,
								onScanCompletedListener);
						test_last_saved_image = picFileName;
					}
				}
				if (image_capture_intent) {
					finishActivity(Activity.RESULT_OK, null);
				}
			}
        }
        catch(FileNotFoundException e) {
            
                Log.e(TAG, "File not found: " + e.getMessage());
            e.getStackTrace();
        }
        catch(IOException e) {
            
                Log.e(TAG, "I/O error writing file: " + e.getMessage());
            e.getStackTrace();
        }

        
            Log.d(TAG, "onPictureTaken started preview");

        // I have received crashes where camera_controller was null - could perhaps happen if this thread was running just as the camera is closing?
        if( success && picFile != null ) {
            // update thumbnail - this should be done after restarting preview, so that the preview is started asap
            long time_s = System.currentTimeMillis();
            Bitmap thumbnail = bitmap;
            if (thumbnail == null && data != null) {
                Size size = null;
                synchronized (Preview.this) {
                    if (camera_controller != null) {
                        size = camera_controller.getPictureSize();
                    }
                }
                
                if (size != null) {
                    int previewWidth = getSurfaceView().getWidth();
                    int ratio = (int) Math.ceil((double) size.width / previewWidth);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inMutable = false;
                    options.inPurgeable = true;
                    options.inSampleSize = Integer.highestOneBit(ratio) * 4; // * 4 to increase performance, without noticeable loss in visual quality
                    if( !SettingsManager.getPreferenceThumbnailAnimation()) {
                        // can use lower resolution if we don't have the thumbnail animation
                        options.inSampleSize *= 4;
                    }
                     {
                        Log.d(TAG, "    picture width   : " + size.width);
                        Log.d(TAG, "    preview width   : " + previewWidth);
                        Log.d(TAG, "    ratio           : " + ratio);
                        Log.d(TAG, "    inSampleSize    : " + options.inSampleSize);
                    }
                    thumbnail = BitmapFactory.decodeByteArray(data, 0, data.length, options);
                }
            }

            Log.d(TAG, "    thumbnail orientation: " + rotation);

            Log.d(TAG, "    time to create thumbnail: " + (System.currentTimeMillis() - time_s));
        }
        System.gc();
	}
	
	private void finishActivity(final int resultCode, final Intent data) {
		if (activity != null && activity.getActivity() != null) {
			activity.getActivity().runOnUiThread(new Runnable() {

				@Override
				public void run() {
					activity.getActivity().setResult(resultCode, data);
					activity.getActivity().finish();
				}
			});
		}
	}
	
	private void setGPSDirectionExif(ExifInterface exif) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());
    	if( this.has_geo_direction && SettingsManager.getPreferenceGpsDirection()) {
			float geo_angle = (float) Math.toDegrees(Preview.this.geo_direction[0]);
			if( geo_angle < 0.0f ) {
				geo_angle += 360.0f;
			}
			
				Log.d(TAG, "save geo_angle: " + geo_angle);
			// see http://www.sno.phy.queensu.ca/~phil/exiftool/TagNames/GPS.html
			String GPSImgDirection_string = Math.round(geo_angle*100) + "/100";
			
				Log.d(TAG, "GPSImgDirection_string: " + GPSImgDirection_string);
		   	exif.setAttribute(TAG_GPS_IMG_DIRECTION, GPSImgDirection_string);
		   	exif.setAttribute(TAG_GPS_IMG_DIRECTION_REF, "M");
    	}
	}

	private void setDateTimeExif(ExifInterface exif) {
    	String exif_datetime = exif.getAttribute(ExifInterface.TAG_DATETIME);
    	if( exif_datetime != null ) {
        	
    			Log.d(TAG, "write datetime tags: " + exif_datetime);
        	exif.setAttribute("DateTimeOriginal", exif_datetime);
        	exif.setAttribute("DateTimeDigitized", exif_datetime);
    	}
	}
	
    public synchronized void tryAutoFocus(final boolean startup, final boolean manual) {
    	// manual: whether user has requested autofocus (e.g., by touching screen, or volume focus, or hardware focus button)
    	// consider whether you want to call requestAutoFocus() instead (which properly cancels any in-progress auto-focus first)
		 {
			Log.d(TAG, "tryAutoFocus");
			Log.d(TAG, "startup? " + startup);
			Log.d(TAG, "manual? " + manual);
		}
		if (canAutoFocus()) {
//		    if (manual) {
//                cancelAutoFocus();
//            }
		    
                Log.d(TAG, "try to start autofocus");
            mFocusHelper.setFocusComplete(FocusHelper.FOCUS_WAITING, -1);
            
            
                Log.d(TAG, "set focus_success to " + mFocusHelper.getFocusState());
            try {
                camera_controller.autoFocus(autoFocusCallback);
                count_cameraAutoFocus++;
                
                    Log.d(TAG, "autofocus started");
            }
            catch(RuntimeException e) {
                // just in case? We got a RuntimeException report here from 1 user on Google Play
                autoFocusCallback.onAutoFocus(false);

                
                    Log.e(TAG, "runtime exception from autoFocus");
				e.printStackTrace();
            }
		}
    }
    
    private synchronized void tryVideoAutoFocus() {
        if (isVideoFocasAuto() && camera_controller != null) {
            try {
                camera_controller.autoFocus(new CameraController.AutoFocusCallback() {
                    
                    @Override
                    public void onAutoFocus(boolean success) {
                        
                            Log.d(TAG, "video autofocus callback success=" + success);
                    }
                });
                count_cameraAutoFocus++;
                
                    Log.d(TAG, "video autofocus started");
            } catch(Throwable tr) {
				Log.e(TAG, "", tr);
            }
        }
    }
    
    /**
     * 当前是否能自动对焦
     * 
     * @return
     */
    private boolean canAutoFocus() {
        if (isVideoOrMotion()) {
            return false;
        } else if( !this.has_surface ) {
            
                Log.d(TAG, "preview surface not yet available");
            return false;
        }
        else if( !this.is_preview_started ) {
//            
//                Log.d(TAG, "preview not yet started");
            return false;
        }
        //else if( is_taking_photo ) {
        else if( this.isTakingPhotoOrOnTimer() ) {
            // if taking a video, we allow manual autofocuses
            // autofocus may cause problem if there is a video corruption problem, see testTakeVideoBitrate() on Nexus 7 at 30Mbs or 50Mbs, where the startup autofocus would cause a problem here
            
                Log.d(TAG, "currently taking a photo");
            return false;
        }
        else if (!mFocusHelper.isAutoFocus()) {
            
                Log.d(TAG, "not auto focus mode");
            return false;
        } else if (mFocusHelper.isFocusWaiting()) {
//            
//                Log.d(TAG, "currently focus waiting");
            return false;
        } else if (activity.isSeekbarTouching()) {
            return false;
        }
        return true;
    }
    
    private boolean isVideoFocasAuto() {
        if (isVideoOrMotion()) {
            return mFocusHelper.isAutoFocus();
        }
        return false;
    }
    
    private synchronized void cancelAutoFocus() {
        
            Log.d(TAG, "cancelAutoFocus");
        if( camera_controller != null ) {
            try {
                camera_controller.cancelAutoFocus();
            }
            catch(RuntimeException e) {
                // had a report of crash on some devices, see comment at https://sourceforge.net/p/opencamera/tickets/4/ made on 20140520
                
                    Log.d(TAG, "cancelAutoFocus() failed");
                e.printStackTrace();
            }
            autoFocusCompleted(false, false, true);
        }
    }
    
    private synchronized void autoFocusCompleted(boolean manual, boolean success, boolean cancelled) {
		 {
			Log.d(TAG, "autoFocusCompleted");
			Log.d(TAG, "    manual? " + manual);
			Log.d(TAG, "    success? " + success);
			Log.d(TAG, "    cancelled? " + cancelled);
		}
        if (cancelled) {
            mFocusHelper.setFocusState(FocusHelper.FOCUS_DONE);
        } else {
            mFocusHelper.setFocusComplete(success ? FocusHelper.FOCUS_SUCCESS
                    : FocusHelper.FOCUS_FAILED, System.currentTimeMillis());
        }
        if (!isVideoOrMotion()) {
            if (mFocusHelper.getFocusWaitingState() == FocusHelper.FOCUS_WAITING_STATE_TAKE_PIC) {
                takePictureWhenFocused();
            } else if (mFocusHelper.getFocusWaitingState() == FocusHelper.FOCUS_WAITING_STATE_TAKE_OR_PENDING) {
                takePictureOrPending();
            }
        }
        mFocusHelper.setFocusWaitingState(FocusHelper.FOCUS_WAITING_STATE_NONE);
    }
    
    private synchronized void startCameraPreview() {
        disableDrawCaptureFrame();
        long debug_time = 0;
         {
            Log.d(TAG, "startCameraPreview");
            debug_time = System.currentTimeMillis();
        }
        //if( camera != null && !is_taking_photo && !is_preview_started ) {
        if( camera_controller != null && !this.isTakingPhotoOrOnTimer() && !is_preview_started 
                && camera_controller.getCamera() != null) {
            
                Log.d(TAG, "starting the camera preview");
            {
                
                    Log.d(TAG, "setRecordingHint: " + isVideoOrMotion());
                camera_controller.setRecordingHint(this.isVideoOrMotion() && !isFrontCamera());
            }
            if( this.isVideoOrMotion() ) {
                matchPreviewFpsToVideo();
            }
            // else, we reset the preview fps to default in switchVideo
            try {
                int displayOrientation = camera_controller.getDisplayOrientation();
                boolean isFrontFacing = camera_controller_manager.isFrontFacing(cameraId);
                 {
                    Log.d(TAG, "displayOrientation=" + displayOrientation + " isFrontFacing=" + isFrontFacing);
                }
                if (isFrontFacing) {
                    displayOrientation = (360 - displayOrientation) % 360;
                }
				if (isVideoOrMotion() && PhoneInfo.isNotSupportVideoRender()) {
					camera_controller.startPreview();
					startOverlayGone();
				} else {
					gpuImage.setUpCamera(this, displayOrientation, isFrontFacing, false);
				}
                count_cameraStartPreview++;
            }
            catch(RuntimeException e) {
                
                    Log.d(TAG, "RuntimeException trying to startPreview");
                e.printStackTrace();
                return;
            }
            this.is_preview_started = true;
             {
                Log.d(TAG, "time after starting camera preview: " + (System.currentTimeMillis() - debug_time));
            }
        }
        this.setPreviewPaused(false);
    }

	public void startOverlayGone() {
		activity.startOverlayGone();
	}

    private void setPreviewPaused(boolean paused) {
		
			Log.d(TAG, "setPreviewPaused: " + paused);
		/*is_preview_paused = paused;
		if( is_preview_paused ) {*/
	    if( paused ) {
	    	this.phase = PHASE_PREVIEW_PAUSED;
		    // shouldn't call showGUI(false), as should already have been disabled when we started to take a photo (or above when exiting immersive mode)
		}
		else {
	    	this.phase = PHASE_NORMAL;
		}
    }

    public void onAccelerometerSensorChanged(SensorEvent event) {
		/*
    	Log.d(TAG, "onAccelerometerSensorChanged: " + event.values[0] + ", " + event.values[1] + ", " + event.values[2]);*/

    	this.has_gravity = true;
    	for(int i=0;i<3;i++) {
    		//this.gravity[i] = event.values[i];
    		this.gravity[i] = sensor_alpha * this.gravity[i] + (1.0f-sensor_alpha) * event.values[i];
    	}
    	calculateGeoDirection();
    	
		double x = gravity[0];
		double y = gravity[1];
		this.has_level_angle = true;
		this.level_angle = Math.atan2(-x, y) * 180.0 / Math.PI;
		if( this.level_angle < -0.0 ) {
			this.level_angle += 360.0;
		}
		this.orig_level_angle = this.level_angle;
		this.level_angle -= (float)this.current_orientation;
		if( this.level_angle < -180.0 ) {
			this.level_angle += 360.0;
		}
		else if( this.level_angle > 180.0 ) {
			this.level_angle -= 360.0;
		}

		getSurfaceView().invalidate();
	}

    public void onMagneticSensorChanged(SensorEvent event) {
    	this.has_geomagnetic = true;
    	for(int i=0;i<3;i++) {
    		//this.geomagnetic[i] = event.values[i];
    		this.geomagnetic[i] = sensor_alpha * this.geomagnetic[i] + (1.0f-sensor_alpha) * event.values[i];
    	}
    	calculateGeoDirection();
    }
    
    private void calculateGeoDirection() {
    	if( !this.has_gravity || !this.has_geomagnetic ) {
    		return;
    	}
    	if( !SensorManager.getRotationMatrix(this.deviceRotation, this.deviceInclination, this.gravity, this.geomagnetic) ) {
    		return;
    	}
        SensorManager.remapCoordinateSystem(this.deviceRotation, SensorManager.AXIS_X, SensorManager.AXIS_Z, this.cameraRotation);
    	this.has_geo_direction = true;
    	SensorManager.getOrientation(cameraRotation, geo_direction);
    	//SensorManager.getOrientation(deviceRotation, geo_direction);
		/* {
			Log.d(TAG, "geo_direction: " + (geo_direction[0]*180/Math.PI) + ", " + (geo_direction[1]*180/Math.PI) + ", " + (geo_direction[2]*180/Math.PI));
		}*/
    }
    
    public boolean supportsFaceDetection() {
		
			Log.d(TAG, "supportsFaceDetection");
    	return supports_face_detection;
    }
    
    public boolean supportsVideoStabilization() {
		
			Log.d(TAG, "supportsVideoStabilization");
    	return supports_video_stabilization;
    }
    
    public boolean canDisableShutterSound() {
		
			Log.d(TAG, "canDisableShutterSound");
    	return true;
    }

    public List<String> getSupportedSceneModes() {
		
			Log.d(TAG, "getSupportedSceneModes");
		return this.scene_modes;
    }

    public List<Size> getSupportedPreviewSizes() {
		
			Log.d(TAG, "getSupportedPreviewSizes");
    	return this.supported_preview_sizes;
    }
    
    public List<Size> getSupportedPictureSizes() {
		
			Log.d(TAG, "getSupportedPictureSizes");
		return this.sizes;
    }
    
    public int getCurrentPictureSizeIndex() {
		
			Log.d(TAG, "getCurrentPictureSizeIndex");
    	return this.current_size_index;
    }
    
    public List<VideoQuality> getSupportedVideoQuality() {
		
			Log.d(TAG, "getSupportedVideoQuality");
		return this.video_quality;
    }
    
    public List<Size> getSupportedVideoSizes() {
		
			Log.d(TAG, "getSupportedVideoSizes");
		return this.video_sizes;
    }
    
	public List<String> getSupportedFlashValues() {
		return supported_flash_values;
	}

    public int getCameraId() {
    	return this.cameraId;
    }
    
    private int getImageQuality(){
		
			Log.d(TAG, "getImageQuality");
		String image_quality_s = SettingsManager.getPreferenceQuality();
		int image_quality = 0;
		try {
			image_quality = Integer.parseInt(image_quality_s);
		}
		catch(NumberFormatException exception) {
			
				Log.e(TAG, "image_quality_s invalid format: " + image_quality_s);
			image_quality = 100;
		}
		return image_quality;
    }
    
    public void onStart() {
        this.app_is_stoped = false;
		asyncOpenCamera();
    }
    
    public void onResume() {
        Log.d(TAG, "onResume");
		mFocusDistanceChecker.register();
		mKeyPressed = false;
    }

    public void onPause() {
		
			Log.d(TAG, "onPause");
		mFocusDistanceChecker.unRegister();
		mSoundManager.restoreRinger();
    }
    
    public void onStop() {
        this.app_is_stoped = true;
		this.closeCamera();
    }

	public void asyncOpenCamera() {
		synchronized (mAsyncHandler) {
			mAsyncHandler.sendEmptyMessage(ASYNC_MSG_WHAT_OPEN_CAMERA);
		}
	}

	public void asyncCloseCamera() {
		synchronized (mAsyncHandler) {
			mAsyncHandler.sendEmptyMessage(ASYNC_MSG_WHAT_CLOSE_CAMERA);
		}
	}

	public void asyncCloseCameraAndOpenSetting() {
		synchronized (mAsyncHandler) {
			mAsyncHandler.sendEmptyMessage(ASYNC_MSG_WHAT_CLOSE_CAMERA_AND_OPEN_SETTING);
		}
	}

	public void asyncOpenPipActivity() {
		synchronized (mAsyncHandler) {
			if (!mAsyncHandler.hasMessages(ASYNC_MSG_WHAT_OPEN_PIP_ACTIVITY)) {
				mAsyncHandler.sendEmptyMessage(ASYNC_MSG_WHAT_OPEN_PIP_ACTIVITY);
			}
		}
	}

	public void asyncCloseCameraAndOpenPipActivity(String pipPkgName) {
		synchronized (mAsyncHandler) {
			if (!mAsyncHandler.hasMessages(ASYNC_MSG_WHAT_CLOSE_CAMERA_AND_OPEN_PIP_ACTIVITY)) {
				Message message = mAsyncHandler.obtainMessage(ASYNC_MSG_WHAT_CLOSE_CAMERA_AND_OPEN_PIP_ACTIVITY, pipPkgName);
				mAsyncHandler.sendMessage(message);
			}
		}
	}
    
    public void onDestroy() {
		mSoundManager.release();
    }
    
    public void onSaveInstanceState(Bundle state) {
		
			Log.d(TAG, "onSaveInstanceState");
		
			Log.d(TAG, "save cameraId: " + cameraId);
    	state.putInt("cameraId", cameraId);
		
			Log.d(TAG, "save zoom_factor: " + zoom_factor);
    	state.putInt("zoom_factor", zoom_factor);
	}

    public void setUIRotation(int ui_rotation) {
		Log.d(TAG, "setUIRotation");
		this.ui_rotation = ui_rotation;
	}

    private synchronized void updateParametersFromLocation() {
    	if( camera_controller != null ) {
			camera_controller.removeLocationInfo();
    	}
    }

	public void setMode(int mode) {
		this.mMode = mode;
	}

	public boolean isVideoOrMotion() {
		return mMode == MODE_VIDEO || mMode == MODE_MOTION;
	}

	public boolean isVideo() {
		return mMode == MODE_VIDEO;
	}

	public boolean isBeauty() {
		return mMode == MODE_BEAUTY;
	}
	
    public boolean isTakingPhoto() {
    	return this.phase == PHASE_TAKING_PHOTO;
    }


	public boolean isMotion() {
		return mMode == MODE_MOTION;
	}

    public CameraControllerManager getCameraControllerManager() {
    	return this.camera_controller_manager;
    }

    public boolean supportsFlash() {
    	return this.supported_flash_values != null;
    }
    
    public boolean supportsZoom() {
    	return this.has_zoom;
    }
    
    public int getMaxZoom() {
    	return this.max_zoom_factor;
    }
    
    public int getMaxNumFocusAreas() {
    	return this.max_num_focus_areas;
    }
    
    public boolean isTakingPhotoOrOnTimer() {
    	//return this.is_taking_photo;
    	return this.phase == PHASE_TAKING_PHOTO || this.phase == PHASE_TIMER;
    }
    
    public boolean isOnTimer() {
    	//return this.is_taking_photo_on_timer;
    	return this.phase == PHASE_TIMER;
    }

    public boolean isPreviewStarted() {
    	return this.is_preview_started;
    }
    
    protected void getMeasureSpec(View view, int [] spec, int widthSpec, int heightSpec) {
        if( !this.hasAspectRatio() ) {
            spec[0] = widthSpec;
            spec[1] = heightSpec;
            return;
        }
        double aspect_ratio = this.getAspectRatio();

        int previewWidth = MeasureSpec.getSize(widthSpec);
        int previewHeight = MeasureSpec.getSize(heightSpec);

        // Get the padding of the border background.
        int hPadding = view.getPaddingLeft() + view.getPaddingRight();
        int vPadding = view.getPaddingTop() + view.getPaddingBottom();

        // Resize the preview frame with correct aspect ratio.
        previewWidth -= hPadding;
        previewHeight -= vPadding;

        boolean widthLonger = previewWidth > previewHeight;
        int longSide = (widthLonger ? previewWidth : previewHeight);
        int shortSide = (widthLonger ? previewHeight : previewWidth);
        if (longSide > shortSide * aspect_ratio) {
            longSide = (int) ((double) shortSide * aspect_ratio);
        } else {
            shortSide = (int) ((double) longSide / aspect_ratio);
        }
        if (widthLonger) {
            previewWidth = longSide;
            previewHeight = shortSide;
        } else {
            previewWidth = shortSide;
            previewHeight = longSide;
        }

        // Add the padding of the border.
        previewWidth += hPadding;
        previewHeight += vPadding;
        spec[0] = MeasureSpec.makeMeasureSpec(previewWidth, MeasureSpec.EXACTLY);
        spec[1] = MeasureSpec.makeMeasureSpec(previewHeight, MeasureSpec.EXACTLY);
    }
    
    public void switchFilter(int filterId) {
		mFilterId = filterId;
		synchronized (mAsyncHandler) {
			mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_UPDATE_FILTER);
			mAsyncHandler.sendEmptyMessage(ASYNC_MSG_WHAT_UPDATE_FILTER);
		}
    }
    
    /**
     * 更新滤镜对象
     */
    private void updateFilter() {
        if (gpuImage != null) {
            GPUImageFilter filter = createFilter();
            if (filter != null) {
                gpuImage.setFilter(filter, true);
            }
        }
		checkFlashState(false);
		activity.updateFlashButton();
		activity.updateHdrButton(false);
    }

	private GPUImageFilter createFilter(boolean isStartRecording) {
		GPUImageFilter filter = null;
		if (!activity.hasStickers() && !activity.hasBackground() && mFilterId == -1) {
			filter = getDefalutFilter();
		} else if (mFilterId == -2) {
			filter = new GPUImageBeautyFilter();
		} else {
			LocalFilterBO lfb = activity.getFilterAdapter().getItem(mFilterId + 1);
			filter = ImageFilterTools.createFilterForType(getContext(), lfb);
			if (!PhoneInfo.isNotSupportOES() && !PhoneInfo.isNotSupportVideoRender()) {
				GPUImageFilterGroup filterGroup = new GPUImageFilterGroup();
				filterGroup.addFilter(new GPUImageOESFilter());
				filterGroup.addFilter(filter);

				if (isStartRecording) {
					List<String> backgroundBitmaps = activity.getBackgroundBitmaps();
					if (backgroundBitmaps != null && backgroundBitmaps.size() > 0) {
						GPUImageNormalBlendFilter backgroundFilter = new GPUImageNormalBlendFilter();
						backgroundFilter.setBitmapList(backgroundBitmaps);
						filterGroup.addFilter(backgroundFilter);
					}

					String stickerBitmap = activity.getStickerBitmap();
					if (stickerBitmap != null) {
						GPUImageNormalBlendFilter stickerFilter = new GPUImageNormalBlendFilter();
						stickerFilter.setBitmap(BitmapFactory.decodeFile(stickerBitmap));
						filterGroup.addFilter(stickerFilter);
					}
				}

				filter = filterGroup;
			}
		}
		return filter;
	}

	private GPUImageFilter createFilter() {
		return createFilter(false);
	}

	private boolean isDadTwoInputFilter() {
		if (mFilterId == -1 || mFilterId == -2) {
			return false;
		}
		LocalFilterBO lfb = activity.getFilterAdapter().getItem(mFilterId + 1);
		return ImageFilterTools.isBadTwoInputFilter(lfb.getPackageName());
	}
    
    public void addView() {
		viewGroup.addView(glSurfaceView);
		viewGroup.addView(mFocusOverlay);
    }
    
    /**
     * 是否是滤镜模式
     * 
     * @return
     */
    public boolean isFiltMode() {
        return mFilterId >= 0 || gpuImage.hasEffect() || gpuImage.hasHDREffect() || activity.hasStickers() || activity.hasBackground();
    }

	public boolean isFiltModeNotEffect() {
		return mFilterId >= 0;
	}
    
    public void toggleTakeButtonClick(boolean delay) {
		synchronized (mAsyncHandler) {
			if (FolderTools.isSdcardPathCanWrite(activity.getActivity(), FolderHelper.getOrCreateSaveLocation())) {
				mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_TAKE_CLICK);
				mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_TOUCH);
				mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_CYCLEFLASH);
				mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_PAUSE_VIDEO);
				mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_STOP_VIDEO);
				if (delay) {
					mAsyncHandler.sendEmptyMessageDelayed(ASYNC_MSG_WHAT_TAKE_CLICK, 200);
				} else {
					mAsyncHandler.sendEmptyMessage(ASYNC_MSG_WHAT_TAKE_CLICK);
				}
				FolderTools.checkSdCardState(activity.getActivity());
			} else {
				Toast.makeText(activity.getActivity(), R.string.storage_not_ready, Toast.LENGTH_SHORT).show();
			}
		}
    }
    
    public boolean toggleTakeButtonLongClick() {
        if (isVideoOrMotion()) {
            return false;
        }
        mDistanceCheckerListener.onDistanceChanged();
        return false;
    }

	public void toggleStopVideo() {
		synchronized (mAsyncHandler) {
			if (isMotion() && mAsyncHandler.hasMessages(ASYNC_MSG_WHAT_TAKE_CLICK)) {
				activity.showToast(activity.getString(R.string.motion_duration_tips));
			}
			mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_TAKE_CLICK);
			mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_TOUCH);
			mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_CYCLEFLASH);
			mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_PAUSE_VIDEO);
			mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_STOP_VIDEO);
			long recordTime = getVideoRecordTime();
			if (isMotion() && recordTime < MIN_MOTION_TIME) {
				mAsyncHandler.sendEmptyMessageDelayed(ASYNC_MSG_WHAT_STOP_VIDEO, MIN_MOTION_TIME - recordTime);
			} else {
				mAsyncHandler.sendEmptyMessage(ASYNC_MSG_WHAT_STOP_VIDEO);
			}
		}
	}
    
    public void onShutterButtonFocus(boolean pressed) {
        if (pressed) {
            toggleTakeButtonLongClick();
        }
        mKeyPressed = pressed;
    }
    
    private boolean isTakeButtonPressed() {
        return activity.isTakeButtonPressed() || mKeyPressed;
    }
    
    private SurfaceView getSurfaceView() {
		if (isVideoOrMotion() && PhoneInfo.isNotSupportVideoRender()) {
			return surfaceView;
		} else {
			return glSurfaceView;
		}
    }

	public boolean isShowHDRSetting() {
		if (isVideoOrMotion()) {
			return false;
		}
		if (supportHDR()) {
			return true;
		}
		return !isFrontCamera() && (!isFiltMode() || gpuImage.hasHDREffect());
	}
    
    /**
     * 是否支持HDR模式
     * 
     * @return
     */
    public synchronized boolean supportHDR() {
        if (camera_controller_manager == null || camera_controller_manager.isFrontFacing(cameraId)) {
            return false;
        }
        if (camera_controller != null) {
            return camera_controller.supportedHDR();
        }
        return false;
    }
    
    /**
     * 设置HDR模式开关
     * 
     * @param isOn
     */
    public void setHDROn(boolean isOn) {
        if (isOn != isHDROn()) {
            SettingsManager.setPreferenceHdrOn(isOn);
			if (supportHDR()) {
				synchronized (mAsyncHandler) {
					mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_CYCLEFLASH);
					mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_RESTAT_CAMERA);
					mAsyncHandler.sendEmptyMessage(ASYNC_MSG_WHAT_RESTAT_CAMERA);
				}
			} else {
				synchronized (mAsyncHandler) {
					mAsyncHandler.removeMessages(ASYNC_MSG_WHAT_UPDATE_FILTER);
					mAsyncHandler.sendEmptyMessage(ASYNC_MSG_WHAT_UPDATE_FILTER);
				}
				checkFlashState(false);
			}
        }
        activity.updateFlashButton();
    }
    
    /**
     * HDR模式是否开启
     * 
     * @return
     */
    public boolean isHDROn() {
        return SettingsManager.getPreferenceHdrOn();
    }
    
    /**
     * 网格线是否开启
     * 
     * @return
     */
    public boolean isGridOn() {
        String preference_grid = SettingsManager.getPreferenceGrid();
        if (preference_grid != null && preference_grid.equals("preference_grid_3x3")) {
            return true;
        }
        return false;
    }
    
    /**
     * 设置网格线开关
     * 
     * @param isOn
     */
    public void setGridOn(boolean isOn) {
        SettingsManager.setPreferenceGrid(isOn ? "preference_grid_3x3" : "");
        mFocusOverlay.invalidate();
    }
    
    private boolean isSupportedFocusAuto() {
        List<String> supportedFocusValues = getSupportedFocusValues();
        if (supportedFocusValues != null 
                && supportedFocusValues.contains("focus_mode_auto")) {
            return true;
        }
        return false;
    }
    
    /**
     * 获取当前录像时常
     * 
     * @return
     */
    public long getVideoRecordTime() {
		synchronized (PHASE_LOCK) {
			long video_time;
			if (isVideoPausing()) {
				video_time = savedRecordTime;
			} else {
				video_time = (System.currentTimeMillis() - video_start_time) + savedRecordTime;
			}
			return video_time;
		}
    }
    
    /**
     * 获取延迟拍摄剩余时间
     * 
     * @return
     */
    public int getDelayRemainingTime() {
        int remaining_time = 0;
        if (this.isOnTimer()) {
            remaining_time = (int)((take_photo_time - System.currentTimeMillis() + 999) / 1000);
            
                Log.d(TAG, "remaining_time: " + remaining_time);
        }
        return remaining_time;
    }
    
    public boolean isFrontCamera() {
    	return camera_controller_manager.isFrontFacing(cameraId);
    }
    
    /**
     * 是否正在录像
     * 
     * @return
     */
    public boolean isVideoRecording() {
        return isVideoOrMotion() && (video_recorder != null || phase == PHASE_TAKING_PHOTO);
    }
    
    public synchronized Camera getCamera() {
        if (camera_controller != null) {
            return camera_controller.getCamera();
        }
        return null;
    }

	private void changeSurfaceMode() {
		viewGroup.removeAllViews();
		if (mHolder != null) {
			mHolder.removeCallback(this);
		}
		if (isVideoOrMotion()) {
			showSurfaceView(surfaceView);
		} else {
			showSurfaceView(glSurfaceView);
		}
	}

	private void showSurfaceView(SurfaceView sview) {
		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		mHolder = sview.getHolder();
		mHolder.addCallback(this);
		// deprecated setting, but required on Android versions prior to 3.0
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS); // deprecated
		viewGroup.addView(sview);
		viewGroup.addView(mFocusOverlay);
	}

	public boolean isVignetteEnable() {
		return gpuImage.isVigetteEnable();
	}

	public boolean isRadialEnable() {
		return gpuImage.isSelectiveBlurEnable();
	}

	public boolean isLinearEnable() {
		return gpuImage.isTiltShiftEnable();
	}

	public boolean isEffectPressed() {
		return gpuImage.isPressed();
	}

	public void effectPressUp() {
		gpuImage.pressUp();
	}

	public void delayEffectPressUp() {
		gpuImage.delayPressUp();
	}


	public synchronized GPUImageFilter getDefalutFilter() {
		if (camera_controller != null &&
				!isVideoOrMotion() && isHDROn() && !supportHDR() && !isFrontCamera()) {
			if (PhoneInfo.isNotSupportVideoRender() || PhoneInfo.isNotSupportOES()) {
				return new GPUImageHDRFilter();
			} else {
				return new GPUImageHDROESFilter();
			}
		}
		if (PhoneInfo.isNotSupportVideoRender()) {
			return new GPUImageFilter();
		}
		if (PhoneInfo.isNotSupportOES()) {
			return new GPUImageFilter();
		} else {
			return new GPUImageOESFilter();
		}
	}

	private void enableDrawCaptureFrame() {
		mCanDrawPhotoFrame = true;
		mFocusOverlay.postInvalidate();
	}

	private void disableDrawCaptureFrame() {
		mCanDrawPhotoFrame = false;
		mFocusOverlay.postInvalidate();
	}

	public List<String> getSupportedWhiteBlance() {
		return supported_white_balances;
	}

	public String getCurrentWhiteBlance() {
		return mWhiteBalanceValue;
	}

	public synchronized boolean setWhiteBlance(String value) {
		if (camera_controller != null && camera_controller.setWhiteBalance(value)) {
			mWhiteBalanceValue = value;
			return true;
		}
		return false;
	}

	public List<String> getSupportedIsos() {
		return supported_isos;
	}

	public String getCurrentIso() {
		return mIsoValue;
	}

	public synchronized boolean setIso(String value) {
		if (camera_controller != null && camera_controller.setISO(value)) {
			mIsoValue = value;
			return true;
		}
		return false;
	}

	public List<String> getSupportedEv() {
		return supported_ev_values;
	}

	public String getCurrentEv() {
		return mEvValue;
	}

	public synchronized boolean setEv(String value) {
		try {
			int i;
			if (value.startsWith("+")) {
				i = Integer.valueOf(value.substring(1));
			} else {
				i = Integer.valueOf(value);
			}
			if (camera_controller != null && camera_controller.setExposureCompensation((int) (i / exposure_step))) {
				mEvValue = value;
				return true;
			}
		} catch (Throwable tr) {
		}
		return false;
	}

	public boolean isMotionPressedInside() {
		return mMotionPressedInside;
	}

	public void onMotionPressedOutside() {
		mMotionPressedInside = false;
	}

	public void onMotionPressedInside() {
		mMotionPressedInside = true;
	}
	public android.opengl.GLSurfaceView getGlSurfaceView() {
		return glSurfaceView;
	}
	public int getMode() {
		return mMode;
	}
}
