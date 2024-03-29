package com.camera.camera2testbed;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class BasicCamera2 {
    private static final String TAG = "Camera2TestBed";
    private int mPreviewFormat = -1;
    private Size mPreviewSize = null;
    private String mCameraId;
    private Context mContext;
    private CameraManager mCameraManager = null;
    private CameraDevice mCameraDevice = null;
    private TextureView mTextureView;
    private ImageReader mPreviewReader = null;

    // background thread to process image
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    private int save_pic_count = 0;
    final private int save_pic_sum = 10;
    private byte mByteNv12[] = null;

    private int mCount = 0;
    private int mFrameCount = 0;
    private long mProcessTime = 0;
    private static final int MAX_FRAME_COUNT = 100;
    private static final int OPPO_DEVICE = 0;
    private static final int CAR_DEVICE = 1;
    private int device_type = OPPO_DEVICE;
    private static int IMAGE_CACHE_CAPACITY = 3;
    private static final int PREVIEW_FORMAT = ImageFormat.YUV_420_888;

    public BasicCamera2(Context context, TextureView textureView) {
        mContext = context;
        mTextureView = textureView;
    }

    public boolean initCamera(String cameraId) {
        mCameraId = cameraId;
        if (false == initCameraManager()) {
            return false;
        }
        if (false == getCameraInfo()) {
            Log.e(TAG, "get camera info failed!");
            return false;
        }
        initBackgroundThread();
        if (false == initImageReader()) {
            return false;
        }
        if (false == openCamera()) {
            return false;
        }
        initBackgroundThread();
        Log.d(TAG, "init camera ok!");
        return true;
    }

    private boolean openCamera() {
        try {
            if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "check camera permission failed!");
                return false;
            }
            mCameraManager.openCamera(mCameraId, mCameraStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.e(TAG, "open camera failed:" + e.toString());
            return false;
        }
        return true;
    }
    private boolean initCameraManager() {
        mCameraManager = (CameraManager)mContext.getSystemService(Context.CAMERA_SERVICE);
        if (null == mCameraManager) {
            Log.e(TAG, "can not get camera manager!");
            return false;
        }
        return true;
    }
    private boolean initImageReader() {
        Log.d(TAG, "camera preview width:" + mPreviewSize.getWidth() + " height:" + mPreviewSize.getHeight() + " format:" + mPreviewFormat);
        mPreviewReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), mPreviewFormat,IMAGE_CACHE_CAPACITY);
        if (null == mPreviewReader) {
            Log.e(TAG, "ImageReader new instance failed...!");
            return false;
        }
        Log.d(TAG, "init image reader ok!");
        mPreviewReader.setOnImageAvailableListener(mPreviewImageAvailableListener, mBackgroundHandler);
        return true;
    }

    private void freeImageReader() {
        if (null == mPreviewReader) {
            return;
        }
        mPreviewReader.close();
        mPreviewReader = null;
    }
    private boolean getCameraInfo() {
        if (null == mCameraManager) {
            return false;
        }
        boolean cameraid_success = false;
        try {
            String[] cameraIdList = mCameraManager.getCameraIdList();
            if (0 == cameraIdList.length) {
                Log.e(TAG, "can not get camera id!");
                return false;
            }
            for (String cameraId : cameraIdList) {
                if (0 != cameraId.compareTo(mCameraId)) {
                    continue;
                }
                cameraid_success = true;
                Log.d(TAG, "get camera id:" + mCameraId);
                CameraCharacteristics cameraCharacteristics = mCameraManager.getCameraCharacteristics(cameraId);
                StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (!getPreviewFormat(streamConfigurationMap)) {
                    Log.e(TAG, "can not get camera preview format!");
                    return false;
                }
                if (!getPreviewSize(streamConfigurationMap)) {
                    Log.e(TAG, "can not get camera preview size!");
                    return false;
                }
                break;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.e(TAG, "get camera info exception:" + e.toString());
            return false;
        }
        return cameraid_success;
    }
    private boolean getPreviewFormat(StreamConfigurationMap streamConfigurationMap) {
        int[] formats = streamConfigurationMap.getOutputFormats();
        for (int format : formats) {
            Log.d(TAG, "camera support format:" + format);
        }
        for (int format : formats) {
            switch (format) {
                case PREVIEW_FORMAT:
                    mPreviewFormat = format;
                    Log.d(TAG, "camera preview format is:" + PREVIEW_FORMAT);
                    break;
            }
        }
        return -1 != mPreviewFormat;
    }
    private boolean getPreviewSize(StreamConfigurationMap streamConfigurationMap) {
        if (-1 == mPreviewFormat) {
            return false;
        }
        Size[] outputSizes = streamConfigurationMap.getOutputSizes(mPreviewFormat);
        for(Size size : outputSizes) {
            Log.d(TAG,"camera support preview size width:" + size.getWidth() + " height:" + size.getHeight());
        }
        if (null == mTextureView) {
            return false;
        }
        mPreviewSize = setOptimalPreviewSize(outputSizes, mTextureView.getMeasuredWidth(), mTextureView.getMeasuredHeight());
        Log.d(TAG,"best optimal preview width:" + mPreviewSize.getWidth() + " height:" + mPreviewSize.getHeight());
        return true;
    }
    private Size setOptimalPreviewSize(Size[] sizes, int previewViewWidth, int previewViewHeight) {
        List<Size> bigEnoughSizes = new ArrayList<>();
        List<Size> notBigEnoughSizes = new ArrayList<>();
        for (Size size : sizes) {
            if (size.getWidth() >= previewViewWidth && size.getHeight() >= previewViewHeight) {
                bigEnoughSizes.add(size);
            } else {
                notBigEnoughSizes.add(size);
            }
        }
        if (bigEnoughSizes.size() > 0) {
            return Collections.min(bigEnoughSizes, new Comparator<Size>() {
                @Override
                public int compare(Size lhs, Size rhs) {
                    return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                            (long) rhs.getWidth() * rhs.getHeight());
                }
            });
        } else if (notBigEnoughSizes.size() > 0) {
            return Collections.max(notBigEnoughSizes, new Comparator<Size>() {
                @Override
                public int compare(Size lhs, Size rhs) {
                    return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                            (long) rhs.getWidth() * rhs.getHeight());
                }
            });
        } else {
            Log.d(TAG, "No suitable preview size found!");
            return sizes[0];
        }
    }
    void initBackgroundThread() {
        mBackgroundThread = new HandlerThread("BasicCamera2Thread");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message message) {
                if (message.what != 1) {
                    return false;
                }
                if (null == mByteNv12) {
                    mByteNv12 = new byte[mPreviewReader.getWidth() * mPreviewReader.getHeight() * 3 / 2];
                }
                long curTimeMillis = System.currentTimeMillis();
                final Image img = (Image)message.obj;
                Log.d(TAG, "Capture Y size " + img.getPlanes()[0].getBuffer().remaining());
                Log.d(TAG, "Capture U size " + img.getPlanes()[1].getBuffer().remaining());
                Log.d(TAG, "Capture V size " + img.getPlanes()[2].getBuffer().remaining());
                Log.d(TAG, "Preview wdith:" + mPreviewReader.getWidth() + " height:" + mPreviewReader.getHeight());
                boolean ret = getBytesForImage(img);
                img.close();
                if ((save_pic_count++ <= save_pic_sum) && (true == ret)) {
                    if (ImageFormat.YUV_420_888 == PREVIEW_FORMAT) {
                        writeYUVToSdCard("NV12");
                    }
                }
                long endTimeMillis = System.currentTimeMillis();
                ++mFrameCount;
                mProcessTime += (endTimeMillis - curTimeMillis);
                if (mFrameCount >= MAX_FRAME_COUNT) {
                    Log.d(TAG, "preview process frame average elapse:" + mProcessTime * 1.0 / mFrameCount + "ms");
                    mFrameCount = 0;
                    mProcessTime = 0;
                }
                return true;
            }
        });
    }
    private boolean getBytesForImage(final Image img) {
        if (ImageFormat.YUV_420_888 == PREVIEW_FORMAT) {
            switch (device_type) {
                case OPPO_DEVICE:
                    mByteNv12 = YuvUtil.getBytesFromImageAsType(img, YuvUtil.YUV420SP, false);
                    break;
                case CAR_DEVICE:
                    byte byteY[] = new byte[img.getPlanes()[0].getBuffer().remaining()];
                    byte byteU[] = new byte[img.getPlanes()[1].getBuffer().remaining()];
                    img.getPlanes()[0].getBuffer().get(byteY, 0, img.getPlanes()[0].getBuffer().remaining());
                    img.getPlanes()[1].getBuffer().get(byteU, 0, img.getPlanes()[1].getBuffer().remaining());
                    YuvUtil.YUV420ToNV12(byteY, byteU, mByteNv12);
                    break;
            }
        }
        Log.d(TAG, "NV12 bytes:" + mByteNv12.length);
        return mByteNv12.length > 0;
    }
    // when image is ready send it to the background thread to process
    private ImageReader.OnImageAvailableListener mPreviewImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            if (!mBackgroundHandler.hasMessages(1)) {
                final Image img = reader.acquireNextImage();
                Message msg = new Message();
                msg.what = 1;
                msg.obj = img;
                mBackgroundHandler.sendMessage(msg);
            }
        }
    };
    // open camera to process
    private CameraDevice.StateCallback mCameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(mTextureView.getWidth(), mTextureView.getHeight());
            Surface surface = new Surface(surfaceTexture);
            ArrayList<Surface> previewList = new ArrayList<>();
            previewList.add(surface);
            previewList.add(mPreviewReader.getSurface());
            try {
                mCameraDevice.createCaptureSession(previewList, new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        try {
                            CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                            builder.addTarget(surface);
                            builder.addTarget(mPreviewReader.getSurface());
                            CaptureRequest captureRequest = builder.build();
                            session.setRepeatingRequest(captureRequest, new CameraCaptureSession.CaptureCallback() {
                                @Override
                                public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
                                    super.onCaptureProgressed(session, request, partialResult);
                                }

                                @Override
                                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                                    super.onCaptureCompleted(session, request, result);
                                }
                            }, mBackgroundHandler);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                    }
                }, mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            releaseCamera();
            freeImageReader();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            releaseCamera();
            freeImageReader();
        }
    };
    private void releaseCamera() {
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
    }
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        mBackgroundThread.quit();
        mBackgroundThread = null;
        mBackgroundHandler = null;
    }
    private void writeYUVToSdCard(String type) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        timeStamp += "_" + System.currentTimeMillis();
        String path = mContext.getExternalFilesDir(null).getPath();
        Log.d(TAG, "store pic path: " + path);
        String fileName = path + "/" + type + "_" + timeStamp + "_" + mPreviewReader.getWidth() + "x" + mPreviewReader.getHeight() + "." + type;
        Log.i(TAG, "file:" + fileName + " begin write!");
        FileOutputStream output = null;
        try {
            output = new FileOutputStream(fileName, true);
            output.write(mByteNv12);
        } catch (IOException e) {
            Log.e(TAG, "YUV:file error:" + e.toString());
        }
        finally {
            try {
                output.close();
                Log.i(TAG, "file:" + fileName + " write success!");
            } catch (IOException e) {
            }
        }
    }
}
