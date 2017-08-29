/*
The contents of this file are subject to the Mozilla Public License
Version 1.1 (the "License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.mozilla.org/MPL/

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
License for the specific language governing rights and limitations
under the License.

The Original Code is collection of files collectively known as Open Camera.

The Initial Developer of the Original Code is Almalence Inc.
Portions created by Initial Developer are Copyright (C) 2013 
by Almalence Inc. All Rights Reserved.
 */

/* <!-- +++
 package com.almalence.opencam_plus;
 +++ --> */
// <!-- -+-

package com.jb.zcamera.camera;

//-+- -->

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;

import java.io.File;

/**
 * Plays an AssetFileDescriptor, but does all the hard work on another thread so
 * that any slowness with preparing or loading doesn't block the calling thread.
 */
public class SoundManager {
    private static final String TAG = "SoundManager";

    private SoundPool mPlayer;

    private int mShutterSoundStartId = 0;
    private int mRecordSoundStartId = 0;

    private int mAudioStreamType;

    private AudioManager mAudioManager;

    private int mStoredRingerMode;

    @SuppressLint("NewApi")
    public SoundManager(Context mContext) {
        mAudioStreamType = AudioManager.STREAM_SYSTEM;
        mAudioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
        mStoredRingerMode = -1;
        initSound();
    }

    public synchronized void playShutterSound() {
        switch (mAudioManager.getRingerMode()) {
            case AudioManager.RINGER_MODE_NORMAL:
                if (mPlayer != null && mShutterSoundStartId > 0) {
                    mPlayer.play(mShutterSoundStartId, 1, 1, 0, 0, 1);
                }
                break;
            case AudioManager.RINGER_MODE_SILENT:
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                break;
            default:
                break;
        }
    }

    public synchronized void playRecordSound(boolean wait) {
        switch (mAudioManager.getRingerMode()) {
            case AudioManager.RINGER_MODE_NORMAL:
                if (mPlayer != null && mRecordSoundStartId > 0) {
                    mPlayer.play(mRecordSoundStartId, 1, 1, 0, 0, 1);
                }
                if (wait) {
                    // 阻塞300毫秒不让录像音被录进去
                    try {
                        Thread.sleep(300);
                    } catch (Throwable tr) {
                    }
                }
                break;
            case AudioManager.RINGER_MODE_SILENT:
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                break;
            default:
                break;
        }
    }

    public synchronized void release() {
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }

    private void initSound() {
        if (mPlayer == null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                mPlayer = new SoundPool.Builder()
                        .setMaxStreams(5)
                        .setAudioAttributes(
                                new AudioAttributes.Builder()
                                        .setLegacyStreamType(mAudioStreamType)
                                        .setContentType(
                                                AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                        .build()).build();
            } else {
                mPlayer = new SoundPool(5, mAudioStreamType, 0);
            }
            File file = new File("/system/media/audio/ui/camera_click.ogg");
            if (file.canRead()) {
                mShutterSoundStartId = mPlayer.load(file.getAbsolutePath(), 1);
            }

            file = new File("/system/media/audio/ui/VideoRecord.ogg");
            if (file.canRead()) {
                mRecordSoundStartId = mPlayer.load(file.getAbsolutePath(), 1);
            }
        }
    }

    public synchronized void muteRinger() {
        int currentRingerMode = mAudioManager.getRingerMode();
        if (currentRingerMode != AudioManager.RINGER_MODE_SILENT
                && currentRingerMode != AudioManager.RINGER_MODE_VIBRATE) {
            mStoredRingerMode = currentRingerMode;
            mAudioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        }
    }

    public synchronized void restoreRinger() {
        if (mStoredRingerMode != -1) {
            mAudioManager.setRingerMode(mStoredRingerMode);
            mStoredRingerMode = -1;
        }
    }

    public synchronized boolean isRingerMute() {
        int currentRingerMode = mAudioManager.getRingerMode();
        return currentRingerMode == AudioManager.RINGER_MODE_SILENT
                || currentRingerMode == AudioManager.RINGER_MODE_VIBRATE;
    }
}
