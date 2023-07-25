package com.camera.camera2testbed;

import android.content.Context;
import android.util.Log;
import android.view.TextureView;

import java.util.concurrent.ConcurrentLinkedDeque;

public class DmsCameraThread extends Thread implements StreamCaptureCallback {
    private static final String TAG = "DmsCameraThread";
    private BasicCameraCapture mDmsCameraCapture;
    private final int DMS_CAMERA_ID = 0;
    public static final int DMS_CAMERA_WIDTH = 1920;
    public static final int DMS_CAMERA_HEIGHT = 1080;
    public static final String DMS_CAMERA_TYPE = "NV12";

    public DmsCameraThread(Context context, TextureView textureView) {
        mDmsCameraCapture = new Camera2Capture(context, textureView);
        mDmsCameraCapture.setCameraId(DMS_CAMERA_ID);
        mDmsCameraCapture.setCameraFormat(Camera2Capture.NV12);
        mDmsCameraCapture.setCameraScale(DMS_CAMERA_WIDTH, DMS_CAMERA_HEIGHT);
        mDmsCameraCapture.setStreamCaptureCallback(this);
    }

    public boolean initDmsCamera() {
        return mDmsCameraCapture.initCamera();
    }
    @Override
    public void captureCallback(byte[] data) {
        if (MainActivity.mQueue.size() < MainActivity.QUEUE_SIZE) {
            MainActivity.mQueue.offer(data);
            Log.d(TAG, "offer to the queue!");
        }
        else {
            Log.d(TAG, "queue is full!");
        }
    }
    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(10000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
