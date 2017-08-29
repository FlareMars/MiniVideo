package com.jb.zcamera.camera;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;

class MySurfaceView extends SurfaceView {
	private static final String TAG = "MySurfaceView";

	private Preview preview = null;
	private int [] measure_spec = new int[2];
	
	MySurfaceView(Context context, Bundle savedInstanceState, Preview preview) {
		super(context);
		this.preview = preview;
		Log.d(TAG, "new MySurfaceView");
	}

//	@SuppressLint("ClickableViewAccessibility")
//	public boolean onTouchEvent(MotionEvent event) {
//		return preview.touchEvent(event);
//    }
//
//	@Override
//	public void onDraw(Canvas canvas) {
//	    super.onDraw(canvas);
//		preview.draw(this, canvas);
//	}

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        preview.getMeasureSpec(this, measure_spec, widthSpec, heightSpec);
        super.onMeasure(measure_spec[0], measure_spec[1]);
    }
}
