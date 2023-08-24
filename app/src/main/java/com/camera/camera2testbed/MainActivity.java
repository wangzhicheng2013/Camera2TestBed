package com.camera.camera2testbed;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.view.TextureView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Date;
import java.text.SimpleDateFormat;
import android.os.Environment;
import java.util.concurrent.ConcurrentLinkedDeque;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    private static final String TAG = "MainActivity";
    private TextureView mTextureView;
    private ImageView mQuitButton;
    private ImageView mDumpImageButton;
    private DmsCameraThread mDmsCameraThread;
    private DmsProcessThread mDmsProcessThread;
    public static ConcurrentLinkedDeque<byte[]>mQueue = new ConcurrentLinkedDeque<byte[]>();
    public static final int QUEUE_SIZE = 3;
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        if (false == checkPermissions()) {
            return;
        }
        initDmsThread();
    }
    private void initView() {
        mTextureView = (TextureView) findViewById(R.id.tv_camera);
        mQuitButton = findViewById(R.id.quit_btn);
        mDumpImageButton = findViewById(R.id.dump_image_btn);
        mQuitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            // jump to ID4 Demo Car MainActivity
            public void onClick(View view) {
                Log.d(TAG, "main activity exit!");
                finish();
                System.exit(0);
            }
        });
        mDumpImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String outputImagePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + timeStamp + "_" + DmsCameraThread.DMS_CAMERA_WIDTH + "x" + DmsCameraThread.DMS_CAMERA_HEIGHT + "." + DmsCameraThread.DMS_CAMERA_TYPE;
                // get pictures from /mnt/runtime/default/emulated/10/
                Toast.makeText(view.getContext(), "Dump文件是:" + outputImagePath, Toast.LENGTH_SHORT).show();
                byte[] data = mQueue.poll();
                if (data != null) {
                    Log.d(TAG, "get data:" + data.length);
                    PublicTools.dumpImage(outputImagePath, data);
                }
            }
        });
    }
    private void initDmsThread() {
        mDmsCameraThread = new DmsCameraThread(this, mTextureView);
        mDmsProcessThread = new DmsProcessThread();
        if (true == mDmsProcessThread.init()) {
            mDmsProcessThread.start();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                if (true == mDmsCameraThread.initDmsCamera()) {
                    mDmsCameraThread.start();
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
            }
        });
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestCameraPermission() {
        requestPermissions(new String[]{ Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE }, REQUEST_CAMERA_PERMISSION);
    }
    // 权限请求结果处理 权限通过 打开相机
    @RequiresApi(api = Build.VERSION_CODES.M)
    public boolean checkPermissions() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            return false;
        }
        return true;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "请授予相机权限！", Toast.LENGTH_SHORT).show();
            } else {
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}