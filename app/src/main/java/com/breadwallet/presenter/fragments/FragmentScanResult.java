
package com.breadwallet.presenter.fragments;

import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.tools.animation.AmountAdapter;


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
    private TextView scanresult;
    private RelativeLayout customKeyboardLayout;
    private TextView amountToPay;
    public static String address;
    static final String DOUBLE_ARROW = "\u21CB";
    static final String BITCOIN_LOWERCASE = "\u2422";

    @Override
    public View onCreateView(LayoutInflater inflater,
                             final ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        final View rootView = inflater.inflate(
                R.layout.fragment_scan_result, container, false);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        scanresult = (TextView) getActivity().findViewById(R.id.scan_result);
        customKeyboardLayout = (RelativeLayout) getActivity().findViewById(R.id.custom_keyboard_layout);
        amountToPay = (TextView) getActivity().findViewById(R.id.amount_to_pay);
        createCustomKeyboardButtons();
        Log.e(TAG, "createCustomKeyboardButtons called!!!!!!");
//        TextView doubleArrow = (TextView) getActivity().findViewById(R.id.test_text_charset);
//        doubleArrow.setText(DOUBLE_ARROW);
    }

    @Override
    public void onResume() {

//        Log.e(TAG, "This is the address: " + address);
        String result = address;
//        Log.e(TAG, "This is the result = address: " + result);
        String cleanResult = extractTheCleanAddress(result);
        if (cleanResult != null) {
            scanresult.setText("to: " + cleanResult);
        } else {
            scanresult.setText("NO VALID ADDRESS");
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

    private void createCustomKeyboardButtons() {
        Point sizePoint = new Point();
        getActivity().getWindowManager().getDefaultDisplay().getSize(sizePoint);
        int availableWidth = sizePoint.x;
        Log.e(TAG, "AvailableWidth: " + availableWidth);
        float gapRate = 0.2f;
        float gap = (availableWidth * gapRate);
        float interButtonGap = gap / 5;
        float buttonSize = (availableWidth - gap) / 3;
        int minimumHeight = (int) (buttonSize * 4 + interButtonGap * 4);
        int buttonTextSize = 50;
        Log.d(TAG, "The gap: " + gap + ", The buttonSize: " + buttonSize);
        if (customKeyboardLayout == null) {
            customKeyboardLayout = (RelativeLayout) getActivity().findViewById(R.id.custom_keyboard_layout);
        }
        customKeyboardLayout.setMinimumHeight(minimumHeight);
        int childCount = 12;
        for (int i = 0; i < childCount; i++) {
            Button b = new Button(getActivity());
            b.setWidth((int) buttonSize);
            b.setHeight((int) buttonSize);
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
                    b.setX(interButtonGap / 2 + interButtonGap * 2 + buttonSize);
                    break;
                case 2:
                    b.setX(interButtonGap / 2 + interButtonGap * 3 + buttonSize * 2);
                    break;
                case 3:
                    b.setX(interButtonGap / 2 + interButtonGap);
                    b.setY(buttonSize + interButtonGap);
                    break;
                case 4:
                    b.setX(interButtonGap / 2 + interButtonGap * 2 + buttonSize);
                    b.setY(buttonSize + interButtonGap);
                    break;
                case 5:
                    b.setY(buttonSize + interButtonGap);
                    b.setX(interButtonGap / 2 + interButtonGap * 3 + buttonSize * 2);
                    break;
                case 6:
                    b.setY(buttonSize * 2 + interButtonGap * 2);
                    b.setX(interButtonGap / 2 + interButtonGap);
                    break;
                case 7:
                    b.setX(interButtonGap / 2 + interButtonGap * 2 + buttonSize);
                    b.setY(buttonSize * 2 + interButtonGap * 2);
                    break;
                case 8:
                    b.setX(interButtonGap / 2 + interButtonGap * 3 + buttonSize * 2);
                    b.setY(buttonSize * 2 + interButtonGap * 2);
                    break;
                case 9:
                    b.setText(".");
                    b.setY(buttonSize * 3 + interButtonGap * 3);
                    b.setX(interButtonGap / 2 + interButtonGap);
                    break;
                case 10:
                    b.setX(interButtonGap / 2 + interButtonGap * 2 + buttonSize);
                    b.setText("0");
                    b.setY(buttonSize * 3 + interButtonGap * 3);
                    break;
                case 11:
                    b.setX(interButtonGap / 2 + interButtonGap * 3 + buttonSize * 2 + buttonSize / 4);
                    b.setBackgroundResource(R.drawable.deletetoleft);
                    b.setWidth((int) (buttonSize / 2));
                    b.setHeight((int) (buttonSize / 2));
                    b.setY(buttonSize * 3 + interButtonGap * 3 + buttonSize / 4);
                    break;
            }
            customKeyboardLayout.addView(b);

        }
    }

    @Override
    public void onClick(View v) {
        String tmp = ((Button) v).getText().toString();
        switch (tmp) {
            case "":
                AmountAdapter.doBackSpace(getActivity(), amountToPay);
                break;
            case ".":
                AmountAdapter.insertComma(getActivity(), amountToPay);
                break;
            default:
                AmountAdapter.insertDigit(getActivity(), amountToPay, tmp);
                break;
        }
    }

}
