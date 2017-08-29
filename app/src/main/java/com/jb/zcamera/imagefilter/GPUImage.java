/*
 * Copyright (C) 2012 CyberAgent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jb.zcamera.imagefilter;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.location.Location;
import android.media.CamcorderProfile;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.Display;
import android.view.MotionEvent;
import android.view.WindowManager;

import com.jb.zcamera.camera.Preview;
import com.jb.zcamera.gallery.util.AsyncTask;
import com.jb.zcamera.imagefilter.filter.GPUImageFilter;
import com.jb.zcamera.imagefilter.filter.GPUImageHDRFilter;
import com.jb.zcamera.imagefilter.filter.GPUImageHDROESFilter;
import com.jb.zcamera.imagefilter.util.Rotation;
import com.jb.zcamera.ui.MultiTouchDetector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

/**
 * The main accessor for GPUImage functionality. This class helps to do common
 * tasks through a simple interface.
 */
public class GPUImage implements MultiTouchDetector.TouchEventListener, IRenderCallback {
    private final Context mContext;
    private GPUImageRenderer mRenderer;
    private GLSurfaceView mGlSurfaceView;
    private GPUImageFilter mFilter;
    private GPUImageFilter mBaseFilter;
    private Bitmap mCurrentBitmap;
    private ScaleType mScaleType = ScaleType.CENTER_CROP;

    private MultiTouchDetector mMultiTouchDetector;
    private Handler mHandler;
    private static final int MSG_WHAT_ACTION_UP = 100;
    private static final int MSG_WHAT_ACTION_DOWN = 101;
    private boolean mIsCamera;
    private IRenderCallback mRenderCallback;

    /**
     * Instantiates a new GPUImage object.
     *
     * @param context the context
     */
    public GPUImage(final Context context, boolean isCamera) {
        if (!supportsOpenGLES2(context)) {
            throw new IllegalStateException("OpenGL ES 2.0 is not supported on this phone.");
        }

        mContext = context;
        mIsCamera = isCamera;
        mFilter = new GPUImageFilter();
        mRenderer = new GPUImageRenderer(mFilter, this, isCamera);
        mMultiTouchDetector = new MultiTouchDetector(this);
        mHandler = new Handler(context.getMainLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (msg.what == MSG_WHAT_ACTION_UP) {
                    return true;
                } else if (msg.what == MSG_WHAT_ACTION_DOWN) {
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * Checks if OpenGL ES 2.0 is supported on the current device.
     *
     * @param context the context
     * @return true, if successful
     */
    private boolean supportsOpenGLES2(final Context context) {
        final ActivityManager activityManager = (ActivityManager)
                context.getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo =
                activityManager.getDeviceConfigurationInfo();
        return configurationInfo.reqGlEsVersion >= 0x20000;
    }

    /**
     * Sets the GLSurfaceView which will display the preview.
     *
     * @param view the GLSurfaceView
     */
    public void setGLSurfaceView(final GLSurfaceView view) {
        mGlSurfaceView = view;
        mGlSurfaceView.setEGLContextClientVersion(2);
        mGlSurfaceView.setEGLConfigChooser(8, 8, 8, 0, 0, 0);
        mGlSurfaceView.getHolder().setFormat(PixelFormat.RGBA_8888);
        mGlSurfaceView.setRenderer(mRenderer);
        mGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
//        mGlSurfaceView.setPreserveEGLContextOnPause(true);
        mGlSurfaceView.requestRender();
    }

    /**
     * Request the preview to be rendered again.
     */
    public void requestRender() {
        if (mGlSurfaceView != null) {
            mGlSurfaceView.requestRender();
        }
    }

    /**
     * Sets the up camera to be connected to GPUImage to get a filtered preview.
     *
     * @param preview the preview
     * @param degrees by how many degrees the image should be rotated
     * @param flipHorizontal if the image should be flipped horizontally
     * @param flipVertical if the image should be flipped vertically
     */
    public void setUpCamera(final Preview preview, final int degrees, final boolean flipHorizontal,
                            final boolean flipVertical) {
//        requestRender();
        Rotation rotation = Rotation.NORMAL;
        switch (degrees) {
            case 90:
                rotation = Rotation.ROTATION_90;
                break;
            case 180:
                rotation = Rotation.ROTATION_180;
                break;
            case 270:
                rotation = Rotation.ROTATION_270;
                break;
        }
        mRenderer.setUpSurfaceTexture(preview);
        mRenderer.setRotationCamera(rotation, flipHorizontal, flipVertical);
    }

    public void setUpCamera(Camera camera) {
        camera.setPreviewCallback(mRenderer);
    }

    /**
     * Sets the filter which should be applied to the image which was (or will
     * be) set by setImage(...).
     *
     * @param filter the new filter
     */
    public void setFilter(final GPUImageFilter filter, boolean isCamera) {
        mBaseFilter = filter;
        updateFilter(true);
    }

    /**
     * Sets the image on which the filter should be applied.
     *
     * @param bitmap the new image
     */
    public void setImage(final Bitmap bitmap) {
        mCurrentBitmap = bitmap;
        mRenderer.setImageBitmap(bitmap, false);
        requestRender();
    }

    /**
     * This sets the scale type of GPUImage. This has to be run before setting the image.
     * If image is set and scale type changed, image needs to be reset.
     *
     * @param scaleType The new ScaleType
     */
    public void setScaleType(ScaleType scaleType) {
        mScaleType = scaleType;
        mRenderer.setScaleType(scaleType);
        mRenderer.deleteImage();
        mCurrentBitmap = null;
        requestRender();
    }

    /**
     * Sets the rotation of the displayed image.
     *
     * @param rotation new rotation
     */
    public void setRotation(Rotation rotation) {
        mRenderer.setRotation(rotation);
    }

    /**
     * Deletes the current image.
     */
    public void deleteImage() {
        mRenderer.deleteImage();
        mCurrentBitmap = null;
        requestRender();
    }

    /**
     * Sets the image on which the filter should be applied from a Uri.
     *
     * @param uri the uri of the new image
     */
    public void setImage(final Uri uri) {
        new LoadImageUriTask(this, uri).execute();
    }

    /**
     * Sets the image on which the filter should be applied from a File.
     *
     * @param file the file of the new image
     */
    public void setImage(final File file) {
        new LoadImageFileTask(this, file).execute();
    }

    private String getPath(final Uri uri) {
        String[] projection = {
                MediaStore.Images.Media.DATA,
        };
        Cursor cursor = mContext.getContentResolver()
                .query(uri, projection, null, null, null);
        int pathIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        String path = null;
        if (cursor.moveToFirst()) {
            path = cursor.getString(pathIndex);
        }
        cursor.close();
        return path;
    }

    /**
     * Gets the current displayed image with applied filter as a Bitmap.
     *
     * @return the current image with filter applied
     */
    public Bitmap getBitmapWithFilterApplied() {
        return getBitmapWithFilterApplied(mCurrentBitmap);
    }

    /**
     * Gets the given bitmap with current filter applied as a Bitmap.
     *
     * @param bitmap the bitmap on which the current filter should be applied
     * @return the bitmap with filter applied
     */
    public Bitmap getBitmapWithFilterApplied(final Bitmap bitmap) {
        if (hasEffect()) {
            pressUp();
        }
        if (mGlSurfaceView != null) {
            mRenderer.deleteImage();
            mRenderer.runOnDraw(new Runnable() {

                @Override
                public void run() {
                    synchronized(mFilter) {
                        mFilter.destroy();
                        mFilter.notify();
                    }
                }
            });
            synchronized(mFilter) {
                requestRender();
                try {
                    mFilter.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        GPUImageRenderer renderer = new GPUImageRenderer(mFilter, this, false);
        renderer.setRotation(Rotation.NORMAL,
                mRenderer.isFlippedHorizontally(), mRenderer.isFlippedVertically());
        renderer.setScaleType(mScaleType);
        PixelBuffer buffer = new PixelBuffer(bitmap.getWidth(), bitmap.getHeight());
        buffer.setRenderer(renderer);
        renderer.setImageBitmap(bitmap, false);
        Bitmap result = buffer.getBitmap();
        mFilter.destroy();
        renderer.deleteImage();
        buffer.destroy();

        mRenderer.setFilter(mFilter, null);
        if (mCurrentBitmap != null) {
            mRenderer.setImageBitmap(mCurrentBitmap, false);
        }
        requestRender();

        return result;
    }
    
    /**
     * Gets the given bitmap with current filter applied as a Bitmap.
     *
     * @param bitmap the bitmap on which the current filter should be applied
     * @return the bitmap with filter applied
     */
    public Bitmap getExternalBitmapWithFilterApplied(Bitmap bitmap, GPUImageFilter filter) {
    	
//        if (mGlSurfaceView != null) {
//            mRenderer.deleteImage();
//            mRenderer.runOnDraw(new Runnable() {
//
//                @Override
//                public void run() {
//                    synchronized(mFilter) {
//                        mFilter.destroy();
//                        mFilter.notify();
//                    }
//                }
//            });
//            synchronized(mFilter) {
//                requestRender();
//                try {
//                    mFilter.wait();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
        
        GPUImageRenderer renderer = new GPUImageRenderer(filter, this, false);
        renderer.setRotation(Rotation.NORMAL,
                mRenderer.isFlippedHorizontally(), mRenderer.isFlippedVertically());
        renderer.setScaleType(mScaleType);
        PixelBuffer buffer = new PixelBuffer(bitmap.getWidth(), bitmap.getHeight());
        buffer.setRenderer(renderer);
        renderer.setImageBitmap(bitmap, false);
        Bitmap result = buffer.getBitmap();
        filter.destroy();
        renderer.deleteImage();
        buffer.destroy();

//        mRenderer.setFilter(mFilter);
//        if (mCurrentBitmap != null) {
//            mRenderer.setImageBitmap(bitmap, false);
//        }
//        requestRender();

        return result;
    }
    
    

    /**
     * Gets the images for multiple filters on a image. This can be used to
     * quickly get thumbnail images for filters. <br>
     * Whenever a new Bitmap is ready, the listener will be called with the
     * bitmap. The order of the calls to the listener will be the same as the
     * filter order.
     *
     * @param bitmap the bitmap on which the filters will be applied
     * @param filters the filters which will be applied on the bitmap
     * @param listener the listener on which the results will be notified
     */
    public static void getBitmapForMultipleFilters(final GPUImage gpuImage, final Bitmap bitmap,
                                                   final List<GPUImageFilter> filters, final ResponseListener<Bitmap> listener) {
        if (filters.isEmpty()) {
            return;
        }
        GPUImageRenderer renderer = new GPUImageRenderer(filters.get(0), gpuImage, false);
        renderer.setImageBitmap(bitmap, false);
        PixelBuffer buffer = new PixelBuffer(bitmap.getWidth(), bitmap.getHeight());
        buffer.setRenderer(renderer);

        for (GPUImageFilter filter : filters) {
            renderer.setFilter(filter, null);
            listener.response(buffer.getBitmap());
            filter.destroy();
        }
        renderer.deleteImage();
        buffer.destroy();
    }


    /**
     * 这个方法会生成新的图片
     * @param bitmap
     * @param filter
     * @return
     */
    public static Bitmap getBitmapForFilter(final Bitmap bitmap, final GPUImageFilter filter) {
        GPUImageRenderer renderer = new GPUImageRenderer(filter, null, false);
        renderer.setImageBitmap(bitmap, false);
        PixelBuffer buffer = new PixelBuffer(bitmap.getWidth(), bitmap.getHeight());
        try {
            if (filter == null) {
                return bitmap;
            }
            buffer.setRenderer(renderer);
            renderer.setFilter(filter, null);
            return buffer.getBitmap();
        } catch (Throwable e){
        } finally {
            filter.destroy();
            renderer.deleteImage();
            buffer.destroy();
        }
        return bitmap;
    }

    /**
     * 将当前图片应用滤镜效果， 但是不产生新的Bitmap
     * @param bitmap
     * @param filter
     * @return
     */
    public static void applyFilter(final Bitmap bitmap, final GPUImageFilter filter) {
        GPUImageRenderer renderer = new GPUImageRenderer(filter, null, false);
        renderer.setImageBitmap(bitmap, false);
        PixelBuffer buffer = new PixelBuffer(bitmap.getWidth(), bitmap.getHeight());
        try {
            if (filter == null) {
                return;
            }
            buffer.setRenderer(renderer);
            renderer.setFilter(filter, null);
            buffer.applyToBitmap(bitmap);
        } catch (Throwable e){
        } finally {
            filter.destroy();
            renderer.deleteImage();
            buffer.destroy();
        }
    }

    /**
     * Deprecated: Please use
     * {@link GPUImageView#saveToPictures(String, String, com.jb.zcamera.imagefilter.GPUImageView.OnPictureSavedListener)}
     *
     * Save current image with applied filter to Pictures. It will be stored on
     * the default Picture folder on the phone below the given folderName and
     * fileName. <br>
     * This method is async and will notify when the image was saved through the
     * listener.
     *
     * @param folderName the folder name
     * @param fileName the file name
     * @param listener the listener
     */
    @Deprecated
    public void saveToPictures(final String folderName, final String fileName,
                               final OnPictureSavedListener listener) {
        saveToPictures(mCurrentBitmap, folderName, fileName, listener);
    }

    /**
     * Deprecated: Please use
     * {@link GPUImageView#saveToPictures(String, String, com.jb.zcamera.imagefilter.GPUImageView.OnPictureSavedListener)}
     *
     * Apply and save the given bitmap with applied filter to Pictures. It will
     * be stored on the default Picture folder on the phone below the given
     * folerName and fileName. <br>
     * This method is async and will notify when the image was saved through the
     * listener.
     *
     * @param bitmap the bitmap
     * @param folderName the folder name
     * @param fileName the file name
     * @param listener the listener
     */
    @Deprecated
    public void saveToPictures(final Bitmap bitmap, final String folderName, final String fileName,
                               final OnPictureSavedListener listener) {
        new SaveTask(bitmap, folderName, fileName, listener).execute();
    }

    /**
     * Runs the given Runnable on the OpenGL thread.
     *
     * @param runnable The runnable to be run on the OpenGL thread.
     */
    void runOnGLThread(Runnable runnable) {
        mRenderer.runOnDrawEnd(runnable);
    }

    private int getOutputWidth() {
        if (mRenderer != null && mRenderer.getFrameWidth() != 0) {
            return mRenderer.getFrameWidth();
        } else if (mCurrentBitmap != null) {
            return mCurrentBitmap.getWidth();
        } else {
            WindowManager windowManager =
                    (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            Display display = windowManager.getDefaultDisplay();
            return display.getWidth();
        }
    }

    private int getOutputHeight() {
        if (mRenderer != null && mRenderer.getFrameHeight() != 0) {
            return mRenderer.getFrameHeight();
        } else if (mCurrentBitmap != null) {
            return mCurrentBitmap.getHeight();
        } else {
            WindowManager windowManager =
                    (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            Display display = windowManager.getDefaultDisplay();
            return display.getHeight();
        }
    }
    
    public void setFiltFrameListener(FiltFrameListener listener) {
        if (mRenderer != null) {
            mRenderer.setFiltFrameListener(listener);
        }
    }

    public boolean onTouchEvent(MotionEvent event) {
        return mMultiTouchDetector.onTouchEvent(event);
    }

    @Override
    public void onActionDown(float x, float y) {
        mHandler.removeMessages(MSG_WHAT_ACTION_UP);
        mHandler.removeMessages(MSG_WHAT_ACTION_DOWN);
        Message msg = mHandler.obtainMessage(MSG_WHAT_ACTION_DOWN);
        msg.obj = new float[] {x, y};
        mHandler.sendMessageDelayed(msg, 400);
    }

    @Override
    public void onActionUp(float x, float y) {
        delayPressUp();
    }

    @Override
    public void onActionPointerDown() {
        mHandler.removeMessages(MSG_WHAT_ACTION_DOWN);
    }

    @Override
    public void onActionPointerUp() {
//        if (mSelectiveBlurFilter != null) {
//            mSelectiveBlurFilter.setPressed(false);
//        } else if (mTiltShiftFilter != null) {
//            mTiltShiftFilter.setPressed(false);
//        }
//        mHandler.removeMessages(MSG_WHAT_ACTION_UP);
//        mHandler.sendEmptyMessageDelayed(MSG_WHAT_ACTION_UP, 2000);
    }

    @Override
    public void onDrag(float x, float y) {

    }

    @Override
    public boolean onRotation(float rotation, float x, float y) {
        return false;
    }

    @Override
    public boolean onScale(float scale) {

        return false;
    }

    @Override
    public void onSurfaceTextureCreated(SurfaceTexture surfaceTexture) {
        if (mRenderCallback != null) {
            mRenderCallback.onSurfaceTextureCreated(surfaceTexture);
        }
    }

    @Override
    public void onFrameAvaliable(long frameTimeNanos) {
        requestRender();
        if (mRenderCallback != null) {
            mRenderCallback.onFrameAvaliable(frameTimeNanos);
        }
    }

    public void setRenderCallback(IRenderCallback callback) {
        mRenderCallback = callback;
    }

    public void removeRenderCallback() {
        mRenderCallback = null;
    }

    @Deprecated
    private class SaveTask extends AsyncTask<Void, Void, Void> {

        private final Bitmap mBitmap;
        private final String mFolderName;
        private final String mFileName;
        private final OnPictureSavedListener mListener;
        private final Handler mHandler;

        public SaveTask(final Bitmap bitmap, final String folderName, final String fileName,
                        final OnPictureSavedListener listener) {
            mBitmap = bitmap;
            mFolderName = folderName;
            mFileName = fileName;
            mListener = listener;
            mHandler = new Handler();
        }

        @Override
        protected Void doInBackground(final Void... params) {
            Bitmap result = getBitmapWithFilterApplied(mBitmap);
            saveImage(mFolderName, mFileName, result);
            return null;
        }

        private void saveImage(final String folderName, final String fileName, final Bitmap image) {
            File path = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File file = new File(path, folderName + "/" + fileName);
            try {
                file.getParentFile().mkdirs();
                image.compress(CompressFormat.JPEG, 80, new FileOutputStream(file));
                MediaScannerConnection.scanFile(mContext.getApplicationContext(),
                        new String[] {
                            file.toString()
                        }, null,
                        new MediaScannerConnection.OnScanCompletedListener() {
                            @Override
                            public void onScanCompleted(final String path, final Uri uri) {
                                if (mListener != null) {
                                    mHandler.post(new Runnable() {

                                        @Override
                                        public void run() {
                                            mListener.onPictureSaved(uri);
                                        }
                                    });
                                }
                            }
                        });
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public interface OnPictureSavedListener {
        void onPictureSaved(Uri uri);
    }

    private class LoadImageUriTask extends LoadImageTask {

        private final Uri mUri;

        public LoadImageUriTask(GPUImage gpuImage, Uri uri) {
            super(gpuImage);
            mUri = uri;
        }

        @Override
        protected Bitmap decode(BitmapFactory.Options options) {
            try {
                InputStream inputStream;
                if (mUri.getScheme().startsWith("http") || mUri.getScheme().startsWith("https")) {
                    inputStream = new URL(mUri.toString()).openStream();
                } else {
                    inputStream = mContext.getContentResolver().openInputStream(mUri);
                }
                return BitmapFactory.decodeStream(inputStream, null, options);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected int getImageOrientation() throws IOException {
            Cursor cursor = mContext.getContentResolver().query(mUri,
                    new String[] { MediaStore.Images.ImageColumns.ORIENTATION }, null, null, null);

            if (cursor == null || cursor.getCount() != 1) {
                return 0;
            }

            cursor.moveToFirst();
            int orientation = cursor.getInt(0);
            cursor.close();
            return orientation;
        }
    }

    private class LoadImageFileTask extends LoadImageTask {

        private final File mImageFile;

        public LoadImageFileTask(GPUImage gpuImage, File file) {
            super(gpuImage);
            mImageFile = file;
        }

        @Override
        protected Bitmap decode(BitmapFactory.Options options) {
            return BitmapFactory.decodeFile(mImageFile.getAbsolutePath(), options);
        }

        @Override
        protected int getImageOrientation() throws IOException {
            ExifInterface exif = new ExifInterface(mImageFile.getAbsolutePath());
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
            switch (orientation) {
                case ExifInterface.ORIENTATION_NORMAL:
                    return 0;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        }
    }

    private abstract class LoadImageTask extends AsyncTask<Void, Void, Bitmap> {

        private final GPUImage mGPUImage;
        private int mOutputWidth;
        private int mOutputHeight;

        @SuppressWarnings("deprecation")
        public LoadImageTask(final GPUImage gpuImage) {
            mGPUImage = gpuImage;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            if (mRenderer != null && mRenderer.getFrameWidth() == 0) {
                try {
                    synchronized (mRenderer.mSurfaceChangedWaiter) {
                        mRenderer.mSurfaceChangedWaiter.wait(3000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            mOutputWidth = getOutputWidth();
            mOutputHeight = getOutputHeight();
            return loadResizedImage();
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            mGPUImage.deleteImage();
            mGPUImage.setImage(bitmap);
        }

        protected abstract Bitmap decode(BitmapFactory.Options options);

        private Bitmap loadResizedImage() {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            decode(options);
            int scale = 1;
            while (checkSize(options.outWidth / scale > mOutputWidth, options.outHeight / scale > mOutputHeight)) {
                scale++;
            }

            scale--;
            if (scale < 1) {
                scale = 1;
            }
            options = new BitmapFactory.Options();
            options.inSampleSize = scale;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            options.inPurgeable = true;
            options.inTempStorage = new byte[32 * 1024];
            Bitmap bitmap = decode(options);
            if (bitmap == null) {
                return null;
            }
            bitmap = rotateImage(bitmap);
            bitmap = scaleBitmap(bitmap);
            return bitmap;
        }

        private Bitmap scaleBitmap(Bitmap bitmap) {
            // resize to desired dimensions
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] newSize = getScaleSize(width, height);
            Bitmap workBitmap = Bitmap.createScaledBitmap(bitmap, newSize[0], newSize[1], true);
            if (workBitmap != bitmap) {
                bitmap.recycle();
                bitmap = workBitmap;
                System.gc();
            }

            if (mScaleType == ScaleType.CENTER_CROP) {
                // Crop it
                int diffWidth = newSize[0] - mOutputWidth;
                int diffHeight = newSize[1] - mOutputHeight;
                workBitmap = Bitmap.createBitmap(bitmap, diffWidth / 2, diffHeight / 2,
                        newSize[0] - diffWidth, newSize[1] - diffHeight);
                if (workBitmap != bitmap) {
                    bitmap.recycle();
                    bitmap = workBitmap;
                }
            }

            return bitmap;
        }

        /**
         * Retrieve the scaling size for the image dependent on the ScaleType.<br>
         * <br>
         * If CROP: sides are same size or bigger than output's sides<br>
         * Else   : sides are same size or smaller than output's sides
         */
        private int[] getScaleSize(int width, int height) {
            float newWidth;
            float newHeight;

            float withRatio = (float) width / mOutputWidth;
            float heightRatio = (float) height / mOutputHeight;

            boolean adjustWidth = mScaleType == ScaleType.CENTER_CROP
                    ? withRatio > heightRatio : withRatio < heightRatio;

            if (adjustWidth) {
                newHeight = mOutputHeight;
                newWidth = (newHeight / height) * width;
            } else {
                newWidth = mOutputWidth;
                newHeight = (newWidth / width) * height;
            }
            return new int[]{Math.round(newWidth), Math.round(newHeight)};
        }

        private boolean checkSize(boolean widthBigger, boolean heightBigger) {
            if (mScaleType == ScaleType.CENTER_CROP) {
                return widthBigger && heightBigger;
            } else {
                return widthBigger || heightBigger;
            }
        }

        private Bitmap rotateImage(final Bitmap bitmap) {
            if (bitmap == null) {
                return null;
            }
            Bitmap rotatedBitmap = bitmap;
            try {
                int orientation = getImageOrientation();
                if (orientation != 0) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(orientation);
                    rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                            bitmap.getHeight(), matrix, true);
                    bitmap.recycle();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return rotatedBitmap;
        }

        protected abstract int getImageOrientation() throws IOException;
    }

    public interface ResponseListener<T> {
        void response(T item);
    }

    public enum ScaleType { CENTER_INSIDE, CENTER_CROP }

    private void updateFilter(boolean destroyOld) {
        if (mBaseFilter != null) {
            mFilter = mBaseFilter;
        }
        mRenderer.setFilter(mFilter, destroyOld ? null : mBaseFilter);
        requestRender();
    }

    public void setVignetteEnable(boolean enable) {

    }

    public boolean isVigetteEnable() {
        return false;
    }

    public void setSelectiveBlurEnable(boolean enable) {

    }

    public boolean isSelectiveBlurEnable() {
        return false;
    }

    public void setTiltShiftEnable(boolean enable) {

    }

    public boolean isTiltShiftEnable() {
        return false;
    }

    public void setBeautyEnable(boolean enable) {

    }

    public boolean isBeautyEnable() {
        return false;
    }

    public boolean isPressed() {

        return false;
    }

    public void pressUp() {

    }

    public void delayPressUp() {
        if (isSelectiveBlurEnable() || isTiltShiftEnable()) {
            mHandler.removeMessages(MSG_WHAT_ACTION_UP);
            mHandler.sendEmptyMessageDelayed(MSG_WHAT_ACTION_UP, mIsCamera ? 1500 : 1000);
        }
    }

    public boolean hasEffect() {
        return isVigetteEnable() || isSelectiveBlurEnable() || isTiltShiftEnable() || isBeautyEnable();
    }

    public void clearEffect() {
        updateFilter(false);
    }

    public boolean hasHDREffect() {
        return mFilter.getClass() == GPUImageHDRFilter.class
                || mFilter.getClass() == GPUImageHDROESFilter.class;
    }


    public void startRecording(GPUImageFilter videoFilter, final boolean isBadTwoInputFilter, int rotation, File outputFile,
                               CamcorderProfile profile, Location location, final boolean recordAudio) {
        if (mRenderer != null) {
            mRenderer.startRecording(videoFilter, isBadTwoInputFilter, rotation, outputFile, profile, location, recordAudio);
        }
    }

    public void stopRecording() {
        if (mRenderer != null) {
            mRenderer.stopRecording();
        }
    }

    public boolean isRecording() {
        if (mRenderer != null) {
            return mRenderer.isRecording();
        }
        return false;
    }

    public void setSelectiveBlurSize(float value){

    }

    public void setTiltShiftBlurSize(float value){

    }

    public SurfaceTexture getSurfaceTexture() {
        return mRenderer.getSurfaceTexture();
    }
}
