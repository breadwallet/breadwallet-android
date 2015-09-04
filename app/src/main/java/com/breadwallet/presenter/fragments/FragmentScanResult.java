
package com.breadwallet.presenter.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.tools.adapter.AmountAdapter;
import com.breadwallet.tools.adapter.CurrencyListAdapter;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.listeners.BackPressCustomKeyboardOnTouchListener;
import com.breadwallet.tools.others.CurrencyManager;

import java.math.BigDecimal;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 7/14/15.
 * Copyright (c) 2015 Mihail Gutan <mihail@breadwallet.com>
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

public class FragmentScanResult extends Fragment implements View.OnClickListener {
    public static final String TAG = "FragmentScanResult";
    private TextView scanResult;
    private RelativeLayout customKeyboardLayout;
    public static TextView amountToPay;
    public static TextView amountBeforeArrow;
    public static String address;
    public static final int BITCOIN_LEFT = 1;
    public static final int BITCOIN_RIGHT = 2;
    public static int currentCurrencyPosition = BITCOIN_RIGHT;
    private static TextView doubleArrow;
    private static String ISO;
    public static double rate = -1;
    static final String DOUBLE_ARROW = "\u21CB";

    @Override
    public View onCreateView(LayoutInflater inflater,
                             final ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        final View rootView = inflater.inflate(R.layout.fragment_scan_result, container, false);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        scanResult = (TextView) getActivity().findViewById(R.id.scan_result);
        customKeyboardLayout = (RelativeLayout) getActivity().findViewById(R.id.custom_keyboard_layout);
        amountToPay = (TextView) getActivity().findViewById(R.id.amount_to_pay);
        amountBeforeArrow = (TextView) getActivity().findViewById(R.id.amount_before_arrow);
        customKeyboardLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                customKeyboardLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                int[] locations = new int[2];
                customKeyboardLayout.getLocationOnScreen(locations);
                createCustomKeyboardButtons(locations[1]);
            }
        });

        updateBothTextValues(new BigDecimal("0"), new BigDecimal("0"));
        doubleArrow = (TextView) getActivity().findViewById(R.id.double_arrow_text);
        doubleArrow.setText(DOUBLE_ARROW);
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AmountAdapter.switchCurrencies();
                SpringAnimator.showAnimation(amountBeforeArrow);
                SpringAnimator.showAnimation(amountToPay);
            }
        };
        doubleArrow.setOnClickListener(listener);
        amountBeforeArrow.setOnClickListener(listener);
        amountToPay.setOnClickListener(listener);

    }

    @Override
    public void onResume() {
//        Log.e(TAG, "This is the address: " + address);
        updateRateAndISO();
        String result = address;
//        Log.e(TAG, "This is the result = address: " + result);
        String cleanResult = extractTheCleanAddress(result);
        if (cleanResult != null) {
            scanResult.setText("to: " + cleanResult);
        } else {
            scanResult.setText("NO VALID ADDRESS");
        }

        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        AmountAdapter.resetKeyboard();
    }

    public String extractTheCleanAddress(String str) {
        if (str == null) return "";
        int length = str.length();
        String tmp;
        if (length < 34) {
            return "";
        } else {
            tmp = str.substring(length - 34);
            int tmpLength = tmp.length();
            for (int i = 0; i < tmpLength; i++) {
                if (tmp.charAt(i) < 48) {
                    Log.e(TAG, "Bad address, char: " + tmp.charAt(i));
                    return "";
                } else {
                    if (tmp.charAt(i) > 57 && tmp.charAt(i) < 65) {
                        Log.e(TAG, "Bad address, char: " + tmp.charAt(i));
                        return "";
                    }
                    if (tmp.charAt(i) > 90 && tmp.charAt(i) < 61) {
                        Log.e(TAG, "Bad address, char: " + tmp.charAt(i));
                        return "";
                    }
                    if (tmp.charAt(i) > 122) {
                        Log.e(TAG, "Bad address, char: " + tmp.charAt(i));
                        return "";
                    }
                }
            }
        }
        return tmp;
    }

    private void createCustomKeyboardButtons(int y) {


        int availableWidth = MainActivity.screenParamitersPoint.x;
        int availableHeight = MainActivity.screenParamitersPoint.y;
        int spaceNeededForRest = 100;
        Log.e(TAG, "AvailableWidth: " + availableWidth + ", availableHeight: " + availableHeight +
                ", spaceNeededForRest: " + spaceNeededForRest);
        float gapRate = 0.2f;
        float gap = (availableWidth * gapRate);
        float interButtonGap = gap / 5;
        float buttonWidth = (availableWidth - gap) / 3;
        float buttonHeight = buttonWidth;
        float test = buttonHeight * 4 + gap;

        Log.e(TAG, "space taken: " + test);

        int keyboardLayoutY = y;
        Log.e(TAG, "test: " + test + ", keyboardLayoutY: " + keyboardLayoutY);
        if (test > (availableHeight - (spaceNeededForRest + keyboardLayoutY))) {
            Log.e(TAG, "More Space needed!");
            buttonHeight = ((availableHeight - (spaceNeededForRest + keyboardLayoutY)) - gap) / 4;
        }
        int minimumHeight = (int) (buttonHeight * 4 + interButtonGap * 4);
        int buttonTextSize = 50;
        Log.d(TAG, "The gap: " + gap + ", The buttonHeight: " + buttonHeight + ", buttonWidth: " + buttonWidth);
        if (customKeyboardLayout == null) {
            customKeyboardLayout = (RelativeLayout) getActivity().findViewById(R.id.custom_keyboard_layout);
        }
        customKeyboardLayout.setMinimumHeight(minimumHeight);
        int childCount = 12;
        for (int i = 0; i < childCount; i++) {
            Button b = new Button(getActivity());
            b.setWidth((int) buttonWidth);
            b.setHeight((int) buttonHeight);
            b.setTextSize(buttonTextSize);
            b.setTextColor(getResources().getColor(R.color.darkblue));
            b.setBackgroundResource(R.drawable.button);
            b.setOnClickListener(this);
            if (i < 9)
                b.setText(String.valueOf(i + 1));
            switch (i) {
                case 0:
                    b.setX(interButtonGap / 2 + interButtonGap);
                    break;
                case 1:
                    b.setX(interButtonGap / 2 + interButtonGap * 2 + buttonWidth);
                    break;
                case 2:
                    b.setX(interButtonGap / 2 + interButtonGap * 3 + buttonWidth * 2);
                    break;
                case 3:
                    b.setX(interButtonGap / 2 + interButtonGap);
                    b.setY(buttonHeight + interButtonGap);
                    break;
                case 4:
                    b.setX(interButtonGap / 2 + interButtonGap * 2 + buttonWidth);
                    b.setY(buttonHeight + interButtonGap);
                    break;
                case 5:
                    b.setY(buttonHeight + interButtonGap);
                    b.setX(interButtonGap / 2 + interButtonGap * 3 + buttonWidth * 2);
                    break;
                case 6:
                    b.setY(buttonHeight * 2 + interButtonGap * 2);
                    b.setX(interButtonGap / 2 + interButtonGap);
                    break;
                case 7:
                    b.setX(interButtonGap / 2 + interButtonGap * 2 + buttonWidth);
                    b.setY(buttonHeight * 2 + interButtonGap * 2);
                    break;
                case 8:
                    b.setX(interButtonGap / 2 + interButtonGap * 3 + buttonWidth * 2);
                    b.setY(buttonHeight * 2 + interButtonGap * 2);
                    break;
                case 9:
                    b.setText(".");
                    b.setY(buttonHeight * 3 + interButtonGap * 3);
                    b.setX(interButtonGap / 2 + interButtonGap);
                    break;
                case 10:
                    b.setX(interButtonGap / 2 + interButtonGap * 2 + buttonWidth);
                    b.setText("0");
                    b.setY(buttonHeight * 3 + interButtonGap * 3);
                    break;
                case 11:
                    b.setLongClickable(true);

                    b.setOnTouchListener(new BackPressCustomKeyboardOnTouchListener());
                    b.setId(R.id.keyboard_back_button);
                    b.setX(interButtonGap / 2 + interButtonGap * 3 + buttonWidth * 2 + buttonWidth / 4);
                    b.setBackgroundResource(R.drawable.deletetoleft);
                    b.setWidth((int) (buttonWidth / 2));
                    b.setHeight((int) (buttonHeight / 2));
                    b.setY(buttonHeight * 3 + interButtonGap * 3 + buttonHeight / 4);
                    break;
            }
            customKeyboardLayout.addView(b);
            Log.e(TAG, "FINAL CHECK: ");
        }
    }

    @Override
    public void onClick(View v) {
        String tmp = ((Button) v).getText().toString();
        AmountAdapter.preConditions(tmp);
    }

    public static void updateBothTextValues(BigDecimal bitcoinValue, BigDecimal otherValue) {
        if (ISO == null) {
            updateRateAndISO();
        }
        final String btcIso = "BTC";
        if (currentCurrencyPosition == BITCOIN_RIGHT) {
            amountToPay.setText(CurrencyManager.getFormattedCurrencyString(btcIso, bitcoinValue.toString()));
            amountBeforeArrow.setText(CurrencyManager.getFormattedCurrencyString(ISO, otherValue.toString()));
        } else if (currentCurrencyPosition == BITCOIN_LEFT) {
            amountToPay.setText(CurrencyManager.getFormattedCurrencyString(ISO, bitcoinValue.toString()));
            amountBeforeArrow.setText(CurrencyManager.getFormattedCurrencyString(btcIso, otherValue.toString()));
        } else {
            throw new IllegalArgumentException("currentCurrencyPosition should be BITCOIN_LEFT or BITCOIN_RIGHT");
        }
    }

    public static void updateRateAndISO() {

        SharedPreferences settings = MainActivity.app.getSharedPreferences(MainActivity.PREFS_NAME, 0);
        int position = settings.getInt(FragmentCurrency.POSITION, 0);
        if (CurrencyListAdapter.currencyListAdapter != null && !CurrencyListAdapter.currencyListAdapter.isEmpty()) {
            CurrencyEntity currencyItem = CurrencyListAdapter.currencyListAdapter.getItem(position);
            ISO = currencyItem.code;
            rate = currencyItem.rate;
        }
        if (ISO == null)
            ISO = settings.getString(FragmentCurrency.CURRENT_CURRENCY, "USD");
        if (rate <= 0) {
            rate = settings.getFloat(FragmentCurrency.RATE, 1);
        }
        Log.d(TAG, "ISO: " + ISO + ", rate: " + rate);
    }

}
