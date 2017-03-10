package com.breadwallet.presenter.customviews;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
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
    private Paint trianglesPaint;
    private Path trianglePath;


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
        trianglePath = new Path();
        trianglesPaint = new Paint();
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        trianglesPaint.setShadowLayer(14f, 0, 0, getContext().getColor(R.color.dark_grey));
        trianglesPaint.setAntiAlias(true);

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        trianglesPaint.setShader(new LinearGradient(0, 0, w, 0, getContext().getColor(R.color.logo_gradient_start),
                getContext().getColor(R.color.logo_gradient_end), Shader.TileMode.MIRROR));
        invalidate();

    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

//        int width = getMeasuredWidth();
//        int height = getMeasuredHeight();
//
//        trianglePath.moveTo(0, 0);
//        trianglePath.lineTo(width, 0);
//        trianglePath.lineTo(0, height / 4);
//        trianglePath.lineTo(0, 0);
//
//        canvas.drawPath(trianglePath, trianglesPaint);

    }

}
