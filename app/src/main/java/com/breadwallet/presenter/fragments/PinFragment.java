package com.breadwallet.presenter.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BRKeyboard;
import com.breadwallet.presenter.customviews.PinLayout;
import com.breadwallet.presenter.interfaces.BRAuthCompletion;
import com.breadwallet.tools.animation.DecelerateOvershootInterpolator;
import com.breadwallet.tools.util.Utils;


/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 6/29/15.
 * Copyright (c) 2016 breadwallet LLC
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class PinFragment extends Fragment implements PinLayout.OnPinInserted {
    private static final String TAG = PinFragment.class.getName();

    private BRAuthCompletion mCompletion;

    private BRKeyboard mKeyboard;

    private TextView mTitle;
    private TextView mMessage;
    private RelativeLayout mDialogLayout;
    private ConstraintLayout mMainLayout;
    private boolean mAuthSucceeded;
    private PinLayout mPinDigitViews;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.

        View rootView = inflater.inflate(R.layout.fragment_pin, container, false);
        mKeyboard = rootView.findViewById(R.id.brkeyboard);
        mKeyboard.setDeleteImage(R.drawable.ic_delete_gray);

        mTitle = rootView.findViewById(R.id.title);
        mMessage = rootView.findViewById(R.id.message);
        mDialogLayout = rootView.findViewById(R.id.pin_dialog);
        mMainLayout = rootView.findViewById(R.id.activity_pin);

        mMainLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().getFragmentManager().beginTransaction().remove(PinFragment.this).commit();
            }
        });

        mPinDigitViews = rootView.findViewById(R.id.pin_digits);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        float keyboardTrY = mKeyboard.getTranslationY();
        Bundle bundle = getArguments();
        String titleString = bundle.getString("title");
        String messageString = bundle.getString("message");
        if (!Utils.isNullOrEmpty(titleString)) {
            mTitle.setText(titleString);
        }
        if (!Utils.isNullOrEmpty(messageString)) {
            mMessage.setText(messageString);
        }
        mKeyboard.setTranslationY(keyboardTrY + mKeyboard.getHeight());
        mKeyboard.animate()
                .translationY(keyboardTrY)
                .setDuration(R.dimen.animation_medium)
                .setInterpolator(new DecelerateOvershootInterpolator(2.0f, 1f))
                .withLayer();
        float dialogScaleX = mDialogLayout.getScaleX();
        float dialogScaleY = mDialogLayout.getScaleY();
        mDialogLayout.setScaleX(dialogScaleX / 2);
        mDialogLayout.setScaleY(dialogScaleY / 2);
        mDialogLayout.animate()
                .scaleY(dialogScaleY)
                .scaleX(dialogScaleX)
                .setInterpolator(new OvershootInterpolator(2f));

    }

    @Override
    public void onResume() {
        super.onResume();
        mAuthSucceeded = false;
        mPinDigitViews.setup(mKeyboard, this);
    }

    private void handleSuccess() {
        mAuthSucceeded = true;
        mCompletion.onComplete();
        Activity activity = getActivity();

        if (activity != null && !activity.isDestroyed()) {
            activity.getFragmentManager().popBackStack();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mKeyboard.animate().translationY(R.dimen.animation_short).withLayer();
        mDialogLayout.animate().scaleY(0).scaleX(0).alpha(0);
        mMainLayout.animate().alpha(0);
        if (!mAuthSucceeded) {
            mCompletion.onCancel();
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (getActivity() != null) {
                    getActivity().getFragmentManager().beginTransaction().remove(PinFragment.this).commitAllowingStateLoss();
                }
            }
        }, DateUtils.SECOND_IN_MILLIS);
    }

    @Override
    public void onPause() {
        super.onPause();
        mPinDigitViews.cleanUp();
    }

    public void setCompletion(BRAuthCompletion completion) {
        this.mCompletion = completion;
    }

    @Override
    public void onPinInserted(String pin, boolean isPinCorrect) {
        if (isPinCorrect) {
            handleSuccess();
        }
    }
}
