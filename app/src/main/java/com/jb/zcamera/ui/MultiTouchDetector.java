package com.jb.zcamera.ui;

import android.graphics.PointF;
import android.view.MotionEvent;

/**
 * Created by oujingwen on 15-5-18.
 */
public class MultiTouchDetector {
    float x_down = 0;
    float y_down = 0;
    float x_move = 0;
    float y_move = 0;
    PointF start = new PointF();
    PointF mid = new PointF();
    float oldDist = 1f;
    float oldRotation = 0;

    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    int mode = NONE;

    private TouchEventListener listener;

    public MultiTouchDetector(TouchEventListener listener) {
        this.listener = listener;
    }

    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mode = DRAG;
                x_down = event.getX();
                y_down = event.getY();
                x_move = x_down;
                y_move = y_down;
                listener.onActionDown(x_down, y_down);
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                mode = ZOOM;
                oldDist = spacing(event);
                oldRotation = rotation(event);
                midPoint(mid, event);
                listener.onActionPointerDown();
                break;
            case MotionEvent.ACTION_MOVE:
                if (mode == ZOOM) {
                    float newRotation = rotation(event);
                    float rotation = newRotation - oldRotation;
                    float newDist = spacing(event);
                    float scale = newDist / oldDist;
                    if (Math.abs(rotation) > 1) {
                        oldRotation = newRotation;
                        listener.onRotation(rotation, mid.x, mid.y);
                    }
                    if (Math.abs(1 - scale) > 0.01) {
                        oldDist = newDist;
                        listener.onScale(scale);
                    }
                } else if (mode == DRAG) {
                    if (spacing(x_move, y_move, event.getX(), event.getY()) > 10) {
                        x_move = event.getX();
                        y_move = event.getY();
                        listener.onDrag(event.getX(), event.getY());
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                mode = NONE;
                listener.onActionUp(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                listener.onActionPointerUp();
                break;
        }
        return true;
    }

    // 触碰两点间距离
    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private float spacing(float x1, float y1, float x2, float y2) {
        float x = x1 - x2;
        float y = y1 - y2;
        return (float) Math.sqrt(x * x + y * y);
    }

    // 取手势中心点
    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    // 取旋转角度
    private float rotation(MotionEvent event) {
        double delta_x = (event.getX(0) - event.getX(1));
        double delta_y = (event.getY(0) - event.getY(1));
        double radians = Math.atan2(delta_y, delta_x);
        return (float) Math.toDegrees(radians);
    }

    public interface TouchEventListener {
        void onActionDown(float x, float y);
        void onActionUp(float x, float y);
        void onActionPointerDown();
        void onActionPointerUp();
        void onDrag(float x, float y);
        boolean onRotation(float rotation, float x, float y);
        boolean onScale(float scale);
    }
}
