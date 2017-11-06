
package com.vitaviva.qrcodescanner.core;


import android.graphics.Rect;

public interface IScanCursorView {

    Rect getCropAreaInWindow();

    void startScanning();

    void stopScanning();

}
