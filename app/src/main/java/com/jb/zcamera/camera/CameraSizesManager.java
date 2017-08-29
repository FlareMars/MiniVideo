package com.jb.zcamera.camera;

import android.util.Log;

import com.gomo.minivideo.CameraApp;

import java.util.ArrayList;
import java.util.List;

/**
 * 摄像头尺寸缓存数据管理
 * <p/>
 * Created by oujingwen on 16-1-5.
 */
public class CameraSizesManager {
    private static final String TAG = CameraSizesManager.class.getSimpleName();

    private static CameraSizesManager sInstance;

    private CameraSizesDBHelper mDBHelper;

    private List<Integer> mCameraIds;
    private List<CameraSizesDBHelper.CameraSize> mCameraSizes;

    private CameraSizesManager() {
        mDBHelper = new CameraSizesDBHelper(CameraApp.getApplication());
        mCameraIds = new ArrayList<>();
        mCameraSizes = mDBHelper.queryAll(mCameraIds);
    }

    public synchronized static final CameraSizesManager getInstance() {
        if (sInstance == null) {
            sInstance = new CameraSizesManager();
        }
        return sInstance;
    }

    /**
     * 是否有缓存摄像头尺寸数据
     *
     * @param cameraId
     * @return
     */
    public synchronized boolean hasCameraSizes(int cameraId) {
        return mCameraIds.contains(Integer.valueOf(cameraId));
    }

    /**
     * 缓存摄像头尺寸数据
     *
     * @param cameraId
     * @param pictureSizes
     * @param videoSizes
     */
    public synchronized void addCameraSizes(int cameraId, List<Size> pictureSizes, List<VideoQuality> videoSizes) {
        if (!hasCameraSizes(cameraId) && pictureSizes != null && pictureSizes.size() > 0
                && videoSizes != null && videoSizes.size() > 0) {
            mCameraIds.add(cameraId);
            List<CameraSizesDBHelper.CameraSize> sizes = new ArrayList<>();
            CameraSizesDBHelper.CameraSize size = null;
            for (Size psize : pictureSizes) {
                size = new CameraSizesDBHelper.CameraSize(cameraId,
                        CameraSizesDBHelper.CameraSize.TYPE_PICTURE,
                        psize.getWidth(), psize.getHeight(), null);
                sizes.add(size);
                mCameraSizes.add(size);
            }
            for (VideoQuality quality : videoSizes) {
                size = new CameraSizesDBHelper.CameraSize(cameraId,
                        CameraSizesDBHelper.CameraSize.TYPE_VIDEO,
                        quality.mSize.getWidth(), quality.mSize.getHeight(), quality.mQuality);
                sizes.add(size);
                mCameraSizes.add(size);
            }
            try {
                mDBHelper.insert(sizes);
            } catch (Throwable tr) {
                Log.e(TAG, "", tr);
            }
        }
    }

    /**
     * 获取缓存摄像头尺寸数据
     *
     * @param cameraId
     * @param pictureSizes 用于填充拍照尺寸数据列表
     * @param videoSizes 用于填充视频尺寸数据列表
     */
    public synchronized void getCameraSizes(int cameraId, List<Size> pictureSizes, List<VideoQuality> videoSizes) {
        if (hasCameraSizes(cameraId) && pictureSizes != null && videoSizes != null) {
            pictureSizes.clear();
            videoSizes.clear();
            for (CameraSizesDBHelper.CameraSize size : mCameraSizes) {
                if (size.mCameraId == cameraId) {
                    if (size.mType == CameraSizesDBHelper.CameraSize.TYPE_PICTURE) {
                        pictureSizes.add(new Size(size.mWidth, size.mHeight));
                    } else if (size.mType == CameraSizesDBHelper.CameraSize.TYPE_VIDEO) {
                        videoSizes.add(new VideoQuality(size.mValue, new Size(size.mWidth, size.mHeight)));
                    }
                }
            }
        }
    }

    /**
     * 清理缓存
     */
    public synchronized boolean clear() {
        if (mDBHelper.clearAll()) {
            mCameraIds.clear();
            mCameraSizes.clear();
            return true;
        }
        return false;
    }
}
