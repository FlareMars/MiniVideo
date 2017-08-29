package com.jb.zcamera.av;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.opengl.EGLContext;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Trace;
import android.util.Log;

import com.jb.zcamera.utils.ZCameraUtil;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

import javax.microedition.khronos.opengles.GL10;

/**
 * @hide
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class CameraEncoder implements Runnable {
    private static final String TAG = "CameraEncoder";
    private static final boolean TRACE = false;         // Systrace
    private static final boolean VERBOSE = false;       // Lots of logging

    private enum STATE {
        /* Stopped or pre-construction */
        UNINITIALIZED,
        /* Construction-prompted initialization */
        INITIALIZING,
        /* Camera frames are being received */
        INITIALIZED,
        /* Camera frames are being sent to Encoder */
        RECORDING,
        /* Was recording, and is now stopping */
        STOPPING,
        /* Releasing resources. */
        RELEASING,
        /* This instance can no longer be used */
        RELEASED
    }

    private volatile STATE mState = STATE.UNINITIALIZED;

    // EncoderHandler Message types (Message#what)
    private static final int MSG_FRAME_AVAILABLE = 2;
    private static final int MSG_SET_SURFACE_TEXTURE = 3;
    private static final int MSG_RELEASE = 6;
    private static final int MSG_RESET = 7;
    private static final int MSG_RESET_AND_START = 8;

    // ----- accessed exclusively by encoder thread -----
    private WindowSurface mInputWindowSurface;
    private EglCore mEglCore;
    private int mFrameNum;
    private VideoEncoderCore mVideoEncoder;
    private SessionConfig mSessionConfig;

    // ----- accessed by multiple threads -----
    private volatile EncoderHandler mHandler;
    private EglStateSaver mEglSaver;
    private final Object mStopFence = new Object();
    private final Object mSurfaceTextureFence = new Object();   // guards mSurfaceTexture shared with GLSurfaceView.Renderer
    private final Object mReadyForFrameFence = new Object();    // guards mReadyForFrames/mRecording
    private boolean mReadyForFrames;                            // Is the SurfaceTexture et all created
    private boolean mRecording;                                 // Are frames being recorded
    private boolean mEosRequested;                              // Should an EOS be sent on next frame. Used to stop encoder
    private final Object mReadyFence = new Object();            // guards ready/running
    private boolean mReady;                                     // mHandler created on Encoder thread
    private boolean mRunning;                                   // Encoder thread running

    private boolean mEncodedFirstFrame;

    private boolean mThumbnailRequested;
    private int mThumbnailScaleFactor;
    private int mThumbnailRequestedOnFrame;

    private int mFrameCount;
    private final float[] mSTMatrix = new float[16];

    private RenderAdapter mRenderAdapter;

    public CameraEncoder(SessionConfig config, RenderAdapter adapter) {
        mRenderAdapter = adapter;
        mState = STATE.INITIALIZING;
        init(config);
        mEglSaver = new EglStateSaver();
        startEncodingThread();
        mState = STATE.INITIALIZED;
    }

    /**
     * Resets per-recording state. This excludes {@link io.kickflip.sdk.av.EglStateSaver},
     * which should be re-used across recordings made by this CameraEncoder instance.
     *
     * @param config the desired parameters for the next recording.
     */
    private void init(SessionConfig config) {
        mEncodedFirstFrame = false;
        mReadyForFrames = false;
        mRecording = false;
        mEosRequested = false;

        mThumbnailRequested = false;
        mThumbnailRequestedOnFrame = -1;

        mSessionConfig = ZCameraUtil.checkNotNull(config);
    }

    /**
     * Prepare for a new recording with the given parameters.
     * This must be called after {@link #stopRecording()} and before {@link #release()}
     *
     * @param config the desired parameters for the next recording. Make sure you're
     *               providing a new {@link io.kickflip.sdk.av.SessionConfig} to avoid
     *               overwriting a previous recording.
     */
    public void reset(SessionConfig config) {
        if (mState != STATE.UNINITIALIZED)
            throw new IllegalArgumentException("reset called in invalid state");
        mState = STATE.INITIALIZING;
        mHandler.sendMessage(mHandler.obtainMessage(MSG_RESET, config));
    }

    public void resetAndStart(SessionConfig config) {
        if (mState != STATE.UNINITIALIZED)
            throw new IllegalArgumentException("reset called in invalid state");
        mState = STATE.INITIALIZING;
        mHandler.sendMessage(mHandler.obtainMessage(MSG_RESET_AND_START, config));
    }

    private void handleReset(SessionConfig config) throws IOException {
        if (mState != STATE.INITIALIZING)
            throw new IllegalArgumentException("handleRelease called in invalid state");
        Log.i(TAG, "handleReset");
        init(config);
        // Make display EGLContext current
        mEglSaver.makeSavedStateCurrent();
        prepareEncoder(mEglSaver.getSavedEGLContext(),
                mSessionConfig.getVideoWidth(),
                mSessionConfig.getVideoHeight(),
                mSessionConfig.getVideoBitrate(),
                mSessionConfig.getMuxer());
        mReadyForFrames = true;
        mState = STATE.INITIALIZED;
    }

    private void handleResetAndStart(SessionConfig config) throws IOException {
        handleReset(config);
        startRecording();
    }

    public SessionConfig getConfig() {
        return mSessionConfig;
    }


    /**
     * Request a thumbnail be generated from
     * the next available frame
     *
     * @param scaleFactor a downscale factor. e.g scaleFactor 2 will
     *                    produce a 640x360 thumbnail from a 1280x720 frame
     */
    public void requestThumbnail(int scaleFactor) {
        mThumbnailRequested = true;
        mThumbnailScaleFactor = scaleFactor;
        mThumbnailRequestedOnFrame = -1;
    }

    /**
     * Request a thumbnail be generated deltaFrame frames from now.
     *
     * @param scaleFactor a downscale factor. e.g scaleFactor 2 will
     * produce a 640x360 thumbnail from a 1280x720 frame
     */
    public void requestThumbnailOnDeltaFrameWithScaling(int deltaFrame, int scaleFactor) {
        requestThumbnailOnFrameWithScaling(mFrameNum + deltaFrame, scaleFactor);
    }

    /**
     * Request a thumbnail be generated from
     * the given frame
     *
     * @param scaleFactor a downscale factor. e.g scaleFactor 2 will
     *                    produce a 640x360 thumbnail from a 1280x720 frame
     */
    public void requestThumbnailOnFrameWithScaling(int frame, int scaleFactor) {
        mThumbnailScaleFactor = scaleFactor;
        mThumbnailRequestedOnFrame = frame;
    }

    public void adjustBitrate(int targetBitrate) {
        mVideoEncoder.adjustBitrate(targetBitrate);
    }


    public void logSavedEglState() {
        mEglSaver.logState();
    }

    /**
     * Called from GLSurfaceView.Renderer thread
     *
     * @return The SurfaceTexture containing the camera frame to display. The
     * display EGLContext is current on the calling thread
     * when this call completes
     */
    public SurfaceTexture getSurfaceTextureForDisplay() {
        synchronized (mSurfaceTextureFence) {
            return mRenderAdapter.getSurfaceTexture();
        }
    }

    public boolean isSurfaceTextureReadyForDisplay() {
        synchronized (mSurfaceTextureFence) {
            return !(getSurfaceTextureForDisplay() == null);
        }
    }

    /**
     * Called from UI thread
     *
     * @return is the CameraEncoder in the recording state
     */
    public boolean isRecording() {
        synchronized (mReadyFence) {
            return mRecording;
        }
    }

    /**
     * Called from UI thread
     */
    public void startRecording() {
        if (mState != STATE.INITIALIZED) {
            Log.e(TAG, "startRecording called in invalid state. Ignoring");
            return;
        }
        Log.i(TAG, "startRecording");
        synchronized (mReadyForFrameFence) {
            mFrameNum = 0;
            mRecording = true;
            mState = STATE.RECORDING;
        }

    }

    private void startEncodingThread() {
        synchronized (mReadyFence) {
            if (mRunning) {
                Log.w(TAG, "Encoder thread running when start requested");
                return;
            }
            mRunning = true;
            new Thread(this, "CameraEncoder").start();
            while (!mReady) {
                try {
                    mReadyFence.wait();
                } catch (InterruptedException ie) {
                    // ignore
                }
            }
        }
    }

    /**
     * Stop recording. After this call you must call either {@link #release()} to release resources if you're not going to
     * make any subsequent recordings, or {@link #reset(io.kickflip.sdk.av.SessionConfig)} to prepare
     * the encoder for the next recording
     * <p/>
     * Called from UI thread
     */
    public void stopRecording() {
        if (mState != STATE.RECORDING)
            throw new IllegalArgumentException("StopRecording called in invalid state");
        mState = STATE.STOPPING;
        Log.i(TAG, "stopRecording");
        synchronized (mReadyForFrameFence) {
            mEosRequested = true;
        }
    }


    /**
     * Release resources, including the Camera.
     * After this call this instance of CameraEncoder is no longer usable.
     * This call blocks until release is complete.
     * <p/>
     * Called from UI thread
     */
    public void release() {
        if (mState == STATE.STOPPING) {
            Log.i(TAG, "Release called while stopping. Trying to sync");
            synchronized (mStopFence) {
                while (mState != STATE.UNINITIALIZED) {
                    Log.i(TAG, "Release called while stopping. Waiting for uninit'd state. Current state: " + mState);
                    try {
                        mStopFence.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            Log.i(TAG, "Stopped. Proceeding to release");
        } else if (mState != STATE.UNINITIALIZED) {
            Log.i(TAG, "release called in invalid state " + mState);
            return;
            //throw new IllegalArgumentException("release called in invalid state");
        }
        mState = STATE.RELEASING;
        mHandler.sendMessage(mHandler.obtainMessage(MSG_RELEASE));
    }

    private void handleRelease() {
        if (mState != STATE.RELEASING)
            throw new IllegalArgumentException("handleRelease called in invalid state");
        Log.i(TAG, "handleRelease");
        shutdown();
        mRenderAdapter.realse();
        mState = STATE.RELEASED;
    }

    /**
     * Called by release()
     * Safe to release resources
     * <p/>
     * Called on Encoder thread
     */
    private void shutdown() {
        releaseEglResources();
        Looper.myLooper().quit();
    }

    /**
     * Called on an "arbitrary thread"
     *
     * @param surfaceTexture the SurfaceTexture initiating the call
     */
    public void onFrameAvailable(Runnable runnable) {
        // Pass SurfaceTexture to Encoding thread via Handler
        // Then Encode and display frame
        mHandler.sendMessage(mHandler.obtainMessage(MSG_FRAME_AVAILABLE, runnable));
    }

    /**
     * Called on Encoder thread
     *
     * @param surfaceTexture the SurfaceTexure that initiated the call to onFrameAvailable
     */
    private void handleFrameAvailable(Runnable runnable) {
        if (runnable != null) {
            runnable.run();
        }
        if (TRACE) Trace.beginSection("handleFrameAvail");
        synchronized (mReadyForFrameFence) {
            if (!mReadyForFrames) {
                if (VERBOSE) Log.i(TAG, "Ignoring available frame, not ready");
                return;
            }
            mFrameNum++;
            if (VERBOSE && (mFrameNum % 30 == 0)) Log.i(TAG, "handleFrameAvailable");
//            if (!surfaceTexture.equals(mSurfaceTexture))
//                Log.w(TAG, "SurfaceTexture from OnFrameAvailable does not match saved SurfaceTexture!");

            if (mRecording) {
                mInputWindowSurface.makeCurrent();
                if (TRACE) Trace.beginSection("drainVEncoder");
                mVideoEncoder.drainEncoder(false);
                if (TRACE) Trace.endSection();

//                surfaceTexture.getTransformMatrix(mTransform);
                if (TRACE) Trace.beginSection("drawVEncoderFrame");
                mRenderAdapter.drawFrame(mEosRequested);
                if (TRACE) Trace.endSection();
                if (!mEncodedFirstFrame) {
                    mEncodedFirstFrame = true;
                }

                if (mThumbnailRequestedOnFrame == mFrameNum) {
                    mThumbnailRequested = true;
                }
                if (mThumbnailRequested) {
                    saveFrameAsImage();
                    mThumbnailRequested = false;
                }

                mInputWindowSurface.setPresentationTime(getSurfaceTextureForDisplay().getTimestamp());
                mInputWindowSurface.swapBuffers();

                if (mEosRequested) {
                    /*if (VERBOSE) */
                    Log.i(TAG, "Sending last video frame. Draining encoder");
                    mVideoEncoder.signalEndOfStream();
                    mVideoEncoder.drainEncoder(true);
                    mRecording = false;
                    mEosRequested = false;
                    releaseEncoder();
                    synchronized (mStopFence) {
                        mState = STATE.UNINITIALIZED;
                        mStopFence.notify();
                    }
                }
            }
        }

        // Signal GLSurfaceView to render
        requestRender();

        if (TRACE) Trace.endSection();
    }

    private void saveFrameAsImage() {
        try {
            File recordingDir = new File(mSessionConfig.getMuxer().getOutputPath()).getParentFile();
            File imageFile = new File(recordingDir, String.format("%d.jpg", System.currentTimeMillis()));
            mInputWindowSurface.saveFrame(imageFile, mThumbnailScaleFactor);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * The GLSurfaceView.Renderer calls here after creating a
     * new texture in the display rendering context. Use the
     * created textureId to create a SurfaceTexture for
     * connection to the camera
     * <p/>
     * Called on the GlSurfaceView.Renderer thread
     *
     * @param textureId the id of the texture bound to the new display surface
     */
    public void saveEGLState() {
        if (VERBOSE) Log.i(TAG, "onSurfaceCreated. Saving EGL State");
        synchronized (mReadyFence) {
            // The Host Activity lifecycle may go through a OnDestroy ... OnCreate ... OnSurfaceCreated ... OnPause ... OnStop...
            // on it's way out, so our real sense of bearing should come from whether the EncoderThread is running
            if (mReady) {
                mEglSaver.saveEGLState();
                mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_SURFACE_TEXTURE, 0));
            }
        }
        mFrameCount = 0;
    }

    /**
     * Called on Encoder thread
     */
    private void handleSetSurfaceTexture(int textureId) throws IOException {
        synchronized (mSurfaceTextureFence) {
            // We're setting up the initial SurfaceTexture
            prepareEncoder(mEglSaver.getSavedEGLContext(),
                    mSessionConfig.getVideoWidth(),
                    mSessionConfig.getVideoHeight(),
                    mSessionConfig.getVideoBitrate(),
                    mSessionConfig.getMuxer());
            mReadyForFrames = true;
        }
    }

    @Override
    public void run() {
        Looper.prepare();
        synchronized (mReadyFence) {
            mHandler = new EncoderHandler(this);
            mReady = true;
            mReadyFence.notify();
        }
        Looper.loop();

        if (VERBOSE) Log.d(TAG, "Encoder thread exiting");
        synchronized (mReadyFence) {
            mReady = mRunning = false;
            mHandler = null;
            mReadyFence.notify();
        }
    }

    /**
     * Called with the display EGLContext current, on Encoder thread
     *
     * @param sharedContext The display EGLContext to be shared with the Encoder Surface's context.
     * @param width         the desired width of the encoder's video output
     * @param height        the desired height of the encoder's video output
     * @param bitRate       the desired bitrate of the video encoder
     * @param muxer         the desired output muxer
     */
    private void prepareEncoder(EGLContext sharedContext, int width, int height, int bitRate,
                                Muxer muxer) throws IOException {
        mVideoEncoder = new VideoEncoderCore(width, height, bitRate, muxer);
        if (mEglCore != null) {
            mEglCore.release();
        }
        // This is the first prepare called for this CameraEncoder instance
        mEglCore = new EglCore(sharedContext, EglCore.FLAG_RECORDABLE);
        if (mInputWindowSurface != null) mInputWindowSurface.release();
        mInputWindowSurface = new WindowSurface(mEglCore, mVideoEncoder.getInputSurface());
        mInputWindowSurface.makeCurrent();
    }

    private void releaseEncoder() {
        mVideoEncoder.release();
    }

    /**
     * Release all recording-specific resources.
     * The Encoder, EGLCore and FullFrameRect are tied to capture resolution,
     * and other parameters.
     */
    private void releaseEglResources() {
        mReadyForFrames = false;
        if (mInputWindowSurface != null) {
            mInputWindowSurface.release();
            mInputWindowSurface = null;
        }
        if (mEglCore != null) {
            mEglCore.release();
            mEglCore = null;
        }
    }

    public void onSurfaceChanged(GL10 unused, int width, int height) {
        Log.d(TAG, "onSurfaceChanged " + width + "x" + height);
    }

    public void onDrawFrame() {
        if (VERBOSE){
            if(mFrameCount % 30 == 0){
                logSavedEglState();
            }
        }

//        // Draw the video frame.
//        if(isSurfaceTextureReadyForDisplay()){
//            getSurfaceTextureForDisplay().updateTexImage();
////            getSurfaceTextureForDisplay().getTransformMatrix(mSTMatrix);
//            //Drawing texture overlay:
//            drawFrame();
//        }
        mFrameCount++;
    }

    private void requestRender() {
        mRenderAdapter.requestRender();
    }


    /**
     * Handles encoder state change requests.  The handler is created on the encoder thread.
     */
    private static class EncoderHandler extends Handler {
        private WeakReference<CameraEncoder> mWeakEncoder;

        public EncoderHandler(CameraEncoder encoder) {
            mWeakEncoder = new WeakReference<CameraEncoder>(encoder);
        }

        /**
         * Called on Encoder thread
         *
         * @param inputMessage
         */
        @Override
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            Object obj = inputMessage.obj;

            CameraEncoder encoder = mWeakEncoder.get();
            if (encoder == null) {
                Log.w(TAG, "EncoderHandler.handleMessage: encoder is null");
                return;
            }

            try {
                switch (what) {
                    case MSG_SET_SURFACE_TEXTURE:
                        encoder.handleSetSurfaceTexture((Integer) obj);
                        break;
                    case MSG_FRAME_AVAILABLE:
                        encoder.handleFrameAvailable(obj == null ? null : (Runnable) obj);
                        break;
                    case MSG_RELEASE:
                        encoder.handleRelease();
                        break;
                    case MSG_RESET:
                        encoder.handleReset((SessionConfig) obj);
                        break;
                    case MSG_RESET_AND_START:
                        encoder.handleResetAndStart((SessionConfig) obj);
                        break;
                    default:
                        throw new RuntimeException("Unexpected msg what=" + what);
                }
            } catch (IOException e) {
                Log.e(TAG, "Unable to reset! Could be trouble creating MediaCodec encoder");
                e.printStackTrace();
            }
        }
    }
}
