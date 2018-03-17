package com.breadwallet.presenter.customviews;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.breadwallet.R;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 2/24/17.
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
public class BRLockScreenConstraintLayout extends ConstraintLayout {
    public static final String TAG = BRLockScreenConstraintLayout.class.getName();
    private Paint trianglesPaintBlack;
    private Path pathBlack;
    private Paint trianglesPaint;
    private Path path;
    private float mXfract = 0f;
    private float mYfract = 0f;

    private int width;
    private int height;
    private boolean created;

    public BRLockScreenConstraintLayout(Context context) {
        super(context);
        init();
    }

    public BRLockScreenConstraintLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BRLockScreenConstraintLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        trianglesPaintBlack = new Paint();
        trianglesPaint = new Paint();
        trianglesPaintBlack.setStyle(Paint.Style.FILL);
        pathBlack = new Path();
        path = new Path();

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (w != 0 && !created) {
            created = true;
            width = w;
            height = h;
            createTriangles(w, h);

            trianglesPaint.setShader(new LinearGradient(0, 0, w, 0, getContext().getColor(R.color.logo_gradient_start),
                    getContext().getColor(R.color.logo_gradient_end), Shader.TileMode.MIRROR));
            trianglesPaintBlack.setShadowLayer(10.0f, 5f, 5f, getContext().getColor(R.color.gray_shadow));

            invalidate();
        }

    }

    private void createTriangles(int w, int h) {
        pathBlack.moveTo(0, 0);
        path.moveTo(0, 0);

        pathBlack.lineTo(w, 0);
        path.lineTo(w + 1, 0);

        pathBlack.lineTo(-1, h / 4);
        pathBlack.lineTo(0, h / 4 + 2);
        path.lineTo(0, h / 4 + 1);

        pathBlack.lineTo(w-1, h / 2 - h / 8);
        path.lineTo(w, h / 2 - h / 8 - 1);

        pathBlack.lineTo(0, h-1);
        path.lineTo(1, h);
    }

    public void setYFraction(final float fraction) {
        mYfract = fraction;
        float translationY = getHeight() * fraction;
        setTranslationY(translationY);
    }

    public float getYFraction() {
        return mYfract;
    }

    public void setXFraction(final float fraction) {
        mXfract = fraction;
        float translationX = getWidth() * fraction;
        setTranslationX(translationX);
    }

    public float getXFraction() {
        return mXfract;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        long start = System.currentTimeMillis();
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        // Correct any translations set before the measure was set
        setTranslationX(mXfract * width);
        setTranslationY(mYfract * height);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawPath(pathBlack, trianglesPaintBlack);
        canvas.drawPath(path, trianglesPaint);

    }

}
