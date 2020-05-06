package com.breadwallet.legacy.presenter.customviews;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;

import androidx.annotation.Nullable;

import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;

import com.breadwallet.R;
import com.breadwallet.app.BreadApp;
import com.breadwallet.tools.security.BrdUserState;
import com.breadwallet.tools.security.BrdUserManager;
import com.breadwallet.tools.security.BrdUserManagerKt;
import com.breadwallet.tools.util.EventUtils;

import java.util.ArrayList;
import java.util.List;

import static org.kodein.di.TypesKt.TT;

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
public class PinLayout extends LinearLayout implements BRKeyboard.OnInsertListener {
    private static final String TAG = PinLayout.class.getName();
    private static final int FIRST_INDEX = 0;
    private static final int SIXTH_INDEX = 5;
    private static final int PIN_INSERTED_DELAY_MILLISECONDS = 50;
    private View mRootView;
    private List<View> mPinDigitViewsAll;
    private List<View> mPinDigitViews;
    private BRKeyboard mKeyboard;
    private int mBaseResourceID;
    private StringBuilder mPinStringBuilder = new StringBuilder();
    private int mPinLimit = BrdUserManagerKt.PIN_LENGTH;
    private PinLayoutListener mOnPinInsertedListener;
    private boolean mIsPinUpdating;
    private int mPinDotBackground;
    private BrdUserManager mUserManager = BreadApp.getKodeinInstance().Instance(TT(BrdUserManager.class), null);

    public PinLayout(Context context) {
        super(context);
        init(context, null);
    }

    public PinLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public PinLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public PinLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mRootView = inflate(getContext(), R.layout.pin_digits, this);
        mPinDigitViews = new ArrayList<>();
        mPinDigitViewsAll = new ArrayList<>();
        TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.PinLayout);
        mBaseResourceID = attributes.getResourceId(R.styleable.PinLayout_pinDigitsResId, R.drawable.ic_pin_dot_empty);
        mPinDigitViews.add(mRootView.findViewById(R.id.digit1));
        mPinDigitViews.add(mRootView.findViewById(R.id.digit2));
        mPinDigitViews.add(mRootView.findViewById(R.id.digit3));
        mPinDigitViews.add(mRootView.findViewById(R.id.digit4));
        mPinDigitViews.add(mRootView.findViewById(R.id.digit5));
        mPinDigitViews.add(mRootView.findViewById(R.id.digit6));
        mPinDigitViewsAll.addAll(mPinDigitViews);
        setPinDigitViewResourceId(mBaseResourceID);

        useNewDigitLimit(!mUserManager.pinCodeNeedsUpgrade());

        TypedValue pinDotColorValue = new TypedValue();
        getContext().getTheme().resolveAttribute(R.attr.pin_dot_filled_background, pinDotColorValue, true);
        mPinDotBackground = pinDotColorValue.resourceId;

    }

    private void useNewDigitLimit(boolean useNewLimit) {
        View first = mPinDigitViewsAll.get(FIRST_INDEX);
        View last = mPinDigitViewsAll.get(SIXTH_INDEX);
        if (useNewLimit) {
            mPinLimit = BrdUserManagerKt.PIN_LENGTH;
            first.setVisibility(View.VISIBLE);
            last.setVisibility(View.VISIBLE);
            if (!mPinDigitViews.contains(first)) {
                mPinDigitViews.add(0, first);
            }
            if (!mPinDigitViews.contains(last)) {
                mPinDigitViews.add(last);
            }
        } else {
            mPinLimit = BrdUserManagerKt.LEGACY_PIN_LENGTH;
            first.setVisibility(View.GONE);
            last.setVisibility(View.GONE);
            if (mPinDigitViews.contains(first)) {
                mPinDigitViews.remove(first);
            }
            if (mPinDigitViews.contains(last)) {
                mPinDigitViews.remove(last);
            }
        }
    }

    private void setPinDigitViewResourceId(int resId) {
        for (View view : mPinDigitViews) {
            view.setBackgroundResource(resId);
        }
    }

    private void handleKeyInsert() {
        int pinLength = mPinStringBuilder.length();

        updatePinUi(pinLength);

        if (mPinStringBuilder.length() == mPinLimit) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    String pin = mPinStringBuilder.toString();
                    if (mUserManager.hasPinCode() && mUserManager.verifyPinCode(pin)) {
                        mOnPinInsertedListener.onPinInserted(pin, true);
                        useNewDigitLimit(true);
                    } else {
                        mOnPinInsertedListener.onPinInserted(pin, false);
                        if (!mIsPinUpdating) {
                            authFailed();
                        }
                    }

                    updatePinUi(0);
                    mPinStringBuilder = new StringBuilder();
                }
            }, PIN_INSERTED_DELAY_MILLISECONDS);

        }
    }

    private void updatePinUi(int pinLength) {
        for (int i = 0; i < mPinDigitViews.size(); i++) {
            if (pinLength > 0) {
                mPinDigitViews.get(i).setBackgroundResource(mPinDotBackground);
            } else {
                mPinDigitViews.get(i).setBackgroundResource(mBaseResourceID);
            }
            pinLength--;
        }
    }

    private void handleDigitClick(Integer digit) {
        if (mPinStringBuilder.length() < mPinLimit) {
            mPinStringBuilder.append(digit);
        }
        handleKeyInsert();
    }

    private void handleDeleteClick() {
        if (mPinStringBuilder.length() > 0) {
            mPinStringBuilder.deleteCharAt(mPinStringBuilder.length() - 1);
        }
        handleKeyInsert();
    }

    public void setup(BRKeyboard keyboard, PinLayoutListener onPinInsertedListener) {
        this.mKeyboard = keyboard;
        mKeyboard.setOnInsertListener(this);
        mKeyboard.setShowDecimal(false);
        mOnPinInsertedListener = onPinInsertedListener;
    }

    public void cleanUp() {
        mKeyboard.setOnInsertListener(null);
        mOnPinInsertedListener = null;
    }

    @Override
    public void onKeyInsert(String key) {
        if (key == null) {
            Log.e(TAG, "onInsert: key is null! ");
            return;
        }

        if (key.isEmpty()) {
            handleDeleteClick();
        } else if (Character.isDigit(key.charAt(0))) {
            handleDigitClick(Integer.parseInt(key.substring(0, 1)));
        } else {
            Log.e(TAG, "onInsert: oops: " + key);
        }
    }

    private void authFailed() {
        if (mUserManager.getState() instanceof BrdUserState.Disabled) {
            EventUtils.pushEvent(EventUtils.EVENT_LOGIN_LOCKED);
            mOnPinInsertedListener.onPinLocked();
        }
    }

    public void setIsPinUpdating(boolean updating) {
        mIsPinUpdating = updating;
    }

    public interface PinLayoutListener {
        /**
         * Callback to notify the pin that has been entered.
         *
         * @param pin          The PIN that has been entered.
         * @param isPinCorrect True if the PIN is correct.
         */
        void onPinInserted(String pin, boolean isPinCorrect);

        /**
         * Callback for when the PIN has been locked.
         */
        void onPinLocked();
    }

}
