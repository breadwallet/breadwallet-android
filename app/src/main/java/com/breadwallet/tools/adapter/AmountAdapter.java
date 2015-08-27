package com.breadwallet.tools.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.fragments.FragmentCurrency;
import com.breadwallet.presenter.fragments.FragmentScanResult;
import com.breadwallet.tools.others.CurrencyManager;

import java.text.DecimalFormat;
import java.text.ParseException;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 8/14/15.
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
public class AmountAdapter {
    private static final String TAG = "AmountAdapter";
    public static boolean comaHasBeenInserted = false;
    private static final int DIGITS_LIMIT = 10;
    private static boolean isTextColorGrey = true;
    private static String ISO;


    public static void preConditions(Activity context, TextView textView, String tmp) {
        switch (tmp) {
            case "":
                doBackSpace(context, textView);
                break;
            case ".":
                insertComma(context, textView);
                break;
            default:
                insertDigit(context, textView, tmp);
                break;
        }
    }

    public static void doBackSpace(Context context, TextView amountToPay) {
        CharSequence amount = amountToPay.getText().toString().substring(1);
        int length = amount.length();
        if (amount.charAt(length - 1) == '.') {
            comaHasBeenInserted = false;
        }
        if (length == 1) {
            changeTextColor(context, amountToPay, 2);
            setAmountText(amountToPay,"0");
        } else if (length == 2 && amount.charAt(0) == '0') {
            changeTextColor(context, amountToPay, 2);
            setAmountText(amountToPay, "0");
        } else {
            setAmountText(amountToPay, amount.subSequence(0, length - 1).toString());
        }
        updateExchangedCurrency(amountToPay.getText().toString().substring(1));
    }

    public static void insertComma(Context context, TextView amountToPay) {
        if (isTextColorGrey) {
            changeTextColor(context, amountToPay, 1);
        }
        CharSequence amount = amountToPay.getText().toString().substring(1);
        int length = amount.length();
        if (!comaHasBeenInserted) {
            comaHasBeenInserted = true;
            if (length == 1 && amount.charAt(0) == '0') {
                setAmountText(amountToPay, "0.");
            } else {
                setAmountText(amountToPay,amount.toString() + ".");
            }
        }
        updateExchangedCurrency(amountToPay.getText().toString().substring(1));
    }

    public static void insertDigit(Context context, TextView amountToPay, String tmp) {
        CharSequence amount = amountToPay.getText().toString().substring(1);
        int length = amount.length();
        if (isTextColorGrey) {
            changeTextColor(context, amountToPay, 1);
        }
        if (isDigitInsertingLegal(amountToPay.getText().toString())) {
            if (length == 1 && amount.equals("0")) {
                setAmountText(amountToPay, tmp);
            } else {
                setAmountText(amountToPay,amount + tmp);
            }
        }
        updateExchangedCurrency(amountToPay.getText().toString().substring(1));

    }

    private static boolean isDigitInsertingLegal(String text) {
        if (text.length() < DIGITS_LIMIT) {
            if (comaHasBeenInserted) {
                return digitsAfterComma(text) < 2 ? true : false;
            }
            return true;
        }
        return false;
    }

    private static int digitsAfterComma(String text) {
        int length = text.length();
        int i;

        for (i = 0; i < length; i++) {
            if (text.charAt(i) == '.') {
                return length - i - 1;
            }
        }
        return -1;
    }

    /**
     * Sets the textColor of the amount TextView to black or grey
     *
     * @patam color the color of the textView: 1 Black, 2 Grey.
     */
    private static void changeTextColor(Context context, TextView textView, int color) {
        isTextColorGrey = color == 1 ? false : true;
        textView.setTextColor((color == 1) ? context.getResources().getColor(R.color.black)
                : context.getResources().getColor(android.R.color.darker_gray));
    }

    public static void resetKeyboard() {
        comaHasBeenInserted = false;
        isTextColorGrey = true;
    }

    public static void updateExchangedCurrency(String tmp) {
        double parsedValue = 0;
        try {
            parsedValue = DecimalFormat.getInstance().parse(tmp).doubleValue();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "parsedValue: " + parsedValue + " | tmp:" + tmp);
        FragmentScanResult.setExchangeText(MainActivity.app,parsedValue);
    }

    private static void setAmountText(TextView textView, String text) {
        if(ISO == null){
            SharedPreferences settings = MainActivity.app.getSharedPreferences(MainActivity.PREFS_NAME, 0);
            ISO = settings.getString(FragmentCurrency.CURRENT_CURRENCY, "USD");
        }
        String iso = FragmentScanResult.currentCurrencyPosition
                == FragmentScanResult.BITCOIN_RIGHT ? "BTC" : ISO;
        textView.setText(CurrencyManager.getFormattedCurrencyString(iso, text));
    }
}
