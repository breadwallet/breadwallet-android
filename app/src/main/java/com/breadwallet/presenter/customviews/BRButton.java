package com.breadwallet.presenter.customviews;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;

import com.breadwallet.R;
import com.breadwallet.tools.animation.BRAnimator;
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
    private static int ANIMATION_DURATION = 30;
    private Bitmap shadow;
    private Rect shadowRect;
    private RectF bRect;
    private int width;
    private int height;
    private int modifiedWidth;
    private int modifiedHeight;
    private Paint bPaint;
    private Paint bPaintStroke;
    private int type = 2;
    private static final float SHADOW_PRESSED = 0.88f;
    private static final float SHADOW_UNPRESSED = 0.95f;
    private float shadowOffSet = SHADOW_UNPRESSED;
    private static final int ROUND_PIXELS = 16;
    private boolean isBreadButton; //meaning is has the special animation and shadow

    public BRButton(Context context) {
        super(context);
        init(context, null);
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
        shadow = BitmapFactory.decodeResource(getResources(), R.drawable.shadow);
        bPaint = new Paint();
        bPaintStroke = new Paint();

        shadowRect = new Rect(0, 0, 100, 100);
        bRect = new RectF(0, 0, 100, 100);
        TypedArray a = ctx.obtainStyledAttributes(attrs, R.styleable.BRText);
        String customFont = a.getString(R.styleable.BRText_customFont);
        TypefacesManager.setCustomFont(ctx, this, Utils.isNullOrEmpty(customFont) ? "CircularPro-Medium.otf" : customFont);
        float px16 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15, getResources().getDisplayMetrics());
        //check attributes you need, for example all paddings
        int[] attributes = new int[]{android.R.attr.paddingStart, android.R.attr.paddingTop, android.R.attr.paddingEnd, android.R.attr.paddingBottom, R.attr.isBreadButton, R.attr.buttonType};
        //then obtain typed array
        TypedArray arr = ctx.obtainStyledAttributes(attrs, attributes);
        //You can check if attribute exists (in this example checking paddingRight)

        isBreadButton = arr.getBoolean(4, false);

        int paddingLeft = arr.hasValue(0) ? arr.getDimensionPixelOffset(0, -1) : (int) px16;
        int paddingTop = arr.hasValue(1) ? arr.getDimensionPixelOffset(1, -1) : 0;
        int paddingRight = arr.hasValue(2) ? arr.getDimensionPixelOffset(2, -1) : (int) px16;
        int paddingBottom = arr.hasValue(3) ? arr.getDimensionPixelOffset(3, -1) + (isBreadButton ? (int) px16 : 0) : (isBreadButton ? (int) px16 : 0);

        setType(arr.getInteger(5, 0));

        bPaint.setAntiAlias(true);
        bPaintStroke.setAntiAlias(true);

        if (isBreadButton) {
            setBackground(getResources().getDrawable(R.drawable.shadow_trans));
        }

        setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
        a.recycle();
        arr.recycle();
        final ViewTreeObserver observer = getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (observer.isAlive()) {
                    observer.removeOnGlobalLayoutListener(this);
                }
                correctTextSizeIfNeeded();
                correctTextBalance();
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isBreadButton) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                if (type != 3)
                    press(ANIMATION_DURATION);
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                unPress(ANIMATION_DURATION);
            }
        }

        return super.onTouchEvent(event);

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;

    }

    private void correctTextSizeIfNeeded() {
        int limit = 100;
        int lines = getLineCount();
        float px = getTextSize();
        while (lines > 1 && !getText().toString().contains("\n")) {
            limit--;
//            Log.e(TAG, "correctTextSizeIfNeeded: (" + getText().toString() + ")resizing by 2..: " + px);
            px -= 1;
            setTextSize(TypedValue.COMPLEX_UNIT_PX, px);
            lines = getLineCount();
            if (limit <= 0) {
                Log.e(TAG, "correctTextSizeIfNeeded: Failed to rescale, limit reached, final: " + px);
                break;
            }
        }
    }

    private void correctTextBalance() {
//        Rect bounds = new Rect();
//        Paint textPaint = getPaint();
//        textPaint.getTextBounds(getText().toString(), 0, getText().toString().length(), bounds);
//        int height = bounds.height();
//        int width = bounds.width();

//        int paddingLeft = getPaddingLeft();
//        int paddingTop = getPaddingTop();
//        int paddingRight = getPaddingRight();
//        int paddingBottom = getPaddingBottom();
//
//        paddingTop = 5;
//        paddingBottom = height - 5 - modifiedHeight;
//
//        setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
//        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isBreadButton) {
            shadowRect.set(5, height / 4, width - 5, (int) (height * shadowOffSet));
            modifiedWidth = width - 10;
            modifiedHeight = height - height / 4 - 5;
            bRect.set(5, 5, modifiedWidth, modifiedHeight + 5);
            canvas.drawBitmap(shadow, null, shadowRect, null);
            canvas.drawRoundRect(bRect, ROUND_PIXELS, ROUND_PIXELS, bPaint);
            if (type == 2 || type == 3)
                canvas.drawRoundRect(bRect, ROUND_PIXELS, ROUND_PIXELS, bPaintStroke);
        }
        super.onDraw(canvas);

    }

    public void setType(int type) {
        if (type == 3) press(1);
        this.type = type;

        if (type == 1) { //blue
            bPaint.setColor(getContext().getColor(R.color.button_primary_normal));
            setTextColor(getContext().getColor(R.color.white));
        } else if (type == 2) { //gray stroke
            bPaintStroke.setColor(getContext().getColor(R.color.extra_light_gray));
            bPaintStroke.setStyle(Paint.Style.STROKE);
            bPaintStroke.setStrokeWidth(Utils.getPixelsFromDps(getContext(), 1));
            setTextColor(getContext().getColor(R.color.light_gray));
            bPaint.setColor(getContext().getColor(R.color.button_secondary));
            bPaint.setStyle(Paint.Style.FILL);
        } else if (type == 3) { //blue strokeww
            bPaintStroke.setColor(getContext().getColor(R.color.button_primary_normal));
            bPaintStroke.setStyle(Paint.Style.STROKE);
            bPaintStroke.setStrokeWidth(Utils.getPixelsFromDps(getContext(), 1));
            setTextColor(getContext().getColor(R.color.button_primary_normal));
            bPaint.setColor(getContext().getColor(R.color.button_secondary));
            bPaint.setStyle(Paint.Style.FILL);
        }
        invalidate();
    }

    private void press(int duration) {
        ScaleAnimation scaleAnim = new ScaleAnimation(
                1f, 0.96f,
                1f, 0.96f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 1f);
        scaleAnim.setDuration(duration);
        scaleAnim.setRepeatCount(0);
        scaleAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleAnim.setFillAfter(true);
        scaleAnim.setFillBefore(true);
        scaleAnim.setFillEnabled(true);

        ValueAnimator shadowAnim = ValueAnimator.ofFloat(SHADOW_UNPRESSED, SHADOW_PRESSED);
        shadowAnim.setDuration(duration);
        shadowAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                shadowOffSet = (float) animation.getAnimatedValue();
                invalidate();
            }
        });

        startAnimation(scaleAnim);
        shadowAnim.start();

    }

    private void unPress(int duration) {
        ScaleAnimation scaleAnim = new ScaleAnimation(
                0.96f, 1f,
                0.96f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 1f);
        scaleAnim.setDuration(duration);
        scaleAnim.setRepeatCount(0);
        scaleAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleAnim.setFillAfter(true);
        scaleAnim.setFillBefore(true);
        scaleAnim.setFillEnabled(true);

        ValueAnimator shadowAnim = ValueAnimator.ofFloat(SHADOW_PRESSED, SHADOW_UNPRESSED);
        shadowAnim.setDuration(duration);
        shadowAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                shadowOffSet = (float) animation.getAnimatedValue();
                invalidate();
            }
        });

        startAnimation(scaleAnim);
        shadowAnim.start();
    }
}
