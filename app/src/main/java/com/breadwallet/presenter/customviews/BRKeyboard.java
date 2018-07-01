package com.breadwallet.presenter.customviews;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.RelativeSizeSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.breadwallet.R;
import com.breadwallet.tools.util.Utils;

import java.util.ArrayList;


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
    private OnInsertListener mKeyInsertListener;
    private ImageButton mDeleteButton;
    private ArrayList<Button> mPinButtons;
    private static final int LAST_NUMBER_INDEX = 9;
    private static final int DECIMAL_INDEX = 10;

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

        boolean showAlphabet = false;
        TypedArray attributes = getContext().obtainStyledAttributes(attrs, R.styleable.BRKeyboard);
        final int attributeCount = attributes.getIndexCount();
        for (int i = 0; i < attributeCount; ++i) {
            int attr = attributes.getIndex(i);
            switch (attr) {
                case R.styleable.BRKeyboard_showAlphabet:
                    showAlphabet = attributes.getBoolean(attr, false);
                    break;
            }
        }
        attributes.recycle();

        this.setWillNotDraw(false);

        mDeleteButton = root.findViewById(R.id.delete);

        mPinButtons = new ArrayList<>();
        mPinButtons.add((Button) root.findViewById(R.id.num0));
        mPinButtons.add((Button) root.findViewById(R.id.num1));
        mPinButtons.add((Button) root.findViewById(R.id.num2));
        mPinButtons.add((Button) root.findViewById(R.id.num3));
        mPinButtons.add((Button) root.findViewById(R.id.num4));
        mPinButtons.add((Button) root.findViewById(R.id.num5));
        mPinButtons.add((Button) root.findViewById(R.id.num6));
        mPinButtons.add((Button) root.findViewById(R.id.num7));
        mPinButtons.add((Button) root.findViewById(R.id.num8));
        mPinButtons.add((Button) root.findViewById(R.id.num9));
        mPinButtons.add((Button) root.findViewById(R.id.decimal));

        int bottomPaddingDimen = getContext().getResources().getInteger(R.integer.pin_keyboard_bottom_padding);
        int bottomPaddingPixels = Utils.getPixelsFromDps(getContext(), bottomPaddingDimen);

        for (int i = 0; i < mPinButtons.size(); i++) {
            Button button = mPinButtons.get(i);
            button.setOnClickListener(this);

            if (i <= LAST_NUMBER_INDEX) {
                button.setText(getText(i, showAlphabet));
            }

            if (showAlphabet) {
                button.setPadding(0, 0, 0, bottomPaddingPixels);
            }
        }

        mDeleteButton.setOnClickListener(this);
        if (showAlphabet) {
            mDeleteButton.setPadding(0, 0, 0, bottomPaddingPixels);
        }

        invalidate();
    }

    private CharSequence getText(int index, boolean showAlphabet) {
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

    public void setOnInsertListener(OnInsertListener listener) {
        mKeyInsertListener = listener;
    }

    @Override
    public void onClick(View v) {
        if (mKeyInsertListener != null) {
            mKeyInsertListener.onKeyInsert(v instanceof ImageButton ? "" : ((Button) v).getText().toString());
        }

    }

    public interface OnInsertListener {
        void onKeyInsert(String key);
    }

    public void setBRKeyboardColor(int color) {
        setBackgroundColor(getContext().getColor(color));
    }

    public void setBRButtonBackgroundResId(int resId) {
        for (Button button : mPinButtons) {
            button.setBackgroundResource(resId);
        }
        mDeleteButton.setBackgroundResource(resId);
        invalidate();
    }

    public void setShowDecimal(boolean showDecimal) {
        mPinButtons.get(DECIMAL_INDEX).setVisibility(showDecimal ? VISIBLE : GONE);
        invalidate();
    }

    /**
     * Change the background of a specific button
     *
     * @param color the color to be used
     */
    public void setDeleteButtonBackgroundColor(int color) {
        mDeleteButton.setBackgroundColor(color);
        invalidate();
    }

    public void setDeleteImage(int resourceId) {
        mDeleteButton.setImageDrawable(getResources().getDrawable(resourceId));
        invalidate();
    }

    public void setButtonTextColor(int[] colors) {
        for (int i = 0; i < mPinButtons.size(); i++) {
            mPinButtons.get(i).setTextColor(colors[i]);
        }

        invalidate();
    }

}
