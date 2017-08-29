
package com.jb.zcamera.camera;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * 对焦距离检测
 * 
 * @author oujingwen
 *
 */
public class FocusDistanceChecker implements SensorEventListener {
    private static final int MSG_FOR_SENSOR_FOCUS = 11;

    private static final float STABILIZE_ANGLE = 3.0F;

    private static final float STABILIZE_SUM_ANGLE = 5.0F;

    private static final int STABLE_TIME = 400;

    private static final String TAG = "DistanceChecker";

    private static final float TRIGGER_FOCUS_ANGLE = 30.0F;

    private static final float TRIGGER_FOCUS_SUM_ANGLE = 30.0F;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message paramMessage) {
            super.handleMessage(paramMessage);
            if ((FocusDistanceChecker.this.mListener != null)
                    && (FocusDistanceChecker.this.mListener.preCheck())) {
                FocusDistanceChecker.this.mStabilizeSensorValue = new float[3];
                FocusDistanceChecker.this.mIsBeyondSensorThreshold = false;
                FocusDistanceChecker.this.updateLastSensorValues();
                Log.i("DistanceChecker", "start onDistanceChanged");
                FocusDistanceChecker.this.mListener.onDistanceChanged();
            }
        }
    };

    private boolean mIsBeyondSensorThreshold;

    private float[] mLastSensorValues = new float[3];

    private DistanceCheckerListener mListener;

    private Sensor mSensor;

    private SensorManager mSensorManager;

    float[] mStabilizeSensorValue = new float[3];

    private float[] mTempSensorValues = new float[3];

    public FocusDistanceChecker(Context paramContext,
                                DistanceCheckerListener paramDistanceCheckerListener) {
        this.mSensorManager = ((SensorManager)paramContext.getApplicationContext().getSystemService("sensor"));
        this.mListener = paramDistanceCheckerListener;
    }

    private boolean checkRange(float[] values1, float[] values2, float singleThreshold,
            float totalThreshold) {
        if ((values1.length < 3) || (values2.length < 3)) {
            return false;
        }
        float f1 = 0.0F;
        for (int i = 0; i < 3; i++) {
            int j = 0;
            float f2 = Math.abs(values1[i] - values2[i]);
            if (f2 > 180.0F) {
                f2 = 360.0F - f2;
            }
            if (f2 > singleThreshold) {
                return true;
            }
            f1 += f2;
            if (f1 > totalThreshold) {
                return true;
            }
        }
        return false;
    }

    public void onAccuracyChanged(Sensor paramSensor, int paramInt) {
    }

    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ORIENTATION || event.values == null
                || event.values.length < 3) {
            return;
        }

        System.arraycopy(event.values, 0, this.mTempSensorValues, 0, this.mTempSensorValues.length);

        if ((this.mListener != null) && (!this.mListener.preCheck())) {
            if (this.mHandler.hasMessages(MSG_FOR_SENSOR_FOCUS)) {
                this.mHandler.removeMessages(MSG_FOR_SENSOR_FOCUS);
            }
            return;
        }

        this.mIsBeyondSensorThreshold = checkRange(event.values, this.mLastSensorValues,
                TRIGGER_FOCUS_ANGLE, TRIGGER_FOCUS_SUM_ANGLE);

        if (this.mIsBeyondSensorThreshold
                && checkRange(this.mStabilizeSensorValue, event.values, STABILIZE_ANGLE,
                        STABILIZE_SUM_ANGLE)) {
            if (this.mHandler.hasMessages(MSG_FOR_SENSOR_FOCUS)) {
                this.mHandler.removeMessages(MSG_FOR_SENSOR_FOCUS);
            }
            this.mHandler.sendEmptyMessageDelayed(MSG_FOR_SENSOR_FOCUS, STABLE_TIME);
            System.arraycopy(event.values, 0, this.mStabilizeSensorValue, 0,
                    this.mStabilizeSensorValue.length);
        }
    }

    public void register() {
        if (this.mSensor == null) {
            this.mSensor = this.mSensorManager.getDefaultSensor(3);
            if (this.mSensor != null) {
                this.mSensorManager.registerListener(this, this.mSensor, 3);
                Log.d(TAG, "Sensor type : " + this.mSensor.getType() + "/");
            }
        }
    }

    public void reset() {
        this.mLastSensorValues = new float[3];
        this.mIsBeyondSensorThreshold = false;
        this.mStabilizeSensorValue = new float[3];
    }

    public void unRegister() {
        try {
            if (this.mSensor != null) {
                this.mSensorManager.unregisterListener(this);
                this.mSensor = null;
            }
        } catch (Exception localException) {
            Log.e(TAG, "un-register the focus sensor error, just ignore it !!!");
        }
    }

    public void updateLastSensorValues() {
        System.arraycopy(this.mTempSensorValues, 0, this.mLastSensorValues, 0,
                this.mTempSensorValues.length);
    }

    public static abstract interface DistanceCheckerListener {
        public abstract void onDistanceChanged();

        public abstract boolean preCheck();
    }
}
