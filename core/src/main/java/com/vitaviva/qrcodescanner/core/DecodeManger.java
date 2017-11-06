package com.vitaviva.qrcodescanner.core;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.util.EnumMap;
import java.util.Map;
import java.util.Vector;

public class DecodeManger {
    private static DecodeManger mDecodeManager = null;
    private Handler mDecodeHandler = null;
    private DecodeResultListener mDecodeResultListener = null;

    //给handler使用的消息what
    private static final int MSG_DECODE_FILE = 2;
    private static final int MSG_DECODE_IMAGE = 4;
    private static final int MSG_SET_DECODE_TYPE = 10;

    //扫描结果，是扫描的文件，还是摄像头拍摄的扫描结果
    public static final int TYPE_FILE = MSG_DECODE_FILE;
    public static final int TYPE_CAMERA = MSG_DECODE_IMAGE;

    public interface DecodeResultListener {
        void onResult(int type, Result result);
    }

    public static DecodeManger getInstance(Context context) {
        if (mDecodeManager == null) {
            synchronized (DecodeManger.class) {
//                BarcodeDecodeLoader.init(context.getApplicationContext(), DecodeManger.class.getClassLoader());
                mDecodeManager = new DecodeManger(context.getApplicationContext());
            }
        }
        return mDecodeManager;
    }

    private DecodeManger(Context context) {
        HandlerThread decodeThread = new HandlerThread("decodeThread");
        decodeThread.start();
        mDecodeHandler = new DecodeHandler(decodeThread.getLooper(), context);
    }

    public void destroy() {
        if (mDecodeHandler != null) {
            mDecodeHandler.removeMessages(MSG_DECODE_IMAGE);
            mDecodeHandler.getLooper().quit();
            mDecodeHandler = null;
        }
        setDecodeResultListener(null);
        mDecodeManager = null;
    }

    public void setDecodeResultListener(DecodeResultListener listener) {
        mDecodeResultListener = listener;
    }

    public void setType(int type) {
        if (null != mDecodeHandler) {
            mDecodeHandler.obtainMessage(MSG_SET_DECODE_TYPE, type, 0).sendToTarget();
        }
    }

    public boolean isDecoding() {
        if (null == mDecodeHandler) {
            return false;
        } else {
            return mDecodeHandler.hasMessages(MSG_DECODE_IMAGE)
                    || mDecodeHandler.hasMessages(MSG_DECODE_FILE);
        }
    }

    public void decode(byte[] data, int width, int height, Rect croppedRect) {
        DecodeMessage msg = new DecodeMessage();
        msg.data = data;
        msg.width = width;
        msg.height = height;
        msg.croppedRect = croppedRect;
        if (null != mDecodeHandler) {
            mDecodeHandler.obtainMessage(MSG_DECODE_IMAGE, msg).sendToTarget();
        }
    }

    public void decode(Uri uri) {
        if (null != mDecodeHandler) {
            mDecodeHandler.obtainMessage(MSG_DECODE_FILE, uri).sendToTarget();
        }
    }


    private class DecodeMessage {
        byte[] data;
        int width;
        int height;
        Rect croppedRect;
    }

    private class DecodeHandler extends Handler {
        private int decodeType = 2;
        private MultiFormatReader multiFormatReader;
        private Context context;

        DecodeHandler(Looper looper, Context context) {
            super(looper);
            this.context = context;
            Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);

            Vector<BarcodeFormat> decodeFormats = new Vector<BarcodeFormat>();
            decodeFormats.addAll(DecodeFormatManager.ONE_D_FORMATS);
            decodeFormats.addAll(DecodeFormatManager.QR_CODE_FORMATS);
            decodeFormats.addAll(DecodeFormatManager.DATA_MATRIX_FORMATS);
            hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
            hints.put(DecodeHintType.CHARACTER_SET, "ISO8859_1");

            multiFormatReader = new MultiFormatReader();
            multiFormatReader.setHints(hints);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SET_DECODE_TYPE: {
//                    mDecoder.getDecoder().setType(msg.arg1);
//                    decodeType = msg.arg1;
//                    break;
                }
                case MSG_DECODE_IMAGE: {
                    DecodeMessage decodeMessage = (DecodeMessage) msg.obj;
                    if (decodeMessage == null) return;
                    Result result = decode(decodeMessage.data,
                            decodeMessage.width,
                            decodeMessage.height,
                            decodeMessage.croppedRect, true);
                    if (result != null) {
                        removeMessages(MSG_DECODE_IMAGE);
                    }
                    if (mDecodeResultListener != null) {
                        mDecodeResultListener.onResult(TYPE_CAMERA, result);
                    }
                    break;
                }
                case MSG_DECODE_FILE: {
                    Bitmap bitmap = Util.decodeUri(context, (Uri) msg.obj, 500, 500);
                    BitmapLuminanceSource source = new BitmapLuminanceSource(bitmap);
                    Result result = decode(source);
                    if (result != null) {
                        removeMessages(MSG_DECODE_FILE);
                    }
                    if (mDecodeResultListener != null) {
                        mDecodeResultListener.onResult(TYPE_FILE, result);
                    }
                    break;
                }
            }
        }


        public byte[] getYUV420sp(int inputWidth, int inputHeight,
                                  Bitmap scaled) {
            int[] argb = new int[inputWidth * inputHeight];

            scaled.getPixels(argb, 0, inputWidth, 0, 0, inputWidth, inputHeight);

            byte[] yuv = new byte[inputWidth * inputHeight * 3 / 2];

            encodeYUV420SP(yuv, argb, inputWidth, inputHeight);

            scaled.recycle();

            return yuv;
        }

        private void encodeYUV420SP(byte[] yuv420sp, int[] argb, int width,
                                    int height) {
            // 帧图片的像素大小
            final int frameSize = width * height;
            // ---YUV数据---
            int Y, U, V;
            // Y的index从0开始
            int yIndex = 0;
            // UV的index从frameSize开始
            int uvIndex = frameSize;

            // ---颜色数据---
//      int a, R, G, B;
            int R, G, B;
            //
            int argbIndex = 0;
            //

            // ---循环所有像素点，RGB转YUV---
            for (int j = 0; j < height; j++) {
                for (int i = 0; i < width; i++) {

                    // a is not used obviously
//              a = (argb[argbIndex] & 0xff000000) >> 24;
                    R = (argb[argbIndex] & 0xff0000) >> 16;
                    G = (argb[argbIndex] & 0xff00) >> 8;
                    B = (argb[argbIndex] & 0xff);
                    //
                    argbIndex++;

                    // well known RGB to YUV algorithm
                    Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
                    U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
                    V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;

                    //
                    Y = Math.max(0, Math.min(Y, 255));
                    U = Math.max(0, Math.min(U, 255));
                    V = Math.max(0, Math.min(V, 255));

                    // NV21 has a plane of Y and interleaved planes of VU each
                    // sampled by a factor of 2
                    // meaning for every 4 Y pixels there are 1 V and 1 U. Note the
                    // sampling is every other
                    // pixel AND every other scanline.
                    // ---Y---
                    yuv420sp[yIndex++] = (byte) Y;
                    // ---UV---
                    if ((j % 2 == 0) && (i % 2 == 0)) {
                        //
                        yuv420sp[uvIndex++] = (byte) V;
                        //
                        yuv420sp[uvIndex++] = (byte) U;
                    }
                }
            }
        }

        private Result decode(LuminanceSource source) {
            Result rawResult = null;
            if (source != null) {
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                try {
                    rawResult = multiFormatReader.decodeWithState(bitmap);
                } catch (ReaderException re) {
                    //continue
                } finally {
                    multiFormatReader.reset();
                }
            }
            return rawResult;
        }


        private Result decode(byte[] data, int width, int height, Rect rect, boolean rotate) {
            PlanarYUVLuminanceSource source;
            if (rotate) {//zxing竖屏下无法识别一维码，对数据旋转
                byte[] rotatedData = new byte[data.length];
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++)
                        rotatedData[x * height + height - y - 1] = data[x + y * width];
                }
                int tmp = width;
                width = height;
                height = tmp;
                data = rotatedData;
                Rect rotedRect = new Rect(width - rect.bottom, rect.left, width - rect.top, rect.right);
                source = buildLuminanceSource(data, width, height, rotedRect);
            } else {
                source = buildLuminanceSource(data, width, height, rect);
            }

            return decode(source);
        }

        /**
         * A factory method to build the appropriate LuminanceSource object based on the format
         * of the preview buffers, as described by Camera.Parameters.
         *
         * @param data   A preview frame.
         * @param width  The width of the image.
         * @param height The height of the image.
         * @return A PlanarYUVLuminanceSource instance.
         */
        private PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height, Rect rect) {
            if (rect == null) {
                return null;
            }
            // Go ahead and assume it's YUV rather than die.
            return new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top,
                    rect.width(), rect.height(), false);
        }

    }
}
