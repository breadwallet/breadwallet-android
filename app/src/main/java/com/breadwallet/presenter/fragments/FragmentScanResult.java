package com.breadwallet.presenter.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
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
import android.graphics.Typeface;

import com.breadwallet.R;
import com.breadwallet.BreadWalletApp;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.animation.SpringAnimator;
import com.google.firebase.crash.FirebaseCrash;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;

import static com.breadwallet.tools.util.BRConstants.CURRENT_UNIT_BITS;
import static com.breadwallet.tools.util.BRStringFormatter.getNumberOfDecimalPlaces;


/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 7/14/15.
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

public class FragmentScanResult extends Fragment implements View.OnClickListener {
    private static final String TAG = FragmentScanResult.class.getName();
    private TextView scanResult;
    private RelativeLayout customKeyboardLayout;
    private TextView rightTextView;
    private TextView leftTextView;
    public static String address;
    //amount stuff
    private boolean isTextColorGrey = true;
    private ValueItem rightValue;
    private ValueItem leftValue;
    private int buttonCode = BRConstants.PAY_BUTTON;
    private boolean pressAvailable = true;
    private int unit = BRConstants.CURRENT_UNIT_BITS;

    public static FragmentScanResult instance;
    private String ISO;
    public double rate = -1;

    public static boolean isARequest = false;

    public FragmentScanResult() {
        instance = this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             final ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        final View rootView = inflater.inflate(R.layout.fragment_scan_result, container, false);
        scanResult = (TextView) rootView.findViewById(R.id.scan_result);
        customKeyboardLayout = (RelativeLayout) rootView.findViewById(R.id.custom_keyboard_layout);
        rightTextView = (TextView) rootView.findViewById(R.id.right_textview);
        leftTextView = (TextView) rootView.findViewById(R.id.left_textview);
        TextView doubleArrow = (TextView) rootView.findViewById(R.id.double_arrow_text);
        rightValue = new ValueItem("0", true);
        leftValue = new ValueItem("0", false);
        /**
         * This mess is for the custom keyboard to be created after the soft keyboard is hidden
         * (if it was previously shown) to prevent the wrong position of the keyboard layout placement
         */
        customKeyboardLayout.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    public void onGlobalLayout() {
                        MainActivity app = (MainActivity) getActivity();
                        if (app != null)
                            if (!app.isSoftKeyboardShown()) {
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
                switchCurrencies();
                SpringAnimator.showAnimation(leftTextView);
                SpringAnimator.showAnimation(rightTextView);
            }
        };
        doubleArrow.setText(BRConstants.DOUBLE_ARROW);
        doubleArrow.setOnClickListener(listener);
        leftTextView.setOnClickListener(listener);
        rightTextView.setOnClickListener(listener);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        if (!isARequest && (address == null || address.length() < 20)) {
            throw new RuntimeException("address is corrupted: " + address);
        }
        Activity app = getActivity();
        if (app != null)
            unit = SharedPreferencesManager.getCurrencyUnit(app);
        updateRateAndISO();
        calculateAndPassValuesToFragment("0");
        scanResult.setText(isARequest ? "" : getString(R.string.to) + address);

        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        resetKeyboard();
        Activity app = getActivity();
        if (app != null)
            ((BreadWalletApp) app.getApplication()).setLockerPayButton(BRConstants.LOCKER_BUTTON);
        isARequest = false;
        BRAnimator.hideScanResultFragment();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    private void createCustomKeyboardButtons(int y) {
        Activity app = getActivity();
        if (app == null) return;
        int availableWidth = MainActivity.screenParametersPoint.x;
        int availableHeight = MainActivity.screenParametersPoint.y;
        int spaceNeededForRest = availableHeight / 14;
        float gapRate = 0.2f;
        float gap = (availableWidth * gapRate);
        float interButtonGap = gap / 5;
        float buttonWidth = (availableWidth - gap) / 3;
        float buttonHeight = buttonWidth;
        float spaceNeeded = buttonHeight * 4 + gap;
        int buttonTextSize = 45;
        if (spaceNeeded > (availableHeight - (spaceNeededForRest + y))) {
            buttonHeight = ((availableHeight - (spaceNeededForRest + y)) - gap) / 4;
            buttonTextSize = (int) ((buttonHeight / 7));
        }
        int minimumHeight = (int) (buttonHeight * 4 + interButtonGap * 4);
        if (customKeyboardLayout == null) {
            customKeyboardLayout = (RelativeLayout) app.findViewById(R.id.custom_keyboard_layout);
        }
        customKeyboardLayout.setMinimumHeight(minimumHeight);
        int childCount = 12;
        for (int i = 0; i < childCount; i++) {
            Button b = new Button(app);
            b.setFilterTouchesWhenObscured(true);
            b.setWidth((int) buttonWidth);
            b.setHeight((int) buttonHeight);
            b.setTextSize(buttonTextSize);
            b.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
            //noinspection deprecation
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
                    imageB = new ImageButton(app);
                    imageB.setBackgroundResource(R.drawable.button_regular_blue);
                    imageB.setImageResource(R.drawable.deletetoleft);
                    imageB.setOnClickListener(this);
                    imageB.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    imageB.setLongClickable(true);
                    imageB.setOnClickListener(this);
                    imageB.setId(R.id.keyboard_back_button);
                    //noinspection deprecation
                    imageB.setX(interButtonGap / 2 + interButtonGap * 3 + buttonWidth * 2);
                    imageB.setY(buttonHeight * 3 + interButtonGap * 3);
                    imageB.setMinimumWidth((int) buttonWidth);
                    imageB.setMinimumHeight((int) buttonHeight);
                    break;
            }
            customKeyboardLayout.addView(imageB != null ? imageB : b);
        }
    }

    @Override
    public void onClick(View v) {
        String tmp;
        try {
            tmp = ((Button) v).getText().toString();
        } catch (ClassCastException ex) {
            tmp = "";
        }
        preConditions(tmp);
    }

    public void updateBothTextValues(String left, String right) {
        if (ISO == null) updateRateAndISO();
        if (ISO == null) ISO = "USD";
        leftValue.value = left;
        rightValue.value = right;
        final String btcISO = "BTC";

        String formattedRightVal = getFormattedCurrencyStringForKeyboard(rightValue.isBitcoin ? btcISO : ISO, rightValue.value, true);
        String formattedLeftVal = getFormattedCurrencyStringForKeyboard(leftValue.isBitcoin ? btcISO : ISO, leftValue.value, false);

        rightTextView.setText(formattedRightVal);
        leftTextView.setText(formattedLeftVal);
    }

    private String getCleanValue(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '.' || Character.isDigit(c)) builder.append(c);
        }
        return builder.toString();
    }

    private void updateRateAndISO() {
        Activity app = getActivity();
        if (app == null) return;
        ISO = SharedPreferencesManager.getIso(app);
        getOtherValue().iso = ISO;
        rate = SharedPreferencesManager.getRate(app);
    }

    public void preConditions(String tmp) {
        Activity app = getActivity();
        if (app == null) return;
        if (FragmentScanResult.isARequest) {
            buttonCode = BRConstants.REQUEST_BUTTON;
        } else {
            buttonCode = BRConstants.PAY_BUTTON;
        }
        switch (tmp) {
            case "":
                doBackSpace();
                break;
            case ".":
                insertSeparator();
                break;
            default:
                insertDigit(tmp);
                break;
        }
    }

    private void doBackSpace() {
        Activity app = getActivity();
        if (app == null) return;
        String amount = rightValue.value;
        int length = amount.length();
        if (length > 1) {
            calculateAndPassValuesToFragment(rightValue.value.substring(0, length - 1));
        } else {
            ((BreadWalletApp) app.getApplication()).setLockerPayButton(BRConstants.LOCKER_BUTTON);
            changeTextColor(2);
            calculateAndPassValuesToFragment("0");
        }
    }

    private void insertSeparator() {
        Activity app = getActivity();
        if (app == null) return;
        if (isTextColorGrey) {
            changeTextColor(1);
            ((BreadWalletApp) app.getApplication()).setLockerPayButton(buttonCode);
        }
        String amount = rightValue.value;
        int maxDigit = getMaxFractionDigits();

        if (!amount.contains(".") && maxDigit != 0)
            calculateAndPassValuesToFragment(amount + ".");
    }

    private void insertDigit(String tmp) {
        Activity app = getActivity();
        if (app == null) return;
        String amount = rightValue.value;

        int length = amount.length();
        if (isTextColorGrey) {
            changeTextColor(1);
            ((BreadWalletApp) app.getApplication()).setLockerPayButton(buttonCode);
        }
        if (isDigitInsertingLegal(tmp)) {
            if (length == 1 && amount.equals("0")) {
                calculateAndPassValuesToFragment(tmp);
            } else {
                calculateAndPassValuesToFragment(rightValue.value + tmp);
            }
        }
    }

    private boolean isDigitInsertingLegal(String tmp) {
        int maxDig = getMaxFractionDigits();
        long limit = 21000000000000L;
        if (unit == BRConstants.CURRENT_UNIT_MBITS)
            limit = 21000000000L;
        if (unit == BRConstants.CURRENT_UNIT_BITCOINS)
            limit = 21000000L;

        if (rightValue.isBitcoin) {
            maxDig = BRConstants.MAX_DIGITS_AFTER_SEPARATOR_BITS;

            if (unit == BRConstants.CURRENT_UNIT_MBITS)
                maxDig = BRConstants.MAX_DIGITS_AFTER_SEPARATOR_MBITS;
            if (unit == BRConstants.CURRENT_UNIT_BITCOINS)
                maxDig = BRConstants.MAX_DIGITS_AFTER_SEPARATOR_BITCOINS;

        }
        boolean isFractionStarted = rightValue.value.contains(".");
        int nrOfDecimals = getNumberOfDecimalPlaces(rightValue.value);
        if (isFractionStarted)
            return nrOfDecimals < maxDig;

        long l = 0;
        try {
            l = Long.valueOf(rightValue.value + tmp);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return l < limit;
    }

    private int getMaxFractionDigits() {
        try {
            Currency currency = Currency.getInstance(rightValue.iso);
            return currency.getDefaultFractionDigits();
        } catch (Exception e) {
            return 2;
        }
    }

    /**
     * Sets the textColor of the amount TextView to black or grey
     *
     * @param color the color of the textView: 1 Black, 2 Grey.
     */
    private void changeTextColor(int color) {
        Activity app = getActivity();
        if (app == null) return;
        isTextColorGrey = color != 1;
        rightTextView.setTextColor((color == 1) ? app.getColor(R.color.black)
                : app.getColor(android.R.color.darker_gray));
    }

    public void resetKeyboard() {
        isTextColorGrey = true;
        rightValue.value = "0";
        leftValue.value = "0";
    }

    public void calculateAndPassValuesToFragment(String valuePassed) {
        String divideBy = "1000000";
        if (unit == BRConstants.CURRENT_UNIT_MBITS) divideBy = "1000";
        if (unit == BRConstants.CURRENT_UNIT_BITCOINS) divideBy = "1";
        rightValue.value = valuePassed;
        BigDecimal rightValueObject = new BigDecimal(valuePassed);
        BigDecimal leftValueObject;
        BigDecimal theRate = new BigDecimal(rate);

        //from bits to other currency using rate
        if (theRate.intValue() > 0 && rightValueObject.doubleValue() != 0) {
            if (rightValue.isBitcoin) {
                leftValueObject = theRate.multiply(rightValueObject.divide(new BigDecimal(divideBy)));
            } else {
                //from other currency to bits using rate
                leftValueObject = rightValueObject.multiply(new BigDecimal(divideBy)).
                        divide(theRate, 8, RoundingMode.HALF_UP);
            }
        } else {
            leftValueObject = new BigDecimal("0");
        }

        updateBothTextValues(leftValueObject.toString(), valuePassed);
    }

    public void switchCurrencies() {
        if (checkPressingAvailability()) {
            rightValue.value = getCleanValue(rightTextView.getText().toString());
            leftValue.value = getCleanValue(leftTextView.getText().toString());

            ValueItem tmp = rightValue;
            rightValue = leftValue;
            leftValue = tmp;
            updateBothTextValues(leftValue.value, rightValue.value);
        }
    }

    public boolean checkPressingAvailability() {
        if (pressAvailable) {
            pressAvailable = false;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    pressAvailable = true;
                }
            }, 100);
            return true;
        } else {
            return false;
        }
    }

    public String   getFormattedCurrencyStringForKeyboard(String isoCurrencyCode, String amount, boolean rightItem) {
        Activity app = getActivity();
        if (amount == null || app == null) {
            FirebaseCrash.report(new NullPointerException("getFormattedCurrencyStringForKeyboard: AMOUNT == null"));
            Log.e(TAG, "getFormattedCurrencyStringForKeyboard: AMOUNT == null");
            return "0";
        }

        String multiplyBy = "100";

        if (unit == BRConstants.CURRENT_UNIT_MBITS) multiplyBy = "100000";
        if (unit == BRConstants.CURRENT_UNIT_BITCOINS) multiplyBy = "100000000";

        BigDecimal result;

        if (isoCurrencyCode.equals("BTC")) {
            result = new BigDecimal(amount).multiply(new BigDecimal(multiplyBy));
        } else {
            result = new BigDecimal(amount).multiply(new BigDecimal("100"));
        }

        DecimalFormat currencyFormat;
        result = result.divide(new BigDecimal("100"));
        currencyFormat = (DecimalFormat) DecimalFormat.getCurrencyInstance(Locale.getDefault());
        DecimalFormatSymbols decimalFormatSymbols;
        Currency currency;
        String symbol = null;
        decimalFormatSymbols = currencyFormat.getDecimalFormatSymbols();
        int decimalPoints = 0;
        if (Objects.equals(isoCurrencyCode, "BTC")) {
            String currencySymbolString = BRConstants.bitcoinLowercase;
            currencyFormat.setMinimumFractionDigits(0);
            switch (unit) {
                case CURRENT_UNIT_BITS:
                    currencySymbolString = BRConstants.bitcoinLowercase;
                    decimalPoints = 2;
                    if (getNumberOfDecimalPlaces(result.toPlainString()) == 1)
                        currencyFormat.setMinimumFractionDigits(1);
                    break;
                case BRConstants.CURRENT_UNIT_MBITS:
                    currencySymbolString = "m" + BRConstants.bitcoinUppercase;
                    decimalPoints = 5;
                    result = new BigDecimal(String.valueOf(result.doubleValue())).divide(new BigDecimal("1000"));
                    break;
                case BRConstants.CURRENT_UNIT_BITCOINS:
                    currencySymbolString = BRConstants.bitcoinUppercase;
                    decimalPoints = 8;
                    result = new BigDecimal(String.valueOf(result.doubleValue())).divide(new BigDecimal("1000000"));
                    break;
            }
            symbol = currencySymbolString;
        } else {
            try {
                currency = Currency.getInstance(isoCurrencyCode);
            } catch (IllegalArgumentException e) {
                currency = Currency.getInstance(Locale.getDefault());
            }
            symbol = currency.getSymbol();
            decimalPoints = currency.getDefaultFractionDigits();
        }
        decimalFormatSymbols.setCurrencySymbol(symbol);
        currencyFormat.setMaximumFractionDigits(decimalPoints);
        int currNrOfDecimal = getNumberOfDecimalPlaces(amount);
        currencyFormat.setGroupingUsed(true);
        if (rightItem && amount.endsWith("."))
            currencyFormat.setDecimalSeparatorAlwaysShown(true);
        currencyFormat.setDecimalFormatSymbols(decimalFormatSymbols);
        currencyFormat.setNegativePrefix(decimalFormatSymbols.getCurrencySymbol() + "-");
        currencyFormat.setNegativeSuffix("");
        currencyFormat.setMinimumFractionDigits(currNrOfDecimal > decimalPoints ? decimalPoints : currNrOfDecimal);
        return currencyFormat.format(result);
    }

    public ValueItem getBitcoinValue() {
        if (rightValue.isBitcoin) return rightValue;
        else return leftValue;
    }

    public ValueItem getOtherValue() {
        if (!rightValue.isBitcoin) return rightValue;
        else return leftValue;
    }

    public class ValueItem {
        public String value;
        public boolean isBitcoin;
        public String iso;

        public ValueItem(String value, boolean isBitcoin) {
            this.value = value;
            this.isBitcoin = isBitcoin;
        }

    }

}
