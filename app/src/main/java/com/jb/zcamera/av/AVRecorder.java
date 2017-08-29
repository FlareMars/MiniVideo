package com.jb.zcamera.av;

import android.annotation.TargetApi;
import android.os.Build;

import java.io.IOException;

/**
 * Records an Audio / Video stream to disk.
 *
 * Example usage:
 * <ul>
 *     <li>AVRecorder recorder = new AVRecorder(mSessionConfig);</li>
 *     <li>recorder.setPreviewDisplay(mPreviewDisplay);</li>
 *     <li>recorder.startRecording();</li>
 *     <li>recorder.stopRecording();</li>
 *     <li>(Optional) recorder.reset(mNewSessionConfig);</li>
 *     <li>(Optional) recorder.startRecording();</li>
 *     <li>(Optional) recorder.stopRecording();</li>
 *     <li>recorder.release();</li>
 * </ul>
 * @hide
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class AVRecorder {

    protected CameraEncoder mCamEncoder;
    protected MicrophoneEncoder mMicEncoder;
    private SessionConfig mConfig;
    private boolean mIsRecording;

    public AVRecorder(SessionConfig config, RenderAdapter adapter) throws IOException {
        init(config, adapter);
    }

    private void init(SessionConfig config, RenderAdapter adapter) throws IOException {
        mConfig = config;
        mCamEncoder = new CameraEncoder(config, adapter);
        if (isRecordAudio()) {
            mMicEncoder = new MicrophoneEncoder(config);
        }
        mIsRecording = false;
    }

    public void adjustVideoBitrate(int targetBitRate){
        mCamEncoder.adjustBitrate(targetBitRate);
    }


    public void startRecording(){
        mIsRecording = true;
        if (isRecordAudio() && mMicEncoder != null) {
            mMicEncoder.startRecording();
        }
        mCamEncoder.startRecording();
    }

    public boolean isRecording(){
        return mCamEncoder != null && mCamEncoder.isRecording();
    }

    public void stopRecording(){
        mIsRecording = false;
        if (isRecordAudio() && mMicEncoder != null) {
            mMicEncoder.stopRecording();
        }
        mCamEncoder.stopRecording();
    }

    /**
     * Prepare for a subsequent recording. Must be called after {@link #stopRecording()}
     * and before {@link #release()}
     * @param config
     */
    public void reset(SessionConfig config) throws IOException {
        mCamEncoder.reset(config);
        if (isRecordAudio()) {
            if (mMicEncoder == null) {
                mMicEncoder = new MicrophoneEncoder(config);
            } else {
                mMicEncoder.reset(config);
            }
        }
        mConfig = config;
        mIsRecording = false;
    }

    public void resetAndStart(SessionConfig config) throws IOException {
        mCamEncoder.resetAndStart(config);
        if (isRecordAudio()) {
            if (mMicEncoder == null) {
                mMicEncoder = new MicrophoneEncoder(config);
            } else {
                mMicEncoder.reset(config);
            }
            mMicEncoder.startRecording();
        }
        mConfig = config;
        mIsRecording = true;
    }

    /**
     * Release resources. Must be called after {@link #stopRecording()} After this call
     * this instance may no longer be used.
     */
    public void release() {
        mCamEncoder.release();
        // MicrophoneEncoder releases all it's resources when stopRecording is called
        // because it doesn't have any meaningful state
        // between recordings. It might someday if we decide to present
        // persistent audio volume meters etc.
        // Until then, we don't need to write MicrophoneEncoder.release()
    }

    public void saveEGLState() {
        mCamEncoder.saveEGLState();
    }

    public void onFrameAvailable(Runnable runnbale) {
        mCamEncoder.onFrameAvailable(runnbale);
    }

    public void onDrawFrame() {
        mCamEncoder.onDrawFrame();
    }

    public boolean isRecordAudio() {
        return mConfig.isRecordAudio();
    }
}
