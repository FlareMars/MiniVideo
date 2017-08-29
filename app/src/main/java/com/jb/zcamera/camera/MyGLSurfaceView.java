package com.jb.zcamera.camera;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;

class MyGLSurfaceView extends GLSurfaceView {
	private static final String TAG = "MySurfaceView";

	private Preview preview = null;
	private int [] measure_spec = new int[2];
	
	MyGLSurfaceView(Context context, Bundle savedInstanceState, Preview preview) {
		super(context);
		setEGLConfigChooser(8, 8, 8, 8, 16, 0);
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
//	    preview.draw(this, canvas);
//	}

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        preview.getMeasureSpec(this, measure_spec, widthSpec, heightSpec);
        super.onMeasure(measure_spec[0], measure_spec[1]);
    }
}
