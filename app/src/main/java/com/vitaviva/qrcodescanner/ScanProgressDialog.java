package com.vitaviva.qrcodescanner;

import android.content.Context;

public class ScanProgressDialog extends ProgressDialog{

    public ScanProgressDialog(Context context) {
        super(context);
        setCancelable(false);
    }

}
