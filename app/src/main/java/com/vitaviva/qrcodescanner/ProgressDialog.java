
package com.vitaviva.qrcodescanner;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ProgressDialog extends CustomDialog implements View.OnClickListener {
    private ProgressView updateImg;
    private TextView updateMsg;
    private ImageView updateClose;
    private RelativeLayout progressView;
    private LinearLayout layoutContent;

    public ProgressDialog(Context context) {
        super(context);

        addContentView(R.layout.dialog_update);
        initView();
        setAllWidgetGone();
    }

    private void initView() {
        setCanceledOnTouchOutside(false);
        updateImg = (ProgressView) findViewById(R.id.update_img);
        updateMsg = (TextView) findViewById(R.id.update_msg);
        updateClose = (ImageView) findViewById(R.id.update_close);
        updateClose.setOnClickListener(this);
        progressView = (RelativeLayout) findViewById(R.id.progress_view);
        layoutContent = (LinearLayout) findViewById(R.id.layout_progress_content);
    }

    public void setImageResource(int resId) {
        updateImg.setImageResource(resId);
    }

    public void setShowStyle(int level) {
        setTextSize(14);
        mStyleLevel = level;

        if (mRootView != null && mRootView.getLayoutParams() != null) {
            int layoutWidth = mRootView.getLayoutParams().width;
            mRootView.getLayoutParams().width = (int) (layoutWidth * 0.9);
        }

        if (layoutContent.getLayoutParams() != null) {
            layoutContent.getLayoutParams().height = DensityUtils.dip2px(getContext(), 148);
        }
    }

    public void setUpdateMsg(String msg) {
        updateMsg.setText(msg);
    }

    public void setUpdateMsg(int msgId) {
        updateMsg.setText(msgId);
    }

    public void setTextSize(int size) {
        updateMsg.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
    }

    public void setShowCloseButton(boolean isShow) {
        updateClose.setVisibility(isShow ? View.VISIBLE : View.GONE);
    }

    public void postDelayed(Runnable runnable, long delayMillis) {
        updateImg.postDelayed(runnable, delayMillis);
    }

    public void clearAnimation() {
        updateImg.clearAnimation();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.update_close) {
            this.dismiss();
        }
    }
}
