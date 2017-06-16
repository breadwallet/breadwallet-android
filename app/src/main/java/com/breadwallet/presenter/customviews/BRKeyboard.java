package com.breadwallet.presenter.customviews;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.breadwallet.R;
import com.breadwallet.tools.animation.SpringAnimator;

import java.util.ArrayList;
import java.util.List;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 2/22/17.
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
public class BRKeyboard extends LinearLayout implements View.OnClickListener {
    public static final String TAG = BRKeyboard.class.getName();
    List<OnInsertListener> listeners = new ArrayList<>();
    private Button num0;
    private Button num1;
    private Button num2;
    private Button num3;
    private Button num4;
    private Button num5;
    private Button num6;
    private Button num7;
    private Button num8;
    private Button num9;
    private Button numDot;
    private ImageButton numDelete;
    private boolean showAlphabet;

    public BRKeyboard(Context context) {
        super(context);
        init(null);
    }

    public BRKeyboard(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public BRKeyboard(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    public BRKeyboard(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        View root = inflate(getContext(), R.layout.pin_pad, this);

        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.BRKeyboard);
        final int N = a.getIndexCount();
        for (int i = 0; i < N; ++i) {
            int attr = a.getIndex(i);
            switch (attr) {
                case R.styleable.BRKeyboard_showAlphabet:
                    showAlphabet = a.getBoolean(attr, false);
                    break;
            }
        }
        a.recycle();

        this.setWillNotDraw(false);
        num0 = (Button) root.findViewById(R.id.num0);
        num1 = (Button) root.findViewById(R.id.num1);
        num2 = (Button) root.findViewById(R.id.num2);
        num3 = (Button) root.findViewById(R.id.num3);
        num4 = (Button) root.findViewById(R.id.num4);
        num5 = (Button) root.findViewById(R.id.num5);
        num6 = (Button) root.findViewById(R.id.num6);
        num7 = (Button) root.findViewById(R.id.num7);
        num8 = (Button) root.findViewById(R.id.num8);
        num9 = (Button) root.findViewById(R.id.num9);
        numDot = (Button) root.findViewById(R.id.numDot);
        numDelete = (ImageButton) root.findViewById(R.id.numDelete);

        num0.setOnClickListener(this);
        num1.setOnClickListener(this);
        num2.setOnClickListener(this);
        num3.setOnClickListener(this);
        num4.setOnClickListener(this);
        num5.setOnClickListener(this);
        num6.setOnClickListener(this);
        num7.setOnClickListener(this);
        num8.setOnClickListener(this);
        num9.setOnClickListener(this);
        numDot.setOnClickListener(this);
        numDelete.setOnClickListener(this);

        num0.setText(getText(0));
        num1.setText(getText(1));
        num2.setText(getText(2));
        num3.setText(getText(3));
        num4.setText(getText(4));
        num5.setText(getText(5));
        num6.setText(getText(6));
        num7.setText(getText(7));
        num8.setText(getText(8));
        num9.setText(getText(9));

    }

    private CharSequence getText(int index) {
        SpannableString span1 = new SpannableString(String.valueOf(index));
        if (showAlphabet) {

            SpannableString span2;
            switch (index) {
                case 2:
                    span2 = new SpannableString("ABC");
                    break;
                case 3:
                    span2 = new SpannableString("DEF");
                    break;
                case 4:
                    span2 = new SpannableString("GHI");
                    break;
                case 5:
                    span2 = new SpannableString("JKL");
                    break;
                case 6:
                    span2 = new SpannableString("MNO");
                    break;
                case 7:
                    span2 = new SpannableString("PQRS");
                    break;
                case 8:
                    span2 = new SpannableString("TUV");
                    break;
                case 9:
                    span2 = new SpannableString("WXYZ");
                    break;
                default:
                    span2 = new SpannableString(" ");
                    break;
            }

            span1.setSpan(new RelativeSizeSpan(1f), 0, 1, 0);
            span2.setSpan(new RelativeSizeSpan(0.35f), 0, span2.length(), 0);
            return TextUtils.concat(span1, "\n", span2);
        } else {
            return span1;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        invalidate();

    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

    }

    public void addOnInsertListener(OnInsertListener listener) {
        listeners.add(listener);
    }

    @Override
    public void onClick(View v) {
        for (OnInsertListener listener : listeners) {
            listener.onClick(v instanceof ImageButton ? "" : ((Button) v).getText().toString());
        }
    }

    public interface OnInsertListener {
        void onClick(String key);
    }

    public void setBRKeyboardColor(int color) {
        setBackgroundColor(getContext().getColor(color));
    }

    public void setBRButtonTextColor(int color) {
        num0.setTextColor(getContext().getColor(color));
        num1.setTextColor(getContext().getColor(color));
        num2.setTextColor(getContext().getColor(color));
        num3.setTextColor(getContext().getColor(color));
        num4.setTextColor(getContext().getColor(color));
        num5.setTextColor(getContext().getColor(color));
        num6.setTextColor(getContext().getColor(color));
        num7.setTextColor(getContext().getColor(color));
        num8.setTextColor(getContext().getColor(color));
        num9.setTextColor(getContext().getColor(color));
        numDot.setTextColor(getContext().getColor(color));
//        numDelete.setColorFilter(getContext().getColor(color));
        invalidate();
    }

    public void setBRButtonBackgroundResId(int resId) {
        num0.setBackgroundResource(resId);
        num1.setBackgroundResource(resId);
        num2.setBackgroundResource(resId);
        num3.setBackgroundResource(resId);
        num4.setBackgroundResource(resId);
        num5.setBackgroundResource(resId);
        num6.setBackgroundResource(resId);
        num7.setBackgroundResource(resId);
        num8.setBackgroundResource(resId);
        num9.setBackgroundResource(resId);
        numDot.setBackgroundResource(resId);
        numDelete.setBackgroundResource(resId);
        invalidate();
    }

    public void setShowDot(boolean b) {
        numDot.setVisibility(b ? VISIBLE : GONE);
        invalidate();
    }

    public void setBreadground(Drawable drawable) {
        setBackground(drawable);
        invalidate();
    }

    /**
     * Change the background of a specific button
     *
     * @param index    the index of the button (10 - delete, 11 - dot)
     * @param drawable the drawable to be used
     */
    public void setCustomButtonBackgroundDrawable(int index, Drawable drawable) {
        switch (index) {
            case 0:
                num0.setBackground(drawable);
                break;
            case 1:
                num1.setBackground(drawable);
                break;
            case 2:
                num2.setBackground(drawable);
                break;
            case 3:
                num3.setBackground(drawable);
                break;
            case 4:
                num4.setBackground(drawable);
                break;
            case 5:
                num5.setBackground(drawable);
                break;
            case 6:
                num6.setBackground(drawable);
                break;
            case 7:
                num7.setBackground(drawable);
                break;
            case 8:
                num8.setBackground(drawable);
                break;
            case 9:
                num9.setBackground(drawable);
                break;
            case 10:
                numDelete.setBackground(drawable);
                break;
            case 11:
                numDot.setBackground(drawable);
                break;
        }
    }

    /**
     * Change the background of a specific button
     *
     * @param index the index of the button (10 - delete, 11 - dot)
     * @param color the color to be used
     */
    public void setCustomButtonBackgroundColor(int index, int color) {
        switch (index) {
            case 0:
                num0.setBackgroundColor(color);
                break;
            case 1:
                num1.setBackgroundColor(color);
                break;
            case 2:
                num2.setBackgroundColor(color);
                break;
            case 3:
                num3.setBackgroundColor(color);
                break;
            case 4:
                num4.setBackgroundColor(color);
                break;
            case 5:
                num5.setBackgroundColor(color);
                break;
            case 6:
                num6.setBackgroundColor(color);
                break;
            case 7:
                num7.setBackgroundColor(color);
                break;
            case 8:
                num8.setBackgroundColor(color);
                break;
            case 9:
                num9.setBackgroundColor(color);
                break;
            case 10:
                numDelete.setBackgroundColor(color);
                break;
            case 11:
                numDot.setBackgroundColor(color);
                break;
        }
    }

    public void setDeleteImage(Drawable res) {
        numDelete.setImageDrawable(res);
    }

}
