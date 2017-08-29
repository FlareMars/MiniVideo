
package com.jb.zcamera.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.animation.OvershootInterpolator;

import com.jb.zcamera.utils.DimensUtil;

/**
 * 对焦帮助类
 * 
 * @author oujingwen
 */
public class FocusHelper {
    public static final int FOCUS_WAITING = 0;

    public static final int FOCUS_SUCCESS = 1;

    public static final int FOCUS_FAILED = 2;

    public static final int FOCUS_DONE = 3;
    
    public static final int FOCUS_WAITING_STATE_NONE = 0;
    
    public static final int FOCUS_WAITING_STATE_TAKE_PIC = 1;
    
    public static final int FOCUS_WAITING_STATE_TAKE_OR_PENDING = 2;
    
    private static final int FOCUS_DURATION_MS = 500;

    private static final int OUTER_CIRCLE_RADIUS = 40;
    private static final int INER_CIRCLE_RADIUS = 15;

    private static final int OUTER_CIRCLE_WIDTH = 2;
    private static final int INER_CIRCLE_WIDTH = 3;

    private static final int AMPLITUDE = 8;

    private int mOuterCircleRadius;
    private int mInerCircleRadius;

    private int mOuterCircleWidth;
    private int mInerCircleWidth;

    private int mAmplitude;

    private OvershootInterpolator mInterpolator;

    // 对焦状态最大停留时间
    private static int MAX_STAY_TIME = 500;

    private boolean hasFocusArea = false;

    private int focusScreenX = 0;

    private int focusScreenY = 0;

    private long focusCompleteTime = -1;
    
    private long focusStartTime;

    private int focusState =  FOCUS_DONE;

    private float scaledDensity;

    private int mFocusWaitingState;
    
    private long mKeepTimestamp;
    
    private boolean isAutoFocus;

    private FocusOverlay mFocusOverlay;
    
    public FocusHelper(Context context, FocusOverlay focusOverlay) {
        scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
        mOuterCircleRadius = DimensUtil.dip2px(scaledDensity, OUTER_CIRCLE_RADIUS);
        mInerCircleRadius = DimensUtil.dip2px(scaledDensity, INER_CIRCLE_RADIUS);
        mOuterCircleWidth = DimensUtil.dip2px(scaledDensity, OUTER_CIRCLE_WIDTH);
        mInerCircleWidth = DimensUtil.dip2px(scaledDensity, INER_CIRCLE_WIDTH);
        mAmplitude = DimensUtil.dip2px(scaledDensity, AMPLITUDE);
        mInterpolator = new OvershootInterpolator();
        mFocusOverlay = focusOverlay;
    }

    public synchronized boolean hasFocusArea() {
        return hasFocusArea;
    }

    public synchronized void setHasFocusArea(boolean hasFocusArea) {
        this.hasFocusArea = hasFocusArea;
    }

    public synchronized int getFocusScreenX() {
        return focusScreenX;
    }

    public synchronized int getFocusScreenY() {
        return focusScreenY;
    }

    public synchronized void setFocusScreen(int focusScreenX, int focusScreenY) {
        this.focusScreenX = focusScreenX;
        this.focusScreenY = focusScreenY;
    }

    public synchronized long getFocusCompleteTime() {
        return focusCompleteTime;
    }

    public synchronized void setFocusComplete(int focusSuccess, long focusCompleteTime) {
        if (focusSuccess == FOCUS_WAITING) {
            focusStartTime = System.currentTimeMillis();
        }
        this.focusCompleteTime = focusCompleteTime;
        this.setFocusState(focusSuccess);
    }

    public synchronized int getFocusState() {
        return focusState;
    }

    public synchronized void setFocusState(int focusState) {
        this.focusState = focusState;
        mFocusOverlay.postInvalidate();
    }
    
    public synchronized void clearFocusState() {
        if (focusState == FOCUS_SUCCESS || focusState == FOCUS_FAILED) {
            setFocusState(FOCUS_DONE);
        }
    }

    public synchronized boolean isFocusNone() {
        return focusState == FOCUS_DONE;
    }

    public synchronized boolean isFocusWaiting() {
        if (focusState == FOCUS_WAITING) {
            return System.currentTimeMillis() - focusStartTime < 5000;
        }
        return false;
    }

    public synchronized boolean isFocusSuccess() {
        return focusState == FOCUS_SUCCESS;
    }

    public synchronized boolean isFocusFailed() {
        return focusState == FOCUS_FAILED;
    }

    public synchronized void setFocusNone() {
        setFocusState(FOCUS_DONE);
    }

    public synchronized void setFocusWaiting() {
        focusStartTime = System.currentTimeMillis();
        setFocusState(FOCUS_WAITING);
    }

    public synchronized void setFocusSuccess() {
        setFocusState(FOCUS_SUCCESS);
    }

    public synchronized void setFocusFailed() {
        setFocusState(FOCUS_FAILED);
    }
    
    public synchronized int getFocusWaitingState() {
        return mFocusWaitingState;
    }

    public synchronized void setFocusWaitingState(int focusWaitingState) {
        mFocusWaitingState = focusWaitingState;
    }

    /**
     * 绘画焦点
     * 
     * @param canvas
     * @param paint
     */
    public synchronized boolean draw(Canvas canvas, Paint paint, Rect clipBounds, boolean keep) {
        if (!isAutoFocus()) {
            return false;
        }
        if (isFocusNone()) {
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        long stayTime = currentTime - focusCompleteTime;
        
        if (keep && mKeepTimestamp == 0) {
            mKeepTimestamp = currentTime;
        } else if (!keep && mKeepTimestamp != 0) {
            mKeepTimestamp = 0;
        }
        
        if (!isFocusWaiting()
                && stayTime > MAX_STAY_TIME
                && !(keep && focusCompleteTime > mKeepTimestamp)) {
            return false;
        }

        canvas.save();

        // 焦点位置
        int pos_x;
        int pos_y;
        if (hasFocusArea) {
            pos_x = focusScreenX;
            pos_y = focusScreenY;
        } else {
            pos_x = clipBounds.width() / 2;
            pos_y = clipBounds.height() / 2;
        }

        // 设置画笔
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(mOuterCircleWidth);
        canvas.drawCircle(pos_x + clipBounds.left, pos_y + clipBounds.top, mOuterCircleRadius, paint);

        float inerCircleR = mInerCircleRadius;
        long focusTime = currentTime - focusStartTime;

        if (focusTime < FOCUS_DURATION_MS) {
            inerCircleR = mInterpolator.getInterpolation(focusTime / (float)FOCUS_DURATION_MS) * mAmplitude + mInerCircleRadius;
        }

        paint.setStrokeWidth(mInerCircleWidth);
        canvas.drawCircle(pos_x + clipBounds.left, pos_y + clipBounds.top, inerCircleR, paint);
        canvas.restore();

        mFocusOverlay.postInvalidateDelayed(80);
        return true;
    }

    public synchronized boolean isAutoFocus() {
        return isAutoFocus;
    }

    public synchronized void setAutoFocus(boolean isAutoFocus) {
        this.isAutoFocus = isAutoFocus;
    }
    
}
