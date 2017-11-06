package com.vitaviva.qrcodescanner.core;

import android.graphics.Bitmap;

import com.google.zxing.LuminanceSource;

public class BitmapLuminanceSource extends LuminanceSource {
    byte[] bitmapPixels;

    public BitmapLuminanceSource(Bitmap bitmap) {
        super(bitmap.getWidth(), bitmap.getHeight());

        // 首先，要取得该图片的像素数组内容
        int[] data = new int[bitmap.getWidth() * bitmap.getHeight()];
        this.bitmapPixels = new byte[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(data, 0, getWidth(), 0, 0, getWidth(), getHeight());

        // 将int数组转换为byte数组
        for (int i = 0; i < data.length; i++) {
            this.bitmapPixels[i] = (byte) data[i];
        }
    }

    @Override
    public byte[] getRow(int i, byte[] bytes) {
        // 这里要得到指定行的像素数据
        System.arraycopy(bitmapPixels, i * getWidth(), bytes, 0, getWidth());
        return bytes;
    }

    @Override
    public byte[] getMatrix() {
        // 返回我们生成好的像素数据
        return bitmapPixels;
    }
}
