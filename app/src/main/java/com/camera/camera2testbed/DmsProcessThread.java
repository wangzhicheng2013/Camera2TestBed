package com.camera.camera2testbed;

import android.util.Log;

public class DmsProcessThread extends Thread {
    private static final String TAG = "DmsProcessThread";
    public DmsProcessThread() {
    }
    public boolean init() {
        Log.d(TAG, "dms process thread init ok!");
        return true;
    }
    @Override
    public void run() {
        int i = 0;
        while (true) {
            byte[] data = MainActivity.mQueue.poll();
            if (data != null) {
                Log.d(TAG, "get data:" + data.length);
            }
            if (++i >= 20) {
                try {
                    i = 0;
                    Thread.sleep(1000);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
