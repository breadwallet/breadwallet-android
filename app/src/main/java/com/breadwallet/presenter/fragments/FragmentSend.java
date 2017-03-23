package com.breadwallet.presenter.fragments;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.OvershootInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BRSoftKeyboard;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.sqlite.CurrencyDataSource;
import com.breadwallet.tools.sqlite.PeerDataSource;
import com.breadwallet.tools.util.BRString;

import java.math.BigDecimal;
import java.util.List;


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

public class FragmentSend extends Fragment {
    private static final String TAG = FragmentSend.class.getName();
    public LinearLayout backgroundLayout;
    public ConstraintLayout signalLayout;
    public static final int ANIMATION_DURATION = 300;
    private BRSoftKeyboard keyboard;
    private EditText addressEdit;
    private Button scan;
    private Button paste;
    private Button send;
    private Spinner spinner;
    private EditText commentEdit;
    private StringBuilder amountBuilder;
    private TextView isoText;
    private EditText amountEdit;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.

        View rootView = inflater.inflate(R.layout.fragment_send, container, false);
        backgroundLayout = (LinearLayout) rootView.findViewById(R.id.background_layout);
        signalLayout = (ConstraintLayout) rootView.findViewById(R.id.signal_layout);
        keyboard = (BRSoftKeyboard) rootView.findViewById(R.id.keyboard);
        keyboard.setBRButtonBackgroundColor(R.color.white);
        keyboard.setBRKeyboardColor(R.color.white);
        isoText = (TextView) rootView.findViewById(R.id.iso_text);
        addressEdit = (EditText) rootView.findViewById(R.id.address_edit);
        scan = (Button) rootView.findViewById(R.id.scan_button);
        paste = (Button) rootView.findViewById(R.id.paste_button);
        send = (Button) rootView.findViewById(R.id.send_button);
        commentEdit = (EditText) rootView.findViewById(R.id.comment_edit);
        spinner = (Spinner) rootView.findViewById(R.id.cur_spinner);
        amountEdit = (EditText) rootView.findViewById(R.id.amount_edit);
        setListeners();
        amountBuilder = new StringBuilder(0);

        return rootView;
    }


    private void setListeners() {
        paste.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SpringAnimator.showAnimation(v);
            }
        });

        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SpringAnimator.showAnimation(v);
            }
        });
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SpringAnimator.showAnimation(v);
            }
        });
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String item = parent.getItemAtPosition(position).toString();
                Log.e(TAG, "onItemSelected: " + item);
                isoText.setText(item);
                SpringAnimator.showAnimation(isoText);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                isoText.setText("BTC");
                SpringAnimator.showAnimation(isoText);
            }
        });

        keyboard.addOnInsertListener(new BRSoftKeyboard.OnInsertListener() {
            @Override
            public void onClick(String key) {
                handleClick(key);
            }
        });
        List<String> curList = CurrencyDataSource.getInstance(getContext()).getAllISOs();
        spinner.setAdapter(new ArrayAdapter<>(getContext(), R.layout.bread_spinner_item, curList));
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final ViewTreeObserver observer = signalLayout.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                observer.removeGlobalOnLayoutListener(this);
                animateBackgroundDim();
                animateSignalSlide();
            }
        });

    }

    private void animateSignalSlide() {
        float translationY = signalLayout.getTranslationY();
        float signalHeight = signalLayout.getHeight();
        signalLayout.setTranslationY(translationY + signalHeight);
        signalLayout.animate().translationY(translationY).setDuration(ANIMATION_DURATION).setInterpolator(new OvershootInterpolator(0.7f));
    }

    private void animateBackgroundDim() {
        int transColor = android.R.color.transparent;
        int blackTransColor = R.color.black_trans;

        ValueAnimator anim = new ValueAnimator();
        anim.setIntValues(transColor, blackTransColor);
        anim.setEvaluator(new ArgbEvaluator());
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                backgroundLayout.setBackgroundColor((Integer) valueAnimator.getAnimatedValue());
            }
        });

        anim.setDuration(ANIMATION_DURATION);
        anim.start();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void handleClick(String key) {
        if (key == null) {
            Log.e(TAG, "handleClick: key is null! ");
            return;
        }
        if (key.length() > 1) {
            Log.e(TAG, "handleClick: key is longer: " + key);
            return;
        }

        if (key.isEmpty()) {
            handleDeleteClick();
        } else if (Character.isDigit(key.charAt(0))) {
            handleDigitClick(Integer.parseInt(key));
        } else {
            handleSeparatorClick();
        }
    }

    private void handleDigitClick(Integer dig) {
        String  currAmount = amountBuilder.toString();
        if (new BigDecimal(currAmount.concat(String.valueOf(dig))).doubleValue() <= getMaxAmount(isoText.getText().toString()).doubleValue()) {
            amountBuilder.append(dig);
            updateText();
        } else {
            SpringAnimator.failShakeAnimation(getActivity(), isoText);
        }
//        if ((curAmount.contains(".") && (curAmount.length() - curAmount.indexOf(".") < 8)) || curAmount.length() < ) {
//            amountBuilder.append(dig);
//        }
    }

    private void handleSeparatorClick() {

    }

    private void handleDeleteClick() {

    }

    private void updateText() {
        amountEdit.setText(amountBuilder.toString());
    }






}