package com.jb.zcamera.camera;

public abstract class CameraControllerManager {

	public abstract int getNumberOfCameras();

	public abstract boolean isFrontFacing(int cameraId);
}
