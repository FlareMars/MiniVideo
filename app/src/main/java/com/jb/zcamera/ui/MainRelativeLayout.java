package com.jb.zcamera.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.RelativeLayout;

public class MainRelativeLayout extends RelativeLayout {

	public MainRelativeLayout(Context context, AttributeSet attrs,
                              int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public MainRelativeLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public MainRelativeLayout(Context context) {
		super(context);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return true;
	}
}
