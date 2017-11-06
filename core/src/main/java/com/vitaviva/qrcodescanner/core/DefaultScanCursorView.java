
package com.vitaviva.qrcodescanner.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;

public class DefaultScanCursorView extends View implements IScanCursorView {

    private static final int DEFAULT_FRAME_PADDING_DP = 3; // 绿色边框与高亮区域的间距

    private static final int DEFAULT_SCAN_ANIMATION_DUARION = 4000 * 2;
    private static final int DEFAULT_MODE_ANIMATION_DURATION = 100;
    private static final int RECT_MIN_WIDTH = 300;
    private int mFramePadding; // 绿色边框与高亮区域的间距

    private Bitmap cursorBmp;

    private Paint mPaint;

    private Paint mTextPaint;
    private String scanHint;

    private Rect mFrameRect = new Rect();
    private Rect mCursorRect = new Rect();

    private Rect mOldRect = new Rect();
    private Rect mNewRect = new Rect();
    private Rect mDefaultRect;

    private Bitmap mCorner1;
    private Bitmap mCorner2;
    private Bitmap mCorner3;
    private Bitmap mCorner4;

    private final static float SQUARE_SCAN_SCALE = 0.7f;

    private float scale;
    private ScanAnimation scanAnimation = null;

    private boolean isAnimating = false;
    private Animation moveCropAnimation = new Animation() {
        {
            setAnimationListener(new AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    isAnimating = true;
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    mOldRect.set(mNewRect);
                    isAnimating = false;
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);

            int leftGap = mNewRect.left - mOldRect.left;
            int topGap = mNewRect.top - mOldRect.top;
            int rightGap = mNewRect.right - mOldRect.right;
            int bottomGap = mNewRect.bottom - mOldRect.bottom;

            mFrameRect.set(
                    mOldRect.left + (int) (leftGap * interpolatedTime),//
                    mOldRect.top + (int) (topGap * interpolatedTime),//
                    mOldRect.right + (int) (rightGap * interpolatedTime),//
                    mOldRect.bottom + (int) (bottomGap * interpolatedTime));

            invalidate();
        }
    };

    private class ScanAnimation extends Animation {
        private long mElapsedAtPause = 0;
        private boolean mPaused = false;

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);

            float newTime = 0;
            if (interpolatedTime <= 0.5) {
                newTime = interpolatedTime * 2;
            } else {
                newTime = -2 * interpolatedTime + 2;
            }

            int left = mFrameRect.left;
            int top = mFrameRect.top
                    + (int) ((mFrameRect.height() - cursorBmp.getHeight()) * (newTime));
            int right = mFrameRect.right;
            int bottom = top + cursorBmp.getHeight();
            mCursorRect.set(left, top, right, bottom);

            invalidate();
        }

        @Override
        public boolean getTransformation(long currentTime, Transformation outTransformation) {
            if (mPaused && mElapsedAtPause == 0) {
                mElapsedAtPause = currentTime - getStartTime();
            }
            if (mPaused)
                setStartTime(currentTime - mElapsedAtPause);
            return super.getTransformation(currentTime, outTransformation);
        }

        public void pause() {
            mElapsedAtPause = 0;
            mPaused = true;
        }

        public void resume() {
            mPaused = false;
        }
    }

    private Rect getFrameRect() {
        return mFrameRect;
    }

    public DefaultScanCursorView(Context context) {
        super(context);
        init();
    }

    public DefaultScanCursorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DefaultScanCursorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_MOVE:
                if (event.getPointerCount() >= 2) {
                    float minX = Math.min(event.getX(0), event.getX(1));
                    float minY = Math.min(event.getY(0), event.getY(1));
                    float maxX = Math.max(event.getX(0), event.getX(1));
                    float maxY = Math.max(event.getY(0), event.getY(1));
                    mNewRect.set((int) minX, (int) minY,
                            (int) (minX + Math.max(maxX - minX, RECT_MIN_WIDTH)),
                            (int) (minY + Math.max(maxY - minY, RECT_MIN_WIDTH)));
                    updateCropArea();
                }
                break;
        }
        return true;
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {

            if (mDefaultRect == null) {
                mDefaultRect = new Rect();
                int width = right - left;
                int height = bottom - top;
                int minLength = width <= height ? width : height;

                int squareWidht = (int) (SQUARE_SCAN_SCALE * minLength);
                mDefaultRect.set((width - squareWidht) / 2, (height - squareWidht) / 2,
                        (width - squareWidht) / 2 + squareWidht,
                        (height - squareWidht) / 2 + squareWidht);
                mFrameRect = new Rect(mDefaultRect);
            }
        }
    }

    private void init() {
        mPaint = new Paint();
        mPaint.setColor(0x80000000);
        mTextPaint = new Paint();
        mTextPaint.setColor(0xB2FFFFFF);
        mTextPaint.setTextSize(getResources().getDimension(R.dimen.scan_hint_textsize));
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        scanHint = getResources().getString(R.string.align_qrcode);
        scale = getResources().getDisplayMetrics().density;
        mFramePadding = (int) (DEFAULT_FRAME_PADDING_DP * scale + 0.5f);

        mCorner1 = BitmapFactory.decodeResource(getContext().getResources(),
                R.drawable.corner_left_top);
        mCorner2 = BitmapFactory.decodeResource(getContext().getResources(),
                R.drawable.corner_right_top);
        mCorner3 = BitmapFactory.decodeResource(getContext().getResources(),
                R.drawable.corner_left_bottom);
        mCorner4 = BitmapFactory.decodeResource(getContext().getResources(),
                R.drawable.corner_right_bottom);

        cursorBmp = BitmapFactory.decodeResource(getContext().getResources(),
                R.drawable.barcode_scan_cursor);
        //screenScale = BrowserApplicationContext.INSTANCE.getWindowAspectRatio() > THRESHOLD_SCALE ; 
    }

    public void setCornersRes(int topLeftCornerRes, int rightTopCornerRes, int leftBottomCornerRes, int rightBottomRes) {
        mCorner1 = BitmapFactory.decodeResource(getContext().getResources(),
                topLeftCornerRes);
        mCorner2 = BitmapFactory.decodeResource(getContext().getResources(),
                R.drawable.corner_right_top);
        mCorner3 = BitmapFactory.decodeResource(getContext().getResources(),
                R.drawable.corner_left_bottom);
        mCorner4 = BitmapFactory.decodeResource(getContext().getResources(),
                R.drawable.corner_right_bottom);
    }

    public void setCursorRes(int cursorRes) {
        cursorBmp = BitmapFactory.decodeResource(getContext().getResources(), cursorRes);
    }

    protected void onDraw(Canvas canvas) {
        Rect frameRect = getFrameRect();
        // int location[] = new int[2];
        // getLocationInWindow(location);
        // BLog.d("DefaultScanCursorView", (frameRect.top ) + " " +
        // mFramePadding);
        canvas.drawRect(0, 0, getWidth(), frameRect.top + mFramePadding, mPaint);
        canvas.drawRect(0, frameRect.top + mFramePadding, frameRect.left
                        + mFramePadding, frameRect.bottom - mFramePadding,
                mPaint);
        canvas.drawRect(frameRect.right - mFramePadding, frameRect.top
                        + mFramePadding, getWidth(),
                frameRect.bottom - mFramePadding, mPaint);
        canvas.drawRect(0, frameRect.bottom - mFramePadding, getWidth(), getHeight(),
                mPaint);
        drawCorners(canvas, frameRect);
        drawCursor(canvas, frameRect);
        drawText(canvas, frameRect);
    }

    protected void drawCorners(Canvas canvas, Rect rect) {

        canvas.drawBitmap(mCorner1, rect.left, rect.top, null);
        canvas.drawBitmap(mCorner2, rect.right - mCorner2.getWidth(), rect.top, null);
        canvas.drawBitmap(mCorner3, rect.left, rect.bottom - mCorner3.getHeight(), null);
        canvas.drawBitmap(mCorner4, rect.right - mCorner4.getWidth(),
                rect.bottom - mCorner4.getHeight(), null);

    }

    protected void drawCursor(Canvas canvas, Rect rect) {
        canvas.drawBitmap(cursorBmp, null, mCursorRect, null);
    }

    protected void drawText(Canvas canvas, Rect rect) {
        canvas.drawText(scanHint, getWidth() / 2, (float) (rect.bottom + mTextPaint.getTextSize() * 1.5), mTextPaint);
    }

    public void startScanning() {
        if (scanAnimation == null) {
            scanAnimation = new ScanAnimation();
            scanAnimation.setRepeatCount(Animation.INFINITE);
            scanAnimation.setInterpolator(new LinearInterpolator());
            scanAnimation.setDuration(DEFAULT_SCAN_ANIMATION_DUARION);
            View v = (View) getParent();
            v.startAnimation(scanAnimation);
        } else {
            scanAnimation.resume();
        }
    }

    public void stopScanning() {
        if (scanAnimation != null) {
            scanAnimation.pause();
        }
    }

    @Override
    public Rect getCropAreaInWindow() {
        int location[] = new int[2];
        getLocationInWindow(location);

        Rect frameRect = getFrameRect();
        return new Rect(location[0] + frameRect.left, location[1] + frameRect.top, location[0]
                + frameRect.right, location[1] + frameRect.bottom);
    }

    public void updateCropArea() {
        if (isAnimating) return;
        moveCropAnimation.setDuration(DEFAULT_MODE_ANIMATION_DURATION);
        startAnimation(moveCropAnimation);
    }

    public void release() {
        if (mCorner1 != null) {
            mCorner1.recycle();
            mCorner1 = null;
        }
        if (mCorner2 != null) {
            mCorner2.recycle();
            mCorner2 = null;
        }
        if (mCorner3 != null) {
            mCorner3.recycle();
            mCorner3 = null;
        }
        if (mCorner4 != null) {
            mCorner4.recycle();
            mCorner4 = null;
        }
        if (cursorBmp != null) {
            cursorBmp.recycle();
            cursorBmp = null;
        }
    }
}
