
package com.vitaviva.qrcodescanner;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.ImageView;

public class ProgressView extends ImageView {
    private int frameCount;
    private int duration;

    public ProgressView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setAnimation(attrs);
    }

    public ProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setAnimation(attrs);
        startAnimation(frameCount, duration);
    }

    public ProgressView(Context context) {
        super(context);
    }

    private void setAnimation(AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ProgressView);
        frameCount = a.getInt(R.styleable.ProgressView_frameCount, 12);
        duration = a.getInt(R.styleable.ProgressView_duration, 1000);
        a.recycle();
    }

    public void startAnimation(final int frameCount, final int duration) {
        Animation a = AnimationUtils.loadAnimation(getContext(), R.anim.progress_anim);
        a.setDuration(duration);
        a.setInterpolator(new Interpolator() {
            @Override
            public float getInterpolation(float input) {
//                return (float) Math.floor(input * frameCount) / frameCount;
                return input;
            }
        });
        startAnimation(a);
    }
}
