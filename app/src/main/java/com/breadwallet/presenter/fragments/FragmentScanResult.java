
package com.breadwallet.presenter.fragments;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.BreadWalletApp;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.tools.AddressReader;
import com.breadwallet.tools.CurrencyManager;
import com.breadwallet.tools.adapter.AmountAdapter;
import com.breadwallet.tools.adapter.CurrencyListAdapter;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.listeners.BackPressCustomKeyboardOnTouchListener;

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

        scanResult = (TextView) rootView.findViewById(R.id.scan_result);
        customKeyboardLayout = (RelativeLayout) rootView.findViewById(R.id.custom_keyboard_layout);
        amountToPay = (TextView) rootView.findViewById(R.id.amount_to_pay);
        amountBeforeArrow = (TextView) rootView.findViewById(R.id.amount_before_arrow);
        doubleArrow = (TextView) rootView.findViewById(R.id.double_arrow_text);

        /**
         * This mess is for the custom keyboard to be created after the soft keyboard is hidden
         * (if it was previously shown) to prevent the wrong position of the keyboard layout placement
         */
        customKeyboardLayout.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    public void onGlobalLayout() {
                        if (!MainActivity.app.isSoftKeyboardShown()) {
                            int[] locations = new int[2];
                            customKeyboardLayout.getLocationOnScreen(locations);
                            customKeyboardLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            createCustomKeyboardButtons(locations[1]);

                        }
                    }
                });
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AmountAdapter.switchCurrencies();
                SpringAnimator.showAnimation(amountBeforeArrow);
                SpringAnimator.showAnimation(amountToPay);
            }
        };
        updateBothTextValues(new BigDecimal("0"), new BigDecimal("0"));
        doubleArrow.setText(DOUBLE_ARROW);
        doubleArrow.setOnClickListener(listener);
        amountBeforeArrow.setOnClickListener(listener);
        amountToPay.setOnClickListener(listener);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        updateRateAndISO();
        String result = address;
        //Log.e(TAG, "This is the address: " + address);
        //Log.e(TAG, "This is the result = address: " + result);
        String cleanResult = AddressReader.getTheAddress(result);
        scanResult.setText("to: " + cleanResult);
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        AmountAdapter.resetKeyboard();
        ((BreadWalletApp) getActivity().getApplication()).setLockerPayButton(BreadWalletApp.LOCKER_BUTTON);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    private void createCustomKeyboardButtons(int y) {

        int availableWidth = MainActivity.screenParametersPoint.x;
        int availableHeight = MainActivity.screenParametersPoint.y;
        int spaceNeededForRest = availableHeight / 14;
//        Log.e(TAG, "screenParametersPoint.x: " + availableWidth + " screenParametersPoint.y: " + availableHeight);
//        Log.e(TAG, "AvailableWidth: " + availableWidth + ", availableHeight: " + availableHeight +
//                ", spaceNeededForRest: " + spaceNeededForRest);
        float gapRate = 0.2f;
        float gap = (availableWidth * gapRate);
        float interButtonGap = gap / 5;
        float buttonWidth = (availableWidth - gap) / 3;
        float buttonHeight = buttonWidth;
        float spaceNeeded = buttonHeight * 4 + gap;
//        Log.e(TAG, "space taken: " + spaceNeeded);
        int keyboardLayoutY = y;
        int buttonTextSize = 45;
//        Log.e(TAG, "spaceNeeded: " + spaceNeeded + ", keyboardLayoutY: " + keyboardLayoutY);
//        Log.e(TAG, String.format("gap:%f interButtonGap:%f spaceNeeded:%f spaceNeededForRest:%d keyboardLayoutY:%d",
//                gap, interButtonGap, spaceNeeded, spaceNeededForRest, keyboardLayoutY));
        if (spaceNeeded > (availableHeight - (spaceNeededForRest + keyboardLayoutY))) {
//            Log.e(TAG, "More Space needed! buttonHeight: " + buttonHeight);
            buttonHeight = ((availableHeight - (spaceNeededForRest + keyboardLayoutY)) - gap) / 4;
            buttonTextSize = (int) ((buttonHeight / 9));
        }
        int minimumHeight = (int) (buttonHeight * 4 + interButtonGap * 4);
//        Log.d(TAG, "The gap: " + gap + ", The buttonHeight: " + buttonHeight + ", buttonWidth: " + buttonWidth);
        if (customKeyboardLayout == null) {
            customKeyboardLayout = (RelativeLayout) getActivity().findViewById(R.id.custom_keyboard_layout);
        }
        customKeyboardLayout.setMinimumHeight(minimumHeight);
        int childCount = 12;
//        Log.e(TAG, "Button params: width " + buttonWidth + " height " + buttonHeight);
        for (int i = 0; i < childCount; i++) {
            Button b = new Button(getActivity());
            b.setWidth((int) buttonWidth);
            b.setHeight((int) buttonHeight);
            b.setTextSize(buttonTextSize);
            b.setTextColor(getResources().getColor(R.color.dark_blue));
            b.setBackgroundResource(R.drawable.button_regular_blue);
            b.setOnClickListener(this);
            b.setGravity(Gravity.CENTER);
            b.setTextAlignment(View.TEXT_ALIGNMENT_GRAVITY);
            ImageButton imageB = null;
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
                    imageB = new ImageButton(getActivity());
                    imageB.setImageResource(R.drawable.deletetoleft);
                    imageB.setOnClickListener(this);
                    imageB.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    imageB.setLongClickable(true);
                    imageB.setOnTouchListener(new BackPressCustomKeyboardOnTouchListener());
                    imageB.setId(R.id.keyboard_back_button);
                    imageB.setBackgroundColor(android.R.color.transparent);
                    imageB.setX(interButtonGap / 2 + interButtonGap * 3 + buttonWidth * 2 + (buttonWidth / 4));
                    imageB.setY(buttonHeight * 3 + interButtonGap * 3 + (buttonHeight / 4));
                    break;
            }
            customKeyboardLayout.addView(imageB != null ? imageB : b);
        }
    }

    @Override
    public void onClick(View v) {
        String tmp = ((Button) v).getText().toString();
        AmountAdapter.preConditions(tmp);
    }

    public static void updateBothTextValues(BigDecimal bitcoinValue, BigDecimal otherValue) {
        if (ISO == null) updateRateAndISO();
        final String btcIso = "BTC";
        if (currentCurrencyPosition == BITCOIN_RIGHT) {
            amountToPay.setText(CurrencyManager.getFormattedCurrencyString(btcIso, bitcoinValue.toString()));
            amountBeforeArrow.setText(CurrencyManager.getFormattedCurrencyString(ISO, otherValue.toString()));
        } else if (currentCurrencyPosition == BITCOIN_LEFT) {
            amountToPay.setText(CurrencyManager.getFormattedCurrencyString(ISO, bitcoinValue.toString()));
            amountBeforeArrow.setText(CurrencyManager.getFormattedCurrencyString(btcIso, otherValue.toString()));
        } else {
            throw new IllegalArgumentException("currentPosition should be BITCOIN_LEFT or BITCOIN_RIGHT");
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

    /**
     * Saves the y coordinate of the keyboard once it's created successfully the first time
     */
    private void saveKeyboardLocationInPrefs() {


    }

}
