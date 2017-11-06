package com.vitaviva.qrcodescanner;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;

public class DialogUtil {
    public static final Dialog createCameraAlertDialog(final Context context, int titleId, int messageId, int posiBtntextId){
        CustomDialog r = new CustomDialog(context);
        r.setTitle(titleId);
        r.setMessage(messageId);
        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        dialog.cancel();
                        break;
                }
            }
        };
        r.setPositiveButton(posiBtntextId, onClickListener);
        r.setCancelable(false);
        return r;
    }
}
