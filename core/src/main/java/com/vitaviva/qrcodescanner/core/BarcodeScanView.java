
package com.vitaviva.qrcodescanner.core;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.io.IOException;
import java.lang.ref.WeakReference;

@SuppressWarnings("deprecation")
public class BarcodeScanView extends SurfaceView implements SurfaceHolder.Callback,
        CameraScanner.OnPreparedListener, CameraScanner.OnCompleteListener,
        CameraScanner.OnErrorListener, CameraScanner.OnStartedListener {

    private static final String TAG = "BarcodeScanView";

    private static final String CROPPED_AREA = "croppedArea";

    // private static final int MSG_REQUEST_AUTOFOCUS = 1;
    // private static final int MSG_REQUEST_FRAME = 2;
    private static final int MSG_BARCODE_RESULT = 3;
    private static final int MSG_DECODE_IMAGE = 4;
    // private static final int MSG_DECODE_FILE = 5;
    // private static final int MSG_DECODE_FILE_FAIL = 7;
    // private static final int MSG_START_SCANNING = 8;
    // private static final int MSG_STOP_SCANNING = 9;
    private static final int MSG_SET_DECODE_TYPE = 10;
    // private static final int MSG_PREPARE_SCANNING = 11;
    // private static final int MSG_CONFIG_FLASHLIGHT = 12;
    // private static final int MSG_FINISH_SCANNING = 13;
    public static final int MSG_SCAN_PREPARED = 14;
    public static final int MSG_SCAN_FINISHED = 15;
    // private static final int MSG_CACULATE_CROPPED_AREA = 16;
    //private static final int MSG_ADJUST_SURFACE_SIZE = 17;

    public static final int MSG_SCAN_ERROR = 18;
    public static final int MSG_SCAN_BEGINNED = 19;

    private IScanCursorView scanCursorView;

    private CameraScanner mScanner;

    private int screenWidth;
    private int screenHeight;

    private OnPreparedListener mOnPreparedListener = null;
    private OnErrorListener mOnErrorListener = null;
    private DecodeManger mDecodeManager = null;
    private DecodeManger.DecodeResultListener mResultListener;

    public static final int PREPARE_ERROR_TYPE = CameraScanner.PREPARE_ERROR_TYPE;

    public static final int CONFIG_FLASH_LIGHT_ERROR_TYPE = CameraScanner.CONFIG_FLASH_LIGHT_ERROR_TYPE;

    public static final int START_SCAN_ERROR_TYPE = CameraScanner.START_SCAN_ERROR_TYPE;

    public static final int DECODE_ERROR_TYPE = 10;

    private boolean autoPrepareMode = true;

    public Size getCameraSize() {
        return mScanner.getCameraSize();
    }

    private Handler uiHandler = new UiHandler(this);

    private Rect getCursorViewRelativePosition() {
        Rect result;
        if (scanCursorView != null) {
            Rect absRect = scanCursorView.getCropAreaInWindow();
            int location[] = new int[2];
            getLocationInWindow(location);
            result = new Rect(absRect.left - location[0], absRect.top - location[1],
                    absRect.right
                            - location[0], absRect.bottom - location[1]);
        } else {
            result = new Rect();
        }
        return result;
    }

    private Rect calculateCroppedPreviewArea() {
        Rect scanCursorArea = new Rect();
        if (mDecodeManager != null) {
            Size cameraSize = mScanner.getCameraSize();
            if (cameraSize != null) {
                float wRatio;
                float hRatio;

                scanCursorArea = getCursorViewRelativePosition();
                if (screenWidth >= screenHeight) {

                    wRatio = (float) cameraSize.width / getWidth();
                    hRatio = (float) cameraSize.height / getHeight();

                    scanCursorArea.left *= wRatio;
                    scanCursorArea.top *= hRatio;
                    scanCursorArea.right *= wRatio;
                    scanCursorArea.bottom *= hRatio;
                }
                // portrait
                else {

                    wRatio = (float) cameraSize.width / getHeight();
                    hRatio = (float) cameraSize.height / getWidth();
                    // translate coordinate
                    Rect tmpRect = new Rect(scanCursorArea.top, getWidth() - scanCursorArea.right,
                            scanCursorArea.bottom, getWidth() - scanCursorArea.left);
                    scanCursorArea.left = (int) (tmpRect.left * wRatio);
                    scanCursorArea.top = (int) (tmpRect.top * hRatio);
                    scanCursorArea.right = (int) (tmpRect.right * wRatio);
                    scanCursorArea.bottom = (int) (tmpRect.bottom * hRatio);
                }

                Rect previewArea = new Rect(0, 0, cameraSize.width, cameraSize.height);

                if (!previewArea.contains(scanCursorArea)) {
                    scanCursorArea.set(previewArea);
                }

            }
        }
        return scanCursorArea;
    }

    public BarcodeScanView(Context context) {
        this(context, null);
        init();
    }

    public BarcodeScanView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        init();
    }

    public BarcodeScanView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        // init surfaceholder
        SurfaceHolder holder = getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder.addCallback(this);

        WindowManager manager = (WindowManager) getContext().getSystemService(
                Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        screenWidth = display.getWidth();
        screenHeight = display.getHeight();
        int width = screenWidth >= screenHeight ? screenWidth : screenHeight;
        int height = screenWidth + screenHeight - width;
        int rotation = display.getRotation();
        mScanner = CameraScanner.getInstance();
        mScanner.setRotation(0);  //此处直接设为0，配合竖屏展示
        mScanner.setReqSize(width, height);

    }


    public void setDecodeResultListener(DecodeManger.DecodeResultListener listener) {
        this.mResultListener = listener;
    }

    public void decode(Uri uri) {
        if (mDecodeManager != null) {
            mDecodeManager.decode(uri);
        }
    }

    public void destroy() {
        if (mDecodeManager != null) {
            mDecodeManager.destroy();
        }
        setOnErrorListener(null);
        setOnPreparedListener(null);
    }

    public void prepareAsync() {
        if (BuildConfig.DEBUG) {
            Log.v(TAG, "getHeight-->" + getHeight() + "  getWidht2-->" + getWidth());
        }
        mScanner.setReqSize(getHeight(), getWidth());
        // init scan thread and handler
        mScanner.prepareAsync(this);
    }

    public void start() {
        mScanner.setOnStartedListener(this);
        mScanner.setPreviewCallback(mPreviewCallback);
        mScanner.start();
        if (scanCursorView != null) {
            scanCursorView.startScanning();
        }
    }

    private final Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            DecodeManger decodeManger = DecodeManger.getInstance(getContext().getApplicationContext());
            if (!decodeManger.isDecoding()) {  // 此处会导致内存泄露。发送消息太快，但解码太慢，而每张图片接近2M，一直发会存起来
                Size size = getCameraSize();
                decodeManger.decode(data, size.width, size.height, calculateCroppedPreviewArea());
            }
        }
    };


    public void releaseAsync() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "releaseAsync setBackgroundColor(Color.BLACK)");
        }
        setBackgroundColor(Color.BLACK);
        mScanner.releaseAsync(this);
        if (scanCursorView != null) {
            scanCursorView.stopScanning();
        }
    }

    public void requestNextFrame() {
        mScanner.requestNextFrame();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "surfaceCreated");
        }
        setBackgroundColor(Color.BLACK);
        hasSurface = true;
        if (autoPrepareMode) {
            prepareAsync();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "surfaceChanged  width:" + width + " height:" + height);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //releaseAsync();
        hasSurface = false;
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "surfaceDestroyed");
        }
    }

    public void setScanCursorView(IScanCursorView scanCursorView) {
        this.scanCursorView = scanCursorView;
    }

    public interface OnPreparedListener {
        void onPrepared(BarcodeScanView scanView);
    }

    public interface OnCompleteListener {
        void onComplete(BarcodeScanView scanView);
    }

    public interface OnErrorListener {
        void onError(BarcodeScanView scanView, int type);
    }

    public void setOnPreparedListener(OnPreparedListener listener) {
        mOnPreparedListener = listener;
    }

    public void setOnErrorListener(OnErrorListener mOnErrorListener) {
        this.mOnErrorListener = mOnErrorListener;
    }

    public void setAutoPrepareMode(boolean autoPrepare) {
        autoPrepareMode = autoPrepare;
    }

    @Override
    public void onPrepared(Camera camera) {
        try {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "setPreviewDisplay");
            }
            camera.setPreviewDisplay(getHolder());
        } catch (IOException e) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "setPreviewDisplay error");
            }
            uiHandler.obtainMessage(MSG_SCAN_ERROR, PREPARE_ERROR_TYPE, 0).sendToTarget();
            return;
        }
        uiHandler.sendEmptyMessage(MSG_SCAN_PREPARED);
    }

    @Override
    public void onComplete() {
        uiHandler.sendEmptyMessage(MSG_SCAN_FINISHED);
    }

    @Override
    public void onError(int type) {
        uiHandler.obtainMessage(MSG_SCAN_ERROR, type, 0).sendToTarget();
    }

    private boolean hasSurface = false;

    public boolean hasSurface() {
        return hasSurface;
    }

    @Override
    public void onStarted(Camera camera) {
        uiHandler.sendEmptyMessage(MSG_SCAN_BEGINNED);
    }

    public void scanPrepared() {
        if (mOnPreparedListener != null) {
            mOnPreparedListener.onPrepared(BarcodeScanView.this);
        }
    }

    public void scanError(int type) {
        setBackgroundColor(Color.BLACK);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "MSG_SCAN_ERROR setBackgroundColor(Color.BLACK)");
        }
        if (mOnErrorListener != null) {
            mOnErrorListener.onError(BarcodeScanView.this, type);
        }
    }

    public void scanError() {
        setBackgroundColor(Color.TRANSPARENT);
    }

    static class UiHandler extends Handler {
        WeakReference<BarcodeScanView> reference;

        public UiHandler(BarcodeScanView barcodeScanView) {
            reference = new WeakReference<>(barcodeScanView);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case BarcodeScanView.MSG_SCAN_PREPARED: {
                    if (reference.get() != null) {
                        reference.get().mDecodeManager = DecodeManger.getInstance(reference.get().getContext().getApplicationContext());
                        if (reference.get().mResultListener != null) {
                            reference.get().mDecodeManager.setDecodeResultListener(reference.get().mResultListener);
                        }
                        reference.get().scanPrepared();
                    }
                    break;
                }
                case BarcodeScanView.MSG_SCAN_ERROR: {
                    if (reference.get() != null) {
                        reference.get().scanError(msg.arg1);
                    }
                    break;
                }
                case BarcodeScanView.MSG_SCAN_BEGINNED: {
                    if (reference.get() != null) {
                        reference.get().scanError();
                    }
                    break;
                }
            }
        }

    }

}

