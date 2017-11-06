
package com.vitaviva.qrcodescanner.core;

import android.hardware.Camera;
import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.Size;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.util.List;

public class CameraScanner {

    private static final String TAG = "CameraScanner";

    private static final int MSG_REQUEST_AUTO_FOCUS = 1;
    private static final int MSG_REQUEST_FRAME = 2;
    private static final int MSG_START_SCANNING = 8;
    private static final int MSG_PREPARE_SCANNING = 11;
    private static final int MSG_CONFIG_FLASHLIGHT = 12;
    private static final int MSG_FINISH_SCANNING = 13;

    public static final int PREPARE_ERROR_TYPE = 1;
    public static final int CONFIG_FLASH_LIGHT_ERROR_TYPE = 2;
    public static final int START_SCAN_ERROR_TYPE = 3;

    private Camera mCamera;

    private Size mCameraSize;

    private boolean isSupportFlashLight = false;

    private boolean isPreparedToScan = false;

    private ScannerEventHandler mEventHandler;

    private Camera.PreviewCallback mPreviewCallback;

    private OnCompleteListener mOnCompleteListener;

    private OnErrorListener mOnErrorListener;

    private OnStartedListener mOnStartedListener;

    private int rotation = 0;

    private int reqWidth = 640;
    private int reqHeight = 480;

    private CameraScanner() {
        HandlerThread thread = new HandlerThread("scanThread");
        thread.start();
        mEventHandler = new ScannerEventHandler(thread.getLooper());
    }

    private static CameraScanner mInstance = new CameraScanner();

    public static CameraScanner getInstance() {
        return mInstance;
    }

    public void prepareAsync(Object listener) {
        mEventHandler.obtainMessage(MSG_PREPARE_SCANNING, listener).sendToTarget();
    }

    public void start() {
        if (!isPreparedToScan) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "start():not isPreparedToScan");
            }
            return;
        }
        mEventHandler.obtainMessage(MSG_START_SCANNING).sendToTarget();
    }

    public void releaseAsync(OnCompleteListener listener) {
        mEventHandler.removeMessages();
        mEventHandler.obtainMessage(MSG_FINISH_SCANNING, listener).sendToTarget();
    }

    private final Camera.AutoFocusCallback mAutoFocusCallback = new Camera.AutoFocusCallback() {

        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            requestNextFrame();
        }
    };

    private class ScannerEventHandler extends Handler {

        ScannerEventHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_PREPARE_SCANNING: {
                    if (BuildConfig.DEBUG) {
                        Log.v(TAG, "camera prepare scanning");
                    }
                    if (msg.obj instanceof OnErrorListener) {
                        mOnErrorListener = (OnErrorListener) msg.obj;
                    }
                    removeMessages();
                    if (mCamera == null) {
                        try {
                            mCamera = Camera.open();
                            mCamera = new BarcodeCameraConfig(mCamera).configDisplayOrientation(rotation)
                                    .configPreviewSize2(reqWidth, reqHeight)
                                    .configFocusMode()
                                    .getCamera();
                        } catch (Throwable e) {
                            if (mOnErrorListener != null) {
                                if (BuildConfig.DEBUG) {
                                    Log.d(TAG, "Camera.open() error");
                                }
                                mOnErrorListener.onError(PREPARE_ERROR_TYPE);
                            }
                            return;
                        }
                    }
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "mCamera inited");
                    }

                    mCamera.setErrorCallback(new ErrorCallback() {
                        @Override
                        public void onError(int error, Camera camera) {
                            if (BuildConfig.DEBUG) {
                                Log.d(TAG, error + "");
                            }
                            releaseAsync(mOnCompleteListener);
                        }
                    });

                    List<String> flashModes = null;
                    try {
                        mCameraSize = mCamera.getParameters().getPreviewSize();
                        flashModes = mCamera.getParameters().getSupportedFlashModes();
                    } catch (Exception e) {
                        if (mOnErrorListener != null) {
                            mOnErrorListener.onError(PREPARE_ERROR_TYPE);
                        }
                        return;
                    }
                    if (flashModes != null) {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, flashModes.toString());
                        }
                    }
                    isSupportFlashLight = flashModes != null && flashModes.contains(Camera.Parameters.FLASH_MODE_TORCH) && flashModes.contains(Camera.Parameters.FLASH_MODE_OFF);

                    isPreparedToScan = true;

                    OnPreparedListener mOnPreparedListener = (OnPreparedListener) msg.obj;
                    if (mOnPreparedListener != null) {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "mOnPreparedListener.onPrepared()");
                        }
                        mOnPreparedListener.onPrepared(mCamera);
                    }
                    break;
                }
                case MSG_FINISH_SCANNING: {
                    if (BuildConfig.DEBUG) {
                        Log.v(TAG, "camera finish scanning");
                    }
                    removeMessages();
                    isPreparedToScan = false;
                    isSupportFlashLight = false;
                    if (mCamera != null) {
                        try {
                            mCamera.stopPreview();
                            mCamera.setPreviewDisplay(null);
                        } catch (Exception e) {
                        }
                        try {
                            mCamera.setOneShotPreviewCallback(null);
                        } catch (Exception e) {
                        }
                        try {
                            mCamera.setErrorCallback(null);
                        } catch (Exception e) {
                        }
                        try {
                            mCamera.stopSmoothZoom();
                        } catch (Exception e) {
                        }
                        try {
                            mCamera.setPreviewCallback(null);
                            mCamera.release();
                        } catch (Exception e) {
                        }
                        mCamera = null;
                    }
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "mCamera release");
                    }
                    mOnErrorListener = null;
                    mPreviewCallback = null;
                    mOnStartedListener = null;
                    mOnCompleteListener = (OnCompleteListener) msg.obj;
                    if (mOnCompleteListener != null) {
                        mOnCompleteListener.onComplete();
                        mOnCompleteListener = null;
                    }
                    if (BuildConfig.DEBUG) {
                        Log.v(TAG, "camera finish scanning end");
                    }
                    break;
                }
                case MSG_START_SCANNING: {
                    if (BuildConfig.DEBUG) {
                        Log.v(TAG, "camera start scanning");
                    }
                    if (!isPreparedToScan) {
                        return;
                    }
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "startPreview");
                    }
                    try {
                        mCamera.startPreview();
                    } catch (Exception e) {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "startPreview error");
                        }
                        if (mOnErrorListener != null) {
                            mOnErrorListener.onError(START_SCAN_ERROR_TYPE);
                        }
                    }
                    if (mOnStartedListener != null) {
                        mOnStartedListener.onStarted(mCamera);
                    }
                    sendEmptyMessage(MSG_REQUEST_AUTO_FOCUS);
                    break;
                }
                case MSG_REQUEST_AUTO_FOCUS: {
                    if (BuildConfig.DEBUG) {
                        Log.v(TAG, "camera autofocus");
                    }
                    if (!isPreparedToScan) {
                        return;
                    }
                    try {
                        removeMessages(MSG_REQUEST_AUTO_FOCUS);
                        mCamera.autoFocus(mAutoFocusCallback);
                    } catch (Throwable e) {
                        if (BuildConfig.DEBUG) {
                            Log.v(TAG, "camera autofocus error");
                        }
                        removeMessages(MSG_REQUEST_FRAME);
                        sendMessageDelayed(obtainMessage(MSG_REQUEST_FRAME), 30);
                    }
                    sendEmptyMessageDelayed(MSG_REQUEST_AUTO_FOCUS, 1500L);
                    if (BuildConfig.DEBUG) {
                        Log.v(TAG, "camera autofocus end");
                    }
                    break;
                }
                case MSG_REQUEST_FRAME: {
                    if (BuildConfig.DEBUG) {
                        Log.v(TAG, "camera request frame");
                    }
                    //BLog.d(TAG, "msg:MSG_REQUEST_FRAME");
                    if (!isPreparedToScan) {
                        return;
                    }
                    mCamera.setOneShotPreviewCallback(mPreviewCallback);
                    break;
                }
                case MSG_CONFIG_FLASHLIGHT: {
                    if (BuildConfig.DEBUG) {
                        Log.v(TAG, "camera config");
                    }
                    if (!isPreparedToScan) {
                        return;
                    }
                    try {
                        mCamera = new BarcodeCameraConfig(mCamera).configFlashlight((Boolean) msg.obj)
                                .getCamera();
                    } catch (Exception e) {
                        if (mOnErrorListener != null) {
                            mOnErrorListener.onError(CONFIG_FLASH_LIGHT_ERROR_TYPE);
                        }
                    }
                    break;
                }
            }
        }

        private void removeMessages() {
            mEventHandler.removeMessages(MSG_START_SCANNING);
            mEventHandler.removeMessages(MSG_REQUEST_AUTO_FOCUS);
            mEventHandler.removeMessages(MSG_REQUEST_FRAME);
            mEventHandler.removeMessages(MSG_CONFIG_FLASHLIGHT);
        }
    }

    public void configFlashLight(boolean openFlashLight) {
        if (!isPreparedToScan) {
            return;
        }
        mEventHandler.sendMessage(mEventHandler.obtainMessage(MSG_CONFIG_FLASHLIGHT,
                openFlashLight));
    }

    public void requestNextFrame() {
        if (!isPreparedToScan) {
            return;
        }
        mEventHandler.removeMessages(MSG_REQUEST_FRAME);
        if (BuildConfig.DEBUG) {
            Log.v(TAG, "next frame message");
        }
        mEventHandler.sendMessageDelayed(mEventHandler.obtainMessage(MSG_REQUEST_FRAME), 10);
    }

    public void setPreviewCallback(Camera.PreviewCallback mPreviewCallback) {
        this.mPreviewCallback = mPreviewCallback;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
    }

    public interface OnPreparedListener {
        void onPrepared(Camera camera);
    }

    public interface OnStartedListener {
        void onStarted(Camera camera);
    }

    public interface OnCompleteListener {
        void onComplete();
    }

    public interface OnErrorListener {
        void onError(int type);
    }

    public void setReqSize(int width, int height) {
        reqWidth = width;
        reqHeight = height;
    }

    public Size getCameraSize() {
        return mCameraSize;
    }


    public boolean isSupportFlashLight() {
        return isSupportFlashLight;
    }

    public void setOnStartedListener(OnStartedListener listener) {
        mOnStartedListener = listener;
    }
}
