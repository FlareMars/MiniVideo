
package com.jb.zcamera.camera;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.jb.zcamera.folder.FolderHelper;
import com.jb.zcamera.image.MediaBroadcastHelper;
import com.jb.zcamera.utils.ActionConstant;
import com.jb.zcamera.utils.PhoneInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * 视频拼接处理服务
 * 
 * @author oujingwen
 */
public class ProcessVideoService extends Service {
    private final static String TAG = "ProcessVideoService";

    public static final String ACTION_UPDATE_GALLARY_ICON = "action_update_gallary_icon";

    public static final String ACTION_ACTIVITY_RESULT = "action_activity_result";

    private static final String EXTRA_KEY = "extra_key";
    
    private static final String EXTRA_RECORD_TIME = "extra_record_time";
    
    private static final String EXTRA_LOCATION = "extra_location";

    private ServiceHandler mServiceHandler;

    private Looper mServiceLooper;

    private static final Object mStartingServiceSync = new Object();

    private static PowerManager.WakeLock mStartingService;

    private static final int MESSAGE_WHAT_INTENT = 1;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "ProcessVideoService: onCreate()");
        HandlerThread thread = new HandlerThread(TAG, Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public void onStart(Intent intent, int startId) {
        if (intent == null || !intent.hasExtra(EXTRA_KEY)) {
            return;
        }

        Log.i(TAG, "ProcessVideoService: onStart()");

        Message msg = mServiceHandler.obtainMessage(MESSAGE_WHAT_INTENT);
        msg.arg1 = startId;
        msg.obj = intent;
        mServiceHandler.sendMessage(msg);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MESSAGE_WHAT_INTENT) {
                int serviceId = msg.arg1;
                final Intent intent = (Intent)msg.obj;
                final boolean viewAndShare = ActionConstant.Actions.ACTION_MOTION_CAPTURE_AND_SHARE.equals(intent.getAction());
                final boolean editAndPublish = ActionConstant.Actions.ACTION_CAPTURE_TO_EDIT_AND_PUBLISH.equals(intent.getAction()) && PhoneInfo.hasJellyBeanMR2();
                final boolean caputure = MediaStore.ACTION_VIDEO_CAPTURE.equals(intent.getAction())
                        || ActionConstant.Actions.ACTION_MOTION_CAPTURE.equals(intent.getAction())
                        || viewAndShare || editAndPublish;
                final Uri outputUri = intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT);
                ArrayList<File> fileList = (ArrayList<File>)intent.getExtras().get(EXTRA_KEY);
                long recordTime = intent.getExtras().getLong(EXTRA_RECORD_TIME);
                Object object = intent.getExtras().get(EXTRA_LOCATION);
                Location location = object != null ? (Location) object : null;
                if (fileList.size() > 0) {
                    File fileSaved = fileList.remove(fileList.size() - 1);
                    if (fileList.size() > 0) {
                        File firstFile = fileList.get(0);
                        for (int i = 1; i < fileList.size(); i++) {
                            File currentFile = fileList.get(i);
                            VideoUtils.append(firstFile.getAbsolutePath(),
                                    currentFile.getAbsolutePath());
                        }
                        VideoUtils.append(firstFile.getAbsolutePath(), fileSaved.getAbsolutePath());

                        if (!fileList.get(0).getAbsoluteFile().equals(fileSaved.getAbsoluteFile())) {
                            fileSaved.delete();
                            firstFile.renameTo(fileSaved);
                        }
                    } else {
                        if (recordTime < 800) {
                            try {
                                fileSaved.delete();
                            } catch (Throwable tr) {
                                Log.e(TAG, "", tr);
                            }
                            fileSaved = null;
                        }
                    }
                    if (fileSaved != null) {
                        if (caputure && outputUri != null) {
                            OutputStream os = null;
                            FileInputStream fis = null;
                            try {
                                os = getContentResolver().openOutputStream(outputUri);
                                fis = new FileInputStream(fileSaved);
                                byte[] buffer = new byte[1024];
                                int count = -1;
                                while((count = fis.read(buffer, 0, 1024)) > 0) {
                                    os.write(buffer, 0, count);
                                }
                                sendFinishBroadcaset(outputUri, caputure);
                                fileSaved.delete();
                            } catch (Throwable e) {
                                Log.e(this.getClass().getName(), "", e);
                            } finally {
                                if (os != null) {
                                    try {
                                        os.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                                if (fis != null) {
                                    try {
                                        fis.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                            MediaBroadcastHelper.getInstance().decWaitingCount();
                        } else {
                            fileSaved = FolderHelper.checkMoveToExtSdcard(getBaseContext(), fileSaved, "video/mp4");
                            FolderHelper.broadcastVideoFile(getBaseContext(), fileSaved, location,
                                    new FolderHelper.OnScanCompletedListener() {
                                @Override
                                public void onScanCompleted(String path, Uri uri, int orientation) {
                                    sendFinishBroadcaset(uri, caputure);
//                                    if (FolderHelper.isMotionFilePath(path) && editAndPublish) {
//                                        VideoEditActivity.startVideoEditActivity(getApplicationContext(), path, intent.getIntExtra(ActionConstant.Extras.EXTRA_TOPIC_ID, 0));
//                                    }
                                    MediaBroadcastHelper.getInstance().decWaitingCount();
                                }
                            });
                        }
                    } else {
                        MediaBroadcastHelper.getInstance().decWaitingCount();
                    }
                }
                finishStartingService(ProcessVideoService.this, serviceId);
            }
        }
    }

    private void sendFinishBroadcaset(Uri uri, boolean capture) {
        Intent intent = new Intent(capture ? ACTION_ACTIVITY_RESULT : ACTION_UPDATE_GALLARY_ICON);
        intent.putExtra("data", uri);
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "ProcessVideoService: onDestroy()");
        mServiceLooper.quit();
        super.onDestroy();
    }

    /**
     * Start the service to process the current event notifications, acquiring
     * the wake lock before returning to ensure that the service will run.
     */
    public static void beginStartingService(Context context, Intent intent) {
        synchronized (mStartingServiceSync) {
            Log.d(TAG, "beginStartingService()");
            if (mStartingService == null) {
                PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
                mStartingService = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
                mStartingService.setReferenceCounted(false);
            }
            mStartingService.acquire();
            context.startService(intent);
        }
    }

    /**
     * Called back by the service when it has finished processing notifications,
     * releasing the wake lock if the service is now stopping.
     */
    public static void finishStartingService(Service service, int startId) {
        synchronized (mStartingServiceSync) {
            Log.d(TAG, "finishStartingService()");
            if (mStartingService != null) {
                if (service.stopSelfResult(startId)) {
                    mStartingService.release();
                }
            }
        }
    }

    public static void post(Context context, ArrayList<File> fileList, long recordTime, Location location, Intent activityIntent) {
        MediaBroadcastHelper.getInstance().incWaitingCount();
        Intent intent = new Intent(context, ProcessVideoService.class);
        if (activityIntent != null && !TextUtils.isEmpty(activityIntent.getAction())) {
            intent.setAction(activityIntent.getAction());
            if (activityIntent.getExtras() != null) {
                intent.putExtras(activityIntent.getExtras());
            }
        }
        intent.putExtra(EXTRA_KEY, fileList);
        intent.putExtra(EXTRA_RECORD_TIME, recordTime);
        intent.putExtra(EXTRA_LOCATION, location);
        beginStartingService(context, intent);
    }
}
