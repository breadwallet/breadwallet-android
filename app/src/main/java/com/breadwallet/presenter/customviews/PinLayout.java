package com.breadwallet.presenter.customviews;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;

import com.breadwallet.R;
import com.breadwallet.tools.security.AuthManager;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;

import java.util.ArrayList;
import java.util.List;

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
    private View mRootView;
    private List<View> mPinDigitViewsAll;
    private List<View> mPinDigitViews;
    private BRKeyboard mKeyboard;
    private int mBaseResourceID;
    public static final int MAX_PIN_DIGITS = 6;
    public static final int OLD_MAX_PIN_DIGITS = 4;
    private StringBuilder mPinStringBuilder = new StringBuilder();
    private int mPinLimit = MAX_PIN_DIGITS;
    private static final int FIRST_INDEX = 0;
    private static final int SIXTH_INDEX = 5;
    private static final int LOCK_FAIL_ATTEMPT_COUNT = 3;
    private OnPinInserted mOnPinInsertedListener;
    private String mLastInsertedPin;
    private boolean mIsPinUpdating;
    private int mPinDotBackground;

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

        mPinLimit = BRKeyStore.getPinCode(context).length();

        if (mPinLimit == OLD_MAX_PIN_DIGITS) {
            useNewDigitLimit(false);
        } else if (mPinLimit == 0) {
            mPinLimit = MAX_PIN_DIGITS;
        }

        TypedValue pinDotColorValue = new TypedValue();
        getContext().getTheme().resolveAttribute(R.attr.pin_dot_filled_background, pinDotColorValue, true);
        mPinDotBackground = pinDotColorValue.resourceId;

    }

    private void useNewDigitLimit(boolean useNewLimit) {
        View first = mPinDigitViewsAll.get(FIRST_INDEX);
        View last = mPinDigitViewsAll.get(SIXTH_INDEX);
        if (useNewLimit) {
            mPinLimit = MAX_PIN_DIGITS;
            first.setVisibility(View.VISIBLE);
            last.setVisibility(View.VISIBLE);
            if (!mPinDigitViews.contains(first)) {
                mPinDigitViews.add(0, first);
            }
            if (!mPinDigitViews.contains(last)) {
                mPinDigitViews.add(last);
            }
        } else {
            mPinLimit = OLD_MAX_PIN_DIGITS;
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
            String pin = mPinStringBuilder.toString();
            String currentPin = BRKeyStore.getPinCode(getContext());
            if (pin.equals(currentPin)) {
                mOnPinInsertedListener.onPinInserted(pin, true);
                authSuccess(getContext());
                useNewDigitLimit(true);
            } else {
                mOnPinInsertedListener.onPinInserted(pin, false);
                if (!mIsPinUpdating && !currentPin.isEmpty()) {
                    authFailed(getContext(), pin);
                }
            }
            updatePinUi(0);
            mPinStringBuilder = new StringBuilder();
            mLastInsertedPin = pin;

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



    public void setup(BRKeyboard keyboard, OnPinInserted onPinInsertedListener) {
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

    public interface OnPinInserted {
        void onPinInserted(String pin, boolean isPinCorrect);
    }

    public void authFailed(final Context app, String pin) {
        if (!pin.equals(mLastInsertedPin)) {
            int failCount = BRKeyStore.getFailCount(app);
            BRKeyStore.putFailCount(failCount + 1, app);
        }
        if (BRKeyStore.getFailCount(app) >= LOCK_FAIL_ATTEMPT_COUNT) {
            AuthManager.getInstance().setWalletDisabled((Activity) app);
        }
    }

    //when pin auth success
    public void authSuccess(final Context app) {
        //put the new total limit
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                BaseWalletManager walletManager = WalletsMaster.getInstance(app).getCurrentWallet(app);
                if (walletManager != null) {
                    BRKeyStore.putTotalLimit(app, walletManager.getTotalSent(app).add(BRKeyStore.getSpendLimit(app, walletManager.getIso())), walletManager.getIso());
                }
                BRKeyStore.putFailCount(0, app);
                BRKeyStore.putLastPinUsedTime(System.currentTimeMillis(), app);
            }
        });
    }

    public void setIsPinUpdating(boolean updating) {
        mIsPinUpdating = updating;
    }

    public static boolean isPinLengthValid(int len) {
        return len == MAX_PIN_DIGITS || len == OLD_MAX_PIN_DIGITS;
    }

}
