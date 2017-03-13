package com.breadwallet.tools.animation;

import android.graphics.Bitmap;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ImageView;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 3/13/17.
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
public class BlurAnimation extends Animation {

    private final ImageView imageView;
    private final Bitmap bitmap;
    private final float startValue;
    private final float stopValue;
    private final float difValue;

    private BlurAnimation(ImageView imageView, Bitmap bitmap, int startValue, int stopValue) {
        this.imageView = imageView;
        this.bitmap = bitmap;
        this.startValue = startValue;
        this.stopValue = stopValue;
        this.difValue = stopValue - startValue;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        super.applyTransformation(interpolatedTime, t);

        int current = (int)(this.difValue * interpolatedTime + this.startValue + 0.5f);
        Bitmap blurred = quickBlur(this.bitmap, current);
        this.imageView.setImageBitmap(blurred);
    }

    public Bitmap quickBlur(Bitmap bitmap, int factor) {
        if(factor <= 0) {
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        }
        return Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() / factor, bitmap.getHeight() / factor, true);
    }
}