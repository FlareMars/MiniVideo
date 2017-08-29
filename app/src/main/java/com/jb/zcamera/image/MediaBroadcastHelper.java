
package com.jb.zcamera.image;

/**
 * 媒体文件异步添加到图库帮助类，用于控制图库Loading 的显示
 * 
 * @author oujingwen
 *
 */
public class MediaBroadcastHelper {

    private static final String TAG = "MediaBroadcastHelper";

    private static MediaBroadcastHelper sInstance;

    private int waitingCount = 0;

    private ScanCompletedListener mListener;

    private MediaBroadcastHelper() {
    }

    public synchronized static final MediaBroadcastHelper getInstance() {
        if (sInstance == null) {
            sInstance = new MediaBroadcastHelper();
        }
        return sInstance;
    }

    /**
     * 增加正在处理媒体文件数量
     */
    public synchronized void incWaitingCount() {
        waitingCount++;
    }

    /**
     * 减少正在处理媒体文件数量，为0时通知加载完成
     */
    public synchronized void decWaitingCount() {
        if (waitingCount > 0) {
            waitingCount--;
        }
        if (waitingCount <= 0 && mListener != null) {
            mListener.onScanCompleted();
            mListener = null;
        }
    }

    /**
     * 是否有媒体文件正在加载
     * 
     * @return
     */
    public synchronized boolean isWaiting() {
        return waitingCount > 0;
    }

    /**
     * 设置加载监听器，为避免界面内存泄露，界面退出时需要清理监听
     * 
     * @param listener
     */
    public synchronized void setScanCompletedListener(ScanCompletedListener listener) {
        this.mListener = listener;
    }

    /**
     * 加载完成回调监听器
     * 
     * @author oujingwen
     *
     */
    public interface ScanCompletedListener {
        void onScanCompleted();
    }

}
