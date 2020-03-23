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
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;

import androidx.annotation.ColorRes;
import androidx.annotation.Nullable;

import com.breadwallet.R;
import com.breadwallet.tools.manager.FontManager;
import com.breadwallet.tools.util.Utils;

import timber.log.Timber;

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
    private static final int ANIMATION_DURATION = 30;
    private Bitmap shadow;
    private Rect shadowRect;
    private RectF bRect;
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
        bPaint.setAntiAlias(true);
        bPaintStroke = new Paint();
        bPaintStroke.setAntiAlias(true);
        shadowRect = new Rect(0, 0, 100, 100);
        bRect = new RectF(0, 0, 100, 100);
        TypedArray a = ctx.obtainStyledAttributes(attrs, R.styleable.BRButton);
        String customFont = a.getString(R.styleable.BRButton_customBFont);
        FontManager.setCustomFont(ctx, this, Utils.isNullOrEmpty(customFont) ? "BarlowSemiCondensed-Medium.ttf" : customFont);
        int px16 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15, getResources().getDisplayMetrics());

        isBreadButton = a.getBoolean(R.styleable.BRButton_isBreadButton, false);

        int[] attributes = new int[]{android.R.attr.paddingStart, android.R.attr.paddingTop, android.R.attr.paddingEnd, android.R.attr.paddingBottom, R.attr.isBreadButton, R.attr.buttonType};
        TypedArray arr = ctx.obtainStyledAttributes(attrs, attributes);

        int paddingLeft = arr.getDimensionPixelOffset(0, px16);
        int paddingTop = arr.getDimensionPixelOffset(1, 0);
        int paddingRight = arr.getDimensionPixelOffset(2, px16);
        int paddingBottom = arr.getDimensionPixelOffset(3, 0) + (isBreadButton ? px16 : 0);

        int type = a.getInteger(R.styleable.BRButton_buttonType, 0);
        setType(type);

        if (isBreadButton) {
            setBackground(getContext().getDrawable(R.drawable.shadow_trans));
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
                    onTouch(true, ANIMATION_DURATION);
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                onTouch(false, ANIMATION_DURATION);
            }
        }

        return super.onTouchEvent(event);
    }

    private void correctTextSizeIfNeeded() {
        int limit = 100;
        int lines = getLineCount();
        float px = getTextSize();
        while (lines > 1 && !getText().toString().contains("\n")) {
            limit--;
            px -= 1;
            setTextSize(TypedValue.COMPLEX_UNIT_PX, px);
            lines = getLineCount();
            if (limit <= 0) {
                Timber.d("correctTextSizeIfNeeded: Failed to rescale, limit reached, final: %s", px);
                break;
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isBreadButton) {
            final int width = getWidth();
            final int height = getHeight();
            shadowRect.set(5, height / 4, width - 5, (int) (height * shadowOffSet));
            int modifiedWidth = width - 10;
            int modifiedHeight = height - height / 4 - 5;
            bRect.set(5, 5, modifiedWidth, modifiedHeight + 5);
            canvas.drawBitmap(shadow, null, shadowRect, null);
            canvas.drawRoundRect(bRect, ROUND_PIXELS, ROUND_PIXELS, bPaint);
            if (type == 2 || type == 3 || type == 5) {
                canvas.drawRoundRect(bRect, ROUND_PIXELS, ROUND_PIXELS, bPaintStroke);
            }
        }
        super.onDraw(canvas);
    }

    public void setType(int type) {
        if (type == 3) onTouch(true, 1);
        this.type = type;

        if (type == 1) { //blue
            bPaint.setColor(getContext().getColor(R.color.litecoin_litewallet_blue));
            setTextColor(getContext().getColor(R.color.white));
        } else if (type == 2) { //gray stroke
            setTextColor(getContext().getColor(R.color.light_gray));
            setOutline(R.color.litecoin_litewallet_blue, R.color.button_secondary);
        } else if (type == 3) { //blue strokeww
            setTextColor(getContext().getColor(R.color.litecoin_litewallet_blue));
            setOutline(R.color.litecoin_litewallet_blue, R.color.button_secondary);
        } else if (type == 4) { //white stroke
            setOutline(R.color.white, R.color.button_secondary);
        } else if (type == 5) {
            setOutline(R.color.white, R.color.litecoin_litewallet_blue);
        }
        invalidate();
    }

    private void setOutline(@ColorRes int strokeColor, @ColorRes int color) {
        bPaintStroke.setColor(getContext().getColor(strokeColor));
        bPaintStroke.setStyle(Paint.Style.STROKE);
        bPaintStroke.setStrokeWidth(Utils.getPixelsFromDps(getContext(), 1));
        bPaint.setColor(getContext().getColor(color));
        bPaint.setStyle(Paint.Style.FILL);
    }

    private void onTouch(boolean pressed, int duration) {
        float from = pressed ? 1f : 0.96f;
        float to = pressed ? 0.96f : 1f;
        ScaleAnimation scaleAnim = new ScaleAnimation(
                from, to,
                from, to,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 1f);
        scaleAnim.setDuration(duration);
        scaleAnim.setRepeatCount(0);
        scaleAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleAnim.setFillAfter(true);
        scaleAnim.setFillBefore(true);
        scaleAnim.setFillEnabled(true);

        float[] values = new float[]{SHADOW_UNPRESSED, SHADOW_PRESSED};
        if (!pressed) {
            values = new float[]{SHADOW_PRESSED, SHADOW_UNPRESSED};
        }
        ValueAnimator shadowAnim = ValueAnimator.ofFloat(values);
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
