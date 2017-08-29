
package com.jb.zcamera.camera;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * 传感器帮助类
 * 
 * @author oujingwen
 *
 */
public class SensorHelper {
    private static final String TAG = "SensorHelper";

    private Context mContext;

    private SensorManager mSensorManager = null;

    private Sensor mSensorAccelerometer = null;

    private Sensor mSensorMagnetic = null;

    public SensorHelper(Context context) {
        this.mContext = context.getApplicationContext();
        mSensorManager = (SensorManager)mContext.getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            Log.d(TAG, "found accelerometer");
            mSensorAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        } else {
            Log.d(TAG, "no support for accelerometer");
        }
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null) {
            Log.d(TAG, "found magnetic sensor");
            mSensorMagnetic = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        } else {
            Log.d(TAG, "no support for magnetic sensor");
        }
    }

    public void registerAccelerometerListener(SensorEventListener accelerometerListener) {
        mSensorManager.registerListener(accelerometerListener, mSensorAccelerometer,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void registerMagneticListener(SensorEventListener magneticListener) {
        mSensorManager.registerListener(magneticListener, mSensorMagnetic,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void unregisterAccelerometerListener(SensorEventListener accelerometerListener) {
        mSensorManager.unregisterListener(accelerometerListener);
    }

    public void unregisterMagneticListener(SensorEventListener magneticListener) {
        mSensorManager.unregisterListener(magneticListener);
    }
}
