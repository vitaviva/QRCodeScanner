
package com.vitaviva.qrcodescanner;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.Html;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.HashSet;

public class CustomDialog extends Dialog {
    private float mDensity;
    public int mStyleLevel;
    private boolean isDimSetting;

    protected View mRootView;
    private TextView mTitle;
    private ImageView mTitleIcon;
    protected FrameLayout mCustom;
    private Bitmap mDrawingCacheBit;
    private View mContentView;// 从子类中传过来的contentView

    private TextView mPositiveButton;
    private OnClickListener mPositiveButtonListener, mNegativeButtonListener,
            mNeutralButtonListener;
    private TextView mNegativeButton;
    private TextView mNeutralButton;

    private TextView mMessage;
    private Context mContext;
    private View mDelimiterline;
    private View mSeperator2, mSeperator3;
    private String mTag;
    private static HashSet<String> mDialogTags = new HashSet<String>();
    private static CustomDialog mTopDialog = null;
    private View header;
    private View titleLine;

    public static final int STYLE_SMALL_LEVEL = 0X1;

    // 默认的对话框按钮，只用于关闭当前对话框
    private OnClickListener mDefaultButtonListener = new OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
        }
    };

    public CustomDialog(Context context) {
        super(context);
        mContext = context;
        init();
    }

    public CustomDialog(Context context, int theme) {
        super(context, theme);
        mContext = context;
        init();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_HOME) {
            dismiss();
        }

        return super.onKeyUp(keyCode, event);
    }

    public void setAllWidgetGone() {
        showDilemiterLineAboveButton(false);
        findViewById(R.id.buttonPanel).setVisibility(View.GONE);
        hideHeder();
    }

    private final void init() {
        getContext().setTheme(R.style.dialog);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        super.setContentView(R.layout.custom_dialog);

        mRootView = findViewById(R.id.root);
        DisplayMetrics metric = new DisplayMetrics();

        try {
            ((Activity) mContext).getWindowManager().getDefaultDisplay().getMetrics(metric);
        } catch (Exception e) {
        }

        int width = Math.min(metric.widthPixels, metric.heightPixels);
        mDensity = metric.density == 0 ? 1.5f : metric.density;
        width -= 30 * mDensity;
        mRootView.getLayoutParams().width = width;

        mTitle = (TextView) this.findViewById(R.id.title);
        mTitleIcon = (ImageView) findViewById(R.id.icon);
        mCustom = (FrameLayout) this.findViewById(R.id.custom);
        mPositiveButton = (TextView) this.findViewById(R.id.button1);
        mPositiveButton.setOnClickListener(mButtonOnClickListener);
        mPositiveButton.setVisibility(View.GONE);
        mNegativeButton = (TextView) this.findViewById(R.id.button2);
        mNegativeButton.setOnClickListener(mButtonOnClickListener);
        mNegativeButton.setVisibility(View.GONE);
        mNeutralButton = (TextView) this.findViewById(R.id.button3);
        mNeutralButton.setOnClickListener(mButtonOnClickListener);
        mNeutralButton.setVisibility(View.GONE);
        mSeperator2 = this.findViewById(R.id.btn_seperator2);
        mSeperator3 = this.findViewById(R.id.btn_seperator3);
        mDelimiterline = this.findViewById(R.id.delimiter_line_above_button);
        header = findViewById(R.id.title_header);
        titleLine = findViewById(R.id.title_line);
        setCanceledOnTouchOutside(true);
    }

    public void changeButtonPostion() {
        ViewGroup parent = (ViewGroup) findViewById(R.id.buttonPanel);
        parent.removeView(mPositiveButton);
        parent.removeView(mNegativeButton);
        parent.addView(mNegativeButton, 0);
        parent.addView(mPositiveButton);
    }

    public void setTitleIcon(int iconId) {
        mTitleIcon.setImageResource(iconId);
        mTitleIcon.setVisibility(View.VISIBLE);
    }

    // Title
    public void setTitle(int resid) {
        mTitle.setText(resid);
    }

    public void setTitle(CharSequence text) {
        mTitle.setText(text);
    }

    public void setTitleSize(float size) {
        mTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
    }

    public void setMsgSize(float size) {
        if (mMessage != null) {
            mMessage.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        }
    }

    public void setDimSetting(boolean value) {
        this.isDimSetting = value;
    }

    public void setCloseBtnVisibility(int state) {
        findViewById(R.id.close).setVisibility(state);
        if (View.VISIBLE == state) {
            findViewById(R.id.close).setOnClickListener(
                    new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            ((InputMethodManager) v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE))
                                    .hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);

                            dismiss();
                        }
                    });
        }
    }

    public void hideHeder() {
        header.setVisibility(View.GONE);
        titleLine.setVisibility(View.GONE);
    }

    /**
     * Handy method if you just want to display a message as the content of the
     * dialog
     *
     * @param resid      String resource id with parameters
     * @param formatArgs parameter values
     */
    public void setMessage(int resid, Object... formatArgs) {
        String msg = this.getContext().getResources()
                .getString(resid, formatArgs);
        setMessage(msg);
    }

    /**
     * Handy method if you just want to display a message as the content of the
     * dialog
     *
     * @param resid String resource id
     */
    public void setMessage(int resid) {
        String msg = this.getContext().getResources().getString(resid);
        setMessage(msg);
    }

    /**
     * Handy method if you just want to display a message as the content of the
     * dialog
     *
     * @param message String to be displayed
     */
    public void setMessage(CharSequence message) {
        if (null == mMessage) {
            ScrollView scrollView = new ScrollView(getContext());
            int height = (int) (mDensity * 130);
            FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, height);
            FrameLayout.LayoutParams lytp = new FrameLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            lytp.leftMargin = lytp.rightMargin = DensityUtils.dip2px(mContext, 22);
            layoutParams.gravity = Gravity.CENTER;
            lytp.gravity = Gravity.CENTER;
            scrollView.setLayoutParams(layoutParams);
            mMessage = new TextView(this.getContext());
            mMessage.setLayoutParams(lytp);
            mMessage.setLineSpacing((float) DensityUtils.dip2px(mContext, 8), 1.0f);
            mMessage.setTextColor(this.getContext().getResources()
                    .getColor(R.color.custom_dialog_content_text));
            mMessage.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            scrollView.addView(mMessage);
            mContentView = scrollView;
            mCustom.addView(scrollView);
        }
        mMessage.setGravity(Gravity.CENTER);
        mMessage.setText(message);

    }

    private View.OnClickListener mButtonOnClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (v.getId() == R.id.button1 && mPositiveButtonListener != null) {
                mPositiveButtonListener.onClick(CustomDialog.this, DialogInterface.BUTTON_POSITIVE);
            }
            if (v.getId() == R.id.button2 && mNegativeButtonListener != null) {
                mNegativeButtonListener.onClick(CustomDialog.this, DialogInterface.BUTTON_NEGATIVE);
            }
            if (v.getId() == R.id.button3 && mNeutralButtonListener != null) {
                mNeutralButtonListener.onClick(CustomDialog.this, DialogInterface.BUTTON_NEUTRAL);
            }
        }
    };

    /**
     * When positive button is clicked, the "which" value passed on to the
     * listener is DialogInterface.BUTTON_POSITIVE
     *
     * @param resid    The string to display on the positive button
     * @param listener
     */
    public void setPositiveButton(int resid, OnClickListener listener) {
        String text = mContext.getResources().getString(resid);
        setPositiveButton(text, -1, listener);
    }

    public void setPositiveButton(int resid, int color, OnClickListener listener) {
        String text = mContext.getResources().getString(resid);
        setPositiveButton(text, color, listener);
    }

    public void setPositiveButton(int resid) {
        String text = mContext.getResources().getString(resid);
        setPositiveButton(text, -1, mDefaultButtonListener);
    }

    public void setPositiveButton(String resid, int color, OnClickListener listener) {
        mPositiveButton.setText(Html.fromHtml(resid));
        mPositiveButton.setVisibility(View.VISIBLE);
        mPositiveButtonListener = listener;

        if (color != -1) {
            mPositiveButton.setTextColor(color);
        }
    }

    public void setPositiveButtonEnable(boolean enable) {
        mPositiveButton.setEnabled(enable);
        if (!enable) {
            mPositiveButton.setTextColor(Color.GRAY);
        } else {
            mPositiveButton.setTextColor(Color.WHITE);
        }
    }

    public void setPositiveButtonText(int resid) {
        String text = mContext.getResources().getString(resid);
        mPositiveButton.setText(Html.fromHtml(text));
        mPositiveButton.setVisibility(View.VISIBLE);
    }

    public void setPositiveButtonTextColor(int color) {
        mPositiveButton.setTextColor(color);
    }

    /**
     * When negative button is clicked, the "which" value passed on to the
     * listener is DialogInterface.BUTTON_NEGATIVE
     *
     * @param resid    The string to display on the negative button
     * @param listener
     */
    public void setNegativeButton(int resid, OnClickListener listener) {
        String text = mContext.getResources().getString(resid);
        setNegativeButton(text, listener);
        mSeperator3.setVisibility(View.VISIBLE);
    }

    public void setNegativeButton(int resid) {
        setNegativeButton(resid, mDefaultButtonListener);
        mSeperator3.setVisibility(View.VISIBLE);
    }

    public void setNeutralButton(int resid) {
        setNeutralButton(resid, mDefaultButtonListener);
    }

    public void setNegativeButton(String resid, OnClickListener listener) {
        mNegativeButton.setText(Html.fromHtml(resid));
        mNegativeButton.setVisibility(View.VISIBLE);
        mNegativeButtonListener = listener;
        mSeperator3.setVisibility(View.VISIBLE);
    }

    public void setNeutralButton(int resid, OnClickListener listener) {
        mNeutralButton.setText(resid);
        mNeutralButton.setVisibility(View.VISIBLE);
        mNeutralButtonListener = listener;
    }

    public void setNeutralButton(int resid, int color, OnClickListener listener) {
        mNeutralButton.setText(resid);
        mNeutralButton.setVisibility(View.VISIBLE);
        mNeutralButton.setTextColor(color);
        mNeutralButtonListener = listener;
        mSeperator2.setVisibility(View.VISIBLE);
    }

    public void addContentView(int resid) {
        addContentView(getLayoutInflater().inflate(resid, null));
    }

    public void addContentView(View view) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        addContentView(view, lp);
    }

    public void addContentView(View view, FrameLayout.LayoutParams lp) {
        mContentView = view;
        mCustom.addView(view, lp);
    }

    public View getContentView() {
        return mContentView;
    }

    /**
     * Set the minmum width or/and height of the dialog, pass -1 to ignore
     * either dimension. If minHeight is set, extra space will be given to
     * content area.
     *
     * @param minWidth
     * @param minHeight
     */
    public final void setMinSize(int minWidth, int minHeight) {
        if (minWidth != -1) {
            mRootView.setMinimumWidth(minWidth);
        }
        if (minHeight != -1) {
            mRootView.setMinimumHeight(minHeight);
        }
    }

    public void showDilemiterLineAboveButton(boolean show) {
        if (mDelimiterline != null)
            mDelimiterline.setVisibility(
                    show ? View.VISIBLE : View.GONE);
    }

    public void showOnce(String tag) {
        mTag = tag;
        if (mDialogTags.contains(mTag)) {
            return;
        }
        try {
            mDialogTags.add(mTag);
            show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isDialogShowingByTag(String tag) {
        return mDialogTags.contains(tag);
    }

    @Override
    public void show() {
        // 防止Activity销毁了，show dialog crash
        if (((Activity) mContext).isFinishing()) {
            return;
        }

        mTopDialog = this;
        super.show();
    }

    public static void hideDialog(String tag) {
        if (mDialogTags.contains(tag) && mTopDialog != null) {
            mTopDialog.dismiss();
            mTopDialog = null;
        }
    }

    @Override
    public void dismiss() {
        if (false == TextUtils.isEmpty(mTag) && mDialogTags.contains(mTag)) {
            mDialogTags.remove(mTag);
        }

        try {
            //youmeng上此处有crash，应该是某些机型，如果dialog不存在，再调用dismiss就会异常
            super.dismiss();
        } catch (Exception e) {

        }

        mTopDialog = null;
    }


    public void hideTitle() {
        header.setVisibility(View.GONE);
        titleLine.setVisibility(View.GONE);
    }

    public void HideNeutralButton() {
        mNeutralButton.setVisibility(View.GONE);
    }

    public void hideNegativeButton() {
        mNegativeButton.setVisibility(View.GONE);
    }

    public void showNegativeButton() {
        mNegativeButton.setVisibility(View.VISIBLE);
    }

    public Bitmap getRootCopy() {
        try {
            mRootView.setDrawingCacheEnabled(true);
            Bitmap src = mRootView.getDrawingCache();
            if (src != null) {
                mDrawingCacheBit = Bitmap.createBitmap(src, 1, 1, src.getWidth() - 2, src.getHeight() - 2);
                return mDrawingCacheBit;
            }
        } catch (Error e) {
            e.printStackTrace();
        }
        return null;
    }

}
