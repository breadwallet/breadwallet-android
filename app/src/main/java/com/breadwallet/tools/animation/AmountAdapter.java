package com.breadwallet.tools.animation;

import android.content.Context;
import android.util.Log;
import android.widget.TextView;

import com.breadwallet.R;

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
    private static boolean comaHasBeenInserted = false;
    private static int digitsAfterComma = 2;
    private static final int DIGITS_LIMIT = 10;
    private static boolean isTextColorGrey = true;

    public static void doBackSpace(Context context, TextView amountToPay) {
        CharSequence amount = amountToPay.getText();
        int length = amount.length();
        if (digitsAfterComma < 2) {
            digitsAfterComma++;
        }
        if (amount.charAt(length - 1) == '.') {
            comaHasBeenInserted = false;
            digitsAfterComma = 2;
        }
        if (length == 1) {
            changeTextColor(context, amountToPay, 2);
            amountToPay.setText("0");
        } else if (length == 2 && amount.charAt(0) == '0') {
            changeTextColor(context, amountToPay, 2);
            amountToPay.setText("0");
        } else {
            amountToPay.setText(amount.subSequence(0, length - 1));
        }
        Log.e(TAG, "doBackSpace|comaHasBeenInserted: " + comaHasBeenInserted + ", digitsAfterComma: " +
                digitsAfterComma + ", current text: " + amountToPay.getText().toString());
    }

    public static void insertComma(Context context, TextView amountToPay) {
        if (isTextColorGrey) {
            changeTextColor(context, amountToPay, 1);
        }
        CharSequence amount = amountToPay.getText();
        int length = amount.length();
        if (!comaHasBeenInserted) {
            comaHasBeenInserted = true;
            if (length == 1 && amount.charAt(0) == '0') {
                amountToPay.setText("0.");
            } else {
                amountToPay.setText(amount.toString() + ".");
            }
        }
//        Log.e(TAG, "insertComma|comaHasBeenInserted: " + comaHasBeenInserted + ", digitsAfterComma: " + digitsAfterComma + ", buttonPressed: " + ".");
    }

    public static void insertDigit(Context context, TextView amountToPay, String tmp) {
        CharSequence amount = amountToPay.getText();
        int length = amount.length();
        if (isTextColorGrey) {
            changeTextColor(context, amountToPay, 1);
        }
        if (isDigitInsertingLegal(amountToPay.length())) {
            if (length == 1 && amount.equals("0")) {
                amountToPay.setText(tmp);
            } else {
                amountToPay.setText(amount + tmp);
            }
        }
//        Log.e(TAG, "insertDigit|comaHasBeenInserted: " + comaHasBeenInserted + ", digitsAfterComma: " + digitsAfterComma + ", buttonPressed: " + tmp);
    }

    private static boolean isDigitInsertingLegal(int length) {
        if (comaHasBeenInserted) {
            if (digitsAfterComma > 0) {
                digitsAfterComma--;
                return true;
            } else {
                return false;
            }
        }
        if (length > DIGITS_LIMIT)
            return false;
        return true;
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
        digitsAfterComma = 2;
        Log.e(TAG, "resetKeyboard called!!!!");
    }
}
