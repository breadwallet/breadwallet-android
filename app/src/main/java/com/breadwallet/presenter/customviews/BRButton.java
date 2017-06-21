package com.breadwallet.presenter.customviews;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Button;

import com.breadwallet.R;
import com.breadwallet.tools.manager.TypefacesManager;
import com.breadwallet.tools.util.Utils;

import java.lang.reflect.Field;

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
    private final int ANIMATION_DURATION = 50;
    //    private int currentX = 0;
//    private int currentY = 0;
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
        float px16 = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        //check attributes you need, for example all paddings
        int[] attributes = new int[]{android.R.attr.paddingLeft, android.R.attr.paddingTop, android.R.attr.paddingRight, android.R.attr.paddingBottom, R.attr.isBreadButton};
        //then obtain typed array
        TypedArray arr = ctx.obtainStyledAttributes(attrs, attributes);
        //You can check if attribute exists (in this examle checking paddingRight)
        int paddingLeft = arr.hasValue(0) ? arr.getDimensionPixelOffset(0, -1) : 0;
        int paddingTop = arr.hasValue(1) ? arr.getDimensionPixelOffset(1, -1) : 0;
        int paddingRight = arr.hasValue(2) ? arr.getDimensionPixelOffset(2, -1) : 0;
        int paddingBottom = arr.hasValue(3) ? arr.getDimensionPixelOffset(3, -1) + (int) px16 : (int) px16;
        isBreadButton = arr.getBoolean(4, false);
        setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom);
        a.recycle();
        arr.recycle();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isBreadButton) {

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }

                ScaleAnimation scaleAnim = new ScaleAnimation(
                        1f, 0.9f,
                        1f, 0.9f,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 1f);
                scaleAnim.setDuration(ANIMATION_DURATION);
                scaleAnim.setRepeatCount(0);
                scaleAnim.setInterpolator(new AccelerateDecelerateInterpolator());
                scaleAnim.setFillAfter(true);
                scaleAnim.setFillBefore(true);
                scaleAnim.setFillEnabled(true);

                startAnimation(scaleAnim);
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
//            clicked_on_image = false;
                ScaleAnimation scaleAnim = new ScaleAnimation(
                        0.9f, 1f,
                        0.9f, 1f,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 1f);
                scaleAnim.setDuration(ANIMATION_DURATION);
                scaleAnim.setRepeatCount(0);
                scaleAnim.setInterpolator(new AccelerateDecelerateInterpolator());
                scaleAnim.setFillAfter(true);
                scaleAnim.setFillBefore(true);
                scaleAnim.setFillEnabled(true);

                startAnimation(scaleAnim);

            }
        }

        return super.onTouchEvent(event);

    }

    //Used for new ListenerInfo class structure used beginning with API 14 (ICS)
    private View.OnClickListener getOnClickListener() {
        View.OnClickListener retrievedListener = null;
        String viewStr = "android.view.View";
        String lInfoStr = "android.view.View$ListenerInfo";

        try {
            Field listenerField = Class.forName(viewStr).getDeclaredField("mListenerInfo");
            Object listenerInfo = null;

            if (listenerField != null) {
                listenerField.setAccessible(true);
                listenerInfo = listenerField.get(this);
            }

            Field clickListenerField = Class.forName(lInfoStr).getDeclaredField("mOnClickListener");

            if (clickListenerField != null && listenerInfo != null) {
                retrievedListener = (View.OnClickListener) clickListenerField.get(listenerInfo);
            }
        } catch (NoSuchFieldException ex) {
            Log.e("Reflection", "No Such Field.");
        } catch (IllegalAccessException ex) {
            Log.e("Reflection", "Illegal Access.");
        } catch (ClassNotFoundException ex) {
            Log.e("Reflection", "Class Not Found.");
        }

        return retrievedListener;
    }

}
