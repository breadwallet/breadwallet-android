package com.breadwallet.presenter.customviews;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;

import com.breadwallet.R;
import com.breadwallet.tools.manager.TypefacesManager;
import com.breadwallet.tools.util.Utils;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 5/3/17.
 * Copyright (c) 2017 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
@SuppressLint("AppCompatCustomView") // we don't need to support older versions
public class BRButton extends Button {
    private static final String TAG = BRButton.class.getName();
    private final int ANIMATION_DURATION = 200;
    private int currentX = 0;
    private int currentY = 0;
    private boolean isBreadButton; //meaning is has the special animation and shadow

    public BRButton(Context context) {
        super(context);
    }

    public BRButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public BRButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public BRButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context ctx, AttributeSet attrs) {
        TypedArray a = ctx.obtainStyledAttributes(attrs, R.styleable.BRText);
        String customFont = a.getString(R.styleable.BRText_customFont);
        TypefacesManager.setCustomFont(ctx, this, Utils.isNullOrEmpty(customFont) ? "CircularPro-Medium.otf" : customFont);
        a.recycle();
        float px16 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        setPadding(0, 0, 0, (int) px16);
        setElevation(0);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isBreadButton) {

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }

                ScaleAnimation scaleAnim = new ScaleAnimation(
                        1f, 0.8f,
                        1f, 0.8f,
                        Animation.RELATIVE_TO_SELF, 1,
                        Animation.RELATIVE_TO_SELF, 1);
                scaleAnim.setDuration(ANIMATION_DURATION);
                scaleAnim.setRepeatCount(0);
                scaleAnim.setInterpolator(new AccelerateDecelerateInterpolator());
                scaleAnim.setFillAfter(true);
                scaleAnim.setFillBefore(true);
                scaleAnim.setFillEnabled(true);

                startAnimation(scaleAnim);
//        else if (event.getAction() == MotionEvent.ACTION_MOVE) {
//            if (clicked_on_image) {
//                //do stuff, drag the image or whatever
//            }
//        }
                return super.onTouchEvent(event);
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
//            clicked_on_image = false;
            }
            return true;
        } else {
            return super.onTouchEvent(event);
        }

    }

}
