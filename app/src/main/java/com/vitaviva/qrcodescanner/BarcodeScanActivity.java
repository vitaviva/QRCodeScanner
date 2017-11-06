
package com.vitaviva.qrcodescanner;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Browser;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.zxing.Result;
import com.vitaviva.qrcodescanner.core.BarcodeScanView;
import com.vitaviva.qrcodescanner.core.CameraScanner;
import com.vitaviva.qrcodescanner.core.CollapsibleView;
import com.vitaviva.qrcodescanner.core.DecodeManger;
import com.vitaviva.qrcodescanner.core.DefaultScanCursorView;


public class BarcodeScanActivity extends Activity {

    private static final int MSG_DECODE_FILE = 2;
    private static final int MSG_FILE_BARCODE_RESULT = 3;
    private static final int MSG_SCAN_TIME_TOO_LONG = 4;

    private DefaultScanCursorView mDefaultScanCursorView;
    private BarcodeScanView mBarcodeScanView;
    private View mTitleBar;
    private TextView mArticleTx;
    private TextView mAlbumBtn;
    private ToggleButton mFlashLightBtn;
    private CollapsibleView mCollapsibleView;

    private ScanProgressDialog mScanProgressDialog;

    private static final int FILE_TYPE = 1;
    private static final int SCAN_TYPE = 2;

    private static final int MEDIASTORE_REQUESTCODE = 0;
    private static final int VCARD_REQUESTCODE = 1;
    private static final int TEXT_REQUESTCODE = 2;
    private static final int BARCODE_REQUESTCODE = 3;

    private static final int DEFAULT_SCAN_TIME = 60 * 1000;

    private CameraScanner mScanner;

    private Dialog mAlertDialog;

    public static boolean isResume = true;

//    private DecodeManger mDecodeManager = null;

    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 100;
    public static boolean requestedCameraPermission = false;

    //记录屏幕是否关闭过. 用于处理锁屏之后,再点亮屏幕后进入时,重新启动扫码.
    // (按Home键与锁屏不同,会销毁surfaceview,重新进入后会再次创建,创建成功之后会触发对应的调用动作)
    private boolean bScreenOffed = false;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_FILE_BARCODE_RESULT:
                    Result result = (Result) msg.obj;
                    if (msg.arg1 == DecodeManger.TYPE_FILE) {
                        mScanProgressDialog.hide();
                        mBarcodeScanView.setAutoPrepareMode(true);
                        if (result == null) {
                            Toast.makeText(BarcodeScanActivity.this, R.string.barcode_file_scan_failed_hint, Toast.LENGTH_LONG).show();
                            mBarcodeScanView.prepareAsync();
                        } else {
                            handleDecodeResult(msg.arg1, result);
                        }
                    } else if (msg.arg1 == DecodeManger.TYPE_CAMERA) {
                        if (result == null) {
                            mBarcodeScanView.requestNextFrame();
                        } else {
                            handleDecodeResult(msg.arg1, result);
                        }
                    }
                    break;
                case MSG_SCAN_TIME_TOO_LONG: {
                    Toast.makeText(BarcodeScanActivity.this, R.string.scan_time_too_long, Toast.LENGTH_LONG).show();
                    finish();
                    break;
                }
            }
        }
    };

    private DecodeManger.DecodeResultListener mResultListener = new DecodeManger.DecodeResultListener() {
        @Override
        public void onResult(int type, Result result) {
            //扫描结果，从会从这里发给子线程去过滤。  一种是文件扫描，一种是摄像头扫描
            Message.obtain(handler, MSG_FILE_BARCODE_RESULT, type, 0, result).sendToTarget();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.barcode_scan_layout);
        mScanner = CameraScanner.getInstance();
        mDefaultScanCursorView = (DefaultScanCursorView) findViewById(R.id.barcode_scan_cursorview);
        mBarcodeScanView = (BarcodeScanView) findViewById(R.id.barcode_scan_scanview);
        mBarcodeScanView.setScanCursorView(mDefaultScanCursorView);
        mBarcodeScanView.setOnErrorListener(mOnErrorListener);
        mBarcodeScanView.setOnPreparedListener(mOnPreparedListener);
        mBarcodeScanView.setDecodeResultListener(mResultListener);
        mArticleTx = (TextView) findViewById(R.id.title);
        mArticleTx.setText(R.string.title_barcode);
        mArticleTx.setTextColor(getResources().getColor(R.color.white));
        View v = findViewById(R.id.back);
        v.setBackgroundResource(R.drawable.barcode_back_bg);
        v.setOnClickListener(mOnClickListener);
        findViewById(R.id.title_left_button_line).setVisibility(View.GONE);
        mAlbumBtn = (TextView) findViewById(R.id.title_right_button);
        mAlbumBtn.setText(R.string.album);
        mAlbumBtn.setVisibility(View.VISIBLE);
        mAlbumBtn.setOnClickListener(mOnClickListener);
        mAlbumBtn
                .setTextColor(getResources().getColorStateList(R.color.barcode_album_btn_selector));
        mTitleBar = findViewById(R.id.title_bar);
        mTitleBar.setBackgroundResource(R.drawable.barcode_scan_select_picture_bg_normal);
        mFlashLightBtn = (ToggleButton) findViewById(R.id.barcode_scan_flashlight_btn);
        mFlashLightBtn.setVisibility(View.GONE);
        mCollapsibleView = (CollapsibleView) findViewById(R.id.barcode_scan_unfoldableview);
        mCollapsibleView.setAboveBitmap(R.drawable.barcode_logo_top_half,
                R.drawable.barcode_logo_bottom_half);
        mCollapsibleView.setBelowBitmap(R.drawable.barcode_logo_bottom_half,
                R.drawable.barcode_logo_top_half);


        findViewById(R.id.barcode_scan_flashlight_btn).setOnClickListener(mOnClickListener);//这三句是为了有系统按键音

        // Request Camera permission
        if (ContextCompat.checkSelfPermission(BarcodeScanActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestedCameraPermission = false;
            ActivityCompat.requestPermissions(BarcodeScanActivity.this, new String[]{
                    Manifest.permission.CAMERA
            }, MY_PERMISSIONS_REQUEST_CAMERA);
        } else {
            requestedCameraPermission = true;
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mScreenStateBroadReceiver, intentFilter);

    }

    private BarcodeScanView.OnErrorListener mOnErrorListener = new BarcodeScanView.OnErrorListener() {
        @Override
        public void onError(BarcodeScanView scanView, int type) {
            // mBarcodeScanView.releaseAsync();
            if (type == BarcodeScanView.PREPARE_ERROR_TYPE
                    || type == BarcodeScanView.CONFIG_FLASH_LIGHT_ERROR_TYPE) {
                if (mAlertDialog == null) {
                    mAlertDialog = DialogUtil.createCameraAlertDialog(BarcodeScanActivity.this,
                            R.string.hint_information, R.string.camera_open_problem, R.string.ok);
                    mAlertDialog.setOnCancelListener(new OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            finish();
                        }
                    });
                }
                mAlertDialog.show();
            }
        }

    };

    private CollapsibleView.OnFoldProgressListener mOnFoldProgressListener = new CollapsibleView.OnFoldProgressListener() {
        @Override
        public void onFoldFinish() {
            finish();
        }
    };

    private BarcodeScanView.OnPreparedListener mOnPreparedListener = new BarcodeScanView.OnPreparedListener() {
        @Override
        public void onPrepared(BarcodeScanView scanView) {
            if (mCollapsibleView.getState() == CollapsibleView.State.UNFOLDED) {
                onScanPrepared();
            } else {
                mCollapsibleView.setOnUnfoldProgressListener(mOnUnfoldProgressListener);
                mCollapsibleView.unfold();
            }
        }
    };

    private CollapsibleView.OnUnfoldProgressListener mOnUnfoldProgressListener = new CollapsibleView.OnUnfoldProgressListener() {
        @Override
        public void onUnfoldFinish() {
            onScanPrepared();
        }
    };


    private void onScanPrepared() {
        if (mScanner.isSupportFlashLight()) {
            mFlashLightBtn.setVisibility(View.VISIBLE);
            mFlashLightBtn.setOnCheckedChangeListener(flashLightBtnOnCheckedChangeListener);
            mScanner.configFlashLight(mFlashLightBtn.isChecked());
        }
        mBarcodeScanView.start();
        handler.removeMessages(MSG_SCAN_TIME_TOO_LONG);
        handler.sendEmptyMessageDelayed(MSG_SCAN_TIME_TOO_LONG, DEFAULT_SCAN_TIME);
    }

    private CompoundButton.OnCheckedChangeListener flashLightBtnOnCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            mScanner.configFlashLight(isChecked);
        }
    };

    private OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.title_right_button) {
                try {
                    if (mCollapsibleView.getState() != CollapsibleView.State.UNFOLDED) {
                        return;
                    }
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    startActivityForResult(intent, MEDIASTORE_REQUESTCODE);
                } catch (Exception e) {
                    // TODO: handle exception
                    return;
                }
            } else if (id == R.id.back) {
                onBackPressed();
            }
        }

    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MEDIASTORE_REQUESTCODE) {
            if (resultCode == RESULT_OK && null != data) {
                mBarcodeScanView.setAutoPrepareMode(false);
                Uri selectedImage = data.getData();
                mBarcodeScanView.decode(selectedImage);
                if (mScanProgressDialog == null) {
                    mScanProgressDialog = new ScanProgressDialog(this);
                    mScanProgressDialog.findViewById(R.id.update_close).setVisibility(View.GONE);
                    mScanProgressDialog.setUpdateMsg(R.string.scanning);
                }
                mScanProgressDialog.show();
            }
        } else if (requestCode == BARCODE_REQUESTCODE) {
            if (resultCode == RESULT_OK) {
                finish();
            }
        } else if (requestCode == TEXT_REQUESTCODE) {
            if (resultCode == RESULT_OK) {
                finish();
            }
        } else if (requestCode == VCARD_REQUESTCODE) {
            if (resultCode == RESULT_OK) {
                finish();
            }
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        if (mScanProgressDialog != null) {
            mScanProgressDialog.dismiss();
        }
        unregisterReceiver(mScreenStateBroadReceiver);

        handler.removeMessages(MSG_SCAN_TIME_TOO_LONG);
        mCollapsibleView.release();
        mBarcodeScanView.destroy();
        mDefaultScanCursorView.release();
    }

    private void handleDecodeResult(int type, Result result) {
        if (Patterns.WEB_URL.matcher(result.getText().trim()).matches()) {
            loadUrl(result);
        } else {
            Intent intent = new Intent(this, TextScanResultActivity.class);
            intent.putExtra(TextScanResultActivity.KEY_TEXT, result.getText());
            startActivityForResult(intent, TEXT_REQUESTCODE);
        }
    }

    private void loadUrl(Result result) {
        String urlStr = result.getText().trim();
        String scheme = Uri.parse(urlStr).getScheme();
        if (TextUtils.isEmpty(scheme)) {
            urlStr = "http://" + urlStr;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlStr));
        intent.putExtra(Browser.EXTRA_APPLICATION_ID, getPackageName());
        intent.putExtra(Browser.EXTRA_CREATE_NEW_TAB, true);
        startActivity(intent);
        finish();
    }


    public void finish(boolean animation) {
        if (animation) {
            // mBarcodeScanView.releaseAsync();
            mCollapsibleView.setOnFoldProgressListener(mOnFoldProgressListener);
            mCollapsibleView.fold();
        } else {
            finish();
        }
    }

    private void backToHome() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        startActivity(intent);
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (mCollapsibleView.getState() == CollapsibleView.State.ISFOLDING
                || mCollapsibleView.getState() == CollapsibleView.State.ISUNFOLDING) {
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_CAMERA) {
            return true;
        }
        if (mCollapsibleView.getState() == CollapsibleView.State.ISFOLDING
                || mCollapsibleView.getState() == CollapsibleView.State.ISUNFOLDING) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (!isResume && bScreenOffed && mBarcodeScanView.hasSurface()) {
            mBarcodeScanView.prepareAsync();
        }
        bScreenOffed = false;

        isResume = true;
        // final IntentFilter homeFilter = new
        // IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        // registerReceiver(homePressReceiver, homeFilter);
        mAlbumBtn.setEnabled(true);
    }

    public void onPause() {
        super.onPause();
        isResume = false;
        // the only accessible place to invoke releaseAsync()
        mBarcodeScanView.releaseAsync();

        mCollapsibleView.setOnUnfoldProgressListener(null);
        handler.removeMessages(MSG_SCAN_TIME_TOO_LONG);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA:
                requestedCameraPermission = true;
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // only to adapt the condition that recover back from lock-screen state
                    if (mBarcodeScanView.hasSurface()) {
                        mBarcodeScanView.prepareAsync();
                    }
                } else {
                    Toast.makeText(BarcodeScanActivity.this, "请授予相机权限，否则二维码扫描功能不能使用！！！", Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public void onBackPressed() {
        mFlashLightBtn.setVisibility(View.GONE);
        finish(mCollapsibleView.getState() == CollapsibleView.State.UNFOLDED);
    }

    private BroadcastReceiver mScreenStateBroadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                bScreenOffed = true;
            }
        }
    };
}
