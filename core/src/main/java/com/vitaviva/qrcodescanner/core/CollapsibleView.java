
package com.vitaviva.qrcodescanner.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.Transformation;

public class CollapsibleView extends View {

    private static final int DEFAULT_ANIMATION_DURATION = 500;

    private int cursorPosition = 0;

    private Bitmap aboveBmpOpening;
    private Bitmap aboveBmpClosing;
    private Bitmap aboveBmp;// 上边两个的引用
    private Bitmap belowBmpOpening;
    private Bitmap belowBmpClosing;
    private Bitmap belowBmp;// 上边两个的引用

    private Paint mPaint;

    private OnUnfoldProgressListener mOnUnfoldProgressListener;
    private OnFoldProgressListener mOnFoldProgressListener;

    private UnfoldAnimation mUnfoldAnimation = new UnfoldAnimation();
    private FoldAnimation mFoldAnimation = new FoldAnimation();

    // private int actionBarHeight; //不全部打开，打开到actionbar的部分就停止

    public enum State {
        FOLDED, UNFOLDED, ISFOLDING, ISUNFOLDING
    }

    private State mState = State.FOLDED;

    public void setAboveBitmap(int resourceIdOpen, int resourceIdClose) {
        aboveBmpOpening = BitmapFactory.decodeResource(getContext().getResources(), resourceIdOpen);
        aboveBmpClosing = BitmapFactory.decodeResource(getContext().getResources(),
                resourceIdClose);
    }

    public void setBelowBitmap(int resourceIdOpen, int resourceIdClose) {
        belowBmpOpening = BitmapFactory.decodeResource(getContext().getResources(), resourceIdOpen);
        belowBmpClosing = BitmapFactory.decodeResource(getContext().getResources(),
                resourceIdClose);
        // if (State.UNFOLDED == mState) {
        // belowBmp = belowBmpCloseing;
        // } else if (State.FOLDED == mState) {
        // belowBmp = belowBmpOpening;
        // }
    }

    private class UnfoldAnimation extends Animation implements AnimationListener {

        public UnfoldAnimation() {
            super();
            setDuration(DEFAULT_ANIMATION_DURATION);
            setAnimationListener(this);
        }

        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);
            // 这里getHeight() / 2 + 1的 + 1为了弥补可能上下留边的问题
            cursorPosition = (int) ((getHeight() / 2 + 1) * interpolatedTime);
            invalidate();
        }

        @Override
        public void onAnimationStart(Animation animation) {
            mState = State.ISUNFOLDING;
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            mState = State.UNFOLDED;
            if (mOnUnfoldProgressListener != null) {
                mOnUnfoldProgressListener.onUnfoldFinish();
            }
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    }

    private class FoldAnimation extends Animation implements AnimationListener {

        public FoldAnimation() {
            super();
            setDuration(DEFAULT_ANIMATION_DURATION);
            setAnimationListener(this);
        }

        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);
            int halfHeight = getHeight() / 2;
            cursorPosition = (int) (halfHeight * (1 - interpolatedTime));
            invalidate();
        }

        @Override
        public void onAnimationStart(Animation animation) {
            mState = State.ISFOLDING;
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            mState = State.FOLDED;
            if (mOnFoldProgressListener != null) {
                mOnFoldProgressListener.onFoldFinish();
            }
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    }

    public CollapsibleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private final void init() {
        mPaint = new Paint();
        mPaint.setColor(0xff333435);
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            if (mState == State.FOLDED) {
                cursorPosition = 0;
            } else if (mState == State.UNFOLDED) {
                cursorPosition = getHeight() / 2 + 1;
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        // draw upper part
        canvas.save();
        canvas.translate(0, -cursorPosition);
        canvas.drawRect(0, 0, width, height / 2, mPaint);
        if (aboveBmp == null) {
            aboveBmp = aboveBmpOpening;
        }
        if (aboveBmp != null) {
            int bmpWidth = aboveBmp.getWidth();
            int bmpHeight = aboveBmp.getHeight();
            canvas.drawBitmap(aboveBmp, null, new Rect(width / 2
                    - bmpWidth / 2, height / 2 - bmpHeight, width / 2 + bmpWidth / 2, height / 2),
                    null);
        }
        canvas.restore();
        // draw under part
        canvas.save();
        canvas.translate(0, height / 2 + cursorPosition);
        canvas.drawRect(0, 0, width, height / 2, mPaint);
        if (belowBmp == null) {
            belowBmp = belowBmpOpening;
        }
        if (belowBmp != null) {
            int bmpWidth = belowBmp.getWidth();
            int bmpHeight = belowBmp.getHeight();
            canvas.drawBitmap(belowBmp, null, new Rect(width / 2
                    - bmpWidth / 2, 0, width / 2 + bmpWidth / 2, bmpHeight), null);
        }
        canvas.restore();
    }

    public void unfold() {
        belowBmp = belowBmpOpening;
        aboveBmp = aboveBmpOpening;
        startAnimation(mUnfoldAnimation);
    }

    public void fold() {
        belowBmp = belowBmpClosing;
        aboveBmp = aboveBmpClosing;
        startAnimation(mFoldAnimation);
    }

    public void setOnUnfoldProgressListener(OnUnfoldProgressListener listener) {
        mOnUnfoldProgressListener = listener;
    }

    public interface OnUnfoldProgressListener {
        void onUnfoldFinish();
    }

    public interface OnFoldProgressListener {
        void onFoldFinish();
    }

    public void setOnFoldProgressListener(OnFoldProgressListener listener) {
        mOnFoldProgressListener = listener;
    }

    public State getState() {
        return mState;
    }
    
    public void release(){
        if(aboveBmpOpening != null){
            aboveBmpOpening.recycle();
        }
        if(aboveBmpClosing != null){
            aboveBmpClosing.recycle();
        }
        if(belowBmpOpening != null){
            belowBmpOpening.recycle();
        }
        if(belowBmpClosing != null){
            belowBmpClosing.recycle();
        }
    }

}
