package com.jb.zcamera.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

/**
 * 预览覆盖层，用于预览View上层绘画
 *
 * Created by oujingwen on 15-9-2.
 */
public class FocusOverlay extends View {

    private Preview mPreview;
    private Paint mPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);

    public FocusOverlay(Context context, Preview preview) {
        super(context);
        this.mPreview = preview;
    }

    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent(MotionEvent event) {
        if (mPreview != null) {
            return mPreview.touchEvent(event);
        }
        return false;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mPreview != null) {
            mPreview.draw(this, canvas, mPaint);
        }
    }
}
