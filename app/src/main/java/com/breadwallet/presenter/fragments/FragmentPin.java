package com.breadwallet.presenter.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.BreadActivity;
import com.breadwallet.presenter.customviews.BRKeyboard;
import com.breadwallet.presenter.interfaces.BRAuthCompletion;
import com.breadwallet.tools.animation.DecelerateOvershootInterpolator;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.security.AuthManager;
import com.breadwallet.tools.security.BRKeyStore;
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

public class FragmentPin extends Fragment {
    private static final String TAG = FragmentPin.class.getName();

    private BRAuthCompletion completion;

    private BRKeyboard keyboard;
    private LinearLayout pinLayout;
    private View dot1;
    private View dot2;
    private View dot3;
    private View dot4;
    private View dot5;
    private View dot6;
    private StringBuilder pin = new StringBuilder();
    private int pinLimit = 6;
//    private boolean pinInsertAllowed;

    private TextView title;
    private TextView message;
    private RelativeLayout dialogLayout;
    ConstraintLayout mainLayout;
    private boolean authSucceeded;
    private String customTitle;
    private String customMessage;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.

        View rootView = inflater.inflate(R.layout.fragment_bread_pin, container, false);
        keyboard = (BRKeyboard) rootView.findViewById(R.id.brkeyboard);
        pinLayout = (LinearLayout) rootView.findViewById(R.id.pinLayout);

        if (BRKeyStore.getPinCode(getContext()).length() == 4) pinLimit = 4;

        title = (TextView) rootView.findViewById(R.id.title);
        message = (TextView) rootView.findViewById(R.id.message);
        dialogLayout = (RelativeLayout) rootView.findViewById(R.id.pin_dialog);
        mainLayout = (ConstraintLayout) rootView.findViewById(R.id.activity_pin);

        mainLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().getFragmentManager().beginTransaction().remove(FragmentPin.this).commit();
            }
        });

        dot1 = rootView.findViewById(R.id.dot1);
        dot2 = rootView.findViewById(R.id.dot2);
        dot3 = rootView.findViewById(R.id.dot3);
        dot4 = rootView.findViewById(R.id.dot4);
        dot5 = rootView.findViewById(R.id.dot5);
        dot6 = rootView.findViewById(R.id.dot6);

        keyboard.addOnInsertListener(new BRKeyboard.OnInsertListener() {
            @Override
            public void onClick(String key) {
                handleClick(key);
            }
        });
        keyboard.setShowDot(false);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        float keyboardTrY = keyboard.getTranslationY();
        Bundle bundle = getArguments();
        String titleString = bundle.getString("title");
        String messageString = bundle.getString("message");
        if (!Utils.isNullOrEmpty(titleString)) {
            customTitle = titleString;
            title.setText(customTitle);
        }
        if (!Utils.isNullOrEmpty(messageString)) {
            customMessage = messageString;
            message.setText(customMessage);
        }
        keyboard.setTranslationY(keyboardTrY + BreadActivity.screenParametersPoint.y / 3);
        keyboard.animate()
                .translationY(keyboardTrY)
                .setDuration(400)
                .setInterpolator(new DecelerateOvershootInterpolator(2.0f, 1f))
                .withLayer();
        float dialogScaleX = dialogLayout.getScaleX();
        float dialogScaleY = dialogLayout.getScaleY();
        dialogLayout.setScaleX(dialogScaleX / 2);
        dialogLayout.setScaleY(dialogScaleY / 2);
        dialogLayout.animate()
                .scaleY(dialogScaleY)
                .scaleX(dialogScaleX)
                .setInterpolator(new OvershootInterpolator(2f));

    }

    @Override
    public void onResume() {
        super.onResume();
        updateDots();
        authSucceeded = false;
    }


    private void handleClick(String key) {
        if (key == null) {
            Log.e(TAG, "handleClick: key is null! ");
            return;
        }

        if (key.isEmpty()) {
            handleDeleteClick();
        } else if (Character.isDigit(key.charAt(0))) {
            handleDigitClick(Integer.parseInt(key.substring(0, 1)));
        } else {
            Log.e(TAG, "handleClick: oops: " + key);
        }
    }

    private void handleDigitClick(Integer dig) {
        if (pin.length() < pinLimit)
            pin.append(dig);
        updateDots();
    }

    private void handleDeleteClick() {
        if (pin.length() > 0)
            pin.deleteCharAt(pin.length() - 1);
        updateDots();
    }

    private void updateDots() {
        if(dot1 == null) return;
        AuthManager.getInstance().updateDots(getActivity(), pinLimit, pin.toString(), dot1, dot2, dot3, dot4, dot5, dot6, R.drawable.ic_pin_dot_gray, new AuthManager.OnPinSuccess() {
            @Override
            public void onSuccess() {
                if (AuthManager.getInstance().checkAuth(pin.toString(), getContext())) {
                    handleSuccess();
                } else {
                    handleFail();
                }
                pin = new StringBuilder("");
            }
        });

    }

    private void handleSuccess() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                authSucceeded = true;
                completion.onComplete();
                AuthManager.getInstance().authSuccess(getActivity());
                if (getActivity() != null) {
                    getActivity().getFragmentManager().popBackStack();
                }

            }
        }).start();

    }

    private void handleFail() {
        SpringAnimator.failShakeAnimation(getActivity(), pinLayout);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                updateDots();
            }
        }, 500);
        AuthManager.getInstance().authFail(getActivity());
    }

    @Override
    public void onStop() {
        super.onStop();
        keyboard.animate()
                .translationY(1000)
                .withLayer();
        dialogLayout.animate()
                .scaleY(0)
                .scaleX(0).alpha(0);
        mainLayout.animate().alpha(0);
        if (!authSucceeded)
            completion.onCancel();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (getActivity() != null)
                    getActivity().getFragmentManager().beginTransaction().remove(FragmentPin.this).commit();
            }
        }, 1000);

    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void setCompletion(BRAuthCompletion completion) {
        this.completion = completion;
    }

}