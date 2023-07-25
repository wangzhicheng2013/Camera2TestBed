package com.camera.camera2testbed;

import java.util.concurrent.ConcurrentLinkedDeque;

public interface BasicCameraCapture {
    void setCameraId(int id);
    boolean initCamera();
    void setCameraFormat(int format);
    void setCameraScale(int width, int height);
    int getFrameLen();
    void setStreamCaptureCallback(StreamCaptureCallback cb);
}
